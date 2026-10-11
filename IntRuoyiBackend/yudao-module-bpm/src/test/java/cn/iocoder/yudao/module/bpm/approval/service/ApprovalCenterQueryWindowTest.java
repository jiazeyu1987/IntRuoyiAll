package cn.iocoder.yudao.module.bpm.approval.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.approval.core.*;
import cn.iocoder.yudao.module.bpm.approval.service.provider.*;
import cn.iocoder.yudao.module.bpm.approval.service.signature.ApprovalSignatureRecordService;
import cn.iocoder.yudao.module.bpm.controller.admin.approval.ApprovalTaskPageReqVO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Exercises the public center method with explicit provider contracts; not real-provider prefix proof. */
class ApprovalCenterQueryWindowTest {
    private static final LocalDateTime NOW = LocalDateTime.parse("2026-10-07T12:00:00.123456");

    @ParameterizedTest
    @EnumSource(ApprovalTaskViewType.class)
    void eachViewUsesItsFormalTimeAndMarksMissingWithoutBorrowingOtherTimes(ApprovalTaskViewType view) throws Exception {
        var recent=row(ApprovalModuleCode.EDHR,"TASK","recent").setTaskCreatedAt(NOW.minusYears(5))
                .setTaskCompletedAt(NOW.minusYears(5)).setInitiatedAt(NOW.minusYears(5));
        var earlier=row(ApprovalModuleCode.EDHR,"TASK","earlier").setTaskCreatedAt(NOW.plusYears(5))
                .setTaskCompletedAt(NOW.plusYears(5)).setInitiatedAt(NOW.plusYears(5));
        var missing=row(ApprovalModuleCode.EDHR,"TASK","missing").setTaskCreatedAt(NOW.plusYears(10))
                .setTaskCompletedAt(NOW.plusYears(10)).setInitiatedAt(NOW.plusYears(10));
        setPrimary(recent,view,NOW); setPrimary(earlier,view,NOW.minusNanos(1000)); setPrimary(missing,view,null);
        var provider=provider(ApprovalModuleCode.EDHR,List.of(missing,earlier,recent));
        var center=center(provider);
        var page=center.getTaskPage(12L,new ApprovalTaskQuery().setViewType(view));
        assertEquals(List.of("recent","earlier","missing"),page.getList().stream().map(ApprovalTaskSummary::getSourceTaskId).toList());
        assertFalse(page.getList().get(0).getSortTimeMissing()); assertTrue(page.getList().get(2).getSortTimeMissing());
        assertEquals(NOW,primary(recent,view)); assertNull(primary(missing,view));
        var json=new ObjectMapper().findAndRegisterModules().valueToTree(page);
        assertFalse(json.path("list").get(0).path("sortTimeMissing").asBoolean());
        assertTrue(json.path("list").get(2).path("sortTimeMissing").asBoolean());
    }

    @Test
    void equalTimesUseModuleThenTypeThenUtf8IdAcrossPagesIncludingSupplementaryCharacters() {
        String supplementary="\uD800\uDC00", bmp="\uE000"; // UTF-16 and UTF-8 compare in opposite orders.
        var edhr=provider(ApprovalModuleCode.EDHR,List.of(row(ApprovalModuleCode.EDHR,"A","10"),
                row(ApprovalModuleCode.EDHR,"A","2"),row(ApprovalModuleCode.EDHR,"A",bmp),
                row(ApprovalModuleCode.EDHR,"A",supplementary),row(ApprovalModuleCode.EDHR,"B","1")));
        var bpm=provider(ApprovalModuleCode.BPM,List.of(row(ApprovalModuleCode.BPM,"Z","z")));
        var center=center(edhr,bpm);
        var first=center.getTaskPage(12L,new ApprovalTaskQuery().setPageSize(3));
        var second=center.getTaskPage(12L,new ApprovalTaskQuery().setPageNo(2).setPageSize(3));
        assertEquals(List.of("z","10","2"),first.getList().stream().map(ApprovalTaskSummary::getSourceTaskId).toList());
        assertEquals(List.of(bmp,supplementary,"1"),second.getList().stream().map(ApprovalTaskSummary::getSourceTaskId).toList());
        assertEquals(6L,first.getTotal()); assertEquals(first.getTotal(),second.getTotal());
    }

