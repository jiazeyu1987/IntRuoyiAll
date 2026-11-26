package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesRouteDccProjectBindingDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamDeviceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowEdgeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowBoundaryEdgeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesRouteDccProjectBindingMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordParsedTable;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportView;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordRecognitionDeviceSyncService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordRouteGenerationServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.craft.MesProCraftService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.DccProjectResolver;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessCreationService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MesProBatchRecordRouteSnapshotTest {

    private final MesProBatchRecordRouteGenerationServiceImpl generation =
            new MesProBatchRecordRouteGenerationServiceImpl();
    private final MesProRouteVersionMapper versions = mock(MesProRouteVersionMapper.class);
    private final MesProRouteService routeService = mock(MesProRouteService.class);
    private final MesProProcessMapper processes = mock(MesProProcessMapper.class);
    private final MesProRouteDO route = MesProRouteDO.builder().id(1L).code("R1").name("Route").status(0).build();
    private final MesProRouteVersionDO active = MesProRouteVersionDO.builder()
            .id(2L).routeId(1L).versionNo("V1").active(true).lifecycleStatus("ACTIVE").build();

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(generation, "routeVersionMapper", versions);
        ReflectionTestUtils.setField(generation, "routeService", routeService);
        ReflectionTestUtils.setField(generation, "processMapper", processes);
        ReflectionTestUtils.setField(generation, "routeProductMapper", mock(MesProRouteProductMapper.class));
        ReflectionTestUtils.setField(generation, "routeProcessFlowEdgeMapper", mock(MesProRouteProcessFlowEdgeMapper.class));
        ReflectionTestUtils.setField(generation, "routeProcessFlowBoundaryEdgeMapper", mock(MesProRouteProcessFlowBoundaryEdgeMapper.class));
        ReflectionTestUtils.setField(generation, "deviceIdentitySnapshots", mock(MesProRouteDeviceIdentitySnapshotService.class));
        ReflectionTestUtils.setField(generation, "routeControlledContentAdapter", mock(MesProRouteControlledContentAdapter.class));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "/frozen/sop.pdf")
    void newRoutePersistsExplicitSopAndPassesCompleteSnapshotValidation(String sop) throws Exception {
        JSONObject generated = ReflectionTestUtils.invokeMethod(generation, "buildGeneratedRouteSnapshot",
                route, List.of(MesProRouteProcessDO.builder().id(101L).routeId(1L).processId(11L).sort(1).build()),
                List.of(MesProProcessDO.builder().id(11L).sopFileUrl(sop).build()),
                List.of(binding("Process")), List.of("Product"), false, Map.of());

        ReflectionTestUtils.invokeMethod(generation, "updateRouteVersionSnapshot", active, generated);

        ArgumentCaptor<MesProRouteVersionDO> saved = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(versions).updateById(saved.capture());
        assertComplete(saved.getValue(), sop);
        assertEquals(saved.getValue().getRouteSnapshotJson(), active.getRouteSnapshotJson());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void rebuildCandidatePreservesFrozenSopAndRecordsUnboundNewProcess(boolean updating) throws Exception {
        String frozenSop = "/frozen/old-sop.pdf";
        when(routeService.buildCurrentRouteSnapshotJson(1L, 2L)).thenReturn(baseline(frozenSop));
        // The existing node must retain its frozen value, even when the current definition differs.
        when(processes.selectBatchIds(List.of(11L))).thenReturn(List.of(
                MesProProcessDO.builder().id(11L).name("Process").sopFileUrl("/current/changed.pdf").build()));
        prepareCandidate(updating);
        MesProProcessCreationService creation = mock(MesProProcessCreationService.class);
        ReflectionTestUtils.setField(generation, "processCreationService", creation);
        DccProjectResolver resolver = mock(DccProjectResolver.class);
        ReflectionTestUtils.setField(generation, "projectCodeResolver", resolver);
        when(resolver.requireEnabledByRoute(1L)).thenReturn(new DccProjectResolver.ResolvedProject(9L, "P9", "Project"));
        when(creation.create(any(), any())).thenAnswer(call -> {
            MesProProcessDO process = call.getArgument(0);
            process.setId(22L);
            return 22L;
        });

        MesProRouteVersionDO candidate = ReflectionTestUtils.invokeMethod(generation,
                "createOrUpdateCandidateRouteVersion", route, active,
                List.of(binding("Process"), binding("New process")), List.of("Product"), false,
                updating ? 3L : null);

        MesProRouteVersionDO saved = captureCandidate(updating);
        assertComplete(saved, frozenSop);
        JSONObject newNode = nodes(saved).getJSONObject(1);
        assertTrue(newNode.containsKey("sopFileUrl"));
        assertNull(newNode.get("sopFileUrl"));
        assertEquals(22L, newNode.getLong("processId"));
        assertEquals(saved.getRouteSnapshotJson(), candidate.getRouteSnapshotJson());
        verify(creation).create(any(), any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void wordBindingCandidatePreservesUnboundSopAndIndependentFormBindings(boolean updating) {
        prepareCandidate(updating);
        MesProRouteMapper routes = mock(MesProRouteMapper.class);
        ReflectionTestUtils.setField(generation, "routeMapper", routes);
        when(routes.selectById(1L)).thenReturn(route);
        when(routes.selectByIdForUpdate(1L)).thenReturn(route);
        when(versions.selectActiveByRouteId(1L)).thenReturn(active);
        when(routeService.buildCurrentRouteSnapshotJson(1L, 2L)).thenReturn(baseline(null));
        DccProjectCodeMapper projects = mock(DccProjectCodeMapper.class);
        ReflectionTestUtils.setField(generation, "dccProjectCodeMapper", projects);
        when(projects.selectById(9L)).thenReturn(DccProjectCodeDO.builder().id(9L)
                .status(DccProjectCodeStatusConstants.ENABLE).build());
        MesRouteDccProjectBindingMapper bindings = mock(MesRouteDccProjectBindingMapper.class);
        ReflectionTestUtils.setField(generation, "routeDccProjectBindingMapper", bindings);
        when(bindings.selectCurrentByRouteId(1L)).thenReturn(MesRouteDccProjectBindingDO.builder()
                .routeId(1L).dccProjectCodeId(9L).build());

        generation.generateBatchRecordBindingCandidateForUploadedWord("Record", List.of(
                        MesProBatchRecordParsedTable.builder().tableTitle("产品信息").sourceTableIndex(0).build(),
                        MesProBatchRecordParsedTable.builder().tableTitle("Process").sourceTableIndex(1).build()),
                List.of(MesProBatchRecordReportView.builder().sourceTableIndex(1).reportId("report-new")
                        .batchRecordDefinitionId(7L).batchRecordVersionId(8L).formSlotType("MAIN").build()),
                7L, 8L, 1L, 2L, true, updating ? 3L : null, 9L);

        MesProRouteVersionDO saved = captureCandidate(updating);
        assertComplete(saved, null);
        JSONObject config = JSON.parseObject(saved.getRouteSnapshotJson()).getJSONObject("configSnapshots")
                .getJSONArray("batchUseConfigs").getJSONObject(0);
        assertEquals("report-new", config.getJSONArray("batchRecordReports").getJSONObject(0).getString("reportId"));
        assertEquals(701L, config.getJSONArray("formBindings").getJSONObject(0).getLong("formTemplateId"));
        verify(versions, never()).updateById(argThat((MesProRouteVersionDO value) -> Long.valueOf(2L).equals(value.getId())));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "/frozen/sop.pdf")
    void recognitionInitializationKeepsCompleteActiveSnapshot(String sop) {
        MesRouteDccProjectBindingMapper bindings = mock(MesRouteDccProjectBindingMapper.class);
        MesProBatchRecordRecognitionDeviceSyncService recognition = new MesProBatchRecordRecognitionDeviceSyncService(
                bindings, processes, mock(MesProcessPoolTeamDeviceMapper.class), versions,
                mock(MesProRouteCandidateConfigService.class));
        ReflectionTestUtils.setField(recognition, "craftService", mock(MesProCraftService.class));
        when(bindings.selectCurrentListByDccProjectCodeId(9L)).thenReturn(List.of(
                MesRouteDccProjectBindingDO.builder().routeId(1L).build()));
        active.setRouteSnapshotJson(baseline(sop));
        when(versions.selectActiveByRouteId(1L)).thenReturn(active);
        when(processes.selectById(11L)).thenReturn(MesProProcessDO.builder().id(11L).name("Process").build());

        recognition.initializeActiveVersionProductionConfigs(9L,
                "{\"schemaVersion\":3,\"processes\":[{\"routeProcessId\":101,\"equipmentGroups\":[]}]}");

        ArgumentCaptor<MesProRouteVersionDO> saved = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(versions).updateById(saved.capture());
        assertComplete(saved.getValue(), sop);
        assertEquals(2L, saved.getValue().getId());
    }

    private void prepareCandidate(boolean updating) {
        when(versions.selectActiveByRouteIdForUpdate(1L)).thenReturn(active);
        if (updating) {
            when(versions.selectOpenCandidateByRouteId(1L)).thenReturn(MesProRouteVersionDO.builder()
                    .id(3L).routeId(1L).sourceRouteVersionId(2L).versionNo("V2").lifecycleStatus("DRAFT")
                    .routeSnapshotJson(baseline(null)).build());
        } else {
            when(versions.selectListByRouteId(1L)).thenReturn(List.of(active));
            doAnswer(call -> {
                ((MesProRouteVersionDO) call.getArgument(0)).setId(3L);
                return 1;
            }).when(versions).insert(any(MesProRouteVersionDO.class));
        }
    }

    private MesProRouteVersionDO captureCandidate(boolean updating) {
        ArgumentCaptor<MesProRouteVersionDO> saved = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        if (updating) verify(versions).updateById(saved.capture());
        else verify(versions).insert(saved.capture());
        return saved.getValue();
    }

    private Object binding(String name) throws Exception {
        Class<?> type = Class.forName(MesProBatchRecordRouteGenerationServiceImpl.class.getName() + "$RouteProcessReportBinding");
        Constructor<?> constructor = type.getDeclaredConstructor(String.class, MesProBatchRecordReportView.class);
        constructor.setAccessible(true);
        return constructor.newInstance(name, null);
    }

    private void assertComplete(MesProRouteVersionDO version, String sop) {
        String json = version.getRouteSnapshotJson();
        assertTrue(MesProRouteVersionSnapshotValidator.hasCompleteConfigSnapshot(json), json);
        JSONObject node = nodes(version).getJSONObject(0);
        assertTrue(node.containsKey("sopFileUrl"));
        assertEquals(sop, node.get("sopFileUrl"));
        assertFalse(json.contains("\"$ref\""));
        assertEquals(new MesProRouteSnapshotCanonicalizer().sha256(json), version.getRouteSnapshotSha256());
        assertEquals(MesProRouteSnapshotCanonicalizer.FORMAT_VERSION, version.getRouteSnapshotFormatVersion());
    }

    private JSONArray nodes(MesProRouteVersionDO version) {
        return JSON.parseObject(version.getRouteSnapshotJson()).getJSONObject("configSnapshots")
                .getJSONObject("flowGraph").getJSONArray("nodes");
    }

    private String baseline(String sop) {
        JSONObject snapshot = JSON.parseObject("""
                {"routeId":1,"routeVersionId":2,"routeCode":"R1","routeName":"Route",
                 "configSnapshots":{"flowGraph":{"nodes":[
                   {"routeProcessId":101,"processId":11,"processName":"Process","sort":1,"sopFileUrl":null}]},
                  "products":[],"scheduleConfigs":[],"scheduleUseConfigs":[],
                  "batchUseConfigs":[{"routeProcessId":101,"processId":11,"sort":1,
                    "batchRecordReports":[],"formBindings":[{"routeProcessId":101,"formTemplateId":701}]}],
                  "productionProcessConfigSchemaVersion":1,"productionProcessConfigs":[
                    {"routeProcessId":101,"processId":11,"overagePercent":0,
                     "lossReasons":[],"deviceSelectionGroups":[],"parameterRules":[]}]}}
                """);
        snapshot.getJSONObject("configSnapshots").getJSONObject("flowGraph").getJSONArray("nodes")
                .getJSONObject(0).put("sopFileUrl", sop);
        String json = MesProRouteVersionSnapshotIdentityWriter.toSnapshotJson(snapshot);
        assertTrue(MesProRouteVersionSnapshotValidator.hasCompleteConfigSnapshot(json));
        return json;
    }
}
