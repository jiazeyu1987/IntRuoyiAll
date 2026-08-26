package cn.iocoder.yudao.module.mes.service.pro.simulation.stage4;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;

import java.util.Objects;

/**
 * Identifies the isolated Stage4 fixture so generic detail reads do not mutate it
 * with route-derived production tasks.
 */
public final class MesStage4DossierUploadSimulationMarker {

    public static final String PREFIX =
            "[STAGE4_SIMULATION][stageCode=STAGE4_DOSSIER_UPLOAD][isSimulation=true]";

    private MesStage4DossierUploadSimulationMarker() {
    }

    public static String value(String simulationRunId) {
        return PREFIX + "[simulationRunId=" + simulationRunId + "]";
    }

    public static boolean isStage4Simulation(MesProEdhrBatchExecutionDO batch) {
        if (batch == null || !Objects.equals(batch.getRemark(), batch.getActiveContextKey())) {
            return false;
        }
        String marker = batch.getRemark();
        String runIdPrefix = PREFIX + "[simulationRunId=";
        return marker != null
                && marker.startsWith(runIdPrefix)
                && marker.endsWith("]")
                && marker.length() > runIdPrefix.length() + 1;
    }
}
