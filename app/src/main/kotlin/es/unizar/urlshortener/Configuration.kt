package es.unizar.urlshortener

import es.unizar.urlshortener.core.FileStore
import es.unizar.urlshortener.core.rabbitmq.Receiver
import es.unizar.urlshortener.core.usecases.*
import es.unizar.urlshortener.infrastructure.delivery.HashServiceImpl
import es.unizar.urlshortener.infrastructure.delivery.ValidatorServiceImpl
import es.unizar.urlshortener.infrastructure.repositories.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Wires use cases with service implementations, and services implementations with repositories.
 *
 * **Note**: Spring Boot is able to discover this [Configuration] without further configuration.
 */
@Configuration
class ApplicationConfiguration(
    @Autowired val shortUrlEntityRepository: ShortUrlEntityRepository,
    @Autowired val clickEntityRepository: ClickEntityRepository,
    @Autowired var fileStorage: FileStore
) {

    @Bean
    fun cleanFileStorage() {
        fileStorage.deleteAll()
        fileStorage.init()
    }

    @Bean
    fun clickRepositoryService() = ClickRepositoryServiceImpl(clickEntityRepository)

    @Bean
    fun shortUrlRepositoryService() = ShortUrlRepositoryServiceImpl(shortUrlEntityRepository)

    @Bean
    fun validatorService() = ValidatorServiceImpl()

    @Bean
    fun hashService() = HashServiceImpl()

    @Bean
    fun redirectUseCase() = RedirectUseCaseImpl(shortUrlRepositoryService())

    @Bean
    fun logClickUseCase() = LogClickUseCaseImpl(clickRepositoryService())

    @Bean
    fun createShortUrlUseCase() =
        CreateShortUrlUseCaseImpl(shortUrlRepositoryService(), validatorService(), hashService())

    /*** Nueva funcionalidades ***/
    @Bean
    fun validateUrlUseCase() = ValidateUrlUseCaseImpl(shortUrlRepositoryService())

    @Bean
    fun infoClientUserCase() = InfoClientUserCaseImpl(clickRepositoryService())

    @Bean
    fun createUrlsFromCsvUseCase() = CreateUrlsFromCsvUseCaseImpl(createShortUrlUseCase())

    @Bean
    fun Receiver() = Receiver(shortUrlRepositoryService())

}