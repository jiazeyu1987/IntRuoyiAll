package cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Schema(description = "管理后台 - 表单填写规则识别候选")
@Data
public class FormTemplateFillRuleCandidateVO {

    @Schema(description = "行索引，从 0 开始")
    private Integer rowIndex;

    @Schema(description = "列索引，从 0 开始")
    private Integer columnIndex;

    @Schema(description = "字段名称")
    private String label;

    @Schema(description = "值类型")
    private String valueType;

    @Schema(description = "填写控件")
    private String componentFlag;

    @Schema(description = "是否必填")
    private Boolean required;

    @Schema(description = "字段约束")
    private Map<String, Object> constraints;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "占位提示")
    private String placeholder;

    @Schema(description = "帮助提示")
    private String helpText;

    @Schema(description = "识别置信度")
    private Double confidence;

    @Schema(description = "识别理由")
    private String reason;
}
