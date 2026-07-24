package cn.iocoder.yudao.module.srm.controller.admin.supplierrisk.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - SRM 供应商风险 Response VO")
@Data
public class SrmSupplierRiskRespVO {

    @Schema(description = "风险记录编号", example = "1")
    private Long id;

    @Schema(description = "ERP 供应商编号", example = "2")
    private Long supplierId;

    @Schema(description = "供应商名称", example = "山东瑛泰医疗器械有限公司")
    private String supplierName;

    @Schema(description = "风险等级", example = "HIGH")
    private String riskLevel;

    @Schema(description = "风险等级文案", example = "高")
    private String riskLevelLabel;

    @Schema(description = "风险状态", example = "OPEN")
    private String riskStatus;

    @Schema(description = "风险状态文案", example = "未处理")
    private String riskStatusLabel;

    @Schema(description = "来源类型", example = "ACCESS_REQUEST")
    private String sourceType;

    @Schema(description = "来源类型文案", example = "准入申请")
    private String sourceTypeLabel;

    @Schema(description = "来源编号", example = "1")
    private Long sourceId;

    @Schema(description = "来源编码", example = "ACCESS-001")
    private String sourceCode;

    @Schema(description = "来源名称", example = "准入申请-供应商A")
    private String sourceName;

    @Schema(description = "风险描述")
    private String riskDescription;

    @Schema(description = "风险备注")
    private String riskRemark;

    @Schema(description = "上报人")
    private String reportedName;

    @Schema(description = "上报时间")
    private LocalDateTime reportedTime;

    @Schema(description = "处理人")
    private String resolvedName;

    @Schema(description = "处理时间")
    private LocalDateTime resolvedTime;

    @Schema(description = "处理说明")
    private String resolutionRemark;
}
