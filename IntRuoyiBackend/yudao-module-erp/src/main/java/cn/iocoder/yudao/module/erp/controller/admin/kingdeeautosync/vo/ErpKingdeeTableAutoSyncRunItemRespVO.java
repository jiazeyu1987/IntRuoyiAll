package cn.iocoder.yudao.module.erp.controller.admin.kingdeeautosync.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - ERP 表格自动同步执行明细 Response VO")
@Data
@Accessors(chain = true)
public class ErpKingdeeTableAutoSyncRunItemRespVO {

    @Schema(description = "同步类型")
    private String syncType;

    @Schema(description = "显示名称")
    private String label;

    @Schema(description = "正式 JobHandler")
    private String handlerName;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "执行信息")
    private String message;
}
