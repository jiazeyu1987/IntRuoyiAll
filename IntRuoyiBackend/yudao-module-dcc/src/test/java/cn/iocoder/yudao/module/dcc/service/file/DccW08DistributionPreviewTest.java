package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.*;
import cn.iocoder.yudao.module.dcc.service.preview.*;
import cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenExpectedContext;
import cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenService;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.infra.service.file.access.*;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Actual Query ingress and exact guard; crypto/storage/audit external ports remain explicit unit doubles. */
class DccW08DistributionPreviewTest {
    DccControlledFileQueryServiceImpl query;
    DccControlledFileDO file;
    DccControlledFileDistributionDO distribution;
    DccControlledFileDistributionRecipientDO recipient;
    DccControlledFileMessageJobDO job;
    DccControlledFileMapper files;
    DccControlledFileMasterMapper masters;
    DccControlledFileMessageJobMapper jobs;
    DccControlledPreviewAccessService preview;
    DccViewerTokenService tokens;
    FileService storage;
    DccDistributionReceiptService bodyReceipts;
    final LocalDateTime time=LocalDateTime.of(2026,10,11,10,0);

    @BeforeEach void setup() throws Exception {
        TenantContextHolder.setTenantId(1L);
        query=new DccControlledFileQueryServiceImpl();
        for(var field:DccControlledFileQueryServiceImpl.class.getDeclaredFields()) {
            if(!Modifier.isStatic(field.getModifiers()) && field.getAnnotation(Resource.class)!=null)
                ReflectionTestUtils.setField(query,field.getName(),mock(field.getType()));
        }
        file=DccControlledFileDO.builder().id(5L).tenantId(1L).masterId(100L).categoryId(2L).dccProjectCodeId(9L)
                .status("ACTIVE").versionNo("A/1").processInstanceId("issued-round").fileNumber("SOP-1").publishedFileId(500L)
                .controlledTime(time).activatedTime(time).distributedTime(time).build();
        distribution=DccControlledFileDistributionDO.builder().id(10L).tenantId(1L).controlledFileId(5L)
                .distributionMedium("PUBLIC_FOLDER").status("PENDING").build();
        recipient=DccControlledFileDistributionRecipientDO.builder().id(11L).tenantId(1L).distributionId(10L)
                .userId(99L).messageJobId(12L).build();
        job=DccControlledFileMessageJobDO.builder().id(12L).tenantId(1L).businessType("DISTRIBUTION")
                .businessId(10L).recipientUserId(99L).status("SENT").sentAt(time).build();
        files=port("controlledFileMapper");masters=port("controlledFileMasterMapper");jobs=port("messageJobMapper");
        when(files.selectById(5L)).thenReturn(file);
        when(((DccControlledFileDistributionMapper)port("distributionMapper")).selectById(10L)).thenReturn(distribution);
        when(((DccControlledFileDistributionRecipientMapper)port("distributionRecipientMapper")).selectById(11L)).thenReturn(recipient);
        when(jobs.selectById(12L)).thenReturn(job);
        when(((AdminUserApi)port("adminUserApi")).getUser(99L)).thenReturn(new AdminUserRespDTO().setId(99L).setTenantId(1L).setStatus(0));
        when(((FileMapper)port("fileMapper")).selectById(500L)).thenReturn(FileDO.builder().id(500L).configId(1L)
                .name("issued-A1.pdf").type("application/pdf").path("issued/A1.pdf").build());
        preview=port("previewAccessService");tokens=port("viewerTokenService");storage=port("fileService");
        bodyReceipts=port("distributionReceiptService");
        when(((DccControlledFileAccessLogMapper)port("accessLogMapper")).insert(any(DccControlledFileAccessLogDO.class))).thenReturn(1);
        when(preview.prepareAccess(any())).thenReturn(new DccPreviewAccessResult(30L,"event",31L,"trace","token","tid","nonce",time,time.plusMinutes(15),"{}"));
        doReturn(new byte[]{1,2,3}).when(storage).getFileContent(1L,"issued/A1.pdf");
        var access=DccControlledFileAccessEventDO.builder().id(30L).accessEventCode("event").controlledFileId(5L)
                .userId(99L).fileVersionNo("A/1").accessType("PREVIEW").purpose("DIST_PREVIEW:10:11").result("SUCCESS").build();
        var watermark=DccControlledFileWatermarkTraceDO.builder().id(31L).traceCode("trace").accessEventId(30L)
                .accessEventCode("event").controlledFileId(5L).userId(99L).fileVersionNo("A/1").build();
        when(((DccControlledFileAccessEventMapper)port("accessEventMapper")).selectOne(any())).thenReturn(access);
        when(((DccControlledFileWatermarkTraceMapper)port("watermarkTraceMapper")).selectOne(any())).thenReturn(watermark);
        BusinessFileAccessService business=port("businessFileAccessService");
        when(business.assertAllowed(any())).thenAnswer(call->{
            BusinessFileAccessRequest request=call.getArgument(0);
            assertEquals(500L,request.fileId());
            query.assertBusinessFileAccess(request.userId(),5L,request.operation(),new DccRequestAuditContext("127.0.0.1","unit-fixture","request"));
            return Optional.of(new BusinessFileAccessReference("dcc","DCC_CONTROLLED_FILE",5L,"A/1",1L,null));
        });
    }
    @AfterEach void clear(){TenantContextHolder.clear();}
    @SuppressWarnings("unchecked") private <T>T port(String field){return (T)ReflectionTestUtils.getField(query,field);}
    private DccRequestAuditContext audit(){return new DccRequestAuditContext("127.0.0.1","unit-fixture","request");}

