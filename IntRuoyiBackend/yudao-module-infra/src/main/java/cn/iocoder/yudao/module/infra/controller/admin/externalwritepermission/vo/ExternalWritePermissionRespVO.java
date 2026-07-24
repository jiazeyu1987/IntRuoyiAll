package cn.iocoder.yudao.module.infra.controller.admin.externalwritepermission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 外部系统写权限 Response VO")
@Data
public class ExternalWritePermissionRespVO {

    @Schema(description = "是否允许写入", requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
    private Boolean enabled;

}
