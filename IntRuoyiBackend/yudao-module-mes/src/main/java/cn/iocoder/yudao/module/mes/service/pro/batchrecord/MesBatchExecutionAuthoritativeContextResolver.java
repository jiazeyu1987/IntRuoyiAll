package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFlow6CompletionBackfillReceipt;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import cn.hutool.crypto.digest.DigestUtil;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_ENTRY_CREDENTIAL_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_ENTRY_RECEIPT_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_ENTRY_SCENARIO_MISMATCH;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_ENTRY_SOURCE_RELATION_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_ENTRY_TENANT_REQUIRED;

/** Resolves receiptId through the owning process before Flow 6 validates or writes anything. */
@Service
public class MesBatchExecutionAuthoritativeContextResolver {

    private static final String ACTIVE_RECEIPT_TYPE = "CompletionBackfillReceipt";
    private static final String INDEPENDENT_RECEIPT_TYPE = "IndependentBatchPrerequisiteReceipt";
    private static final Set<String> ACTIVE_ENTRY_TYPES = Set.of(
            "ACTIVE_ORDER_COMPLETION", "ACTIVE_ORDER_SCHEDULED", "ACTIVE_ORDER_PQC", "MANUAL_CONTROLLED_RETRY");
    private static final Set<String> INDEPENDENT_ENTRY_TYPES = Set.of("MANUAL", "SCHEDULED", "PQC_INDEPENDENT");

    private final MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort completionReceiptPort;
    private final MesIndependentBatchPrerequisiteReceiptService independentReceiptService;
    private final MesProcessPoolActiveOrderPickListBindingMapper pickListBindingMapper;

    public MesBatchExecutionAuthoritativeContextResolver(
            MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort completionReceiptPort,
            MesIndependentBatchPrerequisiteReceiptService independentReceiptService,
            MesProcessPoolActiveOrderPickListBindingMapper pickListBindingMapper) {
        this.completionReceiptPort = completionReceiptPort;
        this.independentReceiptService = independentReceiptService;
        this.pickListBindingMapper = pickListBindingMapper;
    }

    public MesBatchExecutionAuthoritativeContext resolve(MesBatchExecutionProvisionCommand request, Long tenantId) {
        if (tenantId == null) {
            throw exception(PRO_EDHR_BATCH_ENTRY_TENANT_REQUIRED);
        }
        if (request == null || StrUtil.isBlank(request.getEntryType())) {
            throw exception(PRO_EDHR_BATCH_ENTRY_SCENARIO_MISMATCH);
        }
        if (request.getCompletionBackfillReceipt() != null || request.getIndependentReceipt() != null) {
            throw exception(PRO_EDHR_BATCH_ENTRY_RECEIPT_INVALID);
        }
        if (StrUtil.isBlank(request.getSourceCredentialId())) {
            throw exception(PRO_EDHR_BATCH_ENTRY_CREDENTIAL_REQUIRED);
        }
        if (ACTIVE_ENTRY_TYPES.contains(request.getEntryType())) {
            return resolveActive(request, tenantId);
        }
        if (INDEPENDENT_ENTRY_TYPES.contains(request.getEntryType())) {
            return resolveIndependent(request, tenantId);
        }
        throw exception(PRO_EDHR_BATCH_ENTRY_SCENARIO_MISMATCH);
    }

