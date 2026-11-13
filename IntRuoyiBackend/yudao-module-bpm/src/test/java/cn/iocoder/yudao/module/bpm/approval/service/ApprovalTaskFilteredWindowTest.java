package cn.iocoder.yudao.module.bpm.approval.service;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterValueUtils;
import cn.iocoder.yudao.module.bpm.approval.core.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class ApprovalTaskFilteredWindowTest {
    @Test void exactCountTraversesFixedWindowsAndKeepsOnlyRequestedMatches() {
        var offsets=new ArrayList<Long>();
        ApprovalTaskFilteredWindow.Source source=(offset,limit)->{
            assertEquals(200,limit);offsets.add(offset);
            var rows=new ArrayList<ApprovalTaskSummary>();
            for(long id=offset;id<Math.min(offset+limit,1003);id++)rows.add(row(id));
            return new PageResult<>(rows,1003L);
        };
        var context=ApprovalTaskQueryContext.of(12L,ApprovalTaskViewType.TODO,ApprovalModuleCode.EDHR,null,1,10);
        var result=ApprovalTaskFilteredWindow.read(List.of(source),context,new ApprovalQueryWindow(90,5),true,
                row->Long.parseLong(row.getSourceTaskId())%7==0);
        assertEquals(144L,result.getTotal());assertEquals(List.of("630","637","644","651","658"),result.getList().stream().map(ApprovalTaskSummary::getSourceTaskId).toList());
        assertEquals(List.of(0L,200L,400L,600L,800L,1000L),offsets);
    }
    @Test void staticSqlCountDoesNotScanBeyondRequestedWindow() {
        var calls=new ArrayList<Long>();
        ApprovalTaskFilteredWindow.Source source=(offset,limit)->{
            calls.add(offset);return new PageResult<>(List.of(row(offset)),1000000L);
        };
        var context=ApprovalTaskQueryContext.of(12L,ApprovalTaskViewType.TODO,ApprovalModuleCode.EDHR,null,1,10);
        var result=ApprovalTaskFilteredWindow.read(List.of(source),context,new ApprovalQueryWindow(0,1),false,row->true);
        assertEquals(1000000L,result.getTotal());assertEquals(List.of(0L),calls);
    }
    @Test void formalOutcomeDisplayFiltersShareOneProofProjectionAndPreserveStoredOpinion() {
        var context=ApprovalTaskQueryContext.of(12L,ApprovalTaskViewType.DONE,ApprovalModuleCode.EDHR,null,1,10);
        var split=ListFilterValueUtils.split("[{\"key\":\"approvalResult\",\"operator\":\"contains\",\"value\":\"审批通过\"},{\"key\":\"approvalRemark\",\"operator\":\"contains\",\"value\":\"正式意见\"}]",
                ApprovalListFilterValues.STATIC_KEYS,ApprovalListFilterValues.dynamicFields(context));
        var approved=row(1).setDecisionState("APPROVE").setApprovalResult(ApprovalTaskReviewResult.APPROVE).setApprovalRemark("  正式意见\n第二行  ");
        assertTrue(split.matches(approved));assertFalse(split.matches(row(2).setDecisionState("UNKNOWN").setApprovalRemark("正式意见")));
        assertTrue(split.hasDynamicFilters());assertNull(split.staticFilters());
    }
    private static ApprovalTaskSummary row(long id){return ApprovalTaskSummary.builder().moduleCode(ApprovalModuleCode.EDHR)
            .sourceTaskType("EDHR_WORK_TASK").sourceTaskId(Long.toString(id)).taskCreatedAt(LocalDateTime.of(2026,1,1,0,0).minusSeconds(id)).build();}
}
