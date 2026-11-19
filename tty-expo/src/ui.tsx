import React, { createContext, useContext } from 'react';
import { Pressable, StyleSheet, Text, View, type StyleProp, type TextStyle, type ViewStyle } from 'react-native';
import { MONO_FONT, paletteFor, type ThemeId } from './types';

export type Palette = ReturnType<typeof paletteFor>;

export const PaletteContext = createContext<Palette>(paletteFor('green'));

export function usePalette(): Palette {
  return useContext(PaletteContext);
}

export function ThemeProvider({ theme, children }: { theme: ThemeId; children: React.ReactNode }) {
  return <PaletteContext.Provider value={paletteFor(theme)}>{children}</PaletteContext.Provider>;
}

interface ButtonProps {
  label: string;
  onPress: () => void;
  active?: boolean;
  disabled?: boolean;
  accessibilityLabel?: string;
  style?: StyleProp<ViewStyle>;
  textStyle?: StyleProp<TextStyle>;
}

export function TerminalButton({
  label,
  onPress,
  active = false,
  disabled = false,
  accessibilityLabel,
  style,
  textStyle,
}: ButtonProps) {
  const palette = usePalette();
  return (
    <Pressable
      onPress={onPress}
      disabled={disabled}
      accessibilityRole="button"
      accessibilityLabel={accessibilityLabel ?? label}
      hitSlop={6}
      style={({ pressed }) => [
        styles.button,
        active && { backgroundColor: `${palette.primary}22` },
        pressed && !disabled && { opacity: 0.6 },
        style,
      ]}
    >
      <Text
        style={[
          styles.buttonText,
          { color: active ? palette.primary : disabled ? `${palette.secondary}66` : palette.secondary },
          active && { fontWeight: '700' },
          textStyle,
        ]}
      >
        {label}
      </Text>
    </Pressable>
  );
}

export function Divider({ color }: { color?: string }) {
  const palette = usePalette();
  return <View style={{ height: StyleSheet.hairlineWidth, backgroundColor: color ?? palette.border }} />;
}

export function SectionLabel({ children }: { children: React.ReactNode }) {
  const palette = usePalette();
  return (
    <Text style={[styles.section, { color: palette.accent }]}>
      {children}
    </Text>
  );
}

export function FieldRow({ label, value }: { label: string; value: string }) {
  const palette = usePalette();
  return (
    <View style={styles.fieldRow}>
      <Text style={[styles.fieldLabel, { color: palette.secondary }]}>{label.toUpperCase()}</Text>
      <Text style={[styles.fieldValue, { color: palette.primary }]} numberOfLines={2}>
        {value}
      </Text>
    </View>
  );
}

export function Chip({
  label,
  selected,
  onPress,
}: {
  label: string;
  selected: boolean;
  onPress: () => void;
}) {
  const palette = usePalette();
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityState={{ selected }}
      style={[
        styles.chip,
        {
          backgroundColor: selected ? `${palette.primary}26` : 'transparent',
          borderColor: selected ? palette.primary : palette.border,
        },
      ]}
    >
      <Text style={{ color: selected ? palette.primary : palette.secondary, fontSize: 11 }}>{label}</Text>
    </Pressable>
  );
}

export const textStyles = StyleSheet.create({
  mono: { fontFamily: MONO_FONT },
});

const styles = StyleSheet.create({
  button: {
    paddingVertical: 10,
    paddingHorizontal: 10,
    alignItems: 'center',
    justifyContent: 'center',
  },
  buttonText: {
    fontFamily: MONO_FONT,
    fontSize: 11,
    letterSpacing: 1,
  },
  section: {
    fontFamily: MONO_FONT,
    fontSize: 11,
    letterSpacing: 1.5,
    fontWeight: '700',
    marginTop: 18,
    marginBottom: 6,
  },
  fieldRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
    paddingVertical: 4,
    gap: 12,
  },
  fieldLabel: {
    fontFamily: MONO_FONT,
    fontSize: 11,
    letterSpacing: 0.8,
    flex: 1,
  },
  fieldValue: {
    fontFamily: MONO_FONT,
    fontSize: 11,
    textAlign: 'right',
    flexShrink: 1,
  },
  chip: {
    borderWidth: StyleSheet.hairlineWidth,
    paddingHorizontal: 10,
    paddingVertical: 7,
    borderRadius: 2,
    marginRight: 6,
    marginBottom: 6,
  },
});
