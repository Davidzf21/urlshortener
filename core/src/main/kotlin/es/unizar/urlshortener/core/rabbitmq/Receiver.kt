package es.unizar.urlshortener.core.rabbitmq

import es.unizar.urlshortener.core.ReachableUrlState
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.client.RestTemplate

class Receiver (
        private val shortUrlRepository: ShortUrlRepositoryService
){

    @Autowired
    lateinit var restTemplate: RestTemplate

    companion object {
        const val RECEIVE_METHOD_NAME = "receiveMessage"
    }

    fun receiveMessage(message: String) {
        println("[Receiver] ha recibido el mensaje \"$message\"")
        reachableURL(message.split("|")[0], message.split("|")[1])
    }

    /*** Validacion de que la URL es alcanzable ***/
    fun reachableURL(id: String, url: String) {
        try {
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
}