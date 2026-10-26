import React, { useState, useEffect, useRef, useCallback } from 'react';
import { Bell, Loader2 } from 'lucide-react';
import { notificationApi } from '../../api/notificationApi';

const POLL_MS = 60000;

/** Navbar bell: unread badge (polled), dropdown list, click marks an item read. */
export function NotificationBell() {
  const [unread, setUnread] = useState(0);
  const [isOpen, setIsOpen] = useState(false);
  const [items, setItems] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [loadError, setLoadError] = useState('');
  const ref = useRef(null);

  const refreshCount = useCallback(async () => {
    try {
      const res = await notificationApi.unreadCount();
      setUnread(res?.count ?? 0);
    } catch (_) {
      // Badge is best-effort; the dropdown surfaces real errors when opened
    }
  }, []);

  useEffect(() => {
    refreshCount();
    const timer = setInterval(refreshCount, POLL_MS);
    return () => clearInterval(timer);
  }, [refreshCount]);

  useEffect(() => {
    if (!isOpen) return;
    const onClickOutside = (e) => {
      if (ref.current && !ref.current.contains(e.target)) setIsOpen(false);
    };
    const onKey = (e) => e.key === 'Escape' && setIsOpen(false);
    document.addEventListener('mousedown', onClickOutside);
    window.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onClickOutside);
      window.removeEventListener('keydown', onKey);
    };
  }, [isOpen]);

  const toggle = async () => {
    const next = !isOpen;
    setIsOpen(next);
    if (!next) return;
    setIsLoading(true);
    setLoadError('');
    try {
      const page = await notificationApi.list(0, 20);
      setItems(page?.content || []);
    } catch (err) {
      setLoadError(err?.message || 'Không tải được thông báo.');
    } finally {
      setIsLoading(false);
    }
  };

  const markRead = async (n) => {
    if (n.readAt) return;
    try {
      await notificationApi.markRead(n.notificationId);
      setItems(prev => prev.map(i => (i.notificationId === n.notificationId ? { ...i, readAt: new Date().toISOString() } : i)));
      setUnread(c => Math.max(0, c - 1));
    } catch (_) {
      refreshCount();
    }
  };

  return (
    <div className="relative" ref={ref}>
      <button
        type="button"
        onClick={toggle}
        aria-label={unread > 0 ? `Thông báo (${unread} chưa đọc)` : 'Thông báo'}
        aria-expanded={isOpen}
        className="relative w-9 h-9 rounded-xl border-2 border-green-400 bg-paper hover:bg-green-50 flex items-center justify-center text-green-900 transition focus:outline-none focus:ring-2 focus:ring-green-600"
      >
        <Bell className="w-4 h-4" />
        {unread > 0 && (
          <span className="absolute -top-1.5 -right-1.5 min-w-[18px] h-[18px] px-1 rounded-full bg-pink-600 text-white text-[10px] font-bold flex items-center justify-center">
            {unread > 99 ? '99+' : unread}
          </span>
        )}
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-80 max-w-[calc(100vw-2rem)] bg-paper border-2 border-green-500 rounded-2xl shadow-[4px_4px_0_#DEB9B5] z-50 overflow-hidden text-xs">
          <div className="px-4 py-3 border-b-2 border-green-200 font-bold">Thông báo</div>
          <div className="max-h-96 overflow-y-auto">
            {isLoading ? (
              <div className="p-6 flex items-center justify-center gap-2 text-green-800"><Loader2 className="w-4 h-4 animate-spin" /> Đang tải...</div>
            ) : loadError ? (
              <p className="p-4 text-pink-800">{loadError}</p>
            ) : items.length === 0 ? (
              <p className="p-6 text-center text-green-800">Không có thông báo nào.</p>
            ) : (
              <ul className="divide-y divide-green-100">
                {items.map(n => (
                  <li key={n.notificationId}>
                    <button
                      type="button"
                      onClick={() => markRead(n)}
                      className={`w-full text-left px-4 py-3 hover:bg-green-50 focus:outline-none focus:bg-green-50 ${n.readAt ? 'text-green-800' : 'font-semibold text-ink'}`}
                    >
                      <span className="flex items-start gap-2">
                        {!n.readAt && <span className="mt-1 w-2 h-2 rounded-full bg-pink-600 shrink-0" aria-hidden="true" />}
                        <span>
                          <span className="block">{n.message}</span>
                          <span className="block text-[10px] text-green-700 font-normal mt-0.5">{new Date(n.createdAt).toLocaleString('vi-VN')}</span>
                        </span>
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
