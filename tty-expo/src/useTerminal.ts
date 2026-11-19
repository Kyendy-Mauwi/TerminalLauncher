import { useCallback, useEffect, useRef, useState } from 'react';
import { commandNames } from './catalog';
import { runCommand, type Output } from './commands';
import { parseCommand } from './parser';
import { loadAliases, loadHistory, recordHistory } from './storage';
import { resolveApp, type CatalogApp } from './apps';
import { promptFor, type AppSettings, type TabKey, type TerminalLine } from './types';

interface Options {
  settings: AppSettings;
  updateSettings: (patch: Partial<AppSettings>) => void;
  ready: boolean;
  goTab: (tab: TabKey) => void;
}

const sleep = (millis: number) => new Promise<void>((resolve) => setTimeout(resolve, millis));

export function useTerminal({ settings, updateSettings, ready, goTab }: Options) {
  const [lines, setLines] = useState<TerminalLine[]>([]);
  const [input, setInput] = useState('');
  const [booting, setBooting] = useState(true);
  const [executing, setExecuting] = useState(false);
  const [recentApps, setRecentApps] = useState<CatalogApp[]>([]);

  const idRef = useRef(1);
  const bootStarted = useRef(false);
  const bootSkip = useRef(false);
  const readyPrinted = useRef(false);
  const historyRef = useRef<string[]>([]);
  const cursorRef = useRef(-1);
  const settingsRef = useRef(settings);
  settingsRef.current = settings;

  const print = useCallback((output: Output[] | Output | string) => {
    const list: Output[] = Array.isArray(output)
      ? output
      : typeof output === 'string'
        ? [{ text: output, style: 'normal' }]
        : [output];
    setLines((previous) => {
      const next = [...previous];
      for (const item of list) {
        next.push({ id: idRef.current++, text: item.text, style: item.style ?? 'normal' });
      }
      return next.slice(-600);
    });
  }, []);

  const printReady = useCallback(() => {
    if (readyPrinted.current) return;
    readyPrinted.current = true;
    const current = settingsRef.current;
    print([
      { text: 'SYSTEM READY.', style: 'success' },
      { text: `${current.startupMessage}, ${current.username.toUpperCase()}.`, style: 'accent' },
      { text: 'Your phone. Your rules.', style: 'dim' },
      { text: '', style: 'dim' },
      { text: "Type 'help' to see available commands.", style: 'dim' },
      { text: '', style: 'dim' },
    ]);
    setBooting(false);
  }, [print]);

  useEffect(() => {
    if (!ready || bootStarted.current) return;
    bootStarted.current = true;
    let cancelled = false;

    const boot = async () => {
      const current = settingsRef.current;
      setLines([]);
      print([{ text: 'INITIALIZING T3rm!n@l...', style: 'dim' }, { text: '', style: 'dim' }]);

      if (current.bootSequence) {
        const steps = [
          'LOADING SYSTEM MODULES',
          'INITIALIZING INTERFACE',
          'LOADING APP CATALOGUE',
          'CHECKING DEVICE STATE',
        ];
        for (const step of steps) {
          if (cancelled || bootSkip.current) break;
          if (current.animations) await sleep(140);
          if (cancelled) break;
          print({ text: `${step.padEnd(32, '.')} OK` });
        }
        print({ text: '', style: 'dim' });
      }

      if (!cancelled) printReady();
    };

    void boot();
    return () => {
      cancelled = true;
    };
  }, [ready, print, printReady]);

  const skipBoot = useCallback(() => {
    bootSkip.current = true;
    printReady();
  }, [printReady]);

  const submit = useCallback(async () => {
    const parsed = parseCommand(input);
    if (!parsed) {
      setInput('');
      return;
    }

    if (booting) skipBoot();
    setInput('');
    setExecuting(true);
    print({ text: `${promptFor(settingsRef.current)} ${parsed.raw}`, style: 'command' });

    try {
      const current = settingsRef.current;
      await recordHistory(parsed.raw, current.historyLimit);
      historyRef.current = await loadHistory();
      cursorRef.current = -1;

      await runCommand(parsed, {
        settings: current,
        update: updateSettings,
        print,
        clearScreen: () => setLines([]),
        goTab,
        recentApps,
        recordRecent: (app) =>
          setRecentApps((previous) => [app, ...previous.filter((entry) => entry.id !== app.id)].slice(0, 10)),
      });
    } catch (error) {
      print([
        { text: 'COMMAND FAILED.', style: 'error' },
        { text: error instanceof Error ? error.message : 'Unknown error.', style: 'dim' },
        { text: 'Nothing was changed.', style: 'dim' },
      ]);
    } finally {
      setExecuting(false);
    }
  }, [input, booting, skipBoot, print, updateSettings, goTab, recentApps]);

  const historyPrevious = useCallback(() => {
    const entries = historyRef.current;
    if (entries.length === 0) return;
    cursorRef.current = Math.min(cursorRef.current + 1, entries.length - 1);
    setInput(entries[entries.length - 1 - cursorRef.current]);
  }, []);

  const historyNext = useCallback(() => {
    const entries = historyRef.current;
    if (entries.length === 0) return;
    cursorRef.current -= 1;
    if (cursorRef.current < 0) {
      cursorRef.current = -1;
      setInput('');
    } else {
      setInput(entries[entries.length - 1 - cursorRef.current]);
    }
  }, []);

  const completeInput = useCallback(async () => {
    const text = input.trimStart();
    if (!text) return;
    const parts = text.split(/\s+/);

    if (parts.length === 1) {
      const matches = commandNames().filter((name) => name.startsWith(parts[0].toLowerCase()));
      if (matches.length === 1) {
        setInput(`${matches[0]} `);
      } else if (matches.length > 1) {
        print({ text: `  ${matches.join('  ')}`, style: 'dim' });
      }
      return;
    }

    const command = parts[0].toLowerCase();
    if (!['open', 'search', 'install', 'favorites', 'alias'].includes(command)) return;

    const prefix = parts.slice(1).join(' ');
    const aliases = await loadAliases();
    const matches = resolveApp(prefix, aliases);
    if (matches.length === 1) {
      setInput(`${command} ${matches[0].label}`);
    } else if (matches.length > 1) {
      print({ text: `  ${matches.map((app) => app.label).join('  ')}`, style: 'dim' });
    }
  }, [input, print]);

  const resetSessionHistory = useCallback(() => {
    cursorRef.current = -1;
  }, []);

  return {
    lines,
    input,
    booting,
    executing,
    submit,
    skipBoot,
    setInput,
    historyPrevious,
    historyNext,
    completeInput,
    resetSessionHistory,
  };
}
