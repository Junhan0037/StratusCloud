package com.stratuscloud.data.domain

enum class ManagedDatabaseStatus {
    PROVISIONING,
    AVAILABLE,
    BACKING_UP,
    RESTORING,
    FAILED,
    DELETED
}
