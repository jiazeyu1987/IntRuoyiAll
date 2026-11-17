package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesDeviationTaskPermissionServiceTest {
    private final PermissionApi permissions = mock(PermissionApi.class);
    private final AdminUserApi users = mock(AdminUserApi.class);
    private final MesDeviationTaskPermissionService service = new MesDeviationTaskPermissionService();

    @BeforeEach void setUp() {
        ReflectionTestUtils.setField(service, "permissionApi", permissions);
        ReflectionTestUtils.setField(service, "adminUserApi", users);
        when(users.getUser(201L)).thenReturn(new AdminUserRespDTO().setId(201L).setTenantId(71L).setStatus(0));
        when(permissions.hasAnyPermissions(201L, "mes:pro-edhr-work-task:query")).thenReturn(true);
    }

    @Test void everyReviewResponsibilityRechecksItsOwnPermission() {
        String[][] responsibilities = {{"DEVIATION_SIGN", "PREPARER", "handle"},
                {"DEVIATION_SIGN", "VERIFIER", "verify"}, {"DEVIATION_SIGN", "DEPARTMENT_OWNER", "department-confirm"},
                {"DEVIATION_APPROVE", "QA", "qa-close"}, {"DEVIATION_APPROVE", "QUALITY_OWNER", "quality-approve"},
                {"DEVIATION_APPROVE", "MANAGEMENT_REP", "critical-management-approve"},
                {"DEVIATION_VERIFY", "CORRECTIVE_VERIFIER", "verify"}, {"DEVIATION_CLOSE", "CLOSE", "qa-close"}};
        for (String[] responsibility : responsibilities) {
            String permission = "mes:pro-edhr-deviation:" + responsibility[2];
            when(permissions.hasAnyPermissions(201L, permission)).thenReturn(false);
            assertTrue(service.blockedReason(71L, 201L, responsibility[0], responsibility[1]).contains("办理权限"));
            when(permissions.hasAnyPermissions(201L, permission)).thenReturn(true);
            assertNull(service.blockedReason(71L, 201L, responsibility[0], responsibility[1]));
        }
    }

    @Test void missingOrConflictingReviewNodeCannotGrantAccess() {
        for (String type : new String[]{"DEVIATION_SIGN", "DEVIATION_APPROVE", "DEVIATION_VERIFY", "DEVIATION_CLOSE"}) {
            assertTrue(service.blockedReason(71L, 201L, type, null).contains("职责信息不完整"));
            assertTrue(service.blockedReason(71L, 201L, type, "FORGED").contains("职责信息不完整"));
        }
        assertTrue(service.blockedReason(71L, 201L, "DEVIATION_APPROVE", "PREPARER").contains("职责信息不完整"));
    }

    @Test void rectificationUsesPersonalResponsibilityWithoutInvestigationRole() {
        assertNull(service.blockedReason(71L, 201L, "DEVIATION_CORRECT", null));
        verify(permissions, never()).hasAnyPermissions(201L, "mes:pro-edhr-deviation:handle");
        when(permissions.hasAnyPermissions(201L, "mes:pro-edhr-work-task:query")).thenReturn(false);
        assertTrue(service.blockedReason(71L, 201L, "DEVIATION_CORRECT", null).contains("工作任务查询权限"));
    }

    @Test void investigationRequiresQueryAndHandlingPermissions() {
        when(permissions.hasAnyPermissions(201L, "mes:pro-edhr-deviation:handle")).thenReturn(true);
        assertTrue(service.blockedReason(71L, 201L, "DEVIATION_RECEIVE", null).contains("偏差查询权限"));
        when(permissions.hasAnyPermissions(201L, "mes:pro-edhr-deviation:query")).thenReturn(true);
        assertNull(service.blockedReason(71L, 201L, "DEVIATION_RECEIVE", null));
    }

    @Test void disabledForeignTenantOrMissingUserIsBlocked() {
        assertNotNull(service.blockedReason(72L, 201L, "DEVIATION_CORRECT", null));
        when(users.getUser(201L)).thenReturn(new AdminUserRespDTO().setId(201L).setTenantId(71L).setStatus(1));
        assertNotNull(service.blockedReason(71L, 201L, "DEVIATION_CORRECT", null));
        when(users.getUser(201L)).thenReturn(null);
        assertNotNull(service.blockedReason(71L, 201L, "DEVIATION_CORRECT", null));
    }
}
