package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.bpm.approval.service.ownership.*;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.dal.dataobject.businessapproval.BusinessApprovalRequestDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.businessapproval.BusinessApprovalRequestMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskServiceImpl;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesEdhrEngineOwnershipMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import jakarta.annotation.Resource;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real Flowable and MES mapper operations share one H2 resource and Spring transaction. */
class MesEdhrDomainAssignmentFlowableTest extends BaseDbUnitTest {
    @Resource DataSource dataSource;
    @Resource MesEdhrEngineOwnershipMapper ownershipMapper;
    @Resource MesProBatchRecordExecutionMapper executionMapper;

    @ParameterizedTest @ValueSource(longs = {99L, 100L})
    void everyFrozenCandidateCanReadThenAssignAndRealValidate(Long actor) {
        try (Harness h = new Harness(dataSource, ownershipMapper, executionMapper)) {
            h.insertDomainFacts("APPROVE", "99,100");
            try (var security = mockStatic(SecurityFrameworkUtils.class)) {
                security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(actor);
                assertEquals("88", h.assignment.read(h.taskId, 401L).getAssignee());
                assertEquals("88", h.engine.getTaskService().createTaskQuery().taskId(h.taskId).singleResult().getAssignee());
                assertEquals(0, h.engine.getTaskService().getTaskComments(h.taskId).size());
                assertEquals(0, h.engine.getTaskService().getTaskComments(h.taskId, "EDHR_DOMAIN_ASSIGNMENT").size());
                h.tx.executeWithoutResult(status -> assertEquals(actor.toString(), h.assignment.assign(h.taskId, 401L).getAssignee()));
                assertEquals(actor.toString(), h.bpm.validateTask(actor, h.taskId).getAssignee());
                assertThrows(RuntimeException.class, () -> h.bpm.validateTask(88L, h.taskId));
                assertEquals("88", h.engine.getTaskService().createTaskQuery().taskId(h.taskId).singleResult().getOwner());
                var comments = h.engine.getTaskService().getTaskComments(h.taskId, "EDHR_DOMAIN_ASSIGNMENT");
                assertEquals(1, comments.size());
                assertTrue(comments.get(0).getFullMessage().contains("frozenActors=[99, 100]"));
                assertTrue(comments.get(0).getFullMessage().contains(
                        "candidateDigest=" + cn.hutool.crypto.digest.DigestUtil.sha256Hex("EDHR_FROZEN_ACTORS_V1:99,100")));
                assertThrows(RuntimeException.class, () -> h.policy.assertGenericMutationAllowed(h.bpm.getTask(h.taskId), actor));
            }
        }
    }

    @Test void signatureFailureRollsBackEngineAssignmentAuditAndDomainWriteOnSameResource() {
        try (Harness h = new Harness(dataSource, ownershipMapper, executionMapper)) {
            h.insertDomainFacts("APPROVE", "99,100");
            try (var security = mockStatic(SecurityFrameworkUtils.class)) {
                security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(100L);
                assertThrows(IllegalStateException.class, () -> h.tx.executeWithoutResult(status -> {
                    h.assignment.assign(h.taskId, 401L);
                    h.jdbc.update("UPDATE mes_pro_edhr_work_task SET status='DONE' WHERE id=401");
                    throw new IllegalStateException("SIGNATURE_PASSWORD_REJECTED");
                }));
                assertEquals("88", h.bpm.getTask(h.taskId).getAssignee());
                assertNull(h.bpm.getTask(h.taskId).getOwner());
                assertEquals(0, h.engine.getTaskService().getTaskComments(h.taskId).size());
                assertEquals(0, h.engine.getTaskService().getTaskComments(h.taskId, "EDHR_DOMAIN_ASSIGNMENT").size());
                assertEquals("TODO", h.jdbc.queryForObject("SELECT status FROM mes_pro_edhr_work_task WHERE id=401", String.class));
                h.tx.executeWithoutResult(status -> h.assignment.assign(h.taskId, 401L));
                assertEquals("100", h.bpm.validateTask(100L, h.taskId).getAssignee());
            }
        }
    }

