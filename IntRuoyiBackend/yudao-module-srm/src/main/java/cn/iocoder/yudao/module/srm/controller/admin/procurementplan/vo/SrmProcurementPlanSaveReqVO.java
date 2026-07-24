package cn.iocoder.yudao.module.srm.controller.admin.procurementplan.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - SRM 采购计划保存 Request VO")
@Data
public class SrmProcurementPlanSaveReqVO {

    @Schema(description = "采购计划编号", example = "1")
    private Long id;

    @Schema(description = "计划标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "年度耗材采购")
    @NotBlank(message = "计划标题不能为空")
    private String planTitle;

    @Schema(description = "采购方式", requiredMode = Schema.RequiredMode.REQUIRED, example = "NON_BIDDING")
    @NotBlank(message = "采购方式不能为空")
    private String procurementMethod;

    @Schema(description = "预计金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "1280.50")
    @NotNull(message = "预计金额不能为空")
    private BigDecimal expectedAmount;

    @Schema(description = "备注", example = "测试租户真实计划")
    private String remark;

    @Valid
    @NotEmpty(message = "采购计划至少需要一条行项目")
    private List<Line> lines;

    @Data
    public static class Line {

        @NotNull(message = "物料编号不能为空")
        private Long materialId;

        @NotBlank(message = "物料编码不能为空")
        private String materialCode;

        @NotBlank(message = "物料名称不能为空")
        private String materialName;

        @NotNull(message = "数量不能为空")
        private BigDecimal quantity;

        @NotBlank(message = "单位不能为空")
        private String unit;

        @NotNull(message = "需求日期不能为空")
        private LocalDate requiredDate;
    }
}
