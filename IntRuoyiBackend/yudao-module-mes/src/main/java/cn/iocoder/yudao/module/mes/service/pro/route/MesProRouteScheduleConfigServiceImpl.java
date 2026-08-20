package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.scheduleconfig.MesProRouteResourceCapacityPreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.scheduleconfig.MesProRouteScheduleConfigRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.scheduleconfig.MesProRouteScheduleConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.cal.plan.MesCalPlanDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.cal.plan.MesCalPlanShiftDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdProductionLineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationWorkerDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteScheduleConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProCapacityPlanDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProScheduleCalendarRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteScheduleConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProCapacityPlanMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProScheduleCalendarRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.enums.cal.MesCalPlanStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleCapacityModeEnum;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryProcessService;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryService;
import cn.iocoder.yudao.module.mes.service.cal.plan.MesCalPlanService;
import cn.iocoder.yudao.module.mes.service.cal.plan.MesCalPlanShiftService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdProductionLineService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationMachineService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationWorkerService;
import cn.iocoder.yudao.module.mes.service.pro.schedule.CapacityWindowAllocator;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception0;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_SCHEDULE_CAPACITY_MODE_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_SCHEDULE_CALENDAR_RULE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_SCHEDULE_CONFIG_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_SCHEDULE_INFINITE_DURATION_FORMULA_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_SCHEDULE_MANUAL_CAPACITY_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_SCHEDULE_NIGHT_SHIFT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_SCHEDULE_SHIFT_HOURS_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_PROCESS_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE;

@Service
@Validated
public class MesProRouteScheduleConfigServiceImpl implements MesProRouteScheduleConfigService {

    private static final String SNAPSHOT_CONFIGS_KEY = "configSnapshots";
    private static final String FLOW_GRAPH_KEY = "flowGraph";
    private static final String SCHEDULE_CONFIGS_KEY = "scheduleConfigs";
    private static final String NIGHT_SHIFT_MARK = "DAY_AND_NIGHT";
    private static final String CAPACITY_SOURCE_MACHINE = "MACHINE";
    private static final String CAPACITY_SOURCE_WORKER = "WORKER";
    private static final String CAPACITY_SOURCE_UNCONFIGURED = "UNCONFIGURED";
    private static final Set<String> READABLE_CANDIDATE_SNAPSHOT_STATUSES = Set.of(
            MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT,
            MesProRouteVersionLifecycleServiceImpl.STATUS_PENDING_APPROVAL,
            MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH,
            MesProRouteVersionLifecycleServiceImpl.STATUS_REJECTED,
            MesProRouteVersionLifecycleServiceImpl.STATUS_CANCELLED);

    @Value("${mes.schedule.allow-legacy-finite-hourly-write:false}")
    private boolean allowLegacyFiniteHourlyWrite = false;

    @Resource
    private MesProRouteVersionMapper routeVersionMapper;
    @Resource
    private MesProRouteProcessMapper routeProcessMapper;
    @Resource
    private MesProRouteProcessService routeProcessService;
    @Resource
    private MesProRouteScheduleConfigMapper routeScheduleConfigMapper;
    @Resource
    private MesProScheduleCalendarRuleMapper scheduleCalendarRuleMapper;
    @Resource
    private MesProCapacityPlanMapper capacityPlanMapper;
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
    private MesMdProductionLineService productionLineService;
    @Resource
    private MesProScheduleOrderMapper scheduleOrderMapper;
    @Resource
    private MesProScheduleOrderProcessMapper scheduleOrderProcessMapper;
    @Resource
    private MesProRouteCandidateConfigService routeCandidateConfigService;
    @Resource
    private MesCalPlanShiftService planShiftService;
    @Resource
    private MesCalPlanService planService;
    @Resource
    private CapacityWindowAllocator capacityWindowAllocator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveConfig(MesProRouteScheduleConfigSaveReqVO reqVO) {
        MesProRouteVersionDO routeVersion = requireDraftCandidateVersion(reqVO.getRouteVersionId());
        MesProRouteProcessDO routeProcess = routeProcessService.resolveCurrentRouteProcess(
                reqVO.getRouteProcessId(), routeVersion.getRouteId(), null);
        if (routeProcess == null || !routeVersion.getRouteId().equals(routeProcess.getRouteId())) {
            throw exception(PRO_ROUTE_FLOW_CONFIG_PROCESS_REQUIRED);
        }
        validateNightShiftExplicit(reqVO);
        validateCapacity(reqVO);
        if (Boolean.TRUE.equals(reqVO.getNightShiftEnabled())) {
            reqVO.setCalendarRuleId(resolveCalendarRuleId(reqVO.getCalendarRuleId()));
            validateNightShiftResources(routeProcess, reqVO.getCapacityMode());
        }
        rejectLegacyFiniteHourlyWriteAfterMigration(reqVO, null);
        routeCandidateConfigService.saveConfigSnapshot(routeVersion.getId(), SCHEDULE_CONFIGS_KEY,
                buildMergedScheduleConfigSnapshot(reqVO, routeVersion, routeProcess));
        return routeVersion.getId();
    }

    private MesProRouteVersionDO requireDraftCandidateVersion(Long routeVersionId) {
        if (routeVersionId == null) {
            throw exception(PRO_ROUTE_VERSION_NOT_EXISTS, routeVersionId);
        }
        MesProRouteVersionDO routeVersion = routeVersionMapper.selectById(routeVersionId);
        if (routeVersion == null) {
            throw exception(PRO_ROUTE_VERSION_NOT_EXISTS, routeVersionId);
        }
        if (isDraftCandidate(routeVersion)) {
            return routeVersion;
        }
        throw exception(PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE,
                routeVersion.getId(), routeVersion.getLifecycleStatus());
    }

    private boolean isDraftCandidate(MesProRouteVersionDO routeVersion) {
        return Boolean.FALSE.equals(routeVersion.getActive())
                && MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT.equals(routeVersion.getLifecycleStatus());
    }

    private boolean isReadableCandidateSnapshotVersion(MesProRouteVersionDO routeVersion) {
        return routeVersion != null
                && Boolean.FALSE.equals(routeVersion.getActive())
                && READABLE_CANDIDATE_SNAPSHOT_STATUSES.contains(routeVersion.getLifecycleStatus());
    }

