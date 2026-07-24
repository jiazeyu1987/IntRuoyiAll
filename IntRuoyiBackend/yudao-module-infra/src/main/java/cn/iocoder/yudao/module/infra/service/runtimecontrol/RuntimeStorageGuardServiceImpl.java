package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlAlertCreateReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlCapacityStatusRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlStorageMetricRespVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RuntimeStorageGuardServiceImpl implements RuntimeStorageGuardService {

    private static final String ALERT_ACTION = "storage-capacity-warning";
    private static final String ALERT_TEMPLATE_CODE = "RUNTIME_OPS_ALERT";

    private final RuntimeControlProperties properties;
    private final RuntimeStorageGuardSnapshotStore snapshotStore;
    private final RuntimeOpsAlertService alertService;

    public RuntimeStorageGuardServiceImpl(RuntimeControlProperties properties,
                                          RuntimeStorageGuardSnapshotStore snapshotStore,
                                          RuntimeOpsAlertService alertService) {
        this.properties = properties;
        this.snapshotStore = snapshotStore;
        this.alertService = alertService;
    }

    @Override
    public RuntimeControlCapacityStatusRespVO getCapacityStatus() {
        LocalDateTime sampledAt = LocalDateTime.now();
        RuntimeControlCapacityStatusRespVO previous = snapshotStore.readLatest();
        RuntimeControlStorageMetricRespVO disk = collectDiskMetric();
        RuntimeControlStorageMetricRespVO logDirectory = collectLogDirectoryMetric(previous);
        List<String> reasons = collectReasons(disk, logDirectory);

        RuntimeControlCapacityStatusRespVO response = new RuntimeControlCapacityStatusRespVO();
        response.setSampledAt(sampledAt);
        response.setDisk(disk);
        response.setLogDirectory(logDirectory);
        response.setReasons(reasons);
        response.setStatus(aggregate(List.of(disk.getStatus(), logDirectory.getStatus())));
        return response;
    }

    @Override
    public RuntimeControlCapacityStatusRespVO refreshCapacityStatus() {
        RuntimeControlCapacityStatusRespVO response = getCapacityStatus();
        RuntimeControlStorageMetricRespVO disk = response.getDisk();
        RuntimeControlStorageMetricRespVO logDirectory = response.getLogDirectory();
        RuntimeOpsInspectionStatus alertSeverity = alertSeverity(disk, logDirectory);
        if (alertSeverity != null) {
            response.setAlert(alertService.createAlert(alertReq(response, alertSeverity)));
        }
        snapshotStore.save(response);
        return response;
    }

    private RuntimeControlStorageMetricRespVO collectDiskMetric() {
        RuntimeControlStorageMetricRespVO metric = new RuntimeControlStorageMetricRespVO();
        if (StrUtil.isBlank(properties.getStorageGuard().getMonitorPath())) {
            metric.setStatus(RuntimeOpsInspectionStatus.BLOCKED);
            metric.setReason("磁盘采样路径配置缺失：storageGuard.monitorPath");
            return metric;
        }
        Path monitorPath = resolveConfiguredPath(properties.getStorageGuard().getMonitorPath());
        metric.setPath(monitorPath.toString());
        if (!Files.exists(monitorPath) || !Files.isReadable(monitorPath)) {
            metric.setStatus(RuntimeOpsInspectionStatus.BLOCKED);
            metric.setReason("磁盘采样路径不存在或不可读：" + monitorPath);
            return metric;
        }
        try {
            FileStore fileStore = Files.getFileStore(monitorPath);
            long totalBytes = fileStore.getTotalSpace();
            long usableBytes = fileStore.getUsableSpace();
            long usedBytes = totalBytes - usableBytes;
            double usagePercent = totalBytes == 0 ? 0.0 : usedBytes * 100.0 / totalBytes;
            metric.setTotalBytes(totalBytes);
            metric.setUsableBytes(usableBytes);
            metric.setUsedBytes(usedBytes);
            metric.setUsagePercent(usagePercent);
            metric.setStatus(statusByDiskUsage(usagePercent));
            if (metric.getStatus() != RuntimeOpsInspectionStatus.PASS) {
                metric.setReason("磁盘使用率超过阈值：usagePercent=" + usagePercent
                        + ", warnPercent=" + properties.getStorageGuard().getDiskUsageWarnPercent()
                        + ", noGoPercent=" + properties.getStorageGuard().getDiskUsageNoGoPercent());
            }
            return metric;
        } catch (IOException | RuntimeException ex) {
            metric.setStatus(RuntimeOpsInspectionStatus.BLOCKED);
            metric.setReason("磁盘容量采集失败：" + failureReason(ex));
            return metric;
        }
    }

    private RuntimeControlStorageMetricRespVO collectLogDirectoryMetric(RuntimeControlCapacityStatusRespVO previous) {
        RuntimeControlStorageMetricRespVO metric = new RuntimeControlStorageMetricRespVO();
        if (StrUtil.isBlank(properties.getStorageGuard().getLogDir())) {
            metric.setStatus(RuntimeOpsInspectionStatus.BLOCKED);
            metric.setReason("日志目录配置缺失：storageGuard.logDir");
            return metric;
        }
        Path logDir = resolveConfiguredPath(properties.getStorageGuard().getLogDir());
        metric.setPath(logDir.toString());
        if (!Files.isDirectory(logDir) || !Files.isReadable(logDir)) {
            metric.setStatus(RuntimeOpsInspectionStatus.BLOCKED);
            metric.setReason("日志目录不存在或不可读：" + logDir);
            return metric;
        }
        try {
            long sizeBytes = directorySize(logDir);
            long previousSizeBytes = previous == null || previous.getLogDirectory() == null
                    || previous.getLogDirectory().getSizeBytes() == null ? sizeBytes : previous.getLogDirectory().getSizeBytes();
            long growthBytes = Math.max(0L, sizeBytes - previousSizeBytes);
            metric.setSizeBytes(sizeBytes);
            metric.setGrowthBytes(growthBytes);
            metric.setStatus(statusByLogThreshold(sizeBytes, growthBytes));
            if (metric.getStatus() != RuntimeOpsInspectionStatus.PASS) {
                metric.setReason("日志目录超过阈值：sizeBytes=" + sizeBytes + ", growthBytes=" + growthBytes
                        + ", sizeWarnBytes=" + properties.getStorageGuard().getLogDirWarnBytes()
                        + ", growthWarnBytes=" + properties.getStorageGuard().getLogGrowthWarnBytes());
            }
            return metric;
        } catch (IOException | RuntimeException ex) {
            metric.setStatus(RuntimeOpsInspectionStatus.BLOCKED);
            metric.setReason("日志目录采集失败：" + failureReason(ex));
            return metric;
        }
    }

    private long directorySize(Path root) throws IOException {
        try (java.util.stream.Stream<Path> stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile)
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (IOException ex) {
                            throw new IllegalStateException("读取日志文件大小失败：" + path + ": " + ex.getMessage(), ex);
                        }
                    })
                    .sum();
        }
    }

    private RuntimeOpsInspectionStatus statusByDiskUsage(double usagePercent) {
        Double noGoPercent = properties.getStorageGuard().getDiskUsageNoGoPercent();
        Double warnPercent = properties.getStorageGuard().getDiskUsageWarnPercent();
        if (noGoPercent != null && usagePercent >= noGoPercent) {
            return RuntimeOpsInspectionStatus.NO_GO;
        }
        if (warnPercent != null && usagePercent >= warnPercent) {
            return RuntimeOpsInspectionStatus.WARN;
        }
        return RuntimeOpsInspectionStatus.PASS;
    }

    private RuntimeOpsInspectionStatus statusByLogThreshold(long sizeBytes, long growthBytes) {
        Long sizeWarnBytes = properties.getStorageGuard().getLogDirWarnBytes();
        Long growthWarnBytes = properties.getStorageGuard().getLogGrowthWarnBytes();
        if ((sizeWarnBytes != null && sizeBytes > sizeWarnBytes)
                || (growthWarnBytes != null && growthBytes > growthWarnBytes)) {
            return RuntimeOpsInspectionStatus.WARN;
        }
        return RuntimeOpsInspectionStatus.PASS;
    }

    private List<String> collectReasons(RuntimeControlStorageMetricRespVO... metrics) {
        List<String> reasons = new ArrayList<>();
        for (RuntimeControlStorageMetricRespVO metric : metrics) {
            if (StrUtil.isNotBlank(metric.getReason())) {
                reasons.add(metric.getReason());
            }
        }
        return reasons;
    }

    private RuntimeOpsInspectionStatus aggregate(List<RuntimeOpsInspectionStatus> statuses) {
        if (statuses.stream().anyMatch(status -> RuntimeOpsInspectionStatus.BLOCKED == status)) {
            return RuntimeOpsInspectionStatus.BLOCKED;
        }
        if (statuses.stream().anyMatch(status -> RuntimeOpsInspectionStatus.NO_GO == status)) {
            return RuntimeOpsInspectionStatus.NO_GO;
        }
        if (statuses.stream().anyMatch(status -> RuntimeOpsInspectionStatus.WARN == status)) {
            return RuntimeOpsInspectionStatus.WARN;
        }
        return RuntimeOpsInspectionStatus.PASS;
    }

    private RuntimeOpsInspectionStatus alertSeverity(RuntimeControlStorageMetricRespVO... metrics) {
        boolean hasWarn = false;
        for (RuntimeControlStorageMetricRespVO metric : metrics) {
            if (metric.getStatus() == RuntimeOpsInspectionStatus.NO_GO) {
                return RuntimeOpsInspectionStatus.NO_GO;
            }
            if (metric.getStatus() == RuntimeOpsInspectionStatus.WARN) {
                hasWarn = true;
            }
        }
        return hasWarn ? RuntimeOpsInspectionStatus.WARN : null;
    }

    private RuntimeControlAlertCreateReqVO alertReq(RuntimeControlCapacityStatusRespVO response,
                                                   RuntimeOpsInspectionStatus alertSeverity) {
        String content = String.join("；", response.getReasons());
        RuntimeControlAlertCreateReqVO reqVO = new RuntimeControlAlertCreateReqVO();
        reqVO.setEnvironment("local");
        reqVO.setAction(ALERT_ACTION);
        reqVO.setSeverity(alertSeverity.name());
        reqVO.setTitle("运行控制台容量阈值告警");
        reqVO.setContent(content);
        reqVO.setNotifyTemplateCode(ALERT_TEMPLATE_CODE);
        reqVO.setTemplateParams(new LinkedHashMap<>(Map.of(
                "environment", "local",
                "action", ALERT_ACTION,
                "severity", alertSeverity.name(),
                "title", "运行控制台容量阈值告警",
                "content", content
        )));
        return reqVO;
    }

    private Path resolveConfiguredPath(String configuredPath) {
        Path path = Path.of(configuredPath);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return Path.of(properties.getRepoRoot()).resolve(path).normalize();
    }

    private String failureReason(Exception ex) {
        return StrUtil.blankToDefault(ex.getMessage(), ex.getClass().getSimpleName());
    }
}
