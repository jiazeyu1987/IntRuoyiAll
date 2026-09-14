package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlBackupPointRespVO;
import cn.iocoder.yudao.module.infra.service.file.NasBrowserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_INVALID;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED;

@Service
@Slf4j
public class RuntimeBackupDrillServiceImpl implements RuntimeBackupDrillService {

    private static final String STATUS_RECOVERABLE = "RECOVERABLE";
    private static final String STATUS_UNRECOVERABLE = "UNRECOVERABLE";

    private final RuntimeBackupNasRepository backupRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public RuntimeBackupDrillServiceImpl(RuntimeBackupNasRepository backupRepository) {
        this.backupRepository = backupRepository;
    }

    RuntimeBackupDrillServiceImpl(cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties properties,
                                  NasBrowserService nasBrowserService) {
        this(new RuntimeBackupNasRepository(properties, nasBrowserService));
    }

    @Override
    public List<RuntimeControlBackupPointRespVO> listBackupPoints() {
        List<RuntimeBackupNasRepository.BackupPointDir> directories = backupRepository.listBackupPointDirs();
        Set<String> knownBackupIds = directories.stream()
                .map(RuntimeBackupNasRepository.BackupPointDir::backupId)
                .collect(Collectors.toSet());
        List<RuntimeControlBackupPointRespVO> points = directories.stream()
                .map(directory -> buildBackupPoint(directory, knownBackupIds))
                .toList();
        Map<String, RuntimeControlBackupPointRespVO> byId = new LinkedHashMap<>();
        points.forEach(point -> byId.put(point.getBackupId(), point));
        points.forEach(point -> {
            if (!hasCompleteCurrentChainEvidence(point, byId)) {
                List<String> reasons = new ArrayList<>(point.getUnrecoverableReasons());
                reasons.add("FULL/INCREMENTAL 链成员证据缺失或无效");
                point.setUnrecoverableReasons(reasons.stream().distinct().toList());
                point.setRecoverabilityStatus(STATUS_UNRECOVERABLE);
            }
        });
        return points;
    }

