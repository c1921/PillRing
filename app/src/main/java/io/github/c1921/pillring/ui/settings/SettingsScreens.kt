package io.github.c1921.pillring.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.c1921.pillring.R
import io.github.c1921.pillring.locale.AppLanguage
import io.github.c1921.pillring.permission.PermissionAction
import io.github.c1921.pillring.permission.PermissionHealthItem
import io.github.c1921.pillring.permission.PermissionState
import io.github.c1921.pillring.ui.UiTestTags
import io.github.c1921.pillring.ui.theme.PillRingTheme
import io.github.c1921.pillring.update.AppUpdateRepository
import io.github.c1921.pillring.update.UpdateStatus
import io.github.c1921.pillring.update.UpdateUiState

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun SettingsOverviewScreen(
    permissionItems: List<PermissionHealthItem>,
    selectedLanguage: AppLanguage,
    effectiveLanguageForSummary: AppLanguage,
    appVersionName: String,
    onBackClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onPermissionClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    val languageSummary = languageSummaryText(
        selectedLanguage = selectedLanguage,
        effectiveLanguageForSummary = effectiveLanguageForSummary
    )
    val permissionSummary = permissionOverviewSummary(permissionItems)
    val aboutVersionName = appVersionName.ifBlank {
        stringResource(R.string.settings_about_version_unknown)
    }
    val aboutSummary = stringResource(R.string.settings_about_summary, aboutVersionName)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_page_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag(UiTestTags.SETTINGS_BACK_BUTTON)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SettingsOverviewItem(
                    title = stringResource(R.string.settings_language_title),
                    summary = languageSummary,
                    icon = Icons.Outlined.Language,
                    testTag = UiTestTags.SETTINGS_LANGUAGE_ITEM,
                    iconTestTag = UiTestTags.SETTINGS_LANGUAGE_ICON,
                    onClick = onLanguageClick
                )
            }
            item {
                SettingsOverviewItem(
                    title = stringResource(R.string.permission_health_title),
                    summary = permissionSummary,
                    icon = Icons.Outlined.VerifiedUser,
                    testTag = UiTestTags.SETTINGS_PERMISSION_ITEM,
                    iconTestTag = UiTestTags.SETTINGS_PERMISSION_ICON,
                    onClick = onPermissionClick
                )
            }
            item {
                SettingsOverviewItem(
                    title = stringResource(R.string.settings_about_title),
                    summary = aboutSummary,
                    icon = Icons.Outlined.Info,
                    testTag = UiTestTags.SETTINGS_ABOUT_ITEM,
                    iconTestTag = UiTestTags.SETTINGS_ABOUT_ICON,
                    onClick = onAboutClick
                )
            }
        }
    }
}

