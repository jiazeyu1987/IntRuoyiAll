package cn.iocoder.yudao.module.srm.dal.dataobject.framework;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@TableName("srm_framework_agreement")
@KeySequence("srm_framework_agreement_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmFrameworkAgreementDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String agreementNo;

    private Long frameworkPlanId;

    private String frameworkPlanNo;

    private Long supplierId;

    private String supplierName;

    private String procurementMethod;

    private BigDecimal budgetAmount;

    private LocalDate validStartDate;

    private LocalDate validEndDate;

    private String agreementStatus;

    private String remark;
}
