package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.runBlocking
import org.apache.el.parser.AstFalse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.servlet.function.ServerResponse.async

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

    override fun create(url: String, data: ShortUrlProperties): ShortUrl =
            if (validatorService.isValid(url)) {
                runBlocking {
                    val validateResponse = async { validateUrlUseCase.ValidateURL(url, data.ip!!) }
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

                    /*** Comprobamos la validacion de la URL ***/
                    when (validateResponse.await()) {
                        ValidateUrlResponse.OK -> {
                            println("OK")
                            if (shortUrlRepository.updateValidate(su.hash, ValidateUrlState.VALIDATION_ACEPT)) {
                                su = shortUrlRepository.findByKey(su.hash)!!
                            } else {
                                throw UpdateValidationNoWork(url)
                            }
                        }
                        ValidateUrlResponse.NO_REACHABLE -> {
                            println("NO_REACHABLE")
                            su.validation = ValidateUrlState.VALIDATION_FAIL_NOT_REACHABLE
                            su.redirection.mode = 400
                            shortUrlRepository.save(su)
                        }
                        ValidateUrlResponse.UNSAFE -> {
                            println("UNSAFE")
                            su.validation = ValidateUrlState.VALIDATION_FAIL_NOT_SAFE
                            su.properties.safe = false
                            su.redirection.mode = 403
                            shortUrlRepository.save(su)
                        }
                        ValidateUrlResponse.BLOCK_URL -> {
                            println("BLOCK_URL")
                            su.validation = ValidateUrlState.VALIDATION_FAIL_BLOCK_URL
                            su.redirection.mode = 403
                            shortUrlRepository.save(su)
                        }
                        ValidateUrlResponse.BLOCK_IP -> {
                            println("BLOCK_IP")
                            su.validation = ValidateUrlState.VALIDATION_FAIL_BLOCK_IP
                            su.redirection.mode = 403
                            shortUrlRepository.save(su)
                        }
                    }
                    println(su)
                    su
                }
            } else {
                throw InvalidUrlException(url)
            }
}
