package com.stratuscloud.api.storage.controller

import com.stratuscloud.api.storage.dto.StorageObjectResponse
import com.stratuscloud.storage.service.StorageService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/v1/storage/presigned")
class StoragePresignedController(
    private val storageService: StorageService
) {

    @PutMapping("/upload/{tokenId}")
    fun uploadObject(
        @PathVariable tokenId: UUID,
        @RequestBody body: ByteArray,
        request: HttpServletRequest
    ): ResponseEntity<StorageObjectResponse> {
        val uploaded = storageService.uploadObject(tokenId, body, request.contentType)
        return ResponseEntity.status(201).body(StorageObjectResponse.from(uploaded))
    }

    @GetMapping("/download/{tokenId}")
    fun downloadObject(@PathVariable tokenId: UUID): ResponseEntity<ByteArray> {
        val (entity, body) = storageService.downloadObject(tokenId)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(entity.contentType))
            .body(body)
    }
}
