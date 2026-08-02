package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED;

@Service
@Validated
public class MesTeamLeaderActiveOrderServiceImpl implements MesTeamLeaderActiveOrderService {

    static final String STATUS_ACTIVE = "ACTIVE";
    static final String STATUS_REMOVED = "REMOVED";

    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesProWorkOrderService workOrderService;
    private final MesProcessPoolTeamMaintenanceAuditMapper auditMapper;
    private final MesProScheduleOrderMapper scheduleOrderMapper;
    private final MesProScheduleOrderProcessMapper scheduleOrderProcessMapper;
    private final MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;

    public MesTeamLeaderActiveOrderServiceImpl(MesProcessPoolActiveOrderMapper activeOrderMapper,
                                               MesProWorkOrderService workOrderService,
                                               MesProcessPoolTeamMaintenanceAuditMapper auditMapper,
                                               MesProScheduleOrderMapper scheduleOrderMapper,
                                               MesProScheduleOrderProcessMapper scheduleOrderProcessMapper,
                                               MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper) {
        this.activeOrderMapper = activeOrderMapper;
        this.workOrderService = workOrderService;
        this.auditMapper = auditMapper;
        this.scheduleOrderMapper = scheduleOrderMapper;
        this.scheduleOrderProcessMapper = scheduleOrderProcessMapper;
        this.processSnapshotMapper = processSnapshotMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addActiveOrder(MesTeamLeaderActiveOrderAddReqBO reqBO) {
        if (reqBO == null || reqBO.getLeaderUserId() == null || reqBO.getWorkOrderId() == null
                || reqBO.getRouteId() == null || reqBO.getRouteVersionId() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "activeOrder");
        }
        MesProcessPoolActiveOrderDO existing = selectExistingActiveOrder(reqBO);
        if (existing != null) {
            return existing.getId();
        }
        BigDecimal erpFixedQuantity = workOrderService.validateWorkOrderExists(reqBO.getWorkOrderId()).getQuantity();
        if (erpFixedQuantity == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "activeOrder.erpFixedQuantitySnapshot");
        }
        MesProScheduleOrderDO scheduleOrder = requireMatchingScheduleOrder(reqBO);
        MesProcessPoolActiveOrderDO activeOrder = MesProcessPoolActiveOrderDO.builder()
                .leaderUserId(reqBO.getLeaderUserId())
                .workOrderId(reqBO.getWorkOrderId())
                .routeId(reqBO.getRouteId())
                .routeVersionId(reqBO.getRouteVersionId())
                .erpFixedQuantitySnapshot(erpFixedQuantity)
                .activeStatus(STATUS_ACTIVE)
                .businessStatus(STATUS_ACTIVE)
                .joinedAt(LocalDateTime.now())
                .version(0)
                .build();
        try {
            activeOrderMapper.insert(activeOrder);
        } catch (DuplicateKeyException ex) {
            MesProcessPoolActiveOrderDO concurrentlyAdded = selectExistingActiveOrder(reqBO);
            if (concurrentlyAdded != null) {
                return concurrentlyAdded.getId();
            }
            throw ex;
        }
        insertProcessSnapshots(activeOrder, erpFixedQuantity, scheduleOrder);
        TeamMaintenanceAuditSupport.insertAudit(auditMapper, reqBO.getLeaderUserId(), "ADD_ACTIVE_ORDER",
                "ACTIVE_ORDER", activeOrder.getId(), null, activeOrder.toString());
        return activeOrder.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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
                .businessStatus(STATUS_REMOVED)
                .removedAt(LocalDateTime.now())
                .version(activeOrder.getVersion())
                .build();
        activeOrderMapper.updateById(update);
        TeamMaintenanceAuditSupport.insertAudit(auditMapper, reqBO.getLeaderUserId(), "REMOVE_ACTIVE_ORDER",
                "ACTIVE_ORDER", activeOrder.getId(), activeOrder.toString(), update.toString());
    }

    private MesProcessPoolActiveOrderDO selectExistingActiveOrder(MesTeamLeaderActiveOrderAddReqBO reqBO) {
        return activeOrderMapper.selectActiveByWorkOrderRouteVersion(reqBO.getWorkOrderId(), reqBO.getRouteId(),
                reqBO.getRouteVersionId());
    }

    @Override
    public List<MesProcessPoolActiveOrderDO> listActiveOrders(Long leaderUserId) {
        if (leaderUserId == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "activeOrderList");
        }
        return activeOrderMapper.selectActiveListByLeader(leaderUserId);
    }

    private MesProScheduleOrderDO requireMatchingScheduleOrder(MesTeamLeaderActiveOrderAddReqBO reqBO) {
        MesProScheduleOrderDO scheduleOrder = scheduleOrderMapper.selectEffectiveByWorkOrderId(reqBO.getWorkOrderId());
        if (scheduleOrder == null || scheduleOrder.getId() == null
                || !Objects.equals(scheduleOrder.getRouteId(), reqBO.getRouteId())
                || !Objects.equals(scheduleOrder.getRouteVersionId(), reqBO.getRouteVersionId())) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, reqBO.getWorkOrderId());
        }
        return scheduleOrder;
    }

    private void insertProcessSnapshots(MesProcessPoolActiveOrderDO activeOrder, BigDecimal erpFixedQuantity,
                                        MesProScheduleOrderDO scheduleOrder) {
        List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots = scheduleOrderProcessMapper
                .selectListByScheduleOrderId(scheduleOrder.getId())
                .stream()
                .filter(process -> Boolean.TRUE.equals(process.getEnabled()))
                .map(process -> toProcessSnapshot(activeOrder, process, erpFixedQuantity))
                .toList();
        if (snapshots.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrder.getId());
        }
        if (!Boolean.TRUE.equals(processSnapshotMapper.insertBatch(snapshots))) {
            throw new IllegalStateException("Failed to insert active order process snapshots");
        }
    }

    private MesProcessPoolActiveOrderProcessSnapshotDO toProcessSnapshot(MesProcessPoolActiveOrderDO activeOrder,
                                                                         MesProScheduleOrderProcessDO process,
                                                                         BigDecimal erpFixedQuantity) {
        if (process == null || process.getRouteProcessId() == null || process.getProcessId() == null) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrder.getId());
        }
        BigDecimal factor = requirePositive(process.getProductionQuantityFactor(), activeOrder.getId())
                .setScale(6, RoundingMode.HALF_UP);
        BigDecimal plannedQuantity = requirePositive(process.getPlannedQuantity(), activeOrder.getId())
                .setScale(6, RoundingMode.HALF_UP);
        BigDecimal expectedPlannedQuantity = erpFixedQuantity.multiply(factor).setScale(6, RoundingMode.HALF_UP);
        if (plannedQuantity.compareTo(expectedPlannedQuantity) != 0) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrder.getId());
        }
        return MesProcessPoolActiveOrderProcessSnapshotDO.builder()
                .activeOrderId(activeOrder.getId())
                .workOrderId(activeOrder.getWorkOrderId())
                .routeId(activeOrder.getRouteId())
                .routeVersionId(activeOrder.getRouteVersionId())
                .routeProcessId(process.getRouteProcessId())
                .processId(process.getProcessId())
                .erpFixedQuantitySnapshot(erpFixedQuantity.setScale(6, RoundingMode.HALF_UP))
                .productionQuantityFactorSnapshot(factor)
                .plannedQuantitySnapshot(plannedQuantity)
                .build();
    }

    private static BigDecimal requirePositive(BigDecimal value, Long activeOrderId) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        return value;
    }
}
