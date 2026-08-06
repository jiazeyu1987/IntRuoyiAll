package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDefectReasonDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDeviceParameterRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamDeviceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeProfileDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamProcessDeviceDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDefectReasonMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDeviceParameterRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamDeviceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamEmployeeBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamEmployeeProfileMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamProcessDeviceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesFrontlineRuntimeConfigServiceTest {

    private static final Long LOGIN_USER_ID = 9001L;
    private static final Long ROUTE_ID = 101L;
    private static final Long ROUTE_PROCESS_ID = 1001L;
    private static final Long PROCESS_ID = 201L;

    @Mock
    private MesFrontlineDeviceAccountContextService contextService;
    @Mock
    private MesProcessPoolTeamEmployeeBindingMapper employeeBindingMapper;
    @Mock
    private MesProcessPoolTeamEmployeeProfileMapper employeeProfileMapper;
    @Mock
    private MesProcessPoolTeamProcessDeviceMapper processDeviceMapper;
    @Mock
    private MesProcessPoolTeamDeviceMapper deviceMapper;
    @Mock
    private MesProcessPoolDeviceParameterRuleMapper parameterRuleMapper;
    @Mock
    private MesProcessPoolDefectReasonMapper defectReasonMapper;

    private MesFrontlineRuntimeConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MesFrontlineRuntimeConfigServiceImpl(contextService, employeeBindingMapper,
                employeeProfileMapper, processDeviceMapper, deviceMapper, parameterRuleMapper, defectReasonMapper);
    }

    @Test
    void getRuntimeConfig_returnsTeamLeaderConfiguredRuntimeOptions() {
        when(contextService.requireAuthorizedProcess(LOGIN_USER_ID, ROUTE_ID, ROUTE_PROCESS_ID, PROCESS_ID))
                .thenReturn(new MesFrontlineRouteProcessCandidate(ROUTE_ID, "R-101", "Route 101",
                        ROUTE_PROCESS_ID, PROCESS_ID, "P-201", "精洗", 10,
                        7001L, "D-001", "压力泵", 301L, "WS-301", "精洗工位"));
        when(employeeBindingMapper.selectList(any())).thenReturn(List.of(
                employeeBinding(9002L, 8802L),
                employeeBinding(9001L, 8801L)));
        when(employeeProfileMapper.selectList(any())).thenReturn(List.of(
                employeeProfile(8801L, 9001L, null, "TMP-001", "临时工甲", "临时工甲-A", "TEMPORARY", true),
                employeeProfile(8802L, 9002L, 10002L, "E-002", "其它班组员工", "其它班组员工", "FORMAL", true)));
        when(processDeviceMapper.selectList(any())).thenReturn(List.of(
                processDevice(9001L, 7001L),
                processDevice(9001L, 7002L),
                processDevice(9001L, 7003L)));
        when(deviceMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                teamDevice(7001L, 9001L, "D-001", "压力泵", "ENABLED", true),
                teamDevice(7002L, 9001L, "D-002", "报修泵", "REPAIRING", true),
                teamDevice(7003L, 9001L, "D-003", "禁用泵", "DISABLED", false)));
        when(parameterRuleMapper.selectList(any())).thenReturn(List.of(
                parameterRule(7001L, ROUTE_PROCESS_ID, "pressure", "压力", "MPa",
                        "10", "20", "15", "DECIMAL"),
                parameterRule(7001L, null, "legacy-pressure", "历史空路线压力", "MPa",
                        "10", "20", "15", "DECIMAL"),
                parameterRule(7001L, 2002L, "temperature", "温度", "℃",
                        "30", "60", "45", "DECIMAL")));
        when(defectReasonMapper.selectList(any())).thenReturn(List.of(
                defectReason(8301L, ROUTE_PROCESS_ID, "LOSS", "LOSS-001", "正常损耗"),
                defectReason(8302L, 2002L, "LOSS", "LOSS-002", "其它工序损耗")));

        MesFrontlineRuntimeConfig config = service.getRuntimeConfig(LOGIN_USER_ID, ROUTE_ID,
                ROUTE_PROCESS_ID, PROCESS_ID);

        assertEquals(ROUTE_ID, config.routeId());
        assertEquals(ROUTE_PROCESS_ID, config.routeProcessId());
        assertEquals(PROCESS_ID, config.processId());
        assertEquals(1, config.employees().size());
        assertEquals("临时工甲-A", config.employees().get(0).employeeName());
        assertNull(config.employees().get(0).systemUserId());
        assertEquals(1, config.devices().size());
        assertEquals("压力泵", config.devices().get(0).deviceName());
        assertEquals("ENABLED", config.devices().get(0).deviceStatus());
        assertEquals(1, config.devices().get(0).parameters().size());
        assertEquals("pressure", config.devices().get(0).parameters().get(0).parameterCode());
        assertEquals(new BigDecimal("15"), config.devices().get(0).parameters().get(0).defaultValue());
        assertEquals(1, config.defectReasons().size());
        assertEquals("正常损耗", config.defectReasons().get(0).reasonName());
    }

    @Test
    void getRuntimeConfig_returnsEnabledLeaderPersonnelProfilesInsteadOfOnlyProcessBindings() {
        when(contextService.requireAuthorizedProcess(LOGIN_USER_ID, ROUTE_ID, ROUTE_PROCESS_ID, PROCESS_ID))
                .thenReturn(new MesFrontlineRouteProcessCandidate(ROUTE_ID, "R-101", "Route 101",
                        ROUTE_PROCESS_ID, PROCESS_ID, "P-201", "精洗", 10,
                        7001L, "D-001", "压力泵", 301L, "WS-301", "精洗工位"));
        when(employeeBindingMapper.selectList(any())).thenReturn(List.of(
                employeeBinding(9001L, 8801L)));
        when(employeeProfileMapper.selectList(any())).thenReturn(List.of(
                employeeProfile(8801L, 9001L, null, "TMP-001", "临时工甲", "临时工甲-A", "TEMPORARY", true),
                employeeProfile(8803L, 9001L, 10003L, "USER-10003", "正式工乙", "正式工乙", "FORMAL", true),
                employeeProfile(8804L, 9001L, 10004L, "USER-10004", "禁用员工", "禁用员工", "FORMAL", false),
                employeeProfile(8805L, 9002L, 10005L, "USER-10005", "其它组员工", "其它组员工", "FORMAL", true)));
        when(processDeviceMapper.selectList(any())).thenReturn(List.of(processDevice(9001L, 7001L)));
        when(deviceMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                teamDevice(7001L, 9001L, "D-001", "压力泵", "ENABLED", true)));
        when(parameterRuleMapper.selectList(any())).thenReturn(List.of());
        when(defectReasonMapper.selectList(any())).thenReturn(List.of());

        MesFrontlineRuntimeConfig config = service.getRuntimeConfig(LOGIN_USER_ID, ROUTE_ID,
                ROUTE_PROCESS_ID, PROCESS_ID);

        assertEquals(2, config.employees().size());
        assertEquals(8801L, config.employees().get(0).employeeProfileId());
        assertEquals(8803L, config.employees().get(1).employeeProfileId());
        assertEquals(10003L, config.employees().get(1).systemUserId());
    }

    @Test
    void getRuntimeConfig_returnsEnabledLossReasonsByRouteProcessWithoutLeaderOwnership() {
        when(contextService.requireAuthorizedProcess(LOGIN_USER_ID, ROUTE_ID, ROUTE_PROCESS_ID, PROCESS_ID))
                .thenReturn(new MesFrontlineRouteProcessCandidate(ROUTE_ID, "R-101", "Route 101",
                        ROUTE_PROCESS_ID, PROCESS_ID, "P-201", "精洗", 10,
                        null, null, null, 301L, "WS-301", "精洗工位"));
        when(employeeBindingMapper.selectList(any())).thenReturn(List.of());
        when(processDeviceMapper.selectList(any())).thenReturn(List.of());
        when(defectReasonMapper.selectList(any())).thenReturn(List.of(
                defectReason(8301L, ROUTE_PROCESS_ID, "LOSS", "LOSS-001", "正常损耗").setLeaderUserId(9999L),
                defectReason(8302L, 2002L, "LOSS", "LOSS-002", "其它工序损耗").setLeaderUserId(9999L),
                defectReason(8303L, ROUTE_PROCESS_ID, "LOSS", "LOSS-003", "停用损耗")
                        .setLeaderUserId(9999L).setEnabled(Boolean.FALSE)));

        MesFrontlineRuntimeConfig config = service.getRuntimeConfig(LOGIN_USER_ID, ROUTE_ID,
                ROUTE_PROCESS_ID, PROCESS_ID);

        assertEquals(1, config.defectReasons().size());
        assertEquals(8301L, config.defectReasons().get(0).reasonId());
        assertEquals("LOSS-001", config.defectReasons().get(0).reasonCode());
    }

    private static MesProcessPoolTeamEmployeeBindingDO employeeBinding(Long leaderUserId, Long employeeProfileId) {
        return MesProcessPoolTeamEmployeeBindingDO.builder()
                .leaderUserId(leaderUserId)
                .processId(PROCESS_ID)
                .employeeProfileId(employeeProfileId)
                .enabled(Boolean.TRUE)
                .build();
    }

    private static MesProcessPoolTeamEmployeeProfileDO employeeProfile(Long id, Long leaderUserId, Long systemUserId,
                                                                       String employeeCode, String employeeName,
                                                                       String displayName, String employeeType,
                                                                       boolean enabled) {
        return MesProcessPoolTeamEmployeeProfileDO.builder()
                .id(id)
                .leaderUserId(leaderUserId)
                .systemUserId(systemUserId)
                .employeeCode(employeeCode)
                .employeeName(employeeName)
                .displayName(displayName)
                .employeeType(employeeType)
                .enabled(enabled)
                .build();
    }

    private static MesProcessPoolTeamProcessDeviceDO processDevice(Long leaderUserId, Long deviceId) {
        return MesProcessPoolTeamProcessDeviceDO.builder()
                .leaderUserId(leaderUserId)
                .processId(PROCESS_ID)
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

    private static MesProcessPoolDeviceParameterRuleDO parameterRule(Long deviceId, Long routeProcessId,
                                                                     String parameterCode, String parameterName,
                                                                     String unit, String lowerLimit,
                                                                     String upperLimit, String defaultValue,
                                                                     String valueType) {
        return MesProcessPoolDeviceParameterRuleDO.builder()
                .leaderUserId(9001L)
                .routeProcessId(routeProcessId)
                .processId(PROCESS_ID)
                .deviceId(deviceId)
                .parameterCode(parameterCode)
                .parameterName(parameterName)
                .unit(unit)
                .lowerLimit(new BigDecimal(lowerLimit))
                .upperLimit(new BigDecimal(upperLimit))
                .defaultValue(new BigDecimal(defaultValue))
                .valueType(valueType)
                .enabled(Boolean.TRUE)
                .build();
    }

    private static MesProcessPoolDefectReasonDO defectReason(Long id, Long routeProcessId, String reasonType,
                                                             String reasonCode, String reasonName) {
        return MesProcessPoolDefectReasonDO.builder()
                .id(id)
                .leaderUserId(9001L)
                .routeProcessId(routeProcessId)
                .processId(PROCESS_ID)
                .reasonType(reasonType)
                .reasonCode(reasonCode)
                .reasonName(reasonName)
                .enabled(Boolean.TRUE)
                .build();
    }

}
