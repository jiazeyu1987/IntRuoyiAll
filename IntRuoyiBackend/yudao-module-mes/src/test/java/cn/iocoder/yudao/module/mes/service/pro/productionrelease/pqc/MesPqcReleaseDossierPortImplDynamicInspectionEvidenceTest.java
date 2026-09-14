package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerException;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerType;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseBatchRecordPlan;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseBatchRecordWriteResult;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseBatchRecordWriter;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseLossReportPlan;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseLossReportWriteResult;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseLossReportWriter;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseProcessInspectionPlan;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseProcessInspectionWriteResult;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseProcessInspectionWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesPqcReleaseDossierPortImplDynamicInspectionEvidenceTest {

    private static final Long BATCH_EXECUTION_ID = 910001L;

    @Mock
    private MesTeamLeaderActiveOrderReleaseBatchRecordWriter batchRecordWriter;
    @Mock
    private MesTeamLeaderActiveOrderReleaseProcessInspectionWriter processInspectionWriter;
    @Mock
    private MesTeamLeaderActiveOrderReleaseLossReportWriter lossReportWriter;

    private MesPqcReleaseDossierPortImpl port;

    @BeforeEach
    void setUp() {
        port = new MesPqcReleaseDossierPortImpl(
                null, null, null, null, null, null, null, null, null, null,
                batchRecordWriter, processInspectionWriter, lossReportWriter, null);
    }

    @Test
    void dynamicFormCenterProcessInspectionEvidenceSatisfiesFormalDossierWrite() {
        MesPqcReleaseDossierPlan plan = plan();
        when(batchRecordWriter.write(plan.getBatchRecordPlan(), BATCH_EXECUTION_ID))
                .thenReturn(batchRecordWrite(List.of(110001L)));
        when(processInspectionWriter.write(plan.getProcessInspectionPlan(), BATCH_EXECUTION_ID))
                .thenReturn(processInspectionWrite(List.of(), List.of(220001L)));
        when(lossReportWriter.write(plan.getLossReportPlan(), BATCH_EXECUTION_ID))
                .thenReturn(noLossWrite());

        MesPqcReleaseDossierWriteResult result = port.write(plan, BATCH_EXECUTION_ID);

        assertEquals(List.of(110001L), result.getBatchRecordEvidenceIds());
        assertEquals(List.of(), result.getProcessInspectionEvidenceIds());
        assertEquals(List.of(220001L), result.getProcessInspectionFormCenterInstanceIds());
        assertEquals(List.of(), result.getLossReportEvidenceIds());
    }

    @Test
    void missingProcessInspectionEvidenceBlocksAsProcessInspectionSource() {
        MesPqcReleaseDossierPlan plan = plan();
        when(batchRecordWriter.write(plan.getBatchRecordPlan(), BATCH_EXECUTION_ID))
                .thenReturn(batchRecordWrite(List.of(110001L)));
        when(processInspectionWriter.write(plan.getProcessInspectionPlan(), BATCH_EXECUTION_ID))
                .thenReturn(processInspectionWrite(List.of(), List.of()));
        when(lossReportWriter.write(plan.getLossReportPlan(), BATCH_EXECUTION_ID))
                .thenReturn(noLossWrite());

        MesReleaseFlowBlockerException failure = assertThrows(MesReleaseFlowBlockerException.class,
                () -> port.write(plan, BATCH_EXECUTION_ID));

        assertEquals(MesReleaseFlowBlockerType.PROCESS_INSPECTION_SOURCE_REQUIRED,
                failure.getFailure().getBlockers().get(0).getBlockerType());
    }

    @Test
    void dynamicProcessInspectionEvidenceWithoutFieldAuditBlocks() {
        MesPqcReleaseDossierPlan plan = plan();
        when(batchRecordWriter.write(plan.getBatchRecordPlan(), BATCH_EXECUTION_ID))
                .thenReturn(batchRecordWrite(List.of(110001L)));
        when(processInspectionWriter.write(plan.getProcessInspectionPlan(), BATCH_EXECUTION_ID))
                .thenReturn(processInspectionWrite(List.of(), List.of(220001L))
                        .setFieldAuditIds(List.of())
                        .setFieldAuditHeadHashes(List.of()));
        when(lossReportWriter.write(plan.getLossReportPlan(), BATCH_EXECUTION_ID))
                .thenReturn(noLossWrite());

        MesReleaseFlowBlockerException failure = assertThrows(MesReleaseFlowBlockerException.class,
                () -> port.write(plan, BATCH_EXECUTION_ID));

        assertEquals(MesReleaseFlowBlockerType.PROCESS_INSPECTION_SOURCE_REQUIRED,
                failure.getFailure().getBlockers().get(0).getBlockerType());
    }

    private MesPqcReleaseDossierPlan plan() {
        return new MesPqcReleaseDossierPlan()
                .setBatchRecordPlan(new MesTeamLeaderActiveOrderReleaseBatchRecordPlan()
                        .setSourceObjectIds(List.of(11L))
                        .setSourceValueHashes(List.of("batch-source-hash")))
                .setProcessInspectionPlan(new MesTeamLeaderActiveOrderReleaseProcessInspectionPlan()
                        .setSourceObjectIds(List.of(21L))
                        .setSourceValueHashes(List.of("inspection-source-hash")))
                .setLossReportPlan(new MesTeamLeaderActiveOrderReleaseLossReportPlan()
                        .setSourceObjectIds(List.of(31L))
                        .setSourceValueHashes(List.of("loss-source-hash")));
    }

    private MesTeamLeaderActiveOrderReleaseBatchRecordWriteResult batchRecordWrite(List<Long> executionIds) {
        return new MesTeamLeaderActiveOrderReleaseBatchRecordWriteResult()
                .setDocumentType("BATCH_RECORD")
                .setBatchRecordExecutionIds(executionIds)
                .setFieldAuditIds(List.of(120001L))
                .setFieldAuditHeadHashes(List.of("batch-audit-head"))
                .setSourceObjectIds(List.of(11L))
                .setSourceValueHashes(List.of("batch-source-hash"))
                .setBlockers(List.of());
    }

    private MesTeamLeaderActiveOrderReleaseProcessInspectionWriteResult processInspectionWrite(
            List<Long> executionIds, List<Long> formCenterInstanceIds) {
        return new MesTeamLeaderActiveOrderReleaseProcessInspectionWriteResult()
                .setDocumentType("PROCESS_INSPECTION")
                .setBatchRecordExecutionIds(executionIds)
                .setFormCenterInstanceIds(formCenterInstanceIds)
                .setFieldAuditIds(List.of(230001L))
                .setFieldAuditHeadHashes(List.of("inspection-audit-head"))
                .setSourceObjectIds(List.of(21L))
                .setSourceValueHashes(List.of("inspection-source-hash"))
                .setBlockers(List.of());
    }

    private MesTeamLeaderActiveOrderReleaseLossReportWriteResult noLossWrite() {
        return new MesTeamLeaderActiveOrderReleaseLossReportWriteResult()
                .setDocumentType("LOSS_REPORT")
                .setBatchRecordExecutionIds(List.of())
                .setSourceObjectIds(List.of(31L))
                .setSourceValueHashes(List.of("loss-source-hash"))
                .setBlockers(List.of())
                .setLossReportStatus("NOT_REQUIRED")
                .setHasActualLoss(false)
                .setLossQuantity(BigDecimal.ZERO)
                .setSourceSnapshotHash("loss-source-snapshot");
    }
}
