package cn.iocoder.yudao.module.dcc.controller.admin.directory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Schema(description = "管理后台 - DCC 目录访问规则保存 Request VO")
@Data
public class DccDirectoryAccessRuleSaveReqVO {

    @Schema(description = "目录编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "目录编号不能为空")
    private Long directoryId;

    @Schema(description = "主体类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "USER")
    @NotBlank(message = "主体类型不能为空")
    @Pattern(regexp = "USER|DEPT|ROLE|POSITION", message = "主体类型必须是 USER、DEPT、ROLE、POSITION")
    private String subjectType;

    @Schema(description = "主体编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "主体编号不能为空")
    private Long subjectId;

    @Schema(description = "是否可查询", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "查询权限不能为空")
    private Boolean canQuery;

    @Schema(description = "是否可预览", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "预览权限不能为空")
    private Boolean canPreview;

    @Schema(description = "是否可下载", requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
    @NotNull(message = "下载权限不能为空")
    private Boolean canDownload;

    @Schema(description = "是否启用", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "规则启用状态不能为空")
    private Boolean active;

    @Schema(description = "变更原因", example = "目录授权调整")
    private String changeReason;
}
