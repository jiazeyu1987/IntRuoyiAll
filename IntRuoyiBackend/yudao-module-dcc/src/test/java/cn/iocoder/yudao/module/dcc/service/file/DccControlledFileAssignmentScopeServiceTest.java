package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionRecipientMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeAssignmentFileMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DccControlledFileAssignmentScopeServiceTest extends BaseMockitoUnitTest {

    @Mock private PermissionApi permissionApi;
    @Mock private DccProjectCodeAssignmentFileMapper assignmentFileMapper;
    @Mock private DccControlledFileDistributionRecipientMapper distributionRecipientMapper;

    @InjectMocks
    private DccControlledFileAssignmentScopeService service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(31L);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void resolveActiveAssignedControlledFileIds_fullScopeIsUnrestricted() {
        when(permissionApi.hasAnyPermissions(99L, "dcc:controlled-file:scope:all")).thenReturn(true);

        assertNull(service.resolveActiveAssignedControlledFileIds(99L));

        verifyNoInteractions(assignmentFileMapper, distributionRecipientMapper);
    }

    @Test
    void resolveActiveAssignedControlledFileIds_withoutAssignmentPermissionIsUnrestricted() {
        when(permissionApi.hasAnyPermissions(99L, "dcc:controlled-file:scope:all")).thenReturn(false);
        when(permissionApi.hasAnyPermissions(99L, "dcc:project-code-assignment:execute")).thenReturn(false);

        assertNull(service.resolveActiveAssignedControlledFileIds(99L));

        verifyNoInteractions(assignmentFileMapper, distributionRecipientMapper);
    }

    @Test
    void resolveActiveAssignedControlledFileIds_combinesAssignmentsAndElectronicDistribution() {
        when(permissionApi.hasAnyPermissions(99L, "dcc:controlled-file:scope:all")).thenReturn(false);
        when(permissionApi.hasAnyPermissions(99L, "dcc:project-code-assignment:execute")).thenReturn(true);
        when(assignmentFileMapper.selectActiveControlledFileIdsByAssigneeUserId(eq(99L), any(LocalDateTime.class)))
                .thenReturn(List.of(100L, 101L));
        when(distributionRecipientMapper.selectActiveElectronicControlledFileIdsByUserId(31L, 99L))
                .thenReturn(List.of(101L, 102L));

        assertEquals(Set.of(100L, 101L, 102L), service.resolveActiveAssignedControlledFileIds(99L));
        assertTrue(service.isWithinAssignedFileScope(99L, 102L));
        assertFalse(service.isWithinAssignedFileScope(99L, 103L));
    }

    @Test
    void filterBusinessVisibleUserIds_removesCandidatesExcludedByAssignmentHardScope() {
        when(permissionApi.hasAnyPermissions(1L, "dcc:controlled-file:scope:all")).thenReturn(false);
        when(permissionApi.hasAnyPermissions(1L, "dcc:project-code-assignment:execute")).thenReturn(false);
        when(permissionApi.hasAnyPermissions(2L, "dcc:controlled-file:scope:all")).thenReturn(false);
        when(permissionApi.hasAnyPermissions(2L, "dcc:project-code-assignment:execute")).thenReturn(true);
        when(assignmentFileMapper.selectActiveControlledFileIdsByAssigneeUserId(eq(2L), any(LocalDateTime.class)))
                .thenReturn(List.of(999L));
        when(distributionRecipientMapper.selectActiveElectronicControlledFileIdsByUserId(31L, 2L))
                .thenReturn(List.of());

        assertEquals(Set.of(1L), service.filterBusinessVisibleUserIds(Set.of(1L, 2L), 100L));
        verify(distributionRecipientMapper, never())
                .selectActiveElectronicControlledFileIdsByUserId(31L, 1L);
    }
}
