import React, { useEffect, useRef } from 'react';

/**
 * VoiceWaveform Component (60 FPS GPU-Accelerated)
 * Complies with gsap-motion-craft & aives-ui-ux-craft
 * Uses requestAnimationFrame and transform: scaleY() for non-layout-thrashing animation.
 */
export function VoiceWaveform({ 
  isSpeaking = true, 
  volume = 0.6, 
  barCount = 14, 
  height = 24, 
  barWidth = 'w-1', 
  barGap = 'gap-1',
  colorClass = 'bg-sky-400' 
}) {
  const barsRef = useRef([]);

  useEffect(() => {
    if (!isSpeaking) {
      barsRef.current.forEach((bar) => {
        if (bar) bar.style.transform = 'scaleY(0.18)';
      });
      return;
    }

    let animationFrameId;
    const animate = () => {
      const time = Date.now() * 0.007;
      barsRef.current.forEach((bar, index) => {
        if (bar) {
          // Dynamic harmonic sine equation for natural voice wave simulation
          const wave1 = Math.sin(time + index * 0.45);
          const wave2 = Math.cos(time * 0.7 + index * 0.3);
          const factor = Math.abs(wave1 * 0.6 + wave2 * 0.4) * volume + 0.18;
          const clamped = Math.min(1.0, Math.max(0.18, factor));
          bar.style.transform = `scaleY(${clamped})`;
        }
      });
      animationFrameId = requestAnimationFrame(animate);
    };

    animationFrameId = requestAnimationFrame(animate);
    return () => cancelAnimationFrame(animationFrameId);
  }, [isSpeaking, volume]);

  return (
    <div 
      className={`flex items-center ${barGap} px-2.5 py-1.5 bg-slate-900/80 rounded-xl border border-slate-700/80 shadow-inner select-none`}
      style={{ height: `${height + 12}px` }}
      aria-label="Voice Activity Waveform"
    >
      {Array.from({ length: barCount }).map((_, i) => (
        <span
          key={i}
          ref={(el) => (barsRef.current[i] = el)}
          className={`${barWidth} ${colorClass} rounded-full origin-bottom transition-transform duration-75`}
          style={{ height: `${height}px`, transform: 'scaleY(0.18)' }}
        />
      ))}
    </div>
  );
}
