package cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - MES QA 检验规程发布版本 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesQaInspectionRegulationPublishedVersionRespVO {

    @Schema(description = "QA 检验规程 ID")
    private Long regulationId;

    @Schema(description = "QA 检验规程发布版本 ID")
    private Long publishedVersionId;

    @Schema(description = "版本号")
    private String versionNo;

    @Schema(description = "发布时间")
    private LocalDateTime publishedAt;

    @Schema(description = "发布后不可原地修改")
    private Boolean immutable;

    @Schema(description = "规程编码")
    private String regulationCode;

    @Schema(description = "规程名称")
    private String regulationName;

    @Schema(description = "产品 ID")
    private Long productId;

    @Schema(description = "产品名称")
    private String productName;

    @Schema(description = "工艺路线 ID")
    private Long routeId;

    @Schema(description = "工艺路线名称")
    private String routeName;

    @Schema(description = "工艺路线版本 ID")
    private Long routeVersionId;

    @Schema(description = "工艺路线版本号")
    private String routeVersionNo;

    @Schema(description = "工艺路线工序 ID")
    private Long routeProcessId;

    @Schema(description = "工序 ID")
    private Long processId;

    @Schema(description = "工序名称")
    private String routeProcessName;

    @Schema(description = "逐工序批记录绑定摘要")
    private String batchRecordBindingSummary;

    @Schema(description = "首检规则")
    private List<InspectionRule> firstInspectionRules;

    @Schema(description = "巡检规则")
    private List<InspectionRule> patrolInspectionRules;

    @Schema(description = "末检规则")
    private List<InspectionRule> finalInspectionRules;

    @Schema(description = "检验规则")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InspectionRule {

        @Schema(description = "检验类型")
        private String inspectionType;

        @Schema(description = "检验项目编码")
        private String itemCode;

        @Schema(description = "检验项目名称")
        private String itemName;

        @Schema(description = "检验方法")
        private String inspectionMethod;

        @Schema(description = "合格标准")
        private String standardText;

        @Schema(description = "结果类型")
        private String resultType;

        @Schema(description = "首检数量")
        private Integer firstInspectionQuantity;

        @Schema(description = "巡检数量系数")
        private BigDecimal patrolInspectionRatio;
    }
}
