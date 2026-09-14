package cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - ERP NAS 表格同步立即执行 Response VO")
@Data
@Accessors(chain = true)
public class ErpNasTableSyncRunOnceRespVO {

    @Schema(description = "运行编号")
    private Long runId;

    @Schema(description = "运行状态")
    private String status;

    @Schema(description = "输出路径")
    private String outputPath;

    @Schema(description = "失败信息")
    private String failureMessage;
}
