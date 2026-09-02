package cn.iocoder.yudao.module.mes.service.pro.simulation.stage4;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionAttachmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchProvisioningRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionBackfillDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionAttachmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchProvisioningRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionBackfillMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderSourceTypeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderTypeEnum;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceCaptureCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceEntryType;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceLinkType;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceProvisionStatus;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceSource;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceSourceHash;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceValidationResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityValidator;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHasher;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrSpecialNodeAttachment;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrSpecialNodeAttachmentPrepareUploadCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrSpecialNodeAttachmentPrepareUploadResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrSpecialNodeAttachmentHasher;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionReceiptHash;
import com.alibaba.fastjson.JSON;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class MesStage4DossierUploadSimulationServiceImpl
        implements MesStage4DossierUploadSimulationService {

    public static final String INPUT_MODE_STAGE2_5_BATCH_EXECUTION = "STAGE2_5_BATCH_EXECUTION";
    public static final String INPUT_MODE_INDEPENDENT_BATCH_EXECUTION =
            "STAGE4_INDEPENDENT_BATCH_EXECUTION";
    private static final String DETAIL_PATH = "/mes/pro/feedback/edhr-batch-execution/detail";
    private static final String COMPLETE_BATCH_EXECUTION_SCHEMA =
            "stage4IndependentBatchExecutionSnapshot.v1";
    private static final String FILE_EVIDENCE_SOURCE =
            "STAGE4_INDEPENDENT_FIXTURE_FORMAL_ATTACHMENT_UPLOAD";
    private static final String SIMULATION_MARKER = "[STAGE4_SIMULATION]";
    private static final Pattern RUN_ID = Pattern.compile("[A-Za-z0-9._:-]{1,128}");
    private static final Pattern HASH = Pattern.compile("[0-9a-f]{64}");
    private static final List<NodeDefinition> NODES = List.of(
            new NodeDefinition("INCOMING_INSPECTION_REPORT", "来料检报告", 0, "incoming-inspection-report.pdf"),
            new NodeDefinition("STERILIZATION_REPORT", "灭菌报告", 9000, "sterilization-report.pdf"),
            new NodeDefinition("FINISHED_PRODUCT_INSPECTION_REPORT", "成品检报告", 9010,
                    "finished-product-inspection-report.pdf"),
            new NodeDefinition("FINISHED_PRODUCT_INSPECTION_RECORD", "成品检记录", 9020,
                    "finished-product-inspection-record.pdf"));

    private final MesProEdhrBatchExecutionMapper batchExecutionMapper;
    private final MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    private final MesProEdhrBatchProvisioningRecordMapper provisioningRecordMapper;
    private final MesProEdhrWorkTaskAssignmentRuleMapper assignmentRuleMapper;
    private final MesProcessPoolActiveOrderCompletionReceiptMapper completionReceiptMapper;
    private final MesProcessPoolActiveOrderCompletionBackfillMapper completionBackfillMapper;
    private final MesProcessPoolActiveOrderPickListBindingMapper pickListBindingMapper;
    private final MesProcessPoolActiveOrderPickListBindingItemMapper pickListBindingItemMapper;
    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesProWorkOrderMapper workOrderMapper;
    private final ErpKingdeeProductionPickListMapper pickListMapper;
    private final ErpKingdeeProductionPickListItemMapper pickListItemMapper;
    private final MesProBatchRecordExecutionAttachmentMapper attachmentMapper;
    private final MesProEdhrBatchTraceabilityService traceabilityService;
    private final MesProEdhrBatchExecutionService batchExecutionService;
    private final FileService fileService;

    public MesStage4DossierUploadSimulationServiceImpl(
            MesProEdhrBatchExecutionMapper batchExecutionMapper,
            MesProEdhrBatchExecutionTaskMapper batchTaskMapper,
            MesProEdhrBatchProvisioningRecordMapper provisioningRecordMapper,
            MesProEdhrWorkTaskAssignmentRuleMapper assignmentRuleMapper,
            MesProcessPoolActiveOrderCompletionReceiptMapper completionReceiptMapper,
            MesProcessPoolActiveOrderCompletionBackfillMapper completionBackfillMapper,
            MesProcessPoolActiveOrderPickListBindingMapper pickListBindingMapper,
            MesProcessPoolActiveOrderPickListBindingItemMapper pickListBindingItemMapper,
            MesProcessPoolActiveOrderMapper activeOrderMapper,
            MesProWorkOrderMapper workOrderMapper,
            ErpKingdeeProductionPickListMapper pickListMapper,
            ErpKingdeeProductionPickListItemMapper pickListItemMapper,
            MesProBatchRecordExecutionAttachmentMapper attachmentMapper,
            MesProEdhrBatchTraceabilityService traceabilityService,
            MesProEdhrBatchExecutionService batchExecutionService,
            FileService fileService) {
        this.batchExecutionMapper = batchExecutionMapper;
        this.batchTaskMapper = batchTaskMapper;
        this.provisioningRecordMapper = provisioningRecordMapper;
        this.assignmentRuleMapper = assignmentRuleMapper;
        this.completionReceiptMapper = completionReceiptMapper;
        this.completionBackfillMapper = completionBackfillMapper;
        this.pickListBindingMapper = pickListBindingMapper;
        this.pickListBindingItemMapper = pickListBindingItemMapper;
        this.activeOrderMapper = activeOrderMapper;
        this.workOrderMapper = workOrderMapper;
        this.pickListMapper = pickListMapper;
        this.pickListItemMapper = pickListItemMapper;
        this.attachmentMapper = attachmentMapper;
        this.traceabilityService = traceabilityService;
        this.batchExecutionService = batchExecutionService;
        this.fileService = fileService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesStage4DossierUploadSimulationResult simulate(MesStage4DossierUploadSimulationCommand command) {
        Long actorUserId = requireActor(command);
        String simulationRunId = normalizeRunId(command.getSimulationRunId());
        String inputMode = normalizeInputMode(command.getInputMode());
        Stage4InputResolution resolution = resolveStage4Input(command, inputMode, actorUserId, simulationRunId);
        MesProEdhrBatchExecutionDO batch = resolution.batch();
        FormalStage4Source source = resolution.source();
        String cleanedRunId = cleanupOwnedAttachments(batch.getId());
        batch.setRemark(batch.getRemark() + SIMULATION_MARKER + "[simulationRunId=" + simulationRunId + "]");
        batchExecutionMapper.updateById(batch);
        Map<String, MesProEdhrBatchExecutionTaskDO> tasks = loadFixtureTasks(batch.getId());
        Map<String, Object> completeBatchExecutionSnapshot = resolution.completeBatchExecutionSnapshot();
        Map<String, Object> inputSnapshot = completeBatchExecutionSnapshot == null
                ? buildInputSnapshot(batch, tasks, simulationRunId, source)
                : buildStage4InputFromCompleteBatchExecution(completeBatchExecutionSnapshot,
                batch, tasks, simulationRunId, source);
        MesStage4DossierUploadSimulationContractValidator.validateInput(inputSnapshot);
        batch.setAggregateHash(hash(inputSnapshot));
        batchExecutionMapper.updateById(batch);

        for (NodeDefinition node : NODES) {
            MesProEdhrBatchExecutionTaskDO task = tasks.get(node.nodeType());
            MesProEdhrSpecialNodeAttachmentPrepareUploadResult prepared =
                    batchExecutionService.prepareSpecialNodeAttachmentUpload(
                            new MesProEdhrSpecialNodeAttachmentPrepareUploadCommand()
                                    .setTaskId(task.getId())
                                    .setFileName(node.fileName())
                                    .setContentType("application/pdf")
                                    .setContent(buildFileContent(simulationRunId, node.nodeType())));
            batchExecutionService.completePreReleaseDossierNode(task.getId(), actorUserId,
                    "STERILIZATION_REPORT".equals(node.nodeType())
                            ? "STE-STAGE4-" + shortRunId(simulationRunId) : null,
                    List.of(toAttachment(prepared)));
            markAttachmentOwned(batch.getId(), task.getId(), simulationRunId);
        }

        Map<String, Object> dossierSnapshot = buildDossierSnapshot(batch, simulationRunId,
                source.provisioningRecord().getSourceSnapshotHash());
        MesStage4DossierUploadSimulationContractValidator.validateOutput(dossierSnapshot);
        return new MesStage4DossierUploadSimulationResult()
                .setSimulationRunId(simulationRunId)
                .setInputMode(inputMode)
                .setCleanedSimulationRunId(cleanedRunId)
                .setBatchExecutionId(String.valueOf(batch.getId()))
                .setBatchExecutionCode(batch.getBatchExecutionCode())
                .setDetailPath(DETAIL_PATH + "?id=" + batch.getId() + "&simulationRunId=" + simulationRunId)
                .setCompleteBatchExecutionSnapshot(completeBatchExecutionSnapshot == null
                        ? inputSnapshot : completeBatchExecutionSnapshot)
                .setBatchExecutionSnapshot(inputSnapshot)
                .setBatchExecutionDossierSnapshot(dossierSnapshot)
                .setDossierReadyForRelease(true);
    }

    private Long requireActor(MesStage4DossierUploadSimulationCommand command) {
        if (command == null || command.getActorUserId() == null || command.getActorUserId() <= 0) {
            throw new IllegalArgumentException("Stage4 simulation actor is required");
        }
        if (TenantContextHolder.getTenantId() == null) {
            throw new IllegalStateException("Stage4 simulation requires an active tenant context");
        }
        return command.getActorUserId();
    }

    private String normalizeInputMode(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (Objects.equals(INPUT_MODE_STAGE2_5_BATCH_EXECUTION, value)
                || Objects.equals(INPUT_MODE_INDEPENDENT_BATCH_EXECUTION, value)) {
            return value;
        }
        throw new IllegalArgumentException("Stage4 input mode is unsupported");
    }

    private Stage4InputResolution resolveStage4Input(MesStage4DossierUploadSimulationCommand command,
                                                     String inputMode,
                                                     Long actorUserId,
                                                     String simulationRunId) {
        if (Objects.equals(INPUT_MODE_INDEPENDENT_BATCH_EXECUTION, inputMode)) {
            IndependentBatchExecutionInputFixture fixture =
                    createIndependentBatchExecutionInputFixture(actorUserId, simulationRunId);
            return new Stage4InputResolution(fixture.batch(), fixture.source(),
                    fixture.completeBatchExecutionSnapshot());
        }
        MesProEdhrBatchExecutionDO batch = requireStage2_5Batch(command);
        return new Stage4InputResolution(batch, requireFormalSource(batch), null);
    }

    private MesProEdhrBatchExecutionDO requireStage2_5Batch(
            MesStage4DossierUploadSimulationCommand command) {
        if (command.getBatchExecutionId() == null || command.getBatchExecutionId() <= 0
                || command.getStage2_5SimulationRunId() == null
                || command.getStage2_5SimulationRunId().isBlank()) {
            throw new IllegalArgumentException("Stage4 requires the Stage2.5 batch and source run");
        }
        String sourceRunId = normalizeRunId(command.getStage2_5SimulationRunId());
        MesProEdhrBatchExecutionDO batch = batchExecutionMapper.selectById(command.getBatchExecutionId());
        if (batch == null || !Objects.equals(batch.getTenantId(), TenantContextHolder.getTenantId())
                || batch.getRemark() == null
                || !batch.getRemark().contains("[STAGE2_5_SIMULATION][simulationRunId=" + sourceRunId + "]")) {
            throw new IllegalStateException("STAGE4_STAGE2_5_BATCH_SOURCE_INVALID");
        }
        return batch;
    }

    private FormalStage4Source requireFormalSource(MesProEdhrBatchExecutionDO batch) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        MesProEdhrBatchProvisioningRecordDO provisioningRecord = provisioningRecordMapper
                .selectByBatchExecutionId(tenantId, batch.getId());
        if (provisioningRecord == null
                || !Objects.equals("ACTIVE_ORDER_COMPLETION", provisioningRecord.getEntryType())
                || provisioningRecord.getSourceCredentialId() == null
                || provisioningRecord.getSourceCredentialHash() == null
                || provisioningRecord.getSourceSnapshotHash() == null
                || !HASH.matcher(provisioningRecord.getSourceSnapshotHash()).matches()) {
            throw new IllegalStateException("STAGE4_FORMAL_PROVISIONING_SOURCE_REQUIRED");
        }
        Long receiptId = parsePositiveLong(provisioningRecord.getSourceCredentialId(),
                "STAGE4_COMPLETION_RECEIPT_SOURCE_INVALID");
        MesProcessPoolActiveOrderCompletionReceiptDO receipt = completionReceiptMapper.selectById(receiptId);
        if (receipt == null || !Objects.equals(receipt.getTenantId(), tenantId)
                || !Objects.equals(receipt.getId(), receiptId)
                || !Objects.equals(receipt.getReceiptHash(), provisioningRecord.getSourceCredentialHash())
                || !Objects.equals(receipt.getSourceSnapshotHash(), provisioningRecord.getSourceSnapshotHash())
                || !Objects.equals(receipt.getWorkOrderId(), batch.getWorkOrderId())
                || !Objects.equals(receipt.getBatchCode(), batch.getBatchCode())
                || !Objects.equals(receipt.getRouteId(), batch.getRouteId())
                || !Objects.equals(receipt.getRouteVersionId(), batch.getRouteVersionId())
                || !isSuccessfulCompletionReceipt(receipt)) {
            throw new IllegalStateException("STAGE4_COMPLETION_RECEIPT_SOURCE_INVALID");
        }
        MesProcessPoolActiveOrderCompletionReceiptDO lockedReceipt = completionReceiptMapper
                .selectByActiveOrderIdForUpdate(receipt.getActiveOrderId());
        if (lockedReceipt == null || !Objects.equals(lockedReceipt.getId(), receipt.getId())
                || !Objects.equals(lockedReceipt.getReceiptHash(), receipt.getReceiptHash())
                || !Objects.equals(lockedReceipt.getSourceSnapshotHash(), receipt.getSourceSnapshotHash())) {
            throw new IllegalStateException("STAGE4_COMPLETION_RECEIPT_SOURCE_INVALID");
        }
        MesProcessPoolActiveOrderPickListBindingDO binding =
                pickListBindingMapper.selectByActiveOrderId(receipt.getActiveOrderId());
        if (binding == null || !Objects.equals(binding.getTenantId(), tenantId)
                || !Objects.equals(binding.getActiveOrderId(), receipt.getActiveOrderId())
                || !Objects.equals(binding.getWorkOrderId(), receipt.getWorkOrderId())
                || binding.getId() == null || binding.getPickListId() == null
                || binding.getSourceSnapshotHash() == null || binding.getSourceSnapshotHash().isBlank()
                || !"BOUND".equalsIgnoreCase(binding.getBindingStatus())) {
            throw new IllegalStateException("STAGE4_PICK_LIST_BINDING_SOURCE_INVALID");
        }
        return new FormalStage4Source(provisioningRecord, lockedReceipt, binding);
    }

    private boolean isSuccessfulCompletionReceipt(MesProcessPoolActiveOrderCompletionReceiptDO receipt) {
        return receipt != null
                && Objects.equals(MesProcessPoolActiveOrderCompletionReceiptDO.RECEIPT_STATUS_BACKFILL_SUCCEEDED,
                receipt.getReceiptStatus())
                && Objects.equals(MesProcessPoolActiveOrderCompletionReceiptDO.BACKFILL_STATUS_SUCCESS,
                receipt.getCompletionStatus())
                && Objects.equals(MesProcessPoolActiveOrderCompletionReceiptDO.BACKFILL_STATUS_SUCCESS,
                receipt.getBatchRecordStatus())
                && Objects.equals(MesProcessPoolActiveOrderCompletionReceiptDO.BACKFILL_STATUS_SUCCESS,
                receipt.getProcessInspectionStatus())
                && receipt.getBatchRecordId() != null
                && receipt.getProcessInspectionId() != null
                && receipt.getHasActualLoss() != null
                && receipt.getSourceSnapshotHash() != null
                && HASH.matcher(receipt.getSourceSnapshotHash()).matches()
                && receipt.getReceiptHash() != null
                && HASH.matcher(receipt.getReceiptHash()).matches();
    }

    private Long parsePositiveLong(String value, String errorCode) {
        try {
            Long parsed = Long.valueOf(value);
            if (parsed > 0) {
                return parsed;
            }
        } catch (RuntimeException ignored) {
            // The caller receives the formal Stage4 source validation error below.
        }
        throw new IllegalStateException(errorCode);
    }

    private String normalizeRunId(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (!RUN_ID.matcher(value).matches()) {
            throw new IllegalArgumentException("simulationRunId contains unsupported characters");
        }
        return value;
    }

    private String cleanupOwnedAttachments(Long batchExecutionId) {
        List<MesProBatchRecordExecutionAttachmentDO> attachments = attachmentMapper
                .selectListByBatchExecutionId(batchExecutionId).stream()
                .filter(item -> Objects.equals(item.getBatchExecutionId(), batchExecutionId)
                        && item.getReasonText() != null && item.getReasonText().startsWith(SIMULATION_MARKER))
                .toList();
        if (attachments.isEmpty()) return null;
        String runId = attachments.get(0).getReasonText()
                .replace(SIMULATION_MARKER + "[simulationRunId=", "");
        int end = runId.indexOf(']');
        if (end <= 0 || !RUN_ID.matcher(runId.substring(0, end)).matches()) {
            throw new IllegalStateException("STAGE4_SIMULATION_CLEANUP_SCOPE_INVALID");
        }
        Set<Long> fileIds = attachments.stream().map(MesProBatchRecordExecutionAttachmentDO::getFileId)
                .filter(Objects::nonNull).collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        try {
            if (!fileIds.isEmpty()) {
                fileService.deleteFileList(new ArrayList<>(fileIds));
            }
        } catch (Exception exception) {
            throw new IllegalStateException("STAGE4_SIMULATION_FILE_CLEANUP_FAILED", exception);
        }
        Set<Long> ownedTaskIds = attachments.stream()
                .map(MesProBatchRecordExecutionAttachmentDO::getBatchTaskId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        attachments.forEach(item -> attachmentMapper.deleteById(item.getId()));
        for (Long taskId : ownedTaskIds) {
            MesProEdhrBatchExecutionTaskDO task = batchTaskMapper.selectById(taskId);
            if (task == null || !MesStage4DossierUploadSimulationContractValidator.REQUIRED_NODE_TYPES
                    .contains(task.getNodeType())) {
                continue;
            }
            batchTaskMapper.update(null, new LambdaUpdateWrapper<MesProEdhrBatchExecutionTaskDO>()
                    .eq(MesProEdhrBatchExecutionTaskDO::getId, taskId)
                    .set(MesProEdhrBatchExecutionTaskDO::getStatus,
                            MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING)
                    .set(MesProEdhrBatchExecutionTaskDO::getApprovedAt, null)
                    .set(MesProEdhrBatchExecutionTaskDO::getSpecialPayloadJson, JSON.toJSONString(Map.of(
                            "stageCode", "STAGE4_DOSSIER_UPLOAD",
                            "simulationRunId", runId.substring(0, end),
                            "isSimulation", true,
                            "status", "PENDING_UPLOAD"))));
        }
        return runId.substring(0, end);
    }

    private void markAttachmentOwned(Long batchExecutionId, Long batchTaskId, String runId) {
        List<MesProBatchRecordExecutionAttachmentDO> attachments = attachmentMapper
                .selectListByBatchExecutionId(batchExecutionId).stream()
                .filter(item -> Objects.equals(item.getBatchTaskId(), batchTaskId))
                .max(java.util.Comparator.comparing(MesProBatchRecordExecutionAttachmentDO::getId))
                .stream().toList();
        attachments.forEach(item -> {
            item.setReasonText(SIMULATION_MARKER + "[simulationRunId=" + runId + "]");
            item.setAttachmentHash(MesProEdhrSpecialNodeAttachmentHasher.attachmentHash(item));
            attachmentMapper.updateById(item);
        });
    }

    private IndependentBatchExecutionInputFixture createIndependentBatchExecutionInputFixture(
            Long actorUserId, String simulationRunId) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        LocalDateTime now = LocalDateTime.now();
        long base = 8_000_000_000L + Integer.toUnsignedLong(simulationRunId.hashCode()) * 10L;
        String safe = shortRunId(simulationRunId);
        String marker = MesStage4DossierUploadSimulationMarker.value(simulationRunId);
        Long routeId = base + 300;
        String routeSnapshotJson = buildRouteSnapshot(actorUserId, simulationRunId);
        MesProEdhrWorkTaskAssignmentRuleDO archiveRule = new MesProEdhrWorkTaskAssignmentRuleDO()
                .setScopeType("ROUTE")
                .setScopeId(routeId)
                .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_ARCHIVE)
                .setAssigneeUserId(actorUserId)
                .setCandidateSourceType("USER")
                .setCandidateSourceId(actorUserId)
                .setDueMinutes(1440)
                .setEnabled(true)
                .setRemark(marker);
        if (assignmentRuleMapper.insert(archiveRule) != 1 || archiveRule.getId() == null) {
            throw new IllegalStateException("STAGE4_INDEPENDENT_ARCHIVE_RULE_CREATE_FAILED");
        }

        MesProWorkOrderDO workOrder = new MesProWorkOrderDO()
                .setCode("STAGE4-WO-" + safe)
                .setName("Stage4 independent batch execution fixture")
                .setType(MesProWorkOrderTypeEnum.SELF.getType())
                .setOrderSourceType(MesProWorkOrderSourceTypeEnum.STORE.getType())
                .setOrderSourceCode("STAGE4-INDEPENDENT-" + safe)
                .setProductId(base + 100)
                .setQuantity(BigDecimal.ONE)
                .setQuantityProduced(BigDecimal.ONE)
                .setBatchCode("STAGE4-BATCH-" + safe)
                .setParentId(MesProWorkOrderDO.PARENT_ID_NULL)
                .setStatus(MesProWorkOrderStatusEnum.CONFIRMED.getStatus())
                .setRemark(marker);
        workOrder.setTenantId(tenantId);
        if (workOrderMapper.insert(workOrder) != 1 || workOrder.getId() == null) {
            throw new IllegalStateException("STAGE4_INDEPENDENT_WORK_ORDER_CREATE_FAILED");
        }

        MesProcessPoolActiveOrderDO activeOrder = new MesProcessPoolActiveOrderDO()
                .setLeaderUserId(actorUserId)
                .setWorkOrderId(workOrder.getId())
                .setRouteId(routeId)
                .setRouteVersionId(routeId + 1)
                .setErpFixedQuantitySnapshot(BigDecimal.ONE)
                .setActiveStatus("ACTIVE")
                .setBusinessStatus("COMPLETED")
                .setJoinedAt(now)
                .setSortOrder(workOrder.getId())
                .setSimulated(true)
                .setSimulationStage(INPUT_MODE_INDEPENDENT_BATCH_EXECUTION)
                .setSimulationRunId(simulationRunId)
                .setVersion(1);
        activeOrder.setTenantId(tenantId);
        if (activeOrderMapper.insert(activeOrder) != 1 || activeOrder.getId() == null) {
            throw new IllegalStateException("STAGE4_INDEPENDENT_ACTIVE_ORDER_CREATE_FAILED");
        }

        String pickListSourceFid = "STAGE4-" + safe + "-PICK";
        String pickListSourceBillNo = "STAGE4-PL-" + safe;
        ErpKingdeeProductionPickListDO pickList = new ErpKingdeeProductionPickListDO()
                .setSourceFormId("STAGE4_SIMULATION")
                .setSourceFid(pickListSourceFid)
                .setSourceBillNo(pickListSourceBillNo)
                .setBillDate(now)
                .setDocumentStatus("C")
                .setDescription("Stage4 independent simulated pick list")
                .setSourceModifyTime(now)
                .setLastSyncTime(now)
                .setRawPayload(JSON.toJSONString(Map.of(
                        "simulationRunId", simulationRunId,
                        "inputMode", INPUT_MODE_INDEPENDENT_BATCH_EXECUTION,
                        "isSimulation", true)));
        pickList.setTenantId(tenantId);
        if (pickListMapper.insert(pickList) != 1 || pickList.getId() == null) {
            throw new IllegalStateException("STAGE4_INDEPENDENT_PICK_LIST_CREATE_FAILED");
        }

        ErpKingdeeProductionPickListItemDO pickListItem = new ErpKingdeeProductionPickListItemDO()
                .setProductionPickListId(pickList.getId())
                .setSourceFormId("STAGE4_SIMULATION")
                .setSourceFid(pickListSourceFid + "-LINE")
                .setSourceEntryId("STAGE4-" + safe + "-E1")
                .setSourceLineKey("STAGE4-" + safe + "-LINE-1")
                .setSourceBillNo(pickListSourceBillNo)
                .setMaterialNumber("STAGE4-SIM-MATERIAL")
                .setMaterialName("Stage4 simulated material")
                .setMaterialSpecification("Independent fixture material")
                .setUnitName("件")
                .setRequestedQuantity(BigDecimal.ONE)
                .setActualQuantity(BigDecimal.ONE)
                .setBaseActualQuantity(BigDecimal.ONE)
                .setProductionOrderNo(workOrder.getCode())
                .setProductionOrderLineNo(1)
                .setSourceModifyTime(now)
                .setLastSyncTime(now)
                .setRawPayload(JSON.toJSONString(Map.of(
                        "simulationRunId", simulationRunId,
                        "quantity", 1,
                        "isSimulation", true)));
        pickListItem.setTenantId(tenantId);
        if (pickListItemMapper.insert(pickListItem) != 1 || pickListItem.getId() == null) {
            throw new IllegalStateException("STAGE4_INDEPENDENT_PICK_LIST_ITEM_CREATE_FAILED");
        }

        String pickListSnapshotHash = hash(Map.of(
                "pickListId", pickList.getId(),
                "pickListSourceFid", pickList.getSourceFid(),
                "pickListSourceBillNo", pickList.getSourceBillNo(),
                "pickListItemId", pickListItem.getId(),
                "pickListItemSourceEntryId", pickListItem.getSourceEntryId()));
        MesProcessPoolActiveOrderPickListBindingDO binding = new MesProcessPoolActiveOrderPickListBindingDO()
                .setId(IdUtil.getSnowflake().nextId())
                .setActiveOrderId(activeOrder.getId())
                .setWorkOrderId(workOrder.getId())
                .setPickListId(pickList.getId())
                .setSourceFid(pickList.getSourceFid())
                .setSourceBillNo(pickList.getSourceBillNo())
                .setSourceDocumentStatus(pickList.getDocumentStatus())
                .setSourceModifyTime(now)
                .setSourceSnapshotHash(pickListSnapshotHash)
                .setBindingStatus("BOUND")
                .setBoundBy(actorUserId)
                .setBoundAt(now)
                .setIdempotencyKey("STAGE4-INDEPENDENT-PICK-BINDING-" + simulationRunId)
                .setRequestPayloadHash(hash(marker + ":PICK_BINDING"))
                .setBindingVersion(1)
                .setSimulated(true)
                .setSimulationStage(INPUT_MODE_INDEPENDENT_BATCH_EXECUTION)
                .setSimulationRunId(simulationRunId);
        binding.setTenantId(tenantId);
        if (pickListBindingMapper.insert(binding) != 1 || binding.getId() == null) {
            throw new IllegalStateException("STAGE4_INDEPENDENT_PICK_LIST_BINDING_CREATE_FAILED");
        }
        MesProcessPoolActiveOrderPickListBindingItemDO bindingItem = MesProcessPoolActiveOrderPickListBindingItemDO.builder()
                .id(IdUtil.getSnowflake().nextId())
                .bindingId(binding.getId())
                .pickListItemId(pickListItem.getId())
                .sourceEntryId(pickListItem.getSourceEntryId())
                .sourceLineKey(pickListItem.getSourceLineKey())
                .materialNumber(pickListItem.getMaterialNumber())
                .materialName(pickListItem.getMaterialName())
                .materialSpecification(pickListItem.getMaterialSpecification())
                .unitName(pickListItem.getUnitName())
                .requestedQuantity(pickListItem.getRequestedQuantity())
                .actualQuantity(pickListItem.getActualQuantity())
                .baseActualQuantity(pickListItem.getBaseActualQuantity())
                .productionOrderNo(workOrder.getCode())
                .productionOrderLineNo(1)
                .sourceModifyTime(now)
                .itemSnapshotHash(hash(String.valueOf(pickListItem.getId())))
                .simulated(true)
                .simulationStage(INPUT_MODE_INDEPENDENT_BATCH_EXECUTION)
                .simulationRunId(simulationRunId)
                .build();
        bindingItem.setTenantId(tenantId);
        if (pickListBindingItemMapper.insert(bindingItem) != 1 || bindingItem.getId() == null) {
            throw new IllegalStateException("STAGE4_INDEPENDENT_PICK_LIST_BINDING_ITEM_CREATE_FAILED");
        }

        MesProEdhrBatchExecutionDO batch = new MesProEdhrBatchExecutionDO()
                .setBatchExecutionCode("STAGE4-SIM-" + safe)
                .setWorkOrderId(workOrder.getId())
                .setWorkOrderCode(workOrder.getCode())
                .setBatchCode(workOrder.getBatchCode())
                .setActiveContextKey(marker)
                .setAttemptNo(1)
                .setProductId(workOrder.getProductId())
                .setProductCode("STAGE4-SIM-PRODUCT")
                .setProductName("Stage4 independent batch execution fixture")
                .setRouteId(routeId)
                .setRouteVersionId(routeId + 1)
                .setRouteVersionNo("STAGE4-V1")
                .setRouteCode("STAGE4-SIM-ROUTE")
                .setRouteName("Stage4 independent batch execution fixture route")
                .setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_IN_PROGRESS)
                .setProvisioningStatus("BATCH_READY")
                .setTaskTotal(NODES.size())
                .setTaskApprovedCount(0)
                .setBlockedCount(0)
                .setRemark(marker)
                .setRouteSnapshotJson(routeSnapshotJson);
        batch.setTenantId(tenantId);
        if (batchExecutionMapper.insert(batch) != 1 || batch.getId() == null) {
            throw new IllegalStateException("STAGE4_INDEPENDENT_BATCH_CREATE_FAILED");
        }

        String routeBindingSnapshotHash = hash(routeSnapshotJson);
        Map<String, MesProEdhrBatchExecutionTaskDO> tasks = new LinkedHashMap<>();
        for (NodeDefinition node : NODES) {
            MesProEdhrBatchExecutionTaskDO task = new MesProEdhrBatchExecutionTaskDO()
                    .setBatchExecutionId(batch.getId())
                    .setNodeType(node.nodeType())
                    .setRouteProcessId(routeId + node.sort())
                    .setRouteProcessSort(node.sort())
                    .setProcessId(routeId + 500 + node.sort())
                    .setProcessCode("STAGE4-" + node.nodeType())
                    .setProcessName(node.nodeName())
                    .setBatchRecordReportId("STAGE4-" + node.nodeType())
                    .setBatchRecordReportName(node.nodeName())
                    .setBatchRecordSort(1)
                    .setInstanceScope("BATCH_SHARED")
                    .setExecutionMode("SEQUENTIAL")
                    .setRecordCategory("INTERNAL_RECORD")
                    .setValidationProfile("CONTROLLED_BATCH")
                    .setRecordbookEnabled(false)
                    .setRouteBindingSnapshotHash(routeBindingSnapshotHash)
                    .setRequiredPolicy("REQUIRED")
                    .setOwnerRoleKey("QUALITY")
                    .setArchiveVisibility("FINAL_DHR")
                    .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING)
                    .setRequiredFlag(true)
                    .setSpecialPayloadJson(JSON.toJSONString(Map.of(
                            "stageCode", "STAGE4_DOSSIER_UPLOAD",
                            "inputMode", INPUT_MODE_INDEPENDENT_BATCH_EXECUTION,
                            "simulationRunId", simulationRunId,
                            "isSimulation", true,
                            "status", "PENDING_UPLOAD")));
            if (batchTaskMapper.insert(task) != 1 || task.getId() == null) {
                throw new IllegalStateException("STAGE4_INDEPENDENT_BATCH_TASK_CREATE_FAILED:" + node.nodeType());
            }
            tasks.put(node.nodeType(), task);
        }

        List<Long> taskIds = NODES.stream().map(node -> tasks.get(node.nodeType()).getId()).toList();
        List<Long> batchSourceIds = List.of(taskIds.get(0), pickList.getId(), pickListItem.getId());
        List<Long> processInspectionSourceIds = List.of(taskIds.get(1), taskIds.get(2), taskIds.get(3));
        Map<String, Object> formalSource = new LinkedHashMap<>();
        formalSource.put("schemaVersion", "stage4IndependentFormalSource.v1");
        formalSource.put("simulationRunId", simulationRunId);
        formalSource.put("inputMode", INPUT_MODE_INDEPENDENT_BATCH_EXECUTION);
        formalSource.put("workOrderId", workOrder.getId());
        formalSource.put("activeOrderId", activeOrder.getId());
        formalSource.put("batchExecutionId", batch.getId());
        formalSource.put("routeId", routeId);
        formalSource.put("routeVersionId", routeId + 1);
        formalSource.put("productionSourceIds", batchSourceIds);
        formalSource.put("processInspectionSourceIds", processInspectionSourceIds);
        formalSource.put("pickListId", pickList.getId());
        formalSource.put("pickListItemId", pickListItem.getId());
        formalSource.put("pickListBindingId", binding.getId());
        formalSource.put("pickListBindingItemId", bindingItem.getId());
        formalSource.put("routeBindingSnapshotHash", routeBindingSnapshotHash);
        String sourceSnapshotHash = canonicalHash(routeSnapshotJson);
        formalSource.put("sourceSnapshotHash", sourceSnapshotHash);
        tasks.values().forEach(task -> {
            task.setMaterialSourceSnapshotHash(sourceSnapshotHash);
            if (batchTaskMapper.updateById(task) != 1) {
                throw new IllegalStateException("STAGE4_INDEPENDENT_BATCH_TASK_SOURCE_UPDATE_FAILED:" + task.getNodeType());
            }
        });

        String batchSourceIdsJson = JSON.toJSONString(batchSourceIds);
        String processInspectionSourceIdsJson = JSON.toJSONString(processInspectionSourceIds);
        String lossFacts = JSON.toJSONString(List.of(Map.of(
                "processId", taskIds.get(0),
                "status", "NO_LOSS",
                "hasActualLoss", false,
                "lossQuantity", 0,
                "zeroLossConfirmationSnapshot", marker + ":NO_LOSS",
                "sourceHash", hash(marker + ":NO_LOSS"))));
        MesProcessPoolActiveOrderCompletionReceiptDO receipt = new MesProcessPoolActiveOrderCompletionReceiptDO()
                .setActiveOrderId(activeOrder.getId())
                .setWorkOrderId(workOrder.getId())
                .setBatchCode(workOrder.getBatchCode())
                .setRouteId(routeId)
                .setRouteVersionId(routeId + 1)
                .setLeaderUserId(actorUserId)
                .setRequestIdempotencyKey("STAGE4-INDEPENDENT-COMPLETION-" + simulationRunId)
                .setRequestPayloadHash(hash(marker + ":COMPLETION_REQUEST"))
                .setSourceSnapshotHash(sourceSnapshotHash)
                .setFormalSourceSnapshotJson(JSON.toJSONString(formalSource))
                .setSignatureSnapshotJson(JSON.toJSONString(Map.of(
                        "simulationRunId", simulationRunId,
                        "productionSubmit", true,
                        "productionLeaderReview", true,
                        "pqcSubmit", true,
                        "pqcLeaderConfirmation", true)))
                .setExpectedVersion(1)
                .setCompletedVersion(1)
                .setReceiptStatus(MesProcessPoolActiveOrderCompletionReceiptDO.RECEIPT_STATUS_BACKFILL_SUCCEEDED)
                .setCompletionStatus(MesProcessPoolActiveOrderCompletionReceiptDO.STATUS_SUCCESS)
                .setBatchRecordStatus(MesProcessPoolActiveOrderCompletionReceiptDO.BACKFILL_STATUS_SUCCESS)
                .setProcessInspectionStatus(MesProcessPoolActiveOrderCompletionReceiptDO.BACKFILL_STATUS_SUCCESS)
                .setLossReportStatus(MesProcessPoolActiveOrderCompletionReceiptDO.LOSS_REPORT_STATUS_NOT_REQUIRED)
                .setHasActualLoss(false)
                .setLossQuantity(BigDecimal.ZERO)
                .setZeroLossConfirmationSnapshot(JSON.toJSONString(Map.of(
                        "simulationRunId", simulationRunId, "status", "NO_LOSS")))
                .setLossConditionFactsJson(lossFacts)
                .setBatchRecordSourceIdsJson(batchSourceIdsJson)
                .setProcessInspectionSourceIdsJson(processInspectionSourceIdsJson)
                .setLossSourceHash(hash(lossFacts))
                .setProvisionHandoff(MesProcessPoolActiveOrderCompletionReceiptDO.PROVISION_HANDOFF_PENDING_FLOW6)
                .setCompletedAt(now)
                .setCompletedBy(actorUserId);
        receipt.setTenantId(tenantId);
        receipt.setCreateTime(now);

        MesProcessPoolActiveOrderCompletionBackfillDO batchRecordBackfill = createIndependentBackfill(
                activeOrder, workOrder, MesProcessPoolActiveOrderCompletionBackfillDO.TYPE_BATCH_RECORD,
                batchSourceIdsJson, sourceSnapshotHash,
                JSON.toJSONString(Map.of("simulationRunId", simulationRunId, "source", "FORMAL_BATCH_RECORD")),
                actorUserId, now);
        MesProcessPoolActiveOrderCompletionBackfillDO processInspectionBackfill = createIndependentBackfill(
                activeOrder, workOrder, MesProcessPoolActiveOrderCompletionBackfillDO.TYPE_PROCESS_INSPECTION,
                processInspectionSourceIdsJson, sourceSnapshotHash,
                JSON.toJSONString(Map.of("simulationRunId", simulationRunId, "source", "FORMAL_PROCESS_INSPECTION")),
                actorUserId, now);
        MesProcessPoolActiveOrderCompletionBackfillDO lossReportBackfill = createIndependentBackfill(
                activeOrder, workOrder, MesProcessPoolActiveOrderCompletionBackfillDO.TYPE_LOSS_REPORT,
                "[]", hash(lossFacts), lossFacts, actorUserId, now);
        receipt.setBatchRecordId(batchRecordBackfill.getId())
                .setProcessInspectionId(processInspectionBackfill.getId())
                .setBatchRecordSourceIdsJson(JSON.toJSONString(List.of(batchRecordBackfill.getId())))
                .setProcessInspectionSourceIdsJson(JSON.toJSONString(List.of(processInspectionBackfill.getId())));
        receipt.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt));
        if (completionReceiptMapper.insert(receipt) != 1 || receipt.getId() == null) {
            throw new IllegalStateException("STAGE4_INDEPENDENT_COMPLETION_RECEIPT_CREATE_FAILED");
        }
        MesProcessPoolActiveOrderCompletionReceiptDO persistedReceipt = completionReceiptMapper.selectById(receipt.getId());
        if (persistedReceipt == null) {
            throw new IllegalStateException("STAGE4_INDEPENDENT_COMPLETION_RECEIPT_NOT_PERSISTED");
        }
        persistedReceipt.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(persistedReceipt));
        if (completionReceiptMapper.updateById(persistedReceipt) != 1) {
            throw new IllegalStateException("STAGE4_INDEPENDENT_COMPLETION_RECEIPT_SEAL_FAILED");
        }
        receipt = persistedReceipt;

        MesProEdhrBatchProvisioningRecordDO provisioningRecord = new MesProEdhrBatchProvisioningRecordDO()
                .setTenantId(tenantId)
                .setBatchExecutionId(batch.getId())
                .setEntryType("ACTIVE_ORDER_COMPLETION")
                .setEntryBusinessId(String.valueOf(activeOrder.getId()))
                .setSourceCredentialId(String.valueOf(persistedReceipt.getId()))
                .setSourceCredentialHash(persistedReceipt.getReceiptHash())
                .setSourceSnapshotHash(sourceSnapshotHash)
                .setSourceVersion("stage4-independent-v1")
                .setIdempotencyKey("STAGE4-INDEPENDENT-PROVISION-" + simulationRunId)
                .setStatus("BATCH_READY")
                .setAttemptCount(1);
        FormalStage4Source source = new FormalStage4Source(provisioningRecord, receipt, binding);
        Map<String, Object> inputSnapshot = buildInputSnapshot(batch, tasks, simulationRunId, source);
        inputSnapshot.put("inputMode", INPUT_MODE_INDEPENDENT_BATCH_EXECUTION);
        inputSnapshot.put("sourceInputContract", COMPLETE_BATCH_EXECUTION_SCHEMA);
        Map<String, Object> completeSnapshot = buildCompleteBatchExecutionSnapshot(
                simulationRunId, workOrder, activeOrder, pickList, pickListItem, binding, bindingItem,
                batch, tasks, receipt, formalSource, batchRecordBackfill, processInspectionBackfill,
                lossReportBackfill, inputSnapshot, sourceSnapshotHash);
        MesProEdhrBatchTraceabilityValidator traceValidator = new MesProEdhrBatchTraceabilityValidator();
        List<MesProEdhrBatchTraceSource> traceSources = buildIndependentTraceSources(
                simulationRunId, workOrder, activeOrder, pickList, pickListItem, binding, batch, tasks,
                receipt, batchRecordBackfill, processInspectionBackfill, sourceSnapshotHash);
        String traceSourceBundleHash = traceValidator.calculateSourceBundleHash(traceSources);
        completeSnapshot.put("traceability", Map.of(
                "entryType", MesProEdhrBatchTraceEntryType.ACTIVE_ORDER_COMPLETION,
                "originKey", "STAGE4:" + safe + ":ACTIVE_ORDER_COMPLETION",
                "batchProvisionReceiptId", String.valueOf(batch.getId()),
                "sourceSnapshotHash", sourceSnapshotHash,
                "sourceBundleHash", traceSourceBundleHash,
                "sourceTypes", traceSources.stream().map(MesProEdhrBatchTraceSource::getLinkType).toList()));
        String sourceBundleHash = hash(completeSnapshot);
        completeSnapshot.put("sourceBundleHash", sourceBundleHash);
        provisioningRecord.setSourceBundleHash(sourceBundleHash);
        provisioningRecord.setCreateTime(now);
        if (provisioningRecordMapper.insert(provisioningRecord) != 1 || provisioningRecord.getId() == null) {
            throw new IllegalStateException("STAGE4_INDEPENDENT_PROVISIONING_RECORD_CREATE_FAILED");
        }

        MesProEdhrBatchTraceCaptureCommand traceCommand = new MesProEdhrBatchTraceCaptureCommand()
                .setBatchExecutionId(batch.getId())
                .setEntryType(MesProEdhrBatchTraceEntryType.ACTIVE_ORDER_COMPLETION)
                .setOriginKey("STAGE4:" + safe + ":ACTIVE_ORDER_COMPLETION")
                .setActiveOrderId(activeOrder.getId())
                .setWorkOrderId(workOrder.getId())
                .setCompletionTransactionId(String.valueOf(receipt.getId()))
                .setCompletionVersion(receipt.getCompletedVersion())
                .setCompletionBackfillReceiptId(receipt.getId())
                .setCompletionBackfillReceiptHash(receipt.getReceiptHash())
                .setPickListBindingId(binding.getId())
                .setPickListId(pickList.getId())
                .setPickListBindingVersion(binding.getBindingVersion())
                .setSourceSnapshotHash(sourceSnapshotHash)
                .setBatchProvisionReceiptId(batch.getId())
                .setBatchProvisionStatus(MesProEdhrBatchTraceProvisionStatus.CREATED)
                .setSourceBundleHash(traceSourceBundleHash)
                .setIdempotencyKey("STAGE4-INDEPENDENT-TRACE-" + simulationRunId)
                .setHasActualLoss(false)
                .setCapturedBy(actorUserId)
                .setSources(traceSources);
        MesProEdhrBatchTraceValidationResult traceValidation = traceValidator.validate(traceCommand);
        if (!traceValidation.valid()) {
            throw new IllegalStateException("STAGE4_INDEPENDENT_TRACE_SOURCE_INVALID:"
                    + traceValidation.blockerCode() + ":" + traceValidation.blockerScope());
        }
        traceabilityService.capture(traceCommand);
        batch.setAggregateHash(hash(inputSnapshot));
        if (batchExecutionMapper.updateById(batch) != 1) {
            throw new IllegalStateException("STAGE4_INDEPENDENT_BATCH_UPDATE_FAILED");
        }
        return new IndependentBatchExecutionInputFixture(batch, source, completeSnapshot);
    }

    private List<MesProEdhrBatchTraceSource> buildIndependentTraceSources(
            String simulationRunId,
            MesProWorkOrderDO workOrder,
            MesProcessPoolActiveOrderDO activeOrder,
            ErpKingdeeProductionPickListDO pickList,
            ErpKingdeeProductionPickListItemDO pickListItem,
            MesProcessPoolActiveOrderPickListBindingDO binding,
            MesProEdhrBatchExecutionDO batch,
            Map<String, MesProEdhrBatchExecutionTaskDO> tasks,
            MesProcessPoolActiveOrderCompletionReceiptDO receipt,
            MesProcessPoolActiveOrderCompletionBackfillDO batchRecordBackfill,
            MesProcessPoolActiveOrderCompletionBackfillDO processInspectionBackfill,
            String sourceSnapshotHash) {
        List<MesProEdhrBatchTraceSource> sources = new ArrayList<>();
        sources.add(traceSource(MesProEdhrBatchTraceLinkType.ACTIVE_ORDER, "ACTIVE_ORDER", activeOrder.getId(),
                sourceSnapshotHash, JSON.toJSONString(Map.of(
                        "simulationRunId", simulationRunId, "activeOrderId", activeOrder.getId())), "CAPTURED"));
        sources.add(traceSource(MesProEdhrBatchTraceLinkType.WORK_ORDER, "WORK_ORDER", workOrder.getId(),
                sourceSnapshotHash, JSON.toJSONString(Map.of(
                        "simulationRunId", simulationRunId, "workOrderId", workOrder.getId())), "CAPTURED"));
        sources.add(traceSource(MesProEdhrBatchTraceLinkType.MATERIAL_ISSUE, "PICK_LIST", pickList.getId(),
                sourceSnapshotHash, batch.getRouteSnapshotJson(), "CAPTURED"));
        sources.add(traceSource(MesProEdhrBatchTraceLinkType.MATERIAL_ISSUE_LINE, "PICK_LIST_LINE",
                pickListItem.getId(), sourceSnapshotHash, batch.getRouteSnapshotJson(), "CAPTURED"));
        sources.add(traceSource(MesProEdhrBatchTraceLinkType.PRODUCTION_SUBMIT, "BATCH_TASK",
                tasks.get("INCOMING_INSPECTION_REPORT").getId(), sourceSnapshotHash,
                JSON.toJSONString(Map.of("simulationRunId", simulationRunId, "action", "PRODUCTION_SUBMIT")),
                "CAPTURED"));
        sources.add(traceSource(MesProEdhrBatchTraceLinkType.PRODUCTION_SIGNATURE, "BATCH_TASK",
                tasks.get("INCOMING_INSPECTION_REPORT").getId(), sourceSnapshotHash,
                JSON.toJSONString(Map.of("simulationRunId", simulationRunId, "action", "PRODUCTION_SIGNATURE")),
                "CAPTURED"));
        sources.add(traceSource(MesProEdhrBatchTraceLinkType.PRODUCTION_LEADER_REVIEW, "BATCH_TASK",
                tasks.get("INCOMING_INSPECTION_REPORT").getId(), sourceSnapshotHash,
                JSON.toJSONString(Map.of("simulationRunId", simulationRunId, "action", "PRODUCTION_LEADER_REVIEW")),
                "CAPTURED"));
        sources.add(traceSource(MesProEdhrBatchTraceLinkType.PQC_TASK, "BATCH_TASK",
                tasks.get("STERILIZATION_REPORT").getId(), sourceSnapshotHash,
                JSON.toJSONString(Map.of("simulationRunId", simulationRunId, "action", "PQC_TASK")), "CAPTURED"));
        sources.add(traceSource(MesProEdhrBatchTraceLinkType.PQC_SUBMISSION, "BATCH_TASK",
                tasks.get("STERILIZATION_REPORT").getId(), sourceSnapshotHash,
                JSON.toJSONString(Map.of("simulationRunId", simulationRunId, "action", "PQC_SUBMISSION")),
                "CAPTURED"));
        sources.add(traceSource(MesProEdhrBatchTraceLinkType.PQC_SIGNATURE, "BATCH_TASK",
                tasks.get("STERILIZATION_REPORT").getId(), sourceSnapshotHash,
                JSON.toJSONString(Map.of("simulationRunId", simulationRunId, "action", "PQC_SIGNATURE")),
                "CAPTURED"));
        sources.add(traceSource(MesProEdhrBatchTraceLinkType.PQC_LEADER_CONFIRMATION, "BATCH_TASK",
                tasks.get("STERILIZATION_REPORT").getId(), sourceSnapshotHash,
                JSON.toJSONString(Map.of("simulationRunId", simulationRunId,
                        "action", "PQC_LEADER_CONFIRMATION")), "CAPTURED"));
        sources.add(traceSource(MesProEdhrBatchTraceLinkType.PQC_AGGREGATE_DETAIL, "BATCH_TASK",
                tasks.get("FINISHED_PRODUCT_INSPECTION_REPORT").getId(), sourceSnapshotHash,
                JSON.toJSONString(Map.of("simulationRunId", simulationRunId,
                        "action", "PQC_AGGREGATE_DETAIL")), "CAPTURED"));
        sources.add(traceSource(MesProEdhrBatchTraceLinkType.BATCH_RECORD_RECEIPT, "BACKFILL",
                batchRecordBackfill.getId(), sourceSnapshotHash,
                JSON.toJSONString(Map.of("simulationRunId", simulationRunId, "backfill", "BATCH_RECORD")),
                "CAPTURED"));
        sources.add(traceSource(MesProEdhrBatchTraceLinkType.PROCESS_INSPECTION_RECEIPT, "BACKFILL",
                processInspectionBackfill.getId(), sourceSnapshotHash,
                JSON.toJSONString(Map.of("simulationRunId", simulationRunId,
                        "backfill", "PROCESS_INSPECTION")), "CAPTURED"));
        sources.add(traceSource(MesProEdhrBatchTraceLinkType.COMPLETION_BACKFILL_RECEIPT,
                "COMPLETION_RECEIPT", receipt.getId(), receipt.getReceiptHash(),
                MesTeamLeaderActiveOrderCompletionReceiptHash.snapshotJson(receipt), "CAPTURED"));
        sources.add(traceSource(MesProEdhrBatchTraceLinkType.BATCH_PROVISION_RECEIPT, "BATCH_EXECUTION",
                batch.getId(), sourceSnapshotHash, batch.getRouteSnapshotJson(), "CAPTURED"));
        sources.add(traceSource(MesProEdhrBatchTraceLinkType.NO_LOSS_CONFIRMED, "COMPLETION_RECEIPT",
                receipt.getId(), hash(receipt.getLossConditionFactsJson()), receipt.getLossConditionFactsJson(),
                "NO_LOSS"));
        return sources;
    }

    private MesProEdhrBatchTraceSource traceSource(String linkType, String objectType, Long objectId,
                                                   String expectedSnapshotHash, String snapshotJson,
                                                   String relationStatus) {
        String calculatedHash = MesProEdhrBatchTraceSourceHash.calculate(linkType, snapshotJson);
        if (!Objects.equals(calculatedHash, expectedSnapshotHash)
                && (MesProEdhrBatchTraceLinkType.MATERIAL_ISSUE.equals(linkType)
                || MesProEdhrBatchTraceLinkType.MATERIAL_ISSUE_LINE.equals(linkType)
                || MesProEdhrBatchTraceLinkType.BATCH_PROVISION_RECEIPT.equals(linkType)
                || MesProEdhrBatchTraceLinkType.COMPLETION_BACKFILL_RECEIPT.equals(linkType))) {
            throw new IllegalStateException("STAGE4_TRACE_SOURCE_SNAPSHOT_HASH_INVALID:" + linkType);
        }
        return new MesProEdhrBatchTraceSource()
                .setLinkType(linkType)
                .setSourceObjectType(objectType)
                .setSourceObjectId(objectId)
                .setSourceVersion(1)
                .setSnapshotJson(snapshotJson)
                .setSnapshotHash(calculatedHash)
                .setRelationStatus(relationStatus)
                .setRelationReason("STAGE4_INDEPENDENT_FORMAL_FIXTURE");
    }

    private MesProcessPoolActiveOrderCompletionBackfillDO createIndependentBackfill(
            MesProcessPoolActiveOrderDO activeOrder, MesProWorkOrderDO workOrder, String type,
            String sourceIds, String sourceHash, String payload, Long actorUserId, LocalDateTime materializedAt) {
        MesProcessPoolActiveOrderCompletionBackfillDO backfill = MesProcessPoolActiveOrderCompletionBackfillDO.builder()
                .activeOrderId(activeOrder.getId())
                .workOrderId(workOrder.getId())
                .backfillType(type)
                .status("SUCCESS")
                .sourceIdsJson(sourceIds)
                .sourceSnapshotHash(sourceHash)
                .payloadJson(payload)
                .materializedAt(materializedAt)
                .materializedBy(actorUserId)
                .build();
        backfill.setTenantId(TenantContextHolder.getRequiredTenantId());
        if (completionBackfillMapper.insert(backfill) != 1 || backfill.getId() == null) {
            throw new IllegalStateException("STAGE4_INDEPENDENT_COMPLETION_BACKFILL_CREATE_FAILED:" + type);
        }
        return backfill;
    }

    private Map<String, MesProEdhrBatchExecutionTaskDO> loadFixtureTasks(Long batchExecutionId) {
        Map<String, MesProEdhrBatchExecutionTaskDO> tasks = new LinkedHashMap<>();
        for (MesProEdhrBatchExecutionTaskDO task : batchTaskMapper.selectListByBatchExecutionId(batchExecutionId)) {
            if (MesStage4DossierUploadSimulationContractValidator.REQUIRED_NODE_TYPES.contains(task.getNodeType())) {
                tasks.put(task.getNodeType(), task);
            }
        }
        if (!Objects.equals(tasks.keySet(), MesStage4DossierUploadSimulationContractValidator.REQUIRED_NODE_TYPES)) {
            throw new IllegalStateException("STAGE4_FIXTURE_NODE_CONTRACT_INVALID");
        }
        return tasks;
    }

    private Map<String, Object> buildCompleteBatchExecutionSnapshot(
            String simulationRunId,
            MesProWorkOrderDO workOrder,
            MesProcessPoolActiveOrderDO activeOrder,
            ErpKingdeeProductionPickListDO pickList,
            ErpKingdeeProductionPickListItemDO pickListItem,
            MesProcessPoolActiveOrderPickListBindingDO binding,
            MesProcessPoolActiveOrderPickListBindingItemDO bindingItem,
            MesProEdhrBatchExecutionDO batch,
            Map<String, MesProEdhrBatchExecutionTaskDO> tasks,
            MesProcessPoolActiveOrderCompletionReceiptDO receipt,
            Map<String, Object> formalSource,
            MesProcessPoolActiveOrderCompletionBackfillDO batchRecordBackfill,
            MesProcessPoolActiveOrderCompletionBackfillDO processInspectionBackfill,
            MesProcessPoolActiveOrderCompletionBackfillDO lossReportBackfill,
            Map<String, Object> stage4InputProjection,
            String sourceSnapshotHash) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("schemaVersion", COMPLETE_BATCH_EXECUTION_SCHEMA);
        snapshot.put("simulationRunId", simulationRunId);
        snapshot.put("stageCode", INPUT_MODE_INDEPENDENT_BATCH_EXECUTION);
        snapshot.put("isSimulation", true);
        snapshot.put("sourceSnapshotHash", sourceSnapshotHash);
        snapshot.put("batchExecutionId", String.valueOf(batch.getId()));

        Map<String, Object> workOrderSnapshot = new LinkedHashMap<>();
        workOrderSnapshot.put("id", String.valueOf(workOrder.getId()));
        workOrderSnapshot.put("code", workOrder.getCode());
        workOrderSnapshot.put("name", workOrder.getName());
        workOrderSnapshot.put("productId", String.valueOf(workOrder.getProductId()));
        workOrderSnapshot.put("quantity", workOrder.getQuantity());
        workOrderSnapshot.put("quantityProduced", workOrder.getQuantityProduced());
        workOrderSnapshot.put("batchCode", workOrder.getBatchCode());
        workOrderSnapshot.put("status", workOrder.getStatus());
        snapshot.put("workOrder", workOrderSnapshot);

        Map<String, Object> activeOrderSnapshot = new LinkedHashMap<>();
        activeOrderSnapshot.put("id", String.valueOf(activeOrder.getId()));
        activeOrderSnapshot.put("workOrderId", String.valueOf(activeOrder.getWorkOrderId()));
        activeOrderSnapshot.put("leaderUserId", String.valueOf(activeOrder.getLeaderUserId()));
        activeOrderSnapshot.put("routeId", String.valueOf(activeOrder.getRouteId()));
        activeOrderSnapshot.put("routeVersionId", String.valueOf(activeOrder.getRouteVersionId()));
        activeOrderSnapshot.put("activeStatus", activeOrder.getActiveStatus());
        activeOrderSnapshot.put("businessStatus", activeOrder.getBusinessStatus());
        activeOrderSnapshot.put("simulated", activeOrder.getSimulated());
        activeOrderSnapshot.put("simulationStage", activeOrder.getSimulationStage());
        activeOrderSnapshot.put("simulationRunId", activeOrder.getSimulationRunId());
        snapshot.put("activeOrder", activeOrderSnapshot);

        Map<String, Object> materialIssueSnapshot = new LinkedHashMap<>();
        materialIssueSnapshot.put("pickListId", String.valueOf(pickList.getId()));
        materialIssueSnapshot.put("pickListSourceFid", pickList.getSourceFid());
        materialIssueSnapshot.put("pickListSourceBillNo", pickList.getSourceBillNo());
        materialIssueSnapshot.put("pickListDocumentStatus", pickList.getDocumentStatus());
        materialIssueSnapshot.put("pickListItemId", String.valueOf(pickListItem.getId()));
        materialIssueSnapshot.put("materialNumber", pickListItem.getMaterialNumber());
        materialIssueSnapshot.put("materialName", pickListItem.getMaterialName());
        materialIssueSnapshot.put("requestedQuantity", pickListItem.getRequestedQuantity());
        materialIssueSnapshot.put("actualQuantity", pickListItem.getActualQuantity());
        materialIssueSnapshot.put("bindingId", String.valueOf(binding.getId()));
        materialIssueSnapshot.put("bindingItemId", String.valueOf(bindingItem.getId()));
        materialIssueSnapshot.put("bindingStatus", binding.getBindingStatus());
        materialIssueSnapshot.put("sourceSnapshotHash", binding.getSourceSnapshotHash());
        snapshot.put("materialIssue", materialIssueSnapshot);

        Map<String, Object> batchSnapshot = new LinkedHashMap<>();
        batchSnapshot.put("id", String.valueOf(batch.getId()));
        batchSnapshot.put("code", batch.getBatchExecutionCode());
        batchSnapshot.put("batchCode", batch.getBatchCode());
        batchSnapshot.put("workOrderId", String.valueOf(batch.getWorkOrderId()));
        batchSnapshot.put("routeId", String.valueOf(batch.getRouteId()));
        batchSnapshot.put("routeVersionId", String.valueOf(batch.getRouteVersionId()));
        batchSnapshot.put("routeVersionNo", batch.getRouteVersionNo());
        batchSnapshot.put("routeCode", batch.getRouteCode());
        batchSnapshot.put("routeName", batch.getRouteName());
        batchSnapshot.put("routeSnapshotJson", batch.getRouteSnapshotJson());
        batchSnapshot.put("status", batch.getStatus());
        batchSnapshot.put("provisioningStatus", batch.getProvisioningStatus());
        batchSnapshot.put("taskTotal", batch.getTaskTotal());
        snapshot.put("batchExecution", batchSnapshot);

        List<Map<String, Object>> taskSnapshots = new ArrayList<>();
        for (NodeDefinition node : NODES) {
            MesProEdhrBatchExecutionTaskDO task = tasks.get(node.nodeType());
            Map<String, Object> taskSnapshot = new LinkedHashMap<>();
            taskSnapshot.put("id", String.valueOf(task.getId()));
            taskSnapshot.put("nodeType", task.getNodeType());
            taskSnapshot.put("nodeName", node.nodeName());
            taskSnapshot.put("routeProcessId", String.valueOf(task.getRouteProcessId()));
            taskSnapshot.put("routeProcessSort", task.getRouteProcessSort());
            taskSnapshot.put("processId", String.valueOf(task.getProcessId()));
            taskSnapshot.put("status", task.getStatus());
            taskSnapshot.put("requiredFlag", task.getRequiredFlag());
            taskSnapshot.put("routeBindingSnapshotHash", task.getRouteBindingSnapshotHash());
            taskSnapshot.put("materialSourceSnapshotHash", task.getMaterialSourceSnapshotHash());
            taskSnapshot.put("specialPayloadJson", task.getSpecialPayloadJson());
            taskSnapshots.add(taskSnapshot);
        }
        snapshot.put("batchExecutionTasks", taskSnapshots);

        Map<String, Object> receiptSnapshot = new LinkedHashMap<>();
        receiptSnapshot.put("id", String.valueOf(receipt.getId()));
        receiptSnapshot.put("activeOrderId", String.valueOf(receipt.getActiveOrderId()));
        receiptSnapshot.put("workOrderId", String.valueOf(receipt.getWorkOrderId()));
        receiptSnapshot.put("receiptHash", receipt.getReceiptHash());
        receiptSnapshot.put("sourceSnapshotHash", receipt.getSourceSnapshotHash());
        receiptSnapshot.put("formalSourceSnapshotJson", receipt.getFormalSourceSnapshotJson());
        receiptSnapshot.put("signatureSnapshotJson", receipt.getSignatureSnapshotJson());
        receiptSnapshot.put("receiptStatus", receipt.getReceiptStatus());
        receiptSnapshot.put("completionStatus", receipt.getCompletionStatus());
        receiptSnapshot.put("batchRecordStatus", receipt.getBatchRecordStatus());
        receiptSnapshot.put("processInspectionStatus", receipt.getProcessInspectionStatus());
        receiptSnapshot.put("batchRecordId", String.valueOf(receipt.getBatchRecordId()));
        receiptSnapshot.put("processInspectionId", String.valueOf(receipt.getProcessInspectionId()));
        receiptSnapshot.put("lossReportStatus", receipt.getLossReportStatus());
        receiptSnapshot.put("hasActualLoss", receipt.getHasActualLoss());
        receiptSnapshot.put("lossQuantity", receipt.getLossQuantity());
        receiptSnapshot.put("batchRecordSourceIdsJson", receipt.getBatchRecordSourceIdsJson());
        receiptSnapshot.put("processInspectionSourceIdsJson", receipt.getProcessInspectionSourceIdsJson());
        receiptSnapshot.put("lossSourceHash", receipt.getLossSourceHash());
        receiptSnapshot.put("provisionHandoff", receipt.getProvisionHandoff());
        snapshot.put("completionReceipt", receiptSnapshot);

        snapshot.put("formalSourceSnapshot", formalSource);
        snapshot.put("completionBackfills", List.of(
                backfillSnapshot(batchRecordBackfill),
                backfillSnapshot(processInspectionBackfill),
                backfillSnapshot(lossReportBackfill)));
        snapshot.put("stage4InputProjection", stage4InputProjection);
        return snapshot;
    }

    private Map<String, Object> backfillSnapshot(MesProcessPoolActiveOrderCompletionBackfillDO backfill) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", String.valueOf(backfill.getId()));
        snapshot.put("activeOrderId", String.valueOf(backfill.getActiveOrderId()));
        snapshot.put("workOrderId", String.valueOf(backfill.getWorkOrderId()));
        snapshot.put("backfillType", backfill.getBackfillType());
        snapshot.put("status", backfill.getStatus());
        snapshot.put("sourceIdsJson", backfill.getSourceIdsJson());
        snapshot.put("sourceSnapshotHash", backfill.getSourceSnapshotHash());
        snapshot.put("payloadJson", backfill.getPayloadJson());
        return snapshot;
    }

    private Map<String, Object> buildStage4InputFromCompleteBatchExecution(
            Map<String, Object> completeBatchExecutionSnapshot,
            MesProEdhrBatchExecutionDO batch,
            Map<String, MesProEdhrBatchExecutionTaskDO> tasks,
            String simulationRunId,
            FormalStage4Source source) {
        if (!Objects.equals(COMPLETE_BATCH_EXECUTION_SCHEMA,
                String.valueOf(completeBatchExecutionSnapshot.get("schemaVersion")))
                || !Objects.equals(String.valueOf(batch.getId()),
                String.valueOf(completeBatchExecutionSnapshot.get("batchExecutionId")))) {
            throw new IllegalStateException("STAGE4_COMPLETE_BATCH_EXECUTION_INPUT_INVALID");
        }
        Object projectionObject = completeBatchExecutionSnapshot.get("stage4InputProjection");
        if (!(projectionObject instanceof Map<?, ?> projection)) {
            throw new IllegalStateException("STAGE4_COMPLETE_BATCH_EXECUTION_PROJECTION_REQUIRED");
        }
        Map<String, Object> inputSnapshot = new LinkedHashMap<>();
        projection.forEach((key, value) -> inputSnapshot.put(String.valueOf(key), value));
        inputSnapshot.put("inputMode", INPUT_MODE_INDEPENDENT_BATCH_EXECUTION);
        inputSnapshot.put("sourceInputContract", COMPLETE_BATCH_EXECUTION_SCHEMA);
        inputSnapshot.put("completeBatchExecutionSnapshotHash", hash(completeBatchExecutionSnapshot));
        inputSnapshot.put("simulationRunId", simulationRunId);
        inputSnapshot.put("batchExecutionId", String.valueOf(batch.getId()));
        inputSnapshot.put("sourceSnapshotHash", source.provisioningRecord().getSourceSnapshotHash());
        inputSnapshot.put("stage4TaskCount", tasks.size());
        return inputSnapshot;
    }

    private String buildRouteSnapshot(Long actorUserId, String simulationRunId) {
        List<Map<String, Object>> owners = NODES.stream()
                .map(node -> Map.<String, Object>of(
                        "attachmentCode", node.nodeType(),
                        "candidateSourceType", "USER",
                        "candidateSourceIds", List.of(actorUserId)))
                .toList();
        return JSON.toJSONString(Map.of(
                "schemaVersion", "stage4.routeSnapshot.v1",
                "stageCode", "STAGE4_DOSSIER_UPLOAD",
                "simulationRunId", simulationRunId,
                "isSimulation", true,
                "configSnapshots", Map.of("batchRecordAttachmentOwners", owners)));
    }

    private Map<String, Object> buildInputSnapshot(MesProEdhrBatchExecutionDO batch,
                                                    Map<String, MesProEdhrBatchExecutionTaskDO> tasks,
                                                    String simulationRunId,
                                                    FormalStage4Source source) {
        MesProcessPoolActiveOrderCompletionReceiptDO receipt = source.completionReceipt();
        MesProcessPoolActiveOrderPickListBindingDO binding = source.pickListBinding();
        String sourceSnapshotHash = source.provisioningRecord().getSourceSnapshotHash();
        List<Map<String, Object>> requiredNodes = NODES.stream()
                .map(node -> Map.<String, Object>of(
                        "nodeType", node.nodeType(),
                        "nodeName", node.nodeName(),
                        "batchExecutionTaskId", String.valueOf(tasks.get(node.nodeType()).getId()),
                        "status", "PENDING_UPLOAD",
                        "savedAttachmentCount", 0))
                .toList();
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("schemaVersion", MesStage4DossierUploadSimulationContractValidator.SCHEMA_VERSION);
        snapshot.put("simulationRunId", simulationRunId);
        snapshot.put("sourceInputContract", "backfillResultSnapshot.v1");
        snapshot.put("batchExecutionId", String.valueOf(batch.getId()));
        snapshot.put("batchExecutionCode", batch.getBatchExecutionCode());
        snapshot.put("batchCode", batch.getBatchCode());
        snapshot.put("activeContextKey", batch.getActiveContextKey());
        snapshot.put("activeOrderId", String.valueOf(receipt.getActiveOrderId()));
        snapshot.put("activeOrderCode", source.provisioningRecord().getEntryBusinessId());
        snapshot.put("workOrderId", String.valueOf(receipt.getWorkOrderId()));
        snapshot.put("workOrderCode", batch.getWorkOrderCode());
        snapshot.put("erpWorkOrderNo", batch.getWorkOrderCode());
        snapshot.put("routeId", String.valueOf(batch.getRouteId()));
        snapshot.put("routeVersionId", String.valueOf(batch.getRouteVersionId()));
        snapshot.put("routeVersionNo", batch.getRouteVersionNo());
        snapshot.put("materialIssueSource", Map.of(
                "pickListId", String.valueOf(binding.getPickListId()),
                "pickListBindingId", String.valueOf(binding.getId()),
                "sourceSnapshotHash", sourceSnapshotHash,
                "pickListSourceSnapshotHash", binding.getSourceSnapshotHash()));
        snapshot.put("batchRecordLinks", actualLinks(receipt.getBatchRecordSourceIdsJson(),
                "BATCH_RECORD", sourceSnapshotHash));
        snapshot.put("processInspectionLinks", actualLinks(receipt.getProcessInspectionSourceIdsJson(),
                "PROCESS_INSPECTION", sourceSnapshotHash));
        boolean hasLoss = Boolean.TRUE.equals(receipt.getHasActualLoss());
        snapshot.put("hasLoss", hasLoss);
        snapshot.put("lossReportRequirement", Map.of("required", hasLoss,
                "status", hasLoss ? "COMPLETED" : "NOT_REQUIRED",
                "sourceHash", hasLoss ? receipt.getLossSourceHash() : "not-required:no-loss"));
        snapshot.put("optionalLossReportLinks", hasLoss
                ? actualLinks(String.valueOf(receipt.getLossRecordId()), "LOSS_REPORT", receipt.getLossSourceHash())
                : List.of());
        snapshot.put("specialNodeUploadStatus", Map.of("overallStatus", "PENDING_UPLOAD",
                "requiredNodes", requiredNodes));
        snapshot.put("sourceHash", Map.of("stage3BatchExecutionSnapshot", hash(batch.getRouteSnapshotJson()),
                "backfillResultSnapshot", hash(receipt.getFormalSourceSnapshotJson()),
                "materialIssueSource", hash(snapshot.get("materialIssueSource")),
                "batchRecordLinks", hash(snapshot.get("batchRecordLinks")),
                "processInspectionLinks", hash(snapshot.get("processInspectionLinks")),
                "optionalLossReportLinks", hash(snapshot.get("optionalLossReportLinks")),
                "specialNodeUploadStatus", hash(snapshot.get("specialNodeUploadStatus"))));
        snapshot.put("status", MesStage4DossierUploadSimulationContractValidator.INPUT_STATUS);
        snapshot.put("blockers", List.of());
        return snapshot;
    }

    private List<Map<String, Object>> actualLinks(String rawIds, String category, String sourceHash) {
        if (rawIds == null || rawIds.isBlank() || sourceHash == null || sourceHash.isBlank()) {
            throw new IllegalStateException("STAGE4_BACKFILL_SOURCE_INVALID:" + category);
        }
        List<?> ids = rawIds != null && rawIds.trim().startsWith("[")
                ? JSON.parseArray(rawIds) : List.of(rawIds);
        if (ids.isEmpty() || ids.stream().anyMatch(id -> id == null || Long.parseLong(String.valueOf(id)) <= 0)) {
            throw new IllegalStateException("STAGE4_BACKFILL_SOURCE_INVALID:" + category);
        }
        return ids.stream().map(id -> Map.<String, Object>of(
                "sourceId", String.valueOf(id), "recordCategory", category,
                "backfillStatus", "COMPLETED", "sourceHash", sourceHash)).toList();
    }

    private Map<String, Object> buildDossierSnapshot(MesProEdhrBatchExecutionDO batch, String simulationRunId,
                                                     String sourceSnapshotHash) {
        Map<String, MesProBatchRecordExecutionAttachmentDO> latestAttachments = new LinkedHashMap<>();
        for (MesProBatchRecordExecutionAttachmentDO attachment : attachmentMapper
                .selectListByBatchExecutionId(batch.getId())) {
            if (!Objects.equals("ADD", attachment.getAttachmentAction())
                    || !MesStage4DossierUploadSimulationContractValidator.REQUIRED_NODE_TYPES
                    .contains(attachment.getFieldKey())) {
                continue;
            }
            MesProBatchRecordExecutionAttachmentDO current = latestAttachments.get(attachment.getFieldKey());
            if (current == null || attachment.getVersionNo() > current.getVersionNo()
                    || attachment.getId() > current.getId()) {
                latestAttachments.put(attachment.getFieldKey(), attachment);
            }
        }
        if (latestAttachments.size() != 4) {
            throw new IllegalStateException("STAGE4_DOSSIER_ATTACHMENT_INCOMPLETE");
        }
        List<MesProEdhrBatchExecutionTaskDO> tasks = batchTaskMapper.selectListByBatchExecutionId(batch.getId());
        Map<String, Object> nodeStatuses = new LinkedHashMap<>();
        for (NodeDefinition node : NODES) {
            MesProEdhrBatchExecutionTaskDO task = tasks.stream()
                    .filter(item -> Objects.equals(node.nodeType(), item.getNodeType()))
                    .findFirst().orElseThrow();
            nodeStatuses.put(node.nodeType(), task.getStatus() == MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED
                    ? "COMPLETED" : "PENDING_UPLOAD");
        }
        MesProBatchRecordExecutionAttachmentDO incoming = latestAttachments.get("INCOMING_INSPECTION_REPORT");
        MesProBatchRecordExecutionAttachmentDO sterilization = latestAttachments.get("STERILIZATION_REPORT");
        MesProBatchRecordExecutionAttachmentDO finishedReport = latestAttachments.get("FINISHED_PRODUCT_INSPECTION_REPORT");
        MesProBatchRecordExecutionAttachmentDO finishedRecord = latestAttachments.get("FINISHED_PRODUCT_INSPECTION_RECORD");
        List<MesProBatchRecordExecutionAttachmentDO> all = List.of(incoming, sterilization, finishedReport, finishedRecord);
        if (all.stream().anyMatch(item -> item.getSha256() == null || !item.getSha256().matches("[0-9a-f]{64}")
                || item.getAttachmentHash() == null || item.getAttachmentHash().isBlank()
                || item.getVersionNo() != 1)) {
            throw new IllegalStateException("STAGE4_DOSSIER_ATTACHMENT_HASH_INVALID");
        }
        Map<String, Object> hashes = new LinkedHashMap<>();
        hashes.put("incomingInspectionAttachmentHash", incoming.getSha256());
        hashes.put("sterilizationAttachmentHash", sterilization.getSha256());
        hashes.put("finishedProductInspectionAttachmentHashes", List.of(
                finishedReport.getSha256(), finishedRecord.getSha256()));
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("contractName", MesStage4DossierUploadSimulationContractValidator.OUTPUT_CONTRACT_NAME);
        snapshot.put("contractVersion", MesStage4DossierUploadSimulationContractValidator.OUTPUT_CONTRACT_VERSION);
        snapshot.put("simulationRunId", simulationRunId);
        snapshot.put("batchExecutionId", String.valueOf(batch.getId()));
        snapshot.put("sourceSnapshotHash", sourceSnapshotHash);
        snapshot.put("incomingInspectionAttachmentId", String.valueOf(incoming.getId()));
        snapshot.put("sterilizationAttachmentId", String.valueOf(sterilization.getId()));
        snapshot.put("finishedProductInspectionAttachmentIds", List.of(
                String.valueOf(finishedReport.getId()), String.valueOf(finishedRecord.getId())));
        snapshot.put("hashes", hashes);
        snapshot.put("nodeStatuses", nodeStatuses);
        snapshot.put("fileId", Map.of(
                "INCOMING_INSPECTION_REPORT", String.valueOf(incoming.getFileId()),
                "STERILIZATION_REPORT", String.valueOf(sterilization.getFileId()),
                "FINISHED_PRODUCT_INSPECTION_REPORT", String.valueOf(finishedReport.getFileId()),
                "FINISHED_PRODUCT_INSPECTION_RECORD", String.valueOf(finishedRecord.getFileId())));
        snapshot.put("fileName", Map.of(
                "INCOMING_INSPECTION_REPORT", incoming.getFileName(),
                "STERILIZATION_REPORT", sterilization.getFileName(),
                "FINISHED_PRODUCT_INSPECTION_REPORT", finishedReport.getFileName(),
                "FINISHED_PRODUCT_INSPECTION_RECORD", finishedRecord.getFileName()));
        snapshot.put("storagePath", Map.of(
                "INCOMING_INSPECTION_REPORT", incoming.getStoragePath(),
                "STERILIZATION_REPORT", sterilization.getStoragePath(),
                "FINISHED_PRODUCT_INSPECTION_REPORT", finishedReport.getStoragePath(),
                "FINISHED_PRODUCT_INSPECTION_RECORD", finishedRecord.getStoragePath()));
        snapshot.put("attachmentAudit", all.stream().collect(java.util.stream.Collectors.toMap(
                MesProBatchRecordExecutionAttachmentDO::getFieldKey,
                attachment -> toAttachmentAudit(attachment, sourceSnapshotHash),
                (left, right) -> left,
                LinkedHashMap::new)));
        snapshot.put("fileEvidence", all.stream().collect(java.util.stream.Collectors.toMap(
                MesProBatchRecordExecutionAttachmentDO::getFieldKey,
                attachment -> toFileEvidence(attachment, nodeStatuses, sourceSnapshotHash),
                (left, right) -> left,
                LinkedHashMap::new)));
        snapshot.put("sterilizationBatchNo", "STE-STAGE4-" + shortRunId(simulationRunId));
        snapshot.put("reportSnapshotHash", hash(JSON.toJSONString(snapshot)));
        snapshot.put("dossierReadyForRelease", nodeStatuses.values().stream().allMatch("COMPLETED"::equals));
        snapshot.put("finalReleaseRecordId", null);
        snapshot.put("blockers", List.of());
        return snapshot;
    }

    private Map<String, Object> toAttachmentAudit(MesProBatchRecordExecutionAttachmentDO attachment,
                                                  String sourceSnapshotHash) {
        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("attachmentId", String.valueOf(attachment.getId()));
        audit.put("fileId", String.valueOf(attachment.getFileId()));
        audit.put("nodeType", attachment.getFieldKey());
        audit.put("attachmentAction", attachment.getAttachmentAction());
        audit.put("versionNo", attachment.getVersionNo());
        audit.put("fileName", attachment.getFileName());
        audit.put("storagePath", attachment.getStoragePath());
        audit.put("sha256", attachment.getSha256());
        audit.put("attachmentHash", attachment.getAttachmentHash());
        audit.put("operatorId", String.valueOf(attachment.getOperatorId()));
        audit.put("operatorName", attachment.getOperatorName());
        audit.put("operatedAt", String.valueOf(attachment.getOperatedAt()));
        audit.put("verificationStatus", "VALID");
        audit.put("sourceSnapshotHash", sourceSnapshotHash);
        return audit;
    }

    private Map<String, Object> toFileEvidence(MesProBatchRecordExecutionAttachmentDO attachment,
                                                Map<String, Object> nodeStatuses,
                                                String sourceSnapshotHash) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("nodeKey", attachment.getFieldKey());
        evidence.put("fileId", String.valueOf(attachment.getFileId()));
        evidence.put("fileName", attachment.getFileName());
        evidence.put("sha256", attachment.getSha256());
        evidence.put("sourceSnapshotHash", sourceSnapshotHash);
        evidence.put("source", FILE_EVIDENCE_SOURCE);
        evidence.put("completionStatus", nodeStatuses.get(attachment.getFieldKey()));
        evidence.put("attachmentId", String.valueOf(attachment.getId()));
        evidence.put("storagePath", attachment.getStoragePath());
        return evidence;
    }

    private MesProEdhrSpecialNodeAttachment toAttachment(
            MesProEdhrSpecialNodeAttachmentPrepareUploadResult result) {
        return new MesProEdhrSpecialNodeAttachment()
                .setUploadToken(result.getUploadToken())
                .setFileId(result.getFileId())
                .setFileUrl(result.getFileUrl())
                .setStorageConfigId(result.getStorageConfigId())
                .setStoragePath(result.getStoragePath())
                .setFileName(result.getFileName())
                .setContentType(result.getContentType())
                .setFileSize(result.getFileSize())
                .setSha256(result.getSha256())
                .setStorageRetentionJson(result.getStorageRetentionJson())
                .setStorageRetentionHash(result.getStorageRetentionHash());
    }

    private byte[] buildFileContent(String simulationRunId, String nodeType) {
        return ("Stage4 dossier simulation run=" + simulationRunId + " node=" + nodeType)
                .getBytes(StandardCharsets.UTF_8);
    }

    private String shortRunId(String simulationRunId) {
        String value = simulationRunId.replaceAll("[^A-Za-z0-9]", "");
        if (value.length() <= 24) {
            return value;
        }
        String prefix = value.substring(0, 12);
        String suffix = DigestUtil.sha256Hex(simulationRunId).substring(0, 12).toUpperCase(Locale.ROOT);
        return prefix + suffix;
    }

    private String hash(Object value) {
        return DigestUtil.sha256Hex(JSON.toJSONString(value));
    }

    private String canonicalHash(String json) {
        return DigestUtil.sha256Hex(MesProBatchRecordExecutionFieldAuditHasher.canonicalizeJsonString(json));
    }

    private record NodeDefinition(String nodeType, String nodeName, int sort, String fileName) {
    }

    private record FormalStage4Source(MesProEdhrBatchProvisioningRecordDO provisioningRecord,
                                      MesProcessPoolActiveOrderCompletionReceiptDO completionReceipt,
                                      MesProcessPoolActiveOrderPickListBindingDO pickListBinding) {
    }

    private record Stage4InputResolution(MesProEdhrBatchExecutionDO batch,
                                         FormalStage4Source source,
                                         Map<String, Object> completeBatchExecutionSnapshot) {
    }

    private record IndependentBatchExecutionInputFixture(MesProEdhrBatchExecutionDO batch,
                                                         FormalStage4Source source,
                                                         Map<String, Object> completeBatchExecutionSnapshot) {
    }
}
