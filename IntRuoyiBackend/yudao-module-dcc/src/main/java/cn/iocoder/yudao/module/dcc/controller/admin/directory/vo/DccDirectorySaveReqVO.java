package cn.iocoder.yudao.module.dcc.controller.admin.directory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - DCC 目录新增/修改 Request VO")
@Data
public class DccDirectorySaveReqVO {

    @Schema(description = "目录编号", example = "1")
    private Long id;

    @Schema(description = "父目录编号", example = "100")
    private Long parentId;

    @Schema(description = "目录编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "SOP_LIBRARY")
    @NotBlank(message = "目录编码不能为空")
    private String code;

    @Schema(description = "目录名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "SOP库")
    @NotBlank(message = "目录名称不能为空")
    private String name;

    @Schema(description = "是否启用", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "目录启用状态不能为空")
    private Boolean active;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "目录排序不能为空")
    private Integer sort;

    @Schema(description = "备注", example = "质量体系目录")
    private String remark;
}
