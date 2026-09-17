package com.aicode.feature.agent.domain.mcp.server

import com.aicode.core.util.FileLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MCP 服务端运行状态快照，供 UI 展示。
 *
 * @param running 是否正在运行
 * @param port 监听端口
 * @param path 路径（默认 /mcp）
 * @param addresses 可访问的地址列表（本机 + 局域网 IP）
 * @param authEnabled 是否启用了访问保护
 */
data class McpServerState(
    val running: Boolean = false,
    val port: Int = McpServerConfig.DEFAULT_PORT,
    val path: String = McpServerConfig.DEFAULT_PATH,
    val addresses: List<String> = emptyList(),
    val authEnabled: Boolean = false
)

/**
 * MCP 服务端总管家：负责启动/停止 HTTP 服务、维护运行状态、收集可访问地址。
 *
 * 与客户端的 [com.aicode.feature.agent.domain.mcp.McpManager] 相对：
 *  - McpManager：把外部 MCP server 的工具“拉进来”给 App 内 Agent 用
 *  - McpServerManager：把 App 内工具“暴露出去”给外部 AI 客户端用
 */
@Singleton
class McpServerManager @Inject constructor(
    private val httpServer: McpHttpServer,
    private val configRepository: McpServerConfigRepository
) {
    private companion object {
        const val TAG = "McpServerManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(McpServerState())
    val state: StateFlow<McpServerState> = _state.asStateFlow()

    /** App 启动时调用：若配置里启用了服务端则自动启动。 */
    fun start() {
        scope.launch {
            val config = configRepository.load()
            if (config.enabled) {
                applyConfig(config)
            } else {
                _state.value = McpServerState(port = config.port, authEnabled = config.authEnabled)
            }
        }
    }

    /** 更新配置并应用（启用则启动，禁用则停止）。 */
    fun applyConfig(config: McpServerConfig) {
        if (config.enabled) {
            startServer(config)
        } else {
            stopServer(config)
        }
    }

    /** 直接启动（使用给定配置）。 */
    fun startServer(config: McpServerConfig) {
        try {
            httpServer.start(config.port)
            _state.value = McpServerState(
                running = true,
                port = config.port,
                addresses = collectAddresses(config),
                authEnabled = config.authEnabled
            )
            FileLogger.i(TAG, "MCP 服务端已启动，端口 ${config.port}")
        } catch (e: Exception) {
            FileLogger.e(TAG, "MCP 服务端启动失败", e)
            _state.value = McpServerState(port = config.port, authEnabled = config.authEnabled)
        }
    }

    /** 停止服务端。 */
    fun stopServer(config: McpServerConfig = McpServerState().let {
        McpServerConfig(port = _state.value.port)
    }) {
        httpServer.stop()
        _state.value = McpServerState(port = config.port, authEnabled = config.authEnabled)
        FileLogger.i(TAG, "MCP 服务端已停止")
    }

    fun isRunning(): Boolean = httpServer.isRunning()

    /** 收集可访问地址：本机 + 局域网。 */
    private fun collectAddresses(config: McpServerConfig): List<String> {
        val port = config.port
        val path = McpServerConfig.DEFAULT_PATH
        val list = mutableListOf<String>()
        list.add("http://127.0.0.1:$port$path")
        if (config.bindAllInterfaces) {
            runCatching {
                NetworkInterface.getNetworkInterfaces().toList().forEach { ni ->
                    ni.inetAddresses.toList().forEach { addr ->
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            list.add("http://${addr.hostAddress}:$port$path")
                        }
                    }
                }
            }.onFailure { FileLogger.e(TAG, "收集局域网地址失败", it) }
        }
        return list.distinct()
    }
}
