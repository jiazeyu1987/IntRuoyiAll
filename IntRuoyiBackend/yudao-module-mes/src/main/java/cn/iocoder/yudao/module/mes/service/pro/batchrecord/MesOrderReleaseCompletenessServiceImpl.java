package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderTransferTraceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolWorkOrderAbnormalDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.materialstock.MesWmMaterialStockDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderTransferTraceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolWorkOrderAbnormalMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.materialstock.MesWmMaterialStockMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

    @Resource
    private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Resource
    private MesPqcInspectionTaskMapper pqcInspectionTaskMapper;
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
        return pass(MesProEdhrReleaseServiceImpl.CHECK_INSPECTION_RESULT, "检验结果检查",
                "INSPECTION", MODULE_QMS, "PQC_INSPECTION_TASK", String.valueOf(activeOrder.getId()),
                String.valueOf(activeOrder.getId()), "PQC 检验任务均已确认");
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
                .selectListByActiveOrderIdAndSourceTypes(activeOrder.getId(), List.of(
                        MesProcessPoolActiveOrderTransferTraceDO.SOURCE_TYPE_TRANSFER,
                        MesProcessPoolActiveOrderTransferTraceDO.SOURCE_TYPE_SHIPMENT,
                        MesProcessPoolActiveOrderTransferTraceDO.SOURCE_TYPE_REPLENISHMENT,
                        MesProcessPoolActiveOrderTransferTraceDO.SOURCE_TYPE_RETURN,
                        MesProcessPoolActiveOrderTransferTraceDO.SOURCE_TYPE_BATCH_TRACE));
        if (CollUtil.isEmpty(traces)) {
            return blocker(MesProEdhrReleaseServiceImpl.CHECK_INVENTORY_CONSISTENCY, "库存一致性检查",
                    "INVENTORY", MODULE_WMS, "ACTIVE_ORDER_TRANSFER_TRACE", String.valueOf(activeOrder.getId()),
                    String.valueOf(activeOrder.getId()), "未找到 activeOrderId 的调拨/发货/补退料/批次库存追溯",
                    "同步并绑定正式调拨、发货、补料、退料和批次库存来源后重新预检");
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
        if (batch.getWorkOrderId() == null || batch.getRouteId() == null) {
            return null;
        }
        return activeOrderMapper.selectActiveByWorkOrderAndRoute(batch.getWorkOrderId(), batch.getRouteId());
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
