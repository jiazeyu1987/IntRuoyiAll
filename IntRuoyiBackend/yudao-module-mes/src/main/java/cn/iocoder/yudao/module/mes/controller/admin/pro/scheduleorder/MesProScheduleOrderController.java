package cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderAdmissionDiffPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderAdmissionDiffPageRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderAdmissionDiffRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderBatchReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderCreateFromWorkOrderReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderCreateFromWorkOrdersReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderDailyCompareRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderExportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderOperationLogRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderPreflightRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderPreflightReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderProcessRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderProcessWipRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderProcessWipSettingsReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderUpdatePriorityReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderUpdateReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProScheduleIssueDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesKingdeeProductionMaterialListDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProScheduleIssueMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesKingdeeProductionMaterialListMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProFeedbackStatusEnum;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteService;
import cn.iocoder.yudao.module.mes.service.pro.scheduleorder.MesProScheduleOrderService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleDailyCompareStatusEnum.AHEAD;
import static cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleDailyCompareStatusEnum.BEHIND;
import static cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleDailyCompareStatusEnum.NO_FEEDBACK;
import static cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleDailyCompareStatusEnum.NO_PLAN;
import static cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleDailyCompareStatusEnum.NORMAL;

@Tag(name = "管理后台 - MES 排产工单")
@RestController
@RequestMapping("/mes/pro/schedule-order")
@Validated
public class MesProScheduleOrderController {

    static final List<String> DEFAULT_EXPORT_COLUMNS = List.of(
            "erpWorkOrderCode",
            "productCode",
            "productName",
            "productSpecification",
            "quantityProgress",
            "promiseDate",
            "latestStartTime",
            "plannedStartTime",
            "plannedEndTime",
            "priorityNo",
            "productionMaterialListSummary",
            "currentProcessName",
            "createTime");

    static final Map<String, Integer> EXPORT_COLUMN_INDEX_MAP = buildExportColumnIndexMap();

    @Resource
    private MesProScheduleOrderService scheduleOrderService;
    @Resource
    private MesMdItemService itemService;
    @Resource
    private MesProRouteService routeService;
    @Resource
    private MesProProcessService processService;
    @Resource
    private MesProWorkOrderService workOrderService;
    @Resource
    private MesKingdeeProductionMaterialListMapper productionMaterialListMapper;
    @Resource
    private MesProScheduleIssueMapper scheduleIssueMapper;
    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create-from-work-order")
    @Operation(summary = "从生产工单生成排产工单")
    @PreAuthorize("@ss.hasPermission('mes:pro-schedule-order:create')")
    public CommonResult<Long> createFromWorkOrder(
            @Valid @RequestBody MesProScheduleOrderCreateFromWorkOrderReqVO createReqVO) {
        return success(scheduleOrderService.createFromWorkOrder(createReqVO));
    }

    @PostMapping("/create-from-work-orders")
    @Operation(summary = "从生产工单批量生成排产工单")
    @PreAuthorize("@ss.hasPermission('mes:pro-schedule-order:create')")
    public CommonResult<List<Long>> createFromWorkOrders(
            @Valid @RequestBody MesProScheduleOrderCreateFromWorkOrdersReqVO createReqVO) {
        return success(scheduleOrderService.createFromWorkOrders(createReqVO));
    }

    @PutMapping("/priority")
    @Operation(summary = "更新排产工单优先级")
    @PreAuthorize("@ss.hasPermission('mes:pro-schedule-order:update')")
    public CommonResult<Boolean> updatePriority(
            @Valid @RequestBody MesProScheduleOrderUpdatePriorityReqVO updateReqVO) {
        scheduleOrderService.updatePriority(updateReqVO.getId(), updateReqVO.getPriorityNo());
        return success(true);
    }

    @PutMapping("/update")
    @Operation(summary = "修改排产工单交期、优先级和备注")
    @PreAuthorize("@ss.hasPermission('mes:pro-schedule-order:update')")
    public CommonResult<Boolean> updateScheduleOrder(
            @Valid @RequestBody MesProScheduleOrderUpdateReqVO updateReqVO) {
        scheduleOrderService.updateScheduleOrder(updateReqVO);
        return success(true);
    }

