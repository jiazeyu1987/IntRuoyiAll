package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeProfileDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamEmployeeProfileMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationMachineService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationWorkerService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class MesFrontlineUniqueLeaderAssignmentTest {

    @Mock
    private ObjectProvider<MesFrontlineDeviceAccountRouteBindingSource> routeBindingSourceProvider;
    @Mock
    private MesProRouteProcessMapper routeProcessMapper;
    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesProProcessService processService;
    @Mock
    private MesMdWorkstationWorkerService workstationWorkerService;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private MesProRouteService routeService;
    @Mock
    private MesMdWorkstationService workstationService;
    @Mock
    private MesMdWorkstationMachineService workstationMachineService;
    @Mock
    private MesDvMachineryService machineryService;
    @Mock
    private MesProcessPoolTeamEmployeeProfileMapper employeeProfileMapper;
    @Mock
    private MesFrontlineActiveOrderProcessService activeOrderProcessService;

    private MesFrontlineDeviceAccountContextServiceImpl contextService;

    @BeforeEach
    void setUp() {
        contextService = new MesFrontlineDeviceAccountContextServiceImpl(routeBindingSourceProvider, routeProcessMapper,
                routeVersionMapper, processService, workstationWorkerService, adminUserApi, permissionApi,
                routeService, workstationService, workstationMachineService, machineryService, employeeProfileMapper,
                activeOrderProcessService);
    }

    @Test
    void rejectsActualEmployeeWithoutEnabledLeaderAssignment() {
        when(employeeProfileMapper.selectList(any())).thenReturn(List.of());

        assertThrows(ServiceException.class,
                () -> contextService.requireUniqueResponsibleLeaderUserId(7001L));
    }

    @Test
    void rejectsActualEmployeeAssignedToMultipleEnabledLeaders() {
        when(employeeProfileMapper.selectList(any())).thenReturn(List.of(
                employeeProfile(8101L, 9001L, 7001L),
                employeeProfile(8102L, 9002L, 7001L)));

        assertThrows(ServiceException.class,
                () -> contextService.requireUniqueResponsibleLeaderUserId(7001L));
    }

    @Test
    void resolvesActualEmployeeUniqueEnabledLeader() {
        when(employeeProfileMapper.selectList(any())).thenReturn(List.of(
                employeeProfile(8101L, 9001L, 7001L)));

        assertEquals(9001L, contextService.requireUniqueResponsibleLeaderUserId(7001L));
    }

    @Test
    void realEmployeeMaintenanceTransferLeavesFrontlineWithExactlyOneEnabledLeader() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new Configuration(), "frontline-transfer"),
                MesProcessPoolTeamEmployeeProfileDO.class);
        var oldProfile = employeeProfile(8101L, 9001L, 7001L);
        var newProfile = employeeProfile(8102L, 9002L, 7001L).setEnabled(false);
        List<MesProcessPoolTeamEmployeeProfileDO> profiles = List.of(oldProfile, newProfile);
        when(employeeProfileMapper.selectList(any())).thenAnswer(invocation -> {
            LambdaQueryWrapper<MesProcessPoolTeamEmployeeProfileDO> query = invocation.getArgument(0);
            String sql = query.getSqlSegment();
            return profiles.stream().filter(profile -> !sql.contains("enabled") || Boolean.TRUE.equals(profile.getEnabled()))
                    .toList();
        });
        when(employeeProfileMapper.selectById(8101L)).thenReturn(oldProfile);
        when(employeeProfileMapper.selectById(8102L)).thenReturn(newProfile);
        when(employeeProfileMapper.updateById(any(MesProcessPoolTeamEmployeeProfileDO.class))).thenAnswer(invocation -> {
            var update = invocation.getArgument(0, MesProcessPoolTeamEmployeeProfileDO.class);
            profiles.stream().filter(profile -> profile.getId().equals(update.getId())).findFirst().orElseThrow()
                    .setEnabled(update.getEnabled());
            return 1;
        });
        var scopes = mock(MesProcessPoolTeamLeaderScopeMapper.class);
        when(scopes.insert(any(MesProcessPoolTeamLeaderScopeDO.class))).thenReturn(1);
        MesTeamLeaderRuntimeConfigService service = new MesTeamLeaderRuntimeConfigServiceImpl(
                mock(MesTeamLeaderScopeService.class), mock(MesRouteStartProductionLeaderAuthorizationService.class),
                employeeProfileMapper, scopes, mock(MesProcessPoolTeamDeviceMapper.class),
                mock(MesProcessPoolTeamProcessDeviceMapper.class), mock(MesProcessPoolDeviceParameterRuleMapper.class),
                routeProcessMapper, mock(MesProcessPoolDefectReasonMapper.class), mock(MesProcessPoolTeamMaintenanceAuditMapper.class),
                adminUserApi, mock(PasswordEncoder.class));

        assertEquals(9001L, contextService.requireUniqueResponsibleLeaderUserId(7001L));
        service.updateEmployeeEnabled(MesTeamEmployeeStatusUpdateReqBO.builder().leaderUserId(9001L)
                .employeeProfileId(8101L).enabled(false).build());
        assertThrows(ServiceException.class, () -> contextService.requireUniqueResponsibleLeaderUserId(7001L));
        service.updateEmployeeEnabled(MesTeamEmployeeStatusUpdateReqBO.builder().leaderUserId(9002L)
                .employeeProfileId(8102L).enabled(true).build());
        assertEquals(9002L, contextService.requireUniqueResponsibleLeaderUserId(7001L));
        assertEquals(false, oldProfile.getEnabled());
        assertEquals(2, profiles.size());
    }

    private static MesProcessPoolTeamEmployeeProfileDO employeeProfile(
            Long id, Long leaderUserId, Long systemUserId) {
        return MesProcessPoolTeamEmployeeProfileDO.builder()
                .id(id)
                .leaderUserId(leaderUserId)
                .systemUserId(systemUserId)
                .employeeCode("USER-" + systemUserId)
                .employeeName("员工" + systemUserId)
                .enabled(Boolean.TRUE)
                .build();
    }
}
