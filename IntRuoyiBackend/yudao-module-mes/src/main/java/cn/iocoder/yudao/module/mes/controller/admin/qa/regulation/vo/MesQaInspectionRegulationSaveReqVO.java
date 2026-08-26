package cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - MES QA 检验规程保存/发布 Request VO")
@Data
public class MesQaInspectionRegulationSaveReqVO {

    @Schema(description = "QA 检验规程 ID")
    private Long regulationId;

    @Schema(description = "DCC 项目代码 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "DCC 项目代码不能为空")
    private Long dccProjectCodeId;

    @Schema(description = "规程编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "规程编码不能为空")
    private String regulationCode;

    @Schema(description = "规程名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "规程名称不能为空")
    private String regulationName;

    @Schema(description = "版本号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "版本号不能为空")
    private String versionNo;

    @Schema(description = "生效日期")
    private LocalDate effectiveDate;

    @Schema(description = "末检是否适用", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "末检适用性不能为空")
    private Boolean finalInspectionApplicable;

    @Schema(description = "末检不适用依据")
    private String finalInspectionNotApplicableReason;

    @Schema(description = "检验类型规则", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    private List<InspectionTypeRule> inspectionTypeRules;

    @Schema(description = "QA 工序及检验项目", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    private List<InspectionProcess> processes;

    @Data
    public static class InspectionTypeRule {
        private String key;
        private String inspectionType;
        private String label;
        private String roundLabel;
        private Boolean required;
        private Integer fixedQuantity;
        private String notApplicableReason;
        private String taskRule;
        private String releaseGate;
    }

    @Data
    public static class InspectionProcess {
        private String processCode;
        private String processName;
        private Integer sort;
        @Valid
        private List<InspectionItem> items;
    }

    @Data
    public static class InspectionItem {
        private Integer itemSort;
        private String itemCode;
        private String itemName;
        private String inspectionMethod;
        private String inspectionTool;
        private String samplingPlanText;
        private String standardText;
        private BigDecimal standardLowerLimit;
        private BigDecimal standardUpperLimit;
        private String standardUnit;
        private Integer standardPrecision;
        private String resultType;
        private List<String> applicableInspectionTypes;
        private Integer firstInspectionQuantity;
        private BigDecimal patrolInspectionRatio;
        private Boolean critical;
        private String failureRule;
        private String sourceNote;
        private Integer sourceOriginalPage;
        private String sourceOriginalItem;
        private String sourceOriginalExcerpt;
        private String sourceOriginalMethod;
    }
}
