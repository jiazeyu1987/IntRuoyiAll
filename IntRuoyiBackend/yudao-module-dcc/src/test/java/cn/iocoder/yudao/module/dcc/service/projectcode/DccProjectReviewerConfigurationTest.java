package cn.iocoder.yudao.module.dcc.service.projectcode;

import org.junit.jupiter.api.Test;
import jakarta.annotation.Resource;
import cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectReviewerConfigurationService;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class DccProjectReviewerConfigurationTest extends DccProjectProductDecisionPersistenceTest {
    @Resource DccProjectReviewerConfigurationService configuration;
    @Test void absentReviewerConfigurationRejectsBeforeCreatingApplicationOrIdentityClaims() {
        new org.springframework.jdbc.core.JdbcTemplate(reviewerDataSource).update("DELETE FROM dcc_project_reviewer_config");
        assertThrows(RuntimeException.class,this::pendingReview);
        assertTrue(requests.selectList().isEmpty());
        assertTrue(claims.selectList().isEmpty());
    }
    private void enableConfigEditor() {
        when(permissions.hasAnyPermissions(9L,"dcc:project-code:update")).thenReturn(true);
        when(permissions.hasAnyRoles(9L,"doc_control")).thenReturn(true);
        when(users.getUser(8L)).thenReturn(new AdminUserRespDTO().setId(8L).setTenantId(1L).setStatus(0).setUsername("reviewer8").setNickname("后续审核人"));
    }
    @Test void configurationChangesDoNotReplaceFrozenReviewerAndNewSubmissionUsesCurrentAccount() {
        enableConfigEditor();Long first=pendingReview();
        configuration.save(9L,new DccProjectReviewerConfigurationService.Save(8L,"更换后续新申请审核人"));
        assertEquals(1L,requests.selectById(first).getConfiguredReviewerUserId());
        assertEquals("admin",requests.selectById(first).getConfiguredReviewerUsername());
        assertThrows(RuntimeException.class,()->service.review(8L,first,"无权接管历史审核",true));
        assertEquals("PENDING_REVIEW",requests.selectById(first).getStatus());
        service.review(1L,first,"原指定人驳回并保留历史",false);
        Long next=pendingReview();assertEquals(8L,requests.selectById(next).getConfiguredReviewerUserId());
        assertEquals("后续审核人",requests.selectById(next).getConfiguredReviewerNickname());
        service.review(8L,next,"由实际冻结账号审核",true);
        assertEquals("PENDING_APPROVAL",requests.selectById(next).getStatus());
        assertThrows(RuntimeException.class,()->service.approve(8L,next,"批准规则仍独立",false));
        assertEquals(1L,requests.selectById(first).getReviewerUserId());
    }
    @Test void disabledAndForeignTenantAccountsCannotBeConfiguredOrSubmitAndFrozenDisabledAccountCannotReview() {
        enableConfigEditor();Long first=pendingReview();
        when(users.getUser(1L)).thenReturn(new AdminUserRespDTO().setId(1L).setTenantId(1L).setStatus(1).setUsername("admin").setNickname("停用"));
        assertFalse(configuration.get().enabled());
        assertThrows(RuntimeException.class,()->service.review(1L,first,"停用不得审核",true));
        assertThrows(RuntimeException.class,this::pendingReview);
        assertThrows(RuntimeException.class,()->configuration.save(9L,new DccProjectReviewerConfigurationService.Save(1L,"禁止停用人员")));
        when(users.getUser(8L)).thenReturn(new AdminUserRespDTO().setId(8L).setTenantId(2L).setStatus(0).setUsername("foreign").setNickname("其他租户"));
        assertThrows(RuntimeException.class,()->configuration.save(9L,new DccProjectReviewerConfigurationService.Save(8L,"禁止跨租户账号")));
        assertEquals(1,requests.selectList().size());assertEquals("PENDING_REVIEW",requests.selectById(first).getStatus());
    }
    @Test void configurationWriteRequiresPermissionAndAuditFailureRollsBackTheConfig() {
        enableConfigEditor();
        assertThrows(RuntimeException.class,()->configuration.save(7L,new DccProjectReviewerConfigurationService.Save(8L,"无维护授权")));
        doThrow(new IllegalStateException("审计不可用")).when(audit).append(org.mockito.ArgumentMatchers.any());
        assertThrows(RuntimeException.class,()->configuration.save(9L,new DccProjectReviewerConfigurationService.Save(8L,"真实审计失败")));
        assertEquals(1L,configuration.get().reviewerUserId());
    }
}
