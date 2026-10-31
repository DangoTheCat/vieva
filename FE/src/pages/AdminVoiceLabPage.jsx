import React, { useState, useEffect } from 'react';
import { speechApi } from '../api/speechApi';
import { getErrorMessage } from '../utils/errorCodes';
import { Volume2, Save, Loader2, Square, Mic, RotateCcw } from 'lucide-react';

const LOCALES = [
  { value: 'vi-VN', label: 'Tiếng Việt (vi-VN)' },
  { value: 'en-US', label: 'English (en-US)' }
];

const inputClass = 'w-full bg-white border border-pink-200 rounded-xl p-2.5 text-xs text-ink focus:outline-none focus:border-pink-500 focus:ring-1 focus:ring-pink-500 shadow-2xs';

const toForm = (s) => ({
  sttLanguage: s?.sttLanguage || 'vi-VN',
  ttsLanguage: s?.ttsLanguage || 'vi-VN',
  vietnameseVoice: s?.vietnameseVoice || '',
  englishVoice: s?.englishVoice || '',
  speed: s?.speed != null ? Number(s.speed) : 1,
  pitch: s?.pitch != null ? Number(s.pitch) : 1
});

/**
 * System speech settings (GET/PATCH /admin/speech) applied to every oral exam room.
 * The BE validates locales/voices against the configured TTS provider.
 */
