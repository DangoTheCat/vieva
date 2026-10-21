import { useState, useRef, useEffect, useCallback } from 'react';

/**
 * useVoiceSTT Hook
 * Direct WebSocket STT hook connecting to ws://localhost:8002/ws/transcribe (Phương án A)
 * Uses MediaRecorder with 250ms audio slices or AudioContext, sending chunks over WebSocket.
 * Listens for:
 *   - { "type": "partial", "text": "..." }
 *   - { "type": "final", "text": "..." }
 * 
 * Exposes:
 *   isRecording, transcript, partialTranscript, audioBlob, startRecording, stopRecording, resetTranscript, isConnected
 */
export function useVoiceSTT({
  wsUrl = 'ws://localhost:8002/ws/transcribe',
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

  // Cleanly close WebSocket
  const closeSocket = useCallback(() => {
    if (socketRef.current) {
      try {
        if (socketRef.current.readyState === WebSocket.OPEN || socketRef.current.readyState === WebSocket.CONNECTING) {
          socketRef.current.close();
        }
      } catch (err) {
        console.warn('[useVoiceSTT] Error closing socket:', err);
      }
      socketRef.current = null;
      setIsConnected(false);
    }
  }, []);

  // Connect WebSocket
  const connectSocket = useCallback(() => {
    return new Promise((resolve, reject) => {
      try {
        if (socketRef.current && socketRef.current.readyState === WebSocket.OPEN) {
          resolve(socketRef.current);
          return;
        }

        const ws = new WebSocket(wsUrl);
        socketRef.current = ws;

        ws.onopen = () => {
          setIsConnected(true);
          resolve(ws);
        };

        ws.onmessage = (event) => {
          try {
            const data = JSON.parse(event.data);
            if (data.type === 'partial') {
              const text = data.text || '';
              setPartialTranscript(text);
              if (onPartialTranscript) onPartialTranscript(text);
            } else if (data.type === 'final') {
              const text = data.text || '';
              setTranscript((prev) => {
                const updated = prev ? `${prev} ${text}`.trim() : text.trim();
                if (onFinalTranscript) onFinalTranscript(updated);
                return updated;
              });
              setPartialTranscript('');
            }
          } catch (e) {
            // If raw text instead of JSON
            if (typeof event.data === 'string') {
              setPartialTranscript(event.data);
            }
          }
        };

        ws.onerror = (err) => {
          console.warn('[useVoiceSTT] WebSocket error:', err);
          setIsConnected(false);
          if (onError) onError(err);
          reject(err);
        };

        ws.onclose = () => {
          setIsConnected(false);
        };
      } catch (err) {
        console.warn('[useVoiceSTT] Failed to initialize WebSocket:', err);
        setIsConnected(false);
        reject(err);
      }
    });
  }, [wsUrl, onFinalTranscript, onPartialTranscript, onError]);

  const resetTranscript = useCallback(() => {
    setTranscript('');
    setPartialTranscript('');
    setAudioBlob(null);
  }, []);

  // Start recording
  const startRecording = useCallback(async () => {
    resetTranscript();
    recordedChunksRef.current = [];

    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: {
          channelCount: 1,
          sampleRate: 16000,
          echoCancellation: true,
          noiseSuppression: true
        }
      });
      streamRef.current = stream;

      // Try connecting to WS STT server
      let wsInstance = null;
      try {
        wsInstance = await connectSocket();
      } catch (wsErr) {
        console.warn('[useVoiceSTT] STT server not reachable, fallback to offline audio recording:', wsErr);
      }

      // Pick supported mime type
      const mimeTypes = ['audio/webm;codecs=opus', 'audio/webm', 'audio/ogg;codecs=opus', 'audio/mp4'];
      const supportedMimeType = mimeTypes.find((type) => MediaRecorder.isTypeSupported(type)) || '';

      const recorder = new MediaRecorder(stream, supportedMimeType ? { mimeType: supportedMimeType } : undefined);
      mediaRecorderRef.current = recorder;

      recorder.ondataavailable = async (e) => {
        if (e.data && e.data.size > 0) {
          recordedChunksRef.current.push(e.data);
          // If WS is open, send binary slice (Phương án A: chunks 250ms)
          if (socketRef.current && socketRef.current.readyState === WebSocket.OPEN) {
            try {
              const arrayBuffer = await e.data.arrayBuffer();
              socketRef.current.send(arrayBuffer);
            } catch (sendErr) {
              console.warn('[useVoiceSTT] Error sending audio chunk:', sendErr);
            }
          }
        }
      };

      recorder.onstop = () => {
        const fullBlob = new Blob(recordedChunksRef.current, {
          type: supportedMimeType || 'audio/webm'
        });
        setAudioBlob(fullBlob);
        setIsRecording(false);
      };

      // Slice audio every 250ms
      recorder.start(250);
      setIsRecording(true);
    } catch (err) {
      console.error('[useVoiceSTT] Cannot start recording:', err);
      setIsRecording(false);
      if (onError) onError(err);
      throw err;
    }
  }, [connectSocket, resetTranscript, onError]);

  // Stop recording
  const stopRecording = useCallback(() => {
    if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
      try {
        mediaRecorderRef.current.stop();
      } catch (e) {
        console.warn('[useVoiceSTT] Error stopping MediaRecorder:', e);
      }
    }

    if (streamRef.current) {
      try {
        streamRef.current.getTracks().forEach((track) => track.stop());
      } catch (e) {
        console.warn('[useVoiceSTT] Error stopping tracks:', e);
      }
      streamRef.current = null;
    }

    // Tell STT server stream ended
    if (socketRef.current && socketRef.current.readyState === WebSocket.OPEN) {
      try {
        socketRef.current.send(JSON.stringify({ type: 'eos' }));
      } catch (e) {}
    }

    setIsRecording(false);
  }, []);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      stopRecording();
      closeSocket();
    };
  }, [stopRecording, closeSocket]);

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
