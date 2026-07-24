package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import lombok.Builder;

import java.util.List;

@Builder
public record MesProBatchRecordImportResult(
        int importedCount,
        int createdCount,
        int updatedCount,
        Long batchRecordDefinitionId,
        Long batchRecordVersionId,
        Long sourceBatchRecordVersionId,
        String versionNo,
        String versionStatus,
        String approvalInstanceId,
        Long routeId,
        String routeCode,
        String routeName,
        Long routeVersionId,
        String routeVersionNo,
        Integer routeProcessCount,
        Integer batchRecordRouteBindingCount,
        Integer boundProductNameCount,
        Integer boundProductCodeCount,
        List<String> skippedProductNames,
        List<MesProBatchRecordReportView> reports
) {
}
