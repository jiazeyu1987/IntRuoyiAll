package cn.iocoder.yudao.module.dcc.service.projectcode;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.productcreate.DccProjectProductCreateReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.productcatalog.DccProductCatalogMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.folder.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.*;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.*;
import org.springframework.test.context.jdbc.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 实际服务/事务/H2统一账本，只有外部账号目录及授权端口替身。 */
@Import({DccProjectReviewerConfigurationService.class,DccProjectProductCreateServiceImpl.class,DccProjectProductCreateWriteService.class,
        DccProjectProductCreateStateService.class,DccProjectProductCreateFailureService.class,
        DccProjectAttributesService.class,DccProjectLeaderService.class,DccFolderTemplateService.class,
        DccProjectConfigurationAuditService.class,DccProjectProductAuditService.class,GxpAuditServiceImpl.class})
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(scripts="/sql/dcc_b_gxp_audit_tables.sql",executionPhase=Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts="/sql/dcc_b_gxp_audit_clean.sql",executionPhase=Sql.ExecutionPhase.AFTER_TEST_METHOD)
class DccProjectProductLedgerTest extends BaseDbUnitTest {
    @Resource javax.sql.DataSource reviewerDataSource;
    @org.junit.jupiter.api.BeforeEach void configuredReviewerFixture() {
        new org.springframework.jdbc.core.JdbcTemplate(reviewerDataSource).update("INSERT INTO dcc_project_reviewer_config(tenant_id,reviewer_user_id,reviewer_username,reviewer_nickname,version_no,updated_by,change_reason) VALUES(1,1,'admin','显式配置测试审核人',1,1,'测试正式配置')");
    }

