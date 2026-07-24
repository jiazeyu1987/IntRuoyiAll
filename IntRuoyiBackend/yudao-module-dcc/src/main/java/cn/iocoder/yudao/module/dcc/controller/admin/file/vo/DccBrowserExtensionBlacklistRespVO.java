package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - DCC 文件查阅后缀黑名单 Response VO")
@Data
public class DccBrowserExtensionBlacklistRespVO {

    @Schema(description = "黑名单后缀通配符", example = "[\"*.db\", \"*.pyc\"]")
    private List<String> extensionPatterns;

}
