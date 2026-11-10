package cn.iocoder.yudao.module.dcc.service.projectcode;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.productcreate.DccProjectProductCreateReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.folder.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributeErrors.WRITE_INCOMPLETE;

/** 当前基线人员逻辑下验证真实决策/事务，未指定待讨论的正式审核人。 */
@Import({DccProjectReviewerRoleService.class,DccProjectReviewerConfigurationService.class,DccProjectProductCreateServiceImpl.class,DccProjectProductCreateWriteService.class,
        cn.iocoder.yudao.module.dcc.service.productcatalog.DccProductCatalogRowNumberAllocator.class,
        DccW01ProjectProductJdbcFixture.class,
        DccProjectProductCreateStateService.class,DccProjectProductCreateFailureService.class,
        DccProjectAttributesService.class,DccProjectLeaderService.class,DccFolderTemplateService.class,
        DccProjectConfigurationAuditService.class,DccProjectProductAuditService.class})
@org.springframework.test.context.TestPropertySource(properties="spring.sql.init.encoding=UTF-8")
@org.springframework.test.context.jdbc.SqlConfig(encoding="UTF-8")
class DccProjectProductDecisionPersistenceTest extends BaseDbUnitTest {
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private cn.iocoder.yudao.module.dcc.registrationcertificate.service.handling.DccRegistrationHandlingTaskService registrationHandlingTasks;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectProductNotificationService projectNotifications;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectProductActorSupport projectActors;
    @Resource javax.sql.DataSource reviewerDataSource;
    @org.junit.jupiter.api.BeforeEach void configuredReviewerFixture() {
        new org.springframework.jdbc.core.JdbcTemplate(reviewerDataSource).update("INSERT INTO dcc_project_reviewer_config(tenant_id,reviewer_user_id,reviewer_username,reviewer_nickname,version_no,updated_by,change_reason) VALUES(1,1,'admin','显式配置测试审核人',1,1,'测试正式配置')");
    }