    @Test void multipleReviewRowsShareActivityButEachActionRequiresItsOwnFrozenActor() {
        try (Harness h = new Harness(dataSource, ownershipMapper, executionMapper)) {
            h.insertDomainFacts("REVIEW", "99,100");
            h.jdbc.update("INSERT INTO mes_pro_edhr_work_task(id,task_code,task_type,status,execution_id,bpm_task_id,assignee_user_id,candidate_user_snapshot,action_url,tenant_id,deleted) VALUES(402,'review-two','REVIEW','TODO',301,?,100,'100','/detail',1,0)", h.taskId);
            try (var security = mockStatic(SecurityFrameworkUtils.class)) {
                security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
                assertThrows(RuntimeException.class, () -> h.tx.executeWithoutResult(status -> h.assignment.assign(h.taskId, 402L)));
                h.tx.executeWithoutResult(status -> {
                    h.assignment.assign(h.taskId, 401L);
                    h.jdbc.update("UPDATE mes_pro_edhr_work_task SET status='DONE' WHERE id=401");
                });
                security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(100L);
                h.tx.executeWithoutResult(status -> h.assignment.assign(h.taskId, 402L));
                assertEquals("100", h.bpm.validateTask(100L, h.taskId).getAssignee());
                assertNotNull(h.engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(h.process.getId()).singleResult());
                assertThrows(RuntimeException.class, () -> h.tx.executeWithoutResult(status -> h.assignment.assign(h.taskId, 401L)));
            }
        }
    }

    @Test void actualAssignmentListenerUsesFreshActorAndNeverRunsAutomaticDomainStrategies() {
        try (Harness h = new Harness(dataSource, ownershipMapper, executionMapper)) {
            h.insertDomainFacts("APPROVE", "99,100");
            var messages = h.attachAssignmentListener();
            try (var security = mockStatic(SecurityFrameworkUtils.class)) {
                security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
                h.tx.executeWithoutResult(status -> {
                    h.assignment.assign(h.taskId, 401L);
                    security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(100L);
                    h.assignment.assign(h.taskId, 401L);
                });
                assertEquals("100", h.bpm.getTask(h.taskId).getAssignee());
                assertNotNull(h.engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(h.process.getId()).singleResult());
                // Two real TASK_ASSIGNED events each retain their notification. Both reread the final actor.
                verify(messages, times(2)).sendMessageWhenTaskAssigned(argThat(message -> Long.valueOf(100L).equals(message.getAssigneeUserId())));
                verify(h.definitions, never()).getProcessDefinitionInfo(any());
                clearInvocations(messages);
                assertThrows(IllegalStateException.class, () -> h.tx.executeWithoutResult(status -> {
                    security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
                    h.assignment.assign(h.taskId, 401L);
                    throw new IllegalStateException("SIGNATURE_REJECTED");
                }));
                verifyNoInteractions(messages);
                security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
                h.tx.executeWithoutResult(status -> {
                    h.assignment.assign(h.taskId, 401L);
                    h.engine.getTaskService().complete(h.taskId);
                });
                assertNull(h.bpm.getTask(h.taskId));
                verifyNoInteractions(messages);
            }
        }
    }

    @ParameterizedTest @ValueSource(strings = {"actor", "task", "phase", "terminal", "snapshot", "request"})
    void staleOrForeignFactsRejectBeforeEngineAssignment(String mode) {
        try (Harness h = new Harness(dataSource, ownershipMapper, executionMapper)) {
            h.insertDomainFacts("APPROVE", "99,100");
            if (mode.equals("phase")) h.jdbc.update("INSERT INTO mes_pro_edhr_work_task(id,task_code,task_type,status,execution_id,bpm_task_id,assignee_user_id,action_url,tenant_id,deleted) VALUES(402,'review','REVIEW','TODO',301,?,99,'/detail',1,0)", h.taskId);
            if (mode.equals("terminal")) h.jdbc.update("UPDATE mes_pro_edhr_work_task SET status='DONE' WHERE id=401");
            if (mode.equals("snapshot")) h.jdbc.update("UPDATE mes_pro_batch_record_approval_snapshot SET current_bpm_task_id='stale' WHERE id=20");
            if (mode.equals("request")) h.request.setProcessInstanceId("foreign");
            try (var security = mockStatic(SecurityFrameworkUtils.class)) {
                security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(mode.equals("actor") ? 101L : 99L);
                assertThrows(RuntimeException.class, () -> h.tx.executeWithoutResult(status -> h.assignment.assign(h.taskId, mode.equals("task") ? 999L : 401L)));
                assertEquals("88", h.bpm.getTask(h.taskId).getAssignee());
                assertEquals(0, h.engine.getTaskService().getTaskComments(h.taskId).size());
                assertEquals(0, h.engine.getTaskService().getTaskComments(h.taskId, "EDHR_DOMAIN_ASSIGNMENT").size());
            }
        }
    }

