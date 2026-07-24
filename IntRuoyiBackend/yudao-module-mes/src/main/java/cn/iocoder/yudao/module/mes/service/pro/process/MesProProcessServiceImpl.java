package cn.iocoder.yudao.module.mes.service.pro.process;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ObjUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.process.vo.MesProProcessMachineryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.process.vo.MesProProcessPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.process.vo.MesProProcessRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.scheduleconfig.MesProRouteScheduleConfigRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.repair.MesDvRepairDO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.process.vo.MesProProcessSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.repair.MesDvRepairMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.enums.dv.MesDvRepairStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProRouteFlowConfigTypeEnum;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordFormSlotType;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteScheduleConfigService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.*;

/**
 * MES 生产工序 Service 实现类
 *
 * @author 瑛泰源码
 */
@Service
@Validated
public class MesProProcessServiceImpl implements MesProProcessService {

    @Resource
    private MesProProcessMapper processMapper;
    @Resource
    private MesDvMachineryProcessMapper machineryProcessMapper;
    @Resource
    private MesDvMachineryMapper machineryMapper;
    @Resource
    private MesDvRepairMapper repairMapper;
    @Resource
    private MesProRouteMapper routeMapper;
    @Resource
    private MesProRouteFlowProcessConfigMapper routeFlowProcessConfigMapper;
    @Resource
    private MesProRouteFlowProcessBatchRecordMapper routeFlowProcessBatchRecordMapper;
    @Resource
    private MesProBatchRecordReportMapper batchRecordReportMapper;
    @Resource
    private MesMdWorkstationService workstationService;
    @Resource
    private MesProRouteVersionMapper routeVersionMapper;
    @Resource
    private MesProRouteScheduleConfigService routeScheduleConfigService;
    @Resource
    private MesProProcessContentService processContentService;
    @Resource
    @Lazy
    private MesProRouteProcessService routeProcessService;

    private static final String CAPACITY_SOURCE_MACHINE = "MACHINE";
    private static final String CAPACITY_SOURCE_WORKER = "WORKER";
    private static final String CAPACITY_SOURCE_UNCONFIGURED = "UNCONFIGURED";
    private static final String AVAILABILITY_NORMAL = "NORMAL";
    private static final String AVAILABILITY_REPAIR = "REPAIR";
    private static final String ROUTE_FLOW_TYPE_BATCH = "BATCH";
    private static final String ROUTE_FLOW_TYPE_SCHEDULE = MesProRouteFlowConfigTypeEnum.SCHEDULE.getType();
    private static final BigDecimal DEFAULT_PRODUCTION_QUANTITY_FACTOR = new BigDecimal("1.000000");

    @Override
    public Long createProcess(MesProProcessSaveReqVO createReqVO) {
        // 1. 校验编码、名称唯一
        validateProcessSaveData(createReqVO);

        // 2. 插入工序
        MesProProcessDO process = BeanUtils.toBean(createReqVO, MesProProcessDO.class);
        processMapper.insert(process);
        return process.getId();
    }

    @Override
    public void updateProcess(MesProProcessSaveReqVO updateReqVO) {
        // 1.1 校验存在
        validateProcessExists(updateReqVO.getId());
        // 1.2 校验编码、名称唯一
        validateProcessSaveData(updateReqVO);

        // 2. 更新工序
        MesProProcessDO updateObj = BeanUtils.toBean(updateReqVO, MesProProcessDO.class);
        processMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProcess(Long id) {
        // 1.1 校验存在
        validateProcessExists(id);
        // 1.2 校验是否被工艺路线引用
        if (CollUtil.isNotEmpty(routeProcessService.getRouteProcessListByProcessId(id))) {
            throw exception(PRO_PROCESS_USED_BY_ROUTE);
        }

        // 2. 删除工序
        processMapper.deleteById(id);
        // 3. 级联删除工序内容
        processContentService.deleteProcessContentByProcessId(id);
    }

    @Override
    public void validateProcessExists(Long id) {
        if (processMapper.selectById(id) == null) {
            throw exception(PRO_PROCESS_NOT_EXISTS);
        }
    }

    @Override
    public void validateProcessExistsAndEnable(Long id) {
        MesProProcessDO process = processMapper.selectById(id);
        if (process == null) {
            throw exception(PRO_PROCESS_NOT_EXISTS);
        }
        if (ObjUtil.notEqual(CommonStatusEnum.ENABLE.getStatus(), process.getStatus())) {
            throw exception(PRO_PROCESS_IS_DISABLE);
        }
    }

    private void validateProcessCodeUnique(Long id, String code) {
        MesProProcessDO process = processMapper.selectByProductNameAndCode(null, code);
        if (process == null) {
            return;
        }
        if (id == null) {
            throw exception(PRO_PROCESS_CODE_EXISTS);
        }
        if (!process.getId().equals(id)) {
            throw exception(PRO_PROCESS_CODE_EXISTS);
        }
    }

    private void validateProcessNameUnique(Long id, String name) {
        MesProProcessDO process = processMapper.selectByProductNameAndName(null, name);
        if (process == null) {
            return;
        }
        if (id == null) {
            throw exception(PRO_PROCESS_NAME_EXISTS);
        }
        if (!process.getId().equals(id)) {
            throw exception(PRO_PROCESS_NAME_EXISTS);
        }
    }

    private void validateProcessSaveData(MesProProcessSaveReqVO saveReqVO) {
        // 1. 校验编码唯一
        validateProcessCodeUnique(saveReqVO.getId(), saveReqVO.getProductName(), saveReqVO.getCode());
    }

    private void validateProcessCodeUnique(Long id, String productName, String code) {
        MesProProcessDO process = processMapper.selectByProductNameAndCode(normalizeProductName(productName), code);
        if (process == null) {
            return;
        }
        if (id == null) {
            throw exception(PRO_PROCESS_CODE_EXISTS);
        }
        if (!process.getId().equals(id)) {
            throw exception(PRO_PROCESS_CODE_EXISTS);
        }
    }

    private void validateProcessNameUnique(Long id, String productName, String name) {
        MesProProcessDO process = processMapper.selectByProductNameAndName(normalizeProductName(productName), name);
        if (process == null) {
            return;
        }
        if (id == null) {
            throw exception(PRO_PROCESS_NAME_EXISTS);
        }
        if (!process.getId().equals(id)) {
            throw exception(PRO_PROCESS_NAME_EXISTS);
        }
    }

    private String normalizeProductName(String productName) {
        String normalized = StrUtil.trim(productName);
        return StrUtil.isBlank(normalized) ? null : normalized;
    }

    @Override
    public MesProProcessDO getProcess(Long id) {
        return processMapper.selectById(id);
    }

    @Override
    public MesProProcessRespVO getProcessWithCapacity(Long id, Long routeId) {
        MesProProcessDO process = processMapper.selectById(id);
        if (process == null) {
            return null;
        }
        MesProProcessRespVO respVO = BeanUtils.toBean(process, MesProProcessRespVO.class);
        enrichProcessRespList(List.of(respVO), routeId);
        return respVO;
    }

    @Override
    public List<MesProProcessDO> getProcessList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return processMapper.selectByIds(ids);
    }

