import asyncio
import io
import os
import unittest
import wave
from unittest.mock import MagicMock, patch

try:
    import numpy as np
except ImportError:
    np = None

try:
    import soundfile as sf
except (ImportError, OSError):
    sf = None

from fastapi.testclient import TestClient
import main


class FakeTtsEngine:
    """Mock engine simulating ZeroTTS without GPU or model downloads."""
    def __init__(self, sample_rate=48000, voices=None, fail=False, delay=0.0):
        self.sample_rate = sample_rate
        self._voices = voices if voices is not None else ["maichi", "baotrang"]
        self.fail = fail
        self.delay = delay
        self.call_count = 0
        self.last_text = None
        self.last_voice = None

    def list_voices(self):
        """Official ZeroTTS API method."""
        return list(self._voices)

    @property
    def voices(self):
        """Fallback property for backwards compatibility."""
        return list(self._voices)

    def synthesize(self, text: str, voice: str = "maichi"):
        import time
        if self.delay > 0:
            time.sleep(self.delay)
        self.call_count += 1
        self.last_text = text
        self.last_voice = voice
        if self.fail:
            raise RuntimeError("Engine model explosion")
        if np is not None:
            t = np.linspace(0, 0.1, int(self.sample_rate * 0.1), endpoint=False, dtype=np.float32)
            return np.sin(2 * np.pi * 440 * t)
        return b"RIFFstubWAVEfmt fakeaudio"


class TestTtsService(unittest.IsolatedAsyncioTestCase):
    def setUp(self):
        main.tts_engine = None
        main.synthesis_semaphore = None
        os.environ.pop("TTS_VOICES", None)
        os.environ.pop("TTS_MAX_CONCURRENCY", None)

    async def test_health_when_engine_unready(self):
        main.tts_engine = None
        res = await main.health()
        self.assertFalse(res["engine_ready"])
        self.assertEqual(res["voice_count"], 0)
        self.assertEqual(res["status"], "unready")

    async def test_voices_when_engine_unready_raises_503(self):
        main.tts_engine = None
        with self.assertRaises(main.HTTPException) as ctx:
            await main.get_voices()
        self.assertEqual(ctx.exception.status_code, 503)

    async def test_speak_when_engine_unready_raises_503(self):
        main.tts_engine = None
        with self.assertRaises(main.HTTPException) as ctx:
            await main.speak(text="Xin chao", voice="maichi")
        self.assertEqual(ctx.exception.status_code, 503)

    async def test_synthesize_when_engine_unready_raises_503(self):
        main.tts_engine = None
        req = main.SynthesizeRequest(text="Xin chao", speaker="maichi")
        with self.assertRaises(main.HTTPException) as ctx:
            await main.synthesize(req)
        self.assertEqual(ctx.exception.status_code, 503)

    def test_voice_discovery_from_official_list_voices_api(self):
        main.tts_engine = FakeTtsEngine(voices=["maichi", "baotrang", "kimoanh"])
        voices = main.get_available_voices()
        self.assertEqual(sorted(voices), ["baotrang", "kimoanh", "maichi"])

    def test_empty_engine_capability_respected_no_config_fallback(self):
        main.tts_engine = FakeTtsEngine(voices=[])
        os.environ["TTS_VOICES"] = "maichi,minh"
        voices = main.get_available_voices()
        self.assertEqual(voices, [])

    def test_default_speaker_absent_picks_first_available(self):
        main.tts_engine = FakeTtsEngine(voices=["voice_x", "voice_y"])
        text, chosen = main.validate_synthesis_params("Hello", None)
        self.assertEqual(chosen, "voice_x")

    def test_configured_voices_cannot_add_unsupported_ids(self):
        main.tts_engine = FakeTtsEngine(voices=["maichi", "baotrang"])
        os.environ["TTS_VOICES"] = "maichi,invented_voice,baotrang"
        voices = main.get_available_voices()
        self.assertEqual(sorted(voices), ["baotrang", "maichi"])
        self.assertNotIn("invented_voice", voices)

    def test_input_validation_empty_and_whitespace(self):
        main.tts_engine = FakeTtsEngine()
        for bad_text in [None, "", "   ", "\t\n"]:
            with self.assertRaises(main.HTTPException) as ctx:
                main.validate_synthesis_params(bad_text, "maichi")
            self.assertEqual(ctx.exception.status_code, 400)
            self.assertIn("cannot be empty", ctx.exception.detail)

    def test_input_validation_exceeds_max_length(self):
        main.tts_engine = FakeTtsEngine()
        long_text = "a" * 2001
        with self.assertRaises(main.HTTPException) as ctx:
            main.validate_synthesis_params(long_text, "maichi")
        self.assertEqual(ctx.exception.status_code, 400)
        self.assertIn("2000", ctx.exception.detail)

    def test_input_validation_unsupported_voice(self):
        main.tts_engine = FakeTtsEngine(voices=["maichi"])
        with self.assertRaises(main.HTTPException) as ctx:
            main.validate_synthesis_params("Xin chao", "robot_voice")
        self.assertEqual(ctx.exception.status_code, 400)
        self.assertIn("Unsupported voice", ctx.exception.detail)

    def test_concurrency_nonpositive_rejected_and_defaults_to_1(self):
        for bad in ["0", "-2", "invalid"]:
            os.environ["TTS_MAX_CONCURRENCY"] = bad
            self.assertEqual(main.get_max_concurrency(), 1)

        os.environ["TTS_MAX_CONCURRENCY"] = "4"
        self.assertEqual(main.get_max_concurrency(), 4)

    async def test_successful_synthesis_wav_generation_get_speak(self):
        fake_engine = FakeTtsEngine(sample_rate=48000, voices=["maichi"])
        main.tts_engine = fake_engine

        resp = await main.speak(text="Kiem thu he thong", voice="maichi")
        self.assertEqual(resp.media_type, "audio/wav")
        self.assertEqual(fake_engine.call_count, 1)

        chunks = []
        async for chunk in resp.body_iterator:
            chunks.append(chunk)
        body = b"".join(chunks)
        self.assertTrue(body.startswith(b"RIFF"))

        # Validate WAV structure
        with wave.open(io.BytesIO(body), "rb") as wf:
            self.assertEqual(wf.getnchannels(), 1)
            self.assertEqual(wf.getsampwidth(), 2)
            self.assertEqual(wf.getframerate(), 48000)
            self.assertGreater(wf.getnframes(), 0)

    async def test_successful_synthesis_post_synthesize(self):
        fake_engine = FakeTtsEngine(sample_rate=48000, voices=["maichi"])
        main.tts_engine = fake_engine

        req = main.SynthesizeRequest(text="Xin chao ban", speaker="maichi")
        resp = await main.synthesize(req)
        self.assertEqual(resp.media_type, "audio/wav")
        self.assertEqual(fake_engine.call_count, 1)

    async def test_engine_failure_returns_generic_500_without_leaking_exception(self):
        main.tts_engine = FakeTtsEngine(fail=True, voices=["maichi"])

        with self.assertRaises(main.HTTPException) as ctx:
            await main.speak(text="Test loi", voice="maichi")

        self.assertEqual(ctx.exception.status_code, 500)
        self.assertEqual(ctx.exception.detail, "Speech synthesis failed")

    async def test_cancellation_does_not_release_slot_prematurely(self):
        fake_engine = FakeTtsEngine(voices=["maichi"], delay=0.1)
        main.tts_engine = fake_engine
        os.environ["TTS_MAX_CONCURRENCY"] = "1"
        main.synthesis_semaphore = asyncio.Semaphore(1)

        sem = main.get_semaphore()
        task = asyncio.create_task(main.speak(text="Dang xu ly", voice="maichi"))
        await asyncio.sleep(0.02)

        self.assertEqual(sem._value, 0)
        task.cancel()
        try:
            await task
        except asyncio.CancelledError:
            pass

        self.assertEqual(sem._value, 1)


