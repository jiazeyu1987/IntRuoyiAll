package cn.iocoder.yudao.module.srm.dal.dataobject.purchaseorder;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@TableName("srm_purchase_order_line")
@KeySequence("srm_purchase_order_line_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmPurchaseOrderLineDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long orderId;

    private String lineNo;

    private Long sourcePlanLineId;

    private Long materialId;

    private String materialCode;

    private String materialName;

    private BigDecimal requestedQuantity;

    private String unit;

    private LocalDate requestedDeliveryDate;

    private BigDecimal confirmedQuantity;

    private LocalDate confirmedDeliveryDate;

    private String supplierRemark;

    private BigDecimal pendingChangedQuantity;

    private LocalDate pendingChangedDeliveryDate;

    private String pendingChangedRemark;
}
