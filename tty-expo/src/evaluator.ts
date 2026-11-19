export type Evaluation = { ok: true; value: number } | { ok: false; reason: string };

/** Recursive-descent arithmetic evaluator. No eval(), no code execution. */
export function evaluate(expression: string): Evaluation {
  const source = expression.trim();
  if (!source) return { ok: false, reason: 'Empty expression.' };

  let index = 0;

  const skip = () => {
    while (index < source.length && /\s/.test(source[index])) index += 1;
  };

  const peek = (): string | null => {
    skip();
    return index < source.length ? source[index] : null;
  };

  const fail = (reason: string): never => {
    throw new Error(reason);
  };

  const parsePrimary = (): number => {
    const char = peek();
    if (char === '(') {
      index += 1;
      const value = parseExpression();
      if (peek() !== ')') fail('Missing closing parenthesis.');
      index += 1;
      return value;
    }

    skip();
    const start = index;
    while (index < source.length && /[0-9.]/.test(source[index])) index += 1;
    if (start === index) {
      fail(index < source.length ? `Unexpected character '${source[index]}'.` : 'Unexpected end of expression.');
    }
    const literal = source.substring(start, index);
    const parsed = Number(literal);
    if (Number.isNaN(parsed)) fail(`Invalid number '${literal}'.`);
    return parsed;
  };

  const parseUnary = (): number => {
    const char = peek();
    if (char === '-') {
      index += 1;
      return -parseUnary();
    }
    if (char === '+') {
      index += 1;
      return parseUnary();
    }
    return parsePrimary();
  };

  const parsePower = (): number => {
    const base = parseUnary();
    if (peek() === '^') {
      index += 1;
      return Math.pow(base, parsePower());
    }
    return base;
  };

  const parseTerm = (): number => {
    let value = parsePower();
    for (;;) {
      const char = peek();
      if (char === '*') {
        index += 1;
        value *= parsePower();
      } else if (char === '/') {
        index += 1;
        const divisor = parsePower();
        if (divisor === 0) fail('Division by zero.');
        value /= divisor;
      } else if (char === '%') {
        index += 1;
        const divisor = parsePower();
        if (divisor === 0) fail('Division by zero.');
        value %= divisor;
      } else {
        return value;
      }
    }
  };

  const parseExpression = (): number => {
    let value = parseTerm();
    for (;;) {
      const char = peek();
      if (char === '+') {
        index += 1;
        value += parseTerm();
      } else if (char === '-') {
        index += 1;
        value -= parseTerm();
      } else {
        return value;
      }
    }
  };

  try {
    const value = parseExpression();
    skip();
    if (index < source.length) {
      return { ok: false, reason: `Unexpected token near '${source.substring(index).trim()}'.` };
    }
    if (!Number.isFinite(value)) return { ok: false, reason: 'Result is not a finite number.' };
    return { ok: true, value };
  } catch (error) {
    return { ok: false, reason: error instanceof Error ? error.message : 'Invalid expression.' };
  }
}

export function formatNumber(value: number): string {
  if (Number.isInteger(value) && Math.abs(value) < 1e15) return String(value);
  return String(Number(value.toFixed(6)));
}
