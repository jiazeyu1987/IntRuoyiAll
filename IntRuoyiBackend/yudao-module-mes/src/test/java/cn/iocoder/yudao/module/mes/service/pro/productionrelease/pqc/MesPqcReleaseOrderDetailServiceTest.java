package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.service.pro.workorder.kingdee.MesKingdeeProductionMaterialListQueryService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchRecordSignatureSubjectAdapter;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureEvidenceDTO;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureVerificationDTO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.service.pro.frontline.ActiveOrderSnapshotResolver;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineProcessMaterialServiceImpl;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrOperationAuditEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionReplenishmentListMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionReplenishmentListItemMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

class MesPqcReleaseOrderDetailServiceTest {
    @Test
    void rejectsBeforeReadingOrderSources() {
        var auth = mock(MesPqcProductionReleaseService.class);
        var applications = mock(MesProcessPoolActiveOrderReleaseApplicationMapper.class);
        var orders = mock(MesProcessPoolActiveOrderMapper.class);
        var signatures = mock(ElectronicSignatureQueryService.class);
        var users = mock(AdminUserService.class);
        var detail = mock(MesTeamLeaderActiveOrderDetailService.class);
        var materials = mock(MesKingdeeProductionMaterialListQueryService.class);
        var denied = new IllegalStateException("not a frozen PQC candidate");
        when(auth.get(7L, 8L)).thenThrow(denied);
        var service = new MesPqcReleaseOrderDetailService(auth, applications, orders, signatures, users, detail, materials);
        assertSame(denied, assertThrows(IllegalStateException.class, () -> service.get(7L, 8L)));
        verifyNoInteractions(applications, orders, signatures, users, detail, materials);
    }

