package cn.iocoder.yudao.module.mes.service.pro.simulation.stage2_5;

import jakarta.validation.Valid;

public interface MesStage2_5BackfillBatchExecutionSimulationService {

    MesStage2_5BackfillBatchExecutionSimulationResult simulate(
            @Valid MesStage2_5BackfillBatchExecutionSimulationCommand command);
}