    private JSONObject buildMergedScheduleConfigSnapshot(MesProRouteScheduleConfigSaveReqVO reqVO,
                                                         MesProRouteVersionDO routeVersion,
                                                         MesProRouteProcessDO routeProcess) {
        JSONObject configs = resolveCandidateScheduleConfigMap(routeVersion);
        String routeProcessKey = String.valueOf(routeProcess.getId());
        MesProRouteScheduleConfigDO existing = configs.containsKey(routeProcessKey)
                ? toScheduleConfigDO(routeVersion, configs.getJSONObject(routeProcessKey)) : null;
        MesProRouteScheduleConfigDO config = BeanUtils.toBean(reqVO, MesProRouteScheduleConfigDO.class);
        config.setItemId(null);
        config.setRouteVersionId(routeVersion.getId());
        config.setRouteProcessId(routeProcess.getId());
        config.setId(existing == null ? null : existing.getId());
        normalizeConfig(config, existing);
        configs.put(routeProcessKey, buildScheduleConfigSnapshot(config, routeVersion.getRouteId()));
        return configs;
    }

    private JSONObject buildScheduleConfigSnapshot(MesProRouteScheduleConfigDO config, Long routeId) {
        JSONObject snapshot = new JSONObject(true);
        snapshot.put("id", config.getId());
        snapshot.put("routeVersionId", config.getRouteVersionId());
        snapshot.put("routeId", routeId);
        snapshot.put("routeProcessId", config.getRouteProcessId());
        snapshot.put("capacityMode", config.getCapacityMode());
        snapshot.put("hourlyCapacity", config.getHourlyCapacity());
        snapshot.put("infiniteDurationQuantityFactor", config.getInfiniteDurationQuantityFactor());
        snapshot.put("infiniteDurationBaseMinutes", config.getInfiniteDurationBaseMinutes());
        snapshot.put("nightShiftEnabled", config.getNightShiftEnabled());
        snapshot.put("calendarRuleId", config.getCalendarRuleId());
        snapshot.put("configVersion", config.getConfigVersion());
        snapshot.put("remark", config.getRemark());
        return snapshot;
    }

    private MesProRouteScheduleConfigDO findCurrentConfig(MesProRouteVersionDO routeVersion,
                                                          Long routeProcessId) {
        MesProRouteScheduleConfigDO direct = routeScheduleConfigMapper
                .selectByRouteVersionIdAndRouteProcessId(routeVersion.getId(), routeProcessId);
        if (direct != null) {
            return direct;
        }
        return routeScheduleConfigMapper.selectListByRouteVersionId(routeVersion.getId()).stream()
                .filter(item -> item.getRouteProcessId() != null)
                .filter(item -> Objects.equals(routeProcessId,
                        routeProcessService.resolveCurrentRouteProcess(
                                item.getRouteProcessId(), routeVersion.getRouteId(), null).getId()))
                .findFirst()
                .orElse(null);
    }

    private void syncActiveWipSnapshots(MesProRouteScheduleConfigDO config, Long routeId) {
        List<MesProScheduleOrderDO> scheduleOrders = scheduleOrderMapper.selectListForProcessWip().stream()
                .filter(order -> !Boolean.TRUE.equals(order.getManualFinished()))
                .filter(order -> Objects.equals(order.getRouteVersionId(), config.getRouteVersionId()))
                .toList();
        if (scheduleOrders.isEmpty()) {
            return;
        }
        Map<Long, MesProScheduleOrderDO> scheduleOrderMap = scheduleOrders.stream()
                .collect(Collectors.toMap(MesProScheduleOrderDO::getId, item -> item, (left, right) -> left,
                        LinkedHashMap::new));
        List<MesProScheduleOrderProcessDO> processes = scheduleOrderProcessMapper
                .selectListByScheduleOrderIds(scheduleOrderMap.keySet());
        for (MesProScheduleOrderProcessDO process : processes) {
            MesProScheduleOrderDO scheduleOrder = scheduleOrderMap.get(process.getScheduleOrderId());
            Long routeVersionId = process.getRouteVersionId() == null
                    ? scheduleOrder.getRouteVersionId() : process.getRouteVersionId();
            MesProRouteProcessDO currentRouteProcess = process.getRouteProcessId() == null
                    ? null : routeProcessService.resolveCurrentRouteProcess(
                            process.getRouteProcessId(), routeId, process.getProcessId());
            if (!Boolean.TRUE.equals(process.getEnabled())
                    || (process.getProgressPercent() != null
                    && process.getProgressPercent().compareTo(new BigDecimal("100")) >= 0)
                    || !Objects.equals(routeVersionId, config.getRouteVersionId())
                    || currentRouteProcess == null
                    || !Objects.equals(currentRouteProcess.getId(), config.getRouteProcessId())) {
                continue;
            }
            MesProScheduleOrderProcessDO updateObj = new MesProScheduleOrderProcessDO();
            updateObj.setId(process.getId());
            updateObj.setRouteProcessId(currentRouteProcess.getId());
            updateObj.setProcessId(currentRouteProcess.getProcessId());
            updateObj.setRouteScheduleConfigId(config.getId());
            applyActiveWipCapacitySnapshot(updateObj, config, currentRouteProcess, process);
            updateObj.setNightShiftEnabled(Boolean.TRUE.equals(config.getNightShiftEnabled()));
            updateObj.setCalendarRuleId(config.getCalendarRuleId());
            scheduleOrderProcessMapper.updateById(updateObj);
        }
    }

