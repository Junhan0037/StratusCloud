package com.stratuscloud.storage.service

import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.UUID

@Component
class LocalObjectStorage(
    private val storageProperties: StorageProperties
) {

    fun writeObject(objectId: UUID, content: ByteArray) {
        val path = resolvePath(objectId)
        Files.createDirectories(path.parent)
        Files.write(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
    }

    fun readObject(objectId: UUID): ByteArray {
        return Files.readAllBytes(resolvePath(objectId))
    }

    fun deleteObject(objectId: UUID) {
        Files.deleteIfExists(resolvePath(objectId))
    }

    private fun resolvePath(objectId: UUID): Path {
        val root = Path.of(storageProperties.rootPath).toAbsolutePath().normalize()
        return root.resolve(objectId.toString().take(2)).resolve("$objectId.bin")
    }
}
