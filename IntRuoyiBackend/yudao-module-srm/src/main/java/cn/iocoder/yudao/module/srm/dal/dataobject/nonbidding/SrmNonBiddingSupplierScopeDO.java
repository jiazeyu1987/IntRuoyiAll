package cn.iocoder.yudao.module.srm.dal.dataobject.nonbidding;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@TableName("srm_non_bidding_supplier_scope")
@KeySequence("srm_non_bidding_supplier_scope_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmNonBiddingSupplierScopeDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long projectId;

    private Long supplierId;

    private String supplierName;
}
