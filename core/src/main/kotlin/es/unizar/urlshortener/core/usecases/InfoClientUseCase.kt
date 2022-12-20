package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle;

/**
 * Class [InfoClientUserCase] Given a hash of url, it returns the list of the information
 * of all the times that have been clicked.
 */
interface InfoClientUserCase {
    fun getInfo(key: String): ArrayList<InfoClientResponse>?
}

/**
 * Implementation of [InfoClientUserCase].
 */
class InfoClientUserCaseImpl(
        private val clickRepository: ClickRepositoryService
) : InfoClientUserCase {
    override fun getInfo(key: String): ArrayList<InfoClientResponse>? {
        if(clickRepository.existHash(key)){
            var list = ArrayList<InfoClientResponse>()
            val fmt: DateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            for(e in clickRepository.findByHash(key)){
                list.add(InfoClientResponse(fmt.format(e.created), e.properties.browser, e.properties.platform))
            }
            return list
        } else {
            return null
        }
    }
}