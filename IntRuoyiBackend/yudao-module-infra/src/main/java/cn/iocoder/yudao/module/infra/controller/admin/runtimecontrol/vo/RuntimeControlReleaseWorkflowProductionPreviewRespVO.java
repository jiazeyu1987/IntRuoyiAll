package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class RuntimeControlReleaseWorkflowProductionPreviewRespVO {
    private String previewId;
    private String workflowId;
    private long expectedStateVersion;
    private String releaseTag;
    private String packageDigest;
    private String manifestDigest;
    private String targetEnvironment;
    private String targetDisplayName;
    private String currentProdReleaseTag;
    private String testOperationId;
    private String testedBy;
    private Instant testedAt;
    private String targetFingerprint;
    private boolean eligible;
    private List<RuntimeControlReleaseWorkflowProductionCheckVO> checks;
    private List<String> blockers;
    private Instant createdAt;
    private Instant expiresAt;
}
