package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleasePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleasePrecheckReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseSubmitReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionSignatureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionSignatureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionSignatureMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionSignatureMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.USER_PASSWORD_FAILED;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Import(MesProEdhrReleaseServiceImpl.class)
class MesProEdhrReleaseServiceImplTest extends BaseDbUnitTest {

    @Resource
    private MesProEdhrReleaseService releaseService;
    @Resource
    private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Resource
    private MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    @Resource
    private MesProBatchRecordExecutionMapper executionMapper;
    @Resource
    private MesProBatchRecordExecutionSignatureMapper signatureMapper;
    @Resource
    private MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    @Resource
    private MesProEdhrBatchExecutionSignatureMapper batchSignatureMapper;
    @Resource
    private MesProEdhrWorkTaskAssignmentRuleMapper assignmentRuleMapper;
    @MockitoBean
    private AdminUserApi adminUserApi;
    @MockitoBean
    private MesProEdhrWorkTaskService workTaskService;
    @MockitoBean
    private MesProEdhrOperationAuditService operationAuditService;

    @Test
    void precheckFailsWhenOrdinaryProcessMissingSubmitSignature() {
        MesProEdhrBatchExecutionDO batch = insertClosedBatch("BATCH-REL-NO-SUBMIT-SIGNATURE");
        MesProEdhrBatchExecutionTaskDO task = insertApprovedOrdinaryTask(batch.getId(), 7101L);
        insertCompletedExecution(task.getExecutionId(), false);

        MesProEdhrReleaseRespVO result = releaseService.precheck(new MesProEdhrReleasePrecheckReqVO()
                .setBatchExecutionId(batch.getId()));

        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_FAIL, result.getDhrStatus());
    }

    @Test
    void precheckDhrCompletenessPassesWithoutOrdinaryReviewApproveTasksWhenFillSignedAndClosed() {
        MesProEdhrBatchExecutionDO batch = insertClosedBatch("BATCH-REL-SUBMIT-SIGNED");
        MesProEdhrBatchExecutionTaskDO task = insertApprovedOrdinaryTask(batch.getId(), 7201L);
        insertCompletedExecution(task.getExecutionId(), true);

        MesProEdhrReleaseRespVO result = releaseService.precheck(new MesProEdhrReleasePrecheckReqVO()
                .setBatchExecutionId(batch.getId()));

        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS, result.getDhrStatus());
    }

    @Test
    void precheckDhrCompletenessIgnoresNotIntegratedSpecialReportTasksWhenOrdinaryEvidenceComplete() {
        MesProEdhrBatchExecutionDO batch = insertReadyToCloseBatch("BATCH-REL-SPECIAL-NODES-NOT-INTEGRATED");
        MesProEdhrBatchExecutionTaskDO task = insertApprovedOrdinaryTask(batch.getId(), 7251L);
        insertCompletedExecution(task.getExecutionId(), true);
        insertWaitingSpecialTask(batch.getId(), "INCOMING_INSPECTION_REPORT");
        insertWaitingSpecialTask(batch.getId(), "STERILIZATION_REPORT");
        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(batch.getId())
                .setTaskTotal(3)
                .setTaskApprovedCount(1));

        MesProEdhrReleaseRespVO result = releaseService.precheck(new MesProEdhrReleasePrecheckReqVO()
                .setBatchExecutionId(batch.getId()));

        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS, result.getDhrStatus());
        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_PASSED, result.getReleaseStatus());
    }

    @Test
    void submitReleasesDirectlyWhenOwnerSignsAndDhrPassesAndExternalSourcesAreNotYetIntegrated() {
        MesProEdhrBatchExecutionDO batch = insertClosedBatch("BATCH-REL-DHR-PASS-SOURCE-NA");
        insertRouteCloseOwnerRule(batch.getRouteId(), 10001L);
        MesProEdhrBatchExecutionTaskDO task = insertApprovedOrdinaryTask(batch.getId(), 7301L);
        insertCompletedExecution(task.getExecutionId(), true);

        MesProEdhrReleaseRespVO precheck = releaseService.precheck(new MesProEdhrReleasePrecheckReqVO()
                .setBatchExecutionId(batch.getId()));

        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_PASSED, precheck.getReleaseStatus());
        assertEquals(0, precheck.getFailedCheckCount());
        assertEquals(0, precheck.getBlockingCheckCount());
        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE, precheck.getInspectionStatus());
        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE, precheck.getDeviationStatus());
        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE, precheck.getReworkStatus());
        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE, precheck.getScrapStatus());
        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE, precheck.getInventoryStatus());

        MesProEdhrReleaseRespVO submitted;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10001L);
            submitted = releaseService.submit(new MesProEdhrReleaseSubmitReqVO()
                    .setReleaseTransactionId(precheck.getReleaseTransactionId())
                    .setIdempotencyKey("submit-dhr-pass-source-na")
                    .setPassword("owner-sign-secret")
                    .setSubmitReason("DHR 完整且外部来源未接入项按非阻塞信息项处理"));
        }

        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_RELEASED, submitted.getReleaseStatus());
        assertEquals(10001L, submitted.getSubmittedBy());
        assertEquals(10001L, submitted.getApprovedBy());
        assertNotNull(submitted.getSubmittedAt());
        assertNotNull(submitted.getApprovedAt());
        assertNotNull(submitted.getApprovalSignoffEvidenceHash());
        verify(adminUserApi).validatePassword(10001L, "owner-sign-secret");
        verify(workTaskService, never()).createReleaseApprovalTaskAfterSubmit(any(), any());
        List<MesProEdhrBatchExecutionSignatureDO> signatures =
                batchSignatureMapper.selectListByBatchExecutionId(batch.getId());
        assertEquals(1, signatures.size());
        assertEquals("BATCH_RELEASE", signatures.get(0).getActionType());
        assertEquals(10001L, signatures.get(0).getActorId());
        assertEquals(Boolean.TRUE, signatures.get(0).getPasswordVerified());
        assertFalse(signatures.get(0).getAggregateHash().isBlank());
        assertEquals(signatures.get(0).getAggregateHash(), submitted.getApprovalSignoffEvidenceHash());
    }

    @Test
    void submitReleasesDirectlyBeforeBatchCloseWhenOwnerSignsAndDhrEvidenceIsComplete() {
        MesProEdhrBatchExecutionDO batch = insertReadyToCloseBatch("BATCH-REL-PRE-CLOSE-DHR-PASS");
        insertRouteCloseOwnerRule(batch.getRouteId(), 10001L);
        MesProEdhrBatchExecutionTaskDO task = insertApprovedOrdinaryTask(batch.getId(), 7401L);
        insertCompletedExecution(task.getExecutionId(), true);

        MesProEdhrReleaseRespVO precheck = releaseService.precheck(new MesProEdhrReleasePrecheckReqVO()
                .setBatchExecutionId(batch.getId()));

        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_PASSED, precheck.getReleaseStatus());
        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS, precheck.getDhrStatus());

        MesProEdhrReleaseRespVO submitted;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10001L);
            submitted = releaseService.submit(new MesProEdhrReleaseSubmitReqVO()
                    .setReleaseTransactionId(precheck.getReleaseTransactionId())
                    .setIdempotencyKey("submit-pre-close-dhr-pass")
                    .setPassword("owner-sign-secret"));
        }

        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_RELEASED, submitted.getReleaseStatus());
        verify(workTaskService, never()).createReleaseApprovalTaskAfterSubmit(any(), any());
    }

    @Test
    void submitRejectsWhenCurrentUserIsNotRouteCloseOwner() {
        MesProEdhrBatchExecutionDO batch = insertClosedBatch("BATCH-REL-NON-OWNER");
        insertRouteCloseOwnerRule(batch.getRouteId(), 10001L);
        MesProEdhrBatchExecutionTaskDO task = insertApprovedOrdinaryTask(batch.getId(), 7501L);
        insertCompletedExecution(task.getExecutionId(), true);
        MesProEdhrReleaseRespVO precheck = releaseService.precheck(new MesProEdhrReleasePrecheckReqVO()
                .setBatchExecutionId(batch.getId()));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10002L);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> releaseService.submit(new MesProEdhrReleaseSubmitReqVO()
                            .setReleaseTransactionId(precheck.getReleaseTransactionId())
                            .setIdempotencyKey("submit-non-owner")
                            .setPassword("non-owner-secret")
                            .setSubmitReason("非负责人尝试放行")));
            assertEquals(1_040_750_435, exception.getCode());
        }

        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_PASSED,
                releaseTransactionMapper.selectById(precheck.getReleaseTransactionId()).getReleaseStatus());
        assertEquals(0, batchSignatureMapper.selectListByBatchExecutionId(batch.getId()).size());
        verify(workTaskService, never()).createReleaseApprovalTaskAfterSubmit(any(), any());
    }

    @Test
    void submitRejectsWhenOwnerSignaturePasswordIsMissingOrInvalid() {
        MesProEdhrBatchExecutionDO missingPasswordBatch = insertClosedBatch("BATCH-REL-MISSING-SIGNATURE");
        insertRouteCloseOwnerRule(missingPasswordBatch.getRouteId(), 10001L);
        MesProEdhrBatchExecutionTaskDO missingPasswordTask =
                insertApprovedOrdinaryTask(missingPasswordBatch.getId(), 7601L);
        insertCompletedExecution(missingPasswordTask.getExecutionId(), true);
        MesProEdhrReleaseRespVO missingPasswordPrecheck = releaseService.precheck(new MesProEdhrReleasePrecheckReqVO()
                .setBatchExecutionId(missingPasswordBatch.getId()));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10001L);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> releaseService.submit(new MesProEdhrReleaseSubmitReqVO()
                            .setReleaseTransactionId(missingPasswordPrecheck.getReleaseTransactionId())
                            .setIdempotencyKey("submit-missing-owner-signature")
                            .setPassword(" ")));
            assertEquals(1_040_750_436, exception.getCode());
        }
        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_PASSED,
                releaseTransactionMapper.selectById(missingPasswordPrecheck.getReleaseTransactionId()).getReleaseStatus());
        assertEquals(0, batchSignatureMapper.selectListByBatchExecutionId(missingPasswordBatch.getId()).size());

        MesProEdhrBatchExecutionDO invalidPasswordBatch = insertClosedBatch("BATCH-REL-INVALID-SIGNATURE");
        insertRouteCloseOwnerRule(invalidPasswordBatch.getRouteId(), 10001L);
        MesProEdhrBatchExecutionTaskDO invalidPasswordTask =
                insertApprovedOrdinaryTask(invalidPasswordBatch.getId(), 7602L);
        insertCompletedExecution(invalidPasswordTask.getExecutionId(), true);
        MesProEdhrReleaseRespVO invalidPasswordPrecheck = releaseService.precheck(new MesProEdhrReleasePrecheckReqVO()
                .setBatchExecutionId(invalidPasswordBatch.getId()));
        doThrow(new ServiceException(USER_PASSWORD_FAILED)).when(adminUserApi).validatePassword(10001L, "wrong-pass");

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10001L);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> releaseService.submit(new MesProEdhrReleaseSubmitReqVO()
                            .setReleaseTransactionId(invalidPasswordPrecheck.getReleaseTransactionId())
                            .setIdempotencyKey("submit-invalid-owner-signature")
                            .setPassword("wrong-pass")));
            assertEquals(USER_PASSWORD_FAILED.getCode(), exception.getCode());
        }
        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_PASSED,
                releaseTransactionMapper.selectById(invalidPasswordPrecheck.getReleaseTransactionId()).getReleaseStatus());
        assertEquals(0, batchSignatureMapper.selectListByBatchExecutionId(invalidPasswordBatch.getId()).size());
        verify(workTaskService, never()).createReleaseApprovalTaskAfterSubmit(any(), any());
    }

    @Test
    void precheckRestartsRejectedReleaseTransactionByBatchExecutionId() {
        MesProEdhrBatchExecutionDO batch = insertClosedBatch("E2E-REL-RETURNED-REJECTED");
        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(batch.getId())
                .setBatchExecutionCode("EDHRB-1784554940202"));
        batch = batchExecutionMapper.selectById(batch.getId());
        MesProEdhrBatchExecutionTaskDO task = insertApprovedOrdinaryTask(batch.getId(), randomLongId());
        insertCompletedExecution(task.getExecutionId(), true);
        Long transactionId = randomLongId();
        releaseTransactionMapper.insert(MesProEdhrReleaseTransactionDO.builder()
                .id(transactionId)
                .releaseCode("EDHR-REL-RETURNED-" + transactionId)
                .batchExecutionId(batch.getId())
                .batchExecutionCode(batch.getBatchExecutionCode())
                .workOrderId(batch.getWorkOrderId())
                .workOrderCode(batch.getWorkOrderCode())
                .batchCode(batch.getBatchCode())
                .productId(batch.getProductId())
                .productCode(batch.getProductCode())
                .productName(batch.getProductName())
                .routeId(batch.getRouteId())
                .routeCode(batch.getRouteCode())
                .routeName(batch.getRouteName())
                .releaseStatus(MesProEdhrReleaseServiceImpl.STATUS_REJECTED)
                .dhrStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS)
                .inspectionStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE)
                .deviationStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE)
                .reworkStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE)
                .scrapStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE)
                .inventoryStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE)
                .requiredCheckCount(6)
                .failedCheckCount(0)
                .blockingCheckCount(0)
                .submittedBy(10001L)
                .submittedAt(LocalDateTime.now())
                .rejectedBy(10002L)
                .rejectedAt(LocalDateTime.now())
                .rejectReason("资料需重新确认")
                .version(1)
                .build());

        MesProEdhrReleaseRespVO result = releaseService.precheck(new MesProEdhrReleasePrecheckReqVO()
                .setBatchExecutionId(batch.getId()));

        assertEquals(transactionId, result.getReleaseTransactionId());
        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_PASSED, result.getReleaseStatus());
        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS, result.getDhrStatus());
        assertEquals(0, result.getFailedCheckCount());
        assertEquals(0, result.getBlockingCheckCount());
    }

    @Test
    void pageFiltersPendingApprovalBeforeBatchPagination() {
        MesProEdhrBatchExecutionDO pendingBatch = insertClosedBatch(910000L, "BATCH-REL-PENDING-SECOND-PAGE");
        releaseTransactionMapper.insert(MesProEdhrReleaseTransactionDO.builder()
                .id(920000L)
                .releaseCode("EDHR-REL-PENDING-SECOND-PAGE")
                .batchExecutionId(pendingBatch.getId())
                .batchExecutionCode(pendingBatch.getBatchExecutionCode())
                .workOrderId(pendingBatch.getWorkOrderId())
                .workOrderCode(pendingBatch.getWorkOrderCode())
                .batchCode(pendingBatch.getBatchCode())
                .productId(pendingBatch.getProductId())
                .productCode(pendingBatch.getProductCode())
                .productName(pendingBatch.getProductName())
                .routeId(pendingBatch.getRouteId())
                .routeCode(pendingBatch.getRouteCode())
                .routeName(pendingBatch.getRouteName())
                .releaseStatus(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL)
                .dhrStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS)
                .inspectionStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS)
                .deviationStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS)
                .reworkStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS)
                .scrapStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS)
                .inventoryStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS)
                .requiredCheckCount(6)
                .failedCheckCount(0)
                .blockingCheckCount(0)
                .submittedBy(10001L)
                .submittedAt(LocalDateTime.now())
                .version(1)
                .build());
        for (int i = 1; i <= 20; i++) {
            insertClosedBatch(910000L + i, "BATCH-REL-NO-TX-" + i);
        }
        MesProEdhrReleasePageReqVO reqVO = new MesProEdhrReleasePageReqVO()
                .setReleaseStatus(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL)
                .setBatchExecutionCode(pendingBatch.getBatchExecutionCode());
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);

        PageResult<MesProEdhrReleaseRespVO> page = releaseService.getPage(reqVO);

        assertEquals(1L, page.getTotal());
        assertEquals(1, page.getList().size());
        assertEquals(pendingBatch.getId(), page.getList().get(0).getBatchExecutionId());
        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL,
                page.getList().get(0).getReleaseStatus());
    }

    @Test
    void pageCompletedTraceIncludesReleasedClosedBatchAndTerminalStatusesBeforePagination() {
        MesProEdhrBatchExecutionDO releasedClosedBatch = insertClosedBatch(930000L, "BATCH-TRACE-RELEASED-CLOSED");
        releaseTransactionMapper.insert(MesProEdhrReleaseTransactionDO.builder()
                .id(931000L)
                .releaseCode("EDHR-REL-COMPLETED-CLOSED")
                .batchExecutionId(releasedClosedBatch.getId())
                .batchExecutionCode(releasedClosedBatch.getBatchExecutionCode())
                .workOrderId(releasedClosedBatch.getWorkOrderId())
                .workOrderCode(releasedClosedBatch.getWorkOrderCode())
                .batchCode(releasedClosedBatch.getBatchCode())
                .productId(releasedClosedBatch.getProductId())
                .productCode(releasedClosedBatch.getProductCode())
                .productName(releasedClosedBatch.getProductName())
                .routeId(releasedClosedBatch.getRouteId())
                .routeCode(releasedClosedBatch.getRouteCode())
                .routeName(releasedClosedBatch.getRouteName())
                .releaseStatus(MesProEdhrReleaseServiceImpl.STATUS_RELEASED)
                .dhrStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS)
                .inspectionStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS)
                .deviationStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS)
                .reworkStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS)
                .scrapStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS)
                .inventoryStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS)
                .requiredCheckCount(6)
                .failedCheckCount(0)
                .blockingCheckCount(0)
                .submittedBy(10001L)
                .submittedAt(LocalDateTime.now())
                .approvedBy(10001L)
                .approvedAt(LocalDateTime.now())
                .version(1)
                .build());
        MesProEdhrBatchExecutionDO archivedBatch = insertClosedBatch(930001L, "BATCH-TRACE-ARCHIVED");
        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(archivedBatch.getId())
                .setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_ARCHIVED));
        MesProEdhrBatchExecutionDO rejectedBatch = insertClosedBatch(930002L, "BATCH-TRACE-REJECTED");
        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(rejectedBatch.getId())
                .setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_REJECTED));
        insertClosedBatch(930003L, "BATCH-TRACE-STILL-EXECUTING-CLOSED");

        MesProEdhrReleasePageReqVO reqVO = new MesProEdhrReleasePageReqVO()
                .setCompletedTraceOnly(true);
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        PageResult<MesProEdhrReleaseRespVO> page = releaseService.getPage(reqVO);
        List<Long> batchIds = page.getList().stream()
                .map(MesProEdhrReleaseRespVO::getBatchExecutionId)
                .toList();

        assertEquals(3L, page.getTotal());
        assertTrue(batchIds.contains(releasedClosedBatch.getId()));
        assertTrue(batchIds.contains(archivedBatch.getId()));
        assertTrue(batchIds.contains(rejectedBatch.getId()));
    }

    private MesProEdhrBatchExecutionDO insertClosedBatch(String batchCode) {
        return insertClosedBatch(null, batchCode);
    }

    private MesProEdhrBatchExecutionDO insertReadyToCloseBatch(String batchCode) {
        MesProEdhrBatchExecutionDO batch = insertClosedBatch(batchCode);
        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(batch.getId())
                .setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_READY_TO_CLOSE)
                .setAggregateHash(null)
                .setClosedBy(null)
                .setClosedAt(null));
        return batchExecutionMapper.selectById(batch.getId());
    }

    private MesProEdhrBatchExecutionDO insertClosedBatch(Long id, String batchCode) {
        MesProEdhrBatchExecutionDO batch = MesProEdhrBatchExecutionDO.builder()
                .id(id)
                .batchExecutionCode("EDHR-BATCH-" + randomLongId())
                .workOrderId(randomLongId())
                .workOrderCode("WO-" + randomLongId())
                .batchCode(batchCode)
                .productId(randomLongId())
                .productCode("PRODUCT-" + randomLongId())
                .productName("测试产品")
                .routeId(randomLongId())
                .routeCode("ROUTE-" + randomLongId())
                .routeName("测试路线")
                .status(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED)
                .taskTotal(1)
                .taskApprovedCount(1)
                .blockedCount(0)
                .aggregateHash("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                .closedBy(10001L)
                .closedAt(LocalDateTime.now())
                .build();
        batchExecutionMapper.insert(batch);
        return batch;
    }

    private MesProEdhrBatchExecutionTaskDO insertApprovedOrdinaryTask(Long batchExecutionId, Long executionId) {
        MesProEdhrBatchExecutionTaskDO task = MesProEdhrBatchExecutionTaskDO.builder()
                .batchExecutionId(batchExecutionId)
                .nodeType("ROUTE_FORM")
                .routeProcessId(randomLongId())
                .routeProcessSort(1)
                .processId(randomLongId())
                .processCode("PROC-" + randomLongId())
                .processName("普通工序")
                .batchRecordReportId("RPT-" + randomLongId())
                .batchRecordReportName("普通工序记录")
                .batchRecordSort(1)
                .executionMode("SEQUENTIAL")
                .recordCategory("BATCH_RECORD")
                .validationProfile("CONTROLLED_BATCH")
                .requiredPolicy("REQUIRED")
                .ownerRoleKey("PRODUCTION")
                .archiveVisibility("FINAL_DHR")
                .executionId(executionId)
                .status(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED)
                .requiredFlag(Boolean.TRUE)
                .submittedAt(LocalDateTime.now())
                .approvedAt(LocalDateTime.now())
                .build();
        batchTaskMapper.insert(task);
        return task;
    }

    private void insertRouteCloseOwnerRule(Long routeId, Long ownerUserId) {
        assignmentRuleMapper.insert(MesProEdhrWorkTaskAssignmentRuleDO.builder()
                .scopeType("ROUTE")
                .scopeId(routeId)
                .taskType(MesProEdhrWorkTaskService.TASK_TYPE_CLOSE)
                .assigneeUserId(ownerUserId)
                .candidateSourceType("USER")
                .candidateSourceId(ownerUserId)
                .enabled(Boolean.TRUE)
                .build());
    }

    private void insertWaitingSpecialTask(Long batchExecutionId, String nodeType) {
        batchTaskMapper.insert(MesProEdhrBatchExecutionTaskDO.builder()
                .batchExecutionId(batchExecutionId)
                .nodeType(nodeType)
                .routeProcessSort(2)
                .processName(nodeType)
                .recordCategory("SPECIAL_REPORT")
                .validationProfile("EXTERNAL_SOURCE")
                .requiredPolicy("REQUIRED")
                .ownerRoleKey("QUALITY")
                .archiveVisibility("FINAL_DHR")
                .status(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING)
                .requiredFlag(Boolean.TRUE)
                .build());
    }

    private void insertCompletedExecution(Long executionId, boolean includeSubmitSignature) {
        executionMapper.insert(MesProBatchRecordExecutionDO.builder()
                .id(executionId)
                .executionCode("BRE-" + randomLongId())
                .templateId(randomLongId())
                .templateCode("TPL-" + randomLongId())
                .templateName("普通工序模板")
                .workOrderId(randomLongId())
                .workOrderCode("WO-" + randomLongId())
                .routeProcessId(randomLongId())
                .batchRecordReportId("RPT-" + randomLongId())
                .batchCode("BATCH")
                .status(4)
                .sheetLayoutJson("{\"rows\":{}}")
                .metaJson("{}")
                .executionSnapshotJson("{\"layout\":{\"rows\":{}},\"fields\":[]}")
                .cellValuesJson("[{\"rowIndex\":1,\"columnIndex\":1,\"value\":\"已填写\"}]")
                .cellValuesHash("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
                .fieldAuditRevision(1L)
                .fieldAuditHeadHash("cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc")
                .submittedBy(10001L)
                .submittedAt(LocalDateTime.now())
                .closedAt(LocalDateTime.now())
                .domainTraceStatus("VERIFIED")
                .build());
        if (includeSubmitSignature) {
            signatureMapper.insert(MesProBatchRecordExecutionSignatureDO.builder()
                    .executionId(executionId)
                    .actorId(10001L)
                    .actorName("生产填写人")
                    .actionType("SUBMIT")
                    .signatureMode("PASSWORD")
                    .passwordVerified(Boolean.TRUE)
                    .signedAt(LocalDateTime.now())
                    .fieldAuditRevision(1L)
                    .fieldAuditHeadHash("cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc")
                    .cellValuesHash("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
                    .build());
        }
    }
}
