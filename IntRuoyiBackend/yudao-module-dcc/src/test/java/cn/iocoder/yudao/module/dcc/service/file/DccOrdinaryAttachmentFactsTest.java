package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileTemporaryFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileTemporaryFileMapper;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketBoundFile;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import jakarta.annotation.Resource;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class DccOrdinaryAttachmentFactsTest {
    DccControlledFileAttachmentServiceImpl service;
    DccControlledFileAttachmentMapper attachments;
    DccControlledFileTemporaryFileMapper tickets;
    DccControlledFileDO file;
    DccControlledFileTemporaryFileDO ticket;
    final LocalDateTime uploaded=LocalDateTime.of(2026,10,14,19,0);
    @BeforeEach void setup() throws Exception {
        TenantContextHolder.setTenantId(1L);TransactionSynchronizationManager.setActualTransactionActive(true);
        service=new DccControlledFileAttachmentServiceImpl();
        for(var f:service.getClass().getDeclaredFields())if(!Modifier.isStatic(f.getModifiers())&&f.getAnnotation(Resource.class)!=null)
            ReflectionTestUtils.setField(service,f.getName(),mock(f.getType()));
        attachments=port("attachmentMapper");
        // Keep the original RED executable before the new dependency exists.
        try { tickets=port("temporaryFileMapper"); } catch(IllegalArgumentException absent) { tickets=null; }
        file=DccControlledFileDO.builder().id(20L).tenantId(1L).requesterId(99L).categoryId(2L).status("WORKING").build();
        DccControlledFileMapper files=port("controlledFileMapper");when(files.selectByIdAndTenantForUpdate(1L,20L)).thenReturn(file);
        AdminUserApi users=port("users");when(users.getUser(99L)).thenReturn(new AdminUserRespDTO().setId(99L).setTenantId(1L).setStatus(0).setUsername("uploader").setNickname("真实上传人"));
        ticket=DccControlledFileTemporaryFileDO.builder().id(800L).uploadTicket("attachment-ticket").uploaderId(99L)
                .categoryId(2L).purpose("ATTACHMENT").status("BOUND").cleanupStatus("BOUND").boundControlledFileId(20L)
                .storageFileId(111L).originalFileName("Notes.pdf").contentType("application/pdf").fileSize(12L).fileSha256("a".repeat(64)).build();
        ticket.setTenantId(1L);ticket.setCreateTime(uploaded);
        if(tickets!=null)when(tickets.selectOne(org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.toolkit.support.SFunction<DccControlledFileTemporaryFileDO,?>>any(),eq("attachment-ticket"))).thenReturn(ticket);
        FileMapper storage=port("fileMapper");when(storage.selectById(111L)).thenReturn(FileDO.builder().id(111L).name("Notes.pdf").type("application/pdf").size(12L).configId(1L).path("own/notes.pdf").build());
        doAnswer(c->{c.getArgument(0,DccControlledFileAttachmentDO.class).setId(700L);return 1;}).when(attachments).insert(any(DccControlledFileAttachmentDO.class));
    }
    @AfterEach void clear(){TenantContextHolder.clear();TransactionSynchronizationManager.clear();}
    @SuppressWarnings("unchecked") <T>T port(String n){return (T)ReflectionTestUtils.getField(service,n);}
    DccUploadTicketBoundFile bound(){return new DccUploadTicketBoundFile("attachment-ticket",111L,"Notes.pdf","application/pdf",12L);}
    @Test void savesActualUploaderUploadTimeHashAndExactVersionForOneOrMany() {
        service.bindAttachments(20L,List.of(bound()));
        var captor=org.mockito.ArgumentCaptor.forClass(DccControlledFileAttachmentDO.class);verify(attachments).insert(captor.capture());
        var saved=captor.getValue();assertEquals(20L,saved.getControlledFileId());assertEquals(99L,saved.getUploadedBy());
        assertEquals("uploader",saved.getUploadedUsernameSnapshot());assertEquals("真实上传人",saved.getUploadedNicknameSnapshot());
        assertEquals(uploaded,saved.getUploadedAt());assertEquals("a".repeat(64),saved.getFileSha256());assertNull(saved.getApplicationProcessInstanceId());
    }
    @Test void zeroAttachmentsIsLegalAndPerformsNoBinding(){service.bindAttachments(20L,List.of());verifyNoInteractions(attachments);}
    @Test void multipleAttachmentsKeepIndependentIdentityAndSelectionOrder(){
        var second=new DccControlledFileTemporaryFileDO();org.springframework.beans.BeanUtils.copyProperties(ticket,second);
        second.setId(801L);second.setUploadTicket("second-ticket");second.setStorageFileId(112L);second.setOriginalFileName("Second.pdf");
        when(tickets.selectOne(org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.toolkit.support.SFunction<DccControlledFileTemporaryFileDO,?>>any(),eq("second-ticket"))).thenReturn(second);
        FileMapper storage=port("fileMapper");when(storage.selectById(112L)).thenReturn(FileDO.builder().id(112L).name("Second.pdf").type("application/pdf").size(12L).configId(1L).path("own/second.pdf").build());
        service.bindAttachments(20L,List.of(bound(),new DccUploadTicketBoundFile("second-ticket",112L,"Second.pdf","application/pdf",12L)));
        var captor=org.mockito.ArgumentCaptor.forClass(DccControlledFileAttachmentDO.class);verify(attachments,times(2)).insert(captor.capture());
        assertEquals(List.of(0,1),captor.getAllValues().stream().map(DccControlledFileAttachmentDO::getSortNo).toList());
        assertEquals(List.of(111L,112L),captor.getAllValues().stream().map(DccControlledFileAttachmentDO::getStorageFileId).toList());
    }
    @Test void rejectsForeignActorTenantPurposeOrVersionBeforeInsert(){
        ticket.setTenantId(2L);assertThrows(RuntimeException.class,()->service.bindAttachments(20L,List.of(bound())));
        ticket.setTenantId(1L);ticket.setUploaderId(77L);assertThrows(RuntimeException.class,()->service.bindAttachments(20L,List.of(bound())));
        ticket.setUploaderId(99L);ticket.setPurpose("SOURCE");assertThrows(RuntimeException.class,()->service.bindAttachments(20L,List.of(bound())));
        ticket.setPurpose("ATTACHMENT");ticket.setBoundControlledFileId(21L);assertThrows(RuntimeException.class,()->service.bindAttachments(20L,List.of(bound())));
        verify(attachments,never()).insert(any(DccControlledFileAttachmentDO.class));
    }
    @Test void freezesApplicationRoundWithoutRewritingAnEarlierRound() throws Exception {
        file.setProcessInstanceId("actual-bpm");
        var row=DccControlledFileAttachmentDO.builder().id(700L).controlledFileId(20L).attachmentPurpose("ATTACHMENT").uploadedBy(99L).uploadedAt(uploaded).build();row.setTenantId(1L);
        when(attachments.selectListByControlledFileId(20L)).thenReturn(List.of(row));
        when(attachments.updateById(any(DccControlledFileAttachmentDO.class))).thenReturn(1);
        var method=service.getClass().getMethod("bindApplicationRound",Long.class,String.class);method.invoke(service,20L,"actual-bpm");
        var captor=org.mockito.ArgumentCaptor.forClass(DccControlledFileAttachmentDO.class);verify(attachments).updateById(captor.capture());
        assertEquals("actual-bpm",captor.getValue().getApplicationProcessInstanceId());
        row.setApplicationProcessInstanceId("earlier-bpm");clearInvocations(attachments);method.invoke(service,20L,"actual-bpm");
        verify(attachments,never()).updateById(any(DccControlledFileAttachmentDO.class));
    }
}
