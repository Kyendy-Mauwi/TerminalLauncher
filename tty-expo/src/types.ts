export type ThemeId = 'green' | 'amber' | 'cyan' | 'matrix' | 'mono';
export type CursorStyle = 'block' | 'bar' | 'underline';
export type TabKey = 'terminal' | 'apps' | 'system' | 'settings' | 'calculator';

export type LineStyle =
  | 'normal'
  | 'dim'
  | 'accent'
  | 'success'
  | 'warning'
  | 'error'
  | 'banner'
  | 'command';

export interface TerminalLine {
  id: number;
  text: string;
  style: LineStyle;
}

export interface AppSettings {
  username: string;
  hostname: string;
  theme: ThemeId;
  fontSize: number;
  cursor: CursorStyle;
  opacity: number;
  historyLimit: number;
  animations: boolean;
  startupMessage: string;
  dateFormat: string;
  timeFormat: string;
  haptics: boolean;
  bootSequence: boolean;
  tripleTap: boolean;
  showStatusPanel: boolean;
  defaultTab: 'terminal' | 'apps';
}

export const DEFAULT_SETTINGS: AppSettings = {
  username: 'KYENDY',
  hostname: 'ANDROID',
  theme: 'green',
  fontSize: 13,
  cursor: 'block',
  opacity: 1,
  historyLimit: 200,
  animations: true,
  startupMessage: 'WELCOME',
  dateFormat: 'yyyy-MM-dd',
  timeFormat: 'HH:mm',
  haptics: true,
  bootSequence: true,
  tripleTap: true,
  showStatusPanel: true,
  defaultTab: 'terminal',
};

export const THEMES: Record<ThemeId, {
  background: string;
  surface: string;
  primary: string;
  secondary: string;
  accent: string;
  warning: string;
  error: string;
  border: string;
  matrix: boolean;
}> = {
  green: {
    background: '#080B0F', surface: '#0E141A', primary: '#39FF88', secondary: '#8AAE9A',
    accent: '#00D9FF', warning: '#FFB547', error: '#FF5555', border: '#1B2A24', matrix: false,
  },
  amber: {
    background: '#0B0905', surface: '#171208', primary: '#FFB547', secondary: '#C9A167',
    accent: '#FFE0A3', warning: '#FF8A3D', error: '#FF5555', border: '#3A2C15', matrix: false,
  },
  cyan: {
    background: '#05090C', surface: '#0A1419', primary: '#00D9FF', secondary: '#7FB6C4',
    accent: '#39FF88', warning: '#FFB547', error: '#FF5555', border: '#153039', matrix: false,
  },
  matrix: {
    background: '#000000', surface: '#04120A', primary: '#00FF41', secondary: '#2E8B57',
    accent: '#B6FFC9', warning: '#FFD166', error: '#FF5555', border: '#0E3018', matrix: true,
  },
  mono: {
    background: '#050608', surface: '#101317', primary: '#E6EDF3', secondary: '#9AA7B2',
    accent: '#FFFFFF', warning: '#FFC66D', error: '#FF7B72', border: '#232A31', matrix: false,
  },
};

export const TAB_BAR_HEIGHT = 44;

export const MONO_FONT = 'monospace';

export function promptFor(settings: AppSettings): string {
  return `${settings.username.toUpperCase()}@${settings.hostname.toUpperCase()}:~$`;
}

export function paletteFor(theme: ThemeId) {
  return THEMES[theme] ?? THEMES.green;
}
