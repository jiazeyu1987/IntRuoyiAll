package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlReleaseWorkflowProductionCheckVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlReleaseWorkflowProductionPreviewRespVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
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
    private final Map<String, RuntimeControlReleaseWorkflowProductionPreviewRespVO> previews = new ConcurrentHashMap<>();

    public ReleaseWorkflowProductionPreviewService(RuntimeControlProperties properties) {
        this.properties = properties;
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
            if (prod == null) {
                throw new IllegalStateException("PRODUCTION_TARGET_MISSING");
            }
            Map<String, Object> target = new LinkedHashMap<>();
            target.put("environment", "prod");
            target.put("presetId", properties.getReleaseWorkflow().getPresetId());
            target.put("presetVersion", properties.getReleaseWorkflow().getPresetVersion());
            target.put("writeEnabled", properties.getReleaseWorkflow().isProductionWriteEnabled());
            target.put("accessEnabled", prod.isAccessEnabled());
            target.put("host", prod.getHost());
            target.put("serverUser", prod.getServerUser());
            target.put("remoteAppDir", prod.getRemoteAppDir());
            target.put("remoteReleaseRoot", prod.getRemoteReleaseRoot());
            target.put("remoteDataRoot", prod.getRemoteDataRoot());
            target.put("remoteDataDiskMount", prod.getRemoteDataDiskMount());
            target.put("remoteDataDiskDevice", prod.getRemoteDataDiskDevice());
            target.put("remoteMinioContainer", prod.getRemoteMinioContainer());
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
