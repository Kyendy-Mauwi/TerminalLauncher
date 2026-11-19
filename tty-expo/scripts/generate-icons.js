// Generates the T3rm!n@l app icons: a charcoal rounded card with a white ">_" prompt.
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
  return Math.hypot(x - (ax + t * dx), y - (ay + t * dy));
}

/** Signed distance to a rounded rectangle centred on the origin. */
function distanceToRoundedBox(x, y, halfWidth, halfHeight, radius) {
  const dx = Math.abs(x) - halfWidth + radius;
  const dy = Math.abs(y) - halfHeight + radius;
  return Math.hypot(Math.max(dx, 0), Math.max(dy, 0)) + Math.min(Math.max(dx, dy), 0) - radius;
}

// Glyph in a 100x100 coordinate space: thick ">" chevron plus "_" underscore.
const STROKES = [
  { ax: 27, ay: 24, bx: 53, by: 50, radius: 6.3 },
  { ax: 53, ay: 50, bx: 27, by: 76, radius: 6.3 },
  { ax: 59, ay: 77, bx: 85, by: 77, radius: 6.3 },
];

const SUPERSAMPLE = 4;
const CARD = [47, 47, 47];
const WHITE = [255, 255, 255];

/**
 * Draws the logo. `card` fills a rounded square centred in the canvas; the glyph is
 * drawn on top. Everything outside the card is transparent unless `background` is set.
 */
function renderLogo(size, { glyphScale, card = null, glyphColor = WHITE, background = null }) {
  const big = size * SUPERSAMPLE;
  // Premultiplied float accumulators so edges blend without dark halos.
  const pixels = new Float32Array(big * big * 4);

  const box = big * glyphScale;
  const offset = (big - box) / 2;
  const unit = box / 100;
  const toPx = (value) => offset + value * unit;
  const strokes = STROKES.map((stroke) => ({
    ax: toPx(stroke.ax),
    ay: toPx(stroke.ay),
    bx: toPx(stroke.bx),
    by: toPx(stroke.by),
    r: stroke.radius * unit,
  }));

  const cardShape = card
    ? {
        half: big * card.half,
        radius: big * (card.radius ?? 0),
        color: card.color ?? CARD,
      }
    : null;

  for (let y = 0; y < big; y += 1) {
    for (let x = 0; x < big; x += 1) {
      const index = (y * big + x) * 4;
      let r = 0;
      let g = 0;
      let b = 0;
      let a = 0;

      if (background) {
        r = background[0];
        g = background[1];
        b = background[2];
        a = 1;
      }

      if (cardShape) {
        const d = distanceToRoundedBox(x + 0.5 - big / 2, y + 0.5 - big / 2, cardShape.half, cardShape.half, cardShape.radius);
        if (d <= 0) {
          r = cardShape.color[0];
          g = cardShape.color[1];
          b = cardShape.color[2];
          a = 1;
        }
      }

      let distance = Infinity;
      for (const stroke of strokes) {
        const d = distanceToSegment(x + 0.5, y + 0.5, stroke.ax, stroke.ay, stroke.bx, stroke.by) - stroke.r;
        if (d < distance) distance = d;
      }
      if (distance <= 0) {
        r = glyphColor[0];
        g = glyphColor[1];
        b = glyphColor[2];
        a = 1;
      }

      pixels[index] = r * a;
      pixels[index + 1] = g * a;
      pixels[index + 2] = b * a;
      pixels[index + 3] = a;
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
      const alpha = a / samples;
      if (alpha > 0) {
        out[target] = Math.round(Math.min(255, r / a));
        out[target + 1] = Math.round(Math.min(255, g / a));
        out[target + 2] = Math.round(Math.min(255, b / a));
      }
      out[target + 3] = Math.round(alpha * 255);
    }
  }
  return out;
}

const FULL_CARD = { half: 0.5, radius: 0 };

const assets = [
  // Square launcher icon: full-bleed card so the launcher mask does the rounding.
  { file: 'icon.png', size: 1024, glyphScale: 0.62, card: FULL_CARD },
  // Splash mark keeps the rounded card silhouette on a transparent field.
  { file: 'splash-icon.png', size: 1024, glyphScale: 0.5, card: { half: 0.42, radius: 0.16 } },
  { file: 'favicon.png', size: 96, glyphScale: 0.66, card: FULL_CARD },
  // Standalone logo for docs/store listings: white field, inset rounded card.
  { file: 'logo.png', size: 512, glyphScale: 0.42, card: { half: 0.34, radius: 0.13 }, background: [255, 255, 255] },
  // Watermark for the terminal background: glyph only, tinted at runtime.
  { file: 'watermark.png', size: 512, glyphScale: 0.78 },
  // Adaptive icon layers: glyph sits inside the 66% safe zone, card colour is the background.
  { file: 'android-icon-foreground.png', size: 512, glyphScale: 0.44 },
  { file: 'android-icon-background.png', size: 512, glyphScale: 0.44, card: FULL_CARD },
  { file: 'android-icon-monochrome.png', size: 432, glyphScale: 0.44 },
];

for (const asset of assets) {
  const rgba = renderLogo(asset.size, asset);
  const png = encodePng(asset.size, asset.size, rgba);
  fs.writeFileSync(path.join(OUT, asset.file), png);
  console.log(`wrote ${asset.file} ${asset.size}x${asset.size} (${png.length} bytes)`);
}
