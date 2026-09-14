package cn.iocoder.yudao.module.signature.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureService;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureSubjectAdapter;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureCommand;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureResult;
import cn.iocoder.yudao.module.signature.api.dto.SignatureActionDefinition;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectSnapshot;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditStateEnvelope;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpWriteOperation;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.signature.enums.SignatureErrorCodeConstants.*;

@Service
public class ElectronicSignatureServiceImpl implements ElectronicSignatureService {

    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String KEY_VERSION = "system-local-v1";
    private static final String VERIFICATION_STATUS_VALID = "VALID";
    private static final String AUTHENTICATION_METHOD = "SESSION_PLUS_PASSWORD";

    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private ElectronicSignatureRecordMapper signatureRecordMapper;
    @Resource
    private GxpAuditService gxpAuditService;
    @Resource
    private List<ElectronicSignatureSubjectAdapter> subjectAdapters;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @GxpWriteOperation(operationId = "signature.record.create")
    public ElectronicSignatureResult sign(ElectronicSignatureCommand command) {
        validateCommand(command);
        Long actorId = SecurityFrameworkUtils.getLoginUserId();
        if (actorId == null) {
            throw exception(ESIGN_LOGIN_REQUIRED);
        }
        String commandHash = hash(commandHashPayload(command, actorId));
        ElectronicSignatureRecordDO existingRecord = selectExisting(command.idempotencyKey());
        if (existingRecord != null) {
            if (!Objects.equals(existingRecord.getCommandHash(), commandHash)) {
                throw exception(ESIGN_DUPLICATE_IDEMPOTENCY_KEY);
            }
            return toResult(existingRecord);
        }

        adminUserApi.reauthenticateForSignature(actorId, command.credential());
        SignatureActionDefinition actionDefinition = findAction(command);
        SignatureSubjectSnapshot snapshot = loadSnapshot(command, actorId, actionDefinition);
        LocalDateTime signedAt = LocalDateTime.now();
        String contentHash = hash(snapshot.canonicalContentJson());
        String beforeContentHash = StrUtil.isBlank(snapshot.beforeContentJson()) ? null : hash(snapshot.beforeContentJson());
        String afterContentHash = StrUtil.isBlank(snapshot.afterContentJson()) ? null : hash(snapshot.afterContentJson());
        String timeEvidenceId = "SERVER_CLOCK:" + signedAt;
        String evidenceHash = hash(evidencePayload(command, actorId, actionDefinition, snapshot, signedAt,
                timeEvidenceId, contentHash, beforeContentHash, afterContentHash));

        ElectronicSignatureRecordDO record = ElectronicSignatureRecordDO.builder()
                .moduleCode(command.moduleCode())
                .actionCode(command.actionCode())
                .subjectType(snapshot.subjectType())
                .subjectId(snapshot.subjectId())
                .subjectVersion(snapshot.subjectVersion())
                .actorId(actorId)
                .meaningCode(actionDefinition.meaningCode())
                .meaningLabel(actionDefinition.meaningLabel())
                .reason(command.reason())
                .signedAt(signedAt)
                .timeEvidenceId(timeEvidenceId)
                .authenticationMethod(AUTHENTICATION_METHOD)
                .contentHash(contentHash)
                .beforeContentHash(beforeContentHash)
                .afterContentHash(afterContentHash)
                .canonicalContentJson(snapshot.canonicalContentJson())
                .beforeContentJson(snapshot.beforeContentJson())
                .afterContentJson(snapshot.afterContentJson())
                .fieldDiffJson(snapshot.fieldDiffJson())
                .evidenceHash(evidenceHash)
                .algorithm(HASH_ALGORITHM)
                .keyVersion(KEY_VERSION)
                .policyVersion(actionDefinition.policyVersion())
                .verificationStatus(VERIFICATION_STATUS_VALID)
                .idempotencyKey(command.idempotencyKey())
                .commandHash(commandHash)
                .processInstanceId(snapshot.processInstanceId())
                .taskId(snapshot.taskId())
                .nodeCode(snapshot.nodeCode())
                .nodeOrder(snapshot.nodeOrder())
                .build();
        record.setTenantId(TenantContextHolder.getRequiredTenantId());
        signatureRecordMapper.insert(record);
        gxpAuditService.append(buildGxpAuditCommand(command, snapshot, record));
        return toResult(record);
    }

