package cn.iocoder.yudao.module.srm.dal.dataobject.outsourceexecution;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("srm_reconciliation")
@KeySequence("srm_reconciliation_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmReconciliationDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String reconciliationNo;

    private Long executionId;

    private String executionNo;

    private Long sourcePurchaseOrderId;

    private String sourcePurchaseOrderNo;

    private Long supplierId;

    private String supplierName;

    private String reconciliationStatus;

    private String simulationSource;

    private String simulationLabel;

    private BigDecimal unitPrice;

    private BigDecimal receivedQuantity;

    private BigDecimal qualifiedQuantity;

    private BigDecimal diffQuantity;

    private BigDecimal reconciliationAmount;

    private BigDecimal diffAmount;

    private Long confirmedBy;

    private String confirmedName;

    private LocalDateTime confirmedTime;

    private String confirmRemark;
}
