package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordRouteDRecognizerTest {

    private static final String GENERIC_PROCESS_TITLE = "灭菌工序生产记录";
    private static final List<String> EXPECTED_TITLES = List.of(
            "产品信息",
            "粗洗工序生产记录",
            "精洗工序生产记录",
            "清洗工序生产记录",
            "清洁工序生产记录",
            "组装Ⅰ工序生产记录",
            "光固Ⅰ工序生产记录",
            "硅化Ⅰ工序生产记录",
            "硅化Ⅱ工序生产记录",
            "组装Ⅱ工序生产记录",
            "检测工序生产记录",
            "光固Ⅱ工序生产记录",
            "单包装工序生产记录",
            "中包装工序生产记录",
            "大包装工序生产记录"
    );

    @Test
    void recognize_returnsFifteenTemplatesFromPdfTables() throws Exception {
        Path tempDir = Files.createTempDirectory("route-d-recognizer-");
        Path fakePython = tempDir.resolve("fake-python.cmd");
        try {
            Files.writeString(fakePython, """
                    @echo off
                    setlocal
                    set "PDF_TARGET=%4"
                    > "%PDF_TARGET%" echo fake-pdf
                    echo route-d-test-ok
                    """, StandardCharsets.UTF_8);

            MesProBatchRecordRouteDRecognizer recognizer = new RouteDPdfTableStub(createPdfTableFixture());
            setField(recognizer, "pythonCommand", fakePython.toString());
            setField(recognizer, "pythonWorkingDirectory", tempDir.toString());
            setField(recognizer, "timeoutMs", 5000L);
            setField(recognizer, "pdfExportScript", "fake-script");

            List<MesProBatchRecordParsedTable> tables = recognizer.recognize("pilot.doc", loadPilotFixtureBytes());

            assertEquals(15, tables.size());
            assertEquals(EXPECTED_TITLES, tables.stream().map(MesProBatchRecordParsedTable::getTableTitle).toList());
            assertEquals(1, tables.get(0).getSourceTableIndex());
            assertEquals(15, tables.get(14).getSourceTableIndex());
            assertTrue(tables.stream().allMatch(table -> table.getRowCount() >= 3));
        } finally {
            deleteRecursively(tempDir);
        }
    }

    @Test
    void recognize_splitsGenericProcessHeaderNotInFixedTitleList() throws Exception {
        Path tempDir = Files.createTempDirectory("route-d-recognizer-generic-title-");
        Path fakePython = tempDir.resolve("fake-python-generic-title.cmd");
        try {
            Files.writeString(fakePython, """
                    @echo off
                    setlocal
                    set "PDF_TARGET=%4"
                    > "%PDF_TARGET%" echo fake-pdf
                    echo route-d-test-generic-title
                    """, StandardCharsets.UTF_8);

            List<String> expectedTitles = new ArrayList<>(EXPECTED_TITLES);
            expectedTitles.set(5, GENERIC_PROCESS_TITLE);

            MesProBatchRecordRouteDRecognizer recognizer = new RouteDPdfTableStub(createPdfTableFixture(expectedTitles));
            setField(recognizer, "pythonCommand", fakePython.toString());
            setField(recognizer, "pythonWorkingDirectory", tempDir.toString());
            setField(recognizer, "timeoutMs", 5000L);
            setField(recognizer, "pdfExportScript", "fake-script");

            List<MesProBatchRecordParsedTable> tables = recognizer.recognize("pilot.doc", loadPilotFixtureBytes());

            assertEquals(15, tables.size());
            assertEquals(expectedTitles, tables.stream().map(MesProBatchRecordParsedTable::getTableTitle).toList());
            assertEquals(GENERIC_PROCESS_TITLE, tables.get(5).getTableTitle());
        } finally {
            deleteRecursively(tempDir);
        }
    }

    @Test
    void recognize_failsFastWhenPdfIsMissing() throws Exception {
        Path tempDir = Files.createTempDirectory("route-d-recognizer-missing-pdf-");
        Path fakePython = tempDir.resolve("fake-python-missing-pdf.cmd");
        try {
            Files.writeString(fakePython, """
                    @echo off
                    setlocal
                    echo route-d-test-missing-pdf
                    """, StandardCharsets.UTF_8);

            MesProBatchRecordRouteDRecognizer recognizer = new MesProBatchRecordRouteDRecognizer();
            setField(recognizer, "pythonCommand", fakePython.toString());
            setField(recognizer, "pythonWorkingDirectory", tempDir.toString());
            setField(recognizer, "timeoutMs", 5000L);
            setField(recognizer, "pdfExportScript", "fake-script");

            ServiceException exception = assertThrows(ServiceException.class,
                    () -> recognizer.recognize("pilot.doc", loadPilotFixtureBytes()));

            assertEquals(PRO_BATCH_RECORD_REPORT_PARSE_FAILED.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("route_d_pdf_missing"));
        } finally {
            deleteRecursively(tempDir);
        }
    }

    @Test
    void recognize_whenPythonCommandBlank_failsFastBeforeExternalProcess() throws Exception {
        MesProBatchRecordRouteDRecognizer recognizer = new MesProBatchRecordRouteDRecognizer();
        setField(recognizer, "pythonCommand", "   ");
        setField(recognizer, "timeoutMs", 5000L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> recognizer.recognize("pilot.doc", loadPilotFixtureBytes()));

        assertEquals(PRO_BATCH_RECORD_REPORT_PARSE_FAILED.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("route_d_python_command_missing"));
    }

    @Test
    void recognize_preservesObviousPdfColumnsAsSeparateCells() throws Exception {
        Path tempDir = Files.createTempDirectory("route-d-recognizer-structured-tables-");
        Path fakePython = tempDir.resolve("fake-python-structured-tables.cmd");
        try {
            Files.writeString(fakePython, """
                    @echo off
                    setlocal
                    set "PDF_TARGET=%4"
                    > "%PDF_TARGET%" echo fake-pdf
                    echo route-d-test-structured-tables
                    """, StandardCharsets.UTF_8);

            MesProBatchRecordRouteDRecognizer recognizer = new RouteDPdfTableStub(createStructuredPdfTableFixture());
            setField(recognizer, "pythonCommand", fakePython.toString());
            setField(recognizer, "pythonWorkingDirectory", tempDir.toString());
            setField(recognizer, "timeoutMs", 5000L);
            setField(recognizer, "pdfExportScript", "fake-script");

            List<MesProBatchRecordParsedTable> tables = recognizer.recognize("pilot.doc", loadPilotFixtureBytes());

            MesProBatchRecordParsedTable firstTable = tables.get(0);
            assertEquals(3, firstTable.getColumnCount());
            List<MesProBatchRecordParsedCell> headerRow = firstTable.getRows().stream()
                    .filter(row -> row.stream().anyMatch(cell -> "列A".equals(cell.getText())))
                    .findFirst()
                    .orElseThrow();
            assertEquals(List.of("列A", "列B", "列C"),
                    headerRow.stream().map(MesProBatchRecordParsedCell::getText).toList());
            List<MesProBatchRecordParsedCell> noteRow = firstTable.getRows().stream()
                    .filter(row -> row.stream().anyMatch(cell -> "说明段落".equals(cell.getText())))
                    .findFirst()
                    .orElseThrow();
            assertEquals(1, noteRow.size());
            assertEquals(firstTable.getColumnCount(), noteRow.get(0).getColSpan());
        } finally {
            deleteRecursively(tempDir);
        }
    }

    private static byte[] loadPilotFixtureBytes() throws Exception {
        try (InputStream inputStream = MesProBatchRecordRouteDRecognizerTest.class
                .getResourceAsStream("/fixtures/pressure-pump-record.doc")) {
            if (inputStream == null) {
                throw new IllegalStateException("missing fixture /fixtures/pressure-pump-record.doc");
            }
            return inputStream.readAllBytes();
        }
    }

    private static List<MesProBatchRecordRouteDRecognizer.RawPdfTable> createPdfTableFixture() {
        return createPdfTableFixture(EXPECTED_TITLES);
    }

    private static List<MesProBatchRecordRouteDRecognizer.RawPdfTable> createPdfTableFixture(List<String> titles) {
        List<MesProBatchRecordRouteDRecognizer.RawPdfTable> tables = new ArrayList<>();
        tables.add(table(row("球囊扩张压力泵生产记录", "记录编号", "RE-PP-ID-01"), row("", "版本", "A/1")));
        tables.add(table(
                row(span(titles.get(0), 8)),
                row("产品名称", "", "球囊扩张压力泵", "", "型号规格", "", "", ""),
                row("配件进货批号信息", "", "", "", "", "", "", ""),
                row("装配及包装信息", "", "", "", "", "", "", "")
        ));
        for (int index = 1; index < titles.size(); index++) {
            String title = titles.get(index);
            tables.add(table(
                    row(span(title + " □关键/特殊工序 ☑非关键/特殊工序", 6)),
                    row("列A", "列B", "列C", "", "", ""),
                    row("值A", "值B", "值C", "", "", ""),
                    row(span("说明段落", 6))
            ));
        }
        return tables;
    }

    private static List<MesProBatchRecordRouteDRecognizer.RawPdfTable> createStructuredPdfTableFixture() {
        return List.of(
                table(
                        row(span("产品信息", 3)),
                        row("列A", "列B", "列C"),
                        row("值A", "值B", "值C"),
                        row(span("说明段落", 3))
                ),
                table(row(span("粗洗工序生产记录", 3)), row("A", "B", "C")),
                table(row(span("精洗工序生产记录", 3)), row("A", "B", "C")),
                table(row(span("清洗工序生产记录", 3)), row("A", "B", "C")),
                table(row(span("清洁工序生产记录", 3)), row("A", "B", "C")),
                table(row(span("组装Ⅰ工序生产记录", 3)), row("A", "B", "C")),
                table(row(span("光固Ⅰ工序生产记录", 3)), row("A", "B", "C")),
                table(row(span("硅化Ⅰ工序生产记录", 3)), row("A", "B", "C")),
                table(row(span("硅化Ⅱ工序生产记录", 3)), row("A", "B", "C")),
                table(row(span("组装Ⅱ工序生产记录", 3)), row("A", "B", "C")),
                table(row(span("检测工序生产记录", 3)), row("A", "B", "C")),
                table(row(span("光固Ⅱ工序生产记录", 3)), row("A", "B", "C")),
                table(row(span("单包装工序生产记录", 3)), row("A", "B", "C")),
                table(row(span("中包装工序生产记录", 3)), row("A", "B", "C")),
                table(row(span("大包装工序生产记录", 3)), row("A", "B", "C"))
        );
    }

    private static MesProBatchRecordRouteDRecognizer.RawPdfTable table(List<MesProBatchRecordRouteDRecognizer.RawPdfCell>... rows) {
        MesProBatchRecordRouteDRecognizer.RawPdfTable table = new MesProBatchRecordRouteDRecognizer.RawPdfTable();
        table.setRows(List.of(rows));
        return table;
    }

    private static List<MesProBatchRecordRouteDRecognizer.RawPdfCell> row(String... cells) {
        List<MesProBatchRecordRouteDRecognizer.RawPdfCell> row = new ArrayList<>();
        for (String cellText : cells) {
            row.add(cell(cellText, 1));
        }
        return row;
    }

    private static List<MesProBatchRecordRouteDRecognizer.RawPdfCell> row(MesProBatchRecordRouteDRecognizer.RawPdfCell... cells) {
        return List.of(cells);
    }

    private static MesProBatchRecordRouteDRecognizer.RawPdfCell span(String text, int colSpan) {
        return cell(text, colSpan);
    }

    private static MesProBatchRecordRouteDRecognizer.RawPdfCell cell(String text, int colSpan) {
        MesProBatchRecordRouteDRecognizer.RawPdfCell cell = new MesProBatchRecordRouteDRecognizer.RawPdfCell();
        cell.setText(text);
        cell.setRowSpan(1);
        cell.setColSpan(colSpan);
        return cell;
    }

    private static void deleteRecursively(Path path) throws Exception {
        if (path == null || Files.notExists(path)) {
            return;
        }
        try (var stream = Files.walk(path)) {
            stream.sorted((left, right) -> right.getNameCount() - left.getNameCount())
                    .forEach(current -> {
                        try {
                            Files.deleteIfExists(current);
                        } catch (Exception ignored) {
                            // best-effort cleanup for test temp files
                        }
                    });
        }
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Class<?> type = target.getClass();
        java.lang.reflect.Field field = null;
        while (type != null && field == null) {
            try {
                field = type.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        if (field == null) {
            throw new NoSuchFieldException(fieldName);
        }
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class RouteDPdfTableStub extends MesProBatchRecordRouteDRecognizer {

        private final List<RawPdfTable> pdfTables;

        private RouteDPdfTableStub(List<RawPdfTable> pdfTables) {
            this.pdfTables = pdfTables;
        }

        @Override
        protected List<RawPdfTable> extractPdfTables(Path pdfPath) {
            return pdfTables;
        }
    }
}
