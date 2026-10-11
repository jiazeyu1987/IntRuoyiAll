package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesIndependentBatchPrerequisiteReceiptDO;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_WORK_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_ENTRY_RECEIPT_EXPIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_INDEPENDENT_RECEIPT_ALREADY_REVOKED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_INDEPENDENT_RECEIPT_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_INDEPENDENT_RECEIPT_IDEMPOTENCY_CONFLICT;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_INDEPENDENT_RECEIPT_SOURCE_CHANGED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_INDEPENDENT_RECEIPT_TENANT_MISMATCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class MesIndependentBatchPrerequisiteReceiptServiceTest {

    private static final String SOURCE_SNAPSHOT_HASH = cn.hutool.crypto.digest.DigestUtil.sha256Hex("snapshot");

    private final InMemoryStore store = new InMemoryStore();
    private final MesProWorkOrderService workOrderService = mock(MesProWorkOrderService.class);
    private final TestClock clock = new TestClock();
    private final MesIndependentBatchPrerequisiteReceiptServiceImpl service =
            new MesIndependentBatchPrerequisiteReceiptServiceImpl(store,
                    clock, "mes-test", "test-secret");

    @BeforeEach
    void injectWorkOrderService() {
        ReflectionTestUtils.setField(service, "workOrderService", workOrderService);
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(1L);
        wire(service, store);
    }

    @org.junit.jupiter.api.AfterEach void clearTenant(){cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear();}
    @Test
    void issueVerifyAndRepeatedVerifyUsePersistedCanonicalEvidence() {
        MesIndependentBatchPrerequisiteReceipt receipt = service.issue(command(), 1L, 9L);
        verify(workOrderService).validateWorkOrderExistsForUpdate(22L);
        assertNotNull(receipt.getReceiptId());
        assertEquals(64, receipt.getPayloadHash().length());
        assertEquals(receipt.getPayloadHash(),
                MesIndependentBatchPrerequisiteReceiptCanonicalizer.sha256(store.byReceipt.get(receipt.getReceiptId()).getCanonicalPayload()));
        assertEquals(receipt.getReceiptId(), service.verify(verifyCommand(receipt), 1L).getReceiptId());
        assertEquals(receipt.getReceiptId(), service.verify(verifyCommand(receipt), 1L).getReceiptId());
    }

    @Test
    void issueVerifySurvivesDatabaseSecondPrecisionRoundTrip() {
        PrecisionNormalizingStore precisionStore = new PrecisionNormalizingStore();
        MesIndependentBatchPrerequisiteReceiptServiceImpl precisionService =
                new MesIndependentBatchPrerequisiteReceiptServiceImpl(precisionStore,
                        Clock.fixed(Instant.parse("2026-08-23T00:00:00.987654321Z"), ZoneOffset.UTC),
                        "mes-test", "test-secret");
        ReflectionTestUtils.setField(precisionService, "workOrderService", workOrderService);

        wire(precisionService,precisionStore);
        MesIndependentBatchPrerequisiteReceipt receipt = precisionService.issue(command(), 1L, 9L);

        assertEquals(receipt.getReceiptId(), precisionService.verify(verifyCommand(receipt), 1L).getReceiptId());
        assertEquals(receipt.getPayloadHash(),
                MesIndependentBatchPrerequisiteReceiptCanonicalizer.sha256(
                        precisionStore.byReceipt.get(receipt.getReceiptId()).getCanonicalPayload()));
    }

    @Test
    void tamperedCanonicalPayloadAndSourceChangeFailFast() {
        MesIndependentBatchPrerequisiteReceipt receipt = service.issue(command(), 1L, 9L);
        store.byReceipt.get(receipt.getReceiptId()).setCanonicalPayload("tampered");
        IllegalStateException hash = assertThrows(IllegalStateException.class, () -> service.verify(verifyCommand(receipt), 1L));
        assertEquals("SOURCE_OWNER_INTEGRITY_FAILED", hash.getMessage());

        MesIndependentBatchPrerequisiteReceipt evidenceReceipt = service.issue(command().setIdempotencyKey("evidence"), 1L, 9L);
        store.byReceipt.get(evidenceReceipt.getReceiptId()).setSourceEvidenceJson("[]");
        IllegalStateException evidence = assertThrows(IllegalStateException.class,
                () -> service.verify(verifyCommand(evidenceReceipt), 1L));
        assertEquals("SOURCE_OWNER_INTEGRITY_FAILED", evidence.getMessage());

        MesIndependentBatchPrerequisiteReceipt second = service.issue(command().setIdempotencyKey("second"), 1L, 9L);
        ServiceException source = assertThrows(ServiceException.class,
                () -> service.verify(verifyCommand(second).setSourceSnapshotHash("changed"), 1L));
        assertEquals(PRO_EDHR_INDEPENDENT_RECEIPT_SOURCE_CHANGED.getCode(), source.getCode());
    }

    @Test
    void missingExplicitRelationStatusIsRejectedDuringIssue() {
        MesIndependentBatchPrerequisiteReceiptIssueCommand issueCommand = command();
        issueCommand.getSourceEvidence().get(0).setRelationStatus(null);

        ServiceException error = assertThrows(ServiceException.class, () -> service.issue(issueCommand, 1L, 9L));
        assertEquals(PRO_EDHR_INDEPENDENT_RECEIPT_INVALID.getCode(), error.getCode());
    }

    @Test
    void deletedWorkOrderCannotReceivePrerequisiteReceipt() {
        doThrow(exception(PRO_WORK_ORDER_NOT_EXISTS)).when(workOrderService).validateWorkOrderExistsForUpdate(22L);

        ServiceException error = assertThrows(ServiceException.class, () -> service.issue(command(), 1L, 9L));

        assertEquals(PRO_WORK_ORDER_NOT_EXISTS.getCode(), error.getCode());
        assertTrue(store.byReceipt.isEmpty());
    }

    @Test
    void expiryRevokeTenantAndIdempotencyAreStable() {
        MesIndependentBatchPrerequisiteReceipt receipt = service.issue(command(), 1L, 9L);
        ServiceException tenant = assertThrows(ServiceException.class, () -> service.verify(verifyCommand(receipt), 2L));
        assertEquals(PRO_EDHR_INDEPENDENT_RECEIPT_TENANT_MISMATCH.getCode(), tenant.getCode());

        assertEquals(receipt.getReceiptId(), service.issue(command(), 1L, 9L).getReceiptId());
        ServiceException conflict = assertThrows(ServiceException.class,
                () -> service.issue(command().setSourceSnapshotHash(cn.hutool.crypto.digest.DigestUtil.sha256Hex("other")), 1L, 9L));
        assertEquals(PRO_EDHR_INDEPENDENT_RECEIPT_IDEMPOTENCY_CONFLICT.getCode(), conflict.getCode());

        service.revoke(new MesIndependentBatchPrerequisiteReceiptRevokeCommand()
                .setReceiptId(receipt.getReceiptId()).setReason("source withdrawn"), 1L, 9L);
        ServiceException revoked = assertThrows(ServiceException.class, () -> service.verify(verifyCommand(receipt), 1L));
        assertEquals(PRO_EDHR_INDEPENDENT_RECEIPT_ALREADY_REVOKED.getCode(), revoked.getCode());
        ServiceException again = assertThrows(ServiceException.class, () -> service.revoke(
                new MesIndependentBatchPrerequisiteReceiptRevokeCommand().setReceiptId(receipt.getReceiptId()).setReason("again"), 1L, 9L));
        assertEquals(PRO_EDHR_INDEPENDENT_RECEIPT_IDEMPOTENCY_CONFLICT.getCode(), again.getCode());
    }

    @Test
    void expiredReceiptIsRejected() {
        MesIndependentBatchPrerequisiteReceipt receipt = service.issue(command(), 1L, 9L);
        // The unchanged, creation-verified row expires naturally; changing it would test tampering.
        clock.now = Instant.parse("2026-08-23T05:00:00Z");
        ServiceException error = assertThrows(ServiceException.class, () -> service.verify(verifyCommand(receipt), 1L));
        assertEquals(PRO_EDHR_BATCH_ENTRY_RECEIPT_EXPIRED.getCode(), error.getCode());
    }

    @Test
    void internalPortReadsPersistedReceiptAndNeverTrustsCallerObject() {
        MesIndependentBatchPrerequisiteReceipt issued = service.issue(command(), 1L, 9L);
        String persistedPayloadHash = issued.getPayloadHash();
        String persistedCanonicalPayload = store.byReceipt.get(issued.getReceiptId()).getCanonicalPayload();
        String persistedReceiptHash = store.byReceipt.get(issued.getReceiptId()).getReceiptHash();
        issued.setPayloadHash("caller-forged").setSignature("caller-forged");

        MesIndependentBatchPrerequisiteReceipt verified = service.getVerifiedByReceiptId(
                1L, issued.getReceiptId(), issued.getEntryType(), SOURCE_SNAPSHOT_HASH);

        assertEquals(persistedPayloadHash, verified.getPayloadHash());
        assertEquals(persistedCanonicalPayload, verified.getCanonicalPayload());
        assertEquals(persistedReceiptHash, verified.getReceiptHash());
        assertEquals(SOURCE_SNAPSHOT_HASH, verified.getSourceSnapshotHash());
        ServiceException tenant = assertThrows(ServiceException.class,
                () -> service.getVerifiedByReceiptId(2L, issued.getReceiptId(), issued.getEntryType(), SOURCE_SNAPSHOT_HASH));
        assertEquals(PRO_EDHR_INDEPENDENT_RECEIPT_TENANT_MISMATCH.getCode(), tenant.getCode());
    }

    private MesIndependentBatchPrerequisiteReceiptIssueCommand command() {
        return new MesIndependentBatchPrerequisiteReceiptIssueCommand()
                .setEntryType("PQC_INDEPENDENT").setWorkOrderId(22L).setWorkOrderCode("WO-22")
                .setRouteId(7L).setRouteVersionId(55L).setRouteVersion("v2").setBatchCode("B-1")
                .setSourceRelationId("relation-1").setSourceRelationVersion("v1")
                .setSourceRelationSnapshotHash(cn.hutool.crypto.digest.DigestUtil.sha256Hex("relation-snapshot")).setSourceObjectType("PQC_APPLICATION")
                .setSourceObjectId("pqc-1").setMaterialSourceType("MATERIAL_ISSUE").setMaterialSourceId("material-1")
                .setSourceContextHash(cn.hutool.crypto.digest.DigestUtil.sha256Hex("context")).setSourceSnapshotHash(SOURCE_SNAPSHOT_HASH)
                .setBusinessReason("independent test").setIdempotencyKey("idempotency-1")
                .setSourceEvidence(java.util.List.of(new MesBatchExecutionSourceEvidence()
                        .setSourceType("PQC").setSourceId("pqc-1").setSourceVersion("v1")
                        .setSourceSnapshotHash(cn.hutool.crypto.digest.DigestUtil.sha256Hex("pqc-snapshot"))
                        .setPayloadHash(cn.hutool.crypto.digest.DigestUtil.sha256Hex("pqc-payload"))
                        .setSignature("pqc-signature").setRelationStatus("BOUND")));
    }

    private MesIndependentBatchPrerequisiteReceiptVerifyCommand verifyCommand(MesIndependentBatchPrerequisiteReceipt receipt) {
        return new MesIndependentBatchPrerequisiteReceiptVerifyCommand().setReceiptId(receipt.getReceiptId())
                .setEntryType(receipt.getEntryType()).setSourceSnapshotHash(receipt.getSourceSnapshotHash());
    }

    /**
     * Unit boundary: audit append and mapper storage are in-memory ports. The real owner writer,
     * binding store and audit/source readers verify their captured bytes and rows. This does not
     * certify the audit append kernel, physical persistence or transaction rollback.
     */
    private static void wire(MesIndependentBatchPrerequisiteReceiptServiceImpl target,InMemoryStore store){
        var audit=org.mockito.Mockito.mock(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService.class);
        var eventMapper=org.mockito.Mockito.mock(cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventMapper.class);
        var relationMapper=org.mockito.Mockito.mock(cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventRelationMapper.class);
        var bindingMapper=org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesEdhrSourceCreationBindingMapper.class);
        var sourceMapper=org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesIndependentBatchPrerequisiteReceiptMapper.class);
        var eventRows=new java.util.LinkedHashMap<Long,cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO>();
        var relations=new java.util.HashMap<Long,java.util.List<cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditRelation>>();
        var bindingRows=new java.util.ArrayList<cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesEdhrSourceCreationBindingDO>();
        org.mockito.Mockito.when(sourceMapper.selectByReceiptId(org.mockito.ArgumentMatchers.anyLong(),org.mockito.ArgumentMatchers.anyString())).thenAnswer(i->store.selectByReceiptId(i.getArgument(0),i.getArgument(1)));
        org.mockito.Mockito.when(bindingMapper.insert(org.mockito.ArgumentMatchers.any(cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesEdhrSourceCreationBindingDO.class))).thenAnswer(i->{var row=(cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesEdhrSourceCreationBindingDO)i.getArgument(0);row.setId(1000L+bindingRows.size());bindingRows.add(row);return 1;});
        org.mockito.Mockito.when(bindingMapper.selectBySourceRow(org.mockito.ArgumentMatchers.anyLong(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyLong())).thenAnswer(i->bindingRows.stream().filter(r->r.getTenantId().equals(i.getArgument(0))&&r.getOwnerProtocol().equals(i.getArgument(1))&&r.getSourceRowId().equals(i.getArgument(2))).toList());
        org.mockito.Mockito.when(bindingMapper.selectByReceiptVersion(org.mockito.ArgumentMatchers.anyLong(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString())).thenAnswer(i->bindingRows.stream().filter(r->r.getTenantId().equals(i.getArgument(0))&&r.getOwnerProtocol().equals(i.getArgument(1))&&r.getSourceReceiptId().equals(i.getArgument(2))&&r.getSourceVersion().equals(i.getArgument(3))).toList());
        org.mockito.Mockito.when(bindingMapper.selectByCreationEvent(org.mockito.ArgumentMatchers.anyLong(),org.mockito.ArgumentMatchers.anyLong())).thenAnswer(i->bindingRows.stream().filter(r->r.getTenantId().equals(i.getArgument(0))&&r.getCreationEventId().equals(i.getArgument(1))).toList());
        org.mockito.Mockito.when(eventMapper.selectByTenantIdAndId(org.mockito.ArgumentMatchers.eq(1L),org.mockito.ArgumentMatchers.anyLong())).thenAnswer(i->eventRows.get(i.getArgument(1)));
        org.mockito.Mockito.when(relationMapper.selectEventIdsByTargets(org.mockito.ArgumentMatchers.eq(1L),org.mockito.ArgumentMatchers.anyList(),org.mockito.ArgumentMatchers.anyString())).thenAnswer(i->relations.entrySet().stream().filter(e->e.getValue().stream().anyMatch(r->((java.util.List<?>)i.getArgument(1)).contains(r.objectType())&&r.objectId().equals(i.getArgument(2)))).map(java.util.Map.Entry::getKey).toList());
        org.mockito.Mockito.when(relationMapper.selectListByTenantIdAndEventId(org.mockito.ArgumentMatchers.eq(1L),org.mockito.ArgumentMatchers.anyLong())).thenAnswer(i->{Long id=i.getArgument(1);var result=new java.util.ArrayList<cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventRelationDO>();for(var r:relations.getOrDefault(id,java.util.List.of())){var row=new cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventRelationDO();row.setId(id*100+result.size()+1);row.setTenantId(1L);row.setEventId(id);row.setRelationType(r.relationType());row.setTargetType(r.objectType());row.setTargetId(r.objectId());row.setTargetVersion(r.objectVersion());row.setTargetHash(r.objectHash());result.add(row);}return result;});
        org.mockito.Mockito.when(audit.append(org.mockito.ArgumentMatchers.any(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand.class))).thenAnswer(i->{
            var c=(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand)i.getArgument(0);
            var e=new cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO();org.springframework.beans.BeanUtils.copyProperties(c,e);
            e.setId(200L+eventRows.size());e.setTenantId(1L);e.setLedgerSequence((long)eventRows.size()+1);e.setDomain("MES");e.setSubjectType("INDEPENDENT_BATCH_PREREQUISITE_RECEIPT");e.setAction(c.getOperationId().endsWith(".issue")?"CREATE":"REVOKE");
            e.setActorId(9L);e.setActorUsername("TEST_ACTOR");e.setActorDisplayName("TEST_ACTOR");e.setServerOccurredAt(LocalDateTime.of(2026,8,23,0,0));e.setPolicyVersion("TEST_POLICY");e.setIdempotencyPayloadHash("a".repeat(64));e.setAlgorithm("SHA-256");e.setEventSchemaVersion(2);e.setCanonicalizationVersion("GXP_CANONICAL_V2");e.setResultStatus("SUCCESS");e.setTransactionId("TEST_AUDIT_PORT");
            e.setAuthenticatedActorJson("{\"actorId\":9,\"tenantId\":1}");e.setPerformedByJson(e.getAuthenticatedActorJson());
            e.setBeforeState(c.getBeforeState().getState());e.setAfterState(c.getAfterState().getState());e.setBeforeObjectVersion(c.getBeforeState().getObjectVersion());e.setAfterObjectVersion(c.getAfterState().getObjectVersion());e.setBeforeStateJson(c.getBeforeState().getCanonicalJson());e.setAfterStateJson(c.getAfterState().getCanonicalJson());
            e.setRelationManifestJson(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(c.getLinks()));e.setEvidenceManifestJson(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(c.getEvidences()));e.setStatePayloadHash(cn.hutool.crypto.digest.DigestUtil.sha256Hex(e.getBeforeStateJson()+"\u001f"+e.getAfterStateJson()));
            var raw=new java.util.TreeMap<String,Object>();var bean=new org.springframework.beans.BeanWrapperImpl(e);for(var field:cn.iocoder.yudao.module.system.service.gxpaudit.GxpCreationAuditVerificationService.ProtectedColumns.class.getRecordComponents())raw.put(field.getName(),field.getName().equals("serverOccurredAt")?e.getServerOccurredAt().toString():bean.getPropertyValue(field.getName()));
            e.setCanonicalEventJson(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(raw));e.setEventHash(cn.hutool.crypto.digest.DigestUtil.sha256Hex(e.getCanonicalEventJson()));eventRows.put(e.getId(),e);relations.put(e.getId(),java.util.List.copyOf(c.getLinks()));return new cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditAppendResult(e.getId(),e.getLedgerSequence(),e.getEventHash(),false);
        });
        var shared=new cn.iocoder.yudao.module.system.service.gxpaudit.GxpCreationAuditVerificationService();org.springframework.test.util.ReflectionTestUtils.setField(shared,"auditEventMapper",eventMapper);org.springframework.test.util.ReflectionTestUtils.setField(shared,"eventRelationMapper",relationMapper);
        var bindings=new MesEdhrSourceCreationBindingStore(bindingMapper);var integrity=new MesEdhrSourceOwnerIntegrity("mes-test","test-secret");
        var writer=new MesEdhrSourceCreationAuditWriter(audit,bindings,integrity,shared);
        org.springframework.test.util.ReflectionTestUtils.setField(writer,"approvalAuthority",org.mockito.Mockito.mock(cn.iocoder.yudao.module.system.api.permission.ApprovalAuthorityApi.class));
        var verifier=new MesEdhrSourceCreationVerificationService(sourceMapper,org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrMaterialGateReceiptMapper.class),org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper.class),integrity,bindings,shared,new MesEdhrSourceCreationProtocolRegistry());
        org.springframework.test.util.ReflectionTestUtils.setField(target,"creationWriter",writer);org.springframework.test.util.ReflectionTestUtils.setField(target,"creationVerifier",verifier);org.springframework.test.util.ReflectionTestUtils.setField(target,"creationBindings",bindings);
    }
    static class InMemoryStore implements MesIndependentBatchPrerequisiteReceiptStore {
        protected final Map<String, MesIndependentBatchPrerequisiteReceiptDO> byReceipt = new HashMap<>();
        @Override public MesIndependentBatchPrerequisiteReceiptDO selectByReceiptId(Long tenantId, String receiptId) {
            MesIndependentBatchPrerequisiteReceiptDO value = byReceipt.get(receiptId);
            return value != null && tenantId.equals(value.getTenantId()) ? value : null;
        }
        @Override public MesIndependentBatchPrerequisiteReceiptDO selectByReceiptIdUnscoped(String receiptId) {
            return byReceipt.get(receiptId);
        }
        @Override public MesIndependentBatchPrerequisiteReceiptDO selectByIdempotencyKey(Long tenantId, String entryType, String key) {
            return byReceipt.values().stream().filter(value -> tenantId.equals(value.getTenantId())
                    && entryType.equals(value.getEntryType()) && key.equals(value.getIdempotencyKey())).findFirst().orElse(null);
        }
        @Override public void insert(MesIndependentBatchPrerequisiteReceiptDO data) { data.setId((long)byReceipt.size()+1); byReceipt.put(data.getReceiptId(), data); }
        @Override public void update(MesIndependentBatchPrerequisiteReceiptDO data) { byReceipt.put(data.getReceiptId(), data); }
    }

    private static final class PrecisionNormalizingStore extends InMemoryStore {
        @Override public void insert(MesIndependentBatchPrerequisiteReceiptDO data) {
            var persisted = normalize(data);
            super.insert(persisted);
            data.setId(persisted.getId());
        }
        @Override public void update(MesIndependentBatchPrerequisiteReceiptDO data) {
            super.update(normalize(data));
        }
        private MesIndependentBatchPrerequisiteReceiptDO normalize(MesIndependentBatchPrerequisiteReceiptDO value) {
            return new MesIndependentBatchPrerequisiteReceiptDO()
                    .setId(value.getId()).setReceiptId(value.getReceiptId()).setTenantId(value.getTenantId())
                    .setEntryType(value.getEntryType()).setWorkOrderId(value.getWorkOrderId())
                    .setWorkOrderCode(value.getWorkOrderCode()).setRouteId(value.getRouteId())
                    .setRouteVersionId(value.getRouteVersionId()).setRouteVersion(value.getRouteVersion())
                    .setBatchCode(value.getBatchCode()).setSourceRelationId(value.getSourceRelationId())
                    .setSourceRelationVersion(value.getSourceRelationVersion())
                    .setSourceRelationSnapshotHash(value.getSourceRelationSnapshotHash())
                    .setSourceObjectType(value.getSourceObjectType()).setSourceObjectId(value.getSourceObjectId())
                    .setMaterialSourceType(value.getMaterialSourceType()).setMaterialSourceId(value.getMaterialSourceId())
                    .setSourceContextHash(value.getSourceContextHash()).setSourceSnapshotHash(value.getSourceSnapshotHash())
                    .setBusinessReason(value.getBusinessReason()).setIssuerSystem(value.getIssuerSystem())
                    .setIssuerUserId(value.getIssuerUserId()).setIssuerUserRole(value.getIssuerUserRole())
                    .setIssuedAt(value.getIssuedAt() == null ? null : value.getIssuedAt().truncatedTo(ChronoUnit.SECONDS))
                    .setExpiresAt(value.getExpiresAt() == null ? null : value.getExpiresAt().truncatedTo(ChronoUnit.SECONDS))
                    .setRevokedAt(value.getRevokedAt()).setRevocationReason(value.getRevocationReason())
                    .setCredentialVersion(value.getCredentialVersion()).setStatus(value.getStatus())
                    .setCanonicalPayload(value.getCanonicalPayload()).setSourceEvidenceJson(value.getSourceEvidenceJson())
                    .setReceiptHash(value.getReceiptHash()).setPayloadHash(value.getPayloadHash())
                    .setSignature(value.getSignature()).setAuditEventId(value.getAuditEventId())
                    .setIdempotencyKey(value.getIdempotencyKey());
        }
    }

    private static final class TestClock extends Clock {
        private Instant now = Instant.parse("2026-08-23T00:00:00Z");
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) {
            return Clock.fixed(now, zone);
        }
        @Override public Instant instant() { return now; }
    }
}
