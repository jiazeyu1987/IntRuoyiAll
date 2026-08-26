package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

@Data
@Accessors(chain = true)
@Schema(description = "管理后台 - IDI Stage6 模拟数据生成 Response VO")
public class MesStage6IdiSimulationRespVO {

    private String simulationRunId;
    private String cleanedSimulationRunId;
    private Long workOrderId;
    private String workOrderCode;
    private Long activeOrderId;
    private Long completionReceiptId;
    private String completionStatus;
    private Integer productionSubmitCount;
    private Integer productionReviewCount;
    private Integer pqcSubmitCount;
    private Integer pqcReviewCount;
    private String releasePreparationStatus;
    private String traceEntryPath;
    private Long batchExecutionId;
    private Long executionId;
    private Long releaseTransactionId;
    private Long releaseDecisionId;
    private String releaseReceiptId;
    private Map<String, Object> releaseSnapshot;
    private Map<String, Object> traceabilitySnapshot;
}