    @Override
    public PageResult<MesProProcessDO> getProcessPage(MesProProcessPageReqVO pageReqVO) {
        return processMapper.selectPage(pageReqVO);
    }

    @Override
    public PageResult<MesProProcessRespVO> getProcessPageWithCapacity(MesProProcessPageReqVO pageReqVO) {
        PageResult<MesProProcessDO> pageResult = processMapper.selectPage(pageReqVO);
        List<MesProProcessRespVO> respList = BeanUtils.toBean(pageResult.getList(), MesProProcessRespVO.class);
        if (CollUtil.isEmpty(respList)) {
            return new PageResult<>(respList, pageResult.getTotal());
        }
        enrichProcessRespList(respList, pageReqVO.getRouteId());
        return new PageResult<>(respList, pageResult.getTotal());
    }

    private void enrichProcessRespList(List<MesProProcessRespVO> respList, Long routeId) {
        Map<Long, MesProProcessRespVO> respMap = respList.stream()
                .collect(Collectors.toMap(MesProProcessRespVO::getId, item -> item, (a, b) -> a, LinkedHashMap::new));
        Map<Long, List<MesProProcessMachineryRespVO>> machineryMap = buildMachineryRespMap(respMap.keySet());
        Map<Long, BigDecimal> defaultShiftCapacityMap = buildDefaultShiftCapacityMap(respList, machineryMap);
        Map<Long, List<MesProProcessRespVO.RouteSimpleRespVO>> routeMap =
                buildRouteRespMap(respMap.keySet(), routeId, defaultShiftCapacityMap);
        Map<Long, WorkstationSummary> workstationSummaryMap = buildWorkstationSummaryMap(respMap.keySet(), routeId);
        Map<Long, BatchRecordSummary> batchRecordSummaryMap =
                buildBatchRecordSummaryMap(respMap.keySet(), routeId);
        Map<Long, ScheduleConfigSummary> scheduleConfigSummaryMap =
                buildScheduleConfigSummaryMap(respMap.keySet(), routeId);
        for (MesProProcessRespVO respVO : respList) {
            List<MesProProcessRespVO.RouteSimpleRespVO> routeList =
                    routeMap.getOrDefault(respVO.getId(), Collections.emptyList());
            respVO.setRouteList(routeList);
            applyRouteCapacityConflict(respVO, routeList, routeId);
            WorkstationSummary workstationSummary = workstationSummaryMap.get(respVO.getId());
            if (workstationSummary != null) {
                respVO.setWorkstationNames(workstationSummary.workstationNames());
                respVO.setWorkstations(workstationSummary.workstations());
            } else {
                respVO.setWorkstationNames("");
                respVO.setWorkstations(Collections.emptyList());
            }
            ScheduleConfigSummary scheduleConfigSummary = scheduleConfigSummaryMap.get(respVO.getId());
            if (scheduleConfigSummary != null) {
                respVO.setProductionQuantityFactor(scheduleConfigSummary.productionQuantityFactor());
                respVO.setShiftCapacity(scheduleConfigSummary.shiftCapacity());
            }
            BatchRecordSummary batchRecordSummary = batchRecordSummaryMap.get(respVO.getId());
            if (batchRecordSummary != null) {
                respVO.setBatchRecordFormNames(batchRecordSummary.batchRecordFormNames());
                respVO.setBatchRecordForms(batchRecordSummary.batchRecordForms());
                respVO.setLossReportFormNames(batchRecordSummary.lossReportFormNames());
                respVO.setLossReportForms(batchRecordSummary.lossReportForms());
                respVO.setProcessInspectionFormNames(batchRecordSummary.processInspectionFormNames());
                respVO.setProcessInspectionForms(batchRecordSummary.processInspectionForms());
                respVO.setParameterRecordFormNames(batchRecordSummary.parameterRecordFormNames());
                respVO.setParameterRecordForms(batchRecordSummary.parameterRecordForms());
            }
            List<MesProProcessMachineryRespVO> machineryList = machineryMap.getOrDefault(respVO.getId(), Collections.emptyList());
            if (CollUtil.isNotEmpty(machineryList)) {
                respVO.setMachineryQuantityTotal(machineryList.size());
                respVO.setAvailableShiftCapacityTotal(defaultShiftCapacityMap.get(respVO.getId()));
                respVO.setCapacitySource(CAPACITY_SOURCE_MACHINE);
            } else if (respVO.getManualShiftCapacity() != null) {
                respVO.setMachineryQuantityTotal(null);
                respVO.setAvailableShiftCapacityTotal(defaultShiftCapacityMap.get(respVO.getId()));
                respVO.setCapacitySource(CAPACITY_SOURCE_WORKER);
            } else {
                respVO.setMachineryQuantityTotal(null);
                respVO.setAvailableShiftCapacityTotal(null);
                respVO.setCapacitySource(CAPACITY_SOURCE_UNCONFIGURED);
            }
        }
    }

