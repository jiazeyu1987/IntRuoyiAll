package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import lombok.Builder;

@Builder
public record MesProBatchRecordImportRouteProductOption(
        String optionKey,
        Long routeProductId,
        Long routeId,
        String routeCode,
        String routeName,
        Long routeVersionId,
        String routeVersionNo,
        Long productId,
        String productCode,
        String productName,
        boolean existing
) {
}
