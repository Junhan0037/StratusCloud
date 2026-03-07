package com.stratuscloud.iam.service

object IamAction {
    const val PROJECT_CREATE = "iam:project:create"
    const val PROJECT_READ = "iam:project:read"
    const val USER_CREATE = "iam:user:create"
    const val MEMBERSHIP_ADD = "iam:membership:add"
    const val MEMBERSHIP_ROLE_UPDATE = "iam:membership:role:update"
    const val POLICY_CREATE = "iam:policy:create"
    const val POLICY_LIST = "iam:policy:list"
    const val ROLE_POLICY_BIND = "iam:role-policy:bind"
    const val API_KEY_CREATE = "iam:api-key:create"
    const val API_KEY_REVOKE = "iam:api-key:revoke"
    const val API_KEY_LIST = "iam:api-key:list"
    const val SECRET_CREATE = "iam:secret:create"
    const val SECRET_LIST = "iam:secret:list"
    const val SECRET_READ = "iam:secret:read"
    const val SECRET_ROTATE = "iam:secret:rotate"
    const val SECRET_VERSION_REVOKE = "iam:secret-version:revoke"
    const val AUDIT_LOG_LIST = "iam:audit-log:list"
}
