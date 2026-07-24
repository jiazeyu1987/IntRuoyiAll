package cn.iocoder.yudao.module.dcc.controller.admin.directory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - DCC 已绑定访问规则目录 Response VO")
@Data
public class DccDirectoryAccessRuleDirectoryRespVO {

    @Schema(description = "目录编号", example = "1")
    private Long id;

    @Schema(description = "目录名称", example = "经营体系管理制度")
    private String name;

    @Schema(description = "目录完整路径", example = "质量管理/1.QMS documents/4 经营体系管理制度")
    private String directoryPath;
}
