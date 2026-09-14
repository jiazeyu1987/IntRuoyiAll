package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordParsedCellTest {

    @Test
    void schema_shouldKeepCellRuleAuditFieldsUniqueAndBuildable() {
        assertEquals(1, countDeclaredField("reviewedCellRule"));
        assertEquals(1, countDeclaredField("cellRuleSource"));

        MesProBatchRecordParsedCell cell = MesProBatchRecordParsedCell.builder()
                .reviewedCellRule(true)
                .cellRuleSource("manual-review")
                .inputType("Checkbox")
                .build();

        assertTrue(cell.isReviewedCellRule());
        assertEquals("manual-review", cell.getCellRuleSource());
        assertEquals("Checkbox", cell.getInputType());
    }

    private long countDeclaredField(String name) {
        return Arrays.stream(MesProBatchRecordParsedCell.class.getDeclaredFields())
                .filter(field -> name.equals(field.getName()))
                .count();
    }
}
