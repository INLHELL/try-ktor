package service

import model.Accounts
import model.Transfer
import model.TransferStatus
import model.TransferStatus.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TransferService {
    companion object {
        val log: Logger = LoggerFactory.getLogger(TransferService::class.java)
    }

    fun transfer(transfer: Transfer): TransferStatus =
        transaction {
            try {
                val fromAccount = Accounts
                    .select { (Accounts.id eq transfer.from) and (Accounts.amount greaterEq transfer.amount) }
                    .mapNotNull { mapToAccount(it) }
                    .singleOrNull()

                if (fromAccount == null) {
                    val description =
                        "Charged account with id: ${transfer.from} not found " +
                                "or its balance lower then transferred amount: ${transfer.amount}"
                    log.info(description)
                    return@transaction TransferFailed(description)
                }
                log.debug("Charged account with id: ${fromAccount.id} and amount: ${fromAccount.amount}")

                val toAccount = fromAccount.let {
                    Accounts
                        .select { Accounts.id eq transfer.to }
                        .mapNotNull { mapToAccount(it) }
                        .singleOrNull()
                }

                if (toAccount == null) {
                    val description =
                        "Receiver account with id: ${transfer.to} not found"
                    log.info(description)
                    return@transaction TransferFailed(description)
                }
                log.debug("Recharged account with id: ${toAccount.id} and balance: ${toAccount.amount} with amount: ${transfer.amount}")

                Accounts.update({ Accounts.id eq transfer.from }) {
                    with(SqlExpressionBuilder) {
                        it.update(amount, amount - transfer.amount)
                    }
                }
                Accounts.update({ Accounts.id eq transfer.to }) {
                    with(SqlExpressionBuilder) {
                        it.update(amount, amount + transfer.amount)
                    }
                }
                return@transaction TransferSucceeded
            } catch (e: Exception) {
                log.error("Unexpected error", e)
                return@transaction TransferFailed("Unexpected error")
            }
        }
}