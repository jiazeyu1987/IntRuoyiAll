package cn.iocoder.yudao.module.mes.service.pro.schedulerworkbench;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRoutePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRouteRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.resource.MesProRouteResourcePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.resource.MesProRouteResourceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.scheduleconfig.MesProRouteScheduleConfigRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.scheduleconfig.MesProRouteScheduleConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteFlowConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteFlowProcessConfigRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteFlowProcessConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchRouteConfigImportRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationWorkerDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleCapacityModeEnum;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationMachineService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationWorkerService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteResourceService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteScheduleConfigService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteFlowConfigService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CONFIG_PACKAGE_CONTENT_INVALID;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CONFIG_PACKAGE_FORMAT_UNSUPPORTED;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CONFIG_PACKAGE_REFERENCE_MISSING;

@Service
public class MesProSchedulerWorkbenchRouteConfigPackageServiceImpl
        implements MesProSchedulerWorkbenchRouteConfigPackageService {

    private static final String PACKAGE_VERSION = "scheduler-route-config.v1";
    private static final String ROUTE_CONFIG_PACKAGE_IMPORT_REMARK = "由排产员工作台路线配置包导入";
    private static final String HISTORICAL_HOURLY_CAPACITY_IMPORT_REMARK = "历史小时产能已按产能覆盖口径导入";
    private static final String OBSOLETE_HISTORICAL_HOURLY_CAPACITY_IMPORT_MARK =
            "LEGACY_FINITE_HOURLY_IMPORTED_AS_MANUAL_OVERRIDE";
    private static final String OBSOLETE_ROUTE_CONFIG_PACKAGE_IMPORT_REMARK =
            "Imported from scheduler workbench route config package";
    private static final String SCHEDULE_USE_TYPE = "SCHEDULE";
    private static final int EXPORT_PAGE_SIZE = 200;

    @Resource
    private MesProRouteService routeService;
    @Resource
    private MesProRouteFlowConfigService routeFlowConfigService;
    @Resource
    private MesProRouteScheduleConfigService routeScheduleConfigService;
    @Resource
    private MesProRouteResourceService routeResourceService;
    @Resource
    private MesProRouteMapper routeMapper;
    @Resource
    private MesProRouteProcessMapper routeProcessMapper;
    @Resource
    private MesProProcessMapper processMapper;
    @Resource
    private MesMdWorkstationMapper workstationMapper;
    @Resource
    private MesDvMachineryMapper machineryMapper;
    @Resource
    private MesMdWorkstationMachineService workstationMachineService;
    @Resource
    private MesMdWorkstationWorkerService workstationWorkerService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public byte[] exportPackage() {
        List<MesProRouteRespVO> routes = loadAllRoutes();

        RouteConfigPackage payload = new RouteConfigPackage();
        payload.setPackageVersion(PACKAGE_VERSION);
        payload.setExportedAt(OffsetDateTime.now().toString());
        payload.setRoutes(new ArrayList<>());
        for (MesProRouteRespVO route : routes) {
            if (route.getId() == null || route.getActiveRouteVersionId() == null) {
                continue;
            }
            RouteConfigRoutePayload routePayload = new RouteConfigRoutePayload();
            routePayload.setRouteId(route.getId());
            routePayload.setRouteCode(route.getCode());
            routePayload.setRouteName(route.getName());
            routePayload.setRouteVersionId(route.getActiveRouteVersionId());
            routePayload.setUseProcessConfigs(
                    routeFlowConfigService.getRouteFlowProcessConfigList(route.getId(), SCHEDULE_USE_TYPE));
            routePayload.setScheduleConfigs(
                    routeScheduleConfigService.getConfigRespListByRouteVersionId(route.getActiveRouteVersionId()));
            routePayload.setResources(loadAllResources(route.getId()));
            payload.getRoutes().add(routePayload);
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize scheduler route config package", ex);
        }
    }

    @Override
    public MesProSchedulerWorkbenchRouteConfigImportRespVO importPackage(byte[] content) {
        RouteConfigPackage payload = parsePackage(content);
        if (!PACKAGE_VERSION.equals(payload.getPackageVersion())) {
            throw exception(CONFIG_PACKAGE_FORMAT_UNSUPPORTED, payload.getPackageVersion());
        }
        if (payload.getRoutes() == null || payload.getRoutes().isEmpty()) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "排产路线配置包 routes 不能为空");
        }

        int flowConfigProcessCount = 0;
        int scheduleConfigCount = 0;
        int resourceCount = 0;
        for (RouteConfigRoutePayload route : payload.getRoutes()) {
            validateRoutePayload(route);
            ResolvedRouteContext resolvedContext = resolveRouteContext(route);
            routeFlowConfigService.saveRouteFlowConfigForConfigPackageImport(buildUseConfigSaveReq(route, resolvedContext));
            flowConfigProcessCount += route.getUseProcessConfigs().size();

            for (MesProRouteScheduleConfigRespVO scheduleConfig : route.getScheduleConfigs()) {
                routeScheduleConfigService.saveConfig(buildScheduleConfigSaveReq(scheduleConfig, resolvedContext));
                scheduleConfigCount++;
            }

            for (MesProRouteResourceRespVO resource : route.getResources()) {
                if ("UNCONFIGURED".equals(resource.getResourceType())) {
                    continue;
                }
                validateRouteResourceReference(resource, resolvedContext);
                resourceCount++;
            }
        }

        MesProSchedulerWorkbenchRouteConfigImportRespVO respVO =
                new MesProSchedulerWorkbenchRouteConfigImportRespVO();
        respVO.setRouteCount(payload.getRoutes().size());
        respVO.setFlowConfigProcessCount(flowConfigProcessCount);
        respVO.setScheduleConfigCount(scheduleConfigCount);
        respVO.setResourceCount(resourceCount);
        return respVO;
    }

    private List<MesProRouteResourceRespVO> loadAllResources(Long routeId) {
        List<MesProRouteResourceRespVO> result = new ArrayList<>();
        int pageNo = 1;
        while (true) {
            MesProRouteResourcePageReqVO reqVO = new MesProRouteResourcePageReqVO();
            reqVO.setPageNo(pageNo);
            reqVO.setPageSize(EXPORT_PAGE_SIZE);
            reqVO.setRouteId(routeId);
            PageResult<MesProRouteResourceRespVO> page = routeResourceService.getResourcePage(reqVO);
            result.addAll(page.getList());
            if (page.getList().size() < EXPORT_PAGE_SIZE) {
                break;
            }
            pageNo++;
        }
        return result;
    }

    private List<MesProRouteRespVO> loadAllRoutes() {
        List<MesProRouteRespVO> result = new ArrayList<>();
        int pageNo = 1;
        while (true) {
            MesProRoutePageReqVO pageReqVO = new MesProRoutePageReqVO();
            pageReqVO.setPageNo(pageNo);
            pageReqVO.setPageSize(EXPORT_PAGE_SIZE);
            PageResult<MesProRouteRespVO> page = routeService.getRoutePageRespVO(pageReqVO);
            result.addAll(page.getList());
            if (page.getList().size() < EXPORT_PAGE_SIZE) {
                break;
            }
            pageNo++;
        }
        return result;
    }

    private RouteConfigPackage parsePackage(byte[] content) {
        try {
            return objectMapper.readValue(content, RouteConfigPackage.class);
        } catch (IOException ex) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "排产路线配置包 JSON 非法");
        }
    }

    private void validateRoutePayload(RouteConfigRoutePayload route) {
        if (route == null) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "排产路线配置包包含空路线项");
        }
        if (route.getRouteId() == null) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "排产路线配置包缺少 routeId");
        }
        if (StrUtil.isBlank(route.getRouteCode())) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID,
                    "排产路线配置包缺少 routeCode，routeId=" + route.getRouteId());
        }
        if (route.getRouteVersionId() == null) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID,
                    "排产路线配置包缺少 routeVersionId，routeCode=" + route.getRouteCode());
        }
        if (route.getUseProcessConfigs() == null) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID,
                    "排产路线配置包缺少 flowProcessConfigs，routeCode=" + route.getRouteCode());
        }
        if (route.getScheduleConfigs() == null) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID,
                    "排产路线配置包缺少 scheduleConfigs，routeCode=" + route.getRouteCode());
        }
        if (route.getResources() == null) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID,
                    "排产路线配置包缺少 resources，routeCode=" + route.getRouteCode());
        }
    }

    private MesProRouteFlowConfigSaveReqVO buildUseConfigSaveReq(RouteConfigRoutePayload route,
                                                                ResolvedRouteContext resolvedContext) {
        MesProRouteFlowConfigSaveReqVO reqVO = new MesProRouteFlowConfigSaveReqVO();
        reqVO.setRouteId(resolvedContext.routeId());
        reqVO.setRouteVersionId(resolvedContext.routeVersionId());
        reqVO.setUseType(SCHEDULE_USE_TYPE);
        reqVO.setConfigVersion(PACKAGE_VERSION);
        reqVO.setRemark(ROUTE_CONFIG_PACKAGE_IMPORT_REMARK);
        reqVO.setProcessConfigs(route.getUseProcessConfigs().stream()
                .map(item -> buildUseProcessConfigSaveReq(item, resolvedContext))
                .toList());
        return reqVO;
    }

    private MesProRouteFlowProcessConfigSaveReqVO buildUseProcessConfigSaveReq(
            MesProRouteFlowProcessConfigRespVO processConfig, ResolvedRouteContext resolvedContext) {
        MesProRouteFlowProcessConfigSaveReqVO reqVO = new MesProRouteFlowProcessConfigSaveReqVO();
        reqVO.setRouteProcessId(resolvedContext.resolveRouteProcessId(processConfig.getRouteProcessId(),
                processConfig.getProcessCode(), processConfig.getSort()));
        reqVO.setEnabled(processConfig.getEnabled());
        reqVO.setExecutionMode(processConfig.getExecutionMode());
        reqVO.setRemark(processConfig.getRemark());
        reqVO.setBatchRecordReports(List.of());
        return reqVO;
    }

    private MesProRouteScheduleConfigSaveReqVO buildScheduleConfigSaveReq(
            MesProRouteScheduleConfigRespVO scheduleConfig, ResolvedRouteContext resolvedContext) {
        MesProRouteScheduleConfigSaveReqVO reqVO = new MesProRouteScheduleConfigSaveReqVO();
        reqVO.setRouteVersionId(resolvedContext.routeVersionId());
        reqVO.setRouteProcessId(resolvedContext.resolveRouteProcessId(scheduleConfig.getRouteProcessId(),
                null, null));
        boolean legacyFiniteHourly = MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode()
                .equals(scheduleConfig.getCapacityMode());
        reqVO.setCapacityMode(normalizeImportedCapacityMode(scheduleConfig.getCapacityMode()));
        reqVO.setHourlyCapacity(scheduleConfig.getHourlyCapacity());
        reqVO.setInfiniteDurationQuantityFactor(scheduleConfig.getInfiniteDurationQuantityFactor());
        reqVO.setInfiniteDurationBaseMinutes(scheduleConfig.getInfiniteDurationBaseMinutes());
        reqVO.setNightShiftEnabled(scheduleConfig.getNightShiftEnabled());
        reqVO.setCalendarRuleId(scheduleConfig.getCalendarRuleId());
        reqVO.setConfigVersion(scheduleConfig.getConfigVersion());
        reqVO.setRemark(legacyFiniteHourly
                ? appendHistoricalHourlyCapacityImportRemark(scheduleConfig.getRemark())
                : scheduleConfig.getRemark());
        return reqVO;
    }

    private String normalizeImportedCapacityMode(String capacityMode) {
        return MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode().equals(capacityMode)
                ? MesProScheduleCapacityModeEnum.MANUAL_OVERRIDE.getMode()
                : capacityMode;
    }

    private String appendHistoricalHourlyCapacityImportRemark(String remark) {
        String normalizedRemark = removeObsoleteImportRemarks(remark);
        if (StrUtil.contains(normalizedRemark, HISTORICAL_HOURLY_CAPACITY_IMPORT_REMARK)) {
            return normalizedRemark;
        }
        if (StrUtil.isBlank(normalizedRemark)) {
            return HISTORICAL_HOURLY_CAPACITY_IMPORT_REMARK;
        }
        return normalizedRemark + "; " + HISTORICAL_HOURLY_CAPACITY_IMPORT_REMARK;
    }

    private String removeObsoleteImportRemarks(String remark) {
        if (StrUtil.isBlank(remark)) {
            return remark;
        }
        return StrUtil.splitTrim(remark, ';').stream()
                .filter(StrUtil::isNotBlank)
                .filter(item -> !OBSOLETE_HISTORICAL_HOURLY_CAPACITY_IMPORT_MARK.equals(item))
                .filter(item -> !OBSOLETE_ROUTE_CONFIG_PACKAGE_IMPORT_REMARK.equals(item))
                .collect(Collectors.joining("; "));
    }

    private void validateRouteResourceReference(MesProRouteResourceRespVO resource,
                                                ResolvedRouteContext resolvedContext) {
        Long workstationId = resolvedContext.resolveWorkstationId(resource.getWorkstationCode(), resource.getWorkstationId());
        if ("MACHINE".equals(resource.getResourceType())) {
            resolvedContext.resolveWorkstationMachineId(resource, workstationId);
            return;
        }
        if ("WORKER".equals(resource.getResourceType())) {
            resolvedContext.resolveWorker(resource, workstationId);
            return;
        }
        throw exception(CONFIG_PACKAGE_CONTENT_INVALID,
                "路线编码【" + resolvedContext.routeCode() + "】包含不支持的资源类型，resourceType="
                        + resource.getResourceType());
    }

    private ResolvedRouteContext resolveRouteContext(RouteConfigRoutePayload route) {
        MesProRouteRespVO targetRoute = findTargetRoute(route.getRouteCode());
        if (targetRoute.getId() == null) {
            throw exception(CONFIG_PACKAGE_REFERENCE_MISSING,
                    "路线编码【" + route.getRouteCode() + "】在目标环境缺少 routeId");
        }
        if (targetRoute.getActiveRouteVersionId() == null) {
            throw exception(CONFIG_PACKAGE_REFERENCE_MISSING,
                    "路线编码【" + route.getRouteCode() + "】在目标环境缺少 activeRouteVersionId");
        }
        Map<Long, ProcessMatchKey> sourceProcessKeys = collectSourceProcessKeys(route);
        Map<Long, Long> routeProcessIdMap = new HashMap<>();
        for (Map.Entry<Long, ProcessMatchKey> entry : sourceProcessKeys.entrySet()) {
            routeProcessIdMap.put(entry.getKey(),
                    resolveTargetRouteProcessId(targetRoute.getId(), route.getRouteCode(), entry.getValue()));
        }
        Map<String, Long> workstationIdByCode = collectWorkstationIds(route.getResources());
        Map<Long, List<MesMdWorkstationMachineDO>> machineBindings = collectMachineBindings(workstationIdByCode.values());
        Map<Long, List<MesMdWorkstationWorkerDO>> workerBindings = collectWorkerBindings(workstationIdByCode.values());
        return new ResolvedRouteContext(route.getRouteCode(), targetRoute.getId(), targetRoute.getActiveRouteVersionId(),
                routeProcessIdMap, workstationIdByCode, machineBindings, workerBindings);
    }

    private MesProRouteRespVO findTargetRoute(String routeCode) {
        MesProRoutePageReqVO reqVO = new MesProRoutePageReqVO();
        reqVO.setCode(routeCode);
        List<MesProRouteRespVO> routes = routeService.getRoutePageRespVO(reqVO).getList();
        return routes.stream()
                .filter(item -> StrUtil.equals(routeCode, item.getCode()))
                .findFirst()
                .orElseThrow(() -> exception(CONFIG_PACKAGE_REFERENCE_MISSING,
                        "路线编码【" + routeCode + "】在目标环境不存在"));
    }

    private Map<Long, ProcessMatchKey> collectSourceProcessKeys(RouteConfigRoutePayload route) {
        Map<Long, ProcessMatchKey> result = new HashMap<>();
        for (MesProRouteFlowProcessConfigRespVO item : route.getUseProcessConfigs()) {
            if (item.getRouteProcessId() != null) {
                result.put(item.getRouteProcessId(), new ProcessMatchKey(item.getProcessCode(), item.getSort()));
            }
        }
        for (MesProRouteResourceRespVO item : route.getResources()) {
            if (item.getRouteProcessId() != null) {
                result.putIfAbsent(item.getRouteProcessId(), new ProcessMatchKey(item.getProcessCode(), item.getSort()));
            }
        }
        for (MesProRouteScheduleConfigRespVO item : route.getScheduleConfigs()) {
            if (item.getRouteProcessId() != null) {
                result.putIfAbsent(item.getRouteProcessId(), new ProcessMatchKey(null, null));
            }
        }
        return result;
    }

    private Long resolveTargetRouteProcessId(Long targetRouteId, String routeCode, ProcessMatchKey key) {
        if (key.sort() != null) {
            MesProRouteProcessDO routeProcess = routeProcessMapper.selectByRouteIdAndSort(targetRouteId, key.sort());
            if (routeProcess != null) {
                if (StrUtil.isBlank(key.processCode())) {
                    return routeProcess.getId();
                }
                MesProProcessDO process = processMapper.selectById(routeProcess.getProcessId());
                if (process != null && StrUtil.equalsIgnoreCase(key.processCode(), process.getCode())) {
                    return routeProcess.getId();
                }
            }
        }
        if (StrUtil.isNotBlank(key.processCode())) {
            for (MesProProcessDO process : processMapper.selectListByCodes(List.of(key.processCode()))) {
                MesProRouteProcessDO routeProcess = routeProcessMapper.selectByRouteIdAndProcessId(targetRouteId, process.getId());
                if (routeProcess != null) {
                    return routeProcess.getId();
                }
            }
        }
        if (key.sort() != null) {
            MesProRouteProcessDO routeProcess = routeProcessMapper.selectByRouteIdAndSort(targetRouteId, key.sort());
            if (routeProcess != null) {
                return routeProcess.getId();
            }
        }
        throw exception(CONFIG_PACKAGE_REFERENCE_MISSING,
                "路线编码【" + routeCode + "】下未找到匹配工序，processCode="
                        + key.processCode() + "，sort=" + key.sort());
    }

    private Map<String, Long> collectWorkstationIds(List<MesProRouteResourceRespVO> resources) {
        Map<String, Long> result = new HashMap<>();
        for (MesProRouteResourceRespVO resource : resources) {
            if (StrUtil.isBlank(resource.getWorkstationCode())) {
                continue;
            }
            MesMdWorkstationDO workstation = workstationMapper.selectByCode(resource.getWorkstationCode());
            if (workstation == null || workstation.getId() == null) {
                throw exception(CONFIG_PACKAGE_REFERENCE_MISSING,
                        "工位编码【" + resource.getWorkstationCode() + "】在目标环境不存在");
            }
            result.put(resource.getWorkstationCode(), workstation.getId());
        }
        return result;
    }

    private Map<Long, List<MesMdWorkstationMachineDO>> collectMachineBindings(Collection<Long> workstationIds) {
        if (workstationIds.isEmpty()) {
            return Map.of();
        }
        return workstationMachineService.getWorkstationMachineListByWorkstationIds(workstationIds).stream()
                .collect(Collectors.groupingBy(MesMdWorkstationMachineDO::getWorkstationId));
    }

    private Map<Long, List<MesMdWorkstationWorkerDO>> collectWorkerBindings(Collection<Long> workstationIds) {
        if (workstationIds.isEmpty()) {
            return Map.of();
        }
        return workstationWorkerService.getWorkstationWorkerListByWorkstationIds(workstationIds).stream()
                .collect(Collectors.groupingBy(MesMdWorkstationWorkerDO::getWorkstationId));
    }

    private record ProcessMatchKey(String processCode, Integer sort) {
    }

    private record ResolvedWorker(Long workerId, Long postId) {
    }

    private final class ResolvedRouteContext {

        private final String routeCode;
        private final Long routeId;
        private final Long routeVersionId;
        private final Map<Long, Long> routeProcessIdMap;
        private final Map<String, Long> workstationIdByCode;
        private final Map<Long, List<MesMdWorkstationMachineDO>> machineBindings;
        private final Map<Long, List<MesMdWorkstationWorkerDO>> workerBindings;

        private ResolvedRouteContext(String routeCode, Long routeId, Long routeVersionId, Map<Long, Long> routeProcessIdMap,
                                     Map<String, Long> workstationIdByCode,
                                     Map<Long, List<MesMdWorkstationMachineDO>> machineBindings,
                                     Map<Long, List<MesMdWorkstationWorkerDO>> workerBindings) {
            this.routeCode = routeCode;
            this.routeId = routeId;
            this.routeVersionId = routeVersionId;
            this.routeProcessIdMap = routeProcessIdMap;
            this.workstationIdByCode = workstationIdByCode;
            this.machineBindings = machineBindings;
            this.workerBindings = workerBindings;
        }

        private Long routeId() {
            return routeId;
        }

        private Long routeVersionId() {
            return routeVersionId;
        }

        private String routeCode() {
            return routeCode;
        }

        private Long resolveRouteProcessId(Long sourceRouteProcessId, String processCode, Integer sort) {
            Long targetId = routeProcessIdMap.get(sourceRouteProcessId);
            if (targetId != null) {
                return targetId;
            }
            throw exception(CONFIG_PACKAGE_REFERENCE_MISSING,
                    "路线编码【" + routeCode + "】下的工序引用无法解析，sourceRouteProcessId="
                            + sourceRouteProcessId + "，processCode=" + processCode + "，sort=" + sort);
        }

        private Long resolveWorkstationId(String workstationCode, Long sourceWorkstationId) {
            if (StrUtil.isNotBlank(workstationCode)) {
                Long targetId = workstationIdByCode.get(workstationCode);
                if (targetId != null) {
                    return targetId;
                }
            }
            throw exception(CONFIG_PACKAGE_REFERENCE_MISSING,
                    "工位编码【" + workstationCode + "】在目标环境不存在");
        }

        private Long resolveWorkstationMachineId(MesProRouteResourceRespVO resource, Long workstationId) {
            if (!Objects.equals("MACHINE", resource.getResourceType())) {
                return null;
            }
            if (StrUtil.isBlank(resource.getMachineryCode())) {
                throw exception(CONFIG_PACKAGE_CONTENT_INVALID,
                        "路线编码【" + routeCode + "】的设备资源缺少 machineryCode，workstationCode="
                                + resource.getWorkstationCode());
            }
            MesDvMachineryDO machinery = machineryMapper.selectByCode(resource.getMachineryCode());
            if (machinery == null || machinery.getId() == null) {
                throw exception(CONFIG_PACKAGE_REFERENCE_MISSING,
                        "设备编码【" + resource.getMachineryCode() + "】在目标环境不存在");
            }
            return machineBindings.getOrDefault(workstationId, List.of()).stream()
                    .filter(item -> ObjUtil.equal(item.getMachineryId(), machinery.getId()))
                    .map(MesMdWorkstationMachineDO::getId)
                    .findFirst()
                    .orElseThrow(() -> exception(CONFIG_PACKAGE_REFERENCE_MISSING,
                            "工位编码【" + resource.getWorkstationCode() + "】未绑定设备编码【"
                                    + resource.getMachineryCode() + "】"));
        }

        private ResolvedWorker resolveWorker(MesProRouteResourceRespVO resource, Long workstationId) {
            if (!Objects.equals("WORKER", resource.getResourceType())) {
                return new ResolvedWorker(null, null);
            }
            List<MesMdWorkstationWorkerDO> bindings = workerBindings.getOrDefault(workstationId, List.of());
            if (resource.getPostId() != null) {
                return bindings.stream()
                        .filter(item -> ObjUtil.equal(item.getPostId(), resource.getPostId()))
                        .map(item -> new ResolvedWorker(item.getId(), item.getPostId()))
                        .findFirst()
                        .orElseThrow(() -> exception(CONFIG_PACKAGE_REFERENCE_MISSING,
                                "工位编码【" + resource.getWorkstationCode() + "】未绑定岗位【"
                                        + resource.getPostId() + "】"));
            }
            MesMdWorkstationWorkerDO worker = bindings.stream()
                    .min(Comparator.comparing(MesMdWorkstationWorkerDO::getId, Comparator.nullsLast(Long::compareTo)))
                    .orElse(null);
            if (worker == null) {
                throw exception(CONFIG_PACKAGE_REFERENCE_MISSING,
                        "工位编码【" + resource.getWorkstationCode() + "】未绑定人力资源");
            }
            return new ResolvedWorker(worker.getId(), worker.getPostId());
        }
    }

    public static class RouteConfigPackage {
        private String packageVersion;
        private String exportedAt;
        private List<RouteConfigRoutePayload> routes;

        public String getPackageVersion() {
            return packageVersion;
        }

        public void setPackageVersion(String packageVersion) {
            this.packageVersion = packageVersion;
        }

        public String getExportedAt() {
            return exportedAt;
        }

        public void setExportedAt(String exportedAt) {
            this.exportedAt = exportedAt;
        }

        public List<RouteConfigRoutePayload> getRoutes() {
            return routes;
        }

        public void setRoutes(List<RouteConfigRoutePayload> routes) {
            this.routes = routes;
        }
    }

    public static class RouteConfigRoutePayload {
        private Long routeId;
        private String routeCode;
        private String routeName;
        private Long routeVersionId;
        private List<MesProRouteFlowProcessConfigRespVO> flowProcessConfigs;
        private List<MesProRouteScheduleConfigRespVO> scheduleConfigs;
        private List<MesProRouteResourceRespVO> resources;

        public Long getRouteId() {
            return routeId;
        }

        public void setRouteId(Long routeId) {
            this.routeId = routeId;
        }

        public String getRouteCode() {
            return routeCode;
        }

        public void setRouteCode(String routeCode) {
            this.routeCode = routeCode;
        }

        public String getRouteName() {
            return routeName;
        }

        public void setRouteName(String routeName) {
            this.routeName = routeName;
        }

        public Long getRouteVersionId() {
            return routeVersionId;
        }

        public void setRouteVersionId(Long routeVersionId) {
            this.routeVersionId = routeVersionId;
        }

        public List<MesProRouteFlowProcessConfigRespVO> getUseProcessConfigs() {
            return flowProcessConfigs;
        }

        public void setUseProcessConfigs(List<MesProRouteFlowProcessConfigRespVO> flowProcessConfigs) {
            this.flowProcessConfigs = flowProcessConfigs;
        }

        public List<MesProRouteScheduleConfigRespVO> getScheduleConfigs() {
            return scheduleConfigs;
        }

        public void setScheduleConfigs(List<MesProRouteScheduleConfigRespVO> scheduleConfigs) {
            this.scheduleConfigs = scheduleConfigs;
        }

        public List<MesProRouteResourceRespVO> getResources() {
            return resources;
        }

        public void setResources(List<MesProRouteResourceRespVO> resources) {
            this.resources = resources;
        }
    }
}
