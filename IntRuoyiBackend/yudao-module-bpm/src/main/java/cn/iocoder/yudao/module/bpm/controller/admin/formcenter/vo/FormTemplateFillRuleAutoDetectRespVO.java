package cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 表单填写规则自动识别结果")
@Data
public class FormTemplateFillRuleAutoDetectRespVO {

    @Schema(description = "模板编号")
    private Long templateId;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "源版本号")
    private String sourceVersionNo;

    @Schema(description = "版本号")
    private String versionNo;

    @Schema(description = "目标版本状态")
    private String targetStatus;

    @Schema(description = "是否新建草稿版本")
    private Boolean draftCreated;

    @Schema(description = "候选数量")
    private Integer candidateCount;

    @Schema(description = "候选规则")
    private List<FormTemplateFillRuleCandidateVO> candidates;
}
