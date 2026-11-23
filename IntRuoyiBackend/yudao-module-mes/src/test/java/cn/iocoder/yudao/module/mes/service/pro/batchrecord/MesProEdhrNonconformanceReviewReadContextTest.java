package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderDetail;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderDetailService;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.handoff.MesActiveOrderHandoffTaskMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.MesActiveOrderHandoffTaskDO;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesProEdhrNonconformanceReviewReadContextTest {
    @Mock MesProEdhrNonconformanceReviewMapper reviewMapper;
    @Mock MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock MesTeamLeaderActiveOrderDetailService activeOrderDetailService;
    @Mock MesActiveOrderHandoffTaskMapper summaryHandoffTaskMapper;
    @Mock AdminUserService summaryUserService;
    @Mock MesMarketReleaseAccess summaryMarketReleaseAccess;
    @InjectMocks MesProEdhrNonconformanceReviewServiceImpl service;
    @BeforeEach void tenant() { TenantContextHolder.setTenantId(1L); }
    @AfterEach void clear() { TenantContextHolder.clear(); }
    private MesProEdhrNonconformanceReviewDO review() {
        return new MesProEdhrNonconformanceReviewDO().setId(501L).setActiveOrderId(409L).setWorkOrderId(201L).setTenantId(1L);
    }
    private MesProcessPoolActiveOrderDO archivedOrder() {
        var order = new MesProcessPoolActiveOrderDO().setId(409L).setWorkOrderId(201L).setBusinessStatus("REWORKED");
        order.setTenantId(1L);
        order.setDeleted(true);
        return order;
    }
    @Test void replacedCycleRemainsReadOnlyAndUsesArchivedProjection() {
        when(reviewMapper.selectById(501L)).thenReturn(review());
        when(activeOrderMapper.selectByIdIgnoreDeleted(409L)).thenReturn(archivedOrder());
        var detail = new MesTeamLeaderActiveOrderDetail().setActiveOrderId(409L);
        when(activeOrderDetailService.getArchivedFormalDetail(409L)).thenReturn(detail);
        when(activeOrderDetailService.getArchivedSignatureDetail(409L, 742L)).thenReturn(detail);
        assertSame(detail, service.getActiveOrderDetail(501L));
        assertSame(detail, service.getActiveOrderSignatureDetail(501L, 742L));
        verify(activeOrderDetailService, never()).getFormalDetail(anyLong());
        verify(activeOrderDetailService, never()).getDetail(anyLong(), anyLong());
    }
    @Test void wrongWorkOrderOrTenantIsRejectedBeforeDetailRead() {
        when(reviewMapper.selectById(501L)).thenReturn(review());
        var order = archivedOrder().setWorkOrderId(202L);
        when(activeOrderMapper.selectByIdIgnoreDeleted(409L)).thenReturn(order);
        assertThrows(RuntimeException.class, () -> service.getActiveOrderSignatureDetail(501L, 742L));
        order.setWorkOrderId(201L); order.setTenantId(2L);
        assertThrows(RuntimeException.class, () -> service.getActiveOrderDetail(501L));
        order.setTenantId(1L);
        when(reviewMapper.selectById(501L)).thenReturn(review().setTenantId(2L));
        assertThrows(RuntimeException.class, () -> service.getActiveOrderSignatureDetail(501L, 742L));
        verifyNoInteractions(activeOrderDetailService);
    }
    @Test void missingReviewFailsBeforeAnyOrderRead() {
        assertThrows(RuntimeException.class, () -> service.getActiveOrderSignatureDetail(501L, 742L));
        verifyNoInteractions(activeOrderMapper, activeOrderDetailService);
    }

    private MesActiveOrderHandoffTaskDO summaryTask() {
        var task = new MesActiveOrderHandoffTaskDO().setId(2124588436506107904L)
                .setActiveOrderId(409L).setWorkOrderId(201L).setTaskType("QA_REVIEW")
                .setSourceType("NONCONFORMANCE_REVIEW").setSourceId(501L).setRoundId(501L)
                .setNotificationOnly(false).setStatus("DONE");
        task.setTenantId(1L);
        return task;
    }
    private void prepareSummary() {
        when(reviewMapper.selectById(501L)).thenReturn(review().setQaReviewerUserId(10L).setQaUserId(11L));
        when(activeOrderMapper.selectByIdIgnoreDeleted(409L)).thenReturn(archivedOrder());
        var reviewer = new AdminUserDO(); reviewer.setId(10L); reviewer.setTenantId(1L); reviewer.setNickname("指定QA");
        var actor = new AdminUserDO(); actor.setId(11L); actor.setTenantId(1L); actor.setNickname("实际处置QA");
        when(summaryUserService.getUser(10L)).thenReturn(reviewer);
        when(summaryUserService.getUser(11L)).thenReturn(actor);
    }
    @Test void summaryUsesOriginalCycleAndSeparatesAssignedAndActualQa() throws Exception {
        prepareSummary();
        var task = summaryTask();
        when(summaryHandoffTaskMapper.byIdentity(409L, "QA_REVIEW", "NONCONFORMANCE_REVIEW", 501L, 501L)).thenReturn(task);
        for (String status : java.util.List.of("TODO", "DONE", "CANCELED")) {
            task.setStatus(status);
            var result = service.getSummary(501L);
            assertEquals("指定QA", result.getQaReviewerName()); assertEquals("实际处置QA", result.getQaUserName());
            assertEquals(task.getId(), result.getQaTaskId()); assertEquals(status, result.getQaTaskStatus());
            assertEquals(409L, result.getReview().getActiveOrderId());
            assertTrue(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(result)
                    .contains("\"qaTaskId\":\"2124588436506107904\""));
        }
        verify(summaryMarketReleaseAccess, times(3)).requireActiveOrderReadable(409L);
        verifyNoInteractions(activeOrderDetailService);
    }
    @Test void missingFormalTaskFailsInsteadOfBorrowingAnotherCycle() {
        prepareSummary();
        assertThrows(IllegalStateException.class, () -> service.getSummary(501L));
    }
    @Test void mismatchedTaskTenantAndUnknownStatusFail() {
        prepareSummary(); var task = summaryTask(); task.setTenantId(2L);
        when(summaryHandoffTaskMapper.byIdentity(409L, "QA_REVIEW", "NONCONFORMANCE_REVIEW", 501L, 501L)).thenReturn(task);
        assertThrows(IllegalStateException.class, () -> service.getSummary(501L));
        task.setTenantId(1L); task.setActiveOrderId(410L);
        assertThrows(IllegalStateException.class, () -> service.getSummary(501L));
        task.setActiveOrderId(409L).setStatus("UNKNOWN");
        assertThrows(IllegalStateException.class, () -> service.getSummary(501L));
    }
    @Test void unassignedLegacyReviewReportsNoTaskAndNoPerson() {
        when(reviewMapper.selectById(501L)).thenReturn(review());
        when(activeOrderMapper.selectByIdIgnoreDeleted(409L)).thenReturn(archivedOrder());
        var result = service.getSummary(501L);
        assertNull(result.getQaReviewerName()); assertNull(result.getQaTaskId());
        verifyNoInteractions(summaryUserService);
    }
    @Test void accessDeniedStopsSummaryBeforeNamesAndTasksAreRead() {
        when(reviewMapper.selectById(501L)).thenReturn(review());
        when(activeOrderMapper.selectByIdIgnoreDeleted(409L)).thenReturn(archivedOrder());
        doThrow(new IllegalStateException("denied")).when(summaryMarketReleaseAccess).requireActiveOrderReadable(409L);
        assertThrows(IllegalStateException.class, () -> service.getSummary(501L));
        verifyNoInteractions(summaryUserService, summaryHandoffTaskMapper);
    }
}
