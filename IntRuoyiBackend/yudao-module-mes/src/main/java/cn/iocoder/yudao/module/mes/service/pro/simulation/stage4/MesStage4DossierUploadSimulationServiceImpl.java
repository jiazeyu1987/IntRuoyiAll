package cn.iocoder.yudao.module.mes.service.pro.simulation.stage4;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionAttachmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionAttachmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrSpecialNodeAttachment;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrSpecialNodeAttachmentPrepareUploadCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrSpecialNodeAttachmentPrepareUploadResult;
import com.alibaba.fastjson.JSON;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class MesStage4DossierUploadSimulationServiceImpl
        implements MesStage4DossierUploadSimulationService {

    private static final String DETAIL_PATH = "/mes/pro/feedback/edhr-batch-execution/detail";
    private static final String FILE_EVIDENCE_SOURCE =
            "STAGE4_INDEPENDENT_FIXTURE_FORMAL_ATTACHMENT_UPLOAD";
    private static final String SIMULATION_MARKER = "[STAGE4_SIMULATION]";
    private static final Pattern RUN_ID = Pattern.compile("[A-Za-z0-9._:-]{1,128}");
    private static final List<NodeDefinition> NODES = List.of(
            new NodeDefinition("INCOMING_INSPECTION_REPORT", "来料检报告", 0, "incoming-inspection-report.pdf"),
            new NodeDefinition("STERILIZATION_REPORT", "灭菌报告", 9000, "sterilization-report.pdf"),
            new NodeDefinition("FINISHED_PRODUCT_INSPECTION_REPORT", "成品检报告", 9010,
                    "finished-product-inspection-report.pdf"),
            new NodeDefinition("FINISHED_PRODUCT_INSPECTION_RECORD", "成品检记录", 9020,
                    "finished-product-inspection-record.pdf"));

    private final MesProEdhrBatchExecutionMapper batchExecutionMapper;
    private final MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    private final MesProEdhrBatchExecutionOriginMapper originMapper;
    private final MesProcessPoolActiveOrderCompletionReceiptMapper completionReceiptMapper;
    private final MesProBatchRecordExecutionAttachmentMapper attachmentMapper;
    private final MesProEdhrBatchExecutionService batchExecutionService;
    private final FileService fileService;

    public MesStage4DossierUploadSimulationServiceImpl(
            MesProEdhrBatchExecutionMapper batchExecutionMapper,
            MesProEdhrBatchExecutionTaskMapper batchTaskMapper,
            MesProEdhrBatchExecutionOriginMapper originMapper,
            MesProcessPoolActiveOrderCompletionReceiptMapper completionReceiptMapper,
            MesProBatchRecordExecutionAttachmentMapper attachmentMapper,
            MesProEdhrBatchExecutionService batchExecutionService,
            FileService fileService) {
        this.batchExecutionMapper = batchExecutionMapper;
        this.batchTaskMapper = batchTaskMapper;
        this.originMapper = originMapper;
        this.completionReceiptMapper = completionReceiptMapper;
        this.attachmentMapper = attachmentMapper;
        this.batchExecutionService = batchExecutionService;
        this.fileService = fileService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesStage4DossierUploadSimulationResult simulate(MesStage4DossierUploadSimulationCommand command) {
        Long actorUserId = requireActor(command);
        String simulationRunId = normalizeRunId(command.getSimulationRunId());
        MesProEdhrBatchExecutionDO batch = requireStage2_5Batch(command);
        MesProEdhrBatchExecutionOriginDO origin = requireSingleOrigin(batch.getId());
        MesProcessPoolActiveOrderCompletionReceiptDO completionReceipt = completionReceiptMapper
                .selectByActiveOrderIdForUpdate(origin.getActiveOrderId());
        if (completionReceipt == null || !Objects.equals(completionReceipt.getId(), origin.getCompletionBackfillReceiptId())
                || !Objects.equals(completionReceipt.getReceiptHash(), origin.getCompletionBackfillReceiptHash())) {
            throw new IllegalStateException("STAGE4_COMPLETION_RECEIPT_SOURCE_INVALID");
        }
        String cleanedRunId = cleanupOwnedAttachments(batch.getId());
        batch.setRemark(batch.getRemark() + SIMULATION_MARKER + "[simulationRunId=" + simulationRunId + "]");
        batchExecutionMapper.updateById(batch);
        Map<String, MesProEdhrBatchExecutionTaskDO> tasks = loadFixtureTasks(batch.getId());
        Map<String, Object> inputSnapshot = buildInputSnapshot(batch, tasks, simulationRunId,
                origin, completionReceipt);
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
            batchExecutionService.completeSpecialNode(task.getId(),
                    "STERILIZATION_REPORT".equals(node.nodeType())
                            ? "STE-STAGE4-" + shortRunId(simulationRunId) : null,
                    List.of(toAttachment(prepared)));
            markAttachmentOwned(batch.getId(), task.getId(), simulationRunId);
        }

        Map<String, Object> dossierSnapshot = buildDossierSnapshot(batch, simulationRunId);
        MesStage4DossierUploadSimulationContractValidator.validateOutput(dossierSnapshot);
        return new MesStage4DossierUploadSimulationResult()
                .setSimulationRunId(simulationRunId)
                .setCleanedSimulationRunId(cleanedRunId)
                .setBatchExecutionId(String.valueOf(batch.getId()))
                .setBatchExecutionCode(batch.getBatchExecutionCode())
                .setDetailPath(DETAIL_PATH + "?id=" + batch.getId() + "&simulationRunId=" + simulationRunId)
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

    private MesProEdhrBatchExecutionOriginDO requireSingleOrigin(Long batchExecutionId) {
        List<MesProEdhrBatchExecutionOriginDO> origins = originMapper.selectListByBatchExecutionId(batchExecutionId);
        if (origins.size() != 1 || origins.get(0).getActiveOrderId() == null
                || origins.get(0).getWorkOrderId() == null || origins.get(0).getPickListId() == null
                || origins.get(0).getPickListBindingId() == null
                || origins.get(0).getCompletionBackfillReceiptId() == null
                || origins.get(0).getCompletionBackfillReceiptHash() == null) {
            throw new IllegalStateException("STAGE4_FORMAL_ORIGIN_REQUIRED");
        }
        return origins.get(0);
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
        attachments.forEach(item -> attachmentMapper.deleteById(item.getId()));
        return runId.substring(0, end);
    }

    private void markAttachmentOwned(Long batchExecutionId, Long batchTaskId, String runId) {
        List<MesProBatchRecordExecutionAttachmentDO> attachments = attachmentMapper
                .selectListByBatchExecutionId(batchExecutionId).stream()
                .filter(item -> Objects.equals(item.getBatchTaskId(), batchTaskId))
                .max(java.util.Comparator.comparing(MesProBatchRecordExecutionAttachmentDO::getId))
                .stream().toList();
        attachments.forEach(item -> attachmentMapper.updateById(item
                .setReasonText(SIMULATION_MARKER + "[simulationRunId=" + runId + "]")));
    }

    private MesProEdhrBatchExecutionDO createFixture(Long actorUserId, String simulationRunId) {
        long base = 8_000_000_000L + Integer.toUnsignedLong(simulationRunId.hashCode()) * 10L;
        String safe = shortRunId(simulationRunId);
        MesProEdhrBatchExecutionDO batch = new MesProEdhrBatchExecutionDO()
                .setBatchExecutionCode("STAGE4-SIM-" + safe)
                .setWorkOrderId(base + 200)
                .setWorkOrderCode("STAGE4-WO-" + safe)
                .setBatchCode("STAGE4-BATCH-" + safe)
                .setActiveContextKey(MesStage4DossierUploadSimulationMarker.value(simulationRunId))
                .setAttemptNo(1)
                .setProductId(base + 100)
                .setProductCode("STAGE4-SIM-PRODUCT")
                .setProductName("Stage4 dossier upload fixture")
                .setRouteId(base + 300)
                .setRouteVersionId(base + 301)
                .setRouteVersionNo("STAGE4-V1")
                .setRouteCode("STAGE4-SIM-ROUTE")
                .setRouteName("Stage4 dossier upload fixture route")
                .setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_IN_PROGRESS)
                .setTaskTotal(NODES.size())
                .setTaskApprovedCount(0)
                .setBlockedCount(0)
                .setRemark(MesStage4DossierUploadSimulationMarker.value(simulationRunId))
                .setRouteSnapshotJson(buildRouteSnapshot(actorUserId, simulationRunId));
        batchExecutionMapper.insert(batch);
        for (NodeDefinition node : NODES) {
            batchTaskMapper.insert(new MesProEdhrBatchExecutionTaskDO()
                    .setBatchExecutionId(batch.getId())
                    .setNodeType(node.nodeType())
                    .setRouteProcessId(base + node.sort())
                    .setRouteProcessSort(node.sort())
                    .setProcessId(base + 500 + node.sort())
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
                    .setRequiredPolicy("REQUIRED")
                    .setOwnerRoleKey("QUALITY")
                    .setArchiveVisibility("FINAL_DHR")
                    .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING)
                    .setRequiredFlag(true)
                    .setSpecialPayloadJson(JSON.toJSONString(Map.of(
                            "stageCode", "STAGE4_DOSSIER_UPLOAD",
                            "simulationRunId", simulationRunId,
                            "isSimulation", true,
                            "status", "PENDING_UPLOAD"))));
        }
        return batch;
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
                                                    MesProEdhrBatchExecutionOriginDO origin,
                                                    MesProcessPoolActiveOrderCompletionReceiptDO receipt) {
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
        snapshot.put("activeOrderId", String.valueOf(origin.getActiveOrderId()));
        snapshot.put("activeOrderCode", origin.getOriginKey());
        snapshot.put("workOrderId", String.valueOf(origin.getWorkOrderId()));
        snapshot.put("workOrderCode", batch.getWorkOrderCode());
        snapshot.put("erpWorkOrderNo", batch.getWorkOrderCode());
        snapshot.put("routeId", String.valueOf(batch.getRouteId()));
        snapshot.put("routeVersionId", String.valueOf(batch.getRouteVersionId()));
        snapshot.put("routeVersionNo", batch.getRouteVersionNo());
        snapshot.put("materialIssueSource", Map.of(
                "pickListId", String.valueOf(origin.getPickListId()),
                "pickListBindingId", String.valueOf(origin.getPickListBindingId()),
                "sourceSnapshotHash", origin.getSourceSnapshotHash()));
        snapshot.put("batchRecordLinks", actualLinks(receipt.getBatchRecordSourceIdsJson(),
                "BATCH_RECORD", origin.getSourceSnapshotHash()));
        snapshot.put("processInspectionLinks", actualLinks(receipt.getProcessInspectionSourceIdsJson(),
                "PROCESS_INSPECTION", origin.getSourceSnapshotHash()));
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

    private Map<String, Object> buildDossierSnapshot(MesProEdhrBatchExecutionDO batch, String simulationRunId) {
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
                this::toAttachmentAudit,
                (left, right) -> left,
                LinkedHashMap::new)));
        snapshot.put("fileEvidence", all.stream().collect(java.util.stream.Collectors.toMap(
                MesProBatchRecordExecutionAttachmentDO::getFieldKey,
                attachment -> toFileEvidence(attachment, nodeStatuses),
                (left, right) -> left,
                LinkedHashMap::new)));
        snapshot.put("sterilizationBatchNo", "STE-STAGE4-" + shortRunId(simulationRunId));
        snapshot.put("reportSnapshotHash", hash(JSON.toJSONString(snapshot)));
        snapshot.put("dossierReadyForRelease", nodeStatuses.values().stream().allMatch("COMPLETED"::equals));
        snapshot.put("finalReleaseRecordId", null);
        snapshot.put("blockers", List.of());
        return snapshot;
    }

    private Map<String, Object> toAttachmentAudit(MesProBatchRecordExecutionAttachmentDO attachment) {
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
        return audit;
    }

    private Map<String, Object> toFileEvidence(MesProBatchRecordExecutionAttachmentDO attachment,
                                                Map<String, Object> nodeStatuses) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("nodeKey", attachment.getFieldKey());
        evidence.put("fileId", String.valueOf(attachment.getFileId()));
        evidence.put("fileName", attachment.getFileName());
        evidence.put("sha256", attachment.getSha256());
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
        return value.length() <= 24 ? value : value.substring(0, 24);
    }

    private String hash(Object value) {
        return DigestUtil.sha256Hex(JSON.toJSONString(value));
    }

    private record NodeDefinition(String nodeType, String nodeName, int sort, String fileName) {
    }
}
