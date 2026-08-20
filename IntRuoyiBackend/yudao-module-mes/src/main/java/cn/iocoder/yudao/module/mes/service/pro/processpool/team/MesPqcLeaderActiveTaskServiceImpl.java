package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PQC_TASK_IDENTITY_MISMATCH;

@Service
public class MesPqcLeaderActiveTaskServiceImpl implements MesPqcLeaderActiveTaskService {

    private static final Set<String> ACTIVE_TASK_STATUSES = Set.of(
            MesPqcInspectionTaskDO.TASK_STATUS_PENDING,
            MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED);
    private static final Set<String> LOCKED_QA_VERSION_STATUSES = Set.of("PUBLISHED", "RETIRED");
    private static final Map<String, Integer> RULE_SORTS = Map.of(
            "FIRST", 1,
            "PATROL_AM", 2,
            "PATROL_PM", 3,
            "FINAL", 4);

    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesPqcInspectionTaskMapper taskMapper;
    private final MesProWorkOrderMapper workOrderMapper;
    private final MesProRouteMapper routeMapper;
    private final MesProRouteVersionMapper routeVersionMapper;
    private final MesQaInspectionRegulationMapper regulationMapper;
    private final MesQaInspectionRegulationVersionMapper regulationVersionMapper;
    private final MesQaInspectionRegulationProcessMapper regulationProcessMapper;

    public MesPqcLeaderActiveTaskServiceImpl(
            MesProcessPoolActiveOrderMapper activeOrderMapper,
            MesPqcInspectionTaskMapper taskMapper,
            MesProWorkOrderMapper workOrderMapper,
            MesProRouteMapper routeMapper,
            MesProRouteVersionMapper routeVersionMapper,
            MesQaInspectionRegulationMapper regulationMapper,
            MesQaInspectionRegulationVersionMapper regulationVersionMapper,
            MesQaInspectionRegulationProcessMapper regulationProcessMapper) {
        this.activeOrderMapper = activeOrderMapper;
        this.taskMapper = taskMapper;
        this.workOrderMapper = workOrderMapper;
        this.routeMapper = routeMapper;
        this.routeVersionMapper = routeVersionMapper;
        this.regulationMapper = regulationMapper;
        this.regulationVersionMapper = regulationVersionMapper;
        this.regulationProcessMapper = regulationProcessMapper;
    }

    @Override
    public List<MesPqcLeaderActiveTaskRow> listActiveTasks() {
        List<MesProcessPoolActiveOrderDO> activeOrders = activeOrderMapper.selectActiveList();
        if (activeOrders.isEmpty()) {
            return List.of();
        }
        if (activeOrders.stream().anyMatch(order -> order == null || order.getId() == null)) {
            throw identityMismatch("activeOrderId缺失");
        }
        List<Long> activeOrderIds = activeOrders.stream()
                .map(MesProcessPoolActiveOrderDO::getId)
                .toList();
        List<MesPqcInspectionTaskDO> tasks = taskMapper.selectListByActiveOrderIdsAndStatuses(
                activeOrderIds, ACTIVE_TASK_STATUSES);
        if (tasks == null) {
            throw identityMismatch("PQC任务查询未返回正式结果");
        }
        if (tasks.isEmpty()) {
            return List.of();
        }

        Map<Long, MesProcessPoolActiveOrderDO> activeOrdersById = mapById(
                activeOrders, MesProcessPoolActiveOrderDO::getId, "activeOrder");
        List<MesPqcInspectionTaskDO> currentTasks = tasks.stream()
                .filter(task -> matchesCurrentActiveOrderSnapshot(task, activeOrdersById))
                .toList();
        if (currentTasks.isEmpty()) {
            return List.of();
        }
        currentTasks.forEach(task -> requireCurrentTaskComplete(task, activeOrdersById));

        Map<Long, MesProWorkOrderDO> workOrdersById = mapById(
                workOrderMapper.selectBatchIds(distinctIds(currentTasks, MesPqcInspectionTaskDO::getWorkOrderId)),
                MesProWorkOrderDO::getId, "workOrder");
        Map<Long, MesProRouteDO> routesById = mapById(
                routeMapper.selectBatchIds(distinctIds(currentTasks, MesPqcInspectionTaskDO::getRouteId)),
                MesProRouteDO::getId, "route");
        Map<Long, MesProRouteVersionDO> routeVersionsById = mapById(
                routeVersionMapper.selectBatchIds(distinctIds(currentTasks, MesPqcInspectionTaskDO::getRouteVersionId)),
                MesProRouteVersionDO::getId, "routeVersion");
        Map<Long, MesQaInspectionRegulationDO> regulationsById = mapById(
                regulationMapper.selectBatchIds(distinctIds(currentTasks, task ->
                        activeOrdersById.get(task.getActiveOrderId()).getQaRegulationId())),
                MesQaInspectionRegulationDO::getId, "qaRegulation");
        Map<Long, MesQaInspectionRegulationVersionDO> qaVersionsById = mapById(
                regulationVersionMapper.selectBatchIds(
                        distinctIds(currentTasks, MesPqcInspectionTaskDO::getRegulationVersionId)),
                MesQaInspectionRegulationVersionDO::getId, "qaVersion");
        Map<Long, MesQaInspectionRegulationProcessDO> qaProcessesById = mapById(
                regulationProcessMapper.selectBatchIds(distinctIds(currentTasks, MesPqcInspectionTaskDO::getQaProcessId)),
                MesQaInspectionRegulationProcessDO::getId, "qaProcess");

        return currentTasks.stream()
                .map(task -> toRow(task, activeOrdersById, workOrdersById, routesById,
                        routeVersionsById, regulationsById, qaVersionsById, qaProcessesById))
                .sorted(Comparator
                        .comparing(MesPqcLeaderActiveTaskRow::getBusinessDate)
                        .thenComparing(row -> RULE_SORTS.get(row.getInspectionRuleKey()))
                        .thenComparing(MesPqcLeaderActiveTaskRow::getRoundNo)
                        .thenComparing(MesPqcLeaderActiveTaskRow::getPqcTaskId))
                .toList();
    }

