package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.SimpleParserPublishReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.craft.MesProCraftDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamDeviceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.*;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.craft.MesProCraftMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamDeviceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.*;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import cn.iocoder.yudao.module.mes.service.pro.craft.MesProCraftService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.service.pro.route.*;
import com.alibaba.fastjson.*;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MesSimpleParserPublishServiceTest {
    private JdbcTemplate jdbc;
    private MesSimpleParserPublishService service;
    private MesProProcessService processes;
    private MesProCraftService crafts;
    private MesTeamLeaderRuntimeConfigService devices;
    private MesProRouteService routes;
    private MesProRouteCandidateConfigService configs;
    private MesProcessPoolTeamDeviceMapper deviceMapper;
    private MesDvMachineryMapper machineryMapper;
    private MesProProcessMapper processMapper;
    private MesProCraftMapper craftMapper;
    private MesMdItemMapper itemMapper;
    private MockedStatic<SecurityFrameworkUtils> security;
    private final Map<Long,MesProProcessDO> createdProcesses=new HashMap<>();

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        security=mockStatic(SecurityFrameworkUtils.class); security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(7L);
        var dataSource=new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");
        jdbc=new JdbcTemplate(dataSource);
        jdbc.execute("create table mes_simple_parser_publish_receipt (tenant_id bigint, user_id bigint, request_id varchar(36), request_hash varchar(64), result_json clob, primary key(tenant_id,user_id,request_id))");
        jdbc.execute("create table created_resource (kind varchar(30), id bigint)");
        processes=mock(MesProProcessService.class);crafts=mock(MesProCraftService.class);devices=mock(MesTeamLeaderRuntimeConfigService.class);
        routes=mock(MesProRouteService.class);configs=mock(MesProRouteCandidateConfigService.class);
        deviceMapper=mock(MesProcessPoolTeamDeviceMapper.class);processMapper=mock(MesProProcessMapper.class);
        machineryMapper=mock(MesDvMachineryMapper.class);
        when(machineryMapper.selectList(any())).thenReturn(List.of());
        craftMapper=mock(MesProCraftMapper.class);itemMapper=mock(MesMdItemMapper.class);
        var routeMapper=mock(MesProRouteMapper.class);var versionMapper=mock(MesProRouteVersionMapper.class);
        var workflow=mock(MesProRouteVersionWorkflowService.class);
        when(routeMapper.selectList(any())).thenReturn(List.of());when(deviceMapper.selectList(any())).thenReturn(List.of());
        when(processes.createProcess(any())).thenAnswer(call -> {
            Long id=100L+createdProcesses.size();var req=(cn.iocoder.yudao.module.mes.controller.admin.pro.process.vo.MesProProcessCreateReqVO)call.getArgument(0);
            createdProcesses.put(id,MesProProcessDO.builder().id(id).name(req.getName()).code("GX-"+id).status(0).build());
            jdbc.update("insert into created_resource values('PROCESS',?)",id);return id;
        });
        when(processMapper.selectById(anyLong())).thenAnswer(call -> createdProcesses.get(call.getArgument(0)));
        when(devices.createDevice(any())).thenAnswer(call -> {jdbc.update("insert into created_resource values('DEVICE',50)");return 50L;});
        when(crafts.create(any())).thenAnswer(call -> {
            var req=(cn.iocoder.yudao.module.mes.controller.admin.pro.craft.vo.MesProCraftSaveReqVO)call.getArgument(0);
            var craft=new MesProCraftDO();craft.setId(req.getProcessId()+1000);craft.setCode("GY-"+craft.getId());craft.setName(req.getName());
            craft.setProcessId(req.getProcessId());craft.setParametersJson(req.getParameters().toJSONString());
            jdbc.update("insert into created_resource values('CRAFT',?)",craft.getId());return craft;
        });
        when(routes.createRouteWithProcesses(any())).thenAnswer(call -> {jdbc.update("insert into created_resource values('ROUTE',9)");return 9L;});
        when(routeMapper.selectById(9L)).thenReturn(MesProRouteDO.builder().id(9L).code("GYLC-0009").status(1).build());
        var active=new MesProRouteVersionDO();active.setId(20L);active.setVersionNo("V1");when(versionMapper.selectActiveByRouteId(9L)).thenReturn(active);
        var candidate=new MesProRouteVersionDO();candidate.setId(21L);candidate.setVersionNo("V2");candidate.setRouteSnapshotSha256("snapshot");
        candidate.setRouteSnapshotJson("{\"configSnapshots\":{\"flowGraph\":{\"nodes\":[{\"routeProcessId\":200,\"processId\":100,\"sort\":1},{\"routeProcessId\":201,\"processId\":101,\"sort\":2}]}}}");
        when(workflow.createCandidate(any())).thenReturn(candidate);
        var target=new MesSimpleParserPublishService();
        Map<String,Object> fields=new HashMap<>();fields.put("processService",processes);fields.put("craftService",crafts);fields.put("deviceService",devices);
        fields.put("routeService",routes);fields.put("candidateConfig",configs);fields.put("processMapper",processMapper);fields.put("deviceMapper",deviceMapper);
        fields.put("routeMapper",routeMapper);fields.put("versionMapper",versionMapper);fields.put("workflow",workflow);fields.put("jdbc",jdbc);
        fields.put("itemMapper",itemMapper);fields.put("craftMapper",craftMapper);
        fields.put("machineryMapper",machineryMapper);
        fields.forEach((name,value)->ReflectionTestUtils.setField(target,name,value));
        ProxyFactory proxy=new ProxyFactory(target);proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(dataSource),new AnnotationTransactionAttributeSource()));
        service=(MesSimpleParserPublishService)proxy.getProxy();
    }
    @AfterEach void close(){security.close();TenantContextHolder.clear();}
    private SimpleParserPublishReqVO request() {
        var req=new SimpleParserPublishReqVO();req.setRequestId(UUID.randomUUID().toString());req.setRouteName("新路线");
        JSONObject process=JSON.parseObject("""
          {"name":"同名工序","criticalProcess":true,"inputs":[],"outputs":[],"equipmentGroups":[
            {"selectionMode":"SINGLE","equipmentOptions":[{"code":"EQ-1","name":"清洗设备"}],
              "parameters":[{"name":"清洗功率","referenceValue":"20至30","ui":{"control":"number","defaultValue":25,"min":20,"max":30,"step":1}}]}]}
          """);
        req.setRecognitionJson(JSON.parseObject(JSON.toJSONString(Map.of("schemaVersion",3,"product",Map.of("name","产品","code","IDI-01"),"processes",List.of(process,JSON.parseObject(process.toJSONString()))))));
        return req;
    }
    private int count(String table){return jdbc.queryForObject("select count(*) from "+table,Integer.class);}
    @Test void ledgerDeviceIsReusedAndOnlyExecutionBindingIsCreatedOnce() {
        var ledger=MesDvMachineryDO.builder().id(980126L).code("EQ-1").name("台账名称").build();
        when(machineryMapper.selectList(any())).thenReturn(List.of(ledger));
        var req=request();
        var preview=service.preview(req);
        var row=preview.getJSONArray("deviceRows").getJSONObject(0);
        assertEquals("REUSE",row.getString("action"));
        assertEquals(980126L,row.getLong("machineryId"));
        assertEquals("台账名称",row.getString("name"));
        assertNull(row.getLong("id"));
        assertEquals(0,count("created_resource"));
        req.setPreviewToken(preview.getString("previewToken"));
        var result=service.publish(req); service.publish(req);
        verify(devices,times(1)).createDevice(argThat(r -> r.getDeviceCode().equals("EQ-1") && r.getDeviceName().equals("台账名称")));
        assertEquals("REUSE",result.getJSONArray("deviceRows").getJSONObject(0).getString("action"));
        var capture=org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(configs).saveConfigSnapshots(eq(21L),eq("snapshot"),capture.capture());
        JSONObject saved=(JSONObject)JSON.toJSON(capture.getValue());
        assertEquals(50L,saved.getJSONArray("productionProcessConfigs").getJSONObject(0)
                .getJSONArray("deviceSelectionGroups").getJSONObject(0).getJSONArray("deviceIds").getLong(0));
        verify(machineryMapper,never()).insert(any(MesDvMachineryDO.class));
    }
    @Test void catalogDeduplicatesByCodeAndUsesLedgerNameWithoutLeaderFiltering() {
        when(machineryMapper.selectList(any())).thenReturn(List.of(MesDvMachineryDO.builder().id(9L).code("EQ-1").name("台账名称").build()));
        when(deviceMapper.selectList(any())).thenReturn(List.of(MesProcessPoolTeamDeviceDO.builder().id(50L)
                .leaderUserId(99L).deviceCode("EQ-1").deviceName("旧名称").enabled(true).deviceStatus("ENABLED").build()));
        var catalog=service.listDevices();
        assertEquals(1,catalog.size()); assertEquals("台账名称",catalog.get(0).getString("deviceName"));
        assertTrue(catalog.get(0).getBooleanValue("enabled"));
        var req=request(); req.setPreviewToken(service.preview(req).getString("previewToken"));service.publish(req);
        verifyNoInteractions(devices);
    }
    @Test void changedOrDuplicateLedgerIdentityCannotBePublished() {
        var ledger=MesDvMachineryDO.builder().id(9L).code("EQ-1").name("原名称").build();
        when(machineryMapper.selectList(any())).thenReturn(List.of(ledger));
        var req=request(); req.setPreviewToken(service.preview(req).getString("previewToken"));
        ledger.setName("新名称");
        assertThrows(ServiceException.class,()->service.publish(req));
        verifyNoInteractions(devices,processes,routes);
        when(machineryMapper.selectList(any())).thenReturn(List.of(ledger,ledger));
        assertThrows(ServiceException.class,()->service.listDevices());
        assertThrows(ServiceException.class,()->service.preview(request()));
    }
    @Test void disabledExistingDeviceRemainsKnownAndCannotBeRecreated() {
        when(deviceMapper.selectList(any())).thenReturn(List.of(MesProcessPoolTeamDeviceDO.builder().id(50L)
                .deviceCode("EQ-1").deviceName("清洗设备").enabled(false).deviceStatus("DISABLED").build()));
        assertFalse(service.listDevices().get(0).getBooleanValue("enabled"));
        assertThrows(ServiceException.class,()->service.preview(request()));
        verifyNoInteractions(devices);
    }
    @Test void previewDoesNotWriteAndSameNameProcessesRemainNew() {
        var preview=service.preview(request());
        assertEquals(2,preview.getJSONArray("processRows").size());
        assertEquals("CREATE",preview.getJSONArray("processRows").getJSONObject(0).getString("action"));
        assertEquals(1,preview.getJSONArray("deviceRows").size());
        assertEquals(0,count("created_resource"));assertEquals(0,count("mes_simple_parser_publish_receipt"));
        verifyNoInteractions(processes,crafts,devices,routes,configs);
    }
    @Test void publishCreatesDistinctProcessesAndAtomicReceiptAndRetryReturnsSameRoute() {
        var req=request();
        var material=new cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO();material.setId(77L);material.setCode("MAT");material.setName("原料");material.setStatus(0);
        when(itemMapper.selectList(any())).thenReturn(List.of(material));
        ((JSONObject)req.getRecognitionJson()).getJSONArray("processes").getJSONObject(0).put("inputs",JSON.parseArray("[{\"code\":\"MAT\",\"name\":\"原料\"}]"));
        req.setPreviewToken(service.preview(req).getString("previewToken"));
        var result=service.publish(req);
        assertEquals(9L,result.getLong("routeId"));assertEquals("DRAFT",result.getString("candidateStatus"));
        assertEquals(6,count("created_resource"));assertEquals(1,count("mes_simple_parser_publish_receipt"));
        assertEquals(new MesProRouteSnapshotCanonicalizer().canonicalize(result.toJSONString()),new MesProRouteSnapshotCanonicalizer().canonicalize(service.publish(req).toJSONString()));verify(routes,times(1)).createRouteWithProcesses(any());
        var capture=org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(configs).saveConfigSnapshots(eq(21L),eq("snapshot"),capture.capture());
        JSONObject saved=(JSONObject)JSON.toJSON(capture.getValue());
        assertTrue(saved.getJSONObject("flowGraph").getJSONArray("nodes").getJSONObject(0).getBoolean("keyFlag"));
        JSONObject production=saved.getJSONArray("productionProcessConfigs").getJSONObject(0);
        assertNotNull(production.getLong("craftId"));assertEquals(25,production.getJSONArray("parameterRules").getJSONObject(0).getIntValue("defaultValue"));
        assertTrue(saved.getJSONArray("batchUseConfigs").getJSONObject(0).getJSONArray("formBindings").isEmpty());
        assertEquals(77L,saved.getJSONArray("batchUseConfigs").getJSONObject(0).getJSONArray("inputMaterialIds").getLong(0));
        req.setRouteName("改动内容");assertThrows(ServiceException.class,()->service.publish(req));
    }
    @Test void deviceOnlyGroupsRemainGroupsWithoutInventingCrafts() {
        var req=request();
        for (Object raw:((JSONObject)req.getRecognitionJson()).getJSONArray("processes"))
            ((JSONObject)raw).getJSONArray("equipmentGroups").getJSONObject(0).put("parameters",new JSONArray());
        var preview=service.preview(req);assertTrue(preview.getJSONArray("craftRows").isEmpty());
        req.setPreviewToken(preview.getString("previewToken"));service.publish(req);
        verifyNoInteractions(crafts);assertEquals(4,count("created_resource"));
        var capture=org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(configs).saveConfigSnapshots(eq(21L),eq("snapshot"),capture.capture());
        JSONObject config=((JSONObject)JSON.toJSON(capture.getValue())).getJSONArray("productionProcessConfigs").getJSONObject(0);
        assertEquals(1,config.getJSONArray("deviceSelectionGroups").size());assertTrue(config.getJSONArray("parameterRules").isEmpty());
    }
    @Test void previewRecoversCommittedReceiptWithoutResolvingAlreadyCreatedResources() {
        var req=request();req.setPreviewToken(service.preview(req).getString("previewToken"));
        var published=service.publish(req);
        // 页面刷新后没有确认 token，但保留请求 UUID 与原始校对内容。
        req.setPreviewToken(null);
        clearInvocations(processes,crafts,devices,routes,configs,deviceMapper,itemMapper,processMapper,craftMapper);
        when(deviceMapper.selectList(any())).thenThrow(new IllegalStateException("已完成回执不应重新解析目录"));
        var recovered=service.preview(req);
        assertEquals(new MesProRouteSnapshotCanonicalizer().canonicalize(published.toJSONString()),
                new MesProRouteSnapshotCanonicalizer().canonicalize(recovered.toJSONString()));
        assertEquals(6,count("created_resource"));assertEquals(1,count("mes_simple_parser_publish_receipt"));
        verifyNoInteractions(processes,crafts,devices,routes,configs,deviceMapper,itemMapper,processMapper,craftMapper);
        req.setRouteName("不同内容");assertThrows(ServiceException.class,()->service.preview(req));
        req.setRouteName("新路线");
        security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(8L);
        assertThrows(IllegalStateException.class,()->service.preview(req),"其他用户不能读取本用户回执");
        security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(7L);
        TenantContextHolder.setTenantId(2L);
        assertThrows(IllegalStateException.class,()->service.preview(req),"其他租户不能读取本租户回执");
    }
    @Test void downstreamFailureRollsBackResourcesAndReceipt() {
        var req=request();req.setPreviewToken(service.preview(req).getString("previewToken"));
        doThrow(new IllegalStateException("candidate save failed")).when(configs).saveConfigSnapshots(anyLong(),anyString(),anyMap());
        assertThrows(IllegalStateException.class,()->service.publish(req));
        assertEquals(0,count("created_resource"));assertEquals(0,count("mes_simple_parser_publish_receipt"));
    }
    @Test void missingNumberAndChangedReusedDeviceFailBeforeBusinessWrites() {
        var req=request();var processesJson=((JSONObject)req.getRecognitionJson()).getJSONArray("processes");
        processesJson.getJSONObject(0).getJSONArray("equipmentGroups").getJSONObject(0).getJSONArray("equipmentOptions").getJSONObject(0).put("code","");
        var invalidRequest=req;assertThrows(ServiceException.class,()->service.preview(invalidRequest));
        req=request();var existing=MesProcessPoolTeamDeviceDO.builder().id(1L).deviceCode("EQ-1").deviceName("清洗设备").enabled(true).deviceStatus("ENABLED").leaderUserId(4L).build();
        when(deviceMapper.selectList(any())).thenReturn(List.of(existing));req.setPreviewToken(service.preview(req).getString("previewToken"));
        existing.setLeaderUserId(5L);var finalReq=req;
        assertThrows(ServiceException.class,()->service.publish(finalReq));assertEquals(0,count("created_resource"));assertEquals(0,count("mes_simple_parser_publish_receipt"));
    }
    @Test void reuseIsExplicitAndCraftMustMatchSelectedProcessAndExactParameters() {
        var req=request();
        var selected=new SimpleParserPublishReqVO.ProcessSelection();selected.setProcessIndex(0);selected.setProcessId(333L);
        var chosen=new SimpleParserPublishReqVO.CraftSelection();chosen.setGroupIndex(0);chosen.setCraftId(90L);
        selected.setCraftSelections(List.of(chosen));req.setProcessSelections(List.of(selected));
        when(processMapper.selectOne(any())).thenReturn(MesProProcessDO.builder().id(333L).code("GX-0333").name("同名工序").status(0).build());
        var craft=new MesProCraftDO();craft.setId(90L);craft.setProcessId(333L);craft.setName("现有工艺");craft.setCode("GY-0090");
        JSONArray definitions=MesSimpleParserPublishService.parameters(((JSONObject)req.getRecognitionJson()).getJSONArray("processes").getJSONObject(0).getJSONArray("equipmentGroups").getJSONObject(0).getJSONArray("parameters"));
        definitions.getJSONObject(0).put("parameterCode","EXISTING_CODE");craft.setParametersJson(definitions.toJSONString());
        when(craftMapper.selectOne(any())).thenReturn(craft);
        var preview=service.preview(req);
        assertEquals("REUSE",preview.getJSONArray("craftRows").getJSONObject(0).getString("action"));
        assertEquals("CREATE",preview.getJSONArray("processRows").getJSONObject(1).getString("action"));
        craft.setProcessId(334L);assertThrows(ServiceException.class,()->service.preview(req));craft.setProcessId(333L);
        definitions.getJSONObject(0).put("lowerLimit",19);craft.setParametersJson(definitions.toJSONString());
        assertThrows(ServiceException.class,()->service.preview(req));assertEquals(0,count("created_resource"));
    }
    @Test void directoryCodesResolveAliasesWithoutMutatingRecognizedInput() {
        var req=request();
        var source=((JSONObject)req.getRecognitionJson()).getJSONArray("processes");
        source.getJSONObject(0).getJSONArray("equipmentGroups").getJSONObject(0).getJSONArray("equipmentOptions").getJSONObject(0).put("name","Word 设备别名甲");
        source.getJSONObject(1).getJSONArray("equipmentGroups").getJSONObject(0).getJSONArray("equipmentOptions").getJSONObject(0).put("name","Word 设备别名乙");
        var existing=MesProcessPoolTeamDeviceDO.builder().id(50L).deviceCode("EQ-1").deviceName("目录清洗设备").enabled(true).deviceStatus("ENABLED").build();
        when(deviceMapper.selectList(any())).thenReturn(List.of(existing));
        var material=new cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO();material.setId(77L);material.setCode("A001.02.033.106");material.setName("压力表");material.setStatus(0);
        when(itemMapper.selectList(any())).thenReturn(List.of(material));
        source.getJSONObject(0).put("inputs",JSON.parseArray("[{\"code\":\"A001.02.033.106\",\"name\":\"30atm压力表\"}]"));
        String before=JSON.toJSONString(req.getRecognitionJson());
        var preview=service.preview(req);
        assertEquals(before,JSON.toJSONString(req.getRecognitionJson()));
        assertEquals(1,preview.getJSONArray("deviceRows").size());assertEquals("目录清洗设备",preview.getJSONArray("deviceRows").getJSONObject(0).getString("name"));
        assertEquals("压力表",preview.getJSONArray("materialRows").getJSONObject(0).getString("name"));
        assertEquals("30atm压力表",preview.getJSONArray("materialRows").getJSONObject(0).getString("sourceName"));
        assertEquals(3,preview.getJSONArray("warnings").size());
        req.setPreviewToken(preview.getString("previewToken"));var published=service.publish(req);
        assertEquals("目录清洗设备",published.getJSONArray("devices").getJSONObject(0).getString("name"));verifyNoInteractions(devices);
        assertEquals(before,JSON.toJSONString(req.getRecognitionJson()));
    }
    @Test void ambiguousCodesAndUnnumberedMaterialAliasesAreNotAutoResolved() {
        var req=request();var device=MesProcessPoolTeamDeviceDO.builder().id(50L).deviceCode("EQ-1").deviceName("清洗设备").enabled(true).deviceStatus("ENABLED").build();
        when(deviceMapper.selectList(any())).thenReturn(List.of(device,device));
        assertThrows(ServiceException.class,()->service.preview(req));when(deviceMapper.selectList(any())).thenReturn(List.of(device));
        var source=((JSONObject)req.getRecognitionJson()).getJSONArray("processes");
        source.getJSONObject(0).put("inputs",JSON.parseArray("[{\"code\":\"MAT\",\"name\":\"别名\"}]"));
        var item=new cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO();item.setId(1L);item.setStatus(0);item.setName("目录名");item.setCode("MAT");
        when(itemMapper.selectList(any())).thenReturn(List.of(item,item));assertEquals(1,service.preview(req).getJSONArray("blockers").size());
        source.getJSONObject(0).getJSONArray("inputs").getJSONObject(0).remove("code");
        when(itemMapper.selectList(any())).thenReturn(List.of(item));assertEquals(1,service.preview(req).getJSONArray("blockers").size());
        assertEquals(0,count("created_resource"));
    }
    @Test void unresolvedMaterialsAreAllListedAndPreventEveryBusinessWrite() {
        var req=request();var source=((JSONObject)req.getRecognitionJson()).getJSONArray("processes");
        source.getJSONObject(0).put("inputs",JSON.parseArray("""
                [{"code":"MISSING","name":"缺失原料"},{"code":"DUP","name":"重复原料"},{"code":"OK","name":"识别别名"}]
                """));
        source.getJSONObject(1).put("outputs",JSON.parseArray("""
                [{"code":"OFF","name":"停用产物"},{"name":"非精确名称"},{}]
                """));
        var enabled=new cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO();
        enabled.setId(77L);enabled.setCode("OK");enabled.setName("目录原料");enabled.setStatus(0);
        var disabled=new cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO();
        disabled.setId(78L);disabled.setCode("OFF");disabled.setName("停用产物");disabled.setStatus(1);
        when(itemMapper.selectList(any())).thenReturn(List.of(),List.of(enabled,enabled),List.of(enabled),List.of(disabled),List.of(enabled),
                List.of(),List.of(enabled,enabled),List.of(enabled),List.of(disabled),List.of(enabled));
        String original=JSON.toJSONString(req.getRecognitionJson());
        var preview=service.preview(req);var blockers=preview.getJSONArray("blockers");
        assertEquals(5,blockers.size());
        assertTrue(blockers.getString(0).contains("工序 1（同名工序）输入物料（编码：MISSING，名称：缺失原料）"));
        assertTrue(blockers.getString(1).contains("目录匹配不唯一"));
        assertTrue(blockers.getString(2).contains("工序 2（同名工序）输出物料（编码：OFF，名称：停用产物）"));
        assertTrue(blockers.getString(2).contains("未启用"));
        assertTrue(blockers.getString(3).contains("精确匹配"));
        assertTrue(blockers.getString(4).contains("未提供编码或名称"));
        assertEquals(2,preview.getJSONArray("processRows").size());
        assertEquals(2,preview.getJSONArray("craftRows").size());
        assertEquals(1,preview.getJSONArray("deviceRows").size());
        var materials=preview.getJSONArray("materialRows");assertEquals(1,materials.size());
        assertEquals(77L,materials.getJSONObject(0).getLong("id"));
        assertEquals("目录原料",materials.getJSONObject(0).getString("name"));
        assertEquals(2,materials.getJSONObject(0).getIntValue("index"));
        assertEquals(original,JSON.toJSONString(req.getRecognitionJson()));
        assertEquals(0,count("created_resource"));assertEquals(0,count("mes_simple_parser_publish_receipt"));
        verifyNoInteractions(processes,crafts,devices,routes,configs);
        req.setPreviewToken(preview.getString("previewToken"));
        var error=assertThrows(ServiceException.class,()->service.publish(req));
        assertTrue(error.getMessage().contains("缺失原料"));assertTrue(error.getMessage().contains("停用产物"));
        verifyNoInteractions(processes,crafts,devices,routes,configs);
        assertEquals(0,count("created_resource"));assertEquals(0,count("mes_simple_parser_publish_receipt"));
    }
    @Test void parameterConversionPreservesZeroRangesAndRejectsUnrepresentableStep() {
        var input=JSON.parseArray("""
           [{"name":"压力","referenceValue":"0-1","ui":{"control":"number","defaultValue":0,"min":0,"max":1,"step":0.1}}]
           """);
        var parameters=MesSimpleParserPublishService.parameters(input);
        assertEquals(0,parameters.getJSONObject(0).getIntValue("defaultValue"));assertEquals(1,parameters.getJSONObject(0).getIntValue("decimalScale"));
        input.getJSONObject(0).getJSONObject("ui").put("step",0.5);
        assertThrows(ServiceException.class,()->MesSimpleParserPublishService.parameters(input));
    }
}
