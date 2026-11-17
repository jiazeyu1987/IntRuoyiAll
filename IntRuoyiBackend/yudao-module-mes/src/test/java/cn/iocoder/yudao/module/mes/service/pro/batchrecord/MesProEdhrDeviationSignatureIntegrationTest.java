package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationCreateReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeviationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrDeviationService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditAppendResult;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import javax.sql.DataSource;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_DEVIATION_SIGNATURE_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import({MesProEdhrDeviationServiceImpl.class, MesProEdhrDeviationNumberGenerator.class})
class MesProEdhrDeviationSignatureIntegrationTest extends BaseDbUnitTest {

    @Resource
    private MesProEdhrDeviationServiceImpl deviationService;
    @org.springframework.boot.test.mock.mockito.MockBean
    private MesProEdhrDeviationAccessService accessService;
    @Resource
    private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Resource
    private DataSource dataSource;

    @MockitoBean
    private MesProBatchRecordExecutionSignatureService signatureService;
    @MockitoBean
    private GxpAuditService gxpAuditService;

    @MockitoBean
    private MesDeviationAdministratorService administratorService;

    @org.junit.jupiter.api.BeforeEach
    void stubAdministrator() {
        when(administratorService.requireReceiver(701L)).thenReturn(
                new cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesDeviationAdministratorRespVO()
                        .setId(701L).setNickname("发起管理员"));
        when(administratorService.requireReceiver(801L)).thenReturn(
                new cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesDeviationAdministratorRespVO()
                        .setId(801L).setNickname("偏差管理员"));
    }

    @MockitoBean
    private MesProEdhrWorkTaskService workTaskService;

    @MockitoBean
    private cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper activeOrderMapper;
    @MockitoBean
    private cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper workOrderMapper;

    private MesProEdhrDeviationCreateReqVO productionRequest() {
        TenantContextHolder.setTenantId(1L);
        var order = new cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO()
                .setId(91001L).setWorkOrderId(81001L).setLeaderUserId(701L)
                .setActiveStatus("ACTIVE").setBusinessStatus("ACTIVE");
        order.setTenantId(1L);
        when(activeOrderMapper.selectById(91001L)).thenReturn(order);
        when(activeOrderMapper.selectByIdForUpdate(91001L)).thenReturn(order);
        when(workOrderMapper.selectById(81001L)).thenReturn(
                new cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO()
                        .setId(81001L).setCode("WO-PRODUCTION").setBatchCode("LOT-PRODUCTION"));
        return request(null).setActiveOrderId(91001L).setReceiverId(801L).setSignaturePassword("test-production-credential");
    }

