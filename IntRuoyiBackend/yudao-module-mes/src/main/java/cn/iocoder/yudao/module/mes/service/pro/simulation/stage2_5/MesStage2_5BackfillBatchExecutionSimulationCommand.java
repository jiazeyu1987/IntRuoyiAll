package cn.iocoder.yudao.module.mes.service.pro.simulation.stage2_5;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.regex.Pattern;

@Data
@Accessors(chain = true)
public class MesStage2_5BackfillBatchExecutionSimulationCommand {

    private static final Pattern RUN_ID = Pattern.compile("[A-Za-z0-9._:-]{1,128}");

    private String simulationRunId;
    private Long activeOrderId;
    private Integer expectedVersion;
    private Long actorUserId;

    public static MesStage2_5BackfillBatchExecutionSimulationCommand validate(
            String simulationRunId, Long activeOrderId, Integer expectedVersion, Long actorUserId) {
        if (simulationRunId == null || !RUN_ID.matcher(simulationRunId.trim()).matches()) {
            throw new IllegalArgumentException("simulationRunId contains unsupported characters");
        }
        if (activeOrderId == null || activeOrderId <= 0) {
            throw new IllegalArgumentException("activeOrderId is required");
        }
        if (expectedVersion == null || expectedVersion < 0) {
            throw new IllegalArgumentException("expectedVersion is required");
        }
        if (actorUserId == null || actorUserId <= 0) {
            throw new IllegalArgumentException("actorUserId is required");
        }
        return new MesStage2_5BackfillBatchExecutionSimulationCommand()
                .setSimulationRunId(simulationRunId.trim())
                .setActiveOrderId(activeOrderId)
                .setExpectedVersion(expectedVersion)
                .setActorUserId(actorUserId);
    }
}