    private void applyActiveWipCapacitySnapshot(MesProScheduleOrderProcessDO updateObj,
                                                MesProRouteScheduleConfigDO config,
                                                MesProRouteProcessDO routeProcess,
                                                MesProScheduleOrderProcessDO process) {
        BigDecimal shiftHours = requireShiftHours(process.getShiftHours(), routeProcess.getProcessId());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("routeScheduleConfigId", config.getId());
        payload.put("routeProcessId", routeProcess.getId());
        payload.put("processId", routeProcess.getProcessId());
        payload.put("capacityMode", config.getCapacityMode());
        payload.put("configVersion", config.getConfigVersion());
        payload.put("shiftHours", shiftHours);
        payload.put("nightShiftEnabled", Boolean.TRUE.equals(config.getNightShiftEnabled()));
        payload.put("calendarRuleId", config.getCalendarRuleId());

        updateObj.setCapacityMode(config.getCapacityMode());
        updateObj.setInfiniteDurationQuantityFactor(config.getInfiniteDurationQuantityFactor());
        updateObj.setInfiniteDurationBaseMinutes(config.getInfiniteDurationBaseMinutes());
        if (MesProScheduleCapacityModeEnum.isManualOverrideLike(config.getCapacityMode())) {
            BigDecimal hourlyCapacity = config.getHourlyCapacity();
            BigDecimal shiftCapacity = hourlyCapacity == null ? null : hourlyCapacity.multiply(shiftHours);
            updateObj.setCapacitySource(process.getCapacitySource());
            updateObj.setHourlyCapacityTotal(hourlyCapacity);
            updateObj.setShiftCapacityTotal(shiftCapacity);
            payload.put("capacitySource", process.getCapacitySource());
            payload.put("hourlyCapacityTotal", hourlyCapacity);
            payload.put("shiftCapacityTotal", shiftCapacity);
            updateObj.setResourceSnapshotJson(JsonUtils.toJsonString(payload));
            return;
        }
        if (MesProScheduleCapacityModeEnum.RESOURCE_CALCULATED.getMode().equals(config.getCapacityMode())) {
            MesProRouteResourceCapacityPreviewRespVO preview = getResourcePreview(routeProcess.getId());
            BigDecimal hourlyCapacity = nullToZero(preview.getResourceCapacityHourly());
            BigDecimal shiftCapacity = hourlyCapacity.multiply(shiftHours);
            updateObj.setCapacitySource(preview.getCapacitySource());
            updateObj.setHourlyCapacityTotal(hourlyCapacity);
            updateObj.setShiftCapacityTotal(shiftCapacity);
            payload.put("capacitySource", preview.getCapacitySource());
            payload.put("hourlyCapacityTotal", hourlyCapacity);
            payload.put("shiftCapacityTotal", shiftCapacity);
            payload.put("workstationRows", preview.getWorkstationRows());
            payload.put("blockingIssues", preview.getBlockingIssues());
            updateObj.setResourceSnapshotJson(JsonUtils.toJsonString(payload));
            return;
        }
        if (MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode().equals(config.getCapacityMode())) {
            updateObj.setCapacitySource(process.getCapacitySource());
            updateObj.setHourlyCapacityTotal(process.getHourlyCapacityTotal());
            updateObj.setShiftCapacityTotal(process.getShiftCapacityTotal());
            payload.put("capacitySource", process.getCapacitySource());
            payload.put("hourlyCapacityTotal", process.getHourlyCapacityTotal());
            payload.put("shiftCapacityTotal", process.getShiftCapacityTotal());
            payload.put("infiniteDurationQuantityFactor", config.getInfiniteDurationQuantityFactor());
            payload.put("infiniteDurationBaseMinutes", config.getInfiniteDurationBaseMinutes());
            updateObj.setResourceSnapshotJson(JsonUtils.toJsonString(payload));
            return;
        }
        throw new IllegalStateException("未知排产产能模式，routeScheduleConfigId=" + config.getId());
    }

    @Override
    public List<MesProRouteScheduleConfigDO> getConfigListByRouteVersionId(Long routeVersionId) {
        MesProRouteVersionDO routeVersion = routeVersionMapper.selectById(routeVersionId);
        if (routeVersion == null) {
            throw exception(PRO_ROUTE_FLOW_CONFIG_PROCESS_REQUIRED);
        }
        if (isReadableCandidateSnapshotVersion(routeVersion)) {
            return getCandidateScheduleConfigList(routeVersion);
        }
        return normalizeConfigRouteProcessIds(routeVersion,
                routeScheduleConfigMapper.selectListByRouteVersionId(routeVersionId));
    }

    @Override
    public List<MesProRouteScheduleConfigRespVO> getConfigRespListByRouteVersionId(Long routeVersionId) {
        MesProRouteVersionDO routeVersion = routeVersionMapper.selectById(routeVersionId);
        if (routeVersion == null) {
            throw exception(PRO_ROUTE_FLOW_CONFIG_PROCESS_REQUIRED);
        }
        List<MesProRouteScheduleConfigDO> configs = isReadableCandidateSnapshotVersion(routeVersion)
                ? getCandidateScheduleConfigList(routeVersion)
                : normalizeConfigRouteProcessIds(routeVersion,
                        routeScheduleConfigMapper.selectListByRouteVersionId(routeVersionId));
        Map<Long, BigDecimal> shiftHoursMap = loadShiftHoursMap(routeVersion.getRouteId());
        return BeanUtils.toBean(configs, MesProRouteScheduleConfigRespVO.class, vo -> {
            BigDecimal shiftHours = normalizeShiftHours(shiftHoursMap.get(vo.getRouteProcessId()));
            vo.setShiftHours(shiftHours);
            if (MesProScheduleCapacityModeEnum.isManualOverrideLike(vo.getCapacityMode())
                    && vo.getHourlyCapacity() != null && shiftHours != null) {
                vo.setStandardShiftCapacity(vo.getHourlyCapacity().multiply(shiftHours));
            }
        });
    }

