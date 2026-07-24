package cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 表单中心 Jimu 模板调整 Request VO")
@Data
public class FormCenterTemplateJimuSchemaReqVO {

    @Schema(description = "Jimu schema JSON", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Jimu schema 不能为空")
    private String jimuSchema;

}
