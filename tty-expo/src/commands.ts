import { Platform } from 'react-native';
import { parseCommand, type ParsedCommand } from './parser';
import { commandNames, categoryGroups, findCommand, type CommandSpec } from './catalog';
import { evaluate, formatNumber } from './evaluator';
import { suggest } from './suggestions';
import { formatBytes, formatDatePattern, formatShortDuration, formatUptime, padRight } from './format';
import {
  APP_CATALOG,
  appSuggestions,
  launchCatalogApp,
  probeApp,
  resolveApp,
  type CatalogApp,
} from './apps';
import {
  addNote,
  addFavorite,
  clearHistory,
  clearNotes,
  loadAliases,
  loadFavorites,
  loadHistory,
  loadNotes,
  removeAlias,
  removeFavorite,
  removeNote,
  setAlias,
} from './storage';
import {
  getBatteryInfo,
  getBrightnessPercent,
  getDeviceInfo,
  getNetworkInfo,
  getStorageInfo,
  getUptimeMillis,
  openSettingsPanel,
  openUrl,
  setBrightnessPercent,
  SETTINGS_PANELS,
  startDocumentPicker,
  startIntent,
} from './device';
import type { AppSettings, LineStyle, TabKey } from './types';

export interface Output {
  text: string;
  style?: LineStyle;
}

export interface CommandContext {
  settings: AppSettings;
  update: (patch: Partial<AppSettings>) => void;
  print: (output: Output[] | Output | string) => void;
  clearScreen: () => void;
  goTab: (tab: TabKey) => void;
  recentApps: CatalogApp[];
  recordRecent: (app: CatalogApp) => void;
}

export async function runCommand(parsed: ParsedCommand, ctx: CommandContext): Promise<void> {
  const spec = findCommand(parsed.name);
  const name = spec?.name ?? parsed.name;
  const say = (text: string, style: LineStyle = 'normal') => ctx.print({ text, style });

  for (const warning of parsed.warnings) say(warning, 'warning');

  try {
    switch (name) {
      case 'help':
        await help(parsed, ctx);
        return;
      case 'clear':
        ctx.clearScreen();
        return;
      case 'home':
        ctx.goTab('terminal');
        return;
      case 'back':
        ctx.goTab('terminal');
        return;
      case 'exit':
        say('EXPO GO CANNOT EXIT TO A HOME SCREEN.', 'warning');
        say('Use the Android home button or gesture to leave the app.', 'dim');
        return;
      case 'about':
        about(ctx);
        return;
      case 'settings':
        await settings(parsed, ctx);
        return;

      case 'apps':
        await apps(parsed, ctx);
        return;
      case 'open':
        await open(parsed, ctx);
        return;
      case 'search':
        await search(parsed, ctx);
        return;
      case 'recent':
        recent(ctx);
        return;
      case 'favorites':
        await favorites(parsed, ctx);
        return;
      case 'install':
        await install(parsed, ctx);
        return;
      case 'alias':
        await alias(parsed, ctx);
        return;

      case 'sysinfo':
        await sysinfo(ctx);
        return;
      case 'battery':
        await battery(ctx);
        return;
      case 'storage':
        await storage(ctx);
        return;
      case 'memory':
        memory(ctx);
        return;
      case 'device':
        device(ctx);
        return;
      case 'uptime':
        await uptime(ctx);
        return;
      case 'network':
        await network(ctx);
        return;
      case 'wifi':
        await wifi(parsed, ctx);
        return;
      case 'bluetooth':
        await bluetooth(parsed, ctx);
        return;
      case 'brightness':
        await brightness(parsed, ctx);
        return;
      case 'volume':
        await volume(ctx);
        return;
      case 'notifications':
        notifications(ctx);
        return;
      case 'date':
        say(formatDatePattern(new Date(), ctx.settings.dateFormat));
        return;
      case 'time':
        say(formatDatePattern(new Date(), ctx.settings.timeFormat));
        return;

      case 'notes':
        await notes(parsed, ctx);
        return;
      case 'calculator':
        calculator(parsed, ctx);
        return;
      case 'calendar':
        await everydayCmd(
          'CALENDAR',
          async () => {
            const opened = await openUrl('content://com.android.calendar/time');
            return (
              opened ||
              (await startIntent('android.intent.action.MAIN', { packageName: 'com.google.android.calendar' }))
            );
          },
          ctx
        );
        return;
      case 'clock':
        await everydayCmd('CLOCK', () => startIntent('android.intent.action.SHOW_ALARMS'), ctx);
        return;
      case 'camera':
        await everydayCmd('CAMERA', () => startIntent('android.media.action.STILL_IMAGE_CAMERA'), ctx);
        return;
      case 'files':
        await everydayCmd('DOCUMENT PICKER', () => startDocumentPicker(), ctx);
        return;
      case 'contacts':
        await everydayCmd(
          'CONTACTS',
          async () => {
            const opened = await openUrl('content://contacts/people/');
            return (
              opened || (await startIntent('android.intent.action.VIEW', { packageName: 'com.android.contacts' }))
            );
          },
          ctx
        );
        return;
      case 'messages':
        await everydayCmd(
          'MESSAGES',
          async () => {
            const opened = await openUrl('sms:');
            return opened || (await startIntent('android.intent.action.MAIN'));
          },
          ctx
        );
        return;
      case 'phone':
        await everydayCmd('DIALER', () => openUrl('tel:'), ctx);
        return;
      case 'wallpaper':
        await everydayCmd('WALLPAPER PICKER', () => startIntent('android.intent.action.SET_WALLPAPER'), ctx);
        return;

      case 'theme':
        theme(parsed, ctx);
        return;
      case 'font':
        say('THIS BUILD USES THE SYSTEM MONOSPACE FACE.', 'accent');
        say('A bundled JetBrains Mono needs expo-font + the native app for pixel parity.', 'dim');
        return;
      case 'fontsize':
        fontSize(parsed, ctx);
        return;
      case 'opacity':
        opacity(parsed, ctx);
        return;
      case 'cursor':
        cursor(parsed, ctx);
        return;
      case 'animations':
        animations(parsed, ctx);
        return;

      case 'whoami':
        say(ctx.settings.username);
        say(`host=${ctx.settings.hostname}  platform=${Platform.OS}`, 'dim');
        return;
      case 'hostname':
        hostname(parsed, ctx);
        return;
      case 'history':
        await history(parsed, ctx);
        return;
      case 'echo':
        say(parsed.rawArgs);
        return;
      case 'version':
        version(ctx);
        return;
      case 'ping':
        await ping(parsed, ctx);
        return;
      case 'reboot':
        say('REBOOT UNAVAILABLE.', 'warning');
        say('Rebooting needs privileged system access; Expo Go has none.', 'dim');
        say('Hold the power button and choose Restart.', 'dim');
        return;
      default:
        unknown(parsed, ctx);
    }
  } catch (error) {
    say(`UNEXPECTED ERROR: ${error instanceof Error ? error.message : 'unknown'}`, 'error');
    say('Nothing was changed.', 'dim');
  }
}

