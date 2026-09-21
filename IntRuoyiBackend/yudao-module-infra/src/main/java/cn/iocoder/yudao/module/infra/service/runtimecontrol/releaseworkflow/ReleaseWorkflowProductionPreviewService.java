package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlReleaseWorkflowProductionCheckVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlReleaseWorkflowProductionPreviewRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlReleasePackageRespVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeControlService;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ReleaseWorkflowProductionPreviewService {
    private static final Duration PREVIEW_TTL = Duration.ofMinutes(10);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RuntimeControlProperties properties;
    private final RuntimeControlService runtimeControlService;
    private final ReleaseWorkflowTestEvidenceStore testEvidenceStore;
    private final Map<String, RuntimeControlReleaseWorkflowProductionPreviewRespVO> previews = new ConcurrentHashMap<>();

    @Autowired
    public ReleaseWorkflowProductionPreviewService(RuntimeControlProperties properties,
                                                   RuntimeControlService runtimeControlService,
                                                   ReleaseWorkflowTestEvidenceStore testEvidenceStore) {
        this.properties = properties;
        this.runtimeControlService = runtimeControlService;
        this.testEvidenceStore = testEvidenceStore;
    }

    public RuntimeControlReleaseWorkflowProductionPreviewRespVO create(
            ReleaseWorkflowRecord workflow, long expectedStateVersion) {
        if (workflow.stateVersion() != expectedStateVersion) {
            throw new IllegalStateException("PRODUCTION_PREVIEW_STALE");
        }
        Instant now = Instant.now();
        RuntimeControlReleaseWorkflowProductionPreviewRespVO preview = new RuntimeControlReleaseWorkflowProductionPreviewRespVO();
        preview.setPreviewId("prod-preview-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        preview.setWorkflowId(workflow.workflowId());
        preview.setExpectedStateVersion(expectedStateVersion);
        preview.setReleaseTag(workflow.releaseTag());
        preview.setPackageDigest(workflow.packageDigest());
        preview.setManifestDigest(workflow.manifestDigest());
        preview.setTargetEnvironment("prod");
        preview.setTargetDisplayName("正式服务器");
        preview.setTestOperationId(workflow.testOperationId());
        preview.setTargetFingerprint(targetFingerprint());
        preview.setCreatedAt(now);
        preview.setExpiresAt(now.plus(PREVIEW_TTL));
        List<RuntimeControlReleaseWorkflowProductionCheckVO> checks = new ArrayList<>();
        List<String> blockers = new ArrayList<>();
        check(checks, blockers, "WORKFLOW_TESTED", workflow.state() == ReleaseWorkflowRecord.State.TESTED, "工作流必须处于 TESTED");
        check(checks, blockers, "PACKAGE_DIGEST", workflow.packageDigest() != null, "缺少不可变发布包摘要");
        check(checks, blockers, "MANIFEST_DIGEST", workflow.manifestDigest() != null, "缺少 manifest 摘要");
        check(checks, blockers, "TEST_EVIDENCE", workflow.testOperationId() != null && workflow.testOperationEvidencePath() != null, "缺少测试服验收证据");
        check(checks, blockers, "PRODUCTION_WRITE_ENABLED", properties.getReleaseWorkflow().isProductionWriteEnabled(), "正式服写入开关未开启");
        RuntimeControlProperties.Environment prod = properties.getEnvironments().get("prod");
        check(checks, blockers, "PRODUCTION_ACCESS_ENABLED", prod != null && prod.isAccessEnabled(), "正式环境访问未启用");
        check(checks, blockers, "PRODUCTION_TARGET_CONFIGURED", prod != null
                && prod.getHost() != null && !prod.getHost().isBlank()
                && prod.getRemoteAppDir() != null && !prod.getRemoteAppDir().isBlank(), "正式环境目标配置缺失");
        preview.setEvidenceFingerprint(inspectActualEvidence(workflow, checks, blockers));
        preview.setChecks(checks);
        preview.setBlockers(blockers);
        preview.setEligible(blockers.isEmpty());
        previews.put(preview.getPreviewId(), preview);
        return preview;
    }

    public RuntimeControlReleaseWorkflowProductionPreviewRespVO require(String previewId) {
        RuntimeControlReleaseWorkflowProductionPreviewRespVO preview = previews.get(previewId);
        if (preview == null) throw new IllegalStateException("PRODUCTION_PREVIEW_NOT_FOUND");
        if (preview.getExpiresAt().isBefore(Instant.now())) throw new IllegalStateException("PRODUCTION_PREVIEW_STALE");
        return preview;
    }

    public void requireBinding(String previewId, ReleaseWorkflowRecord workflow, long expectedStateVersion) {
        RuntimeControlReleaseWorkflowProductionPreviewRespVO preview = require(previewId);
        if (!preview.isEligible() || !workflow.workflowId().equals(preview.getWorkflowId())
                || workflow.stateVersion() != expectedStateVersion
                || !workflow.releaseTag().equals(preview.getReleaseTag())
                || !safeEquals(workflow.packageDigest(), preview.getPackageDigest())
                || !safeEquals(workflow.manifestDigest(), preview.getManifestDigest())
                || !targetFingerprint().equals(preview.getTargetFingerprint())) {
            throw new IllegalStateException("PRODUCTION_PREVIEW_BINDING_MISMATCH");
        }
        List<RuntimeControlReleaseWorkflowProductionCheckVO> checks = new ArrayList<>();
        List<String> blockers = new ArrayList<>();
        String fingerprint = inspectActualEvidence(workflow, checks, blockers);
        if (!blockers.isEmpty() || !safeEquals(preview.getEvidenceFingerprint(), fingerprint)) {
            throw new IllegalStateException("PRODUCTION_PREVIEW_EVIDENCE_CHANGED: " + String.join("; ", blockers));
        }
    }

    private String inspectActualEvidence(ReleaseWorkflowRecord workflow,
                                         List<RuntimeControlReleaseWorkflowProductionCheckVO> checks,
                                         List<String> blockers) {
        RuntimeControlReleasePackageRespVO releasePackage = null;
        String packageError = "NAS manifest 与工作流包摘要不一致或不可用";
        try {
            releasePackage = runtimeControlService.getReleasePackage(workflow.releaseTag()).orElse(null);
        } catch (ServiceException | IllegalStateException ex) {
            packageError = "NAS 发布包不可读：" + ex.getClass().getSimpleName();
        }
        boolean packageMatches = releasePackage != null
                && "AVAILABLE".equals(releasePackage.getStatus())
                && Boolean.TRUE.equals(releasePackage.getChecksumPresent())
                && "app-release".equals(releasePackage.getPublishScope())
                && workflow.releaseTag().equals(releasePackage.getReleaseTag())
                && workflow.releaseTag().equals(releasePackage.getPackageDirectoryName())
                && workflow.packageDigest() != null && workflow.packageDigest().equals(releasePackage.getPackageDigest())
                && workflow.manifestDigest() != null && workflow.manifestDigest().equals(releasePackage.getManifestDigest());
        check(checks, blockers, "PACKAGE_EVIDENCE", packageMatches, packageError);

        ReleaseWorkflowTestEvidenceStore.VerifiedEvidence operationEvidence = null;
        String operationError = "测试服成功 operation 证明缺失或失效";
        try {
            operationEvidence = testEvidenceStore.verify(workflow);
        } catch (ServiceException ex) {
            operationError = "TEST_OPERATION_EVIDENCE_INVALID";
        } catch (IllegalStateException ex) {
            operationError = ex.getMessage();
        }
        check(checks, blockers, "TEST_OPERATION_EVIDENCE", operationEvidence != null,
                operationError);

        boolean testedMatches = packageMatches && operationEvidence != null
                && Boolean.TRUE.equals(releasePackage.getTested())
                && releasePackage.getTestedDigest() != null
                && "v2".equals(releasePackage.getTestedSchemaVersion())
                && workflow.releaseTag().equals(releasePackage.getTestedReleaseTag())
                && workflow.releaseTag().equals(releasePackage.getTestedPackageDirectoryName())
                && workflow.packageDigest().equals(releasePackage.getTestedPackageDigest())
                && workflow.manifestDigest().equals(releasePackage.getTestedManifestDigest())
                && "test".equals(releasePackage.getTestedEnvironment())
                && workflow.testOperationId().equals(releasePackage.getTestedOperationId())
                && "SUCCESS".equals(releasePackage.getTestedOperationStatus())
                && operationEvidence.requestedAt().equals(releasePackage.getTestedOperationRequestedAt())
                && "PASS".equals(releasePackage.getTestedResult())
                && hasText(releasePackage.getOperatorName())
                && hasText(releasePackage.getTestedConclusion())
                && isUtcInstant(releasePackage.getTestedAt());
        check(checks, blockers, "TESTED_ATTESTATION", testedMatches,
                "tested.json 与工作流、manifest 或测试 operation 不一致");

        ReleaseWorkflowExecutorContract.VerifiedExecutor executor = null;
        String executorError = "维护发布器路径或文件 SHA 与批准值不一致";
        try {
            executor = ReleaseWorkflowExecutorContract.verify(properties);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            executorError = ex.getMessage();
        }
        check(checks, blockers, "RELEASE_EXECUTOR", executor != null,
                executorError);

        if (!packageMatches || !testedMatches || operationEvidence == null || executor == null) {
            return null;
        }
        Map<String, String> identity = new LinkedHashMap<>();
        identity.put("manifestDigest", releasePackage.getManifestDigest());
        identity.put("packageDigest", releasePackage.getPackageDigest());
        identity.put("testedDigest", releasePackage.getTestedDigest());
        identity.put("testOperationEvidenceDigest", operationEvidence.sha256());
        identity.put("executorDigest", executor.sha256());
        try {
            return ReleaseDigestContract.manifestDigest(objectMapper.writeValueAsBytes(identity));
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalStateException("PRODUCTION_EVIDENCE_FINGERPRINT_FAILED", ex);
        }
    }

    private boolean isUtcInstant(String value) {
        try {
            return hasText(value) && Instant.parse(value) != null;
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void check(List<RuntimeControlReleaseWorkflowProductionCheckVO> checks, List<String> blockers,
                       String code, boolean passed, String message) {
        checks.add(new RuntimeControlReleaseWorkflowProductionCheckVO(code, passed ? "PASS" : "BLOCKED", message,
                "workflow/" + code.toLowerCase() + ".json"));
        if (!passed) blockers.add(code + ": " + message);
    }

    private String targetFingerprint() {
        try {
            RuntimeControlProperties.Environment prod = properties.getEnvironments().get("prod");
            Map<String, Object> target = new LinkedHashMap<>();
            target.put("environment", "prod");
            target.put("presetId", properties.getReleaseWorkflow().getPresetId());
            target.put("presetVersion", properties.getReleaseWorkflow().getPresetVersion());
            target.put("writeEnabled", properties.getReleaseWorkflow().isProductionWriteEnabled());
            target.put("accessEnabled", prod != null && prod.isAccessEnabled());
            target.put("host", prod == null ? null : prod.getHost());
            target.put("serverUser", prod == null ? null : prod.getServerUser());
            target.put("remoteAppDir", prod == null ? null : prod.getRemoteAppDir());
            target.put("remoteReleaseRoot", prod == null ? null : prod.getRemoteReleaseRoot());
            target.put("remoteDataRoot", prod == null ? null : prod.getRemoteDataRoot());
            target.put("remoteDataDiskMount", prod == null ? null : prod.getRemoteDataDiskMount());
            target.put("remoteDataDiskDevice", prod == null ? null : prod.getRemoteDataDiskDevice());
            target.put("remoteMinioContainer", prod == null ? null : prod.getRemoteMinioContainer());
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(objectMapper.writeValueAsBytes(target));
            StringBuilder result = new StringBuilder();
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (java.security.NoSuchAlgorithmException | com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalStateException("PRODUCTION_TARGET_FINGERPRINT_FAILED", ex);
        }
    }

    private static boolean safeEquals(String left, String right) { return left == null ? right == null : left.equals(right); }
}
