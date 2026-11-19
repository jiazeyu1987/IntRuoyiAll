package cn.iocoder.yudao.module.mes.service.pro.route.importer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MesProRouteTransferSnapshotTest {
    @Test void scheduleProjectionIdsDoNotCollideWithProcessIdsDuringImport() {
        var rows = graph();
        rows.put("mes_pro_route_process:71", JSON.parseObject("{\"id\":71,\"route_id\":1,\"process_id\":3}"));
        rows.put("mes_pro_route_schedule_config:71", JSON.parseObject("{\"id\":71,\"route_version_id\":20,\"route_process_id\":71}"));
        rows.get("mes_pro_route_version:20").put("route_snapshot_json", "{\"configSnapshots\":{\"scheduleConfigs\":{\"71\":{\"id\":71,\"routeProcessId\":71}}}}");
        MesProRouteTransferSnapshot.normalize(rows);
        Map<String,Long> ids = new HashMap<>();
        rows.forEach((key,row)->ids.put(key,row.getLong("id")+100));
        var snapshot = JSON.parseObject(MesProRouteResourceImportService.rewriteJson(rows.get("mes_pro_route_version:20").getString("route_snapshot_json"),ids));
        var config = snapshot.getJSONObject("configSnapshots").getJSONObject("scheduleConfigs").getJSONObject("71");
        assertEquals(171L,config.getLong("routeProcessId"));
        assertNotEquals(config.getLong("routeProcessId"),config.getLong("id"));
    }
    @Test void projectsSnapshotReasonsPerVersionAndProcessAndRewritesAllReferences() {
        Map<String, JSONObject> rows = new LinkedHashMap<>();
        for (int route = 1; route <= 2; route++) {
            long version = route * 10L;
            long step = route * 100L;
            JSONObject snapshot = JSON.parseObject("{\"configSnapshots\":{\"productionProcessConfigs\":[{\"routeProcessId\":" + step
                    + ",\"processId\":3,\"lossReasons\":[{\"id\":999,\"reasonCode\":\"SHYY-001\",\"reasonName\":\"后盖裂\",\"enabled\":true}]}]}}");
            rows.put("mes_pro_route_version:" + version, JSON.parseObject("{\"id\":" + version + ",\"route_id\":" + route + "}"));
            rows.get("mes_pro_route_version:" + version).put("route_snapshot_json", snapshot.toJSONString());
            rows.put("mes_pro_route_process:" + step, JSON.parseObject("{\"id\":" + step + ",\"route_id\":" + route + ",\"process_id\":3}"));
        }
        MesProRouteTransferSnapshot.normalize(rows);
        var reasons = rows.entrySet().stream().filter(e -> e.getKey().startsWith("mes_pro_route_process_loss_reason:")).toList();
        assertEquals(2, reasons.size());
        assertNotEquals(reasons.get(0).getValue().getLong("id"), reasons.get(1).getValue().getLong("id"));
        Map<String, Long> mapping = new HashMap<>();
        rows.forEach((key, value) -> mapping.put(key, value.getLong("id") + 1000));
        mapping.put("mes_pro_process:3", 3000L);
        for (var entry : reasons) {
            JSONObject row = entry.getValue();
            assertEquals(new HashSet<>(MesProRouteTransferSchema.TABLES.get("mes_pro_route_process_loss_reason").columns()), row.keySet());
            assertEquals("后盖裂", row.getString("reason_name"));
            JSONObject snapshot = JSON.parseObject(rows.get("mes_pro_route_version:" + row.getLong("route_version_id")).getString("route_snapshot_json"));
            JSONObject reason = snapshot.getJSONObject("configSnapshots").getJSONArray("productionProcessConfigs").getJSONObject(0).getJSONArray("lossReasons").getJSONObject(0);
            assertEquals(row.getLong("id"), reason.getLong("id"));
            JSONObject migrated = JSON.parseObject(MesProRouteResourceImportService.rewriteJson(snapshot.toJSONString(), mapping));
            assertEquals(mapping.get(entry.getKey()), migrated.getJSONObject("configSnapshots").getJSONArray("productionProcessConfigs").getJSONObject(0).getJSONArray("lossReasons").getJSONObject(0).getLong("id"));
        }
    }

    @Test void rejectsReasonsOutsideTheirRouteAndDuplicateReasonCodes() {
        var rows = graph();
        rows.get("mes_pro_route_version:20").put("route_snapshot_json", "{\"configSnapshots\":{\"productionProcessConfigs\":[{\"routeProcessId\":2,\"processId\":3,\"lossReasons\":[{\"id\":99,\"reasonCode\":\"A\",\"reasonName\":\"原因\",\"enabled\":true}]}]}}");
        assertThrows(ServiceException.class, () -> MesProRouteTransferSnapshot.normalize(rows));
        rows.put("mes_pro_route_process:2", JSON.parseObject("{\"id\":2,\"route_id\":1,\"process_id\":3}"));
        JSONObject snapshot = JSON.parseObject(rows.get("mes_pro_route_version:20").getString("route_snapshot_json"));
        var reasons = snapshot.getJSONObject("configSnapshots").getJSONArray("productionProcessConfigs").getJSONObject(0).getJSONArray("lossReasons");
        reasons.add(JSON.parseObject(reasons.getJSONObject(0).toJSONString()));
        rows.get("mes_pro_route_version:20").put("route_snapshot_json", snapshot.toJSONString());
        assertThrows(ServiceException.class, () -> MesProRouteTransferSnapshot.normalize(rows));
    }
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
    @Test void preservesBusinessKeyedProductsBomsAndScheduleConfigs() {
        var rows = graph();
        JSONObject version = rows.get("mes_pro_route_version:20");
        version.put("route_snapshot_json", """
                {"configSnapshots":{
                  "products":{"100":{"id":50,"itemId":100,"quantity":2}},
                  "productBoms":{"bom":{"id":60,"processId":3,"productId":100,"itemId":101,"quantity":1.5}},
                  "scheduleConfigs":{"2":{"id":70,"routeProcessId":2,"capacityMode":"RESOURCE_CALCULATED"}}
                }}
                """);
        rows.put("mes_pro_route_product:51", JSON.parseObject("{\"id\":51,\"route_id\":1,\"item_id\":100}"));
        rows.put("mes_pro_route_product_bom:61", JSON.parseObject("{\"id\":61,\"route_id\":1,\"process_id\":3,\"product_id\":100,\"item_id\":101}"));
        rows.put("mes_pro_route_schedule_config:71", JSON.parseObject("{\"id\":71,\"route_version_id\":20,\"route_process_id\":2}"));

        MesProRouteTransferSnapshot.normalize(rows);

        JSONObject configs = JSON.parseObject(version.getString("route_snapshot_json")).getJSONObject("configSnapshots");
        assertEquals(51L, configs.getJSONObject("products").getJSONObject("100").getLong("id"));
        assertEquals(2, configs.getJSONObject("products").getJSONObject("100").getIntValue("quantity"));
        assertEquals(61L, configs.getJSONObject("productBoms").getJSONObject("bom").getLong("id"));
        assertEquals(1.5, configs.getJSONObject("productBoms").getJSONObject("bom").getDoubleValue("quantity"));
        Long scheduleId = configs.getJSONObject("scheduleConfigs").getJSONObject("2").getLong("id");
        assertNotEquals(71L, scheduleId);
        assertNotNull(rows.get("mes_pro_route_schedule_config:" + scheduleId));
        assertEquals("RESOURCE_CALCULATED", configs.getJSONObject("scheduleConfigs").getJSONObject("2").getString("capacityMode"));
    }

    @Test void preservesEmptyBusinessKeyedCollections() {
        var rows = graph();
        JSONObject version = rows.get("mes_pro_route_version:20");
        version.put("route_snapshot_json", "{\"configSnapshots\":{\"products\":{},\"productBoms\":{},\"scheduleConfigs\":{}}}");
        MesProRouteTransferSnapshot.normalize(rows);
        JSONObject configs = JSON.parseObject(version.getString("route_snapshot_json")).getJSONObject("configSnapshots");
        for (String key : List.of("products", "productBoms", "scheduleConfigs")) {
            assertTrue(configs.getJSONObject(key).isEmpty());
        }
    }

    @Test void rejectsInvalidCollectionAndEntries() {
        for (String value : List.of("123", "[null]", "{\"2\":false}")) {
            var rows = graph();
            rows.get("mes_pro_route_version:20").put("route_snapshot_json", "{\"configSnapshots\":{\"scheduleConfigs\":" + value + "}}");
            assertThrows(ServiceException.class, () -> MesProRouteTransferSnapshot.normalize(rows));
        }
    }
}