    @Test void genericAssignmentRetainsExistingAutomaticPolicyPath() {
        try (Harness h = new Harness(dataSource, ownershipMapper, executionMapper)) {
            h.insertDomainFacts("APPROVE", "99,100");
            h.attachAssignmentListener();
            var genericPolicy = mock(NativeApprovalOwnershipPolicy.class);
            when(genericPolicy.inspect(any(), any())).thenReturn(NativeApprovalOwnership.generic());
            ReflectionTestUtils.setField(h.bpm, "nativeApprovalOwnershipPolicy", genericPolicy);
            h.tx.executeWithoutResult(status -> h.engine.getTaskService().setAssignee(h.taskId, "99"));
            verify(h.definitions).getProcessDefinitionInfo(h.process.getProcessDefinitionId());
            assertNotNull(h.bpm.getTask(h.taskId));
        }
    }

    @Test void unclaimedDomainDoesNotBecomeGenericAutomaticApproval() {
        try (Harness h = new Harness(dataSource, ownershipMapper, executionMapper)) {
            h.insertDomainFacts("APPROVE", "99,100");
            var messages = h.attachAssignmentListener();
            var unclaimed = mock(NativeApprovalOwnershipPolicy.class);
            when(unclaimed.inspect(any(), any())).thenReturn(new NativeApprovalOwnership(
                    NativeApprovalOwnership.Owner.UNCLAIMED_EDHR, null, Map.of(), "FORMAL_REQUEST_MISSING"));
            ReflectionTestUtils.setField(h.bpm, "nativeApprovalOwnershipPolicy", unclaimed);
            // Invoke the real registered callback explicitly so Spring's afterCompletion logging
            // cannot turn a thrown rejection into a misleading test success.
            org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
            try {
                h.bpm.processTaskAssigned(h.bpm.getTask(h.taskId));
                var callbacks = org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations();
                assertThrows(RuntimeException.class, () -> callbacks.get(0).afterCompletion(
                        org.springframework.transaction.support.TransactionSynchronization.STATUS_COMMITTED));
                verify(h.definitions, never()).getProcessDefinitionInfo(any());
                verifyNoInteractions(messages);
            } finally {
                org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
            }
        }
    }

