package es.unizar.urlshortener.core.usecases

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import es.unizar.urlshortener.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpEntity
import org.springframework.web.client.RestTemplate
import java.io.File
import java.io.IOException
import java.net.URI
import java.nio.file.Paths
import java.util.*

/**
 * Dada una URL comprueba se es alcanzable y segura mediante la
 * herramienta de Google Safe Browse.
 *
 * **Note**: This is an example of functionality.
 */

interface ValidateUrlUseCase {
    suspend fun reachableURL(id: String, url: String)
    suspend fun blockURL(id: String, url: String)
    suspend fun blockIP(id: String, ipRemote: String)
}

/**
 * Implementation of [ValidateUrlResponse].
 */
class ValidateUrlUseCaseImpl(
        private val shortUrlRepository: ShortUrlRepositoryService
) : ValidateUrlUseCase {

    @Autowired
    lateinit var restTemplate: RestTemplate

    /*** Validacion de que la URL es alcanzable ***/
    override suspend fun reachableURL(id: String, url: String) {
        return try {
            var resp = restTemplate.getForEntity(url, String::class.java)
            if(resp.statusCode.is2xxSuccessful) {
                shortUrlRepository.updateReachableInfo(id, ReachableUrlState.REACHABLE)
            } else {
                println("NOT REACHABLE")
                shortUrlRepository.updateMode(id, 400)
                shortUrlRepository.updateReachableInfo(id, ReachableUrlState.FAIL_NOT_REACHABLE)
            }
        } catch (e: Exception){
            println("NOT REACHABLE")
            shortUrlRepository.updateMode(id, 400)
            shortUrlRepository.updateReachableInfo(id, ReachableUrlState.FAIL_NOT_REACHABLE)
        }
    }

    /*** Comprobar que la URL no esta en la lista de bloqueados ***/
    override suspend fun blockURL(id: String, url: String) {
        val path = ClassPathResource("BLOCK_URL.txt").file
        try {
            val sc = withContext(Dispatchers.IO) {
                Scanner(File(path.toString()))
            }
            while (sc.hasNextLine()) {
                val line = sc.nextLine()
                if(url.contains(line)){
                    println("BLOCK URL")
                    shortUrlRepository.updateMode(id, 403)
                    shortUrlRepository.updateBlockInfo(id, BlockUrlState.FAIL_BLOCK_URL)
                }
            }
        } catch (e: IOException) {
            shortUrlRepository.updateMode(id, 403)
            shortUrlRepository.updateBlockInfo(id, BlockUrlState.FAIL_BLOCK_URL)
            e.printStackTrace()
        }
    }

    /*** Comprobar que la IP del creador no esta en la lista de bloqueados ***/
    override suspend fun blockIP(id: String, ipRemote: String) {
        val path = ClassPathResource("BLOCK_IP.txt").file
        try {
            val sc = withContext(Dispatchers.IO) {
                Scanner(File(path.toString()))
            }
            while (sc.hasNextLine()) {
                val line = sc.nextLine()
                if(line.equals(ipRemote)){
                    println("BLOCK IP")
                    shortUrlRepository.updateMode(id, 403)
                    shortUrlRepository.updateBlockInfo(id, BlockUrlState.FAIL_BLOCK_IP)
                }
            }
        } catch (e: IOException) {
            shortUrlRepository.updateMode(id, 403)
            shortUrlRepository.updateBlockInfo(id, BlockUrlState.FAIL_BLOCK_IP)
            e.printStackTrace()
        }
    }

}