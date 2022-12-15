package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher


/**
 * Given an url returns the key that is used to create a short URL.
 * When the url is created optional data may be added.
 *
 * **Note**: This is an example of functionality.
 */
interface CreateShortUrlUseCase {
    fun create(url: String, data: ShortUrlProperties): ShortUrl
}

/**
 * Implementation of [CreateShortUrlUseCase].
 */
class CreateShortUrlUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService,
    private val validatorService: ValidatorService,
    private val hashService: HashService,
) : CreateShortUrlUseCase {

    @Autowired
    lateinit var validateUrlUseCase: ValidateUrlUseCase

    @Autowired
    lateinit var applicationEventPublisher: ApplicationEventPublisher

    override fun create(url: String, data: ShortUrlProperties): ShortUrl =
            if (validatorService.isValid(url)) {
                runBlocking {
                    val id: String = hashService.hasUrl(url)
                    var su = ShortUrl(
                            hash = id,
                            redirection = Redirection(target = url),
                            properties = ShortUrlProperties(
                                    safe = data.safe,
                                    ip = data.ip,
                                    sponsor = data.sponsor
                            ),
                            validation = ValidateUrlState.VALIDATION_IN_PROGRESS
                    )
                    shortUrlRepository.save(su)

                    /*** Enviar mensaje en la cola ***/
                    applicationEventPublisher.publishEvent(GoogleEvent(this, su.hash, url))

                    /*** Validaciones de la URL con Corutinas ***/
                    val validateResponse = async { validateUrlUseCase.ValidateURL(su.hash, url, data.ip!!) }

                    /*** Comprobamos la validacion de la URL ***/
                    validateResponse.await()
                    println(shortUrlRepository.findByKey(su.hash))
                    shortUrlRepository.findByKey(su.hash)!!
                }
            } else {
                throw InvalidUrlException(url)
            }
}
