package route.exception

class InvalidAmountException(override val message: String) : RuntimeException(message)
class InvalidTransactionException(override val message: String) : RuntimeException(message)