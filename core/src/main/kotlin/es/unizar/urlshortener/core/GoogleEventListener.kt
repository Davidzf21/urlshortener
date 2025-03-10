package es.unizar.urlshortener.core

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.http.HttpEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.net.URI

/**
 * Component [GoogleEventListener] receives all events from the event queue and processes them.
 */
@Component
class GoogleEventListener (
        private val shortUrlRepository: ShortUrlRepositoryService
        ): ApplicationListener<GoogleEvent> {

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

    /** Recive le [event] de la cola de GoogleEvent **/
    override fun onApplicationEvent(event: GoogleEvent) {
        SafeURL(event.id, event.url)
    }

    /*** Validacion de que la [url] es segura con Google Safe Browse ***/
    private fun SafeURL(id: String, url: String) {
        val request = ThreatMatchesFindRequestBody(
                ClientInfo(googleClient, googleVersion),
                ThreatInfo(
                        listOf(ThreatType.MALWARE, ThreatType.POTENTIALLY_HARMFUL_APPLICATION, ThreatType.UNWANTED_SOFTWARE),
                        listOf(PlatformType.ALL_PLATFORMS),
                        listOf(ThreatEntryType.URL),
                        listOf(ThreatEntry(url, ThreatEntryRequestType.URL))
                )
        )
        val mapper = jacksonObjectMapper()
        val serializador = mapper.writeValueAsString(request)
        //https://testsafebrowsing.appspot.com/s/malware.html UNSAFE EXAMPLE
        val httpResponse = restTemplate.postForObject(URI(googleUrl+googleValue), HttpEntity(serializador),
                ThreatMatchesFindResponseBody::class.java)
        if(!httpResponse?.matches.isNullOrEmpty()){
            println("NOT SAFE")
            shortUrlRepository.updateMode(id, 403)
            shortUrlRepository.updateSafe(id, false)
        } else {
            shortUrlRepository.updateSafe(id, true)
        }
    }
}