package cn.iocoder.yudao.module.dcc.service.projectcode;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.bpm.approval.core.*;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskQueryContext;
import cn.iocoder.yudao.module.bpm.service.task.*;
import cn.iocoder.yudao.module.dcc.approval.DccApprovalTaskAdapter;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.productcreate.DccProjectProductCreateReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.*;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileWorkflowService;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.folder.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApiImpl;
import cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyTemplateDO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyOperationDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditServiceImpl;
import cn.iocoder.yudao.module.system.service.notify.*;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.*;
import org.springframework.test.context.jdbc.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Actual Spring/H2 request + official notify services + Gxp; account/template ports are explicit fixtures. */
@Import({DccProjectReviewerConfigurationService.class,DccProjectProductCreateServiceImpl.class,
        DccProjectProductCreateWriteService.class,DccProjectProductCreateStateService.class,
        DccProjectProductCreateFailureService.class,DccProjectAttributesService.class,DccProjectLeaderService.class,
        DccFolderTemplateService.class,DccProjectConfigurationAuditService.class,DccProjectProductAuditService.class,
        GxpAuditServiceImpl.class,NotifyMessageSendApiImpl.class,NotifySendServiceImpl.class,
        NotifyMessageServiceImpl.class,DccApprovalTaskAdapter.class,
        DccProjectProductActorSupport.class,DccProjectProductNotificationService.class,
        cn.iocoder.yudao.module.dcc.approval.DccProjectProductTaskDelegate.class})
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(scripts={"/sql/dcc_b_gxp_audit_tables.sql","/sql/g49_project_application_notify_tables.sql"},
        executionPhase=Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts="/sql/dcc_b_gxp_audit_clean.sql",executionPhase=Sql.ExecutionPhase.AFTER_TEST_METHOD)
class DccProjectApplicationNotificationTest extends BaseDbUnitTest {
    @Resource javax.sql.DataSource source;
    @Resource DccProjectProductCreateServiceImpl service;
    @Resource DccProjectProductCreateRequestMapper requests;
    @Resource DccProjectProductIdentityClaimMapper claims;
    @Resource DccFolderTemplateMapper folders;
    @Resource GxpAuditPolicyOperationMapper policies;
    @Resource GxpAuditEventMapper events;
    @Resource DccApprovalTaskAdapter adapter;
    @Resource DccProjectProductNotificationService notification;
    @MockitoSpyBean NotifyMessageSendApiImpl messageApi;
    @MockitoBean AdminUserApi users;
    @MockitoBean PermissionApi permissions;
    @MockitoBean DccProjectAccessService access;
    @MockitoBean NotifyTemplateService templates;
    @MockitoBean BpmTaskService tasks;
    @MockitoBean BpmProcessInstanceService instances;
    @MockitoBean DccControlledFileWorkflowService fileWorkflow;

