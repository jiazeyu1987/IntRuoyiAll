package cn.iocoder.yudao.module.mes.service.pro.simulation.stage5;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesStage5FinalReleaseSimulationCommand {

    private Long actorUserId;
    private String simulationRunId;
    private Long batchExecutionId;
    private String stage4SimulationRunId;
    private String previousSimulationRunId;
}
