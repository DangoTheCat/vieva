import { useState, useRef, useEffect, useCallback } from 'react';
import { getAuthToken, getSttWsUrl, toSttLang } from '../utils/speechConfig';

const FINAL_TIMEOUT_MS = 5000;

/**
 * useVoiceSTT — streams microphone audio to the STT service over WebSocket.
 *
 * Contract (ai-services/stt/main.py, covered by ai-services/stt/test_ws.py):
 *   client -> server  binary webm/opus chunks (250 ms), then {"type":"commit"} at the end of an answer
 *   server -> client  {"type":"partial","text"} and {"type":"final","text"}; {"type":"error",...}
 *   URL ?lang=vi|en follows the admin speech settings.
 *
 * stopRecording() resolves with { transcript, audioBlob } once the server's "final" arrives
 * (or after 5 s, with what is known), so the answer is never submitted half-transcribed.
 * An STT failure is reported through onError; it never ends the exam session by itself.
 */
export function useVoiceSTT({
  wsUrl,
  language = 'vi-VN',
  onFinalTranscript,
  onPartialTranscript,
  onError
} = {}) {
  const [isRecording, setIsRecording] = useState(false);
  const [isConnected, setIsConnected] = useState(false);
  const [transcript, setTranscript] = useState('');
  const [partialTranscript, setPartialTranscript] = useState('');
  const [audioBlob, setAudioBlob] = useState(null);

  const socketRef = useRef(null);
  const mediaRecorderRef = useRef(null);
  const streamRef = useRef(null);
  const recordedChunksRef = useRef([]);
  const sendQueueRef = useRef(Promise.resolve());
  const transcriptRef = useRef('');
  const finalWaiterRef = useRef(null);
  const stopPromiseRef = useRef(null);
  const callbacksRef = useRef({ onFinalTranscript, onPartialTranscript, onError });
  callbacksRef.current = { onFinalTranscript, onPartialTranscript, onError };

  const baseWsUrl = wsUrl || getSttWsUrl();
  const token = getAuthToken();
  const url = `${baseWsUrl}${baseWsUrl.includes('?') ? '&' : '?'}lang=${toSttLang(language)}`
    + (token ? `&token=${encodeURIComponent(token)}` : '');

  const settleFinal = useCallback((text) => {
    const waiter = finalWaiterRef.current;
    if (waiter) {
      finalWaiterRef.current = null;
      clearTimeout(waiter.timer);
      waiter.resolve(text);
    }
  }, []);

  const closeSocket = useCallback(() => {
    const ws = socketRef.current;
    socketRef.current = null;
    if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
      try { ws.close(); } catch (e) { /* already closing */ }
    }
    setIsConnected(false);
  }, []);

  const connectSocket = useCallback(() => new Promise((resolve, reject) => {
    const current = socketRef.current;
    if (current && current.readyState === WebSocket.OPEN && current.url === url) {
      resolve(current);
      return;
    }
    if (current) closeSocket();

    const ws = new WebSocket(url);
    ws.binaryType = 'arraybuffer';
    socketRef.current = ws;

    ws.onopen = () => {
      setIsConnected(true);
      resolve(ws);
    };
    ws.onmessage = (event) => {
      let data;
      try { data = JSON.parse(event.data); } catch (e) { return; }
      const text = (data.text ?? data.transcript ?? '').trim();
      if (data.type === 'partial') {
        setPartialTranscript(text);
        callbacksRef.current.onPartialTranscript?.(text);
      } else if (data.type === 'final') {
        const updated = [transcriptRef.current, text].filter(Boolean).join(' ').trim();
        transcriptRef.current = updated;
        setTranscript(updated);
        setPartialTranscript('');
        callbacksRef.current.onFinalTranscript?.(updated);
        settleFinal(updated);
      } else if (data.type === 'error') {
        callbacksRef.current.onError?.(new Error(data.message || 'STT error'));
      }
    };
    ws.onerror = (err) => {
      setIsConnected(false);
      callbacksRef.current.onError?.(err);
      settleFinal(transcriptRef.current);
      reject(err);
    };
    ws.onclose = () => {
      setIsConnected(false);
      settleFinal(transcriptRef.current);
      if (socketRef.current === ws) socketRef.current = null;
    };
  }), [url, closeSocket, settleFinal]);

  const resetTranscript = useCallback(() => {
    transcriptRef.current = '';
    setTranscript('');
    setPartialTranscript('');
    setAudioBlob(null);
    recordedChunksRef.current = [];
  }, []);

  /** Starts (or continues) recording the current answer. Earlier segments of the same answer are kept. */
  const startRecording = useCallback(async () => {
    if (mediaRecorderRef.current && mediaRecorderRef.current.state === 'recording') return;
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: { channelCount: 1, sampleRate: 16000, echoCancellation: true, noiseSuppression: true }
      });
      streamRef.current = stream;

      try {
        await connectSocket();
      } catch (wsErr) {
        // Recording still works; the audio is uploaded with the answer and the lecturer can review it.
        console.warn('[useVoiceSTT] STT service unreachable, recording audio only:', wsErr);
      }

      const mimeTypes = ['audio/webm;codecs=opus', 'audio/webm', 'audio/ogg;codecs=opus', 'audio/mp4'];
      const mimeType = mimeTypes.find((type) => window.MediaRecorder && MediaRecorder.isTypeSupported(type)) || '';
      const recorder = new MediaRecorder(stream, mimeType ? { mimeType } : undefined);
      mediaRecorderRef.current = recorder;

      recorder.ondataavailable = (e) => {
        if (!e.data || e.data.size === 0) return;
        recordedChunksRef.current.push(e.data);
        const chunk = e.data;
        // Keep chunks in order: arrayBuffer() is async
        sendQueueRef.current = sendQueueRef.current.then(async () => {
          const ws = socketRef.current;
          if (ws && ws.readyState === WebSocket.OPEN) {
            ws.send(await chunk.arrayBuffer());
          }
        }).catch((err) => console.warn('[useVoiceSTT] Error sending audio chunk:', err));
      };

      recorder.onstop = () => {
        const blob = new Blob(recordedChunksRef.current, { type: mimeType || 'audio/webm' });
        setAudioBlob(blob);
        setIsRecording(false);
        const resolveStop = stopPromiseRef.current;
        stopPromiseRef.current = null;
        const finish = (text) => resolveStop?.({ transcript: text, audioBlob: blob });
        sendQueueRef.current.then(() => {
          const ws = socketRef.current;
          if (!ws || ws.readyState !== WebSocket.OPEN) {
            finish(transcriptRef.current);
            return;
          }
          const timer = setTimeout(() => settleFinal(transcriptRef.current), FINAL_TIMEOUT_MS);
          finalWaiterRef.current = { resolve: finish, timer };
          ws.send(JSON.stringify({ type: 'commit' }));
        });
      };

      recorder.start(250);
      setIsRecording(true);
    } catch (err) {
      console.error('[useVoiceSTT] Cannot start recording:', err);
      setIsRecording(false);
      callbacksRef.current.onError?.(err);
      throw err;
    }
  }, [connectSocket, settleFinal]);

  /** Stops recording; resolves with { transcript, audioBlob } for the answer so far. */
  const stopRecording = useCallback(() => {
    const recorder = mediaRecorderRef.current;
    const current = () => ({
      transcript: transcriptRef.current,
      audioBlob: recordedChunksRef.current.length
        ? new Blob(recordedChunksRef.current, { type: recorder?.mimeType || 'audio/webm' })
        : null
    });
    const promise = new Promise((resolve) => {
      if (recorder && recorder.state !== 'inactive') {
        stopPromiseRef.current = resolve;
        try { recorder.stop(); } catch (e) { resolve(current()); }
      } else {
        resolve(current());
      }
    });
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((track) => track.stop());
      streamRef.current = null;
    }
    return promise;
  }, []);

  useEffect(() => () => {
    const recorder = mediaRecorderRef.current;
    if (recorder && recorder.state !== 'inactive') {
      try { recorder.stop(); } catch (e) { /* ignore */ }
    }
    streamRef.current?.getTracks().forEach((track) => track.stop());
    closeSocket();
  }, [closeSocket]);

  return {
    isRecording,
    isConnected,
    transcript,
    partialTranscript,
    audioBlob,
    startRecording,
    stopRecording,
    resetTranscript
  };
}
