package com.stratuscloud.network.service

import com.stratuscloud.iam.exception.BadRequestException
import com.stratuscloud.iam.exception.DuplicateResourceException
import com.stratuscloud.iam.exception.ResourceNotFoundException
import com.stratuscloud.iam.repository.ProjectRepository
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
import com.stratuscloud.network.domain.NetworkRouteTargetType
import com.stratuscloud.network.repository.NetworkDnsRecordRepository
import com.stratuscloud.network.repository.NetworkElasticIpRepository
import com.stratuscloud.network.repository.NetworkLoadBalancerListenerRepository
import com.stratuscloud.network.repository.NetworkLoadBalancerRepository
import com.stratuscloud.network.repository.NetworkLoadBalancerRuleRepository
import com.stratuscloud.network.repository.NetworkNatGatewayRepository
import com.stratuscloud.network.repository.NetworkRouteRepository
import com.stratuscloud.network.repository.NetworkSubnetRepository
import com.stratuscloud.network.repository.NetworkVpcRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class NetworkEdgeService(
    private val projectRepository: ProjectRepository,
    private val vpcRepository: NetworkVpcRepository,
    private val subnetRepository: NetworkSubnetRepository,
    private val routeRepository: NetworkRouteRepository,
    private val loadBalancerRepository: NetworkLoadBalancerRepository,
    private val loadBalancerListenerRepository: NetworkLoadBalancerListenerRepository,
    private val loadBalancerRuleRepository: NetworkLoadBalancerRuleRepository,
    private val elasticIpRepository: NetworkElasticIpRepository,
    private val natGatewayRepository: NetworkNatGatewayRepository,
    private val dnsRecordRepository: NetworkDnsRecordRepository
) {

    @Transactional
    fun createLoadBalancer(
        tenantId: UUID,
        projectId: UUID,
        vpcId: UUID,
        name: String,
        type: NetworkLoadBalancerType,
        scheme: NetworkLoadBalancerScheme,
        actorId: UUID
    ): NetworkLoadBalancerEntity {
        validateProjectScope(tenantId, projectId)
        val vpc = getVpc(vpcId)
        requireScope(vpc.tenantId == tenantId && vpc.projectId == projectId, "vpc scope mismatch: $vpcId")
        val normalizedName = name.trim()
        if (loadBalancerRepository.existsByVpcIdAndName(vpcId, normalizedName)) {
            throw DuplicateResourceException("load balancer already exists: $normalizedName")
        }
        return loadBalancerRepository.save(
            NetworkLoadBalancerEntity(
                tenantId = tenantId,
                projectId = projectId,
                vpcId = vpcId,
                name = normalizedName,
                type = type,
                scheme = scheme,
                createdBy = actorId.toString()
            )
        )
    }

    @Transactional(readOnly = true)
    fun listLoadBalancers(tenantId: UUID, projectId: UUID): List<NetworkLoadBalancerEntity> {
        return loadBalancerRepository.findAllByTenantIdAndProjectIdOrderByCreatedAtDesc(tenantId, projectId)
    }

    @Transactional(readOnly = true)
    fun getLoadBalancer(loadBalancerId: UUID): NetworkLoadBalancerEntity {
        return loadBalancerRepository.findById(loadBalancerId)
            .orElseThrow { ResourceNotFoundException("load balancer not found: $loadBalancerId") }
    }

    @Transactional
    fun deleteLoadBalancer(loadBalancerId: UUID) {
        val loadBalancer = getLoadBalancer(loadBalancerId)
        if (elasticIpRepository.existsByAttachmentTargetTypeAndAttachmentTargetId(NetworkElasticIpAttachmentType.LOAD_BALANCER, loadBalancerId)) {
            throw BadRequestException("cannot delete load balancer with attached elastic ip: $loadBalancerId")
        }
        if (dnsRecordRepository.existsByTargetTypeAndTargetId(NetworkDnsTargetType.LOAD_BALANCER, loadBalancerId)) {
            throw BadRequestException("cannot delete load balancer referenced by dns record: $loadBalancerId")
        }
        loadBalancerListenerRepository.findAllByLoadBalancerIdOrderByCreatedAtAsc(loadBalancerId).forEach { listener ->
            loadBalancerRuleRepository.deleteAllByListenerId(requireNotNull(listener.id))
        }
        loadBalancerListenerRepository.deleteAllByLoadBalancerId(loadBalancerId)
        loadBalancerRepository.delete(loadBalancer)
    }

    @Transactional
    fun createLoadBalancerListener(
        loadBalancerId: UUID,
        protocol: NetworkLoadBalancerProtocol,
        port: Int,
        defaultTargetSubnetId: UUID,
        actorId: UUID
    ): NetworkLoadBalancerListenerEntity {
        val loadBalancer = getLoadBalancer(loadBalancerId)
        validateListenerCompatibility(loadBalancer, protocol)
        if (port !in 1..65535) {
            throw BadRequestException("invalid listener port: $port")
        }
        if (loadBalancerListenerRepository.existsByLoadBalancerIdAndPort(loadBalancerId, port)) {
            throw DuplicateResourceException("listener already exists on port: $port")
        }
        val subnet = getSubnet(defaultTargetSubnetId)
        requireScope(subnet.vpcId == loadBalancer.vpcId, "listener target subnet must belong to load balancer vpc")
        return loadBalancerListenerRepository.save(
            NetworkLoadBalancerListenerEntity(
                tenantId = loadBalancer.tenantId,
                projectId = loadBalancer.projectId,
                loadBalancerId = loadBalancerId,
                protocol = protocol,
                port = port,
                defaultTargetSubnetId = defaultTargetSubnetId,
                createdBy = actorId.toString()
            )
        )
    }

    @Transactional(readOnly = true)
    fun getLoadBalancerListener(listenerId: UUID): NetworkLoadBalancerListenerEntity {
        return loadBalancerListenerRepository.findById(listenerId)
            .orElseThrow { ResourceNotFoundException("load balancer listener not found: $listenerId") }
    }

    @Transactional
    fun createLoadBalancerRule(
        listenerId: UUID,
        priority: Int,
        pathPattern: String,
        targetSubnetId: UUID,
        actorId: UUID
    ): NetworkLoadBalancerRuleEntity {
        val listener = getLoadBalancerListener(listenerId)
        val loadBalancer = getLoadBalancer(listener.loadBalancerId)
        if (listener.protocol != NetworkLoadBalancerProtocol.HTTP || loadBalancer.type != NetworkLoadBalancerType.L7) {
            throw BadRequestException("listener does not support path routing: $listenerId")
        }
        if (priority < 1) {
            throw BadRequestException("priority must be positive")
        }
        if (loadBalancerRuleRepository.existsByListenerIdAndPriority(listenerId, priority)) {
            throw DuplicateResourceException("listener rule priority already exists: $priority")
        }
        val normalizedPath = pathPattern.trim()
        if (!normalizedPath.startsWith("/")) {
            throw BadRequestException("path pattern must start with '/'")
        }
        val subnet = getSubnet(targetSubnetId)
        requireScope(subnet.vpcId == loadBalancer.vpcId, "rule target subnet must belong to load balancer vpc")
        return loadBalancerRuleRepository.save(
            NetworkLoadBalancerRuleEntity(
                tenantId = listener.tenantId,
                projectId = listener.projectId,
                listenerId = listenerId,
                priority = priority,
                pathPattern = normalizedPath,
                targetSubnetId = targetSubnetId,
                createdBy = actorId.toString()
            )
        )
    }

    @Transactional(readOnly = true)
    fun listLoadBalancerListeners(loadBalancerId: UUID): List<NetworkLoadBalancerListenerEntity> {
        return loadBalancerListenerRepository.findAllByLoadBalancerIdOrderByCreatedAtAsc(loadBalancerId)
    }

    @Transactional(readOnly = true)
    fun listLoadBalancerRules(listenerId: UUID): List<NetworkLoadBalancerRuleEntity> {
        return loadBalancerRuleRepository.findAllByListenerIdOrderByPriorityAsc(listenerId)
    }

    @Transactional
    fun createElasticIp(
        tenantId: UUID,
        projectId: UUID,
        name: String,
        actorId: UUID
    ): NetworkElasticIpEntity {
        validateProjectScope(tenantId, projectId)
        val normalizedName = name.trim()
        if (elasticIpRepository.existsByProjectIdAndName(projectId, normalizedName)) {
            throw DuplicateResourceException("elastic ip already exists: $normalizedName")
        }
        return elasticIpRepository.save(
            NetworkElasticIpEntity(
                tenantId = tenantId,
                projectId = projectId,
                name = normalizedName,
                publicIp = generatePublicIp(),
                allocationStatus = NetworkElasticIpAllocationStatus.UNASSIGNED,
                createdBy = actorId.toString()
            )
        )
    }

    @Transactional(readOnly = true)
    fun listElasticIps(tenantId: UUID, projectId: UUID): List<NetworkElasticIpEntity> {
        return elasticIpRepository.findAllByTenantIdAndProjectIdOrderByCreatedAtDesc(tenantId, projectId)
    }

    @Transactional(readOnly = true)
    fun getElasticIp(elasticIpId: UUID): NetworkElasticIpEntity {
        return elasticIpRepository.findById(elasticIpId)
            .orElseThrow { ResourceNotFoundException("elastic ip not found: $elasticIpId") }
    }

    @Transactional
    fun deleteElasticIp(elasticIpId: UUID) {
        val elasticIp = getElasticIp(elasticIpId)
        if (elasticIp.attachmentTargetId != null) {
            throw BadRequestException("cannot delete elastic ip while attached: $elasticIpId")
        }
        if (dnsRecordRepository.existsByTargetTypeAndTargetId(NetworkDnsTargetType.ELASTIC_IP, elasticIpId)) {
            throw BadRequestException("cannot delete elastic ip referenced by dns record: $elasticIpId")
        }
        elasticIpRepository.delete(elasticIp)
    }

    @Transactional
    fun attachElasticIp(
        elasticIpId: UUID,
        targetType: NetworkElasticIpAttachmentType,
        targetId: UUID
    ): NetworkElasticIpEntity {
        val elasticIp = getElasticIp(elasticIpId)
        validateAttachmentTarget(elasticIp.tenantId, elasticIp.projectId, targetType, targetId)
        if (elasticIp.attachmentTargetId != null && elasticIp.attachmentTargetId != targetId) {
            throw BadRequestException("elastic ip is already attached: $elasticIpId")
        }
        if (elasticIpRepository.existsByAttachmentTargetTypeAndAttachmentTargetId(targetType, targetId) &&
            elasticIpRepository.findByAttachmentTargetTypeAndAttachmentTargetId(targetType, targetId)?.id != elasticIpId
        ) {
            throw BadRequestException("target already has elastic ip attached: $targetId")
        }
        elasticIp.attachmentTargetType = targetType
        elasticIp.attachmentTargetId = targetId
        elasticIp.allocationStatus = NetworkElasticIpAllocationStatus.ASSIGNED
        return elasticIpRepository.save(elasticIp)
    }

    @Transactional
    fun detachElasticIp(elasticIpId: UUID): NetworkElasticIpEntity {
        val elasticIp = getElasticIp(elasticIpId)
        elasticIp.attachmentTargetType = null
        elasticIp.attachmentTargetId = null
        elasticIp.allocationStatus = NetworkElasticIpAllocationStatus.UNASSIGNED
        return elasticIpRepository.save(elasticIp)
    }

    @Transactional
    fun createNatGateway(
        tenantId: UUID,
        projectId: UUID,
        vpcId: UUID,
        subnetId: UUID,
        name: String,
        actorId: UUID
    ): NetworkNatGatewayEntity {
        validateProjectScope(tenantId, projectId)
        val vpc = getVpc(vpcId)
        requireScope(vpc.tenantId == tenantId && vpc.projectId == projectId, "vpc scope mismatch: $vpcId")
        val subnet = getSubnet(subnetId)
        requireScope(subnet.vpcId == vpcId, "nat gateway subnet must belong to vpc")
        val normalizedName = name.trim()
        if (natGatewayRepository.existsByVpcIdAndName(vpcId, normalizedName)) {
            throw DuplicateResourceException("nat gateway already exists: $normalizedName")
        }
        if (natGatewayRepository.existsBySubnetId(subnetId)) {
            throw BadRequestException("nat gateway already exists in subnet: $subnetId")
        }
        return natGatewayRepository.save(
            NetworkNatGatewayEntity(
                tenantId = tenantId,
                projectId = projectId,
                vpcId = vpcId,
                subnetId = subnetId,
                name = normalizedName,
                createdBy = actorId.toString()
            )
        )
    }

    @Transactional(readOnly = true)
    fun listNatGateways(tenantId: UUID, projectId: UUID): List<NetworkNatGatewayEntity> {
        return natGatewayRepository.findAllByTenantIdAndProjectIdOrderByCreatedAtDesc(tenantId, projectId)
    }

    @Transactional(readOnly = true)
    fun getNatGateway(natGatewayId: UUID): NetworkNatGatewayEntity {
        return natGatewayRepository.findById(natGatewayId)
            .orElseThrow { ResourceNotFoundException("nat gateway not found: $natGatewayId") }
    }

    @Transactional
    fun deleteNatGateway(natGatewayId: UUID) {
        val natGateway = getNatGateway(natGatewayId)
        if (routeRepository.existsByTargetTypeAndTargetResourceId(NetworkRouteTargetType.NAT_GATEWAY, natGatewayId)) {
            throw BadRequestException("cannot delete nat gateway referenced by route: $natGatewayId")
        }
        if (elasticIpRepository.existsByAttachmentTargetTypeAndAttachmentTargetId(NetworkElasticIpAttachmentType.NAT_GATEWAY, natGatewayId)) {
            throw BadRequestException("cannot delete nat gateway with attached elastic ip: $natGatewayId")
        }
        natGatewayRepository.delete(natGateway)
    }

    @Transactional
    fun createDnsRecord(
        tenantId: UUID,
        projectId: UUID,
        name: String,
        targetType: NetworkDnsTargetType,
        targetId: UUID,
        actorId: UUID
    ): NetworkDnsRecordEntity {
        validateProjectScope(tenantId, projectId)
        val normalizedName = name.trim().lowercase()
        if (dnsRecordRepository.existsByProjectIdAndName(projectId, normalizedName)) {
            throw DuplicateResourceException("dns record already exists: $normalizedName")
        }
        validateDnsTarget(tenantId, projectId, targetType, targetId)
        return dnsRecordRepository.save(
            NetworkDnsRecordEntity(
                tenantId = tenantId,
                projectId = projectId,
                name = normalizedName,
                recordType = NetworkDnsRecordType.A,
                targetType = targetType,
                targetId = targetId,
                createdBy = actorId.toString()
            )
        )
    }

    @Transactional(readOnly = true)
    fun listDnsRecords(tenantId: UUID, projectId: UUID): List<NetworkDnsRecordEntity> {
        return dnsRecordRepository.findAllByTenantIdAndProjectIdOrderByCreatedAtDesc(tenantId, projectId)
    }

    @Transactional(readOnly = true)
    fun getDnsRecord(dnsRecordId: UUID): NetworkDnsRecordEntity {
        return dnsRecordRepository.findById(dnsRecordId)
            .orElseThrow { ResourceNotFoundException("dns record not found: $dnsRecordId") }
    }

    @Transactional
    fun deleteDnsRecord(dnsRecordId: UUID) {
        dnsRecordRepository.delete(getDnsRecord(dnsRecordId))
    }

    private fun validateListenerCompatibility(
        loadBalancer: NetworkLoadBalancerEntity,
        protocol: NetworkLoadBalancerProtocol
    ) {
        when (loadBalancer.type) {
            NetworkLoadBalancerType.L4 -> requireScope(protocol == NetworkLoadBalancerProtocol.TCP, "l4 load balancer supports only TCP listeners")
            NetworkLoadBalancerType.L7 -> requireScope(protocol == NetworkLoadBalancerProtocol.HTTP, "l7 load balancer supports only HTTP listeners")
        }
    }

    private fun validateAttachmentTarget(
        tenantId: UUID,
        projectId: UUID,
        targetType: NetworkElasticIpAttachmentType,
        targetId: UUID
    ) {
        when (targetType) {
            NetworkElasticIpAttachmentType.LOAD_BALANCER -> {
                val loadBalancer = getLoadBalancer(targetId)
                requireScope(loadBalancer.tenantId == tenantId && loadBalancer.projectId == projectId, "load balancer scope mismatch: $targetId")
            }

            NetworkElasticIpAttachmentType.NAT_GATEWAY -> {
                val natGateway = getNatGateway(targetId)
                requireScope(natGateway.tenantId == tenantId && natGateway.projectId == projectId, "nat gateway scope mismatch: $targetId")
            }
        }
    }

    private fun validateDnsTarget(
        tenantId: UUID,
        projectId: UUID,
        targetType: NetworkDnsTargetType,
        targetId: UUID
    ) {
        when (targetType) {
            NetworkDnsTargetType.LOAD_BALANCER -> {
                val loadBalancer = getLoadBalancer(targetId)
                requireScope(loadBalancer.tenantId == tenantId && loadBalancer.projectId == projectId, "load balancer scope mismatch: $targetId")
            }

            NetworkDnsTargetType.ELASTIC_IP -> {
                val elasticIp = getElasticIp(targetId)
                requireScope(elasticIp.tenantId == tenantId && elasticIp.projectId == projectId, "elastic ip scope mismatch: $targetId")
            }
        }
    }

    private fun getVpc(vpcId: UUID) = vpcRepository.findById(vpcId)
        .orElseThrow { ResourceNotFoundException("vpc not found: $vpcId") }

    private fun getSubnet(subnetId: UUID) = subnetRepository.findById(subnetId)
        .orElseThrow { ResourceNotFoundException("subnet not found: $subnetId") }

    private fun validateProjectScope(tenantId: UUID, projectId: UUID) {
        val project = projectRepository.findById(projectId)
            .orElseThrow { ResourceNotFoundException("project not found: $projectId") }
        if (project.tenantId != tenantId) {
            throw BadRequestException("project does not belong to tenant: $projectId")
        }
    }

    private fun requireScope(condition: Boolean, message: String) {
        if (!condition) {
            throw BadRequestException(message)
        }
    }

    private fun generatePublicIp(): String {
        var candidateIndex = elasticIpRepository.count().toInt() + 10
        while (true) {
            val thirdOctet = candidateIndex / 250
            val fourthOctet = candidateIndex % 250 + 1
            val candidate = "203.0.${thirdOctet}.${fourthOctet}"
            if (!elasticIpRepository.existsByPublicIp(candidate)) {
                return candidate
            }
            candidateIndex++
        }
    }
}
