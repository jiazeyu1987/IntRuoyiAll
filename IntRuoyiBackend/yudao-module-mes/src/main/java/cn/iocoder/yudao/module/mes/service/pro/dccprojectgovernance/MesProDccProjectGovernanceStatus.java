package cn.iocoder.yudao.module.mes.service.pro.dccprojectgovernance;

import lombok.Builder;

import java.util.List;

@Builder
public record MesProDccProjectGovernanceStatus(
        String projectName,
        Integer dccProjectCodeCount,
        String routeStatus,
        Long routeCount,
        List<String> routeCodes,
        List<String> routeVersionNos,
        String mainBatchRecordStatus,
        Long mainBatchRecordCount,
        List<String> mainBatchRecordVersionNos,
        String lossReportStatus,
        Long lossReportCount,
        List<String> lossReportCodes,
        List<String> lossReportVersionNos,
        String processInspectionStatus,
        Long processInspectionCount,
        List<String> processInspectionCodes,
        List<String> processInspectionVersionNos,
        String parameterRecordStatus,
        Long parameterRecordCount,
        List<String> parameterRecordCodes,
        List<String> parameterRecordVersionNos,
        List<String> blockerMessages
) {
}
