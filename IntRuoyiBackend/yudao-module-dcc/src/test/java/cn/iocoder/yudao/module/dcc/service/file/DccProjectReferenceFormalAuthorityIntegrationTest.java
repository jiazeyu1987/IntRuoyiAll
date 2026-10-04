package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.common.biz.infra.logger.ApiErrorLogCommonApi;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.web.core.handler.GlobalExceptionHandler;
import cn.iocoder.yudao.module.dcc.controller.admin.file.*;
import cn.iocoder.yudao.module.dcc.service.file.relations.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccFolderTemplateService;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessServiceImpl;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.PostApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import({DccProjectReferenceController.class,DccProjectReferenceService.class,DccProjectReferenceAuthorityImpl.class,
        DccProjectLeaderService.class,DccFolderTemplateService.class,DccProjectConfigurationAuditService.class,
        DccProjectAccessServiceImpl.class,
        DccLatestControlledFileResolverImpl.class,DccRelationStore.class,GxpAuditServiceImpl.class,
        DccProjectReferenceFormalAuthorityIntegrationTest.TestBeans.class})
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(scripts="/sql/dcc_b_gxp_audit_tables.sql",executionPhase=Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts="/sql/dcc_b_gxp_audit_clean.sql",executionPhase=Sql.ExecutionPhase.AFTER_TEST_METHOD)
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:dcc_d_formal_leader_http;MODE=MYSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1;NON_KEYWORDS=value")
class DccProjectReferenceFormalAuthorityIntegrationTest extends BaseDbUnitTest {
    @TestConfiguration(proxyBeanMethods=false) @EnableMethodSecurity static class TestBeans {
        @Bean JdbcTemplate jdbc(javax.sql.DataSource source){return new JdbcTemplate(source);}
    }
    @Resource JdbcTemplate jdbc;@Resource DccProjectReferenceController controller;
    @Resource DccLatestControlledFileResolver resolver;
    @Resource DccProjectReferenceService referenceService;
    @Resource PlatformTransactionManager transactions;
    @MockitoBean AdminUserApi users;@MockitoBean PermissionApi permission;
    @MockitoBean DeptApi departments;@MockitoBean PostApi posts;@MockitoBean RoleApi roles;
    @MockitoBean DccRelationAccessPolicy access;@MockitoBean ApiErrorLogCommonApi logs;
    MockMvc mvc;
    @BeforeEach void seed() throws Exception {
        try(var connection=jdbc.getDataSource().getConnection()){assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:"));}
        for(String sql:Files.readString(Path.of("../sql/mysql/20260930_dcc_d_relations.sql")).replaceAll("(?m)^--.*$","").split(";"))if(!sql.isBlank())jdbc.execute(sql);
        jdbc.update("DELETE FROM dcc_project_file_reference");
        policy("dcc.project-reference.create");policy("dcc.project-reference.cancel");
        for(long user:new long[]{1,7,8,9,99})when(users.getUser(user)).thenReturn(new AdminUserRespDTO().setId(user).setStatus(0));
        jdbc.update("INSERT INTO dcc_project_code(id,project_name,project_code,project_leader_user_id,status,tenant_id) VALUES(1,'源项目','P1',99,'ENABLE',1),(2,'目标项目','P2',7,'ENABLE',1),(3,'第二引用项目','P3',9,'ENABLE',1)");
        jdbc.update("INSERT INTO dcc_project_folder(id,project_code_id,parent_id,name,sort_order,active,source_template_id,source_node_key,tenant_id) VALUES(21,2,0,'质量',0,TRUE,1,'quality',1),(22,2,0,'工程',1,TRUE,1,'engineering',1),(31,3,0,'质量',0,TRUE,1,'quality',1)");
        jdbc.update("INSERT INTO dcc_file_directory(id,parent_id,code,name,active,tenant_id) VALUES(99,0,'NAS-99','旧NAS目录',1,1)");
        jdbc.update("INSERT INTO dcc_controlled_file_master(id,category_id,file_name,file_number,status,tenant_id,dcc_project_code_id,current_active_controlled_file_id,latest_controlled_file_id) VALUES(10,1,'第一.pdf','F1','ACTIVE_CHAIN',1,1,100,100),(20,1,'第二.pdf','F2','ACTIVE_CHAIN',1,1,200,200)");
        for(long id:new long[]{100,200})jdbc.update("INSERT INTO dcc_controlled_file(id,master_id,category_id,directory_id,source_file_id,original_file_id,file_name,title,file_number,version_no,status,submitter_id,requester_id,dcc_project_code_id,tenant_id,controlled_time,activated_time,process_instance_id) VALUES(?,?,1,99,10,10,?,'源文件',?,'A/1','ACTIVE',99,99,1,1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'source-round')",id,id/10,"源"+id+".pdf","N"+id);
        var global=new GlobalExceptionHandler("dcc-d-test",logs);
        mvc=MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(global).build();login(7);
    }
    void policy(String id){jdbc.update("INSERT INTO gxp_audit_policy_operation(tenant_id,policy_version,operation_id,source_type,source_locator,domain,subject_type,action_type,reason_policy,signature_policy,state_policy,retention_class,test_ids,owner,applicability,active) VALUES(1,'DCC-D-ISOLATED',?,'SERVICE_METHOD','DCC.TEST','DCC','PROJECT_REFERENCE','CREATE','REQUIRED','NOT_REQUIRED','PRESENT_TO_PRESENT','GXP_CONTROLLED_DOCUMENT','INT-D-02','dcc-d','GXP',TRUE)",id);}
    void login(long id){var user=new LoginUser();user.setId(id);user.setTenantId(1L);user.setUserType(2);user.setInfo(Map.of("username","dcc-d-"+id,LoginUser.INFO_KEY_NICKNAME,"正式账号"));SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,null,List.of()));}
    String create(long project,long folder,long file){return "{\"projectId\":"+project+",\"folderId\":"+folder+",\"selectedFileId\":"+file+",\"reason\":\"引用确认\"}";}
    String cancel(long project,long folder,long master,long reference){return "{\"projectId\":"+project+",\"folderId\":"+folder+",\"masterId\":"+master+",\"referenceId\":"+reference+",\"confirmed\":true,\"reason\":\"取消确认\"}";}
    void createHttp(long project,long folder,long file) throws Exception {mvc.perform(post("/dcc/project-file-references").contentType("application/json").content(create(project,folder,file))).andExpect(jsonPath("$.code").value(0));}
    long id(long project,long folder){return jdbc.queryForObject("SELECT id FROM dcc_project_file_reference WHERE project_id=? AND folder_id=?",Long.class,project,folder);}
    int references(){return jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_file_reference",Integer.class);}
    @AfterEach void clear(){
        jdbc.update("DELETE FROM dcc_project_file_reference");
        jdbc.update("DELETE FROM dcc_current_file_relation WHERE source_master_id=10");
        jdbc.update("DELETE FROM dcc_current_file_relation_set WHERE source_master_id=10");
        jdbc.update("DELETE FROM dcc_controlled_file_related_file WHERE controlled_file_id=100");
        SecurityContextHolder.clearContext();
    }
    @Test void genuineBLeaderFolderAndDAtomicBatchSaveProduceRealAuditAndKeepPinnedVersion() throws Exception {
        mvc.perform(post("/dcc/project-file-references/batch").contentType("application/json").content("{\"projectId\":2,\"folderId\":21,\"selectedFileIds\":[100,200],\"reason\":\"原子引用\"}"))
                .andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data[0].reference.selectedControlledFileId").value("100"));
        assertEquals(2,references());assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM gxp_audit_event",Integer.class));
        jdbc.update("INSERT INTO dcc_controlled_file(id,master_id,category_id,directory_id,source_file_id,original_file_id,file_name,title,file_number,version_no,status,submitter_id,requester_id,dcc_project_code_id,tenant_id,controlled_time,process_instance_id) VALUES(101,10,1,99,10,10,'源100.pdf','源文件','N100','B/1','CONTROLLED_PENDING_EFFECTIVE',99,99,1,1,CURRENT_TIMESTAMP,'source-new')");
        jdbc.update("UPDATE dcc_controlled_file_master SET latest_controlled_file_id=101 WHERE id=10");
        assertEquals(101L,resolver.resolveLatest(10L).controlledFileId());assertTrue(resolver.resolveLatest(10L).pendingEffect());
        mvc.perform(get("/dcc/project-file-references").param("projectId","2").param("folderId","21")).andExpect(jsonPath("$.data[0].selectedVersion.controlledFileId").value("100"));
        verify(access,never()).assertContentReadable(any(),any());
    }
    @Test void otherProjectLeaderMemberAndNonLeaderAdminCannotCreateOrCancelEvenWithMenuPermission() throws Exception {
        createHttp(2,21,100);long ref=id(2,21);
        for(long actor:new long[]{99,9,8,1}){
            login(actor);when(permission.hasAnyPermissions(eq(actor),any(String[].class))).thenReturn(true);
            mvc.perform(post("/dcc/project-file-references").contentType("application/json").content(create(2,22,100))).andExpect(jsonPath("$.code").value(1_080_090_004));
            mvc.perform(post("/dcc/project-file-references/cancel").contentType("application/json").content(cancel(2,21,10,ref))).andExpect(jsonPath("$.code").value(1_080_090_004));
        }
        assertEquals(1,references());assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM gxp_audit_event",Integer.class));
    }
    @Test void oldNasDirectoryAndForeignProjectFolderNeverSubstituteTheBFolderIdentity() throws Exception {
        for(long folder:new long[]{99,31})mvc.perform(post("/dcc/project-file-references").contentType("application/json").content(create(2,folder,100))).andExpect(jsonPath("$.code").value(1_080_090_010));
        jdbc.update("UPDATE dcc_project_folder SET active=FALSE WHERE id=21");
        mvc.perform(post("/dcc/project-file-references").contentType("application/json").content(create(2,21,100))).andExpect(jsonPath("$.code").value(1_080_090_010));assertEquals(0,references());
    }
    @Test void formalLeaderChangeBlocksOldOwnerAndLastExactCancellationRestoresDistinctProjectCount() throws Exception {
        createHttp(2,21,100);createHttp(2,22,100);login(9);createHttp(3,31,100);login(7);
        mvc.perform(get("/dcc/project-file-references/usage").param("selectedFileId","100")).andExpect(jsonPath("$.data.referenceProjectCount").value(2));
        mvc.perform(post("/dcc/project-file-references/cancel").contentType("application/json").content(cancel(2,21,10,id(2,21)))).andExpect(jsonPath("$.data").value(2));
        long ref=id(2,22);jdbc.update("UPDATE dcc_project_code SET project_leader_user_id=9 WHERE id=2");
        mvc.perform(post("/dcc/project-file-references/cancel").contentType("application/json").content(cancel(2,22,10,ref))).andExpect(jsonPath("$.code").value(1_080_090_004));
        login(9);mvc.perform(post("/dcc/project-file-references/cancel").contentType("application/json").content(cancel(2,22,10,ref))).andExpect(jsonPath("$.data").value(1));
        mvc.perform(post("/dcc/project-file-references/cancel").contentType("application/json").content(cancel(3,31,10,id(3,31)))).andExpect(jsonPath("$.data").value(0));
        assertEquals(0,references());assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file",Integer.class));
    }
    @Test void formalAuditFailureRollsBackEntireHttpBatchAndWrongTenantHasZeroWrites() throws Exception {
        jdbc.update("DELETE FROM gxp_audit_policy_operation WHERE operation_id='dcc.project-reference.create'");
        mvc.perform(post("/dcc/project-file-references/batch").contentType("application/json").content("{\"projectId\":2,\"folderId\":21,\"selectedFileIds\":[100,200],\"reason\":\"原子引用\"}"))
                .andExpect(jsonPath("$.code").value(cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.GXP_AUDIT_POLICY_NOT_FOUND.getCode()));assertEquals(0,references());
        TenantContextHolder.setTenantId(2L);mvc.perform(post("/dcc/project-file-references").contentType("application/json").content(create(2,21,100))).andExpect(jsonPath("$.code").value(1_080_090_004));
        assertEquals(0,references());
    }
    @Test void obsoletePinnedSourceIsTraceableAndFinalCancellationPreservesBothFileRelationKinds() throws Exception {
        jdbc.update("INSERT INTO dcc_current_file_relation_set(tenant_id,source_master_id,controlled_file_id) VALUES(1,10,100)");
        jdbc.update("INSERT INTO dcc_current_file_relation(tenant_id,source_master_id,related_master_id) VALUES(1,10,20)");
        jdbc.update("INSERT INTO dcc_controlled_file_related_file(controlled_file_id,related_controlled_file_id,project_code_id,related_master_id,related_file_name_snapshot,related_version_no_snapshot,relation_source,tenant_id) VALUES(100,200,1,20,'审批时目标.pdf','A/1','UPLOAD',1)");
        createHttp(2,21,100);long reference=id(2,21);
        // The isolated database supplies the authoritative obsolete fact; this is not A approval/E2E.
        jdbc.update("UPDATE dcc_controlled_file SET status='OBSOLETE',obsoleted_time=CURRENT_TIMESTAMP WHERE id=100");
        mvc.perform(get("/dcc/project-file-references").param("projectId","2").param("folderId","21"))
                .andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data[0].reference.selectedControlledFileId").value("100"))
                .andExpect(jsonPath("$.data[0].selectedVersion.status").value("OBSOLETE"))
                .andExpect(jsonPath("$.data[0].selectedVersion.controlled").value(false))
                .andExpect(jsonPath("$.data[0].selectedVersion.executable").value(false))
                .andExpect(jsonPath("$.data[0].referenceProjectCount").value(1));
        mvc.perform(post("/dcc/project-file-references").contentType("application/json").content(create(2,22,100)))
                .andExpect(jsonPath("$.code").value(DccRelationErrorCodes.BUSINESS_FAILURE));
        assertEquals(1,references());
        mvc.perform(post("/dcc/project-file-references/cancel").contentType("application/json").content(cancel(2,21,10,reference)))
                .andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data").value(0));
        mvc.perform(get("/dcc/project-file-references/usage").param("selectedFileId","100"))
                .andExpect(jsonPath("$.data.referenceProjectCount").value(0)).andExpect(jsonPath("$.data.referenced").value(false));
        assertEquals(0,references());assertEquals("OBSOLETE",jdbc.queryForObject("SELECT status FROM dcc_controlled_file WHERE id=100",String.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_current_file_relation WHERE source_master_id=10",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_related_file WHERE controlled_file_id=100 AND related_controlled_file_id=200",Integer.class));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM gxp_audit_event",Integer.class));verify(access,never()).assertContentReadable(any(),any());
    }
    @ParameterizedTest
    @ValueSource(strings={"CREATE_LEADER","CANCEL_LEADER","CREATE_FOLDER","CANCEL_FOLDER"})
    void formalAuthorityFactsRemainLockedUntilReferenceMutationCommits(String operation) throws Exception {
        boolean cancel=operation.startsWith("CANCEL"),leaderChange=operation.endsWith("LEADER");
        if(cancel)createHttp(2,21,100);
        long reference=cancel?id(2,21):0;
        var authorityRead=new CountDownLatch(1);var resume=new CountDownLatch(1);var updateStarted=new CountDownLatch(1);
        when(users.getUser(7L)).thenAnswer(call->{
            authorityRead.countDown();if(!resume.await(10,TimeUnit.SECONDS))throw new AssertionError("formal account query did not resume");
            return new AdminUserRespDTO().setId(7L).setStatus(0);
        });
        var pool=Executors.newFixedThreadPool(2);
        try{
            var mutation=pool.submit(()->{
                TenantContextHolder.setTenantId(1L);login(7);
                try{return cancel?referenceService.cancel(7L,2L,21L,10L,reference,true,"并发取消确认"):
                        referenceService.create(7L,2L,21L,100L,"并发引用确认").referenceProjectCount();}
                finally{SecurityContextHolder.clearContext();TenantContextHolder.clear();}
            });
            assertTrue(authorityRead.await(10,TimeUnit.SECONDS));
            var update=pool.submit(()->new TransactionTemplate(transactions).execute(status->{
                updateStarted.countDown();
                return leaderChange?jdbc.update("UPDATE dcc_project_code SET project_leader_user_id=9 WHERE id=2 AND tenant_id=1"):
                        jdbc.update("UPDATE dcc_project_folder SET active=FALSE WHERE id=21 AND tenant_id=1");
            }));
            assertTrue(updateStarted.await(10,TimeUnit.SECONDS));
            assertThrows(TimeoutException.class,()->update.get(300,TimeUnit.MILLISECONDS),"formal project/folder update must wait for D mutation transaction");
            resume.countDown();assertEquals(cancel?0L:1L,mutation.get(10,TimeUnit.SECONDS));assertEquals(1,update.get(10,TimeUnit.SECONDS));
            assertEquals(cancel?0:1,references());
            mvc.perform(post("/dcc/project-file-references").contentType("application/json").content(create(2,21,100)))
                    .andExpect(jsonPath("$.code").value(leaderChange?1_080_090_004:1_080_090_010));
        }finally{resume.countDown();pool.shutdown();assertTrue(pool.awaitTermination(15,TimeUnit.SECONDS));}
    }
    @Test void committedObsoletionCannotBeHiddenByTheReferenceTransactionsEarlierMybatisRead() throws Exception {
        var pool=Executors.newSingleThreadExecutor();
        try{
            var failure=assertThrows(DccRelationFailure.class,()->new TransactionTemplate(transactions).execute(status->{
                assertTrue(resolver.resolveSelected(100L).controlled());
                try{
                    pool.submit(()->new TransactionTemplate(transactions).executeWithoutResult(other->{
                        jdbc.queryForList("SELECT id FROM dcc_controlled_file_master WHERE id=10 AND tenant_id=1 FOR UPDATE",Long.class);
                        jdbc.update("UPDATE dcc_controlled_file SET status='OBSOLETE',obsoleted_time=CURRENT_TIMESTAMP WHERE id=100 AND tenant_id=1");
                    })).get(10,TimeUnit.SECONDS);
                }catch(Exception error){throw new AssertionError("isolated obsoletion transaction failed",error);}
                assertTrue(resolver.resolveSelected(100L).controlled(),"ordinary repeated Mapper lookup retains the transaction's earlier cached projection");
                return referenceService.create(7L,2L,21L,100L,"锁后必须读取已作废事实");
            }));
            assertEquals("DCC_REFERENCE_SOURCE_NOT_CONTROLLED",failure.getMessage());assertEquals(0,references());
            assertEquals("OBSOLETE",jdbc.queryForObject("SELECT status FROM dcc_controlled_file WHERE id=100",String.class));
            assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM gxp_audit_event",Integer.class));
        }finally{pool.shutdown();assertTrue(pool.awaitTermination(15,TimeUnit.SECONDS));}
    }
    @Test void exactReferenceReplayReturnsCurrentObsoleteProjectionDespiteEarlierCachedActiveRead() throws Exception {
        createHttp(2,21,100);long original=id(2,21);var pool=Executors.newSingleThreadExecutor();
        try{
            var replay=new TransactionTemplate(transactions).execute(status->{
                assertTrue(resolver.resolveSelected(100L).controlled());
                try{
                    pool.submit(()->new TransactionTemplate(transactions).executeWithoutResult(other->{
                        jdbc.queryForList("SELECT id FROM dcc_controlled_file_master WHERE id=10 AND tenant_id=1 FOR UPDATE",Long.class);
                        jdbc.update("UPDATE dcc_controlled_file SET status='OBSOLETE',obsoleted_time=CURRENT_TIMESTAMP WHERE id=100 AND tenant_id=1");
                    })).get(10,TimeUnit.SECONDS);
                }catch(Exception error){throw new AssertionError("isolated obsoletion transaction failed",error);}
                assertTrue(resolver.resolveSelected(100L).controlled());
                return referenceService.create(7L,2L,21L,100L,"固定所选版本的成功重放");
            });
            assertEquals(original,replay.reference().id());assertEquals(100L,replay.selectedVersion().controlledFileId());
            assertEquals("OBSOLETE",replay.selectedVersion().status());assertFalse(replay.selectedVersion().controlled());assertFalse(replay.selectedVersion().executable());
            assertEquals(1,references());assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM gxp_audit_event",Integer.class));
        }finally{pool.shutdown();assertTrue(pool.awaitTermination(15,TimeUnit.SECONDS));}
    }
    @Test void authoritativeSelectedLockReadRequiresAnExistingCallerTransaction(){
        assertThrows(org.springframework.transaction.IllegalTransactionStateException.class,()->resolver.resolveSelectedForUpdate(100L));
    }
}
