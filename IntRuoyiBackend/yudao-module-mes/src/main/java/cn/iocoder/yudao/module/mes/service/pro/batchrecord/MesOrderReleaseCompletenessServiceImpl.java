package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderTransferTraceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolWorkOrderAbnormalDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.materialstock.MesWmMaterialStockDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderTransferTraceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolWorkOrderAbnormalMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.materialstock.MesWmMaterialStockMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
        return pass(MesProEdhrReleaseServiceImpl.CHECK_INSPECTION_RESULT, "检验结果检查",
                "INSPECTION", MODULE_QMS, "PQC_INSPECTION_TASK", String.valueOf(activeOrder.getId()),
                String.valueOf(activeOrder.getId()), "PQC 检验任务身份完整且均已确认");
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
        List<String> missing = new ArrayList<>();
        for (MesProcessPoolActiveOrderProcessSnapshotDO snapshot : snapshots) {
            requirePqcTaskIdentity(tasks, snapshot, "FIRST", "FIRST", missing);
            requirePqcTaskIdentity(tasks, snapshot, "PATROL", "AM", missing);
            requirePqcTaskIdentity(tasks, snapshot, "PATROL", "PM", missing);
            if (isFinalInspectionApplicableForSnapshot(tasks, snapshot, missing)) {
                requirePqcTaskIdentity(tasks, snapshot, "FINAL", "FINAL", missing);
            }
        }
        return missing;
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
        if (versionIds.size() > 1) {
            missing.add("routeProcessId=" + snapshot.getRouteProcessId()
                    + ", processId=" + snapshot.getProcessId()
                    + " 存在多个发布规程版本：" + versionIds);
            return true;
        }
        MesQaInspectionRegulationVersionDO version = regulationVersionMapper.selectById(versionIds.get(0));
        if (version == null) {
            missing.add("routeProcessId=" + snapshot.getRouteProcessId()
                    + ", processId=" + snapshot.getProcessId()
                    + ", regulationVersionId=" + versionIds.get(0) + " 发布规程版本不存在");
            return true;
        }
        if (Boolean.FALSE.equals(version.getFinalInspectionApplicable())) {
            if (StrUtil.isBlank(version.getFinalInspectionNotApplicableReason())) {
                missing.add("routeProcessId=" + snapshot.getRouteProcessId()
                        + ", processId=" + snapshot.getProcessId()
                        + ", regulationVersionId=" + version.getId() + " 末检不适用但缺少明确依据");
                return true;
            }
            return false;
        }
        return Boolean.TRUE.equals(version.getFinalInspectionApplicable());
    }

    private void requirePqcTaskIdentity(List<MesPqcInspectionTaskDO> tasks,
                                        MesProcessPoolActiveOrderProcessSnapshotDO snapshot,
                                        String inspectionType, String shiftCode, List<String> missing) {
        boolean exists = tasks.stream().anyMatch(task ->
                Objects.equals(snapshot.getRouteProcessId(), task.getRouteProcessId())
                        && Objects.equals(snapshot.getProcessId(), task.getProcessId())
                        && Objects.equals(inspectionType, task.getInspectionType())
                        && Objects.equals(shiftCode, task.getShiftCode())
                        && Objects.equals(PQC_DEFAULT_ROUND_NO, task.getRoundNo()));
        if (!exists) {
            missing.add("routeProcessId=" + snapshot.getRouteProcessId()
                    + ", processId=" + snapshot.getProcessId()
                    + ", inspectionType=" + inspectionType
                    + ", shiftCode=" + shiftCode
                    + ", roundNo=" + PQC_DEFAULT_ROUND_NO);
        }
    }

    private String invalidInventoryTraceReason(MesProcessPoolActiveOrderTransferTraceDO trace) {
        if (trace == null) {
            return "trace=null：正式追溯行缺失";
        }
        String traceLabel = "traceId=" + trace.getId() + ", sourceType=" + trace.getSourceType();
        if (trace.getQuantity() == null || trace.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            return traceLabel + "：数量为空或非正";
        }
        if (trace.getMaterialStockId() == null || trace.getBatchId() == null || trace.getItemId() == null
                || trace.getSourceObjectId() == null || trace.getSourceObjectType() == null
                || trace.getSourceObjectCode() == null) {
            return traceLabel + "：正式库存/批次/来源对象不完整";
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
