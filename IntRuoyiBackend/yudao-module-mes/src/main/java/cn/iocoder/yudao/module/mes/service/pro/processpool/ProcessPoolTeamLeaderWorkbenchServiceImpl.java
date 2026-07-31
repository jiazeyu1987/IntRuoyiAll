package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTeamLeaderWorkbenchRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelineDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelineEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelinePageReqVO;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.List;

@Service
@Validated
public class ProcessPoolTeamLeaderWorkbenchServiceImpl implements ProcessPoolTeamLeaderWorkbenchService {

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_PASS = "PASS";
    private static final String STATUS_FAILURE = "FAILURE";
    private static final String STATUS_FAIL = "FAIL";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PARTIAL = "PARTIAL";
    private static final String STATUS_ALLOCATED = "ALLOCATED";
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_APPROVED = "APPROVED";

    private final ProcessPoolTimelineService timelineService;

    public ProcessPoolTeamLeaderWorkbenchServiceImpl(ProcessPoolTimelineService timelineService) {
        this.timelineService = timelineService;
    }

    @Override
    public ProcessPoolTeamLeaderWorkbenchRespVO getWorkbench(ProcessPoolTimelinePageReqVO reqVO) {
        PageResult<ProcessPoolTimelineEventRespVO> page = timelineService.getTimelinePage(reqVO);
        List<ProcessPoolTimelineEventRespVO> events = page.getList() == null
                ? Collections.emptyList()
                : page.getList();
        return new ProcessPoolTeamLeaderWorkbenchRespVO()
                .setTotal(page.getTotal())
                .setEvents(events)
                .setSummary(buildSummary(events));
    }

    @Override
    public ProcessPoolTimelineDetailRespVO getDetail(Long eventId) {
        return timelineService.getTimelineDetail(eventId);
    }

    private ProcessPoolTeamLeaderWorkbenchRespVO.Summary buildSummary(List<ProcessPoolTimelineEventRespVO> events) {
        return new ProcessPoolTeamLeaderWorkbenchRespVO.Summary()
                .setVisibleEventCount(events.size())
                .setPqcSuccessCount(count(events, event -> isAny(event.getPqcResult(), STATUS_SUCCESS, STATUS_PASS)))
                .setPqcFailureCount(count(events, event -> isAny(event.getPqcResult(), STATUS_FAILURE, STATUS_FAIL)))
                .setFifoPendingCount(count(events, event -> isAny(event.getFifoAllocationStatus(), STATUS_PENDING, STATUS_PARTIAL)))
                .setFifoAllocatedCount(count(events, event -> isAny(event.getFifoAllocationStatus(), STATUS_ALLOCATED)))
                .setAuditCopyPendingCount(count(events, event -> isAny(event.getAuditCopyStatus(), STATUS_PENDING)))
                .setAuditCopySubmittedCount(count(events, event -> isAny(event.getAuditCopyStatus(), STATUS_SUBMITTED, STATUS_APPROVED)))
                .setModifiedRecordCount(count(events, this::hasModificationHistory));
    }

    private int count(List<ProcessPoolTimelineEventRespVO> events, EventPredicate predicate) {
        int result = 0;
        for (ProcessPoolTimelineEventRespVO event : events) {
            if (predicate.test(event)) {
                result++;
            }
        }
        return result;
    }

    private boolean hasModificationHistory(ProcessPoolTimelineEventRespVO event) {
        String summary = event.getModificationHistorySummary();
        return summary != null && !summary.isBlank() && !summary.contains("暂无");
    }

    private boolean isAny(String actual, String... expectedValues) {
        if (actual == null) {
            return false;
        }
        for (String expected : expectedValues) {
            if (expected.equalsIgnoreCase(actual)) {
                return true;
            }
        }
        return false;
    }

    @FunctionalInterface
    private interface EventPredicate {
        boolean test(ProcessPoolTimelineEventRespVO event);
    }

}
