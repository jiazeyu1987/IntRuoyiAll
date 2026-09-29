package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchExecutionRejectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewDisposeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewCounterDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewCounterMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStatus;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_NONCONFORMANCE_REVIEW_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_NONCONFORMANCE_REVIEW_SOURCE_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PASSWORD_INVALID;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProEdhrNonconformanceReviewApplicationScopeTest {

    @BeforeAll
    static void initializeMyBatisMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), FileDO.class.getName()), FileDO.class);
    }

    private static final String REVIEW_MATERIAL_URL =
            "http://localhost:48081/admin-api/infra/file/10/get/review.pdf";
    private static final Long REVIEW_MATERIAL_FILE_ID = 9102L;

    @Mock private MesProEdhrNonconformanceReviewMapper reviewMapper;
    @Mock private MesProEdhrNonconformanceReviewCounterMapper reviewCounterMapper;
    @Mock private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Mock private MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper;
    @Mock private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock private cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;
    @Mock private MesProProcessPoolEventMapper processPoolEventMapper;
    @Mock private MesProWorkOrderMapper workOrderMapper;
    @Mock private MesProEdhrWorkTaskMapper workTaskMapper;
    @Mock private MesProBatchRecordExecutionSignatureService signatureService;
    @Mock private MesProEdhrBatchExecutionOriginMapper batchExecutionOriginMapper;
    @Mock private MesPqcInspectionTaskMapper pqcInspectionTaskMapper;
    @Mock private MesProEdhrOperationAuditService operationAuditService;
    @Mock private FileMapper fileMapper;
    @Mock private cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService unifiedAudit;
    private final java.util.Map<String, Long> materialIds = new java.util.HashMap<>();

    @Mock private cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper signatureRecordMapper;
    @Mock private cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventMapper auditReceiptMapper;
    private java.util.function.UnaryOperator<cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO>
            signatureMutation = java.util.function.UnaryOperator.identity();
    private cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO lastSignature;
    private MesProEdhrNonconformanceReviewServiceImpl service;

    @org.junit.jupiter.api.AfterEach
    void clearIdentity() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    @BeforeEach
    void setUp() {
        service = new MesProEdhrNonconformanceReviewServiceImpl();
        // Type-based optional injection keeps the pre-implementation RED at the missing behavior,
        // not at ReflectionTestUtils failing because the production dependency does not exist yet.
        org.springframework.util.ReflectionUtils.doWithFields(service.getClass(), field -> {
            if (field.getType() == cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService.class) {
                org.springframework.util.ReflectionUtils.makeAccessible(field);
                field.set(service, unifiedAudit);
            }
            if (field.getType() == cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventMapper.class) {
                org.springframework.util.ReflectionUtils.makeAccessible(field);
                field.set(service, auditReceiptMapper);
            }
        });
        lenient().when(auditReceiptMapper.selectByIdempotencyKeyForUpdate(eq(122L), any()))
                .thenAnswer(call -> receiptFromLastAppend());
        ReflectionTestUtils.setField(service, "reviewMapper", reviewMapper);
        ReflectionTestUtils.setField(service, "reviewCounterMapper", reviewCounterMapper);
        ReflectionTestUtils.setField(service, "batchExecutionMapper", batchExecutionMapper);
        ReflectionTestUtils.setField(service, "releaseApplicationMapper", releaseApplicationMapper);
        ReflectionTestUtils.setField(service, "activeOrderMapper", activeOrderMapper);
        ReflectionTestUtils.setField(service, "reworkCycleService",
                new cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesActiveOrderReworkCycleService(
                        activeOrderMapper, processSnapshotMapper, pqcInspectionTaskMapper));
        lenient().when(activeOrderMapper.selectByIdForUpdate(any())).thenAnswer(invocation ->
                new MesProcessPoolActiveOrderDO().setId(invocation.getArgument(0)).setWorkOrderId(3001L)
                        .setLeaderUserId(3002L).setRouteId(4001L).setRouteVersionId(4002L)
                        .setQaRegulationVersionId(5001L).setErpFixedQuantitySnapshot(java.math.BigDecimal.TEN)
                        .setActiveStatus("ACTIVE").setBusinessStatus("COMPLETED").setVersion(1));
        lenient().when(processSnapshotMapper.selectListByActiveOrderIdForUpdate(any())).thenReturn(List.of(
                new cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO()
                        .setActiveOrderId(8101L).setWorkOrderId(3001L)));
        lenient().when(pqcInspectionTaskMapper.selectListByActiveOrderIdForUpdate(any())).thenReturn(List.of(
                new MesPqcInspectionTaskDO().setActiveOrderId(8101L).setWorkOrderId(3001L)));
        lenient().when(activeOrderMapper.retireForRework(any(), any(), any())).thenReturn(1);
        lenient().when(activeOrderMapper.insert(any(MesProcessPoolActiveOrderDO.class))).thenAnswer(invocation -> {
            ((MesProcessPoolActiveOrderDO) invocation.getArgument(0)).setId(8201L);
            return 1;
        });
        lenient().when(processSnapshotMapper.insert(any(cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO.class))).thenReturn(1);
        lenient().when(pqcInspectionTaskMapper.insert(any(MesPqcInspectionTaskDO.class))).thenReturn(1);
        ReflectionTestUtils.setField(service, "processPoolEventMapper", processPoolEventMapper);
        ReflectionTestUtils.setField(service, "workOrderMapper", workOrderMapper);
        ReflectionTestUtils.setField(service, "workTaskMapper", workTaskMapper);
        ReflectionTestUtils.setField(service, "signatureService", signatureService);
        ReflectionTestUtils.setField(service, "batchExecutionOriginMapper", batchExecutionOriginMapper);
        ReflectionTestUtils.setField(service, "pqcInspectionTaskMapper", pqcInspectionTaskMapper);
        ReflectionTestUtils.setField(service, "operationAuditService", operationAuditService);
        ReflectionTestUtils.setField(service, "fileMapper", fileMapper);
        lenient().when(signatureService.recordQaDispositionSignature(any(), any(), any(), any(), any()))
                .thenReturn(9101L);
        lenient().when(reviewMapper.updateById(any(MesProEdhrNonconformanceReviewDO.class))).thenReturn(1);
        lenient().when(batchExecutionMapper.updateById(any(MesProEdhrBatchExecutionDO.class))).thenReturn(1);
        TenantContextHolder.setTenantId(122L);
        var actor = new cn.iocoder.yudao.framework.security.core.LoginUser();
        actor.setId(21L);
        actor.setTenantId(122L);
        actor.setUserType(2);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        actor, null, List.of()));
        ReflectionTestUtils.setField(service, "signatureRecordMapper", signatureRecordMapper);
        var signatureQuery = org.mockito.Mockito.mock(cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService.class);
        ReflectionTestUtils.setField(service, "signatureQueryService", signatureQuery);
        lenient().when(signatureQuery.verifyEvidence(any())).thenAnswer(call ->
                new cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureVerificationDTO(
                        call.getArgument(0), "VALID", lastSignature.getContentHash(),
                        cn.hutool.crypto.digest.DigestUtil.sha256Hex(lastSignature.getCanonicalContentJson()),
                        "fixture-evidence", "fixture-evidence", "SHA-256", null));
        lenient().when(signatureRecordMapper.selectById(any())).thenAnswer(call -> {
            var invocation = org.mockito.Mockito.mockingDetails(signatureService).getInvocations().stream()
                    .filter(i -> java.util.Set.of("recordQaDispositionSignature", "recordNonconformanceReviewCreateSignature",
                            "recordBatchActionSignature").contains(i.getMethod().getName()))
                    .reduce((first, second) -> second).orElseThrow();
            boolean batch = invocation.getMethod().getName().equals("recordBatchActionSignature");
            boolean create = invocation.getMethod().getName().equals("recordNonconformanceReviewCreateSignature");
            String action = batch ? invocation.getArgument(4) : create ? "NONCONFORMANCE_REVIEW_CREATE" : "QA_DISPOSITION";
            String subject = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(batch ? invocation.getArgument(1) : 0L, action,
                    null, null, null, null, null, null, null, batch ? "EDHR_BATCH" : "EDHR_NONCONFORMANCE_REVIEW",
                    invocation.getArgument(1), batch ? invocation.getArgument(5) : create ? "eDHR不合格评审创建" : "eDHR不合格评审处置", action,
                    null, null, invocation.getArgument(batch ? 6 : 4), null);
            var snapshot = new MesBatchRecordSignatureSubjectAdapter().loadAndAuthorize(
                    new cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand(
                            21L, "MES", action, "MES_BATCH_RECORD", subject,
                            MesBatchRecordSignatureSubjectAdapter.subjectVersion(subject), invocation.getArgument(3)));
            var fields = new java.util.TreeMap<String, Object>(
                    com.alibaba.fastjson.JSON.parseObject(snapshot.canonicalContentJson()));
            String canonical = cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(fields);
            lastSignature = cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO.builder()
                    .id(call.getArgument(0)).moduleCode("MES").actionCode(action)
                    .subjectType("MES_BATCH_RECORD").subjectId(subject)
                    .subjectVersion(MesBatchRecordSignatureSubjectAdapter.subjectVersion(subject))
                    .actorId(21L).reason(invocation.getArgument(3)).canonicalContentJson(canonical)
                    .contentHash(cn.hutool.crypto.digest.DigestUtil.sha256Hex(canonical))
                    .evidenceHash("fixture-evidence")
                    .verificationStatus("VALID").build();
            lastSignature.setTenantId(122L);
            return signatureMutation.apply(lastSignature);
        });
        lenient().when(reviewCounterMapper.insertOrIncrement(any())).thenReturn(1);
        lenient().when(reviewCounterMapper.selectByTenantIdForUpdate(any())).thenReturn(
                MesProEdhrNonconformanceReviewCounterDO.builder().tenantId(122L).currentSerial(0L).build());
        materialIds.clear();
        stubFormalFile(REVIEW_MATERIAL_URL, FileDO.builder().id(REVIEW_MATERIAL_FILE_ID)
                .configId(10L).name("review.pdf").path("review.pdf").url(REVIEW_MATERIAL_URL).build());
        lenient().when(signatureService.recordNonconformanceReviewCreateSignature(
                any(), any(), any(), any(), any())).thenReturn(9200L);
    }

    @Test
    void creatingApplicationReviewRecordsActiveOrderOperationFact() {
        when(releaseApplicationMapper.selectByIdForUpdate(7001L)).thenReturn(
                new MesProcessPoolActiveOrderReleaseApplicationDO()
                        .setId(7001L)
                        .setActiveOrderId(8101L)
                        .setApplicationStatus(MesReleaseFlowStatus.PQC_RELEASE_PENDING)
                        .setVersion(1)
                        .setWorkOrderId(3001L)
                        .setWorkOrderCode("WO-001")
                        .setBatchCode("BATCH-001"));
        when(workOrderMapper.selectByIdForUpdate(3001L)).thenReturn(
                new MesProWorkOrderDO().setId(3001L).setTemporaryFrozen(false));
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3001L), true)).thenReturn(1);
        when(reviewMapper.insert(any(MesProEdhrNonconformanceReviewDO.class))).thenAnswer(invocation -> {
            invocation.<MesProEdhrNonconformanceReviewDO>getArgument(0).setId(1001L);
            return 1;
        });

        service.create(new MesProEdhrNonconformanceReviewCreateReqVO()
                .setSourceType("PQC_RELEASE")
                .setSourceId(7001L)
                .setNonconformanceReason("检验结论需要评审")
                .setSignaturePassword("create-password"));

        ArgumentCaptor<MesProEdhrOperationAuditCommand> auditCaptor =
                ArgumentCaptor.forClass(MesProEdhrOperationAuditCommand.class);
        verify(operationAuditService).recordInCallerTransaction(auditCaptor.capture());
        assertEquals("NONCONFORMANCE_REVIEW_CREATE", auditCaptor.getValue().getOperationType());
        assertTrue(auditCaptor.getValue().getMetadataJson().contains("\"activeOrderId\":8101"));
        ArgumentCaptor<MesProEdhrNonconformanceReviewDO> reviewCaptor =
                ArgumentCaptor.forClass(MesProEdhrNonconformanceReviewDO.class);
        verify(reviewMapper).insert(reviewCaptor.capture());
        assertEquals(8101L, reviewCaptor.getValue().getActiveOrderId());
        assertCreationAudit("1001", "PQC_RELEASE", "7001", "9200", "检验结论需要评审");
    }

    @Test
    void activeOrderCanStartReviewBeforeCompletionWithoutDownstreamBatch() {
        when(activeOrderMapper.selectByIdForUpdate(8101L)).thenReturn(new MesProcessPoolActiveOrderDO()
                .setId(8101L).setActiveStatus("ACTIVE").setBusinessStatus("ACTIVE").setWorkOrderId(3001L));
        when(batchExecutionOriginMapper.selectListByTraceFilter(8101L, 3001L, null, null))
                .thenReturn(List.of());
        lenient().when(workOrderMapper.selectByIdForUpdate(3001L)).thenReturn(
                new MesProWorkOrderDO().setId(3001L).setCode("WO-001")
                        .setBatchCode("BATCH-001").setTemporaryFrozen(false));
        lenient().when(workOrderMapper.updateTemporaryFrozenByIds(List.of(3001L), true)).thenReturn(1);
        lenient().when(reviewMapper.insert(any(MesProEdhrNonconformanceReviewDO.class))).thenAnswer(invocation -> {
            invocation.<MesProEdhrNonconformanceReviewDO>getArgument(0).setId(1202L);
            return 1;
        });

        MesProEdhrNonconformanceReviewRespVO result = service.create(
                new MesProEdhrNonconformanceReviewCreateReqVO().setActiveOrderId(8101L)
                        .setNonconformanceReason("完工前发现检验异常"));

        assertEquals(1202L, result.getId());
        assertEquals("ACTIVE_ORDER", result.getSourceType());
        assertEquals(8101L, result.getSourceId());
        assertEquals(8101L, result.getActiveOrderId());
        assertEquals(3001L, result.getWorkOrderId());
        assertEquals("WO-001", result.getWorkOrderCode());
        assertEquals("BATCH-001", result.getBatchCode());
        assertNull(result.getBatchExecutionId());
        verify(workOrderMapper).updateTemporaryFrozenByIds(List.of(3001L), true);
        verifyNoInteractions(batchExecutionMapper);
        verify(operationAuditService).recordInCallerTransaction(argThat(command ->
                command.getMetadataJson().contains("\"activeOrderId\":8101")));
    }

    @Test
    void completedOrderWithMissingBatchStillRejectsInvalidSource() {
        when(activeOrderMapper.selectByIdForUpdate(8101L)).thenReturn(new MesProcessPoolActiveOrderDO()
                .setId(8101L).setActiveStatus("ACTIVE").setBusinessStatus("COMPLETED").setWorkOrderId(3001L));
        when(batchExecutionOriginMapper.selectListByTraceFilter(8101L, 3001L, null, null))
                .thenReturn(List.of());

        ServiceException error = assertThrows(ServiceException.class, () -> service.create(
                new MesProEdhrNonconformanceReviewCreateReqVO().setActiveOrderId(8101L)
                        .setNonconformanceReason("已完工来源损坏")));

        assertEquals(PRO_EDHR_NONCONFORMANCE_REVIEW_SOURCE_INVALID.getCode(), error.getCode());
        verify(reviewMapper, never()).insert(any(MesProEdhrNonconformanceReviewDO.class));
        verifyNoInteractions(workOrderMapper, batchExecutionMapper, operationAuditService);
    }

    @Test
    void activeOrderOnlyCreationRecordsAuditFactWithoutCreateSignature() {
        when(activeOrderMapper.selectByIdForUpdate(8101L)).thenReturn(new MesProcessPoolActiveOrderDO()
                .setId(8101L)
                .setActiveStatus("ACTIVE")
                .setWorkOrderId(3001L));
        when(reviewMapper.selectFirstBlockingByWorkOrderId(3001L)).thenReturn(null);
        when(batchExecutionOriginMapper.selectListByTraceFilter(8101L, 3001L, null, null))
                .thenReturn(List.of(new MesProEdhrBatchExecutionOriginDO()
                        .setBatchExecutionId(9001L)
                        .setActiveOrderId(8101L)
                        .setWorkOrderId(3001L)
                        .setEntryType(MesProEdhrBatchTraceFormalSourceResolver.ACTIVE_ORDER_COMPLETION)));
        when(batchExecutionMapper.selectByIdForUpdate(9001L)).thenReturn(new MesProEdhrBatchExecutionDO()
                .setId(9001L)
                .setWorkOrderId(3001L)
                .setBatchExecutionCode("EXEC-001")
                .setWorkOrderCode("WO-001")
                .setBatchCode("BATCH-001")
                .setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_IN_PROGRESS));
        when(workOrderMapper.selectByIdForUpdate(3001L)).thenReturn(
                new MesProWorkOrderDO().setId(3001L).setCode("WO-001")
                        .setBatchCode("BATCH-001").setTemporaryFrozen(false));
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3001L), true)).thenReturn(1);
        when(reviewMapper.insert(any(MesProEdhrNonconformanceReviewDO.class))).thenAnswer(invocation -> {
            invocation.<MesProEdhrNonconformanceReviewDO>getArgument(0).setId(1201L);
            return 1;
        });

        MesProEdhrNonconformanceReviewRespVO result = service.create(
                new MesProEdhrNonconformanceReviewCreateReqVO()
                        .setActiveOrderId(8101L)
                        .setNonconformanceReason("活跃订单直接创建评审"));

        assertEquals(1201L, result.getId());
        assertEquals("ACTIVE_ORDER", result.getSourceType());
        assertEquals(8101L, result.getSourceId());
        assertEquals(8101L, result.getActiveOrderId());
        assertTrue(result.getReviewCode().matches("BHGSP-\\d{6}-00000000"));
        verify(activeOrderMapper).selectByIdForUpdate(8101L);
        verify(signatureService, never()).recordNonconformanceReviewCreateSignature(
                any(), any(), any(), any(), any());
        ArgumentCaptor<MesProEdhrOperationAuditCommand> auditCaptor =
                ArgumentCaptor.forClass(MesProEdhrOperationAuditCommand.class);
        verify(operationAuditService).recordInCallerTransaction(auditCaptor.capture());
        assertFalse(auditCaptor.getValue().getMetadataJson().contains("\"signatureId\":9200"));
    }

    @Test
    void legacyCreationStillRequiresSignature() {
        ServiceException error = assertThrows(ServiceException.class, () -> service.create(
                new MesProEdhrNonconformanceReviewCreateReqVO()
                        .setSourceType("PQC_RELEASE")
                        .setBatchExecutionId(9001L)
                        .setNonconformanceReason("legacy入口缺少签名")));

        assertEquals(PRO_EDHR_NONCONFORMANCE_REVIEW_REQUIRED.getCode(), error.getCode());
        verifyNoInteractions(batchExecutionMapper, reviewMapper, workOrderMapper, signatureService,
                operationAuditService);
    }

    @Test
    void activeOrderCreationUsesTenantCounterContinuouslyAcrossReviews() {
        when(activeOrderMapper.selectByIdForUpdate(8101L)).thenReturn(new MesProcessPoolActiveOrderDO()
                .setId(8101L).setActiveStatus("ACTIVE").setWorkOrderId(3001L));
        when(activeOrderMapper.selectByIdForUpdate(8102L)).thenReturn(new MesProcessPoolActiveOrderDO()
                .setId(8102L).setActiveStatus("ACTIVE").setWorkOrderId(3002L));
        when(reviewMapper.selectFirstBlockingByWorkOrderId(any())).thenReturn(null);
        when(batchExecutionOriginMapper.selectListByTraceFilter(8101L, 3001L, null, null))
                .thenReturn(List.of(new MesProEdhrBatchExecutionOriginDO()
                        .setBatchExecutionId(9001L)
                        .setActiveOrderId(8101L)
                        .setWorkOrderId(3001L)
                        .setEntryType(MesProEdhrBatchTraceFormalSourceResolver.ACTIVE_ORDER_COMPLETION)));
        when(batchExecutionOriginMapper.selectListByTraceFilter(8102L, 3002L, null, null))
                .thenReturn(List.of(new MesProEdhrBatchExecutionOriginDO()
                        .setBatchExecutionId(9002L)
                        .setActiveOrderId(8102L)
                        .setWorkOrderId(3002L)
                        .setEntryType(MesProEdhrBatchTraceFormalSourceResolver.ACTIVE_ORDER_COMPLETION)));
        when(batchExecutionMapper.selectByIdForUpdate(9001L)).thenReturn(new MesProEdhrBatchExecutionDO()
                .setId(9001L).setWorkOrderId(3001L).setBatchExecutionCode("EXEC-001")
                .setWorkOrderCode("WO-001").setBatchCode("BATCH-001")
                .setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_IN_PROGRESS));
        when(batchExecutionMapper.selectByIdForUpdate(9002L)).thenReturn(new MesProEdhrBatchExecutionDO()
                .setId(9002L).setWorkOrderId(3002L).setBatchExecutionCode("EXEC-002")
                .setWorkOrderCode("WO-002").setBatchCode("BATCH-002")
                .setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_IN_PROGRESS));
        when(workOrderMapper.selectByIdForUpdate(3001L)).thenReturn(
                new MesProWorkOrderDO().setId(3001L).setCode("WO-001").setBatchCode("BATCH-001")
                        .setTemporaryFrozen(false));
        when(workOrderMapper.selectByIdForUpdate(3002L)).thenReturn(
                new MesProWorkOrderDO().setId(3002L).setCode("WO-002").setBatchCode("BATCH-002")
                        .setTemporaryFrozen(false));
        when(workOrderMapper.updateTemporaryFrozenByIds(any(), eq(true))).thenReturn(1);
        when(reviewMapper.insert(any(MesProEdhrNonconformanceReviewDO.class))).thenAnswer(invocation -> {
            invocation.<MesProEdhrNonconformanceReviewDO>getArgument(0)
                    .setId(1200L + invocation.getArgument(0, MesProEdhrNonconformanceReviewDO.class).getWorkOrderId());
            return 1;
        });
        when(reviewCounterMapper.selectByTenantIdForUpdate(122L)).thenReturn(
                MesProEdhrNonconformanceReviewCounterDO.builder().tenantId(122L).currentSerial(0L).build(),
                MesProEdhrNonconformanceReviewCounterDO.builder().tenantId(122L).currentSerial(1L).build());

        MesProEdhrNonconformanceReviewRespVO first = service.create(
                new MesProEdhrNonconformanceReviewCreateReqVO().setActiveOrderId(8101L)
                        .setNonconformanceReason("第一次直接创建")
                        .setSignaturePassword("create-password"));
        MesProEdhrNonconformanceReviewRespVO second = service.create(
                new MesProEdhrNonconformanceReviewCreateReqVO().setActiveOrderId(8102L)
                        .setNonconformanceReason("第二次直接创建")
                        .setSignaturePassword("create-password"));

        assertTrue(first.getReviewCode().matches("BHGSP-\\d{6}-00000000"));
        assertTrue(second.getReviewCode().matches("BHGSP-\\d{6}-00000001"));
        verify(reviewCounterMapper, org.mockito.Mockito.times(2)).insertOrIncrement(122L);
    }

    @Test
    void allPageDelegatesStatusFilterToReviewMapper() {
        MesProEdhrNonconformanceReviewPageReqVO req = new MesProEdhrNonconformanceReviewPageReqVO();
        req.setReviewStatus("closed");
        MesProEdhrNonconformanceReviewDO row = MesProEdhrNonconformanceReviewDO.builder()
                .id(301L).reviewCode("BHGSP-202609-00000000").reviewStatus("closed").build();
        when(reviewMapper.selectPage(req)).thenReturn(new PageResult<>(List.of(row), 1L));

        PageResult<MesProEdhrNonconformanceReviewRespVO> page = service.getPage(req);

        assertEquals(1L, page.getTotal());
        assertEquals("closed", page.getList().get(0).getReviewStatus());
        verify(reviewMapper).selectPage(req);
    }

    @Test
    void activeOrderCandidatesExposeCurrentActiveOrders() {
        when(activeOrderMapper.selectActiveList()).thenReturn(java.util.List.of(
                new MesProcessPoolActiveOrderDO()
                        .setId(8101L)
                        .setWorkOrderId(3001L)
                        .setActiveStatus("ACTIVE")
                        .setBusinessStatus("ACTIVE")));
        when(workOrderMapper.selectBatchIds(java.util.List.of(3001L))).thenReturn(java.util.List.of(
                new MesProWorkOrderDO().setId(3001L).setCode("WO-001").setBatchCode("BATCH-001")));

        var candidates = service.listActiveOrderCandidates();

        assertEquals(1, candidates.size());
        assertEquals(8101L, candidates.get(0).getId());
        assertEquals("WO-001", candidates.get(0).getWorkOrderCode());
        assertEquals("BATCH-001", candidates.get(0).getBatchCode());
    }

    @Test
    void creatingReviewWithInvalidElectronicSignatureDoesNotFreezeOrRecordFact() {
        when(releaseApplicationMapper.selectByIdForUpdate(7001L)).thenReturn(
                new MesProcessPoolActiveOrderReleaseApplicationDO()
                        .setId(7001L)
                        .setActiveOrderId(8101L)
                        .setApplicationStatus(MesReleaseFlowStatus.PQC_RELEASE_PENDING)
                        .setVersion(1)
                        .setWorkOrderId(3001L)
                        .setWorkOrderCode("WO-001")
                        .setBatchCode("BATCH-001"));
        when(workOrderMapper.selectByIdForUpdate(3001L)).thenReturn(
                new MesProWorkOrderDO().setId(3001L).setTemporaryFrozen(false));
        when(reviewMapper.insert(any(MesProEdhrNonconformanceReviewDO.class))).thenAnswer(invocation -> {
            invocation.<MesProEdhrNonconformanceReviewDO>getArgument(0).setId(1001L);
            return 1;
        });
        when(signatureService.recordNonconformanceReviewCreateSignature(
                eq(21L), eq(1001L), eq("wrong-password"), eq("检验结论需要评审"), any()))
                .thenThrow(exception(PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PASSWORD_INVALID));

        assertThrows(ServiceException.class, () -> service.create(
                new MesProEdhrNonconformanceReviewCreateReqVO()
                        .setSourceType("PQC_RELEASE")
                        .setSourceId(7001L)
                        .setNonconformanceReason("检验结论需要评审")
                        .setSignaturePassword("wrong-password")));

        verify(batchExecutionMapper, never()).updateById(any(MesProEdhrBatchExecutionDO.class));
        verify(workOrderMapper, never()).updateTemporaryFrozenByIds(any(), any());
        verifyNoInteractions(operationAuditService);
    }

    @Test
    void disposingApplicationReviewRecordsActiveOrderOperationFact() {
        stubPendingReview("rework");
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3001L), false)).thenReturn(1);
        when(releaseApplicationMapper.closeFromNonconformance(eq(7001L), eq(1), eq("NONCONFORMANCE_REWORK"),
                eq(21L), any(), eq("返工处理"), any())).thenReturn(1);
        when(workTaskMapper.completePqcDecisionTask(eq(8001L), any(), eq("NONCONFORMANCE_REWORK")))
                .thenReturn(1);

        service.dispose(disposeRequest("rework"));

        ArgumentCaptor<MesProEdhrOperationAuditCommand> auditCaptor =
                ArgumentCaptor.forClass(MesProEdhrOperationAuditCommand.class);
        verify(operationAuditService).recordInCallerTransaction(auditCaptor.capture());
        assertEquals("NONCONFORMANCE_REVIEW_DISPOSE", auditCaptor.getValue().getOperationType());
        assertTrue(auditCaptor.getValue().getMetadataJson().contains("\"activeOrderId\":8101"));
    }

    @Test
    void concessionReleaseDispositionRecordsActiveOrderOperationFactWithBusinessActionName() {
        stubPendingReview("concession_release");
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3001L), false)).thenReturn(1);

        service.dispose(disposeRequest("concession_release"));

        ArgumentCaptor<MesProEdhrOperationAuditCommand> auditCaptor =
                ArgumentCaptor.forClass(MesProEdhrOperationAuditCommand.class);
        verify(operationAuditService).recordInCallerTransaction(auditCaptor.capture());
        assertEquals("NONCONFORMANCE_REVIEW_DISPOSE", auditCaptor.getValue().getOperationType());
        assertEquals("让步放行", auditCaptor.getValue().getActionName());
        assertTrue(auditCaptor.getValue().getMetadataJson().contains("\"activeOrderId\":8101"));
        assertTrue(auditCaptor.getValue().getMetadataJson().contains("\"disposition\":\"concession_release\""));
        assertTrue(auditCaptor.getValue().getMetadataJson()
                .contains("\"reviewMaterialUrl\":\"" + REVIEW_MATERIAL_URL + "\""));
        assertTrue(auditCaptor.getValue().getMetadataJson()
                .contains("\"reviewMaterialFileId\":" + REVIEW_MATERIAL_FILE_ID));
        assertTrue(auditCaptor.getValue().getMetadataJson().contains("\"reviewMaterialsJson\""));
        assertTrue(auditCaptor.getValue().getMetadataJson().contains("\"reviewOpinion\":\"让步放行\""));
        assertTrue(auditCaptor.getValue().getMetadataJson().contains("\"qaSignature\":\"电子签名#9101\""));
        assertTrue(auditCaptor.getValue().getMetadataJson().contains("\"signatureId\":9101"));
    }

    @Test
    void disposeKeepsExplicitMaterialAndDeletedEventIdentities() {
        stubPendingReview("concession_release");
        String firstUrl = "https://files.example/first/review.pdf";
        String secondUrl = "https://files.example/second/review.pdf";
        stubFormalFile(firstUrl, FileDO.builder().id(9301L).configId(10L).name("review.pdf")
                .path("first/review.pdf").url(firstUrl).build());
        stubFormalFile(secondUrl, FileDO.builder().id(9302L).configId(10L).name("review.pdf")
                .path("second/review.pdf").url(secondUrl).build());
        when(workOrderMapper.updateTemporaryFrozenByIds(List.of(3001L), false)).thenReturn(1);
        var request = disposeRequestWithEvent(firstUrl);
        request.setReviewMaterialEvents(List.of(
                new MesProEdhrNonconformanceReviewDisposeReqVO.ReviewMaterialEventReqVO()
                        .setFileId(9301L).setAction("UPLOAD").setUrl(firstUrl).setSequence(1),
                new MesProEdhrNonconformanceReviewDisposeReqVO.ReviewMaterialEventReqVO()
                        .setFileId(9302L).setAction("UPLOAD").setUrl(secondUrl).setSequence(2),
                new MesProEdhrNonconformanceReviewDisposeReqVO.ReviewMaterialEventReqVO()
                        .setFileId(9302L).setAction("DELETE").setUrl(secondUrl).setSequence(3)));
        service.dispose(request);
        ArgumentCaptor<MesProEdhrNonconformanceReviewDO> updateCaptor =
                ArgumentCaptor.forClass(MesProEdhrNonconformanceReviewDO.class);
        verify(reviewMapper).updateById(updateCaptor.capture());
        assertEquals(9301L, updateCaptor.getValue().getReviewMaterialFileId());
        var payload = com.alibaba.fastjson.JSON.parseObject(updateCaptor.getValue().getReviewMaterialsJson());
        assertEquals(9301L, payload.getJSONArray("activeMaterials").getJSONObject(0).getLong("fileId"));
        assertEquals(1, payload.getJSONArray("activeMaterials").size());
        assertEquals(9302L, payload.getJSONArray("reviewMaterialEvents").getJSONObject(2).getLong("fileId"));
        assertEquals("DELETE", payload.getJSONArray("reviewMaterialEvents").getJSONObject(2).getString("action"));
        assertTrue(updateCaptor.getValue().getTraceSnapshotJson().contains("\"reviewMaterialsJson\""));
    }
    @Test
    void disposeUsesExactPersistedUrlForLiteralPercentAndAuthoritativeFileName() {
        stubPendingReview("concession_release");
        String url = "http://localhost:48081/admin-api/infra/file/10/get/100%.pdf";
        stubFormalFile(url, FileDO.builder().id(9401L).configId(10L)
                .name("100%.pdf").path("100%.pdf").url(url).build());
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3001L), false)).thenReturn(1);

        service.dispose(new MesProEdhrNonconformanceReviewDisposeReqVO()
                .setId(1001L)
                .setDisposition("concession_release")
                .setReviewMaterialUrl(url)
                .setReviewMaterials(java.util.List.of(reviewMaterials(url).get(0)
                        .setFileName("客户端猜测.pdf")))
                .setReviewMaterialEvents(java.util.List.of(
                        new MesProEdhrNonconformanceReviewDisposeReqVO.ReviewMaterialEventReqVO()
                                .setAction("UPLOAD").setFileId(materialIds.get(url)).setUrl(url).setFileName("客户端猜测.pdf").setSequence(1)))
                .setReviewOpinion("让步放行")
                .setSignaturePassword("qa-signature-password"));

        ArgumentCaptor<MesProEdhrNonconformanceReviewDO> updateCaptor =
                ArgumentCaptor.forClass(MesProEdhrNonconformanceReviewDO.class);
        verify(reviewMapper).updateById(updateCaptor.capture());
        assertEquals(9401L, updateCaptor.getValue().getReviewMaterialFileId());
        assertTrue(updateCaptor.getValue().getReviewMaterialsJson().contains("\"fileName\":\"100%.pdf\""));
        assertTrue(updateCaptor.getValue().getReviewMaterialsJson().contains("\"configId\":10"));
        assertTrue(updateCaptor.getValue().getReviewMaterialsJson().contains("\"path\":\"mes/edhr-ncr/reviews/1001/upload-9401/100%.pdf\""));
        assertFalse(updateCaptor.getValue().getReviewMaterialsJson().contains("客户端猜测.pdf"));
    }

    @Test
    void disposePreservesLiteralPercentEncodedSequenceWithoutDecodingIt() {
        stubPendingReview("concession_release");
        String url = "http://localhost:48081/admin-api/infra/file/10/get/100%25.pdf";
        stubFormalFile(url, FileDO.builder().id(9402L).configId(10L)
                .name("100%25.pdf").path("100%25.pdf").url(url).build());
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3001L), false)).thenReturn(1);

        service.dispose(new MesProEdhrNonconformanceReviewDisposeReqVO()
                .setId(1001L)
                .setDisposition("concession_release")
                .setReviewMaterialUrl(url)
                .setReviewMaterials(java.util.List.of(new MesProEdhrNonconformanceReviewDisposeReqVO.ReviewMaterialReqVO()
                        .setFileId(materialIds.get(url)).setUrl(url).setFileName("客户端猜测.pdf").setSortNo(1)))
                .setReviewMaterialEvents(java.util.List.of(
                        new MesProEdhrNonconformanceReviewDisposeReqVO.ReviewMaterialEventReqVO()
                                .setAction("UPLOAD").setFileId(materialIds.get(url)).setUrl(url).setFileName("客户端猜测.pdf").setSequence(1)))
                .setReviewOpinion("让步放行")
                .setSignaturePassword("qa-signature-password"));

        ArgumentCaptor<MesProEdhrNonconformanceReviewDO> updateCaptor =
                ArgumentCaptor.forClass(MesProEdhrNonconformanceReviewDO.class);
        verify(reviewMapper).updateById(updateCaptor.capture());
        assertEquals(9402L, updateCaptor.getValue().getReviewMaterialFileId());
        assertTrue(updateCaptor.getValue().getReviewMaterialsJson().contains("\"fileName\":\"100%25.pdf\""));
        assertTrue(updateCaptor.getValue().getReviewMaterialsJson().contains("\"path\":\"mes/edhr-ncr/reviews/1001/upload-9402/100%25.pdf\""));
        assertFalse(updateCaptor.getValue().getReviewMaterialsJson().contains("客户端猜测.pdf"));
    }

    @Test
    void disposeSupportsExactS3PresignedUrlWithoutAdminPathParsing() {
        stubPendingReview("concession_release");
        String url = "https://s3.example.test/ncr/100%25.pdf?X-Amz-Signature=fixture";
        stubFormalFile(url, FileDO.builder().id(9403L).configId(21L)
                .name("100%25.pdf").path("ncr/100%25.pdf").url(url).build());
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3001L), false)).thenReturn(1);

        service.dispose(new MesProEdhrNonconformanceReviewDisposeReqVO()
                .setId(1001L)
                .setDisposition("concession_release")
                .setReviewMaterialUrl(url)
                .setReviewMaterials(java.util.List.of(new MesProEdhrNonconformanceReviewDisposeReqVO.ReviewMaterialReqVO()
                        .setFileId(materialIds.get(url)).setUrl(url).setFileName("客户端猜测.pdf").setSortNo(1)))
                .setReviewMaterialEvents(java.util.List.of(
                        new MesProEdhrNonconformanceReviewDisposeReqVO.ReviewMaterialEventReqVO()
                                .setAction("UPLOAD").setFileId(materialIds.get(url)).setUrl(url).setFileName("客户端猜测.pdf").setSequence(1)))
                .setReviewOpinion("让步放行")
                .setSignaturePassword("qa-signature-password"));

        ArgumentCaptor<MesProEdhrNonconformanceReviewDO> updateCaptor =
                ArgumentCaptor.forClass(MesProEdhrNonconformanceReviewDO.class);
        verify(reviewMapper).updateById(updateCaptor.capture());
        assertEquals(9403L, updateCaptor.getValue().getReviewMaterialFileId());
        assertTrue(updateCaptor.getValue().getReviewMaterialsJson().contains("\"configId\":21"));
        assertTrue(updateCaptor.getValue().getReviewMaterialsJson().contains("\"path\":\"mes/edhr-ncr/reviews/1001/upload-9403/ncr/100%25.pdf\""));
        assertTrue(updateCaptor.getValue().getReviewMaterialsJson().contains("\"url\":\"" + url + "\""));
    }

    @Test
    void disposeUsesExplicitIdentityRatherThanCaseInsensitiveUrlCandidates() {
        stubPendingReview("concession_release");
        String exactUrl = REVIEW_MATERIAL_URL;
        stubFormalFile(exactUrl, FileDO.builder().id(9406L).configId(10L).name("review.pdf")
                .path("review.pdf").url(exactUrl).build());
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3001L), false)).thenReturn(1);

        service.dispose(disposeRequestWithEvent(exactUrl).setReviewMaterialEvents(null));

        ArgumentCaptor<MesProEdhrNonconformanceReviewDO> updateCaptor =
                ArgumentCaptor.forClass(MesProEdhrNonconformanceReviewDO.class);
        verify(reviewMapper).updateById(updateCaptor.capture());
        assertEquals(9406L, updateCaptor.getValue().getReviewMaterialFileId());
        assertTrue(updateCaptor.getValue().getReviewMaterialsJson().contains("\"fileName\":\"review.pdf\""));
    }

    @Test
    void disposeRejectsMissingPersistedFileUrlInsteadOfUsingConfigAndPath() {
        when(reviewMapper.selectByIdForUpdate(1001L)).thenReturn(new MesProEdhrNonconformanceReviewDO().setId(1001L).setReviewStatus("pending_review"));
        String url = REVIEW_MATERIAL_URL;
        stubFormalFile(url, FileDO.builder().id(9404L).configId(10L)
                .name("review.pdf").path("review.pdf").url(null).build());

        assertThrows(ServiceException.class, () -> service.dispose(disposeRequestWithEvent(url)));
        verify(fileMapper).selectById(materialIds.get(url));
        verify(reviewMapper, never()).updateById(any(MesProEdhrNonconformanceReviewDO.class));
    }

    @Test
    void disposeRejectsForgedUrlThatOnlyReachesTheSameDecodedPath() {
        when(reviewMapper.selectByIdForUpdate(1001L)).thenReturn(new MesProEdhrNonconformanceReviewDO().setId(1001L).setReviewStatus("pending_review"));
        String persistedUrl = REVIEW_MATERIAL_URL;
        String forgedUrl = "http://evil.example.test/admin-api/infra/file/10/get/review.pdf";
        stubFormalFile(forgedUrl, FileDO.builder().id(9405L).configId(10L)
                .name("review.pdf").path("review.pdf").url(persistedUrl).build());

        assertThrows(ServiceException.class, () -> service.dispose(disposeRequestWithEvent(forgedUrl)));
        verify(fileMapper).selectById(materialIds.get(forgedUrl));
        verify(reviewMapper, never()).updateById(any(MesProEdhrNonconformanceReviewDO.class));
    }

    @Test
    void pqcReleaseApplicationCanStartReviewBeforeBatchCreation() {
        when(releaseApplicationMapper.selectByIdForUpdate(7001L)).thenReturn(
                new MesProcessPoolActiveOrderReleaseApplicationDO()
                        .setId(7001L)
                        .setActiveOrderId(8101L)
                        .setApplicationStatus(MesReleaseFlowStatus.PQC_RELEASE_PENDING)
                        .setVersion(1)
                        .setWorkOrderId(3001L)
                        .setWorkOrderCode("WO-001")
                        .setBatchCode("BATCH-001"));
        when(workOrderMapper.selectByIdForUpdate(3001L)).thenReturn(
                new MesProWorkOrderDO().setId(3001L).setTemporaryFrozen(false));
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3001L), true)).thenReturn(1);
        when(reviewMapper.insert(any(MesProEdhrNonconformanceReviewDO.class))).thenAnswer(invocation -> {
            invocation.<MesProEdhrNonconformanceReviewDO>getArgument(0).setId(1001L);
            return 1;
        });

        MesProEdhrNonconformanceReviewRespVO result = service.create(
                new MesProEdhrNonconformanceReviewCreateReqVO()
                        .setSourceType("PQC_RELEASE")
                        .setSourceId(7001L)
                        .setNonconformanceReason("检验结论需要评审")
                        .setSignaturePassword("create-password"));

        assertEquals(1001L, result.getId());
        assertEquals(3001L, result.getWorkOrderId());
        assertEquals("WO-001", result.getWorkOrderCode());
        assertNull(result.getBatchExecutionId());
        verify(workOrderMapper).updateTemporaryFrozenByIds(java.util.List.of(3001L), true);
        verify(batchExecutionMapper, never()).updateById(any(MesProEdhrBatchExecutionDO.class));
    }

    @Test
    void pqcReleaseApplicationReviewKeepsApplicationScopeWhenBatchExecutionIdIsPresent() {
        when(releaseApplicationMapper.selectByIdForUpdate(7001L)).thenReturn(
                new MesProcessPoolActiveOrderReleaseApplicationDO()
                        .setId(7001L)
                        .setActiveOrderId(8101L)
                        .setApplicationStatus(MesReleaseFlowStatus.PQC_RELEASE_PENDING)
                        .setVersion(1)
                        .setWorkOrderId(3001L)
                        .setWorkOrderCode("WO-001")
                        .setBatchCode("BATCH-001")
                        .setBatchExecutionId(9301L));
        when(workOrderMapper.selectByIdForUpdate(3001L)).thenReturn(
                new MesProWorkOrderDO().setId(3001L).setTemporaryFrozen(false));
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3001L), true)).thenReturn(1);
        when(reviewMapper.insert(any(MesProEdhrNonconformanceReviewDO.class))).thenAnswer(invocation -> {
            invocation.<MesProEdhrNonconformanceReviewDO>getArgument(0).setId(1001L);
            return 1;
        });

        MesProEdhrNonconformanceReviewRespVO result = service.create(
                new MesProEdhrNonconformanceReviewCreateReqVO()
                        .setSourceType("PQC_RELEASE")
                        .setSourceId(7001L)
                        .setBatchExecutionId(9301L)
                        .setNonconformanceReason("检验结论需要评审")
                        .setSignaturePassword("create-password"));

        assertEquals(1001L, result.getId());
        assertEquals(8101L, result.getActiveOrderId());
        ArgumentCaptor<MesProEdhrNonconformanceReviewDO> reviewCaptor =
                ArgumentCaptor.forClass(MesProEdhrNonconformanceReviewDO.class);
        verify(reviewMapper).insert(reviewCaptor.capture());
        assertEquals(7001L, reviewCaptor.getValue().getSourceId());
        assertNull(reviewCaptor.getValue().getBatchExecutionId());
        verify(batchExecutionMapper, never()).updateById(any(MesProEdhrBatchExecutionDO.class));
    }

    @Test
    void pqcSubmissionCanStartReviewWithoutBatchExecution() {
        when(processPoolEventMapper.selectByIdForUpdate(160L)).thenReturn(
                new MesProProcessPoolEventDO()
                        .setId(160L)
                        .setEventType(MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION)
                        .setWorkOrderId(3003L));
        when(workOrderMapper.selectByIdForUpdate(3003L)).thenReturn(
                new MesProWorkOrderDO()
                        .setId(3003L)
                        .setCode("WO-PQC-001")
                        .setBatchCode("BATCH-PQC-001")
                        .setTemporaryFrozen(false));
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3003L), true)).thenReturn(1);
        when(pqcInspectionTaskMapper.selectBySubmittedEventId(160L)).thenReturn(
                new MesPqcInspectionTaskDO()
                        .setId(2600L)
                        .setSubmittedEventId(160L)
                        .setActiveOrderId(8103L)
                        .setWorkOrderId(3003L));
        when(reviewMapper.insert(any(MesProEdhrNonconformanceReviewDO.class))).thenAnswer(invocation -> {
            invocation.<MesProEdhrNonconformanceReviewDO>getArgument(0).setId(1003L);
            return 1;
        });

        MesProEdhrNonconformanceReviewRespVO result = service.create(
                new MesProEdhrNonconformanceReviewCreateReqVO()
                        .setSourceType("PQC_SUBMISSION")
                        .setSourceId(160L)
                        .setNonconformanceReason("PQC提交不合格")
                        .setSignaturePassword("create-password"));

        assertEquals(1003L, result.getId());
        assertEquals("PQC_SUBMISSION", result.getSourceType());
        assertEquals(160L, result.getSourceId());
        assertEquals(8103L, result.getActiveOrderId());
        assertEquals(3003L, result.getWorkOrderId());
        assertEquals("WO-PQC-001", result.getWorkOrderCode());
        assertEquals("BATCH-PQC-001", result.getBatchCode());
        assertNull(result.getBatchExecutionId());
        verify(workOrderMapper).updateTemporaryFrozenByIds(java.util.List.of(3003L), true);
        verify(batchExecutionMapper, never()).updateById(any(MesProEdhrBatchExecutionDO.class));
    }

    @Test
    void pqcSubmissionUsesSourceEventWhenBatchExecutionIdIsStale() {
        when(processPoolEventMapper.selectByIdForUpdate(161L)).thenReturn(
                new MesProProcessPoolEventDO()
                        .setId(161L)
                        .setEventType(MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION)
                        .setWorkOrderId(3004L));
        when(workOrderMapper.selectByIdForUpdate(3004L)).thenReturn(
                new MesProWorkOrderDO()
                        .setId(3004L)
                        .setCode("WO-PQC-002")
                        .setBatchCode("BATCH-PQC-002")
                        .setTemporaryFrozen(false));
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3004L), true)).thenReturn(1);
        when(pqcInspectionTaskMapper.selectBySubmittedEventId(161L)).thenReturn(
                new MesPqcInspectionTaskDO()
                        .setId(2601L)
                        .setSubmittedEventId(161L)
                        .setActiveOrderId(8104L)
                        .setWorkOrderId(3004L));
        when(reviewMapper.insert(any(MesProEdhrNonconformanceReviewDO.class))).thenAnswer(invocation -> {
            invocation.<MesProEdhrNonconformanceReviewDO>getArgument(0).setId(1004L);
            return 1;
        });

        MesProEdhrNonconformanceReviewRespVO result = service.create(
                new MesProEdhrNonconformanceReviewCreateReqVO()
                        .setSourceType("PQC_SUBMISSION")
                        .setSourceId(161L)
                        .setBatchExecutionId(999_999L)
                        .setNonconformanceReason("PQC提交不合格")
                        .setSignaturePassword("create-password"));

        assertEquals(1004L, result.getId());
        assertEquals("PQC_SUBMISSION", result.getSourceType());
        assertEquals(161L, result.getSourceId());
        assertEquals(8104L, result.getActiveOrderId());
        assertEquals(3004L, result.getWorkOrderId());
        assertNull(result.getBatchExecutionId());
        verify(batchExecutionMapper, never()).selectById(999_999L);
        verify(batchExecutionMapper, never()).updateById(any(MesProEdhrBatchExecutionDO.class));
    }

    @Test
    void batchReviewFreezesWorkOrderAndCapturesOriginalState() {
        when(batchExecutionMapper.selectById(9001L)).thenReturn(new MesProEdhrBatchExecutionDO()
                .setId(9001L)
                .setBatchExecutionCode("BE-9001")
                .setWorkOrderId(3002L)
                .setWorkOrderCode("WO-002")
                .setBatchCode("BATCH-002")
                .setStatus(20));
        when(workOrderMapper.selectByIdForUpdate(3002L)).thenReturn(
                new MesProWorkOrderDO().setId(3002L).setTemporaryFrozen(false));
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3002L), true)).thenReturn(1);
        stubBatchOrigin(9001L, 8102L);
        when(reviewMapper.insert(any(MesProEdhrNonconformanceReviewDO.class))).thenAnswer(invocation -> {
            invocation.<MesProEdhrNonconformanceReviewDO>getArgument(0).setId(1002L);
            return 1;
        });

        service.create(new MesProEdhrNonconformanceReviewCreateReqVO()
                .setSourceType("PQC_RELEASE")
                .setBatchExecutionId(9001L)
                .setNonconformanceReason("批次不合格")
                .setSignaturePassword("create-password"));

        ArgumentCaptor<MesProEdhrNonconformanceReviewDO> reviewCaptor =
                ArgumentCaptor.forClass(MesProEdhrNonconformanceReviewDO.class);
        verify(reviewMapper).insert(reviewCaptor.capture());
        assertEquals(false, reviewCaptor.getValue().getPreviousWorkOrderTemporaryFrozen());
        verify(workOrderMapper).updateTemporaryFrozenByIds(java.util.List.of(3002L), true);
        verify(batchExecutionMapper).updateById(any(MesProEdhrBatchExecutionDO.class));
    }


    @Test
    void releaseOwnerRejectCreatesSignedPendingReviewAndFreezesBatch() {
        when(batchExecutionMapper.selectByIdForUpdate(9002L)).thenReturn(new MesProEdhrBatchExecutionDO()
                .setId(9002L)
                .setBatchExecutionCode("BE-9002")
                .setWorkOrderId(3008L)
                .setWorkOrderCode("WO-008")
                .setBatchCode("BATCH-008")
                .setStatus(20));
        when(workOrderMapper.selectByIdForUpdate(3008L)).thenReturn(
                new MesProWorkOrderDO().setId(3008L).setTemporaryFrozen(false));
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3008L), true)).thenReturn(1);
        stubBatchOrigin(9002L, 8108L);
        when(signatureService.recordBatchActionSignature(eq(21L), eq(9002L), eq("release-password"),
                eq("末检结果不合格"), eq(MesProBatchRecordExecutionSignatureService.ACTION_NONCONFORMANCE_REJECT),
                eq("eDHR不合格评审发起"), any())).thenReturn(9202L);
        when(reviewMapper.insert(any(MesProEdhrNonconformanceReviewDO.class))).thenAnswer(invocation -> {
            invocation.<MesProEdhrNonconformanceReviewDO>getArgument(0).setId(1202L);
            return 1;
        });

        MesProEdhrNonconformanceReviewRespVO result = service.rejectBatch(
                new MesProEdhrBatchExecutionRejectReqVO()
                        .setBatchExecutionId(9002L)
                        .setNonconformanceReason("末检结果不合格")
                        .setSignaturePassword("release-password"));

        assertEquals(1202L, result.getId());
        assertEquals("PQC_RELEASE", result.getSourceType());
        assertEquals(9002L, result.getBatchExecutionId());
        assertEquals("pending_review", result.getReviewStatus());
        ArgumentCaptor<MesProEdhrNonconformanceReviewDO> reviewCaptor =
                ArgumentCaptor.forClass(MesProEdhrNonconformanceReviewDO.class);
        verify(reviewMapper).insert(reviewCaptor.capture());
        assertTrue(reviewCaptor.getValue().getRemark().contains("9202"));
        verify(signatureService).recordBatchActionSignature(eq(21L), eq(9002L), eq("release-password"),
                eq("末检结果不合格"), eq(MesProBatchRecordExecutionSignatureService.ACTION_NONCONFORMANCE_REJECT),
                eq("eDHR不合格评审发起"), any());
        verify(batchExecutionMapper).updateById(argThat((MesProEdhrBatchExecutionDO batch) ->
                batch.getId().equals(9002L) && batch.getStatus().equals(15)));
        verify(workOrderMapper).updateTemporaryFrozenByIds(java.util.List.of(3008L), true);
        assertCreationAudit("1202", "PQC_RELEASE", null, "9202", "末检结果不合格");
    }

    @Test
    void releaseOwnerRejectWithInvalidSignatureDoesNotCreateReviewOrFreezeBatch() {
        when(batchExecutionMapper.selectByIdForUpdate(9003L)).thenReturn(new MesProEdhrBatchExecutionDO()
                .setId(9003L)
                .setWorkOrderId(3009L)
                .setStatus(20));
        when(signatureService.recordBatchActionSignature(eq(21L), eq(9003L), eq("wrong-password"),
                eq("末检结果不合格"), eq(MesProBatchRecordExecutionSignatureService.ACTION_NONCONFORMANCE_REJECT),
                eq("eDHR不合格评审发起"), any()))
                .thenThrow(exception(PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PASSWORD_INVALID));

        assertThrows(ServiceException.class, () -> service.rejectBatch(
                new MesProEdhrBatchExecutionRejectReqVO()
                        .setBatchExecutionId(9003L)
                        .setNonconformanceReason("末检结果不合格")
                        .setSignaturePassword("wrong-password")));

        verify(reviewMapper, never()).insert(any(MesProEdhrNonconformanceReviewDO.class));
        verify(batchExecutionMapper, never()).updateById(any(MesProEdhrBatchExecutionDO.class));
        verify(workOrderMapper, never()).updateTemporaryFrozenByIds(any(), any());
    }

    @Test
    void workOrderPendingReviewFreezeErrorIncludesBranchDetail() {
        when(reviewMapper.selectFirstBlockingByWorkOrderId(3005L)).thenReturn(
                MesProEdhrNonconformanceReviewDO.builder()
                        .id(1101L)
                        .reviewCode("NCR-1101")
                        .reviewStatus("pending_review")
                        .workOrderId(3005L)
                        .sourceType("PQC_SUBMISSION")
                        .sourceId(160L)
                        .build());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.ensureWorkOrderNotFrozen(3005L, "报工"));

        assertTrue(exception.getMessage().contains("冻结分支=待处置不合格评审"));
        assertTrue(exception.getMessage().contains("action=报工"));
        assertTrue(exception.getMessage().contains("workOrderId=3005"));
        assertTrue(exception.getMessage().contains("reviewId=1101"));
        assertTrue(exception.getMessage().contains("sourceType=PQC_SUBMISSION"));
    }

    @Test
    void workOrderVoidDispositionFreezeErrorIncludesBranchDetail() {
        when(reviewMapper.selectFirstBlockingByWorkOrderId(3006L)).thenReturn(
                MesProEdhrNonconformanceReviewDO.builder()
                        .id(1102L)
                        .reviewCode("NCR-1102")
                        .reviewStatus("closed")
                        .disposition("void")
                        .workOrderId(3006L)
                        .sourceType("PQC_RELEASE")
                        .sourceId(7001L)
                        .build());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.ensureWorkOrderNotFrozen(3006L, "报工"));

        assertTrue(exception.getMessage().contains("冻结分支=作废处置不合格评审"));
        assertTrue(exception.getMessage().contains("action=报工"));
        assertTrue(exception.getMessage().contains("workOrderId=3006"));
        assertTrue(exception.getMessage().contains("reviewId=1102"));
        assertTrue(exception.getMessage().contains("sourceType=PQC_RELEASE"));
    }

    @Test
    void workOrderTemporaryFrozenErrorIncludesBranchDetail() {
        when(workOrderMapper.selectByIdForUpdate(3007L)).thenReturn(
                new MesProWorkOrderDO().setId(3007L).setTemporaryFrozen(true));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.ensureWorkOrderNotFrozen(3007L, "报工"));

        assertTrue(exception.getMessage().contains("冻结分支=工单临时冻结"));
        assertTrue(exception.getMessage().contains("action=报工"));
        assertTrue(exception.getMessage().contains("workOrderId=3007"));
    }

    @Test
    void pqcSubmissionFreezeCheckUsesActiveOrderAndDoesNotBlockOnPqcReleaseReview() {
        when(reviewMapper.selectFirstBlockingPqcSubmissionByActiveOrderId(8101L)).thenReturn(null);
        when(workOrderMapper.selectByIdForUpdate(3001L)).thenReturn(
                new MesProWorkOrderDO().setId(3001L).setTemporaryFrozen(false));

        service.ensurePqcSubmissionNotFrozen(8101L, 3001L, "PQC提交");

        verify(reviewMapper).selectFirstBlockingPqcSubmissionByActiveOrderId(8101L);
        verify(reviewMapper, never()).selectFirstBlockingByWorkOrderId(3001L);
    }

    @Test
    void pqcSubmissionFreezeCheckFailsFastWithoutActiveOrder() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.ensurePqcSubmissionNotFrozen(null, 3001L, "PQC提交"));

        assertEquals(PRO_EDHR_NONCONFORMANCE_REVIEW_SOURCE_INVALID.getCode(), exception.getCode());
        verifyNoInteractions(workOrderMapper);
    }

    @Test
    void batchFreezeCheckUsesActiveOrderReviewWhenBatchOriginHasNoReviewBatchId() {
        when(reviewMapper.selectPendingByBatchExecutionId(9004L)).thenReturn(null);
        when(batchExecutionOriginMapper.selectListByBatchExecutionId(9004L)).thenReturn(
                java.util.List.of(new MesProEdhrBatchExecutionOriginDO()
                        .setBatchExecutionId(9004L)
                        .setActiveOrderId(8104L)));
        when(reviewMapper.selectFirstBlockingPqcSubmissionByActiveOrderId(8104L)).thenReturn(
                MesProEdhrNonconformanceReviewDO.builder()
                        .id(1104L)
                        .reviewCode("NCR-1104")
                        .reviewStatus("pending_review")
                        .sourceType("ACTIVE_ORDER")
                        .sourceId(8104L)
                        .activeOrderId(8104L)
                        .batchExecutionId(null)
                        .workOrderId(3004L)
                        .build());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.ensureBatchNotFrozen(9004L, "上市放行"));

        assertTrue(exception.getMessage().contains("reviewId=1104"));
        verify(reviewMapper).selectFirstBlockingPqcSubmissionByActiveOrderId(8104L);
    }

    @Test
    void activeOrderDisposeClosesAssociatedReleaseApplicationAndTodo() {
        MesProEdhrNonconformanceReviewDO review = MesProEdhrNonconformanceReviewDO.builder()
                .id(2601L)
                .sourceType("ACTIVE_ORDER")
                .sourceId(8101L)
                .activeOrderId(8101L)
                .workOrderId(3001L)
                .workOrderCode("WO-001")
                .reviewStatus("pending_review")
                .previousWorkOrderTemporaryFrozen(false)
                .frozenAt(LocalDateTime.of(2026, 9, 26, 8, 0))
                .nonconformanceReason("活跃订单评审")
                .build();
        MesProcessPoolActiveOrderReleaseApplicationDO application =
                new MesProcessPoolActiveOrderReleaseApplicationDO()
                        .setId(7601L)
                        .setActiveOrderId(8101L)
                        .setApplicationStatus(MesReleaseFlowStatus.PQC_RELEASE_PENDING)
                        .setVersion(1)
                        .setWorkOrderId(3001L)
                        .setPqcReleaseWorkTaskId(8601L);
        when(reviewMapper.selectByIdForUpdate(2601L)).thenReturn(review);
        when(reviewMapper.selectById(2601L)).thenReturn(review.setDisposition("rework"));
        when(reviewMapper.selectFreezeLifecycleByWorkOrderId(3001L)).thenReturn(java.util.List.of(review));
        when(releaseApplicationMapper.selectListByActiveOrderIdsForUpdate(java.util.List.of(8101L)))
                .thenReturn(java.util.List.of(application));
        when(workTaskMapper.selectByIdForUpdate(8601L)).thenReturn(new MesProEdhrWorkTaskDO()
                .setId(8601L)
                .setTaskType("PQC_PRODUCTION_RELEASE")
                .setBusinessScopeType("RELEASE_APPLICATION")
                .setBusinessScopeId(7601L)
                .setStatus(MesProEdhrWorkTaskStatus.TODO));
        when(workOrderMapper.selectByIdForUpdate(3001L)).thenReturn(
                new MesProWorkOrderDO().setId(3001L).setTemporaryFrozen(true));
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3001L), false)).thenReturn(1);
        when(signatureService.recordQaDispositionSignature(eq(21L), eq(2601L), eq("qa-signature-password"),
                eq("返工处理"), any())).thenReturn(9601L);
        when(releaseApplicationMapper.closeFromNonconformance(eq(7601L), eq(1), eq("NONCONFORMANCE_REWORK"),
                eq(21L), any(), eq("返工处理"), any())).thenReturn(1);
        when(workTaskMapper.completePqcDecisionTask(eq(8601L), any(), eq("NONCONFORMANCE_REWORK"))).thenReturn(1);

        service.dispose(disposeRequest(2601L, "rework"));

        verify(releaseApplicationMapper).selectListByActiveOrderIdsForUpdate(java.util.List.of(8101L));
        verify(releaseApplicationMapper).closeFromNonconformance(eq(7601L), eq(1), eq("NONCONFORMANCE_REWORK"),
                eq(21L), any(), eq("返工处理"), any());
        verify(workTaskMapper).completePqcDecisionTask(eq(8601L), any(), eq("NONCONFORMANCE_REWORK"));
    }

    @Test
    void batchVoidKeepsWorkOrderFrozen() {
        MesProEdhrNonconformanceReviewDO review = MesProEdhrNonconformanceReviewDO.builder()
                .id(1002L)
                .sourceType("PQC_RELEASE")
                .activeOrderId(8102L)
                .batchExecutionId(9001L)
                .workOrderId(3002L)
                .reviewStatus("pending_review")
                .previousBatchStatus(20)
                .previousWorkOrderTemporaryFrozen(false)
                .nonconformanceReason("批次不合格")
                .build();
        when(reviewMapper.selectByIdForUpdate(1002L)).thenReturn(review);
        when(reviewMapper.selectById(1002L)).thenReturn(review.setDisposition("void"));
        when(batchExecutionMapper.selectById(9001L)).thenReturn(
                new MesProEdhrBatchExecutionDO().setId(9001L).setStatus(15));
        when(workOrderMapper.selectByIdForUpdate(3002L)).thenReturn(
                new MesProWorkOrderDO().setId(3002L).setTemporaryFrozen(true));
        when(signatureService.recordQaDispositionSignature(eq(21L), eq(1002L), eq("qa-signature-password"),
                eq("作废处理"), any())).thenReturn(9102L);
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3002L), true)).thenReturn(1);

        service.dispose(disposeRequest(1002L, "void"));

        verify(workOrderMapper).updateTemporaryFrozenByIds(java.util.List.of(3002L), true);
        verify(batchExecutionMapper).updateById(any(MesProEdhrBatchExecutionDO.class));
    }

    @Test
    void concessionRestoresOriginalWorkOrderStateAndKeepsPqcTaskActive() {
        stubPendingReview("concession_release");
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3001L), false)).thenReturn(1);

        service.dispose(disposeRequest("concession_release"));

        verify(workOrderMapper).updateTemporaryFrozenByIds(java.util.List.of(3001L), false);
        verify(releaseApplicationMapper, never()).closeFromNonconformance(
                any(), any(), any(), any(), any(), any(), any());
        verify(workTaskMapper, never()).completePqcDecisionTask(any(), any(), any());
    }

    @Test
    void qaReworkStartsNewActiveOrderCycleWithoutReusingCompletedOrder() {
        stubPendingReview("rework");
        when(workOrderMapper.updateTemporaryFrozenByIds(List.of(3001L), false)).thenReturn(1);
        when(releaseApplicationMapper.closeFromNonconformance(eq(7001L), eq(1), eq("NONCONFORMANCE_REWORK"),
                eq(21L), any(), eq("返工处理"), any())).thenReturn(1);
        when(workTaskMapper.completePqcDecisionTask(eq(8001L), any(), eq("NONCONFORMANCE_REWORK")))
                .thenReturn(1);

        service.dispose(disposeRequest("rework"));

        verify(activeOrderMapper).insert(argThat((MesProcessPoolActiveOrderDO next) ->
                "ACTIVE".equals(next.getActiveStatus()) && "ACTIVE".equals(next.getBusinessStatus())
                        && Long.valueOf(3001L).equals(next.getWorkOrderId())
                        && !Long.valueOf(8101L).equals(next.getId())));
    }

    @Test
    void replayingSameQaReworkReturnsClosedReviewWithoutCreatingAnotherCycle() {
        stubPendingReview("rework");
        when(workOrderMapper.updateTemporaryFrozenByIds(List.of(3001L), false)).thenReturn(1);
        when(releaseApplicationMapper.closeFromNonconformance(eq(7001L), eq(1), eq("NONCONFORMANCE_REWORK"),
                eq(21L), any(), eq("返工处理"), any())).thenReturn(1);
        when(workTaskMapper.completePqcDecisionTask(eq(8001L), any(), eq("NONCONFORMANCE_REWORK"))).thenReturn(1);
        service.dispose(disposeRequest("rework"));
        ArgumentCaptor<MesProEdhrNonconformanceReviewDO> updated = ArgumentCaptor.forClass(MesProEdhrNonconformanceReviewDO.class);
        verify(reviewMapper).updateById(updated.capture());
        var closed = updated.getValue().setActiveOrderId(8101L).setWorkOrderId(3001L)
                .setSourceType("PQC_RELEASE").setSourceId(7001L);
        when(reviewMapper.selectByIdForUpdate(1001L)).thenReturn(closed);
        lenient().when(activeOrderMapper.selectByReworkReviewId(1001L)).thenReturn(new MesProcessPoolActiveOrderDO()
                .setId(8201L).setReworkSourceActiveOrderId(8101L).setReworkReviewId(1001L).setWorkOrderId(3001L));

        assertEquals(1001L, service.dispose(disposeRequest("rework")).getId());
        assertThrows(ServiceException.class, () -> service.dispose(disposeRequest("rework")
                .setReviewOpinion("changed signed decision")));
        verify(activeOrderMapper, org.mockito.Mockito.times(1)).insert(any(MesProcessPoolActiveOrderDO.class));
        verify(signatureService, org.mockito.Mockito.times(1)).recordQaDispositionSignature(any(), any(), any(), any(), any());
        verify(unifiedAudit, org.mockito.Mockito.times(1)).append(any());
        verify(reviewMapper, org.mockito.Mockito.times(1)).updateById(any(MesProEdhrNonconformanceReviewDO.class));
        verify(operationAuditService, org.mockito.Mockito.times(1)).recordInCallerTransaction(any());
    }

    @Test
    void reworkClosesApplicationAndPqcTaskAndRestoresOriginalWorkOrderState() {
        stubPendingReview("rework");
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3001L), false)).thenReturn(1);
        when(releaseApplicationMapper.closeFromNonconformance(eq(7001L), eq(1), eq("NONCONFORMANCE_REWORK"),
                eq(21L), any(), eq("返工处理"), any())).thenReturn(1);
        when(workTaskMapper.completePqcDecisionTask(eq(8001L), any(), eq("NONCONFORMANCE_REWORK")))
                .thenReturn(1);

        service.dispose(disposeRequest("rework"));

        verify(workOrderMapper).updateTemporaryFrozenByIds(java.util.List.of(3001L), false);
        verify(workTaskMapper).completePqcDecisionTask(eq(8001L), any(), eq("NONCONFORMANCE_REWORK"));
    }

    @Test
    void voidClosesApplicationAndPqcTaskAndKeepsWorkOrderFrozen() {
        stubPendingReview("void");
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3001L), true)).thenReturn(1);
        when(releaseApplicationMapper.closeFromNonconformance(eq(7001L), eq(1), eq("NONCONFORMANCE_VOID"),
                eq(21L), any(), eq("作废处理"), any())).thenReturn(1);
        when(workTaskMapper.completePqcDecisionTask(eq(8001L), any(), eq("NONCONFORMANCE_VOID")))
                .thenReturn(1);

        service.dispose(disposeRequest("void"));

        verify(workOrderMapper).updateTemporaryFrozenByIds(java.util.List.of(3001L), true);
        verify(workTaskMapper).completePqcDecisionTask(eq(8001L), any(), eq("NONCONFORMANCE_VOID"));
    }

    @Test
    void disposeRecordsQaElectronicSignatureSnapshot() {
        stubPendingReview("rework");
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3001L), false)).thenReturn(1);
        when(releaseApplicationMapper.closeFromNonconformance(eq(7001L), eq(1), eq("NONCONFORMANCE_REWORK"),
                eq(21L), any(), eq("返工处理"), any())).thenReturn(1);
        when(workTaskMapper.completePqcDecisionTask(eq(8001L), any(), eq("NONCONFORMANCE_REWORK")))
                .thenReturn(1);

        service.dispose(disposeRequest("rework"));

        verify(signatureService).recordQaDispositionSignature(eq(21L), eq(1001L), eq("qa-signature-password"),
                eq("返工处理"), any());
        ArgumentCaptor<MesProEdhrNonconformanceReviewDO> updateCaptor =
                ArgumentCaptor.forClass(MesProEdhrNonconformanceReviewDO.class);
        verify(reviewMapper).updateById(updateCaptor.capture());
        assertEquals("电子签名#9101", updateCaptor.getValue().getQaSignature());
        assertTrue(updateCaptor.getValue().getTraceSnapshotJson().contains("\"qaSignatureSnapshotJson\""));
        assertTrue(updateCaptor.getValue().getTraceSnapshotJson().contains("\"signatureId\":9101"));
        assertTrue(updateCaptor.getValue().getTraceSnapshotJson().contains("\"actionType\":\"QA_DISPOSITION\""));
    }

    @Test
    void disposeWithoutSignaturePasswordFailsBeforeMutation() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.dispose(new MesProEdhrNonconformanceReviewDisposeReqVO()
                        .setId(1001L)
                        .setDisposition("rework")
                        .setReviewMaterialUrl(REVIEW_MATERIAL_URL)
                        .setReviewMaterials(reviewMaterials(REVIEW_MATERIAL_URL))
                        .setReviewOpinion("返工处理")
                        .setSignaturePassword(" ")));

        assertEquals(PRO_EDHR_NONCONFORMANCE_REVIEW_REQUIRED.getCode(), exception.getCode());
        verifyNoInteractions(reviewMapper, batchExecutionMapper, releaseApplicationMapper, workOrderMapper,
                workTaskMapper, signatureService);
    }

    @Test
    void overlappingSecondReviewCapturesReviewIntroducedFreezeAsNonExternal() {
        LocalDateTime firstFrozenAt = LocalDateTime.of(2026, 9, 13, 9, 0);
        MesProEdhrNonconformanceReviewDO firstReview = MesProEdhrNonconformanceReviewDO.builder()
                .id(2101L)
                .workOrderId(3010L)
                .reviewStatus("pending_review")
                .previousWorkOrderTemporaryFrozen(false)
                .frozenAt(firstFrozenAt)
                .build();
        when(processPoolEventMapper.selectByIdForUpdate(171L)).thenReturn(
                new MesProProcessPoolEventDO()
                        .setId(171L)
                        .setEventType(MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION)
                        .setWorkOrderId(3010L));
        when(workOrderMapper.selectByIdForUpdate(3010L)).thenReturn(
                new MesProWorkOrderDO()
                        .setId(3010L)
                        .setCode("WO-CYCLE")
                        .setBatchCode("BATCH-CYCLE")
                        .setTemporaryFrozen(true));
        lenient().when(reviewMapper.selectFreezeLifecycleByWorkOrderId(3010L)).thenReturn(
                java.util.List.of(firstReview));
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3010L), true)).thenReturn(1);
        when(pqcInspectionTaskMapper.selectBySubmittedEventId(171L)).thenReturn(
                new MesPqcInspectionTaskDO()
                        .setId(2701L)
                        .setSubmittedEventId(171L)
                        .setActiveOrderId(8110L)
                        .setWorkOrderId(3010L));
        when(reviewMapper.insert(any(MesProEdhrNonconformanceReviewDO.class))).thenAnswer(invocation -> {
            invocation.<MesProEdhrNonconformanceReviewDO>getArgument(0).setId(2102L);
            return 1;
        });

        service.create(new MesProEdhrNonconformanceReviewCreateReqVO()
                .setSourceType("PQC_SUBMISSION")
                .setSourceId(171L)
                .setNonconformanceReason("第二份同轮评审")
                .setSignaturePassword("create-password"));

        ArgumentCaptor<MesProEdhrNonconformanceReviewDO> reviewCaptor =
                ArgumentCaptor.forClass(MesProEdhrNonconformanceReviewDO.class);
        verify(reviewMapper).insert(reviewCaptor.capture());
        assertEquals(false, reviewCaptor.getValue().getPreviousWorkOrderTemporaryFrozen());
    }

    @Test
    void closingFirstOverlappingReviewKeepsFreezeUntilRemainingReviewCloses() {
        LocalDateTime firstFrozenAt = LocalDateTime.of(2026, 9, 13, 9, 0);
        LocalDateTime secondFrozenAt = LocalDateTime.of(2026, 9, 13, 9, 5);
        MesProEdhrNonconformanceReviewDO firstReview = MesProEdhrNonconformanceReviewDO.builder()
                .id(2201L)
                .sourceType("PQC_SUBMISSION")
                .sourceId(181L)
                .activeOrderId(8120L)
                .workOrderId(3020L)
                .reviewStatus("pending_review")
                .previousWorkOrderTemporaryFrozen(false)
                .frozenAt(firstFrozenAt)
                .nonconformanceReason("第一份同轮评审")
                .build();
        MesProEdhrNonconformanceReviewDO secondReview = MesProEdhrNonconformanceReviewDO.builder()
                .id(2202L)
                .workOrderId(3020L)
                .reviewStatus("pending_review")
                .previousWorkOrderTemporaryFrozen(false)
                .frozenAt(secondFrozenAt)
                .build();
        when(reviewMapper.selectByIdForUpdate(2201L)).thenReturn(firstReview);
        when(reviewMapper.selectById(2201L)).thenReturn(firstReview.setDisposition("concession_release"));
        when(workOrderMapper.selectByIdForUpdate(3020L)).thenReturn(
                new MesProWorkOrderDO().setId(3020L).setTemporaryFrozen(true));
        lenient().when(reviewMapper.selectFreezeLifecycleByWorkOrderId(3020L)).thenReturn(
                java.util.List.of(firstReview, secondReview));
        lenient().when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3020L), false)).thenReturn(1);
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3020L), true)).thenReturn(1);

        service.dispose(disposeRequest(2201L, "concession_release"));

        verify(workOrderMapper).updateTemporaryFrozenByIds(java.util.List.of(3020L), true);
    }

    @Test
    void closingLaterOverlappingReviewDoesNotKeepReviewIntroducedFreezeAfterCycleEnds() {
        LocalDateTime firstFrozenAt = LocalDateTime.of(2026, 9, 13, 9, 0);
        LocalDateTime secondFrozenAt = LocalDateTime.of(2026, 9, 13, 9, 5);
        LocalDateTime firstClosedAt = LocalDateTime.of(2026, 9, 13, 9, 10);
        MesProEdhrNonconformanceReviewDO firstReview = MesProEdhrNonconformanceReviewDO.builder()
                .id(2301L)
                .workOrderId(3030L)
                .reviewStatus("closed")
                .disposition("concession_release")
                .previousWorkOrderTemporaryFrozen(false)
                .frozenAt(firstFrozenAt)
                .closedAt(firstClosedAt)
                .build();
        MesProEdhrNonconformanceReviewDO laterReview = MesProEdhrNonconformanceReviewDO.builder()
                .id(2302L)
                .sourceType("PQC_SUBMISSION")
                .sourceId(191L)
                .activeOrderId(8130L)
                .workOrderId(3030L)
                .reviewStatus("pending_review")
                .previousWorkOrderTemporaryFrozen(true)
                .frozenAt(secondFrozenAt)
                .nonconformanceReason("第二份同轮评审")
                .build();
        when(reviewMapper.selectByIdForUpdate(2302L)).thenReturn(laterReview);
        when(reviewMapper.selectById(2302L)).thenReturn(laterReview.setDisposition("concession_release"));
        when(workOrderMapper.selectByIdForUpdate(3030L)).thenReturn(
                new MesProWorkOrderDO().setId(3030L).setTemporaryFrozen(true));
        lenient().when(reviewMapper.selectFreezeLifecycleByWorkOrderId(3030L)).thenReturn(
                java.util.List.of(firstReview, laterReview));
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3030L), false)).thenReturn(1);
        lenient().when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3030L), true)).thenReturn(1);

        service.dispose(disposeRequest(2302L, "concession_release"));

        verify(workOrderMapper).updateTemporaryFrozenByIds(java.util.List.of(3030L), false);
    }

    @Test
    void secondRoundManualFreezeRemainsAfterLaterReviewCloses() {
        LocalDateTime secondFrozenAt = LocalDateTime.of(2026, 9, 13, 10, 0);
        MesProEdhrNonconformanceReviewDO laterReview = MesProEdhrNonconformanceReviewDO.builder()
                .id(2402L)
                .sourceType("PQC_SUBMISSION")
                .sourceId(201L)
                .activeOrderId(8140L)
                .workOrderId(3040L)
                .reviewStatus("pending_review")
                .previousWorkOrderTemporaryFrozen(true)
                .frozenAt(secondFrozenAt)
                .nonconformanceReason("第二轮评审")
                .build();
        when(reviewMapper.selectByIdForUpdate(2402L)).thenReturn(laterReview);
        when(reviewMapper.selectById(2402L)).thenReturn(laterReview.setDisposition("concession_release"));
        when(workOrderMapper.selectByIdForUpdate(3040L)).thenReturn(
                new MesProWorkOrderDO().setId(3040L).setTemporaryFrozen(true));
        lenient().when(reviewMapper.selectFreezeLifecycleByWorkOrderId(3040L)).thenReturn(
                java.util.List.of(laterReview));
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3040L), true)).thenReturn(1);

        service.dispose(disposeRequest(2402L, "concession_release"));

        verify(workOrderMapper).updateTemporaryFrozenByIds(java.util.List.of(3040L), true);
    }

    @Test
    void closedHistoricalExternalFreezeDoesNotRefreezeLaterLifecycle() {
        LocalDateTime firstFrozenAt = LocalDateTime.of(2026, 9, 13, 9, 0);
        LocalDateTime firstClosedAt = LocalDateTime.of(2026, 9, 13, 9, 10);
        LocalDateTime secondFrozenAt = LocalDateTime.of(2026, 9, 13, 10, 0);
        MesProEdhrNonconformanceReviewDO historicalReview = MesProEdhrNonconformanceReviewDO.builder()
                .id(2501L)
                .workOrderId(3050L)
                .reviewStatus("closed")
                .disposition("concession_release")
                .previousWorkOrderTemporaryFrozen(true)
                .frozenAt(firstFrozenAt)
                .closedAt(firstClosedAt)
                .build();
        MesProEdhrNonconformanceReviewDO laterReview = MesProEdhrNonconformanceReviewDO.builder()
                .id(2502L)
                .sourceType("PQC_SUBMISSION")
                .sourceId(211L)
                .activeOrderId(8150L)
                .workOrderId(3050L)
                .reviewStatus("pending_review")
                .previousWorkOrderTemporaryFrozen(false)
                .frozenAt(secondFrozenAt)
                .nonconformanceReason("第二轮评审")
                .build();
        when(reviewMapper.selectByIdForUpdate(2502L)).thenReturn(laterReview);
        when(reviewMapper.selectById(2502L)).thenReturn(laterReview.setDisposition("concession_release"));
        when(workOrderMapper.selectByIdForUpdate(3050L)).thenReturn(
                new MesProWorkOrderDO().setId(3050L).setTemporaryFrozen(true));
        lenient().when(reviewMapper.selectFreezeLifecycleByWorkOrderId(3050L)).thenReturn(
                java.util.List.of(historicalReview, laterReview));
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3050L), false)).thenReturn(1);

        service.dispose(disposeRequest(2502L, "concession_release"));

        verify(workOrderMapper).updateTemporaryFrozenByIds(java.util.List.of(3050L), false);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
            "concession_release,mes.nonconformance.concession",
            "rework,mes.nonconformance.rework",
            "void,mes.nonconformance.void"})
    void successfulDispositionAppendsOneReadableUnifiedFact(String disposition, String operation) {
        prepareUnifiedDisposition(disposition);
        service.dispose(disposeRequest(disposition));

        var captured = ArgumentCaptor.forClass(
                cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand.class);
        verify(unifiedAudit, org.mockito.Mockito.times(1)).append(captured.capture());
        var command = captured.getValue();
        assertEquals(operation, command.getOperationId());
        assertEquals("1001", command.getSubjectId());
        assertEquals(disposeRequest(disposition).getReviewOpinion(), command.getReason());
        assertEquals("USER", command.getReasonSource());
        assertEquals("SUCCESS", command.getResultStatus());
        assertEquals("9101", command.getSignatureRecordId());
        org.junit.jupiter.api.Assertions.assertNotNull(command.getSignatureContentHash());
        assertEquals(lastSignature.getContentHash(), command.getSignatureContentHash());
        assertEquals("PRESENT", command.getBeforeState().getState());
        assertEquals("PRESENT", command.getAfterState().getState());
        assertTrue(command.getSubjectVersion().matches("sha256:[0-9a-f]{64}"));
        assertTrue(command.getBeforeState().getObjectVersion().matches("sha256:[0-9a-f]{64}"));
        assertTrue(command.getAfterState().getObjectVersion().matches("sha256:[0-9a-f]{64}"));
        assertEquals("sha256:" + cn.hutool.crypto.digest.DigestUtil.sha256Hex(
                command.getBeforeState().getCanonicalJson()), command.getBeforeState().getObjectVersion());
        assertEquals("sha256:" + cn.hutool.crypto.digest.DigestUtil.sha256Hex(
                command.getAfterState().getCanonicalJson()), command.getAfterState().getObjectVersion());
        assertEquals(command.getAfterState().getObjectVersion(), command.getSubjectVersion());
        org.junit.jupiter.api.Assertions.assertNotEquals(command.getBeforeState().getCanonicalJson(),
                command.getAfterState().getCanonicalJson());
        var before = com.alibaba.fastjson.JSON.parseObject(command.getBeforeState().getCanonicalJson());
        var after = com.alibaba.fastjson.JSON.parseObject(command.getAfterState().getCanonicalJson());
        assertEquals("pending_review", before.getString("reviewStatus"));
        assertEquals("closed", after.getString("reviewStatus"));
        assertEquals(disposition, after.getString("disposition"));
        assertEquals("9102", after.getJSONArray("materials").getJSONObject(0).getString("fileId"));
        assertEquals(java.util.Set.of("reviewId", "sourceType", "sourceId", "activeOrderId",
                "batchExecutionId", "batchStatus", "workOrderId", "previousWorkOrderTemporaryFrozen",
                "temporaryFrozen", "reviewStatus", "disposition", "nonconformanceReason", "reviewOpinion",
                "materials", "signatureRecordId", "signatureContentHash", "applicationId",
                "reworkActiveOrderId"), after.keySet());
        assertTrue(command.getLinks().contains(new cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditRelation(
                "SUBJECT", "NONCONFORMANCE_REVIEW", "1001", null, null)));
        assertTrue(command.getLinks().contains(new cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditRelation(
                "AFFECTED", "ACTIVE_ORDER", "8101", null, null)));
        assertTrue(command.getLinks().contains(new cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditRelation(
                "AFFECTED", "RELEASE_APPLICATION", "7001", null, null)));
        assertFalse(command.getLinks().stream().anyMatch(link -> "BATCH_EXECUTION".equals(link.objectType())));
        assertTrue(command.getAfterState().getCanonicalJson().contains("closed"));
        assertTrue(command.getAfterState().getCanonicalJson().contains(disposition));
        assertTrue(command.getAfterState().getCanonicalJson().contains("9102"), "actual material identity");
        assertTrue(command.getLinks().stream().anyMatch(link -> "7001".equals(link.objectId())),
                "actual release application must be linked");
        assertTrue(command.getLinks().stream().anyMatch(link -> "8101".equals(link.objectId())),
                "actual active order must be linked");
        assertTrue(command.getLinks().stream().anyMatch(link -> "1001".equals(link.objectId())),
                "actual review must be linked");
        assertTrue(command.getIdempotencyKey().length() <= 96);
        assertFalse(command.getBeforeState().getCanonicalJson().contains("qa-signature-password"));
        assertFalse(command.getAfterState().getCanonicalJson().contains("qa-signature-password"));
        verify(batchExecutionMapper, never()).insert(any(MesProEdhrBatchExecutionDO.class));
        verify(batchExecutionMapper, never()).updateById(any(MesProEdhrBatchExecutionDO.class));
        verify(operationAuditService).recordInCallerTransaction(any());
        if (!"concession_release".equals(disposition)) {
            assertTrue(command.getLinks().stream().anyMatch(link -> "8001".equals(link.objectId())
                    && "WORK_TASK".equals(link.objectType()) && "AFFECTED".equals(link.relationType())),
                    "completed PQC task must be linked");
        }
        if ("rework".equals(disposition)) {
            assertTrue(command.getLinks().stream().anyMatch(link -> "8201".equals(link.objectId())
                    && "ACTIVE_ORDER".equals(link.objectType()) && "REWORK_CYCLE".equals(link.relationType())),
                    "new rework cycle must be linked without manufacturing a batch");
        }
    }

    @Test
    void unifiedLedgerLockPrecedesFirstReviewBusinessLock() {
        prepareUnifiedDisposition("concession_release");
        service.dispose(disposeRequest("concession_release"));
        var order = org.mockito.Mockito.inOrder(unifiedAudit, reviewMapper, signatureService);
        order.verify(unifiedAudit).acquireLedgerLock();
        order.verify(reviewMapper).selectByIdForUpdate(1001L);
        order.verify(signatureService).recordQaDispositionSignature(any(), any(), any(), any(), any());
        order.verify(unifiedAudit).append(any());
    }

    @Test
    void unifiedAppendFailureEscapesAfterBusinessAndSpecializedAuditWrites() {
        stubPendingReview("concession_release", true, false);
        when(workOrderMapper.updateTemporaryFrozenByIds(List.of(3001L), false)).thenReturn(1);
        IllegalStateException failure = new IllegalStateException("unified-ledger-unavailable");
        org.mockito.Mockito.doThrow(failure).when(unifiedAudit).append(any());
        org.junit.jupiter.api.Assertions.assertSame(failure, assertThrows(IllegalStateException.class,
                () -> service.dispose(disposeRequest("concession_release"))));
        var order = org.mockito.Mockito.inOrder(reviewMapper, workOrderMapper, operationAuditService, unifiedAudit);
        order.verify(reviewMapper).updateById(any(MesProEdhrNonconformanceReviewDO.class));
        order.verify(workOrderMapper).updateTemporaryFrozenByIds(List.of(3001L), false);
        order.verify(operationAuditService).recordInCallerTransaction(any());
        order.verify(unifiedAudit).append(any());
        // Mockito proves ordering/propagation only; database rollback is a separate integration gate.
    }

    @Test
    void invalidDispositionNeverLeavesASuccessAuditFact() {
        assertThrows(ServiceException.class, () -> service.dispose(disposeRequest("unsupported")));
        verify(unifiedAudit, never()).append(any());
        verify(reviewMapper, never()).updateById(any(MesProEdhrNonconformanceReviewDO.class));
        verifyNoInteractions(signatureService, operationAuditService);
    }

    @Test
    void invalidSignatureNeverWritesReviewOrUnifiedSuccess() {
        stubPendingReview("concession_release", false);
        when(signatureService.recordQaDispositionSignature(any(), any(), any(), any(), any()))
                .thenThrow(exception(PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PASSWORD_INVALID));
        assertEquals(PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PASSWORD_INVALID.getCode(),
                assertThrows(ServiceException.class,
                        () -> service.dispose(disposeRequest("concession_release"))).getCode());
        verify(unifiedAudit, never()).append(any());
        verify(reviewMapper, never()).updateById(any(MesProEdhrNonconformanceReviewDO.class));
        verify(workOrderMapper, never()).updateTemporaryFrozenByIds(any(), any());
        verifyNoInteractions(operationAuditService);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
            "tenant", "actor", "action", "subject", "content", "hash"})
    void mismatchedPersistedSignatureRejectsBeforeBusinessWrites(String changedField) {
        stubPendingReview("concession_release", true, false);
        signatureMutation = record -> {
            switch (changedField) {
                case "tenant" -> record.setTenantId(999L);
                case "actor" -> record.setActorId(22L);
                case "action" -> record.setActionCode("NONCONFORMANCE_REVIEW_CREATE");
                case "subject" -> record.setSubjectId(MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(
                        0L, "QA_DISPOSITION", null, null, null, null, null, null, null,
                        "EDHR_NONCONFORMANCE_REVIEW", 9999L, "eDHR不合格评审处置",
                        "QA_DISPOSITION", null, null, "different-review", null));
                case "content" -> {
                    var json = com.alibaba.fastjson.JSON.parseObject(record.getCanonicalContentJson());
                    json.put("reviewSourceId", "9999");
                    record.setCanonicalContentJson(json.toJSONString());
                    record.setContentHash(cn.hutool.crypto.digest.DigestUtil.sha256Hex(record.getCanonicalContentJson()));
                }
                case "hash" -> record.setContentHash("0".repeat(64));
                default -> throw new AssertionError(changedField);
            }
            return record;
        };
        assertEquals(PRO_EDHR_NONCONFORMANCE_REVIEW_SOURCE_INVALID.getCode(),
                assertThrows(ServiceException.class,
                        () -> service.dispose(disposeRequest("concession_release"))).getCode());
        verify(signatureRecordMapper).selectById(9101L);
        verify(reviewMapper, never()).updateById(any(MesProEdhrNonconformanceReviewDO.class));
        verify(workOrderMapper, never()).updateTemporaryFrozenByIds(any(), any());
        verifyNoInteractions(operationAuditService);
        verify(unifiedAudit, never()).append(any());
    }

    @Test
    void reworkReplayWithMissingUnifiedReceiptRejectsWithoutRepairWrites() {
        prepareClosedReworkForReceiptTest();
        org.mockito.Mockito.doReturn(null).when(auditReceiptMapper)
                .selectByIdempotencyKeyForUpdate(122L, reworkReceiptKey());
        ServiceException error = assertThrows(ServiceException.class, () -> service.dispose(disposeRequest("rework")));
        assertTrue(error.getMessage().contains("GXP_AUDIT_RECEIPT_MISSING"),
                "The specified missing-receipt error must not be replaced by an unrelated source/state error");
        assertReplayHasNoWrites();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"tenant", "operation", "subject", "key"})
    void reworkReplayRejectsMismatchedReceiptIdentity(String changedField) {
        var receipt = prepareClosedReworkForReceiptTest();
        switch (changedField) {
            case "tenant" -> receipt.setTenantId(999L);
            case "operation" -> receipt.setOperationId("mes.nonconformance.void");
            case "subject" -> receipt.setSubjectId("9999");
            case "key" -> receipt.setIdempotencyKey("GXP2:" + "0".repeat(64));
            default -> throw new AssertionError(changedField);
        }
        org.mockito.Mockito.doReturn(receipt).when(auditReceiptMapper)
                .selectByIdempotencyKeyForUpdate(122L, reworkReceiptKey());
        assertEquals(cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.GXP_AUDIT_IDEMPOTENCY_CONFLICT.getCode(),
                assertThrows(ServiceException.class, () -> service.dispose(disposeRequest("rework"))).getCode());
        assertReplayHasNoWrites();
    }

    @Test
    void reworkReplayReadsTheOriginalReceiptAndPreservesItsIdentity() {
        var receipt = prepareClosedReworkForReceiptTest();
        org.mockito.Mockito.doReturn(receipt).when(auditReceiptMapper)
                .selectByIdempotencyKeyForUpdate(122L, reworkReceiptKey());
        assertEquals(1001L, service.dispose(disposeRequest("rework")).getId());
        verify(auditReceiptMapper).selectByIdempotencyKeyForUpdate(122L, reworkReceiptKey());
        assertEquals(88001L, receipt.getId());
        assertEquals("saved-event-hash", receipt.getEventHash());
        assertReplayHasNoWrites();
    }

    private cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO prepareClosedReworkForReceiptTest() {
        prepareUnifiedDisposition("rework");
        service.dispose(disposeRequest("rework"));
        var receipt = receiptFromLastAppend();
        var updated = ArgumentCaptor.forClass(MesProEdhrNonconformanceReviewDO.class);
        verify(reviewMapper).updateById(updated.capture());
        var closed = updated.getValue().setActiveOrderId(8101L).setWorkOrderId(3001L)
                .setSourceType("PQC_RELEASE").setSourceId(7001L);
        when(reviewMapper.selectByIdForUpdate(1001L)).thenReturn(closed);
        when(activeOrderMapper.selectByReworkReviewId(1001L)).thenReturn(new MesProcessPoolActiveOrderDO()
                .setId(8201L).setReworkSourceActiveOrderId(8101L).setReworkReviewId(1001L).setWorkOrderId(3001L));
        org.mockito.Mockito.clearInvocations(unifiedAudit, reviewMapper, signatureService, workOrderMapper,
                operationAuditService, activeOrderMapper, releaseApplicationMapper, workTaskMapper, auditReceiptMapper);
        return receipt;
    }

    private void assertReplayHasNoWrites() {
        verify(unifiedAudit, never()).append(any());
        verify(reviewMapper, never()).updateById(any(MesProEdhrNonconformanceReviewDO.class));
        verify(reviewMapper, never()).insert(any(MesProEdhrNonconformanceReviewDO.class));
        verify(workOrderMapper, never()).updateTemporaryFrozenByIds(any(), any());
        verify(activeOrderMapper, never()).insert(any(MesProcessPoolActiveOrderDO.class));
        verify(releaseApplicationMapper, never()).closeFromNonconformance(any(), any(), any(), any(), any(), any(), any());
        verify(workTaskMapper, never()).completePqcDecisionTask(any(), any(), any());
        verifyNoInteractions(signatureService, operationAuditService);
    }

    private String reworkReceiptKey() {
        var identity = new java.util.TreeMap<String, Object>();
        identity.put("tenantId", "122");
        identity.put("operationId", "mes.nonconformance.rework");
        identity.put("identity", List.of("1001", "rework"));
        return "GXP2:" + cn.hutool.crypto.digest.DigestUtil.sha256Hex(
                cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(identity));
    }

    private cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO receiptFromLastAppend() {
        var command = org.mockito.Mockito.mockingDetails(unifiedAudit).getInvocations().stream()
                .filter(i -> i.getMethod().getName().equals("append")).reduce((a, b) -> b)
                .orElseThrow().<cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand>getArgument(0);
        var receipt = new cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO();
        receipt.setId(88001L);
        receipt.setTenantId(122L);
        receipt.setOperationId(command.getOperationId());
        receipt.setSubjectType("NONCONFORMANCE_REVIEW");
        receipt.setSubjectId(command.getSubjectId());
        receipt.setSubjectVersion(command.getSubjectVersion());
        receipt.setIdempotencyKey(command.getIdempotencyKey());
        receipt.setSignatureRecordId(command.getSignatureRecordId());
        receipt.setSignatureContentHash(command.getSignatureContentHash());
        receipt.setResultStatus("SUCCESS");
        receipt.setActorId(21L);
        receipt.setEventHash("saved-event-hash");
        receipt.setAfterStateJson(command.getAfterState().getCanonicalJson());
        return receipt;
    }

    private void assertCreationAudit(String reviewId, String sourceType, String sourceId, String signatureId, String reason) {
        var captured = ArgumentCaptor.forClass(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand.class);
        verify(unifiedAudit).append(captured.capture());
        var command = captured.getValue();
        assertEquals("mes.nonconformance.create", command.getOperationId());
        assertEquals(reviewId, command.getSubjectId());
        assertEquals(reason, command.getReason());
        assertEquals("USER", command.getReasonSource());
        assertEquals("ABSENT", command.getBeforeState().getState());
        assertEquals("PRESENT", command.getAfterState().getState());
        assertEquals(signatureId, command.getSignatureRecordId());
        var after = com.alibaba.fastjson.JSON.parseObject(command.getAfterState().getCanonicalJson());
        assertEquals(reviewId, after.getString("reviewId"));
        assertEquals(sourceType, after.getString("sourceType"));
        assertEquals(sourceId, after.getString("sourceId"));
        assertEquals("pending_review", after.getString("reviewStatus"));
        assertEquals("sha256:" + cn.hutool.crypto.digest.DigestUtil.sha256Hex(command.getAfterState().getCanonicalJson()),
                command.getSubjectVersion());
    }

    private void prepareUnifiedDisposition(String disposition) {
        stubPendingReview(disposition);
        lenient().when(workOrderMapper.updateTemporaryFrozenByIds(List.of(3001L), "void".equals(disposition))).thenReturn(1);
        // Return a separate post-write review, so before-state capture cannot rely on a mutated stub.
        org.mockito.Mockito.lenient().doAnswer(invocation -> {
            MesProEdhrNonconformanceReviewDO update = invocation.getArgument(0);
            MesProEdhrNonconformanceReviewDO persisted = new MesProEdhrNonconformanceReviewDO();
            org.springframework.beans.BeanUtils.copyProperties(reviewMapper.selectByIdForUpdate(1001L), persisted);
            persisted.setReviewStatus(update.getReviewStatus()).setDisposition(update.getDisposition())
                    .setReviewOpinion(update.getReviewOpinion()).setReviewMaterialsJson(update.getReviewMaterialsJson())
                    .setReviewMaterialFileId(update.getReviewMaterialFileId()).setQaSignature(update.getQaSignature())
                    .setTraceSnapshotJson(update.getTraceSnapshotJson()).setClosedAt(update.getClosedAt());
            when(reviewMapper.selectById(1001L)).thenReturn(persisted);
            return 1;
        }).when(reviewMapper).updateById(any(MesProEdhrNonconformanceReviewDO.class));
        if (!"concession_release".equals(disposition)) {
            String decision = "NONCONFORMANCE_" + disposition.toUpperCase(java.util.Locale.ROOT);
            when(releaseApplicationMapper.closeFromNonconformance(eq(7001L), eq(1), eq(decision),
                    eq(21L), any(), any(), any())).thenReturn(1);
            when(workTaskMapper.completePqcDecisionTask(eq(8001L), any(), eq(decision))).thenReturn(1);
        }
    }

    private void stubPendingReview(String disposition) {
        stubPendingReview(disposition, true);
    }

    private void stubPendingReview(String disposition, boolean successfulSignature) {
        stubPendingReview(disposition, successfulSignature, successfulSignature);
    }

    private void stubPendingReview(String disposition, boolean successfulSignature, boolean returnsResponse) {
        MesProEdhrNonconformanceReviewDO review = MesProEdhrNonconformanceReviewDO.builder()
                .id(1001L)
                .sourceType("PQC_RELEASE")
                .sourceId(7001L)
                .activeOrderId(8101L)
                .workOrderId(3001L)
                .workOrderCode("WO-001")
                .reviewStatus("pending_review")
                .previousWorkOrderTemporaryFrozen(false)
                .frozenAt(LocalDateTime.of(2026, 9, 13, 8, 0))
                .nonconformanceReason("检验结论需要评审")
                .build();
        when(reviewMapper.selectByIdForUpdate(1001L)).thenReturn(review);
        if (returnsResponse) {
            when(reviewMapper.selectById(1001L)).thenReturn(review.setDisposition(disposition));
        }
        lenient().when(reviewMapper.selectFreezeLifecycleByWorkOrderId(3001L)).thenReturn(
                java.util.List.of(review));
        when(releaseApplicationMapper.selectByIdForUpdate(7001L)).thenReturn(
                new MesProcessPoolActiveOrderReleaseApplicationDO()
                        .setId(7001L)
                        .setActiveOrderId(8101L)
                        .setApplicationStatus(MesReleaseFlowStatus.PQC_RELEASE_PENDING)
                        .setVersion(1)
                        .setWorkOrderId(3001L)
                        .setPqcReleaseWorkTaskId(8001L));
        when(workOrderMapper.selectByIdForUpdate(3001L)).thenReturn(
                new MesProWorkOrderDO().setId(3001L).setTemporaryFrozen(true));
        if (successfulSignature) {
            when(signatureService.recordQaDispositionSignature(eq(21L), eq(1001L), eq("qa-signature-password"),
                    eq("void".equals(disposition) ? "作废处理" :
                            "rework".equals(disposition) ? "返工处理" : "让步放行"), any()))
                    .thenReturn(9101L);
        }
        if (!"concession_release".equals(disposition)) {
            when(workTaskMapper.selectByIdForUpdate(8001L)).thenReturn(new MesProEdhrWorkTaskDO()
                    .setId(8001L)
                    .setTaskType("PQC_PRODUCTION_RELEASE")
                    .setBusinessScopeType("RELEASE_APPLICATION")
                    .setBusinessScopeId(7001L)
                    .setStatus(MesProEdhrWorkTaskStatus.TODO));
        }
    }

    private MesProEdhrNonconformanceReviewDisposeReqVO disposeRequest(String disposition) {
        return new MesProEdhrNonconformanceReviewDisposeReqVO()
                .setId(1001L)
                .setDisposition(disposition)
                .setReviewMaterialUrl(REVIEW_MATERIAL_URL)
                .setReviewMaterials(reviewMaterials(REVIEW_MATERIAL_URL))
                .setReviewOpinion("void".equals(disposition) ? "作废处理" :
                        "rework".equals(disposition) ? "返工处理" : "让步放行")
                .setSignaturePassword("qa-signature-password");
    }

    @Test
    void reworkKeepsIndependentWorkOrderFreezeAndNewCycleCannotBypassIt() {
        stubPendingReview("rework");
        var review = reviewMapper.selectByIdForUpdate(1001L);
        review.setPreviousWorkOrderTemporaryFrozen(true);
        when(workOrderMapper.updateTemporaryFrozenByIds(List.of(3001L), true)).thenReturn(1);
        when(releaseApplicationMapper.closeFromNonconformance(eq(7001L), eq(1), eq("NONCONFORMANCE_REWORK"),
                eq(21L), any(), eq("返工处理"), any())).thenReturn(1);
        when(workTaskMapper.completePqcDecisionTask(eq(8001L), any(), eq("NONCONFORMANCE_REWORK"))).thenReturn(1);

        service.dispose(disposeRequest("rework"));

        verify(workOrderMapper).updateTemporaryFrozenByIds(List.of(3001L), true);
        verify(activeOrderMapper).insert(argThat((MesProcessPoolActiveOrderDO next) ->
                "ACTIVE".equals(next.getBusinessStatus()) && Long.valueOf(3001L).equals(next.getWorkOrderId())));
        assertThrows(ServiceException.class, () -> service.ensureWorkOrderNotFrozen(3001L, "返工生产"));
    }

    private List<MesProEdhrNonconformanceReviewDisposeReqVO.ReviewMaterialReqVO> reviewMaterials(String url) {
        return java.util.List.of(new MesProEdhrNonconformanceReviewDisposeReqVO.ReviewMaterialReqVO()
                .setFileId(materialIds.get(url)).setUrl(url)
                .setFileName("review.pdf")
                .setSortNo(1));
    }

    private void stubFormalFile(String requestedUrl, FileDO file) {
        file.setPath("mes/edhr-ncr/reviews/1001/upload-" + file.getId() + "/" + file.getPath());
        materialIds.put(requestedUrl, file.getId());
        lenient().when(fileMapper.selectById(file.getId())).thenReturn(file);
    }
    private MesProEdhrNonconformanceReviewDisposeReqVO disposeRequestWithEvent(String url) {
        return new MesProEdhrNonconformanceReviewDisposeReqVO()
                .setId(1001L)
                .setDisposition("concession_release")
                .setReviewMaterialUrl(url)
                .setReviewMaterials(reviewMaterials(url))
                .setReviewMaterialEvents(java.util.List.of(
                        new MesProEdhrNonconformanceReviewDisposeReqVO.ReviewMaterialEventReqVO()
                                .setAction("UPLOAD").setFileId(materialIds.get(url)).setUrl(url).setFileName("review.pdf").setSequence(1)))
                .setReviewOpinion("让步放行")
                .setSignaturePassword("qa-signature-password");
    }

    private void stubBatchOrigin(Long batchExecutionId, Long activeOrderId) {
        when(batchExecutionOriginMapper.selectListByBatchExecutionId(batchExecutionId)).thenReturn(
                java.util.List.of(new MesProEdhrBatchExecutionOriginDO()
                        .setBatchExecutionId(batchExecutionId)
                        .setActiveOrderId(activeOrderId)));
    }

    private MesProEdhrNonconformanceReviewDisposeReqVO disposeRequest(Long reviewId, String disposition) {
        var request = disposeRequest(disposition).setId(reviewId);
        Long fileId = 100000L + reviewId;
        lenient().when(fileMapper.selectById(fileId)).thenReturn(FileDO.builder().id(fileId)
                .configId(10L).name("review.pdf").url(REVIEW_MATERIAL_URL)
                .path("mes/edhr-ncr/reviews/" + reviewId + "/upload-1/review.pdf").build());
        request.getReviewMaterials().get(0).setFileId(fileId);
        return request;
    }
}
