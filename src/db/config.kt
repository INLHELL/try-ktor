package db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import model.Accounts
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun hikari(): HikariDataSource = HikariDataSource(
    HikariConfig().apply {
        driverClassName = "org.h2.Driver"
        jdbcUrl = "jdbc:h2:mem:test;DATABASE_TO_UPPER=false;"
        maximumPoolSize = 3
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    }
)

fun initialiseDatabase() {
    Database.connect(hikari())
    transaction {
        SchemaUtils.create(Accounts)
    }
}
