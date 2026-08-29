package cn.iocoder.yudao.module.mes.service.pro.simulation.stage4;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
public class MesStage4DossierUploadSimulationResult {

    private String simulationRunId;
    private String inputMode;
    private String cleanedSimulationRunId;
    private String batchExecutionId;
    private String batchExecutionCode;
    private String detailPath;
    private Map<String, Object> completeBatchExecutionSnapshot;
    private Map<String, Object> batchExecutionSnapshot;
    private Map<String, Object> batchExecutionDossierSnapshot;
    private boolean dossierReadyForRelease;
    private List<String> blockers = List.of();
}
