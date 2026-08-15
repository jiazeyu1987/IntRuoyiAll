package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolOrderProcessCompletionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

@Component
public class MesTeamLeaderActiveOrderReleaseSourceSnapshotHasher {

    public static final String VERSION = "PQC_RELEASE_SOURCE_V1";

    public String hash(Input input) {
        Objects.requireNonNull(input, "source snapshot input is required");
        Map<String, Object> root = map();
        root.put("version", VERSION);
        root.put("tenantId", input.tenantId());
        root.put("activeOrder", activeOrder(input.activeOrder()));
        root.put("workOrder", workOrder(input.workOrder()));
        root.put("processSnapshots", processSnapshots(input.processSnapshots()));
        root.put("productionCompletions", productionCompletions(input.productionCompletions()));
        root.put("inspectionTasks", inspectionTasks(input.inspectionTasks()));
        root.put("inspectionDetails", inspectionDetails(input.inspectionDetails()));
        return DigestUtil.sha256Hex(JsonUtils.toJsonString(root));
    }

    private Map<String, Object> activeOrder(MesProcessPoolActiveOrderDO activeOrder) {
        Objects.requireNonNull(activeOrder, "active order is required");
        Map<String, Object> value = map();
        value.put("id", activeOrder.getId());
        value.put("leaderUserId", activeOrder.getLeaderUserId());
        value.put("workOrderId", activeOrder.getWorkOrderId());
        value.put("routeId", activeOrder.getRouteId());
        value.put("routeVersionId", activeOrder.getRouteVersionId());
        value.put("activeStatus", activeOrder.getActiveStatus());
        value.put("businessStatus", activeOrder.getBusinessStatus());
        value.put("erpFixedQuantity", decimal(activeOrder.getErpFixedQuantitySnapshot()));
        return value;
    }

    private Map<String, Object> workOrder(MesProWorkOrderDO workOrder) {
        Objects.requireNonNull(workOrder, "work order is required");
        Map<String, Object> value = map();
        value.put("id", workOrder.getId());
        value.put("code", workOrder.getCode());
        value.put("productId", workOrder.getProductId());
        value.put("batchCode", workOrder.getBatchCode());
        value.put("quantity", decimal(workOrder.getQuantity()));
        return value;
    }

    private List<Map<String, Object>> processSnapshots(
            List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots) {
        return List.copyOf(snapshots).stream()
                .sorted(Comparator.comparing(MesProcessPoolActiveOrderProcessSnapshotDO::getRouteProcessId)
                        .thenComparing(MesProcessPoolActiveOrderProcessSnapshotDO::getProcessId)
                        .thenComparing(MesProcessPoolActiveOrderProcessSnapshotDO::getId))
                .map(snapshot -> {
                    Map<String, Object> value = map();
                    value.put("id", snapshot.getId());
                    value.put("activeOrderId", snapshot.getActiveOrderId());
                    value.put("workOrderId", snapshot.getWorkOrderId());
                    value.put("routeId", snapshot.getRouteId());
                    value.put("routeVersionId", snapshot.getRouteVersionId());
                    value.put("routeProcessId", snapshot.getRouteProcessId());
                    value.put("processId", snapshot.getProcessId());
                    return value;
                }).toList();
    }

