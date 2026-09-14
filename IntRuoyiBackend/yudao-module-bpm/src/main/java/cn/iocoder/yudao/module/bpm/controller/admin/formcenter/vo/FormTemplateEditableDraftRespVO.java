package cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 表单模板可编辑草稿版本")
@Data
public class FormTemplateEditableDraftRespVO {

    @Schema(description = "模板编号")
    private Long templateId;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "源版本号")
    private String sourceVersionNo;

    @Schema(description = "可编辑版本号")
    private String versionNo;

    @Schema(description = "目标版本状态")
    private String targetStatus;

    @Schema(description = "是否新建草稿版本")
    private Boolean draftCreated;

}
