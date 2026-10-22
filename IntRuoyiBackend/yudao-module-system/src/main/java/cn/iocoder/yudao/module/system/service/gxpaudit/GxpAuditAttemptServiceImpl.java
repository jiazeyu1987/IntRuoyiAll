package cn.iocoder.yudao.module.system.service.gxpaudit;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.GXP_AUDIT_ATTEMPT_INVALID;

@Service
public class GxpAuditAttemptServiceImpl implements GxpAuditAttemptService {

    @Resource
    private GxpAuditService gxpAuditService;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public GxpAuditAppendResult record(GxpAuditAttemptCommand command) {
        if (command == null || StrUtil.isBlank(command.attemptedOperationId())
                || StrUtil.isBlank(command.serverAttemptId())
                || !List.of("FAILED", "DENIED").contains(command.resultStatus())
                || StrUtil.isBlank(command.errorCode()) || StrUtil.isBlank(command.requestId())) {
            throw exception(GXP_AUDIT_ATTEMPT_INVALID, "command");
        }
        String subjectId = StrUtil.blankToDefault(command.subjectId(),
                "REQUEST_ATTEMPT:" + command.serverAttemptId());
        String subjectVersion = StrUtil.blankToDefault(command.subjectVersion(), command.serverAttemptId());
        return gxpAuditService.append(GxpAuditCommand.builder()
                .operationId("gxp.attempt.record")
                .subjectId(subjectId)
                .subjectVersion(subjectVersion)
                .reason(StrUtil.blankToDefault(command.reason(), command.errorCode()))
                .resultStatus(command.resultStatus())
                .errorCode(command.errorCode())
                .attemptedOperationId(command.attemptedOperationId())
                .reasonCode("SYSTEM_ERROR")
                .reasonSource("SYSTEM")
                .beforeState(GxpAuditStateEnvelope.builder().state("ABSENT").canonicalJson("null").build())
                .afterState(GxpAuditStateEnvelope.builder().state("PRESENT")
                        .objectVersion(command.serverAttemptId())
                        .canonicalJson("{\"attemptId\":\"" + command.serverAttemptId() + "\"}").build())
                .idempotencyKey("GXP2:ATTEMPT:" + command.serverAttemptId())
                .requestId(command.requestId())
                .sourceType(StrUtil.blankToDefault(command.sourceType(), "SERVICE_METHOD"))
                .sourceLocator(StrUtil.blankToDefault(command.sourceLocator(),
                        "GxpAuditAttemptServiceImpl#record"))
                .build());
    }
}
