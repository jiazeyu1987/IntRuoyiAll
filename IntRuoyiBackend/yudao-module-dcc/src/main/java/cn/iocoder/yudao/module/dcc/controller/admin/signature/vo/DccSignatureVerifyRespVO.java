package cn.iocoder.yudao.module.dcc.controller.admin.signature.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - DCC电子签名证据校验 Response VO")
@Data
public class DccSignatureVerifyRespVO {

    @Schema(description = "签名记录 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "8400123")
    private Long signatureId;

    @Schema(description = "存储证据摘要")
    private String storedEvidenceHash;

    @Schema(description = "重算证据摘要")
    private String recomputedEvidenceHash;

    @Schema(description = "证据摘要短码", example = "6f2c91ab03d4")
    private String evidenceHashShort;

    @Schema(description = "校验状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "VALID")
    private String verificationStatus;

    @Schema(description = "校验失败原因", example = "CONTROLLED_COPY_HASH_MISMATCH")
    private String verificationReason;

    @Schema(description = "受控副本摘要状态", example = "BOUND")
    private String controlledCopyHashStatus;

    @Schema(description = "受控副本文件 ID", example = "8400999")
    private Long controlledCopyFileId;

    @Schema(description = "受控副本 SHA-256")
    private String controlledCopyHash;

    @Schema(description = "校验时间")
    private LocalDateTime verifiedAt;
}
