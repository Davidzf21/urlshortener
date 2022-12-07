package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
    fun ValidateURL(url: String): ValidateUrlResponse
    fun ReachableURL(url: String): ValidateUrlResponse
    fun SafeURL(url: String): ValidateUrlResponse
    fun BlockURL(url: String): ValidateUrlResponse
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
    override fun ValidateURL(url: String): ValidateUrlResponse {
        var response1 = ReachableURL(url)
        var response2 = SafeURL(url)
        var response3 = BlockURL(url)
        if(response1.equals(ValidateUrlResponse.NO_REACHABLE)){
            return response1
        } else if (response2.equals(ValidateUrlResponse.UNSAFE)){
            return response2
        } else if(response3.equals(ValidateUrlResponse.BLOCK)){
            return response3
        } else {
            return ValidateUrlResponse.OK
        }
    }

    /*** Validacion de que la URL es alcanzable ***/
    override fun ReachableURL(url: String): ValidateUrlResponse {
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
    override fun SafeURL(url: String): ValidateUrlResponse {
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
    override fun BlockURL(url: String): ValidateUrlResponse {
        val path = Paths.get("repositories/src/main/resources/BLOCK_URL.txt")
        try {
            val sc = Scanner(File(path.toString()))
            while (sc.hasNextLine()) {
                val line = sc.nextLine()
                if(line.equals(url)){
                    return ValidateUrlResponse.BLOCK
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return ValidateUrlResponse.OK
    }

}