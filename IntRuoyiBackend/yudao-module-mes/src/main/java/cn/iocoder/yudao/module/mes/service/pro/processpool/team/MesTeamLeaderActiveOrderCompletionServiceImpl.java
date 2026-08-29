package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_IDEMPOTENCY_CONFLICT;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_NOT_OWNED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_PERSISTENCE_FAILED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_PROGRESS_NOT_COMPLETE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_VERSION_CONFLICT;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS;

@Service
public class MesTeamLeaderActiveOrderCompletionServiceImpl implements MesTeamLeaderActiveOrderCompletionService {

    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesProcessPoolActiveOrderCompletionReceiptMapper receiptMapper;
    private final MesTeamLeaderActiveOrderCompletionProgressPort progressPort;
    private final MesTeamLeaderActiveOrderCompletionBackfillPort backfillPort;

    public MesTeamLeaderActiveOrderCompletionServiceImpl(
            MesProcessPoolActiveOrderMapper activeOrderMapper,
            MesProcessPoolActiveOrderCompletionReceiptMapper receiptMapper,
            MesTeamLeaderActiveOrderCompletionProgressPort progressPort,
            MesTeamLeaderActiveOrderCompletionBackfillPort backfillPort) {
        this.activeOrderMapper = activeOrderMapper;
        this.receiptMapper = receiptMapper;
        this.progressPort = progressPort;
        this.backfillPort = backfillPort;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesTeamLeaderActiveOrderCompletionResult complete(
            Long leaderUserId, MesTeamLeaderActiveOrderCompletionCommand command) {
        validateCommand(command);
        MesProcessPoolActiveOrderDO activeOrder = activeOrderMapper.selectByIdForUpdate(command.getActiveOrderId());
        if (activeOrder == null) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS, command.getActiveOrderId());
        }
        if (!java.util.Objects.equals(activeOrder.getLeaderUserId(), leaderUserId)) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_NOT_OWNED, activeOrder.getId());
        }
        String requestPayloadHash = sha256(activeOrder.getId() + "|" + command.getExpectedVersion()
                + "|" + command.getIdempotencyKey());
        MesProcessPoolActiveOrderCompletionReceiptDO existingByOrder =
                receiptMapper.selectByActiveOrderIdForUpdate(activeOrder.getId());
        MesProcessPoolActiveOrderCompletionReceiptDO existingByKey =
                receiptMapper.selectByIdempotencyKeyForUpdate(command.getIdempotencyKey());
        if (existingByOrder != null || existingByKey != null) {
            MesProcessPoolActiveOrderCompletionReceiptDO existing = existingByOrder != null ? existingByOrder : existingByKey;
            if (java.util.Objects.equals(existing.getActiveOrderId(), activeOrder.getId())
                    && java.util.Objects.equals(existing.getRequestIdempotencyKey(), command.getIdempotencyKey())
                    && java.util.Objects.equals(existing.getRequestPayloadHash(), requestPayloadHash)) {
                String currentSourceSnapshotHash = backfillPort.readSourceSnapshotHash(
                        leaderUserId, activeOrder, command);
                if (!java.util.Objects.equals(existing.getSourceSnapshotHash(), currentSourceSnapshotHash)) {
                    throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_IDEMPOTENCY_CONFLICT,
                            activeOrder.getId(), command.getIdempotencyKey());
                }
                return toResult(existing);
            }
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_IDEMPOTENCY_CONFLICT, activeOrder.getId(),
                    command.getIdempotencyKey());
        }
        if (!java.util.Objects.equals(activeOrder.getActiveStatus(), "ACTIVE")) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING, activeOrder.getId(), "ACTIVE_ORDER_NOT_ACTIVE");
        }
        if (!java.util.Objects.equals(activeOrder.getVersion(), command.getExpectedVersion())) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_VERSION_CONFLICT, activeOrder.getId(),
                    command.getExpectedVersion(), activeOrder.getVersion());
        }

        MesTeamLeaderActiveOrderCompletionProgress progress = progressPort.read(leaderUserId, activeOrder);
        if (progress == null || !progress.isDoubleComplete()) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_PROGRESS_NOT_COMPLETE, activeOrder.getId());
        }

        MesTeamLeaderActiveOrderCompletionBackfillDraft draft = backfillPort.prepare(leaderUserId, activeOrder, command);
        if (draft == null || draft.getSourceSnapshotHash() == null || draft.getSourceSnapshotHash().isBlank()) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING, activeOrder.getId(), "SOURCE_SNAPSHOT_HASH");
        }
        validateDraft(activeOrder.getId(), draft);

        draft.setMaterializedBy(leaderUserId);
        backfillPort.write(draft, activeOrder.getId());
        if (draft.getBatchRecordId() == null || draft.getProcessInspectionId() == null
                || (Boolean.TRUE.equals(draft.getHasActualLoss()) && draft.getLossRecordId() == null)
                || (!Boolean.TRUE.equals(draft.getHasActualLoss()) && draft.getLossRecordId() != null)) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_PERSISTENCE_FAILED, activeOrder.getId());
        }
        Integer currentVersion = activeOrder.getVersion() == null ? 0 : activeOrder.getVersion();
        if (activeOrderMapper.markCompleted(activeOrder.getId(), currentVersion, leaderUserId) != 1) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_VERSION_CONFLICT, activeOrder.getId(),
                    currentVersion, currentVersion + 1);
        }
        LocalDateTime completedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        MesProcessPoolActiveOrderCompletionReceiptDO receipt = MesProcessPoolActiveOrderCompletionReceiptDO.builder()
                .activeOrderId(activeOrder.getId())
                .build();
        receipt.setTenantId(activeOrder.getTenantId());
        receipt.setWorkOrderId(activeOrder.getWorkOrderId());
        receipt.setBatchCode(draft.getBatchCode());
        receipt.setRouteId(draft.getRouteId());
        receipt.setRouteVersionId(draft.getRouteVersionId());
        receipt.setLeaderUserId(leaderUserId);
        receipt.setRequestIdempotencyKey(command.getIdempotencyKey());
        receipt.setRequestPayloadHash(requestPayloadHash);
        receipt.setSourceSnapshotHash(draft.getSourceSnapshotHash());
        receipt.setFormalSourceSnapshotJson(draft.getFormalSourceSnapshotJson());
        receipt.setSignatureSnapshotJson(draft.getSignatureSnapshotJson());
        receipt.setExpectedVersion(command.getExpectedVersion());
        receipt.setCompletedVersion(currentVersion + 1);
        receipt.setReceiptStatus(MesProcessPoolActiveOrderCompletionReceiptDO.RECEIPT_STATUS_BACKFILL_SUCCEEDED);
        receipt.setCompletionStatus(MesProcessPoolActiveOrderCompletionReceiptDO.STATUS_SUCCESS);
        receipt.setBatchRecordStatus(draft.getBatchRecordStatus());
        receipt.setProcessInspectionStatus(draft.getProcessInspectionStatus());
        receipt.setBatchRecordId(draft.getBatchRecordId());
        receipt.setProcessInspectionId(draft.getProcessInspectionId());
        receipt.setLossReportStatus(draft.getLossReportStatus());
        receipt.setHasActualLoss(draft.getHasActualLoss());
        receipt.setLossQuantity(draft.getLossQuantity());
        receipt.setLossRecordId(draft.getLossRecordId());
        receipt.setZeroLossConfirmationSnapshot(draft.getZeroLossConfirmationSnapshot());
        receipt.setLossConditionFactsJson(draft.getLossConditionFactsJson());
        receipt.setBatchRecordSourceIdsJson(draft.getBatchRecordSourceIdsJson());
        receipt.setProcessInspectionSourceIdsJson(draft.getProcessInspectionSourceIdsJson());
        receipt.setLossSourceHash(draft.getLossSourceHash());
        receipt.setProvisionHandoff(MesProcessPoolActiveOrderCompletionReceiptDO.PROVISION_HANDOFF_PENDING_FLOW6);
        receipt.setCompletedAt(completedAt);
        receipt.setCompletedBy(leaderUserId);
        receipt.setCreateTime(completedAt);
        receipt.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt));
        if (receiptMapper.insert(receipt) <= 0 || receipt.getId() == null) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_PERSISTENCE_FAILED, activeOrder.getId());
        }
        return toResult(receipt);
    }

    private static void validateDraft(Long activeOrderId, MesTeamLeaderActiveOrderCompletionBackfillDraft draft) {
        if (!MesProcessPoolActiveOrderCompletionReceiptDO.BACKFILL_STATUS_SUCCESS
                .equals(draft.getBatchRecordStatus())
                || !MesProcessPoolActiveOrderCompletionReceiptDO.BACKFILL_STATUS_SUCCESS
                .equals(draft.getProcessInspectionStatus())
                 || draft.getBatchRecordSourceIdsJson() == null || draft.getBatchRecordSourceIdsJson().isBlank()
                 || draft.getProcessInspectionSourceIdsJson() == null
                 || draft.getProcessInspectionSourceIdsJson().isBlank()
                 || draft.getFormalSourceSnapshotJson() == null || draft.getFormalSourceSnapshotJson().isBlank()
                 || draft.getSignatureSnapshotJson() == null || draft.getSignatureSnapshotJson().isBlank()
                || draft.getWorkOrderId() == null || draft.getBatchCode() == null || draft.getBatchCode().isBlank()
                || draft.getRouteId() == null || draft.getRouteVersionId() == null) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING, activeOrderId,
                    "BACKFILL_DRAFT_INCOMPLETE");
        }
        if (draft.getHasActualLoss() == null || draft.getLossQuantity() == null
                || draft.getLossQuantity().signum() < 0) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING, activeOrderId,
                    "LOSS_FACT_INCOMPLETE");
        }
        validateLossConditions(activeOrderId, draft);
        if (draft.getHasActualLoss()) {
            if (!MesProcessPoolActiveOrderCompletionReceiptDO.LOSS_REPORT_STATUS_SUCCESS
                    .equals(draft.getLossReportStatus())
                    || draft.getLossQuantity().signum() <= 0
                    || draft.getLossRecordId() == null) {
                throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING, activeOrderId,
                        "LOSS_RECORD_REQUIRED");
            }
        } else if (!MesProcessPoolActiveOrderCompletionReceiptDO.LOSS_REPORT_STATUS_NOT_REQUIRED
                .equals(draft.getLossReportStatus())
                || draft.getLossQuantity().signum() != 0
                || draft.getLossRecordId() != null
                || draft.getZeroLossConfirmationSnapshot() == null
                || draft.getZeroLossConfirmationSnapshot().isBlank()) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING, activeOrderId,
                    "ZERO_LOSS_CONFIRMATION_REQUIRED");
        }
    }

    private static void validateLossConditions(Long activeOrderId,
                                               MesTeamLeaderActiveOrderCompletionBackfillDraft draft) {
        if (draft.getLossConditionFactsJson() == null || draft.getLossConditionFactsJson().isBlank()) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING, activeOrderId,
                    "LOSS_CONDITION_FACTS_REQUIRED");
        }
        final List<MesTeamLeaderActiveOrderCompletionLossCondition> conditions;
        try {
            conditions = JsonUtils.parseArray(draft.getLossConditionFactsJson(),
                    MesTeamLeaderActiveOrderCompletionLossCondition.class);
        } catch (RuntimeException parseException) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING, activeOrderId,
                    "LOSS_CONDITION_FACTS_INVALID");
        }
        if (conditions == null || conditions.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING, activeOrderId,
                    "LOSS_CONDITION_FACTS_EMPTY");
        }
        boolean hasActualLoss = false;
        java.math.BigDecimal totalLoss = java.math.BigDecimal.ZERO;
        for (MesTeamLeaderActiveOrderCompletionLossCondition condition : conditions) {
            if (condition == null || condition.getProcessId() == null || condition.getStatus() == null
                    || condition.getSourceHash() == null || condition.getSourceHash().isBlank()
                    || condition.getHasActualLoss() == null || condition.getLossQuantity() == null
                    || condition.getLossQuantity().signum() < 0) {
                throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING, activeOrderId,
                        "LOSS_CONDITION_FACT_INCOMPLETE");
            }
            if (MesTeamLeaderActiveOrderCompletionLossCondition.BLOCKED.equals(condition.getStatus())) {
                throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING, activeOrderId,
                        "LOSS_CONDITION_BLOCKED");
            }
            if (MesTeamLeaderActiveOrderCompletionLossCondition.REQUIRED.equals(condition.getStatus())) {
                if (!condition.getHasActualLoss() || condition.getLossQuantity().signum() <= 0
                        || condition.getLossRecordId() == null) {
                    throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING, activeOrderId,
                            "LOSS_CONDITION_REQUIRED_INVALID");
                }
                hasActualLoss = true;
                totalLoss = totalLoss.add(condition.getLossQuantity());
            } else if (MesTeamLeaderActiveOrderCompletionLossCondition.NO_LOSS.equals(condition.getStatus())) {
                if (condition.getHasActualLoss() || condition.getLossQuantity().signum() != 0
                        || condition.getLossRecordId() != null
                        || condition.getZeroLossConfirmationSnapshot() == null
                        || condition.getZeroLossConfirmationSnapshot().isBlank()) {
                    throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING, activeOrderId,
                            "LOSS_CONDITION_NO_LOSS_INVALID");
                }
            } else {
                throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING, activeOrderId,
                        "LOSS_CONDITION_STATUS_INVALID");
            }
        }
        if (!java.util.Objects.equals(draft.getHasActualLoss(), hasActualLoss)
                || draft.getLossQuantity().compareTo(totalLoss) != 0) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING, activeOrderId,
                    "LOSS_FACT_SUMMARY_MISMATCH");
        }
    }

    private static void validateCommand(MesTeamLeaderActiveOrderCompletionCommand command) {
        if (command == null || command.getActiveOrderId() == null || command.getExpectedVersion() == null
                || command.getIdempotencyKey() == null || command.getIdempotencyKey().isBlank()) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING,
                    command == null ? null : command.getActiveOrderId(), "COMMAND_REQUIRED");
        }
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is unavailable", exception);
        }
    }

    private static MesTeamLeaderActiveOrderCompletionResult toResult(
            MesProcessPoolActiveOrderCompletionReceiptDO receipt) {
        return new MesTeamLeaderActiveOrderCompletionResult()
                .setActiveOrderId(receipt.getActiveOrderId())
                .setCompletionReceiptId(receipt.getId())
                .setBatchCode(receipt.getBatchCode())
                .setRouteId(receipt.getRouteId())
                .setRouteVersionId(receipt.getRouteVersionId())
                .setReceiptHash(receipt.getReceiptHash())
                .setFlow6ReceiptStatus(MesFlow6CompletionBackfillReceipt.STATUS_BACKFILL_SUCCEEDED)
                .setActiveOrderVersion(receipt.getCompletedVersion())
                .setBatchRecordStatus(receipt.getBatchRecordStatus())
                .setProcessInspectionStatus(receipt.getProcessInspectionStatus())
                .setLossReportStatus(receipt.getLossReportStatus())
                .setHasActualLoss(receipt.getHasActualLoss())
                .setLossQuantity(receipt.getLossQuantity())
                .setProvisionHandoff(receipt.getProvisionHandoff());
    }
}
