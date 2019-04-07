package route

import io.ktor.application.call
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.NotAcceptable
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.route
import model.Transfer
import model.TransferStatus
import model.TransferStatus.TransferFailed
import model.TransferStatus.TransferSucceeded
import service.TransferService


fun Route.transactions(transferService: TransferService) {

    route("/transfer") {
        post {
            val transfer: Transfer = call.receive()
            val status: TransferStatus = transferService.transfer(transfer)
            when (status) {
                is TransferFailed -> call.respond(NotAcceptable, status)
                is TransferSucceeded -> call.respond(OK)
            }
        }
    }
}
