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

    @Schema(description = "识别字段")
    private List<FormRecognizedField> recognizedFields;

    @Schema(description = "Jimu 调整 JSON")
    private String jimuSchemaJson;

    @Schema(description = "源文件名")
    private String sourceFileName;

    @Schema(description = "绑定批记录报表 ID")
    private String batchRecordReportId;

    @Schema(description = "绑定批记录报表名称")
    private String batchRecordReportName;

    @Schema(description = "绑定批记录名称")
    private String batchRecordName;

    @Schema(description = "绑定批记录版本号")
    private String batchRecordVersionNo;

    @Schema(description = "绑定批记录表单槽位类型")
    private String batchRecordFormSlotType;

    @Schema(description = "绑定状态：BOUND/UNBOUND/BROKEN")
    private String batchRecordBindingStatus;

    @Schema(description = "绑定异常说明")
    private String batchRecordBindingError;

}
