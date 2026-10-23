package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionServiceImpl;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskServiceImpl;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTaskAssigneeSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.service.file.relations.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import jakarta.annotation.Resource;
import org.flowable.engine.ProcessEngine;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Actual persisted NULLround and real Flowable current task; no signed-write privilege is added. */
@Import(DccRelationAccessPolicyImpl.class)
class DccInitialRevisionArrangementReadTest extends BaseDbUnitTest {
    @Resource DataSource dataSource;
    @Resource PlatformTransactionManager transactionManager;
    @Resource(name="dccControlledFileMapper") DccControlledFileMapper fileMapper;
    @Resource DccControlledFileMasterMapper masterMapper;
    @Resource DccControlledFileTaskAssigneeSnapshotMapper obligationMapper;
    @Resource DccRelationAccessPolicyImpl policy;
    @MockBean DccControlledFileQueryService query;
    @MockBean AdminUserApi users;
    @MockBean DccProjectAccessService projects;
    @MockBean BpmTaskService bpmTasks;
    @MockBean BpmProcessDefinitionService definitions;
    @MockBean org.flowable.engine.TaskService tasks;
    JdbcTemplate jdbc;
    ProcessEngine engine;
    String round, taskId;
    Long obligationId;
    DccRelationRemediationService remediation;
    final java.util.ArrayList<String> ownedProcessIds = new java.util.ArrayList<>();
    final String obligation = "42:MATRIX_REVIEW:51";

    @BeforeEach void fixture() throws Exception {
        TenantContextHolder.setTenantId(1L);
        jdbc = new JdbcTemplate(dataSource);
        for (String sql : Files.readString(Path.of("../sql/mysql/20260930_dcc_d_relations.sql"))
                .replaceAll("(?m)^--.*$", "").split(";")) if (!sql.isBlank()) jdbc.execute(sql);
        jdbc.update("DELETE FROM dcc_relation_arrangement WHERE tenant_id=1 AND source_file_id=42");
        jdbc.update("DELETE FROM dcc_controlled_file_task_assignee_snapshot WHERE controlled_file_id=42");
        var config = new SpringProcessEngineConfiguration();
        config.setDataSource(dataSource); config.setTransactionManager(transactionManager);
        config.setDatabaseType("mysql"); config.setDatabaseSchemaUpdate("true");
        config.setAsyncExecutorActivate(false); config.setDisableIdmEngine(true);
        engine = config.buildProcessEngine();
        start("dcc-controlled-file-revision", "1");
        jdbc.update("INSERT INTO dcc_controlled_file_master(id,category_id,file_name,file_number,status,tenant_id,deleted) VALUES(10,10,'SOP.pdf','N-1','ACTIVE_CHAIN',1,0)");
        jdbc.update("""
                INSERT INTO dcc_controlled_file(id,master_id,category_id,directory_id,source_file_id,original_file_id,file_name,title,
                  file_number,version_no,status,submitter_id,requester_id,tenant_id,deleted,process_instance_id,process_definition_key)
                VALUES(42,10,10,3,100,100,'SOP.pdf','SOP','N-1','A/1','PENDING_MATRIX_REVIEW',99,99,1,0,?,'dcc-controlled-file-upload')
                """, round); // The current task definition, rather than an old file key, is authoritative.
        var row = DccControlledFileTaskAssigneeSnapshotDO.builder().controlledFileId(42L).tenantId(1L)
                .stageCode("MATRIX_REVIEW").stageNo(1).departmentId(51L).leaderUserId(99L).assigneeUserId(99L)
                .obligationId(obligation).build();
        assertEquals(1, obligationMapper.insert(row)); obligationId = row.getId();
        var actualBpm = new BpmTaskServiceImpl(); ReflectionTestUtils.setField(actualBpm, "taskService", engine.getTaskService());
        when(bpmTasks.validateTask(anyLong(), anyString())).thenAnswer(call -> actualBpm.validateTask(call.getArgument(0), call.getArgument(1)));
        var actualDefinitions = new BpmProcessDefinitionServiceImpl();
        ReflectionTestUtils.setField(actualDefinitions, "repositoryService", engine.getRepositoryService());
        when(definitions.getProcessDefinition(anyString())).thenAnswer(call -> actualDefinitions.getProcessDefinition(call.getArgument(0)));
        when(tasks.createTaskQuery()).thenAnswer(call -> engine.getTaskService().createTaskQuery());
        when(users.getUser(99L)).thenReturn(new AdminUserRespDTO().setId(99L).setTenantId(1L).setStatus(0));
        var resolver = new DccLatestControlledFileResolverImpl();
        ReflectionTestUtils.setField(resolver, "fileMapper", fileMapper);
        ReflectionTestUtils.setField(resolver, "masterMapper", masterMapper);
        remediation = new DccRelationRemediationService(new DccRelationStore(jdbc, mock(GxpAuditService.class)),
                resolver, policy, mock(DccRelationNotificationPostCommitScheduler.class));
    }

