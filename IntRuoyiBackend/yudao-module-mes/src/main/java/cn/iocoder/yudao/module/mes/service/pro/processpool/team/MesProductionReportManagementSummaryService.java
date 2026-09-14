package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class MesProductionReportManagementSummaryService {

    private final MesProProcessPoolEventMapper eventMapper;
    private final MesProcessPoolReportAllocationMapper allocationMapper;
    private final MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper;
    private final MesReportAllocationPoolQuantityService poolQuantityService;
    private final MesReportAllocationReleaseStateService releaseStateService;

    public MesProductionReportManagementSummaryService(
            MesProProcessPoolEventMapper eventMapper,
            MesProcessPoolReportAllocationMapper allocationMapper,
            MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper,
            MesReportAllocationPoolQuantityService poolQuantityService,
            MesReportAllocationReleaseStateService releaseStateService) {
        this.eventMapper = eventMapper;
        this.allocationMapper = allocationMapper;
        this.releaseApplicationMapper = releaseApplicationMapper;
        this.poolQuantityService = poolQuantityService;
        this.releaseStateService = releaseStateService;
    }

    public void initializeProductionEvent(MesProProcessPoolEventDO event) {
        if (!isProductionEvent(event)) {
            return;
        }
        BigDecimal output = poolQuantityService.requireSubmittedOutputQuantity(event);
        applySummary(event, output, List.of(), Set.of());
    }

    public void refreshProductionEvent(MesProProcessPoolEventDO event) {
        if (!isProductionEvent(event)) {
            return;
        }
        List<MesProcessPoolReportAllocationDO> allocations = allocationMapper.selectListByEventId(event.getId());
        Set<Long> activeOrderIds = activeOrderIds(allocations);
        Set<Long> releasedActiveOrderIds = releaseStateService.findReleasedActiveOrderIds(activeOrderIds);
        BigDecimal output = poolQuantityService.requirePoolQuantity(event);
        applySummary(event, output, allocations, releasedActiveOrderIds);
        updateSummary(event);
    }

    public void refreshByReleaseTransactionId(Long releaseTransactionId) {
        for (Long eventId : listEventIdsByReleaseTransactionId(releaseTransactionId)) {
            MesProProcessPoolEventDO event = eventMapper.selectByIdForUpdate(eventId);
            if (event == null) {
                throw new IllegalStateException("Missing production report event: " + eventId);
            }
            refreshProductionEvent(event);
        }
    }

    public void lockProductionEventsByReleaseTransactionId(Long releaseTransactionId) {
        for (Long eventId : listEventIdsByReleaseTransactionId(releaseTransactionId)) {
            if (eventMapper.selectByIdForUpdate(eventId) == null) {
                throw new IllegalStateException("Missing production report event: " + eventId);
            }
        }
    }

    private List<Long> listEventIdsByReleaseTransactionId(Long releaseTransactionId) {
        List<Long> activeOrderIds = releaseApplicationMapper.selectListByReleaseTransactionId(releaseTransactionId)
                .stream()
                .map(MesProcessPoolActiveOrderReleaseApplicationDO::getActiveOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (activeOrderIds.isEmpty()) {
            return List.of();
        }
        return allocationMapper.selectListByActiveOrderIds(activeOrderIds).stream()
                .map(MesProcessPoolReportAllocationDO::getEventId)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }

    private void applySummary(MesProProcessPoolEventDO event, BigDecimal output,
                              List<MesProcessPoolReportAllocationDO> allocations,
                              Set<Long> releasedActiveOrderIds) {
        BigDecimal allocated = allocations.stream()
                .map(MesProcessPoolReportAllocationDO::getAllocatedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unallocated = output.subtract(allocated).max(BigDecimal.ZERO);
        Set<Long> activeOrderIds = activeOrderIds(allocations);
        String releaseStatus = resolveReleaseStatus(activeOrderIds, releasedActiveOrderIds);
        String managementStatus = resolveManagementStatus(output, allocated, releaseStatus);
        event.setReportOutputQuantity(output)
                .setReportAllocatedQuantity(allocated)
                .setReportUnallocatedQuantity(unallocated)
                .setReportReleaseStatus(releaseStatus)
                .setReportManagementStatus(managementStatus);
    }

    private void updateSummary(MesProProcessPoolEventDO event) {
        MesProProcessPoolEventDO update = new MesProProcessPoolEventDO()
                .setId(event.getId())
                .setReportOutputQuantity(event.getReportOutputQuantity())
                .setReportAllocatedQuantity(event.getReportAllocatedQuantity())
                .setReportUnallocatedQuantity(event.getReportUnallocatedQuantity())
                .setReportReleaseStatus(event.getReportReleaseStatus())
                .setReportManagementStatus(event.getReportManagementStatus());
        if (eventMapper.updateById(update) != 1) {
            throw new IllegalStateException("Failed to update production report management summary: " + event.getId());
        }
    }

    private static Set<Long> activeOrderIds(Collection<MesProcessPoolReportAllocationDO> allocations) {
        return allocations.stream()
                .map(MesProcessPoolReportAllocationDO::getActiveOrderId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private static String resolveReleaseStatus(Set<Long> activeOrderIds, Set<Long> releasedActiveOrderIds) {
        if (activeOrderIds.isEmpty()) {
            return MesProProcessPoolEventDO.REPORT_RELEASE_STATUS_NOT_ALLOCATED;
        }
        long releasedCount = activeOrderIds.stream().filter(releasedActiveOrderIds::contains).count();
        if (releasedCount == 0) {
            return MesProProcessPoolEventDO.REPORT_RELEASE_STATUS_NOT_RELEASED;
        }
        if (releasedCount < activeOrderIds.size()) {
            return MesProProcessPoolEventDO.REPORT_RELEASE_STATUS_PARTIALLY_RELEASED;
        }
        return MesProProcessPoolEventDO.REPORT_RELEASE_STATUS_RELEASED;
    }

    private static String resolveManagementStatus(BigDecimal output, BigDecimal allocated, String releaseStatus) {
        if (allocated.compareTo(BigDecimal.ZERO) == 0) {
            return MesProProcessPoolEventDO.REPORT_MANAGEMENT_STATUS_UNALLOCATED;
        }
        if (allocated.compareTo(output) < 0) {
            return MesProProcessPoolEventDO.REPORT_MANAGEMENT_STATUS_PARTIALLY_ALLOCATED;
        }
        if (MesProProcessPoolEventDO.REPORT_RELEASE_STATUS_RELEASED.equals(releaseStatus)) {
            return MesProProcessPoolEventDO.REPORT_MANAGEMENT_STATUS_ARCHIVED;
        }
        return MesProProcessPoolEventDO.REPORT_MANAGEMENT_STATUS_PENDING_RELEASE;
    }

    private static boolean isProductionEvent(MesProProcessPoolEventDO event) {
        return event != null && Objects.equals(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT,
                event.getEventType());
    }
}
