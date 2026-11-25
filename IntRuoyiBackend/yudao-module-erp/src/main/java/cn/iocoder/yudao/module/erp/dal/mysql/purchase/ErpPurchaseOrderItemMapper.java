package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * ERP 采购订单明项目 Mapper
 *
 * @author 瑛泰源码
 */
@Mapper
public interface ErpPurchaseOrderItemMapper extends BaseMapperX<ErpPurchaseOrderItemDO> {

    default int updateSourceItem(ErpPurchaseOrderItemDO item, Long orderId) {
        return update(new ErpPurchaseOrderItemDO(), new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ErpPurchaseOrderItemDO>()
                .eq(ErpPurchaseOrderItemDO::getId, item.getId()).eq(ErpPurchaseOrderItemDO::getOrderId, orderId)
                .set(ErpPurchaseOrderItemDO::getSourceEntryId, item.getSourceEntryId())
                .set(ErpPurchaseOrderItemDO::getProductId, item.getProductId())
                .set(ErpPurchaseOrderItemDO::getProductUnitId, item.getProductUnitId())
                .set(ErpPurchaseOrderItemDO::getProductPrice, item.getProductPrice())
                .set(ErpPurchaseOrderItemDO::getCount, item.getCount())
                .set(ErpPurchaseOrderItemDO::getTotalPrice, item.getTotalPrice())
                .set(ErpPurchaseOrderItemDO::getTaxPercent, item.getTaxPercent())
                .set(ErpPurchaseOrderItemDO::getTaxPrice, item.getTaxPrice())
                .set(ErpPurchaseOrderItemDO::getRemark, item.getRemark())
                .set(ErpPurchaseOrderItemDO::getInCount, item.getInCount())
                .set(ErpPurchaseOrderItemDO::getReturnCount, item.getReturnCount()));
    }

    default List<ErpPurchaseOrderItemDO> selectListByOrderId(Long orderId) {
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ErpPurchaseOrderItemDO>().eq(ErpPurchaseOrderItemDO::getOrderId, orderId)
                .last(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive() ? "FOR UPDATE" : ""));
    }

    default List<ErpPurchaseOrderItemDO> selectListByOrderIds(Collection<Long> orderIds) {
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ErpPurchaseOrderItemDO>().in(ErpPurchaseOrderItemDO::getOrderId, orderIds)
                .last(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive() ? "FOR UPDATE" : ""));
    }

    default int deleteByOrderId(Long orderId) {
        return delete(ErpPurchaseOrderItemDO::getOrderId, orderId);
    }

}