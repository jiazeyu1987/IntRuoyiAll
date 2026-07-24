package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordRouteFRecognizerTest {

    private static final Path PILOT_SAMPLE = Path.of(
            "C:\\Users\\BJB110\\Desktop\\2\\2\\RE-PP-ID-01\uFF08A 1\uFF09\u7403\u56CA\u6269\u5F20\u538B\u529B\u6CF5\u751F\u4EA7\u8BB0\u5F55(1).doc");

    @Test
    void recognize_roundTripsPilotSampleThroughExcelIntermediate() throws Exception {
        byte[] bytes = Files.readAllBytes(PILOT_SAMPLE);
        MesProBatchRecordDocParser docParser = new MesProBatchRecordDocParser();
        MesProBatchRecordRouteFRecognizer recognizer = new MesProBatchRecordRouteFRecognizer(docParser);

        List<MesProBatchRecordParsedTable> expected = docParser.parse(bytes);
        List<MesProBatchRecordParsedTable> actual = recognizer.recognize(bytes);

        assertEquals(15, actual.size());
        assertIterableEquals(
                expected.stream().map(MesProBatchRecordParsedTable::getSourceTableIndex).collect(Collectors.toList()),
                actual.stream().map(MesProBatchRecordParsedTable::getSourceTableIndex).collect(Collectors.toList()));
        assertIterableEquals(
                expected.stream().map(MesProBatchRecordParsedTable::getTableTitle).collect(Collectors.toList()),
                actual.stream().map(MesProBatchRecordParsedTable::getTableTitle).collect(Collectors.toList()));
        assertIterableEquals(
                expected.stream().map(MesProBatchRecordParsedTable::getRowCount).collect(Collectors.toList()),
                actual.stream().map(MesProBatchRecordParsedTable::getRowCount).collect(Collectors.toList()));
        assertIterableEquals(
                expected.stream().map(MesProBatchRecordParsedTable::getColumnCount).collect(Collectors.toList()),
                actual.stream().map(MesProBatchRecordParsedTable::getColumnCount).collect(Collectors.toList()));
    }

    @Test
    void recognize_failsFastWhenExcelIntermediateIsMissing() throws Exception {
        byte[] bytes = Files.readAllBytes(PILOT_SAMPLE);
        MesProBatchRecordRouteFRecognizer recognizer = new MesProBatchRecordRouteFRecognizer(
                new MesProBatchRecordDocParser()) {
            @Override
            protected byte[] buildExcelIntermediate(List<MesProBatchRecordParsedTable> sourceTables) {
                return new byte[0];
            }
        };

        ServiceException exception = assertThrows(ServiceException.class, () -> recognizer.recognize(bytes));

        assertEquals(PRO_BATCH_RECORD_REPORT_PARSE_FAILED.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("route_f_excel_intermediate_empty"));
    }

    @Test
    void parseExcelIntermediate_preservesImplicitFullWidthRows() {
        MesProBatchRecordRouteFRecognizer recognizer = new MesProBatchRecordRouteFRecognizer(new MesProBatchRecordDocParser());

        MesProBatchRecordParsedTable sourceTable = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("产品信息")
                .rowCount(2)
                .columnCount(6)
                .rows(List.of(
                        List.of(cell("产品信息", 540)),
                        List.of(
                                cell("A", 100),
                                cell("B", 100),
                                cell("C", 100),
                                cell("D", 100),
                                cell("E", 100),
                                cell("F", 100)
                        )))
                .build();

        MesProBatchRecordParsedTable roundTripped = roundTrip(recognizer, sourceTable);

        assertEquals(6, roundTripped.getRows().get(0).get(0).getColSpan());
    }

    @Test
    void parseExcelIntermediate_preservesTrailingImplicitMergedSegments() {
        MesProBatchRecordRouteFRecognizer recognizer = new MesProBatchRecordRouteFRecognizer(new MesProBatchRecordDocParser());

        MesProBatchRecordParsedTable sourceTable = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(10)
                .tableTitle("组装Ⅱ工序生产记录")
                .rowCount(2)
                .columnCount(6)
                .rows(List.of(
                        List.of(
                                cell("组装Ⅱ生产操作及自检记录", 100),
                                cell("物料编码\n物料名称\n批号", 500)
                        ),
                        List.of(
                                cell("A", 100),
                                cell("B", 100),
                                cell("C", 100),
                                cell("D", 100),
                                cell("E", 100),
                                cell("F", 100)
                        )))
                .build();

        MesProBatchRecordParsedTable roundTripped = roundTrip(recognizer, sourceTable);

        assertEquals(1, roundTripped.getRows().get(0).get(0).getColSpan());
        assertEquals(5, roundTripped.getRows().get(0).get(1).getColSpan());
    }

    private MesProBatchRecordParsedTable roundTrip(MesProBatchRecordRouteFRecognizer recognizer,
                                                   MesProBatchRecordParsedTable sourceTable) {
        byte[] excelBytes = recognizer.buildExcelIntermediate(List.of(sourceTable));
        return recognizer.parseExcelIntermediate(excelBytes).get(0);
    }

    private MesProBatchRecordParsedCell cell(String text, int widthPx) {
        return MesProBatchRecordParsedCell.builder()
                .text(text)
                .widthPx(widthPx)
                .heightPx(36)
                .build();
    }
}
