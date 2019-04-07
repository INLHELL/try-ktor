package model

import route.exception.InvalidAmountException
import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.LongIdTable
import route.exception.InvalidTransactionException
import java.math.BigDecimal
import java.math.BigDecimal.ZERO

data class Transfer(
    @JsonProperty(required = true) val from: Long,
    @JsonProperty(required = true) val to: Long,
    @JsonProperty(required = true) val amount: BigDecimal
) {
    init {
        if (amount <= ZERO) {
            throw InvalidAmountException("Amount of money for transferring is less then zero")
        }
        if (from == to) {
            throw InvalidTransactionException("Sender and receiver account ids (from: $from and to: $to) that is used for transferring money are equal")
        }
    }
}

data class Account(
    @JsonProperty(required = true) val id: Long? = 0,
    @JsonProperty(required = true) val amount: BigDecimal = ZERO
)

object Accounts : LongIdTable() {
    val amount = decimal("amount", 20, 5).default(ZERO)
}

sealed class TransferStatus {
    object TransferSucceeded : TransferStatus()
    class TransferFailed(val description: String) : TransferStatus()
}

sealed class RechargeStatus {
    object RechargeSucceeded : RechargeStatus()
    class RechargeFailed(val description: String) : RechargeStatus()
}