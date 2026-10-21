import asyncio
import io
import json
import logging
import os
import sys
import numpy as np
import soundfile as sf
import torch
from contextlib import asynccontextmanager
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
from faster_whisper import WhisperModel

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)],
)
logger = logging.getLogger("stt-service")

MODEL_ID = os.getenv("STT_MODEL_ID", "suzii/vi-whisper-large-v3-turbo")
DEVICE = os.getenv("STT_DEVICE", "cuda" if torch.cuda.is_available() else "cpu")
COMPUTE_TYPE = os.getenv("STT_COMPUTE_TYPE", "float16" if torch.cuda.is_available() else "int8")

model: WhisperModel = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    global model
    logger.info(f"Loading STT model '{MODEL_ID}' on device='{DEVICE}', compute_type='{COMPUTE_TYPE}'...")
    try:
        model = WhisperModel(MODEL_ID, device=DEVICE, compute_type=COMPUTE_TYPE)
        logger.info(f"STT model '{MODEL_ID}' successfully loaded.")
    except Exception as e:
        logger.error(f"Failed to load model on {DEVICE}: {e}. Retrying with CPU fallback...")
        model = WhisperModel(MODEL_ID, device="cpu", compute_type="int8")
        logger.info("STT model loaded with fallback device='cpu'.")
    yield
    logger.info("Shutting down STT service...")

app = FastAPI(
    title="VieVa STT Service",
    description="Vietnamese Speech-to-Text streaming service using faster-whisper and vi-whisper-large-v3-turbo",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/health")
async def health():
    return {
        "status": "healthy",
        "service": "stt",
        "model": MODEL_ID,
        "device": DEVICE,
        "cuda_available": torch.cuda.is_available(),
    }

def transcribe_audio_chunk(audio_bytes: bytes, language: str = "vi") -> str:
    """Decodes audio bytes (wav/ogg/webm/pcm) and transcribes using faster-whisper with Silero VAD."""
    if not audio_bytes or len(audio_bytes) < 44:
        return ""
    try:
        # soundfile can read wav/ogg/flac from in-memory BytesIO
        audio_file = io.BytesIO(audio_bytes)
        data, sample_rate = sf.read(audio_file)
        if data.ndim > 1:
            data = data.mean(axis=1)  # convert stereo to mono
        # Convert to float32
        data = data.astype(np.float32)

        segments, _ = model.transcribe(
            data,
            language=language,
            task="transcribe",
            vad_filter=True,
            vad_parameters=dict(
                threshold=0.5,
                min_speech_duration_ms=250,
                min_silence_duration_ms=500,
                speech_pad_ms=400,
            ),
            beam_size=5,
        )
        return " ".join([seg.text.strip() for seg in segments if seg.text.strip()]).strip()
    except Exception as e:
        logger.error(f"Error during audio chunk transcription: {e}")
        return ""

@app.websocket("/ws/transcribe")
async def websocket_transcribe(websocket: WebSocket):
    await websocket.accept()
    logger.info("WebSocket connection accepted for STT transcription.")
    audio_buffer = bytearray()
    
    try:
        while True:
            message = await websocket.receive()
            if "bytes" in message and message["bytes"]:
                chunk = message["bytes"]
                audio_buffer.extend(chunk)

                # Periodic partial transcription if accumulated buffer has audio
                if len(audio_buffer) >= 32000:  # ~1 sec of 16kHz 16-bit PCM or small chunk
                    partial_text = await asyncio.to_thread(transcribe_audio_chunk, bytes(audio_buffer))
                    if partial_text:
                        await websocket.send_text(json.dumps({
                            "type": "partial",
                            "transcript": partial_text,
                        }))

            elif "text" in message and message["text"]:
                data = json.loads(message["text"])
                msg_type = data.get("type", "")

                if msg_type == "commit" or msg_type == "stop":
                    # Perform final transcription on accumulated buffer
                    final_text = await asyncio.to_thread(transcribe_audio_chunk, bytes(audio_buffer))
                    await websocket.send_text(json.dumps({
                        "type": "final",
                        "transcript": final_text,
                    }))
                    audio_buffer.clear()
                    if msg_type == "stop":
                        break
                elif msg_type == "reset":
                    audio_buffer.clear()
                    await websocket.send_text(json.dumps({
                        "type": "reset_ack",
                    }))
    except WebSocketDisconnect:
        logger.info("WebSocket client disconnected.")
    except Exception as e:
        logger.error(f"WebSocket error: {e}")
        try:
            await websocket.send_text(json.dumps({"type": "error", "message": str(e)}))
        except Exception:
            pass
    finally:
        try:
            await websocket.close()
        except Exception:
            pass
