package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionOpenOrCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleasePrecheckReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolOrderProcessCompletionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolOrderProcessCompletionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrCandidateResolver;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseSubmitForApprovalCommand;
import com.alibaba.fastjson.JSON;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_PROGRESS_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_WORK_ORDER_NOT_EXISTS;

@Service
public class MesTeamLeaderActiveOrderReleaseGenerationService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_BLOCKED = "BLOCKED";
    private static final String STATUS_PENDING_RELEASE_APPROVAL = "PENDING_RELEASE_APPROVAL";
    private static final String TASK_TYPE_RELEASE_APPROVE = "RELEASE_APPROVE";
    private static final String RULE_SCOPE_TYPE_ROUTE = "ROUTE";
    private static final String PLANNING_SNAPSHOT = "AO_RELEASE_SOURCE_V1:PLANNING";
    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 128;
    private static final int MAX_APPLY_REMARK_LENGTH = 500;

    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesProWorkOrderMapper workOrderMapper;
    private final MesMdItemMapper itemMapper;
    private final MesProRouteMapper routeMapper;
    private final MesProRouteVersionMapper routeVersionMapper;
    private final MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;
    private final MesProcessPoolOrderProcessCompletionMapper completionMapper;
    private final MesPqcInspectionTaskMapper pqcTaskMapper;
    private final MesPqcProcessInspectionAggregateDetailMapper aggregateDetailMapper;
    private final MesWorkOrderAbnormalStateService abnormalStateService;
    private final MesProProcessPoolEventMapper eventMapper;
    private final MesProcessPoolReportAllocationMapper allocationMapper;
    private final MesProcessPoolSubmissionReviewMapper reviewMapper;
    private final MesTeamLeaderActiveOrderReleaseBatchRecordWriter batchRecordWriter;
    private final MesTeamLeaderActiveOrderReleaseProcessInspectionWriter processInspectionWriter;
    private final MesTeamLeaderActiveOrderReleaseLossReportWriter lossReportWriter;
    private final MesTeamLeaderActiveOrderReleaseDossierCompletenessChecker completenessChecker;
    private final MesProEdhrBatchExecutionService batchExecutionService;
    private final MesProEdhrReleaseService releaseService;
    private final MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper;
    private final MesTeamLeaderActiveOrderReleaseApplicationPersistenceService persistenceService;
    private final MesProEdhrWorkTaskAssignmentRuleMapper assignmentRuleMapper;
    private final MesProEdhrCandidateResolver candidateResolver;
    private final MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    private final MesTeamLeaderActiveOrderReleaseSourceSnapshotHasher sourceSnapshotHasher;

    public MesTeamLeaderActiveOrderReleaseGenerationService(
            MesProcessPoolActiveOrderMapper activeOrderMapper,
            MesProWorkOrderMapper workOrderMapper,
            MesMdItemMapper itemMapper,
            MesProRouteMapper routeMapper,
            MesProRouteVersionMapper routeVersionMapper,
            MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper,
            MesProcessPoolOrderProcessCompletionMapper completionMapper,
            MesPqcInspectionTaskMapper pqcTaskMapper,
            MesPqcProcessInspectionAggregateDetailMapper aggregateDetailMapper,
            MesWorkOrderAbnormalStateService abnormalStateService,
            MesProProcessPoolEventMapper eventMapper,
            MesProcessPoolReportAllocationMapper allocationMapper,
            MesProcessPoolSubmissionReviewMapper reviewMapper,
            MesTeamLeaderActiveOrderReleaseBatchRecordWriter batchRecordWriter,
            MesTeamLeaderActiveOrderReleaseProcessInspectionWriter processInspectionWriter,
            MesTeamLeaderActiveOrderReleaseLossReportWriter lossReportWriter,
            MesTeamLeaderActiveOrderReleaseDossierCompletenessChecker completenessChecker,
            MesProEdhrBatchExecutionService batchExecutionService,
            MesProEdhrReleaseService releaseService,
            MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper,
            MesTeamLeaderActiveOrderReleaseApplicationPersistenceService persistenceService,
            MesProEdhrWorkTaskAssignmentRuleMapper assignmentRuleMapper,
            MesProEdhrCandidateResolver candidateResolver,
            MesProEdhrBatchExecutionTaskMapper batchTaskMapper,
            MesTeamLeaderActiveOrderReleaseSourceSnapshotHasher sourceSnapshotHasher) {
        this.activeOrderMapper = activeOrderMapper;
        this.workOrderMapper = workOrderMapper;
        this.itemMapper = itemMapper;
        this.routeMapper = routeMapper;
        this.routeVersionMapper = routeVersionMapper;
        this.processSnapshotMapper = processSnapshotMapper;
        this.completionMapper = completionMapper;
        this.pqcTaskMapper = pqcTaskMapper;
        this.aggregateDetailMapper = aggregateDetailMapper;
        this.abnormalStateService = abnormalStateService;
        this.eventMapper = eventMapper;
        this.allocationMapper = allocationMapper;
        this.reviewMapper = reviewMapper;
        this.batchRecordWriter = batchRecordWriter;
        this.processInspectionWriter = processInspectionWriter;
        this.lossReportWriter = lossReportWriter;
        this.completenessChecker = completenessChecker;
        this.batchExecutionService = batchExecutionService;
        this.releaseService = releaseService;
        this.applicationMapper = applicationMapper;
        this.persistenceService = persistenceService;
        this.assignmentRuleMapper = assignmentRuleMapper;
        this.candidateResolver = candidateResolver;
        this.batchTaskMapper = batchTaskMapper;
        this.sourceSnapshotHasher = sourceSnapshotHasher;
    }

    @Transactional(rollbackFor = Exception.class)
    public MesTeamLeaderActiveOrderReleaseApplicationResult generate(
            Long leaderUserId, MesTeamLeaderActiveOrderReleaseApplyCommand command) {
        validateApplyCommand(leaderUserId, command);
        String requestKey = requireRequestIdempotencyKey(command.getIdempotencyKey());
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null || tenantId <= 0) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "activeOrderReleaseTenant");
        }
        MesProcessPoolActiveOrderDO activeOrder = requireActiveOrder(command.getActiveOrderId(), leaderUserId);
        MesProcessPoolActiveOrderReleaseApplicationDO requestExisting =
                applicationMapper.selectByRequestIdempotencyKey(command.getActiveOrderId(), requestKey);
        if (requestExisting != null) {
            return persistenceService.toResult(requestExisting);
        }

        MesProWorkOrderDO workOrder = requireWorkOrder(activeOrder.getWorkOrderId());
        LockedMasterData masterData = requireLockedMasterData(activeOrder, workOrder);
        String batchCode = requireBatchCode(workOrder);
        List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots = requireSnapshots(activeOrder);
        List<MesProcessPoolOrderProcessCompletionDO> completions =
                requireFormalProductionCompletions(activeOrder, snapshots);
        requireFormalPqcCompletions(activeOrder, snapshots);

        MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand batchCommand =
                new MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand()
                        .setTenantId(tenantId)
                        .setActiveOrderId(activeOrder.getId())
                        .setWorkOrderId(workOrder.getId())
                        .setRouteId(activeOrder.getRouteId())
                        .setRouteVersionId(activeOrder.getRouteVersionId())
                        .setProductId(workOrder.getProductId())
                        .setBatchCode(batchCode)
                        .setApplicantUserId(leaderUserId)
                        .setWorkOrder(workOrder)
                        .setSourceSnapshotHash(PLANNING_SNAPSHOT)
                        .setProcessSources(loadBatchRecordSources(activeOrder, snapshots, completions));
        MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand inspectionCommand =
                new MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand()
                        .setTenantId(tenantId)
                        .setActiveOrderId(activeOrder.getId())
                        .setWorkOrderId(workOrder.getId())
                        .setProductId(workOrder.getProductId())
                        .setRouteId(activeOrder.getRouteId())
                        .setRouteVersionId(activeOrder.getRouteVersionId())
                        .setBatchCode(batchCode)
                        .setSourceSnapshotHash(PLANNING_SNAPSHOT);
        MesTeamLeaderActiveOrderReleaseLossReportPlanCommand lossCommand =
                new MesTeamLeaderActiveOrderReleaseLossReportPlanCommand()
                        .setTenantId(tenantId)
                        .setActiveOrderId(activeOrder.getId())
                        .setWorkOrderId(workOrder.getId())
                        .setRouteId(activeOrder.getRouteId())
                        .setRouteVersionId(activeOrder.getRouteVersionId())
                        .setProductId(workOrder.getProductId())
                        .setBatchCode(batchCode)
                        .setSourceSnapshotHash(PLANNING_SNAPSHOT)
                        .setProcessSnapshots(snapshots);

        // The formal hash is derived from plan evidence. The planning marker is replaced before any write.
        MesTeamLeaderActiveOrderReleaseBatchRecordPlan batchPlan = batchRecordWriter.plan(batchCommand);
        MesTeamLeaderActiveOrderReleaseProcessInspectionPlan inspectionPlan =
                processInspectionWriter.plan(inspectionCommand);
        MesTeamLeaderActiveOrderReleaseLossReportPlan lossPlan = lossReportWriter.plan(lossCommand);
        requirePlanResult(batchPlan, inspectionPlan, lossPlan);

        ReleaseOwner owner = resolveReleaseOwner(activeOrder.getRouteId());
        List<MesTeamLeaderActiveOrderReleaseBlocker> blockers = collectPlanningBlockers(
                activeOrder, workOrder, batchPlan, inspectionPlan, lossPlan, owner);
        String sourceSnapshotHash = sourceSnapshotHasher.hash(
                new MesTeamLeaderActiveOrderReleaseSourceSnapshotHasher.Input(
                        tenantId, activeOrder, workOrder, masterData.product(), masterData.route(),
                        masterData.routeVersion(), snapshots, completions,
                        batchPlan, inspectionPlan, lossPlan, owner.rule(), owner.sourceType(),
                        owner.sourceId(), owner.candidateUserIds()));
        batchCommand.setSourceSnapshotHash(sourceSnapshotHash);
        inspectionCommand.setSourceSnapshotHash(sourceSnapshotHash);
        lossCommand.setSourceSnapshotHash(sourceSnapshotHash);

        String businessKey = String.join("|", MesTeamLeaderActiveOrderReleaseSourceSnapshotHasher.VERSION,
                String.valueOf(workOrder.getId()), String.valueOf(activeOrder.getRouteVersionId()),
                sourceSnapshotHash);
        MesProcessPoolActiveOrderReleaseApplicationDO businessExisting =
                applicationMapper.selectByBusinessIdempotencyKey(activeOrder.getId(), businessKey);
        if (businessExisting != null) {
            if (!Objects.equals(activeOrder.getId(), businessExisting.getActiveOrderId())
                    || !Objects.equals(workOrder.getId(), businessExisting.getWorkOrderId())
                    || !Objects.equals(activeOrder.getRouteVersionId(), businessExisting.getRouteVersionId())
                    || !Objects.equals(businessKey, businessExisting.getBusinessIdempotencyKey())
                    || !Objects.equals(sourceSnapshotHash, businessExisting.getSourceSnapshotHash())) {
                throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                        "业务幂等回执与当前正式来源快照不一致");
            }
            return persistenceService.toResult(businessExisting);
        }
        MesTeamLeaderActiveOrderReleaseDossierSummary summary = buildSummary(
                batchPlan, inspectionPlan, lossPlan, sourceSnapshotHash);
        MesProcessPoolActiveOrderReleaseApplicationDO application = buildApplication(
                activeOrder, workOrder, requestKey, businessKey, sourceSnapshotHash, summary,
                blockers, leaderUserId, command.getApplyRemark());
        if (!blockers.isEmpty()) {
            throw new MesTeamLeaderActiveOrderReleaseBlockedException(application);
        }

        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(
                new EdhrBatchExecutionOpenOrCreateReqVO()
                        .setWorkOrderId(workOrder.getId())
                        .setBatchCode(batchCode)
                        .setRouteId(activeOrder.getRouteId())
                        .setRemark("生产组长申请放行资料自动创建批次执行"));
        Long batchExecutionId = requireCurrentBatch(batch, activeOrder, workOrder, batchCode);
        batchCommand.setBatchExecutionId(batchExecutionId);

        MesTeamLeaderActiveOrderReleaseBatchRecordWriteResult batchWrite =
                batchRecordWriter.write(batchPlan, batchExecutionId);
        MesTeamLeaderActiveOrderReleaseProcessInspectionWriteResult inspectionWrite =
                processInspectionWriter.write(inspectionPlan, batchExecutionId);
        MesTeamLeaderActiveOrderReleaseLossReportWriteResult lossWrite =
                lossReportWriter.write(lossPlan, batchExecutionId);
        List<MesTeamLeaderActiveOrderReleaseBlocker> writeBlockers = collectWriteBlockers(
                batchWrite, inspectionWrite, lossWrite);
        if (!writeBlockers.isEmpty()) {
            application.setBlockerSnapshotJson(JSON.toJSONString(writeBlockers));
            throw new MesTeamLeaderActiveOrderReleaseBlockedException(application);
        }
        validateWriteEvidence(batchPlan, inspectionPlan, lossPlan, batchWrite, inspectionWrite, lossWrite);

        List<MesProEdhrBatchExecutionTaskDO> currentTasks =
                batchTaskMapper.selectListByBatchExecutionId(batchExecutionId);
        List<MesTeamLeaderActiveOrderReleaseDocumentEvidence> documents = new ArrayList<>();
        documents.addAll(batchDocuments(batchExecutionId, batchPlan, batchWrite, currentTasks));
        documents.addAll(inspectionDocuments(batchExecutionId, inspectionPlan, inspectionWrite, currentTasks));
        if (lossWrite == null || lossWrite.getDocumentEvidence() == null) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                    "损耗报告 writer 未返回正式文档证据");
        }
        documents.addAll(lossWrite.getDocumentEvidence());
        MesTeamLeaderActiveOrderReleaseDossierCompletenessResult completeness = completenessChecker.check(
                new MesTeamLeaderActiveOrderReleaseDossierCompletenessCommand()
                        .setBatchExecutionId(batchExecutionId)
                        .setSourceSnapshotHash(sourceSnapshotHash)
                        .setDocuments(List.copyOf(documents))
                        .setReleaseApprovalRuleId(owner.rule().getId())
                        .setReleaseOwnerCandidateUserIds(owner.candidateUserIds()));
        if (completeness == null || !completeness.isComplete()
                || completeness.getBlockers() == null || !completeness.getBlockers().isEmpty()) {
            List<MesTeamLeaderActiveOrderReleaseBlocker> completenessBlockers = completeness == null
                    || completeness.getBlockers() == null || completeness.getBlockers().isEmpty()
                    ? List.of(blocker("DOSSIER_COMPLETENESS_BLOCKED", "DOSSIER", batchExecutionId,
                    "放行 dossier 完成性检查未通过", "请补齐正式文档、字段审计和双签证据"))
                    : completeness.getBlockers();
            application.setBlockerSnapshotJson(JSON.toJSONString(completenessBlockers));
            throw new MesTeamLeaderActiveOrderReleaseBlockedException(application);
        }

        MesProEdhrReleaseRespVO precheck = releaseService.precheck(
                new MesProEdhrReleasePrecheckReqVO().setBatchExecutionId(batchExecutionId));
        if (hasPrecheckBlocker(precheck)) {
            List<MesTeamLeaderActiveOrderReleaseBlocker> precheckBlockers = List.of(blocker(
                    "RELEASE_PRECHECK_BLOCKED", "RELEASE_TRANSACTION",
                    precheck == null ? null : precheck.getReleaseTransactionId(),
                    precheck == null ? "eDHR 放行预检无有效回执" : precheck.getPrecheckSummary(),
                    "请补齐 eDHR 放行预检失败项后重新申请"));
            application.setBlockerSnapshotJson(JSON.toJSONString(precheckBlockers));
            throw new MesTeamLeaderActiveOrderReleaseBlockedException(application);
        }
        MesProEdhrReleaseRespVO submitted = releaseService.submitForApproval(
                new MesProEdhrReleaseSubmitForApprovalCommand()
                        .setReleaseTransactionId(precheck.getReleaseTransactionId())
                        .setIdempotencyKey(businessKey)
                        .setSubmitReason(StrUtil.blankToDefault(StrUtil.trim(command.getApplyRemark()),
                                "生产组长申请生成放行资料，提交负责人审批")));
        if (submitted == null || submitted.getReleaseTransactionId() == null
                || submitted.getReleaseApprovalWorkTaskId() == null) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                    "eDHR 未返回正式 RELEASE_APPROVE 待办");
        }
        application.setApplicationStatus(STATUS_PENDING_RELEASE_APPROVAL)
                .setBatchExecutionId(batchExecutionId)
                .setReleaseTransactionId(submitted.getReleaseTransactionId())
                .setReleaseApprovalWorkTaskId(submitted.getReleaseApprovalWorkTaskId())
                .setLastPrecheckAt(submitted.getLastPrecheckAt() == null
                        ? precheck.getLastPrecheckAt() : submitted.getLastPrecheckAt())
                .setBlockerSnapshotJson(null);
        return persistenceService.persistPending(application);
    }

    private void validateApplyCommand(Long leaderUserId, MesTeamLeaderActiveOrderReleaseApplyCommand command) {
        if (leaderUserId == null || command == null || command.getActiveOrderId() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "activeOrderReleaseApply");
        }
        String remark = StrUtil.trim(command.getApplyRemark());
        if (remark != null && remark.length() > MAX_APPLY_REMARK_LENGTH) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "activeOrderReleaseApplyRemark");
        }
    }

    private String requireRequestIdempotencyKey(String rawKey) {
        String key = StrUtil.trim(rawKey);
        if (StrUtil.isBlank(key)) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "releaseApplyIdempotencyKey");
        }
        if (key.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "releaseApplyIdempotencyKeyLength");
        }
        return key;
    }

    private MesProcessPoolActiveOrderDO requireActiveOrder(Long activeOrderId, Long leaderUserId) {
        MesProcessPoolActiveOrderDO activeOrder = activeOrderMapper.selectByIdForUpdate(activeOrderId);
        if (activeOrder == null || !Objects.equals(leaderUserId, activeOrder.getLeaderUserId())
                || !STATUS_ACTIVE.equals(activeOrder.getActiveStatus()) || activeOrder.getRouteId() == null
                || activeOrder.getRouteVersionId() == null) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS, activeOrderId);
        }
        return activeOrder;
    }

    private MesProWorkOrderDO requireWorkOrder(Long workOrderId) {
        MesProWorkOrderDO workOrder = workOrderId == null ? null : workOrderMapper.selectByIdForUpdate(workOrderId);
        if (workOrder == null) {
            throw exception(PRO_WORK_ORDER_NOT_EXISTS);
        }
        if (workOrder.getProductId() == null) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                    "生产工单缺少正式产品，workOrderId=" + workOrderId);
        }
        return workOrder;
    }

    private String requireBatchCode(MesProWorkOrderDO workOrder) {
        String batchCode = StrUtil.trim(workOrder.getBatchCode());
        if (StrUtil.isBlank(batchCode)) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                    "生产工单缺少正式批号，workOrderId=" + workOrder.getId());
        }
        return batchCode;
    }

    private LockedMasterData requireLockedMasterData(
            MesProcessPoolActiveOrderDO activeOrder, MesProWorkOrderDO workOrder) {
        MesMdItemDO product = itemMapper.selectByIdForUpdate(workOrder.getProductId());
        MesProRouteDO route = routeMapper.selectByIdForUpdate(activeOrder.getRouteId());
        MesProRouteVersionDO routeVersion = routeVersionMapper.selectByIdForUpdate(activeOrder.getRouteVersionId());
        if (product == null || !Objects.equals(workOrder.getProductId(), product.getId())) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                    "生产工单缺少可锁定的正式产品，productId=" + workOrder.getProductId());
        }
        if (route == null || !Objects.equals(activeOrder.getRouteId(), route.getId())) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                    "活跃订单缺少可锁定的正式路线，routeId=" + activeOrder.getRouteId());
        }
        if (routeVersion == null || !Objects.equals(activeOrder.getRouteId(), routeVersion.getRouteId())
                || !Objects.equals(activeOrder.getRouteVersionId(), routeVersion.getId())
                || routeVersion.getPublishedTime() == null) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                    "活跃订单路线版本不是已发布冻结版本，routeVersionId="
                            + activeOrder.getRouteVersionId());
        }
        return new LockedMasterData(product, route, routeVersion);
    }

    private List<MesProcessPoolActiveOrderProcessSnapshotDO> requireSnapshots(
            MesProcessPoolActiveOrderDO activeOrder) {
        List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots =
                processSnapshotMapper.selectListByActiveOrderIdForUpdate(activeOrder.getId());
        if (snapshots == null || snapshots.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrder.getId());
        }
        Set<ProcessIdentity> identities = new LinkedHashSet<>();
        for (MesProcessPoolActiveOrderProcessSnapshotDO snapshot : snapshots) {
            if (snapshot == null || !Objects.equals(activeOrder.getId(), snapshot.getActiveOrderId())
                    || !Objects.equals(activeOrder.getWorkOrderId(), snapshot.getWorkOrderId())
                    || !Objects.equals(activeOrder.getRouteId(), snapshot.getRouteId())
                    || !Objects.equals(activeOrder.getRouteVersionId(), snapshot.getRouteVersionId())
                    || snapshot.getRouteProcessId() == null || snapshot.getProcessId() == null
                    || !identities.add(new ProcessIdentity(snapshot.getRouteProcessId(), snapshot.getProcessId()))) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrder.getId());
            }
        }
        return List.copyOf(snapshots);
    }

    private List<MesProcessPoolOrderProcessCompletionDO> requireFormalProductionCompletions(
            MesProcessPoolActiveOrderDO activeOrder,
            List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots) {
        List<MesProcessPoolOrderProcessCompletionDO> all =
                completionMapper.selectListByWorkOrderIds(List.of(activeOrder.getWorkOrderId()));
        List<MesProcessPoolOrderProcessCompletionDO> matched = new ArrayList<>();
        for (MesProcessPoolActiveOrderProcessSnapshotDO snapshot : snapshots) {
            List<MesProcessPoolOrderProcessCompletionDO> candidates = list(all).stream()
                    .filter(item -> item != null
                            && Objects.equals(activeOrder.getWorkOrderId(), item.getWorkOrderId())
                            && Objects.equals(snapshot.getRouteProcessId(), item.getRouteProcessId())
                            && Objects.equals(snapshot.getProcessId(), item.getProcessId()))
                    .toList();
            if (candidates.size() != 1 || !isFormalProductionComplete(candidates.get(0))) {
                throw progressRequired(activeOrder.getId(), snapshot, "production");
            }
            matched.add(candidates.get(0));
        }
        return List.copyOf(matched);
    }

    private boolean isFormalProductionComplete(MesProcessPoolOrderProcessCompletionDO completion) {
        return MesProcessPoolOrderProcessCompletionDO.STATUS_COMPLETED.equals(completion.getCompletionStatus())
                && MesProcessPoolOrderProcessCompletionDO.BACKFILL_STATUS_SUCCESS.equals(completion.getBackfillStatus())
                && completion.getBackfillExecutionId() != null
                && completion.getTargetQuantity() != null && completion.getTargetQuantity().signum() > 0
                && completion.getConfirmedQuantity() != null
                && completion.getConfirmedQuantity().compareTo(completion.getTargetQuantity()) >= 0
                && completion.getLastEventId() != null && completion.getLastReviewId() != null
                && StrUtil.isNotBlank(completion.getSourceEventIdsJson())
                && StrUtil.isNotBlank(completion.getSourceAllocationIdsJson())
                && StrUtil.isNotBlank(completion.getAggregateHash())
                && StrUtil.isNotBlank(completion.getBackfillIdempotencyKey());
    }

    private void requireFormalPqcCompletions(
            MesProcessPoolActiveOrderDO activeOrder,
            List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots) {
        List<MesPqcInspectionTaskDO> tasks = list(pqcTaskMapper.selectListByActiveOrderId(activeOrder.getId()));
        List<MesPqcProcessInspectionAggregateDetailDO> details =
                list(aggregateDetailMapper.selectListByActiveOrderId(activeOrder.getId()));
        for (MesProcessPoolActiveOrderProcessSnapshotDO snapshot : snapshots) {
            List<MesPqcInspectionTaskDO> confirmed = tasks.stream()
                    .filter(task -> task != null
                            && MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED.equals(task.getTaskStatus())
                            && Objects.equals(activeOrder.getId(), task.getActiveOrderId())
                            && Objects.equals(activeOrder.getWorkOrderId(), task.getWorkOrderId())
                            && Objects.equals(activeOrder.getRouteId(), task.getRouteId())
                            && Objects.equals(activeOrder.getRouteVersionId(), task.getRouteVersionId())
                            && Objects.equals(snapshot.getRouteProcessId(), task.getRouteProcessId())
                            && Objects.equals(snapshot.getProcessId(), task.getProcessId()))
                    .toList();
            boolean aggregatePresent = confirmed.stream().anyMatch(task -> details.stream().anyMatch(detail ->
                    detail != null && Objects.equals(task.getId(), detail.getPqcTaskId())
                            && Objects.equals(activeOrder.getId(), detail.getActiveOrderId())
                            && Objects.equals(activeOrder.getWorkOrderId(), detail.getWorkOrderId())
                            && Objects.equals(activeOrder.getRouteId(), detail.getRouteId())
                            && Objects.equals(activeOrder.getRouteVersionId(), detail.getRouteVersionId())
                            && Objects.equals(snapshot.getRouteProcessId(), detail.getRouteProcessId())
                            && Objects.equals(snapshot.getProcessId(), detail.getProcessId())));
            if (confirmed.isEmpty() || !aggregatePresent) {
                throw progressRequired(activeOrder.getId(), snapshot, "pqcConfirmed");
            }
        }
    }

    private ServiceException progressRequired(Long activeOrderId,
                                              MesProcessPoolActiveOrderProcessSnapshotDO snapshot,
                                              String progressType) {
        return exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_PROGRESS_REQUIRED,
                "activeOrderId=" + activeOrderId + "，routeProcessId=" + snapshot.getRouteProcessId()
                        + "，processId=" + snapshot.getProcessId() + "，formalProgress=" + progressType);
    }

    private List<MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand.ProcessSource> loadBatchRecordSources(
            MesProcessPoolActiveOrderDO activeOrder,
            List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots,
            List<MesProcessPoolOrderProcessCompletionDO> completions) {
        List<MesProProcessPoolEventDO> allEvents = list(
                eventMapper.selectProductionSubmitsByWorkOrderAndRoute(
                        activeOrder.getWorkOrderId(), activeOrder.getRouteId()));
        List<MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand.ProcessSource> sources = new ArrayList<>();
        for (int index = 0; index < snapshots.size(); index++) {
            MesProcessPoolActiveOrderProcessSnapshotDO snapshot = snapshots.get(index);
            List<MesProProcessPoolEventDO> events = allEvents.stream()
                    .filter(event -> event != null
                            && Objects.equals(snapshot.getRouteProcessId(), event.getRouteProcessId())
                            && Objects.equals(snapshot.getProcessId(), event.getProcessId()))
                    .toList();
            List<MesProcessPoolReportAllocationDO> allocations = events.stream()
                    .flatMap(event -> list(allocationMapper.selectListByEventId(event.getId())).stream())
                    .toList();
            List<MesProcessPoolSubmissionReviewDO> reviews = events.stream()
                    .flatMap(event -> list(reviewMapper.selectListByEventId(event.getId())).stream())
                    .toList();
            sources.add(new MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand.ProcessSource()
                    .setSnapshot(snapshot)
                    .setCompletion(completions.get(index))
                    .setSourceEvents(events)
                    .setAllocations(allocations)
                    .setReviews(reviews));
        }
        return List.copyOf(sources);
    }

    private void requirePlanResult(
            MesTeamLeaderActiveOrderReleaseBatchRecordPlan batchPlan,
            MesTeamLeaderActiveOrderReleaseProcessInspectionPlan inspectionPlan,
            MesTeamLeaderActiveOrderReleaseLossReportPlan lossPlan) {
        if (batchPlan == null || batchPlan.getCommand() == null || batchPlan.getPreparedProcesses() == null
                || batchPlan.getBlockers() == null || batchPlan.getSourceObjectIds() == null
                || batchPlan.getSourceValueHashes() == null || batchPlan.getSignatureEvidence() == null
                || inspectionPlan == null || inspectionPlan.getCommand() == null
                || inspectionPlan.getPreparedInspections() == null || inspectionPlan.getBlockers() == null
                || inspectionPlan.getSourceObjectIds() == null || inspectionPlan.getSourceValueHashes() == null
                || inspectionPlan.getSignatureEvidence() == null
                || lossPlan == null || lossPlan.getCommand() == null || lossPlan.getPreparedReports() == null
                || lossPlan.getBlockers() == null || lossPlan.getSourceObjectIds() == null
                || lossPlan.getSourceValueHashes() == null || lossPlan.getSignatureEvidence() == null) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                    "放行 writer 未返回完整的无副作用 plan");
        }
    }

    private ReleaseOwner resolveReleaseOwner(Long routeId) {
        MesProEdhrWorkTaskAssignmentRuleDO rule = assignmentRuleMapper.selectEnabledByScopeAndType(
                RULE_SCOPE_TYPE_ROUTE, routeId, TASK_TYPE_RELEASE_APPROVE);
        if (rule == null) {
            return new ReleaseOwner(null, null, null, List.of(),
                    blocker("RELEASE_OWNER_REQUIRED", "ROUTE", routeId,
                            "缺少正式 RELEASE_APPROVE 审批规则", "请配置路线级 RELEASE_APPROVE 责任人"));
        }
        try {
            MesProEdhrCandidateResolver.MesProEdhrCandidateContract candidate =
                    candidateResolver.resolveAssignmentRule(rule);
            List<Long> candidateUserIds = parseCandidateUserIds(candidate.userSnapshot());
            if (candidateUserIds.isEmpty()) {
                return new ReleaseOwner(rule, candidate.sourceType(), candidate.sourceId(), List.of(),
                        blocker("RELEASE_OWNER_REQUIRED", "ASSIGNMENT_RULE", rule.getId(),
                                "RELEASE_APPROVE 候选责任人为空", "请配置有效的放行候选责任人"));
            }
            return new ReleaseOwner(rule, candidate.sourceType(), candidate.sourceId(), candidateUserIds, null);
        } catch (ServiceException ex) {
            return new ReleaseOwner(rule, rule.getCandidateSourceType(), rule.getCandidateSourceId(), List.of(),
                    blocker("RELEASE_OWNER_REQUIRED", "ASSIGNMENT_RULE", rule.getId(),
                            "RELEASE_APPROVE 候选责任人配置无效", "请修复放行候选责任人配置"));
        }
    }

    private List<Long> parseCandidateUserIds(String snapshot) {
        if (StrUtil.isBlank(snapshot)) {
            return List.of();
        }
        try {
            return java.util.Arrays.stream(snapshot.split(","))
                    .map(String::trim)
                    .filter(StrUtil::isNotBlank)
                    .map(Long::valueOf)
                    .filter(id -> id > 0)
                    .distinct()
                    .sorted()
                    .toList();
        } catch (NumberFormatException ex) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                    "RELEASE_APPROVE 候选责任人快照格式无效");
        }
    }

    private List<MesTeamLeaderActiveOrderReleaseBlocker> collectPlanningBlockers(
            MesProcessPoolActiveOrderDO activeOrder,
            MesProWorkOrderDO workOrder,
            MesTeamLeaderActiveOrderReleaseBatchRecordPlan batchPlan,
            MesTeamLeaderActiveOrderReleaseProcessInspectionPlan inspectionPlan,
            MesTeamLeaderActiveOrderReleaseLossReportPlan lossPlan,
            ReleaseOwner owner) {
        List<MesTeamLeaderActiveOrderReleaseBlocker> blockers = new ArrayList<>();
        if (abnormalStateService.hasOpenAbnormal(activeOrder.getWorkOrderId())) {
            blockers.add(blocker("ABNORMAL_OPEN", "WORK_ORDER", activeOrder.getWorkOrderId(),
                    "生产工单存在未关闭异常", "请先关闭异常再申请放行").setObjectCode(workOrder.getCode()));
        }
        blockers.addAll(batchPlan.getBlockers());
        blockers.addAll(inspectionPlan.getBlockers());
        blockers.addAll(lossPlan.getBlockers());
        if (owner.blocker() != null) {
            blockers.add(owner.blocker());
        }
        return List.copyOf(blockers);
    }

    private MesTeamLeaderActiveOrderReleaseDossierSummary buildSummary(
            MesTeamLeaderActiveOrderReleaseBatchRecordPlan batchPlan,
            MesTeamLeaderActiveOrderReleaseProcessInspectionPlan inspectionPlan,
            MesTeamLeaderActiveOrderReleaseLossReportPlan lossPlan,
            String sourceSnapshotHash) {
        Set<String> signatures = new LinkedHashSet<>();
        batchPlan.getSignatureEvidence().forEach(item -> signatures.add(String.valueOf(item.getSignatureId())));
        inspectionPlan.getSignatureEvidence().forEach(item -> signatures.add(String.valueOf(item.getSignatureId())));
        lossPlan.getSignatureEvidence().forEach(item -> signatures.add(String.valueOf(item.getSignatureId())));
        return new MesTeamLeaderActiveOrderReleaseDossierSummary()
                .setBatchRecordCount(batchPlan.getPreparedProcesses().size())
                .setProcessInspectionFormCount(inspectionPlan.getPreparedInspections().size())
                .setLossReportFormCount(lossPlan.getPreparedReports().size())
                .setSignatureEvidenceCount(signatures.size())
                .setSourceSnapshotHash(sourceSnapshotHash);
    }

    private MesProcessPoolActiveOrderReleaseApplicationDO buildApplication(
            MesProcessPoolActiveOrderDO activeOrder,
            MesProWorkOrderDO workOrder,
            String requestKey,
            String businessKey,
            String sourceSnapshotHash,
            MesTeamLeaderActiveOrderReleaseDossierSummary summary,
            List<MesTeamLeaderActiveOrderReleaseBlocker> blockers,
            Long leaderUserId,
            String remark) {
        return new MesProcessPoolActiveOrderReleaseApplicationDO()
                .setActiveOrderId(activeOrder.getId())
                .setWorkOrderId(activeOrder.getWorkOrderId())
                .setWorkOrderCode(workOrder.getCode())
                .setRouteId(activeOrder.getRouteId())
                .setRouteVersionId(activeOrder.getRouteVersionId())
                .setProductId(workOrder.getProductId())
                .setBatchCode(workOrder.getBatchCode())
                .setApplicationStatus(STATUS_BLOCKED)
                .setSourceSnapshotHash(sourceSnapshotHash)
                .setRequestIdempotencyKey(requestKey)
                .setBusinessIdempotencyKey(businessKey)
                .setBlockerSnapshotJson(blockers.isEmpty() ? null : JSON.toJSONString(blockers))
                .setDossierSummaryJson(JSON.toJSONString(summary))
                .setAppliedBy(leaderUserId)
                .setAppliedAt(LocalDateTime.now())
                .setRemark(StrUtil.trim(remark));
    }

    private Long requireCurrentBatch(EdhrBatchExecutionRespVO batch,
                                     MesProcessPoolActiveOrderDO activeOrder,
                                     MesProWorkOrderDO workOrder,
                                     String batchCode) {
        if (batch == null || batch.getId() == null
                || !Objects.equals(workOrder.getId(), batch.getWorkOrderId())
                || !Objects.equals(batchCode, batch.getBatchCode())
                || !Objects.equals(workOrder.getProductId(), batch.getProductId())
                || !Objects.equals(activeOrder.getRouteId(), batch.getRouteId())
                || !Objects.equals(activeOrder.getRouteVersionId(), batch.getRouteVersionId())) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                    "eDHR 未打开当前工单、批号和路线版本的唯一批次");
        }
        return batch.getId();
    }

    private List<MesTeamLeaderActiveOrderReleaseBlocker> collectWriteBlockers(
            MesTeamLeaderActiveOrderReleaseBatchRecordWriteResult batchWrite,
            MesTeamLeaderActiveOrderReleaseProcessInspectionWriteResult inspectionWrite,
            MesTeamLeaderActiveOrderReleaseLossReportWriteResult lossWrite) {
        if (batchWrite == null || inspectionWrite == null || lossWrite == null
                || batchWrite.getBlockers() == null || inspectionWrite.getBlockers() == null
                || lossWrite.getBlockers() == null) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                    "放行 writer 未返回完整写入结果");
        }
        List<MesTeamLeaderActiveOrderReleaseBlocker> blockers = new ArrayList<>();
        blockers.addAll(batchWrite.getBlockers());
        blockers.addAll(inspectionWrite.getBlockers());
        blockers.addAll(lossWrite.getBlockers());
        return List.copyOf(blockers);
    }

    private void validateWriteEvidence(
            MesTeamLeaderActiveOrderReleaseBatchRecordPlan batchPlan,
            MesTeamLeaderActiveOrderReleaseProcessInspectionPlan inspectionPlan,
            MesTeamLeaderActiveOrderReleaseLossReportPlan lossPlan,
            MesTeamLeaderActiveOrderReleaseBatchRecordWriteResult batchWrite,
            MesTeamLeaderActiveOrderReleaseProcessInspectionWriteResult inspectionWrite,
            MesTeamLeaderActiveOrderReleaseLossReportWriteResult lossWrite) {
        boolean batchConsistent = "BATCH_RECORD".equals(batchWrite.getDocumentType())
                && Objects.equals(batchPlan.getSourceObjectIds(), batchWrite.getSourceObjectIds())
                && Objects.equals(batchPlan.getSourceValueHashes(), batchWrite.getSourceValueHashes())
                && Objects.equals(batchPlan.getSignatureEvidence(), batchWrite.getSignatureEvidence());
        boolean inspectionConsistent = "PROCESS_INSPECTION".equals(inspectionWrite.getDocumentType())
                && Objects.equals(inspectionPlan.getSourceObjectIds(), inspectionWrite.getSourceObjectIds())
                && Objects.equals(inspectionPlan.getSourceValueHashes(), inspectionWrite.getSourceValueHashes())
                && Objects.equals(inspectionPlan.getSignatureEvidence(), inspectionWrite.getSignatureEvidence());
        boolean lossConsistent = "LOSS_REPORT".equals(lossWrite.getDocumentType())
                && Objects.equals(lossPlan.getSourceObjectIds(), lossWrite.getSourceObjectIds())
                && Objects.equals(lossPlan.getSourceValueHashes(), lossWrite.getSourceValueHashes())
                && Objects.equals(lossPlan.getSignatureEvidence(), lossWrite.getSignatureEvidence());
        if (!batchConsistent || !inspectionConsistent || !lossConsistent) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                    "writer 写入回执与已哈希 plan 正式证据不一致");
        }
    }

    private List<MesTeamLeaderActiveOrderReleaseDocumentEvidence> batchDocuments(
            Long batchExecutionId,
            MesTeamLeaderActiveOrderReleaseBatchRecordPlan plan,
            MesTeamLeaderActiveOrderReleaseBatchRecordWriteResult write,
            List<MesProEdhrBatchExecutionTaskDO> tasks) {
        requireEvidenceCardinality(plan.getPreparedProcesses().size(), write.getBatchRecordExecutionIds(),
                write.getFieldAuditIds(), "批记录");
        List<MesTeamLeaderActiveOrderReleaseSignatureEvidence> signatures = write.getSignatureEvidence().stream()
                .map(this::commonSignature).toList();
        List<MesTeamLeaderActiveOrderReleaseDocumentEvidence> documents = new ArrayList<>();
        for (int index = 0; index < plan.getPreparedProcesses().size(); index++) {
            MesTeamLeaderActiveOrderReleaseBatchRecordPlan.PreparedProcess prepared =
                    plan.getPreparedProcesses().get(index);
            MesProEdhrBatchExecutionTaskDO task = matchBatchTask(batchExecutionId, tasks, prepared);
            documents.add(document("BATCH_RECORD", batchExecutionId, task,
                    write.getBatchRecordExecutionIds().get(index), write.getFieldAuditIds().get(index),
                    prepared.getRules().size(), prepared.getRules().size(), write.getSourceObjectIds(),
                    write.getSourceValueHashes(), signatures, plan.getCommand().getSourceSnapshotHash(),
                    snapshotHashes(task)));
        }
        return documents;
    }

    private List<MesTeamLeaderActiveOrderReleaseDocumentEvidence> inspectionDocuments(
            Long batchExecutionId,
            MesTeamLeaderActiveOrderReleaseProcessInspectionPlan plan,
            MesTeamLeaderActiveOrderReleaseProcessInspectionWriteResult write,
            List<MesProEdhrBatchExecutionTaskDO> tasks) {
        requireEvidenceCardinality(plan.getPreparedInspections().size(), write.getBatchRecordExecutionIds(),
                write.getFieldAuditIds(), "过程检验");
        List<MesTeamLeaderActiveOrderReleaseSignatureEvidence> signatures = write.getSignatureEvidence().stream()
                .map(this::commonSignature).toList();
        List<MesTeamLeaderActiveOrderReleaseDocumentEvidence> documents = new ArrayList<>();
        for (int index = 0; index < plan.getPreparedInspections().size(); index++) {
            MesTeamLeaderActiveOrderReleaseProcessInspectionPlan.PreparedInspection prepared =
                    plan.getPreparedInspections().get(index);
            MesProEdhrBatchExecutionTaskDO task = matchInspectionTask(batchExecutionId, tasks, prepared);
            int required = prepared.getMappedValues().size();
            documents.add(document("PROCESS_INSPECTION", batchExecutionId, task,
                    write.getBatchRecordExecutionIds().get(index), write.getFieldAuditIds().get(index),
                    required, required, write.getSourceObjectIds(), write.getSourceValueHashes(), signatures,
                    plan.getCommand().getSourceSnapshotHash(), snapshotHashes(task)));
        }
        return documents;
    }

    private void requireEvidenceCardinality(int required, List<Long> executionIds,
                                            List<Long> auditIds, String documentName) {
        if (required <= 0 || executionIds == null || auditIds == null
                || executionIds.size() != required || auditIds.size() != required
                || executionIds.stream().anyMatch(Objects::isNull)
                || auditIds.stream().anyMatch(Objects::isNull)) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                    documentName + " writer 正式 execution 与字段审计证据数量不一致");
        }
    }

    private MesProEdhrBatchExecutionTaskDO matchBatchTask(
            Long batchExecutionId, List<MesProEdhrBatchExecutionTaskDO> tasks,
            MesTeamLeaderActiveOrderReleaseBatchRecordPlan.PreparedProcess prepared) {
        MesProcessPoolActiveOrderProcessSnapshotDO snapshot = prepared.getSource().getSnapshot();
        MesProRouteFlowProcessBatchRecordDO binding = prepared.getBinding();
        return requireUniqueTask(list(tasks).stream().filter(task -> task != null
                && Objects.equals(batchExecutionId, task.getBatchExecutionId())
                && Objects.equals(snapshot.getRouteProcessId(), task.getRouteProcessId())
                && Objects.equals(snapshot.getProcessId(), task.getProcessId())
                && Objects.equals(binding.getBatchRecordReportId(), task.getBatchRecordReportId())
                && Objects.equals(binding.getBatchRecordDefinitionId(), task.getBatchRecordDefinitionId())
                && Objects.equals(binding.getBatchRecordVersionId(), task.getBatchRecordVersionId())
                && Objects.equals(binding.getId(), task.getRouteBindingId())
                && "BATCH_RECORD".equals(task.getRecordCategory())
                && !"PROCESS_INSPECTION".equals(task.getFormSlotType())
                && !"LOSS_REPORT".equals(task.getFormSlotType())).toList(), "批记录");
    }

    private MesProEdhrBatchExecutionTaskDO matchInspectionTask(
            Long batchExecutionId, List<MesProEdhrBatchExecutionTaskDO> tasks,
            MesTeamLeaderActiveOrderReleaseProcessInspectionPlan.PreparedInspection prepared) {
        MesPqcInspectionTaskDO source = prepared.getSource().getTask();
        MesProRouteFlowProcessBatchRecordDO binding = prepared.getBinding();
        return requireUniqueTask(list(tasks).stream().filter(task -> task != null
                && Objects.equals(batchExecutionId, task.getBatchExecutionId())
                && Objects.equals(source.getRouteProcessId(), task.getRouteProcessId())
                && Objects.equals(source.getProcessId(), task.getProcessId())
                && Objects.equals(binding.getBatchRecordReportId(), task.getBatchRecordReportId())
                && Objects.equals(binding.getBatchRecordDefinitionId(), task.getBatchRecordDefinitionId())
                && Objects.equals(binding.getBatchRecordVersionId(), task.getBatchRecordVersionId())
                && Objects.equals(binding.getId(), task.getRouteBindingId())
                && "PROCESS_INSPECTION".equals(task.getFormSlotType())
                && "INTERNAL_RECORD".equals(task.getRecordCategory())).toList(), "过程检验");
    }

    private MesProEdhrBatchExecutionTaskDO requireUniqueTask(
            List<MesProEdhrBatchExecutionTaskDO> matches, String documentName) {
        if (matches.size() != 1 || matches.get(0).getId() == null) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                    "当前 eDHR 批次缺少唯一" + documentName + "任务");
        }
        return matches.get(0);
    }

    private MesTeamLeaderActiveOrderReleaseDocumentEvidence document(
            String documentType, Long batchExecutionId, MesProEdhrBatchExecutionTaskDO task,
            Long executionId, Long auditId, int requiredFields, int auditedFields,
            List<Long> sourceObjectIds, List<String> sourceValueHashes,
            List<MesTeamLeaderActiveOrderReleaseSignatureEvidence> signatures,
            String sourceSnapshotHash, List<String> targetSnapshotHashes) {
        return new MesTeamLeaderActiveOrderReleaseDocumentEvidence()
                .setDocumentType(documentType)
                .setBatchExecutionId(batchExecutionId)
                .setBatchExecutionTaskId(task.getId())
                .setBatchRecordExecutionIds(List.of(executionId))
                .setTargetReportIds(List.of(task.getBatchRecordReportId()))
                .setTargetDefinitionIds(List.of(task.getBatchRecordDefinitionId()))
                .setTargetVersionIds(List.of(task.getBatchRecordVersionId()))
                .setTargetSnapshotHashes(targetSnapshotHashes)
                .setFieldAuditIds(List.of(auditId))
                .setRequiredFieldCount(requiredFields)
                .setAuditedRequiredFieldCount(auditedFields)
                .setSourceObjectIds(sourceObjectIds)
                .setSourceValueHashes(sourceValueHashes)
                .setSignatureEvidence(signatures)
                .setSourceSnapshotHash(sourceSnapshotHash)
                .setSourceConsistent(true);
    }

    private List<String> snapshotHashes(MesProEdhrBatchExecutionTaskDO task) {
        List<String> hashes = new ArrayList<>();
        if (StrUtil.isNotBlank(task.getRouteBindingSnapshotHash())) {
            hashes.add(task.getRouteBindingSnapshotHash());
        }
        if (StrUtil.isNotBlank(task.getSlotConfigSnapshotHash())) {
            hashes.add(task.getSlotConfigSnapshotHash());
        }
        return List.copyOf(hashes);
    }

    private MesTeamLeaderActiveOrderReleaseSignatureEvidence commonSignature(
            MesTeamLeaderActiveOrderReleaseBatchRecordWriteResult.SignatureEvidence source) {
        return new MesTeamLeaderActiveOrderReleaseSignatureEvidence()
                .setRole(source.getRole()).setSourceType(source.getSourceType()).setSourceId(source.getSourceId())
                .setSignatureId(source.getSignatureId()).setUserId(source.getUserId())
                .setSignedAt(source.getSignedAt()).setEvidenceHash(source.getEvidenceHash());
    }

    private MesTeamLeaderActiveOrderReleaseSignatureEvidence commonSignature(
            MesTeamLeaderActiveOrderReleaseProcessInspectionWriteResult.SignatureEvidence source) {
        return new MesTeamLeaderActiveOrderReleaseSignatureEvidence()
                .setRole(source.getRole()).setSourceType(source.getSourceType()).setSourceId(source.getSourceId())
                .setSignatureId(source.getSignatureId()).setUserId(source.getUserId())
                .setSignedAt(source.getSignedAt()).setEvidenceHash(source.getEvidenceHash());
    }

    private boolean hasPrecheckBlocker(MesProEdhrReleaseRespVO precheck) {
        return precheck == null || precheck.getReleaseTransactionId() == null
                || positive(precheck.getBlockingCheckCount()) || positive(precheck.getFailedCheckCount());
    }

    private boolean positive(Integer value) {
        return value != null && value > 0;
    }

    private MesTeamLeaderActiveOrderReleaseBlocker blocker(
            String type, String objectType, Object objectId, String reason, String suggestion) {
        return new MesTeamLeaderActiveOrderReleaseBlocker()
                .setBlockerType(type)
                .setObjectType(objectType)
                .setObjectId(objectId == null ? null : String.valueOf(objectId))
                .setReason(reason)
                .setSuggestion(suggestion);
    }

    private <T> List<T> list(List<T> values) {
        return values == null ? List.of() : values;
    }

    private record ProcessIdentity(Long routeProcessId, Long processId) {
    }

    private record LockedMasterData(MesMdItemDO product, MesProRouteDO route,
                                    MesProRouteVersionDO routeVersion) {
    }

    private record ReleaseOwner(
            MesProEdhrWorkTaskAssignmentRuleDO rule,
            String sourceType,
            Long sourceId,
            List<Long> candidateUserIds,
            MesTeamLeaderActiveOrderReleaseBlocker blocker) {
    }
}
