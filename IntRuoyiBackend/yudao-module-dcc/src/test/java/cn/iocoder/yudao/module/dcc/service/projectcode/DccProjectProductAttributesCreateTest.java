package cn.iocoder.yudao.module.dcc.service.projectcode;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.productcreate.DccProjectProductCreateReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.productcatalog.DccProductCatalogMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.folder.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Import({DccProjectReviewerConfigurationService.class,DccProjectProductCreateServiceImpl.class, DccProjectProductCreateWriteService.class,
        DccProjectAttributesService.class, DccProjectLeaderService.class, DccFolderTemplateService.class, DccProjectConfigurationAuditService.class,DccProjectProductAuditService.class})
class DccProjectProductAttributesCreateTest extends BaseDbUnitTest {
    @Resource javax.sql.DataSource reviewerDataSource;
    @org.junit.jupiter.api.BeforeEach void configuredReviewerFixture() {
        new org.springframework.jdbc.core.JdbcTemplate(reviewerDataSource).update("INSERT INTO dcc_project_reviewer_config(tenant_id,reviewer_user_id,reviewer_username,reviewer_nickname,version_no,updated_by,change_reason) VALUES(1,1,'admin','显式配置测试审核人',1,1,'测试正式配置')");
    }

    @Resource private DccProjectProductCreateServiceImpl service;
    @Resource private DccProjectProductCreateWriteService writer;
    @Resource private DccProjectAttributesService attributes;
    @Resource private DccFolderTemplateService folders;
    @MockitoSpyBean private DccProjectProductCreateRequestMapper requests;
    @Resource private DccProjectCodeMapper projects;
    @Resource private DccProductCatalogMapper products;
    @MockitoSpyBean private DccProjectProductRelationMapper relations;
    @MockitoSpyBean private DccProjectProductIdentityClaimMapper claims;
    @MockitoSpyBean private DccFolderTemplateMapper templateMapper;
    @MockitoSpyBean private DccProjectFolderMapper folderMapper;
    @MockitoBean private AdminUserApi users;
    @MockitoBean private PermissionApi permissions;
    @MockitoBean private cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService audit;
    @MockitoBean private DccProjectAccessService access;
    @MockitoBean private DccProjectProductCreateFailureService failure;
    @MockitoBean private DccProjectProductCreateStateService state;
    private DccProjectAttributes defaults() { return new DccProjectAttributes(List.of("NMPA", "CE"), null, "Y", "N", "Y", "甲方"); }
    @BeforeEach void setupAccounts() {
        var user = new AdminUserRespDTO(); user.setId(7L); user.setStatus(0); user.setNickname("正式负责人");
        when(users.getUser(7L)).thenReturn(user);
        var reviewer = new AdminUserRespDTO(); reviewer.setId(1L); reviewer.setStatus(0); reviewer.setUsername("admin"); reviewer.setTenantId(1L);reviewer.setNickname("显式配置测试审核人");
        when(users.getUser(1L)).thenReturn(reviewer);
        when(permissions.hasAnyPermissions(7L, "dcc:project-code:update")).thenReturn(true);
    }
    private DccProjectProductCreateReqVO request() {
        var template = new DccFolderTemplateStructure(List.of(new DccFolderTemplateStructure.Node("root", null, "申请时目录", 0)));
        Long templateId = folders.save(7L, new DccFolderTemplateService.Save(null, "模板", null, true, template, "创建测试模板"));
        var input = new DccProjectProductCreateReqVO();
        input.setProjectName("项目B"); input.setProjectCode("B"); input.setProjectLeader("伪造文本");
        input.setProjectLeaderUserId(7L); input.setDefaultAttributes(defaults()); input.setFolderTemplateId(templateId);
        input.setResubmissionReason("按驳回意见补齐后重提"); input.setProductCode("PR-B"); input.setProductName("产品B"); input.setClassification("一类");input.setCreationReason("正式新建申请原因");
        return input;
    }
    @Test void requestAndApprovedProjectPersistAccountDefaultsAndFrozenFolderSnapshot() {
        var input = request();
        Long id = service.createRequest(9L, input);
        var saved = requests.selectById(id);
        assertEquals("PENDING_REVIEW", saved.getStatus());
        assertEquals(7L, saved.getProjectLeaderUserId());
        assertEquals("正式负责人", saved.getProjectLeader());
        assertEquals(defaults(), attributes.readValue(saved.getDefaultAttributesJson()));
        var response = cn.iocoder.yudao.framework.common.util.object.BeanUtils.toBean(saved,
                cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.productcreate.DccProjectProductCreateRespVO.class);
        assertEquals(7L, response.getProjectLeaderUserId());
        assertEquals(input.getFolderTemplateId(), response.getFolderTemplateId());
        assertEquals(saved.getDefaultAttributesJson(), response.getDefaultAttributesJson());
        folders.save(7L, new DccFolderTemplateService.Save(input.getFolderTemplateId(), "模板", null, true,
                new DccFolderTemplateStructure(List.of(new DccFolderTemplateStructure.Node("root", null, "后续模板目录", 0))), "修改测试模板"));
        saved.setStatus("WRITING"); saved.setWriteAttemptNo(1);saved.setWriteReason("批准资产写入原因");saved.setWriteOperatorUserId(1L); requests.updateById(saved);
        var completed = writer.writeApprovedRequest(id); // 仅验证既有批准后的写入事务，不模拟审批或选审核人
        assertEquals("COMPLETED", completed.getStatus());
        var project = projects.selectById(completed.getGeneratedProjectCodeId());
        assertEquals(7L, project.getProjectLeaderUserId());
        assertEquals(defaults(), attributes.defaults(project));
        assertEquals("申请时目录", folders.listProjectFolders(project.getId()).get(0).getName());
        assertNotNull(products.selectById(completed.getGeneratedProductCatalogId()));
        assertEquals(1, relations.selectList().size());
    }
    @Test void directoryFailureRollsBackProjectProductAndRelationCreation() {
        Long id = service.createRequest(9L, request());
        var saved = requests.selectById(id); saved.setStatus("WRITING"); saved.setWriteAttemptNo(1);saved.setWriteReason("批准资产写入原因");saved.setWriteOperatorUserId(1L); requests.updateById(saved);
        doThrow(new IllegalStateException("目录写入失败")).when(folderMapper).insert(any(DccProjectFolderDO.class));
        assertThrows(RuntimeException.class, () -> writer.writeApprovedRequest(id));
        assertNull(projects.selectByNormalizedProjectCode("B"));
        assertNull(products.selectByNormalizedProductCode("PR-B"));
        assertTrue(relations.selectList().isEmpty());
        assertEquals("WRITING", requests.selectById(id).getStatus());
    }
    @Test void zeroDirectoryInsertCannotCompleteApprovedProjectWithoutItsDirectory() {
        Long id = service.createRequest(9L, request());
        var saved = requests.selectById(id); saved.setStatus("WRITING"); saved.setWriteAttemptNo(1);saved.setWriteReason("批准资产写入原因");saved.setWriteOperatorUserId(1L); requests.updateById(saved);
        doReturn(0).when(folderMapper).insert(any(DccProjectFolderDO.class));
        assertThrows(RuntimeException.class, () -> writer.writeApprovedRequest(id));
        assertNull(projects.selectByNormalizedProjectCode("B"));
        assertNull(products.selectByNormalizedProductCode("PR-B"));
        assertTrue(folderMapper.selectList().isEmpty());
        assertTrue(relations.selectList().isEmpty());
        assertEquals("WRITING", requests.selectById(id).getStatus());
        assertNull(requests.selectById(id).getGeneratedProjectCodeId());
    }
    @Test void zeroTemplateUsedUpdateCannotPersistRequestWithDeletableSourceTemplate() {
        var input = request();
        doReturn(0).when(templateMapper).updateById(any(DccFolderTemplateDO.class));
        assertThrows(RuntimeException.class, () -> service.createRequest(9L, input));
        assertTrue(requests.selectList().isEmpty());
        assertTrue(claims.selectList().isEmpty());
        assertFalse(templateMapper.selectById(input.getFolderTemplateId()).getEverUsed());
    }
    @Test void zeroCompletionUpdateRollsBackAllGeneratedAssetsAndPreservesOriginalRequest() {
        Long id = service.createRequest(9L, request());
        var saved = requests.selectById(id); saved.setStatus("WRITING"); saved.setWriteAttemptNo(1);saved.setWriteReason("批准资产写入原因");saved.setWriteOperatorUserId(1L); requests.updateById(saved);
        doReturn(0).when(requests).updateById(any(DccProjectProductCreateRequestDO.class));
        assertThrows(RuntimeException.class, () -> writer.writeApprovedRequest(id));
        assertNull(projects.selectByNormalizedProjectCode("B"));
        assertNull(products.selectByNormalizedProductCode("PR-B"));
        assertTrue(folderMapper.selectList().isEmpty());
        assertTrue(relations.selectList().isEmpty());
        var persisted = requests.selectById(id);
        assertEquals("WRITING", persisted.getStatus());
        assertNull(persisted.getGeneratedProjectCodeId());
        assertNull(persisted.getGeneratedProductCatalogId());
        assertNull(persisted.getCompletedTime());
    }
    @Test void zeroRelationInsertCannotLeaveCompletedAssetsWithoutTheirFormalRelationship() {
        Long id = service.createRequest(9L, request());
        var saved = requests.selectById(id); saved.setStatus("WRITING"); saved.setWriteAttemptNo(1);saved.setWriteReason("批准资产写入原因");saved.setWriteOperatorUserId(1L); requests.updateById(saved);
        doReturn(0).when(relations).insert(any(DccProjectProductRelationDO.class));
        assertThrows(RuntimeException.class, () -> writer.writeApprovedRequest(id));
        assertNull(projects.selectByNormalizedProjectCode("B"));
        assertNull(products.selectByNormalizedProductCode("PR-B"));
        assertTrue(folderMapper.selectList().isEmpty());
        assertTrue(relations.selectList().isEmpty());
        assertEquals("WRITING", requests.selectById(id).getStatus());
        assertNull(requests.selectById(id).getRelationId());
    }
    @Test void zeroIdentityClaimInsertCannotLeaveAnAcceptedUnprotectedApplication() {
        var input = request();
        doReturn(0).when(claims).insert(any(DccProjectProductIdentityClaimDO.class));
        assertThrows(RuntimeException.class, () -> service.createRequest(9L, input));
        assertTrue(requests.selectList().isEmpty());
        assertTrue(claims.selectList().isEmpty());
        assertFalse(templateMapper.selectById(input.getFolderTemplateId()).getEverUsed());
    }
    @Test void incompleteDefaultsAndInvalidLeaderRejectBeforeRequestWrites() {
        var input = request(); input.setDefaultAttributes(null);
        assertThrows(RuntimeException.class, () -> service.createRequest(9L, input));
        input.setDefaultAttributes(defaults()); input.setProjectLeaderUserId(999L);
        assertThrows(RuntimeException.class, () -> service.createRequest(9L, input));
        assertTrue(requests.selectList().isEmpty());
    }
    private Long rejected(DccProjectProductCreateReqVO input) {
        Long id = service.createRequest(9L, input);
        service.review(1L, id, "共同基线驳回测试", false);
        return id;
    }
    private Long resubmit(Long applicant, Long rejectedId, DccProjectProductCreateReqVO input) throws Exception {
        return service.resubmitRejectedRequest(applicant, rejectedId, input);
    }
    @Test void rejectedRequestResubmitsAsNewPendingReviewWithOriginalHistoryFrozen() throws Exception {
        var input = request(); Long originalId = rejected(input);
        var original = requests.selectById(originalId);
        folders.save(7L, new DccFolderTemplateService.Save(input.getFolderTemplateId(), "模板", null, true,
                new DccFolderTemplateStructure(List.of(new DccFolderTemplateStructure.Node("root", null, "重提时目录", 0))), "更新模板"));
        input.setDefaultAttributes(new DccProjectAttributes(List.of("FDA"), null, "N", "Y", "Y", "重提目标"));
        input.setRemark("按驳回意见修改");
        Long nextId = resubmit(9L, originalId, input);
        assertNotEquals(originalId, nextId);
        var next = requests.selectById(nextId);
        assertEquals("PENDING_REVIEW", next.getStatus());
        assertEquals(originalId, next.getPreviousRequestId()); assertEquals(9L, next.getApplicantUserId());
        assertEquals(input.getDefaultAttributes(), attributes.readValue(next.getDefaultAttributesJson()));
        assertTrue(next.getFolderTemplateSnapshotJson().contains("重提时目录"));
        assertEquals(original.getDefaultAttributesJson(), requests.selectById(originalId).getDefaultAttributesJson());
        assertEquals(original.getFolderTemplateSnapshotJson(), requests.selectById(originalId).getFolderTemplateSnapshotJson());
        assertEquals("共同基线驳回测试", requests.selectById(originalId).getRejectReason());
        assertEquals(2, requests.selectList().size()); assertNull(projects.selectByNormalizedProjectCode("B"));
    }
    @Test void resubmitRejectsDifferentApplicantAdminWrongTenantAndNonRejectedState() throws Exception {
        var input = request(); Long originalId = rejected(input);
        for (Long applicant : new Long[]{1L, 8L, null}) {
            assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class, () -> service.resubmitRejectedRequest(applicant, originalId, input));
        }
        var original = requests.selectById(originalId); original.setTenantId(2L); requests.updateById(original);
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class, () -> service.resubmitRejectedRequest(9L, originalId, input));
        original.setTenantId(1L); original.setStatus("PENDING_APPROVAL"); requests.updateById(original);
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class, () -> service.resubmitRejectedRequest(9L, originalId, input));
        assertEquals(1, requests.selectList().size());
    }
    @Test void resubmittedHistoryCannotForkAgainAndFailedReplacementLeavesNoPartialRequest() throws Exception {
        var input = request(); Long originalId = rejected(input);
        input.setDefaultAttributes(null);
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class, () -> service.resubmitRejectedRequest(9L, originalId, input));
        assertEquals(1, requests.selectList().size());
        input.setDefaultAttributes(defaults()); Long nextId = resubmit(9L, originalId, input);
        input.setProjectCode("DIFFERENT-B"); input.setProductCode("DIFFERENT-PR"); input.setProductName("不同产品");
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class, () -> service.resubmitRejectedRequest(9L, originalId, input));
        assertEquals(2, requests.selectList().size());
        assertEquals(originalId, requests.selectById(nextId).getPreviousRequestId());
        assertEquals(nextId, service.getPendingRequests().stream().filter(row -> row.getId().equals(originalId))
                .findFirst().orElseThrow().getResubmittedRequestId());
    }
    @Test void auditFailureRollsBackRejectedResubmissionAndDoesNotForkOriginalHistory() {
        var input = request(); Long originalId = rejected(input);
        doThrow(new IllegalStateException("重提审计失败")).when(audit).append(any());
        assertThrows(RuntimeException.class, () -> service.resubmitRejectedRequest(9L, originalId, input));
        assertEquals(1, requests.selectList().size());
        assertEquals("REJECTED", requests.selectById(originalId).getStatus());
        assertNull(service.getPendingRequests().get(0).getResubmittedRequestId());
    }
    @Test void requestReadCannotExposeOtherTenantRejectedApplication() {
        Long id = rejected(request());
        var other = requests.selectById(id); other.setTenantId(2L); requests.updateById(other);
        assertTrue(service.getPendingRequests().isEmpty(), "当前租户不可读取其他租户原申请和后继身份");
    }
}
