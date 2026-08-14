package cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - ERP NAS 表格同步计划明细 Response VO")
@Data
@Accessors(chain = true)
public class ErpNasTableSyncPlanItemRespVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "ERP 表类型")
    private String syncType;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "Sheet 名称")
    private String sheetName;
}
