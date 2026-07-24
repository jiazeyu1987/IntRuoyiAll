package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlRollbackCandidateRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlRestoreCandidateRespVO;
import cn.iocoder.yudao.module.infra.service.file.NasBrowserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_INVALID;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED;

@Service
public class RuntimeOpsCandidateServiceImpl implements RuntimeOpsCandidateService {

    private static final String STATUS_AVAILABLE = "AVAILABLE";
    private static final String STATUS_BLOCKED = "BLOCKED";
    private static final String ROLLBACK_PREFIX = "rollback:";
    private static final String RESTORE_PREFIX = "restore:";
    private static final int ROLLBACK_CANDIDATE_SCAN_LIMIT = 5;
    private static final int RESTORE_CANDIDATE_SCAN_LIMIT = 5;

    private final RuntimeBackupNasRepository backupRepository;
    private final RuntimeReleasePackageNasRepository releasePackageRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public RuntimeOpsCandidateServiceImpl(RuntimeBackupNasRepository backupRepository,
                                          RuntimeReleasePackageNasRepository releasePackageRepository) {
        this.backupRepository = backupRepository;
        this.releasePackageRepository = releasePackageRepository;
    }

    RuntimeOpsCandidateServiceImpl(cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties properties,
                                   NasBrowserService nasBrowserService) {
        this(new RuntimeBackupNasRepository(properties, nasBrowserService),
                new RuntimeReleasePackageNasRepository(properties, nasBrowserService));
    }

    @Override
    public List<RuntimeControlRollbackCandidateRespVO> listRollbackCandidates() {
        return releasePackageRepository.listReleasePackageDirs().stream()
                .limit(ROLLBACK_CANDIDATE_SCAN_LIMIT)
                .map(this::buildRollbackCandidate)
                .toList();
    }

    @Override
    public List<RuntimeControlRestoreCandidateRespVO> listRestoreCandidates() {
        return backupRepository.listBackupPointDirs().stream()
                .limit(RESTORE_CANDIDATE_SCAN_LIMIT)
                .map(this::buildRestoreCandidate)
                .toList();
    }

