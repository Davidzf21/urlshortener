package es.unizar.urlshortener

import es.unizar.urlshortener.infrastructure.delivery.ShortUrlDataOut
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
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.jdbc.JdbcTestUtils
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.context.WebApplicationContext
import java.nio.file.Files
import java.net.URI
import java.nio.file.Paths


@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class HttpRequestTest {
    @LocalServerPort
    private val port = 0

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

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

    /*** TEST - NUEVAS FUNCIONALIDADES ***/

    @Test
    fun `Test para comprobar la funcionalidad de Google Safe Browsing`() {
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
        assertThat(response1.statusCode).isEqualTo(HttpStatus.OK)   //Comp. de 200 OK
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
        assertThat(respHeaders.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)    //Comp. de 400 BAD_REQUEST
        assertThat(respHeaders.body?.properties?.get("error")).isEqualTo("URI de destino no es alcanzable") //Comp. del mensaje de error
        // GET /{id}
        val response = restTemplate.getForEntity(target, String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) //Comp. de 400 BAD_REQUEST
        assertThat(response.body?.contains("redirection block")).isEqualTo(true) //Comp. del mensaje de error
    }

    @Test
    fun `Test para comprobar la funcionalidad de que una URL esta bloqueada`() {
        //val path = Paths.get("..\\repositories\\src\\main\\resources\\BLOCK_URL.txt") // URL Bloqueada
        var line = "https://www.amazon.es/"
        /*try {
            val sc = Scanner(File(path.toString()))
            line = sc.nextLine()
        } catch (e: IOException) {
            e.printStackTrace()
        }*/
        val respHeaders = shortUrl("https://www.amazon.es/")
        val target = respHeaders.headers.location
        require(target != null)
        // POST /api/link
        //assertThat(respHeaders.statusCode).isEqualTo(HttpStatus.FORBIDDEN) //Comp. de 403 FORBIDDEN
        // GET /{id}
        //val response = restTemplate.getForEntity(target, String::class.java)
        //assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN) //Comp. de 403 FORBIDDEN
    }

    @Test
    fun `Test para comprobar la funcionalidad de CSV --- Fichero que no es csv`() {
        val sendFile = fileToSend("noCsv.txt")
        val mock = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        mock.perform(
                multipart("/api/bulk")
                        .file(sendFile)).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    fun `Test para comprobar la funcionalidad de CSV --- Fichero csv vacio`() {
        val sendFile = fileToSend("emptyCSV.csv")
        val mock = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        mock.perform(
                multipart("/api/bulk")
                        .file(sendFile))
                .andExpect(MockMvcResultMatchers.status().isCreated)
                .andExpect(MockMvcResultMatchers.content().contentType("text/csv"))
                .andExpect(MockMvcResultMatchers.content().bytes(fileToBytes("emptyCSV.csv")))
    }

    @Test
    fun `Test para comprobar la funcionalidad de CSV --- Fichero csv formato no valido`() {
        val sendFile = fileToSend("InvalidCSV.csv")
        val mock = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        val resp = mock.perform(
                multipart("/api/bulk")
                        .file(sendFile))
                .andExpect(MockMvcResultMatchers.status().isCreated)
                .andExpect(MockMvcResultMatchers.content().contentType("text/csv"))
                .andReturn()
        assertThat(resp.response.contentAsString.contains("FALLO DE FORMATO"))
        assertThat(resp.response.getHeaderValue("Location").toString().isNotEmpty())
    }

    @Test
    fun `Test para comprobar la funcionalidad de CSV --- Fichero csv formato valido`() {
        val sendFile = fileToSend("ValidCSV.csv")
        val mock = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        val resp = mock.perform(
                multipart("/api/bulk")
                        .file(sendFile))
                .andExpect(MockMvcResultMatchers.status().isCreated)
                .andExpect(MockMvcResultMatchers.content().contentType("text/csv"))
                .andReturn()
        assertThat(resp.response.getHeaderValue("Location").toString().isNotEmpty())

        val a = resp.response.contentAsString
        println(a)
    }


    /*** ********************************************** ***/

    // Create a new MockMultipartFile with the given content.
    private fun fileToSend(name: String): MockMultipartFile {
        val path = Paths.get("src/test/resources/$name")
        val content = Files.readAllBytes(path)
        return MockMultipartFile("file", name, "text/plain", content)
    }

    // File to a bytes
    private fun fileToBytes(name: String): ByteArray {
        val path = Paths.get("src/test/resources/$name")
        return Files.readAllBytes(path)
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