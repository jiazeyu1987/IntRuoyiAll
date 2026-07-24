package cn.iocoder.yudao.module.infra.controller.admin.externalwritepermission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 外部系统写权限保存 Request VO")
@Data
public class ExternalWritePermissionSaveReqVO {

    @Schema(description = "是否允许写入", requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
    @NotNull(message = "是否允许写入不能为空")
    private Boolean enabled;

}