function usage(specName: string, message: string, ctx: CommandContext): void {
  const spec = findCommand(specName);
  ctx.print([
    { text: `ERROR: ${message}`, style: 'error' },
    { text: '', style: 'dim' },
    { text: `Usage: ${spec?.usage ?? specName}`, style: 'normal' },
  ]);
}

async function help(parsed: ParsedCommand, ctx: CommandContext): Promise<void> {
  const query = parsed.args[0];
  if (query) {
    const spec: CommandSpec | undefined = findCommand(query);
    if (!spec) {
      ctx.print({ text: `NO HELP AVAILABLE FOR: ${query.toLowerCase()}`, style: 'error' });
      suggest(query, commandNames()).forEach((candidate) => {
        ctx.print({ text: `  Did you mean: ${candidate}`, style: 'accent' });
      });
      return;
    }
    ctx.print([
      { text: `MANUAL :: ${spec.name.toUpperCase()}`, style: 'banner' },
      { text: '', style: 'dim' },
      { text: `USAGE       : ${spec.usage}` },
      { text: `DESCRIPTION : ${spec.description}` },
      { text: `CATEGORY    : ${spec.category}` },
      ...(spec.aliases?.length ? [{ text: `ALIASES     : ${spec.aliases.join(', ')}` }] : []),
    ]);
    return;
  }

  const lines: Output[] = [{ text: 'AVAILABLE COMMANDS', style: 'banner' }, { text: '', style: 'dim' }];
  for (const group of categoryGroups()) {
    lines.push({ text: group.category, style: 'accent' });
    for (const spec of group.specs) {
      lines.push({ text: `  ${padRight(spec.name, 13)}${spec.description}` });
    }
    lines.push({ text: '', style: 'dim' });
  }
  lines.push({ text: "Type 'help <command>' for usage details.", style: 'dim' });
  ctx.print(lines);
}

function about(ctx: CommandContext): void {
  const app = getDeviceInfo();
  ctx.print([
    { text: 'T3rm!n@l', style: 'banner' },
    { text: 'Your phone. Your rules.', style: 'dim' },
    { text: '', style: 'dim' },
    { text: `VERSION     : ${app.appVersion} (${app.appBuild})` },
    { text: `RUNTIME     : EXPO / REACT NATIVE (${Platform.OS.toUpperCase()})` },
    { text: `HOST        : ${app.packageName}` },
    { text: '', style: 'dim' },
    { text: 'WHAT WORKS HERE', style: 'accent' },
    { text: '  Terminal engine, themes, settings, notes, history, aliases.' },
    { text: '  Battery, network, storage, device, uptime, brightness.' },
    { text: '  App launching and system panels.' },
  ]);
}

