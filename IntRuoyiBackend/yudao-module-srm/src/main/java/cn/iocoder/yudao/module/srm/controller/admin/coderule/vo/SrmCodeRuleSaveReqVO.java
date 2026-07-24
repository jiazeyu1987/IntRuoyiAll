package cn.iocoder.yudao.module.srm.controller.admin.coderule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - SRM 编码规则新增/修改 Request VO")
@Data
public class SrmCodeRuleSaveReqVO {

    @Schema(description = "规则编号", example = "1024")
    private Long id;

    @Schema(description = "规则编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "PLAN_RULE")
    @NotEmpty(message = "规则编码不能为空")
    private String ruleCode;

    @Schema(description = "规则名称", example = "采购计划编码规则")
    private String ruleName;

    @Schema(description = "目标表单", requiredMode = Schema.RequiredMode.REQUIRED, example = "PROCUREMENT_PLAN")
    @NotEmpty(message = "目标表单不能为空")
    private String targetForm;

    @Schema(description = "编码前缀", requiredMode = Schema.RequiredMode.REQUIRED, example = "PL")
    @NotEmpty(message = "编码前缀不能为空")
    private String prefix;

    @Schema(description = "日期格式", example = "yyyyMMdd")
    private String datePattern;

    @Schema(description = "是否启用日期段", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "是否启用日期段不能为空")
    private Boolean dateSegmentEnabled;

    @Schema(description = "流水宽度", requiredMode = Schema.RequiredMode.REQUIRED, example = "4")
    @NotNull(message = "流水宽度不能为空")
    private Integer serialWidth;

    @Schema(description = "流水步长", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "流水步长不能为空")
    private Integer step;

    @Schema(description = "最小流水", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "最小流水不能为空")
    private Long minSerial;

    @Schema(description = "最大流水", requiredMode = Schema.RequiredMode.REQUIRED, example = "9999")
    @NotNull(message = "最大流水不能为空")
    private Long maxSerial;

    @Schema(description = "分隔符", example = "-")
    private String separator;

    @Schema(description = "是否启用", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "是否启用不能为空")
    private Boolean enabled;

    @Schema(description = "备注", example = "D7-1 编码规则")
    private String remark;

}
