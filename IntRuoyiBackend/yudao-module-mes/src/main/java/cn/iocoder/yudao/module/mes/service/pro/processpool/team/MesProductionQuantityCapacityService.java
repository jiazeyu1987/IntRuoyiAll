package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MesProductionQuantityCapacityService {
    private final MesProcessPoolActiveOrderMapper orders;
    private final MesProcessPoolActiveOrderProcessSnapshotMapper snapshots;
    private final MesProcessPoolReportAllocationMapper allocations;
    private final MesProProcessPoolEventMapper events;

    public record Capacity(MesProcessPoolActiveOrderDO order, BigDecimal previous,
                           BigDecimal requested, BigDecimal projected, BigDecimal upper) {
        public boolean exceeded() { return projected.compareTo(upper) > 0; }
    }

    /** Called inside the submit transaction, under the common GxP ledger lock. */
    public Capacity evaluate(MesProFrontlineFeedbackSubmitReqVO request) {
        var context = request.getProcessPoolContext();
        var order = orders.selectByIdForUpdate(context.getActiveOrderId());
        if (order == null || !"ACTIVE".equals(order.getActiveStatus())
                || !Objects.equals(order.getWorkOrderId(), context.getWorkOrderId())) {
            throw new IllegalStateException("提交目标不是当前可生产的活跃订单");
        }
        var snapshot = snapshots.selectByActiveOrderAndProcess(order.getId(), context.getRouteProcessId(),
                context.getProcessId());
        var range = MesProductionQuantityRange.of(snapshot);
        var rows = allocations.selectListByActiveOrderIdsAndProcessForUpdate(List.of(order.getId()),
                context.getProcessId()).stream()
                .filter(row -> Objects.equals(row.getRouteProcessId(), context.getRouteProcessId())).toList();
        var eventIds = rows.stream().map(row -> row.getEventId()).distinct().toList();
        var sourceEvents = eventIds.isEmpty() ? List.<cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO>of()
                : events.selectProductionSubmitsByIdsForUpdate(eventIds);
        BigDecimal previous = BigDecimal.ZERO;
        BigDecimal projected = BigDecimal.ZERO;
        var totals = MesOutputMaterialProgressCalculator.cumulativeOutputQuantities(order, snapshot, sourceEvents, rows);
        if (totals.isEmpty()) {
            previous = rows.stream().map(row -> row.getAllocatedQuantity()).reduce(BigDecimal.ZERO, BigDecimal::add);
            projected = previous.add(request.getFeedbackPayload().getOutputQuantity());
        } else {
            for (var total : totals.entrySet()) {
                BigDecimal requested = request.getMaterialDetails().stream()
                        .filter(detail -> Objects.equals(detail.getMaterialId(), total.getKey()))
                        .map(detail -> detail.getOutputQuantity()).reduce(BigDecimal.ZERO, BigDecimal::add);
                previous = previous.max(total.getValue());
                projected = projected.max(total.getValue().add(requested));
            }
        }
        return new Capacity(order, previous, request.getFeedbackPayload().getOutputQuantity(), projected, range.upper());
    }
}
