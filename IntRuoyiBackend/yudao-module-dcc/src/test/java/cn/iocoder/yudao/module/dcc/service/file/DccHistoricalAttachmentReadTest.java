package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePreviewMetadataRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccCategoryViewMatrixRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryPermissionRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccCategoryViewMatrixRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryPermissionRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.*;
import cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.dcc.service.download.DccDownloadPolicyService;
import cn.iocoder.yudao.module.dcc.service.download.DccDownloadFileBinary;
import cn.iocoder.yudao.module.dcc.service.file.access.DccBusinessFileAccessProvider;
import cn.iocoder.yudao.module.dcc.service.preview.DccControlledPreviewAccessService;
import cn.iocoder.yudao.module.dcc.service.preview.DccPreviewAccessRequest;
import cn.iocoder.yudao.module.dcc.service.preview.DccPreviewAccessResult;
import cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenExpectedContext;
import cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenService;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.infra.service.file.access.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import jakarta.annotation.Resource;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** C23: real Query, detail guard, category/view qualification, attachment ownership and download policy. */
class DccHistoricalAttachmentReadTest {
    private static final long READER=99L, AUTHOR=77L, A=20L, B=21L, MASTER=700L, PROJECT=4L, CATEGORY=2L, DIRECTORY=3L;
    private static final LocalDateTime A_TIME=LocalDateTime.of(2026,10,18,16,23,2);
    private static final LocalDateTime B_TIME=LocalDateTime.of(2026,10,18,18,47,28);
    private static final DccRequestAuditContext AUDIT=new DccRequestAuditContext("127.0.0.1","C23 historical unit test","c23-preview");
    private DccControlledFileQueryServiceImpl query;
    private DccControlledFileDetailAuthorizationGuard guard;
    private DccControlledFileCategoryPermissionSupport permissions;
    private DccControlledFileViewMatrixAccessService viewMatrix;
    private DccControlledFileAttachmentServiceImpl attachments;
    private DccControlledFileMapper files;
    private DccControlledFileAttachmentMapper attachmentRows;
    private FileMapper binaries;
    private FileService storage;
    private DccControlledFileDO oldVersion,currentVersion;
    private final Map<Long,DccControlledFileAttachmentDO> rows=new LinkedHashMap<>();
    private final Map<Long,FileDO> physical=new LinkedHashMap<>();
    private final Map<Long,Long> storageOwners=new LinkedHashMap<>();
    private final Map<String,byte[]> bytes=new LinkedHashMap<>();
    private final List<BusinessFileAccessRequest> businessChecks=new ArrayList<>();
    private boolean viewAllowed=true,downloadAllowed,projectAllowed=true,assignmentRestricted,businessDenied;
    private DccControlledFileAccessEventDO previewEvent;
    private DccControlledFileWatermarkTraceDO previewTrace;

