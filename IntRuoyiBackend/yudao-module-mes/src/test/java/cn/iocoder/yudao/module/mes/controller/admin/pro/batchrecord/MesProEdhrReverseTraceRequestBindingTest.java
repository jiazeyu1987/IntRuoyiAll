package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.TargetScope;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.web.bind.WebDataBinder;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProEdhrReverseTraceRequestBindingTest {

    @Test
    void targetScopeBindsFrontendDateTimeQueryParameters() {
        TargetScope targetScope = new TargetScope().setKind("RELEASED_HISTORY");
        WebDataBinder binder = new WebDataBinder(targetScope);
        binder.setConversionService(new DefaultFormattingConversionService());

        binder.bind(new MutablePropertyValues(Map.of(
                "releaseApprovedFrom", "2026-09-28 08:30:00",
                "releaseApprovedTo", "2026-09-28 18:45:00")));

        assertFalse(binder.getBindingResult().hasErrors(), binder.getBindingResult().toString());
        assertEquals(LocalDateTime.of(2026, 9, 28, 8, 30), targetScope.getReleaseApprovedFrom());
        assertEquals(LocalDateTime.of(2026, 9, 28, 18, 45), targetScope.getReleaseApprovedTo());
    }

    @Test
    void targetScopeJsonContractRemainsTimestampBased() throws Exception {
        TargetScope parsed = JsonUtils.getObjectMapper().readValue("""
                {
                  "kind": "RELEASED_HISTORY",
                  "releaseApprovedFrom": "2026-09-28 08:30:00",
                  "releaseApprovedTo": "2026-09-28 18:45:00"
                }
                """, TargetScope.class);

        assertEquals(LocalDateTime.of(2026, 9, 28, 8, 30), parsed.getReleaseApprovedFrom());
        assertEquals(LocalDateTime.of(2026, 9, 28, 18, 45), parsed.getReleaseApprovedTo());

        JsonNode serialized = JsonUtils.getObjectMapper()
                .readTree(JsonUtils.toJsonString(parsed));
        assertTrue(serialized.get("releaseApprovedFrom").isNumber());
        assertTrue(serialized.get("releaseApprovedTo").isNumber());
    }
}
