package cn.iocoder.yudao.module.dcc.controller.admin.signature.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - DCC电子签名记录 Response VO")
@Data
public class DccElectronicSignatureRespVO {

    @Schema(description = "签名记录ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "受控文件ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "900")
    private Long controlledFileId;

    @Schema(description = "受控文件标题", example = "受控文件A")
    private String controlledFileTitle;

    @Schema(description = "文件名称", example = "成品检验规程")
    private String fileName;

    @Schema(description = "文件编号", example = "DCC-001")
    private String controlledFileNumber;

    @Schema(description = "文件编号", example = "DCC-001")
    private String fileNumber;

    @Schema(description = "受控文件状态", example = "PENDING_MATRIX_REVIEW")
    private String controlledFileStatus;

    @Schema(description = "签名修订ID", example = "900")
    private Long revisionId;

    @Schema(description = "版本号", example = "A.1")
    private String versionNo;

    @Schema(description = "BPM任务ID", example = "task-1")
    private String taskId;

    @Schema(description = "签名人用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "99")
    private Long actorId;

    @Schema(description = "签名人用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "99")
    private Long signerUserId;

    @Schema(description = "签名人账号", example = "auditor")
    private String actorUsername;

    @Schema(description = "签名人昵称", example = "审核员")
    private String actorNickname;

    @Schema(description = "签名人名称", example = "审核员")
    private String signerName;

    @Schema(description = "签名人账号快照", example = "auditor")
    private String actorUsernameSnapshot;

    @Schema(description = "签名人昵称快照", example = "审核员")
    private String actorNicknameSnapshot;

    @Schema(description = "签名人部门ID快照", example = "20")
    private Long actorDeptIdSnapshot;

    @Schema(description = "签名人部门名称快照", example = "质量部")
    private String actorDeptNameSnapshot;

    @Schema(description = "签名人岗位名称快照", example = "QA复核员")
    private String actorPostNamesSnapshot;

    @Schema(description = "签名人角色名称快照", example = "质量管理员")
    private String actorRoleNamesSnapshot;

    @Schema(description = "签名目的", example = "会签审核通过")
    private String signaturePurpose;

    @Schema(description = "权限依据", example = "DCC电子签名授权启用")
    private String authorizationBasis;

    @Schema(description = "认证方式", example = "PASSWORD")
    private String authenticationMethod;

    @Schema(description = "记录版本快照", example = "A.1")
    private String recordVersionSnapshot;

    @Schema(description = "记录摘要快照")
    private String recordHashSnapshot;

    @Schema(description = "客户端IP快照")
    private String clientIpSnapshot;

    @Schema(description = "客户端User-Agent快照")
    private String userAgentSnapshot;

    @Schema(description = "快照状态", example = "CAPTURED")
    private String snapshotStatus;

    @Schema(description = "签名动作", requiredMode = Schema.RequiredMode.REQUIRED, example = "APPROVE")
    private String actionType;

    @Schema(description = "签名动作结果", example = "APPROVED")
    private String taskActionResult;

    @Schema(description = "签名含义编码", example = "REVIEW_APPROVE")
    private String meaningCode;

    @Schema(description = "源文件摘要")
    private String sourceFileHash;

    @Schema(description = "源文件摘要短码", example = "0e7b12ca44fe")
    private String sourceFileHashShort;

    @Schema(description = "源文件对象Key", example = "dcc/source/DCC-001-A.1.pdf")
    private String sourceObjectKey;

    @Schema(description = "源文件版本ID", example = "A.1")
    private String sourceVersionId;

    @Schema(description = "受控副本摘要状态", example = "NOT_APPLICABLE")
    private String controlledCopyHashStatus;

    @Schema(description = "受控副本摘要")
    private String controlledCopyHash;

    @Schema(description = "受控副本摘要短码")
    private String controlledCopyHashShort;

    @Schema(description = "受控副本对象Key", example = "dcc/controlled-copy/DCC-001-A.1.pdf")
    private String controlledCopyObjectKey;

    @Schema(description = "受控副本版本ID", example = "A.1")
    private String controlledCopyVersionId;

    @Schema(description = "签名图片ID", example = "9001")
    private Long signatureImageId;

    @Schema(description = "签名图片版本号", example = "2")
    private Integer signatureImageVersionNo;

    @Schema(description = "签名图片文件ID", example = "6001")
    private Long signatureImageFileId;

    @Schema(description = "签名图片文件URL")
    private String signatureImageFileUrl;

    @Schema(description = "签名图片SHA-256")
    private String signatureImageSha256;

    @Schema(description = "签名图片SHA-256短码")
    private String signatureImageSha256Short;

    @Schema(description = "签名图片MIME类型", example = "image/png")
    private String signatureImageContentType;

    @Schema(description = "签名图片文件大小", example = "10240")
    private Long signatureImageFileSize;

    @Schema(description = "签名图片状态快照", example = "ACTIVE")
    private String signatureImageStatusSnapshot;

    @Schema(description = "签名图片校验状态", example = "VALID")
    private String signatureImageVerifiedStatus;

    @Schema(description = "证据载荷版本", example = "v1")
    private String payloadVersion;

    @Schema(description = "证据摘要算法", example = "HMAC_SHA256")
    private String hashAlgorithm;

    @Schema(description = "证据密钥版本", example = "dcc-signature-2026-05")
    private String keyVersion;

    @Schema(description = "证据摘要")
    private String evidenceHash;

    @Schema(description = "证据摘要短码", example = "6f2c91ab03d4")
    private String evidenceHashShort;

    @Schema(description = "证据状态", example = "VALID")
    private String evidenceStatus;

    @Schema(description = "签名方式", requiredMode = Schema.RequiredMode.REQUIRED, example = "PASSWORD")
    private String signatureMode;

    @Schema(description = "密码验签结果", example = "true")
    private Boolean passwordVerified;

    @Schema(description = "签名意见", example = "looks good")
    private String comment;

    @Schema(description = "签名时间", example = "2026-05-16 19:50:00")
    private LocalDateTime signedAt;
}
