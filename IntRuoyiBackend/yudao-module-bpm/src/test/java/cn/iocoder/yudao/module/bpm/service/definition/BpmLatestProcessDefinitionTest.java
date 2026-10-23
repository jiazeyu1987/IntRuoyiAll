package cn.iocoder.yudao.module.bpm.service.definition;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmApprovalDetailRespVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmProcessInstanceCreateReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.definition.BpmProcessDefinitionInfoMapper;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceServiceImpl;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.engine.repository.ProcessDefinition;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real isolated Flowable deployments/query/instance start, no business DB or UI actions. */
class BpmLatestProcessDefinitionTest {
    private static final String KEY = "dcc-controlled-file-upload";
    private static ProcessEngine engine;
    private BpmProcessDefinitionServiceImpl definitions;
    private BpmProcessDefinitionInfoMapper info;

    @BeforeAll static void startEngine() {
        var configuration = new StandaloneInMemProcessEngineConfiguration();
        configuration.setJdbcUrl("jdbc:h2:mem:g53_latest_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        configuration.setDatabaseSchemaUpdate("true");
        configuration.setAsyncExecutorActivate(false);
        configuration.setDisableIdmEngine(true);
        engine = configuration.buildProcessEngine();
    }

    @AfterAll static void closeEngine() { engine.close(); }

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        TenantContextHolder.setIgnore(false);
        definitions = new BpmProcessDefinitionServiceImpl();
        info = mock(BpmProcessDefinitionInfoMapper.class);
        ReflectionTestUtils.setField(definitions, "repositoryService", engine.getRepositoryService());
        ReflectionTestUtils.setField(definitions, "processDefinitionMapper", info);
    }

    @AfterEach void cleanup() {
        for (var deployment : engine.getRepositoryService().createDeploymentQuery().list()) {
            engine.getRepositoryService().deleteDeployment(deployment.getId(), true);
        }
        TenantContextHolder.clear();
    }

    ProcessDefinition deploy(String tenant) {
        String bpmn = """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" targetNamespace="g53-test">
                  <process id="dcc-controlled-file-upload" name="隔离上传" isExecutable="true">
                    <startEvent id="start"/><sequenceFlow id="s1" sourceRef="start" targetRef="review"/>
                    <userTask id="review" name="隔离审核"/><sequenceFlow id="s2" sourceRef="review" targetRef="end"/>
                    <endEvent id="end"/>
                  </process>
                </definitions>
                """;
        var deployed = engine.getRepositoryService().createDeployment().tenantId(tenant)
                .addString("g53.bpmn20.xml", bpmn).deploy();
        return engine.getRepositoryService().createProcessDefinitionQuery().deploymentId(deployed.getId()).singleResult();
    }

    @Test void sameTenantMultipleActiveVersionsSelectLatestAndKeepExistingInstanceOnItsOriginalDefinition() {
        var first = deploy("1");
        var existing = engine.getRuntimeService().startProcessInstanceById(first.getId());
        var latest = deploy("1");
        deploy("2");deploy("2");deploy("2");
        var selected = definitions.getActiveProcessDefinition(KEY);
        assertEquals(latest.getId(), selected.getId());assertEquals(2, selected.getVersion());
        assertEquals("1", selected.getTenantId());
        assertEquals(first.getId(), engine.getRuntimeService().createProcessInstanceQuery()
                .processInstanceId(existing.getId()).singleResult().getProcessDefinitionId());
        assertFalse(engine.getRepositoryService().getProcessDefinition(first.getId()).isSuspended());
    }

    @Test void latestSuspendedCannotSilentlyStartTheOlderActiveVersion() {
        var older = deploy("1");var latest = deploy("1");
        engine.getRepositoryService().suspendProcessDefinitionById(latest.getId(), false, null);
        assertNull(definitions.getActiveProcessDefinition(KEY));
        assertFalse(engine.getRepositoryService().getProcessDefinition(older.getId()).isSuspended());
        var service = instanceService();
        var request = dto();
        var boundaryFailure = assertThrows(RuntimeException.class, () -> service.createProcessInstance(9L, request));
        // The existing authenticated Flowable boundary wraps supplier failures; assert its actual business cause.
        var failure = assertInstanceOf(ServiceException.class, boundaryFailure.getCause());
        assertEquals(PROCESS_DEFINITION_NOT_EXISTS.getCode(), failure.getCode());
        assertEquals(0, engine.getRuntimeService().createProcessInstanceQuery().count());
    }

    @Test void absentTenantKeyNeverBorrowsAnotherTenantDefinition() {
        deploy("2");
        assertNull(definitions.getActiveProcessDefinition(KEY));
        assertNull(definitions.getActiveProcessDefinition("missing-key"));
    }

    @Test void formalDtoStartUsesLatestDefinitionWhileExplicitIdStartRetainsTheChosenOlderDefinition() {
        var older = deploy("1");var latest = deploy("1");
        when(info.selectByProcessDefinitionId(anyString())).thenReturn(new BpmProcessDefinitionInfoDO());
        var service = instanceService();
        String byKey = service.createProcessInstance(9L, dto());
        assertEquals(latest.getId(), engine.getRuntimeService().createProcessInstanceQuery()
                .processInstanceId(byKey).singleResult().getProcessDefinitionId());
        var explicit = new BpmProcessInstanceCreateReqVO();
        explicit.setProcessDefinitionId(older.getId());explicit.setVariables(new HashMap<>());
        // Existing explicit-id contract gets that definition; keep name generation on its existing path.
        when(info.selectByProcessDefinitionId(older.getId())).thenReturn(new BpmProcessDefinitionInfoDO());
        String byId = service.createProcessInstance(9L, explicit);
        assertEquals(older.getId(), engine.getRuntimeService().createProcessInstanceQuery()
                .processInstanceId(byId).singleResult().getProcessDefinitionId());
    }

    private BpmProcessInstanceServiceImpl instanceService() {
        var service = spy(new BpmProcessInstanceServiceImpl());
        ReflectionTestUtils.setField(service, "runtimeService", engine.getRuntimeService());
        ReflectionTestUtils.setField(service, "processDefinitionService", definitions);
        var detail = new BpmApprovalDetailRespVO();detail.setActivityNodes(new ArrayList<>());
        doReturn(detail).when(service).getApprovalDetail(anyLong(), any());
        return service;
    }

    private BpmProcessInstanceCreateReqDTO dto() {
        var request = new BpmProcessInstanceCreateReqDTO();request.setProcessDefinitionKey(KEY);
        request.setBusinessKey("G53-ISOLATED");request.setName("隔离真实发起");request.setVariables(new HashMap<>());
        return request;
    }
}
