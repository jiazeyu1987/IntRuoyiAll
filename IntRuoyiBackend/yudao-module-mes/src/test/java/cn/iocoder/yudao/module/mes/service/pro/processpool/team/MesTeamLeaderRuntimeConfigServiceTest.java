package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDefectReasonDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDeviceParameterRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamDeviceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeProfileDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamProcessDeviceDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDefectReasonMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDeviceParameterRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamDeviceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamEmployeeBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamEmployeeProfileMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamProcessDeviceMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderRuntimeConfigServiceTest {

    @Mock
    private MesTeamLeaderScopeService scopeService;
    @Mock
    private MesProcessPoolTeamEmployeeProfileMapper employeeProfileMapper;
    @Mock
    private MesProcessPoolTeamEmployeeBindingMapper employeeBindingMapper;
    @Mock
    private MesProcessPoolTeamDeviceMapper deviceMapper;
    @Mock
    private MesProcessPoolTeamProcessDeviceMapper processDeviceMapper;
    @Mock
    private MesProcessPoolDeviceParameterRuleMapper parameterRuleMapper;
    @Mock
    private MesProcessPoolDefectReasonMapper defectReasonMapper;
    @Mock
    private MesProcessPoolTeamMaintenanceAuditMapper auditMapper;

    private MesTeamLeaderRuntimeConfigService service;

    @BeforeEach
    void setUp() {
        service = new MesTeamLeaderRuntimeConfigServiceImpl(scopeService, employeeProfileMapper, employeeBindingMapper,
                deviceMapper, processDeviceMapper, parameterRuleMapper, defectReasonMapper, auditMapper);
    }

    @Test
    void shouldCreateTemporaryEmployeeWithoutSystemUserAndBindToProcess() {
        when(employeeProfileMapper.insert(any(MesProcessPoolTeamEmployeeProfileDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolTeamEmployeeProfileDO.class).setId(8801L);
            return 1;
        });

        Long employeeProfileId = service.createEmployee(MesTeamEmployeeProfileSaveReqBO.builder()
                .leaderUserId(3001L)
                .employeeCode("TMP-001")
                .employeeName("临时工甲")
                .employeeType("TEMPORARY")
                .build());

        assertEquals(8801L, employeeProfileId);
        ArgumentCaptor<MesProcessPoolTeamEmployeeProfileDO> profileCaptor =
                ArgumentCaptor.forClass(MesProcessPoolTeamEmployeeProfileDO.class);
        verify(employeeProfileMapper).insert(profileCaptor.capture());
        assertNull(profileCaptor.getValue().getSystemUserId());
        assertEquals("TEMPORARY", profileCaptor.getValue().getEmployeeType());
        assertTrue(profileCaptor.getValue().getEnabled());

        when(employeeProfileMapper.selectById(8801L)).thenReturn(profileCaptor.getValue());
        when(employeeBindingMapper.insert(any(MesProcessPoolTeamEmployeeBindingDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolTeamEmployeeBindingDO.class).setId(8201L);
            return 1;
        });

        Long bindingId = service.bindEmployeeToProcess(MesTeamProcessEmployeeBindingSaveReqBO.builder()
                .leaderUserId(3001L)
                .employeeProfileId(8801L)
                .processId(6001L)
                .build());

        assertEquals(8201L, bindingId);
        verify(scopeService).assertCanMaintainProcess(3001L, 6001L);
        ArgumentCaptor<MesProcessPoolTeamEmployeeBindingDO> bindingCaptor =
                ArgumentCaptor.forClass(MesProcessPoolTeamEmployeeBindingDO.class);
        verify(employeeBindingMapper).insert(bindingCaptor.capture());
        assertEquals(8801L, bindingCaptor.getValue().getEmployeeProfileId());
        assertNull(bindingCaptor.getValue().getEmployeeUserId());
        verify(auditMapper, org.mockito.Mockito.atLeastOnce()).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));
    }

    @Test
    void shouldCreateDeviceAndRejectRepairingDeviceWhenBindingProcess() {
        when(deviceMapper.insert(any(MesProcessPoolTeamDeviceDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolTeamDeviceDO.class).setId(7001L);
            return 1;
        });

        Long deviceId = service.createDevice(MesTeamDeviceSaveReqBO.builder()
                .leaderUserId(3001L)
                .deviceCode("D-001")
                .deviceName("压力泵")
                .deviceStatus("ENABLED")
                .build());

        assertEquals(7001L, deviceId);
        ArgumentCaptor<MesProcessPoolTeamDeviceDO> deviceCaptor =
                ArgumentCaptor.forClass(MesProcessPoolTeamDeviceDO.class);
        verify(deviceMapper).insert(deviceCaptor.capture());
        assertEquals("ENABLED", deviceCaptor.getValue().getDeviceStatus());
        assertTrue(deviceCaptor.getValue().getEnabled());

        when(deviceMapper.selectById(7001L)).thenReturn(MesProcessPoolTeamDeviceDO.builder()
                .id(7001L)
                .leaderUserId(3001L)
                .deviceCode("D-001")
                .deviceName("压力泵")
                .deviceStatus("REPAIRING")
                .enabled(Boolean.TRUE)
                .build());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.bindDeviceToProcess(
                MesTeamProcessDeviceBindingSaveReqBO.builder()
                        .leaderUserId(3001L)
                        .deviceId(7001L)
                        .processId(6001L)
                        .build()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_DEVICE_UNAVAILABLE.getCode(), ex.getCode());
        verify(processDeviceMapper, never()).insert(any(MesProcessPoolTeamProcessDeviceDO.class));
    }

    @Test
    void shouldUpdateDeviceStatusForRepairDisableAndRecover() {
        when(deviceMapper.selectById(7001L)).thenReturn(MesProcessPoolTeamDeviceDO.builder()
                .id(7001L)
                .leaderUserId(3001L)
                .deviceCode("D-001")
                .deviceName("压力泵")
                .deviceStatus("ENABLED")
                .enabled(Boolean.TRUE)
                .build());

        service.updateDeviceStatus(MesTeamDeviceStatusUpdateReqBO.builder()
                .leaderUserId(3001L)
                .deviceId(7001L)
                .deviceStatus("REPAIRING")
                .build());

        ArgumentCaptor<MesProcessPoolTeamDeviceDO> repairCaptor =
                ArgumentCaptor.forClass(MesProcessPoolTeamDeviceDO.class);
        verify(deviceMapper).updateById(repairCaptor.capture());
        assertEquals(7001L, repairCaptor.getValue().getId());
        assertEquals("REPAIRING", repairCaptor.getValue().getDeviceStatus());
        assertTrue(repairCaptor.getValue().getEnabled());
        assertNotNull(repairCaptor.getValue().getStatusChangedAt());

        service.updateDeviceStatus(MesTeamDeviceStatusUpdateReqBO.builder()
                .leaderUserId(3001L)
                .deviceId(7001L)
                .deviceStatus("DISABLED")
                .build());

        ArgumentCaptor<MesProcessPoolTeamDeviceDO> disableCaptor =
                ArgumentCaptor.forClass(MesProcessPoolTeamDeviceDO.class);
        verify(deviceMapper, org.mockito.Mockito.times(2)).updateById(disableCaptor.capture());
        assertEquals("DISABLED", disableCaptor.getValue().getDeviceStatus());
        assertEquals(Boolean.FALSE, disableCaptor.getValue().getEnabled());

        service.updateDeviceStatus(MesTeamDeviceStatusUpdateReqBO.builder()
                .leaderUserId(3001L)
                .deviceId(7001L)
                .deviceStatus("ENABLED")
                .build());

        ArgumentCaptor<MesProcessPoolTeamDeviceDO> recoverCaptor =
                ArgumentCaptor.forClass(MesProcessPoolTeamDeviceDO.class);
        verify(deviceMapper, org.mockito.Mockito.times(3)).updateById(recoverCaptor.capture());
        assertEquals("ENABLED", recoverCaptor.getValue().getDeviceStatus());
        assertTrue(recoverCaptor.getValue().getEnabled());
    }


    @Test
    void shouldPersistParameterDefaultValueAndRejectDefaultOutsideRange() {
        when(deviceMapper.selectById(7001L)).thenReturn(MesProcessPoolTeamDeviceDO.builder()
                .id(7001L)
                .leaderUserId(3001L)
                .deviceStatus("ENABLED")
                .enabled(Boolean.TRUE)
                .build());
        when(parameterRuleMapper.insert(any(MesProcessPoolDeviceParameterRuleDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolDeviceParameterRuleDO.class).setId(8401L);
            return 1;
        });

        Long ruleId = service.saveDeviceParameterRule(MesTeamDeviceParameterRuleSaveReqBO.builder()
                .leaderUserId(3001L)
                .processId(6001L)
                .deviceId(7001L)
                .parameterCode("pressure")
                .parameterName("压力")
                .unit("MPa")
                .lowerLimit(new BigDecimal("10"))
                .upperLimit(new BigDecimal("20"))
                .defaultValue(new BigDecimal("15"))
                .valueType("DECIMAL")
                .build());

        assertEquals(8401L, ruleId);
        ArgumentCaptor<MesProcessPoolDeviceParameterRuleDO> ruleCaptor =
                ArgumentCaptor.forClass(MesProcessPoolDeviceParameterRuleDO.class);
        verify(parameterRuleMapper).insert(ruleCaptor.capture());
        assertEquals(new BigDecimal("15"), ruleCaptor.getValue().getDefaultValue());
        assertEquals("MPa", ruleCaptor.getValue().getUnit());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.saveDeviceParameterRule(
                MesTeamDeviceParameterRuleSaveReqBO.builder()
                        .leaderUserId(3001L)
                        .processId(6001L)
                        .deviceId(7001L)
                        .parameterCode("pressure")
                        .lowerLimit(new BigDecimal("10"))
                        .upperLimit(new BigDecimal("20"))
                        .defaultValue(new BigDecimal("25"))
                        .build()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_DEVICE_PARAMETER_LIMIT_INVALID.getCode(), ex.getCode());
    }

    @Test
    void shouldBindConfiguredDefectReasonToProcess() {
        when(defectReasonMapper.insert(any(MesProcessPoolDefectReasonDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolDefectReasonDO.class).setId(8301L);
            return 1;
        });

        Long reasonId = service.saveProcessDefectReason(MesTeamProcessDefectReasonSaveReqBO.builder()
                .leaderUserId(3001L)
                .processId(6001L)
                .reasonType("LOSS")
                .reasonCode("LOSS-001")
                .reasonName("正常损耗")
                .build());

        assertEquals(8301L, reasonId);
        verify(scopeService).assertCanMaintainProcess(3001L, 6001L);
        ArgumentCaptor<MesProcessPoolDefectReasonDO> reasonCaptor =
                ArgumentCaptor.forClass(MesProcessPoolDefectReasonDO.class);
        verify(defectReasonMapper).insert(reasonCaptor.capture());
        assertEquals(6001L, reasonCaptor.getValue().getProcessId());
        assertEquals("LOSS-001", reasonCaptor.getValue().getReasonCode());
        assertTrue(reasonCaptor.getValue().getEnabled());
    }
}
