import React from 'react';
import { Modal, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { openSettingsPanel, SETTINGS_PANELS } from './device';
import { MONO_FONT, type TabKey } from './types';
import { usePalette } from './ui';

interface Props {
  visible: boolean;
  onClose: () => void;
  onNavigate: (tab: TabKey) => void;
}

export function QuickControls({ visible, onClose, onNavigate }: Props) {
  const palette = usePalette();

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onClose}>
      <Pressable style={styles.backdrop} onPress={onClose} accessibilityLabel="Close quick controls">
        <View />
      </Pressable>
      <View style={[styles.sheet, { backgroundColor: palette.surface, borderColor: palette.border }]}>
        <ScrollView keyboardShouldPersistTaps="handled">
          <Text style={[styles.title, { color: palette.accent }]}>QUICK CONTROLS</Text>
          <Text style={[styles.subtitle, { color: palette.secondary }]}>
            Opened from the terminal header. Tap the header three times.
          </Text>

          <Text style={[styles.section, { color: palette.accent }]}>TERMINAL</Text>
          <Item label="RESUME TERMINAL" onPress={() => onNavigate('terminal')} />
          <Item label="T3rm!n@l SETTINGS" onPress={() => onNavigate('settings')} />
          <Item label="SYSTEM MONITOR" onPress={() => onNavigate('system')} />
          <Item label="APPLICATIONS" onPress={() => onNavigate('apps')} />
          <Item label="CALCULATOR" onPress={() => onNavigate('calculator')} />

          <Text style={[styles.section, { color: palette.accent }]}>ANDROID SETTINGS</Text>
          {SETTINGS_PANELS.filter((panel) => panel !== 'appNotifications').map((panel) => (
            <Item key={panel} label={panel.toUpperCase()} onPress={() => void openSettingsPanel(panel)} />
          ))}

          <Pressable onPress={onClose} accessibilityRole="button" style={styles.close}>
            <Text style={{ color: palette.secondary, fontFamily: MONO_FONT, fontSize: 11 }}>[ CLOSE ]</Text>
          </Pressable>
        </ScrollView>
      </View>
    </Modal>
  );
}

function Item({ label, onPress }: { label: string; onPress: () => void }) {
  const palette = usePalette();
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={label}
      style={({ pressed }) => [styles.item, { opacity: pressed ? 0.6 : 1 }]}
    >
      <Text style={{ color: palette.primary, fontFamily: MONO_FONT, fontSize: 12 }}>  {label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  backdrop: { flex: 1, backgroundColor: 'rgba(0,0,0,0.6)' },
  sheet: {
    position: 'absolute',
    left: 0,
    right: 0,
    bottom: 0,
    maxHeight: '75%',
    borderTopWidth: StyleSheet.hairlineWidth,
    paddingHorizontal: 20,
    paddingTop: 16,
    paddingBottom: 24,
  },
  title: { fontFamily: MONO_FONT, fontSize: 13, fontWeight: '700', letterSpacing: 2 },
  subtitle: { fontFamily: MONO_FONT, fontSize: 10, marginTop: 4 },
  section: { fontFamily: MONO_FONT, fontSize: 10, fontWeight: '700', letterSpacing: 1.2, marginTop: 16, marginBottom: 4 },
  item: { paddingVertical: 11 },
  close: { alignItems: 'center', paddingVertical: 14 },
});
