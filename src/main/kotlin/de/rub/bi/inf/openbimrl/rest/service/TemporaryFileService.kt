package de.rub.bi.inf.openbimrl.rest.service

import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID
import kotlin.io.path.createTempDirectory



@Service
class TemporaryFileService {
    private final val tempDir: Path = createTempDirectory();

    fun saveToFile(s: String): File {
        val file = tempDir.resolve(UUID.randomUUID().toString()).toFile()
        file.writeText(s)
        return file
    }
}