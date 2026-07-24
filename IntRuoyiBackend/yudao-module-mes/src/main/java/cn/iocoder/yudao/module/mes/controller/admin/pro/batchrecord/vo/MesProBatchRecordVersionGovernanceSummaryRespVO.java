package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProBatchRecordVersionGovernanceSummaryRespVO {

    private Long definitionId;

    private Long currentVersionId;

    private String currentVersionNo;

    private Long versionCount;

    private Long activeExecutionCount;

    private Long historicalExecutionCount;

    private Long slotBindingCount;

    private Long rollbackPendingCount;

    private Long blockingInspectionCount;
}
