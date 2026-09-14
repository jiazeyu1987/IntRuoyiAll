package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_DUPLICATE;

@Service
public class MesTeamLeaderOrderProcessTargetService {

    private static final BigDecimal DEFAULT_PRODUCTION_QUANTITY_FACTOR =
            BigDecimal.ONE.setScale(6, RoundingMode.HALF_UP);

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

    public Optional<MesTeamLeaderOrderProcessTarget> findTarget(MesProcessPoolActiveOrderDO activeOrder,
                                                                Long routeProcessId,
                                                                Long processId) {
        if (activeOrder == null || activeOrder.getId() == null || activeOrder.getWorkOrderId() == null
                || routeProcessId == null || processId == null) {
            return Optional.empty();
        }
        MesProcessPoolActiveOrderProcessSnapshotDO snapshot =
                processSnapshotMapper.selectByActiveOrderAndProcess(activeOrder.getId(), routeProcessId, processId);
        if (snapshot == null || !activeOrder.getWorkOrderId().equals(snapshot.getWorkOrderId())) {
            return Optional.empty();
        }
        return Optional.of(buildTarget(snapshot, activeOrder.getId()));
    }

    public Optional<MesTeamLeaderOrderProcessTarget> findUniqueTargetForProcess(
            MesProcessPoolActiveOrderDO activeOrder, Long processId) {
        if (activeOrder == null || activeOrder.getId() == null || activeOrder.getWorkOrderId() == null
                || processId == null) {
            return Optional.empty();
        }
        List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots =
                processSnapshotMapper.selectListByActiveOrderAndProcess(activeOrder.getId(), processId);
        if (snapshots.size() > 1) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_DUPLICATE, activeOrder.getId(), processId);
        }
        if (snapshots.isEmpty() || !activeOrder.getWorkOrderId().equals(snapshots.get(0).getWorkOrderId())) {
            return Optional.empty();
        }
        return Optional.of(buildTarget(snapshots.get(0), activeOrder.getId()));
    }

    public MesTeamLeaderOrderProcessTarget requireUniqueTargetForProcess(
            MesProcessPoolActiveOrderDO activeOrder, Long processId) {
        return findUniqueTargetForProcess(activeOrder, processId)
                .orElseThrow(() -> exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED,
                        activeOrder == null ? null : activeOrder.getId()));
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
        return buildTarget(snapshot, activeOrderId);
    }

    private static MesTeamLeaderOrderProcessTarget buildTarget(MesProcessPoolActiveOrderProcessSnapshotDO snapshot,
                                                               Long activeOrderId) {
        BigDecimal erpFixedQuantity = requirePositive(snapshot.getErpFixedQuantitySnapshot(), activeOrderId)
                .setScale(6, RoundingMode.HALF_UP);
        BigDecimal productionQuantityFactor = normalizeProductionQuantityFactor(
                snapshot.getProductionQuantityFactorSnapshot(), activeOrderId);
        BigDecimal plannedQuantity = normalizePlannedQuantity(snapshot.getPlannedQuantitySnapshot(),
                erpFixedQuantity, productionQuantityFactor, activeOrderId);
        return new MesTeamLeaderOrderProcessTarget(snapshot.getRouteProcessId(), snapshot.getProcessId(),
                erpFixedQuantity, productionQuantityFactor, plannedQuantity);
    }

    private static BigDecimal normalizeProductionQuantityFactor(BigDecimal value, Long activeOrderId) {
        if (value == null) {
            return DEFAULT_PRODUCTION_QUANTITY_FACTOR;
        }
        return requirePositive(value, activeOrderId).setScale(6, RoundingMode.HALF_UP);
    }

    private static BigDecimal normalizePlannedQuantity(BigDecimal value,
                                                       BigDecimal erpFixedQuantity,
                                                       BigDecimal productionQuantityFactor,
                                                       Long activeOrderId) {
        if (value == null) {
            return erpFixedQuantity.multiply(productionQuantityFactor).setScale(6, RoundingMode.HALF_UP);
        }
        return requirePositive(value, activeOrderId).setScale(6, RoundingMode.HALF_UP);
    }

    private static BigDecimal requirePositive(BigDecimal value, Long activeOrderId) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        return value;
    }
}
