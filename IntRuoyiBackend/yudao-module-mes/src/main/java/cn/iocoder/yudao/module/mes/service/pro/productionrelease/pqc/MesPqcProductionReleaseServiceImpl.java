package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowAuditCommand;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowAuditEventType;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowAuditRecorder;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlocker;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerException;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerType;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowFailureRespVO;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowIdempotency;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStage;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStatus;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseFormalFactSnapshots;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.report.MesProductionReleaseManagerStageInitializationCommand;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.report.MesProductionReleaseManagerStageInitializationResult;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.report.MesProductionReleaseManagerStageInitializer;
import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class MesPqcProductionReleaseServiceImpl implements MesPqcProductionReleaseService {

    private static final String TASK_TYPE_PQC_RELEASE = "PQC_PRODUCTION_RELEASE";
    private static final String BUSINESS_SCOPE_RELEASE_APPLICATION = "RELEASE_APPLICATION";
    private static final String VIEW_STATUS_PENDING = "PENDING";
    private static final String VIEW_STATUS_RELEASED = "RELEASED";
    private static final String VIEW_STATUS_VOIDED = "VOIDED";
    private static final String VIEW_STATUS_REWORKED = "REWORKED";
    private static final String VIEW_STATUS_CONCESSION_RELEASED = "CONCESSION_RELEASED";
    private static final Set<String> ACTIVE_TASK_STATUSES = Set.of(
            MesProEdhrWorkTaskStatus.TODO,
            MesProEdhrWorkTaskStatus.DOING,
            MesProEdhrWorkTaskStatus.OVERDUE);
    private static final Set<String> REQUIRED_REPORT_NODE_TYPES = Set.of(
            "INCOMING_INSPECTION_REPORT",
            "STERILIZATION_REPORT",
            "FINISHED_PRODUCT_INSPECTION_REPORT",
            "FINISHED_PRODUCT_INSPECTION_RECORD");

    private final MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper;
    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesProEdhrWorkTaskMapper workTaskMapper;
    private final MesPqcReleaseDossierPort dossierPort;
    private final MesProductionReleaseBatchExecutionPort batchExecutionPort;
    private final MesProductionReleaseReportStageInitializer reportStageInitializer;
    private final MesProductionReleaseManagerStageInitializer managerStageInitializer;
    private final MesReleaseFlowAuditRecorder auditRecorder;
    private final MesProBatchRecordExecutionSignatureService signatureService;
    private final MesProEdhrNonconformanceReviewService nonconformanceReviewService;
    private final MesProEdhrNonconformanceReviewMapper nonconformanceReviewMapper;
    private final Clock clock;

    @Autowired
    public MesPqcProductionReleaseServiceImpl(
            MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper,
            MesProcessPoolActiveOrderMapper activeOrderMapper,
            MesProEdhrWorkTaskMapper workTaskMapper,
            MesPqcReleaseDossierPort dossierPort,
            MesProductionReleaseBatchExecutionPort batchExecutionPort,
            MesProductionReleaseReportStageInitializer reportStageInitializer,
            MesProductionReleaseManagerStageInitializer managerStageInitializer,
            MesReleaseFlowAuditRecorder auditRecorder,
            MesProBatchRecordExecutionSignatureService signatureService,
            MesProEdhrNonconformanceReviewService nonconformanceReviewService,
            MesProEdhrNonconformanceReviewMapper nonconformanceReviewMapper) {
        this(applicationMapper, activeOrderMapper, workTaskMapper, dossierPort, batchExecutionPort,
                reportStageInitializer, managerStageInitializer, auditRecorder, signatureService, nonconformanceReviewService,
                nonconformanceReviewMapper, Clock.systemUTC());
    }

    public MesPqcProductionReleaseServiceImpl(
            MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper,
            MesProcessPoolActiveOrderMapper activeOrderMapper,
            MesProEdhrWorkTaskMapper workTaskMapper,
            MesPqcReleaseDossierPort dossierPort,
            MesProductionReleaseBatchExecutionPort batchExecutionPort,
            MesProductionReleaseReportStageInitializer reportStageInitializer,
            MesProductionReleaseManagerStageInitializer managerStageInitializer,
            MesReleaseFlowAuditRecorder auditRecorder,
            MesProBatchRecordExecutionSignatureService signatureService,
            MesProEdhrNonconformanceReviewService nonconformanceReviewService,
            MesProEdhrNonconformanceReviewMapper nonconformanceReviewMapper,
            Clock clock) {
        this.applicationMapper = applicationMapper;
        this.activeOrderMapper = activeOrderMapper;
        this.workTaskMapper = workTaskMapper;
        this.dossierPort = dossierPort;
        this.batchExecutionPort = batchExecutionPort;
        this.reportStageInitializer = reportStageInitializer;
        this.managerStageInitializer = managerStageInitializer;
        this.auditRecorder = auditRecorder;
        this.signatureService = signatureService;
        this.nonconformanceReviewService = nonconformanceReviewService;
        this.nonconformanceReviewMapper = nonconformanceReviewMapper;
        this.clock = clock;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesPqcProductionReleaseDecisionResult approve(
            Long actorUserId, MesPqcProductionReleaseApproveCommand command) {
        requireApproveCommand(actorUserId, command);
        String idempotencyKey = MesReleaseFlowIdempotency.requireKey(command.getIdempotencyKey());
        String opinion = trimAndValidateOptionalText(command.getApprovalOpinion(), "approvalOpinion");
        String udiControlDocumentNo = requireUdiControlDocumentNo(command.getUdiControlDocumentNo());
        MesProcessPoolActiveOrderReleaseApplicationDO application = requireApplicationForUpdate(command.getApplicationId());
        String payloadHash = decisionPayloadHash("APPROVE", command.getApplicationId(),
                command.getPqcReleaseWorkTaskId(), command.getExpectedVersion(), actorUserId, opinion,
                udiControlDocumentNo);
        MesPqcProductionReleaseDecisionResult replay = replayOrRejectProcessedApplication(
                application, actorUserId, "APPROVE", idempotencyKey, payloadHash);
        if (replay != null) {
            return replay;
        }
        MesProEdhrWorkTaskDO workTask = requireProcessableTask(
                application, command.getPqcReleaseWorkTaskId(), command.getExpectedVersion(), actorUserId);
        nonconformanceReviewService.ensureWorkOrderNotFrozen(application.getWorkOrderId(), "PQC放行");
        ensureNoClosedNonconformanceOutcome(application);
        MesProcessPoolActiveOrderDO activeOrder = requireAndLockActiveOrder(application, udiControlDocumentNo, actorUserId);
        signatureService.validatePqcSubmitSignature(actorUserId, command.getSignaturePassword());

        Long batchExecutionId = requireExistingBatchExecutionId(application);
        Long signatureId = signatureService.recordPqcReleaseSignature(
                actorUserId, batchExecutionId, application.getId(), command.getSignaturePassword(), opinion);
        LocalDateTime decidedAt = LocalDateTime.now(clock);
        String activeOrderFactsSnapshotHash = MesProductionReleaseFormalFactSnapshots.activeOrderFactsSnapshotHash(
                application, "APPROVE", actorUserId, decidedAt);
        MesPqcProductionReleaseDecisionResult result = baseResult(application, workTask)
                .setDecision("APPROVE")
                .setStatus(MesReleaseFlowStatus.MANAGER_RELEASE_PENDING)
                .setBatchExecutionId(batchExecutionId)
                .setSignatureId(signatureId)
                .setBatchRecordEvidenceIds(List.of())
                .setProcessInspectionEvidenceIds(List.of())
                .setProcessInspectionFormCenterInstanceIds(List.of())
                .setLossReportEvidenceIds(List.of())
                .setLossReportFormCenterInstanceIds(List.of())
                .setLossReportFieldAuditIds(List.of())
                .setLossReportFieldAuditHeadHashes(List.of())
                .setReportUploadTasks(List.of())
                .setUdiControlDocumentNo(udiControlDocumentNo)
                .setReportSnapshotHash(activeOrderFactsSnapshotHash)
                .setVersion(command.getExpectedVersion() + 2)
                .setDecidedBy(actorUserId)
                .setDecidedAt(decidedAt)
                .setDecisionIdempotencyKey(idempotencyKey)
                .setDecisionPayloadHash(payloadHash);
        int updated = applicationMapper.approveFromPending(application.getId(), command.getExpectedVersion(),
                batchExecutionId, actorUserId, decidedAt, activeOrderFactsSnapshotHash,
                JSON.toJSONString(result));
        requireCasSuccess(updated, application);
        requireTaskCompletion(workTaskMapper.completePqcDecisionTask(workTask.getId(), decidedAt, "APPROVE"), application);
        MesProductionReleaseManagerStageInitializationResult managerStage = managerStageInitializer
                .initializeManagerReleaseStage(new MesProductionReleaseManagerStageInitializationCommand()
                        .setApplicationId(application.getId())
                        .setBatchExecutionId(batchExecutionId)
                        .setReportSnapshotHash(activeOrderFactsSnapshotHash)
                        .setReportEvidences(List.of())
                        .setExpectedApplicationVersion(command.getExpectedVersion() + 1)
                        .setActiveOrderFormalFacts(true));
        requireManagerHandoff(application, command.getExpectedVersion() + 1,
                activeOrderFactsSnapshotHash, managerStage);
        batchExecutionPort.markReadyForMarketRelease(batchExecutionId, actorUserId);
        recordDecisionAudit(application, workTask, result, idempotencyKey,
                MesReleaseFlowAuditEventType.PQC_PRODUCTION_RELEASE_APPROVED);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesPqcProductionReleaseDecisionResult reject(
            Long actorUserId, MesPqcProductionReleaseRejectCommand command) {
        requireRejectCommand(actorUserId, command);
        String idempotencyKey = MesReleaseFlowIdempotency.requireKey(command.getIdempotencyKey());
        String reason = StrUtil.trim(command.getRejectReason());
        if (StrUtil.isBlank(reason) || reason.length() > 500) {
            throw blocker(MesReleaseFlowBlockerType.UNSUPPORTED_RELEASE_ACTION, null,
                    "REJECT_REASON", null, "rejectReason must contain 1 to 500 characters",
                    "provide the formal PQC rejection reason");
        }
        String payloadHash = decisionPayloadHash("REJECT", command.getApplicationId(),
                command.getPqcReleaseWorkTaskId(), command.getExpectedVersion(), actorUserId, reason, null);
        MesProcessPoolActiveOrderReleaseApplicationDO application = requireApplicationForUpdate(command.getApplicationId());
        MesPqcProductionReleaseDecisionResult replay = replayOrRejectProcessedApplication(
                application, actorUserId, "REJECT", idempotencyKey, payloadHash);
        if (replay != null) {
            return replay;
        }
        MesProEdhrWorkTaskDO workTask = requireProcessableTask(
                application, command.getPqcReleaseWorkTaskId(), command.getExpectedVersion(), actorUserId);
        LocalDateTime decidedAt = LocalDateTime.now(clock);
        MesPqcProductionReleaseDecisionResult result = baseResult(application, workTask)
                .setDecision("REJECT")
                .setStatus(MesReleaseFlowStatus.PQC_RELEASE_REJECTED)
                .setRejectReason(reason)
                .setVersion(command.getExpectedVersion() + 1)
                .setDecidedBy(actorUserId)
                .setDecidedAt(decidedAt)
                .setDecisionIdempotencyKey(idempotencyKey)
                .setDecisionPayloadHash(payloadHash);
        int updated = applicationMapper.rejectFromPending(application.getId(), command.getExpectedVersion(),
                actorUserId, decidedAt, reason, JSON.toJSONString(result));
        requireCasSuccess(updated, application);
        requireTaskCompletion(workTaskMapper.completePqcDecisionTask(workTask.getId(), decidedAt, "REJECT"), application);
        recordDecisionAudit(application, workTask, result, idempotencyKey,
                MesReleaseFlowAuditEventType.PQC_PRODUCTION_RELEASE_REJECTED);
        return result;
    }

    @Override
    public MesPqcProductionReleaseDecisionResult get(Long actorUserId, Long applicationId) {
        if (actorUserId == null || applicationId == null || applicationId <= 0) {
            throw blocker(MesReleaseFlowBlockerType.WORK_TASK_NOT_PROCESSABLE, null,
                    "RELEASE_APPLICATION", applicationId == null ? null : String.valueOf(applicationId),
                    "release application query identity is invalid", "provide a valid applicationId");
        }
        MesProcessPoolActiveOrderReleaseApplicationDO application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw blocker(MesReleaseFlowBlockerType.WORK_TASK_NOT_PROCESSABLE, null,
                    "RELEASE_APPLICATION", String.valueOf(applicationId),
                    "production release application does not exist", "query an existing release application");
        }
        MesProEdhrWorkTaskDO workTask = workTaskMapper.selectById(application.getPqcReleaseWorkTaskId());
        requireFrozenPqcTask(application, workTask, actorUserId);
        MesPqcProductionReleaseDecisionResult stored = parseStoredDecision(application);
        return stored == null ? baseResult(application, workTask)
                .setDecision(application.getPqcDecision())
                .setStatus(application.getApplicationStatus())
                .setRejectReason(application.getPqcRejectReason())
                .setBatchExecutionId(application.getBatchExecutionId())
                .setReportSnapshotHash(application.getReportSnapshotHash())
                .setVersion(application.getVersion())
                .setDecidedBy(application.getPqcDecidedBy())
                .setDecidedAt(application.getPqcDecidedAt()) : stored;
    }

    @Override
    public PageResult<MesPqcProductionReleasePageItem> getPqcReleasePage(
            Long actorUserId, MesPqcProductionReleasePageQuery query) {
        requirePqcPageQuery(actorUserId, query);
        cn.iocoder.yudao.framework.common.pojo.PageParam pageParam =
                new cn.iocoder.yudao.framework.common.pojo.PageParam();
        pageParam.setPageNo(query.getPageNo());
        pageParam.setPageSize(query.getPageSize());
        PageResult<MesProcessPoolActiveOrderReleaseApplicationDO> applicationPage =
                applicationMapper.selectPqcReleasePage(
                        pageParam, TenantContextHolder.getTenantId(), actorUserId, query.getViewStatus(),
                        StrUtil.trim(query.getWorkOrderCode()), StrUtil.trim(query.getBatchCode()));
        if (applicationPage == null) {
            throw new IllegalStateException("PQC release page query returned no page result");
        }
        List<MesProcessPoolActiveOrderReleaseApplicationDO> applications = applicationPage.getList();
        if (applications == null || applications.isEmpty()) {
            return new PageResult<>(List.of(), applicationPage.getTotal());
        }
        Map<Long, MesProEdhrNonconformanceReviewDO> reviewsByApplicationId = new java.util.HashMap<>();
        nonconformanceReviewMapper.selectLatestBySourceIds(
                        MesProEdhrNonconformanceReviewService.SOURCE_TYPE_PQC_RELEASE,
                        applications.stream().map(MesProcessPoolActiveOrderReleaseApplicationDO::getId).toList(),
                        TenantContextHolder.getTenantId())
                .forEach(review -> reviewsByApplicationId.putIfAbsent(review.getSourceId(), review));

        List<MesPqcProductionReleasePageItem> pageRows = new java.util.ArrayList<>();
        for (MesProcessPoolActiveOrderReleaseApplicationDO application : applications) {
            MesProEdhrNonconformanceReviewDO review = reviewsByApplicationId.get(application.getId());
            pageRows.add(toPageItem(application, review, query.getViewStatus()));
        }
        return new PageResult<>(pageRows, applicationPage.getTotal());
    }

    private void requireApproveCommand(Long actorUserId, MesPqcProductionReleaseApproveCommand command) {
        if (actorUserId == null || command == null || command.getApplicationId() == null
                || command.getApplicationId() <= 0 || command.getPqcReleaseWorkTaskId() == null
                || command.getPqcReleaseWorkTaskId() <= 0 || command.getExpectedVersion() == null
                || command.getExpectedVersion() <= 0 || StrUtil.isBlank(command.getSignaturePassword())) {
            throw blocker(MesReleaseFlowBlockerType.UNSUPPORTED_RELEASE_ACTION, null,
                    "PQC_RELEASE_DECISION", null, "PQC approval command is incomplete",
                    "provide applicationId, pqcReleaseWorkTaskId, expectedVersion and signaturePassword");
        }
    }

    private String requireUdiControlDocumentNo(String value) {
        try {
            return MesPqcProductionReleaseUdiPolicy.requireNormalized(value);
        } catch (IllegalArgumentException exception) {
            throw blocker(MesReleaseFlowBlockerType.UNSUPPORTED_RELEASE_ACTION, null,
                    "UDI_CONTROL_DOCUMENT_NO", null, exception.getMessage(),
                    "provide a non-blank UDI control document number with at most 128 characters");
        }
    }

    private MesProcessPoolActiveOrderDO requireAndLockActiveOrder(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            String udiControlDocumentNo,
            Long actorUserId) {
        if (application.getActiveOrderId() == null || application.getActiveOrderId() <= 0) {
            throw blocker(MesReleaseFlowBlockerType.UNSUPPORTED_RELEASE_ACTION, application,
                    "ACTIVE_ORDER", null,
                    "release application is missing the formal activeOrderId source",
                    "repair the release application source before approving the PQC release");
        }
        if (activeOrderMapper == null) {
            throw blocker(MesReleaseFlowBlockerType.UNSUPPORTED_RELEASE_ACTION, application,
                    "ACTIVE_ORDER", String.valueOf(application.getActiveOrderId()),
                    "active-order persistence boundary is unavailable",
                    "configure the active-order mapper before approving the PQC release");
        }
        MesProcessPoolActiveOrderDO activeOrder = activeOrderMapper.selectByIdForUpdate(application.getActiveOrderId());
        try {
            MesPqcProductionReleaseUdiPolicy.WriteDecision decision =
                    MesPqcProductionReleaseUdiPolicy.checkCompatibility(activeOrder, udiControlDocumentNo);
            if (decision == MesPqcProductionReleaseUdiPolicy.WriteDecision.WRITE) {
                if (activeOrder.getVersion() == null) {
                    throw new IllegalStateException("formal active order version is missing");
                }
                int updated = activeOrderMapper.writeUdiControlDocumentNo(
                        activeOrder.getId(), activeOrder.getVersion(), udiControlDocumentNo, actorUserId);
                if (updated != 1) {
                    throw new IllegalStateException("active-order UDI control document number persistence failed");
                }
                activeOrder.setUdiControlDocumentNo(udiControlDocumentNo);
            }
            return activeOrder;
        } catch (IllegalStateException exception) {
            throw blocker(MesReleaseFlowBlockerType.UNSUPPORTED_RELEASE_ACTION, application,
                    "UDI_CONTROL_DOCUMENT_NO", String.valueOf(application.getActiveOrderId()),
                    exception.getMessage(), "use the existing UDI number or repair the formal active-order source");
        }
    }

    private void requirePqcPageQuery(Long actorUserId, MesPqcProductionReleasePageQuery query) {
        if (actorUserId == null || TenantContextHolder.getTenantId() == null || query == null
                || query.getPageNo() == null || query.getPageSize() == null
                || query.getPageNo() <= 0 || query.getPageSize() <= 0
                || !Set.of(VIEW_STATUS_PENDING, VIEW_STATUS_RELEASED, VIEW_STATUS_VOIDED,
                        VIEW_STATUS_REWORKED, VIEW_STATUS_CONCESSION_RELEASED).contains(query.getViewStatus())) {
            throw blocker(MesReleaseFlowBlockerType.UNSUPPORTED_RELEASE_ACTION, null,
                    "PQC_RELEASE_PAGE", null, "PQC release page query is invalid",
                    "provide a valid authenticated page query and view status");
        }
    }

    private void ensureNoClosedNonconformanceOutcome(
            MesProcessPoolActiveOrderReleaseApplicationDO application) {
        List<MesProEdhrNonconformanceReviewDO> reviews = nonconformanceReviewMapper.selectLatestBySourceIds(
                MesProEdhrNonconformanceReviewService.SOURCE_TYPE_PQC_RELEASE, List.of(application.getId()),
                TenantContextHolder.getTenantId());
        if (!reviews.isEmpty() && MesProEdhrNonconformanceReviewService.STATUS_CLOSED
                .equals(reviews.get(0).getReviewStatus())
                && !MesProEdhrNonconformanceReviewService.DISPOSITION_CONCESSION_RELEASE
                .equals(reviews.get(0).getDisposition())) {
            throw blocker(MesReleaseFlowBlockerType.UNSUPPORTED_RELEASE_ACTION, application,
                    "NONCONFORMANCE_REVIEW", String.valueOf(reviews.get(0).getId()),
                    "nonconformance review already produced a terminal PQC disposition",
                    "view the disposition in the matching production release status tab");
        }
    }

    private MesPqcProductionReleasePageItem toPageItem(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            MesProEdhrNonconformanceReviewDO review,
            String viewStatus) {
        return new MesPqcProductionReleasePageItem()
                .setApplicationId(application.getId())
                .setPqcReleaseWorkTaskId(application.getPqcReleaseWorkTaskId())
                .setVersion(application.getVersion())
                .setViewStatus(viewStatus)
                .setApplicationStatus(application.getApplicationStatus())
                .setActiveOrderId(application.getActiveOrderId())
                .setWorkOrderId(application.getWorkOrderId())
                .setWorkOrderCode(application.getWorkOrderCode())
                .setBatchCode(application.getBatchCode())
                .setProductId(application.getProductId())
                .setBatchExecutionId(application.getBatchExecutionId())
                .setAppliedAt(application.getAppliedAt())
                .setAppliedBy(application.getAppliedBy())
                .setDecidedAt(application.getPqcDecidedAt())
                .setDecidedBy(application.getPqcDecidedBy())
                .setUnderReview(review != null && MesProEdhrNonconformanceReviewService.STATUS_PENDING_REVIEW
                        .equals(review.getReviewStatus()))
                .setNonconformanceReviewId(review == null ? null : review.getId())
                .setNonconformanceDisposition(review == null ? null : review.getDisposition())
                .setNonconformanceReason(review == null ? null : review.getNonconformanceReason())
                .setNonconformanceClosedAt(review == null ? null : review.getClosedAt());
    }

    private void requireRejectCommand(Long actorUserId, MesPqcProductionReleaseRejectCommand command) {
        if (actorUserId == null || command == null || command.getApplicationId() == null
                || command.getApplicationId() <= 0 || command.getPqcReleaseWorkTaskId() == null
                || command.getPqcReleaseWorkTaskId() <= 0 || command.getExpectedVersion() == null
                || command.getExpectedVersion() <= 0) {
            throw blocker(MesReleaseFlowBlockerType.UNSUPPORTED_RELEASE_ACTION, null,
                    "PQC_RELEASE_DECISION", null, "PQC rejection command is incomplete",
                    "provide applicationId, pqcReleaseWorkTaskId and expectedVersion");
        }
    }

    private String trimAndValidateOptionalText(String value, String fieldName) {
        String trimmed = StrUtil.trim(value);
        if (trimmed != null && trimmed.length() > 500) {
            throw blocker(MesReleaseFlowBlockerType.UNSUPPORTED_RELEASE_ACTION, null,
                    "PQC_RELEASE_DECISION", null, fieldName + " exceeds 500 characters",
                    "shorten " + fieldName + " to at most 500 characters");
        }
        return trimmed;
    }

    private MesProcessPoolActiveOrderReleaseApplicationDO requireApplicationForUpdate(Long applicationId) {
        MesProcessPoolActiveOrderReleaseApplicationDO application = applicationMapper.selectByIdForUpdate(applicationId);
        if (application == null || !MesReleaseFlowStatus.isPersistentStatus(application.getApplicationStatus())
                || application.getVersion() == null || application.getVersion() <= 0) {
            throw blocker(MesReleaseFlowBlockerType.LEGACY_RELEASE_APPLICATION_MIGRATION_REQUIRED, application,
                    "RELEASE_APPLICATION", String.valueOf(applicationId),
                    "release application is missing or uses a legacy lifecycle",
                    "migrate the application with approved evidence before retrying");
        }
        return application;
    }

    private MesProEdhrWorkTaskDO requireProcessableTask(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            Long workTaskId,
            Integer expectedVersion,
            Long actorUserId) {
        if (!MesReleaseFlowStatus.PQC_RELEASE_PENDING.equals(application.getApplicationStatus())
                || !Objects.equals(expectedVersion, application.getVersion())) {
            throw blocker(MesReleaseFlowBlockerType.STATE_VERSION_CONFLICT, application,
                    "RELEASE_APPLICATION", String.valueOf(application.getId()),
                    "application status or version no longer matches the PQC decision command",
                    "reload the authoritative receipt before deciding");
        }
        if (!Objects.equals(workTaskId, application.getPqcReleaseWorkTaskId())) {
            throw blocker(MesReleaseFlowBlockerType.WORK_TASK_NOT_PROCESSABLE, application,
                    "WORK_TASK", String.valueOf(workTaskId), "PQC work task does not belong to the application",
                    "use the frozen PQC work task from the application receipt");
        }
        MesProEdhrWorkTaskDO workTask = workTaskMapper.selectById(workTaskId);
        if (workTask == null || !TASK_TYPE_PQC_RELEASE.equals(workTask.getTaskType())
                || !BUSINESS_SCOPE_RELEASE_APPLICATION.equals(workTask.getBusinessScopeType())
                || !Objects.equals(application.getId(), workTask.getBusinessScopeId())
                || !ACTIVE_TASK_STATUSES.contains(workTask.getStatus())) {
            throw blocker(MesReleaseFlowBlockerType.WORK_TASK_NOT_PROCESSABLE, application,
                    "WORK_TASK", String.valueOf(workTaskId), "PQC work task is not processable",
                    "reload the current PQC work task receipt");
        }
        requireAuthorized(application, workTask, actorUserId);
        return workTask;
    }

    private void requireAuthorized(MesProcessPoolActiveOrderReleaseApplicationDO application,
                                   MesProEdhrWorkTaskDO workTask,
                                   Long actorUserId) {
        if (workTask == null || !containsCandidate(workTask.getCandidateUserSnapshot(), actorUserId)) {
            throw blocker(MesReleaseFlowBlockerType.WORK_TASK_NOT_PROCESSABLE, application,
                    "WORK_TASK", workTask == null ? null : String.valueOf(workTask.getId()),
                    "current user is not in the frozen PQC candidate snapshot",
                    "use an authorized frozen PQC candidate");
        }
    }

    private void requireFrozenPqcTask(MesProcessPoolActiveOrderReleaseApplicationDO application,
                                      MesProEdhrWorkTaskDO workTask,
                                      Long actorUserId) {
        if (workTask == null
                || !TASK_TYPE_PQC_RELEASE.equals(workTask.getTaskType())
                || !BUSINESS_SCOPE_RELEASE_APPLICATION.equals(workTask.getBusinessScopeType())
                || !Objects.equals(application.getId(), workTask.getBusinessScopeId())
                || !containsCandidate(workTask.getCandidateUserSnapshot(), actorUserId)) {
            throw blocker(MesReleaseFlowBlockerType.WORK_TASK_NOT_PROCESSABLE, application,
                    "WORK_TASK", workTask == null ? null : String.valueOf(workTask.getId()),
                    "current user is not in the frozen PQC candidate snapshot",
                    "use an authorized frozen PQC candidate");
        }
    }

    private boolean containsCandidate(String snapshot, Long userId) {
        if (StrUtil.isBlank(snapshot) || userId == null) {
            return false;
        }
        String expected = String.valueOf(userId);
        return List.of(snapshot.split(",")).stream()
                .map(String::trim)
                .anyMatch(expected::equals);
    }

    private Long requireExistingBatchExecutionId(MesProcessPoolActiveOrderReleaseApplicationDO application) {
        Long batchExecutionId = application == null ? null : application.getBatchExecutionId();
        if (batchExecutionId == null || batchExecutionId <= 0) {
            throw blocker(MesReleaseFlowBlockerType.LEGACY_BATCH_EXECUTION_MIGRATION_REQUIRED, application,
                    "BATCH_EXECUTION", null, "release application is missing the P3 batch execution association",
                    "run P3 to push the P2 batch-record facts before PQC release");
        }
        return batchExecutionId;
    }

    private MesPqcReleaseDossierWriteResult requireDossierWrite(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            MesPqcReleaseDossierWriteResult result) {
        boolean validLossReceipt = result != null
                && (("SUCCESS".equals(result.getLossReportStatus())
                && Boolean.TRUE.equals(result.getHasActualLoss())
                && result.getLossQuantity() != null && result.getLossQuantity().signum() > 0
                && (!empty(result.getLossReportEvidenceIds()) || !empty(result.getLossReportFormCenterInstanceIds()))
                && (empty(result.getLossReportFormCenterInstanceIds())
                    || (!empty(result.getLossReportFieldAuditIds()) && !empty(result.getLossReportFieldAuditHeadHashes()))))
                || ("NOT_REQUIRED".equals(result.getLossReportStatus())
                && Boolean.FALSE.equals(result.getHasActualLoss())
                && result.getLossQuantity() != null && result.getLossQuantity().signum() == 0
                && empty(result.getLossReportEvidenceIds()) && empty(result.getLossReportFormCenterInstanceIds())));
        if (result == null || empty(result.getBatchRecordEvidenceIds())) {
            throw blocker(MesReleaseFlowBlockerType.BATCH_RECORD_SOURCE_REQUIRED, application,
                    "RELEASE_DOSSIER", String.valueOf(application.getId()),
                    "formal batch-record mapping must return persistent evidence identifiers",
                    "repair the batch-record mapping before retrying");
        }
        if (empty(result.getProcessInspectionEvidenceIds())
                && empty(result.getProcessInspectionFormCenterInstanceIds())) {
            throw blocker(MesReleaseFlowBlockerType.PROCESS_INSPECTION_SOURCE_REQUIRED, application,
                    "RELEASE_DOSSIER", String.valueOf(application.getId()),
                    "formal process-inspection mapping must return persistent evidence identifiers",
                    "repair the process-inspection mapping before retrying");
        }
        if (!validLossReceipt) {
            throw blocker(MesReleaseFlowBlockerType.LOSS_REPORT_SOURCE_REQUIRED, application,
                    "RELEASE_DOSSIER", String.valueOf(application.getId()),
                    "formal loss-report mapping must return a consistent status and evidence receipt",
                    "repair the loss-report mapping before retrying");
        }
        return result;
    }

    private MesProductionReleaseReportStageInitializationResult requireReportStage(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            MesProductionReleaseReportStageInitializationResult result) {
        if (result == null || StrUtil.isBlank(result.getReportSnapshotHash())
                || result.getReportUploadTasks() == null || result.getReportUploadTasks().size() != 4) {
            throw reportOwnerBlocker(application, "four frozen report upload tasks are required");
        }
        Set<String> nodeTypes = new HashSet<>();
        for (MesProductionReleaseReportUploadTaskReceipt task : result.getReportUploadTasks()) {
            if (task == null || task.getBatchTaskId() == null || task.getWorkTaskId() == null
                    || task.getCandidateUserIds() == null || task.getCandidateUserIds().isEmpty()
                    || !MesProEdhrWorkTaskStatus.TODO.equals(task.getStatus())
                    || !REQUIRED_REPORT_NODE_TYPES.contains(task.getNodeType())
                    || !nodeTypes.add(task.getNodeType())) {
                throw reportOwnerBlocker(application, "report upload task receipt is incomplete or duplicated");
            }
        }
        if (!nodeTypes.equals(REQUIRED_REPORT_NODE_TYPES)) {
            throw reportOwnerBlocker(application, "report upload task node set is incomplete");
        }
        return result;
    }

    private MesReleaseFlowBlockerException reportOwnerBlocker(
            MesProcessPoolActiveOrderReleaseApplicationDO application, String reason) {
        return blocker(MesReleaseFlowBlockerType.REPORT_OWNER_REQUIRED, application,
                "REPORT_UPLOAD_STAGE", String.valueOf(application.getId()), reason,
                "configure all four frozen report owners before PQC approval");
    }

    private MesPqcProductionReleaseDecisionResult baseResult(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            MesProEdhrWorkTaskDO workTask) {
        return new MesPqcProductionReleaseDecisionResult()
                .setApplicationId(application.getId())
                .setPqcReleaseWorkTaskId(workTask == null ? application.getPqcReleaseWorkTaskId() : workTask.getId())
                .setBatchRecordEvidenceIds(List.of())
                .setProcessInspectionEvidenceIds(List.of())
                .setProcessInspectionFormCenterInstanceIds(List.of())
                .setLossReportEvidenceIds(List.of())
                .setLossReportFormCenterInstanceIds(List.of())
                .setLossReportFieldAuditIds(List.of())
                .setLossReportFieldAuditHeadHashes(List.of())
                .setReportUploadTasks(List.of())
                .setSourceSnapshotHash(application.getSourceSnapshotHash());
    }

    private MesPqcProductionReleaseDecisionResult replayOrRejectProcessedApplication(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            Long actorUserId,
            String decision,
            String idempotencyKey,
            String payloadHash) {
        if (MesReleaseFlowStatus.PQC_RELEASE_PENDING.equals(application.getApplicationStatus())) {
            return null;
        }
        MesProEdhrWorkTaskDO workTask = workTaskMapper.selectById(application.getPqcReleaseWorkTaskId());
        requireFrozenPqcTask(application, workTask, actorUserId);
        MesPqcProductionReleaseDecisionResult stored = parseStoredDecision(application);
        if (stored != null && Objects.equals(idempotencyKey, stored.getDecisionIdempotencyKey())) {
            if (Objects.equals(decision, stored.getDecision())
                    && Objects.equals(payloadHash, stored.getDecisionPayloadHash())) {
                return stored;
            }
            throw blocker(MesReleaseFlowBlockerType.IDEMPOTENCY_PAYLOAD_CONFLICT, application,
                    "IDEMPOTENCY_KEY", idempotencyKey,
                    "idempotency key is already bound to a different PQC decision payload",
                    "query the existing receipt or submit a new key for a new decision");
        }
        throw blocker(MesReleaseFlowBlockerType.UNSUPPORTED_RELEASE_ACTION, application,
                "RELEASE_APPLICATION", String.valueOf(application.getId()),
                "PQC decision is already terminal for this application",
                "query the authoritative decision receipt");
    }

    private MesPqcProductionReleaseDecisionResult parseStoredDecision(
            MesProcessPoolActiveOrderReleaseApplicationDO application) {
        if (StrUtil.isBlank(application.getDossierSummaryJson())) {
            return null;
        }
        try {
            return JSON.parseObject(application.getDossierSummaryJson(), MesPqcProductionReleaseDecisionResult.class);
        } catch (RuntimeException exception) {
            throw blocker(MesReleaseFlowBlockerType.LEGACY_RELEASE_APPLICATION_MIGRATION_REQUIRED, application,
                    "RELEASE_APPLICATION", String.valueOf(application.getId()),
                    "stored PQC decision receipt is invalid",
                    "repair the historical receipt with approved evidence before retrying");
        }
    }

    private void requireCasSuccess(int updated, MesProcessPoolActiveOrderReleaseApplicationDO application) {
        if (updated != 1) {
            throw blocker(MesReleaseFlowBlockerType.STATE_VERSION_CONFLICT, application,
                    "RELEASE_APPLICATION", String.valueOf(application.getId()),
                    "PQC decision lost the application version race",
                    "reload the authoritative receipt before deciding");
        }
    }

    private void requireTaskCompletion(int updated, MesProcessPoolActiveOrderReleaseApplicationDO application) {
        if (updated != 1) {
            throw blocker(MesReleaseFlowBlockerType.WORK_TASK_NOT_PROCESSABLE, application,
                    "WORK_TASK", String.valueOf(application.getPqcReleaseWorkTaskId()),
                    "PQC work task completion lost its status race",
                    "reload the authoritative work task before deciding");
        }
    }

    private void requireManagerHandoff(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            Integer expectedVersion,
            String reportSnapshotHash,
            MesProductionReleaseManagerStageInitializationResult managerStage) {
        if (managerStage == null || managerStage.getReleaseTransactionId() == null
                || managerStage.getManagerReleaseWorkTaskId() == null
                || StrUtil.isBlank(managerStage.getManagerCandidateSnapshotHash())) {
            throw blocker(MesReleaseFlowBlockerType.RELEASE_TRANSACTION_NOT_PROCESSABLE, application,
                    "MANAGER_RELEASE_STAGE", String.valueOf(application.getId()),
                    "manager release stage receipt is incomplete",
                    "configure the management representative role and retry PQC release");
        }
        int updated = applicationMapper.handoffReportsToManager(application.getId(), expectedVersion,
                reportSnapshotHash, managerStage.getReleaseTransactionId(),
                managerStage.getManagerReleaseWorkTaskId(), managerStage.getManagerCandidateSnapshotHash());
        if (updated != 1) {
            throw blocker(MesReleaseFlowBlockerType.STATE_VERSION_CONFLICT, application,
                    "RELEASE_APPLICATION", String.valueOf(application.getId()),
                    "manager release handoff lost the application version race",
                    "reload the authoritative receipt before deciding");
        }
    }

    private void recordDecisionAudit(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            MesProEdhrWorkTaskDO workTask,
            MesPqcProductionReleaseDecisionResult result,
            String idempotencyKey,
            String eventType) {
        auditRecorder.record(new MesReleaseFlowAuditCommand()
                .setEventType(eventType)
                .setStage(MesReleaseFlowStage.SP_2)
                .setRequestId(idempotencyKey)
                .setIdempotencyKey(idempotencyKey)
                .setTenantId(TenantContextHolder.getTenantId())
                .setApplicationId(application.getId())
                .setActiveOrderId(application.getActiveOrderId())
                .setWorkTaskId(workTask.getId())
                .setBatchExecutionId(result.getBatchExecutionId())
                .setSignatureId(result.getSignatureId())
                .setFromStatus(MesReleaseFlowStatus.PQC_RELEASE_PENDING)
                .setToStatus(result.getStatus())
                .setVersion(result.getVersion())
                .setActorUserId(result.getDecidedBy())
                .setOccurredAt(result.getDecidedAt())
                .setSourceSnapshotHash(result.getSourceSnapshotHash())
                .setResultStatus("SUCCESS"));
    }

    private String decisionPayloadHash(
            String decision, Long applicationId, Long workTaskId, Integer expectedVersion, Long actorUserId,
            String detail, String udiControlDocumentNo) {
        return MesReleaseFlowIdempotency.payloadHash(decision, String.valueOf(applicationId),
                String.valueOf(workTaskId), String.valueOf(expectedVersion), String.valueOf(actorUserId), detail,
                udiControlDocumentNo);
    }

    private boolean empty(List<?> values) {
        return values == null || values.isEmpty();
    }

    private <T> List<T> copy(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private MesReleaseFlowBlockerException blocker(
            MesReleaseFlowBlockerType type,
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            String objectType,
            String objectId,
            String reason,
            String suggestion) {
        return new MesReleaseFlowBlockerException(reason, new MesReleaseFlowFailureRespVO()
                .setStage(MesReleaseFlowStage.SP_2)
                .setCurrentStatus(application == null ? null : application.getApplicationStatus())
                .setBlockers(List.of(new MesReleaseFlowBlocker()
                        .setBlockerType(type)
                        .setObjectType(objectType)
                        .setObjectId(objectId)
                        .setReason(reason)
                        .setSuggestion(suggestion))));
    }
}
