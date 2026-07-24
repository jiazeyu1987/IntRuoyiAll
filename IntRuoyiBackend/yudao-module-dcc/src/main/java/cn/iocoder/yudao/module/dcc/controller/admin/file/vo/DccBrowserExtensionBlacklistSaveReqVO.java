package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - DCC 文件查阅后缀黑名单保存 Request VO")
@Data
public class DccBrowserExtensionBlacklistSaveReqVO {

    @Schema(description = "黑名单后缀通配符", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "[\"*.db\", \"*.pyc\"]")
    @NotNull(message = "黑名单后缀不能为空")
    @Size(max = 50, message = "黑名单后缀最多配置 50 项")
    private List<String> extensionPatterns;

}
