package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingDO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFlow6CompletionBackfillReceipt;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_ENTRY_BACKFILL_NOT_SUCCEEDED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_ENTRY_BUSINESS_ID_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_ENTRY_CREDENTIAL_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_ENTRY_IDEMPOTENCY_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_ENTRY_RECEIPT_EXPIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_ENTRY_RECEIPT_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_ENTRY_RECEIPT_REVOKED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_ENTRY_SCENARIO_MISMATCH;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_ENTRY_SOURCE_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_ENTRY_SOURCE_RELATION_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_ENTRY_TENANT_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_ENTRY_TYPE_REQUIRED;

/** Validates Flow 9 entry contracts before Flow 6 owns batch state. */
@Service
public class MesBatchExecutionEntryContractService {

    private static final String ACTIVE_RECEIPT_TYPE = "CompletionBackfillReceipt";
    private static final String INDEPENDENT_RECEIPT_TYPE = "IndependentBatchPrerequisiteReceipt";
    private static final Set<String> ACTIVE_ENTRY_TYPES = Set.of(
            "ACTIVE_ORDER_COMPLETION", "ACTIVE_ORDER_SCHEDULED", "ACTIVE_ORDER_PQC", "MANUAL_CONTROLLED_RETRY");
    private static final Set<String> INDEPENDENT_ENTRY_TYPES = Set.of("MANUAL", "SCHEDULED", "PQC_INDEPENDENT");

    /** Validates only a context resolved from the owning receipt service. */
    public MesBatchExecutionProvisionCommand validate(MesBatchExecutionAuthoritativeContext context) {
        if (context == null || context.getProvisionCommand() == null) {
            throw exception(PRO_EDHR_BATCH_ENTRY_RECEIPT_INVALID);
        }
        MesBatchExecutionProvisionCommand command = context.getProvisionCommand();
        Long securityTenantId = TenantContextHolder.getTenantId();
        if (securityTenantId == null || command.getTenantId() == null
                || !Objects.equals(securityTenantId, command.getTenantId())
                || blank(command.getEntryType()) || blank(command.getEntryBusinessId())
                || blank(command.getSourceCredentialId()) || blank(command.getIdempotencyKey())
                || blank(command.getSourceContextHash()) || blank(command.getSourceSnapshotHash())) {
            throw exception(PRO_EDHR_BATCH_ENTRY_RECEIPT_INVALID);
        }
        if (ACTIVE_ENTRY_TYPES.contains(command.getEntryType())) {
            validateActiveContext(context, securityTenantId);
            return command;
        }
        if (INDEPENDENT_ENTRY_TYPES.contains(command.getEntryType())) {
            validateIndependentContext(context, securityTenantId);
            return command;
        }
        throw exception(PRO_EDHR_BATCH_ENTRY_SCENARIO_MISMATCH);
    }

    /**
     * Legacy unit-contract validator retained for isolated contract tests. Production entry
     * points must call the authoritative-context overload above before invoking this logic.
     */
    @Deprecated
    public MesBatchExecutionProvisionCommand validate(MesBatchExecutionProvisionCommand command) {
        if (command == null || blank(command.getEntryType())) {
            throw exception(PRO_EDHR_BATCH_ENTRY_TYPE_REQUIRED);
        }
        Long securityTenantId = TenantContextHolder.getTenantId();
        if (securityTenantId == null) {
            throw exception(PRO_EDHR_BATCH_ENTRY_TENANT_REQUIRED);
        }
        if (blank(command.getEntryBusinessId())) {
            throw exception(PRO_EDHR_BATCH_ENTRY_BUSINESS_ID_REQUIRED);
        }
        if (blank(command.getSourceContextHash())) {
            throw exception(PRO_EDHR_BATCH_ENTRY_SOURCE_CONTEXT_REQUIRED);
        }
        if (blank(command.getSourceCredentialId())) {
            throw exception(PRO_EDHR_BATCH_ENTRY_CREDENTIAL_REQUIRED);
        }
        if (blank(command.getIdempotencyKey())) {
            throw exception(PRO_EDHR_BATCH_ENTRY_IDEMPOTENCY_REQUIRED);
        }
        if (!ACTIVE_ENTRY_TYPES.contains(command.getEntryType())
                && !INDEPENDENT_ENTRY_TYPES.contains(command.getEntryType())) {
            throw exception(PRO_EDHR_BATCH_ENTRY_SCENARIO_MISMATCH);
        }
        if (ACTIVE_ENTRY_TYPES.contains(command.getEntryType())) {
            validateActive(command, securityTenantId);
        } else {
            validateIndependent(command, securityTenantId);
        }
        return command;
    }