async function settings(parsed: ParsedCommand, ctx: CommandContext): Promise<void> {
  const panel = parsed.args[0]?.toLowerCase();
  if (!panel || panel === 'terminal' || panel === 'tty') {
    ctx.goTab('settings');
    ctx.print({ text: 'OPENING T3rm!n@l SETTINGS...', style: 'accent' });
    return;
  }

  if (!SETTINGS_PANELS.includes(panel)) {
    ctx.print([
      { text: `UNKNOWN SETTINGS PANEL: ${panel}`, style: 'error' },
      { text: `Panels: ${SETTINGS_PANELS.join(', ')}, terminal`, style: 'dim' },
    ]);
    return;
  }

  const opened = await openSettingsPanel(panel);
  ctx.print(
    opened
      ? { text: `OPENING ${panel.toUpperCase()} SETTINGS...`, style: 'accent' }
      : { text: 'ANDROID SETTINGS UNAVAILABLE ON THIS RUNTIME.', style: 'error' }
  );
}

async function apps(parsed: ParsedCommand, ctx: CommandContext): Promise<void> {
  const filter = parsed.args.join(' ').trim().toLowerCase();
  const aliases = await loadAliases();
  const source = filter
    ? APP_CATALOG.filter(
        (app) =>
          app.label.toLowerCase().includes(filter) ||
          app.packageName.includes(filter) ||
          (app.aliases ?? []).some((alias) => alias.includes(filter))
      )
    : APP_CATALOG;

  if (source.length === 0) {
    ctx.print({ text: `NO CATALOGUE APPS MATCH "${filter}".`, style: 'warning' });
    return;
  }

  const availability = await Promise.all(source.map(async (app) => ({ app, available: await probeApp(app) })));
  const aliasFor = (app: CatalogApp) =>
    Object.entries(aliases).find(([key]) => key === app.id)?.[1] ?? '';

  ctx.print([
    { text: `OPENABLE APPS (${source.length})`, style: 'banner' },
    { text: '', style: 'dim' },
    ...availability.map(({ app, available }) => ({
      text: `  ${available ? '[+]' : '[?]'} ${padRight(app.label, 16)}${aliasFor(app) || app.packageName}`,
    })),
    { text: '', style: 'dim' },
    { text: '[+] deep link reachable   [?] falls back to the store', style: 'dim' },
    { text: 'Expo Go cannot enumerate all installed packages.', style: 'dim' },
  ]);
}

async function open(parsed: ParsedCommand, ctx: CommandContext): Promise<void> {
  if (parsed.args.length === 0) {
    usage('open', 'Missing application name.', ctx);
    return;
  }

  const query = parsed.args.join(' ');
  const aliases = await loadAliases();
  const matches = resolveApp(query, aliases);

  if (matches.length === 0) {
    ctx.print([
      { text: 'APPLICATION NOT FOUND.', style: 'error' },
      { text: '', style: 'dim' },
      { text: 'This build opens a curated catalogue, not every installed app.', style: 'dim' },
      { text: "See 'apps' for the list, or 'install' to open the store.", style: 'dim' },
      ...appSuggestions(query).map((label) => ({ text: `  Did you mean: ${label}`, style: 'accent' as const })),
    ]);
    return;
  }

  if (matches.length > 1) {
    ctx.print([
      { text: `MULTIPLE MATCHES FOR "${query}".`, style: 'warning' },
      { text: '', style: 'dim' },
      ...matches.map((app) => ({ text: `  open ${app.label}`, style: 'accent' as const })),
    ]);
    return;
  }

  const app = matches[0];
  ctx.print({ text: `LAUNCHING ${app.label.toUpperCase()}...`, style: 'accent' });
  const result = await launchCatalogApp(app);
  if (result.ok) {
    ctx.recordRecent(app);
    ctx.print({ text: 'APPLICATION STARTED.', style: 'success' });
    ctx.print({ text: result.message, style: 'dim' });
  } else {
    ctx.print({ text: 'LAUNCH FAILED.', style: 'error' });
    ctx.print({ text: result.message, style: 'dim' });
  }
}

async function search(parsed: ParsedCommand, ctx: CommandContext): Promise<void> {
  if (parsed.args.length === 0) {
    usage('search', 'Missing search query.', ctx);
    return;
  }
  const query = parsed.args.join(' ');
  const aliases = await loadAliases();
  const matches = resolveApp(query, aliases);
  if (matches.length === 0) {
    ctx.print({ text: `NO RESULTS FOR "${query}".`, style: 'warning' });
    return;
  }
  ctx.print([
    { text: `SEARCH RESULTS (${matches.length})`, style: 'banner' },
    { text: '', style: 'dim' },
    ...matches.map((app) => ({ text: `  ${padRight(app.label, 18)}${app.packageName}` })),
    { text: '', style: 'dim' },
    { text: 'Launch with: open <name>', style: 'dim' },
  ]);
}

function recent(ctx: CommandContext): void {
  if (ctx.recentApps.length === 0) {
    ctx.print([
      { text: 'NO RECENT APPLICATIONS THIS SESSION.', style: 'warning' },
      { text: 'Apps opened through the terminal appear here.', style: 'dim' },
    ]);
    return;
  }
  ctx.print([
    { text: 'RECENT APPLICATIONS', style: 'banner' },
    { text: '', style: 'dim' },
    ...ctx.recentApps.map((app, index) => ({ text: `  ${index + 1}  ${app.label}` })),
  ]);
}