    @Test
    void authorizedPqcViewerUsesPersistedOwnerOnlyAfterAuthorization() {
        var auth = mock(MesPqcProductionReleaseService.class);
        var applications = mock(MesProcessPoolActiveOrderReleaseApplicationMapper.class);
        var orders = mock(MesProcessPoolActiveOrderMapper.class);
        var signatures = mock(ElectronicSignatureQueryService.class);
        var users = mock(AdminUserService.class);
        var detail = mock(MesTeamLeaderActiveOrderDetailService.class);
        var materials = mock(MesKingdeeProductionMaterialListQueryService.class);
        when(auth.get(7L, 8L)).thenReturn(new MesPqcProductionReleaseDecisionResult().setApplicationId(8L));
        when(applications.selectById(8L)).thenReturn(MesProcessPoolActiveOrderReleaseApplicationDO.builder()
                .id(8L).activeOrderId(10L).workOrderId(11L).build());
        when(orders.selectByIdIgnoreDeleted(10L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(10L).workOrderId(11L).leaderUserId(99L).build());
        var result = new MesTeamLeaderActiveOrderDetail();
        result.setWorkOrderCode("WO-11");
        when(detail.getArchivedFormalDetail(10L)).thenReturn(result);
        when(materials.getPage(any())).thenReturn(new cn.iocoder.yudao.framework.common.pojo.PageResult<>(java.util.List.of(), 0L));
        var service = new MesPqcReleaseOrderDetailService(auth, applications, orders, signatures, users, detail, materials);
        assertSame(result, service.get(7L, 8L).detail());
        var order = inOrder(auth, applications, orders, detail);
        order.verify(auth).get(7L, 8L);
        order.verify(applications).selectById(8L);
        order.verify(orders).selectByIdIgnoreDeleted(10L);
        order.verify(detail).getArchivedFormalDetail(10L);
        verifyNoInteractions(signatures, users);
    }

    @Test
    void releasedPqcApplicationAddsProductionReleaseSummaryWithFormalSignature() {
        var auth = mock(MesPqcProductionReleaseService.class);
        var applications = mock(MesProcessPoolActiveOrderReleaseApplicationMapper.class);
        var orders = mock(MesProcessPoolActiveOrderMapper.class);
        var signatures = mock(ElectronicSignatureQueryService.class);
        var users = mock(AdminUserService.class);
        var detail = mock(MesTeamLeaderActiveOrderDetailService.class);
        var materials = mock(MesKingdeeProductionMaterialListQueryService.class);
        var signedAt = LocalDateTime.of(2026, 9, 17, 9, 30);
        when(auth.get(7L, 8L)).thenReturn(new MesPqcProductionReleaseDecisionResult()
                .setApplicationId(8L)
                .setStatus("REPORT_UPLOAD_PENDING")
                .setDecision("APPROVE")
                .setSignatureId(66L));
        when(applications.selectById(8L)).thenReturn(MesProcessPoolActiveOrderReleaseApplicationDO.builder()
                .id(8L)
                .activeOrderId(10L)
                .workOrderId(11L)
                .batchExecutionId(88L)
                .build());
        when(orders.selectByIdIgnoreDeleted(10L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(10L)
                .workOrderId(11L)
                .leaderUserId(99L)
                .build());
        String subjectId = releaseSubjectId(88L, 8L);
        when(signatures.getById(66L)).thenReturn(signatureEvidence(66L, 77L, subjectId, signedAt));
        when(signatures.verifyEvidence(66L)).thenReturn(
                new ElectronicSignatureVerificationDTO(66L, "VALID", "content-hash", "content-hash",
                        "evidence-hash", "evidence-hash", "SHA-256", "v1"));
        when(users.getUser(77L)).thenReturn(new AdminUserDO().setId(77L).setNickname("王放行"));
        var result = new MesTeamLeaderActiveOrderDetail();
        result.setWorkOrderCode("WO-11");
        when(detail.getArchivedFormalDetail(10L)).thenReturn(result);
        when(materials.getPage(any())).thenReturn(new cn.iocoder.yudao.framework.common.pojo.PageResult<>(java.util.List.of(), 0L));

        var service = new MesPqcReleaseOrderDetailService(auth, applications, orders, signatures, users, detail, materials);
        var summary = service.get(7L, 8L).detail().getPqcProductionRelease();

        assertNotNull(summary);
        assertEquals("REPORT_UPLOAD_PENDING", summary.getStatus());
        assertEquals("已生产放行", summary.getStatusLabel());
        assertNotNull(summary.getSignature());
        assertEquals(66L, summary.getSignature().getSignatureId());
        assertEquals("王放行", summary.getSignature().getSignerName());
        assertEquals(signedAt, summary.getSignature().getSignedAt());
        assertEquals(MesProBatchRecordExecutionSignatureService.ACTION_PQC_RELEASE,
                summary.getSignature().getRole());
    }

    @Test
    void releasedPqcApplicationUsesUnifiedSignatureEvidenceWhenRetiredBatchSignatureRecordIsAbsent() {
        var auth = mock(MesPqcProductionReleaseService.class);
        var applications = mock(MesProcessPoolActiveOrderReleaseApplicationMapper.class);
        var orders = mock(MesProcessPoolActiveOrderMapper.class);
        var signatures = mock(ElectronicSignatureQueryService.class);
        var users = mock(AdminUserService.class);
        var detail = mock(MesTeamLeaderActiveOrderDetailService.class);
        var materials = mock(MesKingdeeProductionMaterialListQueryService.class);
        var signedAt = LocalDateTime.of(2026, 9, 18, 0, 7);
        when(auth.get(7L, 56L)).thenReturn(new MesPqcProductionReleaseDecisionResult()
                .setApplicationId(56L)
                .setStatus("REPORT_UPLOAD_PENDING")
                .setDecision("APPROVE")
                .setSignatureId(9001L));
        when(applications.selectById(56L)).thenReturn(MesProcessPoolActiveOrderReleaseApplicationDO.builder()
                .id(56L)
                .activeOrderId(1009200145L)
                .workOrderId(11L)
                .batchExecutionId(900000001059L)
                .build());
        when(orders.selectByIdIgnoreDeleted(1009200145L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(1009200145L)
                .workOrderId(11L)
                .leaderUserId(99L)
                .build());
        String subjectId = releaseSubjectId(900000001059L, 56L);
        when(signatures.getById(9001L)).thenReturn(signatureEvidence(9001L, 1L, subjectId, signedAt));
        when(signatures.verifyEvidence(9001L)).thenReturn(
                new ElectronicSignatureVerificationDTO(9001L, "VALID", "content-hash", "content-hash",
                        "evidence-hash", "evidence-hash", "SHA-256", "v1"));
        when(users.getUser(1L)).thenReturn(new AdminUserDO().setId(1L).setNickname("管理员"));
        var result = new MesTeamLeaderActiveOrderDetail();
        result.setWorkOrderCode("WO-11");
        when(detail.getArchivedFormalDetail(1009200145L)).thenReturn(result);
        when(materials.getPage(any())).thenReturn(new cn.iocoder.yudao.framework.common.pojo.PageResult<>(java.util.List.of(), 0L));

        var service = new MesPqcReleaseOrderDetailService(auth, applications, orders, signatures, users, detail, materials);
        var summary = service.get(7L, 56L).detail().getPqcProductionRelease();

        assertNotNull(summary);
        assertEquals(9001L, summary.getSignature().getSignatureId());
        assertEquals("管理员", summary.getSignature().getSignerName());
        assertEquals(signedAt, summary.getSignature().getSignedAt());
    }

    @Test
    void releasedPqcApplicationFailsFastWhenUnifiedSignatureEvidenceIsMissing() {
        var auth = mock(MesPqcProductionReleaseService.class);
        var applications = mock(MesProcessPoolActiveOrderReleaseApplicationMapper.class);
        var orders = mock(MesProcessPoolActiveOrderMapper.class);
        var signatures = mock(ElectronicSignatureQueryService.class);
        var users = mock(AdminUserService.class);
        var detail = mock(MesTeamLeaderActiveOrderDetailService.class);
        var materials = mock(MesKingdeeProductionMaterialListQueryService.class);
        when(auth.get(7L, 8L)).thenReturn(new MesPqcProductionReleaseDecisionResult()
                .setApplicationId(8L)
                .setStatus("REPORT_UPLOAD_PENDING")
                .setDecision("APPROVE")
                .setSignatureId(66L));
        when(applications.selectById(8L)).thenReturn(MesProcessPoolActiveOrderReleaseApplicationDO.builder()
                .id(8L)
                .activeOrderId(10L)
                .workOrderId(11L)
                .batchExecutionId(88L)
                .build());
        when(orders.selectByIdIgnoreDeleted(10L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(10L)
                .workOrderId(11L)
                .leaderUserId(99L)
                .build());
        when(signatures.getById(66L)).thenReturn(null);
        var result = new MesTeamLeaderActiveOrderDetail();
        result.setWorkOrderCode("WO-11");
        when(detail.getArchivedFormalDetail(10L)).thenReturn(result);

        var service = new MesPqcReleaseOrderDetailService(auth, applications, orders, signatures, users, detail, materials);
        assertEquals("PQC_RELEASE_SIGNATURE_RECORD_MISSING",
                assertThrows(IllegalStateException.class, () -> service.get(7L, 8L)).getMessage());
        verifyNoInteractions(materials);
    }

    @Test
    void completedOrderReadsThroughRealFormalDetailAndFrozenMaterialServices() {
        var fixture = new FormalSourceFixture("COMPLETED", false);
        var result = fixture.service.get(7L, 8L);
        assertEquals(10L, result.detail().getActiveOrderId());
        assertEquals(11L, result.detail().getWorkOrderId());
        assertEquals("WO-11", result.detail().getWorkOrderCode());
        assertEquals(30L, result.detail().getProcesses().get(0).getRouteProcessId());
        assertEquals(BigDecimal.ONE, result.detail().getProcesses().get(0).getRequiredQuantity());
        verify(fixture.processSnapshots).selectByActiveOrderAndProcess(10L, 30L, 31L);
        verify(fixture.orders, never()).selectMaps(any());
    }

    @Test
    void logicallyArchivedOrderReadsItsPersistedFormalSource() {
        var fixture = new FormalSourceFixture("CLOSED", true);
        var result = fixture.service.get(7L, 8L);
        assertEquals("WO-11", result.detail().getWorkOrderCode());
        assertEquals(10L, result.detail().getActiveOrderId());
        verify(fixture.orders, never()).selectById(any());
        verify(fixture.orders, never()).selectMaps(any());
    }

    @Test
    void pendingPqcApplicationRemainsReadableWithoutReleaseSignature() {
        var fixture = new FormalSourceFixture("ACTIVE", false);
        var result = fixture.service.get(7L, 8L);
        assertEquals("WO-11", result.detail().getWorkOrderCode());
        assertNull(result.detail().getPqcProductionRelease());
        verifyNoInteractions(fixture.signatures, fixture.users);
    }

    @Test
    void archivedSourceWithDifferentWorkOrderIsRejectedBeforeDetailRead() {
        var fixture = new FormalSourceFixture("CLOSED", true);
        fixture.order.setWorkOrderId(12L);
        assertEquals("PQC_RELEASE_ACTIVE_ORDER_SOURCE_INVALID",
                assertThrows(IllegalStateException.class, () -> fixture.service.get(7L, 8L)).getMessage());
        verifyNoInteractions(fixture.rows, fixture.processSnapshots, fixture.materials, fixture.signatures);
    }

    @Test
    void archivedSourceWithoutPersistedLeaderIsRejectedBeforeDetailRead() {
        var fixture = new FormalSourceFixture("CLOSED", true);
        fixture.order.setLeaderUserId(null);
        assertEquals("PQC_RELEASE_ACTIVE_ORDER_SOURCE_INVALID",
                assertThrows(IllegalStateException.class, () -> fixture.service.get(7L, 8L)).getMessage());
        verifyNoInteractions(fixture.rows, fixture.processSnapshots, fixture.materials);
    }

    @Test
    void archivedFormalMaterialSourceStillRejectsWrongFrozenProcessVersion() {
        var fixture = new FormalSourceFixture("CLOSED", true);
        fixture.processSnapshot.setRouteVersionId(999L);
        assertThrows(ServiceException.class, () -> fixture.service.get(7L, 8L));
        verify(fixture.processSnapshots).selectByActiveOrderAndProcess(10L, 30L, 31L);
        verifyNoInteractions(fixture.materials, fixture.signatures);
    }

    @Test
    void archivedReleasedApplicationPreservesVerifiedFormalSignature() {
        var fixture = new FormalSourceFixture("CLOSED", true);
        var signedAt = LocalDateTime.of(2026, 10, 4, 12, 30);
        fixture.approve(signatureEvidence(66L, 77L, releaseSubjectId(88L, 8L), signedAt));
        var result = fixture.service.get(7L, 8L);
        assertEquals("RELEASED", result.detail().getPqcProductionRelease().getStatus());
        assertEquals(66L, result.detail().getPqcProductionRelease().getSignature().getSignatureId());
        assertEquals("王放行", result.detail().getPqcProductionRelease().getSignature().getSignerName());
        assertEquals(signedAt, result.detail().getPqcProductionRelease().getSignature().getSignedAt());
        verify(fixture.signatures).verifyEvidence(66L);
    }

    @Test
    void archivedReleasedApplicationRejectsAnotherApplicationsSignature() {
        var fixture = new FormalSourceFixture("CLOSED", true);
        fixture.approve(signatureEvidence(66L, 77L, releaseSubjectId(88L, 9L),
                LocalDateTime.of(2026, 10, 4, 12, 30)));
        assertEquals("PQC_RELEASE_SIGNATURE_RECORD_MISSING",
                assertThrows(IllegalStateException.class, () -> fixture.service.get(7L, 8L)).getMessage());
        verifyNoInteractions(fixture.materials, fixture.users);
        verify(fixture.signatures, never()).verifyEvidence(any());
    }

    @Test
    void deniedViewerNeverReadsEvenArchivedFormalSources() {
        var fixture = new FormalSourceFixture("CLOSED", true);
        var denied = new IllegalStateException("not a frozen PQC candidate");
        when(fixture.auth.get(7L, 8L)).thenThrow(denied);
        assertSame(denied, assertThrows(IllegalStateException.class, () -> fixture.service.get(7L, 8L)));
        verifyNoInteractions(fixture.applications, fixture.orders, fixture.rows,
                fixture.processSnapshots, fixture.materials, fixture.signatures, fixture.users);
    }

    /** Uses the actual three read services; only persistent/query ports are replaced. */
    private static final class FormalSourceFixture {
        final MesPqcProductionReleaseService auth = mock(MesPqcProductionReleaseService.class);
        final MesProcessPoolActiveOrderReleaseApplicationMapper applications =
                mock(MesProcessPoolActiveOrderReleaseApplicationMapper.class);
        final MesProcessPoolActiveOrderMapper orders = mock(MesProcessPoolActiveOrderMapper.class);
        final MesProcessPoolActiveOrderDetailReadMapper rows = mock(MesProcessPoolActiveOrderDetailReadMapper.class);
        final MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshots =
                mock(MesProcessPoolActiveOrderProcessSnapshotMapper.class);
        final ElectronicSignatureQueryService signatures = mock(ElectronicSignatureQueryService.class);
        final AdminUserService users = mock(AdminUserService.class);
        final MesKingdeeProductionMaterialListQueryService materials =
                mock(MesKingdeeProductionMaterialListQueryService.class);
        final MesProcessPoolActiveOrderDO order;
        final MesProcessPoolActiveOrderProcessSnapshotDO processSnapshot;
        final MesPqcReleaseOrderDetailService service;

        FormalSourceFixture(String activeStatus, boolean deleted) {
            order = MesProcessPoolActiveOrderDO.builder().id(10L).workOrderId(11L).leaderUserId(99L)
                    .routeId(20L).routeVersionId(21L).dccProjectCodeId(22L)
                    .qaRegulationId(23L).qaRegulationVersionId(24L)
                    .activeStatus(activeStatus).businessStatus("COMPLETED").build();
            order.setDeleted(deleted);
            when(auth.get(7L, 8L)).thenReturn(new MesPqcProductionReleaseDecisionResult()
                    .setApplicationId(8L).setStatus("PQC_RELEASE_PENDING"));
            when(applications.selectById(8L)).thenReturn(MesProcessPoolActiveOrderReleaseApplicationDO.builder()
                    .id(8L).activeOrderId(10L).workOrderId(11L).batchExecutionId(88L).build());
            when(orders.selectById(10L)).thenReturn(deleted ? null : order);
            when(orders.selectByIdIgnoreDeleted(10L)).thenReturn(order);
            // Models the existing ACTIVE-only mapper query without replacing the resolver.
            when(orders.selectMaps(any())).thenReturn("ACTIVE".equals(activeStatus) && !deleted
                    ? List.of(Map.of("activeOrderId", 10L, "workOrderId", 11L, "routeId", 20L,
                    "routeVersionId", 21L, "dccProjectCodeId", 22L, "qaRegulationId", 23L,
                    "qaRegulationVersionId", 24L)) : List.of());
            when(rows.selectByActiveOrderId(10L)).thenReturn(List.of(new MesTeamLeaderActiveOrderDetailReadDO()
                    .setSnapshotId(40L).setActiveOrderId(10L).setWorkOrderId(11L).setWorkOrderCode("WO-11")
                    .setRouteName("Frozen route").setRouteProcessId(30L).setProcessId(31L)
                    .setProcessName("Frozen process").setRequiredQuantity(BigDecimal.ONE)));
            processSnapshot = MesProcessPoolActiveOrderProcessSnapshotDO.builder()
                    .id(40L).activeOrderId(10L).workOrderId(11L).routeId(20L).routeVersionId(21L)
                    .routeProcessId(30L).processId(31L).build();
            when(processSnapshots.selectByActiveOrderAndProcess(10L, 30L, 31L)).thenReturn(processSnapshot);
            var versions = mock(MesProRouteVersionMapper.class);
            when(versions.selectById(21L)).thenReturn(MesProRouteVersionDO.builder().id(21L).routeId(20L)
                    .routeSnapshotJson("""
                            {"routeId":20,"configSnapshots":{"batchUseConfigs":[
                            {"routeProcessId":30,"inputMaterialIds":[],"outputMaterialIds":[]}]}}
                            """).build());
            var workOrders = mock(MesProWorkOrderMapper.class);
            when(workOrders.selectById(11L)).thenReturn(MesProWorkOrderDO.builder().id(11L).build());
            var items = mock(MesMdItemMapper.class);
            var frozenMaterials = new MesFrontlineProcessMaterialServiceImpl(
                    new ActiveOrderSnapshotResolver(orders), processSnapshots, versions, workOrders, items);
            var detail = new MesTeamLeaderActiveOrderDetailServiceImpl(orders, rows, frozenMaterials,
                    mock(MesPqcInspectionTaskMapper.class), mock(MesPqcProcessInspectionAggregateDetailMapper.class),
                    mock(MesQaInspectionRegulationProcessMapper.class),
                    mock(ErpKingdeeProductionReplenishmentListItemMapper.class),
                    mock(ErpKingdeeProductionReplenishmentListMapper.class),
                    mock(ErpKingdeeProductionPickListMapper.class), mock(ErpKingdeeProductionPickListItemMapper.class),
                    items, mock(MesProcessPoolActiveOrderCompletionBackfillMapper.class),
                    mock(MesProProcessPoolEventMapper.class), mock(MesProProcessPoolEventRevisionMapper.class),
                    applications, mock(MesProEdhrNonconformanceReviewMapper.class),
                    mock(MesProEdhrOperationAuditEventMapper.class), mock(MesProEdhrReleaseTransactionMapper.class),
                    mock(MesProcessPoolTeamMaintenanceAuditMapper.class), signatures,
                    mock(MesSubmissionSignatureIdentityReader.class), users);
            when(materials.getPage(any())).thenReturn(new PageResult<>(List.of(), 0L));
            service = new MesPqcReleaseOrderDetailService(auth, applications, orders, signatures, users, detail, materials);
        }

        void approve(ElectronicSignatureEvidenceDTO signature) {
            when(auth.get(7L, 8L)).thenReturn(new MesPqcProductionReleaseDecisionResult()
                    .setApplicationId(8L).setStatus("RELEASED").setDecision("APPROVE").setSignatureId(66L));
            when(signatures.getById(66L)).thenReturn(signature);
            when(signatures.verifyEvidence(66L)).thenReturn(new ElectronicSignatureVerificationDTO(
                    66L, "VALID", "content-hash", "content-hash", "evidence-hash", "evidence-hash", "SHA-256", "v1"));
            when(users.getUser(77L)).thenReturn(new AdminUserDO().setId(77L).setNickname("王放行"));
        }
    }

    private static ElectronicSignatureEvidenceDTO signatureEvidence(
            Long id, Long actorId, String subjectId, LocalDateTime signedAt) {
        return new ElectronicSignatureEvidenceDTO(id,
                MesBatchRecordSignatureSubjectAdapter.MODULE_CODE,
                MesProBatchRecordExecutionSignatureService.ACTION_PQC_RELEASE,
                MesBatchRecordSignatureSubjectAdapter.SUBJECT_TYPE,
                subjectId,
                "subject-v1",
                actorId,
                MesProBatchRecordExecutionSignatureService.ACTION_PQC_RELEASE,
                "PQC放行",
                "同意放行",
                signedAt,
                "SERVER_CLOCK:" + signedAt,
                "SESSION_PLUS_PASSWORD",
                "content-hash",
                "evidence-hash",
                "SHA-256",
                "v1",
                "policy-v1",
                "VALID",
                null,
                null,
                null,
                null,
                "{}",
                null,
                null,
                null,
                null);
    }

    private static String releaseSubjectId(Long batchExecutionId, Long applicationId) {
        return MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(batchExecutionId,
                MesProBatchRecordExecutionSignatureService.ACTION_PQC_RELEASE,
                null, null, null, null, null, null, null,
                "PQC_RELEASE_APPLICATION", applicationId, "PQC生产放行",
                MesProBatchRecordExecutionSignatureService.ACTION_PQC_RELEASE,
                null, null, null, null);
    }
}

