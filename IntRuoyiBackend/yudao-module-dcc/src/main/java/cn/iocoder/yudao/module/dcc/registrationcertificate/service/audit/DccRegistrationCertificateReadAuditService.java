package cn.iocoder.yudao.module.dcc.registrationcertificate.service.audit;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAuditDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAuditMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Map;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_AUDIT_EVENT_CONFLICT;

@Service
public class DccRegistrationCertificateReadAuditService {

    private final DccRegistrationCertificateAuditMapper auditMapper;
    private final DccRegistrationCertificateBusinessClock businessClock;

    public DccRegistrationCertificateReadAuditService(
            DccRegistrationCertificateAuditMapper auditMapper,
            DccRegistrationCertificateBusinessClock businessClock) {
        this.auditMapper = require(auditMapper, "auditMapper");
        this.businessClock = require(businessClock, "businessClock");
    }

    public void record(DccRegistrationCertificateReadAuditCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("read audit command is required");
        }
        String traceId = requireText(command.requestTraceId(), "requestTraceId");
        String operation = requireText(command.operation(), "operation");
        String result = requireText(command.result(), "result");
        String eventKey = eventKey(traceId, operation, command.certificateId(),
                command.requestedCertificateId(), result);
        DccRegistrationCertificateAuditDO audit = DccRegistrationCertificateAuditDO.builder()
                .tenantId(command.tenantId())
                .ownerCompanyId(command.ownerCompanyId())
                .certificateId(command.certificateId())
                .requestedOwnerCompanyId(command.requestedOwnerCompanyId())
                .requestedCertificateId(command.requestedCertificateId())
                .versionId(command.versionId())
                .snapshotId(command.snapshotId())
                .businessFileId(command.businessFileId())
                .eventKey(eventKey)
                .eventType(operation + ("SUCCESS".equals(result) ? "_SUCCEEDED" : "_FAILED"))
                .actorId(command.actorId())
                .result(result)
                .resultCode(command.resultCode())
                .requestTraceId(traceId)
                .detailJson(StrUtil.blankToDefault(command.detailJson(),
                        JsonUtils.toJsonString(Map.of("operation", operation, "result", result))))
                .occurredAt(businessClock.now())
                .creator(command.actorId() == null ? null : String.valueOf(command.actorId()))
                .build();
        try {
            if (auditMapper.insert(audit) != 1) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_AUDIT_EVENT_CONFLICT);
            }
        } catch (DuplicateKeyException duplicate) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_AUDIT_EVENT_CONFLICT);
        }
    }

    private static String eventKey(String traceId, String operation, Long certificateId,
                                   Long requestedCertificateId, String result) {
        if ("SUCCESS".equals(result)) {
            if (certificateId == null || certificateId <= 0) {
                throw new IllegalArgumentException("successful read audit requires certificateId");
            }
            return traceId + ":" + operation + ":CERTIFICATE:" + certificateId + ":SUCCESS";
        }
        if (!"FAILURE".equals(result)) {
            throw new IllegalArgumentException("read audit result must be SUCCESS or FAILURE");
        }
        Long requested = requestedCertificateId != null ? requestedCertificateId : certificateId;
        if (requested == null || requested <= 0) {
            throw new IllegalArgumentException("failed read audit requires requested certificate identity");
        }
        return traceId + ":" + operation + ":REQUESTED:" + requested + ":FAILURE";
    }

    private static String requireText(String value, String name) {
        if (StrUtil.isBlank(value)) {
            throw new IllegalArgumentException("registration certificate read audit " + name + " is required");
        }
        return StrUtil.trim(value);
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
