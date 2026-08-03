package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Admin - DCC NAS control audit recognize response VO")
@Data
public class DccNasControlAuditRecognizeRespVO {

    @Schema(description = "Matched file count")
    private Long matchedCount;

    @Schema(description = "Unclassified pending file count")
    private Long unclassifiedPendingCount;

    @Schema(description = "Ambiguous file count")
    private Long ambiguousCount;

    @Schema(description = "Skipped file count")
    private Long skippedCount;
}