    @Resource DccProjectProductCreateServiceImpl service;
    @Resource DccProjectProductCreateStateService state;
    @Resource DccProjectProductCreateFailureService failures;
    @Resource DccFolderTemplateService templates;
    @MockitoSpyBean DccProjectProductCreateRequestMapper requests;
    @Resource DccProjectProductIdentityClaimMapper claims;
    @Resource DccProjectCodeMapper projects;
    @Resource DccProjectProductRelationMapper relations;
    @MockitoBean AdminUserApi users;
    @MockitoBean PermissionApi permissions;
    @MockitoBean DccProjectAccessService access;
    @MockitoBean GxpAuditService audit;
    @BeforeEach void accounts() {
        when(projectActors.isApprover(1L)).thenReturn(true); // Explicit external authority port for persistence cases.
        when(projectActors.canUpdate(1L)).thenReturn(true);
        var leader=new AdminUserRespDTO();leader.setId(7L);leader.setStatus(0);leader.setNickname("正式负责人");
        var existingReviewer=new AdminUserRespDTO();existingReviewer.setId(1L);existingReviewer.setStatus(0);existingReviewer.setUsername("admin");existingReviewer.setTenantId(1L);existingReviewer.setNickname("显式配置测试审核人");
        when(users.getUser(7L)).thenReturn(leader);when(users.getUser(1L)).thenReturn(existingReviewer);
        when(permissions.hasAnyPermissions(7L,"dcc:project-code:update")).thenReturn(true);
    }
    Long pendingReview() {
        Long template=templates.save(7L,new DccFolderTemplateService.Save(null,"决策模板",null,true,
                new DccFolderTemplateStructure(List.of(new DccFolderTemplateStructure.Node("root",null,"正式目录",0))),"测试新增"));
        // Historical requests retain the original reviewer/admin contract; new requests use role review.
        var row=DccProjectProductCreateRequestDO.builder().projectName("决策项目").projectCode("DECISION-B")
                .projectLeader("正式负责人").projectLeaderUserId(7L).folderTemplateId(template)
                .folderTemplateSnapshotJson(templates.captureForRequest(template))
                .defaultAttributesJson(JsonUtils.toJsonString(new DccProjectAttributes(List.of("CE"),null,"Y","N","N",null)))
                .productCode("PR-DECISION-B").productName("决策产品").classification("一类").creationReason("历史正式申请原因")
                .status("PENDING_REVIEW").applicantUserId(9L).configuredReviewerUserId(1L)
                .configuredReviewerUsername("admin").configuredReviewerNickname("显式配置测试审核人")
                .submittedTime(java.time.LocalDateTime.now()).build();
        row.setTenantId(1L);requests.insert(row);
        for(var entry:java.util.Map.of("PROJECT_CODE","DECISION-B","PRODUCT_CODE","PR-DECISION-B","PRODUCT_NAME","决策产品").entrySet()){
            var claim=DccProjectProductIdentityClaimDO.builder().requestId(row.getId()).identityType(entry.getKey()).identityValue(entry.getValue()).build();claim.setTenantId(1L);claims.insert(claim);
        }
        return row.getId();
    }
    @ParameterizedTest
    @org.junit.jupiter.params.provider.NullAndEmptySource
    @ValueSource(strings={"   ","\t\n","\u00A0\uFEFF\u3000"})
    void blankApproveOpinionPersistsActualReviewApprovalAndCompletedAssets(String reason) {
        Long id=pendingReview();
        var reviewed=service.review(1L,id,reason,true);
        assertEquals("PENDING_APPROVAL",reviewed.getStatus());assertEquals("",reviewed.getReviewReason());
        var completed=service.approve(1L,id,reason,true);
        assertEquals("COMPLETED",completed.getStatus());assertEquals("",completed.getApprovalReason());
        assertNotNull(completed.getReviewerUserId());assertNotNull(completed.getApproverUserId());
        assertEquals(1,projects.selectList().size());assertEquals(1,relations.selectList().size());
        verify(access).initializeApprovedProjectLeaderOwner(completed.getGeneratedProjectCodeId(),7L,
                "项目申请审批通过（申请ID：" + id + "），初始化项目负责人OWNER权限");
    }
    @ParameterizedTest
    @org.junit.jupiter.params.provider.NullAndEmptySource
    @ValueSource(strings={"   ","\u00A0\uFEFF\u3000"})
    void blankRejectOpinionCannotSaveDecisionAtEitherNode(String reason) {
        Long id=pendingReview();
        assertThrows(ServiceException.class,()->service.review(1L,id,reason,false));
        assertEquals("PENDING_REVIEW",requests.selectById(id).getStatus());
        service.review(1L,id,null,true);
        assertThrows(ServiceException.class,()->service.approve(1L,id,reason,false));
        assertEquals("PENDING_APPROVAL",requests.selectById(id).getStatus());assertTrue(projects.selectList().isEmpty());
    }
    @Test void failureOfBlankOpinionApprovedWriteRetainsActualErrorAndAllowsExplicitRetry() {
        Long id=pendingReview();service.review(1L,id,null,true);state.markApprovalDecision(1L,id,null,true);
        failures.markWriteFailed(id,"ACTUAL_ERROR","实际目录写入失败");
        var failed=requests.selectById(id);assertEquals("WRITE_FAILED",failed.getStatus());
        assertEquals("ACTUAL_ERROR",failed.getWriteErrorCode());assertEquals("实际目录写入失败",failed.getWriteErrorMessage());
        assertEquals("",failed.getWriteReason());
        assertThrows(ServiceException.class,()->service.retryWrite(1L,id,null));
        assertEquals("COMPLETED",service.retryWrite(1L,id,"修复目录后明确重试").getStatus());
    }
    @ParameterizedTest @ValueSource(booleans={true,false})
    void zeroReviewUpdatePreservesOriginalDecisionAndIdentityProtection(boolean approve) {
        Long id=pendingReview();String before=JsonUtils.toJsonString(requests.selectById(id));
        assertEquals(3,claims.selectList().size());
        doReturn(0).when(requests).updateById(any(DccProjectProductCreateRequestDO.class));
        var failure=assertThrows(ServiceException.class,()->service.review(1L,id,"共同基线审核测试",approve));
        assertEquals(WRITE_INCOMPLETE.getCode(),failure.getCode());
        assertEquals(before,JsonUtils.toJsonString(requests.selectById(id)));assertEquals(3,claims.selectList().size());
    }
    @ParameterizedTest @ValueSource(booleans={true,false})
    void zeroApprovalUpdateCannotAdvanceOrDeleteIdentityProtection(boolean approve) {
        Long id=pendingReview();service.review(1L,id,"共同基线审核测试",true);
        String before=JsonUtils.toJsonString(requests.selectById(id));
        doReturn(0).when(requests).updateById(any(DccProjectProductCreateRequestDO.class));
        var failure=assertThrows(ServiceException.class,()->service.approve(1L,id,"共同基线批准测试",approve));
        assertEquals(WRITE_INCOMPLETE.getCode(),failure.getCode());
        assertEquals(before,JsonUtils.toJsonString(requests.selectById(id)));assertEquals(3,claims.selectList().size());
        assertTrue(projects.selectList().isEmpty());assertTrue(relations.selectList().isEmpty());
    }
    @Test void anotherTenantRequestCannotBeApproved() {
        Long id=pendingReview();service.review(1L,id,"共同基线审核测试",true);
        var foreign=requests.selectById(id);foreign.setTenantId(2L);requests.updateById(foreign);
        String before=JsonUtils.toJsonString(requests.selectById(id));
        assertThrows(ServiceException.class,()->state.markApprovalDecision(1L,id,"错租户批准测试",true));
        assertEquals(before,JsonUtils.toJsonString(requests.selectById(id)));assertEquals(3,claims.selectList().size());
    }
    @Test void failureRecorderCannotChangeAnotherTenantWritingRequest() {
        Long id=pendingReview();service.review(1L,id,"共同基线审核测试",true);
        state.markApprovalDecision(1L,id,"共同基线批准测试",true);
        var foreign=requests.selectById(id);foreign.setTenantId(2L);requests.updateById(foreign);
        String before=JsonUtils.toJsonString(requests.selectById(id));
        assertThrows(ServiceException.class,()->failures.markWriteFailed(id,"TEST_ERROR","错租户失败登记"));
        assertEquals(before,JsonUtils.toJsonString(requests.selectById(id)));
    }
    @Test void failureRecorderCannotReplaceCompletedHistory() {
        Long id=pendingReview();service.review(1L,id,"共同基线审核测试",true);service.approve(1L,id,"共同基线批准测试",true);
        String before=JsonUtils.toJsonString(requests.selectById(id));
        assertThrows(RuntimeException.class,()->failures.markWriteFailed(id,"STALE_ERROR","过期失败事件"));
        assertEquals(before,JsonUtils.toJsonString(requests.selectById(id)));
    }
    @Test void failureRecorderRejectsZeroWrite() {
        Long id=pendingReview();service.review(1L,id,"共同基线审核测试",true);
        state.markApprovalDecision(1L,id,"共同基线批准测试",true);
        doReturn(0).when(requests).updateById(any(DccProjectProductCreateRequestDO.class));
        var failure=assertThrows(ServiceException.class,()->failures.markWriteFailed(id,"TEST_ERROR","失败写入测试"));
        assertEquals(WRITE_INCOMPLETE.getCode(),failure.getCode());
        assertEquals("WRITING",requests.selectById(id).getStatus());assertNull(requests.selectById(id).getWriteErrorCode());
    }
    @Test void recordedFailureKeepsItsActualEvidenceWhenRetryStateWriteFails() {
        Long id=pendingReview();service.review(1L,id,"共同基线审核测试",true);
        state.markApprovalDecision(1L,id,"共同基线批准测试",true);
        failures.markWriteFailed(id,"ACTUAL_WRITE_ERROR","实际目录写入失败");
        var failed=requests.selectById(id);
        assertEquals("WRITE_FAILED",failed.getStatus());assertEquals("ACTUAL_WRITE_ERROR",failed.getWriteErrorCode());
        assertEquals("实际目录写入失败",failed.getWriteErrorMessage());assertNotNull(failed.getFailedTime());
        String before=JsonUtils.toJsonString(failed);
        doReturn(0).when(requests).updateById(any(DccProjectProductCreateRequestDO.class));
        var error=assertThrows(ServiceException.class,()->state.markRetryWriting(1L,id,"本次明确重试原因"));
        assertEquals(WRITE_INCOMPLETE.getCode(),error.getCode());
        assertEquals(before,JsonUtils.toJsonString(requests.selectById(id)));
    }
}
