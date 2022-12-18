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
    suspend fun validateURL(id: String, url: String, ipRemote: String): ValidateUrlResponse
    suspend fun reachableURL(url: String): ValidateUrlResponse
    suspend fun blockURL(url: String): ValidateUrlResponse
    suspend fun blockIP(ipRemote: String): ValidateUrlResponse
}

/**
 * Implementation of [ValidateUrlResponse].
 */
class ValidateUrlUseCaseImpl(
        private val shortUrlRepository: ShortUrlRepositoryService
) : ValidateUrlUseCase {

    @Autowired
    lateinit var restTemplate: RestTemplate

    /*** Comprobacion en paralelo y con corutanas de que la URL es segura y alcanzable ***/
    override suspend fun validateURL(id: String, url: String, ipRemote: String): ValidateUrlResponse = coroutineScope {
        // Lanzamiento hilos ligeros
        val respReachable = async { reachableURL(url) }
        val respBlockURL = async { blockURL(url) }
        val respBlockIP = async { blockIP(ipRemote) }
        // Respuesta hilos ligeros
        if(respBlockURL.await() != ValidateUrlResponse.OK) {
            println("BLOCK URL")
            shortUrlRepository.updateValidate(id, ValidateUrlState.VALIDATION_FAIL_BLOCK_URL)
            shortUrlRepository.updateMode(id, 403)
            respBlockURL.await()
        }
        else if (respBlockIP.await() != ValidateUrlResponse.OK){
            println("BLOCK IP")
            shortUrlRepository.updateValidate(id, ValidateUrlState.VALIDATION_FAIL_BLOCK_IP)
            shortUrlRepository.updateMode(id, 403)
            respBlockIP.await()
        }
        else if (respReachable.await() != ValidateUrlResponse.OK){
            println("NOT REACHABLE")
            shortUrlRepository.updateValidate(id, ValidateUrlState.VALIDATION_FAIL_NOT_REACHABLE)
            shortUrlRepository.updateMode(id, 400)
            respReachable.await()
        } else {
            ValidateUrlResponse.OK
        }
    }

    /*** Validacion de que la URL es alcanzable ***/
    override suspend fun reachableURL(url: String): ValidateUrlResponse {
        return try {
            var resp = restTemplate.getForEntity(url, String::class.java)
            if(resp.statusCode.is2xxSuccessful) {
                ValidateUrlResponse.OK
            } else {
                ValidateUrlResponse.NO_REACHABLE
            }
        } catch (e: Exception){
            ValidateUrlResponse.NO_REACHABLE
        }
    }

    /*** Comprobar que la URL no esta en la lista de bloqueados ***/
    override suspend fun blockURL(url: String): ValidateUrlResponse {
        val path = ClassPathResource("BLOCK_URL.txt").file
        try {
            val sc = withContext(Dispatchers.IO) {
                Scanner(File(path.toString()))
            }
            while (sc.hasNextLine()) {
                val line = sc.nextLine()
                if(url.contains(line)){
                    return ValidateUrlResponse.BLOCK_URL
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return ValidateUrlResponse.OK
    }

    /*** Comprobar que la IP del creador no esta en la lista de bloqueados ***/
    override suspend fun blockIP(ipRemote: String): ValidateUrlResponse {
        val path = ClassPathResource("BLOCK_IP.txt").file
        try {
            val sc = withContext(Dispatchers.IO) {
                Scanner(File(path.toString()))
            }
            while (sc.hasNextLine()) {
                val line = sc.nextLine()
                if(line.equals(ipRemote)){
                    return ValidateUrlResponse.BLOCK_IP
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return ValidateUrlResponse.OK
    }

}