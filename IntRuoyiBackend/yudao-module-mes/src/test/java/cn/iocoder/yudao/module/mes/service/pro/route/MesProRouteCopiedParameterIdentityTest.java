package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MesProRouteCopiedParameterIdentityTest {

    @Test
    void copyRemapsParameterIdentityWithoutChangingSource() {
        JSONObject source = JSON.parseObject("{\"configSnapshots\":{\"productionProcessConfigs\":["
                + "{\"routeProcessId\":10,\"processId\":20,\"parameterRules\":["
                + "{\"routeProcessId\":10,\"processId\":20,\"parameterCode\":\"TEMP\"}]}]}}");
        MesProRouteVersionDO version = MesProRouteVersionDO.builder()
                .id(1L).routeSnapshotJson(source.toJSONString()).build();
        JSONObject inherited = new JSONObject();

        ReflectionTestUtils.invokeMethod(new MesProRouteServiceImpl(),
                "inheritCopiedProductionProcessConfigs", inherited, version, Map.of(10L, 100L));

        JSONObject copied = inherited.getJSONArray("productionProcessConfigs").getJSONObject(0);
        assertEquals(100L, copied.getLong("routeProcessId"));
        assertEquals(100L, copied.getJSONArray("parameterRules").getJSONObject(0).getLong("routeProcessId"));
        assertEquals(20L, copied.getJSONArray("parameterRules").getJSONObject(0).getLong("processId"));
        assertEquals(10L, JSON.parseObject(version.getRouteSnapshotJson())
                .getJSONObject("configSnapshots").getJSONArray("productionProcessConfigs")
                .getJSONObject(0).getJSONArray("parameterRules").getJSONObject(0).getLong("routeProcessId"));
    }

    @Test
    void rejectsParameterBelongingToAnotherProcess() {
        MesProRouteVersionDO version = MesProRouteVersionDO.builder().id(1L)
                .routeSnapshotJson("{\"configSnapshots\":{\"productionProcessConfigs\":["
                        + "{\"routeProcessId\":10,\"parameterRules\":[{\"routeProcessId\":11}]}]}}")
                .build();
        assertThrows(RuntimeException.class, () -> ReflectionTestUtils.invokeMethod(
                new MesProRouteServiceImpl(), "inheritCopiedProductionProcessConfigs",
                new JSONObject(), version, Map.of(10L, 100L)));
    }
}
