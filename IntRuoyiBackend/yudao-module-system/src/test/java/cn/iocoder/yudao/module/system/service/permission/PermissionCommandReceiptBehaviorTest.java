package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleMenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.UserRoleDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMenuMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.UserRoleMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.PermissionCommandReceiptMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.*;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.*;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditAppendResult;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Mockito.*;

/** Service protocol checkpoint only: mocks do not prove receipt SQL/transaction guarantees. */
class PermissionCommandReceiptBehaviorTest {
    enum Operation { ROLE_MENU, USER_ROLE, DATA_SCOPE }

    private final ObjectMapper json = new ObjectMapper();
    private PermissionServiceImpl service;
    private RoleMenuMapper roleMenus;
    private UserRoleMapper userRoles;
    private RoleService roles;
    private RoleMapper roleMapper;
    private GxpAuditService audit;
    private GxpAuditCommand captured;
    private PermissionCommandReceiptMapper receipts;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(91L);
        service = new PermissionServiceImpl();
        roleMenus = mock(RoleMenuMapper.class);
        userRoles = mock(UserRoleMapper.class);
        roles = mock(RoleService.class);
        audit = mock(GxpAuditService.class);
        ReflectionTestUtils.setField(service, "roleMenuMapper", roleMenus);
        ReflectionTestUtils.setField(service, "userRoleMapper", userRoles);
        ReflectionTestUtils.setField(service, "roleService", roles);
        ReflectionTestUtils.setField(service, "menuService", mock(MenuService.class));
        // Real protocol with explicit SQL-port doubles; the separate H2 class proves persistence.
        PermissionCommandProtocol protocol = new PermissionCommandProtocol();
        receipts = mock(PermissionCommandReceiptMapper.class);
        when(receipts.insert(any())).thenReturn(1);
        GxpAuditPolicyActivationMapper activations = mock(GxpAuditPolicyActivationMapper.class);
        GxpAuditPolicyActivationDO activation = new GxpAuditPolicyActivationDO();
        activation.setPolicyVersion("fixture");
        when(activations.selectLatestForUpdate(91L)).thenReturn(activation);
        GxpAuditPolicyOperationMapper operations = mock(GxpAuditPolicyOperationMapper.class);
        GxpAuditPolicyOperationDO policy = new GxpAuditPolicyOperationDO();
        policy.setActive(true); policy.setApplicability("GXP");
        when(operations.selectByPolicyVersionForUpdate(eq(91L), eq("fixture"), anyString())).thenReturn(policy);
        ReflectionTestUtils.setField(protocol, "receipts", receipts);
        ReflectionTestUtils.setField(protocol, "events", mock(GxpAuditEventMapper.class));
        ReflectionTestUtils.setField(protocol, "activations", activations);
        ReflectionTestUtils.setField(protocol, "operations", operations);
        ReflectionTestUtils.setField(protocol, "audit", audit);
        ReflectionTestUtils.setField(service, "permissionCommandProtocol", protocol);
        LoginUser actor = new LoginUser(); actor.setId(1001L); actor.setTenantId(91L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor, null));
        roleMapper = mock(RoleMapper.class);
        AdminUserMapper userMapper = mock(AdminUserMapper.class);
        AdminUserDO user = new AdminUserDO(); user.setId(12L); user.setTenantId(91L);
        when(userMapper.selectPermissionSubjectForUpdate(91L, 12L)).thenReturn(user);
        ReflectionTestUtils.setField(service, "roleMapper", roleMapper);
        ReflectionTestUtils.setField(service, "adminUserMapper", userMapper);
        RoleDO role = new RoleDO();
        role.setId(12L);
        role.setDataScope(1);
        role.setDataScopeDeptIds(Set.of());
        when(roles.getRole(12L)).thenReturn(role);
        when(roleMapper.selectPermissionSubjectForUpdate(91L, 12L)).thenReturn(role);
        when(audit.append(any())).thenAnswer(invocation -> {
            captured = invocation.getArgument(0);
            return new GxpAuditAppendResult(301L, 1L, "ab".repeat(32), false);
        });
    }

    @AfterEach
    void cleanup() { TenantContextHolder.clear(); SecurityContextHolder.clearContext(); }

    @ParameterizedTest @EnumSource(Operation.class)
    void digestIndexHitMustStillMatchOriginalKeyBytes(Operation op) throws Exception {
        invoke(op, "collision-original");
        var saved = org.mockito.ArgumentCaptor.forClass(
                cn.iocoder.yudao.module.system.dal.dataobject.permission.PermissionCommandReceiptDO.class);
        verify(receipts).insert(saved.capture());
        var receipt = saved.getValue();
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest("collision-original".getBytes(StandardCharsets.UTF_8));
        assertArrayEquals(digest, receipt.getSourceKeySha256());
        // SQL-port fault injection, not a claim to have found a real SHA-256 collision.
        receipt.setSourceKey("different-original-bytes".getBytes(StandardCharsets.UTF_8));
        when(receipts.selectForUpdate(eq(91L), eq(captured.getOperationId()), aryEq(digest)))
                .thenReturn(receipt);
        clearInvocations(receipts, roleMapper, roleMenus, userRoles, roles, audit);
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> invoke(op, "collision-original"));
        assertTrue(failure.getMessage().contains("receipt-binding-conflict"));
        verify(receipts).selectForUpdate(eq(91L), eq(captured.getOperationId()), aryEq(digest));
        verify(receipts, never()).insert(any());
        verify(audit, never()).append(any());
        verifyNoInteractions(roleMapper, roleMenus, userRoles, roles);
    }

    private void invoke(Operation op, String key) {
        switch (op) {
            case ROLE_MENU -> service.assignRoleMenu(12L, Set.of(2L, 10L), "controlled change", key);
            case USER_ROLE -> service.assignUserRole(12L, Set.of(2L, 10L), "controlled change", key);
            case DATA_SCOPE -> service.assignRoleDataScope(12L, 2, Set.of(2L, 10L), "controlled change", key);
        }
    }

    @ParameterizedTest
    @EnumSource(Operation.class)
    void acceptsExactUtf8BoundaryAndSupplementaryCharacters(Operation op) {
        for (String key : List.of("a".repeat(1024), "\uD83D\uDE00".repeat(256), "界".repeat(341) + "a")) {
            assertEquals(1024, key.getBytes(StandardCharsets.UTF_8).length);
            assertDoesNotThrow(() -> invoke(op, key));
            assertNotNull(captured, "legal input must reach the audit boundary");
        }
    }

    @ParameterizedTest
    @EnumSource(Operation.class)
    void rejectsOversizeKeysBeforeAnyBusinessAccess(Operation op) {
        for (String key : List.of("a".repeat(1025), "界".repeat(342))) {
            assertThrows(RuntimeException.class, () -> invoke(op, key));
        }
        verifyNoInteractions(roleMenus, userRoles, roles);
        verify(audit, never()).append(any());
    }

    @ParameterizedTest
    @EnumSource(Operation.class)
    void rejectsUnpairedSurrogatesWithoutReplacement(Operation op) {
        for (String key : List.of("x\uD800", "\uDC00x")) {
            assertThrows(RuntimeException.class, () -> invoke(op, key));
        }
        verifyNoInteractions(roleMenus, userRoles, roles);
        verify(audit, never()).append(any());
    }

    @ParameterizedTest
    @EnumSource(Operation.class)
    void rejectsBlankKeysBeforeBusinessAccess(Operation op) {
        for (String key : List.of("", " ", "\t\r\n")) {
            assertThrows(RuntimeException.class, () -> invoke(op, key));
        }
        verifyNoInteractions(roleMenus, userRoles, roles);
        verify(audit, never()).append(any());
    }

    @ParameterizedTest
    @EnumSource(Operation.class)
    void bindsExactOriginalKeyToCanonicalGxp2Identity(Operation op) throws Exception {
        for (String key : List.of("Key", "key", "é", "e\u0301", " key ")) {
            invoke(op, key);
            String subjectType = op == Operation.USER_ROLE ? "SYSTEM_USER" : "SYSTEM_ROLE";
            // Independent fixed-order canonical JSON oracle, not production identity helper.
            String canonical = "{\"identity\":" + json.writeValueAsString(List.of(subjectType, "12", key))
                    + ",\"operationId\":" + json.writeValueAsString(captured.getOperationId())
                    + ",\"tenantId\":\"91\"}";
            assertEquals("GXP2:" + sha256(canonical), captured.getIdempotencyKey());
        }
    }

    @ParameterizedTest
    @EnumSource(Operation.class)
    void includesSubjectInClosedDomainSnapshot(Operation op) throws Exception {
        invoke(op, "profile-case");
        JsonNode before = json.readTree(captured.getBeforeState().getCanonicalJson());
        JsonNode after = json.readTree(captured.getAfterState().getCanonicalJson());
        String id = op == Operation.USER_ROLE ? "userId" : "roleId";
        assertEquals("12", before.path(id).asText());
        assertEquals("12", after.path(id).asText());
        assertEquals(op == Operation.DATA_SCOPE ? 3 : 2, before.size());
        assertEquals(op == Operation.DATA_SCOPE ? 3 : 2, after.size());
        assertFalse(after.has("sourceKey"), "source belongs in the receipt, not business data");
    }

    @ParameterizedTest
    @EnumSource(Operation.class)
    void bindsReceiptSourceThroughTypedEvidence(Operation op) {
        invoke(op, "source-case");
        assertNotNull(captured.getEvidences(), "success requires command-source receipt evidence");
        var sources = captured.getEvidences().stream()
                .filter(e -> "SYSTEM_PERMISSION_COMMAND_RECEIPT".equals(e.evidenceType())).toList();
        assertEquals(1, sources.size());
        var source = sources.get(0);
        assertEquals("COMMAND_SOURCE", source.role());
        assertEquals("1", source.sourceVersion());
        assertTrue(source.sourceId().matches("[1-9][0-9]*"));
        assertTrue(source.sha256().matches("[0-9a-f]{64}"));
        assertEquals("PermissionServiceImpl", captured.getSource());
    }

    @ParameterizedTest
    @EnumSource(Operation.class)
    void refusesSuccessWhenAppendProvidesNoReceipt(Operation op) {
        when(audit.append(any())).thenReturn(null);
        assertThrows(RuntimeException.class, () -> invoke(op, "missing-append-receipt"));
    }

    @ParameterizedTest
    @EnumSource(Operation.class)
    void refusesMalformedEventHashRatherThanTruncatingIt(Operation op) {
        when(audit.append(any())).thenReturn(new GxpAuditAppendResult(301L, 1L, "not-hex", false));
        assertThrows(RuntimeException.class, () -> invoke(op, "invalid-hash"));
    }

    @ParameterizedTest
    @EnumSource(Operation.class)
    void propagatesAppendFailureWithoutReturningVoidSuccess(Operation op) {
        IllegalStateException failure = new IllegalStateException("injected append failure");
        when(audit.append(any())).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class, () -> invoke(op, "append-failure")));
    }

    @ParameterizedTest @EnumSource(Operation.class)
    void objectVersionsUseIndependentPrefixedSnapshotHashes(Operation op) throws Exception {
        invoke(op, "version-contract");
        String beforeVersion = "sha256:" + sha256(captured.getBeforeState().getCanonicalJson());
        String afterVersion = "sha256:" + sha256(captured.getAfterState().getCanonicalJson());
        assertAll(
                () -> assertEquals(beforeVersion, captured.getBeforeState().getObjectVersion()),
                () -> assertEquals(afterVersion, captured.getAfterState().getObjectVersion()),
                () -> assertEquals(afterVersion, captured.getSubjectVersion()));
    }

    @ParameterizedTest @EnumSource(Operation.class)
    void samePersistedAfterHasSameSubjectVersionDespiteDifferentBefore(Operation op) throws Exception {
        // Four distinct current-read results: before-A, after, before-B, same after.
        // Do not mutate one shared DO and accidentally change the captured before snapshot.
        switch (op) {
            case ROLE_MENU -> when(roleMenus.selectPermissionRowsForUpdate(91L, 12L)).thenReturn(
                    List.of(), menuRows(2L, 10L), menuRows(3L), menuRows(2L, 10L));
            case USER_ROLE -> when(userRoles.selectPermissionRowsForUpdate(91L, 12L)).thenReturn(
                    List.of(), userRoleRows(2L, 10L), userRoleRows(3L), userRoleRows(2L, 10L));
            case DATA_SCOPE -> when(roleMapper.selectPermissionSubjectForUpdate(91L, 12L)).thenReturn(
                    scopeRole(1, Set.of()), scopeRole(2, Set.of(2L, 10L)),
                    scopeRole(2, Set.of(3L)), scopeRole(2, Set.of(2L, 10L)));
        }
        invoke(op, "same-after-first");
        GxpAuditCommand first = captured;
        invoke(op, "same-after-second");
        GxpAuditCommand second = captured;
        assertNotEquals(first.getBeforeState().getCanonicalJson(), second.getBeforeState().getCanonicalJson());
        assertEquals(first.getAfterState().getCanonicalJson(), second.getAfterState().getCanonicalJson());
        String expected = "sha256:" + sha256(first.getAfterState().getCanonicalJson());
        assertAll(
                () -> assertEquals(first.getSubjectVersion(), second.getSubjectVersion()),
                () -> assertEquals(expected, first.getSubjectVersion()),
                () -> assertEquals(expected, second.getSubjectVersion()));
    }

    private static List<RoleMenuDO> menuRows(Long... ids) {
        return java.util.Arrays.stream(ids).map(id -> {
            RoleMenuDO row = new RoleMenuDO(); row.setRoleId(12L); row.setMenuId(id); row.setTenantId(91L);
            return row;
        }).toList();
    }

    private static List<UserRoleDO> userRoleRows(Long... ids) {
        return java.util.Arrays.stream(ids).map(id -> {
            UserRoleDO row = new UserRoleDO(); row.setUserId(12L); row.setRoleId(id); row.setTenantId(91L);
            return row;
        }).toList();
    }

    private static RoleDO scopeRole(int scope, Set<Long> departments) {
        RoleDO row = new RoleDO(); row.setId(12L); row.setTenantId(91L);
        row.setDataScope(scope); row.setDataScopeDeptIds(departments);
        return row;
    }

    private static String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
