package com.aicode.feature.agent.domain.mcp.server

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aicode.core.util.FileLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** App 级 DataStore：MCP 服务端配置。 */
private val Context.mcpServerDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mcp_server_config"
)

/**
 * MCP 服务端配置仓库：用 DataStore 持久化服务端的开关、端口、绑定方式与访问令牌。
 *
 * 与客户端配置 [com.aicode.feature.agent.domain.mcp.McpConfigRepository] 分开：
 *  - 客户端配置跟工作区走（文件式，多项目隔离）
 *  - 服务端配置是 App 全局的（单一端口，全局唯一）
 */
@Singleton
class McpServerConfigRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private companion object {
        const val TAG = "McpServerConfigRepository"
        val KEY_ENABLED = booleanPreferencesKey("enabled")
        val KEY_PORT = intPreferencesKey("port")
        val KEY_BIND_ALL = booleanPreferencesKey("bind_all_interfaces")
        val KEY_TOKEN = stringPreferencesKey("auth_token")
    }

    /** 读取配置（缺失项用默认值）。 */
    suspend fun load(): McpServerConfig {
        return try {
            val prefs = context.mcpServerDataStore.data.first()
            McpServerConfig(
                enabled = prefs[KEY_ENABLED] ?: false,
                port = prefs[KEY_PORT] ?: McpServerConfig.DEFAULT_PORT,
                bindAllInterfaces = prefs[KEY_BIND_ALL] ?: false,
                authToken = prefs[KEY_TOKEN] ?: ""
            )
        } catch (e: Exception) {
            FileLogger.e(TAG, "读取 MCP 服务端配置失败，使用默认值", e)
            McpServerConfig()
        }
    }

    /** 保存配置。 */
    suspend fun save(config: McpServerConfig) {
        try {
            context.mcpServerDataStore.edit { prefs ->
                prefs[KEY_ENABLED] = config.enabled
                prefs[KEY_PORT] = config.port
                prefs[KEY_BIND_ALL] = config.bindAllInterfaces
                prefs[KEY_TOKEN] = config.authToken
            }
            FileLogger.i(TAG, "MCP 服务端配置已保存: enabled=${config.enabled}, port=${config.port}")
        } catch (e: Exception) {
            FileLogger.e(TAG, "保存 MCP 服务端配置失败", e)
        }
    }
}
