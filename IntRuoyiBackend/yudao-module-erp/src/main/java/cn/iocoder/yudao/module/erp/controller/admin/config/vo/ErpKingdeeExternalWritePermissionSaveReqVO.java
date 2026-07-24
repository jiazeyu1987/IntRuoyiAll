package cn.iocoder.yudao.module.erp.controller.admin.config.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - ERP 写权限保存 Request VO")
@Data
public class ErpKingdeeExternalWritePermissionSaveReqVO {

    @Schema(description = "是否允许写入外部 ERP", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "ERP写权限开关不能为空")
    private Boolean enabled;

}
