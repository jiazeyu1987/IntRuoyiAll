package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.SystemEntitlementClaimDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.SystemEntitlementGrantDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.SystemEntitlementPolicyDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.MenuMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.SystemEntitlementClaimMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.SystemEntitlementGrantMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.SystemEntitlementPolicyMapper;
import cn.iocoder.yudao.module.system.service.permission.bo.SystemEntitlementSyncCommand;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString;
import static org.junit.jupiter.api.Assertions.*;

@Import(SystemEntitlementServiceImpl.class)
class SystemEntitlementServiceTest extends BaseDbUnitTest {

    private static final Long TENANT_ID = 122L;
    private static final String POLICY_CODE = "MES_EDHR_FILLER_MINIMAL";
    private static final String SOURCE_TYPE = "EDHR_PROCESS_FORM_FILLER";
    private static final String PERMISSION_BATCH_QUERY = "mes:pro-edhr-batch-execution:query";
    private static final String PERMISSION_BATCH_UPDATE = "mes:pro-edhr-batch-execution:update";
    private static final String PERMISSION_EXECUTION_QUERY = "mes:pro-batch-record-execution:query";
    private static final String PERMISSION_EXECUTION_UPDATE = "mes:pro-batch-record-execution:update";
    private static final String PERMISSION_FORBIDDEN_CLOSE = "mes:pro-edhr-batch-execution:close";

    @Resource
    private SystemEntitlementService entitlementService;
    @Resource
    private SystemEntitlementPolicyMapper policyMapper;
    @Resource
    private SystemEntitlementClaimMapper claimMapper;
    @Resource
    private SystemEntitlementGrantMapper grantMapper;
    @Resource
    private MenuMapper menuMapper;

    @BeforeEach
    void setUp() {
        insertMenu(101L, PERMISSION_BATCH_QUERY);
        insertMenu(102L, PERMISSION_BATCH_UPDATE);
        insertMenu(103L, PERMISSION_EXECUTION_QUERY);
        insertMenu(104L, PERMISSION_EXECUTION_UPDATE);
        insertMenu(199L, PERMISSION_FORBIDDEN_CLOSE);
        policyMapper.insert(new SystemEntitlementPolicyDO()
                .setPolicyCode(POLICY_CODE)
                .setPolicyName("eDHR 填写人最小权益")
                .setModuleCode("mes")
                .setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setAllowedPermissionCodesJson(toJsonString(List.of(
                        PERMISSION_BATCH_QUERY,
                        PERMISSION_BATCH_UPDATE,
                        PERMISSION_EXECUTION_QUERY,
                        PERMISSION_EXECUTION_UPDATE)))
                .setForbiddenPermissionCodesJson(toJsonString(List.of(PERMISSION_FORBIDDEN_CLOSE))));
    }

    @Test
    void entitlementPolicy_isGlobalTableAndMustIgnoreTenantInterceptor() {
        assertNotNull(SystemEntitlementPolicyDO.class.getAnnotation(TenantIgnore.class));
    }

    @Test
    void syncClaims_grantsOnlyMinimalPermissionsAndIsIdempotent() {
        entitlementService.syncClaims(command("routeA|reportA|v1|rule1", Set.of(101L), "digest-a"));
        entitlementService.syncClaims(command("routeA|reportA|v1|rule1", Set.of(101L), "digest-a"));

        List<SystemEntitlementGrantDO> grants = grantMapper.selectActiveListByUserId(TENANT_ID, 101L);

        assertEquals(4, grants.size());
        assertTrue(grants.stream().anyMatch(grant -> PERMISSION_BATCH_QUERY.equals(grant.getPermissionCode())));
        assertTrue(grants.stream().noneMatch(grant -> PERMISSION_FORBIDDEN_CLOSE.equals(grant.getPermissionCode())));
        assertTrue(grants.stream().allMatch(grant -> Integer.valueOf(1).equals(grant.getActiveClaimCount())));
    }

    @Test
    void syncClaims_transfersOwnershipWhenFillerChanges() {
        entitlementService.syncClaims(command("routeA|reportA|v1|rule1", Set.of(101L), "digest-a"));

        entitlementService.syncClaims(command("routeA|reportA|v1|rule1", Set.of(202L), "digest-b"));

        List<SystemEntitlementClaimDO> oldClaims = claimMapper.selectListBySource(TENANT_ID, SOURCE_TYPE,
                "routeA|reportA|v1|rule1", POLICY_CODE, 101L);
        assertTrue(oldClaims.stream().allMatch(claim -> "REVOKED".equals(claim.getStatus())));
        assertTrue(grantMapper.selectActiveListByUserId(TENANT_ID, 101L).isEmpty());
        assertEquals(4, grantMapper.selectActiveListByUserId(TENANT_ID, 202L).size());
    }

