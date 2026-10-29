package cn.iocoder.yudao.module.dcc.service.projectcode;

import cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.*;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.productcreate.DccProjectProductCreateReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccFolderTemplateDO;
import cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccFolderTemplateStructure;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserIdempotentReqDTO;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Import({DccProjectReviewerRoleService.class,DccProjectProductCreateServiceImpl.class,DccProjectProductCreateStateService.class,
 DccProjectProductCreateFailureService.class,DccProjectReviewerConfigurationService.class,DccProjectProductActorSupport.class,DccProjectProductNotificationService.class,
 cn.iocoder.yudao.module.dcc.approval.DccProjectProductTaskDelegate.class})
class DccProjectSingleRoleReviewTest extends DccApprovedProjectLeaderOwnerTest {
 @Resource DccProjectProductCreateService service;
 @Resource DccProjectReviewerRoleService reviewers;
 @MockitoBean NotifyMessageSendApi notices;
 @Resource cn.iocoder.yudao.module.dcc.approval.DccProjectProductTaskDelegate nativeTasks;
 @BeforeEach void roleFixture(){
  jdbc.update("INSERT INTO gxp_audit_ledger_sequence(tenant_id,next_ledger_sequence) VALUES(1,1)");
  jdbc.update("INSERT INTO gxp_audit_policy_activation(tenant_id,policy_version,policy_hash,request_id,actor_id,approval_reference,activated_at_utc,effective_after_sequence,canonical_activation_json,activation_hash) VALUES(1,'isolated-owner-test','project-center-policy','project-center-fixture',9,'isolated-test',CURRENT_TIMESTAMP,0,'{}','project-center-activation')");
  jdbc.execute("CREATE TABLE IF NOT EXISTS system_role(id BIGINT PRIMARY KEY,tenant_id BIGINT,code VARCHAR(64),name VARCHAR(64),status INT,deleted INT DEFAULT 0)");
  jdbc.execute("CREATE TABLE IF NOT EXISTS system_user_role(id BIGINT PRIMARY KEY,tenant_id BIGINT,user_id BIGINT,role_id BIGINT,deleted INT DEFAULT 0)");
  jdbc.execute("CREATE TABLE IF NOT EXISTS system_users(id BIGINT PRIMARY KEY,tenant_id BIGINT,username VARCHAR(64),nickname VARCHAR(64),status INT,deleted INT DEFAULT 0)");
  jdbc.update("DELETE FROM system_role");jdbc.update("DELETE FROM system_user_role");jdbc.update("DELETE FROM system_users");
  jdbc.update("INSERT INTO system_role VALUES(10,1,'dcc_project_reviewer','项目审核员',0,0)");
  jdbc.update("INSERT INTO system_user_role VALUES(70,1,7,10,0),(80,1,8,10,0)");
  jdbc.update("INSERT INTO system_users(id,tenant_id,username,nickname,status) VALUES(1,1,'admin','管理员',0),(7,1,'a','审核甲',0),(8,1,'b','审核乙',0),(9,1,'applicant','申请人',0)");
  for(long id:new long[]{1,7,8,9})when(permissions.hasAnyPermissions(id,"dcc:project-code:query")).thenReturn(true);
  for(long id:new long[]{7,8})when(permissions.hasAnyPermissions(id,"dcc:project-code:review")).thenReturn(true);
  when(notices.sendSingleMessageIdempotentlyToAdmin(any())).thenReturn(100L);
  var completePolicy=policies.selectList().get(0);
  for(String op:List.of("dcc.project-product.create","dcc.project-product.review")){
   var policy=cn.iocoder.yudao.framework.common.util.object.BeanUtils.toBean(completePolicy,cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyOperationDO.class);policy.setId(null);policy.setOperationId(op);policies.insert(policy);
  }
  jdbc.update("INSERT INTO dcc_project_reviewer_config(tenant_id,reviewer_user_id,reviewer_username,reviewer_nickname,version_no,updated_by,change_reason) VALUES(1,7,'a','审核甲',1,1,'旧配置仅用于legacy')");
  actor(9L);
 }
 @AfterEach void roleCleanup(){jdbc.update("DELETE FROM system_user_role");jdbc.update("DELETE FROM system_role");jdbc.update("DELETE FROM system_users");}
 void actor(long id){TenantContextHolder.setTenantId(1L);TenantContextHolder.setIgnore(false);SecurityFrameworkUtils.setLoginUser(new LoginUser().setId(id).setTenantId(1L).setUserType(2),new org.springframework.mock.web.MockHttpServletRequest());}
 DccProjectProductCreateReqVO input(String suffix){
  var template=new DccFolderTemplateDO();template.setTenantId(1L);template.setName("模板"+suffix);template.setActive(true);template.setEverUsed(false);template.setEditedByUserId(7L);
  template.setStructureJson(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(new DccFolderTemplateStructure(List.of(new DccFolderTemplateStructure.Node("root",null,"正式目录",0)))));templates.insert(template);
  var req=new DccProjectProductCreateReqVO();req.setProjectName("项目"+suffix);req.setProjectCode("ROLE-"+suffix);req.setProjectLeaderUserId(7L);req.setDefaultAttributes(new cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes(List.of("NMPA"),null,"Y","N","N",null));req.setFolderTemplateId(template.getId());req.setProductName("产品"+suffix);req.setProductCode("PRODUCT-"+suffix);req.setClassification("一类");req.setCreationReason("新建角色审核项目");return req;
 }
 Long create(String suffix){actor(9L);return service.createRequest(9L,input(suffix));}
 @Test void newRequestFreezesOnlyRoleAndNotifiesEveryEligibleMemberNotAnAdminSecondStage(){
  Long id=create("CREATE");var row=requests.selectById(id);assertEquals(DccProjectReviewerRoleService.CONTRACT,row.getReviewContract());assertEquals(10L,row.getReviewerRoleId());assertNull(row.getConfiguredReviewerUserId());
  var capture=org.mockito.ArgumentCaptor.forClass(NotifySendSingleToUserIdempotentReqDTO.class);verify(notices,times(2)).sendSingleMessageIdempotentlyToAdmin(capture.capture());assertEquals(Set.of(7L,8L),new HashSet<>(capture.getAllValues().stream().map(NotifySendSingleToUserIdempotentReqDTO::getUserId).toList()));
 }
 @Test void secondRoleMemberAloneCompletesWholeCreationWithoutAdminOrUpdatePermission(){
  Long id=create("ONE");actor(8L);
  var task=nativeTasks.list(cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskQueryContext.of(8L,cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType.TODO,cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode.DCC,null,1,10)).get(0);
  assertEquals(Set.of("APPROVE","REJECT"),task.getAvailableActions());assertTrue(task.getRequiresSignature());
  nativeTasks.review(cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskReviewContext.of(8L,cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode.DCC,task.getSourceTaskType(),task.getSourceTaskId(),id.toString(),null,cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult.APPROVE,"核对通过","signature",false));
  var done=requests.selectById(id);assertEquals("COMPLETED",done.getStatus());
  assertEquals(8L,done.getReviewerUserId());assertNotNull(done.getReviewedTime());assertNull(done.getApproverUserId());assertNull(done.getApprovedTime());
  assertEquals(1,projects.selectList().size());assertEquals(1,products.selectList().size());assertEquals(1,relations.selectList().size());assertEquals(1,folderRows.selectList().size());assertEquals("OWNER",accessRules.selectList().get(0).getAccessLevel());assertEquals(7L,accessRules.selectList().get(0).getSubjectId());
  assertEquals(3,events.selectList().size());verify(permissions,never()).hasAnyPermissions(8L,"dcc:project-code:update");
 }
 @Test void oldSingleUserConfigCannotRescueMissingNewRoleAndRevokedMemberCannotApprove(){
  jdbc.update("DELETE FROM system_role");assertThrows(RuntimeException.class,()->create("NO-ROLE"));assertTrue(requests.selectList().isEmpty());
  jdbc.update("INSERT INTO system_role VALUES(10,1,'dcc_project_reviewer','项目审核员',0,0)");Long id=create("REVOKE");jdbc.update("DELETE FROM system_user_role WHERE user_id=8");actor(8L);assertThrows(RuntimeException.class,()->service.review(8L,id,"撤销后不能审核",true));assertEquals("PENDING_REVIEW",requests.selectById(id).getStatus());assertTrue(projects.selectList().isEmpty());
 }
 @Test void sameActorExactReplayReturnsOriginalWithoutNewAuditOrAssetsDifferentPayloadRejects(){
  Long id=create("REPLAY");actor(7L);var done=service.review(7L,id,"确认通过",true);int count=events.selectList().size();
  assertEquals(done.getGeneratedProjectCodeId(),service.review(7L,id,"确认通过",true).getGeneratedProjectCodeId());assertEquals(count,events.selectList().size());assertEquals(1,projects.selectList().size());
  assertThrows(RuntimeException.class,()->service.review(7L,id,"另一意见",true));actor(8L);assertThrows(RuntimeException.class,()->service.review(8L,id,"确认通过",true));
 }
 @Test void lateActualCompletionAuditFailureRollsBackReviewDecisionAndAllAssets(){
  Long id=create("ROLLBACK");int count=events.selectList().size();
  doThrow(new IllegalStateException("完整建档审计故障")).when(actualAuditKernel).append(argThat(command->"dcc.project-product.complete".equals(command.getOperationId())));
  actor(8L);assertThrows(IllegalStateException.class,()->service.review(8L,id,"确认通过",true));var row=requests.selectById(id);assertEquals("PENDING_REVIEW",row.getStatus());assertNull(row.getReviewerUserId());assertNull(row.getReviewedTime());
  assertTrue(projects.selectList().isEmpty());assertTrue(products.selectList().isEmpty());assertTrue(folderRows.selectList().isEmpty());assertTrue(accessRules.selectList().isEmpty());assertTrue(relations.selectList().isEmpty());assertEquals(count,events.selectList().size());
 }
 @Test void roleRejectionReplaysWithoutAdminAndLegacyApprovalEndpointsRejectNewMode(){
  Long id=create("REJECT");actor(7L);var rejected=service.review(7L,id,"退回补充",false);assertEquals("REJECTED",rejected.getStatus());assertNull(rejected.getApproverUserId());int count=events.selectList().size();service.review(7L,id,"退回补充",false);assertEquals(count,events.selectList().size());
  actor(1L);assertThrows(RuntimeException.class,()->service.approve(1L,id,"不能二审",true));assertThrows(RuntimeException.class,()->service.retryWrite(1L,id,"不能二审重试"));
 }
 @Test void currentRoleMembersShareOneNativeTodoAndLaterMemberCanReadThenWinnerClosesAll(){
  Long id=create("TASKS");
  var context=new cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskQueryContext().setLoginUserId(7L).setViewType(cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType.TODO);
  actor(7L);var a=nativeTasks.list(context);assertEquals(1,a.size());assertNull(a.get(0).getAssigneeUserId());assertEquals("项目审核员",a.get(0).getCurrentNodeName());assertFalse(a.get(0).getRequiresSignature());
  actor(8L);context.setLoginUserId(8L);assertEquals(a.get(0).getSourceTaskId(),nativeTasks.list(context).get(0).getSourceTaskId());
  jdbc.update("DELETE FROM system_user_role WHERE user_id=8");assertTrue(nativeTasks.list(context).isEmpty());
  jdbc.update("INSERT INTO system_user_role VALUES(90,1,9,10,0)");when(permissions.hasAnyPermissions(9L,"dcc:project-code:review")).thenReturn(true);actor(9L);context.setLoginUserId(9L);
  assertEquals(id,service.getRequest(9L,id).getId());assertEquals(1,nativeTasks.list(context).size());service.review(9L,id,"新增成员一次审批",true);assertTrue(nativeTasks.list(context).isEmpty());
  context.setViewType(cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType.DONE);var done=nativeTasks.list(context);assertEquals(1,done.size());assertEquals(9L,done.get(0).getAssigneeUserId());
 }
 @Test void oldNullContractPreservesFrozenReviewerThenAdminTwoStageAndNoRoleRewrite(){
  Long id=approvedRequest();jdbc.update("UPDATE dcc_project_product_create_request SET status='PENDING_REVIEW',configured_reviewer_user_id=7,configured_reviewer_username='a',configured_reviewer_nickname='审核甲',write_attempt_no=NULL,write_reason=NULL,write_operator_user_id=NULL WHERE id=?",id);
  when(users.getUser(1L)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(1L).setTenantId(1L).setStatus(0).setUsername("admin").setNickname("管理员"));when(permissions.hasAnyPermissions(1L,"dcc:project-code:update")).thenReturn(true);
  when(permissions.hasAnyPermissions(7L,"dcc:project-code:update")).thenReturn(true);actor(7L);var reviewed=service.review(7L,id,"旧审核通过",true);assertEquals("PENDING_APPROVAL",reviewed.getStatus());assertNull(reviewed.getReviewContract());assertTrue(projects.selectList().isEmpty());
  var policy=cn.iocoder.yudao.framework.common.util.object.BeanUtils.toBean(policies.selectList().get(0),cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyOperationDO.class);policy.setId(null);policy.setOperationId("dcc.project-product.approve");policies.insert(policy);
  actor(1L);var completed=service.approve(1L,id,"旧管理员二审",true);assertEquals("COMPLETED",completed.getStatus());assertEquals(1L,completed.getApproverUserId());assertNull(completed.getReviewContract());
 }
 @Test void concurrentExactSameActorDecisionHasOneCreationAndOneOriginalReplay() throws Exception {
  Long id=create("CONCURRENT");var ready=new CountDownLatch(2);var release=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
  java.util.concurrent.Callable<Long> decision=()->{actor(8L);ready.countDown();try{if(!release.await(10,TimeUnit.SECONDS))throw new IllegalStateException("test timeout");return service.review(8L,id,"同一确认",true).getGeneratedProjectCodeId();}finally{TenantContextHolder.clear();org.springframework.security.core.context.SecurityContextHolder.clearContext();}};
  try{var a=pool.submit(decision);var b=pool.submit(decision);assertTrue(ready.await(10,TimeUnit.SECONDS));release.countDown();assertEquals(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS));assertEquals(1,projects.selectList().size());assertEquals(3,events.selectList().size());}
  finally{release.countDown();pool.shutdownNow();}
 }
 @Test void directoryFailureBeforeCompletionRollsBackReviewAndOwnerThenLegitimateRetryWorks(){
  Long id=create("DIRECTORY-FAIL");actor(8L);doThrow(new IllegalStateException("目录写失败")).when(folderRows).insert(any(cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectFolderDO.class));
  assertThrows(IllegalStateException.class,()->service.review(8L,id,"确认通过",true));assertEquals("PENDING_REVIEW",requests.selectById(id).getStatus());assertTrue(accessRules.selectList().isEmpty());assertTrue(projects.selectList().isEmpty());assertEquals(1,events.selectList().size());
  reset(folderRows);assertEquals("COMPLETED",service.review(8L,id,"确认通过",true).getStatus());
 }
 @Test void actualControllerRoleReadinessAndPermissionSplitReturnTypedRoleAndLiveCanReview() throws Exception{
  Long id=create("HTTP");actor(8L);var controller=new cn.iocoder.yudao.module.dcc.controller.admin.projectcode.DccProjectProductCreateController();
  org.springframework.test.util.ReflectionTestUtils.setField(controller,"service",service);org.springframework.test.util.ReflectionTestUtils.setField(controller,"reviewerRoles",reviewers);
  var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller).build();
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/dcc/project-product-requests/reviewer-role"))
    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.reviewContract").value(DccProjectReviewerRoleService.CONTRACT))
    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.roleId").value("10"))
    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.candidates.length()").value(2));
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/dcc/project-product-requests/{id}",id))
    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.canReview").value(true));
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/dcc/project-product-requests/{id}/review/approve",id).contentType("application/json").content("{\"reason\":\"一个角色成员审批\"}"))
    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.status").value("COMPLETED"))
    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.canReview").value(false));
  assertEquals("@ss.hasAnyPermissions('dcc:project-code:review','dcc:project-code:update')",controller.getClass().getMethod("reviewApprove",Long.class,cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.productcreate.DccProjectProductApprovalActionReqVO.class).getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class).value());
 }
 @Test void submittedRoleIdentityCannotBeOverwrittenByNormalRequestUpdate(){
  Long id=create("IMMUTABLE");var row=requests.selectById(id);row.setReviewerRoleId(999L);row.setReviewerRoleCode("other");row.setReviewContract("UNKNOWN");requests.updateById(row);
  var stored=requests.selectById(id);assertEquals(10L,stored.getReviewerRoleId());assertEquals("dcc_project_reviewer",stored.getReviewerRoleCode());assertEquals(DccProjectReviewerRoleService.CONTRACT,stored.getReviewContract());
 }
 @Test void rejectedLegacyRequestResubmitsAsExplicitNewRoleContractWithoutChangingOldHistory(){
  Long oldId=approvedRequest();jdbc.update("UPDATE dcc_project_product_create_request SET status='REJECTED',configured_reviewer_user_id=7,reject_reason='旧意见' WHERE id=?",oldId);
  String oldBefore=cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(requests.selectById(oldId));
  var policy=cn.iocoder.yudao.framework.common.util.object.BeanUtils.toBean(policies.selectList().get(0),cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyOperationDO.class);policy.setId(null);policy.setOperationId("dcc.project-product.resubmit");policies.insert(policy);
  var payload=input("RESUBMIT");payload.setResubmissionReason("按旧意见重新申请");actor(9L);Long nextId=service.resubmitRejectedRequest(9L,oldId,payload);
  assertNotEquals(oldId,nextId);var next=requests.selectById(nextId);assertEquals(DccProjectReviewerRoleService.CONTRACT,next.getReviewContract());assertNull(next.getConfiguredReviewerUserId());assertEquals(oldId,next.getPreviousRequestId());
  assertEquals(oldBefore,cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(requests.selectById(oldId)));
 }
}
