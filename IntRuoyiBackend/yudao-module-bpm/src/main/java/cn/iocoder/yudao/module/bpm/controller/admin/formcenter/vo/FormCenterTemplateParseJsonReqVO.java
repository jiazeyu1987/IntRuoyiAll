package cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Schema(description = "管理后台 - 表单中心模板解析 JSON Request VO")
@Data
public class FormCenterTemplateParseJsonReqVO {

    @Schema(description = "doc/docx 表单文件", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "表单文件不能为空")
    private MultipartFile file;

}
