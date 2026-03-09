package com.stratuscloud.network.service

import com.stratuscloud.iam.exception.BadRequestException
import com.stratuscloud.iam.exception.DuplicateResourceException
import com.stratuscloud.iam.exception.ResourceNotFoundException
import com.stratuscloud.iam.repository.ProjectRepository
import com.stratuscloud.network.domain.NetworkRouteEntity
import com.stratuscloud.network.domain.NetworkRouteTableAssociationEntity
import com.stratuscloud.network.domain.NetworkRouteTableEntity
import com.stratuscloud.network.domain.NetworkRouteTargetType
import com.stratuscloud.network.domain.NetworkRuleProtocol
import com.stratuscloud.network.domain.NetworkSecurityGroupEntity
import com.stratuscloud.network.domain.NetworkSecurityGroupRuleEntity
import com.stratuscloud.network.domain.NetworkSubnetEntity
import com.stratuscloud.network.domain.NetworkVpcEntity
import com.stratuscloud.network.repository.NetworkNatGatewayRepository
import com.stratuscloud.network.repository.NetworkRouteRepository
import com.stratuscloud.network.repository.NetworkRouteTableAssociationRepository
import com.stratuscloud.network.repository.NetworkRouteTableRepository
import com.stratuscloud.network.repository.NetworkSecurityGroupRepository
import com.stratuscloud.network.repository.NetworkSecurityGroupRuleRepository
import com.stratuscloud.network.repository.NetworkSubnetRepository
import com.stratuscloud.network.repository.NetworkVpcRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class NetworkService(
    private val projectRepository: ProjectRepository,
    private val vpcRepository: NetworkVpcRepository,
    private val subnetRepository: NetworkSubnetRepository,
    private val routeTableRepository: NetworkRouteTableRepository,
    private val routeRepository: NetworkRouteRepository,
    private val routeTableAssociationRepository: NetworkRouteTableAssociationRepository,
    private val securityGroupRepository: NetworkSecurityGroupRepository,
    private val securityGroupRuleRepository: NetworkSecurityGroupRuleRepository,
    private val natGatewayRepository: NetworkNatGatewayRepository
) {

    @Transactional
    fun createVpc(
        tenantId: UUID,
        projectId: UUID,
        name: String,
        cidrBlock: String,
        actorId: UUID
    ): NetworkVpcEntity {
        validateProjectScope(tenantId, projectId)
        val normalizedName = name.trim()
        if (vpcRepository.existsByProjectIdAndName(projectId, normalizedName)) {
            throw DuplicateResourceException("vpc already exists: $normalizedName")
        }
        val normalizedCidr = NetworkCidr.parse(cidrBlock).original
        val vpc = vpcRepository.save(
            NetworkVpcEntity(
                tenantId = tenantId,
                projectId = projectId,
                name = normalizedName,
                cidrBlock = normalizedCidr,
                createdBy = actorId.toString()
            )
        )
        val defaultRouteTable = routeTableRepository.save(
            NetworkRouteTableEntity(
                tenantId = tenantId,
                projectId = projectId,
                vpcId = requireNotNull(vpc.id),
                name = "$normalizedName-default",
                isDefault = true,
                createdBy = actorId.toString()
            )
        )
        routeRepository.save(
            NetworkRouteEntity(
                tenantId = tenantId,
                projectId = projectId,
                vpcId = requireNotNull(vpc.id),
                routeTableId = requireNotNull(defaultRouteTable.id),
                destinationCidr = normalizedCidr,
                targetType = NetworkRouteTargetType.LOCAL,
                createdBy = actorId.toString()
            )
        )
        vpc.defaultRouteTableId = defaultRouteTable.id
        return vpcRepository.save(vpc)
    }

    @Transactional(readOnly = true)
    fun listVpcs(tenantId: UUID, projectId: UUID): List<NetworkVpcEntity> {
        return vpcRepository.findAllByTenantIdAndProjectIdOrderByCreatedAtDesc(tenantId, projectId)
    }

    @Transactional(readOnly = true)
    fun getVpc(vpcId: UUID): NetworkVpcEntity {
        return vpcRepository.findById(vpcId)
            .orElseThrow { ResourceNotFoundException("vpc not found: $vpcId") }
    }

    @Transactional
    fun deleteVpc(vpcId: UUID) {
        val vpc = getVpc(vpcId)
        if (subnetRepository.existsByVpcId(vpcId)) {
            throw BadRequestException("cannot delete vpc with subnets: $vpcId")
        }
        if (securityGroupRepository.existsByVpcId(vpcId)) {
            throw BadRequestException("cannot delete vpc with security groups: $vpcId")
        }
        val routeTables = routeTableRepository.findAllByVpcIdOrderByCreatedAtDesc(vpcId)
        if (routeTables.any { !it.isDefault }) {
            throw BadRequestException("cannot delete vpc with custom route tables: $vpcId")
        }
        routeTables.forEach { routeTable ->
            routeRepository.deleteAllByRouteTableId(requireNotNull(routeTable.id))
            routeTableAssociationRepository.findAllByRouteTableIdOrderByCreatedAtAsc(requireNotNull(routeTable.id))
                .forEach { routeTableAssociationRepository.delete(it) }
            routeTableRepository.delete(routeTable)
        }
        vpcRepository.delete(vpc)
    }

    @Transactional
    fun createSubnet(
        tenantId: UUID,
        projectId: UUID,
        vpcId: UUID,
        name: String,
        cidrBlock: String,
        availabilityZone: String,
        actorId: UUID
    ): Pair<NetworkSubnetEntity, NetworkRouteTableAssociationEntity> {
        validateProjectScope(tenantId, projectId)
        val vpc = getVpc(vpcId)
        requireScope(vpc.tenantId == tenantId && vpc.projectId == projectId, "vpc scope mismatch: $vpcId")
        val normalizedName = name.trim()
        if (subnetRepository.existsByVpcIdAndName(vpcId, normalizedName)) {
            throw DuplicateResourceException("subnet already exists: $normalizedName")
        }

        val vpcCidr = NetworkCidr.parse(vpc.cidrBlock)
        val subnetCidr = NetworkCidr.parse(cidrBlock)
        if (!vpcCidr.contains(subnetCidr)) {
            throw BadRequestException("subnet cidr must be contained in vpc cidr")
        }
        val existingSubnets = subnetRepository.findAllByVpcIdOrderByCreatedAtDesc(vpcId)
        if (existingSubnets.any { NetworkCidr.parse(it.cidrBlock).overlaps(subnetCidr) }) {
            throw BadRequestException("subnet cidr overlaps existing subnet in vpc")
        }

        val subnet = subnetRepository.save(
            NetworkSubnetEntity(
                tenantId = tenantId,
                projectId = projectId,
                vpcId = vpcId,
                name = normalizedName,
                cidrBlock = subnetCidr.original,
                availabilityZone = availabilityZone.trim(),
                createdBy = actorId.toString()
            )
        )
        val defaultRouteTableId = vpc.defaultRouteTableId
            ?: throw ResourceNotFoundException("default route table missing for vpc: $vpcId")
        val association = replaceAssociation(
            subnetId = requireNotNull(subnet.id),
            routeTableId = defaultRouteTableId,
            actorId = actorId
        )
        return subnet to association
    }

    @Transactional(readOnly = true)
    fun listSubnets(tenantId: UUID, projectId: UUID, vpcId: UUID?): List<NetworkSubnetEntity> {
        return if (vpcId != null) {
            subnetRepository.findAllByVpcIdOrderByCreatedAtDesc(vpcId)
        } else {
            subnetRepository.findAllByTenantIdAndProjectIdOrderByCreatedAtDesc(tenantId, projectId)
        }
    }

    @Transactional(readOnly = true)
    fun getSubnet(subnetId: UUID): NetworkSubnetEntity {
        return subnetRepository.findById(subnetId)
            .orElseThrow { ResourceNotFoundException("subnet not found: $subnetId") }
    }

    @Transactional
    fun deleteSubnet(subnetId: UUID) {
        routeTableAssociationRepository.deleteBySubnetId(subnetId)
        val subnet = getSubnet(subnetId)
        subnetRepository.delete(subnet)
    }

    @Transactional
    fun createRouteTable(
        tenantId: UUID,
        projectId: UUID,
        vpcId: UUID,
        name: String,
        actorId: UUID
    ): NetworkRouteTableEntity {
        validateProjectScope(tenantId, projectId)
        val vpc = getVpc(vpcId)
        requireScope(vpc.tenantId == tenantId && vpc.projectId == projectId, "vpc scope mismatch: $vpcId")
        val normalizedName = name.trim()
        if (routeTableRepository.existsByVpcIdAndName(vpcId, normalizedName)) {
            throw DuplicateResourceException("route table already exists: $normalizedName")
        }
        val routeTable = routeTableRepository.save(
            NetworkRouteTableEntity(
                tenantId = tenantId,
                projectId = projectId,
                vpcId = vpcId,
                name = normalizedName,
                isDefault = false,
                createdBy = actorId.toString()
            )
        )
        routeRepository.save(
            NetworkRouteEntity(
                tenantId = tenantId,
                projectId = projectId,
                vpcId = vpcId,
                routeTableId = requireNotNull(routeTable.id),
                destinationCidr = vpc.cidrBlock,
                targetType = NetworkRouteTargetType.LOCAL,
                createdBy = actorId.toString()
            )
        )
        return routeTable
    }

    @Transactional(readOnly = true)
    fun listRouteTables(vpcId: UUID): List<NetworkRouteTableEntity> {
        return routeTableRepository.findAllByVpcIdOrderByCreatedAtDesc(vpcId)
    }

    @Transactional(readOnly = true)
    fun getRouteTable(routeTableId: UUID): NetworkRouteTableEntity {
        return routeTableRepository.findById(routeTableId)
            .orElseThrow { ResourceNotFoundException("route table not found: $routeTableId") }
    }

    @Transactional
    fun deleteRouteTable(routeTableId: UUID) {
        val routeTable = getRouteTable(routeTableId)
        if (routeTable.isDefault) {
            throw BadRequestException("default route table cannot be deleted")
        }
        if (routeTableAssociationRepository.existsByRouteTableId(routeTableId)) {
            throw BadRequestException("cannot delete route table with subnet associations: $routeTableId")
        }
        routeRepository.deleteAllByRouteTableId(routeTableId)
        routeTableRepository.delete(routeTable)
    }

    @Transactional
    fun createRoute(
        routeTableId: UUID,
        destinationCidr: String,
        targetType: NetworkRouteTargetType,
        targetResourceId: UUID?,
        actorId: UUID
    ): NetworkRouteEntity {
        val routeTable = getRouteTable(routeTableId)
        val normalizedCidr = NetworkCidr.parse(destinationCidr).original
        if (routeRepository.existsByRouteTableIdAndDestinationCidr(routeTableId, normalizedCidr)) {
            throw DuplicateResourceException("route already exists for destination: $normalizedCidr")
        }
        val normalizedTargetResourceId = when (targetType) {
            NetworkRouteTargetType.NAT_GATEWAY -> {
                val natGatewayId = targetResourceId ?: throw BadRequestException("target resource id is required for nat gateway route")
                val natGateway = natGatewayRepository.findById(natGatewayId)
                    .orElseThrow { ResourceNotFoundException("nat gateway not found: $natGatewayId") }
                requireScope(natGateway.vpcId == routeTable.vpcId, "nat gateway must belong to same vpc as route table")
                natGatewayId
            }

            else -> {
                if (targetResourceId != null) {
                    throw BadRequestException("target resource id is not allowed for route type $targetType")
                }
                null
            }
        }
        return routeRepository.save(
            NetworkRouteEntity(
                tenantId = routeTable.tenantId,
                projectId = routeTable.projectId,
                vpcId = routeTable.vpcId,
                routeTableId = routeTableId,
                destinationCidr = normalizedCidr,
                targetType = targetType,
                targetResourceId = normalizedTargetResourceId,
                createdBy = actorId.toString()
            )
        )
    }

    @Transactional
    fun deleteRoute(routeId: UUID) {
        val route = getRoute(routeId)
        if (route.targetType == NetworkRouteTargetType.LOCAL) {
            throw BadRequestException("local route cannot be deleted")
        }
        routeRepository.delete(route)
    }

    @Transactional
    fun associateSubnet(
        routeTableId: UUID,
        subnetId: UUID,
        actorId: UUID
    ): NetworkRouteTableAssociationEntity {
        val routeTable = getRouteTable(routeTableId)
        val subnet = getSubnet(subnetId)
        requireScope(routeTable.vpcId == subnet.vpcId, "route table and subnet must belong to same vpc")
        return replaceAssociation(subnetId, routeTableId, actorId)
    }

    @Transactional(readOnly = true)
    fun getAssociationBySubnet(subnetId: UUID): NetworkRouteTableAssociationEntity? {
        return routeTableAssociationRepository.findBySubnetId(subnetId)
    }

    @Transactional(readOnly = true)
    fun getAssociation(associationId: UUID): NetworkRouteTableAssociationEntity {
        return routeTableAssociationRepository.findById(associationId)
            .orElseThrow { ResourceNotFoundException("route table association not found: $associationId") }
    }

    @Transactional(readOnly = true)
    fun getRoute(routeId: UUID): NetworkRouteEntity {
        return routeRepository.findById(routeId)
            .orElseThrow { ResourceNotFoundException("route not found: $routeId") }
    }

    @Transactional
    fun deleteAssociation(associationId: UUID, actorId: UUID): NetworkRouteTableAssociationEntity {
        val association = routeTableAssociationRepository.findById(associationId)
            .orElseThrow { ResourceNotFoundException("route table association not found: $associationId") }
        val subnet = getSubnet(association.subnetId)
        val vpc = getVpc(subnet.vpcId)
        val defaultRouteTableId = vpc.defaultRouteTableId
            ?: throw ResourceNotFoundException("default route table missing for vpc: ${vpc.id}")
        routeTableAssociationRepository.delete(association)
        return replaceAssociation(subnet.id ?: error("subnet id is null"), defaultRouteTableId, actorId)
    }

    @Transactional
    fun createSecurityGroup(
        tenantId: UUID,
        projectId: UUID,
        vpcId: UUID,
        name: String,
        description: String?,
        actorId: UUID
    ): NetworkSecurityGroupEntity {
        validateProjectScope(tenantId, projectId)
        val vpc = getVpc(vpcId)
        requireScope(vpc.tenantId == tenantId && vpc.projectId == projectId, "vpc scope mismatch: $vpcId")
        val normalizedName = name.trim()
        if (securityGroupRepository.existsByVpcIdAndName(vpcId, normalizedName)) {
            throw DuplicateResourceException("security group already exists: $normalizedName")
        }
        return securityGroupRepository.save(
            NetworkSecurityGroupEntity(
                tenantId = tenantId,
                projectId = projectId,
                vpcId = vpcId,
                name = normalizedName,
                description = description?.trim()?.takeIf { it.isNotBlank() },
                createdBy = actorId.toString()
            )
        )
    }

    @Transactional(readOnly = true)
    fun listSecurityGroups(tenantId: UUID, projectId: UUID): List<NetworkSecurityGroupEntity> {
        return securityGroupRepository.findAllByTenantIdAndProjectIdOrderByCreatedAtDesc(tenantId, projectId)
    }

    @Transactional(readOnly = true)
    fun getSecurityGroup(securityGroupId: UUID): NetworkSecurityGroupEntity {
        return securityGroupRepository.findById(securityGroupId)
            .orElseThrow { ResourceNotFoundException("security group not found: $securityGroupId") }
    }

    @Transactional
    fun deleteSecurityGroup(securityGroupId: UUID) {
        securityGroupRuleRepository.deleteAllBySecurityGroupId(securityGroupId)
        val securityGroup = getSecurityGroup(securityGroupId)
        securityGroupRepository.delete(securityGroup)
    }

    @Transactional
    fun replaceSecurityGroupRules(
        securityGroupId: UUID,
        rules: List<SecurityGroupRuleDraft>,
        actorId: UUID
    ): List<NetworkSecurityGroupRuleEntity> {
        val securityGroup = getSecurityGroup(securityGroupId)
        rules.forEach { validateRule(it) }
        securityGroupRuleRepository.deleteAllBySecurityGroupId(securityGroupId)
        return rules.map { draft ->
            securityGroupRuleRepository.save(
                NetworkSecurityGroupRuleEntity(
                    tenantId = securityGroup.tenantId,
                    projectId = securityGroup.projectId,
                    securityGroupId = securityGroupId,
                    direction = draft.direction,
                    protocol = draft.protocol,
                    portRangeStart = draft.portRangeStart,
                    portRangeEnd = draft.portRangeEnd,
                    cidrBlock = NetworkCidr.parse(draft.cidrBlock).original,
                    description = draft.description?.trim()?.takeIf { it.isNotBlank() },
                    createdBy = actorId.toString()
                )
            )
        }
    }

    @Transactional(readOnly = true)
    fun listRoutes(routeTableId: UUID): List<NetworkRouteEntity> {
        return routeRepository.findAllByRouteTableIdOrderByCreatedAtAsc(routeTableId)
    }

    @Transactional(readOnly = true)
    fun listAssociations(routeTableId: UUID): List<NetworkRouteTableAssociationEntity> {
        return routeTableAssociationRepository.findAllByRouteTableIdOrderByCreatedAtAsc(routeTableId)
    }

    @Transactional(readOnly = true)
    fun listSecurityGroupRules(securityGroupId: UUID): List<NetworkSecurityGroupRuleEntity> {
        return securityGroupRuleRepository.findAllBySecurityGroupIdOrderByCreatedAtAsc(securityGroupId)
    }

    private fun validateProjectScope(tenantId: UUID, projectId: UUID) {
        val project = projectRepository.findById(projectId)
            .orElseThrow { ResourceNotFoundException("project not found: $projectId") }
        if (project.tenantId != tenantId) {
            throw BadRequestException("project does not belong to tenant: $projectId")
        }
    }

    private fun replaceAssociation(
        subnetId: UUID,
        routeTableId: UUID,
        actorId: UUID
    ): NetworkRouteTableAssociationEntity {
        routeTableAssociationRepository.findBySubnetId(subnetId)?.let { existing ->
            routeTableAssociationRepository.delete(existing)
            routeTableAssociationRepository.flush()
        }
        val routeTable = getRouteTable(routeTableId)
        return routeTableAssociationRepository.save(
            NetworkRouteTableAssociationEntity(
                tenantId = routeTable.tenantId,
                projectId = routeTable.projectId,
                routeTableId = routeTableId,
                subnetId = subnetId,
                createdBy = actorId.toString()
            )
        )
    }

    private fun requireScope(condition: Boolean, message: String) {
        if (!condition) {
            throw BadRequestException(message)
        }
    }

    private fun validateRule(rule: SecurityGroupRuleDraft) {
        NetworkCidr.parse(rule.cidrBlock)
        val start = rule.portRangeStart
        val end = rule.portRangeEnd
        if (rule.protocol == NetworkRuleProtocol.ALL || rule.protocol == NetworkRuleProtocol.ICMP) {
            if (start != null || end != null) {
                throw BadRequestException("port range is not allowed for protocol ${rule.protocol}")
            }
            return
        }
        if (start == null || end == null) {
            throw BadRequestException("port range is required for protocol ${rule.protocol}")
        }
        if (start !in 1..65535 || end !in 1..65535 || start > end) {
            throw BadRequestException("invalid port range: $start-$end")
        }
    }
}

data class SecurityGroupRuleDraft(
    val direction: com.stratuscloud.network.domain.NetworkRuleDirection,
    val protocol: NetworkRuleProtocol,
    val portRangeStart: Int?,
    val portRangeEnd: Int?,
    val cidrBlock: String,
    val description: String?
)
