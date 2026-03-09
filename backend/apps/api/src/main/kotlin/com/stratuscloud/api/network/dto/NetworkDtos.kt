package com.stratuscloud.api.network.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.stratuscloud.network.domain.NetworkDnsRecordEntity
import com.stratuscloud.network.domain.NetworkDnsRecordType
import com.stratuscloud.network.domain.NetworkDnsTargetType
import com.stratuscloud.network.domain.NetworkElasticIpAllocationStatus
import com.stratuscloud.network.domain.NetworkElasticIpAttachmentType
import com.stratuscloud.network.domain.NetworkElasticIpEntity
import com.stratuscloud.network.domain.NetworkLoadBalancerEntity
import com.stratuscloud.network.domain.NetworkLoadBalancerListenerEntity
import com.stratuscloud.network.domain.NetworkLoadBalancerProtocol
import com.stratuscloud.network.domain.NetworkLoadBalancerRuleEntity
import com.stratuscloud.network.domain.NetworkLoadBalancerScheme
import com.stratuscloud.network.domain.NetworkLoadBalancerType
import com.stratuscloud.network.domain.NetworkNatGatewayEntity
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
    val targetType: NetworkRouteTargetType,
    val targetResourceId: UUID? = null
)

data class RouteResponse(
    val id: UUID,
    val destinationCidr: String,
    val targetType: NetworkRouteTargetType,
    val targetResourceId: UUID?,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(entity: NetworkRouteEntity): RouteResponse {
            return RouteResponse(
                id = entity.id ?: error("route id is null"),
                destinationCidr = entity.destinationCidr,
                targetType = entity.targetType,
                targetResourceId = entity.targetResourceId,
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

data class CreateLoadBalancerRequest(
    val tenantId: UUID,
    val projectId: UUID,
    val vpcId: UUID,
    @field:NotBlank
    @field:Size(min = 2, max = 100)
    val name: String,
    val type: NetworkLoadBalancerType,
    val scheme: NetworkLoadBalancerScheme
)

data class CreateLoadBalancerListenerRequest(
    val protocol: NetworkLoadBalancerProtocol,
    val port: Int,
    val defaultTargetSubnetId: UUID
)

data class CreateLoadBalancerRuleRequest(
    val priority: Int,
    @field:NotBlank
    @field:Size(min = 2, max = 200)
    val pathPattern: String,
    val targetSubnetId: UUID
)

data class LoadBalancerRuleResponse(
    val id: UUID,
    val priority: Int,
    val pathPattern: String,
    val targetSubnetId: UUID,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(entity: NetworkLoadBalancerRuleEntity): LoadBalancerRuleResponse {
            return LoadBalancerRuleResponse(
                id = entity.id ?: error("load balancer rule id is null"),
                priority = entity.priority,
                pathPattern = entity.pathPattern,
                targetSubnetId = entity.targetSubnetId,
                createdAt = entity.createdAt
            )
        }
    }
}

data class LoadBalancerListenerResponse(
    val id: UUID,
    val protocol: NetworkLoadBalancerProtocol,
    val port: Int,
    val defaultTargetSubnetId: UUID,
    val rules: List<LoadBalancerRuleResponse>,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(
            entity: NetworkLoadBalancerListenerEntity,
            rules: List<NetworkLoadBalancerRuleEntity>
        ): LoadBalancerListenerResponse {
            return LoadBalancerListenerResponse(
                id = entity.id ?: error("load balancer listener id is null"),
                protocol = entity.protocol,
                port = entity.port,
                defaultTargetSubnetId = entity.defaultTargetSubnetId,
                rules = rules.map(LoadBalancerRuleResponse::from),
                createdAt = entity.createdAt
            )
        }
    }
}

data class LoadBalancerResponse(
    val id: UUID,
    val tenantId: UUID,
    val projectId: UUID,
    val vpcId: UUID,
    val name: String,
    val type: NetworkLoadBalancerType,
    val scheme: NetworkLoadBalancerScheme,
    val listeners: List<LoadBalancerListenerResponse>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(
            entity: NetworkLoadBalancerEntity,
            listeners: List<LoadBalancerListenerResponse>
        ): LoadBalancerResponse {
            return LoadBalancerResponse(
                id = entity.id ?: error("load balancer id is null"),
                tenantId = entity.tenantId,
                projectId = entity.projectId,
                vpcId = entity.vpcId,
                name = entity.name,
                type = entity.type,
                scheme = entity.scheme,
                listeners = listeners,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }
}

data class CreateElasticIpRequest(
    val tenantId: UUID,
    val projectId: UUID,
    @field:NotBlank
    @field:Size(min = 2, max = 100)
    val name: String
)

data class AttachElasticIpRequest(
    val targetType: NetworkElasticIpAttachmentType,
    val targetId: UUID
)

data class ElasticIpAttachmentResponse(
    val targetType: NetworkElasticIpAttachmentType,
    val targetId: UUID
)

data class ElasticIpResponse(
    val id: UUID,
    val tenantId: UUID,
    val projectId: UUID,
    val name: String,
    val publicIp: String,
    val allocationStatus: NetworkElasticIpAllocationStatus,
    val attachment: ElasticIpAttachmentResponse?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(entity: NetworkElasticIpEntity): ElasticIpResponse {
            return ElasticIpResponse(
                id = entity.id ?: error("elastic ip id is null"),
                tenantId = entity.tenantId,
                projectId = entity.projectId,
                name = entity.name,
                publicIp = entity.publicIp,
                allocationStatus = entity.allocationStatus,
                attachment = if (entity.attachmentTargetType != null && entity.attachmentTargetId != null) {
                    ElasticIpAttachmentResponse(
                        targetType = entity.attachmentTargetType ?: error("elastic ip attachment type is null"),
                        targetId = entity.attachmentTargetId ?: error("elastic ip attachment target id is null")
                    )
                } else {
                    null
                },
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }
}

data class CreateNatGatewayRequest(
    val tenantId: UUID,
    val projectId: UUID,
    val vpcId: UUID,
    val subnetId: UUID,
    @field:NotBlank
    @field:Size(min = 2, max = 100)
    val name: String
)

data class NatGatewayResponse(
    val id: UUID,
    val tenantId: UUID,
    val projectId: UUID,
    val vpcId: UUID,
    val subnetId: UUID,
    val name: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(entity: NetworkNatGatewayEntity): NatGatewayResponse {
            return NatGatewayResponse(
                id = entity.id ?: error("nat gateway id is null"),
                tenantId = entity.tenantId,
                projectId = entity.projectId,
                vpcId = entity.vpcId,
                subnetId = entity.subnetId,
                name = entity.name,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }
}

data class CreateDnsRecordRequest(
    val tenantId: UUID,
    val projectId: UUID,
    @field:NotBlank
    @field:Size(min = 2, max = 200)
    val name: String,
    val targetType: NetworkDnsTargetType,
    val targetId: UUID
)

data class DnsRecordResponse(
    val id: UUID,
    val tenantId: UUID,
    val projectId: UUID,
    val name: String,
    val recordType: NetworkDnsRecordType,
    val targetType: NetworkDnsTargetType,
    val targetId: UUID,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(entity: NetworkDnsRecordEntity): DnsRecordResponse {
            return DnsRecordResponse(
                id = entity.id ?: error("dns record id is null"),
                tenantId = entity.tenantId,
                projectId = entity.projectId,
                name = entity.name,
                recordType = entity.recordType,
                targetType = entity.targetType,
                targetId = entity.targetId,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }
}
