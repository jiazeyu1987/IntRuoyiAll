package cn.iocoder.yudao.module.mes.service.pro.route.importer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import java.util.*;

/** Personnel are configured in the destination tenant, never transported with process definitions. */
final class MesProRouteTransferPersonnel {
    static final Set<String> LOCAL_MAINTAINER_TABLES = Set.of(
            "mes_pro_process_pool_team_device", "mes_pro_process_pool_team_process_device",
            "mes_pro_process_pool_device_parameter_rule");
    private static final Set<String> FIELDS = Set.of(
            "projectleader", "projectengineer", "projectleaderuserid", "chargeuserid", "leaderuserid",
            "candidatesourcetype", "candidatesourceids", "candidatesourcenames",
            "assigneduserids", "assignedusernames", "ownername", "fillablescopejson", "permissionscopeid",
            "slotconfigsnapshothash", "creator", "updater", "createtime", "updatetime", "tenantid", "deleted");
    private static final Set<String> ASSIGNMENTS = Set.of("routeStartProductionLeaders", "batchRecordAttachmentOwners");

    static void remove(Object value) {
        if (value instanceof JSONObject object) {
            for (String key : new ArrayList<>(object.keySet())) {
                String normalized = key.replace("_", "").toLowerCase(Locale.ROOT);
                if (FIELDS.contains(normalized) || normalized.endsWith("userid") || normalized.endsWith("userids")
                        || normalized.endsWith("username") || normalized.endsWith("usernames")) {
                    object.remove(key);
                } else if (ASSIGNMENTS.contains(key)) {
                    object.put(key, new JSONArray());
                } else if (normalized.equals("remark")) {
                    object.put(key, visibleRemark(object.getString(key)));
                } else if (normalized.endsWith("json") && object.get(key) instanceof String json && !json.isBlank()) {
                    Object parsed = JSON.parse(json);
                    remove(parsed);
                    object.put(key, JSON.toJSONString(parsed, SerializerFeature.WriteMapNullValue));
                } else {
                    remove(object.get(key));
                }
            }
        } else if (value instanceof JSONArray array) {
            for (Object child : array) remove(child);
        }
    }

    static String visibleRemark(String remark) {
        return remark == null ? null : remark.replaceAll("(?s)\\[owner].*?\\[/owner]", "").trim();
    }

    private MesProRouteTransferPersonnel() {}
}
