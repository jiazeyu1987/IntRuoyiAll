package cn.iocoder.yudao.module.srm.dal.dataobject.supplier;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("srm_supplier_portal_application")
@KeySequence("srm_supplier_portal_application_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmSupplierPortalApplicationDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;

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

    private String submitterName;

    private LocalDateTime submittedTime;

    private Long auditBy;

    private String auditName;

    private LocalDateTime auditTime;

    private String auditRemark;
}
