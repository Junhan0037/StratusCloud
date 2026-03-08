package com.stratuscloud.api.network.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.stratuscloud.network.domain.NetworkRouteEntity
import com.stratuscloud.network.domain.NetworkRouteTableAssociationEntity
import com.stratuscloud.network.domain.NetworkRouteTableEntity
import com.stratuscloud.network.domain.NetworkRouteTargetType
import com.stratuscloud.network.domain.NetworkRuleDirection
import com.stratuscloud.network.domain.NetworkRuleProtocol
import com.stratuscloud.network.domain.NetworkSecurityGroupEntity
import com.stratuscloud.network.domain.NetworkSecurityGroupRuleEntity
import com.stratuscloud.network.domain.NetworkSubnetEntity
import com.stratuscloud.network.domain.NetworkVpcEntity
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.UUID

data class CreateVpcRequest(
    val tenantId: UUID,
    val projectId: UUID,
    @field:NotBlank
    @field:Size(min = 2, max = 100)
    val name: String,
    @field:NotBlank
    @field:Size(min = 9, max = 32)
    val cidrBlock: String
)

data class VpcResponse(
    val id: UUID,
    val tenantId: UUID,
    val projectId: UUID,
    val name: String,
    val cidrBlock: String,
    val defaultRouteTableId: UUID,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(entity: NetworkVpcEntity): VpcResponse {
            return VpcResponse(
                id = entity.id ?: error("vpc id is null"),
                tenantId = entity.tenantId,
                projectId = entity.projectId,
                name = entity.name,
                cidrBlock = entity.cidrBlock,
                defaultRouteTableId = entity.defaultRouteTableId ?: error("default route table id is null"),
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }
}

data class CreateSubnetRequest(
    val tenantId: UUID,
    val projectId: UUID,
    val vpcId: UUID,
    @field:NotBlank
    @field:Size(min = 2, max = 100)
    val name: String,
    @field:NotBlank
    @field:Size(min = 9, max = 32)
    val cidrBlock: String,
    @field:NotBlank
    @field:Size(min = 2, max = 40)
    val availabilityZone: String
)

data class SubnetResponse(
    val id: UUID,
    val tenantId: UUID,
    val projectId: UUID,
    val vpcId: UUID,
    val name: String,
    val cidrBlock: String,
    val availabilityZone: String,
    val routeTableId: UUID?,
    val routeTableAssociationId: UUID?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(entity: NetworkSubnetEntity, association: NetworkRouteTableAssociationEntity?): SubnetResponse {
            return SubnetResponse(
                id = entity.id ?: error("subnet id is null"),
                tenantId = entity.tenantId,
                projectId = entity.projectId,
                vpcId = entity.vpcId,
                name = entity.name,
                cidrBlock = entity.cidrBlock,
                availabilityZone = entity.availabilityZone,
                routeTableId = association?.routeTableId,
                routeTableAssociationId = association?.id,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }
}

data class CreateRouteTableRequest(
    val tenantId: UUID,
    val projectId: UUID,
    val vpcId: UUID,
    @field:NotBlank
    @field:Size(min = 2, max = 100)
    val name: String
)

data class CreateRouteRequest(
    @field:NotBlank
    @field:Size(min = 9, max = 32)
    val destinationCidr: String,
    val targetType: NetworkRouteTargetType
)

data class RouteResponse(
    val id: UUID,
    val destinationCidr: String,
    val targetType: NetworkRouteTargetType,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(entity: NetworkRouteEntity): RouteResponse {
            return RouteResponse(
                id = entity.id ?: error("route id is null"),
                destinationCidr = entity.destinationCidr,
                targetType = entity.targetType,
                createdAt = entity.createdAt
            )
        }
    }
}

data class CreateRouteTableAssociationRequest(
    val subnetId: UUID
)

data class RouteTableAssociationResponse(
    val id: UUID,
    val routeTableId: UUID,
    val subnetId: UUID,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(entity: NetworkRouteTableAssociationEntity): RouteTableAssociationResponse {
            return RouteTableAssociationResponse(
                id = entity.id ?: error("route table association id is null"),
                routeTableId = entity.routeTableId,
                subnetId = entity.subnetId,
                createdAt = entity.createdAt
            )
        }
    }
}

data class RouteTableResponse(
    val id: UUID,
    val tenantId: UUID,
    val projectId: UUID,
    val vpcId: UUID,
    val name: String,
    @field:JsonProperty("isDefault")
    val isDefault: Boolean,
    val routes: List<RouteResponse>,
    val associations: List<RouteTableAssociationResponse>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(
            entity: NetworkRouteTableEntity,
            routes: List<NetworkRouteEntity>,
            associations: List<NetworkRouteTableAssociationEntity>
        ): RouteTableResponse {
            return RouteTableResponse(
                id = entity.id ?: error("route table id is null"),
                tenantId = entity.tenantId,
                projectId = entity.projectId,
                vpcId = entity.vpcId,
                name = entity.name,
                isDefault = entity.isDefault,
                routes = routes.map(RouteResponse::from),
                associations = associations.map(RouteTableAssociationResponse::from),
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }
}

data class CreateSecurityGroupRequest(
    val tenantId: UUID,
    val projectId: UUID,
    val vpcId: UUID,
    @field:NotBlank
    @field:Size(min = 2, max = 100)
    val name: String,
    @field:Size(max = 300)
    val description: String?
)

data class ReplaceSecurityGroupRulesRequest(
    @field:Valid
    val rules: List<SecurityGroupRuleInput>
)

data class SecurityGroupRuleInput(
    val direction: NetworkRuleDirection,
    val protocol: NetworkRuleProtocol,
    val portRangeStart: Int?,
    val portRangeEnd: Int?,
    @field:NotBlank
    @field:Size(min = 9, max = 32)
    val cidrBlock: String,
    @field:Size(max = 300)
    val description: String?
)

data class SecurityGroupRuleResponse(
    val id: UUID,
    val direction: NetworkRuleDirection,
    val protocol: NetworkRuleProtocol,
    val portRangeStart: Int?,
    val portRangeEnd: Int?,
    val cidrBlock: String,
    val description: String?,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(entity: NetworkSecurityGroupRuleEntity): SecurityGroupRuleResponse {
            return SecurityGroupRuleResponse(
                id = entity.id ?: error("security group rule id is null"),
                direction = entity.direction,
                protocol = entity.protocol,
                portRangeStart = entity.portRangeStart,
                portRangeEnd = entity.portRangeEnd,
                cidrBlock = entity.cidrBlock,
                description = entity.description,
                createdAt = entity.createdAt
            )
        }
    }
}

data class SecurityGroupResponse(
    val id: UUID,
    val tenantId: UUID,
    val projectId: UUID,
    val vpcId: UUID,
    val name: String,
    val description: String?,
    val rules: List<SecurityGroupRuleResponse>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(entity: NetworkSecurityGroupEntity, rules: List<NetworkSecurityGroupRuleEntity>): SecurityGroupResponse {
            return SecurityGroupResponse(
                id = entity.id ?: error("security group id is null"),
                tenantId = entity.tenantId,
                projectId = entity.projectId,
                vpcId = entity.vpcId,
                name = entity.name,
                description = entity.description,
                rules = rules.map(SecurityGroupRuleResponse::from),
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }
}
