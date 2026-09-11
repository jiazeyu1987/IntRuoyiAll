package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamDeviceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class MesProRouteCandidateConfigServiceTest {

    @InjectMocks
    private MesProRouteCandidateConfigServiceImpl service;

    @Mock
    private MesProRouteVersionMapper routeVersionMapper;

    @Mock
    private MesProcessPoolTeamDeviceMapper teamDeviceMapper;

    @BeforeEach
    void setUp() {
        lenient().when(routeVersionMapper.update(any(MesProRouteVersionDO.class), any())).thenReturn(1);
    }

    @Test
    void saveConfigSnapshot_shouldMergeIntoDraftCandidateWithoutActivatingIt() {
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(2002L)
                .routeId(9001L)
                .versionNo("V2")
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .sourceRouteVersionId(2001L)
                .routeSnapshotJson("{\"routeId\":9001,\"routeCode\":\"R-001\"}")
                .build();
        Map<String, Object> flowGraphSnapshot = Map.of(
                "graphVersion", 3L,
                "edges", List.of(Map.of("sourceRouteProcessId", 11L, "targetRouteProcessId", 12L))
        );
        when(routeVersionMapper.selectById(candidate.getId())).thenReturn(candidate);
        when(routeVersionMapper.selectActiveByRouteId(9001L)).thenReturn(MesProRouteVersionDO.builder()
                .id(2001L)
                .routeId(9001L)
                .versionNo("V1")
                .active(Boolean.TRUE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE)
                .build());

        service.saveConfigSnapshot(candidate.getId(), "flowGraph", flowGraphSnapshot);

        ArgumentCaptor<MesProRouteVersionDO> updateCaptor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper).update(updateCaptor.capture(), any());
        MesProRouteVersionDO update = updateCaptor.getValue();
        assertEquals(candidate.getId(), update.getId());
        assertFalse(Boolean.TRUE.equals(update.getActive()));
        JSONObject snapshot = JSON.parseObject(update.getRouteSnapshotJson());
        assertEquals(9001L, snapshot.getLongValue("routeId"));
        assertEquals("R-001", snapshot.getString("routeCode"));
        JSONObject configSnapshots = snapshot.getJSONObject("configSnapshots");
        assertTrue(configSnapshots.containsKey("flowGraph"));
        assertEquals(3L, configSnapshots.getJSONObject("flowGraph").getLongValue("graphVersion"));
    }

    @Test
    void saveConfigSnapshot_shouldRejectActiveVersion() {
        MesProRouteVersionDO active = MesProRouteVersionDO.builder()
                .id(2001L)
                .routeId(9001L)
                .versionNo("V1")
                .active(Boolean.TRUE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE)
                .routeSnapshotJson("{\"routeId\":9001}")
                .build();
        when(routeVersionMapper.selectById(active.getId())).thenReturn(active);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.saveConfigSnapshot(active.getId(), "flowGraph", Map.of("graphVersion", 4L)));

        assertEquals(ErrorCodeConstants.PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE.getCode(), ex.getCode());
        verify(routeVersionMapper, never()).update(any(MesProRouteVersionDO.class), any());
    }

    @Test
    void saveConfigSnapshots_shouldPersistProductionProcessSchemaAndConfigsInOneUpdate() {
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(2002L)
                .routeId(9001L)
                .versionNo("V2")
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .sourceRouteVersionId(2001L)
                .routeSnapshotJson("{\"routeId\":9001,\"routeCode\":\"R-001\",\"configSnapshots\":{\"flowGraph\":{\"nodes\":[{\"routeProcessId\":10,\"processId\":20}]}}}")
                .build();
        when(routeVersionMapper.selectById(candidate.getId())).thenReturn(candidate);
        when(routeVersionMapper.selectActiveByRouteId(9001L)).thenReturn(MesProRouteVersionDO.builder()
                .id(2001L)
                .routeId(9001L)
                .versionNo("V1")
                .active(Boolean.TRUE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE)
                .build());

        service.saveConfigSnapshots(candidate.getId(), Map.of(
                "productionProcessConfigSchemaVersion", 1,
                "productionProcessConfigs", List.of(Map.of(
                        "routeProcessId", 10L,
                        "processId", 20L,
                        "overagePercent", 10,
                        "lossReasons", List.of(),
                        "deviceSelectionGroups", List.of(),
                        "parameterRules", List.of()))));

        ArgumentCaptor<MesProRouteVersionDO> updateCaptor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper).update(updateCaptor.capture(), any());
        JSONObject configSnapshots = JSON.parseObject(updateCaptor.getValue().getRouteSnapshotJson())
                .getJSONObject("configSnapshots");
        assertEquals(1, configSnapshots.getIntValue("productionProcessConfigSchemaVersion"));
        assertEquals(1, configSnapshots.getJSONArray("productionProcessConfigs").size());
    }

    @Test
    void saveConfigSnapshot_shouldRejectWhenSourceActiveVersionDrifted() {
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(2002L)
                .routeId(9001L)
                .versionNo("V2")
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .sourceRouteVersionId(2001L)
                .routeSnapshotJson("{\"routeId\":9001,\"routeCode\":\"R-001\"}")
                .build();
        MesProRouteVersionDO currentActive = MesProRouteVersionDO.builder()
                .id(2003L)
                .routeId(9001L)
                .versionNo("V3")
                .active(Boolean.TRUE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE)
                .build();
        when(routeVersionMapper.selectById(candidate.getId())).thenReturn(candidate);
        when(routeVersionMapper.selectActiveByRouteId(9001L)).thenReturn(currentActive);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.saveConfigSnapshot(candidate.getId(), "flowGraph", Map.of("graphVersion", 5L)));

        assertEquals(ErrorCodeConstants.PRO_ROUTE_VERSION_CONFLICT.getCode(), ex.getCode());
        verify(routeVersionMapper, never()).update(any(MesProRouteVersionDO.class), any());
    }

    @Test
    void saveConfigSnapshots_shouldRejectStaleCandidateSnapshotHash() {
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(2002L)
                .routeId(9001L)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .sourceRouteVersionId(2001L)
                .routeSnapshotJson("{\"routeId\":9001,\"configSnapshots\":{}}")
                .routeSnapshotSha256("current-hash")
                .build();
        when(routeVersionMapper.selectById(candidate.getId())).thenReturn(candidate);
        when(routeVersionMapper.selectActiveByRouteId(9001L)).thenReturn(MesProRouteVersionDO.builder()
                .id(2001L).routeId(9001L).active(Boolean.TRUE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE).build());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.saveConfigSnapshots(
                candidate.getId(), "stale-hash", Map.of("productionProcessConfigs", List.of())));

        assertEquals(ErrorCodeConstants.PRO_ROUTE_VERSION_CONFLICT.getCode(), ex.getCode());
        verify(routeVersionMapper, never()).update(any(MesProRouteVersionDO.class), any());
    }

    @Test
    void saveConfigSnapshots_shouldRejectIncompleteProductionProcessConfig() {
        String snapshotJson = """
                {"routeId":9001,"configSnapshots":{"flowGraph":{"nodes":[
                  {"routeProcessId":10,"processId":20}
                ]}}}
                """;
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(2002L).routeId(9001L).active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .sourceRouteVersionId(2001L)
                .routeSnapshotJson(snapshotJson)
                .routeSnapshotSha256("current-hash")
                .build();
        when(routeVersionMapper.selectById(candidate.getId())).thenReturn(candidate);
        when(routeVersionMapper.selectActiveByRouteId(9001L)).thenReturn(MesProRouteVersionDO.builder()
                .id(2001L).routeId(9001L).active(Boolean.TRUE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE).build());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.saveConfigSnapshots(
                candidate.getId(), Map.of(
                        "productionProcessConfigSchemaVersion", 1,
                        "productionProcessConfigs", List.of(Map.of(
                                "routeProcessId", 10L,
                                "processId", 20L,
                                "lossReasons", List.of(),
                                "deviceSelectionGroups", List.of(),
                                "parameterRules", List.of())))));

        assertEquals(ErrorCodeConstants.PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE.getCode(), ex.getCode());
        verify(routeVersionMapper, never()).update(any(MesProRouteVersionDO.class), any());
    }

    @Test
    void saveConfigSnapshots_shouldRejectParameterRuleOwnedByAnotherProcess() {
        MesProRouteVersionDO candidate = productionCandidate();
        when(routeVersionMapper.selectById(candidate.getId())).thenReturn(candidate);
        when(routeVersionMapper.selectActiveByRouteId(candidate.getRouteId())).thenReturn(activeSource());

        Map<String, Object> rule = Map.of(
                "routeProcessId", 999L,
                "processId", 20L,
                "deviceId", 501L,
                "parameterCode", "temperature",
                "parameterName", "温度",
                "valueType", "DECIMAL",
                "standardText", "20-30℃");
        Map<String, Object> config = Map.of(
                "routeProcessId", 10L,
                "processId", 20L,
                "overagePercent", 0,
                "lossReasons", List.of(),
                "deviceSelectionGroups", List.of(Map.of(
                        "deviceGroupKey", "cleaning",
                        "selectionMode", "SINGLE",
                        "deviceIds", List.of(501L))),
                "parameterRules", List.of(rule));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.saveConfigSnapshots(
                candidate.getId(), candidate.getRouteSnapshotSha256(), Map.of(
                        "productionProcessConfigSchemaVersion", 1,
                        "productionProcessConfigs", List.of(config))));

        assertEquals(ErrorCodeConstants.PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE.getCode(), ex.getCode());
        verify(routeVersionMapper, never()).update(any(MesProRouteVersionDO.class), any());
    }

    @Test
    void saveConfigSnapshots_shouldRejectUnknownRouteDevice() {
        MesProRouteVersionDO candidate = productionCandidate();
        when(routeVersionMapper.selectById(candidate.getId())).thenReturn(candidate);
        when(routeVersionMapper.selectActiveByRouteId(candidate.getRouteId())).thenReturn(activeSource());
        when(teamDeviceMapper.selectBatchIds(any())).thenReturn(List.of());
        Map<String, Object> config = Map.of(
                "routeProcessId", 10L,
                "processId", 20L,
                "overagePercent", 0,
                "lossReasons", List.of(),
                "deviceSelectionGroups", List.of(Map.of(
                        "deviceGroupKey", "cleaning",
                        "selectionMode", "SINGLE",
                        "deviceIds", List.of(501L))),
                "parameterRules", List.of());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.saveConfigSnapshots(
                candidate.getId(), candidate.getRouteSnapshotSha256(), Map.of(
                        "productionProcessConfigSchemaVersion", 1,
                        "productionProcessConfigs", List.of(config))));

        assertEquals(ErrorCodeConstants.PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE.getCode(), ex.getCode());
        verify(routeVersionMapper, never()).update(any(MesProRouteVersionDO.class), any());
    }

    @Test
    void saveConfigSnapshot_shouldRejectConcurrentSnapshotUpdate() {
        MesProRouteVersionDO candidate = productionCandidate();
        when(routeVersionMapper.selectById(candidate.getId())).thenReturn(candidate);
        when(routeVersionMapper.selectActiveByRouteId(candidate.getRouteId())).thenReturn(activeSource());
        when(routeVersionMapper.update(any(MesProRouteVersionDO.class), any())).thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.saveConfigSnapshot(
                candidate.getId(), "unrelatedConfig", Map.of("enabled", true)));

        assertEquals(ErrorCodeConstants.PRO_ROUTE_VERSION_CONFLICT.getCode(), ex.getCode());
    }

    private static MesProRouteVersionDO productionCandidate() {
        return MesProRouteVersionDO.builder()
                .id(2002L)
                .routeId(9001L)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .sourceRouteVersionId(2001L)
                .routeSnapshotJson("{\"routeId\":9001,\"configSnapshots\":{\"flowGraph\":{\"nodes\":[{\"routeProcessId\":10,\"processId\":20}]}}}")
                .routeSnapshotSha256("current-hash")
                .build();
    }

    private static MesProRouteVersionDO activeSource() {
        return MesProRouteVersionDO.builder()
                .id(2001L)
                .routeId(9001L)
                .active(Boolean.TRUE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE)
                .build();
    }
}
