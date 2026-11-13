package cn.iocoder.yudao.module.dcc.service.projectcode;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.productcreate.DccProjectProductCreateReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccFolderTemplateDO;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes;
import cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccFolderTemplateStructure;
import cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.*;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyOperationDO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@Import({DccProjectProductCreateServiceImpl.class, DccProjectProductCreateStateService.class,
        DccProjectProductCreateFailureService.class, DccProjectReviewerConfigurationService.class,
        DccProjectReviewerRoleService.class, DccProjectProductActorSupport.class})
class DccProjectDirectCreateTest extends DccApprovedProjectLeaderOwnerTest {
    @Resource DccProjectProductCreateService service;
    @MockitoSpyBean DccProjectReviewerRoleService reviewerRoles;
    @MockitoBean DccProjectProductNotificationService notifications;

    @BeforeEach void directCreatePolicy() {
        var policy = BeanUtils.toBean(policies.selectList().get(0), GxpAuditPolicyOperationDO.class);
        policy.setId(null); policy.setOperationId("dcc.project-product.create"); policies.insert(policy);
        when(permissions.hasAnyPermissions(9L, "dcc:project-code:query")).thenReturn(true);
        DccProjectProductRegressionFixture.login(9L);
    }

    DccProjectProductCreateReqVO input() {
        var template = new DccFolderTemplateDO(); template.setTenantId(1L); template.setName("直接创建模板");
        template.setActive(true); template.setEverUsed(false); template.setEditedByUserId(7L);
        template.setStructureJson(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(
                new DccFolderTemplateStructure(List.of(new DccFolderTemplateStructure.Node("root", null, "直接创建目录", 0)))));
        templates.insert(template);
        var req = new DccProjectProductCreateReqVO(); req.setProjectName("直接创建项目"); req.setProjectCode("DIRECT-01");
        req.setProjectLeaderUserId(7L); req.setFolderTemplateId(template.getId());
        req.setDefaultAttributes(new DccProjectAttributes(List.of("NMPA"), null, "Y", "N", "N", null));
        req.setProductCode("DIRECT-P01"); req.setProductName("直接创建产品"); req.setClassification("一类");
        req.setCreationReason("新建项目及产品，无需审核"); return req;
    }

    @Test void createsAllAssetsAndOwnerWithoutReviewerConfigurationOrApprovalFacts() {
        Long id = service.createRequest(9L, input()); var row = service.getRequest(9L, id);
        assertEquals("COMPLETED", row.getStatus()); assertEquals("DIRECT_CREATE_V1", row.getReviewContract());
        assertNull(row.getReviewerRoleId()); assertNull(row.getReviewerUserId()); assertNull(row.getApproverUserId());
        assertNull(row.getReviewedTime()); assertNull(row.getApprovedTime());
        assertNotNull(projects.selectById(row.getGeneratedProjectCodeId()));
        assertNotNull(products.selectById(row.getGeneratedProductCatalogId()));
        assertNotNull(relations.selectById(row.getRelationId()));
        assertEquals("直接创建目录", folders.listProjectFolders(row.getGeneratedProjectCodeId()).get(0).getName());
        assertEquals(1, accessRules.selectList().size());
        verify(reviewerRoles, never()).readiness(any()); verify(reviewerRoles, never()).freezeRole();
        verifyNoInteractions(notifications);
        assertTrue(events.selectList().stream().noneMatch(event -> event.getAfterStateJson().contains("APPROVE")));
    }

    @Test void duplicateIdentityCannotCreateASecondProjectOrProduct() {
        var req = input(); service.createRequest(9L, req);
        assertThrows(RuntimeException.class, () -> service.createRequest(9L, req));
        assertEquals(1, projects.selectList().size()); assertEquals(1, products.selectList().size());
        assertEquals(1, requests.selectList().size());
    }

    @Test void folderFailureRollsBackRequestAndEveryGeneratedAsset() {
        var req = input(); doThrow(new IllegalStateException("目录写入失败")).when(folderRows)
                .insert(any(cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectFolderDO.class));
        assertThrows(RuntimeException.class, () -> service.createRequest(9L, req));
        assertTrue(requests.selectList().isEmpty()); assertTrue(projects.selectList().isEmpty());
        assertTrue(products.selectList().isEmpty()); assertTrue(relations.selectList().isEmpty());
        assertTrue(accessRules.selectList().isEmpty()); assertTrue(events.selectList().isEmpty());
        assertFalse(templates.selectById(req.getFolderTemplateId()).getEverUsed());
    }

    @Test void cannotReviewApproveOrRetryAnAlreadyDirectlyCreatedRecord() {
        Long id = service.createRequest(9L, input());
        assertThrows(RuntimeException.class, () -> service.review(9L, id, "审核", true));
        assertThrows(RuntimeException.class, () -> service.approve(9L, id, "批准", true));
        assertThrows(RuntimeException.class, () -> service.retryWrite(9L, id, "重试"));
        assertEquals("COMPLETED", requests.selectById(id).getStatus());
    }
}
