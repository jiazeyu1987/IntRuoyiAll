package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import lombok.Builder;

import java.util.List;

@Builder
public record MesProBatchRecordImportPreflightResult(
        String routeKey,
        String batchRecordName,
        Long batchRecordDefinitionId,
        Long currentBatchRecordVersionId,
        String currentBatchRecordVersionNo,
        String currentBatchRecordVersionStatus,
        Long latestBatchRecordVersionId,
        String latestBatchRecordVersionNo,
        String latestBatchRecordVersionStatus,
        Boolean currentBatchRecordHasMainReports,
        String routeGovernanceStatus,
        Boolean routeUpgradeRequired,
        List<DuplicateRoute> duplicateRoutes,
        Long currentRouteId,
        String currentRouteCode,
        String currentRouteName,
        Long currentRouteVersionId,
        String currentRouteVersionNo,
        Boolean currentRouteVersionActive,
        Boolean hasHistoricalReferences,
        List<ReferenceBlocker> referenceBlockers,
        List<String> allowedActions,
        String recommendedAction,
        String nextVersionNo,
        List<MesProBatchRecordImportRouteProductOption> routeProductOptions
) {

    @Builder
    public record ReferenceBlocker(
            String versionNo,
            String referenceName,
            Long count,
            String cleanupEntrance,
            String cleanupAction
    ) {
    }

    @Builder
    public record DuplicateRoute(
            Long routeId,
            String routeCode,
            String routeName,
            Long routeVersionId,
            String routeVersionNo
    ) {
    }
}
