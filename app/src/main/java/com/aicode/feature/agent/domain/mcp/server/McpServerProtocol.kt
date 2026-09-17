package com.aicode.feature.agent.domain.mcp.server

import com.aicode.core.util.FileLogger
import com.aicode.feature.agent.domain.mcp.McpToolDescriptor
import com.aicode.feature.agent.domain.tool.AgentTool
import com.aicode.feature.agent.domain.tool.ToolRegistry
import com.aicode.feature.agent.domain.tool.ToolResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MCP 服务端协议处理器：把 App 内 [ToolRegistry] 的工具桥接成 MCP 协议响应。
 *
 * 支持的 JSON-RPC 方法：
 *  - initialize: 握手，返回协议版本与服务端能力
 *  - notifications/initialized: 客户端就绪通知（无响应）
 *  - tools/list: 列出可用工具（转成 MCP 的 McpToolDescriptor）
 *  - tools/call: 执行某个工具并返回结果
 *  - ping: 存活探测
 */
@Singleton
class McpServerProtocol @Inject constructor(
    private val toolRegistry: ToolRegistry,
    private val json: Json = DEFAULT_JSON
) {
    private companion object {
        const val TAG = "McpServerProtocol"
        val DEFAULT_JSON = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        const val PROTOCOL_VERSION = "2024-11-05"
        const val SERVER_NAME = "aicode-mcp-server"
        const val SERVER_VERSION = "1.0.0"
        const val JSONRPC_VERSION = "2.0"
        const val METHOD_NOT_FOUND = -32601
        const val INVALID_PARAMS = -32602
        const val INTERNAL_ERROR = -32603
    }

    /** 处理一个 JSON-RPC 请求，返回响应 JSON；通知返回 null。 */
    suspend fun handle(request: JsonObject): JsonObject? {
        val id: JsonElement? = request["id"]
        val method = request["method"]?.jsonPrimitive?.contentOrNull
            ?: return null
        val params = request["params"]?.jsonObject
        FileLogger.i(TAG, "收到 MCP 请求: method=$method")
        return when (method) {
            "initialize" -> result(id, initializeResult())
            "notifications/initialized" -> null
            "tools/list" -> result(id, toolsListResult())
            "tools/call" -> result(id, toolsCallResult(params))
            "ping" -> result(id, buildJsonObject { })
            else -> error(id, METHOD_NOT_FOUND, "Method not found: $method")
        }
    }

    private fun initializeResult(): JsonObject = buildJsonObject {
        put("protocolVersion", PROTOCOL_VERSION)
        put("capabilities", buildJsonObject {
            put("tools", buildJsonObject { })
        })
        put("serverInfo", buildJsonObject {
            put("name", SERVER_NAME)
            put("version", SERVER_VERSION)
        })
    }

    private fun toolsListResult(): JsonObject {
        val toolsJson = toolRegistry.getAvailableTools().map { tool ->
            tool.toMcpDescriptor()
        }
        return buildJsonObject {
            put("tools", JsonArray(toolsJson))
        }
    }

    /** 把一个 AgentTool 转成 MCP 工具描述 JSON。 */
    private fun AgentTool.toMcpDescriptor(): JsonObject = buildJsonObject {
        put("name", name)
        put("description", description)
        put("inputSchema", mapToJson(toJsonSchema()))
    }

    /** 把 Map<String, Any> 递归转 JsonObject（toJsonSchema() 的返回格式）。 */
    private fun mapToJson(map: Map<String, Any>): JsonObject = buildJsonObject {
        map.forEach { (k, v) -> put(k, anyToJson(v)) }
    }

    private fun anyToJson(value: Any?): JsonElement = when (value) {
        null -> JsonNull
        is String -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is Map<*, *> -> buildJsonObject {
            value.forEach { (k, v) -> if (k != null) put(k.toString(), anyToJson(v)) }
        }
        is List<*> -> JsonArray(value.map { anyToJson(it) })
        else -> JsonPrimitive(value.toString())
    }

    private suspend fun toolsCallResult(params: JsonObject?): JsonObject {
        val name = params?.get("name")?.jsonPrimitive?.contentOrNull
            ?: return errorResult("缺少工具名 name")
        val arguments: JsonObject = params["arguments"]?.jsonObject ?: buildJsonObject { }
        val tool = toolRegistry.getTool(name)
            ?: return errorResult("工具不存在: $name")
        return try {
            val argMap: Map<String, JsonElement> = arguments
            val result = tool.execute(argMap)
            toolResultToJson(result)
        } catch (e: Exception) {
            FileLogger.e(TAG, "工具执行失败: $name", e)
            errorResult("工具执行失败: ${e.message}")
        }
    }

    /** 把 ToolResult 转成 MCP tools/call 的返回体。 */
    private fun toolResultToJson(result: ToolResult): JsonObject = when (result) {
        is ToolResult.Success -> buildJsonObject {
            put("content", JsonArray(listOf(textContent(result.data.toString()))))
            put("isError", false)
        }
        is ToolResult.Partial -> buildJsonObject {
            put("content", JsonArray(listOf(textContent(result.data.toString() + "\n" + result.message))))
            put("isError", false)
        }
        is ToolResult.Error -> errorResult("[${result.code}] ${result.message}")
    }

    private fun textContent(text: String): JsonObject = buildJsonObject {
        put("type", "text")
        put("text", text)
    }

    private fun errorResult(message: String): JsonObject = buildJsonObject {
        put("content", JsonArray(listOf(textContent(message))))
        put("isError", true)
    }

    private fun result(id: JsonElement?, result: JsonObject): JsonObject = buildJsonObject {
        put("jsonrpc", JSONRPC_VERSION)
        if (id != null && id !is JsonNull) put("id", id)
        put("result", result)
    }

    private fun error(id: JsonElement?, code: Int, message: String): JsonObject = buildJsonObject {
        put("jsonrpc", JSONRPC_VERSION)
        if (id != null && id !is JsonNull) put("id", id)
        put("error", buildJsonObject {
            put("code", code)
            put("message", message)
        })
    }

    /** JsonPrimitive 取字符串的扩展（null 安全）。 */
    private val JsonPrimitive.contentOrNull: String?
        get() = if (this is JsonNull) null else content
}
