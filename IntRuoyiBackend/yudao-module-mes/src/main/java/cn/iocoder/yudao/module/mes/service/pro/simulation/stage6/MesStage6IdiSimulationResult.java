package cn.iocoder.yudao.module.mes.service.pro.simulation.stage6;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

@Data
@Accessors(chain = true)
public class MesStage6IdiSimulationResult {

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
