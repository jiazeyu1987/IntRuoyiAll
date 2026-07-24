package cn.iocoder.yudao.module.mes.controller.admin.pro.route;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.process.MesProRouteProcessBaseRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.process.MesProRouteProcessMachineryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.process.MesProRouteProcessRelationRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.process.MesProRouteProcessRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.process.MesProRouteProcessSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.repair.MesDvRepairDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationWorkerDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowEdgeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteScheduleConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProScheduleResourceAdjustmentDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.repair.MesDvRepairMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteScheduleConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowEdgeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.enums.dv.MesDvRepairStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleCapacityModeEnum;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryProcessService;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationMachineService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationWorkerService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProScheduleResourceAdjustmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_SCHEDULE_HOURLY_CAPACITY_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_SCHEDULE_SHIFT_HOURS_REQUIRED;

@Tag(name = "管理后台 - MES 工艺路线工序")
@RestController
@RequestMapping("/mes/pro/route-process")
@Validated
public class MesProRouteProcessController {

    private static final String CAPACITY_SOURCE_MACHINE = "MACHINE";
    private static final String CAPACITY_SOURCE_WORKER = "WORKER";
    private static final String CAPACITY_SOURCE_UNCONFIGURED = "UNCONFIGURED";
    private static final String RESOURCE_STATUS_NORMAL = "NORMAL";
    private static final String RESOURCE_STATUS_REPAIR = "REPAIR";
    private static final String RESOURCE_STATUS_CAPACITY_MISSING = "CAPACITY_MISSING";
    private static final String RESOURCE_STATUS_UNCONFIGURED = "UNCONFIGURED";
    private static final String RESOURCE_STATUS_ADJUSTED = "ADJUSTED";
    private static final String RESOURCE_REASON_SHIFT_HOURS_MISSING = "班次小时未配置";
    private static final String RESOURCE_REASON_SHIFT_HOURS_CONFLICT = "班次小时配置不一致";

    @Resource
    private MesProRouteProcessService routeProcessService;
    @Resource
    private MesProProcessService processService;
    @Resource
    private MesMdWorkstationService workstationService;
    @Resource
    private MesMdWorkstationMachineService workstationMachineService;
    @Resource
    private MesMdWorkstationWorkerService workstationWorkerService;
    @Resource
    private MesDvMachineryService machineryService;
    @Resource
    private MesDvMachineryProcessService machineryProcessService;
    @Resource
    private MesDvRepairMapper repairMapper;
    @Resource
    private MesProBatchRecordReportMapper batchRecordReportMapper;
    @Resource
    private MesProScheduleResourceAdjustmentService scheduleResourceAdjustmentService;
    @Resource
    private MesProRouteVersionMapper routeVersionMapper;
    @Resource
    private MesProRouteScheduleConfigMapper routeScheduleConfigMapper;
    @Resource
    private MesProRouteProcessFlowEdgeMapper routeProcessFlowEdgeMapper;

