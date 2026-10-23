package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.service.file.DccApplicationCustomMatrix.ApprovalSourceMode;
import cn.iocoder.yudao.module.dcc.service.file.DccApplicationCustomMatrixService.PreviewSourceContext;
import cn.iocoder.yudao.module.dcc.service.file.DccApplicationCustomMatrixService.SourceContext;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ROUTE_NOT_READY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Actual custom preview/reserved resolver and canonical/token services; only directory ports isolated. */
class DccCustomMissingDepartmentLeaderTest {
    private static final List<Long> DEPARTMENTS = List.of(51L, 52L);
    private static final String MISSING_DEPARTMENT = "DCC241016C15无负责人会签部";
    private final DccApplicationCustomMatrixService matrices = new DccApplicationCustomMatrixService();
    private final DccApplicationCustomMatrix matrix = new DccApplicationCustomMatrix(77L, 66L);
    private DccControlledFileApprovalRouteAssigneeResolver resolver;
    private AdminUserApi users;
    private DeptApi departments;

    @BeforeEach
    void setUpCustomRouteWithValidAuthorAndSupervisor() {
        TenantContextHolder.setTenantId(1L);
        SecurityFrameworkUtils.setLoginUser(new LoginUser().setId(99L).setTenantId(1L).setUserType(2),
                new MockHttpServletRequest());
        users = mock(AdminUserApi.class);
        departments = mock(DeptApi.class);
        when(users.getUser(99L)).thenReturn(account(99L));
        when(users.getUser(88L)).thenReturn(account(88L));
        var provider = new StaticListableBeanFactory();
        provider.addBean("testDirectSupervisor", (DccDirectSupervisorSource) context ->
                new DccDirectSupervisorSource.Evidence(context.tenantId(), context.requesterId(), 88L,
                        "USER_MANAGEMENT", "DCC241016C15-supervisor", "1"));
        var properties = new DccApprovalMatrixTokenProperties();
        properties.setSigningSecret("DCC241016C15-unit-signing-key-not-runtime-32-bytes");
        var tokens = new DccApprovalMatrixTokenService();
        ReflectionTestUtils.setField(tokens, "properties", properties);
        resolver = new DccControlledFileApprovalRouteAssigneeResolver();
        ReflectionTestUtils.setField(resolver, "adminUserApi", users);
        ReflectionTestUtils.setField(resolver, "deptApi", departments);
        ReflectionTestUtils.setField(resolver, "nativeSupervisors",
                provider.getBeanProvider(DccDirectSupervisorSource.class));
        ReflectionTestUtils.setField(resolver, "nativeSourceTokens", tokens);
    }

    @AfterEach
    void clearIdentity() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    @ParameterizedTest
    @ValueSource(strings = {"PREVIEW", "RESERVED"})
    void missingLeaderMustBlockCustomRouteAndIdentifyTheSignoffDepartment(String phase) {
        when(departments.getDeptList(DEPARTMENTS)).thenReturn(List.of(
                department(51L, "DCC241016C15完整会签部", 700L),
                department(52L, MISSING_DEPARTMENT, null)));

        ServiceException failure = assertThrows(ServiceException.class, () -> resolve(phase));

        assertAll("missing department leader must be an actionable business refusal",
                () -> assertEquals(CONTROLLED_FILE_ROUTE_NOT_READY.getCode(), failure.getCode()),
                () -> assertTrue(failure.getMessage().contains("会签部门"), failure.getMessage()),
                () -> assertTrue(failure.getMessage().contains("部门负责人"), failure.getMessage()),
                () -> assertTrue(failure.getMessage().contains(MISSING_DEPARTMENT), failure.getMessage()));
        verify(departments).validateDeptList(DEPARTMENTS);
        verify(users, never()).validateUserList(any());
    }

    @Test
    void completeDepartmentLeadersRetainAllCustomRouteObligations() {
        when(departments.getDeptList(DEPARTMENTS)).thenReturn(List.of(
                department(51L, "DCC241016C15完整会签部", 700L),
                department(52L, MISSING_DEPARTMENT, 701L)));

        var route = resolve("PREVIEW");

        assertEquals(List.of("SUPERVISOR_REVIEW", "MATRIX_REVIEW", "MATRIX_APPROVAL", "DOC_CONTROL_REVIEW"),
                route.nodes().stream().map(DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode::stageCode).toList());
        assertEquals(DEPARTMENTS, route.nodes().get(1).candidateSourceIds());
        assertEquals(List.of(700L, 701L), route.nodes().get(1).resolvedUserIds());
        assertTrue(route.nodes().get(1).requireAllApprovals());
        assertEquals(List.of(77L), route.nodes().get(2).resolvedUserIds());
        assertEquals(List.of(66L), route.nodes().get(3).resolvedUserIds());
    }

    @ParameterizedTest
    @CsvSource({
            "PREVIEW,99,编制人,true", "PREVIEW,99,编制人,false",
            "RESERVED,99,编制人,true", "RESERVED,99,编制人,false",
            "PREVIEW,88,编制人直接主管,true", "PREVIEW,88,编制人直接主管,false",
            "RESERVED,88,编制人直接主管,true", "RESERVED,88,编制人直接主管,false"
    })
    void missingAccountPostIsActionableAndCannotResolveRoute(String phase, Long userId,
                                                            String responsibility, boolean nullPosts) {
        when(users.getUser(userId)).thenReturn(account(userId).setPostIds(nullPosts ? null : Set.of()));

        ServiceException failure = assertThrows(ServiceException.class, () -> resolve(phase));

        assertEquals(CONTROLLED_FILE_ROUTE_NOT_READY.getCode(), failure.getCode());
        assertTrue(failure.getMessage().contains(responsibility + "未配置系统岗位"), failure.getMessage());
        verifyNoInteractions(departments);
    }

    @Test
    void crossTenantAccountStillFailsIdentityBeforeMissingPostValidation() {
        when(users.getUser(99L)).thenReturn(account(99L).setTenantId(2L).setPostIds(Set.of()));
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> resolve("PREVIEW"));
        assertEquals("NATIVE_SUPERVISOR_ENABLED_ACCOUNT_REQUIRED", failure.getMessage());
        verifyNoInteractions(departments);
    }

    private DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute resolve(String phase) {
        if ("PREVIEW".equals(phase)) {
            var context = new PreviewSourceContext(ApprovalSourceMode.APPLICATION_CUSTOM, 1L, 5L, 2L, 6L,
                    99L, "UPLOAD", "DCC241016C15-preview", "upload:6:exact", "a".repeat(64));
            return resolver.resolveCustomPreview(99L, context,
                    matrices.canonicalizePreview(context, matrix, DEPARTMENTS));
        }
        var context = new SourceContext(ApprovalSourceMode.APPLICATION_CUSTOM, 1L, 5L, 2L, 6L,
                99L, 20L, 1, "UPLOAD", "DCC241016C15-reserved", "upload:6:exact", "a".repeat(64));
        return resolver.resolveCustomReserved(99L, context,
                matrices.canonicalize(context, matrix, DEPARTMENTS));
    }

    private static AdminUserRespDTO account(Long id) {
        return new AdminUserRespDTO().setId(id).setTenantId(1L).setStatus(0).setDeptId(51L).setPostIds(Set.of(10L));
    }

    private static DeptRespDTO department(Long id, String name, Long leader) {
        return new DeptRespDTO().setId(id).setName(name).setStatus(0).setLeaderUserId(leader);
    }
}
