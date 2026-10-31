/**
 * Speech service endpoints and language helpers shared by the STT/TTS hooks.
 *
 * URLs come from Vite env vars so the same build works locally and behind nginx:
 *   VITE_STT_WS_URL  e.g. ws://localhost:8002/ws/transcribe  or  /ws/stt  (relative = same host)
 *   VITE_TTS_URL     e.g. http://localhost:8001             or  /tts
 */
const env = (typeof import.meta !== 'undefined' && import.meta.env) || {};

const toWsUrl = (value) => {
  if (!value) return null;
  if (value.startsWith('ws://') || value.startsWith('wss://')) return value;
  if (value.startsWith('/')) {
    const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    return `${proto}//${window.location.host}${value}`;
  }
  return value;
};

export const getSttWsUrl = () =>
  toWsUrl(env.VITE_STT_WS_URL) || `ws://${window.location.hostname || 'localhost'}:8002/ws/transcribe`;

export const getTtsBaseUrl = () =>
  env.VITE_TTS_URL || `http://${window.location.hostname || 'localhost'}:8001`;

/** 'vi-VN' -> 'vi', 'en-US' -> 'en' (what the STT service expects in ?lang=). */
export const toSttLang = (locale) => (String(locale || 'vi-VN').toLowerCase().startsWith('en') ? 'en' : 'vi');

/** Picks the configured TTS voice for the active TTS language. */
export const pickVoice = (settings) => {
  if (!settings) return undefined;
  const isEnglish = String(settings.ttsLanguage || '').toLowerCase().startsWith('en');
  return (isEnglish ? settings.englishVoice : settings.vietnameseVoice) || settings.voice || undefined;
};
