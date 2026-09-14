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
        record(command, false);
    }

    public void recordRepeatableListRead(DccRegistrationCertificateReadAuditCommand command) {
        record(command, true);
    }

    private void record(DccRegistrationCertificateReadAuditCommand command, boolean repeatableListRead) {
        if (command == null) {
            throw new IllegalArgumentException("读取审计命令不能为空");
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
            if (repeatableListRead && isSuccessfulListRead(operation, result)) {
                return;
            }
            throw new ServiceException(REGISTRATION_CERTIFICATE_AUDIT_EVENT_CONFLICT);
        }
    }

    private static boolean isSuccessfulListRead(String operation, String result) {
        return "SUCCESS".equals(result) && ("PAGE".equals(operation) || "OLD_INDEX".equals(operation));
    }

    private static String eventKey(String traceId, String operation, Long certificateId,
                                   Long requestedCertificateId, String result) {
        if ("SUCCESS".equals(result)) {
            if (certificateId == null || certificateId <= 0) {
                throw new IllegalArgumentException("成功的读取审计必须包含注册证 ID");
            }
            return traceId + ":" + operation + ":CERTIFICATE:" + certificateId + ":SUCCESS";
        }
        if (!"FAILURE".equals(result)) {
            throw new IllegalArgumentException("读取审计结果必须为成功或失败状态");
        }
        Long requested = requestedCertificateId != null ? requestedCertificateId : certificateId;
        if (requested == null || requested <= 0) {
            throw new IllegalArgumentException("失败的读取审计必须包含请求的注册证身份");
        }
        return traceId + ":" + operation + ":REQUESTED:" + requested + ":FAILURE";
    }

    private static String requireText(String value, String name) {
        if (StrUtil.isBlank(value)) {
            throw new IllegalArgumentException("注册证读取审计参数 " + name + " 不能为空");
        }
        return StrUtil.trim(value);
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + "不能为空");
        }
        return value;
    }
}
