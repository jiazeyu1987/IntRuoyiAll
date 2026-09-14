package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteDO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.APPROVAL_POSITION_UPLOADER_MAPPING_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

class DccControlledFileRouteReadinessServiceTest extends BaseMockitoUnitTest {

    @Mock
    private DccControlledFileApprovalRouteAssigneeResolver routeAssigneeResolver;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private DccElectronicSignatureAuthorizationService signatureAuthorizationService;
    @Mock
    private DccElectronicSignatureImageService signatureImageService;

    @InjectMocks
    private DccControlledFileRouteReadinessService service;

    @Test
    void evaluate_aggregatesPostPermissionAuthorizationAndImageBlockers() {
        when(routeAssigneeResolver.resolveRouteForReadiness(10L, 99L)).thenReturn(resolvedRoute(List.of(
                new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode(
                        1, "DOC_CONTROL_REVIEW", "文控审核", 1, "USER", 101L,
                        List.of(101L, 102L, 103L, 104L), "ALL", 100, true,
                        List.of(101L, 102L, 103L, 104L)))));
        when(adminUserApi.getUserList(List.of(101L, 102L, 103L, 104L))).thenReturn(List.of(
                user(101L, "无岗位", Set.of()),
                user(102L, "无权限", Set.of(12L)),
                user(103L, "未授权", Set.of(13L)),
                user(104L, "无签名图", Set.of(14L))));
        when(permissionApi.hasAnyPermissions(101L, "dcc:controlled-file:review")).thenReturn(true);
        when(permissionApi.hasAnyPermissions(102L, "dcc:controlled-file:review")).thenReturn(false);
        when(permissionApi.hasAnyPermissions(103L, "dcc:controlled-file:review")).thenReturn(true);
        when(permissionApi.hasAnyPermissions(104L, "dcc:controlled-file:review")).thenReturn(true);
        when(signatureAuthorizationService.getAuthorizationMap(List.of(101L, 102L, 103L, 104L)))
                .thenReturn(Map.of(101L, true, 102L, true, 103L, false, 104L, true));
        when(signatureImageService.requireActiveSnapshot(101L)).thenReturn(validImage());
        when(signatureImageService.requireActiveSnapshot(102L)).thenReturn(validImage());
        when(signatureImageService.requireActiveSnapshot(103L)).thenReturn(validImage());
        doThrow(new ServiceException(1_080_000_035, "signature image missing"))
                .when(signatureImageService).requireActiveSnapshot(104L);

        DccControlledFileRouteReadinessService.RouteReadinessEvaluation evaluation =
                service.evaluate(10L, 99L, List.of());

        assertFalse(evaluation.response().getReady());
        assertEquals(List.of(
                        "APPROVER_POST_MISSING",
                        "APPROVER_STAGE_PERMISSION_MISSING",
                        "APPROVER_SIGNATURE_NOT_AUTHORIZED",
                        "APPROVER_SIGNATURE_IMAGE_INVALID"),
                evaluation.response().getBlockers().stream().map(blocker -> blocker.getReasonCode()).toList());
        assertEquals(List.of(101L, 102L, 103L, 104L),
                evaluation.response().getBlockers().stream().map(blocker -> blocker.getUserId()).toList());
    }

    @Test
    void evaluate_submitterDepartmentLeaderMissingReturnsOrganizationBlocker() {
        doThrow(new ServiceException(APPROVAL_POSITION_UPLOADER_MAPPING_INVALID.getCode(),
                "Approval position runtime mapping failed: 编制人直接主管 requires a local department leader for the submitter"))
                .when(routeAssigneeResolver).resolveRouteForReadiness(10L, 99L);

        DccControlledFileRouteReadinessService.RouteReadinessEvaluation evaluation =
                service.evaluate(10L, 99L, List.of());

        assertFalse(evaluation.response().getReady());
        assertEquals(1, evaluation.response().getBlockers().size());
        assertEquals("SUBMITTER_ORG_MAPPING_INVALID", evaluation.response().getBlockers().get(0).getReasonCode());
        assertTrue(evaluation.response().getBlockers().get(0).getMessage().contains("部门负责人"));
    }

    private DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute resolvedRoute(
            List<DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode> nodes) {
        return new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute(
                DccCategoryApprovalRouteDO.builder().id(30L).categoryId(10L).versionNo(1).active(true).build(), nodes);
    }

    private AdminUserRespDTO user(Long id, String nickname, Set<Long> postIds) {
        return new AdminUserRespDTO().setId(id).setNickname(nickname).setPostIds(postIds);
    }

    private DccElectronicSignatureImageSnapshot validImage() {
        return DccElectronicSignatureImageSnapshot.builder()
                .imageId(501L)
                .fileId(1501L)
                .sha256("valid-sha256")
                .imageStatus("ENABLED")
                .verifiedStatus("VALID")
                .build();
    }
}
