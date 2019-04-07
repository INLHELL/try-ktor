package route

import io.ktor.application.call
import io.ktor.http.HttpStatusCode.Companion.Accepted
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.http.HttpStatusCode.Companion.NoContent
import io.ktor.http.HttpStatusCode.Companion.NotAcceptable
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.put
import io.ktor.routing.route
import model.RechargeStatus.RechargeFailed
import model.RechargeStatus.RechargeSucceeded
import route.util.getParam
import service.AccountService
import java.math.BigDecimal
import java.math.BigDecimal.ZERO

fun Route.account(accountService: AccountService) {
    route("/accounts") {
        get("/{id}") {
            val id: Long = call.getParam("id")
            accountService
                .getAccountById(id)
                ?.let { call.respond(Accepted, it) }
                ?: call.respond(NotFound)
        }
        put("/{id}/deposit/{amount}") {
            val id: Long = call.getParam("id")
            val depositAmount: BigDecimal = call.getParam("amount")

            if (depositAmount <= ZERO) {
                call.respond(NotAcceptable)
                return@put
            }

            val status = accountService.rechargeAccount(id, depositAmount)
            when (status) {
                is RechargeFailed -> call.respond(NotAcceptable, status)
                is RechargeSucceeded -> call.respond(NoContent)
            }

        }
        post {
            val id = accountService.createAccount()
            call.respond(Created, id)
        }
    }
}


