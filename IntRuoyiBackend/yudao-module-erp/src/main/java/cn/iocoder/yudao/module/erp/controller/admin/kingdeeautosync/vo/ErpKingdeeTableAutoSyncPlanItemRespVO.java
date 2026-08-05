package cn.iocoder.yudao.module.erp.controller.admin.kingdeeautosync.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - ERP 表格自动同步计划明细 Response VO")
@Data
@Accessors(chain = true)
public class ErpKingdeeTableAutoSyncPlanItemRespVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "同步类型")
    private String syncType;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "排序")
    private Integer sortOrder;
}