    private MesBatchExecutionAuthoritativeContext resolveActive(
            MesBatchExecutionProvisionCommand request, Long tenantId) {
        Long receiptId = parseReceiptId(request.getSourceCredentialId());
        MesFlow6CompletionBackfillReceipt receipt = completionReceiptPort.getByReceiptId(receiptId, tenantId);
        if (receipt == null || !Objects.equals(receiptId, receipt.getReceiptId())
                || !Objects.equals(tenantId, receipt.getTenantId()) || !isCompleteSuccessfulReceipt(receipt)) {
            throw exception(PRO_EDHR_BATCH_ENTRY_RECEIPT_INVALID);
        }
        requireMatch(request.getSourceCredentialType(), ACTIVE_RECEIPT_TYPE);
        requireMatch(request.getActiveOrderId(), receipt.getActiveOrderId());
        requireMatch(request.getWorkOrderId(), receipt.getWorkOrderId());
        requireMatch(request.getBatchCode(), receipt.getBatchCode());
        requireMatch(request.getRouteId(), receipt.getRouteId());
        requireMatch(request.getRouteVersionId(), receipt.getRouteVersionId());
        requireMatch(request.getSourceContextHash(), receipt.getSourceSnapshotHash());
        requireMatch(request.getSourceSnapshotHash(), receipt.getSourceSnapshotHash());
        requireMatch(request.getCompletionBackfillReceiptId(), String.valueOf(receipt.getReceiptId()));
        requireMatch(request.getCompletionBackfillReceiptHash(), receipt.getReceiptHash());
        if (request.getTenantId() != null && !Objects.equals(request.getTenantId(), tenantId)) {
            throw exception(PRO_EDHR_BATCH_ENTRY_RECEIPT_INVALID);
        }
        MesProcessPoolActiveOrderPickListBindingDO pickListBinding = resolvePickListBinding(receipt, tenantId);
        MesCompletionBackfillReceipt canonicalReceipt = new MesCompletionBackfillReceipt()
                .setReceiptId(String.valueOf(receipt.getReceiptId())).setTenantId(receipt.getTenantId())
                .setActiveOrderId(receipt.getActiveOrderId()).setWorkOrderId(receipt.getWorkOrderId())
                .setBatchCode(receipt.getBatchCode()).setRouteId(receipt.getRouteId())
                .setRouteVersionId(receipt.getRouteVersionId()).setSourceContextHash(receipt.getSourceSnapshotHash())
                .setSourceSnapshotHash(receipt.getSourceSnapshotHash())
                .setCompletionVersion(receipt.getCompletionVersion() == null ? null : receipt.getCompletionVersion().longValue())
                .setExpectedActiveOrderVersion(receipt.getExpectedActiveOrderVersion() == null ? null : receipt.getExpectedActiveOrderVersion().longValue())
                .setCompletionTransactionId(receipt.getRequestIdempotencyKey())
                .setCompletionEventId(receipt.getRequestIdempotencyKey())
                .setStatus(receipt.getStatus()).setReceiptHash(receipt.getReceiptHash())
                .setIdempotencyKey(receipt.getRequestIdempotencyKey())
                .setPayloadHash(receipt.getPayloadHash())
                .setAuditEventId("ACTIVE_ORDER_COMPLETION_RECEIPT:" + receipt.getReceiptId())
                .setReceiptVersion("1").setProductionProgress(100).setInspectionProgress(100)
                .setInspectionBackfillStatus(receipt.getProcessInspectionStatus())
                .setProductionBackfillStatus(receipt.getBatchRecordStatus())
                .setLossBackfillStatus(Boolean.TRUE.equals(receipt.getHasActualLoss())
                        ? receipt.getLossReportStatus() : "NO_LOSS")
                .setBatchRecordId(receipt.getBatchRecordId()).setProcessInspectionId(receipt.getProcessInspectionId())
                .setHasActualLoss(receipt.getHasActualLoss()).setLossQuantity(receipt.getLossQuantity())
                .setLossRecordId(receipt.getLossRecordId()).setLossReportStatus(receipt.getLossReportStatus())
                .setLossDecision(Boolean.TRUE.equals(receipt.getHasActualLoss()) ? "ACTUAL_LOSS" : "NO_LOSS")
                .setSourceVersion(String.valueOf(receipt.getCompletionVersion()))
                .setSourceBundleHash(traceSourceBundleHash(traceEvidence(receipt, pickListBinding)))
                .setPickListBindingId(pickListBinding.getId()).setPickListId(pickListBinding.getPickListId())
                .setBatchPickListRelationId(pickListBinding.getId())
                .setBindingVersion(pickListBinding.getBindingVersion() == null ? null
                        : pickListBinding.getBindingVersion().longValue())
                .setPickListHeaderSnapshotHash(pickListBinding.getSourceSnapshotHash())
                .setPickListLineSnapshotHash(pickListBinding.getSourceSnapshotHash())
                .setSourceEvidence(traceEvidence(receipt, pickListBinding));
        MesBatchExecutionProvisionCommand canonical = new MesBatchExecutionProvisionCommand()
                .setEntryType(request.getEntryType()).setEntryBusinessId(request.getEntryBusinessId())
                .setSourceCredentialType(ACTIVE_RECEIPT_TYPE).setSourceCredentialId(String.valueOf(receipt.getReceiptId()))
                .setSourceContextHash(receipt.getSourceSnapshotHash()).setTenantId(receipt.getTenantId())
                .setActiveOrderId(receipt.getActiveOrderId()).setWorkOrderId(receipt.getWorkOrderId())
                .setWorkOrderCode(request.getWorkOrderCode()).setBatchCode(receipt.getBatchCode())
                .setRouteId(receipt.getRouteId()).setRouteVersionId(receipt.getRouteVersionId())
                .setSourceSnapshotHash(receipt.getSourceSnapshotHash()).setIdempotencyKey(request.getIdempotencyKey())
                .setCompletionVersion(receipt.getCompletionVersion() == null ? null : receipt.getCompletionVersion().longValue())
                .setExpectedActiveOrderVersion(receipt.getExpectedActiveOrderVersion() == null ? null : receipt.getExpectedActiveOrderVersion().longValue())
                .setCompletionTransactionId(receipt.getRequestIdempotencyKey())
                .setCompletionBackfillReceiptId(String.valueOf(receipt.getReceiptId()))
                .setCompletionBackfillReceiptHash(receipt.getReceiptHash())
                .setSourceVersion(canonicalReceipt.getSourceVersion()).setSourceBundleHash(canonicalReceipt.getSourceBundleHash())
                .setPickListBindingId(pickListBinding.getId()).setPickListId(pickListBinding.getPickListId())
                .setBindingVersion(canonicalReceipt.getBindingVersion())
                .setBatchPickListRelationId(pickListBinding.getId())
                .setPickListHeaderSnapshotHash(canonicalReceipt.getPickListHeaderSnapshotHash())
                .setPickListLineSnapshotHash(canonicalReceipt.getPickListLineSnapshotHash())
                .setSourceEvidence(canonicalReceipt.getSourceEvidence())
                .setExpectedSourceVersion(canonicalReceipt.getSourceVersion())
                .setPayloadHash(canonicalReceipt.getPayloadHash())
                .setCompletionBackfillReceipt(canonicalReceipt);
        return new MesBatchExecutionAuthoritativeContext().setProvisionCommand(canonical)
                .setCompletionReceipt(receipt).setPickListBinding(pickListBinding);
    }

