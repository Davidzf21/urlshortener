package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import es.unizar.urlshortener.core.usecases.InfoClientUserCase
import es.unizar.urlshortener.core.usecases.LogClickUseCase
import es.unizar.urlshortener.core.usecases.RedirectUseCase
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
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
import ru.chermenin.ua.UserAgent
import java.net.URI
import java.util.*
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.models.examples.Example
import javax.servlet.http.HttpServletRequest

/**
 * The specification of the controller.
 */
@OpenAPIDefinition(info = Info(title = "Shorturl API - Grupo A", version = "1.0",
        description = "Endpoints de UrlShortener creado para la asignatura de IW por el grupo A"))
interface UrlShortenerController {

    /**
     * Redirects and logs a short url identified by its [id].
     *
     * **Note**: Delivery of use cases [RedirectUseCase] and [LogClickUseCase].
     */
    @Operation(summary = "Redireccionar una URL identificada por {id}",
            description = "Redireccionar y registra una URL corta identificada por su {id}.",
            parameters = [Parameter(name = "id", description = "Hash que identifica una URL",
                    required = true, example = "e7f83ee8", schema = Schema(type = "string"))])
    fun redirectTo(id: String, request: HttpServletRequest): ResponseEntity<Void>

    /**
     * Creates a short url from details provided in [data].
     *
     * **Note**: Delivery of use case [CreateShortUrlUseCase].
     */
    @Operation(summary = "Crea una url acortada",
            description = "Crea y almacena en la BBDD, si es posible, una url acortada " +
                    "con la información aportada en [data].")
    fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut>

    /**
     * Devuelve información relevante sobre la URI acortada identificada por el parámetro {id}
     *
     * **Note**: Delivery of use case [InfoClientUserCase].
     */
    @Operation(summary = "Devuelve info. de la URL identificada por {id}",
            description = "Devuelve información relevante sobre la URI acortada identificada por el parámetro {id}.",
            parameters = [Parameter(name = "id", description = "Hash que identifica una URL",
                    required = true, example = "e7f83ee8", schema = Schema(type = "string"))])
    fun infoner(id: String, request: HttpServletRequest): ArrayList<InfoClientResponse>?
}

/**
 * Data required to create a short url.
 */
@Schema(name = "ShortUrlDataIn",
        description = "Entidad de información de URL corta de entrada")
data class ShortUrlDataIn(
    val url: String,
    val sponsor: String? = null
)

/**
 * Data returned after the creation of a short url.
 */
@Schema(name = "ShortUrlDataOut",
        description = "Entidad de información de URL corta de salida")
data class ShortUrlDataOut(
        val url: URI? = null,
        var properties: Map<String, Any> = emptyMap()
)


/**
 * The implementation of the controller.
 *
 * **Note**: Spring Boot is able to discover this [RestController] without further configuration.
 */
@Tag(name = "URL Shortener Endpoints", description = "Operations related to urlshortener")
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
                async { logClickUseCase.setBrowser(id, UserAgent.Companion.parse(userAgent)) }
                async { logClickUseCase.setPlataform(id, UserAgent.Companion.parse(userAgent)) }
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
            var errors = ""
            var state = HttpStatus.CREATED
            if(!it.properties.safe) {
                errors = "URL de destino no es segura (Google Safe Browsing)"
                state = HttpStatus.BAD_REQUEST
            }
            when (it.blockInfo) {
                BlockUrlState.FAIL_BLOCK_URL -> {
                    errors = "URL de destino está bloqueada"
                    state = HttpStatus.FORBIDDEN
                }
                BlockUrlState.FAIL_BLOCK_IP -> {
                    errors = "IP del creador está bloqueada"
                    state = HttpStatus.FORBIDDEN
                }
                else -> {}
            }
            when (it.reachableInfo) {
                ReachableUrlState.FAIL_NOT_REACHABLE -> {
                    errors = "URL de destino no es alcanzable"
                    state = HttpStatus.BAD_REQUEST
                }
                else -> {}
            }
            val response = ShortUrlDataOut(
                url = url,
                properties = mapOf(
                    "safe" to it.properties.safe,
                    "error" to errors
                )
            )
            println(it)
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
