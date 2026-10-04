package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.*;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRoleCodes;
import cn.iocoder.yudao.module.signature.api.dto.*;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.springframework.test.context.jdbc.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Sql(scripts = "/sql/pqc-signature-contract.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/sa07-read-contract.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = {"/sql/pqc-signature-contract-clean.sql", "/sql/sa07-read-contract-clean.sql"}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@org.springframework.context.annotation.Import(MesSignatureH2DialectConfiguration.class)
class MesSa07Sa08SignatureContractTest extends BaseDbUnitTest {
    @Resource ElectronicSignatureRecordMapper records;
    @Resource MesProProcessPoolEventMapper events;
    @Resource MesProcessPoolActiveOrderMapper orders;
    @Resource MesProcessPoolActiveOrderProcessSnapshotMapper snapshots;
    @Resource MesProcessPoolReportAllocationMapper allocations;
    @Resource javax.sql.DataSource dataSource;
    private org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Resource cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolMapper pools;
    private MesSignaturePersistenceFixture signatures;
    private MockedStatic<SecurityFrameworkUtils> security;

    @BeforeEach void setupSignatureRuntime() {
        jdbc = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
        TenantContextHolder.setTenantId(1L);
        security = mockStatic(SecurityFrameworkUtils.class);
        security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
        signatures = new MesSignaturePersistenceFixture(records, mock(AdminUserApi.class), mock(GxpAuditService.class));
    }
    @AfterEach void clearRuntime() { security.close(); TenantContextHolder.clear(); }

    @Test void originalSignedOrderAndAllocatedTargetRemainSeparateInBothReaders() throws Exception {
        var context = new MesProductionSubmitSignatureContext(100L, 40L, 30L, "source-submission");
        String subject = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(0L, "PRODUCTION_SUBMIT",
                null, null, null, null, null, null, null, context.SOURCE_TYPE, context.activeOrderId(),
                context.sourceName(), null, null, null, null, null);
        var signed = signatures.writer.signAuthorized(new ElectronicSignatureCommand("MES", "PRODUCTION_SUBMIT",
                "MES_BATCH_RECORD", subject, DigestUtil.sha256Hex(subject), "test-only", "production",
                "source-signature", null, null), new AuthorizedSignatureIdentity("MES_EMPLOYEE_PROFILE", 7L, "Frozen signer B"), () -> {});
        var eventWriter = new cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolEventServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(eventWriter, "processPoolEventMapper", events);
        org.springframework.test.util.ReflectionTestUtils.setField(eventWriter, "processPoolMapper", pools);
        org.springframework.test.util.ReflectionTestUtils.setField(eventWriter, "nonconformanceReviewService", mock(MesProEdhrNonconformanceReviewService.class));
        org.springframework.test.util.ReflectionTestUtils.setField(eventWriter, "reportManagementSummaryService", mock(MesProductionReportManagementSummaryService.class));
        Long eventId = eventWriter.createEvent(cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolCreateEventReqDTO.builder()
                .workOrderId(10L).routeId(20L).routeProcessId(40L).processId(30L).eventType("PRODUCTION_SUBMIT")
                .eventIdempotencyKey("source-submission").actualEmployeeId(7L).signatureUserId(7L)
                .deviceAccountId(99L).workstationId(90L).templateType("PRODUCTION").feedbackSourceType("TEST").feedbackSourceId(1L)
                .signatureId(signed.signatureId()).rawPayload("{\"activeOrderId\":100,\"signatureIdentityDomain\":\"MES_EMPLOYEE_PROFILE\"}").build());
        // BaseDbUnitTest does not install the production tenant SQL interceptor.
        jdbc.update("UPDATE mes_pro_process_pool_event SET tenant_id=1 WHERE id=?", eventId);
        var event = events.selectById(eventId);
        var source = order(100L, 10L);
        var target = order(101L, 12L);
        orders.insert(source); orders.insert(target);
        var original = snapshot(100L, 10L, 40L);
        var allocated = snapshot(101L, 12L, 41L);
        snapshots.insert(original); snapshots.insert(allocated);
        var allocation = new MesProcessPoolReportAllocationDO().setId(51L).setEventId(event.getId())
                .setActiveOrderId(101L).setWorkOrderId(12L).setRouteProcessId(41L).setProcessId(30L)
                .setAllocatedQuantity(BigDecimal.ONE).setLifecycleStatus("CURRENT");
        allocation.setTenantId(1L);
        allocations.insert(allocation);
        jdbc.update("INSERT INTO mes_pro_work_order(id,tenant_id,code) VALUES(10,1,'SOURCE'),(12,1,'TARGET')");
        jdbc.update("INSERT INTO mes_pro_route(id,tenant_id,name) VALUES(20,1,'Route')");
        jdbc.update("INSERT INTO mes_pro_process(id,tenant_id,name) VALUES(30,1,'Process')");
        jdbc.update("INSERT INTO mes_pro_process_pool_team_employee_profile VALUES(7,1,'Current different name','Current different name',false)");
        var binding = new MesProductionSubmissionReadBinding(orders, snapshots, allocations);
        var reader = new MesSubmissionSignatureIdentityReader(signatures.query, events, null, binding);
        var display = reader.read(signed.signatureId(), eventId, 101L, "PRODUCTION_SUBMIT");
        assertEquals("Frozen signer B", display.getSignerName());
        var applications = mock(MesProcessPoolActiveOrderReleaseApplicationMapper.class);
        var detailService = MesSignatureDetailFixture.detail(jdbc, orders, events, signatures.query, binding,
                applications, mock(MesProEdhrReleaseTransactionMapper.class), mock(MesProEdhrOperationAuditEventMapper.class));
        var batchService = mock(MesProEdhrBatchExecutionService.class);
        when(batchService.get(800L)).thenReturn(new cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionRespVO()
                .setActiveOrderId(101L).setWorkOrderId(12L));
        var details = new MesProEdhrBatchActiveOrderDetailService(batchService, orders, detailService);
        var application = new MesProcessPoolActiveOrderReleaseApplicationDO().setId(701L).setActiveOrderId(101L).setWorkOrderId(12L);
        when(applications.selectById(701L)).thenReturn(application);
        var pqcAuthorization = mock(cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcProductionReleaseService.class);
        when(pqcAuthorization.get(99L,701L)).thenReturn(new cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcProductionReleaseDecisionResult());
        var materials = mock(cn.iocoder.yudao.module.mes.service.pro.workorder.kingdee.MesKingdeeProductionMaterialListQueryService.class);
        when(materials.getPage(any())).thenReturn(new cn.iocoder.yudao.framework.common.pojo.PageResult<>(List.of(),0L));
        var pqcDetails = new cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcReleaseOrderDetailService(
                pqcAuthorization,applications,orders,signatures.query,null,detailService,materials);
        var evidenceReader = new MesActiveOrderSignatureEvidenceService(detailService, pqcDetails, details, signatures.query,
                events, null, null, null, binding, null, null, null, null, null);
        for (var detail : List.of(detailService.getDetail(99L,101L), details.getDetail(800L))) {
            assertEquals(eventId,detail.getProcesses().get(0).getSubmissions().get(0).getEventId());
            assertEquals("Current different name",detail.getProcesses().get(0).getSubmissions().get(0).getSubmitterName());
            assertEquals("Frozen signer B",detail.getProcesses().get(0).getSubmissions().get(0).getSubmitterSignature().getSignerName());
        }
        assertEquals("VALID", evidenceReader.getTeam(99L,101L,signed.signatureId()).verification().verificationStatus());
        assertEquals("VALID", evidenceReader.getPqc(99L,701L,signed.signatureId()).verification().verificationStatus());
        assertEquals("VALID", evidenceReader.getBatch(800L, null, signed.signatureId()).verification().verificationStatus());
        assertEquals("VALID", evidenceReader.getBatch(null,101L,signed.signatureId()).verification().verificationStatus());
        assertEquals(100L, com.alibaba.fastjson.JSON.parseObject(signatures.query.getById(signed.signatureId())
                .canonicalContentJson()).getLong("reviewSourceId"));
        // Formal source retention includes a removed source order and an inactive viewing order.
        jdbc.update("UPDATE mes_pro_process_pool_active_order SET deleted=true WHERE id=100");
        jdbc.update("UPDATE mes_pro_process_pool_active_order SET active_status='REMOVED' WHERE id=101");
        assertEquals("Frozen signer B",details.getDetail(800L).getProcesses().get(0).getSubmissions().get(0)
                .getSubmitterSignature().getSignerName());
        assertEquals("VALID",evidenceReader.getBatch(800L,null,signed.signatureId()).verification().verificationStatus());
        jdbc.update("UPDATE mes_pro_process_pool_active_order SET deleted=false WHERE id=100");
        jdbc.update("UPDATE mes_pro_process_pool_active_order SET active_status='ACTIVE' WHERE id=101");
        var originalEvidence = signatures.query.getById(signed.signatureId());
        for (String invalid : List.of("actor","operator","domain","source","tenant","hash")) {
            event.setActualEmployeeId(7L).setSignatureUserId(7L).setDeviceAccountId(99L)
                    .setRawPayload("{\"activeOrderId\":100,\"signatureIdentityDomain\":\"MES_EMPLOYEE_PROFILE\"}");
            event.setTenantId(1L);
            switch (invalid) {
                case "actor" -> event.setActualEmployeeId(8L).setSignatureUserId(8L);
                case "operator" -> event.setDeviceAccountId(98L);
                case "domain" -> event.setRawPayload("{\"activeOrderId\":100,\"signatureIdentityDomain\":\"SYSTEM_USER\"}");
                case "source" -> event.setRawPayload("{\"activeOrderId\":999,\"signatureIdentityDomain\":\"MES_EMPLOYEE_PROFILE\"}");
                case "tenant" -> event.setTenantId(2L);
                case "hash" -> records.updateById(ElectronicSignatureRecordDO.builder().id(signed.signatureId()).evidenceHash("b".repeat(64)).build());
            }
            events.updateById(event);
            assertThrows(IllegalStateException.class, () -> reader.read(signed.signatureId(),eventId,101L,"PRODUCTION_SUBMIT"),invalid);
            assertThrows(RuntimeException.class, () -> evidenceReader.getTeam(99L,101L,signed.signatureId()),invalid);
            assertThrows(RuntimeException.class, () -> evidenceReader.getPqc(99L,701L,signed.signatureId()),invalid);
            assertThrows(RuntimeException.class, () -> evidenceReader.getBatch(800L,null,signed.signatureId()),invalid);
        }
        event.setTenantId(1L);
        events.updateById(event);
        records.updateById(ElectronicSignatureRecordDO.builder().id(signed.signatureId()).evidenceHash(originalEvidence.evidenceHash()).build());
        for (String invalid : List.of("missing", "tenant", "route", "workOrder", "process", "snapshot", "source")) {
            allocation.setTenantId(1L); allocation.setWorkOrderId(12L); allocation.setProcessId(30L);
            target.setRouteId(20L); allocated.setRouteId(20L); original.setWorkOrderId(10L);
            allocation.setLifecycleStatus("CURRENT");
            switch (invalid) {
                case "missing" -> allocation.setLifecycleStatus("SUPERSEDED");
                case "tenant" -> allocation.setTenantId(2L);
                case "route" -> target.setRouteId(21L);
                case "workOrder" -> allocation.setWorkOrderId(13L);
                case "process" -> allocation.setProcessId(31L);
                case "snapshot" -> allocated.setRouteId(21L);
                case "source" -> original.setWorkOrderId(13L);
            }
            allocations.updateById(allocation); orders.updateById(target); snapshots.updateById(allocated); snapshots.updateById(original);
            assertThrows(IllegalStateException.class, () -> reader.read(signed.signatureId(), eventId, 101L, "PRODUCTION_SUBMIT"), invalid);
            assertThrows(RuntimeException.class, () -> evidenceReader.getTeam(99L,101L,signed.signatureId()),invalid);
            assertThrows(RuntimeException.class, () -> evidenceReader.getPqc(99L,701L,signed.signatureId()),invalid);
            assertThrows(RuntimeException.class, () -> evidenceReader.getBatch(800L,null,signed.signatureId()),invalid);
        }
    }

    @Test void persistedBpmContractBindsExactOwnerAndRejectsTamperingAndWrongAssociations() {
        var signed = signatures.bpm(99L, 2001L, "approved");
        var signoff = new MesProductionReleaseSignoffService(signatures.query);
        assertEquals(signed.getUnifiedSignatureId(), signoff.findVerifiedSignatureId(2001L, 99L,
                signed.getSubjectId(), signed.getEvidenceHash(), "approved").orElseThrow());
        assertTrue(signoff.findVerifiedSignatureId(2002L, 99L, signed.getSubjectId(), signed.getEvidenceHash(), "approved").isEmpty());
        assertTrue(signoff.findVerifiedSignatureId(2001L, 98L, signed.getSubjectId(), signed.getEvidenceHash(), "approved").isEmpty());
        assertTrue(signoff.findVerifiedSignatureId(2001L, 99L, signed.getSubjectId(), signed.getEvidenceHash(), "changed").isEmpty());
        for (var wrongSource : List.of(
                signatures.bpm(99L,2001L,"approved",cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode.DCC,"EDHR_WORK_TASK"),
                signatures.bpm(99L,2001L,"approved",cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode.EDHR,"OTHER_TASK"))) {
            assertTrue(signoff.findVerifiedSignatureId(2001L,99L,wrongSource.getSubjectId(),wrongSource.getEvidenceHash(),"approved").isEmpty());
        }
        var application = new MesProcessPoolActiveOrderReleaseApplicationDO().setId(701L).setActiveOrderId(801L)
                .setBatchExecutionId(901L).setReleaseTransactionId(1001L).setReleaseApprovalWorkTaskId(2001L)
                .setApplicationStatus("RELEASED");
        application.setTenantId(1L);
        var transaction = new MesProEdhrReleaseTransactionDO().setId(1001L).setBatchExecutionId(901L)
                .setReleaseStatus("RELEASED").setApprovedBy(99L).setApprovalOpinion("approved")
                .setApprovalSignatureId(signed.getUnifiedSignatureId()).setApprovalSignoffEvidenceHash(signed.getEvidenceHash());
        var task = new MesProEdhrWorkTaskDO().setId(2001L).setBatchExecutionId(901L).setTaskType("RELEASE_APPROVE")
                .setBusinessScopeType("RELEASE_TRANSACTION").setBusinessScopeId(1001L).setCandidateSourceType("ROLE_GROUP")
                .setCandidateUserSnapshot("99").setResponsibilitySourceKey(MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE).setStatus("DONE");
        var batch = new MesProEdhrBatchExecutionDO().setId(901L).setTenantId(1L);
        var applications = mock(MesProcessPoolActiveOrderReleaseApplicationMapper.class);
        var transactions = mock(MesProEdhrReleaseTransactionMapper.class);
        var tasks = mock(MesProEdhrWorkTaskMapper.class);
        var batches = mock(MesProEdhrBatchExecutionMapper.class);
        when(applications.selectListByActiveOrderIds(List.of(801L))).thenReturn(List.of(application));
        when(transactions.selectById(1001L)).thenReturn(transaction);
        when(tasks.selectById(2001L)).thenReturn(task);
        when(batches.selectById(901L)).thenReturn(batch);
        var details = mock(MesProEdhrBatchActiveOrderDetailService.class);
        when(details.getDetail(901L)).thenReturn(new MesTeamLeaderActiveOrderDetail().setActiveOrderId(801L)
                .setOperationFacts(List.of(new MesTeamLeaderActiveOrderDetail.OperationFact()
                        .setOperationType("BATCH_RECORD_RELEASE_APPROVED").setSignatureId(signed.getUnifiedSignatureId())
                        .setActorUserId(99L).setActorName("Manager"))));
        var reader = new MesActiveOrderSignatureEvidenceService(null, null, details, signatures.query,
                null, null, null, null, null, applications, transactions, tasks, batches, signoff);
        assertEquals("VALID", reader.getBatch(901L, null, signed.getUnifiedSignatureId()).verification().verificationStatus());
        task.setBusinessScopeId(1002L);
        assertThrows(IllegalStateException.class, () -> reader.getBatch(901L, null, signed.getUnifiedSignatureId()));
        task.setBusinessScopeId(1001L); application.setTenantId(2L);
        assertThrows(IllegalStateException.class, () -> reader.getBatch(901L, null, signed.getUnifiedSignatureId()));
        application.setTenantId(1L); transaction.setApprovalSignatureId(999L);
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> reader.getBatch(901L, null, signed.getUnifiedSignatureId()));
        transaction.setApprovalSignatureId(signed.getUnifiedSignatureId());
        records.updateById(ElectronicSignatureRecordDO.builder().id(signed.getUnifiedSignatureId()).evidenceHash("b".repeat(64)).build());
        assertTrue(signoff.findVerifiedSignatureId(2001L, 99L, signed.getSubjectId(), "b".repeat(64), "approved").isEmpty());
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> reader.getBatch(901L, null, signed.getUnifiedSignatureId()));
        TenantContextHolder.setTenantId(2L);
        assertTrue(signoff.findVerifiedSignatureId(2001L, 99L, signed.getSubjectId(), signed.getEvidenceHash(), "approved").isEmpty());
    }

    private static MesProcessPoolActiveOrderDO order(long id, long work) {
        var order = new MesProcessPoolActiveOrderDO().setId(id).setWorkOrderId(work).setRouteId(20L).setLeaderUserId(99L).setActiveStatus("ACTIVE");
        order.setTenantId(1L); return order;
    }
    private static MesProcessPoolActiveOrderProcessSnapshotDO snapshot(long order, long work, long routeProcess) {
        var snapshot = new MesProcessPoolActiveOrderProcessSnapshotDO().setActiveOrderId(order).setWorkOrderId(work)
                .setRouteId(20L).setRouteVersionId(21L).setRouteProcessId(routeProcess).setProcessId(30L)
                .setProcessCodeSnapshot("P").setProcessNameSnapshot("Process").setErpFixedQuantitySnapshot(BigDecimal.TEN)
                .setProductionQuantityFactorSnapshot(BigDecimal.ONE).setPlannedQuantitySnapshot(BigDecimal.TEN);
        snapshot.setTenantId(1L); return snapshot;
    }
}
