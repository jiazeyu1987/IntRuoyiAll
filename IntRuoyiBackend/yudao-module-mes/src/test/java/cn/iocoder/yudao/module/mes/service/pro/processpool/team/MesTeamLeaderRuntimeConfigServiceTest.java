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
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private PasswordEncoder passwordEncoder;

    private MesTeamLeaderRuntimeConfigService service;

    @BeforeEach
    void setUp() {
        service = new MesTeamLeaderRuntimeConfigServiceImpl(scopeService, employeeProfileMapper, employeeBindingMapper,
                deviceMapper, processDeviceMapper, parameterRuleMapper, defectReasonMapper, auditMapper,
                adminUserApi, passwordEncoder);
    }

    @Test
    void shouldCreateTemporaryProductionPersonWithSignaturePasswordHashAndAudit() {
        when(employeeProfileMapper.selectList(any())).thenReturn(List.of());
        when(passwordEncoder.encode("sign-123")).thenReturn("bcrypt-temp-sign");
        when(employeeProfileMapper.insert(any(MesProcessPoolTeamEmployeeProfileDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolTeamEmployeeProfileDO.class).setId(8801L);
            return 1;
        });

        Long profileId = service.createTemporaryEmployee(MesTeamTemporaryEmployeeCreateReqBO.builder()
                .leaderUserId(3001L)
                .displayName("临时工甲")
                .signaturePassword("sign-123")
                .build());

        assertEquals(8801L, profileId);
        ArgumentCaptor<MesProcessPoolTeamEmployeeProfileDO> profileCaptor =
                ArgumentCaptor.forClass(MesProcessPoolTeamEmployeeProfileDO.class);
        verify(employeeProfileMapper).insert(profileCaptor.capture());
        assertEquals("临时工甲", profileCaptor.getValue().getDisplayName());
        assertEquals("TEMPORARY", profileCaptor.getValue().getEmployeeType());
        assertNull(profileCaptor.getValue().getSystemUserId());
        assertEquals("bcrypt-temp-sign", profileCaptor.getValue().getSignaturePasswordHash());
        assertNotNull(profileCaptor.getValue().getSignaturePasswordUpdatedAt());
        verify(adminUserApi, never()).validateUser(any());
        verify(auditMapper).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));
    }

    @Test
    void shouldLinkFormalUserWithoutStoringSignaturePassword() {
        AdminUserRespDTO formalUser = new AdminUserRespDTO();
        formalUser.setId(2001L);
        formalUser.setNickname("张三");
        when(adminUserApi.getUserListBySubordinate(3001L)).thenReturn(List.of(formalUser));
        when(employeeProfileMapper.selectList(any())).thenReturn(List.of());
        when(employeeProfileMapper.insert(any(MesProcessPoolTeamEmployeeProfileDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolTeamEmployeeProfileDO.class).setId(8802L);
            return 1;
        });

        Long profileId = service.linkFormalEmployee(MesTeamFormalEmployeeLinkReqBO.builder()
                .leaderUserId(3001L)
                .systemUserId(2001L)
                .displayName("张三")
                .build());

        assertEquals(8802L, profileId);
        verify(adminUserApi).validateUser(2001L);
        ArgumentCaptor<MesProcessPoolTeamEmployeeProfileDO> profileCaptor =
                ArgumentCaptor.forClass(MesProcessPoolTeamEmployeeProfileDO.class);
        verify(employeeProfileMapper).insert(profileCaptor.capture());
        assertEquals(2001L, profileCaptor.getValue().getSystemUserId());
        assertEquals("FORMAL", profileCaptor.getValue().getEmployeeType());
        assertNull(profileCaptor.getValue().getSignaturePasswordHash());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void shouldRejectDuplicateFormalUserBeforeDatabaseInsert() {
        AdminUserRespDTO formalUser = new AdminUserRespDTO();
        formalUser.setId(2001L);
        formalUser.setNickname("张三");
        when(adminUserApi.getUserListBySubordinate(3001L)).thenReturn(List.of(formalUser));
        when(employeeProfileMapper.selectList(any())).thenReturn(List.of(MesProcessPoolTeamEmployeeProfileDO.builder()
                .id(8802L)
                .leaderUserId(3001L)
                .systemUserId(2001L)
                .employeeCode("USER-2001")
                .displayName("张三")
                .employeeName("张三")
                .employeeType("FORMAL")
                .enabled(Boolean.TRUE)
                .build()));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.linkFormalEmployee(
                MesTeamFormalEmployeeLinkReqBO.builder()
                        .leaderUserId(3001L)
                        .systemUserId(2001L)
                        .displayName("张三-A")
                        .build()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_FORMAL_EMPLOYEE_DUPLICATE.getCode(),
                ex.getCode());
        verify(employeeProfileMapper, never()).insert(any(MesProcessPoolTeamEmployeeProfileDO.class));
    }

    @Test
    void shouldRejectDuplicateActiveDisplayNameForSameLeaderWithSuffixGuidance() {
        when(employeeProfileMapper.selectList(any())).thenReturn(List.of(MesProcessPoolTeamEmployeeProfileDO.builder()
                .id(8801L)
                .leaderUserId(3001L)
                .displayName("张三")
                .employeeName("张三")
                .employeeType("TEMPORARY")
                .enabled(Boolean.TRUE)
                .build()));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.createTemporaryEmployee(
                MesTeamTemporaryEmployeeCreateReqBO.builder()
                        .leaderUserId(3001L)
                        .displayName("张三")
                        .signaturePassword("sign-123")
                        .build()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_EMPLOYEE_DISPLAY_NAME_DUPLICATE.getCode(),
                ex.getCode());
        verify(employeeProfileMapper, never()).insert(any(MesProcessPoolTeamEmployeeProfileDO.class));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void shouldSearchFormalCandidatesOnlyFromAllowedSubordinateUsers() {
        AdminUserRespDTO zhang = new AdminUserRespDTO();
        zhang.setId(2001L);
        zhang.setNickname("张三");
        AdminUserRespDTO li = new AdminUserRespDTO();
        li.setId(2002L);
        li.setNickname("李四");
        when(adminUserApi.getUserListBySubordinate(3001L)).thenReturn(List.of(zhang, li));

        List<MesTeamFormalUserCandidateBO> candidates = service.searchFormalUserCandidates(3001L, "张");

        assertEquals(1, candidates.size());
        assertEquals(2001L, candidates.get(0).getSystemUserId());
        assertEquals("张三", candidates.get(0).getDisplayName());
    }

    @Test
    void shouldAllowResetOnlyTemporaryEmployeeSignaturePassword() {
        when(employeeProfileMapper.selectById(8801L)).thenReturn(MesProcessPoolTeamEmployeeProfileDO.builder()
                .id(8801L)
                .leaderUserId(3001L)
                .displayName("临时工甲")
                .employeeType("TEMPORARY")
                .enabled(Boolean.TRUE)
                .build());
        when(passwordEncoder.encode("new-sign")).thenReturn("bcrypt-new-sign");

        service.resetTemporaryEmployeeSignaturePassword(MesTeamTempSignaturePasswordResetReqBO.builder()
                .leaderUserId(3001L)
                .employeeProfileId(8801L)
                .signaturePassword("new-sign")
                .build());

        ArgumentCaptor<MesProcessPoolTeamEmployeeProfileDO> updateCaptor =
                ArgumentCaptor.forClass(MesProcessPoolTeamEmployeeProfileDO.class);
        verify(employeeProfileMapper).updateById(updateCaptor.capture());
        assertEquals(8801L, updateCaptor.getValue().getId());
        assertEquals("bcrypt-new-sign", updateCaptor.getValue().getSignaturePasswordHash());

        when(employeeProfileMapper.selectById(8802L)).thenReturn(MesProcessPoolTeamEmployeeProfileDO.builder()
                .id(8802L)
                .leaderUserId(3001L)
                .displayName("正式工甲")
                .employeeType("FORMAL")
                .systemUserId(2001L)
                .enabled(Boolean.TRUE)
                .build());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.resetTemporaryEmployeeSignaturePassword(
                MesTeamTempSignaturePasswordResetReqBO.builder()
                        .leaderUserId(3001L)
                        .employeeProfileId(8802L)
                        .signaturePassword("new-sign")
                        .build()));
        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_FORMAL_SIGNATURE_PASSWORD_MANAGED_BY_USER.getCode(),
                ex.getCode());
    }

    @Test
    void shouldManageProductionPersonnelListStatusRenameAndAuditForCurrentLeaderOnly() {
        MesProcessPoolTeamEmployeeProfileDO temporary = MesProcessPoolTeamEmployeeProfileDO.builder()
                .id(8801L)
                .leaderUserId(3001L)
                .displayName("临时工甲")
                .employeeName("临时工甲")
                .employeeType("TEMPORARY")
                .enabled(Boolean.TRUE)
                .build();
        when(employeeProfileMapper.selectList(any())).thenReturn(List.of(temporary));

        List<MesProcessPoolTeamEmployeeProfileDO> profiles = service.listEmployeeProfiles(3001L, null);

        assertEquals(1, profiles.size());
        assertEquals(3001L, profiles.get(0).getLeaderUserId());
        assertEquals("临时工甲", profiles.get(0).getDisplayName());

        when(employeeProfileMapper.selectById(8801L)).thenReturn(temporary);
        when(employeeProfileMapper.selectList(any())).thenReturn(List.of());

        service.renameEmployee(MesTeamEmployeeDisplayNameUpdateReqBO.builder()
                .leaderUserId(3001L)
                .employeeProfileId(8801L)
                .displayName("临时工甲-A")
                .build());

        ArgumentCaptor<MesProcessPoolTeamEmployeeProfileDO> renameCaptor =
                ArgumentCaptor.forClass(MesProcessPoolTeamEmployeeProfileDO.class);
        verify(employeeProfileMapper).updateById(renameCaptor.capture());
        assertEquals(8801L, renameCaptor.getValue().getId());
        assertEquals("临时工甲-A", renameCaptor.getValue().getDisplayName());
        verify(auditMapper, org.mockito.Mockito.atLeastOnce()).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));

        service.updateEmployeeEnabled(MesTeamEmployeeStatusUpdateReqBO.builder()
                .leaderUserId(3001L)
                .employeeProfileId(8801L)
                .enabled(Boolean.FALSE)
                .build());

        ArgumentCaptor<MesProcessPoolTeamEmployeeProfileDO> disableCaptor =
                ArgumentCaptor.forClass(MesProcessPoolTeamEmployeeProfileDO.class);
        verify(employeeProfileMapper, org.mockito.Mockito.times(2)).updateById(disableCaptor.capture());
        assertEquals(Boolean.FALSE, disableCaptor.getValue().getEnabled());
        assertNotNull(disableCaptor.getValue().getDisabledAt());

        MesProcessPoolTeamMaintenanceAuditDO audit = MesProcessPoolTeamMaintenanceAuditDO.builder()
                .id(9901L)
                .leaderUserId(3001L)
                .targetId(8801L)
                .actionType("DISABLE_EMPLOYEE")
                .resultStatus("SUCCESS")
                .changeSummary("禁用生产人员：临时工甲-A")
                .auditTime(LocalDateTime.now())
                .build();
        when(auditMapper.selectList(any())).thenReturn(List.of(audit));
        List<MesProcessPoolTeamMaintenanceAuditDO> audits = service.listEmployeeAuditRecords(3001L, 8801L);
        assertEquals(1, audits.size());
        assertEquals("DISABLE_EMPLOYEE", audits.get(0).getActionType());
        assertEquals("SUCCESS", audits.get(0).getResultStatus());
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
