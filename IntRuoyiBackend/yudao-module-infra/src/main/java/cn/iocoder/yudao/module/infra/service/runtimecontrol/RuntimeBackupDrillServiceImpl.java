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
        return backupRepository.listBackupPointDirs().stream()
                .map(this::buildBackupPoint)
                .toList();
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

    private RuntimeControlBackupPointRespVO buildBackupPoint(RuntimeBackupNasRepository.BackupPointDir backupPointDir) {
        String backupId = backupPointDir.backupId();
        String manifestPath = backupRepository.childPath(backupPointDir, "manifest", "manifest.json");
        String checksumPath = backupRepository.childPath(backupPointDir, "manifest", "checksums.txt");
        String rehearsalReportPath = backupRepository.childPath(backupPointDir, "manifest", "rehearsal-report.json");
        String dccManifestPath = backupRepository.childPath(backupPointDir, "manifest", "dcc-backup-manifest.json");
        String snapshotPath = backupRepository.childPath(backupPointDir, "manifest", "现场快照.md");
        List<String> reasons = new ArrayList<>();

        JsonNode manifest = readManifest(backupId, manifestPath, reasons);
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
        backupPoint.setUnrecoverableReasons(reasons);
        backupPoint.setRecoverabilityStatus(reasons.isEmpty() ? STATUS_RECOVERABLE : STATUS_UNRECOVERABLE);
        return backupPoint;
    }

    private JsonNode readManifest(String backupId, String manifestPath, List<String> reasons) {
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
            return manifest;
        } catch (ServiceException ex) {
            reasons.add("manifest.json 读取失败：" + ex.getMessage());
            return null;
        } catch (IOException ex) {
            reasons.add("manifest.json 解析失败：" + ex.getMessage());
            return null;
        }
    }

    private void populateManifestSummary(RuntimeControlBackupPointRespVO backupPoint, JsonNode manifest) {
        if (manifest == null || manifest.isNull()) {
            return;
        }
        String deployImageTag = text(manifest.at("/deploy"), "imageTag");
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
            if (backupRepository.readText(checksumPath).isBlank()) {
                reasons.add("checksum 清单为空");
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