async function favorites(parsed: ParsedCommand, ctx: CommandContext): Promise<void> {
  const action = parsed.args[0]?.toLowerCase();
  const favoritesList = await loadFavorites();

  if (!action) {
    if (favoritesList.length === 0) {
      ctx.print([
        { text: 'NO FAVOURITES YET.', style: 'warning' },
        { text: 'Add one with: favorites add <application>', style: 'dim' },
      ]);
      return;
    }
    ctx.print([
      { text: 'FAVOURITES', style: 'banner' },
      { text: '', style: 'dim' },
      ...favoritesList.map((favorite) => ({ text: `  ${favorite.label}` })),
      { text: '', style: 'dim' },
      { text: 'favorites add|remove <application>', style: 'dim' },
    ]);
    return;
  }

  if (action !== 'add' && action !== 'remove' && action !== 'rm') {
    usage('favorites', `Unknown action '${action}'. Use add or remove.`, ctx);
    return;
  }

  const query = parsed.args.slice(1).join(' ');
  if (!query) {
    usage(`favorites ${action}`, 'Missing application name.', ctx);
    return;
  }

  const match = resolveApp(query, await loadAliases())[0];
  if (!match) {
    ctx.print({ text: 'APPLICATION NOT FOUND.', style: 'error' });
    return;
  }

  if (action === 'add') {
    await addFavorite({ id: match.id, label: match.label });
    ctx.print({ text: `ADDED TO FAVOURITES: ${match.label}`, style: 'success' });
  } else {
    await removeFavorite(match.id);
    ctx.print({ text: `REMOVED FROM FAVOURITES: ${match.label}`, style: 'success' });
  }
}

async function install(parsed: ParsedCommand, ctx: CommandContext): Promise<void> {
  if (parsed.args.length === 0) {
    usage('install', 'Missing application name.', ctx);
    return;
  }
  const query = parsed.args.join(' ');
  const url = `https://play.google.com/store/search?q=${encodeURIComponent(query)}&c=apps`;
  const opened = await openUrl(url);
  ctx.print(
    opened
      ? { text: `OPENING THE PLAY STORE FOR "${query}"...`, style: 'accent' }
      : { text: 'NO APP STORE AVAILABLE.', style: 'error' }
  );
  ctx.print({ text: 'Android never allows silent installation.', style: 'dim' });
}

async function alias(parsed: ParsedCommand, ctx: CommandContext): Promise<void> {
  const action = parsed.args[0]?.toLowerCase();
  const aliases = await loadAliases();

  if (!action) {
    const entries = Object.entries(aliases);
    if (entries.length === 0) {
      ctx.print([
        { text: 'NO ALIASES DEFINED.', style: 'warning' },
        { text: 'Create one with: alias set <alias> <application>', style: 'dim' },
      ]);
      return;
    }
    ctx.print([
      { text: 'APPLICATION ALIASES', style: 'banner' },
      { text: '', style: 'dim' },
      ...entries.map(([id, value]) => ({
        text: `  ${padRight(value, 16)}${APP_CATALOG.find((app) => app.id === id)?.label ?? id}`,
      })),
    ]);
    return;
  }

  if (action === 'set') {
    if (parsed.args.length < 3) {
      usage('alias set', 'Missing alias or application.', ctx);
      return;
    }
    const value = parsed.args[1].toLowerCase();
    if (!/^[a-z0-9_-]{1,16}$/.test(value)) {
      ctx.print({ text: `INVALID ALIAS: ${value}`, style: 'error' });
      ctx.print({ text: 'Use 1-16 characters: letters, digits, - or _.', style: 'dim' });
      return;
    }
    const app = resolveApp(parsed.args.slice(2).join(' '), aliases)[0];
    if (!app) {
      ctx.print({ text: 'APPLICATION NOT FOUND.', style: 'error' });
      return;
    }
    await setAlias(app.id, value);
    ctx.print([
      { text: `ALIAS CREATED: ${value} -> ${app.label}`, style: 'success' },
      { text: `Try: open ${value}`, style: 'dim' },
    ]);
    return;
  }

  if (action === 'remove' || action === 'rm') {
    const target = parsed.args.slice(1).join(' ').toLowerCase();
    if (!target) {
      usage('alias remove', 'Missing alias or application.', ctx);
      return;
    }
    const entry = Object.entries(aliases).find(([id, value]) => value === target || id === target);
    if (!entry) {
      ctx.print({ text: `NO ALIAS MATCHES: ${target}`, style: 'error' });
      return;
    }
    await removeAlias(entry[0]);
    ctx.print({ text: 'ALIAS REMOVED.', style: 'success' });
    return;
  }

  usage('alias', `Unknown action '${action}'. Use set or remove.`, ctx);
}

