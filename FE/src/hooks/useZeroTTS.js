import { useState, useRef, useCallback, useEffect } from 'react';

/**
 * useZeroTTS Hook
 * Connects to http://localhost:8001/synthesize or directly plays audio from ttsAudioUrl.
 * Falls back gracefully to Web SpeechSynthesis API if zero-shot TTS service is offline.
 * 
 * Exposes:
 *   isPlaying, play(textOrUrl), stop()
 */
export function useZeroTTS({
  ttsBaseUrl = 'http://localhost:8001',
  defaultVoiceId = 'lecturer_female_1',
  onError
} = {}) {
  const [isPlaying, setIsPlaying] = useState(false);
  const audioRef = useRef(null);
  const abortControllerRef = useRef(null);

  // Stop playback immediately
  const stop = useCallback(() => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
      abortControllerRef.current = null;
    }

    if (audioRef.current) {
      try {
        audioRef.current.pause();
        audioRef.current.currentTime = 0;
      } catch (e) {
        console.warn('[useZeroTTS] Error pausing audio:', e);
      }
      audioRef.current = null;
    }

    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
    }

    setIsPlaying(false);
  }, []);

  // Web SpeechSynthesis fallback
  const playFallbackSpeech = useCallback((text) => {
    if (!('speechSynthesis' in window)) {
      setIsPlaying(false);
      return;
    }
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = 'vi-VN';
    utterance.rate = 1.0;

    utterance.onstart = () => setIsPlaying(true);
    utterance.onend = () => setIsPlaying(false);
    utterance.onerror = () => setIsPlaying(false);

    window.speechSynthesis.speak(utterance);
  }, []);

  // Play audio from URL or synthesized text
  const play = useCallback(async (textOrUrl) => {
    if (!textOrUrl || typeof textOrUrl !== 'string') return;
    stop();

    const isDirectUrl = textOrUrl.startsWith('http://') || 
                        textOrUrl.startsWith('https://') || 
                        textOrUrl.startsWith('blob:') || 
                        textOrUrl.startsWith('/api/');

    if (isDirectUrl) {
      try {
        const audio = new Audio(textOrUrl);
        audioRef.current = audio;
        audio.onended = () => setIsPlaying(false);
        audio.onerror = () => {
          setIsPlaying(false);
          if (onError) onError(new Error('Audio playback error'));
        };
        await audio.play();
        setIsPlaying(true);
        return;
      } catch (err) {
        console.warn('[useZeroTTS] Error playing direct audio URL:', err);
        setIsPlaying(false);
        if (onError) onError(err);
        return;
      }
    }

    // Synthesize text via ZeroTTS API
    abortControllerRef.current = new AbortController();
    try {
      setIsPlaying(true);
      const response = await fetch(`${ttsBaseUrl}/synthesize`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          text: textOrUrl,
          voice_id: defaultVoiceId,
          speed: 1.0
        }),
        signal: abortControllerRef.current.signal
      });

      if (!response.ok) {
        throw new Error(`TTS service returned HTTP ${response.status}`);
      }

      // Check whether response is audio binary or JSON containing audio_url/base64
      const contentType = response.headers.get('content-type') || '';
      let audioBlobUrl = '';

      if (contentType.includes('application/json')) {
        const data = await response.json();
        if (data.audio_url) {
          audioBlobUrl = data.audio_url;
        } else if (data.audio_base64) {
          audioBlobUrl = `data:audio/wav;base64,${data.audio_base64}`;
        } else {
          throw new Error('TTS response missing audio data');
        }
      } else {
        const blob = await response.blob();
        audioBlobUrl = URL.createObjectURL(blob);
      }

      const audio = new Audio(audioBlobUrl);
      audioRef.current = audio;
      audio.onended = () => {
        setIsPlaying(false);
        if (audioBlobUrl.startsWith('blob:')) {
          URL.revokeObjectURL(audioBlobUrl);
        }
      };
      audio.onerror = () => {
        setIsPlaying(false);
        if (audioBlobUrl.startsWith('blob:')) {
          URL.revokeObjectURL(audioBlobUrl);
        }
      };
      await audio.play();
    } catch (err) {
      if (err.name === 'AbortError') return;
      console.warn('[useZeroTTS] Synthesize service failed or offline, falling back to Web SpeechSynthesis:', err);
      // Fallback
      playFallbackSpeech(textOrUrl);
    }
  }, [stop, ttsBaseUrl, defaultVoiceId, onError, playFallbackSpeech]);

  // Clean up on unmount
  useEffect(() => {
    return () => {
      stop();
    };
  }, [stop]);

  return {
    isPlaying,
    play,
    stop
  };
}
