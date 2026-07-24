package cn.iocoder.yudao.module.srm.dal.dataobject.framework;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("srm_framework_plan")
@KeySequence("srm_framework_plan_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmFrameworkPlanDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String frameworkPlanNo;

    private String planTitle;

    private Long supplierId;

    private String supplierName;

    private String procurementMethod;

    private BigDecimal budgetAmount;

    private LocalDate validStartDate;

    private LocalDate validEndDate;

    private String planStatus;

    private String remark;

    private Long submittedBy;

    private String submittedName;

    private LocalDateTime submittedTime;

    private Long auditBy;

    private String auditName;

    private LocalDateTime auditTime;

    private String auditRemark;

    private Long agreementId;

    private String agreementNo;

    private LocalDateTime agreementTime;
}