async function sysinfo(ctx: CommandContext): Promise<void> {
  const [batteryInfo, networkInfo, storageInfo, uptimeMillis] = await Promise.all([
    getBatteryInfo(),
    getNetworkInfo(),
    getStorageInfo(),
    getUptimeMillis(),
  ]);
  const deviceInfo = getDeviceInfo();

  ctx.print([
    { text: 'SYSTEM MONITOR', style: 'banner' },
    { text: '', style: 'dim' },
    { text: `DEVICE   : ${deviceInfo.manufacturer} ${deviceInfo.model}` },
    { text: `RUNTIME  : ${deviceInfo.osName} ${deviceInfo.osVersion}` },
    { text: `BATTERY  : ${batteryInfo.percent ?? '--'}%  ${batteryInfo.state}` },
    { text: `NETWORK  : ${networkInfo.connected ? networkInfo.type : 'OFFLINE'}` },
    { text: `STORAGE  : ${storageInfo.free == null ? 'UNAVAILABLE' : `${formatBytes(storageInfo.free)} FREE`}` },
    { text: `UPTIME   : ${uptimeMillis == null ? 'UNAVAILABLE' : formatUptime(uptimeMillis)}` },
    { text: `TIME     : ${formatDatePattern(new Date(), ctx.settings.timeFormat)}` },
    { text: '', style: 'dim' },
    { text: '[ SYSTEM READY ]', style: 'success' },
  ]);
}

async function battery(ctx: CommandContext): Promise<void> {
  const info = await getBatteryInfo();
  ctx.print([
    { text: 'BATTERY', style: 'banner' },
    { text: '', style: 'dim' },
    { text: `LEVEL  : ${info.percent ?? 'UNKNOWN'}%` },
    { text: `STATUS : ${info.state}` },
    { text: `POWER  : ${info.charging ? 'CHARGING' : 'BATTERY'}` },
    { text: '', style: 'dim' },
    { text: 'Read via expo-battery.', style: 'dim' },
  ]);
}

async function storage(ctx: CommandContext): Promise<void> {
  const info = await getStorageInfo();
  if (info.free == null && info.total == null) {
    ctx.print([
      { text: 'STORAGE INFORMATION UNAVAILABLE IN EXPO GO.', style: 'warning' },
      { text: 'Disk statistics need the native build.', style: 'dim' },
    ]);
    return;
  }
  const used = (info.total ?? 0) - (info.free ?? 0);
  ctx.print([
    { text: 'STORAGE', style: 'banner' },
    { text: '', style: 'dim' },
    { text: `FREE  : ${formatBytes(info.free ?? 0)}` },
    { text: `TOTAL : ${formatBytes(info.total ?? 0)}` },
    { text: `USED  : ${formatBytes(used)}` },
  ]);
}

function memory(ctx: CommandContext): void {
  const deviceInfo = getDeviceInfo();
  ctx.print([
    { text: 'MEMORY', style: 'banner' },
    { text: '', style: 'dim' },
    {
      text: `TOTAL : ${deviceInfo.totalMemory ? formatBytes(deviceInfo.totalMemory) : 'UNAVAILABLE'}`,
    },
    { text: 'FREE  : NOT EXPOSED BY EXPO GO', style: 'dim' },
    { text: 'Use the native build for ActivityManager memory details.', style: 'dim' },
  ]);
}

function device(ctx: CommandContext): void {
  const info = getDeviceInfo();
  ctx.print([
    { text: 'DEVICE', style: 'banner' },
    { text: '', style: 'dim' },
    { text: `MAKER    : ${info.manufacturer}` },
    { text: `MODEL    : ${info.model}` },
    { text: `OS       : ${info.osName} ${info.osVersion}` },
    { text: `PHYSICAL : ${info.isDevice ? 'YES' : 'EMULATOR'}` },
    { text: `APP      : ${info.appVersion} (${info.appBuild})` },
    { text: `HOSTNAME : ${ctx.settings.hostname}` },
    { text: `USER     : ${ctx.settings.username}` },
  ]);
}

async function uptime(ctx: CommandContext): Promise<void> {
  const millis = await getUptimeMillis();
  ctx.print({ text: 'UPTIME', style: 'banner' });
  ctx.print({ text: '', style: 'dim' });
  ctx.print({ text: millis == null ? 'UNAVAILABLE' : `UP ${formatUptime(millis)}` });
}

async function network(ctx: CommandContext): Promise<void> {
  const info = await getNetworkInfo();
  ctx.print([
    { text: 'NETWORK', style: 'banner' },
    { text: '', style: 'dim' },
    { text: `STATUS    : ${info.connected ? 'CONNECTED' : 'OFFLINE'}` },
    { text: `TRANSPORT : ${info.type}` },
    { text: `REACHABLE : ${info.reachable == null ? 'UNKNOWN' : info.reachable ? 'YES' : 'NO'}` },
    { text: `IP        : ${info.ip ?? 'UNKNOWN'}` },
    { text: '', style: 'dim' },
    { text: 'Use wifi or settings wifi for Wi-Fi controls.', style: 'dim' },
  ]);
}

