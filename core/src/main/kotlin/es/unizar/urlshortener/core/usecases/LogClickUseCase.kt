package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.Click
import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.ClickRepositoryService
import java.util.*
import javax.servlet.http.HttpServletRequest

/**
 * Log that somebody has requested the redirection identified by a key.
 *
 * **Note**: This is an example of functionality.
 */
interface LogClickUseCase {
    fun logClick(key: String, data: ClickProperties)
    fun getBrowser(browserDetails: String): String
    fun getPlataform(browserDetails: String): String
}

/**
 * Implementation of [LogClickUseCase].
 */
class LogClickUseCaseImpl(
    private val clickRepository: ClickRepositoryService
) : LogClickUseCase {
    override fun logClick(key: String, data: ClickProperties) {
        val cl = Click(
            hash = key,
            properties = ClickProperties(
                ip = data.ip, browser = data.browser, platform = data.platform
            )
        )
        clickRepository.save(cl)
    }

    /** Devuelve el nombre del navegador desde donde se hace la peticion ***/
    override fun getBrowser(browserDetails: String): String  {
        // Reference: https://gist.github.com/c0rp-aubakirov/a4349cbd187b33138969
        val user = browserDetails.lowercase(Locale.getDefault())

        var browser: String = ""

        //===============Browser===========================
        if (user.contains("msie")) {
            val substring = browserDetails.substring(browserDetails.indexOf("MSIE")).split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
            browser = substring.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0].replace("MSIE", "IE") + "-" + substring.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
        } else if (user.contains("safari") && user.contains("version")) {
            browser = (browserDetails.substring(browserDetails.indexOf("Safari")).split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]).split(
                    "/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0] + "-" + (browserDetails.substring(
                    browserDetails.indexOf("Version")).split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]).split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
        } else if (user.contains("opr") || user.contains("opera")) {
            if (user.contains("opera")) browser = (browserDetails.substring(browserDetails.indexOf("Opera")).split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]).split(
                    "/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0] + "-" + (browserDetails.substring(
                    browserDetails.indexOf("Version")).split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]).split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1] else if (user.contains("opr")) browser = browserDetails.substring(browserDetails.indexOf("OPR")).split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0].replace("/",
                    "-").replace(
                    "OPR", "Opera")
        } else if (user.contains("chrome")) {
            browser = browserDetails.substring(browserDetails.indexOf("Chrome")).split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0].replace("/", "-")
        } else if (user.indexOf("mozilla/7.0") > -1 || user.indexOf("netscape6") != -1 || user.indexOf(
                        "mozilla/4.7") != -1 || user.indexOf("mozilla/4.78") != -1 || (user.indexOf(
                        "mozilla/4.08") != -1) || (user.indexOf("mozilla/3") != -1)) {
            //browser=(userAgent.substring(userAgent.indexOf("MSIE")).split(" ")[0]).replace("/", "-");
            browser = "Netscape-?"
        } else if (user.contains("firefox")) {
            browser = (browserDetails.substring(browserDetails.indexOf("Firefox")).split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]).replace("/", "-")
        } else if (user.contains("rv")) {
            browser = "IE"
        } else {
            browser = "TEST NAVEGADOR"
        }

        return (browser)!!
    }

    /** Devuelve el nombre del SO desde donde se hace la peticion ***/
    override fun getPlataform(browserDetailsUperCase: String): String {
        // Reference: https://gist.github.com/c0rp-aubakirov/a4349cbd187b33138969
        val browserDetails = browserDetailsUperCase.lowercase();
        //=================OS=======================
        if (browserDetails.contains("windows")) {
            return "Windows";
        } else if (browserDetails.contains("mac")) {
            return "Mac";
        } else if (browserDetails.contains("x11")) {
            return "Unix";
        } else if (browserDetails.contains("android")) {
            return "Android";
        } else if (browserDetails.contains("iphone")) {
            return "IPhone";
        } else {
            return "TEST PLATAFORMA";
        }
    }
}
