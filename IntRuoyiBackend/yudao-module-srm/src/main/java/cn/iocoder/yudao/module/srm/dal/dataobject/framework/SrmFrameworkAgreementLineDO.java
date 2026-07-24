package cn.iocoder.yudao.module.srm.dal.dataobject.framework;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

@TableName("srm_framework_agreement_line")
@KeySequence("srm_framework_agreement_line_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmFrameworkAgreementLineDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long agreementId;

    private Long frameworkPlanId;

    private Long frameworkPlanLineId;

    private Long materialId;

    private String materialCode;

    private String materialName;

    private BigDecimal quantity;

    private String unit;

    private BigDecimal budgetAmount;
}
