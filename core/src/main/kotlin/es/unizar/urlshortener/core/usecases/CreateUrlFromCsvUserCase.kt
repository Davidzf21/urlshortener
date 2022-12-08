package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import org.springframework.web.multipart.MultipartFile

/**
 * Given a file creates a short URL for each url in the file
 * and creates a new file with the short URL or the error occurred.
 */
interface CreateUrlsFromCsvUseCase {
    fun create(file: MultipartFile, remoteAddr: String): ArrayList<String>
}

/**
 * Implementation of [CreateUrlsFromCsvUseCase].
 */
class CreateUrlsFromCsvUseCaseImpl(
        private val createShortUrlUseCase: CreateShortUrlUseCase
) : CreateUrlsFromCsvUseCase {
    override fun create(file: MultipartFile, remoteAddr: String): ArrayList<String> {
        if(checkTypeFile(file)){
            throw InvalidFileType()
        }
        val shortUrlsFile = ArrayList<ShortUrl>()
        val content = String(file.bytes).split("\r\n")
        var fall = false
        for(line in content){
            if(line.isNotEmpty()) {
                if(!line.contains(";")) {
                    val shortUrl = createShortUrlUseCase.create(
                            url = line,
                            data = ShortUrlProperties(
                                    ip = remoteAddr,
                            )
                    )
                    shortUrlsFile.add(shortUrl)
                } else {
                    fall = true
                    break
                }
            }
        }
        return makeList(shortUrlsFile, fall)
    }

    private fun checkTypeFile(file: MultipartFile): Boolean {
        val nameSplitByPoint = file.originalFilename?.split(".")
        if (nameSplitByPoint == null || nameSplitByPoint[nameSplitByPoint.size-1] != "csv") {
            return true
        }
        return false
    }

    private fun makeList(it: ArrayList<ShortUrl>, fall: Boolean): ArrayList<String> {
        val newLines = ArrayList<String>()
        for(i in 0 until it.size){
            val originalURL = it[i].redirection.target
            val shortURL = "http://localhost:8080/" + it[i].hash
            var error = "OK"
            if (it[i].validation.equals(ValidateUrlState.VALIDATION_FAIL_NOT_REACHABLE)){
                error = "ERROR: VALIDATION_FAIL_NOT_REACHABLE"
            } else if (it[i].validation.equals(ValidateUrlState.VALIDATION_FAIL_NOT_SAFE)){
                error = "ERROR: VALIDATION_FAIL_NOT_SAFE"
            } else if (it[i].validation.equals(ValidateUrlState.VALIDATION_FAIL_BLOCK)){
                error = "ERROR: VALIDATION_FAIL_BLOCK"
            }
            newLines.add("$originalURL;$shortURL;$error")
        }
        if(fall){
            newLines.add("FALLO DE FORMATO;NO SE HA PODIDO PROCESAR EL FICHERO")
        }
        return newLines
    }

}