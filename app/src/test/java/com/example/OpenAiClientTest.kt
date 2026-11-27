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

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/v1/chat/completions") { exchange ->
            lastAuthHeader = exchange.requestHeaders.getFirst("Authorization")
            lastRequestBody = exchange.requestBody.bufferedReader().use { it.readText() }
            val bytes = nextResponseBody.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(nextStatusCode, bytes.size.toLong())
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
}
