package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.service.pro.craft.MesProCraftService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Regression for tenant 173's copied GROUP scope blocking the production leader workbench. */
class MesProRouteTreeLeaderScopeRegressionTest {
    @Test
    void copiedGroupClearsTemplateLeaderBeforeProjectionAndLeavesSourceUntouched() {
        var service = new MesProRouteTreeService();
        var routes = mock(MesProRouteMapper.class);
        var versions = mock(MesProRouteVersionMapper.class);
        var projection = mock(MesProRouteVersionPublishProjectionServiceImpl.class);
        var source = route(10, 913913L);
        String sourceJson = MesProRouteVersionSnapshotIdentityWriter.toSnapshotJson(source);
        assertTrue(MesProRouteVersionSnapshotValidator.hasCompleteConfigSnapshot(sourceJson));
        when(versions.selectActiveByRouteId(10L)).thenReturn(MesProRouteVersionDO.builder()
                .id(100L).routeId(10L).routeSnapshotJson(sourceJson).build());
        when(routes.insert(any(MesProRouteDO.class))).thenAnswer(call -> {
            call.<MesProRouteDO>getArgument(0).setId(20L);
            return 1;
        });
        when(versions.insert(any(MesProRouteVersionDO.class))).thenAnswer(call -> {
            call.<MesProRouteVersionDO>getArgument(0).setId(200L);
            return 1;
        });
        when(versions.updateById(any(MesProRouteVersionDO.class))).thenReturn(1);
        AtomicReference<String> projected = new AtomicReference<>();
        when(projection.projectCandidate(any())).thenAnswer(call -> {
            MesProRouteVersionDO version = call.getArgument(0);
            projected.set(version.getRouteSnapshotJson());
            return new MesProRouteVersionPublishProjectionServiceImpl.ProjectionResult(
                    version.getRouteSnapshotJson(), Set.of(101L));
        });
        ObjectProvider<MesProCraftService> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(mock(MesProCraftService.class));
        ReflectionTestUtils.setField(service, "routes", routes);
        ReflectionTestUtils.setField(service, "versions", versions);
        ReflectionTestUtils.setField(service, "crafts", provider);
        ReflectionTestUtils.setField(service, "projection", projection);
        ReflectionTestUtils.setField(service, "devices", mock(MesProRouteDeviceIdentitySnapshotService.class));
        ReflectionTestUtils.setField(service, "platform", mock(MesProRouteControlledContentAdapter.class));
        ReflectionTestUtils.setField(service, "owners", mock(MesProRouteOwnerPermissionService.class));

        assertEquals(20L, service.createGroup("目录模板", null, 10L));

        JSONObject result = JSON.parseObject(projected.get());
        assertEquals(20L, result.getLong("routeId"));
        assertEquals(new JSONArray(), configs(result).getJSONArray("routeStartProductionLeaders"));
        JSONObject projectedNode = configs(result).getJSONObject("flowGraph").getJSONArray("nodes").getJSONObject(0);
        assertTrue(projectedNode.containsKey("sopFileUrl"), "Explicit unbound SOP must survive directory snapshot serialization");
        assertNull(projectedNode.get("sopFileUrl"));
        assertEquals(sourceJson, MesProRouteVersionSnapshotIdentityWriter.toSnapshotJson(source));
        assertEquals(913913L, leaders(source).getJSONObject(0).getJSONArray("candidateSourceIds").getLong(0));
        assertTrue(MesProRouteVersionSnapshotValidator.hasCompleteConfigSnapshot(projected.get()));
    }

    @Test
    void inheritRetainsChildScopeAndNeverCopiesParentUsersOrParentIdentity() {
        JSONObject parent = route(10, 1001L), child = route(20, 2001L);
        JSONObject beforeParent = copy(parent), beforeChild = copy(child);
        JSONObject result = MesProRouteTreePatch.inherit(parent, child);
        assertEquals(leaders(child), leaders(result));
        assertEquals(20L, leaders(result).getJSONObject(0).getLong("productionLineId"));
        assertEquals(beforeParent, parent);
        assertEquals(beforeChild, child);
    }

