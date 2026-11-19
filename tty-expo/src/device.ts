import * as Battery from 'expo-battery';
import * as Network from 'expo-network';
import * as Device from 'expo-device';
import * as Application from 'expo-application';
import * as Brightness from 'expo-brightness';
import * as IntentLauncher from 'expo-intent-launcher';
import * as Linking from 'expo-linking';
import * as FileSystem from 'expo-file-system';
import { Platform } from 'react-native';
import { formatBytes } from './format';

export interface BatteryInfo {
  percent: number | null;
  charging: boolean;
  state: string;
}

export interface NetworkInfo {
  connected: boolean;
  type: string;
  reachable: boolean | null;
  ip: string | null;
}

export interface DeviceInfo {
  model: string;
  manufacturer: string;
  osName: string;
  osVersion: string;
  isDevice: boolean;
  totalMemory: number | null;
  appVersion: string;
  appBuild: string;
  packageName: string;
}

export interface StorageInfo {
  free: number | null;
  total: number | null;
}

type IntentExtras = Record<string, string | number | boolean>;

const SETTINGS_ACTIONS: Record<string, string> = {
  wifi: 'android.settings.WIFI_SETTINGS',
  bluetooth: 'android.settings.BLUETOOTH_SETTINGS',
  display: 'android.settings.DISPLAY_SETTINGS',
  sound: 'android.settings.SOUND_SETTINGS',
  battery: 'android.settings.BATTERY_SAVER_SETTINGS',
  storage: 'android.settings.INTERNAL_STORAGE_SETTINGS',
  apps: 'android.settings.APPLICATION_SETTINGS',
  location: 'android.settings.LOCATION_SOURCE_SETTINGS',
  notifications: 'android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS',
  appNotifications: 'android.settings.APP_NOTIFICATION_SETTINGS',
  home: 'android.settings.HOME_SETTINGS',
  settings: 'android.settings.SETTINGS',
};

export const SETTINGS_PANELS = Object.keys(SETTINGS_ACTIONS);

export async function getBatteryInfo(): Promise<BatteryInfo> {
  try {
    const level = await Battery.getBatteryLevelAsync();
    const state = await Battery.getBatteryStateAsync();
    const charging =
      state === Battery.BatteryState.CHARGING || state === Battery.BatteryState.FULL;
    return {
      percent: level >= 0 ? Math.round(level * 100) : null,
      charging,
      state:
        state === Battery.BatteryState.CHARGING
          ? 'CHARGING'
          : state === Battery.BatteryState.FULL
            ? 'FULL'
            : state === Battery.BatteryState.UNPLUGGED
              ? 'DISCHARGING'
              : 'UNKNOWN',
    };
  } catch {
    return { percent: null, charging: false, state: 'UNAVAILABLE' };
  }
}

export async function getNetworkInfo(): Promise<NetworkInfo> {
  try {
    const state = await Network.getNetworkStateAsync();
    let ip: string | null = null;
    try {
      ip = (await Network.getIpAddressAsync()) || null;
    } catch {
      ip = null;
    }
    return {
      connected: Boolean(state.isConnected),
      type: String(state.type ?? 'UNKNOWN').replace('NetworkStateType.', '').toUpperCase(),
      reachable: state.isInternetReachable ?? null,
      ip,
    };
  } catch {
    return { connected: false, type: 'UNAVAILABLE', reachable: null, ip: null };
  }
}

export function getDeviceInfo(): DeviceInfo {
  return {
    model: Device.modelName ?? 'UNKNOWN',
    manufacturer: Device.manufacturer ?? 'UNKNOWN',
    osName: Device.osName ?? Platform.OS,
    osVersion: Device.osVersion ?? 'UNKNOWN',
    isDevice: Boolean(Device.isDevice),
    totalMemory: typeof Device.totalMemory === 'number' ? Device.totalMemory : null,
    appVersion: Application.nativeApplicationVersion ?? '1.0.0',
    appBuild: Application.nativeBuildVersion ?? '0',
    packageName: Application.applicationId ?? 'host.exp.exponent',
  };
}

