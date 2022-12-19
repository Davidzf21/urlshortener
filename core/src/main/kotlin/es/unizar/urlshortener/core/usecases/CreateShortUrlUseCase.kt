package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import kotlinx.coroutines.*
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
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

    @Value("\${spring.rabbitmq.exchange}")
    lateinit var exchangeName: String
    @Value("\${spring.rabbitmq.routingkey}")
    lateinit var routingKey: String

    @Autowired
    lateinit var rabbitTemplate: RabbitTemplate

    @Autowired
    lateinit var validateUrlUseCase: ValidateUrlUseCase

    @Autowired
    lateinit var applicationEventPublisher: ApplicationEventPublisher

    override fun create(url: String, data: ShortUrlProperties): ShortUrl =
            if (validatorService.isValid(url)) {
                val id: String = hashService.hasUrl(url)
                var su = ShortUrl(
                        hash = id,
                        redirection = Redirection(target = url),
                        properties = ShortUrlProperties(
                                safe = data.safe,
                                ip = data.ip,
                                sponsor = data.sponsor
                        ),
                        blockInfo = BlockUrlState.OK,
                        reachableInfo = ReachableUrlState.REACHABLE
                )
                shortUrlRepository.save(su)

                /*** Enviar mensaje por RabbitMQ para comprobar la accesibilidad (Reachable) ***/
                println("[Application] Enviando el mensaje ...");
                rabbitTemplate.convertAndSend(exchangeName, routingKey, "$id|$url");

                /*** Enviar mensaje en la cola para Google Safe Browsing ***/
                applicationEventPublisher.publishEvent(GoogleEvent(this, su.hash, url))

                /*** Validaciones de la URL con Corutinas ***/
                CoroutineScope(Dispatchers.IO).launch() {
                    async { validateUrlUseCase.blockURL(id, url) }
                    async { validateUrlUseCase.blockIP(id, data.ip!!) }
                }

                shortUrlRepository.findByKey(su.hash)!!
            } else {
                throw InvalidUrlException(url)
            }
}
