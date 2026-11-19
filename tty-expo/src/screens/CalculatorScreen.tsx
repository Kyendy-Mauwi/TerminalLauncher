import React, { useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { evaluate, formatNumber } from '../evaluator';
import { MONO_FONT } from '../types';
import { usePalette } from '../ui';

const KEY_ROWS: string[][] = [
  ['C', '⌫', '(', ')'],
  ['7', '8', '9', '÷'],
  ['4', '5', '6', '×'],
  ['1', '2', '3', '−'],
  ['0', '.', '%', '+'],
];

export function CalculatorScreen() {
  const palette = usePalette();
  const [expression, setExpression] = useState('');
  const [result, setResult] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const press = (key: string) => {
    if (key === 'C') {
      setExpression('');
      setResult(null);
      setError(null);
      return;
    }
    if (key === '⌫') {
      setExpression((current) => current.slice(0, -1));
      return;
    }
    setError(null);
    setExpression((current) => (current.length >= 64 ? current : current + key));
  };

  const compute = () => {
    const normalized = expression.replace(/÷/g, '/').replace(/×/g, '*').replace(/−/g, '-');
    const outcome = evaluate(normalized);
    if (outcome.ok) {
      setResult(formatNumber(outcome.value));
      setError(null);
    } else {
      setError(outcome.reason);
      setResult(null);
    }
  };

  return (
    <View style={[styles.container, { backgroundColor: palette.background }]}>
      <Text style={[styles.title, { color: palette.accent }]}>CALCULATOR</Text>
      <View style={[styles.divider, { backgroundColor: palette.border }]} />

      <View style={styles.display}>
        <Text style={[styles.expression, { color: palette.primary }]} numberOfLines={2}>
          {expression || '0'}
        </Text>
        <Text style={[styles.result, { color: error ? palette.error : palette.accent }]} numberOfLines={2}>
          {error ?? result ?? ' '}
        </Text>
      </View>

      <View style={styles.pad}>
        {KEY_ROWS.map((row) => (
          <View key={row.join('')} style={styles.row}>
            {row.map((key) => (
              <CalcKey key={key} label={key} onPress={() => press(key)} />
            ))}
          </View>
        ))}
        <Pressable
          onPress={compute}
          accessibilityRole="button"
          accessibilityLabel="Equals"
          style={[styles.equals, { borderColor: palette.primary, backgroundColor: `${palette.primary}1F` }]}
        >
          <Text style={{ color: palette.primary, fontFamily: MONO_FONT, fontSize: 18, fontWeight: '700' }}>=</Text>
        </Pressable>
        <Text style={[styles.hint, { color: palette.secondary }]}>
          Same safe evaluator as the terminal. No eval(), no code execution.
        </Text>
      </View>
    </View>
  );
}

function CalcKey({ label, onPress }: { label: string; onPress: () => void }) {
  const palette = usePalette();
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={`Calculator key ${label}`}
      style={({ pressed }) => [
        styles.key,
        {
          borderColor: palette.border,
          backgroundColor: pressed ? `${palette.primary}1A` : `${palette.surface}88`,
        },
      ]}
    >
      <Text style={{ color: palette.secondary, fontFamily: MONO_FONT, fontSize: 18 }}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, paddingHorizontal: 16 },
  title: { fontFamily: MONO_FONT, fontSize: 14, fontWeight: '700', letterSpacing: 2, marginTop: 12 },
  divider: { height: StyleSheet.hairlineWidth, marginTop: 8 },
  display: { flex: 1, justifyContent: 'flex-end', alignItems: 'flex-end', paddingBottom: 16 },
  expression: { fontFamily: MONO_FONT, fontSize: 26, textAlign: 'right' },
  result: { fontFamily: MONO_FONT, fontSize: 18, textAlign: 'right', marginTop: 6 },
  pad: { paddingBottom: 10 },
  row: { flexDirection: 'row', gap: 6, marginBottom: 6 },
  key: {
    flex: 1,
    height: 52,
    borderWidth: StyleSheet.hairlineWidth,
    borderRadius: 3,
    alignItems: 'center',
    justifyContent: 'center',
  },
  equals: {
    height: 52,
    borderWidth: StyleSheet.hairlineWidth,
    borderRadius: 3,
    alignItems: 'center',
    justifyContent: 'center',
  },
  hint: { fontFamily: MONO_FONT, fontSize: 9, marginTop: 10 },
});
