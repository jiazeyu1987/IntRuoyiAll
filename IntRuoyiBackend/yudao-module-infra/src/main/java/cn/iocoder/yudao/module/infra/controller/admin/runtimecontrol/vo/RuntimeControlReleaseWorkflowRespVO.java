package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow.ReleaseWorkflowRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Schema(description = "管理后台 - 程序发布工作流响应")
@Data
public class RuntimeControlReleaseWorkflowRespVO {

    private String workflowId;
    private String releaseTag;
    private String publishScope;
    private String presetId;
    private String presetVersion;
    private String state;
    private long stateVersion;
    private int attempt;
    private String operationId;
    private String errorCode;
    private String failedStage;
    private boolean retryable;
    private List<String> evidenceRefs;
    private String packageDigest;
    private String manifestDigest;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastHeartbeatAt;
    private String requestedBy;
    private String reason;
    private String sourceSelectionId;

    public static RuntimeControlReleaseWorkflowRespVO from(ReleaseWorkflowRecord record) {
        RuntimeControlReleaseWorkflowRespVO result = new RuntimeControlReleaseWorkflowRespVO();
        result.workflowId = record.workflowId();
        result.releaseTag = record.releaseTag();
        result.publishScope = record.publishScope();
        result.presetId = record.presetId();
        result.presetVersion = record.presetVersion();
        result.state = record.state().name();
        result.stateVersion = record.stateVersion();
        result.attempt = record.attempt();
        result.operationId = record.operationId();
        result.errorCode = record.errorCode();
        result.failedStage = record.failedStage();
        result.retryable = record.retryable();
        result.evidenceRefs = record.evidenceRefs();
        result.packageDigest = record.packageDigest();
        result.manifestDigest = record.manifestDigest();
        result.createdAt = record.createdAt();
        result.updatedAt = record.updatedAt();
        result.lastHeartbeatAt = record.lastHeartbeatAt();
        result.requestedBy = record.requestedBy();
        result.reason = record.reason();
        result.sourceSelectionId = record.sourceSelectionId();
        return result;
    }
}
