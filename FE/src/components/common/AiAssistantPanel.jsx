import React, { useState, useEffect, useRef } from 'react';
import { Bot, X, Send } from 'lucide-react';
import { aiAssistantApi } from '../../api/aiAssistantApi';
import { getErrorMessage } from '../../utils/errorCodes';

const MAX_MESSAGE_LENGTH = 2000;

export function AiAssistantPanel({ isOpen, onClose }) {
  const [messages, setMessages] = useState([]);
  const [draft, setDraft] = useState('');
  const [isSending, setIsSending] = useState(false);
  const listRef = useRef(null);

  // Keyboard accessibility: Escape to close
  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  useEffect(() => {
    listRef.current?.scrollTo({ top: listRef.current.scrollHeight });
  }, [messages]);

  if (!isOpen) return null;

  const handleSend = async (e) => {
    e.preventDefault();
    const text = draft.trim();
    if (!text || isSending) return;

    setMessages(prev => [...prev, { role: 'user', text }]);
    setDraft('');
    setIsSending(true);
    try {
      const res = await aiAssistantApi.chat(text);
      setMessages(prev => [...prev, { role: 'assistant', text: res.reply }]);
    } catch (err) {
      setMessages(prev => [...prev, { role: 'error', text: getErrorMessage(err) }]);
    } finally {
      setIsSending(false);
    }
  };

  return (
    <aside
      className="fixed bottom-4 right-4 z-50 w-[min(380px,calc(100vw-2rem))] h-[min(520px,calc(100vh-6rem))] bg-white rounded-2xl border border-slate-200/90 shadow-2xl flex flex-col overflow-hidden animate-modal-entry"
      role="dialog"
      aria-label="Trợ lý AI"
    >
      <div className="p-3.5 bg-sidebarBg text-white flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-xl bg-sky-500/20 text-sky-300 flex items-center justify-center">
            <Bot className="w-4 h-4" />
          </div>
          <div>
            <h3 className="font-heading font-extrabold text-sm">Trợ Lý AI</h3>
            <p className="text-[10px] text-slate-400">Trả lời dựa trên dữ liệu hệ thống AIVES</p>
          </div>
        </div>
        <button
          type="button"
          onClick={onClose}
          className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition focus:outline-none focus:ring-2 focus:ring-sky-500"
          aria-label="Đóng trợ lý AI"
        >
          <X className="w-4 h-4" />
        </button>
      </div>

      <div ref={listRef} className="flex-1 overflow-y-auto p-3.5 space-y-2.5 bg-canvasBg text-xs">
        {messages.length === 0 && (
          <p className="text-center text-slate-400 mt-8 px-4">
            Hỏi về môn học, ngân hàng câu hỏi hoặc cách sử dụng hệ thống.
          </p>
        )}
        {messages.map((m, idx) => (
          <div key={idx} className={`flex ${m.role === 'user' ? 'justify-end' : 'justify-start'}`}>
            <div className={`max-w-[85%] px-3 py-2 rounded-2xl leading-relaxed whitespace-pre-wrap ${m.role === 'user'
                ? 'bg-sky-600 text-white'
                : m.role === 'error'
                  ? 'bg-rose-50 text-rose-700 border border-rose-200'
                  : 'bg-white text-slate-800 border border-slate-200'
              }`}>
              {m.text}
            </div>
          </div>
        ))}
        {isSending && (
          <div className="flex justify-start">
            <div className="px-3 py-2 rounded-2xl bg-white border border-slate-200 text-slate-400 animate-pulse">
              Đang trả lời...
            </div>
          </div>
        )}
      </div>

      <form onSubmit={handleSend} className="p-3 border-t border-slate-200 flex items-end gap-2">
        <textarea
          rows="2"
          maxLength={MAX_MESSAGE_LENGTH}
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey) handleSend(e);
          }}
          placeholder="Nhập câu hỏi..."
          className="flex-1 resize-none px-3 py-2 rounded-xl text-xs border border-slate-300 focus:outline-none focus:ring-2 focus:ring-sky-500"
        />
        <button
          type="submit"
          disabled={isSending || !draft.trim()}
          className="p-2.5 rounded-xl bg-sky-600 hover:bg-sky-700 text-white transition active:scale-95 disabled:opacity-50 focus:outline-none focus:ring-2 focus:ring-sky-500"
          aria-label="Gửi"
        >
          <Send className="w-4 h-4" />
        </button>
      </form>
    </aside>
  );
}
