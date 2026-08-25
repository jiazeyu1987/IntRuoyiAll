package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFlow6CompletionBackfillReceipt;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;

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
                || !Objects.equals(tenantId, receipt.getTenantId())
                || !isCompleteSuccessfulReceipt(receipt)) {
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
                .setSourceSnapshotHash(receipt.getSourceSnapshotHash()).setCompletionVersion(receipt.getCompletionVersion().longValue())
                .setStatus(receipt.getStatus()).setReceiptHash(receipt.getReceiptHash())
                .setIdempotencyKey(receipt.getRequestIdempotencyKey()).setHasActualLoss(receipt.getHasActualLoss())
                .setLossQuantity(receipt.getLossQuantity()).setLossRecordId(receipt.getLossRecordId())
                .setLossReportStatus(receipt.getLossReportStatus())
                .setLossDecision(Boolean.TRUE.equals(receipt.getHasActualLoss()) ? "ACTUAL_LOSS" : "NO_LOSS")
                .setProductionBackfillStatus(receipt.getStatus()).setInspectionBackfillStatus(receipt.getStatus())
                .setLossBackfillStatus(Boolean.TRUE.equals(receipt.getHasActualLoss()) ? receipt.getLossReportStatus() : "NO_LOSS");
        MesBatchExecutionProvisionCommand canonical = new MesBatchExecutionProvisionCommand()
                .setEntryType(request.getEntryType()).setEntryBusinessId(request.getEntryBusinessId())
                .setSourceCredentialType(ACTIVE_RECEIPT_TYPE).setSourceCredentialId(String.valueOf(receipt.getReceiptId()))
                .setSourceContextHash(receipt.getSourceSnapshotHash()).setTenantId(receipt.getTenantId())
                .setActiveOrderId(receipt.getActiveOrderId()).setWorkOrderId(receipt.getWorkOrderId())
                .setWorkOrderCode(request.getWorkOrderCode()).setBatchCode(receipt.getBatchCode())
                .setRouteId(receipt.getRouteId()).setRouteVersionId(receipt.getRouteVersionId())
                .setSourceSnapshotHash(receipt.getSourceSnapshotHash()).setIdempotencyKey(receipt.getRequestIdempotencyKey())
                .setCompletionVersion(receipt.getCompletionVersion().longValue())
                .setCompletionBackfillReceiptId(String.valueOf(receipt.getReceiptId()))
                .setCompletionBackfillReceiptHash(receipt.getReceiptHash())
                .setPickListBindingId(pickListBinding.getId())
                .setPickListId(pickListBinding.getPickListId())
                .setBindingVersion(Long.valueOf(pickListBinding.getBindingVersion()))
                .setCompletionBackfillReceipt(canonicalReceipt);
        return new MesBatchExecutionAuthoritativeContext().setProvisionCommand(canonical).setCompletionReceipt(receipt)
                .setPickListBinding(pickListBinding);
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
                .setSourceSnapshotHash(receipt.getSourceSnapshotHash()).setIdempotencyKey(receipt.getIdempotencyKey())
                .setExpectedSourceVersion(receipt.getSourceRelationVersion()).setPayloadHash(receipt.getPayloadHash())
                .setSourceEvidence(receipt.getSourceEvidence())
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
                    && "BACKFILL_SUCCEEDED".equals(receipt.getLossReportStatus());
        }
        return receipt.getLossQuantity().signum() == 0 && receipt.getLossRecordId() == null
                && Set.of("NO_LOSS", "NOT_REQUIRED").contains(receipt.getLossReportStatus())
                && !StrUtil.isBlank(receipt.getZeroLossConfirmationSnapshot());
    }
}
