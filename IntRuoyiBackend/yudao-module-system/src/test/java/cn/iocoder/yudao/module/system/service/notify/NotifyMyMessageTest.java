package cn.iocoder.yudao.module.system.service.notify;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyMessageDO;
import cn.iocoder.yudao.module.system.dal.mysql.notify.NotifyMessageMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class NotifyMyMessageTest {
 @Test void isolatesRecipientTenantAndUserType() {
  NotifyMessageMapper mapper = mock(NotifyMessageMapper.class);
  NotifyMessageServiceImpl service = new NotifyMessageServiceImpl();
  ReflectionTestUtils.setField(service, "notifyMessageMapper", mapper);
  NotifyMessageReadEnricher enricher = mock(NotifyMessageReadEnricher.class);
  doAnswer(invocation -> {
   ((NotifyMessageDO) invocation.getArgument(0)).setTemplateContent("projected");
   return null;
  }).when(enricher).enrich(any());
  ReflectionTestUtils.setField(service, "readEnrichers", java.util.List.of(enricher));
  denied(() -> service.getMyNotifyMessage(null, 2L));
  denied(() -> service.getMyNotifyMessage(0L, 2L));
  denied(() -> service.getMyNotifyMessage(-1L, 2L));
  verifyNoInteractions(mapper, enricher);
  TenantContextHolder.setTenantId(1L);
  try {
   NotifyMessageDO message = new NotifyMessageDO().setId(9L).setUserId(2L).setUserType(UserTypeEnum.ADMIN.getValue());
   message.setTenantId(1L);
   when(mapper.selectById(9L)).thenReturn(message);
   NotifyMessageDO projected = service.getMyNotifyMessage(9L, 2L);
   assertEquals(9L, projected.getId());
   assertEquals("projected", projected.getTemplateContent());
   assertNull(message.getTemplateContent());
   verify(enricher).enrich(any());
   clearInvocations(enricher);
   denied(() -> service.getMyNotifyMessage(9L, 3L));
   message.setTenantId(3L);
   denied(() -> service.getMyNotifyMessage(9L, 2L));
   message.setTenantId(1L); message.setUserType(UserTypeEnum.MEMBER.getValue());
   denied(() -> service.getMyNotifyMessage(9L, 2L));
   when(mapper.selectById(9L)).thenReturn(null);
   denied(() -> service.getMyNotifyMessage(9L, 2L));
   verifyNoInteractions(enricher);
  } finally { TenantContextHolder.clear(); }
 }
 private void denied(org.junit.jupiter.api.function.Executable action) {
  assertEquals(1_002_027_000, assertThrows(ServiceException.class, action).getCode());
 }
}
