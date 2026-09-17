package com.aicode.feature.settings.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.generalDataStore by preferencesDataStore(name = "general_prefs")

/** App 启动（含切换工作区）时进入哪个会话。 */
enum class StartupSessionMode {
    /** 新开会话：复用当前工作区里还没发过消息的空会话，否则新建。 */
    NEW_SESSION,

    /** 打开最近会话：直接进入最近更新的会话，该工作区还没有会话时才新建。 */
    RECENT_SESSION
}

/**
 * 「通用设置」里的用户偏好。
 *
 * 目前两项：拉取模型成功后是否自动移除远端已不存在的本地模型（默认开启），
 * 以及启动时进入新会话还是最近会话（默认新开会话）。
 * DataStore 用法与 [KeepaliveSettingsRepository] 一致。
 */
@Singleton
class GeneralSettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private companion object {
        val AUTO_REMOVE_STALE_MODELS_KEY = booleanPreferencesKey("auto_remove_stale_models")
        val STARTUP_SESSION_MODE_KEY = stringPreferencesKey("startup_session_mode")
    }

    /** 拉取模型后自动对齐本地列表的开关流；未设置时回退到 true（默认开启）。 */
    val autoRemoveStaleModelsFlow: Flow<Boolean> =
        context.generalDataStore.data.map { it[AUTO_REMOVE_STALE_MODELS_KEY] ?: true }

    suspend fun setAutoRemoveStaleModels(enabled: Boolean) {
        context.generalDataStore.edit { it[AUTO_REMOVE_STALE_MODELS_KEY] = enabled }
    }

    /** 启动时会话偏好流；未设置或值无法识别时回退到 [StartupSessionMode.NEW_SESSION]。 */
    val startupSessionModeFlow: Flow<StartupSessionMode> = context.generalDataStore.data.map { prefs ->
        when (prefs[STARTUP_SESSION_MODE_KEY]) {
            StartupSessionMode.RECENT_SESSION.name -> StartupSessionMode.RECENT_SESSION
            else -> StartupSessionMode.NEW_SESSION
        }
    }

    suspend fun setStartupSessionMode(mode: StartupSessionMode) {
        context.generalDataStore.edit { it[STARTUP_SESSION_MODE_KEY] = mode.name }
    }

    /** 切换到工作区前读一次偏好，避免在会话初始化路径上多开一条收集流。 */
    suspend fun startupSessionMode(): StartupSessionMode = startupSessionModeFlow.first()

    /** 备份快照：当前自动对齐开关。 */
    suspend fun autoRemoveStaleModelsSnapshot(): Boolean = autoRemoveStaleModelsFlow.first()

    /** 备份快照：当前启动时会话偏好名。 */
    suspend fun startupSessionModeSnapshot(): String = startupSessionModeFlow.first().name

    /** 从备份还原自动对齐开关。 */
    suspend fun restoreAutoRemoveStaleModels(enabled: Boolean) = setAutoRemoveStaleModels(enabled)

    /** 从备份还原启动时会话偏好；旧备份无此字段（null）或值无法识别时回退新开会话。 */
    suspend fun restoreStartupSessionMode(mode: String?) {
        setStartupSessionMode(
            StartupSessionMode.entries.firstOrNull { it.name == mode } ?: StartupSessionMode.NEW_SESSION
        )
    }
}