    @Override
    public RuntimeControlBackupPointRespVO getBackupPoint(String backupId) {
        if (StrUtil.isBlank(backupId)) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "backupId");
        }
        if (backupId.contains("..") || backupId.contains("/") || backupId.contains("\\")) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "backupId 不合法：" + backupId);
        }
        return listBackupPoints().stream()
                .filter(backupPoint -> backupId.equals(backupPoint.getBackupId()))
                .findFirst()
                .orElseThrow(() -> exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "备份点不存在：" + backupId));
    }

    private RuntimeControlBackupPointRespVO buildBackupPoint(RuntimeBackupNasRepository.BackupPointDir backupPointDir,
                                                              Set<String> knownBackupIds) {
        String backupId = backupPointDir.backupId();
        String manifestPath = backupRepository.childPath(backupPointDir, "manifest", "manifest.json");
        String checksumPath = backupRepository.childPath(backupPointDir, "manifest", "checksums.txt");
        String rehearsalReportPath = backupRepository.childPath(backupPointDir, "manifest", "rehearsal-report.json");
        String dccManifestPath = backupRepository.childPath(backupPointDir, "manifest", "dcc-backup-manifest.json");
        String snapshotPath = backupRepository.childPath(backupPointDir, "manifest", "现场快照.md");
        List<String> reasons = new ArrayList<>();

        JsonNode manifest = readManifest(backupId, manifestPath, knownBackupIds, reasons);
        validateChecksum(checksumPath, reasons);
        LocalDateTime lastVerifiedAt = readOptionalRehearsalVerifiedAt(rehearsalReportPath);
        String rehearsalStatus = readOptionalRehearsalStatus(rehearsalReportPath);

        RuntimeControlBackupPointRespVO backupPoint = new RuntimeControlBackupPointRespVO();
        backupPoint.setBackupId(backupId);
        backupPoint.setManifestPath(manifestPath);
        backupPoint.setChecksumPath(checksumPath);
        backupPoint.setRehearsalReportPath(rehearsalReportPath);
        backupPoint.setSnapshotPath(snapshotPath);
        backupPoint.setLastVerifiedAt(lastVerifiedAt);
        backupPoint.setRehearsalStatus(rehearsalStatus);
        populateManifestSummary(backupPoint, manifest);
        populateDccManifestSummary(backupPoint, dccManifestPath, reasons);
        if (manifest != null && backupPoint.getCompletedAt() == null) {
            reasons.add("manifest completedAt 缺失或非法");
        }
        if (!"PASSED".equals(rehearsalStatus) || lastVerifiedAt == null) {
            reasons.add("恢复演练未通过或证据缺失");
        }
        if (manifest != null) {
            JsonNode validation = manifest.path("validation");
            if (!"PASSED".equals(text(validation, "rehearsalStatus"))
                    || StrUtil.isBlank(text(validation, "lastRehearsedAt"))) {
                reasons.add("manifest 演练状态尚未原子标记为 PASSED");
            }
        }
        backupPoint.setUnrecoverableReasons(reasons);
        backupPoint.setRecoverabilityStatus(reasons.isEmpty() ? STATUS_RECOVERABLE : STATUS_UNRECOVERABLE);
        return backupPoint;
    }

    private JsonNode readManifest(String backupId, String manifestPath, Set<String> knownBackupIds,
                                  List<String> reasons) {
        if (!backupRepository.isRegularFile(manifestPath)) {
            reasons.add("manifest.json 缺失");
            return null;
        }
        try {
            JsonNode manifest = objectMapper.readTree(backupRepository.readText(manifestPath));
            String manifestBackupId = text(manifest, "backupId");
            if (StrUtil.isNotBlank(manifestBackupId) && !backupId.equals(manifestBackupId)) {
                reasons.add("manifest backupId 与目录不一致");
            }
            String targetEnvironment = text(manifest, "targetEnvironment");
            String targetHost = text(manifest, "targetHost");
            if (!"test".equals(targetEnvironment) || !"172.30.30.58".equals(targetHost)) {
                reasons.add("manifest targetEnvironment/targetHost 缺少测试服证明，必须为 targetEnvironment=test 且 targetHost=172.30.30.58");
            }
            if (!"test".equals(text(manifest, "repositoryEnvironment"))
                    || !"172.30.30.58".equals(text(manifest, "repositoryHost"))) {
                reasons.add("manifest repositoryEnvironment/repositoryHost 缺少测试备份仓库证明");
            }
            String sourceEnvironment = text(manifest, "sourceEnvironment");
            String sourceHost = text(manifest, "sourceHost");
            if (!("production".equals(sourceEnvironment) && "172.30.30.57".equals(sourceHost))
                    && !("test".equals(sourceEnvironment) && "172.30.30.58".equals(sourceHost))) {
                reasons.add("manifest sourceEnvironment/sourceHost 备份源证明无效");
            }
            validateChainIdentity(backupId, manifest, knownBackupIds, reasons);
            return manifest;
        } catch (ServiceException ex) {
            reasons.add("manifest.json 读取失败：" + ex.getMessage());
            return null;
        } catch (IOException ex) {
            reasons.add("manifest.json 解析失败：" + ex.getMessage());
            return null;
        }
    }

    private boolean hasCompleteCurrentChainEvidence(RuntimeControlBackupPointRespVO target,
                                                    Map<String, RuntimeControlBackupPointRespVO> byId) {
        if (StrUtil.isBlank(target.getBackupId()) || StrUtil.isBlank(target.getBaseBackupId())) {
            return false;
        }
        String currentId = target.getBackupId();
        List<String> visited = new ArrayList<>();
        while (StrUtil.isNotBlank(currentId) && !visited.contains(currentId)) {
            visited.add(currentId);
            RuntimeControlBackupPointRespVO current = byId.get(currentId);
            if (current == null || hasNonRehearsalBlocker(current)
                    || !target.getBaseBackupId().equals(current.getBaseBackupId())) {
                return false;
            }
            if (target.getBaseBackupId().equals(currentId)) {
                return "FULL".equals(current.getBackupKind()) && StrUtil.isBlank(current.getParentBackupId());
            }
            if (!"INCREMENTAL".equals(current.getBackupKind()) || StrUtil.isBlank(current.getParentBackupId())) {
                return false;
            }
            currentId = current.getParentBackupId();
        }
        return false;
    }

    private boolean hasNonRehearsalBlocker(RuntimeControlBackupPointRespVO point) {
        if (point.getUnrecoverableReasons() == null) {
            return false;
        }
        return point.getUnrecoverableReasons().stream().anyMatch(reason -> !reason.contains("演练"));
    }

    private void validateChainIdentity(String backupId, JsonNode manifest, Set<String> knownBackupIds,
                                       List<String> reasons) {
        String backupKind = text(manifest, "backupKind");
        String baseBackupId = text(manifest, "baseBackupId");
        String parentBackupId = text(manifest, "parentBackupId");
        String mysqlEvidenceSchema = text(manifest.path("mysqlEvidence"), "schemaVersion");
        if ("FULL".equals(backupKind)) {
            if (!backupId.equals(baseBackupId) || StrUtil.isNotBlank(parentBackupId)) {
                reasons.add("FULL manifest baseBackupId/parentBackupId 链身份无效");
            }
            if (!"mysql-full-dump-v1".equals(mysqlEvidenceSchema)) {
                reasons.add("FULL manifest MySQL 全量证据缺失或类型错误");
            }
            return;
        }
        if ("INCREMENTAL".equals(backupKind)) {
            if (StrUtil.isBlank(baseBackupId) || !knownBackupIds.contains(baseBackupId)) {
                reasons.add("INCREMENTAL manifest baseBackupId 缺失或基线不存在");
            }
            if (StrUtil.isBlank(parentBackupId) || !knownBackupIds.contains(parentBackupId)) {
                reasons.add("INCREMENTAL manifest parentBackupId 缺失或父点不存在");
            }
            if (!"mysql-binlog-segment-v1".equals(mysqlEvidenceSchema)) {
                reasons.add("INCREMENTAL manifest MySQL binlog 证据缺失或类型错误");
            }
            return;
        }
        reasons.add("manifest backupKind 必须为 FULL 或 INCREMENTAL");
    }

    private void populateManifestSummary(RuntimeControlBackupPointRespVO backupPoint, JsonNode manifest) {
        if (manifest == null || manifest.isNull()) {
            return;
        }
        String deployImageTag = text(manifest.at("/deploy"), "imageTag");
        backupPoint.setBackupKind(text(manifest, "backupKind"));
        backupPoint.setBaseBackupId(text(manifest, "baseBackupId"));
        backupPoint.setParentBackupId(StrUtil.emptyToNull(text(manifest, "parentBackupId")));
        backupPoint.setImageTag(StrUtil.blankToDefault(deployImageTag, text(manifest, "imageTag")));
        backupPoint.setCompletedAt(parseManifestCompletedAt(manifest));
        backupPoint.setBackupMode(text(manifest.at("/backupStrategy"), "mode"));
        JsonNode retentionPolicy = manifest.at("/retentionPolicy");
        backupPoint.setRetentionKeepLast(integer(retentionPolicy, "keepLast"));
        backupPoint.setRetentionKeepDays(integer(retentionPolicy, "keepDays"));
        backupPoint.setRetentionMaxNasUsedPercent(integer(retentionPolicy, "maxNasUsedPercent"));
        JsonNode objectDeltaStats = manifest.at("/objectDeltaStats");
        backupPoint.setObjectAddedCount(integer(objectDeltaStats, "addedCount"));
        backupPoint.setObjectModifiedCount(integer(objectDeltaStats, "modifiedCount"));
        backupPoint.setObjectDeletedCount(integer(objectDeltaStats, "deletedCount"));
        backupPoint.setObjectReusedCount(integer(objectDeltaStats, "reusedCount"));
    }

    private LocalDateTime parseManifestCompletedAt(JsonNode manifest) {
        String value = firstText(manifest, List.of("completedAt", "finishedAt"));
        if (StrUtil.isBlank(value)) {
            value = firstText(manifest.at("/time"), List.of("completedAt", "finishedAt"));
        }
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private void populateDccManifestSummary(RuntimeControlBackupPointRespVO backupPoint, String dccManifestPath,
                                            List<String> reasons) {
        if (!backupRepository.isRegularFile(dccManifestPath)) {
            reasons.add("DCC backup manifest 缺失");
            return;
        }
        try {
            JsonNode dccManifest = objectMapper.readTree(backupRepository.readText(dccManifestPath));
            String backupMode = text(dccManifest, "backupMode");
            String chainStatus = text(dccManifest, "chainStatus");
            backupPoint.setDccBackupMode(backupMode);
            backupPoint.setDccChainStatus(chainStatus);
            backupPoint.setDccChangeSummary(stringMap(dccManifest.at("/changeSummary")));
            if (StrUtil.isBlank(backupMode)) {
                reasons.add("DCC backup manifest 缺少 backupMode");
            }
            if (!"COMPLETE".equals(chainStatus)) {
                reasons.add("DCC backup manifest chainStatus 不是 COMPLETE");
            }
        } catch (ServiceException ex) {
            reasons.add("DCC backup manifest 读取失败：" + ex.getMessage());
        } catch (IOException ex) {
            reasons.add("DCC backup manifest 解析失败：" + ex.getMessage());
        }
    }

    private void validateChecksum(String checksumPath, List<String> reasons) {
        if (!backupRepository.isRegularFile(checksumPath)) {
            reasons.add("checksum 清单缺失");
            return;
        }
        try {
            String checksumText = backupRepository.readText(checksumPath);
            if (checksumText.isBlank()) {
                reasons.add("checksum 清单为空");
                return;
            }
            boolean malformed = checksumText.lines().filter(line -> !line.isBlank()).anyMatch(line -> {
                if (!line.matches("^[0-9a-fA-F]{64}  [^\\r\\n]+$")) {
                    return true;
                }
                String relativePath = line.substring(66).replace('\\', '/');
                return relativePath.startsWith("/") || relativePath.contains("../") || relativePath.equals("..");
            });
            if (malformed) {
                reasons.add("checksum 清单格式或相对路径非法");
            }
        } catch (ServiceException ex) {
            reasons.add("checksum 清单读取失败：" + ex.getMessage());
        }
    }

    private LocalDateTime readOptionalRehearsalVerifiedAt(String rehearsalReportPath) {
        if (!backupRepository.isRegularFile(rehearsalReportPath)) {
            return null;
        }
        try {
            JsonNode report = objectMapper.readTree(backupRepository.readText(rehearsalReportPath));
            return parseVerifiedAt(report);
        } catch (ServiceException | IOException ex) {
            log.warn("Optional restore rehearsal report could not be read: {}", rehearsalReportPath, ex);
            return null;
        }
    }

    private String readOptionalRehearsalStatus(String rehearsalReportPath) {
        if (!backupRepository.isRegularFile(rehearsalReportPath)) {
            return "not-run";
        }
        try {
            JsonNode report = objectMapper.readTree(backupRepository.readText(rehearsalReportPath));
            return StrUtil.blankToDefault(text(report, "status"), "unknown");
        } catch (ServiceException | IOException ex) {
            log.warn("Optional restore rehearsal report status could not be read: {}", rehearsalReportPath, ex);
            return "unreadable";
        }
    }

    private LocalDateTime parseVerifiedAt(JsonNode report) {
        String value = firstText(report, List.of("verifiedAt", "completedAt", "finishedAt", "sampledAt"));
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ex) {
            log.warn("Optional restore rehearsal report verified time could not be parsed: {}", value, ex);
            return null;
        }
    }

    private String firstText(JsonNode node, List<String> fieldNames) {
        for (String fieldName : fieldNames) {
            String value = text(node, fieldName);
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode field = node == null ? null : node.get(fieldName);
        return field == null || field.isNull() ? "" : field.asText();
    }

    private Integer integer(JsonNode node, String fieldName) {
        JsonNode field = node == null ? null : node.get(fieldName);
        return field == null || field.isNull() || !field.canConvertToInt() ? null : field.asInt();
    }

    private Map<String, String> stringMap(JsonNode node) {
        Map<String, String> result = new LinkedHashMap<>();
        if (node == null || node.isMissingNode() || !node.isObject()) {
            return result;
        }
        node.fields().forEachRemaining(entry -> result.put(entry.getKey(), entry.getValue().asText()));
        return result;
    }
}
