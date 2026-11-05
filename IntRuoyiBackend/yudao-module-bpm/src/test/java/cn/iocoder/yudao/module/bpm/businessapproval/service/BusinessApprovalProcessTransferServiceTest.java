package cn.iocoder.yudao.module.bpm.businessapproval.service;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils;
import cn.iocoder.yudao.module.bpm.controller.admin.businessapproval.vo.BusinessApprovalPolicyTransferVO;
import cn.iocoder.yudao.module.bpm.controller.admin.definition.vo.model.BpmModelMetaInfoVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.*;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.candidate.BpmTaskCandidateInvoker;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.BpmnModelUtils;
import cn.iocoder.yudao.module.bpm.service.definition.*;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.*;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.service.permission.*;
import org.flowable.engine.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real Flowable deployment and Spring rollback; role/user services are isolated external ports. */
class BusinessApprovalProcessTransferServiceTest {
    ProcessEngine engine;
    RepositoryService repository;
    TransactionTemplate transaction;
    JdbcTemplate jdbc;
    BusinessApprovalProcessTransferService service;
    BpmProcessDefinitionService definitions;
    RoleService roles;
    RoleCategoryService roleCategories;
    BpmCategoryService categories;
    AdminUserMapper users;
    BpmTaskCandidateInvoker candidates;
    BusinessApprovalPolicyTransferVO pack;
    String sourceDefinition;
    static final String KEY = "mes-edhr-batch-execution-void-v1";
    static final String XML = """
        <?xml version="1.0" encoding="UTF-8"?>
        <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="test">
          <process id="mes-edhr-batch-execution-void-v1" name="eDHR批次执行作废" isExecutable="true">
            <startEvent id="start"/><sequenceFlow id="a" sourceRef="start" targetRef="review"/>
            <userTask id="review" name="作废审核"><extensionElements><flowable:candidateStrategy>10</flowable:candidateStrategy>
              <flowable:candidateParam>123</flowable:candidateParam></extensionElements></userTask>
            <sequenceFlow id="b" sourceRef="review" targetRef="end"/><endEvent id="end"/>
          </process>
        </definitions>
        """;

    @BeforeEach void setup() {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:transfer_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        var tm = new DataSourceTransactionManager(ds);
        transaction = new TransactionTemplate(tm);
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE imported_role (id BIGINT PRIMARY KEY)");
        var config = new SpringProcessEngineConfiguration();
        config.setDataSource(ds); config.setTransactionManager(tm); config.setDatabaseSchemaUpdate("true");
        config.setAsyncExecutorActivate(false); config.setDisableIdmEngine(true);
        engine = config.buildProcessEngine(); repository = engine.getRepositoryService();
        definitions = mock(BpmProcessDefinitionService.class);
        roles = mock(RoleService.class); roleCategories = mock(RoleCategoryService.class);
        categories = mock(BpmCategoryService.class); users = mock(AdminUserMapper.class);
        candidates = mock(BpmTaskCandidateInvoker.class);
        service = new BusinessApprovalProcessTransferService();
        ReflectionTestUtils.setField(service, "repositoryService", repository);
        ReflectionTestUtils.setField(service, "definitionService", definitions);
        ReflectionTestUtils.setField(service, "roleService", roles);
        ReflectionTestUtils.setField(service, "roleCategoryService", roleCategories);
        ReflectionTestUtils.setField(service, "categoryService", categories);
        ReflectionTestUtils.setField(service, "userMapper", users);
        ReflectionTestUtils.setField(service, "candidateInvoker", candidates);

        TenantContextHolder.setTenantId(1L);
        var source = repository.createDeployment().tenantId("1").addString("void.bpmn", XML).deploy();
        sourceDefinition = repository.createProcessDefinitionQuery().deploymentId(source.getId()).singleResult().getId();
        var info = new BpmProcessDefinitionInfoDO();
        info.setModelType(10); info.setFormType(20); info.setCategory("BATCH_RECORD");
        info.setFormCustomCreatePath("/void/create"); info.setFormCustomViewPath("/void/view");
        info.setManagerUserIds(List.of(11L)); info.setVisible(true);
        when(definitions.getProcessDefinitionInfo(sourceDefinition)).thenReturn(info);
        var category = BpmCategoryDO.builder().id(1L).code("BATCH_RECORD").name("批记录").status(0).sort(1).build();
        when(categories.getCategoryMap(any())).thenAnswer(call -> TenantContextHolder.getRequiredTenantId() == 1L ? Map.of("BATCH_RECORD", category) : Map.of());
        var role = new RoleDO(); role.setId(123L); role.setTenantId(1L); role.setCode("edhr_batch_void_admin");
        role.setName("批次作废管理员"); role.setStatus(0); role.setCategoryId(2L);
        when(roles.getRole(123L)).thenReturn(role);
        var rc = new RoleCategoryDO(); rc.setId(2L); rc.setTenantId(1L); rc.setCode("batch-record"); rc.setName("批记录"); rc.setSort(1); rc.setStatus(0);
        when(roleCategories.getRoleCategory(2L)).thenReturn(rc);
        when(roleCategories.getRoleCategoryList()).thenReturn(List.of());
        when(roleCategories.createRoleCategory(any())).thenReturn(902L);
        when(roles.createRole(any(), eq(2))).thenAnswer(call -> { jdbc.update("INSERT INTO imported_role VALUES (987)"); return 987L; });
        var owner = new AdminUserDO(); owner.setId(200L); owner.setTenantId(202L); owner.setStatus(0);
        when(users.selectSessionSubject(202L, 200L)).thenReturn(owner);
        var request = new MockHttpServletRequest(); WebFrameworkUtils.setLoginUserId(request, 200L);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(definitions.createProcessDefinition(any(), any(), any(), isNull(), isNull())).thenAnswer(call -> {
            org.flowable.engine.repository.Model model = call.getArgument(0);
            byte[] xml = call.getArgument(2);
            var deployment = repository.createDeployment().tenantId("202").key(model.getKey()).name(model.getName())
                    .addBytes(model.getKey() + ".bpmn", xml).deploy();
            return repository.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult().getId();
        });
        var policy = new BusinessApprovalPolicyTransferVO.Item();
        policy.setPolicyMode("BPM_REQUIRED"); policy.setStatus("PUBLISHED"); policy.setProcessDefinitionKey(KEY);
        pack = new BusinessApprovalPolicyTransferVO(); pack.setPolicies(List.of(policy));
        pack.setProcesses(service.exportProcesses(pack.getPolicies()));
        TenantContextHolder.setTenantId(202L);
    }

