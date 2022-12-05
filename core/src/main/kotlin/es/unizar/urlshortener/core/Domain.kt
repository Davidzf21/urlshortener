package es.unizar.urlshortener.core

import java.time.OffsetDateTime

/**
 * Diferentes valores ara el campo de [Validacion].
 */
enum class ValidateUrlResponse {
    OK,
    NO_REACHABLE,
    UNSAFE,
    BLOCK
}

enum class ValidateUrlState {
    VALIDATION_ACEPT,
    VALIDATION_IN_PROGRESS,
    VALIDATION_FAIL_NOT_REACHABLE,
    VALIDATION_FAIL_NOT_SAFE,
    VALIDATION_FAIL_BLOCK,
    VALIDATION_NOT_DONE
}

/**
 * Clase para representar los valores de [InfoClientUseCase].
 */
class InfoClientResponse(date: String, browser: String?, platform: String?) {

    var date: String
    var browser: String?
    var platform: String?

    init {
        this.date = date
        this.browser = browser
        this.platform = platform
    }
}

/**
 * A [Click] captures a request of redirection of a [ShortUrl] identified by its [hash].
 */
data class Click(
    val hash: String,
    val properties: ClickProperties = ClickProperties(),
    val created: OffsetDateTime = OffsetDateTime.now()
)

/**
 * A [ShortUrl] is the mapping between a remote url identified by [redirection] and a local short url identified by [hash].
 */
data class ShortUrl(
        val hash: String,
        val redirection: Redirection,
        val created: OffsetDateTime = OffsetDateTime.now(),
        val properties: ShortUrlProperties = ShortUrlProperties(),
        var validation: ValidateUrlState = ValidateUrlState.VALIDATION_NOT_DONE
)

/**
 * A [Redirection] specifies the [target] and the [status code][mode] of a redirection.
 * By default, the [status code][mode] is 307 TEMPORARY REDIRECT.
 */
data class Redirection(
        val target: String,
        var mode: Int = 307
)

/**
 * A [ShortUrlProperties] is the bag of properties that a [ShortUrl] may have.
 */
data class ShortUrlProperties(
        val ip: String? = null,
        val sponsor: String? = null,
        var safe: Boolean = true,
        val owner: String? = null,
        val country: String? = null
)

/**
 * A [ClickProperties] is the bag of properties that a [Click] may have.
 */
data class ClickProperties(
    val ip: String? = null,
    val referrer: String? = null,
    val browser: String? = null,
    val platform: String? = null,
    val country: String? = null
)

