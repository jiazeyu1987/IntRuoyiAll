package cn.iocoder.yudao.module.erp.dal.mysql.kingdeeautosync;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.kingdeeautosync.ErpKingdeeTableAutoSyncPlanItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpKingdeeTableAutoSyncPlanItemMapper extends BaseMapperX<ErpKingdeeTableAutoSyncPlanItemDO> {

    default List<ErpKingdeeTableAutoSyncPlanItemDO> selectListByPlanId(Long planId) {
        return selectList(new LambdaQueryWrapperX<ErpKingdeeTableAutoSyncPlanItemDO>()
                .eq(ErpKingdeeTableAutoSyncPlanItemDO::getPlanId, planId)
                .orderByAsc(ErpKingdeeTableAutoSyncPlanItemDO::getSortOrder)
                .orderByAsc(ErpKingdeeTableAutoSyncPlanItemDO::getId));
    }
}