    @Override
    public RuntimeControlRollbackCandidateRespVO requireAvailableRollbackCandidate(String candidateId) {
        if (StrUtil.isBlank(candidateId)) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "selectedImageCandidateId");
        }
        RuntimeControlRollbackCandidateRespVO candidate = listRollbackCandidates().stream()
                .filter(item -> candidateId.equals(item.getCandidateId()))
                .findFirst()
                .orElseThrow(() -> exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID,
                        "selectedImageCandidateId 候选不存在：" + candidateId));
        if (!STATUS_AVAILABLE.equals(candidate.getStatus())) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID,
                    "selectedImageCandidateId 候选被阻断：" + String.join("；", candidate.getBlockedReasons()));
        }
        return candidate;
    }

    @Override
    public RuntimeControlRestoreCandidateRespVO requireAvailableRestoreCandidate(String candidateId) {
        if (StrUtil.isBlank(candidateId)) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "selectedRecoverySetCandidateId");
        }
        RuntimeControlRestoreCandidateRespVO candidate = listRestoreCandidates().stream()
                .filter(item -> candidateId.equals(item.getCandidateId()))
                .findFirst()
                .orElseThrow(() -> exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID,
                        "selectedRecoverySetCandidateId 候选不存在：" + candidateId));
        if (!STATUS_AVAILABLE.equals(candidate.getStatus())) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID,
                    "selectedRecoverySetCandidateId 候选被阻断：" + String.join("；", candidate.getBlockedReasons()));
        }
        return candidate;
    }

    private RuntimeControlRollbackCandidateRespVO buildRollbackCandidate(
            RuntimeReleasePackageNasRepository.ReleasePackageDir releasePackageDir) {
        String directoryName = releasePackageDir.directoryName();
        String manifestPath = releasePackageManifestPath(releasePackageDir);
        String prodHistoryPath = releasePackageRepository.childPath(releasePackageDir, "prod-latest.json");
        String compatibilityPath = releasePackageRepository.childPath(releasePackageDir, "rollback-compatibility.json");
        List<String> blockedReasons = new ArrayList<>();
        JsonNode manifest = readReleaseManifest(directoryName, manifestPath, blockedReasons);
        String releaseTag = releaseTag(directoryName, manifest);
        String imageTag = releasePackageImageTag(directoryName, manifest, blockedReasons);
        validateProdHistory(directoryName, prodHistoryPath, blockedReasons);
        RollbackCompatibility compatibility = readRollbackCompatibility(directoryName, compatibilityPath, blockedReasons);

        RuntimeControlRollbackCandidateRespVO candidate = new RuntimeControlRollbackCandidateRespVO();
        candidate.setCandidateId(ROLLBACK_PREFIX + directoryName);
        candidate.setBackupId(directoryName);
        candidate.setReleaseTag(releaseTag);
        candidate.setImageTag(imageTag);
        candidate.setManifestPath(manifestPath);
        candidate.setProdHistoryPath(prodHistoryPath);
        candidate.setCompatibilityEvidencePath(compatibilityPath);
        candidate.setCompatibilityStatus(compatibility.status());
        candidate.setCompatibilityCheckedAt(compatibility.checkedAt());
        candidate.setCompatibilitySummary(compatibility.summary());
        candidate.setBlockedReasons(blockedReasons);
        candidate.setStatus(blockedReasons.isEmpty() ? STATUS_AVAILABLE : STATUS_BLOCKED);
        return candidate;
    }

    private RollbackCompatibility readRollbackCompatibility(String directoryName, String compatibilityPath,
                                                            List<String> blockedReasons) {
        if (!releasePackageRepository.isRegularFile(compatibilityPath)) {
            blockedReasons.add("缺少 rollback-compatibility.json");
            return new RollbackCompatibility("", "", "");
        }
        try {
            JsonNode compatibility = objectMapper.readTree(releasePackageRepository.readText(compatibilityPath));
            String status = text(compatibility, "status");
            if (!"COMPATIBLE".equals(status)) {
                blockedReasons.add("rollback-compatibility.json status 不是 COMPATIBLE");
            }
            String packageDirectoryName = text(compatibility, "packageDirectoryName");
            if (StrUtil.isBlank(packageDirectoryName)) {
                blockedReasons.add("rollback-compatibility.json 缺少 packageDirectoryName");
            } else if (!directoryName.equals(packageDirectoryName)) {
                blockedReasons.add("rollback-compatibility packageDirectoryName 与目录不一致");
            }
            String checkedAt = text(compatibility, "checkedAt");
            if (StrUtil.isBlank(checkedAt)) {
                blockedReasons.add("rollback-compatibility.json 缺少 checkedAt");
            }
            String summary = text(compatibility, "summary");
            if (StrUtil.isBlank(summary)) {
                blockedReasons.add("rollback-compatibility.json 缺少 summary");
            }
            return new RollbackCompatibility(status, checkedAt, summary);
        } catch (ServiceException ex) {
            blockedReasons.add("rollback-compatibility.json 读取失败：" + ex.getMessage());
            return new RollbackCompatibility("", "", "");
        } catch (IOException ex) {
            blockedReasons.add("rollback-compatibility.json 解析失败：" + ex.getMessage());
            return new RollbackCompatibility("", "", "");
        }
    }

    private void validateProdHistory(String directoryName, String prodHistoryPath, List<String> blockedReasons) {
        if (!releasePackageRepository.isRegularFile(prodHistoryPath)) {
            blockedReasons.add("缺少正式服发布历史记录");
            return;
        }
        try {
            JsonNode history = objectMapper.readTree(releasePackageRepository.readText(prodHistoryPath));
            String packageDirectoryName = text(history, "packageDirectoryName");
            if (StrUtil.isBlank(packageDirectoryName)) {
                blockedReasons.add("prod-latest.json 缺少 packageDirectoryName");
            } else if (!directoryName.equals(packageDirectoryName)) {
                blockedReasons.add("prod-latest packageDirectoryName 与目录不一致");
            }
            String environment = text(history, "environment");
            if (StrUtil.isNotBlank(environment) && !"prod".equals(environment)) {
                blockedReasons.add("prod-latest environment 不是 prod");
            }
        } catch (ServiceException ex) {
            blockedReasons.add("正式服发布历史读取失败：" + ex.getMessage());
        } catch (IOException ex) {
            blockedReasons.add("正式服发布历史解析失败：" + ex.getMessage());
        }
    }

    private JsonNode readReleaseManifest(String directoryName, String manifestPath, List<String> blockedReasons) {
        if (!releasePackageRepository.isRegularFile(manifestPath)) {
            blockedReasons.add("缺少 release-manifest.json");
            return null;
        }
        try {
            JsonNode manifest = objectMapper.readTree(releasePackageRepository.readText(manifestPath));
            String packageDirectoryName = text(manifest, "packageDirectoryName");
            if (StrUtil.isNotBlank(packageDirectoryName) && !directoryName.equals(packageDirectoryName)) {
                blockedReasons.add("release-manifest packageDirectoryName 与目录不一致");
            }
            return manifest;
        } catch (ServiceException ex) {
            blockedReasons.add("release-manifest.json 读取失败：" + ex.getMessage());
            return null;
        } catch (IOException ex) {
            blockedReasons.add("release-manifest.json 解析失败：" + ex.getMessage());
            return null;
        }
    }

    private String releaseTag(String directoryName, JsonNode manifest) {
        String releaseTag = text(manifest, "releaseTag");
        if (StrUtil.isNotBlank(releaseTag)) {
            return releaseTag;
        }
        return directoryName;
    }

    private String releasePackageImageTag(String directoryName, JsonNode manifest, List<String> blockedReasons) {
        String packageDirectoryName = text(manifest, "packageDirectoryName");
        if (StrUtil.isBlank(packageDirectoryName)) {
            blockedReasons.add("release-manifest.json 缺少 packageDirectoryName");
            return "";
        }
        if (!directoryName.equals(packageDirectoryName)) {
            return "";
        }
        return packageDirectoryName;
    }

    private RuntimeControlRestoreCandidateRespVO buildRestoreCandidate(RuntimeBackupNasRepository.BackupPointDir backupPointDir) {
        String backupId = backupPointDir.backupId();
        String manifestPath = manifestPath(backupPointDir);
        String checksumPath = backupRepository.childPath(backupPointDir, "manifest", "checksums.txt");
        String dccBackupManifestPath = backupRepository.childPath(backupPointDir, "manifest", "dcc-backup-manifest.json");
        String rehearsalReportPath = backupRepository.childPath(backupPointDir, "manifest", "rehearsal-report.json");
        String snapshotPath = backupRepository.childPath(backupPointDir, "manifest", "现场快照.md");
        List<String> blockedReasons = new ArrayList<>();
        ManifestEvidence manifest = readManifest(backupId, manifestPath, blockedReasons);
        RecoverySet recoverySet = readRecoverySet(backupPointDir, manifest.node(), manifest.sha256(), blockedReasons);
        DccRestoreSummary dccSummary = readDccRestoreSummary(dccBackupManifestPath, blockedReasons);
        String imageTag = resolveImageTag(backupPointDir, manifest.node(), recoverySet.programVersion(), blockedReasons);
        requireRegularFile(checksumPath, "缺少 checksum 清单", blockedReasons);

        RuntimeControlRestoreCandidateRespVO candidate = new RuntimeControlRestoreCandidateRespVO();
        candidate.setCandidateId(RESTORE_PREFIX + backupId);
        candidate.setBackupId(backupId);
        candidate.setImageTag(imageTag);
        candidate.setRecoverySetId(recoverySet.id());
        candidate.setRecoverySetStatus(recoverySet.status());
        candidate.setProgramVersion(recoverySet.programVersion());
        candidate.setRedisPolicy(recoverySet.redisPolicy());
        candidate.setConfigurationManifestPath(recoverySet.configurationManifestPath());
        candidate.setConfigurationComposePath(recoverySet.configurationComposePath());
        candidate.setRecoverySetManifestHash(recoverySet.manifestHash());
        candidate.setComponentSummary(recoverySet.componentSummary());
        candidate.setDccBackupMode(dccSummary.backupMode());
        candidate.setDccChainStatus(dccSummary.chainStatus());
        candidate.setDccChangeSummary(dccSummary.changeSummary());
        candidate.setManifestPath(manifestPath);
        candidate.setChecksumPath(checksumPath);
        candidate.setRehearsalReportPath(rehearsalReportPath);
        candidate.setSnapshotPath(snapshotPath);
        candidate.setBlockedReasons(blockedReasons);
        candidate.setStatus(blockedReasons.isEmpty() ? STATUS_AVAILABLE : STATUS_BLOCKED);
        return candidate;
    }

    private DccRestoreSummary readDccRestoreSummary(String dccBackupManifestPath, List<String> blockedReasons) {
        if (!backupRepository.isRegularFile(dccBackupManifestPath)) {
            blockedReasons.add("缺少 DCC backup manifest");
            return DccRestoreSummary.empty();
        }
        try {
            JsonNode dccManifest = objectMapper.readTree(backupRepository.readText(dccBackupManifestPath));
            String schemaVersion = text(dccManifest, "schemaVersion");
            if (!"dcc-backup-manifest-v1".equals(schemaVersion)) {
                blockedReasons.add("DCC backup manifest schemaVersion 不支持：" + schemaVersion);
            }
            String backupMode = text(dccManifest, "backupMode");
            String chainStatus = text(dccManifest, "chainStatus");
            if (StrUtil.isBlank(backupMode)) {
                blockedReasons.add("DCC backup manifest 缺少 backupMode");
            }
            if (!"COMPLETE".equals(chainStatus)) {
                blockedReasons.add("DCC backup manifest chainStatus 不是 COMPLETE");
            }
            Map<String, String> changeSummary = new LinkedHashMap<>();
            JsonNode summary = dccManifest.get("changeSummary");
            if (summary != null && summary.isObject()) {
                summary.fields().forEachRemaining(entry -> changeSummary.put(entry.getKey(), entry.getValue().asText()));
            }
            if (changeSummary.isEmpty()) {
                blockedReasons.add("DCC backup manifest 缺少 changeSummary");
            }
            return new DccRestoreSummary(backupMode, chainStatus, changeSummary);
        } catch (ServiceException ex) {
            blockedReasons.add("DCC backup manifest 读取失败：" + ex.getMessage());
            return DccRestoreSummary.empty();
        } catch (IOException ex) {
            blockedReasons.add("DCC backup manifest 解析失败：" + ex.getMessage());
            return DccRestoreSummary.empty();
        }
    }

    private ManifestEvidence readManifest(String backupId, String manifestPath, List<String> blockedReasons) {
        if (!backupRepository.isRegularFile(manifestPath)) {
            blockedReasons.add("缺少 manifest.json");
            return new ManifestEvidence(null, "");
        }
        try {
            String manifestText = backupRepository.readText(manifestPath);
            JsonNode manifest = objectMapper.readTree(manifestText);
            String manifestBackupId = text(manifest, "backupId");
            if (StrUtil.isNotBlank(manifestBackupId) && !backupId.equals(manifestBackupId)) {
                blockedReasons.add("manifest backupId 与目录不一致");
            }
            String targetEnvironment = text(manifest, "targetEnvironment");
            String targetHost = text(manifest, "targetHost");
            if (!"test".equals(targetEnvironment) || !"172.30.30.58".equals(targetHost)) {
                blockedReasons.add("manifest targetEnvironment/targetHost 缺少测试服证明，必须为 targetEnvironment=test 且 targetHost=172.30.30.58");
            }
            return new ManifestEvidence(manifest, sha256(manifestText));
        } catch (ServiceException ex) {
            blockedReasons.add("manifest.json 读取失败：" + ex.getMessage());
            return new ManifestEvidence(null, "");
        } catch (IOException ex) {
            blockedReasons.add("manifest.json 解析失败：" + ex.getMessage());
            return new ManifestEvidence(null, "");
        }
    }

    private RecoverySet readRecoverySet(RuntimeBackupNasRepository.BackupPointDir backupPointDir, JsonNode manifest,
                                        String manifestHash, List<String> blockedReasons) {
        JsonNode recoverySet = manifest == null ? null : manifest.get("recoverySet");
        if (recoverySet == null || recoverySet.isNull()) {
            blockedReasons.add("manifest.json 缺少 recoverySet");
            return RecoverySet.empty(manifestHash);
        }
        String id = text(recoverySet, "id");
        if (StrUtil.isBlank(id)) {
            blockedReasons.add("recoverySet 缺少 id");
        } else if (!backupPointDir.backupId().equals(id)) {
            blockedReasons.add("recoverySet.id 与目录不一致");
        }
        String status = text(recoverySet, "status");
        if (!"COMPLETE".equals(status)) {
            blockedReasons.add("recoverySet.status 不是 COMPLETE");
        }
        String programVersion = text(recoverySet.at("/program"), "imageTag");
        if (StrUtil.isBlank(programVersion)) {
            blockedReasons.add("recoverySet.program.imageTag 缺失");
        }
        String mysqlDumpPath = text(recoverySet.at("/mysql"), "dumpPath");
        requireRecoverySetFile(backupPointDir, mysqlDumpPath, "recoverySet.mysql.dumpPath", blockedReasons);
        String minioSnapshotPath = text(recoverySet.at("/minio"), "snapshotPath");
        requireRecoverySetSnapshot(backupPointDir, minioSnapshotPath, "recoverySet.minio.snapshotPath", blockedReasons);
        String businessSnapshotPath = text(recoverySet.at("/businessFiles"), "snapshotPath");
        requireRecoverySetSnapshot(backupPointDir, businessSnapshotPath, "recoverySet.businessFiles.snapshotPath",
                blockedReasons);
        String redisPolicy = text(recoverySet.at("/redis"), "policy");
        if (StrUtil.isBlank(redisPolicy)) {
            blockedReasons.add("recoverySet.redis.policy 缺失");
        }
        String configurationManifestPath = text(recoverySet.at("/configuration"), "manifestPath");
        requireRecoverySetFile(backupPointDir, configurationManifestPath, "recoverySet.configuration.manifestPath",
                blockedReasons);
        String configurationComposePath = text(recoverySet.at("/configuration"), "composePath");
        requireRecoverySetFile(backupPointDir, configurationComposePath, "recoverySet.configuration.composePath",
                blockedReasons);
        String checksumsPath = text(recoverySet.at("/checksums"), "path");
        requireRecoverySetFile(backupPointDir, checksumsPath, "recoverySet.checksums.path", blockedReasons);
        String checksumsHash = text(recoverySet.at("/checksums"), "sha256");
        if (StrUtil.isBlank(checksumsHash)) {
            blockedReasons.add("recoverySet.checksums.sha256 缺失");
        }

        Map<String, String> summary = new LinkedHashMap<>();
        summary.put("mysql", mysqlDumpPath);
        summary.put("minio", minioSnapshotPath);
        summary.put("businessFiles", businessSnapshotPath);
        summary.put("configuration", configurationManifestPath);
        summary.put("configurationCompose", configurationComposePath);
        summary.put("checksums", checksumsPath);
        return new RecoverySet(id, status, programVersion, redisPolicy, configurationManifestPath,
                configurationComposePath, manifestHash, summary);
    }

    private String resolveImageTag(RuntimeBackupNasRepository.BackupPointDir backupPointDir,
                                   JsonNode manifest, String recoverySetProgramVersion, List<String> blockedReasons) {
        String imageTagPath = backupRepository.childPath(backupPointDir, "deploy", "image-tag.txt");
        String fileImageTag = readTrimmed(imageTagPath);
        String manifestImageTag = manifestImageTag(manifest);
        String imageTag = StrUtil.blankToDefault(recoverySetProgramVersion,
                StrUtil.blankToDefault(manifestImageTag, fileImageTag));
        if (StrUtil.isBlank(imageTag)) {
            blockedReasons.add("缺少镜像标签");
        }
        if (StrUtil.isNotBlank(manifestImageTag) && StrUtil.isNotBlank(fileImageTag)
                && !manifestImageTag.equals(fileImageTag)) {
            blockedReasons.add("manifest.deploy.imageTag 与 deploy/image-tag.txt 不一致");
        }
        if (StrUtil.isNotBlank(recoverySetProgramVersion) && StrUtil.isNotBlank(manifestImageTag)
                && !recoverySetProgramVersion.equals(manifestImageTag)) {
            blockedReasons.add("recoverySet.program.imageTag 与 manifest.deploy.imageTag 不一致");
        }
        if (StrUtil.isNotBlank(recoverySetProgramVersion) && StrUtil.isNotBlank(fileImageTag)
                && !recoverySetProgramVersion.equals(fileImageTag)) {
            blockedReasons.add("recoverySet.program.imageTag 与 deploy/image-tag.txt 不一致");
        }
        return imageTag;
    }

    private String manifestImageTag(JsonNode manifest) {
        if (manifest == null) {
            return "";
        }
        String deployImageTag = text(manifest.at("/deploy"), "imageTag");
        if (StrUtil.isNotBlank(deployImageTag)) {
            return deployImageTag;
        }
        return text(manifest, "imageTag");
    }

    private void requireRegularFile(String path, String reason, List<String> blockedReasons) {
        if (!backupRepository.isRegularFile(path)) {
            blockedReasons.add(reason);
        }
    }

    private void requireRecoverySetFile(RuntimeBackupNasRepository.BackupPointDir backupPointDir, String relativePath,
                                        String fieldName, List<String> blockedReasons) {
        if (StrUtil.isBlank(relativePath)) {
            blockedReasons.add(fieldName + " 缺失");
            return;
        }
        if (!backupRepository.isRegularFile(backupRepository.childPath(backupPointDir, relativePath))) {
            blockedReasons.add(fieldName + " 指向的文件不存在：" + relativePath);
        }
    }

    private void requireRecoverySetDirectory(RuntimeBackupNasRepository.BackupPointDir backupPointDir,
                                             String relativePath, String fieldName, List<String> blockedReasons) {
        if (StrUtil.isBlank(relativePath)) {
            blockedReasons.add(fieldName + " 缺失");
            return;
        }
        if (!backupRepository.isDirectory(backupRepository.childPath(backupPointDir, relativePath))) {
            blockedReasons.add(fieldName + " 指向的目录不存在：" + relativePath);
        }
    }

    private void requireRecoverySetSnapshot(RuntimeBackupNasRepository.BackupPointDir backupPointDir,
                                            String relativePath, String fieldName, List<String> blockedReasons) {
        if (StrUtil.isBlank(relativePath)) {
            blockedReasons.add(fieldName + " 缺失");
            return;
        }
        if (!"objects/manifest-object-inventory.json".equals(relativePath)) {
            blockedReasons.add(fieldName + " 必须指向 objects/manifest-object-inventory.json，当前为：" + relativePath);
            return;
        }
        String path = backupRepository.childPath(backupPointDir, relativePath);
        if (!backupRepository.isRegularFile(path)) {
            blockedReasons.add(fieldName + " 指向的对象增量清单不存在：" + relativePath);
        }
    }

    private String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String readTrimmed(String path) {
        if (!backupRepository.isRegularFile(path)) {
            return "";
        }
        try {
            return backupRepository.readText(path).trim();
        } catch (ServiceException ex) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "读取候选文件失败：" + ex.getMessage());
        }
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode field = node == null ? null : node.get(fieldName);
        return field == null || field.isNull() ? "" : field.asText();
    }

    private String manifestPath(RuntimeBackupNasRepository.BackupPointDir backupPointDir) {
        return backupRepository.childPath(backupPointDir, "manifest", "manifest.json");
    }

    private String releasePackageManifestPath(RuntimeReleasePackageNasRepository.ReleasePackageDir releasePackageDir) {
        return releasePackageRepository.childPath(releasePackageDir, "release-manifest.json");
    }

    private record ManifestEvidence(JsonNode node, String sha256) {
    }

    private record RecoverySet(String id, String status, String programVersion, String redisPolicy,
                               String configurationManifestPath, String configurationComposePath, String manifestHash,
                               Map<String, String> componentSummary) {
        private static RecoverySet empty(String manifestHash) {
            return new RecoverySet("", "", "", "", "", "", manifestHash, Map.of());
        }
    }

    private record DccRestoreSummary(String backupMode, String chainStatus, Map<String, String> changeSummary) {
        private static DccRestoreSummary empty() {
            return new DccRestoreSummary("", "", Map.of());
        }
    }

    private record RollbackCompatibility(String status, String checkedAt, String summary) {
    }
}
