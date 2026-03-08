package com.stratuscloud.network.service

import com.stratuscloud.iam.exception.BadRequestException
import java.net.InetAddress

data class NetworkCidr(
    val original: String,
    val prefixLength: Int,
    val networkValue: Long,
    val start: Long,
    val end: Long
) {
    fun contains(other: NetworkCidr): Boolean = other.start >= start && other.end <= end

    fun overlaps(other: NetworkCidr): Boolean = start <= other.end && other.start <= end

    companion object {
        fun parse(value: String): NetworkCidr {
            val trimmed = value.trim()
            val parts = trimmed.split("/")
            if (parts.size != 2) {
                throw BadRequestException("invalid cidr block: $value")
            }
            val prefixLength = parts[1].toIntOrNull()
                ?: throw BadRequestException("invalid cidr prefix: $value")
            if (prefixLength !in 0..32) {
                throw BadRequestException("cidr prefix out of range: $value")
            }

            val bytes = InetAddress.getByName(parts[0]).address
            if (bytes.size != 4) {
                throw BadRequestException("only IPv4 cidr is supported: $value")
            }
            val ipValue = bytes.fold(0L) { acc, byte -> (acc shl 8) or (byte.toInt() and 0xff).toLong() }
            val mask = if (prefixLength == 0) 0L else (-1L shl (32 - prefixLength)) and 0xffffffffL
            val networkValue = ipValue and mask
            if (ipValue != networkValue) {
                throw BadRequestException("cidr must use network address: $value")
            }
            val hostMask = 0xffffffffL xor mask
            return NetworkCidr(
                original = trimmed,
                prefixLength = prefixLength,
                networkValue = networkValue,
                start = networkValue,
                end = networkValue or hostMask
            )
        }
    }
}
