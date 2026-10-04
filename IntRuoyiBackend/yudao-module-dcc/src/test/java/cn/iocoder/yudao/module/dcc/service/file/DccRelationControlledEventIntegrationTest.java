package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.service.file.relations.*;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessServiceImpl;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccSignoffAssignmentReqVO;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskServiceImpl;
import cn.iocoder.yudao.module.system.api.notify.*;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.PostApi;
import cn.iocoder.yudao.module.system.service.notify.*;
import cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyTemplateDO;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.flowable.engine.TaskService;
import org.flowable.engine.ProcessEngine;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_ACCESS_DENIED;
import cn.iocoder.yudao.framework.common.exception.ServiceException;

@Import({DccControlledFileLifecycleService.class,DccWorkflowDatePolicy.class,DccWorkflowFileStateAudit.class,
        DccObsoleteRetentionService.class,DccControlledFileNameClaimService.class,
        DccLatestControlledFileResolverImpl.class,DccRelationRemediationService.class,DccRelationStore.class,
        DccRelationAccessPolicyImpl.class,DccProjectAccessServiceImpl.class,
        DccRelationControlledEventConsumer.class,DccRelationNotificationPostCommitScheduler.class,
        DccRelationNotificationDispatcher.class,DccRelationPlatformNotificationSender.class,
        NotifyMessageSendApiImpl.class,NotifySendServiceImpl.class,NotifyMessageServiceImpl.class,
        GxpAuditServiceImpl.class,DccRelationControlledEventIntegrationTest.TestBeans.class})
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(scripts="/sql/dcc_b_gxp_audit_tables.sql",executionPhase=Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts="/sql/dcc_b_gxp_audit_clean.sql",executionPhase=Sql.ExecutionPhase.AFTER_TEST_METHOD)
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:dcc_d_control_combination;MODE=MYSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1;NON_KEYWORDS=value;LOCK_TIMEOUT=10000")
class DccRelationControlledEventIntegrationTest extends BaseDbUnitTest {
    @TestConfiguration(proxyBeanMethods=false) static class TestBeans {
        @Bean JdbcTemplate jdbc(javax.sql.DataSource source){return new JdbcTemplate(source);}
        @Bean DccControlledFileVersionPolicy versionPolicy(){return DccControlledFileVersionPolicy.defaultPolicy();}
        @Bean({"applicationTaskExecutor","testQueue"}) TestQueue testQueue(){return new TestQueue();}
    }
    static class TestQueue implements Executor {final List<Runnable> tasks=new ArrayList<>();public void execute(Runnable task){tasks.add(task);}void drain(){while(!tasks.isEmpty())tasks.remove(0).run();}}
    @Resource JdbcTemplate jdbc;@Resource PlatformTransactionManager manager;
    @Resource DccControlledFileLifecycleService lifecycle;@Resource DccWorkflowDatePolicy dates;
    @Resource DccRelationRemediationService remediation;@Resource DccLatestControlledFileResolver resolver;
    @Resource DccControlledFileMapper files;@Resource DccControlledFileMasterMapper masters;
    @Resource DccControlledFileTaskAssigneeSnapshotMapper obligations;@Resource DccControlledFileSignatureMapper signatures;
    @Resource ApplicationEventPublisher publisher;@Resource TestQueue queue;
    @MockitoBean DccControlledFileQueryService query;
    @MockitoBean TaskService tasks;
    @MockitoBean AdminUserApi users;
    @MockitoBean PermissionApi permissions;
    @MockitoBean DeptApi departments;
    @MockitoBean RoleApi roles;
    @MockitoBean PostApi posts;
    @MockitoBean NotifyTemplateService templates;
    @BeforeEach void seed() throws Exception {
        try(var connection=jdbc.getDataSource().getConnection()){assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:"));}
        for(String sql:Files.readString(Path.of("../sql/mysql/20260930_dcc_d_relations.sql")).replaceAll("(?m)^--.*$","").split(";"))if(!sql.isBlank())jdbc.execute(sql);
        for(String sql:new ClassPathResource("sql/dcc_d_platform_message_fixture.sql").getContentAsString(StandardCharsets.UTF_8).split(";"))if(!sql.isBlank())jdbc.execute(sql);
        cleanupOwnTables();queue.tasks.clear();dates.setZoneId("Asia/Singapore");dates.setReminderLeadDays(3);
        var login=new LoginUser();login.setId(7L);login.setTenantId(1L);login.setUserType(2);login.setInfo(Map.of("username","dcc-d-control",LoginUser.INFO_KEY_NICKNAME,"文控"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login,null,List.of()));
        policy("dcc.relation.arrange");policy("dcc.relation.controlled");
        policy("dcc.controlled-file.control");policy("dcc.controlled-file.activate");policy("dcc.controlled-file.auto-obsolete");
        var template=NotifyTemplateDO.builder().id(710L).code("dcc_relation_remediation").type(2).nickname("DCC系统").status(0)
                .content("{fileNumber} {versionNo} 关联整改期限 {dueAt}")
                .params(List.of("fileNumber","versionNo","relatedMasterId","dueAt","sourceControlledFileId","detailUrl")).build();
        when(templates.getNotifyTemplateByCodeFromCache("dcc_relation_remediation")).thenReturn(template);
        when(templates.formatNotifyTemplateContent(anyString(),anyMap())).thenAnswer(i->new NotifyTemplateServiceImpl().formatNotifyTemplateContent(i.getArgument(0),i.getArgument(1)));
        when(users.getUser(8L)).thenReturn(new AdminUserRespDTO().setId(8L).setStatus(0));
        when(permissions.getUserRoleIdListByUserId(8L)).thenReturn(Set.of());
        var taskQuery=mock(TaskQuery.class,RETURNS_SELF);
        when(tasks.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.list()).thenReturn(List.of(mock(Task.class)));
        jdbc.update("INSERT INTO dcc_project_code(id,project_name,project_code,status,tenant_id) VALUES(1,'源项目','SOURCE-P','ENABLE',1),(2,'目标项目','TARGET-P','ENABLE',1)");
        jdbc.update("INSERT INTO dcc_project_access_rule(dcc_project_code_id,subject_type,subject_id,access_level,active,change_reason,tenant_id) VALUES(2,'USER',8,'EDIT',TRUE,'组合验证正式整改权限',1)");
        jdbc.update("INSERT INTO dcc_controlled_file_master(id,category_id,file_name,file_number,status,tenant_id,dcc_project_code_id,current_active_controlled_file_id,latest_controlled_file_id) VALUES(10,1,'主.pdf','SOURCE','ACTIVE_CHAIN',1,1,1,1),(20,1,'目标.pdf','TARGET','ACTIVE_CHAIN',1,2,200,201)");
        insertFile(1,10,"A/1","ACTIVE","round-old",LocalDate.now().minusDays(5));
        insertFile(2,10,"B/1","READY_TO_PUBLISH","round-new",LocalDate.now().plusDays(10));
        insertFile(200,20,"A/1","ACTIVE","target-old",LocalDate.now().minusDays(5));
        insertFile(201,20,"B/1","CONTROLLED_PENDING_EFFECTIVE","target-new",LocalDate.now().plusDays(10));
        jdbc.update("UPDATE dcc_controlled_file SET controlled_time=CURRENT_TIMESTAMP,activated_time=CURRENT_TIMESTAMP WHERE id IN(1,200)");
        jdbc.update("INSERT INTO dcc_controlled_file_name_claim(tenant_id,master_id,normalized_name,source_original_file_name,normalized_file_number,deleted) VALUES(1,10,'主.pdf','主.pdf','SOURCE',0)");
        jdbc.update("UPDATE dcc_controlled_file SET controlled_time=CURRENT_TIMESTAMP WHERE id=201");
        jdbc.update("INSERT INTO dcc_controlled_file_related_file(controlled_file_id,related_controlled_file_id,project_code_id,related_master_id,related_file_name_snapshot,related_version_no_snapshot,relation_source,tenant_id) VALUES(2,200,2,20,'目标.pdf','A/1','UPLOAD',1)");
        jdbc.update("INSERT INTO dcc_controlled_file_task_assignee_snapshot(controlled_file_id,stage_code,department_id,assignee_user_id,leader_user_id,process_instance_id,bpm_task_id,obligation_id,tenant_id) VALUES(2,'MATRIX_REVIEW',10,7,7,'round-new','signoff-task','relation-obligation',1)");
        new TransactionTemplate(manager).executeWithoutResult(s->remediation.saveArrangements(7L,2L,"round-new",List.of(new Arrangement(20L,8L,LocalDate.now().plusDays(5).atTime(12,0))),"会签整改安排"));
    }
    void insertFile(long id,long master,String version,String status,String round,LocalDate date){
        jdbc.update("INSERT INTO dcc_controlled_file(id,master_id,category_id,directory_id,source_file_id,original_file_id,file_name,title,file_number,version_no,status,submitter_id,requester_id,tenant_id,published_file_id,stamped_file_id,effective_date,process_instance_id,dcc_project_code_id) VALUES(?,?,1,1,100,100,'测试.pdf','测试',?,?,?,7,7,1,100,100,?,?,?)",id,master,master==10?"SOURCE":"TARGET",version,status,date,round,master==10?1:2);
    }
    void policy(String id){jdbc.update("INSERT INTO gxp_audit_policy_operation(tenant_id,policy_version,operation_id,source_type,source_locator,domain,subject_type,action_type,reason_policy,signature_policy,state_policy,retention_class,test_ids,owner,applicability,active) VALUES(1,'DCC-D-ISOLATED',?,'SERVICE_METHOD','DCC.TEST','DCC','DCC_RELATION','CREATE','REQUIRED','NOT_REQUIRED','ABSENT_TO_PRESENT','GXP_CONTROLLED_DOCUMENT','INT-D-01','dcc-d','GXP',TRUE)",id);}
    void cleanupOwnTables(){for(String table:List.of("dcc_relation_change_command","dcc_relation_notification_outbox","dcc_relation_remediation_task","dcc_relation_controlled_event","dcc_relation_arrangement","dcc_current_file_relation","dcc_current_file_relation_set","system_notify_message"))jdbc.update("DELETE FROM "+table);jdbc.update("DELETE FROM dcc_controlled_file_related_file WHERE controlled_file_id=2");jdbc.update("DELETE FROM dcc_controlled_file_task_assignee_snapshot WHERE controlled_file_id=2");}
    @AfterEach void after(){cleanupOwnTables();queue.tasks.clear();SecurityContextHolder.clearContext();}
    void control(){new TransactionTemplate(manager).executeWithoutResult(s->{lifecycle.completeControl(files.selectByIdAndTenantForUpdate(1L,2L),masters.selectByIdForUpdate(10L),7L);assertEquals(0,count("system_notify_message"));});}
    int count(String table){return jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class);}
    DccControlledFileLifecycleEvent controlEvent(){return jdbc.queryForObject("SELECT event_key,event_type,tenant_id,master_id,controlled_file_id,previous_active_file_id,version_no,approval_process_instance_id,occurred_at FROM dcc_workflow_lifecycle_event WHERE event_type='CONTROLLED'",
            (rs,n)->new DccControlledFileLifecycleEvent(rs.getString(1),rs.getString(2),rs.getLong(3),rs.getLong(4),rs.getLong(5),rs.getLong(6),rs.getString(7),rs.getString(8),rs.getObject(9,LocalDateTime.class)));}
    @Test void realAControlEventCreatesDTasksInItsTransactionAndNotifiesAfterCommitOnly(){
        control();assertEquals(1,count("dcc_relation_controlled_event"));assertEquals(1,count("dcc_relation_remediation_task"));assertEquals(1,count("dcc_relation_notification_outbox"));
        assertEquals(1L,masters.selectById(10L).getCurrentActiveControlledFileId());assertEquals(2L,masters.selectById(10L).getLatestControlledFileId());
        assertEquals(201L,resolver.resolveLatest(20L).controlledFileId());assertEquals(0,count("system_notify_message"));assertEquals(1,queue.tasks.size());
        queue.drain();assertEquals(1,count("system_notify_message"));assertEquals("SENT",jdbc.queryForObject("SELECT status FROM dcc_relation_notification_outbox",String.class));
        var event=controlEvent();new TransactionTemplate(manager).executeWithoutResult(s->publisher.publishEvent(event));queue.drain();assertEquals(1,count("system_notify_message"));assertEquals(1,count("dcc_relation_remediation_task"));
    }
    @Test void onePersistedControlEventSchedulesExactlyOneNotificationDelivery() {
        control();
        assertEquals(1, count("dcc_workflow_lifecycle_event"));
        assertEquals(1, count("dcc_relation_controlled_event"));
        assertEquals(1, count("dcc_relation_notification_outbox"));
        assertEquals(1, queue.tasks.size(), "one A control transition must enter D exactly once");
        queue.drain();
        assertEquals(1, count("system_notify_message"));
    }
    @Test void missingDAuditRollsBackAControlStatePointersAndDRequiredFacts(){
        jdbc.update("DELETE FROM gxp_audit_policy_operation WHERE operation_id='dcc.relation.controlled'");
        assertThrows(RuntimeException.class,this::control);
        assertNull(files.selectById(2L).getControlledTime());assertEquals(1L,masters.selectById(10L).getLatestControlledFileId());
        assertEquals(0,count("dcc_workflow_lifecycle_event"));assertEquals(0,count("dcc_relation_controlled_event"));assertEquals(0,count("dcc_relation_remediation_task"));assertTrue(queue.tasks.isEmpty());
    }
    @Test void activationDoesNotRepeatControlledNotificationOrResetDeadline(){
        control();queue.drain();var deadline=jdbc.queryForObject("SELECT due_at FROM dcc_relation_remediation_task",LocalDateTime.class);
        jdbc.update("UPDATE dcc_controlled_file SET effective_date=? WHERE id=2",LocalDate.now());lifecycle.activateDue(2L);queue.drain();
        assertEquals(1,count("system_notify_message"));assertEquals(deadline,jdbc.queryForObject("SELECT due_at FROM dcc_relation_remediation_task",LocalDateTime.class));
        assertEquals(2L,masters.selectById(10L).getCurrentActiveControlledFileId());
        var obsoleteTime=files.selectById(1L).getObsoletedTime();assertNotNull(obsoleteTime);
        assertEquals(obsoleteTime,jdbc.queryForObject("SELECT obsolete_time FROM dcc_controlled_file_name_claim WHERE tenant_id=1 AND master_id=10",LocalDateTime.class));
        assertEquals(obsoleteTime.plusYears(20),jdbc.queryForObject("SELECT retain_until FROM dcc_controlled_file_name_claim WHERE tenant_id=1 AND master_id=10",LocalDateTime.class));
        assertEquals(0,jdbc.queryForObject("SELECT deleted FROM dcc_controlled_file_name_claim WHERE tenant_id=1 AND master_id=10",Integer.class));
        assertFalse(lifecycle.activateDue(2L));queue.drain();assertEquals(1,count("system_notify_message"));
        assertEquals(obsoleteTime.plusYears(20),jdbc.queryForObject("SELECT retain_until FROM dcc_controlled_file_name_claim WHERE tenant_id=1 AND master_id=10",LocalDateTime.class));
    }
    @Test void failedRealIdentityRetentionRollsBackActivationWithoutRepeatingCommittedControlRemediation(){
        control();queue.drain();var deadline=jdbc.queryForObject("SELECT due_at FROM dcc_relation_remediation_task",LocalDateTime.class);
        jdbc.update("DELETE FROM dcc_controlled_file_name_claim WHERE tenant_id=1 AND master_id=10");
        jdbc.update("UPDATE dcc_controlled_file SET effective_date=? WHERE id=2",LocalDate.now());
        var failure=assertThrows(IllegalStateException.class,()->lifecycle.activateDue(2L));
        assertEquals("no reserved source name/number identity for obsolete master",failure.getMessage());
        assertEquals("ACTIVE",files.selectById(1L).getStatus());assertNull(files.selectById(1L).getObsoletedTime());
        assertEquals("CONTROLLED_PENDING_EFFECTIVE",files.selectById(2L).getStatus());assertNull(files.selectById(2L).getActivatedTime());
        assertEquals(1L,masters.selectById(10L).getCurrentActiveControlledFileId());assertEquals(2L,masters.selectById(10L).getLatestControlledFileId());
        assertEquals(1,count("dcc_workflow_lifecycle_event"));assertEquals(1,count("dcc_relation_controlled_event"));
        assertEquals(1,count("dcc_relation_remediation_task"));assertEquals(1,count("system_notify_message"));assertTrue(queue.tasks.isEmpty());
        assertEquals(deadline,jdbc.queryForObject("SELECT due_at FROM dcc_relation_remediation_task",LocalDateTime.class));
    }
    @Test void unregisteredEventKeyCannotConsumeAFileFactOrCreateAnyRemediation(){
        jdbc.update("UPDATE dcc_controlled_file SET controlled_time=?,status='CONTROLLED_PENDING_EFFECTIVE' WHERE id=2",dates.now());
        jdbc.update("UPDATE dcc_controlled_file_master SET latest_controlled_file_id=2 WHERE id=10");
        var time=files.selectById(2L).getControlledTime();
        assertThrows(RuntimeException.class,()->new TransactionTemplate(manager).executeWithoutResult(s->publisher.publishEvent(
                new DccControlledFileLifecycleEvent("invented-key","CONTROLLED",1L,10L,2L,1L,"B/1","round-new",time))));
        assertEquals(0,count("dcc_relation_controlled_event"));assertEquals(0,count("dcc_relation_notification_outbox"));assertTrue(queue.tasks.isEmpty());
    }
    @ParameterizedTest
    @ValueSource(strings={"VIEW","INACTIVE","EXPIRED","DISABLED"})
    void withdrawnFormalRemediationAuthorityRollsBackAControlAndSchedulesNothing(String withdrawal){
        switch(withdrawal){
            case "VIEW" -> jdbc.update("UPDATE dcc_project_access_rule SET access_level='VIEW' WHERE subject_id=8");
            case "INACTIVE" -> jdbc.update("UPDATE dcc_project_access_rule SET active=FALSE WHERE subject_id=8");
            case "EXPIRED" -> jdbc.update("UPDATE dcc_project_access_rule SET expire_time=? WHERE subject_id=8",LocalDateTime.now().minusSeconds(5));
            case "DISABLED" -> when(users.getUser(8L)).thenReturn(new AdminUserRespDTO().setId(8L).setStatus(1));
            default -> throw new AssertionError(withdrawal);
        }
        var failure=assertThrows(RuntimeException.class,this::control);
        if("DISABLED".equals(withdrawal)){
            assertInstanceOf(DccRelationFailure.class,failure);
            assertEquals("DCC_RELATION_ASSIGNEE_UNAVAILABLE",failure.getMessage());
        }else{
            assertEquals(DCC_PROJECT_ACCESS_DENIED.getCode(),assertInstanceOf(ServiceException.class,failure).getCode());
        }
        assertNull(files.selectById(2L).getControlledTime());assertEquals("READY_TO_PUBLISH",files.selectById(2L).getStatus());
        assertEquals(1L,masters.selectById(10L).getLatestControlledFileId());
        assertEquals(0,count("dcc_workflow_lifecycle_event"));assertEquals(0,count("dcc_relation_controlled_event"));
        assertEquals(0,count("dcc_relation_remediation_task"));assertEquals(0,count("dcc_relation_notification_outbox"));
        assertEquals(0,count("system_notify_message"));assertTrue(queue.tasks.isEmpty());assertEquals(1,count("dcc_relation_arrangement"));
    }
    @Test void lateHistoricalEventCannotRegressCurrentRelationsUsingACachedLatestPointer() throws Exception {
        var oldTime=dates.now().minusDays(5);
        jdbc.update("UPDATE dcc_controlled_file SET controlled_time=? WHERE id=1",oldTime);
        jdbc.update("INSERT INTO dcc_workflow_lifecycle_event(tenant_id,event_key,event_type,master_id,controlled_file_id,previous_active_file_id,version_no,approval_process_instance_id,occurred_at,delivery_status) VALUES(1,'DCC:1:1:round-old:CONTROLLED','CONTROLLED',10,1,NULL,'A/1','round-old',?,'PENDING')",oldTime);
        var oldEvent=new DccControlledFileLifecycleEvent("DCC:1:1:round-old:CONTROLLED","CONTROLLED",1L,10L,1L,null,"A/1","round-old",oldTime);
        var pool=Executors.newSingleThreadExecutor();
        try{
            new TransactionTemplate(manager).executeWithoutResult(status->{
                assertEquals(1L,resolver.resolveLatest(10L).controlledFileId());
                try{
                    pool.submit(()->{
                        TenantContextHolder.setTenantId(1L);
                        var actor=new LoginUser();actor.setId(7L);actor.setTenantId(1L);actor.setUserType(2);actor.setInfo(Map.of("username","dcc-d-control",LoginUser.INFO_KEY_NICKNAME,"文控"));
                        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor,null,List.of()));
                        try{control();}finally{SecurityContextHolder.clearContext();TenantContextHolder.clear();}
                    }).get(10,TimeUnit.SECONDS);
                }catch(Exception error){throw new AssertionError("actual A control transaction failed",error);}
                assertEquals(1L,resolver.resolveLatest(10L).controlledFileId(),"ordinary Mapper resolution exposes its earlier transaction cache");
                publisher.publishEvent(oldEvent);
            });
            assertEquals(2L,jdbc.queryForObject("SELECT controlled_file_id FROM dcc_current_file_relation_set WHERE tenant_id=1 AND source_master_id=10",Long.class));
            assertEquals(0L,jdbc.queryForObject("SELECT row_version FROM dcc_current_file_relation_set WHERE tenant_id=1 AND source_master_id=10",Long.class));
            assertEquals(20L,jdbc.queryForObject("SELECT related_master_id FROM dcc_current_file_relation WHERE tenant_id=1 AND source_master_id=10",Long.class));
            assertEquals(2,count("dcc_relation_controlled_event"));assertEquals(1,count("dcc_relation_remediation_task"));
            queue.drain();assertEquals(1,count("system_notify_message"));
        }finally{pool.shutdown();assertTrue(pool.awaitTermination(15,TimeUnit.SECONDS));}
    }
    @Test void latestLockProjectionRequiresAnExistingCallerTransaction(){
        assertThrows(org.springframework.transaction.IllegalTransactionStateException.class,()->resolver.resolveLatestForUpdate(10L));
    }
    @ParameterizedTest
    @ValueSource(strings={"SELECTED","UNSELECTED","MISSING_ARRANGEMENT_POLICY"})
    void realSignedArrangementAndFlowableTaskFeedTheSameControlAndNotificationFacts(String scenario) throws Exception {
        boolean selected=!"UNSELECTED".equals(scenario),auditFailure="MISSING_ARRANGEMENT_POLICY".equals(scenario);
        int initialAudit=count("gxp_audit_event");
        jdbc.update("DELETE FROM dcc_relation_arrangement WHERE source_file_id=2");
        jdbc.update("DELETE FROM dcc_controlled_file_task_assignee_snapshot WHERE controlled_file_id=2");
        var configuration=new SpringProcessEngineConfiguration();configuration.setDataSource(jdbc.getDataSource());configuration.setTransactionManager(manager);
        configuration.setDatabaseType("mysql");configuration.setDatabaseSchemaUpdate("true");configuration.setAsyncExecutorActivate(false);configuration.setDisableIdmEngine(true);
        ProcessEngine engine=configuration.buildProcessEngine();String round=null;
        try{
            String model="""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="DCC-signed-control-combination">
                      <process id="dcc-d-signed-control-combination" isExecutable="true">
                        <startEvent id="start"/><sequenceFlow id="toSignoff" sourceRef="start" targetRef="MATRIX_REVIEW"/>
                        <userTask id="MATRIX_REVIEW" name="会签" flowable:assignee="7"/>
                        <sequenceFlow id="toEnd" sourceRef="MATRIX_REVIEW" targetRef="end"/><endEvent id="end"/>
                      </process>
                    </definitions>
                    """;
            engine.getRepositoryService().createDeployment().tenantId("1").addString("signed-control.bpmn20.xml",model).deploy();
            round=engine.getRuntimeService().startProcessInstanceByKeyAndTenantId("dcc-d-signed-control-combination","2",Map.of("controlledFileId",2L),"1").getId();
            var task=engine.getTaskService().createTaskQuery().processInstanceId(round).singleResult();
            engine.getTaskService().setVariableLocal(task.getId(),BpmnVariableConstants.TASK_VARIABLE_DCC_OBLIGATION_ID,"signed-control-obligation");
            jdbc.update("UPDATE dcc_controlled_file SET status='PENDING_MATRIX_REVIEW',process_instance_id=? WHERE id=2",round);
            jdbc.update("INSERT INTO dcc_controlled_file_task_assignee_snapshot(controlled_file_id,stage_code,department_id,assignee_user_id,leader_user_id,process_instance_id,bpm_task_id,obligation_id,tenant_id) VALUES(2,'MATRIX_REVIEW',10,7,7,?,?,'signed-control-obligation',1)",round,task.getId());
            when(users.getUser(9L)).thenReturn(new AdminUserRespDTO().setId(9L).setStatus(0).setDeptId(10L).setNickname("本轮会签人").setPostIds(Set.of(10L)));
            when(tasks.createTaskQuery()).thenAnswer(call->engine.getTaskService().createTaskQuery());
            var bpm=new BpmTaskServiceImpl();wire(bpm,"taskService",engine.getTaskService());
            var signing=mock(DccSignatureVerificationService.class);
            // The authentication/HMAC kernel remains an explicit test port; its actual signature projection joins this transaction.
            when(signing.verifyPasswordAndCreateWorkflowSignature(any(),any(),any(),any(),any(),eq("ASSIGN"),any(),any())).thenAnswer(call->{
                var record=DccControlledFileSignatureDO.builder().controlledFileId(2L).revisionId(2L).versionNo("B/1")
                        .processInstanceId(call.getArgument(3)).taskId(task.getId()).actorId(7L).actionType("ASSIGN").meaningCode("MATRIX_REVIEW_ASSIGN")
                        .signatureMode("PASSWORD").passwordVerified(true).signedAt(dates.now()).comment(call.getArgument(7)).evidencePayloadVersion("v4-workflow").evidenceStatus("VALID").build();
                assertEquals(1,signatures.insert(record));
                return DccUnifiedSignatureResult.builder().signatureId(record.getId()).evidenceStatus("VALID").build();
            });
            var assignment=new DccWorkflowSignoffAssignmentService();
            wire(assignment,"fileMapper",files,"masterMapper",masters,"snapshotMapper",obligations,"bpmTaskService",bpm,
                    "taskService",engine.getTaskService(),"runtimeService",engine.getRuntimeService(),"adminUserApi",users,
                    "readinessService",mock(DccControlledFileRouteReadinessService.class),"signatureService",signing,"remediationService",remediation);
            var dueAt=LocalDate.now().plusDays(5).atTime(12,0);var input=new DccSignoffAssignmentReqVO();
            input.setTaskId(task.getId());input.setAssigneeUserId(9L);input.setPassword("isolated-kernel-port");input.setReason("本轮明确整改安排");
            input.setRelationArrangements(selected?List.of(new Arrangement(20L,8L,dueAt)):List.of());
            if(auditFailure){
                jdbc.update("DELETE FROM gxp_audit_policy_operation WHERE operation_id='dcc.relation.arrange'");
                var error=assertThrows(ServiceException.class,()->new TransactionTemplate(manager).executeWithoutResult(status->assignment.assign(7L,2L,input)));
                assertEquals(cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.GXP_AUDIT_POLICY_NOT_FOUND.getCode(),error.getCode());
                assertEquals("7",engine.getTaskService().createTaskQuery().taskId(task.getId()).singleResult().getAssignee());
                assertNull(engine.getTaskService().getVariableLocal(task.getId(),"dccAssignmentSignatureId"));
                assertNull(obligations.selectListByControlledFileId(2L).get(0).getAssignmentSignatureId());
                assertEquals(0,count("dcc_controlled_file_signature"));assertEquals(0,count("dcc_relation_arrangement"));
                assertEquals(initialAudit,count("gxp_audit_event"));assertEquals(0,count("dcc_relation_controlled_event"));assertEquals(0,count("system_notify_message"));assertTrue(queue.tasks.isEmpty());return;
            }
            new TransactionTemplate(manager).executeWithoutResult(status->assignment.assign(7L,2L,input));
            assertEquals("9",engine.getTaskService().createTaskQuery().taskId(task.getId()).singleResult().getAssignee());
            var saved=obligations.selectListByControlledFileId(2L).get(0);assertNotNull(saved.getAssignmentSignatureId());
            assertEquals(saved.getAssignmentSignatureId(),engine.getTaskService().getVariableLocal(task.getId(),"dccAssignmentSignatureId"));
            assertTrue(signatures.selectById(saved.getAssignmentSignatureId()).getComment().contains(saved.getAssignmentPayloadHash()));
            assertEquals(selected?1:0,count("dcc_relation_arrangement"));assertEquals(initialAudit+(selected?1:0),count("gxp_audit_event"));assertEquals(0,count("system_notify_message"));assertTrue(queue.tasks.isEmpty());
            assertEquals(selected?1:0,jdbc.queryForObject("SELECT COUNT(*) FROM gxp_audit_event WHERE tenant_id=1 AND operation_id='dcc.relation.arrange' AND subject_id=?",Integer.class,"ARRANGEMENT:2:"+round+":20"));
            new TransactionTemplate(manager).executeWithoutResult(status->assignment.assign(7L,2L,input));assertEquals(1,count("dcc_controlled_file_signature"));
            // Approval/finalization preconditions are explicitly supplied; this fixture does not simulate the complete A approval route.
            engine.getTaskService().complete(task.getId());jdbc.update("UPDATE dcc_controlled_file SET status='READY_TO_PUBLISH' WHERE id=2");
            control();assertEquals(1,queue.tasks.size());assertEquals(0,count("system_notify_message"));
            assertEquals(selected?1:0,count("dcc_relation_remediation_task"));assertEquals(selected?1:0,count("dcc_relation_notification_outbox"));
            if(selected){
                assertEquals(8L,jdbc.queryForObject("SELECT assignee_user_id FROM dcc_relation_remediation_task",Long.class));
                assertEquals(dueAt,jdbc.queryForObject("SELECT due_at FROM dcc_relation_remediation_task",LocalDateTime.class));
                assertEquals(saved.getProcessInstanceId(),jdbc.queryForObject("SELECT application_round FROM dcc_relation_controlled_event",String.class));
            }
            queue.drain();assertEquals(selected?1:0,count("system_notify_message"));
            if(selected)assertEquals(8L,jdbc.queryForObject("SELECT user_id FROM system_notify_message",Long.class));
            new TransactionTemplate(manager).executeWithoutResult(status->publisher.publishEvent(controlEvent()));queue.drain();
            assertEquals(selected?1:0,count("system_notify_message"));assertEquals(selected?1:0,count("dcc_relation_remediation_task"));
        }finally{
            if(round!=null && engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(round).singleResult()!=null)
                engine.getRuntimeService().deleteProcessInstance(round,"task-owned offline fixture cleanup");
            engine.close();jdbc.update("DELETE FROM dcc_controlled_file_signature WHERE controlled_file_id=2");
        }
    }
    private void wire(Object target,Object... pairs){for(int index=0;index<pairs.length;index+=2)ReflectionTestUtils.setField(target,(String)pairs[index],pairs[index+1]);}
}
