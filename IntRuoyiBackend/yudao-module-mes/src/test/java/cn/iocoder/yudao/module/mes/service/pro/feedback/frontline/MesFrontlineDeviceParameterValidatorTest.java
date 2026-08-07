package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackPayloadReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDeviceParameterRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamDeviceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamProcessDeviceDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDeviceParameterRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamDeviceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamProcessDeviceMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesFrontlineDeviceParameterValidatorTest {

    @Mock
    private MesProcessPoolTeamProcessDeviceMapper processDeviceMapper;
    @Mock
    private MesProcessPoolTeamDeviceMapper deviceMapper;
    @Mock
    private MesProcessPoolDeviceParameterRuleMapper parameterRuleMapper;

    @Test
    void allowsProcessWithoutConfiguredDeviceOrParameterReadings() {
        MesFrontlineDeviceParameterValidator validator = new MesFrontlineDeviceParameterValidatorImpl(
                processDeviceMapper, deviceMapper, parameterRuleMapper);

        assertDoesNotThrow(() -> validator.validateSelectedDeviceAndParameters(
                7101L, 6001L, null, List.of()));

        verifyNoInteractions(processDeviceMapper, deviceMapper, parameterRuleMapper);
    }

    @Test
    void rejectsSubmittedReadingForTextStandard() {
        when(deviceMapper.selectById(7001L)).thenReturn(MesProcessPoolTeamDeviceDO.builder()
                .id(7001L)
                .leaderUserId(3001L)
                .deviceCode("B09353")
                .deviceName("超声波清洗机")
                .deviceStatus("ENABLED")
                .enabled(Boolean.TRUE)
                .build());
        when(processDeviceMapper.selectList(any())).thenReturn(List.of(MesProcessPoolTeamProcessDeviceDO.builder()
                .leaderUserId(3001L)
                .processId(6001L)
                .deviceId(7001L)
                .enabled(Boolean.TRUE)
                .build()));
        when(parameterRuleMapper.selectList(any())).thenReturn(List.of(MesProcessPoolDeviceParameterRuleDO.builder()
                .leaderUserId(3001L)
                .routeProcessId(7101L)
                .processId(6001L)
                .deviceId(7001L)
                .parameterCode("cleaning-medium")
                .parameterName("清洗介质")
                .standardText("纯化水")
                .valueType("TEXT_STANDARD")
                .enabled(Boolean.TRUE)
                .build()));
        MesFrontlineDeviceParameterValidator validator = new MesFrontlineDeviceParameterValidatorImpl(
                processDeviceMapper, deviceMapper, parameterRuleMapper);

        MesProFrontlineFeedbackPayloadReqVO.SelectedDeviceReqVO selectedDevice =
                new MesProFrontlineFeedbackPayloadReqVO.SelectedDeviceReqVO().setDeviceId(7001L);
        MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO reading =
                new MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO()
                        .setDeviceId(7001L)
                        .setParameterCode("cleaning-medium")
                        .setValue(BigDecimal.ONE);

        assertThrows(ServiceException.class, () -> validator.validateSelectedDeviceAndParameters(
                7101L, 6001L, selectedDevice, List.of(reading)));
    }

    @Test
    void rejectsMissingNumericParameterReading() {
        when(deviceMapper.selectById(7001L)).thenReturn(enabledDevice());
        when(processDeviceMapper.selectList(any())).thenReturn(List.of(enabledProcessDevice()));
        when(parameterRuleMapper.selectList(any())).thenReturn(List.of(numericRule()));
        MesFrontlineDeviceParameterValidator validator = new MesFrontlineDeviceParameterValidatorImpl(
                processDeviceMapper, deviceMapper, parameterRuleMapper);

        MesProFrontlineFeedbackPayloadReqVO.SelectedDeviceReqVO selectedDevice =
                new MesProFrontlineFeedbackPayloadReqVO.SelectedDeviceReqVO().setDeviceId(7001L);

        assertThrows(ServiceException.class, () -> validator.validateSelectedDeviceAndParameters(
                7101L, 6001L, selectedDevice, List.of()));
    }

    @Test
    void retainsAboveUpperStatusForCompleteNumericReading() {
        when(deviceMapper.selectById(7001L)).thenReturn(enabledDevice());
        when(processDeviceMapper.selectList(any())).thenReturn(List.of(enabledProcessDevice()));
        when(parameterRuleMapper.selectList(any())).thenReturn(List.of(numericRule()));
        MesFrontlineDeviceParameterValidator validator = new MesFrontlineDeviceParameterValidatorImpl(
                processDeviceMapper, deviceMapper, parameterRuleMapper);

        MesProFrontlineFeedbackPayloadReqVO.SelectedDeviceReqVO selectedDevice =
                new MesProFrontlineFeedbackPayloadReqVO.SelectedDeviceReqVO().setDeviceId(7001L);
        MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO reading =
                new MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO()
                        .setDeviceId(7001L)
                        .setParameterCode("temperature")
                        .setValue(new BigDecimal("11"));

        validator.validateSelectedDeviceAndParameters(7101L, 6001L, selectedDevice, List.of(reading));

        assertEquals("ABOVE_UPPER", reading.getParameterStatus());
        assertEquals(new BigDecimal("10"), reading.getUpperLimit());
    }

    private static MesProcessPoolTeamDeviceDO enabledDevice() {
        return MesProcessPoolTeamDeviceDO.builder()
                .id(7001L)
                .leaderUserId(3001L)
                .deviceCode("B09353")
                .deviceName("超声波清洗机")
                .deviceStatus("ENABLED")
                .enabled(Boolean.TRUE)
                .build();
    }

    private static MesProcessPoolTeamProcessDeviceDO enabledProcessDevice() {
        return MesProcessPoolTeamProcessDeviceDO.builder()
                .leaderUserId(3001L)
                .processId(6001L)
                .deviceId(7001L)
                .enabled(Boolean.TRUE)
                .build();
    }

    private static MesProcessPoolDeviceParameterRuleDO numericRule() {
        return MesProcessPoolDeviceParameterRuleDO.builder()
                .leaderUserId(3001L)
                .routeProcessId(7101L)
                .processId(6001L)
                .deviceId(7001L)
                .parameterCode("temperature")
                .parameterName("温度")
                .unit("℃")
                .lowerLimit(BigDecimal.ZERO)
                .upperLimit(new BigDecimal("10"))
                .valueType("NUMERIC")
                .enabled(Boolean.TRUE)
                .build();
    }
}