    @Resource DccProjectProductCreateServiceImpl service;
    @Resource DccProjectProductCreateStateService state;
    @Resource DccProjectProductCreateWriteService writer;
    @Resource DccProjectProductCreateFailureService failures;
    @Resource DccProjectProductCreateRequestMapper requests;
    @Resource DccProjectProductIdentityClaimMapper claims;
    @Resource DccProjectCodeMapper projects;
    @Resource DccProductCatalogMapper products;
    @Resource DccProjectProductRelationMapper relations;
    @Resource DccFolderTemplateMapper templates;
    @MockitoSpyBean DccProjectFolderMapper folders;
    @Resource GxpAuditPolicyOperationMapper policies;
    @Resource GxpAuditEventMapper events;
    @MockitoSpyBean GxpAuditServiceImpl ledger;
    @MockitoBean AdminUserApi users;
    @MockitoBean PermissionApi permissions;
    @MockitoBean DccProjectAccessService access;
    @BeforeEach void accounts() {
        var leader=new AdminUserRespDTO();leader.setId(7L);leader.setStatus(0);leader.setNickname("正式负责人");
        var existingReviewer=new AdminUserRespDTO();existingReviewer.setId(1L);existingReviewer.setStatus(0);existingReviewer.setUsername("admin");existingReviewer.setTenantId(1L);existingReviewer.setNickname("显式配置测试审核人");
        when(users.getUser(7L)).thenReturn(leader);when(users.getUser(1L)).thenReturn(existingReviewer);
        when(users.getUser(9L)).thenReturn(new AdminUserRespDTO().setId(9L).setTenantId(1L).setStatus(0)
                .setUsername("actual-applicant-9").setNickname("正式账号9"));
        login(9L);
    }
    @AfterEach void clear() {SecurityContextHolder.clearContext();}
    void login(Long id) {
        var actor=new LoginUser();actor.setId(id);actor.setTenantId(1L);actor.setUserType(2);
        actor.setInfo(Map.of(LoginUser.INFO_KEY_NICKNAME,"正式账号"+id,LoginUser.INFO_KEY_DEPT_ID,"9"));
        SecurityFrameworkUtils.setLoginUser(actor,new MockHttpServletRequest());
    }
    void policy(String operation) {
        var row=new GxpAuditPolicyOperationDO();row.setTenantId(1L);row.setOperationId(operation);row.setPolicyVersion("B-REV-01-ISOLATED");
        row.setSourceType("SERVICE_METHOD");row.setSourceLocator("ISOLATED_TEST");row.setDomain("DCC");row.setSubjectType("DCC_PROJECT_PRODUCT_REQUEST");
        row.setActionType("UPDATE");row.setReasonPolicy("REQUIRED");row.setSignaturePolicy("NOT_REQUIRED");row.setStatePolicy("BEFORE_AFTER");
        row.setRetentionClass("ISOLATED_TEST");row.setTestIds("B-REV-01");row.setOwner("B-test");row.setApplicability("GXP");row.setActive(true);
        policies.insert(row);
    }
    DccProjectProductCreateReqVO input() {
        var template=new DccFolderTemplateDO();template.setTenantId(1L);template.setName("冻结来源模板");template.setActive(true);
        template.setEverUsed(false);template.setEditedByUserId(7L);template.setStructureJson(JsonUtils.toJsonString(new DccFolderTemplateStructure(
                List.of(new DccFolderTemplateStructure.Node("root",null,"来源根目录",0)))));templates.insert(template);
        var input=new DccProjectProductCreateReqVO();input.setProjectName("账本项目");input.setProjectCode("REV01-P");input.setProjectLeaderUserId(7L);
        input.setDefaultAttributes(new DccProjectAttributes(List.of("CE"),null,"Y","N","Y","目标甲"));input.setFolderTemplateId(template.getId());
        input.setProductCode("REV01-PR");input.setProductName("账本产品");input.setClassification("一类");input.setCreationReason("正式新建申请原因");input.setRemark("产品备注");
        input.setResubmissionReason("独立的重提原因");
        return input;
    }
    @Test void createPersistsCompleteRequestAndMatchingUnifiedLedgerEvent() {
        policy("dcc.project-product.create");var input=input();
        Long id=service.createRequest(9L,input);
        var recorded=events.selectList();assertEquals(1,recorded.size(),"新建不能仅写状态/模板使用标记而漏统一账本");
        var event=recorded.get(0);assertEquals("dcc.project-product.create",event.getOperationId());
        assertEquals("DCC_PROJECT_PRODUCT_REQUEST:"+id,event.getSubjectId());assertEquals(9L,event.getActorId());
        assertEquals("actual-applicant-9",event.getActorUsername());assertEquals("正式账号9",event.getActorDisplayName());
        assertEquals("PENDING_REVIEW",event.getAfterState());assertEquals("ABSENT",event.getBeforeState());
        assertTrue(event.getAfterStateJson().contains("目标甲"));assertTrue(event.getAfterStateJson().contains("来源根目录"));
        assertEquals(3,claims.selectList().size());assertTrue(templates.selectById(input.getFolderTemplateId()).getEverUsed());
        assertEquals("正式新建申请原因",event.getReason());assertEquals("正式新建申请原因",requests.selectById(id).getCreationReason());
        assertEquals("1",event.getSubjectVersion());assertNotNull(event.getEventHash());
        assertEquals(3,((List<?>)JsonUtils.parseObject(event.getAfterStateJson(),Map.class).get("identityClaims")).size());
    }
    @Test void createWithoutLedgerPolicyCannotLeaveRequestClaimsOrTemplateUsage() {
        var input=input();assertThrows(RuntimeException.class,()->service.createRequest(9L,input));
        assertTrue(requests.selectList().isEmpty());assertTrue(claims.selectList().isEmpty());assertTrue(events.selectList().isEmpty());
        assertFalse(templates.selectById(input.getFolderTemplateId()).getEverUsed());
    }
    @org.junit.jupiter.params.ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(booleans={true,false})
    void reviewDecisionHasRealBeforeAfterAndLedgerFailureRollsBack(boolean approve) {
        policy("dcc.project-product.create");policy("dcc.project-product.review");
        Long id=service.createRequest(9L,input());login(1L);
        service.review(1L,id,"本次审核说明",approve);
        var event=events.selectList().stream().filter(row->"dcc.project-product.review".equals(row.getOperationId())).findFirst().orElseThrow();
        assertEquals("PENDING_REVIEW",event.getBeforeState());assertEquals(approve?"PENDING_APPROVAL":"REJECTED",event.getAfterState());
        assertEquals(1L,event.getActorId());assertEquals("本次审核说明",event.getReason());
        assertEquals(approve?3:0,claims.selectList().size());
    }
    @org.junit.jupiter.params.ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(booleans={true,false})
    void approvalDecisionHasOwnEventBeforeAssetWriting(boolean approve) {
        policy("dcc.project-product.create");policy("dcc.project-product.review");policy("dcc.project-product.approve");
        Long id=service.createRequest(9L,input());login(1L);service.review(1L,id,"审核说明",true);
        state.markApprovalDecision(1L,id,"本次批准说明",approve);
        var event=events.selectList().stream().filter(row->"dcc.project-product.approve".equals(row.getOperationId())).findFirst().orElseThrow();
        assertEquals("PENDING_APPROVAL",event.getBeforeState());assertEquals(approve?"WRITING":"REJECTED",event.getAfterState());
        assertEquals("本次批准说明",event.getReason());assertTrue(projects.selectList().isEmpty());
    }
    @Test void completedWriteEventContainsOnlyActuallyPersistedAssets() {
        for(String op:List.of("create","review","approve","complete"))policy("dcc.project-product."+op);
        Long id=service.createRequest(9L,input());login(1L);service.review(1L,id,"审核说明",true);
        state.markApprovalDecision(1L,id,"实际批准写入原因",true);writer.writeApprovedRequest(id);
        var event=events.selectList().stream().filter(row->"dcc.project-product.complete".equals(row.getOperationId())).findFirst().orElseThrow();
        assertEquals("WRITING",event.getBeforeState());assertEquals("COMPLETED",event.getAfterState());
        assertEquals("实际批准写入原因",event.getReason());
        assertEquals(1,projects.selectList().size());assertEquals(1,products.selectList().size());assertEquals(1,relations.selectList().size());
        assertTrue(event.getAfterStateJson().contains("generatedProject"));assertTrue(event.getAfterStateJson().contains("projectFolders"));
    }
    @Test void failureAndRetryHaveTheirOwnActualStateEvents() {
        for(String op:List.of("create","review","approve","write-failed","retry"))policy("dcc.project-product."+op);
        Long id=service.createRequest(9L,input());login(1L);service.review(1L,id,"审核说明",true);
        state.markApprovalDecision(1L,id,"实际写入意图",true);failures.markWriteFailed(id,"TEST_ERROR","实际目录失败");
        state.markRetryWriting(1L,id,"本次明确重试原因");
        var failure=events.selectList().stream().filter(row->"dcc.project-product.write-failed".equals(row.getOperationId())).findFirst().orElseThrow();
        var retry=events.selectList().stream().filter(row->"dcc.project-product.retry".equals(row.getOperationId())).findFirst().orElseThrow();
        assertEquals("WRITE_FAILED",failure.getAfterState());assertTrue(failure.getAfterStateJson().contains("实际目录失败"));
        assertEquals("WRITE_FAILED",retry.getBeforeState());assertEquals("WRITING",retry.getAfterState());assertTrue(projects.selectList().isEmpty());
        assertEquals("本次明确重试原因",retry.getReason());assertEquals("2",retry.getSubjectVersion());
        assertEquals("实际写入意图",failure.getReason());assertEquals("1",failure.getSubjectVersion());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"create","review","approve","complete","write-failed","retry"})
    void missingPolicyRollsBackExactlyTheAffectedTransaction(String action) { assertAuditRollback(action,true); }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"create","review","approve","complete","write-failed","retry"})
    void actualLedgerAppendFailureRollsBackExactlyTheAffectedTransaction(String action) { assertAuditRollback(action,false); }
    void assertAuditRollback(String action,boolean missingPolicy) {
        for(String op:List.of("create","review","approve","complete","write-failed","retry")) {
            if(!missingPolicy || !op.equals(action)) policy("dcc.project-product."+op);
        }
        if(!missingPolicy) doThrow(new IllegalStateException("统一账本实际append失败"))
                .when(ledger).append(argThat(command->command!=null && ("dcc.project-product."+action).equals(command.getOperationId())));
        var input=input();
        if(action.equals("create")) {
            assertThrows(RuntimeException.class,()->service.createRequest(9L,input));
            assertTrue(requests.selectList().isEmpty());assertTrue(claims.selectList().isEmpty());assertTrue(events.selectList().isEmpty());
            assertFalse(templates.selectById(input.getFolderTemplateId()).getEverUsed());return;
        }
        Long id=service.createRequest(9L,input);login(1L);
        if(!action.equals("review")) service.review(1L,id,"正式审核原因",true);
        if(List.of("complete","write-failed","retry").contains(action)) state.markApprovalDecision(1L,id,"正式批准写入原因",true);
        if(action.equals("retry")) failures.markWriteFailed(id,"ACTUAL_ERROR","已记录实际失败");
        String before=JsonUtils.toJsonString(requests.selectById(id));
        String claimsBefore=JsonUtils.toJsonString(claims.selectList());int eventCount=events.selectList().size();
        assertThrows(RuntimeException.class,()->{
            switch(action) {
                case "review" -> service.review(1L,id,"拒绝审核原因",false);
                case "approve" -> state.markApprovalDecision(1L,id,"拒绝批准原因",false);
                case "complete" -> writer.writeApprovedRequest(id);
                case "write-failed" -> failures.markWriteFailed(id,"ACTUAL_ERROR","真实失败");
                case "retry" -> state.markRetryWriting(1L,id,"本轮实际重试原因");
                default -> throw new AssertionError(action);
            }
        });
        assertEquals(before,JsonUtils.toJsonString(requests.selectById(id)),"该事务全部回滚，原快照/状态不被改写");
        assertEquals(claimsBefore,JsonUtils.toJsonString(claims.selectList()));assertEquals(eventCount,events.selectList().size());
        assertTrue(projects.selectList().isEmpty());assertTrue(products.selectList().isEmpty());assertTrue(relations.selectList().isEmpty());assertTrue(folders.selectList().isEmpty());
    }
    @Test void failedAssetsRecordOnlyWriteFailedAndSuccessfulRetryUsesNewIntentAndAttempt() {
        for(String op:List.of("create","review","approve","complete","write-failed","retry"))policy("dcc.project-product."+op);
        Long id=service.createRequest(9L,input());login(1L);service.review(1L,id,"审核批准资料",true);
        doThrow(new IllegalStateException("目录实际创建失败")).when(folders).insert(any(DccProjectFolderDO.class));
        assertThrows(RuntimeException.class,()->service.approve(1L,id,"批准实际资产创建",true));
        assertEquals("WRITE_FAILED",requests.selectById(id).getStatus());assertTrue(projects.selectList().isEmpty());
        assertEquals(0,events.selectList().stream().filter(event->"dcc.project-product.complete".equals(event.getOperationId())).count());
        var failure=events.selectList().stream().filter(event->"dcc.project-product.write-failed".equals(event.getOperationId())).findFirst().orElseThrow();
        assertEquals("WRITE_FAILED",failure.getAfterState());assertEquals("批准实际资产创建",failure.getReason());
        assertTrue(failure.getAfterStateJson().contains("目录实际创建失败"));assertFalse(failure.getAfterStateJson().contains("generatedProject\""));
        reset(folders);
        var completed=service.retryWrite(1L,id,"目录故障修复后本次重试");
        assertEquals("COMPLETED",completed.getStatus());assertEquals(2,completed.getWriteAttemptNo());assertNull(completed.getWriteErrorCode());
        var complete=events.selectList().stream().filter(event->"dcc.project-product.complete".equals(event.getOperationId())).findFirst().orElseThrow();
        assertEquals("2",complete.getSubjectVersion());assertEquals("目录故障修复后本次重试",complete.getReason());
        assertEquals(6,events.selectList().size());
        assertTrue(failure.getAfterStateJson().contains("目录实际创建失败"),"历史失败事件不能被成功重试覆盖");
    }
    @Test void blankCreationReasonCannotBorrowRemarkOrResubmissionReasonAndResubmissionDoesNotCreateInitialEvent() {
        for(String op:List.of("create","review","resubmit"))policy("dcc.project-product."+op);
        var input=input();input.setCreationReason(" ");
        assertThrows(RuntimeException.class,()->service.createRequest(9L,input));assertTrue(requests.selectList().isEmpty());
        input.setCreationReason("首次申请明确原因");Long id=service.createRequest(9L,input());
        login(1L);service.review(1L,id,"原审核驳回原因",false);
        String original=JsonUtils.toJsonString(requests.selectById(id));login(9L);
        input.setCreationReason(null);input.setResubmissionReason("仅本次重提意图");
        Long next=service.resubmitRejectedRequest(9L,id,input);
        assertEquals(id,requests.selectById(next).getPreviousRequestId());assertNull(requests.selectById(next).getCreationReason());
        assertEquals(original,JsonUtils.toJsonString(requests.selectById(id)));
        assertEquals(1,events.selectList().stream().filter(event->"dcc.project-product.create".equals(event.getOperationId())).count());
        var resubmit=events.selectList().stream().filter(event->"dcc.project-product.resubmit".equals(event.getOperationId())).findFirst().orElseThrow();
        assertEquals("仅本次重提意图",resubmit.getReason());assertTrue(resubmit.getAfterStateJson().contains("rejectedPredecessor"));
    }
    @Test void rejectedIllegalOrBlankActionsLeaveNoDecisionEventOrPartialState() {
        for(String op:List.of("create","review","approve","write-failed","retry"))policy("dcc.project-product."+op);
        Long id=service.createRequest(9L,input());login(1L);
        String original=JsonUtils.toJsonString(requests.selectById(id));
        assertThrows(RuntimeException.class,()->service.review(7L,id,"无审核资格",true));
        assertThrows(RuntimeException.class,()->service.review(1L,id," ",true));
        assertThrows(RuntimeException.class,()->service.approve(1L,id,"错阶段批准",true));
        assertEquals(1,events.selectList().size());assertEquals(original,JsonUtils.toJsonString(requests.selectById(id)));
        service.review(1L,id,"审核说明",true);state.markApprovalDecision(1L,id,"批准意图",true);
        failures.markWriteFailed(id,"ACTUAL_ERROR","实际失败");
        var failed=requests.selectById(id);String before=JsonUtils.toJsonString(failed);int eventCount=events.selectList().size();
        assertThrows(RuntimeException.class,()->service.retryWrite(1L,id," "));
        assertEquals(before,JsonUtils.toJsonString(requests.selectById(id)));assertEquals(eventCount,events.selectList().size());
    }
    @Test void completionLedgerFailureAfterApprovalProducesOnlyActualFailedAttemptAndCanRetry() {
        for(String op:List.of("create","review","approve","write-failed","retry"))policy("dcc.project-product."+op);
        Long id=service.createRequest(9L,input());login(1L);service.review(1L,id,"审核说明",true);
        assertThrows(RuntimeException.class,()->service.approve(1L,id,"批准后尝试正式写入",true));
        assertEquals("WRITE_FAILED",requests.selectById(id).getStatus());
        assertTrue(projects.selectList().isEmpty());assertTrue(products.selectList().isEmpty());assertTrue(relations.selectList().isEmpty());assertTrue(folders.selectList().isEmpty());
        assertEquals(4,events.selectList().size());
        assertEquals(0,events.selectList().stream().filter(event->"dcc.project-product.complete".equals(event.getOperationId())).count());
        policy("dcc.project-product.complete");service.retryWrite(1L,id,"补齐完成账本策略后本次重试");
        assertEquals("COMPLETED",requests.selectById(id).getStatus());assertEquals(6,events.selectList().size());
    }
    @Test void repeatedFailedAttemptsHaveDistinctStableVersionsAndNeverRewriteEarlierFailures() {
        for(String op:List.of("create","review","approve","complete","write-failed","retry"))policy("dcc.project-product."+op);
        Long id=service.createRequest(9L,input());login(1L);service.review(1L,id,"审核说明",true);
        doThrow(new IllegalStateException("目录连续失败")).when(folders).insert(any(DccProjectFolderDO.class));
        assertThrows(RuntimeException.class,()->service.approve(1L,id,"首次批准写入",true));
        var first=events.selectList().stream().filter(event->"dcc.project-product.write-failed".equals(event.getOperationId())).findFirst().orElseThrow();
        String history=first.getCanonicalEventJson();
        assertThrows(RuntimeException.class,()->service.retryWrite(1L,id,"第一次独立重试"));
        assertEquals(2,requests.selectById(id).getWriteAttemptNo());
        assertEquals(2,events.selectList().stream().filter(event->"dcc.project-product.write-failed".equals(event.getOperationId())).count());
        reset(folders);service.retryWrite(1L,id,"第二次独立重试");assertEquals(3,requests.selectById(id).getWriteAttemptNo());
        var complete=events.selectList().stream().filter(event->"dcc.project-product.complete".equals(event.getOperationId())).findFirst().orElseThrow();
        assertEquals("3",complete.getSubjectVersion());assertEquals("第二次独立重试",complete.getReason());
        assertEquals(history,events.selectById(first.getId()).getCanonicalEventJson());
        assertEquals(events.selectList().size(),events.selectList().stream().map(GxpAuditEventDO::getIdempotencyKey).distinct().count());
    }
}
