package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketBoundFile;
import cn.iocoder.yudao.module.dcc.service.upload.DccTrainingUploadTicketBoundFile;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.LocalDateTime;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class DccTrainingAttachmentServiceTest {
    private DccControlledFileAttachmentServiceImpl service;
    private DccControlledFileAttachmentMapper attachments;
    private DccControlledFileMapper files;
    private DccOfflineTrainingRecordService training;
    private AdminUserApi users;
    private DccControlledFileDO current;
    private DccUploadTicketBoundFile bound;
    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);TransactionSynchronizationManager.setActualTransactionActive(true);
        service=new DccControlledFileAttachmentServiceImpl();attachments=mock(DccControlledFileAttachmentMapper.class);
        files=mock(DccControlledFileMapper.class);training=mock(DccOfflineTrainingRecordService.class);users=mock(AdminUserApi.class);
        var storage=mock(FileMapper.class);
        current=DccControlledFileDO.builder().id(20L).tenantId(1L).requesterId(99L).categoryId(2L).dccProjectCodeId(4L)
                .processInstanceId("round-1").needTraining(true).status(DccOfflineTrainingRecordService.STATUS).build();
        bound=new DccUploadTicketBoundFile("formal-ticket",111L,"Training.pdf","application/pdf",12L);
        when(files.selectByIdAndTenantForUpdate(1L,20L)).thenReturn(current);
        when(users.getUser(99L)).thenReturn(new AdminUserRespDTO().setId(99L).setTenantId(1L).setStatus(0)
                .setUsername("uploader").setNickname("真实上传人"));
        when(storage.selectById(111L)).thenReturn(FileDO.builder().id(111L).name("Training.pdf").type("application/pdf")
                .size(12L).configId(1L).path("owned/training.pdf").build());
        when(training.requireConfiguredRecipients(current)).thenReturn(List.of(99L));
        when(training.requireUpload(99L,current)).thenReturn(new DccOfflineTrainingRecordService.Waiting(current,"round-1","execution-1"));
        doAnswer(call->{var row=call.getArgument(0,DccControlledFileAttachmentDO.class);row.setId(700L);return 1;})
                .when(attachments).insert(any(DccControlledFileAttachmentDO.class));
        wire(service,"attachmentMapper",attachments,"controlledFileMapper",files,"offlineTraining",training,"users",users,"fileMapper",storage);
    }
    @AfterEach void clear() {TenantContextHolder.clear();TransactionSynchronizationManager.clear();}
    @Test void bindsExactVersionRoundUploaderAndServerTimeThenMapsClickableAttachmentId() {
        LocalDateTime before=LocalDateTime.now().withNano(0);
        Long id=bind(99L,20L,"round-1",bound);assertEquals(700L,id);assertNotEquals(bound.storageFileId(),id);
        var captor=org.mockito.ArgumentCaptor.forClass(DccControlledFileAttachmentDO.class);
        verify(attachments).insert(captor.capture());var row=captor.getValue();
        assertEquals("a".repeat(64),row.getFileSha256());
        assertEquals(1L,row.getTenantId());assertEquals(20L,row.getControlledFileId());
        assertEquals("TRAINING_RECORD",ReflectionTestUtils.getField(row,"attachmentPurpose"));
        assertEquals("round-1",ReflectionTestUtils.getField(row,"applicationProcessInstanceId"));
        assertEquals(99L,ReflectionTestUtils.getField(row,"uploadedBy"));
        assertEquals("uploader",ReflectionTestUtils.getField(row,"uploadedUsernameSnapshot"));
        assertEquals("真实上传人",ReflectionTestUtils.getField(row,"uploadedNicknameSnapshot"));
        var time=(LocalDateTime)ReflectionTestUtils.getField(row,"uploadedAt");
        assertFalse(time.isBefore(before));assertFalse(time.isAfter(LocalDateTime.now()));
        when(attachments.selectListByControlledFileId(20L)).thenReturn(List.of(row));
        var response=service.listAttachments(20L).get(0);assertEquals(700L,response.getAttachmentId());
        assertEquals(time,ReflectionTestUtils.getField(response,"uploadedAt"));
        assertEquals(99L,ReflectionTestUtils.getField(response,"uploadedBy"));
    }
    @Test void rejectsNonUploaderWrongRoundDisabledAccountAndMissingTransactionBeforeInsert() {
        assertThrows(RuntimeException.class,()->bind(77L,20L,"round-1",bound));
        assertThrows(RuntimeException.class,()->bind(99L,20L,"other-round",bound));
        users.getUser(99L).setStatus(1);assertThrows(RuntimeException.class,()->bind(99L,20L,"round-1",bound));
        users.getUser(99L).setStatus(0);TransactionSynchronizationManager.setActualTransactionActive(false);
        assertThrows(RuntimeException.class,()->bind(99L,20L,"round-1",bound));
        verify(attachments,never()).insert(any(DccControlledFileAttachmentDO.class));
    }
    @Test void rejectsForgedPhysicalFileAndBadTenantBeforeInsert() {
        assertThrows(IllegalArgumentException.class,()->bind(99L,20L,"round-1",
                new DccUploadTicketBoundFile("formal-ticket",111L,"Other.pdf","application/pdf",12L)));
        current.setTenantId(2L);assertThrows(IllegalArgumentException.class,()->bind(99L,20L,"round-1",bound));
        verify(attachments,never()).insert(any(DccControlledFileAttachmentDO.class));
    }
    @Test void lostInsertFailsInsteadOfReportingBindingSuccess() {
        when(attachments.insert(any(DccControlledFileAttachmentDO.class))).thenReturn(0);
        assertThrows(IllegalStateException.class,()->bind(99L,20L,"round-1",bound));
    }
    @Test void ordinaryAttachmentsValidateAllItemsBeforeFirstWriteAndRejectLostInsert() {
        assertThrows(RuntimeException.class,()->service.bindAttachments(20L,List.of(bound,
                new DccUploadTicketBoundFile("bad-ticket",null,"Invalid.pdf","application/pdf",12L))));
        verify(attachments,never()).insert(any(DccControlledFileAttachmentDO.class));
        when(attachments.insert(any(DccControlledFileAttachmentDO.class))).thenReturn(0);
        assertThrows(IllegalStateException.class,()->service.bindAttachments(20L,List.of(bound)));
    }
    @Test void attachmentBinaryNeverReturnsCrossTenantOrDeletedFacts() {
        var row=DccControlledFileAttachmentDO.builder().id(700L).controlledFileId(20L).storageFileId(111L)
                .originalFileName("Training.pdf").contentType("application/pdf").fileSize(12L).build();
        row.setTenantId(2L);when(attachments.selectById(700L)).thenReturn(row);
        assertThrows(RuntimeException.class,()->service.getAttachment(20L,700L));
        row.setTenantId(1L);row.setDeleted(true);
        assertThrows(RuntimeException.class,()->service.getAttachment(20L,700L));
        row.setDeleted(false);assertEquals(111L,service.getAttachment(20L,700L).storageFileId());
        assertThrows(RuntimeException.class,()->service.getAttachment(null,700L));
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.NullAndEmptySource
    @org.junit.jupiter.params.provider.ValueSource(strings={"abc","AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
            "gggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggg"," aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
    void invalidTrainingTicketDigestNeverInsertsOrChangesFile(String digest){
        assertThrows(IllegalArgumentException.class,()->service.bindTrainingRecord(99L,20L,"round-1",new DccTrainingUploadTicketBoundFile(bound,digest)));
        verifyNoInteractions(attachments);verify(files,never()).updateById(any(DccControlledFileDO.class));
    }
    @Test void missingTypedFileNeverFallsBackToGenericMetadata(){
        assertThrows(IllegalArgumentException.class,()->service.bindTrainingRecord(99L,20L,"round-1",null));
        assertThrows(IllegalArgumentException.class,()->service.bindTrainingRecord(99L,20L,"round-1",new DccTrainingUploadTicketBoundFile(null,"a".repeat(64))));
        verifyNoInteractions(attachments);
    }
    private Long bind(Long actor,Long file,String round,DccUploadTicketBoundFile upload) {
        return service.bindTrainingRecord(actor,file,round,new DccTrainingUploadTicketBoundFile(upload,"a".repeat(64)));
    }
    private static void wire(Object bean,Object... fields) {
        for(int i=0;i<fields.length;i+=2) ReflectionTestUtils.setField(bean,(String)fields[i],fields[i+1]);
    }
}
