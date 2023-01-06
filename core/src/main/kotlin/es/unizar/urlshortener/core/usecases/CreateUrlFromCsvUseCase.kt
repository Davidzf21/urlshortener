package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.rabbitmq.Receiver
import org.springframework.web.multipart.MultipartFile
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Given a file creates a short URL for each url in the file
 * and creates a new file with the short URL or the error occurred.
 */
interface CreateUrlsFromCsvUseCase {
    fun create(file: MultipartFile, remoteAddr: String, urlServer: String): ArrayList<String>
}

/**
 * Implementation of [CreateUrlsFromCsvUseCase].
 */
class CreateUrlsFromCsvUseCaseImpl(
        private val createShortUrlUseCase: CreateShortUrlUseCase
) : CreateUrlsFromCsvUseCase {

    override fun create(file: MultipartFile, remoteAddr: String, urlServer: String): ArrayList<String> {
        if(checkTypeFile(file)){
            throw InvalidFileType()
        }
        val shortUrlsFile = ArrayList<ShortUrl>()
        var fall = false
        val reader = BufferedReader(InputStreamReader(file.inputStream))
        var line: String? = reader.readLine()
        while (!line.isNullOrEmpty()) {
            if(!line.contains(";")) {
                if(line.contains("https://")) {
                    val shortUrl = createShortUrlUseCase.create(
                            url = line,
                            data = ShortUrlProperties(
                                    ip = remoteAddr,
                            )
                    )
                    shortUrlsFile.add(shortUrl)
                } else {
                    val shortUrlFail = ShortUrl (hash = "Format Invalid", redirection = Redirection (target = line))
                    shortUrlsFile.add(shortUrlFail)
                }
            } else {
                fall = true
                break
            }
            line = reader.readLine()
        }
        return makeList(shortUrlsFile, fall, urlServer)
    }

    /**
     * La función [checkTypeFile] sirve para comprobar que el fichero tiene un formato válido.
     */
    private fun checkTypeFile(file: MultipartFile): Boolean {
        val nameSplitByPoint = file.originalFilename?.split(".")
        if (nameSplitByPoint == null || nameSplitByPoint[nameSplitByPoint.size-1] != "csv") {
            return true
        }
        return false
    }

    /**
     * La función [makeList] sirve para crear la lista de URLs acortadas.
     */
    private fun makeList(it: ArrayList<ShortUrl>, fall: Boolean, urlServer: String): ArrayList<String> {
        val newLines = ArrayList<String>()
        for(i in 0 until it.size){
            val originalURL = it[i].redirection?.target
            var shortURL = "Format Invalid"
            var error = "OK"
            if (it[i].hash != "Format Invalid") { // Comprobación de formato válido 'http' o 'https'
                shortURL = urlServer + it[i].hash
            }
            else {
                error = "ERROR: debe ser una URI http o https"
            }
            newLines.add("$originalURL;$shortURL;$error")
        }
        if(fall){ // Comprobación de contenido de fichero válido
            newLines.add("FALLO DE FORMATO;NO SE HA PODIDO PROCESAR EL FICHERO")
        }
        return newLines
    }

}