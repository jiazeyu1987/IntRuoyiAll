package cn.iocoder.yudao.module.srm.dal.dataobject.purchaseorder;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@TableName("srm_purchase_order_change_line")
@KeySequence("srm_purchase_order_change_line_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmPurchaseOrderChangeLineDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long changeId;

    private Long orderLineId;

    private Long materialId;

    private String materialCode;

    private String materialName;

    private BigDecimal beforeQuantity;

    private LocalDate beforeDeliveryDate;

    private String beforeSupplierRemark;

    private BigDecimal changedQuantity;

    private LocalDate changedDeliveryDate;

    private String changedSupplierRemark;
}
