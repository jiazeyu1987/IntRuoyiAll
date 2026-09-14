package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record MesProBatchRecordReportView(
        String batchRecordName,
        Long batchRecordDefinitionId,
        Long batchRecordVersionId,
        String productName,
        String projectCode,
        Long dccProjectCodeId,
        String versionNo,
        String versionStatus,
        String formSlotType,
        String routeKey,
        Integer sourceTableIndex,
        String tableTitle,
        String reportId,
        String reportCode,
        String reportName,
        String sourceFileName,
        LocalDateTime lastImportTime,
        LocalDateTime updateTime
) {
}
