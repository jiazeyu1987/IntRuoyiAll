package cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Schema(description = "管理后台 - 表单中心模板导入 Request VO")
@Data
public class FormCenterTemplateImportReqVO {

    @Schema(description = "模板名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    @Schema(description = "从下拉模板池选择的稳定模板编号；为空时按模板名称自动识别新建或升版")
    private Long selectedTemplateId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "doc/docx 模板文件", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "模板文件不能为空")
    private MultipartFile file;

}
