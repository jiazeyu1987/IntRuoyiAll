package cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class DccProjectFileTemplateSaveReqVO {

    @Valid
    @NotEmpty(message = "项目文件模板不能为空")
    @Size(max = 500, message = "单个项目最多配置 500 个文件模板项")
    private List<DccProjectFileTemplateItemSaveReqVO> items;
}
