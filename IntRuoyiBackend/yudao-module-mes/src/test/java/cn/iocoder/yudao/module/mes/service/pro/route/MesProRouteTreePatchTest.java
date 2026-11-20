package cn.iocoder.yudao.module.mes.service.pro.route;

import com.alibaba.fastjson.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MesProRouteTreePatchTest {
    @Test void lossReasonsSynchronizeSameNamedProcessAcrossIndependentProcessIds() {
        JSONObject before = route(10, 1), after = copy(before), child = route(20, 2, 3);
        config(before, 1).put("processName", "检测工序");
        config(after, 1).put("processName", "检测工序");
        config(child, 2).put("processName", "检测工序");
        config(child, 3).put("processName", "包装工序");
        config(before, 1).put("lossReasons", JSON.parseArray("[{id:1,reasonCode:'A',reasonName:'旧原因'}]"));
        config(after, 1).put("lossReasons", JSON.parseArray("[{id:1,reasonCode:'A',reasonName:'旧原因'},{id:2,reasonCode:'B',reasonName:'新原因'}]"));
        config(child, 2).put("lossReasons", JSON.parseArray("[{id:3,reasonCode:'C',reasonName:'子流程原因'}]"));
        JSONObject original = copy(child);
        JSONObject result = MesProRouteTreePatch.apply(before, after, child);
        JSONArray expected = JSON.parseArray("[{id:3,reasonCode:'C',reasonName:'子流程原因'},{id:1,reasonCode:'A',reasonName:'旧原因'},{id:2,reasonCode:'B',reasonName:'新原因'}]");
        assertEquals(expected, config(result, 2).get("lossReasons"));
        config(original, 2).put("lossReasons", expected);
        assertEquals(copy(original), copy(result), "仅同步目标工序原因，保留子流程身份、顺序和其他配置");
        assertEquals("C", config(child, 2).getJSONArray("lossReasons").getJSONObject(0).getString("reasonCode"));
        JSONObject cleared = copy(after); config(cleared, 1).put("lossReasons", new JSONArray());
        assertThrows(RuntimeException.class, () -> MesProRouteTreePatch.apply(after, cleared, result));
    }
    @Test void lossReasonSyncRejectsMissingOrAmbiguousNames() {
        JSONObject before = route(10, 1), after = copy(before), child = route(20, 2, 3);
        config(before, 1).put("processName", "检测工序");
        config(after, 1).put("processName", "检测工序");
        config(after, 1).put("lossReasons", JSON.parseArray("[{id:2,reasonCode:'B'}]"));
        assertThrows(RuntimeException.class, () -> MesProRouteTreePatch.apply(before, after, child));
        config(child, 2).put("processName", "检测工序"); config(child, 3).put("processName", "检测工序");
        assertThrows(RuntimeException.class, () -> MesProRouteTreePatch.apply(before, after, child));
    }
    @Test void parentFieldOverridesChildAgainButPreservesOtherFields() {
        JSONObject parent = route(10, 1, 2), changed = copy(parent), child = route(20, 1, 2);
        config(child, 1).put("overagePercent", 9);
        config(child, 1).put("prepareTime", 50);
        config(changed, 1).put("overagePercent", 3);
        JSONObject result = MesProRouteTreePatch.apply(parent, changed, child);
        assertEquals(3, config(result, 1).getIntValue("overagePercent"));
        assertEquals(50, config(result, 1).getIntValue("prepareTime"));
        config(result, 1).put("overagePercent", 7);
        JSONObject next = copy(changed); config(next, 1).put("overagePercent", 4);
        assertEquals(4, config(MesProRouteTreePatch.apply(changed, next, result), 1).getIntValue("overagePercent"));
        assertEquals(9, config(child, 1).getIntValue("overagePercent"), "历史输入不得被原地改写");
    }
    @Test void addingCommonProcessRetainsIndependentProcessAndRebuildsSequentialEdges() {
        JSONObject parent = route(10, 1, 2), changed = route(10, 1, 4, 2), child = route(20, 1, 3, 2);
        JSONObject result = MesProRouteTreePatch.apply(parent, changed, child);
        assertEquals(List.of(1L, 3L, 4L, 2L), MesProRouteTreePatch.order(result));
        assertEquals(3, graph(result).getJSONArray("edges").size());
        assertEquals(4, result.getJSONObject("configSnapshots").getJSONArray("productionProcessConfigs").size());
        assertEquals(20, result.getIntValue("routeId"));
        assertEquals(List.of(1L, 3L, 2L), MesProRouteTreePatch.order(child));
        assertEquals(104, config(result, 4).getLongValue("routeProcessId"));
        assertTrue(graph(result).getJSONArray("nodes").toJavaList(JSONObject.class).stream()
                .allMatch(node -> node.getLongValue("routeProcessId") > 0), "正式快照校验要求所有工序引用为正数");
        var validation = new MesProRouteSnapshotCanonicalizer().validate(20L, result.toJSONString());
        assertTrue(validation.ready(), validation.blockers().toString());
    }
    @Test void addedProcessReferenceCollisionFailsBeforePublishing() {
        JSONObject before = route(10, 1, 2), after = route(10, 1, 4, 2), child = route(20, 1, 3, 2);
        graph(child).getJSONArray("nodes").getJSONObject(1).put("routeProcessId", 104);
        config(child, 3).put("routeProcessId", 104);
        graph(child).getJSONArray("edges").getJSONObject(0).put("targetRouteProcessId", 104);
        graph(child).getJSONArray("edges").getJSONObject(1).put("sourceRouteProcessId", 104);
        assertTrue(assertThrows(RuntimeException.class, () -> MesProRouteTreePatch.apply(before, after, child))
                .getMessage().contains("唯一"));
    }
    @Test void addingDraftProcessKeepsCandidateReferencesAndMergesConvergedEndBoundary() {
        JSONObject before = route(10, 1, 2), after = route(10, 1, 2, 4), child = route(20, 1, 2, 3);
        graph(before).put("boundaryEdges", JSON.parseArray("[{boundaryType:'START',routeProcessId:101},{boundaryType:'END',routeProcessId:102}]"));
        graph(after).getJSONArray("nodes").getJSONObject(2).put("routeProcessId", -1L);
        config(after, 4).put("routeProcessId", -1L);
        graph(after).getJSONArray("edges").getJSONObject(1).put("targetRouteProcessId", -1L);
        graph(after).put("boundaryEdges", JSON.parseArray("[{boundaryType:'START',routeProcessId:101},{boundaryType:'END',routeProcessId:-1}]"));
        graph(child).put("boundaryEdges", JSON.parseArray("[{boundaryType:'START',routeProcessId:201},{boundaryType:'END',routeProcessId:203}]"));

        JSONObject result = MesProRouteTreePatch.apply(before, after, child);

        assertEquals(List.of(1L, 2L, 3L, 4L), MesProRouteTreePatch.order(result));
        JSONObject added = graph(result).getJSONArray("nodes").getJSONObject(3);
        assertEquals(-1L, added.getLongValue("routeProcessId"));
        assertEquals(-1L, added.getLongValue("clientRouteProcessId"));
        assertEquals(-1L, config(result, 4).getLongValue("routeProcessId"));
        assertEquals(2, graph(result).getJSONArray("boundaryEdges").size(), "开始和结束各保留一条连线");
        var canonicalizer = new MesProRouteSnapshotCanonicalizer();
        var validation = canonicalizer.validateCandidate(20L, result.toJSONString());
        assertTrue(validation.ready(), validation.blockers().toString());
        assertFalse(canonicalizer.validate(20L, result.toJSONString()).ready(), "正式快照仍禁止负数引用");
    }
    @Test void deletingCommonProcessRemovesOnlyThatProcess() {
        JSONObject result = MesProRouteTreePatch.apply(route(10, 1, 2), route(10, 1), route(20, 1, 3, 2));
        assertEquals(List.of(1L, 3L), MesProRouteTreePatch.order(result));
        assertEquals(2, result.getJSONObject("configSnapshots").getJSONArray("productionProcessConfigs").size());
    }
    @Test void reorderingCommonProcessesMovesIndependentBlockWithItsPredecessor() {
        JSONObject result = MesProRouteTreePatch.apply(route(10, 1, 2), route(10, 2, 1), route(20, 1, 3, 2));
        assertEquals(List.of(2L, 1L, 3L), MesProRouteTreePatch.order(result));
    }
    @Test void removingAnchorOfIndependentProcessRequiresExplicitResolution() {
        assertThrows(RuntimeException.class, () ->
            MesProRouteTreePatch.apply(route(10, 1, 2), route(10, 2), route(20, 1, 3, 2)));
    }
    @Test void missingCorrespondingProcessBlocksFieldUpdateInsteadOfCreatingIt() {
        JSONObject before = route(10, 1, 2), after = copy(before);
        config(after, 2).put("overagePercent", 4);
        assertThrows(RuntimeException.class, () -> MesProRouteTreePatch.apply(before, after, route(20, 1)));
    }
    @Test void inheritanceKeepsIndependentIdentityAndUsesTargetProcessReferences() {
        JSONObject source = route(10, 1, 2), target = route(20, 1, 2);
        config(source, 1).put("overagePercent", 8);
        target.getJSONObject("configSnapshots").put("products", JSON.parseArray("[{\"itemId\":300}]"));
        JSONObject result = MesProRouteTreePatch.inherit(source, target);
        assertEquals(20, result.getIntValue("routeId"));
        assertEquals("R20", result.getString("routeCode"));
        assertEquals(201, graph(result).getJSONArray("nodes").getJSONObject(0).getIntValue("routeProcessId"));
        assertEquals(8, config(result, 1).getIntValue("overagePercent"));
        assertEquals(300, result.getJSONObject("configSnapshots").getJSONArray("products").getJSONObject(0).getIntValue("itemId"));
    }
    @Test void deviceParameterDeltaPreservesChildSpecificRulesAndFields() {
        JSONObject parent = route(10, 1), child = route(20, 1);
        config(parent, 1).put("deviceSelectionGroups", JSON.parseArray("[{deviceGroupKey:'g1',selectionMode:'SINGLE',deviceIds:[1]}]"));
        config(child, 1).put("deviceSelectionGroups", JSON.parseArray("[{deviceGroupKey:'g1',selectionMode:'MULTIPLE',deviceIds:[1]},{deviceGroupKey:'local',selectionMode:'SINGLE',deviceIds:[2]}]"));
        config(parent, 1).put("parameterRules", JSON.parseArray("[{deviceId:1,parameterCode:'PRESSURE',lowerLimit:1,upperLimit:10}]"));
        config(child, 1).put("parameterRules", JSON.parseArray("[{deviceId:1,parameterCode:'PRESSURE',lowerLimit:3,upperLimit:11},{deviceId:2,parameterCode:'PRESSURE',lowerLimit:4,upperLimit:12}]"));
        JSONObject after = copy(parent);
        config(after, 1).getJSONArray("deviceSelectionGroups").getJSONObject(0).put("selectionMode", "MULTIPLE");
        config(after, 1).getJSONArray("parameterRules").getJSONObject(0).put("upperLimit", 20);
        config(child, 1).getJSONArray("parameterRules").getJSONObject(0).put("routeProcessId", 201);
        config(child, 1).getJSONArray("parameterRules").getJSONObject(0).put("sort", 7);
        JSONObject result = config(MesProRouteTreePatch.apply(parent, after, child), 1);
        assertEquals(2, result.getJSONArray("deviceSelectionGroups").size());
        assertEquals(2, result.getJSONArray("parameterRules").size());
        assertEquals(3, result.getJSONArray("parameterRules").getJSONObject(0).getIntValue("lowerLimit"));
        assertEquals(20, result.getJSONArray("parameterRules").getJSONObject(0).getIntValue("upperLimit"));
        assertEquals(7, result.getJSONArray("parameterRules").getJSONObject(0).getIntValue("sort"));
        assertEquals(12, result.getJSONArray("parameterRules").getJSONObject(1).getIntValue("upperLimit"));
    }
    @Test void reorderKeepsBoundaryAndLayoutSortReferencesAlignedIncludingLastLocalProcess() {
        JSONObject parent = route(10, 1, 2), after = route(10, 2, 1), child = route(20, 1, 3, 2);
        graph(parent).put("boundaryEdges", JSON.parseArray("[{boundaryType:'START',routeProcessId:101,routeProcessSort:1},{boundaryType:'END',routeProcessId:102,routeProcessSort:2}]"));
        graph(after).put("boundaryEdges", JSON.parseArray("[{boundaryType:'START',routeProcessId:102,routeProcessSort:1},{boundaryType:'END',routeProcessId:101,routeProcessSort:2}]"));
        graph(child).put("boundaryEdges", JSON.parseArray("[{boundaryType:'START',routeProcessId:201,routeProcessSort:1},{boundaryType:'END',routeProcessId:202,routeProcessSort:3}]"));
        graph(child).put("layouts", JSON.parseArray("[{routeProcessId:203,routeProcessSort:2,x:50}]"));
        JSONObject result = graph(MesProRouteTreePatch.apply(parent, after, child));
        JSONObject end = result.getJSONArray("boundaryEdges").toJavaList(JSONObject.class).stream()
                .filter(edge -> "END".equals(edge.getString("boundaryType"))).findFirst().orElseThrow();
        assertEquals(203, end.getIntValue("routeProcessId"));
        assertEquals(3, end.getIntValue("routeProcessSort"));
        assertEquals(3, result.getJSONArray("layouts").getJSONObject(0).getIntValue("routeProcessSort"));
    }
    @Test void clientReferenceAliasesNeverReplaceOfficialTargetReferences() {
        JSONObject parent = route(10, 1), after = copy(parent), child = route(20, 1);
        graph(child).getJSONArray("nodes").getJSONObject(0).put("clientRouteProcessId", -1);
        config(after, 1).put("overagePercent", 8);
        assertEquals(201, config(MesProRouteTreePatch.apply(parent, after, child), 1).getIntValue("routeProcessId"));
    }
    @Test void staleEditorCommandsDoNotBecomeInheritedBusinessReferences() {
        JSONObject before = route(10, 1, 2), after = route(10, 2, 1), child = route(20, 1, 2);
        graph(before).put("routeProcessCreates", JSON.parseArray("[{clientRouteProcessId:-1,processId:2}]"));
        graph(child).put("routeProcessCreates", JSON.parseArray("[{clientRouteProcessId:-9,processId:2}]"));
        graph(after).put("routeProcessUpdates", JSON.parseArray("[{id:102,sort:1}]"));
        JSONObject result = MesProRouteTreePatch.apply(before, after, child);
        assertEquals(List.of(2L, 1L), MesProRouteTreePatch.order(result));
        assertFalse(graph(result).containsKey("routeProcessCreates"));
        assertFalse(graph(result).containsKey("routeProcessUpdates"));
    }
    @Test void editorEnrichmentDoesNotRequireUnchangedProcessesToExistInChild() {
        JSONObject before = route(10, 1, 2), after = copy(before), child = route(20, 3, 4);
        for (JSONObject node : graph(after).getJSONArray("nodes").toJavaList(JSONObject.class)) {
            node.put("processName", "粗洗工序"); node.put("processCode", "GX-" + node.getLong("processId"));
        }
        config(after, 1).put("creator", "admin"); config(after, 1).put("updater", "admin");
        graph(after).put("layouts", JSON.parseArray("[{routeProcessId:101,x:50,y:100}]"));
        JSONObject result = MesProRouteTreePatch.apply(before, after, child);
        assertEquals(copy(child), copy(result), "仅补充名称、审计字段和坐标时，不修改子流程或制造缺失工序冲突");
    }
    @Test void realProcessChangesStillBlockWhenEditorAlsoEnrichesNames() {
        JSONObject before = route(10, 1, 2), after = copy(before);
        graph(after).getJSONArray("nodes").getJSONObject(1).put("processName", "粗洗工序");
        config(after, 2).put("overagePercent", 4);
        assertTrue(assertThrows(RuntimeException.class,
                () -> MesProRouteTreePatch.apply(before, after, route(20, 1))).getMessage()
                .contains("缺少待修改工序或绑定"));
    }
    @Test void businessChangePreservesChildAuditFieldsAndProductBindings() {
        JSONObject before = route(10, 1), after = copy(before), child = route(20, 1);
        config(before, 1).put("creator", "parent-old");
        config(after, 1).put("updater", "parent-new");
        config(after, 1).put("overagePercent", 4);
        config(child, 1).put("creator", "child-creator"); config(child, 1).put("updater", "child-updater");
        child.getJSONObject("configSnapshots").put("products", JSON.parseArray("[{itemId:300}]"));
        JSONObject result = MesProRouteTreePatch.apply(before, after, child);
        assertEquals(4, config(result, 1).getIntValue("overagePercent"));
        assertEquals("child-creator", config(result, 1).getString("creator"));
        assertEquals("child-updater", config(result, 1).getString("updater"));
        assertEquals(child.getJSONObject("configSnapshots").get("products"), result.getJSONObject("configSnapshots").get("products"));
        assertEquals("R20", result.getString("routeCode"));
    }
    @Test void editorStringProcessIdsAreRestoredAsExactLongIdentities() {
        long process = 2117828464390516946L;
        JSONObject before = route(10, process), after = copy(before), child = route(20, process);
        for (JSONObject root : List.of(after, child)) {
            config(root, process).put("processId", String.valueOf(process));
            config(root, process).put("parameterRules", JSON.parseArray("[{deviceId:7,parameterCode:'P',processId:'" + process + "'}]"));
        }
        config(after, process).put("overagePercent", 4);
        JSONObject result = MesProRouteTreePatch.apply(before, after, child);
        assertEquals(process, config(result, process).get("processId"));
        assertEquals(process, config(result, process).getJSONArray("parameterRules").getJSONObject(0).get("processId"));
        var validation = new MesProRouteSnapshotCanonicalizer().validateCandidate(20L, result.toJSONString());
        assertTrue(validation.ready(), validation.blockers().toString());
        assertInstanceOf(String.class, config(child, process).get("processId"), "不得原地改写历史快照");
    }
    @Test void invalidProcessIdentityIsNotSilentlyCoerced() {
        JSONObject before = route(10, 1), after = copy(before);
        config(after, 1).put("processId", "1.5");
        assertTrue(assertThrows(RuntimeException.class,
                () -> MesProRouteTreePatch.apply(before, after, route(20, 1))).getMessage().contains("工序标识无效"));
    }
    @Test void regeneratedBindingRecordAndDecimalScaleAreNotBusinessChanges() {
        JSONObject before = route(10, 1), after = copy(before), child = route(20, 2);
        before.getJSONObject("configSnapshots").put("batchUseConfigs", JSON.parseArray(
                "[{routeProcessId:101,routeFlowConfigId:9001,productionQuantityFactor:1,enabled:false}]"));
        after.getJSONObject("configSnapshots").put("batchUseConfigs", JSON.parseArray(
                "[{routeProcessId:101,routeFlowConfigId:9002,productionQuantityFactor:1.0,enabled:false}]"));
        child.getJSONObject("configSnapshots").put("batchUseConfigs", JSON.parseArray(
                "[{routeProcessId:202,routeFlowConfigId:9003,productionQuantityFactor:2,enabled:false}]"));
        JSONObject result = MesProRouteTreePatch.apply(before, after, child);
        assertEquals(copy(child), copy(result));
        after.getJSONObject("configSnapshots").getJSONArray("batchUseConfigs").getJSONObject(0)
                .put("productionQuantityFactor", 2);
        assertThrows(RuntimeException.class, () -> MesProRouteTreePatch.apply(before, after, child),
                "实际生产系数改变时仍需存在对应工序绑定");
    }
    private static JSONObject route(long routeId, long... processes) {
        JSONObject root = new JSONObject(true), configs = new JSONObject(true), graph = new JSONObject(true);
        root.put("routeId", routeId); root.put("routeCode", "R" + routeId); root.put("configSnapshots", configs);
        configs.put("flowGraph", graph);
        JSONArray nodes = new JSONArray(), edges = new JSONArray(), production = new JSONArray();
        graph.put("nodes", nodes); graph.put("edges", edges); graph.put("layouts", new JSONArray()); graph.put("boundaryEdges", new JSONArray());
        configs.put("productionProcessConfigs", production); configs.put("products", new JSONArray());
        for (int i = 0; i < processes.length; i++) {
            long id = processes[i], reference = routeId * 10 + id;
            JSONObject node = new JSONObject(true); node.put("routeProcessId", reference);
            node.put("processId", id); node.put("sort", i + 1); nodes.add(node);
            JSONObject config = new JSONObject(true); config.put("routeProcessId", reference); config.put("processId", id);
            config.put("sort", i + 1); config.put("overagePercent", 1); production.add(config);
            if (i > 0) {
                JSONObject edge = new JSONObject(true); edge.put("sourceRouteProcessId", routeId * 10 + processes[i - 1]);
                edge.put("targetRouteProcessId", reference); edge.put("relationType", "NORMAL"); edge.put("sort", i); edges.add(edge);
            }
        }
        return root;
    }
    private static JSONObject graph(JSONObject root) { return root.getJSONObject("configSnapshots").getJSONObject("flowGraph"); }
    private static JSONObject config(JSONObject root, long process) {
        return root.getJSONObject("configSnapshots").getJSONArray("productionProcessConfigs").toJavaList(JSONObject.class)
                .stream().filter(item -> item.getLongValue("processId") == process).findFirst().orElseThrow();
    }
    private static JSONObject copy(JSONObject value) { return JSON.parseObject(value.toJSONString()); }
}
