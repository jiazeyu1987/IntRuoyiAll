package cn.iocoder.yudao.module.srm.controller.admin.supplierportal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - SRM 供应商门户申请 Response VO")
@Data
public class SrmSupplierPortalApplicationRespVO {

    private Long id;

    private Long userId;

    private Long supplierId;

    private String companyName;

    private String unifiedSocialCreditCode;

    private String contactName;

    private String contactPhone;

    private String contactEmail;

    private String qualificationAttachmentUrls;

    private LocalDate qualificationExpireDate;

    private String bankName;

    private String bankAccount;

    private String bankAddress;

    private String applicationStatus;

    private String applicationStatusLabel;

    private String submitterName;

    private LocalDateTime submittedTime;

    private String auditName;

    private LocalDateTime auditTime;

    private String auditRemark;
}
