import React, { useEffect, useId, useLayoutEffect, useMemo, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { Check, ChevronDown } from 'lucide-react';

// Flatten <option> children (including fragments, arrays and conditionals) into plain data
function collectOptions(children, out = []) {
  React.Children.forEach(children, (child) => {
    if (!React.isValidElement(child)) return;
    if (child.type === React.Fragment) {
      collectOptions(child.props.children, out);
      return;
    }
    if (child.type === 'option') {
      const { value, disabled, children: label } = child.props;
      const text = React.Children.toArray(label).join('');
      out.push({ value: value != null ? String(value) : text, label: text, disabled: !!disabled });
    }
  });
  return out;
}

/**
 * Drop-in replacement for a native single <select>: same value / onChange(e.target.value) / <option> children,
 * but with a styled, animated, keyboard-accessible popup instead of the OS dropdown.
 */
export function Select({ value, onChange, children, className = '', disabled = false, id, name, placeholder, 'aria-label': ariaLabel, title }) {
  const options = useMemo(() => collectOptions(children), [children]);
  const current = value != null ? String(value) : '';
  const selected = options.find(o => o.value === current);

  const [open, setOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);
  const [rect, setRect] = useState(null);
  const triggerRef = useRef(null);
  const listRef = useRef(null);
  const listId = useId();

  const place = () => {
    const r = triggerRef.current?.getBoundingClientRect();
    if (!r) return;
    const spaceBelow = window.innerHeight - r.bottom;
    const dropUp = spaceBelow < 260 && r.top > spaceBelow;
    const width = Math.min(Math.max(r.width, 280), window.innerWidth - 24);
    const left = Math.max(12, Math.min(r.left, window.innerWidth - width - 12));
    setRect({ left, width, top: r.bottom + 6, bottom: window.innerHeight - r.top + 6, dropUp });
  };

  useLayoutEffect(() => {
    if (!open) return;
    place();
    window.addEventListener('resize', place);
    window.addEventListener('scroll', place, true);
    return () => {
      window.removeEventListener('resize', place);
      window.removeEventListener('scroll', place, true);
    };
  }, [open]);

  useEffect(() => {
    if (!open) return;
    const onDown = (e) => {
      if (triggerRef.current?.contains(e.target) || listRef.current?.contains(e.target)) return;
      setOpen(false);
    };
    document.addEventListener('mousedown', onDown);
    return () => document.removeEventListener('mousedown', onDown);
  }, [open]);

  // Long labels may make the popup wider than the trigger: slide it left so it stays on screen
  useLayoutEffect(() => {
    const el = listRef.current;
    if (!open || !el) return;
    const overflow = el.getBoundingClientRect().right - (window.innerWidth - 12);
    if (overflow > 0) el.style.left = `${Math.max(12, rect.left - overflow)}px`;
  }, [open, rect]);

  useEffect(() => {
    if (!open || activeIndex < 0) return;
    listRef.current?.querySelector(`[data-index="${activeIndex}"]`)?.scrollIntoView({ block: 'nearest' });
  }, [open, activeIndex]);

  const openMenu = () => {
    if (disabled) return;
    setActiveIndex(Math.max(0, options.findIndex(o => o.value === current)));
    setOpen(true);
  };

  const choose = (opt) => {
    if (!opt || opt.disabled) return;
    setOpen(false);
    triggerRef.current?.focus();
    if (opt.value !== current) onChange?.({ target: { value: opt.value, name }, currentTarget: { value: opt.value, name } });
  };

  const move = (dir) => {
    if (!options.length) return;
    let i = activeIndex;
    for (let n = 0; n < options.length; n++) {
      i = (i + dir + options.length) % options.length;
      if (!options[i].disabled) break;
    }
    setActiveIndex(i);
  };

  const onKeyDown = (e) => {
    if (disabled) return;
    if (!open) {
      if (['ArrowDown', 'ArrowUp', 'Enter', ' '].includes(e.key)) {
        e.preventDefault();
        openMenu();
      }
      return;
    }
    if (e.key === 'ArrowDown') { e.preventDefault(); move(1); }
    else if (e.key === 'ArrowUp') { e.preventDefault(); move(-1); }
    else if (e.key === 'Home') { e.preventDefault(); setActiveIndex(0); }
    else if (e.key === 'End') { e.preventDefault(); setActiveIndex(options.length - 1); }
    else if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); choose(options[activeIndex]); }
    else if (e.key === 'Escape' || e.key === 'Tab') { setOpen(false); }
  };

  // Callers pass the old <select> classes; keep sizing/border/radius from them and add the trigger behaviour
  const triggerClass = `ui-select-trigger relative inline-flex items-center gap-2 text-left ${className} ${open ? 'is-open' : ''}`;

  return (
    <>
      <button
        ref={triggerRef}
        id={id}
        type="button"
        role="combobox"
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-controls={listId}
        aria-label={ariaLabel}
        title={title || selected?.label}
        disabled={disabled}
        onClick={() => (open ? setOpen(false) : openMenu())}
        onKeyDown={onKeyDown}
        className={triggerClass}
      >
        <span className={`flex-1 min-w-0 truncate ${selected ? '' : 'text-slate-400'}`}>
          {selected?.label ?? placeholder ?? '—'}
        </span>
        <ChevronDown className={`ui-select-chevron w-4 h-4 shrink-0 text-slate-500 transition-transform duration-200 ${open ? 'rotate-180' : ''}`} />
      </button>

      {open && rect && createPortal(
        <ul
          ref={listRef}
          id={listId}
          role="listbox"
          aria-label={ariaLabel}
          style={{
            position: 'fixed',
            left: rect.left,
            width: 'max-content',
            minWidth: rect.width,
            maxWidth: Math.max(rect.width, Math.min(560, window.innerWidth - 24)),
            ...(rect.dropUp ? { bottom: rect.bottom } : { top: rect.top })
          }}
          className={`ui-select-menu z-[100] max-h-72 overflow-y-auto rounded-xl border border-slate-200 bg-white p-1 shadow-[0_12px_32px_-8px_rgba(31,31,29,0.18),0_2px_6px_rgba(31,31,29,0.06)] ${rect.dropUp ? 'origin-bottom' : 'origin-top'}`}
        >
          {options.length === 0 && <li className="px-3 py-2 text-xs text-slate-400">Không có lựa chọn</li>}
          {options.map((opt, i) => {
            const isSelected = opt.value === current;
            const isActive = i === activeIndex;
            return (
              <li
                key={`${opt.value}-${i}`}
                data-index={i}
                role="option"
                aria-selected={isSelected}
                aria-disabled={opt.disabled}
                onMouseEnter={() => !opt.disabled && setActiveIndex(i)}
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => choose(opt)}
                className={`flex items-center gap-2 rounded-lg px-3 py-2 text-xs leading-snug transition-colors duration-100 ${
                  opt.disabled ? 'cursor-not-allowed text-slate-300'
                    : isActive ? 'cursor-pointer bg-pink-50 text-ink'
                      : 'cursor-pointer text-slate-700'
                } ${isSelected ? 'font-semibold text-ink' : 'font-medium'}`}
              >
                <span className="flex-1 min-w-0">{opt.label}</span>
                {isSelected && <Check className="w-3.5 h-3.5 shrink-0 text-pink-600" />}
              </li>
            );
          })}
        </ul>,
        document.body
      )}
    </>
  );
}
