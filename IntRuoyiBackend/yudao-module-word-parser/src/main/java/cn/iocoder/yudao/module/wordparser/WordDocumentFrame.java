package cn.iocoder.yudao.module.wordparser;

import java.util.List;

public record WordDocumentFrame(List<List<WordCell>> headerRows, List<List<WordCell>> footerRows) {

    public WordDocumentFrame {
        headerRows = headerRows.stream().map(List::copyOf).toList();
        footerRows = footerRows.stream().map(List::copyOf).toList();
    }
}
