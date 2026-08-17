package cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAuditDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAuditMapper;
import org.springframework.stereotype.Service;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_AUDIT_EVENT_CONFLICT;

@Service
public class DccRegistrationCertificateTerminalAuditService {

    private final DccRegistrationCertificateAuditMapper auditMapper;
    private final DccRegistrationCertificateBusinessClock businessClock;

    public DccRegistrationCertificateTerminalAuditService(
            DccRegistrationCertificateAuditMapper auditMapper,
            DccRegistrationCertificateBusinessClock businessClock) {
        this.auditMapper = require(auditMapper, "auditMapper");
        this.businessClock = require(businessClock, "businessClock");
    }

    public DccRegistrationCertificateAuditDO find(Long tenantId, String eventKey) {
        return auditMapper.selectByTenantIdAndEventKey(tenantId, eventKey);
    }

    public void recordSuccess(DccRegistrationCertificateCommandMetadata metadata,
                              DccRegistrationCertificateCommandContext context,
                              Long versionId, Long snapshotId, Long businessFileId) {
        if (context.ownerCompanyId() == null || context.certificateId() == null) {
            throw new IllegalStateException("successful registration certificate audit requires trusted identity");
        }
        DccRegistrationCertificateAuditDetail detail = new DccRegistrationCertificateAuditDetail(
                metadata.commandKind(), metadata.actorId(), metadata.payloadHash(), context.certificateId(), null, null);
        insert(DccRegistrationCertificateAuditDO.builder()
                .tenantId(metadata.tenantId())
                .ownerCompanyId(context.ownerCompanyId())
                .certificateId(context.certificateId())
                .requestedOwnerCompanyId(context.requestedOwnerCompanyId())
                .requestedCertificateId(context.requestedCertificateId())
                .versionId(versionId)
                .snapshotId(snapshotId)
                .businessFileId(businessFileId)
                .eventKey(metadata.idempotencyKey())
                .eventType(metadata.commandKind() + "_SUCCEEDED")
                .actorId(metadata.actorId())
                .result("SUCCESS")
                .resultCode("OK")
                .requestTraceId(metadata.requestTraceId())
                .detailJson(JsonUtils.toJsonString(detail))
                .occurredAt(businessClock.now())
                .creator(String.valueOf(metadata.actorId()))
                .build());
    }

    void insert(DccRegistrationCertificateAuditDO audit) {
        if (auditMapper.insert(audit) != 1) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_AUDIT_EVENT_CONFLICT);
        }
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return value;
    }
}