    @PostMapping("/freeze")
    @Operation(summary = "冻结排产工单")
    @PreAuthorize("@ss.hasPermission('mes:pro-schedule-order:update')")
    public CommonResult<Boolean> freezeScheduleOrders(
            @Valid @RequestBody MesProScheduleOrderBatchReqVO reqVO) {
        scheduleOrderService.freezeScheduleOrders(reqVO);
        return success(true);
    }

    @PostMapping("/unfreeze")
    @Operation(summary = "解冻排产工单")
    @PreAuthorize("@ss.hasPermission('mes:pro-schedule-order:update')")
    public CommonResult<Boolean> unfreezeScheduleOrders(
            @Valid @RequestBody MesProScheduleOrderBatchReqVO reqVO) {
        scheduleOrderService.unfreezeScheduleOrders(reqVO);
        return success(true);
    }

    @PostMapping("/manual-finish")
    @Operation(summary = "人工完成排产工单")
    @PreAuthorize("@ss.hasPermission('mes:pro-schedule-order:manual-finish')")
    public CommonResult<Boolean> manualFinish(
            @Valid @RequestBody MesProScheduleOrderActionReqVO reqVO) {
        scheduleOrderService.manualFinish(reqVO);
        return success(true);
    }

    @PostMapping("/revoke-manual-finish")
    @Operation(summary = "撤销人工完成排产工单")
    @PreAuthorize("@ss.hasPermission('mes:pro-schedule-order:revoke-complete')")
    public CommonResult<Boolean> revokeManualFinish(
            @Valid @RequestBody MesProScheduleOrderActionReqVO reqVO) {
        scheduleOrderService.revokeManualFinish(reqVO);
        return success(true);
    }