async function wifi(parsed: ParsedCommand, ctx: CommandContext): Promise<void> {
  if (parsed.args[0]?.toLowerCase() === 'settings') {
    const opened = await openSettingsPanel('wifi');
    ctx.print({ text: opened ? 'OPENING WI-FI SETTINGS...' : 'WI-FI SETTINGS UNAVAILABLE.', style: opened ? 'accent' : 'error' });
    return;
  }
  const info = await getNetworkInfo();
  ctx.print([
    { text: 'WI-FI', style: 'banner' },
    { text: '', style: 'dim' },
    { text: `CONNECTED : ${info.type === 'WIFI' && info.connected ? 'YES' : 'NO'}` },
    { text: `TRANSPORT : ${info.type}` },
    { text: '', style: 'dim' },
    { text: 'Apps cannot toggle Wi-Fi. Use: wifi settings', style: 'dim' },
  ]);
}

async function bluetooth(parsed: ParsedCommand, ctx: CommandContext): Promise<void> {
  const opened = await openSettingsPanel('bluetooth');
  ctx.print({ text: opened ? 'OPENING BLUETOOTH SETTINGS...' : 'BLUETOOTH SETTINGS UNAVAILABLE.', style: opened ? 'accent' : 'error' });
  ctx.print({ text: 'Expo Go cannot read or toggle Bluetooth state.', style: 'dim' });
}

async function brightness(parsed: ParsedCommand, ctx: CommandContext): Promise<void> {
  const requested = parsed.args[0] ? Number(parsed.args[0].replace('%', '')) : NaN;
  if (Number.isNaN(requested)) {
    const current = await getBrightnessPercent();
    ctx.print([
      { text: 'BRIGHTNESS', style: 'banner' },
      { text: '', style: 'dim' },
      { text: `LEVEL : ${current == null ? 'UNAVAILABLE' : `${current}%`}` },
      { text: 'Set with: brightness <0-100>', style: 'dim' },
      { text: 'Applies to this app window, not system settings.', style: 'dim' },
    ]);
    return;
  }
  if (requested < 0 || requested > 100) {
    usage('brightness', 'Brightness must be between 0 and 100.', ctx);
    return;
  }
  const applied = await setBrightnessPercent(requested);
  ctx.print(
    applied
      ? { text: `BRIGHTNESS SET TO ${Math.round(requested)}%.`, style: 'success' }
      : { text: 'BRIGHTNESS CHANGE REJECTED.', style: 'error' }
  );
}

async function volume(ctx: CommandContext): Promise<void> {
  const opened = await openSettingsPanel('sound');
  ctx.print({ text: opened ? 'OPENING SOUND SETTINGS...' : 'SOUND SETTINGS UNAVAILABLE.' });
  ctx.print({ text: 'Changing volume directly needs a native audio module.', style: 'dim' });
}

function notifications(ctx: CommandContext): void {
  ctx.print([
    { text: 'NOTIFICATION ACCESS IS NOT AVAILABLE HERE.', style: 'warning' },
    { text: '', style: 'dim' },
    { text: 'Reading other apps notifications requires a native' },
    { text: 'NotificationListenerService with explicit system permission.' },
    { text: 'Expo Go cannot register that service.' },
    { text: '', style: 'dim' },
    { text: 'The Kotlin build implements it; nothing is ever stored or transmitted.', style: 'dim' },
  ]);
}

async function notes(parsed: ParsedCommand, ctx: CommandContext): Promise<void> {
  const action = parsed.args[0]?.toLowerCase();
  const notesList = await loadNotes();

  if (!action) {
    if (notesList.length === 0) {
      ctx.print([
        { text: 'NO NOTES SAVED.', style: 'warning' },
        { text: 'Create one with: notes add <text>', style: 'dim' },
      ]);
      return;
    }
    ctx.print([
      { text: `NOTES (${notesList.length})`, style: 'banner' },
      { text: '', style: 'dim' },
      ...notesList.flatMap((note) => [
        { text: `  #${note.id}  ${formatDatePattern(new Date(note.createdAt), 'yyyy-MM-dd HH:mm')}`, style: 'accent' as LineStyle },
        { text: `    ${note.content.slice(0, 160)}` },
      ]),
      { text: '', style: 'dim' },
      { text: 'notes add <text> | notes remove <id> | notes clear', style: 'dim' },
    ]);
    return;
  }

  if (action === 'add') {
    const content = parsed.args.slice(1).join(' ').trim();
    if (!content) {
      usage('notes add', 'Missing note text.', ctx);
      return;
    }
    const note = await addNote(content);
    ctx.print({ text: `NOTE #${note.id} SAVED LOCALLY.`, style: 'success' });
    return;
  }

  if (action === 'remove' || action === 'rm') {
    const id = parsed.args[1];
    if (!id) {
      usage('notes remove', 'Missing note id.', ctx);
      return;
    }
    await removeNote(id);
    ctx.print({ text: `NOTE #${id} REMOVED.`, style: 'success' });
    return;
  }

  if (action === 'clear') {
    await clearNotes();
    ctx.print({ text: 'ALL NOTES REMOVED.', style: 'success' });
    return;
  }

  const note = await addNote(parsed.args.join(' '));
  ctx.print({ text: `NOTE #${note.id} SAVED LOCALLY.`, style: 'success' });
}