    private void validateActiveContext(MesBatchExecutionAuthoritativeContext context, Long tenantId) {
        MesBatchExecutionProvisionCommand command = context.getProvisionCommand();
        MesFlow6CompletionBackfillReceipt receipt = context.getCompletionReceipt();
        List<MesProcessPoolActiveOrderPickListBindingDO> bindings = context.getPickListBindings();
        if (receipt == null || !Objects.equals(receipt.getTenantId(), tenantId)
                || !Objects.equals(receipt.getReceiptId(), parseLong(command.getSourceCredentialId()))
                || !MesFlow6CompletionBackfillReceipt.STATUS_BACKFILL_SUCCEEDED.equals(receipt.getStatus())
                || receipt.getWorkOrderId() == null || receipt.getRouteId() == null
                || receipt.getRouteVersionId() == null || blank(receipt.getBatchCode())
                || blank(receipt.getSourceSnapshotHash()) || blank(receipt.getReceiptHash())
                || receipt.getBatchRecordId() == null || receipt.getProcessInspectionId() == null
                || receipt.getHasActualLoss() == null || receipt.getLossQuantity() == null) {
            throw exception(PRO_EDHR_BATCH_ENTRY_RECEIPT_INVALID);
        }
        validatePickListSources(command, receipt, bindings, tenantId);
        if (Boolean.TRUE.equals(receipt.getHasActualLoss())
                && (receipt.getLossRecordId() == null || receipt.getLossQuantity().signum() <= 0
                || blank(receipt.getLossReportStatus()))) {
            throw exception(PRO_EDHR_BATCH_ENTRY_RECEIPT_INVALID);
        }
        if (Boolean.FALSE.equals(receipt.getHasActualLoss())
                && (receipt.getLossRecordId() != null || receipt.getLossQuantity().signum() != 0
                || blank(receipt.getZeroLossConfirmationSnapshot()))) {
            throw exception(PRO_EDHR_BATCH_ENTRY_RECEIPT_INVALID);
        }
    }

    private void validatePickListSources(MesBatchExecutionProvisionCommand command,
                                         MesFlow6CompletionBackfillReceipt receipt,
                                         List<MesProcessPoolActiveOrderPickListBindingDO> bindings,
                                         Long tenantId) {
        if (bindings == null || bindings.isEmpty()
                || command.getPickListSources() == null || command.getPickListSources().isEmpty()
                || command.getPickListSources().size() != bindings.size()) {
            throw exception(PRO_EDHR_BATCH_ENTRY_RECEIPT_INVALID);
        }
        for (MesProcessPoolActiveOrderPickListBindingDO binding : bindings) {
            if (binding == null || binding.getId() == null || binding.getPickListId() == null
                    || binding.getBindingVersion() == null || binding.getBindingVersion() <= 0
                    || blank(binding.getSourceSnapshotHash())
                    || !Objects.equals(binding.getTenantId(), tenantId)
                    || !Objects.equals(binding.getActiveOrderId(), receipt.getActiveOrderId())
                    || !Objects.equals(binding.getWorkOrderId(), receipt.getWorkOrderId())
                    || !"BOUND".equalsIgnoreCase(binding.getBindingStatus())
                    || command.getPickListSources().stream().noneMatch(source ->
                    Objects.equals(source.getPickListBindingId(), binding.getId())
                            && Objects.equals(source.getPickListId(), binding.getPickListId())
                            && Objects.equals(source.getBindingVersion(), Long.valueOf(binding.getBindingVersion()))
                            && Objects.equals(source.getSourceSnapshotHash(), binding.getSourceSnapshotHash()))) {
                throw exception(PRO_EDHR_BATCH_ENTRY_RECEIPT_INVALID);
            }
        }
    }

