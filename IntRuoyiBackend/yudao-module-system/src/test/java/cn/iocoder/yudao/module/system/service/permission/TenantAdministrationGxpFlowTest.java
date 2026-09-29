package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.service.tenant.*;
import cn.iocoder.yudao.module.system.controller.admin.tenant.vo.tenant.TenantSaveReqVO;
import cn.iocoder.yudao.module.system.controller.admin.tenant.vo.packages.TenantPackageSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.tenant.TenantDO;
import cn.iocoder.yudao.module.system.dal.dataobject.tenant.TenantPackageDO;
import cn.iocoder.yudao.module.system.dal.mysql.tenant.TenantMapper;
import cn.iocoder.yudao.module.system.dal.mysql.tenant.TenantPackageMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real H2 transaction, permission protocol and audit writer; user/role creation are fixture ports. */
@Import({TenantServiceImpl.class, TenantPackageServiceImpl.class})
class TenantAdministrationGxpFlowTest extends PermissionReceiptPersistenceTest {
    @Resource TenantServiceImpl tenantService;
    @Resource TenantPackageServiceImpl packageService;
    @Resource TenantMapper tenants;
    @Resource TenantPackageMapper packages;
    @MockitoSpyBean PermissionServiceImpl permissionService;
    private JdbcTemplate sql;

    @BeforeEach void tenantFixture() {
        sql = new JdbcTemplate(dataSource);
        tenants.insert(tenant(1L, 0L, "platform"));
        tenants.insert(tenant(20L, 10L, "target"));
        tenants.insert(tenant(30L, 10L, "second"));
        sql.execute("ALTER TABLE system_tenant ALTER COLUMN id RESTART WITH 1000");
        packages.insert(new TenantPackageDO().setId(10L).setName("old").setStatus(0).setMenuIds(Set.of(2L)));
        packages.insert(new TenantPackageDO().setId(11L).setName("new").setStatus(0).setMenuIds(Set.of(3L)));
        sql.update("INSERT INTO system_role(id,name,code,sort,status,type,tenant_id) VALUES(200,'admin','tenant_admin',1,0,1,20),(300,'admin','tenant_admin',1,0,1,30)");
        sql.update("INSERT INTO system_role_menu(role_id,menu_id,tenant_id) VALUES(200,2,20),(300,2,30)");
        doReturn(true).when(permissionService).hasAnyPermissions(eq(1001L), any(String[].class));
        when(roles.getRoleList()).thenAnswer(i -> roleMapper.selectList(RoleDO::getTenantId, TenantContextHolder.getRequiredTenantId()));
        when(roles.createRole(any(), anyInt())).thenAnswer(i -> {
            Long tenant = TenantContextHolder.getRequiredTenantId();
            sql.update("INSERT INTO system_role(id,name,code,sort,status,type,tenant_id) VALUES(900,'admin','tenant_admin',1,0,1,?)", tenant);
            return 900L;
        });
        when(users.createUser(any())).thenAnswer(i -> {
            sql.update("INSERT INTO system_users(id,username,canonical_username,nickname,tenant_id) VALUES(901,'newadmin','newadmin','admin',?)", TenantContextHolder.getRequiredTenantId());
            return 901L;
        });
    }
    private TenantDO tenant(Long id, Long packageId, String name) {
        return new TenantDO().setId(id).setName(name).setContactName("test").setStatus(0)
                .setPackageId(packageId).setExpireTime(LocalDateTime.now().plusYears(1)).setAccountCount(10);
    }
    private TenantSaveReqVO request(Long id, Long packageId, String name) {
        return new TenantSaveReqVO().setId(id).setName(name).setContactName("test").setStatus(0)
                .setPackageId(packageId).setExpireTime(LocalDateTime.now().plusYears(1)).setAccountCount(10)
                .setUsername("newadmin").setPassword("Test-only-123!");
    }
    @Test void createsTenantWithoutTargetAuditInitialization() {
        Long id = tenantService.createTenant(request(null,10L,"created"));
        assertEquals(901L, tenants.selectById(id).getContactUserId());
        assertEquals(1, sql.queryForObject("SELECT COUNT(*) FROM system_user_role WHERE user_id=901 AND role_id=900 AND tenant_id=?",Integer.class,id));
        assertEquals(2, sql.queryForObject("SELECT COUNT(*) FROM gxp_audit_event WHERE tenant_id=1 AND actor_id=1001",Integer.class));
        assertEquals(0, sql.queryForObject("SELECT COUNT(*) FROM gxp_audit_ledger_sequence WHERE tenant_id=?",Integer.class,id));
        assertEquals(1L,TenantContextHolder.getRequiredTenantId());
        assertEquals(1L, SecurityFrameworkUtils.getLoginUser().getTenantId());
        for (String state : sql.queryForList("SELECT after_state_json FROM gxp_audit_event", String.class)) {
            assertTrue(state.contains("targetTenantId"));
            assertTrue(state.contains(id.toString()));
        }
        for (String source : sql.queryForList("SELECT source_json FROM system_gxp_command_receipt", String.class)) {
            assertTrue(source.contains("targetTenantId"));
            assertTrue(source.contains(id.toString()));
        }
    }
    @Test void changesTenantPackageAndRoleMenus() {
        tenantService.updateTenant(request(20L,11L,"target"));
        assertEquals(11L,tenants.selectById(20L).getPackageId());
        assertEquals(List.of(3L), sql.queryForList("SELECT menu_id FROM system_role_menu WHERE role_id=200 AND deleted=false",Long.class));
    }
    @Test void synchronizesPackageMenusAcrossTwoTenants() {
        packageService.updateTenantPackage(new TenantPackageSaveReqVO().setId(10L).setName("old").setStatus(0).setMenuIds(Set.of(4L)));
        assertEquals(List.of(4L,4L), sql.queryForList("SELECT menu_id FROM system_role_menu WHERE role_id IN(200,300) AND deleted=false ORDER BY role_id",Long.class));
        assertEquals(2,sql.queryForObject("SELECT COUNT(*) FROM gxp_audit_event WHERE tenant_id=1",Integer.class));
    }
    @Test void auditFailureRollsBackNewTenantAndCreatedPrincipals() {
        probe.arm(null,null,new IllegalStateException("audit fault"));
        assertThrows(RuntimeException.class,()->tenantService.createTenant(request(null,10L,"failed")));
        assertNull(tenants.selectByName("failed"));
        assertEquals(0,sql.queryForObject("SELECT COUNT(*) FROM system_role WHERE id=900",Integer.class));
        assertEquals(0,sql.queryForObject("SELECT COUNT(*) FROM system_users WHERE id=901",Integer.class));
        assertEquals(1L,TenantContextHolder.getRequiredTenantId());
    }