    private MesBatchExecutionAuthoritativeContext resolveIndependent(
            MesBatchExecutionProvisionCommand request, Long tenantId) {
        MesIndependentBatchPrerequisiteReceipt receipt = independentReceiptService.verify(
                new MesIndependentBatchPrerequisiteReceiptVerifyCommand()
                        .setReceiptId(request.getSourceCredentialId()).setEntryType(request.getEntryType())
                        .setSourceSnapshotHash(request.getSourceSnapshotHash()), tenantId);
        if (receipt == null || !Objects.equals(tenantId, receipt.getTenantId())
                || !Objects.equals(request.getSourceCredentialId(), receipt.getReceiptId())
                || !Objects.equals(request.getEntryType(), receipt.getEntryType())) {
            throw exception(PRO_EDHR_BATCH_ENTRY_RECEIPT_INVALID);
        }
        requireMatch(request.getWorkOrderId(), receipt.getWorkOrderId());
        requireMatch(request.getBatchCode(), receipt.getBatchCode());
        requireMatch(request.getRouteId(), receipt.getRouteId());
        requireMatch(request.getRouteVersionId(), receipt.getRouteVersionId());
        requireMatch(request.getSourceSnapshotHash(), receipt.getSourceSnapshotHash());
        requireMatch(request.getSourceContextHash(), receipt.getSourceContextHash());
        requireMatch(request.getSourceRelationId(), receipt.getSourceRelationId());
        requireMatch(request.getExpectedSourceVersion(), receipt.getSourceRelationVersion());
        requireMatch(request.getPayloadHash(), receipt.getPayloadHash());
        if (request.getTenantId() != null && !Objects.equals(request.getTenantId(), tenantId)) {
            throw exception(PRO_EDHR_BATCH_ENTRY_RECEIPT_INVALID);
        }
        MesBatchExecutionProvisionCommand canonical = new MesBatchExecutionProvisionCommand()
                .setEntryType(receipt.getEntryType()).setEntryBusinessId(request.getEntryBusinessId())
                .setSourceCredentialType(INDEPENDENT_RECEIPT_TYPE).setSourceCredentialId(receipt.getReceiptId())
                .setSourceRelationId(receipt.getSourceRelationId()).setSourceContextHash(receipt.getSourceContextHash())
                .setTenantId(receipt.getTenantId()).setWorkOrderId(receipt.getWorkOrderId())
                .setWorkOrderCode(receipt.getWorkOrderCode()).setBatchCode(receipt.getBatchCode())
                .setRouteId(receipt.getRouteId()).setRouteVersionId(receipt.getRouteVersionId())
                .setSourceSnapshotHash(receipt.getSourceSnapshotHash()).setIdempotencyKey(request.getIdempotencyKey())
                .setExpectedSourceVersion(receipt.getSourceRelationVersion()).setPayloadHash(receipt.getPayloadHash())
                .setSourceVersion(receipt.getSourceRelationVersion())
                .setSourceBundleHash(traceSourceBundleHash(normalizeIndependentEvidence(receipt.getSourceEvidence())))
                .setSourceEvidence(normalizeIndependentEvidence(receipt.getSourceEvidence()))
                .setIndependentReceipt(receipt);
        return new MesBatchExecutionAuthoritativeContext().setProvisionCommand(canonical)
                .setIndependentReceipt(receipt);
    }