    private void validateCommand(ElectronicSignatureCommand command) {
        if (command == null) {
            throw exception(ESIGN_COMMAND_INVALID, "命令不能为空");
        }
        if (StrUtil.hasBlank(command.moduleCode(), command.actionCode(), command.subjectType(), command.subjectId(),
                command.expectedSubjectVersion(), command.credential(), command.reason(), command.idempotencyKey())) {
            throw exception(ESIGN_COMMAND_INVALID, "模块、动作、对象、版本、凭据、原因和幂等键均不能为空");
        }
        if (command.businessOccurredAt() != null && StrUtil.isBlank(command.businessTimeZone())) {
            throw exception(ESIGN_COMMAND_INVALID, "业务发生时间存在时业务时区不能为空");
        }
    }

    private ElectronicSignatureRecordDO selectExisting(String idempotencyKey) {
        return signatureRecordMapper.selectOne(new LambdaQueryWrapper<ElectronicSignatureRecordDO>()
                .eq(ElectronicSignatureRecordDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .eq(ElectronicSignatureRecordDO::getIdempotencyKey, idempotencyKey));
    }

    private SignatureActionDefinition findAction(ElectronicSignatureCommand command) {
        if (CollUtil.isEmpty(subjectAdapters)) {
            throw exception(ESIGN_ACTION_NOT_REGISTERED, command.moduleCode(), command.actionCode());
        }
        return subjectAdapters.stream()
                .filter(adapter -> Objects.equals(adapter.moduleCode(), command.moduleCode()))
                .flatMap(adapter -> adapter.supportedActions().stream())
                .filter(action -> Objects.equals(action.actionCode(), command.actionCode())
                        && Objects.equals(action.subjectType(), command.subjectType()))
                .findFirst()
                .orElseThrow(() -> exception(ESIGN_ACTION_NOT_REGISTERED, command.moduleCode(), command.actionCode()));
    }

    private SignatureSubjectSnapshot loadSnapshot(ElectronicSignatureCommand command, Long actorId,
                                                  SignatureActionDefinition actionDefinition) {
        ElectronicSignatureSubjectAdapter adapter = subjectAdapters.stream()
                .filter(item -> Objects.equals(item.moduleCode(), command.moduleCode()))
                .findFirst()
                .orElseThrow(() -> exception(ESIGN_ACTION_NOT_REGISTERED, command.moduleCode(), command.actionCode()));
        SignatureSubjectSnapshot snapshot = adapter.loadAndAuthorize(new SignatureSubjectCommand(actorId,
                command.moduleCode(), command.actionCode(), command.subjectType(), command.subjectId(),
                command.expectedSubjectVersion(), command.reason()));
        if (snapshot == null) {
            throw exception(ESIGN_SUBJECT_NOT_SIGNABLE, "签名主题快照不能为空");
        }
        if (StrUtil.hasBlank(snapshot.subjectType(), snapshot.subjectId(), snapshot.subjectVersion(),
                snapshot.canonicalContentJson())) {
            throw exception(ESIGN_SUBJECT_NOT_SIGNABLE, "签名主题类型、编号、版本和规范化内容不能为空");
        }
        if (!Objects.equals(snapshot.subjectType(), actionDefinition.subjectType())
                || !Objects.equals(snapshot.subjectId(), command.subjectId())
                || !Objects.equals(snapshot.subjectVersion(), command.expectedSubjectVersion())) {
            throw exception(ESIGN_SUBJECT_NOT_SIGNABLE, "签名主题与请求版本不一致");
        }
        return snapshot;
    }

    private String commandHashPayload(ElectronicSignatureCommand command, Long actorId) {
        return String.join("|", String.valueOf(TenantContextHolder.getRequiredTenantId()), String.valueOf(actorId),
                command.moduleCode(), command.actionCode(), command.subjectType(), command.subjectId(),
                command.expectedSubjectVersion(), command.reason(), command.idempotencyKey(),
                StrUtil.nullToEmpty(command.businessOccurredAt()), StrUtil.nullToEmpty(command.businessTimeZone()));
    }

    private String evidencePayload(ElectronicSignatureCommand command, Long actorId,
                                   SignatureActionDefinition actionDefinition, SignatureSubjectSnapshot snapshot,
                                   LocalDateTime signedAt, String timeEvidenceId, String contentHash,
                                   String beforeContentHash, String afterContentHash) {
        return String.join("|", String.valueOf(TenantContextHolder.getRequiredTenantId()), String.valueOf(actorId),
                command.moduleCode(), command.actionCode(), snapshot.subjectType(), snapshot.subjectId(),
                snapshot.subjectVersion(), actionDefinition.meaningCode(), actionDefinition.meaningLabel(),
                command.reason(), signedAt.toString(), timeEvidenceId, AUTHENTICATION_METHOD, contentHash,
                StrUtil.nullToEmpty(beforeContentHash), StrUtil.nullToEmpty(afterContentHash),
                StrUtil.nullToEmpty(snapshot.beforeContentJson()), StrUtil.nullToEmpty(snapshot.afterContentJson()),
                StrUtil.nullToEmpty(snapshot.fieldDiffJson()), HASH_ALGORITHM, KEY_VERSION,
                actionDefinition.policyVersion(), VERIFICATION_STATUS_VALID);
    }

    private GxpAuditCommand buildGxpAuditCommand(ElectronicSignatureCommand command,
                                                 SignatureSubjectSnapshot snapshot,
                                                 ElectronicSignatureRecordDO record) {
        return GxpAuditCommand.builder()
                .operationId("signature.record.create")
                .subjectId(snapshot.subjectType() + ":" + snapshot.subjectId())
                .subjectVersion(snapshot.subjectVersion())
                .reason(command.reason())
                .beforeState(GxpAuditStateEnvelope.builder()
                        .state("NO_SIGNATURE_RECORD")
                        .objectVersion(snapshot.subjectVersion())
                        .canonicalJson("{}")
                        .build())
                .afterState(GxpAuditStateEnvelope.builder()
                        .state("ELECTRONIC_SIGNATURE_RECORDED")
                        .objectVersion(snapshot.subjectVersion())
                        .canonicalJson(gxpAuditAfterStateJson(command, snapshot, record))
                        .build())
                .idempotencyKey(command.idempotencyKey())
                .source("ElectronicSignatureServiceImpl.sign")
                .signatureRecordId(String.valueOf(record.getId()))
                .signatureContentHash(record.getContentHash())
                .build();
    }

    private String gxpAuditAfterStateJson(ElectronicSignatureCommand command,
                                          SignatureSubjectSnapshot snapshot,
                                          ElectronicSignatureRecordDO record) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("signatureId", record.getId());
        payload.put("moduleCode", command.moduleCode());
        payload.put("actionCode", command.actionCode());
        payload.put("subjectType", snapshot.subjectType());
        payload.put("subjectId", snapshot.subjectId());
        payload.put("subjectVersion", snapshot.subjectVersion());
        payload.put("actorId", record.getActorId());
        payload.put("meaningCode", record.getMeaningCode());
        payload.put("meaningLabel", record.getMeaningLabel());
        payload.put("reason", record.getReason());
        payload.put("signedAt", record.getSignedAt());
        payload.put("authenticationMethod", record.getAuthenticationMethod());
        payload.put("contentHash", record.getContentHash());
        payload.put("beforeContentHash", record.getBeforeContentHash());
        payload.put("afterContentHash", record.getAfterContentHash());
        payload.put("evidenceHash", record.getEvidenceHash());
        payload.put("policyVersion", record.getPolicyVersion());
        payload.put("verificationStatus", record.getVerificationStatus());
        payload.put("processInstanceId", record.getProcessInstanceId());
        payload.put("taskId", record.getTaskId());
        payload.put("nodeCode", record.getNodeCode());
        payload.put("nodeOrder", record.getNodeOrder());
        return cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(payload);
    }

    private String hash(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] bytes = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", ex);
        }
    }

    private ElectronicSignatureResult toResult(ElectronicSignatureRecordDO record) {
        return new ElectronicSignatureResult(record.getId(), record.getVerificationStatus(), record.getSignedAt(),
                record.getTimeEvidenceId(), record.getSubjectVersion(), record.getContentHash(),
                record.getEvidenceHash(), record.getAlgorithm(), record.getKeyVersion());
    }

}
