package cn.iocoder.yudao.module.dcc.dal.mysql.permission;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclRestorePlanDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;

@Mapper
public interface DccNasAclRestorePlanMapper extends BaseMapperX<DccNasAclRestorePlanDO> {

    default int claimReadyPlan(Long planId, LocalDateTime startedAt) {
        return update(null, new LambdaUpdateWrapper<DccNasAclRestorePlanDO>()
                .eq(DccNasAclRestorePlanDO::getId, planId)
                .eq(DccNasAclRestorePlanDO::getStatus, "READY")
                .set(DccNasAclRestorePlanDO::getStatus, "EXECUTING")
                .set(DccNasAclRestorePlanDO::getStartedAt, startedAt));
    }

    default int reclaimExecutingPlan(Long planId, LocalDateTime currentStartedAt, LocalDateTime reclaimedAt) {
        return update(null, new LambdaUpdateWrapper<DccNasAclRestorePlanDO>()
                .eq(DccNasAclRestorePlanDO::getId, planId)
                .eq(DccNasAclRestorePlanDO::getStatus, "EXECUTING")
                .eq(DccNasAclRestorePlanDO::getStartedAt, currentStartedAt)
                .set(DccNasAclRestorePlanDO::getStartedAt, reclaimedAt));
    }

    default int refreshExecutingPlanLease(Long planId, LocalDateTime currentStartedAt, LocalDateTime refreshedAt) {
        return update(null, new LambdaUpdateWrapper<DccNasAclRestorePlanDO>()
                .eq(DccNasAclRestorePlanDO::getId, planId)
                .eq(DccNasAclRestorePlanDO::getStatus, "EXECUTING")
                .eq(DccNasAclRestorePlanDO::getStartedAt, currentStartedAt)
                .set(DccNasAclRestorePlanDO::getStartedAt, refreshedAt));
    }
}
