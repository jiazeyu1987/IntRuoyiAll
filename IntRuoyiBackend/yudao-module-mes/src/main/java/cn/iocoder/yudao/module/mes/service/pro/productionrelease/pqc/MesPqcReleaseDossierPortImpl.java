package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolOrderProcessCompletionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolOrderProcessCompletionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlocker;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerException;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerType;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowFailureRespVO;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStage;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseBatchRecordPlan;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseBatchRecordWriteResult;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseBatchRecordWriter;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseBlocker;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseLossReportPlan;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseLossReportPlanCommand;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseLossReportWriteResult;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseLossReportWriter;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseProcessInspectionPlan;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseProcessInspectionWriteResult;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseProcessInspectionWriter;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseSourceSnapshotHasher;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class MesPqcReleaseDossierPortImpl implements MesPqcReleaseDossierPort {

    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesProWorkOrderMapper workOrderMapper;
    private final MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;
    private final MesProcessPoolOrderProcessCompletionMapper completionMapper;
    private final MesPqcInspectionTaskMapper pqcTaskMapper;
    private final MesPqcProcessInspectionAggregateDetailMapper aggregateDetailMapper;
    private final MesProProcessPoolEventMapper eventMapper;
    private final MesProcessPoolReportAllocationMapper allocationMapper;
    private final MesProcessPoolSubmissionReviewMapper reviewMapper;
    private final MesTeamLeaderActiveOrderReleaseBatchRecordWriter batchRecordWriter;
    private final MesTeamLeaderActiveOrderReleaseProcessInspectionWriter processInspectionWriter;
    private final MesTeamLeaderActiveOrderReleaseLossReportWriter lossReportWriter;
    private final MesTeamLeaderActiveOrderReleaseSourceSnapshotHasher sourceSnapshotHasher;

    public MesPqcReleaseDossierPortImpl(
            MesProcessPoolActiveOrderMapper activeOrderMapper,
            MesProWorkOrderMapper workOrderMapper,
            MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper,
            MesProcessPoolOrderProcessCompletionMapper completionMapper,
            MesPqcInspectionTaskMapper pqcTaskMapper,
            MesPqcProcessInspectionAggregateDetailMapper aggregateDetailMapper,
            MesProProcessPoolEventMapper eventMapper,
            MesProcessPoolReportAllocationMapper allocationMapper,
            MesProcessPoolSubmissionReviewMapper reviewMapper,
            MesTeamLeaderActiveOrderReleaseBatchRecordWriter batchRecordWriter,
            MesTeamLeaderActiveOrderReleaseProcessInspectionWriter processInspectionWriter,
            MesTeamLeaderActiveOrderReleaseLossReportWriter lossReportWriter,
            MesTeamLeaderActiveOrderReleaseSourceSnapshotHasher sourceSnapshotHasher) {
        this.activeOrderMapper = activeOrderMapper;
        this.workOrderMapper = workOrderMapper;
        this.processSnapshotMapper = processSnapshotMapper;
        this.completionMapper = completionMapper;
        this.pqcTaskMapper = pqcTaskMapper;
        this.aggregateDetailMapper = aggregateDetailMapper;
        this.eventMapper = eventMapper;
        this.allocationMapper = allocationMapper;
        this.reviewMapper = reviewMapper;
        this.batchRecordWriter = batchRecordWriter;
        this.processInspectionWriter = processInspectionWriter;
        this.lossReportWriter = lossReportWriter;
        this.sourceSnapshotHasher = sourceSnapshotHasher;
    }

    @Override
    public MesPqcReleaseDossierPlan plan(
            MesProcessPoolActiveOrderReleaseApplicationDO application, Long actorUserId) {
        Long tenantId = TenantContextHolder.getTenantId();
        MesProcessPoolActiveOrderDO activeOrder = activeOrderMapper.selectById(application.getActiveOrderId());
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(application.getWorkOrderId());
        List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots = list(
                processSnapshotMapper.selectListByActiveOrderIdForUpdate(application.getActiveOrderId()));
        List<MesProcessPoolOrderProcessCompletionDO> completions = list(
                completionMapper.selectListByWorkOrderIds(List.of(application.getWorkOrderId())));
        List<MesPqcInspectionTaskDO> inspectionTasks = list(
                pqcTaskMapper.selectListByActiveOrderId(application.getActiveOrderId()));
        List<MesPqcProcessInspectionAggregateDetailDO> inspectionDetails = list(
                aggregateDetailMapper.selectListByActiveOrderId(application.getActiveOrderId()));
        requireApplicationSources(application, tenantId, activeOrder, workOrder, snapshots);
        String currentSourceHash = sourceSnapshotHasher.hash(
                new MesTeamLeaderActiveOrderReleaseSourceSnapshotHasher.Input(
                        tenantId, activeOrder, workOrder, snapshots, completions,
                        inspectionTasks, inspectionDetails));
        if (!Objects.equals(application.getSourceSnapshotHash(), currentSourceHash)) {
            throw blocker(MesReleaseFlowBlockerType.FROZEN_ROUTE_SOURCE_REQUIRED, application, null,
                    "authoritative production or inspection sources changed after SP-1");
        }

        MesTeamLeaderActiveOrderReleaseBatchRecordPlan batchPlan = batchRecordWriter.plan(
                new MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand()
                        .setTenantId(tenantId)
                        .setActiveOrderId(application.getActiveOrderId())
                        .setWorkOrderId(application.getWorkOrderId())
                        .setRouteId(application.getRouteId())
                        .setRouteVersionId(application.getRouteVersionId())
                        .setProductId(application.getProductId())
                        .setBatchCode(application.getBatchCode())
                        .setApplicantUserId(actorUserId)
                        .setWorkOrder(workOrder)
                        .setSourceSnapshotHash(currentSourceHash)
                        .setProcessSources(loadBatchRecordSources(application, snapshots, completions)));
        MesTeamLeaderActiveOrderReleaseProcessInspectionPlan inspectionPlan = processInspectionWriter.plan(
                new MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand()
                        .setTenantId(tenantId)
                        .setActiveOrderId(application.getActiveOrderId())
                        .setWorkOrderId(application.getWorkOrderId())
                        .setProductId(application.getProductId())
                        .setRouteId(application.getRouteId())
                        .setRouteVersionId(application.getRouteVersionId())
                        .setBatchCode(application.getBatchCode())
                        .setSourceSnapshotHash(currentSourceHash));
        MesTeamLeaderActiveOrderReleaseLossReportPlan lossPlan = lossReportWriter.plan(
                new MesTeamLeaderActiveOrderReleaseLossReportPlanCommand()
                        .setTenantId(tenantId)
                        .setActiveOrderId(application.getActiveOrderId())
                        .setWorkOrderId(application.getWorkOrderId())
                        .setRouteId(application.getRouteId())
                        .setRouteVersionId(application.getRouteVersionId())
                        .setProductId(application.getProductId())
                        .setBatchCode(application.getBatchCode())
                        .setSourceSnapshotHash(currentSourceHash)
                        .setProcessSnapshots(snapshots));
        requireFormalPlan(application, batchPlan, inspectionPlan, lossPlan);
        return new MesPqcReleaseDossierPlan()
                .setSourceSnapshotHash(currentSourceHash)
                .setBatchRecordPlan(batchPlan)
                .setProcessInspectionPlan(inspectionPlan)
                .setLossReportPlan(lossPlan);
    }

    @Override
    public MesPqcReleaseDossierWriteResult write(MesPqcReleaseDossierPlan plan, Long batchExecutionId) {
        MesTeamLeaderActiveOrderReleaseBatchRecordWriteResult batchWrite =
                batchRecordWriter.write(plan.getBatchRecordPlan(), batchExecutionId);
        MesTeamLeaderActiveOrderReleaseProcessInspectionWriteResult inspectionWrite =
                processInspectionWriter.write(plan.getProcessInspectionPlan(), batchExecutionId);
        MesTeamLeaderActiveOrderReleaseLossReportWriteResult lossWrite =
                lossReportWriter.write(plan.getLossReportPlan(), batchExecutionId);
        requireFormalWrite(plan, batchExecutionId, batchWrite, inspectionWrite, lossWrite);
        return new MesPqcReleaseDossierWriteResult()
                .setBatchRecordEvidenceIds(List.copyOf(batchWrite.getBatchRecordExecutionIds()))
                .setProcessInspectionEvidenceIds(List.copyOf(inspectionWrite.getBatchRecordExecutionIds()))
                .setLossReportEvidenceIds(List.copyOf(lossWrite.getBatchRecordExecutionIds()));
    }

    private void requireApplicationSources(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            Long tenantId,
            MesProcessPoolActiveOrderDO activeOrder,
            MesProWorkOrderDO workOrder,
            List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots) {
        if (tenantId == null || tenantId <= 0 || activeOrder == null || workOrder == null || snapshots.isEmpty()
                || !Objects.equals(application.getWorkOrderId(), activeOrder.getWorkOrderId())
                || !Objects.equals(application.getRouteId(), activeOrder.getRouteId())
                || !Objects.equals(application.getRouteVersionId(), activeOrder.getRouteVersionId())
                || !Objects.equals(application.getProductId(), workOrder.getProductId())
                || !Objects.equals(application.getBatchCode(), StrUtil.trim(workOrder.getBatchCode()))) {
            throw blocker(MesReleaseFlowBlockerType.FROZEN_ROUTE_SOURCE_REQUIRED, application, null,
                    "release application no longer matches the frozen active-order sources");
        }
    }

    private List<MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand.ProcessSource> loadBatchRecordSources(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots,
            List<MesProcessPoolOrderProcessCompletionDO> completions) {
        List<MesProProcessPoolEventDO> allEvents = list(eventMapper.selectProductionSubmitsByWorkOrderAndRoute(
                application.getWorkOrderId(), application.getRouteId()));
        List<MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand.ProcessSource> sources = new ArrayList<>();
        for (MesProcessPoolActiveOrderProcessSnapshotDO snapshot : snapshots) {
            MesProcessPoolOrderProcessCompletionDO completion = completions.stream()
                    .filter(item -> Objects.equals(snapshot.getRouteProcessId(), item.getRouteProcessId())
                            && Objects.equals(snapshot.getProcessId(), item.getProcessId()))
                    .findFirst().orElse(null);
            List<MesProProcessPoolEventDO> events = allEvents.stream()
                    .filter(item -> Objects.equals(snapshot.getRouteProcessId(), item.getRouteProcessId())
                            && Objects.equals(snapshot.getProcessId(), item.getProcessId()))
                    .toList();
            List<MesProcessPoolReportAllocationDO> allocations = events.stream()
                    .flatMap(item -> list(allocationMapper.selectListByEventId(item.getId())).stream())
                    .toList();
            List<MesProcessPoolSubmissionReviewDO> reviews = events.stream()
                    .flatMap(item -> list(reviewMapper.selectListByEventId(item.getId())).stream())
                    .toList();
            sources.add(new MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand.ProcessSource()
                    .setSnapshot(snapshot)
                    .setCompletion(completion)
                    .setSourceEvents(events)
                    .setAllocations(allocations)
                    .setReviews(reviews));
        }
        return List.copyOf(sources);
    }

    private void requireFormalPlan(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            MesTeamLeaderActiveOrderReleaseBatchRecordPlan batchPlan,
            MesTeamLeaderActiveOrderReleaseProcessInspectionPlan inspectionPlan,
            MesTeamLeaderActiveOrderReleaseLossReportPlan lossPlan) {
        if (batchPlan == null || batchPlan.getPreparedProcesses() == null
                || batchPlan.getPreparedProcesses().isEmpty()) {
            throw blocker(MesReleaseFlowBlockerType.BATCH_RECORD_SOURCE_REQUIRED, application, null,
                    "formal batch-record plan is missing");
        }
        requireNoBlockers(application, MesReleaseFlowBlockerType.BATCH_RECORD_SOURCE_REQUIRED, batchPlan.getBlockers());
        if (batchPlan.getPreparedProcesses().stream().anyMatch(item -> item.getBinding() == null
                || StrUtil.isBlank(item.getBinding().getBatchRecordReportId()))) {
            throw blocker(MesReleaseFlowBlockerType.BATCH_RECORD_SOURCE_REQUIRED, application, null,
                    "formBindings cannot replace the formal per-process batch-record report binding");
        }
        if (inspectionPlan == null || inspectionPlan.getPreparedInspections() == null
                || inspectionPlan.getPreparedInspections().isEmpty()) {
            throw blocker(MesReleaseFlowBlockerType.PROCESS_INSPECTION_SOURCE_REQUIRED, application, null,
                    "formal process-inspection plan is missing");
        }
        requireNoBlockers(application, MesReleaseFlowBlockerType.PROCESS_INSPECTION_SOURCE_REQUIRED,
                inspectionPlan.getBlockers());
        if (inspectionPlan.getPreparedInspections().stream().anyMatch(item -> item.getBinding() == null
                || StrUtil.isBlank(item.getBinding().getBatchRecordReportId()))) {
            throw blocker(MesReleaseFlowBlockerType.PROCESS_INSPECTION_SOURCE_REQUIRED, application, null,
                    "formBindings cannot replace the formal process-inspection report binding");
        }
        if (lossPlan == null || lossPlan.getPreparedReports() == null || lossPlan.getPreparedReports().isEmpty()) {
            throw blocker(MesReleaseFlowBlockerType.LOSS_REPORT_SOURCE_REQUIRED, application, null,
                    "formal loss-report plan is missing, including the zero-loss report");
        }
        requireNoBlockers(application, MesReleaseFlowBlockerType.LOSS_REPORT_SOURCE_REQUIRED, lossPlan.getBlockers());
        if (lossPlan.getPreparedReports().stream().anyMatch(item -> item.getBinding() == null
                || StrUtil.isBlank(item.getBinding().getBatchRecordReportId()))) {
            throw blocker(MesReleaseFlowBlockerType.LOSS_REPORT_SOURCE_REQUIRED, application, null,
                    "formBindings cannot replace the formal loss-report binding");
        }
    }

    private void requireNoBlockers(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            MesReleaseFlowBlockerType type,
            List<MesTeamLeaderActiveOrderReleaseBlocker> blockers) {
        if (blockers != null && !blockers.isEmpty()) {
            throw blocker(type, application, blockers.get(0), blockers.get(0).getReason());
        }
    }

    private void requireFormalWrite(
            MesPqcReleaseDossierPlan plan,
            Long batchExecutionId,
            MesTeamLeaderActiveOrderReleaseBatchRecordWriteResult batchWrite,
            MesTeamLeaderActiveOrderReleaseProcessInspectionWriteResult inspectionWrite,
            MesTeamLeaderActiveOrderReleaseLossReportWriteResult lossWrite) {
        if (batchWrite == null || inspectionWrite == null || lossWrite == null
                || !"BATCH_RECORD".equals(batchWrite.getDocumentType())
                || !"PROCESS_INSPECTION".equals(inspectionWrite.getDocumentType())
                || !"LOSS_REPORT".equals(lossWrite.getDocumentType())
                || !Objects.equals(plan.getBatchRecordPlan().getSourceObjectIds(), batchWrite.getSourceObjectIds())
                || !Objects.equals(plan.getBatchRecordPlan().getSourceValueHashes(), batchWrite.getSourceValueHashes())
                || !Objects.equals(plan.getProcessInspectionPlan().getSourceObjectIds(), inspectionWrite.getSourceObjectIds())
                || !Objects.equals(plan.getProcessInspectionPlan().getSourceValueHashes(), inspectionWrite.getSourceValueHashes())
                || !Objects.equals(plan.getLossReportPlan().getSourceObjectIds(), lossWrite.getSourceObjectIds())
                || !Objects.equals(plan.getLossReportPlan().getSourceValueHashes(), lossWrite.getSourceValueHashes())) {
            throw blocker(MesReleaseFlowBlockerType.BATCH_RECORD_SOURCE_REQUIRED, null, null,
                    "writer receipt does not match the planned formal source evidence");
        }
        requireNoWriteBlockers(batchExecutionId, MesReleaseFlowBlockerType.BATCH_RECORD_SOURCE_REQUIRED,
                batchWrite.getBlockers());
        requireNoWriteBlockers(batchExecutionId, MesReleaseFlowBlockerType.PROCESS_INSPECTION_SOURCE_REQUIRED,
                inspectionWrite.getBlockers());
        requireNoWriteBlockers(batchExecutionId, MesReleaseFlowBlockerType.LOSS_REPORT_SOURCE_REQUIRED,
                lossWrite.getBlockers());
        if (empty(batchWrite.getBatchRecordExecutionIds()) || empty(inspectionWrite.getBatchRecordExecutionIds())
                || empty(lossWrite.getBatchRecordExecutionIds())) {
            throw blocker(MesReleaseFlowBlockerType.BATCH_RECORD_SOURCE_REQUIRED, null, null,
                    "writer did not return all three persistent mapping evidence sets");
        }
    }

    private void requireNoWriteBlockers(
            Long batchExecutionId,
            MesReleaseFlowBlockerType type,
            List<MesTeamLeaderActiveOrderReleaseBlocker> blockers) {
        if (blockers != null && !blockers.isEmpty()) {
            throw blocker(type, null, blockers.get(0), blockers.get(0).getReason())
                    ;
        }
    }

    private boolean empty(List<?> values) {
        return values == null || values.isEmpty();
    }

    private <T> List<T> list(List<T> values) {
        return values == null ? List.of() : values;
    }

    private MesReleaseFlowBlockerException blocker(
            MesReleaseFlowBlockerType type,
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            MesTeamLeaderActiveOrderReleaseBlocker source,
            String reason) {
        String message = StrUtil.blankToDefault(reason, "formal production release source is unavailable");
        return new MesReleaseFlowBlockerException(message, new MesReleaseFlowFailureRespVO()
                .setStage(MesReleaseFlowStage.SP_2)
                .setCurrentStatus(application == null ? null : application.getApplicationStatus())
                .setBlockers(List.of(new MesReleaseFlowBlocker()
                        .setBlockerType(type)
                        .setObjectType(source == null ? "RELEASE_DOSSIER" : source.getObjectType())
                        .setObjectId(source == null || source.getObjectId() == null
                                ? application == null ? null : String.valueOf(application.getId())
                                : source.getObjectId())
                        .setObjectCode(source == null ? null : source.getObjectCode())
                        .setRouteProcessId(source == null ? null : source.getRouteProcessId())
                        .setProcessId(source == null ? null : source.getProcessId())
                        .setFieldCode(source == null ? null : source.getFieldCode())
                        .setCellKey(source == null ? null : source.getCellKey())
                        .setReason(message)
                        .setSuggestion(source == null ? "repair the formal source binding before retrying"
                                : source.getSuggestion()))));
    }
}