    private void validateIndependentContext(MesBatchExecutionAuthoritativeContext context, Long tenantId) {
        MesBatchExecutionProvisionCommand command = context.getProvisionCommand();
        MesIndependentBatchPrerequisiteReceipt receipt = context.getIndependentReceipt();
        if (receipt == null || !Objects.equals(receipt.getTenantId(), tenantId)
                || !Objects.equals(receipt.getReceiptId(), command.getSourceCredentialId())
                || !Objects.equals(receipt.getEntryType(), command.getEntryType())
                || !Objects.equals(receipt.getWorkOrderId(), command.getWorkOrderId())
                || !Objects.equals(receipt.getRouteId(), command.getRouteId())
                || !Objects.equals(receipt.getRouteVersionId(), command.getRouteVersionId())
                || !Objects.equals(receipt.getBatchCode(), command.getBatchCode())
                || !Objects.equals(receipt.getSourceSnapshotHash(), command.getSourceSnapshotHash())
                || !Objects.equals(receipt.getSourceContextHash(), command.getSourceContextHash())
                || !Objects.equals(receipt.getPayloadHash(), command.getPayloadHash())
                || !("ISSUED".equals(receipt.getStatus()) || "VALID".equals(receipt.getStatus()))
                || receipt.getRevokedAt() != null || receipt.getIssuedAt() == null
                || receipt.getExpiresAt() == null || !receipt.getExpiresAt().isAfter(LocalDateTime.now())
                || blank(receipt.getReceiptHash()) || blank(receipt.getSignature())) {
            throw exception(PRO_EDHR_BATCH_ENTRY_RECEIPT_INVALID);
        }
    }

