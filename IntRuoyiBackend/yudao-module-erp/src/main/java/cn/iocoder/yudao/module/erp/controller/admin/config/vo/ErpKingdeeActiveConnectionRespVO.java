package cn.iocoder.yudao.module.erp.controller.admin.config.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - ERP 金蝶当前连接 Response VO")
@Data
public class ErpKingdeeActiveConnectionRespVO {

    @Schema(description = "当前连接类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "TEST")
    private String activeConnectionType;

    @Schema(description = "当前连接名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "测试账套")
    private String activeConnectionName;

    @Schema(description = "可选连接", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ErpKingdeeConnectionOptionRespVO> options;

}
