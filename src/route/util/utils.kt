package route.util

import io.ktor.application.ApplicationCall
import java.math.BigDecimal

@Suppress("UNCHECKED_CAST")
inline fun <reified T> ApplicationCall.getParam(paramName: String): T {
    return when(T::class) {
        Long::class -> this.parameters[paramName]!!.toLong() as T
        BigDecimal::class -> this.parameters[paramName]!!.toBigDecimal() as T
        else -> this.parameters[paramName]!! as T
    }
}