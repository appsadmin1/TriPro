import React, { useState, useEffect, useRef } from 'react';
import { Box, Slider, Typography, Stack } from '@mui/material';

interface ColorPickerWheelProps {
  initialColor: string;
  onColorChange: (hex: string) => void;
}

const ColorPickerWheel: React.FC<ColorPickerWheelProps> = ({ initialColor, onColorChange }) => {
  const [hsv, setHsv] = useState({ h: 0, s: 0, v: 100 });
  const wheelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const { h, s, v } = hexToHsv(initialColor);
    setHsv({ h, s, v });
  }, [initialColor]);

  const handleWheelClick = (e: React.MouseEvent | React.TouchEvent) => {
    if (!wheelRef.current) return;
    const rect = wheelRef.current.getBoundingClientRect();
    const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX;
    const clientY = 'touches' in e ? e.touches[0].clientY : e.clientY;

    const x = clientX - rect.left - rect.width / 2;
    const y = clientY - rect.top - rect.height / 2;
    const radius = rect.width / 2;
    const distance = Math.sqrt(x * x + y * y);

    if (distance > radius) return;

    let angle = Math.atan2(y, x) * (180 / Math.PI);
    angle += 90; // Adjust so 0 is at the top (matching conic-gradient)
    if (angle < 0) angle += 360;
    if (angle >= 360) angle -= 360;

    const saturation = (distance / radius) * 100;

    const newHsv = { ...hsv, h: angle, s: saturation };
    setHsv(newHsv);
    onColorChange(hsvToHex(newHsv.h, newHsv.s, newHsv.v));
  };

  const handleBrightnessChange = (_: Event, value: number | number[]) => {
    const newHsv = { ...hsv, v: value as number };
    setHsv(newHsv);
    onColorChange(hsvToHex(newHsv.h, newHsv.s, newHsv.v));
  };

  return (
    <Stack spacing={2} alignItems="center" sx={{ width: '100%' }}>
      <Box
        ref={wheelRef}
        onClick={handleWheelClick}
        onMouseDown={(e) => {
           const onMouseMove = (moveEvent: MouseEvent) => handleWheelClick(moveEvent as any);
           const onMouseUp = () => {
             window.removeEventListener('mousemove', onMouseMove);
             window.removeEventListener('mouseup', onMouseUp);
           };
           window.addEventListener('mousemove', onMouseMove);
           window.addEventListener('mouseup', onMouseUp);
        }}
        sx={{
          width: 200,
          height: 200,
          borderRadius: '50%',
          position: 'relative',
          cursor: 'crosshair',
          background: `
            radial-gradient(circle, white, transparent),
            conic-gradient(red, yellow, lime, cyan, blue, magenta, red)
          `,
          boxShadow: 3,
          '&::after': {
            content: '""',
            position: 'absolute',
            top: 0, left: 0, right: 0, bottom: 0,
            borderRadius: '50%',
            backgroundColor: 'black',
            opacity: (100 - hsv.v) / 100,
            pointerEvents: 'none'
          }
        }}
      >
        <Box
          sx={{
            position: 'absolute',
            width: 12,
            height: 12,
            border: '2px solid white',
            borderRadius: '50%',
            boxShadow: '0 0 2px black',
            transform: 'translate(-50%, -50%)',
            left: `${50 + (hsv.s / 2) * Math.cos((hsv.h - 90) * Math.PI / 180)}%`,
            top: `${50 + (hsv.s / 2) * Math.sin((hsv.h - 90) * Math.PI / 180)}%`,
            pointerEvents: 'none'
          }}
        />
      </Box>

      <Box sx={{ width: '80%' }}>
        <Typography variant="caption" color="text.secondary">Brightness</Typography>
        <Slider
          value={hsv.v}
          min={0}
          max={100}
          onChange={handleBrightnessChange}
        />
      </Box>
    </Stack>
  );
};

// Helper functions
function hexToHsv(hex: string) {
  let r = 0, g = 0, b = 0;
  if (hex.length === 4) {
    r = parseInt(hex[1] + hex[1], 16);
    g = parseInt(hex[2] + hex[2], 16);
    b = parseInt(hex[3] + hex[3], 16);
  } else if (hex.length === 7) {
    r = parseInt(hex.substring(1, 3), 16);
    g = parseInt(hex.substring(3, 5), 16);
    b = parseInt(hex.substring(5, 7), 16);
  }
  r /= 255; g /= 255; b /= 255;
  const max = Math.max(r, g, b), min = Math.min(r, g, b);
  let h = 0, s = 0, v = max;
  const d = max - min;
  s = max === 0 ? 0 : d / max;
  if (max !== min) {
    switch (max) {
      case r: h = (g - b) / d + (g < b ? 6 : 0); break;
      case g: h = (b - r) / d + 2; break;
      case b: h = (r - g) / d + 4; break;
    }
    h /= 6;
  }
  return { h: h * 360, s: s * 100, v: v * 100 };
}

function hsvToHex(h: number, s: number, v: number) {
  h /= 360; s /= 100; v /= 100;
  let r = 0, g = 0, b = 0;
  const i = Math.floor(h * 6);
  const f = h * 6 - i;
  const p = v * (1 - s);
  const q = v * (1 - f * s);
  const t = v * (1 - (1 - f) * s);
  switch (i % 6) {
    case 0: r = v; g = t; b = p; break;
    case 1: r = q; g = v; b = p; break;
    case 2: r = p; g = v; b = t; break;
    case 3: r = p; g = q; b = v; break;
    case 4: r = t; g = p; b = v; break;
    case 5: r = v; g = p; b = q; break;
  }
  const toHex = (x: number) => {
    const hex = Math.round(x * 255).toString(16);
    return hex.length === 1 ? '0' + hex : hex;
  };
  return `#${toHex(r)}${toHex(g)}${toHex(b)}`.toUpperCase();
}

export default ColorPickerWheel;
