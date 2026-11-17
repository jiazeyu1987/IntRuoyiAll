package cn.iocoder.yudao.module.mes.service.pro.craft;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.craft.MesProCraftDO;
import com.alibaba.fastjson.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static cn.iocoder.yudao.module.mes.service.pro.craft.MesProCraftParameters.*;
import static cn.iocoder.yudao.module.mes.service.pro.craft.MesSharedRouteSnapshotChange.*;
import static org.junit.jupiter.api.Assertions.*;

class MesSharedRouteSnapshotChangeTest {
    private JSONArray parameters(int temperature) {
        return JSON.parseArray("[{\"parameterCode\":\"T\",\"parameterName\":\"温度\",\"valueType\":\"INTEGER\",\"standardText\":\"热合温度\",\"defaultValue\":" + temperature + "}]");
    }
    private MesProCraftDO craft(long id, long process, int temperature) {
        var c = new MesProCraftDO(); c.setId(id); c.setProcessId(process); c.setCode("GY-" + id);
        c.setName("热合"); c.setParametersJson(parameters(temperature).toJSONString()); return c;
    }
    private JSONObject config() {
        JSONObject c = JSON.parseObject("""
            {"routeProcessId":100,"processId":10,"processName":"热合","sort":1,
             "deviceSelectionGroups":[{"deviceGroupKey":"heat","selectionMode":"SINGLE","deviceIds":[1,2]},
                                      {"deviceGroupKey":"cool","selectionMode":"MULTIPLE","deviceIds":[3]}],
             "parameterRules":[],"overagePercent":0,"lossReasons":[],
             "batchRecordReports":[{"reportId":71}],"formBindings":[{"slotType":"START","formId":81}]}
            """);
        bindAll(c, List.of(new Binding(11L,"GY-11",parameters(180),List.of(1L,2L)), new Binding(12L,"GY-12",parameters(20),List.of(3L))));
        return c;
    }

    @Test void replacementUsesExplicitNewDeviceParametersAndPreservesOtherGroupsAndEvidence() {
        JSONObject c = config();
        var originalReports = canonical(c.get("batchRecordReports"));
        var originalForms = canonical(c.get("formBindings"));
        device(c, Set.of(1L), 4L, craft(13,10,200),false);
        assertEquals(List.of(4L,2L), c.getJSONArray("deviceSelectionGroups").getJSONObject(0).getJSONArray("deviceIds").toJavaList(Long.class));
        var rules = c.getJSONArray("parameterRules").toJavaList(JSONObject.class);
        assertEquals(200,rules.stream().filter(r -> r.getLong("deviceId")==4L).findFirst().orElseThrow().getIntValue("defaultValue"));
        assertEquals(180,rules.stream().filter(r -> r.getLong("deviceId")==2L).findFirst().orElseThrow().getIntValue("defaultValue"));
        assertFalse(rules.stream().anyMatch(r -> r.getLong("deviceId")==1L));
        assertEquals(List.of(11L,12L,13L), bindings(c).stream().map(b->b.getLong("craftId")).toList());
        assertEquals(originalReports,canonical(c.get("batchRecordReports")));
        assertEquals(originalForms,canonical(c.get("formBindings")));
    }

    @Test void missingParameterDecisionAndAlreadyUsedTargetFailWithoutMutatingConfig() {
        JSONObject c = config(); String before = canonical(c);
        assertThrows(RuntimeException.class,()->device(c,Set.of(1L),4L,null,false));
        assertEquals(before,canonical(c));
        assertThrows(RuntimeException.class,()->device(c,Set.of(1L),2L,craft(13,10,200),false));
        assertEquals(before,canonical(c));
        assertThrows(RuntimeException.class,()->device(c,Set.of(1L),4L,craft(13,20,200),false));
        assertEquals(before,canonical(c));
    }

    @Test void selectingExistingCraftMergesDeviceAssociationWithoutDuplicatingCraft() {
        JSONObject c = config();
        device(c,Set.of(1L),4L,craft(12,10,20),false);
        assertEquals(2,bindings(c).size());
        assertEquals(List.of(3L,4L),bindings(c).get(1).getJSONArray("craftDeviceIds").toJavaList(Long.class));
    }

    @Test void explicitNoParametersRemovesOnlyReplacedDeviceRules() {
        JSONObject c = config();
        device(c,Set.of(1L),4L,null,true);
        assertEquals(List.of(2L,3L),c.getJSONArray("parameterRules").toJavaList(JSONObject.class).stream().map(r->r.getLong("deviceId")).toList());
        assertEquals(List.of(2L),bindings(c).get(0).getJSONArray("craftDeviceIds").toJavaList(Long.class));
    }

    @Test void sameNameDifferentIdentityDoesNotChangeUnrelatedProcessOrOriginalSnapshot() {
        JSONObject root=JSON.parseObject("""
          {"configSnapshots":{"flowGraph":{"nodes":[{"routeProcessId":100,"processId":10,"processName":"热合"},
            {"routeProcessId":101,"processId":20,"processName":"热合"}],"edges":[{"from":100,"to":101}]},
            "productionProcessConfigs":[],"batchRecordReports":[{"reportId":71}],"formBindings":[{"formId":81}]}}
          """);
        root.getJSONObject("configSnapshots").getJSONArray("productionProcessConfigs").add(config());
        String original = canonical(root);
        JSONObject copy=JSON.parseObject(root.toJSONString());
        process(copy,10L,JSON.parseObject("{\"id\":30,\"name\":\"新热合\",\"code\":\"GX-30\"}"));
        assertEquals(30L,nodes(copy).get(0).getLong("processId"));
        assertEquals(20L,nodes(copy).get(1).getLong("processId"));
        assertEquals(original,canonical(root));
        var cfg=configurations(copy).get(0);
        assertThrows(RuntimeException.class,()->processBindings(cfg,List.of(new Binding(31L,"GY-31",parameters(200),List.of(1L)))));
        processBindings(cfg,List.of(new Binding(31L,"GY-31",parameters(200),List.of(1L,2L,3L))));
        assertEquals(30L,cfg.getJSONArray("parameterRules").getJSONObject(0).getLong("processId"));
        assertEquals(canonical(root.getJSONObject("configSnapshots").get("formBindings")),canonical(copy.getJSONObject("configSnapshots").get("formBindings")));
    }

    @Test void craftReplacementUpdatesOnlySelectedSchemeAndRejectsCrossProcessTarget() {
        JSONObject c=config();
        assertThrows(RuntimeException.class,()->MesSharedRouteSnapshotChange.craft(c,11L,craft(13,20,200)));
        MesSharedRouteSnapshotChange.craft(c,11L,craft(13,10,200));
        assertEquals(13L,c.getLong("craftId"));
        assertEquals(12L,bindings(c).get(1).getLong("craftId"));
        assertEquals(List.of(1L,2L),c.getJSONArray("craftDeviceIds").toJavaList(Long.class));
    }
}
