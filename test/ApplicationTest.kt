package com.example

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.Application
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpHeaders.ContentType
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpMethod.Companion.Put
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.Accepted
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.http.HttpStatusCode.Companion.NoContent
import io.ktor.http.HttpStatusCode.Companion.NotAcceptable
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import model.Account
import model.Transfer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    private val mapper = jacksonObjectMapper()

    @Test
    fun `create account and put some money on its deposit`() = withTestApplication(Application::module) {
        with(handleRequest(Post, "/accounts")) {
            assertEquals(Created, response.status())
            val id = response.content as String
            with(handleRequest(Get, "/accounts/$id")) {
                assertEquals(Accepted, response.status())
                val expectedJson = mapper.writeValueAsString(Account(id.toLong(), 0.toBigDecimal().setScale(5)))
                assertEquals(expectedJson, response.content as String)
            }
            val amount = 200.toBigDecimal()
            with(handleRequest(Put, "/accounts/$id/deposit/$amount")) {
                assertEquals(NoContent, response.status())
            }
            with(handleRequest(Get, "/accounts/$id")) {
                assertEquals(Accepted, response.status())
                val expectedJson = mapper.writeValueAsString(Account(id.toLong(), amount.setScale(5)))
                assertEquals(expectedJson, response.content as String)
            }
        }
    }


    @Test
    fun `get account that doesn't exist and put some money on it`() = withTestApplication(Application::module) {
        with(handleRequest(Get, "/accounts/${Long.MAX_VALUE}")) {
            assertEquals(NotFound, response.status())
            with(handleRequest(Put, "/accounts/${Long.MAX_VALUE}/deposit/200")) {
                assertEquals(NoContent, response.status())
            }
        }
    }

    @Test
    fun `account exists but amount of money for recharge less then zero`() = withTestApplication(Application::module) {
        with(handleRequest(Post, "/accounts")) {
            val id = response.content as String
            with(handleRequest(Put, "/accounts/$id/deposit/-100")) {
                assertEquals(NotAcceptable, response.status())
            }
        }
    }

    @Test
    fun `transfer money between two accounts`() {
        withTestApplication(Application::module) {
            val fromId = handleRequest(Post, "/accounts").response.content as String
            val toId = handleRequest(Post, "/accounts").response.content as String

            val fromAmount = 100
            with(handleRequest(Put, "/accounts/$fromId/deposit/$fromAmount")) {
                assertEquals(NoContent, response.status())
            }

            with(handleRequest(Post, "/transfer") {
                addHeader(ContentType, Json.toString())
                setBody(
                    mapper.writeValueAsString(
                        Transfer(
                            fromId.toLong(),
                            toId.toLong(),
                            fromAmount.toBigDecimal()
                        )
                    )
                )
            }) {
                assertEquals(OK, response.status())
            }

            with(handleRequest(Get, "/accounts/$fromId")) {
                assertEquals(Accepted, response.status())
                val expectedJson =
                    mapper.writeValueAsString(Account(fromId.toLong(), 0.toBigDecimal().setScale(5)))
                assertEquals(expectedJson, response.content as String)
            }
            with(handleRequest(Get, "/accounts/$toId")) {
                assertEquals(Accepted, response.status())
                val expectedJson = mapper.writeValueAsString(
                    Account(
                        toId.toLong(),
                        fromAmount.toBigDecimal().setScale(5)
                    )
                )
                assertEquals(expectedJson, response.content as String)
            }
        }
    }

    @Test
    fun `account transfer money to itself`() {
        withTestApplication(Application::module) {
            val id = handleRequest(Post, "/accounts").response.content as String
            val amount = 100

            with(handleRequest(Put, "/accounts/$id/deposit/$amount")) {
                assertEquals(NoContent, response.status())
            }

            with(handleRequest(Post, "/transfer") {
                addHeader(ContentType, Json.toString())
                setBody(
                    """
                        {
                            "from": $id,
                            "to": $id,
                            "amount": 100
                        }
                    """.trimIndent()
                )
            }) {
                assertEquals(HttpStatusCode.BadRequest, response.status())
            }

            with(handleRequest(Get, "/accounts/$id")) {
                assertEquals(Accepted, response.status())
                val expectedJson =
                    mapper.writeValueAsString(Account(id.toLong(), amount.toBigDecimal().setScale(5)))
                assertEquals(expectedJson, response.content as String)
            }
        }
    }

    @Test
    fun `one of account has not enough money to transfer`() {
        withTestApplication(Application::module) {
            val fromId = handleRequest(Post, "/accounts").response.content as String
            val toId = handleRequest(Post, "/accounts").response.content as String

            val fromAmount = 100
            with(handleRequest(Put, "/accounts/$fromId/deposit/$fromAmount")) {
                assertEquals(NoContent, response.status())
            }

            val amountToTransfer = fromAmount * 10
            with(handleRequest(Post, "/transfer") {
                addHeader(ContentType, Json.toString())
                setBody(
                    mapper.writeValueAsString(
                        Transfer(
                            fromId.toLong(),
                            toId.toLong(),
                            amountToTransfer.toBigDecimal()
                        )
                    )
                )
            }) {
                assertEquals(NotAcceptable, response.status())
            }

            with(handleRequest(Get, "/accounts/$fromId")) {
                assertEquals(Accepted, response.status())
                val expectedJson =
                    mapper.writeValueAsString(Account(fromId.toLong(), fromAmount.toBigDecimal().setScale(5)))
                assertEquals(expectedJson, response.content as String)
            }
            with(handleRequest(Get, "/accounts/$toId")) {
                assertEquals(Accepted, response.status())
                val expectedJson = mapper.writeValueAsString(
                    Account(
                        toId.toLong(),
                        0.toBigDecimal().setScale(5)
                    )
                )
                assertEquals(expectedJson, response.content as String)
            }
        }
    }

    @Test
    fun `one hundred accounts transfer one dollar to target`() {
        withTestApplication(Application::module) {
            val targetId = handleRequest(Post, "/accounts").response.content as String
            val numberOfSenders = 100

            val listOfSenderIds = (1..numberOfSenders).map {
                handleRequest(Post, "/accounts").response.content as String
            }

            listOfSenderIds.forEach { id ->
                with(handleRequest(Put, "/accounts/$id/deposit/1")) {
                    assertEquals(NoContent, response.status())
                }
            }

            val threadPool = Executors.newCachedThreadPool()
            val latch = CountDownLatch(numberOfSenders)
            listOfSenderIds.map { id ->
                threadPool.submit {
                    latch.countDown()
                    handleRequest(Post, "/transfer") {
                        addHeader(ContentType, Json.toString())
                        setBody(
                            mapper.writeValueAsString(
                                Transfer(
                                    id.toLong(),
                                    targetId.toLong(),
                                    1.toBigDecimal()
                                )
                            )
                        )
                    }
                }
            }.forEach { it.get() }

            with(handleRequest(Get, "/accounts/$targetId")) {
                assertEquals(Accepted, response.status())
                val expectedJson =
                    mapper.writeValueAsString(Account(targetId.toLong(), numberOfSenders.toBigDecimal().setScale(5)))
                assertEquals(expectedJson, response.content as String)
            }
            listOfSenderIds.forEach { senderId ->
                with(handleRequest(Get, "/accounts/$senderId")) {
                    assertEquals(Accepted, response.status())
                    val expectedJson =
                        mapper.writeValueAsString(Account(senderId.toLong(), 0.toBigDecimal().setScale(5)))
                    assertEquals(expectedJson, response.content as String)
                }
            }

        }
    }

    @Test
    fun `one account transfer one dollar to hundred target accounts`() {
        withTestApplication(Application::module) {
            val senderId = handleRequest(Post, "/accounts").response.content as String
            with(handleRequest(Put, "/accounts/$senderId/deposit/100")) {
                assertEquals(NoContent, response.status())
            }

            val numberOfReceivers = 100

            val listOfReceiverIds = (1..numberOfReceivers).map {
                handleRequest(Post, "/accounts").response.content as String
            }

            val threadPool = Executors.newCachedThreadPool()
            val latch = CountDownLatch(numberOfReceivers)
            listOfReceiverIds.map { receiverId ->
                threadPool.submit {
                    latch.countDown()
                    handleRequest(Post, "/transfer") {
                        addHeader(ContentType, Json.toString())
                        setBody(
                            mapper.writeValueAsString(
                                Transfer(
                                    senderId.toLong(),
                                    receiverId.toLong(),
                                    1.toBigDecimal()
                                )
                            )
                        )
                    }
                }
            }.forEach { it.get() }

            with(handleRequest(Get, "/accounts/$senderId")) {
                assertEquals(Accepted, response.status())
                val expectedJson =
                    mapper.writeValueAsString(Account(senderId.toLong(), 0.toBigDecimal().setScale(5)))
                assertEquals(expectedJson, response.content as String)
            }
            listOfReceiverIds.forEach { receiverId ->
                with(handleRequest(Get, "/accounts/$receiverId")) {
                    assertEquals(Accepted, response.status())
                    val expectedJson =
                        mapper.writeValueAsString(Account(receiverId.toLong(), 1.toBigDecimal().setScale(5)))
                    assertEquals(expectedJson, response.content as String)
                }
            }
        }
    }

    @Test
    fun `ping pong, first group send 3 dollars to second group, second group send 6 to first`() {
        withTestApplication(Application::module) {
            val numberOfSenders = 100
            val listOfSenderIds = (1..numberOfSenders).map {
                handleRequest(Post, "/accounts").response.content as String
            }


            val numberOfReceivers = 100
            val listOfReceiverIds = (1..numberOfReceivers).map {
                handleRequest(Post, "/accounts").response.content as String
            }

            listOfSenderIds.map { senderId ->
                with(handleRequest(Put, "/accounts/$senderId/deposit/10")) {
                    assertEquals(NoContent, response.status())
                }
            }

            listOfReceiverIds.map { receiverId ->
                with(handleRequest(Put, "/accounts/$receiverId/deposit/10")) {
                    assertEquals(NoContent, response.status())
                }
            }

            val threadPool = Executors.newCachedThreadPool()
            val latch = CountDownLatch(numberOfReceivers+numberOfSenders)
            listOfSenderIds.mapIndexed { index, senderId ->
                threadPool.submit {
                    latch.countDown()
                    handleRequest(Post, "/transfer") {
                        addHeader(ContentType, Json.toString())
                        setBody(
                            mapper.writeValueAsString(
                                Transfer(
                                    senderId.toLong(),
                                    listOfReceiverIds[index].toLong(),
                                    3.toBigDecimal()
                                )
                            )
                        )
                    }
                }
            }.forEach { it.get() }

            listOfReceiverIds.mapIndexed { index, receiverId ->
                threadPool.submit {
                    latch.countDown()
                    handleRequest(Post, "/transfer") {
                        addHeader(ContentType, Json.toString())
                        setBody(
                            mapper.writeValueAsString(
                                Transfer(
                                    receiverId.toLong(),
                                    listOfSenderIds[index].toLong(),
                                    6.toBigDecimal()
                                )
                            )
                        )
                    }
                }
            }.forEach { it.get() }

            listOfSenderIds.forEach { senderId ->
                with(handleRequest(Get, "/accounts/$senderId")) {
                    assertEquals(Accepted, response.status())
                    val expectedJson =
                        mapper.writeValueAsString(Account(senderId.toLong(), 13.toBigDecimal().setScale(5)))
                    assertEquals(expectedJson, response.content as String)
                }
            }
            listOfReceiverIds.forEach { receiverId ->
                with(handleRequest(Get, "/accounts/$receiverId")) {
                    assertEquals(Accepted, response.status())
                    val expectedJson =
                        mapper.writeValueAsString(Account(receiverId.toLong(), 7.toBigDecimal().setScale(5)))
                    assertEquals(expectedJson, response.content as String)
                }
            }
        }
    }
}