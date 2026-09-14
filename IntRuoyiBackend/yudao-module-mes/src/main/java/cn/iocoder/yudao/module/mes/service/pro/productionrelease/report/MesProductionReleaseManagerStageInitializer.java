package cn.iocoder.yudao.module.mes.service.pro.productionrelease.report;

public interface MesProductionReleaseManagerStageInitializer {

    MesProductionReleaseManagerStageInitializationResult initializeManagerReleaseStage(
            MesProductionReleaseManagerStageInitializationCommand command);
}
