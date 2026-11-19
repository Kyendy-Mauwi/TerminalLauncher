export function formatBytes(value: number): string {
  if (!Number.isFinite(value) || value <= 0) return 'UNKNOWN';
  const kb = 1024;
  const mb = kb * 1024;
  const gb = mb * 1024;
  if (value >= gb) return `${(value / gb).toFixed(1)} GB`;
  if (value >= mb) return `${(value / mb).toFixed(1)} MB`;
  if (value >= kb) return `${Math.round(value / kb)} KB`;
  return `${Math.round(value)} B`;
}

export function formatUptime(millis: number): string {
  const total = Math.max(0, Math.floor(millis / 1000));
  const hours = Math.floor(total / 3600);
  const minutes = Math.floor((total % 3600) / 60);
  const seconds = total % 60;
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`;
}

export function formatShortDuration(millis: number): string {
  const total = Math.max(0, Math.floor(millis / 1000));
  if (total < 60) return `${total}s`;
  if (total < 3600) return `${Math.floor(total / 60)}m ${total % 60}s`;
  return `${Math.floor(total / 3600)}h ${Math.floor((total % 3600) / 60)}m`;
}

/**
 * Minimal pattern formatter supporting yyyy, MM, dd, HH, mm, ss so users can set their own
 * date/time formats without pulling in a date library.
 */
export function formatDatePattern(date: Date, pattern: string): string {
  const pad = (value: number) => String(value).padStart(2, '0');
  const safe = pattern && pattern.trim() ? pattern : 'yyyy-MM-dd HH:mm';
  return safe
    .replace(/yyyy/g, String(date.getFullYear()))
    .replace(/MM/g, pad(date.getMonth() + 1))
    .replace(/dd/g, pad(date.getDate()))
    .replace(/HH/g, pad(date.getHours()))
    .replace(/mm/g, pad(date.getMinutes()))
    .replace(/ss/g, pad(date.getSeconds()));
}

export function padRight(value: string, width: number): string {
  return value.length >= width ? value : value + ' '.repeat(width - value.length);
}
