package cn.iocoder.yudao.module.mes.service.pro.simulation.stage1;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Map;
import java.util.List;

@Data
@Accessors(chain = true)
public class MesStage1ActiveOrderCompleteSimulationResult {

    private String simulationRunId;
    private String cleanedSimulationRunId;
    private Long activeOrderId;
    private Long workOrderId;
    private Long pickListId;
    private List<Long> pickListIds;
    private Integer productionSubmitCount;
    private Integer productionReviewCount;
    private Integer pqcSubmitCount;
    private Integer pqcReviewCount;
    private BigDecimal productionProgressPercent;
    private BigDecimal inspectionProgressPercent;
    private boolean productionProgress100;
    private boolean inspectionProgress100;
    private boolean completionButtonEnabled;
    private Map<String, Object> activeOrderCompleteSnapshot;
}