    private Long parseReceiptId(String value) {
        try {
            return Long.valueOf(value);
        } catch (RuntimeException ex) {
            throw exception(PRO_EDHR_BATCH_ENTRY_RECEIPT_INVALID);
        }
    }

    private void requireMatch(Object requested, Object authoritative) {
        if (requested != null && !Objects.equals(requested, authoritative)) {
            throw exception(PRO_EDHR_BATCH_ENTRY_RECEIPT_INVALID);
        }
    }

    private MesProcessPoolActiveOrderPickListBindingDO resolvePickListBinding(
            MesFlow6CompletionBackfillReceipt receipt, Long tenantId) {
        if (receipt.getActiveOrderId() == null || pickListBindingMapper == null) {
            throw exception(PRO_EDHR_BATCH_ENTRY_SOURCE_RELATION_REQUIRED);
        }
        MesProcessPoolActiveOrderPickListBindingDO binding =
                pickListBindingMapper.selectByActiveOrderId(receipt.getActiveOrderId());
        if (binding == null || !Objects.equals(binding.getTenantId(), tenantId)
                || !Objects.equals(binding.getActiveOrderId(), receipt.getActiveOrderId())
                || !Objects.equals(binding.getWorkOrderId(), receipt.getWorkOrderId())
                || binding.getId() == null || binding.getPickListId() == null
                || binding.getBindingVersion() == null || binding.getBindingVersion() <= 0
                || StrUtil.isBlank(binding.getSourceSnapshotHash())
                || !"BOUND".equalsIgnoreCase(binding.getBindingStatus())) {
            throw exception(PRO_EDHR_BATCH_ENTRY_SOURCE_RELATION_REQUIRED);
        }
        return binding;
    }

    private boolean isCompleteSuccessfulReceipt(MesFlow6CompletionBackfillReceipt receipt) {
        if (!MesFlow6CompletionBackfillReceipt.STATUS_BACKFILL_SUCCEEDED.equals(receipt.getStatus())
                || receipt.getWorkOrderId() == null || StrUtil.isBlank(receipt.getBatchCode())
                || receipt.getRouteId() == null || receipt.getRouteVersionId() == null
                || receipt.getCompletionVersion() == null || receipt.getCompletionVersion() <= 0
                || StrUtil.isBlank(receipt.getSourceSnapshotHash()) || StrUtil.isBlank(receipt.getReceiptHash())
                || StrUtil.isBlank(receipt.getRequestIdempotencyKey()) || receipt.getHasActualLoss() == null
                || receipt.getLossQuantity() == null || StrUtil.isBlank(receipt.getLossReportStatus())) {
            return false;
        }
        if (Boolean.TRUE.equals(receipt.getHasActualLoss())) {
            return receipt.getLossQuantity().signum() > 0 && receipt.getLossRecordId() != null
                    && MesProcessPoolActiveOrderCompletionReceiptDO.LOSS_REPORT_STATUS_SUCCESS
                    .equals(receipt.getLossReportStatus());
        }
        return receipt.getLossQuantity().signum() == 0 && receipt.getLossRecordId() == null
                && MesProcessPoolActiveOrderCompletionReceiptDO.LOSS_REPORT_STATUS_NOT_REQUIRED
                .equals(receipt.getLossReportStatus()) && !StrUtil.isBlank(receipt.getZeroLossConfirmationSnapshot());
    }

