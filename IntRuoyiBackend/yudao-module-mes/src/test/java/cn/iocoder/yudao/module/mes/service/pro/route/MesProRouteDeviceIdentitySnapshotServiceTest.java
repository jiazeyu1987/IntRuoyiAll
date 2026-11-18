package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamDeviceDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamDeviceMapper;
import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesProRouteDeviceIdentitySnapshotServiceTest {
    private MesProRouteDeviceIdentitySnapshotService service;
    private MesProRouteVersionMapper versions;
    private MesProcessPoolTeamDeviceMapper devices;
    private JdbcTemplate jdbc;

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("""
          CREATE TABLE mes_pro_route_device_identity_snapshot (
            tenant_id BIGINT, route_version_id BIGINT, identities_json CLOB, identities_sha256 VARCHAR(64),
            capture_source VARCHAR(32), captured_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, captured_by VARCHAR(64),
            PRIMARY KEY(tenant_id,route_version_id))
          """);
        service = new MesProRouteDeviceIdentitySnapshotService();
        versions = mock(MesProRouteVersionMapper.class); devices = mock(MesProcessPoolTeamDeviceMapper.class);
        ReflectionTestUtils.setField(service, "versions", versions);
        ReflectionTestUtils.setField(service, "devices", devices);
        ReflectionTestUtils.setField(service, "jdbc", new NamedParameterJdbcTemplate(jdbc));
    }
    @AfterEach void cleanup() { TenantContextHolder.clear(); }

    private MesProRouteVersionDO version(long id, long deviceId) {
        var root = JSON.parseObject("""
            {"configSnapshots":{"productionProcessConfigs":[{"deviceSelectionGroups":[{"deviceIds":[]}]}]}}
            """);
        root.getJSONObject("configSnapshots").getJSONArray("productionProcessConfigs").getJSONObject(0)
            .getJSONArray("deviceSelectionGroups").getJSONObject(0).getJSONArray("deviceIds").add(deviceId);
        return MesProRouteVersionDO.builder().id(id).active(false).lifecycleStatus("DRAFT")
            .routeSnapshotJson(root.toJSONString()).routeSnapshotSha256("unchanged-route-hash").build();
    }
    private void live(long id, String name) {
        when(devices.selectBatchIds(Set.of(id))).thenReturn(List.of(new MesProcessPoolTeamDeviceDO()
            .setId(id).setDeviceCode("D" + id).setDeviceName(name)));
    }
    @Test void publishedIdentitySurvivesMasterRenameWithoutChangingRouteHash() {
        var old = version(101, 1); live(1, "旧设备名称"); service.captureCreated(old);
        old.setLifecycleStatus("ACTIVE"); old.setActive(true);
        when(versions.selectByIdForUpdate(101L)).thenReturn(old);
        live(1, "新设备名称");
        assertThrows(ServiceException.class, () -> service.captureDraft(101L));
        assertEquals("旧设备名称", service.require(101L, Set.of(1L)).get(1L).deviceName());
        assertEquals("unchanged-route-hash", old.getRouteSnapshotSha256());
        service.captureCreated(version(102, 1));
        assertEquals("新设备名称", service.require(102L, Set.of(1L)).get(1L).deviceName());
    }
    @Test void sharedUpdatePinsProposedNameBeforeMasterChangesAndRepeatedSaveIsSafe() {
        var draft = version(101, 1); live(1, "旧设备名称"); service.captureCreated(draft);
        when(versions.selectByIdForUpdate(101L)).thenReturn(draft);
        var proposal = JSON.parseObject("""
          {"request":{"subjectType":"DEVICE","action":"UPDATE"},"sourceDeviceIds":[1],
           "data":{"code":"D1","name":"审批中的新名称"}}
          """);
        service.captureDraft(101L, proposal); service.captureDraft(101L, proposal);
        assertEquals("审批中的新名称", service.require(101L, Set.of(1L)).get(1L).deviceName());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_route_device_identity_snapshot", Integer.class));
        assertEquals("unchanged-route-hash", draft.getRouteSnapshotSha256());
    }
    @Test void replacementPinsTargetIdentityAndCannotReturnOldDevice() {
        var draft = version(101, 2); live(2, "执行设备B"); service.captureCreated(draft);
        when(versions.selectByIdForUpdate(101L)).thenReturn(draft);
        service.captureDraft(101L, JSON.parseObject("""
          {"request":{"subjectType":"DEVICE","action":"REPLACE"},"sourceDeviceIds":[1],"targetDeviceId":2,
           "data":{"code":"D2","name":"设备B"}}
          """));
        assertEquals("设备B", service.require(101L, Set.of(2L)).get(2L).deviceName());
        assertThrows(ServiceException.class, () -> service.require(101L, Set.of(1L)));
    }
    @Test void missingSnapshotOrOtherTenantCannotUseCurrentMasterData() {
        live(1, "当前设备"); service.captureCreated(version(101, 1)); clearInvocations(devices);
        assertThrows(ServiceException.class, () -> service.require(102L, Set.of(1L)));
        TenantContextHolder.setTenantId(2L);
        assertThrows(ServiceException.class, () -> service.require(101L, Set.of(1L)));
        verifyNoInteractions(devices);
    }
    @Test void corruptionIsRejectedAndPublishedRowCannotBeReinitialized() {
        var v = version(101, 1); live(1, "设备A"); service.captureCreated(v);
        assertThrows(ServiceException.class, () -> service.captureCreated(v));
        jdbc.update("UPDATE mes_pro_route_device_identity_snapshot SET identities_json='{}'");
        assertThrows(ServiceException.class, () -> service.require(101L, Set.of(1L)));
    }
    @Test void pendingApprovalSnapshotCannotBeChanged() {
        var v = version(101, 1); live(1, "设备A"); service.captureCreated(v);
        v.setLifecycleStatus("PENDING_APPROVAL"); when(versions.selectByIdForUpdate(101L)).thenReturn(v);
        assertThrows(ServiceException.class, () -> service.captureDraft(101L));
        assertEquals("设备A", service.require(101L, Set.of(1L)).get(1L).deviceName());
    }
}
