package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangeRequestReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionArchiveDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionArchiveMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import({MesProEdhrBatchVoidEffectServiceImpl.class, MesEdhrBatchLifecycleGuard.class})
class MesProEdhrBatchVoidEffectServiceImplTest extends BaseDbUnitTest {

    @org.junit.jupiter.api.BeforeEach void tenant() { cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(0L); org.springframework.test.util.ReflectionTestUtils.setField(batchVoidEffectService,"workTaskService",workTaskService); }
    @org.junit.jupiter.api.AfterEach void clearTenant() { cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear(); }
    @Resource private cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper applications;
    @Resource private cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper realTasks;
    @Resource private cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrRecordChangeEventMapper changes;

    @MockitoBean private cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService gxpAuditService;
    @MockitoBean private cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper reviewMapper;

    private static final Long ACTOR_ID = 101L;
    private static final String HASH_64 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @Resource
    private MesProEdhrBatchVoidEffectService batchVoidEffectService;
    @Resource
    private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Resource
    private MesProEdhrBatchExecutionArchiveMapper batchArchiveMapper;

    @MockitoBean
    private AdminUserApi adminUserApi;
    @MockitoBean
    private MesProEdhrGoldenFingerPermissionService goldenFingerPermissionService;
    @MockitoBean
    private MesProEdhrWorkTaskService workTaskService;
    @MockitoBean
    private MesProEdhrBatchExecutionOriginMapper batchExecutionOriginMapper;
    @MockitoBean
    private MesProEdhrOperationAuditService operationAuditService;
    @MockitoBean
    private MesProBatchRecordExecutionSignatureService signatureService;

    @Test
    void executeDirectPlatformVoidBatchExecution_cancelsActiveWorkTasks() {
        MesProEdhrBatchExecutionDO batch = insertClosedBatchExecution();
        insertSealedBatchArchive(batch.getId());

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            batchVoidEffectService.executeDirectPlatformVoidBatchExecution(new EdhrRecordChangeRequestReqVO()
                    .setBatchExecutionId(batch.getId())
                    .setReasonCategory("ORDER_CANCELLED")
                    .setReasonText("金手指直通作废后工作台任务必须同步关闭。")
                    .setPassword("request-pass")
                    .setComment("direct void"), ACTOR_ID);
        }

