package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TenantAdminBusinessPolicyTest {
    @Test void onlyEnabledTenantAdministratorReceivesBusinessAccess() {
        assertTrue(TenantAdminBusinessPolicy.isTenantAdmin(List.of(new RoleDO().setCode("tenant_admin").setStatus(0))));
        assertFalse(TenantAdminBusinessPolicy.isTenantAdmin(List.of(new RoleDO().setCode("tenant_admin").setStatus(1))));
        assertFalse(TenantAdminBusinessPolicy.isTenantAdmin(List.of(new RoleDO().setCode("operator").setStatus(0))));
    }
    @Test void platformPermissionsAreNotGrantedEvenUnderBusinessPath() {
        for (String permission : List.of("system:tenant:update", "system:tenant-package:update", "system:menu:create", "infra:config:update")) {
            assertFalse(TenantAdminBusinessPolicy.isBusinessPermission(permission));
            assertFalse(TenantAdminBusinessPolicy.isBusinessMenu(new MenuDO().setStatus(0)
                    .setPath("/mes").setPermission(permission)));
        }
    }
    @Test void businessEntriesAndButtonsAreGrantedButDisabledEntriesAreNot() {
        for (String permission : List.of("mes:pro-feedback:create", "srm:supplier:query", "dcc:document:update", "erp:invoice-voucher-print:query")) {
            assertTrue(TenantAdminBusinessPolicy.isBusinessPermission(permission));
            assertTrue(TenantAdminBusinessPolicy.isBusinessMenu(new MenuDO().setStatus(0).setPermission(permission)));
            assertFalse(TenantAdminBusinessPolicy.isBusinessMenu(new MenuDO().setStatus(1).setPermission(permission)));
        }
        assertTrue(TenantAdminBusinessPolicy.isBusinessMenu(new MenuDO().setStatus(0).setPath("controlled-file/admin")));
    }
}
