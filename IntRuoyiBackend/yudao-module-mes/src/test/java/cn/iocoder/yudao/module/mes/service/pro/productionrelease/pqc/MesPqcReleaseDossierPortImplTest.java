package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFlow6CompletionBackfillReceipt;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseBatchRecordWriter;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseLossReportWriter;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseProcessInspectionWriter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class MesPqcReleaseDossierPortImplTest {

    @Test
    void activeOrderWriteReusesTxAReceiptWithoutCallingLegacyWriters() {
        MesTeamLeaderActiveOrderReleaseBatchRecordWriter batchWriter =
                mock(MesTeamLeaderActiveOrderReleaseBatchRecordWriter.class);
        MesTeamLeaderActiveOrderReleaseProcessInspectionWriter inspectionWriter =
                mock(MesTeamLeaderActiveOrderReleaseProcessInspectionWriter.class);
        MesTeamLeaderActiveOrderReleaseLossReportWriter lossWriter =
                mock(MesTeamLeaderActiveOrderReleaseLossReportWriter.class);
        MesFlow6CompletionBackfillReceipt receipt = new MesFlow6CompletionBackfillReceipt()
                .setReceiptId(88L).setBatchRecordId(101L).setProcessInspectionId(102L)
                .setHasActualLoss(false).setLossQuantity(java.math.BigDecimal.ZERO)
                .setLossReportStatus("NOT_REQUIRED").setSourceSnapshotHash("source-hash");
        MesPqcReleaseDossierPortImpl port = new MesPqcReleaseDossierPortImpl(
                null, null, null, null, null, null, null, null, null, null,
                batchWriter, inspectionWriter, lossWriter, null,
                mock(MesProcessPoolActiveOrderCompletionReceiptMapper.class),
                mock(MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort.class));

        MesPqcReleaseDossierWriteResult result = port.write(new MesPqcReleaseDossierPlan()
                .setActiveOrderId(10L).setCompletionBackfillReceiptId(88L)
                .setCompletionReceipt(receipt), 500L);

        assertEquals(java.util.List.of(101L), result.getBatchRecordEvidenceIds());
        assertEquals(java.util.List.of(102L), result.getProcessInspectionEvidenceIds());
        assertEquals(java.util.List.of(), result.getLossReportEvidenceIds());
        verifyNoInteractions(batchWriter, inspectionWriter, lossWriter);
    }
}
