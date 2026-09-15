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
    @Mock private cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper eventMapper;
    @Mock private cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper allocationMapper;
    @Mock private cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper reviewMapper;

    private MesPqcReleaseDossierPortImpl port;

    @BeforeEach
    void setUp() {
        port = new MesPqcReleaseDossierPortImpl(
                null, null, null, null, null, null, null, eventMapper, allocationMapper, reviewMapper,
                batchRecordWriter, processInspectionWriter, lossReportWriter, null);
    }

    @Test
    void batchRecordSourcesFollowOnlyTheTargetOrdersAllocations() {
        var application = cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO
                .builder().activeOrderId(10L).workOrderId(30L).routeId(40L).build();
        var snapshot = cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO
                .builder().routeProcessId(101L).processId(201L).build();
        var allocation = cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO
                .builder().id(501L).activeOrderId(10L).workOrderId(30L).routeProcessId(101L).processId(201L)
                .eventId(401L).reviewId(601L).allocatedQuantity(new BigDecimal("60")).build();
        var source = cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO
                .builder().id(401L).workOrderId(29L).routeId(40L).routeProcessId(101L).processId(201L).build();
        when(allocationMapper.selectListByActiveOrderIdForUpdate(10L)).thenReturn(List.of(allocation));
        when(eventMapper.selectProductionSubmitsByIdsForUpdate(List.of(401L))).thenReturn(List.of(source));
        when(reviewMapper.selectById(601L)).thenReturn(
                cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO
                        .builder().id(601L).eventId(401L).build());

        List<cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand.ProcessSource> result =
                org.springframework.test.util.ReflectionTestUtils.invokeMethod(port, "loadBatchRecordSources",
                        application, List.of(snapshot), List.of());

        assertEquals(29L, result.get(0).getSourceEvents().get(0).getWorkOrderId());
        assertEquals(List.of(allocation), result.get(0).getAllocations());
        assertEquals(new BigDecimal("60"), result.get(0).getAllocations().get(0).getAllocatedQuantity());
    }

    @Test
    void mixedLossEvidenceRetainsBothTypes() {
        var plan = plan();
        when(batchRecordWriter.write(plan.getBatchRecordPlan(), BATCH_EXECUTION_ID)).thenReturn(batchRecordWrite(List.of(11L)));
        when(processInspectionWriter.write(plan.getProcessInspectionPlan(), BATCH_EXECUTION_ID)).thenReturn(processInspectionWrite(List.of(21L), List.of()));
        when(lossReportWriter.write(plan.getLossReportPlan(), BATCH_EXECUTION_ID)).thenReturn(noLossWrite()
                .setHasActualLoss(true).setLossReportStatus("SUCCESS").setLossQuantity(BigDecimal.TEN)
                .setBatchRecordExecutionIds(List.of(31L)).setFormCenterInstanceIds(List.of(32L))
                .setFieldAuditIds(List.of(33L, 34L)).setFieldAuditHeadHashes(List.of("traditional", "dynamic")));
        var result = port.write(plan, BATCH_EXECUTION_ID);
        assertEquals(List.of(31L), result.getLossReportEvidenceIds());
        assertEquals(List.of(32L), result.getLossReportFormCenterInstanceIds());
        assertEquals(List.of(33L, 34L), result.getLossReportFieldAuditIds());
    }

    @Test
    void dynamicLossWithoutAuditCannotPassRelease() {
        var plan = plan();
        when(batchRecordWriter.write(plan.getBatchRecordPlan(), BATCH_EXECUTION_ID)).thenReturn(batchRecordWrite(List.of(11L)));
        when(processInspectionWriter.write(plan.getProcessInspectionPlan(), BATCH_EXECUTION_ID)).thenReturn(processInspectionWrite(List.of(21L), List.of()));
        when(lossReportWriter.write(plan.getLossReportPlan(), BATCH_EXECUTION_ID)).thenReturn(noLossWrite()
                .setHasActualLoss(true).setLossReportStatus("SUCCESS").setLossQuantity(BigDecimal.ONE)
                .setFormCenterInstanceIds(List.of(32L)));
        var error = assertThrows(MesReleaseFlowBlockerException.class, () -> port.write(plan, BATCH_EXECUTION_ID));
        assertEquals(MesReleaseFlowBlockerType.LOSS_REPORT_SOURCE_REQUIRED, error.getFailure().getBlockers().get(0).getBlockerType());
    }

    @Test
    void dynamicLossEvidenceMustSurviveFormalDossierWrite() {
        var plan = plan();
        when(batchRecordWriter.write(plan.getBatchRecordPlan(), BATCH_EXECUTION_ID))
                .thenReturn(batchRecordWrite(List.of(110001L)));
        when(processInspectionWriter.write(plan.getProcessInspectionPlan(), BATCH_EXECUTION_ID))
                .thenReturn(processInspectionWrite(List.of(), List.of(220001L)));
        when(lossReportWriter.write(plan.getLossReportPlan(), BATCH_EXECUTION_ID))
                .thenReturn(noLossWrite().setHasActualLoss(true).setLossReportStatus("SUCCESS")
                        .setLossQuantity(BigDecimal.ONE).setFormCenterInstanceIds(List.of(330001L))
                        .setFieldAuditIds(List.of(330002L)).setFieldAuditHeadHashes(List.of("loss-audit-head")));

        var result = port.write(plan, BATCH_EXECUTION_ID);

        assertEquals(List.of(), result.getLossReportEvidenceIds());
        assertEquals(List.of(330001L), com.alibaba.fastjson.JSON.parseObject(
                com.alibaba.fastjson.JSON.toJSONString(result)).getJSONArray("lossReportFormCenterInstanceIds").toJavaList(Long.class));
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
