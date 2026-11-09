package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_FILE_NUMBER_CONFLICT;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.service.category.*;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadSizePolicyService;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketService;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketCreated;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketCreateCommand;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentLifecycleCoreService;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class DccControlledFileHistoricalImportServiceTest {
    private final LocalDateTime now=LocalDateTime.of(2026,11,2,12,0);
    private DccControlledFileHistoricalImportService service;
    private DccControlledFileMapper files;
    private DccControlledFileMasterMapper masters;
    private DccControlledFileEditPermissionSupport edit;
    private PermissionService permissions;
    private FileService storage;
    private DccDocumentPdfConversionService converter;
    private ControlledContentLifecycleCoreService platform;
    private DccControlledFileSourceOwnershipService ownership;
    private DccUploadTicketService tickets;
    private cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStore relationStore;
    private final Map<Long,DccControlledFileDO> saved=new HashMap<>();

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);TransactionSynchronizationManager.initSynchronization();
        service=new DccControlledFileHistoricalImportService();
        files=mock(DccControlledFileMapper.class);masters=mock(DccControlledFileMasterMapper.class);
        edit=mock(DccControlledFileEditPermissionSupport.class);permissions=mock(PermissionService.class);
        storage=mock(FileService.class);converter=mock(DccDocumentPdfConversionService.class);
        platform=mock(ControlledContentLifecycleCoreService.class);ownership=mock(DccControlledFileSourceOwnershipService.class);
        tickets=mock(DccUploadTicketService.class);
        relationStore=mock(cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStore.class);
        var dataSource=new org.springframework.jdbc.datasource.DriverManagerDataSource("jdbc:h2:mem:import-unit", "sa", "");
        var taxonomy=mock(DccFileTypeTaxonomyAdminService.class);var placement=mock(DccPublicUploadPlacementService.class);
        var clock=new DccWorkflowDatePolicy(){@Override public LocalDateTime now(){return now;}};
        wire("files",files,"masters",masters,"editPermission",edit,"permissions",permissions,"fileService",storage,
                "converter",converter,"platform",platform,"ownership",ownership,"relationStore",relationStore,"taxonomy",taxonomy,"placement",placement,
                "clock",clock,"versions",DccControlledFileVersionPolicy.defaultPolicy(),
                "uploadTickets",tickets,"transactionManager",new org.springframework.jdbc.datasource.DataSourceTransactionManager(dataSource),
                "sizePolicy",mock(DccUploadSizePolicyService.class),"names",mock(DccControlledFileNameClaimService.class));
        when(permissions.hasAnyRoles(88L,"doc_control")).thenReturn(true);
        when(permissions.hasAnyPermissions(eq(88L),any(String[].class))).thenReturn(true);
        var option=new cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccFileTypeTaxonomyRespVO();
        option.setId(30L);option.setName("规程");option.setActive(true);option.setApprovalSourceMode("PUBLIC_MATRIX");
        when(taxonomy.getUploadTaxonomyOptions()).thenReturn(List.of(option));
        when(taxonomy.validateCategoryMapping(30L,20L,true)).thenReturn(new DccFileTypeTaxonomyPath(30L,"DHF","设计","规程",null,null));
        when(placement.create(eq(88L),any(DccControlledFileSubmitReqVO.class),any(Function.class)))
                .thenAnswer(c->((Function<DccDerivedUploadStorage,Long>)c.getArgument(2)).apply(new DccDerivedUploadStorage(1L,88L,10L,11L,20L,1L,2L,3L,false)));
        doAnswer(c->{((DccControlledFileMasterDO)c.getArgument(0)).setId(100L);return 1;}).when(masters).insert(any(DccControlledFileMasterDO.class));
        when(masters.updateById(any(DccControlledFileMasterDO.class))).thenReturn(1);
        doAnswer(c->{var f=(DccControlledFileDO)c.getArgument(0);f.setId(101L);saved.put(101L,f);return 1;}).when(files).insert(any(DccControlledFileDO.class));
        when(files.selectById(anyLong())).thenAnswer(c->saved.get(c.getArgument(0)));
        when(files.selectByCreationIdempotencyForUpdate(eq(1L),eq(88L),anyString())).thenAnswer(c->saved.values().stream()
                .filter(f->f.getCreationIdempotencyKey().equals(c.getArgument(2))).findFirst().orElse(null));
        when(storage.createFileAndReturnId(any(byte[].class),anyString(),anyString(),any())).thenReturn(500L,501L);
        when(ownership.rollbackCleanup(any())).thenReturn(()->{});
        when(tickets.createTicket(any())).thenAnswer(c->{
            DccUploadTicketCreateCommand command=c.getArgument(0);
            return new DccUploadTicketCreated("UT-IMPORT",command.sessionId(),command.purpose(),"AVAILABLE",now.plusMinutes(30),
                    command.storageFileId(),command.originalFileName(),command.contentType(),command.fileSize());
        });
    }
    @AfterEach void cleanup() {TransactionSynchronizationManager.clearSynchronization();TenantContextHolder.clear();saved.clear();}
    private void wire(Object...args){for(int i=0;i<args.length;i+=2)ReflectionTestUtils.setField(service,(String)args[i],args[i+1]);}
    private DccControlledFileHistoricalImportReqVO request() {
        var r=new DccControlledFileHistoricalImportReqVO();r.setDccProjectCodeId(10L);r.setProjectFolderId(11L);
        r.setCategoryId(20L);r.setFileTypeTaxonomyId(30L);r.setFileNumber("E2E-IMPORT-001");r.setVersionNo("B/2");
        r.setIdempotencyKey("test-key");
        r.setFile(new MockMultipartFile("file","original-controlled.pdf","application/pdf","%PDF-1.7\ncontrolled".getBytes()));return r;
    }
    @Test void admitsActualPdfAndKeepsOriginalDatesSeparateFromBusinessUploadTime() throws Exception {
        var r=request();var result=service.create(88L,r);var f=saved.get(result.id());
        assertEquals("ACTIVE",result.status());assertEquals(now,f.getControlledTime());
        assertEquals(now,f.getEffectiveTime());assertEquals(now,f.getImportedTime());assertEquals(now,f.getActivatedTime());
        assertEquals(88L,f.getImportedBy());assertEquals("CONTROLLED_IMPORT",f.getImportSource());
        assertEquals(500L,f.getPublishedFileId());assertEquals(500L,f.getReadOnlyFileId());assertNull(f.getStampedFileId());
        assertNull(f.getProcessInstanceId());assertNull(f.getEffectiveSignatureId());assertEquals("[]",f.getDistributionDepartmentIdsJson());
        verify(edit).assertEditableProjectCategory(88L,10L,20L);verifyNoInteractions(converter);
        verify(platform).projectDccControlledImport(any(),eq(100L),eq(101L),eq(88L),eq(now));
        var order=inOrder(ownership,platform,relationStore);
        order.verify(ownership).claimSubmissionSource(eq(101L),any(),eq(88L),eq("CONTROLLED_IMPORT"));
        order.verify(platform).projectDccControlledImport(any(),eq(100L),eq(101L),eq(88L),eq(now));
        order.verify(relationStore).initializeControlledImportRelations(same(f),eq(88L));
    }
    @Test void fractionalClockPersistsOneSecondPrecisionEventBeforeStrictPlatformProjection() throws Exception {
        wire("clock",new DccWorkflowDatePolicy(){@Override public LocalDateTime now(){return now.withNano(800_000_000);}});
        var jdbc=new org.springframework.jdbc.core.JdbcTemplate(new org.springframework.jdbc.datasource.DriverManagerDataSource(
                "jdbc:h2:mem:import_precision_"+UUID.randomUUID()+";MODE=MYSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE dcc_controlled_file_master(id BIGINT,tenant_id BIGINT,deleted INT,dcc_project_code_id BIGINT,category_id BIGINT,file_type_taxonomy_leaf_id BIGINT,current_active_controlled_file_id BIGINT)");
        jdbc.execute("""
                CREATE TABLE dcc_controlled_file(id BIGINT,master_id BIGINT,tenant_id BIGINT,deleted INT,version_no VARCHAR(32),status VARCHAR(40),
                imported_by BIGINT,imported_time TIMESTAMP(0),controlled_time TIMESTAMP(6),effective_time TIMESTAMP(0),activated_time TIMESTAMP(6),
                import_source VARCHAR(32),effective_mode VARCHAR(32),dcc_project_code_id BIGINT,category_id BIGINT,file_type_taxonomy_id BIGINT,
                published_file_id BIGINT,read_only_file_id BIGINT,process_instance_id VARCHAR(128),effective_signature_id BIGINT)
                """);
        doAnswer(c->{var m=(DccControlledFileMasterDO)c.getArgument(0);m.setId(100L);
            return jdbc.update("INSERT INTO dcc_controlled_file_master VALUES(100,1,0,?,?,?,NULL)",m.getDccProjectCodeId(),m.getCategoryId(),m.getFileTypeTaxonomyLeafId());
        }).when(masters).insert(any(DccControlledFileMasterDO.class));
        when(masters.updateById(any(DccControlledFileMasterDO.class))).thenAnswer(c->{
            var m=(DccControlledFileMasterDO)c.getArgument(0);
            return jdbc.update("UPDATE dcc_controlled_file_master SET current_active_controlled_file_id=? WHERE id=?",m.getCurrentActiveControlledFileId(),m.getId());
        });
        doAnswer(c->{var f=(DccControlledFileDO)c.getArgument(0);f.setId(101L);saved.put(101L,f);
            return jdbc.update("INSERT INTO dcc_controlled_file VALUES(101,100,1,0,?,?,?,?,?,?,?,'CONTROLLED_IMPORT','CONTROLLED_IMPORT',?,?,?,500,500,NULL,NULL)",
                    f.getVersionNo(),f.getStatus(),f.getImportedBy(),f.getImportedTime(),f.getControlledTime(),f.getEffectiveTime(),f.getActivatedTime(),
                    f.getDccProjectCodeId(),f.getCategoryId(),f.getFileTypeTaxonomyId());
        }).when(files).insert(any(DccControlledFileDO.class));
        var refs=mock(cn.iocoder.yudao.module.system.dal.mysql.controlledcontent.ControlledContentVersionRefMapper.class);
        var audits=mock(cn.iocoder.yudao.module.system.dal.mysql.controlledcontent.ControlledContentTransitionAuditMapper.class);
        when(refs.selectChainForUpdate(1L,"DCC_CONTROLLED_FILE","100")).thenReturn(List.of());
        doAnswer(c->{((cn.iocoder.yudao.module.system.dal.dataobject.controlledcontent.ControlledContentVersionRefDO)c.getArgument(0)).setId(900L);return 1;})
                .when(refs).insert(any(cn.iocoder.yudao.module.system.dal.dataobject.controlledcontent.ControlledContentVersionRefDO.class));
        var strictPlatform=new ControlledContentLifecycleCoreService(refs,audits,
                new cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentStateMachine());
        ReflectionTestUtils.setField(strictPlatform,"jdbcTemplate",jdbc);wire("platform",strictPlatform);
        assertEquals("ACTIVE",service.create(88L,request()).status());
        var persisted=jdbc.queryForMap("SELECT imported_time,controlled_time,effective_time,activated_time FROM dcc_controlled_file WHERE id=101");
        for(var timestamp:persisted.values()) assertEquals(now,((java.sql.Timestamp)timestamp).toLocalDateTime());
        verify(refs).insert(argThat((cn.iocoder.yudao.module.system.dal.dataobject.controlledcontent.ControlledContentVersionRefDO ref)->
                "ACTIVE".equals(ref.getCanonicalStatus()) && ref.getNativeVersionId().equals(101L) && ref.getLastTransitionTime().equals(now)));
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"QMS","GENERAL"})
    void libraryEditorGetsMappedTypesAndImportsWithoutLegacyRules(String scope) throws Exception {
        var projects=mock(cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper.class);
        var authorization=new DccLibraryUploadPermissionFixture(projects,10L,88L,scope,"EDIT");
        var categoryMapper=mock(cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper.class);
        when(categoryMapper.selectList()).thenReturn(List.of(cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO.builder()
                .id(20L).fileTypeTaxonomyId(30L).active(true).build()));
        when(projects.selectList()).thenReturn(List.of(authorization.project));
        var folders=mock(cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectFolderMapper.class);
        var departments=mock(cn.iocoder.yudao.module.system.service.dept.DeptService.class);
        var realEdit=new DccControlledFileEditPermissionSupport();
        ReflectionTestUtils.setField(realEdit,"permissions",authorization.categories);
        wire("projects",projects,"categories",categoryMapper,"categoryAccess",authorization.categories,
                "projectAccess",authorization.access,"editPermission",realEdit,"folders",folders,"deptService",departments);
        assertFalse(authorization.categories.hasCategoryPermission(20L,88L,
                cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum.EDIT));
        var options=service.options(88L,10L);
        assertEquals(List.of(20L),options.fileTypes().stream().map(DccControlledFileHistoricalImportService.FileType::categoryId).toList());
        assertTrue(options.projects().isEmpty(),"library rows cannot appear under PROJECT choices");
        assertTrue(service.options(88L,null).projects().isEmpty());
        assertEquals(101L,service.create(88L,request()).id());
        assertEquals(10L,saved.get(101L).getDccProjectCodeId());
        authorization.rule.setAccessLevel("VIEW");
        assertEquals(CONTROLLED_FILE_ACCESS_DENIED.getCode(),assertThrows(ServiceException.class,()->service.options(88L,10L)).getCode());
        assertEquals(CONTROLLED_FILE_ACCESS_DENIED.getCode(),assertThrows(ServiceException.class,()->service.create(88L,request())).getCode());
        verify(files,times(1)).insert(any(DccControlledFileDO.class));
    }

    @Test void duplicateMappingDisablesOnlyItsCandidateAndReadFailuresRemainFailures() {
        var projects=mock(cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper.class);
        var auth=new DccLibraryUploadPermissionFixture(projects,10L,88L,"QMS","EDIT");
        var categoryMapper=mock(cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper.class);
        var categories=List.of(
                cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO.builder().id(20L).fileTypeTaxonomyId(30L).active(true).approvalSourceMode("PUBLIC_MATRIX").build(),
                cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO.builder().id(21L).fileTypeTaxonomyId(31L).active(true).approvalSourceMode("PUBLIC_MATRIX").build(),
                cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO.builder().id(22L).fileTypeTaxonomyId(31L).active(true).approvalSourceMode("PUBLIC_MATRIX").build());
        when(categoryMapper.selectList()).thenReturn(categories);
        when(projects.selectList()).thenReturn(List.of(auth.project));
        var nodes=List.of(
                cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileTypeTaxonomyDO.builder().id(1L).parentId(0L).levelNo(1).name("QMS").active(true).sort(1).build(),
                cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileTypeTaxonomyDO.builder().id(2L).parentId(1L).levelNo(2).name("质量").active(true).sort(1).build(),
                cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileTypeTaxonomyDO.builder().id(30L).parentId(2L).levelNo(3).name("有效规程").active(true).sort(1).build(),
                cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileTypeTaxonomyDO.builder().id(31L).parentId(2L).levelNo(3).name("重复映射").active(true).sort(2).build());
        var taxonomyMapper=mock(cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileTypeTaxonomyMapper.class);
        when(taxonomyMapper.selectList()).thenReturn(nodes);
        when(taxonomyMapper.selectById(30L)).thenReturn(nodes.get(2));
        when(taxonomyMapper.selectMappingTaxonomyForUpdate(1L,30L)).thenReturn(nodes.get(2));
        when(categoryMapper.selectMappingCategoriesForUpdate(1L,30L)).thenReturn(List.of(categories.get(0)));
        var realTaxonomy=new cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyAdminServiceImpl();
        DccLibraryUploadPermissionFixture.wire(realTaxonomy,"taxonomyMapper",taxonomyMapper,"categoryMapper",categoryMapper);
        wire("projects",projects,"categories",categoryMapper,"categoryAccess",auth.categories,"projectAccess",auth.access,
                "taxonomy",realTaxonomy,"folders",mock(cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectFolderMapper.class),
                "deptService",mock(cn.iocoder.yudao.module.system.service.dept.DeptService.class));
        var options=service.options(88L,10L);assertEquals(3,options.fileTypes().size());
        assertNull(options.fileTypes().get(0).uploadBlocker());assertEquals("QMS / 质量 / 有效规程",options.fileTypes().get(0).label());
        for(var invalid:options.fileTypes().subList(1,3)) {
            assertTrue(invalid.uploadBlocker().contains("多个启用文件类别"));
            assertTrue(invalid.label().contains(invalid.uploadBlocker()));
        }
        verify(taxonomyMapper,never()).selectMappingTaxonomyForUpdate(1L,31L);
        var failure=new IllegalStateException("taxonomy database unavailable");
        when(taxonomyMapper.selectList()).thenThrow(failure);
        assertSame(failure,assertThrows(IllegalStateException.class,()->service.options(88L,10L)));
    }

    @Test void docxUsesRealConversionAndNeverPretendsToStampIt() throws Exception {
        var mapper=mock(FileMapper.class);wire("fileMapper",mapper);var source=new FileDO();source.setId(500L);
        when(mapper.selectById(500L)).thenReturn(source);
        when(converter.convertToPdf(source)).thenReturn(new DccConvertedPdf("converted.pdf","%PDF-1.7\nconverted".getBytes()));
        var r=request();r.setFile(new MockMultipartFile("file","signed.docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document",new byte[]{1,2,3}));
        var result=service.create(88L,r);assertEquals(501L,saved.get(result.id()).getReadOnlyFileId());
        assertEquals(500L,saved.get(result.id()).getOriginalFileId());assertNull(saved.get(result.id()).getStampedFileId());
        verify(converter).convertToPdf(source);
        verify(tickets).markBound(argThat(c->c.uploadTicket().equals("UT-IMPORT") && c.controlledFileId().equals(101L)
                && c.userId().equals(88L) && c.categoryId().equals(20L) && c.purpose().equals("SOURCE")));
    }
    @Test void exactReplayReturnsSameRecordAndChangedPayloadIsRejected() throws Exception {
        var r=request();var first=service.create(88L,r);assertEquals(first.id(),service.create(88L,r).id());
        verify(storage,times(1)).createFileAndReturnId(any(byte[].class),anyString(),anyString(),any());
        verify(relationStore,times(1)).initializeControlledImportRelations(any(),eq(88L));
        r.setFileNumber("changed-number");
        var changed=assertThrows(ServiceException.class,()->service.create(88L,r));
        assertEquals(BAD_REQUEST.getCode(),changed.getCode());assertEquals("同一导入请求键对应的内容已改变",changed.getMessage());
        verify(storage,times(1)).createFileAndReturnId(any(byte[].class),anyString(),anyString(),any());
    }
    @Test void rejectsDuplicateFormalIdentityBeforeAllocatingBytes() {
        when(masters.selectByNewLogicalIdentity(1L,10L,30L,"E2E-IMPORT-001")).thenReturn(new DccControlledFileMasterDO());
        var duplicate=assertThrows(ServiceException.class,()->service.create(88L,request()));
        assertEquals(CONTROLLED_FILE_FILE_NUMBER_CONFLICT.getCode(),duplicate.getCode());verifyNoInteractions(storage);
    }
    @Test void rejectsMissingDocumentControlRoleOrTargetEditBeforeStorage() {
        when(permissions.hasAnyRoles(88L,"doc_control")).thenReturn(false);
        var missingRole=assertThrows(ServiceException.class,()->service.create(88L,request()));
        assertEquals(CONTROLLED_FILE_ACCESS_DENIED.getCode(),missingRole.getCode());verifyNoInteractions(edit,storage);
        when(permissions.hasAnyRoles(88L,"doc_control")).thenReturn(true);
        doThrow(exception(CONTROLLED_FILE_ACCESS_DENIED)).when(edit).assertEditableProjectCategory(88L,10L,20L);
        var targetDenied=assertThrows(ServiceException.class,()->service.create(88L,request()));
        assertEquals(CONTROLLED_FILE_ACCESS_DENIED.getCode(),targetDenied.getCode());verifyNoInteractions(storage);
    }
    @Test void workingVersionIsRejectedWithoutAllocatingBytes() {
        var r=request();r.setVersionNo("B/2-1");
        assertBadRequest(()->service.create(88L,r),"必须填写合法正式受控版本，不能导入工作小版本");
        verifyNoInteractions(storage);
    }
    private void assertBadRequest(org.junit.jupiter.api.function.Executable action,String message) {
        var rejected=assertThrows(ServiceException.class,action);
        assertEquals(BAD_REQUEST.getCode(),rejected.getCode());assertEquals(message,rejected.getMessage());
    }
    @Test void invalidPdfReturnsSpecificUserErrorBeforeStorage() {
        var r=request();
        r.setFile(new MockMultipartFile("file","invalid.pdf","application/pdf","not a PDF".getBytes()));
        assertBadRequest(()->service.create(88L,r),"上传文件不是有效PDF正文");
        verifyNoInteractions(storage);
    }
    @Test void attachmentBytesAndUploaderFactsArePersistedAndChangedReplayIsRejected() throws Exception {
        var mapper=mock(DccControlledFileAttachmentMapper.class);
        var users=mock(cn.iocoder.yudao.module.system.api.user.AdminUserApi.class);
        when(users.getUser(88L)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO()
                .setId(88L).setTenantId(1L).setStatus(0).setUsername("compiler").setNickname("编制人"));
        when(mapper.insert(any(DccControlledFileAttachmentDO.class))).thenAnswer(c->{c.<DccControlledFileAttachmentDO>getArgument(0).setId(800L);return 1;});
        wire("attachments",mapper,"users",users);
        var r=request();r.setAttachments(List.of(new MockMultipartFile("attachments","evidence.txt","text/plain","evidence".getBytes())));
        service.create(88L,r);
        verify(mapper).insert(argThat((DccControlledFileAttachmentDO a)->a.getControlledFileId().equals(101L)
                && a.getStorageFileId().equals(501L) && a.getAttachmentPurpose().equals("ATTACHMENT")
                && a.getUploadedBy().equals(88L) && a.getUploadedUsernameSnapshot().equals("compiler")
                && a.getFileSha256().equals(cn.hutool.crypto.digest.DigestUtil.sha256Hex("evidence"))));
        r.setAttachments(List.of(new MockMultipartFile("attachments","evidence.txt","text/plain","changed".getBytes())));
        assertBadRequest(()->service.create(88L,r),"同一导入请求键对应的内容已改变");
    }
    @Test void conversionFailureRemainsAnInternalFailureInsteadOfBadRequest() {
        var mapper=mock(FileMapper.class);wire("fileMapper",mapper);var source=new FileDO();source.setId(500L);
        when(mapper.selectById(500L)).thenReturn(source);when(converter.convertToPdf(source)).thenReturn(null);
        var r=request();r.setFile(new MockMultipartFile("file","signed.docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document",new byte[]{1,2,3}));
        var failure=assertThrows(IllegalStateException.class,()->service.create(88L,r));
        assertEquals("正文转换未返回真实PDF",failure.getMessage());verifyNoInteractions(platform);
    }
    @Test void temporaryTicketFailureCleansAllocatedSourceBeforeConversion() {
        Runnable cleanup=mock(Runnable.class);
        when(ownership.rollbackCleanup(any())).thenReturn(cleanup);
        doThrow(new IllegalStateException("temporary ticket insert failed")).when(tickets).createTicket(any());
        var r=request();r.setFile(new MockMultipartFile("file","signed.docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document",new byte[]{1,2,3}));
        var failure=assertThrows(IllegalStateException.class,()->service.create(88L,r));
        assertEquals("temporary ticket insert failed",failure.getMessage());
        verify(cleanup).run();verifyNoInteractions(converter,platform);
        verify(files,never()).insert(any(DccControlledFileDO.class));
    }
    @Test void corruptStoredImportDateRemainsAnInternalIntegrityFailure() throws Exception {
        var result=service.create(88L,request());var stored=saved.get(result.id());
        stored.setControlledTime(now.plusDays(1));
        var failure=assertThrows(IllegalStateException.class,()->DccControlledFileHistoricalImportService.requireImportFacts(stored));
        assertEquals("导入原件的原受控时间与真实上传或生效时间不一致",failure.getMessage());
    }
    @Test void exactFutureActivationPreservesOriginalTimeAndUsesSystemActor() {
        var f=DccControlledFileDO.builder().id(101L).tenantId(1L).masterId(100L).importSource("CONTROLLED_IMPORT").effectiveMode("CONTROLLED_IMPORT")
                .importedBy(88L).importedTime(now.minusDays(2)).controlledTime(now.minusDays(3)).effectiveTime(now)
                .status("CONTROLLED_PENDING_EFFECTIVE").sourceFileId(500L).readOnlyFileId(500L).publishedFileId(500L).build();
        var master=DccControlledFileMasterDO.builder().id(100L).latestControlledFileId(101L).build();
        assertFalse(service.activateLocked(f,master,now.minusSeconds(1)));verifyNoInteractions(platform);
        when(files.updateById(any(DccControlledFileDO.class))).thenReturn(1);
        assertTrue(service.activateLocked(f,master,now));assertEquals(now,f.getEffectiveTime());
        verify(platform).projectDccControlledImport(any(),eq(100L),eq(101L),eq(-1L),eq(now));
    }
}
