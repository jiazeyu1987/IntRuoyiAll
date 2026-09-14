package cn.iocoder.yudao.module.mes.service.pro.simulation.stage6;

import jakarta.validation.Valid;

public interface MesStage6IdiSimulationService {

    MesStage6IdiSimulationResult simulate(@Valid MesStage6IdiSimulationCommand command);
}
