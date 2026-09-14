package cn.iocoder.yudao.module.erp.controller.admin.config.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - ERP 金蝶连接选项 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErpKingdeeConnectionOptionRespVO {

    @Schema(description = "连接类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "TEST")
    private String connectionType;

    @Schema(description = "连接名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "测试账套")
    private String connectionName;

}
