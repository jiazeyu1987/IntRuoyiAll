package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFlow6CompletionBackfillReceipt;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MesPqcReleaseDossierPortImplTest {

    @Test
    void readsTenantScopedFlow4ReceiptWithoutWritingDossierDocuments() {
        MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort receiptPort =
                mock(MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort.class);
        MesFlow6CompletionBackfillReceipt receipt = new MesFlow6CompletionBackfillReceipt()
                .setReceiptId(88L)
                .setBatchRecordId(101L)
                .setProcessInspectionId(102L)
                .setHasActualLoss(false)
                .setLossQuantity(java.math.BigDecimal.ZERO)
                .setLossReportStatus("NOT_REQUIRED")
                .setSourceSnapshotHash("source-hash");
        when(receiptPort.getByReceiptId(88L, 1L)).thenReturn(receipt);
        MesPqcReleaseDossierPortImpl port = new MesPqcReleaseDossierPortImpl(receiptPort);

        TenantContextHolder.setTenantId(1L);
        try {
            assertSame(receipt, port.readCompletionReceipt(88L, 1L));
        } finally {
            TenantContextHolder.clear();
        }
    }
}
