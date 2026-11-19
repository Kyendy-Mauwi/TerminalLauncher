import AsyncStorage from '@react-native-async-storage/async-storage';
import { DEFAULT_SETTINGS, type AppSettings } from './types';

const SETTINGS_KEY = 'tty.settings.v1';
const HISTORY_KEY = 'tty.history.v1';
const NOTES_KEY = 'tty.notes.v1';
const FAVORITES_KEY = 'tty.favorites.v1';
const ALIASES_KEY = 'tty.aliases.v1';

async function readJson<T>(key: string, fallback: T): Promise<T> {
  try {
    const raw = await AsyncStorage.getItem(key);
    if (!raw) return fallback;
    return JSON.parse(raw) as T;
  } catch {
    return fallback;
  }
}

async function writeJson(key: string, value: unknown): Promise<void> {
  try {
    await AsyncStorage.setItem(key, JSON.stringify(value));
  } catch {
    // Storage can be unavailable; the app still works for the current session.
  }
}

export async function loadSettings(): Promise<AppSettings> {
  const stored = await readJson<Partial<AppSettings>>(SETTINGS_KEY, {});
  return {
    ...DEFAULT_SETTINGS,
    ...stored,
    fontSize: clamp(Number(stored.fontSize ?? DEFAULT_SETTINGS.fontSize), 9, 22),
    opacity: clamp(Number(stored.opacity ?? DEFAULT_SETTINGS.opacity), 0.4, 1),
    historyLimit: clamp(Number(stored.historyLimit ?? DEFAULT_SETTINGS.historyLimit), 25, 1000),
  };
}

export function saveSettings(settings: AppSettings): Promise<void> {
  return writeJson(SETTINGS_KEY, settings);
}

function clamp(value: number, min: number, max: number): number {
  if (Number.isNaN(value)) return min;
  return Math.min(max, Math.max(min, value));
}

export async function loadHistory(): Promise<string[]> {
  const history = await readJson<string[]>(HISTORY_KEY, []);
  return Array.isArray(history) ? history.filter((entry) => typeof entry === 'string') : [];
}

export async function recordHistory(command: string, limit: number): Promise<string[]> {
  const history = await loadHistory();
  if (history[history.length - 1] === command) return history;
  const next = [...history, command].slice(-limit);
  await writeJson(HISTORY_KEY, next);
  return next;
}

export async function clearHistory(): Promise<void> {
  await writeJson(HISTORY_KEY, []);
}

export interface Note {
  id: string;
  content: string;
  createdAt: number;
}

export async function loadNotes(): Promise<Note[]> {
  return readJson<Note[]>(NOTES_KEY, []);
}

export async function addNote(content: string): Promise<Note> {
  const notes = await loadNotes();
  const note: Note = { id: String(Date.now()), content, createdAt: Date.now() };
  await writeJson(NOTES_KEY, [note, ...notes].slice(0, 200));
  return note;
}

export async function removeNote(id: string): Promise<void> {
  const notes = await loadNotes();
  await writeJson(NOTES_KEY, notes.filter((note) => note.id !== id));
}

export async function clearNotes(): Promise<void> {
  await writeJson(NOTES_KEY, []);
}

export interface Favorite {
  id: string;
  label: string;
}

export async function loadFavorites(): Promise<Favorite[]> {
  return readJson<Favorite[]>(FAVORITES_KEY, []);
}

export async function addFavorite(favorite: Favorite): Promise<void> {
  const favorites = await loadFavorites();
  if (favorites.some((entry) => entry.id === favorite.id)) return;
  await writeJson(FAVORITES_KEY, [...favorites, favorite]);
}

export async function removeFavorite(id: string): Promise<void> {
  const favorites = await loadFavorites();
  await writeJson(FAVORITES_KEY, favorites.filter((entry) => entry.id !== id));
}

export type AliasMap = Record<string, string>;

export async function loadAliases(): Promise<AliasMap> {
  return readJson<AliasMap>(ALIASES_KEY, {});
}

export async function setAlias(appId: string, alias: string): Promise<void> {
  const aliases = await loadAliases();
  aliases[appId] = alias;
  await writeJson(ALIASES_KEY, aliases);
}

export async function removeAlias(appId: string): Promise<void> {
  const aliases = await loadAliases();
  delete aliases[appId];
  await writeJson(ALIASES_KEY, aliases);
}