function calculator(parsed: ParsedCommand, ctx: CommandContext): void {
  if (!parsed.rawArgs) {
    ctx.goTab('calculator');
    ctx.print({ text: 'OPENING CALCULATOR...', style: 'accent' });
    ctx.print({ text: 'Or evaluate inline: calculator 12 * (3 + 4)', style: 'dim' });
    return;
  }
  const result = evaluate(parsed.rawArgs);
  if (result.ok) {
    ctx.print({ text: `  ${parsed.rawArgs.trim()}` });
    ctx.print({ text: `= ${formatNumber(result.value)}`, style: 'success' });
  } else {
    ctx.print({ text: 'CALCULATION ERROR.', style: 'error' });
    ctx.print({ text: result.reason, style: 'dim' });
  }
}

async function everydayCmd(label: string, action: () => Promise<boolean>, ctx: CommandContext): Promise<void> {
  const opened = await action();
  ctx.print({ text: opened ? `OPENING ${label}...` : `${label} UNAVAILABLE.`, style: opened ? 'accent' : 'error' });
  if (!opened) ctx.print({ text: 'No handler responded on this device.', style: 'dim' });
}

function theme(parsed: ParsedCommand, ctx: CommandContext): void {
  const requested = parsed.args[0]?.toLowerCase();
  if (!requested) {
    ctx.print([
      { text: 'THEMES', style: 'banner' },
      { text: '', style: 'dim' },
      ...(['green', 'amber', 'cyan', 'matrix', 'mono'] as const).map((themeId) => ({
        text: `  ${themeId === ctx.settings.theme ? '*' : ' '} ${themeId}`,
      })),
      { text: '', style: 'dim' },
      { text: 'Usage: theme <green|amber|cyan|matrix|mono>', style: 'dim' },
    ]);
    return;
  }
  if (!['green', 'amber', 'cyan', 'matrix', 'mono'].includes(requested)) {
    ctx.print({ text: `UNKNOWN THEME: ${requested}`, style: 'error' });
    return;
  }
  ctx.update({ theme: requested as AppSettings['theme'] });
  ctx.print({ text: `THEME SET TO ${requested.toUpperCase()}.`, style: 'success' });
}

function fontSize(parsed: ParsedCommand, ctx: CommandContext): void {
  const value = Number(parsed.args[0]);
  if (Number.isNaN(value)) {
    usage('fontsize', `Current size is ${ctx.settings.fontSize}sp. Provide 9-22.`, ctx);
    return;
  }
  if (value < 9 || value > 22) {
    usage('fontsize', 'Size must be between 9 and 22.', ctx);
    return;
  }
  ctx.update({ fontSize: Math.round(value) });
  ctx.print({ text: `FONT SIZE SET TO ${Math.round(value)}SP.`, style: 'success' });
}

function opacity(parsed: ParsedCommand, ctx: CommandContext): void {
  const value = Number((parsed.args[0] ?? '').replace('%', ''));
  if (Number.isNaN(value)) {
    usage('opacity', `Current opacity is ${Math.round(ctx.settings.opacity * 100)}%.`, ctx);
    return;
  }
  if (value < 40 || value > 100) {
    usage('opacity', 'Opacity must be between 40 and 100.', ctx);
    return;
  }
  ctx.update({ opacity: value / 100 });
  ctx.print({ text: `BACKGROUND OPACITY SET TO ${value}%.`, style: 'success' });
}

function cursor(parsed: ParsedCommand, ctx: CommandContext): void {
  const style = parsed.args[0]?.toLowerCase();
  if (!style) {
    ctx.print({ text: `CURSOR: ${ctx.settings.cursor}`, style: 'banner' });
    ctx.print({ text: 'Usage: cursor block | bar | underline', style: 'dim' });
    return;
  }
  if (!['block', 'bar', 'underline'].includes(style)) {
    usage('cursor', `Unknown style '${style}'.`, ctx);
    return;
  }
  ctx.update({ cursor: style as AppSettings['cursor'] });
  ctx.print({ text: `CURSOR SET TO ${style.toUpperCase()}.`, style: 'success' });
}

function animations(parsed: ParsedCommand, ctx: CommandContext): void {
  const value = parsed.args[0]?.toLowerCase();
  if (value && !['on', 'off', 'enable', 'disable'].includes(value)) {
    usage('animations', 'Use on or off.', ctx);
    return;
  }
  const enabled = value ? value === 'on' || value === 'enable' : !ctx.settings.animations;
  ctx.update({ animations: enabled });
  ctx.print({ text: `ANIMATIONS ${enabled ? 'ENABLED' : 'DISABLED'}.`, style: 'success' });
}

