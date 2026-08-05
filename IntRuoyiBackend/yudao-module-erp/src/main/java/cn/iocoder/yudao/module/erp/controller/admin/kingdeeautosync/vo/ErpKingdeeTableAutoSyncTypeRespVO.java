package cn.iocoder.yudao.module.erp.controller.admin.kingdeeautosync.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - ERP 表格自动同步类型 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErpKingdeeTableAutoSyncTypeRespVO {

    @Schema(description = "同步类型")
    private String syncType;

    @Schema(description = "显示名称")
    private String label;

    @Schema(description = "正式 JobHandler")
    private String handlerName;
}
