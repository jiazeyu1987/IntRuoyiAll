package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES eDHR 记录本全局开关更新 Request VO")
@Data
@Accessors(chain = true)
public class EdhrRecordbookGlobalSettingUpdateReqVO {

    @Schema(description = "是否启用记录本", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "enabled 不能为空")
    private Boolean enabled;
}
