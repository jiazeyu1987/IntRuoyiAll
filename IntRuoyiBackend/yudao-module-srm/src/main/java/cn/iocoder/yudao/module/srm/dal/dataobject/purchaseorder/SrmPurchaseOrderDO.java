package cn.iocoder.yudao.module.srm.dal.dataobject.purchaseorder;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

@TableName("srm_purchase_order")
@KeySequence("srm_purchase_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmPurchaseOrderDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String orderNo;

    private Long sourcePlanId;

    private String sourcePlanNo;

    private Long supplierId;

    private String supplierName;

    private String orderStatus;

    private String orderRemark;

    private Long confirmedBy;

    private String confirmedName;

    private LocalDateTime confirmedTime;

    private String confirmRemark;
}
