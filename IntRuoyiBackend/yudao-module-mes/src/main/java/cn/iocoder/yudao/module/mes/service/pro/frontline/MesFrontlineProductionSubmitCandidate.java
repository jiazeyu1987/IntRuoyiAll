package cn.iocoder.yudao.module.mes.service.pro.frontline;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Objects;

public record MesFrontlineProductionSubmitCandidate(Long eventId,
                                                     LocalDateTime serverSubmitTime,
                                                     Long activeOrderId,
                                                     Long routeProcessId,
                                                     Long processId) {

    public MesFrontlineProductionSubmitCandidate(Long eventId, LocalDateTime serverSubmitTime) {
        this(eventId, serverSubmitTime, null, null, null);
    }

    public static MesFrontlineProductionSubmitCandidate requireActiveOrderProcessSnapshot(
            Long eventId,
            LocalDateTime serverSubmitTime,
            Long activeOrderId,
            Long routeProcessId,
            Long processId,
            Collection<ActiveOrderProcessSnapshot> snapshots) {
        boolean backedBySnapshot = snapshots != null && snapshots.stream()
                .filter(Objects::nonNull)
                .anyMatch(snapshot -> Objects.equals(activeOrderId, snapshot.activeOrderId())
                        && Objects.equals(routeProcessId, snapshot.routeProcessId())
                        && Objects.equals(processId, snapshot.processId()));
        if (!backedBySnapshot) {
            throw new IllegalArgumentException("production submit event is not backed by active-order process snapshot: "
                    + "eventId=" + eventId + ", activeOrderId=" + activeOrderId
                    + ", routeProcessId=" + routeProcessId + ", processId=" + processId);
        }
        return new MesFrontlineProductionSubmitCandidate(eventId, serverSubmitTime,
                activeOrderId, routeProcessId, processId);
    }

    public boolean belongsToSnapshot(Collection<ActiveOrderProcessSnapshot> snapshots) {
        return snapshots != null && snapshots.stream()
                .filter(Objects::nonNull)
                .anyMatch(snapshot -> Objects.equals(activeOrderId, snapshot.activeOrderId())
                        && Objects.equals(routeProcessId, snapshot.routeProcessId())
                        && Objects.equals(processId, snapshot.processId()));
    }

    public record ActiveOrderProcessSnapshot(Long activeOrderId,
                                             Long routeProcessId,
                                             Long processId) {
    }
}
