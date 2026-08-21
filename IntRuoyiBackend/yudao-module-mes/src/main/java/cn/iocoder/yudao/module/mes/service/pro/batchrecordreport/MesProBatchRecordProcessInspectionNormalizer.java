package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Keeps process-inspection Word tables on the source-structure recognition path.
 */
@Component
public class MesProBatchRecordProcessInspectionNormalizer implements MesProBatchRecordFormProfile {

    private static final List<String> PROCESS_INSPECTION_HEADERS = List.of(
            "检验项目",
            "检测结果",
            "检验设备",
            "判定");

    @Override
    public String formSlotType() {
        return MesProBatchRecordFormSlotType.PROCESS_INSPECTION.getType();
    }

    @Override
    public int priority() {
        return 20;
    }

    @Override
    public boolean supportsSourceTable(MesProBatchRecordParsedTable table) {
        if (table == null || table.getRows() == null || table.getRows().isEmpty()) {
            return false;
        }
        String title = normalize(table.getTableTitle());
        String tableText = table.getRows().stream()
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .map(MesProBatchRecordParsedCell::getText)
                .map(this::normalize)
                .filter(text -> !text.isBlank())
                .reduce(title, (left, right) -> left + "\n" + right);
        long headerCount = PROCESS_INSPECTION_HEADERS.stream()
                .filter(tableText::contains)
                .count();
        return headerCount >= PROCESS_INSPECTION_HEADERS.size()
                && (tableText.contains("过程检验") || tableText.contains("检验记录"));
    }

    @Override
    public boolean supportsSourceTables(List<MesProBatchRecordParsedTable> sourceTables) {
        return !supportedSourceTables(sourceTables).isEmpty();
    }

    @Override
    public List<MesProBatchRecordParsedTable> normalizeSourceTables(List<MesProBatchRecordParsedTable> sourceTables) {
        List<MesProBatchRecordParsedTable> supportedTables = supportedSourceTables(sourceTables);
        if (supportedTables.isEmpty()) {
            return List.of();
        }
        List<MesProBatchRecordParsedTable> normalizedTables = new ArrayList<>();
        for (int index = 0; index < supportedTables.size(); index++) {
            MesProBatchRecordParsedTable sourceTable = supportedTables.get(index);
            int templateIndex = sourceTable.getSourceTableIndex() == null ? index + 1 : sourceTable.getSourceTableIndex();
            normalizedTables.add(normalizeSourceTable(templateIndex, sourceTable));
        }
        return normalizedTables;
    }

    @Override
    public MesProBatchRecordParsedTable normalizeSourceTable(int templateIndex,
                                                              MesProBatchRecordParsedTable sourceTable) {
        List<List<MesProBatchRecordParsedCell>> rows = sourceTable.getRows() == null
                ? List.of()
                : sourceTable.getRows();
        return MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(templateIndex)
                .sourceTopLevelTableIndex(sourceTable.getSourceTopLevelTableIndex())
                .sourceSplitIndex(sourceTable.getSourceSplitIndex())
                .tableTitle(sourceTable.getTableTitle())
                .rowCount(rows.size())
                .columnCount(resolveColumnCount(sourceTable, rows))
                .columnWidths(sourceTable.getColumnWidths())
                .preserveSourceGrid(Boolean.TRUE)
                .routeBSource(sourceTable.getRouteBSource())
                .documentFrame(sourceTable.getDocumentFrame())
                .rows(rows)
                .build();
    }

    private int resolveColumnCount(MesProBatchRecordParsedTable sourceTable,
                                   List<List<MesProBatchRecordParsedCell>> rows) {
        if (sourceTable.getColumnCount() != null && sourceTable.getColumnCount() > 0) {
            return sourceTable.getColumnCount();
        }
        return rows.stream()
                .filter(Objects::nonNull)
                .mapToInt(row -> row.stream()
                        .filter(Objects::nonNull)
                        .mapToInt(cell -> Math.max(1, cell.getColSpan()))
                        .sum())
                .max()
                .orElse(1);
    }

    private String normalize(String text) {
        return Objects.toString(text, "")
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replaceAll("\\s+", "")
                .trim();
    }

    private List<MesProBatchRecordParsedTable> supportedSourceTables(List<MesProBatchRecordParsedTable> sourceTables) {
        if (sourceTables == null || sourceTables.isEmpty()) {
            return List.of();
        }
        return sourceTables.stream()
                .filter(this::supportsSourceTable)
                .toList();
    }
}
