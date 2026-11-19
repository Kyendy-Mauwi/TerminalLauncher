import { requireNativeModule } from 'expo';

export interface InstalledApp {
  packageName: string;
  label: string;
  system: boolean;
}

interface AppListNativeModule {
  listApps(): Promise<InstalledApp[]>;
  openApp(packageName: string): Promise<boolean>;
}

let cached: AppListNativeModule | null | undefined;

/** Resolves the native module once; null inside Expo Go, where it is not bundled. */
export function getAppListModule(): AppListNativeModule | null {
  if (cached === undefined) {
    try {
      cached = requireNativeModule<AppListNativeModule>('AppList');
    } catch {
      cached = null;
    }
  }
  return cached;
}

export async function listInstalledApps(): Promise<InstalledApp[]> {
  const native = getAppListModule();
  if (!native) return [];
  try {
    return await native.listApps();
  } catch {
    return [];
  }
}

export async function openInstalledApp(packageName: string): Promise<boolean> {
  const native = getAppListModule();
  if (!native) return false;
  try {
    return await native.openApp(packageName);
  } catch {
    return false;
  }
}
