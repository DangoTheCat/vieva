import asyncio
import io
import logging
import os
import sys
import wave
from contextlib import asynccontextmanager
from typing import Any, List, Optional, Tuple

try:
    import numpy as np
except ImportError:
    np = None

try:
    import soundfile as sf
except (ImportError, OSError):
    sf = None

try:
    import torch
except ImportError:
    torch = None

from fastapi import FastAPI, HTTPException, Query
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
MAX_TEXT_LENGTH = 2000
DEFAULT_SAMPLE_RATE = 48000

tts_engine = None
synthesis_semaphore: Optional[asyncio.Semaphore] = None


def get_max_concurrency() -> int:
    raw = os.getenv("TTS_MAX_CONCURRENCY", "1")
    try:
        val = int(raw)
        if val <= 0:
            logger.warning("TTS_MAX_CONCURRENCY must be positive, defaulting to 1")
            return 1
        return val
    except ValueError:
        logger.warning(f"Invalid TTS_MAX_CONCURRENCY '{raw}', defaulting to 1")
        return 1


def get_semaphore() -> asyncio.Semaphore:
    global synthesis_semaphore
    if synthesis_semaphore is None:
        synthesis_semaphore = asyncio.Semaphore(get_max_concurrency())
    return synthesis_semaphore


def get_available_voices() -> List[str]:
    """
    Returns real engine capability IDs or explicit TTS_VOICES.
    Checks list_voices() (official ZeroTTS API), get_voices(), or voices attribute.
    If engine exposes capability (even empty), respect it without falling back
    to unvalidated configured voices.
    Never invents voices or falls back to DEFAULT_SPEAKER when unconfigured.
    """
    if tts_engine is None:
        return []

    engine_voices: Optional[List[str]] = None
    if hasattr(tts_engine, "list_voices") and callable(tts_engine.list_voices):
        try:
            res = tts_engine.list_voices()
            if isinstance(res, (list, tuple, set)):
                engine_voices = [
                    str(x["name"]) if isinstance(x, dict) and "name" in x
                    else (str(x["id"]) if isinstance(x, dict) and "id" in x else str(x))
                    for x in res
                ]
            elif isinstance(res, dict):
                engine_voices = [str(k) for k in res.keys()]
        except Exception as e:
            logger.warning(f"Failed to query engine list_voices(): {e}")
    elif hasattr(tts_engine, "get_voices") and callable(tts_engine.get_voices):
        try:
            res = tts_engine.get_voices()
            if isinstance(res, (list, tuple, set)):
                engine_voices = [
                    str(x["id"]) if isinstance(x, dict) and "id" in x
                    else (str(x["name"]) if isinstance(x, dict) and "name" in x else str(x))
                    for x in res
                ]
            elif isinstance(res, dict):
                engine_voices = [str(k) for k in res.keys()]
        except Exception as e:
            logger.warning(f"Failed to query engine get_voices(): {e}")
    elif hasattr(tts_engine, "voices"):
        res = getattr(tts_engine, "voices")
        if isinstance(res, (list, tuple, set)):
            engine_voices = [
                str(x["id"]) if isinstance(x, dict) and "id" in x
                else (str(x["name"]) if isinstance(x, dict) and "name" in x else str(x))
                for x in res
            ]
        elif isinstance(res, dict):
            engine_voices = [str(k) for k in res.keys()]

    raw_cfg = os.getenv("TTS_VOICES", "").strip()
    cfg_voices = [v.strip() for v in raw_cfg.split(",") if v.strip()]

    # If engine exposed its capability, respect it strictly (even if empty)
    if engine_voices is not None:
        if len(engine_voices) == 0:
            return []
        if cfg_voices:
            return [v for v in cfg_voices if v in engine_voices]
        return sorted(list(dict.fromkeys(engine_voices)))

    return sorted(list(dict.fromkeys(cfg_voices)))


def validate_synthesis_params(text: Optional[str], voice: Optional[str]) -> Tuple[str, str]:
    if text is None or not text.strip():
        raise HTTPException(status_code=400, detail="Text cannot be empty")

    cleaned_text = text.strip()
    if len(cleaned_text) > MAX_TEXT_LENGTH:
        raise HTTPException(status_code=400, detail=f"Text exceeds {MAX_TEXT_LENGTH} characters limit")

    available = get_available_voices()
    if not available:
        logger.error("No valid voices available in engine or configuration")
        raise HTTPException(status_code=500, detail="Speech synthesis failed")

    if voice and voice.strip():
        chosen_voice = voice.strip()
    else:
        # Choose valid available default if configured DEFAULT_SPEAKER absent
        if DEFAULT_SPEAKER in available:
            chosen_voice = DEFAULT_SPEAKER
        else:
            chosen_voice = available[0]

    if chosen_voice not in available:
        raise HTTPException(status_code=400, detail=f"Unsupported voice '{chosen_voice}'")

    return cleaned_text, chosen_voice


