package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - MES eDHR 独立表单模板创建 Request VO")
@Data
public class MesProEdhrFormTemplateCreateReqVO {

    @Schema(description = "模板编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "模板编码不能为空")
    private String templateCode;

    @Schema(description = "模板名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    @Schema(description = "模板版本", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "模板版本不能为空")
    private String templateVersion;

    @Schema(description = "字段定义 JSON", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "字段定义不能为空")
    private String fieldSchemaJson;

    @Schema(description = "备注")
    private String remark;
}
