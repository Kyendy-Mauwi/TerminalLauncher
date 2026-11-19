import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { FlatList, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { APP_CATALOG, launchCatalogApp, probeApp } from '../apps';
import { getAppListModule, listInstalledApps, openInstalledApp, type InstalledApp } from '../installedApps';
import { addFavorite, loadAliases, loadFavorites, removeFavorite, type AliasMap, type Favorite } from '../storage';
import { MONO_FONT } from '../types';
import { usePalette } from '../ui';

interface Row {
  id: string;
  label: string;
  meta: string;
  packageName: string;
  system?: boolean;
}

export function AppsScreen() {
  const palette = usePalette();
  const [installed, setInstalled] = useState<InstalledApp[]>([]);
  const [usingNativeList, setUsingNativeList] = useState(false);
  const [available, setAvailable] = useState<Record<string, boolean>>({});
  const [favorites, setFavorites] = useState<Favorite[]>([]);
  const [aliases, setAliases] = useState<AliasMap>({});
  const [query, setQuery] = useState('');
  const [message, setMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    setLoading(true);
    const [aliasesMap, favoritesList, apps] = await Promise.all([
      loadAliases(),
      loadFavorites(),
      listInstalledApps(),
    ]);
    setAliases(aliasesMap);
    setFavorites(favoritesList);
    setInstalled(apps);
    setUsingNativeList(apps.length > 0 && getAppListModule() !== null);
    if (apps.length === 0) {
      const probe = await Promise.all(
        APP_CATALOG.map(async (app) => ({ id: app.id, ok: await probeApp(app) }))
      );
      setAvailable(Object.fromEntries(probe.map((entry) => [entry.id, entry.ok])));
    }
    setLoading(false);
  }, []);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const rows = useMemo<Row[]>(() => {
    if (usingNativeList) {
      const needle = query.trim().toLowerCase();
      return installed
        .filter(
          (app) =>
            !needle ||
            app.label.toLowerCase().includes(needle) ||
            app.packageName.toLowerCase().includes(needle)
        )
        .map((app) => ({
          id: app.packageName,
          label: app.label,
          meta: app.packageName,
          packageName: app.packageName,
          system: app.system,
        }));
    }
    return APP_CATALOG.map((app) => ({
      id: app.id,
      label: app.label,
      meta: `${aliases[app.id] ? `${aliases[app.id]} · ` : ''}${app.packageName}`,
      packageName: app.packageName,
    }));
  }, [aliases, installed, query, usingNativeList]);

  const toggleFavorite = useCallback(
    async (row: Row) => {
      const isFavorite = favorites.some((entry) => entry.id === row.id);
      if (isFavorite) {
        await removeFavorite(row.id);
        setMessage(`${row.label} removed from favourites.`);
      } else {
        await addFavorite({ id: row.id, label: row.label });
        setMessage(`${row.label} added to favourites.`);
      }
      setFavorites(await loadFavorites());
    },
    [favorites]
  );

  const openApp = useCallback(
    async (row: Row) => {
      setMessage(`LAUNCHING ${row.label.toUpperCase()}...`);
      if (usingNativeList) {
        const opened = await openInstalledApp(row.packageName);
        setMessage(opened ? `${row.label} opened.` : `${row.label} could not be opened.`);
        return;
      }
      const catalogApp = APP_CATALOG.find((app) => app.id === row.id);
      if (!catalogApp) {
        setMessage(`${row.label} could not be opened.`);
        return;
      }
      const result = await launchCatalogApp(catalogApp);
      setMessage(result.message);
    },
    [usingNativeList]
  );

  return (
    <View style={[styles.container, { backgroundColor: palette.background }]}>
      <View style={styles.header}>
        <Text style={[styles.title, { color: palette.accent }]}>APPLICATIONS</Text>
        <Pressable onPress={refresh} accessibilityRole="button" accessibilityLabel="Refresh apps" hitSlop={8}>
          <Text style={[styles.action, { color: palette.secondary }]}>REFRESH</Text>
        </Pressable>
      </View>
      <View style={[styles.divider, { backgroundColor: palette.border }]} />

      <Text style={[styles.note, { color: palette.secondary }]}>
        {usingNativeList
          ? `${installed.length} APPLICATIONS INSTALLED. TAP TO OPEN.`
          : 'Expo Go cannot list installed packages. Run the development build for the full list.'}
      </Text>

      {usingNativeList && (
        <View style={[styles.search, { borderColor: palette.border }]}>
          <Text style={[styles.searchPrompt, { color: palette.primary }]}>/</Text>
          <TextInput
            value={query}
            onChangeText={setQuery}
            placeholder="filter by name or package"
            placeholderTextColor={palette.secondary}
            style={[styles.searchInput, { color: palette.primary }]}
            autoCapitalize="none"
            autoCorrect={false}
            returnKeyType="search"
            accessibilityLabel="Filter applications"
          />
        </View>
      )}

      {message && <Text style={[styles.message, { color: palette.warning }]}>{message}</Text>}
      {favorites.length > 0 && (
        <Text style={[styles.favorites, { color: palette.primary }]} numberOfLines={2}>
          ★ {favorites.map((favorite) => favorite.label).join('   ')}
        </Text>
      )}

      <FlatList
        data={rows}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.list}
        keyboardShouldPersistTaps="handled"
        initialNumToRender={24}
        windowSize={12}
        ListEmptyComponent={
          <Text style={{ color: palette.secondary, fontFamily: MONO_FONT }}>
            {loading ? 'SCANNING…' : `NO APPLICATIONS MATCH “${query}”`}
          </Text>
        }
        renderItem={({ item }) => {
          const isFavorite = favorites.some((entry) => entry.id === item.id);
          const marker = usingNativeList ? '›' : available[item.id] ? '›' : '!';
          return (
            <View style={[styles.row, { borderBottomColor: palette.border }]}>
              <Pressable
                style={styles.rowMain}
                onPress={() => void openApp(item)}
                accessibilityRole="button"
                accessibilityLabel={`Open ${item.label}`}
              >
                <Text style={[styles.appLabel, { color: palette.primary }]}>
                  {marker} {item.label}
                  {item.system ? '  ·' : ''}
                </Text>
                <Text style={[styles.appMeta, { color: palette.secondary }]} numberOfLines={1}>
                  {item.meta}
                </Text>
              </Pressable>
              <Pressable
                onPress={() => void toggleFavorite(item)}
                accessibilityRole="button"
                accessibilityLabel={isFavorite ? `Remove ${item.label} from favourites` : `Add ${item.label} to favourites`}
                hitSlop={8}
              >
                <Text style={{ color: isFavorite ? palette.warning : palette.secondary, fontSize: 16 }}>
                  {isFavorite ? '★' : '☆'}
                </Text>
              </Pressable>
            </View>
          );
        }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, paddingHorizontal: 16 },
  header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingTop: 12 },
  title: { fontFamily: MONO_FONT, fontSize: 14, fontWeight: '700', letterSpacing: 2 },
  action: { fontFamily: MONO_FONT, fontSize: 11, letterSpacing: 1 },
  divider: { height: StyleSheet.hairlineWidth, marginTop: 8 },
  note: { fontFamily: MONO_FONT, fontSize: 10, marginTop: 10, lineHeight: 15 },
  search: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: StyleSheet.hairlineWidth,
    borderRadius: 2,
    paddingHorizontal: 8,
    marginTop: 10,
    gap: 6,
  },
  searchPrompt: { fontFamily: MONO_FONT, fontSize: 12 },
  searchInput: { flex: 1, fontFamily: MONO_FONT, fontSize: 12, paddingVertical: 7 },
  message: { fontFamily: MONO_FONT, fontSize: 11, marginTop: 8 },
  favorites: { fontFamily: MONO_FONT, fontSize: 11, marginTop: 8 },
  list: { paddingTop: 8, paddingBottom: 24 },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 11,
    borderBottomWidth: StyleSheet.hairlineWidth,
  },
  rowMain: { flex: 1, gap: 2 },
  appLabel: { fontFamily: MONO_FONT, fontSize: 13 },
  appMeta: { fontFamily: MONO_FONT, fontSize: 10 },
});
