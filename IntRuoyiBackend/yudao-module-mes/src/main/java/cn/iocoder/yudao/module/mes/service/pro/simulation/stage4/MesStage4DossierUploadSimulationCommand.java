package cn.iocoder.yudao.module.mes.service.pro.simulation.stage4;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesStage4DossierUploadSimulationCommand {

    private Long actorUserId;
    private String simulationRunId;
}
