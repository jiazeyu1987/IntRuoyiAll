package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - eDHR 不合格评审处置 Request VO")
@Data
public class MesProEdhrNonconformanceReviewDisposeReqVO {

    @Schema(description = "评审单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "评审单不能为空")
    private Long id;

    @Schema(description = "处置结论：concession_release/rework/void", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "处置结论不能为空")
    private String disposition;

    @Schema(description = "评审材料URL", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "评审材料不能为空")
    private String reviewMaterialUrl;

    @Schema(description = "评审意见", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "评审意见不能为空")
    private String reviewOpinion;

    @Schema(description = "QA签名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "QA签名不能为空")
    private String qaSignature;
}
