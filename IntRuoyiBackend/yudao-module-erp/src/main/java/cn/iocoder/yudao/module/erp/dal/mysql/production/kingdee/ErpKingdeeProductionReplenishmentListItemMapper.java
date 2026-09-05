package cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionReplenishmentListItemDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ErpKingdeeProductionReplenishmentListItemMapper
        extends BaseMapperX<ErpKingdeeProductionReplenishmentListItemDO> {

    default List<ErpKingdeeProductionReplenishmentListItemDO> selectListByReplenishmentListIds(
            Collection<Long> replenishmentListIds) {
        return selectList(new LambdaQueryWrapperX<ErpKingdeeProductionReplenishmentListItemDO>()
                .inIfPresent(ErpKingdeeProductionReplenishmentListItemDO::getProductionReplenishmentListId,
                        replenishmentListIds)
                .orderByAsc(ErpKingdeeProductionReplenishmentListItemDO::getId));
    }

    default ErpKingdeeProductionReplenishmentListItemDO selectBySourceLineKey(
            String sourceLineKey) {
        return selectOne(ErpKingdeeProductionReplenishmentListItemDO::getSourceLineKey,
                sourceLineKey);
    }

    default List<Long> selectReplenishmentListIdsByProductionOrderNo(String productionOrderNo) {
        return selectObjs(new LambdaQueryWrapperX<ErpKingdeeProductionReplenishmentListItemDO>()
                .select(ErpKingdeeProductionReplenishmentListItemDO::getProductionReplenishmentListId)
                .like(ErpKingdeeProductionReplenishmentListItemDO::getProductionOrderNo, productionOrderNo))
                .stream()
                .map(value -> ((Number) value).longValue())
                .distinct()
                .toList();
    }

    default List<ErpKingdeeProductionReplenishmentListItemDO> selectListByProductionOrderNo(
            String productionOrderNo) {
        return selectList(new LambdaQueryWrapperX<ErpKingdeeProductionReplenishmentListItemDO>()
                .eq(ErpKingdeeProductionReplenishmentListItemDO::getProductionOrderNo, productionOrderNo)
                .orderByAsc(ErpKingdeeProductionReplenishmentListItemDO::getProductionReplenishmentListId)
                .orderByAsc(ErpKingdeeProductionReplenishmentListItemDO::getId));
    }

    @Delete("DELETE FROM erp_kingdee_production_replenishment_list_item "
            + "WHERE production_replenishment_list_id = #{productionReplenishmentListId}")
    int deleteByProductionReplenishmentListId(Long productionReplenishmentListId);

}