    JdbcTemplate jdbc() { return new JdbcTemplate(source); }
    @BeforeEach void fixtures() {
        for (long id:List.of(1L,2L,7L,9L)) {
            String name=id==1?"admin":"actor-"+id;
            var user=new AdminUserRespDTO().setId(id).setTenantId(1L).setStatus(0).setUsername(name).setNickname("账号"+id);
            when(users.getUser(id)).thenReturn(user);
            when(permissions.hasAnyPermissions(id,"dcc:project-code:update")).thenReturn(true);
            when(permissions.hasAnyPermissions(id,"dcc:project-code:query")).thenReturn(true);
            jdbc().update("INSERT INTO system_users(id,tenant_id,username,canonical_username,nickname,status) VALUES(?,1,?,?,?,0)",id,name,name,"账号"+id);
        }
        jdbc().update("INSERT INTO dcc_project_reviewer_config(tenant_id,reviewer_user_id,reviewer_username,reviewer_nickname,version_no,updated_by,change_reason) VALUES(1,2,'actor-2','账号2',1,1,'隔离正式配置')");
        var template=new NotifyTemplateDO();template.setId(701L);template.setCode("dcc-project-product-application-event");
        template.setType(2);template.setNickname("DCC");template.setStatus(0);template.setContent("{businessTitle}：{eventName}，{reason}");
        template.setParams(List.of("businessTitle","eventName","reason","notifyTargetType","notifyTargetId","actionUrl"));
        when(templates.getNotifyTemplateByCodeFromCache(template.getCode())).thenReturn(template);
        when(templates.formatNotifyTemplateContent(anyString(),anyMap())).thenAnswer(a->JsonUtils.toJsonString(a.getArgument(1)));
        when(tasks.getTaskTodoPage(any(),any())).thenReturn(new PageResult<>(List.of(),0L));
        when(tasks.getTaskDonePage(any(),any())).thenReturn(new PageResult<>(List.of(),0L));
        for(String operation:List.of("create","review","approve","complete","resubmit")) {
            var p=new GxpAuditPolicyOperationDO();p.setTenantId(1L);p.setOperationId("dcc.project-product."+operation);
            p.setPolicyVersion("G49_ISOLATED");p.setSourceType("SERVICE_METHOD");p.setSourceLocator("ISOLATED_TEST");
            p.setDomain("DCC");p.setSubjectType("DCC_PROJECT_PRODUCT_REQUEST");p.setActionType("UPDATE");
            p.setReasonPolicy("REQUIRED");p.setSignaturePolicy("NOT_REQUIRED");p.setStatePolicy("BEFORE_AFTER");
            p.setRetentionClass("ISOLATED");p.setTestIds("G49");p.setOwner("TEST");p.setApplicability("GXP");p.setActive(true);policies.insert(p);
        }
        login(9L);
    }
    @AfterEach void clearIdentity() { SecurityContextHolder.clearContext(); }
    void login(Long id) {
        var actor=new LoginUser();actor.setId(id);actor.setTenantId(1L);actor.setUserType(2);
        actor.setInfo(Map.of(LoginUser.INFO_KEY_NICKNAME,"账号"+id,LoginUser.INFO_KEY_DEPT_ID,"9"));
        SecurityFrameworkUtils.setLoginUser(actor,new MockHttpServletRequest());
    }
    DccProjectProductCreateReqVO input() {
        var f=new DccFolderTemplateDO();f.setTenantId(1L);f.setName("来源模板");f.setActive(true);f.setEverUsed(false);f.setEditedByUserId(7L);
        f.setStructureJson(JsonUtils.toJsonString(new DccFolderTemplateStructure(List.of(new DccFolderTemplateStructure.Node("root",null,"目录",0)))));folders.insert(f);
        var r=new DccProjectProductCreateReqVO();r.setProjectName("通知项目");r.setProjectCode("G49-P");r.setProjectLeaderUserId(7L);r.setFolderTemplateId(f.getId());
        r.setDefaultAttributes(new DccProjectAttributes(List.of("CE"),null,"Y","N","N",null));r.setProductCode("G49-PR");r.setProductName("通知产品");r.setClassification("一类");r.setCreationReason("新建真实原因");r.setResubmissionReason("修正真实原因");return r;
    }
    List<Map<String,Object>> messages() { return jdbc().queryForList("SELECT user_id,business_key,template_params FROM system_notify_message WHERE tenant_id=1 ORDER BY id"); }
    @Test void createFreezesReviewerAndPersistsOneOfficialMessageWithRequestAndLedger() {
        Long id=service.createRequest(9L,input());
        assertEquals(1,messages().size());assertEquals(2L,((Number)messages().get(0).get("user_id")).longValue());
        assertTrue(messages().get(0).get("template_params").toString().contains("/mdm/product-catalog?requestId="+id));
        assertEquals(3,claims.selectList().size());assertEquals(1,events.selectList().size());
    }
    @Test void missingNotifyTemplateRollsBackActualRequestClaimsAndGxp() {
        when(templates.getNotifyTemplateByCodeFromCache(anyString())).thenReturn(null);
        assertThrows(RuntimeException.class,()->service.createRequest(9L,input()));
        assertTrue(requests.selectList().isEmpty());assertTrue(claims.selectList().isEmpty());assertTrue(events.selectList().isEmpty());assertTrue(messages().isEmpty());
    }
    @Test void reviewPassSendsApprovalTaskToExactCurrentTenantAdmin() {
        Long id=service.createRequest(9L,input());login(2L);service.review(2L,id,"审核实际原因",true);
        assertEquals(2,messages().size());assertEquals(1L,((Number)messages().get(1).get("user_id")).longValue());
        assertEquals("PENDING_APPROVAL",requests.selectById(id).getStatus());
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void eachRejectionNotifiesOriginalApplicantWithoutRewritingOriginalIdentity(boolean atApproval) {
        Long id=service.createRequest(9L,input());login(2L);
        if(atApproval) {service.review(2L,id,"真实审核通过",true);login(1L);service.approve(1L,id,"真实批准驳回",false);}
        else service.review(2L,id,"真实审核驳回",false);
        assertEquals(atApproval?3:2,messages().size());var message=messages().get(messages().size()-1);
        assertEquals(9L,((Number)message.get("user_id")).longValue());assertEquals("REJECTED",requests.selectById(id).getStatus());
    }
    @Test void frozenReviewerGetsNativeTodoWithoutFabricatingFileOrBpmIdentity() {
        Long id=service.createRequest(9L,input());
        var page=adapter.page(ApprovalTaskQueryContext.of(2L,ApprovalTaskViewType.TODO,ApprovalModuleCode.DCC,null,1,10));
        assertEquals(1,page.getTotal());var row=page.getList().get(0);
        assertEquals(String.valueOf(id),row.getBusinessKey());assertEquals("DCC_PROJECT_PRODUCT_REVIEW",row.getSourceTaskType());
        assertNull(row.getProcessInstanceId());assertEquals(Boolean.FALSE,row.getRequiresSignature());assertEquals(Set.of("PROCESS_IN_MODULE"),row.getAvailableActions());
    }

    @Test void lateFailureAfterActualOfficialMessageInsertRollsBackAllSameTransactionRows() {
        doAnswer(call -> { call.callRealMethod(); throw new IllegalStateException("ACTUAL_MESSAGE_POST_INSERT_FAILURE"); })
                .when(messageApi).sendSingleMessageIdempotentlyToAdmin(any());
        assertThrows(RuntimeException.class, () -> service.createRequest(9L,input()));
        assertTrue(requests.selectList().isEmpty());assertTrue(claims.selectList().isEmpty());
        assertTrue(events.selectList().isEmpty());assertTrue(messages().isEmpty());
    }

    @Test void officialMessageReplayIsZeroWriteAndConflictingPayloadRejects() {
        Long id=service.createRequest(9L,input());
        var tx=new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.datasource.DataSourceTransactionManager(source));
        tx.executeWithoutResult(ignored -> notification.reviewTodo(requests.selectById(id),"新建真实原因"));
        assertEquals(1,messages().size());
        assertThrows(RuntimeException.class,()->tx.executeWithoutResult(ignored -> notification.reviewTodo(requests.selectById(id),"冲突载荷")));
        assertEquals(1,messages().size());assertEquals("PENDING_REVIEW",requests.selectById(id).getStatus());
    }