    @AfterEach void close() {
        engine.close(); RequestContextHolder.resetRequestAttributes(); TenantContextHolder.clear();
    }

    @Test void blankTenantReceivesRealDeployedProcessAndRemappedRoleWithoutSourceMutation() {
        var exported = pack.getProcesses().get(0);
        assertTrue(exported.getBpmnXml().contains("edhr_batch_void_admin"));
        assertTrue(exported.getMetaInfo().getManagerUserIds().isEmpty());
        assertNull(exported.getRoles().get(0).getCategory().getId());
        transaction.executeWithoutResult(s -> service.importProcesses(pack));
        var definition = repository.createProcessDefinitionQuery().processDefinitionTenantId("202").singleResult();
        assertNotNull(definition); assertFalse(definition.isSuspended());
        var task = (org.flowable.bpmn.model.UserTask) repository.getBpmnModel(definition.getId()).getMainProcess().getFlowElement("review");
        assertEquals("987", BpmnModelUtils.parseCandidateParam(task));
        var original = (org.flowable.bpmn.model.UserTask) repository.getBpmnModel(sourceDefinition).getMainProcess().getFlowElement("review");
        assertEquals("123", BpmnModelUtils.parseCandidateParam(original));
        var model = repository.createModelQuery().modelTenantId("202").singleResult();
        assertEquals(definition.getDeploymentId(), model.getDeploymentId());
        assertTrue(model.getMetaInfo().contains("200"));
        verify(candidates).validateBpmnConfig(any());
        // Re-import reuses the target definition and never creates duplicate roles or versions.
        transaction.executeWithoutResult(s -> service.importProcesses(pack));
        assertEquals(1L, repository.createProcessDefinitionQuery().processDefinitionTenantId("202").count());
        verify(roles, times(1)).createRole(any(), anyInt());
    }

    @Test void laterFailureRollsBackEngineModelDeploymentAndRoleWrites() {
        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(s -> {
            service.importProcesses(pack);
            throw new IllegalStateException("policy insert failed");
        }));
        assertEquals(0L, repository.createProcessDefinitionQuery().processDefinitionTenantId("202").count());
        assertEquals(0L, repository.createDeploymentQuery().deploymentTenantId("202").count());
        assertEquals(0L, repository.createModelQuery().modelTenantId("202").count());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM imported_role", Integer.class));
    }

    @Test void missingDependencyOrUnsupportedStaticIdentityCannotImport() {
        pack.setProcesses(List.of());
        assertThrows(RuntimeException.class, () -> transaction.executeWithoutResult(s -> service.importProcesses(pack)));
        TenantContextHolder.setTenantId(1L);
        pack.setProcesses(service.exportProcesses(pack.getPolicies()));
        TenantContextHolder.setTenantId(202L);
        pack.getProcesses().get(0).setBpmnXml(pack.getProcesses().get(0).getBpmnXml().replace("<userTask ", "<userTask flowable:assignee=\"11\" "));
        assertThrows(RuntimeException.class, () -> transaction.executeWithoutResult(s -> service.importProcesses(pack)));
        verify(roles, never()).createRole(any(), anyInt());
    }

    @Test void suspendedTargetWorkflowIsNotReactivatedOrReplaced() {
        transaction.executeWithoutResult(s -> service.importProcesses(pack));
        var target = repository.createProcessDefinitionQuery().processDefinitionTenantId("202").singleResult();
        repository.suspendProcessDefinitionById(target.getId());
        assertThrows(RuntimeException.class, () -> transaction.executeWithoutResult(s -> service.importProcesses(pack)));
        assertEquals(1L, repository.createProcessDefinitionQuery().processDefinitionTenantId("202").count());
        assertTrue(repository.getProcessDefinition(target.getId()).isSuspended());
    }
}
