package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolQuantityFragmentDO;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProcessPoolProductionReportCorrectionServiceTest {

    @Mock
    private MesProProcessPoolEventMapper eventMapper;
    @Mock
    private MesProProcessPoolQuantityFragmentMapper fragmentMapper;
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
                eventMapper, fragmentMapper, revisionService, signatureService, lossReasonValidator, scopeService,
                reportManagementSummaryService);
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
        return MesProProcessPoolEventDO.builder()
                .id(176L)
                .poolId(71L)
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT)
                .workOrderId(980008L)
                .routeId(922119L)
                .routeProcessId(928611L)
                .processId(922987L)
                .actualEmployeeId(964L)
                .rawPayload("{\"fieldValues\":{\"OUTPUT_QUANTITY\":4,\"SCRAP_QUANTITY\":0},"
                        + "\"outputQuantity\":4,\"lossQuantity\":0,\"lossDetails\":[],"
                        + "\"lossReasonDetails\":[],\"deviceParameterReadings\":[]}")
                .serverSubmitTime(LocalDateTime.of(2026, 8, 7, 8, 30))
                .signatureId(9001L)
                .signatureUserId(964L)
                .build();
    }

    private static MesProProcessPoolEventDO eventWithBusinessDetails() {
        return event().setRawPayload("{\"fieldValues\":{\"OUTPUT_QUANTITY\":4,\"SCRAP_QUANTITY\":2,"
                + "\"DEVICE_PARAMETERS\":{\"球囊成型机\":{\"pressure\":20}}},"
                + "\"outputQuantity\":4,\"lossQuantity\":2,"
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
