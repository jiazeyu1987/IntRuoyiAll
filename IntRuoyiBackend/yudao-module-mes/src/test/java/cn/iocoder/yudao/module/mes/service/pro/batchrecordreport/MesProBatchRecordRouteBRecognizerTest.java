package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordRouteBRecognizerTest {

    private static final Path PILOT_SAMPLE = BatchRecordReportTestFixtures.pressurePumpRecordDoc();
    private static final Path FIXED_SAMPLE = Path.of(
            "D:\\ProjectPackage\\Int\\IntRuoyi\\resource\\\u6279\u8bb0\u5f55\u6a21\u677f.doc");
    private static final Path PRESS_BALLOON_PUMP_SAMPLE = Path.of(
            "E:\\\u6279\u8BB0\u5F55\\RE-PP-IDPR-01\uFF08A 1\uFF09 \u6309\u538B\u5F0F\u7403\u56CA\u6269\u5F20\u538B\u529B\u6CF5\u751F\u4EA7\u8BB0\u5F55--2026.02.02\u751F\u6548.doc");

    private static final List<String> EXPECTED_TITLES = List.of(
            "\u4ea7\u54c1\u4fe1\u606f",
            "\u7c97\u6d17\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u7cbe\u6d17\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u6e05\u6d17\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u6e05\u6d01\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u7ec4\u88c5\u2160\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u5149\u56fa\u2160\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u7845\u5316\u2160\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u7845\u5316\u2161\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u7ec4\u88c5\u2161\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u68c0\u6d4b\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u5149\u56fa\u2161\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u5355\u5305\u88c5\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u4e2d\u5305\u88c5\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u5927\u5305\u88c5\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55"
    );
    private static final List<String> FIXED_SOURCE_EXPECTED_TITLES = List.of(
            "\u4ea7\u54c1\u4fe1\u606f",
            "\u7c97\u6d17\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u7cbe\u6d17\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u6e05\u6d17\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u6e05\u6d01\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u7ec4\u88c5\u2160\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u5149\u56fa\u2160\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u7845\u5316\u2160\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u7845\u5316\u2161\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u7ec4\u88c5\u2161\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u68c0\u6d4b\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u5149\u56fa\u2161\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u5355\u5305\u88c5\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u4e2d\u5305\u88c5\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u5927\u5305\u88c5\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55"
    );
    private static final List<String> PRESS_BALLOON_PUMP_EXPECTED_TITLES = List.of(
            "\u4ea7\u54c1\u4fe1\u606f",
            "\u7c97\u6d17\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u7cbe\u6d17\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u6e05\u6d17\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u6e05\u6d01\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u7ec4\u88c5\u2160\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u7845\u5316\u2160\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u7ec4\u88c5\u2161\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u5149\u56fa\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u7845\u5316\u2161\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u7845\u5316\u2162\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u7ec4\u88c5\u2162\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u68c0\u6d4b\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u5355\u5305\u88c5\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u4e2d\u5305\u88c5\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u5927\u5305\u88c5\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55"
    );

    private static final List<Integer> EXPECTED_ROW_COUNTS = List.of(
            46, 19, 19, 37, 21, 17, 17, 19, 18, 19, 19, 19, 23, 17, 17
    );

    private final MesProBatchRecordRouteBRecognizer recognizer = new MesProBatchRecordRouteBRecognizer();

    @Test
    void recognizePilotSample_returnsFifteenBusinessTemplatesViaWordCom() throws Exception {
        Assumptions.assumeTrue(Files.exists(PILOT_SAMPLE), "pilot sample doc fixture is not available on this machine");
        byte[] bytes = Files.readAllBytes(PILOT_SAMPLE);
        MesProBatchRecordRouteBRecognizer recognizer = new MesProBatchRecordRouteBRecognizer();

        List<MesProBatchRecordParsedTable> tables = recognizer.recognize(
                PILOT_SAMPLE,
                bytes,
                PILOT_SAMPLE.getFileName().toString());

        assertEquals(15, tables.size());
        assertEquals(1, tables.get(0).getSourceTableIndex());
        assertEquals(15, tables.get(14).getSourceTableIndex());
        assertIterableEquals(EXPECTED_TITLES,
                tables.stream().map(MesProBatchRecordParsedTable::getTableTitle).toList());
        assertIterableEquals(EXPECTED_ROW_COUNTS,
                tables.stream().map(MesProBatchRecordParsedTable::getRowCount).toList());
        assertTrue(tables.stream().allMatch(table -> !table.getRows().isEmpty()));
    }

    @Test
    void recognizeFixedSourceDoc_doesNotSplitInternalSummarySectionsIntoExtraTemplates() throws Exception {
        Assumptions.assumeTrue(Files.exists(FIXED_SAMPLE), "fixed sample doc fixture is not available on this machine");
        byte[] bytes = Files.readAllBytes(FIXED_SAMPLE);

        List<MesProBatchRecordParsedTable> tables = recognizer.recognize(
                FIXED_SAMPLE,
                bytes,
                FIXED_SAMPLE.getFileName().toString());

        assertEquals(15, tables.size());
        assertIterableEquals(FIXED_SOURCE_EXPECTED_TITLES,
                tables.stream().map(MesProBatchRecordParsedTable::getTableTitle).toList());
        assertEquals("\u4ea7\u54c1\u4fe1\u606f", tables.get(0).getTableTitle());
        assertTrue(tables.get(0).getRowCount() > 14);
        assertContainsText(tables.get(0), "\u4ea7\u54c1\u4fe1\u606f");
        assertContainsText(tables.get(0), "\u914d\u4ef6\u8fdb\u8d27\u6279\u53f7\u4fe1\u606f");
        assertContainsText(tables.get(0), "\u88c5\u914d\u53ca\u5305\u88c5\u4fe1\u606f");
        assertContainsText(tables.get(0), "\u8fc7\u7a0b\u653e\u884c\u4fe1\u606f");
        assertTrue(tables.stream().noneMatch(table -> "\u751f\u4ea7\u6279\u91cf\u6c47\u603b".equals(table.getTableTitle())));
    }

    @Test
    void recognizePressBalloonPumpSample_returnsFifteenTemplatesViaWordCom() throws Exception {
        Assumptions.assumeTrue(Files.exists(PRESS_BALLOON_PUMP_SAMPLE),
                "press balloon pump sample doc fixture is not available on this machine");
        byte[] bytes = Files.readAllBytes(PRESS_BALLOON_PUMP_SAMPLE);

        List<MesProBatchRecordParsedTable> tables = recognizer.recognize(
                PRESS_BALLOON_PUMP_SAMPLE,
                bytes,
                PRESS_BALLOON_PUMP_SAMPLE.getFileName().toString());

        assertEquals(16, tables.size());
        assertIterableEquals(PRESS_BALLOON_PUMP_EXPECTED_TITLES,
                tables.stream().map(MesProBatchRecordParsedTable::getTableTitle).toList());
        assertEquals(1, tables.get(0).getSourceTableIndex());
        assertEquals(16, tables.get(15).getSourceTableIndex());
        assertTrue(tables.stream().allMatch(table -> !table.getRows().isEmpty()));
    }

    @Test
    void recognizePilotSample_keepsMaterialMatrixHeaderAsHorizontalColumns() throws Exception {
        Assumptions.assumeTrue(Files.exists(PILOT_SAMPLE), "pilot sample doc fixture is not available on this machine");
        byte[] bytes = Files.readAllBytes(PILOT_SAMPLE);

        List<MesProBatchRecordParsedTable> tables = recognizer.recognize(
                PILOT_SAMPLE,
                bytes,
                PILOT_SAMPLE.getFileName().toString());

        MesProBatchRecordParsedTable materialMatrixTable = tables.stream()
                .filter(table -> containsText(table, "物料编码") && containsText(table, "齿条"))
                .findFirst()
                .orElse(null);
        assertNotNull(materialMatrixTable, "必须能定位到物料编码/齿条所在的并列表格");

        List<MesProBatchRecordParsedCell> headerRow = materialMatrixTable.getRows().stream()
                .filter(row -> row.stream().anyMatch(cell -> "物料编码".equals(cell.getText())))
                .findFirst()
                .orElse(List.of());

        assertTrue(headerRow.size() >= 6,
                "物料表表头必须保持至少 6 个横向单元格，而不是折叠成单列纵向文本");
        assertEquals("物料编码", headerRow.get(0).getText());
        assertEquals(1, headerRow.get(0).getColSpan());
        assertEquals("物料名称", headerRow.get(1).getText());
        assertEquals(1, headerRow.get(1).getColSpan());
        assertTrue(headerRow.get(2).getText().contains("批号"));
        assertEquals(1, headerRow.get(2).getColSpan());
        assertEquals("物料编码", headerRow.get(3).getText());
        assertEquals(1, headerRow.get(3).getColSpan());
        assertEquals("物料名称", headerRow.get(4).getText());
        assertEquals(1, headerRow.get(4).getColSpan());
        assertTrue(headerRow.get(5).getText().contains("批号"));
        assertEquals(1, headerRow.get(5).getColSpan());
    }

    @Test
    void recognizePilotSample_assemblyOneShouldPreserveWordVisualGridBoundaries() throws Exception {
        Assumptions.assumeTrue(Files.exists(PILOT_SAMPLE), "pilot sample doc fixture is not available on this machine");
        byte[] bytes = Files.readAllBytes(PILOT_SAMPLE);

        List<MesProBatchRecordParsedTable> tables = recognizer.recognize(
                PILOT_SAMPLE,
                bytes,
                PILOT_SAMPLE.getFileName().toString());

        MesProBatchRecordParsedTable assemblyOne = tables.stream()
                .filter(table -> "组装Ⅰ工序生产记录".equals(table.getTableTitle()))
                .findFirst()
                .orElseThrow();

        assertEquals(162, assemblyOne.getColumnCount(),
                "组装Ⅰ工序生产记录必须保留 Word 细粒度视觉网格列，而不是压缩为粗粒度合并列");
        assertNotNull(assemblyOne.getColumnWidths(), "Route B 必须输出 Word 视觉列宽向量");
        assertEquals(assemblyOne.getColumnCount(), assemblyOne.getColumnWidths().size(),
                "视觉列宽向量数量必须与列数一致");
        assertTrue(assemblyOne.getColumnWidths().stream().allMatch(width -> width > 0),
                "视觉列宽必须全部为正数");

        List<MesProBatchRecordParsedCell> packedRow = assemblyOne.getRows().stream()
                .filter(row -> row.stream().anyMatch(cell -> "组装Ⅰ生产操作及自检记录".equals(cell.getText())))
                .findFirst()
                .orElseThrow();
        assertEquals(2, packedRow.size());
        assertEquals(0, packedRow.get(0).getColumnIndex());
        assertEquals(1, packedRow.get(0).getColSpan());
        assertNotNull(packedRow.get(1).getColumnIndex(), "右侧合并单元格必须保留起始视觉列");
        assertEquals(assemblyOne.getColumnCount() - 1,
                packedRow.get(1).getColumnIndex() + packedRow.get(1).getColSpan(),
                "右侧合并单元格必须保留 Word 原始跨度，不得用尾列填充改写源网格");
    }

    @Test
    void recognizePilotSample_assemblyOneChecklistWidthHintsShouldPreserveDocLikeProportions() throws Exception {
        Assumptions.assumeTrue(Files.exists(PILOT_SAMPLE), "pilot sample doc fixture is not available on this machine");
        byte[] bytes = Files.readAllBytes(PILOT_SAMPLE);

        MesProBatchRecordParsedTable assemblyOne = recognizer.recognize(
                        PILOT_SAMPLE,
                        bytes,
                        PILOT_SAMPLE.getFileName().toString())
                .stream()
                .filter(table -> "组装Ⅰ工序生产记录".equals(table.getTableTitle()))
                .findFirst()
                .orElseThrow();

        List<MesProBatchRecordParsedCell> checklistHeaderRow = assemblyOne.getRows().stream()
                .filter(row -> row.stream().anyMatch(cell -> "检查要求".equals(cell.getText())))
                .findFirst()
                .orElseThrow();

        assertEquals(5, checklistHeaderRow.size());
        int sideWidth = checklistHeaderRow.get(0).getWidthPx();
        int narrativeWidth = checklistHeaderRow.get(1).getWidthPx();
        int resultWidth = checklistHeaderRow.get(2).getWidthPx();
        int operatorWidth = checklistHeaderRow.get(3).getWidthPx();
        int reviewerWidth = checklistHeaderRow.get(4).getWidthPx();

        assertTrue(sideWidth < resultWidth,
                "Route B 识别阶段应保留窄侧栏而不是把侧栏识别得比结果区更宽: side=" + sideWidth + ", result=" + resultWidth);
        assertTrue(narrativeWidth > resultWidth,
                "Route B 识别阶段应保留宽正文而不是把检查要求压窄: narrative=" + narrativeWidth + ", result=" + resultWidth);
        assertTrue(Math.abs(resultWidth - operatorWidth) <= 24,
                "尾部三列宽度线索应接近: result=" + resultWidth + ", operator=" + operatorWidth + ", reviewer=" + reviewerWidth);
        assertTrue(Math.abs(resultWidth - reviewerWidth) <= 24,
                "尾部三列宽度线索应接近: result=" + resultWidth + ", operator=" + operatorWidth + ", reviewer=" + reviewerWidth);
    }

    @Test
    void recognize_whenPythonCommandMissing_failFast() {
        MesProBatchRecordRouteBRecognizer recognizer = new MesProBatchRecordRouteBRecognizer(
                "__missing_route_b_python__",
                Path.of(System.getProperty("java.io.tmpdir")),
                5000L);

        ServiceException exception = assertThrows(ServiceException.class, () ->
                recognizer.recognize(PILOT_SAMPLE, new byte[]{1}, PILOT_SAMPLE.getFileName().toString()));

        assertEquals(PRO_BATCH_RECORD_REPORT_PARSE_FAILED.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("route_b_python_process_start_failed"));
    }

    @Test
    void recognize_whenPythonProcessTimesOut_terminatesChildProcessTree() throws Exception {
        Path tempDir = Files.createTempDirectory("route-b-timeout-process-tree-");
        Path fakePython = tempDir.resolve("fake-python-with-child.cmd");
        Path childPidFile = tempDir.resolve("child.pid");
        try {
            Files.writeString(fakePython, """
                    @echo off
                    setlocal
                    set "PID_FILE=%~dp0child.pid"
                    powershell -NoProfile -ExecutionPolicy Bypass -Command "$child = Start-Process -FilePath powershell -ArgumentList '-NoProfile','-Command','Start-Sleep -Seconds 30' -PassThru; [System.IO.File]::WriteAllText($env:PID_FILE, [string]$child.Id); Start-Sleep -Seconds 30"
                    """);

            MesProBatchRecordRouteBRecognizer recognizer = new MesProBatchRecordRouteBRecognizer(
                    fakePython.toString(),
                    tempDir,
                    800L);

            ServiceException exception = assertThrows(ServiceException.class, () ->
                    recognizer.recognize(null, new byte[]{1}, "source.doc"));

            assertEquals(PRO_BATCH_RECORD_REPORT_PARSE_FAILED.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("route_b_python_process_timeout"));
            long childPid = waitForChildPid(childPidFile);
            assertTrue(!isProcessAlive(childPid),
                    "Route B timeout must terminate child process tree; leaked child pid=" + childPid);
        } finally {
            cleanupChildProcess(childPidFile);
            deleteRecursively(tempDir);
        }
    }

    @Test
    void routeBTimeoutFieldDeclaresSpringConfigurationProperty() throws Exception {
        Field timeoutField = MesProBatchRecordRouteBRecognizer.class.getDeclaredField("timeoutMs");

        Value value = timeoutField.getAnnotation(Value.class);

        assertNotNull(value, "Route B 超时时间必须接入 Spring 配置，不能继续硬编码固定 180 秒");
        assertTrue(value.value().contains("yudao.mes.batch-record-report.route-b.timeout-ms"));
    }

    @Test
    void routeBDefaultTimeout_supportsLargeWordComDocuments() {
        Long timeoutMs = (Long) ReflectionTestUtils.getField(new MesProBatchRecordRouteBRecognizer(), "timeoutMs");

        assertEquals(600_000L, timeoutMs);
    }

    @Test
    void recognizePilotSample_assemblyOneOperationBandShouldKeepSharedVerticalSideHeader() throws Exception {
        Assumptions.assumeTrue(Files.exists(PILOT_SAMPLE), "pilot sample doc fixture is not available on this machine");
        byte[] bytes = Files.readAllBytes(PILOT_SAMPLE);

        MesProBatchRecordParsedTable assemblyOne = recognizer.recognize(
                        PILOT_SAMPLE,
                        bytes,
                        PILOT_SAMPLE.getFileName().toString())
                .stream()
                .filter(table -> "组装Ⅰ工序生产记录".equals(table.getTableTitle()))
                .findFirst()
                .orElseThrow();

        List<MesProBatchRecordParsedCell> operationHeaderRow = assemblyOne.getRows().stream()
                .filter(row -> row.stream().anyMatch(cell -> "组装Ⅰ生产操作及自检记录".equals(cell.getText())))
                .findFirst()
                .orElseThrow();

        assertEquals("组装Ⅰ生产操作及自检记录", operationHeaderRow.get(0).getText());
        assertTrue(Math.max(1, operationHeaderRow.get(0).getRowSpan()) >= 4,
                "Route B 识别阶段应保留跨物料区与自检区的纵向主带，而不是只剩单行标题: rowSpan="
                        + operationHeaderRow.get(0).getRowSpan());
    }

    @Test
    void resolveRepresentativeTitle_prefersStandaloneInfoTitleOverGenericFallback() {
        List<List<MesProBatchRecordParsedCell>> rows = List.of(
                row("球囊扩张压力泵生产记录", "记录编号", "RE-PP-ID-01"),
                row("装配及包装信息"),
                row("工序名称", "操作人员", "装配日期")
        );

        String title = recognizer.resolveRepresentativeTitle("产品信息", rows);

        assertEquals("装配及包装信息", title);
    }

    @Test
    void isGenericTemplateHeaderRow_acceptsStandaloneShortInfoTitleButRejectsDocumentHeader() {
        assertTrue(recognizer.isGenericTemplateHeaderRow(row("装配及包装信息")));
        assertTrue(recognizer.isGenericTemplateHeaderRow(row("粗洗工序生产记录")));
        assertTrue(recognizer.isGenericTemplateHeaderRow(row("生产记录汇总表")));
        assertTrue(recognizer.isGenericTemplateHeaderRow(row("产品信息")));
        assertTrue(recognizer.isGenericTemplateHeaderRow(row("清洁工序生产记录")));

        assertTrue(!recognizer.isGenericTemplateHeaderRow(row("球囊扩张压力泵生产记录", "记录编号", "RE-PP-ID-01")));
        assertTrue(!recognizer.isGenericTemplateHeaderRow(row("检查要求", "结果", "操作人/日期", "复核人/日期")));
    }

    private static List<MesProBatchRecordParsedCell> row(String... texts) {
        List<MesProBatchRecordParsedCell> row = new ArrayList<>();
        for (String text : texts) {
            row.add(MesProBatchRecordParsedCell.builder()
                    .text(text)
                    .rowSpan(1)
                    .colSpan(1)
                    .bold(false)
                    .fontSize(10)
                    .horizontalAlign("center")
                    .verticalAlign("middle")
                    .widthPx(120)
                    .heightPx(24)
                    .build());
        }
        return row;
    }

    private static void assertContainsText(MesProBatchRecordParsedTable table, String expectedText) {
        assertTrue(containsText(table, expectedText), "missing text: " + expectedText);
    }

    private static boolean containsText(MesProBatchRecordParsedTable table, String expectedText) {
        return table.getRows().stream()
                .flatMap(List::stream)
                .map(MesProBatchRecordParsedCell::getText)
                .filter(text -> text != null)
                .anyMatch(text -> text.contains(expectedText));
    }

    private static long waitForChildPid(Path childPidFile) throws Exception {
        long deadline = System.currentTimeMillis() + 5_000L;
        while (System.currentTimeMillis() < deadline) {
            if (Files.exists(childPidFile) && Files.size(childPidFile) > 0) {
                return Long.parseLong(Files.readString(childPidFile).trim());
            }
            Thread.sleep(100L);
        }
        throw new AssertionError("child pid file was not written: " + childPidFile);
    }

    private static boolean isProcessAlive(long pid) {
        return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
    }

    private static void cleanupChildProcess(Path childPidFile) throws Exception {
        if (Files.notExists(childPidFile) || Files.size(childPidFile) == 0) {
            return;
        }
        long childPid = Long.parseLong(Files.readString(childPidFile).trim());
        ProcessHandle.of(childPid).ifPresent(handle -> {
            if (handle.isAlive()) {
                handle.destroyForcibly();
            }
        });
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
}
