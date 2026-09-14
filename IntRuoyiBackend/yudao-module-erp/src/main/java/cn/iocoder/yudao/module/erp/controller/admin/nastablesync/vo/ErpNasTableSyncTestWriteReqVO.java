package cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - ERP NAS 表格同步测试写入 Request VO")
@Data
public class ErpNasTableSyncTestWriteReqVO {

    @Schema(description = "NAS 相对目录；为空时使用当前计划目录", example = "ERP/自动同步")
    private String nasDirectory;
}
