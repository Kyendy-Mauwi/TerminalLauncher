export interface ParsedCommand {
  raw: string;
  name: string;
  args: string[];
  rawArgs: string;
  warnings: string[];
}

/**
 * Quote-aware tokenizer, ported from the native implementation. Supports double/single quotes,
 * backslash escapes, arbitrary whitespace and never throws.
 */
export function parseCommand(input: string): ParsedCommand | null {
  const trimmed = input.trim();
  if (!trimmed) return null;

  const tokens: string[] = [];
  const warnings: string[] = [];
  let builder = '';
  let inToken = false;
  let quote: string | null = null;
  let escaped = false;
  let nameEndIndex = -1;

  for (let i = 0; i < trimmed.length; i += 1) {
    const char = trimmed[i];

    if (escaped) {
      builder += char;
      escaped = false;
      inToken = true;
      continue;
    }

    if (char === '\\' && quote !== "'") {
      escaped = true;
      inToken = true;
      continue;
    }

    if (quote) {
      if (char === quote) {
        quote = null;
      } else {
        builder += char;
      }
      continue;
    }

    if (char === '"' || char === "'") {
      quote = char;
      inToken = true;
      continue;
    }

    if (/\s/.test(char)) {
      if (inToken) {
        tokens.push(builder);
        builder = '';
        inToken = false;
        if (nameEndIndex < 0) nameEndIndex = i;
      }
      continue;
    }

    builder += char;
    inToken = true;
  }

  if (escaped) builder += '\\';
  if (quote) {
    warnings.push(`Unterminated ${quote === '"' ? 'double' : 'single'} quote.`);
  }
  if (inToken) {
    tokens.push(builder);
    if (nameEndIndex < 0) nameEndIndex = trimmed.length;
  }

  if (tokens.length === 0) return null;

  const name = tokens[0];
  const args = tokens.slice(1);
  const rawArgs = args.length === 0 ? '' : trimmed.substring(Math.min(nameEndIndex, trimmed.length)).trim();

  return { raw: trimmed, name: name.toLowerCase(), args, rawArgs, warnings };
}

export function normalizeCommand(input: string): string {
  return input.trim().replace(/\s+/g, ' ');
}
