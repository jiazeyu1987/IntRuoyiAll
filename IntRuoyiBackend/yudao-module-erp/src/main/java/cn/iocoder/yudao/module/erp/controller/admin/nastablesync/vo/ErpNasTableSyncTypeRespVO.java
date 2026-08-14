package cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - ERP NAS 表格同步支持类型 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErpNasTableSyncTypeRespVO {

    @Schema(description = "ERP 表类型")
    private String syncType;

    @Schema(description = "中文名称")
    private String label;

    @Schema(description = "默认 Sheet 名")
    private String defaultSheetName;
}
