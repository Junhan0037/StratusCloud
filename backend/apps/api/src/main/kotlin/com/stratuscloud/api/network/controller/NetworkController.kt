package com.stratuscloud.api.network.controller

import com.stratuscloud.api.common.security.ApiAuditRecorder
import com.stratuscloud.api.common.security.AuthContextHolder
import com.stratuscloud.api.common.security.AuthorizationFacade
import com.stratuscloud.api.network.dto.CreateRouteRequest
import com.stratuscloud.api.network.dto.CreateRouteTableAssociationRequest
import com.stratuscloud.api.network.dto.CreateRouteTableRequest
import com.stratuscloud.api.network.dto.CreateSecurityGroupRequest
import com.stratuscloud.api.network.dto.CreateSubnetRequest
import com.stratuscloud.api.network.dto.CreateVpcRequest
import com.stratuscloud.api.network.dto.ReplaceSecurityGroupRulesRequest
import com.stratuscloud.api.network.dto.RouteResponse
import com.stratuscloud.api.network.dto.RouteTableAssociationResponse
import com.stratuscloud.api.network.dto.RouteTableResponse
import com.stratuscloud.api.network.dto.SecurityGroupResponse
import com.stratuscloud.api.network.dto.SubnetResponse
import com.stratuscloud.api.network.dto.VpcResponse
import com.stratuscloud.iam.service.IamAction
import com.stratuscloud.network.service.NetworkService
import com.stratuscloud.network.service.SecurityGroupRuleDraft
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/v1/network")
class NetworkController(
    private val networkService: NetworkService,
    private val authorizationFacade: AuthorizationFacade,
    private val apiAuditRecorder: ApiAuditRecorder
) {

    @PostMapping("/vpcs")
    fun createVpc(@Valid @RequestBody request: CreateVpcRequest): ResponseEntity<VpcResponse> {
        val principal = AuthContextHolder.getRequired()
        authorizationFacade.authorize(
            principal = principal,
            tenantId = request.tenantId,
            projectId = request.projectId,
            action = IamAction.NETWORK_VPC_CREATE,
            resource = "project:${request.projectId}",
            resourceType = "VPC",
            resourceId = null,
            metadata = mapOf("name" to request.name, "cidrBlock" to request.cidrBlock)
        )
        val created = networkService.createVpc(
            tenantId = request.tenantId,
            projectId = request.projectId,
            name = request.name,
            cidrBlock = request.cidrBlock,
            actorId = principal.actorId
        )
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = created.tenantId,
            projectId = created.projectId,
            action = IamAction.NETWORK_VPC_CREATE,
            resourceType = "VPC",
            resourceId = created.id.toString(),
            metadata = mapOf("name" to created.name)
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(VpcResponse.from(created))
    }

    @GetMapping("/vpcs")
    fun listVpcs(
        @RequestParam tenantId: UUID,
        @RequestParam projectId: UUID
    ): ResponseEntity<List<VpcResponse>> {
        val principal = AuthContextHolder.getRequired()
        authorizationFacade.authorize(
            principal = principal,
            tenantId = tenantId,
            projectId = projectId,
            action = IamAction.NETWORK_VPC_LIST,
            resource = "project:$projectId",
            resourceType = "VPC",
            resourceId = null
        )
        return ResponseEntity.ok(networkService.listVpcs(tenantId, projectId).map(VpcResponse::from))
    }

    @GetMapping("/vpcs/{vpcId}")
    fun getVpc(@PathVariable vpcId: UUID): ResponseEntity<VpcResponse> {
        val principal = AuthContextHolder.getRequired()
        val vpc = networkService.getVpc(vpcId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = vpc.tenantId,
            projectId = vpc.projectId,
            action = IamAction.NETWORK_VPC_READ,
            resource = "network-vpc:$vpcId",
            resourceType = "VPC",
            resourceId = vpcId.toString()
        )
        return ResponseEntity.ok(VpcResponse.from(vpc))
    }

    @DeleteMapping("/vpcs/{vpcId}")
    fun deleteVpc(@PathVariable vpcId: UUID): ResponseEntity<Void> {
        val principal = AuthContextHolder.getRequired()
        val vpc = networkService.getVpc(vpcId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = vpc.tenantId,
            projectId = vpc.projectId,
            action = IamAction.NETWORK_VPC_DELETE,
            resource = "network-vpc:$vpcId",
            resourceType = "VPC",
            resourceId = vpcId.toString()
        )
        networkService.deleteVpc(vpcId)
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = vpc.tenantId,
            projectId = vpc.projectId,
            action = IamAction.NETWORK_VPC_DELETE,
            resourceType = "VPC",
            resourceId = vpcId.toString()
        )
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/subnets")
    fun createSubnet(@Valid @RequestBody request: CreateSubnetRequest): ResponseEntity<SubnetResponse> {
        val principal = AuthContextHolder.getRequired()
        authorizationFacade.authorize(
            principal = principal,
            tenantId = request.tenantId,
            projectId = request.projectId,
            action = IamAction.NETWORK_SUBNET_CREATE,
            resource = "network-vpc:${request.vpcId}",
            resourceType = "SUBNET",
            resourceId = null,
            metadata = mapOf("name" to request.name, "cidrBlock" to request.cidrBlock)
        )
        val (subnet, association) = networkService.createSubnet(
            tenantId = request.tenantId,
            projectId = request.projectId,
            vpcId = request.vpcId,
            name = request.name,
            cidrBlock = request.cidrBlock,
            availabilityZone = request.availabilityZone,
            actorId = principal.actorId
        )
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = subnet.tenantId,
            projectId = subnet.projectId,
            action = IamAction.NETWORK_SUBNET_CREATE,
            resourceType = "SUBNET",
            resourceId = subnet.id.toString(),
            metadata = mapOf("routeTableAssociationId" to association.id)
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(SubnetResponse.from(subnet, association))
    }

    @GetMapping("/subnets")
    fun listSubnets(
        @RequestParam tenantId: UUID,
        @RequestParam projectId: UUID,
        @RequestParam(required = false) vpcId: UUID?
    ): ResponseEntity<List<SubnetResponse>> {
        val principal = AuthContextHolder.getRequired()
        authorizationFacade.authorize(
            principal = principal,
            tenantId = tenantId,
            projectId = projectId,
            action = IamAction.NETWORK_SUBNET_LIST,
            resource = "project:$projectId",
            resourceType = "SUBNET",
            resourceId = null
        )
        return ResponseEntity.ok(
            networkService.listSubnets(tenantId, projectId, vpcId).map { subnet ->
                SubnetResponse.from(subnet, networkService.getAssociationBySubnet(requireNotNull(subnet.id)))
            }
        )
    }

    @GetMapping("/subnets/{subnetId}")
    fun getSubnet(@PathVariable subnetId: UUID): ResponseEntity<SubnetResponse> {
        val principal = AuthContextHolder.getRequired()
        val subnet = networkService.getSubnet(subnetId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = subnet.tenantId,
            projectId = subnet.projectId,
            action = IamAction.NETWORK_SUBNET_READ,
            resource = "network-subnet:$subnetId",
            resourceType = "SUBNET",
            resourceId = subnetId.toString()
        )
        return ResponseEntity.ok(
            SubnetResponse.from(subnet, networkService.getAssociationBySubnet(subnetId))
        )
    }

    @DeleteMapping("/subnets/{subnetId}")
    fun deleteSubnet(@PathVariable subnetId: UUID): ResponseEntity<Void> {
        val principal = AuthContextHolder.getRequired()
        val subnet = networkService.getSubnet(subnetId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = subnet.tenantId,
            projectId = subnet.projectId,
            action = IamAction.NETWORK_SUBNET_DELETE,
            resource = "network-subnet:$subnetId",
            resourceType = "SUBNET",
            resourceId = subnetId.toString()
        )
        networkService.deleteSubnet(subnetId)
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = subnet.tenantId,
            projectId = subnet.projectId,
            action = IamAction.NETWORK_SUBNET_DELETE,
            resourceType = "SUBNET",
            resourceId = subnetId.toString()
        )
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/route-tables")
    fun createRouteTable(@Valid @RequestBody request: CreateRouteTableRequest): ResponseEntity<RouteTableResponse> {
        val principal = AuthContextHolder.getRequired()
        authorizationFacade.authorize(
            principal = principal,
            tenantId = request.tenantId,
            projectId = request.projectId,
            action = IamAction.NETWORK_ROUTE_TABLE_CREATE,
            resource = "network-vpc:${request.vpcId}",
            resourceType = "ROUTE_TABLE",
            resourceId = null,
            metadata = mapOf("name" to request.name)
        )
        val routeTable = networkService.createRouteTable(
            tenantId = request.tenantId,
            projectId = request.projectId,
            vpcId = request.vpcId,
            name = request.name,
            actorId = principal.actorId
        )
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = routeTable.tenantId,
            projectId = routeTable.projectId,
            action = IamAction.NETWORK_ROUTE_TABLE_CREATE,
            resourceType = "ROUTE_TABLE",
            resourceId = routeTable.id.toString(),
            metadata = mapOf("name" to routeTable.name)
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(buildRouteTableResponse(routeTable))
    }

    @GetMapping("/route-tables")
    fun listRouteTables(@RequestParam vpcId: UUID): ResponseEntity<List<RouteTableResponse>> {
        val principal = AuthContextHolder.getRequired()
        val vpc = networkService.getVpc(vpcId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = vpc.tenantId,
            projectId = vpc.projectId,
            action = IamAction.NETWORK_ROUTE_TABLE_LIST,
            resource = "network-vpc:$vpcId",
            resourceType = "ROUTE_TABLE",
            resourceId = null
        )
        return ResponseEntity.ok(networkService.listRouteTables(vpcId).map(::buildRouteTableResponse))
    }

    @GetMapping("/route-tables/{routeTableId}")
    fun getRouteTable(@PathVariable routeTableId: UUID): ResponseEntity<RouteTableResponse> {
        val principal = AuthContextHolder.getRequired()
        val routeTable = networkService.getRouteTable(routeTableId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = routeTable.tenantId,
            projectId = routeTable.projectId,
            action = IamAction.NETWORK_ROUTE_TABLE_READ,
            resource = "network-route-table:$routeTableId",
            resourceType = "ROUTE_TABLE",
            resourceId = routeTableId.toString()
        )
        return ResponseEntity.ok(buildRouteTableResponse(routeTable))
    }

    @DeleteMapping("/route-tables/{routeTableId}")
    fun deleteRouteTable(@PathVariable routeTableId: UUID): ResponseEntity<Void> {
        val principal = AuthContextHolder.getRequired()
        val routeTable = networkService.getRouteTable(routeTableId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = routeTable.tenantId,
            projectId = routeTable.projectId,
            action = IamAction.NETWORK_ROUTE_TABLE_DELETE,
            resource = "network-route-table:$routeTableId",
            resourceType = "ROUTE_TABLE",
            resourceId = routeTableId.toString()
        )
        networkService.deleteRouteTable(routeTableId)
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = routeTable.tenantId,
            projectId = routeTable.projectId,
            action = IamAction.NETWORK_ROUTE_TABLE_DELETE,
            resourceType = "ROUTE_TABLE",
            resourceId = routeTableId.toString()
        )
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/route-tables/{routeTableId}/routes")
    fun createRoute(
        @PathVariable routeTableId: UUID,
        @Valid @RequestBody request: CreateRouteRequest
    ): ResponseEntity<RouteResponse> {
        val principal = AuthContextHolder.getRequired()
        val routeTable = networkService.getRouteTable(routeTableId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = routeTable.tenantId,
            projectId = routeTable.projectId,
            action = IamAction.NETWORK_ROUTE_CREATE,
            resource = "network-route-table:$routeTableId",
            resourceType = "ROUTE",
            resourceId = null,
            metadata = mapOf("destinationCidr" to request.destinationCidr, "targetType" to request.targetType.name)
        )
        val route = networkService.createRoute(
            routeTableId = routeTableId,
            destinationCidr = request.destinationCidr,
            targetType = request.targetType,
            actorId = principal.actorId
        )
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = route.tenantId,
            projectId = route.projectId,
            action = IamAction.NETWORK_ROUTE_CREATE,
            resourceType = "ROUTE",
            resourceId = route.id.toString(),
            metadata = mapOf("destinationCidr" to route.destinationCidr)
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(RouteResponse.from(route))
    }

    @DeleteMapping("/routes/{routeId}")
    fun deleteRoute(@PathVariable routeId: UUID): ResponseEntity<Void> {
        val principal = AuthContextHolder.getRequired()
        val route = networkService.getRoute(routeId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = route.tenantId,
            projectId = route.projectId,
            action = IamAction.NETWORK_ROUTE_DELETE,
            resource = "network-route:$routeId",
            resourceType = "ROUTE",
            resourceId = routeId.toString()
        )
        networkService.deleteRoute(routeId)
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = route.tenantId,
            projectId = route.projectId,
            action = IamAction.NETWORK_ROUTE_DELETE,
            resourceType = "ROUTE",
            resourceId = routeId.toString()
        )
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/route-tables/{routeTableId}/associations")
    fun associateSubnet(
        @PathVariable routeTableId: UUID,
        @Valid @RequestBody request: CreateRouteTableAssociationRequest
    ): ResponseEntity<RouteTableAssociationResponse> {
        val principal = AuthContextHolder.getRequired()
        val routeTable = networkService.getRouteTable(routeTableId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = routeTable.tenantId,
            projectId = routeTable.projectId,
            action = IamAction.NETWORK_ROUTE_TABLE_ASSOCIATE,
            resource = "network-route-table:$routeTableId",
            resourceType = "ROUTE_TABLE_ASSOCIATION",
            resourceId = null,
            metadata = mapOf("subnetId" to request.subnetId)
        )
        val association = networkService.associateSubnet(routeTableId, request.subnetId, principal.actorId)
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = association.tenantId,
            projectId = association.projectId,
            action = IamAction.NETWORK_ROUTE_TABLE_ASSOCIATE,
            resourceType = "ROUTE_TABLE_ASSOCIATION",
            resourceId = association.id.toString(),
            metadata = mapOf("subnetId" to association.subnetId, "routeTableId" to association.routeTableId)
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(RouteTableAssociationResponse.from(association))
    }

    @DeleteMapping("/route-table-associations/{associationId}")
    fun deleteAssociation(@PathVariable associationId: UUID): ResponseEntity<RouteTableAssociationResponse> {
        val principal = AuthContextHolder.getRequired()
        val association = networkService.getAssociation(associationId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = association.tenantId,
            projectId = association.projectId,
            action = IamAction.NETWORK_ROUTE_TABLE_DISASSOCIATE,
            resource = "network-route-table-association:$associationId",
            resourceType = "ROUTE_TABLE_ASSOCIATION",
            resourceId = associationId.toString()
        )
        val restoredAssociation = networkService.deleteAssociation(associationId, principal.actorId)
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = restoredAssociation.tenantId,
            projectId = restoredAssociation.projectId,
            action = IamAction.NETWORK_ROUTE_TABLE_DISASSOCIATE,
            resourceType = "ROUTE_TABLE_ASSOCIATION",
            resourceId = associationId.toString()
        )
        return ResponseEntity.ok(RouteTableAssociationResponse.from(restoredAssociation))
    }

    @PostMapping("/security-groups")
    fun createSecurityGroup(@Valid @RequestBody request: CreateSecurityGroupRequest): ResponseEntity<SecurityGroupResponse> {
        val principal = AuthContextHolder.getRequired()
        authorizationFacade.authorize(
            principal = principal,
            tenantId = request.tenantId,
            projectId = request.projectId,
            action = IamAction.NETWORK_SECURITY_GROUP_CREATE,
            resource = "network-vpc:${request.vpcId}",
            resourceType = "SECURITY_GROUP",
            resourceId = null,
            metadata = mapOf("name" to request.name)
        )
        val securityGroup = networkService.createSecurityGroup(
            tenantId = request.tenantId,
            projectId = request.projectId,
            vpcId = request.vpcId,
            name = request.name,
            description = request.description,
            actorId = principal.actorId
        )
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = securityGroup.tenantId,
            projectId = securityGroup.projectId,
            action = IamAction.NETWORK_SECURITY_GROUP_CREATE,
            resourceType = "SECURITY_GROUP",
            resourceId = securityGroup.id.toString(),
            metadata = mapOf("name" to securityGroup.name)
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(buildSecurityGroupResponse(securityGroup))
    }

    @GetMapping("/security-groups")
    fun listSecurityGroups(
        @RequestParam tenantId: UUID,
        @RequestParam projectId: UUID
    ): ResponseEntity<List<SecurityGroupResponse>> {
        val principal = AuthContextHolder.getRequired()
        authorizationFacade.authorize(
            principal = principal,
            tenantId = tenantId,
            projectId = projectId,
            action = IamAction.NETWORK_SECURITY_GROUP_LIST,
            resource = "project:$projectId",
            resourceType = "SECURITY_GROUP",
            resourceId = null
        )
        return ResponseEntity.ok(networkService.listSecurityGroups(tenantId, projectId).map(::buildSecurityGroupResponse))
    }

    @GetMapping("/security-groups/{securityGroupId}")
    fun getSecurityGroup(@PathVariable securityGroupId: UUID): ResponseEntity<SecurityGroupResponse> {
        val principal = AuthContextHolder.getRequired()
        val securityGroup = networkService.getSecurityGroup(securityGroupId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = securityGroup.tenantId,
            projectId = securityGroup.projectId,
            action = IamAction.NETWORK_SECURITY_GROUP_READ,
            resource = "network-security-group:$securityGroupId",
            resourceType = "SECURITY_GROUP",
            resourceId = securityGroupId.toString()
        )
        return ResponseEntity.ok(buildSecurityGroupResponse(securityGroup))
    }

    @DeleteMapping("/security-groups/{securityGroupId}")
    fun deleteSecurityGroup(@PathVariable securityGroupId: UUID): ResponseEntity<Void> {
        val principal = AuthContextHolder.getRequired()
        val securityGroup = networkService.getSecurityGroup(securityGroupId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = securityGroup.tenantId,
            projectId = securityGroup.projectId,
            action = IamAction.NETWORK_SECURITY_GROUP_DELETE,
            resource = "network-security-group:$securityGroupId",
            resourceType = "SECURITY_GROUP",
            resourceId = securityGroupId.toString()
        )
        networkService.deleteSecurityGroup(securityGroupId)
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = securityGroup.tenantId,
            projectId = securityGroup.projectId,
            action = IamAction.NETWORK_SECURITY_GROUP_DELETE,
            resourceType = "SECURITY_GROUP",
            resourceId = securityGroupId.toString()
        )
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/security-groups/{securityGroupId}/rules")
    fun replaceSecurityGroupRules(
        @PathVariable securityGroupId: UUID,
        @Valid @RequestBody request: ReplaceSecurityGroupRulesRequest
    ): ResponseEntity<SecurityGroupResponse> {
        val principal = AuthContextHolder.getRequired()
        val securityGroup = networkService.getSecurityGroup(securityGroupId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = securityGroup.tenantId,
            projectId = securityGroup.projectId,
            action = IamAction.NETWORK_SECURITY_GROUP_RULES_WRITE,
            resource = "network-security-group:$securityGroupId",
            resourceType = "SECURITY_GROUP",
            resourceId = securityGroupId.toString()
        )
        networkService.replaceSecurityGroupRules(
            securityGroupId = securityGroupId,
            rules = request.rules.map { rule ->
                SecurityGroupRuleDraft(
                    direction = rule.direction,
                    protocol = rule.protocol,
                    portRangeStart = rule.portRangeStart,
                    portRangeEnd = rule.portRangeEnd,
                    cidrBlock = rule.cidrBlock,
                    description = rule.description
                )
            },
            actorId = principal.actorId
        )
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = securityGroup.tenantId,
            projectId = securityGroup.projectId,
            action = IamAction.NETWORK_SECURITY_GROUP_RULES_WRITE,
            resourceType = "SECURITY_GROUP",
            resourceId = securityGroupId.toString(),
            metadata = mapOf("ruleCount" to request.rules.size)
        )
        return ResponseEntity.ok(buildSecurityGroupResponse(networkService.getSecurityGroup(securityGroupId)))
    }

    private fun buildRouteTableResponse(entity: com.stratuscloud.network.domain.NetworkRouteTableEntity): RouteTableResponse {
        return RouteTableResponse.from(
            entity = entity,
            routes = networkService.listRoutes(requireNotNull(entity.id)),
            associations = networkService.listAssociations(requireNotNull(entity.id))
        )
    }

    private fun buildSecurityGroupResponse(entity: com.stratuscloud.network.domain.NetworkSecurityGroupEntity): SecurityGroupResponse {
        return SecurityGroupResponse.from(
            entity = entity,
            rules = networkService.listSecurityGroupRules(requireNotNull(entity.id))
        )
    }
}
