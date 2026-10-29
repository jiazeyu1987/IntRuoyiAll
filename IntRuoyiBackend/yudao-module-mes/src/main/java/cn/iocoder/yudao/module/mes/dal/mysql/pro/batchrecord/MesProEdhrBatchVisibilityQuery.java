package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import java.util.List;

/** Immutable request-local authorization input; its size depends on rules, never the batch count. */
public record MesProEdhrBatchVisibilityQuery(Long tenantId, Long userId, boolean overview, List<Long> ruleIds) {
    public MesProEdhrBatchVisibilityQuery { ruleIds = List.copyOf(ruleIds); }
    public LambdaQueryWrapperX<MesProEdhrBatchExecutionDO> apply(LambdaQueryWrapperX<MesProEdhrBatchExecutionDO> query) {
        query.eq(MesProEdhrBatchExecutionDO::getTenantId, tenantId)
                .apply(MesProEdhrBatchVisibilitySql.visible(userId, overview, ruleIds));
        return query;
    }
}