    @Override
    public List<MesProProcessMachineryRespVO> getProcessMachineryList(Long processId) {
        validateProcessExists(processId);
        return buildMachineryRespMap(Set.of(processId)).getOrDefault(processId, Collections.emptyList());
    }

    private Map<Long, BigDecimal> buildDefaultShiftCapacityMap(Collection<MesProProcessRespVO> respList,
                                                               Map<Long, List<MesProProcessMachineryRespVO>> machineryMap) {
        if (CollUtil.isEmpty(respList)) {
            return Collections.emptyMap();
        }
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        for (MesProProcessRespVO respVO : respList) {
            List<MesProProcessMachineryRespVO> machineryList =
                    machineryMap.getOrDefault(respVO.getId(), Collections.emptyList());
            if (CollUtil.isNotEmpty(machineryList)) {
                result.put(respVO.getId(), sumAvailableShiftCapacity(machineryList));
            } else if (respVO.getManualShiftCapacity() != null) {
                result.put(respVO.getId(), respVO.getManualShiftCapacity());
            }
        }
        return result;
    }

    @Override
    public List<MesProProcessDO> getProcessListByStatus(Integer status) {
        return processMapper.selectListByStatus(status);
    }

    private Map<Long, List<MesProProcessRespVO.RouteSimpleRespVO>> buildRouteRespMap(Collection<Long> processIds,
                                                                                     Long capacityRouteId,
                                                                                     Map<Long, BigDecimal> defaultShiftCapacityByProcessId) {
        if (CollUtil.isEmpty(processIds)) {
            return Collections.emptyMap();
        }
        RouteProcessContext routeProcessContext = buildRouteProcessContext(processIds, capacityRouteId);
        List<MesProRouteProcessDO> routeProcesses = routeProcessContext.routeProcesses();
        if (CollUtil.isEmpty(routeProcesses)) {
            return Collections.emptyMap();
        }
        Map<Long, MesProRouteDO> routeMap = routeProcessContext.routeMap();
        List<MesProRouteProcessDO> capacityRouteProcesses = routeProcesses;
        Set<Long> capacityRouteProcessIds = capacityRouteProcesses.stream()
                .map(MesProRouteProcessDO::getId)
                .filter(ObjUtil::isNotNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, BigDecimal> shiftCapacityByRouteProcessId =
                buildShiftCapacityByRouteProcessId(capacityRouteProcesses, capacityRouteProcessIds);
        Map<Long, List<MesProProcessRespVO.RouteSimpleRespVO>> result = new LinkedHashMap<>();
        for (MesProRouteProcessDO routeProcess : routeProcesses) {
            Long processId = routeProcess.getProcessId();
            MesProRouteDO route = routeMap.get(routeProcess.getRouteId());
            if (route == null) {
                throw new IllegalStateException("Missing route master: processId=" + processId
                        + ", routeId=" + routeProcess.getRouteId());
            }
            MesProProcessRespVO.RouteSimpleRespVO routeRespVO = new MesProProcessRespVO.RouteSimpleRespVO();
            routeRespVO.setId(route.getId());
            routeRespVO.setRouteProcessId(routeProcess.getId());
            routeRespVO.setCode(route.getCode());
            routeRespVO.setName(route.getName());
            routeRespVO.setShiftCapacity(shiftCapacityByRouteProcessId.getOrDefault(routeProcess.getId(),
                    defaultShiftCapacityByProcessId.get(processId)));
            result.computeIfAbsent(processId, key -> new ArrayList<>()).add(routeRespVO);
        }
        return result;
    }

    private RouteProcessContext buildRouteProcessContext(Collection<Long> processIds, Long routeId) {
        if (CollUtil.isEmpty(processIds)) {
            return RouteProcessContext.empty();
        }
        List<MesProRouteProcessDO> routeProcesses = routeProcessService.getRouteProcessListByProcessIds(processIds);
        if (CollUtil.isEmpty(routeProcesses)) {
            return RouteProcessContext.empty();
        }
        if (routeId != null) {
            List<MesProRouteProcessDO> scopedRouteProcesses = routeProcesses.stream()
                    .filter(routeProcess -> routeId.equals(routeProcess.getRouteId()))
                    .toList();
            return new RouteProcessContext(scopedRouteProcesses, buildRouteMap(scopedRouteProcesses));
        }
        Map<Long, MesProRouteDO> routeMap = buildRouteMap(routeProcesses);
        if (CollUtil.isEmpty(routeMap)) {
            return RouteProcessContext.empty();
        }
        List<MesProRouteProcessDO> visibleRouteProcesses = routeProcesses.stream()
                .filter(routeProcess -> routeMap.containsKey(routeProcess.getRouteId()))
                .toList();
        return new RouteProcessContext(visibleRouteProcesses, routeMap);
    }

    private void applyRouteCapacityConflict(MesProProcessRespVO respVO,
                                            List<MesProProcessRespVO.RouteSimpleRespVO> routeList,
                                            Long routeId) {
        if (routeId != null || CollUtil.isEmpty(routeList) || routeList.size() < 2) {
            respVO.setRouteCapacityConflict(Boolean.FALSE);
            respVO.setRouteCapacityConflictMessage(null);
            return;
        }
        BigDecimal firstCapacity = null;
        for (MesProProcessRespVO.RouteSimpleRespVO route : routeList) {
            BigDecimal shiftCapacity = route.getShiftCapacity();
            if (shiftCapacity == null) {
                continue;
            }
            if (firstCapacity == null) {
                firstCapacity = shiftCapacity;
                continue;
            }
            if (firstCapacity.compareTo(shiftCapacity) != 0) {
                respVO.setRouteCapacityConflict(Boolean.TRUE);
                respVO.setRouteCapacityConflictMessage(
                        "多条工艺路线的排产产能不一致，请进入工艺流程使用覆盖产能处理。");
                return;
            }
        }
        respVO.setRouteCapacityConflict(Boolean.FALSE);
        respVO.setRouteCapacityConflictMessage(null);
    }

    private Map<Long, WorkstationSummary> buildWorkstationSummaryMap(Collection<Long> processIds, Long routeId) {
        if (CollUtil.isEmpty(processIds)) {
            return Collections.emptyMap();
        }
        Map<Long, MutableWorkstationSummary> result = new LinkedHashMap<>();
        List<MesProRouteProcessDO> routeProcesses = buildRouteProcessContext(processIds, routeId).routeProcesses();
        Set<Long> routeProcessBoundProcessIds = addRouteProcessWorkstationsToSummary(result, routeProcesses);
        addWorkstationsToSummary(result, workstationService.getWorkstationListByProcessIds(processIds),
                routeProcessBoundProcessIds);

        if (result.isEmpty()) {
            return Collections.emptyMap();
        }
        return result.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, item -> item.getValue().toSummary(),
                        (a, b) -> a, LinkedHashMap::new));
    }

    private void addWorkstationsToSummary(Map<Long, MutableWorkstationSummary> result,
                                          List<MesMdWorkstationDO> workstations,
                                          Set<Long> excludedProcessIds) {
        if (CollUtil.isEmpty(workstations)) {
            return;
        }
        Set<Long> excludedIds = excludedProcessIds == null ? Collections.emptySet() : excludedProcessIds;
        for (MesMdWorkstationDO workstation : workstations) {
            Long processId = workstation.getProcessId();
            if (processId == null || excludedIds.contains(processId)) {
                continue;
            }
            result.computeIfAbsent(processId, key -> new MutableWorkstationSummary()).add(workstation);
        }
    }

    private Set<Long> addRouteProcessWorkstationsToSummary(Map<Long, MutableWorkstationSummary> result,
                                                           List<MesProRouteProcessDO> routeProcesses) {
        if (CollUtil.isEmpty(routeProcesses)) {
            return Collections.emptySet();
        }
        Set<Long> workstationIds = routeProcesses.stream()
                .map(MesProRouteProcessDO::getWorkstationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (CollUtil.isEmpty(workstationIds)) {
            return Collections.emptySet();
        }
        List<MesMdWorkstationDO> routeWorkstations = workstationService.getWorkstationList(workstationIds);
        Map<Long, MesMdWorkstationDO> workstationMap = CollUtil.isEmpty(routeWorkstations)
                ? Collections.emptyMap() : routeWorkstations.stream()
                .filter(workstation -> workstation.getId() != null)
                .collect(Collectors.toMap(MesMdWorkstationDO::getId, item -> item, (a, b) -> a, LinkedHashMap::new));
        Map<Long, Long> processIdentityMap = loadRouteProcessIdentityMap(routeProcesses);
        Set<Long> boundProcessIds = new LinkedHashSet<>();
        for (MesProRouteProcessDO routeProcess : routeProcesses) {
            Long workstationId = routeProcess.getWorkstationId();
            if (workstationId == null) {
                continue;
            }
            MesMdWorkstationDO workstation = workstationMap.get(workstationId);
            if (workstation == null) {
                throw new IllegalStateException("路线工序绑定的工作站不存在: routeProcessId="
                        + routeProcess.getId() + ", workstationId=" + workstationId);
            }
            Long normalizedWorkstationProcessId = normalizeProcessIdentity(workstation.getProcessId(), processIdentityMap);
            if (!Objects.equals(normalizedWorkstationProcessId, routeProcess.getProcessId())) {
                throw new IllegalStateException("路线工序绑定的工作站工序不一致: routeProcessId="
                        + routeProcess.getId() + ", processId=" + routeProcess.getProcessId()
                        + ", workstationId=" + workstation.getId()
                        + ", workstationProcessId=" + workstation.getProcessId());
            }
            workstation.setProcessId(normalizedWorkstationProcessId);
            result.computeIfAbsent(routeProcess.getProcessId(), key -> new MutableWorkstationSummary()).add(workstation);
            boundProcessIds.add(routeProcess.getProcessId());
        }
        return boundProcessIds;
    }

    private Map<Long, Long> loadRouteProcessIdentityMap(List<MesProRouteProcessDO> routeProcesses) {
        Set<Long> routeProcessProcessIds = routeProcesses.stream()
                .map(MesProRouteProcessDO::getProcessId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (CollUtil.isEmpty(routeProcessProcessIds)) {
            return Collections.emptyMap();
        }
        return routeProcessService.getProcessIdentityMap(routeProcessProcessIds);
    }

    private Long normalizeProcessIdentity(Long processId, Map<Long, Long> processIdentityMap) {
        if (processId == null || processIdentityMap == null) {
            return processId;
        }
        return processIdentityMap.getOrDefault(processId, processId);
    }

    private Map<Long, ScheduleConfigSummary> buildScheduleConfigSummaryMap(Collection<Long> processIds, Long routeId) {
        if (CollUtil.isEmpty(processIds)) {
            return Collections.emptyMap();
        }
        RouteProcessContext routeProcessContext = buildRouteProcessContext(processIds, routeId);
        List<MesProRouteProcessDO> routeProcesses = routeProcessContext.routeProcesses();
        if (CollUtil.isEmpty(routeProcesses)) {
            return Collections.emptyMap();
        }
        Map<Long, Long> processIdByRouteProcessId = routeProcesses.stream()
                .collect(Collectors.toMap(MesProRouteProcessDO::getId, MesProRouteProcessDO::getProcessId,
                        (a, b) -> a, LinkedHashMap::new));
        Map<Long, MesProRouteDO> routeMap = routeProcessContext.routeMap();
        Set<Long> routeProcessIds = processIdByRouteProcessId.keySet();
        List<MesProRouteFlowProcessConfigDO> flowConfigs =
                routeFlowProcessConfigMapper.selectListByRouteProcessIdsAndUseType(routeProcessIds, ROUTE_FLOW_TYPE_SCHEDULE);
        Map<Long, MesProRouteFlowProcessConfigDO> flowConfigByRouteProcessId =
                CollUtil.isEmpty(flowConfigs) ? Collections.emptyMap() : flowConfigs.stream()
                        .filter(item -> processIdByRouteProcessId.containsKey(item.getRouteProcessId()))
                        .filter(item -> routeId == null || routeId.equals(item.getRouteId()))
                        .collect(Collectors.toMap(MesProRouteFlowProcessConfigDO::getRouteProcessId,
                                item -> item, (a, b) -> a, LinkedHashMap::new));
        Map<Long, BigDecimal> shiftCapacityByRouteProcessId =
                buildShiftCapacityByRouteProcessId(routeProcesses, routeProcessIds);

        Map<Long, ScheduleConfigSummary> result = new LinkedHashMap<>();
        routeProcesses.stream()
                .sorted(Comparator
                        .comparingInt((MesProRouteProcessDO routeProcess) ->
                                routeDisplayPriority(routeMap.get(routeProcess.getRouteId())))
                        .thenComparing(routeProcess -> routeSortKey(routeProcess.getRouteId()))
                        .thenComparing(routeProcess -> reportSortKey(routeProcess.getSort()))
                        .thenComparing(routeProcess -> idSortKey(routeProcess.getId())))
                .forEach(routeProcess -> {
                    Long processId = processIdByRouteProcessId.get(routeProcess.getId());
                    if (processId == null || result.containsKey(processId)) {
                        return;
                    }
                    MesProRouteFlowProcessConfigDO flowConfig = flowConfigByRouteProcessId.get(routeProcess.getId());
                    BigDecimal productionQuantityFactor = flowConfig == null
                            || flowConfig.getProductionQuantityFactor() == null
                            ? DEFAULT_PRODUCTION_QUANTITY_FACTOR : flowConfig.getProductionQuantityFactor();
                    BigDecimal shiftCapacity = shiftCapacityByRouteProcessId.get(routeProcess.getId());
                    result.put(processId, new ScheduleConfigSummary(productionQuantityFactor, shiftCapacity));
                });
        return result;
    }

    private Map<Long, BigDecimal> buildShiftCapacityByRouteProcessId(List<MesProRouteProcessDO> routeProcesses,
                                                                      Set<Long> routeProcessIds) {
        if (CollUtil.isEmpty(routeProcesses) || CollUtil.isEmpty(routeProcessIds)) {
            return Collections.emptyMap();
        }
        Set<Long> routeIds = routeProcesses.stream()
                .map(MesProRouteProcessDO::getRouteId)
                .filter(ObjUtil::isNotNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<MesProRouteVersionDO> routeVersions = routeVersionMapper.selectListByRouteIds(routeIds);
        if (CollUtil.isEmpty(routeVersions)) {
            return Collections.emptyMap();
        }
        List<MesProRouteVersionDO> activeRouteVersions = routeVersions.stream()
                .filter(routeVersion -> Boolean.TRUE.equals(routeVersion.getActive()))
                .filter(routeVersion -> routeIds.contains(routeVersion.getRouteId()))
                .collect(Collectors.toMap(MesProRouteVersionDO::getRouteId,
                        item -> item, (first, second) -> first, LinkedHashMap::new))
                .values()
                .stream()
                .toList();
        if (CollUtil.isEmpty(activeRouteVersions)) {
            return Collections.emptyMap();
        }
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        for (MesProRouteVersionDO routeVersion : activeRouteVersions) {
            List<MesProRouteScheduleConfigRespVO> scheduleConfigs =
                    routeScheduleConfigService.getConfigRespListByRouteVersionId(routeVersion.getId());
            if (CollUtil.isEmpty(scheduleConfigs)) {
                continue;
            }
            for (MesProRouteScheduleConfigRespVO scheduleConfig : scheduleConfigs) {
                Long routeProcessId = scheduleConfig.getRouteProcessId();
                if (routeProcessId == null || !routeProcessIds.contains(routeProcessId)) {
                    continue;
                }
                result.putIfAbsent(routeProcessId, scheduleConfig.getStandardShiftCapacity());
            }
        }
        return result;
    }

    private Map<Long, BatchRecordSummary> buildBatchRecordSummaryMap(Collection<Long> processIds, Long routeId) {
        if (CollUtil.isEmpty(processIds)) {
            return Collections.emptyMap();
        }
        RouteProcessContext routeProcessContext = buildRouteProcessContext(processIds, routeId);
        List<MesProRouteProcessDO> routeProcesses = routeProcessContext.routeProcesses();
        if (CollUtil.isEmpty(routeProcesses)) {
            return Collections.emptyMap();
        }
        Map<Long, Long> processIdByRouteProcessId = routeProcesses.stream()
                .collect(Collectors.toMap(MesProRouteProcessDO::getId, MesProRouteProcessDO::getProcessId,
                        (a, b) -> a, LinkedHashMap::new));
        Map<Long, Long> routeIdByRouteProcessId = routeProcesses.stream()
                .collect(Collectors.toMap(MesProRouteProcessDO::getId, MesProRouteProcessDO::getRouteId,
                        (a, b) -> a, LinkedHashMap::new));
        Map<Long, MesProRouteDO> routeMap = routeProcessContext.routeMap();
        Set<Long> routeProcessIds = processIdByRouteProcessId.keySet();
        List<MesProRouteFlowProcessConfigDO> configs =
                routeFlowProcessConfigMapper.selectListByRouteProcessIdsAndUseType(routeProcessIds, ROUTE_FLOW_TYPE_BATCH);
        if (CollUtil.isEmpty(configs)) {
            return Collections.emptyMap();
        }
        Set<Long> enabledRouteProcessIds = configs.stream()
                .filter(item -> processIdByRouteProcessId.containsKey(item.getRouteProcessId()))
                .filter(item -> routeId == null || routeId.equals(item.getRouteId()))
                .map(MesProRouteFlowProcessConfigDO::getRouteProcessId)
                .filter(ObjUtil::isNotNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (CollUtil.isEmpty(enabledRouteProcessIds)) {
            return Collections.emptyMap();
        }

        List<MesProRouteFlowProcessBatchRecordDO> batchRecords =
                routeFlowProcessBatchRecordMapper.selectListByRouteProcessIdsAndUseType(
                        enabledRouteProcessIds, ROUTE_FLOW_TYPE_BATCH);
        if (CollUtil.isEmpty(batchRecords)) {
            return Collections.emptyMap();
        }
        Set<String> reportIds = batchRecords.stream()
                .map(MesProRouteFlowProcessBatchRecordDO::getBatchRecordReportId)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, MesProBatchRecordReportDO> reportMap = CollUtil.isEmpty(reportIds) ? Collections.emptyMap()
                : batchRecordReportMapper.selectListByReportIds(reportIds).stream()
                .collect(Collectors.toMap(MesProBatchRecordReportDO::getReportId, item -> item,
                        (a, b) -> a, LinkedHashMap::new));

        Map<Long, MutableBatchRecordSummary> result = new LinkedHashMap<>();
        List<MesProRouteFlowProcessBatchRecordDO> sortedBatchRecords = batchRecords.stream()
                .sorted(Comparator
                        .comparingInt((MesProRouteFlowProcessBatchRecordDO batchRecord) ->
                                routeDisplayPriority(routeMap.get(routeIdByRouteProcessId.get(batchRecord.getRouteProcessId()))))
                        .thenComparing(batchRecord -> routeSortKey(routeIdByRouteProcessId.get(batchRecord.getRouteProcessId())))
                        .thenComparing(batchRecord -> reportSortKey(batchRecord.getReportSort()))
                        .thenComparing(batchRecord -> idSortKey(batchRecord.getId())))
                .toList();
        for (MesProRouteFlowProcessBatchRecordDO batchRecord : sortedBatchRecords) {
            Long processId = processIdByRouteProcessId.get(batchRecord.getRouteProcessId());
            if (processId == null) {
                continue;
            }
            String reportId = batchRecord.getBatchRecordReportId();
            if (StrUtil.isBlank(reportId)) {
                continue;
            }
            MesProBatchRecordReportDO report = reportMap.get(reportId);
            if (report == null) {
                throw new IllegalStateException("Missing batch record report: routeProcessId="
                        + batchRecord.getRouteProcessId() + ", reportId=" + reportId);
            }
            MutableBatchRecordSummary summary =
                    result.computeIfAbsent(processId, key -> new MutableBatchRecordSummary());
            String formSlotType = resolveFormSlotType(batchRecord, report);
            if (MesProBatchRecordFormSlotType.MAIN.getType().equals(formSlotType)) {
                if (summary.hasBatchRecordForm()) {
                    continue;
                }
                summary.batchRecordFormNames.add(report.getReportName());
                summary.batchRecordForms.add(new BatchRecordFormLink(report.getReportId(), report.getReportName()));
            } else if (MesProBatchRecordFormSlotType.LOSS_REPORT.getType().equals(formSlotType)) {
                summary.lossReportFormNames.add(report.getReportName());
                summary.lossReportForms.add(new BatchRecordFormLink(report.getReportId(), report.getReportName()));
            } else if (MesProBatchRecordFormSlotType.PROCESS_INSPECTION.getType().equals(formSlotType)) {
                summary.processInspectionFormNames.add(report.getReportName());
                summary.processInspectionForms.add(new BatchRecordFormLink(report.getReportId(), report.getReportName()));
            } else if (MesProBatchRecordFormSlotType.PARAMETER_RECORD.getType().equals(formSlotType)) {
                summary.parameterRecordFormNames.add(report.getReportName());
                summary.parameterRecordForms.add(new BatchRecordFormLink(report.getReportId(), report.getReportName()));
            }
        }
        return result.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, item -> item.getValue().toSummary(),
                        (a, b) -> a, LinkedHashMap::new));
    }

    private Map<Long, MesProRouteDO> buildRouteMap(List<MesProRouteProcessDO> routeProcesses) {
        List<Long> routeIds = routeProcesses.stream()
                .map(MesProRouteProcessDO::getRouteId)
                .filter(ObjUtil::isNotNull)
                .distinct()
                .toList();
        if (CollUtil.isEmpty(routeIds)) {
            return Collections.emptyMap();
        }
        return routeMapper.selectBatchIds(routeIds).stream()
                .collect(Collectors.toMap(MesProRouteDO::getId, item -> item, (a, b) -> a, LinkedHashMap::new));
    }

    private String resolveFormSlotType(MesProRouteFlowProcessBatchRecordDO batchRecord, MesProBatchRecordReportDO report) {
        String rawFormSlotType = StrUtil.trim(batchRecord.getFormSlotType());
        String formSlotType = StrUtil.isBlank(rawFormSlotType)
                ? MesProBatchRecordFormSlotType.normalize(report.getFormSlotType())
                : MesProBatchRecordFormSlotType.normalize(rawFormSlotType);
        if (formSlotType == null) {
            throw new IllegalStateException("Invalid batch record form slot type: routeProcessId="
                    + batchRecord.getRouteProcessId() + ", reportId=" + batchRecord.getBatchRecordReportId()
                    + ", formSlotType=" + StrUtil.blankToDefault(rawFormSlotType, report.getFormSlotType()));
        }
        return formSlotType;
    }

    private int routeDisplayPriority(MesProRouteDO route) {
        if (route == null) {
            return 1;
        }
        String routeName = StrUtil.trim(route.getName());
        if (StrUtil.startWith(routeName, "E2E-") || StrUtil.startWith(routeName, "ADMIN-")) {
            return 1;
        }
        return 0;
    }

    private Long routeSortKey(Long routeId) {
        return routeId == null ? Long.MAX_VALUE : routeId;
    }

    private Integer reportSortKey(Integer reportSort) {
        return reportSort == null ? Integer.MAX_VALUE : reportSort;
    }

    private Long idSortKey(Long id) {
        return id == null ? Long.MAX_VALUE : id;
    }

    private Map<Long, List<MesProProcessMachineryRespVO>> buildMachineryRespMap(Collection<Long> processIds) {
        if (CollUtil.isEmpty(processIds)) {
            return Collections.emptyMap();
        }
        Map<Long, Long> identityMap = routeProcessService.getProcessIdentityMap(processIds);
        List<MesDvMachineryProcessDO> processRows =
                machineryProcessMapper.selectListByProcessIds(new ArrayList<>(identityMap.keySet()));
        if (CollUtil.isEmpty(processRows)) {
            return Collections.emptyMap();
        }
        processRows = processRows.stream()
                .filter(row -> row.getMachineryId() != null)
                .toList();
        if (CollUtil.isEmpty(processRows)) {
            return Collections.emptyMap();
        }
        processRows.forEach(row -> {
            Long currentProcessId = identityMap.get(row.getProcessId());
            if (currentProcessId != null) {
                row.setProcessId(currentProcessId);
            }
        });
        Set<Long> machineryIds = processRows.stream()
                .map(MesDvMachineryProcessDO::getMachineryId)
                .filter(ObjUtil::isNotNull)
                .collect(Collectors.toSet());
        Map<Long, MesDvMachineryDO> machineryMap = CollUtil.isEmpty(machineryIds) ? Collections.emptyMap()
                : machineryMapper.selectBatchIds(machineryIds).stream()
                .collect(Collectors.toMap(MesDvMachineryDO::getId, item -> item, (a, b) -> a));
        Map<Long, MesDvRepairDO> activeRepairMap = buildActiveRepairMap(machineryIds);

        Map<Long, List<MesProProcessMachineryRespVO>> result = new LinkedHashMap<>();
        for (MesDvMachineryProcessDO processRow : processRows) {
            Long processId = processRow.getProcessId();
            Long machineryId = processRow.getMachineryId();
            MesDvMachineryDO machinery = machineryMap.get(machineryId);
            if (machinery == null) {
                throw new IllegalStateException("Missing machinery master: processId=" + processId
                        + ", machineryId=" + machineryId);
            }
            MesProProcessMachineryRespVO respVO = buildMachineryRespVO(processRow, machinery,
                    activeRepairMap.get(machineryId));
            result.computeIfAbsent(processId, key -> new ArrayList<>()).add(respVO);
        }
        return result;
    }

    private Map<Long, MesDvRepairDO> buildActiveRepairMap(Collection<Long> machineryIds) {
        if (CollUtil.isEmpty(machineryIds)) {
            return Collections.emptyMap();
        }
        List<MesDvRepairDO> repairs = repairMapper.selectListByMachineryIdsAndStatuses(machineryIds, List.of(
                MesDvRepairStatusEnum.CONFIRMED.getStatus(),
                MesDvRepairStatusEnum.APPROVING.getStatus()));
        Map<Long, MesDvRepairDO> result = new LinkedHashMap<>();
        for (MesDvRepairDO repair : repairs) {
            result.putIfAbsent(repair.getMachineryId(), repair);
        }
        return result;
    }

    private MesProProcessMachineryRespVO buildMachineryRespVO(MesDvMachineryProcessDO processRow,
                                                               MesDvMachineryDO machinery,
                                                               MesDvRepairDO activeRepair) {
        MesProProcessMachineryRespVO respVO = new MesProProcessMachineryRespVO();
        respVO.setMachineryId(machinery.getId());
        respVO.setMachineryCode(machinery.getCode());
        respVO.setMachineryName(machinery.getName());
        respVO.setMachineryStatus(machinery.getStatus());
        respVO.setShiftCapacity(processRow.getTenHalfHourDailyCapacity());
        boolean underRepair = activeRepair != null;
        respVO.setUnderRepair(underRepair);
        respVO.setAvailableShiftCapacity(underRepair ? BigDecimal.ZERO : processRow.getTenHalfHourDailyCapacity());
        respVO.setAvailabilityStatus(underRepair ? AVAILABILITY_REPAIR : AVAILABILITY_NORMAL);
        respVO.setAvailabilityReason(underRepair ? buildRepairReason(activeRepair) : null);
        return respVO;
    }

    private String buildRepairReason(MesDvRepairDO repair) {
        String repairName = StrUtil.blankToDefault(repair.getName(), "维修工单");
        if (StrUtil.isNotBlank(repair.getCode())) {
            return repairName + "（" + repair.getCode() + "）";
        }
        return repairName;
    }

    private BigDecimal sumAvailableShiftCapacity(List<MesProProcessMachineryRespVO> machineryList) {
        return machineryList.stream()
                .map(MesProProcessMachineryRespVO::getAvailableShiftCapacity)
                .filter(ObjUtil::isNotNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private record ScheduleConfigSummary(BigDecimal productionQuantityFactor, BigDecimal shiftCapacity) {
    }

    private record WorkstationSummary(String workstationNames,
                                      List<MesProProcessRespVO.WorkstationSimpleRespVO> workstations) {
    }

    private record BatchRecordSummary(String batchRecordFormNames,
                                      List<MesProProcessRespVO.BatchRecordFormLinkRespVO> batchRecordForms,
                                      String lossReportFormNames,
                                      List<MesProProcessRespVO.BatchRecordFormLinkRespVO> lossReportForms,
                                      String processInspectionFormNames,
                                      List<MesProProcessRespVO.BatchRecordFormLinkRespVO> processInspectionForms,
                                      String parameterRecordFormNames,
                                      List<MesProProcessRespVO.BatchRecordFormLinkRespVO> parameterRecordForms) {
    }

    private record BatchRecordFormLink(String reportId, String reportName) {
    }

    private record RouteProcessContext(List<MesProRouteProcessDO> routeProcesses,
                                       Map<Long, MesProRouteDO> routeMap) {

        private static RouteProcessContext empty() {
            return new RouteProcessContext(Collections.emptyList(), Collections.emptyMap());
        }
    }

    private static class MutableWorkstationSummary {

        private final Set<String> workstationNames = new LinkedHashSet<>();
        private final Set<Long> workstationIds = new LinkedHashSet<>();
        private final List<MesProProcessRespVO.WorkstationSimpleRespVO> workstations = new ArrayList<>();

        private void add(MesMdWorkstationDO workstation) {
            Long workstationId = workstation.getId();
            if (workstationId != null && !workstationIds.add(workstationId)) {
                return;
            }
            MesProProcessRespVO.WorkstationSimpleRespVO respVO =
                    new MesProProcessRespVO.WorkstationSimpleRespVO();
            respVO.setId(workstationId);
            respVO.setCode(workstation.getCode());
            respVO.setName(workstation.getName());
            workstations.add(respVO);

            String displayName = formatWorkstationDisplayName(workstation);
            if (StrUtil.isNotBlank(displayName)) {
                workstationNames.add(displayName);
            }
        }

        private WorkstationSummary toSummary() {
            return new WorkstationSummary(joinNames(workstationNames), List.copyOf(workstations));
        }

    }

    private static class MutableBatchRecordSummary {

        private final Set<String> batchRecordFormNames = new LinkedHashSet<>();
        private final Set<BatchRecordFormLink> batchRecordForms = new LinkedHashSet<>();
        private final Set<String> lossReportFormNames = new LinkedHashSet<>();
        private final Set<BatchRecordFormLink> lossReportForms = new LinkedHashSet<>();
        private final Set<String> processInspectionFormNames = new LinkedHashSet<>();
        private final Set<BatchRecordFormLink> processInspectionForms = new LinkedHashSet<>();
        private final Set<String> parameterRecordFormNames = new LinkedHashSet<>();
        private final Set<BatchRecordFormLink> parameterRecordForms = new LinkedHashSet<>();

        private boolean hasBatchRecordForm() {
            return CollUtil.isNotEmpty(batchRecordForms);
        }

        private BatchRecordSummary toSummary() {
            return new BatchRecordSummary(joinNames(batchRecordFormNames), toBatchRecordFormRespVOs(batchRecordForms),
                    joinNames(lossReportFormNames), toBatchRecordFormRespVOs(lossReportForms),
                    joinNames(processInspectionFormNames), toBatchRecordFormRespVOs(processInspectionForms),
                    joinNames(parameterRecordFormNames), toBatchRecordFormRespVOs(parameterRecordForms));
        }

        private static String joinNames(Collection<String> names) {
            return names.stream()
                    .filter(Objects::nonNull)
                    .map(StrUtil::trim)
                    .filter(StrUtil::isNotBlank)
                    .distinct()
                    .collect(Collectors.joining("、"));
        }

        private static List<MesProProcessRespVO.BatchRecordFormLinkRespVO> toBatchRecordFormRespVOs(
                Collection<BatchRecordFormLink> links) {
            return links.stream()
                    .filter(link -> link != null && StrUtil.isNotBlank(link.reportId())
                            && StrUtil.isNotBlank(link.reportName()))
                    .map(link -> {
                        MesProProcessRespVO.BatchRecordFormLinkRespVO respVO =
                                new MesProProcessRespVO.BatchRecordFormLinkRespVO();
                        respVO.setReportId(link.reportId());
                        respVO.setReportName(link.reportName());
                        return respVO;
                    })
                    .toList();
        }

    }

    private static String formatWorkstationDisplayName(MesMdWorkstationDO workstation) {
        String code = StrUtil.trim(workstation.getCode());
        String name = StrUtil.trim(workstation.getName());
        if (StrUtil.isNotBlank(code) && StrUtil.isNotBlank(name)) {
            return code + " " + name;
        }
        if (StrUtil.isNotBlank(name)) {
            return name;
        }
        if (StrUtil.isNotBlank(code)) {
            return code;
        }
        return workstation.getId() == null ? "" : "工作站#" + workstation.getId();
    }

    private static String joinNames(Collection<String> names) {
        return names.stream()
                .filter(Objects::nonNull)
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.joining("、"));
    }

}
