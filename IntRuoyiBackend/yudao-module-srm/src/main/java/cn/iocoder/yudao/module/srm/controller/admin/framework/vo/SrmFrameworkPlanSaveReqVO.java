package cn.iocoder.yudao.module.srm.controller.admin.framework.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - SRM 框架计划保存 Request VO")
@Data
public class SrmFrameworkPlanSaveReqVO {

    private Long id;

    @NotBlank(message = "框架计划标题不能为空")
    private String planTitle;

    @NotNull(message = "供应商编号不能为空")
    private Long supplierId;

    @NotBlank(message = "采购方式不能为空")
    private String procurementMethod;

    @NotNull(message = "预算金额不能为空")
    private BigDecimal budgetAmount;

    @NotNull(message = "有效期开始日期不能为空")
    private LocalDate validStartDate;

    @NotNull(message = "有效期结束日期不能为空")
    private LocalDate validEndDate;

    private String remark;

    @Valid
    @NotEmpty(message = "框架计划至少需要一条物料行")
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

        @NotNull(message = "预算金额不能为空")
        private BigDecimal budgetAmount;
    }
}
