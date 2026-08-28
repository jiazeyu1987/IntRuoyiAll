package cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAuditDO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DccRegistrationCertificateFailureAuditService {

    private final DccRegistrationCertificateTerminalAuditService terminalAuditService;
    private final DccRegistrationCertificateBusinessClock businessClock;

    public DccRegistrationCertificateFailureAuditService(
            DccRegistrationCertificateTerminalAuditService terminalAuditService,
            DccRegistrationCertificateBusinessClock businessClock) {
        this.terminalAuditService = terminalAuditService;
        this.businessClock = businessClock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void recordFailure(DccRegistrationCertificateCommandMetadata metadata,
                              DccRegistrationCertificateCommandContext context,
                              Integer failureCode, String failureMessage) {
        DccRegistrationCertificateAuditDetail detail = new DccRegistrationCertificateAuditDetail(
                metadata.commandKind(), metadata.actorId(), metadata.payloadHash(), null,
                null, null, null, failureCode, failureMessage);
        terminalAuditService.insert(DccRegistrationCertificateAuditDO.builder()
                .tenantId(metadata.tenantId())
                .ownerCompanyId(context.ownerCompanyId())
                .certificateId(context.certificateId())
                .requestedOwnerCompanyId(context.requestedOwnerCompanyId())
                .requestedCertificateId(context.requestedCertificateId())
                .eventKey(metadata.idempotencyKey())
                .eventType(metadata.commandKind() + "_FAILED")
                .actorId(metadata.actorId())
                .result("FAILURE")
                .resultCode(String.valueOf(failureCode))
                .requestTraceId(metadata.requestTraceId())
                .detailJson(JsonUtils.toJsonString(detail))
                .occurredAt(businessClock.now())
                .creator(String.valueOf(metadata.actorId()))
                .build());
    }
}
