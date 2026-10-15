package cn.iocoder.yudao.module.dcc.service.file;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import org.flowable.engine.*;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.*;import java.util.*;
import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;import static org.mockito.ArgumentMatchers.*;

class DccApplicationExecutionContractServiceTest {
    ProcessEngine engine;DccApplicationExecutionContractService service;DccControlledFileMapper files;String legacyXml,definitionId;
    @BeforeEach void fixture()throws Exception{
        TenantContextHolder.setTenantId(1L);engine=new StandaloneInMemProcessEngineConfiguration().setJdbcUrl("jdbc:h2:mem:w05_prepare_"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1")
                .setDatabaseSchemaUpdate("true").setAsyncExecutorActivate(false).setDisableIdmEngine(true).buildProcessEngine();
        Path root=Path.of("").toAbsolutePath();while(root!=null&&!Files.isRegularFile(root.resolve("AGENTS.md")))root=root.getParent();
        var matcher=java.util.regex.Pattern.compile("SET @dcc_upload_bpmn = '([\\s\\S]*?)';").matcher(Files.readString(root.resolve("IntRuoyiBackend/sql/mysql/20260930_dcc_a_workflow_bpmn_v4.sql")));
        assertTrue(matcher.find());legacyXml=matcher.group(1);definitionId=deploy(legacyXml);
        var definitions=mock(BpmProcessDefinitionService.class);when(definitions.getProcessDefinition(anyString())).thenAnswer(call->engine.getRepositoryService().createProcessDefinitionQuery().processDefinitionId(call.getArgument(0)).singleResult());
        when(definitions.getProcessDefinitionBpmnModel(anyString())).thenAnswer(call->engine.getRepositoryService().getBpmnModel(call.getArgument(0)));
        files=mock(DccControlledFileMapper.class);when(files.update(isNull(),any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(1);
        var configuration=new com.baomidou.mybatisplus.core.MybatisConfiguration();var builder=new org.apache.ibatis.builder.MapperBuilderAssistant(configuration,"prepared");builder.setCurrentNamespace(DccControlledFileMapper.class.getName());com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(builder,DccControlledFileDO.class);
        service=new DccApplicationExecutionContractService();ReflectionTestUtils.setField(service,"definitions",definitions);ReflectionTestUtils.setField(service,"files",files);
    }
    @AfterEach void cleanup(){if(engine!=null)engine.close();TenantContextHolder.clear();}
    String deploy(String xml){var deployment=engine.getRepositoryService().createDeployment().tenantId("1").addString("real.bpmn20.xml",xml).deploy();return engine.getRepositoryService().createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult().getId();}
    DccControlledFileDO draft(){return DccControlledFileDO.builder().id(20L).tenantId(1L).requesterId(99L).status("WORKING").changeType("NEW").build();}
    @Test void exactOldPreparationSurvivesNewLatestDefinitionAndDoesNotReadCurrentPublicMatrix(){var file=draft();var original=service.prepareLegacy(99L,file,definitionId);String changed=deploy(legacyXml.replace("name=\"培训等待\"","name=\"另一个未知模型\""));assertNotEquals(changed,original.processDefinitionId());var verified=service.requirePrepared(file,DccApplicationExecutionContractService.LEGACY,original.processDefinitionId(),original.definitionDigest(),DccControlledFileProcessDefinitionKeys.UPLOAD);assertEquals(original,verified);verify(files,times(1)).update(isNull(),any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));}
    @Test void absentPreparedUnknownGraphAndPublicUserClaimingLegacyAreRejected(){assertThrows(IllegalStateException.class,()->service.requirePrepared(draft(),"LEGACY_V4_ACTION",definitionId,"a".repeat(64),DccControlledFileProcessDefinitionKeys.UPLOAD));String changed=deploy(legacyXml.replace("name=\"培训等待\"","name=\"未知\""));assertThrows(IllegalStateException.class,()->service.prepareLegacy(99L,draft(),changed));assertThrows(IllegalArgumentException.class,()->service.requireOrdinaryPublic("LEGACY_V4_ACTION",definitionId,"a".repeat(64),DccControlledFileProcessDefinitionKeys.UPLOAD));verifyNoInteractions(files);}
    @Test void futureObsoleteSourceVerifiesItsExactPublicFullGraphWithoutPreparingOrActivatingNativeFile()throws Exception{
        Path root=Path.of("").toAbsolutePath();while(root!=null&&!Files.isRegularFile(root.resolve("AGENTS.md")))root=root.getParent();
        String xml=Files.readString(root.resolve("doc/tasks/20261011-dcc-w05-approval-signatures/bpmn-v5/obsolete.bpmn20.xml"));String id=deploy(xml);
        String digest=DccLifecycleEffectiveContractResolver.definitionDigest(engine.getRepositoryService().getBpmnModel(id));
        var verified=service.requireOrdinaryPublic(DccApplicationExecutionContractService.PUBLIC,id,digest,DccControlledFileProcessDefinitionKeys.OBSOLETE);
        assertEquals(id,verified.processDefinitionId());assertEquals(DccControlledFileProcessDefinitionKeys.OBSOLETE,verified.processKey());
        assertEquals("a3fec88d08ba5a1b4c6830ac05e653b5bc2faa72e9e11b3b54c1fa8006d2f79d",verified.definitionDigest());
        assertThrows(RuntimeException.class,()->service.requireOrdinaryPublic(DccApplicationExecutionContractService.PUBLIC,id,digest,DccControlledFileProcessDefinitionKeys.REVISION));
        verifyNoInteractions(files);var definitions=(BpmProcessDefinitionService)ReflectionTestUtils.getField(service,"definitions");verify(definitions,never()).getActiveProcessDefinition(anyString());
    }
}
