package cn.iocoder.yudao.module.infra.service.backupplan;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.infra.controller.admin.backupplan.vo.BackupPlanStatusRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlBackupPointRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOperationRespVO;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeBackupDrillService;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeBackupNasRepository;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeControlOperationStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class BackupEvidenceExportService {

    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final BackupPlanService backupPlanService;
    private final RuntimeBackupDrillService backupDrillService;
    private final RuntimeBackupNasRepository backupRepository;
    private final RuntimeControlOperationStore operationStore;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT);

    public BackupEvidenceExportService(BackupPlanService backupPlanService,
                                       RuntimeBackupDrillService backupDrillService,
                                       RuntimeBackupNasRepository backupRepository,
                                       RuntimeControlOperationStore operationStore) {
        this.backupPlanService = backupPlanService;
        this.backupDrillService = backupDrillService;
        this.backupRepository = backupRepository;
        this.operationStore = operationStore;
    }

    public ExportResult exportLatest() {
        BackupPlanStatusRespVO status = backupPlanService.getStatus();
        List<RuntimeControlBackupPointRespVO> points = backupDrillService.listBackupPoints();
        RuntimeControlBackupPointRespVO latest = points.isEmpty() ? null : points.get(0);
        List<RuntimeControlBackupPointRespVO> chain = resolveChain(latest, points);
        String manifestText = readOptionalText(latest == null ? null : latest.getManifestPath());
        JsonNode sourceManifest = parseJson(latest == null ? null : latest.getManifestPath(), manifestText);
        String checksumText = readOptionalText(latest == null ? null : latest.getChecksumPath());
        String rehearsalText = readOptionalText(latest == null ? null : latest.getRehearsalReportPath());
        JsonNode sourceRehearsal = parseJson(latest == null ? null : latest.getRehearsalReportPath(), rehearsalText);
        List<ChainEvidenceSnapshot> chainSnapshots = snapshotChain(latest, chain, sourceManifest,
                manifestText, checksumText);
        boolean currentChainEvidenceValid = isCurrentChainEvidenceValid(chain, chainSnapshots);
        String conclusion = isPass(status, latest, chain, sourceRehearsal, currentChainEvidenceValid)
                ? "PASS" : "BLOCKED";
        List<String> blockers = blockers(status, latest, chain, sourceRehearsal, currentChainEvidenceValid);

        LinkedHashMap<String, byte[]> files = new LinkedHashMap<>();
        files.put("审查摘要.txt", summary(status, latest, chain, sourceRehearsal, conclusion, blockers)
                .getBytes(StandardCharsets.UTF_8));
        files.put("backup-plan.json", json(planEvidence(status)));
        files.put("scheduler-status.json", json(schedulerEvidence(status)));
        files.put("backup-chain.json", json(chainEvidence(latest, chain, sourceManifest,
                manifestText, chainSnapshots)));
        files.put("integrity-result.json", json(integrityEvidence(latest, sourceManifest, checksumText)));
        files.put("rehearsal-report.json", json(rehearsalEvidence(latest, sourceRehearsal)));
        files.put("operations.json", json(operationEvidence()));

        byte[] evidenceManifest = json(evidenceManifest(files, conclusion, blockers));
        files.put("evidence-manifest.json", evidenceManifest);
        files.put("evidence-manifest.sha256", (sha256(evidenceManifest) + "\n").getBytes(StandardCharsets.US_ASCII));

        String environment = StrUtil.blankToDefault(status.getRepositoryEnvironment(), "unknown");
        return new ExportResult("IntRuoyi-" + environment + "-backup-evidence-"
                + FILE_TIME.format(LocalDateTime.now()) + ".zip",
                zip(files), conclusion);
    }

    private boolean isPass(BackupPlanStatusRespVO status, RuntimeControlBackupPointRespVO latest,
                           List<RuntimeControlBackupPointRespVO> chain, JsonNode rehearsal,
                           boolean currentChainEvidenceValid) {
        return "正常".equals(status.getHealthStatus())
                && latest != null
                && !chain.isEmpty()
                && currentChainEvidenceValid
                && "RECOVERABLE".equals(latest.getRecoverabilityStatus())
                && currentRehearsalEvidencePassed(rehearsal);
    }

    private List<String> blockers(BackupPlanStatusRespVO status, RuntimeControlBackupPointRespVO latest,
                                  List<RuntimeControlBackupPointRespVO> chain, JsonNode rehearsal,
                                  boolean currentChainEvidenceValid) {
        List<String> blockers = new ArrayList<>();
        if (!"正常".equals(status.getHealthStatus())) {
            blockers.add(StrUtil.blankToDefault(status.getBlockedReason(), "备份计划状态不是正常"));
        }
        if (latest == null) {
            blockers.add("最近成功备份点缺失");
            return blockers;
        }
        if (!"RECOVERABLE".equals(latest.getRecoverabilityStatus())) {
            blockers.addAll(latest.getUnrecoverableReasons() == null
                    ? List.of("最新备份点不可恢复") : latest.getUnrecoverableReasons());
        }
        if (chain.isEmpty()) {
            blockers.add("备份链断裂，无法从目标点回溯到 FULL 基线");
        }
        if (!currentChainEvidenceValid) {
            blockers.add("导出时重新读取的备份链 manifest 或 checksum 已缺失、变化或格式无效");
        }
        if (!currentRehearsalEvidencePassed(rehearsal)) {
            blockers.add("最新备份点恢复演练未通过");
        }
        return blockers.stream().distinct().toList();
    }

    private String summary(BackupPlanStatusRespVO status, RuntimeControlBackupPointRespVO latest,
                           List<RuntimeControlBackupPointRespVO> chain, JsonNode rehearsal,
                           String conclusion, List<String> blockers) {
        StringBuilder text = new StringBuilder();
        text.append("系统名称: IntRuoyi\n");
        text.append("导出时间: ").append(LocalDateTime.now()).append('\n');
        text.append("仓库环境: ").append(value(status.getRepositoryEnvironment())).append('\n');
        text.append("全量计划: ").append(value(status.getFullSchedule())).append('\n');
        text.append("增量计划: ").append(value(status.getIncrementalSchedule())).append('\n');
        text.append("最新备份点: ").append(latest == null ? "无" : latest.getBackupId()).append('\n');
        text.append("最新 chainId: ").append(latest == null ? "无" : value(latest.getBaseBackupId())).append('\n');
        text.append("FULL 基线: ").append(latest == null ? "无" : value(latest.getBaseBackupId())).append('\n');
        text.append("增量段数量: ").append(incrementalCount(chain)).append('\n');
        text.append("最近备份完成时间: ").append(latest == null ? "无" : latest.getCompletedAt()).append('\n');
        text.append("最近演练状态: ").append(latest == null ? "无" : value(latest.getRehearsalStatus())).append('\n');
        text.append("最近演练时间: ").append(rehearsal.path("completedAt").asText("无")).append('\n');
        text.append("完整性结论: ").append("PASS".equals(conclusion) ? "PASS" : "BLOCKED").append('\n');
        text.append("总体结论: ").append(conclusion).append('\n');
        if (!blockers.isEmpty()) {
            text.append("缺失或阻断项:\n");
            blockers.forEach(blocker -> text.append("- ").append(blocker).append('\n'));
        }
        return text.toString();
    }

    private Map<String, Object> planEvidence(BackupPlanStatusRespVO status) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("planStatus", status.getPlanStatus());
        result.put("healthStatus", status.getHealthStatus());
        result.put("fullSchedule", status.getFullSchedule());
        result.put("incrementalSchedule", status.getIncrementalSchedule());
        result.put("repositoryEnvironment", status.getRepositoryEnvironment());
        result.put("maxFreshnessHours", status.getMaxFreshnessHours());
        result.put("blockedReason", status.getBlockedReason());
        return result;
    }

    private Map<String, Object> schedulerEvidence(BackupPlanStatusRespVO status) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nextRunTime", status.getNextRunTime());
        result.put("lastRunTime", status.getLastRunTime());
        result.put("lastResultCode", status.getLastResultCode());
        result.put("healthStatus", status.getHealthStatus());
        return result;
    }

    private Map<String, Object> chainEvidence(RuntimeControlBackupPointRespVO latest,
                                              List<RuntimeControlBackupPointRespVO> chain,
                                              JsonNode manifest, String manifestText,
                                              List<ChainEvidenceSnapshot> chainSnapshots) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("backupId", latest == null ? null : latest.getBackupId());
        result.put("backupKind", text(manifest, "backupKind"));
        result.put("baseBackupId", text(manifest, "baseBackupId"));
        result.put("parentBackupId", text(manifest, "parentBackupId"));
        result.put("chainId", latest == null ? null : latest.getBaseBackupId());
        result.put("targetBackupId", latest == null ? null : latest.getBackupId());
        result.put("incrementalSegmentCount", incrementalCount(chain));
        result.put("manifestSha256", StrUtil.isBlank(manifestText)
                ? null : sha256(manifestText.getBytes(StandardCharsets.UTF_8)));
        result.put("completedAt", latest == null ? null : latest.getCompletedAt());
        result.put("recoverabilityStatus", latest == null ? "MISSING" : latest.getRecoverabilityStatus());
        List<Map<String, Object>> members = new ArrayList<>();
        List<Map<String, Object>> payloads = new ArrayList<>();
        for (ChainEvidenceSnapshot snapshot : chainSnapshots) {
            RuntimeControlBackupPointRespVO point = snapshot.point();
            Map<String, Object> member = new LinkedHashMap<>();
            member.put("backupId", point.getBackupId());
            member.put("backupKind", point.getBackupKind());
            member.put("baseBackupId", point.getBaseBackupId());
            member.put("parentBackupId", point.getParentBackupId());
            member.put("manifestSha256", StrUtil.isBlank(snapshot.manifestText())
                    ? null : sha256(snapshot.manifestText().getBytes(StandardCharsets.UTF_8)));
            members.add(member);
            payloads.addAll(payloadIndex(snapshot.manifest(), snapshot.checksumText()));
        }
        result.put("members", members);
        result.put("payloadIndex", payloads);
        return result;
    }

    private long incrementalCount(List<RuntimeControlBackupPointRespVO> chain) {
        return chain.stream().filter(point -> "INCREMENTAL".equals(point.getBackupKind())).count();
    }

    private boolean isCurrentChainEvidenceValid(List<RuntimeControlBackupPointRespVO> chain,
                                                List<ChainEvidenceSnapshot> snapshots) {
        if (chain.isEmpty() || snapshots.size() != chain.size()) {
            return false;
        }
        for (int index = 0; index < snapshots.size(); index++) {
            ChainEvidenceSnapshot snapshot = snapshots.get(index);
            boolean target = index == snapshots.size() - 1;
            if (!manifestMatchesPoint(snapshot.manifest(), snapshot.point(), target)
                    || !isChecksumInventoryValid(snapshot.checksumText())) {
                return false;
            }
        }
        return true;
    }

    private List<ChainEvidenceSnapshot> snapshotChain(RuntimeControlBackupPointRespVO latest,
                                                      List<RuntimeControlBackupPointRespVO> chain,
                                                      JsonNode latestManifest, String latestManifestText,
                                                      String latestChecksumText) {
        List<RuntimeControlBackupPointRespVO> evidencePoints = chain.isEmpty() && latest != null
                ? List.of(latest) : chain;
        List<ChainEvidenceSnapshot> snapshots = new ArrayList<>();
        for (RuntimeControlBackupPointRespVO point : evidencePoints) {
            boolean target = latest != null && latest.getBackupId().equals(point.getBackupId());
            String currentManifestText = target ? latestManifestText : readOptionalText(point.getManifestPath());
            JsonNode currentManifest = target ? latestManifest
                    : parseJson(point.getManifestPath(), currentManifestText);
            String currentChecksums = target ? latestChecksumText : readOptionalText(point.getChecksumPath());
            snapshots.add(new ChainEvidenceSnapshot(point, currentManifestText, currentManifest, currentChecksums));
        }
        return snapshots;
    }

    private boolean isChecksumInventoryValid(String checksums) {
        List<String> lines = checksums.lines().filter(line -> !line.isBlank()).toList();
        return !lines.isEmpty() && lines.stream().allMatch(line ->
                line.matches("^[0-9a-fA-F]{64}  [^\\r\\n]+$") && checksumPathIsSafe(line.substring(66)));
    }

    private boolean checksumPathIsSafe(String path) {
        String normalized = path.replace('\\', '/');
        return !normalized.startsWith("/") && !normalized.equals("..") && !normalized.contains("../");
    }

    private boolean manifestMatchesPoint(JsonNode manifest, RuntimeControlBackupPointRespVO point, boolean target) {
        if (StrUtil.isBlank(point.getBackupId()) || StrUtil.isBlank(point.getBackupKind())
                || StrUtil.isBlank(point.getBaseBackupId()) || manifest.isMissingNode()
                || !"success".equals(text(manifest, "status"))
                || !point.getBackupId().equals(text(manifest, "backupId"))
                || !point.getBackupKind().equals(text(manifest, "backupKind"))
                || !point.getBaseBackupId().equals(text(manifest, "baseBackupId"))
                || !StrUtil.blankToDefault(point.getParentBackupId(), "")
                .equals(StrUtil.blankToDefault(text(manifest, "parentBackupId"), ""))) {
            return false;
        }
        if (!"test".equals(text(manifest, "repositoryEnvironment"))
                || !"172.30.30.58".equals(text(manifest, "repositoryHost"))) {
            return false;
        }
        String sourceEnvironment = text(manifest, "sourceEnvironment");
        String sourceHost = text(manifest, "sourceHost");
        if (!("production".equals(sourceEnvironment) && "172.30.30.57".equals(sourceHost))
                && !("test".equals(sourceEnvironment) && "172.30.30.58".equals(sourceHost))) {
            return false;
        }
        JsonNode validation = manifest.path("validation");
        if (target && (!"PASSED".equals(text(validation, "rehearsalStatus"))
                || StrUtil.isBlank(text(validation, "lastRehearsedAt")))) {
            return false;
        }
        JsonNode mysqlEvidence = manifest.path("mysqlEvidence");
        boolean mysqlValid;
        if ("FULL".equals(point.getBackupKind())) {
            mysqlValid = "mysql-full-dump-v1".equals(text(mysqlEvidence, "schemaVersion"))
                    && validSha256(text(mysqlEvidence, "sha256"));
        } else {
            mysqlValid = "mysql-binlog-segment-v1".equals(text(mysqlEvidence, "schemaVersion"))
                    && mysqlEvidence.path("segments").isArray() && !mysqlEvidence.path("segments").isEmpty();
            for (JsonNode segment : mysqlEvidence.path("segments")) {
                if (!validSha256(text(segment, "sha256")) || StrUtil.isBlank(text(segment, "path"))) {
                    mysqlValid = false;
                }
            }
        }
        if (!mysqlValid) {
            return false;
        }
        for (JsonNode object : manifest.path("objects")) {
            if (!"deleted".equals(text(object, "status"))
                    && (!validSha256(text(object, "sha256"))
                    || !text(object, "sha256").equals(text(object, "repositoryKey")))) {
                return false;
            }
        }
        return true;
    }

    private boolean validSha256(String value) {
        return value != null && value.matches("^[0-9a-fA-F]{64}$");
    }

    private boolean currentRehearsalEvidencePassed(JsonNode rehearsal) {
        return "PASSED".equalsIgnoreCase(rehearsal.path("status").asText())
                && (StrUtil.isNotBlank(rehearsal.path("completedAt").asText())
                || StrUtil.isNotBlank(rehearsal.path("verifiedAt").asText()));
    }

    private List<RuntimeControlBackupPointRespVO> resolveChain(RuntimeControlBackupPointRespVO latest,
                                                               List<RuntimeControlBackupPointRespVO> points) {
        if (latest == null || StrUtil.isBlank(latest.getBackupId()) || StrUtil.isBlank(latest.getBaseBackupId())) {
            return List.of();
        }
        Map<String, RuntimeControlBackupPointRespVO> byId = new LinkedHashMap<>();
        points.forEach(point -> byId.put(point.getBackupId(), point));
        List<RuntimeControlBackupPointRespVO> reverse = new ArrayList<>();
        List<String> visited = new ArrayList<>();
        RuntimeControlBackupPointRespVO current = latest;
        while (current != null && !visited.contains(current.getBackupId())) {
            visited.add(current.getBackupId());
            reverse.add(current);
            if (latest.getBaseBackupId().equals(current.getBackupId())) {
                if (!"FULL".equals(current.getBackupKind())
                        || !latest.getBaseBackupId().equals(current.getBaseBackupId())) {
                    return List.of();
                }
                Collections.reverse(reverse);
                return reverse;
            }
            if (!"INCREMENTAL".equals(current.getBackupKind())
                    || !latest.getBaseBackupId().equals(current.getBaseBackupId())
                    || StrUtil.isBlank(current.getParentBackupId())) {
                return List.of();
            }
            current = byId.get(current.getParentBackupId());
        }
        return List.of();
    }

    private List<Map<String, Object>> payloadIndex(JsonNode manifest, String checksumText) {
        List<Map<String, Object>> payloads = new ArrayList<>();
        JsonNode mysqlEvidence = manifest.path("mysqlEvidence");
        if (mysqlEvidence.hasNonNull("dumpPath")) {
            payloads.add(payload("mysql", mysqlEvidence.path("dumpPath").asText(),
                    mysqlEvidence.path("size").longValue(), mysqlEvidence.path("sha256").asText(null)));
        }
        for (JsonNode segment : mysqlEvidence.path("segments")) {
            payloads.add(payload("mysql-binlog", segment.path("path").asText(),
                    segment.path("size").longValue(), segment.path("sha256").asText(null)));
        }
        for (JsonNode object : manifest.path("objects")) {
            if (!"deleted".equals(object.path("status").asText())) {
                payloads.add(payload("object", object.path("path").asText(),
                        object.path("size").longValue(), object.path("sha256").asText(null)));
            }
        }
        checksumText.lines().filter(StrUtil::isNotBlank).forEach(line -> {
            String[] parts = line.trim().split("\\s+", 2);
            if (parts.length == 2) {
                payloads.add(payload("configuration", parts[1], null, parts[0]));
            }
        });
        return payloads;
    }

    private Map<String, Object> payload(String type, String path, Long size, String sha256) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", type);
        payload.put("path", path);
        payload.put("size", size);
        payload.put("sha256", sha256);
        return payload;
    }

    private Map<String, Object> integrityEvidence(RuntimeControlBackupPointRespVO latest, JsonNode manifest,
                                                   String checksumText) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", latest == null ? "MISSING" : latest.getRecoverabilityStatus());
        result.put("manifestPresent", !manifest.isMissingNode());
        result.put("checksumPresent", StrUtil.isNotBlank(checksumText));
        result.put("checksumEntryCount", checksumText.lines().filter(StrUtil::isNotBlank).count());
        result.put("checksumInventorySha256", StrUtil.isBlank(checksumText)
                ? null : sha256(checksumText.getBytes(StandardCharsets.UTF_8)));
        result.put("blockedReasons", latest == null ? List.of("最近成功备份点缺失")
                : latest.getUnrecoverableReasons());
        return result;
    }

    private Map<String, Object> rehearsalEvidence(RuntimeControlBackupPointRespVO latest, JsonNode rehearsal) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("backupId", latest == null ? null : latest.getBackupId());
        result.put("status", rehearsal.isMissingNode() ? "MISSING" : rehearsal.path("status").asText("UNKNOWN"));
        result.put("completedAt", rehearsal.path("completedAt").asText(null));
        result.put("checks", rehearsal.path("checks"));
        return result;
    }

    private List<Map<String, Object>> operationEvidence() {
        return operationStore.listLatest(100).stream().map(this::safeOperation).toList();
    }

    private Map<String, Object> safeOperation(RuntimeControlOperationRespVO operation) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("operationId", operation.getOperationId());
        result.put("action", operation.getAction());
        result.put("status", operation.getStatus());
        result.put("environment", operation.getEnvironment());
        result.put("requestedBy", operation.getRequestedBy());
        result.put("requestedAt", operation.getRequestedAt());
        return result;
    }

    private Map<String, Object> evidenceManifest(LinkedHashMap<String, byte[]> files, String conclusion,
                                                  List<String> blockers) {
        List<Map<String, Object>> entries = new ArrayList<>();
        files.forEach((path, content) -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("path", path);
            entry.put("size", content.length);
            entry.put("sha256", sha256(content));
            entries.add(entry);
        });
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("schemaVersion", "backup-evidence-package-v1");
        result.put("system", "IntRuoyi");
        result.put("generatedAt", LocalDateTime.now());
        result.put("conclusion", conclusion);
        result.put("blockers", blockers);
        result.put("files", entries);
        return result;
    }

    private JsonNode parseJson(String path, String content) {
        if (StrUtil.isBlank(path) || StrUtil.isBlank(content)) {
            return objectMapper.missingNode();
        }
        try {
            return objectMapper.readTree(content);
        } catch (Exception ex) {
            return objectMapper.missingNode();
        }
    }

    private String readOptionalText(String path) {
        if (StrUtil.isBlank(path) || !backupRepository.isRegularFile(path)) {
            return "";
        }
        return backupRepository.readText(path);
    }

    private byte[] json(Object value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (Exception ex) {
            throw new IllegalStateException("备份证据 JSON 生成失败", ex);
        }
    }

    private byte[] zip(LinkedHashMap<String, byte[]> files) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, byte[]> entry : files.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
            zip.finish();
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("备份审查证据 ZIP 生成失败", ex);
        }
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode field = node.path(fieldName);
        return field.isMissingNode() || field.isNull() ? null : field.asText();
    }

    private String value(String value) {
        return StrUtil.blankToDefault(value, "未配置");
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public record ExportResult(String filename, byte[] content, String conclusion) {
    }

    private record ChainEvidenceSnapshot(RuntimeControlBackupPointRespVO point, String manifestText,
                                         JsonNode manifest, String checksumText) {
    }
}
