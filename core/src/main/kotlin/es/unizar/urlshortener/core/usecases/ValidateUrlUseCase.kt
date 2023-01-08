package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.core.io.ClassPathResource
import java.io.File
import java.io.IOException
import java.util.*


/**
 * Class [ValidateUrlUseCase] Given url, it checks that it is not on a
 * blocked list and neither is the creator's ip
 */

interface ValidateUrlUseCase {
    suspend fun blockURL(id: String, url: String)
    suspend fun blockIP(id: String, ipRemote: String)
}

/**
 * Implementation of [ValidateUrlResponse].
 */
class ValidateUrlUseCaseImpl(
        private val shortUrlRepository: ShortUrlRepositoryService
) : ValidateUrlUseCase {

    /*** Comprobar que la [url] no esta en la lista de bloqueados ***/
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

    /*** Comprobar que la [ipRemote] del creador no esta en la lista de bloqueados ***/
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