    private List<MesBatchExecutionSourceEvidence> traceEvidence(
            MesFlow6CompletionBackfillReceipt receipt, MesProcessPoolActiveOrderPickListBindingDO binding) {
        List<MesBatchExecutionSourceEvidence> evidence = new ArrayList<>();
        evidence.add(simpleEvidence("ACTIVE_ORDER", receipt.getActiveOrderId(), receipt.getSourceSnapshotHash(), "BOUND"));
        evidence.add(simpleEvidence("WORK_ORDER", receipt.getWorkOrderId(), receipt.getSourceSnapshotHash(), "BOUND"));
        evidence.add(simpleEvidence("MATERIAL_ISSUE", binding.getPickListId(), receipt.getSourceSnapshotHash(), "BOUND"));
        evidence.add(simpleEvidence("MATERIAL_ISSUE_LINE", binding.getId(), receipt.getSourceSnapshotHash(), "BOUND"));
        for (String type : List.of("PRODUCTION_SUBMIT", "PRODUCTION_SIGNATURE", "PRODUCTION_LEADER_REVIEW")) {
            evidence.add(simpleEvidence(type, receipt.getBatchRecordId(), receipt.getReceiptHash(), "BOUND"));
        }
        for (String type : List.of("PQC_TASK", "PQC_SUBMISSION", "PQC_SIGNATURE",
                "PQC_LEADER_CONFIRMATION", "PQC_AGGREGATE_DETAIL")) {
            evidence.add(simpleEvidence(type, receipt.getProcessInspectionId(), receipt.getReceiptHash(), "BOUND"));
        }
        evidence.add(simpleEvidence("BATCH_RECORD_RECEIPT", receipt.getBatchRecordId(), receipt.getReceiptHash(), "BOUND"));
        evidence.add(simpleEvidence("PROCESS_INSPECTION_RECEIPT", receipt.getProcessInspectionId(), receipt.getReceiptHash(), "BOUND"));
        evidence.add(simpleEvidence("COMPLETION_BACKFILL_RECEIPT", receipt.getReceiptId(), receipt.getReceiptHash(), "BOUND"));
        if (Boolean.TRUE.equals(receipt.getHasActualLoss())) {
            evidence.add(simpleEvidence("LOSS_FACT", receipt.getLossRecordId(), receipt.getReceiptHash(), "HAS_LOSS"));
            evidence.add(simpleEvidence("LOSS_REPORT_RECEIPT", receipt.getLossRecordId(), receipt.getReceiptHash(), "BOUND"));
        } else {
            evidence.add(simpleEvidence("NO_LOSS_CONFIRMED", receipt.getReceiptId(), receipt.getReceiptHash(), "NO_LOSS"));
        }
        return evidence;
    }

