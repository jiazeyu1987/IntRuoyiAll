package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDeviceParameterRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamDeviceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamProcessDeviceDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDeviceParameterRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamDeviceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamProcessDeviceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderProcessConfigServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-08-06T08:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    @Mock
    private MesTeamLeaderLossReasonService lossReasonService;
    @Mock
    private MesProcessPoolTeamProcessDeviceMapper processDeviceMapper;
    @Mock
    private MesProcessPoolTeamDeviceMapper deviceMapper;
    @Mock
    private MesProcessPoolDeviceParameterRuleMapper parameterRuleMapper;
    @Mock
    private MesProProcessPoolEventMapper eventMapper;

    private MesTeamLeaderProcessConfigService service;

    @BeforeEach
    void setUp() {
        service = new MesTeamLeaderProcessConfigServiceImpl(lossReasonService, processDeviceMapper, deviceMapper,
                parameterRuleMapper, eventMapper, FIXED_CLOCK);
    }

    @Test
    void listProcessConfigs_returnsAuthorizedRouteRowsWithLossDevicesParametersAndAverage() {
        when(lossReasonService.listLossReasonRows(3001L)).thenReturn(List.of(
                new MesTeamLeaderLossReasonRow()
                        .setRouteId(9001L)
                        .setRouteCode("R-PCU")
                        .setRouteName("PCU 路线")
                        .setRouteProcessId(7101L)
                        .setProcessId(6001L)
                        .setProcessCode("P-CLEAN")
                        .setProcessName("精洗")
                        .setSort(10)
                        .setReasons(List.of(new MesTeamLeaderLossReasonItem()
                                .setId(8301L)
                                .setReasonCode("LOSS-001")
                                .setReasonName("正常损耗")
                                .setEnabled(Boolean.TRUE))),
                new MesTeamLeaderLossReasonRow()
                        .setRouteId(9002L)
                        .setRouteCode("R-OTHER")
                        .setRouteName("未授权外路线不应由依赖返回")
                        .setRouteProcessId(7102L)
                        .setProcessId(6002L)
                        .setProcessCode("P-PACK")
                        .setProcessName("包装")
                        .setSort(20)
                        .setReasons(List.of())));
        when(processDeviceMapper.selectList(any())).thenReturn(List.of(
                processDevice(8101L, 3001L, 6001L, 7001L),
                processDevice(8102L, 3001L, 6002L, 7002L)));
        when(deviceMapper.selectBatchIds(any())).thenReturn(List.of(
                teamDevice(7001L, 3001L, "D-001", "压力泵", "ENABLED", true),
                teamDevice(7002L, 3001L, "D-002", "封口机", "ENABLED", true)));
        when(parameterRuleMapper.selectList(any())).thenReturn(List.of(
                parameterRule(8401L, 7101L, 6001L, 7001L, "pressure", "压力", "MPa",
                        "10", "20", "15", "DECIMAL"),
                parameterRule(8402L, 7102L, 6002L, 7002L, "speed", "速度", "rpm",
                        "100", "200", "150", "DECIMAL")));
        when(eventMapper.selectList(any())).thenReturn(List.of(
                submitEvent(7101L, 7001L, "2026-08-05T08:00:00", "{\"equipmentParameters\":{\"pressure\":10}}"),
                submitEvent(7101L, 7001L, "2026-08-06T08:00:00", "{\"equipmentParameters\":{\"pressure\":20}}"),
                submitEvent(7101L, 7001L, "2026-08-04T08:00:00", "{\"equipmentParameters\":{\"pressure\":\"bad\"}}"),
                submitEvent(7101L, 7009L, "2026-08-06T08:00:00", "{\"equipmentParameters\":{\"pressure\":99}}"),
                submitEvent(7109L, 7001L, "2026-08-06T08:00:00", "{\"equipmentParameters\":{\"pressure\":88}}"),
                event("PQC_INSPECTION", 7101L, 7001L, "2026-08-06T08:00:00",
                        "{\"equipmentParameters\":{\"pressure\":77}}"),
                submitEvent(7101L, 7001L, "2026-07-01T08:00:00", "{\"equipmentParameters\":{\"pressure\":66}}")));

        List<MesTeamLeaderProcessConfigRow> rows = service.listProcessConfigs(3001L);

        assertEquals(2, rows.size());
        MesTeamLeaderProcessConfigRow first = rows.get(0);
        assertEquals(7101L, first.getRouteProcessId());
        assertEquals("PCU 路线", first.getRouteName());
        assertEquals("精洗", first.getProcessName());
        assertEquals(1, first.getLossReasons().size());
        assertEquals("LOSS-001", first.getLossReasons().get(0).getReasonCode());
        assertEquals(1, first.getDevices().size());
        assertEquals("压力泵", first.getDevices().get(0).getDeviceName());
        assertEquals(1, first.getDevices().get(0).getParameters().size());
        MesTeamLeaderProcessConfigParameter parameter = first.getDevices().get(0).getParameters().get(0);
        assertEquals(8401L, parameter.getRuleId());
        assertEquals("pressure", parameter.getParameterCode());
        assertEquals(new BigDecimal("15"), parameter.getTargetValue());
        assertEquals(new BigDecimal("15.000000"), parameter.getActualAverage());
        assertEquals(2, parameter.getSampleCount());
        assertEquals(30, parameter.getStatisticsWindowDays());
        assertEquals(LocalDateTime.of(2026, 7, 7, 16, 0), parameter.getStatisticsStartTime());
        assertEquals(LocalDateTime.of(2026, 8, 6, 16, 0), parameter.getStatisticsEndTime());
    }

    @Test
    void listProcessConfigs_returnsNullAverageAndZeroSampleWhenNoNumericSubmitSamplesExist() {
        when(lossReasonService.listLossReasonRows(3001L)).thenReturn(List.of(new MesTeamLeaderLossReasonRow()
                .setRouteId(9001L)
                .setRouteProcessId(7101L)
                .setProcessId(6001L)
                .setSort(10)
                .setReasons(List.of())));
        when(processDeviceMapper.selectList(any())).thenReturn(List.of(processDevice(8101L, 3001L, 6001L, 7001L)));
        when(deviceMapper.selectBatchIds(any())).thenReturn(List.of(
                teamDevice(7001L, 3001L, "D-001", "压力泵", "ENABLED", true)));
        when(parameterRuleMapper.selectList(any())).thenReturn(List.of(
                parameterRule(8401L, 7101L, 6001L, 7001L, "pressure", "压力", "MPa",
                        "10", "20", "15", "DECIMAL")));
        when(eventMapper.selectList(any())).thenReturn(List.of(
                submitEvent(7101L, 7001L, "2026-08-05T08:00:00",
                        "{\"equipmentParameters\":{\"pressure\":\"not-a-number\"}}")));

        MesTeamLeaderProcessConfigParameter parameter = service.listProcessConfigs(3001L)
                .get(0).getDevices().get(0).getParameters().get(0);

        assertNull(parameter.getActualAverage());
        assertEquals(0, parameter.getSampleCount());
        assertEquals(new BigDecimal("15"), parameter.getTargetValue(),
                "targetValue must stay separate from the read-only actualAverage");
        assertTrue(parameter.getStatisticsStartTime().isBefore(parameter.getStatisticsEndTime()));
    }

    private static MesProcessPoolTeamProcessDeviceDO processDevice(Long id, Long leaderUserId, Long processId,
                                                                   Long deviceId) {
        return MesProcessPoolTeamProcessDeviceDO.builder()
                .id(id)
                .leaderUserId(leaderUserId)
                .processId(processId)
                .deviceId(deviceId)
                .enabled(Boolean.TRUE)
                .build();
    }

    private static MesProcessPoolTeamDeviceDO teamDevice(Long id, Long leaderUserId, String deviceCode,
                                                         String deviceName, String deviceStatus, boolean enabled) {
        return MesProcessPoolTeamDeviceDO.builder()
                .id(id)
                .leaderUserId(leaderUserId)
                .deviceCode(deviceCode)
                .deviceName(deviceName)
                .deviceStatus(deviceStatus)
                .enabled(enabled)
                .build();
    }

    private static MesProcessPoolDeviceParameterRuleDO parameterRule(Long id, Long routeProcessId, Long processId,
                                                                     Long deviceId, String parameterCode,
                                                                     String parameterName, String unit,
                                                                     String lowerLimit, String upperLimit,
                                                                     String targetValue, String valueType) {
        return MesProcessPoolDeviceParameterRuleDO.builder()
                .id(id)
                .leaderUserId(3001L)
                .routeProcessId(routeProcessId)
                .processId(processId)
                .deviceId(deviceId)
                .parameterCode(parameterCode)
                .parameterName(parameterName)
                .unit(unit)
                .lowerLimit(new BigDecimal(lowerLimit))
                .upperLimit(new BigDecimal(upperLimit))
                .defaultValue(new BigDecimal(targetValue))
                .valueType(valueType)
                .enabled(Boolean.TRUE)
                .build();
    }

    private static MesProProcessPoolEventDO submitEvent(Long routeProcessId, Long deviceId, String serverSubmitTime,
                                                        String rawPayload) {
        return event(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT, routeProcessId, deviceId,
                serverSubmitTime, rawPayload);
    }

    private static MesProProcessPoolEventDO event(String eventType, Long routeProcessId, Long deviceId,
                                                  String serverSubmitTime, String rawPayload) {
        return MesProProcessPoolEventDO.builder()
                .eventType(eventType)
                .routeProcessId(routeProcessId)
                .deviceId(deviceId)
                .serverSubmitTime(LocalDateTime.parse(serverSubmitTime))
                .rawPayload(rawPayload)
                .build();
    }
}
