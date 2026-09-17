package com.aicode.feature.agent.domain.mcp.server

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aicode.core.theme.Spacing

/**
 * MCP 服务端面板：显示运行状态、可访问地址，并提供启停开关。
 *
 * 注意：这是最小可验证版本，文案暂时硬编码；后续会迁移到 strings.xml 双语。
 */
@Composable
internal fun McpServerPanel(
    manager: McpServerManager,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by manager.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "MCP 服务端",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (state.running) "运行中 · 端口 ${state.port}" else "已停止",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = state.running,
                onCheckedChange = onToggle
            )
        }

        if (state.running && state.addresses.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.xs))
            state.addresses.forEach { addr ->
                Text(
                    text = addr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (state.authEnabled) {
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = "访问保护：已开启",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