export function AdminVoiceLabPage({ showToast }) {
  const [settings, setSettings] = useState(null);
  const [form, setForm] = useState(toForm(null));
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [isPreviewing, setIsPreviewing] = useState(false);
  const [previewText, setPreviewText] = useState('Xin chào, em hãy trình bày ngắn gọn ý tưởng chính của câu trả lời.');
  // WF07: a configuration is saved only after a successful test of exactly these values
  const [testResult, setTestResult] = useState(null);
  const [testedKey, setTestedKey] = useState(null);
  const audioRef = React.useRef(null);
  const formKey = JSON.stringify(form);
  const testPassed = testResult && testedKey === formKey && testResult.sttAvailable
    && (testResult.audioBase64 || testResult.browserVoice);

  const load = async () => {
    setIsLoading(true);
    try {
      const data = await speechApi.getAdminSettings();
      setSettings(data);
      setForm(toForm(data));
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    load();
    return () => window.speechSynthesis?.cancel();
  }, []);

  const handleSave = async (e) => {
    e.preventDefault();
    if (!testPassed) {
      showToast({ type: 'warning', message: 'Hãy bấm "Thử nghiệm" với cấu hình hiện tại và đảm bảo thử nghiệm thành công trước khi lưu.' });
      return;
    }
    setIsSaving(true);
    try {
      const updated = await speechApi.updateSettings({
        sttLanguage: form.sttLanguage,
        ttsLanguage: form.ttsLanguage,
        vietnameseVoice: form.vietnameseVoice.trim() || null,
        englishVoice: form.englishVoice.trim() || null,
        speed: Number(form.speed),
        pitch: Number(form.pitch)
      });
      setSettings(updated);
      setForm(toForm(updated));
      showToast({ type: 'success', message: 'Đã lưu cấu hình giọng nói cho phòng thi.' });
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setIsSaving(false);
    }
  };

  /** Tests the current values on the real services (POST /admin/speech/test) and plays the sample. */
  const handlePreview = async () => {
    setIsPreviewing(true);
    let result;
    try {
      const english = form.ttsLanguage.startsWith('en');
      result = await speechApi.testSettings({
        ttsLanguage: form.ttsLanguage,
        sttLanguage: form.sttLanguage,
        voice: (english ? form.englishVoice : form.vietnameseVoice).trim() || null,
        sampleText: previewText
      });
      setTestResult(result);
      setTestedKey(formKey);
    } catch (err) {
      setIsPreviewing(false);
      setTestResult(null);
      showToast({ type: 'error', message: getErrorMessage(err) });
      return;
    }

    if (result.audioBase64) {
      const audio = new Audio(`data:audio/wav;base64,${result.audioBase64}`);
      audioRef.current = audio;
      audio.onended = () => setIsPreviewing(false);
      audio.onerror = () => setIsPreviewing(false);
      audio.play().catch(() => setIsPreviewing(false));
      return;
    }
    // Language read by the student's browser (or engine offline): preview with the browser voice
    if (!('speechSynthesis' in window)) {
      setIsPreviewing(false);
      return;
    }
    window.speechSynthesis.cancel();
    const utter = new SpeechSynthesisUtterance(result.sampleText || previewText);
    utter.lang = form.ttsLanguage;
    utter.rate = Math.min(Number(form.speed) || 1, 10);
    utter.pitch = Math.min(Number(form.pitch) || 1, 2);
    utter.onend = () => setIsPreviewing(false);
    utter.onerror = () => setIsPreviewing(false);
    setIsPreviewing(true);
    window.speechSynthesis.speak(utter);
  };

  const stopPreview = () => {
    window.speechSynthesis?.cancel();
    audioRef.current?.pause();
    setIsPreviewing(false);
  };

  return (
    <div className="max-w-3xl mx-auto w-full space-y-6 pb-10 text-ink animate-modal-entry">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="font-heading font-bold text-2xl tracking-tight text-ink">Cấu Hình Giọng Nói</h1>
          <p className="text-xs text-slate-500 mt-1">Ngôn ngữ nhận diện (STT), giọng đọc (TTS), tốc độ và cao độ dùng trong phòng thi vấn đáp.</p>
        </div>
        <button 
          type="button" 
          onClick={load} 
          disabled={isLoading}
          className="px-3.5 py-1.5 rounded-xl bg-white border border-pink-200 text-xs font-semibold text-slate-700 flex items-center gap-1.5 hover:bg-slate-50 shadow-2xs transition disabled:opacity-50 cursor-pointer"
        >
          <RotateCcw className="w-3.5 h-3.5 text-slate-500" /> Tải lại
        </button>
      </header>

      {isLoading ? (
        <div className="h-72 rounded-2xl animate-pulse bg-slate-100 border border-pink-200" />
      ) : (
        <form onSubmit={handleSave} className="bg-white border border-pink-200 rounded-2xl p-6 shadow-2xs space-y-5 text-xs">
          <div className="grid sm:grid-cols-2 gap-4">
            <label className="block font-semibold text-slate-700 space-y-1">
              <span className="flex items-center gap-1.5"><Mic className="w-3.5 h-3.5 text-pink-600" /> Ngôn ngữ nhận diện (STT)</span>
              <select value={form.sttLanguage} onChange={e => setForm({ ...form, sttLanguage: e.target.value })} className={inputClass}>
                {LOCALES.map(l => <option key={l.value} value={l.value}>{l.label}</option>)}
              </select>
            </label>
            <label className="block font-semibold text-slate-700 space-y-1">
              <span className="flex items-center gap-1.5"><Volume2 className="w-3.5 h-3.5 text-pink-600" /> Ngôn ngữ đọc (TTS)</span>
              <select value={form.ttsLanguage} onChange={e => setForm({ ...form, ttsLanguage: e.target.value })} className={inputClass}>
                {LOCALES.map(l => <option key={l.value} value={l.value}>{l.label}</option>)}
              </select>
            </label>
            <label className="block font-semibold text-slate-700 space-y-1">
              <span>Giọng tiếng Việt</span>
              <input value={form.vietnameseVoice} onChange={e => setForm({ ...form, vietnameseVoice: e.target.value })} className={inputClass} placeholder="Mã giọng của dịch vụ TTS" />
            </label>
            <label className="block font-semibold text-slate-700 space-y-1">
              <span>Giọng tiếng Anh (tuỳ chọn)</span>
              <input value={form.englishVoice} onChange={e => setForm({ ...form, englishVoice: e.target.value })} className={inputClass} placeholder="Để trống nếu không dùng" />
            </label>
          </div>

          <div className="grid sm:grid-cols-2 gap-4">
            {[
              { key: 'speed', label: 'Tốc độ đọc' },
              { key: 'pitch', label: 'Cao độ' }
            ].map(({ key, label }) => (
              <label key={key} className="block font-semibold text-slate-700 space-y-1">
                <span className="flex justify-between"><span>{label}</span><span className="font-mono text-slate-500">{Number(form[key]).toFixed(2)}×</span></span>
                <input type="range" min="0.25" max="4" step="0.05" value={form[key]}
                  onChange={e => setForm({ ...form, [key]: e.target.value })} className="w-full accent-pink-600" />
              </label>
            ))}
          </div>
          <p className="text-[11px] text-slate-500">
            Dịch vụ TTS có thể không hỗ trợ đổi tốc độ/cao độ — khi đó hệ thống sẽ thông báo và chỉ áp dụng giá trị mặc định 1.00×.
          </p>

          <div className="space-y-2 pt-3 border-t border-pink-200">
            <span className="font-semibold text-slate-700 block">Thử nghiệm trước khi lưu (gọi dịch vụ TTS/STT thật)</span>
            <div className="flex gap-2">
              <input value={previewText} onChange={e => setPreviewText(e.target.value)} className={inputClass} />
              <button 
                type="button" 
                onClick={isPreviewing ? stopPreview : handlePreview}
                className="px-3.5 py-2 rounded-xl bg-white border border-pink-200 font-semibold text-slate-700 flex items-center gap-1.5 hover:bg-slate-50 shrink-0 shadow-2xs transition cursor-pointer"
              >
                {isPreviewing ? <Square className="w-4 h-4 text-rose-600" /> : <Volume2 className="w-4 h-4 text-pink-600" />} {isPreviewing ? 'Dừng' : 'Thử nghiệm'}
              </button>
            </div>
            {testResult && (
              <div aria-live="polite" className={`rounded-xl border p-3 text-xs space-y-1 ${testPassed ? 'border-emerald-300 bg-emerald-50 text-emerald-900' : 'border-amber-300 bg-amber-50 text-amber-900'}`}>
                <p>TTS: {testResult.ttsAvailable ? 'đang hoạt động' : 'không phản hồi'}{testResult.browserVoice ? ' (ngôn ngữ này dùng giọng trình duyệt)' : ''}</p>
                <p>STT: {testResult.sttAvailable ? 'đang hoạt động' : 'không phản hồi'}</p>
                <p className="font-semibold">{testedKey === formKey ? testResult.message : 'Cấu hình đã thay đổi sau lần thử, hãy thử nghiệm lại.'}</p>
              </div>
            )}
          </div>

          <div className="flex items-center justify-between gap-3 pt-3 border-t border-pink-200">
            <p className="text-[11px] text-slate-400 font-mono">
              {settings?.updatedAt ? `Cập nhật lần cuối: ${new Date(settings.updatedAt).toLocaleString('vi-VN')}` : 'Đang dùng cấu hình mặc định'}
            </p>
            <button 
              type="submit" 
              disabled={isSaving || !testPassed}
              title={testPassed ? undefined : 'Cần thử nghiệm thành công trước khi lưu'}
              className="px-5 py-2 rounded-xl bg-pink-600 hover:bg-pink-700 text-white font-semibold flex items-center gap-1.5 shadow-xs transition active:scale-[0.98] disabled:opacity-50 cursor-pointer"
            >
              {isSaving ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />} Lưu Cấu Hình
            </button>
          </div>
        </form>
      )}
    </div>
  );
}
