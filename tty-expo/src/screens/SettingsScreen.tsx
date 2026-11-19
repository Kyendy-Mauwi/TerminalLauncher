import React, { useEffect, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Switch, Text, TextInput, View } from 'react-native';
import { MONO_FONT, type AppSettings, type CursorStyle, type ThemeId } from '../types';
import { usePalette } from '../ui';
import { useKeyboardInset } from '../useKeyboardInset';

interface Props {
  settings: AppSettings;
  onChange: (patch: Partial<AppSettings>) => void;
  onReset: () => void;
}

const THEME_IDS: ThemeId[] = ['green', 'amber', 'cyan', 'matrix', 'mono'];
const CURSORS: CursorStyle[] = ['block', 'bar', 'underline'];

export function SettingsScreen({ settings, onChange, onReset }: Props) {
  const palette = usePalette();
  const keyboard = useKeyboardInset();

  return (
    <View style={{ flex: 1 }} onLayout={keyboard.onLayout}>
      <ScrollView
        style={{ backgroundColor: palette.background }}
        contentContainerStyle={[styles.content, { paddingBottom: 24 + keyboard.inset }]}
        keyboardShouldPersistTaps="handled"
      >
        <Text style={[styles.title, { color: palette.accent }]}>SETTINGS</Text>
        <View style={[styles.divider, { backgroundColor: palette.border }]} />

        <Text style={[styles.section, { color: palette.accent }]}>IDENTITY</Text>
        <TextSetting label="USERNAME" value={settings.username} onCommit={(value) => onChange({ username: value.replace(/[^A-Za-z0-9._-]/g, '').slice(0, 16) || 'KYENDY' })} />
        <TextSetting label="DEVICE ALIAS" value={settings.hostname} onCommit={(value) => onChange({ hostname: value.replace(/[^A-Za-z0-9._-]/g, '').slice(0, 16) || 'ANDROID' })} />
        <TextSetting label="STARTUP MESSAGE" value={settings.startupMessage} onCommit={(value) => onChange({ startupMessage: value.slice(0, 40) })} />
        <Text style={[styles.hint, { color: palette.secondary }]}>
          PROMPT: {settings.username.toUpperCase()}@{settings.hostname.toUpperCase()}:~$
        </Text>

        <Text style={[styles.section, { color: palette.accent }]}>APPEARANCE</Text>
        <Text style={[styles.label, { color: palette.secondary }]}>THEME</Text>
        <View style={styles.wrap}>
          {THEME_IDS.map((theme) => (
            <Choice
              key={theme}
              label={theme.toUpperCase()}
              selected={settings.theme === theme}
              onPress={() => onChange({ theme })}
            />
          ))}
        </View>

        <Stepper
          label="FONT SIZE"
          value={`${settings.fontSize} sp`}
          onDecrease={() => onChange({ fontSize: Math.max(9, settings.fontSize - 1) })}
          onIncrease={() => onChange({ fontSize: Math.min(22, settings.fontSize + 1) })}
        />
        <Stepper
          label="OPACITY"
          value={`${Math.round(settings.opacity * 100)}%`}
          onDecrease={() => onChange({ opacity: Math.max(0.4, Number((settings.opacity - 0.1).toFixed(2))) })}
          onIncrease={() => onChange({ opacity: Math.min(1, Number((settings.opacity + 0.1).toFixed(2))) })}
        />

        <Text style={[styles.label, { color: palette.secondary }]}>CURSOR</Text>
        <View style={styles.wrap}>
          {CURSORS.map((cursor) => (
            <Choice
              key={cursor}
              label={cursor.toUpperCase()}
              selected={settings.cursor === cursor}
              onPress={() => onChange({ cursor })}
            />
          ))}
        </View>

        <Toggle label="ANIMATIONS" value={settings.animations} onChange={(value) => onChange({ animations: value })} />
        <Toggle label="BOOT SEQUENCE" value={settings.bootSequence} onChange={(value) => onChange({ bootSequence: value })} />
        <Toggle label="HAPTIC FEEDBACK" value={settings.haptics} onChange={(value) => onChange({ haptics: value })} />
        <Toggle label="TRIPLE-TAP QUICK CONTROLS" value={settings.tripleTap} onChange={(value) => onChange({ tripleTap: value })} />
        <Toggle label="SHOW STATUS PANEL" value={settings.showStatusPanel} onChange={(value) => onChange({ showStatusPanel: value })} />

        <Text style={[styles.section, { color: palette.accent }]}>TERMINAL</Text>
        <Stepper
          label="HISTORY LIMIT"
          value={String(settings.historyLimit)}
          onDecrease={() => onChange({ historyLimit: Math.max(25, settings.historyLimit - 25) })}
          onIncrease={() => onChange({ historyLimit: Math.min(1000, settings.historyLimit + 25) })}
        />
        <TextSetting label="DATE FORMAT" value={settings.dateFormat} onCommit={(value) => onChange({ dateFormat: value || 'yyyy-MM-dd' })} />
        <TextSetting label="TIME FORMAT" value={settings.timeFormat} onCommit={(value) => onChange({ timeFormat: value || 'HH:mm' })} />

        <Text style={[styles.label, { color: palette.secondary }]}>DEFAULT TAB</Text>
        <View style={styles.wrap}>
          <Choice label="TERMINAL" selected={settings.defaultTab === 'terminal'} onPress={() => onChange({ defaultTab: 'terminal' })} />
          <Choice label="APPS" selected={settings.defaultTab === 'apps'} onPress={() => onChange({ defaultTab: 'apps' })} />
        </View>

        <Text style={[styles.section, { color: palette.accent }]}>EXPO LIMITS</Text>
        <Text style={[styles.hint, { color: palette.secondary }]}>
          Launcher role, installed-app scanning, notification reading, volume and system Wi-Fi control
          require the native Kotlin build. Everything else here works offline via AsyncStorage and
          Expo modules.
        </Text>

        <Pressable
          onPress={onReset}
          accessibilityRole="button"
          style={[styles.reset, { borderColor: palette.error }]}
        >
          <Text style={{ color: palette.error, fontFamily: MONO_FONT, fontSize: 11, letterSpacing: 1 }}>
            [ RESET ALL SETTINGS ]
          </Text>
        </Pressable>

        <Text style={[styles.hint, { color: palette.secondary, marginBottom: 24 }]}>
          Settings persist on this device. Nothing is uploaded.
        </Text>
      </ScrollView>
    </View>
  );
}

