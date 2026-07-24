package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProBatchRecordVersionGovernanceMetricsRespVO {

    private Long versionId;

    private Long pendingApprovalCount;

    private Long approvedVersionCount;

    private Long rollbackRequestCount;

    private Long confirmRequiredItemCount;

    private Long blockerItemCount;

    private String latestInspectionStatus;
}
