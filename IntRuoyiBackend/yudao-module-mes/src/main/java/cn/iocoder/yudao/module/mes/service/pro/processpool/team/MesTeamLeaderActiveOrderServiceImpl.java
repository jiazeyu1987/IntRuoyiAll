package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditMapper;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;

@Service
@Validated
public class MesTeamLeaderActiveOrderServiceImpl implements MesTeamLeaderActiveOrderService {

    static final String STATUS_ACTIVE = "ACTIVE";
    static final String STATUS_REMOVED = "REMOVED";

    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesProWorkOrderService workOrderService;
    private final MesProcessPoolTeamMaintenanceAuditMapper auditMapper;

    public MesTeamLeaderActiveOrderServiceImpl(MesProcessPoolActiveOrderMapper activeOrderMapper,
                                               MesProWorkOrderService workOrderService,
                                               MesProcessPoolTeamMaintenanceAuditMapper auditMapper) {
        this.activeOrderMapper = activeOrderMapper;
        this.workOrderService = workOrderService;
        this.auditMapper = auditMapper;
    }

    @Override
    public Long addActiveOrder(MesTeamLeaderActiveOrderAddReqBO reqBO) {
        if (reqBO == null || reqBO.getLeaderUserId() == null || reqBO.getWorkOrderId() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "activeOrder");
        }
        workOrderService.validateWorkOrderExists(reqBO.getWorkOrderId());
        MesProcessPoolActiveOrderDO activeOrder = MesProcessPoolActiveOrderDO.builder()
                .leaderUserId(reqBO.getLeaderUserId())
                .workOrderId(reqBO.getWorkOrderId())
                .activeStatus(STATUS_ACTIVE)
                .joinedAt(LocalDateTime.now())
                .build();
        activeOrderMapper.insert(activeOrder);
        TeamMaintenanceAuditSupport.insertAudit(auditMapper, reqBO.getLeaderUserId(), "ADD_ACTIVE_ORDER",
                "ACTIVE_ORDER", activeOrder.getId(), null, activeOrder.toString());
        return activeOrder.getId();
    }

    @Override
    public void removeActiveOrder(MesTeamLeaderActiveOrderRemoveReqBO reqBO) {
        if (reqBO == null || reqBO.getLeaderUserId() == null || reqBO.getActiveOrderId() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "removeActiveOrder");
        }
        MesProcessPoolActiveOrderDO activeOrder = activeOrderMapper.selectById(reqBO.getActiveOrderId());
        if (activeOrder == null || !Objects.equals(activeOrder.getLeaderUserId(), reqBO.getLeaderUserId())) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS, reqBO.getActiveOrderId());
        }
        MesProcessPoolActiveOrderDO update = MesProcessPoolActiveOrderDO.builder()
                .id(activeOrder.getId())
                .activeStatus(STATUS_REMOVED)
                .removedAt(LocalDateTime.now())
                .build();
        activeOrderMapper.updateById(update);
        TeamMaintenanceAuditSupport.insertAudit(auditMapper, reqBO.getLeaderUserId(), "REMOVE_ACTIVE_ORDER",
                "ACTIVE_ORDER", activeOrder.getId(), activeOrder.toString(), update.toString());
    }

    @Override
    public List<MesProcessPoolActiveOrderDO> listActiveOrders(Long leaderUserId) {
        if (leaderUserId == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "activeOrderList");
        }
        return activeOrderMapper.selectActiveListByLeader(leaderUserId);
    }
}
