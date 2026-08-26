package cn.iocoder.yudao.module.mes.service.pro.simulation.stage1;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesStage1ActiveOrderCompleteSimulationCommand {

    private String simulationRunId;
    private Long templateActiveOrderId;
    private Long actorUserId;

    public static MesStage1ActiveOrderCompleteSimulationCommand validate(String simulationRunId,
                                                                          Long templateActiveOrderId,
                                                                          Long actorUserId) {
        if (simulationRunId == null || !simulationRunId.matches("[A-Za-z0-9._:-]{1,128}")) {
            throw new IllegalArgumentException("STAGE1_SIMULATION_RUN_ID_INVALID");
        }
        if (templateActiveOrderId == null || templateActiveOrderId <= 0
                || actorUserId == null || actorUserId <= 0) {
            throw new IllegalArgumentException("STAGE1_SIMULATION_ACTOR_OR_TEMPLATE_INVALID");
        }
        return new MesStage1ActiveOrderCompleteSimulationCommand()
                .setSimulationRunId(simulationRunId)
                .setTemplateActiveOrderId(templateActiveOrderId)
                .setActorUserId(actorUserId);
    }
}
