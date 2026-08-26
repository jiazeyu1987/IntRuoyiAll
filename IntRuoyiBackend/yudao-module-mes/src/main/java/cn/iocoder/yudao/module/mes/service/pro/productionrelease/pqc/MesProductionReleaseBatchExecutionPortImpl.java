package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlocker;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerException;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerType;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowFailureRespVO;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStage;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStatus;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrProductionReleaseBatchCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchExecutionEntryContractService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchExecutionProvisionCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesIndependentBatchPrerequisiteReceipt;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesIndependentBatchPrerequisiteReceiptService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesIndependentBatchPrerequisiteReceiptVerifyCommand;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;

@Component
public class MesProductionReleaseBatchExecutionPortImpl implements MesProductionReleaseBatchExecutionPort {

    private static final String CONTEXT_PREFIX = "PQC_RELEASE:";

    private final MesProEdhrBatchExecutionMapper batchExecutionMapper;
    private final MesProEdhrBatchExecutionService batchExecutionService;
    private final MesBatchExecutionEntryContractService entryContractService;
    private final MesIndependentBatchPrerequisiteReceiptService independentReceiptService;

    @Autowired
    public MesProductionReleaseBatchExecutionPortImpl(
            MesProEdhrBatchExecutionMapper batchExecutionMapper,
            MesProEdhrBatchExecutionService batchExecutionService,
            MesBatchExecutionEntryContractService entryContractService,
            MesIndependentBatchPrerequisiteReceiptService independentReceiptService) {
        this.batchExecutionMapper = batchExecutionMapper;
        this.batchExecutionService = batchExecutionService;
        this.entryContractService = entryContractService;
        this.independentReceiptService = independentReceiptService;
    }

    @Override
    public Long openOrCreate(MesProductionReleaseBatchExecutionCommand command) {
        if (command == null) {
            throw exception(BAD_REQUEST);
        }
        reloadIndependentReceipt(command);
        entryContractService.validate(toProvisionCommand(command));
        String activeContextKey = CONTEXT_PREFIX + command.getApplicationId();
        MesProEdhrBatchExecutionDO releaseBatch = batchExecutionMapper.selectByActiveContextKey(activeContextKey);
        if (releaseBatch != null) {
            requireSameFrozenContext(command, releaseBatch);
            return releaseBatch.getId();
        }
        MesProEdhrBatchExecutionDO legacy = batchExecutionMapper.selectByContext(
                command.getWorkOrderId(), command.getBatchCode(), command.getRouteId());
        if (legacy != null) {
            throw legacyBlocker(command, legacy);
        }
        return batchExecutionService.openOrCreateFromProductionRelease(
                new MesProEdhrProductionReleaseBatchCommand()
                        .setApplicationId(command.getApplicationId())
                        .setWorkOrderId(command.getWorkOrderId())
                        .setWorkOrderCode(command.getWorkOrderCode())
                        .setBatchCode(command.getBatchCode())
                        .setRouteId(command.getRouteId())
                        .setRouteVersionId(command.getRouteVersionId())
                        .setEntryType(command.getEntryType())
                        .setEntryBusinessId(command.getEntryBusinessId())
                        .setSourceCredentialType(command.getSourceCredentialType())
                        .setSourceCredentialId(command.getSourceCredentialId())
                        .setSourceRelationId(command.getSourceRelationId())
                        .setSourceContextHash(command.getSourceContextHash())
                        .setTenantId(command.getTenantId())
                        .setActiveOrderId(command.getActiveOrderId())
                        .setPickListBindingId(command.getPickListBindingId())
                        .setPickListId(command.getPickListId())
                        .setBindingVersion(command.getBindingVersion())
                        .setBatchPickListRelationId(command.getBatchPickListRelationId())
                        .setSourceSnapshotHash(command.getSourceSnapshotHash())
                        .setIdempotencyKey(command.getIdempotencyKey())
                        .setExpectedSourceVersion(command.getExpectedSourceVersion())
                        .setCompletionTransactionId(command.getCompletionTransactionId())
                        .setExpectedActiveOrderVersion(command.getExpectedActiveOrderVersion())
                        .setCompletionVersion(command.getCompletionVersion())
                        .setSourceVersion(command.getSourceVersion())
                        .setSourceBundleHash(command.getSourceBundleHash())
                        .setCompletionBackfillReceiptId(command.getCompletionBackfillReceiptId())
                        .setCompletionBackfillReceiptHash(command.getCompletionBackfillReceiptHash())
                        .setPickListHeaderSnapshotHash(command.getPickListHeaderSnapshotHash())
                        .setPickListLineSnapshotHash(command.getPickListLineSnapshotHash())
                        .setSourceEvidence(command.getSourceEvidence())
                        .setPayloadHash(command.getPayloadHash())
                        .setCompletionBackfillReceipt(command.getCompletionBackfillReceipt())
                        .setIndependentReceipt(command.getIndependentReceipt())
                        .setActiveContextKey(activeContextKey)
                        .setRemark("PQC production release application " + command.getApplicationId()));
    }

