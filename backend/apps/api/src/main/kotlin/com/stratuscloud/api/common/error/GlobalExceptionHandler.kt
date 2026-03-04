package com.stratuscloud.api.common.error

import com.stratuscloud.common.trace.TraceIdProvider
import com.stratuscloud.iam.exception.DuplicateResourceException
import com.stratuscloud.iam.exception.ResourceNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler(
    private val traceIdProvider: TraceIdProvider
) {

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(ex: ResourceNotFoundException): ResponseEntity<ErrorResponse> {
        return build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.message ?: "resource not found")
    }

    @ExceptionHandler(DuplicateResourceException::class)
    fun handleDuplicate(ex: DuplicateResourceException): ResponseEntity<ErrorResponse> {
        return build(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", ex.message ?: "duplicate resource")
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val details = ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "invalid") }
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "validation failed", details)
    }

    @ExceptionHandler(Exception::class)
    fun handleUnknown(ex: Exception): ResponseEntity<ErrorResponse> {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", ex.message ?: "internal error")
    }

    private fun build(
        status: HttpStatus,
        code: String,
        message: String,
        details: Map<String, Any?> = emptyMap()
    ): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(status).body(
            ErrorResponse(
                code = code,
                message = message,
                traceId = traceIdProvider.newTraceId(),
                details = details
            )
        )
    }
}
