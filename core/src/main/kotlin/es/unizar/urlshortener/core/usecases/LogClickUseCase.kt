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
 *
 * **Note**: This is an example of functionality.
 */
interface LogClickUseCase {
    fun logClick(key: String, data: ClickProperties)
    suspend fun setPropieties(id: String, data: UserAgent)
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

    override suspend fun setPropieties(id: String, data: UserAgent) {
        CoroutineScope(Dispatchers.IO).launch() {
            async { setBrowser(id, data) }
            async { setPlataform(id, data) }
        }
    }

    /** Devuelve el nombre del navegador desde donde se hace la peticion ***/
    override suspend fun setBrowser(id: String, data: UserAgent) {
        clickRepository.editBrowser(id, data.toString().split("/")[2])
    }

    /** Devuelve el nombre del SO desde donde se hace la peticion ***/
    override suspend fun setPlataform(id: String, data: UserAgent) {
        clickRepository.editSO(id, data.toString().split("/")[1])
    }
}
