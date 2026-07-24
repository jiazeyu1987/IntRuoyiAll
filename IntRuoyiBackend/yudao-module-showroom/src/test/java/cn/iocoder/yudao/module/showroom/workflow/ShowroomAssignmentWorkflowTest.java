package cn.iocoder.yudao.module.showroom.workflow;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductRevision;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomPersistentContentService;
import cn.iocoder.yudao.module.showroom.dal.dataobject.workflow.ShowroomFieldAssignmentDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.workflow.ShowroomFieldAssignmentMapper;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomAssignmentCreate;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomAssignmentSubmitResult;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomFieldAssignment;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomAssignmentService;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomApprovalActorResolver;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomPersistentWorkflowService;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApiImpl;
import cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyMessageDO;
import cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyTemplateDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.UserRoleDO;
import cn.iocoder.yudao.module.system.dal.mysql.dept.DeptMapper;
import cn.iocoder.yudao.module.system.dal.mysql.notify.NotifyMessageMapper;
import cn.iocoder.yudao.module.system.dal.mysql.notify.NotifyTemplateMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.UserRoleMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.service.notify.NotifyMessageServiceImpl;
import cn.iocoder.yudao.module.system.service.notify.NotifySendServiceImpl;
import cn.iocoder.yudao.module.system.service.notify.NotifyTemplateServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.enums.CommonStatusEnum.ENABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import({
        ShowroomPersistentContentService.class,
        ShowroomPersistentWorkflowService.class,
        ShowroomAssignmentService.class,
        ShowroomApprovalActorResolver.class,
        NotifyMessageSendApiImpl.class,
        NotifySendServiceImpl.class,
        NotifyMessageServiceImpl.class,
        NotifyTemplateServiceImpl.class
})
class ShowroomAssignmentWorkflowTest extends BaseDbUnitTest {

    private static final String ASSIGNMENT_TEMPLATE_CODE = "SHOWROOM_ASSIGNMENT";

    @Resource
    private ShowroomPersistentContentService contentService;
    @Resource
    private ShowroomAssignmentService assignmentService;
    @Resource
    private ShowroomPersistentWorkflowService workflowService;
    @Resource
    private ShowroomFieldAssignmentMapper assignmentMapper;
    @Resource
    private NotifyMessageMapper notifyMessageMapper;
    @Resource
    private NotifyTemplateMapper notifyTemplateMapper;
    @Resource
    private AdminUserMapper adminUserMapper;
    @Resource
    private DeptMapper deptMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private UserRoleMapper userRoleMapper;

    @Test
    void assignmentShouldRequirePersistedNotifyMessageId() {
        seedActors(true);
        seedNotifyTemplate();
        ShowroomProductRevision productRevision = publishProduct();

        ShowroomFieldAssignment assignment = assignmentService.createAssignment(new ShowroomAssignmentCreate(
                "PRODUCT", productRevision.productId(), "core_selling_points", 700L, 100L, null));

        ShowroomFieldAssignmentDO assignmentDO = assignmentMapper.selectById(assignment.assignmentId());
        NotifyMessageDO notifyMessage = notifyMessageMapper.selectById(assignmentDO.getNotifyMessageId());
        assertNotNull(notifyMessage);
        assertEquals(ASSIGNMENT_TEMPLATE_CODE, notifyMessage.getTemplateCode());
        assertEquals(700L, notifyMessage.getUserId());
        assertEquals(assignment.notifyMessageId(), notifyMessage.getId());
    }

    @Test
    void assignmentShouldRequireAssigneeEditorRole() {
        seedActors(false);
        seedNotifyTemplate();
        ShowroomProductRevision productRevision = publishProduct();

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> assignmentService.createAssignment(new ShowroomAssignmentCreate(
                        "PRODUCT", productRevision.productId(), "core_selling_points", 700L, 100L, null)));

