package cn.iocoder.yudao.module.wordparser;

import java.util.List;

public record WordTable(
        int sourceTopLevelTableIndex,
        int rowCount,
        int columnCount,
        List<Integer> columnWidths,
        List<List<WordCell>> rows) {

    public WordTable {
        columnWidths = List.copyOf(columnWidths);
        rows = rows.stream().map(List::copyOf).toList();
    }
}
