package cn.iocoder.yudao.module.erp.dal.mysql.stock.kingdee;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.kingdee.ErpKingdeeStockMoveListItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ErpKingdeeStockMoveListItemMapper extends BaseMapperX<ErpKingdeeStockMoveListItemDO> {

    default List<ErpKingdeeStockMoveListItemDO> selectListByStockMoveIds(Collection<Long> stockMoveIds) {
        return selectList(new LambdaQueryWrapperX<ErpKingdeeStockMoveListItemDO>()
                .inIfPresent(ErpKingdeeStockMoveListItemDO::getStockMoveId, stockMoveIds)
                .orderByAsc(ErpKingdeeStockMoveListItemDO::getId));
    }

    default ErpKingdeeStockMoveListItemDO selectBySourceLineKey(String sourceLineKey) {
        return selectOne(ErpKingdeeStockMoveListItemDO::getSourceLineKey, sourceLineKey);
    }

}