        assertTrue(error.getMessage().contains("SHOWROOM_ROLE_BINDING_MISSING"));
        assertTrue(assignmentMapper.selectList().isEmpty());
    }

    @Test
    void completedAssignmentShouldAutoSubmitToSupervisorReview() {
        seedActors(true);
        seedNotifyTemplate();
        ShowroomProductRevision productRevision = publishProduct();

        ShowroomFieldAssignment assignment = assignmentService.createAssignment(new ShowroomAssignmentCreate(
                "PRODUCT", productRevision.productId(), "core_selling_points", 700L, 100L, null));

        ShowroomAssignmentSubmitResult result = assignmentService.completeAndSubmit(
                assignment.assignmentId(), "补充后的卖点", 700L, null);

        assertEquals("AUTO_SUBMITTED", result.assignment().status());
        assertNotNull(result.assignment().lastSavedRevisionId());
        assertEquals(result.changeRequest().changeRequestId(), result.assignment().lastChangeRequestId());
        assertEquals("PENDING_SUPERVISOR_REVIEW", result.changeRequest().status());
        assertEquals(300L, result.changeRequest().gaoxinUserId());
        assertEquals(assignment.assignmentId(), result.changeRequest().sourceAssignmentId());
        assertEquals(jsonValue("旧卖点"), result.changeRequest().items().get(0).oldValueJson());
        assertEquals(jsonValue("补充后的卖点"), result.changeRequest().items().get(0).newValueJson());
        assertEquals("补充后的卖点",
                contentService.getProductRevision(result.assignment().lastSavedRevisionId()).fields()
                        .get("core_selling_points"));
        assertEquals("AUTO_SUBMITTED", assignmentService.getAssignment(assignment.assignmentId()).status());
        assertEquals(1, assignmentService.pageAssignments("PRODUCT", productRevision.productId(), null, null, 1, 20)
                .size());
    }

    @Test
    void completedAssignmentShouldAutoSubmitToGaoxinWhenAssigneeDeptMissing() {
        seedActors(true, false);
        seedNotifyTemplate();
        ShowroomProductRevision productRevision = publishProduct();

        ShowroomFieldAssignment assignment = assignmentService.createAssignment(new ShowroomAssignmentCreate(
                "PRODUCT", productRevision.productId(), "core_selling_points", 700L, 100L, null));

        ShowroomAssignmentSubmitResult result = assignmentService.completeAndSubmit(
                assignment.assignmentId(), "补充后的卖点", 700L, null);

        assertEquals("AUTO_SUBMITTED", result.assignment().status());
        assertEquals("PENDING_GAOXIN_APPROVAL", result.changeRequest().status());
        assertEquals(300L, result.changeRequest().gaoxinUserId());
        assertEquals(assignment.assignmentId(), result.changeRequest().sourceAssignmentId());
        assertEquals("补充后的卖点",
                contentService.getProductRevision(result.assignment().lastSavedRevisionId()).fields()
                        .get("core_selling_points"));
        assertEquals("AUTO_SUBMITTED", assignmentService.getAssignment(assignment.assignmentId()).status());
    }

    @Test
    void completedAssignmentShouldAutoSubmitToGaoxinWhenAssigneeDeptLeaderMissing() {
        seedActors(true, true, false);
        seedNotifyTemplate();
        ShowroomProductRevision productRevision = publishProduct();

        ShowroomFieldAssignment assignment = assignmentService.createAssignment(new ShowroomAssignmentCreate(
                "PRODUCT", productRevision.productId(), "core_selling_points", 700L, 100L, null));

        ShowroomAssignmentSubmitResult result = assignmentService.completeAndSubmit(
                assignment.assignmentId(), "缺主管后的卖点", 700L, null);

        assertEquals("AUTO_SUBMITTED", result.assignment().status());
        assertEquals("PENDING_GAOXIN_APPROVAL", result.changeRequest().status());
        assertEquals(300L, result.changeRequest().gaoxinUserId());
        assertEquals(assignment.assignmentId(), result.changeRequest().sourceAssignmentId());
    }

    @Test
    void wholeProductAssignmentShouldReopenAfterRejectedChangeRequest() {
        seedActors(true);
        seedNotifyTemplate();
        ShowroomProductRevision productRevision = publishProduct();

        ShowroomFieldAssignment assignment = assignmentService.createAssignment(new ShowroomAssignmentCreate(
                "PRODUCT", productRevision.productId(),
                ShowroomAssignmentService.PRODUCT_WHOLE_ASSIGNMENT_FIELD_CODE, 700L, 100L, null));

        assignmentService.markWholeProductAssignmentSubmitted(
                productRevision.productId(), 700L, productRevision.revisionId(), 9100L);

        ShowroomFieldAssignment reopened = assignmentService.reopenWholeProductAssignmentForRejectedChangeRequest(9100L);

        assertEquals("OPEN", reopened.status());
        assertEquals(700L, reopened.assigneeUserId());
        assertEquals(productRevision.revisionId(), reopened.lastSavedRevisionId());
        assertEquals(9100L, reopened.lastChangeRequestId());

        ShowroomFieldAssignmentDO assignmentDO = assignmentMapper.selectById(assignment.assignmentId());
        assertEquals("OPEN", assignmentDO.getStatus());
        assertEquals(9100L, assignmentDO.getLastChangeRequestId());
        assertNull(assignmentDO.getClosedAt());
    }

    private ShowroomProductRevision publishProduct() {
        ShowroomProductRevision draft = contentService.saveProductDraft(new ShowroomProductDraft(null, "YT-GW-001",
                "导管鞘组 V1", "Introducer Sheath Set", Map.of(
                "target_market", "旧市场",
                "core_selling_points", "旧卖点",
                "registration_certificate", "注册证 V1"
        )));
        return contentService.publishProductRevision(draft.revisionId(), 901L);
    }

    private void seedNotifyTemplate() {
        NotifyTemplateDO template = NotifyTemplateDO.builder()
                .name("展厅指派提醒")
                .code(ASSIGNMENT_TEMPLATE_CODE)
                .nickname("展厅系统")
                .content("请处理{fieldCode}")
                .params(List.of("fieldCode"))
                .type(1)
                .status(ENABLE.getStatus())
                .remark("assignment notify")
                .build();
        notifyTemplateMapper.insert(template);
    }

    private void seedActors(boolean assignEditorRole) {
        seedActors(assignEditorRole, true);
    }

    private void seedActors(boolean assignEditorRole, boolean assignAssigneeDept) {
        seedActors(assignEditorRole, assignAssigneeDept, true);
    }

    private void seedActors(boolean assignEditorRole, boolean assignAssigneeDept, boolean assignDeptLeader) {
        DeptMapper deptMapper = this.deptMapper;
        deptMapper.deleteById(10L);
        deptMapper.deleteById(20L);
        adminUserMapper.deleteById(200L);
        adminUserMapper.deleteById(300L);
        adminUserMapper.deleteById(700L);
        roleMapper.deleteById(30L);
        roleMapper.deleteById(31L);
        userRoleMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserRoleDO>()
                .in(UserRoleDO::getUserId, 700L, 300L, 200L));

        var leader = new cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO();
        leader.setId(200L);
        leader.setUsername("leader");
        leader.setPassword("pwd");
        leader.setNickname("部门负责人");
        leader.setDeptId(10L);
        leader.setStatus(ENABLE.getStatus());
        leader.setCreateTime(LocalDateTime.now());
        adminUserMapper.insert(leader);

        var publicityApprover = new cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO();
        publicityApprover.setId(300L);
        publicityApprover.setUsername("publicity");
        publicityApprover.setPassword("pwd");
        publicityApprover.setNickname("企宣审批人");
        publicityApprover.setDeptId(20L);
        publicityApprover.setStatus(ENABLE.getStatus());
        publicityApprover.setCreateTime(LocalDateTime.now());
        adminUserMapper.insert(publicityApprover);

        var assignee = new cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO();
        assignee.setId(700L);
        assignee.setUsername("editor");
        assignee.setPassword("pwd");
        assignee.setNickname("编辑");
        assignee.setDeptId(assignAssigneeDept ? 10L : null);
        assignee.setStatus(ENABLE.getStatus());
        assignee.setCreateTime(LocalDateTime.now());
        adminUserMapper.insert(assignee);

        var dept = new cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO();
        dept.setId(10L);
        dept.setName("展厅部");
        dept.setParentId(0L);
        dept.setSort(1);
        dept.setLeaderUserId(assignDeptLeader ? 200L : null);
        dept.setStatus(ENABLE.getStatus());
        dept.setCreateTime(LocalDateTime.now());
        deptMapper.insert(dept);

        var publicityDept = new cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO();
        publicityDept.setId(20L);
        publicityDept.setName("企宣部");
        publicityDept.setParentId(0L);
        publicityDept.setSort(2);
        publicityDept.setLeaderUserId(300L);
        publicityDept.setStatus(ENABLE.getStatus());
        publicityDept.setCreateTime(LocalDateTime.now());
        deptMapper.insert(publicityDept);

        var role = new cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO();
        role.setId(30L);
        role.setName("编辑角色");
        role.setCode("EDITOR");
        role.setSort(1);
        role.setStatus(ENABLE.getStatus());
        role.setType(2);
        role.setDataScope(1);
        role.setCreateTime(LocalDateTime.now());
        role.setTenantId(TenantContextHolder.getRequiredTenantId());
        roleMapper.insert(role);

        var publicityRole = new cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO();
        publicityRole.setId(31L);
        publicityRole.setName("企宣角色");
        publicityRole.setCode("showroom_publicity");
        publicityRole.setSort(2);
        publicityRole.setStatus(ENABLE.getStatus());
        publicityRole.setType(2);
        publicityRole.setDataScope(1);
        publicityRole.setCreateTime(LocalDateTime.now());
        publicityRole.setTenantId(TenantContextHolder.getRequiredTenantId());
        roleMapper.insert(publicityRole);

        UserRoleDO publicityUserRole = new UserRoleDO();
        publicityUserRole.setUserId(300L);
        publicityUserRole.setRoleId(31L);
        userRoleMapper.insert(publicityUserRole);

        if (assignEditorRole) {
            UserRoleDO userRole = new UserRoleDO();
            userRole.setUserId(700L);
            userRole.setRoleId(30L);
            userRoleMapper.insert(userRole);
        }
    }

    private static String jsonValue(String value) {
        LinkedHashMap<String, String> payload = new LinkedHashMap<>();
        payload.put("value", value);
        return JsonUtils.toJsonString(payload);
    }

}
