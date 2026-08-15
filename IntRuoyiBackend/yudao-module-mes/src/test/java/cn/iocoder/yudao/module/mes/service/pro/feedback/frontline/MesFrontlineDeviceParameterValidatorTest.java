package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackPayloadReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDeviceParameterRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamDeviceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamProcessDeviceDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDeviceParameterRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamDeviceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamProcessDeviceMapper;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineDeviceParameterOption;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineTeamDeviceOption;
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

    @Test
    void acceptsBinaryBooleanReadingsAndRejectsOtherValues() {
        when(deviceMapper.selectById(7001L)).thenReturn(enabledDevice());
        when(processDeviceMapper.selectList(any())).thenReturn(List.of(enabledProcessDevice()));
        when(parameterRuleMapper.selectList(any())).thenReturn(List.of(booleanRule()));
        MesFrontlineDeviceParameterValidator validator = new MesFrontlineDeviceParameterValidatorImpl(
                processDeviceMapper, deviceMapper, parameterRuleMapper);

        MesProFrontlineFeedbackPayloadReqVO.SelectedDeviceReqVO selectedDevice =
                new MesProFrontlineFeedbackPayloadReqVO.SelectedDeviceReqVO().setDeviceId(7001L);
        MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO unchecked =
                booleanReading(BigDecimal.ZERO);
        MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO checked =
                booleanReading(BigDecimal.ONE);
        MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO invalid =
                booleanReading(new BigDecimal("2"));

        assertDoesNotThrow(() -> validator.validateSelectedDeviceAndParameters(
                7101L, 6001L, selectedDevice, List.of(unchecked)));
        assertDoesNotThrow(() -> validator.validateSelectedDeviceAndParameters(
                7101L, 6001L, selectedDevice, List.of(checked)));
        assertThrows(ServiceException.class, () -> validator.validateSelectedDeviceAndParameters(
                7101L, 6001L, selectedDevice, List.of(invalid)));
    }

    @Test
    void ignoresNullRouteProcessParameterRulesThatRuntimeConfigDoesNotShow() {
        when(deviceMapper.selectById(7001L)).thenReturn(enabledDevice());
        when(processDeviceMapper.selectList(any())).thenReturn(List.of(enabledProcessDevice()));
        when(parameterRuleMapper.selectList(any())).thenReturn(List.of(
                numericRule(),
                MesProcessPoolDeviceParameterRuleDO.builder()
                        .leaderUserId(3001L)
                        .routeProcessId(null)
                        .processId(6001L)
                        .deviceId(7001L)
                        .parameterCode("legacy-temperature")
                        .parameterName("历史温度")
                        .unit("℃")
                        .lowerLimit(BigDecimal.ZERO)
                        .upperLimit(new BigDecimal("10"))
                        .valueType("NUMERIC")
                        .enabled(Boolean.TRUE)
                        .build()));
        MesFrontlineDeviceParameterValidator validator = new MesFrontlineDeviceParameterValidatorImpl(
                processDeviceMapper, deviceMapper, parameterRuleMapper);

        MesProFrontlineFeedbackPayloadReqVO.SelectedDeviceReqVO selectedDevice =
                new MesProFrontlineFeedbackPayloadReqVO.SelectedDeviceReqVO().setDeviceId(7001L);
        MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO reading =
                new MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO()
                        .setDeviceId(7001L)
                        .setParameterCode("temperature")
                        .setValue(new BigDecimal("5"));

        assertDoesNotThrow(() -> validator.validateSelectedDeviceAndParameters(
                7101L, 6001L, selectedDevice, List.of(reading)));
    }

    @Test
    void validatesDeviceAndParameterReadingFromSnapshotWithoutMapperReads() {
        MesFrontlineDeviceParameterValidator validator = new MesFrontlineDeviceParameterValidatorImpl(
                processDeviceMapper, deviceMapper, parameterRuleMapper);
        MesFrontlineTeamDeviceOption snapshotDevice = new MesFrontlineTeamDeviceOption(
                7001L, "B09353", "超声波清洗机", "ENABLED", List.of(
                new MesFrontlineDeviceParameterOption("temperature", "温度", "℃",
                        BigDecimal.ZERO, new BigDecimal("10"), new BigDecimal("5"), "DECIMAL",
                        null, List.of(), null, 1)));
        MesProFrontlineFeedbackPayloadReqVO.SelectedDeviceReqVO selectedDevice =
                new MesProFrontlineFeedbackPayloadReqVO.SelectedDeviceReqVO().setDeviceId(7001L);
        MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO reading =
                new MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO()
                        .setDeviceId(7001L).setParameterCode("temperature").setValue(new BigDecimal("11"));

        validator.validateSnapshotDeviceAndParameters(List.of(snapshotDevice), selectedDevice, List.of(reading));

        assertEquals("ABOVE_UPPER", reading.getParameterStatus());
        assertEquals("B09353", reading.getDeviceCode());
        assertEquals(new BigDecimal("10"), reading.getUpperLimit());
        verifyNoInteractions(processDeviceMapper, deviceMapper, parameterRuleMapper);
    }

    @Test
    void rejectsParameterThatIsAbsentFromSnapshot() {
        MesFrontlineDeviceParameterValidator validator = new MesFrontlineDeviceParameterValidatorImpl(
                processDeviceMapper, deviceMapper, parameterRuleMapper);
        MesFrontlineTeamDeviceOption snapshotDevice = new MesFrontlineTeamDeviceOption(
                7001L, "B09353", "超声波清洗机", "ENABLED", List.of());
        MesProFrontlineFeedbackPayloadReqVO.SelectedDeviceReqVO selectedDevice =
                new MesProFrontlineFeedbackPayloadReqVO.SelectedDeviceReqVO().setDeviceId(7001L);
        MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO reading =
                new MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO()
                        .setDeviceId(7001L).setParameterCode("live-added").setValue(BigDecimal.ONE);

        assertThrows(ServiceException.class, () -> validator.validateSnapshotDeviceAndParameters(
                List.of(snapshotDevice), selectedDevice, List.of(reading)));
        verifyNoInteractions(processDeviceMapper, deviceMapper, parameterRuleMapper);
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

    private static MesProcessPoolDeviceParameterRuleDO booleanRule() {
        return MesProcessPoolDeviceParameterRuleDO.builder()
                .leaderUserId(3001L)
                .routeProcessId(7101L)
                .processId(6001L)
                .deviceId(7001L)
                .parameterCode("METERING_VALID")
                .parameterName("在计量效期内")
                .defaultValue(BigDecimal.ZERO)
                .standardText("是否在计量效期内")
                .valueType(MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_BOOLEAN)
                .enabled(Boolean.TRUE)
                .build();
    }

    private static MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO booleanReading(
            BigDecimal value) {
        return new MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO()
                .setDeviceId(7001L)
                .setParameterCode("METERING_VALID")
                .setValue(value);
    }
}
