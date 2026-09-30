package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionRecipientMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRouteSnapshotMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class DccControlledFileDetailAuthorizationGuardTest extends BaseMockitoUnitTest {

    @Mock
    private DccControlledFileAssignmentScopeService assignmentScopeService;
    @Mock
    private DccDirectoryAccessPermissionService directoryAccessPermissionService;
    @Mock
    private DccControlledFileCategoryPermissionSupport permissionSupport;
    @Mock
    private DccControlledFileViewMatrixAccessService viewMatrixAccessService;
    @Mock
    private DccControlledFileDistributionRecipientMapper distributionRecipientMapper;
    @Mock
    private DccControlledFileRouteSnapshotMapper routeSnapshotMapper;
    @Mock
    private BpmTaskService bpmTaskService;

    @InjectMocks
    private DccControlledFileDetailAuthorizationGuard guard;

    @Test
    void activeFileWithoutAnyFileScopeIsDenied() {
        TenantContextHolder.setTenantId(31L);
        DccControlledFileDO file = activeFile();
        when(assignmentScopeService.isWithinAssignedFileScope(99L, 910L)).thenReturn(true);
        when(viewMatrixAccessService.canAccessCurrentViewMatrix(99L, file)).thenReturn(false);
        when(directoryAccessPermissionService.getAuthorizedDirectoryIds(eq(99L), any())).thenReturn(Set.of());
        when(distributionRecipientMapper.countActiveElectronicRecipientAccess(31L, 910L, 99L)).thenReturn(0L);

        assertFalse(guard.isAllowed(99L, file));
    }

    @Test
    void activeFileWithViewMatrixScopeIsAllowed() {
        TenantContextHolder.setTenantId(31L);
        DccControlledFileDO file = activeFile();
        when(assignmentScopeService.isWithinAssignedFileScope(99L, 910L)).thenReturn(true);
        when(viewMatrixAccessService.canAccessCurrentViewMatrix(99L, file)).thenReturn(true);

        assertTrue(guard.isAllowed(99L, file));
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    private DccControlledFileDO activeFile() {
        return DccControlledFileDO.builder()
                .id(910L).categoryId(10L).directoryId(20L).requesterId(88L)
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus()).publishedFileId(911L).build();
    }
}
