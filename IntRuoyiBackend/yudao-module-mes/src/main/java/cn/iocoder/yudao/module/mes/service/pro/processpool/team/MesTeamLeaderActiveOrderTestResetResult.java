package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MesTeamLeaderActiveOrderTestResetResult {

    private String workOrderCode;
    private Long workOrderId;
    private Long previousActiveOrderCount;
    private Long activeOrderId;
    private String action;
    private Long deletedEventCount;
    private Long deletedBatchExecutionCount;
    private Long deletedRecordExecutionCount;
}
