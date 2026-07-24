package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import lombok.Builder;

@Builder
public record MesProBatchRecordRouteGenerationResult(
        Long routeId,
        String routeCode,
        String routeName,
        Long routeVersionId,
        String routeVersionNo,
        Integer routeProcessCount,
        Integer batchRecordRouteBindingCount,
        Integer boundProductNameCount,
        Integer boundProductCodeCount,
        java.util.List<String> skippedProductNames
) {
}
