package cn.iocoder.yudao.module.dcc.service.file;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Explicit automatic-system actor only for distribution ledger append, restored before returning. */
final class DccDistributionAuditSupport {
    static Object snapshot(Object value) {
        if(value instanceof cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccDistributionBatchDO batch) {
            var state=new java.util.TreeMap<String,Object>();
            state.put("batchId",batch.getId());state.put("tenantId",batch.getTenantId());state.put("sourceEventId",batch.getSourceEventId());
            state.put("eventKey",batch.getEventKey());state.put("controlledFileId",batch.getControlledFileId());state.put("version",batch.getVersionNo());
            state.put("round",batch.getApprovalProcessInstanceId());state.put("eventPayloadHash",batch.getEventPayloadHash());state.put("snapshotPayloadHash",batch.getSnapshotPayloadHash());
            state.put("snapshotAt",batch.getSnapshotAt());state.put("snapshotPolicyVersion",batch.getSnapshotPolicyVersion());
            state.put("state",batch.getStatus());state.put("errorCode",batch.getErrorCode());state.put("errorMessageHash",batch.getErrorMessage()==null?null:digest(batch.getErrorMessage()));
            state.put("errorMessageUtf8Bytes",batch.getErrorMessage()==null?0:batch.getErrorMessage().getBytes(StandardCharsets.UTF_8).length);
            state.put("rowVersion",batch.getRowVersion());state.put("failureAt",batch.getFailureAt());state.put("preparedAt",batch.getDispatchPreparedAt());state.put("sentAt",batch.getSentAt());return state;
        }
        if(value instanceof cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccDistributionDispatchAttemptDO attempt) {
            var state=new java.util.TreeMap<String,Object>();state.put("attemptId",attempt.getId());state.put("batchId",attempt.getBatchId());state.put("jobId",attempt.getMessageJobId());
            state.put("tenantId",attempt.getTenantId());state.put("number",attempt.getAttemptNo());state.put("businessKey",attempt.getBusinessKey());state.put("userId",attempt.getRecipientUserId());
            state.put("template",attempt.getTemplateCode());state.put("state",attempt.getStatus());state.put("platformMessageId",attempt.getPlatformMessageId());
            state.put("startedAt",attempt.getStartedAt());state.put("finishedAt",attempt.getFinishedAt());state.put("errorCode",attempt.getErrorCode());
            state.put("errorMessageHash",attempt.getErrorMessage()==null?null:digest(attempt.getErrorMessage()));
            state.put("errorMessageUtf8Bytes",attempt.getErrorMessage()==null?0:attempt.getErrorMessage().getBytes(StandardCharsets.UTF_8).length);return state;
        }
        return value;
    }
    static String failure(RuntimeException error) {
        String reason=error.getMessage()==null?error.getClass().getSimpleName():error.getMessage();
        reason=reason.replaceAll("(?i)(password|passwd|pwd|token|secret|authorization|api[_-]?key)\\s*[:=]\\s*[^\\s,;]+","$1=[REDACTED]")
                .replaceAll("(?i)Bearer\\s+[^\\s,;]+","Bearer [REDACTED]");
        if(reason.getBytes(StandardCharsets.UTF_8).length>65535)
            throw new IllegalStateException("下发失败原因超出TEXT的65535 UTF-8字节容量，未截断",error);
        return reason;
    }
    static String digest(Object payload){return cn.hutool.crypto.digest.DigestUtil.sha256Hex(JsonUtils.toJsonString(payload));}
    static void append(GxpAuditService audit,String operation,Long id,Long sequence,String before,String after,
                       Object beforePayload,Object afterPayload,String reason,String key) {
        if(!TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("下发账本与审计必须同事务");
        Long tenant=TenantContextHolder.getRequiredTenantId();
        var original=SecurityContextHolder.getContext();
        var context=SecurityContextHolder.createEmptyContext();
        var actor=new LoginUser().setId(-1L).setTenantId(tenant).setUserType(2)
                .setInfo(Map.of("username","SYSTEM_ACTOR",LoginUser.INFO_KEY_NICKNAME,"SYSTEM_ACTOR"));
        context.setAuthentication(new UsernamePasswordAuthenticationToken(actor,null));SecurityContextHolder.setContext(context);
        try {
            String namespace=operation.startsWith("dcc.distribution.batch.") || afterPayload instanceof cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccDistributionBatchDO
                    ? "DCC_DISTRIBUTION_BATCH" : "DCC_DISTRIBUTION_ATTEMPT";
            audit.append(GxpAuditCommand.builder().operationId(operation).subjectId(namespace+":"+tenant+":"+id)
                    .subjectVersion(String.valueOf(sequence)).reason(reason)
                    .beforeState(GxpAuditStateEnvelope.builder().state(before).objectVersion(String.valueOf(sequence))
                            .canonicalJson(JsonUtils.toJsonString(snapshot(beforePayload))).build())
                    .afterState(GxpAuditStateEnvelope.builder().state(after).objectVersion(String.valueOf(sequence))
                            .canonicalJson(JsonUtils.toJsonString(snapshot(afterPayload))).build())
                    .idempotencyKey("DCC_DIST:"+digest(key)).source(operation).build());
        } finally {SecurityContextHolder.setContext(original);}
    }
}
