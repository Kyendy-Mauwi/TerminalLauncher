import { Platform } from 'react-native';
import {
  canOpenUrl,
  isApplicationInstalled,
  launchApplication,
  openUrl,
  startIntent,
} from './device';
import type { AliasMap } from './storage';

export interface CatalogApp {
  id: string;
  label: string;
  packageName: string;
  /** Deep link used when the package cannot be resolved directly (OEM variants, iOS). */
  scheme?: string;
  intentAction?: string;
  intentData?: string;
  intentCategory?: string;
  /** Non-store destination used only when the app is genuinely absent. */
  fallbackUrl?: string;
  aliases?: string[];
}

/**
 * Expo Go cannot enumerate installed packages, so this catalogue is the explicit replacement.
 *
 * Every entry launches the installed app directly through its own launcher activity
 * (`PackageManager.getLaunchIntentForPackage`) and falls back to the app's public deep link or a
 * system intent action for OEM-specific system apps. No entry redirects to the Play Store: if the
 * app is not installed the launcher reports that instead of sending the user shopping.
 */
export const APP_CATALOG: CatalogApp[] = [
  {
    id: 'chrome',
    label: 'Chrome',
    packageName: 'com.android.chrome',
    fallbackUrl: 'https://www.google.com',
    aliases: ['browser', 'web'],
  },
  {
    id: 'youtube',
    label: 'YouTube',
    packageName: 'com.google.android.youtube',
    scheme: 'vnd.youtube://',
  },
  {
    id: 'maps',
    label: 'Maps',
    packageName: 'com.google.android.apps.maps',
    scheme: 'geo:0,0?q=',
    aliases: ['navigation'],
  },
  {
    id: 'gmail',
    label: 'Gmail',
    packageName: 'com.google.android.gm',
    scheme: 'mailto:',
    aliases: ['mail', 'email'],
  },
  {
    id: 'whatsapp',
    label: 'WhatsApp',
    packageName: 'com.whatsapp',
    scheme: 'whatsapp://',
    aliases: ['wa'],
  },
  {
    id: 'instagram',
    label: 'Instagram',
    packageName: 'com.instagram.android',
    scheme: 'instagram://app',
  },
  {
    id: 'spotify',
    label: 'Spotify',
    packageName: 'com.spotify.music',
    scheme: 'spotify://',
    aliases: ['music'],
  },
  {
    id: 'telegram',
    label: 'Telegram',
    packageName: 'org.telegram.messenger',
    scheme: 'tg://',
  },
  {
    id: 'calendar',
    label: 'Calendar',
    packageName: 'com.google.android.calendar',
    intentAction: 'android.intent.action.VIEW',
    intentData: 'content://com.android.calendar/time',
  },
  {
    id: 'camera',
    label: 'Camera',
    packageName: 'com.android.camera',
    intentAction: 'android.media.action.STILL_IMAGE_CAMERA',
    aliases: ['photo'],
  },
  {
    id: 'clock',
    label: 'Clock',
    packageName: 'com.google.android.deskclock',
    intentAction: 'android.intent.action.SHOW_ALARMS',
    aliases: ['alarm'],
  },
  {
    id: 'contacts',
    label: 'Contacts',
    packageName: 'com.android.contacts',
    intentAction: 'android.intent.action.VIEW',
    intentData: 'content://contacts/people/',
  },
  {
    id: 'messages',
    label: 'Messages',
    packageName: 'com.google.android.apps.messaging',
    scheme: 'sms:',
  },
  {
    id: 'phone',
    label: 'Phone',
    packageName: 'com.android.dialer',
    scheme: 'tel:',
  },
  {
    id: 'settings',
    label: 'Settings',
    packageName: 'com.android.settings',
    intentAction: 'android.settings.SETTINGS',
  },
  {
    id: 'playstore',
    label: 'Play Store',
    packageName: 'com.android.vending',
    scheme: 'market://',
    fallbackUrl: 'https://play.google.com/store/apps',
    aliases: ['store', 'play'],
  },
];

export function allAliasTargets(aliases: AliasMap): Record<string, string> {
  const map: Record<string, string> = {};
  for (const app of APP_CATALOG) {
    for (const alias of app.aliases ?? []) map[alias] = app.id;
    map[app.label.toLowerCase()] = app.id;
  }
  for (const [appId, alias] of Object.entries(aliases)) {
    if (alias) map[alias.toLowerCase()] = appId;
  }
  return map;
}

export function resolveApp(query: string, aliases: AliasMap): CatalogApp[] {
  const needle = query.trim().toLowerCase();
  if (!needle) return [];

  const direct = APP_CATALOG.filter(
    (app) =>
      app.label.toLowerCase() === needle ||
      app.id === needle ||
      (app.aliases ?? []).includes(needle)
  );
  if (direct.length > 0) return direct;

  const aliasTarget = Object.entries(aliases).find(([, alias]) => alias.toLowerCase() === needle);
  if (aliasTarget) {
    const match = APP_CATALOG.find((app) => app.id === aliasTarget[0]);
    if (match) return [match];
  }

  const prefix = APP_CATALOG.filter(
    (app) => app.label.toLowerCase().startsWith(needle) || app.packageName.toLowerCase().startsWith(needle)
  );
  if (prefix.length > 0) return prefix;

  return APP_CATALOG.filter(
    (app) => app.label.toLowerCase().includes(needle) || app.packageName.includes(needle)
  );
}

export function appSuggestions(query: string): string[] {
  const needle = query.trim().toLowerCase();
  if (!needle) return [];
  return APP_CATALOG.map((app) => app.label)
    .filter((label) => label.toLowerCase()[0] === needle[0])
    .slice(0, 4);
}

export async function probeApp(app: CatalogApp): Promise<boolean> {
  if (Platform.OS === 'android' && (await isApplicationInstalled(app.packageName))) {
    return true;
  }
  if (app.scheme && (await canOpenUrl(app.scheme))) {
    return true;
  }
  // System apps are reachable through their intent action even when package visibility hides
  // their package from queries, so they are treated as available.
  return Platform.OS === 'android' && Boolean(app.intentAction);
}

export interface OpenResult {
  ok: boolean;
  message: string;
}

export async function launchCatalogApp(app: CatalogApp): Promise<OpenResult> {
  // 1. Direct launch of the installed package's own launcher activity.
  if (await launchApplication(app.packageName)) {
    return { ok: true, message: `${app.label} opened.` };
  }

  // 2. The app's public deep link (also covers OEM packages and non-Android platforms).
  if (app.scheme && (await openUrl(app.scheme))) {
    return { ok: true, message: `${app.label} opened through its deep link.` };
  }

  // 3. System intent actions resolve OEM system apps whose package name varies.
  if (Platform.OS === 'android' && app.intentAction) {
    const launched = await startIntent(app.intentAction, {
      data: app.intentData,
      category: app.intentCategory,
    });
    if (launched) {
      return { ok: true, message: `${app.label} opened.` };
    }
    const launchedInPackage = await startIntent(app.intentAction, {
      packageName: app.packageName,
      data: app.intentData,
      category: app.intentCategory,
    });
    if (launchedInPackage) {
      return { ok: true, message: `${app.label} opened.` };
    }
  }

  // 4. A non-store fallback (for example the default browser instead of Chrome).
  if (app.fallbackUrl && (await openUrl(app.fallbackUrl))) {
    return {
      ok: false,
      message: `${app.label} is not installed. Opened the next best app instead.`,
    };
  }

  return { ok: false, message: `${app.label} is not installed on this device.` };
}
