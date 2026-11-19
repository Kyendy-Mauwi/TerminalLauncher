import { useCallback, useEffect, useState } from 'react';
import { getBatteryInfo, getDeviceInfo, getNetworkInfo, type BatteryInfo, type DeviceInfo, type NetworkInfo } from './device';

export interface StatusSnapshot {
  battery: BatteryInfo;
  network: NetworkInfo;
  device: DeviceInfo;
}

export function useStatus(active: boolean, intervalMs = 20_000) {
  const [status, setStatus] = useState<StatusSnapshot | null>(null);

  const refresh = useCallback(async () => {
    const [battery, network] = await Promise.all([getBatteryInfo(), getNetworkInfo()]);
    setStatus({ battery, network, device: getDeviceInfo() });
  }, []);

  useEffect(() => {
    if (!active) return;
    let mounted = true;
    void refresh();
    const id = setInterval(() => {
      if (mounted) void refresh();
    }, intervalMs);
    return () => {
      mounted = false;
      clearInterval(id);
    };
  }, [active, intervalMs, refresh]);

  return { status, refresh };
}
