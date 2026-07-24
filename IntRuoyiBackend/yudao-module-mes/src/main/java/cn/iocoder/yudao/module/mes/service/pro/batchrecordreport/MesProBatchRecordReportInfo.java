package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import java.time.LocalDateTime;

public record MesProBatchRecordReportInfo(
        String reportId,
        String reportCode,
        String reportName,
        LocalDateTime updateTime
) {
}
