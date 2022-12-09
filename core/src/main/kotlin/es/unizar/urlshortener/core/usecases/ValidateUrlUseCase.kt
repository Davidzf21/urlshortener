package es.unizar.urlshortener.core.usecases

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import es.unizar.urlshortener.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
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
    suspend fun ValidateURL(url: String, ipRemote: String): ValidateUrlResponse
    suspend fun ReachableURL(url: String): ValidateUrlResponse
    suspend fun SafeURL(url: String): ValidateUrlResponse
    suspend fun BlockURL(url: String): ValidateUrlResponse
    suspend fun BlockIP(ipRemote: String): ValidateUrlResponse
}

/**
 * Implementation of [ValidateUrlResponse].
 */
class ValidateUrlUseCaseImpl(
) : ValidateUrlUseCase {

    @Autowired
    lateinit var restTemplate: RestTemplate

    @Value("\${google.API.clientName}")
    lateinit var googleClient: String
    @Value("\${google.API.clientVersion}")
    lateinit var googleVersion: String
    @Value("\${google.API.url}")
    lateinit var googleUrl: String
    @Value("\${google.API.value}")
    lateinit var googleValue: String

    /*** Comprobacion en paralelo y con corutanas de que la URL es segura y alcanzable ***/
    override suspend fun ValidateURL(url: String, ipRemote: String): ValidateUrlResponse = coroutineScope {
        // Lanzamiento hilos ligeros
        val respReachable = async { ReachableURL(url) }
        val respSafe = async { SafeURL(url) }
        val respBlockURL = async { BlockURL(url) }
        val respBlockIP = async { BlockIP(ipRemote) }
        // Respuesta hilos ligeros
        if(respBlockURL.await() != ValidateUrlResponse.OK) respBlockURL.await()
        else if (respBlockIP.await() != ValidateUrlResponse.OK) respBlockIP.await()
        else if (respSafe.await() != ValidateUrlResponse.OK) respSafe.await()
        else respReachable.await()
    }

    /*** Validacion de que la URL es alcanzable ***/
    override suspend fun ReachableURL(url: String): ValidateUrlResponse {
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

    /*** Validacion de que la URL es segura con Google Safe Browse ***/
    override suspend fun SafeURL(url: String): ValidateUrlResponse {
        val Request = ThreatMatchesFindRequestBody(
                ClientInfo(googleClient, googleVersion),
                ThreatInfo(
                        listOf(ThreatType.MALWARE,ThreatType.POTENTIALLY_HARMFUL_APPLICATION,ThreatType.UNWANTED_SOFTWARE),
                        listOf(PlatformType.ALL_PLATFORMS),
                        listOf(ThreatEntryType.URL),
                        listOf(ThreatEntry(url,ThreatEntryRequestType.URL))
                )
        )
        val mapper = jacksonObjectMapper()
        val serializador = mapper.writeValueAsString(Request)
        //https://testsafebrowsing.appspot.com/s/malware.html UNSAFE EXAMPLE
        val httpResponse = restTemplate.postForObject(URI(googleUrl+googleValue), HttpEntity(serializador),ThreatMatchesFindResponseBody::class.java)
        if(!httpResponse?.matches.isNullOrEmpty()){
            return ValidateUrlResponse.UNSAFE
        }
        return ValidateUrlResponse.OK
    }

    /*** Comprobar que la URL no esta en la lista de bloqueados ***/
    override suspend fun BlockURL(url: String): ValidateUrlResponse {
        val path = Paths.get("repositories/src/main/resources/BLOCK_URL.txt")
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
    override suspend fun BlockIP(ipRemote: String): ValidateUrlResponse {
        val path = Paths.get("repositories/src/main/resources/BLOCK_IP.txt")
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