    @Test void noEligibleAdminOrDisabledRecipientCannotLeaveReviewDecisionCommitted() {
        Long id=service.createRequest(9L,input());login(2L);
        jdbc().update("UPDATE system_users SET status=1 WHERE id=1");
        String before=JsonUtils.toJsonString(requests.selectById(id));int eventCount=events.selectList().size();
        assertThrows(RuntimeException.class,()->service.review(2L,id,"真实审核意见",true));
        assertEquals(before,JsonUtils.toJsonString(requests.selectById(id)));
        assertEquals(eventCount,events.selectList().size());assertEquals(1,messages().size());
    }

    @Test void exactAuthorizedReadRetainsRejectedOriginalAndAccurateSuccessorWhileOtherActorCannotRead() {
        var input=input();Long id=service.createRequest(9L,input);login(2L);service.review(2L,id,"修正原因",false);
        login(9L);Long next=service.resubmitRejectedRequest(9L,id,input);
        var original=service.getRequest(9L,id);
        assertEquals("REJECTED",original.getStatus());assertEquals(next,original.getResubmittedRequestId());
        assertEquals(id,service.getRequest(9L,next).getPreviousRequestId());assertEquals(3,messages().size());
        assertThrows(RuntimeException.class,()->service.getRequest(7L,id));
        var foreign=requests.selectById(id);foreign.setTenantId(2L);requests.updateById(foreign);
        assertThrows(RuntimeException.class,()->service.getRequest(9L,id));
    }

    @Test void completedRequestHasExactReadableOriginalIdentityAndStageDoneFacts() {
        Long id=service.createRequest(9L,input());login(2L);service.review(2L,id,"通过审核",true);login(1L);service.approve(1L,id,"批准意见",true);
        assertEquals("COMPLETED",service.getRequest(9L,id).getStatus());
        var done=adapter.page(ApprovalTaskQueryContext.of(2L,ApprovalTaskViewType.DONE,ApprovalModuleCode.DCC,"G49-P",1,10));
        assertEquals(1,done.getTotal());assertEquals(ApprovalTaskReviewResult.APPROVE,done.getList().get(0).getApprovalResult());
        assertNotNull(done.getList().get(0).getTaskCompletedAt());assertEquals("G49-P",done.getList().get(0).getBusinessCode());
    }

