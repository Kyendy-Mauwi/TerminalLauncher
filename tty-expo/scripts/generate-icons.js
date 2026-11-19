// Generates the T3rm!n@l app icons: a neon ">_" prompt glyph.
// Run with: node scripts/generate-icons.js
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const OUT = path.join(__dirname, '..', 'assets');

function crc32(buffer) {
  let crc = 0xffffffff;
  for (const byte of buffer) {
    crc ^= byte;
    for (let bit = 0; bit < 8; bit += 1) {
      crc = crc & 1 ? (crc >>> 1) ^ 0xedb88320 : crc >>> 1;
    }
  }
  return (crc ^ 0xffffffff) >>> 0;
}

function chunk(type, data) {
  const length = Buffer.alloc(4);
  length.writeUInt32BE(data.length);
  const body = Buffer.concat([Buffer.from(type, 'ascii'), data]);
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(body));
  return Buffer.concat([length, body, crc]);
}

function encodePng(width, height, rgba) {
  const signature = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width, 0);
  ihdr.writeUInt32BE(height, 4);
  ihdr[8] = 8;
  ihdr[9] = 6;
  const rows = Buffer.alloc((width * 4 + 1) * height);
  for (let y = 0; y < height; y += 1) {
    rows[y * (width * 4 + 1)] = 0;
    rgba.copy(rows, y * (width * 4 + 1) + 1, y * width * 4, (y + 1) * width * 4);
  }
  return Buffer.concat([
    signature,
    chunk('IHDR', ihdr),
    chunk('IDAT', zlib.deflateSync(rows, { level: 9 })),
    chunk('IEND', Buffer.alloc(0)),
  ]);
}

function distanceToSegment(x, y, ax, ay, bx, by) {
  const dx = bx - ax;
  const dy = by - ay;
  const lengthSquared = dx * dx + dy * dy;
  const t = lengthSquared === 0 ? 0 : Math.max(0, Math.min(1, ((x - ax) * dx + (y - ay) * dy) / lengthSquared));
  const px = ax + t * dx;
  const py = ay + t * dy;
  return Math.hypot(x - px, y - py);
}

// Glyph in a 100x100 coordinate space: ">" chevron plus "_" underscore.
const STROKES = [
  { ax: 20, ay: 22, bx: 58, by: 50, radius: 5.6 },
  { ax: 58, ay: 50, bx: 20, by: 78, radius: 5.6 },
  { ax: 66, ay: 80, bx: 90, by: 80, radius: 5.2 },
];

const SUPERSAMPLE = 4;

/**
 * Draws the glyph centred in `size`, occupying `scale` of the canvas.
 * Returns straight RGBA bytes at `size` resolution.
 */
function renderGlyph(size, { glyphScale, background, color, transparent }) {
  const big = size * SUPERSAMPLE;
  const pixels = new Float32Array(big * big * 4);

  const box = big * glyphScale;
  const offset = (big - box) / 2;
  const unit = box / 100;
  const toPx = (value) => offset + value * unit;

  const strokes = STROKES.map((stroke) => ({
    ...stroke,
    ax: toPx(stroke.ax),
    ay: toPx(stroke.ay),
    bx: toPx(stroke.bx),
    by: toPx(stroke.by),
    r: stroke.radius * unit,
  }));
  const glow = 5 * unit;

  const [cr, cg, cb] = color;
  const [br, bg, bb] = background;

  for (let y = 0; y < big; y += 1) {
    for (let x = 0; x < big; x += 1) {
      const index = (y * big + x) * 4;
      let distance = Infinity;
      for (const stroke of strokes) {
        const d = distanceToSegment(x + 0.5, y + 0.5, stroke.ax, stroke.ay, stroke.bx, stroke.by) - stroke.r;
        if (d < distance) distance = d;
      }
      let alpha = 0;
      if (distance <= 0) {
        alpha = 1;
      } else if (distance < glow) {
        alpha = 0.35 * (1 - distance / glow);
      }

      if (transparent) {
        pixels[index] = cr;
        pixels[index + 1] = cg;
        pixels[index + 2] = cb;
        pixels[index + 3] = alpha * 255;
      } else {
        pixels[index] = br + (cr - br) * alpha;
        pixels[index + 1] = bg + (cg - bg) * alpha;
        pixels[index + 2] = bb + (cb - bb) * alpha;
        pixels[index + 3] = 255;
      }
    }
  }

  const out = Buffer.alloc(size * size * 4);
  const samples = SUPERSAMPLE * SUPERSAMPLE;
  for (let y = 0; y < size; y += 1) {
    for (let x = 0; x < size; x += 1) {
      let r = 0;
      let g = 0;
      let b = 0;
      let a = 0;
      for (let sy = 0; sy < SUPERSAMPLE; sy += 1) {
        for (let sx = 0; sx < SUPERSAMPLE; sx += 1) {
          const source = ((y * SUPERSAMPLE + sy) * big + (x * SUPERSAMPLE + sx)) * 4;
          r += pixels[source];
          g += pixels[source + 1];
          b += pixels[source + 2];
          a += pixels[source + 3];
        }
      }
      const target = (y * size + x) * 4;
      out[target] = Math.round(r / samples);
      out[target + 1] = Math.round(g / samples);
      out[target + 2] = Math.round(b / samples);
      out[target + 3] = Math.round(a / samples);
    }
  }
  return out;
}

const DARK = [8, 11, 15];
const GREEN = [51, 255, 136];
const WHITE = [255, 255, 255];

const assets = [
  { file: 'icon.png', size: 1024, glyphScale: 0.62, background: DARK, color: GREEN, transparent: false },
  { file: 'splash-icon.png', size: 1024, glyphScale: 0.4, background: DARK, color: GREEN, transparent: true },
  { file: 'favicon.png', size: 96, glyphScale: 0.68, background: DARK, color: GREEN, transparent: false },
  { file: 'watermark.png', size: 512, glyphScale: 0.78, background: DARK, color: GREEN, transparent: true },
  { file: 'android-icon-foreground.png', size: 512, glyphScale: 0.5, background: DARK, color: GREEN, transparent: true },
  { file: 'android-icon-background.png', size: 512, glyphScale: 0.5, background: DARK, color: GREEN, transparent: false },
  { file: 'android-icon-monochrome.png', size: 432, glyphScale: 0.5, background: DARK, color: WHITE, transparent: true },
];

for (const asset of assets) {
  const rgba = renderGlyph(asset.size, asset);
  const png = encodePng(asset.size, asset.size, rgba);
  fs.writeFileSync(path.join(OUT, asset.file), png);
  console.log(`wrote ${asset.file} ${asset.size}x${asset.size} (${png.length} bytes)`);
}
