package cn.iocoder.yudao.module.mes.service.pro.simulation.stage2_5;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
public class MesStage2_5BackfillBatchExecutionSimulationResult {

    private String simulationRunId;
    private String cleanedSimulationRunId;
    private Long batchExecutionId;
    private String batchExecutionCode;
    private Long completionReceiptId;
    private String detailPath;
    private Map<String, Object> batchExecutionSnapshot;
    private List<String> blockers;
}
