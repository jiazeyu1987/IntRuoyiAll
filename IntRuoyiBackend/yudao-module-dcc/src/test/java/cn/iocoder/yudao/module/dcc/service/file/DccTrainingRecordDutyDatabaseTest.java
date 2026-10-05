package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.file.DccControlledFileController;
import cn.iocoder.yudao.module.dcc.controller.admin.file.DccControlledFileWorkflowController;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTrainingRecordReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileUploadPreviewReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryPermissionRuleDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryPermissionRuleMapper;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketCreateCommand;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketServiceImpl;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.flowable.engine.ProcessEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Actual category rules distinguish doc control training work from the existing content approval user. */
class DccTrainingRecordDutyDatabaseTest extends DccOfflineTrainingEntryDatabaseTest {
    @Resource DccFileCategoryPermissionRuleMapper rules;
    @Resource DccFileCategoryMapper categoryRows;
    @Resource cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileTemporaryFileMapper ticketRows;
    private DccControlledFileCategoryPermissionSupport actualCategories;
    private DccOfflineTrainingRecordService actualTraining;
    private DccUploadTicketServiceImpl tickets;
    private DccControlledFileController previewController;
    private DccControlledFileWorkflowController uploadController;
    private ProcessEngine actualEngine;
    private String session;
    private static final byte[] CONTENT="%PDF-1.4\nActual isolated offline training record\n%%EOF".getBytes(StandardCharsets.UTF_8);

    @BeforeEach
    void independentTrainingDuty() throws Exception {
        actualTraining=read("training",DccOfflineTrainingRecordService.class);
        AdminUserApi accounts=read("users",AdminUserApi.class);
        PermissionApi permissions=read("permissions",PermissionApi.class);
        actualEngine=read("engine",ProcessEngine.class);
        when(accounts.getUser(1L)).thenReturn(new AdminUserRespDTO().setId(1L).setTenantId(1L)
                .setUsername("original-approver").setNickname("内容批准人").setStatus(0));
        when(permissions.hasAnyRoles(88L,"doc_control")).thenReturn(true);
        actualCategories=new DccControlledFileCategoryPermissionSupport();
        wire(actualCategories,"permissionRuleMapper",rules,"adminUserApi",accounts,"permissionApi",permissions,
                "deptApi",mock(cn.iocoder.yudao.module.system.api.dept.DeptApi.class));
        rules.insert(DccFileCategoryPermissionRuleDO.builder().categoryId(2L).actionType("TRAINING_RECORD")
                .subjectType("USER").subjectId(88L).scopeType("GLOBAL").active(true).build());
        rules.insert(DccFileCategoryPermissionRuleDO.builder().categoryId(2L).actionType("APPROVE")
                .subjectType("USER").subjectId(1L).scopeType("GLOBAL").active(true).build());
        jdbc.update("INSERT INTO dcc_file_category(id,code,name,active,source,lifecycle_stage,tenant_id) VALUES(2,'DOC-TRAIN','真实培训类别',1,'LOCAL','OUTPUT',1)");
        wire(actualTraining,"categories",actualCategories);
        wire(workflow,"categoryPermissionSupport",actualCategories,"permissionApi",permissions);
        var actor=new LoginUser();actor.setId(88L);actor.setTenantId(1L);actor.setUserType(2);
        actor.setInfo(Map.of("username","doc-control-88",LoginUser.INFO_KEY_NICKNAME,"独立文控"));
        SecurityFrameworkUtils.setLoginUser(actor,new MockHttpServletRequest());
        String round=files.selectById(20L).getProcessInstanceId();
        session="dcc-training:20:"+round.length()+":"+round+":isolated-duty";
        var physical=mock(cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper.class);
        when(physical.selectById(111L)).thenReturn(FileDO.builder().id(111L).name("OfflineTraining.pdf")
                .type("application/pdf").size((long)CONTENT.length).build());
        tickets=spy(new DccUploadTicketServiceImpl());wire(tickets,"temporaryFileMapper",ticketRows,"fileMapper",physical);
        wire(workflow,"uploadTicketService",tickets);
        var tasks=mock(cn.iocoder.yudao.module.bpm.service.task.BpmTaskService.class);
        when(tasks.triggerTask(round,"TRAINING")).thenAnswer(call->{
            actualEngine.getRuntimeService().trigger(actualEngine.getRuntimeService().createExecutionQuery()
                    .processInstanceId(round).activityId("TRAINING").singleResult().getId());return true;
        });wire(workflow,"bpmTaskService",tasks);
        var upload=new DccControlledFileUploadServiceImpl();
        var storage=mock(cn.iocoder.yudao.module.infra.service.file.FileService.class);
        when(storage.createFileAndReturnId(any(),eq("OfflineTraining.pdf"),anyString(),eq("application/pdf"))).thenReturn(111L);
        var ownership=mock(DccControlledFileSourceOwnershipService.class);
        when(ownership.rollbackCleanup(any())).thenReturn(()->{
            try {storage.deleteFile(111L);}
            catch(Exception failure){throw new IllegalStateException("ISOLATED_PREVIEW_CLEANUP_FAILURE",failure);}
        });
        wire(upload,"categoryMapper",categoryRows,"permissionSupport",actualCategories,"permissionApi",permissions,
                "workflowService",workflow,"uploadSizePolicyService",mock(cn.iocoder.yudao.module.dcc.service.upload.DccUploadSizePolicyService.class),
                "uploadTicketService",tickets,"fileMapper",physical,"fileService",storage,
                "sourceOwnershipService",ownership,
                "watermarkService",mock(DccControlledPreviewWatermarkService.class),
                "accessAuditService",mock(cn.iocoder.yudao.module.dcc.service.audit.DccControlledFileAccessAuditService.class));
        previewController=new DccControlledFileController();wire(previewController,"uploadService",upload);
        uploadController=new DccControlledFileWorkflowController();wire(uploadController,"workflowService",workflow);
    }

