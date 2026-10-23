package cn.iocoder.yudao.module.bpm.service.task;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmApprovalDetailRespVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmTaskCandidateStrategyEnum;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BpmProcessInstanceExactDefinitionStartTest {
    private static ProcessEngine engine;
    private static ProcessDefinition frozen;
    private static ProcessDefinition latest;
    private static ProcessDefinition foreign;
    private BpmProcessDefinitionService definitions;
    private BpmProcessInstanceServiceImpl service;
    private AdminUserApi users;
    private BpmProcessDefinitionInfoDO info;

    @BeforeAll
    static void openPrivateEngine() {
        engine = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration()
                .setJdbcUrl("jdbc:h2:mem:exact-start-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1")
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE)
                .setAsyncExecutorActivate(false).buildProcessEngine();
        frozen = deploy("1", "frozen");
        latest = deploy("1", "later");
        foreign = deploy("2", "foreign");
    }

    private static ProcessDefinition deploy(String tenant, String label) {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" targetNamespace="exact-start">
                  <process id="exact_start_contract" name="%s" isExecutable="true">
                    <startEvent id="start"/><sequenceFlow id="s1" sourceRef="start" targetRef="review"/>
                    <userTask id="review" name="Review"/><sequenceFlow id="s2" sourceRef="review" targetRef="end"/>
                    <endEvent id="end"/>
                  </process>
                </definitions>
                """.formatted(label);
        var deployment = engine.getRepositoryService().createDeployment().tenantId(tenant)
                .addString(label + ".bpmn20.xml", xml).deploy();
        return engine.getRepositoryService().createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
    }

    @AfterAll
    static void closePrivateEngine() { if (engine != null) engine.close(); }

    @BeforeEach
    void prepareRealService() {
        TenantContextHolder.setTenantId(1L);
        definitions = mock(BpmProcessDefinitionService.class);
        users = mock(AdminUserApi.class);
        service = spy(new BpmProcessInstanceServiceImpl());
        ReflectionTestUtils.setField(service, "runtimeService", engine.getRuntimeService());
        ReflectionTestUtils.setField(service, "processDefinitionService", definitions);
        ReflectionTestUtils.setField(service, "adminUserApi", users);
        info = new BpmProcessDefinitionInfoDO();
        when(definitions.getActiveProcessDefinition("exact_start_contract")).thenReturn(latest);
        when(definitions.getProcessDefinition(frozen.getId())).thenReturn(frozen);
        when(definitions.getProcessDefinitionInfo(anyString())).thenReturn(info);
        when(definitions.canUserStartProcessDefinition(same(info), eq(91L))).thenReturn(true);
        doReturn(new BpmApprovalDetailRespVO().setActivityNodes(new ArrayList<>()))
                .when(service).getApprovalDetail(eq(91L), any());
    }

    @AfterEach
    void cleanTaskInstances() {
        engine.getRuntimeService().createProcessInstanceQuery().list().forEach(
                instance -> engine.getRuntimeService().deleteProcessInstance(instance.getId(), "private test cleanup"));
        TenantContextHolder.clear();
    }

    private BpmProcessInstanceCreateReqDTO request() {
        return new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey("exact_start_contract")
                .setProcessDefinitionId(frozen.getId()).setBusinessKey("DCC:101:round-3")
                .setName("  Frozen approval  ").setVariables(new HashMap<>(Map.of("version", "A/3")))
                .setStartUserSelectAssignees(Map.of("review", List.of(92L)))
                .setApproveUserSelectAssignees(Map.of("later_review", List.of(93L)));
    }

    private void rejectsWithoutInstance(BpmProcessInstanceCreateReqDTO req) {
        assertThrows(RuntimeException.class, () -> service.createProcessInstance(91L, req));
        assertEquals(0, engine.getRuntimeService().createProcessInstanceQuery().count());
        verify(definitions, never()).getActiveProcessDefinition(anyString());
    }

    @Test
    void frozenIdWinsOverNewDeploymentAndPreservesBusinessFacts() {
        String id = service.createProcessInstance(91L, request());
        ProcessInstance instance = engine.getRuntimeService().createProcessInstanceQuery()
                .processInstanceId(id).includeProcessVariables().singleResult();
        assertNotEquals(frozen.getId(), latest.getId());
        assertEquals(frozen.getId(), instance.getProcessDefinitionId());
        assertEquals("DCC:101:round-3", instance.getBusinessKey());
        assertEquals("Frozen approval", instance.getName());
        assertEquals("1", instance.getTenantId());
        assertEquals("91", instance.getStartUserId());
        assertEquals("A/3", instance.getProcessVariables().get("version"));
        assertEquals(Map.of("review", List.of(92L)), instance.getProcessVariables()
                .get(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_SELECT_ASSIGNEES));
        assertEquals(Map.of("later_review", List.of(93L)), instance.getProcessVariables()
                .get(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_APPROVE_USER_SELECT_ASSIGNEES));
        verify(definitions).canUserStartProcessDefinition(info, 91L);
        verify(definitions, never()).getActiveProcessDefinition(anyString());
    }

    @Test
    void explicitNullRetainsOriginalKeyEntry() {
        String id = service.createProcessInstance(91L, request().setProcessDefinitionId(null));
        assertEquals(latest.getId(), engine.getRuntimeService().createProcessInstanceQuery()
                .processInstanceId(id).singleResult().getProcessDefinitionId());
        verify(definitions).getActiveProcessDefinition("exact_start_contract");
        verify(definitions, never()).getProcessDefinition(anyString());
    }

    @Test void missingExactDefinitionCannotFallBack() { rejectsWithoutInstance(request().setProcessDefinitionId("missing")); }
    @Test void blankExactDefinitionCannotFallBack() { rejectsWithoutInstance(request().setProcessDefinitionId(" ")); }
    @Test void wrongKeyCannotStartExactDefinition() { rejectsWithoutInstance(request().setProcessDefinitionKey("other_key")); }

    @Test void returnedDefinitionIdMustMatchExactRequest() {
        when(definitions.getProcessDefinition(frozen.getId())).thenReturn(latest);
        rejectsWithoutInstance(request());
    }

    @Test void foreignTenantCannotStartExactDefinition() {
        when(definitions.getProcessDefinition(foreign.getId())).thenReturn(foreign);
        rejectsWithoutInstance(request().setProcessDefinitionId(foreign.getId()));
    }

    @Test void missingTenantCannotStartTenantDefinition() {
        TenantContextHolder.clear();
        rejectsWithoutInstance(request());
    }

    @Test void suspendedDefinitionKeepsOriginalGuard() {
        ProcessDefinition suspended = mock(ProcessDefinition.class);
        when(suspended.getId()).thenReturn(frozen.getId());
        when(suspended.getKey()).thenReturn(frozen.getKey());
        when(suspended.getTenantId()).thenReturn("1");
        when(suspended.isSuspended()).thenReturn(true);
        when(definitions.getProcessDefinition(frozen.getId())).thenReturn(suspended);
        rejectsWithoutInstance(request());
    }

    @Test void missingFormalDefinitionInfoKeepsOriginalGuard() {
        when(definitions.getProcessDefinitionInfo(frozen.getId())).thenReturn(null);
        rejectsWithoutInstance(request());
    }

    @Test void startPermissionDenialKeepsOriginalGuard() {
        when(definitions.canUserStartProcessDefinition(info, 91L)).thenReturn(false);
        rejectsWithoutInstance(request());
    }

    private void requireSelfSelectedCandidate() {
        var node = new BpmApprovalDetailRespVO.ActivityNode().setId("review").setName("Review")
                .setCandidateStrategy(BpmTaskCandidateStrategyEnum.START_USER_SELECT.getStrategy());
        doReturn(new BpmApprovalDetailRespVO().setActivityNodes(new ArrayList<>(List.of(node))))
                .when(service).getApprovalDetail(eq(91L), any());
    }

    @Test void missingSelectedCandidateKeepsOriginalGuard() {
        requireSelfSelectedCandidate();
        rejectsWithoutInstance(request().setStartUserSelectAssignees(Map.of()));
    }

    @Test void selectedCandidateMustExistInFormalDirectory() {
        requireSelfSelectedCandidate();
        when(users.getUserMap(List.of(92L))).thenReturn(Map.of());
        rejectsWithoutInstance(request());
    }
}