    static final class Harness implements AutoCloseable {
        final ProcessEngine engine;
        final BpmTaskServiceImpl bpm = new BpmTaskServiceImpl();
        final JdbcTemplate jdbc;
        final TransactionTemplate tx;
        final ProcessInstance process;
        final String taskId;
        final BusinessApprovalRequestDO request;
        final NativeApprovalOwnershipPolicy policy;
        final EdhrDomainTaskAssignmentService assignment;
        final BpmProcessDefinitionService definitions;
        final String deploymentId;
        final MesEdhrFlowableTestSchema owned;
        Harness(DataSource ds, MesEdhrEngineOwnershipMapper mapper, MesProBatchRecordExecutionMapper executions) {
            jdbc = new JdbcTemplate(ds);
            var tm = new DataSourceTransactionManager(ds); tx = new TransactionTemplate(tm);
            owned = MesEdhrFlowableTestSchema.open(ds, tm);
            try {
                engine = owned.engine();
                var deployment = owned.deploy("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="edhr">
                          <process id="mes-edhr-approval-v1" isExecutable="true">
                            <startEvent id="start"/><sequenceFlow id="s1" sourceRef="start" targetRef="approveNode"/>
                            <userTask id="approveNode" name="审批" flowable:assignee="88"/>
                            <sequenceFlow id="s2" sourceRef="approveNode" targetRef="end"/><endEvent id="end"/>
                          </process>
                        </definitions>
                        """);
                deploymentId = deployment.getId();
                var definition = engine.getRepositoryService().createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
                process = owned.start(definition.getId());
                taskId = engine.getTaskService().createTaskQuery().processInstanceId(process.getId()).singleResult().getId();
                ReflectionTestUtils.setField(bpm, "taskService", engine.getTaskService());
                ReflectionTestUtils.setField(bpm, "runtimeService", engine.getRuntimeService());
                definitions = mock(BpmProcessDefinitionService.class);
                when(definitions.getProcessDefinition(definition.getId())).thenReturn(definition);
                var processes = mock(BpmProcessInstanceService.class);
                when(processes.getProcessInstance(process.getId())).thenAnswer(call -> engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(process.getId()).includeProcessVariables().singleResult());
                ReflectionTestUtils.setField(bpm, "processInstanceService", processes);
                var models = mock(cn.iocoder.yudao.module.bpm.service.definition.BpmModelService.class);
                when(models.getBpmnModelByDefinitionId(definition.getId())).thenReturn(engine.getRepositoryService().getBpmnModel(definition.getId()));
                ReflectionTestUtils.setField(bpm, "modelService", models);
                var permissions = mock(PermissionApi.class); when(permissions.hasAnyPermissions(any(), any(String[].class))).thenReturn(true);
                var reader = new MesEdhrEngineOwnershipReader(mapper, bpm, permissions);
                var ledger = mock(GxpAuditService.class);
                ReflectionTestUtils.setField(reader, "gxpAuditService", ledger);
                var context = BusinessApprovalContext.builder().tenantId(1L).dataDomain("MES").systemCode("MES")
                        .objectType("EDHR_BATCH_EXECUTION").objectId("301").objectVersion("1")
                        .actionCode("SUBMIT_REVIEW").objectState("DRAFT").applicantUserId(88L).build();
                request = BusinessApprovalRequestDO.builder().id(9401L).tenantId(1L).policyMode("BPM_REQUIRED")
                        .requestStatus("PENDING_BPM").dataDomain("MES").systemCode("MES").objectType("EDHR_BATCH_EXECUTION")
                        .objectId("301").objectVersion("1").actionCode("SUBMIT_REVIEW").objectState("DRAFT")
                        .applicantUserId(88L).processInstanceId(process.getId()).processDefinitionKey("mes-edhr-approval-v1")
                        .effectExecutorCode(MesProBatchRecordExecutionBusinessApprovalEffectExecutor.EXECUTOR_CODE)
                        .businessContextJson(JsonUtils.toJsonString(context)).build();
                var requests = mock(BusinessApprovalRequestMapper.class);
                when(requests.selectOwnershipByTenantAndProcessId(1L, process.getId())).thenReturn(List.of(request));
                when(requests.selectOwnershipByTenantAndProcessIdForUpdate(1L, process.getId())).thenReturn(List.of(request));
                var forms = mock(FormActionInstanceMapper.class);
                when(forms.selectOwnershipByTenantAndProcessId(1L, process.getId())).thenReturn(List.of());
                // Capture-only ownership repository boundary; real service generates and verifies provenance.
                // MES and Flowable below continue to use the actual shared database transaction.
                var ownershipRows = mock(cn.iocoder.yudao.module.bpm.dal.mysql.approval.BpmApprovalProcessOwnershipMapper.class);
                var persistedOwnership = new java.util.concurrent.atomic.AtomicReference<cn.iocoder.yudao.module.bpm.dal.dataobject.approval.BpmApprovalProcessOwnershipDO>();
                when(ownershipRows.selectExact(1L, process.getId())).thenAnswer(call -> persistedOwnership.get());
                when(ownershipRows.insert(any(cn.iocoder.yudao.module.bpm.dal.dataobject.approval.BpmApprovalProcessOwnershipDO.class))).thenAnswer(call -> {
                    var row = call.getArgument(0, cn.iocoder.yudao.module.bpm.dal.dataobject.approval.BpmApprovalProcessOwnershipDO.class);
                    row.setId(9501L);
                    persistedOwnership.set(row);
                    return 1;
                });
                var formalOwnership = new ApprovalProcessOwnershipService(ownershipRows);
                tx.executeWithoutResult(status -> formalOwnership.record(process, definition,
                        new cn.iocoder.yudao.module.bpm.api.task.dto.ApprovalProcessAuthorityDTO(1L,
                                "NATIVE_EXTERNAL_EDHR", "BUSINESS_REQUEST", "9401", "1",
                                "EDHR_BUSINESS_APPROVAL_SUBMIT", process.getBusinessKey()), Map.of()));
                policy = new NativeApprovalOwnershipPolicy(requests, forms, definitions, processes, reader);
                ReflectionTestUtils.setField(policy, "formalOwnership", formalOwnership);
                assertEquals("9401", formalOwnership.requireExact(1L, process.getId(), definition.getId(),
                        definition.getKey(), process.getBusinessKey()).getAuthorityId());
                assignment = new EdhrDomainTaskAssignmentService(policy, engine.getTaskService(), bpm);
                ReflectionTestUtils.setField(bpm, "nativeApprovalOwnershipPolicy", policy);
                ReflectionTestUtils.setField(bpm, "bpmProcessDefinitionService", definitions);
                ReflectionTestUtils.setField(bpm, "historyService", engine.getHistoryService());
            } catch (RuntimeException | Error failure) {
                owned.closeAfterFailure(failure);
                throw failure;
            }
        }
        cn.iocoder.yudao.module.bpm.service.message.BpmMessageService attachAssignmentListener() {
            var messages = mock(cn.iocoder.yudao.module.bpm.service.message.BpmMessageService.class);
            var users = mock(cn.iocoder.yudao.module.system.api.user.AdminUserApi.class);
            when(users.getUser(88L)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(88L).setNickname("发起人"));
            ReflectionTestUtils.setField(bpm, "adminUserApi", users);
            ReflectionTestUtils.setField(bpm, "messageService", messages);
            var info = new cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO();
            info.setAutoApprovalType(cn.iocoder.yudao.module.bpm.enums.definition.BpmAutoApproveTypeEnum.APPROVE_ALL.getType());
            when(definitions.getProcessDefinitionInfo(any())).thenReturn(info);
            var listener = new cn.iocoder.yudao.module.bpm.framework.flowable.core.listener.BpmTaskEventListener();
            ReflectionTestUtils.setField(listener, "taskService", bpm);
            ReflectionTestUtils.setField(listener, "formCenterBpmEventBridge", mock(cn.iocoder.yudao.module.bpm.formcenter.runtime.FormCenterBpmEventBridge.class));
            engine.getRuntimeService().addEventListener(listener,
                    org.flowable.common.engine.api.delegate.event.FlowableEngineEventType.TASK_ASSIGNED);
            return messages;
        }
        void setApplicant(Long applicant) {
            request.setApplicantUserId(applicant);
            var context = JsonUtils.parseObject(request.getBusinessContextJson(), BusinessApprovalContext.class);
            request.setBusinessContextJson(JsonUtils.toJsonString(context.toBuilder().applicantUserId(applicant).build()));
        }
        void bindExecution(Long executionId) {
            request.setObjectId(executionId.toString());
            var context = JsonUtils.parseObject(request.getBusinessContextJson(), BusinessApprovalContext.class);
            request.setBusinessContextJson(JsonUtils.toJsonString(context.toBuilder().objectId(executionId.toString()).build()));
        }
        void insertDomainFacts(String phase, String candidates) {
            jdbc.update("INSERT INTO mes_pro_batch_record_execution(id,execution_code,work_order_id,work_order_code,batch_code,sheet_layout_json,status,revision_no,process_instance_id,process_definition_key,tenant_id,deleted) VALUES(301,'flowable',1,'WO','B','{}',1,1,?,'mes-edhr-approval-v1',1,0)", process.getId());
            jdbc.update("INSERT INTO mes_pro_batch_record_approval_snapshot(id,execution_id,process_instance_id,process_definition_key,current_bpm_task_id,current_task_definition_key,approval_status,snapshot_json,snapshot_hash,tenant_id,deleted) VALUES(20,301,?,'mes-edhr-approval-v1',?,'approveNode','SUBMITTED','{}','hash',1,0)", process.getId(), taskId);
            jdbc.update("INSERT INTO mes_pro_edhr_work_task(id,task_code,task_type,status,execution_id,bpm_task_id,assignee_user_id,candidate_user_snapshot,action_url,tenant_id,deleted) VALUES(401,'flowable-task',?,'TODO',301,?,99,?,'/detail',1,0)", phase, taskId, candidates);
        }
        @Override public void close() {
            owned.close();
        }
    }
}
