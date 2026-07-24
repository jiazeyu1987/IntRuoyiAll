package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - DCC 受控文件电子签名动作 Response VO")
@Data
public class DccSignatureActionRespVO {

    @Schema(description = "任务动作结果", requiredMode = Schema.RequiredMode.REQUIRED, example = "APPROVED")
    private String taskActionResult;

    @Schema(description = "签名记录 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "8400123")
    private Long signatureId;

    @Schema(description = "受控文件 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "710088")
    private Long controlledFileId;

    @Schema(description = "签名修订 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "710088")
    private Long revisionId;

    @Schema(description = "版本号", requiredMode = Schema.RequiredMode.REQUIRED, example = "A.1")
    private String versionNo;

    @Schema(description = "签名含义编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "REVIEW_APPROVE")
    private String meaningCode;

    @Schema(description = "受控副本摘要状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "NOT_APPLICABLE")
    private String controlledCopyHashStatus;

    @Schema(description = "证据状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "VALID")
    private String evidenceStatus;

    @Schema(description = "证据摘要短码", requiredMode = Schema.RequiredMode.REQUIRED, example = "6f2c91ab03d4")
    private String evidenceHashShort;

    @Schema(description = "签名时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime signedAt;

    @Schema(description = "动作完成后的受控文件状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "PENDING_MATRIX_REVIEW")
    private String nextStatus;
}
