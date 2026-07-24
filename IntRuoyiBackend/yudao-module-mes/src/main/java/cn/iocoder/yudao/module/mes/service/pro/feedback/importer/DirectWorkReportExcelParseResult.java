package cn.iocoder.yudao.module.mes.service.pro.feedback.importer;

import java.util.List;

public record DirectWorkReportExcelParseResult(
        int sheetCount,
        int skippedRows,
        List<DirectWorkReportExcelRow> rows
) {
}
