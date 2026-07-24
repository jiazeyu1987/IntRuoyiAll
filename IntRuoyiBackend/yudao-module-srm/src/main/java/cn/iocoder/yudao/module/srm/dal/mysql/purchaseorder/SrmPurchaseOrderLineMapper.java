package cn.iocoder.yudao.module.srm.dal.mysql.purchaseorder;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.dal.dataobject.purchaseorder.SrmPurchaseOrderLineDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SrmPurchaseOrderLineMapper extends BaseMapperX<SrmPurchaseOrderLineDO> {

    default List<SrmPurchaseOrderLineDO> selectListByOrderId(Long orderId) {
        return selectList(new LambdaQueryWrapperX<SrmPurchaseOrderLineDO>()
                .eq(SrmPurchaseOrderLineDO::getOrderId, orderId)
                .orderByAsc(SrmPurchaseOrderLineDO::getId));
    }

    default void clearPendingChangeFieldsByOrderId(Long orderId) {
        update(new LambdaUpdateWrapper<SrmPurchaseOrderLineDO>()
                .eq(SrmPurchaseOrderLineDO::getOrderId, orderId)
                .set(SrmPurchaseOrderLineDO::getPendingChangedQuantity, null)
                .set(SrmPurchaseOrderLineDO::getPendingChangedDeliveryDate, null)
                .set(SrmPurchaseOrderLineDO::getPendingChangedRemark, null));
    }
}
