package cn.iocoder.yudao.module.mes.service.pro.simulation.stage5;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
public class MesStage5FinalReleaseSimulationResult {

    private String simulationRunId;
    private String cleanedSimulationRunId;
    private String batchExecutionId;
    private String batchExecutionCode;
    private String releaseApplicationId;
    private String releaseTransactionId;
    private String managerReleaseWorkTaskId;
    private String managerSignoffEvidenceHash;
    private String managerCandidateSnapshotHash;
    private String reportSnapshotHash;
    private String sourceDossierHash;
    private String releaseStatus;
    private String applicationStatus;
    private String managerWorkTaskPath;
    private boolean finalReleaseReady;
    private Map<String, Object> batchExecutionDossierSnapshot;
    private Map<String, Object> managerReleaseContext;
    private Map<String, Object> precheckResult;
    private Map<String, Object> runManifest;
    private Map<String, Object> releaseSnapshot;
    private List<String> blockers = List.of();
}
