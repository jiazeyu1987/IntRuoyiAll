package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * ERP 采购入库项 Mapper
 *
 * @author 瑛泰源码
 */
@Mapper
public interface ErpPurchaseInItemMapper extends BaseMapperX<ErpPurchaseInItemDO> {

    default List<ErpPurchaseInItemDO> selectListByInId(Long inId) {
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ErpPurchaseInItemDO>().eq(ErpPurchaseInItemDO::getInId, inId)
                .last(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive() ? "FOR UPDATE" : ""));
    }

    default List<ErpPurchaseInItemDO> selectListByInIds(Collection<Long> inIds) {
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ErpPurchaseInItemDO>().in(ErpPurchaseInItemDO::getInId, inIds)
                .last(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive() ? "FOR UPDATE" : ""));
    }

    default int deleteByInId(Long inId) {
        return delete(ErpPurchaseInItemDO::getInId, inId);
    }

    /**
     * 基于采购订单编号，查询每个采购订单项的入库数量之和
     *
     * @param inIds 入库订单项编号数组
     * @return key：采购订单项编号；value：入库数量之和
     */
    default Map<Long, BigDecimal> selectOrderItemCountSumMapByInIds(Collection<Long> inIds) {
        if (CollUtil.isEmpty(inIds)) {
            return Collections.emptyMap();
        }
        List<ErpPurchaseInItemDO> rows = selectList(new QueryWrapper<ErpPurchaseInItemDO>()
                .in("in_id", inIds).last(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive() ? "FOR UPDATE" : ""));
        Map<Long, BigDecimal> result = new java.util.HashMap<>();
        for (var row : rows) {
            if (row.getCount() == null) throw new IllegalStateException("单据明细数量为空");
            result.merge(row.getOrderItemId(), row.getCount(), BigDecimal::add);
        }
        return result;
    }

}