package cn.iocoder.yudao.module.srm.controller.admin.supplieraccess.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - SRM 供应商准入 Response VO")
@Data
public class SrmSupplierAccessRespVO {

    @Schema(description = "准入档案编号", example = "1")
    private Long id;

    @Schema(description = "ERP 供应商编号", example = "2")
    private Long supplierId;

    @Schema(description = "供应商名称", example = "山东瑛泰医疗器械有限公司")
    private String supplierName;

    @Schema(description = "准入状态", example = "APPROVED")
    private String accessStatus;

    @Schema(description = "准入状态文案", example = "已通过")
    private String accessStatusLabel;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;

    @Schema(description = "准入备注")
    private String accessRemark;

    @Schema(description = "门户联系人", example = "张三")
    private String portalContactName;

    @Schema(description = "门户联系电话", example = "13800138000")
    private String portalContactPhone;

    @Schema(description = "资质到期日")
    private LocalDate qualificationExpireDate;

    @Schema(description = "资质状态文案", example = "待更新")
    private String qualificationStatusLabel;

    @Schema(description = "样品测试状态", example = "PENDING")
    private String sampleTestStatus;

    @Schema(description = "样品测试状态文案", example = "待审核")
    private String sampleTestStatusLabel;

    @Schema(description = "小批试用状态", example = "NOT_STARTED")
    private String trialOrderStatus;

    @Schema(description = "小批试用状态文案", example = "未开始")
    private String trialOrderStatusLabel;

    @Schema(description = "准入阶段概览", example = "小批试用中")
    private String onboardingStageSummary;

    @Schema(description = "未处理高风险数量", example = "1")
    private Long openHighRiskCount;

    @Schema(description = "资格概览", example = "高风险阻断")
    private String eligibilitySummary;

    @Schema(description = "提交人", example = "aoteman")
    private String submittedName;

    @Schema(description = "提交时间")
    private LocalDateTime submittedTime;

    @Schema(description = "审核人", example = "aoteman")
    private String auditName;

    @Schema(description = "审核时间")
    private LocalDateTime auditTime;

    @Schema(description = "审核备注")
    private String auditRemark;

    @Schema(description = "停用人")
    private String disabledName;

    @Schema(description = "停用时间")
    private LocalDateTime disabledTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
