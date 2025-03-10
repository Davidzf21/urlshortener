package es.unizar.urlshortener.core

import es.unizar.urlshortener.core.*

/**
 * [ClickRepositoryService] is the port to the repository that provides persistence to [Clicks][Click].
 */
interface ClickRepositoryService {
    fun save(cl: Click): Click
    fun existHash(id: String): Boolean
    fun editBrowser(id: String, data:String): Boolean //Nueva funcionalidad
    fun editSO(id: String, data:String): Boolean //Nueva funcionalidad
    fun findByHash(id: String): List<Click>
}

/**
 * [ShortUrlRepositoryService] is the port to the repository that provides management to [ShortUrl][ShortUrl].
 */
interface ShortUrlRepositoryService {
    fun findByKey(id: String): ShortUrl?
    fun save(su: ShortUrl): ShortUrl
    fun updateBlockInfo(id: String, state: BlockUrlState) //Nueva funcionalidad
    fun updateReachableInfo(id: String, state: ReachableUrlState) //Nueva funcionalidad
    fun updateMode(id: String, mode: Int): Boolean //Nueva funcionalidad
    fun updateSafe(id: String, safe: Boolean): Boolean //Nueva funcionalidad
    fun deleteByKey(id: String)
}

/**
 * [ValidatorService] is the port to the service that validates if an url can be shortened.
 *
 * **Note**: It is a design decision to create this port. It could be part of the core .
 */
interface ValidatorService {
    fun isValid(url: String): Boolean
}

/**
 * [HashService] is the port to the service that creates a hash from a URL.
 *
 * **Note**: It is a design decision to create this port. It could be part of the core .
 */
interface HashService {
    fun hasUrl(url: String): String
}