export function levenshtein(a: string, b: string): number {
  if (a === b) return 0;
  if (!a) return b.length;
  if (!b) return a.length;

  let previous = Array.from({ length: b.length + 1 }, (_, i) => i);
  let current = new Array<number>(b.length + 1);

  for (let i = 1; i <= a.length; i += 1) {
    current[0] = i;
    for (let j = 1; j <= b.length; j += 1) {
      const substitution = previous[j - 1] + (a[i - 1] === b[j - 1] ? 0 : 1);
      current[j] = Math.min(substitution, current[j - 1] + 1, previous[j] + 1);
    }
    const swap = previous;
    previous = current;
    current = swap;
  }
  return previous[b.length];
}

export function score(query: string, candidate: string): number {
  if (query === candidate) return 0;
  let distance = levenshtein(query.toLowerCase(), candidate.toLowerCase());
  if (candidate.toLowerCase().startsWith(query.charAt(0).toLowerCase())) distance -= 1;
  if (candidate.toLowerCase().startsWith(query.toLowerCase())) distance -= 1;
  return Math.max(0, distance);
}

export function suggest(query: string, candidates: string[], max = 3): string[] {
  const needle = query.trim().toLowerCase();
  if (!needle) return [];

  return candidates
    .map((candidate) => ({ candidate, value: score(needle, candidate) }))
    .filter(({ value }) => value < Math.max(3, needle.length))
    .sort((a, b) => a.value - b.value || a.candidate.localeCompare(b.candidate))
    .map(({ candidate }) => candidate)
    .filter((candidate, index, all) => all.indexOf(candidate) === index)
    .slice(0, max);
}