    @BeforeEach
    @SuppressWarnings({"unchecked","rawtypes"})
    void setup() {
        TenantContextHolder.setTenantId(1L);TenantContextHolder.setIgnore(false);
        var login=new LoginUser();login.setId(READER);login.setTenantId(1L);login.setUserType(2);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login,null,List.of()));
        query=withMockPorts(new DccControlledFileQueryServiceImpl());
        guard=withMockPorts(new DccControlledFileDetailAuthorizationGuard());
        permissions=withMockPorts(new DccControlledFileCategoryPermissionSupport());
        viewMatrix=withMockPorts(new DccControlledFileViewMatrixAccessService());
        attachments=withMockPorts(new DccControlledFileAttachmentServiceImpl());
        var assignments=withMockPorts(new DccControlledFileAssignmentScopeService());
        var api=port(query,"adminUserApi",AdminUserApi.class);
        var permissionApi=port(query,"permissionApi",PermissionApi.class);
        var directory=port(query,"directoryAccessPermissionService",DccDirectoryAccessPermissionService.class);
        var projects=port(query,"projectAccessService",DccProjectAccessService.class);
        var user=new AdminUserRespDTO().setId(READER).setTenantId(1L).setStatus(0).setUsername("C23Reader").setNickname("C23Reader");
        when(api.getUser(READER)).thenReturn(user);
        when(projects.listReadableProjectIds(READER)).thenAnswer(i->projectAllowed?List.of(PROJECT):List.of());
        when(directory.getAuthorizedDirectoryIds(eq(READER),any())).thenAnswer(i->switch(i.<DccAccessTypeEnum>getArgument(1)) {
            case QUERY -> Set.of(DIRECTORY); // Name-only query is deliberately not a VIEW grant.
            case PREVIEW -> viewAllowed?Set.of(DIRECTORY):Set.of();
            case DOWNLOAD -> downloadAllowed?Set.of(DIRECTORY):Set.of();
            default -> Set.of();
        });
        when(permissionApi.hasAnyPermissions(READER,"dcc:project-code-assignment:execute")).thenAnswer(i->assignmentRestricted);
        set(assignments,"permissionApi",permissionApi);
        for(Object target:List.of(guard,permissions,viewMatrix))set(target,"adminUserApi",api);
        set(permissions,"permissionApi",permissionApi);
        var categoryRules=port(permissions,"permissionRuleMapper",DccFileCategoryPermissionRuleMapper.class);
        when(categoryRules.selectList(any(SFunction.class),eq(CATEGORY))).thenAnswer(i->downloadAllowed?List.of(downloadRule()):List.of());
        var viewRules=port(viewMatrix,"ruleMapper",DccCategoryViewMatrixRuleMapper.class);
        when(viewRules.selectActiveListByCategoryId(CATEGORY)).thenAnswer(i->viewAllowed?List.of(viewRule()):List.of());
        set(guard,"permissionSupport",permissions);set(guard,"viewMatrixAccessService",viewMatrix);
        set(guard,"assignmentScopeService",assignments);set(guard,"directoryAccessPermissionService",directory);set(guard,"projectAccessService",projects);
        set(query,"detailAuthorizationGuard",guard);set(query,"permissionSupport",permissions);set(query,"viewMatrixAccessService",viewMatrix);
        set(query,"assignmentScopeService",assignments);set(query,"downloadPolicyService",new DccDownloadPolicyService());set(query,"attachmentService",attachments);
        files=port(query,"controlledFileMapper",DccControlledFileMapper.class);
        binaries=port(query,"fileMapper",FileMapper.class);storage=port(query,"fileService",FileService.class);
        set(guard,"masterMapper",port(query,"controlledFileMasterMapper",DccControlledFileMasterMapper.class));
        oldVersion=version(A,"A/1","OBSOLETE",A_TIME);currentVersion=version(B,"B/1","ACTIVE",B_TIME);
        oldVersion.setSupersededByFileId(B);currentVersion.setRevisionSourceControlledFileId(A);
        when(files.selectById(A)).thenReturn(oldVersion);when(files.selectById(B)).thenReturn(currentVersion);
        when(files.selectListByMasterId(MASTER)).thenReturn(List.of(oldVersion,currentVersion));
        var master=DccControlledFileMasterDO.builder().id(MASTER).tenantId(1L).dccProjectCodeId(PROJECT).categoryId(CATEGORY)
                .fileTypeTaxonomyLeafId(66L).normalizedFileNumber("C23-HISTORY").currentActiveControlledFileId(B).latestControlledFileId(B).build();
        when(port(query,"controlledFileMasterMapper",DccControlledFileMasterMapper.class).selectById(MASTER)).thenReturn(master);
        var project=DccProjectCodeDO.builder().id(PROJECT).projectName("C23 project").projectCode("C23").status("ENABLE").build();project.setTenantId(1L);
        when(port(query,"projectCodeMapper",cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper.class).selectById(PROJECT)).thenReturn(project);
        attachmentRows=port(attachments,"attachmentMapper",DccControlledFileAttachmentMapper.class);
        var builder=new MapperBuilderAssistant(new MybatisConfiguration(),"c23-historical-attachment");
        builder.setCurrentNamespace(DccControlledFileAttachmentMapper.class.getName());TableInfoHelper.initTableInfo(builder,DccControlledFileAttachmentDO.class);
        addAttachment(700L,A,110L,"c23-A-notes.txt","ATTACHMENT",A_TIME,"C23 ordinary A");
        addAttachment(701L,A,111L,"c23-A-training.pdf","TRAINING_RECORD",A_TIME.plusMinutes(30),"%PDF-1.4\nC23 training A");
        addAttachment(710L,B,120L,"c23-B-notes.txt","ATTACHMENT",B_TIME,"C23 ordinary B");
        addAttachment(711L,B,121L,"c23-B-training.pdf","TRAINING_RECORD",B_TIME.plusMinutes(30),"%PDF-1.4\nC23 training B");
        oldVersion.setTrainingRecordFileId(111L);currentVersion.setTrainingRecordFileId(121L);
        addPhysical(44L,A,"c23-A-body.pdf","%PDF-1.4\nC23 main A");addPhysical(45L,B,"c23-B-body.pdf","%PDF-1.4\nC23 main B");
        when(attachmentRows.selectById(anyLong())).thenAnswer(i->rows.get(i.<Long>getArgument(0)));
        when(attachmentRows.selectListByControlledFileId(anyLong())).thenAnswer(i->rows.values().stream()
                .filter(r->Objects.equals(r.getControlledFileId(),i.getArgument(0))).toList());
        when(attachmentRows.selectList(any(Wrapper.class))).thenAnswer(i->{
            AbstractWrapper<?,?,?> wrapper=i.getArgument(0);wrapper.getSqlSegment();
            return rows.values().stream().filter(r->wrapper.getParamNameValuePairs().containsValue(r.getStorageFileId())).toList();
        });
        when(binaries.selectById(anyLong())).thenAnswer(i->physical.get(i.<Long>getArgument(0)));
        try { when(storage.getFileContent(eq(1L),anyString())).thenAnswer(i->bytes.get(i.<String>getArgument(1))); }
        catch(Exception ex) { throw new AssertionError(ex); }
        wireBusinessAndPreviewPorts();
    }

    @AfterEach void clearIdentity() { SecurityContextHolder.clearContext();TenantContextHolder.clear(); }

    @Test void currentBIsARealPositiveControlWithoutObsoleteAuthority() {
        var detail=query.getControlledFile(READER,B);
        assertEquals(B,detail.getId());assertEquals("B/1",detail.getVersionNo());assertEquals("B/1",detail.getCurrentActiveVersionNo());
        assertEquals(List.of(710L,711L),detail.getAttachments().stream().map(r->r.getAttachmentId()).toList());
        assertFalse(Boolean.TRUE.equals(detail.getCanObsolete()));assertFalse(detail.getActionProjection().getAllowedActions().contains("OBSOLETE"));
        assertFalse(permissions.hasCategoryPermission(CATEGORY,READER,DccFileCategoryPermissionActionEnum.OBSOLETE));
        var metadata=query.getAttachmentPreviewMetadata(READER,B,710L,AUDIT);
        assertArrayEquals(content(120L),preview(B,710L,metadata).bytes());
        downloadAllowed=true;assertArrayEquals(content(120L),download(B,710L).bytes());
        assertEquals(3,businessChecks.size(),"Metadata, binary and download must each traverse the real provider callback");
    }

    @Test void sharedMetadataAndPaperRecordsAcceptRetainedViewWithoutObsoleteAction() {
        var paper=withMockPorts(new DccPaperDistributionAckServiceImpl());
        set(paper,"controlledFileMapper",files);set(paper,"detailAuthorizationGuard",guard);
        assertTrue(guard.isAllowed(READER,oldVersion), "shared metadata reads must accept the same retained-version VIEW qualification");
        assertTrue(paper.getPaperDistributionRecords(READER,A).isEmpty());
        assertFalse(permissions.hasCategoryPermission(CATEGORY,READER,DccFileCategoryPermissionActionEnum.OBSOLETE));
        viewAllowed=false;
        assertFalse(guard.isAllowed(READER,oldVersion));
        assertDenied(()->paper.getPaperDistributionRecords(READER,A));
        verifyNoInteractions(storage);
    }

    @Test void currentDetailsListExactObsoleteAWithoutGrantingObsoleteActions() {
        var detail=query.getControlledFile(READER,B);
        assertEquals(B,detail.getId(),"B detail must work before the intended missing-A assertion");
        var history=detail.getVersionHistory().stream().filter(r->Objects.equals(A,r.getId())).toList();
        assertEquals(1,history.size(),"VIEW reader without OBSOLETE must retain the exact A/1 history entry");
        var a=history.get(0);assertEquals("A/1",a.getVersionNo());assertEquals("OBSOLETE",a.getStatus());assertEquals("B/1",a.getCurrentActiveVersionNo());
        assertFalse(a.getActionProjection().getAllowedActions().contains("OBSOLETE"));
    }

    @Test void frozenRelationKeepsItsReadableObsoleteVersionWithoutDailyOrEditAccess() {
        set(query,"versionPolicy",DccControlledFileVersionPolicy.defaultPolicy());
        assertDoesNotThrow(()->query.assertRelationNameVisible(READER,A));
        var old=query.getRelationPermissions(READER,A);
        assertEquals(A,old.controlledFileId());assertEquals("A/1",old.versionNo());assertEquals("OBSOLETE",old.status());
        assertTrue(old.canPreview());assertFalse(old.canEdit());
        assertDoesNotThrow(()->query.assertRelationContentReadable(READER,A));
        assertDenied(()->query.assertRelationEditable(READER,A));
        assertDenied(()->query.getPreviewMetadata(READER,A,DccControlledFileBodyReadContext.daily(A),AUDIT));
        verifyNoInteractions(storage);
    }

    @ParameterizedTest @ValueSource(strings={"no-view","foreign-project","assignment-scope"})
    void frozenRelationNeverBypassesHistoricalReadQualification(String boundary) {
        if("no-view".equals(boundary))viewAllowed=false;
        if("foreign-project".equals(boundary))projectAllowed=false;
        if("assignment-scope".equals(boundary))assignmentRestricted=true;
        assertDenied(()->query.assertRelationNameVisible(READER,A));
        assertDenied(()->query.getRelationPermissions(READER,A));
        assertDenied(()->query.assertRelationContentReadable(READER,A));
        verifyNoInteractions(storage);
    }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void historicalMetadataKeepsAOrdinaryAndTrainingFacts(boolean explicitHistory) {
        downloadAllowed=explicitHistory;
        DccControlledFileRespVO result;
        if(explicitHistory){
            var reader=withMockPorts(new DccSelectedDetailReadService());set(reader,"files",files);set(reader,"query",query);set(reader,"guard",guard);
            result=reader.read(READER,A,historyContext());
        } else result=query.getControlledFile(READER,A); // The real readonly-record route also uses this metadata entry.
        assertEquals(A,result.getId());assertEquals("A/1",result.getVersionNo());assertEquals("OBSOLETE",result.getStatus());
        assertEquals(List.of(700L,701L),result.getAttachments().stream().map(r->r.getAttachmentId()).toList());
        for(var row:result.getAttachments()){
            var original=rows.get(row.getAttachmentId());assertEquals(A,row.getControlledFileId());assertEquals(original.getOriginalFileName(),row.getFileName());
            assertEquals(original.getAttachmentPurpose(),row.getAttachmentPurpose());assertEquals(AUTHOR,row.getUploadedBy());
            assertEquals(original.getUploadedAt(),row.getUploadedAt());assertEquals("round-A",row.getApplicationProcessInstanceId());
            assertEquals(Boolean.valueOf(downloadAllowed),row.getCanDownload(),"Attachment qualification must not use the main-body download flag");
        }
        assertFalse(Boolean.TRUE.equals(result.getCanDownload()));assertFalse(result.getActionProjection().getAllowedActions().contains("OBSOLETE"));
        verifyNoInteractions(storage);
    }

    @ParameterizedTest @ValueSource(longs={700,701})
    void historicalPreviewChecksMetadataAndBinaryThroughRealBusinessGate(long attachmentId) {
        var metadata=query.getAttachmentPreviewMetadata(READER,A,attachmentId,AUDIT);
        assertEquals(rows.get(attachmentId).getOriginalFileName(),metadata.getFileName());
        assertEquals(1,businessChecks.size());var result=preview(A,attachmentId,metadata);
        assertArrayEquals(content(rows.get(attachmentId).getStorageFileId()),result.bytes());assertEquals(2,businessChecks.size());
        var expected=ArgumentCaptor.forClass(DccViewerTokenExpectedContext.class);
        verify(port(query,"viewerTokenService",DccViewerTokenService.class)).verify(eq(metadata.getViewerToken()),expected.capture());
        assertEquals(A,expected.getValue().fileId());assertEquals("A/1",expected.getValue().versionId());assertEquals(READER,expected.getValue().userId());
        assertDenied(()->query.assertBusinessFileAccess(READER,A,BusinessFileAccessOperation.PREVIEW,AUDIT)); // Local attachment scope was cleared.
    }

    @ParameterizedTest @ValueSource(longs={700,701})
    void historicalDownloadUsesDownloadQualificationAndAIdentity(long attachmentId) {
        downloadAllowed=true;var result=download(A,attachmentId);var original=rows.get(attachmentId);
        assertEquals(original.getOriginalFileName(),result.fileName());assertArrayEquals(content(original.getStorageFileId()),result.bytes());
        assertEquals(cn.hutool.crypto.digest.DigestUtil.sha256Hex(result.bytes()),result.plainSha256());assertEquals(1,businessChecks.size());
        var event=ArgumentCaptor.forClass(DccControlledFileAccessEventDO.class);
        verify(port(query,"accessEventMapper",DccControlledFileAccessEventMapper.class)).insert(event.capture());
        assertEquals(A,event.getValue().getControlledFileId());assertEquals("A/1",event.getValue().getFileVersionNo());assertEquals("ATTACHMENT_DOWNLOAD:"+attachmentId,event.getValue().getPurpose());
        assertDenied(()->query.assertBusinessFileAccess(READER,A,BusinessFileAccessOperation.DOWNLOAD,AUDIT));
    }

    @Test void historicalViewQualificationDoesNotGrantDownload() {
        assertTrue(viewMatrix.canAccessCurrentViewMatrix(READER,oldVersion));
        assertDenied(()->download(A,700L));verifyNoInteractions(storage);
    }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void noViewAndDownloadOnlyCannotReadHistory(boolean onlyDownload) {
        viewAllowed=false;downloadAllowed=onlyDownload;
        assertFalse(viewMatrix.canAccessCurrentViewMatrix(READER,oldVersion));
        assertEquals(onlyDownload,permissions.hasCategoryPermission(CATEGORY,READER,DccFileCategoryPermissionActionEnum.DOWNLOAD));
        assertDenied(()->query.getControlledFile(READER,A));assertDenied(()->query.getControlledFile(READER,A,historyContext()));
        assertDenied(()->query.getAttachmentPreviewMetadata(READER,A,700L,AUDIT));
        assertDenied(()->preview(A,700L,issuedProof(oldVersion)));assertDenied(()->download(A,700L));verifyNoInteractions(storage);
    }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void foreignProjectOrAssignmentScopeStillRejectsHistoricalReads(boolean restrictAssignment) {
        downloadAllowed=true;if(restrictAssignment)assignmentRestricted=true;else projectAllowed=false;
        assertDenied(()->query.getControlledFile(READER,A));assertDenied(()->query.getControlledFile(READER,A,historyContext()));
        assertDenied(()->query.getAttachmentPreviewMetadata(READER,A,700L,AUDIT));
        assertDenied(()->preview(A,700L,issuedProof(oldVersion)));assertDenied(()->download(A,700L));verifyNoInteractions(storage);
    }

    @Test void dailyOldBodyAndMainDownloadRemainDenied() {
        downloadAllowed=true;var daily=DccControlledFileBodyReadContext.daily(A);
        assertDenied(()->query.getControlledFile(READER,A,daily));
        assertDenied(()->query.getPreviewMetadata(READER,A,daily,AUDIT));
        assertDenied(()->query.readPreviewFile(READER,A,daily,"viewer","event","trace","token-id","nonce",AUDIT));
        assertDenied(()->query.readDownloadFile(READER,A,true,"c23-main-download",AUDIT));verifyNoInteractions(storage);
    }

    @Test void currentAttachmentCannotBeSubstitutedIntoHistoricalVersion() {
        downloadAllowed=true;
        var error=assertThrows(ServiceException.class,()->download(A,710L));assertEquals(CONTROLLED_FILE_NOT_EXISTS.getCode(),error.getCode());
        verifyNoInteractions(storage);
    }

    @Test void revokedViewIsRecheckedAtBinaryRead() {
        var metadata=query.getAttachmentPreviewMetadata(READER,A,700L,AUDIT);viewAllowed=false;
        assertDenied(()->preview(A,700L,metadata));verifyNoInteractions(storage);
    }

    @ParameterizedTest @ValueSource(longs={20,21})
    void secondBusinessGateDenialDoesNotReturnAttachmentBytes(long selected) {
        downloadAllowed=true;businessDenied=true;assertDenied(()->download(selected,selected==A?700L:710L));
        assertEquals(1,businessChecks.size(),"Outer permission rejection must not masquerade as a tested second storage gate");verifyNoInteractions(storage);
    }

    private void wireBusinessAndPreviewPorts() {
        var provider=new DccBusinessFileAccessProvider(query,files,null,null);
        when(port(query,"businessFileAccessService",BusinessFileAccessService.class).assertAllowed(any())).thenAnswer(i->{
            BusinessFileAccessRequest request=i.getArgument(0);businessChecks.add(request);if(businessDenied)return Optional.empty();
            Long owner=storageOwners.get(request.fileId());assertNotNull(owner,"Declared physical file needs its exact version owner");
            var file=owner==A?oldVersion:currentVersion;
            var reference=new BusinessFileAccessReference("dcc","DCC_CONTROLLED_FILE",file.getId(),file.getVersionNo(),1L,null);
            provider.assertAllowed(request,reference);return Optional.of(reference);
        });
        when(port(query,"previewAccessService",DccControlledPreviewAccessService.class).prepareAccess(any())).thenAnswer(i->issue(i.getArgument(0)));
        var events=port(query,"accessEventMapper",DccControlledFileAccessEventMapper.class);
        when(events.selectOne(any())).thenAnswer(i->previewEvent);
        when(port(query,"watermarkTraceMapper",DccControlledFileWatermarkTraceMapper.class).selectOne(any())).thenAnswer(i->previewTrace);
        doAnswer(i->{i.getArgument(0,DccControlledFileAccessEventDO.class).setId(800L);return 1;}).when(events).insert(any(DccControlledFileAccessEventDO.class));
        var downloads=port(query,"downloadRecordMapper",DccControlledFileDownloadRecordMapper.class);
        doAnswer(i->{i.getArgument(0,DccControlledFileDownloadRecordDO.class).setId(900L);return 1;}).when(downloads).insert(any(DccControlledFileDownloadRecordDO.class));
        when(downloads.update(isNull(),any())).thenReturn(1);when(downloads.updateById(any(DccControlledFileDownloadRecordDO.class))).thenReturn(1);
        when(port(query,"accessLogMapper",DccControlledFileAccessLogMapper.class).insert(any(DccControlledFileAccessLogDO.class))).thenReturn(1);
        when(port(query,"transactionManager",PlatformTransactionManager.class).getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }

    private DccPreviewAccessResult issue(DccPreviewAccessRequest request) {
        previewEvent=DccControlledFileAccessEventDO.builder().id(800L).tenantId(1L).controlledFileId(request.fileId()).fileVersionNo(request.versionId())
                .userId(READER).accessType("PREVIEW").purpose(request.purpose()).result("SUCCESS").accessEventCode("event").build();
        previewTrace=DccControlledFileWatermarkTraceDO.builder().id(801L).traceCode("trace").accessEventId(800L).accessEventCode("event")
                .controlledFileId(request.fileId()).fileVersionNo(request.versionId()).userId(READER).build();
        var now=LocalDateTime.now();return new DccPreviewAccessResult(800L,"event",801L,"trace","viewer","token-id","nonce",now,now.plusMinutes(15),"{}");
    }
    private DccControlledFilePreviewMetadataRespVO issuedProof(DccControlledFileDO file) {
        // Audit/token-port fixture for negative reads; it grants no Query or guard authority.
        var issued=issue(new DccPreviewAccessRequest(1L,READER,file.getId(),file.getVersionNo(),file.getFileNumber(),"PREVIEW","CONTROLLED_PREVIEW",900L,"99","Reader",null,null,"tenant","TRACE_CODE_ONLY","127.0.0.1","unit","negative"));
        var result=new DccControlledFilePreviewMetadataRespVO();result.setViewerToken(issued.viewerToken());result.setAccessEventCode(issued.accessEventCode());result.setWatermarkTraceCode(issued.watermarkTraceCode());result.setViewerTokenId(issued.viewerTokenId());result.setViewerTokenNonce(issued.viewerTokenNonce());return result;
    }
    private DccControlledFileBinary preview(long file,long attachment,DccControlledFilePreviewMetadataRespVO metadata) {
        return query.readAttachmentPreviewFile(READER,file,attachment,metadata.getViewerToken(),metadata.getAccessEventCode(),metadata.getWatermarkTraceCode(),metadata.getViewerTokenId(),metadata.getViewerTokenNonce(),AUDIT);
    }
    private DccDownloadFileBinary download(long file,long attachment) { return query.readAttachmentDownloadFile(READER,file,attachment,true,"c23-download-"+attachment,AUDIT); }
    private DccControlledFileBodyReadContext historyContext() { return new DccControlledFileBodyReadContext(DccControlledFileBodyReadContext.Intent.VERSION_HISTORY,Long.toString(A),"A/1",null,null,null); }
    private void assertDenied(org.junit.jupiter.api.function.Executable action) { assertEquals(CONTROLLED_FILE_ACCESS_DENIED.getCode(),assertThrows(ServiceException.class,action).getCode()); }
    private DccControlledFileDO version(long id,String version,String status,LocalDateTime time) {
        return DccControlledFileDO.builder().id(id).tenantId(1L).masterId(MASTER).dccProjectCodeId(PROJECT).categoryId(CATEGORY).directoryId(DIRECTORY).fileTypeTaxonomyId(66L)
                .fileNumber("C23-HISTORY").fileName("c23-"+version.charAt(0)+"-source.pdf").sourceOriginalFileName("c23-source.pdf").versionNo(version).revisionCode(version.substring(0,1)).iterationNo(1)
                .status(status).requesterId(AUTHOR).publishedFileId(id==A?44L:45L).stampedFileId(id==A?44L:45L).processType("CONTROLLED_FILE").changeType(id==A?"NEW":"REVISION")
                .processDefinitionKey(id==A?DccControlledFileProcessDefinitionKeys.UPLOAD:DccControlledFileProcessDefinitionKeys.REVISION).needTraining(true).controlledTime(time).activatedTime(time).publishedTime(time).build();
    }
    private DccCategoryViewMatrixRuleDO viewRule() { return DccCategoryViewMatrixRuleDO.builder().id(1L).categoryId(CATEGORY).subjectType("USER").subjectId(READER).subjectLabel("C23Reader").marker("●").scopeType("ALL_MEMBERS").active(true).build(); }
    private DccFileCategoryPermissionRuleDO downloadRule() { return DccFileCategoryPermissionRuleDO.builder().id(2L).categoryId(CATEGORY).actionType("DOWNLOAD").subjectType("USER").subjectId(READER).active(true).build(); }
    private void addAttachment(long id,long owner,long binary,String name,String purpose,LocalDateTime time,String body) {
        addPhysical(binary,owner,name,body);var file=physical.get(binary);
        var row=DccControlledFileAttachmentDO.builder().id(id).controlledFileId(owner).storageFileId(binary).originalFileName(name).contentType(file.getType()).fileSize(file.getSize()).attachmentPurpose(purpose)
                .uploadedBy(AUTHOR).uploadedUsernameSnapshot("C23Author").uploadedNicknameSnapshot("C23Author").uploadedAt(time).applicationProcessInstanceId(owner==A?"round-A":"round-B").sortNo(purpose.equals("ATTACHMENT")?0:1).build();row.setTenantId(1L);rows.put(id,row);
    }
    private void addPhysical(long id,long owner,String name,String body) { var content=body.getBytes(StandardCharsets.UTF_8);String location="c23-history/"+name;physical.put(id,FileDO.builder().id(id).configId(1L).name(name).path(location).type(name.endsWith(".pdf")?"application/pdf":"text/plain").size((long)content.length).build());storageOwners.put(id,owner);bytes.put(location,content); }
    private byte[] content(long binary) { return bytes.get(physical.get(binary).getPath()); }
    private static void set(Object target,String name,Object value) { ReflectionTestUtils.setField(target,name,value); }
    private static <T>T port(Object target,String name,Class<T> type) { return type.cast(ReflectionTestUtils.getField(target,name)); }
    private static <T>T withMockPorts(T target) {
        for(var field:target.getClass().getDeclaredFields())if(!Modifier.isStatic(field.getModifiers())&&field.getAnnotation(Resource.class)!=null)set(target,field.getName(),mock(field.getType()));
        return target;
    }
}
