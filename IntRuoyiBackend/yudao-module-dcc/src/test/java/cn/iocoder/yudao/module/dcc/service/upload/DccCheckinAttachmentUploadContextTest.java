package cn.iocoder.yudao.module.dcc.service.upload;

import cn.iocoder.yudao.module.dcc.service.file.*;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.dcc.service.file.DccRevisionAttachmentsDatabaseTest.Fixture;
import cn.iocoder.yudao.module.dcc.service.audit.DccControlledFileAccessAuditService;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockMultipartFile;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Real upload method, real scoped tickets and private H2 slot; authorization is an explicit external port. */
class DccCheckinAttachmentUploadContextTest {
    Fixture f;DccControlledFileUploadServiceImpl upload;DccControlledFileWorkflowService workflow;
    @BeforeEach void setup() throws Exception {
        f=new Fixture();upload=new DccControlledFileUploadServiceImpl();Fixture.mockDependencies(upload);
        var categories=mock(DccFileCategoryMapper.class);
        when(categories.selectById(2L)).thenReturn(DccFileCategoryDO.builder().id(2L).active(true).lifecycleStage("OUTPUT").build());
        workflow=mock(DccControlledFileWorkflowService.class);
        Fixture.wire(upload,"fileService",f.storage,"fileMapper",f.physical,"categoryMapper",categories,
                "permissionSupport",f.permission,"uploadTicketService",f.tickets,"sourceOwnershipService",f.ownership,"workflowService",workflow);
        // A08 added placement guard retains the existing formal directory contract.
        if(Arrays.stream(upload.getClass().getDeclaredFields()).anyMatch(x->x.getName().equals("publicUploadPlacementService")))
            Fixture.wire(upload,"publicUploadPlacementService",f.placement);
    }
    @AfterEach void close(){f.close();}
    DccControlledFileUploadPreviewReqVO request(String name,String session,byte[] data){
        var r=new DccControlledFileUploadPreviewReqVO();r.setUploadContext("CHECKIN");r.setControlledFileId(20L);r.setCategoryId(2L);
        r.setPurpose("ATTACHMENT");r.setSessionId(session);r.setFiles(new MockMultipartFile[]{new MockMultipartFile("files",name,"application/pdf",data)});return r;
    }
    DccControlledFileUploadRespVO execute(DccControlledFileUploadPreviewReqVO r){return f.tx.execute(s->{
        try{return upload.uploadPreviewFile(99L,r,new DccRequestAuditContext("127.0.0.1","G06-isolated","g06-"+r.getSessionId()));}
        catch(RuntimeException e){throw e;}catch(Exception e){throw new RuntimeException(e);}
    });}
    @Test void twoAttachmentUploadsHaveDistinctFormalScopedTicketsAndPersistedSlots(){
        var a=execute(request("New-one.pdf","client-1",new byte[]{7,8}));var b=execute(request("New-two.pdf","client-2",new byte[]{9,10}));
        assertNotEquals(a.getUploadTicket(),b.getUploadTicket());assertNotEquals(a.getSessionId(),b.getSessionId());
        assertTrue(a.getSessionId().matches("dcc-checkin:20:[a-f0-9]{32}"));assertTrue(b.getSessionId().matches("dcc-checkin:20:[a-f0-9]{32}"));
        assertEquals("ATTACHMENT",a.getPurpose());assertEquals("AVAILABLE",a.getStatus());
        assertEquals(2,f.jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_temporary_file WHERE session_id IN (?,?) AND purpose='ATTACHMENT'",Integer.class,a.getSessionId(),b.getSessionId()));
        verify(workflow,times(2)).validateSourceUploadContext(eq(99L),any());
    }
    @Test void sameSlotSameContentReplaysButDifferentContentRejectsWithoutNewStorage(){
        var r=request("Slot.pdf","one-slot",new byte[]{7,8});var first=execute(r);int count=f.bytes.size();String before=f.snapshot();
        var replay=execute(r);assertEquals(first.getUploadTicket(),replay.getUploadTicket());assertEquals(count,f.bytes.size());assertEquals(before,f.snapshot());
        assertThrows(RuntimeException.class,()->execute(request("Slot.pdf","one-slot",new byte[]{7,9})));
        assertEquals(count,f.bytes.size());assertEquals(before,f.snapshot());
    }
    @Test void categoryOrFormalCheckoutAuthorizationRefusalPrecedesStorageAndTicket(){
        when(f.permission.hasCategoryPermission(anyLong(),anyLong(),eq(DccFileCategoryPermissionActionEnum.UPLOAD))).thenReturn(false);
        String before=f.snapshot();assertThrows(RuntimeException.class,()->execute(request("Denied.pdf","denied-1",new byte[]{1})));
        assertEquals(before,f.snapshot());verify(f.storage,never()).createFileAndReturnId(any(byte[].class),anyString(),anyString(),anyString());
        when(f.permission.hasCategoryPermission(anyLong(),anyLong(),eq(DccFileCategoryPermissionActionEnum.UPLOAD))).thenReturn(true);
        doThrow(new IllegalArgumentException("checkout actor or exact tenant/version refused")).when(workflow).validateSourceUploadContext(eq(99L),any());
        assertThrows(RuntimeException.class,()->execute(request("Denied.pdf","denied-2",new byte[]{1})));assertEquals(before,f.snapshot());
    }
    @Test void invalidDirectoryFailsWithoutStorageOrTicket(){
        doThrow(new IllegalArgumentException("invalid folder")).when(f.placement).lockSourceLocation(99L,20L);
        String before=f.snapshot();assertThrows(RuntimeException.class,()->execute(request("Denied.pdf","bad-folder",new byte[]{1})));assertEquals(before,f.snapshot());
        verify(f.storage,never()).createFileAndReturnId(any(byte[].class),anyString(),anyString(),anyString());
    }
    @Test void physicalStorageOrTicketPersistenceFailureDoesNotReturnSuccessOrLeakNewBytes(){
        doThrow(new IllegalStateException("storage failed")).when(f.storage).createFileAndReturnId(any(byte[].class),anyString(),anyString(),anyString());
        String before=f.snapshot();assertThrows(RuntimeException.class,()->execute(request("Fail.pdf","storage-fail",new byte[]{1})));assertEquals(before,f.snapshot());
    }
    @Test void ticketInsertFailureCleansAllocatedBytesAndRetryIsComplete(){
        f.jdbc.execute("ALTER TABLE dcc_controlled_file_temporary_file ADD CONSTRAINT fail_new_ticket CHECK(original_file_name<>'New-fail.pdf')");
        String before=f.snapshot();int count=f.bytes.size();assertThrows(RuntimeException.class,()->execute(request("New-fail.pdf","ticket-fail",new byte[]{1,2})));
        assertEquals(before,f.snapshot());assertEquals(count,f.bytes.size());
        f.jdbc.execute("ALTER TABLE dcc_controlled_file_temporary_file DROP CONSTRAINT fail_new_ticket");
        assertNotNull(execute(request("New-fail.pdf","ticket-fail",new byte[]{1,2})).getUploadTicket());
    }
    @Test void lateCallerFailureRollsBackNewTicketAndStorageThenRetries(){
        String before=f.snapshot();int count=f.bytes.size();
        assertThrows(RuntimeException.class,()->f.tx.execute(s->{execute(request("Late.pdf","late-fail",new byte[]{3}));throw new IllegalStateException("late DB failure");}));
        assertEquals(before,f.snapshot());assertEquals(count,f.bytes.size());
        assertNotNull(execute(request("Late.pdf","late-fail",new byte[]{3})).getUploadTicket());
    }
    @Test void missingVersionOrTenantBypassCannotIssueAttachmentTicket(){
        var r=request("Missing.pdf","missing-version",new byte[]{1});r.setControlledFileId(null);
        assertThrows(RuntimeException.class,()->execute(r));
        TenantContextHolder.setIgnore(true);assertThrows(RuntimeException.class,()->execute(request("Ignore.pdf","ignore-tenant",new byte[]{1})));TenantContextHolder.setIgnore(false);
    }
}
