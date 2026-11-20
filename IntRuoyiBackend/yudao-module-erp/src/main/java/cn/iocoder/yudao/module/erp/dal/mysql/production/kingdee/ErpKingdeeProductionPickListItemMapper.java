package cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListItemDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Mapper
public interface ErpKingdeeProductionPickListItemMapper
        extends BaseMapperX<ErpKingdeeProductionPickListItemDO> {

    static Map<String, ListFilterQueryUtils.Field> orderFilterFields() {
        return Map.of("productionOrderNos", ListFilterQueryUtils.text(
                "erp_kingdee_production_pick_list_item.production_order_no"));
    }

    default List<Long> selectPickListIdsByOrderFilter(PageParam filter) {
        return selectObjs(ListFilterQueryUtils.apply(filter,
                new QueryWrapper<ErpKingdeeProductionPickListItemDO>()
                        .select("DISTINCT production_pick_list_id"), orderFilterFields()))
                .stream().map(value -> ((Number) value).longValue()).toList();
    }

    default List<ErpKingdeeProductionPickListItemDO> selectListByPickListIds(
            Collection<Long> pickListIds) {
        return selectList(new LambdaQueryWrapperX<ErpKingdeeProductionPickListItemDO>()
                .inIfPresent(ErpKingdeeProductionPickListItemDO::getProductionPickListId,
                        pickListIds)
                .orderByAsc(ErpKingdeeProductionPickListItemDO::getId));
    }

    default ErpKingdeeProductionPickListItemDO selectBySourceLineKey(
            String sourceLineKey) {
        return selectOne(ErpKingdeeProductionPickListItemDO::getSourceLineKey,
                sourceLineKey);
    }

    default List<Long> selectPickListIdsByProductionOrderNo(String productionOrderNo) {
        return selectObjs(new LambdaQueryWrapperX<ErpKingdeeProductionPickListItemDO>()
                .select(ErpKingdeeProductionPickListItemDO::getProductionPickListId)
                .like(ErpKingdeeProductionPickListItemDO::getProductionOrderNo, productionOrderNo))
                .stream()
                .map(value -> ((Number) value).longValue())
                .distinct()
                .toList();
    }

    default List<ErpKingdeeProductionPickListItemDO> selectListByProductionOrderNo(
            String productionOrderNo) {
        return selectList(new LambdaQueryWrapperX<ErpKingdeeProductionPickListItemDO>()
                .eq(ErpKingdeeProductionPickListItemDO::getProductionOrderNo, productionOrderNo)
                .orderByAsc(ErpKingdeeProductionPickListItemDO::getProductionPickListId)
                .orderByAsc(ErpKingdeeProductionPickListItemDO::getId));
    }

    @Delete("DELETE FROM erp_kingdee_production_pick_list_item "
            + "WHERE production_pick_list_id = #{productionPickListId}")
    int deleteByProductionPickListId(Long productionPickListId);

}
