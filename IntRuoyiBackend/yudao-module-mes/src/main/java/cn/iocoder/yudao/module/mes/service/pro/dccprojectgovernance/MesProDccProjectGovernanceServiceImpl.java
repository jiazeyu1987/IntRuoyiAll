package cn.iocoder.yudao.module.mes.service.pro.dccprojectgovernance;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordDefinitionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordDefinitionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordFormSlotType;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Validated
public class MesProDccProjectGovernanceServiceImpl implements MesProDccProjectGovernanceService {

    public static final String STATUS_OK = "OK";
    public static final String STATUS_MISSING = "MISSING";
    public static final String STATUS_DUPLICATE = "DUPLICATE";

    @Resource
    private DccProjectCodeMapper dccProjectCodeMapper;
    @Resource
    private MesMdItemMapper itemMapper;
    @Resource
    private MesProRouteProductMapper routeProductMapper;
    @Resource
    private MesProRouteMapper routeMapper;
    @Resource
    private MesProRouteVersionMapper routeVersionMapper;
    @Resource
    private MesProBatchRecordDefinitionMapper definitionMapper;
    @Resource
    private MesProBatchRecordVersionMapper versionMapper;
    @Resource
    private MesProBatchRecordReportMapper reportMapper;

    @Override
    public List<MesProDccProjectGovernanceStatus> getStatus(List<String> projectNames) {
        return getStatus(projectNames, true, true, true);
    }

    @Override
    public List<MesProDccProjectGovernanceStatus> getStatus(List<String> projectNames,
                                                            boolean routeStatusRequired,
                                                            boolean mainBatchRecordStatusRequired,
                                                            boolean formSlotStatusRequired) {
        List<String> normalizedProjectNames = normalizeProjectNames(projectNames);
        return normalizedProjectNames.stream()
                .map(projectName -> buildStatus(projectName, routeStatusRequired, mainBatchRecordStatusRequired,
                        formSlotStatusRequired))
                .toList();
    }

    private List<String> normalizeProjectNames(List<String> projectNames) {
        if (projectNames == null) {
            return List.of();
        }
        return projectNames.stream()
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
    }

    private MesProDccProjectGovernanceStatus buildStatus(String projectName,
                                                         boolean routeStatusRequired,
                                                         boolean mainBatchRecordStatusRequired,
                                                         boolean formSlotStatusRequired) {
        List<String> blockers = new ArrayList<>();
        List<DccProjectCodeDO> projectCodes = routeStatusRequired
                ? dccProjectCodeMapper.selectEnabledListByProjectName(projectName) : List.of();
        List<MesProRouteDO> routes = routeStatusRequired ? resolveRoutes(projectCodes) : List.of();
        List<MesProBatchRecordDefinitionDO> definitions = mainBatchRecordStatusRequired
                ? definitionMapper.selectListByBatchRecordName(projectName) : List.of();
        SlotStatus mainBatchRecord = mainBatchRecordStatusRequired
                ? resolveMainBatchRecordStatus(definitions) : emptySlotStatus();
        SlotStatus lossReport = formSlotStatusRequired
                ? resolveFormSlotStatus(projectName, MesProBatchRecordFormSlotType.LOSS_REPORT) : emptySlotStatus();
        SlotStatus processInspection = formSlotStatusRequired
                ? resolveFormSlotStatus(projectName, MesProBatchRecordFormSlotType.PROCESS_INSPECTION) : emptySlotStatus();
        SlotStatus parameterRecord = formSlotStatusRequired
                ? resolveFormSlotStatus(projectName, MesProBatchRecordFormSlotType.PARAMETER_RECORD) : emptySlotStatus();

        String routeStatus = statusByCount(routes.size());
        List<String> routeVersionNos = resolveRouteVersionNos(routes);
        if (routeStatusRequired) {
            appendBlocker(blockers, "工艺路线", routeStatus, routes.size(), routes.stream().map(this::formatRouteCode).toList());
        }
        if (mainBatchRecordStatusRequired) {
            appendBlocker(blockers, "主批记录", mainBatchRecord.status(), mainBatchRecord.count(), mainBatchRecord.identifiers());
        }
        if (formSlotStatusRequired) {
            appendBlocker(blockers, "损耗单", lossReport.status(), lossReport.count(), lossReport.identifiers());
            appendBlocker(blockers, "过程检验单", processInspection.status(), processInspection.count(), processInspection.identifiers());
            appendBlocker(blockers, "参数记录表", parameterRecord.status(), parameterRecord.count(), parameterRecord.identifiers());
        }

        return MesProDccProjectGovernanceStatus.builder()
                .projectName(projectName)
                .dccProjectCodeCount(projectCodes.size())
                .routeStatus(routeStatus)
                .routeCount((long) routes.size())
                .routeCodes(routes.stream().map(this::formatRouteCode).toList())
                .routeVersionNos(routeVersionNos)
                .mainBatchRecordStatus(mainBatchRecord.status())
                .mainBatchRecordCount(mainBatchRecord.count())
                .mainBatchRecordVersionNos(mainBatchRecord.identifiers())
                .lossReportStatus(lossReport.status())
                .lossReportCount(lossReport.count())
                .lossReportCodes(lossReport.identifiers())
                .lossReportVersionNos(lossReport.versionNos())
                .processInspectionStatus(processInspection.status())
                .processInspectionCount(processInspection.count())
                .processInspectionCodes(processInspection.identifiers())
                .processInspectionVersionNos(processInspection.versionNos())
                .parameterRecordStatus(parameterRecord.status())
                .parameterRecordCount(parameterRecord.count())
                .parameterRecordCodes(parameterRecord.identifiers())
                .parameterRecordVersionNos(parameterRecord.versionNos())
                .blockerMessages(blockers)
                .build();
    }

