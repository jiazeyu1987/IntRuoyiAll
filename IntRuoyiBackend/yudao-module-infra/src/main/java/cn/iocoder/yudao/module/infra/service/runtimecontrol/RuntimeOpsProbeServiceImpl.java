package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlAlertCreateReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlAlertRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlProbeLatestRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlProbeRespVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RuntimeOpsProbeServiceImpl implements RuntimeOpsProbeService {

    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(3);
    private static final int FAILURE_ALERT_THRESHOLD = 1;

    private final RuntimeControlProperties properties;
    private final RuntimeOpsProbeStore probeStore;
    private final RuntimeOpsProbeHttpClient httpClient;
    private final RuntimeOpsAlertService alertService;

    public RuntimeOpsProbeServiceImpl(RuntimeControlProperties properties, RuntimeOpsProbeStore probeStore,
                                      RuntimeOpsProbeHttpClient httpClient, RuntimeOpsAlertService alertService) {
        this.properties = properties;
        this.probeStore = probeStore;
        this.httpClient = httpClient;
        this.alertService = alertService;
    }

    @Override
    public RuntimeControlProbeLatestRespVO runProbes() {
        LocalDateTime sampledAt = LocalDateTime.now();
        List<RuntimeControlProbeRespVO> probes = collectProbeTargets().stream()
                .map(target -> runProbe(target, sampledAt))
                .toList();
        List<RuntimeControlProbeRespVO> effectiveProbes = probes.isEmpty()
                ? List.of(blockedMissingProbeConfig(sampledAt))
                : probes;
        RuntimeControlProbeLatestRespVO latest = new RuntimeControlProbeLatestRespVO();
        latest.setSampledAt(sampledAt);
        latest.setProbes(effectiveProbes);
        latest.setStatus(RuntimeOpsBusinessHealthServiceImpl.aggregate(effectiveProbes.stream()
                .map(RuntimeControlProbeRespVO::getStatus).toList()));
        probeStore.save(latest);
        RuntimeControlAlertRespVO alert = createAlertWhenThresholdReached(latest);
        latest.setAlert(alert);
        return probeStore.save(latest);
    }

    @Override
    public RuntimeControlProbeLatestRespVO getLatestProbes() {
        RuntimeControlProbeLatestRespVO latest = probeStore.readLatest();
        if (latest != null) {
            return latest;
        }
        LocalDateTime sampledAt = LocalDateTime.now();
        RuntimeControlProbeLatestRespVO response = new RuntimeControlProbeLatestRespVO();
        response.setStatus(RuntimeOpsInspectionStatus.NO_GO);
        response.setSampledAt(sampledAt);
        response.setProbes(List.of(blockedNeverRun(sampledAt)));
        return response;
    }

    private RuntimeControlProbeRespVO runProbe(ProbeTarget target, LocalDateTime sampledAt) {
        RuntimeControlProbeRespVO probe = new RuntimeControlProbeRespVO();
        probe.setEnvironment(target.environment());
        probe.setComponent(target.component());
        probe.setProbeType(target.probeType());
        probe.setUrl(target.url());
        probe.setSampledAt(sampledAt);
        if (StrUtil.isBlank(target.url())) {
            probe.setStatus(RuntimeOpsInspectionStatus.BLOCKED);
            probe.setError("探针 URL 未配置：" + target.environment() + "/" + target.component());
            return probe;
        }
        try {
            RuntimeOpsProbeHttpResult result = httpClient.probe(target.url(), PROBE_TIMEOUT);
            probe.setHttpStatusCode(result.getStatusCode());
            probe.setDurationMillis(result.getDurationMillis());
            probe.setStatus(statusOfHttp(result.getStatusCode()));
            if (RuntimeOpsInspectionStatus.PASS != probe.getStatus()) {
                probe.setError("HTTP " + result.getStatusCode());
            }
            return probe;
        } catch (RuntimeException ex) {
            probe.setStatus(RuntimeOpsInspectionStatus.NO_GO);
            probe.setError("探针不可达：" + StrUtil.blankToDefault(ex.getMessage(), ex.getClass().getSimpleName()));
            return probe;
        }
    }

    private RuntimeOpsInspectionStatus statusOfHttp(Integer statusCode) {
        if (statusCode == null) {
            return RuntimeOpsInspectionStatus.BLOCKED;
        }
        if (statusCode >= 200 && statusCode < 400) {
            return RuntimeOpsInspectionStatus.PASS;
        }
        if (statusCode >= 400 && statusCode < 500) {
            return RuntimeOpsInspectionStatus.WARN;
        }
        return RuntimeOpsInspectionStatus.NO_GO;
    }

    private RuntimeControlAlertRespVO createAlertWhenThresholdReached(RuntimeControlProbeLatestRespVO latest) {
        List<RuntimeControlProbeRespVO> failures = latest.getProbes().stream()
                .filter(probe -> RuntimeOpsInspectionStatus.NO_GO == probe.getStatus()
                        || RuntimeOpsInspectionStatus.BLOCKED == probe.getStatus())
                .toList();
        if (failures.size() < FAILURE_ALERT_THRESHOLD) {
            return null;
        }
        String environment = StrUtil.blankToDefault(failures.get(0).getEnvironment(), "local");
        RuntimeControlAlertCreateReqVO reqVO = new RuntimeControlAlertCreateReqVO();
        reqVO.setEnvironment(environment);
        reqVO.setAction("probe-failed");
        reqVO.setSeverity("NO_GO");
        reqVO.setTitle("探针失败");
        reqVO.setContent(buildFailureContent(failures));
        reqVO.setNotifyTemplateCode("RUNTIME_OPS_ALERT");
        reqVO.setTemplateParams(new LinkedHashMap<>(Map.of(
                "environment", environment,
                "action", "probe-failed",
                "severity", "NO_GO",
                "title", "探针失败",
                "content", reqVO.getContent()
        )));
        return alertService.createAlert(reqVO);
    }

    private String buildFailureContent(List<RuntimeControlProbeRespVO> failures) {
        return failures.stream()
                .map(probe -> probe.getEnvironment() + "/" + probe.getComponent() + " "
                        + probe.getStatus() + targetText(probe) + " "
                        + StrUtil.blankToDefault(probe.getError(), "无错误详情"))
                .reduce((left, right) -> left + "; " + right)
                .orElse("探针失败");
    }

    private String targetText(RuntimeControlProbeRespVO probe) {
        return StrUtil.isBlank(probe.getUrl()) ? "" : " 目标=" + probe.getUrl();
    }

    private List<ProbeTarget> collectProbeTargets() {
        List<ProbeTarget> targets = new ArrayList<>();
        properties.getEnvironments().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> entry.getValue().getTargets().entrySet().stream()
                        .sorted(Comparator.comparing(Map.Entry::getKey))
                        .filter(targetEntry -> isProbeType(targetEntry.getValue().getActionComponent()))
                        .forEach(targetEntry -> targets.add(new ProbeTarget(entry.getKey(), targetEntry.getKey(),
                                targetEntry.getValue().getActionComponent(), targetEntry.getValue().getUrl()))));
        return targets;
    }

    private boolean isProbeType(String actionComponent) {
        return "backend".equals(actionComponent) || "frontend".equals(actionComponent)
                || "website".equals(actionComponent);
    }

    private RuntimeControlProbeRespVO blockedMissingProbeConfig(LocalDateTime sampledAt) {
        RuntimeControlProbeRespVO probe = new RuntimeControlProbeRespVO();
        probe.setEnvironment("runtime-control");
        probe.setComponent("backend/frontend/website");
        probe.setProbeType("config");
        probe.setStatus(RuntimeOpsInspectionStatus.BLOCKED);
        probe.setError("未配置 backend/frontend/website 探针目标");
        probe.setSampledAt(sampledAt);
        return probe;
    }

    private RuntimeControlProbeRespVO blockedNeverRun(LocalDateTime sampledAt) {
        RuntimeControlProbeRespVO probe = new RuntimeControlProbeRespVO();
        probe.setEnvironment("runtime-control");
        probe.setComponent("backend/frontend/website");
        probe.setProbeType("latest");
        probe.setStatus(RuntimeOpsInspectionStatus.BLOCKED);
        probe.setError("尚未执行探针，不能展示 PASS");
        probe.setSampledAt(sampledAt);
        return probe;
    }

    private record ProbeTarget(String environment, String component, String probeType, String url) {
    }
}