    @Override
    public MesProRouteResourceCapacityPreviewRespVO getResourcePreview(Long routeProcessId) {
        MesProRouteProcessDO routeProcess = routeProcessService.getRouteProcess(routeProcessId);
        if (routeProcess == null || routeProcess.getProcessId() == null) {
            throw exception(PRO_ROUTE_FLOW_CONFIG_PROCESS_REQUIRED);
        }
        MesProRouteResourceCapacityPreviewRespVO preview = new MesProRouteResourceCapacityPreviewRespVO();
        preview.setRouteProcessId(routeProcess.getId());
        preview.setProcessId(routeProcess.getProcessId());

        if (routeProcess.getWorkstationId() == null) {
            preview.setCapacitySource(CAPACITY_SOURCE_UNCONFIGURED);
            addIssue(preview, null, "BLOCKED_NO_WORKSTATION", "Route process has no bound workstation", null, null);
            return preview;
        }
        MesMdWorkstationDO workstation = workstationService.getWorkstation(routeProcess.getWorkstationId());
        if (workstation == null) {
            preview.setCapacitySource(CAPACITY_SOURCE_UNCONFIGURED);
            addIssue(preview, null, "BLOCKED_NO_WORKSTATION", "Bound workstation missing", null, null);
            return preview;
        }
        List<MesMdWorkstationDO> workstations = List.of(
                copyWorkstationForRouteProcess(workstation, routeProcess.getProcessId()));

        List<Long> workstationIds = workstations.stream()
                .map(MesMdWorkstationDO::getId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, List<MesMdWorkstationMachineDO>> machineMap = convertMultiMap(
                workstationMachineService.getWorkstationMachineListByWorkstationIds(workstationIds),
                MesMdWorkstationMachineDO::getWorkstationId);
        Map<Long, List<MesMdWorkstationWorkerDO>> workerMap = convertMultiMap(
                workstationWorkerService.getWorkstationWorkerListByWorkstationIds(workstationIds),
                MesMdWorkstationWorkerDO::getWorkstationId);
        Set<Long> productionLineIds = workstations.stream()
                .map(MesMdWorkstationDO::getProductionLineId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        Map<Long, MesMdProductionLineDO> productionLineMap = productionLineService.getProductionLineMap(productionLineIds);
        Set<Long> machineryIds = machineMap.values().stream()
                .flatMap(List::stream)
                .map(MesMdWorkstationMachineDO::getMachineryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        Map<Long, MesDvMachineryDO> machineryMap = machineryService.getMachineryMap(machineryIds);
        Map<String, MesDvMachineryProcessDO> machineryProcessMap = buildMachineryProcessMap(
                machineryProcessService.getMachineryProcessListByMachineryIdsAndProcessIds(
                        machineryIds, Set.of(routeProcess.getProcessId())));

        BigDecimal resourceCapacityHourly = BigDecimal.ZERO;
        boolean hasMachine = false;
        boolean hasWorker = false;
        for (MesMdWorkstationDO boundWorkstation : workstations.stream()
                .sorted(Comparator.comparing(MesMdWorkstationDO::getId, Comparator.nullsLast(Long::compareTo)))
                .toList()) {
            MesProRouteResourceCapacityPreviewRespVO.WorkstationRow row = new MesProRouteResourceCapacityPreviewRespVO.WorkstationRow();
            row.setWorkstationId(boundWorkstation.getId());
            row.setWorkstationCode(boundWorkstation.getCode());
            row.setWorkstationName(boundWorkstation.getName());
            row.setProductionLineId(boundWorkstation.getProductionLineId());
            MesMdProductionLineDO productionLine = boundWorkstation.getProductionLineId() == null
                    ? null : productionLineMap.get(boundWorkstation.getProductionLineId());
            row.setProductionLineName(productionLine == null ? null : productionLine.getName());
            row.setShiftHours(requireShiftHours(boundWorkstation.getShiftHours(), routeProcess.getProcessId()));
            if (boundWorkstation.getProductionLineId() == null || productionLine == null) {
                addIssue(preview, row, "BLOCKED_NO_LINE", "Production line missing", null, null);
            }

            List<MesMdWorkstationMachineDO> machines = machineMap.getOrDefault(boundWorkstation.getId(), Collections.emptyList());
            if (!machines.isEmpty()) {
                hasMachine = true;
                row.setResourceType(CAPACITY_SOURCE_MACHINE);
                BigDecimal rowCapacity = BigDecimal.ZERO;
                for (MesMdWorkstationMachineDO machineBinding : machines.stream()
                        .sorted(Comparator.comparing(MesMdWorkstationMachineDO::getId, Comparator.nullsLast(Long::compareTo)))
                        .toList()) {
                    MesProRouteResourceCapacityPreviewRespVO.MachineRow machineRow = new MesProRouteResourceCapacityPreviewRespVO.MachineRow();
                    machineRow.setWorkstationMachineId(machineBinding.getId());
                    machineRow.setMachineryId(machineBinding.getMachineryId());
                    machineRow.setQuantity(machineBinding.getQuantity());
                    MesDvMachineryDO machinery = machineryMap.get(machineBinding.getMachineryId());
                    if (machinery != null) {
                        machineRow.setMachineryCode(machinery.getCode());
                        machineRow.setMachineryName(machinery.getName());
                    }
                    MesDvMachineryProcessDO machineryProcess = machineryProcessMap.get(
                            buildMachineryProcessKey(machineBinding.getMachineryId(), routeProcess.getProcessId()));
                    BigDecimal standardHourlyCapacity = machineryProcess == null
                            ? null : machineryProcess.getStandardHourlyCapacity();
                    machineRow.setStandardHourlyCapacity(standardHourlyCapacity);
                    if (standardHourlyCapacity == null || standardHourlyCapacity.compareTo(BigDecimal.ZERO) <= 0) {
                        addIssue(preview, row, "BLOCKED_NO_MACHINERY_PROCESS_CAPACITY",
                                "Machinery process capacity missing", machineBinding, machinery);
                    } else if (machineBinding.getQuantity() == null || machineBinding.getQuantity() <= 0) {
                        addIssue(preview, row, "BLOCKED_NO_RESOURCE_QUANTITY",
                                "Machinery quantity missing", machineBinding, machinery);
                    } else {
                        BigDecimal machineCapacity = standardHourlyCapacity.multiply(BigDecimal.valueOf(machineBinding.getQuantity()));
                        machineRow.setHourlyCapacity(machineCapacity);
                        rowCapacity = rowCapacity.add(machineCapacity);
                    }
                    row.getMachineRows().add(machineRow);
                }
                row.setHourlyCapacity(rowCapacity);
                resourceCapacityHourly = resourceCapacityHourly.add(rowCapacity);
            } else {
                hasWorker = true;
                row.setResourceType(CAPACITY_SOURCE_WORKER);
                int workerQuantity = workerMap.getOrDefault(boundWorkstation.getId(), Collections.emptyList()).stream()
                        .map(MesMdWorkstationWorkerDO::getQuantity)
                        .filter(Objects::nonNull)
                        .reduce(0, Integer::sum);
                row.setWorkerQuantity(workerQuantity);
                row.setSingleStandardHourlyCapacity(boundWorkstation.getSingleStandardHourlyCapacity());
                if (!positive(boundWorkstation.getSingleStandardHourlyCapacity())) {
                    addIssue(preview, row, "BLOCKED_NO_WORKER_CAPACITY", "Worker capacity missing", null, null);
                } else {
                    BigDecimal workerCapacity = boundWorkstation.getSingleStandardHourlyCapacity();
                    row.setHourlyCapacity(workerCapacity);
                    resourceCapacityHourly = resourceCapacityHourly.add(workerCapacity);
                }
            }
            preview.getWorkstationRows().add(row);
        }
        preview.setResourceCapacityHourly(resourceCapacityHourly);
        preview.setCapacitySource(hasMachine ? CAPACITY_SOURCE_MACHINE : hasWorker ? CAPACITY_SOURCE_WORKER : CAPACITY_SOURCE_UNCONFIGURED);
        return preview;
    }

    @Override
    public void validateNightShiftResources(Long routeProcessId, String capacityMode) {
        MesProRouteProcessDO routeProcess = routeProcessService.resolveCurrentRouteProcess(routeProcessId, null, null);
        if (routeProcess == null || routeProcess.getProcessId() == null) {
            throw exception(PRO_ROUTE_FLOW_CONFIG_PROCESS_REQUIRED);
        }
        validateNightShiftResources(routeProcess, capacityMode);
    }

    private void validateNightShiftResources(MesProRouteProcessDO routeProcess, String capacityMode) {
        if (routeProcess.getWorkstationId() == null) {
            throw nightShiftResourceException(routeProcess, null, null, null,
                    "未绑定工作站，无法确定夜班班次、设备和产能");
        }
        MesMdWorkstationDO workstation = workstationService.getWorkstation(routeProcess.getWorkstationId());
        if (workstation == null) {
            throw nightShiftResourceException(routeProcess, null, null, null,
                    "绑定的工作站不存在");
        }
        if (!Objects.equals(CommonStatusEnum.ENABLE.getStatus(), workstation.getStatus())) {
            throw nightShiftResourceException(routeProcess, workstation, null, null,
                    "绑定的工作站未启用");
        }
        MesMdProductionLineDO productionLine = workstation.getProductionLineId() == null
                ? null : productionLineService.getProductionLine(workstation.getProductionLineId());
        if (productionLine == null) {
            throw nightShiftResourceException(routeProcess, workstation, null, null,
                    "工作站未绑定有效产线");
        }
        if (!Objects.equals(CommonStatusEnum.ENABLE.getStatus(), productionLine.getStatus())) {
            throw nightShiftResourceException(routeProcess, workstation, productionLine, null,
                    "绑定的产线未启用");
        }
        if (productionLine.getCalendarPlanId() == null) {
            throw nightShiftResourceException(routeProcess, workstation, productionLine, null,
                    "产线未配置排班计划，缺少夜班班次");
        }
        MesCalPlanDO plan = planService.getPlan(productionLine.getCalendarPlanId());
        if (plan == null) {
            throw nightShiftResourceException(routeProcess, workstation, productionLine, null,
                    "产线绑定的排班计划不存在，缺少夜班班次");
        }
        if (!Objects.equals(MesCalPlanStatusEnum.CONFIRMED.getStatus(), plan.getStatus())) {
            throw nightShiftResourceException(routeProcess, workstation, productionLine, null,
                    "排班计划未确认，缺少正式夜班班次");
        }
        List<MesCalPlanShiftDO> nightShifts = safeList(
                planShiftService.getPlanShiftListByPlanId(productionLine.getCalendarPlanId())).stream()
                .filter(capacityWindowAllocator::isNightShift)
                .toList();
        if (nightShifts.isEmpty()) {
            throw nightShiftResourceException(routeProcess, workstation, productionLine, null,
                    "排班计划缺少夜班班次");
        }
        for (MesCalPlanShiftDO shift : nightShifts) {
            int shiftMinutes;
            try {
                shiftMinutes = capacityWindowAllocator.calculateShiftCapacityMinutes(shift);
            } catch (RuntimeException invalidShiftTime) {
                throw nightShiftResourceException(routeProcess, workstation, productionLine, shift,
                        "夜班班次时间无效");
            }
            if (shiftMinutes <= 0) {
                throw nightShiftResourceException(routeProcess, workstation, productionLine, shift,
                        "夜班班次没有可用时长产能");
            }
        }
        assertNightShiftCapacityPlan(routeProcess, workstation, productionLine, nightShifts);
        if (MesProScheduleCapacityModeEnum.RESOURCE_CALCULATED.getMode().equals(capacityMode)) {
            assertResourceCalculatedCapacity(routeProcess, workstation, productionLine, nightShifts.get(0));
        }
    }

    private void assertNightShiftCapacityPlan(MesProRouteProcessDO routeProcess,
                                              MesMdWorkstationDO workstation,
                                              MesMdProductionLineDO productionLine,
                                              List<MesCalPlanShiftDO> nightShifts) {
        Set<Long> nightShiftIds = nightShifts.stream()
                .map(MesCalPlanShiftDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<MesProCapacityPlanDO> capacityPlans = safeList(capacityPlanMapper.selectListByLineIdsAndDate(
                List.of(productionLine.getId()), LocalDate.now().atStartOfDay()));
        boolean hasNightCapacity = capacityPlans.stream()
                .filter(plan -> Boolean.TRUE.equals(plan.getEnabled()))
                .filter(plan -> plan.getCapacityMinutes() != null && plan.getCapacityMinutes() > 0)
                .map(MesProCapacityPlanDO::getShiftId)
                .anyMatch(nightShiftIds::contains);
        if (!hasNightCapacity) {
            throw nightShiftResourceException(routeProcess, workstation, productionLine, nightShifts.get(0),
                    "夜班班次缺少未来可用产能计划");
        }
    }

    private void assertResourceCalculatedCapacity(MesProRouteProcessDO routeProcess,
                                                  MesMdWorkstationDO workstation,
                                                  MesMdProductionLineDO productionLine,
                                                  MesCalPlanShiftDO shift) {
        List<MesMdWorkstationMachineDO> machineBindings = safeList(
                workstationMachineService.getWorkstationMachineListByWorkstationIds(Set.of(workstation.getId())));
        if (machineBindings.isEmpty()) {
            if (!positive(workstation.getSingleStandardHourlyCapacity())) {
                throw nightShiftResourceException(routeProcess, workstation, productionLine, shift,
                        "人工小时产能未配置");
            }
            return;
        }
        Set<Long> machineryIds = machineBindings.stream()
                .map(MesMdWorkstationMachineDO::getMachineryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        Map<Long, MesDvMachineryDO> machineryMap = machineryService.getMachineryMap(machineryIds);
        Map<String, MesDvMachineryProcessDO> machineryProcessMap = buildMachineryProcessMap(
                machineryProcessService.getMachineryProcessListByMachineryIdsAndProcessIds(
                        machineryIds, Set.of(routeProcess.getProcessId())));
        for (MesMdWorkstationMachineDO binding : machineBindings) {
            MesDvMachineryDO machinery = machineryMap.get(binding.getMachineryId());
            String machineryLabel = displayLabel(machinery == null ? null : machinery.getCode(),
                    machinery == null ? null : machinery.getName(), binding.getMachineryId());
            if (machinery == null) {
                throw nightShiftResourceException(routeProcess, workstation, productionLine, shift,
                        "设备[" + machineryLabel + "]不存在");
            }
            if (binding.getQuantity() == null || binding.getQuantity() <= 0) {
                throw nightShiftResourceException(routeProcess, workstation, productionLine, shift,
                        "设备[" + machineryLabel + "]数量未配置");
            }
            MesDvMachineryProcessDO machineryProcess = machineryProcessMap.get(
                    buildMachineryProcessKey(binding.getMachineryId(), routeProcess.getProcessId()));
            if (machineryProcess == null || !positive(machineryProcess.getStandardHourlyCapacity())) {
                throw nightShiftResourceException(routeProcess, workstation, productionLine, shift,
                        "设备[" + machineryLabel + "]缺少当前工序的设备工序小时产能");
            }
        }
    }

    private RuntimeException nightShiftResourceException(MesProRouteProcessDO routeProcess,
                                                         MesMdWorkstationDO workstation,
                                                         MesMdProductionLineDO productionLine,
                                                         MesCalPlanShiftDO shift,
                                                         String reason) {
        String workstationLabel = displayLabel(workstation == null ? null : workstation.getCode(),
                workstation == null ? null : workstation.getName(),
                workstation == null ? routeProcess.getWorkstationId() : workstation.getId());
        String lineLabel = displayLabel(productionLine == null ? null : productionLine.getCode(),
                productionLine == null ? null : productionLine.getName(),
                productionLine == null ? null : productionLine.getId());
        String shiftLabel = displayLabel(null, shift == null ? null : shift.getName(), shift == null ? null : shift.getId());
        return exception0(400,
                "工序启用夜班失败：routeProcessId={}，工作站[{}]，产线[{}]，夜班[{}]，{}",
                routeProcess.getId(), workstationLabel, lineLabel, shiftLabel, reason);
    }

    private String displayLabel(String code, String name, Long id) {
        if (StrUtil.isNotBlank(code) && StrUtil.isNotBlank(name)) {
            return code + "/" + name;
        }
        if (StrUtil.isNotBlank(code)) {
            return code;
        }
        if (StrUtil.isNotBlank(name)) {
            return name;
        }
        return id == null ? "未配置" : String.valueOf(id);
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private MesMdWorkstationDO copyWorkstationForRouteProcess(MesMdWorkstationDO workstation, Long processId) {
        MesMdWorkstationDO copy = BeanUtils.toBean(workstation, MesMdWorkstationDO.class);
        copy.setProcessId(processId);
        return copy;
    }

    private Map<String, MesDvMachineryProcessDO> buildMachineryProcessMap(List<MesDvMachineryProcessDO> rows) {
        Map<String, MesDvMachineryProcessDO> result = new LinkedHashMap<>();
        if (rows == null) {
            return result;
        }
        for (MesDvMachineryProcessDO row : rows) {
            result.merge(buildMachineryProcessKey(row.getMachineryId(), row.getProcessId()), row,
                    this::pickProcessCapacityRow);
        }
        return result;
    }

    private MesDvMachineryProcessDO pickProcessCapacityRow(MesDvMachineryProcessDO existing,
                                                           MesDvMachineryProcessDO current) {
        if (existing == null || existing.getStandardHourlyCapacity() == null) {
            return current;
        }
        if (current.getStandardHourlyCapacity() == null
                || existing.getStandardHourlyCapacity().compareTo(current.getStandardHourlyCapacity()) == 0) {
            return existing;
        }
        throw new IllegalStateException(String.format("Machinery process capacity conflict: machineryId=%s, processId=%s",
                current.getMachineryId(), current.getProcessId()));
    }

    private void addIssue(MesProRouteResourceCapacityPreviewRespVO preview,
                          MesProRouteResourceCapacityPreviewRespVO.WorkstationRow row,
                          String code,
                          String message,
                          MesMdWorkstationMachineDO machineBinding,
                          MesDvMachineryDO machinery) {
        MesProRouteResourceCapacityPreviewRespVO.BlockingIssue issue = new MesProRouteResourceCapacityPreviewRespVO.BlockingIssue();
        issue.setCode(code);
        issue.setMessage(message);
        issue.setRouteProcessId(preview.getRouteProcessId());
        if (row != null) {
            issue.setWorkstationId(row.getWorkstationId());
            issue.setWorkstationCode(row.getWorkstationCode());
        }
        if (machineBinding != null) {
            issue.setMachineryId(machineBinding.getMachineryId());
        }
        if (machinery != null) {
            issue.setMachineryCode(machinery.getCode());
        }
        preview.getBlockingIssues().add(issue);
        if (row != null) {
            row.getBlockingIssues().add(issue);
        }
    }

    private String buildMachineryProcessKey(Long machineryId, Long processId) {
        return machineryId + ":" + processId;
    }

    private List<MesProRouteScheduleConfigDO> getCandidateScheduleConfigList(MesProRouteVersionDO routeVersion) {
        List<MesProRouteScheduleConfigDO> configs = new ArrayList<>();
        for (Object value : resolveCandidateScheduleConfigMap(routeVersion).values()) {
            configs.add(toScheduleConfigDO(routeVersion, toScheduleConfigJson(value, routeVersion)));
        }
        return configs;
    }

    private JSONObject resolveCandidateScheduleConfigMap(MesProRouteVersionDO routeVersion) {
        Object snapshot = resolveCandidateConfigSnapshot(routeVersion, SCHEDULE_CONFIGS_KEY);
        Set<Long> candidateRouteProcessIds = resolveCandidateRouteProcessIds(routeVersion);
        JSONObject result = new JSONObject(true);
        if (snapshot instanceof JSONObject configsByRouteProcessId) {
            for (Map.Entry<String, Object> entry : configsByRouteProcessId.entrySet()) {
                JSONObject config = toScheduleConfigJson(entry.getValue(), routeVersion);
                if (config.getLong("routeProcessId") == null) {
                    config.put("routeProcessId", parseRouteProcessIdKey(entry.getKey(), routeVersion));
                }
                Long candidateRouteProcessId = requireCandidateRouteProcessId(
                        routeVersion, candidateRouteProcessIds, config.getLong("routeProcessId"));
                config.put("routeProcessId", candidateRouteProcessId);
                result.put(String.valueOf(candidateRouteProcessId), config);
            }
            return result;
        }
        if (snapshot instanceof JSONArray configs) {
            for (Object value : configs) {
                JSONObject config = toScheduleConfigJson(value, routeVersion);
                Long candidateRouteProcessId = requireCandidateRouteProcessId(
                        routeVersion, candidateRouteProcessIds, config.getLong("routeProcessId"));
                config.put("routeProcessId", candidateRouteProcessId);
                result.put(String.valueOf(candidateRouteProcessId), config);
            }
            return result;
        }
        throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersion.getId());
    }

    private Set<Long> resolveCandidateRouteProcessIds(MesProRouteVersionDO routeVersion) {
        Object flowGraphSnapshot = resolveCandidateConfigSnapshot(routeVersion, FLOW_GRAPH_KEY);
        JSONObject flowGraph = toScheduleConfigJson(flowGraphSnapshot, routeVersion);
        JSONArray nodes = flowGraph.getJSONArray("nodes");
        if (nodes == null) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersion.getId());
        }
        Set<Long> routeProcessIds = new java.util.LinkedHashSet<>();
        for (Object value : nodes) {
            JSONObject node = toScheduleConfigJson(value, routeVersion);
            Long routeProcessId = node.getLong("routeProcessId");
            if (routeProcessId == null) {
                routeProcessId = node.getLong("clientRouteProcessId");
            }
            Long processId = node.getLong("processId");
            Integer sort = node.getInteger("sort");
            if (routeProcessId == null || processId == null || processId <= 0 || sort == null
                    || !routeProcessIds.add(routeProcessId)) {
                throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersion.getId());
            }
        }
        return routeProcessIds;
    }

    private Long requireCandidateRouteProcessId(MesProRouteVersionDO routeVersion,
                                                Set<Long> candidateRouteProcessIds,
                                                Long routeProcessId) {
        if (routeProcessId == null || !candidateRouteProcessIds.contains(routeProcessId)) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersion.getId());
        }
        return routeProcessId;
    }