    @Test
    void newChildWithNoScopeMustRemainUnassignedRatherThanInheritingParentPermission() {
        JSONObject child = route(20, null);
        configs(child).remove("routeStartProductionLeaders");
        JSONObject result = MesProRouteTreePatch.inherit(route(10, 1001L), child);
        assertFalse(configs(result).containsKey("routeStartProductionLeaders"));
        assertThrows(RuntimeException.class, () -> MesProRouteVersionSnapshotValidator
                .requireProductionLeaderAssignments(result.toJSONString()));
    }

    @Test
    void parentPublicationChangesProcessConfigurationWithoutChangingChildResponsibility() {
        JSONObject parent = route(10, 1001L), changed = copy(parent), child = route(20, 2001L);
        configs(changed).put("routeStartProductionLeaders", leaders(route(10, 9999L)));
        configs(changed).getJSONArray("productionProcessConfigs").getJSONObject(0).put("overagePercent", 5);
        JSONObject originalChild = copy(child);

        JSONObject result = MesProRouteTreePatch.apply(parent, changed, child);

        assertEquals(5, configs(result).getJSONArray("productionProcessConfigs").getJSONObject(0).getIntValue("overagePercent"));
        assertEquals(leaders(child), leaders(result));
        assertEquals(2001L, leaders(result).getJSONObject(0).getJSONArray("candidateSourceIds").getLong(0));
        assertEquals(originalChild, child, "Published historical input must not be modified in place");
        configs(changed).remove("routeStartProductionLeaders");
        assertEquals(leaders(child), leaders(MesProRouteTreePatch.apply(parent, changed, child)));
    }

    @Test
    void publicationCannotAuthorizeAnUnassignedChildOrEraseAnExplicitEmptyScope() {
        JSONObject parent = route(10, 1001L), changed = route(10, 9999L);
        for (boolean hasExplicitEmpty : new boolean[]{true, false}) {
            JSONObject child = route(20, null);
            if (!hasExplicitEmpty) configs(child).remove("routeStartProductionLeaders");
            JSONObject result = MesProRouteTreePatch.apply(parent, changed, child);
            assertEquals(hasExplicitEmpty, configs(result).containsKey("routeStartProductionLeaders"));
            if (hasExplicitEmpty) assertEquals(new JSONArray(), leaders(result));
            assertThrows(RuntimeException.class, () -> MesProRouteVersionSnapshotValidator
                    .requireProductionLeaderAssignments(result.toJSONString()));
        }
    }

    private static JSONObject route(long routeId, Long userId) {
        JSONObject root = JSON.parseObject("""
                {"routeId":%d,"routeCode":"R%d","routeName":"Route%d","configSnapshots":{
                  "flowGraph":{"nodes":[{"routeProcessId":%d,"processId":1,"sort":1,"sopFileUrl":null}],
                    "edges":[],"layouts":[],"boundaryEdges":[]},
                  "products":[],"productBoms":[],"scheduleConfigs":[],"batchUseConfigs":[],"scheduleUseConfigs":[],
                  "productionProcessConfigSchemaVersion":1,
                  "productionProcessConfigs":[{"routeProcessId":%d,"processId":1,"sort":1,"overagePercent":1,
                    "lossReasons":[],"deviceSelectionGroups":[],"parameterRules":[]}],
                  "routeStartProductionLeaders":[]}}
                """.formatted(routeId, routeId, routeId, routeId * 10 + 1, routeId * 10 + 1));
        if (userId != null) leaders(root).add(JSON.parseObject("""
                {"productionLineId":%d,"candidateSourceType":"USERS","candidateSourceIds":[%d],"sort":1}
                """.formatted(routeId, userId)));
        return root;
    }

    private static JSONObject configs(JSONObject root) { return root.getJSONObject("configSnapshots"); }
    private static JSONArray leaders(JSONObject root) { return configs(root).getJSONArray("routeStartProductionLeaders"); }
    private static JSONObject copy(JSONObject root) {
        return JSON.parseObject(MesProRouteVersionSnapshotIdentityWriter.toSnapshotJson(root));
    }
}
