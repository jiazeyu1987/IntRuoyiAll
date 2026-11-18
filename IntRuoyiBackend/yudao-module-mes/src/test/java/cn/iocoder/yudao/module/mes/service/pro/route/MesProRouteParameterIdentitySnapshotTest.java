package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesProRouteParameterIdentitySnapshotTest {
    private final MesProRouteFlowConfigServiceImpl service = new MesProRouteFlowConfigServiceImpl();
    private final MesProRouteVersionMapper versions = mock(MesProRouteVersionMapper.class);
    private final MesProProcessMapper processes = mock(MesProProcessMapper.class);

    private MesProRouteVersionDO version(String status, String processName) {
        var root = JSON.parseObject("""
                {"routeId":1,"configSnapshots":{"productionProcessConfigSchemaVersion":1,
                 "flowGraph":{"nodes":[{"routeProcessId":11,"processId":21}]},
                 "productionProcessConfigs":[{"routeProcessId":11,"processId":21,
                    "overagePercent":0,"lossReasons":[],"deviceSelectionGroups":[],"parameterRules":[]}]}}
                """);
        root.getJSONObject("configSnapshots").getJSONArray("productionProcessConfigs")
                .getJSONObject(0).put("processName", processName);
        var version = MesProRouteVersionDO.builder().id(1L).routeId(1L).lifecycleStatus(status)
                .routeSnapshotJson(root.toJSONString()).routeSnapshotSha256("frozen").build();
        ReflectionTestUtils.setField(service, "routeVersionMapper", versions);
        ReflectionTestUtils.setField(service, "processMapper", processes);
        when(versions.selectById(1L)).thenReturn(version);
        return version;
    }

    @ParameterizedTest
    @ValueSource(strings = {"DRAFT", "PENDING_APPROVAL", "ACTIVE", "SUPERSEDED"})
    void parameterViewUsesVersionProcessNameWithoutJoiningCurrentMaster(String status) {
        var version = version(status, "粗洗工序旧名称");
        var before = version.getRouteSnapshotJson();

        var result = service.getRouteProcessDeviceParameterConfig(1L, 11L);

        assertEquals("粗洗工序旧名称", result.getProcessName());
        assertEquals(21L, result.getProcessId());
        assertEquals(before, version.getRouteSnapshotJson());
        verifyNoInteractions(processes);
        verify(versions, never()).updateById(any(MesProRouteVersionDO.class));
    }

    @Test void incompleteIdentityDoesNotSubstituteCurrentMasterName() {
        version("SUPERSEDED", null);
        assertThrows(ServiceException.class, () -> service.getRouteProcessDeviceParameterConfig(1L, 11L));
        verifyNoInteractions(processes);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void historicalDeviceIdentitySurvivesRenameOrDeletion(boolean deleted) {
        var version = version("SUPERSEDED", "粗洗工序");
        var root = JSON.parseObject(version.getRouteSnapshotJson());
        root.getJSONObject("configSnapshots").getJSONArray("productionProcessConfigs").getJSONObject(0)
            .getJSONArray("deviceSelectionGroups").add(JSON.parseObject("""
                {"deviceGroupKey":"G1","selectionMode":"SINGLE","deviceIds":[31]}
                """));
        version.setRouteSnapshotJson(root.toJSONString());
        var snapshots = mock(MesProRouteDeviceIdentitySnapshotService.class);
        var devices = mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamDeviceMapper.class);
        ReflectionTestUtils.setField(service, "deviceIdentitySnapshots", snapshots);
        ReflectionTestUtils.setField(service, "teamDeviceMapper", devices);
        when(snapshots.require(1L, java.util.Set.of(31L))).thenReturn(java.util.Map.of(31L,
            new MesProRouteDeviceIdentitySnapshotService.Identity(31L, "OLD-31", "历史设备")));
        when(devices.selectBatchIds(java.util.Set.of(31L))).thenReturn(deleted ? java.util.List.of()
            : java.util.List.of(new cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamDeviceDO()
                .setId(31L).setDeviceCode("NEW-31").setDeviceName("当前名称").setDeviceStatus("REPAIRING")));

        var result = service.getRouteProcessDeviceParameterConfig(1L, 11L).getDevices().get(0);

        assertEquals("OLD-31", result.getDeviceCode());
        assertEquals("历史设备", result.getDeviceName());
        assertEquals(deleted ? "DELETED" : "REPAIRING", result.getDeviceStatus());
        assertEquals(root.toJSONString(), version.getRouteSnapshotJson());
    }
}
