package cn.iocoder.yudao.module.dcc.service.projectcode;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.productcreate.DccProjectProductCreateReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectFolderDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.productcatalog.DccProductCatalogMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.folder.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 验证共同基线既有人员逻辑后的真实失败/重试，不把admin认定为待定需求最终人员。 */
@Import({DccProjectReviewerConfigurationService.class,DccProjectProductCreateServiceImpl.class, DccProjectProductCreateWriteService.class,
        DccProjectProductCreateStateService.class, DccProjectProductCreateFailureService.class,
        DccProjectAttributesService.class, DccProjectLeaderService.class, DccFolderTemplateService.class,
        DccProjectConfigurationAuditService.class,DccProjectProductAuditService.class})
class DccProjectProductWriteRetryTest extends BaseDbUnitTest {
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectProductNotificationService projectNotifications;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectProductActorSupport projectActors;
    @Resource javax.sql.DataSource reviewerDataSource;
    @org.junit.jupiter.api.BeforeEach void configuredReviewerFixture() {
        new org.springframework.jdbc.core.JdbcTemplate(reviewerDataSource).update("INSERT INTO dcc_project_reviewer_config(tenant_id,reviewer_user_id,reviewer_username,reviewer_nickname,version_no,updated_by,change_reason) VALUES(1,1,'admin','显式配置测试审核人',1,1,'测试正式配置')");
    }

    @Resource private DccProjectProductCreateServiceImpl service;
    @Resource private DccFolderTemplateService templates;
    @Resource private DccProjectProductCreateRequestMapper requests;
    @Resource private DccProjectCodeMapper projects;
    @Resource private DccProductCatalogMapper products;
    @Resource private DccProjectProductRelationMapper relations;
    @MockitoSpyBean private DccProjectFolderMapper folders;
    @MockitoBean private AdminUserApi users;
    @MockitoBean private PermissionApi permissions;
    @MockitoBean private DccProjectAccessService access;
    @MockitoBean private GxpAuditService audit;
    @Test void writeFailureCanRetryWithoutPartialAssetsOrStaleActiveError() {
        var leader = new AdminUserRespDTO(); leader.setId(7L); leader.setStatus(0); leader.setNickname("正式负责人");
        var existingReviewer = new AdminUserRespDTO(); existingReviewer.setId(1L); existingReviewer.setStatus(0); existingReviewer.setUsername("admin");existingReviewer.setTenantId(1L);existingReviewer.setNickname("显式配置测试审核人");
        when(users.getUser(7L)).thenReturn(leader); when(users.getUser(1L)).thenReturn(existingReviewer);
        when(permissions.hasAnyPermissions(7L, "dcc:project-code:update")).thenReturn(true);
        Long templateId = templates.save(7L, new DccFolderTemplateService.Save(null, "重试模板", null, true,
                new DccFolderTemplateStructure(List.of(new DccFolderTemplateStructure.Node("root", null, "正式目录", 0))), "测试新增"));
        var input = new DccProjectProductCreateReqVO();
        input.setProjectName("重试项目"); input.setProjectCode("RETRY-B"); input.setProjectLeaderUserId(7L);
        input.setFolderTemplateId(templateId); input.setDefaultAttributes(new DccProjectAttributes(List.of("CE"), null, "Y", "N", "N", null));
        input.setProductCode("PR-RETRY-B"); input.setProductName("重试产品"); input.setClassification("一类");input.setCreationReason("正式新建申请原因");
        Long requestId = service.createRequest(9L, input);
        service.review(1L, requestId, "共同基线审核测试", true);
        doThrow(new IllegalStateException("真实目录写入失败")).when(folders).insert(any(DccProjectFolderDO.class));
        assertThrows(RuntimeException.class, () -> service.approve(1L, requestId, "共同基线批准测试", true));
        var failed = requests.selectById(requestId);
        assertEquals("WRITE_FAILED", failed.getStatus());
        assertNotNull(failed.getWriteErrorCode()); assertTrue(failed.getWriteErrorMessage().contains("目录写入失败"));
        assertNull(projects.selectByNormalizedProjectCode("RETRY-B"));
        assertNull(products.selectByNormalizedProductCode("PR-RETRY-B")); assertTrue(relations.selectList().isEmpty());
        reset(folders);
        service.retryWrite(1L, requestId,"修复目录后重试");
        var completed = requests.selectById(requestId);
        assertEquals("COMPLETED", completed.getStatus());
        assertNull(completed.getWriteErrorCode(), "成功重试必须真正清空当前错误状态");
        assertNull(completed.getWriteErrorMessage());
        assertNotNull(completed.getCompletedTime());
        assertNotNull(completed.getGeneratedProjectCodeId());
        assertEquals(1, folders.listByProject(completed.getGeneratedProjectCodeId()).size());
    }
}
