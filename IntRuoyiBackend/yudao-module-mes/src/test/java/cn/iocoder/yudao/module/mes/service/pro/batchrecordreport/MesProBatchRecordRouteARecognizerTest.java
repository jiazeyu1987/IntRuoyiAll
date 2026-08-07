package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordRouteARecognizerTest {

    private static final Path PILOT_SAMPLE = BatchRecordReportTestFixtures.pressurePumpRecordDoc();
    private static final Path FIXED_SAMPLE = Path.of(
            "D:\\ProjectPackage\\Int\\IntRuoyi\\resource\\\u6279\u8bb0\u5f55\u6a21\u677f.doc");

    private final MesProBatchRecordRouteARecognizer recognizer =
            new MesProBatchRecordRouteARecognizer(TestBatchRecordFixtures.wordParser());

    @Test
    void recognizePilotSample_returnsFifteenBusinessTemplates() throws Exception {
        byte[] bytes = Files.readAllBytes(PILOT_SAMPLE);

        List<MesProBatchRecordParsedTable> tables = recognizer.recognize(bytes);

        assertEquals(15, tables.size());
        assertEquals(1, tables.get(0).getSourceTableIndex());
        assertEquals(15, tables.get(14).getSourceTableIndex());
        assertEquals("产品信息", tables.get(0).getTableTitle());
        assertFalse(tables.get(0).getTableTitle().isBlank());
        assertTrue(tables.stream().allMatch(table -> !table.getRows().isEmpty()));
    }

    @Test
    void recognizeFixedSourceDoc_keepsProductInfoAsTheFirstReport() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.exists(FIXED_SAMPLE),
                "fixed sample doc fixture is not available on this machine");
        byte[] bytes = Files.readAllBytes(FIXED_SAMPLE);

        List<MesProBatchRecordParsedTable> tables = recognizer.recognize(bytes);

        assertEquals(15, tables.size());
        assertEquals("产品信息", tables.get(0).getTableTitle());
        assertTrue(tables.get(0).getRowCount() > 14);
        assertContainsText(tables.get(0), "配件进货批号信息");
        assertContainsText(tables.get(0), "装配及包装信息");
        assertContainsText(tables.get(0), "过程放行信息");
    }

    @Test
    void recognize_invalidDocBytes_failFast() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> recognizer.recognize("not-a-doc".getBytes(StandardCharsets.UTF_8)));

        assertEquals(PRO_BATCH_RECORD_REPORT_PARSE_FAILED.getCode(), exception.getCode());
    }

    private static void assertContainsText(MesProBatchRecordParsedTable table, String expectedText) {
        boolean found = table.getRows().stream()
                .flatMap(List::stream)
                .map(MesProBatchRecordParsedCell::getText)
                .filter(text -> text != null)
                .anyMatch(text -> text.contains(expectedText));
        assertTrue(found, "missing text: " + expectedText);
    }
}
