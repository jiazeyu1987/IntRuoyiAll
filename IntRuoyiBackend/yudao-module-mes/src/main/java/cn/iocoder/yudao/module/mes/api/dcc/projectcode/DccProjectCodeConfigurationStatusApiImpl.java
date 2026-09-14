package cn.iocoder.yudao.module.mes.api.dcc.projectcode;

import cn.iocoder.yudao.module.dcc.api.projectcode.DccProjectCodeConfigurationQuery;
import cn.iocoder.yudao.module.dcc.api.projectcode.DccProjectCodeConfigurationStatus;
import cn.iocoder.yudao.module.dcc.api.projectcode.DccProjectCodeConfigurationStatusApi;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationProjectStatusRespVO;
import cn.iocoder.yudao.module.mes.service.pro.dccprojectgovernance.MesProDccProjectGovernanceService;
import cn.iocoder.yudao.module.mes.service.pro.dccprojectgovernance.MesProDccProjectGovernanceStatus;
import cn.iocoder.yudao.module.mes.service.qa.regulation.MesQaInspectionRegulationService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Validated
public class DccProjectCodeConfigurationStatusApiImpl implements DccProjectCodeConfigurationStatusApi {

    @Resource
    private MesProDccProjectGovernanceService governanceService;
    @Resource
    private MesQaInspectionRegulationService qaRegulationService;

    @Override
    public Map<Long, DccProjectCodeConfigurationStatus> getStatus(
            Collection<DccProjectCodeConfigurationQuery> projects) {
        List<DccProjectCodeConfigurationQuery> requestedProjects = projects == null ? List.of() : projects.stream()
                .filter(Objects::nonNull)
                .filter(project -> project.projectCodeId() != null)
                .collect(Collectors.toMap(
                        DccProjectCodeConfigurationQuery::projectCodeId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new))
                .values().stream().toList();
        if (requestedProjects.isEmpty()) {
            return Map.of();
        }
        boolean routeStatusRequired = requestedProjects.stream()
                .anyMatch(DccProjectCodeConfigurationQuery::routeStatusRequired);
        boolean mainBatchRecordStatusRequired = requestedProjects.stream()
                .anyMatch(DccProjectCodeConfigurationQuery::mainBatchRecordStatusRequired);
        boolean qaRegulationStatusRequired = requestedProjects.stream()
                .anyMatch(DccProjectCodeConfigurationQuery::qaRegulationStatusRequired);

        Map<String, MesProDccProjectGovernanceStatus> governanceByProjectName =
                routeStatusRequired || mainBatchRecordStatusRequired
                        ? governanceService
                        .getStatus(requestedProjects.stream()
                                        .map(DccProjectCodeConfigurationQuery::projectName)
                                        .filter(Objects::nonNull)
                                        .distinct()
                                        .toList(),
                                routeStatusRequired, mainBatchRecordStatusRequired, false)
                        .stream()
                        .collect(Collectors.toMap(MesProDccProjectGovernanceStatus::projectName,
                                Function.identity(), (left, right) -> left, LinkedHashMap::new))
                        : Map.of();
        Map<Long, MesQaInspectionRegulationProjectStatusRespVO> qaByProjectCodeId = qaRegulationStatusRequired
                ? qaRegulationService
                .getProjectStatuses(requestedProjects.stream()
                        .map(DccProjectCodeConfigurationQuery::projectCodeId)
                        .toList())
                .stream()
                .collect(Collectors.toMap(MesQaInspectionRegulationProjectStatusRespVO::getDccProjectCodeId,
                        Function.identity(), (left, right) -> left, LinkedHashMap::new))
                : Map.of();

        return requestedProjects.stream().collect(Collectors.toMap(
                DccProjectCodeConfigurationQuery::projectCodeId,
                project -> {
                    MesProDccProjectGovernanceStatus governance = governanceByProjectName.get(project.projectName());
                    MesQaInspectionRegulationProjectStatusRespVO qa = qaByProjectCodeId.get(project.projectCodeId());
                    boolean routeConfigured = project.routeStatusRequired()
                            && isConfigured(governance == null ? null : governance.routeStatus());
                    boolean mainBatchRecordConfigured = project.mainBatchRecordStatusRequired()
                            && isConfigured(governance == null ? null : governance.mainBatchRecordStatus());
                    boolean qaRegulationConfigured = project.qaRegulationStatusRequired()
                            && Boolean.TRUE.equals(qa == null ? null : qa.getConfigured());
                    return new DccProjectCodeConfigurationStatus(
                            project.projectCodeId(),
                            routeConfigured,
                            mainBatchRecordConfigured,
                            qaRegulationConfigured);
                },
                (left, right) -> left,
                LinkedHashMap::new));
    }

    private boolean isConfigured(String status) {
        return status != null && !Objects.equals("MISSING", status);
    }
}
