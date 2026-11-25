package cn.iocoder.yudao.module.erp.dal.mysql.kingdeeautosync;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.kingdeeautosync.ErpKingdeeTableAutoSyncPlanDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpKingdeeTableAutoSyncPlanMapper extends BaseMapperX<ErpKingdeeTableAutoSyncPlanDO> {

    int MAX_DAILY_ATTEMPTS = 3;
    int RETRY_INTERVAL_MINUTES = 15;

    default int claimAutoAttempt(Long id, java.time.LocalDate date) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai"));
        return update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ErpKingdeeTableAutoSyncPlanDO>()
                .eq(ErpKingdeeTableAutoSyncPlanDO::getId, id)
                .eq(ErpKingdeeTableAutoSyncPlanDO::getEnabled, true)
                .and(q -> q.isNull(ErpKingdeeTableAutoSyncPlanDO::getLastAutoAttemptDate)
                        .or().lt(ErpKingdeeTableAutoSyncPlanDO::getLastAutoAttemptDate, date)
                        .or(r -> r.eq(ErpKingdeeTableAutoSyncPlanDO::getLastAutoAttemptDate, date)
                                .lt(ErpKingdeeTableAutoSyncPlanDO::getAutoAttemptCount, MAX_DAILY_ATTEMPTS)
                                .le(ErpKingdeeTableAutoSyncPlanDO::getLastAutoAttemptTime, now.minusMinutes(RETRY_INTERVAL_MINUTES))))
                .and(q -> q.isNull(ErpKingdeeTableAutoSyncPlanDO::getLastAutoRunDate)
                        .or().lt(ErpKingdeeTableAutoSyncPlanDO::getLastAutoRunDate, date))
                .setSql("auto_attempt_count = CASE WHEN last_auto_attempt_date = '" + date
                        + "' THEN auto_attempt_count + 1 ELSE 1 END")
                .set(ErpKingdeeTableAutoSyncPlanDO::getLastAutoAttemptTime, now)
                .set(ErpKingdeeTableAutoSyncPlanDO::getLastAutoAttemptDate, date)
                .set(ErpKingdeeTableAutoSyncPlanDO::getLastStatus, "RUNNING")
                .set(ErpKingdeeTableAutoSyncPlanDO::getLastRunTime, java.time.LocalDateTime.now())
                .set(ErpKingdeeTableAutoSyncPlanDO::getLastMessage, "ERP自动同步已开始"));
    }

    default ErpKingdeeTableAutoSyncPlanDO selectCurrentTenantPlan() {
        return selectOne(new LambdaQueryWrapperX<ErpKingdeeTableAutoSyncPlanDO>()
                .orderByDesc(ErpKingdeeTableAutoSyncPlanDO::getId)
                .last("LIMIT 1"));
    }
}
