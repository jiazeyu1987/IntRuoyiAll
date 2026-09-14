package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - MES Stage2.5 活跃订单完工闭环模拟 Response VO")
@Data
@Accessors(chain = true)
public class MesStage2_5BackfillBatchExecutionSimulationRespVO {

    private String simulationRunId;
    private String cleanedSimulationRunId;
    private Long batchExecutionId;
    private String batchExecutionCode;
    private Long completionReceiptId;
    private String detailPath;
    private Map<String, Object> batchExecutionSnapshot;
    private List<String> blockers;
}
