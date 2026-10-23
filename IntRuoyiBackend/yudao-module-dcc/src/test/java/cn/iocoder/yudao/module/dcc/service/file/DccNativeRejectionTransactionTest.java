package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskRejectReqVO;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.event.BpmProcessInstanceEventPublisher;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.listener.BpmProcessInstanceEventListener;
import cn.iocoder.yudao.module.bpm.service.definition.BpmModelService;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.bpm.service.message.BpmMessageService;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceServiceImpl;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskServiceImpl;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.system.dal.mysql.controlledcontent.ControlledContentTransitionAuditMapper;
import cn.iocoder.yudao.module.system.dal.mysql.controlledcontent.ControlledContentVersionRefMapper;
import cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentType;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentKey;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentLifecycleCoreService;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentStateMachine;
import jakarta.annotation.Resource;
import org.flowable.engine.ProcessEngine;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Actual BPM rejection/completion + real DCC/platform SQL in one isolated Spring transaction.
 * Authentication/HMAC remains an explicit port; this is not a real electronic-signature E2E. */
class DccNativeRejectionTransactionTest extends BaseDbUnitTest {
    @Resource DataSource source;
    @Resource PlatformTransactionManager manager;
    @Resource DccControlledFileMapper files;
    @Resource ControlledContentVersionRefMapper refs;
    @Resource ControlledContentTransitionAuditMapper audits;
    private JdbcTemplate jdbc;
    private ProcessEngine engine;
    private BpmTaskServiceImpl tasks;
    private DccControlledFileFinalizationServiceImpl finalization;
    private DccControlledContentAdapter adapter;
    private ControlledContentLifecycleCoreService core;
    private ControlledContentKey key;
    private String round;
    private Task task;
    private final AtomicInteger callbacks = new AtomicInteger();

    @BeforeEach
    void fixture() {
        TenantContextHolder.setTenantId(1L);
        jdbc = new JdbcTemplate(source);
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS controlled_content_version_ref(
            id BIGINT AUTO_INCREMENT PRIMARY KEY,tenant_id BIGINT,content_type VARCHAR(64),content_key VARCHAR(128),
            native_master_id BIGINT,native_version_id BIGINT,version_no VARCHAR(64),canonical_status VARCHAR(64),domain_status VARCHAR(128),
            source_version_ref_id BIGINT,source_native_version_id BIGINT,successor_version_ref_id BIGINT,successor_native_version_id BIGINT,
            active_unique_flag INT,open_candidate_unique_flag INT,approval_process_instance_id VARCHAR(128),last_transition_time TIMESTAMP,
            creator VARCHAR(64),updater VARCHAR(64),create_time TIMESTAMP,update_time TIMESTAMP,deleted BIT DEFAULT 0,
            UNIQUE(tenant_id,content_type,content_key,active_unique_flag),UNIQUE(tenant_id,content_type,content_key,open_candidate_unique_flag))
            """);
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS controlled_content_transition_audit(
            id BIGINT AUTO_INCREMENT PRIMARY KEY,tenant_id BIGINT,version_ref_id BIGINT,content_type VARCHAR(64),content_key VARCHAR(128),
            from_status VARCHAR(64),to_status VARCHAR(64),domain_from_status VARCHAR(128),domain_to_status VARCHAR(128),action VARCHAR(64),
            event_key VARCHAR(128),actor_id BIGINT,reason VARCHAR(1024),create_time TIMESTAMP)
            """);
        jdbc.update("DELETE FROM controlled_content_transition_audit");
        jdbc.update("DELETE FROM controlled_content_version_ref");
        core = new ControlledContentLifecycleCoreService(refs, audits, new ControlledContentStateMachine());
        key = ControlledContentKey.of(1L, ControlledContentType.DCC_CONTROLLED_FILE, "10");
        adapter = new DccControlledContentAdapter();
        ReflectionTestUtils.setField(adapter, "lifecycleCoreService", core);
        finalization = new DccControlledFileFinalizationServiceImpl();
        ReflectionTestUtils.setField(finalization, "controlledFileMapper", files);
        ReflectionTestUtils.setField(finalization, "transactionTemplate", new TransactionTemplate(manager));
        ReflectionTestUtils.setField(finalization, "platformAdapter", adapter);

