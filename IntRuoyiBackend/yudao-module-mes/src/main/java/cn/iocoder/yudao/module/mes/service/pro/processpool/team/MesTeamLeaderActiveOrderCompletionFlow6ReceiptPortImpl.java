package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper;
import org.springframework.stereotype.Service;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrSourceCreationVerificationService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrSourceCreationEvidence;

import cn.iocoder.yudao.module.bpm.approval.content.ApprovalContentJson;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchExecutionPickListSource;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_RECEIPT_NOT_FOUND;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_RECEIPT_TAMPERED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING;

@Service
public class MesTeamLeaderActiveOrderCompletionFlow6ReceiptPortImpl
        implements MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort {

    private final MesProcessPoolActiveOrderCompletionReceiptMapper receiptMapper;

    private final MesEdhrSourceCreationVerificationService creationVerifier;

    public MesTeamLeaderActiveOrderCompletionFlow6ReceiptPortImpl(
            MesProcessPoolActiveOrderCompletionReceiptMapper receiptMapper,
            MesEdhrSourceCreationVerificationService creationVerifier) {
        this.receiptMapper = receiptMapper;
        this.creationVerifier = creationVerifier;
    }

    @Override
    public MesFlow6CompletionBackfillReceipt getByReceiptId(Long receiptId, Long tenantId) {
        MesProcessPoolActiveOrderCompletionReceiptDO receipt = receiptId == null ? null
                : receiptMapper.selectByIdAndTenantId(receiptId, tenantId);
        return toReceipt(receipt, receiptId, tenantId);
    }

    @Override
    public MesFlow6CompletionBackfillReceipt getByActiveOrderId(Long activeOrderId, Long tenantId) {
        MesProcessPoolActiveOrderCompletionReceiptDO receipt = activeOrderId == null ? null
                : receiptMapper.selectByActiveOrderIdAndTenantIdForUpdate(activeOrderId, tenantId);
        return toReceipt(receipt, activeOrderId, tenantId);
    }

    private MesFlow6CompletionBackfillReceipt toReceipt(
            MesProcessPoolActiveOrderCompletionReceiptDO receipt, Long lookupValue, Long tenantId) {
        if (receipt == null) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_RECEIPT_NOT_FOUND, lookupValue);
        }
        if (receipt.getTenantId() == null || !receipt.getTenantId().equals(tenantId)) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_RECEIPT_NOT_FOUND, lookupValue);
        }
        if (receipt.getReceiptHash() == null || receipt.getReceiptHash().isBlank()
                || !receipt.getReceiptHash().equals(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt))) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_RECEIPT_TAMPERED, lookupValue);
        }
        if (!MesProcessPoolActiveOrderCompletionReceiptDO.STATUS_SUCCESS.equals(receipt.getCompletionStatus())
                || !MesProcessPoolActiveOrderCompletionReceiptDO.BACKFILL_STATUS_SUCCESS
                .equals(receipt.getBatchRecordStatus())
                || !MesProcessPoolActiveOrderCompletionReceiptDO.BACKFILL_STATUS_SUCCESS
                .equals(receipt.getProcessInspectionStatus())
                || receipt.getBatchCode() == null || receipt.getBatchCode().isBlank()
                || receipt.getRouteId() == null || receipt.getRouteVersionId() == null
                || receipt.getBatchRecordId() == null || receipt.getProcessInspectionId() == null
                || receipt.getRequestIdempotencyKey() == null || receipt.getRequestIdempotencyKey().isBlank()
                || !MesProcessPoolActiveOrderCompletionReceiptDO.RECEIPT_STATUS_BACKFILL_SUCCEEDED
                .equals(receipt.getReceiptStatus())
                || receipt.getCreateTime() == null
                || receipt.getSourceSnapshotHash() == null || receipt.getSourceSnapshotHash().isBlank()
                || receipt.getFormalSourceSnapshotJson() == null || receipt.getFormalSourceSnapshotJson().isBlank()
                || receipt.getSignatureSnapshotJson() == null || receipt.getSignatureSnapshotJson().isBlank()
                || receipt.getCompletedVersion() == null || !lossFactsAreValid(receipt)) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING,
                    receipt.getActiveOrderId(), "FLOW6_RECEIPT_NOT_READY");
        }
        MesEdhrSourceCreationEvidence creation = creationVerifier.requireCompletionCreation(tenantId, receipt.getId());
        String transactionId = creationVerifier.requireCompletionTransactionId(tenantId, receipt.getId());
        if (creation == null || creation.creationEventId() == null || (!cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditDevelopmentMode.disabled() && creation.creationEventId() <= 0)
                || transactionId == null || transactionId.isBlank()) {
            throw new IllegalStateException("Completion creation audit binding is missing");
        }
        if (!Objects.equals(creation.sourceRowId(), receipt.getId())
                || !Objects.equals(creation.sourceReceiptId(), receipt.getId().toString())
                || !Objects.equals(creation.sourceVersion(), receipt.getCompletedVersion().toString())
                || !Objects.equals(creation.sourceHash(), receipt.getReceiptHash())) {
            throw new IllegalStateException("Completion creation does not match the projected owner receipt");
        }
        List<MesBatchExecutionPickListSource> picks = frozenPickListSources(receipt);
        return new MesFlow6CompletionBackfillReceipt()
                .setReceiptId(receipt.getId())
                .setActiveOrderId(receipt.getActiveOrderId())
                .setWorkOrderId(receipt.getWorkOrderId())
                .setBatchCode(receipt.getBatchCode())
                .setRouteId(receipt.getRouteId())
                .setRouteVersionId(receipt.getRouteVersionId())
                .setTenantId(receipt.getTenantId())
                .setRequestIdempotencyKey(receipt.getRequestIdempotencyKey())
                .setExpectedActiveOrderVersion(receipt.getExpectedVersion() == null ? null
                        : receipt.getExpectedVersion().longValue())
                .setCompletionTransactionId(transactionId)
                .setCompletionEventId(String.valueOf(creation.creationEventId()))
                .setAuditEventId(String.valueOf(creation.creationEventId()))
                .setSourceVersion(creation.sourceVersion())
                .setReceiptVersion(creation.sourceVersion())
                .setPayloadHash(creation.sourceHash())
                .setCreatedAt(receipt.getCreateTime())
                .setSourceSnapshotHash(receipt.getSourceSnapshotHash())
                .setFormalSourceSnapshotJson(receipt.getFormalSourceSnapshotJson())
                .setSignatureSnapshotJson(receipt.getSignatureSnapshotJson())
                .setCompletionVersion(receipt.getCompletedVersion())
                .setStatus(MesFlow6CompletionBackfillReceipt.STATUS_BACKFILL_SUCCEEDED)
                .setBatchRecordStatus(receipt.getBatchRecordStatus())
                .setProcessInspectionStatus(receipt.getProcessInspectionStatus())
                .setBatchRecordId(receipt.getBatchRecordId())
                .setProcessInspectionId(receipt.getProcessInspectionId())
                .setHasActualLoss(receipt.getHasActualLoss())
                .setLossConditionFactsJson(receipt.getLossConditionFactsJson())
                .setLossQuantity(receipt.getLossQuantity())
                .setLossReportStatus(receipt.getLossReportStatus())
                .setLossRecordId(receipt.getLossRecordId())
                .setBatchRecordSourceIdsJson(receipt.getBatchRecordSourceIdsJson())
                .setProcessInspectionSourceIdsJson(receipt.getProcessInspectionSourceIdsJson())
                .setZeroLossConfirmationSnapshot(receipt.getZeroLossConfirmationSnapshot())
                .setReceiptHash(receipt.getReceiptHash())
                .setPickListSources(picks);
    }

    @Override
    public MesEdhrVerifiedCompletionReceiptFact requireVerifiedByReceiptId(Long receiptId, Long tenantId) {
        MesFlow6CompletionBackfillReceipt projection = getByReceiptId(receiptId, tenantId);
        MesEdhrSourceCreationEvidence creation = creationVerifier.requireCompletionCreation(tenantId, receiptId);
        if (!String.valueOf(creation.creationEventId()).equals(projection.getCompletionEventId())) {
            throw new IllegalStateException("Completion creation changed during projection");
        }
        return new MesEdhrVerifiedCompletionReceiptFact(projection, creation, projection.getCompletionTransactionId());
    }

    /** Historical bindings are covered by the original owner hash and verified main/all-part creation. */
    private static List<MesBatchExecutionPickListSource> frozenPickListSources(
            MesProcessPoolActiveOrderCompletionReceiptDO receipt) {
        JsonNode seed = ApprovalContentJson.parse(receipt.getFormalSourceSnapshotJson());
        JsonNode order = seed.get("activeOrderBinding"), workOrder = seed.get("workOrderBinding");
        requireFrozen(seed.isObject() && order != null && order.isObject()
                && workOrder != null && workOrder.isObject());
        requireFrozen(frozenId(order.get("id")) == receipt.getActiveOrderId()
                && frozenId(order.get("tenantId")) == receipt.getTenantId()
                && frozenId(order.get("workOrderId")) == receipt.getWorkOrderId()
                && frozenId(order.get("routeId")) == receipt.getRouteId()
                && frozenId(order.get("routeVersionId")) == receipt.getRouteVersionId()
                && frozenId(workOrder.get("id")) == receipt.getWorkOrderId()
                && receipt.getBatchCode().equals(frozenText(workOrder.get("batchCode"))));
        JsonNode bindings = seed.get("pickListBindings");
        requireFrozen(bindings != null && bindings.isArray() && !bindings.isEmpty());
        JsonNode sourceIds = ApprovalContentJson.parse(receipt.getBatchRecordSourceIdsJson());
        requireFrozen(sourceIds.isArray() && !sourceIds.isEmpty());
        var batchSourceIds = new HashSet<Long>();
        for (JsonNode id : sourceIds) requireFrozen(batchSourceIds.add(frozenId(id)));
        var bindingIds = new HashSet<Long>();
        var pickListIds = new HashSet<Long>();
        var result = new ArrayList<MesBatchExecutionPickListSource>();
        for (JsonNode binding : bindings) {
            requireFrozen(binding.isObject());
            long bindingId = frozenId(binding.get("id")), pickListId = frozenId(binding.get("pickListId"));
            long version = frozenId(binding.get("bindingVersion"));
            String hash = frozenText(binding.get("sourceSnapshotHash"));
            requireFrozen(frozenId(binding.get("tenantId")) == receipt.getTenantId()
                    && frozenId(binding.get("activeOrderId")) == receipt.getActiveOrderId()
                    && frozenId(binding.get("workOrderId")) == receipt.getWorkOrderId()
                    && "BOUND".equals(frozenText(binding.get("bindingStatus")))
                    && version <= Integer.MAX_VALUE && hash.matches("[0-9a-f]{64}")
                    && bindingIds.add(bindingId) && pickListIds.add(pickListId) && batchSourceIds.contains(bindingId));
            result.add(new MesBatchExecutionPickListSource().setPickListBindingId(bindingId)
                    .setPickListId(pickListId).setBindingVersion(version).setSourceSnapshotHash(hash));
        }
        result.sort(Comparator.comparing(MesBatchExecutionPickListSource::getPickListBindingId));
        return List.copyOf(result);
    }

    private static long frozenId(JsonNode value) {
        requireFrozen(value != null);
        long id;
        if (value.isIntegralNumber() && value.canConvertToLong()) id = value.longValue();
        else if (value.isTextual() && value.textValue().matches("[1-9][0-9]*")) {
            try { id = Long.parseLong(value.textValue()); }
            catch (NumberFormatException e) { throw new IllegalStateException("COMPLETION_FROZEN_PICKLIST_SOURCE_INVALID", e); }
        } else throw new IllegalStateException("COMPLETION_FROZEN_PICKLIST_SOURCE_INVALID");
        requireFrozen(id > 0);
        return id;
    }

    private static String frozenText(JsonNode value) {
        requireFrozen(value != null && value.isTextual() && !value.textValue().isBlank());
        return value.textValue();
    }

    private static void requireFrozen(boolean condition) {
        if (!condition) throw new IllegalStateException("COMPLETION_FROZEN_PICKLIST_SOURCE_INVALID");
    }

    private static boolean lossFactsAreValid(MesProcessPoolActiveOrderCompletionReceiptDO receipt) {
        if (receipt.getHasActualLoss() == null || receipt.getLossQuantity() == null
                || receipt.getLossQuantity().signum() < 0 || receipt.getLossConditionFactsJson() == null
                || receipt.getLossConditionFactsJson().isBlank()) {
            return false;
        }
        if (Boolean.TRUE.equals(receipt.getHasActualLoss())) {
            return MesProcessPoolActiveOrderCompletionReceiptDO.LOSS_REPORT_STATUS_SUCCESS
                    .equals(receipt.getLossReportStatus())
                    && receipt.getLossQuantity().compareTo(BigDecimal.ZERO) > 0
                    && receipt.getLossRecordId() != null;
        }
        return MesProcessPoolActiveOrderCompletionReceiptDO.LOSS_REPORT_STATUS_NOT_REQUIRED
                .equals(receipt.getLossReportStatus())
                && receipt.getLossQuantity().compareTo(BigDecimal.ZERO) == 0
                && receipt.getLossRecordId() == null
                && receipt.getZeroLossConfirmationSnapshot() != null
                && !receipt.getZeroLossConfirmationSnapshot().isBlank();
    }
}
