package com.aicode.feature.agent.domain.mcp.server

import com.aicode.core.util.FileLogger
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.header
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 基于 Ktor CIO 的轻量 MCP HTTP 服务器。
 * 监听 [McpServerConfig.port]，在 [McpServerConfig.DEFAULT_PATH] 上处理 JSON-RPC 请求。
 *
 * 若配置里 authToken 非空，则要求请求头 `Authorization: Bearer <token>`，否则返回 401。
 */
@Singleton
class McpHttpServer @Inject constructor(
    private val protocol: McpServerProtocol,
    private val configRepository: McpServerConfigRepository
) {
    private companion object {
        const val TAG = "McpHttpServer"
        val DEFAULT_JSON = Json { ignoreUnknownKeys = true }
    }

    private var server: EmbeddedServer<*, *>? = null
    private var currentPort: Int = -1

    fun isRunning(): Boolean = server != null

    fun start(port: Int) {
        if (server != null && currentPort == port) return
        stop()
        FileLogger.i(TAG, "启动 MCP HTTP 服务，端口 $port")
        val s = embeddedServer(CIO, port = port) {
            install(ContentNegotiation) { json(DEFAULT_JSON) }
            routing {
                post(McpServerConfig.DEFAULT_PATH) {
                    // 访问保护：token 非空时校验 Bearer
                    val config = configRepository.load()
                    if (config.authEnabled) {
                        val header = call.request.header("Authorization") ?: ""
                        val expected = "Bearer ${config.authToken}"
                        if (header != expected) {
                            FileLogger.i(TAG, "拒绝未授权请求（token 不匹配）")
                            call.respondText("unauthorized", ContentType.Text.Plain, HttpStatusCode.Unauthorized)
                            return@post
                        }
                    }
                    val body = call.receiveText()
                    val req = runCatching { DEFAULT_JSON.parseToJsonElement(body).jsonObject }.getOrNull()
                    if (req == null) {
                        call.respondText("invalid json", ContentType.Text.Plain, HttpStatusCode.BadRequest)
                        return@post
                    }
                    val resp = protocol.handle(req)
                    if (resp == null) {
                        call.respondText("", ContentType.Application.Json, HttpStatusCode.Accepted)
                    } else {
                        call.respondText(resp.toString(), ContentType.Application.Json)
                    }
                }
            }
        }
        s.start(wait = false)
        server = s
        currentPort = port
        FileLogger.i(TAG, "MCP HTTP 服务已启动")
    }

    fun stop() {
        server?.let {
            runCatching { it.stop(1000, 2000) }
            FileLogger.i(TAG, "MCP HTTP 服务已停止")
        }
        server = null
        currentPort = -1
    }
}
