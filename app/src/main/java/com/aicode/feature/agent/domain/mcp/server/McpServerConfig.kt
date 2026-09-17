package com.aicode.feature.agent.domain.mcp.server

/**
 * MCP 服务端配置
 *
 * 控制 AiCode 内置 MCP 服务器的启动参数：
 *  - enabled：是否启用服务端
 *  - port：监听端口（默认 8800）
 *  - bindAllInterfaces：true=监听 0.0.0.0（局域网可连），false=仅 127.0.0.1
 *  - authToken：访问令牌（空字符串=不校验，非空=要求 Bearer Token）
 */
data class McpServerConfig(
    val enabled: Boolean = false,
    val port: Int = DEFAULT_PORT,
    val bindAllInterfaces: Boolean = false,
    val authToken: String = ""
) {
    /** 是否开启了访问保护 */
    val authEnabled: Boolean get() = authToken.isNotBlank()

    companion object {
        const val DEFAULT_PORT = 8800
        const val DEFAULT_PATH = "/mcp"
    }
}
