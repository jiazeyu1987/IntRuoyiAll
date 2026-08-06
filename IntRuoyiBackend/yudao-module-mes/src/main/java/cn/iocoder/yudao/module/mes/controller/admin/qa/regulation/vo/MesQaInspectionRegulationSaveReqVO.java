package cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - MES QA 检验规程保存/发布 Request VO")
@Data
public class MesQaInspectionRegulationSaveReqVO {

    @Schema(description = "QA 检验规程 ID；为空时按产品+路线版本+工序定位")
    private Long regulationId;

    @Schema(description = "产品 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "产品不能为空")
    private Long productId;

    @Schema(description = "产品名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "产品名称不能为空")
    private String productName;

    @Schema(description = "工艺路线 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "工艺路线不能为空")
    private Long routeId;

    @Schema(description = "工艺路线名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "工艺路线名称不能为空")
    private String routeName;

    @Schema(description = "工艺路线版本 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "工艺路线版本不能为空")
    private Long routeVersionId;

    @Schema(description = "工艺路线版本号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "工艺路线版本号不能为空")
    private String routeVersionNo;

    @Schema(description = "路线工序 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "路线工序不能为空")
    private Long routeProcessId;

    @Schema(description = "工序 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "工序不能为空")
    private Long processId;

    @Schema(description = "工序名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "工序名称不能为空")
    private String routeProcessName;

    @Schema(description = "逐工序批记录绑定摘要")
    private String batchRecordBindingSummary;

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

    @Schema(description = "末检是否适用；必须显式配置，不能由 FINAL 项目缺失反推", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "末检适用性必须显式配置")
    private Boolean finalInspectionApplicable;

    @Schema(description = "末检不适用依据；finalInspectionApplicable=false 时必填")
    private String finalInspectionNotApplicableReason;

    @Schema(description = "检验项目", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotEmpty(message = "检验项目不能为空")
    private List<InspectionItem> items;

    @Schema(description = "管理后台 - MES QA 检验规程检验项目")
    @Data
    public static class InspectionItem {

        @Schema(description = "检验类型：FIRST/PATROL/FINAL", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "检验类型不能为空")
        private String inspectionType;

        @Schema(description = "检验项目编码", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "检验项目编码不能为空")
        private String itemCode;

        @Schema(description = "检验项目名称", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "检验项目名称不能为空")
        private String itemName;

        @Schema(description = "检验方法", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "检验方法不能为空")
        private String inspectionMethod;

        @Schema(description = "合格标准", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "合格标准不能为空")
        private String standardText;

        @Schema(description = "标准下限")
        private BigDecimal standardLowerLimit;

        @Schema(description = "标准上限")
        private BigDecimal standardUpperLimit;

        @Schema(description = "标准单位")
        private String standardUnit;

        @Schema(description = "标准精度")
        private Integer standardPrecision;

        @Schema(description = "是否要求设备")
        private Boolean equipmentRequired;

        @Schema(description = "检验项目正式设备选项")
        @Valid
        private List<EquipmentOption> equipmentOptions;

        @Schema(description = "结果类型", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "结果类型不能为空")
        private String resultType;

        @Schema(description = "固定检验数量")
        private Integer firstInspectionQuantity;

        @Schema(description = "巡检抽样比例")
        private BigDecimal patrolInspectionRatio;
    }

    @Schema(description = "管理后台 - MES QA 检验规程检验项目设备选项")
    @Data
    public static class EquipmentOption {

        @Schema(description = "MES 设备台账 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "检验设备不能为空")
        private Long equipmentId;

        @Schema(description = "设备编码", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "设备编码不能为空")
        private String equipmentCode;

        @Schema(description = "设备名称", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "设备名称不能为空")
        private String equipmentName;

        @Schema(description = "设备编号/出厂编号/台账编码", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "设备编号不能为空")
        private String equipmentNumber;

        @Schema(description = "是否默认设备")
        private Boolean defaultFlag;

        @Schema(description = "排序")
        private Integer sort;
    }
}