    private SlotStatus emptySlotStatus() {
        return new SlotStatus(STATUS_MISSING, 0L, List.of(), List.of());
    }

    private List<MesProRouteDO> resolveRoutes(List<DccProjectCodeDO> projectCodes) {
        List<String> codes = projectCodes.stream()
                .map(DccProjectCodeDO::getProjectCode)
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
        if (codes.isEmpty()) {
            return List.of();
        }
        List<Long> itemIds = itemMapper.selectListByCodes(codes).stream()
                .map(MesMdItemDO::getId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (itemIds.isEmpty()) {
            return List.of();
        }
        List<Long> routeIds = routeProductMapper.selectListByItemIds(itemIds).stream()
                .map(MesProRouteProductDO::getRouteId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (routeIds.isEmpty()) {
            return List.of();
        }
        Map<Long, MesProRouteDO> routeById = routeMapper.selectBatchIds(routeIds).stream()
                .collect(LinkedHashMap::new, (map, route) -> map.put(route.getId(), route), Map::putAll);
        return routeIds.stream()
                .map(routeById::get)
                .filter(route -> route != null)
                .toList();
    }

    private SlotStatus resolveMainBatchRecordStatus(List<MesProBatchRecordDefinitionDO> definitions) {
        if (definitions.isEmpty()) {
            return new SlotStatus(STATUS_MISSING, 0L, List.of(), List.of());
        }
        List<String> identifiers = definitions.stream()
                .map(this::formatMainBatchRecord)
                .toList();
        if (definitions.size() > 1) {
            return new SlotStatus(STATUS_DUPLICATE, (long) definitions.size(), identifiers, List.of());
        }
        Long reportCount = reportMapper.countMainByDefinitionId(definitions.get(0).getId(),
                MesProBatchRecordFormSlotType.MAIN.getType());
        if (reportCount == null || reportCount == 0) {
            return new SlotStatus(STATUS_MISSING, 0L, identifiers, List.of());
        }
        return new SlotStatus(STATUS_OK, 1L, identifiers, List.of());
    }

    private SlotStatus resolveFormSlotStatus(String projectName, MesProBatchRecordFormSlotType formSlotType) {
        List<MesProBatchRecordReportDO> reports = reportMapper.selectListByBatchRecordNameAndFormSlotType(
                projectName, formSlotType.getType());
        Set<String> identifiers = new LinkedHashSet<>();
        for (MesProBatchRecordReportDO report : reports) {
            identifiers.add(StrUtil.blankToDefault(report.getReportCode(), report.getReportId()));
        }
        return new SlotStatus(statusByCount(identifiers.size()), (long) identifiers.size(), List.copyOf(identifiers),
                resolveBatchRecordVersionNos(reports));
    }

    private String statusByCount(int count) {
        if (count == 0) {
            return STATUS_MISSING;
        }
        if (count == 1) {
            return STATUS_OK;
        }
        return STATUS_DUPLICATE;
    }

    private void appendBlocker(List<String> blockers, String label, String status, long count, List<String> identifiers) {
        if (!STATUS_DUPLICATE.equals(status)) {
            return;
        }
        blockers.add(label + "重复 " + count + " 份：" + String.join("、", identifiers));
    }

    private String formatRouteCode(MesProRouteDO route) {
        return StrUtil.blankToDefault(route.getCode(), String.valueOf(route.getId()));
    }

    private List<String> resolveRouteVersionNos(List<MesProRouteDO> routes) {
        Set<String> versionNos = new LinkedHashSet<>();
        for (MesProRouteDO route : routes) {
            MesProRouteVersionDO activeVersion = routeVersionMapper.selectActiveByRouteId(route.getId());
            if (activeVersion != null && StrUtil.isNotBlank(activeVersion.getVersionNo())) {
                versionNos.add(StrUtil.trim(activeVersion.getVersionNo()));
            }
        }
        return List.copyOf(versionNos);
    }

    private String formatMainBatchRecord(MesProBatchRecordDefinitionDO definition) {
        MesProBatchRecordVersionDO version = definition.getCurrentVersionId() == null
                ? null : versionMapper.selectById(definition.getCurrentVersionId());
        String routeKey = StrUtil.blankToDefault(definition.getRouteKey(), "-");
        String versionNo = version == null ? "-" : StrUtil.blankToDefault(version.getVersionNo(), "-");
        return routeKey + "/" + versionNo;
    }

    private List<String> resolveBatchRecordVersionNos(List<MesProBatchRecordReportDO> reports) {
        Set<Long> versionIds = reports.stream()
                .map(MesProBatchRecordReportDO::getBatchRecordVersionId)
                .filter(id -> id != null && id > 0)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        Map<Long, MesProBatchRecordVersionDO> versionById = selectBatchRecordVersionsById(versionIds);
        Set<String> versionNos = new LinkedHashSet<>();
        for (MesProBatchRecordReportDO report : reports) {
            MesProBatchRecordVersionDO version = versionById.get(report.getBatchRecordVersionId());
            if (version != null && StrUtil.isNotBlank(version.getVersionNo())) {
                versionNos.add(StrUtil.trim(version.getVersionNo()));
            }
        }
        return List.copyOf(versionNos);
    }

    private Map<Long, MesProBatchRecordVersionDO> selectBatchRecordVersionsById(Collection<Long> versionIds) {
        if (versionIds == null || versionIds.isEmpty()) {
            return Map.of();
        }
        return versionMapper.selectBatchIds(versionIds).stream()
                .collect(LinkedHashMap::new, (map, version) -> map.put(version.getId(), version), Map::putAll);
    }

    private record SlotStatus(String status, Long count, List<String> identifiers, List<String> versionNos) {
    }
}
