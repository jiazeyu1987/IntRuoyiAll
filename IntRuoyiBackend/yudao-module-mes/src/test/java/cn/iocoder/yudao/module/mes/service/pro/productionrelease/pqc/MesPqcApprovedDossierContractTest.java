package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.*;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.*;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.report.*;
import cn.iocoder.yudao.module.mes.productionrelease.core.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import jakarta.annotation.Resource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Clock;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Actual approve service and H2 DONE writer, followed by real dossier authorization and file relation writer. */
class MesPqcApprovedDossierContractTest extends BaseDbUnitTest {
    @Resource private MesProEdhrWorkTaskMapper tasks;
    @Resource private MesProEdhrBatchExecutionMapper batches;
    @Resource private MesProEdhrBatchExecutionOriginMapper origins;
    @Resource private MesProcessPoolActiveOrderDossierFileMapper attachments;
    @Resource private MesProEdhrRecordChangeEventMapper changes;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void approvedTaskDoneStillAllowsPqcUploadAndPreservesLifecycleBlocks(boolean alsoBatchUpload) {
        long actor = 7101L, appId = 7001L, activeId = 2001L, batchId = 9001L, taskId = 8001L, workId = 3001L;
        var app = new MesProcessPoolActiveOrderReleaseApplicationDO().setId(appId).setActiveOrderId(activeId)
                .setWorkOrderId(workId).setRouteId(4001L).setRouteVersionId(4002L).setBatchExecutionId(batchId)
                .setPqcReleaseWorkTaskId(taskId).setApplicationStatus("PQC_RELEASE_PENDING").setVersion(1)
                .setWorkOrderCode("WORK-001").setBatchCode("BATCH-001").setSourceSnapshotHash("source");
        app.setTenantId(1L);
        var active = new MesProcessPoolActiveOrderDO().setId(activeId).setWorkOrderId(workId).setLeaderUserId(99L)
                .setBusinessStatus("COMPLETED").setUdiControlDocumentNo("UDI-001");
        active.setTenantId(1L);
        tasks.insert(new MesProEdhrWorkTaskDO().setId(taskId).setTaskCode("PQC-001").setTaskType("PQC_PRODUCTION_RELEASE")
                .setBusinessScopeType("RELEASE_APPLICATION").setBusinessScopeId(appId).setBatchExecutionId(batchId)
                .setAssigneeUserId(actor).setOwnershipLocked(true).setCandidateUserSnapshot(String.valueOf(actor))
                .setStatus("TODO").setActionUrl("/pqc"));
        batches.insert(new MesProEdhrBatchExecutionDO().setId(batchId).setWorkOrderId(workId).setTenantId(1L)
                .setBatchExecutionCode("BATCH-001").setBatchCode("BATCH-001").setRouteId(4001L).setStatus(30));
        origins.insert(new MesProEdhrBatchExecutionOriginDO().setId(9101L).setBatchExecutionId(batchId)
                .setActiveOrderId(activeId).setWorkOrderId(workId).setTenantId(1L).setEntryType("ACTIVE_ORDER_COMPLETION")
                .setOriginKey("ACTIVE-2001").setSourceSnapshotHash("source").setBatchProvisionReceiptId(9102L)
                .setBatchProvisionStatus("COMPLETED").setSourceBundleHash("bundle").setIdempotencyKey("origin-2001")
                .setRelationStatus("LINKED").setCapturedAt(java.time.LocalDateTime.of(2026, 10, 4, 12, 0)));
        var apps = mock(MesProcessPoolActiveOrderReleaseApplicationMapper.class);
        var orders = mock(MesProcessPoolActiveOrderMapper.class);
        var workOrders = mock(MesProWorkOrderMapper.class);
        when(apps.selectById(appId)).thenReturn(app);
        when(apps.selectByIdForUpdate(appId)).thenReturn(app);
        when(apps.selectLatestByActiveOrderId(activeId)).thenReturn(app);
        when(apps.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(app));
        when(orders.selectById(activeId)).thenReturn(active);
        when(orders.selectByIdForUpdate(activeId)).thenReturn(active);
        when(workOrders.selectByIdForUpdate(workId)).thenReturn(new MesProWorkOrderDO().setId(workId));
        when(apps.approveFromPending(eq(appId), eq(1), eq(batchId), eq(actor), any(), any(), any()))
                .thenAnswer(inv -> { app.setApplicationStatus("REPORT_UPLOAD_PENDING").setVersion(2).setPqcDecision("APPROVE"); return 1; });
        when(apps.handoffReportsToManager(eq(appId), eq(2), any(), eq(9201L), eq(9301L), eq("candidates")))
                .thenAnswer(inv -> { app.setApplicationStatus("MANAGER_RELEASE_PENDING").setVersion(3); return 1; });
        var signatures = mock(MesProBatchRecordExecutionSignatureService.class);
        when(signatures.recordPqcReleaseSignature(eq(actor), eq(batchId), eq(appId), any(), any())).thenReturn(9901L);
        var manager = mock(MesProductionReleaseManagerStageInitializer.class);
        when(manager.initializeManagerReleaseStage(any())).thenReturn(new MesProductionReleaseManagerStageInitializationResult()
                .setReleaseTransactionId(9201L).setManagerReleaseWorkTaskId(9301L).setManagerCandidateSnapshotHash("candidates"));
        var audit = mock(GxpAuditService.class);
        var ncr = mock(MesProEdhrNonconformanceReviewMapper.class);
        when(ncr.selectPendingCountByWorkOrderId(workId)).thenReturn(0L);
        var guard = new MesEdhrBatchLifecycleGuard();
        ReflectionTestUtils.setField(guard, "reviewMapper", ncr);
        ReflectionTestUtils.setField(guard, "changeMapper", changes);
        ReflectionTestUtils.setField(guard, "batchMapper", batches);
        var pqc = new MesPqcProductionReleaseServiceImpl(apps, orders, workOrders, tasks,
                mock(MesPqcReleaseDossierPort.class), mock(MesProductionReleaseBatchExecutionPort.class),
                mock(MesProductionReleaseReportStageInitializer.class), manager, mock(MesReleaseFlowAuditRecorder.class),
                signatures, mock(MesProEdhrNonconformanceReviewService.class), ncr, Clock.systemUTC());
        ReflectionTestUtils.setField(pqc, "gxpAuditService", audit);
        ReflectionTestUtils.setField(pqc, "lifecycleGuard", guard);
        var affectedStates = mock(MesReleaseAffectedStateCollector.class);
        when(affectedStates.capture(any(), any(), any(), any(), anyBoolean())).thenReturn(Map.of("batchExecutionId", batchId));
        ReflectionTestUtils.setField(pqc, "affectedStates", affectedStates);
        var permissions = mock(PermissionApi.class);
        when(permissions.hasAnyPermissions(eq(actor), any(String[].class))).thenAnswer(inv ->
                Arrays.stream((String[])inv.getRawArguments()[1]).anyMatch(permission ->
                        permission.equals("mes:pro-production-release:pqc-approve") || alsoBatchUpload &&
                        permission.equals("mes:pro-edhr-batch-execution:upload")));
        var scope = new MesActiveOrderDossierReadScopeService(apps, orders, batches,
                mock(MesProEdhrBatchExecutionVisibilityService.class), origins, pqc, tasks, permissions);
        var users = mock(AdminUserApi.class);
        when(users.getUser(actor)).thenReturn(new AdminUserRespDTO().setId(actor).setNickname("PQC"));
        var storage = mock(FileService.class);
        when(storage.createFileAndReturnId(any(), any(), any(), any())).thenReturn(70001L);
        when(storage.getFile(70001L)).thenReturn(new FileDO().setId(70001L).setConfigId(1L).setName("report.pdf")
                .setPath("mes/report.pdf").setUrl("/file/report.pdf").setType("application/pdf").setSize(3L));
        var files = new MesActiveOrderDossierFileService(apps, orders, attachments, ncr, users, storage,
                mock(MesProEdhrOperationAuditService.class));
        ReflectionTestUtils.setField(files, "gxpAuditService", audit);
        ReflectionTestUtils.setField(files, "dossierReadScopeService", scope);
        ReflectionTestUtils.setField(files, "lifecycleGuard", guard);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new LoginUser().setId(actor).setTenantId(1L).setUserType(2), null, List.of()));
        try {
            pqc.approve(actor, new MesPqcProductionReleaseApproveCommand().setApplicationId(appId)
                    .setPqcReleaseWorkTaskId(taskId).setExpectedVersion(1).setIdempotencyKey("pqc-approve")
                    .setApprovalOpinion("approved").setSignaturePassword("password").setUdiControlDocumentNo("UDI-001"));
            var done = tasks.selectById(taskId);
            assertEquals("DONE", done.getStatus()); assertEquals("APPROVE", done.getReason());
            var upload = new MesActiveOrderDossierFileService.UploadCommand(activeId, appId, "OTHER_FILE",
                    "report.pdf", "application/pdf", new byte[]{1,2,3});
            assertNotNull(files.upload(actor, upload).attachmentId());
            for (int status : List.of(40, 50, 60)) {
                batches.updateById(new MesProEdhrBatchExecutionDO().setId(batchId).setStatus(status));
                var blocked = assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                        () -> files.upload(actor, upload));
                assertEquals(MesEdhrBatchLifecycleGuard.BLOCKED.getCode(), blocked.getCode());
            }
            batches.updateById(new MesProEdhrBatchExecutionDO().setId(batchId).setStatus(30));
            when(ncr.selectPendingCountByWorkOrderId(workId)).thenReturn(1L);
            var blocked = assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                    () -> files.upload(actor, upload));
            assertEquals(cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants
                    .PRO_PROCESS_POOL_ACTIVE_ORDER_DOSSIER_FILE_BLOCKED.getCode(), blocked.getCode());
            verify(storage, times(1)).createFileAndReturnId(any(), any(), any(), any());
        } finally { SecurityContextHolder.clearContext(); }
    }
}
