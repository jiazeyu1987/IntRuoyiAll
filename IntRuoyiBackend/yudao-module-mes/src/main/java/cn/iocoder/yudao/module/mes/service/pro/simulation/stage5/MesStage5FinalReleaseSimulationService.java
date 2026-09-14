package cn.iocoder.yudao.module.mes.service.pro.simulation.stage5;

public interface MesStage5FinalReleaseSimulationService {

    MesStage5FinalReleaseSimulationResult prepare(MesStage5FinalReleaseSimulationCommand command);

    java.util.Map<String, Object> getReleaseSnapshot(String simulationRunId, Long batchExecutionId);
}
