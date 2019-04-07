package service

import model.Account
import model.Accounts
import model.RechargeStatus
import model.RechargeStatus.*
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal

class AccountService {

    companion object {
        val log: Logger = LoggerFactory.getLogger(AccountService::class.java)
    }

    fun getAccountById(id: Long): Account? =
        transaction {
            Accounts
                .select { Accounts.id eq id }
                .mapNotNull { mapToAccount(it) }
                .singleOrNull()
        }

    fun rechargeAccount(id: Long, depositAmount: BigDecimal): RechargeStatus =
        transaction {
            try {
                Accounts.update({ Accounts.id eq id }) {
                    with(SqlExpressionBuilder) {
                        it.update(Accounts.amount, Accounts.amount + depositAmount)
                    }
                }
                log.info("Account with id: $id was recharged with amount: $depositAmount")
                return@transaction RechargeSucceeded
            } catch (e: Exception) {
                log.error("Unexpected error", e)
                return@transaction RechargeFailed("Unexpected error")
            }
        }

    fun createAccount(): Long =
        transaction {
            Accounts.insertAndGetId {}.value
        }.also {
            log.info("New account with id=$it was inserted")
        }
}

fun mapToAccount(row: ResultRow) =
    Account(
        id = row[Accounts.id].value,
        amount = row[Accounts.amount]
    )