        verify(workTaskService).cancelActiveTasksByBatch(batch.getId(),
                "批次已作废：金手指直通作废后工作台任务必须同步关闭。");
    }

    @Test
    void executeDirectPlatformVoidBatchExecutionRecordsActiveOrderOperationFact() {
        MesProEdhrBatchExecutionDO batch = insertClosedBatchExecution();
        insertSealedBatchArchive(batch.getId());
        when(batchExecutionOriginMapper.selectListByBatchExecutionId(batch.getId()))
                .thenReturn(java.util.List.of(new MesProEdhrBatchExecutionOriginDO()
                        .setBatchExecutionId(batch.getId())
                        .setActiveOrderId(8101L)
                        .setOriginKey("ACTIVE_ORDER")));

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            batchVoidEffectService.executeDirectPlatformVoidBatchExecution(new EdhrRecordChangeRequestReqVO()
                    .setBatchExecutionId(batch.getId())
                    .setReasonCategory("ORDER_CANCELLED")
                    .setReasonText("作废必须归属同一活跃订单。")
                    .setPassword("request-pass")
                    .setComment("active-order void"), ACTOR_ID);
        }

        ArgumentCaptor<MesProEdhrOperationAuditCommand> captor =
                ArgumentCaptor.forClass(MesProEdhrOperationAuditCommand.class);
        verify(operationAuditService, org.mockito.Mockito.times(2))
                .recordInCallerTransaction(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(
                java.util.Set.of("BATCH_VOID_REQUEST", "BATCH_VOID_EFFECTIVE"),
                captor.getAllValues().stream()
                        .map(MesProEdhrOperationAuditCommand::getOperationType)
                        .collect(java.util.stream.Collectors.toSet()));
        captor.getAllValues().forEach(command ->
                org.junit.jupiter.api.Assertions.assertTrue(command.getMetadataJson()
                        .contains("\"activeOrderId\":8101")));
    }

    @Test void pendingNcrRejectsPrecheckRequestAndDirectVoidWithoutSignatures() {
        var batch = insertClosedBatchExecution();
        when(reviewMapper.selectPendingByBatchExecutionId(batch.getId())).thenReturn(
                new cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO().setId(991L));
        var request = new EdhrRecordChangeRequestReqVO().setBatchExecutionId(batch.getId())
                .setReasonCategory("ORDER_CANCELLED").setReasonText("pending NCR").setPassword("pass");
        org.junit.jupiter.api.Assertions.assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> batchVoidEffectService.precheckPlatformVoidBatchExecution(request));
        org.junit.jupiter.api.Assertions.assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> batchVoidEffectService.requestPlatformVoidBatchExecution(request, "process-1"));
        org.junit.jupiter.api.Assertions.assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> batchVoidEffectService.executeDirectPlatformVoidBatchExecution(request, ACTOR_ID));
        org.mockito.Mockito.verifyNoInteractions(signatureService, workTaskService, operationAuditService);
        org.junit.jupiter.api.Assertions.assertEquals(30, batchExecutionMapper.selectById(batch.getId()).getStatus());
    }

    @Test void ncrCreatedAfterRequestRejectsBpmEffectAndKeepsReviewableBatch() {
        var batch = insertClosedBatchExecution();
        var archive = insertSealedBatchArchive(batch.getId());
        try (var login = mockLoginUser()) {
            batchVoidEffectService.requestPlatformVoidBatchExecution(new EdhrRecordChangeRequestReqVO()
                    .setBatchExecutionId(batch.getId()).setReasonCategory("ORDER_CANCELLED")
                    .setReasonText("before NCR").setPassword("pass"), "void-process-2");
        }
        when(reviewMapper.selectPendingByBatchExecutionId(batch.getId())).thenReturn(
                new cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO().setId(992L));
        org.junit.jupiter.api.Assertions.assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> batchVoidEffectService.handleVoidBatchExecutionApprovalCallback("void-process-2", "event-2",
                        "APPROVED", null, ACTOR_ID));
        org.junit.jupiter.api.Assertions.assertEquals(30, batchExecutionMapper.selectById(batch.getId()).getStatus());
        org.junit.jupiter.api.Assertions.assertTrue(batchArchiveMapper.selectById(archive.getId()).getArchiveValidFlag());
        org.mockito.Mockito.verifyNoInteractions(workTaskService);
    }

    private static MockedStatic<SecurityFrameworkUtils> mockLoginUser() {
        MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class);
        security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(ACTOR_ID);
        return security;
    }

    private MesProEdhrBatchExecutionDO insertClosedBatchExecution() {
        MesProEdhrBatchExecutionDO batch = MesProEdhrBatchExecutionDO.builder()
                .batchExecutionCode("BATCH-VOID-EFFECT-" + System.nanoTime())
                .workOrderId(30L)
                .workOrderCode("MO-VOID-EFFECT")
                .batchCode("BATCH-VOID-EFFECT")
                .routeId(40L)
                .routeCode("ROUTE-VOID-EFFECT")
                .status(30)
                .taskTotal(2)
                .taskApprovedCount(2)
                .blockedCount(0)
                .aggregateHash(HASH_64)
                .closedBy(ACTOR_ID)
                .closedAt(LocalDateTime.now().minusHours(1))
                .build();
        batch.setTenantId(0L);
        batchExecutionMapper.insert(batch);
        return batch;
    }

    private MesProEdhrBatchExecutionArchiveDO insertSealedBatchArchive(Long batchExecutionId) {
        MesProEdhrBatchExecutionArchiveDO archive = MesProEdhrBatchExecutionArchiveDO.builder()
                .batchExecutionId(batchExecutionId)
                .archiveVersion(1)
                .artifactType("FINAL_PDF")
                .archiveStatus("SEALED")
                .fileName("edhr-batch.pdf")
                .contentType("application/pdf")
                .fileSize(100L)
                .filePath("mes/edhr/batch-void-effect.pdf")
                .contentHash(HASH_64)
                .sourceManifestJson("{\"batchExecutionId\":" + batchExecutionId + "}")
                .generatedBy(ACTOR_ID)
                .generatedAt(LocalDateTime.now().minusMinutes(30))
                .sealedSignatureId(8802L)
                .archiveValidFlag(Boolean.TRUE)
                .archiveValidStatus("VALID")
                .build();
        batchArchiveMapper.insert(archive);
        return archive;
    }

    @Resource private javax.sql.DataSource dataSource;
    private org.springframework.jdbc.core.JdbcTemplate jdbc;
    @org.junit.jupiter.api.BeforeEach void jdbcFixture(){
        jdbc=new org.springframework.jdbc.core.JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE IF NOT EXISTS mes_pro_edhr_nonconformance_review(id BIGINT PRIMARY KEY,tenant_id BIGINT,source_type VARCHAR(40),source_id BIGINT,active_order_id BIGINT,deleted BOOLEAN DEFAULT FALSE,frozen_at TIMESTAMP,review_status VARCHAR(40),disposition VARCHAR(40),closed_at TIMESTAMP)");
    }

    private void installRealTaskCancellation() {
        var real=new MesProEdhrWorkTaskServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(real,"workTaskMapper",realTasks);
        org.springframework.test.util.ReflectionTestUtils.setField(batchVoidEffectService,"workTaskService",real);
    }
    private cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO pendingApplication(Long batchId) {
        var app=cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO.builder()
                .activeOrderId(8101L).workOrderId(30L).workOrderCode("MO-VOID-EFFECT").routeId(40L).routeVersionId(41L)
                .batchCode("BATCH-VOID-EFFECT").batchExecutionId(batchId).applicationStatus("PQC_RELEASE_PENDING")
                .sourceSnapshotHash(HASH_64).version(1).requestIdempotencyKey("void-app-request-"+java.util.UUID.randomUUID())
                .businessIdempotencyKey("void-app-business-"+java.util.UUID.randomUUID()).dossierSummaryJson("{\"formalDossier\":\"unchanged\"}").appliedAt(LocalDateTime.now()).build();
        app.setTenantId(0L);applications.insert(app);
        var task=cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO.builder()
                .taskCode("PQC-VOID-"+app.getId()).taskType("PQC_PRODUCTION_RELEASE").businessScopeType("RELEASE_APPLICATION")
                .businessScopeId(app.getId()).workOrderId(30L).assigneeUserId(ACTOR_ID).candidateUserSnapshot(ACTOR_ID.toString())
                .status("TODO").actionUrl("/pqc-production-release").build();
        realTasks.insert(task);applications.updateById(app.setPqcReleaseWorkTaskId(task.getId()));return app;
    }
    private EdhrRecordChangeRequestReqVO voidRequest(Long id) {
        return new EdhrRecordChangeRequestReqVO().setBatchExecutionId(id).setReasonCategory("ORDER_CANCELLED")
                .setReasonText("正式独立作废，关闭准确申请").setPassword("test-password");
    }
    @Test void batchVoidClosesExactApplicationTaskAndReturnsActualChangeHistoryWithoutPqcSignature() {
        var batch=insertClosedBatchExecution();insertSealedBatchArchive(batch.getId());var app=pendingApplication(batch.getId());installRealTaskCancellation();
        var otherBatch=insertClosedBatchExecution();var otherApp=pendingApplication(otherBatch.getId());
        String otherApplicationBefore=cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(applications.selectById(otherApp.getId()));
        String otherTaskBefore=cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(realTasks.selectById(otherApp.getPqcReleaseWorkTaskId()));
        try(var login=mockLoginUser()) {
            var requested=batchVoidEffectService.requestPlatformVoidBatchExecution(voidRequest(batch.getId()),"sa14-bpm");
            var result=batchVoidEffectService.handleVoidBatchExecutionApprovalCallback("sa14-bpm","approved-event","APPROVED",null,ACTOR_ID);
            org.junit.jupiter.api.Assertions.assertEquals(requested.getId(),result.getId());
            var saved=applications.selectById(app.getId());var task=realTasks.selectById(app.getPqcReleaseWorkTaskId());
            org.junit.jupiter.api.Assertions.assertEquals("BATCH_VOIDED",saved.getApplicationStatus());org.junit.jupiter.api.Assertions.assertEquals(2,saved.getVersion());
            org.junit.jupiter.api.Assertions.assertEquals("CANCELED",task.getStatus());
            org.junit.jupiter.api.Assertions.assertNull(task.getBatchExecutionId());
            org.junit.jupiter.api.Assertions.assertEquals(otherApplicationBefore,cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(applications.selectById(otherApp.getId())));
            org.junit.jupiter.api.Assertions.assertEquals(otherTaskBefore,cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(realTasks.selectById(otherApp.getPqcReleaseWorkTaskId())));
            org.junit.jupiter.api.Assertions.assertNull(saved.getPqcDecision());org.junit.jupiter.api.Assertions.assertNull(saved.getPqcDecidedBy());org.junit.jupiter.api.Assertions.assertNull(saved.getPqcDecidedAt());
            var dossier=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(saved.getDossierSummaryJson());
            org.junit.jupiter.api.Assertions.assertEquals("unchanged",dossier.path("formalDossier").asText());
            org.junit.jupiter.api.Assertions.assertEquals(result.getId().longValue(),dossier.path("batchVoid").path("changeEventId").longValue());
            org.junit.jupiter.api.Assertions.assertEquals("EFFECTIVE",changes.selectById(result.getId()).getChangeStatus());
            String applicationBefore=cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(saved);
            String taskBefore=cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(task);
            batchVoidEffectService.handleVoidBatchExecutionApprovalCallback("sa14-bpm","approved-event","APPROVED",null,ACTOR_ID);
            org.junit.jupiter.api.Assertions.assertEquals(applicationBefore,cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(applications.selectById(app.getId())));
            org.junit.jupiter.api.Assertions.assertEquals(taskBefore,cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(realTasks.selectById(task.getId())));
            cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesSignatureDetailFixture.installJsonFunctions(jdbc);
            jdbc.execute("CREATE DOMAIN IF NOT EXISTS UNSIGNED AS BIGINT");
            var page=new cn.iocoder.yudao.framework.common.pojo.PageParam();
            var history=applications.selectPqcReleasePage(page,0L,ACTOR_ID,"VOIDED",null,null,app.getId(),task.getId());
            org.junit.jupiter.api.Assertions.assertEquals(1L,history.getTotal());org.junit.jupiter.api.Assertions.assertEquals(saved.getDossierSummaryJson(),history.getList().get(0).getDossierSummaryJson());
            org.junit.jupiter.api.Assertions.assertEquals(0L,applications.selectPqcReleasePage(page,0L,ACTOR_ID,"PENDING",null,null,app.getId(),task.getId()).getTotal());
            org.junit.jupiter.api.Assertions.assertEquals(0L,applications.selectPqcReleasePage(page,1L,ACTOR_ID,"VOIDED",null,null,app.getId(),task.getId()).getTotal());
            jdbc.update("UPDATE mes_pro_edhr_record_change_event SET change_status = 'SUBMITTED' WHERE id = ?",result.getId());
            org.junit.jupiter.api.Assertions.assertEquals(0L,applications.selectPqcReleasePage(page,0L,ACTOR_ID,"VOIDED",null,null,app.getId(),task.getId()).getTotal());
        }
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"scope","workOrder","tenant","applicationTenant"})
    void wrongReleaseAssociationRollsBackBatchArchiveApplicationAndTask(String mismatch) {
        var batch=insertClosedBatchExecution();var archive=insertSealedBatchArchive(batch.getId());var app=pendingApplication(batch.getId());installRealTaskCancellation();
        if("scope".equals(mismatch))realTasks.updateById(new cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO().setId(app.getPqcReleaseWorkTaskId()).setBusinessScopeId(app.getId()+1));
        if("workOrder".equals(mismatch))realTasks.updateById(new cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO().setId(app.getPqcReleaseWorkTaskId()).setWorkOrderId(31L));
        if("tenant".equals(mismatch))jdbc.update("UPDATE mes_pro_edhr_work_task SET tenant_id = 1 WHERE id = ?",app.getPqcReleaseWorkTaskId());
        if("applicationTenant".equals(mismatch))jdbc.update("UPDATE mes_pro_process_pool_active_order_release_application SET tenant_id = 1 WHERE id = ?",app.getId());
        try(var login=mockLoginUser()) {
            batchVoidEffectService.requestPlatformVoidBatchExecution(voidRequest(batch.getId()),"sa14-wrong-bpm");
            org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,()->batchVoidEffectService.handleVoidBatchExecutionApprovalCallback("sa14-wrong-bpm","wrong-event","APPROVED",null,ACTOR_ID));
        }
        org.junit.jupiter.api.Assertions.assertEquals(30,batchExecutionMapper.selectById(batch.getId()).getStatus());
        org.junit.jupiter.api.Assertions.assertTrue(batchArchiveMapper.selectById(archive.getId()).getArchiveValidFlag());
        org.junit.jupiter.api.Assertions.assertEquals("PQC_RELEASE_PENDING",applications.selectById(app.getId()).getApplicationStatus());
        org.junit.jupiter.api.Assertions.assertEquals("TODO",realTasks.selectById(app.getPqcReleaseWorkTaskId()).getStatus());
    }

}
