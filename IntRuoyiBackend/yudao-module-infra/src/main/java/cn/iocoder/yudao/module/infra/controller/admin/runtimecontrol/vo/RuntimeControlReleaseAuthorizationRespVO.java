package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow.ReleaseAuthorizationGrant;
import lombok.Data;

import java.time.Instant;

@Data
public class RuntimeControlReleaseAuthorizationRespVO {
    private String grantId;
    private String workflowId;
    private String releaseTag;
    private String packageDigest;
    private String manifestDigest;
    private String targetEnvironment;
    private String presetId;
    private String presetVersion;
    private String approvedScope;
    private String approver;
    private Instant issuedAt;
    private Instant validUntil;

    public static RuntimeControlReleaseAuthorizationRespVO from(ReleaseAuthorizationGrant grant) {
        RuntimeControlReleaseAuthorizationRespVO result = new RuntimeControlReleaseAuthorizationRespVO();
        result.grantId = grant.grantId();
        result.workflowId = grant.workflowId();
        result.releaseTag = grant.releaseTag();
        result.packageDigest = grant.packageDigest();
        result.manifestDigest = grant.manifestDigest();
        result.targetEnvironment = grant.targetEnvironment();
        result.presetId = grant.presetId();
        result.presetVersion = grant.presetVersion();
        result.approvedScope = grant.approvedScope();
        result.approver = grant.approver();
        result.issuedAt = grant.issuedAt();
        result.validUntil = grant.validUntil();
        return result;
    }
}
