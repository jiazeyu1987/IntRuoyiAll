package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED;

@Service
public class MesTeamLeaderOrderProcessTargetService {

    private final MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;

    public MesTeamLeaderOrderProcessTargetService(
            MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper) {
        this.processSnapshotMapper = processSnapshotMapper;
    }

    public MesTeamLeaderOrderProcessTarget requireTarget(MesProcessPoolActiveOrderDO activeOrder,
                                                         Long routeProcessId,
                                                         Long processId) {
        if (activeOrder == null) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, "activeOrder");
        }
        return requireTarget(activeOrder.getId(), activeOrder.getWorkOrderId(), routeProcessId, processId);
    }

    public MesTeamLeaderOrderProcessTarget requireTarget(Long activeOrderId,
                                                         Long workOrderId,
                                                         Long routeProcessId,
                                                         Long processId) {
        if (activeOrderId == null || workOrderId == null || routeProcessId == null || processId == null) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        MesProcessPoolActiveOrderProcessSnapshotDO snapshot =
                processSnapshotMapper.selectByActiveOrderAndProcess(activeOrderId, routeProcessId, processId);
        if (snapshot == null || !workOrderId.equals(snapshot.getWorkOrderId())) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        requirePositive(snapshot.getErpFixedQuantitySnapshot(), activeOrderId);
        requirePositive(snapshot.getProductionQuantityFactorSnapshot(), activeOrderId);
        requirePositive(snapshot.getPlannedQuantitySnapshot(), activeOrderId);
        return new MesTeamLeaderOrderProcessTarget(snapshot.getRouteProcessId(), snapshot.getProcessId(),
                snapshot.getErpFixedQuantitySnapshot(), snapshot.getProductionQuantityFactorSnapshot(),
                snapshot.getPlannedQuantitySnapshot());
    }

    private static void requirePositive(BigDecimal value, Long activeOrderId) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
    }
}
