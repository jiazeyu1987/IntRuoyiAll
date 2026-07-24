package cn.iocoder.yudao.module.mes.service.pro.dccprojectgovernance;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordDefinitionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordDefinitionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordFormSlotType;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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
    private MesProRouteMapper routeMapper;
    @Resource
    private MesProBatchRecordDefinitionMapper definitionMapper;
    @Resource
    private MesProBatchRecordVersionMapper versionMapper;
    @Resource
    private MesProBatchRecordReportMapper reportMapper;

    @Override
    public List<MesProDccProjectGovernanceStatus> getStatus(List<String> projectNames) {
        List<String> normalizedProjectNames = normalizeProjectNames(projectNames);
        return normalizedProjectNames.stream()
                .map(this::buildStatus)
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

    private MesProDccProjectGovernanceStatus buildStatus(String projectName) {
        List<String> blockers = new ArrayList<>();
        List<DccProjectCodeDO> projectCodes = dccProjectCodeMapper.selectEnabledListByProjectName(projectName);
        List<MesProRouteDO> routes = routeMapper.selectListByName(projectName);
        List<MesProBatchRecordDefinitionDO> definitions = definitionMapper.selectListByBatchRecordName(projectName);
        SlotStatus mainBatchRecord = resolveMainBatchRecordStatus(definitions);
        SlotStatus lossReport = resolveFormSlotStatus(projectName, MesProBatchRecordFormSlotType.LOSS_REPORT);
        SlotStatus processInspection = resolveFormSlotStatus(projectName, MesProBatchRecordFormSlotType.PROCESS_INSPECTION);
        SlotStatus parameterRecord = resolveFormSlotStatus(projectName, MesProBatchRecordFormSlotType.PARAMETER_RECORD);

        String routeStatus = statusByCount(routes.size());
        appendBlocker(blockers, "工艺路线", routeStatus, routes.size(), routes.stream().map(this::formatRouteCode).toList());
        appendBlocker(blockers, "主批记录", mainBatchRecord.status(), mainBatchRecord.count(), mainBatchRecord.identifiers());
        appendBlocker(blockers, "损耗单", lossReport.status(), lossReport.count(), lossReport.identifiers());
        appendBlocker(blockers, "过程检验单", processInspection.status(), processInspection.count(), processInspection.identifiers());
        appendBlocker(blockers, "参数记录表", parameterRecord.status(), parameterRecord.count(), parameterRecord.identifiers());

        return MesProDccProjectGovernanceStatus.builder()
                .projectName(projectName)
                .dccProjectCodeCount(projectCodes.size())
                .routeStatus(routeStatus)
                .routeCount((long) routes.size())
                .routeCodes(routes.stream().map(this::formatRouteCode).toList())
                .mainBatchRecordStatus(mainBatchRecord.status())
                .mainBatchRecordCount(mainBatchRecord.count())
                .mainBatchRecordVersionNos(mainBatchRecord.identifiers())
                .lossReportStatus(lossReport.status())
                .lossReportCount(lossReport.count())
                .lossReportCodes(lossReport.identifiers())
                .processInspectionStatus(processInspection.status())
                .processInspectionCount(processInspection.count())
                .processInspectionCodes(processInspection.identifiers())
                .parameterRecordStatus(parameterRecord.status())
                .parameterRecordCount(parameterRecord.count())
                .parameterRecordCodes(parameterRecord.identifiers())
                .blockerMessages(blockers)
                .build();
    }

    private SlotStatus resolveMainBatchRecordStatus(List<MesProBatchRecordDefinitionDO> definitions) {
        if (definitions.isEmpty()) {
            return new SlotStatus(STATUS_MISSING, 0L, List.of());
        }
        List<String> identifiers = definitions.stream()
                .map(this::formatMainBatchRecord)
                .toList();
        if (definitions.size() > 1) {
            return new SlotStatus(STATUS_DUPLICATE, (long) definitions.size(), identifiers);
        }
        Long reportCount = reportMapper.countMainByDefinitionId(definitions.get(0).getId(),
                MesProBatchRecordFormSlotType.MAIN.getType());
        if (reportCount == null || reportCount == 0) {
            return new SlotStatus(STATUS_MISSING, 0L, identifiers);
        }
        return new SlotStatus(STATUS_OK, 1L, identifiers);
    }

    private SlotStatus resolveFormSlotStatus(String projectName, MesProBatchRecordFormSlotType formSlotType) {
        List<MesProBatchRecordReportDO> reports = reportMapper.selectListByBatchRecordNameAndFormSlotType(
                projectName, formSlotType.getType());
        Set<String> identifiers = new LinkedHashSet<>();
        for (MesProBatchRecordReportDO report : reports) {
            identifiers.add(StrUtil.blankToDefault(report.getReportCode(), report.getReportId()));
        }
        return new SlotStatus(statusByCount(identifiers.size()), (long) identifiers.size(), List.copyOf(identifiers));
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

    private String formatMainBatchRecord(MesProBatchRecordDefinitionDO definition) {
        MesProBatchRecordVersionDO version = definition.getCurrentVersionId() == null
                ? null : versionMapper.selectById(definition.getCurrentVersionId());
        String routeKey = StrUtil.blankToDefault(definition.getRouteKey(), "-");
        String versionNo = version == null ? "-" : StrUtil.blankToDefault(version.getVersionNo(), "-");
        return routeKey + "/" + versionNo;
    }

    private record SlotStatus(String status, Long count, List<String> identifiers) {
    }
}
