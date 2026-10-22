package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectApplicationAttributesDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectApplicationAttributesMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.common.engine.api.io.InputStreamProvider;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Actual preparation/verifier with declared unit-test persistence/BPM read ports.
 * It never mocks a successful verifier. H2/Flowable end-to-end proof lives in the consumer database test. */
final class DccW08PreparedUnitFixture {
    final DccApplicationExecutionContractService contracts=new DccApplicationExecutionContractService();
    final DccApplicationRoundService rounds=mock(DccApplicationRoundService.class);
    private final Map<String,org.flowable.bpmn.model.BpmnModel> models=new HashMap<>();
    private final Function<Long,DccControlledFileDO> source;
    private final DccControlledFileMapper consumer;

    DccW08PreparedUnitFixture(DccControlledFileMapper consumer,Function<Long,DccControlledFileDO> source,
            DccControlledFileMasterMapper masters,DccProjectAccessService access,DccControlledFileCategoryPermissionSupport permissions) {
        this.consumer=consumer;this.source=source;
        for(Class<?> entity:List.of(DccControlledFileDO.class,DccProjectApplicationAttributesDO.class))
            if(com.baomidou.mybatisplus.core.metadata.TableInfoHelper.getTableInfo(entity)==null)
                com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                        new org.apache.ibatis.builder.MapperBuilderAssistant(new org.apache.ibatis.session.Configuration(),"w08-unit-declared-mapper"),entity);
        var definitions=mock(BpmProcessDefinitionService.class);
        var files=mock(DccControlledFileMapper.class);
        var attributes=mock(DccProjectApplicationAttributesMapper.class);
        var processes=mock(BpmProcessInstanceService.class);
        var transitions=new DccExecutionDefinitionTransitions();
        String upload=DccControlledFileProcessDefinitionKeys.UPLOAD,revision=DccControlledFileProcessDefinitionKeys.REVISION;
        models.putAll(loadLegacyModels());
        lenient().when(definitions.getProcessDefinition(anyString())).thenAnswer(call->{
            String key=keyForId(call.getArgument(0));if(key==null)return null;
            var definition=mock(ProcessDefinition.class);when(definition.getId()).thenReturn(call.getArgument(0));
            when(definition.getTenantId()).thenReturn(TenantContextHolder.getRequiredTenantId().toString());
            when(definition.getKey()).thenReturn(key);when(definition.getDeploymentId()).thenReturn("declared-unit-bpm-deployment");return definition;
        });
        lenient().when(definitions.getProcessDefinitionBpmnModel(anyString())).thenAnswer(call->models.get(keyForId(call.getArgument(0))));
        configurePersistence(files,attributes,processes);
        transitions.setDefinitionTransitions(List.of(new DccExecutionDefinitionTransitions.Transition(id(upload),digest(upload),id(revision),digest(revision))));
        var bound=new DccBoundWorkflowContractResolver();wire(bound,"definitions",definitions,"processes",processes,"rounds",rounds,"attributes",attributes);
        var resolver=new DccLifecycleEffectiveContractResolver();wire(resolver,"boundContracts",bound);
        wire(contracts,"definitions",definitions,"files",files,"rounds",rounds,"sourceAttributes",attributes,"effectiveContractResolver",resolver,
                "processes",processes,"projectAccess",access,"categoryPermissions",permissions,"masters",masters,"transitions",transitions);
    }

    static Map<String,org.flowable.bpmn.model.BpmnModel> loadLegacyModels() {
        Map<String,org.flowable.bpmn.model.BpmnModel> models=new HashMap<>();
        try {
            Path root=Path.of("").toAbsolutePath();while(root!=null&&!Files.isRegularFile(root.resolve("AGENTS.md")))root=root.getParent();
            String sql=Files.readString(root.resolve("IntRuoyiBackend/sql/mysql/20260930_dcc_a_workflow_bpmn_v4.sql"));
            for(String kind:List.of("upload","revision")){
                var matcher=java.util.regex.Pattern.compile("SET @dcc_"+kind+"_bpmn = '([\\s\\S]*?)';").matcher(sql);
                if(!matcher.find())throw new IllegalStateException("actual immutable graph fixture missing");
                byte[] xml=matcher.group(1).getBytes(StandardCharsets.UTF_8);
                models.put(kind.equals("upload")?DccControlledFileProcessDefinitionKeys.UPLOAD:DccControlledFileProcessDefinitionKeys.REVISION,
                        new BpmnXMLConverter().convertToBpmnModel((InputStreamProvider)()->new ByteArrayInputStream(xml),false,false));
            }
        } catch(java.io.IOException failure){throw new IllegalStateException(failure);}
        return models;
    }

    private void configurePersistence(DccControlledFileMapper files,DccProjectApplicationAttributesMapper attributes,BpmProcessInstanceService processes) {
        lenient().when(files.selectById(anyLong())).thenAnswer(call->lookup(call.getArgument(0)));
        lenient().when(files.selectByIdAndTenantForUpdate(anyLong(),anyLong())).thenAnswer(call->lookup(call.getArgument(1)));
        lenient().when(files.update(isNull(),any())).thenReturn(1);
        lenient().when(rounds.requireDraft(anyString(),anyLong())).thenReturn(1);
        lenient().when(rounds.require(anyString(),anyLong(),anyString())).thenReturn(1);
        lenient().when(rounds.resolveBoundBpm(anyString(),anyLong(),anyInt())).thenAnswer(call->lookup(call.getArgument(1)).getProcessInstanceId());
        lenient().when(attributes.selectList(any())).thenReturn(List.of());
        lenient().when(attributes.find(anyString(),anyLong(),eq(1))).thenAnswer(call->{
            var file=lookup(call.getArgument(1));if(file==null)return null;
            var row=new DccProjectApplicationAttributesDO();row.setTenantId(file.getTenantId());row.setProjectCodeId(file.getDccProjectCodeId());
            row.setApplicationId(file.getId());row.setApplicationType(call.getArgument(0));row.setApplicationRound(1);
            row.setSubmitted(!"WORKING".equals(file.getStatus()));return row;
        });
        lenient().when(processes.getProcessInstance(anyString())).thenAnswer(call->{
            String bpm=call.getArgument(0);DccControlledFileDO file=null;
            for(var invocation:mockingDetails(consumer).getInvocations())for(Object arg:invocation.getArguments())
                if(arg instanceof DccControlledFileDO row&&bpm.equals(row.getProcessInstanceId()))file=row;
            if(file==null){String[] parts=bpm.split(":");try{file=source.apply(Long.valueOf(parts[parts.length-1]));}catch(NumberFormatException failure){return null;}}
            if(file==null||!bpm.equals(file.getProcessInstanceId()))return null;
            var instance=mock(ProcessInstance.class);when(instance.getId()).thenReturn(bpm);when(instance.getTenantId()).thenReturn(file.getTenantId().toString());
            when(instance.getBusinessKey()).thenReturn(file.getId().toString());when(instance.getProcessDefinitionId()).thenReturn(id(key(file)));
            when(instance.getProcessDefinitionKey()).thenReturn(key(file));when(instance.getProcessVariables()).thenReturn(Map.of("controlledFileId",file.getId()));return instance;
        });
    }

    void prepareRoot(DccControlledFileDO root) {
        if(!"WORKING".equals(root.getStatus())||root.getProcessInstanceId()!=null)throw new IllegalArgumentException("unit root must be a real declared WORKING draft");
        contracts.prepareLegacy(root.getRequesterId(),root,id(key(root)));
    }
    void bindDeclaredProcess(DccControlledFileDO row) {
        row.setPreparedWorkflowContract(null);row.setPreparedProcessDefinitionId(null);row.setPreparedDefinitionDigest(null);
        row.setProcessInstanceId("declared-unit-bpm:"+row.getId());row.setProcessDefinitionKey(key(row));
    }
    String definitionId(DccControlledFileDO row){return id(key(row));}
    String definitionDigest(DccControlledFileDO row){return digest(key(row));}
    private DccControlledFileDO lookup(Long id){
        var live=source.apply(id);if(live!=null)return live;
        return mockingDetails(consumer).getInvocations().stream().filter(call->call.getMethod().getName().equals("insert"))
                .map(call->call.getArgument(0)).filter(DccControlledFileDO.class::isInstance).map(DccControlledFileDO.class::cast)
                .filter(row->Objects.equals(id,row.getId())).reduce((first,last)->last).orElse(null);
    }
    private String key(DccControlledFileDO file){if("NEW".equals(file.getChangeType()))return DccControlledFileProcessDefinitionKeys.UPLOAD;
        if("REVISION".equals(file.getChangeType()))return DccControlledFileProcessDefinitionKeys.REVISION;throw new IllegalArgumentException("explicit fixture action missing");}
    private String id(String key){return key+":declared-unit-definition";}
    private String keyForId(String id){return models.keySet().stream().filter(key->id(key).equals(id)).findFirst().orElse(null);}
    private String digest(String key){return DccLifecycleEffectiveContractResolver.definitionDigest(models.get(key));}
    private static void wire(Object target,Object... values){for(int i=0;i<values.length;i+=2)ReflectionTestUtils.setField(target,(String)values[i],values[i+1]);}
}