function hostname(parsed: ParsedCommand, ctx: CommandContext): void {
  const value = parsed.args[0];
  if (!value) {
    ctx.print(ctx.settings.hostname);
    return;
  }
  const sanitized = value.replace(/[^A-Za-z0-9._-]/g, '').slice(0, 16);
  if (!sanitized) {
    usage('hostname', 'Use letters, digits, dots, dashes or underscores.', ctx);
    return;
  }
  ctx.update({ hostname: sanitized });
  ctx.print({ text: `HOSTNAME SET TO ${sanitized.toUpperCase()}.`, style: 'success' });
}

async function history(parsed: ParsedCommand, ctx: CommandContext): Promise<void> {
  const arg = parsed.args[0]?.toLowerCase();
  if (arg === 'clear') {
    await clearHistory();
    ctx.print({ text: 'COMMAND HISTORY CLEARED.', style: 'success' });
    return;
  }
  const limit = arg ? Number(arg) : 30;
  if (Number.isNaN(limit)) {
    usage('history', 'Use a number or clear.', ctx);
    return;
  }
  const entries = await loadHistory();
  if (entries.length === 0) {
    ctx.print({ text: 'HISTORY IS EMPTY.', style: 'dim' });
    return;
  }
  const slice = entries.slice(-Math.max(1, Math.min(limit, 100)));
  ctx.print([
    { text: `COMMAND HISTORY (LAST ${slice.length})`, style: 'banner' },
    { text: '', style: 'dim' },
    ...slice.map((command) => ({ text: `  ${command}` })),
  ]);
}

function version(ctx: CommandContext): void {
  const info = getDeviceInfo();
  ctx.print([
    { text: 'T3rm!n@l', style: 'banner' },
    { text: 'Your phone. Your rules.', style: 'dim' },
    { text: '', style: 'dim' },
    { text: `APP      : ${info.appVersion} (${info.appBuild})` },
    { text: `RUNTIME  : ${info.osName} ${info.osVersion}` },
    { text: `EXPO GO  : ${info.packageName}` },
    { text: '', style: 'dim' },
    { text: 'Offline by design. No analytics, no account.', style: 'dim' },
  ]);
}

async function ping(parsed: ParsedCommand, ctx: CommandContext): Promise<void> {
  const target = parsed.args[0] ?? 'https://1.1.1.1';
  if (!/^[A-Za-z0-9.:/_?=&%#-]{1,200}$/.test(target)) {
    usage('ping', 'Invalid host.', ctx);
    return;
  }
  const url = target.startsWith('http') ? target : `https://${target}`;
  ctx.print({ text: `PING ${url}`, style: 'banner' });
  ctx.print({ text: 'HTTPS latency probe (React Native has no raw sockets).', style: 'dim' });
  ctx.print('');

  const latencies: number[] = [];
  let failure: string | null = null;

  for (let attempt = 0; attempt < 3; attempt += 1) {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 4000);
    const started = Date.now();
    try {
      await fetch(url, { method: 'HEAD', signal: controller.signal, cache: 'no-store' });
      const elapsed = Date.now() - started;
      latencies.push(elapsed);
      ctx.print({ text: `  seq=${attempt}  time=${elapsed}ms` });
    } catch (error) {
      failure = error instanceof Error ? error.name : 'ERROR';
    } finally {
      clearTimeout(timeout);
    }
  }

  if (latencies.length === 0) {
    ctx.print({ text: '', style: 'dim' });
    ctx.print({ text: 'HOST UNREACHABLE.', style: 'error' });
    ctx.print({ text: `Reason: ${failure ?? 'timeout'}.`, style: 'dim' });
    return;
  }

  const average = Math.round(latencies.reduce((sum, value) => sum + value, 0) / latencies.length);
  ctx.print({ text: '', style: 'dim' });
  ctx.print({
    text: `${latencies.length}/3 received · avg ${average}ms · min ${Math.min(...latencies)}ms · max ${Math.max(...latencies)}ms`,
    style: 'success',
  });
}

function unknown(parsed: ParsedCommand, ctx: CommandContext): void {
  ctx.print({ text: `COMMAND NOT FOUND: ${parsed.name}`, style: 'error' });
  ctx.print({ text: '', style: 'dim' });
  const suggestions = suggest(parsed.name, commandNames()).map((name) =>
    parsed.args.length ? `${name} ${parsed.rawArgs}` : name
  );
  if (suggestions.length) {
    ctx.print({ text: 'Did you mean:', style: 'dim' });
    suggestions.forEach((line) => ctx.print({ text: `  ${line}`, style: 'accent' }));
  } else {
    ctx.print({ text: "Type 'help' to see available commands.", style: 'dim' });
  }
}

export async function getRecentSummary(): Promise<string[]> {
  const history = await loadHistory();
  return history.slice(-5).map((entry, index) => `${index + 1}. ${entry}`);
}

export function shortAge(timestamp: number): string {
  return formatShortDuration(Date.now() - timestamp);
}

export { parseCommand };
