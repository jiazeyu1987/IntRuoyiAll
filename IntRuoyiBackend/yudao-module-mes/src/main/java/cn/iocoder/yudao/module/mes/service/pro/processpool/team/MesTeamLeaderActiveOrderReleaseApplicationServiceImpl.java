package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionOpenOrCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleasePrecheckReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolOrderProcessCompletionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolOrderProcessCompletionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseSubmitForApprovalCommand;
import com.alibaba.fastjson.JSON;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_PROGRESS_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_WORK_ORDER_NOT_EXISTS;

@Service
@Validated
public class MesTeamLeaderActiveOrderReleaseApplicationServiceImpl
        implements MesTeamLeaderActiveOrderReleaseApplicationService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String APPLICATION_STATUS_BLOCKED = "BLOCKED";
    private static final String APPLICATION_STATUS_PENDING_RELEASE_APPROVAL = "PENDING_RELEASE_APPROVAL";
    private static final String USE_TYPE_BATCH = "BATCH";
    private static final String RECORD_CATEGORY_BATCH_RECORD = "BATCH_RECORD";
    private static final String PROCESS_INSPECTION = "PROCESS_INSPECTION";
    private static final String LOSS_REPORT = "LOSS_REPORT";
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final int PROGRESS_SCALE = 6;

    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesProWorkOrderMapper workOrderMapper;
    private final MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;
    private final MesProcessPoolOrderProcessCompletionMapper completionMapper;
    private final MesPqcInspectionTaskMapper pqcInspectionTaskMapper;
    private final MesWorkOrderAbnormalStateService abnormalStateService;
    private final MesProRouteFlowProcessBatchRecordMapper routeFlowProcessBatchRecordMapper;
    private final MesPqcProcessInspectionAggregateDetailMapper aggregateDetailMapper;
    private final MesProEdhrBatchExecutionService batchExecutionService;
    private final MesProEdhrReleaseService releaseService;
    private final MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper;

    public MesTeamLeaderActiveOrderReleaseApplicationServiceImpl(
            MesProcessPoolActiveOrderMapper activeOrderMapper,
            MesProWorkOrderMapper workOrderMapper,
            MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper,
            MesProcessPoolOrderProcessCompletionMapper completionMapper,
            MesPqcInspectionTaskMapper pqcInspectionTaskMapper,
            MesWorkOrderAbnormalStateService abnormalStateService,
            MesProRouteFlowProcessBatchRecordMapper routeFlowProcessBatchRecordMapper,
            MesPqcProcessInspectionAggregateDetailMapper aggregateDetailMapper,
            MesProEdhrBatchExecutionService batchExecutionService,
            MesProEdhrReleaseService releaseService,
            MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper) {
        this.activeOrderMapper = activeOrderMapper;
        this.workOrderMapper = workOrderMapper;
        this.processSnapshotMapper = processSnapshotMapper;
        this.completionMapper = completionMapper;
        this.pqcInspectionTaskMapper = pqcInspectionTaskMapper;
        this.abnormalStateService = abnormalStateService;
        this.routeFlowProcessBatchRecordMapper = routeFlowProcessBatchRecordMapper;
        this.aggregateDetailMapper = aggregateDetailMapper;
        this.batchExecutionService = batchExecutionService;
        this.releaseService = releaseService;
        this.applicationMapper = applicationMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesTeamLeaderActiveOrderReleaseApplicationResult apply(
            Long leaderUserId, MesTeamLeaderActiveOrderReleaseApplyCommand command) {
        if (leaderUserId == null || command == null || command.getActiveOrderId() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "activeOrderReleaseApply");
        }
        String requestIdempotencyKey = requireRequestIdempotencyKey(command.getIdempotencyKey());
        MesProcessPoolActiveOrderReleaseApplicationDO requestExisting =
                applicationMapper.selectByRequestIdempotencyKey(command.getActiveOrderId(), requestIdempotencyKey);
        if (requestExisting != null) {
            return toResult(requestExisting);
        }

        MesProcessPoolActiveOrderDO activeOrder = requireActiveOrderForApply(command.getActiveOrderId(), leaderUserId);
        MesProWorkOrderDO workOrder = requireWorkOrder(activeOrder.getWorkOrderId());
        String batchCode = requireBatchCode(workOrder);
        List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots = requireSnapshots(activeOrder);
        ActiveOrderProgress progress = calculateProgress(activeOrder, snapshots);
        requireProgressComplete(activeOrder.getId(), progress);

        List<MesTeamLeaderActiveOrderReleaseBlocker> blockers = new ArrayList<>();
        if (abnormalStateService.hasOpenAbnormal(activeOrder.getWorkOrderId())) {
            blockers.add(blocker("ABNORMAL_OPEN", "WORK_ORDER", activeOrder.getWorkOrderId(),
                    workOrder.getCode(), "生产工单存在未关闭异常", "请先关闭异常再申请放行"));
        }
        SourceInspection sourceInspection = inspectFormalSources(activeOrder, snapshots, blockers);
        String sourceSnapshotHash = buildSourceSnapshotHash(activeOrder, workOrder, snapshots, sourceInspection, blockers);
        MesTeamLeaderActiveOrderReleaseDossierSummary summary = new MesTeamLeaderActiveOrderReleaseDossierSummary()
                .setBatchRecordCount(sourceInspection.officialBatchRecords().size())
                .setProcessInspectionFormCount(sourceInspection.processInspectionBindings().size())
                .setLossReportFormCount(sourceInspection.lossReportBindings().size())
                .setSignatureEvidenceCount(0)
                .setSourceSnapshotHash(sourceSnapshotHash);
        String businessIdempotencyKey = buildBusinessIdempotencyKey(activeOrder, sourceSnapshotHash);

        MesProcessPoolActiveOrderReleaseApplicationDO businessExisting =
                applicationMapper.selectByBusinessIdempotencyKey(activeOrder.getId(), businessIdempotencyKey);
        if (businessExisting != null) {
            return toResult(businessExisting);
        }
        if (!blockers.isEmpty()) {
            return toResult(insertApplication(activeOrder, workOrder, requestIdempotencyKey, businessIdempotencyKey,
                    sourceSnapshotHash, APPLICATION_STATUS_BLOCKED, summary, blockers, null, null, null,
                    leaderUserId, command.getApplyRemark(), null));
        }

        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(workOrder.getId())
                .setBatchCode(batchCode)
                .setRouteId(activeOrder.getRouteId())
                .setRemark("生产组长申请放行资料自动创建批次执行"));
        MesProEdhrReleaseRespVO precheck = releaseService.precheck(new MesProEdhrReleasePrecheckReqVO()
                .setBatchExecutionId(batch.getId()));
        if (hasPrecheckBlocker(precheck)) {
            blockers.add(blocker("RELEASE_PRECHECK_BLOCKED", "RELEASE_TRANSACTION",
                    precheck.getReleaseTransactionId(), precheck.getReleaseCode(),
                    precheck.getPrecheckSummary(), "请补齐 eDHR 放行预检失败项后重新申请"));
            return toResult(insertApplication(activeOrder, workOrder, requestIdempotencyKey, businessIdempotencyKey,
                    sourceSnapshotHash, APPLICATION_STATUS_BLOCKED, summary, blockers, batch.getId(),
                    precheck.getReleaseTransactionId(), null, leaderUserId, command.getApplyRemark(),
                    precheck.getLastPrecheckAt()));
        }
        MesProEdhrReleaseRespVO submitted = releaseService.submitForApproval(
                new MesProEdhrReleaseSubmitForApprovalCommand()
                        .setReleaseTransactionId(precheck.getReleaseTransactionId())
                        .setIdempotencyKey(requestIdempotencyKey)
                        .setSubmitReason(StrUtil.blankToDefault(StrUtil.trim(command.getApplyRemark()),
                                "生产组长申请生成放行资料，提交负责人审批")));
        return toResult(insertApplication(activeOrder, workOrder, requestIdempotencyKey, businessIdempotencyKey,
                sourceSnapshotHash, APPLICATION_STATUS_PENDING_RELEASE_APPROVAL, summary, List.of(), batch.getId(),
                submitted.getReleaseTransactionId(), submitted.getReleaseApprovalWorkTaskId(), leaderUserId,
                command.getApplyRemark(), submitted.getLastPrecheckAt()));
    }

    private MesProcessPoolActiveOrderDO requireActiveOrderForApply(Long activeOrderId, Long leaderUserId) {
        MesProcessPoolActiveOrderDO activeOrder = activeOrderMapper.selectByIdForUpdate(activeOrderId);
        if (activeOrder == null
                || !Objects.equals(activeOrder.getLeaderUserId(), leaderUserId)
                || !STATUS_ACTIVE.equals(activeOrder.getActiveStatus())) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS, activeOrderId);
        }
        return activeOrder;
    }

    private MesProWorkOrderDO requireWorkOrder(Long workOrderId) {
        MesProWorkOrderDO workOrder = workOrderId == null ? null : workOrderMapper.selectById(workOrderId);
        if (workOrder == null) {
            throw exception(PRO_WORK_ORDER_NOT_EXISTS);
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

    private List<MesProcessPoolActiveOrderProcessSnapshotDO> requireSnapshots(MesProcessPoolActiveOrderDO activeOrder) {
        List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots =
                processSnapshotMapper.selectListByActiveOrderId(activeOrder.getId());
        if (snapshots == null || snapshots.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrder.getId());
        }
        for (MesProcessPoolActiveOrderProcessSnapshotDO snapshot : snapshots) {
            if (!Objects.equals(activeOrder.getWorkOrderId(), snapshot.getWorkOrderId())
                    || !Objects.equals(activeOrder.getRouteId(), snapshot.getRouteId())
                    || !Objects.equals(activeOrder.getRouteVersionId(), snapshot.getRouteVersionId())
                    || snapshot.getRouteProcessId() == null
                    || snapshot.getProcessId() == null) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrder.getId());
            }
        }
        return snapshots;
    }

    private ActiveOrderProgress calculateProgress(MesProcessPoolActiveOrderDO activeOrder,
                                                  List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots) {
        Set<ProcessIdentity> processIdentities = snapshots.stream()
                .map(snapshot -> new ProcessIdentity(snapshot.getRouteProcessId(), snapshot.getProcessId()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (processIdentities.size() != snapshots.size()) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrder.getId());
        }
        Set<ProcessIdentity> completedProcesses = completionMapper
                .selectListByWorkOrderIds(List.of(activeOrder.getWorkOrderId()))
                .stream()
                .filter(completion -> MesProcessPoolOrderProcessCompletionDO.STATUS_COMPLETED
                        .equals(completion.getCompletionStatus()))
                .filter(completion -> Objects.equals(activeOrder.getWorkOrderId(), completion.getWorkOrderId()))
                .map(completion -> new ProcessIdentity(completion.getRouteProcessId(), completion.getProcessId()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<ProcessIdentity> inspectedProcesses = pqcInspectionTaskMapper
                .selectListByActiveOrderId(activeOrder.getId())
                .stream()
                .filter(task -> MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED.equals(task.getTaskStatus())
                        || MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED.equals(task.getTaskStatus()))
                .map(task -> new ProcessIdentity(task.getRouteProcessId(), task.getProcessId()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        int total = processIdentities.size();
        long productionCompleted = processIdentities.stream().filter(completedProcesses::contains).count();
        long inspectionCompleted = processIdentities.stream().filter(inspectedProcesses::contains).count();
        return new ActiveOrderProgress(toProgressPercent(productionCompleted, total),
                toProgressPercent(inspectionCompleted, total));
    }

    private void requireProgressComplete(Long activeOrderId, ActiveOrderProgress progress) {
        if (progress.productionProgressPercent().compareTo(ONE_HUNDRED) < 0
                || progress.inspectionProgressPercent().compareTo(ONE_HUNDRED) < 0) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_PROGRESS_REQUIRED,
                    "activeOrderId=" + activeOrderId
                            + "，productionProgressPercent=" + progress.productionProgressPercent()
                            + "，inspectionProgressPercent=" + progress.inspectionProgressPercent());
        }
    }

    private SourceInspection inspectFormalSources(MesProcessPoolActiveOrderDO activeOrder,
                                                  List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots,
                                                  List<MesTeamLeaderActiveOrderReleaseBlocker> blockers) {
        List<Long> routeProcessIds = snapshots.stream()
                .map(MesProcessPoolActiveOrderProcessSnapshotDO::getRouteProcessId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<MesProRouteFlowProcessBatchRecordDO> bindings =
                routeFlowProcessBatchRecordMapper.selectListByRouteProcessIdsAndUseType(routeProcessIds, USE_TYPE_BATCH);
        List<MesProRouteFlowProcessBatchRecordDO> officialBatchRecords = bindings.stream()
                .filter(this::isOfficialBatchRecord)
                .toList();
        Map<Long, List<MesProRouteFlowProcessBatchRecordDO>> officialByRouteProcessId =
                officialBatchRecords.stream()
                        .filter(binding -> binding.getRouteProcessId() != null)
                        .collect(Collectors.groupingBy(MesProRouteFlowProcessBatchRecordDO::getRouteProcessId,
                                LinkedHashMap::new, Collectors.toList()));
        for (MesProcessPoolActiveOrderProcessSnapshotDO snapshot : snapshots) {
            if (officialByRouteProcessId.getOrDefault(snapshot.getRouteProcessId(), List.of()).isEmpty()) {
                blockers.add(blocker("BATCH_RECORD_SOURCE_REQUIRED", "ROUTE_PROCESS",
                        snapshot.getRouteProcessId(), null,
                        "工序缺少正式批记录绑定，routeProcessId=" + snapshot.getRouteProcessId(),
                        "请在工序设置中绑定正式批记录表单"));
            }
        }

        List<MesProRouteFlowProcessBatchRecordDO> processInspectionBindings = bindings.stream()
                .filter(binding -> PROCESS_INSPECTION.equals(binding.getFormSlotType()))
                .toList();
        List<MesPqcProcessInspectionAggregateDetailDO> aggregateDetails =
                aggregateDetailMapper.selectListByActiveOrderId(activeOrder.getId());
        if (processInspectionBindings.isEmpty()) {
            blockers.add(blocker("PROCESS_INSPECTION_SOURCE_REQUIRED", "ACTIVE_ORDER", activeOrder.getId(), null,
                    "当前活跃订单缺少正式过程检验单绑定", "请配置过程检验单正式来源后重新申请"));
        } else if (aggregateDetails.isEmpty()) {
            blockers.add(blocker("PROCESS_INSPECTION_SOURCE_REQUIRED", "ACTIVE_ORDER", activeOrder.getId(), null,
                    "当前活跃订单缺少已确认的过程检验汇集明细", "请由 PQC 组长确认过程检验后重新申请"));
        }

        List<MesProRouteFlowProcessBatchRecordDO> lossReportBindings = bindings.stream()
                .filter(binding -> LOSS_REPORT.equals(binding.getFormSlotType()))
                .toList();
        if (!lossReportBindings.isEmpty()) {
            blockers.add(blocker("LOSS_REPORT_SOURCE_REQUIRED", "ACTIVE_ORDER", activeOrder.getId(), null,
                    "损耗单正式承载已配置，但当前版本尚未确认可回填的正式损耗来源",
                    "请补齐损耗单正式来源映射后重新申请"));
        }
        return new SourceInspection(bindings, officialBatchRecords, processInspectionBindings,
                lossReportBindings, aggregateDetails);
    }

    private boolean isOfficialBatchRecord(MesProRouteFlowProcessBatchRecordDO binding) {
        return RECORD_CATEGORY_BATCH_RECORD.equals(binding.getRecordCategory())
                && !PROCESS_INSPECTION.equals(binding.getFormSlotType())
                && !LOSS_REPORT.equals(binding.getFormSlotType());
    }

    private String buildSourceSnapshotHash(MesProcessPoolActiveOrderDO activeOrder,
                                           MesProWorkOrderDO workOrder,
                                           List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots,
                                           SourceInspection sourceInspection,
                                           List<MesTeamLeaderActiveOrderReleaseBlocker> blockers) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("activeOrderId", activeOrder.getId());
        payload.put("workOrderId", activeOrder.getWorkOrderId());
        payload.put("workOrderCode", workOrder.getCode());
        payload.put("batchCode", workOrder.getBatchCode());
        payload.put("routeId", activeOrder.getRouteId());
        payload.put("routeVersionId", activeOrder.getRouteVersionId());
        payload.put("snapshotIds", snapshots.stream()
                .map(MesProcessPoolActiveOrderProcessSnapshotDO::getId)
                .toList());
        payload.put("bindingIds", sourceInspection.allBindings().stream()
                .map(MesProRouteFlowProcessBatchRecordDO::getId)
                .toList());
        payload.put("aggregateDetailIds", sourceInspection.aggregateDetails().stream()
                .map(MesPqcProcessInspectionAggregateDetailDO::getId)
                .toList());
        payload.put("blockerTypes", blockers.stream()
                .map(MesTeamLeaderActiveOrderReleaseBlocker::getBlockerType)
                .toList());
        return DigestUtil.sha256Hex(JSON.toJSONString(payload));
    }

    private MesProcessPoolActiveOrderReleaseApplicationDO insertApplication(
            MesProcessPoolActiveOrderDO activeOrder,
            MesProWorkOrderDO workOrder,
            String requestIdempotencyKey,
            String businessIdempotencyKey,
            String sourceSnapshotHash,
            String applicationStatus,
            MesTeamLeaderActiveOrderReleaseDossierSummary summary,
            List<MesTeamLeaderActiveOrderReleaseBlocker> blockers,
            Long batchExecutionId,
            Long releaseTransactionId,
            Long releaseApprovalWorkTaskId,
            Long appliedBy,
            String remark,
            LocalDateTime lastPrecheckAt) {
        MesProcessPoolActiveOrderReleaseApplicationDO application =
                new MesProcessPoolActiveOrderReleaseApplicationDO()
                        .setActiveOrderId(activeOrder.getId())
                        .setWorkOrderId(activeOrder.getWorkOrderId())
                        .setWorkOrderCode(workOrder.getCode())
                        .setRouteId(activeOrder.getRouteId())
                        .setRouteVersionId(activeOrder.getRouteVersionId())
                        .setProductId(workOrder.getProductId())
                        .setBatchCode(workOrder.getBatchCode())
                        .setBatchExecutionId(batchExecutionId)
                        .setReleaseTransactionId(releaseTransactionId)
                        .setReleaseApprovalWorkTaskId(releaseApprovalWorkTaskId)
                        .setApplicationStatus(applicationStatus)
                        .setSourceSnapshotHash(sourceSnapshotHash)
                        .setRequestIdempotencyKey(requestIdempotencyKey)
                        .setBusinessIdempotencyKey(businessIdempotencyKey)
                        .setBlockerSnapshotJson(blockers.isEmpty() ? null : JSON.toJSONString(blockers))
                        .setDossierSummaryJson(JSON.toJSONString(summary))
                        .setAppliedBy(appliedBy)
                        .setAppliedAt(LocalDateTime.now())
                        .setLastPrecheckAt(lastPrecheckAt)
                        .setRemark(StrUtil.trim(remark));
        try {
            applicationMapper.insert(application);
        } catch (DuplicateKeyException ex) {
            MesProcessPoolActiveOrderReleaseApplicationDO existing =
                    applicationMapper.selectByBusinessIdempotencyKey(activeOrder.getId(), businessIdempotencyKey);
            if (existing != null) {
                return existing;
            }
            throw ex;
        }
        return application;
    }

    private MesTeamLeaderActiveOrderReleaseApplicationResult toResult(
            MesProcessPoolActiveOrderReleaseApplicationDO application) {
        MesTeamLeaderActiveOrderReleaseDossierSummary summary = StrUtil.isBlank(application.getDossierSummaryJson())
                ? null
                : JSON.parseObject(application.getDossierSummaryJson(),
                MesTeamLeaderActiveOrderReleaseDossierSummary.class);
        List<MesTeamLeaderActiveOrderReleaseBlocker> blockers = StrUtil.isBlank(application.getBlockerSnapshotJson())
                ? List.of()
                : JSON.parseArray(application.getBlockerSnapshotJson(),
                MesTeamLeaderActiveOrderReleaseBlocker.class);
        return new MesTeamLeaderActiveOrderReleaseApplicationResult()
                .setApplicationId(application.getId())
                .setActiveOrderId(application.getActiveOrderId())
                .setWorkOrderId(application.getWorkOrderId())
                .setWorkOrderCode(application.getWorkOrderCode())
                .setBatchExecutionId(application.getBatchExecutionId())
                .setReleaseTransactionId(application.getReleaseTransactionId())
                .setReleaseApprovalWorkTaskId(application.getReleaseApprovalWorkTaskId())
                .setStatus(application.getApplicationStatus())
                .setStatusName(formatApplicationStatus(application.getApplicationStatus()))
                .setDossierSummary(summary)
                .setBlockers(blockers)
                .setAppliedAt(application.getAppliedAt());
    }

    private String formatApplicationStatus(String status) {
        if (APPLICATION_STATUS_PENDING_RELEASE_APPROVAL.equals(status)) {
            return "待生产负责人放行";
        }
        if (APPLICATION_STATUS_BLOCKED.equals(status)) {
            return "资料生成阻塞";
        }
        return status;
    }

    private boolean hasPrecheckBlocker(MesProEdhrReleaseRespVO precheck) {
        return precheck == null
                || precheck.getReleaseTransactionId() == null
                || positive(precheck.getBlockingCheckCount())
                || positive(precheck.getFailedCheckCount());
    }

    private boolean positive(Integer value) {
        return value != null && value > 0;
    }

    private MesTeamLeaderActiveOrderReleaseBlocker blocker(String type, String objectType, Object objectId,
                                                           String objectCode, String reason, String suggestion) {
        return new MesTeamLeaderActiveOrderReleaseBlocker()
                .setBlockerType(type)
                .setObjectType(objectType)
                .setObjectId(objectId == null ? null : String.valueOf(objectId))
                .setObjectCode(objectCode)
                .setReason(reason)
                .setSuggestion(suggestion);
    }

    private String requireRequestIdempotencyKey(String rawKey) {
        String key = StrUtil.trim(rawKey);
        if (StrUtil.isBlank(key)) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "releaseApplyIdempotencyKey");
        }
        return key;
    }

    private String buildBusinessIdempotencyKey(MesProcessPoolActiveOrderDO activeOrder, String sourceSnapshotHash) {
        return String.join("|",
                "ACTIVE_ORDER_RELEASE",
                String.valueOf(activeOrder.getId()),
                String.valueOf(activeOrder.getWorkOrderId()),
                String.valueOf(activeOrder.getRouteId()),
                String.valueOf(activeOrder.getRouteVersionId()),
                sourceSnapshotHash);
    }

    private BigDecimal toProgressPercent(long completedCount, int totalCount) {
        if (totalCount <= 0) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, "activeOrderReleaseProgress");
        }
        return BigDecimal.valueOf(completedCount)
                .multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(totalCount), PROGRESS_SCALE, RoundingMode.HALF_UP);
    }

    private record ProcessIdentity(Long routeProcessId, Long processId) {
    }

    private record ActiveOrderProgress(BigDecimal productionProgressPercent, BigDecimal inspectionProgressPercent) {
    }

    private record SourceInspection(
            List<MesProRouteFlowProcessBatchRecordDO> allBindings,
            List<MesProRouteFlowProcessBatchRecordDO> officialBatchRecords,
            List<MesProRouteFlowProcessBatchRecordDO> processInspectionBindings,
            List<MesProRouteFlowProcessBatchRecordDO> lossReportBindings,
            List<MesPqcProcessInspectionAggregateDetailDO> aggregateDetails) {
    }
}
