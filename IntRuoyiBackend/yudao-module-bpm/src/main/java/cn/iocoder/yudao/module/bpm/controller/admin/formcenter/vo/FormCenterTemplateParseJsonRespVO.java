package cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo;

import cn.iocoder.yudao.module.bpm.formcenter.model.FormRecognizedField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 表单中心模板解析 JSON Response VO")
@Data
public class FormCenterTemplateParseJsonRespVO {

    @Schema(description = "解析类型编码")
    private String parseType;

    @Schema(description = "解析类型名称")
    private String parseTypeName;

    @Schema(description = "源文件名")
    private String sourceFileName;

    @Schema(description = "识别字段 JSON")
    private String recognizedSchemaJson;

    @Schema(description = "Jimu 表格布局与填写规则 JSON")
    private String jimuSchemaJson;

    @Schema(description = "识别字段")
    private List<FormRecognizedField> recognizedFields;

    @Schema(description = "警告")
    private List<String> warnings;

}
