package cn.iocoder.yudao.module.mes.service.pro.craft;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesDeviceParameterSnapshotCodec;
import com.alibaba.fastjson.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static cn.iocoder.yudao.module.mes.service.pro.craft.MesProCraftParameters.*;

class MesProCraftParametersTest {
    @Test void httpRequestParametersUseTheSameValidationAsStoredJson() throws Exception {
        var request = new com.fasterxml.jackson.databind.ObjectMapper().readValue("""
            {"name":"清洗工艺","processId":22,"reason":"新增",
             "parameters":[{"parameterCode":"COUNT","parameterName":"次数",
               "valueType":"INTEGER","standardText":"清洗次数","defaultValue":2}]}
            """, cn.iocoder.yudao.module.mes.controller.admin.pro.craft.vo.MesProCraftSaveReqVO.class);
        assertEquals(fingerprint(22L, JSON.parseArray(JSON.toJSONString(request.getParameters()))),
                fingerprint(22L, request.getParameters()));
        assertDoesNotThrow(() -> MesProCraftService.validateDefinitions(22L, request.getParameters()));
        request.getParameters().getJSONObject(0).put("lowerLimit", 3);
        assertThrows(ServiceException.class, () -> MesProCraftService.validateDefinitions(22L, request.getParameters()));
        request.getParameters().add("不是参数对象");
        assertThrows(ServiceException.class, () -> normalize(request.getParameters()));
    }
    private JSONObject config() {
        return JSON.parseObject("""
            {"routeProcessId":11,"processId":22,"deviceSelectionGroups":[
              {"deviceGroupKey":"clean","selectionMode":"MULTIPLE","deviceIds":[31,32,33]}],"parameterRules":[]}
            """);
    }
    private JSONArray definitions() {
        JSONArray parameters = JSON.parseArray("""
            [
              {"parameterCode":"TEMP","parameterName":"温度","valueType":"DECIMAL","unit":"℃",
               "standardText":"温度范围","lowerLimit":20,"upperLimit":40,"defaultValue":30,"decimalScale":1,"sort":1},
              {"parameterCode":"COUNT","parameterName":"次数","valueType":"INTEGER","standardText":"清洗次数","defaultValue":2,"sort":2},
              {"parameterCode":"VALID","parameterName":"有效","valueType":"BOOLEAN","standardText":"设备有效","defaultValue":1,"sort":3},
              {"parameterCode":"MODE","parameterName":"模式","valueType":"SELECT","standardText":"清洗模式","defaultText":"常规","sort":4},
              {"parameterCode":"NOTE","parameterName":"要求","valueType":"TEXT_STANDARD","standardText":"表面洁净","sort":5}
            ]
            """);
        parameters.getJSONObject(3).put("optionValuesJson", JSON.toJSONString(List.of("常规","加强")));
        return parameters;
    }
    @Test void migrationPreservesAllRuntimeValuesAndExactDeviceScope() {
        JSONObject original = config();
        original.put("parameterRules", expand(original, definitions(), List.of(31L,32L)));
        String before = original.toJSONString();
        Extracted extracted = extract(original);
        assertEquals(List.of(31L,32L), extracted.deviceIds());
        assertFalse(extracted.parameters().toJSONString().contains("deviceId"));
        assertEquals(before, original.toJSONString(), "提取不能修改来源");
        JSONObject migrated = JSON.parseObject(before);
        bind(migrated, 1L, "GY-0001", extracted.parameters(), extracted.deviceIds());
        assertEquals(10, migrated.getJSONArray("parameterRules").size());
        assertTrue(MesDeviceParameterSnapshotCodec.matchesCanonicalSnapshot(
                original.getJSONArray("parameterRules").toJSONString(),
                migrated.getJSONArray("parameterRules").toJSONString(), 11L,22L));
        assertFalse(migrated.getJSONArray("parameterRules").stream()
                .anyMatch(raw -> Objects.equals(33L, ((JSONObject)raw).getLong("deviceId"))));
    }
    @Test void differingEquipmentStandardsCannotBeMerged() {
        JSONObject c=config();
        JSONArray rules=expand(c,definitions(),List.of(31L,32L));
        rules.getJSONObject(5).put("standardText","不同设备标准");
        c.put("parameterRules",rules);
        assertThrows(ServiceException.class,()->extract(c));
    }
    @Test void seedExtractionKeepsDifferentSchemesSeparateAndNeverChangesSource() {
        JSONObject c = config();
        JSONArray rules = expand(c, definitions(), List.of(31L, 32L, 33L));
        rules.getJSONObject(5).put("standardText", "不同设备标准");
        c.put("parameterRules", rules);
        String before = c.toJSONString();
        List<Extracted> schemes = extractSchemes(c);
        assertEquals(2, schemes.size());
        assertEquals(List.of(31L, 33L), schemes.get(0).deviceIds());
        assertEquals(List.of(32L), schemes.get(1).deviceIds());
        assertEquals(5, schemes.get(0).parameters().size());
        assertEquals(5, schemes.get(1).parameters().size());
        assertNotEquals(fingerprint(22L, schemes.get(0).parameters()), fingerprint(22L, schemes.get(1).parameters()));
        assertEquals(before, c.toJSONString());
        assertFalse(schemes.get(0).parameters().toJSONString().contains("deviceId"));
    }
    @Test void sameParametersOnDifferentProcessesHaveDifferentIdentity() {
        assertNotEquals(fingerprint(1L,definitions()),fingerprint(2L,definitions()));
    }
    @Test void orderingAndDecimalRepresentationDoNotCreateDifferentCrafts() {
        JSONArray a=definitions(), b=definitions();
        Collections.reverse(b);
        b.stream().map(raw->(JSONObject)raw).filter(p->"TEMP".equals(p.getString("parameterCode")))
                .findFirst().orElseThrow().put("lowerLimit",new java.math.BigDecimal("20.000"));
        assertEquals(fingerprint(22L,a),fingerprint(22L,b));
    }
    @Test void duplicateCodesAreRejectedRatherThanOverwritten() {
        JSONArray d=definitions();d.add(d.get(0));
        assertThrows(ServiceException.class,()->normalize(d));
    }
    @Test void equipmentFieldsCannotEnterCraftLibrary() {
        JSONArray d=definitions();d.getJSONObject(0).put("deviceId",31);
        assertThrows(ServiceException.class,()->normalize(d));
    }
    @Test void nameChangesInvalidateOptimisticRowIdentityButPreserveParameterIdentity() {
        assertNotEquals(rowFingerprint("旧名称",22L,definitions()),rowFingerprint("新名称",22L,definitions()));
    }
    @Test void runtimeEquivalentDuplicateCodesAreRejected() {
        JSONArray d=definitions();JSONObject copy=JSON.parseObject(d.getJSONObject(0).toJSONString());
        copy.put("parameterCode"," temp ");d.add(copy);
        assertThrows(ServiceException.class,()->normalize(d));
    }
    @Test void missingParameterTypeProducesBusinessValidationError() {
        JSONArray d=definitions();d.getJSONObject(0).remove("valueType");
        assertThrows(ServiceException.class,()->normalize(d));
    }
    @Test void expansionRejectsDevicesOutsideCurrentNode() {
        assertThrows(ServiceException.class,()->expand(config(),definitions(),List.of(99L)));
    }
    @Test void emptyDeviceScopeDoesNotProduceDefaultSuccess() {
        assertThrows(ServiceException.class,()->expand(config(),definitions(),List.of()));
    }
}
