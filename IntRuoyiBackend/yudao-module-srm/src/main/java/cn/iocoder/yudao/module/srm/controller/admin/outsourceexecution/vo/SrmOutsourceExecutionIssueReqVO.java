package cn.iocoder.yudao.module.srm.controller.admin.outsourceexecution.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - SRM 委外发料通知 Request VO")
@Data
public class SrmOutsourceExecutionIssueReqVO {

    @Schema(description = "委外执行单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "委外执行单编号不能为空")
    private Long id;

    @Schema(description = "发料数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "发料数量不能为空")
    private BigDecimal issueQuantity;

    @Schema(description = "发料说明", example = "模拟 PDA 发料")
    private String issueRemark;
}
