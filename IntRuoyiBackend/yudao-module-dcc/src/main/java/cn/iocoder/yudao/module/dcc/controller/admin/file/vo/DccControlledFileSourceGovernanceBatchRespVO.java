package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceGovernanceBatchDO;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileSourceGovernanceBatchExecutionResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(description = "Admin - DCC source governance batch execution result")
@Data
@Builder
public class DccControlledFileSourceGovernanceBatchRespVO {

    private String taskKey;
    private String batchStatus;
    private Integer processedCount;
    private Integer completedCount;
    private Integer blockedCount;
    private Integer failedCount;
    private Integer remainingCount;

    public static DccControlledFileSourceGovernanceBatchRespVO from(
            DccControlledFileSourceGovernanceBatchExecutionResult result) {
        return DccControlledFileSourceGovernanceBatchRespVO.builder()
                .taskKey(result.taskKey())
                .batchStatus(result.batchStatus())
                .processedCount(result.processedCount())
                .completedCount(result.completedCount())
                .blockedCount(result.blockedCount())
                .failedCount(result.failedCount())
                .remainingCount(result.remainingCount())
                .build();
    }

    public static DccControlledFileSourceGovernanceBatchRespVO from(
            DccControlledFileSourceGovernanceBatchDO batch) {
        return DccControlledFileSourceGovernanceBatchRespVO.builder()
                .taskKey(batch.getTaskKey())
                .batchStatus(batch.getBatchStatus())
                .completedCount(batch.getCompletedCount() == null ? 0 : batch.getCompletedCount().intValue())
                .blockedCount(batch.getBlockedCount() == null ? 0 : batch.getBlockedCount().intValue())
                .failedCount(batch.getFailedCount() == null ? 0 : batch.getFailedCount().intValue())
                .build();
    }
}
