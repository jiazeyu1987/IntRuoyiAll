package cn.iocoder.yudao.module.mes.service.pro.handoff;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.MesActiveOrderHandoffTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesNcrFreezeNotificationTest extends MesActiveOrderHandoffLifecycleTest {
    MesNcrFreezeRecipientResolver resolver;
    MesProEdhrWorkTaskMapper workTasks;
    AdminUserMapper users;
    MesProEdhrNonconformanceReviewDO review;
    @BeforeEach void freezeFixture() {
        resolver=new MesNcrFreezeRecipientResolver();workTasks=mock(MesProEdhrWorkTaskMapper.class);users=mock(AdminUserMapper.class);
        ReflectionTestUtils.setField(resolver,"orders",orders);ReflectionTestUtils.setField(resolver,"handoffs",tasks);
        ReflectionTestUtils.setField(resolver,"workTasks",workTasks);ReflectionTestUtils.setField(resolver,"users",users);
        ReflectionTestUtils.setField(service,"freezeRecipients",resolver);
        review=new MesProEdhrNonconformanceReviewDO().setId(91L).setActiveOrderId(413L).setWorkOrderId(274L)
                .setReviewStatus("pending_review").setQaReviewerUserId(345L).setReviewCode("NCR-91").setNonconformanceReason("检验不合格");
        review.setTenantId(1L);
        when(orders.selectList(any())).thenReturn(List.of(order));when(tasks.selectList(any())).thenReturn(List.of());
        when(workTasks.selectList(any())).thenReturn(List.of());
        when(users.selectById(anyLong())).thenAnswer(i->{var u=new AdminUserDO();u.setId(i.getArgument(0));u.setTenantId(1L);u.setStatus(0);return u;});
    }
    @AfterEach void clearTenant(){TenantContextHolder.clear();}
    @Test void recipientsUseOnlyCurrentSystemResponsibilityAndDeduplicateQa() {
        var handoff=new MesActiveOrderHandoffTaskDO().setActiveOrderId(413L).setWorkOrderId(274L).setStatus("TODO")
                .setCandidateUserSnapshot("341,342,345").setResponsibilitySnapshotJson("{\"identityDomain\":\"SYSTEM_USER\"}");handoff.setTenantId(1L);
        when(tasks.selectList(any())).thenReturn(List.of(handoff));
        var accepted=new MesProEdhrWorkTaskDO().setWorkOrderId(274L).setStatus("DOING").setTaskType("APPROVE")
                .setAssigneeUserId(343L).setCandidateUserSnapshot("346");
        var candidate=new MesProEdhrWorkTaskDO().setWorkOrderId(274L).setStatus("OVERDUE").setTaskType("REVIEW")
                .setCandidateUserSnapshot("342,344");
        when(workTasks.selectList(any())).thenReturn(List.of(accepted,candidate));
        assertEquals("341,342,343,344",resolver.resolve(review));
        verify(users,never()).selectById(345L);verify(users,never()).selectById(346L);
    }
    @Test void completedProductionAcceptanceRetainsFormalPersonnelForTheRunningCycle() {
        var handoff=new MesActiveOrderHandoffTaskDO().setActiveOrderId(413L).setWorkOrderId(274L).setStatus("DONE")
                .setTaskType("PRODUCTION_HANDOFF").setCandidateUserSnapshot("342")
                .setResponsibilitySnapshotJson("{\"identityDomain\":\"SYSTEM_USER\"}");handoff.setTenantId(1L);
        when(tasks.selectList(any())).thenReturn(List.of(handoff));
        assertEquals("341,342",resolver.resolve(review));
        handoff.setTaskType("PRODUCTION_REVIEW");
        assertThrows(RuntimeException.class,()->resolver.resolve(review));
    }
    @Test void profileDomainNeverBecomesSystemRecipientEvenWhenNumbersCollide() {
        var handoff=new MesActiveOrderHandoffTaskDO().setActiveOrderId(413L).setWorkOrderId(274L).setStatus("TODO")
                .setCandidateUserSnapshot("342").setResponsibilitySnapshotJson("{\"identityDomain\":\"MES_EMPLOYEE_PROFILE\"}");handoff.setTenantId(1L);
        when(tasks.selectList(any())).thenReturn(List.of(handoff));
        assertThrows(RuntimeException.class,()->resolver.resolve(review));verifyNoInteractions(users);
    }
    @Test void foreignOrDisabledResponsibilityFailsBeforeDelivery() {
        var foreign=new AdminUserDO();foreign.setId(341L);foreign.setTenantId(2L);foreign.setStatus(0);
        when(users.selectById(341L)).thenReturn(foreign);
        assertThrows(RuntimeException.class,()->service.ncrFrozen(review,343L));verifyNoInteractions(delivery);
        foreign.setTenantId(1L);foreign.setStatus(1);
        assertThrows(RuntimeException.class,()->service.ncrFrozen(review,343L));verifyNoInteractions(delivery);
    }
    @Test void noticeIsDoneOnlyAndReplayKeepsFrozenRecipients() {
        service.ncrFrozen(review,343L);
        var captured=ArgumentCaptor.forClass(MesActiveOrderHandoffTaskDO.class);verify(tasks).insert(captured.capture());
        var notice=captured.getValue();assertEquals("NCR_FREEZE_NOTICE",notice.getTaskType());
        assertEquals("DONE",notice.getStatus());assertTrue(notice.getNotificationOnly());assertEquals("341",notice.getCandidateUserSnapshot());
        assertTrue(notice.getActionUrl().startsWith("/user/profile?"));assertTrue(notice.getActionUrl().endsWith("tab=notifyMessage"));
        assertEquals("SYSTEM_USER",JsonUtils.parseTree(notice.getResponsibilitySnapshotJson()).path("identityDomain").asText());
        assertNull(notice.getDueTime());verify(delivery).schedule(notice);
        when(tasks.byIdentity(413L,"NCR_FREEZE_NOTICE","NONCONFORMANCE_REVIEW",91L,91L)).thenReturn(notice);
        order.setLeaderUserId(349L);service.ncrFrozen(review,343L);
        verify(tasks,times(1)).insert(any(MesActiveOrderHandoffTaskDO.class));verify(delivery,times(1)).schedule(any());
    }
    @Test void deliveryFailurePropagatesAndNoticeCannotBeTodo() {
        doThrow(new IllegalStateException("outbox persistence failed")).when(delivery).schedule(any());
        assertThrows(IllegalStateException.class,()->service.ncrFrozen(review,343L));
        var capture=ArgumentCaptor.forClass(MesActiveOrderHandoffTaskDO.class);verify(tasks).insert(capture.capture());
        capture.getValue().setStatus("TODO");assertThrows(RuntimeException.class,()->MesActiveOrderHandoffContract.validate(capture.getValue()));
    }
}
