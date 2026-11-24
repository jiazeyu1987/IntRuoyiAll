package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * ERP 销售订单明项目 Mapper
 *
 * @author 瑛泰源码
 */
@Mapper
public interface ErpSaleOrderItemMapper extends BaseMapperX<ErpSaleOrderItemDO> {

    default int updateSourceItem(ErpSaleOrderItemDO item, Long orderId) {
        return update(new ErpSaleOrderItemDO(), new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ErpSaleOrderItemDO>()
                .eq(ErpSaleOrderItemDO::getId, item.getId()).eq(ErpSaleOrderItemDO::getOrderId, orderId)
                .set(ErpSaleOrderItemDO::getSourceEntryId, item.getSourceEntryId())
                .set(ErpSaleOrderItemDO::getProductId, item.getProductId())
                .set(ErpSaleOrderItemDO::getProductUnitId, item.getProductUnitId())
                .set(ErpSaleOrderItemDO::getProductPrice, item.getProductPrice())
                .set(ErpSaleOrderItemDO::getCount, item.getCount())
                .set(ErpSaleOrderItemDO::getTotalPrice, item.getTotalPrice())
                .set(ErpSaleOrderItemDO::getTaxPercent, item.getTaxPercent())
                .set(ErpSaleOrderItemDO::getTaxPrice, item.getTaxPrice())
                .set(ErpSaleOrderItemDO::getRemark, item.getRemark())
                .set(ErpSaleOrderItemDO::getOutCount, item.getOutCount())
                .set(ErpSaleOrderItemDO::getReturnCount, item.getReturnCount()));
    }

    default List<ErpSaleOrderItemDO> selectListByOrderId(Long orderId) {
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ErpSaleOrderItemDO>().eq(ErpSaleOrderItemDO::getOrderId, orderId)
                .last(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive() ? "FOR UPDATE" : ""));
    }

    default List<ErpSaleOrderItemDO> selectListByOrderIds(Collection<Long> orderIds) {
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ErpSaleOrderItemDO>().in(ErpSaleOrderItemDO::getOrderId, orderIds)
                .last(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive() ? "FOR UPDATE" : ""));
    }

    default int deleteByOrderId(Long orderId) {
        return delete(ErpSaleOrderItemDO::getOrderId, orderId);
    }

}