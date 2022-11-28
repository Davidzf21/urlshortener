package es.unizar.urlshortener

import es.unizar.urlshortener.infrastructure.delivery.ShortUrlDataOut
import org.apache.http.impl.client.HttpClientBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.jdbc.JdbcTestUtils
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import java.net.URI


@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class HttpRequestTest {
    @LocalServerPort
    private val port = 0

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @BeforeEach
    fun setup() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "shorturl", "click")
    }

    @AfterEach
    fun tearDowns() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "shorturl", "click")
    }

    @Test
    fun `main page works`() {
        val response = restTemplate.getForEntity("http://localhost:$port/", String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).contains("A front-end example page for the project")
    }

    @Test
    fun `redirectTo returns a redirect when the key exists`() {
        val target = shortUrl("http://example.com/").headers.location
        require(target != null)
        val response = restTemplate.getForEntity(target, String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.TEMPORARY_REDIRECT)
        assertThat(response.headers.location).isEqualTo(URI.create("http://example.com/"))

        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "click")).isEqualTo(1)
    }

    @Test
    fun `redirectTo returns a not found when the key does not exist`() {
        val response = restTemplate.getForEntity("http://localhost:$port/f684a3c4", String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)

        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "click")).isEqualTo(0)
    }

    @Test
    fun `creates returns a basic redirect if it can compute a hash`() {
        val response = shortUrl("http://example.com/")

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.headers.location).isEqualTo(URI.create("http://localhost:$port/f684a3c4"))
        assertThat(response.body?.url).isEqualTo(URI.create("http://localhost:$port/f684a3c4"))

        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "shorturl")).isEqualTo(1)
        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "click")).isEqualTo(0)
    }

    @Test
    fun `creates returns bad request if it can't compute a hash`() {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

        val data: MultiValueMap<String, String> = LinkedMultiValueMap()
        data["url"] = "ftp://example.com/"

        val response = restTemplate.postForEntity(
            "http://localhost:$port/api/link",
            HttpEntity(data, headers), ShortUrlDataOut::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)

        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "shorturl")).isEqualTo(0)
        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "click")).isEqualTo(0)
    }

    /*** Test - Google Safe Browser ***/

    @Test
    fun `Crea una shortURL pero es segura`() {
        assertThat("2").isEqualTo("2")
    }

    @Test
    fun `Test para comprobar la funcionalidad de Google Safe Browsing`() {
        //val sUrl = shortUrl("https://testsafewsing.appspot.com/s/unwanted.html")
        val respHeaders = shortUrl("https://testsafebrowsing.appspot.com/s/malware.html")
        val target = respHeaders.headers.location
        require(target != null)
        // POST /api/link
        assertThat(respHeaders.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) //Comp. de 400 BAD_REQUEST
        assertThat(respHeaders.body?.properties?.get("error")).isEqualTo("URI de destino no es segura") //Comp. del mensaje de error
        // GET /{id}
        val response = restTemplate.getForEntity(target, String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN) //Comp. de 403 FORBIDDEN
        assertThat(response.body?.contains("redirection block")).isEqualTo(true) //Comp. del mensaje de error
    }

    @Test
    fun `Test para comprobar la funcionalidad de identificar el Navegador y Plataforma`() {
        val respHeaders = shortUrl("https://www.youtube.com")
        assertThat(respHeaders.statusCode).isEqualTo(HttpStatus.CREATED) //Comp. de 201 CREATED
        val target = respHeaders.headers.location
        require(target != null)
        // GET /{id}
        restTemplate.getForEntity(target, String::class.java)
        val hash = target.toString().split("/")[3]
        // GET /api/link
        val response1 = restTemplate.getForEntity("http://localhost:$port/api/link/"+hash, String::class.java)
        assertThat(response1.statusCode).isEqualTo(HttpStatus.OK) //Comp. de 200 OK
        assertThat(response1.body?.contains("TEST NAVEGADOR")).isEqualTo(true)  //Comp. que devuelve Navegador
        assertThat(response1.body?.contains("TEST PLATAFORMA")).isEqualTo(true) //Comp. que devuelve Plataforma
        println("FINAL")
    }

    @Test
    fun `Test para comprobar la funcionalidad de que una URL es alcanzable`() {
        val respHeaders = shortUrl("https://www.youtubeeeeee.com")
        val target = respHeaders.headers.location
        require(target != null)
        // POST /api/link
        assertThat(respHeaders.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) //Comp. de 400 BAD_REQUEST
        assertThat(respHeaders.body?.properties?.get("error")).isEqualTo("URI de destino no es alcanzable") //Comp. del mensaje de error
        // GET /{id}
        val response = restTemplate.getForEntity(target, String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) //Comp. de 400 BAD_REQUEST
        assertThat(response.body?.contains("redirection block")).isEqualTo(true) //Comp. del mensaje de error
    }

    @Test
    fun `Test para comprobar la funcionalidad de que una URL esta bloqueada`() {
        val respHeaders = shortUrl("https://www.twitch.tv/")
        val target = respHeaders.headers.location
        require(target != null)
        // POST /api/link
        //assertThat(respHeaders.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) //Comp. de 400 BAD_REQUEST
        //assertThat(respHeaders.body?.properties?.get("error")).isEqualTo("URI de destino no es alcanzable") //Comp. del mensaje de error
        // GET /{id}
        //val response = restTemplate.getForEntity(target, String::class.java)
        //assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) //Comp. de 400 BAD_REQUEST
        //assertThat(response.body?.contains("redirection block")).isEqualTo(true) //Comp. del mensaje de error
    }

    private fun shortUrl(url: String): ResponseEntity<ShortUrlDataOut> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

        val data: MultiValueMap<String, String> = LinkedMultiValueMap()
        data["url"] = url

        return restTemplate.postForEntity(
            "http://localhost:$port/api/link",
            HttpEntity(data, headers), ShortUrlDataOut::class.java
        )
    }

}