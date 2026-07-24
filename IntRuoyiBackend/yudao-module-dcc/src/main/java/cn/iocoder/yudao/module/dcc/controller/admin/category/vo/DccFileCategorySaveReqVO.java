package cn.iocoder.yudao.module.dcc.controller.admin.category.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - DCC 文件类别新增/修改 Request VO")
@Data
public class DccFileCategorySaveReqVO {

    private Long id;
    private Long parentId;

    @NotBlank(message = "类别编码不能为空")
    private String code;

    @NotBlank(message = "类别名称不能为空")
    private String name;

    @NotNull(message = "类别启用状态不能为空")
    private Boolean active;

    @NotNull(message = "类别排序不能为空")
    private Integer sort;

    private String source;
    private String remark;
    private String description;

    private String lifecycleStage;

    @NotNull(message = "默认文件分类不能为空")
    private Long fileTypeTaxonomyId;

    private Boolean distributionRequired;
    private Boolean trainingRequired;
}
