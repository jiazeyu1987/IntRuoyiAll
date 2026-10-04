package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.module.bpm.service.task.*;
import cn.iocoder.yudao.module.dcc.controller.admin.file.*;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.system.api.user.*;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureServiceImpl;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import jakarta.annotation.Resource;
import org.flowable.engine.ProcessEngine;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.lang.reflect.Modifier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Actual public approval -> real DCC HMAC/unified signature -> H2 projection -> real Flowable completion. */
class DccFileOwnerPublicApprovalTest extends BaseDbUnitTest {
    @Resource javax.sql.DataSource source;
    @Resource PlatformTransactionManager tx;
    @Resource DccControlledFileMapper files;
    @Resource DccControlledFileMasterMapper masters;
    @Resource DccControlledFileSignatureMapper signatures;
    @Resource DccControlledFileRouteSnapshotMapper routes;
    @Resource DccControlledFileTaskAssigneeSnapshotMapper obligations;
    @Resource ElectronicSignatureRecordMapper unifiedRows;
    JdbcTemplate jdbc;ProcessEngine engine;DccControlledFileWorkflowServiceImpl workflow;
    DccApprovalFileOwnerSelectionService owner;DccSignatureVerificationServiceImpl signing;
    DccControlledFileSignatureEvidenceServiceImpl evidence;GxpAuditService audit;
    AdminUserApi accounts; org.flowable.engine.TaskService actualTasks; BpmTaskServiceImpl bpmFacade; Map<String,String> canonicalPayloads; String bpm,task;static final long FILE=9007199254740993L,OWNER=9007199254740995L;
    String modelXml; static final String SECRET="explicit-isolated-owner-hmac-secret";
    static void wire(Object bean,Object...pairs){for(int i=0;i<pairs.length;i+=2)ReflectionTestUtils.setField(bean,(String)pairs[i],pairs[i+1]);}
    static void defaults(Object bean){for(var field:bean.getClass().getDeclaredFields())if(!Modifier.isStatic(field.getModifiers()) && field.getAnnotation(Resource.class)!=null)ReflectionTestUtils.setField(bean,field.getName(),mock(field.getType()));}
    @SuppressWarnings("unchecked") <T> T proxy(T bean){var f=new ProxyFactory(bean);f.setProxyTargetClass(true);f.addAdvice(new TransactionInterceptor(tx,new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));return (T)f.getProxy();}
    @BeforeEach void fixture() throws Exception {
        TenantContextHolder.setTenantId(1L);jdbc=new JdbcTemplate(source);try(var connection=source.getConnection()){assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:"));}
        jdbc.execute("CREATE TABLE IF NOT EXISTS system_electronic_signature(id BIGINT PRIMARY KEY,module_code VARCHAR(64),action_code VARCHAR(64),subject_type VARCHAR(128),subject_id CLOB,subject_version VARCHAR(128),actor_id BIGINT,meaning_code VARCHAR(128),meaning_label VARCHAR(128),reason CLOB,signed_at TIMESTAMP,time_evidence_id VARCHAR(128),authentication_method VARCHAR(128),content_hash VARCHAR(128),before_content_hash VARCHAR(128),after_content_hash VARCHAR(128),canonical_content_json CLOB,before_content_json CLOB,after_content_json CLOB,field_diff_json CLOB,evidence_hash VARCHAR(128),algorithm VARCHAR(64),key_version VARCHAR(128),policy_version VARCHAR(128),verification_status VARCHAR(64),idempotency_key VARCHAR(1024),command_hash VARCHAR(128),process_instance_id VARCHAR(128),task_id VARCHAR(128),node_code VARCHAR(128),node_order INT,tenant_id BIGINT,creator VARCHAR(64),updater VARCHAR(64),create_time TIMESTAMP,update_time TIMESTAMP,deleted BIT DEFAULT 0)");jdbc.update("DELETE FROM system_electronic_signature");
        var cfg=new SpringProcessEngineConfiguration();cfg.setDataSource(source);cfg.setTransactionManager(tx);cfg.setDatabaseSchemaUpdate("true");cfg.setDatabaseType("mysql");cfg.setAsyncExecutorActivate(false);cfg.setDisableIdmEngine(true);engine=cfg.buildProcessEngine();
        modelXml="""
         <?xml version="1.0" encoding="UTF-8"?>
         <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="owner">
          <process id="dcc-controlled-file-upload" isExecutable="true"><startEvent id="start"/><sequenceFlow id="one" sourceRef="start" targetRef="MATRIX_APPROVAL"/>
           <userTask id="MATRIX_APPROVAL" flowable:assignee="${actor}"/><sequenceFlow id="two" sourceRef="MATRIX_APPROVAL" targetRef="DOC_CONTROL_REVIEW"/>
           <userTask id="DOC_CONTROL_REVIEW" flowable:assignee="99"/><sequenceFlow id="three" sourceRef="DOC_CONTROL_REVIEW" targetRef="end"/><endEvent id="end"/>
          </process></definitions>
         """;
        engine.getRepositoryService().createDeployment().tenantId("1").addString("owner.bpmn20.xml",modelXml).deploy();
        engine.getIdentityService().setAuthenticatedUserId("99");try{bpm=engine.getRuntimeService().startProcessInstanceByKeyAndTenantId(DccControlledFileProcessDefinitionKeys.UPLOAD,FILE+"",Map.of("actor","99"),"1").getId();}finally{engine.getIdentityService().setAuthenticatedUserId(null);}
        task=engine.getTaskService().createTaskQuery().processInstanceId(bpm).singleResult().getId();
        masters.insert(DccControlledFileMasterDO.builder().id(10L).tenantId(1L).categoryId(2L).directoryId(3L).fileName("SOP").fileNumber("N-1").dccProjectCodeId(5L).fileTypeTaxonomyLeafId(6L).normalizedFileNumber("N-1").status("ACTIVE_CHAIN").build());
        files.insert(DccControlledFileDO.builder().id(FILE).tenantId(1L).masterId(10L).categoryId(2L).directoryId(3L).sourceFileId(100L).originalFileId(100L).fileName("SOP").title("SOP").sourceOriginalFileName("SOP.pdf").fileNumber("N-1").versionNo("A/1").status("PENDING_MATRIX_APPROVAL").requesterId(99L).submitterId(99L).dccProjectCodeId(5L).fileTypeTaxonomyId(6L).processInstanceId(bpm).processDefinitionKey(DccControlledFileProcessDefinitionKeys.UPLOAD).build());
        for(int i=1;i<=3;i++)routes.insert(DccControlledFileRouteSnapshotDO.builder().controlledFileId(FILE).routeVersionNo(1).stageNo(i).stageOrder(i).stageName("正式签核阶段").candidateSourceType("USER").approveMethod("SEQUENTIAL").requireAllApprovals(false).stageCode(i==1?"MATRIX_REVIEW":i==2?"MATRIX_APPROVAL":"DOC_CONTROL_REVIEW").resolvedUserIds("99,100").build());
        accounts=mock(AdminUserApi.class);when(accounts.getUser(OWNER)).thenReturn(new AdminUserRespDTO().setId(OWNER).setTenantId(1L).setStatus(0).setUsername("old-owner-account").setNickname("首次负责人"));when(accounts.getUser(8L)).thenReturn(new AdminUserRespDTO().setId(8L).setTenantId(1L).setStatus(0).setUsername("next-owner").setNickname("返工负责人"));
        audit=mock(GxpAuditService.class);var unified=new ElectronicSignatureServiceImpl();wire(unified,"adminUserApi",accounts,"signatureRecordMapper",unifiedRows,"gxpAuditService",audit,"subjectAdapters",List.of(new DccControlledFileSignatureSubjectAdapter()));
        var storage=mock(cn.iocoder.yudao.module.infra.service.file.FileService.class);when(storage.getFile(100L)).thenReturn(cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO.builder().id(100L).configId(1L).path("isolated/original").build());when(storage.getFileContent(1L,"isolated/original")).thenReturn("immutable-source".getBytes());
        var properties=new DccSignatureEvidenceProperties();properties.setHmacSecret(SECRET);properties.setKeyVersion("test-owner-v1");evidence=spy(new DccControlledFileSignatureEvidenceServiceImpl());canonicalPayloads=new ConcurrentHashMap<>(); doAnswer(c->{var result=(DccControlledFileSignatureEvidence)c.callRealMethod();canonicalPayloads.put(result.getEvidenceHash(),result.getCanonicalPayload());return result;}).when(evidence).createEvidence(any());wire(evidence,"controlledFileMapper",files,"fileService",storage,"signatureEvidenceProperties",properties);
        signing=new DccSignatureVerificationServiceImpl();defaults(signing);var users=mock(cn.iocoder.yudao.module.system.service.user.AdminUserService.class);when(users.getUser(anyLong())).thenAnswer(c->cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO.builder().id(c.getArgument(0)).username("approver"+c.getArgument(0)).nickname("批准人").postIds(Set.of(7L)).build());
        var posts=mock(cn.iocoder.yudao.module.system.service.dept.PostService.class);when(posts.getPostList(Set.of(7L))).thenReturn(List.of(new cn.iocoder.yudao.module.system.dal.dataobject.dept.PostDO().setId(7L).setName("正式批准岗位")));
        var permission=mock(cn.iocoder.yudao.module.system.service.permission.PermissionService.class);when(permission.getUserRoleIdListByUserId(anyLong())).thenReturn(Set.of(1L));var roles=mock(cn.iocoder.yudao.module.system.service.permission.RoleService.class);when(roles.getRoleList(Set.of(1L))).thenReturn(List.of(new cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO().setId(1L).setName("文控批准")));
        var image=mock(DccElectronicSignatureImageService.class);when(image.requireActiveSnapshot(anyLong())).thenReturn(DccElectronicSignatureImageSnapshot.builder().imageId(7L).versionNo(1).fileId(8L).sha256("image-hash").contentType("image/png").fileSize(4L).imageStatus("ENABLED").verifiedStatus("VALID").build());
        wire(signing,"adminUserService",users,"postService",posts,"permissionService",permission,"roleService",roles,"signatureEvidenceService",evidence,"signatureMapper",signatures,"signatureImageService",image,"electronicSignatureService",unified);
        owner=new DccApprovalFileOwnerSelectionService();wire(owner,"users",accounts,"files",files,"signatures",signatures);
        actualTasks=spy(engine.getTaskService());
        bpmFacade=new BpmTaskServiceImpl();defaults(bpmFacade);
        var processes=mock(BpmProcessInstanceService.class);
        when(processes.getProcessInstance(anyString())).thenAnswer(c->engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(c.getArgument(0)).includeProcessVariables().singleResult());
        var models=mock(cn.iocoder.yudao.module.bpm.service.definition.BpmModelService.class);
        when(models.getBpmnModelByDefinitionId(anyString())).thenAnswer(c->engine.getRepositoryService().getBpmnModel(c.getArgument(0)));
        wire(bpmFacade,"taskService",actualTasks,"runtimeService",engine.getRuntimeService(),"historyService",engine.getHistoryService(),"processInstanceService",processes,"modelService",models);
        var assignment=new DccWorkflowSignoffAssignmentService();wire(assignment,"taskService",engine.getTaskService());
        workflow=new DccControlledFileWorkflowServiceImpl();defaults(workflow);wire(workflow,"controlledFileMapper",files,"controlledFileMasterMapper",masters,"signatureMapper",signatures,"routeSnapshotMapper",routes,"taskAssigneeSnapshotMapper",obligations,"bpmTaskService",bpmFacade,"signatureVerificationService",signing,"fileOwnerSelection",owner,"signoffAssignmentService",assignment);
        var permissions=mock(cn.iocoder.yudao.module.system.api.permission.PermissionApi.class);when(permissions.hasAnyPermissions(any(),any(String[].class))).thenReturn(true);wire(workflow,"permissionApi",permissions);workflow=proxy(workflow);
    }
    @AfterEach void cleanup(){try{if(engine!=null){for(var p:engine.getRuntimeService().createProcessInstanceQuery().list())engine.getRuntimeService().deleteProcessInstance(p.getId(),"isolated owner cleanup");engine.close();}}finally{jdbc.update("DELETE FROM system_electronic_signature");SecurityContextHolder.clearContext();TenantContextHolder.clear();}}
    void login(long actor){var user=new LoginUser();user.setId(actor);user.setTenantId(1L);user.setUserType(2);user.setInfo(Map.of("username","approver"+actor,"nickname","批准人"));SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,null,List.of()));}
    DccControlledFileApproveTaskReqVO request(String id,long selected){var req=new DccControlledFileApproveTaskReqVO();req.setTaskId(id);req.setPassword("isolated-credential");req.setReason("本次真实批准意见");req.setFileOwnerUserId(selected);return req;}
    @Test void actualHttpApprovalCommitsRealOwnerHmacUnifiedSignatureAndBpmTaskTogether() throws Exception {
        login(99L);var controller=new DccControlledFileController();wire(controller,"workflowService",workflow);
        var response=MockMvcBuilders.standaloneSetup(controller).build().perform(post("/dcc/controlled-files/"+FILE+"/approve-task").contentType("application/json").content("{\"taskId\":\""+task+"\",\"password\":\"isolated-credential\",\"reason\":\"本次真实批准意见\",\"fileOwnerUserId\":\""+OWNER+"\"}")).andReturn();
        assertEquals(200,response.getResponse().getStatus());var json=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(response.getResponse().getContentAsString(StandardCharsets.UTF_8),com.fasterxml.jackson.databind.JsonNode.class);assertEquals(0,json.get("code").intValue());
        var saved=files.selectById(FILE);assertEquals(OWNER,saved.getFileOwnerUserId());assertEquals("首次负责人",saved.getFileOwnerNicknameSnapshot());assertEquals(bpm,saved.getFileOwnerProcessInstanceId());
        var signed=signatures.selectById(saved.getFileOwnerSignatureId());assertTrue(signed.getComment().contains(OWNER+""));assertTrue(signed.getComment().contains("old-owner-account"));assertEquals("v4-workflow",signed.getEvidencePayloadVersion());assertEquals(task,signed.getTaskId());assertEquals(1,unifiedRows.selectList().size());
        var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
        assertEquals(signed.getEvidenceHash(),HexFormat.of().formatHex(mac.doFinal(canonicalPayloads.get(signed.getEvidenceHash()).getBytes(StandardCharsets.UTF_8))));
        assertEquals(signed.getComment(),cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(canonicalPayloads.get(signed.getEvidenceHash()),com.fasterxml.jackson.databind.JsonNode.class).get("reasonText").asText());
        assertEquals(signed.getEvidenceHash(),cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(unifiedRows.selectList().get(0).getCanonicalContentJson(),com.fasterxml.jackson.databind.JsonNode.class).get("legacyEvidenceHash").asText());assertNull(engine.getTaskService().createTaskQuery().taskId(task).singleResult());
    }
    @Test void bindCannotAttachDifferentSelectionToAnAuthenticSignedOwnerReason() {
        login(99L);var file=files.selectById(FILE);var selected=owner.prepare(file,cn.iocoder.yudao.module.dcc.enums.DccControlledFileStageCodeEnum.MATRIX_APPROVAL,OWNER);String reason=owner.signedReason("批准",selected);
        new org.springframework.transaction.support.TransactionTemplate(tx).executeWithoutResult(s->{var signature=signing.verifyPasswordAndCreateWorkflowSignature(99L,FILE,task,bpm,"MATRIX_APPROVAL","APPROVE","isolated",reason);
            assertThrows(IllegalStateException.class,()->owner.bind(file,task,99L,signature.getSignatureId(),new DccApprovalFileOwnerSelectionService.Selection(8L,"next-owner","返工负责人"),reason));
        });
        assertNull(files.selectById(FILE).getFileOwnerUserId());
    }
    @Test void realSameBpmReturnAndReapprovalKeepsBothSignedOwnersAndProjectsTheNewChoice() {
        login(99L);workflow.approveTask(99L,FILE,request(task,OWNER));var first=signatures.selectById(files.selectById(FILE).getFileOwnerSignatureId());
        // Real generic BPM return uses its formal model guard, comments/history and Flowable state change.
        returnToApproval();
        jdbc.update("UPDATE dcc_controlled_file SET status='PENDING_MATRIX_APPROVAL' WHERE id=?",FILE);String again=engine.getTaskService().createTaskQuery().processInstanceId(bpm).singleResult().getId();
        workflow.approveTask(99L,FILE,request(again,8L));assertEquals(8L,files.selectById(FILE).getFileOwnerUserId());assertEquals(2,signatures.selectListByControlledFileId(FILE).size());assertEquals(first.getComment(),signatures.selectById(first.getId()).getComment());assertTrue(first.getComment().contains("首次负责人"));
    }
    @Test void realLateFlowableFailureRollsBackOwnerBothSignaturesAndTaskCompletion() {
        login(99L);
        doAnswer(c->{c.callRealMethod();throw new IllegalStateException("isolated late Flowable failure after complete");})
                .when(actualTasks).complete(eq(task),anyMap(),eq(true));
        assertThrows(IllegalStateException.class,()->workflow.approveTask(99L,FILE,request(task,OWNER)));
        assertNull(files.selectById(FILE).getFileOwnerUserId());assertEquals(0,signatures.selectListByControlledFileId(FILE).size());assertEquals(0,unifiedRows.selectList().size());
        assertNotNull(engine.getTaskService().createTaskQuery().taskId(task).singleResult());
        assertEquals(0,engine.getHistoryService().createHistoricTaskInstanceQuery().taskId(task).finished().count());
    }
    @Test void unifiedAuditFailureRollsBackOwnerAndBothSignatureRowsBeforeAdvancingBpm() {
        login(99L);doThrow(new IllegalStateException("isolated unified audit failure")).when(audit).append(any());
        assertThrows(IllegalStateException.class,()->workflow.approveTask(99L,FILE,request(task,OWNER)));
        assertNull(files.selectById(FILE).getFileOwnerUserId());assertEquals(0,signatures.selectListByControlledFileId(FILE).size());assertEquals(0,unifiedRows.selectList().size());
        assertNotNull(engine.getTaskService().createTaskQuery().taskId(task).singleResult());
    }
    @Test void actualCompletedTaskReplayCannotReplaceOwnerOrCreateAnotherSignature() {
        login(99L);workflow.approveTask(99L,FILE,request(task,OWNER));var saved=files.selectById(FILE);
        assertThrows(RuntimeException.class,()->workflow.approveTask(99L,FILE,request(task,8L)));
        assertEquals(saved.getFileOwnerSignatureId(),files.selectById(FILE).getFileOwnerSignatureId());assertEquals(OWNER,files.selectById(FILE).getFileOwnerUserId());
        assertEquals(1,signatures.selectListByControlledFileId(FILE).size());assertEquals(1,unifiedRows.selectList().size());
    }
    @ParameterizedTest @ValueSource(ints={1,2,3,4})
    void actualPublicApprovalRejectsWrongActorTenantInactiveOwnerAndMissingOwnerWithZeroFacts(int invalid) {
        login(invalid==1?100L:99L);
        if(invalid==2)when(accounts.getUser(OWNER)).thenReturn(new AdminUserRespDTO().setId(OWNER).setTenantId(2L).setStatus(0).setUsername("foreign").setNickname("外租户"));
        if(invalid==3)when(accounts.getUser(OWNER)).thenReturn(new AdminUserRespDTO().setId(OWNER).setTenantId(1L).setStatus(1).setUsername("inactive").setNickname("停用"));
        var req=request(task,OWNER);if(invalid==4)req.setFileOwnerUserId(null);
        assertThrows(RuntimeException.class,()->workflow.approveTask(invalid==1?100L:99L,FILE,req));
        assertNull(files.selectById(FILE).getFileOwnerUserId());assertEquals(0,signatures.selectListByControlledFileId(FILE).size());assertEquals(0,unifiedRows.selectList().size());
        assertNotNull(engine.getTaskService().createTaskQuery().taskId(task).singleResult());
    }
    void startOtherNativeProcess(String key,boolean parallel) {
        engine.getRuntimeService().deleteProcessInstance(bpm,"isolated fixture replacement");
        String xml=modelXml.replace(DccControlledFileProcessDefinitionKeys.UPLOAD,key);
        if(parallel)xml=xml.replace("<userTask id=\"MATRIX_APPROVAL\" flowable:assignee=\"${actor}\"/>",
                "<userTask id=\"MATRIX_APPROVAL\" flowable:assignee=\"${actor}\"><multiInstanceLoopCharacteristics isSequential=\"false\" flowable:collection=\"actors\" flowable:elementVariable=\"actor\"/></userTask>");
        engine.getRepositoryService().createDeployment().tenantId("1").addString("owner-parallel.bpmn20.xml",xml).deploy();
        var variables=parallel?Map.<String,Object>of("actors",List.of("99","100")):Map.<String,Object>of("actor","99");
        bpm=engine.getRuntimeService().startProcessInstanceByKeyAndTenantId(key,FILE+"",variables,"1").getId();
        task=engine.getTaskService().createTaskQuery().processInstanceId(bpm).taskAssignee("99").singleResult().getId();
        jdbc.update("UPDATE dcc_controlled_file SET process_instance_id=?,process_definition_key=? WHERE id=?",bpm,key,FILE);
    }
    @Test void actualRevisionApprovalUsesTheSameOwnerHmacAndBpmContract() {
        startOtherNativeProcess(DccControlledFileProcessDefinitionKeys.REVISION,false);login(99L);
        workflow.approveTask(99L,FILE,request(task,OWNER));var saved=files.selectById(FILE);var signed=signatures.selectById(saved.getFileOwnerSignatureId());
        assertEquals(OWNER,saved.getFileOwnerUserId());assertEquals(bpm,signed.getProcessInstanceId());assertEquals("v4-workflow",signed.getEvidencePayloadVersion());
        assertEquals(1,unifiedRows.selectList().size());assertNull(engine.getTaskService().createTaskQuery().taskId(task).singleResult());
    }
    @Test void actualParallelApprovalsSerializeLatestProjectionAndKeepEachActorsImmutableSignature() throws Exception {
        startOtherNativeProcess(DccControlledFileProcessDefinitionKeys.UPLOAD,true);
        String second=engine.getTaskService().createTaskQuery().processInstanceId(bpm).taskAssignee("100").singleResult().getId();
        var ready=new CountDownLatch(2);var go=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try {
            var firstFuture=pool.submit(()->concurrentApprove(99L,task,OWNER,ready,go));
            var secondFuture=pool.submit(()->concurrentApprove(100L,second,8L,ready,go));
            assertTrue(ready.await(10,TimeUnit.SECONDS));go.countDown();firstFuture.get(20,TimeUnit.SECONDS);secondFuture.get(20,TimeUnit.SECONDS);
            var saved=files.selectById(FILE);var records=signatures.selectListByControlledFileId(FILE);assertEquals(2,records.size());assertEquals(2,unifiedRows.selectList().size());
            var current=signatures.selectById(saved.getFileOwnerSignatureId());assertEquals(saved.getFileOwnerApprovalTaskId(),current.getTaskId());assertTrue(current.getComment().contains(saved.getFileOwnerNicknameSnapshot()));
            assertTrue(records.stream().anyMatch(r->r.getActorId()==99L && r.getComment().contains("首次负责人")));
            assertTrue(records.stream().anyMatch(r->r.getActorId()==100L && r.getComment().contains("返工负责人")));
            assertEquals(1,engine.getTaskService().createTaskQuery().processInstanceId(bpm).taskDefinitionKey("DOC_CONTROL_REVIEW").count());
        } finally {go.countDown();pool.shutdownNow();assertTrue(pool.awaitTermination(10,TimeUnit.SECONDS));}
    }
    void concurrentApprove(long actor,String actualTask,long chosen,CountDownLatch ready,CountDownLatch go) {
        TenantContextHolder.setTenantId(1L);login(actor);ready.countDown();
        try {if(!go.await(10,TimeUnit.SECONDS))throw new IllegalStateException("isolated concurrency start timeout");workflow.approveTask(actor,FILE,request(actualTask,chosen));}
        catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}
        finally {SecurityContextHolder.clearContext();TenantContextHolder.clear();}
    }
    @Test void renamedAccountDoesNotRewritePriorHmacButFreshApprovalSnapshotsItsCurrentName() {
        login(99L);workflow.approveTask(99L,FILE,request(task,OWNER));var prior=signatures.selectById(files.selectById(FILE).getFileOwnerSignatureId());
        when(accounts.getUser(OWNER)).thenReturn(new AdminUserRespDTO().setId(OWNER).setTenantId(1L).setStatus(0).setUsername("renamed-owner").setNickname("新的账号名字"));
        assertEquals("首次负责人",files.selectById(FILE).getFileOwnerNicknameSnapshot());assertEquals(prior.getComment(),signatures.selectById(prior.getId()).getComment());
        returnToApproval();
        jdbc.update("UPDATE dcc_controlled_file SET status='PENDING_MATRIX_APPROVAL' WHERE id=?",FILE);
        String next=engine.getTaskService().createTaskQuery().processInstanceId(bpm).singleResult().getId();workflow.approveTask(99L,FILE,request(next,OWNER));
        assertEquals("新的账号名字",files.selectById(FILE).getFileOwnerNicknameSnapshot());assertEquals(prior.getComment(),signatures.selectById(prior.getId()).getComment());
        assertEquals(2,signatures.selectListByControlledFileId(FILE).size());
    }
    void returnToApproval() {
        var review=engine.getTaskService().createTaskQuery().processInstanceId(bpm).singleResult();
        proxy(bpmFacade).returnTask(99L,new cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskReturnReqVO()
                .setId(review.getId()).setTargetTaskDefinitionKey("MATRIX_APPROVAL").setReason("真实退回再批准"));
        var historic=engine.getHistoryService().createHistoricTaskInstanceQuery().taskId(review.getId()).includeTaskLocalVariables().singleResult();
        assertEquals(5,historic.getTaskLocalVariables().get("TASK_STATUS"));assertNotNull(historic.getEndTime());
    }
}
