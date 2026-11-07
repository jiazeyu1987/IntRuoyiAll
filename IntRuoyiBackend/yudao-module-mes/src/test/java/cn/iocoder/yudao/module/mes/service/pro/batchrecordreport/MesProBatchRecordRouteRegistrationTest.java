package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteControlledContentAdapter;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MesProBatchRecordRouteRegistrationTest {
    private final MesProBatchRecordRouteGenerationServiceImpl service = new MesProBatchRecordRouteGenerationServiceImpl();
    private final MesProRouteVersionMapper versions = mock(MesProRouteVersionMapper.class);
    private final MesProRouteControlledContentAdapter adapter = mock(MesProRouteControlledContentAdapter.class);
    private final MesProRouteDO route = MesProRouteDO.builder().id(42L).code("TEST").name("项目").status(0).build();
    private final MesProRouteVersionDO active = MesProRouteVersionDO.builder()
            .id(100L).routeId(42L).versionNo("V1").active(true).lifecycleStatus("ACTIVE").build();

    MesProBatchRecordRouteRegistrationTest() {
        ReflectionTestUtils.setField(service, "routeVersionMapper", versions);
        ReflectionTestUtils.setField(service, "routeControlledContentAdapter", adapter);
        ReflectionTestUtils.setField(service, "routeProductMapper", mock(MesProRouteProductMapper.class));
        ReflectionTestUtils.setField(service, "processMapper", mock(MesProProcessMapper.class));
        MesProRouteService routes = mock(MesProRouteService.class);
        ReflectionTestUtils.setField(service, "routeService", routes);
        when(routes.buildCurrentRouteSnapshotJson(42L, 100L)).thenReturn("""
                {"configSnapshots":{"flowGraph":{"nodes":[]},"batchUseConfigs":[],
                "productionProcessConfigSchemaVersion":1,"productionProcessConfigs":[],
                "routeStartProductionLeaders":[],"batchRecordAttachmentOwners":[]}}
                """);
        when(versions.selectActiveByRouteIdForUpdate(42L)).thenReturn(active);
        when(versions.selectListByRouteId(42L)).thenReturn(List.of(active));
        when(versions.insert(any(MesProRouteVersionDO.class))).thenAnswer(call -> {
            call.<MesProRouteVersionDO>getArgument(0).setId(101L);
            return 1;
        });
    }

    @Test
    void wordInitialVersionRegistersActiveReference() {
        MesProRouteVersionDO created = ReflectionTestUtils.invokeMethod(service,
                "createInitialActiveRouteVersionForUploadedWord", route, null);
        assertNotNull(created);
        assertTrue(created.getActive());
        verify(adapter).recordActiveRegistered(eq(created), isNull(), anyString());
    }

    @Test
    void wordNewCandidateRegistersActiveAndCandidateInOrder() {
        MesProRouteVersionDO candidate = createCandidate(null);
        assertNotNull(candidate);
        assertEquals("DRAFT", candidate.getLifecycleStatus());
        assertEquals(active.getId(), candidate.getSourceRouteVersionId());
        var order = inOrder(adapter);
        order.verify(adapter).recordActiveRegisteredIfMissing(eq(active), isNull(), anyString());
        order.verify(adapter).recordDraftCandidateRegisteredIfMissing(eq(active), eq(candidate), isNull(), anyString());
    }

    @Test
    void wordExistingDraftRegistersMissingReferencesWithoutCreatingAnotherVersion() {
        MesProRouteVersionDO existing = MesProRouteVersionDO.builder().id(101L).routeId(42L)
                .versionNo("V2").active(false).lifecycleStatus("DRAFT").sourceRouteVersionId(100L).build();
        when(versions.selectOpenCandidateByRouteId(42L)).thenReturn(existing);
        assertSame(existing, createCandidate(101L));
        verify(versions, never()).insert(any(MesProRouteVersionDO.class));
        verify(adapter).recordDraftCandidateRegisteredIfMissing(eq(active), eq(existing), isNull(), anyString());
    }

    @Test
    void controlledContentRegistrationFailureIsNotSwallowed() {
        doThrow(new IllegalStateException("registration failed")).when(adapter)
                .recordActiveRegisteredIfMissing(any(), any(), anyString());
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> createCandidate(null));
        assertEquals("registration failed", error.getMessage());
        verify(adapter, never()).recordDraftCandidateRegisteredIfMissing(any(), any(), any(), anyString());
    }

    @Test
    void wordCandidateKeepsProductionSettingsWithoutChangingTheActiveSnapshot() {
        JSONObject source = JSON.parseObject("""
                {"productionProcessConfigSchemaVersion":1,"productionProcessConfigs":[{
                "routeProcessId":11,"processId":22,"sort":1,"overagePercent":7.5,
                "lossReasons":[{"reasonCode":"LOSS"}],
                "deviceSelectionGroups":[{"deviceIds":[55]}],
                "parameterRules":[{"deviceId":55,"standardText":"original"}]}]}
                """);
        String before = source.toJSONString();
        JSONObject target = new JSONObject();
        ReflectionTestUtils.invokeMethod(service, "addProductionProcessSnapshots", target,
                List.of(Map.of("routeProcessId", 11L, "processId", 22L,
                        "sort", 2, "processName", "清洗")), source);
        JSONObject config = target.getJSONArray("productionProcessConfigs").getJSONObject(0);
        assertEquals(1, target.getInteger("productionProcessConfigSchemaVersion"));
        assertEquals(2, config.getInteger("sort"));
        assertEquals("7.5", config.getBigDecimal("overagePercent").toPlainString());
        for (String key : List.of("lossReasons", "deviceSelectionGroups", "parameterRules")) {
            assertEquals(source.getJSONArray("productionProcessConfigs").getJSONObject(0).get(key), config.get(key));
        }
        config.getJSONArray("parameterRules").getJSONObject(0).put("standardText", "changed");
        assertEquals(before, source.toJSONString());
    }

    @Test
    void wordCandidateRejectsMissingExistingProductionSettings() {
        JSONObject source = JSON.parseObject("""
                {"productionProcessConfigSchemaVersion":1,"productionProcessConfigs":[]}
                """);
        assertThrows(RuntimeException.class, () -> ReflectionTestUtils.invokeMethod(service,
                "addProductionProcessSnapshots", new JSONObject(),
                List.of(Map.of("routeProcessId", 11L, "processId", 22L)), source));
    }

    @Test
    void firstWordRouteHasExplicitInitialProductionSettings() {
        JSONObject target = new JSONObject();
        ReflectionTestUtils.invokeMethod(service, "addProductionProcessSnapshots", target,
                List.of(Map.of("routeProcessId", 11L, "processId", 22L,
                        "sort", 1, "processName", "清洗")), null);
        JSONObject config = target.getJSONArray("productionProcessConfigs").getJSONObject(0);
        assertEquals(1, target.getInteger("productionProcessConfigSchemaVersion"));
        assertEquals(11L, config.getLong("routeProcessId"));
        assertEquals(java.math.BigDecimal.ZERO, config.getBigDecimal("overagePercent"));
        for (String key : List.of("lossReasons", "deviceSelectionGroups", "parameterRules")) {
            assertTrue(config.getJSONArray(key).isEmpty());
        }
    }

    private MesProRouteVersionDO createCandidate(Long expectedCandidateId) {
        return ReflectionTestUtils.invokeMethod(service, "createOrUpdateCandidateRouteVersion",
                route, active, List.of(), List.of("项目"), false, expectedCandidateId);
    }
}
