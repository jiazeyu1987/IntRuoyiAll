package cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - MES QA 检验规程发布版本 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesQaInspectionRegulationPublishedVersionRespVO {

    private Long dccProjectCodeId;
    private Long regulationId;
    private Long publishedVersionId;
    private String versionNo;
    private LocalDate effectiveDate;
    private LocalDateTime publishedAt;
    private Boolean immutable;
    private String lifecycleStatus;
    private String regulationCode;
    private String regulationName;
    private Boolean finalInspectionApplicable;
    private String finalInspectionNotApplicableReason;
    private List<InspectionTypeRule> inspectionTypeRules;
    private List<InspectionProcess> processes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
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
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InspectionProcess {
        private Long qaProcessId;
        private String processCode;
        private String processName;
        private Integer sort;
        private List<InspectionItem> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
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
