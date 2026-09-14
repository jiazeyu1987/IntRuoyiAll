package cn.iocoder.yudao.module.erp.dal.mysql.nastablesync;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.nastablesync.ErpNasTableSyncPlanItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpNasTableSyncPlanItemMapper extends BaseMapperX<ErpNasTableSyncPlanItemDO> {

    default List<ErpNasTableSyncPlanItemDO> selectListByPlanId(Long planId) {
        return selectList(new LambdaQueryWrapperX<ErpNasTableSyncPlanItemDO>()
                .eq(ErpNasTableSyncPlanItemDO::getPlanId, planId)
                .orderByAsc(ErpNasTableSyncPlanItemDO::getSortOrder)
                .orderByAsc(ErpNasTableSyncPlanItemDO::getId));
    }
}
