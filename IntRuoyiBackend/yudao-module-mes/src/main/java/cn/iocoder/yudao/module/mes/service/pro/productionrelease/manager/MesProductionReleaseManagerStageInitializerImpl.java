package cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlocker;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerException;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerType;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowFailureRespVO;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStage;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStatus;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.report.MesProductionReleaseManagerStageInitializationCommand;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.report.MesProductionReleaseManagerStageInitializationResult;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.report.MesProductionReleaseManagerStageInitializer;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.report.MesProductionReleaseReportSnapshots;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRequiredCandidateResolver;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRoleCandidates;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRoleCodes;
import com.alibaba.fastjson.JSON;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class MesProductionReleaseManagerStageInitializerImpl
        implements MesProductionReleaseManagerStageInitializer {

    private final MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper;
    private final MesProEdhrBatchExecutionMapper batchExecutionMapper;
    private final MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    private final MesProEdhrWorkTaskMapper workTaskMapper;
    private final MesProductionReleaseRequiredCandidateResolver candidateResolver;

    public MesProductionReleaseManagerStageInitializerImpl(
            MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper,
            MesProEdhrBatchExecutionMapper batchExecutionMapper,
            MesProEdhrReleaseTransactionMapper releaseTransactionMapper,
            MesProEdhrWorkTaskMapper workTaskMapper,
            MesProductionReleaseRequiredCandidateResolver candidateResolver) {
        this.applicationMapper = applicationMapper;
        this.batchExecutionMapper = batchExecutionMapper;
        this.releaseTransactionMapper = releaseTransactionMapper;
        this.workTaskMapper = workTaskMapper;
        this.candidateResolver = candidateResolver;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public MesProductionReleaseManagerStageInitializationResult initializeManagerReleaseStage(
            MesProductionReleaseManagerStageInitializationCommand command) {
        requireCommand(command);
        MesProcessPoolActiveOrderReleaseApplicationDO application = applicationMapper.selectById(command.getApplicationId());
        if (application == null
                || !Objects.equals(application.getBatchExecutionId(), command.getBatchExecutionId())
                || !Objects.equals(application.getVersion(), command.getExpectedApplicationVersion())
                || !Objects.equals(application.getApplicationStatus(), MesReleaseFlowStatus.REPORT_UPLOAD_PENDING)) {
            throw blocker(application, MesReleaseFlowBlockerType.STATE_VERSION_CONFLICT,
                    "release application changed before manager-stage initialization",
                    "refresh the report completion receipt before retrying");
        }
        String recomputedSnapshotHash = MesProductionReleaseReportSnapshots.hash(
                application, command.getReportEvidences());
        if (!Objects.equals(recomputedSnapshotHash, command.getReportSnapshotHash())) {
            throw blocker(application, MesReleaseFlowBlockerType.REPORT_SNAPSHOT_CHANGED,
                    "report snapshot changed before manager-stage initialization",
                    "reload and verify all four completed report evidences");
        }
        if (releaseTransactionMapper.selectByBatchExecutionId(command.getBatchExecutionId()) != null) {
            throw blocker(application, MesReleaseFlowBlockerType.RELEASE_TRANSACTION_NOT_PROCESSABLE,
                    "the release batch already has a release transaction",
                    "use the existing authoritative release transaction");
        }
        MesProEdhrBatchExecutionDO batch = batchExecutionMapper.selectById(command.getBatchExecutionId());
        if (batch == null) {
            throw blocker(application, MesReleaseFlowBlockerType.RELEASE_TRANSACTION_NOT_PROCESSABLE,
                    "release batch execution is missing",
                    "restore the authoritative batch execution before continuing");
        }
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("tenantId is required for manager release stage initialization");
        }
        MesProductionReleaseRoleCandidates candidates = candidateResolver.resolveRequiredCandidates(
                tenantId, MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE);
        requireCandidates(application, candidates);

        MesProEdhrReleaseTransactionDO transaction = buildTransaction(
                batch, application, command.getReportSnapshotHash());
        if (releaseTransactionMapper.insert(transaction) != 1 || transaction.getId() == null) {
            throw new IllegalStateException("manager release transaction insert failed");
        }
        MesProEdhrWorkTaskDO workTask = buildWorkTask(batch, application, transaction, candidates);
        if (workTaskMapper.insert(workTask) != 1 || workTask.getId() == null) {
            throw new IllegalStateException("manager release work task insert failed");
        }
        return new MesProductionReleaseManagerStageInitializationResult()
                .setReleaseTransactionId(transaction.getId())
                .setManagerReleaseWorkTaskId(workTask.getId())
                .setManagerCandidateSnapshotHash(candidates.candidateSnapshotHash());
    }

    private void requireCommand(MesProductionReleaseManagerStageInitializationCommand command) {
        if (command == null || command.getApplicationId() == null || command.getBatchExecutionId() == null
                || command.getExpectedApplicationVersion() == null || command.getExpectedApplicationVersion() < 0
                || StrUtil.isBlank(command.getReportSnapshotHash())
                || command.getReportEvidences() == null || command.getReportEvidences().size() != 4) {
            throw blocker(null, MesReleaseFlowBlockerType.REPORT_SNAPSHOT_CHANGED,
                    "complete four-report manager-stage command is required",
                    "complete and freeze all four report evidences first");
        }
    }

    private void requireCandidates(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            MesProductionReleaseRoleCandidates candidates) {
        if (candidates == null || candidates.roleId() == null
                || !Objects.equals(candidates.roleCode(), MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE)
                || candidates.candidateUserIds().isEmpty() || StrUtil.isBlank(candidates.candidateSnapshotHash())) {
            throw blocker(application, MesReleaseFlowBlockerType.MANAGEMENT_REPRESENTATIVE_ROLE_REQUIRED,
                    "enabled management representative candidates are required",
                    "configure the tenant management representative role and enabled members");
        }
    }

    private MesProEdhrReleaseTransactionDO buildTransaction(
            MesProEdhrBatchExecutionDO batch,
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            String reportSnapshotHash) {
        LocalDateTime now = LocalDateTime.now();
        return new MesProEdhrReleaseTransactionDO()
                .setReleaseCode("EDHR-REL-" + batch.getId())
                .setBatchExecutionId(batch.getId())
                .setBatchExecutionCode(batch.getBatchExecutionCode())
                .setWorkOrderId(batch.getWorkOrderId())
                .setWorkOrderCode(batch.getWorkOrderCode())
                .setBatchCode(batch.getBatchCode())
                .setProductId(batch.getProductId())
                .setProductCode(batch.getProductCode())
                .setProductName(batch.getProductName())
                .setRouteId(batch.getRouteId())
                .setRouteCode(batch.getRouteCode())
                .setRouteName(batch.getRouteName())
                .setDhrStatus("PASS")
                .setInspectionStatus("PASS")
                .setDeviationStatus("PASS")
                .setReworkStatus("PASS")
                .setScrapStatus("PASS")
                .setInventoryStatus("PASS")
                .setReleaseStatus(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL)
                .setRequiredCheckCount(4)
                .setFailedCheckCount(0)
                .setBlockingCheckCount(0)
                .setLastPrecheckAt(now)
                .setPrecheckSnapshotJson(JSON.toJSONString(Map.of(
                        "applicationId", application.getId(),
                        "reportSnapshotHash", reportSnapshotHash)))
                .setVersion(1)
                .setRemark("production release manager approval");
    }

    private MesProEdhrWorkTaskDO buildWorkTask(
            MesProEdhrBatchExecutionDO batch,
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            MesProEdhrReleaseTransactionDO transaction,
            MesProductionReleaseRoleCandidates candidates) {
        String candidateSnapshot = candidates.candidateUserIds().stream()
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(","));
        return new MesProEdhrWorkTaskDO()
                .setTaskCode("MANAGER-RELEASE-" + application.getId())
                .setTaskType("RELEASE_APPROVE")
                .setBatchExecutionId(batch.getId())
                .setBusinessScopeType("RELEASE_TRANSACTION")
                .setBusinessScopeId(transaction.getId())
                .setWorkOrderId(batch.getWorkOrderId())
                .setWorkOrderCode(batch.getWorkOrderCode())
                .setBatchCode(batch.getBatchCode())
                .setRouteId(batch.getRouteId())
                .setProcessName("管理者代表最终放行")
                .setAssigneeUserId(candidates.candidateUserIds().get(0))
                .setCandidateSourceType("ROLE")
                .setCandidateSourceId(candidates.roleId())
                .setCandidateUserSnapshot(candidateSnapshot)
                .setSourceUserId(application.getAppliedBy())
                .setResponsibilitySourceType("ROLE")
                .setResponsibilitySourceKey(candidates.roleCode())
                .setResponsibilitySourceVersion(candidates.candidateSnapshotHash())
                .setResponsibilitySourceDigest(candidates.candidateSnapshotHash())
                .setOwnershipLocked(true)
                .setStatus(MesProEdhrWorkTaskStatus.TODO)
                .setActionUrl("/mes/pro/feedback/edhr-batch-execution/detail?id=" + batch.getId()
                        + "&releaseTransactionId=" + transaction.getId() + "&focus=manager-release")
                .setRemark("管理者代表生产放行待办");
    }

    private MesReleaseFlowBlockerException blocker(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            MesReleaseFlowBlockerType type,
            String reason,
            String suggestion) {
        return new MesReleaseFlowBlockerException(reason, new MesReleaseFlowFailureRespVO()
                .setStage(MesReleaseFlowStage.SP_4)
                .setCurrentStatus(application == null ? null : application.getApplicationStatus())
                .setBlockers(List.of(new MesReleaseFlowBlocker()
                        .setBlockerType(type)
                        .setObjectType("RELEASE_APPLICATION")
                        .setObjectId(application == null ? null : String.valueOf(application.getId()))
                        .setReason(reason)
                        .setSuggestion(suggestion))));
    }
}
