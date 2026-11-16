package cn.iocoder.yudao.module.mes.service.pro.handoff;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.MesActiveOrderHandoffTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.handoff.MesActiveOrderHandoffTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserReqDTO;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
class MesActiveOrderHandoffOverdueServiceTest {
 MesActiveOrderHandoffOverdueService service; MesActiveOrderHandoffTaskMapper tasks; MesProWorkOrderMapper orders;
 NotifyMessageSendApi notifications; AdminUserApi users; LocalDateTime now;
 @BeforeEach void setup() {
  TenantContextHolder.setTenantId(71L); service=new MesActiveOrderHandoffOverdueService();
  tasks=mock(MesActiveOrderHandoffTaskMapper.class);orders=mock(MesProWorkOrderMapper.class);
  notifications=mock(NotifyMessageSendApi.class);users=mock(AdminUserApi.class);now=LocalDateTime.now();
  ReflectionTestUtils.setField(service,"tasks",tasks);ReflectionTestUtils.setField(service,"workOrders",orders);
  ReflectionTestUtils.setField(service,"notifyApi",notifications);ReflectionTestUtils.setField(service,"users",users);
 }
 @AfterEach void cleanup() {TenantContextHolder.clear();}
 MesActiveOrderHandoffTaskDO task() {
  var task=new MesActiveOrderHandoffTaskDO().setId(2L).setActiveOrderId(1L).setWorkOrderId(9L)
    .setTaskType("QA_REVIEW").setSourceType("NONCONFORMANCE_REVIEW").setSourceId(3L).setRoundId(3L)
    .setCandidateUserSnapshot("101,102").setStatus("TODO").setDueTime(now.minusMinutes(1))
    .setActionUrl("/mes/pro/feedback/edhr-nonconformance-review?activeOrderId=1&reviewId=3&handoffTaskId=2&roundId=3&handoffType=QA_REVIEW");
  task.setTenantId(71L);var order=MesProWorkOrderDO.builder().id(9L).code("WO-9").build();order.setTenantId(71L);
  when(orders.selectById(9L)).thenReturn(order);when(tasks.due(71L,now,20)).thenReturn(List.of(task));return task;
 }
 @Test void qaReminderNotifiesSnapshotAndRecordsMarkerWithoutClosingTask() {
  when(notifications.sendSingleMessageToAdmin(any())).thenReturn(5001L);
  var task=task();for(long id:new long[]{101,102})when(users.getUser(id)).thenReturn(new AdminUserRespDTO().setId(id).setTenantId(71L).setStatus(0));
  when(tasks.markOverdueNotified(71L,2L,now)).thenReturn(1);
  assertEquals(1,service.process(now,20));assertEquals("TODO",task.getStatus());
  var captor=ArgumentCaptor.forClass(NotifySendSingleToUserReqDTO.class);verify(notifications,times(2)).sendSingleMessageToAdmin(captor.capture());
  assertEquals(Set.of(101L,102L),new HashSet<>(captor.getAllValues().stream().map(NotifySendSingleToUserReqDTO::getUserId).toList()));
  verify(tasks).markOverdueNotified(71L,2L,now);
 }
 @Test void foreignTenantReceiverFailsWithoutSuccessMarker() {
  task();when(users.getUser(101L)).thenReturn(new AdminUserRespDTO().setId(101L).setTenantId(99L).setStatus(0));
  assertThrows(IllegalStateException.class,()->service.process(now,20));
  verifyNoInteractions(notifications);verify(tasks,never()).markOverdueNotified(any(),any(),any());
 }
 @org.junit.jupiter.params.ParameterizedTest
 @org.junit.jupiter.params.provider.NullSource
 @org.junit.jupiter.params.provider.ValueSource(longs={0,-1})
 void missingReceiptDoesNotMarkNotifiedAndRetryCanSucceed(Long invalidReceipt) {
  task();for(long id:new long[]{101,102})when(users.getUser(id)).thenReturn(new AdminUserRespDTO().setId(id).setTenantId(71L).setStatus(0));
  // The first candidate has a receipt, the second fails: the whole task remains retryable.
  when(notifications.sendSingleMessageToAdmin(any())).thenReturn(5001L,invalidReceipt,5002L,5003L);
  assertThrows(IllegalStateException.class,()->service.process(now,20));
  verify(tasks,never()).markOverdueNotified(any(),any(),any());
  when(tasks.markOverdueNotified(71L,2L,now)).thenReturn(1);
  assertEquals(1,service.process(now,20));
  verify(tasks).markOverdueNotified(71L,2L,now);
  when(tasks.due(71L,now,20)).thenReturn(List.of());
  assertEquals(0,service.process(now,20));
  verify(notifications,times(4)).sendSingleMessageToAdmin(any());
 }
 @Test void disabledTemplateExceptionDoesNotMarkNotified() {
  task();when(users.getUser(101L)).thenReturn(new AdminUserRespDTO().setId(101L).setTenantId(71L).setStatus(0));
  var disabled=cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(
    cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.NOTIFY_SEND_TEMPLATE_DISABLED);
  when(notifications.sendSingleMessageToAdmin(any())).thenThrow(disabled);
  assertSame(disabled,assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->service.process(now,20)));
  verify(tasks,never()).markOverdueNotified(any(),any(),any());
 }
 @Test void noDueTasksSendsNothing() {assertEquals(0,service.process(now,20));verifyNoInteractions(notifications,users);}
}
