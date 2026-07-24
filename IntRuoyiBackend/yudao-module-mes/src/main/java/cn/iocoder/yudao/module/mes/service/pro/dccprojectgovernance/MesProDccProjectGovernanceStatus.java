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
        String mainBatchRecordStatus,
        Long mainBatchRecordCount,
        List<String> mainBatchRecordVersionNos,
        String lossReportStatus,
        Long lossReportCount,
        List<String> lossReportCodes,
        String processInspectionStatus,
        Long processInspectionCount,
        List<String> processInspectionCodes,
        String parameterRecordStatus,
        Long parameterRecordCount,
        List<String> parameterRecordCodes,
        List<String> blockerMessages
) {
}