    @DeleteMapping("/batch-delete")
    @Operation(summary = "批量删除排产工单")
    @PreAuthorize("@ss.hasPermission('mes:pro-schedule-order:delete')")
    public CommonResult<Boolean> deleteScheduleOrders(
            @Valid @RequestBody MesProScheduleOrderBatchReqVO reqVO) {
        scheduleOrderService.deleteScheduleOrders(reqVO);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得排产工单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro-schedule-order:query')")
    public CommonResult<MesProScheduleOrderRespVO> getScheduleOrder(@RequestParam("id") Long id) {
        MesProScheduleOrderDO scheduleOrder = scheduleOrderService.getScheduleOrder(id);
        if (scheduleOrder == null) {
            return success(null);
        }
        return success(buildScheduleOrderRespVOList(List.of(scheduleOrder)).get(0));
    }

    @GetMapping("/page")
    @Operation(summary = "获得排产工单分页")
    @PreAuthorize("@ss.hasPermission('mes:pro-schedule-order:query')")
    public CommonResult<PageResult<MesProScheduleOrderRespVO>> getScheduleOrderPage(
            @Valid MesProScheduleOrderPageReqVO pageReqVO) {
        PageResult<MesProScheduleOrderDO> pageResult = scheduleOrderService.getScheduleOrderPage(pageReqVO);
        return success(new PageResult<>(buildScheduleOrderRespVOList(pageResult.getList()), pageResult.getTotal()));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出排产工单 Excel")
    @PreAuthorize("@ss.hasPermission('mes:pro-schedule-order:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportScheduleOrderExcel(@Valid MesProScheduleOrderPageReqVO pageReqVO,
                                         HttpServletResponse response) throws IOException {
        Set<Integer> includeColumnIndexes = resolveExportColumnIndexes(pageReqVO.getExportColumns());
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        PageResult<MesProScheduleOrderDO> pageResult = scheduleOrderService.getScheduleOrderPage(pageReqVO);
        List<MesProScheduleOrderExportExcelVO> rows = buildScheduleOrderRespVOList(pageResult.getList()).stream()
                .map(MesProScheduleOrderExportExcelVO::from)
                .toList();
        ExcelUtils.write(response, "排产工单.xls", "排产工单",
                MesProScheduleOrderExportExcelVO.class, rows, includeColumnIndexes);
    }

    @GetMapping("/admission-diff")
    @Operation(summary = "获得排产工单待同步差异清单")
    @PreAuthorize("@ss.hasPermission('mes:pro-schedule-order:admission-diff')")
    public CommonResult<MesProScheduleOrderAdmissionDiffPageRespVO> getAdmissionDiff(
            @Valid MesProScheduleOrderAdmissionDiffPageReqVO pageReqVO) {
        MesProScheduleOrderAdmissionDiffPageRespVO result = scheduleOrderService.getAdmissionDiff(pageReqVO);
        enrichAdmissionDiffProducts(result.getList());
        return success(result);
    }

    @PostMapping("/preflight")
    @Operation(summary = "执行排产工单排产前检查")
    @PreAuthorize("@ss.hasPermission('mes:pro-schedule-order:preflight')")
    public CommonResult<MesProScheduleOrderPreflightRespVO> preflight(
            @Valid @RequestBody MesProScheduleOrderPreflightReqVO reqVO) {
        return success(scheduleOrderService.preflight(reqVO));
    }

    @GetMapping("/process-list")
    @Operation(summary = "获得排产工单工序快照列表")
    @Parameter(name = "scheduleOrderId", description = "排产工单编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro-schedule-order:query')")
    public CommonResult<List<MesProScheduleOrderProcessRespVO>> getProcessList(
            @RequestParam("scheduleOrderId") Long scheduleOrderId) {
        List<MesProScheduleOrderProcessDO> processes =
                scheduleOrderService.getScheduleOrderProcessList(scheduleOrderId);
        Map<Long, MesProScheduleOrderService.ProcessProgressMetrics> progressMetricsMap =
                scheduleOrderService.calculateProcessProgressMetrics(scheduleOrderId, processes);
        Map<Long, MesProProcessDO> processMap = processService.getProcessMap(
                convertSet(processes, MesProScheduleOrderProcessDO::getProcessId));
        List<MesProFeedbackDO> feedbackList = scheduleOrderService.getProgressFeedbackList(scheduleOrderId);
        Map<Long, List<MesProFeedbackDO>> feedbackMap = feedbackList.stream()
                .filter(feedback -> feedback.getScheduleOrderProcessId() != null)
                .collect(Collectors.groupingBy(MesProFeedbackDO::getScheduleOrderProcessId));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(
                convertSet(feedbackList, MesProFeedbackDO::getFeedbackUserId));
        return success(BeanUtils.toBean(processes, MesProScheduleOrderProcessRespVO.class, vo -> {
            MesProProcessDO process = processMap.get(vo.getProcessId());
            if (process != null) {
                vo.setProcessCode(process.getCode());
                vo.setProcessName(process.getName());
            }
            MesProScheduleOrderService.ProcessProgressMetrics metrics = progressMetricsMap.get(vo.getId());
            if (metrics != null) {
                vo.setEffectiveCompletedQuantity(metrics.effectiveCompletedQuantity());
                vo.setPendingApprovalQuantity(metrics.pendingApprovalQuantity());
                vo.setPendingInspectionQuantity(metrics.pendingInspectionQuantity());
                vo.setOverReportedQuantity(metrics.overReportedQuantity());
                vo.setReportedQuantity(metrics.reportedQuantity());
                vo.setRemainingQuantity(metrics.remainingQuantity());
                vo.setProgressPercent(metrics.progressPercent());
            }
            List<MesProFeedbackDO> processFeedbackList = feedbackMap.getOrDefault(vo.getId(), Collections.emptyList());
            vo.setFeedbackCount(processFeedbackList.size());
            vo.setLatestFeedbackTime(processFeedbackList.stream()
                    .map(MesProFeedbackDO::getFeedbackTime)
                    .filter(java.util.Objects::nonNull)
                    .max(Comparator.naturalOrder())
                    .orElse(null));
            vo.setFeedbackHistoryList(BeanUtils.toBean(processFeedbackList,
                    MesProScheduleOrderProcessRespVO.FeedbackHistoryRespVO.class, history -> {
                        AdminUserRespDTO user = userMap.get(history.getFeedbackUserId());
                        if (user != null) {
                            history.setFeedbackUserNickname(user.getNickname());
                        }
                        history.setStatusName(getFeedbackStatusName(history.getStatus()));
                    }));
        }));
    }

    private String getFeedbackStatusName(Integer status) {
        if (status == null) {
            return null;
        }
        for (MesProFeedbackStatusEnum item : MesProFeedbackStatusEnum.values()) {
            if (item.getStatus().equals(status)) {
                return item.getName();
            }
        }
        return null;
    }

    @GetMapping("/process-wip-statistics")
    @Operation(summary = "获得当前工序在制订单统计")
    @PreAuthorize("@ss.hasPermission('mes:pro-schedule-order:query')")
    public CommonResult<List<MesProScheduleOrderProcessWipRespVO>> getProcessWipStatistics() {
        return success(scheduleOrderService.getProcessWipStatistics());
    }

    @PutMapping("/process-wip-settings")
    @Operation(summary = "保存当前工序在制夜班与开排日期")
    @PreAuthorize("@ss.hasPermission('mes:pro-schedule-order:update')")
    public CommonResult<Boolean> saveProcessWipSettings(
            @Valid @RequestBody MesProScheduleOrderProcessWipSettingsReqVO reqVO) {
        scheduleOrderService.saveProcessWipSettings(reqVO);
        return success(true);
    }

    @PostMapping("/sync-progress")
    @Operation(summary = "按真实报工同步排产工单进度")
    @Parameter(name = "scheduleOrderId", description = "排产工单编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro-schedule-order:update')")
    public CommonResult<Boolean> syncProgress(@RequestParam("scheduleOrderId") Long scheduleOrderId) {
        scheduleOrderService.syncFeedbackProgress(scheduleOrderId);
        return success(true);
    }

    @GetMapping("/daily-compare")
    @Operation(summary = "获得排产工单按天计划实际对比")
    @PreAuthorize("@ss.hasPermission('mes:pro-schedule-order:query')")
    public CommonResult<List<MesProScheduleOrderDailyCompareRespVO>> getDailyCompare(
            @RequestParam("scheduleOrderId") Long scheduleOrderId,
            @RequestParam(value = "startDate", required = false) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) LocalDate endDate) {
        return success(BeanUtils.toBean(scheduleOrderService.getDailyCompare(scheduleOrderId, startDate, endDate),
                MesProScheduleOrderDailyCompareRespVO.class,
                vo -> vo.setStatusLabel(resolveDailyCompareStatusLabel(vo.getStatus()))));
    }

    @GetMapping("/operation-log")
    @Operation(summary = "获得排产工单操作追溯")
    @PreAuthorize("@ss.hasPermission('mes:pro-schedule-order:query')")
    public CommonResult<List<MesProScheduleOrderOperationLogRespVO>> getOperationLog(
            @RequestParam("scheduleOrderId") Long scheduleOrderId) {
        return success(BeanUtils.toBean(scheduleOrderService.getOperationLogList(scheduleOrderId),
                MesProScheduleOrderOperationLogRespVO.class));
    }

    private List<MesProScheduleOrderRespVO> buildScheduleOrderRespVOList(List<MesProScheduleOrderDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        Map<Long, MesMdItemDO> itemMap = itemService.getItemMap(convertSet(list, MesProScheduleOrderDO::getProductId));
        Map<Long, MesProRouteDO> routeMap = routeService.getRouteMap(convertSet(list, MesProScheduleOrderDO::getRouteId));
        Map<Long, List<MesKingdeeProductionMaterialListDO>> materialListMap = buildMaterialListMapByWorkOrderId(list);
        Set<Long> missingWorkOrderCodeIds = list.stream()
                .filter(item -> item.getWorkOrderId() != null)
                .filter(item -> item.getErpWorkOrderCode() == null || item.getErpWorkOrderCode().isBlank())
                .map(MesProScheduleOrderDO::getWorkOrderId)
                .collect(Collectors.toSet());
        Map<Long, MesProWorkOrderDO> workOrderMap = CollUtil.isEmpty(missingWorkOrderCodeIds)
                ? Collections.emptyMap()
                : workOrderService.getWorkOrderMap(missingWorkOrderCodeIds);
        List<MesProScheduleOrderProcessDO> allProcesses = scheduleOrderService
                .getScheduleOrderProcessListByScheduleOrderIds(convertSet(list, MesProScheduleOrderDO::getId))
                .stream().toList();
        Map<Long, List<MesProScheduleOrderProcessDO>> processMap = allProcesses.stream()
                .collect(Collectors.groupingBy(MesProScheduleOrderProcessDO::getScheduleOrderId));
        Map<Long, Map<Long, MesProScheduleOrderService.ProcessProgressMetrics>> progressMetricsByOrderId = processMap.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> scheduleOrderService.calculateProcessProgressMetrics(entry.getKey(), entry.getValue())));
        Map<Long, MesProProcessDO> processDefinitionMap = processService.getProcessMap(
                convertSet(allProcesses, MesProScheduleOrderProcessDO::getProcessId));
        Map<Long, List<MesProScheduleIssueDO>> blockingIssueMap = buildBlockingIssueMapByWorkOrderId(list);
        return BeanUtils.toBean(list, MesProScheduleOrderRespVO.class, vo -> {
            MesMdItemDO item = vo.getProductId() == null ? null : itemMap.get(vo.getProductId());
            if (item != null) {
                vo.setProductCode(item.getCode());
                vo.setProductName(item.getName());
                vo.setProductSpecification(item.getSpecification());
            }
            MesProRouteDO route = vo.getRouteId() == null ? null : routeMap.get(vo.getRouteId());
            if (route != null) {
                vo.setRouteCode(route.getCode());
                vo.setRouteName(route.getName());
            }
            MesProWorkOrderDO workOrder = vo.getWorkOrderId() == null ? null : workOrderMap.get(vo.getWorkOrderId());
            if ((vo.getErpWorkOrderCode() == null || vo.getErpWorkOrderCode().isBlank())
                    && workOrder != null) {
                vo.setErpWorkOrderCode(workOrder.getCode());
            }
            applyProductionMaterialListSummary(vo, materialListMap.get(vo.getWorkOrderId()));
            applyProcessProgress(vo, processMap.getOrDefault(vo.getId(), Collections.emptyList()),
                    progressMetricsByOrderId.getOrDefault(vo.getId(), Collections.emptyMap()), processDefinitionMap);
            applyBlockingIssueSummary(vo, blockingIssueMap.get(vo.getWorkOrderId()));
        });
    }

    private Map<Long, List<MesProScheduleIssueDO>> buildBlockingIssueMapByWorkOrderId(
            List<MesProScheduleOrderDO> list) {
        Set<Long> workOrderIds = convertSet(list, MesProScheduleOrderDO::getWorkOrderId);
        if (CollUtil.isEmpty(workOrderIds)) {
            return Collections.emptyMap();
        }
        return scheduleIssueMapper.selectListByWorkOrderIds(workOrderIds).stream()
                .filter(issue -> "BLOCKING".equals(issue.getSeverity()))
                .filter(issue -> !Boolean.TRUE.equals(issue.getResolved()))
                .filter(issue -> issue.getWorkOrderId() != null)
                .collect(Collectors.groupingBy(MesProScheduleIssueDO::getWorkOrderId,
                        LinkedHashMap::new, Collectors.toList()));
    }

    private void applyBlockingIssueSummary(MesProScheduleOrderRespVO vo, List<MesProScheduleIssueDO> issues) {
        if (CollUtil.isEmpty(issues)) {
            vo.setBlockingIssueCount(0);
            vo.setLatestBlockingIssueMessage(null);
            return;
        }
        vo.setBlockingIssueCount(issues.size());
        vo.setLatestBlockingIssueMessage(issues.stream()
                .max(Comparator.comparing(MesProScheduleIssueDO::getId,
                        Comparator.nullsLast(Long::compareTo)))
                .map(MesProScheduleIssueDO::getMessage)
                .filter(message -> message != null && !message.isBlank())
                .orElse("存在阻断问题"));
    }

    private Map<Long, List<MesKingdeeProductionMaterialListDO>> buildMaterialListMapByWorkOrderId(
            List<MesProScheduleOrderDO> list) {
        Set<Long> workOrderIds = convertSet(list, MesProScheduleOrderDO::getWorkOrderId);
        if (CollUtil.isEmpty(workOrderIds)) {
            return Collections.emptyMap();
        }
        return productionMaterialListMapper.selectListByWorkOrderIds(workOrderIds).stream()
                .filter(item -> item.getWorkOrderId() != null)
                .collect(Collectors.groupingBy(MesKingdeeProductionMaterialListDO::getWorkOrderId,
                        LinkedHashMap::new, Collectors.toList()));
    }

    private void applyProductionMaterialListSummary(MesProScheduleOrderRespVO vo,
            List<MesKingdeeProductionMaterialListDO> materialRows) {
        if (CollUtil.isEmpty(materialRows)) {
            vo.setProductionMaterialListCount(0);
            vo.setProductionMaterialListSummary(null);
            return;
        }
        LinkedHashSet<String> billNos = materialRows.stream()
                .map(MesKingdeeProductionMaterialListDO::getSourceBillNo)
                .filter(billNo -> billNo != null && !billNo.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        vo.setProductionMaterialListCount(billNos.size());
        vo.setProductionMaterialListSummary(billNos.isEmpty() ? null : String.join("、", billNos));
    }

    private static Map<String, Integer> buildExportColumnIndexMap() {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (int i = 0; i < DEFAULT_EXPORT_COLUMNS.size(); i++) {
            result.put(DEFAULT_EXPORT_COLUMNS.get(i), i);
        }
        return result;
    }

    static Set<Integer> resolveExportColumnIndexes(List<String> exportColumns) {
        List<String> requestedColumns = exportColumns == null ? DEFAULT_EXPORT_COLUMNS : exportColumns;
        if (CollUtil.isEmpty(requestedColumns)) {
            throw ServiceExceptionUtil.invalidParamException("请至少选择一个导出列");
        }
        Set<String> duplicated = new HashSet<>();
        Set<Integer> includeColumnIndexes = new LinkedHashSet<>();
        for (String column : requestedColumns) {
            if (column == null || column.isBlank()) {
                throw ServiceExceptionUtil.invalidParamException("排产工单导出列不能为空");
            }
            Integer index = EXPORT_COLUMN_INDEX_MAP.get(column);
            if (index == null) {
                throw ServiceExceptionUtil.invalidParamException("排产工单导出列不支持: {}", column);
            }
            if (!duplicated.add(column)) {
                throw ServiceExceptionUtil.invalidParamException("排产工单导出列重复: {}", column);
            }
            includeColumnIndexes.add(index);
        }
        if (includeColumnIndexes.isEmpty()) {
            throw ServiceExceptionUtil.invalidParamException("请至少选择一个导出列");
        }
        return includeColumnIndexes;
    }

    private void enrichAdmissionDiffProducts(List<MesProScheduleOrderAdmissionDiffRespVO> rows) {
        if (CollUtil.isEmpty(rows)) {
            return;
        }
        java.util.Set<Long> productIds = convertSet(rows, MesProScheduleOrderAdmissionDiffRespVO::getProductId);
        if (CollUtil.isEmpty(productIds)) {
            return;
        }
        Map<Long, MesMdItemDO> itemMap = itemService.getItemMap(productIds);
        for (MesProScheduleOrderAdmissionDiffRespVO row : rows) {
            MesMdItemDO item = itemMap.get(row.getProductId());
            if (item == null) {
                continue;
            }
            row.setProductCode(item.getCode());
            row.setProductName(item.getName());
            row.setProductSpecification(item.getSpecification());
        }
    }

    private void applyProcessProgress(MesProScheduleOrderRespVO vo, List<MesProScheduleOrderProcessDO> processes,
                                      Map<Long, MesProScheduleOrderService.ProcessProgressMetrics> progressMetricsMap,
                                      Map<Long, MesProProcessDO> processDefinitionMap) {
        List<MesProScheduleOrderProcessDO> enrichedProcesses = processes.stream()
                .map(process -> mergeProgressMetrics(process, progressMetricsMap.get(process.getId())))
                .toList();
        if (!Boolean.TRUE.equals(vo.getManualFinished())) {
            MesProScheduleOrderService.ProgressSummary summary = scheduleOrderService.calculateProcessAggregateProgressSummary(
                    vo.getQuantity(), enrichedProcesses);
            vo.setTotalQuantity(summary.totalQuantity());
            vo.setCompletedQuantity(summary.completedQuantity());
            vo.setUncompletedQuantity(summary.uncompletedQuantity());
            vo.setProgressPercent(summary.progressPercent());
        }
        populateProcessMetrics(vo, progressMetricsMap.values());
        enrichedProcesses.stream()
                .filter(process -> Boolean.TRUE.equals(process.getEnabled()))
                .sorted(Comparator.comparing(MesProScheduleOrderProcessDO::getSort,
                        Comparator.nullsLast(Integer::compareTo)))
                .filter(process -> process.getProgressPercent() == null
                        || process.getProgressPercent().compareTo(new BigDecimal("100")) < 0)
                .findFirst()
                .ifPresent(process -> {
                    vo.setCurrentProcessId(process.getProcessId());
                    vo.setCurrentRouteProcessId(process.getRouteProcessId());
                    vo.setCurrentProcessProgressPercent(process.getProgressPercent());
                    MesProProcessDO processDefinition = processDefinitionMap.get(process.getProcessId());
                    if (processDefinition != null) {
                        vo.setCurrentProcessCode(processDefinition.getCode());
                        vo.setCurrentProcessName(processDefinition.getName());
                    }
                });
    }

    private MesProScheduleOrderProcessDO mergeProgressMetrics(MesProScheduleOrderProcessDO process,
                                                              MesProScheduleOrderService.ProcessProgressMetrics metrics) {
        if (metrics == null) {
            return process;
        }
        process.setReportedQuantity(metrics.reportedQuantity());
        process.setRemainingQuantity(metrics.remainingQuantity());
        process.setProgressPercent(metrics.progressPercent());
        return process;
    }

    private void populateProcessMetrics(MesProScheduleOrderRespVO vo,
                                        java.util.Collection<MesProScheduleOrderService.ProcessProgressMetrics> metricsList) {
        BigDecimal effectiveCompleted = BigDecimal.ZERO.setScale(6);
        BigDecimal pendingApproval = BigDecimal.ZERO.setScale(6);
        BigDecimal pendingInspection = BigDecimal.ZERO.setScale(6);
        BigDecimal overReported = BigDecimal.ZERO.setScale(6);
        for (MesProScheduleOrderService.ProcessProgressMetrics metrics : metricsList) {
            effectiveCompleted = effectiveCompleted.add(metrics.effectiveCompletedQuantity());
            pendingApproval = pendingApproval.add(metrics.pendingApprovalQuantity());
            pendingInspection = pendingInspection.add(metrics.pendingInspectionQuantity());
            overReported = overReported.add(metrics.overReportedQuantity());
        }
        vo.setEffectiveCompletedQuantity(effectiveCompleted);
        vo.setPendingApprovalQuantity(pendingApproval);
        vo.setPendingInspectionQuantity(pendingInspection);
        vo.setOverReportedQuantity(overReported);
    }

    private String resolveDailyCompareStatusLabel(Integer status) {
        if (status == null) {
            return "未知";
        }
        if (NORMAL.getStatus().equals(status)) {
            return "正常";
        }
        if (AHEAD.getStatus().equals(status)) {
            return "提前";
        }
        if (BEHIND.getStatus().equals(status)) {
            return "滞后";
        }
        if (NO_PLAN.getStatus().equals(status)) {
            return "无计划有报工";
        }
        if (NO_FEEDBACK.getStatus().equals(status)) {
            return "有计划无报工";
        }
        return "未知";
    }

}
