package cn.iocoder.yudao.module.srm.dal.dataobject.procurement;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("srm_procurement_plan")
@KeySequence("srm_procurement_plan_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmProcurementPlanDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String planNo;

    private String planTitle;

    private String procurementMethod;

    private BigDecimal expectedAmount;

    private String planStatus;

    private String remark;

    private Long submittedBy;

    private String submittedName;

    private LocalDateTime submittedTime;

    private Long auditBy;

    private String auditName;

    private LocalDateTime auditTime;

    private String auditRemark;

    private Long generatedProjectId;

    private String generatedProjectNo;

    private String generatedProjectType;

    private LocalDateTime generatedTime;
}
