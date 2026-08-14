package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackPayloadReqVO;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineTeamDeviceOption;

import java.util.List;

public interface MesFrontlineDeviceParameterValidator {

    void validateSelectedDeviceAndParameters(
            Long routeProcessId,
            Long processId,
            MesProFrontlineFeedbackPayloadReqVO.SelectedDeviceReqVO selectedDevice,
            List<MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO> deviceParameterReadings);

    void validateSnapshotDeviceAndParameters(
            List<MesFrontlineTeamDeviceOption> snapshotDevices,
            MesProFrontlineFeedbackPayloadReqVO.SelectedDeviceReqVO selectedDevice,
            List<MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO> deviceParameterReadings);

}