    private Long resolveCurrentRouteProcessId(MesProRouteVersionDO routeVersion, Long routeProcessId) {
        if (routeProcessId == null) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersion.getId());
        }
        MesProRouteProcessDO currentRouteProcess = routeProcessService.resolveCurrentRouteProcess(
                routeProcessId, routeVersion.getRouteId(), null);
        if (currentRouteProcess == null || !Objects.equals(routeVersion.getRouteId(), currentRouteProcess.getRouteId())) {
            throw exception(PRO_ROUTE_FLOW_CONFIG_PROCESS_REQUIRED);
        }
        return currentRouteProcess.getId();
    }

    private Object resolveCandidateConfigSnapshot(MesProRouteVersionDO routeVersion, String configKey) {
        if (routeVersion == null || StrUtil.isBlank(routeVersion.getRouteSnapshotJson())) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE,
                    routeVersion == null ? null : routeVersion.getId());
        }
        JSONObject snapshot = JSON.parseObject(routeVersion.getRouteSnapshotJson());
        if (snapshot == null || snapshot.isEmpty()) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersion.getId());
        }
        JSONObject configSnapshots = snapshot.getJSONObject(SNAPSHOT_CONFIGS_KEY);
        if (configSnapshots == null || !configSnapshots.containsKey(configKey)) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersion.getId());
        }
        return configSnapshots.get(configKey);
    }

    private JSONObject toScheduleConfigJson(Object value, MesProRouteVersionDO routeVersion) {
        if (value instanceof JSONObject jsonObject) {
            return jsonObject;
        }
        JSONObject jsonObject = JSON.parseObject(JSON.toJSONString(value));
        if (jsonObject == null) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersion.getId());
        }
        return jsonObject;
    }

    private Long parseRouteProcessIdKey(String key, MesProRouteVersionDO routeVersion) {
        try {
            return Long.valueOf(key);
        } catch (RuntimeException ex) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersion.getId());
        }
    }

    private MesProRouteScheduleConfigDO toScheduleConfigDO(MesProRouteVersionDO routeVersion,
                                                           JSONObject config) {
        Long routeProcessId = config.getLong("routeProcessId");
        if (routeProcessId == null) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersion.getId());
        }
        return MesProRouteScheduleConfigDO.builder()
                .id(config.getLong("id"))
                .routeVersionId(routeVersion.getId())
                .itemId(config.getLong("itemId"))
                .routeProcessId(routeProcessId)
                .capacityMode(config.getString("capacityMode"))
                .hourlyCapacity(config.getBigDecimal("hourlyCapacity"))
                .infiniteDurationQuantityFactor(config.getBigDecimal("infiniteDurationQuantityFactor"))
                .infiniteDurationBaseMinutes(config.getBigDecimal("infiniteDurationBaseMinutes"))
                .nightShiftEnabled(config.getBoolean("nightShiftEnabled"))
                .calendarRuleId(config.getLong("calendarRuleId"))
                .configVersion(config.getString("configVersion"))
                .copiedFromConfigId(config.getLong("copiedFromConfigId"))
                .remark(config.getString("remark"))
                .build();
    }

    private List<MesProRouteScheduleConfigDO> normalizeConfigRouteProcessIds(
            MesProRouteVersionDO routeVersion, List<MesProRouteScheduleConfigDO> configs) {
        for (MesProRouteScheduleConfigDO config : configs) {
            if (config.getRouteProcessId() == null) {
                continue;
            }
            config.setRouteProcessId(routeProcessService.resolveCurrentRouteProcess(
                    config.getRouteProcessId(), routeVersion.getRouteId(), null).getId());
        }
        return configs;
    }

    private void validateNightShiftExplicit(MesProRouteScheduleConfigSaveReqVO reqVO) {
        if (reqVO.getNightShiftEnabled() == null) {
            throw exception(PRO_ROUTE_SCHEDULE_NIGHT_SHIFT_REQUIRED, reqVO.getRouteProcessId());
        }
    }

    private void validateCapacity(MesProRouteScheduleConfigSaveReqVO reqVO) {
        if (MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode().equals(reqVO.getCapacityMode())) {
            if (!allowLegacyFiniteHourlyWrite) {
                throw exception(PRO_ROUTE_SCHEDULE_CAPACITY_MODE_INVALID, reqVO.getRouteProcessId());
            }
            if (!positive(reqVO.getHourlyCapacity())) {
                throw exception(PRO_ROUTE_SCHEDULE_MANUAL_CAPACITY_REQUIRED, reqVO.getRouteProcessId());
            }
            return;
        }
        if (MesProScheduleCapacityModeEnum.MANUAL_OVERRIDE.getMode().equals(reqVO.getCapacityMode())) {
            if (!positive(reqVO.getHourlyCapacity())) {
                throw exception(PRO_ROUTE_SCHEDULE_MANUAL_CAPACITY_REQUIRED, reqVO.getRouteProcessId());
            }
            return;
        }
        if (MesProScheduleCapacityModeEnum.RESOURCE_CALCULATED.getMode().equals(reqVO.getCapacityMode())) {
            return;
        }
        if (MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode().equals(reqVO.getCapacityMode())) {
            if (!positive(reqVO.getInfiniteDurationQuantityFactor())
                    || reqVO.getInfiniteDurationBaseMinutes() == null
                    || reqVO.getInfiniteDurationBaseMinutes().compareTo(BigDecimal.ZERO) < 0) {
                throw exception(PRO_ROUTE_SCHEDULE_INFINITE_DURATION_FORMULA_REQUIRED, reqVO.getRouteProcessId());
            }
            return;
        }
        throw exception(PRO_ROUTE_SCHEDULE_CAPACITY_MODE_INVALID, reqVO.getRouteProcessId());
    }

    private void rejectLegacyFiniteHourlyWriteAfterMigration(MesProRouteScheduleConfigSaveReqVO reqVO,
                                                            MesProRouteScheduleConfigDO existing) {
        if (!MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode().equals(reqVO.getCapacityMode())) {
            return;
        }
        if (allowLegacyFiniteHourlyWrite) {
            return;
        }
        throw exception(PRO_ROUTE_SCHEDULE_CAPACITY_MODE_INVALID, reqVO.getRouteProcessId());
    }
    private void normalizeConfig(MesProRouteScheduleConfigDO config, MesProRouteScheduleConfigDO existing) {
        if (MesProScheduleCapacityModeEnum.isManualOverrideLike(config.getCapacityMode())) {
            config.setInfiniteDurationQuantityFactor(null);
            config.setInfiniteDurationBaseMinutes(null);
        } else if (MesProScheduleCapacityModeEnum.RESOURCE_CALCULATED.getMode().equals(config.getCapacityMode())) {
            config.setHourlyCapacity(null);
            config.setInfiniteDurationQuantityFactor(null);
            config.setInfiniteDurationBaseMinutes(null);
        } else {
            config.setHourlyCapacity(null);
        }
        if (Boolean.TRUE.equals(config.getNightShiftEnabled())) {
            config.setCalendarRuleId(resolveCalendarRuleId(config.getCalendarRuleId()));
            config.setRemark(appendNightShiftMark(config.getRemark()));
        } else {
            config.setCalendarRuleId(null);
            config.setRemark(removeNightShiftMark(config.getRemark()));
        }
        if (StrUtil.isBlank(config.getConfigVersion()) && existing != null
                && !MesProRouteServiceImpl.DEFAULT_SCHEDULE_CONFIG_VERSION.equals(existing.getConfigVersion())) {
            config.setConfigVersion(existing.getConfigVersion());
        }
    }

    private Long resolveCalendarRuleId(Long calendarRuleId) {
        if (calendarRuleId != null) {
            MesProScheduleCalendarRuleDO rule = scheduleCalendarRuleMapper.selectById(calendarRuleId);
            if (rule == null) {
                throw exception(PRO_ROUTE_SCHEDULE_CALENDAR_RULE_REQUIRED);
            }
            return calendarRuleId;
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        MesProScheduleCalendarRuleDO rule = scheduleCalendarRuleMapper.selectByTenantId(tenantId);
        if (rule == null || rule.getId() == null) {
            throw exception(PRO_ROUTE_SCHEDULE_CALENDAR_RULE_REQUIRED);
        }
        return rule.getId();
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Map<Long, BigDecimal> loadShiftHoursMap(Long routeId) {
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteId(routeId);
        if (routeProcesses == null || routeProcesses.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> workstationIds = routeProcesses.stream()
                .map(MesProRouteProcessDO::getWorkstationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        Map<Long, MesMdWorkstationDO> workstationMap = workstationIds.isEmpty()
                ? Collections.emptyMap()
                : convertMap(workstationService.getWorkstationList(workstationIds), MesMdWorkstationDO::getId);
        Map<Long, List<MesMdWorkstationDO>> workstationsByRouteProcessId = new LinkedHashMap<>();
        for (MesProRouteProcessDO routeProcess : routeProcesses) {
            MesMdWorkstationDO workstation = routeProcess.getWorkstationId() == null
                    ? null : workstationMap.get(routeProcess.getWorkstationId());
            workstationsByRouteProcessId.put(routeProcess.getId(),
                    workstation == null ? Collections.emptyList() : List.of(workstation));
        }
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        for (MesProRouteProcessDO routeProcess : routeProcesses) {
            result.put(routeProcess.getId(), resolveProcessShiftHours(
                    routeProcess.getProcessId(),
                    workstationsByRouteProcessId.getOrDefault(routeProcess.getId(), Collections.emptyList())));
        }
        return result;
    }

    private BigDecimal resolveProcessShiftHours(Long processId, List<MesMdWorkstationDO> workstations) {
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
                throw exception(PRO_ROUTE_SCHEDULE_SHIFT_HOURS_REQUIRED, processId);
            }
        }
        return normalizeShiftHours(shiftHours);
    }

    private BigDecimal requireShiftHours(BigDecimal shiftHours, Long processId) {
        if (shiftHours == null || shiftHours.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_ROUTE_SCHEDULE_SHIFT_HOURS_REQUIRED, processId);
        }
        return shiftHours;
    }

    private BigDecimal normalizeShiftHours(BigDecimal shiftHours) {
        return shiftHours == null || shiftHours.compareTo(BigDecimal.ZERO) <= 0 ? null : shiftHours;
    }

    private String appendNightShiftMark(String remark) {
        if (StrUtil.isBlank(remark)) {
            return NIGHT_SHIFT_MARK;
        }
        if (remark.contains(NIGHT_SHIFT_MARK)) {
            return remark;
        }
        return remark + "\n" + NIGHT_SHIFT_MARK;
    }

    private String removeNightShiftMark(String remark) {
        if (StrUtil.isBlank(remark) || !remark.contains(NIGHT_SHIFT_MARK)) {
            return remark;
        }
        return StrUtil.trim(remark.replace(NIGHT_SHIFT_MARK, ""));
    }

}
