import React, { useEffect, useRef } from 'react';

/**
 * AudioVisualizerCanvas (HTML5 Canvas 60 FPS)
 * High-performance audio wave renderer using requestAnimationFrame & Canvas 2D.
 * Prevents DOM layout-thrashing during real-time speech recognition.
 * Styled with Blush Pink (#CE9893) and Sage Green (#819266) design tokens.
 */
export function AudioVisualizerCanvas({
  isActive = false,
  volume = 0.7,
  barCount = 18,
  width = 160,
  height = 28,
  primaryColor = '#CE9893', // Blush Pink
  accentColor = '#819266'   // Sage Green
}) {
  const canvasRef = useRef(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    let animId;
    let phase = 0;

    const render = () => {
      ctx.clearRect(0, 0, width, height);

      const barWidth = Math.max(2, (width / barCount) - 3);
      const gap = 3;

      for (let i = 0; i < barCount; i++) {
        let barHeight;
        if (isActive) {
          // Dynamic harmonic sine & cosine waves for realistic voice frequency response
          const wave1 = Math.sin(phase + i * 0.45);
          const wave2 = Math.cos(phase * 0.8 + i * 0.25);
          const wave3 = Math.sin(phase * 1.3 + i * 0.6);
          const amp = Math.abs(wave1 * 0.5 + wave2 * 0.3 + wave3 * 0.2);
          barHeight = Math.max(4, amp * height * volume * 0.95);
        } else {
          barHeight = 4;
        }

        const x = i * (barWidth + gap) + 4;
        const y = (height - barHeight) / 2;

        // Linear gradient between Blush Pink and Sage Green
        const grad = ctx.createLinearGradient(0, y, 0, y + barHeight);
        grad.addColorStop(0, primaryColor);
        grad.addColorStop(1, accentColor);

        ctx.fillStyle = grad;
        // Rounded bar rectangle
        ctx.beginPath();
        const radius = barWidth / 2;
        ctx.roundRect(x, y, barWidth, barHeight, radius);
        ctx.fill();
      }

      phase += isActive ? 0.09 : 0.02;
      animId = requestAnimationFrame(render);
    };

    animId = requestAnimationFrame(render);
    return () => cancelAnimationFrame(animId);
  }, [isActive, volume, barCount, width, height, primaryColor, accentColor]);

  return (
    <canvas
      ref={canvasRef}
      width={width}
      height={height}
      className="rounded-lg select-none"
      aria-label="Real-time Audio Visualizer"
    />
  );
}
