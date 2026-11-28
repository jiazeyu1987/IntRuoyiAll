import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.PropertyFilter;
import com.alibaba.fastjson.serializer.SerializerFeature;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowNodeRespVO;

public class SerializerCheck {
    public static void main(String[] arguments) {
        MesProRouteProcessFlowNodeRespVO node = new MesProRouteProcessFlowNodeRespVO();
        node.setProcessId(1L);
        node.setSopFileUrl(null);
        JSONObject converted = (JSONObject) JSON.toJSON(node);
        String serialized = JSON.toJSONString(converted,
                (PropertyFilter) (source, name, value) -> value != null || "sopFileUrl".equals(name),
                SerializerFeature.WriteMapNullValue, SerializerFeature.DisableCircularReferenceDetect);
        JSONObject reparsed = JSON.parseObject(serialized);
        System.out.println("memory_only_check=true; no business endpoint or database used");
        System.out.println("after_JSON_toJSON_sop_field_present=" + converted.containsKey("sopFileUrl"));
        System.out.println("after_snapshot_writer_expression_sop_field_present=" + reparsed.containsKey("sopFileUrl"));
        JSONObject explicit = new JSONObject();
        explicit.put("sopFileUrl", null);
        String preserved = JSON.toJSONString(explicit,
                (PropertyFilter) (source, name, value) -> value != null || "sopFileUrl".equals(name),
                SerializerFeature.WriteMapNullValue, SerializerFeature.DisableCircularReferenceDetect);
        System.out.println("explicit_null_preserved=" + JSON.parseObject(preserved).containsKey("sopFileUrl"));
    }
}
