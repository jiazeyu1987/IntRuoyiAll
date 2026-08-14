package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderProcessConfigListReqVO;
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
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Constructor;
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
    void runtimeConstructor_hasAutowiredAnnotationSoSpringDoesNotRequireDefaultConstructor() throws NoSuchMethodException {
        Constructor<MesTeamLeaderProcessConfigServiceImpl> runtimeConstructor =
                MesTeamLeaderProcessConfigServiceImpl.class.getConstructor(
                        MesTeamLeaderLossReasonService.class,
                        MesProcessPoolTeamProcessDeviceMapper.class,
                        MesProcessPoolTeamDeviceMapper.class,
                        MesProcessPoolDeviceParameterRuleMapper.class,
                        MesProProcessPoolEventMapper.class);

        assertTrue(runtimeConstructor.isAnnotationPresent(Autowired.class),
                "Spring must use the public runtime constructor instead of looking for a default constructor");
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

        List<MesTeamLeaderProcessConfigRow> rows = service.listProcessConfigs(
                3001L, new MesTeamLeaderProcessConfigListReqVO());

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
        assertEquals("10-20MPa，目标15MPa", parameter.getStandardText());
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

        MesTeamLeaderProcessConfigParameter parameter = service.listProcessConfigs(
                        3001L, new MesTeamLeaderProcessConfigListReqVO())
                .get(0).getDevices().get(0).getParameters().get(0);

        assertNull(parameter.getActualAverage());
        assertEquals(0, parameter.getSampleCount());
        assertEquals(new BigDecimal("15"), parameter.getTargetValue(),
                "targetValue must stay separate from the read-only actualAverage");
        assertTrue(parameter.getStatisticsStartTime().isBefore(parameter.getStatisticsEndTime()));
    }

    @Test
    void listProcessConfigs_returnsTextStandardWithoutNumericStatistics() {
        when(lossReasonService.listLossReasonRows(3001L)).thenReturn(List.of(new MesTeamLeaderLossReasonRow()
                .setRouteId(9001L)
                .setRouteProcessId(7101L)
                .setProcessId(6001L)
                .setSort(10)
                .setReasons(List.of())));
        when(processDeviceMapper.selectList(any())).thenReturn(List.of(processDevice(8101L, 3001L, 6001L, 7001L)));
        when(deviceMapper.selectBatchIds(any())).thenReturn(List.of(
                teamDevice(7001L, 3001L, "D-001", "清洗机", "ENABLED", true)));
        when(parameterRuleMapper.selectList(any())).thenReturn(List.of(
                MesProcessPoolDeviceParameterRuleDO.builder()
                        .id(8401L)
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

        MesTeamLeaderProcessConfigParameter parameter = service.listProcessConfigs(
                        3001L, new MesTeamLeaderProcessConfigListReqVO())
                .get(0).getDevices().get(0).getParameters().get(0);

        assertEquals("纯化水", parameter.getStandardText());
        assertNull(parameter.getLowerLimit());
        assertNull(parameter.getTargetValue());
        assertNull(parameter.getUpperLimit());
        assertNull(parameter.getActualAverage());
        assertEquals(0, parameter.getSampleCount());
        assertEquals(30, parameter.getStatisticsWindowDays());
    }

    @Test
    void listProcessConfigs_filtersFiveFieldsWithCaseInsensitiveIntersectionAndKeepsFullRows() {
        when(lossReasonService.listLossReasonRows(3001L)).thenReturn(List.of(
                new MesTeamLeaderLossReasonRow()
                        .setRouteId(9001L)
                        .setRouteCode("R-PCU")
                        .setRouteName("球囊扩张导管")
                        .setRouteProcessId(7101L)
                        .setProcessId(6001L)
                        .setProcessCode("P-FORM")
                        .setProcessName("吹球囊成型")
                        .setSort(10)
                        .setReasons(List.of(new MesTeamLeaderLossReasonItem()
                                .setId(8301L)
                                .setReasonCode("LOSS-001")
                                .setReasonName("黑点")
                                .setEnabled(Boolean.TRUE))),
                new MesTeamLeaderLossReasonRow()
                        .setRouteId(9002L)
                        .setRouteCode("R-PACK")
                        .setRouteName("包装路线")
                        .setRouteProcessId(7102L)
                        .setProcessId(6002L)
                        .setProcessCode("P-PACK")
                        .setProcessName("包装")
                        .setSort(20)
                        .setReasons(List.of(new MesTeamLeaderLossReasonItem()
                                .setId(8302L)
                                .setReasonCode("LOSS-002")
                                .setReasonName("包装破损")
                                .setEnabled(Boolean.TRUE)))));
        when(processDeviceMapper.selectList(any())).thenReturn(List.of(
                processDevice(8101L, 3001L, 6001L, 7001L),
                processDevice(8103L, 3001L, 6001L, 7003L),
                processDevice(8102L, 3001L, 6002L, 7002L)));
        when(deviceMapper.selectBatchIds(any())).thenReturn(List.of(
                teamDevice(7001L, 3001L, "EQ-A", "成型机A", "ENABLED", true),
                teamDevice(7003L, 9999L, "EQ-FOREIGN", "其他组长设备", "ENABLED", true),
                teamDevice(7002L, 3001L, "EQ-B", "包装机B", "ENABLED", true)));
        when(parameterRuleMapper.selectList(any())).thenReturn(List.of(
                parameterRule(8401L, 7101L, 6001L, 7001L, "TEMPERATURE", "温度", "℃",
                        "10", "20", "15", "DECIMAL"),
                parameterRule(8402L, 7102L, 6002L, 7002L, "SPEED", "速度", "rpm",
                        "100", "200", "150", "DECIMAL")));
        when(eventMapper.selectList(any())).thenReturn(List.of());

        assertRouteProcessIds(service.listProcessConfigs(3001L,
                new MesTeamLeaderProcessConfigListReqVO().setRouteKeyword(" r-pcu ")), 7101L);
        assertRouteProcessIds(service.listProcessConfigs(3001L,
                new MesTeamLeaderProcessConfigListReqVO().setProcessKeyword("球囊")), 7101L);
        assertRouteProcessIds(service.listProcessConfigs(3001L,
                new MesTeamLeaderProcessConfigListReqVO().setLossReasonKeyword("黑点")), 7101L);
        assertRouteProcessIds(service.listProcessConfigs(3001L,
                new MesTeamLeaderProcessConfigListReqVO().setDeviceKeyword("eq-a")), 7101L);
        assertRouteProcessIds(service.listProcessConfigs(3001L,
                new MesTeamLeaderProcessConfigListReqVO().setParameterKeyword("temperature")), 7101L);

        MesTeamLeaderProcessConfigListReqVO intersection = new MesTeamLeaderProcessConfigListReqVO()
                .setRouteKeyword("球囊")
                .setProcessKeyword("成型")
                .setLossReasonKeyword("黑点")
                .setDeviceKeyword("成型机")
                .setParameterKeyword("温度");
        List<MesTeamLeaderProcessConfigRow> intersectionRows = service.listProcessConfigs(3001L, intersection);
        assertRouteProcessIds(intersectionRows, 7101L);
        assertEquals(1, intersectionRows.get(0).getLossReasons().size());
        assertEquals(1, intersectionRows.get(0).getDevices().size());
        assertEquals(1, intersectionRows.get(0).getDevices().get(0).getParameters().size());

        assertRouteProcessIds(service.listProcessConfigs(3001L,
                new MesTeamLeaderProcessConfigListReqVO().setRouteKeyword("   ")), 7101L, 7102L);
        assertTrue(service.listProcessConfigs(3001L,
                new MesTeamLeaderProcessConfigListReqVO().setDeviceKeyword("不存在设备")).isEmpty());
        assertTrue(service.listProcessConfigs(3001L,
                new MesTeamLeaderProcessConfigListReqVO().setDeviceKeyword("EQ-FOREIGN")).isEmpty());
    }

    private static void assertRouteProcessIds(List<MesTeamLeaderProcessConfigRow> rows,
                                              Long... expectedRouteProcessIds) {
        assertEquals(List.of(expectedRouteProcessIds), rows.stream()
                .map(MesTeamLeaderProcessConfigRow::getRouteProcessId)
                .toList());
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
                .standardText(lowerLimit + "-" + upperLimit + unit + "，目标" + targetValue + unit)
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
