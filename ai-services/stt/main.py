import asyncio
import io
import json
import logging
import os
import sys
import numpy as np
import torch
from contextlib import asynccontextmanager
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
from faster_whisper import WhisperModel, decode_audio

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

SUPPORTED_LANGUAGES = {"vi": "vi", "vi-vn": "vi", "en": "en", "en-us": "en"}
PARTIAL_INTERVAL_SECONDS = float(os.getenv("STT_PARTIAL_INTERVAL_SECONDS", "1.0"))
MIN_PARTIAL_BYTES = 8000


def normalize_language(raw: str | None) -> str:
    """Maps 'vi', 'vi-VN', 'en', 'en-US' to the Whisper language code; defaults to Vietnamese."""
    if not raw:
        return "vi"
    return SUPPORTED_LANGUAGES.get(raw.strip().lower(), "vi")


def decode_audio_bytes(audio_bytes: bytes) -> np.ndarray:
    """Decodes any container the browser produces (webm/opus, ogg, mp4, wav) into 16 kHz mono float32.

    MediaRecorder emits webm/opus, which libsndfile cannot read, so decoding goes through PyAV
    (bundled with faster-whisper)."""
    return decode_audio(io.BytesIO(audio_bytes), sampling_rate=16000)


def transcribe_audio_chunk(audio_bytes: bytes, language: str = "vi") -> str:
    """Transcribes the accumulated audio of the current answer with Silero VAD."""
    if not audio_bytes or len(audio_bytes) < 44:
        return ""
    try:
        data = decode_audio_bytes(audio_bytes)
        if data.size == 0:
            return ""
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
        # A partial webm buffer can end mid-frame; the next partial or the final pass will succeed.
        logger.warning(f"Could not transcribe audio buffer ({len(audio_bytes)} bytes): {e}")
        return ""


async def send_json(websocket: WebSocket, payload: dict) -> None:
    await websocket.send_text(json.dumps(payload, ensure_ascii=False))


@app.websocket("/ws/transcribe")
async def websocket_transcribe(websocket: WebSocket):
    """Streaming STT contract (see README.md):

    client -> server  binary audio chunks of one answer (webm/opus, ~250 ms each)
    client -> server  {"type":"commit"}  end of the answer, keep the socket open ("eos" is accepted as an alias)
    client -> server  {"type":"stop"}    end of the answer, then close
    client -> server  {"type":"reset"}   drop the buffered audio
    server -> client  {"type":"partial","text":"..."}  at most once per STT_PARTIAL_INTERVAL_SECONDS
    server -> client  {"type":"final","text":"..."}    after commit/stop
    server -> client  {"type":"error","code":"...","message":"..."}

    Query string: ?lang=vi|en|vi-VN|en-US (default vi).
    """
    await websocket.accept()
    language = normalize_language(websocket.query_params.get("lang"))
    logger.info(f"STT WebSocket accepted (language={language}).")
    audio_buffer = bytearray()
    loop = asyncio.get_running_loop()
    last_partial_at = 0.0
    last_partial_size = 0

    try:
        while True:
            message = await websocket.receive()
            if message.get("type") == "websocket.disconnect":
                break

            if message.get("bytes"):
                audio_buffer.extend(message["bytes"])
                now = loop.time()
                grown = len(audio_buffer) - last_partial_size
                if now - last_partial_at >= PARTIAL_INTERVAL_SECONDS and grown >= MIN_PARTIAL_BYTES:
                    last_partial_at = now
                    last_partial_size = len(audio_buffer)
                    partial_text = await asyncio.to_thread(transcribe_audio_chunk, bytes(audio_buffer), language)
                    if partial_text:
                        await send_json(websocket, {"type": "partial", "text": partial_text})
                continue

            if not message.get("text"):
                continue
            try:
                data = json.loads(message["text"])
            except json.JSONDecodeError:
                await send_json(websocket, {"type": "error", "code": "BAD_MESSAGE", "message": "Expected a JSON control message"})
                continue

            msg_type = str(data.get("type", "")).lower()
            if msg_type in ("commit", "eos", "stop"):
                final_text = await asyncio.to_thread(transcribe_audio_chunk, bytes(audio_buffer), language)
                await send_json(websocket, {"type": "final", "text": final_text})
                audio_buffer.clear()
                last_partial_at = 0.0
                last_partial_size = 0
                if msg_type == "stop":
                    break
            elif msg_type == "reset":
                audio_buffer.clear()
                last_partial_at = 0.0
                last_partial_size = 0
                await send_json(websocket, {"type": "reset_ack"})
            else:
                await send_json(websocket, {"type": "error", "code": "UNKNOWN_TYPE", "message": f"Unknown message type: {msg_type}"})
    except WebSocketDisconnect:
        logger.info("WebSocket client disconnected.")
    except Exception as e:
        logger.error(f"WebSocket error: {e}")
        try:
            await send_json(websocket, {"type": "error", "code": "STT_FAILURE", "message": str(e)})
        except Exception:
            pass
    finally:
        try:
            await websocket.close()
        except Exception:
            pass
