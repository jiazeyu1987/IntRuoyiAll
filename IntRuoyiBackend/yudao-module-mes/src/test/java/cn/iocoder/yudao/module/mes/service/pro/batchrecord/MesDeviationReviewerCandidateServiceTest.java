package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesDeviationReviewerCandidateMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesDeviationReviewerCandidateServiceTest {
    private final MesDeviationReviewerCandidateMapper mapper = mock(MesDeviationReviewerCandidateMapper.class);
    private final PermissionApi permissions = mock(PermissionApi.class);
    private final AdminUserApi users = mock(AdminUserApi.class);
    private final MesProEdhrDeviationReviewTaskService taskService = new MesProEdhrDeviationReviewTaskService();
    private final MesDeviationReviewerCandidateService service = new MesDeviationReviewerCandidateService();

    @BeforeEach void setUp() {
        TenantContextHolder.setTenantId(71L);
        ReflectionTestUtils.setField(taskService, "permissionApi", permissions);
        ReflectionTestUtils.setField(taskService, "adminUserApi", users);
        ReflectionTestUtils.setField(service, "candidateMapper", mapper);
        ReflectionTestUtils.setField(service, "reviewTaskService", taskService);
    }

    @AfterEach void tearDown() { TenantContextHolder.clear(); }

    @Test void candidatesAndAssignmentUseSameNodeEligibilityWithoutGlobalQuery() {
        var eligible = user(201L, 71L, 0);
        when(mapper.selectEnabledByTenant(71L)).thenReturn(List.of(eligible,
                user(202L, 72L, 0), user(203L, 71L, 1), user(204L, 71L, 0), user(205L, 71L, 0)));
        when(permissions.hasAnyPermissions(201L, "mes:pro-edhr-work-task:query")).thenReturn(true);
        when(permissions.hasAnyPermissions(201L, "mes:pro-edhr-deviation:verify")).thenReturn(true);
        when(permissions.hasAnyPermissions(204L, "mes:pro-edhr-work-task:query")).thenReturn(true);
        var result = service.list("VERIFIER");
        assertEquals(List.of(201L), result.stream().map(row -> row.getId()).toList());
        assertEquals("user201", result.get(0).getUsername());
        when(users.getUser(201L)).thenReturn(eligible);
        assertDoesNotThrow(() -> taskService.validateAssignee("VERIFIER", 201L));
        verify(permissions, never()).hasAnyPermissions(201L, "mes:pro-edhr-deviation:query");
    }

    @Test void candidatePermissionsAreSpecificToTheRequestedNode() {
        when(mapper.selectEnabledByTenant(71L)).thenReturn(List.of(user(201L, 71L, 0)));
        when(permissions.hasAnyPermissions(201L, "mes:pro-edhr-work-task:query")).thenReturn(true);
        when(permissions.hasAnyPermissions(201L, "mes:pro-edhr-deviation:verify")).thenReturn(true);
        assertEquals(1, service.list("VERIFIER").size());
        assertTrue(service.list("QA").isEmpty());
    }

    @Test void unknownNodeFailsBeforeReadingUsers() {
        assertThrows(IllegalArgumentException.class, () -> service.list("UNKNOWN"));
        verifyNoInteractions(mapper, users, permissions);
    }

    @Test void permissionFailureIsNotReportedAsAnEmptyCandidatePool() {
        when(mapper.selectEnabledByTenant(71L)).thenReturn(List.of(user(201L, 71L, 0)));
        when(permissions.hasAnyPermissions(201L, "mes:pro-edhr-work-task:query"))
                .thenThrow(new IllegalStateException("permission service unavailable"));
        assertThrows(IllegalStateException.class, () -> service.list("VERIFIER"));
    }

    private AdminUserRespDTO user(Long id, Long tenant, int status) {
        var result = new AdminUserRespDTO();
        result.setId(id); result.setTenantId(tenant); result.setStatus(status);
        result.setUsername("user" + id); result.setNickname("人员" + id);
        return result;
    }
}
