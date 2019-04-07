package com.example

//import com.google.gson.*

//import com.google.gson.stream.MalformedJsonException
//import io.ktor.gson.GsonConverter
//import io.ktor.gson.gson
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import db.initialiseDatabase
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.features.StatusPages
import io.ktor.features.UnsupportedMediaTypeException
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.UnsupportedMediaType
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.server.netty.EngineMain.main
import org.slf4j.event.Level
import route.account
import route.exception.InvalidTransactionException
import route.transactions
import service.AccountService
import service.TransferService


fun main(args: Array<String>) = main(args)

fun Application.module() {

    initialiseDatabase()

    install(CallLogging) {
        level = Level.INFO
    }

    install(DefaultHeaders)

    install(ContentNegotiation) {
        jackson {}
    }

    install(StatusPages) {
        exception<UnsupportedMediaTypeException> { e ->
            log.error(e.message)
            call.respond(UnsupportedMediaType)
        }
        exception<MismatchedInputException> { e ->
            log.error(e.message)
            call.respond(BadRequest)
        }
        exception<NumberFormatException> { e ->
            log.error(e.message)
            call.respond(BadRequest)
        }
        exception<InvalidDefinitionException> { e ->
            log.error(e.message)
            call.respond(BadRequest)
        }
        exception<InvalidTransactionException> { e ->
            log.error(e.message)
            call.respond(BadRequest)
        }
    }

    install(Routing) {
        val accountService = AccountService()
        val transactionService = TransferService()

        account(accountService)
        transactions(transactionService)
    }
}




