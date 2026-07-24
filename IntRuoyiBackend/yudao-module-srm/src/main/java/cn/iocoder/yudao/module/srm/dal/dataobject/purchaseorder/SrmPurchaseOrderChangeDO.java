package cn.iocoder.yudao.module.srm.dal.dataobject.purchaseorder;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

@TableName("srm_purchase_order_change")
@KeySequence("srm_purchase_order_change_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmPurchaseOrderChangeDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String changeNo;

    private Long orderId;

    private String orderNo;

    private Long supplierId;

    private String supplierName;

    private String changeStatus;

    private String changeReason;

    private String changeRemark;

    private Long submittedBy;

    private String submittedName;

    private LocalDateTime submittedTime;

    private Long confirmedBy;

    private String confirmedName;

    private LocalDateTime confirmedTime;

    private String confirmRemark;

    private Long rejectedBy;

    private String rejectedName;

    private LocalDateTime rejectedTime;

    private String rejectRemark;

    private Long withdrawnBy;

    private String withdrawnName;

    private LocalDateTime withdrawnTime;

    private String withdrawRemark;
}
