package cn.iocoder.yudao.module.erp.dal.mysql.kingdeeautosync;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.kingdeeautosync.ErpKingdeeTableAutoSyncPlanDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpKingdeeTableAutoSyncPlanMapper extends BaseMapperX<ErpKingdeeTableAutoSyncPlanDO> {

    default ErpKingdeeTableAutoSyncPlanDO selectCurrentTenantPlan() {
        return selectOne(new LambdaQueryWrapperX<ErpKingdeeTableAutoSyncPlanDO>()
                .orderByDesc(ErpKingdeeTableAutoSyncPlanDO::getId)
                .last("LIMIT 1"));
    }
}
