package cn.iocoder.yudao.module.erp.controller.admin.sync.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 金蝶生产工单新增 Request VO")
@Data
public class ErpKingdeeProductionOrderCreateReqVO {

    @Schema(description = "ERP 生产工单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "SMOKE-MO-001")
    @NotBlank(message = "ERP 生产工单号不能为空")
    private String billNo;

    @Schema(description = "物料编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "MAT-ROUTE-001")
    @NotBlank(message = "物料编码不能为空")
    private String materialNumber;

    @Schema(description = "单位编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "PCS")
    @NotBlank(message = "单位编码不能为空")
    private String unitNumber;

    @Schema(description = "生产数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "12.5")
    @NotNull(message = "生产数量不能为空")
    @Positive(message = "生产数量必须大于 0")
    private BigDecimal quantity;

    @Schema(description = "计划开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "计划开始时间不能为空")
    private LocalDateTime plannedStartDate;

    @Schema(description = "计划完成时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "计划完成时间不能为空")
    private LocalDateTime plannedFinishDate;

    @Schema(description = "来源单号", example = "SMOKE-SO-001")
    private String sourceBillNo;

    @Schema(description = "批次号", requiredMode = Schema.RequiredMode.REQUIRED, example = "BATCH-SMOKE-001")
    @NotBlank(message = "批次号不能为空")
    private String batchNumber;

}