    @AfterEach void clearOwnActor(){SecurityContextHolder.clearContext();}

    @Test
    void independentRecordDutyIsEligibleWithoutAnyContentApprovalRule() {
        assertFalse(actualCategories.hasCategoryPermission(2L,88L,DccFileCategoryPermissionActionEnum.APPROVE));
        assertTrue(actualCategories.hasCategoryPermission(2L,1L,DccFileCategoryPermissionActionEnum.APPROVE));
        assertTrue(actualTraining.canUpload(88L,files.selectById(20L)),"Independent training rule must not require becoming the content approver");
    }

    @Test
    void actualPublicPreviewAcceptsIndependentTrainingRuleWithoutContentApproval() throws Exception {
        var result=preview();
        assertNotNull(result.getUploadTicket());assertEquals("TRAINING_RECORD",result.getPurpose());
        assertEquals(1,ticketRows.selectList().size());assertFalse(actualCategories.hasCategoryPermission(2L,88L,DccFileCategoryPermissionActionEnum.APPROVE));
    }

    @Test
    void actualPublicTrainingPostUsesTheIndependentRuleAndTriggersTheRealReceiveExecution() {
        var created=tickets.createTicket(new DccUploadTicketCreateCommand(88L,2L,session,"TRAINING_RECORD",111L,
                "OfflineTraining.pdf","application/pdf",(long)CONTENT.length,CONTENT,"isolated-duty-post"));
        var request=new DccControlledFileTrainingRecordReqVO();request.setSessionId(session);request.setTrainingRecordUploadTicket(created.uploadTicket());
        Boolean uploaded=new TransactionTemplate(manager).execute(s->uploadController.uploadTrainingRecord(20L,request).getData());
        assertEquals(Boolean.TRUE,uploaded);
        assertEquals(111L,files.selectById(20L).getTrainingRecordFileId());
        assertEquals("PENDING_DOC_CONTROL_REVIEW",files.selectById(20L).getStatus());
        assertEquals(0,actualEngine.getRuntimeService().createExecutionQuery().activityId("TRAINING").count());
        assertEquals(1,actualEngine.getTaskService().createTaskQuery().taskDefinitionKey("DOC_CONTROL_REVIEW").count());
        assertFalse(actualCategories.hasCategoryPermission(2L,88L,DccFileCategoryPermissionActionEnum.APPROVE));
    }

    private DccControlledFileUploadPreviewReqVO previewRequest() {
        var request=new DccControlledFileUploadPreviewReqVO();request.setCategoryId(2L);request.setControlledFileId(20L);
        request.setPurpose("TRAINING_RECORD");request.setSessionId(session);
        request.setFiles(new MockMultipartFile[]{new MockMultipartFile("files","OfflineTraining.pdf","application/pdf",CONTENT)});
        return request;
    }

