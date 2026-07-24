package cn.iocoder.yudao.module.mes.service.pro.scheduleorder;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.QuickFilter;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderAdmissionDiffPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderAdmissionDiffPageRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderAdmissionDiffRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderAdmissionDiffSummaryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderBatchReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderIssueActionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderPreflightIssueRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderPreflightReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderPreflightRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderPreflightScopeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderPreflightSummaryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderProcessWipSettingsReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderProcessWipRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderUpdateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.MesProWorkOrderPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationWorkerDO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderCreateFromWorkOrderReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderCreateFromWorkOrdersReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMachineMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationWorkerMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowEdgeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteScheduleConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProScheduleCalendarRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProTaskScheduleExtDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDailyCompareDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderOperationLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.task.MesProTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesKingdeeProductionOrderSyncRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowEdgeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteScheduleConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProScheduleCalendarRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProTaskScheduleExtMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderOperationLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.task.MesProTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesKingdeeProductionOrderSyncRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProRouteFlowConfigTypeEnum;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteFlowContextMatcher;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleDailyCompareStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleCapacityModeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProFeedbackStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderDiffStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderRiskStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderRouteStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderStatusEnum;
import cn.iocoder.yudao.module.mes.service.pro.schedule.component.ScheduleDefaultCompatibilityPolicy;
import cn.iocoder.yudao.module.mes.service.pro.schedule.identity.RouteProcessIdentity;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception0;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_PROCESS_FLOW_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_BATCH_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_DELETE_BLOCKED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_FROZEN;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_MANUAL_FINISH_ALREADY;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_MANUAL_FINISH_NOT_ACTIVE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_MANUAL_FINISH_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_PROCESS_WIP_CALENDAR_RULE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_PROCESS_WIP_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_PRIORITY_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_PROMISE_DATE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_REASON_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_ROUTE_PROCESS_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_ROUTE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_ROUTE_SCHEDULE_CONFIG_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_ROUTE_FLOW_CONFIG_DISABLED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_ROUTE_FLOW_CONFIG_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_RESOURCE_CAPACITY_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_SHIFT_HOURS_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_WORK_ORDER_DUPLICATE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_WORK_ORDER_FROZEN;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_WORK_ORDER_NOT_CONFIRMED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_PRODUCTION_QUANTITY_FACTOR_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_PROCESS_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_WORK_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_ACTIVE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_WORKSTATION_PROCESS_MISMATCH;

/**
 * MES 排产工单 Service 实现类
 */
@Service
@Validated
public class MesProScheduleOrderServiceImpl implements MesProScheduleOrderService {

    private static final DateTimeFormatter CODE_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final String CAPACITY_SOURCE_MACHINE = "MACHINE";
    private static final String CAPACITY_SOURCE_WORKER = "WORKER";
    private static final String CAPACITY_SOURCE_UNCONFIGURED = "UNCONFIGURED";
    private static final String CAPACITY_SOURCE_MANUAL_OVERRIDE = "MANUAL_OVERRIDE";
    private static final String CAPACITY_SOURCE_INFINITE_FORMULA = "INFINITE_FORMULA";
    private static final String RESOURCE_STATUS_NORMAL = "NORMAL";
    private static final String RESOURCE_STATUS_CAPACITY_MISSING = "CAPACITY_MISSING";
    private static final String RESOURCE_REASON_NORMAL = "正常";
    private static final String RESOURCE_REASON_UNCONFIGURED = "资源未配置";
    private static final String RESOURCE_REASON_SHIFT_HOURS_MISSING = "班次小时未配置";
    private static final String RESOURCE_REASON_WORKER_CAPACITY_MISSING = "人工单人产能未配置";
    private static final String RESOURCE_REASON_MACHINE_CAPACITY_MISSING = "设备工序产能未配置";
    private static final String RESOURCE_REASON_MACHINE_QUANTITY_MISSING = "设备数量未配置";
    private static final String ADMISSION_READY = "READY_TO_ADMIT";
    private static final String ADMISSION_ALREADY = "ALREADY_ADMITTED";
    private static final String ADMISSION_BLOCKED = "BLOCKED";
    private static final String PREFLIGHT_PASS = "PASS";
    private static final String PREFLIGHT_WARN = "WARN";
    private static final String PREFLIGHT_BLOCKED = "BLOCKED";
    private static final String WARN_DEFAULT_ROUTE_SCHEDULE_CONFIG = "WARN_DEFAULT_ROUTE_SCHEDULE_CONFIG";
    private static final String PERMISSION_ROUTE_UPDATE = "mes:pro-route:update";
    private static final String PERMISSION_ROUTE_CONFIG_UPDATE = "mes:pro-route:schedule-config:update";
    private static final String ROUTE_LIST_TARGET = "MesProRoute";
    private static final String ROUTE_EDIT_TARGET = "MesProRouteEdit";
    private static final String PERMISSION_CALENDAR_UPDATE = "mes:pro-schedule-calendar:update";
    private static final BigDecimal DEFAULT_PRODUCTION_QUANTITY_FACTOR = new BigDecimal("1.000000");

    @Resource
    private MesProScheduleOrderMapper scheduleOrderMapper;
    @Resource
    private MesProScheduleOrderOperationLogMapper scheduleOrderOperationLogMapper;
    @Resource
    private MesProScheduleOrderProcessMapper scheduleOrderProcessMapper;
    @Resource
    private MesProFeedbackMapper feedbackMapper;
    @Resource
    private MesProTaskScheduleExtMapper taskScheduleExtMapper;
    @Resource
    private MesProTaskMapper taskMapper;
    @Resource
    private MesProWorkOrderMapper workOrderMapper;
    @Resource
    private MesKingdeeProductionOrderSyncRecordMapper syncRecordMapper;
    @Resource
    private MesMdItemMapper itemMapper;
    @Resource
    private MesProRouteProductMapper routeProductMapper;
    @Resource
    private MesProRouteMapper routeMapper;
    @Resource
    private MesProRouteProcessMapper routeProcessMapper;
    @Resource
    private MesProRouteProcessService routeProcessService;
    @Resource
    private MesProRouteProcessFlowEdgeMapper routeProcessFlowEdgeMapper;
    @Resource
    private MesProRouteVersionMapper routeVersionMapper;
    @Resource
    private MesProRouteScheduleConfigMapper routeScheduleConfigMapper;
    @Resource
    private MesProRouteFlowConfigMapper routeFlowConfigMapper;
    @Resource
    private MesProRouteFlowProcessConfigMapper routeFlowProcessConfigMapper;
    @Resource
    private MesProScheduleCalendarRuleMapper scheduleCalendarRuleMapper;
    @Resource
    private MesMdWorkstationMapper workstationMapper;
    @Resource
    private MesMdWorkstationMachineMapper workstationMachineMapper;
    @Resource
    private MesMdWorkstationWorkerMapper workstationWorkerMapper;
    @Resource
    private MesDvMachineryMapper machineryMapper;
    @Resource
    private MesDvMachineryProcessMapper machineryProcessMapper;
    @Resource
    private MesProProcessMapper processMapper;
    @Resource
    private ScheduleDefaultCompatibilityPolicy scheduleDefaultCompatibilityPolicy;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFromWorkOrder(MesProScheduleOrderCreateFromWorkOrderReqVO reqVO) {
        return createFromWorkOrder(reqVO, true);
    }

