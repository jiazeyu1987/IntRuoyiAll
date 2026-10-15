package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileTemporaryFileMapper;
import cn.iocoder.yudao.module.dcc.service.upload.*;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.*;
import cn.iocoder.yudao.module.dcc.service.audit.DccControlledFileAccessAuditService;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.interceptor.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Actual Upload/NameClaim/Ticket/File SQL plus isolated bytes; no live storage or business DB. */
class DccUploadNamePreflightDatabaseTest extends BaseDbUnitTest {
    @jakarta.annotation.Resource private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccSourceNameReservationMapper g25Reservations;

    @Resource javax.sql.DataSource dataSource;
    @Resource PlatformTransactionManager transactions;
    @Resource DccControlledFileNameClaimMapper claims;
    @Resource DccControlledFileMasterMapper masters;
    @Resource DccControlledFileMapper files;
    @Resource DccControlledFileTemporaryFileMapper temporaries;
    @Resource FileMapper infraFiles;
    JdbcTemplate jdbc;DccControlledFileUploadServiceImpl upload;DccUploadTicketServiceImpl tickets;
    FileService storage;DccControlledFileWorkflowService workflow;DccControlledFileAccessAuditService audit;
    DccControlledPreviewWatermarkService watermark;final Map<String,byte[]> bytes=new HashMap<>();AtomicLong ids=new AtomicLong(1000);
    static void wire(Object bean,Object... pairs){for(int i=0;i<pairs.length;i+=2)ReflectionTestUtils.setField(bean,(String)pairs[i],pairs[i+1]);}
    static void defaults(Object bean){for(var f:bean.getClass().getDeclaredFields())if(!Modifier.isStatic(f.getModifiers()) && f.getAnnotation(Resource.class)!=null)ReflectionTestUtils.setField(bean,f.getName(),mock(f.getType()));}
    @BeforeEach void fixture() throws Exception {
        TenantContextHolder.setTenantId(1L);jdbc=new JdbcTemplate(dataSource);try(var c=dataSource.getConnection()){assertTrue(c.getMetaData().getURL().startsWith("jdbc:h2:mem:"));}
        jdbc.execute("CREATE TABLE IF NOT EXISTS infra_file(id BIGINT PRIMARY KEY,config_id BIGINT,name VARCHAR(256),path VARCHAR(1024),url VARCHAR(1024),type VARCHAR(128),size BIGINT,creator VARCHAR(64),updater VARCHAR(64),create_time TIMESTAMP,update_time TIMESTAMP,deleted BIT DEFAULT 0)");jdbc.update("DELETE FROM infra_file");bytes.clear();
        storage=mock(FileService.class);
        when(storage.createFileAndReturnId(any(),anyString(),anyString(),anyString())).thenAnswer(c->{long id=ids.incrementAndGet();String path="preflight/"+id;byte[] body=c.getArgument(0);var row=new FileDO();row.setId(id);row.setConfigId(1L);row.setName(c.getArgument(1));row.setPath(path);row.setUrl("test");row.setType(c.getArgument(3));row.setSize((long)body.length);infraFiles.insert(row);bytes.put(path,body);return id;});
        doAnswer(c->{var row=infraFiles.selectById(c.getArgument(0));if(row==null)throw new IllegalStateException("metadata missing");bytes.remove(row.getPath());infraFiles.deleteById(row.getId());return null;}).when(storage).deleteFile(anyLong());
        tickets=new DccUploadTicketServiceImpl();wire(tickets,"temporaryFileMapper",temporaries,"fileMapper",infraFiles,"fileService",storage);
        var names=new DccControlledFileNameClaimService();org.springframework.test.util.ReflectionTestUtils.setField(names,"reservationMapper",g25Reservations);wire(names,"claimMapper",claims,"masterMapper",masters,"fileMapper",files);
        if(Arrays.stream(DccControlledFileNameClaimService.class.getDeclaredFields()).anyMatch(f->f.getName().equals("temporaryFileMapper")))wire(names,"temporaryFileMapper",temporaries);
        upload=new DccControlledFileUploadServiceImpl();defaults(upload);workflow=mock(DccControlledFileWorkflowService.class);audit=mock(DccControlledFileAccessAuditService.class);watermark=mock(DccControlledPreviewWatermarkService.class);
        var categories=mock(cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper.class);when(categories.selectById(2L)).thenReturn(cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO.builder().id(2L).active(true).lifecycleStage("PLAN").build());
        var permission=mock(DccControlledFileCategoryPermissionSupport.class);when(permission.hasCategoryPermission(any(),any(),any())).thenReturn(true);
        wire(upload,"fileService",storage,"fileMapper",infraFiles,"uploadTicketService",tickets,"workflowService",workflow,"accessAuditService",audit,"watermarkService",watermark,"categoryMapper",categories,"permissionSupport",permission);
        if(Arrays.stream(DccControlledFileUploadServiceImpl.class.getDeclaredFields()).anyMatch(f->f.getName().equals("nameClaimService")))wire(upload,"nameClaimService",names);
        if(Arrays.stream(DccControlledFileUploadServiceImpl.class.getDeclaredFields()).anyMatch(f->f.getName().equals("sourceOwnershipService"))){
            var ownership=new DccControlledFileSourceOwnershipService();wire(ownership,"fileMapper",infraFiles);var configs=mock(cn.iocoder.yudao.module.infra.service.file.FileConfigService.class);var client=mock(cn.iocoder.yudao.module.infra.framework.file.core.client.FileClient.class);when(configs.getFileClient(1L)).thenReturn(client);doAnswer(c->{bytes.remove(c.getArgument(0));return null;}).when(client).delete(anyString());wire(ownership,"fileConfigService",configs);wire(upload,"sourceOwnershipService",ownership);
        }
        var factory=new ProxyFactory(upload);factory.setProxyTargetClass(true);factory.addAdvice(new TransactionInterceptor(transactions,new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));upload=(DccControlledFileUploadServiceImpl)factory.getProxy();
    }
    @AfterEach void clear(){jdbc.update("DELETE FROM infra_file");TenantContextHolder.clear();}
    DccControlledFileUploadPreviewReqVO request(String name,String context,String purpose){var req=new DccControlledFileUploadPreviewReqVO();req.setCategoryId(2L);req.setUploadContext(context);req.setPurpose(purpose);req.setDccProjectCodeId(5L);req.setFileTypeTaxonomyId(6L);req.setFileName("TEMPLATE-SOP");req.setSessionId("test-session");req.setControlledFileId(20L);req.setFiles(new org.springframework.web.multipart.MultipartFile[]{new MockMultipartFile("files",name,"application/pdf","%PDF-1.4\nreal".getBytes())});return req;}
    void occupy(String name){jdbc.update("INSERT INTO dcc_controlled_file_name_claim(tenant_id,normalized_name,source_original_file_name,dcc_project_code_id,file_type_taxonomy_leaf_id,normalized_file_number,master_id) VALUES(1,?,?,9,6,'N-OTHER',10)",name,name);}
    @Test void exactActualMultipartNameCollisionRejectsBeforeAnyStorageOrTicketWrite() {
        occupy("SOP.pdf");assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->upload.uploadPreviewFile(99L,request("SOP.pdf","NEW_UPLOAD","SOURCE"),new DccRequestAuditContext("ip","JUnit","name-red")));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_temporary_file",Integer.class));assertTrue(bytes.isEmpty());verifyNoInteractions(storage);
    }
    @Test void responseFailureRollsBackCreatedTicketMetadataAndExactNewBytes() {
        doThrow(new IllegalStateException("watermark response failure")).when(watermark).build(any(),any(),any());
        assertThrows(IllegalStateException.class,()->upload.uploadPreviewFile(99L,request("SAFE.pdf","NEW_UPLOAD","SOURCE"),new DccRequestAuditContext("ip","JUnit","late-failure")));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_temporary_file",Integer.class));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM infra_file",Integer.class));assertTrue(bytes.isEmpty());
    }
    @ParameterizedTest @ValueSource(strings={"sop.pdf","SOP.PDF","SOP.docx"})
    void caseAndCompleteExtensionVariantsDoNotCollide(String name) throws Exception {
        occupy("SOP.pdf");var result=upload.uploadPreviewFile(99L,request(name,"NEW_UPLOAD","SOURCE"),new DccRequestAuditContext("ip","JUnit","variant"));
        assertNotNull(result.getUploadTicket());assertEquals(name,result.getFileName());assertEquals(1,bytes.size());
    }
    @Test void retainedObsoleteAndUnresolvedHistoricClaimsRejectWithoutLeakingTarget() {
        occupy("SOP.pdf");jdbc.update("UPDATE dcc_controlled_file_name_claim SET obsolete_time=CURRENT_TIMESTAMP,retain_until=DATEADD('YEAR',20,CURRENT_TIMESTAMP)");
        var rejected=assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->upload.uploadPreviewFile(99L,request("SOP.pdf","NEW_UPLOAD","SOURCE"),new DccRequestAuditContext("ip","JUnit","retention")));
        assertFalse(rejected.getMessage().contains("N-OTHER"));
        jdbc.update("UPDATE dcc_controlled_file_name_claim SET source_original_file_name=NULL");
        assertThrows(IllegalStateException.class,()->upload.uploadPreviewFile(99L,request("OTHER.pdf","NEW_UPLOAD","SOURCE"),new DccRequestAuditContext("ip","JUnit","unresolved")));
        assertTrue(bytes.isEmpty());verifyNoInteractions(storage);
    }
    @ParameterizedTest @ValueSource(strings={"CHECKIN:SOURCE","NEW_UPLOAD:ATTACHMENT","NEW_UPLOAD:DRAWING_PDF","EXTERNAL_REVIEW:SOURCE"})
    void otherPurposesAndContextsDoNotUseNewNameGate(String value) throws Exception {
        occupy("SOP.pdf");var parts=value.split(":");var result=upload.uploadPreviewFile(99L,request("SOP.pdf",parts[0],parts[1]),new DccRequestAuditContext("ip","JUnit","other-context"));
        assertNotNull(result.getUploadTicket());assertEquals(1,bytes.size());
    }
    @Test void unauthorizedContextFailsBeforeNameQueryOrByteWrite() {
        occupy("SOP.pdf");doThrow(new cn.iocoder.yudao.framework.common.exception.ServiceException(403,"source project denied")).when(workflow).validateSourceUploadContext(any(),any());
        var failure=assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->upload.uploadPreviewFile(99L,request("SOP.pdf","NEW_UPLOAD","SOURCE"),new DccRequestAuditContext("ip","JUnit","denied")));
        assertEquals(403,failure.getCode());assertTrue(bytes.isEmpty());verifyNoInteractions(storage);
    }
    @Test void exactBoundOwnDraftReplayReturnsOriginalTicketAndNoNewBytesButChangedBodyOrSessionRejects() throws Exception {
        var req=request("SOP.pdf","NEW_UPLOAD","SOURCE");var uploaded=upload.uploadPreviewFile(99L,req,new DccRequestAuditContext("ip","JUnit","first"));
        var temporary=temporaries.selectList().get(0);String hash=cn.hutool.crypto.digest.DigestUtil.sha256Hex(req.getFiles()[0].getBytes());
        assertEquals(1L,temporary.getTenantId());
        jdbc.update("INSERT INTO dcc_controlled_file_master(id,tenant_id,category_id,file_name,file_number,dcc_project_code_id,file_type_taxonomy_leaf_id,normalized_file_number,status) VALUES(10,1,2,'SOP','N-1',5,6,'N-1','ACTIVE_CHAIN')");
        files.insert(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO.builder().id(20L).tenantId(1L).masterId(10L).categoryId(2L).directoryId(3L).sourceFileId(temporary.getStorageFileId()).originalFileId(temporary.getStorageFileId()).fileName("SOP").title("SOP").sourceOriginalFileName("SOP.pdf").fileNumber("N-1").versionNo("A/1").status("WORKING").dccProjectCodeId(5L).fileTypeTaxonomyId(6L).requesterId(99L).submitterId(99L).changeType("NEW").sourceSha256(hash).creationIdempotencyKey("draft-key").creationPayloadHash("exact-draft-payload").build());
        occupy("SOP.pdf");jdbc.update("UPDATE dcc_controlled_file_name_claim SET dcc_project_code_id=5,normalized_file_number='N-1'");
        tickets.markBound(new DccUploadTicketMarkBoundCommand(temporary.getUploadTicket(),99L,2L,temporary.getSessionId(),"SOURCE",20L));
        int size=bytes.size();var replay=upload.uploadPreviewFile(99L,req,new DccRequestAuditContext("ip","JUnit","replay"));
        assertEquals(uploaded.getUploadTicket(),replay.getUploadTicket());assertEquals("BOUND",replay.getStatus());assertEquals(size,bytes.size());assertEquals(1,temporaries.selectList().size());
        assertThrows(RuntimeException.class,()->upload.uploadPreviewFile(88L,req,new DccRequestAuditContext("ip","JUnit","foreign-actor")));
        req.setDccProjectCodeId(9L);assertThrows(RuntimeException.class,()->upload.uploadPreviewFile(99L,req,new DccRequestAuditContext("ip","JUnit","foreign-project")));req.setDccProjectCodeId(5L);
        req.setSessionId("different-session");assertThrows(RuntimeException.class,()->upload.uploadPreviewFile(99L,req,new DccRequestAuditContext("ip","JUnit","foreign-session")));
        req.setSessionId("test-session");req.setFiles(new org.springframework.web.multipart.MultipartFile[]{new MockMultipartFile("files","SOP.pdf","application/pdf","%PDF-1.4\nchanged".getBytes())});
        assertThrows(RuntimeException.class,()->upload.uploadPreviewFile(99L,req,new DccRequestAuditContext("ip","JUnit","changed-body")));
        assertEquals(size,bytes.size());assertEquals(1,temporaries.selectList().size());
    }
    @Test void outerTransactionRollbackRemovesNewBytesAndTicketButRetainsEarlierSuccessfulBody() throws Exception {
        upload.uploadPreviewFile(99L,request("ORIGINAL.pdf","NEW_UPLOAD","SOURCE"),new DccRequestAuditContext("ip","JUnit","original"));
        int before=bytes.size();var next=request("NEW.pdf","NEW_UPLOAD","SOURCE");next.setSessionId("second-session");
        assertThrows(RuntimeException.class,()->new TransactionTemplate(transactions).execute(s->{try{upload.uploadPreviewFile(99L,next,new DccRequestAuditContext("ip","JUnit","outer"));}catch(Exception e){throw new IllegalStateException(e);}throw new IllegalStateException("later caller failure");}));
        assertEquals(before,bytes.size());assertEquals(1,temporaries.selectList().size());
    }
    @Test void zeroTicketInsertCannotReturnSuccessOrLeaveAllocatedBytes() {
        var failed=mock(DccControlledFileTemporaryFileMapper.class);when(failed.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of());
        when(failed.insert(any(cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileTemporaryFileDO.class))).thenReturn(0);
        wire(tickets,"temporaryFileMapper",failed);
        assertThrows(RuntimeException.class,()->upload.uploadPreviewFile(99L,request("ZERO.pdf","NEW_UPLOAD","SOURCE"),new DccRequestAuditContext("ip","JUnit","zero-insert")));
        assertTrue(bytes.isEmpty());assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM infra_file",Integer.class));
    }
    @Test void actualMultipartHttpRejectsSourceNameEvenWithDifferentTemplateLabel() throws Exception {
        occupy("SOP.pdf");var controller=new cn.iocoder.yudao.module.dcc.controller.admin.file.DccControlledFileController();wire(controller,"uploadService",upload);
        try(var login=mockStatic(cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.class)) {
            login.when(cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            var logger=mock(cn.iocoder.yudao.framework.common.biz.infra.logger.ApiErrorLogCommonApi.class);
            var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new cn.iocoder.yudao.framework.web.core.handler.GlobalExceptionHandler("P14",logger)).build();
            var response=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/dcc/controlled-files/upload-preview")
                .file(new MockMultipartFile("files","SOP.pdf","application/pdf","%PDF-1.4\nreal".getBytes()))
                .param("categoryId","2").param("dccProjectCodeId","5").param("fileTypeTaxonomyId","6").param("fileName","TEMPLATE-SOP")
                .param("purpose","SOURCE").param("uploadContext","NEW_UPLOAD").param("sessionId","http-source").header("x-request-id","P14-source-http").header("User-Agent","JUnit")).andReturn();
            var json=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(response.getResponse().getContentAsString(),com.fasterxml.jackson.databind.JsonNode.class);
            assertEquals(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NAME_EXISTS.getCode().intValue(),json.get("code").intValue());
            assertEquals(0,temporaries.selectList().size());assertTrue(bytes.isEmpty());
        }
    }
}
