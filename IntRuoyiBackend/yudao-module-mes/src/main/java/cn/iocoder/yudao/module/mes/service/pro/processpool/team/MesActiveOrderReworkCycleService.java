package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

/** Opens a new execution identity from the signed QA disposition, preserving all previous evidence. */
@Service
public class MesActiveOrderReworkCycleService {
    private final MesProcessPoolActiveOrderMapper orderMapper;
    private final MesProcessPoolActiveOrderProcessSnapshotMapper snapshotMapper;
    private final MesPqcInspectionTaskMapper taskMapper;

    public MesActiveOrderReworkCycleService(MesProcessPoolActiveOrderMapper orderMapper,
            MesProcessPoolActiveOrderProcessSnapshotMapper snapshotMapper, MesPqcInspectionTaskMapper taskMapper) {
        this.orderMapper = orderMapper;
        this.snapshotMapper = snapshotMapper;
        this.taskMapper = taskMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long start(Long sourceActiveOrderId, Long workOrderId, Long reviewId, LocalDateTime at) {
        if (sourceActiveOrderId == null || workOrderId == null || reviewId == null || at == null) {
            throw new IllegalStateException("REWORK_FORMAL_SOURCE_REQUIRED");
        }
        var source = orderMapper.selectByIdForUpdate(sourceActiveOrderId);
        if (source == null || !Objects.equals(source.getWorkOrderId(), workOrderId)) {
            throw new IllegalStateException("REWORK_ACTIVE_ORDER_SOURCE_MISMATCH");
        }
        var existing = orderMapper.selectByReworkReviewId(reviewId);
        if (existing != null) {
            if (!Objects.equals(existing.getReworkSourceActiveOrderId(), sourceActiveOrderId)
                    || !Objects.equals(existing.getWorkOrderId(), workOrderId)) {
                throw new IllegalStateException("REWORK_REVIEW_IDEMPOTENCY_CONFLICT");
            }
            return existing.getId();
        }
        if (source.getRouteId() == null || source.getRouteVersionId() == null
                || source.getQaRegulationVersionId() == null || source.getLeaderUserId() == null
                || source.getVersion() == null || source.getErpFixedQuantitySnapshot() == null) {
            throw new IllegalStateException("REWORK_FROZEN_ORDER_CONTEXT_REQUIRED");
        }
        var snapshots = snapshotMapper.selectListByActiveOrderIdForUpdate(sourceActiveOrderId);
        var tasks = taskMapper.selectListByActiveOrderIdForUpdate(sourceActiveOrderId);
        if (snapshots == null || snapshots.isEmpty() || tasks == null || tasks.isEmpty()) {
            throw new IllegalStateException("REWORK_FROZEN_PROCESS_AND_PQC_PLAN_REQUIRED");
        }
        if (orderMapper.retireForRework(sourceActiveOrderId, source.getVersion(), at) != 1) {
            throw new IllegalStateException("REWORK_SOURCE_STATE_CHANGED");
        }
        var next = new MesProcessPoolActiveOrderDO();
        BeanUtils.copyProperties(source, next);
        next.clean();
        next.setId(null).setActiveStatus("ACTIVE").setBusinessStatus("ACTIVE").setVersion(0)
                .setJoinedAt(at).setRemovedAt(null).setReleaseDecisionId(null).setReleasedAt(null).setReleasedBy(null)
                .setReworkSourceActiveOrderId(sourceActiveOrderId).setReworkReviewId(reviewId);
        if (orderMapper.insert(next) != 1 || next.getId() == null) {
            throw new IllegalStateException("REWORK_ORDER_INSERT_FAILED");
        }
        for (var old : snapshots) {
            var snapshot = new MesProcessPoolActiveOrderProcessSnapshotDO();
            BeanUtils.copyProperties(old, snapshot);
            snapshot.clean();
            snapshot.setId(null).setActiveOrderId(next.getId());
            if (snapshotMapper.insert(snapshot) != 1) {
                throw new IllegalStateException("REWORK_PROCESS_SNAPSHOT_INSERT_FAILED");
            }
        }
        for (var old : tasks) {
            var task = new MesPqcInspectionTaskDO();
            BeanUtils.copyProperties(old, task);
            task.clean();
            task.setId(null).setActiveOrderId(next.getId()).setTaskStatus(MesPqcInspectionTaskDO.TASK_STATUS_PENDING)
                    .setBusinessDate(at.toLocalDate()).setActualInspectionQuantity(0)
                    .setSubmittedContentHash(null).setSubmittedEventId(null);
            if (taskMapper.insert(task) != 1) {
                throw new IllegalStateException("REWORK_PQC_PLAN_INSERT_FAILED");
            }
        }
        return next.getId();
    }
}