    @Test
    void productionOrderPersistsNullableBatchSignatureHashAndDispatchesExactSource() {
        var req = productionRequest();
        when(signatureService.recordActiveOrderDeviationInitiationSignature(eq(701L), eq(91001L), anyLong(),
                anyString(), eq("WO-PRODUCTION"), anyString(), anyString())).thenReturn(88003L);
        AtomicReference<GxpAuditCommand> audit = new AtomicReference<>();
        doAnswer(call -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            audit.set(call.getArgument(0));
            return new GxpAuditAppendResult(99003L, 1L, "order-audit-hash", false);
        }).when(gxpAuditService).append(any(GxpAuditCommand.class));
        var result = deviationService.create(701L, req);
        var db = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
        var stored = db.queryForMap("SELECT active_order_id,batch_execution_id,work_order_id,initiator_signature_id,initiator_content_hash FROM mes_pro_edhr_deviation WHERE id=?", result.getId());
        assertEquals(91001L, ((Number)stored.get("active_order_id")).longValue());
        org.junit.jupiter.api.Assertions.assertNull(stored.get("batch_execution_id"));
        assertEquals(81001L, ((Number)stored.get("work_order_id")).longValue());
        assertEquals(88003L, ((Number)stored.get("initiator_signature_id")).longValue());
        assertEquals(result.getInitiatorContentHash(), stored.get("initiator_content_hash"));
        assertEquals(result.getInitiatorContentHash(), audit.get().getSignatureContentHash());
        assertTrue(audit.get().getAfterState().getCanonicalJson().contains("91001"));
        var source = org.mockito.ArgumentCaptor.forClass(MesProEdhrDeviationDO.class);
        verify(workTaskService).assignDeviationTask(source.capture(), eq(MesProEdhrWorkTaskService.TASK_TYPE_DEVIATION_RECEIVE), eq(801L), anyString());
        assertEquals(result.getId(), source.getValue().getId());
        assertEquals(91001L, source.getValue().getActiveOrderId());
        org.junit.jupiter.api.Assertions.assertNull(source.getValue().getBatchExecutionId());
        assertEquals(81001L, source.getValue().getWorkOrderId());
        assertEquals(0L, count("mes_pro_edhr_batch_execution"));
        doThrow(cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(
                MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_DEVIATION_ADMINISTRATOR_REQUIRED))
                .when(administratorService).requireReceiver(801L);
        var revoked = assertThrows(ServiceException.class, () -> deviationService.create(701L, req));
        assertEquals(MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_DEVIATION_ADMINISTRATOR_REQUIRED.getCode(), revoked.getCode());
        assertEquals(1L, count("mes_pro_edhr_deviation"));
        verify(signatureService, times(1)).recordActiveOrderDeviationInitiationSignature(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void productionOrderRejectedSignatureRollsBackDeviationReservationAndSequence() {
        var req = productionRequest();
        doThrow(new IllegalStateException("SIGNATURE_REJECTED"))
                .when(signatureService).recordActiveOrderDeviationInitiationSignature(any(), any(), any(), any(), any(), any(), any());
        var error = assertThrows(IllegalStateException.class, () -> deviationService.create(701L, req));
        assertEquals("SIGNATURE_REJECTED", error.getMessage());
        assertEquals(0L, count("mes_pro_edhr_deviation"));
        assertEquals(0L, count("mes_pro_edhr_deviation_create_request"));
        assertEquals(0L, count("mes_pro_edhr_deviation_sequence"));
        verify(workTaskService, never()).assignDeviationTask(any(), any(), any(), any());
        verify(gxpAuditService, never()).append(any());
    }

    @Test
    void missingInitiationCredentialLeavesNoDeviationRequestOrSequenceWrite() {
        MesProEdhrBatchExecutionDO batch = createBatch();
        MesProEdhrDeviationCreateReqVO request = request(batch.getId()).setSignaturePassword(" ");

        ServiceException error = assertThrows(ServiceException.class, () -> deviationService.create(701L, request));

        assertEquals(PRO_EDHR_DEVIATION_SIGNATURE_REQUIRED.getCode(), error.getCode());
        assertEquals(0L, count("mes_pro_edhr_deviation"));
        assertEquals(0L, count("mes_pro_edhr_deviation_create_request"));
        assertEquals(0L, count("mes_pro_edhr_deviation_sequence"));
        verify(signatureService, never()).recordDeviationInitiationSignature(
                any(), any(), any(), any(), any(), any(), any());
        verify(gxpAuditService, never()).append(any());
    }

    @Test
    void invalidInitiationCredentialRollsBackDeviationRequestAndNumber() {
        MesProEdhrBatchExecutionDO batch = createBatch();
        when(signatureService.recordDeviationInitiationSignature(eq(701L), eq(batch.getId()), anyLong(),
                anyString(), anyString(), eq("bad-password"), anyString()))
                .thenThrow(new IllegalStateException("SIGNATURE_INVALID"));

        assertThrows(IllegalStateException.class,
                () -> deviationService.create(701L, request(batch.getId()).setSignaturePassword("bad-password")));

        assertEquals(0L, count("mes_pro_edhr_deviation"));
        assertEquals(0L, count("mes_pro_edhr_deviation_create_request"));
        assertEquals(0L, count("mes_pro_edhr_deviation_sequence"));
        verify(gxpAuditService, never()).append(any());
    }

    @Test
    void signedCanonicalIntakeAndGxpAuditAppendShareTransactionAndReplayDoesNotResign() {
        MesProEdhrBatchExecutionDO batch = createBatch();
        when(signatureService.recordDeviationInitiationSignature(eq(701L), eq(batch.getId()), anyLong(),
                anyString(), anyString(), anyString(), anyString())).thenReturn(88001L);
        AtomicReference<GxpAuditCommand> auditCommand = new AtomicReference<>();
        doAnswer(invocation -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            assertEquals(1L, count("mes_pro_edhr_deviation"));
            GxpAuditCommand command = invocation.getArgument(0);
            auditCommand.set(command);
            return new GxpAuditAppendResult(99001L, 1L, "event-hash", false);
        }).when(gxpAuditService).append(any(GxpAuditCommand.class));
        MesProEdhrDeviationCreateReqVO first = request(batch.getId()).setSignaturePassword("password-one");

        var created = deviationService.create(701L, first);

        MesProEdhrDeviationDO stored = new org.springframework.jdbc.core.JdbcTemplate(dataSource).queryForObject(
                "SELECT * FROM mes_pro_edhr_deviation WHERE id = ?", (rs, rowNum) ->
                        new MesProEdhrDeviationDO()
                                .setId(rs.getLong("id"))
                                .setDeviationCode(rs.getString("deviation_code"))
                                .setBatchExecutionId(rs.getLong("batch_execution_id"))
                                .setInitiatorSignatureId(rs.getLong("initiator_signature_id"))
                                .setInitiatorContentHash(rs.getString("initiator_content_hash"))
                                .setCreatePayloadHash(rs.getString("create_payload_hash")),
                created.getId());
        assertEquals(88001L, stored.getInitiatorSignatureId());
        assertEquals(deviationService.buildCreatePayloadHash(first), stored.getCreatePayloadHash());
        assertFalse(stored.getCreatePayloadHash().contains("password-one"));
        assertEquals("edhr.deviation.create", auditCommand.get().getOperationId());
        assertEquals("PRESENT", auditCommand.get().getAfterState().getState());
        assertEquals("ABSENT", auditCommand.get().getBeforeState().getState());
        assertEquals("88001", auditCommand.get().getSignatureRecordId());
        assertTrue(auditCommand.get().getReason().contains("PRODUCTION_PROCESS"));
        assertTrue(auditCommand.get().getReason().contains("签名与审计原子性测试偏差内容"));
        assertEquals(auditCommand.get().getAfterState().getObjectVersion(),
                auditCommand.get().getSubjectVersion());
        SignatureCapture signed = captureSignature(batch.getId(), "password-one");
        assertEquals(created.getId(), signed.deviationId());
        assertEquals(signed.contentHash(), stored.getInitiatorContentHash());
        assertEquals(signed.contentHash(), auditCommand.get().getSignatureContentHash());
        assertTrue(auditCommand.get().getAfterState().getCanonicalJson().contains(signed.contentHash()));

        var replay = deviationService.create(701L,
                request(batch.getId()).setSignaturePassword("different-password"));

        assertEquals(created.getId(), replay.getId());
        verify(signatureService, times(1)).recordDeviationInitiationSignature(eq(701L), eq(batch.getId()),
                anyLong(), anyString(), anyString(), anyString(), anyString());
        verify(gxpAuditService, times(1)).append(any(GxpAuditCommand.class));
    }

