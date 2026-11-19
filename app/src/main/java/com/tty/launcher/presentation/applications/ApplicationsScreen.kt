package com.tty.launcher.presentation.applications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tty.launcher.domain.models.AppInfo
import com.tty.launcher.presentation.components.EmptyState
import com.tty.launcher.presentation.components.SectionLabel
import com.tty.launcher.presentation.components.TerminalButton
import com.tty.launcher.presentation.components.TerminalDivider
import com.tty.launcher.presentation.components.TerminalInputField
import com.tty.launcher.presentation.theme.LocalTerminalPalette

@Composable
fun ApplicationsScreen(
    viewModel: ApplicationsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = LocalTerminalPalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .systemBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "APPLICATIONS",
                color = palette.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.weight(1f)
            )
            TerminalButton(
                label = "REFRESH",
                onClick = viewModel::refresh,
                contentDescription = "Refresh application list"
            )
        }

        TerminalInputField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            placeholder = "search applications…",
            modifier = Modifier.padding(top = 8.dp)
        )

        state.message?.let { message ->
            Text(
                text = message,
                color = palette.warning,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        if (state.favorites.isNotEmpty()) {
            SectionLabel("FAVOURITES")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = state.favorites.joinToString("   ") { it.label },
                    color = palette.primary,
                    fontSize = 12.sp,
                    maxLines = 3
                )
            }
            TerminalDivider(modifier = Modifier.padding(top = 6.dp))
        }

        SectionLabel(
            if (state.query.isBlank()) "ALL APPS (${state.applications.size})" else "RESULTS (${state.filtered.size})"
        )

        if (state.isLoading) {
            Text(text = "SCANNING PACKAGES…", color = palette.secondary, fontSize = 12.sp)
        } else if (state.filtered.isEmpty()) {
            EmptyState(
                message = "NO MATCHING APPLICATIONS.",
                hint = "Try a different search or refresh the list."
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(items = state.filtered, key = { it.packageName }) { app ->
                AppRow(
                    app = app,
                    label = state.displayName(app),
                    aliased = state.aliases.containsKey(app.packageName),
                    favorite = state.isFavorite(app.packageName),
                    onLaunch = { viewModel.launch(app) },
                    onToggleFavorite = { viewModel.toggleFavorite(app) }
                )
                TerminalDivider(color = palette.border.copy(alpha = 0.35f))
            }
            item {
                Text(
                    text = "Tap an app to launch it. Tap the star to manage favourites.",
                    color = palette.secondary,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun AppRow(
    app: AppInfo,
    label: String,
    aliased: Boolean,
    favorite: Boolean,
    onLaunch: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val palette = LocalTerminalPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onLaunch)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (favorite) "★" else "›",
            color = if (favorite) palette.warning else palette.secondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(end = 10.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = palette.primary,
                fontSize = 13.sp,
                maxLines = 1
            )
            Text(
                text = app.packageName + if (aliased) "  · alias" else "",
                color = palette.secondary,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
        TerminalButton(
            label = if (favorite) "[★]" else "[☆]",
            onClick = onToggleFavorite,
            contentDescription = if (favorite) "Remove ${app.label} from favourites" else "Add ${app.label} to favourites"
        )
    }
}
