package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackMaterialDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolQuantityFragmentDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMaterialMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolQuantityFragmentMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSignatureCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSignatureResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesFrontlineLossReasonValidator;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderScopeService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesProductionReportManagementSummaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDiffDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionDiffMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchRecordSignatureSubjectAdapter;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureQueryServiceImpl;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProcessPoolProductionReportCorrectionServiceTest {

    @Test
    void ownReturnReusesSignedBusinessCorrectionAndSchedulesASeparateLeaderReview() {
        var original=event();
        var rejected=cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO
                .builder().id(91L).eventId(176L).leaderType("PRODUCTION").reviewStatus("REJECTED").build();
        var context=new MesFrontlineReturnCorrectionService.ReturnContext(original,rejected,413L,0L,"1900000000000000001");
        var own=org.mockito.Mockito.mock(MesFrontlineReturnCorrectionService.class);
        ReflectionTestUtils.setField(service,"ownReturnService",own);
        when(own.requireOwnReturned(176L,413L,91L,0L,3001L,"PRODUCTION")).thenReturn(context);
        when(eventMapper.selectByIdForUpdate(176L)).thenReturn(original);
        when(fragmentMapper.selectListByEventIdForUpdate(176L)).thenReturn(List.of(fragment()));
        when(signatureService.recordFieldChangeSignature(any())).thenReturn(newSignature());
        when(revisionService.updateProductionReportRecord(any())).thenReturn(701L);
        when(fragmentMapper.updateById(any(MesProProcessPoolQuantityFragmentDO.class))).thenReturn(1);
        var result=new MesFrontlineReturnCorrectionService.CorrectionResult(176L,701L,List.of(
                new MesFrontlineReturnCorrectionService.ChangedField("完成数量","4","6")));
        when(own.complete(context,701L,3001L)).thenReturn(result);
        assertEquals(result,service.correctOwnReturned(command(),413L,91L,0L));
        var captured=ArgumentCaptor.forClass(MesProcessPoolEventRevisionUpdateReqBO.class);
        verify(revisionService).updateProductionReportRecord(captured.capture());
        assertEquals(91L,JsonUtils.parseTree(captured.getValue().getAfterPayload()).path("supersededReviewId").longValue());
        assertEquals(3001L,captured.getValue().getRevisionSignatureUserId());
        org.mockito.Mockito.verifyNoInteractions(scopeService);
        verify(own).complete(context,701L,3001L);
    }

    @Test
    void ordinaryLeaderCorrectionRejectsReturnedSystemEmployeeBeforeSignatureOrMutation() {
        var original = event();
        when(eventMapper.selectByIdForUpdate(176L)).thenReturn(original);
        var reviews = (cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper)
                ReflectionTestUtils.getField(service, "submissionReviews");
        when(reviews.selectLatestByEventIdForUpdate(176L)).thenReturn(
                cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO.builder()
                        .id(91L).eventId(176L).leaderType("PRODUCTION").reviewStatus("REJECTED").build());
        var error = assertThrows(IllegalStateException.class, () -> service.correct(command()));
        org.junit.jupiter.api.Assertions.assertTrue(error.getMessage().contains("本人退回待办"));
        org.mockito.Mockito.verifyNoInteractions(signatureService, revisionService, fragmentMapper, reportManagementSummaryService);
        verify(eventMapper, never()).updateById(any(MesProProcessPoolEventDO.class));
        assertEquals(event().getRawPayload(), original.getRawPayload());
    }

    @Test
    void rejectedOwnContextFailsBeforeAnyBusinessWriteOrSignature() {
        var own=org.mockito.Mockito.mock(MesFrontlineReturnCorrectionService.class);
        ReflectionTestUtils.setField(service,"ownReturnService",own);
        when(own.requireOwnReturned(176L,413L,91L,0L,3001L,"PRODUCTION"))
                .thenThrow(new IllegalStateException("not original system signer"));
        assertThrows(IllegalStateException.class,()->service.correctOwnReturned(command(),413L,91L,0L));
        org.mockito.Mockito.verifyNoInteractions(eventMapper,signatureService,revisionService,scopeService);
        verify(own,never()).complete(any(),any(),any());
    }

    @Mock
    private MesProProcessPoolEventMapper eventMapper;
    @Mock
    private MesProProcessPoolQuantityFragmentMapper fragmentMapper;
    @Mock
    private MesProFeedbackMapper feedbackMapper;
    @Mock
    private MesProFeedbackMaterialMapper feedbackMaterialMapper;
    @Mock
    private MesProcessPoolEventRevisionService revisionService;
    @Mock
    private MesProBatchRecordExecutionSignatureService signatureService;
    @Mock
    private MesFrontlineLossReasonValidator lossReasonValidator;
    @Mock
    private MesTeamLeaderScopeService scopeService;
    @Mock
    private MesProductionReportManagementSummaryService reportManagementSummaryService;

    private MesProcessPoolProductionReportCorrectionService service;

    @BeforeEach
    void setUp() {
        service = new MesProcessPoolProductionReportCorrectionService(
                eventMapper, fragmentMapper, feedbackMapper, feedbackMaterialMapper, revisionService,
                signatureService, lossReasonValidator, scopeService, reportManagementSummaryService);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "nonconformanceReviewService", org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService.class));
        var fixtureOwners = org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffOwnerResolver.class);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "handoffOwners", fixtureOwners);
        org.mockito.Mockito.lenient().when(fixtureOwners.submissionIdentity(org.mockito.ArgumentMatchers.any())).thenAnswer(call -> new cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffOwnerResolver.SubmissionIdentity("SYSTEM_USER", call.getArgument(0, cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO.class).getActualEmployeeId(), call.getArgument(0, cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO.class).getDeviceAccountId()));
        ReflectionTestUtils.setField(service,"sharedReportGuard",MesSharedProductionReportCorrectionGuardTest.openFixture());
        initializeAuditFixture();
        ReflectionTestUtils.setField(service, "submissionReviews", org.mockito.Mockito.mock(
                cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper.class));
        lenient().when(feedbackMapper.selectListByIdsForUpdate(List.of(5101L))).thenReturn(List.of(formalFeedback()));
        lenient().when(feedbackMapper.updateCorrectedProductionReport(
                nullable(Long.class), nullable(BigDecimal.class), nullable(BigDecimal.class),
                nullable(BigDecimal.class), nullable(Long.class), nullable(String.class), nullable(String.class)))
                .thenReturn(1);
        lenient().when(feedbackMaterialMapper.selectListByFeedbackIdForUpdate(5101L))
                .thenReturn(List.of(formalMaterial(6101L, 3401L), formalMaterial(6102L, 4801L)));
        lenient().when(feedbackMaterialMapper.updateCorrectedMaterialFact(
                nullable(Long.class), nullable(BigDecimal.class), nullable(BigDecimal.class),
                nullable(String.class), nullable(String.class), nullable(String.class)))
                .thenReturn(1);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans={true,false})
    void actualCorrectionAndSharedGuardCheckFrozenCrossOrderConsumerBeforeSigning(boolean targetFrozen) {
        var guard=new MesSharedProductionReportCorrectionGuard();
        var allocations=org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper.class);
        var orders=org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper.class);
        var receipts=org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper.class);
        var releases=org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesReportAllocationReleaseStateService.class);
        var freezeReviews=org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper.class);
        var workOrders=org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper.class);
        var freeze=new cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewServiceImpl();
        ReflectionTestUtils.setField(freeze,"reviewMapper",freezeReviews);ReflectionTestUtils.setField(freeze,"workOrderMapper",workOrders);
        when(workOrders.selectByIdForUpdate(980008L)).thenReturn(cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO.builder().id(980008L).temporaryFrozen(false).build());
        when(workOrders.selectByIdForUpdate(980009L)).thenReturn(cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO.builder().id(980009L).temporaryFrozen(targetFrozen).build());
        var original=event();var before=original.getRawPayload();
        var source=cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO.builder().id(413L).workOrderId(980008L).routeId(922119L).activeStatus("ACTIVE").businessStatus("ACTIVE").build();source.setTenantId(1L);
        var target=cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO.builder().id(414L).workOrderId(980009L).routeId(922119L).activeStatus("ACTIVE").businessStatus("ACTIVE").build();target.setTenantId(1L);
        var allocation=cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO.builder().id(5L).eventId(176L).activeOrderId(414L).workOrderId(980009L).routeProcessId(928611L).processId(922987L).allocatedQuantity(BigDecimal.ONE).lifecycleStatus("CURRENT").build();allocation.setTenantId(1L);
        when(allocations.selectListByEventIdForUpdate(176L)).thenReturn(List.of(allocation));
        when(orders.selectByIdForUpdate(413L)).thenReturn(source);when(orders.selectByIdForUpdate(414L)).thenReturn(target);
        for(var entry:java.util.Map.of("allocations",allocations,"orders",orders,"receipts",receipts,"releases",releases,"freezes",freeze).entrySet())ReflectionTestUtils.setField(guard,entry.getKey(),entry.getValue());
        ReflectionTestUtils.setField(service,"sharedReportGuard",guard);
        when(eventMapper.selectByIdForUpdate(176L)).thenReturn(original);
        when(fragmentMapper.selectListByEventIdForUpdate(176L)).thenReturn(List.of(fragment()));
        if(targetFrozen) {
            assertThrows(ServiceException.class,()->service.correct(command()));
            org.mockito.Mockito.verifyNoInteractions(signatureService,revisionService);
            verify(eventMapper,never()).updateById(any(MesProProcessPoolEventDO.class));
            verify(fragmentMapper,never()).updateById(any(MesProProcessPoolQuantityFragmentDO.class));
            verify(feedbackMapper,never()).updateCorrectedProductionReport(any(),any(),any(),any(),any(),any(),any());
            verify(feedbackMaterialMapper,never()).updateCorrectedMaterialFact(any(),any(),any(),any(),any(),any());
            assertEquals(before,original.getRawPayload());
        } else {
            source.setRouteVersionId(100L);target.setRouteVersionId(100L);
            var snapshots=org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper.class);
            ReflectionTestUtils.setField(guard,"snapshots",snapshots);
            when(snapshots.selectByActiveOrderAndProcess(anyLong(),anyLong(),anyLong())).thenAnswer(call->{
                var order=call.getArgument(0,Long.class).equals(413L)?source:target;
                var row=cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO.builder()
                        .activeOrderId(order.getId()).workOrderId(order.getWorkOrderId()).routeId(order.getRouteId()).routeVersionId(order.getRouteVersionId())
                        .routeProcessId(call.getArgument(1)).processId(call.getArgument(2)).build();row.setTenantId(1L);return row;
            });
            when(releases.findReleaseApplicationLockedActiveOrderIdsForUpdate(java.util.Set.of(413L,414L))).thenReturn(java.util.Set.of());
            when(signatureService.recordFieldChangeSignature(any())).thenReturn(newSignature());
            when(revisionService.updateProductionReportRecord(any())).thenReturn(709L);
            when(fragmentMapper.updateById(any(MesProProcessPoolQuantityFragmentDO.class))).thenReturn(1);
            assertEquals(709L,service.correct(command()));
            var revision=ArgumentCaptor.forClass(MesProcessPoolEventRevisionUpdateReqBO.class);verify(revisionService).updateProductionReportRecord(revision.capture());
            assertEquals(413L,JsonUtils.parseTree(revision.getValue().getAfterPayload()).path("activeOrderId").longValue());
            assertEquals(6,JsonUtils.parseTree(revision.getValue().getAfterPayload()).path("outputQuantity").intValue());
            verify(signatureService).recordFieldChangeSignature(any());verify(fragmentMapper).updateById(any(MesProProcessPoolQuantityFragmentDO.class));
            verify(feedbackMapper).updateCorrectedProductionReport(any(),any(),any(),any(),any(),any(),any());
        }
    }

    @Test
    void correctsBusinessFieldsAndBuildsServerOwnedAuditEvidence() {
        when(eventMapper.selectByIdForUpdate(176L)).thenReturn(event());
        when(fragmentMapper.selectListByEventIdForUpdate(176L)).thenReturn(List.of(fragment()));
        when(signatureService.recordFieldChangeSignature(any())).thenReturn(newSignature());
        when(revisionService.updateProductionReportRecord(any())).thenReturn(701L);
        when(fragmentMapper.updateById(any(MesProProcessPoolQuantityFragmentDO.class))).thenReturn(1);

        Long revisionId = service.correct(command());

        assertEquals(701L, revisionId);

        ArgumentCaptor<MesProBatchRecordExecutionFieldAuditSignatureCommand> signatureCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionFieldAuditSignatureCommand.class);
        verify(signatureService).recordFieldChangeSignature(signatureCaptor.capture());
        assertEquals(0L, signatureCaptor.getValue().getExecutionId());
        assertEquals("current-user-password", signatureCaptor.getValue().getPassword());
        assertEquals("录入时数量填错", signatureCaptor.getValue().getReasonText());

        ArgumentCaptor<MesProcessPoolEventRevisionUpdateReqBO> revisionCaptor =
                ArgumentCaptor.forClass(MesProcessPoolEventRevisionUpdateReqBO.class);
        verify(scopeService).assertCanAccessEmployee(3001L, "PRODUCTION", 964L);
        verify(revisionService).updateProductionReportRecord(revisionCaptor.capture());
        MesProcessPoolEventRevisionUpdateReqBO revision = revisionCaptor.getValue();
        assertEquals(176L, revision.getEventId());
        assertEquals(9102L, revision.getRevisionSignatureId());
        assertEquals(3001L, revision.getRevisionSignatureUserId());
        assertEquals(3001L, revision.getModifiedByUserId());
        assertEquals("录入时数量填错", revision.getChangeReason());
        assertFalse(revision.getChangedFields().isEmpty());
        assertEquals("6", revision.getChangedFields().stream()
                .filter(field -> "OUTPUT_QUANTITY".equals(field.getFieldCode()))
                .findFirst().orElseThrow().getAfterValue());

        ArgumentCaptor<MesProProcessPoolQuantityFragmentDO> fragmentCaptor =
                ArgumentCaptor.forClass(MesProProcessPoolQuantityFragmentDO.class);
        verify(fragmentMapper).updateById(fragmentCaptor.capture());
        assertEquals(new BigDecimal("6"), fragmentCaptor.getValue().getTotalQuantity());
        assertEquals(new BigDecimal("6"), fragmentCaptor.getValue().getAvailableQuantity());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"pending_review","void","external"})
    void frozenProductionCorrectionRejectsBeforeAnySignatureOrWrite(String state) {
        var original=event();
        when(eventMapper.selectByIdForUpdate(176L)).thenReturn(original);
        ReflectionTestUtils.setField(service,"nonconformanceReviewService",
                cn.iocoder.yudao.module.mes.service.pro.MesSa09Sa14FreezeFixture.authority(original.getWorkOrderId(),state));
        assertThrows(ServiceException.class,()->service.correct(command()));
        org.mockito.Mockito.verifyNoInteractions(signatureService,revisionService,fragmentMapper,scopeService);
        verify(eventMapper,never()).updateById(any(MesProProcessPoolEventDO.class));
        verify(feedbackMapper,never()).updateCorrectedProductionReport(any(),any(),any(),any(),any(),any(),any());
    }

    @Test
    void signedLossCorrectionSynchronizesFormalFeedbackAndMaterialFacts() {
        when(eventMapper.selectByIdForUpdate(176L)).thenReturn(eventWithZeroLossMaterialFacts());
        when(fragmentMapper.selectListByEventIdForUpdate(176L)).thenReturn(List.of(fragment()));
        when(lossReasonValidator.requireEnabledLossReason(928611L, 8301L, new BigDecimal("2")))
                .thenReturn(new cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesFrontlineLossReasonSnapshot(
                        8301L, "LOSS-01", "正常损耗"));
        when(signatureService.recordFieldChangeSignature(any())).thenReturn(newSignature());
        when(revisionService.updateProductionReportRecord(any())).thenReturn(708L);
        MesProcessPoolProductionReportCorrectionCommand command = command()
                .setOutputQuantity(new BigDecimal("4"))
                .setLossDetails(List.of(new MesProcessPoolProductionReportCorrectionCommand.LossDetailCommand()
                        .setReasonId(8301L)
                        .setQuantity(new BigDecimal("2"))))
                .setMaterialDetails(List.of(new MesProcessPoolProductionReportCorrectionCommand.MaterialDetailCommand()
                        .setMaterialId(3401L)
                        .setOutputQuantity(new BigDecimal("4"))
                        .setLossQuantity(new BigDecimal("2"))
                        .setLossDetails(List.of(new MesProcessPoolProductionReportCorrectionCommand.LossDetailCommand()
                                .setReasonId(8301L)
                                .setQuantity(new BigDecimal("2"))))));

        assertEquals(708L, service.correct(command));

        ArgumentCaptor<MesProcessPoolEventRevisionUpdateReqBO> revisionCaptor =
                ArgumentCaptor.forClass(MesProcessPoolEventRevisionUpdateReqBO.class);
        verify(revisionService).updateProductionReportRecord(revisionCaptor.capture());
        MesProcessPoolEventRevisionUpdateReqBO revision = revisionCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertTrue(revision.getAfterPayload().contains("\"lossQuantity\":2"));
        org.junit.jupiter.api.Assertions.assertTrue(revision.getAfterPayload().contains("\"hasActualLoss\":true"));
        org.junit.jupiter.api.Assertions.assertTrue(revision.getAfterPayload().contains("\"zeroLossConfirmed\":false"));
        org.junit.jupiter.api.Assertions.assertTrue(revision.getAfterPayload().contains("\"lossDecision\":\"REQUIRED\""));
        org.junit.jupiter.api.Assertions.assertTrue(revision.getAfterPayload()
                .contains("\"materialId\":3401,\"materialCode\":\"A001.02.034.202\",\"materialName\":\"弹簧\",\"outputQuantity\":4,\"lossQuantity\":2"));
        verify(feedbackMapper, times(3)).selectListByIdsForUpdate(List.of(5101L));
        verify(feedbackMapper).updateCorrectedProductionReport(5101L, new BigDecimal("6"),
                new BigDecimal("4"), new BigDecimal("2"), 8301L, "LOSS-01", "正常损耗");
        verify(feedbackMaterialMapper, times(3)).selectListByFeedbackIdForUpdate(5101L);
        verify(feedbackMaterialMapper).updateCorrectedMaterialFact(6101L, new BigDecimal("4"),
                new BigDecimal("2"), "[{\"reasonId\":8301,\"reasonCode\":\"LOSS-01\",\"reasonName\":\"正常损耗\",\"quantity\":2}]", null, "[]");
    }

    @Test
    void rejectsAnUnchangedBusinessFormBeforeCreatingSignature() {
        when(eventMapper.selectByIdForUpdate(176L)).thenReturn(event());
        when(fragmentMapper.selectListByEventIdForUpdate(176L)).thenReturn(List.of(fragment()));
        MesProcessPoolProductionReportCorrectionCommand unchanged = command()
                .setOutputQuantity(new BigDecimal("4"))
                .setLossDetails(List.of())
                .setDeviceParameterReadings(List.of());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.correct(unchanged));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REVISION_DIFF_REQUIRED.getCode(), ex.getCode());
        verify(signatureService, never()).recordFieldChangeSignature(any());
        verify(revisionService, never()).updateProductionReportRecord(any());
    }

    @Test
    void correctsLossDetailsAndDeviceParameterCopiesWithoutChangingOutputFragment() {
        when(eventMapper.selectByIdForUpdate(176L)).thenReturn(eventWithBusinessDetails());
        when(fragmentMapper.selectListByEventIdForUpdate(176L)).thenReturn(List.of(fragment()));
        when(signatureService.recordFieldChangeSignature(any())).thenReturn(newSignature());
        when(revisionService.updateProductionReportRecord(any())).thenReturn(702L);
        MesProcessPoolProductionReportCorrectionCommand command = command()
                .setOutputQuantity(new BigDecimal("4"))
                .setLossDetails(List.of(new MesProcessPoolProductionReportCorrectionCommand.LossDetailCommand()
                        .setReasonId(8301L)
                        .setQuantity(new BigDecimal("3"))))
                .setDeviceParameterReadings(List.of(
                        new MesProcessPoolProductionReportCorrectionCommand.DeviceParameterReadingCommand()
                                .setDeviceId(41L)
                                .setParameterCode("pressure")
                                .setValue(new BigDecimal("25"))));

        assertEquals(702L, service.correct(command));

        ArgumentCaptor<MesProcessPoolEventRevisionUpdateReqBO> revisionCaptor =
                ArgumentCaptor.forClass(MesProcessPoolEventRevisionUpdateReqBO.class);
        verify(revisionService).updateProductionReportRecord(revisionCaptor.capture());
        MesProcessPoolEventRevisionUpdateReqBO revision = revisionCaptor.getValue();
        assertEquals(3, revision.getChangedFields().size());
        assertEquals("3", revision.getChangedFields().stream()
                .filter(field -> "SCRAP_QUANTITY".equals(field.getFieldCode()))
                .findFirst().orElseThrow().getAfterValue());
        assertEquals("损耗原因：正常损耗", revision.getChangedFields().stream()
                .filter(field -> "LOSS_REASON.8301".equals(field.getFieldCode()))
                .findFirst().orElseThrow().getFieldName());
        assertEquals("25", revision.getChangedFields().stream()
                .filter(field -> "DEVICE_PARAMETERS.pressure".equals(field.getFieldCode()))
                .findFirst().orElseThrow().getAfterValue());
        org.junit.jupiter.api.Assertions.assertTrue(
                revision.getAfterPayload().contains("\"lossQuantity\":3"));
        org.junit.jupiter.api.Assertions.assertTrue(
                revision.getAfterPayload().contains("\"pressure\":25"));
        verify(fragmentMapper, never()).updateById(any(MesProProcessPoolQuantityFragmentDO.class));
    }

    @Test
    void correctsDeviceParameterReadingWhenEquipmentParameterCopiesAreMissing() {
        when(eventMapper.selectByIdForUpdate(176L)).thenReturn(eventWithDeviceReadingsButMissingParameterCopies());
        when(fragmentMapper.selectListByEventIdForUpdate(176L)).thenReturn(List.of(fragment()));
        when(signatureService.recordFieldChangeSignature(any())).thenReturn(newSignature());
        when(revisionService.updateProductionReportRecord(any())).thenReturn(704L);
        MesProcessPoolProductionReportCorrectionCommand command = command()
                .setOutputQuantity(new BigDecimal("4"))
                .setLossDetails(List.of())
                .setDeviceParameterReadings(List.of(
                        new MesProcessPoolProductionReportCorrectionCommand.DeviceParameterReadingCommand()
                                .setDeviceId(41L)
                                .setParameterCode("pressure")
                                .setValue(new BigDecimal("25"))));

        assertEquals(704L, service.correct(command));

        ArgumentCaptor<MesProcessPoolEventRevisionUpdateReqBO> revisionCaptor =
                ArgumentCaptor.forClass(MesProcessPoolEventRevisionUpdateReqBO.class);
        verify(revisionService).updateProductionReportRecord(revisionCaptor.capture());
        MesProcessPoolEventRevisionUpdateReqBO revision = revisionCaptor.getValue();
        assertEquals(1, revision.getChangedFields().size());
        assertEquals("DEVICE_PARAMETERS.pressure", revision.getChangedFields().get(0).getFieldCode());
        assertEquals("20", revision.getChangedFields().get(0).getBeforeValue());
        assertEquals("25", revision.getChangedFields().get(0).getAfterValue());
        org.junit.jupiter.api.Assertions.assertTrue(
                revision.getAfterPayload().contains("\"equipmentParameters\":{\"球囊成型机\":{\"pressure\":25}}"));
        org.junit.jupiter.api.Assertions.assertTrue(
                revision.getAfterPayload().contains("\"DEVICE_PARAMETERS\":{\"球囊成型机\":{\"pressure\":25}}"));
        verify(fragmentMapper, never()).updateById(any(MesProProcessPoolQuantityFragmentDO.class));
    }

    @Test
    void correctsOutputWhenOriginalDeviceParameterValueIsMissing() {
        when(eventMapper.selectByIdForUpdate(176L)).thenReturn(eventWithMissingDeviceParameterValue());
        when(fragmentMapper.selectListByEventIdForUpdate(176L)).thenReturn(List.of(fragment()));
        when(signatureService.recordFieldChangeSignature(any())).thenReturn(newSignature());
        when(revisionService.updateProductionReportRecord(any())).thenReturn(705L);
        when(fragmentMapper.updateById(any(MesProProcessPoolQuantityFragmentDO.class))).thenReturn(1);
        MesProcessPoolProductionReportCorrectionCommand command = command()
                .setDeviceParameterReadings(List.of(
                        new MesProcessPoolProductionReportCorrectionCommand.DeviceParameterReadingCommand()
                                .setDeviceId(41L)
                                .setParameterCode("pressure")));

        assertEquals(705L, service.correct(command));

        ArgumentCaptor<MesProcessPoolEventRevisionUpdateReqBO> revisionCaptor =
                ArgumentCaptor.forClass(MesProcessPoolEventRevisionUpdateReqBO.class);
        verify(revisionService).updateProductionReportRecord(revisionCaptor.capture());
        MesProcessPoolEventRevisionUpdateReqBO revision = revisionCaptor.getValue();
        assertEquals(1, revision.getChangedFields().size());
        assertEquals("OUTPUT_QUANTITY", revision.getChangedFields().get(0).getFieldCode());
        org.junit.jupiter.api.Assertions.assertTrue(revision.getAfterPayload().contains("\"value\":null"));
        org.junit.jupiter.api.Assertions.assertFalse(revision.getAfterPayload().contains("\"value\":0"));
    }

    @Test
    void correctsMissingOriginalDeviceParameterValueWhenLeaderProvidesOne() {
        when(eventMapper.selectByIdForUpdate(176L)).thenReturn(eventWithMissingDeviceParameterValue());
        when(fragmentMapper.selectListByEventIdForUpdate(176L)).thenReturn(List.of(fragment()));
        when(signatureService.recordFieldChangeSignature(any())).thenReturn(newSignature());
        when(revisionService.updateProductionReportRecord(any())).thenReturn(706L);
        MesProcessPoolProductionReportCorrectionCommand command = command()
                .setOutputQuantity(new BigDecimal("4"))
                .setDeviceParameterReadings(List.of(
                        new MesProcessPoolProductionReportCorrectionCommand.DeviceParameterReadingCommand()
                                .setDeviceId(41L)
                                .setParameterCode("pressure")
                                .setValue(new BigDecimal("25"))));

        assertEquals(706L, service.correct(command));

        ArgumentCaptor<MesProcessPoolEventRevisionUpdateReqBO> revisionCaptor =
                ArgumentCaptor.forClass(MesProcessPoolEventRevisionUpdateReqBO.class);
        verify(revisionService).updateProductionReportRecord(revisionCaptor.capture());
        MesProcessPoolEventRevisionUpdateReqBO revision = revisionCaptor.getValue();
        assertEquals(1, revision.getChangedFields().size());
        assertEquals("DEVICE_PARAMETERS.pressure", revision.getChangedFields().get(0).getFieldCode());
        assertEquals("--", revision.getChangedFields().get(0).getBeforeValue());
        assertEquals("25", revision.getChangedFields().get(0).getAfterValue());
        org.junit.jupiter.api.Assertions.assertTrue(revision.getAfterPayload().contains("\"value\":25"));
    }

    @Test
    void correctsSelectDeviceParameterTextValue() {
        when(eventMapper.selectByIdForUpdate(176L)).thenReturn(eventWithSelectDeviceParameterValue());
        when(fragmentMapper.selectListByEventIdForUpdate(176L)).thenReturn(List.of(fragment()));
        when(signatureService.recordFieldChangeSignature(any())).thenReturn(newSignature());
        when(revisionService.updateProductionReportRecord(any())).thenReturn(707L);
        MesProcessPoolProductionReportCorrectionCommand command = command()
                .setOutputQuantity(new BigDecimal("4"))
                .setDeviceParameterReadings(List.of(
                        new MesProcessPoolProductionReportCorrectionCommand.DeviceParameterReadingCommand()
                                .setDeviceId(41L)
                                .setParameterCode("medium")
                                .setTextValue("纯化水")));

        assertEquals(707L, service.correct(command));

        ArgumentCaptor<MesProcessPoolEventRevisionUpdateReqBO> revisionCaptor =
                ArgumentCaptor.forClass(MesProcessPoolEventRevisionUpdateReqBO.class);
        verify(revisionService).updateProductionReportRecord(revisionCaptor.capture());
        MesProcessPoolEventRevisionUpdateReqBO revision = revisionCaptor.getValue();
        assertEquals(1, revision.getChangedFields().size());
        assertEquals("DEVICE_PARAMETERS.medium", revision.getChangedFields().get(0).getFieldCode());
        assertEquals("自来水", revision.getChangedFields().get(0).getBeforeValue());
        assertEquals("纯化水", revision.getChangedFields().get(0).getAfterValue());
        org.junit.jupiter.api.Assertions.assertTrue(revision.getAfterPayload().contains("\"textValue\":\"纯化水\""));
        org.junit.jupiter.api.Assertions.assertTrue(revision.getAfterPayload().contains("\"value\":\"纯化水\""));
        org.junit.jupiter.api.Assertions.assertTrue(
                revision.getAfterPayload().contains("\"equipmentParameters\":{\"球囊成型机\":{\"medium\":\"纯化水\"}}"));
        verify(fragmentMapper, never()).updateById(any(MesProProcessPoolQuantityFragmentDO.class));
    }

    @Test
    void recordsLossReasonChangesAsReadableRowsInsteadOfJson() {
        when(eventMapper.selectByIdForUpdate(176L)).thenReturn(eventWithBusinessDetails());
        when(fragmentMapper.selectListByEventIdForUpdate(176L)).thenReturn(List.of(fragment()));
        when(lossReasonValidator.requireEnabledLossReason(928611L, 8302L, new BigDecimal("2")))
                .thenReturn(new cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesFrontlineLossReasonSnapshot(
                        8302L, "LOSS-02", "设备故障"));
        when(signatureService.recordFieldChangeSignature(any())).thenReturn(newSignature());
        when(revisionService.updateProductionReportRecord(any())).thenReturn(703L);
        MesProcessPoolProductionReportCorrectionCommand command = command()
                .setOutputQuantity(new BigDecimal("4"))
                .setLossDetails(List.of(new MesProcessPoolProductionReportCorrectionCommand.LossDetailCommand()
                        .setReasonId(8302L)
                        .setQuantity(new BigDecimal("2"))))
                .setDeviceParameterReadings(List.of(
                        new MesProcessPoolProductionReportCorrectionCommand.DeviceParameterReadingCommand()
                                .setDeviceId(41L)
                                .setParameterCode("pressure")
                                .setValue(new BigDecimal("20"))));

        assertEquals(703L, service.correct(command));

        ArgumentCaptor<MesProcessPoolEventRevisionUpdateReqBO> revisionCaptor =
                ArgumentCaptor.forClass(MesProcessPoolEventRevisionUpdateReqBO.class);
        verify(revisionService).updateProductionReportRecord(revisionCaptor.capture());
        List<MesProcessPoolEventRevisionFieldChangeBO> changes = revisionCaptor.getValue().getChangedFields();
        assertEquals(2, changes.size());
        assertEquals("损耗原因：正常损耗", changes.get(0).getFieldName());
        assertEquals("2", changes.get(0).getBeforeValue());
        assertEquals("0", changes.get(0).getAfterValue());
        assertEquals("损耗原因：设备故障", changes.get(1).getFieldName());
        assertEquals("0", changes.get(1).getBeforeValue());
        assertEquals("2", changes.get(1).getAfterValue());
        org.junit.jupiter.api.Assertions.assertTrue(changes.stream()
                .noneMatch(item -> item.getBeforeValue().startsWith("[") || item.getAfterValue().startsWith("[")));
    }

    @Test
    void correctsMaterialDetailsWithoutLosingMaterialIdentity() {
        when(eventMapper.selectByIdForUpdate(176L)).thenReturn(eventWithBusinessDetails());
        when(fragmentMapper.selectListByEventIdForUpdate(176L)).thenReturn(List.of(fragment()));
        when(signatureService.recordFieldChangeSignature(any())).thenReturn(newSignature());
        when(revisionService.updateProductionReportRecord(any())).thenReturn(707L);
        MesProcessPoolProductionReportCorrectionCommand command = command()
                .setOutputQuantity(new BigDecimal("4"))
                .setLossDetails(List.of(new MesProcessPoolProductionReportCorrectionCommand.LossDetailCommand()
                        .setReasonId(8301L)
                        .setQuantity(new BigDecimal("1"))))
                .setMaterialDetails(List.of(
                        new MesProcessPoolProductionReportCorrectionCommand.MaterialDetailCommand()
                                .setMaterialId(3401L)
                                .setOutputQuantity(new BigDecimal("3"))
                                .setLossQuantity(new BigDecimal("1"))
                                .setLossDetails(List.of(new MesProcessPoolProductionReportCorrectionCommand
                                        .LossDetailCommand()
                                        .setReasonId(8301L)
                                        .setQuantity(new BigDecimal("1")))),
                        new MesProcessPoolProductionReportCorrectionCommand.MaterialDetailCommand()
                                .setMaterialId(4801L)
                                .setOutputQuantity(new BigDecimal("1"))
                                .setLossQuantity(BigDecimal.ZERO)));

        assertEquals(707L, service.correct(command));

        ArgumentCaptor<MesProcessPoolEventRevisionUpdateReqBO> revisionCaptor =
                ArgumentCaptor.forClass(MesProcessPoolEventRevisionUpdateReqBO.class);
        verify(revisionService).updateProductionReportRecord(revisionCaptor.capture());
        MesProcessPoolEventRevisionUpdateReqBO revision = revisionCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertTrue(revision.getAfterPayload()
                .contains("\"materialId\":3401,\"materialCode\":\"A001.02.034.202\",\"materialName\":\"弹簧\",\"outputQuantity\":3,\"lossQuantity\":1"));
        org.junit.jupiter.api.Assertions.assertTrue(revision.getAfterPayload()
                .contains("\"materialId\":4801,\"materialCode\":\"A001.02.048.102\",\"materialName\":\"杠杆\",\"outputQuantity\":1,\"lossQuantity\":0"));
        assertEquals("3", revision.getChangedFields().stream()
                .filter(field -> "MATERIAL_OUTPUT.3401".equals(field.getFieldCode()))
                .findFirst().orElseThrow().getAfterValue());
        assertEquals("1", revision.getChangedFields().stream()
                .filter(field -> "MATERIAL_LOSS.3401".equals(field.getFieldCode()))
                .findFirst().orElseThrow().getAfterValue());
        verify(fragmentMapper, never()).updateById(any(MesProProcessPoolQuantityFragmentDO.class));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {0, 1, 2})
    void correctsDeviceParameterWithSameLossReasonOnTwoMaterials(int materialLoss) throws Exception {
        MesProProcessPoolEventDO source = eventWithBusinessDetails();
        com.fasterxml.jackson.databind.node.ObjectNode payload =
                (com.fasterxml.jackson.databind.node.ObjectNode) cn.iocoder.yudao.framework.common.util.json.JsonUtils
                        .getObjectMapper().readTree(source.getRawPayload());
        com.fasterxml.jackson.databind.node.ArrayNode losses =
                (com.fasterxml.jackson.databind.node.ArrayNode) payload.get("lossDetails");
        ((com.fasterxml.jackson.databind.node.ObjectNode) losses.get(0)).put("quantity", 1);
        losses.add(losses.get(0).deepCopy());
        payload.set("lossReasonDetails", losses.deepCopy());
        for (com.fasterxml.jackson.databind.JsonNode material : payload.get("materialDetails")) {
            com.fasterxml.jackson.databind.node.ObjectNode fact = (com.fasterxml.jackson.databind.node.ObjectNode) material;
            fact.put("lossQuantity", 1);
            fact.set("lossDetails", losses.arrayNode().add(losses.get(0).deepCopy()));
        }
        String original = payload.toString();
        source.setRawPayload(original);
        when(eventMapper.selectByIdForUpdate(176L)).thenReturn(source);
        when(fragmentMapper.selectListByEventIdForUpdate(176L)).thenReturn(List.of(fragment()));
        when(signatureService.recordFieldChangeSignature(any())).thenReturn(newSignature());
        when(revisionService.updateProductionReportRecord(any())).thenAnswer(invocation -> {
            assertEquals(original, source.getRawPayload(), "Original facts must survive until revision capture");
            return 708L;
        });
        List<MesProcessPoolProductionReportCorrectionCommand.LossDetailCommand> materialReasons = materialLoss == 0
                ? List.of() : List.of(new MesProcessPoolProductionReportCorrectionCommand.LossDetailCommand()
                        .setReasonId(8301L).setQuantity(BigDecimal.valueOf(materialLoss)));
        MesProcessPoolProductionReportCorrectionCommand request = command().setOutputQuantity(new BigDecimal("4"))
                .setLossDetails(materialLoss == 0 ? List.of() : List.of(
                        new MesProcessPoolProductionReportCorrectionCommand.LossDetailCommand()
                                .setReasonId(8301L).setQuantity(BigDecimal.valueOf(2L * materialLoss))))
                .setMaterialDetails(List.of(
                        new MesProcessPoolProductionReportCorrectionCommand.MaterialDetailCommand()
                                .setMaterialId(3401L).setOutputQuantity(new BigDecimal("2"))
                                .setLossQuantity(BigDecimal.valueOf(materialLoss)).setLossDetails(materialReasons),
                        new MesProcessPoolProductionReportCorrectionCommand.MaterialDetailCommand()
                                .setMaterialId(4801L).setOutputQuantity(new BigDecimal("2"))
                                .setLossQuantity(BigDecimal.valueOf(materialLoss)).setLossDetails(materialReasons)))
                .setDeviceParameterReadings(List.of(
                        new MesProcessPoolProductionReportCorrectionCommand.DeviceParameterReadingCommand()
                                .setDeviceId(41L).setParameterCode("pressure").setValue(new BigDecimal("21"))));

        assertEquals(708L, service.correct(request));

        ArgumentCaptor<MesProcessPoolEventRevisionUpdateReqBO> captor =
                ArgumentCaptor.forClass(MesProcessPoolEventRevisionUpdateReqBO.class);
        verify(revisionService).updateProductionReportRecord(captor.capture());
        com.fasterxml.jackson.databind.JsonNode after = cn.iocoder.yudao.framework.common.util.json.JsonUtils
                .getObjectMapper().readTree(captor.getValue().getAfterPayload());
        assertEquals(materialLoss == 0 ? 0 : 1, after.get("lossDetails").size());
        assertEquals(2 * materialLoss, after.get("lossQuantity").intValue());
        for (int index = 0; index < 2; index++) {
            assertEquals(payload.get("materialDetails").get(index).get("materialId"),
                    after.get("materialDetails").get(index).get("materialId"));
            assertEquals(materialLoss, after.get("materialDetails").get(index).get("lossQuantity").intValue());
            assertEquals(materialLoss == 0 ? 0 : 1,
                    after.get("materialDetails").get(index).get("lossDetails").size());
        }
        if (materialLoss == 1) {
            for (int index = 0; index < 2; index++) {
                assertEquals(payload.get("materialDetails").get(index).get("lossDetails"),
                        after.get("materialDetails").get(index).get("lossDetails"));
                assertEquals(payload.get("materialDetails").get(index).get("materialName"),
                        after.get("materialDetails").get(index).get("materialName"));
            }
            assertFalse(captor.getValue().getChangedFields().stream()
                    .anyMatch(field -> field.getFieldCode().startsWith("LOSS_REASON.")));
        } else {
            MesProcessPoolEventRevisionFieldChangeBO reasonChange = captor.getValue().getChangedFields().stream()
                    .filter(field -> "LOSS_REASON.8301".equals(field.getFieldCode())).findFirst().orElseThrow();
            assertEquals("2", reasonChange.getBeforeValue());
            assertEquals(String.valueOf(2 * materialLoss), reasonChange.getAfterValue());
        }
    }

    @Test
    void rejectsConflictingSnapshotsForSameLossReasonBeforeSigning() throws Exception {
        MesProProcessPoolEventDO source = eventWithBusinessDetails();
        com.fasterxml.jackson.databind.node.ObjectNode payload =
                (com.fasterxml.jackson.databind.node.ObjectNode) cn.iocoder.yudao.framework.common.util.json.JsonUtils
                        .getObjectMapper().readTree(source.getRawPayload());
        com.fasterxml.jackson.databind.node.ArrayNode losses =
                (com.fasterxml.jackson.databind.node.ArrayNode) payload.get("lossDetails");
        com.fasterxml.jackson.databind.node.ObjectNode conflicting =
                ((com.fasterxml.jackson.databind.node.ObjectNode) losses.get(0)).deepCopy();
        conflicting.put("reasonName", "不同原因快照");
        losses.add(conflicting);
        source.setRawPayload(payload.toString());
        when(eventMapper.selectByIdForUpdate(176L)).thenReturn(source);
        when(fragmentMapper.selectListByEventIdForUpdate(176L)).thenReturn(List.of(fragment()));

        cn.iocoder.yudao.framework.common.exception.ServiceException error = assertThrows(
                cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> service.correct(command().setOutputQuantity(new BigDecimal("4"))));
        org.junit.jupiter.api.Assertions.assertTrue(error.getMessage().contains("lossDetails.reasonMetadata"));
        verify(signatureService, never()).recordFieldChangeSignature(any());
        verify(revisionService, never()).updateProductionReportRecord(any());
        verify(feedbackMapper, never()).updateCorrectedProductionReport(
                any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void ownReturnCorrectsBothMaterialAndAggregateParameterCopiesWithOneSignedDiff() {
        var original = eventWithDuplicateParameterCopies(false);
        String originalPayload = original.getRawPayload();
        var rejected = cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO
                .builder().id(91L).eventId(176L).leaderType("PRODUCTION").reviewStatus("REJECTED").build();
        var context = new MesFrontlineReturnCorrectionService.ReturnContext(original, rejected, 413L, 0L,
                "1900000000000000001");
        var own = org.mockito.Mockito.mock(MesFrontlineReturnCorrectionService.class);
        ReflectionTestUtils.setField(service, "ownReturnService", own);
        when(own.requireOwnReturned(176L, 413L, 91L, 0L, 3001L, "PRODUCTION")).thenReturn(context);
        when(eventMapper.selectByIdForUpdate(176L)).thenReturn(original);
        when(fragmentMapper.selectListByEventIdForUpdate(176L)).thenReturn(List.of(fragment()));
        when(signatureService.recordFieldChangeSignature(any())).thenReturn(newSignature());
        when(revisionService.updateProductionReportRecord(any())).thenAnswer(invocation -> {
            assertEquals(originalPayload, original.getRawPayload(), "Original facts survive until signed revision capture");
            return 701L;
        });
        var result = new MesFrontlineReturnCorrectionService.CorrectionResult(176L, 701L, List.of(
                new MesFrontlineReturnCorrectionService.ChangedField("清洗次数", "2", "3")));
        when(own.complete(context, 701L, 3001L)).thenReturn(result);
        var request = duplicateParameterCommand(false);

        assertEquals(result, service.correctOwnReturned(request, 413L, 91L, 0L));

        var captured = ArgumentCaptor.forClass(MesProcessPoolEventRevisionUpdateReqBO.class);
        verify(revisionService).updateProductionReportRecord(captured.capture());
        var revision = captured.getValue();
        var after = JsonUtils.parseTree(revision.getAfterPayload());
        assertEquals(2, after.path("deviceParameterReadings").size());
        for (var reading : after.path("deviceParameterReadings")) {
            assertEquals(0, new BigDecimal("3").compareTo(reading.path("value").decimalValue()));
            assertEquals("NORMAL", reading.path("parameterStatus").textValue());
        }
        for (var material : after.path("materialDetails")) {
            assertEquals(2, material.path("outputQuantity").intValue());
            assertEquals(0, material.path("lossQuantity").intValue());
            assertEquals(3, material.path("deviceParameterReadings").get(0).path("value").intValue());
        }
        assertEquals(3, after.path("equipmentParameters").path("清洗机").path("cleaningCount").intValue());
        assertEquals(3, after.path("fieldValues").path("DEVICE_PARAMETERS").path("清洗机")
                .path("cleaningCount").intValue());
        assertEquals(4, after.path("outputQuantity").intValue());
        assertEquals(91L, after.path("supersededReviewId").longValue());
        assertEquals(1, revision.getChangedFields().size());
        assertEquals("DEVICE_PARAMETERS.cleaningCount", revision.getChangedFields().get(0).getFieldCode());
        assertEquals("2", revision.getChangedFields().get(0).getBeforeValue());
        assertEquals("3", revision.getChangedFields().get(0).getAfterValue());
        assertEquals(9102L, revision.getRevisionSignatureId());
        assertEquals(3001L, revision.getRevisionSignatureUserId());
        assertEquals(9001L, original.getSignatureId());
        assertEquals(964L, original.getSignatureUserId());
        assertEquals(2, JsonUtils.parseTree(originalPayload).path("deviceParameterReadings").get(1).path("value").intValue());
        assertEquals(revision.getAfterPayload(), original.getRawPayload(), "Current reader must expose the signed correction");
        var signed = ArgumentCaptor.forClass(MesProBatchRecordExecutionFieldAuditSignatureCommand.class);
        verify(signatureService).recordFieldChangeSignature(signed.capture());
        assertEquals(cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHasher
                .sha256("176|" + cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHasher
                        .canonicalizeJsonString(revision.getAfterPayload()) + "|" + request.getChangeReason().trim()),
                signed.getValue().getSignatureChallengeHash());
        verify(fragmentMapper, never()).updateById(any(MesProProcessPoolQuantityFragmentDO.class));
        org.mockito.Mockito.verifyNoInteractions(scopeService);
        verify(own).complete(context, 701L, 3001L);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void ownReturnAcceptsEquivalentPersistedJsonAndKeepsCanonicalSignedChallenge(boolean pretty) throws Exception {
        var original = eventWithDuplicateParameterCopies(false);
        var rejected = cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO
                .builder().id(91L).eventId(176L).leaderType("PRODUCTION").reviewStatus("REJECTED").build();
        var context = new MesFrontlineReturnCorrectionService.ReturnContext(original, rejected, 413L, 0L,
                "1900000000000000001");
        var own = org.mockito.Mockito.mock(MesFrontlineReturnCorrectionService.class);
        ReflectionTestUtils.setField(service, "ownReturnService", own);
        when(own.requireOwnReturned(176L, 413L, 91L, 0L, 3001L, "PRODUCTION")).thenReturn(context);
        String[] persistedJson = new String[1];
        when(eventMapper.selectByIdForUpdate(176L)).thenReturn(original).thenAnswer(invocation -> {
            var reordered = reverseObjectKeys(JsonUtils.parseTree(original.getRawPayload()));
            persistedJson[0] = pretty ? JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(reordered)
                    : reordered.toString();
            return event().setRawPayload(persistedJson[0]).setReportOutputQuantity(original.getReportOutputQuantity());
        });
        when(fragmentMapper.selectListByEventIdForUpdate(176L)).thenReturn(List.of(fragment()));
        when(signatureService.recordFieldChangeSignature(any())).thenReturn(newSignature());
        when(revisionService.updateProductionReportRecord(any())).thenReturn(701L);
        var result = new MesFrontlineReturnCorrectionService.CorrectionResult(176L, 701L, List.of(
                new MesFrontlineReturnCorrectionService.ChangedField("清洗次数", "2", "3")));
        when(own.complete(context, 701L, 3001L)).thenReturn(result);
        var request = duplicateParameterCommand(false);

        assertEquals(result, service.correctOwnReturned(request, 413L, 91L, 0L));

        MesProcessPoolEventRevisionUpdateReqBO revision = lastArgument(revisionService, "updateProductionReportRecord");
        org.junit.jupiter.api.Assertions.assertNotEquals(revision.getAfterPayload(), persistedJson[0]);
        assertEquals(JsonUtils.parseTree(revision.getAfterPayload()), JsonUtils.parseTree(persistedJson[0]));
        var signed = ArgumentCaptor.forClass(MesProBatchRecordExecutionFieldAuditSignatureCommand.class);
        verify(signatureService).recordFieldChangeSignature(signed.capture());
        String challenge = cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHasher.sha256(
                "176|" + cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHasher
                        .canonicalizeJsonString(persistedJson[0]) + "|" + request.getChangeReason().trim());
        assertEquals(challenge, signed.getValue().getSignatureChallengeHash());
        var audit = (GxpAuditService) ReflectionTestUtils.getField(service, "gxpAuditService");
        var command = ArgumentCaptor.forClass(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand.class);
        verify(audit).append(command.capture());
        assertEquals(challenge, command.getValue().getEvidences().stream()
                .filter(e -> "SIGNATURE_CHALLENGE".equals(e.evidenceType())).findFirst().orElseThrow().sha256());
        assertEquals(9001L, original.getSignatureId());
        verify(fragmentMapper, never()).updateById(any(MesProProcessPoolQuantityFragmentDO.class));
        verify(own).complete(context, 701L, 3001L);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"VALUE", "TYPE", "MISSING_FIELD", "ADDED_FIELD",
            "ARRAY_ORDER", "NULL_FIELD", "NON_OBJECT", "INVALID_JSON"})
    void ownReturnRejectsPersistedJsonFactChangesBeforeAuditAndTaskCompletion(String change) {
        var original = eventWithDuplicateParameterCopies(false);
        var rejected = cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO
                .builder().id(91L).eventId(176L).leaderType("PRODUCTION").reviewStatus("REJECTED").build();
        var context = new MesFrontlineReturnCorrectionService.ReturnContext(original, rejected, 413L, 0L,
                "1900000000000000001");
        var own = org.mockito.Mockito.mock(MesFrontlineReturnCorrectionService.class);
        ReflectionTestUtils.setField(service, "ownReturnService", own);
        when(own.requireOwnReturned(176L, 413L, 91L, 0L, 3001L, "PRODUCTION")).thenReturn(context);
        when(eventMapper.selectByIdForUpdate(176L)).thenReturn(original).thenAnswer(invocation -> {
            var tree = (com.fasterxml.jackson.databind.node.ObjectNode) JsonUtils.parseTree(original.getRawPayload());
            var reading = (com.fasterxml.jackson.databind.node.ObjectNode) tree.path("deviceParameterReadings").get(0);
            switch (change) {
                case "VALUE" -> reading.put("value", 4);
                case "TYPE" -> reading.put("value", "3");
                case "MISSING_FIELD" -> tree.remove("supersededReviewId");
                case "ADDED_FIELD" -> tree.put("unrequestedFact", true);
                case "ARRAY_ORDER" -> {
                    var materials = (com.fasterxml.jackson.databind.node.ArrayNode) tree.path("materialDetails");
                    var first = materials.remove(0); materials.add(first);
                }
                case "NULL_FIELD" -> tree.putNull("supersededReviewId");
                case "NON_OBJECT", "INVALID_JSON" -> { }
                default -> throw new AssertionError(change);
            }
            String json = "NON_OBJECT".equals(change) ? "[]" : "INVALID_JSON".equals(change) ? "{" : tree.toString();
            return event().setRawPayload(json).setReportOutputQuantity(original.getReportOutputQuantity());
        });
        when(fragmentMapper.selectListByEventIdForUpdate(176L)).thenReturn(List.of(fragment()));
        when(signatureService.recordFieldChangeSignature(any())).thenReturn(newSignature());
        when(revisionService.updateProductionReportRecord(any())).thenReturn(701L);

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.correctOwnReturned(duplicateParameterCommand(false), 413L, 91L, 0L));
        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED.getCode(), error.getCode());
        org.junit.jupiter.api.Assertions.assertTrue(error.getMessage().contains("productionCorrection.persistedPayload"));
        verify((GxpAuditService) ReflectionTestUtils.getField(service, "gxpAuditService"), never()).append(any());
        verify(own, never()).complete(any(), any(), any());
        verify(fragmentMapper, never()).updateById(any(MesProProcessPoolQuantityFragmentDO.class));
    }

    @Test
    void ownReturnRejectsDifferentPersistedRevisionBeforeAuditAndTaskCompletion() {
        var original = eventWithDuplicateParameterCopies(false);
        var rejected = cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO
                .builder().id(91L).eventId(176L).leaderType("PRODUCTION").reviewStatus("REJECTED").build();
        var context = new MesFrontlineReturnCorrectionService.ReturnContext(original, rejected, 413L, 0L,
                "1900000000000000001");
        var own = org.mockito.Mockito.mock(MesFrontlineReturnCorrectionService.class);
        ReflectionTestUtils.setField(service, "ownReturnService", own);
        when(own.requireOwnReturned(176L, 413L, 91L, 0L, 3001L, "PRODUCTION")).thenReturn(context);
        when(eventMapper.selectByIdForUpdate(176L)).thenReturn(original);
        when(fragmentMapper.selectListByEventIdForUpdate(176L)).thenReturn(List.of(fragment()));
        when(signatureService.recordFieldChangeSignature(any())).thenReturn(newSignature());
        when(revisionService.updateProductionReportRecord(any())).thenReturn(701L);
        var revisions = (MesProProcessPoolEventRevisionMapper) ReflectionTestUtils.getField(service, "revisionMapper");
        org.mockito.Mockito.doAnswer(invocation -> {
            MesProcessPoolEventRevisionUpdateReqBO input = lastArgument(revisionService, "updateProductionReportRecord");
            var changed = (com.fasterxml.jackson.databind.node.ObjectNode) JsonUtils.parseTree(input.getAfterPayload());
            changed.put("supersededReviewId", 92L);
            return MesProProcessPoolEventRevisionDO.builder().id(701L).eventId(176L)
                    .afterPayload(changed.toString()).build();
        }).when(revisions).selectOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.correctOwnReturned(duplicateParameterCommand(false), 413L, 91L, 0L));
        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED.getCode(), error.getCode());
        org.junit.jupiter.api.Assertions.assertTrue(error.getMessage().contains("productionCorrection.persistedRevision"));
        verify((GxpAuditService) ReflectionTestUtils.getField(service, "gxpAuditService"), never()).append(any());
        verify(own, never()).complete(any(), any(), any());
    }

    private static com.fasterxml.jackson.databind.JsonNode reverseObjectKeys(com.fasterxml.jackson.databind.JsonNode node) {
        if (node.isObject()) {
            var object = JsonUtils.getObjectMapper().createObjectNode();
            var keys = new java.util.ArrayList<String>();
            node.fieldNames().forEachRemaining(keys::add);
            keys.sort(java.util.Comparator.reverseOrder());
            keys.forEach(key -> object.set(key, reverseObjectKeys(node.get(key))));
            return object;
        }
        if (node.isArray()) {
            var array = JsonUtils.getObjectMapper().createArrayNode();
            node.forEach(value -> array.add(reverseObjectKeys(value)));
            return array;
        }
        return node.deepCopy();
    }

    @Test
    void correctsEquivalentDuplicateTextRequestsAndEveryAggregateCopyWithOneDiff() {
        var original = eventWithDuplicateParameterCopies(true);
        when(eventMapper.selectByIdForUpdate(176L)).thenReturn(original);
        when(fragmentMapper.selectListByEventIdForUpdate(176L)).thenReturn(List.of(fragment()));
        when(signatureService.recordFieldChangeSignature(any())).thenReturn(newSignature());
        when(revisionService.updateProductionReportRecord(any())).thenReturn(701L);

        assertEquals(701L, service.correct(duplicateParameterCommand(true)));

        var captured = ArgumentCaptor.forClass(MesProcessPoolEventRevisionUpdateReqBO.class);
        verify(revisionService).updateProductionReportRecord(captured.capture());
        var revision = captured.getValue();
        for (var reading : JsonUtils.parseTree(revision.getAfterPayload()).path("deviceParameterReadings")) {
            assertEquals("纯化水", reading.path("value").textValue());
            assertEquals("纯化水", reading.path("textValue").textValue());
        }
        assertEquals(1, revision.getChangedFields().size());
        assertEquals("自来水", revision.getChangedFields().get(0).getBeforeValue());
        assertEquals("纯化水", revision.getChangedFields().get(0).getAfterValue());
        assertEquals(9001L, original.getSignatureId());
        verify(fragmentMapper, never()).updateById(any(MesProProcessPoolQuantityFragmentDO.class));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"NUMERIC", "TEXT", "MIXED"})
    void rejectsContradictoryDuplicateParameterRequestsBeforeSignatureAndBusinessWrites(String conflict) {
        var original = eventWithDuplicateParameterCopies("TEXT".equals(conflict));
        var request = duplicateParameterCommand("TEXT".equals(conflict));
        var second = request.getDeviceParameterReadings().get(1);
        if ("NUMERIC".equals(conflict)) {
            second.setValue(new BigDecimal("4"));
        } else {
            second.setTextValue("冲突值");
        }
        when(eventMapper.selectByIdForUpdate(176L)).thenReturn(original);
        when(fragmentMapper.selectListByEventIdForUpdate(176L)).thenReturn(List.of(fragment()));

        ServiceException error = assertThrows(ServiceException.class, () -> service.correct(request));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED.getCode(), error.getCode());
        org.junit.jupiter.api.Assertions.assertTrue(error.getMessage().contains("deviceParameterReadings.requestValue"));
        verifyNoParameterCorrectionWrites();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void rejectsContradictoryOriginalAggregateParameterValuesBeforeSigning(boolean text) {
        var original = eventWithDuplicateParameterCopies(text);
        var payload = (com.fasterxml.jackson.databind.node.ObjectNode) JsonUtils.parseTree(original.getRawPayload());
        var second = (com.fasterxml.jackson.databind.node.ObjectNode) payload.path("deviceParameterReadings").get(1);
        if (text) {
            second.put("value", "其他介质").put("textValue", "其他介质");
        } else {
            second.put("value", 5);
        }
        original.setRawPayload(payload.toString());
        when(eventMapper.selectByIdForUpdate(176L)).thenReturn(original);
        when(fragmentMapper.selectListByEventIdForUpdate(176L)).thenReturn(List.of(fragment()));

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.correct(duplicateParameterCommand(text)));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED.getCode(), error.getCode());
        org.junit.jupiter.api.Assertions.assertTrue(error.getMessage().contains("deviceParameterReadings.originalValue"));
        verifyNoParameterCorrectionWrites();
    }

    private void verifyNoParameterCorrectionWrites() {
        verify(signatureService, never()).recordFieldChangeSignature(any());
        verify(revisionService, never()).updateProductionReportRecord(any());
        verify(fragmentMapper, never()).updateById(any(MesProProcessPoolQuantityFragmentDO.class));
        verify(eventMapper, never()).updateById(any(MesProProcessPoolEventDO.class));
        verify(feedbackMapper, never()).updateCorrectedProductionReport(any(), any(), any(), any(), any(), any(), any());
        verify(feedbackMaterialMapper, never()).updateCorrectedMaterialFact(any(), any(), any(), any(), any(), any());
        verify(reportManagementSummaryService, never()).refreshProductionEvent(any());
    }

    private static MesProProcessPoolEventDO eventWithDuplicateParameterCopies(boolean text) {
        var payload = (com.fasterxml.jackson.databind.node.ObjectNode) JsonUtils.parseTree(event().getRawPayload());
        var reading = JsonUtils.getObjectMapper().createObjectNode().put("deviceId", 41L).put("deviceName", "清洗机")
                .put("parameterCode", text ? "medium" : "cleaningCount").put("parameterName", text ? "清洗介质" : "清洗次数")
                .put("parameterStatus", "NORMAL");
        if (text) {
            reading.put("value", "自来水").put("textValue", "自来水");
        } else {
            reading.put("value", 2).put("lowerLimit", 1);
        }
        payload.set("deviceParameterReadings", JsonUtils.getObjectMapper().createArrayNode()
                .add(reading.deepCopy()).add(reading.deepCopy()));
        var materials = payload.putArray("materialDetails");
        for (Long materialId : List.of(3401L, 4801L)) {
            var material = materials.addObject().put("materialId", materialId).put("outputQuantity", 2).put("lossQuantity", 0);
            material.putArray("lossDetails");
            material.putArray("deviceParameterReadings").add(reading.deepCopy());
        }
        return event().setRawPayload(payload.toString());
    }

    private static MesProcessPoolProductionReportCorrectionCommand duplicateParameterCommand(boolean text) {
        var first = new MesProcessPoolProductionReportCorrectionCommand.DeviceParameterReadingCommand()
                .setDeviceId(41L).setParameterCode(text ? "medium" : "cleaningCount");
        var second = new MesProcessPoolProductionReportCorrectionCommand.DeviceParameterReadingCommand()
                .setDeviceId(41L).setParameterCode(text ? "medium" : "cleaningCount");
        if (text) {
            first.setTextValue("纯化水");
            second.setTextValue(" 纯化水 ");
        } else {
            first.setValue(new BigDecimal("3"));
            second.setValue(new BigDecimal("3.0"));
        }
        return command().setOutputQuantity(new BigDecimal("4")).setDeviceParameterReadings(List.of(first, second))
                .setMaterialDetails(List.of(
                        new MesProcessPoolProductionReportCorrectionCommand.MaterialDetailCommand().setMaterialId(3401L)
                                .setOutputQuantity(new BigDecimal("2")).setLossQuantity(BigDecimal.ZERO)
                                .setLossDetails(List.of()).setDeviceParameterReadings(List.of(first)),
                        new MesProcessPoolProductionReportCorrectionCommand.MaterialDetailCommand().setMaterialId(4801L)
                                .setOutputQuantity(new BigDecimal("2")).setLossQuantity(BigDecimal.ZERO)
                                .setLossDetails(List.of()).setDeviceParameterReadings(List.of(first))));
    }

    @org.junit.jupiter.api.AfterEach
    void clearAuditTenant() {
        TenantContextHolder.clear();
    }

    /** Unit boundary fixtures only. Actual persisted snapshots/atomicity are covered by AuditTransactionTest. */
    private void initializeAuditFixture() {
        TenantContextHolder.setTenantId(1L);
        ReflectionTestUtils.setField(service, "gxpAuditService", org.mockito.Mockito.mock(GxpAuditService.class));
        var revisionRows = org.mockito.Mockito.mock(MesProProcessPoolEventRevisionMapper.class);
        var diffRows = org.mockito.Mockito.mock(MesProProcessPoolEventRevisionDiffMapper.class);
        ReflectionTestUtils.setField(service, "revisionMapper", revisionRows);
        ReflectionTestUtils.setField(service, "revisionDiffMapper", diffRows);
        var configuration = new org.apache.ibatis.session.Configuration();
        var assistant = new org.apache.ibatis.builder.MapperBuilderAssistant(configuration, "correction-unit");
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(assistant, MesProProcessPoolEventRevisionDO.class);
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(assistant, MesProProcessPoolEventRevisionDiffDO.class);
        lenient().when(revisionRows.selectOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenAnswer(call -> {
            MesProcessPoolEventRevisionUpdateReqBO input = lastArgument(revisionService, "updateProductionReportRecord");
            var wrapper = call.getArgument(0, com.baomidou.mybatisplus.core.conditions.AbstractWrapper.class);
            wrapper.getSqlSegment();
            Long revisionId = ((Number) wrapper.getParamNameValuePairs().values().iterator().next()).longValue();
            return MesProProcessPoolEventRevisionDO.builder().id(revisionId).eventId(input.getEventId())
                    .afterPayload(input.getAfterPayload()).changeReason(input.getChangeReason())
                    .revisionSignatureId(input.getRevisionSignatureId()).build();
        });
        lenient().when(diffRows.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenAnswer(call -> {
            var wrapper = call.getArgument(0, com.baomidou.mybatisplus.core.conditions.AbstractWrapper.class);
            wrapper.getSqlSegment();
            Long revisionId = ((Number) wrapper.getParamNameValuePairs().values().iterator().next()).longValue();
            return List.of(MesProProcessPoolEventRevisionDiffDO.builder().id(1L).eventId(176L).revisionId(revisionId).build());
        });
        var query = new ElectronicSignatureQueryServiceImpl();
        var signatureRows = org.mockito.Mockito.mock(ElectronicSignatureRecordMapper.class);
        ReflectionTestUtils.setField(query, "signatureRecordMapper", signatureRows);
        ReflectionTestUtils.setField(query, "subjectAdapters", List.of(new MesBatchRecordSignatureSubjectAdapter()));
        ReflectionTestUtils.setField(service, "electronicSignatureQueryService", query);
        lenient().when(signatureRows.selectById(any(java.io.Serializable.class))).thenAnswer(call -> {
            MesProBatchRecordExecutionFieldAuditSignatureCommand input = lastArgument(signatureService, "recordFieldChangeSignature");
            var result = newSignature();
            String subject = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(0L, "FIELD_CHANGE",
                    null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                    input.getSignatureChallengeHash());
            String version = cn.hutool.crypto.digest.DigestUtil.sha256Hex(subject);
            var adapter = new MesBatchRecordSignatureSubjectAdapter();
            var snapshot = adapter.loadAndAuthorize(new SignatureSubjectCommand(3001L, "MES", "FIELD_CHANGE",
                    "MES_BATCH_RECORD", subject, version, input.getReasonText()));
            var sorted = new java.util.TreeMap<String, Object>();
            sorted.putAll(JsonUtils.parseObject(snapshot.canonicalContentJson(), java.util.Map.class));
            String canonical = JsonUtils.toJsonString(sorted);
            var definition = adapter.supportedActions().stream().filter(a -> "FIELD_CHANGE".equals(a.actionCode())).findFirst().orElseThrow();
            var record = ElectronicSignatureRecordDO.builder().id(9102L).moduleCode("MES").actionCode("FIELD_CHANGE")
                    .subjectType("MES_BATCH_RECORD").subjectId(subject).subjectVersion(version).actorId(3001L)
                    .meaningCode(definition.meaningCode()).meaningLabel(definition.meaningLabel()).reason(input.getReasonText())
                    .signedAt(result.getSignedAt()).timeEvidenceId("SERVER_CLOCK:" + result.getSignedAt())
                    .authenticationMethod("SESSION_PLUS_PASSWORD").canonicalContentJson(canonical)
                    .contentHash(cn.hutool.crypto.digest.DigestUtil.sha256Hex(canonical)).algorithm("SHA-256")
                    .keyVersion("system-local-v1").policyVersion(definition.policyVersion()).verificationStatus("VALID").build();
            record.setTenantId(1L);
            record.setEvidenceHash(cn.hutool.crypto.digest.DigestUtil.sha256Hex(String.join("|", "1", "3001", "MES", "FIELD_CHANGE",
                    "MES_BATCH_RECORD", subject, version, record.getMeaningCode(), record.getMeaningLabel(), input.getReasonText(),
                    result.getSignedAt().toString(), record.getTimeEvidenceId(), "SESSION_PLUS_PASSWORD", record.getContentHash(),
                    "", "", "", "", "", "SHA-256", "system-local-v1", record.getPolicyVersion(), "VALID")));
            return record;
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T lastArgument(Object mock, String method) {
        return (T) org.mockito.Mockito.mockingDetails(mock).getInvocations().stream()
                .filter(call -> call.getMethod().getName().equals(method)).reduce((first, last) -> last)
                .orElseThrow().getArgument(0);
    }

    private static MesProcessPoolProductionReportCorrectionCommand command() {
        return new MesProcessPoolProductionReportCorrectionCommand()
                .setEventId(176L)
                .setActorUserId(3001L)
                .setOutputQuantity(new BigDecimal("6"))
                .setLossDetails(List.of())
                .setDeviceParameterReadings(List.of())
                .setChangeReason("录入时数量填错")
                .setSignaturePassword("current-user-password");
    }

    private static MesProProcessPoolEventDO event() {
        MesProProcessPoolEventDO event = MesProProcessPoolEventDO.builder()
                .id(176L)
                .poolId(71L)
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT)
                .workOrderId(980008L)
                .routeId(922119L)
                .routeProcessId(928611L)
                .processId(922987L)
                .actualEmployeeId(964L)
                .feedbackSourceType("MES_PRO_FEEDBACK")
                .feedbackSourceId(5101L)
                .rawPayload("{\"activeOrderId\":413,\"fieldValues\":{\"OUTPUT_QUANTITY\":4,\"SCRAP_QUANTITY\":0},"
                        + "\"outputQuantity\":4,\"lossQuantity\":0,\"lossDetails\":[],"
                        + "\"lossReasonDetails\":[],\"deviceParameterReadings\":[]}")
                .serverSubmitTime(LocalDateTime.of(2026, 8, 7, 8, 30))
                .signatureId(9001L)
                .signatureUserId(964L)
                .build();
        event.setTenantId(1L);
        return event;
    }

    private static MesProProcessPoolEventDO eventWithBusinessDetails() {
        return event().setRawPayload("{\"activeOrderId\":413,\"fieldValues\":{\"OUTPUT_QUANTITY\":4,\"SCRAP_QUANTITY\":2,"
                + "\"DEVICE_PARAMETERS\":{\"球囊成型机\":{\"pressure\":20}}},"
                + "\"outputQuantity\":4,\"lossQuantity\":2,"
                + "\"materialDetails\":[{\"materialId\":3401,\"materialCode\":\"A001.02.034.202\","
                + "\"materialName\":\"弹簧\",\"outputQuantity\":2,\"lossQuantity\":0},"
                + "{\"materialId\":4801,\"materialCode\":\"A001.02.048.102\","
                + "\"materialName\":\"杠杆\",\"outputQuantity\":2,\"lossQuantity\":0}],"
                + "\"lossDetails\":[{\"reasonId\":8301,\"reasonCode\":\"LOSS-01\","
                + "\"reasonName\":\"正常损耗\",\"quantity\":2}],"
                + "\"lossReasonDetails\":[{\"reasonId\":8301,\"reasonCode\":\"LOSS-01\","
                + "\"reasonName\":\"正常损耗\",\"quantity\":2}],"
                + "\"equipmentParameters\":{\"球囊成型机\":{\"pressure\":20}},"
                + "\"deviceParameterReadings\":[{\"deviceId\":41,\"deviceName\":\"球囊成型机\","
                + "\"parameterCode\":\"pressure\",\"parameterName\":\"压力\",\"unit\":\"kPa\","
                + "\"value\":20,\"lowerLimit\":10,\"upperLimit\":30,\"parameterStatus\":\"NORMAL\"}]}");
    }

    private static MesProProcessPoolEventDO eventWithDeviceReadingsButMissingParameterCopies() {
        return event().setRawPayload("{\"activeOrderId\":413,\"fieldValues\":{\"OUTPUT_QUANTITY\":4,\"SCRAP_QUANTITY\":0},"
                + "\"outputQuantity\":4,\"lossQuantity\":0,\"lossDetails\":[],"
                + "\"lossReasonDetails\":[],"
                + "\"deviceParameterReadings\":[{\"deviceId\":41,\"deviceName\":\"球囊成型机\","
                + "\"parameterCode\":\"pressure\",\"parameterName\":\"压力\",\"unit\":\"kPa\","
                + "\"value\":20,\"lowerLimit\":10,\"upperLimit\":30,\"parameterStatus\":\"NORMAL\"}]}");
    }

    private static MesProProcessPoolEventDO eventWithMissingDeviceParameterValue() {
        return event().setRawPayload("{\"activeOrderId\":413,\"fieldValues\":{\"OUTPUT_QUANTITY\":4,\"SCRAP_QUANTITY\":0},"
                + "\"outputQuantity\":4,\"lossQuantity\":0,\"lossDetails\":[],"
                + "\"lossReasonDetails\":[],"
                + "\"deviceParameterReadings\":[{\"deviceId\":41,\"deviceName\":\"球囊成型机\","
                + "\"parameterCode\":\"pressure\",\"parameterName\":\"压力\",\"unit\":\"kPa\","
                + "\"value\":null,\"lowerLimit\":10,\"upperLimit\":30,\"parameterStatus\":null}]}");
    }

    private static MesProProcessPoolEventDO eventWithSelectDeviceParameterValue() {
        return event().setRawPayload("{\"activeOrderId\":413,\"fieldValues\":{\"OUTPUT_QUANTITY\":4,\"SCRAP_QUANTITY\":0,"
                + "\"DEVICE_PARAMETERS\":{\"球囊成型机\":{\"medium\":\"自来水\"}}},"
                + "\"outputQuantity\":4,\"lossQuantity\":0,\"lossDetails\":[],"
                + "\"lossReasonDetails\":[],"
                + "\"equipmentParameters\":{\"球囊成型机\":{\"medium\":\"自来水\"}},"
                + "\"deviceParameterReadings\":[{\"deviceId\":41,\"deviceName\":\"球囊成型机\","
                + "\"parameterCode\":\"medium\",\"parameterName\":\"清洗介质\",\"unit\":\"\","
                + "\"value\":\"自来水\",\"textValue\":\"自来水\",\"valueType\":\"SELECT\","
                + "\"optionValues\":[\"自来水\",\"纯化水\"],\"parameterStatus\":\"NORMAL\"}]}");
    }

    private static MesProProcessPoolEventDO eventWithZeroLossMaterialFacts() {
        return event().setRawPayload("{\"activeOrderId\":413,\"fieldValues\":{\"OUTPUT_QUANTITY\":4,\"SCRAP_QUANTITY\":0},"
                + "\"outputQuantity\":4,\"lossQuantity\":0,"
                + "\"materialDetails\":[{\"materialId\":3401,\"materialCode\":\"A001.02.034.202\","
                + "\"materialName\":\"弹簧\",\"outputQuantity\":4,\"lossQuantity\":0,"
                + "\"lossDetails\":[],\"deviceParameterReadings\":[]}],"
                + "\"lossDetails\":[],\"lossReasonDetails\":[],\"deviceParameterReadings\":[]}");
    }

    private static MesProFeedbackDO formalFeedback() {
        return MesProFeedbackDO.builder()
                .id(5101L)
                .workOrderId(980008L)
                .routeId(922119L)
                .processId(922987L)
                .feedbackQuantity(new BigDecimal("4"))
                .qualifiedQuantity(new BigDecimal("4"))
                .unqualifiedQuantity(BigDecimal.ZERO)
                .laborScrapQuantity(BigDecimal.ZERO)
                .materialScrapQuantity(BigDecimal.ZERO)
                .otherScrapQuantity(BigDecimal.ZERO)
                .build();
    }

    private static MesProFeedbackMaterialDO formalMaterial(Long id, Long materialId) {
        return MesProFeedbackMaterialDO.builder()
                .id(id)
                .feedbackId(5101L)
                .materialId(materialId)
                .outputQuantity(new BigDecimal("4"))
                .lossQuantity(BigDecimal.ZERO)
                .lossDetailsJson("[]")
                .deviceParameterReadingsJson("[]")
                .build();
    }

    private static MesProProcessPoolQuantityFragmentDO fragment() {
        return MesProProcessPoolQuantityFragmentDO.builder()
                .id(63L)
                .eventId(176L)
                .sourceQuantityType("OUTPUT")
                .totalQuantity(new BigDecimal("4"))
                .allocatedQuantity(BigDecimal.ZERO)
                .availableQuantity(new BigDecimal("4"))
                .allocationStatus(MesProProcessPoolQuantityFragmentDO.ALLOCATION_STATUS_AVAILABLE)
                .locked(Boolean.FALSE)
                .build();
    }

    private static MesProBatchRecordExecutionFieldAuditSignatureResult newSignature() {
        return new MesProBatchRecordExecutionFieldAuditSignatureResult()
                .setSignatureId(9102L)
                .setActorId(3001L)
                .setActorName("生产组长")
                .setSignedAt(LocalDateTime.of(2026, 8, 7, 9, 30));
    }
}