    private MesPqcLeaderActiveTaskRow toRow(
            MesPqcInspectionTaskDO task,
            Map<Long, MesProcessPoolActiveOrderDO> activeOrdersById,
            Map<Long, MesProWorkOrderDO> workOrdersById,
            Map<Long, MesProRouteDO> routesById,
            Map<Long, MesProRouteVersionDO> routeVersionsById,
            Map<Long, MesQaInspectionRegulationDO> regulationsById,
            Map<Long, MesQaInspectionRegulationVersionDO> qaVersionsById,
            Map<Long, MesQaInspectionRegulationProcessDO> qaProcessesById) {
        MesProcessPoolActiveOrderDO activeOrder = activeOrdersById.get(task.getActiveOrderId());
        MesProWorkOrderDO workOrder = requireRow(workOrdersById, task.getWorkOrderId(), task, "workOrder");
        MesProRouteDO route = requireRow(routesById, task.getRouteId(), task, "route");
        MesProRouteVersionDO routeVersion = requireRow(
                routeVersionsById, task.getRouteVersionId(), task, "routeVersion");
        MesQaInspectionRegulationDO regulation = requireRow(
                regulationsById, activeOrder.getQaRegulationId(), task, "qaRegulation");
        MesQaInspectionRegulationVersionDO qaVersion = requireRow(
                qaVersionsById, task.getRegulationVersionId(), task, "qaVersion");
        MesQaInspectionRegulationProcessDO qaProcess = requireRow(
                qaProcessesById, task.getQaProcessId(), task, "qaProcess");

        requireText(workOrder.getCode(), task, "workOrderCode");
        requireText(route.getName(), task, "routeName");
        requireText(routeVersion.getVersionNo(), task, "routeVersionNo");
        requireText(regulation.getRegulationCode(), task, "qaRegulationCode");
        requireText(regulation.getRegulationName(), task, "qaRegulationName");
        requireText(qaVersion.getVersionNo(), task, "qaVersionNo");
        requireText(qaProcess.getProcessName(), task, "qaProcessName");
        if (!Objects.equals(routeVersion.getRouteId(), task.getRouteId())
                || !Objects.equals(qaVersion.getRegulationId(), regulation.getId())
                || !LOCKED_QA_VERSION_STATUSES.contains(qaVersion.getLifecycleStatus())
                || !Objects.equals(qaProcess.getRegulationVersionId(), qaVersion.getId())) {
            throw identityMismatch(task, "冻结版本归属不一致");
        }

        return new MesPqcLeaderActiveTaskRow()
                .setPqcTaskId(task.getId())
                .setTaskStatus(task.getTaskStatus())
                .setActiveOrderId(task.getActiveOrderId())
                .setWorkOrderId(workOrder.getId())
                .setWorkOrderCode(workOrder.getCode())
                .setWorkOrderName(workOrder.getName())
                .setQaRegulationId(regulation.getId())
                .setQaRegulationCode(regulation.getRegulationCode())
                .setQaRegulationName(regulation.getRegulationName())
                .setQaVersionId(qaVersion.getId())
                .setQaVersionNo(qaVersion.getVersionNo())
                .setQaProcessId(qaProcess.getId())
                .setQaProcessCode(qaProcess.getProcessCode())
                .setQaProcessName(qaProcess.getProcessName())
                .setRouteId(route.getId())
                .setRouteCode(route.getCode())
                .setRouteName(route.getName())
                .setRouteVersionId(routeVersion.getId())
                .setRouteVersionNo(routeVersion.getVersionNo())
                .setInspectionRuleKey(task.getInspectionRuleKey())
                .setInspectionType(task.getInspectionType())
                .setBusinessDate(task.getBusinessDate())
                .setShiftCode(task.getShiftCode())
                .setRoundNo(task.getRoundNo())
                .setPlannedInspectionQuantity(task.getPlannedInspectionQuantity())
                .setActualInspectionQuantity(task.getActualInspectionQuantity());
    }