    private MesBatchExecutionSourceEvidence simpleEvidence(String type, Long id, String witnessHash,
                                                            String relationStatus) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("sourceType", type);
        snapshot.put("sourceId", id);
        snapshot.put("witnessHash", witnessHash);
        String snapshotJson = JSON.toJSONString(snapshot);
        String calculatedHash = MesProEdhrBatchTraceSourceHash.calculate(type, snapshotJson);
        boolean materialSource = "MATERIAL_ISSUE".equals(type) || "MATERIAL_ISSUE_LINE".equals(type);
        boolean receiptSource = type.endsWith("RECEIPT") || "LOSS_REPORT_RECEIPT".equals(type);
        return new MesBatchExecutionSourceEvidence().setSourceType(type).setSourceId(String.valueOf(id))
                .setSourceVersion("1").setSourceSnapshotHash(materialSource
                        ? witnessHash : receiptSource ? witnessHash : calculatedHash)
                .setPayloadHash(witnessHash).setSignature(witnessHash)
                .setSourceObjectType(type).setSourceObjectId(String.valueOf(id)).setSnapshotJson(snapshotJson)
                .setSourceIdentityKey(type + ":" + type + ":" + id + ":::").setRelationStatus(relationStatus);
    }

    private String traceSourceBundleHash(List<MesBatchExecutionSourceEvidence> evidence) {
        MesProEdhrBatchTraceabilityValidator validator = new MesProEdhrBatchTraceabilityValidator();
        List<MesProEdhrBatchTraceSource> sources = evidence.stream().map(item ->
                new MesProEdhrBatchTraceSource().setLinkType(item.getSourceType())
                        .setSourceObjectType(item.getSourceObjectType())
                        .setSourceObjectId(parseEvidenceId(item.getSourceObjectId()))
                        .setSourceVersion(parseEvidenceVersion(item.getSourceVersion()))
                        .setSourceIdentityKey(item.getSourceIdentityKey())
                        .setSnapshotHash(item.getSourceSnapshotHash())
                        .setRelationStatus(item.getRelationStatus())
                        .setRelationReason(item.getRelationReason())).toList();
        return validator.calculateSourceBundleHash(sources);
    }

    private Long parseEvidenceId(String value) {
        try {
            return Long.valueOf(value);
        } catch (RuntimeException ex) {
            throw exception(PRO_EDHR_BATCH_ENTRY_SOURCE_RELATION_REQUIRED);
        }
    }

    private Integer parseEvidenceVersion(String value) {
        try {
            return Integer.valueOf(value);
        } catch (RuntimeException ex) {
            throw exception(PRO_EDHR_BATCH_ENTRY_SOURCE_RELATION_REQUIRED);
        }
    }

    private List<MesBatchExecutionSourceEvidence> normalizeIndependentEvidence(
            List<MesBatchExecutionSourceEvidence> sourceEvidence) {
        if (sourceEvidence == null || sourceEvidence.isEmpty()) {
            throw exception(PRO_EDHR_BATCH_ENTRY_SOURCE_RELATION_REQUIRED);
        }
        return sourceEvidence.stream().map(item -> {
            if (item == null || StrUtil.isBlank(item.getSourceType())
                    || StrUtil.isBlank(item.getSourceId()) || StrUtil.isBlank(item.getSourceSnapshotHash())
                    || StrUtil.isBlank(item.getSourceVersion()) || StrUtil.isBlank(item.getRelationStatus())
                    || StrUtil.isBlank(item.getSourceObjectType()) || StrUtil.isBlank(item.getSourceObjectId())) {
                throw exception(PRO_EDHR_BATCH_ENTRY_SOURCE_RELATION_REQUIRED);
            }
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("sourceType", item.getSourceType());
            snapshot.put("sourceId", item.getSourceId());
            snapshot.put("witnessHash", item.getSourceSnapshotHash());
            String snapshotJson = item.getSnapshotJson();
            if (StrUtil.isBlank(snapshotJson)) {
                throw exception(PRO_EDHR_BATCH_ENTRY_SOURCE_RELATION_REQUIRED);
            }
            String sourceObjectId = item.getSourceObjectId();
            String sourceObjectType = item.getSourceObjectType();
            parseEvidenceId(sourceObjectId);
            String normalizedSnapshotHash = MesProEdhrBatchTraceSourceHash.calculate(item.getSourceType(), snapshotJson);
            return new MesBatchExecutionSourceEvidence()
                    .setSourceType(item.getSourceType()).setSourceId(item.getSourceId())
                    .setSourceVersion(item.getSourceVersion())
                    .setSourceSnapshotHash(normalizedSnapshotHash)
                    .setPayloadHash(item.getPayloadHash()).setSignature(item.getSignature())
                    .setSourceObjectType(sourceObjectType)
                    .setSourceObjectId(sourceObjectId).setSnapshotJson(snapshotJson)
                    .setSourceIdentityKey(item.getSourceType() + ":" + sourceObjectType + ":"
                            + sourceObjectId + ":::")
                    .setRelationStatus(item.getRelationStatus())
                    .setRelationReason(item.getRelationReason());
        }).toList();
    }
}
