package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordReportLayoutCalibratorTest {

    private static final Path PILOT_SAMPLE = BatchRecordReportTestFixtures.pressurePumpRecordDoc();
    private static final Path FIXED_SAMPLE = Path.of(
            "D:\\ProjectPackage\\Int\\IntRuoyi\\resource\\批记录模板.doc");
    private static final Path PRESSURE_PUMP_SAMPLE = BatchRecordReportTestFixtures.pressurePumpRecordDoc();

    private final MesProBatchRecordDocParser parser = new MesProBatchRecordDocParser();
    private final MesProBatchRecordRouteBRecognizer routeBRecognizer = new MesProBatchRecordRouteBRecognizer();
    private final MesProBatchRecordReportLayoutCalibrator calibrator = new MesProBatchRecordReportLayoutCalibrator();

    @Test
    void calibrate_shouldAddDocHeaderAndFooterForProductInfoTemplate() throws Exception {
        assumePilotSampleAvailable();
        byte[] bytes = Files.readAllBytes(PILOT_SAMPLE);
        MesProBatchRecordParsedTable parsedTable = parser.parse(bytes).get(0);

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);

        assertEquals(parsedTable.getSourceTableIndex(), calibrated.getSourceTableIndex());
        assertEquals(parsedTable.getTableTitle(), calibrated.getTableTitle());
        assertEquals(2, calibrated.getRows().get(0).get(0).getRowSpan());
        assertTrue(calibrated.getRows().get(0).get(0).getColSpan() > 1);
        assertEquals(parsedTable.getColumnCount(), calibrated.getColumnCount());
        assertEquals(parsedTable.getColumnCount(),
                calibrated.getRows().get(calibrated.getRows().size() - 1).get(0).getColSpan());
        long emptySpacerRows = calibrated.getRows().stream().filter(List::isEmpty).count();
        assertTrue(emptySpacerRows <= 1);
    }

    @Test
    void calibrate_shouldRestoreDocumentHeaderForRoughWashTargetImage() throws Exception {
        assumePilotSampleAvailable();
        byte[] bytes = Files.readAllBytes(PILOT_SAMPLE);
        MesProBatchRecordParsedTable parsedTable = parser.parse(bytes).get(1);

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);

        assertEquals(parsedTable.getColumnCount(), calibrated.getColumnCount());
        assertEquals(parsedTable.getColumnWidths(), calibrated.getColumnWidths());
        assertTrue(calibrated.getPreserveSourceGrid());
        assertEquals("球囊扩张压力泵生产记录", calibrated.getRows().get(0).get(0).getText());
        assertEquals("记录编号", calibrated.getRows().get(0).get(1).getText());
        assertEquals("RE-PP-ID-01", calibrated.getRows().get(0).get(2).getText());
        assertEquals("版本", calibrated.getRows().get(1).get(0).getText());
        assertEquals("A/1", calibrated.getRows().get(1).get(1).getText());
        String processHeaderText = calibrated.getRows().get(2).get(0).getText();
        assertTrue(processHeaderText.startsWith("粗洗工序生产记录"));
        assertTrue(processHeaderText.contains("关键/特殊工序"));
        assertTrue(processHeaderText.contains("非关键/特殊工序"));
        assertEquals(20, calibrated.getRows().get(2).get(0).getColSpan());
    }

    @Test
    void calibrate_shouldUseDocLikeHeaderProportionsForRoughWash() throws Exception {
        assumePilotSampleAvailable();
        byte[] bytes = Files.readAllBytes(PILOT_SAMPLE);
        MesProBatchRecordParsedTable parsedTable = parser.parse(bytes).get(1);

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);
        List<MesProBatchRecordParsedCell> headerRow = calibrated.getRows().get(0);

        assertEquals(10, headerRow.get(0).getColSpan());
        assertEquals(4, headerRow.get(1).getColSpan());
        assertEquals(6, headerRow.get(2).getColSpan());
        assertEquals(503, headerRow.get(0).getWidthPx());
        assertEquals(174, headerRow.get(1).getWidthPx());
        assertEquals(364, headerRow.get(2).getWidthPx());
    }

    @Test
    void calibrate_shouldKeepRoughWashMergedSectionsAndFixedTableTree() throws Exception {
        assumePilotSampleAvailable();
        byte[] bytes = Files.readAllBytes(PILOT_SAMPLE);
        MesProBatchRecordParsedTable parsedTable = parser.parse(bytes).get(1);

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);

        assertEquals(22, calibrated.getRowCount());
        assertEquals(parsedTable.getColumnCount(), calibrated.getColumnWidths().size());
        assertTrue(sum(calibrated.getColumnWidths()) >= 1000);
        assertTrue(sum(calibrated.getColumnWidths()) <= 1050);
        assertTrue(calibrated.getPreserveSourceGrid());

        assertEquals("生产前检查记录", calibrated.getRows().get(4).get(0).getText().replace("\n", ""));
        assertEquals(3, calibrated.getRows().get(4).get(0).getRowSpan());
        assertEquals("粗洗生产操作及自检记录", calibrated.getRows().get(7).get(0).getText().replace("\n", ""));
        assertEquals(11, calibrated.getRows().get(7).get(0).getRowSpan());
        assertEquals("清洗次数", calibrated.getRows().get(8).get(4).getText());
        assertEquals(2, calibrated.getRows().get(8).get(4).getColSpan());
        assertEquals("参考值", calibrated.getRows().get(9).get(0).getText());
        assertEquals("实际", calibrated.getRows().get(9).get(1).getText());
        assertEquals(6, calibrated.getRows().subList(10, 16).size());
        assertEquals("/", calibrated.getRows().get(10).get(1).getText());
        assertEquals("弹簧", calibrated.getRows().get(10).get(2).getText());
        assertEquals("自来水", calibrated.getRows().get(10).get(6).getText().replace("\n", ""));
        assertEquals("生产批量汇总", calibrated.getRows().get(17).get(0).getText());
        assertEquals("生产后清场记录", calibrated.getRows().get(18).get(0).getText().replace("\n", ""));
        assertEquals("生效日期：2026年02月02日", calibrated.getRows().get(21).get(0).getText());
        assertTrue(calibrated.getRows().get(21).get(0).isBorderless());
        assertEquals(19, calibrated.getRows().get(6).get(0).getColSpan());
        assertNoSourceIndexedMergedCellExceedsColumnCount(calibrated);
    }

    @Test
    void calibrate_shouldRestoreCleaningTailBlankBlocksFromPressurePumpSourceGrid() throws Exception {
        Assumptions.assumeTrue(Files.exists(PRESSURE_PUMP_SAMPLE),
                "pressure pump source doc is not available on this machine");
        byte[] bytes = Files.readAllBytes(PRESSURE_PUMP_SAMPLE);
        MesProBatchRecordParsedTable parsedTable = routeBRecognizer.recognize(
                        PRESSURE_PUMP_SAMPLE, bytes, PRESSURE_PUMP_SAMPLE.getFileName().toString())
                .stream()
                .filter(table -> "清洗工序生产记录".equals(table.getTableTitle()))
                .findFirst()
                .orElseThrow();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);
        List<List<MesProBatchRecordParsedCell>> rows = calibrated.getRows();
        int firstCleaningBlockRow = findRowsContaining(rows, "套筒").get(0);
        int nextCleaningBlockRow = findRowsContaining(rows, "手柄").get(0);

        for (int columnIndex = 15; columnIndex <= 19; columnIndex++) {
            MesProBatchRecordParsedCell tailCell = cellOccupying(rows, firstCleaningBlockRow, columnIndex);
            assertTrue(textOf(tailCell).isBlank(), "tail column " + columnIndex + " must remain blank");
            assertTrue(Math.max(1, tailCell.getRowSpan()) >= 4,
                    "tail column " + columnIndex + " should keep the original Word large blank block: "
                            + shapeOf(rows.get(firstCleaningBlockRow)));
            assertSame(tailCell, cellOccupying(rows, firstCleaningBlockRow + 3, columnIndex),
                    "tail column " + columnIndex + " should occupy the whole repeated cleaning block");
        }
        for (int columnIndex = 18; columnIndex <= 19; columnIndex++) {
            MesProBatchRecordParsedCell signatureCell = cellOccupying(rows, firstCleaningBlockRow, columnIndex);
            assertSame(signatureCell, cellOccupying(rows, nextCleaningBlockRow, columnIndex),
                    "operator/reviewer column " + columnIndex + " should keep the original Word continuous blank column");
        }
        for (int columnIndex = 15; columnIndex <= 17; columnIndex++) {
            assertNotSame(cellOccupying(rows, firstCleaningBlockRow, columnIndex),
                    cellOccupying(rows, nextCleaningBlockRow, columnIndex),
                    "quantity column " + columnIndex + " should remain separated by repeated cleaning block");
        }
    }

    @Test
    void calibrate_shouldKeepPressurePumpCleaningRowsCoveredToRightEdge() throws Exception {
        Assumptions.assumeTrue(Files.exists(PRESSURE_PUMP_SAMPLE),
                "pressure pump source doc is not available on this machine");
        byte[] bytes = Files.readAllBytes(PRESSURE_PUMP_SAMPLE);
        MesProBatchRecordParsedTable parsedTable = routeBRecognizer.recognize(
                        PRESSURE_PUMP_SAMPLE, bytes, PRESSURE_PUMP_SAMPLE.getFileName().toString())
                .stream()
                .filter(table -> "清洗工序生产记录".equals(table.getTableTitle()))
                .findFirst()
                .orElseThrow();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);

        assertAllRowsCoverDeclaredColumnCount(calibrated);
    }

    @Test
    void calibrate_shouldKeepProductionBatchSummaryBehindRoughWashSideColumn() throws Exception {
        assumePilotSampleAvailable();
        byte[] bytes = Files.readAllBytes(PILOT_SAMPLE);
        MesProBatchRecordParsedTable parsedTable = parser.parse(bytes).get(1);

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);
        List<MesProBatchRecordParsedCell> summaryRow = calibrated.getRows().get(17);

        assertEquals(6, summaryRow.size());
        assertEquals("\u751f\u4ea7\u6279\u91cf\u6c47\u603b", summaryRow.get(0).getText());
        assertEquals(12, summaryRow.get(0).getColSpan());
        assertEquals(5, summaryRow.subList(1, summaryRow.size()).stream()
                .filter(this::isBlankCell)
                .count());
        assertEquals(19, summaryRow.stream().mapToInt(MesProBatchRecordParsedCell::getColSpan).sum());
        assertEquals(20, rowEnd(summaryRow));
    }

    @Test
    void calibrate_shouldApplySharedViewportBudgetToRoughWashFixedLayout() {
        MesProBatchRecordParsedTable parsedTable = buildDetailHeavyStructuredTable(6);

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);

        int totalHeight = calibrated.getRows().stream()
                .filter(row -> !row.isEmpty())
                .map(row -> row.stream().mapToInt(MesProBatchRecordParsedCell::getHeightPx).max().orElse(0))
                .reduce(0, Integer::sum);
        List<MesProBatchRecordParsedCell> detailRow = calibrated.getRows().stream()
                .filter(row -> !row.isEmpty() && "2026-05-17".equals(textOf(row.get(0))))
                .findFirst()
                .orElseThrow();
        int detailRowHeight = detailRow.stream()
                .mapToInt(MesProBatchRecordParsedCell::getHeightPx)
                .max()
                .orElse(0);
        List<MesProBatchRecordParsedCell> summaryRow = calibrated.getRows().stream()
                .filter(row -> !row.isEmpty() && "生产批量汇总".equals(textOf(row.get(0))))
                .findFirst()
                .orElseThrow();
        int summaryRowHeight = summaryRow.stream()
                .mapToInt(MesProBatchRecordParsedCell::getHeightPx)
                .max()
                .orElse(0);

        assertTrue(totalHeight >= 408, "totalHeight=" + totalHeight);
        assertTrue(totalHeight <= MesProBatchRecordReportShapeRules.RELAXED_SINGLE_PAGE_MAX_HEIGHT_PX,
                "totalHeight=" + totalHeight);
        assertTrue(detailRowHeight <= 30, "detailRowHeight=" + detailRowHeight);
        assertTrue(summaryRowHeight >= 22, "summaryRowHeight=" + summaryRowHeight);
    }

    @Test
    void calibrate_shouldTightenPreferredViewportBudgetWhenDetailRowsIncrease() {
        MesProBatchRecordParsedTable lighterTable = buildDetailHeavyStructuredTable(5);
        MesProBatchRecordParsedTable denserTable = buildDetailHeavyStructuredTable(8);

        MesProBatchRecordParsedTable lighterCalibrated = calibrator.calibrate(lighterTable);
        MesProBatchRecordParsedTable denserCalibrated = calibrator.calibrate(denserTable);

        int lighterDetailHeight = lighterCalibrated.getRows().stream()
                .filter(row -> !row.isEmpty() && "2026-05-17".equals(textOf(row.get(0))))
                .findFirst()
                .orElseThrow()
                .stream()
                .mapToInt(MesProBatchRecordParsedCell::getHeightPx)
                .max()
                .orElse(0);
        int denserDetailHeight = denserCalibrated.getRows().stream()
                .filter(row -> !row.isEmpty() && "2026-05-17".equals(textOf(row.get(0))))
                .findFirst()
                .orElseThrow()
                .stream()
                .mapToInt(MesProBatchRecordParsedCell::getHeightPx)
                .max()
                .orElse(0);

        assertTrue(denserDetailHeight <= lighterDetailHeight,
                "denserDetailHeight=" + denserDetailHeight + ", lighterDetailHeight=" + lighterDetailHeight);
    }

    @Test
    void calibrate_shouldReserveViewportHeightForStructuredTailAfterSummary() {
        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(buildStructuredTailAfterSummaryTable());

        int totalHeight = sumVisibleRowHeights(calibrated);
        List<MesProBatchRecordParsedCell> summaryRow = calibrated.getRows().stream()
                .filter(row -> !row.isEmpty() && "生产批量汇总".equals(textOf(row.get(0))))
                .findFirst()
                .orElseThrow();
        List<MesProBatchRecordParsedCell> tailRow = calibrated.getRows().stream()
                .filter(row -> !row.isEmpty() && textOf(row.get(0)).startsWith("1、工作场所无上批遗留"))
                .findFirst()
                .orElseThrow();

        assertTrue(totalHeight > 610, "totalHeight=" + totalHeight);
        assertTrue(totalHeight <= MesProBatchRecordReportShapeRules.RELAXED_SINGLE_PAGE_MAX_HEIGHT_PX,
                "totalHeight=" + totalHeight);
        assertTrue(summaryRow.stream().allMatch(cell -> cell.getHeightPx() >= 22));
        assertTrue(tailRow.stream().allMatch(cell -> cell.getHeightPx() >= 32));
    }

    @Test
    void calibrate_shouldExpandPackedMaterialMatrixForRouteDAssemblyOneRowWithoutSourceRowSpan() {
        MesProBatchRecordParsedTable parsedTable = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(6)
                .tableTitle("组装Ⅰ工序生产记录")
                .rowCount(3)
                .columnCount(7)
                .rows(List.of(
                        structuredRow(structuredCell("组装Ⅰ工序生产记录", 1, 7)),
                        structuredRow(structuredCell("□关键/特殊工序   ☑非关键/特殊工序", 1, 7)),
                        structuredRow(
                                structuredCell("组装Ⅰ生产操作及自检记录", 1, 1),
                                structuredCell("物料编码\n物料名称\n批号\n物料编码\n物料名称\n批号\n/\n齿条\n/\n芯杆\n/\n手柄\n/\n弹簧\n/\n螺盖\n/\nKC-6稀释剂/二甲硅油",
                                        1, 6)
                        )
                ))
                .build();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);

        List<MesProBatchRecordParsedCell> expandedHeaderRow = calibrated.getRows().stream()
                .filter(this::isExpandedMaterialMatrixHeaderRow)
                .findFirst()
                .orElseThrow();
        assertEquals(List.of("物料编码", "物料名称", "批号", "物料编码", "物料名称", "批号"),
                expandedHeaderRow.stream()
                        .skip(1)
                        .limit(6)
                        .map(this::textOf)
                        .toList());

        List<MesProBatchRecordParsedCell> firstDetailRow = calibrated.getRows().stream()
                .filter(row -> row.stream().anyMatch(cell -> "齿条".equals(textOf(cell))))
                .findFirst()
                .orElseThrow();
        assertEquals("/", textOf(firstDetailRow.get(0)));
        assertEquals("齿条", textOf(firstDetailRow.get(1)));
        assertEquals("/", textOf(firstDetailRow.get(3)));
        assertEquals("芯杆", textOf(firstDetailRow.get(4)));

        MesProBatchRecordParsedCell sideHeader = calibrated.getRows().stream()
                .flatMap(List::stream)
                .filter(cell -> "组装Ⅰ生产操作及自检记录".equals(textOf(cell)))
                .findFirst()
                .orElseThrow();
        assertTrue(sideHeader.getRowSpan() >= 4);
    }

    @Test
    void calibrate_shouldMergePackedMaterialMatrixParentheticalContinuationLines() {
        MesProBatchRecordParsedTable parsedTable = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(6)
                .tableTitle("通用工序生产记录")
                .rowCount(3)
                .columnCount(7)
                .rows(List.of(
                        structuredRow(structuredCell("通用工序生产记录", 1, 7)),
                        structuredRow(structuredCell("□关键/特殊工序   ☑非关键/特殊工序", 1, 7)),
                        structuredRow(
                                structuredCell("生产操作及自检记录", 1, 1),
                                structuredCell("""
                                        物料编码
                                        物料名称
                                        批号
                                        物料编码
                                        物料名称
                                        批号
                                        /
                                        套筒
                                        /
                                        □30atm压力表
                                        /
                                        延长管
                                        （尼龙编织管）
                                        /
                                        □40atm压力表
                                        /
                                        旋转接头
                                        /
                                        光固胶
                                        """, 1, 6)
                        )
                ))
                .build();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);
        List<List<MesProBatchRecordParsedCell>> rows = calibrated.getRows();

        List<MesProBatchRecordParsedCell> tubeRow = rowContaining(calibrated, "延长管");
        assertEquals("延长管（尼龙编织管）", textOf(tubeRow.get(1)).replaceAll("\\s+", ""));
        assertEquals("□40atm压力表", textOf(tubeRow.get(4)).replaceAll("\\s+", ""));
        List<MesProBatchRecordParsedCell> swivelRow = rowContaining(calibrated, "旋转接头");
        assertEquals("旋转接头", textOf(swivelRow.get(1)));
        assertEquals("光固胶", textOf(swivelRow.get(4)));
        assertFalse(rows.stream()
                .flatMap(List::stream)
                .anyMatch(cell -> "（尼龙编织管）".equals(textOf(cell))),
                "parenthetical continuation line must not become a standalone material item");
    }

    @Test
    void calibrate_actualPressurePumpLightCureOne_shouldKeepParentheticalMaterialWithPreviousItem() throws Exception {
        Assumptions.assumeTrue(Files.exists(PRESSURE_PUMP_SAMPLE),
                "pressure pump source doc is not available on this machine");
        byte[] bytes = Files.readAllBytes(PRESSURE_PUMP_SAMPLE);
        MesProBatchRecordParsedTable parsedTable = routeBRecognizer.recognize(
                        PRESSURE_PUMP_SAMPLE, bytes, PRESSURE_PUMP_SAMPLE.getFileName().toString())
                .stream()
                .filter(table -> "光固Ⅰ工序生产记录".equals(table.getTableTitle()))
                .findFirst()
                .orElseThrow();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);
        List<List<MesProBatchRecordParsedCell>> rows = calibrated.getRows();
        List<MesProBatchRecordParsedCell> tubeRow = rowContaining(calibrated, "延长管");
        List<MesProBatchRecordParsedCell> swivelRow = rowContaining(calibrated, "旋转接头");

        assertEquals("延长管（尼龙编织管）", textOf(tubeRow.get(1)).replaceAll("\\s+", ""));
        assertEquals("□40atm压力表", textOf(tubeRow.get(4)).replaceAll("\\s+", ""));
        assertEquals("旋转接头", textOf(swivelRow.get(1)));
        assertEquals("光固胶", textOf(swivelRow.get(4)));
        assertFalse(rows.stream()
                .flatMap(List::stream)
                .anyMatch(cell -> "（尼龙编织管）".equals(textOf(cell))),
                "actual pressure-pump light-cure matrix must not expose the continuation as its own cell");
    }

    @Test
    void calibrate_actualPressurePumpCleanDetailBand_shouldStopBeforeSelfInspectionSection() throws Exception {
        Assumptions.assumeTrue(Files.exists(PRESSURE_PUMP_SAMPLE),
                "pressure pump source doc is not available on this machine");
        byte[] bytes = Files.readAllBytes(PRESSURE_PUMP_SAMPLE);
        MesProBatchRecordParsedTable parsedTable = routeBRecognizer.recognize(
                        PRESSURE_PUMP_SAMPLE, bytes, PRESSURE_PUMP_SAMPLE.getFileName().toString())
                .stream()
                .filter(table -> "清洁工序生产记录".equals(table.getTableTitle()))
                .findFirst()
                .orElseThrow();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);
        List<List<MesProBatchRecordParsedCell>> rows = calibrated.getRows();
        int detailHeaderRow = findFirstRowContainingAll(rows,
                List.of("操作日期", "物料编码", "物料名称", "生产数量/pcs"));
        int selfInspectionRow = findRowsContaining(rows, "生产自检").stream()
                .filter(rowIndex -> rowIndex > detailHeaderRow)
                .findFirst()
                .orElseThrow();

        assertEquals(MesProBatchRecordSharedRowTypeRules.RowType.LONG_DESCRIPTION,
                MesProBatchRecordSharedRowTypeRules.classifyRow(rows, selfInspectionRow),
                "self-inspection narrative block must not be classified as material detail data");
        for (int rowIndex = detailHeaderRow + 1; rowIndex < selfInspectionRow; rowIndex++) {
            assertFalse(rowText(rows.get(rowIndex)).contains("生产自检"),
                    "operation detail band must stop before the self-inspection section");
        }
    }

    @Test
    void calibrate_shouldAlignChecklistHeaderAndOutcomeColumns() {
        MesProBatchRecordParsedTable parsedTable = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(6)
                .tableTitle("组装Ⅰ工序生产记录")
                .rowCount(4)
                .columnCount(20)
                .rows(List.of(
                        structuredRow(structuredCell("组装Ⅰ工序生产记录", 1, 20)),
                        structuredRow(
                                structuredCell("生产前检查记录", 3, 5),
                                structuredCell("检查要求", 1, 18),
                                structuredCell("结果", 1, 5),
                                structuredCell("操作人/日期", 1, 4),
                                structuredCell("复核人/日期", 1, 4)
                        ),
                        structuredRow(
                                structuredCell("工作场所无上批遗留的产品、文件或与本批产品生产无关的物料、工具；", 1, 5),
                                structuredCell("墙面、地面、天花板、灯具等环境清洁应符合《INT/PD/6.4工作环境控制程序》。", 1, 18),
                                structuredCell("□符合要求\n□不符合要求", 1, 5),
                                structuredCell("", 1, 4),
                                structuredCell("", 1, 4)
                        ),
                        structuredRow(structuredCell("备注：检查结果符合要求后进行以下生产操作。", 1, 20))
                ))
                .build();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);

        List<MesProBatchRecordParsedCell> headerRow = calibrated.getRows().stream()
                .filter(row -> row.stream().anyMatch(cell -> "检查要求".equals(textOf(cell))))
                .findFirst()
                .orElseThrow();
        List<MesProBatchRecordParsedCell> bodyRow = calibrated.getRows().stream()
                .filter(row -> row.stream().anyMatch(cell -> textOf(cell).contains("□符合要求")))
                .findFirst()
                .orElseThrow();
        List<List<MesProBatchRecordParsedCell>> rows = calibrated.getRows();
        int headerRowIndex = rows.indexOf(headerRow);
        int bodyRowIndex = rows.indexOf(bodyRow);
        int sideHeaderSpan = headerRow.get(0).getColSpan();
        int resultColumnIndex = calibrated.getColumnCount() - 3;
        int operatorColumnIndex = calibrated.getColumnCount() - 2;
        int reviewerColumnIndex = calibrated.getColumnCount() - 1;

        assertEquals("生产前检查记录", textOf(cellOccupying(rows, bodyRowIndex, 0)).replace("\n", ""));
        assertEquals("检查要求", textOf(cellOccupying(rows, headerRowIndex, sideHeaderSpan)));
        assertEquals("结果", textOf(cellOccupying(rows, headerRowIndex, resultColumnIndex)));
        assertEquals("操作人/日期", textOf(cellOccupying(rows, headerRowIndex, operatorColumnIndex)));
        assertEquals("复核人/日期", textOf(cellOccupying(rows, headerRowIndex, reviewerColumnIndex)));
        assertTrue(textOf(cellOccupying(rows, bodyRowIndex, resultColumnIndex)).contains("□符合要求"));
        assertTrue(textOf(cellOccupying(rows, bodyRowIndex, operatorColumnIndex)).isBlank());
        assertTrue(textOf(cellOccupying(rows, bodyRowIndex, reviewerColumnIndex)).isBlank());
    }

    @Test
    void calibrate_shouldRestoreChecklistSideHeaderRowSpanAndShiftBodyBehindIt() {
        MesProBatchRecordParsedTable parsedTable = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(6)
                .tableTitle("组装Ⅰ工序生产记录")
                .rowCount(5)
                .columnCount(21)
                .rows(List.of(
                        structuredRow(structuredCell("组装Ⅰ工序生产记录", 1, 21)),
                        structuredRow(
                                structuredCell("生产前检查记录", 1, 7),
                                structuredCell("检查要求", 1, 11),
                                structuredCell("结果", 1, 2),
                                structuredCell("操作人/日期", 1, 2),
                                structuredCell("复核人/日期", 1, 6)
                        ),
                        structuredRow(
                                structuredCell("1、工作场所无上批遗留的产品、文件或与本批产品生产无关的物料、工具；\n"
                                                + "2、墙面、地面、天花板、灯具等环境清洁应符合《INT/PD/6.4工作环境控制程序》。", 1, 18),
                                structuredCell("□符合要求\n□不符合要求", 1, 1),
                                structuredCell("", 1, 1),
                                structuredCell("", 1, 1)
                        ),
                        structuredRow(structuredCell("备注：检查结果符合要求后进行以下生产操作。", 1, 21))
                ))
                .build();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);

        List<List<MesProBatchRecordParsedCell>> rows = calibrated.getRows();
        int headerRowIndex = findRowsContaining(rows, "检查要求").stream().findFirst().orElseThrow();
        int bodyRowIndex = findRowsContaining(rows, "□符合要求").stream().findFirst().orElseThrow();

        MesProBatchRecordParsedCell sideHeader = cellOccupying(rows, bodyRowIndex, 0);
        assertEquals("生产前检查记录", textOf(sideHeader).replace("\n", ""));
        assertTrue(sideHeader.getRowSpan() >= 2);

        MesProBatchRecordParsedCell bodyNarrative = rows.get(bodyRowIndex).get(0);
        assertTrue(textOf(bodyNarrative).startsWith("1、工作场所无上批遗留"));
        assertEquals(11, bodyNarrative.getColSpan());

        MesProBatchRecordParsedCell headerNarrative = cellOccupying(rows, headerRowIndex, 7);
        assertEquals("检查要求", textOf(headerNarrative));
        assertTrue(textOf(cellOccupying(rows, bodyRowIndex, 18)).contains("□符合要求"));
        assertTrue(textOf(cellOccupying(rows, bodyRowIndex, 19)).isBlank());
        assertTrue(textOf(cellOccupying(rows, bodyRowIndex, 20)).isBlank());
    }

    @Test
    void calibrate_shouldNarrowChecklistSideHeaderAndReserveBalancedTailColumnsForAssemblyChecklist() {
        MesProBatchRecordParsedTable parsedTable = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(6)
                .tableTitle("组装Ⅰ工序生产记录")
                .rowCount(5)
                .columnCount(21)
                .rows(List.of(
                        structuredRow(structuredCell("组装Ⅰ工序生产记录", 1, 21)),
                        structuredRow(
                                structuredCell("生产前检查记录", 1, 7),
                                structuredCell("检查要求", 1, 11),
                                structuredCell("结果", 1, 1),
                                structuredCell("操作人/日期", 1, 1),
                                structuredCell("复核人/日期", 1, 1)
                        ),
                        structuredRow(
                                structuredCell("1、工作场所无上批遗留的产品、文件或与本批产品生产无关的物料、工具；\n"
                                                + "2、墙面、地面、天花板、灯具等环境清洁应符合《INT/PD/6.4工作环境控制程序》。", 1, 18),
                                structuredCell("□符合要求\n□不符合要求", 1, 1),
                                structuredCell("", 1, 1),
                                structuredCell("", 1, 1)
                        ),
                        structuredRow(structuredCell("备注：检查结果符合要求后进行以下生产操作。", 1, 21))
                ))
                .build();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);

        List<List<MesProBatchRecordParsedCell>> rows = calibrated.getRows();
        int headerRowIndex = findRowsContaining(rows, "检查要求").stream().findFirst().orElseThrow();
        int bodyRowIndex = findRowsContaining(rows, "□符合要求").stream().findFirst().orElseThrow();
        List<MesProBatchRecordParsedCell> bodyRow = rows.get(bodyRowIndex);

        MesProBatchRecordParsedCell sideHeader = cellOccupying(rows, bodyRowIndex, 0);
        MesProBatchRecordParsedCell headerNarrative = cellOccupying(rows, headerRowIndex, sideHeader.getColSpan());
        MesProBatchRecordParsedCell bodyNarrative = bodyRow.get(0);
        MesProBatchRecordParsedCell resultCell = bodyRow.get(1);
        MesProBatchRecordParsedCell operatorCell = bodyRow.get(2);
        MesProBatchRecordParsedCell reviewerCell = bodyRow.get(3);

        assertEquals(1, sideHeader.getColSpan(), "header should stay as a narrow side strip");
        assertEquals("检查要求", textOf(headerNarrative));
        assertEquals(11, bodyNarrative.getColSpan(), "body narrative should keep the dominant middle band");
        assertEquals(3, resultCell.getColSpan(), "result tail should keep a dedicated balanced width");
        assertEquals(3, operatorCell.getColSpan(), "operator tail should keep a dedicated balanced width");
        assertEquals(3, reviewerCell.getColSpan(), "reviewer tail should keep a dedicated balanced width");
    }

    @Test
    void calibrate_shouldPreserveRepeatedSubtableHeaderShapesAcrossSegments() {
        MesProBatchRecordParsedTable parsedTable = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(2)
                .tableTitle("通用工序生产记录")
                .rowCount(12)
                .columnCount(20)
                .rows(List.of(
                        row("通用工序生产记录"),
                        row("生产批号", "", "产品规格", "", "生产依据", ""),
                        row("设备编码", "超声波清洗机：T01", "是否在计量效期内", "□是", "□否"),
                        row("检查要求", "结果", "操作人/日期", "复核人/日期"),
                        row("备注：检查结果符合要求后进行以下生产操作"),
                        row("清洗次数", "清洗介质", "清洗功率", "清洗温度", "清洗时间"),
                        row("操作日期", "物料编码", "物料名称", "批号", "清洗次数", "清洗介质", "清洗功率",
                                "清洗温度", "清洗时间", "生产数量/pcs", "自检合格数量/pcs", "不合格数量/pcs",
                                "操作人", "复核人"),
                        row("参考值", "实际", "参考值", "实际", "参考值", "实际", "参考值", "实际", "参考值", "实际"),
                        row("2026-05-17", "A001", "弹簧", "B-01", "2", "纯化水", "100%", "室温", "30min", "1"),
                        row("清洗次数", "清洗介质", "清洗功率", "清洗温度", "清洗时间"),
                        row("操作日期", "物料编码", "物料名称", "批号", "清洗次数", "清洗介质", "清洗功率",
                                "清洗温度", "清洗时间", "生产数量/pcs", "自检合格数量/pcs", "不合格数量/pcs",
                                "操作人", "复核人"),
                        row("参考值", "实际", "参考值", "实际", "参考值", "实际", "参考值", "实际", "参考值", "实际")
                ))
                .build();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);
        List<List<MesProBatchRecordParsedCell>> rows = calibrated.getRows();

        List<Integer> sectionHeaderRows = findRows(rows, "清洗次数");
        List<Integer> detailHeaderRows = findRows(rows, "操作日期");
        List<Integer> valueHeaderRows = findRows(rows, "参考值");

        assertEquals(2, sectionHeaderRows.size());
        assertEquals(2, detailHeaderRows.size());
        assertEquals(2, valueHeaderRows.size());

        assertRowShapeEquals(rows.get(sectionHeaderRows.get(0)), rows.get(sectionHeaderRows.get(1)));
        assertRowShapeEquals(rows.get(detailHeaderRows.get(0)), rows.get(detailHeaderRows.get(1)));
        assertRowShapeEquals(rows.get(valueHeaderRows.get(0)), rows.get(valueHeaderRows.get(1)));
    }

    @Test
    void calibrate_shouldRestoreMetadataLabelValuePairsIntoGroupedColumnBudget() {
        MesProBatchRecordParsedTable parsedTable = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(6)
                .tableTitle("组装Ⅰ工序生产记录")
                .rowCount(2)
                .columnCount(21)
                .rows(List.of(
                        structuredRow(structuredCell("组装Ⅰ工序生产记录", 1, 21)),
                        structuredRow(
                                structuredCell("生产批号", 1, 1),
                                structuredCell("", 1, 1),
                                structuredCell("产品规格", 1, 1),
                                structuredCell("", 1, 1),
                                structuredCell("生产依据", 1, 1),
                                structuredCell("PP-ID-1-08（  /  ）", 1, 16)
                        )
                ))
                .build();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);
        List<MesProBatchRecordParsedCell> metadataRow = calibrated.getRows().stream()
                .filter(row -> row.stream().anyMatch(cell -> "生产批号".equals(textOf(cell))))
                .findFirst()
                .orElseThrow();

        assertEquals(List.of(3, 4, 3, 4, 3, 4),
                metadataRow.stream().map(MesProBatchRecordParsedCell::getColSpan).toList());
        assertEquals("PP-ID-1-08（  /  ）", textOf(metadataRow.get(5)));
    }

    @Test
    void calibrate_shouldKeepRepeatedSubheadersWhenColumnCountChanges() {
        MesProBatchRecordParsedTable parsedTable = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(2)
                .tableTitle("通用工序生产记录")
                .rowCount(8)
                .columnCount(20)
                .rows(List.of(
                        row("通用工序生产记录"),
                        row("生产批号", "", "产品规格", "", "生产依据", ""),
                        row("检查要求", "结果", "操作人/日期", "复核人/日期"),
                        row("备注：检查结果符合要求后进行以下生产操作"),
                        row("分段A", "子项1", "子项2", "子项3", "子项4"),
                        row("2026-05-17", "A001", "弹簧", "B-01", "2", "纯化水", "100%"),
                        row("分段A", "", "子项1", "子项2", "子项3", "子项4"),
                        row("2026-05-18", "A002", "弹簧", "B-02", "2", "纯化水", "100%")
                ))
                .build();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);
        List<List<MesProBatchRecordParsedCell>> rows = calibrated.getRows();
        List<Integer> repeatedHeaderRows = findRows(rows, "分段A");

        assertEquals(2, repeatedHeaderRows.size());
        assertEquals(5, rows.get(repeatedHeaderRows.get(0)).size());
        assertEquals(6, rows.get(repeatedHeaderRows.get(1)).size());
        assertEquals(20, rows.get(repeatedHeaderRows.get(0)).stream()
                .mapToInt(MesProBatchRecordParsedCell::getColSpan)
                .sum());
        assertEquals(20, rows.get(repeatedHeaderRows.get(1)).stream()
                .mapToInt(MesProBatchRecordParsedCell::getColSpan)
                .sum());
        assertTrue(rows.get(repeatedHeaderRows.get(0)).stream().allMatch(cell -> cell.getHeightPx() >= 24));
        assertTrue(rows.get(repeatedHeaderRows.get(1)).stream().allMatch(cell -> cell.getHeightPx() >= 24));
    }

    @Test
    void calibrate_routeAFixedTables_shouldKeepCellsWithinDeclaredColumnBudget() throws Exception {
        assumeFixedSampleAvailable();
        byte[] bytes = Files.readAllBytes(FIXED_SAMPLE);
        List<MesProBatchRecordParsedTable> parsedTables = parser.parse(bytes);

        for (MesProBatchRecordParsedTable parsedTable : parsedTables) {
            MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);
            assertNoMergedCellExceedsColumnCount(calibrated);
        }
    }

    @Test
    void calibrate_routeAT13_shouldNotInsertSourceInconsistentSyntheticContinuationHeader() throws Exception {
        assumeFixedSampleAvailable();
        byte[] bytes = Files.readAllBytes(FIXED_SAMPLE);
        MesProBatchRecordParsedTable parsedTable = parser.parse(bytes).stream()
                .filter(table -> "单包装工序生产记录".equals(table.getTableTitle()))
                .findFirst()
                .orElseThrow();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);
        List<List<MesProBatchRecordParsedCell>> rows = calibrated.getRows();
        List<Integer> documentHeaderRows = findRows(rows, "球囊扩张压力泵生产记录");
        List<Integer> heatSealRows = findRowsContaining(rows, "封口热合机");
        int selfInspectionRow = findRows(rows, "生产自检").get(0);

        assertEquals(1, documentHeaderRows.size(),
                "unexpected synthetic continuation header rows=" + documentHeaderRows
                        + ", heatSealRows=" + heatSealRows
                        + ", selfInspectionRow=" + selfInspectionRow);
        assertTrue(documentHeaderRows.get(0) < heatSealRows.get(0));
        assertTrue(heatSealRows.get(heatSealRows.size() - 1) < selfInspectionRow);
        assertNoMergedCellExceedsColumnCount(calibrated);
    }

    @Test
    void calibrate_routeAT13_shouldNotCollapseEquipmentMatrixIntoFewMultilineCells() throws Exception {
        assumeFixedSampleAvailable();
        byte[] bytes = Files.readAllBytes(FIXED_SAMPLE);
        MesProBatchRecordParsedTable parsedTable = parser.parse(bytes).stream()
                .filter(table -> "单包装工序生产记录".equals(table.getTableTitle()))
                .findFirst()
                .orElseThrow();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);
        List<MesProBatchRecordParsedCell> calibratedRow = calibrated.getRows().stream()
                .filter(row -> row.stream().map(MesProBatchRecordParsedCell::getText)
                        .anyMatch(text -> text != null && text.contains("封口热合机")))
                .findFirst()
                .orElseThrow();
        String shape = calibratedRow.stream()
                .map(cell -> cell.getColSpan() + "x" + cell.getWidthPx() + ":" + textOf(cell).replace("\n", "\\n"))
                .reduce((left, right) -> left + " | " + right)
                .orElse("");

        assertTrue(calibratedRow.size() >= 8, "equipmentRow=" + shape);
    }

    @Test
    void calibrate_routeAT13_shouldNormalizeMaterialNarrativeChecklistIntoBalancedBlocks() throws Exception {
        assumeFixedSampleAvailable();
        byte[] bytes = Files.readAllBytes(FIXED_SAMPLE);
        MesProBatchRecordParsedTable parsedTable = parser.parse(bytes).stream()
                .filter(table -> "单包装工序生产记录".equals(table.getTableTitle()))
                .findFirst()
                .orElseThrow();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);
        List<List<MesProBatchRecordParsedCell>> rows = calibrated.getRows();
        int materialRowIndex = findRowsContaining(rows, "所有的物料转移到指定的区域存放并标识。").get(0);
        List<MesProBatchRecordParsedCell> materialRow = rows.get(materialRowIndex);
        MesProBatchRecordParsedCell sideHeaderCell = cellOccupying(rows, materialRowIndex, 0);
        MesProBatchRecordParsedCell itemCell = cellByText(materialRow, "物料");
        MesProBatchRecordParsedCell narrativeCell = cellContainingText(materialRow, "所有的物料转移到指定的区域存放并标识。");
        MesProBatchRecordParsedCell resultCell = cellContainingText(materialRow, "□是");
        String shape = materialRow.stream()
                .map(cell -> cell.getColSpan() + "x" + cell.getWidthPx() + ":" + textOf(cell).replace("\n", "\\n"))
                .reduce((left, right) -> left + " | " + right)
                .orElse("");

        assertTrue(calibrated.getPreserveSourceGrid());
        assertEquals("生产后清场记录", textOf(sideHeaderCell).replace("\n", ""));
        assertEquals(13, itemCell.getColSpan(), "materialRow=" + shape);
        assertEquals(66, narrativeCell.getColSpan(), "materialRow=" + shape);
        assertEquals(19, resultCell.getColSpan(), "materialRow=" + shape);
        assertEquals(33, materialRow.get(materialRow.size() - 1).getColSpan(), "materialRow=" + shape);
        assertEquals(29, materialRow.get(materialRow.size() - 2).getColSpan(), "materialRow=" + shape);
        assertNoSourceIndexedMergedCellExceedsColumnCount(calibrated);
    }

    @Test
    void calibrate_shouldExpandPackedMaterialMatrixCellsIntoStructuredRowsForRouteAProcessPages() throws Exception {
        assumeFixedSampleAvailable();
        byte[] bytes = Files.readAllBytes(FIXED_SAMPLE);
        List<MesProBatchRecordParsedTable> packedTables = parser.parse(bytes).stream()
                .filter(table -> table.getColumnCount() >= 60)
                .filter(table -> table.getRows().stream()
                        .flatMap(List::stream)
                        .anyMatch(this::isPackedMaterialMatrixCell))
                .toList();

        assertFalse(packedTables.isEmpty(), "fixed sample should include packed material matrix pages");
        for (MesProBatchRecordParsedTable parsedTable : packedTables) {
            MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);
            List<List<MesProBatchRecordParsedCell>> rows = calibrated.getRows();

            boolean stillPacked = rows.stream()
                    .flatMap(List::stream)
                    .anyMatch(this::isPackedMaterialMatrixCell);

            assertFalse(stillPacked, "source-grid-preserved table should still expand packed material block into visible grid: "
                    + parsedTable.getTableTitle());
            assertTrue(rows.stream().anyMatch(this::isExpandedMaterialMatrixHeaderRow),
                    "source-grid-preserved table should expose material matrix header row: "
                            + parsedTable.getTableTitle());
            assertTrue(calibrated.getPreserveSourceGrid(), "tableTitle=" + parsedTable.getTableTitle());
            assertEquals(parsedTable.getColumnCount(), calibrated.getColumnCount());
            assertEquals(parsedTable.getColumnWidths(), calibrated.getColumnWidths());
            assertNoSourceIndexedMergedCellExceedsColumnCount(calibrated);
        }
    }

    @Test
    void calibrate_shouldKeepEffectiveDateRowsCompactForFixedRouteAProcessPages() throws Exception {
        assumeFixedSampleAvailable();
        byte[] bytes = Files.readAllBytes(FIXED_SAMPLE);
        List<MesProBatchRecordParsedTable> processTables = parser.parse(bytes).stream()
                .filter(table -> table.getTableTitle() != null && table.getTableTitle().contains("工序生产记录"))
                .toList();

        assertFalse(processTables.isEmpty(), "fixed sample should include process pages");
        for (MesProBatchRecordParsedTable parsedTable : processTables) {
            MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);
            List<MesProBatchRecordParsedCell> footerRow = calibrated.getRows().stream()
                    .filter(row -> !row.isEmpty() && textOf(row.get(0)).startsWith("生效日期："))
                    .findFirst()
                    .orElseThrow();

            assertTrue(footerRow.stream().allMatch(cell -> cell.getHeightPx() <= 20),
                    "tableTitle=" + parsedTable.getTableTitle()
                            + ", footerHeights=" + footerRow.stream().map(MesProBatchRecordParsedCell::getHeightPx).toList());
        }
    }

    @Test
    void calibrate_shouldRedistributeFourCellChecklistNarrativeBandIntoBalancedTailColumns() {
        MesProBatchRecordParsedTable parsedTable = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(4)
                .tableTitle("通用工序生产记录")
                .rowCount(6)
                .columnCount(20)
                .rows(List.of(
                        row("通用工序生产记录"),
                        row("生产批号", "", "产品规格", "", "生产依据", ""),
                        structuredRow(
                                structuredCell("生产后清场记录", 2, 2),
                                structuredCell("项目", 1, 4),
                                structuredCell("要求", 1, 8),
                                structuredCell("结果", 1, 4),
                                structuredCell("操作人/日期", 1, 2)),
                        structuredRow(
                                structuredCell("物料", 1, 4),
                                structuredCell("所有的物料转移到指定的区域存放并标识。", 1, 8),
                                structuredCell("□是  □否", 1, 4),
                                structuredCell("", 1, 2)),
                        structuredRow(
                                structuredCell("清洁卫生", 1, 4),
                                structuredCell("按清场管理制度执行清洁设备、工器具及环境。", 1, 8),
                                structuredCell("□是  □否", 1, 4),
                                structuredCell("", 1, 2)),
                        row("生效日期：2026年02月02日")
                ))
                .build();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);
        List<MesProBatchRecordParsedCell> materialRow = calibrated.getRows().stream()
                .filter(row -> row.stream().anyMatch(cell -> textOf(cell).contains("所有的物料转移到指定的区域存放并标识。")))
                .findFirst()
                .orElseThrow();
        MesProBatchRecordParsedCell narrativeCell = cellContainingText(materialRow, "所有的物料转移到指定的区域存放并标识。");
        MesProBatchRecordParsedCell resultCell = cellContainingText(materialRow, "□是");
        int totalColSpan = materialRow.stream().mapToInt(MesProBatchRecordParsedCell::getColSpan).sum();
        String shape = materialRow.stream()
                .map(cell -> cell.getColSpan() + "x" + cell.getWidthPx() + ":" + textOf(cell).replace("\n", "\\n"))
                .reduce((left, right) -> left + " | " + right)
                .orElse("");

        assertTrue(narrativeCell.getColSpan() <= Math.max(6, (int) Math.ceil(totalColSpan * 0.5d)),
                "materialRow=" + shape);
        assertTrue(resultCell.getColSpan() >= 4, "materialRow=" + shape);
    }

    @Test
    void calibrate_shouldPreserveChecklistNarrativeBodyHeightForAssemblyChecklist() {
        MesProBatchRecordParsedTable parsedTable = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(6)
                .tableTitle("组装Ⅰ工序生产记录")
                .rowCount(5)
                .columnCount(21)
                .rows(List.of(
                        structuredRow(structuredCell("组装Ⅰ工序生产记录", 1, 21)),
                        structuredRow(
                                structuredCell("生产批号", 1, 1),
                                structuredCell("", 1, 1),
                                structuredCell("产品规格", 1, 1),
                                structuredCell("", 1, 1),
                                structuredCell("生产依据", 1, 1),
                                structuredCell("PP-ID-1-08（  /  ）", 1, 16)
                        ),
                        structuredRow(
                                structuredCell("生产前检查记录", 1, 7),
                                structuredCell("检查要求", 1, 11),
                                structuredCell("结果", 1, 1),
                                structuredCell("操作人/日期", 1, 1),
                                structuredCell("复核人/日期", 1, 1)
                        ),
                        structuredRow(
                                structuredCell("1、工作场所无上批遗留的产品、文件或与本批产品生产无关的物料、工具；\n"
                                                + "2、墙面、地面、天花板、灯具等环境清洁应符合《INT/PD/6.4工作环境控制程序》。\n"
                                                + "具体清场标准及内容依据《INT/GL/7.5.8-03清场管理制度》执行。", 1, 18, 52),
                                structuredCell("□符合要求\n□不符合要求", 1, 1, 52),
                                structuredCell("", 1, 1, 52),
                                structuredCell("", 1, 1, 52)
                        ),
                        structuredRow(structuredCell("备注：检查结果符合要求后进行以下生产操作。", 1, 21))
                ))
                .build();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);
        List<MesProBatchRecordParsedCell> bodyRow = calibrated.getRows().stream()
                .filter(row -> row.stream().anyMatch(cell -> textOf(cell).contains("□符合要求")))
                .findFirst()
                .orElseThrow();

        int actualHeight = bodyRow.stream().mapToInt(MesProBatchRecordParsedCell::getHeightPx).max().orElse(0);
        assertTrue(actualHeight >= 52, "bodyRowHeight=" + actualHeight + ", shape=" + shapeOf(bodyRow));
    }

    @Test
    void calibrate_shouldPreserveAssemblyOperationBandWidthAndHeightProportions() {
        MesProBatchRecordParsedTable parsedTable = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(6)
                .tableTitle("组装Ⅰ工序生产记录")
                .rowCount(8)
                .columnCount(21)
                .rows(List.of(
                        structuredRow(structuredCell("组装Ⅰ工序生产记录", 1, 21)),
                        structuredRow(
                                structuredCell("组装Ⅰ生产操作及自检记录", 1, 1, 84),
                                structuredCell("物料编码", 1, 5),
                                structuredCell("物料名称", 1, 5),
                                structuredCell("批号", 1, 1),
                                structuredCell("物料编码", 1, 4),
                                structuredCell("物料名称", 1, 4),
                                structuredCell("批号", 1, 1)
                        ),
                        structuredRow(
                                structuredCell("设备编码", 1, 4),
                                structuredCell("撤压机：C01017", 1, 6),
                                structuredCell("是否在计量效期内", 1, 4),
                                structuredCell("□是  □否", 1, 3)
                        ),
                        structuredRow(
                                structuredCell("操作日期", 1, 1, 92),
                                structuredCell("生产自检", 1, 2, 92),
                                structuredCell("合格标准：\n"
                                                + "1、撤压检测：产品加压至规定压力，将产品放至撤压机（气压：2ATM，缸径20MM）上应能顺利撤压；\n"
                                                + "2、耐压检测：产品加压至规定压力，无跳压情况。\n"
                                                + "检验方法：1、将待检推杆与专用套筒（吸入10m检测用纯化水）组装，将压力打至20-25ATM，用撤压机撤压；\n"
                                                + "2、将撤压合格推杆继续打压至30ATM。", 1, 13, 92),
                                structuredCell("生产数量/pcs", 1, 1, 92),
                                structuredCell("自检合格数量/pcs", 1, 1, 92),
                                structuredCell("不合格数量/pcs", 1, 1, 92),
                                structuredCell("操作人", 1, 1, 92),
                                structuredCell("复核人", 1, 1, 92)
                        ),
                        structuredRow(
                                structuredCell("", 1, 1, 28),
                                structuredCell("", 1, 2, 28),
                                structuredCell("", 1, 13, 28),
                                structuredCell("", 1, 1, 28),
                                structuredCell("", 1, 1, 28),
                                structuredCell("", 1, 1, 28),
                                structuredCell("", 1, 1, 28),
                                structuredCell("", 1, 1, 28)
                        ),
                        structuredRow(
                                structuredCell("", 1, 1, 28),
                                structuredCell("", 1, 2, 28),
                                structuredCell("", 1, 13, 28),
                                structuredCell("", 1, 1, 28),
                                structuredCell("", 1, 1, 28),
                                structuredCell("", 1, 1, 28),
                                structuredCell("", 1, 1, 28),
                                structuredCell("", 1, 1, 28)
                        ),
                        structuredRow(
                                structuredCell("生产批量汇总", 1, 15),
                                structuredCell("", 1, 1),
                                structuredCell("", 1, 1),
                                structuredCell("", 1, 1),
                                structuredCell("", 1, 1),
                                structuredCell("", 1, 1),
                                structuredCell("", 1, 1)
                        )
                ))
                .build();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);
        List<List<MesProBatchRecordParsedCell>> rows = calibrated.getRows();

        int equipmentRowIndex = findRows(rows, "设备编码").get(0);
        int selfInspectionRowIndex = findRows(rows, "操作日期").get(0);
        List<MesProBatchRecordParsedCell> equipmentRow = rows.get(equipmentRowIndex);
        List<MesProBatchRecordParsedCell> selfInspectionRow = rows.get(selfInspectionRowIndex);

        MesProBatchRecordParsedCell sideHeader = cellOccupying(rows, equipmentRowIndex, 0);
        MesProBatchRecordParsedCell equipmentLabel = cellByText(equipmentRow, "设备编码");
        MesProBatchRecordParsedCell equipmentValue = cellContainingText(equipmentRow, "撤压机：C01017");
        MesProBatchRecordParsedCell effectiveLabel = cellByText(equipmentRow, "是否在计量效期内");
        MesProBatchRecordParsedCell selfInspectionLabel = cellByText(selfInspectionRow, "生产自检");
        MesProBatchRecordParsedCell narrativeCell = cellContainingText(selfInspectionRow, "合格标准：");
        MesProBatchRecordParsedCell quantityCell = cellByText(selfInspectionRow, "生产数量/pcs");
        MesProBatchRecordParsedCell operatorCell = cellByText(selfInspectionRow, "操作人");
        MesProBatchRecordParsedCell reviewerCell = cellByText(selfInspectionRow, "复核人");

        assertEquals(21, calibrated.getColumnCount(),
                "shared side header should not inflate declared column count");
        assertNoMergedCellExceedsColumnCount(calibrated);
        assertEquals("组装Ⅰ生产操作及自检记录", textOf(sideHeader).replace("\n", ""),
                "operation band should preserve the shared vertical side header");
        assertTrue(Math.max(1, sideHeader.getRowSpan()) >= 4,
                "sideHeader=" + shapeOf(equipmentRow) + " / " + shapeOf(selfInspectionRow));
        assertTrue(sideHeader.getWidthPx() >= 120,
                "sideHeader=" + shapeOf(equipmentRow) + " / " + shapeOf(selfInspectionRow));
        assertTrue(equipmentValue.getWidthPx() > equipmentLabel.getWidthPx());
        assertTrue(effectiveLabel.getWidthPx() >= equipmentLabel.getWidthPx());
        assertTrue(selfInspectionLabel.getWidthPx() >= 120,
                "selfInspectionRow=" + shapeOf(selfInspectionRow));
        assertTrue(narrativeCell.getWidthPx() >= 520,
                "selfInspectionRow=" + shapeOf(selfInspectionRow));
        assertTrue(quantityCell.getWidthPx() >= 68,
                "selfInspectionRow=" + shapeOf(selfInspectionRow));
        assertTrue(operatorCell.getWidthPx() >= 68,
                "selfInspectionRow=" + shapeOf(selfInspectionRow));
        assertTrue(reviewerCell.getWidthPx() >= 68,
                "selfInspectionRow=" + shapeOf(selfInspectionRow));
        assertTrue(equipmentRow.stream().mapToInt(MesProBatchRecordParsedCell::getHeightPx).max().orElse(0) >= 52,
                "equipmentRow=" + shapeOf(equipmentRow));
        assertTrue(selfInspectionRow.stream().mapToInt(MesProBatchRecordParsedCell::getHeightPx).max().orElse(0) >= 84,
                "selfInspectionRow=" + shapeOf(selfInspectionRow));
        int summaryRowIndex = findRows(rows, "生产批量汇总").get(0);
        List<MesProBatchRecordParsedCell> summaryRow = rows.get(summaryRowIndex);
        assertTrue(summaryRow.stream().mapToInt(MesProBatchRecordParsedCell::getHeightPx).max().orElse(0) >= 22,
                "summaryRow=" + shapeOf(summaryRow));
    }

    @Test
    void calibrate_shouldRestoreOperationInstructionDetailRowsUsingChecklistColumnSkeleton() {
        MesProBatchRecordParsedTable parsedTable = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(6)
                .tableTitle("组装Ⅰ工序生产记录")
                .rowCount(8)
                .columnCount(21)
                .rows(List.of(
                        structuredRow(
                                structuredCell("组装Ⅰ工序生产记录", 1, 21)
                        ),
                        structuredRow(
                                structuredCell("组装Ⅰ生产操作及自检记录", 1, 1, 84),
                                structuredCell("物料编码", 1, 5),
                                structuredCell("物料名称", 1, 5),
                                structuredCell("批号", 1, 1),
                                structuredCell("物料编码", 1, 4),
                                structuredCell("物料名称", 1, 4),
                                structuredCell("批号", 1, 1)
                        ),
                        structuredRow(
                                structuredCell("设备编码", 1, 4),
                                structuredCell("撤压机：C01017", 1, 6),
                                structuredCell("是否在计量效期内", 1, 4),
                                structuredCell("□是  □否", 1, 3)
                        ),
                        structuredRow(
                                structuredCell("操作日期", 1, 1, 92),
                                structuredCell("生产自检", 1, 2, 92),
                                structuredCell("合格标准：\n"
                                                + "1、撤压检测：产品加压至规定压力，将产品放至撤压机（气压：2ATM，缸径20MM）上应能顺利撤压；\n"
                                                + "2、耐压检测：产品加压至规定压力，无跳压情况。", 1, 13, 92),
                                structuredCell("生产数量/pcs", 1, 1, 92),
                                structuredCell("自检合格数量/pcs", 1, 1, 92),
                                structuredCell("不合格数量/pcs", 1, 1, 92),
                                structuredCell("操作人", 1, 1, 92),
                                structuredCell("复核人", 1, 1, 92)
                        ),
                        structuredRow(
                                structuredCell("", 1, 1, 28),
                                structuredCell("", 1, 2, 28),
                                structuredCell("", 1, 13, 28),
                                structuredCell("", 1, 1, 28),
                                structuredCell("", 1, 1, 28),
                                structuredCell("", 1, 1, 28),
                                structuredCell("", 1, 1, 28),
                                structuredCell("", 1, 1, 28)
                        ),
                        structuredRow(
                                structuredCell("", 1, 1, 28),
                                structuredCell("", 1, 2, 28),
                                structuredCell("", 1, 13, 28),
                                structuredCell("", 1, 1, 28),
                                structuredCell("", 1, 1, 28),
                                structuredCell("", 1, 1, 28),
                                structuredCell("", 1, 1, 28),
                                structuredCell("", 1, 1, 28)
                        ),
                        structuredRow(
                                structuredCell("生产批量汇总", 1, 15),
                                structuredCell("", 1, 1),
                                structuredCell("", 1, 1),
                                structuredCell("", 1, 1),
                                structuredCell("", 1, 1),
                                structuredCell("", 1, 1),
                                structuredCell("", 1, 1)
                        )
                ))
                .build();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);
        List<List<MesProBatchRecordParsedCell>> rows = calibrated.getRows();

        int selfInspectionRowIndex = findRows(rows, "操作日期").get(0);
        List<MesProBatchRecordParsedCell> selfInspectionRow = rows.get(selfInspectionRowIndex);
        List<Integer> blankDetailRowIndexes = findBlankOperationBandDetailRows(rows, selfInspectionRowIndex + 1);

        assertEquals(2, blankDetailRowIndexes.size(), "应恢复出连续两行空白填写区");
        assertEquals("操作日期", textOf(selfInspectionRow.get(0)));
        assertEquals("生产自检", textOf(selfInspectionRow.get(1)));
        assertTrue(textOf(selfInspectionRow.get(2)).contains("合格标准"),
                "自检头行正文区应恢复为合格标准主块");
        assertEquals("生产数量/pcs", textOf(selfInspectionRow.get(3)));
        assertEquals("自检合格数量/pcs", textOf(selfInspectionRow.get(4)));
        assertEquals("不合格数量/pcs", textOf(selfInspectionRow.get(5)));
        assertEquals("操作人", textOf(selfInspectionRow.get(6)));
        assertEquals("复核人", textOf(selfInspectionRow.get(7)));
        for (Integer blankDetailRowIndex : blankDetailRowIndexes) {
            List<MesProBatchRecordParsedCell> blankDetailRow = rows.get(blankDetailRowIndex);
            assertEquals(List.of(1, 1, 1, 1, 1, 1),
                    blankDetailRow.stream().map(MesProBatchRecordParsedCell::getColSpan).toList(),
                    "空白填写行应只保留未被纵向主块覆盖的独立填写格，而不是退化成横向大块: "
                            + shapeOf(blankDetailRow));
            assertSame(cellByText(selfInspectionRow, "生产自检"), cellOccupying(rows, blankDetailRowIndex, 2),
                    "空白续行第 3-5 列应继续被生产自检纵向主块占用");
            assertSame(cellContainingText(selfInspectionRow, "合格标准"), cellOccupying(rows, blankDetailRowIndex, 5),
                    "空白续行正文列应继续被合格标准纵向主块占用");
            assertEquals("组装Ⅰ生产操作及自检记录", textOf(cellOccupying(rows, blankDetailRowIndex, 0)),
                    "空白续行第 1 列应继续被共享左侧主带占用");
            assertEquals(blankDetailRow.get(0), cellOccupying(rows, blankDetailRowIndex, 1),
                    "空白续行第 2 列应保留独立操作日期填写列");
            assertEquals(blankDetailRow.get(1), cellOccupying(rows, blankDetailRowIndex, 17),
                    "空白续行结果列起点应与头行尾部骨架对齐");
            assertEquals(blankDetailRow.get(5), cellOccupying(rows, blankDetailRowIndex, 20),
                    "空白续行操作人列起点应与头行尾部骨架对齐");
        }
    }

    @Test
    void calibrate_shouldKeepFirstSegmentAlignedWhenAnExtraNoteRowIsInserted() {
        MesProBatchRecordParsedTable parsedTable = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(2)
                .tableTitle("通用工序生产记录")
                .rowCount(9)
                .columnCount(20)
                .rows(List.of(
                        row("通用工序生产记录"),
                        row("生产批号", "", "产品规格", "", "生产依据", ""),
                        row("检查要求", "结果", "操作人/日期", "复核人/日期"),
                        row("备注：检查结果符合要求后进行以下生产操作"),
                        row("补充说明：请先确认设备状态与物料准备完成"),
                        row("分段A", "子项1", "子项2", "子项3", "子项4"),
                        row("2026-05-17", "A001", "弹簧", "B-01", "2", "纯化水", "100%"),
                        row("分段A", "", "子项1", "子项2", "子项3", "子项4"),
                        row("2026-05-18", "A002", "弹簧", "B-02", "2", "纯化水", "100%")
                ))
                .build();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);
        List<List<MesProBatchRecordParsedCell>> rows = calibrated.getRows();
        List<Integer> insertedNoteRows = findRows(rows, "补充说明：请先确认设备状态与物料准备完成");
        List<Integer> repeatedHeaderRows = findRows(rows, "分段A");

        assertEquals(1, insertedNoteRows.size());
        assertEquals(20, rows.get(insertedNoteRows.get(0)).get(0).getColSpan());
        assertEquals(2, repeatedHeaderRows.size());
        assertTrue(repeatedHeaderRows.get(0) > insertedNoteRows.get(0));
        assertEquals(20, rows.get(repeatedHeaderRows.get(0)).stream()
                .mapToInt(MesProBatchRecordParsedCell::getColSpan)
                .sum());
        assertEquals(20, rows.get(repeatedHeaderRows.get(1)).stream()
                .mapToInt(MesProBatchRecordParsedCell::getColSpan)
                .sum());
    }

    @Test
    void calibrate_shouldInsertContinuationHeaderBeforeLaterRepeatedOperationSegments() throws Exception {
        assumeFixedSampleAvailable();
        byte[] bytes = Files.readAllBytes(FIXED_SAMPLE);
        MesProBatchRecordParsedTable parsedTable = routeBRecognizer.recognize(
                        FIXED_SAMPLE, bytes, FIXED_SAMPLE.getFileName().toString())
                .stream()
                .filter(table -> "清洗工序生产记录".equals(table.getTableTitle()))
                .findFirst()
                .orElseThrow();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);
        List<List<MesProBatchRecordParsedCell>> rows = calibrated.getRows();
        List<Integer> documentHeaderRows = findRows(rows, "球囊扩张压力泵生产记录");
        int rackSegmentRow = findRowsContaining(rows, "齿条").get(0);
        int handleSegmentRow = findRowsContaining(rows, "手柄").get(0);

        assertEquals(1, documentHeaderRows.size(),
                "source-grid-preserved process pages should not insert synthetic continuation headers");
        assertTrue(handleSegmentRow < rackSegmentRow);
        assertEquals(parsedTable.getColumnCount(), calibrated.getColumnCount());
        assertTrue(calibrated.getColumnWidths() != null);
        assertEquals(calibrated.getColumnCount(), calibrated.getColumnWidths().size());
        assertTrue(Boolean.TRUE.equals(calibrated.getPreserveSourceGrid()));
        assertNoMergedCellExceedsColumnCount(calibrated);
    }

    @Test
    void calibrate_shouldNotInsertContinuationHeaderInsideSourceGridEquipmentMatrix() throws Exception {
        assumeFixedSampleAvailable();
        byte[] bytes = Files.readAllBytes(FIXED_SAMPLE);
        MesProBatchRecordParsedTable parsedTable = routeBRecognizer.recognize(
                        FIXED_SAMPLE, bytes, FIXED_SAMPLE.getFileName().toString())
                .stream()
                .filter(table -> "单包装工序生产记录".equals(table.getTableTitle()))
                .findFirst()
                .orElseThrow();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);
        List<List<MesProBatchRecordParsedCell>> rows = calibrated.getRows();
        List<Integer> documentHeaderRows = findRows(rows, "球囊扩张压力泵生产记录");
        List<Integer> heatSealRows = findRowsContaining(rows, "封口热合机");
        int selfInspectionRow = findRows(rows, "生产自检").get(0);

        assertEquals(1, documentHeaderRows.size(),
                "source-grid process pages must not add synthetic continuation headers absent from Word");
        assertTrue(heatSealRows.stream().allMatch(rowIndex -> rowIndex < selfInspectionRow));
        assertNoMergedCellExceedsColumnCount(calibrated);
    }

    @Test
    void calibrate_shouldInsertContinuationHeadersBeforeLaterOverviewSections() throws Exception {
        assumeFixedSampleAvailable();
        byte[] bytes = Files.readAllBytes(FIXED_SAMPLE);
        MesProBatchRecordParsedTable parsedTable = routeBRecognizer.recognize(
                        FIXED_SAMPLE, bytes, FIXED_SAMPLE.getFileName().toString())
                .stream()
                .filter(table -> "产品信息".equals(table.getTableTitle()))
                .findFirst()
                .orElseThrow();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);
        List<List<MesProBatchRecordParsedCell>> rows = calibrated.getRows();
        List<Integer> documentHeaderRows = findRows(rows, "球囊扩张压力泵生产记录");
        int accessoryPurchaseRow = findRows(rows, "配件进货批号信息").get(0);
        int assemblyRow = findRows(rows, "装配及包装信息").get(0);
        int releaseRow = findRows(rows, "过程放行信息").get(0);

        assertEquals(3, documentHeaderRows.size());
        assertTrue(documentHeaderRows.get(1) < accessoryPurchaseRow);
        assertTrue(documentHeaderRows.get(2) < assemblyRow);
        assertTrue(documentHeaderRows.get(2) < releaseRow);
        assertNoMergedCellExceedsColumnCount(calibrated);
    }

    @Test
    void calibrate_shouldNotMaterializeTrailingBlankResultColumnsAcrossRepeatedOperationBlocks() throws Exception {
        assumeFixedSampleAvailable();
        byte[] bytes = Files.readAllBytes(FIXED_SAMPLE);
        MesProBatchRecordParsedTable parsedTable = routeBRecognizer.recognize(
                        FIXED_SAMPLE, bytes, FIXED_SAMPLE.getFileName().toString())
                .stream()
                .filter(table -> "清洗工序生产记录".equals(table.getTableTitle()))
                .findFirst()
                .orElseThrow();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);
        List<List<MesProBatchRecordParsedCell>> rows = calibrated.getRows();

        long materializedBlankCells = rows.stream()
                .flatMap(List::stream)
                .filter(MesProBatchRecordParsedCell::isVisualBlank)
                .count();
        assertEquals(0, materializedBlankCells,
                "source-grid process pages must not materialize blank cells absent from Word");
        assertNoMergedCellExceedsColumnCount(calibrated);
    }

    @Test
    void calibrate_shouldClampWideTableIntoSinglePageWidthBudget() {
        List<MesProBatchRecordParsedCell> row = new ArrayList<>();
        for (int index = 0; index < 12; index++) {
            row.add(MesProBatchRecordParsedCell.builder()
                    .text("C" + index)
                    .widthPx(160)
                    .heightPx(40)
                    .fontSize(14)
                    .build());
        }
        MesProBatchRecordParsedTable parsedTable = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(2)
                .tableTitle("wide")
                .rowCount(1)
                .columnCount(12)
                .rows(List.of(row))
                .build();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);

        int maxRowWidth = calibrated.getRows().stream()
                .mapToInt(cells -> cells.stream().mapToInt(MesProBatchRecordParsedCell::getWidthPx).sum())
                .max()
                .orElse(0);

        assertTrue(maxRowWidth <= MesProBatchRecordReportShapeRules.resolveTargetRenderWidth(12));
        assertTrue(calibrated.getRows().stream()
                .flatMap(List::stream)
                .allMatch(cell -> cell.getWidthPx() <= MesProBatchRecordReportShapeRules.resolveTargetRenderWidth(12)));
    }

    @Test
    void calibrate_shouldReserveMoreWidthForDenseTailColumns() {
        MesProBatchRecordParsedTable parsedTable = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(2)
                .tableTitle("通用工序生产记录")
                .rowCount(7)
                .columnCount(20)
                .rows(List.of(
                        row("通用工序生产记录"),
                        row("生产批号", "", "产品规格", "", "生产依据", ""),
                        row("检查要求", "结果", "操作人/日期", "复核人/日期"),
                        row("备注：确认环境、工具与物料满足进入工序前要求"),
                        row("操作日期", "物料编码", "物料名称", "批号", "清洗次数", "清洗介质", "清洗功率",
                                "清洗温度", "清洗时间", "生产数量/pcs", "自检合格数量/pcs", "不合格数量/pcs",
                                "操作人", "复核人"),
                        row("参考值", "实际", "参考值", "实际", "参考值", "实际", "参考值", "实际", "参考值", "实际"),
                        row("2026-05-17", "A001", "弹簧", "B-01", "2", "纯化水", "100%", "室温", "30min",
                                "120", "118", "2", "张三", "李四")
                ))
                .build();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);
        List<MesProBatchRecordParsedCell> headerRow = calibrated.getRows().stream()
                .filter(row -> !row.isEmpty() && "操作日期".equals(textOf(row.get(0))))
                .findFirst()
                .orElseThrow();

        MesProBatchRecordParsedCell batchCell = cellByText(headerRow, "批号");
        MesProBatchRecordParsedCell quantityCell = cellByText(headerRow, "生产数量/pcs");
        MesProBatchRecordParsedCell operatorCell = cellByText(headerRow, "操作人");
        MesProBatchRecordParsedCell reviewerCell = cellByText(headerRow, "复核人");

        assertTrue(quantityCell.getWidthPx() >= 44);
        assertTrue(operatorCell.getWidthPx() >= 40);
        assertTrue(reviewerCell.getWidthPx() >= 40);
        assertTrue(headerRow.stream().mapToInt(MesProBatchRecordParsedCell::getWidthPx).sum()
                <= MesProBatchRecordReportShapeRules.resolveTargetRenderWidth(20));
    }

    @Test
    void calibrate_shouldUseDensePageWidthBudgetForHighColumnCountTables() {
        List<MesProBatchRecordParsedCell> row = new ArrayList<>();
        for (int index = 0; index < 20; index++) {
            row.add(MesProBatchRecordParsedCell.builder()
                    .text("C" + index)
                    .widthPx(160)
                    .heightPx(40)
                    .fontSize(14)
                    .build());
        }
        MesProBatchRecordParsedTable parsedTable = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(2)
                .tableTitle("dense")
                .rowCount(1)
                .columnCount(20)
                .rows(List.of(row))
                .build();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);

        int maxRowWidth = calibrated.getRows().stream()
                .mapToInt(cells -> cells.stream().mapToInt(MesProBatchRecordParsedCell::getWidthPx).sum())
                .max()
                .orElse(0);

        assertTrue(maxRowWidth <= MesProBatchRecordReportShapeRules.resolveTargetRenderWidth(20));
        assertTrue(maxRowWidth >= MesProBatchRecordReportShapeRules.TARGET_RENDER_WIDTH_PX);
    }

    @Test
    void calibrate_shouldExpandMixedNarrativeProcessRowsInsteadOfCollapsingThemIntoNarrowColumns() {
        MesProBatchRecordParsedTable parsedTable = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(3)
                .tableTitle("通用工序生产记录")
                .rowCount(6)
                .columnCount(20)
                .rows(List.of(
                        row("通用工序生产记录"),
                        row("生产批号", "", "产品规格", "", "生产依据", ""),
                        row("检查要求", "结果", "操作人/日期", "复核人/日期"),
                        row("1、工作场所无上批遗留的产品、文件或与本批产品生产无关的物料、工具；"
                                        + "2、墙面、地面、天花板、灯具等环境清洁应符合要求。",
                                "□符合要求\n□不符合要求", "", ""),
                        row("操作日期", "物料编码", "物料名称", "批号", "生产数量/pcs", "自检合格数量/pcs", "不合格数量/pcs", "操作人", "复核人"),
                        row("2026-05-17", "/", "弹簧", "B-01", "120", "118", "2", "张三", "李四")
                ))
                .build();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);

        List<MesProBatchRecordParsedCell> narrativeRow = calibrated.getRows().stream()
                .filter(row -> !row.isEmpty() && row.get(0).getText() != null
                        && row.get(0).getText().startsWith("1、工作场所无上批遗留"))
                .findFirst()
                .orElseThrow();

        MesProBatchRecordParsedCell narrativeCell = narrativeRow.get(0);
        MesProBatchRecordParsedCell resultCell = narrativeRow.get(1);

        assertEquals("left", narrativeCell.getHorizontalAlign());
        assertTrue(narrativeCell.getWidthPx() >= resultCell.getWidthPx() * 2);
        assertTrue(narrativeRow.stream().mapToInt(MesProBatchRecordParsedCell::getWidthPx).sum()
                >= MesProBatchRecordReportShapeRules.resolveTargetRenderWidth(20));
    }

    @Test
    void calibrate_shouldStretchSummaryRowsAfterDetailBlocksUsingSharedRowTypeRecognition() {
        MesProBatchRecordParsedTable parsedTable = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(3)
                .tableTitle("通用工序生产记录")
                .rowCount(7)
                .columnCount(20)
                .rows(List.of(
                        row("通用工序生产记录"),
                        row("生产批号", "", "产品规格", "", "生产依据", ""),
                        row("操作日期", "物料编码", "物料名称", "批号", "生产数量/pcs", "自检合格数量/pcs", "不合格数量/pcs", "操作人", "复核人"),
                        row("2026-05-17", "A001", "弹簧", "B-01", "120", "118", "2", "张三", "李四"),
                        row("生产批量汇总", "合格数量", "118", "不合格数量", "2"),
                        row("备注：汇总数量需与批记录明细一致"),
                        row("生效日期：2026年02月02日")
                ))
                .build();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);

        List<MesProBatchRecordParsedCell> summaryRow = calibrated.getRows().stream()
                .filter(row -> !row.isEmpty() && "生产批量汇总".equals(textOf(row.get(0))))
                .findFirst()
                .orElseThrow();

        assertEquals(20, summaryRow.stream().mapToInt(MesProBatchRecordParsedCell::getColSpan).sum());
        assertTrue(summaryRow.stream().mapToInt(MesProBatchRecordParsedCell::getWidthPx).sum()
                >= MesProBatchRecordReportShapeRules.resolveTargetRenderWidth(20));
        assertTrue(summaryRow.stream().allMatch(MesProBatchRecordParsedCell::isBold));
        assertTrue(summaryRow.stream().allMatch(cell -> cell.getHeightPx() >= 28));
    }

    @Test
    void calibrate_shouldUseSharedTargetWidthForOverviewPagesWithStandaloneInfoTitles() {
        MesProBatchRecordParsedTable parsedTable = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("\u88c5\u914d\u53ca\u5305\u88c5\u4fe1\u606f")
                .rowCount(3)
                .columnCount(6)
                .rows(List.of(
                        row("\u88c5\u914d\u53ca\u5305\u88c5\u4fe1\u606f"),
                        row("\u5de5\u5e8f\u540d\u79f0", "\u64cd\u4f5c\u4eba\u5458", "\u88c5\u914d\u65e5\u671f",
                                "\u5de5\u5e8f\u540d\u79f0", "\u64cd\u4f5c\u4eba\u5458", "\u88c5\u914d\u65e5\u671f"),
                        row("\u7c97\u6d17", "", "", "\u7845\u5316\u2161", "", "")
                ))
                .build();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);

        int maxRowWidth = calibrated.getRows().stream()
                .filter(row -> !row.isEmpty())
                .mapToInt(row -> row.stream().mapToInt(MesProBatchRecordParsedCell::getWidthPx).sum())
                .max()
                .orElse(0);

        assertTrue(maxRowWidth >= MesProBatchRecordReportShapeRules.resolveTargetRenderWidth(6));
    }

    @Test
    void calibrate_shouldPreserveSourceWidthsForFirstSummaryOverviewMaterialMatrix() {
        List<Integer> sourceColumnWidths = List.of(100, 50, 50, 50, 50, 100, 50, 50, 50, 50);
        MesProBatchRecordParsedTable parsedTable = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("球囊扩张压力泵生产记录")
                .rowCount(7)
                .columnCount(10)
                .columnWidths(sourceColumnWidths)
                .rows(List.of(
                        sourceRow(sourceCell("球囊扩张压力泵生产记录", 0, 10)),
                        sourceRow(sourceCell("产品信息", 0, 10)),
                        sourceRow(
                                sourceCell("生产批号", 0, 4),
                                sourceCell("", 4, 1),
                                sourceCell("生产数量", 5, 4),
                                sourceCell("", 9, 1)),
                        sourceRow(
                                sourceCell("生产指令", 0, 4),
                                sourceCell("", 4, 1),
                                sourceCell("生产订单号", 5, 4),
                                sourceCell("", 9, 1)),
                        sourceRow(
                                sourceCell("图号", 0, 1),
                                sourceCell("", 1, 4),
                                sourceCell("生产周期", 5, 1),
                                sourceCell("", 6, 4)),
                        sourceRow(sourceCell("生产零配件批号信息", 0, 10)),
                        sourceRow(
                                sourceCell("物料编码", 0, 1),
                                sourceCell("物料名称", 1, 2),
                                sourceCell("物料批号", 3, 2),
                                sourceCell("物料编码", 5, 1),
                                sourceCell("物料名称", 6, 2),
                                sourceCell("物料批号", 8, 2))
                ))
                .build();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);

        assertFalse(calibrated.getPreserveSourceGrid());
        assertEquals(1120, sum(calibrated.getColumnWidths()));
        List<MesProBatchRecordParsedCell> drawingNoRow = calibrated.getRows().stream()
                .filter(row -> row.stream().anyMatch(cell -> "图号".equals(textOf(cell))))
                .findFirst()
                .orElseThrow();
        assertEquals(List.of(1, 4, 1, 4),
                drawingNoRow.stream().map(MesProBatchRecordParsedCell::getColSpan).toList());
        int drawingNoGroupWidth = drawingNoRow.get(0).getWidthPx() + drawingNoRow.get(1).getWidthPx();
        int productionCycleGroupWidth = drawingNoRow.get(2).getWidthPx() + drawingNoRow.get(3).getWidthPx();
        assertTrue(Math.abs(drawingNoGroupWidth - productionCycleGroupWidth) <= 2);

        List<MesProBatchRecordParsedCell> materialHeaderRow = calibrated.getRows().stream()
                .filter(row -> row.size() == 6 && row.stream()
                        .filter(cell -> textOf(cell).startsWith("物料"))
                        .count() == 6)
                .findFirst()
                .orElseThrow();
        assertEquals(List.of(1, 2, 2, 1, 2, 2),
                materialHeaderRow.stream().map(MesProBatchRecordParsedCell::getColSpan).toList());
        List<Integer> materialHeaderWidths = materialHeaderRow.stream()
                .map(MesProBatchRecordParsedCell::getWidthPx)
                .toList();
        int minMaterialHeaderWidth = materialHeaderWidths.stream().mapToInt(Integer::intValue).min().orElseThrow();
        int maxMaterialHeaderWidth = materialHeaderWidths.stream().mapToInt(Integer::intValue).max().orElseThrow();
        assertTrue(maxMaterialHeaderWidth - minMaterialHeaderWidth <= 1,
                "materialHeaderWidths=" + materialHeaderWidths);
    }

    @Test
    void calibrate_shouldKeepActualPressurePumpFirstSummaryMaterialMatrixWidths() throws Exception {
        Assumptions.assumeTrue(Files.exists(PRESSURE_PUMP_SAMPLE),
                "pressure pump source doc is not available on this machine");
        byte[] bytes = Files.readAllBytes(PRESSURE_PUMP_SAMPLE);
        MesProBatchRecordParsedTable parsedTable = routeBRecognizer.recognize(
                        PRESSURE_PUMP_SAMPLE, bytes, PRESSURE_PUMP_SAMPLE.getFileName().toString())
                .stream()
                .filter(table -> "产品信息".equals(table.getTableTitle()))
                .findFirst()
                .orElseThrow();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);

        List<MesProBatchRecordParsedCell> drawingNoRow = calibrated.getRows().stream()
                .filter(row -> row.stream().anyMatch(cell -> "图号".equals(textOf(cell))))
                .findFirst()
                .orElseThrow();
        assertEquals(List.of(1, 4, 1, 4),
                drawingNoRow.stream().map(MesProBatchRecordParsedCell::getColSpan).toList());
        int drawingNoGroupWidth = drawingNoRow.get(0).getWidthPx() + drawingNoRow.get(1).getWidthPx();
        int productionCycleGroupWidth = drawingNoRow.get(2).getWidthPx() + drawingNoRow.get(3).getWidthPx();
        assertTrue(Math.abs(drawingNoGroupWidth - productionCycleGroupWidth) <= 2,
                "drawingNoGroupWidth=" + drawingNoGroupWidth
                        + ", productionCycleGroupWidth=" + productionCycleGroupWidth);

        List<MesProBatchRecordParsedCell> materialHeaderRow = calibrated.getRows().stream()
                .filter(row -> row.size() == 6 && row.stream()
                        .filter(cell -> textOf(cell).startsWith("物料"))
                        .count() == 6)
                .findFirst()
                .orElseThrow();
        assertEquals(List.of(1, 2, 2, 1, 2, 2),
                materialHeaderRow.stream().map(MesProBatchRecordParsedCell::getColSpan).toList());
        List<Integer> materialHeaderWidths = materialHeaderRow.stream()
                .map(MesProBatchRecordParsedCell::getWidthPx)
                .toList();
        int minMaterialHeaderWidth = materialHeaderWidths.stream().mapToInt(Integer::intValue).min().orElseThrow();
        int maxMaterialHeaderWidth = materialHeaderWidths.stream().mapToInt(Integer::intValue).max().orElseThrow();
        assertTrue(maxMaterialHeaderWidth - minMaterialHeaderWidth <= 1,
                "materialHeaderWidths=" + materialHeaderWidths);
    }

    @Test
    void calibrate_shouldAlignActualPressurePumpProductNameLabelWithOverviewFieldLabels() throws Exception {
        Assumptions.assumeTrue(Files.exists(PRESSURE_PUMP_SAMPLE),
                "pressure pump source doc is not available on this machine");
        byte[] bytes = Files.readAllBytes(PRESSURE_PUMP_SAMPLE);
        MesProBatchRecordParsedTable parsedTable = routeBRecognizer.recognize(
                        PRESSURE_PUMP_SAMPLE, bytes, PRESSURE_PUMP_SAMPLE.getFileName().toString())
                .stream()
                .filter(table -> "产品信息".equals(table.getTableTitle()))
                .findFirst()
                .orElseThrow();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);

        List<MesProBatchRecordParsedCell> productNameRow = rowContaining(calibrated, "产品名称");
        List<MesProBatchRecordParsedCell> productionBatchRow = rowContaining(calibrated, "生产批号");
        assertEquals(productionBatchRow.get(0).getWidthPx(), productNameRow.get(0).getWidthPx(),
                "产品名称左侧标签列应与下方字段标签列对齐; productNameRow="
                        + describeRow(productNameRow) + ", productionBatchRow=" + describeRow(productionBatchRow));
    }

    @Test
    void calibrate_shouldKeepActualPressurePumpMaterialDetailRowsAtOverviewFieldHeight() throws Exception {
        Assumptions.assumeTrue(Files.exists(PRESSURE_PUMP_SAMPLE),
                "pressure pump source doc is not available on this machine");
        byte[] bytes = Files.readAllBytes(PRESSURE_PUMP_SAMPLE);
        MesProBatchRecordParsedTable parsedTable = routeBRecognizer.recognize(
                        PRESSURE_PUMP_SAMPLE, bytes, PRESSURE_PUMP_SAMPLE.getFileName().toString())
                .stream()
                .filter(table -> "产品信息".equals(table.getTableTitle()))
                .findFirst()
                .orElseThrow();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);

        int fieldRowHeight = rowHeight(rowContaining(calibrated, "生产批号"));
        for (String materialText : List.of("螺盖", "齿条", "芯杆", "套筒")) {
            List<MesProBatchRecordParsedCell> materialRow = rowContaining(calibrated, materialText);
            assertTrue(rowHeight(materialRow) >= fieldRowHeight,
                    "物料明细行不应低于汇总字段行; materialText=" + materialText
                            + ", materialRow=" + describeRow(materialRow)
                            + ", materialRowHeight=" + rowHeight(materialRow)
                            + ", fieldRowHeight=" + fieldRowHeight);
        }
    }

    @Test
    void calibrate_shouldUseProcessWidthBudgetWhenTableTitleMarksAProcessPageWithoutStandaloneTitleRow() {
        MesProBatchRecordParsedTable parsedTable = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(5)
                .tableTitle("\u6e05\u6d01\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55")
                .rowCount(6)
                .columnCount(10)
                .rows(List.of(
                        row("\u751f\u4ea7\u6279\u53f7", "", "\u4ea7\u54c1\u89c4\u683c", "", "\u751f\u4ea7\u4f9d\u636e", ""),
                        row("\u68c0\u67e5\u8981\u6c42", "\u7ed3\u679c", "\u64cd\u4f5c\u4eba/\u65e5\u671f", "\u590d\u6838\u4eba/\u65e5\u671f"),
                        row("1\u3001\u5de5\u4f5c\u573a\u6240\u65e0\u4e0a\u6279\u9057\u7559\u7684\u4ea7\u54c1\u3001\u6587\u4ef6\u6216\u4e0e\u672c\u6279\u4ea7\u54c1\u751f\u4ea7\u65e0\u5173\u7684\u7269\u6599\u3001\u5de5\u5177\uff1b"
                                        + "2\u3001\u5899\u9762\u3001\u5730\u9762\u3001\u5929\u82b1\u677f\u3001\u706f\u5177\u7b49\u73af\u5883\u6e05\u6d01\u5e94\u7b26\u5408\u8981\u6c42\u3002",
                                "\u25a1\u7b26\u5408\u8981\u6c42\\n\u25a1\u4e0d\u7b26\u5408\u8981\u6c42", "", ""),
                        row("\u64cd\u4f5c\u65e5\u671f", "\u7269\u6599\u7f16\u7801", "\u7269\u6599\u540d\u79f0", "\u6279\u53f7",
                                "\u751f\u4ea7\u6570\u91cf/pcs", "\u81ea\u68c0\u5408\u683c\u6570\u91cf/pcs", "\u4e0d\u5408\u683c\u6570\u91cf/pcs",
                                "\u64cd\u4f5c\u4eba", "\u590d\u6838\u4eba"),
                        row("2026-05-17", "/", "\u538b\u529b\u8868", "B-01", "120", "118", "2", "\u5f20\u4e09", "\u674e\u56db"),
                        row("\u751f\u4ea7\u6279\u91cf\u6c47\u603b", "\u5408\u683c\u6570\u91cf", "118", "\u4e0d\u5408\u683c\u6570\u91cf", "2")
                ))
                .build();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);

        int maxRowWidth = calibrated.getRows().stream()
                .filter(row -> !row.isEmpty())
                .mapToInt(row -> row.stream().mapToInt(MesProBatchRecordParsedCell::getWidthPx).sum())
                .max()
                .orElse(0);

        assertTrue(maxRowWidth >= MesProBatchRecordReportShapeRules.resolveTargetRenderWidth(10),
                "maxRowWidth=" + maxRowWidth);
    }

    @Test
    void calibrate_shouldStretchSharedProcessColumnsToTargetWidthEvenWhenOnlyDetailRowsStayNarrow() {
        MesProBatchRecordParsedTable parsedTable = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(5)
                .tableTitle("\u6e05\u6d01\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55")
                .rowCount(2)
                .columnCount(10)
                .rows(List.of(
                        row("2026-05-17", "/", "\u538b\u529b\u8868", "B-01", "120", "118", "2", "\u5f20\u4e09", "\u674e\u56db"),
                        row("2026-05-18", "/", "\u538b\u529b\u8868", "B-02", "120", "118", "2", "\u738b\u4e94", "\u8d75\u516d")
                ))
                .build();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);

        assertEquals(MesProBatchRecordReportShapeRules.resolveTargetRenderWidth(10), sum(calibrated.getColumnWidths()));
    }

    @Test
    void calibrate_shouldCompressTallRowsIntoSinglePageHeightBudget() {
        List<List<MesProBatchRecordParsedCell>> rows = new ArrayList<>();
        for (int index = 0; index < 30; index++) {
            rows.add(List.of(MesProBatchRecordParsedCell.builder()
                    .text("需要填写的长文本内容" + index)
                    .widthPx(220)
                    .heightPx(72)
                    .fontSize(16)
                    .build()));
        }
        MesProBatchRecordParsedTable parsedTable = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(2)
                .tableTitle("tall")
                .rowCount(rows.size())
                .columnCount(1)
                .rows(rows)
                .build();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);

        int totalHeight = calibrated.getRows().stream()
                .filter(row -> !row.isEmpty())
                .map(row -> row.stream().mapToInt(MesProBatchRecordParsedCell::getHeightPx).max().orElse(0))
                .reduce(0, Integer::sum);

        assertTrue(totalHeight <= MesProBatchRecordReportShapeRules.SINGLE_PAGE_MAX_HEIGHT_PX);
        assertTrue(calibrated.getRows().stream()
                .flatMap(List::stream)
                .allMatch(cell -> cell.getFontSize() <= 10 && cell.getFontSize() >= 8));
        assertTrue(calibrated.getRows().stream()
                .flatMap(List::stream)
                .allMatch(cell -> cell.getHeightPx() <= 36 && cell.getHeightPx() >= 18));
    }

    @Test
    void calibrate_shouldShrinkFieldAndDetailRowsMoreAggressivelyThanSummaryRowsToKeepLowerBlocksVisible() {
        List<List<MesProBatchRecordParsedCell>> rows = new ArrayList<>();
        rows.add(row("\u901a\u7528\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55"));
        rows.add(row("\u751f\u4ea7\u6279\u53f7", "", "\u4ea7\u54c1\u89c4\u683c", "", "\u751f\u4ea7\u4f9d\u636e", ""));
        rows.add(row("\u64cd\u4f5c\u65e5\u671f", "\u7269\u6599\u7f16\u7801", "\u7269\u6599\u540d\u79f0", "\u6279\u53f7",
                "\u751f\u4ea7\u6570\u91cf/pcs", "\u81ea\u68c0\u5408\u683c\u6570\u91cf/pcs", "\u4e0d\u5408\u683c\u6570\u91cf/pcs",
                "\u64cd\u4f5c\u4eba", "\u590d\u6838\u4eba"));
        for (int index = 0; index < 30; index++) {
            rows.add(row("2026-05-17", "A00" + index, "\u5f39\u7c27", "B-" + index, "120", "118", "2", "\u5f20\u4e09", "\u674e\u56db"));
        }
        rows.add(row("\u751f\u4ea7\u6279\u91cf\u6c47\u603b", "\u5408\u683c\u6570\u91cf", "118", "\u4e0d\u5408\u683c\u6570\u91cf", "2"));
        rows.add(row("\u751f\u6548\u65e5\u671f\uff1a2026\u5e7402\u670802\u65e5"));
        MesProBatchRecordParsedTable parsedTable = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(2)
                .tableTitle("\u901a\u7528\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55")
                .rowCount(rows.size())
                .columnCount(20)
                .rows(rows)
                .build();

        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsedTable);

        List<MesProBatchRecordParsedCell> summaryRow = calibrated.getRows().stream()
                .filter(row -> !row.isEmpty() && "\u751f\u4ea7\u6279\u91cf\u6c47\u603b".equals(textOf(row.get(0))))
                .findFirst()
                .orElseThrow();
        List<MesProBatchRecordParsedCell> detailRow = calibrated.getRows().stream()
                .filter(row -> !row.isEmpty() && "2026-05-17".equals(textOf(row.get(0))))
                .findFirst()
                .orElseThrow();

        assertTrue(summaryRow.stream().allMatch(cell -> cell.getHeightPx() >= 22));
        assertTrue(detailRow.stream().allMatch(cell -> cell.getHeightPx() <= 18));
        assertTrue(summaryRow.get(0).getHeightPx() > detailRow.get(0).getHeightPx());
    }

    private int sum(List<Integer> values) {
        return values.stream().reduce(0, Integer::sum);
    }

    private int sumVisibleRowHeights(MesProBatchRecordParsedTable table) {
        return table.getRows().stream()
                .filter(row -> !row.isEmpty())
                .map(row -> row.stream().mapToInt(MesProBatchRecordParsedCell::getHeightPx).max().orElse(0))
                .reduce(0, Integer::sum);
    }

    private MesProBatchRecordParsedTable buildDetailHeavyStructuredTable(int detailRowCount) {
        List<List<MesProBatchRecordParsedCell>> rows = new ArrayList<>();
        rows.add(row("通用工序生产记录"));
        rows.add(row("生产批号", "", "产品规格", "", "生产依据", ""));
        rows.add(row("操作日期", "物料编码", "物料名称", "批号",
                "生产数量/pcs", "自检合格数量/pcs", "不合格数量/pcs", "操作人", "复核人"));
        for (int index = 0; index < detailRowCount; index++) {
            rows.add(row("2026-05-17", "A00" + index, "弹簧", "B-" + index, "120", "118", "2", "张三", "李四"));
        }
        rows.add(row("生产批量汇总", "合格数量", "118", "不合格数量", "2"));
        rows.add(row("生效日期：2026年02月02日"));
        return MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(2)
                .tableTitle("通用工序生产记录")
                .rowCount(rows.size())
                .columnCount(20)
                .rows(rows)
                .build();
    }

    private MesProBatchRecordParsedTable buildStructuredTailAfterSummaryTable() {
        List<List<MesProBatchRecordParsedCell>> rows = new ArrayList<>();
        rows.add(row("通用工序生产记录"));
        rows.add(row("生产批号", "", "产品规格", "", "生产依据", ""));
        rows.add(row("设备编码", "超声波清洗机：T01", "是否在计量效期内", "□是", "□否"));
        rows.add(row("检查要求", "结果", "操作人/日期", "复核人/日期"));
        rows.add(row("备注：检查结果符合要求后进行以下生产操作"));
        rows.add(row("物料批号", "", "清洗介质", "", "清洗时间", ""));
        rows.add(row("清洗温度", "", "操作人员", "", "复核人员", ""));
        rows.add(row("操作日期", "物料编码", "物料名称", "批号",
                "生产数量/pcs", "自检合格数量/pcs", "不合格数量/pcs", "操作人", "复核人"));
        for (int index = 0; index < 5; index++) {
            rows.add(row("2026-05-17", "A00" + index, "弹簧", "B-" + index, "120", "118", "2", "张三", "李四"));
        }
        rows.add(row("生产批量汇总", "合格数量", "118", "不合格数量", "2"));
        rows.add(row("生产后清场记录", "结果", "操作人/日期", "复核人/日期"));
        rows.add(row("1、工作场所无上批遗留的产品、文件或与本批产品生产无关的物料、工具；",
                "□符合要求\n□不符合要求", "", ""));
        rows.add(row("2、墙面、地面、天花板、灯具等环境清洁应符合要求。",
                "□符合要求\n□不符合要求", "", ""));
        rows.add(row("生效日期：2026年02月02日"));
        return MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(2)
                .tableTitle("通用工序生产记录")
                .rowCount(rows.size())
                .columnCount(20)
                .rows(rows)
                .build();
    }

    private void assumePilotSampleAvailable() {
        Assumptions.assumeTrue(Files.exists(PILOT_SAMPLE), "pilot sample doc fixture is not available on this machine");
    }

    private void assumeFixedSampleAvailable() {
        Assumptions.assumeTrue(Files.exists(FIXED_SAMPLE), "fixed route-B source doc is not available on this machine");
    }

    private List<Integer> findRows(List<List<MesProBatchRecordParsedCell>> rows, String firstCellText) {
        List<Integer> indexes = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            List<MesProBatchRecordParsedCell> row = rows.get(index);
            if (!row.isEmpty() && firstCellText.equals(textOf(row.get(0)))) {
                indexes.add(index);
            }
        }
        return indexes;
    }

    private List<Integer> findRowsContaining(List<List<MesProBatchRecordParsedCell>> rows, String text) {
        List<Integer> indexes = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            boolean found = rows.get(index).stream()
                    .anyMatch(cell -> textOf(cell).contains(text));
            if (found) {
                indexes.add(index);
            }
        }
        return indexes;
    }

    private int findFirstRowContainingAll(List<List<MesProBatchRecordParsedCell>> rows, List<String> fragments) {
        for (int index = 0; index < rows.size(); index++) {
            String text = rowText(rows.get(index));
            boolean matched = true;
            for (String fragment : fragments) {
                if (!text.contains(fragment)) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                return index;
            }
        }
        throw new AssertionError("row not found for fragments: " + fragments);
    }

    private List<MesProBatchRecordParsedCell> rowContaining(MesProBatchRecordParsedTable table, String text) {
        return table.getRows().stream()
                .filter(row -> row.stream().anyMatch(cell -> textOf(cell).contains(text)))
                .findFirst()
                .orElseThrow();
    }

    private String describeRow(List<MesProBatchRecordParsedCell> row) {
        return row.stream()
                .map(cell -> textOf(cell) + ":" + cell.getWidthPx() + "px/" + cell.getColSpan()
                        + "/" + cell.getHeightPx() + "h")
                .toList()
                .toString();
    }

    private int rowHeight(List<MesProBatchRecordParsedCell> row) {
        return row.stream()
                .mapToInt(MesProBatchRecordParsedCell::getHeightPx)
                .max()
                .orElse(0);
    }

    private List<Integer> findBlankOperationBandDetailRows(List<List<MesProBatchRecordParsedCell>> rows, int startIndex) {
        List<Integer> indexes = new ArrayList<>();
        for (int index = startIndex; index < rows.size(); index++) {
            List<MesProBatchRecordParsedCell> row = rows.get(index);
            if (row.isEmpty()) {
                continue;
            }
            if (row.stream().anyMatch(cell -> "生产批量汇总".equals(textOf(cell)))) {
                break;
            }
            if (row.stream().allMatch(this::isBlankCell)) {
                indexes.add(index);
            }
        }
        return indexes;
    }

    private MesProBatchRecordParsedCell cellOccupying(List<List<MesProBatchRecordParsedCell>> rows,
                                                      int targetRowIndex,
                                                      int targetColumnIndex) {
        Map<Integer, OccupiedCell> occupiedByColumn = new HashMap<>();
        for (int rowIndex = 0; rowIndex <= targetRowIndex; rowIndex++) {
            int columnIndex = 0;
            for (MesProBatchRecordParsedCell cell : rows.get(rowIndex)) {
                while (occupiedByColumn.containsKey(columnIndex)
                        && occupiedByColumn.get(columnIndex).untilRow >= rowIndex) {
                    columnIndex++;
                }
                int rowSpan = Math.max(cell.getRowSpan(), 1);
                int colSpan = Math.max(cell.getColSpan(), 1);
                for (int offset = 0; offset < colSpan; offset++) {
                    occupiedByColumn.put(columnIndex + offset, new OccupiedCell(cell, rowIndex + rowSpan - 1));
                }
                columnIndex += colSpan;
            }
        }
        OccupiedCell occupiedCell = occupiedByColumn.get(targetColumnIndex);
        if (occupiedCell == null || occupiedCell.untilRow < targetRowIndex) {
            throw new AssertionError("No cell occupies row " + targetRowIndex + ", column " + targetColumnIndex);
        }
        return occupiedCell.cell;
    }

    private boolean isBlankCell(MesProBatchRecordParsedCell cell) {
        return textOf(cell).isBlank() || cell.isVisualBlank();
    }

    private void assertRowShapeEquals(List<MesProBatchRecordParsedCell> expected, List<MesProBatchRecordParsedCell> actual) {
        assertEquals(expected.size(), actual.size());
        for (int index = 0; index < expected.size(); index++) {
            MesProBatchRecordParsedCell expectedCell = expected.get(index);
            MesProBatchRecordParsedCell actualCell = actual.get(index);
            assertEquals(expectedCell.getRowSpan(), actualCell.getRowSpan(), "rowSpan mismatch at cell " + index);
            assertEquals(expectedCell.getColSpan(), actualCell.getColSpan(), "colSpan mismatch at cell " + index);
            assertEquals(expectedCell.getWidthPx(), actualCell.getWidthPx(), "width mismatch at cell " + index);
            assertEquals(expectedCell.getHeightPx(), actualCell.getHeightPx(), "height mismatch at cell " + index);
            assertEquals(expectedCell.isBold(), actualCell.isBold(), "bold mismatch at cell " + index);
        }
    }

    private static void assertNoMergedCellExceedsColumnCount(MesProBatchRecordParsedTable table) {
        Map<Integer, Integer> blockedUntilRowByColumn = new HashMap<>();
        for (int rowIndex = 0; rowIndex < table.getRows().size(); rowIndex++) {
            int columnIndex = 0;
            for (MesProBatchRecordParsedCell cell : table.getRows().get(rowIndex)) {
                while (blockedUntilRowByColumn.getOrDefault(columnIndex, -1) >= rowIndex) {
                    columnIndex++;
                }
                assertTrue(columnIndex + cell.getColSpan() <= table.getColumnCount(),
                        "cell at row " + rowIndex + " exceeds fixed column count");
                if (cell.getRowSpan() > 1) {
                    for (int offset = 0; offset < cell.getColSpan(); offset++) {
                        blockedUntilRowByColumn.put(columnIndex + offset, rowIndex + cell.getRowSpan() - 1);
                    }
                }
                columnIndex += Math.max(1, cell.getColSpan());
            }
        }
    }

    private static void assertNoSourceIndexedMergedCellExceedsColumnCount(MesProBatchRecordParsedTable table) {
        for (int rowIndex = 0; rowIndex < table.getRows().size(); rowIndex++) {
            List<MesProBatchRecordParsedCell> row = table.getRows().get(rowIndex);
            int cursor = 0;
            for (MesProBatchRecordParsedCell cell : row) {
                int startColumn = cell.getColumnIndex() == null ? cursor : cell.getColumnIndex();
                int endColumn = startColumn + Math.max(1, cell.getColSpan());
                assertTrue(endColumn <= table.getColumnCount(),
                        "cell at row " + rowIndex + " exceeds source-indexed column count");
                cursor = Math.max(cursor, endColumn);
            }
        }
    }

    private static void assertAllRowsCoverDeclaredColumnCount(MesProBatchRecordParsedTable table) {
        Map<Integer, OccupiedCell> occupiedByColumn = new HashMap<>();
        for (int rowIndex = 0; rowIndex < table.getRows().size(); rowIndex++) {
            int columnIndex = 0;
            for (MesProBatchRecordParsedCell cell : table.getRows().get(rowIndex)) {
                if (cell.getColumnIndex() != null) {
                    columnIndex = Math.max(0, cell.getColumnIndex());
                }
                while (occupiedByColumn.containsKey(columnIndex)
                        && occupiedByColumn.get(columnIndex).untilRow >= rowIndex) {
                    columnIndex++;
                }
                int rowSpan = Math.max(cell.getRowSpan(), 1);
                int colSpan = Math.max(cell.getColSpan(), 1);
                for (int offset = 0; offset < colSpan; offset++) {
                    occupiedByColumn.put(columnIndex + offset, new OccupiedCell(cell, rowIndex + rowSpan - 1));
                }
                columnIndex += colSpan;
            }
            for (int targetColumn = 0; targetColumn < table.getColumnCount(); targetColumn++) {
                OccupiedCell occupiedCell = occupiedByColumn.get(targetColumn);
                assertTrue(occupiedCell != null && occupiedCell.untilRow >= rowIndex,
                        "row " + rowIndex + " column " + targetColumn
                                + " should be covered to avoid right-edge blank gaps: "
                                + shapeOfStatic(table.getRows().get(rowIndex)));
            }
        }
    }

    private static String shapeOfStatic(List<MesProBatchRecordParsedCell> row) {
        StringBuilder builder = new StringBuilder();
        for (MesProBatchRecordParsedCell cell : row) {
            builder.append("[")
                    .append(cell.getRowSpan())
                    .append("x")
                    .append(cell.getColSpan())
                    .append("@")
                    .append(cell.getColumnIndex())
                    .append("]")
                    .append(cell.getText() == null ? "" : cell.getText().trim());
        }
        return builder.toString();
    }

    private static int rowEnd(List<MesProBatchRecordParsedCell> row) {
        int cursor = 0;
        for (MesProBatchRecordParsedCell cell : row) {
            int startColumn = cell.getColumnIndex() == null ? cursor : cell.getColumnIndex();
            cursor = Math.max(cursor, startColumn + Math.max(1, cell.getColSpan()));
        }
        return cursor;
    }

    private List<MesProBatchRecordParsedCell> row(String... texts) {
        List<MesProBatchRecordParsedCell> row = new ArrayList<>(texts.length);
        for (String text : texts) {
            row.add(MesProBatchRecordParsedCell.builder()
                    .text(text)
                    .widthPx(48)
                    .heightPx(24)
                    .fontSize(9)
                    .horizontalAlign("center")
                    .verticalAlign("middle")
                    .build());
        }
        return row;
    }

    private List<MesProBatchRecordParsedCell> sourceRow(MesProBatchRecordParsedCell... cells) {
        return List.of(cells);
    }

    private MesProBatchRecordParsedCell sourceCell(String text, int columnIndex, int colSpan) {
        return MesProBatchRecordParsedCell.builder()
                .text(text)
                .columnIndex(columnIndex)
                .colSpan(colSpan)
                .widthPx(Math.max(48, colSpan * 48))
                .heightPx(24)
                .fontSize(9)
                .horizontalAlign("center")
                .verticalAlign("middle")
                .build();
    }

    private List<MesProBatchRecordParsedCell> structuredRow(MesProBatchRecordParsedCell... cells) {
        return List.of(cells);
    }

    private MesProBatchRecordParsedCell structuredCell(String text, int rowSpan, int colSpan) {
        return structuredCell(text, rowSpan, colSpan, 24);
    }

    private MesProBatchRecordParsedCell structuredCell(String text, int rowSpan, int colSpan, int heightPx) {
        return MesProBatchRecordParsedCell.builder()
                .text(text)
                .rowSpan(rowSpan)
                .colSpan(colSpan)
                .widthPx(Math.max(48, colSpan * 48))
                .heightPx(heightPx)
                .fontSize(9)
                .horizontalAlign("center")
                .verticalAlign("middle")
                .build();
    }

    private MesProBatchRecordParsedCell cellByText(List<MesProBatchRecordParsedCell> row, String text) {
        return row.stream()
                .filter(cell -> text.equals(textOf(cell)))
                .findFirst()
                .orElseThrow();
    }

    private MesProBatchRecordParsedCell cellContainingText(List<MesProBatchRecordParsedCell> row, String text) {
        return row.stream()
                .filter(cell -> textOf(cell).contains(text))
                .findFirst()
                .orElseThrow();
    }

    private String textOf(MesProBatchRecordParsedCell cell) {
        return cell == null || cell.getText() == null ? "" : cell.getText().trim();
    }

    private String rowText(List<MesProBatchRecordParsedCell> row) {
        StringBuilder builder = new StringBuilder();
        for (MesProBatchRecordParsedCell cell : row) {
            builder.append(textOf(cell));
        }
        return builder.toString();
    }

    private String shapeOf(List<MesProBatchRecordParsedCell> row) {
        StringBuilder builder = new StringBuilder();
        for (MesProBatchRecordParsedCell cell : row) {
            builder.append("[")
                    .append(cell.getRowSpan())
                    .append("x")
                    .append(cell.getColSpan())
                    .append("@h")
                    .append(cell.getHeightPx())
                    .append("]")
                    .append(textOf(cell));
        }
        return builder.toString();
    }

    private boolean isPackedMaterialMatrixCell(MesProBatchRecordParsedCell cell) {
        if (cell == null || cell.getText() == null) {
            return false;
        }
        List<String> lines = cell.getText().lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
        if (lines.size() < 8) {
            return false;
        }
        long materialHeaderCount = lines.subList(0, Math.min(6, lines.size())).stream()
                .filter(line -> "物料编码".equals(line) || "物料名称".equals(line) || "批号".equals(line))
                .count();
        long slashCount = lines.stream().filter("/"::equals).count();
        return materialHeaderCount >= 6 && slashCount >= 2;
    }

    private boolean isExpandedMaterialMatrixHeaderRow(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.size() < 6) {
            return false;
        }
        long materialHeaderCount = row.stream()
                .map(this::textOf)
                .filter(text -> "物料编码".equals(text) || "物料名称".equals(text) || "批号".equals(text))
                .count();
        return materialHeaderCount >= 6;
    }

    private record OccupiedCell(MesProBatchRecordParsedCell cell, int untilRow) {
    }
}
