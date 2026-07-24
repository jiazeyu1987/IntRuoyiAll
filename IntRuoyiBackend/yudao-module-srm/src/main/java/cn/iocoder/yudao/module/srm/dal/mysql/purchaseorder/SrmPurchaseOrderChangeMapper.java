package cn.iocoder.yudao.module.srm.dal.mysql.purchaseorder;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.dal.dataobject.purchaseorder.SrmPurchaseOrderChangeDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmPurchaseOrderChangeMapper extends BaseMapperX<SrmPurchaseOrderChangeDO> {

    default SrmPurchaseOrderChangeDO selectLatestPendingByOrderId(Long tenantId, Long orderId) {
        return selectOne(new LambdaQueryWrapperX<SrmPurchaseOrderChangeDO>()
                .eq(SrmPurchaseOrderChangeDO::getTenantId, tenantId)
                .eq(SrmPurchaseOrderChangeDO::getOrderId, orderId)
                .eq(SrmPurchaseOrderChangeDO::getChangeStatus, "PENDING_CONFIRM")
                .orderByDesc(SrmPurchaseOrderChangeDO::getId)
                .last("LIMIT 1"));
    }
}
