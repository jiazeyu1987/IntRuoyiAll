package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationNcrCreateReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeviationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrDeviationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderDetailService;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import cn.hutool.crypto.digest.DigestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class MesProEdhrDeviationNcrIntegrationTest {

    @Mock private MesProEdhrNonconformanceReviewMapper reviewMapper;
    @Mock private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Mock private MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper;
    @Mock private MesProProcessPoolEventMapper processPoolEventMapper;
    @Mock private MesProEdhrWorkTaskMapper workTaskMapper;
    @Mock private MesProWorkOrderMapper workOrderMapper;
    @Mock private MesProBatchRecordExecutionSignatureService signatureService;
    @Mock private MesProEdhrBatchExecutionOriginMapper batchExecutionOriginMapper;
    @Mock private MesPqcInspectionTaskMapper pqcInspectionTaskMapper;
    @Mock private MesProEdhrOperationAuditService operationAuditService;
    @Mock private MesTeamLeaderActiveOrderDetailService activeOrderDetailService;
    @Mock private FileMapper fileMapper;
    @Mock private MesProEdhrDeviationMapper deviationMapper;

    @Mock private cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService unifiedAudit;
    @Mock private cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewCounterMapper reviewCounterMapper;
    private MesProEdhrNonconformanceReviewServiceImpl service;

    @BeforeEach
    void setUp() {
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(71L);
        service = new MesProEdhrNonconformanceReviewServiceImpl();
        ReflectionTestUtils.setField(service, "reviewCounterMapper", reviewCounterMapper);
        lenient().when(reviewCounterMapper.selectByTenantIdForUpdate(71L)).thenReturn(
                new cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewCounterDO()
                        .setTenantId(71L).setCurrentSerial(1L));
        ReflectionTestUtils.setField(service, "unifiedAudit", unifiedAudit);
        ReflectionTestUtils.setField(service, "reviewMapper", reviewMapper);
        ReflectionTestUtils.setField(service, "batchExecutionMapper", batchExecutionMapper);
        ReflectionTestUtils.setField(service, "releaseApplicationMapper", releaseApplicationMapper);
        ReflectionTestUtils.setField(service, "processPoolEventMapper", processPoolEventMapper);
        ReflectionTestUtils.setField(service, "workTaskMapper", workTaskMapper);
        ReflectionTestUtils.setField(service, "workOrderMapper", workOrderMapper);
        ReflectionTestUtils.setField(service, "signatureService", signatureService);
        ReflectionTestUtils.setField(service, "batchExecutionOriginMapper", batchExecutionOriginMapper);
        ReflectionTestUtils.setField(service, "pqcInspectionTaskMapper", pqcInspectionTaskMapper);
        ReflectionTestUtils.setField(service, "operationAuditService", operationAuditService);
        ReflectionTestUtils.setField(service, "activeOrderDetailService", activeOrderDetailService);
        ReflectionTestUtils.setField(service, "fileMapper", fileMapper);
        ReflectionTestUtils.setField(service, "deviationMapper", deviationMapper);
    }

    @org.junit.jupiter.api.AfterEach
    void clearTenant() {
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear();
    }

    @Test
    void ledgerLockPrecedesBusinessLocks() {
        assertThrows(ServiceException.class, () -> service.createCriticalDeviationReview(501L, request("lock-order")));
        var order = org.mockito.Mockito.inOrder(unifiedAudit, batchExecutionMapper);
        order.verify(unifiedAudit).acquireLedgerLock();
        order.verify(batchExecutionMapper).selectByIdForUpdate(9001L);
    }

    @Test
    void qaCanTransferMultipleOpenCriticalDeviationsBeforePqcPush() {
        stubBatchAndWorkOrder();
        when(reviewMapper.selectByTenantAndIdempotencyKeyForUpdate(any(), eq("ncr-1"))).thenReturn(null);
        when(reviewMapper.selectPendingByBatchExecutionId(9001L)).thenReturn(null);
        when(reviewMapper.insert(org.mockito.ArgumentMatchers.<MesProEdhrNonconformanceReviewDO>any())).thenAnswer(invocation -> {
            invocation.<MesProEdhrNonconformanceReviewDO>getArgument(0).setId(7001L);
            return 1;
        });
        when(signatureService.recordNonconformanceReviewCreateSignature(
                eq(501L), eq(7001L), eq("signature"), any(), any())).thenReturn(8101L);
        when(reviewMapper.attachCreateSignature(any(), eq(7001L), eq(8101L), eq("QA电子签名#8101"), eq(501L)))
                .thenReturn(1);
        when(batchExecutionMapper.updateById(org.mockito.ArgumentMatchers.<MesProEdhrBatchExecutionDO>any()))
                .thenReturn(1);
        when(deviationMapper.selectByTenantAndIdForUpdate(any(), eq(3001L))).thenReturn(critical(3001L));
        when(deviationMapper.selectByTenantAndIdForUpdate(any(), eq(3002L))).thenReturn(critical(3002L));
        when(deviationMapper.closeToNonconformance(any(), eq(9001L),
                org.mockito.ArgumentMatchers.<java.util.Collection<Long>>any(), eq(7001L), any())).thenReturn(2);

        var result = service.createCriticalDeviationReview(501L, request("ncr-1"));

        assertEquals(7001L, result.getId());
        verify(signatureService).recordNonconformanceReviewCreateSignature(
                eq(501L), eq(7001L), eq("signature"), any(), any());
        verify(deviationMapper).closeToNonconformance(any(), eq(9001L), eq(List.of(3001L, 3002L)), eq(7001L), any());
        verify(batchExecutionMapper).updateById(org.mockito.ArgumentMatchers.<MesProEdhrBatchExecutionDO>any());
        var audit = ArgumentCaptor.forClass(MesProEdhrOperationAuditCommand.class);
        verify(operationAuditService).recordInCallerTransaction(audit.capture());
        JSONObject metadata = JSON.parseObject(audit.getValue().getMetadataJson());
        // Transferred deviation IDs are source evidence, never uploaded review materials.
        assertNull(metadata.getString("reviewMaterialsJson"));
        assertEquals(List.of(3001L, 3002L), metadata.getJSONArray("sourceDeviationIds").toJavaList(Long.class));
    }

    @Test
    void missingFormalActiveOrderRejectsTransferBeforeAnyBusinessWrites() {
        stubBatchAndWorkOrder();
        when(batchExecutionOriginMapper.selectListByBatchExecutionId(9001L)).thenReturn(List.of());
        when(deviationMapper.selectByTenantAndIdForUpdate(any(), eq(3001L))).thenReturn(critical(3001L));
        when(deviationMapper.selectByTenantAndIdForUpdate(any(), eq(3002L))).thenReturn(critical(3002L));

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.createCriticalDeviationReview(501L, request("missing-origin")));

        assertEquals(MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_NONCONFORMANCE_REVIEW_SOURCE_INVALID.getCode(),
                error.getCode());
        verify(reviewMapper, never()).insert(org.mockito.ArgumentMatchers.<MesProEdhrNonconformanceReviewDO>any());
        verify(batchExecutionMapper, never()).updateById(org.mockito.ArgumentMatchers.<MesProEdhrBatchExecutionDO>any());
        verify(workOrderMapper, never()).updateTemporaryFrozenByIds(any(), any());
        verify(deviationMapper, never()).closeToNonconformance(any(), any(), anyCollection(), any(), any());
        verifyNoInteractions(signatureService, operationAuditService);
    }

    @Test
    void qaCanTransferSingleCriticalDeviationWithFormalActiveOrderOrigin() {
        stubBatchAndWorkOrder();
        when(reviewMapper.selectByTenantAndIdempotencyKeyForUpdate(any(), eq("ncr-formal-active-origin"))).thenReturn(null);
        when(reviewMapper.selectPendingByBatchExecutionId(9001L)).thenReturn(null);
        when(reviewMapper.insert(org.mockito.ArgumentMatchers.<MesProEdhrNonconformanceReviewDO>any())).thenAnswer(invocation -> {
            invocation.<MesProEdhrNonconformanceReviewDO>getArgument(0).setId(7006L);
            return 1;
        });
        when(signatureService.recordNonconformanceReviewCreateSignature(
                eq(501L), eq(7006L), eq("signature"), any(), any())).thenReturn(8106L);
        when(reviewMapper.attachCreateSignature(any(), eq(7006L), eq(8106L), eq("QA电子签名#8106"), eq(501L)))
                .thenReturn(1);
        when(batchExecutionMapper.updateById(org.mockito.ArgumentMatchers.<MesProEdhrBatchExecutionDO>any()))
                .thenReturn(1);
        when(deviationMapper.selectByTenantAndIdForUpdate(any(), eq(3001L))).thenReturn(critical(3001L));
        when(deviationMapper.closeToNonconformance(any(), eq(9001L),
                org.mockito.ArgumentMatchers.<java.util.Collection<Long>>any(), eq(7006L), any())).thenReturn(1);

        var result = service.createCriticalDeviationReview(501L,
                request("ncr-formal-active-origin").setDeviationIds(List.of(3001L)));

        assertEquals(7006L, result.getId());
        assertEquals(6001L, result.getActiveOrderId());
        verify(deviationMapper).closeToNonconformance(any(), eq(9001L), eq(List.of(3001L)), eq(7006L), any());
    }

    @Test
    void mixedBatchOrNormalDeviationIsRejectedAsWholeSet() {
        stubBatchAndWorkOrder();
        when(reviewMapper.selectByTenantAndIdempotencyKeyForUpdate(any(), eq("ncr-2"))).thenReturn(null);
        when(reviewMapper.selectPendingByBatchExecutionId(9001L)).thenReturn(null);
        when(deviationMapper.selectByTenantAndIdForUpdate(any(), eq(3001L))).thenReturn(critical(3001L));
        lenient().when(deviationMapper.selectByTenantAndIdForUpdate(any(), eq(3002L))).thenReturn(
                critical(3002L).setBatchExecutionId(9002L));

        assertThrows(ServiceException.class,
                () -> service.createCriticalDeviationReview(501L, request("ncr-2")));
        verify(reviewMapper, never()).insert(org.mockito.ArgumentMatchers.<MesProEdhrNonconformanceReviewDO>any());
        verify(signatureService, never()).recordNonconformanceReviewCreateSignature(any(), any(), any(), any(), any());
        verify(deviationMapper, never()).closeToNonconformance(any(), any(),
                org.mockito.ArgumentMatchers.<java.util.Collection<Long>>any(), any(), any());
    }

    @Test
    void emptyOrAlreadyClosedDeviationIsRejected() {
        MesProEdhrDeviationNcrCreateReqVO empty = request("ncr-3").setDeviationIds(List.of());
        assertThrows(ServiceException.class, () -> service.createCriticalDeviationReview(501L, empty));
        verifyNoInteractions(batchExecutionMapper, deviationMapper, signatureService);
    }

    @Test
    void pendingReviewRejectsSecondRequestAndLeavesNewDeviationOpen() {
        stubBatchAndWorkOrder();
        MesProEdhrNonconformanceReviewDO pending = new MesProEdhrNonconformanceReviewDO().setId(7002L);
        when(reviewMapper.selectPendingByBatchExecutionId(9001L)).thenReturn(pending);

        assertThrows(ServiceException.class,
                () -> service.createCriticalDeviationReview(501L, request("ncr-4")));
        verify(deviationMapper, never()).selectByTenantAndIdForUpdate(any(), any());
        verify(deviationMapper, never()).closeToNonconformance(any(), any(),
                org.mockito.ArgumentMatchers.<java.util.Collection<Long>>any(), any(), any());
    }

    @Test
    void sameIdempotencyKeyReplaysExistingReviewWithoutNewSignature() {
        stubBatchAndWorkOrder();
        MesProEdhrNonconformanceReviewDO existing = new MesProEdhrNonconformanceReviewDO()
                .setId(7003L).setIdempotencyKey("ncr-5").setPayloadHash("hash");
        when(reviewMapper.selectByTenantAndIdempotencyKeyForUpdate(any(), eq("ncr-5"))).thenReturn(existing);
        existing.setPayloadHash(payloadHash(request("ncr-5")));

        var result = service.createCriticalDeviationReview(501L, request("ncr-5"));

        assertEquals(7003L, result.getId());
        verifyNoInteractions(deviationMapper, signatureService);
        verify(reviewMapper, never()).insert(org.mockito.ArgumentMatchers.<MesProEdhrNonconformanceReviewDO>any());
    }

    @Test
    void idempotencyPayloadConflictIsRejected() {
        stubBatchAndWorkOrder();
        MesProEdhrNonconformanceReviewDO existing = new MesProEdhrNonconformanceReviewDO()
                .setId(7004L).setIdempotencyKey("ncr-6").setPayloadHash("different");
        when(reviewMapper.selectByTenantAndIdempotencyKeyForUpdate(any(), eq("ncr-6"))).thenReturn(existing);

        assertThrows(ServiceException.class,
                () -> service.createCriticalDeviationReview(501L, request("ncr-6")));
        verifyNoInteractions(deviationMapper, signatureService);
    }

    @Test
    void signatureFailureStopsFreezeAndDeviationClose() {
        stubBatchAndWorkOrder();
        when(reviewMapper.selectByTenantAndIdempotencyKeyForUpdate(any(), eq("ncr-7"))).thenReturn(null);
        when(reviewMapper.selectPendingByBatchExecutionId(9001L)).thenReturn(null);
        when(reviewMapper.insert(org.mockito.ArgumentMatchers.<MesProEdhrNonconformanceReviewDO>any())).thenAnswer(invocation -> {
            invocation.<MesProEdhrNonconformanceReviewDO>getArgument(0).setId(7005L);
            return 1;
        });
        when(deviationMapper.selectByTenantAndIdForUpdate(any(), eq(3001L))).thenReturn(critical(3001L));
        when(deviationMapper.selectByTenantAndIdForUpdate(any(), eq(3002L))).thenReturn(critical(3002L));
        when(signatureService.recordNonconformanceReviewCreateSignature(
                any(), any(), any(), any(), any())).thenThrow(new IllegalStateException("signature failed"));

        assertThrows(IllegalStateException.class,
                () -> service.createCriticalDeviationReview(501L, request("ncr-7")));
        verify(batchExecutionMapper, never()).updateById(org.mockito.ArgumentMatchers.<MesProEdhrBatchExecutionDO>any());
        verify(deviationMapper, never()).closeToNonconformance(any(), any(),
                org.mockito.ArgumentMatchers.<java.util.Collection<Long>>any(), any(), any());
    }

    private void stubBatchAndWorkOrder() {
        lenient().when(batchExecutionMapper.selectByIdForUpdate(9001L)).thenReturn(new MesProEdhrBatchExecutionDO()
                .setId(9001L).setBatchExecutionCode("BE-9001").setBatchCode("B-9001")
                .setWorkOrderId(8001L).setWorkOrderCode("WO-8001").setStatus(1));
        lenient().when(batchExecutionOriginMapper.selectListByBatchExecutionId(9001L)).thenReturn(List.of(
                new MesProEdhrBatchExecutionOriginDO().setBatchExecutionId(9001L).setActiveOrderId(6001L)));
        lenient().when(workOrderMapper.selectByIdForUpdate(8001L)).thenReturn(new MesProWorkOrderDO()
                .setId(8001L).setTemporaryFrozen(false));
        lenient().when(workOrderMapper.updateTemporaryFrozenByIds(any(), eq(true))).thenReturn(1);
        lenient().when(reviewMapper.selectFreezeLifecycleByWorkOrderId(8001L)).thenReturn(List.of());
    }

    private MesProEdhrDeviationDO critical(Long id) {
        return new MesProEdhrDeviationDO().setId(id).setBatchExecutionId(9001L)
                .setLevel("CRITICAL").setStatus("OPEN");
    }

    private MesProEdhrDeviationNcrCreateReqVO request(String idempotencyKey) {
        return new MesProEdhrDeviationNcrCreateReqVO()
                .setBatchExecutionId(9001L).setDeviationIds(List.of(3001L, 3002L))
                .setNonconformanceReason("关键偏差影响批次，需要立即评审")
                .setRemark("QA转审").setSignaturePassword("signature")
                .setIdempotencyKey(idempotencyKey);
    }

    private String payloadHash(MesProEdhrDeviationNcrCreateReqVO request) {
        JSONObject payload = new JSONObject(true);
        payload.put("sourceType", MesProEdhrNonconformanceReviewService.SOURCE_TYPE_DEVIATION);
        payload.put("batchExecutionId", request.getBatchExecutionId());
        payload.put("deviationIds", request.getDeviationIds());
        payload.put("nonconformanceReason", request.getNonconformanceReason());
        payload.put("remark", request.getRemark());
        return DigestUtil.sha256Hex(JSON.toJSONString(payload));
    }
}
