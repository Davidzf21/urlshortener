package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import es.unizar.urlshortener.core.usecases.InfoClientUserCase
import es.unizar.urlshortener.core.usecases.LogClickUseCase
import es.unizar.urlshortener.core.usecases.RedirectUseCase
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.HttpHeaders
import kotlinx.coroutines.*
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestTemplate
import org.springframework.web.servlet.function.ServerResponse.async
import ru.chermenin.ua.UserAgent
import java.net.URI
import java.util.*
import javax.servlet.http.HttpServletRequest


/**
 * The specification of the controller.
 */
interface UrlShortenerController {

    /**
     * Redirects and logs a short url identified by its [id].
     *
     * **Note**: Delivery of use cases [RedirectUseCase] and [LogClickUseCase].
     */
    fun redirectTo(id: String, request: HttpServletRequest): ResponseEntity<Void>

    /**
     * Creates a short url from details provided in [data].
     *
     * **Note**: Delivery of use case [CreateShortUrlUseCase].
     */
    fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut>

    /**
     * Devuelve información relevante sobre la URI acortada identificada por el parámetro id..
     *
     * **Note**:
     */
    fun infoner(id: String, request: HttpServletRequest): ArrayList<InfoClientResponse>?
}

/**
 * Data required to create a short url.
 */
data class ShortUrlDataIn(
    val url: String,
    val sponsor: String? = null
)

/**
 * Data returned after the creation of a short url.
 */
data class ShortUrlDataOut(
        val url: URI? = null,
        var properties: Map<String, Any> = emptyMap()
)


/**
 * The implementation of the controller.
 *
 * **Note**: Spring Boot is able to discover this [RestController] without further configuration.
 */
@RestController
class UrlShortenerControllerImpl(
    val redirectUseCase: RedirectUseCase,
    val logClickUseCase: LogClickUseCase,
    val createShortUrlUseCase: CreateShortUrlUseCase,
    val infoClientUserCase: InfoClientUserCase
) : UrlShortenerController {

    @GetMapping("/{id:(?!api|index).*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<Void> =
        redirectUseCase.redirectTo(id).let {
            if (it.mode == 403) throw RedirectionNotSafeOrBlock(id)
            if (it.mode == 400) throw RedirectionNotReachable(id)
            val userAgent = request.getHeader("User-Agent").toString()
            logClickUseCase.logClick(id, ClickProperties(ip = request.remoteAddr))
            CoroutineScope(Dispatchers.IO).launch() {
                println(userAgent)
                println(UserAgent.Companion.parse(userAgent).toString())
                async { logClickUseCase.setPropieties(id, UserAgent.Companion.parse(userAgent)) }
            }
            val h = HttpHeaders()
            h.location = URI.create(it.target)
            ResponseEntity<Void>(h, HttpStatus.valueOf(it.mode))
        }

    @PostMapping("/api/link", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    override fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut> =
        createShortUrlUseCase.create(
            url = data.url,
            data = ShortUrlProperties(
                ip = request.remoteAddr,
                sponsor = data.sponsor
            )
        ).let {
            val h = HttpHeaders()
            val url = linkTo<UrlShortenerControllerImpl> { redirectTo(it.hash, request) }.toUri()
            h.location = url
            var errores = ""
            var state = HttpStatus.CREATED
            when (it.validation) {
                ValidateUrlState.VALIDATION_FAIL_NOT_SAFE -> {
                    errores = "URI de destino no es segura"
                    state = HttpStatus.BAD_REQUEST
                }
                ValidateUrlState.VALIDATION_FAIL_NOT_REACHABLE -> {
                    errores = "URI de destino no es alcanzable"
                    state = HttpStatus.BAD_REQUEST
                }
                ValidateUrlState.VALIDATION_FAIL_BLOCK_URL -> {
                    errores = "URI de destino esta bloqueada"
                    state = HttpStatus.FORBIDDEN
                }
                ValidateUrlState.VALIDATION_FAIL_BLOCK_IP -> {
                    errores = "IP del creador esta bloqueada"
                    state = HttpStatus.FORBIDDEN
                }
                else -> {}
            }
            val response = ShortUrlDataOut(
                url = url,
                properties = mapOf(
                    "safe" to it.properties.safe,
                    "error" to errores
                )
            )
            ResponseEntity<ShortUrlDataOut>(response, h, state)
        }

    @GetMapping("/api/link/{id}")
    override fun infoner(@PathVariable id: String, request: HttpServletRequest): ArrayList<InfoClientResponse>? {
        return infoClientUserCase.getInfo(id)
    }

}

@Configuration
class RestTemplateConfig {

    /**
     * Build a RestTemplate Bean with the default configuration
     */
    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplateBuilder().build()
    }
}
