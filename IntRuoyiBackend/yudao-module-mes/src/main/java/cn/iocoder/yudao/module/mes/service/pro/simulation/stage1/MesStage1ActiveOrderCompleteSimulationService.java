package cn.iocoder.yudao.module.mes.service.pro.simulation.stage1;

public interface MesStage1ActiveOrderCompleteSimulationService {

    MesStage1ActiveOrderCompleteSimulationResult simulate(
            MesStage1ActiveOrderCompleteSimulationCommand command);
}