class TestRouterHttpIntegration(unittest.TestCase):
    """End-to-end HTTP route tests using FastAPI TestClient with ZeroTTS-shaped engine."""
    def setUp(self):
        self.client = TestClient(main.app)
        main.synthesis_semaphore = None
        os.environ.pop("TTS_VOICES", None)
        os.environ.pop("TTS_MAX_CONCURRENCY", None)

    def test_http_health_and_voices_when_unready(self):
        main.tts_engine = None
        resp = self.client.get("/health")
        self.assertEqual(resp.status_code, 200)
        data = resp.json()
        self.assertFalse(data["engine_ready"])
        self.assertEqual(data["status"], "unready")

        resp = self.client.get("/voices")
        self.assertEqual(resp.status_code, 503)

    def test_http_health_and_voices_when_ready(self):
        main.tts_engine = FakeTtsEngine(sample_rate=48000, voices=["maichi", "baotrang", "quangminh"])
        resp = self.client.get("/health")
        self.assertEqual(resp.status_code, 200)
        data = resp.json()
        self.assertTrue(data["engine_ready"])
        self.assertEqual(data["status"], "healthy")
        self.assertEqual(data["voice_count"], 3)
        self.assertEqual(data["sample_rate"], 48000)

        resp = self.client.get("/voices")
        self.assertEqual(resp.status_code, 200)
        voices_data = resp.json()
        self.assertEqual(voices_data["count"], 3)
        self.assertIn("maichi", voices_data["voices"])
        self.assertIn("baotrang", voices_data["voices"])

    def test_http_post_synthesize_valid_wav(self):
        main.tts_engine = FakeTtsEngine(sample_rate=48000, voices=["maichi"])
        payload = {"text": "Xin chào thế giới", "speaker": "maichi"}
        resp = self.client.post("/synthesize", json=payload)
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(resp.headers.get("content-type"), "audio/wav")
        body = resp.content
        self.assertTrue(body.startswith(b"RIFF"))
        with wave.open(io.BytesIO(body), "rb") as wf:
            self.assertEqual(wf.getnchannels(), 1)
            self.assertEqual(wf.getframerate(), 48000)
            self.assertGreater(wf.getnframes(), 0)

    def test_http_get_speak_valid_wav(self):
        main.tts_engine = FakeTtsEngine(sample_rate=48000, voices=["maichi"])
        resp = self.client.get("/speak", params={"text": "Kiểm thử phát âm", "voice": "maichi"})
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(resp.headers.get("content-type"), "audio/wav")
        body = resp.content
        self.assertTrue(body.startswith(b"RIFF"))

    def test_http_invalid_voice_returns_400(self):
        main.tts_engine = FakeTtsEngine(sample_rate=48000, voices=["maichi"])
        resp = self.client.get("/speak", params={"text": "Kiểm thử", "voice": "non_existent_voice"})
        self.assertEqual(resp.status_code, 400)
        self.assertIn("Unsupported voice", resp.json()["detail"])

    def test_http_empty_text_returns_400(self):
        main.tts_engine = FakeTtsEngine(sample_rate=48000, voices=["maichi"])
        resp = self.client.post("/synthesize", json={"text": "   ", "speaker": "maichi"})
        self.assertEqual(resp.status_code, 400)
        self.assertIn("cannot be empty", resp.json()["detail"])


if __name__ == "__main__":
    unittest.main()