    private Long createFromWorkOrder(MesProScheduleOrderCreateFromWorkOrderReqVO reqVO, boolean requirePromiseDate) {
        if (requirePromiseDate && reqVO.getPromiseDate() == null) {
            throw exception(PRO_SCHEDULE_ORDER_PROMISE_DATE_REQUIRED);
        }
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(reqVO.getWorkOrderId());
        if (workOrder == null) {
            throw exception(PRO_WORK_ORDER_NOT_EXISTS);
        }
        validateWorkOrderSchedulable(workOrder);
        if (CollUtil.isNotEmpty(scheduleOrderMapper.selectListByWorkOrderIds(List.of(workOrder.getId())))) {
            throw exception(PRO_SCHEDULE_ORDER_WORK_ORDER_DUPLICATE);
        }

        MesProRouteProductDO routeProduct = routeProductMapper.selectByItemId(workOrder.getProductId());
        if (routeProduct == null) {
            throw exception(PRO_SCHEDULE_ORDER_ROUTE_REQUIRED);
        }
        MesProRouteDO route = routeMapper.selectById(routeProduct.getRouteId());
        if (route == null || ObjUtil.notEqual(route.getStatus(), CommonStatusEnum.ENABLE.getStatus())) {
            throw exception(PRO_SCHEDULE_ORDER_ROUTE_REQUIRED);
        }
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteId(route.getId());
        if (CollUtil.isEmpty(routeProcesses)) {
            throw exception(PRO_SCHEDULE_ORDER_ROUTE_PROCESS_REQUIRED);
        }

        MesProRouteVersionDO activeRouteVersion = validateScheduleAdmissionRequirements(route, routeProcesses);
        Map<Long, Long> predecessorMap = buildRouteProcessPredecessorMap(route.getId(), routeProcesses);
        Map<Long, MesProProcessDO> processMap = toProcessMap(routeProcesses);
        ResourceSnapshotContext resourceContext = buildResourceSnapshotContext(routeProcesses);
        Map<Long, MesProRouteScheduleConfigDO> scheduleConfigMap =
                buildScheduleConfigMap(activeRouteVersion);
        validateResourceCalculatedSnapshots(routeProcesses, scheduleConfigMap, resourceContext);
        Map<Long, MesProRouteFlowProcessConfigDO> scheduleRouteFlowConfigMap = resolveScheduleRouteFlowConfigMap(route.getId());
        String routeVersion = activeRouteVersion.getVersionNo();
        MesProScheduleOrderDO scheduleOrder = buildScheduleOrder(reqVO, workOrder, route, routeProcesses,
                activeRouteVersion, routeVersion, resourceContext, predecessorMap);
        scheduleOrderMapper.insert(scheduleOrder);
        for (MesProRouteProcessDO routeProcess : routeProcesses) {
            MesProRouteScheduleConfigDO scheduleConfig = scheduleConfigMap.get(routeProcess.getId());
            scheduleOrderProcessMapper.insert(buildProcessSnapshot(scheduleOrder.getId(), workOrder, routeProcess,
                    processMap.get(routeProcess.getProcessId()),
                    resourceContext.snapshotByRouteProcessId.getOrDefault(routeProcess.getId(),
                            ResourceSnapshot.unconfigured(routeProcess.getId())),
                    activeRouteVersion, scheduleConfig, scheduleRouteFlowConfigMap.get(routeProcess.getId()),
                    predecessorMap.get(routeProcess.getId())));
        }
        return scheduleOrder.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> createFromWorkOrders(MesProScheduleOrderCreateFromWorkOrdersReqVO reqVO) {
        if (CollUtil.isEmpty(reqVO.getWorkOrderIds())) {
            throw exception(PRO_SCHEDULE_ORDER_BATCH_REQUIRED);
        }
        List<Long> workOrderIds = reqVO.getWorkOrderIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (CollUtil.isEmpty(workOrderIds)) {
            throw exception(PRO_SCHEDULE_ORDER_BATCH_REQUIRED);
        }
        failFastWhenAnyWorkOrderAlreadyAdmitted(workOrderIds);
        List<Long> scheduleOrderIds = new ArrayList<>();
        for (Long workOrderId : workOrderIds) {
            MesProScheduleOrderCreateFromWorkOrderReqVO singleReqVO = new MesProScheduleOrderCreateFromWorkOrderReqVO();
            singleReqVO.setWorkOrderId(workOrderId);
            singleReqVO.setPromiseDate(reqVO.getPromiseDate());
            scheduleOrderIds.add(createFromWorkOrder(singleReqVO, false));
        }
        return scheduleOrderIds;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePriority(Long id, Integer priorityNo) {
        if (priorityNo == null || priorityNo < 1) {
            throw exception(PRO_SCHEDULE_ORDER_PRIORITY_INVALID);
        }
        MesProScheduleOrderDO scheduleOrder = validateWritableScheduleOrder(id);
        MesProScheduleOrderDO updateObj = new MesProScheduleOrderDO();
        updateObj.setId(id);
        updateObj.setPriorityNo(priorityNo);
        scheduleOrderMapper.updateById(updateObj);
        MesProScheduleOrderDO after = copyForSnapshot(scheduleOrder);
        after.setPriorityNo(priorityNo);
        insertOperationLog(scheduleOrder, after, "UPDATE", "调整优先级");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateScheduleOrder(MesProScheduleOrderUpdateReqVO reqVO) {
        validateOperationReason(reqVO.getReason());
        if (reqVO.getPriorityNo() == null || reqVO.getPriorityNo() < 1) {
            throw exception(PRO_SCHEDULE_ORDER_PRIORITY_INVALID);
        }
        if (reqVO.getPromiseDate() == null) {
            throw exception(PRO_SCHEDULE_ORDER_PROMISE_DATE_REQUIRED);
        }
        MesProScheduleOrderDO scheduleOrder = validateWritableScheduleOrder(reqVO.getId());
        MesProScheduleOrderDO updateObj = new MesProScheduleOrderDO();
        updateObj.setId(reqVO.getId());
        updateObj.setPromiseDate(reqVO.getPromiseDate());
        updateObj.setPriorityNo(reqVO.getPriorityNo());
        updateObj.setRemark(reqVO.getRemark());
        scheduleOrderMapper.updateById(updateObj);

        MesProScheduleOrderDO after = copyForSnapshot(scheduleOrder);
        after.setPromiseDate(reqVO.getPromiseDate());
        after.setPriorityNo(reqVO.getPriorityNo());
        after.setRemark(reqVO.getRemark());
        insertOperationLog(scheduleOrder, after, "UPDATE", reqVO.getReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void freezeScheduleOrders(MesProScheduleOrderBatchReqVO reqVO) {
        validateBatchRequest(reqVO);
        List<MesProScheduleOrderDO> scheduleOrders = getRequiredScheduleOrders(reqVO.getIds());
        List<String> frozenCodes = scheduleOrders.stream()
                .filter(item -> Boolean.TRUE.equals(item.getFrozen()))
                .map(this::getScheduleOrderCode)
                .toList();
        if (CollUtil.isNotEmpty(frozenCodes)) {
            throw exception0(PRO_SCHEDULE_ORDER_FROZEN.getCode(), "排产工单已冻结，禁止重复冻结: {}", frozenCodes);
        }
        LocalDateTime now = LocalDateTime.now();
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        for (MesProScheduleOrderDO scheduleOrder : scheduleOrders) {
            MesProScheduleOrderDO updateObj = new MesProScheduleOrderDO();
            updateObj.setId(scheduleOrder.getId());
            updateObj.setFrozen(Boolean.TRUE);
            updateObj.setFrozenTime(now);
            updateObj.setFrozenBy(operatorId);
            updateObj.setFreezeReason(reqVO.getReason());
            scheduleOrderMapper.updateById(updateObj);

            MesProScheduleOrderDO after = copyForSnapshot(scheduleOrder);
            after.setFrozen(Boolean.TRUE);
            after.setFrozenTime(now);
            after.setFrozenBy(operatorId);
            after.setFreezeReason(reqVO.getReason());
            insertOperationLog(scheduleOrder, after, "FREEZE", reqVO.getReason());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfreezeScheduleOrders(MesProScheduleOrderBatchReqVO reqVO) {
        validateBatchRequest(reqVO);
        List<MesProScheduleOrderDO> scheduleOrders = getRequiredScheduleOrders(reqVO.getIds());
        List<String> unfrozenCodes = scheduleOrders.stream()
                .filter(item -> !Boolean.TRUE.equals(item.getFrozen()))
                .map(this::getScheduleOrderCode)
                .toList();
        if (CollUtil.isNotEmpty(unfrozenCodes)) {
            throw exception0(PRO_SCHEDULE_ORDER_FROZEN.getCode(), "排产工单未冻结，不能解冻: {}", unfrozenCodes);
        }
        for (MesProScheduleOrderDO scheduleOrder : scheduleOrders) {
            scheduleOrderMapper.clearFrozen(scheduleOrder.getId());

            MesProScheduleOrderDO after = copyForSnapshot(scheduleOrder);
            after.setFrozen(Boolean.FALSE);
            after.setFrozenTime(null);
            after.setFrozenBy(null);
            after.setFreezeReason(null);
            insertOperationLog(scheduleOrder, after, "UNFREEZE", reqVO.getReason());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manualFinish(MesProScheduleOrderActionReqVO reqVO) {
        validateOperationReason(reqVO.getReason());
        MesProScheduleOrderDO scheduleOrder = validateWritableScheduleOrder(reqVO.getId());
        if (Boolean.TRUE.equals(scheduleOrder.getManualFinished())) {
            throw exception(PRO_SCHEDULE_ORDER_MANUAL_FINISH_ALREADY);
        }
        if (ObjUtil.equal(scheduleOrder.getStatus(), MesProScheduleOrderStatusEnum.CANCELED.getStatus())
                || ObjUtil.equal(scheduleOrder.getStatus(), MesProScheduleOrderStatusEnum.FINISHED.getStatus())) {
            throw exception(PRO_SCHEDULE_ORDER_MANUAL_FINISH_STATUS_INVALID);
        }
        LocalDateTime now = LocalDateTime.now();
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        BigDecimal scheduleOrderQuantity = resolveScheduleOrderQuantity(scheduleOrder);
        BigDecimal totalQuantity = resolveAggregateTotalQuantity(scheduleOrderQuantity,
                scheduleOrderProcessMapper.selectListByScheduleOrderId(scheduleOrder.getId()));
        MesProScheduleOrderDO updateObj = new MesProScheduleOrderDO();
        updateObj.setId(scheduleOrder.getId());
        updateObj.setManualFinished(Boolean.TRUE);
        updateObj.setManualFinishedTime(now);
        updateObj.setManualFinishedBy(operatorId);
        updateObj.setManualFinishedReason(reqVO.getReason());
        updateObj.setStatus(MesProScheduleOrderStatusEnum.FINISHED.getStatus());
        updateObj.setTotalQuantity(totalQuantity);
        updateObj.setCompletedQuantity(totalQuantity);
        updateObj.setUncompletedQuantity(BigDecimal.ZERO.setScale(6));
        updateObj.setProgressPercent(new BigDecimal("100.000000"));
        scheduleOrderMapper.updateById(updateObj);

        MesProScheduleOrderDO after = copyForSnapshot(scheduleOrder);
        after.setManualFinished(Boolean.TRUE);
        after.setManualFinishedTime(now);
        after.setManualFinishedBy(operatorId);
        after.setManualFinishedReason(reqVO.getReason());
        after.setStatus(MesProScheduleOrderStatusEnum.FINISHED.getStatus());
        after.setTotalQuantity(totalQuantity);
        after.setCompletedQuantity(totalQuantity);
        after.setUncompletedQuantity(BigDecimal.ZERO.setScale(6));
        after.setProgressPercent(new BigDecimal("100.000000"));
        insertOperationLog(scheduleOrder, after, "MANUAL_FINISH", reqVO.getReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeManualFinish(MesProScheduleOrderActionReqVO reqVO) {
        validateOperationReason(reqVO.getReason());
        MesProScheduleOrderDO scheduleOrder = validateWritableScheduleOrder(reqVO.getId());
        if (!Boolean.TRUE.equals(scheduleOrder.getManualFinished())) {
            throw exception(PRO_SCHEDULE_ORDER_MANUAL_FINISH_NOT_ACTIVE);
        }
        RecalculatedProgressSnapshot recalculated = recalculateProgressSnapshot(scheduleOrder.getId(), scheduleOrder, true);
        scheduleOrderMapper.clearManualFinishAndUpdateProgress(scheduleOrder.getId(), recalculated.status(),
                recalculated.summary().totalQuantity(), recalculated.summary().completedQuantity(),
                recalculated.summary().uncompletedQuantity(), recalculated.summary().progressPercent());

        MesProScheduleOrderDO after = copyForSnapshot(scheduleOrder);
        after.setManualFinished(Boolean.FALSE);
        after.setManualFinishedTime(null);
        after.setManualFinishedBy(null);
        after.setManualFinishedReason(null);
        after.setStatus(recalculated.status());
        after.setTotalQuantity(recalculated.summary().totalQuantity());
        after.setCompletedQuantity(recalculated.summary().completedQuantity());
        after.setUncompletedQuantity(recalculated.summary().uncompletedQuantity());
        after.setProgressPercent(recalculated.summary().progressPercent());
        insertOperationLog(scheduleOrder, after, "REVOKE_MANUAL_FINISH", reqVO.getReason());
        insertOperationLog(scheduleOrder, recalculated.auditPayload(), "SYNC_PROGRESS", "同步报工进度");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteScheduleOrders(MesProScheduleOrderBatchReqVO reqVO) {
        validateBatchRequest(reqVO);
        List<MesProScheduleOrderDO> scheduleOrders = getRequiredScheduleOrders(reqVO.getIds());
        List<String> frozenCodes = scheduleOrders.stream()
                .filter(item -> Boolean.TRUE.equals(item.getFrozen()))
                .map(this::getScheduleOrderCode)
                .toList();
        if (CollUtil.isNotEmpty(frozenCodes)) {
            throw exception0(PRO_SCHEDULE_ORDER_FROZEN.getCode(), "排产工单已冻结，不能删除: {}", frozenCodes);
        }
        List<MesProScheduleOrderProcessDO> processes = scheduleOrderProcessMapper.selectListByScheduleOrderIds(reqVO.getIds());
        Set<Long> reportedScheduleOrderIds = processes.stream()
                .filter(process -> normalizeQuantity(process.getReportedQuantity()).compareTo(BigDecimal.ZERO) > 0)
                .map(MesProScheduleOrderProcessDO::getScheduleOrderId)
                .collect(Collectors.toSet());
        List<String> blockedCodes = scheduleOrders.stream()
                .filter(item -> ObjUtil.equal(item.getStatus(), MesProScheduleOrderStatusEnum.FINISHED.getStatus())
                        || reportedScheduleOrderIds.contains(item.getId()))
                .map(this::getScheduleOrderCode)
                .toList();
        if (CollUtil.isNotEmpty(blockedCodes)) {
            throw exception0(PRO_SCHEDULE_ORDER_DELETE_BLOCKED.getCode(),
                    "排产工单存在已报工或已完成记录，不能删除: {}", blockedCodes);
        }
        for (MesProScheduleOrderDO scheduleOrder : scheduleOrders) {
            MesProScheduleOrderDO after = copyForSnapshot(scheduleOrder);
            after.setDeleted(Boolean.TRUE);
            insertOperationLog(scheduleOrder, after, "DELETE", reqVO.getReason());
            scheduleOrderMapper.deleteById(scheduleOrder.getId());
        }
    }

    private Long createMissingRouteScheduleOrder(MesProScheduleOrderCreateFromWorkOrderReqVO reqVO,
                                                 MesProWorkOrderDO workOrder) {
        MesProScheduleOrderDO scheduleOrder = buildMissingRouteScheduleOrder(reqVO, workOrder);
        scheduleOrderMapper.insert(scheduleOrder);
        return scheduleOrder.getId();
    }

    @Override
    public MesProScheduleOrderDO getScheduleOrder(Long id) {
        return scheduleOrderMapper.selectById(id);
    }

    @Override
    public PageResult<MesProScheduleOrderDO> getScheduleOrderPage(MesProScheduleOrderPageReqVO pageReqVO) {
        normalizeCurrentProcessFilter(pageReqVO);
        if (pageReqVO.getCurrentProcessId() != null) {
            pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        }
        List<Long> productIds = resolveScheduleQuickFilterProductIds(pageReqVO.getQuickFilter());
        QuickFilter originalQuickFilter = pageReqVO.getQuickFilter();
        if (isScheduleProductQuickFilter(originalQuickFilter)) {
            pageReqVO.setQuickFilter(null);
        }
        PageResult<MesProScheduleOrderDO> pageResult;
        try {
            pageResult = scheduleOrderMapper.selectPageByProductIds(pageReqVO, productIds);
        } finally {
            pageReqVO.setQuickFilter(originalQuickFilter);
        }
        if (pageReqVO.getCurrentProcessId() == null || CollUtil.isEmpty(pageResult.getList())) {
            return pageResult;
        }
        List<MesProScheduleOrderDO> filteredList = filterByCurrentProcess(pageResult.getList(), pageReqVO.getCurrentProcessId());
        return new PageResult<>(filteredList, (long) filteredList.size());
    }

    private void normalizeCurrentProcessFilter(MesProScheduleOrderPageReqVO pageReqVO) {
        if (pageReqVO.getCurrentProcessId() != null && pageReqVO.getCurrentProcessId() <= 0) {
            pageReqVO.setCurrentProcessId(null);
        }
    }

    private List<Long> resolveScheduleQuickFilterProductIds(QuickFilter quickFilter) {
        if (quickFilter == null || quickFilter.getValue() == null || quickFilter.getValue().trim().isEmpty()) {
            return Collections.emptyList();
        }
        String value = quickFilter.getValue().trim();
        return switch (StrUtil.blankToDefault(quickFilter.getFieldKey(), "")) {
            case "productName" -> toProductIds(itemMapper.selectListByNameLike(value));
            case "productSpecification" -> toProductIds(itemMapper.selectListBySpecificationLike(value));
            default -> Collections.emptyList();
        };
    }

    private boolean isScheduleProductQuickFilter(QuickFilter quickFilter) {
        if (quickFilter == null) {
            return false;
        }
        return switch (StrUtil.blankToDefault(quickFilter.getFieldKey(), "")) {
            case "productName", "productSpecification" -> true;
            default -> false;
        };
    }

    private List<Long> toProductIds(List<MesMdItemDO> matchedProducts) {
        if (CollUtil.isEmpty(matchedProducts)) {
            return List.of(-1L);
        }
        return matchedProducts.stream()
                .map(MesMdItemDO::getId)
                .toList();
    }

    private List<MesProScheduleOrderDO> filterByCurrentProcess(List<MesProScheduleOrderDO> scheduleOrders,
                                                               Long currentProcessId) {
        Set<Long> scheduleOrderIds = scheduleOrders.stream()
                .map(MesProScheduleOrderDO::getId)
                .collect(Collectors.toSet());
        Map<Long, List<MesProScheduleOrderProcessDO>> processMap = scheduleOrderProcessMapper
                .selectListByScheduleOrderIds(scheduleOrderIds)
                .stream()
                .collect(Collectors.groupingBy(MesProScheduleOrderProcessDO::getScheduleOrderId));
        return scheduleOrders.stream()
                .filter(order -> hasWorkbenchWipProcess(processMap.getOrDefault(order.getId(), Collections.emptyList()),
                        order, currentProcessId))
                .toList();
    }

    private boolean hasWorkbenchWipProcess(List<MesProScheduleOrderProcessDO> processes,
                                           MesProScheduleOrderDO scheduleOrder,
                                           Long currentProcessId) {
        return processes.stream()
                .filter(this::isProcessWip)
                .filter(process -> hasResolvableWorkbenchWipIdentity(scheduleOrder, process))
                .map(process -> routeProcessService.resolveFrozenRouteProcess(
                        process.getRouteProcessId(), scheduleOrder.getRouteId(), process.getProcessId()).getProcessId())
                .anyMatch(currentProcessId::equals);
    }

    private boolean hasResolvableWorkbenchWipIdentity(MesProScheduleOrderDO scheduleOrder,
                                                       MesProScheduleOrderProcessDO process) {
        return scheduleOrder != null
                && scheduleOrder.getRouteId() != null
                && process != null
                && process.getRouteProcessId() != null;
    }

    @Override
    public MesProScheduleOrderAdmissionDiffPageRespVO getAdmissionDiff(
            MesProScheduleOrderAdmissionDiffPageReqVO pageReqVO) {
        MesProWorkOrderPageReqVO workOrderPageReqVO = buildWorkOrderPageReqVO(pageReqVO);
        boolean computedFilter = hasComputedAdmissionFilter(pageReqVO);
        if (StrUtil.isNotBlank(pageReqVO.getProductCode())) {
            MesMdItemDO item = itemMapper.selectByCode(pageReqVO.getProductCode());
            if (item == null) {
                return emptyAdmissionDiffResult();
            }
            workOrderPageReqVO.setProductId(item.getId());
        }
        if (computedFilter) {
            return getAdmissionDiffWithComputedFilter(pageReqVO, workOrderPageReqVO);
        }

        PageResult<MesProWorkOrderDO> workOrderPage = workOrderMapper.selectPage(workOrderPageReqVO);
        List<MesProWorkOrderDO> workOrders = workOrderPage.getList();
        if (CollUtil.isEmpty(workOrders)) {
            return emptyAdmissionDiffResult();
        }
        List<Long> workOrderIds = workOrders.stream().map(MesProWorkOrderDO::getId).toList();
        Map<Long, MesProScheduleOrderDO> scheduleOrderMap = scheduleOrderMapper
                .selectListByWorkOrderIds(workOrderIds)
                .stream()
                .collect(Collectors.toMap(MesProScheduleOrderDO::getWorkOrderId, item -> item,
                        (left, right) -> left, LinkedHashMap::new));

        List<MesProScheduleOrderAdmissionDiffRespVO> rows = workOrders.stream()
                .map(workOrder -> buildAdmissionDiffRow(workOrder, scheduleOrderMap.get(workOrder.getId())))
                .filter(row -> matchesAdmissionDiffFilter(row, pageReqVO))
                .toList();
        List<MesProScheduleOrderAdmissionDiffRespVO> pageRows = computedFilter
                ? paginateAdmissionDiffRows(rows, pageReqVO)
                : rows;
        MesProScheduleOrderAdmissionDiffPageRespVO result = new MesProScheduleOrderAdmissionDiffPageRespVO();
        result.setList(pageRows);
        result.setTotal(computedFilter ? (long) rows.size() : workOrderPage.getTotal());
        result.setSummary(buildAdmissionDiffSummary(computedFilter ? rows : pageRows));
        return result;
    }

    private MesProScheduleOrderAdmissionDiffPageRespVO getAdmissionDiffWithComputedFilter(
            MesProScheduleOrderAdmissionDiffPageReqVO pageReqVO, MesProWorkOrderPageReqVO workOrderPageReqVO) {
        int pageNo = Math.max(Objects.requireNonNullElse(pageReqVO.getPageNo(), 1), 1);
        int pageSize = Objects.requireNonNullElse(pageReqVO.getPageSize(), 10);
        int targetOffset = PageParam.PAGE_SIZE_NONE.equals(pageReqVO.getPageSize()) ? 0 : (pageNo - 1) * pageSize;
        int targetLimit = PageParam.PAGE_SIZE_NONE.equals(pageReqVO.getPageSize()) ? Integer.MAX_VALUE : pageSize;
        int scanPageNo = 1;
        int scanPageSize = Math.max(pageSize, 50);
        long matchedTotal = 0L;
        List<MesProScheduleOrderAdmissionDiffRespVO> pageRows = new ArrayList<>();
        MesProScheduleOrderAdmissionDiffSummaryRespVO summary = new MesProScheduleOrderAdmissionDiffSummaryRespVO();
        Map<Long, AdmissionRouteCheck> routeCheckCache = new HashMap<>();

        while (true) {
            workOrderPageReqVO.setPageNo(scanPageNo);
            workOrderPageReqVO.setPageSize(scanPageSize);
            PageResult<MesProWorkOrderDO> workOrderPage = workOrderMapper.selectPage(workOrderPageReqVO);
            List<MesProWorkOrderDO> workOrders = workOrderPage.getList();
            if (CollUtil.isEmpty(workOrders)) {
                break;
            }
            List<Long> workOrderIds = workOrders.stream().map(MesProWorkOrderDO::getId).toList();
            Map<Long, MesProScheduleOrderDO> scheduleOrderMap = scheduleOrderMapper
                    .selectListByWorkOrderIds(workOrderIds)
                    .stream()
                    .collect(Collectors.toMap(MesProScheduleOrderDO::getWorkOrderId, item -> item,
                            (left, right) -> left, LinkedHashMap::new));
            for (MesProWorkOrderDO workOrder : workOrders) {
                MesProScheduleOrderAdmissionDiffRespVO row = buildAdmissionDiffRow(workOrder,
                        scheduleOrderMap.get(workOrder.getId()), routeCheckCache);
                if (!matchesAdmissionDiffFilter(row, pageReqVO)) {
                    continue;
                }
                appendAdmissionSummary(summary, row);
                if (matchedTotal >= targetOffset && pageRows.size() < targetLimit) {
                    pageRows.add(row);
                }
                matchedTotal += 1;
            }
            if (workOrderPage.getTotal() <= (long) scanPageNo * scanPageSize) {
                break;
            }
            scanPageNo += 1;
        }

        MesProScheduleOrderAdmissionDiffPageRespVO result = new MesProScheduleOrderAdmissionDiffPageRespVO();
        result.setList(pageRows);
        result.setTotal(matchedTotal);
        result.setSummary(summary);
        return result;
    }

    @Override
    public MesProScheduleOrderPreflightRespVO preflight(MesProScheduleOrderPreflightReqVO reqVO) {
        if (CollUtil.isEmpty(reqVO.getScheduleOrderIds())) {
            throw new IllegalArgumentException("排产工单编号不能为空");
        }
        List<Long> scheduleOrderIds = reqVO.getScheduleOrderIds();
        List<MesProScheduleOrderDO> scheduleOrders = scheduleOrderMapper.selectListByIds(scheduleOrderIds);
        Map<Long, MesProScheduleOrderDO> scheduleOrderMap = scheduleOrders.stream()
                .collect(Collectors.toMap(MesProScheduleOrderDO::getId, item -> item,
                        (left, right) -> left, LinkedHashMap::new));
        Map<Long, MesMdItemDO> productMap = buildProductMap(scheduleOrders);
        List<MesProScheduleOrderProcessDO> processes =
                scheduleOrderProcessMapper.selectListByScheduleOrderIds(scheduleOrderIds);
        Map<Long, List<MesProScheduleOrderProcessDO>> processesByOrderId = processes.stream()
                .collect(Collectors.groupingBy(MesProScheduleOrderProcessDO::getScheduleOrderId,
                        LinkedHashMap::new, Collectors.toList()));

        List<MesProScheduleOrderPreflightIssueRespVO> issues = new ArrayList<>();
        MesProScheduleOrderPreflightSummaryRespVO summary = new MesProScheduleOrderPreflightSummaryRespVO();
        for (Long scheduleOrderId : scheduleOrderIds) {
            MesProScheduleOrderDO scheduleOrder = scheduleOrderMap.get(scheduleOrderId);
            if (scheduleOrder == null) {
                issues.add(buildScheduleOrderNotFoundIssue(scheduleOrderId));
                summary.setBlockedCount(summary.getBlockedCount() + 1);
                continue;
            }
            int beforeIssueCount = issues.size();
            appendPreflightIssues(scheduleOrder,
                    processesByOrderId.getOrDefault(scheduleOrderId, Collections.emptyList()), issues);
            List<MesProScheduleOrderPreflightIssueRespVO> orderIssues = issues.subList(beforeIssueCount, issues.size());
            if (orderIssues.stream().anyMatch(issue -> PREFLIGHT_BLOCKED.equals(issue.getSeverity()))) {
                summary.setBlockedCount(summary.getBlockedCount() + 1);
            } else if (orderIssues.stream().anyMatch(issue -> PREFLIGHT_WARN.equals(issue.getSeverity()))) {
                summary.setWarnCount(summary.getWarnCount() + 1);
            } else {
                summary.setPassCount(summary.getPassCount() + 1);
            }
        }
        enrichPreflightIssueProductIdentity(issues, productMap);

        MesProScheduleOrderPreflightRespVO result = new MesProScheduleOrderPreflightRespVO();
        result.setCheckedAt(LocalDateTime.now());
        result.setIssues(issues);
        result.setSummary(summary);
        result.setResult(summary.getBlockedCount() > 0 ? PREFLIGHT_BLOCKED
                : summary.getWarnCount() > 0 ? PREFLIGHT_WARN : PREFLIGHT_PASS);
        MesProScheduleOrderPreflightScopeRespVO scope = new MesProScheduleOrderPreflightScopeRespVO();
        scope.setScopeType(StrUtil.blankToDefault(reqVO.getScopeType(), "SELECTED"));
        scope.setScheduleOrderCount(scheduleOrderIds.size());
        result.setScope(scope);
        return result;
    }

    private Map<Long, MesMdItemDO> buildProductMap(List<MesProScheduleOrderDO> scheduleOrders) {
        List<Long> productIds = scheduleOrders.stream()
                .map(MesProScheduleOrderDO::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (CollUtil.isEmpty(productIds)) {
            return Collections.emptyMap();
        }
        return itemMapper.selectListByIds(productIds).stream()
                .collect(Collectors.toMap(MesMdItemDO::getId, item -> item, (left, right) -> left,
                        LinkedHashMap::new));
    }

    private void enrichPreflightIssueProductIdentity(List<MesProScheduleOrderPreflightIssueRespVO> issues,
                                                     Map<Long, MesMdItemDO> productMap) {
        for (MesProScheduleOrderPreflightIssueRespVO issue : issues) {
            if (issue.getProductId() == null) {
                continue;
            }
            MesMdItemDO product = productMap.get(issue.getProductId());
            if (product != null) {
                issue.setProductCode(product.getCode());
                issue.setProductName(product.getName());
            }
            if ("BLOCKED_MISSING_ROUTE".equals(issue.getReasonCode())) {
                issue.setMessage(buildMissingRouteMessage(issue));
            }
        }
    }

    private String buildMissingRouteMessage(MesProScheduleOrderPreflightIssueRespVO issue) {
        String productIdentity = buildProductIdentity(issue);
        String scheduleOrderIdentity = StrUtil.blankToDefault(issue.getScheduleOrderCode(),
                issue.getScheduleOrderId() == null ? "未知排产工单" : String.valueOf(issue.getScheduleOrderId()));
        return productIdentity + " 缺少可用工艺路线，排产工单 " + scheduleOrderIdentity + " 不能进入排产应用";
    }

    private String buildProductIdentity(MesProScheduleOrderPreflightIssueRespVO issue) {
        if (StrUtil.isNotBlank(issue.getProductName()) && StrUtil.isNotBlank(issue.getProductCode())) {
            return "产品 " + issue.getProductName() + "（" + issue.getProductCode() + "）";
        }
        if (StrUtil.isNotBlank(issue.getProductName())) {
            return "产品 " + issue.getProductName();
        }
        if (StrUtil.isNotBlank(issue.getProductCode())) {
            return "产品 " + issue.getProductCode();
        }
        return issue.getProductId() == null ? "产品" : "产品ID " + issue.getProductId();
    }

    private MesProWorkOrderPageReqVO buildWorkOrderPageReqVO(MesProScheduleOrderAdmissionDiffPageReqVO pageReqVO) {
        MesProWorkOrderPageReqVO workOrderPageReqVO = new MesProWorkOrderPageReqVO();
        workOrderPageReqVO.setPageNo(pageReqVO.getPageNo());
        workOrderPageReqVO.setPageSize(pageReqVO.getPageSize());
        workOrderPageReqVO.setCode(pageReqVO.getWorkOrderCode());
        workOrderPageReqVO.setStatus(pageReqVO.getStatus() == null
                ? MesProWorkOrderStatusEnum.CONFIRMED.getStatus() : pageReqVO.getStatus());
        workOrderPageReqVO.setRequestDate(pageReqVO.getRequestDate());
        return workOrderPageReqVO;
    }

    private MesProScheduleOrderAdmissionDiffPageRespVO emptyAdmissionDiffResult() {
        MesProScheduleOrderAdmissionDiffPageRespVO result = new MesProScheduleOrderAdmissionDiffPageRespVO();
        result.setList(Collections.emptyList());
        result.setTotal(0L);
        result.setSummary(new MesProScheduleOrderAdmissionDiffSummaryRespVO());
        return result;
    }

    private MesProScheduleOrderAdmissionDiffRespVO buildAdmissionDiffRow(MesProWorkOrderDO workOrder,
                                                                          MesProScheduleOrderDO scheduleOrder) {
        return buildAdmissionDiffRow(workOrder, scheduleOrder, null);
    }

    private MesProScheduleOrderAdmissionDiffRespVO buildAdmissionDiffRow(MesProWorkOrderDO workOrder,
                                                                         MesProScheduleOrderDO scheduleOrder,
                                                                         Map<Long, AdmissionRouteCheck> routeCheckCache) {
        MesProScheduleOrderAdmissionDiffRespVO row = new MesProScheduleOrderAdmissionDiffRespVO();
        row.setWorkOrderId(workOrder.getId());
        row.setWorkOrderCode(workOrder.getCode());
        row.setProductId(workOrder.getProductId());
        row.setQuantity(workOrder.getQuantity());
        row.setRequestDate(workOrder.getRequestDate());
        row.setWorkOrderStatus(workOrder.getStatus());
        row.setTemporaryFrozen(workOrder.getTemporaryFrozen());
        row.setActions(Collections.emptyList());
        if (Boolean.TRUE.equals(workOrder.getTemporaryFrozen())) {
            applyAdmissionIssue(row, ADMISSION_BLOCKED, PREFLIGHT_BLOCKED, "BLOCKED_WORK_ORDER_FROZEN",
                    "生产工单已临时冻结，不能加入排产工单池", "生产计划", null);
            return row;
        }
        if (!ObjUtil.equal(workOrder.getStatus(), MesProWorkOrderStatusEnum.CONFIRMED.getStatus())) {
            applyAdmissionIssue(row, ADMISSION_BLOCKED, PREFLIGHT_BLOCKED, "BLOCKED_WORK_ORDER_STATUS",
                    "生产工单不是已确认状态，不能加入排产工单池", "生产计划", null);
            return row;
        }
        if (scheduleOrder != null) {
            row.setScheduleOrderId(scheduleOrder.getId());
            applyAdmissionIssue(row, ADMISSION_ALREADY, PREFLIGHT_PASS, "ALREADY_ADMITTED",
                    "生产工单已在排产工单池中", "排产员", null);
            return row;
        }
        AdmissionRouteCheck routeCheck = resolveAdmissionRouteCheck(workOrder, routeCheckCache);
        if (!routeCheck.routeReady()) {
            if (routeCheck.warnOnly()) {
                applyAdmissionWarning(row, routeCheck.reasonCode(), routeCheck.message(),
                        routeCheck.ownerRole(), routeCheck.action());
                return row;
            }
            applyAdmissionIssue(row, ADMISSION_BLOCKED, PREFLIGHT_BLOCKED, routeCheck.reasonCode(),
                    routeCheck.message(), routeCheck.ownerRole(), routeCheck.action());
            return row;
        }
        row.setAdmissionStatus(ADMISSION_READY);
        row.setSchedulableStatus(PREFLIGHT_PASS);
        row.setReasonCode("READY_TO_ADMIT");
        row.setSeverity(PREFLIGHT_PASS);
        row.setMessage("生产工单可加入排产工单池");
        row.setOwnerRole("排产员");
        row.setSelectable(Boolean.TRUE);
        return row;
    }

    private AdmissionRouteCheck resolveAdmissionRouteCheck(MesProWorkOrderDO workOrder,
                                                           Map<Long, AdmissionRouteCheck> routeCheckCache) {
        if (routeCheckCache == null || workOrder.getProductId() == null) {
            return checkWorkOrderRoute(workOrder);
        }
        return routeCheckCache.computeIfAbsent(workOrder.getProductId(),
                ignored -> checkWorkOrderRoute(workOrder));
    }

    private AdmissionRouteCheck checkWorkOrderRoute(MesProWorkOrderDO workOrder) {
        List<MesProRouteProductDO> routeProducts = routeProductMapper.selectListByItemId(workOrder.getProductId());
        if (CollUtil.isEmpty(routeProducts)) {
            return AdmissionRouteCheck.blocked("BLOCKED_MISSING_ROUTE", "产品未绑定启用工艺流程排产配置",
                    "工艺维护", routeAction(workOrder.getProductId(), null, "维护路线"));
        }
        if (routeProducts.size() > 1) {
            return AdmissionRouteCheck.blocked("BLOCKED_ROUTE_PRODUCT_AMBIGUOUS",
                    "产品绑定了多条工艺流程排产配置，请保留唯一启用路线后再入池", "工艺维护",
                    routeAction(workOrder.getProductId(), null, "维护路线"));
        }
        MesProRouteProductDO routeProduct = routeProducts.get(0);
        MesProRouteDO route = routeMapper.selectById(routeProduct.getRouteId());
        if (route == null || ObjUtil.notEqual(route.getStatus(), CommonStatusEnum.ENABLE.getStatus())) {
            return AdmissionRouteCheck.blocked("BLOCKED_ROUTE_DISABLED", "产品绑定的工艺路线不存在或未启用",
                    "工艺维护", routeAction(workOrder.getProductId(), routeProduct.getRouteId(), "维护路线"));
        }
        MesProRouteVersionDO activeRouteVersion = routeVersionMapper.selectActiveByRouteId(route.getId());
        if (activeRouteVersion == null) {
            return AdmissionRouteCheck.blocked("BLOCKED_ROUTE_VERSION_MISSING", "工艺路线缺少激活版本",
                    "工艺维护", routeAction(workOrder.getProductId(), route.getId(), "维护路线版本"));
        }
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteId(route.getId());
        if (CollUtil.isEmpty(routeProcesses)) {
            return AdmissionRouteCheck.blocked("BLOCKED_ROUTE_PROCESS_MISSING", "工艺路线缺少工序",
                    "工艺维护", routeAction(workOrder.getProductId(), route.getId(), "维护路线工序"));
        }
        Map<Long, MesProRouteFlowProcessConfigDO> scheduleRouteFlowConfigMap = resolveScheduleRouteFlowConfigMap(route.getId());
        for (MesProRouteProcessDO routeProcess : routeProcesses) {
            MesProRouteFlowProcessConfigDO scheduleUseConfig = scheduleRouteFlowConfigMap.get(routeProcess.getId());
            if (scheduleUseConfig == null) {
                return AdmissionRouteCheck.blocked("BLOCKED_ROUTE_PROCESS_SCHEDULE_USE_MISSING",
                        "工序缺少智能排产流程配置", "工艺维护",
                        routeAction(workOrder.getProductId(), route.getId(), "维护智能排产用途"));
            }
            if (!Boolean.TRUE.equals(scheduleUseConfig.getEnabled())) {
                return AdmissionRouteCheck.blocked("BLOCKED_ROUTE_PROCESS_DISABLED_FOR_SCHEDULE",
                        "工序已在智能排产用途关闭，不能加入排产工单池", "工艺维护",
                        routeAction(workOrder.getProductId(), route.getId(), "维护智能排产用途"));
            }
        }
        Map<Long, MesProRouteScheduleConfigDO> scheduleConfigMap =
                buildScheduleConfigMap(activeRouteVersion);
        for (MesProRouteProcessDO routeProcess : routeProcesses) {
            MesProRouteScheduleConfigDO scheduleConfig = scheduleConfigMap.get(routeProcess.getId());
            if (scheduleConfig == null) {
                return AdmissionRouteCheck.blocked("BLOCKED_ROUTE_SCHEDULE_CONFIG_MISSING",
                        "工序缺少排产策略配置", "工艺维护",
                        routeConfigAction(route.getId(), routeProcess.getId(), "维护排产策略"));
            }
            if (isDefaultScheduleConfig(scheduleConfig)) {
                return AdmissionRouteCheck.warn(WARN_DEFAULT_ROUTE_SCHEDULE_CONFIG,
                        "工序排产策略仍为系统默认值，允许入池但建议尽快维护正式策略", "工艺维护",
                        routeConfigAction(route.getId(), routeProcess.getId(), "维护排产策略"));
            }
        }
        ResourceSnapshotContext resourceContext = buildResourceSnapshotContext(routeProcesses);
        AdmissionRouteCheck resourceCheck = checkResourceCalculatedCapacity(route, routeProcesses,
                scheduleConfigMap, resourceContext);
        if (resourceCheck != null) {
            return resourceCheck;
        }
        return AdmissionRouteCheck.ready();
    }

    private AdmissionRouteCheck checkResourceCalculatedCapacity(MesProRouteDO route,
                                                               List<MesProRouteProcessDO> routeProcesses,
                                                               Map<Long, MesProRouteScheduleConfigDO> scheduleConfigMap,
                                                               ResourceSnapshotContext resourceContext) {
        for (MesProRouteProcessDO routeProcess : routeProcesses) {
            MesProRouteScheduleConfigDO scheduleConfig = scheduleConfigMap.get(routeProcess.getId());
            if (!isResourceCalculated(scheduleConfig)) {
                continue;
            }
            ResourceSnapshot snapshot = resourceContext.snapshotByRouteProcessId.get(routeProcess.getId());
            if (!hasPositiveResourceCapacity(snapshot)) {
                return AdmissionRouteCheck.blocked("BLOCKED_RESOURCE_CAPACITY_MISSING",
                        "资源计算工序缺少工作站资源产能，不能加入排产工单池", "工艺维护",
                        routeConfigAction(route.getId(), routeProcess.getId(), "维护资源产能"));
            }
        }
        return null;
    }

    private void applyAdmissionIssue(MesProScheduleOrderAdmissionDiffRespVO row, String admissionStatus,
                                     String severity, String reasonCode, String message, String ownerRole,
                                     MesProScheduleOrderIssueActionRespVO action) {
        row.setAdmissionStatus(admissionStatus);
        row.setSchedulableStatus(severity);
        row.setReasonCode(reasonCode);
        row.setSeverity(severity);
        row.setMessage(message);
        row.setOwnerRole(ownerRole);
        row.setSelectable(Boolean.FALSE);
        row.setActions(action == null ? Collections.emptyList() : List.of(action));
    }

    private void applyAdmissionWarning(MesProScheduleOrderAdmissionDiffRespVO row, String reasonCode, String message,
                                       String ownerRole, MesProScheduleOrderIssueActionRespVO action) {
        row.setAdmissionStatus(ADMISSION_READY);
        row.setSchedulableStatus(PREFLIGHT_WARN);
        row.setReasonCode(reasonCode);
        row.setSeverity(PREFLIGHT_WARN);
        row.setMessage(message);
        row.setOwnerRole(ownerRole);
        row.setSelectable(Boolean.TRUE);
        row.setActions(action == null ? Collections.emptyList() : List.of(action));
    }

    private boolean matchesAdmissionDiffFilter(MesProScheduleOrderAdmissionDiffRespVO row,
                                               MesProScheduleOrderAdmissionDiffPageReqVO pageReqVO) {
        if (StrUtil.isNotBlank(pageReqVO.getAdmissionStatus())
                && !pageReqVO.getAdmissionStatus().equals(row.getAdmissionStatus())) {
            return false;
        }
        return StrUtil.isBlank(pageReqVO.getReasonCode()) || pageReqVO.getReasonCode().equals(row.getReasonCode());
    }

    private boolean hasComputedAdmissionFilter(MesProScheduleOrderAdmissionDiffPageReqVO pageReqVO) {
        return StrUtil.isNotBlank(pageReqVO.getAdmissionStatus()) || StrUtil.isNotBlank(pageReqVO.getReasonCode());
    }

    private List<MesProScheduleOrderAdmissionDiffRespVO> paginateAdmissionDiffRows(
            List<MesProScheduleOrderAdmissionDiffRespVO> rows, MesProScheduleOrderAdmissionDiffPageReqVO pageReqVO) {
        if (PageParam.PAGE_SIZE_NONE.equals(pageReqVO.getPageSize())) {
            return rows;
        }
        int pageNo = Math.max(Objects.requireNonNullElse(pageReqVO.getPageNo(), 1), 1);
        int pageSize = Objects.requireNonNullElse(pageReqVO.getPageSize(), 10);
        int fromIndex = Math.min((pageNo - 1) * pageSize, rows.size());
        int toIndex = Math.min(fromIndex + pageSize, rows.size());
        return rows.subList(fromIndex, toIndex);
    }

    private MesProScheduleOrderAdmissionDiffSummaryRespVO buildAdmissionDiffSummary(
            List<MesProScheduleOrderAdmissionDiffRespVO> rows) {
        MesProScheduleOrderAdmissionDiffSummaryRespVO summary = new MesProScheduleOrderAdmissionDiffSummaryRespVO();
        for (MesProScheduleOrderAdmissionDiffRespVO row : rows) {
            appendAdmissionSummary(summary, row);
        }
        return summary;
    }

    private void appendAdmissionSummary(MesProScheduleOrderAdmissionDiffSummaryRespVO summary,
                                        MesProScheduleOrderAdmissionDiffRespVO row) {
        if (PREFLIGHT_WARN.equals(row.getSeverity())) {
            summary.setWarnCount(summary.getWarnCount() + 1);
        } else if (ADMISSION_READY.equals(row.getAdmissionStatus())) {
            summary.setReadyCount(summary.getReadyCount() + 1);
        } else if (ADMISSION_ALREADY.equals(row.getAdmissionStatus())) {
            summary.setAlreadyAdmittedCount(summary.getAlreadyAdmittedCount() + 1);
        } else if (PREFLIGHT_BLOCKED.equals(row.getSeverity())) {
            summary.setBlockedCount(summary.getBlockedCount() + 1);
        }
    }

    private void appendPreflightIssues(MesProScheduleOrderDO scheduleOrder,
                                       List<MesProScheduleOrderProcessDO> processes,
                                       List<MesProScheduleOrderPreflightIssueRespVO> issues) {
        if (!Boolean.TRUE.equals(scheduleOrder.getAutoSchedulable())
                || ObjUtil.notEqual(scheduleOrder.getRouteStatus(), MesProScheduleOrderRouteStatusEnum.READY.getStatus())
                || scheduleOrder.getRouteId() == null) {
            issues.add(buildPreflightIssue(scheduleOrder, null, "BLOCKED_MISSING_ROUTE", PREFLIGHT_BLOCKED,
                    "SCHEDULE_ORDER", scheduleOrder.getId(), "排产工单缺少可用工艺路线，不能进入排产应用",
                    "工艺维护", routeAction(scheduleOrder.getProductId(), scheduleOrder.getRouteId(), "维护路线")));
            appendErpSyncWarning(scheduleOrder, issues);
            return;
        }
        List<MesProScheduleOrderProcessDO> enabledProcesses = processes.stream()
                .filter(process -> Boolean.TRUE.equals(process.getEnabled()))
                .toList();
        for (MesProScheduleOrderProcessDO disabledProcess : processes.stream()
                .filter(process -> !Boolean.TRUE.equals(process.getEnabled()))
                .toList()) {
            issues.add(buildPreflightIssue(scheduleOrder, disabledProcess,
                    "BLOCKED_ROUTE_PROCESS_DISABLED_FOR_SCHEDULE", PREFLIGHT_BLOCKED,
                    "ROUTE_FLOW_CONFIG", scheduleOrder.getRouteId(),
                    "工序已在智能排产用途关闭，不能进入自动排产或手动重排",
                    "工艺维护", routeAction(scheduleOrder.getProductId(), scheduleOrder.getRouteId(), "维护智能排产用途")));
        }
        if (CollUtil.isEmpty(enabledProcesses)) {
            issues.add(buildPreflightIssue(scheduleOrder, null, "BLOCKED_ROUTE_PROCESS_MISSING", PREFLIGHT_BLOCKED,
                    "SCHEDULE_ORDER", scheduleOrder.getId(), "排产工单没有启用的工序快照，不能进入排产应用",
                    "工艺维护", routeAction(scheduleOrder.getProductId(), scheduleOrder.getRouteId(), "维护路线工序")));
        }
        for (MesProScheduleOrderProcessDO process : enabledProcesses) {
            appendProcessPreflightIssues(scheduleOrder, process, issues);
        }
        appendErpSyncWarning(scheduleOrder, issues);
    }

    private void appendProcessPreflightIssues(MesProScheduleOrderDO scheduleOrder,
                                              MesProScheduleOrderProcessDO process,
                                              List<MesProScheduleOrderPreflightIssueRespVO> issues) {
        if (MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode().equals(process.getCapacityMode())) {
            if (process.getInfiniteDurationQuantityFactor() == null
                    || process.getInfiniteDurationQuantityFactor().compareTo(BigDecimal.ZERO) <= 0
                    || process.getInfiniteDurationBaseMinutes() == null
                    || process.getInfiniteDurationBaseMinutes().compareTo(BigDecimal.ZERO) < 0) {
                issues.add(buildPreflightIssue(scheduleOrder, process, "BLOCKED_INVALID_INFINITE_FORMULA",
                        PREFLIGHT_BLOCKED, "ROUTE_SCHEDULE_CONFIG", process.getRouteScheduleConfigId(),
                        "无限产能公式缺少合法的数量系数或基础分钟",
                        "工艺维护", routeConfigAction(scheduleOrder, process, "维护产能公式")));
            }
        } else if (MesProScheduleCapacityModeEnum.isManualOverrideLike(process.getCapacityMode())
                && (process.getHourlyCapacityTotal() == null
                || process.getHourlyCapacityTotal().compareTo(BigDecimal.ZERO) <= 0)) {
            issues.add(buildPreflightIssue(scheduleOrder, process, "BLOCKED_INVALID_FINITE_CAPACITY",
                    PREFLIGHT_BLOCKED, "ROUTE_SCHEDULE_CONFIG", process.getRouteScheduleConfigId(),
                    "有限产能工序缺少大于 0 的小时产能",
                    "工艺维护", routeConfigAction(scheduleOrder, process, "维护产能")));
        } else if (MesProScheduleCapacityModeEnum.RESOURCE_CALCULATED.getMode().equals(process.getCapacityMode())
                && (process.getHourlyCapacityTotal() == null
                || process.getHourlyCapacityTotal().compareTo(BigDecimal.ZERO) <= 0)) {
            issues.add(buildPreflightIssue(scheduleOrder, process, "BLOCKED_RESOURCE_CAPACITY_MISSING",
                    PREFLIGHT_BLOCKED, "ROUTE_SCHEDULE_CONFIG", process.getRouteScheduleConfigId(),
                    "资源计算工序缺少工作站资源产能",
                    "工艺维护", routeConfigAction(scheduleOrder, process, "维护资源产能")));
        }
        if (isDefaultScheduleProcess(process)) {
            issues.add(buildPreflightIssue(scheduleOrder, process, WARN_DEFAULT_ROUTE_SCHEDULE_CONFIG,
                    PREFLIGHT_WARN, "ROUTE_SCHEDULE_CONFIG", process.getRouteScheduleConfigId(),
                    "工序排产策略仍为系统默认值，允许排产但建议尽快维护正式策略",
                    "工艺维护", routeConfigAction(scheduleOrder, process, "维护排产策略")));
        }
        if (Boolean.TRUE.equals(process.getNightShiftEnabled()) && process.getCalendarRuleId() == null) {
            issues.add(buildPreflightIssue(scheduleOrder, process, "BLOCKED_CALENDAR_RULE_MISSING",
                    PREFLIGHT_BLOCKED, "CALENDAR_RULE", null,
                    "工序启用夜班但缺少排程日历规则",
                    "排程维护", calendarAction(scheduleOrder, process)));
        }
    }

    private void appendErpSyncWarning(MesProScheduleOrderDO scheduleOrder,
                                      List<MesProScheduleOrderPreflightIssueRespVO> issues) {
        MesKingdeeProductionOrderSyncRecordDO syncRecord = scheduleOrder.getWorkOrderId() == null
                ? null : syncRecordMapper.selectByWorkOrderId(scheduleOrder.getWorkOrderId());
        if (syncRecord != null) {
            return;
        }
        issues.add(buildPreflightIssue(scheduleOrder, null, "WARN_ERP_SYNC_RECORD_MISSING", PREFLIGHT_WARN,
                "ERP_SYNC", scheduleOrder.getWorkOrderId(), "未找到生产工单的 ERP 同步记录，排产可预览但需确认来源单据",
                "生产计划", workOrderAction(scheduleOrder, "查看生产工单")));
    }

    private MesProScheduleOrderPreflightIssueRespVO buildScheduleOrderNotFoundIssue(Long scheduleOrderId) {
        MesProScheduleOrderPreflightIssueRespVO issue = new MesProScheduleOrderPreflightIssueRespVO();
        issue.setReasonCode("BLOCKED_SCHEDULE_ORDER_NOT_FOUND");
        issue.setSeverity(PREFLIGHT_BLOCKED);
        issue.setObjectType("SCHEDULE_ORDER");
        issue.setObjectId(scheduleOrderId);
        issue.setScheduleOrderId(scheduleOrderId);
        issue.setMessage("排产工单不存在，不能进入排产应用");
        issue.setOwnerRole("排产员");
        return issue;
    }

    private MesProScheduleOrderPreflightIssueRespVO buildPreflightIssue(
            MesProScheduleOrderDO scheduleOrder, MesProScheduleOrderProcessDO process, String reasonCode,
            String severity, String objectType, Long objectId, String message, String ownerRole,
            MesProScheduleOrderIssueActionRespVO action) {
        MesProScheduleOrderPreflightIssueRespVO issue = new MesProScheduleOrderPreflightIssueRespVO();
        issue.setReasonCode(reasonCode);
        issue.setSeverity(severity);
        issue.setObjectType(objectType);
        issue.setObjectId(objectId);
        issue.setWorkOrderId(scheduleOrder.getWorkOrderId());
        issue.setWorkOrderCode(scheduleOrder.getErpWorkOrderCode());
        issue.setScheduleOrderId(scheduleOrder.getId());
        issue.setScheduleOrderCode(scheduleOrder.getCode());
        issue.setProductId(scheduleOrder.getProductId());
        if (process != null) {
            issue.setProcessId(process.getProcessId());
            issue.setProcessName(process.getProcessName());
        }
        issue.setMessage(message);
        issue.setOwnerRole(ownerRole);
        issue.setAction(action);
        return issue;
    }

    private boolean isDefaultScheduleConfig(MesProRouteScheduleConfigDO scheduleConfig) {
        return scheduleDefaultCompatibilityPolicy.warnDefaultRouteScheduleConfig(scheduleConfig);
    }

    private boolean isDefaultScheduleProcess(MesProScheduleOrderProcessDO process) {
        return scheduleDefaultCompatibilityPolicy.warnDefaultResourceSnapshot(process);
    }

    private MesProScheduleOrderIssueActionRespVO routeAction(Long productId, Long routeId, String label) {
        Map<String, Object> query = new LinkedHashMap<>();
        if (productId != null) {
            query.put("productId", productId);
        }
        if (routeId != null) {
            query.put("routeId", routeId);
        }
        return buildIssueAction(label, ROUTE_LIST_TARGET, query, PERMISSION_ROUTE_UPDATE);
    }

    private MesProScheduleOrderIssueActionRespVO routeConfigAction(MesProScheduleOrderDO scheduleOrder,
                                                                   MesProScheduleOrderProcessDO process,
                                                                   String label) {
        return routeConfigAction(scheduleOrder.getRouteId(), process.getRouteProcessId(), label);
    }

    private MesProScheduleOrderIssueActionRespVO routeConfigAction(Long routeId, Long routeProcessId, String label) {
        Map<String, Object> query = new LinkedHashMap<>();
        if (routeId != null) {
            query.put("routeId", routeId);
        }
        if (routeProcessId != null) {
            query.put("routeProcessId", routeProcessId);
        }
        query.put("tab", "schedule-config");
        return buildIssueAction(label, ROUTE_EDIT_TARGET, query, PERMISSION_ROUTE_CONFIG_UPDATE);
    }

    private MesProScheduleOrderIssueActionRespVO calendarAction(MesProScheduleOrderDO scheduleOrder,
                                                               MesProScheduleOrderProcessDO process) {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("scheduleOrderId", scheduleOrder.getId());
        if (process.getProcessId() != null) {
            query.put("processId", process.getProcessId());
        }
        return buildIssueAction("维护日历", "MesProScheduleCalendar", query, PERMISSION_CALENDAR_UPDATE);
    }

    private MesProScheduleOrderIssueActionRespVO workstationAction(Long routeId, String label) {
        Map<String, Object> query = new LinkedHashMap<>();
        if (routeId != null) {
            query.put("routeId", routeId);
        }
        return buildIssueAction(label, "MesMdWorkstation", query, "mes:md-workstation:query");
    }

    private MesProScheduleOrderIssueActionRespVO workOrderAction(MesProScheduleOrderDO scheduleOrder, String label) {
        Map<String, Object> query = new LinkedHashMap<>();
        if (scheduleOrder.getWorkOrderId() != null) {
            query.put("id", scheduleOrder.getWorkOrderId());
        }
        if (StrUtil.isNotBlank(scheduleOrder.getErpWorkOrderCode())) {
            query.put("code", scheduleOrder.getErpWorkOrderCode());
        }
        return buildIssueAction(label, "MesProWorkOrder", query, "mes:pro-work-order:query");
    }

    private MesProScheduleOrderIssueActionRespVO buildIssueAction(String label, String routeName,
                                                                  Map<String, Object> query,
                                                                  String requiredPermission) {
        MesProScheduleOrderIssueActionRespVO action = new MesProScheduleOrderIssueActionRespVO();
        action.setActionLabel(label);
        action.setTargetRouteName(routeName);
        action.setTargetQuery(query);
        action.setRequiredPermission(requiredPermission);
        return action;
    }

    private record AdmissionRouteCheck(boolean routeReady, boolean warnOnly, String reasonCode, String message, String ownerRole,
                                       MesProScheduleOrderIssueActionRespVO action) {
        private static AdmissionRouteCheck ready() {
            return new AdmissionRouteCheck(true, false, null, null, null, null);
        }

        private static AdmissionRouteCheck blocked(String reasonCode, String message, String ownerRole,
                                                   MesProScheduleOrderIssueActionRespVO action) {
            return new AdmissionRouteCheck(false, false, reasonCode, message, ownerRole, action);
        }

        private static AdmissionRouteCheck warn(String reasonCode, String message, String ownerRole,
                                                MesProScheduleOrderIssueActionRespVO action) {
            return new AdmissionRouteCheck(false, true, reasonCode, message, ownerRole, action);
        }
    }

    @Override
    public List<MesProScheduleOrderProcessDO> getScheduleOrderProcessList(Long scheduleOrderId) {
        return scheduleOrderProcessMapper.selectListByScheduleOrderId(scheduleOrderId);
    }

    @Override
    public List<MesProScheduleOrderProcessDO> getScheduleOrderProcessListByScheduleOrderIds(Collection<Long> scheduleOrderIds) {
        return scheduleOrderProcessMapper.selectListByScheduleOrderIds(scheduleOrderIds);
    }

    @Override
    public List<MesProFeedbackDO> getProgressFeedbackList(Long scheduleOrderId) {
        return feedbackMapper.selectProgressListByScheduleOrderId(scheduleOrderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncFeedbackProgress(Long scheduleOrderId) {
        MesProScheduleOrderDO scheduleOrder = validateWritableScheduleOrder(scheduleOrderId);
        RecalculatedProgressSnapshot recalculated = recalculateProgressSnapshot(scheduleOrderId, scheduleOrder, false);
        scheduleOrderMapper.updateProgressSummary(scheduleOrderId, recalculated.summary().totalQuantity(),
                recalculated.summary().completedQuantity(), recalculated.summary().uncompletedQuantity(),
                recalculated.summary().progressPercent(), recalculated.status());
        insertOperationLog(scheduleOrder, recalculated.auditPayload(), "SYNC_PROGRESS", "同步报工进度");
    }

    private Integer resolveProgressStatus(MesProScheduleOrderDO scheduleOrder,
                                          ProgressSummary summary,
                                          List<MesProFeedbackDO> feedbackList) {
        if (ObjUtil.equal(scheduleOrder.getStatus(), MesProScheduleOrderStatusEnum.CANCELED.getStatus())) {
            return MesProScheduleOrderStatusEnum.CANCELED.getStatus();
        }
        if (summary.totalQuantity().compareTo(BigDecimal.ZERO) > 0
                && summary.completedQuantity().compareTo(summary.totalQuantity()) >= 0) {
            return MesProScheduleOrderStatusEnum.FINISHED.getStatus();
        }
        if (CollUtil.isNotEmpty(feedbackList) || summary.completedQuantity().compareTo(BigDecimal.ZERO) > 0) {
            return MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus();
        }
        if (ObjUtil.equal(scheduleOrder.getStatus(), MesProScheduleOrderStatusEnum.FINISHED.getStatus())) {
            return scheduleOrder.getPlannedStartTime() != null || scheduleOrder.getPlannedEndTime() != null
                    ? MesProScheduleOrderStatusEnum.SCHEDULED.getStatus()
                    : MesProScheduleOrderStatusEnum.PREPARE.getStatus();
        }
        return scheduleOrder.getStatus();
    }

    @Override
    public List<MesProScheduleOrderDailyCompareDO> getDailyCompare(Long scheduleOrderId, LocalDate startDate, LocalDate endDate) {
        validateScheduleOrderExists(scheduleOrderId);
        List<MesProScheduleOrderProcessDO> processes = scheduleOrderProcessMapper.selectListByScheduleOrderId(scheduleOrderId);
        List<MesProFeedbackDO> feedbackList = feedbackMapper.selectProgressListByScheduleOrderId(scheduleOrderId);
        Map<Long, Long> processIdByScheduleOrderProcessId = processes.stream()
                .filter(process -> process.getId() != null)
                .collect(Collectors.toMap(MesProScheduleOrderProcessDO::getId,
                        MesProScheduleOrderProcessDO::getProcessId, (left, right) -> left, LinkedHashMap::new));
        Map<DailyCompareKey, BigDecimal> plannedByKey = buildPlannedQuantityByDate(processes, startDate, endDate);
        Map<DailyCompareKey, BigDecimal> actualByKey = new LinkedHashMap<>();
        for (MesProFeedbackDO feedback : feedbackList) {
            if (!ObjUtil.equal(feedback.getStatus(), MesProFeedbackStatusEnum.FINISHED.getStatus())) {
                continue;
            }
            if (feedback.getFeedbackTime() == null) {
                continue;
            }
            LocalDate feedbackDate = feedback.getFeedbackTime().toLocalDate();
            if (outsideDateRange(feedbackDate, startDate, endDate)) {
                continue;
            }
            DailyCompareKey key = new DailyCompareKey(feedbackDate, feedback.getScheduleOrderProcessId());
            actualByKey.merge(key, normalizeQuantity(feedback.getFeedbackQuantity()), BigDecimal::add);
        }

        Set<DailyCompareKey> keys = new HashSet<>();
        keys.addAll(plannedByKey.keySet());
        keys.addAll(actualByKey.keySet());
        return keys.stream()
                .sorted(Comparator.comparing(DailyCompareKey::planDate)
                        .thenComparing(DailyCompareKey::scheduleOrderProcessId, Comparator.nullsLast(Long::compareTo)))
                .map(key -> buildDailyCompare(scheduleOrderId, key, plannedByKey.getOrDefault(key, BigDecimal.ZERO),
                        actualByKey.getOrDefault(key, BigDecimal.ZERO),
                        processIdByScheduleOrderProcessId.get(key.scheduleOrderProcessId())))
                .toList();
    }

    @Override
    public List<MesProScheduleOrderProcessWipRespVO> getProcessWipStatistics() {
        List<MesProScheduleOrderDO> scheduleOrders = scheduleOrderMapper.selectListForProcessWip().stream()
                .filter(order -> !Boolean.TRUE.equals(order.getFrozen()))
                .filter(order -> !Boolean.TRUE.equals(order.getManualFinished()))
                .toList();
        if (CollUtil.isEmpty(scheduleOrders)) {
            return Collections.emptyList();
        }
        Map<Long, MesProScheduleOrderDO> scheduleOrderMap = scheduleOrders.stream()
                .collect(Collectors.toMap(MesProScheduleOrderDO::getId, item -> item, (left, right) -> left,
                        LinkedHashMap::new));
        List<MesProScheduleOrderProcessDO> allProcesses = scheduleOrderProcessMapper.selectListByScheduleOrderIds(
                scheduleOrderMap.keySet());
        List<MesProScheduleOrderProcessDO> wipProcesses = allProcesses.stream()
                .filter(this::isProcessWip)
                .toList();
        if (CollUtil.isEmpty(wipProcesses)) {
            return Collections.emptyList();
        }
        Map<Long, RouteProcessIdentity> keyByScheduleOrderProcessId = new LinkedHashMap<>();
        Map<RouteProcessIdentity, List<MesProScheduleOrderProcessDO>> wipProcessesByRouteProcess =
                new LinkedHashMap<>();
        for (MesProScheduleOrderProcessDO process : wipProcesses) {
            RouteProcessIdentity key = resolveRouteProcessWipKey(process, scheduleOrderMap.get(process.getScheduleOrderId()));
            wipProcessesByRouteProcess.computeIfAbsent(key, ignored -> new ArrayList<>()).add(process);
            keyByScheduleOrderProcessId.put(process.getId(), key);
        }
        Set<Long> routeIds = wipProcessesByRouteProcess.keySet().stream()
                .map(RouteProcessIdentity::routeId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<MesProRouteDO> routes = routeMapper.selectBatchIds(routeIds);
        Map<Long, MesProRouteDO> routeMap = CollUtil.isEmpty(routes) ? Collections.emptyMap()
                : routes.stream().collect(Collectors.toMap(MesProRouteDO::getId, item -> item,
                (left, right) -> left, LinkedHashMap::new));
        Set<Long> routeVersionIds = wipProcessesByRouteProcess.keySet().stream()
                .map(RouteProcessIdentity::routeVersionId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<MesProRouteVersionDO> routeVersions = routeVersionMapper.selectBatchIds(routeVersionIds);
        Map<Long, MesProRouteVersionDO> routeVersionMap = CollUtil.isEmpty(routeVersions) ? Collections.emptyMap()
                : routeVersions.stream().collect(Collectors.toMap(MesProRouteVersionDO::getId, item -> item,
                (left, right) -> left, LinkedHashMap::new));
        Set<Long> routeProcessIds = wipProcessesByRouteProcess.keySet().stream()
                .map(RouteProcessIdentity::routeProcessId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<MesProRouteProcessDO> currentRouteProcesses = loadRouteProcessesIncludingDeleted(routeProcessIds);
        Map<Long, MesProRouteProcessDO> currentRouteProcessMap = CollUtil.isEmpty(currentRouteProcesses)
                ? Collections.emptyMap()
                : currentRouteProcesses.stream().collect(Collectors.toMap(MesProRouteProcessDO::getId, item -> item,
                (left, right) -> left, LinkedHashMap::new));
        ResourceSnapshotContext currentResourceContext = buildResourceSnapshotContext(currentRouteProcesses, false);
        Set<Long> currentProcessIds = currentRouteProcesses.stream()
                .map(MesProRouteProcessDO::getProcessId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<MesProProcessDO> currentProcesses = loadProcessesIncludingDeleted(currentProcessIds);
        Map<Long, MesProProcessDO> currentProcessMap = CollUtil.isEmpty(currentProcesses)
                ? Collections.emptyMap()
                : currentProcesses.stream().collect(Collectors.toMap(MesProProcessDO::getId, item -> item,
                (left, right) -> left, LinkedHashMap::new));
        Map<RouteProcessIdentity, BigDecimal> todayFeedbackQuantityByRouteProcess =
                sumTodayFinishedFeedbackByRouteProcess(wipProcesses, keyByScheduleOrderProcessId);
        return wipProcessesByRouteProcess.entrySet().stream()
                .map(entry -> {
                    RouteProcessIdentity key = entry.getKey();
                    List<MesProScheduleOrderProcessDO> processRows = entry.getValue();
                    MesProRouteProcessDO currentRouteProcess = currentRouteProcessMap.get(key.routeProcessId());
                    if (currentRouteProcess == null) {
                        throw exception(PRO_ROUTE_PROCESS_NOT_EXISTS);
                    }
                    MesProProcessDO currentProcess = currentProcessMap.get(currentRouteProcess.getProcessId());
                    if (currentProcess == null) {
                        throw exception(PRO_PROCESS_NOT_EXISTS);
                    }
                    MesProRouteScheduleConfigDO routeConfig = requireRouteScheduleConfig(
                            key.routeVersionId(), key.routeProcessId(), currentProcess.getId());
                    MesProRouteDO route = routeMap.get(key.routeId());
                    MesProRouteVersionDO routeVersion = routeVersionMap.get(key.routeVersionId());
                    if (routeVersion == null) {
                        throw exception(PRO_ROUTE_VERSION_NOT_EXISTS, key.routeVersionId());
                    }
                    ResourceSnapshot capacitySnapshot = resolveCurrentRouteProcessCapacitySnapshot(routeConfig,
                            currentRouteProcess, currentResourceContext, false);
                    BigDecimal shiftCapacityTotal = normalizeQuantity(capacitySnapshot.shiftCapacityTotal());
                    BigDecimal estimateShiftCapacity = resolveProcessWipEstimateCapacity(shiftCapacityTotal,
                            currentResourceContext.maxShiftCapacityByProcessId().get(currentProcess.getId()));
                    String capacitySource = resolveCurrentRouteProcessCapacitySource(routeConfig, capacitySnapshot);
                    BigDecimal unfinishedDemandQuantity = sumProcessQuantity(processRows,
                            MesProScheduleOrderProcessDO::getRemainingQuantity);
                    LocalDateTime estimatedStartTime = estimateStartTime(processRows);
                    boolean nightShiftEnabled = Boolean.TRUE.equals(routeConfig.getNightShiftEnabled());
                    List<Long> scheduleOrderIds = processRows.stream()
                            .map(MesProScheduleOrderProcessDO::getScheduleOrderId)
                            .filter(Objects::nonNull)
                            .distinct()
                            .sorted()
                            .toList();
                    return MesProScheduleOrderProcessWipRespVO.builder()
                            .routeId(key.routeId())
                            .routeCode(route == null ? null : route.getCode())
                            .routeName(route == null ? null : route.getName())
                            .routeVersionId(key.routeVersionId())
                            .routeVersionNo(routeVersion.getVersionNo())
                            .routeVersionStatus(routeVersion.getLifecycleStatus())
                            .routeProcessId(key.routeProcessId())
                            .processId(currentProcess.getId())
                            .processCode(currentProcess.getCode())
                            .processName(currentProcess.getName())
                            .wipOrderCount((long) scheduleOrderIds.size())
                            .shiftCapacityTotal(shiftCapacityTotal)
                            .capacityMode(routeConfig.getCapacityMode())
                            .capacitySource(capacitySource)
                            .resourceStatus(resolveCurrentRouteProcessResourceStatus(routeConfig, capacitySnapshot))
                            .resourceStatusReason(resolveCurrentRouteProcessResourceStatusReason(routeConfig, capacitySnapshot))
                            .shiftStatus(nightShiftEnabled ? "夜班" : "白班")
                            .nightShiftEnabled(nightShiftEnabled)
                            .plannedStartDate(resolveProcessPlannedStartDate(processRows))
                            .plannedStartDateMixed(resolveProcessPlannedStartDateMixed(processRows))
                            .unfinishedDemandQuantity(unfinishedDemandQuantity)
                            .estimatedStartTime(estimatedStartTime)
                            .estimatedCompletionTime(estimateCompletionTime(
                                    estimatedStartTime, unfinishedDemandQuantity, estimateShiftCapacity))
                            .todayFeedbackQuantity(todayFeedbackQuantityByRouteProcess
                                    .getOrDefault(key, BigDecimal.ZERO).setScale(6))
                            .scheduleOrderIds(scheduleOrderIds)
                            .build();
                })
                .sorted(Comparator.comparing(MesProScheduleOrderProcessWipRespVO::getWipOrderCount).reversed()
                        .thenComparing(MesProScheduleOrderProcessWipRespVO::getRouteName,
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(MesProScheduleOrderProcessWipRespVO::getProcessName,
                                Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveProcessWipSettings(MesProScheduleOrderProcessWipSettingsReqVO reqVO) {
        validateOperationReason(reqVO.getReason());
        List<MesProScheduleOrderDO> scheduleOrders = scheduleOrderMapper.selectListForProcessWip().stream()
                .filter(order -> !Boolean.TRUE.equals(order.getFrozen()))
                .filter(order -> !Boolean.TRUE.equals(order.getManualFinished()))
                .toList();
        if (CollUtil.isEmpty(scheduleOrders)) {
            throw exception(PRO_SCHEDULE_ORDER_PROCESS_WIP_NOT_EXISTS, reqVO.getRouteProcessId());
        }
        Map<Long, MesProScheduleOrderDO> scheduleOrderMap = scheduleOrders.stream()
                .collect(Collectors.toMap(MesProScheduleOrderDO::getId, item -> item, (left, right) -> left,
                        LinkedHashMap::new));
        List<MesProScheduleOrderProcessDO> targetProcesses = scheduleOrderProcessMapper
                .selectListByScheduleOrderIds(scheduleOrderMap.keySet()).stream()
                .filter(this::isProcessWip)
                .filter(process -> Objects.equals(resolveRouteVersionId(
                        process, scheduleOrderMap.get(process.getScheduleOrderId())), reqVO.getRouteVersionId()))
                .filter(process -> matchesAndNormalizeCurrentRouteProcess(
                        process, scheduleOrderMap.get(process.getScheduleOrderId()), reqVO.getRouteProcessId()))
                .collect(Collectors.toCollection(ArrayList::new));
        if (CollUtil.isEmpty(targetProcesses)) {
            throw exception(PRO_SCHEDULE_ORDER_PROCESS_WIP_NOT_EXISTS, reqVO.getRouteProcessId());
        }
        MesProRouteScheduleConfigDO routeConfig = requireRouteScheduleConfig(reqVO.getRouteVersionId(),
                reqVO.getRouteProcessId(), targetProcesses.get(0).getProcessId());
        Long calendarRuleId = resolveRequestedCalendarRuleId(reqVO, routeConfig);
        updateRouteProcessNightShiftConfig(reqVO, routeConfig, calendarRuleId);
        LocalDateTime plannedStartTime = reqVO.getPlannedStartDate() == null
                ? null : reqVO.getPlannedStartDate().atStartOfDay();
        for (MesProScheduleOrderProcessDO process : targetProcesses) {
            applyCurrentRouteScheduleConfig(process, routeConfig);
            MesProScheduleOrderProcessDO updateObj = new MesProScheduleOrderProcessDO();
            updateObj.setId(process.getId());
            updateObj.setRouteProcessId(process.getRouteProcessId());
            updateObj.setProcessId(process.getProcessId());
            updateObj.setRouteScheduleConfigId(routeConfig.getId());
            updateObj.setCapacityMode(routeConfig.getCapacityMode());
            updateObj.setInfiniteDurationQuantityFactor(routeConfig.getInfiniteDurationQuantityFactor());
            updateObj.setInfiniteDurationBaseMinutes(routeConfig.getInfiniteDurationBaseMinutes());
            if (MesProScheduleCapacityModeEnum.isManualOverrideLike(routeConfig.getCapacityMode())) {
                updateObj.setHourlyCapacityTotal(routeConfig.getHourlyCapacity());
                if (routeConfig.getHourlyCapacity() != null && process.getShiftHours() != null) {
                    updateObj.setShiftCapacityTotal(routeConfig.getHourlyCapacity().multiply(process.getShiftHours()));
                }
            } else {
                updateObj.setCapacitySource(process.getCapacitySource());
                updateObj.setHourlyCapacityTotal(process.getHourlyCapacityTotal());
                updateObj.setShiftCapacityTotal(process.getShiftCapacityTotal());
                if (StrUtil.isNotBlank(process.getResourceSnapshotJson())) {
                    updateObj.setResourceSnapshotJson(process.getResourceSnapshotJson());
                }
            }
            updateObj.setNightShiftEnabled(reqVO.getNightShiftEnabled() == null
                    ? Boolean.TRUE.equals(routeConfig.getNightShiftEnabled())
                    : Boolean.TRUE.equals(reqVO.getNightShiftEnabled()));
            updateObj.setCalendarRuleId(calendarRuleId);
            updateObj.setPlannedStartTime(plannedStartTime);
            scheduleOrderProcessMapper.updateById(updateObj);

            MesProScheduleOrderDO scheduleOrder = scheduleOrderMap.get(process.getScheduleOrderId());
            if (scheduleOrder != null) {
                insertOperationLog(scheduleOrder, buildProcessWipSettingsLogSnapshot(process, reqVO),
                        "PROCESS_WIP_SETTINGS", reqVO.getReason());
            }
        }
    }

    private boolean matchesAndNormalizeCurrentRouteProcess(MesProScheduleOrderProcessDO process,
                                                           MesProScheduleOrderDO scheduleOrder,
                                                           Long requestedRouteProcessId) {
        if (process == null || scheduleOrder == null || scheduleOrder.getRouteId() == null
                || process.getRouteProcessId() == null) {
            return false;
        }
        MesProRouteProcessDO frozenRouteProcess = routeProcessService.resolveFrozenRouteProcess(
                process.getRouteProcessId(), scheduleOrder.getRouteId(), process.getProcessId());
        if (!Objects.equals(frozenRouteProcess.getId(), requestedRouteProcessId)) {
            return false;
        }
        process.setRouteProcessId(frozenRouteProcess.getId());
        process.setProcessId(frozenRouteProcess.getProcessId());
        return true;
    }

    private RouteProcessIdentity resolveRouteProcessWipKey(MesProScheduleOrderProcessDO process,
                                                           MesProScheduleOrderDO scheduleOrder) {
        Long routeVersionId = resolveRouteVersionId(process, scheduleOrder);
        if (scheduleOrder == null || scheduleOrder.getRouteId() == null || routeVersionId == null
                || process == null || process.getRouteProcessId() == null) {
            throw exception(PRO_SCHEDULE_ORDER_ROUTE_PROCESS_REQUIRED);
        }
        Long currentRouteProcessId = routeProcessService.resolveFrozenRouteProcess(
                process.getRouteProcessId(), scheduleOrder.getRouteId(), process.getProcessId()).getId();
        return RouteProcessIdentity.of(scheduleOrder.getRouteId(), routeVersionId, currentRouteProcessId);
    }

    private Long resolveRouteVersionId(MesProScheduleOrderProcessDO process, MesProScheduleOrderDO scheduleOrder) {
        if (process != null && process.getRouteVersionId() != null) {
            return process.getRouteVersionId();
        }
        return scheduleOrder == null ? null : scheduleOrder.getRouteVersionId();
    }

    private List<MesProRouteProcessDO> loadRouteProcessesIncludingDeleted(Collection<Long> routeProcessIds) {
        if (CollUtil.isEmpty(routeProcessIds)) {
            return Collections.emptyList();
        }
        Map<Long, MesProRouteProcessDO> routeProcessMap = routeProcessMapper.selectBatchIds(routeProcessIds).stream()
                .filter(routeProcess -> routeProcess.getId() != null)
                .collect(Collectors.toMap(MesProRouteProcessDO::getId, item -> item,
                        (left, right) -> left, LinkedHashMap::new));
        for (Long routeProcessId : routeProcessIds) {
            if (routeProcessId == null || routeProcessMap.containsKey(routeProcessId)) {
                continue;
            }
            MesProRouteProcessDO deletedRouteProcess = routeProcessMapper.selectByIdIgnoreDeleted(routeProcessId);
            if (deletedRouteProcess != null && deletedRouteProcess.getId() != null) {
                routeProcessMap.put(deletedRouteProcess.getId(), deletedRouteProcess);
            }
        }
        return new ArrayList<>(routeProcessMap.values());
    }

    private List<MesProProcessDO> loadProcessesIncludingDeleted(Collection<Long> processIds) {
        if (CollUtil.isEmpty(processIds)) {
            return Collections.emptyList();
        }
        Map<Long, MesProProcessDO> processMap = processMapper.selectBatchIds(processIds).stream()
                .filter(process -> process.getId() != null)
                .collect(Collectors.toMap(MesProProcessDO::getId, item -> item,
                        (left, right) -> left, LinkedHashMap::new));
        Set<Long> missingProcessIds = processIds.stream()
                .filter(Objects::nonNull)
                .filter(processId -> !processMap.containsKey(processId))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (CollUtil.isNotEmpty(missingProcessIds)) {
            processMapper.selectListByIdsIgnoreDeleted(missingProcessIds).stream()
                    .filter(process -> process.getId() != null)
                    .forEach(process -> processMap.putIfAbsent(process.getId(), process));
        }
        return new ArrayList<>(processMap.values());
    }

    private MesProRouteScheduleConfigDO requireRouteScheduleConfig(Long routeVersionId, Long routeProcessId,
                                                                   Long processId) {
        MesProRouteScheduleConfigDO routeConfig = routeScheduleConfigMapper
                .selectByRouteVersionIdAndRouteProcessId(routeVersionId, routeProcessId);
        if (routeConfig == null) {
            MesProRouteVersionDO routeVersion = routeVersionMapper.selectById(routeVersionId);
            if (routeVersion != null) {
                routeConfig = routeScheduleConfigMapper.selectListByRouteVersionId(routeVersionId).stream()
                        .filter(config -> config.getRouteProcessId() != null)
                        .filter(config -> Objects.equals(routeProcessId,
                                routeProcessService.resolveFrozenRouteProcess(
                                        config.getRouteProcessId(), routeVersion.getRouteId(), null).getId()))
                        .findFirst()
                        .orElse(null);
            }
        }
        if (routeConfig == null) {
            throw exception(PRO_SCHEDULE_ORDER_ROUTE_SCHEDULE_CONFIG_REQUIRED, processId);
        }
        return routeConfig;
    }

    private Long resolveRequestedCalendarRuleId(MesProScheduleOrderProcessWipSettingsReqVO reqVO,
                                                MesProRouteScheduleConfigDO routeConfig) {
        if (reqVO.getNightShiftEnabled() == null) {
            return routeConfig.getCalendarRuleId();
        }
        if (!Boolean.TRUE.equals(reqVO.getNightShiftEnabled())) {
            return null;
        }
        if (routeConfig.getCalendarRuleId() != null) {
            return routeConfig.getCalendarRuleId();
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        MesProScheduleCalendarRuleDO tenantRule = scheduleCalendarRuleMapper.selectByTenantId(tenantId);
        if (tenantRule == null || tenantRule.getId() == null) {
            throw exception(PRO_SCHEDULE_ORDER_PROCESS_WIP_CALENDAR_RULE_REQUIRED, reqVO.getRouteProcessId());
        }
        return tenantRule.getId();
    }

    private void updateRouteProcessNightShiftConfig(MesProScheduleOrderProcessWipSettingsReqVO reqVO,
                                                    MesProRouteScheduleConfigDO routeConfig,
                                                    Long calendarRuleId) {
        if (reqVO.getNightShiftEnabled() == null) {
            return;
        }
        MesProRouteScheduleConfigDO updateConfig = new MesProRouteScheduleConfigDO();
        updateConfig.setId(routeConfig.getId());
        updateConfig.setNightShiftEnabled(Boolean.TRUE.equals(reqVO.getNightShiftEnabled()));
        updateConfig.setCalendarRuleId(calendarRuleId);
        routeScheduleConfigMapper.updateById(updateConfig);
        routeConfig.setNightShiftEnabled(updateConfig.getNightShiftEnabled());
        routeConfig.setCalendarRuleId(calendarRuleId);
    }

    private void applyCurrentRouteScheduleConfig(MesProScheduleOrderProcessDO process,
                                                 MesProRouteScheduleConfigDO currentConfig) {
        if (process == null || currentConfig == null) {
            return;
        }
        process.setRouteScheduleConfigId(currentConfig.getId());
        process.setCapacityMode(currentConfig.getCapacityMode());
        if (MesProScheduleCapacityModeEnum.isManualOverrideLike(currentConfig.getCapacityMode())) {
            process.setHourlyCapacityTotal(currentConfig.getHourlyCapacity());
        }
        process.setInfiniteDurationQuantityFactor(currentConfig.getInfiniteDurationQuantityFactor());
        process.setInfiniteDurationBaseMinutes(currentConfig.getInfiniteDurationBaseMinutes());
        process.setNightShiftEnabled(Boolean.TRUE.equals(currentConfig.getNightShiftEnabled()));
        process.setCalendarRuleId(currentConfig.getCalendarRuleId());
    }

    private Map<RouteProcessIdentity, BigDecimal> sumTodayFinishedFeedbackByRouteProcess(
            List<MesProScheduleOrderProcessDO> wipProcesses,
            Map<Long, RouteProcessIdentity> keyByScheduleOrderProcessId) {
        if (CollUtil.isEmpty(wipProcesses)) {
            return Collections.emptyMap();
        }
        Set<Long> scheduleOrderProcessIds = wipProcesses.stream()
                .map(MesProScheduleOrderProcessDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (scheduleOrderProcessIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<MesProFeedbackDO> feedbackList = feedbackMapper.selectFinishedListByScheduleOrderProcessIdsToday(
                scheduleOrderProcessIds, LocalDate.now());
        Map<RouteProcessIdentity, BigDecimal> result = new LinkedHashMap<>();
        for (MesProFeedbackDO feedback : feedbackList) {
            RouteProcessIdentity key = keyByScheduleOrderProcessId.get(feedback.getScheduleOrderProcessId());
            if (key == null) {
                continue;
            }
            result.merge(key, normalizeQuantity(feedback.getFeedbackQuantity()), BigDecimal::add);
        }
        return result;
    }

    private ResourceSnapshot resolveCurrentRouteProcessCapacitySnapshot(MesProRouteScheduleConfigDO routeConfig,
                                                                        MesProRouteProcessDO currentRouteProcess,
                                                                        ResourceSnapshotContext currentResourceContext) {
        return resolveCurrentRouteProcessCapacitySnapshot(routeConfig, currentRouteProcess, currentResourceContext, true);
    }

    private ResourceSnapshot resolveCurrentRouteProcessCapacitySnapshot(MesProRouteScheduleConfigDO routeConfig,
                                                                        MesProRouteProcessDO currentRouteProcess,
                                                                        ResourceSnapshotContext currentResourceContext,
                                                                        boolean failFastOnMissingResource) {
        ResourceSnapshot resourceSnapshot = currentResourceContext.snapshotByRouteProcessId()
                .getOrDefault(currentRouteProcess.getId(), ResourceSnapshot.unconfigured(currentRouteProcess.getId()));
        if (failFastOnMissingResource && isResourceCalculated(routeConfig) && !hasPositiveResourceCapacity(resourceSnapshot)) {
            throw exception(PRO_SCHEDULE_ORDER_RESOURCE_CAPACITY_REQUIRED, currentRouteProcess.getId());
        }
        return applyScheduleConfig(resourceSnapshot, routeConfig);
    }

    private String resolveCurrentRouteProcessCapacitySource(MesProRouteScheduleConfigDO routeConfig,
                                                           ResourceSnapshot capacitySnapshot) {
        if (MesProScheduleCapacityModeEnum.isManualOverrideLike(routeConfig.getCapacityMode())) {
            return CAPACITY_SOURCE_MANUAL_OVERRIDE;
        }
        if (MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode().equals(routeConfig.getCapacityMode())) {
            return CAPACITY_SOURCE_INFINITE_FORMULA;
        }
        return capacitySnapshot.capacitySource();
    }

    private String resolveCurrentRouteProcessResourceStatus(MesProRouteScheduleConfigDO routeConfig,
                                                            ResourceSnapshot capacitySnapshot) {
        if (!isResourceCalculated(routeConfig)) {
            return RESOURCE_STATUS_NORMAL;
        }
        return capacitySnapshot.resourceStatus();
    }

    private String resolveCurrentRouteProcessResourceStatusReason(MesProRouteScheduleConfigDO routeConfig,
                                                                  ResourceSnapshot capacitySnapshot) {
        if (!isResourceCalculated(routeConfig)) {
            return RESOURCE_REASON_NORMAL;
        }
        return capacitySnapshot.resourceStatusReason();
    }

    private BigDecimal sumProcessQuantity(List<MesProScheduleOrderProcessDO> processes,
                                          java.util.function.Function<MesProScheduleOrderProcessDO, BigDecimal> getter) {
        if (CollUtil.isEmpty(processes)) {
            return BigDecimal.ZERO.setScale(6);
        }
        return processes.stream()
                .map(getter)
                .map(this::normalizeQuantity)
                .reduce(BigDecimal.ZERO.setScale(6), BigDecimal::add)
                .setScale(6);
    }

    private BigDecimal resolveProcessWipEstimateCapacity(BigDecimal shiftCapacityTotal,
                                                         BigDecimal processMaxShiftCapacity) {
        BigDecimal currentCapacity = normalizeQuantity(shiftCapacityTotal);
        BigDecimal maxCapacity = normalizeQuantity(processMaxShiftCapacity);
        return currentCapacity.max(maxCapacity);
    }

    private LocalDate resolveProcessPlannedStartDate(List<MesProScheduleOrderProcessDO> processes) {
        if (resolveProcessPlannedStartDateMixed(processes)) {
            return null;
        }
        return processes.stream()
                .map(MesProScheduleOrderProcessDO::getPlannedStartTime)
                .filter(Objects::nonNull)
                .map(LocalDateTime::toLocalDate)
                .findFirst()
                .orElse(null);
    }

    private boolean resolveProcessPlannedStartDateMixed(List<MesProScheduleOrderProcessDO> processes) {
        return processes.stream()
                .map(MesProScheduleOrderProcessDO::getPlannedStartTime)
                .map(time -> time == null ? null : time.toLocalDate())
                .distinct()
                .count() > 1;
    }

    private Map<String, Object> buildProcessWipSettingsLogSnapshot(MesProScheduleOrderProcessDO process,
                                                                   MesProScheduleOrderProcessWipSettingsReqVO reqVO) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("scheduleOrderProcessId", process.getId());
        snapshot.put("routeVersionId", reqVO.getRouteVersionId());
        snapshot.put("routeProcessId", reqVO.getRouteProcessId());
        snapshot.put("processId", process.getProcessId());
        snapshot.put("nightShiftEnabled", Boolean.TRUE.equals(reqVO.getNightShiftEnabled()));
        snapshot.put("plannedStartDate", reqVO.getPlannedStartDate());
        return snapshot;
    }

    private LocalDateTime estimateStartTime(List<MesProScheduleOrderProcessDO> processes) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        if (CollUtil.isEmpty(processes)) {
            return todayStart;
        }
        LocalDateTime latestPlannedStartTime = processes.stream()
                .map(MesProScheduleOrderProcessDO::getPlannedStartTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        return latestPlannedStartTime != null && latestPlannedStartTime.isAfter(todayStart)
                ? latestPlannedStartTime : todayStart;
    }

    private LocalDateTime estimateCompletionTime(LocalDateTime estimatedStartTime,
                                                 BigDecimal unfinishedDemandQuantity,
                                                 BigDecimal shiftCapacityTotal) {
        BigDecimal demand = normalizeQuantity(unfinishedDemandQuantity);
        BigDecimal capacity = normalizeQuantity(shiftCapacityTotal);
        if (demand.compareTo(BigDecimal.ZERO) <= 0) {
            return estimatedStartTime;
        }
        if (capacity.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        long remainingShifts = demand.divide(capacity, 0, RoundingMode.CEILING).longValue();
        return estimatedStartTime.plusDays(remainingShifts);
    }

    private boolean isProcessWip(MesProScheduleOrderProcessDO process) {
        return Boolean.TRUE.equals(process.getEnabled())
                && (process.getProgressPercent() == null
                || process.getProgressPercent().compareTo(new BigDecimal("100")) < 0);
    }

    private java.util.Optional<MesProScheduleOrderProcessDO> resolveCurrentProcess(List<MesProScheduleOrderProcessDO> processes) {
        return processes.stream()
                .sorted(Comparator.comparing(MesProScheduleOrderProcessDO::getSort,
                        Comparator.nullsLast(Integer::compareTo)))
                .filter(this::isProcessWip)
                .findFirst();
    }

    @Override
    public List<MesProScheduleOrderOperationLogDO> getOperationLogList(Long scheduleOrderId) {
        if (scheduleOrderId == null) {
            throw exception(PRO_SCHEDULE_ORDER_NOT_EXISTS);
        }
        return scheduleOrderOperationLogMapper.selectListByScheduleOrderId(scheduleOrderId);
    }

    private void validateWorkOrderSchedulable(MesProWorkOrderDO workOrder) {
        if (Boolean.TRUE.equals(workOrder.getTemporaryFrozen())) {
            throw exception(PRO_SCHEDULE_ORDER_WORK_ORDER_FROZEN);
        }
        Integer status = workOrder.getStatus();
        if (ObjUtil.equal(status, MesProWorkOrderStatusEnum.FINISHED.getStatus())
                || ObjUtil.equal(status, MesProWorkOrderStatusEnum.CANCELED.getStatus())) {
            throw exception(PRO_SCHEDULE_ORDER_WORK_ORDER_NOT_CONFIRMED);
        }
    }

    private MesProScheduleOrderDO validateScheduleOrderExists(Long scheduleOrderId) {
        MesProScheduleOrderDO scheduleOrder = scheduleOrderMapper.selectById(scheduleOrderId);
        if (scheduleOrder == null) {
            throw exception(PRO_SCHEDULE_ORDER_NOT_EXISTS);
        }
        return scheduleOrder;
    }

    private MesProScheduleOrderDO validateWritableScheduleOrder(Long scheduleOrderId) {
        MesProScheduleOrderDO scheduleOrder = validateScheduleOrderExists(scheduleOrderId);
        if (Boolean.TRUE.equals(scheduleOrder.getFrozen())) {
            throw exception0(PRO_SCHEDULE_ORDER_FROZEN.getCode(), "排产工单已冻结，禁止写入操作: {}",
                    getScheduleOrderCode(scheduleOrder));
        }
        return scheduleOrder;
    }

    private void validateBatchRequest(MesProScheduleOrderBatchReqVO reqVO) {
        if (reqVO == null || CollUtil.isEmpty(reqVO.getIds())) {
            throw exception(PRO_SCHEDULE_ORDER_BATCH_REQUIRED);
        }
        validateOperationReason(reqVO.getReason());
    }

    private void validateOperationReason(String reason) {
        if (StrUtil.isBlank(reason)) {
            throw exception(PRO_SCHEDULE_ORDER_REASON_REQUIRED);
        }
    }

    private List<MesProScheduleOrderDO> getRequiredScheduleOrders(Collection<Long> ids) {
        List<MesProScheduleOrderDO> scheduleOrders = scheduleOrderMapper.selectListByIds(ids);
        Set<Long> foundIds = scheduleOrders.stream()
                .map(MesProScheduleOrderDO::getId)
                .collect(Collectors.toSet());
        List<Long> missingIds = ids.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();
        if (CollUtil.isNotEmpty(missingIds)) {
            throw exception0(PRO_SCHEDULE_ORDER_NOT_EXISTS.getCode(), "排产工单不存在: {}", missingIds);
        }
        return scheduleOrders;
    }

    private void insertOperationLog(MesProScheduleOrderDO before, MesProScheduleOrderDO after,
                                    String operationType, String reason) {
        scheduleOrderOperationLogMapper.insert(MesProScheduleOrderOperationLogDO.builder()
                .scheduleOrderId(before.getId())
                .scheduleOrderCode(before.getCode())
                .operationType(operationType)
                .beforeSnapshotJson(JsonUtils.toJsonString(before))
                .afterSnapshotJson(after == null ? null : JsonUtils.toJsonString(after))
                .reason(reason)
                .operatorId(SecurityFrameworkUtils.getLoginUserId())
                .operatorName(SecurityFrameworkUtils.getLoginUserNickname())
                .build());
    }

    private void insertOperationLog(MesProScheduleOrderDO before, Object afterSnapshot,
                                    String operationType, String reason) {
        scheduleOrderOperationLogMapper.insert(MesProScheduleOrderOperationLogDO.builder()
                .scheduleOrderId(before.getId())
                .scheduleOrderCode(before.getCode())
                .operationType(operationType)
                .beforeSnapshotJson(JsonUtils.toJsonString(before))
                .afterSnapshotJson(afterSnapshot == null ? null : JsonUtils.toJsonString(afterSnapshot))
                .reason(reason)
                .operatorId(SecurityFrameworkUtils.getLoginUserId())
                .operatorName(SecurityFrameworkUtils.getLoginUserNickname())
                .build());
    }

    private MesProScheduleOrderDO copyForSnapshot(MesProScheduleOrderDO source) {
        return MesProScheduleOrderDO.builder()
                .id(source.getId())
                .code(source.getCode())
                .workOrderId(source.getWorkOrderId())
                .erpWorkOrderCode(source.getErpWorkOrderCode())
                .productId(source.getProductId())
                .quantity(source.getQuantity())
                .promiseDate(source.getPromiseDate())
                .priorityNo(source.getPriorityNo())
                .status(source.getStatus())
                .diffStatus(source.getDiffStatus())
                .riskStatus(source.getRiskStatus())
                .routeStatus(source.getRouteStatus())
                .autoSchedulable(source.getAutoSchedulable())
                .routeId(source.getRouteId())
                .routeVersionId(source.getRouteVersionId())
                .routeVersion(source.getRouteVersion())
                .scheduleConfigVersion(source.getScheduleConfigVersion())
                .latestStartTime(source.getLatestStartTime())
                .plannedStartTime(source.getPlannedStartTime())
                .plannedEndTime(source.getPlannedEndTime())
                .startRiskFlag(source.getStartRiskFlag())
                .delayRiskFlag(source.getDelayRiskFlag())
                .totalQuantity(source.getTotalQuantity())
                .completedQuantity(source.getCompletedQuantity())
                .uncompletedQuantity(source.getUncompletedQuantity())
                .progressPercent(source.getProgressPercent())
                .frozen(source.getFrozen())
                .frozenTime(source.getFrozenTime())
                .frozenBy(source.getFrozenBy())
                .freezeReason(source.getFreezeReason())
                .manualFinished(source.getManualFinished())
                .manualFinishedTime(source.getManualFinishedTime())
                .manualFinishedBy(source.getManualFinishedBy())
                .manualFinishedReason(source.getManualFinishedReason())
                .sourceSnapshotJson(source.getSourceSnapshotJson())
                .routeSnapshotJson(source.getRouteSnapshotJson())
                .capacitySnapshotJson(source.getCapacitySnapshotJson())
                .remark(source.getRemark())
                .build();
    }

    private String getScheduleOrderCode(MesProScheduleOrderDO scheduleOrder) {
        return StrUtil.blankToDefault(scheduleOrder.getCode(), String.valueOf(scheduleOrder.getId()));
    }

    @Override
    public ProgressSummary calculateProcessAggregateProgressSummary(BigDecimal scheduleOrderQuantity,
                                                                    List<MesProScheduleOrderProcessDO> processes) {
        List<MesProScheduleOrderProcessDO> enabledProcesses = processes.stream()
                .filter(process -> Boolean.TRUE.equals(process.getEnabled()))
                .toList();
        if (enabledProcesses.isEmpty()) {
            BigDecimal totalQuantity = normalizeQuantity(scheduleOrderQuantity);
            return new ProgressSummary(totalQuantity, BigDecimal.ZERO.setScale(6), totalQuantity,
                    BigDecimal.ZERO.setScale(6));
        }
        BigDecimal processUnitQuantity = normalizeQuantity(scheduleOrderQuantity);
        if (processUnitQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("排产工单数量必须大于 0，无法计算整单进度");
        }
        BigDecimal totalQuantity = BigDecimal.ZERO.setScale(6);
        BigDecimal completedQuantity = BigDecimal.ZERO.setScale(6);
        BigDecimal uncompletedQuantity = BigDecimal.ZERO.setScale(6);
        for (MesProScheduleOrderProcessDO process : enabledProcesses) {
            BigDecimal reportedQuantity = normalizeQuantity(process.getReportedQuantity());
            BigDecimal effectiveCompletedQuantity = reportedQuantity.min(processUnitQuantity).setScale(6);
            totalQuantity = totalQuantity.add(processUnitQuantity).setScale(6);
            completedQuantity = completedQuantity.add(effectiveCompletedQuantity).setScale(6);
            uncompletedQuantity = uncompletedQuantity.add(processUnitQuantity.subtract(reportedQuantity).max(BigDecimal.ZERO).setScale(6)).setScale(6);
        }
        BigDecimal progressPercent = calculateProgressPercent(completedQuantity, totalQuantity);
        return new ProgressSummary(totalQuantity, completedQuantity, uncompletedQuantity, progressPercent);
    }

    @Override
    public Map<Long, ProcessProgressMetrics> calculateProcessProgressMetrics(Long scheduleOrderId,
                                                                             List<MesProScheduleOrderProcessDO> processes) {
        if (scheduleOrderId == null || CollUtil.isEmpty(processes)) {
            return Collections.emptyMap();
        }
        List<MesProFeedbackDO> feedbackList = feedbackMapper.selectProgressListByScheduleOrderId(scheduleOrderId);
        Map<Long, BigDecimal> completedByProcessId = sumFeedbackByScheduleOrderProcessId(feedbackList,
                Set.of(
                        MesProFeedbackStatusEnum.FINISHED.getStatus(),
                        MesProFeedbackStatusEnum.APPROVING.getStatus(),
                        MesProFeedbackStatusEnum.UNCHECK.getStatus()
                ));
        Map<Long, BigDecimal> approvingByProcessId = sumFeedbackByScheduleOrderProcessId(feedbackList,
                Set.of(MesProFeedbackStatusEnum.APPROVING.getStatus()));
        Map<Long, BigDecimal> pendingInspectionByProcessId = sumFeedbackByScheduleOrderProcessId(feedbackList,
                Set.of(MesProFeedbackStatusEnum.UNCHECK.getStatus()));
        Map<Long, ProcessProgressMetrics> result = new LinkedHashMap<>();
        for (MesProScheduleOrderProcessDO process : processes) {
            BigDecimal plannedQuantity = normalizeQuantity(process.getPlannedQuantity());
            BigDecimal reportedQuantity = completedByProcessId.getOrDefault(process.getId(), BigDecimal.ZERO).setScale(6);
            BigDecimal effectiveCompletedQuantity = reportedQuantity.min(plannedQuantity).setScale(6);
            BigDecimal pendingApprovalQuantity = approvingByProcessId.getOrDefault(process.getId(), BigDecimal.ZERO).setScale(6);
            BigDecimal pendingInspectionQuantity = pendingInspectionByProcessId.getOrDefault(process.getId(), BigDecimal.ZERO).setScale(6);
            BigDecimal overReportedQuantity = reportedQuantity.subtract(plannedQuantity).max(BigDecimal.ZERO).setScale(6);
            BigDecimal remainingQuantity = plannedQuantity.subtract(reportedQuantity).max(BigDecimal.ZERO).setScale(6);
            BigDecimal progressPercent = calculateProgressPercent(reportedQuantity.min(plannedQuantity), plannedQuantity);
            result.put(process.getId(), new ProcessProgressMetrics(
                    effectiveCompletedQuantity,
                    pendingApprovalQuantity,
                    pendingInspectionQuantity,
                    overReportedQuantity,
                    reportedQuantity,
                    remainingQuantity,
                    progressPercent));
        }
        return result;
    }

    private void failFastWhenAnyWorkOrderAlreadyAdmitted(List<Long> workOrderIds) {
        List<MesProScheduleOrderDO> existingScheduleOrders = scheduleOrderMapper.selectListByWorkOrderIds(workOrderIds);
        if (CollUtil.isEmpty(existingScheduleOrders)) {
            return;
        }
        throw exception(PRO_SCHEDULE_ORDER_WORK_ORDER_DUPLICATE);
    }

    private Map<Long, BigDecimal> sumFeedbackByScheduleOrderProcessId(List<MesProFeedbackDO> feedbackList,
                                                                       Set<Integer> statuses) {
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        for (MesProFeedbackDO feedback : feedbackList) {
            if (feedback.getScheduleOrderProcessId() == null) {
                continue;
            }
            if (!statuses.contains(feedback.getStatus())) {
                continue;
            }
            result.merge(feedback.getScheduleOrderProcessId(), normalizeQuantity(feedback.getFeedbackQuantity()),
                    BigDecimal::add);
        }
        return result;
    }

    private Map<DailyCompareKey, BigDecimal> buildPlannedQuantityByDate(List<MesProScheduleOrderProcessDO> processes,
                                                                        LocalDate startDate, LocalDate endDate) {
        Map<Long, MesProScheduleOrderProcessDO> processMap = processes.stream()
                .filter(process -> Boolean.TRUE.equals(process.getEnabled()))
                .collect(Collectors.toMap(MesProScheduleOrderProcessDO::getId, process -> process, (left, right) -> left,
                        LinkedHashMap::new));
        Map<DailyCompareKey, BigDecimal> plannedByKey = new LinkedHashMap<>();
        if (processMap.isEmpty()) {
            return plannedByKey;
        }
        List<MesProTaskScheduleExtDO> extList = taskScheduleExtMapper.selectListByScheduleOrderProcessIds(processMap.keySet());
        List<Long> taskIds = extList.stream()
                .map(MesProTaskScheduleExtDO::getTaskId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, MesProTaskDO> taskMap = taskIds.isEmpty() ? Collections.emptyMap()
                : taskMapper.selectListByIds(taskIds).stream()
                .collect(Collectors.toMap(MesProTaskDO::getId, task -> task, (left, right) -> left, LinkedHashMap::new));
        for (MesProTaskScheduleExtDO ext : extList) {
            MesProScheduleOrderProcessDO process = processMap.get(ext.getScheduleOrderProcessId());
            MesProTaskDO task = taskMap.get(ext.getTaskId());
            if (process == null || task == null) {
                continue;
            }
            mergeTaskPlanByDate(plannedByKey, process, task, startDate, endDate);
        }
        return plannedByKey;
    }

    private void mergeTaskPlanByDate(Map<DailyCompareKey, BigDecimal> plannedByKey,
                                     MesProScheduleOrderProcessDO process, MesProTaskDO task,
                                     LocalDate startDate, LocalDate endDate) {
        BigDecimal quantity = normalizeQuantity(task.getQuantity());
        LocalDateTime taskStart = task.getStartTime();
        LocalDateTime taskEnd = task.getEndTime();
        if (quantity.compareTo(BigDecimal.ZERO) <= 0 || taskStart == null || taskEnd == null || !taskEnd.isAfter(taskStart)) {
            throw new IllegalStateException("排产任务缺少有效的计划数量或开始结束时间，无法生成日报表计划量，taskId=" + task.getId());
        }
        long totalMinutes = java.time.Duration.between(taskStart, taskEnd).toMinutes();
        LocalDate cursor = taskStart.toLocalDate();
        LocalDate lastDate = taskEnd.minusNanos(1).toLocalDate();
        while (!cursor.isAfter(lastDate)) {
            LocalDateTime dayStart = cursor.atStartOfDay();
            LocalDateTime dayEnd = cursor.plusDays(1).atStartOfDay();
            LocalDateTime segmentStart = taskStart.isAfter(dayStart) ? taskStart : dayStart;
            LocalDateTime segmentEnd = taskEnd.isBefore(dayEnd) ? taskEnd : dayEnd;
            long segmentMinutes = java.time.Duration.between(segmentStart, segmentEnd).toMinutes();
            if (segmentMinutes > 0 && !outsideDateRange(cursor, startDate, endDate)) {
                BigDecimal segmentQuantity = quantity.multiply(BigDecimal.valueOf(segmentMinutes))
                        .divide(BigDecimal.valueOf(totalMinutes), 6, RoundingMode.HALF_UP);
                plannedByKey.merge(new DailyCompareKey(cursor, process.getId()),
                        segmentQuantity, BigDecimal::add);
            }
            cursor = cursor.plusDays(1);
        }
    }

    private MesProScheduleOrderDailyCompareDO buildDailyCompare(Long scheduleOrderId, DailyCompareKey key,
                                                               BigDecimal plannedQuantity, BigDecimal actualQuantity,
                                                               Long processId) {
        BigDecimal planned = plannedQuantity.setScale(6);
        BigDecimal actual = actualQuantity.setScale(6);
        BigDecimal diff = actual.subtract(planned).setScale(6);
        return MesProScheduleOrderDailyCompareDO.builder()
                .scheduleOrderId(scheduleOrderId)
                .scheduleOrderProcessId(key.scheduleOrderProcessId())
                .processId(processId)
                .planDate(key.planDate())
                .plannedQuantity(planned)
                .actualQuantity(actual)
                .diffQuantity(diff)
                .status(resolveDailyCompareStatus(planned, actual))
                .build();
    }

    private Integer resolveDailyCompareStatus(BigDecimal plannedQuantity, BigDecimal actualQuantity) {
        if (plannedQuantity.compareTo(BigDecimal.ZERO) <= 0 && actualQuantity.compareTo(BigDecimal.ZERO) > 0) {
            return MesProScheduleDailyCompareStatusEnum.NO_PLAN.getStatus();
        }
        if (plannedQuantity.compareTo(BigDecimal.ZERO) > 0 && actualQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return MesProScheduleDailyCompareStatusEnum.NO_FEEDBACK.getStatus();
        }
        int compareResult = actualQuantity.compareTo(plannedQuantity);
        if (compareResult == 0) {
            return MesProScheduleDailyCompareStatusEnum.NORMAL.getStatus();
        }
        return compareResult > 0 ? MesProScheduleDailyCompareStatusEnum.AHEAD.getStatus()
                : MesProScheduleDailyCompareStatusEnum.BEHIND.getStatus();
    }

    private boolean outsideDateRange(LocalDate date, LocalDate startDate, LocalDate endDate) {
        return startDate != null && date.isBefore(startDate) || endDate != null && date.isAfter(endDate);
    }

    private BigDecimal normalizeQuantity(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(6) : value.setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateProgressPercent(BigDecimal completedQuantity, BigDecimal totalQuantity) {
        if (totalQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(6);
        }
        return completedQuantity.min(totalQuantity).multiply(BigDecimal.valueOf(100))
                .divide(totalQuantity, 6, RoundingMode.HALF_UP)
                .min(new BigDecimal("100.000000"))
                .setScale(6, RoundingMode.HALF_UP);
    }

    private RecalculatedProgressSnapshot recalculateProgressSnapshot(Long scheduleOrderId,
                                                                     MesProScheduleOrderDO scheduleOrder,
                                                                     boolean clearManualFlag) {
        List<MesProScheduleOrderProcessDO> processes = scheduleOrderProcessMapper.selectListByScheduleOrderId(scheduleOrderId);
        List<MesProFeedbackDO> feedbackList = feedbackMapper.selectProgressListByScheduleOrderId(scheduleOrderId);
        Map<Long, BigDecimal> completedByProcessId = sumFeedbackByScheduleOrderProcessId(feedbackList,
                Set.of(
                        MesProFeedbackStatusEnum.FINISHED.getStatus(),
                        MesProFeedbackStatusEnum.APPROVING.getStatus(),
                        MesProFeedbackStatusEnum.UNCHECK.getStatus()
                ));
        Map<Long, BigDecimal> approvingByProcessId = sumFeedbackByScheduleOrderProcessId(feedbackList,
                Set.of(MesProFeedbackStatusEnum.APPROVING.getStatus()));
        Map<Long, BigDecimal> pendingInspectionByProcessId = sumFeedbackByScheduleOrderProcessId(feedbackList,
                Set.of(MesProFeedbackStatusEnum.UNCHECK.getStatus()));
        List<Map<String, Object>> processProgressMetrics = new ArrayList<>();

        for (MesProScheduleOrderProcessDO process : processes) {
            BigDecimal plannedQuantity = normalizeQuantity(process.getPlannedQuantity());
            BigDecimal reportedQuantity = completedByProcessId.getOrDefault(process.getId(), BigDecimal.ZERO).setScale(6);
            BigDecimal remainingQuantity = plannedQuantity.subtract(reportedQuantity).max(BigDecimal.ZERO).setScale(6);
            BigDecimal overReportedQuantity = reportedQuantity.subtract(plannedQuantity).max(BigDecimal.ZERO).setScale(6);
            process.setReportedQuantity(reportedQuantity);
            process.setRemainingQuantity(remainingQuantity);
            process.setProgressPercent(calculateProgressPercent(reportedQuantity, plannedQuantity));
            scheduleOrderProcessMapper.updateProgress(process.getId(), reportedQuantity, remainingQuantity,
                    process.getProgressPercent());
            Map<String, Object> metrics = new LinkedHashMap<>();
            metrics.put("scheduleOrderProcessId", process.getId());
            metrics.put("processId", process.getProcessId());
            metrics.put("plannedQuantity", plannedQuantity);
            metrics.put("effectiveCompletedQuantity", reportedQuantity.min(plannedQuantity).setScale(6));
            metrics.put("reportedQuantity", reportedQuantity);
            metrics.put("pendingApprovalQuantity", approvingByProcessId.getOrDefault(process.getId(), BigDecimal.ZERO).setScale(6));
            metrics.put("pendingInspectionQuantity", pendingInspectionByProcessId.getOrDefault(process.getId(), BigDecimal.ZERO).setScale(6));
            metrics.put("overReportedQuantity", overReportedQuantity);
            metrics.put("remainingQuantity", remainingQuantity);
            processProgressMetrics.add(metrics);
        }

        boolean manualFinishedLocked = Boolean.TRUE.equals(scheduleOrder.getManualFinished()) && !clearManualFlag;
        ProgressSummary summary;
        Integer progressStatus;
        BigDecimal scheduleOrderQuantity = resolveScheduleOrderQuantity(scheduleOrder);
        if (manualFinishedLocked) {
            BigDecimal totalQuantity = resolveAggregateTotalQuantity(scheduleOrderQuantity, processes);
            summary = new ProgressSummary(totalQuantity, totalQuantity, BigDecimal.ZERO.setScale(6),
                    new BigDecimal("100.000000"));
            progressStatus = MesProScheduleOrderStatusEnum.FINISHED.getStatus();
        } else {
            summary = calculateProcessAggregateProgressSummary(scheduleOrderQuantity, processes);
            progressStatus = resolveProgressStatus(scheduleOrder, summary, feedbackList);
        }
        Map<String, Object> auditPayload = new LinkedHashMap<>();
        auditPayload.put("scheduleOrderId", scheduleOrderId);
        auditPayload.put("operationType", "SYNC_PROGRESS");
        auditPayload.put("totalQuantity", summary.totalQuantity());
        auditPayload.put("completedQuantity", summary.completedQuantity());
        auditPayload.put("uncompletedQuantity", summary.uncompletedQuantity());
        auditPayload.put("progressPercent", summary.progressPercent());
        auditPayload.put("status", progressStatus);
        auditPayload.put("manualFinishedLocked", manualFinishedLocked);
        auditPayload.put("processes", processProgressMetrics);
        return new RecalculatedProgressSnapshot(summary, progressStatus, auditPayload);
    }

    private BigDecimal resolveOrderTotalQuantity(MesProScheduleOrderDO scheduleOrder) {
        BigDecimal totalQuantity = normalizeQuantity(scheduleOrder.getTotalQuantity());
        if (totalQuantity.compareTo(BigDecimal.ZERO) > 0) {
            return totalQuantity;
        }
        return normalizeQuantity(scheduleOrder.getQuantity());
    }

    private BigDecimal resolveScheduleOrderQuantity(MesProScheduleOrderDO scheduleOrder) {
        BigDecimal quantity = normalizeQuantity(scheduleOrder.getQuantity());
        if (quantity.compareTo(BigDecimal.ZERO) > 0) {
            return quantity;
        }
        return resolveOrderTotalQuantity(scheduleOrder);
    }

    private BigDecimal resolveAggregateTotalQuantity(BigDecimal scheduleOrderQuantity,
                                                     List<MesProScheduleOrderProcessDO> processes) {
        if (CollUtil.isEmpty(processes)) {
            return normalizeQuantity(scheduleOrderQuantity);
        }
        long enabledProcessCount = processes.stream()
                .filter(process -> Boolean.TRUE.equals(process.getEnabled()))
                .count();
        if (enabledProcessCount <= 0) {
            return normalizeQuantity(scheduleOrderQuantity);
        }
        return normalizeQuantity(scheduleOrderQuantity).multiply(BigDecimal.valueOf(enabledProcessCount))
                .setScale(6, RoundingMode.HALF_UP);
    }

    private record RecalculatedProgressSnapshot(ProgressSummary summary,
                                                Integer status,
                                                Map<String, Object> auditPayload) {
    }

    private MesProScheduleOrderDO buildScheduleOrder(MesProScheduleOrderCreateFromWorkOrderReqVO reqVO,
                                                     MesProWorkOrderDO workOrder,
                                                     MesProRouteDO route,
                                                     List<MesProRouteProcessDO> routeProcesses,
                                                     MesProRouteVersionDO activeRouteVersion,
                                                     String routeVersion,
                                                     ResourceSnapshotContext resourceContext,
                                                     Map<Long, Long> predecessorMap) {
        return MesProScheduleOrderDO.builder()
                .code(nextCode(workOrder.getCode()))
                .workOrderId(workOrder.getId())
                .erpWorkOrderCode(workOrder.getCode())
                .productId(workOrder.getProductId())
                .quantity(workOrder.getQuantity())
                .promiseDate(reqVO.getPromiseDate())
                .priorityNo(reqVO.getPriorityNo() == null ? 100 : reqVO.getPriorityNo())
                .status(MesProScheduleOrderStatusEnum.PREPARE.getStatus())
                .diffStatus(MesProScheduleOrderDiffStatusEnum.NONE.getStatus())
                .riskStatus(MesProScheduleOrderRiskStatusEnum.NONE.getStatus())
                .routeStatus(MesProScheduleOrderRouteStatusEnum.READY.getStatus())
                .autoSchedulable(Boolean.TRUE)
                .routeId(route.getId())
                .routeVersionId(activeRouteVersion == null ? null : activeRouteVersion.getId())
                .routeVersion(routeVersion)
                .scheduleConfigVersion(routeVersion)
                .sourceSnapshotJson(buildSourceSnapshot(workOrder))
                .routeSnapshotJson(buildRouteSnapshot(route, routeProcesses, activeRouteVersion, routeVersion,
                        predecessorMap))
                .capacitySnapshotJson(buildCapacitySnapshot(resourceContext))
                .remark(reqVO.getRemark())
                .build();
    }

    private MesProScheduleOrderDO buildMissingRouteScheduleOrder(MesProScheduleOrderCreateFromWorkOrderReqVO reqVO,
                                                                 MesProWorkOrderDO workOrder) {
        return MesProScheduleOrderDO.builder()
                .code(nextCode(workOrder.getCode()))
                .workOrderId(workOrder.getId())
                .erpWorkOrderCode(workOrder.getCode())
                .productId(workOrder.getProductId())
                .quantity(workOrder.getQuantity())
                .promiseDate(reqVO.getPromiseDate())
                .priorityNo(reqVO.getPriorityNo() == null ? 100 : reqVO.getPriorityNo())
                .status(MesProScheduleOrderStatusEnum.PREPARE.getStatus())
                .diffStatus(MesProScheduleOrderDiffStatusEnum.NONE.getStatus())
                .riskStatus(MesProScheduleOrderRiskStatusEnum.NONE.getStatus())
                .routeStatus(MesProScheduleOrderRouteStatusEnum.MISSING.getStatus())
                .autoSchedulable(Boolean.FALSE)
                .sourceSnapshotJson(buildSourceSnapshot(workOrder))
                .remark(reqVO.getRemark())
                .build();
    }

    private MesProScheduleOrderProcessDO buildProcessSnapshot(Long scheduleOrderId,
                                                              MesProWorkOrderDO workOrder,
                                                              MesProRouteProcessDO routeProcess,
                                                              MesProProcessDO process,
                                                              ResourceSnapshot resourceSnapshot,
                                                              MesProRouteVersionDO activeRouteVersion,
                                                              MesProRouteScheduleConfigDO scheduleConfig,
                                                              MesProRouteFlowProcessConfigDO scheduleRouteFlowConfig,
                                                              Long predecessorRouteProcessId) {
        Boolean processEnabled = resolveScheduleProcessEnabled(routeProcess, scheduleRouteFlowConfig);
        BigDecimal productionQuantityFactor = resolveProductionQuantityFactor(routeProcess, scheduleRouteFlowConfig);
        BigDecimal quantity = normalizeQuantity(workOrder.getQuantity())
                .multiply(productionQuantityFactor)
                .setScale(6, RoundingMode.HALF_UP);
        ResourceSnapshot configuredSnapshot = applyScheduleConfig(resourceSnapshot, scheduleConfig);
        configuredSnapshot.payload.put("productionQuantityFactor", productionQuantityFactor);
        configuredSnapshot.payload.put("plannedQuantity", quantity);
        return MesProScheduleOrderProcessDO.builder()
                .scheduleOrderId(scheduleOrderId)
                .routeProcessId(routeProcess.getId())
                .predecessorRouteProcessId(predecessorRouteProcessId)
                .rootProcessFlag(predecessorRouteProcessId == null)
                .routeVersionId(activeRouteVersion == null ? null : activeRouteVersion.getId())
                .routeScheduleConfigId(scheduleConfig == null ? null : scheduleConfig.getId())
                .processId(routeProcess.getProcessId())
                .processCode(process == null ? null : process.getCode())
                .processName(process == null ? null : process.getName())
                .sort(routeProcess.getSort())
                .enabled(processEnabled)
                .capacitySource(configuredSnapshot.capacitySource)
                .capacityMode(scheduleConfig.getCapacityMode())
                .hourlyCapacityTotal(configuredSnapshot.hourlyCapacityTotal)
                .infiniteDurationQuantityFactor(scheduleConfig.getInfiniteDurationQuantityFactor())
                .infiniteDurationBaseMinutes(scheduleConfig.getInfiniteDurationBaseMinutes())
                .shiftHours(configuredSnapshot.shiftHours)
                .shiftCapacityTotal(configuredSnapshot.shiftCapacityTotal)
                .productionQuantityFactor(productionQuantityFactor)
                .resourceSnapshotJson(JsonUtils.toJsonString(configuredSnapshot.payload))
                .plannedQuantity(quantity)
                .reportedQuantity(BigDecimal.ZERO)
                .remainingQuantity(quantity)
                .nightShiftEnabled(Boolean.TRUE.equals(scheduleConfig.getNightShiftEnabled()))
                .calendarRuleId(scheduleConfig.getCalendarRuleId())
                .keyProcessFlag(Boolean.TRUE.equals(routeProcess.getKeyFlag()))
                .bottleneckFlag(Boolean.FALSE)
                .remark(routeProcess.getRemark())
                .build();
    }

    private BigDecimal resolveProductionQuantityFactor(MesProRouteProcessDO routeProcess,
                                                       MesProRouteFlowProcessConfigDO scheduleRouteFlowConfig) {
        if (scheduleRouteFlowConfig == null || scheduleRouteFlowConfig.getProductionQuantityFactor() == null) {
            return DEFAULT_PRODUCTION_QUANTITY_FACTOR;
        }
        BigDecimal factor = scheduleRouteFlowConfig.getProductionQuantityFactor();
        if (factor.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_ROUTE_FLOW_CONFIG_PRODUCTION_QUANTITY_FACTOR_INVALID, routeProcess.getId());
        }
        return factor.setScale(6, RoundingMode.HALF_UP);
    }

    private Map<Long, MesProRouteFlowProcessConfigDO> resolveScheduleRouteFlowConfigMap(Long routeId) {
        String useType = MesProRouteFlowConfigTypeEnum.SCHEDULE.getType();
        MesProRouteFlowConfigDO flowConfig = routeFlowConfigMapper.selectByRouteIdAndUseType(routeId, useType);
        if (!MesProRouteFlowContextMatcher.isEnabledFlowContext(flowConfig, routeId, useType)) {
            return Collections.emptyMap();
        }
        List<MesProRouteFlowProcessConfigDO> configs = routeFlowProcessConfigMapper
                .selectListByRouteIdAndUseType(routeId, useType);
        if (CollUtil.isEmpty(configs)) {
            return Collections.emptyMap();
        }
        return configs.stream()
                .filter(config -> MesProRouteFlowContextMatcher.isProcessConfigOwnedBy(
                        flowConfig, config, routeId, useType))
                .filter(config -> config.getRouteProcessId() != null)
                .collect(Collectors.toMap(config -> routeProcessService.resolveFrozenRouteProcess(
                                config.getRouteProcessId(), routeId, null).getId(),
                        config -> config, (left, right) -> left, LinkedHashMap::new));
    }

    private Boolean resolveScheduleProcessEnabled(MesProRouteProcessDO routeProcess,
                                                  MesProRouteFlowProcessConfigDO scheduleRouteFlowConfig) {
        if (scheduleRouteFlowConfig == null) {
            throw exception(PRO_SCHEDULE_ORDER_ROUTE_FLOW_CONFIG_REQUIRED, routeProcess.getProcessId());
        }
        if (!Boolean.TRUE.equals(scheduleRouteFlowConfig.getEnabled())) {
            throw exception(PRO_SCHEDULE_ORDER_ROUTE_FLOW_CONFIG_DISABLED, routeProcess.getProcessId());
        }
        return Boolean.TRUE;
    }

    private String nextCode(String workOrderCode) {
        String normalizedWorkOrderCode = workOrderCode.trim().replaceAll("\\s+", "");
        String prefix = "SCH-" + normalizedWorkOrderCode + "-" + LocalDate.now().format(CODE_DATE_FORMATTER) + "-";
        String maxCode = scheduleOrderMapper.selectMaxCodeByPrefix(prefix);
        int nextSerial = 1;
        if (maxCode != null) {
            nextSerial = Integer.parseInt(maxCode.substring(prefix.length())) + 1;
        }
        return prefix + String.format("%04d", nextSerial);
    }

    private String nextRouteVersion(String routeCode) {
        String normalizedRouteCode = routeCode == null ? "ROUTE" : routeCode.trim().replaceAll("\\s+", "");
        String prefix = "ROUTE-" + normalizedRouteCode + "-" + LocalDate.now().format(CODE_DATE_FORMATTER) + "-";
        String maxVersion = scheduleOrderMapper.selectMaxRouteVersionByPrefix(prefix);
        int nextSerial = 1;
        if (maxVersion != null) {
            nextSerial = Integer.parseInt(maxVersion.substring(prefix.length())) + 1;
        }
        return prefix + String.format("%04d", nextSerial);
    }

    private Map<Long, MesProProcessDO> toProcessMap(List<MesProRouteProcessDO> routeProcesses) {
        List<Long> processIds = routeProcesses.stream().map(MesProRouteProcessDO::getProcessId)
                .filter(Objects::nonNull).distinct().toList();
        if (CollUtil.isEmpty(processIds)) {
            return Collections.emptyMap();
        }
        return processMapper.selectBatchIds(processIds).stream()
                .collect(Collectors.toMap(MesProProcessDO::getId, item -> item, (a, b) -> a, LinkedHashMap::new));
    }

    private String buildSourceSnapshot(MesProWorkOrderDO workOrder) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("workOrderId", workOrder.getId());
        payload.put("workOrderCode", workOrder.getCode());
        payload.put("workOrderName", workOrder.getName());
        payload.put("productId", workOrder.getProductId());
        payload.put("quantity", workOrder.getQuantity());
        payload.put("requestDate", workOrder.getRequestDate());
        payload.put("status", workOrder.getStatus());
        payload.put("temporaryFrozen", workOrder.getTemporaryFrozen());
        return JsonUtils.toJsonString(payload);
    }

    private String buildRouteSnapshot(MesProRouteDO route, List<MesProRouteProcessDO> routeProcesses,
                                      MesProRouteVersionDO activeRouteVersion,
                                      String routeVersion,
                                      Map<Long, Long> predecessorMap) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("routeId", route.getId());
        payload.put("routeCode", route.getCode());
        payload.put("routeName", route.getName());
        payload.put("routeVersionId", activeRouteVersion == null ? null : activeRouteVersion.getId());
        payload.put("routeVersion", routeVersion);
        List<Map<String, Object>> processes = new ArrayList<>(routeProcesses.size());
        for (MesProRouteProcessDO routeProcess : routeProcesses) {
            Map<String, Object> process = new LinkedHashMap<>();
            process.put("routeProcessId", routeProcess.getId());
            process.put("processId", routeProcess.getProcessId());
            process.put("sort", routeProcess.getSort());
            process.put("predecessorRouteProcessId", predecessorMap.get(routeProcess.getId()));
            process.put("rootProcessFlag", predecessorMap.get(routeProcess.getId()) == null);
            processes.add(process);
        }
        payload.put("processes", processes);
        return JsonUtils.toJsonString(payload);
    }

    private Map<Long, Long> buildRouteProcessPredecessorMap(
            Long routeId, List<MesProRouteProcessDO> routeProcesses) {
        Set<Long> routeProcessIds = routeProcesses.stream()
                .map(MesProRouteProcessDO::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Long> predecessorMap = new LinkedHashMap<>();
        Map<Long, Set<Long>> outgoingMap = new LinkedHashMap<>();
        routeProcessIds.forEach(id -> outgoingMap.put(id, new LinkedHashSet<>()));
        for (MesProRouteProcessFlowEdgeDO edge : routeProcessFlowEdgeMapper.selectListByRouteId(routeId)) {
            if (!routeProcessIds.contains(edge.getSourceRouteProcessId())
                    || !routeProcessIds.contains(edge.getTargetRouteProcessId())
                    || Objects.equals(edge.getSourceRouteProcessId(), edge.getTargetRouteProcessId())
                    || predecessorMap.putIfAbsent(
                            edge.getTargetRouteProcessId(), edge.getSourceRouteProcessId()) != null) {
                throw exception(PRO_ROUTE_PROCESS_FLOW_INVALID);
            }
            outgoingMap.get(edge.getSourceRouteProcessId()).add(edge.getTargetRouteProcessId());
        }
        long rootCount = routeProcessIds.stream().filter(id -> !predecessorMap.containsKey(id)).count();
        if (rootCount != 1 || predecessorMap.size() != routeProcessIds.size() - 1) {
            throw exception(PRO_ROUTE_PROCESS_FLOW_INVALID);
        }
        Long rootRouteProcessId = routeProcessIds.stream()
                .filter(id -> !predecessorMap.containsKey(id))
                .findFirst()
                .orElseThrow(() -> exception(PRO_ROUTE_PROCESS_FLOW_INVALID));
        if (reachableRouteProcessIds(rootRouteProcessId, outgoingMap).size() != routeProcessIds.size()) {
            throw exception(PRO_ROUTE_PROCESS_FLOW_INVALID);
        }
        return predecessorMap;
    }

    private Set<Long> reachableRouteProcessIds(Long rootRouteProcessId, Map<Long, Set<Long>> outgoingMap) {
        Set<Long> visited = new LinkedHashSet<>();
        Queue<Long> queue = new ArrayDeque<>();
        visited.add(rootRouteProcessId);
        queue.add(rootRouteProcessId);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            for (Long target : outgoingMap.getOrDefault(current, Set.of())) {
                if (visited.add(target)) {
                    queue.add(target);
                }
            }
        }
        return visited;
    }

    private Map<Long, MesProRouteScheduleConfigDO> buildScheduleConfigMap(MesProRouteVersionDO activeRouteVersion) {
        if (activeRouteVersion == null) {
            return Collections.emptyMap();
        }
        return routeScheduleConfigMapper.selectListByRouteVersionId(activeRouteVersion.getId()).stream()
                .filter(item -> item.getRouteProcessId() != null)
                .collect(Collectors.toMap(item -> routeProcessService.resolveFrozenRouteProcess(
                                item.getRouteProcessId(), activeRouteVersion.getRouteId(), null).getId(),
                        item -> item,
                        (left, right) -> left, LinkedHashMap::new));
    }

    private ResourceSnapshot applyScheduleConfig(ResourceSnapshot resourceSnapshot,
                                                 MesProRouteScheduleConfigDO scheduleConfig) {
        if (scheduleConfig == null) {
            Long processId = resourceSnapshot.payload.get("processId") instanceof Long
                    ? (Long) resourceSnapshot.payload.get("processId") : null;
            throw exception(PRO_SCHEDULE_ORDER_ROUTE_SCHEDULE_CONFIG_REQUIRED, processId);
        }
        Map<String, Object> payload = new LinkedHashMap<>(resourceSnapshot.payload);
        payload.put("routeScheduleConfigId", scheduleConfig.getId());
        payload.put("capacityMode", scheduleConfig.getCapacityMode());
        payload.put("configVersion", scheduleConfig.getConfigVersion());
        payload.put("nightShiftEnabled", Boolean.TRUE.equals(scheduleConfig.getNightShiftEnabled()));
        payload.put("calendarRuleId", scheduleConfig.getCalendarRuleId());
        if (MesProScheduleCapacityModeEnum.isManualOverrideLike(scheduleConfig.getCapacityMode())) {
            BigDecimal hourlyCapacity = nullToZero(scheduleConfig.getHourlyCapacity());
            BigDecimal shiftHours = resourceSnapshot.shiftHours;
            BigDecimal shiftCapacity = shiftHours == null ? null : hourlyCapacity.multiply(shiftHours);
            payload.put("hourlyCapacityTotal", hourlyCapacity);
            payload.put("shiftCapacityTotal", shiftCapacity);
            return new ResourceSnapshot(resourceSnapshot.capacitySource, hourlyCapacity, shiftHours,
                    shiftCapacity, resourceSnapshot.resourceStatus, resourceSnapshot.resourceStatusReason, payload);
        }
        if (MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode().equals(scheduleConfig.getCapacityMode())) {
            payload.put("infiniteDurationQuantityFactor", scheduleConfig.getInfiniteDurationQuantityFactor());
            payload.put("infiniteDurationBaseMinutes", scheduleConfig.getInfiniteDurationBaseMinutes());
            return new ResourceSnapshot(resourceSnapshot.capacitySource, resourceSnapshot.hourlyCapacityTotal,
                    resourceSnapshot.shiftHours, resourceSnapshot.shiftCapacityTotal,
                    resourceSnapshot.resourceStatus, resourceSnapshot.resourceStatusReason, payload);
        }
        if (MesProScheduleCapacityModeEnum.RESOURCE_CALCULATED.getMode().equals(scheduleConfig.getCapacityMode())) {
            payload.put("hourlyCapacityTotal", resourceSnapshot.hourlyCapacityTotal);
            payload.put("shiftCapacityTotal", resourceSnapshot.shiftCapacityTotal);
            return new ResourceSnapshot(resourceSnapshot.capacitySource, resourceSnapshot.hourlyCapacityTotal,
                    resourceSnapshot.shiftHours, resourceSnapshot.shiftCapacityTotal,
                    resourceSnapshot.resourceStatus, resourceSnapshot.resourceStatusReason, payload);
        }
        throw new IllegalStateException("未知排产产能模式，routeScheduleConfigId=" + scheduleConfig.getId());
    }

    private BigDecimal requireSnapshotShiftHours(ResourceSnapshot resourceSnapshot) {
        if (resourceSnapshot.shiftHours != null && resourceSnapshot.shiftHours.compareTo(BigDecimal.ZERO) > 0) {
            return resourceSnapshot.shiftHours;
        }
        Object routeProcessId = resourceSnapshot.payload.get("routeProcessId");
        Long routeProcessIdValue = routeProcessId instanceof Number ? ((Number) routeProcessId).longValue() : null;
        throw exception(PRO_SCHEDULE_ORDER_SHIFT_HOURS_REQUIRED, routeProcessIdValue, null);
    }

    private ResourceSnapshotContext buildResourceSnapshotContext(List<MesProRouteProcessDO> routeProcesses) {
        return buildResourceSnapshotContext(routeProcesses, true);
    }

    private ResourceSnapshotContext buildResourceSnapshotContext(List<MesProRouteProcessDO> routeProcesses,
                                                                boolean failFastOnMissingResource) {
        List<Long> processIds = routeProcesses.stream().map(MesProRouteProcessDO::getProcessId)
                .filter(Objects::nonNull).distinct().toList();
        if (processIds.isEmpty()) {
            return ResourceSnapshotContext.empty();
        }
        Map<Long, Long> processIdentityMap = routeProcessService.getProcessIdentityMap(processIds);
        Set<Long> boundWorkstationIds = routeProcesses.stream()
                .map(MesProRouteProcessDO::getWorkstationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<MesMdWorkstationDO> boundWorkstations = boundWorkstationIds.isEmpty()
                ? Collections.emptyList() : workstationMapper.selectBatchIds(boundWorkstationIds);
        List<MesMdWorkstationDO> processWorkstations = workstationMapper.selectListByProcessIds(
                processIds, CommonStatusEnum.ENABLE.getStatus());
        Map<Long, MesMdWorkstationDO> workstationById = new LinkedHashMap<>();
        boundWorkstations.stream().filter(workstation -> workstation.getId() != null)
                .forEach(workstation -> workstationById.putIfAbsent(workstation.getId(), workstation));
        processWorkstations.stream().filter(workstation -> workstation.getId() != null)
                .forEach(workstation -> workstationById.putIfAbsent(workstation.getId(), workstation));
        List<MesMdWorkstationDO> workstations = new ArrayList<>(workstationById.values());
        workstations.forEach(workstation -> {
            Long currentProcessId = processIdentityMap.get(workstation.getProcessId());
            if (currentProcessId != null) {
                workstation.setProcessId(currentProcessId);
            }
        });
        List<Long> workstationIds = workstations.stream().map(MesMdWorkstationDO::getId).filter(Objects::nonNull).toList();
        List<MesMdWorkstationMachineDO> machineBindings = CollUtil.isEmpty(workstationIds)
                ? Collections.emptyList()
                : workstationMachineMapper.selectListByWorkstationIds(workstationIds);
        List<MesMdWorkstationWorkerDO> workerBindings = CollUtil.isEmpty(workstationIds)
                ? Collections.emptyList()
                : workstationWorkerMapper.selectListByWorkstationIds(workstationIds);
        Map<Long, List<MesMdWorkstationMachineDO>> machinesByWorkstationId = machineBindings.stream()
                .collect(Collectors.groupingBy(MesMdWorkstationMachineDO::getWorkstationId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, List<MesMdWorkstationWorkerDO>> workersByWorkstationId = workerBindings.stream()
                .collect(Collectors.groupingBy(MesMdWorkstationWorkerDO::getWorkstationId, LinkedHashMap::new, Collectors.toList()));
        Set<Long> machineryIds = machineBindings.stream().map(MesMdWorkstationMachineDO::getMachineryId)
                .filter(Objects::nonNull).collect(Collectors.toCollection(HashSet::new));
        Map<Long, MesDvMachineryDO> machineryMap = machineryIds.isEmpty() ? Collections.emptyMap()
                : machineryMapper.selectBatchIds(machineryIds).stream()
                .collect(Collectors.toMap(MesDvMachineryDO::getId, item -> item, (a, b) -> a, LinkedHashMap::new));
        List<MesDvMachineryProcessDO> machineryProcesses =
                machineryProcessMapper.selectListByMachineryIds(machineryIds).stream()
                        .filter(row -> processIdentityMap.containsKey(row.getProcessId()))
                        .peek(row -> row.setProcessId(processIdentityMap.get(row.getProcessId())))
                        .toList();
        Map<String, MesDvMachineryProcessDO> machineryProcessMap =
                buildMachineryProcessMap(machineryProcesses);

        Map<Long, ResourceSnapshot> snapshotMap = new LinkedHashMap<>();
        for (MesProRouteProcessDO routeProcess : routeProcesses) {
            List<MesMdWorkstationDO> routeBoundWorkstations = Collections.emptyList();
            if (routeProcess.getWorkstationId() != null) {
                MesMdWorkstationDO boundWorkstation = workstationById.get(routeProcess.getWorkstationId());
                if (boundWorkstation != null) {
                    if (!Objects.equals(boundWorkstation.getProcessId(), routeProcess.getProcessId())) {
                        throw exception(PRO_WORKSTATION_PROCESS_MISMATCH);
                    }
                    routeBoundWorkstations = List.of(boundWorkstation);
                }
            }
            snapshotMap.put(routeProcess.getId(), buildResourceSnapshot(routeProcess,
                    routeBoundWorkstations,
                    machinesByWorkstationId, workersByWorkstationId, machineryMap, machineryProcessMap,
                    failFastOnMissingResource));
        }
        return new ResourceSnapshotContext(snapshotMap,
                buildMaxShiftCapacityByProcessId(processWorkstations, machinesByWorkstationId,
                        workersByWorkstationId, machineryMap, machineryProcessMap));
    }

    private Map<Long, BigDecimal> buildMaxShiftCapacityByProcessId(List<MesMdWorkstationDO> processWorkstations,
                                                                   Map<Long, List<MesMdWorkstationMachineDO>> machinesByWorkstationId,
                                                                   Map<Long, List<MesMdWorkstationWorkerDO>> workersByWorkstationId,
                                                                   Map<Long, MesDvMachineryDO> machineryMap,
                                                                   Map<String, MesDvMachineryProcessDO> machineryProcessMap) {
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        for (MesMdWorkstationDO workstation : processWorkstations) {
            if (workstation.getId() == null || workstation.getProcessId() == null) {
                continue;
            }
            MesProRouteProcessDO syntheticRouteProcess = MesProRouteProcessDO.builder()
                    .processId(workstation.getProcessId())
                    .workstationId(workstation.getId())
                    .build();
            BigDecimal shiftCapacity = normalizeQuantity(buildResourceSnapshot(syntheticRouteProcess,
                    List.of(workstation), machinesByWorkstationId, workersByWorkstationId,
                    machineryMap, machineryProcessMap, false).shiftCapacityTotal);
            if (shiftCapacity.compareTo(BigDecimal.ZERO) > 0) {
                result.merge(workstation.getProcessId(), shiftCapacity, BigDecimal::max);
            }
        }
        return result;
    }

    private ResourceSnapshot buildResourceSnapshot(MesProRouteProcessDO routeProcess,
                                                   List<MesMdWorkstationDO> workstations,
                                                   Map<Long, List<MesMdWorkstationMachineDO>> machinesByWorkstationId,
                                                   Map<Long, List<MesMdWorkstationWorkerDO>> workersByWorkstationId,
                                                   Map<Long, MesDvMachineryDO> machineryMap,
                                                   Map<String, MesDvMachineryProcessDO> machineryProcessMap,
                                                   boolean failFastOnMissingResource) {
        if (workstations.isEmpty()) {
            return ResourceSnapshot.unconfigured(routeProcess.getId());
        }
        List<Map<String, Object>> resources = new ArrayList<>();
        BigDecimal hourlyTotal = BigDecimal.ZERO;
        BigDecimal shiftHours = null;
        boolean hasMachine = false;
        boolean hasWorker = false;
        boolean hasShiftHoursMissing = false;
        boolean hasWorkerQuantityMissing = false;
        boolean hasWorkerCapacityMissing = false;
        boolean hasMachineCapacityMissing = false;
        boolean hasMachineQuantityMissing = false;
        for (MesMdWorkstationDO workstation : workstations) {
            List<MesMdWorkstationMachineDO> machines =
                    machinesByWorkstationId.getOrDefault(workstation.getId(), Collections.emptyList());
            if (!machines.isEmpty()) {
                hasMachine = true;
                BigDecimal workstationShiftHours = resolveSnapshotShiftHours(workstation, routeProcess,
                        failFastOnMissingResource);
                if (workstationShiftHours == null) {
                    hasShiftHoursMissing = true;
                } else if (shiftHours == null) {
                    shiftHours = workstationShiftHours;
                }
                for (MesMdWorkstationMachineDO machine : machines) {
                    MesDvMachineryDO machinery = machineryMap.get(machine.getMachineryId());
                    MesDvMachineryProcessDO machineryProcess = machineryProcessMap.get(
                            buildMachineryProcessKey(machine.getMachineryId(), routeProcess.getProcessId()));
                    BigDecimal standardHourly = machineryProcess == null ? null : machineryProcess.getStandardHourlyCapacity();
                    if (!positiveResourceValue(standardHourly)) {
                        hasMachineCapacityMissing = true;
                    }
                    if (machine.getQuantity() == null || machine.getQuantity() <= 0) {
                        hasMachineQuantityMissing = true;
                    }
                    BigDecimal quantity = BigDecimal.valueOf(machine.getQuantity() == null ? 0 : machine.getQuantity());
                    BigDecimal hourly = nullToZero(standardHourly).multiply(quantity);
                    hourlyTotal = hourlyTotal.add(hourly);
                    Map<String, Object> resource = baseResourcePayload("MACHINE", workstation);
                    resource.put("workstationMachineId", machine.getId());
                    resource.put("machineryId", machine.getMachineryId());
                    resource.put("machineryCode", machinery == null ? null : machinery.getCode());
                    resource.put("machineryName", machinery == null ? null : machinery.getName());
                    resource.put("quantity", machine.getQuantity());
                    resource.put("standardHourlyCapacity", standardHourly);
                    resource.put("shiftHours", workstationShiftHours);
                    resource.put("hourlyCapacity", hourly);
                    resources.add(resource);
                }
                continue;
            }
            hasWorker = true;
            BigDecimal workstationShiftHours = resolveSnapshotShiftHours(workstation, routeProcess,
                    failFastOnMissingResource);
            if (workstationShiftHours == null) {
                hasShiftHoursMissing = true;
            } else {
                shiftHours = workstationShiftHours;
            }
            MesMdWorkstationWorkerDO worker = workersByWorkstationId.getOrDefault(workstation.getId(), Collections.emptyList())
                    .stream()
                    .min(Comparator.comparing(MesMdWorkstationWorkerDO::getId, Comparator.nullsLast(Long::compareTo)))
                    .orElse(null);
            Integer workerQuantity = resolveSnapshotWorkerQuantity(worker);
            if (!positiveResourceValue(workstation.getSingleStandardHourlyCapacity())) {
                hasWorkerCapacityMissing = true;
            }
            BigDecimal singleHourly = nullToZero(workstation.getSingleStandardHourlyCapacity());
            BigDecimal hourly = singleHourly;
            hourlyTotal = hourlyTotal.add(hourly);
            Map<String, Object> resource = baseResourcePayload("WORKER", workstation);
            resource.put("workstationWorkerId", worker == null ? null : worker.getId());
            resource.put("workerQuantity", workerQuantity);
            resource.put("singleStandardHourlyCapacity", singleHourly);
            resource.put("shiftHours", workstationShiftHours);
            resource.put("hourlyCapacity", hourly);
            resources.add(resource);
        }
        if (shiftHours == null || shiftHours.compareTo(BigDecimal.ZERO) <= 0) {
            if (failFastOnMissingResource) {
                throw exception(PRO_SCHEDULE_ORDER_SHIFT_HOURS_REQUIRED, routeProcess.getId(), null);
            }
            hasShiftHoursMissing = true;
        }
        String capacitySource = hasMachine ? CAPACITY_SOURCE_MACHINE : hasWorker ? CAPACITY_SOURCE_WORKER : CAPACITY_SOURCE_UNCONFIGURED;
        BigDecimal shiftCapacity = shiftHours == null ? BigDecimal.ZERO : hourlyTotal.multiply(shiftHours);
        String resourceStatusReason = resolveSnapshotResourceStatusReason(capacitySource, hasShiftHoursMissing,
                hasWorkerCapacityMissing, hasMachineCapacityMissing, hasMachineQuantityMissing);
        String resourceStatus = RESOURCE_REASON_NORMAL.equals(resourceStatusReason)
                ? RESOURCE_STATUS_NORMAL : RESOURCE_STATUS_CAPACITY_MISSING;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("routeProcessId", routeProcess.getId());
        payload.put("processId", routeProcess.getProcessId());
        payload.put("capacitySource", capacitySource);
        payload.put("hourlyCapacityTotal", hourlyTotal);
        payload.put("shiftHours", shiftHours);
        payload.put("shiftCapacityTotal", shiftCapacity);
        payload.put("resourceStatus", resourceStatus);
        payload.put("resourceStatusReason", resourceStatusReason);
        payload.put("resources", resources);
        return new ResourceSnapshot(capacitySource, hourlyTotal, shiftHours, shiftCapacity,
                resourceStatus, resourceStatusReason, payload);
    }

    private BigDecimal resolveSnapshotShiftHours(MesMdWorkstationDO workstation, MesProRouteProcessDO routeProcess,
                                                 boolean failFastOnMissingResource) {
        BigDecimal shiftHours = workstation.getShiftHours();
        if (shiftHours == null || shiftHours.compareTo(BigDecimal.ZERO) <= 0) {
            if (failFastOnMissingResource) {
                throw exception(PRO_SCHEDULE_ORDER_SHIFT_HOURS_REQUIRED, routeProcess.getId(), workstation.getId());
            }
            return null;
        }
        return shiftHours;
    }

    private Integer resolveSnapshotWorkerQuantity(MesMdWorkstationWorkerDO worker) {
        if (worker == null || worker.getQuantity() == null || worker.getQuantity() <= 0) {
            return 0;
        }
        return worker.getQuantity();
    }

    private String resolveSnapshotResourceStatusReason(String capacitySource,
                                                       boolean hasShiftHoursMissing,
                                                       boolean hasWorkerCapacityMissing,
                                                       boolean hasMachineCapacityMissing,
                                                       boolean hasMachineQuantityMissing) {
        if (CAPACITY_SOURCE_UNCONFIGURED.equals(capacitySource)) {
            return RESOURCE_REASON_UNCONFIGURED;
        }
        if (hasShiftHoursMissing) {
            return RESOURCE_REASON_SHIFT_HOURS_MISSING;
        }
        if (hasWorkerCapacityMissing) {
            return RESOURCE_REASON_WORKER_CAPACITY_MISSING;
        }
        if (hasMachineCapacityMissing) {
            return RESOURCE_REASON_MACHINE_CAPACITY_MISSING;
        }
        if (hasMachineQuantityMissing) {
            return RESOURCE_REASON_MACHINE_QUANTITY_MISSING;
        }
        return RESOURCE_REASON_NORMAL;
    }

    private BigDecimal requireShiftHours(MesMdWorkstationDO workstation, MesProRouteProcessDO routeProcess) {
        BigDecimal shiftHours = workstation.getShiftHours();
        if (shiftHours == null || shiftHours.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_SCHEDULE_ORDER_SHIFT_HOURS_REQUIRED, routeProcess.getId(), workstation.getId());
        }
        return shiftHours;
    }

    private MesProRouteVersionDO validateScheduleAdmissionRequirements(MesProRouteDO route, List<MesProRouteProcessDO> routeProcesses) {
        Map<Long, MesProRouteFlowProcessConfigDO> scheduleRouteFlowConfigMap = resolveScheduleRouteFlowConfigMap(route.getId());
        for (MesProRouteProcessDO routeProcess : routeProcesses) {
            resolveScheduleProcessEnabled(routeProcess, scheduleRouteFlowConfigMap.get(routeProcess.getId()));
        }
        MesProRouteVersionDO activeRouteVersion = routeVersionMapper.selectActiveByRouteId(route.getId());
        if (activeRouteVersion == null) {
            throw exception(PRO_ROUTE_VERSION_ACTIVE_NOT_EXISTS, route.getId());
        }
        Map<Long, MesProRouteScheduleConfigDO> scheduleConfigMap = buildScheduleConfigMap(activeRouteVersion);
        for (MesProRouteProcessDO routeProcess : routeProcesses) {
            if (!scheduleConfigMap.containsKey(routeProcess.getId())) {
                throw exception(PRO_SCHEDULE_ORDER_ROUTE_SCHEDULE_CONFIG_REQUIRED, routeProcess.getProcessId());
            }
        }
        return activeRouteVersion;
    }

    private void validateResourceCalculatedSnapshots(List<MesProRouteProcessDO> routeProcesses,
                                                    Map<Long, MesProRouteScheduleConfigDO> scheduleConfigMap,
                                                    ResourceSnapshotContext resourceContext) {
        for (MesProRouteProcessDO routeProcess : routeProcesses) {
            MesProRouteScheduleConfigDO scheduleConfig = scheduleConfigMap.get(routeProcess.getId());
            if (!isResourceCalculated(scheduleConfig)) {
                continue;
            }
            ResourceSnapshot snapshot = resourceContext.snapshotByRouteProcessId.get(routeProcess.getId());
            if (!hasPositiveResourceCapacity(snapshot)) {
                throw exception(PRO_SCHEDULE_ORDER_RESOURCE_CAPACITY_REQUIRED, routeProcess.getId());
            }
        }
    }

    private boolean isResourceCalculated(MesProRouteScheduleConfigDO scheduleConfig) {
        return scheduleConfig != null
                && MesProScheduleCapacityModeEnum.RESOURCE_CALCULATED.getMode().equals(scheduleConfig.getCapacityMode());
    }

    private boolean hasPositiveResourceCapacity(ResourceSnapshot snapshot) {
        return snapshot != null
                && !CAPACITY_SOURCE_UNCONFIGURED.equals(snapshot.capacitySource)
                && snapshot.hourlyCapacityTotal != null
                && snapshot.hourlyCapacityTotal.compareTo(BigDecimal.ZERO) > 0;
    }

    private Map<String, Object> baseResourcePayload(String resourceType, MesMdWorkstationDO workstation) {
        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("resourceType", resourceType);
        resource.put("workstationId", workstation.getId());
        resource.put("workstationCode", workstation.getCode());
        resource.put("workstationName", workstation.getName());
        return resource;
    }

    private Map<String, MesDvMachineryProcessDO> buildMachineryProcessMap(List<MesDvMachineryProcessDO> rows) {
        Map<String, MesDvMachineryProcessDO> result = new LinkedHashMap<>();
        for (MesDvMachineryProcessDO row : rows) {
            result.putIfAbsent(buildMachineryProcessKey(row.getMachineryId(), row.getProcessId()), row);
        }
        return result;
    }

    private String buildMachineryProcessKey(Long machineryId, Long processId) {
        return machineryId + ":" + processId;
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean positiveResourceValue(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private String buildCapacitySnapshot(ResourceSnapshotContext context) {
        Map<String, Object> payload = new LinkedHashMap<>();
        BigDecimal hourlyTotal = BigDecimal.ZERO;
        BigDecimal shiftCapacityTotal = BigDecimal.ZERO;
        List<Map<String, Object>> processes = new ArrayList<>();
        for (ResourceSnapshot snapshot : context.snapshotByRouteProcessId.values()) {
            hourlyTotal = hourlyTotal.add(snapshot.hourlyCapacityTotal);
            shiftCapacityTotal = shiftCapacityTotal.add(snapshot.shiftCapacityTotal);
            processes.add(snapshot.payload);
        }
        payload.put("hourlyCapacityTotal", hourlyTotal);
        payload.put("shiftCapacityTotal", shiftCapacityTotal);
        payload.put("processes", processes);
        return JsonUtils.toJsonString(payload);
    }

    private record ResourceSnapshotContext(Map<Long, ResourceSnapshot> snapshotByRouteProcessId,
                                           Map<Long, BigDecimal> maxShiftCapacityByProcessId) {
        private static ResourceSnapshotContext empty() {
            return new ResourceSnapshotContext(new HashMap<>(), Collections.emptyMap());
        }
    }

    private record DailyCompareKey(LocalDate planDate, Long scheduleOrderProcessId) {
    }

    private record ResourceSnapshot(String capacitySource, BigDecimal hourlyCapacityTotal, BigDecimal shiftHours,
                                    BigDecimal shiftCapacityTotal, String resourceStatus,
                                    String resourceStatusReason, Map<String, Object> payload) {
        private static ResourceSnapshot unconfigured(Long routeProcessId) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("routeProcessId", routeProcessId);
            payload.put("capacitySource", CAPACITY_SOURCE_UNCONFIGURED);
            payload.put("hourlyCapacityTotal", BigDecimal.ZERO);
            payload.put("shiftHours", null);
            payload.put("shiftCapacityTotal", BigDecimal.ZERO);
            payload.put("resourceStatus", RESOURCE_STATUS_CAPACITY_MISSING);
            payload.put("resourceStatusReason", RESOURCE_REASON_UNCONFIGURED);
            payload.put("resources", List.of());
            return new ResourceSnapshot(CAPACITY_SOURCE_UNCONFIGURED, BigDecimal.ZERO, null,
                    BigDecimal.ZERO, RESOURCE_STATUS_CAPACITY_MISSING, RESOURCE_REASON_UNCONFIGURED, payload);
        }
    }

}
