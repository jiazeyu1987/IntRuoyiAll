package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - MES 一线 PQC QA 工序 Response VO")
@Data
public class MesFrontlinePqcProcessRespVO {

    @Schema(description = "工艺路线编号，仅用于追溯订单所属 DCC 项目")
    private Long routeId;
    @Schema(description = "工艺路线编码")
    private String routeCode;
    @Schema(description = "工艺路线名称")
    private String routeName;
    @Schema(description = "DCC 项目代码编号")
    private Long dccProjectCodeId;
    @Schema(description = "QA 规程根编号")
    private Long regulationId;
    @Schema(description = "QA 规程发布版本编号")
    private Long regulationVersionId;
    @Schema(description = "QA 工序编号")
    private Long qaProcessId;
    @Schema(description = "QA 工序编码")
    private String qaProcessCode;
    @Schema(description = "QA 工序名称")
    private String qaProcessName;
    @Schema(description = "QA 工序排序")
    private Integer qaProcessSort;
    @Schema(description = "活跃订单编号")
    private Long activeOrderId;
    @Schema(description = "当前 PQC 检验任务编号")
    private Long pqcTaskId;
    @Schema(description = "发布态 QA 规程是否启用末检")
    private Boolean finalInspectionApplicable;
    @Schema(description = "当前检验类型")
    private String inspectionType;
    @Schema(description = "业务日期")
    private LocalDate businessDate;
    @Schema(description = "班次编码")
    private String shiftCode;
    @Schema(description = "轮次")
    private Integer roundNo;
    @Schema(description = "计划检验数量")
    private Integer plannedInspectionQuantity;
    @Schema(description = "当前任务的 QA 检验项目")
    private List<PqcInspectionItem> inspectionItems;
    @Schema(description = "当前 QA 工序待检任务选项")
    private List<PqcTaskOption> pqcTaskOptions;

    @Data
    public static class PqcTaskOption {
        private Long pqcTaskId;
        private Long regulationVersionId;
        private Long qaProcessId;
        private Boolean finalInspectionApplicable;
        private String inspectionType;
        private LocalDate businessDate;
        private String shiftCode;
        private Integer roundNo;
        private Integer plannedInspectionQuantity;
        private List<PqcInspectionItem> inspectionItems;
    }

    @Data
    public static class PqcInspectionItem {
        private String itemCode;
        private String itemName;
        private String inspectionMethod;
        private String standardText;
        private String acceptanceStandard;
        private String processInspectionMethod;
        private String inspectionTool;
        private String samplingPlanText;
        private BigDecimal standardLowerLimit;
        private BigDecimal standardUpperLimit;
        private String standardUnit;
        private Integer standardPrecision;
        private Boolean equipmentRequired;
        private String resultType;
        private List<PqcEquipmentOption> equipmentOptions;
    }

    @Data
    public static class PqcEquipmentOption {
        private Long equipmentId;
        private String equipmentCode;
        private String equipmentName;
        private String equipmentNumber;
        private Boolean defaultFlag;
        private Integer sort;
    }
}
