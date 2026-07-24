package cn.iocoder.yudao.module.erp.service.purchase.sync;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpKingdeeIncrementalQuerySpecTest {

    @Test
    void toQuery_shouldBuildModifyTimeWindowAndStableOrder() {
        ErpKingdeeIncrementalQuerySpec spec = ErpKingdeeIncrementalQuerySpec.builder()
                .formId("BD_MATERIAL")
                .fieldKeys("FNumber,FName,FModifyDate")
                .baseFilter("(FNumber <> '')")
                .startRow(20)
                .limit(100)
                .build();

        Map<String, Object> query = spec.toQuery(
                LocalDateTime.of(2026, 1, 1, 8, 30, 0),
                LocalDateTime.of(2026, 1, 2, 8, 30, 0));

        assertEquals("BD_MATERIAL", query.get("FormId"));
        assertEquals("FNumber,FName,FModifyDate", query.get("FieldKeys"));
        assertEquals("FModifyDate ASC", query.get("OrderString"));
        assertEquals(20, query.get("StartRow"));
        assertEquals(100, query.get("Limit"));
        assertTrue(query.get("FilterString").toString().contains("(FNumber <> '')"));
        assertTrue(query.get("FilterString").toString()
                .contains("(FModifyDate >= '2026-01-01 08:30:00' and FModifyDate < '2026-01-02 08:30:00')"));
    }

    @Test
    void toQuery_shouldRejectMissingModifyTimeField() {
        ErpKingdeeIncrementalQuerySpec spec = ErpKingdeeIncrementalQuerySpec.builder()
                .formId("BD_MATERIAL")
                .fieldKeys("FNumber,FName")
                .baseFilter("(FNumber <> '')")
                .startRow(0)
                .limit(100)
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> spec.toQuery(LocalDateTime.now().minusDays(1), LocalDateTime.now()));

        assertTrue(exception.getMessage().contains("FModifyDate"));
    }

    @Test
    void toQuery_shouldRejectInvalidWindow() {
        ErpKingdeeIncrementalQuerySpec spec = ErpKingdeeIncrementalQuerySpec.builder()
                .formId("BD_MATERIAL")
                .fieldKeys("FNumber,FModifyDate")
                .startRow(0)
                .limit(100)
                .build();

        LocalDateTime time = LocalDateTime.of(2026, 1, 1, 8, 30, 0);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> spec.toQuery(time, time));

        assertTrue(exception.getMessage().contains("windowEnd"));
    }

}
