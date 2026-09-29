package cn.iocoder.yudao.module.system.service.gxpaudit;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditLedgerSequenceDO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyActivationDO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyOperationDO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyVersionDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditLedgerSequenceMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventMapper;
import cn.iocoder.yudao.module.system.dal.mysql.tenant.TenantMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditPolicyActivationMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditPolicyOperationMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditPolicyVersionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.GXP_AUDIT_POLICY_ACTIVATION_INVALID;

@Service
public class GxpAuditPolicyActivationServiceImpl implements GxpAuditPolicyActivationService {

    @Resource
    private GxpAuditPolicyBundleLoader bundleLoader;
    @Resource
    private GxpAuditPolicyVersionMapper policyVersionMapper;
    @Resource
    private GxpAuditPolicyOperationMapper policyOperationMapper;
    @Resource
    private GxpAuditPolicyActivationMapper activationMapper;
    @Resource
    private GxpAuditLedgerSequenceMapper ledgerSequenceMapper;
    @Resource
    private GxpAuditService gxpAuditService;
    @Resource
    private TenantMapper tenantMapper;
    @Resource
    private GxpAuditEventMapper auditEventMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GxpAuditPolicyActivationResult activate(GxpAuditPolicyActivationCommand command) {
        validate(command);
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (!Objects.equals(tenantId, command.tenantId())) {
            throw exception(GXP_AUDIT_POLICY_ACTIVATION_INVALID, "tenant");
        }
        LoginUser actor = SecurityFrameworkUtils.getLoginUser();
        if (actor == null || actor.getId() == null || !Objects.equals(actor.getId(), command.actorId())) {
            throw exception(GXP_AUDIT_POLICY_ACTIVATION_INVALID, "actor");
        }
        GxpAuditPolicyBundle bundle = bundleLoader.load();
        if (!bundle.approved()) {
            throw exception(GXP_AUDIT_POLICY_ACTIVATION_INVALID, "policy-status-not-approved");
        }
        if (!Objects.equals(bundle.approvalReference(), command.approvalReference())) {
            throw exception(GXP_AUDIT_POLICY_ACTIVATION_INVALID, "approval-reference");
        }

        if (tenantMapper.selectByIdForUpdate(tenantId) == null) {
            throw exception(GXP_AUDIT_POLICY_ACTIVATION_INVALID, "tenant-not-found");
        }
        GxpAuditLedgerSequenceDO ledger = ledgerSequenceMapper.selectByTenantIdForUpdate(tenantId);
        if (ledger == null) {
            if (auditEventMapper.selectAnyByTenantForUpdate(tenantId) != null) {
                throw exception(GXP_AUDIT_POLICY_ACTIVATION_INVALID, "events-without-ledger-watermark");
            }
            ledger = new GxpAuditLedgerSequenceDO();
            ledger.setTenantId(tenantId);
            ledger.setNextLedgerSequence(1L);
            if (ledgerSequenceMapper.insert(ledger) != 1) {
                throw exception(GXP_AUDIT_POLICY_ACTIVATION_INVALID, "ledger-initialization");
            }
        }
        if (ledger.getNextLedgerSequence() == null || ledger.getNextLedgerSequence() < 1L) {
            throw exception(GXP_AUDIT_POLICY_ACTIVATION_INVALID, "ledger-watermark");
        }
        GxpAuditPolicyActivationDO existing = activationMapper.selectByRequestIdForUpdate(tenantId, command.requestId());
        if (existing != null) {
            if (!Objects.equals(existing.getTenantId(), tenantId)
                    || !Objects.equals(existing.getRequestId(), command.requestId())
                    || !Objects.equals(existing.getActorId(), command.actorId())
                    || !Objects.equals(existing.getPolicyVersion(), bundle.policyVersion())
                    || !Objects.equals(existing.getPolicyHash(), bundle.policyHash())
                    || !Objects.equals(existing.getApprovalReference(), command.approvalReference())) {
                throw exception(GXP_AUDIT_POLICY_ACTIVATION_INVALID, "activation-request-payload-conflict");
            }
            validateReplayVersion(tenantId, existing, command, bundle);
            return new GxpAuditPolicyActivationResult(existing.getId(), existing.getPolicyVersion(),
                    existing.getPolicyHash(), true);
        }

        insertPolicyVersion(tenantId, command, bundle);
        List<GxpAuditPolicyOperationDO> operations = parseOperations(tenantId, bundle);
        for (GxpAuditPolicyOperationDO operation : operations) {
            GxpAuditPolicyOperationDO existingOperation = policyOperationMapper.selectByPolicyVersionForUpdate(
                    tenantId, bundle.policyVersion(), operation.getOperationId());
            if (existingOperation == null && policyOperationMapper.insert(operation) != 1) {
                throw exception(GXP_AUDIT_POLICY_ACTIVATION_INVALID, "policy-operation");
            }
        }

        GxpAuditPolicyActivationDO previous = activationMapper.selectLatestForUpdate(tenantId);
        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("approvalReference", command.approvalReference());
        canonical.put("effectiveAfterSequence", ledger.getNextLedgerSequence() - 1L);
        canonical.put("policyHash", bundle.policyHash());
        canonical.put("policyVersion", bundle.policyVersion());
        canonical.put("previousActivationHash", previous == null ? null : previous.getActivationHash());
        canonical.put("requestId", command.requestId());
        canonical.put("tenantId", tenantId);
        String canonicalJson = JsonUtils.toJsonString(canonical);

        GxpAuditPolicyActivationDO activation = new GxpAuditPolicyActivationDO();
        activation.setTenantId(tenantId);
        activation.setPolicyVersion(bundle.policyVersion());
        activation.setPolicyHash(bundle.policyHash());
        activation.setPreviousActivationHash(previous == null ? null : previous.getActivationHash());
        activation.setRequestId(command.requestId());
        activation.setActorId(command.actorId());
        activation.setApprovalReference(command.approvalReference());
        activation.setActivatedAtUtc(LocalDateTime.now(ZoneOffset.UTC));
        activation.setEffectiveAfterSequence(ledger.getNextLedgerSequence() - 1L);
        activation.setCanonicalActivationJson(canonicalJson);
        activation.setActivationHash(DigestUtil.sha256Hex(canonicalJson));
        if (activationMapper.insert(activation) != 1) {
            throw exception(GXP_AUDIT_POLICY_ACTIVATION_INVALID, "activation-insert");
        }

        gxpAuditService.append(GxpAuditCommand.builder()
                .operationId("gxp.policy.activate")
                .subjectId("GXP_POLICY:" + tenantId)
                .subjectVersion(bundle.policyVersion())
                .reason(command.approvalReference())
                .beforeState(GxpAuditStateEnvelope.builder()
                        .state(previous == null ? "ABSENT" : "PRESENT")
                        .objectVersion(previous == null ? null : previous.getPolicyVersion())
                        .canonicalJson(previous == null ? "null" : previous.getCanonicalActivationJson())
                        .build())
                .afterState(GxpAuditStateEnvelope.builder()
                        .state("PRESENT")
                        .objectVersion(bundle.policyVersion())
                        .canonicalJson(canonicalJson)
                        .build())
                .idempotencyKey("GXP2:" + DigestUtil.sha256Hex("{" + tenantId + ":gxp.policy.activate:"
                        + bundle.policyHash() + ":" + command.requestId() + "}"))
                .requestId(command.requestId())
                .source("GxpAuditPolicyActivationServiceImpl.activate")
                .sourceType("MIGRATION")
                .sourceLocator("GxpAuditPolicyActivationServiceImpl#activate")
                .links(List.of(new GxpAuditRelation("ACTIVE_POLICY", "GXP_POLICY_ACTIVATION",
                        String.valueOf(activation.getId()), bundle.policyVersion(), activation.getActivationHash())))
                .build());
        return new GxpAuditPolicyActivationResult(activation.getId(), bundle.policyVersion(),
                bundle.policyHash(), false);
    }

