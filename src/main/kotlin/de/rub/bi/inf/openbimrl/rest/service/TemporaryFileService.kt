package de.rub.bi.inf.openbimrl.rest.service

import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
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

    fun saveToFile(s: String, fileSuffix: String): UUID {
        val id = UUID.randomUUID()
        val file = tempDir.resolve("${id}.${fileSuffix}").toFile()
        file.writeText(s)
        return id
    }
}
