import React from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { formatBytes, formatUptime } from '../format';
import { getStorageInfo, openSettingsPanel, SETTINGS_PANELS } from '../device';
import { MONO_FONT } from '../types';
import { usePalette } from '../ui';
import type { StatusSnapshot } from '../useStatus';

interface Props {
  status: StatusSnapshot | null;
  onRefresh: () => void;
}

export function SystemScreen({ status, onRefresh }: Props) {
  const palette = usePalette();
  const [storage, setStorage] = React.useState<{ free: number | null; total: number | null } | null>(null);

  React.useEffect(() => {
    void getStorageInfo().then(setStorage);
  }, []);

  const rows: Array<[string, string]> = status
    ? [
        ['MODEL', `${status.device.manufacturer} ${status.device.model}`],
        ['RUNTIME', `${status.device.osName} ${status.device.osVersion}`],
        ['HOST', status.device.packageName],
        ['BATTERY', `${status.battery.percent ?? 'UNKNOWN'}%`],
        ['POWER', status.battery.state],
        ['NETWORK', status.network.connected ? status.network.type : 'OFFLINE'],
        ['REACHABLE', status.network.reachable == null ? 'UNKNOWN' : status.network.reachable ? 'YES' : 'NO'],
        ['IP', status.network.ip ?? 'UNKNOWN'],
        ['STORAGE', storage?.free == null ? 'UNAVAILABLE IN EXPO GO' : `${formatBytes(storage.free)} FREE`],
        ['UPTIME', 'SEE uptime COMMAND'],
      ]
    : [['STATUS', 'LOADING…']];

  return (
    <ScrollView style={{ backgroundColor: palette.background }} contentContainerStyle={styles.content}>
      <View style={styles.header}>
        <Text style={[styles.title, { color: palette.accent }]}>SYSTEM MONITOR</Text>
        <Pressable onPress={onRefresh} accessibilityRole="button" accessibilityLabel="Refresh system info" hitSlop={8}>
          <Text style={[styles.action, { color: palette.secondary }]}>REFRESH</Text>
        </Pressable>
      </View>
      <View style={[styles.divider, { backgroundColor: palette.border }]} />

      {rows.map(([label, value]) => (
        <View key={label} style={styles.row}>
          <Text style={[styles.label, { color: palette.secondary }]}>{label}</Text>
          <Text style={[styles.value, { color: palette.primary }]} numberOfLines={2}>
            {value}
          </Text>
        </View>
      ))}

      <Text style={[styles.section, { color: palette.accent }]}>SYSTEM SETTINGS</Text>
      <View style={styles.buttons}>
        {SETTINGS_PANELS.filter((panel) => panel !== 'appNotifications' && panel !== 'settings').map((panel) => (
          <Pressable
            key={panel}
            onPress={() => void openSettingsPanel(panel)}
            accessibilityRole="button"
            style={[styles.button, { borderColor: palette.border }]}
          >
            <Text style={[styles.buttonText, { color: palette.secondary }]}>{panel.toUpperCase()}</Text>
          </Pressable>
        ))}
      </View>

      <Text style={[styles.note, { color: palette.secondary }]}>
        Values come from expo-battery, expo-network and expo-device. Memory and per-app storage are
        not exposed to Expo Go; the native build reports them.
      </Text>
      <Text style={[styles.note, { color: palette.secondary }]}>
        Uptime helper: {formatUptime(0)} is the display format used by the uptime command.
      </Text>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  content: { paddingHorizontal: 16, paddingTop: 12, paddingBottom: 24 },
  header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  title: { fontFamily: MONO_FONT, fontSize: 14, fontWeight: '700', letterSpacing: 2 },
  action: { fontFamily: MONO_FONT, fontSize: 11 },
  divider: { height: StyleSheet.hairlineWidth, marginTop: 8, marginBottom: 10 },
  row: { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 5, gap: 12 },
  label: { fontFamily: MONO_FONT, fontSize: 11, letterSpacing: 0.8, flex: 1 },
  value: { fontFamily: MONO_FONT, fontSize: 11, textAlign: 'right', flexShrink: 1 },
  section: { fontFamily: MONO_FONT, fontSize: 11, fontWeight: '700', letterSpacing: 1.5, marginTop: 20, marginBottom: 8 },
  buttons: { flexDirection: 'row', flexWrap: 'wrap', gap: 6 },
  button: { borderWidth: StyleSheet.hairlineWidth, paddingHorizontal: 10, paddingVertical: 8, borderRadius: 2 },
  buttonText: { fontFamily: MONO_FONT, fontSize: 10, letterSpacing: 0.8 },
  note: { fontFamily: MONO_FONT, fontSize: 10, lineHeight: 15, marginTop: 16 },
});
