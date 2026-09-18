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

import java.time.LocalDateTime;
import java.util.List;

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
        when(orders.selectById(10L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(10L).workOrderId(11L).leaderUserId(99L).build());
        var result = new MesTeamLeaderActiveOrderDetail();
        result.setWorkOrderCode("WO-11");
        when(detail.getDetail(99L, 10L)).thenReturn(result);
        when(materials.getPage(any())).thenReturn(new cn.iocoder.yudao.framework.common.pojo.PageResult<>(java.util.List.of(), 0L));
        var service = new MesPqcReleaseOrderDetailService(auth, applications, orders, signatures, users, detail, materials);
        assertSame(result, service.get(7L, 8L).detail());
        var order = inOrder(auth, applications, orders, detail);
        order.verify(auth).get(7L, 8L);
        order.verify(applications).selectById(8L);
        order.verify(orders).selectById(10L);
        order.verify(detail).getDetail(99L, 10L);
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
        when(orders.selectById(10L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(10L)
                .workOrderId(11L)
                .leaderUserId(99L)
                .build());
        when(signatures.getById(66L))
                .thenReturn(signatureEvidence(66L, 77L, "release-subject-88", signedAt));
        when(signatures.verifyEvidence(66L)).thenReturn(
                new ElectronicSignatureVerificationDTO(66L, "VALID", "content-hash", "content-hash",
                        "evidence-hash", "evidence-hash", "SHA-256", "v1"));
        when(users.getUser(77L)).thenReturn(new AdminUserDO().setId(77L).setNickname("王放行"));
        var result = new MesTeamLeaderActiveOrderDetail();
        result.setWorkOrderCode("WO-11");
        when(detail.getDetail(99L, 10L)).thenReturn(result);
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
        when(orders.selectById(1009200145L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(1009200145L)
                .workOrderId(11L)
                .leaderUserId(99L)
                .build());
        when(signatures.getById(9001L))
                .thenReturn(signatureEvidence(9001L, 1L, "release-subject-900000001059", signedAt));
        when(signatures.verifyEvidence(9001L)).thenReturn(
                new ElectronicSignatureVerificationDTO(9001L, "VALID", "content-hash", "content-hash",
                        "evidence-hash", "evidence-hash", "SHA-256", "v1"));
        when(users.getUser(1L)).thenReturn(new AdminUserDO().setId(1L).setNickname("管理员"));
        var result = new MesTeamLeaderActiveOrderDetail();
        result.setWorkOrderCode("WO-11");
        when(detail.getDetail(99L, 1009200145L)).thenReturn(result);
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
        when(orders.selectById(10L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(10L)
                .workOrderId(11L)
                .leaderUserId(99L)
                .build());
        when(signatures.getById(66L)).thenReturn(null);
        var result = new MesTeamLeaderActiveOrderDetail();
        result.setWorkOrderCode("WO-11");
        when(detail.getDetail(99L, 10L)).thenReturn(result);

        var service = new MesPqcReleaseOrderDetailService(auth, applications, orders, signatures, users, detail, materials);
        assertEquals("PQC_RELEASE_SIGNATURE_RECORD_MISSING",
                assertThrows(IllegalStateException.class, () -> service.get(7L, 8L)).getMessage());
        verifyNoInteractions(materials);
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
                null);
    }
}