    @Test void currentActorKeywordAndExactNativeTimelineCannotBorrowFileOrForeignStageIdentity() {
        Long id=service.createRequest(9L,input());
        assertEquals(0,adapter.page(ApprovalTaskQueryContext.of(7L,ApprovalTaskViewType.TODO,ApprovalModuleCode.DCC,null,1,10)).getTotal());
        assertEquals(0,adapter.page(ApprovalTaskQueryContext.of(2L,ApprovalTaskViewType.TODO,ApprovalModuleCode.DCC,"other-code",1,10)).getTotal());
        var todo=adapter.page(ApprovalTaskQueryContext.of(2L,ApprovalTaskViewType.TODO,ApprovalModuleCode.DCC,"G49-P",1,10)).getList().get(0);
        var timeline=cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineQueryContext.of(2L,ApprovalModuleCode.DCC,
                todo.getSourceTaskType(),todo.getSourceTaskId(),id.toString(),null);
        assertEquals(1,adapter.listTimeline(timeline).size());assertEquals("PENDING_REVIEW",adapter.listTimeline(timeline).get(0).getStatus());
        timeline.setSourceTaskId("task-file");assertThrows(RuntimeException.class,()->adapter.listTimeline(timeline));
    }

    @Test void actualDetailControllerSerializesEveryLongAsStringWithoutChangingAttemptNumber() throws Exception {
        Long id=service.createRequest(9L,input());
        var row=requests.selectById(id);row.setRelationId(9007199254740993L);row.setWriteAttemptNo(2);requests.updateById(row);
        var controller=new cn.iocoder.yudao.module.dcc.controller.admin.projectcode.DccProjectProductCreateController();
        org.springframework.test.util.ReflectionTestUtils.setField(controller,"service",service);
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/dcc/project-product-requests/"+id))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.id").value(id.toString()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.relationId").value("9007199254740993"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.configuredReviewerUserId").value("2"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.writeAttemptNo").value(2));
    }

    @Test void reviewerWithoutFormalQueryPermissionCannotReceiveAnUnopenableTaskOrMessage() {
        when(permissions.hasAnyPermissions(2L,"dcc:project-code:query")).thenReturn(false);
        assertThrows(RuntimeException.class,()->service.createRequest(9L,input()));
        assertTrue(requests.selectList().isEmpty());assertTrue(events.selectList().isEmpty());assertTrue(messages().isEmpty());
    }

    @Test void applicantAndApproverNeedTheirExistingReadPermissionWithoutAutomaticGrant() {
        when(permissions.hasAnyPermissions(9L,"dcc:project-code:query")).thenReturn(false);
        assertThrows(RuntimeException.class,()->service.createRequest(9L,input()));
        assertTrue(requests.selectList().isEmpty());assertTrue(messages().isEmpty());
        when(permissions.hasAnyPermissions(9L,"dcc:project-code:query")).thenReturn(true);
        Long id=service.createRequest(9L,input());login(2L);
        when(permissions.hasAnyPermissions(1L,"dcc:project-code:query")).thenReturn(false);
        assertThrows(RuntimeException.class,()->service.review(2L,id,"实际审核通过",true));
        assertEquals("PENDING_REVIEW",requests.selectById(id).getStatus());assertEquals(1,messages().size());
        assertEquals(0,adapter.page(ApprovalTaskQueryContext.of(1L,ApprovalTaskViewType.TODO,ApprovalModuleCode.DCC,null,1,10)).getTotal());
    }

    @Test void nativeTimelineKeepsReviewPassWhenTheLaterApprovalRejects() {
        Long id=service.createRequest(9L,input());
        var originalTask=adapter.page(ApprovalTaskQueryContext.of(2L,ApprovalTaskViewType.TODO,ApprovalModuleCode.DCC,null,1,10)).getList().get(0);
        login(2L);service.review(2L,id,"审核实际通过",true);login(1L);service.approve(1L,id,"批准实际驳回",false);
        var query=cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineQueryContext.of(2L,ApprovalModuleCode.DCC,
                originalTask.getSourceTaskType(),originalTask.getSourceTaskId(),id.toString(),null);
        var timeline=adapter.listTimeline(query);assertEquals(3,timeline.size());
        assertEquals("SUBMIT",timeline.get(0).getAction());assertEquals("PENDING_REVIEW",timeline.get(0).getStatus());
        assertEquals("APPROVE",timeline.get(1).getAction());assertEquals("PENDING_APPROVAL",timeline.get(1).getStatus());
        assertEquals("审核实际通过",timeline.get(1).getComment());assertEquals(2L,timeline.get(1).getActorUserId());
        assertEquals("REJECT",timeline.get(2).getAction());assertEquals("REJECTED",timeline.get(2).getStatus());
        assertEquals("批准实际驳回",timeline.get(2).getComment());assertEquals(1L,timeline.get(2).getActorUserId());
    }
}
