package cn.iocoder.yudao.module.erp.controller.admin.config.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - ERP 写权限 Response VO")
@Data
public class ErpKingdeeExternalWritePermissionRespVO {

    @Schema(description = "是否允许写入外部 ERP", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean enabled;

}
