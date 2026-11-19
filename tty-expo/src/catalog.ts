export type Category = 'NAVIGATION' | 'APPLICATIONS' | 'SYSTEM' | 'PRODUCTIVITY' | 'APPEARANCE' | 'UTILITY';

export interface CommandSpec {
  name: string;
  usage: string;
  description: string;
  category: Category;
  aliases?: string[];
}

export const COMMANDS: CommandSpec[] = [
  { name: 'help', usage: 'help [command]', description: 'List commands or show usage for one command.', category: 'NAVIGATION', aliases: ['?'] },
  { name: 'clear', usage: 'clear', description: 'Clear the terminal output.', category: 'NAVIGATION', aliases: ['cls'] },
  { name: 'home', usage: 'home', description: 'Return to this terminal tab.', category: 'NAVIGATION' },
  { name: 'exit', usage: 'exit', description: 'Explain how to leave the app.', category: 'NAVIGATION', aliases: ['quit'] },
  { name: 'back', usage: 'back', description: 'Return to the previous screen.', category: 'NAVIGATION' },
  { name: 'about', usage: 'about', description: 'About this Expo build and its limits.', category: 'NAVIGATION', aliases: ['info'] },
  { name: 'settings', usage: 'settings [panel]', description: 'Open T3rm!n@l settings or an Android settings panel.', category: 'NAVIGATION', aliases: ['config'] },

  { name: 'apps', usage: 'apps [filter]', description: 'List openable applications on this build.', category: 'APPLICATIONS', aliases: ['list'] },
  { name: 'open', usage: 'open [application]', description: 'Open an application through its deep link.', category: 'APPLICATIONS', aliases: ['launch', 'run'] },
  { name: 'search', usage: 'search [query]', description: 'Search the app catalogue.', category: 'APPLICATIONS', aliases: ['find'] },
  { name: 'recent', usage: 'recent [n]', description: 'Applications opened in this session.', category: 'APPLICATIONS', aliases: ['recents'] },
  { name: 'favorites', usage: 'favorites [add|remove] [application]', description: 'List or manage favourite applications.', category: 'APPLICATIONS', aliases: ['fav', 'faves'] },
  { name: 'install', usage: 'install [application]', description: 'Search the Play Store for an application.', category: 'APPLICATIONS', aliases: ['get'] },
  { name: 'alias', usage: 'alias set [alias] [application] | alias remove [alias]', description: 'Manage custom application aliases.', category: 'APPLICATIONS', aliases: ['aliases'] },

  { name: 'sysinfo', usage: 'sysinfo', description: 'Full device status report.', category: 'SYSTEM', aliases: ['status'] },
  { name: 'battery', usage: 'battery', description: 'Battery level and charging state.', category: 'SYSTEM', aliases: ['bat'] },
  { name: 'storage', usage: 'storage', description: 'Free and total device storage.', category: 'SYSTEM', aliases: ['df'] },
  { name: 'memory', usage: 'memory', description: 'Memory information exposed by Expo.', category: 'SYSTEM', aliases: ['mem'] },
  { name: 'device', usage: 'device', description: 'Device model and OS version.', category: 'SYSTEM', aliases: ['uname'] },
  { name: 'uptime', usage: 'uptime', description: 'Time since the device booted.', category: 'SYSTEM' },
  { name: 'network', usage: 'network', description: 'Connectivity state and transport.', category: 'SYSTEM', aliases: ['net'] },
  { name: 'wifi', usage: 'wifi [settings]', description: 'Wi-Fi state; opens Wi-Fi settings.', category: 'SYSTEM' },
  { name: 'bluetooth', usage: 'bluetooth [settings]', description: 'Opens Bluetooth settings.', category: 'SYSTEM', aliases: ['bt'] },
  { name: 'brightness', usage: 'brightness [0-100]', description: 'Read or set this app window brightness.', category: 'SYSTEM' },
  { name: 'volume', usage: 'volume', description: 'Opens the system sound panel.', category: 'SYSTEM', aliases: ['vol'] },
  { name: 'notifications', usage: 'notifications', description: 'Explains notification access limits.', category: 'SYSTEM', aliases: ['notif'] },
  { name: 'date', usage: 'date', description: 'Current date in the configured format.', category: 'SYSTEM' },
  { name: 'time', usage: 'time', description: 'Current time in the configured format.', category: 'SYSTEM' },

  { name: 'notes', usage: 'notes [add|remove|clear] [text]', description: 'Local notes stored on the device.', category: 'PRODUCTIVITY', aliases: ['note'] },
  { name: 'calculator', usage: 'calculator [expression]', description: 'Evaluate an expression or open the calculator.', category: 'PRODUCTIVITY', aliases: ['calc'] },
  { name: 'calendar', usage: 'calendar', description: 'Open the calendar app.', category: 'PRODUCTIVITY' },
  { name: 'clock', usage: 'clock', description: 'Open alarms and clock.', category: 'PRODUCTIVITY', aliases: ['alarm'] },
  { name: 'camera', usage: 'camera', description: 'Open the camera.', category: 'PRODUCTIVITY' },
  { name: 'files', usage: 'files', description: 'Open the system document picker.', category: 'PRODUCTIVITY' },
  { name: 'contacts', usage: 'contacts', description: 'Open contacts.', category: 'PRODUCTIVITY', aliases: ['people'] },
  { name: 'messages', usage: 'messages', description: 'Open messaging.', category: 'PRODUCTIVITY', aliases: ['sms'] },
  { name: 'phone', usage: 'phone', description: 'Open the dialer.', category: 'PRODUCTIVITY', aliases: ['dial'] },
  { name: 'wallpaper', usage: 'wallpaper', description: 'Open the wallpaper chooser.', category: 'PRODUCTIVITY' },

  { name: 'theme', usage: 'theme [green|amber|cyan|matrix|mono]', description: 'Switch the terminal colour theme.', category: 'APPEARANCE', aliases: ['color', 'colour'] },
  { name: 'font', usage: 'font', description: 'Explain the font used by this build.', category: 'APPEARANCE' },
  { name: 'fontsize', usage: 'fontsize [9-22]', description: 'Set terminal text size.', category: 'APPEARANCE', aliases: ['fs'] },
  { name: 'opacity', usage: 'opacity [40-100]', description: 'Set terminal background opacity.', category: 'APPEARANCE' },
  { name: 'cursor', usage: 'cursor [block|bar|underline]', description: 'Set the cursor style.', category: 'APPEARANCE' },
  { name: 'animations', usage: 'animations [on|off]', description: 'Toggle terminal animations.', category: 'APPEARANCE', aliases: ['anim'] },

  { name: 'whoami', usage: 'whoami', description: 'Show the configured terminal user.', category: 'UTILITY' },
  { name: 'hostname', usage: 'hostname [new-name]', description: 'Show or set the device alias.', category: 'UTILITY' },
  { name: 'history', usage: 'history [n|clear]', description: 'Show or clear stored command history.', category: 'UTILITY' },
  { name: 'echo', usage: 'echo [text]', description: 'Print text back to the terminal.', category: 'UTILITY' },
  { name: 'version', usage: 'version', description: 'Build information.', category: 'UTILITY', aliases: ['ver'] },
  { name: 'ping', usage: 'ping [host]', description: 'HTTPS latency probe.', category: 'UTILITY' },
  { name: 'reboot', usage: 'reboot', description: 'Explains why reboot is unavailable.', category: 'UTILITY' },
];

const lookup = new Map<string, CommandSpec>();
for (const spec of COMMANDS) {
  lookup.set(spec.name, spec);
  for (const alias of spec.aliases ?? []) lookup.set(alias, spec);
}

export function findCommand(input: string): CommandSpec | undefined {
  return lookup.get(input.trim().toLowerCase());
}

export function commandNames(): string[] {
  return COMMANDS.map((spec) => spec.name);
}

export function categoryGroups(): Array<{ category: Category; specs: CommandSpec[] }> {
  const order: Category[] = ['NAVIGATION', 'APPLICATIONS', 'SYSTEM', 'PRODUCTIVITY', 'APPEARANCE', 'UTILITY'];
  return order
    .map((category) => ({ category, specs: COMMANDS.filter((spec) => spec.category === category) }))
    .filter((group) => group.specs.length > 0);
}

export const MAX_HISTORY_NAV = 200;
