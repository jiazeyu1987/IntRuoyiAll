package cn.iocoder.yudao.module.erp.dal.mysql.nastablesync;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.nastablesync.ErpNasTableSyncPlanDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpNasTableSyncPlanMapper extends BaseMapperX<ErpNasTableSyncPlanDO> {

    default ErpNasTableSyncPlanDO selectCurrentTenantPlan() {
        return selectOne(new LambdaQueryWrapperX<ErpNasTableSyncPlanDO>()
                .orderByDesc(ErpNasTableSyncPlanDO::getId)
                .last("LIMIT 1"));
    }
}
