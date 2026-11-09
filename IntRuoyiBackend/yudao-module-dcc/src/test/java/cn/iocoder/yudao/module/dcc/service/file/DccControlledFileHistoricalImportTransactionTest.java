package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileTemporaryFileMapper;
import cn.iocoder.yudao.module.dcc.service.audit.DccControlledFileAccessAuditService;
import cn.iocoder.yudao.module.dcc.service.category.*;
import cn.iocoder.yudao.module.dcc.service.file.access.DccBusinessFileAccessProvider;
import cn.iocoder.yudao.module.dcc.service.upload.*;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentLifecycleCoreService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DccControlledFileHistoricalImportTransactionTest extends BaseDbUnitTest {
    @Resource DataSource dataSource;
    @Resource PlatformTransactionManager transactions;
    @Resource DccControlledFileMapper files;
    @Resource DccControlledFileMasterMapper masters;
    @Resource FileMapper fileMapper;
    @Resource DccControlledFileTemporaryFileMapper temporaryFiles;
    private DccControlledFileHistoricalImportService service;
    private DccOnlyOfficeConversionClient conversionClient;
    private ControlledContentLifecycleCoreService platform;
    private FileService storage;
    private DccControlledFileUploadServiceImpl preview;
    private DccControlledFileHistoricalImportReqVO request;
    private org.springframework.jdbc.core.JdbcTemplate jdbc;
    private cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService relationAudit;
    private final byte[] original="controlled Word source".getBytes(StandardCharsets.UTF_8);

    @BeforeEach void setup() throws Exception {
        try(var connection=dataSource.getConnection()) {
            assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:"));
        }
        TenantContextHolder.setTenantId(1L);
        jdbc=new org.springframework.jdbc.core.JdbcTemplate(dataSource);
        jdbc.update("DELETE FROM infra_file");
        jdbc.execute("CREATE TABLE IF NOT EXISTS dcc_current_file_relation_set(tenant_id BIGINT,source_master_id BIGINT,controlled_file_id BIGINT,row_version BIGINT DEFAULT 0,PRIMARY KEY(tenant_id,source_master_id))");
        jdbc.execute("CREATE TABLE IF NOT EXISTS dcc_current_file_relation(tenant_id BIGINT,source_master_id BIGINT,related_master_id BIGINT)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS import_relation_test_audit(operation VARCHAR(64))");
        for(var table:List.of("dcc_current_file_relation_set","dcc_current_file_relation","import_relation_test_audit"))jdbc.update("DELETE FROM "+table);
        storage=mock(FileService.class);var sequence=new AtomicLong(500L);
        when(storage.createFileAndReturnId(any(),anyString(),anyString(),any())).thenAnswer(call->{
            long id=sequence.getAndIncrement();
            fileMapper.insert(FileDO.builder().id(id).configId(1L).path("import/"+id).name(call.getArgument(1))
                    .type(call.getArgument(3)).size((long)((byte[])call.getArgument(0)).length).build());return id;
        });
        when(storage.getFileContent(eq(1L),eq("import/500"))).thenReturn(original);
        doAnswer(call->{fileMapper.deleteById((Long)call.getArgument(0));return null;}).when(storage).deleteFile(anyLong());
        var tickets=new DccUploadTicketServiceImpl();
        wire(tickets,"temporaryFileMapper",temporaryFiles,"fileMapper",fileMapper,"fileService",storage);
        var query=mock(DccControlledFileQueryService.class);
        when(query.identifyControlledFileScope(anyLong())).thenAnswer(c->new DccControlledFileScope(c.getArgument(0),List.of()));
        var provider=new DccBusinessFileAccessProvider(query,files,temporaryFiles,mock(DccControlledFileAccessAuditService.class));
        var access=new BusinessFileAccessService(List.of(provider));
        var properties=new DccOnlyOfficePreviewProperties();properties.setBaseUrl("http://office.test");
        properties.setPublicFileBaseUrl("http://backend.test");properties.setJwtSecret("isolated-import-test-secret");
        var tokens=new DccOnlyOfficePreviewTokenService();wire(tokens,"properties",properties);
        var converter=new DccOnlyOfficeDocumentPdfConversionService();conversionClient=mock(DccOnlyOfficeConversionClient.class);
        wire(converter,"onlyOfficePreviewProperties",properties,"onlyOfficePreviewTokenService",tokens,
                "onlyOfficeConversionClient",conversionClient,"businessFileAccessService",access);
        preview=new DccControlledFileUploadServiceImpl();
        wire(preview,"onlyOfficePreviewProperties",properties,"onlyOfficePreviewTokenService",tokens,
                "businessFileAccessService",access,"fileMapper",fileMapper,"fileService",storage);
        var permissions=mock(PermissionService.class);
        when(permissions.hasAnyRoles(eq(88L),any(String[].class))).thenReturn(true);
        when(permissions.hasAnyPermissions(eq(88L),any(String[].class))).thenReturn(true);
        var taxonomy=mock(DccFileTypeTaxonomyAdminService.class);
        when(taxonomy.validateCategoryMapping(30L,20L,true)).thenReturn(new DccFileTypeTaxonomyPath(30L,"DHF","设计","规程",null,null));
        var placement=mock(DccPublicUploadPlacementService.class);
        when(placement.create(eq(88L),any(),any(Function.class))).thenAnswer(c->
                ((Function<DccDerivedUploadStorage,Long>)c.getArgument(2)).apply(new DccDerivedUploadStorage(1L,88L,10L,11L,20L,1L,2L,3L,false)));
        var ownership=mock(DccControlledFileSourceOwnershipService.class);
        when(ownership.rollbackCleanup(any())).thenReturn(()->{});
        doAnswer(call->{
            var source=call.<DccControlledFilePreparedSource>getArgument(1);
            jdbc.update("INSERT INTO dcc_controlled_file_source_ownership(controlled_file_id,source_file_id,origin_source_file_id,source_sha256,ownership_type,claimed_by,claimed_time,tenant_id,deleted) VALUES(?,?,?,?,?,?,CURRENT_TIMESTAMP,1,0)",
                    call.<Long>getArgument(0),source.sourceFileId(),source.originSourceFileId(),source.sourceSha256(),call.<String>getArgument(3),call.<Long>getArgument(2));
            return null;
        }).when(ownership).claimSubmissionSource(anyLong(),any(),anyLong(),anyString());
        relationAudit=mock(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService.class);
        doAnswer(call->{jdbc.update("INSERT INTO import_relation_test_audit VALUES('controlled-import')");return null;}).when(relationAudit).append(any());
        var relationStore=new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStore(jdbc,relationAudit);
        platform=mock(ControlledContentLifecycleCoreService.class);
        var clock=new DccWorkflowDatePolicy();clock.setZoneId("Asia/Shanghai");
        var target=new DccControlledFileHistoricalImportService();
        wire(target,"files",files,"masters",masters,"permissions",permissions,"taxonomy",taxonomy,
                "editPermission",mock(DccControlledFileEditPermissionSupport.class),"placement",placement,
                "names",mock(DccControlledFileNameClaimService.class),"ownership",ownership,"platform",platform,"relationStore",relationStore,
                "clock",clock,"versions",DccControlledFileVersionPolicy.defaultPolicy(),
                "sizePolicy",mock(DccUploadSizePolicyService.class),
                "fileService",storage,"fileMapper",fileMapper,"converter",converter,"uploadTickets",tickets,"transactionManager",transactions);
        var proxy=new ProxyFactory(target);
        proxy.addAdvice(new TransactionInterceptor(transactions,new AnnotationTransactionAttributeSource()));
        service=(DccControlledFileHistoricalImportService)proxy.getProxy();
        request=new DccControlledFileHistoricalImportReqVO();request.setDccProjectCodeId(10L);request.setProjectFolderId(11L);
        request.setCategoryId(20L);request.setFileTypeTaxonomyId(30L);request.setFileNumber("IMPORT-TRANSACTION");request.setVersionNo("B/2");
        request.setIdempotencyKey(UUID.randomUUID().toString());
        request.setFile(new MockMultipartFile("file","signed.docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document",original));
    }

    @AfterEach void clear() {TenantContextHolder.clear();}
    private static void wire(Object target,Object... args) {
        for(int i=0;i<args.length;i+=2) ReflectionTestUtils.setField(target,(String)args[i],args[i+1]);
    }
    private void allowRealCallback() {
        when(conversionClient.convertToPdf(any())).thenAnswer(call->{
            DccOnlyOfficeConversionCommand command=call.getArgument(0);
            String token=command.documentUrl().substring(command.documentUrl().indexOf("?token=")+7);
            // A real Office fetch uses another connection and cannot see the import's uncommitted rows.
            byte[] fetched=CompletableFuture.supplyAsync(()->{
                try {return preview.readUploadPreviewOnlyOfficeFile(500L,token).bytes();}
                catch(Exception failure){throw new IllegalStateException(failure);}
            }).join();
            assertArrayEquals(original,fetched);
            return "%PDF-1.7\nconverted".getBytes(StandardCharsets.UTF_8);
        });
    }
    @Test void conversionCallbackReadsCommittedSourceAndSuccessfulImportBindsTicket() throws Exception {
        allowRealCallback();
        var imported=service.create(88L,request);
        assertEquals(500L,files.selectById(imported.id()).getOriginalFileId());
        assertEquals(501L,files.selectById(imported.id()).getPublishedFileId());
        var ticket=temporaryFiles.selectList().get(0);
        assertEquals("BOUND",ticket.getStatus());assertEquals("BOUND",ticket.getCleanupStatus());
        assertEquals(imported.id(),ticket.getBoundControlledFileId());
        verify(storage,never()).deleteFile(anyLong());
        assertEquals(imported.id(),service.create(88L,request).id());
        verify(conversionClient,times(1)).convertToPdf(any());
        assertEquals(imported.id(),jdbc.queryForObject("SELECT controlled_file_id FROM dcc_current_file_relation_set WHERE tenant_id=1",Long.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM import_relation_test_audit",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_current_file_relation",Integer.class));
    }
    @Test void conversionFailureRollsBackFormalRecordsAndCleansCommittedTemporarySource() {
        when(conversionClient.convertToPdf(any())).thenThrow(new IllegalStateException("Office conversion failed"));
        var failure=assertThrows(IllegalStateException.class,()->service.create(88L,request));
        assertEquals("Office conversion failed",failure.getMessage());
        assertEquals(0,files.selectList().size());assertEquals(0,masters.selectList().size());
        assertNull(fileMapper.selectById(500L));
        assertEquals("CLEANED",temporaryFiles.selectList().get(0).getCleanupStatus());
        verifyNoInteractions(platform);
    }
    @Test void laterImportFailureRollsBackBindingBeforeCleaningTemporarySource() {
        allowRealCallback();
        doThrow(new IllegalStateException("platform projection failed")).when(platform)
                .projectDccControlledImport(any(),anyLong(),anyLong(),anyLong(),any());
        assertThrows(IllegalStateException.class,()->service.create(88L,request));
        assertEquals(0,files.selectList().size());assertEquals(0,masters.selectList().size());
        assertNull(fileMapper.selectById(500L));assertNull(fileMapper.selectById(501L));
        var ticket=temporaryFiles.selectList().get(0);
        assertEquals("CLEANED",ticket.getCleanupStatus());assertNull(ticket.getBoundControlledFileId());
    }
    @Test void failedRelationAuditRollsBackTheOriginalImportAndItsExplicitSet() {
        allowRealCallback();doThrow(new IllegalStateException("relation audit failed")).when(relationAudit).append(any());
        assertThrows(IllegalStateException.class,()->service.create(88L,request));
        assertEquals(0,files.selectList().size());assertEquals(0,masters.selectList().size());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_current_file_relation_set",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_source_ownership",Integer.class));
        assertNull(fileMapper.selectById(500L));assertNull(fileMapper.selectById(501L));
        assertEquals("CLEANED",temporaryFiles.selectList().get(0).getCleanupStatus());
    }
}