    @PostMapping("/create")
    @Operation(summary = "创建工艺路线工序")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:update')")
    public CommonResult<Long> createRouteProcess(@Valid @RequestBody MesProRouteProcessSaveReqVO createReqVO) {
        return success(routeProcessService.createRouteProcess(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新工艺路线工序")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:update')")
    public CommonResult<Boolean> updateRouteProcess(@Valid @RequestBody MesProRouteProcessSaveReqVO updateReqVO) {
        routeProcessService.updateRouteProcess(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除工艺路线工序")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro-route:update')")
    public CommonResult<Boolean> deleteRouteProcess(@RequestParam("id") Long id) {
        routeProcessService.deleteRouteProcess(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得工艺路线工序")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasAnyPermissions('mes:pro-route:query', 'mes:pro-schedule-order:query', 'mes:pro-route:schedule-config:query', 'mes:pro-route:batch-record-config:query')")
    public CommonResult<MesProRouteProcessRespVO> getRouteProcess(@RequestParam("id") Long id) {
        MesProRouteProcessDO routeProcess = routeProcessService.getRouteProcess(id);
        return success(buildRouteProcessRespVO(routeProcess, null, LocalDate.now()));
    }

    @GetMapping("/list-by-product")
    @Operation(summary = "按产品获得工序列表", description = "根据产品查找关联的工艺路线，返回该路线的工序列表")
    @Parameter(name = "productId", description = "产品编号", required = true, example = "1")
    @PreAuthorize("@ss.hasAnyPermissions('mes:pro-route:query', 'mes:pro-schedule-order:query', 'mes:pro-route:schedule-config:query', 'mes:pro-route:batch-record-config:query')")
    public CommonResult<List<MesProRouteProcessRespVO>> getRouteProcessListByProduct(@RequestParam("productId") Long productId) {
        List<MesProRouteProcessDO> list = routeProcessService.getRouteProcessListByProductId(productId);
        return success(buildRouteProcessRespVOList(list, null, LocalDate.now()));
    }

    @GetMapping("/list-base-by-route")
    @Operation(summary = "按工艺路线获得基础工序列表")
    @Parameter(name = "routeId", description = "工艺路线编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:query')")
    public CommonResult<List<MesProRouteProcessBaseRespVO>> getRouteBaseProcessListByRoute(
            @RequestParam("routeId") Long routeId) {
        return success(buildRouteProcessBaseRespVOList(
                routeProcessService.getRouteProcessListByRouteId(routeId)));
    }

    @GetMapping("/list-by-route")
    @Operation(summary = "按工艺路线获得工序列表")
    @Parameter(name = "routeId", description = "工艺路线编号", required = true, example = "1")
    @Parameter(name = "calendarDate", description = "生效日期，默认今天")
    @PreAuthorize("@ss.hasAnyPermissions('mes:pro-route:query', 'mes:pro-schedule-order:query', 'mes:pro-route:schedule-config:query', 'mes:pro-route:batch-record-config:query')")
    public CommonResult<List<MesProRouteProcessRespVO>> getRouteProcessListByRoute(
            @RequestParam("routeId") Long routeId,
            @RequestParam(value = "calendarDate", required = false) LocalDate calendarDate) {
        List<MesProRouteProcessDO> list = routeProcessService.getRouteProcessListByRouteId(routeId);
        return success(buildRouteProcessRespVOList(list, routeId, calendarDate == null ? LocalDate.now() : calendarDate));
    }

    @GetMapping("/get-by-route-and-process")
    @Operation(summary = "按工艺路线+工序获得工序配置")
    @Parameter(name = "routeId", description = "工艺路线编号", required = true, example = "1")
    @Parameter(name = "processId", description = "工序编号", required = true, example = "1")
    @PreAuthorize("@ss.hasAnyPermissions('mes:pro-route:query', 'mes:pro-schedule-order:query', 'mes:pro-route:schedule-config:query', 'mes:pro-route:batch-record-config:query')")
    public CommonResult<MesProRouteProcessRespVO> getRouteProcessByRouteAndProcess(
            @RequestParam("routeId") Long routeId,
            @RequestParam("processId") Long processId) {
        MesProRouteProcessDO routeProcess = routeProcessService.getRouteProcessByRouteIdAndProcessId(routeId, processId);
        return success(buildRouteProcessRespVO(routeProcess, routeId, LocalDate.now()));
    }

    private List<MesProRouteProcessBaseRespVO> buildRouteProcessBaseRespVOList(List<MesProRouteProcessDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        Set<Long> processIds = new LinkedHashSet<>(convertSet(list, MesProRouteProcessDO::getProcessId));
        Map<Long, MesProProcessDO> processMap = convertMap(
                processService.getProcessList(new ArrayList<>(processIds)), MesProProcessDO::getId);
        RouteProcessRelationMaps relationMaps = buildRouteProcessRelationMaps(list, processMap);
        return BeanUtils.toBean(list, MesProRouteProcessBaseRespVO.class, vo -> {
            MapUtils.findAndThen(processMap, vo.getProcessId(),
                    process -> vo.setProcessCode(process.getCode()).setProcessName(process.getName()));
            List<MesProRouteProcessRelationRespVO> predecessors =
                    relationMaps.predecessors().getOrDefault(vo.getId(), Collections.emptyList());
            vo.setPredecessors(predecessors);
            vo.setPredecessor(predecessors.size() == 1 ? predecessors.get(0) : null);
            vo.setSuccessors(relationMaps.successors().getOrDefault(vo.getId(), Collections.emptyList()));
        });
    }

    private List<MesProRouteProcessRespVO> buildRouteProcessRespVOList(List<MesProRouteProcessDO> list,
                                                                       Long routeIdHint,
                                                                       LocalDate calendarDate) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        Long routeId = routeIdHint != null ? routeIdHint : list.get(0).getRouteId();
        Set<Long> processIds = convertSet(list, MesProRouteProcessDO::getProcessId);
        Map<Long, MesProProcessDO> processMap = convertMap(
                processService.getProcessList(new ArrayList<>(processIds)), MesProProcessDO::getId);
        RouteProcessRelationMaps relationMaps = buildRouteProcessRelationMaps(list, processMap);
        List<MesMdWorkstationDO> processWorkstations =
                workstationService.getWorkstationListByProcessIds(new ArrayList<>(processIds));
        List<MesMdWorkstationDO> workstations = CollUtil.isEmpty(processWorkstations)
                ? new ArrayList<>() : new ArrayList<>(processWorkstations);
        Set<Long> explicitWorkstationIds = list.stream()
                .map(MesProRouteProcessDO::getWorkstationId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (CollUtil.isNotEmpty(explicitWorkstationIds)) {
            Map<Long, MesMdWorkstationDO> loadedWorkstationMap = convertMap(workstations, MesMdWorkstationDO::getId);
            Set<Long> missingWorkstationIds = explicitWorkstationIds.stream()
                    .filter(id -> !loadedWorkstationMap.containsKey(id))
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            if (CollUtil.isNotEmpty(missingWorkstationIds)) {
                List<MesMdWorkstationDO> explicitWorkstations = workstationService.getWorkstationList(missingWorkstationIds);
                if (CollUtil.isNotEmpty(explicitWorkstations)) {
                    workstations.addAll(explicitWorkstations);
                }
            }
        }
        normalizeWorkstationProcessIds(workstations, processIds);
        Map<Long, MesMdWorkstationDO> workstationById = convertMap(workstations, MesMdWorkstationDO::getId);
        Map<Long, List<MesMdWorkstationDO>> workstationListMap =
                convertMultiMap(workstations, MesMdWorkstationDO::getProcessId);
        Map<Long, MesProRouteProcessDO> routeProcessMap = convertMap(list, MesProRouteProcessDO::getId);
        for (MesProRouteProcessDO routeProcess : list) {
            if (routeProcess.getWorkstationId() == null) {
                continue;
            }
            MesMdWorkstationDO workstation = workstationById.get(routeProcess.getWorkstationId());
            if (workstation == null) {
                throw new IllegalStateException(String.format(Locale.ROOT,
                        "路线工序绑定的工作站不存在: routeProcessId=%s, workstationId=%s",
                        routeProcess.getId(), routeProcess.getWorkstationId()));
            }
        }
        Set<String> batchRecordReportIds = new LinkedHashSet<>();
        list.stream()
                .map(MesProRouteProcessDO::getBatchRecordReportId)
                .filter(StrUtil::isNotBlank)
                .forEach(batchRecordReportIds::add);
        Map<String, MesProBatchRecordReportDO> batchRecordReportMap = convertMap(
                batchRecordReportMapper.selectListByReportIds(batchRecordReportIds),
                MesProBatchRecordReportDO::getReportId);
        Map<Long, List<MesProScheduleResourceAdjustmentDO>> adjustmentMap = convertMultiMap(
                scheduleResourceAdjustmentService.getAdjustmentList(routeId, calendarDate),
                MesProScheduleResourceAdjustmentDO::getRouteProcessId);
        Map<Long, List<MesMdWorkstationDO>> routeProcessWorkstationMap = new LinkedHashMap<>();
        for (MesProRouteProcessDO routeProcess : list) {
            routeProcessWorkstationMap.put(routeProcess.getId(),
                    resolveRouteProcessWorkstations(routeProcess,
                            workstationListMap.getOrDefault(routeProcess.getProcessId(), Collections.emptyList()),
                            workstationById));
        }
        Map<Long, MesProRouteScheduleConfigDO> routeScheduleConfigMap = loadRouteScheduleConfigMap(routeId);
        Map<Long, List<MesProRouteProcessMachineryRespVO>> machineryListMap =
                buildMachineryListMap(routeProcessWorkstationMap, routeProcessMap, adjustmentMap);
        Map<Long, List<MesMdWorkstationWorkerDO>> workerBindingMap = buildWorkerBindingMap(workstations);
        return BeanUtils.toBean(list, MesProRouteProcessRespVO.class, vo -> {
            MapUtils.findAndThen(processMap, vo.getProcessId(),
                    process -> vo.setProcessCode(process.getCode())
                            .setProcessName(process.getName())
                            .setProcessProductName(process.getProductName())
                            .setProcessAttention(process.getAttention())
                            .setProcessStatus(process.getStatus())
                            .setProcessManualShiftCapacity(process.getManualShiftCapacity()));
            List<MesProRouteProcessRelationRespVO> predecessors =
                    relationMaps.predecessors().getOrDefault(vo.getId(), Collections.emptyList());
            vo.setPredecessors(predecessors);
            vo.setPredecessor(predecessors.size() == 1 ? predecessors.get(0) : null);
            vo.setSuccessors(relationMaps.successors().getOrDefault(vo.getId(), Collections.emptyList()));
            List<MesMdWorkstationDO> processWorkstationList =
                    routeProcessWorkstationMap.getOrDefault(vo.getId(), Collections.emptyList());
            MesMdWorkstationDO primaryWorkstation = resolvePrimaryWorkstation(routeProcessMap.get(vo.getId()),
                    processWorkstationList, workstationById);
            if (primaryWorkstation != null) {
                vo.setWorkstationId(primaryWorkstation.getId())
                        .setWorkstationCode(primaryWorkstation.getCode())
                        .setWorkstationName(primaryWorkstation.getName());
            }
            List<MesProRouteProcessMachineryRespVO> machineryList =
                    machineryListMap.getOrDefault(vo.getId(), Collections.emptyList());
            vo.setMachineryList(machineryList);
            vo.setMachineryQuantityTotal(machineryList.stream()
                    .mapToInt(MesProRouteProcessMachineryRespVO::getQuantity)
                    .sum());
            fillCapacitySummary(vo, processWorkstationList, primaryWorkstation, machineryList, workerBindingMap,
                    adjustmentMap.getOrDefault(vo.getId(), Collections.emptyList()));
            applyScheduleConfigCapacity(vo, routeScheduleConfigMap.get(vo.getId()));
            MapUtils.findAndThen(batchRecordReportMap, vo.getBatchRecordReportId(),
                    report -> vo.setBatchRecordReportCode(report.getReportCode())
                            .setBatchRecordReportName(report.getReportName()));
        });
    }

    private void normalizeWorkstationProcessIds(List<MesMdWorkstationDO> workstations, Set<Long> targetProcessIds) {
        if (CollUtil.isEmpty(workstations) || CollUtil.isEmpty(targetProcessIds)) {
            return;
        }
        Map<Long, Long> processIdentityMap = routeProcessService.getProcessIdentityMap(targetProcessIds);
        if (CollUtil.isEmpty(processIdentityMap)) {
            return;
        }
        workstations.forEach(workstation -> {
            Long normalizedProcessId = processIdentityMap.get(workstation.getProcessId());
            if (normalizedProcessId != null) {
                workstation.setProcessId(normalizedProcessId);
            }
        });
    }

    private MesMdWorkstationDO resolvePrimaryWorkstation(MesProRouteProcessDO routeProcess,
                                                          List<MesMdWorkstationDO> processWorkstations,
                                                          Map<Long, MesMdWorkstationDO> workstationById) {
        if (routeProcess != null && routeProcess.getWorkstationId() != null) {
            return processWorkstations.stream()
                    .filter(workstation -> Objects.equals(workstation.getId(), routeProcess.getWorkstationId()))
                    .findFirst()
                    .orElse(workstationById.get(routeProcess.getWorkstationId()));
        }
        if (CollUtil.isEmpty(processWorkstations)) {
            return null;
        }
        return processWorkstations.stream()
                .min(Comparator.comparing(MesMdWorkstationDO::getId, Comparator.nullsLast(Long::compareTo)))
                .orElse(null);
    }

    private List<MesMdWorkstationDO> resolveRouteProcessWorkstations(MesProRouteProcessDO routeProcess,
                                                                     List<MesMdWorkstationDO> processWorkstations,
                                                                     Map<Long, MesMdWorkstationDO> workstationById) {
        if (routeProcess != null && routeProcess.getWorkstationId() != null) {
            MesMdWorkstationDO workstation = workstationById.get(routeProcess.getWorkstationId());
            return workstation == null ? Collections.emptyList()
                    : List.of(copyWorkstationForRouteProcess(workstation, routeProcess.getProcessId()));
        }
        return processWorkstations;
    }

    private MesMdWorkstationDO copyWorkstationForRouteProcess(MesMdWorkstationDO workstation, Long processId) {
        MesMdWorkstationDO copy = BeanUtils.toBean(workstation, MesMdWorkstationDO.class);
        copy.setProcessId(processId);
        return copy;
    }

    private RouteProcessRelationMaps buildRouteProcessRelationMaps(
            List<MesProRouteProcessDO> routeProcesses,
            Map<Long, MesProProcessDO> processMap) {
        Map<Long, MesProRouteProcessDO> routeProcessMap = convertMap(
                routeProcesses, MesProRouteProcessDO::getId);
        Map<Long, List<MesProRouteProcessRelationRespVO>> predecessors = new LinkedHashMap<>();
        Map<Long, List<MesProRouteProcessRelationRespVO>> successors = new LinkedHashMap<>();
        Long routeId = routeProcesses.get(0).getRouteId();
        List<MesProRouteProcessFlowEdgeDO> edges = routeProcessFlowEdgeMapper.selectListByRouteId(routeId);
        for (MesProRouteProcessFlowEdgeDO edge : edges) {
            MesProRouteProcessDO source = routeProcessMap.get(edge.getSourceRouteProcessId());
            MesProRouteProcessDO target = routeProcessMap.get(edge.getTargetRouteProcessId());
            if (source == null || target == null) {
                // The flow graph endpoint reports invalid edges; this row list renders current route-process relations only.
                continue;
            }
            predecessors.computeIfAbsent(target.getId(), key -> new ArrayList<>())
                    .add(toRouteProcessRelation(source, processMap));
            successors.computeIfAbsent(source.getId(), key -> new ArrayList<>())
                    .add(toRouteProcessRelation(target, processMap));
        }
        return new RouteProcessRelationMaps(predecessors, successors);
    }

    private MesProRouteProcessRelationRespVO toRouteProcessRelation(
            MesProRouteProcessDO routeProcess,
            Map<Long, MesProProcessDO> processMap) {
        MesProRouteProcessRelationRespVO relation = new MesProRouteProcessRelationRespVO();
        relation.setRouteProcessId(routeProcess.getId());
        relation.setProcessId(routeProcess.getProcessId());
        MesProProcessDO process = processMap.get(routeProcess.getProcessId());
        if (process != null) {
            relation.setProcessCode(process.getCode());
            relation.setProcessName(process.getName());
        }
        return relation;
    }

    private record RouteProcessRelationMaps(
            Map<Long, List<MesProRouteProcessRelationRespVO>> predecessors,
            Map<Long, List<MesProRouteProcessRelationRespVO>> successors) {
    }

    private Map<Long, MesProRouteScheduleConfigDO> loadRouteScheduleConfigMap(Long routeId) {
        MesProRouteVersionDO activeVersion = routeVersionMapper.selectActiveByRouteId(routeId);
        if (activeVersion == null) {
            return Collections.emptyMap();
        }
        List<MesProRouteScheduleConfigDO> configs =
                routeScheduleConfigMapper.selectListByRouteVersionId(activeVersion.getId());
        if (CollUtil.isEmpty(configs)) {
            return Collections.emptyMap();
        }
        Map<Long, MesProRouteScheduleConfigDO> result = new LinkedHashMap<>();
        for (MesProRouteScheduleConfigDO config : configs) {
            result.putIfAbsent(config.getRouteProcessId(), config);
        }
        return result;
    }

    private void applyScheduleConfigCapacity(MesProRouteProcessRespVO vo, MesProRouteScheduleConfigDO config) {
        if (config == null) {
            return;
        }
        if (!MesProScheduleCapacityModeEnum.isManualOverrideLike(config.getCapacityMode())) {
            return;
        }
        if (config.getHourlyCapacity() == null || config.getHourlyCapacity().compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_ROUTE_SCHEDULE_HOURLY_CAPACITY_REQUIRED, config.getRouteProcessId());
        }
        if (vo.getShiftHours() == null) {
            vo.setResourceStatus(RESOURCE_STATUS_CAPACITY_MISSING);
            if (!RESOURCE_REASON_SHIFT_HOURS_CONFLICT.equals(vo.getResourceStatusReason())) {
                vo.setResourceStatusReason(RESOURCE_REASON_SHIFT_HOURS_MISSING);
            }
            return;
        }
        vo.setProcessHourlyCapacityTotal(config.getHourlyCapacity());
        vo.setProcessShiftCapacityTotal(config.getHourlyCapacity().multiply(vo.getShiftHours()));
    }

    private Map<Long, List<MesMdWorkstationWorkerDO>> buildWorkerBindingMap(List<MesMdWorkstationDO> workstations) {
        if (CollUtil.isEmpty(workstations)) {
            return Collections.emptyMap();
        }
        Set<Long> workstationIds = convertSet(workstations, MesMdWorkstationDO::getId);
        return convertMultiMap(
                workstationWorkerService.getWorkstationWorkerListByWorkstationIds(workstationIds),
                MesMdWorkstationWorkerDO::getWorkstationId);
    }

    private void fillCapacitySummary(MesProRouteProcessRespVO vo,
                                     List<MesMdWorkstationDO> workstations,
                                     MesMdWorkstationDO primaryWorkstation,
                                     List<MesProRouteProcessMachineryRespVO> machineryList,
                                     Map<Long, List<MesMdWorkstationWorkerDO>> workerBindingMap,
                                     List<MesProScheduleResourceAdjustmentDO> adjustments) {
        if (CollUtil.isNotEmpty(machineryList)) {
            ShiftHoursResolution shiftHoursResolution = resolveProcessShiftHours(workstations, adjustments);
            BigDecimal shiftHours = shiftHoursResolution.shiftHours();
            vo.setShiftHours(shiftHours);
            BigDecimal hourlyCapacity = BigDecimal.ZERO;
            BigDecimal todayHourlyCapacity = BigDecimal.ZERO;
            int todayAvailableQuantity = 0;
            boolean hasRepair = false;
            boolean hasCapacityMissing = false;
            boolean hasAdjustment = false;
            List<String> statusReasons = new ArrayList<>();
            for (MesProRouteProcessMachineryRespVO machinery : machineryList) {
                if (machinery.getMachineryHourlyCapacityTotal() != null) {
                    hourlyCapacity = hourlyCapacity.add(machinery.getMachineryHourlyCapacityTotal());
                } else {
                    hasCapacityMissing = true;
                }
                if (machinery.getAvailableHourlyCapacityTotal() != null) {
                    todayHourlyCapacity = todayHourlyCapacity.add(machinery.getAvailableHourlyCapacityTotal());
                }
                todayAvailableQuantity += machinery.getAvailableQuantity() == null ? 0 : machinery.getAvailableQuantity();
                if (Boolean.TRUE.equals(machinery.getUnderRepair())) {
                    hasRepair = true;
                    statusReasons.add(machinery.getAvailabilityReason());
                }
                if (RESOURCE_STATUS_ADJUSTED.equals(machinery.getAvailabilityStatus())) {
                    hasAdjustment = true;
                    statusReasons.add(machinery.getAvailabilityReason());
                }
            }
            vo.setCapacitySource(CAPACITY_SOURCE_MACHINE);
            vo.setWorkerQuantityTotal(0);
            vo.setProcessHourlyCapacityTotal(hourlyCapacity);
            vo.setProcessShiftCapacityTotal(multiplyByShiftHours(hourlyCapacity, shiftHours));
            vo.setTodayAvailableResourceQuantityTotal(todayAvailableQuantity);
            vo.setTodayHourlyCapacityTotal(todayHourlyCapacity);
            vo.setTodayShiftCapacityTotal(multiplyByShiftHours(todayHourlyCapacity, shiftHours));
            if (shiftHoursResolution.conflict()) {
                vo.setResourceStatus(RESOURCE_STATUS_CAPACITY_MISSING);
                vo.setResourceStatusReason(RESOURCE_REASON_SHIFT_HOURS_CONFLICT);
            } else if (shiftHours == null) {
                vo.setResourceStatus(RESOURCE_STATUS_CAPACITY_MISSING);
                vo.setResourceStatusReason(RESOURCE_REASON_SHIFT_HOURS_MISSING);
            } else if (hasAdjustment) {
                vo.setResourceStatus(RESOURCE_STATUS_ADJUSTED);
                vo.setResourceStatusReason(String.join("；", statusReasons));
            } else if (hasRepair) {
                vo.setResourceStatus(RESOURCE_STATUS_REPAIR);
                vo.setResourceStatusReason(String.join("；", statusReasons));
            } else if (hasCapacityMissing) {
                vo.setResourceStatus(RESOURCE_STATUS_CAPACITY_MISSING);
                vo.setResourceStatusReason("设备工序产能未配置");
            } else {
                vo.setResourceStatus(RESOURCE_STATUS_NORMAL);
                vo.setResourceStatusReason("正常");
            }
            return;
        }

        if (CollUtil.isNotEmpty(workstations)) {
            ShiftHoursResolution shiftHoursResolution = resolveProcessShiftHours(workstations, adjustments);
            BigDecimal shiftHours = shiftHoursResolution.shiftHours();
            vo.setShiftHours(shiftHours);
            BigDecimal hourlyCapacity = BigDecimal.ZERO;
            int workerQuantityTotal = 0;
            BigDecimal workerSingleHourlyCapacity = null;
            boolean hasCapacityMissing = false;
            MesProScheduleResourceAdjustmentDO workerAdjustment = resolveWorkerAdjustment(adjustments);
            for (MesMdWorkstationDO workstation : workstations) {
                WorkerQuantityResolution workerQuantityResolution = resolveWorkerQuantity(
                        workerBindingMap.getOrDefault(workstation.getId(), Collections.emptyList()));
                int workstationWorkerQuantity = workerQuantityResolution.quantity();
                if (workerAdjustment != null && workerAdjustment.getWorkerQuantityOverride() != null) {
                    workstationWorkerQuantity = workerAdjustment.getWorkerQuantityOverride();
                }
                workerQuantityTotal += workstationWorkerQuantity;
                BigDecimal singleHourlyCapacity = workstation.getSingleStandardHourlyCapacity();
                if (workerAdjustment != null && workerAdjustment.getSingleHourlyCapacityOverride() != null) {
                    singleHourlyCapacity = workerAdjustment.getSingleHourlyCapacityOverride();
                }
                if (singleHourlyCapacity != null) {
                    if (workerSingleHourlyCapacity == null) {
                        workerSingleHourlyCapacity = singleHourlyCapacity;
                    }
                    hourlyCapacity = hourlyCapacity.add(singleHourlyCapacity);
                } else {
                    hasCapacityMissing = true;
                }
            }
            vo.setCapacitySource(CAPACITY_SOURCE_WORKER);
            vo.setWorkerQuantityTotal(workerQuantityTotal);
            vo.setWorkstationWorkerId(resolvePrimaryWorkstationWorkerId(primaryWorkstation, workerBindingMap));
            vo.setProcessHourlyCapacityTotal(hourlyCapacity);
            vo.setProcessShiftCapacityTotal(multiplyByShiftHours(hourlyCapacity, shiftHours));
            vo.setWorkerSingleStandardHourlyCapacity(workerSingleHourlyCapacity);
            vo.setTodayAvailableResourceQuantityTotal(workerQuantityTotal);
            vo.setTodayHourlyCapacityTotal(hourlyCapacity);
            vo.setTodayShiftCapacityTotal(multiplyByShiftHours(hourlyCapacity, shiftHours));
            if (shiftHoursResolution.conflict()) {
                vo.setResourceStatus(RESOURCE_STATUS_CAPACITY_MISSING);
                vo.setResourceStatusReason(RESOURCE_REASON_SHIFT_HOURS_CONFLICT);
            } else if (shiftHours == null) {
                vo.setResourceStatus(RESOURCE_STATUS_CAPACITY_MISSING);
                vo.setResourceStatusReason(RESOURCE_REASON_SHIFT_HOURS_MISSING);
            } else if (hasCapacityMissing) {
                vo.setResourceStatus(RESOURCE_STATUS_CAPACITY_MISSING);
                vo.setResourceStatusReason("人工小时产能未配置");
            } else if (workerAdjustment != null) {
                vo.setResourceStatus(RESOURCE_STATUS_ADJUSTED);
                vo.setResourceStatusReason(StrUtil.blankToDefault(workerAdjustment.getReason(), "已应用日资源调整"));
            } else {
                vo.setResourceStatus(RESOURCE_STATUS_NORMAL);
                vo.setResourceStatusReason("正常");
            }
            return;
        }

        vo.setCapacitySource(CAPACITY_SOURCE_UNCONFIGURED);
        vo.setWorkerQuantityTotal(0);
        vo.setProcessHourlyCapacityTotal(BigDecimal.ZERO);
        vo.setProcessShiftCapacityTotal(BigDecimal.ZERO);
        vo.setTodayAvailableResourceQuantityTotal(0);
        vo.setTodayHourlyCapacityTotal(BigDecimal.ZERO);
        vo.setTodayShiftCapacityTotal(BigDecimal.ZERO);
        vo.setResourceStatus(RESOURCE_STATUS_UNCONFIGURED);
        vo.setResourceStatusReason("资源未配置");
    }

    private ShiftHoursResolution resolveProcessShiftHours(List<MesMdWorkstationDO> workstations,
                                                          List<MesProScheduleResourceAdjustmentDO> adjustments) {
        MesProScheduleResourceAdjustmentDO workerAdjustment = resolveWorkerAdjustment(adjustments);
        if (workerAdjustment != null && workerAdjustment.getShiftHoursOverride() != null) {
            return new ShiftHoursResolution(normalizeShiftHours(workerAdjustment.getShiftHoursOverride()), false);
        }
        BigDecimal shiftHours = null;
        for (MesMdWorkstationDO workstation : workstations) {
            if (workstation.getShiftHours() == null) {
                continue;
            }
            if (shiftHours == null) {
                shiftHours = workstation.getShiftHours();
                continue;
            }
            if (shiftHours.compareTo(workstation.getShiftHours()) != 0) {
                return new ShiftHoursResolution(null, true);
            }
        }
        return new ShiftHoursResolution(normalizeShiftHours(shiftHours), false);
    }

    private record ShiftHoursResolution(BigDecimal shiftHours, boolean conflict) {
    }

    private record WorkerQuantityResolution(int quantity, boolean missing) {
    }

    private BigDecimal multiplyByShiftHours(BigDecimal hourlyCapacity, BigDecimal shiftHours) {
        if (hourlyCapacity == null || shiftHours == null) {
            return null;
        }
        return hourlyCapacity.multiply(shiftHours);
    }

    private BigDecimal normalizeShiftHours(BigDecimal shiftHours) {
        return shiftHours == null || shiftHours.compareTo(BigDecimal.ZERO) <= 0 ? null : shiftHours;
    }

    private Long resolvePrimaryWorkstationWorkerId(MesMdWorkstationDO primaryWorkstation,
                                                   Map<Long, List<MesMdWorkstationWorkerDO>> workerBindingMap) {
        if (primaryWorkstation == null) {
            return null;
        }
        return workerBindingMap.getOrDefault(primaryWorkstation.getId(), Collections.emptyList()).stream()
                .min(Comparator.comparing(MesMdWorkstationWorkerDO::getId, Comparator.nullsLast(Long::compareTo)))
                .map(MesMdWorkstationWorkerDO::getId)
                .orElse(null);
    }

    private WorkerQuantityResolution resolveWorkerQuantity(List<MesMdWorkstationWorkerDO> workers) {
        if (CollUtil.isEmpty(workers)) {
            return new WorkerQuantityResolution(0, true);
        }
        int workerQuantityTotal = 0;
        boolean missing = false;
        for (MesMdWorkstationWorkerDO worker : workers) {
            if (worker.getQuantity() == null || worker.getQuantity() <= 0) {
                missing = true;
                continue;
            }
            workerQuantityTotal += worker.getQuantity();
        }
        return new WorkerQuantityResolution(workerQuantityTotal, missing);
    }

    private Map<Long, List<MesProRouteProcessMachineryRespVO>> buildMachineryListMap(
            Map<Long, List<MesMdWorkstationDO>> routeProcessWorkstationMap,
            Map<Long, MesProRouteProcessDO> routeProcessMap,
            Map<Long, List<MesProScheduleResourceAdjustmentDO>> adjustmentMap) {
        if (CollUtil.isEmpty(routeProcessWorkstationMap)) {
            return Collections.emptyMap();
        }
        List<MesMdWorkstationDO> workstations = routeProcessWorkstationMap.values().stream()
                .flatMap(List::stream)
                .collect(java.util.stream.Collectors.toMap(
                        MesMdWorkstationDO::getId,
                        workstation -> workstation,
                        (first, second) -> first,
                        LinkedHashMap::new))
                .values()
                .stream()
                .toList();
        if (CollUtil.isEmpty(workstations)) {
            return Collections.emptyMap();
        }
        Set<Long> workstationIds = convertSet(workstations, MesMdWorkstationDO::getId);
        List<MesMdWorkstationMachineDO> machineBindings =
                workstationMachineService.getWorkstationMachineListByWorkstationIds(workstationIds);
        if (CollUtil.isEmpty(machineBindings)) {
            return Collections.emptyMap();
        }
        Map<Long, List<MesMdWorkstationMachineDO>> machineBindingMap =
                convertMultiMap(machineBindings, MesMdWorkstationMachineDO::getWorkstationId);
        Set<Long> machineryIds = convertSet(machineBindings, MesMdWorkstationMachineDO::getMachineryId);
        Set<Long> processIds = routeProcessMap.values().stream()
                .map(MesProRouteProcessDO::getProcessId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MesDvMachineryDO> machineryMap = machineryService.getMachineryMap(machineryIds);
        Map<String, MesDvMachineryProcessDO> machineryProcessMap = buildMachineryProcessMap(
                machineryProcessService.getMachineryProcessListByMachineryIdsAndProcessIds(
                        machineryIds, processIds));
        Map<Long, MesDvRepairDO> activeRepairMap = buildActiveRepairMap(machineryIds);

        Map<Long, List<MesProRouteProcessMachineryRespVO>> result = new LinkedHashMap<>();
        for (Map.Entry<Long, List<MesMdWorkstationDO>> entry : routeProcessWorkstationMap.entrySet()) {
            MesProRouteProcessDO routeProcess = routeProcessMap.get(entry.getKey());
            if (routeProcess == null) {
                continue;
            }
            for (MesMdWorkstationDO workstation : entry.getValue()) {
                List<MesMdWorkstationMachineDO> workstationMachines =
                        machineBindingMap.getOrDefault(workstation.getId(), Collections.emptyList());
                for (MesMdWorkstationMachineDO machineBinding : workstationMachines) {
                    MesProRouteProcessMachineryRespVO machinery = buildMachineryRespVO(
                            workstation, machineBinding, machineryMap, machineryProcessMap, activeRepairMap,
                            adjustmentMap.getOrDefault(routeProcess.getId(), Collections.emptyList()));
                    result.computeIfAbsent(routeProcess.getId(), key -> new ArrayList<>()).add(machinery);
                }
            }
        }
        result.values().forEach(machineryList -> machineryList.sort(machineryComparator()));
        return result;
    }

    private MesProRouteProcessMachineryRespVO buildMachineryRespVO(MesMdWorkstationDO workstation,
                                                                    MesMdWorkstationMachineDO machineBinding,
                                                                    Map<Long, MesDvMachineryDO> machineryMap,
                                                                    Map<String, MesDvMachineryProcessDO> machineryProcessMap,
                                                                    Map<Long, MesDvRepairDO> activeRepairMap,
                                                                    List<MesProScheduleResourceAdjustmentDO> adjustments) {
        Long machineryId = machineBinding.getMachineryId();
        if (machineBinding.getQuantity() == null) {
            throw new IllegalStateException("Missing machinery quantity: workstationMachineId="
                    + machineBinding.getId() + ", machineryId=" + machineryId);
        }
        MesDvMachineryDO machinery = machineryMap.get(machineryId);
        if (machinery == null) {
            throw new IllegalStateException("Missing machinery master: workstationMachineId="
                    + machineBinding.getId() + ", machineryId=" + machineryId);
        }

        MesProRouteProcessMachineryRespVO vo = new MesProRouteProcessMachineryRespVO();
        vo.setWorkstationMachineId(machineBinding.getId());
        vo.setWorkstationId(workstation.getId());
        vo.setWorkstationCode(workstation.getCode());
        vo.setWorkstationName(workstation.getName());
        vo.setMachineryId(machinery.getId());
        vo.setMachineryCode(machinery.getCode());
        vo.setMachineryName(machinery.getName());
        vo.setQuantity(machineBinding.getQuantity());
        MesDvMachineryProcessDO machineryProcess = machineryProcessMap.get(
                buildMachineryProcessKey(machineryId, workstation.getProcessId()));
        BigDecimal standardHourlyCapacity = machineryProcess == null ? null
                : machineryProcess.getStandardHourlyCapacity();
        MesDvRepairDO activeRepair = activeRepairMap.get(machineryId);
        MesProScheduleResourceAdjustmentDO machineAdjustment = resolveMachineAdjustment(adjustments, machineBinding, machineryId);
        boolean underRepair = activeRepair != null;
        Integer availableQuantity = underRepair ? 0 : machineBinding.getQuantity();
        if (machineAdjustment != null && machineAdjustment.getAvailableQuantityOverride() != null) {
            availableQuantity = machineAdjustment.getAvailableQuantityOverride();
        }
        vo.setMachineryStandardHourlyCapacity(standardHourlyCapacity);
        vo.setMachineryHourlyCapacityTotal(standardHourlyCapacity == null ? null
                : standardHourlyCapacity.multiply(BigDecimal.valueOf(machineBinding.getQuantity())));
        vo.setAvailableQuantity(availableQuantity);
        vo.setUnderRepair(underRepair);
        if (machineAdjustment != null) {
            vo.setAvailabilityStatus(RESOURCE_STATUS_ADJUSTED);
            vo.setAvailabilityReason(StrUtil.blankToDefault(machineAdjustment.getReason(), "已应用日资源调整"));
        } else {
            vo.setAvailabilityStatus(underRepair ? RESOURCE_STATUS_REPAIR : RESOURCE_STATUS_NORMAL);
            vo.setAvailabilityReason(underRepair ? buildRepairReason(activeRepair) : "正常");
        }
        vo.setAvailableHourlyCapacityTotal(standardHourlyCapacity == null ? null
                : standardHourlyCapacity.multiply(BigDecimal.valueOf(availableQuantity)));
        BigDecimal shiftHours = normalizeShiftHours(workstation.getShiftHours());
        vo.setAvailableShiftCapacityTotal(multiplyByShiftHours(vo.getAvailableHourlyCapacityTotal(), shiftHours));
        return vo;
    }

    private Map<Long, MesDvRepairDO> buildActiveRepairMap(Set<Long> machineryIds) {
        List<MesDvRepairDO> repairs = repairMapper.selectListByMachineryIdsAndStatuses(machineryIds, List.of(
                MesDvRepairStatusEnum.CONFIRMED.getStatus(),
                MesDvRepairStatusEnum.APPROVING.getStatus()));
        if (CollUtil.isEmpty(repairs)) {
            return Collections.emptyMap();
        }
        Map<Long, MesDvRepairDO> result = new LinkedHashMap<>();
        for (MesDvRepairDO repair : repairs) {
            result.putIfAbsent(repair.getMachineryId(), repair);
        }
        return result;
    }

    private String buildRepairReason(MesDvRepairDO repair) {
        String repairName = StrUtil.blankToDefault(repair.getName(), "维修工单");
        if (StrUtil.isNotBlank(repair.getCode())) {
            return repairName + "（" + repair.getCode() + "）";
        }
        return repairName;
    }

    private Map<String, MesDvMachineryProcessDO> buildMachineryProcessMap(List<MesDvMachineryProcessDO> rows) {
        if (CollUtil.isEmpty(rows)) {
            return Collections.emptyMap();
        }
        Map<String, MesDvMachineryProcessDO> result = new LinkedHashMap<>();
        for (MesDvMachineryProcessDO row : rows) {
            result.merge(buildMachineryProcessKey(row.getMachineryId(), row.getProcessId()), row,
                    this::pickConsistentMachineryProcess);
        }
        return result;
    }

    private MesDvMachineryProcessDO pickConsistentMachineryProcess(MesDvMachineryProcessDO existing,
                                                                   MesDvMachineryProcessDO current) {
        if (existing.getStandardHourlyCapacity() == null) {
            return current;
        }
        if (current.getStandardHourlyCapacity() == null
                || existing.getStandardHourlyCapacity().compareTo(current.getStandardHourlyCapacity()) == 0) {
            return existing;
        }
        throw new IllegalStateException(String.format(Locale.ROOT,
                "设备工序产能存在冲突: machineryId=%s, processId=%s",
                current.getMachineryId(), current.getProcessId()));
    }

    private String buildMachineryProcessKey(Long machineryId, Long processId) {
        return machineryId + ":" + processId;
    }

    private Comparator<MesProRouteProcessMachineryRespVO> machineryComparator() {
        return Comparator
                .comparing(MesProRouteProcessMachineryRespVO::getWorkstationId,
                        Comparator.nullsLast(Long::compareTo))
                .thenComparing(MesProRouteProcessMachineryRespVO::getMachineryCode,
                        Comparator.nullsLast(String::compareTo))
                .thenComparing(MesProRouteProcessMachineryRespVO::getMachineryId,
                        Comparator.nullsLast(Long::compareTo));
    }

    private MesProRouteProcessRespVO buildRouteProcessRespVO(MesProRouteProcessDO routeProcess) {
        if (routeProcess == null) {
            return null;
        }
        return buildRouteProcessRespVO(routeProcess, null, LocalDate.now());
    }

    private MesProRouteProcessRespVO buildRouteProcessRespVO(MesProRouteProcessDO routeProcess,
                                                             Long routeIdHint,
                                                             LocalDate calendarDate) {
        if (routeProcess == null) {
            return null;
        }
        return buildRouteProcessRespVOList(ListUtil.of(routeProcess), routeIdHint, calendarDate).get(0);
    }

    private MesProScheduleResourceAdjustmentDO resolveWorkerAdjustment(List<MesProScheduleResourceAdjustmentDO> adjustments) {
        if (CollUtil.isEmpty(adjustments)) {
            return null;
        }
        return adjustments.stream()
                .filter(item -> CAPACITY_SOURCE_WORKER.equals(item.getResourceType()))
                .findFirst()
                .orElse(null);
    }

    private MesProScheduleResourceAdjustmentDO resolveMachineAdjustment(List<MesProScheduleResourceAdjustmentDO> adjustments,
                                                                       MesMdWorkstationMachineDO machineBinding,
                                                                       Long machineryId) {
        if (CollUtil.isEmpty(adjustments)) {
            return null;
        }
        for (MesProScheduleResourceAdjustmentDO adjustment : adjustments) {
            if (!CAPACITY_SOURCE_MACHINE.equals(adjustment.getResourceType())) {
                continue;
            }
            if (adjustment.getWorkstationMachineId() != null
                    && adjustment.getWorkstationMachineId().equals(machineBinding.getId())) {
                return adjustment;
            }
            if (adjustment.getMachineryId() != null && adjustment.getMachineryId().equals(machineryId)) {
                return adjustment;
            }
        }
        return null;
    }
}
