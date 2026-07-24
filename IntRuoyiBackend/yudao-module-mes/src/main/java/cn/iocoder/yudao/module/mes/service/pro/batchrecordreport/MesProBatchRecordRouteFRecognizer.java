package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

@Component
@RequiredArgsConstructor
public class MesProBatchRecordRouteFRecognizer implements MesProBatchRecordRouteRecognizer {

    public static final String ROUTE_KEY = MesProBatchRecordRecognitionRouteKeys.F;

    private static final int DEFAULT_WIDTH_PX = 120;
    private static final int DEFAULT_HEIGHT_PX = 36;
    private static final float IMPLICIT_MERGE_WIDTH_THRESHOLD = 0.85f;

    private final MesProBatchRecordDocParser docParser;

    @Override
    public String routeKey() {
        return ROUTE_KEY;
    }

    @Override
    public List<MesProBatchRecordParsedTable> recognize(Path sourcePath, byte[] sourceBytes, String originalFileName) {
        return recognize(sourceBytes);
    }

    public List<MesProBatchRecordParsedTable> recognize(byte[] sourceBytes) {
        if (sourceBytes == null || sourceBytes.length == 0) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                    "route_f_word_bytes_empty");
        }
        List<MesProBatchRecordParsedTable> sourceTables = docParser.parse(sourceBytes);
        if (sourceTables.isEmpty()) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                    "route_f_word_tables_empty");
        }
        byte[] excelBytes = buildExcelIntermediate(sourceTables);
        if (excelBytes == null || excelBytes.length == 0) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                    "route_f_excel_intermediate_empty");
        }
        List<MesProBatchRecordParsedTable> recognizedTables = parseExcelIntermediate(excelBytes);
        if (recognizedTables.isEmpty()) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                    "route_f_excel_intermediate_invalid");
        }
        if (recognizedTables.size() != sourceTables.size()) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                    "route_f_expected_recognized_tables_" + sourceTables.size()
                            + "_actual_" + recognizedTables.size());
        }
        return recognizedTables;
    }

    protected byte[] buildExcelIntermediate(List<MesProBatchRecordParsedTable> sourceTables) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Map<ExcelStyleKey, CellStyle> styleCache = new HashMap<>();
            Set<String> usedSheetNames = new LinkedHashSet<>();
            for (int tableIndex = 0; tableIndex < sourceTables.size(); tableIndex++) {
                MesProBatchRecordParsedTable table = sourceTables.get(tableIndex);
                Sheet sheet = workbook.createSheet(uniqueSheetName(usedSheetNames, table, tableIndex));
                writeTableToSheet(workbook, sheet, table, styleCache);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                    "route_f_excel_intermediate_write_failed:" + ex.getMessage());
        }
    }

    protected List<MesProBatchRecordParsedTable> parseExcelIntermediate(byte[] excelBytes) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(excelBytes))) {
            if (workbook.getNumberOfSheets() <= 0) {
                throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                        "route_f_excel_intermediate_invalid");
            }
            List<MesProBatchRecordParsedTable> parsedTables = new ArrayList<>();
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                parsedTables.add(parseSheet(workbook, workbook.getSheetAt(sheetIndex), sheetIndex + 1));
            }
            return parsedTables;
        } catch (IOException ex) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                    "route_f_excel_intermediate_read_failed:" + ex.getMessage());
        }
    }

    private void writeTableToSheet(Workbook workbook, Sheet sheet, MesProBatchRecordParsedTable table,
                                   Map<ExcelStyleKey, CellStyle> styleCache) {
        for (int rowIndex = 0; rowIndex < table.getRows().size(); rowIndex++) {
            Row row = sheet.createRow(rowIndex);
            row.setHeightInPoints(pixelsToPoints(resolveRowHeight(table.getRows().get(rowIndex))));
        }
        for (PlacedCell placedCell : placeCells(table)) {
            MesProBatchRecordParsedCell sourceCell = placedCell.cell();
            Row row = sheet.getRow(placedCell.rowIndex());
            Cell cell = row.createCell(placedCell.columnIndex(), CellType.STRING);
            cell.setCellValue(Objects.toString(sourceCell.getText(), ""));
            cell.setCellStyle(resolveStyle(workbook, sourceCell, styleCache));
            int colSpan = placedCell.colSpan();
            int rowSpan = placedCell.rowSpan();
            for (int spanOffset = 0; spanOffset < colSpan; spanOffset++) {
                int widthPx = Math.max(32, sourceCell.getWidthPx() / colSpan);
                int columnWidth = Math.max(sheet.getColumnWidth(placedCell.columnIndex() + spanOffset),
                        pixelsToExcelWidth(widthPx));
                sheet.setColumnWidth(placedCell.columnIndex() + spanOffset, Math.min(columnWidth, 255 * 256));
            }
            if (rowSpan > 1 || colSpan > 1) {
                sheet.addMergedRegion(new CellRangeAddress(
                        placedCell.rowIndex(),
                        placedCell.rowIndex() + rowSpan - 1,
                        placedCell.columnIndex(),
                        placedCell.columnIndex() + colSpan - 1));
            }
        }
    }

    private List<PlacedCell> placeCells(MesProBatchRecordParsedTable table) {
        List<PlacedCell> placedCells = new ArrayList<>();
        Map<Integer, Integer> blockedUntilRowByColumn = new HashMap<>();
        int tableWidthPx = resolveTableWidth(table);
        int tableColumnCount = Math.max(1, table.getColumnCount() == null ? 0 : table.getColumnCount());
        for (int rowIndex = 0; rowIndex < table.getRows().size(); rowIndex++) {
            int columnIndex = 0;
            int consumedWidthPx = 0;
            List<MesProBatchRecordParsedCell> rowCells = table.getRows().get(rowIndex);
            for (int cellIndex = 0; cellIndex < rowCells.size(); cellIndex++) {
                MesProBatchRecordParsedCell cell = rowCells.get(cellIndex);
                while (blockedUntilRowByColumn.getOrDefault(columnIndex, -1) >= rowIndex) {
                    columnIndex++;
                }
                int rowSpan = Math.max(1, cell.getRowSpan());
                int colSpan = resolveEffectiveColSpan(rowCells, cellIndex, cell, columnIndex,
                        consumedWidthPx, tableWidthPx, tableColumnCount);
                placedCells.add(new PlacedCell(rowIndex, columnIndex, rowSpan, colSpan, cell));
                if (rowSpan > 1) {
                    for (int offset = 0; offset < colSpan; offset++) {
                        blockedUntilRowByColumn.put(columnIndex + offset, rowIndex + rowSpan - 1);
                    }
                }
                columnIndex += colSpan;
                consumedWidthPx += Math.max(0, cell.getWidthPx());
            }
        }
        return placedCells;
    }

    private int resolveEffectiveColSpan(List<MesProBatchRecordParsedCell> rowCells, int cellIndex,
                                        MesProBatchRecordParsedCell cell, int columnIndex, int consumedWidthPx,
                                        int tableWidthPx, int tableColumnCount) {
        int explicitColSpan = Math.max(1, cell.getColSpan());
        if (explicitColSpan > 1) {
            return explicitColSpan;
        }
        int remainingColumns = Math.max(1, tableColumnCount - columnIndex);
        if (remainingColumns <= 1
                || cellIndex != rowCells.size() - 1
                || cell.getText() == null
                || cell.getText().isBlank()) {
            return explicitColSpan;
        }
        int remainingWidthPx = Math.max(cell.getWidthPx(), tableWidthPx - consumedWidthPx);
        if (cell.getWidthPx() >= Math.round(remainingWidthPx * IMPLICIT_MERGE_WIDTH_THRESHOLD)) {
            return remainingColumns;
        }
        return explicitColSpan;
    }

    private CellStyle resolveStyle(Workbook workbook, MesProBatchRecordParsedCell sourceCell,
                                   Map<ExcelStyleKey, CellStyle> styleCache) {
        ExcelStyleKey key = new ExcelStyleKey(
                sourceCell.isBold(),
                (short) Math.max(8, sourceCell.getFontSize()),
                toHorizontalAlignment(sourceCell.getHorizontalAlign()),
                toVerticalAlignment(sourceCell.getVerticalAlign()));
        return styleCache.computeIfAbsent(key, ignored -> {
            CellStyle style = workbook.createCellStyle();
            style.setWrapText(true);
            style.setAlignment(key.horizontalAlignment());
            style.setVerticalAlignment(key.verticalAlignment());
            Font font = workbook.createFont();
            font.setBold(key.bold());
            font.setFontHeightInPoints(key.fontSizePt());
            style.setFont(font);
            return style;
        });
    }

    private MesProBatchRecordParsedTable parseSheet(Workbook workbook, Sheet sheet, int sourceTableIndex) {
        MergedRegionLookup mergedRegionLookup = MergedRegionLookup.from(sheet);
        List<List<MesProBatchRecordParsedCell>> rows = new ArrayList<>();
        int maxColumnCount = 0;
        int lastRowNum = Math.max(sheet.getLastRowNum(), 0);
        for (int rowIndex = 0; rowIndex <= lastRowNum; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            List<MesProBatchRecordParsedCell> parsedRow = parseRow(workbook, sheet, row, rowIndex, mergedRegionLookup);
            rows.add(parsedRow);
            int rowColumnCount = parsedRow.stream().mapToInt(MesProBatchRecordParsedCell::getColSpan).sum();
            maxColumnCount = Math.max(maxColumnCount, rowColumnCount);
        }
        if (rows.stream().allMatch(List::isEmpty)) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                    "route_f_excel_intermediate_invalid");
        }
        return MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(sourceTableIndex)
                .tableTitle(resolveSheetTitle(rows, sheet.getSheetName()))
                .rowCount(rows.size())
                .columnCount(maxColumnCount)
                .rows(rows)
                .build();
    }

    private List<MesProBatchRecordParsedCell> parseRow(Workbook workbook, Sheet sheet, Row row, int rowIndex,
                                                       MergedRegionLookup mergedRegionLookup) {
        List<MesProBatchRecordParsedCell> parsedCells = new ArrayList<>();
        int rowMaxColumn = resolveRowMaxColumn(row, rowIndex, mergedRegionLookup);
        int rowHeightPx = row == null ? DEFAULT_HEIGHT_PX : pointsToPixels(row.getHeightInPoints());
        DataFormatter formatter = new DataFormatter();
        for (int columnIndex = 0; columnIndex < rowMaxColumn; ) {
            String positionKey = cellKey(rowIndex, columnIndex);
            if (mergedRegionLookup.followerKeys().contains(positionKey)) {
                columnIndex++;
                continue;
            }
            CellRangeAddress mergedRegion = mergedRegionLookup.topLeftRegions().get(positionKey);
            Cell cell = row == null ? null : row.getCell(columnIndex);
            if (cell == null && mergedRegion == null) {
                columnIndex++;
                continue;
            }
            int rowSpan = mergedRegion == null ? 1 : mergedRegion.getLastRow() - mergedRegion.getFirstRow() + 1;
            int colSpan = mergedRegion == null ? 1 : mergedRegion.getLastColumn() - mergedRegion.getFirstColumn() + 1;
            CellStyle style = cell == null ? null : cell.getCellStyle();
            Font font = style == null ? null : workbook.getFontAt(style.getFontIndex());
            parsedCells.add(MesProBatchRecordParsedCell.builder()
                    .text(cell == null ? "" : formatter.formatCellValue(cell).trim())
                    .rowSpan(rowSpan)
                    .colSpan(colSpan)
                    .bold(font != null && font.getBold())
                    .fontSize(font == null ? 10 : Math.max(8, font.getFontHeightInPoints()))
                    .horizontalAlign(fromHorizontalAlignment(style == null ? null : style.getAlignment()))
                    .verticalAlign(fromVerticalAlignment(style == null ? null : style.getVerticalAlignment()))
                    .widthPx(resolveMergedWidthPx(sheet, columnIndex, colSpan))
                    .heightPx(rowHeightPx)
                    .build());
            columnIndex += Math.max(1, colSpan);
        }
        return parsedCells;
    }

    private int resolveRowMaxColumn(Row row, int rowIndex, MergedRegionLookup mergedRegionLookup) {
        int rowMaxColumn = row == null || row.getLastCellNum() < 0 ? 0 : row.getLastCellNum();
        for (CellRangeAddress region : mergedRegionLookup.regionsByRow().getOrDefault(rowIndex, List.of())) {
            rowMaxColumn = Math.max(rowMaxColumn, region.getLastColumn() + 1);
        }
        return rowMaxColumn;
    }

    private int resolveMergedWidthPx(Sheet sheet, int startColumnIndex, int colSpan) {
        int widthPx = 0;
        for (int offset = 0; offset < Math.max(1, colSpan); offset++) {
            widthPx += excelWidthToPixels(sheet.getColumnWidth(startColumnIndex + offset));
        }
        return widthPx <= 0 ? DEFAULT_WIDTH_PX : widthPx;
    }

    private String resolveSheetTitle(List<List<MesProBatchRecordParsedCell>> rows, String fallback) {
        for (List<MesProBatchRecordParsedCell> row : rows) {
            String rowText = firstNonBlankRowText(row);
            if (!rowText.isBlank()) {
                return extractTemplateTitle(rowText);
            }
        }
        return extractTemplateTitle(fallback);
    }

    private String firstNonBlankRowText(List<MesProBatchRecordParsedCell> row) {
        for (MesProBatchRecordParsedCell cell : row) {
            if (cell.getText() != null && !cell.getText().isBlank()) {
                return cell.getText();
            }
        }
        return "";
    }

    private String extractTemplateTitle(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replace('\r', '\n').trim();
        if (normalized.isBlank()) {
            return "";
        }
        String firstLine = normalized.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .findFirst()
                .orElse(normalized);
        return firstLine.replaceAll("\\s+", " ").trim();
    }

    private String uniqueSheetName(Set<String> usedSheetNames, MesProBatchRecordParsedTable table, int tableIndex) {
        String baseName = sanitizeSheetName(extractTemplateTitle(table.getTableTitle()));
        if (baseName.isBlank()) {
            baseName = "RouteF-" + (tableIndex + 1);
        }
        String candidate = baseName;
        int suffix = 2;
        while (!usedSheetNames.add(candidate)) {
            String suffixText = "-" + suffix++;
            int maxBaseLength = Math.max(1, 31 - suffixText.length());
            candidate = baseName.substring(0, Math.min(baseName.length(), maxBaseLength)) + suffixText;
        }
        return candidate;
    }

    private String sanitizeSheetName(String raw) {
        String value = raw == null ? "" : raw.replaceAll("[\\\\/*?:\\[\\]]", " ").replaceAll("\\s+", " ").trim();
        if (value.isBlank()) {
            return "";
        }
        return value.substring(0, Math.min(value.length(), 31));
    }

    private int resolveRowHeight(List<MesProBatchRecordParsedCell> row) {
        return row.stream()
                .mapToInt(MesProBatchRecordParsedCell::getHeightPx)
                .filter(height -> height > 0)
                .max()
                .orElse(DEFAULT_HEIGHT_PX);
    }

    private int resolveTableWidth(MesProBatchRecordParsedTable table) {
        if (table == null || table.getRows() == null) {
            return DEFAULT_WIDTH_PX;
        }
        return table.getRows().stream()
                .mapToInt(row -> row.stream()
                        .mapToInt(cell -> Math.max(0, cell.getWidthPx()))
                        .sum())
                .max()
                .orElse(DEFAULT_WIDTH_PX);
    }

    private HorizontalAlignment toHorizontalAlignment(String align) {
        if ("center".equalsIgnoreCase(align)) {
            return HorizontalAlignment.CENTER;
        }
        if ("right".equalsIgnoreCase(align)) {
            return HorizontalAlignment.RIGHT;
        }
        return HorizontalAlignment.LEFT;
    }

    private VerticalAlignment toVerticalAlignment(String align) {
        if ("top".equalsIgnoreCase(align)) {
            return VerticalAlignment.TOP;
        }
        if ("bottom".equalsIgnoreCase(align)) {
            return VerticalAlignment.BOTTOM;
        }
        return VerticalAlignment.CENTER;
    }

    private String fromHorizontalAlignment(HorizontalAlignment align) {
        if (align == HorizontalAlignment.CENTER || align == HorizontalAlignment.CENTER_SELECTION) {
            return "center";
        }
        if (align == HorizontalAlignment.RIGHT) {
            return "right";
        }
        return "left";
    }

    private String fromVerticalAlignment(VerticalAlignment align) {
        if (align == VerticalAlignment.TOP) {
            return "top";
        }
        if (align == VerticalAlignment.BOTTOM) {
            return "bottom";
        }
        return "middle";
    }

    private int pixelsToExcelWidth(int widthPx) {
        return Math.max(256, Math.round((float) widthPx * 35));
    }

    private int excelWidthToPixels(int excelWidth) {
        return Math.max(16, Math.round(excelWidth / 35.0f));
    }

    private float pixelsToPoints(int pixels) {
        return Math.max(12, pixels * 0.75f);
    }

    private int pointsToPixels(float points) {
        return Math.max(12, Math.round(points / 0.75f));
    }

    private static String cellKey(int rowIndex, int columnIndex) {
        return rowIndex + ":" + columnIndex;
    }

    private record ExcelStyleKey(boolean bold, short fontSizePt,
                                 HorizontalAlignment horizontalAlignment,
                                 VerticalAlignment verticalAlignment) {
    }

    private record PlacedCell(int rowIndex, int columnIndex, int rowSpan, int colSpan,
                              MesProBatchRecordParsedCell cell) {
    }

    private record MergedRegionLookup(Map<String, CellRangeAddress> topLeftRegions,
                                      Set<String> followerKeys,
                                      Map<Integer, List<CellRangeAddress>> regionsByRow) {

        private static MergedRegionLookup from(Sheet sheet) {
            Map<String, CellRangeAddress> topLeftRegions = new LinkedHashMap<>();
            Set<String> followerKeys = new HashSet<>();
            Map<Integer, List<CellRangeAddress>> regionsByRow = new HashMap<>();
            for (int regionIndex = 0; regionIndex < sheet.getNumMergedRegions(); regionIndex++) {
                CellRangeAddress region = sheet.getMergedRegion(regionIndex);
                topLeftRegions.put(cellKey(region.getFirstRow(), region.getFirstColumn()), region);
                for (int rowIndex = region.getFirstRow(); rowIndex <= region.getLastRow(); rowIndex++) {
                    regionsByRow.computeIfAbsent(rowIndex, ignored -> new ArrayList<>()).add(region);
                    for (int columnIndex = region.getFirstColumn(); columnIndex <= region.getLastColumn(); columnIndex++) {
                        if (rowIndex == region.getFirstRow() && columnIndex == region.getFirstColumn()) {
                            continue;
                        }
                        followerKeys.add(cellKey(rowIndex, columnIndex));
                    }
                }
            }
            return new MergedRegionLookup(topLeftRegions, followerKeys, regionsByRow);
        }
    }
}
