package cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo;

import cn.iocoder.yudao.module.bpm.formcenter.model.FormRecognizedField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 表单中心模板 Response VO")
@Data
public class FormCenterTemplateRespVO {

    @Schema(description = "模板编号")
    private Long templateId;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "版本号")
    private String versionNo;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "修改时间")
    private LocalDateTime updatedTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "批记录设计器报表 ID")
    private String batchRecordReportId;

    @Schema(description = "识别字段")
    private List<FormRecognizedField> recognizedFields;

    @Schema(description = "Jimu 调整 JSON")
    private String jimuSchemaJson;

    @Schema(description = "源文件名")
    private String sourceFileName;

}