    private static boolean matchesCurrentActiveOrderSnapshot(
            MesPqcInspectionTaskDO task,
            Map<Long, MesProcessPoolActiveOrderDO> activeOrdersById) {
        if (task == null || task.getId() == null) {
            throw identityMismatch("pqcTaskId缺失");
        }
        MesProcessPoolActiveOrderDO activeOrder = activeOrdersById.get(task.getActiveOrderId());
        if (activeOrder == null) {
            throw identityMismatch(task, "activeOrder缺失");
        }
        return Objects.equals(task.getWorkOrderId(), activeOrder.getWorkOrderId())
                && Objects.equals(task.getRouteId(), activeOrder.getRouteId())
                && Objects.equals(task.getRouteVersionId(), activeOrder.getRouteVersionId())
                && Objects.equals(task.getRegulationVersionId(), activeOrder.getQaRegulationVersionId());
    }

    private static void requireCurrentTaskComplete(
            MesPqcInspectionTaskDO task,
            Map<Long, MesProcessPoolActiveOrderDO> activeOrdersById) {
        MesProcessPoolActiveOrderDO activeOrder = activeOrdersById.get(task.getActiveOrderId());
        boolean complete = activeOrder != null
                && ACTIVE_TASK_STATUSES.contains(task.getTaskStatus())
                && task.getWorkOrderId() != null
                && task.getRouteId() != null
                && task.getRouteVersionId() != null
                && task.getRegulationVersionId() != null
                && activeOrder.getQaRegulationId() != null
                && task.getQaProcessId() != null
                && RULE_SORTS.containsKey(task.getInspectionRuleKey())
                && task.getInspectionType() != null
                && task.getBusinessDate() != null
                && task.getShiftCode() != null
                && task.getRoundNo() != null
                && task.getPlannedInspectionQuantity() != null;
        if (!complete) {
            throw identityMismatch(task, "当前任务正式身份不完整");
        }
    }

    private static <T> T requireRow(Map<Long, T> rowsById, Long id,
                                    MesPqcInspectionTaskDO task, String field) {
        T row = rowsById.get(id);
        if (row == null) {
            throw identityMismatch(task, field + "缺失");
        }
        return row;
    }

    private static void requireText(String value, MesPqcInspectionTaskDO task, String field) {
        if (value == null || value.isBlank()) {
            throw identityMismatch(task, field + "缺失");
        }
    }

    private static <T> List<Long> distinctIds(Collection<T> rows, Function<T, Long> idGetter) {
        return rows.stream()
                .map(idGetter)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private static <T> Map<Long, T> mapById(Collection<T> rows, Function<T, Long> idGetter,
                                             String source) {
        if (rows == null) {
            throw identityMismatch(source + "查询未返回正式结果");
        }
        try {
            return rows.stream().collect(Collectors.toMap(idGetter, Function.identity(),
                    (left, right) -> {
                        throw identityMismatch(source + "身份重复");
                    }, LinkedHashMap::new));
        } catch (NullPointerException exception) {
            throw identityMismatch(source + "身份缺失");
        }
    }

    private static cn.iocoder.yudao.framework.common.exception.ServiceException identityMismatch(
            MesPqcInspectionTaskDO task, String reason) {
        return identityMismatch("pqcTaskId=" + (task == null ? null : task.getId()) + "，" + reason);
    }

    private static cn.iocoder.yudao.framework.common.exception.ServiceException identityMismatch(String reason) {
        return exception(PRO_FRONTLINE_PQC_TASK_IDENTITY_MISMATCH, reason);
    }
}