    private void validateReplayVersion(Long tenantId, GxpAuditPolicyActivationDO activation,
                                       GxpAuditPolicyActivationCommand command, GxpAuditPolicyBundle bundle) {
        GxpAuditPolicyVersionDO version = policyVersionMapper.selectOne(
                new LambdaQueryWrapper<GxpAuditPolicyVersionDO>()
                        .eq(GxpAuditPolicyVersionDO::getTenantId, tenantId)
                        .eq(GxpAuditPolicyVersionDO::getPolicyVersion, activation.getPolicyVersion())
                        .last("FOR UPDATE"));
        // Only compare existing immutable evidence. Missing historical facts are not reconstructed.
        // This bounded check does not provide a per-activation payload hash or concurrent replay protocol.
        if (version == null
                || !Objects.equals(version.getTenantId(), tenantId)
                || !Objects.equals(version.getPolicyVersion(), bundle.policyVersion())
                || StrUtil.isBlank(version.getPolicyHash())
                || !Objects.equals(version.getPolicyHash(), bundle.policyHash())
                || !Objects.equals(version.getApprovalReference(), command.approvalReference())
                || StrUtil.isBlank(version.getCoverageReportHash())
                || !Objects.equals(version.getCoverageReportHash(), command.coverageReportHash())
                || StrUtil.isBlank(version.getArtifactHash())
                || !Objects.equals(version.getArtifactHash(), bundle.artifactHash())
                || StrUtil.isBlank(version.getSchemaVersion())
                || !Objects.equals(version.getSchemaVersion(), bundle.schemaVersion())
                || StrUtil.isBlank(version.getCanonicalPolicyJson())
                || !Objects.equals(version.getCanonicalPolicyJson(), bundle.canonicalPolicyJson())) {
            throw exception(GXP_AUDIT_POLICY_ACTIVATION_INVALID, "activation-request-payload-conflict");
        }
    }

