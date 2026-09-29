package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationHandlingSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeviationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeviationHandlingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrOperationAuditEventDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrDeviationHandlingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrDeviationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrOperationAuditEventMapper;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureEvidenceDTO;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import com.alibaba.fastjson.JSON;

import java.time.LocalDateTime;

import java.util.List;

import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_DEVIATION_HANDLING_SIGNATURE_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_DEVIATION_HANDLING_REVISION_REASON_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_DEVIATION_HANDLING_VERSION_CONFLICT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProEdhrDeviationHandlingServiceTest {

    @Mock
    private MesProEdhrDeviationMapper deviationMapper;
    @Mock
    private MesProEdhrDeviationHandlingMapper handlingMapper;
    @Mock
    private ElectronicSignatureRecordMapper signatureRecordMapper;
    @Mock
    private MesProBatchRecordExecutionSignatureService signatureService;
    @Mock
    private MesProEdhrOperationAuditService operationAuditService;
    @Mock
    private MesProEdhrOperationAuditEventMapper operationAuditEventMapper;
    @Mock
    private ElectronicSignatureQueryService signatureQueryService;
    @Mock
    private SecurityFrameworkService securityFrameworkService;

    @Mock private cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService gxpAuditService;
    private MesProEdhrDeviationHandlingServiceImpl service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(71L);
        service = new MesProEdhrDeviationHandlingServiceImpl(
                deviationMapper, handlingMapper, signatureRecordMapper, signatureService, operationAuditService);
        ReflectionTestUtils.setField(service, "gxpAuditService", gxpAuditService);
        ReflectionTestUtils.setField(service, "signatureQueryService", signatureQueryService);
        ReflectionTestUtils.setField(service, "operationAuditEventMapper", operationAuditEventMapper);
        ReflectionTestUtils.setField(service, "securityFrameworkService", securityFrameworkService);
        lenient().when(securityFrameworkService.hasPermission(anyString())).thenReturn(true);
        lenient().when(securityFrameworkService.hasAnyPermissions(any(String[].class))).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void ledgerLockPrecedesBusinessLocks() {
        assertThrows(ServiceException.class, () -> service.sign(201L, 901L, "QA", "signature", "review", 1, "current-hash"));
        var order = org.mockito.Mockito.inOrder(gxpAuditService, deviationMapper);
        order.verify(gxpAuditService).acquireLedgerLock();
        order.verify(deviationMapper).selectByTenantAndIdForUpdate(71L, 901L);
    }

    @Test
    void firstSaveAndRevisionReuseOneHandlingRowAndChangeContentVersion() {
        MesProEdhrDeviationDO deviation = openDeviation("NORMAL");
        when(deviationMapper.selectByTenantAndIdForUpdate(71L, 901L)).thenReturn(deviation);
        when(handlingMapper.selectByTenantAndDeviationIdForUpdate(71L, 901L))
                .thenReturn(null)
                .thenReturn(newHandling(7701L, 1, "old-hash"));
        doAnswer(invocation -> {
            MesProEdhrDeviationHandlingDO inserted = invocation.getArgument(0);
            inserted.setId(7701L);
            return 1;
        }).when(handlingMapper).insert(any(MesProEdhrDeviationHandlingDO.class));
        when(handlingMapper.updateContent(eq(71L), eq(901L), eq(1), any(MesProEdhrDeviationHandlingDO.class)))
                .thenReturn(1);

        var first = service.save(201L, 901L, completeRequest(0));
        var revised = service.save(201L, 901L, completeRequest(1)
                .setVerificationContent("重新验证通过")
                .setRevisionReason("验证不合格后修订调查结论"));

        assertEquals(7701L, first.getId());
        assertEquals(1, first.getContentVersion());
        assertEquals(7701L, revised.getId());
        assertEquals(2, revised.getContentVersion());
        assertNotEquals(first.getContentHash(), revised.getContentHash());
        verify(handlingMapper).updateContent(eq(71L), eq(901L), eq(1), any(MesProEdhrDeviationHandlingDO.class));
        verify(operationAuditService).recordInCallerTransaction(any(MesProEdhrOperationAuditCommand.class));
    }

    @Test
    void staleContentVersionIsRejectedWithoutCreatingAnotherHandlingRow() {
        when(deviationMapper.selectByTenantAndIdForUpdate(71L, 901L)).thenReturn(openDeviation("NORMAL"));
        when(handlingMapper.selectByTenantAndDeviationIdForUpdate(71L, 901L))
                .thenReturn(newHandling(7701L, 2, "current-hash"));

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.save(201L, 901L, completeRequest(1)));

        assertEquals(PRO_EDHR_DEVIATION_HANDLING_VERSION_CONFLICT.getCode(), error.getCode());
        verify(handlingMapper, never()).insert(any(MesProEdhrDeviationHandlingDO.class));
        verify(handlingMapper, never()).updateContent(anyLong(), anyLong(), anyInt(), any());
    }

    @Test
    void revisionRequiresReasonAndDoesNotWriteCurrentContentWithoutIt() {
        when(deviationMapper.selectByTenantAndIdForUpdate(71L, 901L)).thenReturn(openDeviation("NORMAL"));
        when(handlingMapper.selectByTenantAndDeviationIdForUpdate(71L, 901L))
                .thenReturn(newHandling(7701L, 1, "current-hash"));

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.save(201L, 901L, completeRequest(1)));

        assertEquals(PRO_EDHR_DEVIATION_HANDLING_REVISION_REASON_REQUIRED.getCode(), error.getCode());
        verify(handlingMapper, never()).updateContent(anyLong(), anyLong(), anyInt(), any());
        verify(operationAuditService, never()).recordInCallerTransaction(any());
    }

    @Test
    void normalCloseAllowsQaAndQualityInEitherOrderButRequiresEveryCommonNode() {
        MesProEdhrDeviationDO deviation = openDeviation("NORMAL").setInitiatorSignatureId(8801L);
        MesProEdhrDeviationHandlingDO handling = completeHandling(7701L, 1);
        when(deviationMapper.selectByTenantAndIdForUpdate(71L, 901L)).thenReturn(deviation);
        when(handlingMapper.selectByTenantAndDeviationIdForUpdate(71L, 901L)).thenReturn(handling);
        stubSignatures(deviation, handling, MesProEdhrDeviationHandlingServiceImpl.NODE_PREPARER,
                MesProEdhrDeviationHandlingServiceImpl.NODE_VERIFIER,
                MesProEdhrDeviationHandlingServiceImpl.NODE_DEPARTMENT_OWNER,
                MesProEdhrDeviationHandlingServiceImpl.NODE_QUALITY_OWNER,
                MesProEdhrDeviationHandlingServiceImpl.NODE_QA);
        when(deviationMapper.closeNormally(eq(71L), eq(901L), eq("NORMAL_COMPLETED"), any()))
                .thenReturn(1);

        service.closeNormally(201L, 901L);

        verify(deviationMapper).closeNormally(eq(71L), eq(901L), eq("NORMAL_COMPLETED"), any());
    }

    @Test
    void criticalCloseRequiresManagementRepresentativeSignature() {
        MesProEdhrDeviationDO deviation = openDeviation("CRITICAL").setInitiatorSignatureId(8801L);
        MesProEdhrDeviationHandlingDO handling = completeHandling(7701L, 1);
        when(deviationMapper.selectByTenantAndIdForUpdate(71L, 901L)).thenReturn(deviation);
        when(handlingMapper.selectByTenantAndDeviationIdForUpdate(71L, 901L)).thenReturn(handling);
        stubSignatures(deviation, handling, MesProEdhrDeviationHandlingServiceImpl.NODE_PREPARER,
                MesProEdhrDeviationHandlingServiceImpl.NODE_VERIFIER,
                MesProEdhrDeviationHandlingServiceImpl.NODE_DEPARTMENT_OWNER,
                MesProEdhrDeviationHandlingServiceImpl.NODE_QA,
                MesProEdhrDeviationHandlingServiceImpl.NODE_QUALITY_OWNER);

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.closeNormally(201L, 901L));

        assertEquals(PRO_EDHR_DEVIATION_HANDLING_SIGNATURE_REQUIRED.getCode(), error.getCode());
        verify(deviationMapper, never()).closeNormally(anyLong(), anyLong(), anyString(), any());
    }

    @Test
    void signatureFromPreviousContentVersionCannotCloseCurrentRevision() {
        MesProEdhrDeviationDO deviation = openDeviation("NORMAL").setInitiatorSignatureId(8801L);
        MesProEdhrDeviationHandlingDO handling = completeHandling(7701L, 2);
        when(deviationMapper.selectByTenantAndIdForUpdate(71L, 901L)).thenReturn(deviation);
        when(handlingMapper.selectByTenantAndDeviationIdForUpdate(71L, 901L)).thenReturn(handling);
        stubSignatures(deviation, newHandling(7701L, 1, "old-hash"),
                MesProEdhrDeviationHandlingServiceImpl.NODE_PREPARER,
                MesProEdhrDeviationHandlingServiceImpl.NODE_VERIFIER,
                MesProEdhrDeviationHandlingServiceImpl.NODE_DEPARTMENT_OWNER,
                MesProEdhrDeviationHandlingServiceImpl.NODE_QA,
                MesProEdhrDeviationHandlingServiceImpl.NODE_QUALITY_OWNER);

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.closeNormally(201L, 901L));

        assertEquals(PRO_EDHR_DEVIATION_HANDLING_SIGNATURE_REQUIRED.getCode(), error.getCode());
        verify(deviationMapper, never()).closeNormally(anyLong(), anyLong(), anyString(), any());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource(value = {
            "1,current-hash", "2,old-hash", "NULL,current-hash", "2,NULL", "2,''"
    }, nullValues = "NULL")
    void reviewedContentMustMatchLockedVersionBeforeSigning(Integer version, String hash) {
        when(deviationMapper.selectByTenantAndIdForUpdate(71L, 901L)).thenReturn(openDeviation("NORMAL"));
        when(handlingMapper.selectByTenantAndDeviationIdForUpdate(71L, 901L))
                .thenReturn(newHandling(7701L, 2, "current-hash"));
        var error = assertThrows(ServiceException.class,
                () -> service.sign(201L, 901L, "QA", "password", "review", version, hash));

        assertEquals(PRO_EDHR_DEVIATION_HANDLING_VERSION_CONFLICT.getCode(), error.getCode());
        org.mockito.Mockito.verifyNoInteractions(signatureService);
    }

    @Test
    void sameActorCanSignMultipleIndependentHandlingNodes() {
        MesProEdhrDeviationDO deviation = openDeviation("CRITICAL");
        MesProEdhrDeviationHandlingDO handling = newHandling(7701L, 1, "current-hash");
        when(deviationMapper.selectByTenantAndIdForUpdate(71L, 901L)).thenReturn(deviation);
        when(handlingMapper.selectByTenantAndDeviationIdForUpdate(71L, 901L)).thenReturn(handling);
        when(signatureService.recordDeviationHandlingSignature(eq(201L), eq(901L), eq(7701L),
                eq("PC-202609-0001"), anyString(), eq(1), eq("current-hash"), eq("password"), anyString()))
                .thenReturn(9901L, 9902L);

        Long first = service.sign(201L, 901L, MesProEdhrDeviationHandlingServiceImpl.NODE_QA,
                "password", "QA确认", 1, "current-hash");
        Long second = service.sign(201L, 901L, MesProEdhrDeviationHandlingServiceImpl.NODE_QUALITY_OWNER,
                "password", "质量批准", 1, "current-hash");

        assertEquals(9901L, first);
        assertEquals(9902L, second);
        verify(signatureService).recordDeviationHandlingSignature(eq(201L), eq(901L), eq(7701L),
                eq("PC-202609-0001"), eq(MesProEdhrDeviationHandlingServiceImpl.NODE_QA), eq(1),
                eq("current-hash"), eq("password"), eq("QA确认"));
        verify(signatureService).recordDeviationHandlingSignature(eq(201L), eq(901L), eq(7701L),
                eq("PC-202609-0001"), eq(MesProEdhrDeviationHandlingServiceImpl.NODE_QUALITY_OWNER), eq(1),
                eq("current-hash"), eq("password"), eq("质量批准"));
    }

    @Test
    void detailReadReturnsOnlyCurrentVersionSignaturesFromUnifiedStore() {
        MesProEdhrDeviationDO deviation = openDeviation("NORMAL");
        MesProEdhrDeviationHandlingDO handling = completeHandling(7701L, 1);
        when(deviationMapper.selectByTenantAndId(71L, 901L)).thenReturn(deviation);
        when(handlingMapper.selectByTenantAndDeviationId(71L, 901L)).thenReturn(handling);
        stubSignatures(deviation, handling, MesProEdhrDeviationHandlingServiceImpl.NODE_PREPARER);

        var result = service.get(901L);

        assertEquals(7701L, result.getId());
        assertEquals(1000L, result.getEffectiveSignatureIds()
                .get(MesProEdhrDeviationHandlingServiceImpl.NODE_PREPARER));
        assertEquals(1, result.getEffectiveSignatureIds().size());
    }

    @Test
    void detailReadReturnsRealSignerNameInSignatureEvidence() {
        MesProEdhrDeviationDO deviation = openDeviation("NORMAL");
        MesProEdhrDeviationHandlingDO handling = completeHandling(7701L, 1);
        when(deviationMapper.selectByTenantAndId(71L, 901L)).thenReturn(deviation);
        when(handlingMapper.selectByTenantAndDeviationId(71L, 901L)).thenReturn(handling);
        stubSignatures(deviation, handling, MesProEdhrDeviationHandlingServiceImpl.NODE_PREPARER);
        when(signatureQueryService.listBySubject(anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    String subjectId = invocation.getArgument(2);
                    return List.of(new ElectronicSignatureEvidenceDTO(1000L, "MES", "DEVIATION_HANDLING_PREPARE",
                        "BATCH_EXECUTION", subjectId,
                        MesBatchRecordSignatureSubjectAdapter.subjectVersion(subjectId), 201L, "PREPARE", "编制", "原因",
                        LocalDateTime.of(2026, 9, 25, 4, 0), "SERVER_CLOCK:2026-09-25T04:00", "SESSION_PLUS_PASSWORD",
                        "content-hash", "evidence-hash", "SHA-256", "v1", "policy-v1", "VALID", null, null,
                        "PREPARER", 1, "{}", null, null, null, "张三"));
                });

        var result = service.get(901L);

        assertEquals("张三", result.getSignatureEvidence().get(0).getActorDisplayName());
    }

    @Test
    void detailReadReturnsRevisionHistoryAndDistinguishesSupersededSignatures() {
        MesProEdhrDeviationDO deviation = openDeviation("NORMAL");
        MesProEdhrDeviationHandlingDO handling = completeHandling(7701L, 2)
                .setContentHash("current-hash");
        when(deviationMapper.selectByTenantAndId(71L, 901L)).thenReturn(deviation);
        when(handlingMapper.selectByTenantAndDeviationId(71L, 901L)).thenReturn(handling);
        when(operationAuditEventMapper.selectRevisionListByObject(
                eq("EDHR_DEVIATION_HANDLING"), eq("7701")))
                .thenReturn(List.of(MesProEdhrOperationAuditEventDO.builder()
                        .id(5001L)
                        .objectType("EDHR_DEVIATION_HANDLING")
                        .objectId("7701")
                        .operationType("UPDATE")
                        .resultStatus("SUCCESS")
                        .actorUserId(201L)
                        .actorUsername("qa-user")
                        .occurredAt(LocalDateTime.of(2026, 9, 25, 4, 10))
                        .beforeSummaryHash("old-hash")
                        .afterSummaryHash("current-hash")
                        .previousAuditHash("previous-audit")
                        .auditHash("current-audit")
                        .metadataJson(JSON.toJSONString(java.util.Map.of(
                                "revisionReason", "验证不合格后修订",
                                "beforeContent", "{\"rootCauseAnalysis\":\"旧根因\"}",
                                "afterContent", "{\"rootCauseAnalysis\":\"新根因\"}",
                                "beforeContentVersion", 1,
                                "beforeContentHash", "old-hash")))
                        .build()));
        when(signatureQueryService.listBySubject(anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    String subjectId = invocation.getArgument(2);
                    return List.of(new ElectronicSignatureEvidenceDTO(1000L, "MES", "DEVIATION_HANDLING_PREPARE",
                            "MES_BATCH_RECORD", subjectId,
                            MesBatchRecordSignatureSubjectAdapter.subjectVersion(subjectId), 201L,
                            "PREPARE", "偏差处理编制", "原因", LocalDateTime.of(2026, 9, 25, 4, 0),
                            "SERVER_CLOCK:2026-09-25T04:00", "SESSION_PLUS_PASSWORD", "current-hash",
                            "evidence-hash", "SHA-256", "v1", "policy-v1", "VALID", null, null,
                            "PREPARER", 1, "{}", null, null, null, "张三"));
                });

        var result = service.get(901L);

        assertEquals(1, result.getRevisionHistory().size());
        var revision = result.getRevisionHistory().get(0);
        assertEquals(5001L, revision.getAuditId());
        assertEquals("验证不合格后修订", revision.getRevisionReason());
        assertEquals("{\"rootCauseAnalysis\":\"旧根因\"}", revision.getBeforeContent());
        assertEquals("{\"rootCauseAnalysis\":\"新根因\"}", revision.getAfterContent());
        assertEquals(201L, revision.getActorUserId());
        assertEquals("previous-audit", revision.getPreviousAuditHash());
        assertEquals("current-audit", revision.getAuditHash());
        assertEquals(5, result.getSignatureHistory().stream()
                .filter(signature -> "SUPERSEDED_INVALID".equals(signature.getValidityStatus())).count());
        assertEquals(5, result.getSignatureHistory().stream()
                .filter(signature -> "CURRENT_VALID".equals(signature.getValidityStatus())).count());
    }

    private MesProEdhrDeviationDO openDeviation(String level) {
        return new MesProEdhrDeviationDO().setId(901L).setTenantId(71L)
                .setDeviationCode("PC-202609-0001").setLevel(level).setStatus("OPEN");
    }

    private MesProEdhrDeviationHandlingDO completeHandling(Long id, int version) {
        MesProEdhrDeviationHandlingDO handling = newHandling(id, version, null);
        handling.setContentHash(service.contentHash(handling));
        return handling;
    }

    private MesProEdhrDeviationHandlingDO newHandling(Long id, int version, String hash) {
        return new MesProEdhrDeviationHandlingDO().setId(id).setTenantId(71L).setDeviationId(901L)
                .setContentVersion(version).setContentHash(hash)
                .setRootCauseAnalysis("根因").setImpactScope("影响范围")
                .setRiskAssessment("风险评估").setProductDisposition("产品处置")
                .setHandlingConclusion(MesProEdhrDeviationHandlingServiceImpl.HANDLING_CONCLUSION_CLOSED_LOOP)
                .setVerificationResult(MesProEdhrDeviationHandlingServiceImpl.VERIFICATION_RESULT_PASS)
                .setVerificationContent("验证完成");
    }

    private MesProEdhrDeviationHandlingSaveReqVO completeRequest(int version) {
        return new MesProEdhrDeviationHandlingSaveReqVO()
                .setExpectedContentVersion(version)
                .setRootCauseAnalysis("根因").setImpactScope("影响范围")
                .setRiskAssessment("风险评估").setProductDisposition("产品处置")
                .setHandlingConclusion(MesProEdhrDeviationHandlingServiceImpl.HANDLING_CONCLUSION_CLOSED_LOOP)
                .setVerificationResult(MesProEdhrDeviationHandlingServiceImpl.VERIFICATION_RESULT_PASS)
                .setVerificationContent("验证完成");
    }

    private void stubSignatures(MesProEdhrDeviationDO deviation, MesProEdhrDeviationHandlingDO handling,
                                String... nodes) {
        List<ElectronicSignatureRecordDO> result = new java.util.ArrayList<>();
        List<String> orderedNodes = List.of(
                MesProEdhrDeviationHandlingServiceImpl.NODE_PREPARER,
                MesProEdhrDeviationHandlingServiceImpl.NODE_VERIFIER,
                MesProEdhrDeviationHandlingServiceImpl.NODE_DEPARTMENT_OWNER,
                MesProEdhrDeviationHandlingServiceImpl.NODE_QA,
                MesProEdhrDeviationHandlingServiceImpl.NODE_QUALITY_OWNER);
        int index = 0;
        for (String node : orderedNodes) {
            if (!java.util.Arrays.asList(nodes).contains(node)) {
                continue;
            }
            String intent = "偏差 " + deviation.getDeviationCode() + " 处理节点 " + node;
            String subjectId = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(
                    0L, actionFor(node), null, null, null, null, null, null, null,
                    MesProEdhrDeviationHandlingServiceImpl.SOURCE_TYPE, handling.getId(), intent, node,
                    (long) handling.getContentVersion(), handling.getContentHash(), handling.getContentHash(), null);
            result.add(ElectronicSignatureRecordDO.builder().id(1000L + index++)
                    .subjectVersion(MesBatchRecordSignatureSubjectAdapter.subjectVersion(subjectId))
                    .verificationStatus("VALID").build());
        }
        when(signatureRecordMapper.selectOne(any())).thenAnswer(invocation ->
                result.isEmpty() ? null : result.remove(0));
    }

    private String actionFor(String node) {
        return switch (node) {
            case MesProEdhrDeviationHandlingServiceImpl.NODE_PREPARER -> MesProBatchRecordExecutionSignatureService.ACTION_DEVIATION_HANDLING_PREPARE;
            case MesProEdhrDeviationHandlingServiceImpl.NODE_VERIFIER -> MesProBatchRecordExecutionSignatureService.ACTION_DEVIATION_HANDLING_VERIFY;
            case MesProEdhrDeviationHandlingServiceImpl.NODE_DEPARTMENT_OWNER -> MesProBatchRecordExecutionSignatureService.ACTION_DEVIATION_HANDLING_DEPARTMENT;
            case MesProEdhrDeviationHandlingServiceImpl.NODE_QA -> MesProBatchRecordExecutionSignatureService.ACTION_DEVIATION_HANDLING_QA;
            case MesProEdhrDeviationHandlingServiceImpl.NODE_QUALITY_OWNER -> MesProBatchRecordExecutionSignatureService.ACTION_DEVIATION_HANDLING_QUALITY;
            case MesProEdhrDeviationHandlingServiceImpl.NODE_MANAGEMENT_REP -> MesProBatchRecordExecutionSignatureService.ACTION_DEVIATION_HANDLING_MANAGEMENT;
            default -> throw new IllegalArgumentException(node);
        };
    }
}
