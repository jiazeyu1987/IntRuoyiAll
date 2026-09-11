package cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - 表单解析 QA 检验规程 JSON Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesQaInspectionRegulationParseRespVO {

    private Integer schemaVersion;
    private String sourceFileName;
    private String regulationCode;
    private String regulationName;
    private String versionNo;
    private String effectiveDate;
    private List<InspectionProcess> processes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InspectionProcess {
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
        private String resultType;
        private List<String> applicableInspectionTypes;
        private Integer firstInspectionQuantity;
        private BigDecimal patrolInspectionRatio;
    }
}
