package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.Click
import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.ClickRepositoryService
import kotlinx.coroutines.*
import org.springframework.web.servlet.function.ServerResponse.async
import ru.chermenin.ua.UserAgent
import java.util.*
import javax.servlet.http.HttpServletRequest

/**
 * Log that somebody has requested the redirection identified by a key.
 */
interface LogClickUseCase {
    fun logClick(key: String, data: ClickProperties)
    suspend fun setBrowser(id: String, data: UserAgent)
    suspend fun setPlataform(id: String, data: UserAgent)
}

/**
 * Implementation of [LogClickUseCase].
 */
class LogClickUseCaseImpl(
    private val clickRepository: ClickRepositoryService
) : LogClickUseCase {
    override fun logClick(key: String, data: ClickProperties) {
        val cl = Click(
            hash = key,
            properties = ClickProperties(
                ip = data.ip, browser = data.browser, platform = data.platform
            )
        )
        clickRepository.save(cl)
    }

    /** Persistencia del nombre del navegador [data] desde donde se hace la peticion de la url identificada como [id] ***/
    override suspend fun setBrowser(id: String, data: UserAgent) {
        clickRepository.editBrowser(id, data.toString().split("/")[2])
    }

    /** Persistencia del nombre del SO [data] desde donde se hace la peticion de la url identificada como [id] ***/
    override suspend fun setPlataform(id: String, data: UserAgent) {
        clickRepository.editSO(id, data.toString().split("/")[1])
    }
}
