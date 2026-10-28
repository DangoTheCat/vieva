import React, { useState, useEffect, useRef } from 'react';
import { notificationApi } from '../../api/notificationApi';
import { Bell, Loader2 } from 'lucide-react';

export function NotificationBell() {
  const [isOpen, setIsOpen] = useState(false);
  const [items, setItems] = useState([]);
  const [unread, setUnread] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [loadError, setLoadError] = useState('');
  const rootRef = useRef(null);

  const refreshCount = async () => {
    try {
      const data = await notificationApi.getUnreadCount();
      setUnread(Number(data?.unreadCount || 0));
    } catch {
      // Background count errors fail silently to not interrupt workflow
    }
  };

  const loadList = async () => {
    setIsLoading(true);
    setLoadError('');
    try {
      const data = await notificationApi.getMyNotifications(0, 20);
      const list = data?.content || [];
      setItems(list);
      setUnread(list.filter(n => !n.readAt).length);
    } catch (err) {
      setLoadError('Không thể tải thông báo.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    refreshCount();
    const interval = setInterval(refreshCount, 30000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    const handleClickOutside = (e) => {
      if (rootRef.current && !rootRef.current.contains(e.target)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const toggle = () => {
    const next = !isOpen;
    setIsOpen(next);
    if (next) loadList();
  };

  const markRead = async (item) => {
    if (item.readAt) return;
    try {
      await notificationApi.markAsRead(item.notificationId);
      setItems(prev => prev.map(n => n.notificationId === item.notificationId ? { ...n, readAt: new Date().toISOString() } : n));
      setUnread(u => Math.max(0, u - 1));
    } catch {
      // Silent error
    }
  };

  return (
    <div className="relative" ref={rootRef}>
      <button
        type="button"
        onClick={toggle}
        aria-label={unread > 0 ? `Thông báo (${unread} chưa đọc)` : 'Thông báo'}
        aria-expanded={isOpen}
        className="relative w-8 h-8 rounded-lg border border-pink-200 bg-white hover:bg-slate-50 flex items-center justify-center text-slate-700 shadow-2xs transition cursor-pointer focus:outline-none focus:ring-1 focus:ring-pink-500"
      >
        <Bell className="w-4 h-4 text-slate-600" />
        {unread > 0 && (
          <span className="absolute -top-1 -right-1 min-w-[16px] h-[16px] px-1 rounded-full bg-rose-600 text-white text-[9px] font-bold flex items-center justify-center shadow-xs">
            {unread > 99 ? '99+' : unread}
          </span>
        )}
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-80 max-w-[calc(100vw-2rem)] bg-white border border-pink-200 rounded-xl shadow-lg z-50 overflow-hidden text-xs text-slate-800 animate-modal-entry">
          <div className="px-4 py-2.5 border-b border-pink-100 font-semibold text-ink bg-slate-50/70 flex items-center justify-between">
            <span>Thông báo</span>
            {unread > 0 && (
              <span className="text-[10px] bg-rose-50 text-rose-600 font-semibold px-1.5 py-0.5 rounded-full border border-rose-200">
                {unread} mới
              </span>
            )}
          </div>
          <div className="max-h-96 overflow-y-auto">
            {isLoading ? (
              <div className="p-6 flex items-center justify-center gap-2 text-slate-500">
                <Loader2 className="w-4 h-4 animate-spin" /> Đang tải...
              </div>
            ) : loadError ? (
              <p className="p-4 text-rose-600">{loadError}</p>
            ) : items.length === 0 ? (
              <p className="p-6 text-center text-slate-500">Không có thông báo nào.</p>
            ) : (
              <ul className="divide-y divide-slate-100">
                {items.map(n => (
                  <li key={n.notificationId}>
                    <button
                      type="button"
                      onClick={() => markRead(n)}
                      className={`w-full text-left px-4 py-3 hover:bg-slate-50 focus:outline-none focus:bg-slate-50 transition cursor-pointer ${n.readAt ? 'text-slate-500' : 'font-medium text-ink bg-slate-50/30'}`}
                    >
                      <span className="flex items-start gap-2">
                        {!n.readAt ? (
                          <span className="mt-1.5 w-2 h-2 rounded-full bg-pink-600 shrink-0" aria-hidden="true" />
                        ) : (
                          <span className="mt-1.5 w-2 h-2 rounded-full bg-slate-300 shrink-0" aria-hidden="true" />
                        )}
                        <span className="min-w-0 flex-1">
                          <span className="block leading-snug">{n.message}</span>
                          <span className="block text-[10px] text-slate-400 font-mono mt-1">{new Date(n.createdAt).toLocaleString('vi-VN')}</span>
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
