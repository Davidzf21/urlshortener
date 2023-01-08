package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.FileStore
import es.unizar.urlshortener.core.usecases.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
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
    @Operation(summary = "Devuelve la pagina HTML estática para subir un documento .csv",
            description = "Devuelve la pagina HTML estática para subir un documento .csv, si se sube un " +
                    "fichero de un formato distinto, no lo procesa")
    fun uploadCsvPage(model: MutableMap<String, Any>): String

    /**
     * Creates a short url from details provided in [data].
     *
     * **Note**: Delivery of use case [CreateShortUrlUseCase].
     */
    @Operation(summary = "Devuelve un fichero .csv con las urls acortadas",
            description = "Dado un fichero .csv, devuekve un fichero check .csv con todas las urls acortadas e" +
                    " indicando si una url esta en un formato erroneo",
            parameters = [Parameter(name = "file", description = "Nombre del fichero subido",
                    required = true, example = "Ejemplo.csv", schema = Schema(type = "string"))])
    fun processCsv(file: MultipartFile, request: HttpServletRequest,
                  response: HttpServletResponse)

}

@Tag(name = "CSV Endpoints", description = "Operations related to .csv files")
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

        val requestUrl = request.requestURL.toString()
        val serverAddress = Regex("(https?://[^:/]+)(:\\d+)?/").find(requestUrl)?.value
        createUrlFromCsvUseCase.create(file, request.remoteAddr, serverAddress.toString()).let {
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
        }

    }

}