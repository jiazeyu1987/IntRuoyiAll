package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackPayloadReqVO;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineTeamDeviceOption;
import org.junit.jupiter.api.Test;

import java.util.List;

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

    private static MesFrontlineTeamDeviceOption device(Long id, String groupKey, String mode) {
        return new MesFrontlineTeamDeviceOption(id, "D-" + id, "设备" + id, "ENABLED",
                groupKey, mode, List.of());
    }

    private static MesProFrontlineFeedbackPayloadReqVO.SelectedDeviceReqVO selected(Long id) {
        return new MesProFrontlineFeedbackPayloadReqVO.SelectedDeviceReqVO().setDeviceId(id);
    }
}
