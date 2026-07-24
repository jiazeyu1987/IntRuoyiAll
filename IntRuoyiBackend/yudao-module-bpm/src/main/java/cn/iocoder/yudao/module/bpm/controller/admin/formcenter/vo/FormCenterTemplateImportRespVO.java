package cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo;

import cn.iocoder.yudao.module.bpm.formcenter.model.FormRecognizedField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 表单中心模板导入 Response VO")
@Data
public class FormCenterTemplateImportRespVO {

    @Schema(description = "模板编号")
    private Long templateId;

    @Schema(description = "版本号")
    private String versionNo;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "导入动作：CREATE/UPGRADE")
    private String importAction;

    @Schema(description = "升版来源模板编号")
    private Long sourceTemplateId;

    @Schema(description = "平台业务审批申请编号")
    private Long approvalRequestId;

    @Schema(description = "BPM 审批流程实例编号")
    private String approvalProcessInstanceId;

    @Schema(description = "识别字段")
    private List<FormRecognizedField> recognizedFields;

    @Schema(description = "警告")
    private List<String> warnings;

}