    @Test
    void boundedInternalCursorReturnsTheEntireSecondHttpPage() {
        var rows=new ArrayList<ApprovalTaskSummary>();
        for(int i=0;i<450;i++) rows.add(row(ApprovalModuleCode.EDHR,"TASK",Integer.toString(i)).setTaskCreatedAt(NOW.minusSeconds(i)));
        var provider=provider(ApprovalModuleCode.EDHR,rows);
        doAnswer(call -> {
            ApprovalTaskQueryContext context=call.getArgument(0);
            ApprovalQueryWindow window=call.getArgument(1);
            assertEquals(1,context.getPageNo()); assertEquals(200,context.getPageSize());
            assertEquals(200L,window.limit()); assertEquals(0L,window.offset()%200);
            assertEquals("batch",context.getKeyword()); assertEquals(12L,context.getLoginUserId());
            return window.slice(rows);
        }).when(provider).window(any(),any());
        var request=new ApprovalTaskPageReqVO().setPageNo(2).setPageSize(200).setKeyword("batch");
        try(var factory=Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(request).isEmpty());
            assertFalse(factory.getValidator().validate(new ApprovalTaskPageReqVO().setPageSize(400)).isEmpty());
        }
        var page=center(provider).getTaskPage(12L,request.toQuery());
        assertEquals(200,page.getList().size()); assertEquals("200",page.getList().get(0).getSourceTaskId());
        assertEquals("399",page.getList().get(199).getSourceTaskId()); assertEquals(450L,page.getTotal());
        assertEquals(200,request.getPageSize());
    }

    @Test
    void longWindowBeyondIntegerRangeReturnsExactEmptyTailForAllAndSingleModule() {
        var provider=provider(ApprovalModuleCode.EDHR,List.of());
        var center=center(provider);
        for(ApprovalModuleCode module:Arrays.asList(null,ApprovalModuleCode.EDHR)) {
            var page=center.getTaskPage(12L,new ApprovalTaskQuery().setModuleCode(module)
                    .setPageNo(Integer.MAX_VALUE).setPageSize(200));
            assertTrue(page.getList().isEmpty()); assertEquals(0L,page.getTotal());
        }
        verify(provider,times(2)).window(any(),eq(new ApprovalQueryWindow(0,200)));
        verify(provider,never()).page(any());
    }

    @Test
    void supportedVeryLargeWindowProducesEmptyTailWithExactKAndNoIntegerSliceOverflow() {
        var provider=provider(ApprovalModuleCode.EDHR,List.of(row(ApprovalModuleCode.EDHR,"TASK","1")));
        var page=center(provider).getTaskPage(12L,new ApprovalTaskQuery().setPageNo(Integer.MAX_VALUE).setPageSize(1));
        assertTrue(page.getList().isEmpty()); assertEquals(1L,page.getTotal());
        verify(provider).window(any(),eq(new ApprovalQueryWindow(0,200)));
    }

    @Test
    void singleModuleUsesTheSameDoneTupleAndMissingResponseFlag() {
        var missing=row(ApprovalModuleCode.EDHR,"TASK","2").setTaskCompletedAt(null).setTaskCreatedAt(NOW.plusYears(1));
        var completed=row(ApprovalModuleCode.EDHR,"TASK","10").setTaskCompletedAt(NOW).setTaskCreatedAt(NOW.minusYears(1));
        var provider=provider(ApprovalModuleCode.EDHR,List.of(missing,completed));
        var page=center(provider).getTaskPage(12L,new ApprovalTaskQuery().setModuleCode(ApprovalModuleCode.EDHR).setViewType(ApprovalTaskViewType.DONE));
        assertEquals(List.of("10","2"),page.getList().stream().map(ApprovalTaskSummary::getSourceTaskId).toList());
        assertFalse(completed.getSortTimeMissing()); assertTrue(missing.getSortTimeMissing());
        verify(provider).window(argThat(context -> context.getPageNo()==1 && context.getPageSize()==200),eq(new ApprovalQueryWindow(0,200)));
    }

    @Test
    void invalidPageNumbersAndSizesFailInsteadOfChangingTheRequest() {
        var provider=provider(ApprovalModuleCode.EDHR,List.of()); var center=center(provider);
        assertThrows(IllegalArgumentException.class,() -> center.getTaskPage(12L,new ApprovalTaskQuery().setPageNo(0)));
        assertThrows(IllegalArgumentException.class,() -> center.getTaskPage(12L,new ApprovalTaskQuery().setPageSize(-1)));
        verify(provider,never()).window(any(),any());
    }

    private static ApprovalCenterServiceImpl center(ApprovalTaskProvider... providers) {
        return new ApprovalCenterServiceImpl(new ApprovalTaskProviderRegistry(List.of(providers)),
                mock(PermissionApi.class),mock(AdminUserApi.class),mock(ApprovalSignatureRecordService.class));
    }
    private static ApprovalTaskProvider provider(ApprovalModuleCode module,List<ApprovalTaskSummary> rows) {
        var provider=mock(ApprovalTaskProvider.class);
        when(provider.getModuleCode()).thenReturn(module); when(provider.isVisibleTo(any())).thenReturn(true);
        when(provider.getSupportedViewTypes()).thenReturn(EnumSet.allOf(ApprovalTaskViewType.class));
        when(provider.window(any(),any())).thenAnswer(call -> {
            ApprovalTaskQueryContext context=call.getArgument(0);
            ApprovalQueryWindow window=call.getArgument(1);
            var ordered=ApprovalCenterServiceImpl.sortRows(rows,context.getViewType());
            return window.slice(ordered);
        });
        return provider;
    }
    private static ApprovalTaskSummary row(ApprovalModuleCode module,String type,String id) {
        return ApprovalTaskSummary.builder().id(module+":"+type+":"+id).moduleCode(module).sourceTaskType(type).sourceTaskId(id)
                .taskCreatedAt(NOW).taskCompletedAt(NOW).initiatedAt(NOW).build();
    }
    private static void setPrimary(ApprovalTaskSummary row,ApprovalTaskViewType view,LocalDateTime value) {
        switch(view) {
            case TODO,CC -> row.setTaskCreatedAt(value);
            case DONE -> row.setTaskCompletedAt(value);
            case MY_INITIATED -> row.setInitiatedAt(value);
        }
    }
    private static LocalDateTime primary(ApprovalTaskSummary row,ApprovalTaskViewType view) {
        return switch(view) {
            case TODO,CC -> row.getTaskCreatedAt();
            case DONE -> row.getTaskCompletedAt();
            case MY_INITIATED -> row.getInitiatedAt();
        };
    }
}
