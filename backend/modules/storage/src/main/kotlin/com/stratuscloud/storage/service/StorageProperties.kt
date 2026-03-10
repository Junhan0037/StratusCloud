package com.stratuscloud.storage.service

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "storage")
class StorageProperties {
    var rootPath: String = "build/storage-data"
}
