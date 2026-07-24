package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlBusinessHealthRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlInspectionCheckRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlInspectionRunRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlProbeLatestRespVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RuntimeOpsInspectionServiceImpl implements RuntimeOpsInspectionService {

    private final RuntimeOpsInspectionRunStore inspectionRunStore;
    private final RuntimeOpsBusinessHealthService businessHealthService;
    private final RuntimeOpsProbeService probeService;

    public RuntimeOpsInspectionServiceImpl(RuntimeOpsInspectionRunStore inspectionRunStore,
                                           RuntimeOpsBusinessHealthService businessHealthService,
                                           RuntimeOpsProbeService probeService) {
        this.inspectionRunStore = inspectionRunStore;
        this.businessHealthService = businessHealthService;
        this.probeService = probeService;
    }

    @Override
    public RuntimeControlInspectionRunRespVO runInspection() {
        LocalDateTime startedAt = LocalDateTime.now();
        RuntimeControlBusinessHealthRespVO businessHealth = businessHealthService.getBusinessHealth();
        RuntimeControlProbeLatestRespVO probes = probeService.runProbes();
        RuntimeOpsInspectionStatus releaseStatus = RuntimeOpsBusinessHealthServiceImpl.aggregate(List.of(
                businessHealth.getStatus(), probes.getStatus()));
        List<RuntimeControlInspectionCheckRespVO> checks = List.of(
                check("business-health", "业务健康", businessHealth.getStatus(), true,
                        "items=" + businessHealth.getItems().size(), reasonOf(businessHealth.getStatus()),
                        businessHealth.getSampledAt()),
                check("probe", "backend/frontend/website 探针", probes.getStatus(), true,
                        "probes=" + probes.getProbes().size(), reasonOf(probes.getStatus()), probes.getSampledAt()),
                check("pre-release-check", "发布前检查报告", releaseStatus, true,
                        "入口=POST /infra/runtime-control/inspection-runs; 证据=business-health,probe",
                        reasonOf(releaseStatus), LocalDateTime.now()),
                check("post-release-observation", "发布后观察报告", releaseStatus, true,
                        "入口=GET /infra/runtime-control/inspection-runs/{id}; 证据=business-health,probe",
                        reasonOf(releaseStatus), LocalDateTime.now())
        );
        RuntimeControlInspectionRunRespVO run = new RuntimeControlInspectionRunRespVO();
        run.setStartedAt(startedAt);
        run.setCompletedAt(LocalDateTime.now());
        run.setChecks(checks);
        run.setStatus(RuntimeOpsBusinessHealthServiceImpl.aggregate(checks.stream()
                .map(RuntimeControlInspectionCheckRespVO::getStatus).toList()));
        run.setSummary(summaryOf(run.getStatus()));
        return inspectionRunStore.save(run);
    }

    @Override
    public RuntimeControlInspectionRunRespVO getInspectionRun(Long id) {
        return inspectionRunStore.get(id);
    }

    private RuntimeControlInspectionCheckRespVO check(String code, String name, RuntimeOpsInspectionStatus status,
                                                      boolean required, String evidence, String reason,
                                                      LocalDateTime sampledAt) {
        RuntimeControlInspectionCheckRespVO check = new RuntimeControlInspectionCheckRespVO();
        check.setCode(code);
        check.setName(name);
        check.setStatus(status);
        check.setRequired(required);
        check.setEvidence(evidence);
        check.setReason(reason);
        check.setSampledAt(sampledAt);
        return check;
    }

    private String reasonOf(RuntimeOpsInspectionStatus status) {
        if (RuntimeOpsInspectionStatus.PASS == status) {
            return null;
        }
        return "关键证据状态为 " + status + "，巡检不能 PASS";
    }

    private String summaryOf(RuntimeOpsInspectionStatus status) {
        if (RuntimeOpsInspectionStatus.PASS == status) {
            return "巡检通过";
        }
        if (RuntimeOpsInspectionStatus.WARN == status) {
            return "巡检存在告警项";
        }
        return "巡检未通过，缺关键证据或探针失败";
    }
}