export async function getStorageInfo(): Promise<StorageInfo> {
  try {
    const legacy = FileSystem as unknown as {
      getFreeDiskStorageAsync?: () => Promise<number>;
      getTotalDiskCapacityAsync?: () => Promise<number>;
    };
    const free = legacy.getFreeDiskStorageAsync ? await legacy.getFreeDiskStorageAsync() : null;
    const total = legacy.getTotalDiskCapacityAsync ? await legacy.getTotalDiskCapacityAsync() : null;
    return { free, total };
  } catch {
    return { free: null, total: null };
  }
}

export async function getUptimeMillis(): Promise<number | null> {
  try {
    const uptime = await Device.getUptimeAsync();
    return typeof uptime === 'number' ? uptime : null;
  } catch {
    return null;
  }
}

export async function getBrightnessPercent(): Promise<number | null> {
  try {
    const value = await Brightness.getBrightnessAsync();
    return Math.round(value * 100);
  } catch {
    return null;
  }
}

export async function setBrightnessPercent(percent: number): Promise<boolean> {
  try {
    if (Platform.OS === 'android') {
      await Brightness.requestPermissionsAsync();
    }
    await Brightness.setBrightnessAsync(Math.min(1, Math.max(0.01, percent / 100)));
    return true;
  } catch {
    return false;
  }
}

export function describeStorage(info: StorageInfo): string {
  if (info.free == null && info.total == null) return 'UNAVAILABLE IN EXPO GO';
  return `${formatBytes(info.free ?? 0)} free / ${formatBytes(info.total ?? 0)} total`;
}

export async function openSettingsPanel(panel: string, extras?: IntentExtras): Promise<boolean> {
  const action = SETTINGS_ACTIONS[panel];
  if (!action) return false;

  if (Platform.OS !== 'android') {
    try {
      await Linking.openSettings();
      return true;
    } catch {
      return false;
    }
  }

  try {
    await IntentLauncher.startActivityAsync(action, extras ? { extra: extras } : undefined);
    return true;
  } catch {
    try {
      await Linking.openSettings();
      return true;
    } catch {
      return false;
    }
  }
}

export interface IntentOptions {
  packageName?: string;
  data?: string;
  category?: string;
  flags?: number;
  extra?: IntentExtras;
}

export async function startIntent(action: string, options?: IntentOptions): Promise<boolean> {
  if (Platform.OS !== 'android') return false;
  try {
    await IntentLauncher.startActivityAsync(action, options);
    return true;
  } catch {
    return false;
  }
}

/**
 * Launches an installed app through its launcher activity. Android resolves the entry point
 * itself (`PackageManager.getLaunchIntentForPackage`), so no deep link is involved and the user
 * lands in the app rather than a store page. Resolves false when the package is not installed.
 */
export async function launchApplication(packageName: string): Promise<boolean> {
  if (Platform.OS !== 'android') return false;
  try {
    await IntentLauncher.openApplication(packageName);
    return true;
  } catch {
    return false;
  }
}

export async function isApplicationInstalled(packageName: string): Promise<boolean> {
  if (Platform.OS !== 'android') return false;
  try {
    const icon = await IntentLauncher.getApplicationIconAsync(packageName);
    return Boolean(icon);
  } catch {
    return false;
  }
}

export async function startDocumentPicker(): Promise<boolean> {
  if (Platform.OS !== 'android') return false;
  try {
    await IntentLauncher.startActivityAsync('android.intent.action.OPEN_DOCUMENT', {
      type: '*/*',
      category: 'android.intent.category.OPENABLE',
    });
    return true;
  } catch {
    return false;
  }
}

export async function openUrl(url: string): Promise<boolean> {
  try {
    await Linking.openURL(url);
    return true;
  } catch {
    return false;
  }
}

export function canOpenUrl(url: string): Promise<boolean> {
  return Linking.canOpenURL(url).catch(() => false);
}
