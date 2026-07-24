package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.importrecord;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - MES 导入报工批次摘要 Response VO")
@Data
public class MesProFeedbackImportBatchSummaryRespVO {

    @Schema(description = "来源文件名")
    private String sourceFileName;

    @Schema(description = "总条数")
    private Integer totalCount;

    @Schema(description = "待归属条数")
    private Integer pendingCount;

    @Schema(description = "已归属条数")
    private Integer attributedCount;

    @Schema(description = "可确认草稿条数")
    private Integer confirmableCount;

    @Schema(description = "跳过的其他订单条数")
    private Integer skippedOtherOrderCount;
}
