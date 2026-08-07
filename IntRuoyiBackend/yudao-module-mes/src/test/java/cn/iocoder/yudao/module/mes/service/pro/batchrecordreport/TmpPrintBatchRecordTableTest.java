package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

class TmpPrintBatchRecordTableTest {

    @Test
    void printTable() throws Exception {
        Path source = BatchRecordReportTestFixtures.pressurePumpRecordDoc();
        byte[] bytes = Files.readAllBytes(source);
        MesProBatchRecordDocParser parser = TestBatchRecordFixtures.wordParser();
        List<MesProBatchRecordParsedTable> tables = parser.parse(bytes);
        MesProBatchRecordParsedTable sourceTable = tables.stream()
                .filter(table -> "组装Ⅰ工序生产记录".equals(table.getTableTitle()))
                .findFirst()
                .orElseThrow();
        MesProBatchRecordParsedTable table = new MesProBatchRecordReportLayoutCalibrator().calibrate(sourceTable);
        JSONObject json = JSON.parseObject(new MesProBatchRecordReportJsonBuilder().build(table, "EBR_TMP_T06"));
        StringBuilder builder = new StringBuilder();
        builder.append("sourceTableTitle=").append(sourceTable.getTableTitle()).append('\n');
        builder.append("sourceColumnCount=").append(sourceTable.getColumnCount()).append('\n');
        for (int rowIndex = 0; rowIndex < sourceTable.getRows().size(); rowIndex++) {
            builder.append("SOURCE ROW ").append(rowIndex + 1).append(':').append('\n');
            List<MesProBatchRecordParsedCell> row = sourceTable.getRows().get(rowIndex);
            for (int cellIndex = 0; cellIndex < row.size(); cellIndex++) {
                MesProBatchRecordParsedCell cell = row.get(cellIndex);
                builder.append("  CELL ").append(cellIndex + 1)
                        .append(" rs=").append(cell.getRowSpan())
                        .append(" cs=").append(cell.getColSpan())
                        .append(" w=").append(cell.getWidthPx())
                        .append(" h=").append(cell.getHeightPx())
                        .append(" text=").append(cell.getText().replace("\r", "\\r").replace("\n", "\\n"))
                        .append('\n');
            }
        }
        builder.append("tableTitle=").append(table.getTableTitle()).append('\n');
        builder.append("rowCount=").append(table.getRowCount()).append('\n');
        builder.append("columnCount=").append(table.getColumnCount()).append('\n');
        builder.append("columnWidths=").append(table.getColumnWidths()).append('\n');
        builder.append("jsonCols=").append(json.getJSONObject("cols").toJSONString()).append('\n');
        builder.append("dataRectWidth=").append(json.getIntValue("dataRectWidth")).append('\n');
        for (int rowIndex = 0; rowIndex < table.getRows().size(); rowIndex++) {
            builder.append("ROW ").append(rowIndex + 1).append(':').append('\n');
            List<MesProBatchRecordParsedCell> row = table.getRows().get(rowIndex);
            for (int cellIndex = 0; cellIndex < row.size(); cellIndex++) {
                MesProBatchRecordParsedCell cell = row.get(cellIndex);
                builder.append("  CELL ").append(cellIndex + 1)
                        .append(" rs=").append(cell.getRowSpan())
                        .append(" cs=").append(cell.getColSpan())
                        .append(" w=").append(cell.getWidthPx())
                        .append(" h=").append(cell.getHeightPx())
                        .append(" text=").append(cell.getText().replace("\r", "\\r").replace("\n", "\\n"))
                        .append('\n');
            }
        }
        builder.append("JSON OPERATION ROWS").append('\n');
        JSONObject rows = json.getJSONObject("rows");
        for (int rowIndex = 12; rowIndex <= 18; rowIndex++) {
            JSONObject rowObject = rows.getJSONObject(String.valueOf(rowIndex));
            if (rowObject == null) {
                continue;
            }
            builder.append("JSON ROW ").append(rowIndex + 1)
                    .append(" height=").append(rowObject.getIntValue("height"))
                    .append(':').append('\n');
            JSONObject cells = rowObject.getJSONObject("cells");
            for (String key : cells.keySet().stream().filter(k -> !"len".equals(k)).sorted().toList()) {
                JSONObject cellObject = cells.getJSONObject(key);
                builder.append("  COL ").append(key)
                        .append(" text=").append(String.valueOf(cellObject.get("text")).replace("\r", "\\r").replace("\n", "\\n"))
                        .append(" merge=").append(cellObject.get("merge"))
                        .append(" fill=").append(cellObject.containsKey("fillForm"))
                        .append('\n');
            }
        }
        System.out.write(builder.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void printRouteBAssemblyOne() throws Exception {
        Path source = BatchRecordReportTestFixtures.pressurePumpRecordDoc();
        byte[] bytes = Files.readAllBytes(source);
        MesProBatchRecordRouteBRecognizer recognizer = TestBatchRecordFixtures.routeBRecognizer();
        List<MesProBatchRecordParsedTable> tables = recognizer.recognize(
                source,
                bytes,
                source.getFileName().toString());
        MesProBatchRecordParsedTable sourceTable = tables.stream()
                .filter(table -> "组装Ⅰ工序生产记录".equals(table.getTableTitle()))
                .findFirst()
                .orElseThrow();
        MesProBatchRecordParsedTable table = new MesProBatchRecordReportLayoutCalibrator().calibrate(sourceTable);
        JSONObject json = JSON.parseObject(new MesProBatchRecordReportJsonBuilder().build(table, "EBR_TMP_B_T06"));
        StringBuilder builder = new StringBuilder();
        builder.append("route=B\n");
        builder.append("sourceTableTitle=").append(sourceTable.getTableTitle()).append('\n');
        builder.append("sourceColumnCount=").append(sourceTable.getColumnCount()).append('\n');
        for (int rowIndex = 0; rowIndex < sourceTable.getRows().size(); rowIndex++) {
            builder.append("SOURCE ROW ").append(rowIndex + 1).append(':').append('\n');
            List<MesProBatchRecordParsedCell> row = sourceTable.getRows().get(rowIndex);
            for (int cellIndex = 0; cellIndex < row.size(); cellIndex++) {
                MesProBatchRecordParsedCell cell = row.get(cellIndex);
                builder.append("  CELL ").append(cellIndex + 1)
                        .append(" rs=").append(cell.getRowSpan())
                        .append(" cs=").append(cell.getColSpan())
                        .append(" w=").append(cell.getWidthPx())
                        .append(" h=").append(cell.getHeightPx())
                        .append(" text=").append(cell.getText().replace("\r", "\\r").replace("\n", "\\n"))
                        .append('\n');
            }
        }
        builder.append("tableTitle=").append(table.getTableTitle()).append('\n');
        builder.append("rowCount=").append(table.getRowCount()).append('\n');
        builder.append("columnCount=").append(table.getColumnCount()).append('\n');
        builder.append("columnWidths=").append(table.getColumnWidths()).append('\n');
        builder.append("jsonCols=").append(json.getJSONObject("cols").toJSONString()).append('\n');
        builder.append("dataRectWidth=").append(json.getIntValue("dataRectWidth")).append('\n');
        for (int rowIndex = 0; rowIndex < table.getRows().size(); rowIndex++) {
            builder.append("ROW ").append(rowIndex + 1).append(':').append('\n');
            List<MesProBatchRecordParsedCell> row = table.getRows().get(rowIndex);
            for (int cellIndex = 0; cellIndex < row.size(); cellIndex++) {
                MesProBatchRecordParsedCell cell = row.get(cellIndex);
                builder.append("  CELL ").append(cellIndex + 1)
                        .append(" rs=").append(cell.getRowSpan())
                        .append(" cs=").append(cell.getColSpan())
                        .append(" w=").append(cell.getWidthPx())
                        .append(" h=").append(cell.getHeightPx())
                        .append(" text=").append(cell.getText().replace("\r", "\\r").replace("\n", "\\n"))
                        .append('\n');
            }
        }
        System.out.write(builder.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void printSyntheticOperationBandCalibration() throws Exception {
        MesProBatchRecordReportLayoutCalibrator calibrator = new MesProBatchRecordReportLayoutCalibrator();
        MesProBatchRecordParsedTable parsedTable = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(6)
                .tableTitle("组装Ⅰ工序生产记录")
                .rowCount(8)
                .columnCount(21)
                .rows(List.of(
                        row(cell("组装Ⅰ工序生产记录", 1, 21, 24)),
                        row(
                                cell("组装Ⅰ生产操作及自检记录", 1, 1, 84),
                                cell("物料编码", 1, 5, 24),
                                cell("物料名称", 1, 5, 24),
                                cell("批号", 1, 1, 24),
                                cell("物料编码", 1, 4, 24),
                                cell("物料名称", 1, 4, 24),
                                cell("批号", 1, 1, 24)
                        ),
                        row(
                                cell("设备编码", 1, 4, 24),
                                cell("撤压机：C01017", 1, 6, 24),
                                cell("是否在计量效期内", 1, 4, 24),
                                cell("□是  □否", 1, 3, 24)
                        ),
                        row(
                                cell("操作日期", 1, 1, 92),
                                cell("生产自检", 1, 2, 92),
                                cell("合格标准：\n1、撤压检测", 1, 13, 92),
                                cell("生产数量/pcs", 1, 1, 92),
                                cell("自检合格数量/pcs", 1, 1, 92),
                                cell("不合格数量/pcs", 1, 1, 92),
                                cell("操作人", 1, 1, 92),
                                cell("复核人", 1, 1, 92)
                        ),
                        row(
                                cell("", 1, 1, 28), cell("", 1, 2, 28), cell("", 1, 13, 28),
                                cell("", 1, 1, 28), cell("", 1, 1, 28), cell("", 1, 1, 28),
                                cell("", 1, 1, 28), cell("", 1, 1, 28)
                        ),
                        row(
                                cell("", 1, 1, 28), cell("", 1, 2, 28), cell("", 1, 13, 28),
                                cell("", 1, 1, 28), cell("", 1, 1, 28), cell("", 1, 1, 28),
                                cell("", 1, 1, 28), cell("", 1, 1, 28)
                        ),
                        row(
                                cell("生产批量汇总", 1, 15, 24),
                                cell("", 1, 1, 24), cell("", 1, 1, 24), cell("", 1, 1, 24),
                                cell("", 1, 1, 24), cell("", 1, 1, 24), cell("", 1, 1, 24)
                        )
                ))
                .build();
        MesProBatchRecordParsedTable table = calibrator.calibrate(parsedTable);
        StringBuilder builder = new StringBuilder();
        builder.append("synthetic route calibration\n");
        builder.append("columnWidths=").append(table.getColumnWidths()).append('\n');
        for (int rowIndex = 0; rowIndex < table.getRows().size(); rowIndex++) {
            builder.append("ROW ").append(rowIndex + 1).append(": ")
                    .append(table.getRows().get(rowIndex).stream()
                            .map(cell -> "[" + cell.getRowSpan() + "x" + cell.getColSpan() + "]"
                                    + cell.getText().replace("\n", "\\n"))
                            .collect(Collectors.joining(" | ")))
                    .append('\n');
        }
        System.out.write(builder.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void printRouteBFailingTableContext() throws Exception {
        Path source = BatchRecordReportTestFixtures.pressurePumpRecordDoc();
        byte[] bytes = Files.readAllBytes(source);
        MesProBatchRecordRouteBRecognizer recognizer = TestBatchRecordFixtures.routeBRecognizer();
        MesProBatchRecordReportLayoutCalibrator calibrator = new MesProBatchRecordReportLayoutCalibrator();
        List<MesProBatchRecordParsedTable> tables = recognizer.recognize(source, bytes, source.getFileName().toString());
        StringBuilder builder = new StringBuilder();
        for (MesProBatchRecordParsedTable table : tables) {
            builder.append("TABLE ").append(table.getSourceTableIndex())
                    .append(" title=").append(table.getTableTitle())
                    .append(" columnCount=").append(table.getColumnCount())
                    .append('\n');
            try {
                MesProBatchRecordParsedTable calibrated = calibrator.calibrate(table);
                builder.append("  CALIBRATED rowCount=").append(calibrated.getRowCount())
                        .append(" columnCount=").append(calibrated.getColumnCount())
                        .append('\n');
            } catch (Exception ex) {
                builder.append("  FAILED: ").append(ex.getMessage()).append('\n');
                int start = Math.max(0, extractRowIndex(ex.getMessage()) - 3);
                int end = Math.min(table.getRows().size() - 1, extractRowIndex(ex.getMessage()) + 3);
                for (int rowIndex = start; rowIndex <= end; rowIndex++) {
                    builder.append("  SOURCE ROW ").append(rowIndex).append(": ")
                            .append(table.getRows().get(rowIndex).stream()
                                    .map(cell -> "[" + cell.getRowSpan() + "x" + cell.getColSpan() + "]"
                                            + cell.getText().replace("\r", "\\r").replace("\n", "\\n"))
                                    .collect(Collectors.joining(" | ")))
                            .append('\n');
                }
                break;
            }
        }
        System.out.write(builder.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static List<MesProBatchRecordParsedCell> row(MesProBatchRecordParsedCell... cells) {
        return List.of(cells);
    }

    private static MesProBatchRecordParsedCell cell(String text, int rowSpan, int colSpan, int heightPx) {
        return MesProBatchRecordParsedCell.builder()
                .text(text)
                .rowSpan(rowSpan)
                .colSpan(colSpan)
                .widthPx(120)
                .heightPx(heightPx)
                .fontSize(10)
                .horizontalAlign("center")
                .verticalAlign("middle")
                .build();
    }

    private static int extractRowIndex(String message) {
        if (message == null) {
            return 0;
        }
        int marker = message.indexOf("rowIndex=");
        if (marker < 0) {
            return 0;
        }
        int start = marker + "rowIndex=".length();
        int end = message.indexOf(' ', start);
        if (end < 0) {
            end = message.length();
        }
        return Integer.parseInt(message.substring(start, end));
    }
}
