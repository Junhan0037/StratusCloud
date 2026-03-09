package com.stratuscloud.api.network.controller

import com.stratuscloud.api.common.security.ApiAuditRecorder
import com.stratuscloud.api.common.security.AuthContextHolder
import com.stratuscloud.api.common.security.AuthorizationFacade
import com.stratuscloud.api.network.dto.AttachElasticIpRequest
import com.stratuscloud.api.network.dto.CreateDnsRecordRequest
import com.stratuscloud.api.network.dto.CreateElasticIpRequest
import com.stratuscloud.api.network.dto.CreateLoadBalancerListenerRequest
import com.stratuscloud.api.network.dto.CreateLoadBalancerRequest
import com.stratuscloud.api.network.dto.CreateLoadBalancerRuleRequest
import com.stratuscloud.api.network.dto.CreateNatGatewayRequest
import com.stratuscloud.api.network.dto.DnsRecordResponse
import com.stratuscloud.api.network.dto.ElasticIpResponse
import com.stratuscloud.api.network.dto.LoadBalancerListenerResponse
import com.stratuscloud.api.network.dto.LoadBalancerResponse
import com.stratuscloud.api.network.dto.LoadBalancerRuleResponse
import com.stratuscloud.api.network.dto.NatGatewayResponse
import com.stratuscloud.iam.service.IamAction
import com.stratuscloud.network.service.NetworkEdgeService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/v1/network")
class NetworkEdgeController(
    private val networkEdgeService: NetworkEdgeService,
    private val authorizationFacade: AuthorizationFacade,
    private val apiAuditRecorder: ApiAuditRecorder
) {

    @PostMapping("/load-balancers")
    fun createLoadBalancer(@Valid @RequestBody request: CreateLoadBalancerRequest): ResponseEntity<LoadBalancerResponse> {
        val principal = AuthContextHolder.getRequired()
        authorizationFacade.authorize(
            principal = principal,
            tenantId = request.tenantId,
            projectId = request.projectId,
            action = IamAction.NETWORK_LOAD_BALANCER_CREATE,
            resource = "network-vpc:${request.vpcId}",
            resourceType = "LOAD_BALANCER",
            resourceId = null,
            metadata = mapOf("name" to request.name, "type" to request.type.name)
        )
        val loadBalancer = networkEdgeService.createLoadBalancer(
            tenantId = request.tenantId,
            projectId = request.projectId,
            vpcId = request.vpcId,
            name = request.name,
            type = request.type,
            scheme = request.scheme,
            actorId = principal.actorId
        )
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = loadBalancer.tenantId,
            projectId = loadBalancer.projectId,
            action = IamAction.NETWORK_LOAD_BALANCER_CREATE,
            resourceType = "LOAD_BALANCER",
            resourceId = loadBalancer.id.toString(),
            metadata = mapOf("name" to loadBalancer.name)
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(buildLoadBalancerResponse(loadBalancer))
    }

    @GetMapping("/load-balancers")
    fun listLoadBalancers(
        @RequestParam tenantId: UUID,
        @RequestParam projectId: UUID
    ): ResponseEntity<List<LoadBalancerResponse>> {
        val principal = AuthContextHolder.getRequired()
        authorizationFacade.authorize(
            principal = principal,
            tenantId = tenantId,
            projectId = projectId,
            action = IamAction.NETWORK_LOAD_BALANCER_LIST,
            resource = "project:$projectId",
            resourceType = "LOAD_BALANCER",
            resourceId = null
        )
        return ResponseEntity.ok(
            networkEdgeService.listLoadBalancers(tenantId, projectId).map(::buildLoadBalancerResponse)
        )
    }

    @GetMapping("/load-balancers/{loadBalancerId}")
    fun getLoadBalancer(@PathVariable loadBalancerId: UUID): ResponseEntity<LoadBalancerResponse> {
        val principal = AuthContextHolder.getRequired()
        val loadBalancer = networkEdgeService.getLoadBalancer(loadBalancerId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = loadBalancer.tenantId,
            projectId = loadBalancer.projectId,
            action = IamAction.NETWORK_LOAD_BALANCER_READ,
            resource = "network-load-balancer:$loadBalancerId",
            resourceType = "LOAD_BALANCER",
            resourceId = loadBalancerId.toString()
        )
        return ResponseEntity.ok(buildLoadBalancerResponse(loadBalancer))
    }

    @DeleteMapping("/load-balancers/{loadBalancerId}")
    fun deleteLoadBalancer(@PathVariable loadBalancerId: UUID): ResponseEntity<Void> {
        val principal = AuthContextHolder.getRequired()
        val loadBalancer = networkEdgeService.getLoadBalancer(loadBalancerId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = loadBalancer.tenantId,
            projectId = loadBalancer.projectId,
            action = IamAction.NETWORK_LOAD_BALANCER_DELETE,
            resource = "network-load-balancer:$loadBalancerId",
            resourceType = "LOAD_BALANCER",
            resourceId = loadBalancerId.toString()
        )
        networkEdgeService.deleteLoadBalancer(loadBalancerId)
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = loadBalancer.tenantId,
            projectId = loadBalancer.projectId,
            action = IamAction.NETWORK_LOAD_BALANCER_DELETE,
            resourceType = "LOAD_BALANCER",
            resourceId = loadBalancerId.toString()
        )
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/load-balancers/{loadBalancerId}/listeners")
    fun createListener(
        @PathVariable loadBalancerId: UUID,
        @Valid @RequestBody request: CreateLoadBalancerListenerRequest
    ): ResponseEntity<LoadBalancerListenerResponse> {
        val principal = AuthContextHolder.getRequired()
        val loadBalancer = networkEdgeService.getLoadBalancer(loadBalancerId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = loadBalancer.tenantId,
            projectId = loadBalancer.projectId,
            action = IamAction.NETWORK_LOAD_BALANCER_LISTENER_CREATE,
            resource = "network-load-balancer:$loadBalancerId",
            resourceType = "LOAD_BALANCER_LISTENER",
            resourceId = null,
            metadata = mapOf("port" to request.port, "protocol" to request.protocol.name)
        )
        val listener = networkEdgeService.createLoadBalancerListener(
            loadBalancerId = loadBalancerId,
            protocol = request.protocol,
            port = request.port,
            defaultTargetSubnetId = request.defaultTargetSubnetId,
            actorId = principal.actorId
        )
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = listener.tenantId,
            projectId = listener.projectId,
            action = IamAction.NETWORK_LOAD_BALANCER_LISTENER_CREATE,
            resourceType = "LOAD_BALANCER_LISTENER",
            resourceId = listener.id.toString()
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(buildListenerResponse(listener))
    }

    @PostMapping("/listeners/{listenerId}/rules")
    fun createRule(
        @PathVariable listenerId: UUID,
        @Valid @RequestBody request: CreateLoadBalancerRuleRequest
    ): ResponseEntity<LoadBalancerRuleResponse> {
        val principal = AuthContextHolder.getRequired()
        val listener = networkEdgeService.getLoadBalancerListener(listenerId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = listener.tenantId,
            projectId = listener.projectId,
            action = IamAction.NETWORK_LOAD_BALANCER_RULE_CREATE,
            resource = "network-load-balancer-listener:$listenerId",
            resourceType = "LOAD_BALANCER_RULE",
            resourceId = null,
            metadata = mapOf("priority" to request.priority, "pathPattern" to request.pathPattern)
        )
        val rule = networkEdgeService.createLoadBalancerRule(
            listenerId = listenerId,
            priority = request.priority,
            pathPattern = request.pathPattern,
            targetSubnetId = request.targetSubnetId,
            actorId = principal.actorId
        )
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = rule.tenantId,
            projectId = rule.projectId,
            action = IamAction.NETWORK_LOAD_BALANCER_RULE_CREATE,
            resourceType = "LOAD_BALANCER_RULE",
            resourceId = rule.id.toString()
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(LoadBalancerRuleResponse.from(rule))
    }

    @PostMapping("/elastic-ips")
    fun createElasticIp(@Valid @RequestBody request: CreateElasticIpRequest): ResponseEntity<ElasticIpResponse> {
        val principal = AuthContextHolder.getRequired()
        authorizationFacade.authorize(
            principal = principal,
            tenantId = request.tenantId,
            projectId = request.projectId,
            action = IamAction.NETWORK_ELASTIC_IP_CREATE,
            resource = "project:${request.projectId}",
            resourceType = "ELASTIC_IP",
            resourceId = null,
            metadata = mapOf("name" to request.name)
        )
        val elasticIp = networkEdgeService.createElasticIp(
            tenantId = request.tenantId,
            projectId = request.projectId,
            name = request.name,
            actorId = principal.actorId
        )
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = elasticIp.tenantId,
            projectId = elasticIp.projectId,
            action = IamAction.NETWORK_ELASTIC_IP_CREATE,
            resourceType = "ELASTIC_IP",
            resourceId = elasticIp.id.toString()
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(ElasticIpResponse.from(elasticIp))
    }

    @GetMapping("/elastic-ips")
    fun listElasticIps(
        @RequestParam tenantId: UUID,
        @RequestParam projectId: UUID
    ): ResponseEntity<List<ElasticIpResponse>> {
        val principal = AuthContextHolder.getRequired()
        authorizationFacade.authorize(
            principal = principal,
            tenantId = tenantId,
            projectId = projectId,
            action = IamAction.NETWORK_ELASTIC_IP_LIST,
            resource = "project:$projectId",
            resourceType = "ELASTIC_IP",
            resourceId = null
        )
        return ResponseEntity.ok(
            networkEdgeService.listElasticIps(tenantId, projectId).map(ElasticIpResponse::from)
        )
    }

    @GetMapping("/elastic-ips/{elasticIpId}")
    fun getElasticIp(@PathVariable elasticIpId: UUID): ResponseEntity<ElasticIpResponse> {
        val principal = AuthContextHolder.getRequired()
        val elasticIp = networkEdgeService.getElasticIp(elasticIpId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = elasticIp.tenantId,
            projectId = elasticIp.projectId,
            action = IamAction.NETWORK_ELASTIC_IP_READ,
            resource = "network-elastic-ip:$elasticIpId",
            resourceType = "ELASTIC_IP",
            resourceId = elasticIpId.toString()
        )
        return ResponseEntity.ok(ElasticIpResponse.from(elasticIp))
    }

    @DeleteMapping("/elastic-ips/{elasticIpId}")
    fun deleteElasticIp(@PathVariable elasticIpId: UUID): ResponseEntity<Void> {
        val principal = AuthContextHolder.getRequired()
        val elasticIp = networkEdgeService.getElasticIp(elasticIpId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = elasticIp.tenantId,
            projectId = elasticIp.projectId,
            action = IamAction.NETWORK_ELASTIC_IP_DELETE,
            resource = "network-elastic-ip:$elasticIpId",
            resourceType = "ELASTIC_IP",
            resourceId = elasticIpId.toString()
        )
        networkEdgeService.deleteElasticIp(elasticIpId)
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = elasticIp.tenantId,
            projectId = elasticIp.projectId,
            action = IamAction.NETWORK_ELASTIC_IP_DELETE,
            resourceType = "ELASTIC_IP",
            resourceId = elasticIpId.toString()
        )
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/elastic-ips/{elasticIpId}/attachments")
    fun attachElasticIp(
        @PathVariable elasticIpId: UUID,
        @Valid @RequestBody request: AttachElasticIpRequest
    ): ResponseEntity<ElasticIpResponse> {
        val principal = AuthContextHolder.getRequired()
        val elasticIp = networkEdgeService.getElasticIp(elasticIpId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = elasticIp.tenantId,
            projectId = elasticIp.projectId,
            action = IamAction.NETWORK_ELASTIC_IP_ATTACH,
            resource = "network-elastic-ip:$elasticIpId",
            resourceType = "ELASTIC_IP",
            resourceId = elasticIpId.toString(),
            metadata = mapOf("targetType" to request.targetType.name, "targetId" to request.targetId)
        )
        val updated = networkEdgeService.attachElasticIp(elasticIpId, request.targetType, request.targetId)
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = updated.tenantId,
            projectId = updated.projectId,
            action = IamAction.NETWORK_ELASTIC_IP_ATTACH,
            resourceType = "ELASTIC_IP",
            resourceId = elasticIpId.toString()
        )
        return ResponseEntity.ok(ElasticIpResponse.from(updated))
    }

    @DeleteMapping("/elastic-ips/{elasticIpId}/attachments")
    fun detachElasticIp(@PathVariable elasticIpId: UUID): ResponseEntity<ElasticIpResponse> {
        val principal = AuthContextHolder.getRequired()
        val elasticIp = networkEdgeService.getElasticIp(elasticIpId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = elasticIp.tenantId,
            projectId = elasticIp.projectId,
            action = IamAction.NETWORK_ELASTIC_IP_DETACH,
            resource = "network-elastic-ip:$elasticIpId",
            resourceType = "ELASTIC_IP",
            resourceId = elasticIpId.toString()
        )
        val updated = networkEdgeService.detachElasticIp(elasticIpId)
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = updated.tenantId,
            projectId = updated.projectId,
            action = IamAction.NETWORK_ELASTIC_IP_DETACH,
            resourceType = "ELASTIC_IP",
            resourceId = elasticIpId.toString()
        )
        return ResponseEntity.ok(ElasticIpResponse.from(updated))
    }

    @PostMapping("/nat-gateways")
    fun createNatGateway(@Valid @RequestBody request: CreateNatGatewayRequest): ResponseEntity<NatGatewayResponse> {
        val principal = AuthContextHolder.getRequired()
        authorizationFacade.authorize(
            principal = principal,
            tenantId = request.tenantId,
            projectId = request.projectId,
            action = IamAction.NETWORK_NAT_GATEWAY_CREATE,
            resource = "network-vpc:${request.vpcId}",
            resourceType = "NAT_GATEWAY",
            resourceId = null,
            metadata = mapOf("name" to request.name, "subnetId" to request.subnetId)
        )
        val natGateway = networkEdgeService.createNatGateway(
            tenantId = request.tenantId,
            projectId = request.projectId,
            vpcId = request.vpcId,
            subnetId = request.subnetId,
            name = request.name,
            actorId = principal.actorId
        )
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = natGateway.tenantId,
            projectId = natGateway.projectId,
            action = IamAction.NETWORK_NAT_GATEWAY_CREATE,
            resourceType = "NAT_GATEWAY",
            resourceId = natGateway.id.toString()
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(NatGatewayResponse.from(natGateway))
    }

    @GetMapping("/nat-gateways")
    fun listNatGateways(
        @RequestParam tenantId: UUID,
        @RequestParam projectId: UUID
    ): ResponseEntity<List<NatGatewayResponse>> {
        val principal = AuthContextHolder.getRequired()
        authorizationFacade.authorize(
            principal = principal,
            tenantId = tenantId,
            projectId = projectId,
            action = IamAction.NETWORK_NAT_GATEWAY_LIST,
            resource = "project:$projectId",
            resourceType = "NAT_GATEWAY",
            resourceId = null
        )
        return ResponseEntity.ok(
            networkEdgeService.listNatGateways(tenantId, projectId).map(NatGatewayResponse::from)
        )
    }

    @GetMapping("/nat-gateways/{natGatewayId}")
    fun getNatGateway(@PathVariable natGatewayId: UUID): ResponseEntity<NatGatewayResponse> {
        val principal = AuthContextHolder.getRequired()
        val natGateway = networkEdgeService.getNatGateway(natGatewayId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = natGateway.tenantId,
            projectId = natGateway.projectId,
            action = IamAction.NETWORK_NAT_GATEWAY_READ,
            resource = "network-nat-gateway:$natGatewayId",
            resourceType = "NAT_GATEWAY",
            resourceId = natGatewayId.toString()
        )
        return ResponseEntity.ok(NatGatewayResponse.from(natGateway))
    }

    @DeleteMapping("/nat-gateways/{natGatewayId}")
    fun deleteNatGateway(@PathVariable natGatewayId: UUID): ResponseEntity<Void> {
        val principal = AuthContextHolder.getRequired()
        val natGateway = networkEdgeService.getNatGateway(natGatewayId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = natGateway.tenantId,
            projectId = natGateway.projectId,
            action = IamAction.NETWORK_NAT_GATEWAY_DELETE,
            resource = "network-nat-gateway:$natGatewayId",
            resourceType = "NAT_GATEWAY",
            resourceId = natGatewayId.toString()
        )
        networkEdgeService.deleteNatGateway(natGatewayId)
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = natGateway.tenantId,
            projectId = natGateway.projectId,
            action = IamAction.NETWORK_NAT_GATEWAY_DELETE,
            resourceType = "NAT_GATEWAY",
            resourceId = natGatewayId.toString()
        )
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/dns-records")
    fun createDnsRecord(@Valid @RequestBody request: CreateDnsRecordRequest): ResponseEntity<DnsRecordResponse> {
        val principal = AuthContextHolder.getRequired()
        authorizationFacade.authorize(
            principal = principal,
            tenantId = request.tenantId,
            projectId = request.projectId,
            action = IamAction.NETWORK_DNS_RECORD_CREATE,
            resource = "project:${request.projectId}",
            resourceType = "DNS_RECORD",
            resourceId = null,
            metadata = mapOf("name" to request.name, "targetType" to request.targetType.name)
        )
        val dnsRecord = networkEdgeService.createDnsRecord(
            tenantId = request.tenantId,
            projectId = request.projectId,
            name = request.name,
            targetType = request.targetType,
            targetId = request.targetId,
            actorId = principal.actorId
        )
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = dnsRecord.tenantId,
            projectId = dnsRecord.projectId,
            action = IamAction.NETWORK_DNS_RECORD_CREATE,
            resourceType = "DNS_RECORD",
            resourceId = dnsRecord.id.toString()
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(DnsRecordResponse.from(dnsRecord))
    }

    @GetMapping("/dns-records")
    fun listDnsRecords(
        @RequestParam tenantId: UUID,
        @RequestParam projectId: UUID
    ): ResponseEntity<List<DnsRecordResponse>> {
        val principal = AuthContextHolder.getRequired()
        authorizationFacade.authorize(
            principal = principal,
            tenantId = tenantId,
            projectId = projectId,
            action = IamAction.NETWORK_DNS_RECORD_LIST,
            resource = "project:$projectId",
            resourceType = "DNS_RECORD",
            resourceId = null
        )
        return ResponseEntity.ok(
            networkEdgeService.listDnsRecords(tenantId, projectId).map(DnsRecordResponse::from)
        )
    }

    @GetMapping("/dns-records/{dnsRecordId}")
    fun getDnsRecord(@PathVariable dnsRecordId: UUID): ResponseEntity<DnsRecordResponse> {
        val principal = AuthContextHolder.getRequired()
        val dnsRecord = networkEdgeService.getDnsRecord(dnsRecordId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = dnsRecord.tenantId,
            projectId = dnsRecord.projectId,
            action = IamAction.NETWORK_DNS_RECORD_READ,
            resource = "network-dns-record:$dnsRecordId",
            resourceType = "DNS_RECORD",
            resourceId = dnsRecordId.toString()
        )
        return ResponseEntity.ok(DnsRecordResponse.from(dnsRecord))
    }

    @DeleteMapping("/dns-records/{dnsRecordId}")
    fun deleteDnsRecord(@PathVariable dnsRecordId: UUID): ResponseEntity<Void> {
        val principal = AuthContextHolder.getRequired()
        val dnsRecord = networkEdgeService.getDnsRecord(dnsRecordId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = dnsRecord.tenantId,
            projectId = dnsRecord.projectId,
            action = IamAction.NETWORK_DNS_RECORD_DELETE,
            resource = "network-dns-record:$dnsRecordId",
            resourceType = "DNS_RECORD",
            resourceId = dnsRecordId.toString()
        )
        networkEdgeService.deleteDnsRecord(dnsRecordId)
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = dnsRecord.tenantId,
            projectId = dnsRecord.projectId,
            action = IamAction.NETWORK_DNS_RECORD_DELETE,
            resourceType = "DNS_RECORD",
            resourceId = dnsRecordId.toString()
        )
        return ResponseEntity.noContent().build()
    }

    private fun buildLoadBalancerResponse(loadBalancer: com.stratuscloud.network.domain.NetworkLoadBalancerEntity): LoadBalancerResponse {
        val listeners = networkEdgeService.listLoadBalancerListeners(requireNotNull(loadBalancer.id)).map(::buildListenerResponse)
        return LoadBalancerResponse.from(loadBalancer, listeners)
    }

    private fun buildListenerResponse(listener: com.stratuscloud.network.domain.NetworkLoadBalancerListenerEntity): LoadBalancerListenerResponse {
        val rules = networkEdgeService.listLoadBalancerRules(requireNotNull(listener.id))
        return LoadBalancerListenerResponse.from(listener, rules)
    }
}