function TextSetting({ label, value, onCommit }: { label: string; value: string; onCommit: (value: string) => void }) {
  const palette = usePalette();
  const [text, setText] = useState(value);

  useEffect(() => {
    setText(value);
  }, [value]);

  return (
    <View style={styles.textRow}>
      <View style={[styles.textField, { borderColor: palette.border, backgroundColor: `${palette.surface}88` }]}>
        <Text style={[styles.inputLabel, { color: palette.secondary }]}>{label}</Text>
        <TextInput
          value={text}
          onChangeText={setText}
          onSubmitEditing={() => onCommit(text)}
          style={[styles.input, { color: palette.primary }]}
          autoCapitalize="none"
          autoCorrect={false}
          selectionColor={palette.primary}
          returnKeyType="done"
          accessibilityLabel={label}
        />
      </View>
      <Pressable onPress={() => onCommit(text)} accessibilityRole="button" accessibilityLabel={`Apply ${label}`} style={styles.setButton}>
        <Text style={{ color: palette.accent, fontFamily: MONO_FONT, fontSize: 11 }}>SET</Text>
      </Pressable>
    </View>
  );
}

function Choice({ label, selected, onPress }: { label: string; selected: boolean; onPress: () => void }) {
  const palette = usePalette();
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityState={{ selected }}
      style={[
        styles.choice,
        {
          borderColor: selected ? palette.primary : palette.border,
          backgroundColor: selected ? `${palette.primary}22` : 'transparent',
        },
      ]}
    >
      <Text style={{ color: selected ? palette.primary : palette.secondary, fontFamily: MONO_FONT, fontSize: 10 }}>
        {label}
      </Text>
    </Pressable>
  );
}

