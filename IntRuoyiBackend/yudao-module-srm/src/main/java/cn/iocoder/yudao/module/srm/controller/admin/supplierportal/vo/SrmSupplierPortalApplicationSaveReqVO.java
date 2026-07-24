package cn.iocoder.yudao.module.srm.controller.admin.supplierportal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - SRM 供应商门户申请保存 Request VO")
@Data
public class SrmSupplierPortalApplicationSaveReqVO {

    @Schema(description = "企业名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String companyName;

    @Schema(description = "统一社会信用代码")
    private String unifiedSocialCreditCode;

    @Schema(description = "联系人")
    private String contactName;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "联系邮箱")
    @Email(message = "联系邮箱格式不正确")
    private String contactEmail;

    @Schema(description = "资质附件 URL，多个以换行分隔")
    private String qualificationAttachmentUrls;

    @Schema(description = "资质到期日")
    private LocalDate qualificationExpireDate;

    @Schema(description = "开户行")
    private String bankName;

    @Schema(description = "银行账号")
    private String bankAccount;

    @Schema(description = "开户地址")
    private String bankAddress;
}
