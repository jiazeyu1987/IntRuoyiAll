package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionPieceDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderTransferTraceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolWorkOrderAbnormalDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.materialstock.MesWmMaterialStockDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionPieceDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderTransferTraceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolWorkOrderAbnormalMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.materialstock.MesWmMaterialStockMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class MesOrderReleaseCompletenessServiceImpl implements MesOrderReleaseCompletenessService {

    private static final String RESULT_PASS = "PASS";
    private static final String RESULT_BLOCKER = "BLOCKER";
    private static final String RESULT_NOT_APPLICABLE = "NOT_APPLICABLE";
    private static final String SEVERITY_INFO = "INFO";
    private static final String SEVERITY_BLOCKER = "BLOCKER";
    private static final String MODULE_QMS = "QMS";
    private static final String MODULE_MES = "MES";
    private static final String MODULE_WMS = "WMS";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final String PQC_STATUS_CONFIRMED = "CONFIRMED";
    private static final int PQC_DEFAULT_ROUND_NO = 1;
    private static final Set<String> PQC_INSPECTION_RESULTS = Set.of(
            MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS,
            MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE);
    private static final Set<String> ACCEPTED_NONCONFORMANCE_DISPOSITIONS = Set.of(
            MesProEdhrNonconformanceReviewService.DISPOSITION_CONCESSION_RELEASE,
            MesProEdhrNonconformanceReviewService.DISPOSITION_REWORK,
            MesProEdhrNonconformanceReviewService.DISPOSITION_VOID);
    private static final Set<String> REQUIRED_INVENTORY_SOURCE_TYPES = Set.of(
            MesProcessPoolActiveOrderTransferTraceDO.SOURCE_TYPE_TRANSFER,
            MesProcessPoolActiveOrderTransferTraceDO.SOURCE_TYPE_SHIPMENT,
            MesProcessPoolActiveOrderTransferTraceDO.SOURCE_TYPE_BATCH_TRACE);
    private static final Set<String> INVENTORY_SOURCE_TYPES = Set.of(
            MesProcessPoolActiveOrderTransferTraceDO.SOURCE_TYPE_TRANSFER,
            MesProcessPoolActiveOrderTransferTraceDO.SOURCE_TYPE_SHIPMENT,
            MesProcessPoolActiveOrderTransferTraceDO.SOURCE_TYPE_REPLENISHMENT,
            MesProcessPoolActiveOrderTransferTraceDO.SOURCE_TYPE_RETURN,
            MesProcessPoolActiveOrderTransferTraceDO.SOURCE_TYPE_BATCH_TRACE);
    private static final Set<String> MOVEMENT_SOURCE_TYPES_REQUIRING_CLOSED_STATUS = Set.of(
            MesProcessPoolActiveOrderTransferTraceDO.SOURCE_TYPE_TRANSFER,
            MesProcessPoolActiveOrderTransferTraceDO.SOURCE_TYPE_SHIPMENT,
            MesProcessPoolActiveOrderTransferTraceDO.SOURCE_TYPE_REPLENISHMENT,
            MesProcessPoolActiveOrderTransferTraceDO.SOURCE_TYPE_RETURN);
    private static final Set<String> CLOSED_SOURCE_STATUSES = Set.of("CLOSED", "COMPLETED", "FINISHED", "4");

    @Resource
    private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Resource
    private MesPqcInspectionTaskMapper pqcInspectionTaskMapper;
    @Resource
    private MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;
    @Resource
    private MesQaInspectionRegulationVersionMapper regulationVersionMapper;
    @Resource
    private MesProcessPoolWorkOrderAbnormalMapper workOrderAbnormalMapper;
    @Resource
    private MesProcessPoolActiveOrderTransferTraceMapper transferTraceMapper;
    @Resource
    private MesWmMaterialStockMapper materialStockMapper;
    @Resource
    private MesProProcessPoolPqcRecordMapper pqcRecordMapper;
    @Resource
    private MesPqcInspectionPieceDetailMapper pqcPieceDetailMapper;
    @Resource
    private MesPqcProcessInspectionAggregateDetailMapper aggregateDetailMapper;
    @Resource
    private MesProEdhrNonconformanceReviewMapper nonconformanceReviewMapper;

    @Override
    public MesOrderReleaseCompletenessCheck evaluateInspectionResult(MesProEdhrBatchExecutionDO batch) {
        MesProcessPoolActiveOrderDO activeOrder = requireActiveOrder(batch,
                MesProEdhrReleaseServiceImpl.CHECK_INSPECTION_RESULT, "检验结果检查", "INSPECTION", MODULE_QMS);
        if (activeOrder == null) {
            return activeOrderMissing(MesProEdhrReleaseServiceImpl.CHECK_INSPECTION_RESULT,
                    "检验结果检查", "INSPECTION", MODULE_QMS, batch);
        }
        List<MesPqcInspectionTaskDO> tasks = pqcInspectionTaskMapper.selectListByActiveOrderId(activeOrder.getId());
        if (CollUtil.isEmpty(tasks)) {
            return blocker(MesProEdhrReleaseServiceImpl.CHECK_INSPECTION_RESULT, "检验结果检查",
                    "INSPECTION", MODULE_QMS, "PQC_INSPECTION_TASK", String.valueOf(activeOrder.getId()),
                    String.valueOf(activeOrder.getId()), "未找到该活跃订单的正式 PQC 检验任务",
                    "先按发布 QA 规程生成并提交/确认 PQC 检验任务");
        }
        List<Long> notConfirmed = tasks.stream()
                .filter(task -> !Objects.equals(PQC_STATUS_CONFIRMED, task.getTaskStatus()))
                .map(MesPqcInspectionTaskDO::getId)
                .toList();
        if (!notConfirmed.isEmpty()) {
            return blocker(MesProEdhrReleaseServiceImpl.CHECK_INSPECTION_RESULT, "检验结果检查",
                    "INSPECTION", MODULE_QMS, "PQC_INSPECTION_TASK", String.valueOf(activeOrder.getId()),
                    String.valueOf(activeOrder.getId()), summarizeIds("存在未确认 PQC 检验任务", notConfirmed),
                    "PQC 组长确认最终修订后重新预检");
        }
        List<String> missingTaskIdentities = missingExpectedPqcTaskIdentities(activeOrder, tasks);
        if (!missingTaskIdentities.isEmpty()) {
            return blocker(MesProEdhrReleaseServiceImpl.CHECK_INSPECTION_RESULT, "检验结果检查",
                    "INSPECTION", MODULE_QMS, "PQC_INSPECTION_TASK", String.valueOf(activeOrder.getId()),
                    String.valueOf(activeOrder.getId()), summarizeText("缺少预期 PQC 检验任务身份", missingTaskIdentities),
                    "按发布 QA 规程重新生成 FIRST、PATROL AM、PATROL PM、FINAL 任务后重新预检");
        }
        List<String> missingResultEvidence = missingFormalPqcInspectionResultEvidence(tasks);
        if (!missingResultEvidence.isEmpty()) {
            return blocker(MesProEdhrReleaseServiceImpl.CHECK_INSPECTION_RESULT, "检验结果检查",
                    "INSPECTION", MODULE_QMS, "PQC_INSPECTION_TASK", String.valueOf(activeOrder.getId()),
                    String.valueOf(activeOrder.getId()),
                    summarizeText("PQC 检验结果证据不完整", missingResultEvidence),
                    "补齐正式 PQC 提交、逐件判定、PQC 组长汇集和必要不合格处置依据后重新预检");
        }
        return pass(MesProEdhrReleaseServiceImpl.CHECK_INSPECTION_RESULT, "检验结果检查",
                "INSPECTION", MODULE_QMS, "PQC_INSPECTION_TASK", String.valueOf(activeOrder.getId()),
                String.valueOf(activeOrder.getId()), "PQC 检验任务身份完整，正式检验结论、逐件判定和必要处置依据完整");
    }

    @Override
    public MesOrderReleaseCompletenessCheck evaluateDeviationClosed(MesProEdhrBatchExecutionDO batch) {
        List<MesProcessPoolWorkOrderAbnormalDO> abnormalities =
                workOrderAbnormalMapper.selectListByWorkOrderId(batch.getWorkOrderId());
        List<Long> open = abnormalities.stream()
                .filter(item -> !Objects.equals(STATUS_CLOSED, item.getReportStatus()))
                .map(MesProcessPoolWorkOrderAbnormalDO::getId)
                .toList();
        if (!open.isEmpty()) {
            return blocker(MesProEdhrReleaseServiceImpl.CHECK_DEVIATION_CLOSED, "偏差关闭检查",
                    "DEVIATION", MODULE_QMS, "QUALITY_ABNORMAL", String.valueOf(batch.getWorkOrderId()),
                    batch.getWorkOrderCode(), summarizeIds("存在未关闭质量异常/偏差", open),
                    "关闭质量异常或记录处置结论后重新预检");
        }
        if (abnormalities.isEmpty()) {
            return notApplicable(MesProEdhrReleaseServiceImpl.CHECK_DEVIATION_CLOSED, "偏差关闭检查",
                    "DEVIATION", MODULE_QMS, "QUALITY_ABNORMAL", String.valueOf(batch.getWorkOrderId()),
                    batch.getWorkOrderCode(), "未发现该工单质量异常/偏差记录");
        }
        return pass(MesProEdhrReleaseServiceImpl.CHECK_DEVIATION_CLOSED, "偏差关闭检查",
                "DEVIATION", MODULE_QMS, "QUALITY_ABNORMAL", String.valueOf(batch.getWorkOrderId()),
                batch.getWorkOrderCode(), "质量异常/偏差均已关闭");
    }

    @Override
    public MesOrderReleaseCompletenessCheck evaluateReworkClosed(MesProEdhrBatchExecutionDO batch) {
        MesProcessPoolActiveOrderDO activeOrder = findActiveOrder(batch);
        if (activeOrder == null) {
            return activeOrderMissing(MesProEdhrReleaseServiceImpl.CHECK_REWORK_CLOSED,
                    "返工完成检查", "REWORK", MODULE_MES, batch);
        }
        List<MesProcessPoolActiveOrderTransferTraceDO> reworkTraces = transferTraceMapper
                .selectListByActiveOrderIdAndSourceTypes(activeOrder.getId(),
                        Set.of(MesProcessPoolActiveOrderTransferTraceDO.SOURCE_TYPE_REWORK));
        List<Long> open = reworkTraces.stream()
                .filter(trace -> !Objects.equals(STATUS_CLOSED, trace.getSourceStatus()))
                .map(MesProcessPoolActiveOrderTransferTraceDO::getId)
                .toList();
        if (!open.isEmpty()) {
            return blocker(MesProEdhrReleaseServiceImpl.CHECK_REWORK_CLOSED, "返工完成检查",
                    "REWORK", MODULE_MES, "ACTIVE_ORDER_REWORK_TRACE", String.valueOf(activeOrder.getId()),
                    String.valueOf(activeOrder.getId()), summarizeIds("存在未闭环返工追溯", open),
                    "完成返工审批和记录后重新预检");
        }
        if (reworkTraces.isEmpty()) {
            return notApplicable(MesProEdhrReleaseServiceImpl.CHECK_REWORK_CLOSED, "返工完成检查",
                    "REWORK", MODULE_MES, "ACTIVE_ORDER_REWORK_TRACE", String.valueOf(activeOrder.getId()),
                    String.valueOf(activeOrder.getId()), "未发现返工追溯记录");
        }
        return pass(MesProEdhrReleaseServiceImpl.CHECK_REWORK_CLOSED, "返工完成检查",
                "REWORK", MODULE_MES, "ACTIVE_ORDER_REWORK_TRACE", String.valueOf(activeOrder.getId()),
                String.valueOf(activeOrder.getId()), "返工追溯均已闭环");
    }

    @Override
    public MesOrderReleaseCompletenessCheck evaluateScrapRecorded(MesProEdhrBatchExecutionDO batch) {
        MesProcessPoolActiveOrderDO activeOrder = findActiveOrder(batch);
        if (activeOrder == null) {
            return activeOrderMissing(MesProEdhrReleaseServiceImpl.CHECK_SCRAP_RECORDED,
                    "报废记录检查", "SCRAP", MODULE_MES, batch);
        }
        List<MesProcessPoolActiveOrderTransferTraceDO> scrapTraces = transferTraceMapper
                .selectListByActiveOrderIdAndSourceTypes(activeOrder.getId(),
                        Set.of(MesProcessPoolActiveOrderTransferTraceDO.SOURCE_TYPE_SCRAP));
        List<Long> incomplete = scrapTraces.stream()
                .filter(trace -> !Objects.equals(STATUS_CLOSED, trace.getSourceStatus()))
                .map(MesProcessPoolActiveOrderTransferTraceDO::getId)
                .toList();
        if (!incomplete.isEmpty()) {
            return blocker(MesProEdhrReleaseServiceImpl.CHECK_SCRAP_RECORDED, "报废记录检查",
                    "SCRAP", MODULE_MES, "ACTIVE_ORDER_SCRAP_TRACE", String.valueOf(activeOrder.getId()),
                    String.valueOf(activeOrder.getId()), summarizeIds("存在未完成报废记录", incomplete),
                    "完成报废记录和库存追溯后重新预检");
        }
        if (scrapTraces.isEmpty()) {
            return notApplicable(MesProEdhrReleaseServiceImpl.CHECK_SCRAP_RECORDED, "报废记录检查",
                    "SCRAP", MODULE_MES, "ACTIVE_ORDER_SCRAP_TRACE", String.valueOf(activeOrder.getId()),
                    String.valueOf(activeOrder.getId()), "未发现报废追溯记录");
        }
        return pass(MesProEdhrReleaseServiceImpl.CHECK_SCRAP_RECORDED, "报废记录检查",
                "SCRAP", MODULE_MES, "ACTIVE_ORDER_SCRAP_TRACE", String.valueOf(activeOrder.getId()),
                String.valueOf(activeOrder.getId()), "报废记录均已闭环");
    }

    @Override
    public MesOrderReleaseCompletenessCheck evaluateInventoryConsistency(MesProEdhrBatchExecutionDO batch) {
        MesProcessPoolActiveOrderDO activeOrder = findActiveOrder(batch);
        if (activeOrder == null) {
            return activeOrderMissing(MesProEdhrReleaseServiceImpl.CHECK_INVENTORY_CONSISTENCY,
                    "库存一致性检查", "INVENTORY", MODULE_WMS, batch);
        }
        List<MesProcessPoolActiveOrderTransferTraceDO> traces = transferTraceMapper
                .selectListByActiveOrderIdAndSourceTypes(activeOrder.getId(), INVENTORY_SOURCE_TYPES);
        if (CollUtil.isEmpty(traces)) {
            return blocker(MesProEdhrReleaseServiceImpl.CHECK_INVENTORY_CONSISTENCY, "库存一致性检查",
                    "INVENTORY", MODULE_WMS, "ACTIVE_ORDER_TRANSFER_TRACE", String.valueOf(activeOrder.getId()),
                    String.valueOf(activeOrder.getId()), "未找到 activeOrderId 的调拨/发货/补退料/批次库存追溯",
                    "同步并绑定正式调拨、发货、补料、退料和批次库存来源后重新预检");
        }
        Set<String> existingSourceTypes = traces.stream()
                .map(MesProcessPoolActiveOrderTransferTraceDO::getSourceType)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        Set<String> missingSourceTypes = REQUIRED_INVENTORY_SOURCE_TYPES.stream()
                .filter(type -> !existingSourceTypes.contains(type))
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        if (!missingSourceTypes.isEmpty()) {
            return blocker(MesProEdhrReleaseServiceImpl.CHECK_INVENTORY_CONSISTENCY, "库存一致性检查",
                    "INVENTORY", MODULE_WMS, "ACTIVE_ORDER_TRANSFER_TRACE", String.valueOf(activeOrder.getId()),
                    String.valueOf(activeOrder.getId()), "缺少必备库存追溯来源：" + missingSourceTypes,
                    "同步并绑定正式调拨、发货和批次追溯来源后重新预检");
        }
        List<String> invalidTraceReasons = traces.stream()
                .map(this::invalidInventoryTraceReason)
                .filter(Objects::nonNull)
                .toList();
        if (!invalidTraceReasons.isEmpty()) {
            return blocker(MesProEdhrReleaseServiceImpl.CHECK_INVENTORY_CONSISTENCY, "库存一致性检查",
                    "INVENTORY", MODULE_WMS, "ACTIVE_ORDER_TRANSFER_TRACE", String.valueOf(activeOrder.getId()),
                    String.valueOf(activeOrder.getId()), summarizeText("无效库存追溯来源", invalidTraceReasons),
                    "修正调拨/发货/补退料/批次追溯数量、来源状态和正式对象后重新预检");
        }
        List<String> duplicateSourceIdentities = duplicateInventoryTraceIdentities(traces);
        if (!duplicateSourceIdentities.isEmpty()) {
            return blocker(MesProEdhrReleaseServiceImpl.CHECK_INVENTORY_CONSISTENCY, "库存一致性检查",
                    "INVENTORY", MODULE_WMS, "ACTIVE_ORDER_TRANSFER_TRACE", String.valueOf(activeOrder.getId()),
                    String.valueOf(activeOrder.getId()), summarizeText("存在重复库存追溯来源", duplicateSourceIdentities),
                    "按正式来源单据、来源行/明细、物料、批次和库存台账去重后重新预检");
        }
        List<Long> stockIds = traces.stream()
                .map(MesProcessPoolActiveOrderTransferTraceDO::getMaterialStockId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<MesWmMaterialStockDO> stocks = stockIds.isEmpty() ? List.of()
                : materialStockMapper.selectListByIds(stockIds);
        List<Long> inconsistentStockIds = stocks.stream()
                .filter(stock -> Boolean.TRUE.equals(stock.getFrozen())
                        || stock.getQuantity() == null
                        || stock.getQuantity().compareTo(BigDecimal.ZERO) < 0)
                .map(MesWmMaterialStockDO::getId)
                .toList();
        if (!inconsistentStockIds.isEmpty()) {
            return blocker(MesProEdhrReleaseServiceImpl.CHECK_INVENTORY_CONSISTENCY, "库存一致性检查",
                    "INVENTORY", MODULE_WMS, "MES_WM_MATERIAL_STOCK", String.valueOf(activeOrder.getId()),
                    String.valueOf(activeOrder.getId()), summarizeIds("存在冻结或负库存台账", inconsistentStockIds),
                    "修复库存台账状态和数量后重新预检");
        }
        return pass(MesProEdhrReleaseServiceImpl.CHECK_INVENTORY_CONSISTENCY, "库存一致性检查",
                "INVENTORY", MODULE_WMS, "ACTIVE_ORDER_TRANSFER_TRACE", String.valueOf(activeOrder.getId()),
                String.valueOf(activeOrder.getId()), "调拨/发货/补退料/批次库存追溯来源已接入");
    }

    private MesProcessPoolActiveOrderDO requireActiveOrder(MesProEdhrBatchExecutionDO batch, String checkCode,
                                                           String checkName, String checkCategory, String module) {
        return findActiveOrder(batch);
    }

    private MesProcessPoolActiveOrderDO findActiveOrder(MesProEdhrBatchExecutionDO batch) {
        if (batch.getWorkOrderId() == null || batch.getRouteId() == null || batch.getRouteVersionId() == null) {
            return null;
        }
        return activeOrderMapper.selectActiveByWorkOrderRouteVersion(batch.getWorkOrderId(), batch.getRouteId(),
                batch.getRouteVersionId());
    }

    private List<String> missingExpectedPqcTaskIdentities(MesProcessPoolActiveOrderDO activeOrder,
                                                          List<MesPqcInspectionTaskDO> tasks) {
        List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots =
                processSnapshotMapper.selectListByActiveOrderId(activeOrder.getId());
        if (CollUtil.isEmpty(snapshots)) {
            return List.of("activeOrderId=" + activeOrder.getId() + " 缺少工序快照，无法证明预期 PQC 任务集合完整");
        }
        List<String> missing = new ArrayList<>(invalidOrDuplicatePqcTaskIdentities(tasks));
        for (MesProcessPoolActiveOrderProcessSnapshotDO snapshot : snapshots) {
            List<MesPqcInspectionTaskDO> snapshotTasks = tasks.stream()
                    .filter(task -> Objects.equals(snapshot.getRouteProcessId(), task.getRouteProcessId())
                            && Objects.equals(snapshot.getProcessId(), task.getProcessId()))
                    .toList();
            requirePqcTaskCoverage(snapshotTasks, snapshot, "FIRST", "FIRST", missing);
            requirePqcTaskCoverage(snapshotTasks, snapshot, "PATROL", "AM", missing);
            requirePqcTaskCoverage(snapshotTasks, snapshot, "PATROL", "PM", missing);
            if (isFinalInspectionApplicableForSnapshot(snapshotTasks, snapshot, missing)) {
                requirePqcTaskCoverage(snapshotTasks, snapshot, "FINAL", "FINAL", missing);
            }
        }
        return missing;
    }

    private List<String> invalidOrDuplicatePqcTaskIdentities(List<MesPqcInspectionTaskDO> tasks) {
        List<String> invalid = new ArrayList<>();
        Map<String, Long> countsByIdentity = new LinkedHashMap<>();
        for (MesPqcInspectionTaskDO task : tasks) {
            String invalidReason = invalidPqcTaskIdentityReason(task);
            if (invalidReason != null) {
                invalid.add(invalidReason);
                continue;
            }
            countsByIdentity.merge(fullPqcTaskIdentity(task), 1L, Long::sum);
        }
        countsByIdentity.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(entry -> entry.getKey() + " 存在重复任务")
                .forEach(invalid::add);
        return invalid;
    }

    private String invalidPqcTaskIdentityReason(MesPqcInspectionTaskDO task) {
        if (task == null || task.getId() == null || task.getActiveOrderId() == null
                || task.getRouteProcessId() == null || task.getProcessId() == null
                || task.getRegulationVersionId() == null || task.getQaProcessId() == null
                || StrUtil.isBlank(task.getQaItemCode()) || StrUtil.isBlank(task.getInspectionRuleKey())
                || StrUtil.isBlank(task.getInspectionType()) || task.getBusinessDate() == null
                || StrUtil.isBlank(task.getShiftCode()) || task.getRoundNo() == null) {
            return "taskId=" + (task == null ? null : task.getId()) + " PQC 任务真实身份字段不完整";
        }
        return null;
    }

    private boolean isFinalInspectionApplicableForSnapshot(List<MesPqcInspectionTaskDO> tasks,
                                                           MesProcessPoolActiveOrderProcessSnapshotDO snapshot,
                                                           List<String> missing) {
        List<Long> versionIds = tasks.stream()
                .filter(task -> Objects.equals(snapshot.getRouteProcessId(), task.getRouteProcessId())
                        && Objects.equals(snapshot.getProcessId(), task.getProcessId())
                        && task.getRegulationVersionId() != null)
                .map(MesPqcInspectionTaskDO::getRegulationVersionId)
                .distinct()
                .toList();
        if (versionIds.isEmpty()) {
            missing.add("routeProcessId=" + snapshot.getRouteProcessId()
                    + ", processId=" + snapshot.getProcessId()
                    + " 缺少发布规程版本，无法证明末检是否适用");
            return true;
        }
        boolean finalRequired = false;
        for (Long versionId : versionIds) {
            MesQaInspectionRegulationVersionDO version = regulationVersionMapper.selectById(versionId);
            if (version == null) {
                missing.add("routeProcessId=" + snapshot.getRouteProcessId()
                        + ", processId=" + snapshot.getProcessId()
                        + ", regulationVersionId=" + versionId + " 发布规程版本不存在");
                finalRequired = true;
                continue;
            }
            if (Boolean.FALSE.equals(version.getFinalInspectionApplicable())) {
                if (StrUtil.isBlank(version.getFinalInspectionNotApplicableReason())) {
                    missing.add("routeProcessId=" + snapshot.getRouteProcessId()
                            + ", processId=" + snapshot.getProcessId()
                            + ", regulationVersionId=" + version.getId() + " 末检不适用但缺少明确依据");
                    finalRequired = true;
                }
                continue;
            }
            if (Boolean.TRUE.equals(version.getFinalInspectionApplicable())) {
                finalRequired = true;
            }
        }
        return finalRequired;
    }

    private void requirePqcTaskCoverage(List<MesPqcInspectionTaskDO> tasks,
                                        MesProcessPoolActiveOrderProcessSnapshotDO snapshot,
                                        String inspectionType, String shiftCode, List<String> missing) {
        boolean exists = tasks.stream().anyMatch(task ->
                Objects.equals(inspectionType, task.getInspectionType())
                        && Objects.equals(shiftCode, task.getShiftCode())
                        && Objects.equals(PQC_DEFAULT_ROUND_NO, task.getRoundNo()));
        if (!exists) {
            missing.add(requiredPqcTaskCoverageText(snapshot, inspectionType, shiftCode));
        }
    }

    private String requiredPqcTaskCoverageText(MesProcessPoolActiveOrderProcessSnapshotDO snapshot,
                                               String inspectionType, String shiftCode) {
        return "routeProcessId=" + snapshot.getRouteProcessId()
                + ", processId=" + snapshot.getProcessId()
                + ", inspectionType=" + inspectionType
                + ", shiftCode=" + shiftCode
                + ", roundNo=" + PQC_DEFAULT_ROUND_NO;
    }

    private List<String> missingFormalPqcInspectionResultEvidence(List<MesPqcInspectionTaskDO> tasks) {
        List<String> missing = new ArrayList<>();
        for (MesPqcInspectionTaskDO task : tasks) {
            String taskText = fullPqcTaskIdentity(task);
            if (task.getSubmittedEventId() == null) {
                missing.add(taskText + " 缺少正式提交事件");
                continue;
            }
            MesProProcessPoolPqcRecordDO record = pqcRecordMapper.selectByEventId(task.getSubmittedEventId());
            if (!validPqcRecord(task, record)) {
                missing.add(taskText + " 缺少正式提交事件或检验结论记录");
                continue;
            }
            List<MesPqcInspectionPieceDetailDO> pieceDetails = pqcPieceDetailMapper.selectListByTaskId(task.getId());
            String pieceDetailReason = invalidPieceDetailsReason(task, pieceDetails);
            if (pieceDetailReason != null) {
                missing.add(taskText + " " + pieceDetailReason);
                continue;
            }
            List<MesPqcProcessInspectionAggregateDetailDO> aggregateDetails =
                    aggregateDetailMapper.selectListByEventId(task.getSubmittedEventId());
            String aggregateReason = invalidAggregateDetailsReason(task, record, pieceDetails, aggregateDetails);
            if (aggregateReason != null) {
                missing.add(taskText + " " + aggregateReason);
                continue;
            }
            FormalScrapQuantityEvidence scrapQuantityEvidence = formalScrapQuantityEvidence(task, record);
            if (scrapQuantityEvidence.invalidReason() != null) {
                missing.add(taskText + " " + scrapQuantityEvidence.invalidReason());
                continue;
            }
            String expectedResult = expectedInspectionResult(scrapQuantityEvidence.scrapQuantity(), pieceDetails);
            if (!Objects.equals(expectedResult, record.getInspectionResult())) {
                missing.add(taskText + " 检验结论与报废数量/逐件判定不一致，expected=" + expectedResult
                        + ", actual=" + record.getInspectionResult());
                continue;
            }
            String dispositionReason = invalidNonconformanceDispositionReason(task, expectedResult);
            if (dispositionReason != null) {
                missing.add(taskText + " " + dispositionReason);
            }
        }
        return missing;
    }

    private String fullPqcTaskIdentity(MesPqcInspectionTaskDO task) {
        if (task == null) {
            return "taskId=null";
        }
        return "activeOrderId=" + task.getActiveOrderId()
                + ", routeProcessId=" + task.getRouteProcessId()
                + ", processId=" + task.getProcessId()
                + ", regulationVersionId=" + task.getRegulationVersionId()
                + ", qaProcessId=" + task.getQaProcessId()
                + ", qaItemCode=" + task.getQaItemCode()
                + ", inspectionRuleKey=" + task.getInspectionRuleKey()
                + ", inspectionType=" + task.getInspectionType()
                + ", businessDate=" + task.getBusinessDate()
                + ", shiftCode=" + task.getShiftCode()
                + ", roundNo=" + task.getRoundNo();
    }

    private boolean validPqcRecord(MesPqcInspectionTaskDO task, MesProProcessPoolPqcRecordDO record) {
        return task != null
                && record != null
                && record.getId() != null
                && Objects.equals(task.getSubmittedEventId(), record.getEventId())
                && Objects.equals(task.getWorkOrderId(), record.getWorkOrderId())
                && Objects.equals(task.getRouteId(), record.getRouteId())
                && Objects.equals(task.getRouteProcessId(), record.getRouteProcessId())
                && Objects.equals(task.getProcessId(), record.getProcessId())
                && Objects.equals(task.getQaProcessId(), record.getQaProcessId())
                && record.getSignatureId() != null
                && record.getSignatureUserId() != null
                && record.getServerSubmitTime() != null
                && StrUtil.isNotBlank(record.getRawPayload())
                && isPqcInspectionResult(record.getInspectionResult());
    }

    private String invalidPieceDetailsReason(MesPqcInspectionTaskDO task,
                                             List<MesPqcInspectionPieceDetailDO> pieceDetails) {
        if (task.getActualInspectionQuantity() == null || task.getActualInspectionQuantity() <= 0) {
            return "实际检验数量无效";
        }
        if (CollUtil.isEmpty(pieceDetails)) {
            return "缺少逐件判定明细";
        }
        List<Integer> expectedSamples = IntStream.rangeClosed(1, task.getActualInspectionQuantity()).boxed().toList();
        List<Integer> actualSamples = pieceDetails.stream()
                .map(MesPqcInspectionPieceDetailDO::getSampleNo)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        if (!expectedSamples.equals(actualSamples)) {
            return "逐件判定样本号不完整，expected=" + expectedSamples + ", actual=" + actualSamples;
        }
        Set<String> pieceKeys = new LinkedHashSet<>();
        for (MesPqcInspectionPieceDetailDO detail : pieceDetails) {
            if (detail == null
                    || detail.getId() == null
                    || !Objects.equals(task.getId(), detail.getTaskId())
                    || detail.getSampleNo() == null
                    || StrUtil.isBlank(detail.getItemCode())
                    || StrUtil.isBlank(detail.getItemName())
                    || StrUtil.isBlank(detail.getInspectionMethod())
                    || StrUtil.isBlank(detail.getStandardText())
                    || StrUtil.isBlank(detail.getResultType())
                    || StrUtil.isBlank(detail.getItemResult())
                    || StrUtil.isBlank(detail.getMeasuredValue())
                    || !Objects.equals(detail.getItemResult(), detail.getMeasuredValue())
                    || !isPqcInspectionResult(detail.getJudgement())) {
                return "逐件判定明细字段不完整或无效";
            }
            String pieceKey = detail.getSampleNo() + "|" + detail.getItemCode();
            if (!pieceKeys.add(pieceKey)) {
                return "逐件判定明细存在重复样本项目，sampleNo=" + detail.getSampleNo()
                        + ", itemCode=" + detail.getItemCode();
            }
        }
        return null;
    }

    private String invalidAggregateDetailsReason(MesPqcInspectionTaskDO task, MesProProcessPoolPqcRecordDO record,
                                                 List<MesPqcInspectionPieceDetailDO> pieceDetails,
                                                 List<MesPqcProcessInspectionAggregateDetailDO> aggregateDetails) {
        if (!MesProProcessPoolPqcRecordDO.PROCESS_INSPECTION_AGGREGATION_STATUS_AGGREGATED.equals(
                record.getProcessInspectionAggregationStatus())
                || record.getProcessInspectionReviewId() == null
                || record.getProcessInspectionAggregatedAt() == null) {
            return "缺少 PQC 组长正式聚合记录";
        }
        if (CollUtil.isEmpty(aggregateDetails)) {
            return "缺少 PQC 组长聚合明细";
        }
        if (aggregateDetails.size() != pieceDetails.size()) {
            return "PQC 组长聚合明细数量与逐件明细不一致";
        }
        Map<Long, MesPqcProcessInspectionAggregateDetailDO> aggregateByPieceId = aggregateDetails.stream()
                .filter(Objects::nonNull)
                .filter(detail -> detail.getSourcePieceDetailId() != null)
                .collect(Collectors.toMap(MesPqcProcessInspectionAggregateDetailDO::getSourcePieceDetailId,
                        detail -> detail, (left, right) -> left, LinkedHashMap::new));
        if (aggregateByPieceId.size() != pieceDetails.size()) {
            return "PQC 组长聚合明细未逐件关联正式明细";
        }
        for (MesPqcInspectionPieceDetailDO pieceDetail : pieceDetails) {
            MesPqcProcessInspectionAggregateDetailDO aggregateDetail = aggregateByPieceId.get(pieceDetail.getId());
            if (!aggregateDetailMatchesTask(task, record, pieceDetail, aggregateDetail)) {
                return "PQC 组长聚合明细与正式任务或逐件明细不一致";
            }
        }
        return null;
    }

    private boolean aggregateDetailMatchesTask(MesPqcInspectionTaskDO task,
                                               MesProProcessPoolPqcRecordDO record,
                                               MesPqcInspectionPieceDetailDO pieceDetail,
                                               MesPqcProcessInspectionAggregateDetailDO aggregateDetail) {
        return aggregateDetail != null
                && aggregateDetail.getId() != null
                && Objects.equals(record.getId(), aggregateDetail.getSourcePqcRecordId())
                && Objects.equals(pieceDetail.getId(), aggregateDetail.getSourcePieceDetailId())
                && Objects.equals(task.getSubmittedEventId(), aggregateDetail.getEventId())
                && Objects.equals(record.getProcessInspectionReviewId(), aggregateDetail.getReviewId())
                && Objects.equals(task.getId(), aggregateDetail.getPqcTaskId())
                && Objects.equals(task.getActiveOrderId(), aggregateDetail.getActiveOrderId())
                && Objects.equals(task.getWorkOrderId(), aggregateDetail.getWorkOrderId())
                && Objects.equals(task.getRouteId(), aggregateDetail.getRouteId())
                && Objects.equals(task.getRouteVersionId(), aggregateDetail.getRouteVersionId())
                && Objects.equals(task.getRouteProcessId(), aggregateDetail.getRouteProcessId())
                && Objects.equals(task.getProcessId(), aggregateDetail.getProcessId())
                && Objects.equals(task.getRegulationVersionId(), aggregateDetail.getRegulationVersionId())
                && Objects.equals(task.getInspectionType(), aggregateDetail.getInspectionType())
                && Objects.equals(task.getBusinessDate(), aggregateDetail.getBusinessDate())
                && Objects.equals(task.getShiftCode(), aggregateDetail.getShiftCode())
                && Objects.equals(task.getRoundNo(), aggregateDetail.getRoundNo())
                && Objects.equals(task.getActualInspectionQuantity(), aggregateDetail.getActualInspectionQuantity())
                && Objects.equals(pieceDetail.getSampleNo(), aggregateDetail.getSampleNo())
                && Objects.equals(pieceDetail.getItemCode(), aggregateDetail.getItemCode())
                && Objects.equals(pieceDetail.getItemName(), aggregateDetail.getItemName())
                && Objects.equals(pieceDetail.getInspectionMethod(), aggregateDetail.getInspectionMethod())
                && Objects.equals(pieceDetail.getStandardText(), aggregateDetail.getStandardText())
                && Objects.equals(pieceDetail.getStandardLowerLimit(), aggregateDetail.getStandardLowerLimit())
                && Objects.equals(pieceDetail.getStandardUpperLimit(), aggregateDetail.getStandardUpperLimit())
                && Objects.equals(pieceDetail.getStandardUnit(), aggregateDetail.getStandardUnit())
                && Objects.equals(pieceDetail.getStandardPrecision(), aggregateDetail.getStandardPrecision())
                && Objects.equals(pieceDetail.getResultType(), aggregateDetail.getResultType())
                && Objects.equals(pieceDetail.getItemResult(), aggregateDetail.getItemResult())
                && Objects.equals(pieceDetail.getMeasuredValue(), aggregateDetail.getMeasuredValue())
                && Objects.equals(pieceDetail.getJudgement(), aggregateDetail.getJudgement())
                && Objects.equals(record.getProcessInspectionAggregatedAt(), aggregateDetail.getAggregatedAt());
    }

    private FormalScrapQuantityEvidence formalScrapQuantityEvidence(MesPqcInspectionTaskDO task,
                                                                    MesProProcessPoolPqcRecordDO record) {
        JSONObject payload;
        try {
            payload = JSON.parseObject(record.getRawPayload());
        } catch (JSONException ex) {
            return new FormalScrapQuantityEvidence(null, "正式 PQC payload 无法解析 scrapQuantity");
        }
        if (payload == null || !payload.containsKey("scrapQuantity")) {
            return new FormalScrapQuantityEvidence(null, "正式 PQC payload 缺少 scrapQuantity");
        }
        Object rawScrapQuantity = payload.get("scrapQuantity");
        if (!(rawScrapQuantity instanceof Number)) {
            return new FormalScrapQuantityEvidence(null, "正式 PQC payload 的 scrapQuantity 不是数字");
        }
        int scrapQuantity;
        try {
            scrapQuantity = new BigDecimal(rawScrapQuantity.toString()).intValueExact();
        } catch (ArithmeticException | NumberFormatException ex) {
            return new FormalScrapQuantityEvidence(null, "正式 PQC payload 的 scrapQuantity 不是整数");
        }
        if (scrapQuantity < 0 || scrapQuantity > task.getActualInspectionQuantity()) {
            return new FormalScrapQuantityEvidence(null,
                    "正式 PQC payload 的 scrapQuantity 超出实际检验数量，scrapQuantity=" + scrapQuantity
                            + ", actualInspectionQuantity=" + task.getActualInspectionQuantity());
        }
        return new FormalScrapQuantityEvidence(scrapQuantity, null);
    }

    private String expectedInspectionResult(Integer scrapQuantity, List<MesPqcInspectionPieceDetailDO> pieceDetails) {
        if (scrapQuantity != null && scrapQuantity > 0) {
            return MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE;
        }
        return pieceDetails.stream()
                .anyMatch(detail -> MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE.equals(
                        detail.getJudgement()))
                ? MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE
                : MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS;
    }

    private String invalidNonconformanceDispositionReason(MesPqcInspectionTaskDO task, String expectedResult) {
        if (!MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE.equals(expectedResult)) {
            return null;
        }
        MesProEdhrNonconformanceReviewDO review = nonconformanceReviewMapper.selectLatestBySource(
                MesProEdhrNonconformanceReviewService.SOURCE_TYPE_PQC_SUBMISSION, task.getSubmittedEventId());
        if (review == null) {
            return "失败检验缺少不合格处置记录";
        }
        if (!MesProEdhrNonconformanceReviewService.STATUS_CLOSED.equals(review.getReviewStatus())
                || !ACCEPTED_NONCONFORMANCE_DISPOSITIONS.contains(review.getDisposition())
                || StrUtil.hasBlank(review.getReviewMaterialUrl(), review.getReviewOpinion(), review.getQaSignature())
                || review.getQaUserId() == null
                || review.getClosedAt() == null) {
            return "失败检验不合格处置证据未闭环";
        }
        return null;
    }

    private boolean isPqcInspectionResult(String result) {
        return result != null && PQC_INSPECTION_RESULTS.contains(result);
    }

    private List<String> duplicateInventoryTraceIdentities(List<MesProcessPoolActiveOrderTransferTraceDO> traces) {
        Map<String, Long> countsByIdentity = new LinkedHashMap<>();
        for (MesProcessPoolActiveOrderTransferTraceDO trace : traces) {
            countsByIdentity.merge(inventoryTraceIdentity(trace), 1L, Long::sum);
        }
        return countsByIdentity.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList();
    }

    private String inventoryTraceIdentity(MesProcessPoolActiveOrderTransferTraceDO trace) {
        return "sourceType=" + trace.getSourceType()
                + ", direction=" + trace.getDirection()
                + ", sourceObjectType=" + trace.getSourceObjectType()
                + ", sourceObjectId=" + trace.getSourceObjectId()
                + ", transferId=" + trace.getTransferId()
                + ", transferLineId=" + trace.getTransferLineId()
                + ", transferDetailId=" + trace.getTransferDetailId()
                + ", materialStockId=" + trace.getMaterialStockId()
                + ", itemId=" + trace.getItemId()
                + ", batchId=" + trace.getBatchId();
    }

    private record FormalScrapQuantityEvidence(Integer scrapQuantity, String invalidReason) {
    }

    private String invalidInventoryTraceReason(MesProcessPoolActiveOrderTransferTraceDO trace) {
        if (trace == null) {
            return "trace=null：正式追溯行缺失";
        }
        String traceLabel = "traceId=" + trace.getId() + ", sourceType=" + trace.getSourceType();
        if (StrUtil.isBlank(trace.getSourceType()) || StrUtil.isBlank(trace.getDirection())) {
            return traceLabel + "：来源类型或方向为空";
        }
        if (trace.getQuantity() == null || trace.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            return traceLabel + "：数量为空或非正";
        }
        if (trace.getMaterialStockId() == null || trace.getBatchId() == null || trace.getItemId() == null
                || StrUtil.isBlank(trace.getSourceObjectId()) || StrUtil.isBlank(trace.getSourceObjectType())
                || StrUtil.isBlank(trace.getSourceObjectCode())) {
            return traceLabel + "：正式库存/批次/来源对象不完整";
        }
        if (MesProcessPoolActiveOrderTransferTraceDO.SOURCE_TYPE_TRANSFER.equals(trace.getSourceType())
                && (trace.getTransferId() == null || trace.getTransferLineId() == null
                || trace.getTransferDetailId() == null)) {
            return traceLabel + "：正式调拨单据/行/明细身份不完整";
        }
        if (MOVEMENT_SOURCE_TYPES_REQUIRING_CLOSED_STATUS.contains(trace.getSourceType())
                && !CLOSED_SOURCE_STATUSES.contains(trace.getSourceStatus())) {
            return traceLabel + "：来源状态未闭环，sourceStatus=" + trace.getSourceStatus();
        }
        return null;
    }

    private MesOrderReleaseCompletenessCheck activeOrderMissing(String checkCode, String checkName,
                                                                String category, String module,
                                                                MesProEdhrBatchExecutionDO batch) {
        return blocker(checkCode, checkName, category, module, "ACTIVE_ORDER",
                String.valueOf(batch.getWorkOrderId()), batch.getWorkOrderCode(),
                "当前批次缺少可追溯的统一 activeOrderId 来源",
                "先把生产订单加入统一活跃订单并完成对应来源绑定");
    }

    private String summarizeIds(String prefix, Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return prefix + "：[]";
        }
        List<Long> sample = ids.stream().limit(12).toList();
        if (ids.size() <= sample.size()) {
            return prefix + "：" + sample;
        }
        return prefix + "：共 " + ids.size() + " 个，示例 " + sample;
    }

    private String summarizeText(String prefix, List<String> items) {
        if (CollUtil.isEmpty(items)) {
            return prefix + "：[]";
        }
        List<String> sample = items.stream().limit(12).toList();
        if (items.size() <= sample.size()) {
            return prefix + "：" + sample;
        }
        return prefix + "：共 " + items.size() + " 个，示例 " + sample;
    }

    private MesOrderReleaseCompletenessCheck pass(String checkCode, String checkName, String category, String module,
                                                  String sourceObjectType, String sourceObjectId,
                                                 String sourceObjectCode, String reason) {
        return new MesOrderReleaseCompletenessCheck(checkCode, checkName, category, RESULT_PASS, SEVERITY_INFO, module,
                sourceObjectType, sourceObjectId, sourceObjectCode, reason, "无需处理");
    }

    private MesOrderReleaseCompletenessCheck notApplicable(String checkCode, String checkName, String category,
                                                          String module, String sourceObjectType,
                                                          String sourceObjectId, String sourceObjectCode,
                                                          String reason) {
        return new MesOrderReleaseCompletenessCheck(checkCode, checkName, category, RESULT_NOT_APPLICABLE,
                SEVERITY_INFO, module, sourceObjectType, sourceObjectId, sourceObjectCode, reason, "无需处理");
    }

    private MesOrderReleaseCompletenessCheck blocker(String checkCode, String checkName, String category, String module,
                                                    String sourceObjectType, String sourceObjectId,
                                                    String sourceObjectCode, String reason, String suggestion) {
        return new MesOrderReleaseCompletenessCheck(checkCode, checkName, category, RESULT_BLOCKER, SEVERITY_BLOCKER,
                module, sourceObjectType, sourceObjectId, sourceObjectCode, reason, suggestion);
    }
}