function Stepper({
  label,
  value,
  onDecrease,
  onIncrease,
}: {
  label: string;
  value: string;
  onDecrease: () => void;
  onIncrease: () => void;
}) {
  const palette = usePalette();
  return (
    <View style={styles.stepper}>
      <Text style={[styles.stepperLabel, { color: palette.secondary }]}>{label}</Text>
      <Pressable onPress={onDecrease} accessibilityRole="button" accessibilityLabel={`Decrease ${label}`} hitSlop={8}>
        <Text style={{ color: palette.accent, fontFamily: MONO_FONT, fontSize: 16 }}>−</Text>
      </Pressable>
      <Text style={{ color: palette.primary, fontFamily: MONO_FONT, fontSize: 11, minWidth: 54, textAlign: 'center' }}>
        {value}
      </Text>
      <Pressable onPress={onIncrease} accessibilityRole="button" accessibilityLabel={`Increase ${label}`} hitSlop={8}>
        <Text style={{ color: palette.accent, fontFamily: MONO_FONT, fontSize: 16 }}>+</Text>
      </Pressable>
    </View>
  );
}

function Toggle({ label, value, onChange }: { label: string; value: boolean; onChange: (value: boolean) => void }) {
  const palette = usePalette();
  return (
    <View style={styles.toggle}>
      <Text style={[styles.toggleLabel, { color: palette.secondary }]}>{label}</Text>
      <Switch
        value={value}
        onValueChange={onChange}
        trackColor={{ false: palette.surface, true: `${palette.primary}77` }}
        thumbColor={value ? palette.primary : palette.secondary}
        accessibilityLabel={label}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  content: { paddingHorizontal: 16, paddingTop: 12 },
  title: { fontFamily: MONO_FONT, fontSize: 14, fontWeight: '700', letterSpacing: 2 },
  divider: { height: StyleSheet.hairlineWidth, marginTop: 8 },
  section: { fontFamily: MONO_FONT, fontSize: 11, fontWeight: '700', letterSpacing: 1.5, marginTop: 18, marginBottom: 6 },
  label: { fontFamily: MONO_FONT, fontSize: 10, letterSpacing: 1, marginTop: 6 },
  hint: { fontFamily: MONO_FONT, fontSize: 10, lineHeight: 15, marginTop: 6 },
  wrap: { flexDirection: 'row', flexWrap: 'wrap', marginTop: 4 },
  choice: { borderWidth: StyleSheet.hairlineWidth, paddingHorizontal: 10, paddingVertical: 7, borderRadius: 2, marginRight: 6, marginBottom: 6 },
  textRow: { flexDirection: 'row', alignItems: 'center', marginTop: 8, gap: 4 },
  textField: { flex: 1, borderWidth: StyleSheet.hairlineWidth, borderRadius: 2, paddingHorizontal: 8, paddingVertical: 4 },
  inputLabel: { fontFamily: MONO_FONT, fontSize: 8, letterSpacing: 0.8, marginTop: 2 },
  input: { fontFamily: MONO_FONT, fontSize: 13, paddingVertical: 4 },
  setButton: { paddingHorizontal: 8, paddingVertical: 10 },
  stepper: { flexDirection: 'row', alignItems: 'center', justifyContent: 'flex-end', gap: 10, paddingVertical: 8 },
  stepperLabel: { flex: 1, fontFamily: MONO_FONT, fontSize: 10, letterSpacing: 0.8 },
  toggle: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingVertical: 2 },
  toggleLabel: { fontFamily: MONO_FONT, fontSize: 10, letterSpacing: 0.8, flex: 1 },
  reset: { borderWidth: StyleSheet.hairlineWidth, paddingVertical: 12, alignItems: 'center', marginTop: 22, borderRadius: 2 },
});
