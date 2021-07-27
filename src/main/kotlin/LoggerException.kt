import org.tinylog.Logger

class LoggerException(message: String) : Exception(message) {
    init {
        Logger.tag("EXCEPTION").error { message }
    }
}
