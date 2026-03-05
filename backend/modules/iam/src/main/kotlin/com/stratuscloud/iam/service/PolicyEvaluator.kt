package com.stratuscloud.iam.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component

@Component
class PolicyEvaluator(
    private val objectMapper: ObjectMapper
) {

    fun isActionAllowed(policyDocument: String, action: String, resource: String): Boolean {
        return runCatching {
            val root = objectMapper.readTree(policyDocument)
            val statements = root.path("statements")
            if (!statements.isArray) {
                return false
            }

            val denyMatched = statements.any { statement ->
                isMatched(statement, "DENY", action, resource)
            }
            if (denyMatched) {
                return false
            }

            statements.any { statement ->
                isMatched(statement, "ALLOW", action, resource)
            }
        }.getOrDefault(false)
    }

    fun validateDocument(policyDocument: String): Boolean {
        return runCatching {
            val root = objectMapper.readTree(policyDocument)
            val statements = root.path("statements")
            statements.isArray && statements.size() > 0
        }.getOrDefault(false)
    }

    private fun isMatched(
        statement: JsonNode,
        expectedEffect: String,
        action: String,
        resource: String
    ): Boolean {
        val effect = statement.path("effect").asText("").uppercase()
        if (effect != expectedEffect) {
            return false
        }
        val actions = statement.path("actions")
        val resources = statement.path("resources")
        if (!actions.isArray || !resources.isArray) {
            return false
        }

        val actionMatched = actions.any { pattern ->
            wildcardMatches(pattern.asText(""), action)
        }
        val resourceMatched = resources.any { pattern ->
            wildcardMatches(pattern.asText(""), resource)
        }
        return actionMatched && resourceMatched
    }

    private fun wildcardMatches(pattern: String, value: String): Boolean {
        if (pattern == "*") {
            return true
        }
        if (pattern.endsWith("*")) {
            return value.startsWith(pattern.removeSuffix("*"))
        }
        return pattern == value
    }
}
