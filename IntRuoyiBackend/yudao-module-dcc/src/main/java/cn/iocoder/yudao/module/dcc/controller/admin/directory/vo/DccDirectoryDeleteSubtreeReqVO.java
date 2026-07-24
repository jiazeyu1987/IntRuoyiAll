package cn.iocoder.yudao.module.dcc.controller.admin.directory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - DCC 删除目录子树 Request VO")
@Data
public class DccDirectoryDeleteSubtreeReqVO {

    @Schema(description = "二次确认文本", requiredMode = Schema.RequiredMode.REQUIRED, example = "PROD")
    @NotBlank(message = "confirmText is required")
    private String confirmText;

}
