package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 活跃订单固定测试重置结果")
@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderTestResetRespVO {

    @Schema(description = "生产订单号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String workOrderCode;
    private Long workOrderId;
    private Long previousActiveOrderCount;
    private Long activeOrderId;
    private String action;
    private Long deletedEventCount;
    private Long deletedBatchExecutionCount;
    private Long deletedRecordExecutionCount;
}