    private MesBatchExecutionProvisionCommand toProvisionCommand(
            MesProductionReleaseBatchExecutionCommand command) {
        return new MesBatchExecutionProvisionCommand()
                .setEntryType(command == null ? null : command.getEntryType())
                .setEntryBusinessId(command == null ? null : command.getEntryBusinessId())
                .setSourceCredentialType(command == null ? null : command.getSourceCredentialType())
                .setSourceCredentialId(command == null ? null : command.getSourceCredentialId())
                .setSourceRelationId(command == null ? null : command.getSourceRelationId())
                .setSourceContextHash(command == null ? null : command.getSourceContextHash())
                .setTenantId(command == null ? null : command.getTenantId())
                .setActiveOrderId(command == null ? null : command.getActiveOrderId())
                .setWorkOrderId(command == null ? null : command.getWorkOrderId())
                .setWorkOrderCode(command == null ? null : command.getWorkOrderCode())
                .setBatchCode(command == null ? null : command.getBatchCode())
                .setRouteId(command == null ? null : command.getRouteId())
                .setRouteVersionId(command == null ? null : command.getRouteVersionId())
                .setPickListBindingId(command == null ? null : command.getPickListBindingId())
                .setPickListId(command == null ? null : command.getPickListId())
                .setBindingVersion(command == null ? null : command.getBindingVersion())
                .setBatchPickListRelationId(command == null ? null : command.getBatchPickListRelationId())
                .setSourceSnapshotHash(command == null ? null : command.getSourceSnapshotHash())
                .setIdempotencyKey(command == null ? null : command.getIdempotencyKey())
                .setExpectedSourceVersion(command == null ? null : command.getExpectedSourceVersion())
                .setCompletionTransactionId(command == null ? null : command.getCompletionTransactionId())
                .setExpectedActiveOrderVersion(command == null ? null : command.getExpectedActiveOrderVersion())
                .setCompletionVersion(command == null ? null : command.getCompletionVersion())
                .setSourceVersion(command == null ? null : command.getSourceVersion())
                .setSourceBundleHash(command == null ? null : command.getSourceBundleHash())
                .setCompletionBackfillReceiptId(command == null ? null : command.getCompletionBackfillReceiptId())
                .setCompletionBackfillReceiptHash(command == null ? null : command.getCompletionBackfillReceiptHash())
                .setPickListHeaderSnapshotHash(command == null ? null : command.getPickListHeaderSnapshotHash())
                .setPickListLineSnapshotHash(command == null ? null : command.getPickListLineSnapshotHash())
                .setSourceEvidence(command == null ? null : command.getSourceEvidence())
                .setPayloadHash(command == null ? null : command.getPayloadHash())
                .setCompletionBackfillReceipt(command == null ? null : command.getCompletionBackfillReceipt())
                .setIndependentReceipt(command == null ? null : command.getIndependentReceipt());
    }

    private void reloadIndependentReceipt(MesProductionReleaseBatchExecutionCommand command) {
        if (!Set.of("MANUAL", "SCHEDULED", "PQC_INDEPENDENT").contains(command.getEntryType())) {
            return;
        }
        Long securityTenantId = TenantContextHolder.getTenantId();
        MesIndependentBatchPrerequisiteReceipt verified = independentReceiptService.verify(
                new MesIndependentBatchPrerequisiteReceiptVerifyCommand()
                        .setReceiptId(command.getSourceCredentialId())
                        .setEntryType(command.getEntryType())
                        .setSourceSnapshotHash(command.getSourceSnapshotHash()),
                securityTenantId);
        command.setIndependentReceipt(verified);
    }

    private void requireSameFrozenContext(
            MesProductionReleaseBatchExecutionCommand command,
            MesProEdhrBatchExecutionDO batch) {
        if (!Objects.equals(command.getWorkOrderId(), batch.getWorkOrderId())
                || !Objects.equals(command.getBatchCode(), batch.getBatchCode())
                || !Objects.equals(command.getRouteId(), batch.getRouteId())
                || !Objects.equals(command.getRouteVersionId(), batch.getRouteVersionId())) {
            throw legacyBlocker(command, batch);
        }
    }

    private MesReleaseFlowBlockerException legacyBlocker(
            MesProductionReleaseBatchExecutionCommand command,
            MesProEdhrBatchExecutionDO batch) {
        String reason = "existing batch execution is not associated with this production release application";
        return new MesReleaseFlowBlockerException(reason, new MesReleaseFlowFailureRespVO()
                .setStage(MesReleaseFlowStage.SP_2)
                .setCurrentStatus(MesReleaseFlowStatus.PQC_RELEASE_PENDING)
                .setBlockers(List.of(new MesReleaseFlowBlocker()
                        .setBlockerType(MesReleaseFlowBlockerType.LEGACY_BATCH_EXECUTION_MIGRATION_REQUIRED)
                        .setObjectType("BATCH_EXECUTION")
                        .setObjectId(batch == null ? null : String.valueOf(batch.getId()))
                        .setObjectCode(command.getBatchCode())
                        .setReason(reason)
                        .setSuggestion("migrate the legacy batch with approved evidence before retrying"))));
    }
}