        var processService = new BpmProcessInstanceServiceImpl();
        var listener = new BpmProcessInstanceEventListener();
        ReflectionTestUtils.setField(listener, "processInstanceService", processService);
        ReflectionTestUtils.setField(processService, "messageService", mock(BpmMessageService.class));
        ReflectionTestUtils.setField(processService, "processDefinitionService", mock(BpmProcessDefinitionService.class));
        ReflectionTestUtils.setField(processService, "processInstanceEventPublisher", new BpmProcessInstanceEventPublisher(event -> {
            callbacks.incrementAndGet();
            finalization.handleProcessInstanceStatusChanged((BpmProcessInstanceStatusEvent) event);
        }));
        var configuration = new SpringProcessEngineConfiguration();
        configuration.setDataSource(source);
        configuration.setTransactionManager(manager);
        configuration.setDatabaseType("mysql");
        configuration.setDatabaseSchemaUpdate("true");
        configuration.setAsyncExecutorActivate(false);
        configuration.setDisableIdmEngine(true);
        configuration.setEventListeners(List.of(listener));
        engine = configuration.buildProcessEngine();
        ReflectionTestUtils.setField(processService, "runtimeService", engine.getRuntimeService());
        ReflectionTestUtils.setField(processService, "historyService", engine.getHistoryService());
        tasks = new BpmTaskServiceImpl();
        ReflectionTestUtils.setField(tasks, "taskService", engine.getTaskService());
        ReflectionTestUtils.setField(tasks, "runtimeService", engine.getRuntimeService());
        ReflectionTestUtils.setField(tasks, "processInstanceService", processService);
        var models = mock(BpmModelService.class);
        when(models.getBpmnModelByDefinitionId(anyString())).thenAnswer(call ->
                engine.getRepositoryService().getBpmnModel(call.getArgument(0)));
        ReflectionTestUtils.setField(tasks, "modelService", models);
    }

    private void start(String suffix, String stage) {
        String definition = "dcc-controlled-file-" + suffix;
        String model = """
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="G64-real-reject">
            <process id="%s" isExecutable="true"><startEvent id="start"/>
            <sequenceFlow id="toTask" sourceRef="start" targetRef="%s"/>
            <userTask id="%s" name="审批" flowable:assignee="99"/>
            <sequenceFlow id="toEnd" sourceRef="%s" targetRef="end"/><endEvent id="end"/>
            </process></definitions>
            """.formatted(definition, stage, stage, stage);
        engine.getRepositoryService().createDeployment().tenantId("1").addString("g64.bpmn20.xml", model).deploy();
        engine.getIdentityService().setAuthenticatedUserId("99");
        round = engine.getRuntimeService().startProcessInstanceByKeyAndTenantId(definition, "42", Map.of(
                "PROCESS_STATUS", 1, "PROCESS_LAST_APPROVER_USER_ID", 99L), "1").getId();
        task = engine.getTaskService().createTaskQuery().processInstanceId(round).singleResult();
        String status = "PENDING_" + stage;
        var file = DccControlledFileDO.builder().id(42L).masterId(10L).tenantId(1L).categoryId(1L).directoryId(1L)
                .sourceFileId(100L).originalFileId(100L).fileName("g64.pdf").title("G64").fileNumber("G64")
                .versionNo("A/2").status(status).processInstanceId(round).processDefinitionKey(definition)
                .requesterId(99L).submitterId(99L).build();
        files.insert(file);
        new TransactionTemplate(manager).executeWithoutResult(s -> adapter.recordSubmitted(file, 99L, round));
        engine.getTaskService().setVariablesLocal(task.getId(), Map.of("TASK_STATUS", 1,
                "dccVerifiedSignatureId", 1001L, "dccVerifiedSignatureActorId", 99L, "dccVerifiedSignatureAction", "REJECT"));
    }

    private void reject() {
        tasks.rejectTask(99L, new BpmTaskRejectReqVO().setId(task.getId()).setReason("本轮真实回调拒绝"));
    }

    @ParameterizedTest
    @CsvSource({"upload,MATRIX_REVIEW", "revision,MATRIX_REVIEW", "revision,MATRIX_APPROVAL",
            "revision,DOC_CONTROL_REVIEW", "approval,DOC_CONTROL_APPROVAL"})
    void actualFlowableRejectUsesTheExactNativeOrLegacyDefinitionKey(String suffix, String stage) {
        start(suffix, stage);
        new TransactionTemplate(manager).executeWithoutResult(s -> reject());
        var file = files.selectById(42L);
        assertEquals("REJECTED", file.getStatus());
        assertEquals(round, file.getProcessInstanceId());
        assertTrue(file.getRejectReason().contains("本轮真实回调拒绝"));
        assertNull(file.getControlledTime());
        assertEquals("REJECTED", core.getVersionRef(key, 42L).getCanonicalStatus());
        assertNull(core.getVersionRef(key, 42L).getOpenCandidateUniqueFlag());
        assertNull(engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(round).singleResult());
        assertEquals(3, engine.getHistoryService().createHistoricTaskInstanceQuery().taskId(task.getId())
                .includeTaskLocalVariables().singleResult().getTaskLocalVariables().get("TASK_STATUS"));
        assertEquals(1, callbacks.get());
    }

    @Test
    void enclosingFailureRollsBackActualBpmDccSharedAndSignatureWrites() {
        start("revision", "MATRIX_REVIEW");
        var error = assertThrows(IllegalStateException.class, () -> new TransactionTemplate(manager).executeWithoutResult(s -> {
            jdbc.update("INSERT INTO dcc_controlled_file_signature(controlled_file_id,task_id,actor_id,action_type,signature_mode,comment,process_instance_id,version_no,tenant_id) VALUES(42,?,99,'REJECT','ISOLATED_GUARD_PORT','本轮隔离证据',?,'A/2',1)", task.getId(), round);
            reject();
            assertEquals("REJECTED", files.selectById(42L).getStatus());
            throw new IllegalStateException("ACTUAL_ENCLOSING_ACTION_FAILED");
        }));
        assertEquals("ACTUAL_ENCLOSING_ACTION_FAILED", error.getMessage());
        assertEquals("PENDING_MATRIX_REVIEW", files.selectById(42L).getStatus());
        assertEquals("IN_REVIEW", core.getVersionRef(key, 42L).getCanonicalStatus());
        assertEquals(1, core.getVersionRef(key, 42L).getOpenCandidateUniqueFlag());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_signature WHERE controlled_file_id=42", Integer.class));
        assertNotNull(engine.getTaskService().createTaskQuery().taskId(task.getId()).singleResult());
        assertEquals(1, engine.getRuntimeService().getVariable(round, "PROCESS_STATUS"));
    }

    @Test
    void mismatchedDefinitionEventCannotUpdateTheActualFileOrProjection() {
        start("revision", "MATRIX_REVIEW");
        var event = new BpmProcessInstanceStatusEvent(this);
        event.setId(round);
        event.setBusinessKey("42");
        event.setProcessDefinitionKey(DccControlledFileProcessDefinitionKeys.UPLOAD);
        event.setActorUserId(99L);
        event.setStatus(3);
        event.setReason("外来身份");
        assertThrows(IllegalStateException.class, () -> finalization.handleProcessInstanceStatusChanged(event));
        assertEquals("PENDING_MATRIX_REVIEW", files.selectById(42L).getStatus());
        assertEquals("IN_REVIEW", core.getVersionRef(key, 42L).getCanonicalStatus());
    }

    @AfterEach
    void cleanup() {
        if (engine != null) engine.close();
        if (jdbc != null) {
            jdbc.update("DELETE FROM controlled_content_transition_audit");
            jdbc.update("DELETE FROM controlled_content_version_ref");
        }
        TenantContextHolder.clear();
    }
}
