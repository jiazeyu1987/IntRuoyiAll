package cn.iocoder.yudao.module.mes.service.pro.simulation.stage6;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.regex.Pattern;

@Data
@Accessors(chain = true)
public class MesStage6IdiSimulationCommand {

    private static final Pattern RUN_ID = Pattern.compile("[A-Za-z0-9._:-]{1,128}");

    private String simulationRunId;
    private String stage5SimulationRunId;
    private Long batchExecutionId;

    public static MesStage6IdiSimulationCommand validate(String simulationRunId, String stage5SimulationRunId,
                                                         Long batchExecutionId) {
        String stage6RunId = normalize(simulationRunId, "simulationRunId");
        String stage5RunId = normalize(stage5SimulationRunId, "stage5SimulationRunId");
        if (batchExecutionId == null || batchExecutionId <= 0) {
            throw new IllegalArgumentException("batchExecutionId contains unsupported value");
        }
        return new MesStage6IdiSimulationCommand()
                .setSimulationRunId(stage6RunId)
                .setStage5SimulationRunId(stage5RunId)
                .setBatchExecutionId(batchExecutionId);
    }

    private static String normalize(String value, String field) {
        if (value == null || !RUN_ID.matcher(value.trim()).matches()) {
            throw new IllegalArgumentException(field + " contains unsupported characters");
        }
        return value.trim();
    }
}
