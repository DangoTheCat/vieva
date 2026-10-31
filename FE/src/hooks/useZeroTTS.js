import { useState, useRef, useCallback, useEffect } from 'react';
import { getTtsBaseUrl } from '../utils/speechConfig';

/**
 * useZeroTTS — reads text aloud through the TTS service (POST {ttsBaseUrl}/synthesize),
 * or plays an audio URL directly. Falls back to the browser's SpeechSynthesis when the
 * service is offline.
 *
 * play() returns a promise that resolves when playback ends, fails or is stopped, so the
 * caller can start the answer timer only after the question has been read.
 * Voice, language and speed follow the admin speech settings passed in by the caller.
 */
export function useZeroTTS({
  ttsBaseUrl,
  voiceId,
  language = 'vi-VN',
  speed = 1.0,
  onError
} = {}) {
  const [isPlaying, setIsPlaying] = useState(false);
  const audioRef = useRef(null);
  const abortControllerRef = useRef(null);
  const doneRef = useRef(null);
  const baseUrl = ttsBaseUrl || getTtsBaseUrl();

  const finishPlayback = useCallback(() => {
    setIsPlaying(false);
    const done = doneRef.current;
    doneRef.current = null;
    done?.();
  }, []);

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
    finishPlayback();
  }, [finishPlayback]);

  const playAudioUrl = useCallback(async (url, revoke) => {
    const audio = new Audio(url);
    audioRef.current = audio;
    const cleanup = () => {
      if (revoke && url.startsWith('blob:')) URL.revokeObjectURL(url);
      finishPlayback();
    };
    audio.onended = cleanup;
    audio.onerror = () => {
      cleanup();
      onError?.(new Error('Audio playback error'));
    };
    setIsPlaying(true);
    await audio.play();
  }, [finishPlayback, onError]);

  const playFallbackSpeech = useCallback((text) => {
    if (!('speechSynthesis' in window)) {
      finishPlayback();
      return;
    }
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = language || 'vi-VN';
    utterance.rate = Number(speed) || 1.0;
    utterance.onstart = () => setIsPlaying(true);
    utterance.onend = finishPlayback;
    utterance.onerror = finishPlayback;
    window.speechSynthesis.speak(utterance);
  }, [language, speed, finishPlayback]);

  /** Reads text (or plays a URL). Resolves when playback is over. */
  const play = useCallback((textOrUrl) => {
    if (!textOrUrl || typeof textOrUrl !== 'string') return Promise.resolve();
    stop();
    const done = new Promise((resolve) => { doneRef.current = resolve; });

    const isDirectUrl = /^(https?:|blob:|\/api\/)/.test(textOrUrl);
    if (isDirectUrl) {
      playAudioUrl(textOrUrl, false).catch((err) => {
        console.warn('[useZeroTTS] Error playing audio URL:', err);
        onError?.(err);
        finishPlayback();
      });
      return done;
    }

    abortControllerRef.current = new AbortController();
    setIsPlaying(true);
    fetch(`${baseUrl}/synthesize`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      // ai-services/tts SynthesizeRequest: { text, speaker }
      body: JSON.stringify(voiceId ? { text: textOrUrl, speaker: voiceId } : { text: textOrUrl }),
      signal: abortControllerRef.current.signal
    })
      .then(async (response) => {
        if (!response.ok) throw new Error(`TTS service returned HTTP ${response.status}`);
        const contentType = response.headers.get('content-type') || '';
        if (contentType.includes('application/json')) {
          const data = await response.json();
          if (data.audio_url) return playAudioUrl(data.audio_url, false);
          if (data.audio_base64) return playAudioUrl(`data:audio/wav;base64,${data.audio_base64}`, false);
          throw new Error('TTS response missing audio data');
        }
        const blob = await response.blob();
        return playAudioUrl(URL.createObjectURL(blob), true);
      })
      .catch((err) => {
        if (err.name === 'AbortError') return;
        console.warn('[useZeroTTS] TTS service failed or offline, using browser speech:', err);
        playFallbackSpeech(textOrUrl);
      });
    return done;
  }, [stop, baseUrl, voiceId, language, speed, onError, playAudioUrl, playFallbackSpeech, finishPlayback]);

  useEffect(() => () => stop(), [stop]);

  return { isPlaying, play, stop };
}
