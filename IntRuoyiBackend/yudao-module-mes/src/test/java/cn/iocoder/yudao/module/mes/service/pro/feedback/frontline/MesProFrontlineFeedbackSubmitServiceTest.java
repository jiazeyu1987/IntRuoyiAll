package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitRespVO;
import cn.iocoder.yudao.module.mes.service.md.autocode.MesMdAutoCodeRecordService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineSubmitAuthorizationService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.ActiveOrderSnapshotResolver;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolSubmitEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProFrontlineFeedbackSubmitServiceTest {

    @Mock
    private MesProFeedbackService feedbackService;
    @Mock
    private MesProFeedbackMaterialService feedbackMaterialService;
    @Mock
    private MesProcessPoolSubmitEventService processPoolSubmitEventService;
    @Mock
    private MesFrontlineSubmitAuthorizationService submitAuthorizationService;
    @Mock
    private MesFrontlineLossReasonValidator lossReasonValidator;
    @Mock
    private MesFrontlineDeviceParameterValidator deviceParameterValidator;
    @Mock
    private MesFrontlineParameterAuditService parameterAuditService;
    @Mock
    private MesMdAutoCodeRecordService autoCodeRecordService;
    @Mock
    private MesProBatchRecordExecutionSignatureService signatureService;
    @Mock
    private ActiveOrderSnapshotResolver activeOrderSnapshotResolver;

    private MesProFrontlineFeedbackSubmitService submitService;

    @BeforeEach
    void setUp() {
        submitService = new MesProFrontlineFeedbackSubmitServiceImpl(
                feedbackService,
                feedbackMaterialService,
                processPoolSubmitEventService,
                submitAuthorizationService,
                parameterAuditService,
                new MesProFrontlineFeedbackMaterialSubmissionValidator(lossReasonValidator),
                new MesProFrontlineFeedbackPayloadSplitter(),
                autoCodeRecordService,
                signatureService,
                activeOrderSnapshotResolver);
        MesProFrontlineFeedbackSubmitSnapshotTestSupport.stubAuthorization(submitAuthorizationService);
        MesProFrontlineFeedbackSubmitTestData.stubLossReasonValidator(lossReasonValidator);
        org.mockito.Mockito.lenient().when(parameterAuditService.resolveAndApply(any()))
                .thenReturn(MesFrontlineParameterAuditResult.empty());
        org.mockito.Mockito.lenient().when(activeOrderSnapshotResolver.requireEffective(81L))
                .thenReturn(new ActiveOrderSnapshotResolver.ActiveOrderSnapshot(
                        81L, 41L, 21L, 627L, 71L, 72L, 73L));
    }

    @Test
    void shouldSignAsSelectedEmployeeWhenDeviceAccountIsLoggedIn() {
        when(processPoolSubmitEventService.findExistingSubmitEvent(any())).thenReturn(Optional.empty());
        when(feedbackService.createFrontlineFeedback(any())).thenReturn(501L);
        when(processPoolSubmitEventService.createSubmitEvent(any())).thenReturn(801L);
        when(signatureService.recordProductionSubmitSignature(9102L, "sign-123", "一线生产报工提交"))
                .thenReturn(4001L);
        stubValidLossReason();

        MesProFrontlineFeedbackSubmitReqVO reqVO = MesProFrontlineFeedbackSubmitTestData.buildSubmitReq()
                .setActualEmployeeId(9102L)
                .setSignatureEmployeeId(9102L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            assertEquals(801L, submitService.submit(reqVO).getProcessPoolEventId());
        }

        verify(submitAuthorizationService).authorize(argThat(command -> {
            assertEquals(9001L, command.loginUserId());
            assertEquals(9102L, command.actualEmployeeId());
            assertEquals(9102L, command.signatureEmployeeId());
            assertEquals("frontline-session-snapshot-001", command.frontlineSessionSnapshotId());
            assertEquals("frontline-session-snapshot-hash-001", command.frontlineSessionSnapshotHash());
            return true;
        }));
        verify(signatureService).recordProductionSubmitSignature(9102L, "sign-123", "一线生产报工提交");
        verify(feedbackService).createFrontlineFeedback(argThat(payload -> {
            assertEquals(9102L, payload.getFeedbackUserId());
            return true;
        }));
        verify(processPoolSubmitEventService).createSubmitEvent(argThat(payload -> {
            assertEquals(9102L, payload.getActualEmployeeId());
            assertEquals(9102L, payload.getSignatureEmployeeId());
            assertEquals(9001L, payload.getDeviceAccountUserId());
            return true;
        }));
    }

    @Test
    void shouldCreateFeedbackSourceAndProcessPoolEventWithoutFormalRecordbook() {
        when(processPoolSubmitEventService.findExistingSubmitEvent(any())).thenReturn(Optional.empty());
        when(feedbackService.createFrontlineFeedback(any())).thenReturn(501L);
        when(processPoolSubmitEventService.createSubmitEvent(any())).thenReturn(801L);
        when(autoCodeRecordService.generateAutoCode(any())).thenReturn("FB-F2-GEN");
        when(signatureService.recordProductionSubmitSignature(eq(9001L), eq("sign-123"), eq("一线生产报工提交")))
                .thenReturn(4001L);
        stubValidLossReason();

        MesProFrontlineFeedbackSubmitReqVO reqVO = MesProFrontlineFeedbackSubmitTestData.buildSubmitReq();
        assertNotNull(reqVO.getRecordbookPayload());
        reqVO.getFeedbackPayload().setCode(null).setType(null);
        MesProFrontlineFeedbackSubmitRespVO respVO;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            respVO = submitService.submit(reqVO);
        }
        assertNotNull(reqVO.getRecordbookPayload());

        assertEquals(501L, respVO.getFeedbackId());
        assertEquals(null, respVO.getRecordbookEntryId());
        assertEquals(null, respVO.getRecordbookEventId());
        assertEquals(801L, respVO.getProcessPoolEventId());

        InOrder inOrder = inOrder(submitAuthorizationService, lossReasonValidator, feedbackService,
                processPoolSubmitEventService);
        inOrder.verify(submitAuthorizationService).authorize(argThat(command -> {
            assertEquals(9001L, command.loginUserId());
            assertEquals(9001L, command.actualEmployeeId());
            assertEquals(9001L, command.signatureEmployeeId());
            assertEquals(501L, command.deviceId());
            assertEquals(11L, command.workstationId());
            assertEquals(21L, command.routeId());
            assertEquals(71L, command.routeProcessId());
            assertEquals(31L, command.processId());
            assertEquals("PRODUCTION_SIMPLE", command.templateNo());
            assertEquals("frontline-session-snapshot-001", command.frontlineSessionSnapshotId());
            assertEquals("frontline-session-snapshot-hash-001", command.frontlineSessionSnapshotHash());
            return true;
        }));
        verify(submitAuthorizationService).authorizeActiveOrder(9001L, 81L, 41L, 21L, 71L, 31L);
        verify(processPoolSubmitEventService).createInitialAllocation(801L, 81L, new BigDecimal("100.500"));
        inOrder.verify(lossReasonValidator).requireSnapshotLossReasons(
                any(),
                eq(MesProFrontlineFeedbackSubmitTestData.buildSubmitReq().getFeedbackPayload().getLossDetails()),
                eq(new BigDecimal("2.500")));
        inOrder.verify(feedbackService).createFrontlineFeedback(argThat(payload -> {
            assertEquals(new BigDecimal("103.000"), payload.getFeedbackQuantity());
            assertEquals(new BigDecimal("100.500"), payload.getQualifiedQuantity());
            assertEquals(new BigDecimal("2.500"), payload.getUnqualifiedQuantity());
            assertEquals(8301L, payload.getLossReasonId());
            assertEquals("LOSS-001", payload.getLossReasonCodeSnapshot());
            assertEquals("正常损耗", payload.getLossReasonNameSnapshot());
            assertEquals(9001L, payload.getFeedbackUserId());
            assertEquals("FB-F2-GEN", payload.getCode());
            assertEquals(1, payload.getType());
            return true;
        }));
        inOrder.verify(feedbackService).submitFeedback(501L);
        inOrder.verify(processPoolSubmitEventService).createSubmitEvent(argThat(payload -> {
            assertEquals("P0-SUBMIT-F2-20260730-001", payload.getProcessPoolSubmissionIdempotencyKey());
            assertEquals(501L, payload.getFeedbackId());
            assertEquals(null, payload.getRecordbookEntryId());
            assertEquals(null, payload.getRecordbookEventId());
            Object recordbookSnapshot = payload.getRawPayload().get("recordbookSourceSnapshot");
            assertNotNull(recordbookSnapshot, payload.getRawPayload().toString());
            assertEquals(901L, ((java.util.Map<?, ?>) recordbookSnapshot).get("recordbookId"));
            assertEquals(4001L, payload.getSignatureId());
            assertEquals(8301L, payload.getRawPayload().get("lossReasonId"));
            assertEquals("正常损耗", payload.getRawPayload().get("lossReasonNameSnapshot"));
            Object activeOrderProcess = payload.getRawPayload().get("activeOrderProcess");
            assertEquals(81L, ((java.util.Map<?, ?>) activeOrderProcess).get("activeOrderId"));
            assertEquals(5101L, ((java.util.Map<?, ?>) activeOrderProcess).get("activeOrderProcessSnapshotId"));
            assertEquals(71L, ((java.util.Map<?, ?>) activeOrderProcess).get("routeProcessId"));
            assertEquals(31L, ((java.util.Map<?, ?>) activeOrderProcess).get("processId"));
            return true;
        }));
    }

    @Test
    void shouldSubmitWithoutMaterialFactsWhenFrozenBatchRecordMaterialsAreEmpty() {
        MesProFrontlineFeedbackSubmitSnapshotTestSupport.stubAuthorization(
                submitAuthorizationService, List.of());
        when(processPoolSubmitEventService.findExistingSubmitEvent(any())).thenReturn(Optional.empty());
        when(feedbackService.createFrontlineFeedback(any())).thenReturn(501L);
        when(processPoolSubmitEventService.createSubmitEvent(any())).thenReturn(801L);
        when(signatureService.recordProductionSubmitSignature(9001L, "sign-123", "一线生产报工提交"))
                .thenReturn(4001L);
        stubValidLossReason();
        MesProFrontlineFeedbackSubmitReqVO request = MesProFrontlineFeedbackSubmitTestData.buildSubmitReq()
                .setMaterialDetails(List.of());

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            assertEquals(801L, submitService.submit(request).getProcessPoolEventId());
        }

        verify(feedbackService).createFrontlineFeedback(argThat(payload ->
                new BigDecimal("100.500").compareTo(payload.getFeedbackQuantity()) == 0
                        && new BigDecimal("98.000").compareTo(payload.getQualifiedQuantity()) == 0
                        && new BigDecimal("2.500").compareTo(payload.getUnqualifiedQuantity()) == 0));
        verify(processPoolSubmitEventService).createInitialAllocation(
                801L, 81L, new BigDecimal("100.500"));
        verifyNoInteractions(feedbackMaterialService);
    }

    @Test
    void shouldAssignProductionSubmitToSelectedActiveOrderWithoutQuantityCap() {
        when(processPoolSubmitEventService.findExistingSubmitEvent(any())).thenReturn(Optional.empty());
        when(feedbackService.createFrontlineFeedback(any())).thenReturn(501L);
        when(processPoolSubmitEventService.createSubmitEvent(any())).thenReturn(801L);
        when(signatureService.recordProductionSubmitSignature(9001L, "sign-123", "一线生产报工提交"))
                .thenReturn(4001L);
        stubValidLossReason();
        MesProFrontlineFeedbackSubmitReqVO request = MesProFrontlineFeedbackSubmitTestData.buildSubmitReq();
        request.getFeedbackPayload().setOutputQuantity(new BigDecimal("200"));
        request.getMaterialDetails().forEach(material -> material.setOutputQuantity(new BigDecimal("200")));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            assertEquals(801L, submitService.submit(request).getProcessPoolEventId());
        }

        verify(submitAuthorizationService).authorizeActiveOrder(9001L, 81L, 41L, 21L, 71L, 31L);
        verify(feedbackService).createFrontlineFeedback(argThat(payload ->
                new BigDecimal("202.500").compareTo(payload.getFeedbackQuantity()) == 0
                        && new BigDecimal("200").compareTo(payload.getQualifiedQuantity()) == 0
                        && Long.valueOf(41L).equals(payload.getWorkOrderId())));
        verify(processPoolSubmitEventService).createSubmitEvent(argThat(payload ->
                new BigDecimal("200").compareTo(payload.getOutputQuantity()) == 0
                        && Long.valueOf(41L).equals(payload.getWorkOrderId())));
    }

    @Test
    void shouldUseMinimumMaterialCompletionForProgressAndPersistEveryMaterial() {
        when(processPoolSubmitEventService.findExistingSubmitEvent(any())).thenReturn(Optional.empty());
        when(feedbackService.createFrontlineFeedback(any())).thenReturn(501L);
        when(processPoolSubmitEventService.createSubmitEvent(any())).thenReturn(801L);
        when(signatureService.recordProductionSubmitSignature(9001L, "sign-123", "一线生产报工提交"))
                .thenReturn(4001L);
        stubValidLossReason();
        MesProFrontlineFeedbackSubmitReqVO request = MesProFrontlineFeedbackSubmitTestData.buildSubmitReq();
        request.getFeedbackPayload().setOutputQuantity(new BigDecimal("999"));
        request.getMaterialDetails().get(0).setOutputQuantity(new BigDecimal("5"));
        request.getMaterialDetails().get(0).setLossQuantity(BigDecimal.ZERO).setLossDetails(List.of());
        request.getMaterialDetails().get(1).setOutputQuantity(new BigDecimal("3"));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            submitService.submit(request);
        }

        verify(feedbackService).createFrontlineFeedback(argThat(payload ->
                new BigDecimal("3").compareTo(payload.getFeedbackQuantity()) == 0));
        verify(feedbackMaterialService).createMaterials(argThat(command -> {
            assertEquals(501L, command.feedbackId());
            assertEquals(627L, command.routeVersionId());
            assertEquals(List.of(new BigDecimal("5"), new BigDecimal("3")), command.entries().stream()
                    .map(MesProFeedbackMaterialCreateCommand.Entry::outputQuantity).toList());
            return true;
        }));
        verify(processPoolSubmitEventService).createSubmitEvent(argThat(payload ->
                new BigDecimal("3").compareTo(payload.getOutputQuantity()) == 0));
        verify(processPoolSubmitEventService).createInitialAllocation(801L, 81L, new BigDecimal("3"));
    }

    @Test
    void shouldPersistExplicitZeroMaterialFactsWithoutAddingOrderProgress() {
        when(processPoolSubmitEventService.findExistingSubmitEvent(any())).thenReturn(Optional.empty());
        when(feedbackService.createFrontlineFeedback(any())).thenReturn(501L);
        when(processPoolSubmitEventService.createSubmitEvent(any())).thenReturn(801L);
        when(signatureService.recordProductionSubmitSignature(9001L, "sign-123", "一线生产报工提交"))
                .thenReturn(4001L);
        MesProFrontlineFeedbackSubmitReqVO request = MesProFrontlineFeedbackSubmitTestData.buildSubmitReq();
        request.getMaterialDetails().forEach(material -> material
                .setOutputQuantity(BigDecimal.ZERO)
                .setLossQuantity(BigDecimal.ZERO)
                .setLossDetails(List.of()));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            submitService.submit(request);
        }

        verify(feedbackService).createFrontlineFeedback(argThat(payload ->
                BigDecimal.ZERO.compareTo(payload.getFeedbackQuantity()) == 0));
        verify(feedbackMaterialService).createMaterials(any());
        verify(processPoolSubmitEventService, never()).createInitialAllocation(any(), any(), any());
    }

    @Test
    void shouldRejectMissingFrozenMaterialBeforeAnyFormalWrite() {
        MesProFrontlineFeedbackSubmitReqVO request = MesProFrontlineFeedbackSubmitTestData.buildSubmitReq();
        request.setMaterialDetails(List.of(request.getMaterialDetails().get(0)));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                    () -> submitService.submit(request));
        }

        verifyNoInteractions(feedbackService, feedbackMaterialService, processPoolSubmitEventService, signatureService);
    }

    @Test
    void shouldRejectFrontlineProductionWithoutSelectedActiveOrder() {
        MesProFrontlineFeedbackSubmitReqVO reqVO = MesProFrontlineFeedbackSubmitTestData.buildSubmitReq();
        reqVO.getFeedbackPayload().setWorkOrderId(null);
        reqVO.getProcessPoolContext().setWorkOrderId(null);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                    () -> submitService.submit(reqVO));
        }

        verifyNoInteractions(feedbackService, processPoolSubmitEventService, signatureService);
    }

    @Test
    void shouldReturnExistingSubmitResultBeforeWritingDuplicateFeedback() {
        when(processPoolSubmitEventService.findExistingSubmitEvent(any()))
                .thenReturn(Optional.of(new cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolSubmitEventResult()
                        .setFeedbackId(501L)
                        .setRecordbookEntryId(701L)
                        .setRecordbookEventId(702L)
                        .setProcessPoolEventId(801L)
                        .setParameterAuditResult(MesFrontlineParameterAuditResult.empty())));
        stubValidLossReason();

        MesProFrontlineFeedbackSubmitRespVO respVO;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            respVO = submitService.submit(MesProFrontlineFeedbackSubmitTestData.buildSubmitReq());
        }

        assertEquals(501L, respVO.getFeedbackId());
        assertEquals(701L, respVO.getRecordbookEntryId());
        assertEquals(702L, respVO.getRecordbookEventId());
        assertEquals(801L, respVO.getProcessPoolEventId());
        verify(processPoolSubmitEventService).findExistingSubmitEvent(argThat(payload -> {
            assertEquals("P0-SUBMIT-F2-20260730-001", payload.getProcessPoolSubmissionIdempotencyKey());
            assertEquals(41L, payload.getWorkOrderId());
            assertEquals(71L, payload.getRouteProcessId());
            assertEquals(31L, payload.getProcessId());
            return true;
        }));
        verify(feedbackService, never()).createFrontlineFeedback(any());
        verify(submitAuthorizationService, never()).authorizeActiveOrder(any(), any(), any(), any(), any(), any());
        verifyNoInteractions(autoCodeRecordService, signatureService);
        verify(processPoolSubmitEventService, never()).createSubmitEvent(any());
    }

    @Test
    void shouldRejectDisabledOrCrossRouteProcessLossReasonBeforeWritingAnyRecord() {
        when(lossReasonValidator.requireSnapshotLossReasons(
                any(),
                eq(MesProFrontlineFeedbackSubmitTestData.buildSubmitReq().getFeedbackPayload().getLossDetails()),
                eq(new BigDecimal("2.500"))))
                .thenThrow(new IllegalStateException("损耗原因不属于当前工序或已禁用"));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            assertThrows(IllegalStateException.class,
                    () -> submitService.submit(MesProFrontlineFeedbackSubmitTestData.buildSubmitReq()));
        }

        verify(submitAuthorizationService).authorize(any());
        verify(lossReasonValidator).requireSnapshotLossReasons(
                any(),
                eq(MesProFrontlineFeedbackSubmitTestData.buildSubmitReq().getFeedbackPayload().getLossDetails()),
                eq(new BigDecimal("2.500")));
        verify(feedbackService, never()).createFrontlineFeedback(any());
        verifyNoInteractions(processPoolSubmitEventService);
    }

    @Test
    void shouldRejectSignatureEmployeeMismatchBeforeWritingAnyRecord() {
        MesProFrontlineFeedbackSubmitReqVO reqVO = MesProFrontlineFeedbackSubmitTestData.buildSubmitReq()
                .setSignatureEmployeeId(3999L);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            assertThrows(RuntimeException.class, () -> submitService.submit(reqVO));
        }

        verify(feedbackService, never()).createFrontlineFeedback(any());
        verifyNoInteractions(processPoolSubmitEventService, submitAuthorizationService);
    }

    @Test
    void shouldRejectClientSuppliedSignatureIdBeforeWritingAnyRecord() {
        MesProFrontlineFeedbackSubmitReqVO reqVO = MesProFrontlineFeedbackSubmitTestData.buildSubmitReq()
                .setSignatureId(4001L);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            assertThrows(RuntimeException.class, () -> submitService.submit(reqVO));
        }

        verifyNoInteractions(submitAuthorizationService, feedbackService,
                processPoolSubmitEventService, signatureService);
    }

    @Test
    void shouldRejectMissingSessionSnapshotBeforeWritingAnyRecord() {
        MesProFrontlineFeedbackSubmitReqVO reqVO = MesProFrontlineFeedbackSubmitTestData.buildSubmitReq()
                .setFrontlineSessionSnapshotId(null)
                .setFrontlineSessionSnapshotHash(null);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            assertThrows(RuntimeException.class, () -> submitService.submit(reqVO));
        }

        verifyNoInteractions(submitAuthorizationService, feedbackService,
                processPoolSubmitEventService, signatureService);
    }

    @Test
    void shouldRejectUnauthorizedDeviceEmployeeContextBeforeWritingAnyRecord() {
        org.mockito.Mockito.reset(submitAuthorizationService);
        when(submitAuthorizationService.authorize(any()))
                .thenThrow(new IllegalStateException("route process not authorized"));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            assertThrows(IllegalStateException.class,
                    () -> submitService.submit(MesProFrontlineFeedbackSubmitTestData.buildSubmitReq()));
        }

        verify(submitAuthorizationService).authorize(any());
        verify(feedbackService, never()).createFrontlineFeedback(any());
        verifyNoInteractions(processPoolSubmitEventService);
    }

    @Test
    void shouldSubmitWithoutDeviceParameterValidation() {
        when(processPoolSubmitEventService.findExistingSubmitEvent(any())).thenReturn(Optional.empty());
        when(feedbackService.createFrontlineFeedback(any())).thenReturn(501L);
        when(processPoolSubmitEventService.createSubmitEvent(any())).thenReturn(801L);
        when(signatureService.recordProductionSubmitSignature(eq(9001L), eq("sign-123"), eq("一线生产报工提交")))
                .thenReturn(4001L);
        stubValidLossReason();

        MesProFrontlineFeedbackSubmitReqVO reqVO = MesProFrontlineFeedbackSubmitTestData.buildSubmitReq();
        reqVO.getFeedbackPayload()
                .setSelectedDevice(null)
                .setDeviceParameterReadings(List.of());

        MesProFrontlineFeedbackSubmitRespVO respVO;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            respVO = submitService.submit(reqVO);
        }

        assertEquals(801L, respVO.getProcessPoolEventId());
        verify(submitAuthorizationService).authorize(any());
        verify(parameterAuditService).resolveAndApply(reqVO);
        verifyNoInteractions(deviceParameterValidator);
    }

    @Test
    void shouldRejectLossQuantityGreaterThanOutputBeforeWritingAnyRecord() {
        MesProFrontlineFeedbackSubmitReqVO reqVO = MesProFrontlineFeedbackSubmitTestData.buildSubmitReq();
        reqVO.getMaterialDetails().get(0)
                .setOutputQuantity(new BigDecimal("10.000"))
                .setLossQuantity(new BigDecimal("11.000"));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            assertThrows(RuntimeException.class, () -> submitService.submit(reqVO));
        }

        verify(submitAuthorizationService).authorize(any());
        verifyNoInteractions(feedbackService, processPoolSubmitEventService, feedbackMaterialService,
                signatureService);
    }

    @Test
    void shouldRejectNegativeProductionQuantityBeforeWritingAnyRecord() {
        MesProFrontlineFeedbackSubmitReqVO negativeOutputReq = MesProFrontlineFeedbackSubmitTestData.buildSubmitReq();
        negativeOutputReq.getMaterialDetails().get(0)
                .setOutputQuantity(new BigDecimal("-1.000"))
                .setLossQuantity(BigDecimal.ZERO);
        MesProFrontlineFeedbackSubmitReqVO negativeLossReq = MesProFrontlineFeedbackSubmitTestData.buildSubmitReq();
        negativeLossReq.getMaterialDetails().get(0)
                .setOutputQuantity(new BigDecimal("10.000"))
                .setLossQuantity(new BigDecimal("-0.001"));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            assertThrows(RuntimeException.class, () -> submitService.submit(negativeOutputReq));
            assertThrows(RuntimeException.class, () -> submitService.submit(negativeLossReq));
        }

        verify(submitAuthorizationService, org.mockito.Mockito.times(2)).authorize(any());
        verifyNoInteractions(feedbackService, processPoolSubmitEventService, feedbackMaterialService,
                signatureService);
    }

    private void stubValidLossReason() {
        org.mockito.Mockito.lenient().when(lossReasonValidator.requireSnapshotLossReasons(
                any(),
                eq(MesProFrontlineFeedbackSubmitTestData.buildSubmitReq().getFeedbackPayload().getLossDetails()),
                eq(new BigDecimal("2.500"))))
                .thenReturn(List.of(new MesFrontlineLossReasonSnapshot(8301L, "LOSS-001", "正常损耗")));
    }
}
