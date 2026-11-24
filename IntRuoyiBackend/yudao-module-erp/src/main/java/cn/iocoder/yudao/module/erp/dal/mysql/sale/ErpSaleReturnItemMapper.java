package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnItemDO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * ERP 销售退货项 Mapper
 *
 * @author 瑛泰源码
 */
@Mapper
public interface ErpSaleReturnItemMapper extends BaseMapperX<ErpSaleReturnItemDO> {

    default List<ErpSaleReturnItemDO> selectListByReturnId(Long returnId) {
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ErpSaleReturnItemDO>().eq(ErpSaleReturnItemDO::getReturnId, returnId)
                .last(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive() ? "FOR UPDATE" : ""));
    }

    default List<ErpSaleReturnItemDO> selectListByReturnIds(Collection<Long> returnIds) {
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ErpSaleReturnItemDO>().in(ErpSaleReturnItemDO::getReturnId, returnIds)
                .last(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive() ? "FOR UPDATE" : ""));
    }

    default int deleteByReturnId(Long returnId) {
        return delete(ErpSaleReturnItemDO::getReturnId, returnId);
    }

    /**
     * 基于销售订单编号，查询每个销售订单项的退货数量之和
     *
     * @param returnIds 出库订单项编号数组
     * @return key：销售订单项编号；value：退货数量之和
     */
    default Map<Long, BigDecimal> selectOrderItemCountSumMapByReturnIds(Collection<Long> returnIds) {
        if (CollUtil.isEmpty(returnIds)) {
            return Collections.emptyMap();
        }
        List<ErpSaleReturnItemDO> rows = selectList(new QueryWrapper<ErpSaleReturnItemDO>()
                .in("return_id", returnIds).last(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive() ? "FOR UPDATE" : ""));
        Map<Long, BigDecimal> result = new java.util.HashMap<>();
        for (var row : rows) {
            if (row.getCount() == null) throw new IllegalStateException("单据明细数量为空");
            result.merge(row.getOrderItemId(), row.getCount(), BigDecimal::add);
        }
        return result;
    }

}