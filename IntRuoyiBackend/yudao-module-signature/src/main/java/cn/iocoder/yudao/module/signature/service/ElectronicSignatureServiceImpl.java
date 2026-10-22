package cn.iocoder.yudao.module.signature.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureService;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureSubjectAdapter;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureCommand;
import cn.iocoder.yudao.module.signature.api.dto.AuthorizedSignatureIdentity;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
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
        return signInternal(command, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @GxpWriteOperation(operationId = "signature.record.create")
    public ElectronicSignatureResult signAuthorized(ElectronicSignatureCommand command,
                                                     AuthorizedSignatureIdentity identity,
                                                     Runnable authenticateProfile) {
        if (identity == null) throw exception(ESIGN_COMMAND_INVALID, "签名身份不能为空");
        return signInternal(command, identity, authenticateProfile);
    }

    private ElectronicSignatureResult signInternal(ElectronicSignatureCommand command,
                                                     AuthorizedSignatureIdentity identity,
                                                     Runnable authenticateProfile) {
        gxpAuditService.acquireLedgerLock();
        validateCommand(command);
        RegisteredAction registered = findAction(command);
        boolean reasonRequired = registered.adapter().isReasonRequired(command.actionCode(), command.subjectType());
        if (reasonRequired && StrUtil.isBlank(command.reason())) {
            throw exception(ESIGN_COMMAND_INVALID, "当前正式签名动作要求填写原因");
        }
        if (!reasonRequired && StrUtil.isBlank(command.reason())) {
            // Empty represents absence of an optional opinion; it is never fabricated receipt wording.
            command = new ElectronicSignatureCommand(command.moduleCode(), command.actionCode(), command.subjectType(),
                    command.subjectId(), command.expectedSubjectVersion(), command.credential(), "",
                    command.idempotencyKey(), command.businessOccurredAt(), command.businessTimeZone());
        }
        Long actorId = SecurityFrameworkUtils.getLoginUserId();
        if (actorId == null) {
            throw exception(ESIGN_LOGIN_REQUIRED);
        }
        Long operatorId = actorId;
        if (identity != null) actorId = identity.signerId();
        String authenticationMethod = identity == null ? AUTHENTICATION_METHOD
                : AuthorizedSignatureIdentity.EMPLOYEE_PROFILE.equals(identity.domain())
                    ? "SESSION_PLUS_EMPLOYEE_PROFILE_PASSWORD" : "SESSION_PLUS_SELECTED_USER_PASSWORD";
        String identityJson = identity == null ? null : JsonUtils.toJsonString(Map.of(
                "domain", identity.domain(), "signerId", actorId, "displayName", identity.displayName(),
                "operatorId", operatorId, "tenantId", TenantContextHolder.getRequiredTenantId()));
        identityJson = identityJson == null ? null : ElectronicSignatureJsonCanonicalizer.canonicalize(identityJson);
        String commandHash = hash(commandHashPayload(command, actorId)
                + (identityJson == null ? "" : "|" + identityJson));
        ElectronicSignatureRecordDO existingRecord = selectExisting(command.idempotencyKey());
        if (existingRecord != null) {
            if (!Objects.equals(existingRecord.getCommandHash(), commandHash)) {
                throw exception(ESIGN_DUPLICATE_IDEMPOTENCY_KEY);
            }
            return toResult(existingRecord);
        }

        if (identity != null && AuthorizedSignatureIdentity.EMPLOYEE_PROFILE.equals(identity.domain())) {
            if (authenticateProfile == null) throw exception(ESIGN_COMMAND_INVALID, "员工档案凭据验证器不能为空");
            authenticateProfile.run();
        } else {
            if (authenticateProfile != null) throw exception(ESIGN_COMMAND_INVALID, "系统用户不接受档案凭据验证器");
            adminUserApi.reauthenticateForSignature(actorId, command.credential());
        }
        SignatureActionDefinition actionDefinition = registered.definition();
        SignatureSubjectSnapshot snapshot = loadSnapshot(command, actorId, registered);
        var resolution=registered.adapter().serverTimeResolution(new SignatureSubjectCommand(actorId,command.moduleCode(),command.actionCode(),
                command.subjectType(),command.subjectId(),command.expectedSubjectVersion(),command.reason()),snapshot);
        if(resolution==null)throw new IllegalStateException("正式电子签名服务端时间精度策略缺失");
        LocalDateTime signedAt = switch(resolution) {
            case ORIGINAL -> LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
            case MICROS -> LocalDateTime.now(java.time.Clock.tick(java.time.Clock.system(java.time.ZoneId.of("Asia/Shanghai")),java.time.Duration.ofNanos(1000)));
        };
        var authorizedSnapshot=snapshot;
        snapshot=registered.adapter().bindServerSignedAt(new SignatureSubjectCommand(actorId,command.moduleCode(),command.actionCode(),
                command.subjectType(),command.subjectId(),command.expectedSubjectVersion(),command.reason()),authorizedSnapshot,signedAt);
        if(snapshot==null || !Objects.equals(snapshot.subjectType(),authorizedSnapshot.subjectType()) || !Objects.equals(snapshot.subjectId(),authorizedSnapshot.subjectId())
                || !Objects.equals(snapshot.subjectVersion(),authorizedSnapshot.subjectVersion()) || !Objects.equals(snapshot.processInstanceId(),authorizedSnapshot.processInstanceId())
                || !Objects.equals(snapshot.taskId(),authorizedSnapshot.taskId()) || !Objects.equals(snapshot.nodeCode(),authorizedSnapshot.nodeCode())
                || !Objects.equals(snapshot.nodeOrder(),authorizedSnapshot.nodeOrder()) || !Objects.equals(snapshot.beforeContentJson(),authorizedSnapshot.beforeContentJson())
                || !Objects.equals(snapshot.afterContentJson(),authorizedSnapshot.afterContentJson()) || !Objects.equals(snapshot.fieldDiffJson(),authorizedSnapshot.fieldDiffJson()))
            throw new IllegalStateException("服务端签名时间绑定不得替换已授权subject或流程内容身份");
        if(!Objects.equals(snapshot.canonicalContentJson(),authorizedSnapshot.canonicalContentJson())) {
            var original=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(authorizedSnapshot.canonicalContentJson(),Map.class);
            var boundContent=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(snapshot.canonicalContentJson(),Map.class);
            if(original==null || boundContent==null || original.containsKey("serverSignedAt") || original.containsKey("derivedSignatureEvidence"))
                throw new IllegalStateException("签名时间绑定缺少完整已授权内容");
            boundContent=new LinkedHashMap<>(boundContent);boundContent.remove("serverSignedAt");boundContent.remove("derivedSignatureEvidence");
            if(!Objects.equals(original,boundContent))throw new IllegalStateException("签名时间绑定不得替换正文或用户所选业务内容");
        }
        String canonicalContentJson = snapshot.canonicalContentJson();
        if (identityJson != null) {
            Map<String, Object> content = JsonUtils.parseObject(canonicalContentJson, Map.class);
            content.put("signatureIdentity", JsonUtils.parseObject(identityJson, Map.class));
            canonicalContentJson = JsonUtils.toJsonString(content);
        }
        canonicalContentJson = ElectronicSignatureJsonCanonicalizer.canonicalize(canonicalContentJson);
        String beforeContentJson = ElectronicSignatureJsonCanonicalizer.canonicalize(snapshot.beforeContentJson());
        String afterContentJson = ElectronicSignatureJsonCanonicalizer.canonicalize(snapshot.afterContentJson());
        String fieldDiffJson = ElectronicSignatureJsonCanonicalizer.canonicalize(snapshot.fieldDiffJson());
        String contentHash = hash(canonicalContentJson);
        String beforeContentHash = StrUtil.isBlank(beforeContentJson) ? null : hash(beforeContentJson);
        String afterContentHash = StrUtil.isBlank(afterContentJson) ? null : hash(afterContentJson);
        String timeEvidenceId = (resolution==ElectronicSignatureSubjectAdapter.ServerTimeResolution.MICROS ? "SERVER_MICROS_CLOCK:" : "SERVER_CLOCK:") + signedAt;
        String evidenceHash = hash(evidencePayload(command, actorId, actionDefinition, snapshot, signedAt,
                timeEvidenceId, contentHash, beforeContentHash, afterContentHash, beforeContentJson, afterContentJson,
                fieldDiffJson, authenticationMethod));

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
                .authenticationMethod(authenticationMethod)
                .contentHash(contentHash)
                .beforeContentHash(beforeContentHash)
                .afterContentHash(afterContentHash)
                .canonicalContentJson(canonicalContentJson)
                .beforeContentJson(beforeContentJson)
                .afterContentJson(afterContentJson)
                .fieldDiffJson(fieldDiffJson)
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
        gxpAuditService.append(buildGxpAuditCommand(command, snapshot, record, reasonRequired));
        return toResult(record);
    }

    private void validateCommand(ElectronicSignatureCommand command) {
        if (command == null) {
            throw exception(ESIGN_COMMAND_INVALID, "命令不能为空");
        }
        if (StrUtil.hasBlank(command.moduleCode(), command.actionCode(), command.subjectType(), command.subjectId(),
                command.expectedSubjectVersion(), command.credential(), command.idempotencyKey())) {
            throw exception(ESIGN_COMMAND_INVALID, "模块、动作、对象、版本、凭据和幂等键均不能为空");
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

    private RegisteredAction findAction(ElectronicSignatureCommand command) {
        if (CollUtil.isEmpty(subjectAdapters)) {
            throw exception(ESIGN_ACTION_NOT_REGISTERED, command.moduleCode(), command.actionCode());
        }
        var adapters = subjectAdapters.stream().filter(Objects::nonNull)
                .filter(adapter -> Objects.equals(adapter.moduleCode(), command.moduleCode())).toList();
        if (adapters.size() != 1 || CollUtil.isEmpty(adapters.get(0).supportedActions()))
            throw exception(ESIGN_ACTION_NOT_REGISTERED, command.moduleCode(), command.actionCode());
        var actions = adapters.get(0).supportedActions().stream().filter(Objects::nonNull)
                .filter(action -> Objects.equals(action.moduleCode(), command.moduleCode())
                        && Objects.equals(action.actionCode(), command.actionCode())
                        && Objects.equals(action.subjectType(), command.subjectType())).toList();
        if (actions.size() != 1)
            throw exception(ESIGN_ACTION_NOT_REGISTERED, command.moduleCode(), command.actionCode());
        return new RegisteredAction(adapters.get(0), actions.get(0));
    }

    private record RegisteredAction(ElectronicSignatureSubjectAdapter adapter, SignatureActionDefinition definition) {}

    private SignatureSubjectSnapshot loadSnapshot(ElectronicSignatureCommand command, Long actorId,
                                                  RegisteredAction registered) {
        SignatureActionDefinition actionDefinition = registered.definition();
        ElectronicSignatureSubjectAdapter adapter = registered.adapter();
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
                                   String beforeContentHash, String afterContentHash, String beforeContentJson,
                                   String afterContentJson, String fieldDiffJson, String authenticationMethod) {
        return String.join("|", String.valueOf(TenantContextHolder.getRequiredTenantId()), String.valueOf(actorId),
                command.moduleCode(), command.actionCode(), snapshot.subjectType(), snapshot.subjectId(),
                snapshot.subjectVersion(), actionDefinition.meaningCode(), actionDefinition.meaningLabel(),
                command.reason(), signedAt.toString(), timeEvidenceId, authenticationMethod, contentHash,
                StrUtil.nullToEmpty(beforeContentHash), StrUtil.nullToEmpty(afterContentHash),
                StrUtil.nullToEmpty(beforeContentJson), StrUtil.nullToEmpty(afterContentJson),
                StrUtil.nullToEmpty(fieldDiffJson), HASH_ALGORITHM, KEY_VERSION,
                actionDefinition.policyVersion(), VERIFICATION_STATUS_VALID);
    }

    private GxpAuditCommand buildGxpAuditCommand(ElectronicSignatureCommand command,
                                                 SignatureSubjectSnapshot snapshot,
                                                 ElectronicSignatureRecordDO record, boolean reasonRequired) {
        // DCC receipt subject is the complete encoded evidence; its full hash is an exact bounded audit identity.
        // Keep the original subject in the signature record and audit after-state, and preserve existing action identities.
        boolean optionalDccReceipt = !reasonRequired && "DCC".equals(command.moduleCode())
                && "DISTRIBUTION_ACK".equals(command.actionCode()) && "DCC_CONTROLLED_FILE".equals(snapshot.subjectType());
        String auditSubjectId = optionalDccReceipt
                ? snapshot.subjectType() + ":SHA256:" + hash(snapshot.subjectId())
                : snapshot.subjectType() + ":" + snapshot.subjectId();
        String auditIdempotencyKey = optionalDccReceipt ? "DCC_RECEIPT_SIGNATURE:" + hash(command.idempotencyKey()) : "SIGNATURE:" + hash(command.idempotencyKey());
        return GxpAuditCommand.builder()
                .operationId(optionalDccReceipt ? "signature.dcc.distribution-receipt.create" : "signature.record.create")
                .subjectId(auditSubjectId)
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
                .idempotencyKey(auditIdempotencyKey)
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