    private List<Map<String, Object>> productionCompletions(
            List<MesProcessPoolOrderProcessCompletionDO> completions) {
        return List.copyOf(completions).stream()
                .sorted(Comparator.comparing(MesProcessPoolOrderProcessCompletionDO::getRouteProcessId)
                        .thenComparing(MesProcessPoolOrderProcessCompletionDO::getProcessId)
                        .thenComparing(MesProcessPoolOrderProcessCompletionDO::getId))
                .map(completion -> {
                    Map<String, Object> value = map();
                    value.put("id", completion.getId());
                    value.put("workOrderId", completion.getWorkOrderId());
                    value.put("routeProcessId", completion.getRouteProcessId());
                    value.put("processId", completion.getProcessId());
                    value.put("targetQuantity", decimal(completion.getTargetQuantity()));
                    value.put("confirmedQuantity", decimal(completion.getConfirmedQuantity()));
                    value.put("completionStatus", completion.getCompletionStatus());
                    value.put("completedAt", time(completion.getCompletedAt()));
                    value.put("backfillStatus", completion.getBackfillStatus());
                    value.put("backfillExecutionId", completion.getBackfillExecutionId());
                    value.put("lastEventId", completion.getLastEventId());
                    value.put("lastReviewId", completion.getLastReviewId());
                    value.put("sourceEventIdsJson", completion.getSourceEventIdsJson());
                    value.put("sourceAllocationIdsJson", completion.getSourceAllocationIdsJson());
                    value.put("aggregateHash", completion.getAggregateHash());
                    value.put("backfillIdempotencyKey", completion.getBackfillIdempotencyKey());
                    return value;
                }).toList();
    }

    private List<Map<String, Object>> inspectionTasks(List<MesPqcInspectionTaskDO> tasks) {
        return List.copyOf(tasks).stream()
                .sorted(Comparator.comparing(MesPqcInspectionTaskDO::getRouteProcessId)
                        .thenComparing(MesPqcInspectionTaskDO::getProcessId)
                        .thenComparing(MesPqcInspectionTaskDO::getId))
                .map(task -> {
                    Map<String, Object> value = map();
                    value.put("id", task.getId());
                    value.put("activeOrderId", task.getActiveOrderId());
                    value.put("workOrderId", task.getWorkOrderId());
                    value.put("routeId", task.getRouteId());
                    value.put("routeVersionId", task.getRouteVersionId());
                    value.put("routeProcessId", task.getRouteProcessId());
                    value.put("processId", task.getProcessId());
                    value.put("taskStatus", task.getTaskStatus());
                    return value;
                }).toList();
    }

    private List<Map<String, Object>> inspectionDetails(
            List<MesPqcProcessInspectionAggregateDetailDO> details) {
        return List.copyOf(details).stream()
                .sorted(Comparator.comparing(MesPqcProcessInspectionAggregateDetailDO::getRouteProcessId)
                        .thenComparing(MesPqcProcessInspectionAggregateDetailDO::getProcessId)
                        .thenComparing(MesPqcProcessInspectionAggregateDetailDO::getId))
                .map(detail -> {
                    Map<String, Object> value = map();
                    value.put("id", detail.getId());
                    value.put("pqcTaskId", detail.getPqcTaskId());
                    value.put("activeOrderId", detail.getActiveOrderId());
                    value.put("workOrderId", detail.getWorkOrderId());
                    value.put("routeId", detail.getRouteId());
                    value.put("routeVersionId", detail.getRouteVersionId());
                    value.put("routeProcessId", detail.getRouteProcessId());
                    value.put("processId", detail.getProcessId());
                    return value;
                }).toList();
    }

    private Map<String, Object> map() {
        return new TreeMap<>();
    }

    private String decimal(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private String time(LocalDateTime value) {
        return value == null ? null : value.truncatedTo(ChronoUnit.SECONDS).toString();
    }

    public record Input(
            Long tenantId,
            MesProcessPoolActiveOrderDO activeOrder,
            MesProWorkOrderDO workOrder,
            List<MesProcessPoolActiveOrderProcessSnapshotDO> processSnapshots,
            List<MesProcessPoolOrderProcessCompletionDO> productionCompletions,
            List<MesPqcInspectionTaskDO> inspectionTasks,
            List<MesPqcProcessInspectionAggregateDetailDO> inspectionDetails) {

        public Input {
            Objects.requireNonNull(tenantId, "tenantId is required");
            Objects.requireNonNull(activeOrder, "activeOrder is required");
            Objects.requireNonNull(workOrder, "workOrder is required");
            processSnapshots = List.copyOf(processSnapshots);
            productionCompletions = List.copyOf(productionCompletions);
            inspectionTasks = List.copyOf(inspectionTasks);
            inspectionDetails = List.copyOf(inspectionDetails);
        }
    }
}
