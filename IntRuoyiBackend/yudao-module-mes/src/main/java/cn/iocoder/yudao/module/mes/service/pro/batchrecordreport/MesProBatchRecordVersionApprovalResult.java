package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import lombok.Builder;

@Builder
public record MesProBatchRecordVersionApprovalResult(
        Long definitionId,
        Long versionId,
        String versionStatus,
        String approvalInstanceId,
        String approvalEventId,
        String approvalResult,
        String processedResult
) {
}
