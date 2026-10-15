package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.file.DccWorkflowLifecycleController;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class DccPendingAutomaticDistributionApiTest {
    @Test void signatureModeListDoesNotRequireAnAbsentPresetDateOrUnapprovedReminderConfiguration() {
        var lifecycle=mock(DccControlledFileLifecycleService.class);
        var categories=mock(DccControlledFileCategoryPermissionSupport.class);
        var permissions=mock(PermissionApi.class);
        when(permissions.hasAnyRoles(99L,"doc_control")).thenReturn(true);
        when(categories.hasCategoryPermission(any(),eq(99L),any())).thenReturn(true);
        var at=LocalDateTime.of(2026,10,11,12,0);
        var file=DccControlledFileDO.builder().id(91L).categoryId(7L).versionNo("A/1").status("ACTIVE")
                .effectiveMode(DccApplicationEffectiveSelection.AFTER_STANDARDIZATION_SIGNATURE)
                .effectiveTime(at).controlledTime(at.plusHours(1)).activatedTime(at.plusHours(1)).build();
        when(lifecycle.pendingDistribution(false)).thenReturn(List.of(file));
        var controller=new DccWorkflowLifecycleController(mock(DccControlledFileWorkflowService.class),
                new DccWorkflowDistributionService(),lifecycle,categories,permissions);
        try(var login=mockStatic(SecurityFrameworkUtils.class)) {
            login.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            var rows=controller.pending(false).getData();
            assertEquals(1,rows.size());assertEquals(91L,rows.get(0).getId());
            assertNull(rows.get(0).getEffectiveDate());
            assertNull(rows.get(0).getDistributionReminderStage());
            assertEquals(at,rows.get(0).getEffectiveTime());
            verify(lifecycle,never()).distributionReminderDates();
        }
    }
    @Test void unapprovedReminderSelectionFailsWithoutAListOrReminderRead() {
        var lifecycle=mock(DccControlledFileLifecycleService.class);
        var permissions=mock(PermissionApi.class);
        when(permissions.hasAnyRoles(99L,"doc_control")).thenReturn(true);
        var controller=new DccWorkflowLifecycleController(mock(DccControlledFileWorkflowService.class),
                new DccWorkflowDistributionService(),lifecycle,
                mock(DccControlledFileCategoryPermissionSupport.class),permissions);
        try(var login=mockStatic(SecurityFrameworkUtils.class)) {
            login.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            assertThrows(IllegalArgumentException.class,()->controller.pending(true));
            verifyNoInteractions(lifecycle);
        }
    }
}
