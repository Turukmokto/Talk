package org.csc.kotlin2021.registry

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.*
import io.ktor.config.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.csc.kotlin2021.UserAddress
import org.csc.kotlin2021.UserInfo
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.Ignore
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail

fun Application.testModule() {
    (environment.config as MapApplicationConfig).apply {
        // define test environment here
    }
    module(testing = true)
}

class ApplicationTest {
    private val objectMapper = jacksonObjectMapper()
    private val testUserName = "pupkin"
    private val testHttpAddress = UserAddress("127.0.0.1", 9999)
    private val newTestHttpAddress = UserAddress("127.0.0.2", 9998)
    private val userData = UserInfo(testUserName, testHttpAddress)

    @BeforeEach
    fun clearRegistry() {
        Registry.users.clear()
    }

    @Test
    fun `health endpoint`() {
        withTestApplication({ testModule() }) {
            handleRequest(HttpMethod.Get, "/v1/health").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("OK", response.content)
            }
        }
    }

    @Test
    fun `register user`() = withRegisteredTestUser {
        assertTrue(Registry.users.containsKey(testUserName))
        assertEquals(testHttpAddress, Registry.users[testUserName])
    }


    @Test
    fun `list users`() = withRegisteredTestUser {
        handleRequest {
            method = HttpMethod.Get
            uri = "/v1/users"
        }.apply {
            assertEquals(HttpStatusCode.OK, response.status())
            val content = response.content ?: fail("No response content")
            val info = objectMapper.readValue<HashMap<String, UserAddress>>(content)

            assertEquals(Registry.users.toMap(), info)
        }
    }

    @Test
    fun `delete incorrect user name`(): Unit = withTestApplication({ testModule() }) {
        handleRequest {
            method = HttpMethod.Delete
            uri = "/v1/users/$testUserName"
            setData()
        }.apply {
            standartAsserts()
            assertFalse(Registry.users.containsKey(testUserName))
        }
    }


    @Test
    fun `update existing user`() = withRegisteredTestUser {
        handleRequest {
            method = HttpMethod.Put
            uri = "/v1/users/$testUserName"
            setUpdateData()
        }.apply {
            standartAsserts()
            assertEquals(newTestHttpAddress, Registry.users[testUserName])
        }
    }

    @Test
    fun `update non existing user`(): Unit = withTestApplication({ testModule() }) {
        handleRequest {
            method = HttpMethod.Put
            uri = "/v1/users/$testUserName"
            setUpdateData()
        }.apply {
            standartAsserts()
            assertEquals(newTestHttpAddress, Registry.users[testUserName])
        }
    }

    @Test
    fun `delete existing user`() = withRegisteredTestUser {
        handleRequest {
            method = HttpMethod.Delete
            uri = "/v1/users/$testUserName"
        }.apply {
            standartAsserts()
            assertFalse(Registry.users.containsKey(testUserName))
        }
    }

    @Test
    fun `delete non existing user`(): Unit = withTestApplication({ testModule() }) {
        handleRequest {
            method = HttpMethod.Delete
            uri = "/v1/users/$testUserName"
        }.apply {
            standartAsserts()
        }
    }

    private fun withRegisteredTestUser(block: TestApplicationEngine.() -> Unit) {
        withTestApplication({ testModule() }) {
            handleRequest {
                method = HttpMethod.Post
                uri = "/v1/users"
                setData()
            }.apply {
                standartAsserts()

                this@withTestApplication.block()
            }
        }
    }

    private fun TestApplicationCall.standartAsserts() {
        assertEquals(HttpStatusCode.OK, response.status())
        val content = response.content ?: fail("No response content")
        val info = objectMapper.readValue<HashMap<String, String>>(content)

        assertNotNull(info["status"])
        assertEquals("ok", info["status"])
    }

    private fun TestApplicationRequest.setData() {
        addHeader("Content-type", "application/json")
        setBody(objectMapper.writeValueAsString(userData))
    }

    private fun TestApplicationRequest.setUpdateData() {
        addHeader("Content-type", "application/json")
        setBody(objectMapper.writeValueAsString(newTestHttpAddress))
    }

}
