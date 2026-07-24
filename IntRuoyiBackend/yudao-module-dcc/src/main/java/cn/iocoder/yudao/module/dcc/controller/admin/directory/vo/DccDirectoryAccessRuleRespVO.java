package cn.iocoder.yudao.module.dcc.controller.admin.directory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - DCC 目录访问规则 Response VO")
@Data
public class DccDirectoryAccessRuleRespVO {

    @Schema(description = "规则编号", example = "1")
    private Long id;
    @Schema(description = "目录编号", example = "1")
    private Long directoryId;
    @Schema(description = "主体类型", example = "USER")
    private String subjectType;
    @Schema(description = "主体编号", example = "100")
    private Long subjectId;
    @Schema(description = "是否可查询", example = "true")
    private Boolean canQuery;
    @Schema(description = "是否可预览", example = "true")
    private Boolean canPreview;
    @Schema(description = "是否可下载", example = "false")
    private Boolean canDownload;
    @Schema(description = "是否启用", example = "true")
    private Boolean active;
    @Schema(description = "变更原因", example = "目录授权调整")
    private String changeReason;
}
