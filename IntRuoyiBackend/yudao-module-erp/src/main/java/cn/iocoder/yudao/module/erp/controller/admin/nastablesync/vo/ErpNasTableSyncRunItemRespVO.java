package cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - ERP NAS 表格同步运行明细 Response VO")
@Data
public class ErpNasTableSyncRunItemRespVO {

    @Schema(description = "ERP 表类型")
    private String syncType;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "Sheet 名称")
    private String sheetName;

    @Schema(description = "行数")
    private Integer rowCount;

    @Schema(description = "失败信息")
    private String failureMessage;
}
