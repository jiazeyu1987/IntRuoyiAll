package cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - ERP NAS 表格同步计划明细保存 Request VO")
@Data
public class ErpNasTableSyncPlanItemSaveReqVO {

    @Schema(description = "ERP 表类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "PRODUCT")
    @NotBlank(message = "ERP 表类型不能为空")
    private String syncType;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;

    @Schema(description = "排序", example = "10")
    private Integer sortOrder;

    @Schema(description = "Sheet 名称", example = "产品")
    private String sheetName;
}
