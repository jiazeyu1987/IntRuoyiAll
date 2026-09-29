package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeProfileDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MesTeamEmployeeOwnershipTransactionTest {

    @ParameterizedTest
    @CsvSource({"LINK,SCOPE", "LINK,AUDIT", "CREATE,SCOPE", "CREATE,AUDIT", "ENABLE,SCOPE", "ENABLE,AUDIT"})
    void downstreamFailureRollsBackProfileAndScope(String action, String failureAt) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:owner_tx_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE employee_profile (id BIGINT PRIMARY KEY, enabled BOOLEAN)");
        jdbc.execute("CREATE TABLE employee_scope (id BIGINT PRIMARY KEY)");
        MesProcessPoolTeamEmployeeProfileMapper profiles = mock(MesProcessPoolTeamEmployeeProfileMapper.class);
        MesProcessPoolTeamLeaderScopeMapper scopes = mock(MesProcessPoolTeamLeaderScopeMapper.class);
        MesProcessPoolTeamMaintenanceAuditMapper audits = mock(MesProcessPoolTeamMaintenanceAuditMapper.class);
        AdminUserApi users = mock(AdminUserApi.class);
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(7L);
        user.setNickname("员工甲");
        when(users.getUser(7L)).thenReturn(user);
        when(profiles.insert(any(MesProcessPoolTeamEmployeeProfileDO.class))).thenAnswer(invocation -> {
            MesProcessPoolTeamEmployeeProfileDO profile = invocation.getArgument(0);
            profile.setId(1L);
            return jdbc.update("INSERT INTO employee_profile(id,enabled) VALUES(1,true)");
        });
        when(profiles.updateById(any(MesProcessPoolTeamEmployeeProfileDO.class))).thenAnswer(invocation ->
                jdbc.update("UPDATE employee_profile SET enabled=true WHERE id=1"));
        if ("ENABLE".equals(action)) {
            jdbc.update("INSERT INTO employee_profile(id,enabled) VALUES(1,false)");
            when(profiles.selectById(1L)).thenReturn(MesProcessPoolTeamEmployeeProfileDO.builder()
                    .id(1L).leaderUserId(101L).systemUserId(7L).employeeType("FORMAL").enabled(false).build());
        }
        when(scopes.insert(any(MesProcessPoolTeamLeaderScopeDO.class))).thenAnswer(invocation -> {
            if ("SCOPE".equals(failureAt)) {
                throw new IllegalStateException("scope-write-failed");
            }
            return jdbc.update("INSERT INTO employee_scope(id) VALUES(1)");
        });
        when(audits.insert(any(MesProcessPoolTeamMaintenanceAuditDO.class)))
                .thenThrow(new IllegalStateException("audit-write-failed"));
        MesTeamLeaderRuntimeConfigServiceImpl target = new MesTeamLeaderRuntimeConfigServiceImpl(
                mock(MesTeamLeaderScopeService.class), mock(MesRouteStartProductionLeaderAuthorizationService.class),
                profiles, scopes, mock(MesProcessPoolTeamDeviceMapper.class), mock(MesProcessPoolTeamProcessDeviceMapper.class),
                mock(MesProcessPoolDeviceParameterRuleMapper.class), mock(MesProRouteProcessMapper.class),
                mock(MesProcessPoolDefectReasonMapper.class), audits, users, mock(PasswordEncoder.class));
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(dataSource),
                new AnnotationTransactionAttributeSource()));
        MesTeamLeaderRuntimeConfigService service = (MesTeamLeaderRuntimeConfigService) proxy.getProxy();

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> {
            switch (action) {
                case "LINK" -> service.linkFormalEmployee(MesTeamFormalEmployeeLinkReqBO.builder()
                        .leaderUserId(101L).systemUserId(7L).displayName("员工甲").build());
                case "CREATE" -> service.createEmployee(MesTeamEmployeeProfileSaveReqBO.builder()
                        .leaderUserId(101L).systemUserId(7L).employeeCode("USER-7")
                        .employeeName("员工甲").employeeType("SYSTEM").build());
                case "ENABLE" -> service.updateEmployeeEnabled(MesTeamEmployeeStatusUpdateReqBO.builder()
                        .leaderUserId(101L).employeeProfileId(1L).enabled(true).build());
                default -> throw new AssertionError(action);
            }
        });
        assertEquals(failureAt.toLowerCase() + "-write-failed", error.getMessage());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM employee_profile WHERE enabled=true", Integer.class));
        assertEquals("ENABLE".equals(action) ? 1 : 0,
                jdbc.queryForObject("SELECT COUNT(*) FROM employee_profile", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM employee_scope", Integer.class));
    }
}
