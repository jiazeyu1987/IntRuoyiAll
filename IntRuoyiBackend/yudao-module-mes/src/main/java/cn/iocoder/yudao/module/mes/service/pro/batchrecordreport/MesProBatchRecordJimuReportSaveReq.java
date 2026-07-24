package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import lombok.Builder;

@Builder
public record MesProBatchRecordJimuReportSaveReq(
        String existingReportId,
        String categoryId,
        String reportCode,
        String reportName,
        MesProBatchRecordParsedTable parsedTable
) {
}
