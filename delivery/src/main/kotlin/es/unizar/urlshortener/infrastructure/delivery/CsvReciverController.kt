package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.FileStore
import es.unizar.urlshortener.core.usecases.*
import org.apache.tomcat.util.http.fileupload.IOUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

interface CsvReciverController {

    /**
     * Redirects and logs a short url identified by its [id].
     *
     * **Note**: Delivery of use cases [RedirectUseCase] and [LogClickUseCase].
     */
    fun uploadCsvPage(model: MutableMap<String, Any>): String

    /**
     * Creates a short url from details provided in [data].
     *
     * **Note**: Delivery of use case [CreateShortUrlUseCase].
     */
    fun processCsv(file: MultipartFile, request: HttpServletRequest,
                  response: HttpServletResponse)

}

@Controller
class CsvReciverControllerImpl(
        val createUrlFromCsvUseCase: CreateUrlsFromCsvUseCase
) : CsvReciverController {

    @Autowired
    lateinit var fileStorage: FileStore

    @GetMapping("/api/bulk")
    override fun uploadCsvPage(model: MutableMap<String, Any>): String {
        return "uploadPage"
    }

    @PostMapping("/api/bulk")
    override fun processCsv(@RequestParam("file") file: MultipartFile, request: HttpServletRequest,
                           response: HttpServletResponse) {

        createUrlFromCsvUseCase.create(file, request.remoteAddr).let {
            val nuevoNombre = "${fileStorage.generateName()}.csv"
            val fileGenerated = fileStorage.newFile(nuevoNombre)
            var clientFileName = file.originalFilename?.split(".")?.get(0)!!
            fileStorage.overWriteFile(nuevoNombre, it)
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""+
                    clientFileName +"_check.csv\"")
            response.contentType = "text/csv"
            response.status = HttpStatus.CREATED.value()
            IOUtils.copy(fileGenerated.inputStream, response.outputStream)
            response.outputStream.close()
            fileGenerated.inputStream.close()
            //fileStorage.deleteFile(nuevoNombre)
        }

    }

}