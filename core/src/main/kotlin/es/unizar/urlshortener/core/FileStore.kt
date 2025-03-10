package es.unizar.urlshortener.core

import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils
import java.io.FileNotFoundException
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicInteger

/**
 * This interface [FileStore] is used for everything related
 * to the files of the CSV functionality.
 */
interface FileStore {
    fun init()
    fun generateName(): String
    fun newFile(filename: String): Resource
    fun overWriteFile(filename: String, lines: List<String>)
    fun deleteFile(filename: String)
    fun deleteAll()
}

/**
 * Service of [FileStore].
 */
@Service
class FileStorageImpl: FileStore {
    private var rootLocation = Paths.get("filestorage")

    private var numFiles = AtomicInteger(0)

    override fun init() {
        Files.createDirectory(rootLocation)
    }

    override fun generateName(): String {
        return "temp${numFiles.incrementAndGet()}"
    }


    override fun newFile(filename: String): Resource {
        val path = Files.createFile(this.rootLocation.resolve(filename))
        val resource = UrlResource(path.toUri())

        if (resource.exists() || resource.isReadable) {
            return resource
        } else {
            throw FileNotFoundException()
        }
    }


    override fun overWriteFile(filename: String, lines: List<String>) {
        PrintWriter(rootLocation.resolve(filename).toString()).use {
            for (line in lines) {
                it.println(line)
            }
        }
    }

    override fun deleteFile(filename: String) {
        Files.deleteIfExists(rootLocation.resolve(filename))
    }

    override fun deleteAll() {
        FileSystemUtils.deleteRecursively(rootLocation.toFile()) 
    }
}