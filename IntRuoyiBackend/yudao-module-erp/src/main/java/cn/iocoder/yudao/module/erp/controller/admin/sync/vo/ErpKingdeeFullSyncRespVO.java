package cn.iocoder.yudao.module.erp.controller.admin.sync.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - ERP 金蝶全量同步响应 VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErpKingdeeFullSyncRespVO {

    @Schema(description = "同步类型", example = "PRODUCTION_ORDER")
    private String syncType;

    @Schema(description = "处理器名称", example = "kingdeeProductionOrderSyncJob")
    private String handlerName;

    @Schema(description = "定时任务编号", example = "1024")
    private Long jobId;

    @Schema(description = "执行结果")
    private String message;

}
