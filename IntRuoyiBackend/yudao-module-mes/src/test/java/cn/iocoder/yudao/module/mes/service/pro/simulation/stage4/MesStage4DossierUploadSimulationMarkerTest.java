package cn.iocoder.yudao.module.mes.service.pro.simulation.stage4;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesStage4DossierUploadSimulationMarkerTest {

    @Test
    void recognizesOnlyTheExactStage4FixtureMarker() {
        String marker = MesStage4DossierUploadSimulationMarker.value("STAGE4-TEST-1");
        MesProEdhrBatchExecutionDO batch = new MesProEdhrBatchExecutionDO()
                .setRemark(marker)
                .setActiveContextKey(marker);

        assertTrue(MesStage4DossierUploadSimulationMarker.isStage4Simulation(batch));
    }

    @Test
    void rejectsMarkerWhenTheTwoPersistedFieldsDoNotMatch() {
        MesProEdhrBatchExecutionDO batch = new MesProEdhrBatchExecutionDO()
                .setRemark(MesStage4DossierUploadSimulationMarker.value("STAGE4-TEST-1"))
                .setActiveContextKey("real-active-context");

        assertFalse(MesStage4DossierUploadSimulationMarker.isStage4Simulation(batch));
    }
}
