package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProductionSubmitSignatureContext;
import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;

/** Original signature identity and allocated viewing identity are independent persisted facts. */
@Component
@RequiredArgsConstructor
public class MesProductionSubmissionReadBinding {
    private final MesProcessPoolActiveOrderMapper activeOrders;
    private final MesProcessPoolActiveOrderProcessSnapshotMapper snapshots;
    private final MesProcessPoolReportAllocationMapper allocations;

    public MesProductionSubmitSignatureContext require(MesProProcessPoolEventDO event, Long targetId) {
        Long tenant = TenantContextHolder.getRequiredTenantId();
        require(event != null && positive(event.getId()) && Objects.equals(event.getTenantId(), tenant)
                && "PRODUCTION_SUBMIT".equals(event.getEventType()) && positive(targetId)
                && positive(event.getWorkOrderId()) && positive(event.getRouteId())
                && positive(event.getRouteProcessId()) && positive(event.getProcessId())
                && event.getEventIdempotencyKey() != null && !event.getEventIdempotencyKey().isBlank());
        var payload = JSON.parseObject(event.getRawPayload());
        require(payload != null && positive(payload.getLong("activeOrderId")));
        Long sourceId = payload.getLong("activeOrderId");
        var source = activeOrders.selectByIdIgnoreDeleted(sourceId);
        var target = activeOrders.selectByIdIgnoreDeleted(targetId);
        require(source != null && target != null && Objects.equals(source.getId(), sourceId)
                && Objects.equals(target.getId(), targetId) && Objects.equals(source.getTenantId(), tenant)
                && Objects.equals(target.getTenantId(), tenant)
                && positive(target.getWorkOrderId())
                && Objects.equals(source.getWorkOrderId(), event.getWorkOrderId())
                && Objects.equals(source.getRouteId(), event.getRouteId())
                && Objects.equals(target.getRouteId(), event.getRouteId()));
        var originalSnapshot = snapshots.selectByActiveOrderAndProcess(sourceId,
                event.getRouteProcessId(), event.getProcessId());
        require(originalSnapshot != null && Objects.equals(originalSnapshot.getTenantId(), tenant)
                && Objects.equals(originalSnapshot.getActiveOrderId(), sourceId)
                && Objects.equals(originalSnapshot.getWorkOrderId(), event.getWorkOrderId())
                && Objects.equals(originalSnapshot.getRouteId(), event.getRouteId())
                && Objects.equals(originalSnapshot.getRouteProcessId(), event.getRouteProcessId())
                && Objects.equals(originalSnapshot.getProcessId(), event.getProcessId()));
        boolean bound = false;
        for (var allocation : allocations.selectListByEventId(event.getId())) {
            if (!Objects.equals(allocation.getActiveOrderId(), targetId)) continue;
            require(positive(allocation.getId()) && positive(allocation.getRouteProcessId())
                    && Objects.equals(allocation.getTenantId(), tenant)
                    && Objects.equals(allocation.getEventId(), event.getId())
                    && Objects.equals(allocation.getWorkOrderId(), target.getWorkOrderId())
                    && Objects.equals(allocation.getProcessId(), event.getProcessId())
                    && MesProcessPoolReportAllocationDO.LIFECYCLE_CURRENT.equals(allocation.getLifecycleStatus())
                    && allocation.getAllocatedQuantity() != null
                    && allocation.getAllocatedQuantity().compareTo(BigDecimal.ZERO) > 0);
            var snapshot = snapshots.selectByActiveOrderAndProcess(targetId,
                    allocation.getRouteProcessId(), allocation.getProcessId());
            require(snapshot != null && Objects.equals(snapshot.getTenantId(), tenant)
                    && Objects.equals(snapshot.getActiveOrderId(), targetId)
                    && Objects.equals(snapshot.getWorkOrderId(), target.getWorkOrderId())
                    && Objects.equals(snapshot.getRouteId(), event.getRouteId())
                    && Objects.equals(snapshot.getRouteProcessId(), allocation.getRouteProcessId())
                    && Objects.equals(snapshot.getProcessId(), event.getProcessId()));
            bound = true;
        }
        require(bound);
        return new MesProductionSubmitSignatureContext(sourceId, event.getRouteProcessId(),
                event.getProcessId(), event.getEventIdempotencyKey());
    }

    private static boolean positive(Long value) { return value != null && value > 0; }
    private static void require(boolean valid) {
        if (!valid) throw new IllegalStateException("MES_PRODUCTION_SUBMISSION_READ_BINDING_INVALID");
    }
}
