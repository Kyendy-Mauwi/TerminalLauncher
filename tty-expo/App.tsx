import { StatusBar } from 'expo-status-bar';
import React, { useCallback, useEffect, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';
import { QuickControls } from './src/QuickControls';
import { AppsScreen } from './src/screens/AppsScreen';
import { CalculatorScreen } from './src/screens/CalculatorScreen';
import { SettingsScreen } from './src/screens/SettingsScreen';
import { SystemScreen } from './src/screens/SystemScreen';
import { TerminalScreen } from './src/screens/TerminalScreen';
import { loadSettings, saveSettings } from './src/storage';
import { DEFAULT_SETTINGS, MONO_FONT, paletteFor, type AppSettings, type TabKey } from './src/types';
import { ThemeProvider, usePalette } from './src/ui';
import { useStatus } from './src/useStatus';
import { useTerminal } from './src/useTerminal';

const TABS: Array<{ key: TabKey; label: string }> = [
  { key: 'terminal', label: 'TERM' },
  { key: 'apps', label: 'APPS' },
  { key: 'system', label: 'SYS' },
  { key: 'settings', label: 'SET' },
];

export default function App() {
  const [settings, setSettings] = useState<AppSettings>(DEFAULT_SETTINGS);
  const [ready, setReady] = useState(false);
  const [tab, setTab] = useState<TabKey>('terminal');
  const [quickControls, setQuickControls] = useState(false);

  useEffect(() => {
    void loadSettings().then((loaded) => {
      setSettings(loaded);
      setTab(loaded.defaultTab);
      setReady(true);
    });
  }, []);

  useEffect(() => {
    if (ready) void saveSettings(settings);
  }, [settings, ready]);

  const updateSettings = useCallback((patch: Partial<AppSettings>) => {
    setSettings((previous) => ({ ...previous, ...patch }));
  }, []);

  const terminal = useTerminal({ settings, updateSettings, ready, goTab: setTab });
  const { status, refresh } = useStatus(ready);

  const resetSettings = useCallback(() => {
    setSettings(DEFAULT_SETTINGS);
    void saveSettings(DEFAULT_SETTINGS);
  }, []);

  return (
    <ThemeProvider theme={settings.theme}>
      <StatusBar style="light" />
      <SafeAreaProvider>
        <Shell
          tab={tab}
          onTab={setTab}
          terminal={terminal}
          settings={settings}
          status={status}
          refresh={refresh}
          updateSettings={updateSettings}
          resetSettings={resetSettings}
          onOpenQuickControls={() => setQuickControls(true)}
        />
      </SafeAreaProvider>
      <QuickControls
        visible={quickControls}
        onClose={() => setQuickControls(false)}
        onNavigate={(next) => {
          setTab(next);
          setQuickControls(false);
        }}
      />
    </ThemeProvider>
  );
}

interface ShellProps {
  tab: TabKey;
  onTab: (tab: TabKey) => void;
  terminal: ReturnType<typeof useTerminal>;
  settings: AppSettings;
  status: ReturnType<typeof useStatus>['status'];
  refresh: () => Promise<void>;
  updateSettings: (patch: Partial<AppSettings>) => void;
  resetSettings: () => void;
  onOpenQuickControls: () => void;
}

function Shell({
  tab,
  onTab,
  terminal,
  settings,
  status,
  refresh,
  updateSettings,
  resetSettings,
  onOpenQuickControls,
}: ShellProps) {
  const palette = usePalette();

  return (
    <SafeAreaView
      edges={['top', 'bottom']}
      style={{ flex: 1, backgroundColor: palette.background }}
    >
      <View style={styles.content}>
        {tab === 'terminal' && (
          <TerminalScreen
            settings={settings}
            lines={terminal.lines}
            input={terminal.input}
            booting={terminal.booting}
            executing={terminal.executing}
            status={status}
            onChangeInput={terminal.setInput}
            onSubmit={() => void terminal.submit()}
            onSkip={terminal.skipBoot}
            onHistoryPrevious={terminal.historyPrevious}
            onHistoryNext={terminal.historyNext}
            onComplete={() => void terminal.completeInput()}
            onTripleTap={onOpenQuickControls}
          />
        )}
        {tab === 'apps' && <AppsScreen />}
        {tab === 'system' && <SystemScreen status={status} onRefresh={() => void refresh()} />}
        {tab === 'settings' && (
          <SettingsScreen settings={settings} onChange={updateSettings} onReset={resetSettings} />
        )}
        {tab === 'calculator' && <CalculatorScreen />}
      </View>

      <View style={[styles.tabBar, { borderTopColor: palette.border, backgroundColor: palette.background }]}>
        {TABS.map((entry) => {
          const active = tab === entry.key || (entry.key === 'terminal' && tab === 'calculator');
          return (
            <Pressable
              key={entry.key}
              onPress={() => onTab(entry.key)}
              accessibilityRole="tab"
              accessibilityState={{ selected: active }}
              accessibilityLabel={entry.label}
              style={({ pressed }) => [styles.tab, pressed && { opacity: 0.6 }]}
            >
              <Text
                style={[
                  styles.tabLabel,
                  { color: active ? palette.primary : palette.secondary },
                  active && styles.tabActive,
                ]}
              >
                {entry.label}
              </Text>
              <View
                style={{
                  height: 1,
                  width: 18,
                  marginTop: 4,
                  backgroundColor: active ? palette.primary : 'transparent',
                }}
              />
            </Pressable>
          );
        })}
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  content: { flex: 1 },
  tabBar: {
    flexDirection: 'row',
    borderTopWidth: StyleSheet.hairlineWidth,
    paddingBottom: 2,
  },
  tab: { flex: 1, alignItems: 'center', paddingTop: 12, paddingBottom: 12 },
  tabLabel: { fontFamily: MONO_FONT, fontSize: 11, letterSpacing: 1.2 },
  tabActive: { fontWeight: '700' },
});
