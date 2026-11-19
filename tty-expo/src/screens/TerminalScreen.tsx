import React, { useEffect, useRef, useState } from 'react';
import { Image, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import * as Haptics from 'expo-haptics';
import { formatDatePattern } from '../format';
import { MONO_FONT, promptFor, type AppSettings, type LineStyle, type TerminalLine } from '../types';
import { usePalette } from '../ui';
import { useKeyboardInset } from '../useKeyboardInset';
import type { StatusSnapshot } from '../useStatus';

interface Props {
  settings: AppSettings;
  lines: TerminalLine[];
  input: string;
  booting: boolean;
  executing: boolean;
  status: StatusSnapshot | null;
  onChangeInput: (value: string) => void;
  onSubmit: () => void;
  onSkip: () => void;
  onHistoryPrevious: () => void;
  onHistoryNext: () => void;
  onComplete: () => void;
  onTripleTap: () => void;
}

export function TerminalScreen(props: Props) {
  const {
    settings,
    lines,
    input,
    booting,
    executing,
    status,
    onChangeInput,
    onSubmit,
    onSkip,
    onHistoryPrevious,
    onHistoryNext,
    onComplete,
    onTripleTap,
  } = props;
  const palette = usePalette();
  // No offset is subtracted: the area must always clear the keyboard, and a visible gap is a far
  // smaller problem than unreadable input.
  const keyboard = useKeyboardInset();
  const scrollRef = useRef<React.ComponentRef<typeof ScrollView>>(null);
  const inputRef = useRef<React.ComponentRef<typeof TextInput>>(null);
  const [clock, setClock] = useState(() => new Date());

  const runWithHaptic = () => {
    if (settings.haptics) {
      void Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light).catch(() => undefined);
    }
    onSubmit();
  };

  useEffect(() => {
    const id = setInterval(() => setClock(new Date()), 15_000);
    return () => clearInterval(id);
  }, []);

  useEffect(() => {
    if (lines.length === 0) return;
    scrollRef.current?.scrollToEnd({ animated: settings.animations });
  }, [lines.length, settings.animations, keyboard.inset]);

  useEffect(() => {
    if (!booting) return;
    const id = setInterval(() => setClock(new Date()), 15_000);
    return () => clearInterval(id);
  }, [booting]);

  return (
    <View
      style={[styles.container, { backgroundColor: palette.background, paddingBottom: keyboard.inset }]}
      onLayout={keyboard.onLayout}
    >
      <Pressable
        onPress={onTripleTap}
        accessibilityLabel="Terminal header, opens quick controls"
        style={styles.header}
      >
        <View style={styles.headerRow}>
          <Text style={[styles.prompt, { color: palette.primary }]} numberOfLines={1}>
            {promptFor(settings)}
          </Text>
          <Text style={[styles.time, { color: palette.accent }]}>
            {formatDatePattern(clock, settings.timeFormat)}
          </Text>
        </View>
        <View style={[styles.divider, { backgroundColor: palette.border }]} />
        {settings.showStatusPanel && (
          <Text style={[styles.statusLine, { color: palette.secondary }]} numberOfLines={2}>
            {statusLine(status)}
          </Text>
        )}
      </Pressable>

      <View style={styles.outputArea}>
        <Image
          source={require('../../assets/watermark.png')}
          style={[styles.watermark, { tintColor: palette.primary }]}
          resizeMode="contain"
          pointerEvents="none"
          accessibilityElementsHidden
          importantForAccessibility="no-hide-descendants"
        />
        <ScrollView
          ref={scrollRef}
          style={styles.output}
          contentContainerStyle={styles.outputContent}
          keyboardShouldPersistTaps="handled"
          onTouchStart={() => inputRef.current?.focus()}
        >
          {lines.map((line) => (
            <Text
              key={line.id}
              style={[
                styles.line,
                { color: colorFor(line.style, palette), fontSize: settings.fontSize },
                line.style === 'banner' && styles.banner,
              ]}
            >
              {line.text || ' '}
            </Text>
          ))}
          {executing && <Text style={[styles.line, { color: palette.accent }]}>…</Text>}
        </ScrollView>
      </View>

      {booting && (
        <Pressable onPress={onSkip} accessibilityRole="button" style={styles.skipRow}>
          <Text style={[styles.skip, { color: palette.accent }]}>[ SKIP ]</Text>
        </Pressable>
      )}

      <View style={styles.inputRow}>
        <Text style={[styles.inputPrompt, { color: palette.primary }]}>{promptFor(settings)}</Text>
        <TextInput
          ref={inputRef}
          value={input}
          onChangeText={onChangeInput}
          onSubmitEditing={runWithHaptic}
          onKeyPress={(event) => {
            const key = event.nativeEvent.key;
            if (key === 'ArrowUp') {
              onHistoryPrevious();
            } else if (key === 'ArrowDown') {
              onHistoryNext();
            } else if (key === 'Tab') {
              onComplete();
            }
          }}
          style={[styles.input, { color: palette.primary, fontSize: settings.fontSize }]}
          autoCapitalize="none"
          autoCorrect={false}
          spellCheck={false}
          multiline={false}
          blurOnSubmit={false}
          returnKeyType="go"
          placeholder=""
          placeholderTextColor={palette.secondary}
          selectionColor={palette.primary}
          accessibilityLabel="Command input"
        />
        <Pressable
          onPress={runWithHaptic}
          accessibilityRole="button"
          accessibilityLabel="Run command"
          hitSlop={8}
          style={styles.runButton}
        >
          <Text style={{ color: palette.primary, fontFamily: MONO_FONT, fontSize: 16 }}>↵</Text>
        </Pressable>
      </View>
    </View>
  );
}

