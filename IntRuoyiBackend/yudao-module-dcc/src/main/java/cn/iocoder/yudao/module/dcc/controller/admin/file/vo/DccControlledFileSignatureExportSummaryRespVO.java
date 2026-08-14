package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - DCC 受控文件签名导出证据汇总 Response VO")
@Data
public class DccControlledFileSignatureExportSummaryRespVO {

    @Schema(description = "受控文件 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "710088")
    private Long controlledFileId;

    @Schema(description = "签名修订 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "710088")
    private Long revisionId;

    @Schema(description = "版本号", example = "A.1")
    private String versionNo;

    @Schema(description = "所有必需签名证据是否有效", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean allRequiredEvidenceValid;

    @Schema(description = "阻断原因", example = "SIGNATURE_EVIDENCE_INVALID")
    private String blockedReason;

    @Schema(description = "签名证据摘要列表")
    private List<SignatureItem> signatures;

    @Schema(description = "管理后台 - DCC 受控文件签名导出证据项 Response VO")
    @Data
    public static class SignatureItem {

        @Schema(description = "签名记录 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "8400123")
        private Long signatureId;

        @Schema(description = "签名动作结果", example = "APPROVED")
        private String taskActionResult;

        @Schema(description = "签名含义编码", example = "REVIEW_APPROVE")
        private String meaningCode;

        @Schema(description = "受控副本摘要状态", example = "NOT_APPLICABLE")
        private String controlledCopyHashStatus;

        @Schema(description = "受控副本文件 ID", example = "8400999")
        private Long controlledCopyFileId;

        @Schema(description = "受控副本 SHA-256")
        private String controlledCopyHash;

        @Schema(description = "证据状态", example = "VALID")
        private String evidenceStatus;

        @Schema(description = "权威复算失败原因", example = "CONTROLLED_COPY_HASH_MISMATCH")
        private String verificationReason;

        @Schema(description = "受控副本绑定事件键", example = "process-900")
        private String bindingEventKey;

        @Schema(description = "受控副本绑定时间")
        private LocalDateTime boundAt;

        @Schema(description = "证据摘要短码", example = "6f2c91ab03d4")
        private String evidenceHashShort;

        @Schema(description = "签名时间")
        private LocalDateTime signedAt;
    }
}
