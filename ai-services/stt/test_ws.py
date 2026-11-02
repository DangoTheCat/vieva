"""Contract tests for the STT WebSocket (/ws/transcribe).

The Whisper model is stubbed, so the tests run without a GPU or model download:
    pip install fastapi httpx pytest numpy
    pytest -q test_ws.py

They pin the message shapes the frontend hook (FE/src/hooks/useVoiceSTT.js) relies on:
partial/final carry the transcript under "text", and commit/eos/stop each produce one final.
"""
import json
import sys
import types

import numpy as np
import pytest

# ---- stub heavy dependencies before importing main ---------------------------------
torch_stub = types.ModuleType("torch")
torch_stub.cuda = types.SimpleNamespace(is_available=lambda: False)
sys.modules.setdefault("torch", torch_stub)

fw_stub = types.ModuleType("faster_whisper")


class _FakeSegment:
    def __init__(self, text):
        self.text = text


class FakeWhisperModel:
    last_language = None
    prompts = []

    def __init__(self, *args, **kwargs):
        self.hf_tokenizer = types.SimpleNamespace(encode=lambda text, **kw: types.SimpleNamespace(ids=list(text.encode())))

    def transcribe(self, data, language="vi", **kwargs):
        FakeWhisperModel.prompts.append(kwargs.get("initial_prompt"))
        FakeWhisperModel.last_language = language
        return [_FakeSegment(f"{language}:{int(data.size)}")], None


fw_stub.WhisperModel = FakeWhisperModel
# Pretend every byte of audio decodes to one sample, so the transcript reveals the buffer size.
fw_stub.decode_audio = lambda stream, sampling_rate=16000: np.zeros(len(stream.getvalue()), dtype=np.float32)
sys.modules.setdefault("faster_whisper", fw_stub)

import main  # noqa: E402
from fastapi.testclient import TestClient  # noqa: E402


@pytest.fixture()
def client(monkeypatch):
    monkeypatch.setattr(main, "PARTIAL_INTERVAL_SECONDS", 0.0)
    monkeypatch.setattr(main, "MIN_PARTIAL_BYTES", 1)
    with TestClient(main.app) as c:
        yield c


def receive(ws):
    return json.loads(ws.receive_text())


def test_partial_and_final_use_text_key(client):
    with client.websocket_connect("/ws/transcribe?lang=vi") as ws:
        ws.send_bytes(b"\x00" * 100)
        partial = receive(ws)
        assert partial == {"type": "partial", "text": "vi:100"}

        ws.send_text(json.dumps({"type": "commit"}))
        final = receive(ws)
        assert final == {"type": "final", "text": "vi:100"}


def test_commit_clears_buffer_for_next_answer(client):
    with client.websocket_connect("/ws/transcribe") as ws:
        ws.send_bytes(b"\x00" * 60)
        receive(ws)  # partial
        ws.send_text(json.dumps({"type": "commit"}))
        assert receive(ws)["text"] == "vi:60"

        ws.send_bytes(b"\x00" * 50)
        receive(ws)  # partial of the second answer only
        ws.send_text(json.dumps({"type": "eos"}))  # legacy alias still accepted
        assert receive(ws) == {"type": "final", "text": "vi:50"}


@pytest.mark.parametrize("lang,expected", [("en", "en"), ("en-US", "en"), ("vi-VN", "vi"), ("fr", "vi")])
def test_language_query_parameter(client, lang, expected):
    with client.websocket_connect(f"/ws/transcribe?lang={lang}") as ws:
        ws.send_bytes(b"\x00" * 100)
        assert receive(ws)["text"] == f"{expected}:100"


def test_stop_sends_final_then_closes(client):
    with client.websocket_connect("/ws/transcribe") as ws:
        ws.send_bytes(b"\x00" * 100)
        receive(ws)
        ws.send_text(json.dumps({"type": "stop"}))
        assert receive(ws)["type"] == "final"


def test_unknown_message_returns_error(client):
    with client.websocket_connect("/ws/transcribe") as ws:
        ws.send_text(json.dumps({"type": "dance"}))
        err = receive(ws)
        assert err["type"] == "error" and err["code"] == "UNKNOWN_TYPE"


def test_context_applies_to_partial_and_final_and_is_isolated(client, monkeypatch):
    monkeypatch.setattr(main, "load_attempt_context", lambda attempt, token: "API, Docker" if attempt == "one" else "")
    FakeWhisperModel.prompts.clear()
    with client.websocket_connect("/ws/transcribe?attemptId=one&token=token&lang=vi") as ws:
        ws.send_bytes(b"x" * 100)
        receive(ws)
        ws.send_text('{"type":"commit"}')
        receive(ws)
    assert len(FakeWhisperModel.prompts) == 2
    assert FakeWhisperModel.prompts[0] == FakeWhisperModel.prompts[1]
    assert FakeWhisperModel.prompts[0]
    with client.websocket_connect("/ws/transcribe?attemptId=two&token=token") as ws:
        ws.send_bytes(b"x" * 100)
        receive(ws)
    assert FakeWhisperModel.prompts[-1] is None


def test_prompt_budget(client):
    assert len(main.prompt_tokens("Docker " * 1000)) == 200
    assert main.prompt_tokens("") is None


def test_context_lookup_fails_open(monkeypatch):
    monkeypatch.setenv("STT_CONTEXT_SECRET", "test")
    monkeypatch.setattr(main.urllib.request, "urlopen", lambda *a, **kw: (_ for _ in ()).throw(TimeoutError()))
    assert main.load_attempt_context("00000000-0000-0000-0000-000000000001", "token") == ""
    assert main.load_attempt_context("bad-id", "token") == ""
