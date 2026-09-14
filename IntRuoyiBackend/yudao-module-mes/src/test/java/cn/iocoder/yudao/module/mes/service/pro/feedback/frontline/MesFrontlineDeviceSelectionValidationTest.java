package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackPayloadReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitReqVO;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineDeviceParameterOption;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineTeamDeviceOption;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MesFrontlineDeviceSelectionValidationTest {

    @Test
    void shouldAllowMultipleDevicesAndNoSelection() {
        List<MesFrontlineTeamDeviceOption> allowed = List.of(
                device(101L, "wash", "MULTIPLE"), device(102L, "wash", "MULTIPLE"));

        assertDoesNotThrow(() -> MesProFrontlineFeedbackSubmitServiceImpl.validateDeviceSelections(
                null, List.of(selected(101L), selected(102L)), List.of(), allowed));
        assertDoesNotThrow(() -> MesProFrontlineFeedbackSubmitServiceImpl.validateDeviceSelections(
                null, List.of(), List.of(), allowed));
    }

    @Test
    void shouldCanonicalizeSelectedDeviceIdentityFromFrozenSession() {
        List<MesFrontlineTeamDeviceOption> allowed = List.of(device(101L, "wash", "SINGLE"));
        var selected = new MesProFrontlineFeedbackPayloadReqVO.SelectedDeviceReqVO()
                .setDeviceId(101L)
                .setDeviceCode("伪造编号")
                .setDeviceName("伪造名称")
                .setInMeteringValidityPeriod(false);

        MesProFrontlineFeedbackSubmitServiceImpl.validateDeviceSelections(
                null, List.of(selected), List.of(), allowed);

        assertEquals("D-101", selected.getDeviceCode());
        assertEquals("设备101", selected.getDeviceName());
        assertEquals(false, selected.getInMeteringValidityPeriod());
    }

    @Test
    void shouldRewriteNoMaterialRawPayloadFromCanonicalProcessDevices() {
        var selected = new MesProFrontlineFeedbackPayloadReqVO.SelectedDeviceReqVO()
                .setDeviceId(101L)
                .setDeviceCode("D-101")
                .setDeviceName("设备101")
                .setInMeteringValidityPeriod(true);
        var reading = decimalReading(101L, "power", "25")
                .setDeviceCode("D-101")
                .setDeviceName("设备101");
        var payload = new MesProFrontlineFeedbackPayloadReqVO()
                .setSelectedDevices(List.of(selected))
                .setDeviceParameterReadings(List.of(reading));
        var request = new MesProFrontlineFeedbackSubmitReqVO()
                .setFeedbackPayload(payload)
                .setRawPayload(Map.of("selectedDevices", List.of(Map.of(
                        "deviceId", 101L, "deviceCode", "伪造编号", "deviceName", "伪造名称"))));

        MesProFrontlineFeedbackSubmitServiceImpl.normalizeProcessDeviceAuditPayload(request);

        assertEquals(List.of(selected), request.getRawPayload().get("selectedDevices"));
        assertEquals(List.of(reading), request.getRawPayload().get("deviceParameterReadings"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> validity =
                (List<Map<String, Object>>) request.getRawPayload().get("deviceMeteringValidity");
        assertEquals("D-101", validity.get(0).get("deviceCode"));
        assertEquals(Boolean.TRUE, validity.get(0).get("inMeteringValidityPeriod"));
    }

    @Test
    void shouldRejectTwoDevicesFromSingleGroup() {
        List<MesFrontlineTeamDeviceOption> allowed = List.of(
                device(101L, "wash", "SINGLE"), device(102L, "wash", "SINGLE"));

        assertThrows(ServiceException.class, () ->
                MesProFrontlineFeedbackSubmitServiceImpl.validateDeviceSelections(
                        null, List.of(selected(101L), selected(102L)), List.of(), allowed));
    }

    @Test
    void shouldRejectReadingForUnselectedDeviceAndLegacyField() {
        var reading = new MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO()
                .setDeviceId(101L).setParameterCode("power");
        List<MesFrontlineTeamDeviceOption> allowed = List.of(device(101L, "wash", "MULTIPLE"));

        assertThrows(ServiceException.class, () ->
                MesProFrontlineFeedbackSubmitServiceImpl.validateDeviceSelections(
                        null, List.of(), List.of(reading), allowed));
        assertThrows(ServiceException.class, () ->
                MesProFrontlineFeedbackSubmitServiceImpl.validateDeviceSelections(
                        selected(101L), List.of(), List.of(), allowed));
    }

    @Test
    void shouldRejectUnknownAndDuplicateParameterReadings() {
        List<MesFrontlineTeamDeviceOption> allowed = List.of(deviceWithDecimalParameter(101L));
        var valid = decimalReading(101L, "power", "25");
        var duplicate = decimalReading(101L, "POWER", "26");
        var unknown = decimalReading(101L, "unknown", "1");

        assertThrows(ServiceException.class, () ->
                MesProFrontlineFeedbackSubmitServiceImpl.validateDeviceSelections(
                        null, List.of(selected(101L)), List.of(valid, duplicate), allowed));
        assertThrows(ServiceException.class, () ->
                MesProFrontlineFeedbackSubmitServiceImpl.validateDeviceSelections(
                        null, List.of(selected(101L)), List.of(valid, unknown), allowed));
    }

    @Test
    void shouldValidateValueAndApplyServerParameterStandard() {
        List<MesFrontlineTeamDeviceOption> allowed = List.of(deviceWithDecimalParameter(101L));
        var missingValue = new MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO()
                .setDeviceId(101L).setParameterCode("power");
        assertThrows(ServiceException.class, () ->
                MesProFrontlineFeedbackSubmitServiceImpl.validateDeviceSelections(
                        null, List.of(selected(101L)), List.of(missingValue), allowed));

        var reading = decimalReading(101L, "POWER", "25");
        assertDoesNotThrow(() -> MesProFrontlineFeedbackSubmitServiceImpl.validateDeviceSelections(
                null, List.of(selected(101L)), List.of(reading), allowed));
        assertEquals("power", reading.getParameterCode());
        assertEquals("清洗功率", reading.getParameterName());
        assertEquals("%", reading.getUnit());
        assertEquals("NORMAL", reading.getParameterStatus());
    }

    private static MesFrontlineTeamDeviceOption device(Long id, String groupKey, String mode) {
        return new MesFrontlineTeamDeviceOption(id, "D-" + id, "设备" + id, "ENABLED",
                groupKey, mode, List.of());
    }

    private static MesProFrontlineFeedbackPayloadReqVO.SelectedDeviceReqVO selected(Long id) {
        return new MesProFrontlineFeedbackPayloadReqVO.SelectedDeviceReqVO().setDeviceId(id);
    }

    private static MesFrontlineTeamDeviceOption deviceWithDecimalParameter(Long id) {
        return new MesFrontlineTeamDeviceOption(id, "D-" + id, "设备" + id, "ENABLED",
                "wash", "MULTIPLE", List.of(new MesFrontlineDeviceParameterOption(
                "power", "清洗功率", "%", new BigDecimal("20"), new BigDecimal("30"),
                new BigDecimal("25"), "DECIMAL", "20-30%", List.of(), null, 0)));
    }

    private static MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO decimalReading(
            Long deviceId, String parameterCode, String value) {
        return new MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO()
                .setDeviceId(deviceId)
                .setParameterCode(parameterCode)
                .setValue(new BigDecimal(value));
    }
}
