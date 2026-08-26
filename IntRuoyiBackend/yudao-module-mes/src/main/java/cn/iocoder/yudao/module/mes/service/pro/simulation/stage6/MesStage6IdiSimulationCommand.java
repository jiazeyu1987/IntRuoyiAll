package cn.iocoder.yudao.module.mes.service.pro.simulation.stage6;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.regex.Pattern;

@Data
@Accessors(chain = true)
public class MesStage6IdiSimulationCommand {

    private static final Pattern RUN_ID = Pattern.compile("[A-Za-z0-9._:-]{1,128}");

    private String simulationRunId;

    public static MesStage6IdiSimulationCommand validate(String simulationRunId) {
        if (simulationRunId == null || !RUN_ID.matcher(simulationRunId.trim()).matches()) {
            throw new IllegalArgumentException("simulationRunId contains unsupported characters");
        }
        return new MesStage6IdiSimulationCommand().setSimulationRunId(simulationRunId.trim());
    }
}