    private void insertPolicyVersion(Long tenantId, GxpAuditPolicyActivationCommand command,
                                     GxpAuditPolicyBundle bundle) {
        GxpAuditPolicyVersionDO existing = policyVersionMapper.selectOne(new LambdaQueryWrapper<GxpAuditPolicyVersionDO>()
                .eq(GxpAuditPolicyVersionDO::getTenantId, tenantId)
                .eq(GxpAuditPolicyVersionDO::getPolicyVersion, bundle.policyVersion())
                .last("FOR UPDATE"));
        if (existing != null) {
            if (!Objects.equals(existing.getPolicyHash(), bundle.policyHash())) {
                throw exception(GXP_AUDIT_POLICY_ACTIVATION_INVALID, "policy-version-hash-conflict");
            }
            return;
        }
        GxpAuditPolicyVersionDO version = new GxpAuditPolicyVersionDO();
        version.setTenantId(tenantId);
        version.setPolicyVersion(bundle.policyVersion());
        version.setPolicyHash(bundle.policyHash());
        version.setApprovedBy(command.actorId());
        version.setApprovedAt(LocalDateTime.now(ZoneOffset.UTC));
        version.setApprovalReference(command.approvalReference());
        version.setCoverageReportHash(command.coverageReportHash());
        version.setSchemaVersion(bundle.schemaVersion());
        version.setCanonicalPolicyJson(bundle.canonicalPolicyJson());
        version.setArtifactHash(bundle.artifactHash());
        if (policyVersionMapper.insert(version) != 1) {
            throw exception(GXP_AUDIT_POLICY_ACTIVATION_INVALID, "policy-version-insert");
        }
    }

    private List<GxpAuditPolicyOperationDO> parseOperations(Long tenantId, GxpAuditPolicyBundle bundle) {
        List<GxpAuditPolicyOperationDO> result = new ArrayList<>();
        JsonNode operations = bundle.policyNode().path("operations");
        if (!operations.isArray()) {
            throw exception(GXP_AUDIT_POLICY_ACTIVATION_INVALID, "policy-operations-invalid");
        }
        for (JsonNode operationNode : operations) {
            GxpAuditPolicyOperationDO operation = new GxpAuditPolicyOperationDO();
            operation.setTenantId(tenantId);
            operation.setPolicyVersion(bundle.policyVersion());
            operation.setOperationId(text(operationNode, "operationId"));
            operation.setSourceType(text(operationNode, "sourceType"));
            operation.setSourceLocator(text(operationNode, "sourceLocator"));
            operation.setDomain(text(operationNode, "domain"));
            operation.setSubjectType(text(operationNode, "subjectType"));
            operation.setActionType(text(operationNode, "actionType"));
            operation.setReasonPolicy(text(operationNode, "reasonPolicy"));
            operation.setSignaturePolicy(text(operationNode, "signaturePolicy"));
            operation.setStatePolicy(text(operationNode, "statePolicy"));
            operation.setRetentionClass(text(operationNode, "retentionClass"));
            operation.setTestIds(operationNode.path("testIds").toString());
            operation.setOwner(text(operationNode, "owner"));
            operation.setApplicability(text(operationNode, "applicability"));
            operation.setActive(true);
            result.add(operation);
        }
        if (result.isEmpty()) {
            throw exception(GXP_AUDIT_POLICY_ACTIVATION_INVALID, "policy-operations-empty");
        }
        return result;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual()) {
            throw exception(GXP_AUDIT_POLICY_ACTIVATION_INVALID, "policy-operation-field:" + field);
        }
        return value.textValue();
    }

    private void validate(GxpAuditPolicyActivationCommand command) {
        if (command == null || command.tenantId() == null || command.actorId() == null
                || StrUtil.isBlank(command.approvalReference()) || StrUtil.isBlank(command.requestId())
                || StrUtil.isBlank(command.coverageReportHash())) {
            throw exception(GXP_AUDIT_POLICY_ACTIVATION_INVALID, "command");
        }
    }
}
