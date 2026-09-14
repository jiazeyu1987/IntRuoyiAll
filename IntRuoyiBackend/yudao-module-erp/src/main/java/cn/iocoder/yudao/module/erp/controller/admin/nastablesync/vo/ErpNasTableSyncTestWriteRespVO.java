package cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - ERP NAS 表格同步测试写入 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErpNasTableSyncTestWriteRespVO {

    @Schema(description = "输出路径")
    private String outputPath;
}
