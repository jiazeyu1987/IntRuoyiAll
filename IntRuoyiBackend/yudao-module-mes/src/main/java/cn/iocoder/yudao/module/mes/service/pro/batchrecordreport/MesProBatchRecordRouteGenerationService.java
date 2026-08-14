package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import java.util.List;

public interface MesProBatchRecordRouteGenerationService {

    void validateUploadedWordRoute(List<MesProBatchRecordParsedTable> parsedTables);

    MesProBatchRecordRouteGenerationResult generateForUploadedWord(String batchRecordName,
                                                                   List<MesProBatchRecordParsedTable> parsedTables,
                                                                   List<MesProBatchRecordReportView> reports,
                                                                   List<String> productNames);

    MesProBatchRecordRouteGenerationResult generateRouteOnlyForUploadedWord(String batchRecordName,
                                                                            List<MesProBatchRecordParsedTable> parsedTables,
                                                                            List<String> productNames);

    MesProBatchRecordRouteGenerationResult generateRouteOnlyForUploadedWord(String batchRecordName,
                                                                            List<MesProBatchRecordParsedTable> parsedTables,
                                                                            List<String> productNames,
                                                                            Long expectedRouteId,
                                                                            Long expectedRouteVersionId,
                                                                            Boolean routeUpgradeConfirmed);

    MesProBatchRecordRouteGenerationResult generateRouteOnlyForUploadedWord(String batchRecordName,
                                                                            List<MesProBatchRecordParsedTable> parsedTables,
                                                                            List<String> productNames,
                                                                            Long expectedRouteId,
                                                                            Long expectedRouteVersionId,
                                                                            Boolean routeUpgradeConfirmed,
                                                                            Long expectedRouteCandidateVersionId);

    MesProBatchRecordRouteGenerationResult generateRouteOnlyForUploadedWord(String batchRecordName,
                                                                            List<MesProBatchRecordParsedTable> parsedTables,
                                                                            List<String> productNames,
                                                                            Long expectedRouteId,
                                                                            Long expectedRouteVersionId,
                                                                            Boolean routeUpgradeConfirmed,
                                                                            Long expectedRouteCandidateVersionId,
                                                                            Long dccProjectCodeId);

    MesProBatchRecordRouteGenerationResult generateForUploadedWord(String batchRecordName,
                                                                   List<MesProBatchRecordParsedTable> parsedTables,
                                                                   List<MesProBatchRecordReportView> reports,
                                                                   List<String> productNames,
                                                                   Long batchRecordDefinitionId,
                                                                   Long batchRecordVersionId);

    MesProBatchRecordRouteGenerationResult generateForUploadedWord(String batchRecordName,
                                                                   List<MesProBatchRecordParsedTable> parsedTables,
                                                                   List<MesProBatchRecordReportView> reports,
                                                                   List<String> productNames,
                                                                   Long batchRecordDefinitionId,
                                                                   Long batchRecordVersionId,
                                                                   Long expectedRouteId,
                                                                   Long expectedRouteVersionId,
                                                                   Boolean routeUpgradeConfirmed);

    MesProBatchRecordRouteGenerationResult generateForUploadedWord(String batchRecordName,
                                                                   List<MesProBatchRecordParsedTable> parsedTables,
                                                                   List<MesProBatchRecordReportView> reports,
                                                                   List<String> productNames,
                                                                   Long batchRecordDefinitionId,
                                                                   Long batchRecordVersionId,
                                                                   Long expectedRouteId,
                                                                   Long expectedRouteVersionId,
                                                                   Boolean routeUpgradeConfirmed,
                                                                   boolean applyExistingRouteRebuild);

    MesProBatchRecordRouteGenerationResult generateForUploadedWord(String batchRecordName,
                                                                   List<MesProBatchRecordParsedTable> parsedTables,
                                                                   List<MesProBatchRecordReportView> reports,
                                                                   List<String> productNames,
                                                                   Long batchRecordDefinitionId,
                                                                   Long batchRecordVersionId,
                                                                   Long expectedRouteId,
                                                                   Long expectedRouteVersionId,
                                                                   Boolean routeUpgradeConfirmed,
                                                                   Long expectedRouteCandidateVersionId,
                                                                   boolean applyExistingRouteRebuild);

    MesProBatchRecordRouteGenerationResult generateForUploadedWord(String batchRecordName,
                                                                   List<MesProBatchRecordParsedTable> parsedTables,
                                                                   List<MesProBatchRecordReportView> reports,
                                                                   List<String> productNames,
                                                                   Long batchRecordDefinitionId,
                                                                   Long batchRecordVersionId,
                                                                   Long expectedRouteId,
                                                                   Long expectedRouteVersionId,
                                                                   Boolean routeUpgradeConfirmed,
                                                                   Long expectedRouteCandidateVersionId,
                                                                   boolean applyExistingRouteRebuild,
                                                                   Long dccProjectCodeId);

    MesProBatchRecordRouteGenerationResult generateBatchRecordBindingCandidateForUploadedWord(
            String batchRecordName,
            List<MesProBatchRecordParsedTable> parsedTables,
            List<MesProBatchRecordReportView> reports,
            Long batchRecordDefinitionId,
            Long batchRecordVersionId,
            Long expectedRouteId,
            Long expectedRouteVersionId,
            Boolean routeUpgradeConfirmed);

    MesProBatchRecordRouteGenerationResult generateBatchRecordBindingCandidateForUploadedWord(
            String batchRecordName,
            List<MesProBatchRecordParsedTable> parsedTables,
            List<MesProBatchRecordReportView> reports,
            Long batchRecordDefinitionId,
            Long batchRecordVersionId,
            Long expectedRouteId,
            Long expectedRouteVersionId,
            Boolean routeUpgradeConfirmed,
            Long expectedRouteCandidateVersionId);

    MesProBatchRecordRouteGenerationResult generateBatchRecordBindingCandidateForUploadedWord(
            String batchRecordName,
            List<MesProBatchRecordParsedTable> parsedTables,
            List<MesProBatchRecordReportView> reports,
            Long batchRecordDefinitionId,
            Long batchRecordVersionId,
            Long expectedRouteId,
            Long expectedRouteVersionId,
            Boolean routeUpgradeConfirmed,
            Long expectedRouteCandidateVersionId,
            Long dccProjectCodeId);
}
