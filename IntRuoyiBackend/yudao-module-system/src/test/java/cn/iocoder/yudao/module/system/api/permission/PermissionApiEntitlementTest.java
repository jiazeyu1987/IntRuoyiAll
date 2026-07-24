package cn.iocoder.yudao.module.system.api.permission;

import cn.iocoder.yudao.module.system.api.permission.dto.SystemEntitlementRevokeReqDTO;
import cn.iocoder.yudao.module.system.api.permission.dto.SystemEntitlementSyncReqDTO;
import cn.iocoder.yudao.module.system.service.permission.SystemEntitlementService;
import cn.iocoder.yudao.module.system.service.permission.bo.SystemEntitlementSyncCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PermissionApiEntitlementTest {

    @InjectMocks
    private PermissionApiImpl permissionApi;
    @Mock
    private SystemEntitlementService entitlementService;

    @Test
    void syncEntitlementClaims_delegatesToSystemEntitlementService() {
        SystemEntitlementSyncReqDTO reqDTO = SystemEntitlementSyncReqDTO.builder()
                .tenantId(122L)
                .sourceType("EDHR_PROCESS_FORM_FILLER")
                .sourceKey("ROUTE|9001|8001|11")
                .sourceVersion("11")
                .sourceDigest("digest-a")
                .policyCode("MES_EDHR_FILLER_MINIMAL")
                .resolvedUserIds(Set.of(101L, 202L))
                .operatorUserId(1L)
                .operatorUsername("admin")
                .build();

        permissionApi.syncEntitlementClaims(reqDTO);

        ArgumentCaptor<SystemEntitlementSyncCommand> captor =
                ArgumentCaptor.forClass(SystemEntitlementSyncCommand.class);
        verify(entitlementService).syncClaims(captor.capture());
        assertEquals("ROUTE|9001|8001|11", captor.getValue().getSourceKey());
        assertEquals(Set.of(101L, 202L), captor.getValue().getResolvedUserIds());
    }

    @Test
    void revokeEntitlementSource_delegatesExplicitRevoke() {
        SystemEntitlementRevokeReqDTO reqDTO = SystemEntitlementRevokeReqDTO.builder()
                .tenantId(122L)
                .sourceType("EDHR_PROCESS_FORM_FILLER")
                .sourceKey("ROUTE|9001|8001|11")
                .policyCode("MES_EDHR_FILLER_MINIMAL")
                .operatorUserId(1L)
                .operatorUsername("admin")
                .build();

        permissionApi.revokeEntitlementSource(reqDTO);

        verify(entitlementService).revokeEntitlementSource(122L, "EDHR_PROCESS_FORM_FILLER",
                "ROUTE|9001|8001|11", "MES_EDHR_FILLER_MINIMAL", 1L, "admin");
    }

}
