package cn.iocoder.yudao.module.mes.service.pro.route.importer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MesProRouteTransferSnapshotTest {
    private Map<String, JSONObject> graph() {
        Map<String, JSONObject> rows = new LinkedHashMap<>();
        rows.put("mes_pro_route_version:20", JSON.parseObject("{\"id\":20,\"route_id\":1,\"route_snapshot_json\":\"{\\\"routeVersionId\\\":19,\\\"configSnapshots\\\":{\\\"batchUseConfigs\\\":[{\\\"id\\\":30,\\\"routeFlowConfigId\\\":40,\\\"routeProcessId\\\":2,\\\"useType\\\":\\\"BATCH\\\",\\\"enabled\\\":false,\\\"creator\\\":\\\"old-user\\\"}]}}\"}"));
        rows.put("mes_pro_route_flow_process_config:31", JSON.parseObject("{\"id\":31,\"route_id\":1,\"route_process_id\":2,\"use_type\":\"BATCH\",\"route_flow_config_id\":41}"));
        return rows;
    }
    @Test void bindsHistoricalProjectionIdsByBusinessIdentityWithoutChangingSettings() {
        var rows = graph(); MesProRouteTransferSnapshot.normalize(rows);
        JSONObject version = rows.get("mes_pro_route_version:20");
        JSONObject snapshot = JSON.parseObject(version.getString("route_snapshot_json"));
        assertEquals(20L, snapshot.getLong("routeVersionId"));
        JSONObject config = snapshot.getJSONObject("configSnapshots").getJSONArray("batchUseConfigs").getJSONObject(0);
        assertEquals(31L, config.getLong("id")); assertEquals(41L, config.getLong("routeFlowConfigId"));
        assertEquals(false, config.getBoolean("enabled")); assertFalse(config.containsKey("creator"));
        assertNotNull(version.getString("route_snapshot_sha256"));
    }
    @Test void rejectsAmbiguousProjectionIdentity() {
        var rows = graph(); rows.put("mes_pro_route_flow_process_config:32", JSON.parseObject(rows.get("mes_pro_route_flow_process_config:31").toJSONString()));
        assertThrows(ServiceException.class, () -> MesProRouteTransferSnapshot.normalize(rows));
    }
    @Test void preservesExplicitEmptyBindingWithoutInventingProjectionIds() {
        var rows = graph(); JSONObject version = rows.get("mes_pro_route_version:20");
        version.put("route_snapshot_json", "{\"configSnapshots\":{\"batchUseConfigs\":[{\"routeProcessId\":2,\"batchRecordBindingSnapshotExplicit\":true,\"formBindings\":[],\"batchRecordReports\":[]}]}}");
        MesProRouteTransferSnapshot.normalize(rows);
        JSONObject config = JSON.parseObject(version.getString("route_snapshot_json")).getJSONObject("configSnapshots").getJSONArray("batchUseConfigs").getJSONObject(0);
        assertFalse(config.containsKey("id")); assertTrue(config.getBoolean("batchRecordBindingSnapshotExplicit"));
        assertEquals(0, config.getJSONArray("formBindings").size());
        assertEquals(0, config.getJSONArray("batchRecordReports").size());
    }
}