    @Test void deniesManagementWithoutMatchingPermissionAndRollsBack() {
        doReturn(false).when(permissionService).hasAnyPermissions(eq(1001L), any(String[].class));
        assertThrows(RuntimeException.class, () -> tenantService.updateTenant(request(20L,11L,"target")));
        assertEquals(10L, tenants.selectById(20L).getPackageId());
        assertEquals(0, sql.queryForObject("SELECT COUNT(*) FROM gxp_audit_event",Integer.class));
    }
    @Test void deniesOrdinaryTenantEvenWithManagementPermission() {
        sql.update("UPDATE system_tenant SET package_id=10 WHERE id=1");
        assertThrows(RuntimeException.class, () -> tenantService.createTenant(request(null,10L,"denied")));
        assertNull(tenants.selectByName("denied"));
    }
    @Test void ordinaryPermissionEndpointStillRejectsCrossTenant() {
        assertThrows(RuntimeException.class, () -> TenantUtils.execute(20L,
                () -> permissionService.assignRoleMenu(200L,Set.of(99L),"test","ordinary-cross")));
        assertEquals(List.of(2L),sql.queryForList("SELECT menu_id FROM system_role_menu WHERE role_id=200 AND deleted=false",Long.class));
        assertEquals(0,sql.queryForObject("SELECT COUNT(*) FROM gxp_audit_event",Integer.class));
    }
    @Test void cannotInitializeAnUnrelatedUserAsTenantAdministrator() {
        assertThrows(RuntimeException.class, () -> TenantUtils.execute(20L,
                () -> permissionService.initializeTenantAdministrator(20L,12L,200L,"test","invalid-user")));
        assertEquals(0,sql.queryForObject("SELECT COUNT(*) FROM system_user_role",Integer.class));
    }
    @Test void laterTenantAuditFailureRollsBackEntirePackageSynchronization() {
        AtomicInteger attempts = new AtomicInteger();
        probe.arm(connection -> {
            if (attempts.incrementAndGet() == 2) throw new IllegalStateException("second tenant audit fault");
        },null,null);
        assertThrows(RuntimeException.class, () -> packageService.updateTenantPackage(
                new TenantPackageSaveReqVO().setId(10L).setName("old").setStatus(0).setMenuIds(Set.of(4L))));
        assertEquals(2,attempts.get(),"must fail after the first tenant's business and audit writes");
        assertEquals(Set.of(2L),packages.selectById(10L).getMenuIds());
        assertEquals(List.of(2L,2L),sql.queryForList("SELECT menu_id FROM system_role_menu WHERE role_id IN(200,300) AND deleted=false ORDER BY role_id",Long.class));
        assertEquals(0,sql.queryForObject("SELECT COUNT(*) FROM gxp_audit_event",Integer.class));
        assertEquals(0,sql.queryForObject("SELECT COUNT(*) FROM system_gxp_command_receipt",Integer.class));
        assertEquals(1L,sql.queryForObject("SELECT next_ledger_sequence FROM gxp_audit_ledger_sequence WHERE tenant_id=1",Long.class));
    }
    @Test void repeatedTenantPackageChoiceIsANewCommand() {
        tenantService.updateTenant(request(20L,11L,"target"));
        tenantService.updateTenant(request(20L,10L,"target"));
        tenantService.updateTenant(request(20L,11L,"target"));
        assertEquals(List.of(3L),sql.queryForList("SELECT menu_id FROM system_role_menu WHERE role_id=200 AND deleted=false",Long.class));
        assertEquals(3,sql.queryForObject("SELECT COUNT(*) FROM system_gxp_command_receipt",Integer.class));
    }
}
