package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamProcessOverageLimitDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamProcessOverageLimitMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_OVERAGE_LIMIT_EXCEEDED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_OVERAGE_PERCENT_INVALID;

@Service
public class MesTeamLeaderOverageLimitServiceImpl implements MesTeamLeaderOverageLimitService {
    /** The business default used when a production leader has not configured a process limit. */
    static final BigDecimal DEFAULT_OVERAGE_PERCENT = new BigDecimal("10");

    private final MesProcessPoolTeamProcessOverageLimitMapper mapper;

    public MesTeamLeaderOverageLimitServiceImpl(MesProcessPoolTeamProcessOverageLimitMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<MesProcessPoolTeamProcessOverageLimitDO> list(Long leaderUserId) {
        return mapper.selectByLeader(leaderUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProcessPoolTeamProcessOverageLimitDO save(Long leaderUserId, Long routeProcessId, Long processId,
                                                         BigDecimal overagePercent) {
        validatePercent(overagePercent);
        MesProcessPoolTeamProcessOverageLimitDO row = mapper.selectByLeaderAndRouteProcessForUpdate(
                leaderUserId, routeProcessId, processId);
        if (row == null) {
            row = MesProcessPoolTeamProcessOverageLimitDO.builder().leaderUserId(leaderUserId)
                    .routeProcessId(routeProcessId).processId(processId).enabled(Boolean.TRUE)
                    .overagePercent(overagePercent.setScale(4, RoundingMode.HALF_UP)).build();
            mapper.insert(row);
        } else {
            row.setOveragePercent(overagePercent.setScale(4, RoundingMode.HALF_UP)).setEnabled(Boolean.TRUE);
            mapper.updateById(row);
        }
        return row;
    }

    @Override
    public BigDecimal requirePercent(Long leaderUserId, Long routeProcessId, Long processId) {
        MesProcessPoolTeamProcessOverageLimitDO row = mapper.selectByLeaderAndRouteProcess(
                leaderUserId, routeProcessId, processId);
        if (row == null || row.getOveragePercent() == null) {
            return DEFAULT_OVERAGE_PERCENT;
        }
        return row.getOveragePercent();
    }

    @Override
    public BigDecimal findPercent(Long leaderUserId, Long routeProcessId, Long processId) {
        MesProcessPoolTeamProcessOverageLimitDO row = mapper.selectByLeaderAndRouteProcess(
                leaderUserId, routeProcessId, processId);
        return row == null || row.getOveragePercent() == null
                ? DEFAULT_OVERAGE_PERCENT : row.getOveragePercent();
    }

    @Override
    public void assertWithinLimit(Long leaderUserId, Long routeProcessId, Long processId,
                                  BigDecimal plannedQuantity, BigDecimal submittedQuantity) {
        BigDecimal percent = requirePercent(leaderUserId, routeProcessId, processId);
        BigDecimal limit = plannedQuantity.multiply(BigDecimal.ONE.add(percent.movePointLeft(2)));
        if (submittedQuantity.compareTo(limit) > 0) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_OVERAGE_LIMIT_EXCEEDED,
                    submittedQuantity, limit, routeProcessId, processId);
        }
    }

    private void validatePercent(BigDecimal percent) {
        if (percent == null || percent.compareTo(BigDecimal.ZERO) < 0 || percent.compareTo(new BigDecimal("100")) > 0) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_OVERAGE_PERCENT_INVALID, percent);
        }
    }
}
