package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchTraceabilityRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.*;
import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesProEdhrActiveOrderPrintSourceTest {
    @Mock MesProEdhrBatchExecutionOriginMapper originMapper;
    @Mock MesProcessPoolActiveOrderCompletionReceiptMapper receiptMapper;
    @Mock MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort receiptPort;
    @Mock MesProEdhrBatchTraceabilityService traceabilityService;
    @InjectMocks MesProEdhrActiveOrderPrintSource source;

    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    private MesProEdhrBatchExecutionDO batch() {
        TenantContextHolder.setTenantId(168L);
        return new MesProEdhrBatchExecutionDO().setId(10L).setTenantId(168L).setWorkOrderId(20L)
                .setRouteId(30L).setRouteVersionId(40L).setStatus(40);
    }
    private MesProEdhrReleaseTransactionDO release() {
        return new MesProEdhrReleaseTransactionDO().setId(50L).setTenantId(168L).setBatchExecutionId(10L)
                .setWorkOrderId(20L).setReleaseStatus("RELEASED").setApprovedBy(60L)
                .setApprovedAt(LocalDateTime.of(2026, 11, 8, 12, 0)).setApprovalSignatureId(70L)
                .setReleaseDecisionId(80L).setApprovalSignoffEvidenceHash("signed").setFinalizationPayloadHash("finalized");
    }
    private MesProcessPoolActiveOrderCompletionReceiptDO freezeReceipt() {
        var receipt = new MesProcessPoolActiveOrderCompletionReceiptDO();
        receipt.setId(90L); receipt.setTenantId(168L);
        receipt.setActiveOrderId(100L); receipt.setWorkOrderId(20L);
        receipt.setRouteId(30L); receipt.setRouteVersionId(40L);
        receipt.setCompletedVersion(3); receipt.setBatchRecordId(110L); receipt.setProcessInspectionId(120L);
        receipt.setReceiptStatus("BACKFILL_SUCCEEDED"); receipt.setCompletionStatus("SUCCESS");
        receipt.setBatchRecordStatus("SUCCESS"); receipt.setProcessInspectionStatus("SUCCESS");
        receipt.setFormalSourceSnapshotJson("{\"activeOrderBinding\":{\"id\":100,\"tenantId\":168,\"workOrderId\":20,\"routeId\":30,\"routeVersionId\":40},"
                + "\"workOrderBinding\":{\"id\":20},\"productionFacts\":{\"events\":[{\"id\":901}],\"reviews\":[]},"
                + "\"snapshots\":[],\"completions\":[],\"pqcTasks\":[],\"pqcDetails\":[]}");
        receipt.setSignatureSnapshotJson("{}"); receipt.setLossConditionFactsJson("[]");
        receipt.setSourceSnapshotHash(MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer.sourceSnapshotHash(
                receipt.getFormalSourceSnapshotJson(), receipt.getLossConditionFactsJson()));
        receipt.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt));
        return receipt;
    }
    private MesProEdhrBatchExecutionOriginDO bind(MesProcessPoolActiveOrderCompletionReceiptDO receipt) {
        var origin = new MesProEdhrBatchExecutionOriginDO().setId(130L).setTenantId(168L).setBatchExecutionId(10L)
                .setWorkOrderId(20L).setActiveOrderId(100L).setEntryType("ACTIVE_ORDER_COMPLETION")
                .setRelationStatus("CAPTURED").setCompletionVersion(3).setCompletionBackfillReceiptId(90L)
                .setCompletionBackfillReceiptHash(receipt.getReceiptHash()).setSourceSnapshotHash(receipt.getSourceSnapshotHash());
        when(originMapper.selectListByBatchExecutionId(10L)).thenReturn(List.of(origin));
        when(receiptMapper.selectByIdAndTenantId(90L, 168L)).thenReturn(receipt);
        return origin;
    }

    @Test void source_keepsExactFrozenPayloadAndVerifiedTraceability() {
        var batch = batch(); var receipt = freezeReceipt(); bind(receipt);
        var creation = new MesEdhrSourceCreationEvidence("COMPLETION", 90L, "90", "3", receipt.getReceiptHash(), 140L, "audit");
        when(receiptPort.requireVerifiedByReceiptId(90L, 168L)).thenReturn(new MesEdhrVerifiedCompletionReceiptFact(
                new MesFlow6CompletionBackfillReceipt().setReceiptHash(receipt.getReceiptHash()), creation, "TX-A-1"));
        var trace = new MesProEdhrBatchTraceabilityRespVO().setBatchExecutionId(10L);
        when(traceabilityService.getTraceability(10L)).thenReturn(trace);

        var result = source.requireSource(batch, release());

        assertEquals(receipt.getFormalSourceSnapshotJson(), result.getString("formalSourceSnapshotJson"));
        assertEquals(receipt.getReceiptHash(), result.getString("completionReceiptHash"));
        assertSame(trace, result.get("traceability"));
        assertEquals(901L, result.getJSONObject("formalSourceSnapshot").getJSONObject("productionFacts")
                .getJSONArray("events").getJSONObject(0).getLong("id"));
    }

    @ParameterizedTest @ValueSource(strings={"notArchived", "notReleased", "otherTenant", "otherOrder", "missingSignature", "missingFinalization"})
    void source_rejectsUnreleasedOrUnboundBatchBeforeReadingFacts(String problem) {
        var batch = batch(); var release = release();
        switch(problem) {
            case "notArchived" -> batch.setStatus(30);
            case "notReleased" -> release.setReleaseStatus("WITHDRAWN");
            case "otherTenant" -> release.setTenantId(1L);
            case "otherOrder" -> release.setWorkOrderId(21L);
            case "missingSignature" -> release.setApprovalSignatureId(null);
            case "missingFinalization" -> release.setFinalizationPayloadHash(null);
        }
        assertEquals(PRO_EDHR_BATCH_PRINT_RELEASE_REQUIRED.getCode(),
                assertThrows(ServiceException.class, () -> source.requireSource(batch, release)).getCode());
        verifyNoInteractions(originMapper, receiptMapper, receiptPort, traceabilityService);
    }

    @Test void source_rejectsChangedFrozenFactsEvenWhenAnOldReceiptHashIsPresent() {
        var batch = batch(); var receipt = freezeReceipt(); bind(receipt);
        receipt.setFormalSourceSnapshotJson(receipt.getFormalSourceSnapshotJson().replace("901", "902"));
        assertEquals(PRO_EDHR_BATCH_PRINT_SOURCE_INVALID.getCode(),
                assertThrows(ServiceException.class, () -> source.requireSource(batch, release())).getCode());
        verifyNoInteractions(receiptPort, traceabilityService);
    }

    @Test void source_rejectsIndependentOrAmbiguousOrigins() {
        var batch = batch();
        when(originMapper.selectListByBatchExecutionId(10L)).thenReturn(List.of());
        assertEquals(PRO_EDHR_BATCH_PRINT_SOURCE_INVALID.getCode(),
                assertThrows(ServiceException.class, () -> source.requireSource(batch, release())).getCode());
        verifyNoInteractions(receiptMapper, receiptPort, traceabilityService);
    }
}