    private Long parseLong(String value) {
        try {
            return Long.valueOf(value);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private void validateActive(MesBatchExecutionProvisionCommand command, Long securityTenantId) {
        if (!ACTIVE_RECEIPT_TYPE.equals(command.getSourceCredentialType())
                || command.getCompletionBackfillReceipt() == null
                || command.getIndependentReceipt() != null) {
            throw exception(PRO_EDHR_BATCH_ENTRY_SCENARIO_MISMATCH);
        }
        MesCompletionBackfillReceipt receipt = command.getCompletionBackfillReceipt();
        if (!"BACKFILL_SUCCEEDED".equals(receipt.getStatus())) {
            throw exception(PRO_EDHR_BATCH_ENTRY_BACKFILL_NOT_SUCCEEDED);
        }
        if (blank(receipt.getReceiptId()) || !Objects.equals(receipt.getReceiptId(), command.getSourceCredentialId())
                || command.getActiveOrderId() == null || command.getWorkOrderId() == null
                || !samePickListSources(command.getPickListSources(), receipt.getPickListSources())
                || !sameLegacyPickListFields(command, receipt)
                || command.getRouteVersionId() == null || receipt.getActiveOrderId() == null
                || command.getBatchCode() == null || command.getRouteId() == null
                || receipt.getTenantId() == null
                || !Objects.equals(securityTenantId, receipt.getTenantId())
                || (command.getTenantId() != null && !Objects.equals(command.getTenantId(), securityTenantId))
                || receipt.getWorkOrderId() == null || receipt.getBatchCode() == null
                || receipt.getRouteId() == null
                || (command.getTenantId() != null && !Objects.equals(command.getTenantId(), receipt.getTenantId()))
                || !command.getActiveOrderId().equals(receipt.getActiveOrderId())
                || !command.getWorkOrderId().equals(receipt.getWorkOrderId())
                || blank(command.getWorkOrderCode()) || blank(receipt.getWorkOrderCode())
                || !command.getWorkOrderCode().equals(receipt.getWorkOrderCode())
                || !command.getBatchCode().equals(receipt.getBatchCode())
                || !command.getRouteId().equals(receipt.getRouteId())
                || !Objects.equals(command.getRouteVersionId(), receipt.getRouteVersionId())
                || blank(receipt.getSourceContextHash())
                || !command.getSourceContextHash().equals(receipt.getSourceContextHash())
                || blank(command.getSourceSnapshotHash())
                || !command.getSourceSnapshotHash().equals(receipt.getSourceSnapshotHash())
                || blank(command.getExpectedSourceVersion())
                || !Objects.equals(command.getExpectedSourceVersion(), receipt.getSourceVersion())
                || blank(command.getSourceVersion())
                || !Objects.equals(command.getSourceVersion(), receipt.getSourceVersion())
                || blank(command.getCompletionTransactionId())
                || !Objects.equals(command.getCompletionTransactionId(), receipt.getCompletionTransactionId())
                || command.getExpectedActiveOrderVersion() == null
                || !Objects.equals(command.getExpectedActiveOrderVersion(), receipt.getExpectedActiveOrderVersion())
                || blank(receipt.getSourceBundleHash())
                || blank(command.getSourceBundleHash())
                || !Objects.equals(command.getSourceBundleHash(), receipt.getSourceBundleHash())
                || blank(receipt.getReceiptHash())
                || blank(command.getCompletionBackfillReceiptId())
                || !Objects.equals(command.getCompletionBackfillReceiptId(), receipt.getReceiptId())
                || blank(command.getCompletionBackfillReceiptHash())
                || !Objects.equals(command.getCompletionBackfillReceiptHash(), receipt.getReceiptHash())
                || !hasActiveEvidenceTypes(command.getSourceEvidence())
                || receipt.getSourceEvidence() == null
                || !sameEvidence(command.getSourceEvidence(), receipt.getSourceEvidence())
                || !Integer.valueOf(100).equals(receipt.getProductionProgress())
                || !Integer.valueOf(100).equals(receipt.getInspectionProgress())
                || !"BACKFILL_SUCCEEDED".equals(receipt.getProductionBackfillStatus())
                || !"BACKFILL_SUCCEEDED".equals(receipt.getInspectionBackfillStatus())
                || !("BACKFILL_SUCCEEDED".equals(receipt.getLossBackfillStatus())
                || "NO_LOSS".equals(receipt.getLossBackfillStatus()))
                || receipt.getCompletionVersion() == null
                || command.getCompletionVersion() == null
                || !Objects.equals(command.getCompletionVersion(), receipt.getCompletionVersion())
                || blank(receipt.getCompletionEventId()) || blank(receipt.getReceiptVersion())
                || receipt.getBatchRecordId() == null || receipt.getProcessInspectionId() == null
                || receipt.getHasActualLoss() == null || blank(receipt.getLossDecision())
                || blank(receipt.getPayloadHash()) || blank(receipt.getAuditEventId())
                || blank(receipt.getIdempotencyKey())
                || blank(command.getPayloadHash())
                || !Objects.equals(command.getPayloadHash(), receipt.getPayloadHash())) {
            throw exception(PRO_EDHR_BATCH_ENTRY_RECEIPT_INVALID);
        }
        if (Boolean.TRUE.equals(receipt.getHasActualLoss())
                && (!"BACKFILL_SUCCEEDED".equals(receipt.getLossBackfillStatus())
                || receipt.getLossRecordId() == null || blank(receipt.getLossReportStatus())
                || receipt.getLossQuantity() == null)) {
            throw exception(PRO_EDHR_BATCH_ENTRY_RECEIPT_INVALID);
        }
        if (Boolean.FALSE.equals(receipt.getHasActualLoss())
                && !Set.of("NO_LOSS").contains(receipt.getLossDecision())) {
            throw exception(PRO_EDHR_BATCH_ENTRY_RECEIPT_INVALID);
        }
    }

    private void validateIndependent(MesBatchExecutionProvisionCommand command, Long securityTenantId) {
        if (!INDEPENDENT_RECEIPT_TYPE.equals(command.getSourceCredentialType())
                || command.getCompletionBackfillReceipt() != null) {
            throw exception(PRO_EDHR_BATCH_ENTRY_SCENARIO_MISMATCH);
        }
        MesIndependentBatchPrerequisiteReceipt receipt = command.getIndependentReceipt();
        if (receipt == null) {
            throw exception(PRO_EDHR_BATCH_ENTRY_CREDENTIAL_REQUIRED);
        }
        if (command.getActiveOrderId() != null) {
            throw exception(PRO_EDHR_BATCH_ENTRY_SCENARIO_MISMATCH);
        }
        if (command.getWorkOrderId() == null || blank(command.getBatchCode())
                || command.getRouteId() == null || command.getRouteVersionId() == null) {
            throw exception(PRO_EDHR_BATCH_ENTRY_RECEIPT_INVALID);
        }
        if (blank(command.getSourceRelationId()) || !Objects.equals(command.getSourceRelationId(), receipt.getSourceRelationId())
                || blank(receipt.getSourceRelationId()) || blank(receipt.getSourceRelationVersion())
                || blank(receipt.getSourceRelationSnapshotHash()) || blank(receipt.getSourceObjectType())
                || blank(receipt.getSourceObjectId()) || blank(receipt.getMaterialSourceType())
                || blank(receipt.getMaterialSourceId()) || blank(receipt.getSourceSnapshotHash())) {
            throw exception(PRO_EDHR_BATCH_ENTRY_SOURCE_RELATION_REQUIRED);
        }
        if (blank(receipt.getReceiptId()) || !Objects.equals(receipt.getReceiptId(), command.getSourceCredentialId())
                || !command.getEntryType().equals(receipt.getEntryType())
                || blank(receipt.getSourceContextHash())
                || !command.getSourceContextHash().equals(receipt.getSourceContextHash())
                || blank(command.getSourceSnapshotHash())
                || !command.getSourceSnapshotHash().equals(receipt.getSourceSnapshotHash())
                || blank(command.getExpectedSourceVersion())
                || !Objects.equals(command.getExpectedSourceVersion(), receipt.getSourceRelationVersion())
                || receipt.getTenantId() == null || receipt.getWorkOrderId() == null
                || !Objects.equals(securityTenantId, receipt.getTenantId())
                || (command.getTenantId() != null && !Objects.equals(command.getTenantId(), securityTenantId))
                || blank(command.getWorkOrderCode()) || blank(receipt.getWorkOrderCode())
                || !Objects.equals(command.getWorkOrderCode(), receipt.getWorkOrderCode())
                || receipt.getRouteId() == null
                || blank(receipt.getRouteVersion()) || blank(receipt.getBatchCode())
                || blank(receipt.getStatus()) || !Set.of("ISSUED", "VALID").contains(receipt.getStatus())
                || blank(receipt.getBusinessReason()) || blank(receipt.getIssuerSystem())
                || receipt.getIssuerUserId() == null || blank(receipt.getIssuerUserRole())
                || receipt.getIssuedAt() == null || receipt.getExpiresAt() == null
                || receipt.getCredentialVersion() == null || receipt.getCredentialVersion() <= 0
                || blank(receipt.getPayloadHash()) || blank(receipt.getReceiptHash())
                || blank(receipt.getSignature())
                || blank(receipt.getAuditEventId()) || blank(receipt.getIdempotencyKey())
                || receipt.getSourceEvidence() == null || receipt.getSourceEvidence().isEmpty()
                || !validEvidence(receipt.getSourceEvidence())
                || blank(command.getPayloadHash())
                || !Objects.equals(command.getPayloadHash(), receipt.getPayloadHash())) {
            throw exception(PRO_EDHR_BATCH_ENTRY_RECEIPT_INVALID);
        }
        if (command.getWorkOrderId() != null && !Objects.equals(command.getWorkOrderId(), receipt.getWorkOrderId())) {
            throw exception(PRO_EDHR_BATCH_ENTRY_RECEIPT_INVALID);
        }
        if ((command.getTenantId() != null && !Objects.equals(command.getTenantId(), receipt.getTenantId()))
                || !Objects.equals(command.getRouteId(), receipt.getRouteId())
                || !Objects.equals(command.getRouteVersionId(), receipt.getRouteVersionId())) {
            throw exception(PRO_EDHR_BATCH_ENTRY_RECEIPT_INVALID);
        }
        if (command.getBatchCode() != null && !Objects.equals(command.getBatchCode(), receipt.getBatchCode())) {
            throw exception(PRO_EDHR_BATCH_ENTRY_RECEIPT_INVALID);
        }
        if (command.getRouteId() != null && !Objects.equals(command.getRouteId(), receipt.getRouteId())) {
            throw exception(PRO_EDHR_BATCH_ENTRY_RECEIPT_INVALID);
        }
        LocalDateTime now = LocalDateTime.now();
        if (receipt.getRevokedAt() != null) {
            throw exception(PRO_EDHR_BATCH_ENTRY_RECEIPT_REVOKED);
        }
        if (receipt.getIssuedAt().isAfter(now)) {
            throw exception(PRO_EDHR_BATCH_ENTRY_RECEIPT_INVALID);
        }
        if (!receipt.getExpiresAt().isAfter(now)) {
            throw exception(PRO_EDHR_BATCH_ENTRY_RECEIPT_EXPIRED);
        }
    }

    private boolean blank(String value) {
        return StrUtil.isBlank(value);
    }

    private boolean samePickListSources(List<MesBatchExecutionPickListSource> commandSources,
                                        List<MesBatchExecutionPickListSource> receiptSources) {
        if (commandSources == null || receiptSources == null || commandSources.isEmpty()
                || commandSources.size() != receiptSources.size()) {
            return false;
        }
        for (int i = 0; i < commandSources.size(); i++) {
            MesBatchExecutionPickListSource commandSource = commandSources.get(i);
            MesBatchExecutionPickListSource receiptSource = receiptSources.get(i);
            if (commandSource == null || receiptSource == null
                    || commandSource.getPickListBindingId() == null
                    || commandSource.getPickListId() == null
                    || commandSource.getBindingVersion() == null || commandSource.getBindingVersion() <= 0
                    || blank(commandSource.getSourceSnapshotHash())
                    || !Objects.equals(commandSource.getPickListBindingId(), receiptSource.getPickListBindingId())
                    || !Objects.equals(commandSource.getPickListId(), receiptSource.getPickListId())
                    || !Objects.equals(commandSource.getBindingVersion(), receiptSource.getBindingVersion())
                    || !Objects.equals(commandSource.getSourceSnapshotHash(), receiptSource.getSourceSnapshotHash())) {
                return false;
            }
        }
        return true;
    }

    private boolean sameLegacyPickListFields(MesBatchExecutionProvisionCommand command,
                                             MesCompletionBackfillReceipt receipt) {
        List<MesBatchExecutionPickListSource> sources = command.getPickListSources();
        if (sources == null || sources.size() != 1) {
            return command.getPickListBindingId() == null
                    && command.getPickListId() == null
                    && command.getBindingVersion() == null
                    && command.getPickListHeaderSnapshotHash() == null
                    && command.getPickListLineSnapshotHash() == null;
        }
        MesBatchExecutionPickListSource source = sources.get(0);
        return Objects.equals(command.getPickListBindingId(), source.getPickListBindingId())
                && Objects.equals(receipt.getPickListBindingId(), source.getPickListBindingId())
                && Objects.equals(command.getPickListId(), source.getPickListId())
                && Objects.equals(receipt.getPickListId(), source.getPickListId())
                && Objects.equals(command.getBindingVersion(), source.getBindingVersion())
                && Objects.equals(receipt.getBindingVersion(), source.getBindingVersion())
                && Objects.equals(command.getPickListHeaderSnapshotHash(), receipt.getPickListHeaderSnapshotHash())
                && Objects.equals(command.getPickListLineSnapshotHash(), receipt.getPickListLineSnapshotHash());
    }

    private boolean sameEvidence(java.util.List<MesBatchExecutionSourceEvidence> commandEvidence,
                                 java.util.List<MesBatchExecutionSourceEvidence> receiptEvidence) {
        if (commandEvidence.size() != receiptEvidence.size()) {
            return false;
        }
        for (int i = 0; i < commandEvidence.size(); i++) {
            MesBatchExecutionSourceEvidence commandItem = commandEvidence.get(i);
            MesBatchExecutionSourceEvidence receiptItem = receiptEvidence.get(i);
            if (commandItem == null || receiptItem == null
                    || blank(commandItem.getSourceType()) || blank(commandItem.getSourceId())
                    || blank(commandItem.getSourceVersion()) || blank(commandItem.getSourceSnapshotHash())
                    || blank(commandItem.getPayloadHash()) || blank(commandItem.getSignature())
                    || !Objects.equals(commandItem.getSourceType(), receiptItem.getSourceType())
                    || !Objects.equals(commandItem.getSourceId(), receiptItem.getSourceId())
                    || !Objects.equals(commandItem.getSourceVersion(), receiptItem.getSourceVersion())
                    || !Objects.equals(commandItem.getSourceSnapshotHash(), receiptItem.getSourceSnapshotHash())
                    || !Objects.equals(commandItem.getPayloadHash(), receiptItem.getPayloadHash())
                    || !Objects.equals(commandItem.getSignature(), receiptItem.getSignature())) {
                return false;
            }
        }
        return true;
    }

    private boolean validEvidence(java.util.List<MesBatchExecutionSourceEvidence> evidence) {
        for (MesBatchExecutionSourceEvidence item : evidence) {
            if (item == null || blank(item.getSourceType()) || blank(item.getSourceId())
                    || blank(item.getSourceVersion()) || blank(item.getSourceSnapshotHash())
                    || blank(item.getPayloadHash()) || blank(item.getSignature())) {
                return false;
            }
        }
        return true;
    }

    private boolean hasActiveEvidenceTypes(java.util.List<MesBatchExecutionSourceEvidence> evidence) {
        if (evidence == null || evidence.size() < 3 || !validEvidence(evidence)) {
            return false;
        }
        Set<String> types = evidence.stream().map(MesBatchExecutionSourceEvidence::getSourceType)
                .collect(java.util.stream.Collectors.toSet());
        return types.containsAll(Set.of("PRODUCTION", "PQC", "LOSS"));
    }
}
