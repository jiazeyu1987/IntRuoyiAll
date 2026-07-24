package cn.iocoder.yudao.module.srm.dal.dataobject.supplier;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

@TableName("srm_supplier_risk")
@KeySequence("srm_supplier_risk_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmSupplierRiskDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long supplierId;

    private Long supplierAccessId;

    private String riskLevel;

    private String riskStatus;

    private String sourceType;

    private Long sourceId;

    private String sourceCode;

    private String sourceName;

    private String riskDescription;

    private String riskRemark;

    private Long reportedBy;

    private String reportedName;

    private LocalDateTime reportedTime;

    private Long resolvedBy;

    private String resolvedName;

    private LocalDateTime resolvedTime;

    private String resolutionRemark;
}
