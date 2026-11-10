package cn.iocoder.yudao.module.dcc.registrationcertificate.service.file;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledPreviewWatermarkRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.*;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.*;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.*;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalService;
import cn.iocoder.yudao.module.dcc.service.file.*;
import cn.iocoder.yudao.module.dcc.service.preview.*;
import cn.iocoder.yudao.module.dcc.service.token.*;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DccRegistrationCertificateApprovalPreviewServiceTest {
    DccRegistrationCertificateApprovalPreviewService service;
    DccRegistrationCertificateApprovalService approvals;
    DccRegistrationCertificateAccessRequestFileMapper links;
    DccRegistrationCertificateFileDO business;
    FileDO source;
    DccControlledFileAccessEventDO event;
    DccControlledFileWatermarkTraceDO trace;
    FileService storage;
    boolean permission=true;
    byte[] bytes={1,2,3,4};
    final DccRequestAuditContext audit=new DccRequestAuditContext("127.0.0.1","JUnit","APPROVAL-PREVIEW-TEST");
    static void wire(Object target,String field,Object value){ReflectionTestUtils.setField(target,field,value);}
    @BeforeEach void setup() throws Exception {
        TenantContextHolder.setTenantId(1L);service=new DccRegistrationCertificateApprovalPreviewService();
        approvals=mock(DccRegistrationCertificateApprovalService.class);
        var request=new DccRegistrationCertificateAccessRequestDO();request.setId(501L);request.setTenantId(1L);request.setCertificateId(601L);request.setOwnerCompanyId(5L);
        when(approvals.requireApprovalDetailRequest(anyLong(),anyLong(),anyLong(),anyString())).thenAnswer(i->{
            if(!permission||!Objects.equals(i.getArgument(0),1L)||!Objects.equals(i.getArgument(1),99L)||!Objects.equals(i.getArgument(2),501L)||!Objects.equals(i.getArgument(3),"exact-bpm"))throw new IllegalArgumentException("EXACT_APPROVAL_AUTHORITY_REQUIRED");return request;
        });
        links=mock(DccRegistrationCertificateAccessRequestFileMapper.class);
        var link=DccRegistrationCertificateAccessRequestFileDO.builder().id(801L).requestId(501L).businessFileId(901L).fileKind("REGISTRATION_CERTIFICATE").status("REQUESTED").build();link.setTenantId(1L);
        when(links.selectByRequestId(1L,501L)).thenReturn(List.of(link));
        var businesses=mock(DccRegistrationCertificateFileMapper.class);
        business=DccRegistrationCertificateFileDO.builder().id(901L).ownerType("VERSION").ownerId(701L).fileKind("REGISTRATION_CERTIFICATE").infraFileId(1001L).originalName("审批.pdf").mimeType("application/pdf").fileSize(4L).sha256(DigestUtil.sha256Hex(bytes)).status("STAGED").build();business.setTenantId(1L);
        when(businesses.selectById(901L)).thenReturn(business);
        var versions=mock(DccRegistrationCertificateVersionMapper.class);var version=DccRegistrationCertificateVersionDO.builder().id(701L).certificateId(601L).status("DRAFT").build();version.setTenantId(1L);when(versions.selectById(701L)).thenReturn(version);
        source=new FileDO();source.setId(1001L);source.setConfigId(1L);source.setPath("staged/approval.pdf");source.setName("审批.pdf");source.setType("application/pdf");source.setSize(4L);
        var files=mock(FileMapper.class);when(files.selectById(1001L)).thenReturn(source);
        storage=mock(FileService.class);when(storage.getFileContent(eq(1L),anyString())).thenAnswer(i->bytes.clone());
        var tokens=new DccViewerTokenService();wire(tokens,"hmacSecret","isolated-approval-viewer-hmac-secret-20261030");
        var access=mock(DccControlledPreviewAccessService.class);
        when(access.prepareSourceBoundAccess(any(),any())).thenAnswer(i->{
            DccPreviewAccessRequest input=i.getArgument(0);DccPreviewSourceIdentity identity=i.getArgument(1);
            event=DccControlledFileAccessEventDO.builder().id(1101L).tenantId(input.tenantId()).accessEventCode("AE-EXACT").controlledFileId(input.fileId()).fileVersionNo(input.versionId()).processInstanceId(input.processInstanceId()).userId(input.userId()).accessType("PREVIEW").purpose(input.purpose()).result("SUCCESS").build();
            trace=DccControlledFileWatermarkTraceDO.builder().id(1201L).traceCode("WM-EXACT").accessEventId(event.getId()).accessEventCode(event.getAccessEventCode()).controlledFileId(input.fileId()).fileVersionNo(input.versionId()).userId(input.userId()).build();
            var issued=tokens.issueSourceBound(new DccViewerTokenIssueCommand(input.tenantId(),input.userId(),input.fileId(),input.versionId(),event.getId(),input.purpose(),input.ttlSeconds()),identity);
            return new DccPreviewAccessResult(event.getId(),event.getAccessEventCode(),trace.getId(),trace.getTraceCode(),issued.token(),issued.payload().getTokenId(),issued.payload().getNonce(),LocalDateTime.now(ZoneOffset.UTC),LocalDateTime.now(ZoneOffset.UTC).plusSeconds(900),"{}");
        });
        var events=mock(DccControlledFileAccessEventMapper.class);when(events.selectOne(any())).thenAnswer(i->event);
        var traces=mock(DccControlledFileWatermarkTraceMapper.class);when(traces.selectOne(any())).thenAnswer(i->trace);
        var watermarks=mock(DccControlledPreviewWatermarkService.class);when(watermarks.build(eq(99L),eq("preview"),anyString())).thenReturn(new DccControlledPreviewWatermarkRespVO());
        var properties=new DccOnlyOfficePreviewProperties();properties.setBaseUrl("https://office.example");properties.setPublicFileBaseUrl("https://app.example");properties.setJwtSecret("isolated-onlyoffice-preview-hmac-key-20261030");
        var office=new DccOnlyOfficePreviewTokenService();wire(office,"properties",properties);
        wire(service,"approvals",approvals);wire(service,"requestFiles",links);wire(service,"businessFiles",businesses);wire(service,"versions",versions);wire(service,"files",files);wire(service,"fileService",storage);wire(service,"access",access);wire(service,"tokens",tokens);wire(service,"events",events);wire(service,"traces",traces);wire(service,"watermarks",watermarks);wire(service,"officeProperties",properties);wire(service,"officeTokens",office);
    }
    @AfterEach void clear(){TenantContextHolder.clear();}
    private cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePreviewMetadataRespVO metadata(){return service.getPreviewMetadata(1L,99L,501L,"exact-bpm",901L,audit);}
    private DccControlledFileBinary read(cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePreviewMetadataRespVO metadata){return service.readPreview(1L,99L,501L,"exact-bpm",901L,metadata.getViewerToken(),metadata.getAccessEventCode(),metadata.getWatermarkTraceCode(),metadata.getViewerTokenId(),metadata.getViewerTokenNonce(),audit);}
    @Test void stagedPdfUsesSourceBoundTokenAndRechecksExactApplicationOnBinaryRead(){
        var metadata=metadata();assertEquals("PDF",metadata.getPreviewKind());assertNull(metadata.getOnlyofficeDocumentUrl());assertArrayEquals(bytes,read(metadata).bytes());
        verify(approvals,times(2)).requireApprovalDetailRequest(1L,99L,501L,"exact-bpm");assertEquals("exact-bpm",event.getProcessInstanceId());assertEquals(64,event.getFileVersionNo().length());
    }
    @Test void unrelatedFileRequesterProcessAndCrossTenantCannotGetMetadata(){
        assertThrows(ServiceException.class,()->service.getPreviewMetadata(1L,99L,501L,"exact-bpm",902L,audit));
        assertThrows(IllegalArgumentException.class,()->service.getPreviewMetadata(1L,88L,501L,"exact-bpm",901L,audit));
        assertThrows(IllegalArgumentException.class,()->service.getPreviewMetadata(1L,99L,501L,"another-bpm",901L,audit));
        TenantContextHolder.setTenantId(2L);assertThrows(ServiceException.class,()->metadata());verifyNoInteractions(storage);
    }
    @Test void removedFrozenReferenceDeniesPreview(){
        when(links.selectByRequestId(1L,501L)).thenReturn(List.of());assertThrows(ServiceException.class,()->metadata());
        verifyNoInteractions(storage);
    }
    @Test void tokenCannotBeMovedAcrossApprovalEventsAndChangedBytesAreRejected(){
        var metadata=metadata();event.setProcessInstanceId("another-bpm");assertThrows(ServiceException.class,()->read(metadata));
        event.setProcessInstanceId("exact-bpm");bytes=new byte[]{4,3,2,1};assertThrows(ServiceException.class,()->read(metadata));
    }
    @Test void revokedAuthorityIsCheckedAgainBeforeReadingAnyBytes(){
        var metadata=metadata();permission=false;assertThrows(IllegalArgumentException.class,()->read(metadata));verifyNoInteractions(storage);
    }
    private String officeToken(){business.setOriginalName("审批.docx");business.setMimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");source.setName(business.getOriginalName());source.setType(business.getMimeType());var metadata=metadata();assertEquals("OFFICE",metadata.getPreviewKind());return metadata.getOnlyofficeDocumentUrl().split("token=",2)[1];}
    @Test void officeCallbackUsesSignedDurableContextAndRestoresTenant(){
        String token=officeToken();TenantContextHolder.clear();TenantContextHolder.setIgnore(true);
        assertArrayEquals(bytes,service.readOnlyOffice(501L,"exact-bpm",901L,1001L,token).bytes());assertNull(TenantContextHolder.getTenantId());assertTrue(TenantContextHolder.isIgnore());
        assertThrows(IllegalArgumentException.class,()->service.readOnlyOffice(501L,"another-bpm",901L,1001L,token));assertNull(TenantContextHolder.getTenantId());assertTrue(TenantContextHolder.isIgnore());
    }
    @Test void officeCallbackRejectsRevokedCandidateAndChangedSourceIdentity(){
        String token=officeToken();TenantContextHolder.setIgnore(true);permission=false;assertThrows(IllegalArgumentException.class,()->service.readOnlyOffice(501L,"exact-bpm",901L,1001L,token));
        permission=true;source.setPath("replaced/approval.docx");assertThrows(ServiceException.class,()->service.readOnlyOffice(501L,"exact-bpm",901L,1001L,token));verifyNoInteractions(storage);
    }
    @Test void officeCallbackRejectsExistingConflictingTenantAndTamperedInfraBeforeLookingUpMaterials(){
        String token=officeToken();clearInvocations(approvals,links);TenantContextHolder.setTenantId(2L);TenantContextHolder.setIgnore(true);
        assertThrows(ServiceException.class,()->service.readOnlyOffice(501L,"exact-bpm",901L,1001L,token));assertEquals(2L,TenantContextHolder.getTenantId());assertTrue(TenantContextHolder.isIgnore());
        TenantContextHolder.clear();assertThrows(ServiceException.class,()->service.readOnlyOffice(501L,"exact-bpm",901L,1002L,token));assertNull(TenantContextHolder.getTenantId());verifyNoInteractions(approvals,links,storage);
    }
}
