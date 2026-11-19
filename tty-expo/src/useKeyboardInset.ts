import { useCallback, useEffect, useRef, useState } from 'react';
import { Keyboard, Platform } from 'react-native';
import type { LayoutChangeEvent } from 'react-native';

interface Options {
  /** Space between the tracked view's bottom edge and the screen bottom (safe area, tab bar). */
  bottomOffset?: number;
}

/**
 * Height the software keyboard overlaps the tracked view, for Android.
 *
 * Measured on-screen coordinates proved unreliable inside Expo Go, so this uses two numbers that
 * cannot lie: the keyboard's own height, and how much the tracked view's layout shrank. If the
 * host window resizes (`adjustResize`) the shrink already clears the keyboard and the inset is 0;
 * if the keyboard floats over an edge-to-edge window nothing shrinks and the inset is the full
 * keyboard height minus the space below the view.
 */
export function useKeyboardInset({ bottomOffset = 0 }: Options = {}) {
  const [inset, setInset] = useState(0);
  const keyboardHeight = useRef(0);
  const currentHeight = useRef(0);
  const restingHeight = useRef(0);
  const timers = useRef<Array<ReturnType<typeof setTimeout>>>([]);

  const clearTimers = () => {
    for (const timer of timers.current) clearTimeout(timer);
    timers.current = [];
  };

  const commit = useCallback(() => {
    if (!keyboardHeight.current) {
      setInset(0);
      return;
    }
    const shrunkBy = Math.max(0, restingHeight.current - currentHeight.current);
    const overlap = keyboardHeight.current - bottomOffset - shrunkBy;
    setInset(Math.max(0, Math.min(Math.round(overlap), Math.round(keyboardHeight.current))));
  }, [bottomOffset]);

  const onLayout = useCallback(
    (event: LayoutChangeEvent) => {
      currentHeight.current = event.nativeEvent.layout.height;
      if (!keyboardHeight.current) {
        restingHeight.current = currentHeight.current;
        return;
      }
      commit();
    },
    [commit]
  );

  useEffect(() => {
    if (Platform.OS === 'web') return;

    const show = Keyboard.addListener('keyboardDidShow', (event) => {
      keyboardHeight.current = event.endCoordinates.height;
      clearTimers();
      // Recompute after the window resize and after our own padding settles.
      for (const delay of [0, 60, 160, 320]) {
        timers.current.push(setTimeout(commit, delay));
      }
    });

    const hide = Keyboard.addListener('keyboardDidHide', () => {
      clearTimers();
      keyboardHeight.current = 0;
      setInset(0);
    });

    return () => {
      clearTimers();
      show.remove();
      hide.remove();
    };
  }, [commit]);

  return { inset, onLayout };
}
