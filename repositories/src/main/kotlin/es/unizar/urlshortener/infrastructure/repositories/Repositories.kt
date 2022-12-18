package es.unizar.urlshortener.infrastructure.repositories

import es.unizar.urlshortener.core.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import javax.persistence.LockModeType

/**
 * Specification of the repository of [ShortUrlEntity].
 *
 * **Note**: Spring Boot is able to discover this [JpaRepository] without further configuration.
 */
interface ShortUrlEntityRepository : JpaRepository<ShortUrlEntity, String> {
    fun findByHash(hash: String): ShortUrlEntity?

    @Transactional
    fun deleteByHash(hash: String)

    @Transactional
    @Modifying
    @Query(value = "update ShortUrlEntity u set u.validation = ?2 where u.hash = ?1")
    fun updateValidateByHash(hash: String, status: ValidateUrlState): Int

    @Transactional
    @Modifying
    @Query(value = "update ShortUrlEntity u set u.mode = ?2 where u.hash = ?1")
    fun updateModeByHash(hash: String, mode: Int): Int

    @Transactional
    @Modifying
    @Query(value = "update ShortUrlEntity u set u.safe = ?2 where u.hash = ?1")
    fun updateSafeByHash(hash: String, safe: Boolean): Int
}

/**
 * Specification of the repository of [ClickEntity].
 *
 * **Note**: Spring Boot is able to discover this [JpaRepository] without further configuration.
 */
interface ClickEntityRepository : JpaRepository<ClickEntity, Long> {

    fun existsByHash(hash: String): Boolean

    fun findAllByHash(hash: String): List<ClickEntity>

    @Transactional
    @Modifying
    @Query(value = "update ClickEntity u set u.browser = ?2 where u.hash = ?1 and u.browser IS NULL")
    fun updateBrowser(hash: String, data: String): Int

    @Transactional
    @Modifying
    @Query(value = "update ClickEntity u set u.platform = ?2 where u.hash = ?1 and u.platform IS NULL")
    fun updateSO(hash: String, data: String): Int

}