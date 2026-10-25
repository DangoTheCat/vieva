import io
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import wave

BASE_URL = os.getenv("TTS_BASE_URL", "http://127.0.0.1:8001")
MAX_WAIT_SECONDS = 600  # Up to 10 minutes for model download and load


def fetch_json(url: str, method: str = "GET", data: dict = None, expected_status: int = 200):
    headers = {"Content-Type": "application/json"} if data is not None else {}
    body = json.dumps(data).encode("utf-8") if data is not None else None
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            status = resp.status
            content = resp.read()
            return status, json.loads(content.decode("utf-8"))
    except urllib.error.HTTPError as e:
        content = e.read()
        try:
            err_json = json.loads(content.decode("utf-8"))
        except Exception:
            err_json = {"detail": content.decode("utf-8", errors="replace")}
        return e.code, err_json


def fetch_raw(url: str, method: str = "GET", data: dict = None):
    headers = {"Content-Type": "application/json"} if data is not None else {}
    body = json.dumps(data).encode("utf-8") if data is not None else None
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    with urllib.request.urlopen(req, timeout=120) as resp:
        return resp.status, resp.headers.get_content_type(), resp.read()


def inspect_wav(wav_bytes: bytes):
    if not wav_bytes.startswith(b"RIFF"):
        raise ValueError(f"Invalid WAV header: {wav_bytes[:16]}")
    with wave.open(io.BytesIO(wav_bytes), "rb") as wf:
        channels = wf.getnchannels()
        sampwidth = wf.getsampwidth()
        framerate = wf.getframerate()
        nframes = wf.getnframes()
        duration = nframes / float(framerate) if framerate > 0 else 0.0
        return {
            "channels": channels,
            "sampwidth": sampwidth,
            "framerate": framerate,
            "nframes": nframes,
            "duration_sec": round(duration, 3),
            "byte_length": len(wav_bytes),
        }


def main():
    print(f"=== ZeroTTS Live Smoke Test starting against {BASE_URL} ===")
    start_time = time.time()
    health_url = f"{BASE_URL}/health"
    engine_ready = False
    last_info = None

    print(f"Polling {health_url} for engine readiness (timeout: {MAX_WAIT_SECONDS}s)...")
    while time.time() - start_time < MAX_WAIT_SECONDS:
        try:
            status, info = fetch_json(health_url)
            last_info = info
            if status == 200 and info.get("engine_ready") is True:
                engine_ready = True
                print(f"[OK] TTS Engine ready after {round(time.time() - start_time, 1)}s: {info}")
                break
            else:
                print(f"  [Waiting] Service responded with: status={status}, ready={info.get('engine_ready')}, elapsed={round(time.time() - start_time, 1)}s")
        except urllib.error.URLError as e:
            print(f"  [Waiting] Connection refused / connecting ({e.reason}), elapsed={round(time.time() - start_time, 1)}s")
        time.sleep(5)

    if not engine_ready:
        print(f"[FAIL] Engine failed to become ready within {MAX_WAIT_SECONDS}s. Last health: {last_info}")
        sys.exit(1)

    # 1. Verify GET /voices
    voices_url = f"{BASE_URL}/voices"
    v_status, v_data = fetch_json(voices_url)
    print(f"1. GET /voices: status={v_status}, count={v_data.get('count')}, voices={v_data.get('voices')}")
    assert v_status == 200, f"Expected 200 from /voices, got {v_status}"
    voices = v_data.get("voices", [])
    assert len(voices) > 0, "No voices available in ready engine!"
    default_voice = "maichi" if "maichi" in voices else voices[0]
    print(f"   Selected voice for synthesis: {default_voice}")

    # 2. Test POST /synthesize
    synth_url = f"{BASE_URL}/synthesize"
    post_payload = {
        "text": "Xin chào, đây là bản thử nghiệm thực tế của ZeroTTS trên Docker.",
        "speaker": default_voice,
    }
    print(f"2. Testing POST /synthesize with payload: {post_payload}")
    p_start = time.time()
    p_status, p_mime, p_bytes = fetch_raw(synth_url, method="POST", data=post_payload)
    p_elapsed = time.time() - p_start
    print(f"   POST /synthesize completed in {round(p_elapsed, 2)}s, status={p_status}, content_type={p_mime}, bytes={len(p_bytes)}")
    assert p_status == 200, f"Expected 200 from POST /synthesize, got {p_status}"
    assert "audio/wav" in p_mime, f"Expected audio/wav mime type, got {p_mime}"
    wav_info_post = inspect_wav(p_bytes)
    print(f"   WAV Inspection (POST): {wav_info_post}")
    assert wav_info_post["nframes"] > 0, "Zero audio frames synthesized!"
    assert wav_info_post["duration_sec"] > 0.5, f"Audio duration suspiciously short: {wav_info_post['duration_sec']}s"

    # Save output to review-output/zerotts-live.wav
    out_dir = "/app/review-output"
    os.makedirs(out_dir, exist_ok=True)
    out_file = os.path.join(out_dir, "zerotts-live.wav")
    with open(out_file, "wb") as f:
        f.write(p_bytes)
    print(f"   Saved live synthesis sample to: {out_file} ({len(p_bytes)} bytes)")

    # Also save to local review-output if mounted or available
    local_out_dir = "ai-services/tts/review-output"
    if os.path.isdir(local_out_dir):
        with open(os.path.join(local_out_dir, "zerotts-live.wav"), "wb") as f:
            f.write(p_bytes)

    # 3. Test GET /speak
    speak_url = f"{BASE_URL}/speak?text={urllib.parse.quote('Kiểm thử phát âm tiếng Việt thành công.')}&voice={default_voice}"
    print(f"3. Testing GET /speak: {speak_url}")
    g_start = time.time()
    g_status, g_mime, g_bytes = fetch_raw(speak_url, method="GET")
    g_elapsed = time.time() - g_start
    print(f"   GET /speak completed in {round(g_elapsed, 2)}s, status={g_status}, content_type={g_mime}, bytes={len(g_bytes)}")
    assert g_status == 200, f"Expected 200 from GET /speak, got {g_status}"
    assert "audio/wav" in g_mime, f"Expected audio/wav mime type, got {g_mime}"
    wav_info_get = inspect_wav(g_bytes)
    print(f"   WAV Inspection (GET): {wav_info_get}")
    assert wav_info_get["nframes"] > 0, "Zero audio frames synthesized on GET /speak!"

    # 4. Test invalid voice
    invalid_url = f"{BASE_URL}/speak?text=Kiem+thu&voice=non_existent_voice_404"
    print(f"4. Testing invalid voice: {invalid_url}")
    inv_status, inv_data = fetch_json(invalid_url)
    print(f"   Invalid voice response: status={inv_status}, body={inv_data}")
    assert inv_status == 400, f"Expected 400 for invalid voice, got {inv_status}"
    assert "Unsupported voice" in inv_data.get("detail", ""), f"Unexpected detail: {inv_data}"

    print("\n=== LIVE SMOKE TEST SUMMARY ===")
    print(f"Result: PASS")
    print(f"Model ID: {last_info.get('model')}")
    print(f"Device: {last_info.get('device')} (cuda_available={last_info.get('cuda_available')})")
    print(f"Sample Rate: {wav_info_post['framerate']} Hz")
    print(f"Sample Channels: {wav_info_post['channels']}")
    print(f"Sample Duration: {wav_info_post['duration_sec']} seconds")
    print(f"Sample File: {out_file}")
    print(f"Voice count: {len(voices)} ({', '.join(voices[:4])}...)")
    print("All live assertions succeeded!\n")


if __name__ == "__main__":
    main()
