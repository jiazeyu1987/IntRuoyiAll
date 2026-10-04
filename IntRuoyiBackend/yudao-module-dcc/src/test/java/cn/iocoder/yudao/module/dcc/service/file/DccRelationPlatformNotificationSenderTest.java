package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.service.file.relations.*;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.*;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserIdempotentReqDTO;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccRelationPlatformNotificationSenderTest {
    NotifyMessageSendApi platform=mock(NotifyMessageSendApi.class);
    DccLatestControlledFileResolver files=mock(DccLatestControlledFileResolver.class);
    Notification notification=new Notification("dcc-remediation:1:100:round-1:20",8L,100L,20L,LocalDateTime.of(2026,10,3,12,0),"N-100","B/1");
    DccRelationPlatformNotificationSender sender=new DccRelationPlatformNotificationSender(platform,files);
    @BeforeEach void tenant(){TenantContextHolder.setTenantId(1L);}
    @AfterEach void clear(){TenantContextHolder.clear();}
    @Test void sendsWithFormalPlatformBusinessKeyAndExactSourceIdentityDeadline() {
        when(files.resolveSelected(100L)).thenReturn(new FileVersion(1L,100L,10L,9L,"N-100","源.pdf","B/1","ACTIVE",true,true,false));
        when(platform.sendSingleMessageIdempotentlyToAdmin(any())).thenReturn(800L);
        assertEquals(800L,sender.send(notification));assertEquals(800L,sender.send(notification));
        var captor=ArgumentCaptor.forClass(NotifySendSingleToUserIdempotentReqDTO.class);
        verify(platform,times(2)).sendSingleMessageIdempotentlyToAdmin(captor.capture());
        var request=captor.getAllValues().get(0);
        assertEquals(notification.businessKey(),request.getBusinessKey());assertEquals(8L,request.getUserId());
        assertEquals("dcc_relation_remediation",request.getTemplateCode());
        assertEquals("N-100",request.getTemplateParams().get("fileNumber"));
        assertEquals("B/1",request.getTemplateParams().get("versionNo"));
        assertEquals("2026-10-03 12:00:00",request.getTemplateParams().get("dueAt"));
        assertEquals("20",request.getTemplateParams().get("relatedMasterId"));
        assertEquals(request.getTemplateParams(),captor.getAllValues().get(1).getTemplateParams());
        verify(platform,never()).sendSingleMessageToAdmin(any());verify(files,never()).resolveLatest(any());
    }
    @Test void wrongTenantOrMissingPlatformIdAreVisibleFailures() {
        when(files.resolveSelected(100L)).thenReturn(new FileVersion(2L,100L,10L,9L,"N-100","源.pdf","B/1","ACTIVE",true,false,true));
        assertThrows(IllegalStateException.class,()->sender.send(notification));verifyNoInteractions(platform);
        when(files.resolveSelected(100L)).thenReturn(new FileVersion(1L,100L,10L,9L,"N-100","源.pdf","B/1","ACTIVE",true,false,true));
        assertThrows(IllegalStateException.class,()->sender.send(notification));
    }
    @Test void retriedOutboxPreservesFrozenPayloadEvenWhenSourceMetadataLaterChanges() {
        when(files.resolveSelected(100L)).thenReturn(new FileVersion(1L,100L,10L,9L,"N-100","源.pdf","B/1","ACTIVE",true,false,true));
        when(platform.sendSingleMessageIdempotentlyToAdmin(any())).thenReturn(800L);sender.send(notification);
        when(files.resolveSelected(100L)).thenReturn(new FileVersion(1L,100L,10L,9L,"N-RENAMED","源新名称.pdf","B/1","OBSOLETE",false,false,false));
        sender.send(notification);
        var captor=ArgumentCaptor.forClass(NotifySendSingleToUserIdempotentReqDTO.class);
        verify(platform,times(2)).sendSingleMessageIdempotentlyToAdmin(captor.capture());
        assertEquals(captor.getAllValues().get(0).getTemplateParams(),captor.getAllValues().get(1).getTemplateParams());
    }
}
