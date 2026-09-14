package cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DccProjectFileTemplateItemSaveReqVO {

    @NotNull(message = "文件分类不能为空")
    private Long fileTypeTaxonomyId;

    @NotBlank(message = "文件名称不能为空")
    @Size(max = 255, message = "文件名称长度不能超过 255 个字符")
    private String fileName;

    @NotNull(message = "模板排序不能为空")
    @Min(value = 0, message = "模板排序不能小于 0")
    private Integer sortOrder;
}
