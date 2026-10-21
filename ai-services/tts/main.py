import io
import logging
import os
import sys
from contextlib import asynccontextmanager
from typing import Optional
import numpy as np
import soundfile as sf
import torch
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)],
)
logger = logging.getLogger("tts-service")

MODEL_ID = os.getenv("TTS_MODEL_ID", "zeroweight-ai/ZeroTTS")
DEFAULT_SPEAKER = os.getenv("TTS_DEFAULT_SPEAKER", "maichi")

tts_engine = None

class SynthesizeRequest(BaseModel):
    text: str = Field(..., description="Vietnamese text content to synthesize")
    speaker: Optional[str] = Field(default=DEFAULT_SPEAKER, description="Speaker voice identifier (e.g. maichi)")

@asynccontextmanager
async def lifespan(app: FastAPI):
    global tts_engine
    logger.info(f"Initializing ZeroTTS engine with model '{MODEL_ID}'...")
    try:
        from zerotts import ZeroTTS
        tts_engine = ZeroTTS.from_pretrained(MODEL_ID)
        logger.info(f"ZeroTTS model '{MODEL_ID}' successfully loaded.")
    except Exception as e:
        logger.error(f"Failed to load ZeroTTS model: {e}")
        tts_engine = None
    yield
    logger.info("Shutting down TTS service...")

app = FastAPI(
    title="VieVa TTS Service",
    description="Vietnamese Text-to-Speech synthesis service powered by ZeroTTS",
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
        "service": "tts",
        "model": MODEL_ID,
        "engine_ready": tts_engine is not None,
        "cuda_available": torch.cuda.is_available(),
    }

def generate_wav_stream(audio_array: np.ndarray, sample_rate: int = 24000):
    """Encodes numpy float array into WAV container in memory and yields bytes."""
    buffer = io.BytesIO()
    sf.write(buffer, audio_array, sample_rate, format="WAV", subtype="PCM_16")
    buffer.seek(0)
    while chunk := buffer.read(4096):
        yield chunk

@app.post("/synthesize")
async def synthesize(request: SynthesizeRequest):
    if not request.text or not request.text.strip():
        raise HTTPException(status_code=400, detail="Text cannot be empty")

    speaker_voice = request.speaker or DEFAULT_SPEAKER
    logger.info(f"Synthesizing speech for speaker='{speaker_voice}', text_len={len(request.text)}")

    if tts_engine is None:
        raise HTTPException(status_code=503, detail="TTS Engine is not initialized or still loading")

    try:
        # ZeroTTS synthesize returns an audio numpy array
        audio = tts_engine.synthesize(request.text, voice=speaker_voice)

        # Sampling rate is usually 24000 for ZeroTTS or check engine sample rate
        sample_rate = getattr(tts_engine, "sample_rate", 24000)

        return StreamingResponse(
            generate_wav_stream(audio, sample_rate),
            media_type="audio/wav",
            headers={
                "Content-Disposition": 'inline; filename="synthesized.wav"',
                "Cache-Control": "no-cache",
            },
        )
    except Exception as e:
        logger.error(f"Synthesis failed: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"Speech synthesis error: {str(e)}")