    @Test
    void missingGxpPolicyAppendFailureRollsBackSignedDeviationAndReservation() {
        MesProEdhrBatchExecutionDO batch = createBatch();
        when(signatureService.recordDeviationInitiationSignature(eq(701L), eq(batch.getId()), anyLong(),
                anyString(), anyString(), anyString(), anyString())).thenReturn(88002L);
        doThrow(new IllegalStateException("GXP_AUDIT_POLICY_NOT_FOUND"))
                .when(gxpAuditService).append(any(GxpAuditCommand.class));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> deviationService.create(701L, request(batch.getId()).setSignaturePassword("valid-password")));

        assertEquals("GXP_AUDIT_POLICY_NOT_FOUND", error.getMessage());
        assertEquals(0L, count("mes_pro_edhr_deviation"));
        assertEquals(0L, count("mes_pro_edhr_deviation_create_request"));
        assertEquals(0L, count("mes_pro_edhr_deviation_sequence"));
        verify(signatureService).recordDeviationInitiationSignature(eq(701L), eq(batch.getId()),
                anyLong(), anyString(), anyString(), anyString(), anyString());
    }

    private SignatureCapture captureSignature(Long batchExecutionId, String credential) {
        org.mockito.ArgumentCaptor<Long> deviationIdCaptor = org.mockito.ArgumentCaptor.forClass(Long.class);
        org.mockito.ArgumentCaptor<String> contentHashCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(signatureService).recordDeviationInitiationSignature(eq(701L), eq(batchExecutionId),
                deviationIdCaptor.capture(), anyString(), anyString(), eq(credential), contentHashCaptor.capture());
        return new SignatureCapture(deviationIdCaptor.getValue(), contentHashCaptor.getValue());
    }

    private record SignatureCapture(Long deviationId, String contentHash) {
    }

    private MesProEdhrBatchExecutionDO createBatch() {
        MesProEdhrBatchExecutionDO batch = MesProEdhrBatchExecutionDO.builder()
                .tenantId(1L)
                .batchExecutionCode("EDHR-SIGN-" + System.nanoTime())
                .workOrderId(81001L).workOrderCode("WO-SIGN").batchCode("LOT-SIGN")
                .routeId(82001L).routeCode("R-SIGN").routeName("签名测试路线")
                .status(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED)
                .taskTotal(0).taskApprovedCount(0).blockedCount(0)
                .build();
        batchExecutionMapper.insert(batch);
        new org.springframework.jdbc.core.JdbcTemplate(dataSource).update(
                "INSERT INTO mes_pro_edhr_batch_execution_origin "
                        + "(tenant_id,batch_execution_id,entry_type,origin_key,active_order_id,"
                        + "source_snapshot_hash,batch_provision_receipt_id,batch_provision_status,"
                        + "source_bundle_hash,idempotency_key,relation_status,captured_at) "
                        + "VALUES (1,?,'ACTIVE_ORDER_COMPLETION',?,91001,'source',1,'SUCCESS','bundle',?,'CURRENT',CURRENT_TIMESTAMP)",
                batch.getId(), "sign-origin-" + batch.getId(), "sign-origin-" + batch.getId());
        return batchExecutionMapper.selectById(batch.getId());
    }

    private static MesProEdhrDeviationCreateReqVO request(Long batchExecutionId) {
        return new MesProEdhrDeviationCreateReqVO()
                .setBatchExecutionId(batchExecutionId).setReceiverId(801L)
                .setLevel("NORMAL")
                .setDiscoveryLocation("工序A")
                .setCategoryCodes(List.of("PRODUCTION_PROCESS"))
                .setDescription("签名与审计原子性测试偏差内容")
                .setEmergencyAction("隔离该批次")
                .setLevelBasis("经评估属于普通偏差")
                .setIdempotencyKey("initiation-sign-test");
    }

    private long count(String table) {
        return new org.springframework.jdbc.core.JdbcTemplate(dataSource)
                .queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }
}