    @Test
    void syncClaims_reactivatesRevokedClaimWhenFillerReturnsToSameUser() {
        entitlementService.syncClaims(command("routeA|reportA|v1|rule1", Set.of(101L), "digest-a"));
        entitlementService.syncClaims(command("routeA|reportA|v1|rule1", Set.of(202L), "digest-b"));

        entitlementService.syncClaims(command("routeA|reportA|v1|rule1", Set.of(101L), "digest-c"));

        List<SystemEntitlementClaimDO> oldClaims = claimMapper.selectListBySource(TENANT_ID, SOURCE_TYPE,
                "routeA|reportA|v1|rule1", POLICY_CODE, 101L);
        assertEquals(1, oldClaims.size());
        assertEquals("ACTIVE", oldClaims.get(0).getStatus());
        assertEquals("digest-c", oldClaims.get(0).getSourceDigest());
        assertNull(oldClaims.get(0).getRevokedAt());
        assertEquals(4, grantMapper.selectActiveListByUserId(TENANT_ID, 101L).size());

        List<SystemEntitlementClaimDO> newClaims = claimMapper.selectListBySource(TENANT_ID, SOURCE_TYPE,
                "routeA|reportA|v1|rule1", POLICY_CODE, 202L);
        assertEquals(1, newClaims.size());
        assertTrue(newClaims.stream().allMatch(claim -> "REVOKED".equals(claim.getStatus())));
        assertTrue(grantMapper.selectActiveListByUserId(TENANT_ID, 202L).isEmpty());
    }

    @Test
    void syncClaims_keepsGrantWhenOtherActiveSourcesRemain() {
        entitlementService.syncClaims(command("routeA|reportA|v1|rule1", Set.of(101L), "digest-a"));
        entitlementService.syncClaims(command("routeB|reportB|v1|rule2", Set.of(101L), "digest-b"));

        entitlementService.syncClaims(command("routeA|reportA|v1|rule1", Set.of(202L), "digest-c"));

        List<SystemEntitlementGrantDO> liminGrants = grantMapper.selectActiveListByUserId(TENANT_ID, 101L);
        assertEquals(4, liminGrants.size());
        assertTrue(liminGrants.stream().allMatch(grant -> Integer.valueOf(1).equals(grant.getActiveClaimCount())));
        assertEquals(4, grantMapper.selectActiveListByUserId(TENANT_ID, 202L).size());
    }

    @Test
    void syncClaims_rejectsEmptySubjectsAndKeepsExistingGrant() {
        entitlementService.syncClaims(command("ROUTE|9001|8001|11", Set.of(101L), "digest-a"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> entitlementService.syncClaims(command("ROUTE|9001|8001|11", Set.of(), "digest-empty")));

        assertTrue(exception.getMessage().contains("empty"));
        assertEquals(4, grantMapper.selectActiveListByUserId(TENANT_ID, 101L).size());
        assertTrue(claimMapper.selectActiveListBySource(TENANT_ID, SOURCE_TYPE, "ROUTE|9001|8001|11", POLICY_CODE)
                .stream().anyMatch(claim -> Long.valueOf(101L).equals(claim.getResolvedUserId())));
    }

    @Test
    void revokeEntitlementSource_revokesOnlySameStableSource() {
        entitlementService.syncClaims(command("ROUTE|9001|8001|11", Set.of(101L), "digest-a"));
        entitlementService.syncClaims(command("ROUTE|9002|8001|11", Set.of(101L), "digest-b"));

        entitlementService.revokeEntitlementSource(TENANT_ID, SOURCE_TYPE, "ROUTE|9001|8001|11", POLICY_CODE,
                1L, "admin");

        assertTrue(claimMapper.selectActiveListBySource(TENANT_ID, SOURCE_TYPE, "ROUTE|9001|8001|11", POLICY_CODE)
                .isEmpty());
        List<SystemEntitlementGrantDO> grants = grantMapper.selectActiveListByUserId(TENANT_ID, 101L);
        assertEquals(4, grants.size());
        assertTrue(grants.stream().allMatch(grant -> Integer.valueOf(1).equals(grant.getActiveClaimCount())));
    }

    private static SystemEntitlementSyncCommand command(String sourceKey, Set<Long> users, String digest) {
        return SystemEntitlementSyncCommand.builder()
                .tenantId(TENANT_ID)
                .sourceType(SOURCE_TYPE)
                .sourceKey(sourceKey)
                .sourceVersion("v1")
                .sourceDigest(digest)
                .policyCode(POLICY_CODE)
                .resolvedUserIds(users)
                .operatorUserId(1L)
                .operatorUsername("admin")
                .build();
    }

    private void insertMenu(Long id, String permission) {
        menuMapper.insert(new MenuDO()
                .setId(id)
                .setName(permission)
                .setPermission(permission)
                .setType(2)
                .setSort(0)
                .setParentId(0L)
                .setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setVisible(true)
                .setKeepAlive(true)
                .setAlwaysShow(true));
    }

}
