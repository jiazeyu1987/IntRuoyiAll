package cn.iocoder.yudao.module.mes.service.pro.simulation.stage5;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.dal.dataobject.signature.BpmApprovalSignatureRecordDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.signature.BpmApprovalSignatureRecordMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionAttachmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTraceLinkDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTraceManifestDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseDecisionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionBackfillDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionAttachmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTraceLinkMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTraceManifestMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseDecisionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionBackfillMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListItemMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderSourceTypeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderTypeEnum;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceCaptureCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceEntryType;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceLinkType;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceSource;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceSourceHash;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityValidator;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHasher;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionReceiptHash;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrSpecialNodeAttachment;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrSpecialNodeAttachmentPrepareUploadCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrSpecialNodeAttachmentPrepareUploadResult;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.report.MesProductionReleaseManagerStageInitializationResult;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.report.MesProductionReleaseManagerStageInitializer;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.report.MesProductionReleaseManagerStageInitializationCommand;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.report.MesProductionReleaseReportNodeEvidence;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.report.MesProductionReleaseReportSnapshots;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class MesStage5FinalReleaseSimulationServiceImpl implements MesStage5FinalReleaseSimulationService {

    public static final String SIMULATION_MARKER =
            "[STAGE5_SIMULATION][stageCode=STAGE5_FINAL_RELEASE][isSimulation=true]";
    private static final String DETAIL_PATH = "/mes/pro/feedback/edhr-batch-execution/detail";
    private static final Pattern RUN_ID = Pattern.compile("[A-Za-z0-9._:-]{1,128}");
    private static final List<NodeDefinition> NODES = List.of(
            new NodeDefinition("INCOMING_INSPECTION_REPORT", "来料检报告", 0, "stage5-incoming-report.pdf"),
            new NodeDefinition("STERILIZATION_REPORT", "灭菌报告", 9000, "stage5-sterilization-report.pdf"),
            new NodeDefinition("FINISHED_PRODUCT_INSPECTION_REPORT", "成品检报告", 9010, "stage5-finished-report.pdf"),
            new NodeDefinition("FINISHED_PRODUCT_INSPECTION_RECORD", "成品检记录", 9020, "stage5-finished-record.pdf"));

    private final MesProEdhrBatchExecutionMapper batchExecutionMapper;
    private final MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    private final MesProEdhrBatchExecutionOriginMapper originMapper;
    private final MesProEdhrBatchExecutionTraceLinkMapper traceLinkMapper;
    private final MesProEdhrBatchExecutionTraceManifestMapper traceManifestMapper;
    private final MesProBatchRecordExecutionAttachmentMapper attachmentMapper;
    private final MesProcessPoolActiveOrderCompletionReceiptMapper completionReceiptMapper;
    private final MesProcessPoolActiveOrderCompletionBackfillMapper completionBackfillMapper;
    private final MesProcessPoolActiveOrderPickListBindingMapper bindingMapper;
    private final MesProcessPoolActiveOrderPickListBindingItemMapper bindingItemMapper;
    private final ErpKingdeeProductionPickListMapper pickListMapper;
    private final ErpKingdeeProductionPickListItemMapper pickListItemMapper;
    private final MesProEdhrBatchTraceabilityService traceabilityService;
    private final MesProEdhrBatchExecutionService batchExecutionService;
    private final MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper;
    private final MesProductionReleaseManagerStageInitializer managerStageInitializer;
    private final MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    private final MesProEdhrReleaseTransactionEventMapper releaseEventMapper;
    private final MesProEdhrReleaseDecisionMapper releaseDecisionMapper;
    private final MesProEdhrWorkTaskMapper workTaskMapper;
    private final MesProWorkOrderMapper workOrderMapper;
    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final FileService fileService;
    private final BpmApprovalSignatureRecordMapper approvalSignatureRecordMapper;

    public MesStage5FinalReleaseSimulationServiceImpl(
            MesProEdhrBatchExecutionMapper batchExecutionMapper,
            MesProEdhrBatchExecutionTaskMapper batchTaskMapper,
            MesProEdhrBatchExecutionOriginMapper originMapper,
            MesProEdhrBatchExecutionTraceLinkMapper traceLinkMapper,
            MesProEdhrBatchExecutionTraceManifestMapper traceManifestMapper,
            MesProBatchRecordExecutionAttachmentMapper attachmentMapper,
            MesProcessPoolActiveOrderCompletionReceiptMapper completionReceiptMapper,
            MesProcessPoolActiveOrderCompletionBackfillMapper completionBackfillMapper,
            MesProcessPoolActiveOrderPickListBindingMapper bindingMapper,
            MesProcessPoolActiveOrderPickListBindingItemMapper bindingItemMapper,
            ErpKingdeeProductionPickListMapper pickListMapper,
            ErpKingdeeProductionPickListItemMapper pickListItemMapper,
            MesProEdhrBatchTraceabilityService traceabilityService,
            MesProEdhrBatchExecutionService batchExecutionService,
            MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper,
            MesProductionReleaseManagerStageInitializer managerStageInitializer,
            MesProEdhrReleaseTransactionMapper releaseTransactionMapper,
            MesProEdhrReleaseTransactionEventMapper releaseEventMapper,
            MesProEdhrReleaseDecisionMapper releaseDecisionMapper,
            MesProEdhrWorkTaskMapper workTaskMapper,
            MesProWorkOrderMapper workOrderMapper,
            MesProcessPoolActiveOrderMapper activeOrderMapper,
            FileService fileService,
            BpmApprovalSignatureRecordMapper approvalSignatureRecordMapper) {
        this.batchExecutionMapper = batchExecutionMapper;
        this.batchTaskMapper = batchTaskMapper;
        this.originMapper = originMapper;
        this.traceLinkMapper = traceLinkMapper;
        this.traceManifestMapper = traceManifestMapper;
        this.attachmentMapper = attachmentMapper;
        this.completionReceiptMapper = completionReceiptMapper;
        this.completionBackfillMapper = completionBackfillMapper;
        this.bindingMapper = bindingMapper;
        this.bindingItemMapper = bindingItemMapper;
        this.pickListMapper = pickListMapper;
        this.pickListItemMapper = pickListItemMapper;
        this.traceabilityService = traceabilityService;
        this.batchExecutionService = batchExecutionService;
        this.applicationMapper = applicationMapper;
        this.managerStageInitializer = managerStageInitializer;
        this.releaseTransactionMapper = releaseTransactionMapper;
        this.releaseEventMapper = releaseEventMapper;
        this.releaseDecisionMapper = releaseDecisionMapper;
        this.workTaskMapper = workTaskMapper;
        this.workOrderMapper = workOrderMapper;
        this.activeOrderMapper = activeOrderMapper;
        this.fileService = fileService;
        this.approvalSignatureRecordMapper = approvalSignatureRecordMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesStage5FinalReleaseSimulationResult prepare(MesStage5FinalReleaseSimulationCommand command) {
        Long actorUserId = requireActor(command);
        String runId = normalizeRunId(command.getSimulationRunId());
        String previousRunId = normalizeOptionalRunId(command.getPreviousSimulationRunId());
        if (batchExecutionMapper.selectStage5SimulationByRemark(marker(runId)) != null) {
            throw new IllegalStateException("STAGE5_SIMULATION_RUN_ID_ALREADY_EXISTS");
        }
        String cleanedRunId = cleanupPreviousSimulation(previousRunId);
        Fixture fixture = createFixture(actorUserId, runId);
        Map<String, Object> dossier = buildDossierSnapshot(fixture.batch(), runId);
        MesStage5FinalReleaseSimulationContractValidator.validateDossierSnapshot(dossier);

        MesProcessPoolActiveOrderReleaseApplicationDO application = createApplication(
                fixture, actorUserId, dossier, runId);
        List<MesProductionReleaseReportNodeEvidence> evidences = buildReportEvidences(fixture.batch().getId());
        String reportSnapshotHash = MesProductionReleaseReportSnapshots.hash(application, evidences);
        MesProductionReleaseManagerStageInitializationResult managerStage =
                managerStageInitializer.initializeManagerReleaseStage(
                        new MesProductionReleaseManagerStageInitializationCommand()
                                .setApplicationId(application.getId())
                                .setBatchExecutionId(fixture.batch().getId())
                                .setExpectedApplicationVersion(application.getVersion())
                                .setReportSnapshotHash(reportSnapshotHash)
                                .setReportEvidences(evidences));
        if (applicationMapper.handoffReportsToManager(application.getId(), application.getVersion(),
                reportSnapshotHash, managerStage.getReleaseTransactionId(),
                managerStage.getManagerReleaseWorkTaskId(), managerStage.getManagerCandidateSnapshotHash()) != 1) {
            throw new IllegalStateException("STAGE5_MANAGER_HANDOFF_VERSION_CONFLICT");
        }
        markOwnedManagerRows(managerStage, runId);
        String managerSignoffEvidenceHash = createManagerSimulationSignature(
                managerStage.getManagerReleaseWorkTaskId(), actorUserId, runId);

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("releaseApplicationId", String.valueOf(application.getId()));
        context.put("releaseTransactionId", String.valueOf(managerStage.getReleaseTransactionId()));
        context.put("managerReleaseWorkTaskId", String.valueOf(managerStage.getManagerReleaseWorkTaskId()));
        context.put("managerCandidateSnapshotHash", managerStage.getManagerCandidateSnapshotHash());
        context.put("reportSnapshotHash", reportSnapshotHash);
        context.put("releaseStatus", MesStage5FinalReleaseSimulationContractValidator.PENDING_RELEASE_STATUS);
        context.put("applicationStatus", MesStage5FinalReleaseSimulationContractValidator.PENDING_APPLICATION_STATUS);
        context.put("transactionVersion", 1);
        context.put("candidateFrozen", true);
        context.put("authoritativeUpstreamContext", "FLOW4_FLOW6_FLOW8_FIXTURE_READY");
        context.put("managerSignoffEvidenceHash", managerSignoffEvidenceHash);

        Map<String, Object> precheck = new LinkedHashMap<>();
        precheck.put("passed", true);
        precheck.put("scope", "STAGE5_REPORT_DOSSIER_AND_MANAGER_HANDOFF");
        precheck.put("finalReleaseReady", false);
        precheck.put("finalReleaseBlocker", "MANAGER_APPROVAL_REQUIRED");
        Map<String, Object> runManifest = new LinkedHashMap<>();
        runManifest.put("simulationRunId", runId);
        runManifest.put("stageCode", "STAGE5_FINAL_RELEASE");
        runManifest.put("isSimulation", true);
        runManifest.put("batchExecutionId", String.valueOf(fixture.batch().getId()));
        runManifest.put("releaseApplicationId", String.valueOf(application.getId()));
        runManifest.put("releaseTransactionId", String.valueOf(managerStage.getReleaseTransactionId()));
        runManifest.put("managerReleaseWorkTaskId", String.valueOf(managerStage.getManagerReleaseWorkTaskId()));
        runManifest.put("managerSignoffEvidenceHash", managerSignoffEvidenceHash);
        runManifest.put("ownedObjectTypes", List.of("WORK_ORDER", "ACTIVE_ORDER", "PICK_LIST",
                "PICK_LIST_BINDING", "COMPLETION_RECEIPT", "COMPLETION_BACKFILL", "TRACE_ORIGIN",
                "TRACE_LINK", "TRACE_MANIFEST", "BATCH_EXECUTION", "BATCH_EXECUTION_TASK",
                "ATTACHMENT", "RELEASE_APPLICATION", "RELEASE_TRANSACTION", "RELEASE_DECISION",
                "MANAGER_RELEASE_WORK_TASK", "MANAGER_ELECTRONIC_SIGNATURE"));
        runManifest.put("cleanupRule", "EXACT_STAGE5_MARKER_AND_PREVIOUS_SIMULATION_RUN_ID");
        runManifest.put("finalReleaseStatus", "PENDING_APPROVAL");
        runManifest.put("releaseSnapshotStatus", "NOT_CREATED");
        runManifest.put("blockers", List.of("MANAGER_APPROVAL_REQUIRED"));

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("simulationRunId", runId);
        output.put("batchExecutionId", String.valueOf(fixture.batch().getId()));
        output.put("releaseApplicationId", String.valueOf(application.getId()));
        output.put("releaseTransactionId", String.valueOf(managerStage.getReleaseTransactionId()));
        output.put("managerReleaseWorkTaskId", String.valueOf(managerStage.getManagerReleaseWorkTaskId()));
        output.put("managerSignoffEvidenceHash", managerSignoffEvidenceHash);
        output.put("managerCandidateSnapshotHash", managerStage.getManagerCandidateSnapshotHash());
        output.put("reportSnapshotHash", reportSnapshotHash);
        output.put("sourceDossierHash", reportSnapshotHash);
        output.put("releaseStatus", MesStage5FinalReleaseSimulationContractValidator.PENDING_RELEASE_STATUS);
        output.put("applicationStatus", MesStage5FinalReleaseSimulationContractValidator.PENDING_APPLICATION_STATUS);
        output.put("finalReleaseReady", false);
        output.put("batchExecutionDossierSnapshot", dossier);
        output.put("managerReleaseContext", context);
        output.put("precheckResult", precheck);
        output.put("blockers", List.of("MANAGER_APPROVAL_REQUIRED"));

        MesStage5FinalReleaseSimulationResult result = new MesStage5FinalReleaseSimulationResult()
                .setSimulationRunId(runId)
                .setCleanedSimulationRunId(cleanedRunId)
                .setBatchExecutionId(String.valueOf(fixture.batch().getId()))
                .setBatchExecutionCode(fixture.batch().getBatchExecutionCode())
                .setReleaseApplicationId(String.valueOf(application.getId()))
                .setReleaseTransactionId(String.valueOf(managerStage.getReleaseTransactionId()))
                .setManagerReleaseWorkTaskId(String.valueOf(managerStage.getManagerReleaseWorkTaskId()))
                .setManagerSignoffEvidenceHash(managerSignoffEvidenceHash)
                .setManagerCandidateSnapshotHash(managerStage.getManagerCandidateSnapshotHash())
                .setReportSnapshotHash(reportSnapshotHash)
                .setSourceDossierHash(reportSnapshotHash)
                .setReleaseStatus(MesStage5FinalReleaseSimulationContractValidator.PENDING_RELEASE_STATUS)
                .setApplicationStatus(MesStage5FinalReleaseSimulationContractValidator.PENDING_APPLICATION_STATUS)
                .setManagerWorkTaskPath("/mes/pro/edhr-work-task?taskType=RELEASE_APPROVE&simulationRunId=" + runId)
                .setFinalReleaseReady(false)
                .setBatchExecutionDossierSnapshot(dossier)
                .setManagerReleaseContext(context)
                .setPrecheckResult(precheck)
                .setRunManifest(runManifest)
                .setReleaseSnapshot(null)
                .setBlockers(List.of("MANAGER_APPROVAL_REQUIRED"));
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getReleaseSnapshot(String simulationRunId, Long batchExecutionId) {
        String runId = normalizeRunId(simulationRunId);
        MesProEdhrBatchExecutionDO batch = batchExecutionMapper.selectStage5SimulationByRemark(marker(runId));
        if (batch == null || (batchExecutionId != null && !Objects.equals(batch.getId(), batchExecutionId))) {
            throw new IllegalStateException("STAGE5_RELEASE_SNAPSHOT_BATCH_NOT_FOUND");
        }
        MesProcessPoolActiveOrderReleaseApplicationDO application = applicationMapper
                .selectByBatchExecutionIdForUpdate(batch.getId());
        if (application == null || !Objects.equals(application.getRemark(), marker(runId))
                || !Objects.equals(application.getApplicationStatus(), "RELEASED")
                || application.getReleaseTransactionId() == null) {
            throw new IllegalStateException("STAGE5_RELEASE_SNAPSHOT_APPLICATION_NOT_RELEASED");
        }
        MesProEdhrReleaseTransactionDO transaction = releaseTransactionMapper
                .selectById(application.getReleaseTransactionId());
        MesProEdhrReleaseDecisionDO decision = releaseDecisionMapper
                .selectReleasedByTransactionIdForUpdate(application.getReleaseTransactionId());
        MesProEdhrWorkTaskDO managerTask = application.getReleaseApprovalWorkTaskId() == null ? null
                : workTaskMapper.selectById(application.getReleaseApprovalWorkTaskId());
        if (transaction == null || decision == null || managerTask == null
                || !Objects.equals(transaction.getRemark(), marker(runId))
                || !Objects.equals(transaction.getReleaseStatus(), MesStage5FinalReleaseSimulationContractValidator.RELEASED_STATUS)
                || !Objects.equals(decision.getBatchExecutionId(), batch.getId())
                || !Objects.equals(decision.getDecisionStatus(), MesStage5FinalReleaseSimulationContractValidator.RELEASED_STATUS)) {
            throw new IllegalStateException("STAGE5_RELEASE_SNAPSHOT_AUTHORITATIVE_RECEIPT_INVALID");
        }
        List<MesProductionReleaseReportNodeEvidence> evidences = buildReportEvidences(batch.getId());
        MesProEdhrBatchExecutionOriginDO origin = requireSingleOrigin(batch.getId(), runId);
        MesProcessPoolActiveOrderCompletionReceiptDO completionReceipt = completionReceiptMapper
                .selectByActiveOrderIdForUpdate(origin.getActiveOrderId());
        List<MesProcessPoolActiveOrderCompletionBackfillDO> backfills = completionBackfillMapper
                .selectListByActiveOrderIdForUpdate(origin.getActiveOrderId());
        if (completionReceipt == null || !Objects.equals(completionReceipt.getId(), origin.getCompletionBackfillReceiptId())
                || !Objects.equals(completionReceipt.getReceiptHash(), origin.getCompletionBackfillReceiptHash())
                || backfills.size() != 3
                || backfills.stream().anyMatch(item -> !Objects.equals(item.getWorkOrderId(), origin.getWorkOrderId())
                || !"SUCCESS".equals(item.getStatus()))) {
            throw new IllegalStateException("STAGE5_RELEASE_SNAPSHOT_SOURCE_CHAIN_INVALID");
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("contractName", MesStage5FinalReleaseSimulationContractValidator.RELEASE_SNAPSHOT_CONTRACT_NAME);
        snapshot.put("contractVersion", MesStage5FinalReleaseSimulationContractValidator.RELEASE_SNAPSHOT_CONTRACT_VERSION);
        snapshot.put("simulationRunId", runId);
        snapshot.put("batchExecutionId", String.valueOf(batch.getId()));
        snapshot.put("releaseReceiptId", String.valueOf(transaction.getId()));
        snapshot.put("releaseDecisionId", String.valueOf(decision.getId()));
        snapshot.put("releasedAt", decision.getDecidedAt() == null ? transaction.getApprovedAt() : decision.getDecidedAt());
        snapshot.put("releaseStatus", transaction.getReleaseStatus());
        snapshot.put("approvedBy", transaction.getApprovedBy());
        snapshot.put("approvedAt", transaction.getApprovedAt());
        snapshot.put("approvalSignoffEvidenceHash", transaction.getApprovalSignoffEvidenceHash());
        snapshot.put("releaseApprovalWorkTaskId", String.valueOf(managerTask.getId()));
        snapshot.put("reportSnapshotHash", application.getReportSnapshotHash());
        snapshot.put("version", transaction.getVersion());
        snapshot.put("threeFileEvidence", List.of(
                categoryEvidence("INCOMING_INSPECTION_REPORT", evidences),
                categoryEvidence("STERILIZATION_REPORT", evidences),
                categoryEvidence("FINISHED_PRODUCT_INSPECTION", evidences)));
        snapshot.put("sourceChain", Map.of(
                "productionSourceIds", JSON.parseArray(completionReceipt.getBatchRecordSourceIdsJson(), Long.class),
                "processInspectionSourceIds", JSON.parseArray(completionReceipt.getProcessInspectionSourceIdsJson(), Long.class),
                "pickListId", String.valueOf(origin.getPickListId()),
                "pickListBindingId", String.valueOf(origin.getPickListBindingId()),
                "completionBackfillReceiptId", String.valueOf(origin.getCompletionBackfillReceiptId()),
                "backfillIds", backfills.stream().map(MesProcessPoolActiveOrderCompletionBackfillDO::getId).toList(),
                "sourceSnapshotHash", origin.getSourceSnapshotHash()));
        MesStage5FinalReleaseSimulationContractValidator.validateReleaseSnapshot(snapshot);
        return snapshot;
    }

    private Map<String, Object> categoryEvidence(String category,
                                                 List<MesProductionReleaseReportNodeEvidence> evidences) {
        List<MesProductionReleaseReportNodeEvidence> matching = evidences.stream()
                .filter(item -> "FINISHED_PRODUCT_INSPECTION".equals(category)
                        ? category.equals("FINISHED_PRODUCT_INSPECTION") &&
                        ("FINISHED_PRODUCT_INSPECTION_REPORT".equals(item.getNodeType())
                                || "FINISHED_PRODUCT_INSPECTION_RECORD".equals(item.getNodeType()))
                        : Objects.equals(category, item.getNodeType()))
                .toList();
        if (matching.isEmpty()) {
            throw new IllegalStateException("STAGE5_RELEASE_SNAPSHOT_FILE_EVIDENCE_MISSING:" + category);
        }
        return Map.of(
                "nodeType", category,
                "attachmentIds", matching.stream().flatMap(item -> item.getAttachmentIds().stream()).toList(),
                "sha256", matching.stream().flatMap(item -> item.getAttachmentHashes().stream()).toList());
    }

    private MesProEdhrBatchExecutionOriginDO requireSingleOrigin(Long batchExecutionId, String runId) {
        List<MesProEdhrBatchExecutionOriginDO> origins = originMapper.selectListByBatchExecutionId(batchExecutionId);
        if (origins.size() != 1 || !Objects.equals(origins.get(0).getOriginKey(),
                traceOriginKey(runId))) {
            throw new IllegalStateException("STAGE5_RELEASE_SNAPSHOT_TRACE_ORIGIN_INVALID");
        }
        return origins.get(0);
    }

    private Long requireActor(MesStage5FinalReleaseSimulationCommand command) {
        if (command == null || command.getActorUserId() == null || command.getActorUserId() <= 0) {
            throw new IllegalArgumentException("Stage5 simulation actor is required");
        }
        TenantContextHolder.getRequiredTenantId();
        return command.getActorUserId();
    }

    private String normalizeRunId(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (!RUN_ID.matcher(value).matches()) {
            throw new IllegalArgumentException("simulationRunId contains unsupported characters");
        }
        return value;
    }

    private String normalizeOptionalRunId(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        return normalizeRunId(raw);
    }

    private Fixture createFixture(Long actorUserId, String runId) {
        String marker = marker(runId);
        LocalDateTime now = LocalDateTime.now();
        MesProWorkOrderDO workOrder = new MesProWorkOrderDO()
                .setCode("STAGE5-WO-" + shortRunId(runId))
                .setName("Stage5 final release fixture")
                .setType(MesProWorkOrderTypeEnum.SELF.getType())
                .setOrderSourceType(MesProWorkOrderSourceTypeEnum.STORE.getType())
                .setQuantity(java.math.BigDecimal.ONE)
                .setQuantityProduced(java.math.BigDecimal.ONE)
                .setBatchCode("STAGE5-BATCH-" + shortRunId(runId))
                .setParentId(MesProWorkOrderDO.PARENT_ID_NULL)
                .setStatus(MesProWorkOrderStatusEnum.CONFIRMED.getStatus())
                .setRemark(marker);
        if (workOrderMapper.insert(workOrder) != 1 || workOrder.getId() == null) {
            throw new IllegalStateException("STAGE5_WORK_ORDER_FIXTURE_CREATE_FAILED");
        }
        long routeId = 9_000_000_000L + workOrder.getId();
        MesProcessPoolActiveOrderDO activeOrder = new MesProcessPoolActiveOrderDO()
                .setLeaderUserId(actorUserId)
                .setWorkOrderId(workOrder.getId())
                .setRouteId(routeId)
                .setRouteVersionId(routeId + 1)
                .setErpFixedQuantitySnapshot(java.math.BigDecimal.ONE)
                .setActiveStatus("ACTIVE")
                .setBusinessStatus("COMPLETED")
                .setJoinedAt(now)
                .setSortOrder(workOrder.getId())
                .setVersion(1);
        if (activeOrderMapper.insert(activeOrder) != 1 || activeOrder.getId() == null) {
            throw new IllegalStateException("STAGE5_ACTIVE_ORDER_FIXTURE_CREATE_FAILED");
        }
        ErpKingdeeProductionPickListDO pickList = new ErpKingdeeProductionPickListDO()
                .setSourceFormId("STAGE5_SIMULATION")
                .setSourceFid(pickListSourceFid(runId))
                .setSourceBillNo("STAGE5-PL-" + shortRunId(runId))
                .setBillDate(now)
                .setDocumentStatus("C")
                .setDescription("Stage5 final release simulated pick list")
                .setSourceModifyTime(now)
                .setLastSyncTime(now)
                .setRawPayload(JSON.toJSONString(Map.of("simulationRunId", runId, "isSimulation", true)));
        if (pickListMapper.insert(pickList) != 1 || pickList.getId() == null) {
            throw new IllegalStateException("STAGE5_PICK_LIST_FIXTURE_CREATE_FAILED");
        }
        ErpKingdeeProductionPickListItemDO pickListItem = new ErpKingdeeProductionPickListItemDO()
                .setProductionPickListId(pickList.getId())
                .setSourceFormId("STAGE5_SIMULATION")
                .setSourceFid(pickListLineSourceFid(runId))
                .setSourceEntryId(pickListLineSourceEntryId(runId))
                .setSourceLineKey("STAGE5-" + runId + "-LINE-1")
                .setSourceBillNo(pickList.getSourceBillNo())
                .setMaterialNumber("STAGE5-SIM-MATERIAL")
                .setMaterialName("Stage5 simulated material")
                .setUnitName("件")
                .setRequestedQuantity(java.math.BigDecimal.ONE)
                .setActualQuantity(java.math.BigDecimal.ONE)
                .setBaseActualQuantity(java.math.BigDecimal.ONE)
                .setProductionOrderNo(workOrder.getCode())
                .setProductionOrderLineNo(1)
                .setSourceModifyTime(now)
                .setLastSyncTime(now)
                .setRawPayload(JSON.toJSONString(Map.of("simulationRunId", runId, "quantity", 1)));
        if (pickListItemMapper.insert(pickListItem) != 1 || pickListItem.getId() == null) {
            throw new IllegalStateException("STAGE5_PICK_LIST_LINE_FIXTURE_CREATE_FAILED");
        }
        String pickListSnapshotHash = hash(pickList.getId() + "|" + pickList.getSourceFid() + "|"
                + pickList.getSourceBillNo() + "|" + pickList.getDocumentStatus() + "|" + pickListItem.getId());
        MesProcessPoolActiveOrderPickListBindingDO binding = new MesProcessPoolActiveOrderPickListBindingDO()
                .setId(IdUtil.getSnowflake().nextId())
                .setActiveOrderId(activeOrder.getId())
                .setWorkOrderId(workOrder.getId())
                .setPickListId(pickList.getId())
                .setSourceFid(pickListSourceFid(runId))
                .setSourceBillNo(pickList.getSourceBillNo())
                .setSourceDocumentStatus(pickList.getDocumentStatus())
                .setSourceModifyTime(now)
                .setSourceSnapshotHash(pickListSnapshotHash)
                .setBindingStatus("BOUND")
                .setBoundBy(actorUserId)
                .setBoundAt(now)
                .setIdempotencyKey("STAGE5-PICK-BINDING-" + runId)
                .setRequestPayloadHash(hash(marker + ":PICK_BINDING"))
                .setBindingVersion(1);
        binding.setTenantId(TenantContextHolder.getRequiredTenantId());
        if (bindingMapper.insert(binding) != 1 || binding.getId() == null) {
            throw new IllegalStateException("STAGE5_PICK_LIST_BINDING_FIXTURE_CREATE_FAILED");
        }
        MesProcessPoolActiveOrderPickListBindingItemDO bindingItem = MesProcessPoolActiveOrderPickListBindingItemDO.builder()
                .bindingId(binding.getId())
                .pickListItemId(pickListItem.getId())
                .sourceEntryId(pickListItem.getSourceEntryId())
                .sourceLineKey(pickListItem.getSourceLineKey())
                .materialNumber(pickListItem.getMaterialNumber())
                .materialName(pickListItem.getMaterialName())
                .unitName(pickListItem.getUnitName())
                .requestedQuantity(pickListItem.getRequestedQuantity())
                .actualQuantity(pickListItem.getActualQuantity())
                .baseActualQuantity(pickListItem.getBaseActualQuantity())
                .productionOrderNo(workOrder.getCode())
                .productionOrderLineNo(1)
                .sourceModifyTime(now)
                .itemSnapshotHash(hash(String.valueOf(pickListItem.getId())))
                .build();
        bindingItem.setId(IdUtil.getSnowflake().nextId());
        bindingItem.setTenantId(TenantContextHolder.getRequiredTenantId());
        if (bindingItemMapper.insert(bindingItem) != 1 || bindingItem.getId() == null) {
            throw new IllegalStateException("STAGE5_PICK_LIST_BINDING_LINE_FIXTURE_CREATE_FAILED");
        }
        String routeSnapshotJson = buildStage5RouteSnapshot(actorUserId, runId, routeId);
        String sourceSnapshotHash = canonicalHash(routeSnapshotJson);
        MesProEdhrBatchExecutionDO batch = new MesProEdhrBatchExecutionDO()
                .setBatchExecutionCode("STAGE5-SIM-" + shortRunId(runId))
                .setWorkOrderId(workOrder.getId())
                .setWorkOrderCode(workOrder.getCode())
                .setBatchCode(workOrder.getBatchCode())
                .setActiveContextKey(marker)
                .setAttemptNo(1)
                .setProductCode("STAGE5-SIM-PRODUCT")
                .setProductName("Stage5 final release fixture")
                .setRouteId(routeId)
                .setRouteVersionId(routeId + 1)
                .setRouteVersionNo("STAGE5-V1")
                .setRouteCode("STAGE5-SIM-ROUTE")
                .setRouteName("Stage5 final release fixture route")
                .setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_IN_PROGRESS)
                .setTaskTotal(NODES.size())
                .setTaskApprovedCount(0)
                .setBlockedCount(0)
                .setRemark(marker)
                .setRouteSnapshotJson(routeSnapshotJson);
        if (batchExecutionMapper.insert(batch) != 1 || batch.getId() == null) {
            throw new IllegalStateException("STAGE5_BATCH_FIXTURE_CREATE_FAILED");
        }
        for (NodeDefinition node : NODES) {
            MesProEdhrBatchExecutionTaskDO task = new MesProEdhrBatchExecutionTaskDO()
                    .setBatchExecutionId(batch.getId())
                    .setNodeType(node.nodeType())
                    .setRouteProcessId(routeId + node.sort())
                    .setRouteProcessSort(node.sort())
                    .setProcessId(routeId + 500 + node.sort())
                    .setProcessCode("STAGE5-" + node.nodeType())
                    .setProcessName(node.nodeName())
                    .setBatchRecordReportId("STAGE5-" + node.nodeType())
                    .setBatchRecordReportName(node.nodeName())
                    .setBatchRecordSort(1)
                    .setInstanceScope("BATCH_SHARED")
                    .setExecutionMode("SEQUENTIAL")
                    .setRecordCategory("INTERNAL_RECORD")
                    .setValidationProfile("CONTROLLED_BATCH")
                    .setRecordbookEnabled(false)
                    .setRouteBindingSnapshotHash(sourceSnapshotHash)
                    .setRequiredPolicy("REQUIRED")
                    .setOwnerRoleKey("QUALITY")
                    .setArchiveVisibility("FINAL_DHR")
                    .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING)
                    .setRequiredFlag(true)
                    .setSpecialPayloadJson(JSON.toJSONString(Map.of(
                            "stageCode", "STAGE5_FINAL_RELEASE",
                            "simulationRunId", runId,
                            "isSimulation", true,
                            "status", "PENDING_UPLOAD")));
            if (batchTaskMapper.insert(task) != 1 || task.getId() == null) {
                throw new IllegalStateException("STAGE5_BATCH_TASK_FIXTURE_CREATE_FAILED");
            }
        }
        Map<String, MesProEdhrBatchExecutionTaskDO> tasks = new LinkedHashMap<>();
        for (MesProEdhrBatchExecutionTaskDO task : batchTaskMapper.selectListByBatchExecutionId(batch.getId())) {
            tasks.put(task.getNodeType(), task);
        }
        for (NodeDefinition node : NODES) {
            MesProEdhrBatchExecutionTaskDO task = tasks.get(node.nodeType());
            MesProEdhrSpecialNodeAttachmentPrepareUploadResult prepared =
                    batchExecutionService.prepareSpecialNodeAttachmentUpload(
                            new MesProEdhrSpecialNodeAttachmentPrepareUploadCommand()
                                    .setTaskId(task.getId())
                                    .setIdempotencyKey(runId + "-" + node.nodeType())
                                    .setFileName(node.fileName())
                                    .setContentType("application/pdf")
                                    .setContent(("Stage5 dossier fixture run=" + runId
                                            + " node=" + node.nodeType()).getBytes(StandardCharsets.UTF_8)));
            batchExecutionService.completeSpecialNode(task.getId(),
                    "STERILIZATION_REPORT".equals(node.nodeType())
                            ? "STE-STAGE5-" + shortRunId(runId) : null,
                    List.of(toAttachment(prepared)));
            MesProEdhrBatchExecutionTaskDO completed = batchTaskMapper.selectById(task.getId());
            MesProBatchRecordExecutionAttachmentDO attachment = attachmentMapper
                    .selectListByBatchExecutionId(batch.getId()).stream()
                    .filter(item -> Objects.equals(item.getBatchTaskId(), task.getId()))
                    .max(java.util.Comparator.comparing(MesProBatchRecordExecutionAttachmentDO::getId))
                    .orElseThrow(() -> new IllegalStateException("STAGE5_REPORT_ATTACHMENT_MISSING"));
            MesProductionReleaseReportNodeEvidence evidence = new MesProductionReleaseReportNodeEvidence()
                    .setBatchExecutionId(batch.getId())
                    .setBatchTaskId(task.getId())
                    .setNodeType(node.nodeType())
                    .setSterilizationBatchNo("STERILIZATION_REPORT".equals(node.nodeType())
                            ? "STE-STAGE5-" + shortRunId(runId) : null)
                    .setActiveAttachmentVersion(1)
                    .setAttachmentIds(List.of(attachment.getId()))
                    .setAttachmentHashes(List.of(attachment.getSha256()));
            if (completed == null || !Objects.equals(completed.getStatus(),
                    MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED)
                    || batchTaskMapper.updateById(completed.setSpecialPayloadJson(evidence.toPayloadJson())) != 1) {
                throw new IllegalStateException("STAGE5_REPORT_EVIDENCE_FIXTURE_CREATE_FAILED");
            }
        }
        MesProcessPoolActiveOrderCompletionReceiptDO completionReceipt = createAuthoritativeUpstream(
                activeOrder, workOrder, pickList, pickListItem, binding, batch, actorUserId, runId,
                sourceSnapshotHash);
        return new Fixture(workOrder, activeOrder, pickList, pickListItem, binding, batch,
                completionReceipt, sourceSnapshotHash);
    }

    private MesProcessPoolActiveOrderCompletionReceiptDO createAuthoritativeUpstream(
            MesProcessPoolActiveOrderDO activeOrder,
            MesProWorkOrderDO workOrder,
            ErpKingdeeProductionPickListDO pickList,
            ErpKingdeeProductionPickListItemDO pickListItem,
            MesProcessPoolActiveOrderPickListBindingDO binding,
            MesProEdhrBatchExecutionDO batch,
            Long actorUserId,
            String runId,
            String sourceSnapshotHash) {
        List<Long> taskIds = batchTaskMapper.selectListByBatchExecutionId(batch.getId()).stream()
                .map(MesProEdhrBatchExecutionTaskDO::getId).filter(Objects::nonNull).toList();
        if (taskIds.size() != NODES.size()) {
            throw new IllegalStateException("STAGE5_FORMAL_SOURCE_TASKS_INCOMPLETE");
        }
        String requestKey = "STAGE5-COMPLETION-" + runId;
        LocalDateTime completedAt = LocalDateTime.now();
        String batchSourceIds = JSON.toJSONString(List.of(taskIds.get(0), pickList.getId(), pickListItem.getId()));
        String inspectionSourceIds = JSON.toJSONString(List.of(taskIds.get(1), taskIds.get(2), taskIds.get(3)));
        String lossFacts = JSON.toJSONString(List.of(Map.of(
                "processId", taskIds.get(0),
                "status", "NO_LOSS",
                "hasActualLoss", false,
                "lossQuantity", 0,
                "zeroLossConfirmationSnapshot", marker(runId) + ":NO_LOSS",
                "sourceHash", hash(marker(runId) + ":NO_LOSS"))));
        MesProcessPoolActiveOrderCompletionReceiptDO receipt = new MesProcessPoolActiveOrderCompletionReceiptDO()
                .setActiveOrderId(activeOrder.getId())
                .setWorkOrderId(workOrder.getId())
                .setBatchCode(workOrder.getBatchCode())
                .setRouteId(activeOrder.getRouteId())
                .setRouteVersionId(activeOrder.getRouteVersionId())
                .setLeaderUserId(actorUserId)
                .setRequestIdempotencyKey(requestKey)
                .setRequestPayloadHash(hash(marker(runId) + ":COMPLETION_REQUEST"))
                .setSourceSnapshotHash(sourceSnapshotHash)
                .setFormalSourceSnapshotJson(JSON.toJSONString(Map.of(
                        "simulationRunId", runId,
                        "productionSourceIds", JSON.parseArray(batchSourceIds),
                        "processInspectionSourceIds", JSON.parseArray(inspectionSourceIds),
                        "pickListId", pickList.getId(),
                        "pickListBindingId", binding.getId())))
                .setSignatureSnapshotJson(JSON.toJSONString(Map.of(
                        "simulationRunId", runId,
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
                .setLossQuantity(java.math.BigDecimal.ZERO)
                .setZeroLossConfirmationSnapshot(JSON.toJSONString(Map.of(
                        "simulationRunId", runId, "status", "NO_LOSS")))
                .setLossConditionFactsJson(lossFacts)
                .setBatchRecordSourceIdsJson(batchSourceIds)
                .setProcessInspectionSourceIdsJson(inspectionSourceIds)
                .setLossSourceHash(hash(lossFacts))
                .setProvisionHandoff(MesProcessPoolActiveOrderCompletionReceiptDO.PROVISION_HANDOFF_PENDING_FLOW6)
                .setCompletedAt(completedAt)
                .setCompletedBy(actorUserId);
        receipt.setTenantId(TenantContextHolder.getRequiredTenantId());
        receipt.setCreateTime(completedAt);
        receipt.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt));
        if (completionReceiptMapper.insert(receipt) != 1 || receipt.getId() == null) {
            throw new IllegalStateException("STAGE5_COMPLETION_RECEIPT_FIXTURE_CREATE_FAILED");
        }
        MesProcessPoolActiveOrderCompletionBackfillDO batchRecordBackfill = createBackfill(activeOrder, workOrder,
                MesProcessPoolActiveOrderCompletionBackfillDO.TYPE_BATCH_RECORD,
                batchSourceIds, sourceSnapshotHash, JSON.toJSONString(Map.of("simulationRunId", runId,
                        "source", "FORMAL_BATCH_RECORD")), actorUserId);
        MesProcessPoolActiveOrderCompletionBackfillDO processInspectionBackfill = createBackfill(activeOrder, workOrder,
                MesProcessPoolActiveOrderCompletionBackfillDO.TYPE_PROCESS_INSPECTION,
                inspectionSourceIds, sourceSnapshotHash, JSON.toJSONString(Map.of("simulationRunId", runId,
                        "source", "FORMAL_PROCESS_INSPECTION")), actorUserId);
        MesProcessPoolActiveOrderCompletionBackfillDO lossReportBackfill = createBackfill(activeOrder, workOrder,
                MesProcessPoolActiveOrderCompletionBackfillDO.TYPE_LOSS_REPORT,
                "[]", hash(lossFacts), lossFacts, actorUserId);

        List<MesProEdhrBatchTraceSource> sources = new ArrayList<>();
        sources.add(traceSource(MesProEdhrBatchTraceLinkType.ACTIVE_ORDER, "ACTIVE_ORDER", activeOrder.getId(),
                sourceSnapshotHash, JSON.toJSONString(Map.of("simulationRunId", runId, "activeOrderId", activeOrder.getId())), "CAPTURED"));
        sources.add(traceSource(MesProEdhrBatchTraceLinkType.WORK_ORDER, "WORK_ORDER", workOrder.getId(),
                sourceSnapshotHash, JSON.toJSONString(Map.of("simulationRunId", runId, "workOrderId", workOrder.getId())), "CAPTURED"));
        sources.add(traceSource(MesProEdhrBatchTraceLinkType.MATERIAL_ISSUE, "PICK_LIST", pickList.getId(),
                sourceSnapshotHash, batch.getRouteSnapshotJson(), "CAPTURED"));
        sources.add(traceSource(MesProEdhrBatchTraceLinkType.MATERIAL_ISSUE_LINE, "PICK_LIST_LINE", pickListItem.getId(),
                sourceSnapshotHash, batch.getRouteSnapshotJson(), "CAPTURED"));
        sources.add(traceSource(MesProEdhrBatchTraceLinkType.PRODUCTION_SUBMIT, "BATCH_TASK", taskIds.get(0),
                sourceSnapshotHash, JSON.toJSONString(Map.of("simulationRunId", runId, "action", "PRODUCTION_SUBMIT")), "CAPTURED"));
        sources.add(traceSource(MesProEdhrBatchTraceLinkType.PRODUCTION_SIGNATURE, "BATCH_TASK", taskIds.get(0),
                sourceSnapshotHash, JSON.toJSONString(Map.of("simulationRunId", runId, "action", "PRODUCTION_SIGNATURE")), "CAPTURED"));
        sources.add(traceSource(MesProEdhrBatchTraceLinkType.PRODUCTION_LEADER_REVIEW, "BATCH_TASK", taskIds.get(0),
                sourceSnapshotHash, JSON.toJSONString(Map.of("simulationRunId", runId, "action", "PRODUCTION_LEADER_REVIEW")), "CAPTURED"));
        sources.add(traceSource(MesProEdhrBatchTraceLinkType.PQC_TASK, "BATCH_TASK", taskIds.get(1),
                sourceSnapshotHash, JSON.toJSONString(Map.of("simulationRunId", runId, "action", "PQC_TASK")), "CAPTURED"));
        sources.add(traceSource(MesProEdhrBatchTraceLinkType.PQC_SUBMISSION, "BATCH_TASK", taskIds.get(1),
                sourceSnapshotHash, JSON.toJSONString(Map.of("simulationRunId", runId, "action", "PQC_SUBMISSION")), "CAPTURED"));
        sources.add(traceSource(MesProEdhrBatchTraceLinkType.PQC_SIGNATURE, "BATCH_TASK", taskIds.get(1),
                sourceSnapshotHash, JSON.toJSONString(Map.of("simulationRunId", runId, "action", "PQC_SIGNATURE")), "CAPTURED"));
        sources.add(traceSource(MesProEdhrBatchTraceLinkType.PQC_LEADER_CONFIRMATION, "BATCH_TASK", taskIds.get(1),
                sourceSnapshotHash, JSON.toJSONString(Map.of("simulationRunId", runId, "action", "PQC_LEADER_CONFIRMATION")), "CAPTURED"));
        sources.add(traceSource(MesProEdhrBatchTraceLinkType.PQC_AGGREGATE_DETAIL, "BATCH_TASK", taskIds.get(2),
                sourceSnapshotHash, JSON.toJSONString(Map.of("simulationRunId", runId, "action", "PQC_AGGREGATE_DETAIL")), "CAPTURED"));
        sources.add(traceSource(MesProEdhrBatchTraceLinkType.BATCH_RECORD_RECEIPT, "BACKFILL", batchRecordBackfill.getId(),
                sourceSnapshotHash, JSON.toJSONString(Map.of("simulationRunId", runId, "backfill", "BATCH_RECORD")), "CAPTURED"));
        sources.add(traceSource(MesProEdhrBatchTraceLinkType.PROCESS_INSPECTION_RECEIPT, "BACKFILL", processInspectionBackfill.getId(),
                sourceSnapshotHash, JSON.toJSONString(Map.of("simulationRunId", runId, "backfill", "PROCESS_INSPECTION")), "CAPTURED"));
        sources.add(traceSource(MesProEdhrBatchTraceLinkType.COMPLETION_BACKFILL_RECEIPT, "COMPLETION_RECEIPT",
                receipt.getId(), receipt.getReceiptHash(),
                MesTeamLeaderActiveOrderCompletionReceiptHash.snapshotJson(receipt), "CAPTURED"));
        sources.add(traceSource(MesProEdhrBatchTraceLinkType.BATCH_PROVISION_RECEIPT, "BATCH_EXECUTION", batch.getId(),
                sourceSnapshotHash, batch.getRouteSnapshotJson(), "CAPTURED"));
        sources.add(traceSource(MesProEdhrBatchTraceLinkType.NO_LOSS_CONFIRMED, "COMPLETION_RECEIPT", receipt.getId(),
                hash(lossFacts), lossFacts, "NO_LOSS"));
        MesProEdhrBatchTraceabilityValidator validator = new MesProEdhrBatchTraceabilityValidator();
        MesProEdhrBatchTraceCaptureCommand traceCommand = new MesProEdhrBatchTraceCaptureCommand()
                .setBatchExecutionId(batch.getId())
                .setEntryType(MesProEdhrBatchTraceEntryType.ACTIVE_ORDER_COMPLETION)
                .setOriginKey(traceOriginKey(runId))
                .setActiveOrderId(activeOrder.getId())
                .setWorkOrderId(workOrder.getId())
                .setCompletionTransactionId(receipt.getId())
                .setCompletionVersion(receipt.getCompletedVersion())
                .setCompletionBackfillReceiptId(receipt.getId())
                .setCompletionBackfillReceiptHash(receipt.getReceiptHash())
                .setPickListBindingId(binding.getId())
                .setPickListId(pickList.getId())
                .setPickListBindingVersion(binding.getBindingVersion())
                .setSourceSnapshotHash(sourceSnapshotHash)
                .setBatchProvisionReceiptId(batch.getId())
                .setBatchProvisionStatus("CREATED")
                .setSourceBundleHash(validator.calculateSourceBundleHash(sources))
                .setIdempotencyKey(marker(runId) + ":TRACE_CAPTURE")
                .setHasActualLoss(false)
                .setCapturedBy(actorUserId)
                .setSources(sources);
        var validation = validator.validate(traceCommand);
        if (!validation.valid()) {
            throw new IllegalStateException("STAGE5_TRACE_SOURCE_INVALID:"
                    + validation.blockerCode() + ":" + validation.blockerScope());
        }
        traceabilityService.capture(traceCommand);
        return receipt;
    }

    private MesProcessPoolActiveOrderCompletionBackfillDO createBackfill(MesProcessPoolActiveOrderDO activeOrder,
                                MesProWorkOrderDO workOrder,
                                String type,
                                String sourceIds,
                                String sourceHash,
                                String payload,
                                Long actorUserId) {
        MesProcessPoolActiveOrderCompletionBackfillDO backfill = MesProcessPoolActiveOrderCompletionBackfillDO.builder()
                .activeOrderId(activeOrder.getId())
                .workOrderId(workOrder.getId())
                .backfillType(type)
                .status("SUCCESS")
                .sourceIdsJson(sourceIds)
                .sourceSnapshotHash(sourceHash)
                .payloadJson(payload)
                .materializedAt(LocalDateTime.now())
                .materializedBy(actorUserId)
                .build();
        backfill.setTenantId(TenantContextHolder.getRequiredTenantId());
        if (completionBackfillMapper.insert(backfill) != 1 || backfill.getId() == null) {
            throw new IllegalStateException("STAGE5_COMPLETION_BACKFILL_FIXTURE_CREATE_FAILED:" + type);
        }
        return backfill;
    }

    private MesProEdhrBatchTraceSource traceSource(String linkType, String objectType, Long objectId,
                                                   String snapshotHash, String snapshotJson,
                                                   String relationStatus) {
        String calculatedHash = MesProEdhrBatchTraceSourceHash.calculate(linkType, snapshotJson);
        if (!Objects.equals(calculatedHash, snapshotHash)
                && (MesProEdhrBatchTraceLinkType.MATERIAL_ISSUE.equals(linkType)
                || MesProEdhrBatchTraceLinkType.MATERIAL_ISSUE_LINE.equals(linkType)
                || MesProEdhrBatchTraceLinkType.BATCH_PROVISION_RECEIPT.equals(linkType)
                || MesProEdhrBatchTraceLinkType.COMPLETION_BACKFILL_RECEIPT.equals(linkType))) {
            throw new IllegalStateException("STAGE5_TRACE_SOURCE_SNAPSHOT_HASH_INVALID:" + linkType);
        }
        return new MesProEdhrBatchTraceSource()
                .setLinkType(linkType)
                .setSourceObjectType(objectType)
                .setSourceObjectId(objectId)
                .setSourceVersion(1)
                .setSnapshotJson(snapshotJson)
                .setSnapshotHash(calculatedHash)
                .setRelationStatus(relationStatus)
                .setRelationReason("STAGE5_FORMAL_SIMULATION_FIXTURE");
    }

    private MesProcessPoolActiveOrderReleaseApplicationDO createApplication(Fixture fixture, Long actorUserId,
                                                                              Map<String, Object> dossier,
                                                                              String runId) {
        LocalDateTime now = LocalDateTime.now();
        String sourceSnapshotHash = fixture.sourceSnapshotHash();
        MesProcessPoolActiveOrderReleaseApplicationDO application =
                new MesProcessPoolActiveOrderReleaseApplicationDO()
                        .setActiveOrderId(fixture.activeOrder().getId())
                        .setWorkOrderId(fixture.workOrder().getId())
                        .setWorkOrderCode(fixture.workOrder().getCode())
                        .setRouteId(fixture.batch().getRouteId())
                        .setRouteVersionId(fixture.batch().getRouteVersionId())
                        .setBatchCode(fixture.batch().getBatchCode())
                        .setBatchExecutionId(fixture.batch().getId())
                        .setApplicationStatus("REPORT_UPLOAD_PENDING")
                        .setSourceSnapshotHash(sourceSnapshotHash)
                        .setRequestIdempotencyKey("STAGE5-REQUEST-" + runId)
                        .setBusinessIdempotencyKey("STAGE5-BUSINESS-" + runId)
                        .setBlockerSnapshotJson(JSON.toJSONString(List.of(
                                "AUTHORITATIVE_UPSTREAM_CONTEXT_REQUIRED")))
                        .setDossierSummaryJson(JSON.toJSONString(dossier))
                        .setAppliedBy(actorUserId)
                        .setAppliedAt(now)
                        .setLastPrecheckAt(now)
                        .setVersion(1)
                        .setRemark(marker(runId));
        if (applicationMapper.insert(application) != 1 || application.getId() == null) {
            throw new IllegalStateException("STAGE5_RELEASE_APPLICATION_CREATE_FAILED");
        }
        return application;
    }

    private List<MesProductionReleaseReportNodeEvidence> buildReportEvidences(Long batchExecutionId) {
        Map<String, MesProEdhrBatchExecutionTaskDO> tasks = new LinkedHashMap<>();
        for (MesProEdhrBatchExecutionTaskDO task : batchTaskMapper.selectListByBatchExecutionId(batchExecutionId)) {
            tasks.put(task.getNodeType(), task);
        }
        Map<String, MesProBatchRecordExecutionAttachmentDO> latest = new LinkedHashMap<>();
        for (MesProBatchRecordExecutionAttachmentDO attachment : attachmentMapper
                .selectListByBatchExecutionId(batchExecutionId)) {
            MesProBatchRecordExecutionAttachmentDO current = latest.get(attachment.getFieldKey());
            if (current == null || attachment.getId() > current.getId()) {
                latest.put(attachment.getFieldKey(), attachment);
            }
        }
        return NODES.stream().map(node -> {
            MesProBatchRecordExecutionAttachmentDO attachment = latest.get(node.nodeType());
            if (attachment == null || tasks.get(node.nodeType()) == null
                    || tasks.get(node.nodeType()).getStatus() != MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED) {
                throw new IllegalStateException("STAGE5_REPORT_EVIDENCE_INCOMPLETE");
            }
            return new MesProductionReleaseReportNodeEvidence()
                    .setBatchExecutionId(batchExecutionId)
                    .setBatchTaskId(tasks.get(node.nodeType()).getId())
                    .setNodeType(node.nodeType())
                    .setSterilizationBatchNo("STERILIZATION_REPORT".equals(node.nodeType())
                            ? "STE-STAGE5-" + batchExecutionId : null)
                    .setActiveAttachmentVersion(attachment.getVersionNo())
                    .setAttachmentIds(List.of(attachment.getId()))
                    .setAttachmentHashes(List.of(attachment.getSha256()));
        }).toList();
    }

    private Map<String, Object> buildDossierSnapshot(MesProEdhrBatchExecutionDO batch, String runId) {
        Map<String, MesProBatchRecordExecutionAttachmentDO> latest = new LinkedHashMap<>();
        for (MesProBatchRecordExecutionAttachmentDO attachment : attachmentMapper
                .selectListByBatchExecutionId(batch.getId())) {
            if (MesStage5FinalReleaseSimulationContractValidator.REQUIRED_NODE_TYPES.contains(attachment.getFieldKey())) {
                latest.put(attachment.getFieldKey(), attachment);
            }
        }
        if (latest.size() != NODES.size()) {
            throw new IllegalStateException("STAGE5_DOSSIER_ATTACHMENT_INCOMPLETE");
        }
        Map<String, Object> hashes = new LinkedHashMap<>();
        hashes.put("incomingInspectionAttachmentHash", latest.get("INCOMING_INSPECTION_REPORT").getSha256());
        hashes.put("sterilizationAttachmentHash", latest.get("STERILIZATION_REPORT").getSha256());
        hashes.put("finishedProductInspectionAttachmentHashes", List.of(
                latest.get("FINISHED_PRODUCT_INSPECTION_REPORT").getSha256(),
                latest.get("FINISHED_PRODUCT_INSPECTION_RECORD").getSha256()));
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("contractName", MesStage5FinalReleaseSimulationContractValidator.CONTRACT_NAME);
        snapshot.put("contractVersion", MesStage5FinalReleaseSimulationContractValidator.CONTRACT_VERSION);
        snapshot.put("simulationRunId", runId);
        snapshot.put("batchExecutionId", String.valueOf(batch.getId()));
        snapshot.put("incomingInspectionAttachmentId", String.valueOf(latest.get("INCOMING_INSPECTION_REPORT").getId()));
        snapshot.put("sterilizationAttachmentId", String.valueOf(latest.get("STERILIZATION_REPORT").getId()));
        snapshot.put("finishedProductInspectionAttachmentIds", List.of(
                String.valueOf(latest.get("FINISHED_PRODUCT_INSPECTION_REPORT").getId()),
                String.valueOf(latest.get("FINISHED_PRODUCT_INSPECTION_RECORD").getId())));
        snapshot.put("hashes", hashes);
        Map<String, Object> nodeStatuses = new LinkedHashMap<>();
        Map<String, Object> fileIds = new LinkedHashMap<>();
        Map<String, Object> fileNames = new LinkedHashMap<>();
        Map<String, Object> storagePaths = new LinkedHashMap<>();
        Map<String, Object> audits = new LinkedHashMap<>();
        for (NodeDefinition node : NODES) {
            MesProBatchRecordExecutionAttachmentDO attachment = latest.get(node.nodeType());
            nodeStatuses.put(node.nodeType(), "COMPLETED");
            fileIds.put(node.nodeType(), String.valueOf(attachment.getFileId()));
            fileNames.put(node.nodeType(), attachment.getFileName());
            storagePaths.put(node.nodeType(), attachment.getStoragePath());
            audits.put(node.nodeType(), toAttachmentAudit(attachment));
        }
        snapshot.put("nodeStatuses", nodeStatuses);
        snapshot.put("fileId", fileIds);
        snapshot.put("fileName", fileNames);
        snapshot.put("storagePath", storagePaths);
        snapshot.put("attachmentAudit", audits);
        snapshot.put("sterilizationBatchNo", "STE-STAGE5-" + shortRunId(runId));
        snapshot.put("reportSnapshotHash", hash(JSON.toJSONString(snapshot)));
        snapshot.put("dossierReadyForRelease", true);
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
        audit.put("verificationStatus", "VALID");
        return audit;
    }

    private void markOwnedManagerRows(MesProductionReleaseManagerStageInitializationResult managerStage, String runId) {
        MesProEdhrReleaseTransactionDO transaction = releaseTransactionMapper
                .selectById(managerStage.getReleaseTransactionId());
        if (transaction == null || !Objects.equals(transaction.getReleaseStatus(),
                MesStage5FinalReleaseSimulationContractValidator.PENDING_RELEASE_STATUS)) {
            throw new IllegalStateException("STAGE5_RELEASE_TRANSACTION_CONTEXT_INVALID");
        }
        transaction.setRemark(marker(runId));
        releaseTransactionMapper.updateById(transaction);
        MesProEdhrWorkTaskDO workTask = workTaskMapper.selectById(managerStage.getManagerReleaseWorkTaskId());
        if (workTask == null || !Objects.equals(workTask.getTaskType(), "RELEASE_APPROVE")
                || !Objects.equals(workTask.getStatus(), "TODO")) {
            throw new IllegalStateException("STAGE5_MANAGER_WORK_TASK_CONTEXT_INVALID");
        }
        workTask.setRemark(marker(runId));
        workTaskMapper.updateById(workTask);
    }

    private String createManagerSimulationSignature(Long managerWorkTaskId, Long actorUserId, String runId) {
        String signatureImageFileUrl = "stage5://manager-signature/" + runId;
        String signoffEvidenceHash = DigestUtil.sha256Hex(signatureImageFileUrl);
        BpmApprovalSignatureRecordDO signature = new BpmApprovalSignatureRecordDO()
                .setModuleCode("EDHR")
                .setSourceTaskType("EDHR_WORK_TASK")
                .setSourceTaskId(String.valueOf(managerWorkTaskId))
                .setBusinessKey("STAGE5-SIGNATURE-" + runId)
                .setProcessInstanceId("STAGE5-RELEASE-" + runId)
                .setSignerUserId(actorUserId)
                .setReviewResult("APPROVE")
                .setReason("Stage5 final release simulation manager approval")
                .setPasswordVerified(Boolean.TRUE)
                .setSignatureImageFileUrl(signatureImageFileUrl)
                .setSignatureImageSha256(signoffEvidenceHash)
                .setSignatureImageContentType("image/png")
                .setSignatureImageStatusSnapshot("SIMULATION")
                .setSignatureImageVerifiedStatus("VERIFIED")
                .setSignedAt(LocalDateTime.now());
        signature.setTenantId(TenantContextHolder.getRequiredTenantId());
        if (approvalSignatureRecordMapper.insert(signature) != 1 || signature.getId() == null) {
            throw new IllegalStateException("STAGE5_MANAGER_SIGNATURE_FIXTURE_CREATE_FAILED");
        }
        return signoffEvidenceHash;
    }

    private String cleanupPreviousSimulation(String previousRunId) {
        MesProEdhrBatchExecutionDO latest = batchExecutionMapper.selectLatestStage5Simulation();
        if (previousRunId == null) {
            if (latest != null) {
                throw new IllegalStateException("STAGE5_PREVIOUS_SIMULATION_RUN_ID_REQUIRED");
            }
            return null;
        }
        MesProEdhrBatchExecutionDO previous = batchExecutionMapper
                .selectStage5SimulationByRemark(marker(previousRunId));
        if (previous == null) {
            throw new IllegalStateException("STAGE5_PREVIOUS_SIMULATION_NOT_FOUND");
        }
        String runId = runIdFromMarker(previous.getRemark());
        List<MesProEdhrBatchExecutionTaskDO> tasks = batchTaskMapper.selectListByBatchExecutionId(previous.getId());
        if (tasks.size() != NODES.size()
                || !Set.copyOf(tasks.stream().map(MesProEdhrBatchExecutionTaskDO::getNodeType).toList())
                .equals(MesStage5FinalReleaseSimulationContractValidator.REQUIRED_NODE_TYPES)) {
            throw new IllegalStateException("STAGE5_SIMULATION_CLEANUP_SCOPE_INVALID");
        }
        MesProcessPoolActiveOrderReleaseApplicationDO application = applicationMapper
                .selectByBatchExecutionIdForUpdate(previous.getId());
        if (application == null || !Objects.equals(application.getRemark(), marker(runId))) {
            throw new IllegalStateException("STAGE5_SIMULATION_CLEANUP_SCOPE_INVALID");
        }
        MesProEdhrReleaseTransactionDO transaction = application.getReleaseTransactionId() == null ? null
                : releaseTransactionMapper.selectById(application.getReleaseTransactionId());
        MesProEdhrWorkTaskDO managerTask = application.getReleaseApprovalWorkTaskId() == null ? null
                : workTaskMapper.selectById(application.getReleaseApprovalWorkTaskId());
        if (transaction == null || !Objects.equals(transaction.getRemark(), marker(runId))
                || managerTask == null || !Objects.equals(managerTask.getRemark(), marker(runId))) {
            throw new IllegalStateException("STAGE5_SIMULATION_CLEANUP_SCOPE_INVALID");
        }
        List<BpmApprovalSignatureRecordDO> signatures = approvalSignatureRecordMapper.selectList(
                new LambdaQueryWrapper<BpmApprovalSignatureRecordDO>()
                        .eq(BpmApprovalSignatureRecordDO::getSourceTaskType, "EDHR_WORK_TASK")
                        .eq(BpmApprovalSignatureRecordDO::getSourceTaskId, String.valueOf(managerTask.getId())));
        String expectedSignatureUrl = "stage5://manager-signature/" + runId;
        if (signatures.size() != 1
                || !Objects.equals(signatures.get(0).getModuleCode(), "EDHR")
                || !Objects.equals(signatures.get(0).getBusinessKey(), "STAGE5-SIGNATURE-" + runId)
                || signatures.get(0).getSignerUserId() == null
                || !Objects.equals(signatures.get(0).getReviewResult(), "APPROVE")
                || !Boolean.TRUE.equals(signatures.get(0).getPasswordVerified())
                || !Objects.equals(signatures.get(0).getSignatureImageFileUrl(), expectedSignatureUrl)
                || !Objects.equals(signatures.get(0).getSignatureImageSha256(), DigestUtil.sha256Hex(expectedSignatureUrl))) {
            throw new IllegalStateException("STAGE5_SIMULATION_CLEANUP_SCOPE_INVALID");
        }
        List<MesProBatchRecordExecutionAttachmentDO> attachments = attachmentMapper
                .selectListByBatchExecutionId(previous.getId());
        Set<Long> taskIds = tasks.stream().map(MesProEdhrBatchExecutionTaskDO::getId).collect(java.util.stream.Collectors.toSet());
        if (attachments.stream().anyMatch(item -> !Objects.equals(item.getBatchExecutionId(), previous.getId())
                || !taskIds.contains(item.getBatchTaskId())
                || !MesStage5FinalReleaseSimulationContractValidator.REQUIRED_NODE_TYPES.contains(item.getFieldKey()))) {
            throw new IllegalStateException("STAGE5_SIMULATION_CLEANUP_SCOPE_INVALID");
        }
        List<MesProEdhrBatchExecutionOriginDO> origins = originMapper
                .selectListByBatchExecutionId(previous.getId());
        if (origins.size() != 1 || !Objects.equals(origins.get(0).getOriginKey(),
                traceOriginKey(runId))) {
            throw new IllegalStateException("STAGE5_SIMULATION_CLEANUP_SCOPE_INVALID");
        }
        List<MesProEdhrBatchExecutionTraceLinkDO> traceLinks = traceLinkMapper
                .selectListByBatchExecutionId(previous.getId());
        if (traceLinks.isEmpty() || traceLinks.stream().anyMatch(item -> !Objects.equals(item.getBatchExecutionId(), previous.getId())
                || !Objects.equals(item.getOriginId(), origins.get(0).getId()))) {
            throw new IllegalStateException("STAGE5_SIMULATION_CLEANUP_SCOPE_INVALID");
        }
        List<MesProEdhrBatchExecutionTraceManifestDO> traceManifests = traceManifestMapper
                .selectListByBatchExecutionId(previous.getId());
        if (traceManifests.isEmpty() || traceManifests.stream()
                .anyMatch(item -> !Objects.equals(item.getBatchExecutionId(), previous.getId()))) {
            throw new IllegalStateException("STAGE5_SIMULATION_CLEANUP_SCOPE_INVALID");
        }
        MesProcessPoolActiveOrderDO activeOrder = activeOrderMapper.selectByIdForUpdate(application.getActiveOrderId());
        if (activeOrder == null || !Objects.equals(activeOrder.getWorkOrderId(), previous.getWorkOrderId())) {
            throw new IllegalStateException("STAGE5_SIMULATION_CLEANUP_SCOPE_INVALID");
        }
        MesProcessPoolActiveOrderCompletionReceiptDO completionReceipt = completionReceiptMapper
                .selectByActiveOrderIdForUpdate(activeOrder.getId());
        List<MesProcessPoolActiveOrderCompletionBackfillDO> backfills = completionBackfillMapper
                .selectListByActiveOrderIdForUpdate(activeOrder.getId());
        Set<String> requiredBackfillTypes = Set.of(
                MesProcessPoolActiveOrderCompletionBackfillDO.TYPE_BATCH_RECORD,
                MesProcessPoolActiveOrderCompletionBackfillDO.TYPE_PROCESS_INSPECTION,
                MesProcessPoolActiveOrderCompletionBackfillDO.TYPE_LOSS_REPORT);
        if (completionReceipt == null || !Objects.equals(completionReceipt.getWorkOrderId(), previous.getWorkOrderId())
                || !Objects.equals(completionReceipt.getRequestIdempotencyKey(), "STAGE5-COMPLETION-" + runId)
                || backfills.size() != requiredBackfillTypes.size()
                || backfills.stream().anyMatch(item -> !Objects.equals(item.getActiveOrderId(), activeOrder.getId())
                || !Objects.equals(item.getWorkOrderId(), previous.getWorkOrderId())
                || item.getBackfillType() == null)
                || !Set.copyOf(backfills.stream()
                .map(MesProcessPoolActiveOrderCompletionBackfillDO::getBackfillType).toList())
                .equals(requiredBackfillTypes)) {
            throw new IllegalStateException("STAGE5_SIMULATION_CLEANUP_SCOPE_INVALID");
        }
        MesProcessPoolActiveOrderPickListBindingDO binding = bindingMapper.selectByActiveOrderId(activeOrder.getId());
        if (binding == null || !Objects.equals(binding.getWorkOrderId(), previous.getWorkOrderId())
                || !Objects.equals(binding.getSourceFid(), pickListSourceFid(runId))
                || !Objects.equals(binding.getIdempotencyKey(), "STAGE5-PICK-BINDING-" + runId)) {
            throw new IllegalStateException("STAGE5_SIMULATION_CLEANUP_SCOPE_INVALID");
        }
        ErpKingdeeProductionPickListDO pickList = pickListMapper.selectById(binding.getPickListId());
        if (pickList == null || !Objects.equals(pickList.getSourceFid(), pickListSourceFid(runId))) {
            throw new IllegalStateException("STAGE5_SIMULATION_CLEANUP_SCOPE_INVALID");
        }
        List<ErpKingdeeProductionPickListItemDO> pickListItems = pickListItemMapper
                .selectListByPickListIds(List.of(pickList.getId()));
        if (pickListItems.isEmpty() || pickListItems.stream().anyMatch(item -> item.getSourceFid() == null
                || !Objects.equals(item.getSourceFid(), pickListLineSourceFid(runId)))) {
            throw new IllegalStateException("STAGE5_SIMULATION_CLEANUP_SCOPE_INVALID");
        }
        Set<Long> fileIds = attachments.stream().map(MesProBatchRecordExecutionAttachmentDO::getFileId)
                .filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        if (!fileIds.isEmpty()) {
            try {
                fileService.deleteFileList(new ArrayList<>(fileIds));
            } catch (Exception exception) {
                throw new IllegalStateException("STAGE5_SIMULATION_FILE_CLEANUP_FAILED", exception);
            }
        }
        attachments.forEach(item -> attachmentMapper.deleteById(item.getId()));
        tasks.forEach(item -> batchTaskMapper.deleteById(item.getId()));
        // Trace origin, links and manifests are immutable audit evidence; retain them during simulation cleanup.
        backfills.forEach(item -> completionBackfillMapper.deleteById(item.getId()));
        completionReceiptMapper.deleteById(completionReceipt.getId());
        bindingItemMapper.delete(new LambdaQueryWrapper<MesProcessPoolActiveOrderPickListBindingItemDO>()
                .eq(MesProcessPoolActiveOrderPickListBindingItemDO::getBindingId, binding.getId()));
        bindingMapper.deleteById(binding.getId());
        pickListItemMapper.delete(new LambdaQueryWrapper<ErpKingdeeProductionPickListItemDO>()
                .eq(ErpKingdeeProductionPickListItemDO::getProductionPickListId, pickList.getId()));
        if (pickListMapper.hardDeleteById(pickList.getId()) != 1) {
            throw new IllegalStateException("STAGE5_SIMULATION_PICK_LIST_CLEANUP_FAILED");
        }
        releaseEventMapper.delete(new LambdaQueryWrapper<MesProEdhrReleaseTransactionEventDO>()
                .eq(MesProEdhrReleaseTransactionEventDO::getReleaseTransactionId, transaction.getId()));
        releaseDecisionMapper.delete(new LambdaQueryWrapper<MesProEdhrReleaseDecisionDO>()
                .eq(MesProEdhrReleaseDecisionDO::getReleaseTransactionId, transaction.getId()));
        signatures.forEach(item -> approvalSignatureRecordMapper.deleteById(item.getId()));
        workTaskMapper.deleteById(managerTask.getId());
        releaseTransactionMapper.deleteById(transaction.getId());
        applicationMapper.deleteById(application.getId());
        activeOrderMapper.deleteById(activeOrder.getId());
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(previous.getWorkOrderId());
        if (workOrder == null || !Objects.equals(workOrder.getRemark(), marker(runId))) {
            throw new IllegalStateException("STAGE5_SIMULATION_CLEANUP_SCOPE_INVALID");
        }
        workOrderMapper.deleteById(workOrder.getId());
        batchExecutionMapper.deleteById(previous.getId());
        return runId;
    }

    private String runIdFromMarker(String marker) {
        String prefix = SIMULATION_MARKER + "[simulationRunId=";
        if (marker == null || !marker.startsWith(prefix) || !marker.endsWith("]")) {
            throw new IllegalStateException("STAGE5_SIMULATION_CLEANUP_SCOPE_INVALID");
        }
        String runId = marker.substring(prefix.length(), marker.length() - 1);
        if (!RUN_ID.matcher(runId).matches()) {
            throw new IllegalStateException("STAGE5_SIMULATION_CLEANUP_SCOPE_INVALID");
        }
        return runId;
    }

    private String marker(String runId) {
        return SIMULATION_MARKER + "[simulationRunId=" + runId + "]";
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

    private String shortRunId(String runId) {
        String value = runId.replaceAll("[^A-Za-z0-9]", "");
        return value.length() <= 24 ? value : value.substring(0, 24);
    }

    private String pickListSourceFid(String runId) {
        return "STAGE5-" + shortRunId(runId) + "-PICK";
    }

    private String pickListLineSourceFid(String runId) {
        return pickListSourceFid(runId) + "-LINE";
    }

    private String pickListLineSourceEntryId(String runId) {
        return "STAGE5-" + shortRunId(runId) + "-E1";
    }

    private String traceOriginKey(String runId) {
        return "STAGE5:" + shortRunId(runId) + ":ACTIVE_ORDER_COMPLETION";
    }

    private String buildStage5RouteSnapshot(Long actorUserId, String runId, Long routeId) {
        List<Map<String, Object>> owners = new ArrayList<>();
        for (NodeDefinition node : NODES) {
            Map<String, Object> owner = new LinkedHashMap<>();
            owner.put("attachmentCode", node.nodeType());
            owner.put("attachmentName", node.nodeName());
            owner.put("candidateSourceType", "USER");
            owner.put("candidateSourceIds", List.of(actorUserId));
            owner.put("candidateSourceNames", List.of(String.valueOf(actorUserId)));
            owners.add(owner);
        }
        Map<String, Object> configSnapshots = new LinkedHashMap<>();
        configSnapshots.put("batchRecordAttachmentOwners", owners);

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("schemaVersion", "stage5.routeSnapshot.v1");
        snapshot.put("stageCode", "STAGE5_FINAL_RELEASE");
        snapshot.put("simulationRunId", runId);
        snapshot.put("isSimulation", true);
        snapshot.put("routeId", routeId);
        snapshot.put("configSnapshots", configSnapshots);
        return JSON.toJSONString(snapshot);
    }

    private String hash(Object value) {
        return DigestUtil.sha256Hex(JSON.toJSONString(value));
    }

    private String canonicalHash(String json) {
        return DigestUtil.sha256Hex(MesProBatchRecordExecutionFieldAuditHasher.canonicalizeJsonString(json));
    }

    private record Fixture(MesProWorkOrderDO workOrder,
                           MesProcessPoolActiveOrderDO activeOrder,
                           ErpKingdeeProductionPickListDO pickList,
                           ErpKingdeeProductionPickListItemDO pickListItem,
                           MesProcessPoolActiveOrderPickListBindingDO binding,
                           MesProEdhrBatchExecutionDO batch,
                           MesProcessPoolActiveOrderCompletionReceiptDO completionReceipt,
                           String sourceSnapshotHash) {
    }

    private record NodeDefinition(String nodeType, String nodeName, int sort, String fileName) {
    }
}