    private cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileUploadRespVO preview() {
        var http=new MockHttpServletRequest();http.addHeader("User-Agent","G58 isolated public upload test");
        return new TransactionTemplate(manager).execute(s->{
            try {return previewController.uploadPreviewFile(previewRequest(),http).getData();}
            catch(RuntimeException failure){throw failure;}
            catch(Exception failure){throw new IllegalStateException("ISOLATED_PUBLIC_PREVIEW_FAILURE",failure);}
        });
    }

    @Test
    void oldContentApprovalRuleCannotSubstituteForTheIndependentTrainingDuty() {
        jdbc.update("UPDATE dcc_file_category_permission_rule SET action_type='APPROVE' WHERE subject_id=88");
        assertTrue(actualCategories.hasCategoryPermission(2L,88L,DccFileCategoryPermissionActionEnum.APPROVE));
        assertFalse(actualTraining.canUpload(88L,files.selectById(20L)));assertTrue(actualTraining.listForActor(88L).isEmpty());
        assertEquals("DCC_OFFLINE_TRAINING_ACTOR_FORBIDDEN",assertThrows(IllegalStateException.class,this::preview).getMessage());
        var ticket=tickets.createTicket(new DccUploadTicketCreateCommand(88L,2L,session,"TRAINING_RECORD",111L,
                "OfflineTraining.pdf","application/pdf",(long)CONTENT.length,CONTENT,"isolated-forbidden-duty"));
        var request=new DccControlledFileTrainingRecordReqVO();request.setSessionId(session);request.setTrainingRecordUploadTicket(ticket.uploadTicket());
        assertEquals("DCC_OFFLINE_TRAINING_ACTOR_FORBIDDEN",assertThrows(IllegalStateException.class,()->
                new TransactionTemplate(manager).execute(s->uploadController.uploadTrainingRecord(20L,request))).getMessage());
        assertNull(files.selectById(20L).getTrainingRecordFileId());assertEquals("PENDING_APPLICANT_TRAINING_RECORD",files.selectById(20L).getStatus());
        assertEquals("AVAILABLE",ticketRows.selectList().get(0).getStatus());
        assertEquals(1,actualEngine.getRuntimeService().createExecutionQuery().activityId("TRAINING").count());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM system_notify_message",Integer.class));
    }

    @Test
    void formalCategoryRuleMaintenanceAllowsIndependentDutyAndPreservesMatrixOnlyApproval() {
        var service=new cn.iocoder.yudao.module.dcc.service.category.DccCategoryPermissionAdminServiceImpl();
        wire(service,"categoryMapper",categoryRows,"permissionRuleMapper",rules);
        var request=new cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryPermissionRuleSaveReqVO();
        request.setActionType("TRAINING_RECORD");request.setSubjectType("USER");request.setSubjectId(88L);request.setActive(true);request.setScopeType("GLOBAL");
        var before=jdbc.queryForMap("SELECT * FROM dcc_file_category_permission_rule WHERE action_type='APPROVE'");
        new TransactionTemplate(manager).executeWithoutResult(s->service.replacePermissionRules(2L,java.util.List.of(request)));
        assertEquals(before,jdbc.queryForMap("SELECT * FROM dcc_file_category_permission_rule WHERE action_type='APPROVE'"));
        assertTrue(actualTraining.canUpload(88L,files.selectById(20L)));
        request.setActionType("APPROVE");
        var failure=assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->
                new TransactionTemplate(manager).execute(s->service.replacePermissionRules(2L,java.util.List.of(request))));
        assertEquals(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CATEGORY_PERMISSION_RULE_MANUAL_REVIEW_APPROVE_FORBIDDEN.getCode(),failure.getCode());
    }

    @Test
    void actualTrainingRuleProducesOfficialWorkMessageWithoutContentApproverIdentity() {
        new TransactionTemplate(manager).executeWithoutResult(s->actualTraining.notifyWaiting(files.selectById(20L)));
        assertEquals(88L,((Number)jdbc.queryForMap("SELECT user_id FROM system_notify_message WHERE tenant_id=1").get("user_id")).longValue());
        assertEquals(1,actualTraining.listForActor(88L).size());
        assertFalse(actualCategories.hasCategoryPermission(2L,88L,DccFileCategoryPermissionActionEnum.APPROVE));
        assertEquals(99L,files.selectById(20L).getRequesterId());
    }

    private <T> T read(String field,Class<T> type){return type.cast(ReflectionTestUtils.getField(this,field));}
    private static void wire(Object target,Object... fields){for(int i=0;i<fields.length;i+=2)ReflectionTestUtils.setField(target,(String)fields[i],fields[i+1]);}
}
