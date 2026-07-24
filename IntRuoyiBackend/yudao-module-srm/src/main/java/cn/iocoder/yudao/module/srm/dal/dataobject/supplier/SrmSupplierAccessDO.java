package cn.iocoder.yudao.module.srm.dal.dataobject.supplier;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("srm_supplier_access")
@KeySequence("srm_supplier_access_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmSupplierAccessDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long supplierId;

    private String accessStatus;

    private Boolean enabled;

    private String accessRemark;

    private String portalContactName;

    private String portalContactPhone;

    private LocalDate qualificationExpireDate;

    private String sampleTestStatus;

    private Long sampleAuditBy;

    private String sampleAuditName;

    private LocalDateTime sampleAuditTime;

    private String sampleAuditRemark;

    private String trialOrderStatus;

    private Long trialAuditBy;

    private String trialAuditName;

    private LocalDateTime trialAuditTime;

    private String trialAuditRemark;

    private Long submittedBy;

    private String submittedName;

    private LocalDateTime submittedTime;

    private Long auditBy;

    private String auditName;

    private LocalDateTime auditTime;

    private String auditRemark;

    private Long disabledBy;

    private String disabledName;

    private LocalDateTime disabledTime;
}
