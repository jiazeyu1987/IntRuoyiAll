package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.module.dcc.controller.admin.distribution.DccDistributionTaskController;
import cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo.DccAutomaticDistributionRetryReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.*;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.*;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class DccDistributionEditEntryPermissionTest {
    @AfterEach void clear() { TenantContextHolder.clear(); SecurityContextHolder.clearContext(); }

    @Test void allDisplayedDistributionEntriesAcceptEditOrDistributeButRejectQueryOnly() throws Exception {
        for (var method : List.of(
                DccDistributionTaskController.class.getMethod("getAutomaticDistributionResult", Long.class),
                DccDistributionTaskController.class.getMethod("getRecipientReceipts", Long.class),
                DccDistributionTaskController.class.getMethod("retryAutomaticDistribution", DccAutomaticDistributionRetryReqVO.class))) {
            var expression = new SpelExpressionParser().parseExpression(method.getAnnotation(PreAuthorize.class).value());
            for (String permission : List.of("dcc:controlled-file:edit", "dcc:controlled-file:distribute", "dcc:controlled-file:query")) {
                var context = new StandardEvaluationContext();
                context.setBeanResolver((ignored, name) -> new Permissions(permission));
                assertEquals(!permission.endsWith(":query"), expression.getValue(context, Boolean.class), method.getName());
            }
        }
    }

    @Test void actualLibraryEditCanReadAndRetryWhileViewOtherActorAndTenantCannot() {
        TenantContextHolder.setTenantId(1L);
        var projects = mock(DccProjectCodeMapper.class);
        var access = new DccLibraryUploadPermissionFixture(projects, 31L, 99L, "QMS", "EDIT");
        var rules = (DccProjectAccessRuleMapper) ReflectionTestUtils.getField(access.access, "accessRuleMapper");
        when(rules.selectActiveReaderRules(eq(1L), any(LocalDateTime.class))).thenReturn(List.of(access.rule));
        var files = mock(DccControlledFileMapper.class);
        var batches = mock(DccDistributionBatchMapper.class);
        var dispatch = mock(DccDistributionBatchService.class);
        var users = mock(AdminUserApi.class);
        when(users.getUser(99L)).thenReturn(new AdminUserRespDTO().setId(99L).setTenantId(1L).setStatus(0));
        var file = DccControlledFileDO.builder().id(42L).dccProjectCodeId(31L).categoryId(15L).versionNo("A/1")
                .processInstanceId("training-round").status("ACTIVE").controlledTime(LocalDateTime.now()).activatedTime(LocalDateTime.now()).build();
        file.setTenantId(1L);
        var batch = DccDistributionBatchDO.builder().id(51L).tenantId(1L).controlledFileId(42L).versionNo("A/1")
                .approvalProcessInstanceId("training-round").status("FAILED").build();
        when(files.selectById(42L)).thenReturn(file);
        when(batches.selectVersion(1L,42L,"training-round")).thenReturn(batch);
        when(batches.selectById(51L)).thenReturn(batch);
        var service = new DccDistributionTaskServiceImpl();
        DccLibraryUploadPermissionFixture.wire(service,"controlledFileMapper",files,"batchMapper",batches,
                "batchService",dispatch,"adminUserApi",users,"permissionSupport",access.categories,"projectAccessService",access.access);
        var request = new DccAutomaticDistributionRetryReqVO();
        request.setControlledFileId(42L);request.setBatchId(51L);request.setVersionNo("A/1");request.setApprovalProcessInstanceId("training-round");
        var login = new LoginUser().setId(99L).setTenantId(1L).setUserType(2);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login,null));
        for (String scope : List.of("QMS","GENERAL")) {
            access.project.setLibraryScope(scope);
            assertEquals("FAILED",service.getAutomaticDistributionResult(99L,42L).getStatus());
            assertTrue(service.getDistributionRecipientReceipts(99L,42L).isEmpty());
            assertEquals("FAILED",service.retryAutomaticDistribution(99L,request).getStatus());
            access.rule.setAccessLevel("VIEW");
            assertThrows(IllegalArgumentException.class,()->service.getAutomaticDistributionResult(99L,42L));
            assertThrows(IllegalArgumentException.class,()->service.getDistributionRecipientReceipts(99L,42L));
            assertThrows(IllegalArgumentException.class,()->service.retryAutomaticDistribution(99L,request));
            access.rule.setAccessLevel("EDIT");
        }
        verify(dispatch,times(2)).retryAutomaticBatch(51L,99L);
        assertThrows(IllegalArgumentException.class,()->service.getAutomaticDistributionResult(100L,42L));
        file.setTenantId(2L);
        assertThrows(IllegalArgumentException.class,()->service.getDistributionRecipientReceipts(99L,42L));
        assertThrows(IllegalArgumentException.class,()->service.retryAutomaticDistribution(99L,request));
        verifyNoMoreInteractions(dispatch);
    }

    public static class Permissions {
        private final String permission;
        Permissions(String permission) { this.permission=permission; }
        public boolean hasAnyPermissions(String... requested) { return Arrays.asList(requested).contains(permission); }
    }
}
