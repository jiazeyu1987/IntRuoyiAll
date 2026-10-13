package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccElectronicSignatureAuthorizationDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccElectronicSignatureAuthorizationMapper;
import cn.iocoder.yudao.module.system.api.permission.ApprovalAuthorityApi;
import cn.iocoder.yudao.module.system.api.permission.dto.ApprovalAuthorityRevisionDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Shared qualification boundary checks; transaction serialization is verified separately. */
class ApprovalSharedAuthorizationLockTest {
    private final DccElectronicSignatureAuthorizationMapper rows = mock(DccElectronicSignatureAuthorizationMapper.class);
    private final ApprovalAuthorityApi authority = mock(ApprovalAuthorityApi.class);
    private final DccElectronicSignatureAuthorizationAuditService audit = mock(DccElectronicSignatureAuthorizationAuditService.class);
    private final DccElectronicSignatureAuthorizationServiceImpl service = new DccElectronicSignatureAuthorizationServiceImpl();
    private DccElectronicSignatureAuthorizationDO row;

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(7L);
        ReflectionTestUtils.setField(service, "authorizationMapper", rows);
        ReflectionTestUtils.setField(service, "authorizationAuditService", audit);
        ReflectionTestUtils.setField(service, "authority", authority);
        row = DccElectronicSignatureAuthorizationDO.builder().id(31L).userId(12L)
                .electronicSignatureEnabled(true).authorizationState("ENABLED").build();
        when(rows.selectByUserIdForUpdate(12L)).thenReturn(row);
        when(authority.lockTenantApprovalAuthority(7L)).thenReturn(new ApprovalAuthorityRevisionDTO(7L, 3L));
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }

    @Test void revokedCurrentRowRefusesSignatureWithoutUsingSnapshotRead() {
        row.setElectronicSignatureEnabled(false); row.setAuthorizationState("DISABLED");
        assertThrows(RuntimeException.class, () -> service.validateElectronicSignatureEnabledLocked(12L));
        var order = inOrder(authority, rows);
        order.verify(authority).lockTenantApprovalAuthority(7L);
        order.verify(rows).selectByUserIdForUpdate(12L);
        verify(rows, never()).selectByUserId(anyLong());
    }

    @Test void revokeUpdatesRowAndAuditBeforeRevision() {
        when(rows.updateById(any(DccElectronicSignatureAuthorizationDO.class))).thenReturn(1);
        service.updateAuthorization(12L, false, 15L, "撤销签署资格");
        var order = inOrder(authority, rows, audit);
        order.verify(authority).lockTenantApprovalAuthority(7L);
        order.verify(rows).selectByUserIdForUpdate(12L);
        order.verify(rows).updateById(argThat((DccElectronicSignatureAuthorizationDO updated) -> !updated.getElectronicSignatureEnabled()));
        order.verify(audit).recordAuthorizationChange(any());
        order.verify(authority).incrementLockedAuthorityRevision(7L, 3L);
    }

    @Test void noAffectedRowCannotBeReportedAsSuccessfulRevoke() {
        when(rows.updateById(any(DccElectronicSignatureAuthorizationDO.class))).thenReturn(0);
        assertThrows(RuntimeException.class, () -> service.updateAuthorization(12L, false, 15L, "撤销"));
        verifyNoInteractions(audit);
        verify(authority, never()).incrementLockedAuthorityRevision(anyLong(), anyLong());
    }
}
