package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING;

/** Reads the dual completion gate from the locked production/PQC source tables. */
@Service
public class MesTeamLeaderActiveOrderCompletionProgressPortImpl
        implements MesTeamLeaderActiveOrderCompletionProgressPort {

    private final MesProcessPoolActiveOrderProcessSnapshotMapper snapshotMapper;
    private final MesProcessPoolReportAllocationMapper allocationMapper;
    private final MesPqcInspectionTaskMapper pqcTaskMapper;

    public MesTeamLeaderActiveOrderCompletionProgressPortImpl(
            MesProcessPoolActiveOrderProcessSnapshotMapper snapshotMapper,
            MesProcessPoolReportAllocationMapper allocationMapper,
            MesPqcInspectionTaskMapper pqcTaskMapper) {
        this.snapshotMapper = snapshotMapper;
        this.allocationMapper = allocationMapper;
        this.pqcTaskMapper = pqcTaskMapper;
    }

    @Override
    public MesTeamLeaderActiveOrderCompletionProgress read(Long leaderUserId,
                                                            MesProcessPoolActiveOrderDO activeOrder) {
        if (activeOrder == null || !Objects.equals(activeOrder.getLeaderUserId(), leaderUserId)) {
            throw sourceMissing(activeOrder, "ACTIVE_ORDER_OWNER");
        }
        List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots =
                snapshotMapper.selectListByActiveOrderIdForUpdate(activeOrder.getId());
        List<MesProcessPoolReportAllocationDO> allocations =
                allocationMapper.selectListByActiveOrderIdForUpdate(activeOrder.getId());
        List<MesPqcInspectionTaskDO> tasks = pqcTaskMapper.selectListByActiveOrderIdForUpdate(activeOrder.getId());
        if (snapshots == null || snapshots.isEmpty() || tasks == null || tasks.isEmpty()) {
            throw sourceMissing(activeOrder, "PRODUCTION_OR_PQC_SNAPSHOT");
        }

        Map<String, BigDecimal> allocatedByProcess = new HashMap<>();
        for (MesProcessPoolReportAllocationDO allocation : allocations == null ? List.<MesProcessPoolReportAllocationDO>of() : allocations) {
            if (allocation == null || !Objects.equals(activeOrder.getId(), allocation.getActiveOrderId())
                    || !Objects.equals(activeOrder.getWorkOrderId(), allocation.getWorkOrderId())
                    || allocation.getRouteProcessId() == null || allocation.getProcessId() == null
                    || allocation.getAllocatedQuantity() == null
                    || allocation.getAllocatedQuantity().signum() < 0) {
                throw sourceMissing(activeOrder, "REPORT_ALLOCATION");
            }
            allocatedByProcess.merge(key(allocation.getRouteProcessId(), allocation.getProcessId()),
                    allocation.getAllocatedQuantity(), BigDecimal::add);
        }
        Map<String, MesProcessPoolActiveOrderProcessSnapshotDO> snapshotsByProcess = new HashMap<>();
        long productionComplete = 0;
        for (MesProcessPoolActiveOrderProcessSnapshotDO snapshot : snapshots) {
            if (snapshot == null || snapshot.getRouteProcessId() == null || snapshot.getProcessId() == null
                    || snapshot.getPlannedQuantitySnapshot() == null
                    || snapshot.getPlannedQuantitySnapshot().signum() <= 0) {
                throw sourceMissing(activeOrder, "PROCESS_TARGET");
            }
            if (snapshotsByProcess.put(key(snapshot.getRouteProcessId(), snapshot.getProcessId()), snapshot) != null) {
                throw sourceMissing(activeOrder, "PROCESS_SNAPSHOT_DUPLICATE");
            }
            BigDecimal allocated = allocatedByProcess.getOrDefault(
                    key(snapshot.getRouteProcessId(), snapshot.getProcessId()), BigDecimal.ZERO);
            if (allocated.compareTo(snapshot.getPlannedQuantitySnapshot()) >= 0) {
                productionComplete++;
            }
        }

        Set<String> matchedTaskKeys = new HashSet<>();
        long inspectionComplete = tasks.stream().filter(Objects::nonNull).peek(task -> {
            if (!Objects.equals(activeOrder.getId(), task.getActiveOrderId())
                    || !Objects.equals(activeOrder.getWorkOrderId(), task.getWorkOrderId())
                    || !Objects.equals(activeOrder.getRouteId(), task.getRouteId())
                    || !Objects.equals(activeOrder.getRouteVersionId(), task.getRouteVersionId())
                    || task.getRouteProcessId() == null || task.getProcessId() == null || task.getId() == null
                    || !snapshotsByProcess.containsKey(key(task.getRouteProcessId(), task.getProcessId()))
                    || !matchedTaskKeys.add(key(task.getRouteProcessId(), task.getProcessId()))) {
                throw sourceMissing(activeOrder, "PQC_TASK");
            }
        }).filter(task -> MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED.equals(task.getTaskStatus())).count();
        if (matchedTaskKeys.size() != snapshotsByProcess.size()) {
            throw sourceMissing(activeOrder, "PQC_TASK_PROCESS_COVERAGE");
        }
        return new MesTeamLeaderActiveOrderCompletionProgress()
                .setProductionProgressPercent(percent(productionComplete, snapshots.size()))
                .setInspectionProgressPercent(percent(inspectionComplete, tasks.size()));
    }

    private static BigDecimal percent(long completed, long total) {
        if (total <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(completed).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 6, RoundingMode.HALF_UP);
    }

    private static String key(Long routeProcessId, Long processId) {
        return routeProcessId + ":" + processId;
    }

    private static RuntimeException sourceMissing(MesProcessPoolActiveOrderDO activeOrder, String field) {
        return exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING,
                activeOrder == null ? null : activeOrder.getId(), field);
    }
}