function colorFor(style: LineStyle, palette: ReturnType<typeof usePalette>): string {
  switch (style) {
    case 'dim':
      return palette.secondary;
    case 'accent':
      return palette.accent;
    case 'success':
      return palette.primary;
    case 'warning':
      return palette.warning;
    case 'error':
      return palette.error;
    case 'banner':
      return palette.accent;
    case 'command':
      return palette.secondary;
    default:
      return palette.primary;
  }
}

function statusLine(status: StatusSnapshot | null): string {
  if (!status) return 'SYSTEM · LOADING DEVICE STATE';
  const parts = [
    (status.device.model || 'ANDROID').toUpperCase(),
    `BAT ${status.battery.percent ?? '--'}%${status.battery.charging ? ' ⚡' : ''}`,
    status.network.connected ? status.network.type : 'OFFLINE',
  ];
  return parts.join('  ·  ');
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: { paddingHorizontal: 16, paddingTop: 10, paddingBottom: 6 },
  headerRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 8 },
  prompt: { fontFamily: MONO_FONT, fontSize: 13, fontWeight: '700', flexShrink: 1 },
  time: { fontFamily: MONO_FONT, fontSize: 12 },
  divider: { height: StyleSheet.hairlineWidth, marginTop: 6 },
  statusLine: { fontFamily: MONO_FONT, fontSize: 11, letterSpacing: 0.5, marginTop: 6 },
  outputArea: { flex: 1 },
  watermark: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    opacity: 0.07,
  },
  output: { flex: 1 },
  outputContent: { paddingHorizontal: 16, paddingTop: 8, paddingBottom: 12 },
  line: { fontFamily: MONO_FONT, lineHeight: 19, letterSpacing: 0.2 },
  banner: { fontWeight: '700' },
  skipRow: { alignItems: 'flex-end', paddingHorizontal: 16 },
  skip: { fontFamily: MONO_FONT, fontSize: 11, paddingVertical: 6 },
  inputRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 8,
    gap: 6,
  },
  inputPrompt: { fontFamily: MONO_FONT, fontSize: 12, fontWeight: '700' },
  input: { flex: 1, fontFamily: MONO_FONT, paddingVertical: 6, paddingHorizontal: 0 },
  runButton: { paddingHorizontal: 6, paddingVertical: 4 },
});
