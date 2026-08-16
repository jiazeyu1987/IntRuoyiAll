package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

public interface MesProductionReleaseReportStageInitializer {

    MesProductionReleaseReportStageInitializationResult initializeRequiredReportStage(
            MesProductionReleaseReportStageInitializationCommand command);
}