@Composable
private fun SettingsOverviewItem(
    title: String,
    summary: String,
    icon: ImageVector,
    testTag: String,
    iconTestTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(iconTestTag)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun AboutSettingsScreen(
    appVersionName: String,
    updateUiState: UpdateUiState,
    onUpdateClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val aboutVersionName = appVersionName.ifBlank {
        stringResource(R.string.settings_about_version_unknown)
    }
    val updateSummary = updateOverviewSummary(updateUiState)
    val updateActionLabelResId = when (updateUiState.status) {
        UpdateStatus.CHECKING -> R.string.settings_update_action_checking
        UpdateStatus.UPDATE_AVAILABLE -> R.string.settings_update_action_open_release
        UpdateStatus.IDLE,
        UpdateStatus.UP_TO_DATE,
        UpdateStatus.FAILED -> R.string.settings_update_action_check_now
    }
    val updateActionEnabled = updateUiState.status != UpdateStatus.CHECKING

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag(UiTestTags.SETTINGS_ABOUT_PAGE),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_about_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag(UiTestTags.SETTINGS_BACK_BUTTON)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_about_version_label),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = aboutVersionName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag(UiTestTags.SETTINGS_ABOUT_VERSION_VALUE)
                        )
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(UiTestTags.SETTINGS_ABOUT_UPDATE_CARD),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_update_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = updateSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag(UiTestTags.SETTINGS_ABOUT_UPDATE_SUMMARY)
                        )
                        FilledTonalButton(
                            onClick = onUpdateClick,
                            enabled = updateActionEnabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(UiTestTags.SETTINGS_ABOUT_UPDATE_BUTTON)
                        ) {
                            Text(text = stringResource(updateActionLabelResId))
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun LanguageSettingsScreen(
    selectedLanguage: AppLanguage,
    effectiveLanguageForSummary: AppLanguage,
    onBackClick: () -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit
) {
    val summary = languageSummaryText(
        selectedLanguage = selectedLanguage,
        effectiveLanguageForSummary = effectiveLanguageForSummary
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_language_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag(UiTestTags.SETTINGS_BACK_BUTTON)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_language_dialog_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LanguageOptionRow(
                            text = stringResource(R.string.settings_language_option_system),
                            selected = selectedLanguage == AppLanguage.SYSTEM,
                            testTag = UiTestTags.SETTINGS_LANGUAGE_OPTION_SYSTEM,
                            onClick = { onLanguageSelected(AppLanguage.SYSTEM) }
                        )
                        LanguageOptionRow(
                            text = stringResource(R.string.settings_language_option_english),
                            selected = selectedLanguage == AppLanguage.ENGLISH,
                            testTag = UiTestTags.SETTINGS_LANGUAGE_OPTION_ENGLISH,
                            onClick = { onLanguageSelected(AppLanguage.ENGLISH) }
                        )
                        LanguageOptionRow(
                            text = stringResource(R.string.settings_language_option_chinese),
                            selected = selectedLanguage == AppLanguage.CHINESE_SIMPLIFIED,
                            testTag = UiTestTags.SETTINGS_LANGUAGE_OPTION_CHINESE,
                            onClick = { onLanguageSelected(AppLanguage.CHINESE_SIMPLIFIED) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun PermissionSettingsScreen(
    permissionItems: List<PermissionHealthItem>,
    onBackClick: () -> Unit,
    onOpenPermissionSettings: (PermissionAction) -> Unit
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag(UiTestTags.SETTINGS_PERMISSION_PAGE),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.permission_health_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag(UiTestTags.SETTINGS_BACK_BUTTON)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            item {
                PermissionHealthPanel(
                    items = permissionItems,
                    onOpenPermissionSettings = onOpenPermissionSettings
                )
            }
        }
    }
}

@Composable
private fun permissionOverviewSummary(items: List<PermissionHealthItem>): String {
    val needsActionCount = items.count { it.state == PermissionState.NEEDS_ACTION }
    if (needsActionCount > 0) {
        return pluralStringResource(
            R.plurals.settings_permission_summary_needs_action,
            needsActionCount,
            needsActionCount
        )
    }

    val manualCheckCount = items.count { it.state == PermissionState.MANUAL_CHECK }
    if (manualCheckCount > 0) {
        return pluralStringResource(
            R.plurals.settings_permission_summary_manual_check,
            manualCheckCount,
            manualCheckCount
        )
    }

    return stringResource(R.string.settings_permission_summary_all_ok)
}

@Composable
private fun updateOverviewSummary(updateUiState: UpdateUiState): String {
    return when (updateUiState.status) {
        UpdateStatus.IDLE -> stringResource(R.string.settings_update_summary_idle)
        UpdateStatus.CHECKING -> stringResource(R.string.settings_update_summary_checking)
        UpdateStatus.UPDATE_AVAILABLE -> {
            val currentVersionName = updateUiState.currentVersionName.ifBlank {
                stringResource(R.string.settings_about_version_unknown)
            }
            val latestVersionName = updateUiState.latestVersionName
                ?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.settings_about_version_unknown)
            stringResource(
                R.string.settings_update_summary_available,
                currentVersionName,
                latestVersionName
            )
        }

        UpdateStatus.UP_TO_DATE -> stringResource(R.string.settings_update_summary_up_to_date)
        UpdateStatus.FAILED -> stringResource(R.string.settings_update_summary_failed)
    }
}

@Composable
private fun LanguageOptionRow(
    text: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick
            )
            .testTag(testTag)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun languageSummaryText(
    selectedLanguage: AppLanguage,
    effectiveLanguageForSummary: AppLanguage
): String {
    return when (selectedLanguage) {
        AppLanguage.SYSTEM -> {
            val effectiveLanguageLabel = stringResource(
                if (effectiveLanguageForSummary == AppLanguage.CHINESE_SIMPLIFIED) {
                    R.string.settings_language_effective_chinese
                } else {
                    R.string.settings_language_effective_english
                }
            )
            stringResource(
                R.string.settings_language_summary_follow_system,
                effectiveLanguageLabel
            )
        }

        AppLanguage.ENGLISH -> stringResource(R.string.settings_language_summary_english)
        AppLanguage.CHINESE_SIMPLIFIED -> stringResource(R.string.settings_language_summary_chinese)
    }
}

@Composable
private fun PermissionHealthPanel(
    items: List<PermissionHealthItem>,
    onOpenPermissionSettings: (PermissionAction) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.permission_health_title),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(R.string.permission_health_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        items.forEach { item ->
            PermissionHealthCard(
                item = item,
                onOpenPermissionSettings = onOpenPermissionSettings
            )
        }
    }
}

@Composable
private fun PermissionHealthCard(
    item: PermissionHealthItem,
    onOpenPermissionSettings: (PermissionAction) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium
            )
            Surface(
                shape = MaterialTheme.shapes.small,
                color = permissionStateContainerColor(item.state)
            ) {
                Text(
                    text = item.statusText,
                    color = permissionStateColor(item.state),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            Text(
                text = item.detailText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FilledTonalButton(
                onClick = { onOpenPermissionSettings(item.action) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = item.actionLabel)
            }
        }
    }
}

@Composable
private fun permissionStateContainerColor(state: PermissionState): Color {
    return when (state) {
        PermissionState.OK -> MaterialTheme.colorScheme.secondaryContainer
        PermissionState.NEEDS_ACTION -> MaterialTheme.colorScheme.errorContainer
        PermissionState.MANUAL_CHECK -> MaterialTheme.colorScheme.tertiaryContainer
        PermissionState.UNAVAILABLE -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
}

@Composable
private fun permissionStateColor(state: PermissionState): Color {
    return when (state) {
        PermissionState.OK -> MaterialTheme.colorScheme.onSecondaryContainer
        PermissionState.NEEDS_ACTION -> MaterialTheme.colorScheme.onErrorContainer
        PermissionState.MANUAL_CHECK -> MaterialTheme.colorScheme.onTertiaryContainer
        PermissionState.UNAVAILABLE -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsOverviewScreenPreview() {
    PillRingTheme {
        SettingsOverviewScreen(
            permissionItems = listOf(
                PermissionHealthItem(
                    id = "notification",
                    title = "Notification permission",
                    statusText = "Needs action",
                    detailText = "Allow notifications to receive reminder popups.",
                    state = PermissionState.NEEDS_ACTION,
                    actionLabel = "Open settings",
                    action = PermissionAction.OPEN_NOTIFICATION_SETTINGS
                )
            ),
            selectedLanguage = AppLanguage.SYSTEM,
            effectiveLanguageForSummary = AppLanguage.ENGLISH,
            appVersionName = "1.0",
            onBackClick = {},
            onLanguageClick = {},
            onPermissionClick = {},
            onAboutClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AboutSettingsScreenPreview() {
    PillRingTheme {
        AboutSettingsScreen(
            appVersionName = "1.0",
            updateUiState = UpdateUiState(
                status = UpdateStatus.UPDATE_AVAILABLE,
                currentVersionName = "0.1.0",
                latestVersionName = "0.2.0",
                releaseUrl = AppUpdateRepository.DEFAULT_RELEASE_PAGE_URL,
                lastCheckedAtEpochMs = System.currentTimeMillis()
            ),
            onUpdateClick = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LanguageSettingsScreenPreview() {
    PillRingTheme {
        LanguageSettingsScreen(
            selectedLanguage = AppLanguage.SYSTEM,
            effectiveLanguageForSummary = AppLanguage.ENGLISH,
            onBackClick = {},
            onLanguageSelected = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionSettingsScreenPreview() {
    PillRingTheme {
        PermissionSettingsScreen(
            permissionItems = listOf(
                PermissionHealthItem(
                    id = "notification",
                    title = "Notification permission",
                    statusText = "Needs action",
                    detailText = "Allow notifications to receive reminder popups.",
                    state = PermissionState.NEEDS_ACTION,
                    actionLabel = "Open settings",
                    action = PermissionAction.OPEN_NOTIFICATION_SETTINGS
                )
            ),
            onBackClick = {},
            onOpenPermissionSettings = {}
        )
    }
}