    void start(String key, String tenant) {
        String model = """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="G56-read">
                  <process id="%s" isExecutable="true">
                    <startEvent id="start"/><sequenceFlow id="toReview" sourceRef="start" targetRef="MATRIX_REVIEW"/>
                    <userTask id="MATRIX_REVIEW" flowable:assignee="99"/>
                    <sequenceFlow id="toEnd" sourceRef="MATRIX_REVIEW" targetRef="end"/><endEvent id="end"/>
                  </process>
                </definitions>
                """.formatted(key);
        engine.getRepositoryService().createDeployment().tenantId(tenant).addString("g56.bpmn20.xml", model).deploy();
        round = engine.getRuntimeService().startProcessInstanceByKeyAndTenantId(key, "42", Map.of("controlledFileId", 42L), tenant).getId();
        ownedProcessIds.add(round);
        taskId = engine.getTaskService().createTaskQuery().processInstanceId(round).singleResult().getId();
        engine.getTaskService().setVariableLocal(taskId, BpmnVariableConstants.TASK_VARIABLE_DCC_OBLIGATION_ID, obligation);
    }

    @AfterEach void cleanup() {
        try {
            if (engine != null) {
                for (String id : ownedProcessIds)
                    if (engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(id).singleResult() != null)
                        engine.getRuntimeService().deleteProcessInstance(id, "G56 fixture cleanup");
                engine.close();
            }
        } finally {
            if (jdbc != null) {
                jdbc.update("DELETE FROM dcc_relation_arrangement WHERE tenant_id=1 AND source_file_id=42");
                jdbc.update("DELETE FROM dcc_controlled_file_task_assignee_snapshot WHERE controlled_file_id=42");
            }
            TenantContextHolder.clear();
        }
    }

    @Test void currentRevisionLeaderCanReadInitialArrangementsWithoutWritingTheUnsignedRound() {
        var beforeFile = jdbc.queryForMap("SELECT * FROM dcc_controlled_file WHERE id=42");
        var beforeObligation = jdbc.queryForMap("SELECT * FROM dcc_controlled_file_task_assignee_snapshot WHERE id=?", obligationId);
        var tx = new TransactionTemplate(transactionManager); tx.setReadOnly(true);
        assertEquals(List.of(), tx.execute(status -> remediation.listArrangements(99L, 42L, round)));
        assertEquals(beforeFile, jdbc.queryForMap("SELECT * FROM dcc_controlled_file WHERE id=42"));
        assertEquals(beforeObligation, jdbc.queryForMap("SELECT * FROM dcc_controlled_file_task_assignee_snapshot WHERE id=?", obligationId));
        assertNull(obligationMapper.selectById(obligationId).getProcessInstanceId());
        assertEquals("99", engine.getTaskService().createTaskQuery().taskId(taskId).singleResult().getAssignee());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_relation_arrangement WHERE source_file_id=42", Integer.class));
        verify(bpmTasks).validateTask(99L, taskId);
        verify(query).assertRelationNameVisible(99L, 42L);
    }

    @Test void initialReadDoesNotGrantTheUnsignedArrangementWritePrivilege() {
        assertDoesNotThrow(() -> policy.assertCanReadArrangements(99L, 42L, round));
        var error = assertThrows(DccRelationFailure.class, () -> policy.assertCanArrange(99L, 42L, round));
        assertEquals("DCC_RELATION_ARRANGEMENT_FORBIDDEN", error.getMessage());
        assertNull(obligationMapper.selectById(obligationId).getProcessInstanceId());
    }

    @Test void alreadyBoundHistoricalParticipantStillReadsWithoutAnActiveTask() {
        jdbc.update("UPDATE dcc_controlled_file_task_assignee_snapshot SET process_instance_id=? WHERE id=?", round, obligationId);
        engine.getTaskService().complete(taskId);
        assertDoesNotThrow(() -> policy.assertCanReadArrangements(99L, 42L, round));
        verifyNoInteractions(bpmTasks, definitions);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACTOR", "ROUND", "OBLIGATION", "UPLOAD", "TENANT", "ENDED", "STATUS", "DISABLED"})
    void incompleteOrForeignCurrentTaskCannotUseANullRoundAsReadAuthority(String change) {
        Long actor = 99L; String requestedRound = round;
        switch (change) {
            case "ACTOR" -> actor = 100L;
            case "ROUND" -> requestedRound = "another-real-round";
            case "OBLIGATION" -> engine.getTaskService().setVariableLocal(taskId, BpmnVariableConstants.TASK_VARIABLE_DCC_OBLIGATION_ID, "another-file:51");
            case "UPLOAD" -> { start("dcc-controlled-file-upload", "1"); requestedRound = round; jdbc.update("UPDATE dcc_controlled_file SET process_instance_id=? WHERE id=42", round); }
            case "TENANT" -> { start("dcc-controlled-file-revision", "2"); requestedRound = round; jdbc.update("UPDATE dcc_controlled_file SET process_instance_id=? WHERE id=42", round); }
            case "ENDED" -> engine.getTaskService().complete(taskId);
            case "STATUS" -> jdbc.update("UPDATE dcc_controlled_file SET status='REJECTED' WHERE id=42");
            case "DISABLED" -> when(users.getUser(99L)).thenReturn(new AdminUserRespDTO().setId(99L).setTenantId(1L).setStatus(1));
            default -> throw new AssertionError(change);
        }
        Long requestedActor = actor; String capturedRound = requestedRound;
        assertThrows(DccRelationFailure.class, () -> policy.assertCanReadArrangements(requestedActor, 42L, capturedRound));
        assertNull(obligationMapper.selectById(obligationId).getProcessInstanceId());
    }
}
