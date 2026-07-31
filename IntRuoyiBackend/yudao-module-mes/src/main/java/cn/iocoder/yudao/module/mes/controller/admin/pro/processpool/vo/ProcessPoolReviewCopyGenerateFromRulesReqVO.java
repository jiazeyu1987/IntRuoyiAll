package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - MES 工序池按正式规则生成审核副本 Request VO")
@Data
public class ProcessPoolReviewCopyGenerateFromRulesReqVO {

    @Schema(description = "来源工序池提交事件ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long eventId;

    @Schema(description = "审核人用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long reviewerUserId;

    @Schema(description = "审核电子签名ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long reviewerSignatureId;

    @Schema(description = "审核电子签名用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long reviewerSignatureUserId;

    @Schema(description = "审核电子签名快照", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String reviewerSignatureSnapshot;
}
