package de.rub.bi.inf.openbimrl.rest.service

import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.createTempDirectory
import kotlin.io.path.listDirectoryEntries


@Service
class TemporaryFileService {
    private final val tempDir: Path = createTempDirectory();
    final val files: List<Path>
        get() = tempDir.listDirectoryEntries()

    fun filesWithGlob(glob: String): List<Path> {
        return tempDir.listDirectoryEntries(glob)
    }

    fun saveToFile(s: String, fileSuffix: String, metadata: Map<String, String>? = null): UUID {
        val id = UUID.randomUUID()
        val file = tempDir.resolve("${id}.${fileSuffix}").toFile()
        file.writeText(s)
        val filePath = file.toPath()
        metadata?.forEach { (k, v) ->
            Files.setAttribute(filePath, "user:$k", v.toByteArray())
        }
        return id
    }

    final inline fun <reified T> getFileMetadata(path: Path, attributeName: String): T {
        return Files.getAttribute(path, "user:$attributeName") as T
    }
}
