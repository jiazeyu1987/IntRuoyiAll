package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.*;
import cn.iocoder.yudao.module.dcc.enums.*;
import cn.iocoder.yudao.module.dcc.service.download.*;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.infra.service.file.access.*;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.lang.reflect.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class DccAttachmentDownloadTest {
    DccControlledFileQueryServiceImpl service;DccControlledFileDO file;
    DccControlledFileAttachmentService attachments;FileService storage;FileMapper files;
    final DccRequestAuditContext audit=new DccRequestAuditContext("127.0.0.1", "unit-test", "request");
    @BeforeEach void setup() throws Exception {
        TenantContextHolder.setTenantId(1L);service=new DccControlledFileQueryServiceImpl();
        for(var f:service.getClass().getDeclaredFields())if(!Modifier.isStatic(f.getModifiers())&&f.getAnnotation(Resource.class)!=null)
            ReflectionTestUtils.setField(service,f.getName(),mock(f.getType()));
        ReflectionTestUtils.setField(service,"downloadPolicyService",new DccDownloadPolicyService());
        file=DccControlledFileDO.builder().id(20L).tenantId(1L).categoryId(2L).directoryId(3L).dccProjectCodeId(4L)
                .publishedFileId(44L).versionNo("A/1").fileNumber("TASK-ATTACHMENT").status("ACTIVE").build();
        DccControlledFileMapper controlled=port("controlledFileMapper");when(controlled.selectById(20L)).thenReturn(file);
        DccControlledFileAssignmentScopeService assignment=port("assignmentScopeService");
        when(assignment.isWithinAssignedFileScope(99L,20L)).thenReturn(true);
        DccProjectAccessService project=port("projectAccessService");when(project.listReadableProjectIds(99L)).thenReturn(List.of(4L));
        DccControlledFileCategoryPermissionSupport permissions=port("permissionSupport");when(permissions.hasCategoryPermission(2L,99L,DccFileCategoryPermissionActionEnum.DOWNLOAD)).thenReturn(true);
        DccDirectoryAccessPermissionService directory=port("directoryAccessPermissionService");when(directory.getAuthorizedDirectoryIds(99L,DccAccessTypeEnum.DOWNLOAD)).thenReturn(Set.of(3L));
        attachments=port("attachmentService");when(attachments.getAttachment(20L,700L)).thenReturn(new DccControlledFileAttachmentBinary(700L,111L,"notes.json","application/json",11L));
        files=port("fileMapper");when(files.selectById(111L)).thenReturn(FileDO.builder().id(111L).configId(1L).path("task/notes.json").name("notes.json").type("application/json").size(11L).build());
        storage=port("fileService");when(storage.getFileContent(1L,"task/notes.json")).thenReturn("{\"ok\":true}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        BusinessFileAccessService business=port("businessFileAccessService");when(business.assertAllowed(any())).thenReturn(Optional.of(new BusinessFileAccessReference("dcc","DCC_CONTROLLED_FILE",20L,"A/1",1L,null)));
        DccControlledFileAccessEventMapper events=port("accessEventMapper");doAnswer(c->{c.getArgument(0,DccControlledFileAccessEventDO.class).setId(800L);return 1;}).when(events).insert(any(DccControlledFileAccessEventDO.class));
        DccControlledFileDownloadRecordMapper downloads=port("downloadRecordMapper");doAnswer(c->{c.getArgument(0,DccControlledFileDownloadRecordDO.class).setId(900L);return 1;}).when(downloads).insert(any(DccControlledFileDownloadRecordDO.class));when(downloads.update(isNull(),any())).thenReturn(1);when(downloads.updateById(any(DccControlledFileDownloadRecordDO.class))).thenReturn(1);
        DccControlledFileAccessLogMapper logs=port("accessLogMapper");when(logs.insert(any(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileAccessLogDO.class))).thenReturn(1);
        PlatformTransactionManager transactions=port("transactionManager");when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }
    @AfterEach void clear(){TenantContextHolder.clear();}
    @SuppressWarnings("unchecked") <T>T port(String name){return (T)ReflectionTestUtils.getField(service,name);}
    DccDownloadFileBinary download(Boolean confirmed) throws Throwable {
        try{return (DccDownloadFileBinary)service.getClass().getMethod("readAttachmentDownloadFile",Long.class,Long.class,Long.class,Boolean.class,String.class,DccRequestAuditContext.class)
                .invoke(service,99L,20L,700L,confirmed,"attachment-request",audit);}catch(InvocationTargetException e){throw e.getCause();}
    }
    @Test void downloadsActualAttachmentWithNameHashAndAuditRatherThanTheMainPublishedBody() throws Throwable {
        var result=download(true);assertEquals("notes.json",result.fileName());assertEquals("application/json",result.contentType());
        assertEquals("{\"ok\":true}",new String(result.bytes(),java.nio.charset.StandardCharsets.UTF_8));assertEquals("attachment-request",result.downloadRequestId());assertEquals(64,result.plainSha256().length());
        verify(files,never()).selectById(44L);verify(storage).getFileContent(1L,"task/notes.json");
    }
    @Test void revokedPermissionAndUnconfirmedWarningNeverReadAttachmentBytes() {
        DccControlledFileCategoryPermissionSupport permissions=port("permissionSupport");when(permissions.hasCategoryPermission(2L,99L,DccFileCategoryPermissionActionEnum.DOWNLOAD)).thenReturn(false);
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->download(true));assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->download(false));verifyNoInteractions(storage);
    }
    @Test void foreignTenantOrForeignAttachmentIsRejectedBeforeStorageRead() {
        file.setTenantId(2L);assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->download(true));file.setTenantId(1L);
        when(attachments.getAttachment(20L,700L)).thenThrow(new IllegalArgumentException("foreign attachment"));assertThrows(IllegalArgumentException.class,()->download(true));verifyNoInteractions(storage);
    }
    @Test void downloadAuditIdentifiesAttachmentAndSnapshotVersion() throws Throwable {
        download(true);
        DccControlledFileAccessEventMapper events=port("accessEventMapper");
        var event=org.mockito.ArgumentCaptor.forClass(DccControlledFileAccessEventDO.class);verify(events).insert(event.capture());
        assertEquals("ATTACHMENT_DOWNLOAD:700",event.getValue().getPurpose());assertEquals(20L,event.getValue().getControlledFileId());assertEquals("A/1",event.getValue().getFileVersionNo());
        DccControlledFileDownloadRecordMapper downloads=port("downloadRecordMapper");
        var record=org.mockito.ArgumentCaptor.forClass(DccControlledFileDownloadRecordDO.class);verify(downloads).insert(record.capture());assertEquals(event.getValue().getId(),record.getValue().getAccessEventId());
    }
    @Test void historyOrPendingStateAndBusinessStorageDenialNeverReturnAttachment() {
        file.setStatus("OBSOLETE");assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->download(true));
        file.setStatus("CONTROLLED_PENDING_EFFECTIVE");assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->download(true));
        file.setStatus("ACTIVE");BusinessFileAccessService business=port("businessFileAccessService");when(business.assertAllowed(any())).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,()->download(true));verifyNoInteractions(storage);
    }
}
