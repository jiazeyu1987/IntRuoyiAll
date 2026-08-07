package cn.iocoder.yudao.module.erp.controller.admin.config.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - ERP 金蝶当前连接保存 Request VO")
@Data
public class ErpKingdeeActiveConnectionSaveReqVO {

    @Schema(description = "连接类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "PRODUCTION")
    @NotBlank(message = "ERP 连接类型不能为空")
    private String connectionType;

}
