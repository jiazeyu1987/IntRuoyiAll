package cn.iocoder.yudao.module.srm.controller.admin.supplieraccess.vo;

import cn.iocoder.yudao.module.srm.controller.admin.supplierrisk.vo.SrmSupplierRiskRespVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - SRM 供应商统一档案 Response VO")
@Data
public class SrmSupplierProfileRespVO {

    @Schema(description = "ERP 供应商编号", example = "2")
    private Long supplierId;

    @Schema(description = "供应商名称", example = "山东瑛泰医疗器械有限公司")
    private String supplierName;

    @Schema(description = "准入档案编号", example = "1")
    private Long accessId;

    @Schema(description = "准入状态", example = "APPROVED")
    private String accessStatus;

    @Schema(description = "准入状态文案", example = "已通过")
    private String accessStatusLabel;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;

    @Schema(description = "门户联系人", example = "张三")
    private String portalContactName;

    @Schema(description = "门户联系电话", example = "13800138000")
    private String portalContactPhone;

    @Schema(description = "资质到期日")
    private LocalDate qualificationExpireDate;

    @Schema(description = "资质状态文案", example = "待更新")
    private String qualificationStatusLabel;

    @Schema(description = "样品测试状态", example = "PASSED")
    private String sampleTestStatus;

    @Schema(description = "样品测试状态文案", example = "已通过")
    private String sampleTestStatusLabel;

    @Schema(description = "小批试用状态", example = "PENDING")
    private String trialOrderStatus;

    @Schema(description = "小批试用状态文案", example = "待审核")
    private String trialOrderStatusLabel;

    @Schema(description = "阶段概览", example = "待准入审批")
    private String onboardingStageSummary;

    @Schema(description = "资格概览", example = "合格")
    private String eligibilitySummary;

    @Schema(description = "准入备注")
    private String accessRemark;

    @Schema(description = "提交人")
    private String submittedName;

    @Schema(description = "提交时间")
    private LocalDateTime submittedTime;

    @Schema(description = "审核人")
    private String auditName;

    @Schema(description = "审核时间")
    private LocalDateTime auditTime;

    @Schema(description = "审核备注")
    private String auditRemark;

    @Schema(description = "样品测试审核人")
    private String sampleAuditName;

    @Schema(description = "样品测试审核时间")
    private LocalDateTime sampleAuditTime;

    @Schema(description = "样品测试审核意见")
    private String sampleAuditRemark;

    @Schema(description = "小批试用审核人")
    private String trialAuditName;

    @Schema(description = "小批试用审核时间")
    private LocalDateTime trialAuditTime;

    @Schema(description = "小批试用审核意见")
    private String trialAuditRemark;

    @Schema(description = "未处理高风险数量", example = "1")
    private Long openHighRiskCount;

    @Schema(description = "全部风险记录")
    private List<SrmSupplierRiskRespVO> riskList;
}
