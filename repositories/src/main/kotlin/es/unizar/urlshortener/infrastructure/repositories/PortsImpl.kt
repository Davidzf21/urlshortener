package es.unizar.urlshortener.infrastructure.repositories

import es.unizar.urlshortener.core.Click
import es.unizar.urlshortener.core.ClickRepositoryService
import es.unizar.urlshortener.core.ShortUrl
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import es.unizar.urlshortener.core.*

/**
 * Implementation of the port [ClickRepositoryService].
 */
class ClickRepositoryServiceImpl(
    private val clickEntityRepository: ClickEntityRepository
) : ClickRepositoryService {
    override fun save(cl: Click): Click = clickEntityRepository.save(cl.toEntity()).toDomain()

    override fun findByHash(id: String): List<Click> = clickEntityRepository.findAllByHash(id).map { it.toDomain() }

    override fun existHash(id: String): Boolean = clickEntityRepository.existsByHash(id)

    override fun editBrowser(id: String, data:String): Boolean {
        id.let{
            val isUpdated = clickEntityRepository.updateBrowser(id, data)
            return isUpdated == 1
        }
    }

    override fun editSO(id: String, data:String): Boolean {
        id.let{
            val isUpdated = clickEntityRepository.updateSO(id, data)
            return isUpdated == 1
        }
    }
}

/**
 * Implementation of the port [ShortUrlRepositoryService].
 */
class ShortUrlRepositoryServiceImpl(
    private val shortUrlEntityRepository: ShortUrlEntityRepository
) : ShortUrlRepositoryService {
    override fun findByKey(id: String): ShortUrl? = shortUrlEntityRepository.findByHash(id)?.toDomain()

    override fun save(su: ShortUrl): ShortUrl = shortUrlEntityRepository.save(su.toEntity()).toDomain()

    override fun updateValidate(id: String, state: ValidateUrlState): Boolean {
        id.let{
            val isUpdated = shortUrlEntityRepository.updateValidateByHash(id,state)
            return isUpdated == 1
        }
    }

    override fun updateMode(id: String, mode: Int): Boolean {
        id.let{
            val isUpdated = shortUrlEntityRepository.updateModeByHash(id,mode)
            return isUpdated == 1
        }
    }

    override fun updateSafe(id: String, safe: Boolean): Boolean {
        id.let{
            val isUpdated = shortUrlEntityRepository.updateSafeByHash(id,safe)
            return isUpdated == 1
        }
    }

    override fun deleteByKey(id: String) {
        shortUrlEntityRepository.deleteByHash(id)
    }
}

