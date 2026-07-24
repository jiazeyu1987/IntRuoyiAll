package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordFillablePatternSupportTest {

    @Test
    void splitTrailingUnderlineFillable_whenCheckboxOtherHasUnderline_returnsTwoCheckboxLabelsAndTextInput() {
        List<MesProBatchRecordParsedCell> cells =
                MesProBatchRecordFillablePatternSupport.splitTrailingUnderlineFillable(
                        "□报废   □其他：______________",
                        240,
                        32,
                        MesProBatchRecordReportShapeRules.INPUT_TYPE_INPUT);

        assertEquals(3, cells.size());
        assertEquals("□报废", cells.get(0).getText());
        assertFalse(cells.get(0).isFillable());
        assertEquals("□其他：", cells.get(1).getText());
        assertFalse(cells.get(1).isFillable());
        assertEquals("", cells.get(2).getText());
        assertTrue(cells.get(2).isFillable());
        assertEquals(MesProBatchRecordReportShapeRules.INPUT_TYPE_INPUT, cells.get(2).getInputType());
    }

    @Test
    void splitTrailingUnderlineFillable_whenNoUnderline_failsFast() {
        assertThrows(IllegalArgumentException.class,
                () -> MesProBatchRecordFillablePatternSupport.splitTrailingUnderlineFillable(
                        "□报废   □其他：",
                        240,
                        32,
                        MesProBatchRecordReportShapeRules.INPUT_TYPE_INPUT));
    }
}
