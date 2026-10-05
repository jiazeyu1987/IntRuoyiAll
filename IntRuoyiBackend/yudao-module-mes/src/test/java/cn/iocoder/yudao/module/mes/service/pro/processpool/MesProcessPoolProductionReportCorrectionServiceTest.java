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
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProcessPoolProductionReportCorrectionServiceTest {

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
        initializeAuditFixture();
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
                .rawPayload("{\"fieldValues\":{\"OUTPUT_QUANTITY\":4,\"SCRAP_QUANTITY\":0},"
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
        return event().setRawPayload("{\"fieldValues\":{\"OUTPUT_QUANTITY\":4,\"SCRAP_QUANTITY\":2,"
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
        return event().setRawPayload("{\"fieldValues\":{\"OUTPUT_QUANTITY\":4,\"SCRAP_QUANTITY\":0},"
                + "\"outputQuantity\":4,\"lossQuantity\":0,\"lossDetails\":[],"
                + "\"lossReasonDetails\":[],"
                + "\"deviceParameterReadings\":[{\"deviceId\":41,\"deviceName\":\"球囊成型机\","
                + "\"parameterCode\":\"pressure\",\"parameterName\":\"压力\",\"unit\":\"kPa\","
                + "\"value\":20,\"lowerLimit\":10,\"upperLimit\":30,\"parameterStatus\":\"NORMAL\"}]}");
    }

    private static MesProProcessPoolEventDO eventWithMissingDeviceParameterValue() {
        return event().setRawPayload("{\"fieldValues\":{\"OUTPUT_QUANTITY\":4,\"SCRAP_QUANTITY\":0},"
                + "\"outputQuantity\":4,\"lossQuantity\":0,\"lossDetails\":[],"
                + "\"lossReasonDetails\":[],"
                + "\"deviceParameterReadings\":[{\"deviceId\":41,\"deviceName\":\"球囊成型机\","
                + "\"parameterCode\":\"pressure\",\"parameterName\":\"压力\",\"unit\":\"kPa\","
                + "\"value\":null,\"lowerLimit\":10,\"upperLimit\":30,\"parameterStatus\":null}]}");
    }

    private static MesProProcessPoolEventDO eventWithSelectDeviceParameterValue() {
        return event().setRawPayload("{\"fieldValues\":{\"OUTPUT_QUANTITY\":4,\"SCRAP_QUANTITY\":0,"
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
        return event().setRawPayload("{\"fieldValues\":{\"OUTPUT_QUANTITY\":4,\"SCRAP_QUANTITY\":0},"
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
