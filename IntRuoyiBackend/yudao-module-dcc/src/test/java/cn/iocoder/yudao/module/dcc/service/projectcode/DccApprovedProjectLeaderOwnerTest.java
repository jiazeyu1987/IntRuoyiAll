package cn.iocoder.yudao.module.dcc.service.projectcode;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.productcatalog.DccProductCatalogMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.folder.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.*;
import cn.iocoder.yudao.module.system.api.user.*;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.api.permission.*;
import cn.iocoder.yudao.module.system.api.dept.*;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.bean.override.mockito.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real writer/access/reader/Gxp kernel on isolated H2; formal user/permission directories are ports. */
@Import({DccProjectProductCreateWriteService.class,DccProjectAccessServiceImpl.class,DccProjectLeaderService.class,
 DccProjectAttributesService.class,DccFolderTemplateService.class,DccProjectProductAuditService.class,DccProjectConfigurationAuditService.class,GxpAuditServiceImpl.class,DccApprovedProjectLeaderOwnerTest.Beans.class})
@Sql(scripts="/sql/dcc_b_gxp_audit_tables.sql",executionPhase=Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts="/sql/dcc_b_gxp_audit_clean.sql",executionPhase=Sql.ExecutionPhase.AFTER_TEST_METHOD)
class DccApprovedProjectLeaderOwnerTest extends BaseDbUnitTest {
 @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods=false)
 static class Beans {@Bean JdbcTemplate jdbcTemplate(javax.sql.DataSource source){return new JdbcTemplate(source);}}
 @Resource DccProjectProductCreateWriteService writer;
 @Resource DccProjectAccessService access;
 @Resource DccFolderTemplateService folders;
 @Resource DccProjectAttributesService attributes;
 @MockitoSpyBean DccProjectProductCreateRequestMapper requests;
 @Resource DccProjectCodeMapper projects;
 @Resource DccProductCatalogMapper products;
 @MockitoSpyBean DccProjectProductRelationMapper relations;
 @MockitoSpyBean DccProjectFolderMapper folderRows;
 @Resource DccFolderTemplateMapper templates;
 @MockitoSpyBean DccProjectAccessRuleMapper accessRules;
 @MockitoSpyBean GxpAuditServiceImpl actualAuditKernel;
 @Resource GxpAuditPolicyOperationMapper policies;
 @Resource GxpAuditEventMapper events;
 @Resource JdbcTemplate jdbc;
 @MockitoBean AdminUserApi users;
 @MockitoBean PermissionApi permissions;
 @MockitoBean DeptApi departments;
 @MockitoBean PostApi posts;
 @MockitoBean RoleApi roles;
 AdminUserRespDTO leader;
 @BeforeEach void fixture(){
  TenantContextHolder.setTenantId(1L);leader=new AdminUserRespDTO().setId(7L).setTenantId(1L).setStatus(0).setUsername("actual-leader").setNickname("项目负责人");when(users.getUser(7L)).thenReturn(leader);
  for(long id:new long[]{1,8,9})when(users.getUser(id)).thenReturn(new AdminUserRespDTO().setId(id).setTenantId(1L).setStatus(0).setUsername("other-"+id).setNickname("其它账号"));
  for(long id:new long[]{1,7,8,9})when(permissions.getUserRoleIdListByUserId(id)).thenReturn(Set.of());
  var login=new LoginUser().setId(1L).setTenantId(1L).setUserType(2).setInfo(Map.of("username","actual-approver","nickname","批准人"));SecurityFrameworkUtils.setLoginUser(login,new org.springframework.mock.web.MockHttpServletRequest());
  var policy=new cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyOperationDO();policy.setTenantId(1L);policy.setPolicyVersion("isolated-owner-test");policy.setOperationId("dcc.project-product.complete");policy.setSourceType("SERVICE_METHOD");policy.setSourceLocator("DccProjectProductCreateWriteService.writeApprovedRequest");policy.setDomain("DCC");policy.setSubjectType("DCC_PROJECT_PRODUCT_REQUEST");policy.setActionType("COMPLETE");policy.setReasonPolicy("REQUIRED");policy.setSignaturePolicy("NOT_REQUIRED");policy.setStatePolicy("BEFORE_AFTER");policy.setRetentionClass("ISOLATED_TEST");policy.setTestIds("OWNER-CREATE");policy.setOwner("dcc");policy.setApplicability("GXP");policy.setActive(true);policies.insert(policy);
 }
 @AfterEach void clear(){org.springframework.security.core.context.SecurityContextHolder.clearContext();}
 Long approvedRequest(){
  var structure=new DccFolderTemplateStructure(List.of(new DccFolderTemplateStructure.Node("root",null,"正式目录",0)));
  var template=new DccFolderTemplateDO();template.setTenantId(1L);template.setName("正式模板");template.setActive(true);template.setEverUsed(true);template.setEditedByUserId(7L);template.setStructureJson(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(structure));templates.insert(template);
  var req=DccProjectProductCreateRequestDO.builder().projectName("新项目").projectCode("OWNER-P1").projectLeader("项目负责人").projectLeaderUserId(7L).defaultAttributesJson(attributes.encode(new DccProjectAttributes(List.of("NMPA"),null,"Y","N","N",null))).folderTemplateId(template.getId()).folderTemplateSnapshotJson(template.getStructureJson()).productCode("OWNER-PRODUCT1").productName("负责人产品").classification("一类").status("WRITING").applicantUserId(9L).reviewerUserId(8L).approverUserId(1L).writeAttemptNo(1).writeReason("批准创建项目和初始负责人授权").writeOperatorUserId(1L).build();req.setTenantId(1L);requests.insert(req);return req.getId();
 }
 @Test void approvedProjectLeaderCanReadNewProjectAndFoldersThroughRealFormalAccess(){
  var completed=writer.writeApprovedRequest(approvedRequest());var projectId=completed.getGeneratedProjectCodeId();
  var read=folders.readProjectFolders(7L,projectId);assertEquals(1,read.size());assertEquals("正式目录",read.get(0).getName());assertTrue(access.listReadableProjectIds(7L).contains(projectId));
  var rules=accessRules.selectListByProjectCodeId(projectId);assertEquals(1,rules.size());assertEquals("USER",rules.get(0).getSubjectType());assertEquals("OWNER",rules.get(0).getAccessLevel());assertEquals(7L,rules.get(0).getSubjectId());assertEquals(1L,rules.get(0).getTenantId());assertTrue(rules.get(0).getActive());
  for(long other:new long[]{1,8,9}){assertThrows(RuntimeException.class,()->folders.readProjectFolders(other,projectId));assertFalse(access.listReadableProjectIds(other).contains(projectId));}
  assertEquals(1,events.selectList().size());assertEquals("COMPLETED",requests.selectById(completed.getId()).getStatus());
  assertTrue(events.selectList().get(0).getAfterStateJson().contains("projectAccessRules"));
  assertTrue(events.selectList().get(0).getAfterStateJson().contains("OWNER"));
 }
 void noGeneratedAssets(Long requestId){
  assertTrue(projects.selectList().isEmpty());assertTrue(products.selectList().isEmpty());assertTrue(relations.selectList().isEmpty());assertTrue(folderRows.selectList().isEmpty());assertTrue(accessRules.selectList().isEmpty());assertTrue(events.selectList().isEmpty());
  var request=requests.selectById(requestId);assertEquals("WRITING",request.getStatus());assertNull(request.getGeneratedProjectCodeId());assertNull(request.getCompletedTime());
 }
 @Test void actualAuditAppendFailureRollsBackProjectOwnerAndEveryGeneratedAsset(){
  Long id=approvedRequest();doThrow(new IllegalStateException("isolated actual append failure")).when(actualAuditKernel).append(any());
  assertThrows(RuntimeException.class,()->writer.writeApprovedRequest(id));noGeneratedAssets(id);
 }
 @Test void folderFailureAfterOwnerInsertRollsBackOwnerAndProjectInTheSameTransaction(){
  Long id=approvedRequest();doThrow(new IllegalStateException("isolated folder failure")).when(folderRows).insert(any(DccProjectFolderDO.class));
  assertThrows(RuntimeException.class,()->writer.writeApprovedRequest(id));noGeneratedAssets(id);
 }
 @Test void zeroInitialOwnerWriteCannotLeaveAnUnreadableCompletedProject(){
  Long id=approvedRequest();doReturn(0).when(accessRules).insert(any(DccProjectAccessRuleDO.class));
  assertThrows(RuntimeException.class,()->writer.writeApprovedRequest(id));noGeneratedAssets(id);
 }
 @Test void lateRelationAndCompletionFailureRollBackOwnerWithAllAssets(){
  Long id=approvedRequest();doReturn(0).when(relations).insert(any(DccProjectProductRelationDO.class));
  assertThrows(RuntimeException.class,()->writer.writeApprovedRequest(id));noGeneratedAssets(id);
  reset(relations);doReturn(0).when(requests).updateById(any(DccProjectProductCreateRequestDO.class));
  assertThrows(RuntimeException.class,()->writer.writeApprovedRequest(id));noGeneratedAssets(id);
 }
 @Test void currentDisabledOrForeignTenantLeaderNeverReceivesAnInitialRule(){
  Long id=approvedRequest();leader.setStatus(1);assertThrows(RuntimeException.class,()->writer.writeApprovedRequest(id));noGeneratedAssets(id);
  leader.setStatus(0);leader.setTenantId(2L);assertThrows(RuntimeException.class,()->writer.writeApprovedRequest(id));noGeneratedAssets(id);
 }
 @Test void completedRequestReplayRejectsWithoutReinitializingLaterFormalPermissions(){
  var completed=writer.writeApprovedRequest(approvedRequest());Long project=completed.getGeneratedProjectCodeId();
  var rule=accessRules.selectListByProjectCodeId(project).get(0);rule.setAccessLevel("VIEW");rule.setChangeReason("后续正式访问调整");accessRules.updateById(rule);
  String before=cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(accessRules.selectListByProjectCodeId(project));int auditCount=events.selectList().size();
  assertThrows(RuntimeException.class,()->writer.writeApprovedRequest(completed.getId()));
  assertEquals(before,cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(accessRules.selectListByProjectCodeId(project)));assertEquals(auditCount,events.selectList().size());assertEquals(1,projects.selectList().size());
 }
}
