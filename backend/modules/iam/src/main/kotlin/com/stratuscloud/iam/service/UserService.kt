package com.stratuscloud.iam.service

import com.stratuscloud.iam.domain.UserEntity
import com.stratuscloud.iam.exception.DuplicateResourceException
import com.stratuscloud.iam.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository
) {

    @Transactional
    fun createUser(email: String, displayName: String, actorId: UUID): UserEntity {
        val normalizedEmail = email.trim().lowercase()
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw DuplicateResourceException("user email already exists")
        }

        return userRepository.save(
            UserEntity(
                email = normalizedEmail,
                displayName = displayName.trim(),
                createdBy = actorId.toString()
            )
        )
    }
}