def _encode_wav_bytes(audio_data: Any, sample_rate: int) -> bytes:
    if isinstance(audio_data, bytes):
        return audio_data

    actual_sr = sample_rate
    if isinstance(audio_data, tuple) and len(audio_data) == 2:
        if isinstance(audio_data[1], int):
            audio_data, actual_sr = audio_data[0], audio_data[1]
        elif isinstance(audio_data[0], int):
            actual_sr, audio_data = audio_data[0], audio_data[1]

    if hasattr(audio_data, "waveform"):
        audio_data = getattr(audio_data, "waveform")
    elif hasattr(audio_data, "audio"):
        audio_data = getattr(audio_data, "audio")

    if torch is not None and isinstance(audio_data, torch.Tensor):
        audio_array = audio_data.detach().cpu().numpy()
    elif np is not None and isinstance(audio_data, np.ndarray):
        audio_array = audio_data
    elif np is not None:
        audio_array = np.array(audio_data, dtype=np.float32)
    else:
        audio_array = audio_data

    if hasattr(audio_array, "squeeze"):
        audio_array = audio_array.squeeze()

    buffer = io.BytesIO()
    if sf is not None:
        try:
            sf.write(buffer, audio_array, actual_sr, format="WAV", subtype="PCM_16")
            return buffer.getvalue()
        except Exception as e:
            logger.debug(f"soundfile write failed: {e}, falling back to wave module")
            buffer = io.BytesIO()

    # Portable standard PCM_16 WAV encoder
    if np is not None and isinstance(audio_array, np.ndarray):
        clipped = np.clip(audio_array, -1.0, 1.0)
        pcm16 = (clipped * 32767.0).astype(np.int16).tobytes()
    else:
        pcm16 = b""
    with wave.open(buffer, "wb") as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(actual_sr)
        wf.writeframes(pcm16)
    return buffer.getvalue()


def _synthesize_and_encode(text: str, voice: str, sample_rate: int) -> bytes:
    try:
        from zerotts import normalize_vi_text
        norm_text = normalize_vi_text(text)
    except Exception:
        norm_text = text

    audio_data = tts_engine.synthesize(norm_text, voice=voice)
    return _encode_wav_bytes(audio_data, sample_rate)


async def perform_synthesis(text: Optional[str], voice: Optional[str]) -> StreamingResponse:
    if tts_engine is None:
        raise HTTPException(status_code=503, detail="TTS engine is not ready")

    cleaned_text, chosen_voice = validate_synthesis_params(text, voice)
    sample_rate = getattr(tts_engine, "sample_rate", getattr(tts_engine, "sampling_rate", DEFAULT_SAMPLE_RATE))

    sem = get_semaphore()
    await sem.acquire()
    slot_held = True
    try:
        # Offload synthesis and WAV encoding together off the event loop
        task = asyncio.create_task(
            asyncio.to_thread(_synthesize_and_encode, cleaned_text, chosen_voice, sample_rate)
        )
        try:
            wav_bytes = await asyncio.shield(task)
            sem.release()
            slot_held = False
        except asyncio.CancelledError:
            # Cancellation must not release concurrency slot while underlying to_thread inference still runs
            try:
                await task
            except Exception:
                pass
            raise
    except HTTPException:
        raise
    except asyncio.CancelledError:
        raise
    except Exception as e:
        logger.error(f"Synthesis failed: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail="Speech synthesis failed")
    finally:
        if slot_held:
            sem.release()

    def stream_chunks():
        chunk_size = 4096
        for i in range(0, len(wav_bytes), chunk_size):
            yield wav_bytes[i:i + chunk_size]

    return StreamingResponse(
        stream_chunks(),
        media_type="audio/wav",
        headers={
            "Content-Disposition": 'inline; filename="synthesized.wav"',
            "Cache-Control": "no-cache",
        },
    )


class SynthesizeRequest(BaseModel):
    text: str = Field(..., description="Text content to synthesize")
    speaker: Optional[str] = Field(default=None, description="Speaker voice identifier")


@asynccontextmanager
async def lifespan(app: FastAPI):
    global tts_engine, synthesis_semaphore
    synthesis_semaphore = asyncio.Semaphore(get_max_concurrency())
    logger.info(f"Initializing ZeroTTS engine with model '{MODEL_ID}'...")
    try:
        from zerotts import ZeroTTS
        tts_engine = ZeroTTS.from_pretrained(MODEL_ID)
        logger.info(f"ZeroTTS model '{MODEL_ID}' successfully loaded.")
    except Exception as e:
        logger.error(f"Failed to load ZeroTTS model: {e}", exc_info=True)
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
    is_ready = tts_engine is not None
    voices = get_available_voices() if is_ready else []
    cuda_avail = False
    device = "cpu"
    try:
        import onnxruntime as ort
        providers = ort.get_available_providers()
        cuda_avail = "CUDAExecutionProvider" in providers
        if cuda_avail:
            device = "cuda"
    except Exception:
        pass
    if not cuda_avail and torch and hasattr(torch, "cuda"):
        cuda_avail = bool(torch.cuda.is_available())
        if cuda_avail:
            device = "cuda"

    sr = getattr(tts_engine, "sample_rate", getattr(tts_engine, "sampling_rate", DEFAULT_SAMPLE_RATE)) if is_ready else DEFAULT_SAMPLE_RATE
    return {
        "status": "healthy" if is_ready else "unready",
        "service": "tts",
        "model": MODEL_ID,
        "engine_ready": is_ready,
        "device": device,
        "cuda_available": cuda_avail,
        "sample_rate": sr,
        "voice_count": len(voices),
    }


@app.get("/voices")
async def get_voices():
    if tts_engine is None:
        raise HTTPException(status_code=503, detail="TTS engine is not ready")
    voices = get_available_voices()
    return {
        "voices": voices,
        "count": len(voices),
    }


@app.get("/speak")
async def speak(
    text: Optional[str] = Query(default=None, description="Text to synthesize"),
    voice: Optional[str] = Query(default=None, description="Voice identifier"),
):
    return await perform_synthesis(text, voice)


@app.post("/synthesize")
async def synthesize(request: SynthesizeRequest):
    return await perform_synthesis(request.text, request.speaker)
