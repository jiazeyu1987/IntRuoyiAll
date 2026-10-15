package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Actual stored Flowable definition and runtime/history identities; API adapters are read-only port wiring. */
class DccLifecycleEffectiveContractResolverTest {
    private ProcessEngine engine;private BpmProcessInstanceService processes;private BpmProcessDefinitionService definitions;
    private String xml;
    @BeforeEach void setup()throws Exception {
        TenantContextHolder.setTenantId(1L);
        var config=new StandaloneInMemProcessEngineConfiguration();config.setJdbcUrl("jdbc:h2:mem:w06_contract_"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1");
        config.setDatabaseSchemaUpdate("true");config.setAsyncExecutorActivate(false);config.setDisableIdmEngine(true);
        engine=config.buildProcessEngine();processes=mock(BpmProcessInstanceService.class);definitions=mock(BpmProcessDefinitionService.class);
        when(processes.getProcessInstance(anyString())).thenAnswer(call->engine.getRuntimeService().createProcessInstanceQuery()
                .processInstanceId(call.getArgument(0)).includeProcessVariables().singleResult());
        when(processes.getHistoricProcessInstance(anyString())).thenAnswer(call->engine.getHistoryService().createHistoricProcessInstanceQuery()
                .processInstanceId(call.getArgument(0)).includeProcessVariables().singleResult());
        when(definitions.getProcessDefinition(anyString())).thenAnswer(call->engine.getRepositoryService().createProcessDefinitionQuery()
                .processDefinitionId(call.getArgument(0)).singleResult());
        when(definitions.getProcessDefinitionBpmnModel(anyString())).thenAnswer(call->engine.getRepositoryService().getBpmnModel(call.getArgument(0)));
        var sql=legacySql();
        var match=Pattern.compile("SET @dcc_upload_bpmn = '([\\s\\S]*?)';").matcher(sql);assertTrue(match.find());xml=match.group(1);
    }
    @AfterEach void cleanup(){if(engine!=null){engine.getRepositoryService().createDeploymentQuery().list().forEach(row->
        engine.getRepositoryService().deleteDeployment(row.getId(),true));engine.close();}TenantContextHolder.clear();}
    @Test void frozenLegacyGraphResolvesAcrossDeploymentNumbersAndIgnoresLatestChangedDefinition()throws Exception {
        var old=deploy(xml);var instance=engine.getRuntimeService().startProcessInstanceById(old.getId(),"20",variables("20"));
        deploy(xml.replace("name=\"培训等待\"","name=\"未知改图\""));
        var file=file(instance.getId());var contract=resolve(file);
        assertEquals(old.getId(),contract.processDefinitionId());
        assertEquals(DccLifecycleEffectiveContract.Rule.LEGACY_V4_PRESET_DATE,contract.rule());
        assertNull(file.getEffectiveMode());
        var again=deploy(xml);assertNotEquals(old.getVersion(),again.getVersion());
        var newInstance=engine.getRuntimeService().startProcessInstanceById(again.getId(),"20",variables("20"));
        assertEquals(contract.definitionDigest(),resolve(file(newInstance.getId())).definitionDigest());
    }
    @Test void endedHistoryUsesItsOwnExactDefinitionInsteadOfAnyActiveDefinition()throws Exception {
        var definition=deploy(xml);var instance=engine.getRuntimeService().startProcessInstanceById(definition.getId(),"20",variables("20"));
        engine.getRuntimeService().deleteProcessInstance(instance.getId(),"task-owned test end");assertNull(processes.getProcessInstance(instance.getId()));
        var contract=resolve(file(instance.getId()));assertEquals(definition.getId(),contract.processDefinitionId());
    }
    @Test void sameKeyDeploymentNumberFourWithChangedTrainingGraphIsRejected()throws Exception {
        for(int i=0;i<3;i++)deploy(xml);
        var changed=deploy(xml.replace("sourceRef=\"TRAINING\" targetRef=\"DOC_CONTROL_REVIEW\"","sourceRef=\"DOC_CONTROL_REVIEW\" targetRef=\"TRAINING\""));
        assertEquals(4,changed.getVersion());var instance=engine.getRuntimeService().startProcessInstanceById(changed.getId(),"20",variables("20"));
        assertThrows(RuntimeException.class,()->resolve(file(instance.getId())));
    }
    @Test void wrongTenantBusinessKeyOrMissingInstanceNeverGetsLegacyContract()throws Exception {
        var definition=deploy(xml);var wrongBusiness=engine.getRuntimeService().startProcessInstanceById(definition.getId(),"21",variables("21"));
        assertThrows(RuntimeException.class,()->resolve(file(wrongBusiness.getId())));
        assertThrows(RuntimeException.class,()->resolve(file("missing-round")));
        TenantContextHolder.setTenantId(2L);assertThrows(RuntimeException.class,()->resolve(file(wrongBusiness.getId())));
    }
    @Test void businessKeyMatchingButFormalControlledFileVariableConflictingIsRejected() {
        var definition=deploy(xml);var instance=engine.getRuntimeService().startProcessInstanceById(definition.getId(),"20",variables("21"));
        assertThrows(IllegalStateException.class,()->resolve(file(instance.getId())));
    }
    @Test void registeredRevisionGraphUsesItsExactCompleteDigest()throws Exception {
        var sql=legacySql();
        var match=Pattern.compile("SET @dcc_revision_bpmn = '([\\s\\S]*?)';").matcher(sql);assertTrue(match.find());
        var definition=deploy(match.group(1));var instance=engine.getRuntimeService().startProcessInstanceById(definition.getId(),"20",variables("20"));
        var file=file(instance.getId());file.setProcessDefinitionKey(DccControlledFileProcessDefinitionKeys.REVISION);
        assertEquals("42aff38a44dec91c5cd64cae5a19507263260d4d5fed7ec0f28b3e4632805cc4",resolve(file).definitionDigest());
        assertEquals(DccLifecycleEffectiveContract.Rule.LEGACY_V4_PRESET_DATE,resolve(file).rule());assertNull(file.getEffectiveMode());
    }
    private org.flowable.engine.repository.ProcessDefinition deploy(String body){var deployed=engine.getRepositoryService().createDeployment().tenantId("1")
        .addString("frozen.bpmn20.xml",body).deploy();return engine.getRepositoryService().createProcessDefinitionQuery().deploymentId(deployed.getId()).singleResult();}
    private Map<String,Object> variables(String id){return Map.of("controlledFileId",id,"PROCESS_DCC_TASK_OBLIGATION_IDS",Map.of("MATRIX_REVIEW",List.of("dept-1")));}
    private DccControlledFileDO file(String round){return DccControlledFileDO.builder().id(20L).tenantId(1L).processInstanceId(round)
        .processDefinitionKey(DccControlledFileProcessDefinitionKeys.UPLOAD).effectiveDate(java.time.LocalDate.of(2026,10,12)).build();}
    private DccLifecycleEffectiveContract resolve(DccControlledFileDO file){var resolver=new DccLifecycleEffectiveContractResolver();
        var bound=new DccBoundWorkflowContractResolver();
        ReflectionTestUtils.setField(bound,"processes",processes);ReflectionTestUtils.setField(bound,"definitions",definitions);
        // Original real legacy fixture has no new round-link/project/prepared facts. Never manufacture them.
        ReflectionTestUtils.setField(bound,"rounds",new DccApplicationRoundService());
        ReflectionTestUtils.setField(resolver,"boundContracts",bound);
        return resolver.resolve(file);}
    private String legacySql()throws java.io.IOException {
        var repository=Path.of("").toAbsolutePath();
        while(repository!=null && !Files.isRegularFile(repository.resolve("AGENTS.md")))repository=repository.getParent();
        if(repository==null)throw new IllegalStateException("Known legacy BPMN repository source root is missing");
        return Files.readString(repository.resolve("IntRuoyiBackend/sql/mysql/20260930_dcc_a_workflow_bpmn_v4.sql"),java.nio.charset.StandardCharsets.UTF_8);
    }
}
