package com.aicode.feature.settings.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aicode.R
import com.aicode.core.theme.Spacing
import com.aicode.core.theme.semanticColors
import com.aicode.core.ui.AppSwitch
import com.aicode.core.ui.AppTextField
import com.aicode.feature.settings.data.repository.StartupSessionMode
import compose.icons.FeatherIcons
import compose.icons.feathericons.Check

/**
 * 通用设置：集中放置不属于单个提供商配置、也不针对某个专用模型的全局偏好。
 */
@Composable
internal fun GeneralSettingsSection(
    autoRemoveStaleModels: Boolean,
    onToggleAutoRemoveStaleModels: (Boolean) -> Unit,
    startupSessionMode: StartupSessionMode,
    onSelectStartupSessionMode: (StartupSessionMode) -> Unit,
    firstByteTimeoutSec: Int,
    onSetFirstByteTimeoutSec: (Int) -> Unit,
    streamIdleTimeoutSec: Int,
    onSetStreamIdleTimeoutSec: (Int) -> Unit
) {
    var showStartupSessionSheet by remember { mutableStateOf(false) }
    var editingFirstByteTimeout by remember { mutableStateOf(false) }
    var editingStreamIdleTimeout by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg)
            .padding(bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        SettingsGroupHeader(text = stringResource(R.string.settings_general_models))
        SettingsGroup {
            SettingsRow(
                icon = null,
                title = stringResource(R.string.settings_auto_remove_stale_models),
                subtitle = stringResource(R.string.settings_auto_remove_stale_models_desc),
                trailing = {
                    AppSwitch(
                        checked = autoRemoveStaleModels,
                        onCheckedChange = onToggleAutoRemoveStaleModels
                    )
                }
            )
        }

        SettingsGroupHeader(text = stringResource(R.string.settings_general_session))
        SettingsGroup {
            SettingsRow(
                icon = null,
                title = stringResource(R.string.settings_startup_session),
                onClick = { showStartupSessionSheet = true },
                trailing = {
                    Text(
                        text = stringResource(startupSessionMode.labelRes()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.semanticColors.subtleText
                    )
                }
            )
        }

        SettingsGroupHeader(text = stringResource(R.string.settings_general_network))
        SettingsGroup {
            SettingsRow(
                icon = null,
                title = stringResource(R.string.settings_first_byte_timeout),
                subtitle = stringResource(R.string.settings_first_byte_timeout_desc),
                onClick = { editingFirstByteTimeout = true },
                trailing = {
                    Text(
                        text = timeoutLabel(firstByteTimeoutSec),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.semanticColors.subtleText
                    )
                }
            )
            SettingsDivider()
            SettingsRow(
                icon = null,
                title = stringResource(R.string.settings_stream_idle_timeout),
                subtitle = stringResource(R.string.settings_stream_idle_timeout_desc),
                onClick = { editingStreamIdleTimeout = true },
                trailing = {
                    Text(
                        text = timeoutLabel(streamIdleTimeoutSec),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.semanticColors.subtleText
                    )
                }
            )
        }
    }

    if (showStartupSessionSheet) {
        StartupSessionSheet(
            selected = startupSessionMode,
            onSelect = {
                onSelectStartupSessionMode(it)
                showStartupSessionSheet = false
            },
            onDismiss = { showStartupSessionSheet = false }
        )
    }

    if (editingFirstByteTimeout) {
        TimeoutInputDialog(
            title = stringResource(R.string.settings_first_byte_timeout),
            initialSec = firstByteTimeoutSec,
            onConfirm = {
                onSetFirstByteTimeoutSec(it)
                editingFirstByteTimeout = false
            },
            onDismiss = { editingFirstByteTimeout = false }
        )
    }

    if (editingStreamIdleTimeout) {
        TimeoutInputDialog(
            title = stringResource(R.string.settings_stream_idle_timeout),
            initialSec = streamIdleTimeoutSec,
            onConfirm = {
                onSetStreamIdleTimeoutSec(it)
                editingStreamIdleTimeout = false
            },
            onDismiss = { editingStreamIdleTimeout = false }
        )
    }
}

/** 超时值的行尾展示：0（或负数）视为不限制。 */
@Composable
private fun timeoutLabel(sec: Int): String =
    if (sec <= 0) stringResource(R.string.settings_timeout_unlimited)
    else stringResource(R.string.settings_timeout_seconds, sec)

/**
 * 超时时间输入弹窗：只接受非负整数秒，留空或填 0 即不限制。
 */
@Composable
private fun TimeoutInputDialog(
    title: String,
    initialSec: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(if (initialSec > 0) initialSec.toString() else "") }
    val parsed = text.trim().toIntOrNull()
    val isValid = text.isBlank() || (parsed != null && parsed >= 0)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(title) },
        text = {
            AppTextField(
                value = text,
                onValueChange = { input -> text = input.filter { it.isDigit() } },
                placeholder = stringResource(R.string.settings_timeout_unlimited),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = !isValid,
                supportingText = { Text(stringResource(R.string.settings_timeout_input_hint)) }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(parsed ?: 0) },
                enabled = isValid
            ) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartupSessionSheet(
    selected: StartupSessionMode,
    onSelect: (StartupSessionMode) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.xl)
        ) {
            Text(
                text = stringResource(R.string.settings_startup_session),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(horizontal = Spacing.lg)
                    .padding(bottom = Spacing.md)
            )

            StartupSessionMode.entries.forEach { mode ->
                val isSelected = mode == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(mode) }
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(mode.labelRes()),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(mode.descRes()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = FeatherIcons.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun StartupSessionMode.labelRes(): Int = when (this) {
    StartupSessionMode.NEW_SESSION -> R.string.settings_startup_session_new
    StartupSessionMode.RECENT_SESSION -> R.string.settings_startup_session_recent
}

private fun StartupSessionMode.descRes(): Int = when (this) {
    StartupSessionMode.NEW_SESSION -> R.string.settings_startup_session_new_desc
    StartupSessionMode.RECENT_SESSION -> R.string.settings_startup_session_recent_desc
}