    @Test void metadataBindsExactTaskWhileBusinessGuardRechecksAndDoesNotResolveMaster() {
        var result=query.getDistributionPreviewMetadata(99L,5L,10L,11L,audit());
        assertEquals("PDF",result.getPreviewKind());assertEquals("token",result.getViewerToken());
        assertEquals(5L,result.getControlledFileId());assertEquals(10L,result.getDistributionId());assertEquals(11L,result.getRecipientId());
        assertEquals("A/1",result.getVersionNo());assertEquals("issued-round",result.getApprovalProcessInstanceId());
        verify(preview).prepareAccess(argThat(request->request.fileId().equals(5L)&&request.versionId().equals("A/1")
                &&request.purpose().equals("DIST_PREVIEW:10:11")));
        verifyNoInteractions(masters);
        verifyNoInteractions(bodyReceipts);
        assertNull(((ThreadLocal<?>)ReflectionTestUtils.getField(query,"distributionPreviewContext")).get());
    }
    @Test void taskDetailHasOnlyExactSelectedVersionAndNoMaintenanceOrParentHistory() {
        var result=query.getDistributionPreviewDetail(99L,5L,10L,11L);
        assertEquals(5L,result.getId());assertEquals("A/1",result.getVersionNo());assertTrue(result.getCanPreview());
        assertFalse(result.getCanDownload());assertFalse(result.getCanPrint());assertFalse(result.getCanObsolete());
        assertEquals(java.util.List.of("PREVIEW"),result.getActionProjection().getAllowedActions());
        assertNull(result.getVersionHistory());assertNull(result.getAttachments());verifyNoInteractions(masters,preview,storage);
    }
    @Test void readsIssuedPublishedBodyAndSavedEventRestoresOnlyThisTask() throws Exception {
        var result=query.readDistributionPreviewFile(99L,5L,10L,11L,"token","event","trace","tid","nonce",audit());
        assertArrayEquals(new byte[]{1,2,3},result.bytes());assertEquals("issued-A1.pdf",result.fileName());
        verify(tokens).verify(eq("token"),argThat((DccViewerTokenExpectedContext c)->c.purpose().equals("DIST_PREVIEW:10:11")
                &&c.fileId().equals(5L)&&c.userId().equals(99L)));
        query.readDistributionPreviewFile(99L,5L,10L,11L,"token","event","trace","tid","nonce",audit());
        // A distribution token is not a grant for the separate DAILY body-read entry.
        var denied=assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                ()->query.readPreviewFile(99L,5L,"token","event","trace","tid","nonce",audit()));
        assertEquals(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED.getCode(),denied.getCode());
        verify(storage,times(2)).getFileContent(1L,"issued/A1.pdf");verifyNoInteractions(masters);
        verify(bodyReceipts,times(2)).recordDistributionBodyOpened(99L,5L,10L,11L);
        assertNull(((ThreadLocal<?>)ReflectionTestUtils.getField(query,"distributionPreviewContext")).get());
        assertNull(((ThreadLocal<?>)ReflectionTestUtils.getField(query,"bodyReadContext")).get());
    }
    @Test void anotherRecipientUnsentAndNoActivationNeverReadOrIssueToken() {
        recipient.setUserId(98L);
        assertThrows(RuntimeException.class,()->query.getDistributionPreviewMetadata(99L,5L,10L,11L,audit()));
        recipient.setUserId(99L);job.setStatus("PENDING");
        assertThrows(RuntimeException.class,()->query.getDistributionPreviewMetadata(99L,5L,10L,11L,audit()));
        job.setStatus("SENT");file.setActivatedTime(null);
        assertThrows(RuntimeException.class,()->query.getDistributionPreviewMetadata(99L,5L,10L,11L,audit()));
        verifyNoInteractions(storage,preview,tokens,masters,bodyReceipts);
    }
    @Test void signedTaskPurposeCannotBeUsedForAnotherTaskAndScopeClears() {
        distribution.setId(13L);recipient.setDistributionId(13L);job.setBusinessId(13L);
        when(((DccControlledFileDistributionMapper)port("distributionMapper")).selectById(13L)).thenReturn(distribution);
        assertThrows(RuntimeException.class,()->query.readDistributionPreviewFile(99L,5L,13L,11L,"token","event","trace","tid","nonce",audit()));
        verifyNoInteractions(storage);
        assertNull(((ThreadLocal<?>)ReflectionTestUtils.getField(query,"distributionPreviewContext")).get());
    }
    @Test void unconfiguredOfficeTaskPreviewReportsRealUnavailableReasonWithoutFakeToken() {
        when(((FileMapper)port("fileMapper")).selectById(500L)).thenReturn(FileDO.builder().id(500L).configId(1L)
                .name("issued-A1.docx").type("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .path("issued/A1.docx").build());
        var result=query.getDistributionPreviewMetadata(99L,5L,10L,11L,audit());
        assertEquals("OFFICE",result.getPreviewKind());assertNotNull(result.getPreviewUnavailableReason());
        assertNull(result.getViewerToken());assertNull(result.getOnlyofficeDocumentUrl());
        assertEquals(5L,result.getControlledFileId());assertEquals(10L,result.getDistributionId());assertEquals(11L,result.getRecipientId());
        assertEquals("A/1",result.getVersionNo());assertEquals("issued-round",result.getApprovalProcessInstanceId());
        verifyNoInteractions(preview,tokens,storage,bodyReceipts);
    }
    @Test void configuredOfficeIssuesRealTaskTokenAndCallbackRechecksTaskBeforeExactPublishedBytes() throws Exception {
        var signedService=configureRealOffice();
        var result=query.getDistributionPreviewMetadata(99L,5L,10L,11L,audit());
        assertNull(result.getPreviewUnavailableReason());assertNotNull(result.getOnlyofficeDocumentUrl());
        String token=result.getOnlyofficeDocumentUrl().split("token=",2)[1];
        var issued=signedService.verifyControlledFile(token,5L);
        assertEquals("DIST_PREVIEW:10:11",issued.getPurpose());assertEquals(500L,issued.getInfraFileId());
        var bytes=query.readOnlyOfficePreviewFile(5L,token,audit());assertArrayEquals(new byte[]{4,5,6},bytes.bytes());
        verifyNoInteractions(bodyReceipts);
        assertNull(((ThreadLocal<?>)ReflectionTestUtils.getField(query,"distributionPreviewContext")).get());verifyNoInteractions(masters);
        job.setStatus("FAILED");assertThrows(RuntimeException.class,()->query.readOnlyOfficePreviewFile(5L,token,audit()));
        verify(storage,times(1)).getFileContent(1L,"issued/A1.docx");
        verifyNoInteractions(bodyReceipts);
        assertNull(((ThreadLocal<?>)ReflectionTestUtils.getField(query,"distributionPreviewContext")).get());
        assertEquals(1L,TenantContextHolder.getRequiredTenantId());
    }
    DccOnlyOfficePreviewTokenService configureRealOffice() throws Exception {
        var properties=new DccOnlyOfficePreviewProperties();properties.setBaseUrl("https://office.local");
        properties.setPublicFileBaseUrl("https://files.local");properties.setJwtSecret("real-unit-HMAC-secret");
        var signedService=new DccOnlyOfficePreviewTokenService();ReflectionTestUtils.setField(signedService,"properties",properties);
        ReflectionTestUtils.setField(query,"onlyOfficePreviewProperties",properties);ReflectionTestUtils.setField(query,"onlyOfficePreviewTokenService",signedService);
        when(((FileMapper)port("fileMapper")).selectById(500L)).thenReturn(FileDO.builder().id(500L).configId(1L)
                .name("issued-A1.docx").type("application/vnd.openxmlformats-officedocument.wordprocessingml.document").path("issued/A1.docx").build());
        when(storage.getFileContent(1L,"issued/A1.docx")).thenReturn(new byte[]{4,5,6});
        var access=DccControlledFileAccessEventDO.builder().id(30L).accessEventCode("event").controlledFileId(5L)
                .userId(99L).fileVersionNo("A/1").accessType("PREVIEW").purpose("DIST_PREVIEW:10:11").result("SUCCESS").build();
        when(((DccControlledFileAccessEventMapper)port("accessEventMapper")).selectById(30L)).thenReturn(access);
        return signedService;
    }
    @Test void byteReadFailureAndOpenedWriteFailureCannotProduceSuccessfulReceiptOrBinary() throws Exception {
        when(storage.getFileContent(1L,"issued/A1.pdf")).thenThrow(new IllegalStateException("storage unavailable"));
        assertThrows(RuntimeException.class,()->query.readDistributionPreviewFile(99L,5L,10L,11L,"token","event","trace","tid","nonce",audit()));
        verifyNoInteractions(bodyReceipts);
        doReturn(new byte[]{1,2,3}).when(storage).getFileContent(1L,"issued/A1.pdf");
        doThrow(new IllegalStateException("opened fact failed")).when(bodyReceipts).recordDistributionBodyOpened(99L,5L,10L,11L);
        assertEquals("opened fact failed",assertThrows(IllegalStateException.class,()->query.readDistributionPreviewFile(99L,5L,10L,11L,"token","event","trace","tid","nonce",audit())).getMessage());
        assertNull(((ThreadLocal<?>)ReflectionTestUtils.getField(query,"distributionPreviewContext")).get());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"PDF,issued.pdf,application/pdf","IMAGE,issued.png,image/png",
            "TEXT,issued.txt,text/plain","AUDIO,issued.mp3,audio/mpeg","VIDEO,issued.mp4,video/mp4"})
    void eachReadableBodyFormatRecordsOpenedOnlyAfterExactBytes(String kind,String name,String mime) throws Exception {
        when(((FileMapper)port("fileMapper")).selectById(500L)).thenReturn(FileDO.builder().id(500L).configId(1L)
                .name(name).type(mime).path("issued/A1.pdf").build());
        var metadata=query.getDistributionPreviewMetadata(99L,5L,10L,11L,audit());
        assertEquals(kind,metadata.getPreviewKind());verifyNoInteractions(bodyReceipts,storage);
        var result=query.readDistributionPreviewFile(99L,5L,10L,11L,"token","event","trace","tid","nonce",audit());
        assertArrayEquals(new byte[]{1,2,3},result.bytes());assertEquals(mime,result.contentType());
        verify(bodyReceipts,times(1)).recordDistributionBodyOpened(99L,5L,10L,11L);
        assertNull(((ThreadLocal<?>)ReflectionTestUtils.getField(query,"distributionPreviewContext")).get());
    }
    @Test void tokenVerificationFailureNeverReadsStorageOrRecordsOpened() {
        when(tokens.verify(eq("token"),any())).thenThrow(new IllegalArgumentException("invalid signature"));
        assertThrows(IllegalArgumentException.class,()->query.readDistributionPreviewFile(99L,5L,10L,11L,"token","event","trace","tid","nonce",audit()));
        verifyNoInteractions(storage,bodyReceipts);
        assertNull(((ThreadLocal<?>)ReflectionTestUtils.getField(query,"distributionPreviewContext")).get());
    }
    @Test void anonymousOfficeCallbackReadsAndStoresProofWithoutInvokingHumanReceiptOrInventingLogin() throws Exception {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        configureRealOffice();
        var actual=new DccDistributionReceiptServiceImpl();
        ReflectionTestUtils.setField(actual,"adminUserApi",port("adminUserApi"));
        ReflectionTestUtils.setField(query,"distributionReceiptService",actual);
        String token=query.getDistributionPreviewMetadata(99L,5L,10L,11L,audit()).getOnlyofficeDocumentUrl().split("token=",2)[1];
        assertArrayEquals(new byte[]{4,5,6},query.readOnlyOfficePreviewFile(5L,token,audit()).bytes());
        verify(storage,times(1)).getFileContent(1L,"issued/A1.docx");
        assertNull(cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUser());
        assertNull(((ThreadLocal<?>)ReflectionTestUtils.getField(query,"distributionPreviewContext")).get());
        assertEquals(1L,TenantContextHolder.getRequiredTenantId());
    }
}
