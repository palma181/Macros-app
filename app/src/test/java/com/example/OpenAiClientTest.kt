package com.example

import com.example.data.model.AiConfig
import com.example.data.model.AiPreset
import com.example.data.remote.OpenAiClient
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OpenAiClientTest {

    private lateinit var server: HttpServer
    private lateinit var baseUrl: String
    private var lastRequestBody: String = ""
    private var lastAuthHeader: String? = null
    private var nextStatusCode: Int = 200
    private var nextResponseBody: String = ""
    private var nextModelsBody: String = """{"data":[{"id":"whisper-large-v3"},{"id":"gemini-3.5-flash-lite"},{"id":"text-embedding-004"}]}"""
    private var customChatHandler: ((String) -> Pair<Int, String>)? = null

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/v1/chat/completions") { exchange ->
            lastAuthHeader = exchange.requestHeaders.getFirst("Authorization")
            lastRequestBody = exchange.requestBody.bufferedReader().use { it.readText() }
            val (code, body) = customChatHandler?.invoke(lastRequestBody) ?: (nextStatusCode to nextResponseBody)
            val bytes = body.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(code, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.createContext("/v1/models") { exchange ->
            val bytes = nextModelsBody.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        baseUrl = "http://127.0.0.1:${server.address.port}/v1"
    }

    @After
    fun tearDown() {
        server.stop(0)
    }

    @Test
    fun `estimateFromText parses markdown wrapped JSON and sends Bearer token`() = runTest {
        nextStatusCode = 200
        nextResponseBody = JSONObject().apply {
            put(
                "choices",
                org.json.JSONArray().put(
                    JSONObject().put(
                        "message",
                        JSONObject().put(
                            "content",
                            "```json\n{\"kcal\":480,\"p\":32,\"c\":42,\"f\":18}\n```"
                        )
                    )
                )
            )
        }.toString()

        val client = OpenAiClient()
        val config = AiConfig(
            preset = AiPreset.OPENAI,
            baseUrl = baseUrl,
            apiKey = "sk-test-123",
            model = "gpt-4o-mini"
        )

        val result = client.estimateFromText(config, "2 huevos con tortilla y frijoles")
        assertTrue(result.isSuccess)
        val est = result.getOrThrow()
        assertEquals(480, est.kcal)
        assertEquals(32, est.protein)
        assertEquals(42, est.carbs)
        assertEquals(18, est.fat)
        assertEquals("Bearer sk-test-123", lastAuthHeader)
        assertTrue(lastRequestBody.contains("gpt-4o-mini"))
    }

    @Test
    fun `estimateFromImage sends multimodal image_url payload and extracts name and macros`() = runTest {
        nextStatusCode = 200
        nextResponseBody = JSONObject().apply {
            put(
                "choices",
                org.json.JSONArray().put(
                    JSONObject().put(
                        "message",
                        JSONObject().put(
                            "content",
                            "{\"name\":\"Pollo a la plancha con arroz\",\"kcal\":560,\"p\":45,\"c\":55,\"f\":14}"
                        )
                    )
                )
            )
        }.toString()

        val client = OpenAiClient()
        val config = AiConfig(
            preset = AiPreset.LOCAL,
            baseUrl = baseUrl,
            apiKey = "",
            model = "llava"
        )

        val result = client.estimateFromImage(config, "data:image/jpeg;base64,AAAA", null)
        assertTrue(result.isSuccess)
        val est = result.getOrThrow()
        assertEquals("Pollo a la plancha con arroz", est.name)
        assertEquals(560, est.kcal)
        assertEquals(45, est.protein)
        assertEquals(55, est.carbs)
        assertEquals(14, est.fat)
        val sentJson = JSONObject(lastRequestBody)
        val contentArr = sentJson.getJSONArray("messages")
            .getJSONObject(0)
            .getJSONArray("content")
        assertEquals(2, contentArr.length())
        assertEquals("image_url", contentArr.getJSONObject(1).getString("type"))
        assertEquals(
            "data:image/jpeg;base64,AAAA",
            contentArr.getJSONObject(1).getJSONObject("image_url").getString("url")
        )
    }

    @Test
    fun `missing api key on remote url returns helpful error`() = runTest {
        val client = OpenAiClient()
        val config = AiConfig(
            preset = AiPreset.OPENAI,
            baseUrl = "https://api.openai.com/v1",
            apiKey = "",
            model = "gpt-4o-mini"
        )
        val result = client.estimateFromText(config, "Manzana")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("clave de API") == true)
    }

    @Test
    fun `http 401 returns clear unauthorized message`() = runTest {
        nextStatusCode = 401
        nextResponseBody = """{"error":{"message":"Incorrect API key provided"}}"""

        val client = OpenAiClient()
        val config = AiConfig(
            preset = AiPreset.OPENAI,
            baseUrl = baseUrl,
            apiKey = "bad-key",
            model = "gpt-4o-mini"
        )
        val result = client.testConnection(config)
        assertTrue(result.isFailure)
        val msg = result.exceptionOrNull()?.message.orEmpty()
        assertTrue(msg.contains("401"))
        assertTrue(msg.contains("Incorrect API key"))
    }

    @Test
    fun `generateRecipesFromIngredients parses basic intermediate and advanced recipes`() = runTest {
        nextStatusCode = 200
        nextResponseBody = JSONObject().apply {
            put(
                "choices",
                org.json.JSONArray().put(
                    JSONObject().put(
                        "message",
                        JSONObject().put(
                            "content",
                            """
                            {
                              "recipes": [
                                {
                                  "title": "Fettuccini con pollo rápido",
                                  "difficulty": "basico",
                                  "prepMinutes": 18,
                                  "ingredients": ["90g fettuccini", "150g pollo"],
                                  "steps": ["Cuece la pasta", "Asa el pollo y mezcla"],
                                  "tips": "Guarda agua de cocción",
                                  "kcal": 510,
                                  "p": 42,
                                  "c": 58,
                                  "f": 10
                                }
                              ]
                            }
                            """.trimIndent()
                        )
                    )
                )
            )
        }.toString()

        val client = OpenAiClient()
        val config = AiConfig(
            preset = AiPreset.OPENAI,
            baseUrl = baseUrl,
            apiKey = "sk-test",
            model = "gpt-4o-mini"
        )
        val result = client.generateRecipesFromIngredients(
            config = config,
            ingredients = listOf("Pollo", "Fettuccini"),
            difficulty = null
        )
        assertTrue(result.isSuccess)
        val recipes = result.getOrThrow()
        assertEquals(1, recipes.size)
        assertEquals("Fettuccini con pollo rápido", recipes.first().title)
        assertEquals(com.example.data.model.RecipeDifficulty.BASIC, recipes.first().difficulty)
        assertEquals(2, recipes.first().steps.size)
        assertEquals(510, recipes.first().kcal)
    }

    @Test
    fun `generateRecipesFromIngredients for SNACKS_DRINKS prompts for smoothies and supplements`() = runTest {
        nextStatusCode = 200
        nextResponseBody = JSONObject().apply {
            put(
                "choices",
                org.json.JSONArray().put(
                    JSONObject().put(
                        "message",
                        JSONObject().put(
                            "content",
                            """
                            {
                              "recipes": [
                                {
                                  "title": "Batido post-entreno de fresa, whey y creatina",
                                  "difficulty": "basico",
                                  "prepMinutes": 5,
                                  "ingredients": ["100g fresa", "1 scoop whey", "5g creatina"],
                                  "steps": ["Licúa todo con hielo"],
                                  "tips": "Tómalo frío",
                                  "kcal": 240,
                                  "p": 27,
                                  "c": 22,
                                  "f": 3
                                }
                              ]
                            }
                            """.trimIndent()
                        )
                    )
                )
            )
        }.toString()

        val client = OpenAiClient()
        val config = AiConfig(
            preset = AiPreset.OPENAI,
            baseUrl = baseUrl,
            apiKey = "sk-test",
            model = "gpt-4o-mini"
        )
        val result = client.generateRecipesFromIngredients(
            config = config,
            ingredients = listOf("Fresa", "Proteína Whey", "Creatina"),
            difficulty = com.example.data.model.RecipeDifficulty.BASIC,
            section = com.example.data.model.RecipeSection.SNACKS_DRINKS
        )
        assertTrue(result.isSuccess)
        val recipe = result.getOrThrow().first()
        assertEquals(com.example.data.model.RecipeSection.SNACKS_DRINKS, recipe.section)
        assertTrue(lastRequestBody.contains("suplementos"))
    }

    @Test
    fun `all free AiPreset providers define valid baseUrls models and signup links`() {
        val freePresets = AiPreset.entries.filter { it.isFreeTier }
        assertTrue(freePresets.size >= 9)
        freePresets.forEach { preset ->
            assertTrue("Missing URL for ${preset.id}", preset.defaultUrl.startsWith("http"))
            assertTrue("Missing model for ${preset.id}", preset.defaultModel.isNotBlank())
            if (preset != AiPreset.LOCAL) {
                assertTrue("Missing signupUrl for ${preset.id}", preset.signupUrl.startsWith("https://"))
            }
        }
    }

    @Test
    fun `fetchAvailableModels filters out whisper and embeddings and auto-recovers 404 model not found`() = runTest {
        nextModelsBody = """{"data":[{"id":"whisper-large-v3"},{"id":"gemini-3.5-flash-lite"},{"id":"text-embedding-004"}]}"""
        customChatHandler = { body ->
            val req = JSONObject(body)
            if (req.getString("model") == "gemini-2.0-flash") {
                404 to """{"error":{"message":"Model not found: gemini-2.0-flash"}}"""
            } else {
                200 to """{"choices":[{"message":{"content":"OK"}}]}"""
            }
        }

        val client = OpenAiClient()
        val config = AiConfig(
            preset = AiPreset.GEMINI,
            baseUrl = baseUrl,
            apiKey = "test-key",
            model = "gemini-2.0-flash"
        )

        val modelsRes = client.fetchAvailableModels(config)
        assertTrue(modelsRes.isSuccess)
        assertEquals(listOf("gemini-3.5-flash-lite"), modelsRes.getOrThrow())

        var resolved = ""
        val connRes = client.testConnection(config) { resolved = it }
        assertTrue(connRes.isSuccess)
        assertEquals("gemini-3.5-flash-lite", resolved)
        assertTrue(connRes.getOrThrow().contains("gemini-3.5-flash-lite"))
    }

    @Test
    fun `plain text OK HTTP 200 returns friendly error instead of JSONException and supports SSE chunks`() = runTest {
        nextStatusCode = 200
        nextResponseBody = "OK"

        val client = OpenAiClient()
        val config = AiConfig(
            preset = AiPreset.OPENAI,
            baseUrl = baseUrl,
            apiKey = "test-key",
            model = "gpt-4o-mini"
        )

        val plainRes = client.askCoach(config, "hola")
        assertTrue(plainRes.isFailure)
        val errMsg = plainRes.exceptionOrNull()?.message.orEmpty()
        assertTrue("Unexpected error: $errMsg", errMsg.contains("JSON compatible"))
        assertTrue(!errMsg.contains("cannot be converted to JSONObject"))

        nextResponseBody = """
            data: {"choices":[{"delta":{"content":"Hola "}}]}
            data: {"choices":[{"delta":{"content":"mundo"}}]}
            data: [DONE]
        """.trimIndent()
        val sseRes = client.askCoach(config, "hola")
        assertTrue(sseRes.isSuccess)
        assertEquals("Hola mundo", sseRes.getOrThrow())
    }
}
