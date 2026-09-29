package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionPieceDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionPieceDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSignatureResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesPqcProcessInspectionAggregationService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesPqcProcessInspectionAggregationServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesReportAllocationReleaseStateService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderScopeService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.ArrayList;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_NONCONFORMANCE_REVIEW_FROZEN_ACTION_LOCKED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MesProcessPoolPqcInspectionCorrectionServiceTest {

    @org.junit.jupiter.api.AfterEach
    void clearSignatureTenant() {
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
            "missing", "tenant", "actor", "action", "source", "content", "evidence"})
    void rejectsUnboundFormalSignatureBeforeBusinessWrites(String defect) {
        Fixture fixture = new Fixture("BOOLEAN", null, null, null);
        var signatures = new FormalSignatureFixture(fixture, defect);
        var audit = mock(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService.class);
        try (var context = correctionAuditContext(fixture, audit, signatures)) {
            var failure = assertThrows(ServiceException.class, () -> fixture.service.correct(fixture.command("合格")));
            assertEquals(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED.getCode(), failure.getCode());
            verify(fixture.signatureService).recordFieldChangeSignature(any());
            verify(fixture.revisionService, never()).updatePqcInspectionRecord(any());
            verify(fixture.taskMapper, never()).updateById(any(MesPqcInspectionTaskDO.class));
            verify(fixture.pqcRecordMapper, never()).updateById(any(MesProProcessPoolPqcRecordDO.class));
            verify(fixture.pieceDetailMapper, never()).deleteByTaskId(Fixture.TASK_ID);
            verify(fixture.pieceDetailMapper, never()).insertBatch(any());
            verify(fixture.reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
            verifyNoInteractions(fixture.aggregationService);
            verify(audit, never()).append(any());
        }
    }

    /** Real query/adapter verification over a Mapper boundary; not a MySQL persistence test. */
    private static final class FormalSignatureFixture {
        final cn.iocoder.yudao.module.signature.service.ElectronicSignatureQueryServiceImpl query =
                new cn.iocoder.yudao.module.signature.service.ElectronicSignatureQueryServiceImpl();
        cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO record;
        final String defect;
        final cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper mapper =
                mock(cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper.class);
        final cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchRecordSignatureSubjectAdapter adapter =
                new cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchRecordSignatureSubjectAdapter();

        FormalSignatureFixture(Fixture fixture, String defect) {
            this.defect = defect;
            cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(1L);
            ReflectionTestUtils.setField(query, "signatureRecordMapper", mapper);
            ReflectionTestUtils.setField(query, "subjectAdapters", List.of(adapter));
            when(mapper.selectById(any(java.io.Serializable.class))).thenAnswer(call ->
                    record != null && java.util.Objects.equals(record.getId(), call.getArgument(0)) ? record : null);
            org.mockito.Mockito.doAnswer(call -> prepare(call.getArgument(0), Fixture.signature()))
                    .when(fixture.signatureService).recordFieldChangeSignature(any());
        }

        MesProBatchRecordExecutionFieldAuditSignatureResult prepare(
                cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSignatureCommand input,
                MesProBatchRecordExecutionFieldAuditSignatureResult result) {
                String action = "action".equals(defect) ? "APPROVE" : "FIELD_CHANGE";
                String challenge = "source".equals(defect) ? "f".repeat(64) : input.getSignatureChallengeHash();
                String subject = cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchRecordSignatureSubjectAdapter
                        .encodeSubjectId(0L, action, null, null, null, null, null, null, null,
                                null, null, null, null, null, null, null, challenge);
                String version = cn.hutool.crypto.digest.DigestUtil.sha256Hex(subject);
                long actor = "actor".equals(defect) ? result.getActorId() + 1 : result.getActorId();
                var snapshot = adapter.loadAndAuthorize(new cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand(
                        actor, "MES", action, "MES_BATCH_RECORD", subject, version, input.getReasonText()));
                String canonical = JsonUtils.toJsonString(sortSnapshotKeys(JsonUtils.parseTree(snapshot.canonicalContentJson())));
                var definition = adapter.supportedActions().stream().filter(a -> action.equals(a.actionCode())).findFirst().orElseThrow();
                record = cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO.builder()
                        .id(result.getSignatureId()).moduleCode("MES").actionCode(action).subjectType("MES_BATCH_RECORD")
                        .subjectId(subject).subjectVersion(version).actorId(actor).meaningCode(definition.meaningCode())
                        .meaningLabel(definition.meaningLabel()).reason(input.getReasonText()).signedAt(result.getSignedAt())
                        .timeEvidenceId("SERVER_CLOCK:" + result.getSignedAt()).authenticationMethod("SESSION_PLUS_PASSWORD")
                        .canonicalContentJson(canonical).contentHash(cn.hutool.crypto.digest.DigestUtil.sha256Hex(canonical))
                        .algorithm("SHA-256").keyVersion("system-local-v1").policyVersion(definition.policyVersion())
                        .verificationStatus("VALID").build();
                record.setTenantId(1L);
                record.setEvidenceHash(cn.hutool.crypto.digest.DigestUtil.sha256Hex(String.join("|",
                        "1", String.valueOf(actor), "MES", action, "MES_BATCH_RECORD", subject, version,
                        record.getMeaningCode(), record.getMeaningLabel(), record.getReason(), result.getSignedAt().toString(),
                        record.getTimeEvidenceId(), "SESSION_PLUS_PASSWORD", record.getContentHash(),
                        "", "", "", "", "", "SHA-256", "system-local-v1", record.getPolicyVersion(), "VALID")));
                assertEquals("VALID", query.verifyEvidence(result.getSignatureId()).verificationStatus());
                if ("tenant".equals(defect)) record.setTenantId(2L);
                if ("content".equals(defect)) record.setCanonicalContentJson("{\"changed\":true}");
                if ("evidence".equals(defect)) record.setEvidenceHash("0".repeat(64));
                if ("missing".equals(defect)) record = null;
                return result;
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void correctionAppendsOneBusinessFactWithActualBeforeAndSignatureRelations(boolean aggregated) {
        Fixture fixture = new Fixture("BOOLEAN", null, null, null);
        var signatures = new FormalSignatureFixture(fixture, "valid");
        var currentRecord = fixture.pqcRecordMapper.selectByEventId(Fixture.EVENT_ID);
        var currentTask = fixture.taskMapper.selectById(Fixture.TASK_ID);
        var currentEvent = fixture.eventMapper.selectById(Fixture.EVENT_ID);
        var pieces = new AtomicReference<>(fixture.pieceDetailMapper.selectListByTaskId(Fixture.TASK_ID));
        when(fixture.pieceDetailMapper.selectListByTaskId(Fixture.TASK_ID)).thenAnswer(call -> pieces.get());
        when(fixture.pieceDetailMapper.insertBatch(any())).thenAnswer(call -> {
            List<MesPqcInspectionPieceDetailDO> replacement = call.getArgument(0);
            for (int i = 0; i < replacement.size(); i++) replacement.get(i).setId(7200L + i);
            pieces.set(replacement);
            return true;
        });
        when(fixture.taskMapper.updateById(any(MesPqcInspectionTaskDO.class))).thenAnswer(call -> {
            currentTask.setActualInspectionQuantity(call.getArgument(0, MesPqcInspectionTaskDO.class)
                    .getActualInspectionQuantity());
            return 1;
        });
        when(fixture.pqcRecordMapper.updateById(any(MesProProcessPoolPqcRecordDO.class))).thenAnswer(call -> {
            var update = call.getArgument(0, MesProProcessPoolPqcRecordDO.class);
            currentRecord.setRawPayload(update.getRawPayload()).setInspectionResult(update.getInspectionResult());
            return 1;
        });
        when(fixture.revisionService.updatePqcInspectionRecord(any())).thenAnswer(call -> {
            currentEvent.setRawPayload(call.getArgument(0, MesProcessPoolEventRevisionUpdateReqBO.class).getAfterPayload());
            return 701L;
        });
        if (aggregated) {
            fixture.setupConfirmedReview();
            currentRecord.setProcessInspectionAggregationStatus("AGGREGATED").setProcessInspectionReviewId(7000L);
            var aggregate = new MesPqcProcessInspectionAggregateDetailDO();
            aggregate.setId(8100L);
            aggregate.setEventId(Fixture.EVENT_ID);
            aggregate.setReviewId(7000L);
            aggregate.setPqcTaskId(Fixture.TASK_ID);
            aggregate.setActiveOrderId(5001L);
            aggregate.setMeasuredValue("原值");
            aggregate.setSourcePqcRecordId(currentRecord.getId());
            aggregate.setSourcePieceDetailId(pieces.get().get(0).getId());
            when(fixture.auditAggregateMapper.selectListByActiveOrderIdForUpdate(5001L))
                    .thenReturn(List.of(aggregate));
            org.mockito.Mockito.doAnswer(call -> {
                currentRecord.setProcessInspectionReviewId(7001L);
                aggregate.setReviewId(7001L);
                aggregate.setMeasuredValue("合格");
                aggregate.setSourcePieceDetailId(pieces.get().get(0).getId());
                return null;
            }).when(fixture.aggregationService).refreshCorrectedPqcSubmission(176L, 7000L, 7001L);
        }
        var audit = mock(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService.class);
        try (var context = correctionAuditContext(fixture, audit, signatures)) {
            assertEquals(701L, fixture.service.correct(fixture.command("合格")));
            var captured = ArgumentCaptor.forClass(
                    cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand.class);
            verify(audit).append(captured.capture());
            var command = captured.getValue();
            assertEquals("mes.pqc-inspection.correct", command.getOperationId());
            assertEquals("USER", command.getReasonSource());
            assertEquals("纠正检验值", command.getReason());
            assertEquals("9102", command.getSignatureRecordId());
            var signed = ArgumentCaptor.forClass(cn.iocoder.yudao.module.mes.service.pro.batchrecord
                    .MesProBatchRecordExecutionFieldAuditSignatureCommand.class);
            verify(fixture.signatureService).recordFieldChangeSignature(signed.capture());
            var challenge = command.getEvidences().stream()
                    .filter(e -> "SIGNATURE_CHALLENGE".equals(e.evidenceType())).findFirst().orElseThrow();
            assertEquals(signed.getValue().getSignatureChallengeHash(), challenge.sha256());
            assertEquals("9102", challenge.sourceId());
            var signatureEvidence = command.getEvidences().stream()
                    .filter(e -> "SIGNATURE".equals(e.evidenceType())).findFirst().orElseThrow();
            assertEquals(signatures.record.getContentHash(), signatureEvidence.sha256());
            assertEquals(signatures.record.getContentHash(), command.getSignatureContentHash());
            assertEquals("PRESENT", command.getBeforeState().getState());
            assertEquals("PRESENT", command.getAfterState().getState());
            assertContentVersion(command.getBeforeState().getCanonicalJson(),
                    command.getBeforeState().getObjectVersion());
            assertContentVersion(command.getAfterState().getCanonicalJson(),
                    command.getAfterState().getObjectVersion());
            var before = JsonUtils.parseTree(command.getBeforeState().getCanonicalJson());
            var after = JsonUtils.parseTree(command.getAfterState().getCanonicalJson());
            assertEquals(176L, before.path("eventId").asLong());
            assertEquals(5101L, before.path("taskId").asLong());
            assertEquals(5001L, before.path("activeOrderId").asLong());
            assertEquals(1001L, before.path("workOrderId").asLong());
            assertEquals("原值", before.path("pieces").get(0).path("measuredValue").asText());
            assertEquals("合格", after.path("pieces").get(0).path("measuredValue").asText());
            assertEquals(aggregated ? 7000L : 0L, before.path("currentReviewId").asLong());
            assertEquals(aggregated ? 7001L : 0L, after.path("currentReviewId").asLong());
            assertEquals(currentRecord.getInspectionResult(), after.path("inspectionResult").asText());
            assertEquals(currentTask.getActualInspectionQuantity().intValue(),
                    after.path("actualInspectionQuantity").asInt());
            assertEquals(command.getAfterState().getObjectVersion(), command.getSubjectVersion());
            assertTrue(command.getSubjectVersion() != null && !command.getSubjectVersion().isBlank());
            assertTrue(command.getLinks().stream().anyMatch(link ->
                    "REVISION".equals(link.objectType()) && "701".equals(link.objectId())));
            assertTrue(command.getLinks().stream().anyMatch(link ->
                    "SIGNATURE".equals(link.objectType()) && "9102".equals(link.objectId())));
            if (aggregated) {
                assertEquals(1, before.path("aggregateRows").size());
                assertEquals(1, after.path("aggregateRows").size());
                assertEquals("原值", before.path("aggregateRows").get(0).path("measuredValue").asText());
                assertEquals("合格", after.path("aggregateRows").get(0).path("measuredValue").asText());
                assertEquals(7000L, before.path("aggregateRows").get(0).path("reviewId").asLong());
                assertEquals(7001L, after.path("aggregateRows").get(0).path("reviewId").asLong());
                for (var state : List.of(before, after)) {
                    var row = state.path("aggregateRows").get(0);
                    assertEquals(Fixture.EVENT_ID, row.path("eventId").asLong());
                    assertEquals(Fixture.TASK_ID, row.path("pqcTaskId").asLong());
                    assertEquals(currentRecord.getId().longValue(), row.path("sourcePqcRecordId").asLong());
                    assertEquals(state.path("pieces").get(0).path("id").asLong(), row.path("sourcePieceDetailId").asLong());
                }
                verify(fixture.aggregationService).refreshCorrectedPqcSubmission(176L, 7000L, 7001L);
                assertTrue(command.getLinks().stream().anyMatch(link -> "7000".equals(link.objectId())
                        && "PQC_REVIEW".equals(link.objectType())));
                assertTrue(command.getLinks().stream().anyMatch(link -> "7001".equals(link.objectId())
                        && "PQC_REVIEW".equals(link.objectType())));
            } else {
                verify(fixture.reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
                verifyNoInteractions(fixture.aggregationService);
                assertEquals(0, after.path("aggregateRows").size());
                assertTrue(before.has("review") && before.get("review").isNull());
                assertTrue(after.has("review") && after.get("review").isNull());
                assertTrue(before.has("currentReviewId") && before.get("currentReviewId").isNull());
                assertTrue(after.has("currentReviewId") && after.get("currentReviewId").isNull());
                assertTrue(after.get("aggregateRows").isArray());
            }
            assertTrue(!command.getBeforeState().getCanonicalJson().contains("valid-password"));
            assertTrue(!command.getAfterState().getCanonicalJson().contains("valid-password"));
            var ordered = org.mockito.Mockito.inOrder(audit, fixture.activeOrderMapper);
            ordered.verify(audit).acquireLedgerLock();
            ordered.verify(fixture.activeOrderMapper).selectByIdForUpdate(5001L);
        }
    }

    /** Actual JDBC rollback at collaborator boundaries, not a production-schema/MySQL whole-chain test. */
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void auditFailureRollsBackActualCorrectionBoundaryWrites(boolean aggregated) {
        Fixture fixture = new Fixture("BOOLEAN", null, null, null);
        if (aggregated) {
            fixture.setupConfirmedReview();
            fixture.pqcRecordMapper.selectByEventId(Fixture.EVENT_ID)
                    .setProcessInspectionAggregationStatus("AGGREGATED").setProcessInspectionReviewId(7000L);
        }
        var dataSource = new org.springframework.jdbc.datasource.DriverManagerDataSource(
                "jdbc:h2:mem:correction_audit_" + java.util.UUID.randomUUID(), "sa", "");
        // Hold the database alive only for this test; no external DB or service is started.
        try (var connection = dataSource.getConnection()) {
            var jdbc = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
            jdbc.execute("CREATE TABLE correction_boundary_fact (kind VARCHAR(40) PRIMARY KEY, fact VARCHAR(200))");
            for (String kind : List.of("event", "task", "record", "piece", "aggregate", "review")) {
                jdbc.update("INSERT INTO correction_boundary_fact VALUES (?, ?)", kind, "before");
            }
            org.mockito.Mockito.doAnswer(call -> {
                jdbc.update("INSERT INTO correction_boundary_fact VALUES ('signature', '9102')");
                return fixture.formalSignatures.prepare(call.getArgument(0), Fixture.signature());
            }).when(fixture.signatureService).recordFieldChangeSignature(any());
            when(fixture.revisionService.updatePqcInspectionRecord(any())).thenAnswer(call -> {
                jdbc.update("INSERT INTO correction_boundary_fact VALUES ('revision', '701')");
                jdbc.update("INSERT INTO correction_boundary_fact VALUES ('diff', 'changed')");
                jdbc.update("UPDATE correction_boundary_fact SET fact='after' WHERE kind='event'");
                return 701L;
            });
            when(fixture.taskMapper.updateById(any(MesPqcInspectionTaskDO.class))).thenAnswer(call ->
                    jdbc.update("UPDATE correction_boundary_fact SET fact='after' WHERE kind='task'"));
            org.mockito.Mockito.doAnswer(call -> jdbc.update(
                    "DELETE FROM correction_boundary_fact WHERE kind='piece'"))
                    .when(fixture.pieceDetailMapper).deleteByTaskId(Fixture.TASK_ID);
            when(fixture.pieceDetailMapper.insertBatch(any())).thenAnswer(call -> {
                jdbc.update("INSERT INTO correction_boundary_fact VALUES ('piece', 'after')");
                return true;
            });
            when(fixture.pqcRecordMapper.updateById(any(MesProProcessPoolPqcRecordDO.class))).thenAnswer(call ->
                    jdbc.update("UPDATE correction_boundary_fact SET fact='after' WHERE kind='record'"));
            if (aggregated) {
                when(fixture.reviewMapper.insert(any(MesProcessPoolSubmissionReviewDO.class))).thenAnswer(call -> {
                    call.getArgument(0, MesProcessPoolSubmissionReviewDO.class).setId(7001L);
                    jdbc.update("INSERT INTO correction_boundary_fact VALUES ('newReview', '7001')");
                    return 1;
                });
                org.mockito.Mockito.doAnswer(call -> {
                    jdbc.update("UPDATE correction_boundary_fact SET fact='after' WHERE kind='aggregate'");
                    return null;
                }).when(fixture.aggregationService).refreshCorrectedPqcSubmission(176L, 7000L, 7001L);
            }
            var audit = mock(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService.class);
            var failure = new ServiceException(1999999011, "correction audit injected failure");
            AtomicBoolean reachedAfterWrites = new AtomicBoolean();
            when(audit.append(any())).thenAnswer(call -> {
                for (String kind : List.of("event", "task", "record", "piece")) {
                    assertEquals("after", jdbc.queryForObject(
                            "SELECT fact FROM correction_boundary_fact WHERE kind=?", String.class, kind), kind);
                }
                assertEquals("9102", jdbc.queryForObject("SELECT fact FROM correction_boundary_fact WHERE kind='signature'", String.class));
                assertEquals("changed", jdbc.queryForObject("SELECT fact FROM correction_boundary_fact WHERE kind='diff'", String.class));
                assertEquals(aggregated ? 1 : 0, jdbc.queryForObject(
                        "SELECT COUNT(*) FROM correction_boundary_fact WHERE kind='newReview' AND fact='7001'", Integer.class));
                assertEquals("after", jdbc.queryForObject(
                        "SELECT fact FROM correction_boundary_fact WHERE kind='piece'", String.class));
                assertEquals("701", jdbc.queryForObject(
                        "SELECT fact FROM correction_boundary_fact WHERE kind='revision'", String.class));
                assertEquals(aggregated ? "after" : "before", jdbc.queryForObject(
                        "SELECT fact FROM correction_boundary_fact WHERE kind='aggregate'", String.class));
                reachedAfterWrites.set(true);
                throw failure;
            });
            try (var context = correctionAuditContext(fixture, audit)) {
                var factory = new org.springframework.aop.framework.ProxyFactory(fixture.service);
                factory.setProxyTargetClass(true);
                factory.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(
                        new org.springframework.jdbc.datasource.DataSourceTransactionManager(dataSource),
                        new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));
                var transactional = (MesProcessPoolPqcInspectionCorrectionService) factory.getProxy();
                var actual = assertThrows(ServiceException.class,
                        () -> transactional.correct(fixture.command("合格")));
                assertEquals(failure.getCode(), actual.getCode());
                assertTrue(reachedAfterWrites.get(), "Failure must occur after actual JDBC mutations");
                assertTrue(!org.springframework.transaction.support.TransactionSynchronizationManager
                        .isActualTransactionActive());
                assertEquals(6, jdbc.queryForObject("SELECT COUNT(*) FROM correction_boundary_fact", Integer.class));
                assertEquals(6, jdbc.queryForObject(
                        "SELECT COUNT(*) FROM correction_boundary_fact WHERE fact='before'", Integer.class));
            }
        } catch (java.sql.SQLException exception) {
            throw new AssertionError("H2 correction rollback fixture could not connect", exception);
        }
    }

    private static org.springframework.context.annotation.AnnotationConfigApplicationContext correctionAuditContext(
            Fixture fixture, cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService audit) {
        return correctionAuditContext(fixture, audit, fixture.formalSignatures);
    }

    private static org.springframework.context.annotation.AnnotationConfigApplicationContext correctionAuditContext(
            Fixture fixture, cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService audit,
            FormalSignatureFixture signatures) {
        var context = new org.springframework.context.annotation.AnnotationConfigApplicationContext();
        context.registerBean("gxpAuditService",
                cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService.class, () -> audit);
        context.registerBean("electronicSignatureQueryService",
                cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService.class, () -> signatures.query);
        context.registerBean("signatureRecordMapper",
                cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper.class, () -> signatures.mapper);
        context.registerBean("mesBatchRecordSignatureSubjectAdapter",
                cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchRecordSignatureSubjectAdapter.class,
                () -> signatures.adapter);
        context.registerBean("activeOrderMapper", MesProcessPoolActiveOrderMapper.class, () -> fixture.activeOrderMapper);
        context.registerBean("reviewMapper", MesProcessPoolSubmissionReviewMapper.class, () -> fixture.reviewMapper);
        context.registerBean("aggregateDetailMapper", MesPqcProcessInspectionAggregateDetailMapper.class,
                () -> fixture.auditAggregateMapper);
        context.refresh();
        // Normal @Resource injection, including future production audit dependency; no reflective field-exists skip.
        context.getAutowireCapableBeanFactory().autowireBean(fixture.service);
        return context;
    }

    private static void assertContentVersion(String canonicalJson, String objectVersion) {
        assertTrue(objectVersion != null && objectVersion.matches("sha256:[0-9a-f]{64}"),
                "Content versions must use the operation-contract sha256: prefix");
        String normalized = JsonUtils.toJsonString(sortSnapshotKeys(JsonUtils.parseTree(canonicalJson)));
        assertEquals(normalized, canonicalJson, "Snapshot object keys must be recursively canonicalized");
        assertEquals("sha256:" + cn.hutool.crypto.digest.DigestUtil.sha256Hex(normalized), objectVersion);
    }

    private static com.fasterxml.jackson.databind.JsonNode sortSnapshotKeys(
            com.fasterxml.jackson.databind.JsonNode node) {
        if (node.isObject()) {
            var sorted = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
            var keys = new java.util.TreeSet<String>();
            node.fieldNames().forEachRemaining(keys::add);
            for (String key : keys) sorted.set(key, sortSnapshotKeys(node.get(key)));
            return sorted;
        }
        if (node.isArray()) {
            var array = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode();
            node.forEach(value -> array.add(sortSnapshotKeys(value)));
            return array;
        }
        return node;
    }

    @Test
    void approvalCommittedAfterIdentityReadStillRefreshesCorrectionAggregation() {
        Fixture fixture = new Fixture("BOOLEAN", null, null, null);
        MesProProcessPoolEventDO event = fixture.eventMapper.selectByIdForUpdate(Fixture.EVENT_ID);
        event.setQaProcessId(6001L);
        MesPqcInspectionTaskDO task = fixture.taskMapper.selectByIdForUpdate(Fixture.TASK_ID);
        task.setQaProcessId(6001L).setRouteVersionId(2101L).setRegulationVersionId(6101L)
                .setInspectionType("PROCESS").setBusinessDate(java.time.LocalDate.of(2026, 9, 28))
                .setShiftCode("DAY").setRoundNo(1);
        MesProProcessPoolPqcRecordDO snapshotRecord = Fixture.record()
                .setWorkOrderId(1001L).setRouteId(2001L).setQaProcessId(6001L)
                .setProcessInspectionAggregationStatus("PENDING");
        MesProProcessPoolPqcRecordDO committedRecord = Fixture.record()
                .setWorkOrderId(1001L).setRouteId(2001L).setQaProcessId(6001L)
                .setProcessInspectionAggregationStatus("PENDING");
        AtomicBoolean identityRead = new AtomicBoolean();
        AtomicBoolean approvalCommitted = new AtomicBoolean();
        AtomicBoolean ownRecordWrite = new AtomicBoolean();
        when(fixture.eventMapper.selectById(Fixture.EVENT_ID)).thenAnswer(invocation -> {
            identityRead.set(true);
            return event;
        });
        when(fixture.activeOrderMapper.selectByIdForUpdate(5001L)).thenAnswer(invocation -> {
            assertTrue(identityRead.get(), "The identity read establishes the older transaction snapshot");
            // Another transaction commits its first approval before correction obtains its write locks.
            task.setTaskStatus("CONFIRMED");
            committedRecord.setProcessInspectionAggregationStatus("AGGREGATED")
                    .setProcessInspectionReviewId(7000L);
            approvalCommitted.set(true);
            return fixture.activeOrder;
        });
        initializeMapperMetadata(MesProProcessPoolPqcRecordDO.class);
        MesProProcessPoolPqcRecordMapper recordMapper = mock(MesProProcessPoolPqcRecordMapper.class,
                org.mockito.Mockito.CALLS_REAL_METHODS);
        org.mockito.Mockito.doAnswer(invocation -> {
            assertTrue(approvalCommitted.get());
            Wrapper<MesProProcessPoolPqcRecordDO> query = invocation.getArgument(0);
            return query.getSqlSegment().contains("FOR UPDATE") || ownRecordWrite.get()
                    ? committedRecord : snapshotRecord;
        }).when(recordMapper).selectOne(org.mockito.ArgumentMatchers.<Wrapper<MesProProcessPoolPqcRecordDO>>any());
        org.mockito.Mockito.doAnswer(invocation -> {
            ownRecordWrite.set(true);
            return 1;
        }).when(recordMapper).updateById(any(MesProProcessPoolPqcRecordDO.class));
        org.mockito.Mockito.doAnswer(invocation -> {
            assertEquals(committedRecord.getProcessInspectionReviewId(), invocation.getArgument(2));
            committedRecord.setProcessInspectionReviewId(invocation.getArgument(3));
            return 1;
        }).when(recordMapper).replaceProcessInspectionReviewIfAggregated(any(), any(), any(), any(), any());
        ReflectionTestUtils.setField(fixture.service, "pqcRecordMapper", recordMapper);
        List<MesProcessPoolSubmissionReviewDO> reviews = fixture.setupConfirmedReview();
        AtomicReference<List<MesPqcInspectionPieceDetailDO>> currentDetails = new AtomicReference<>(
                fixture.pieceDetailMapper.selectListByTaskId(Fixture.TASK_ID));
        when(fixture.pieceDetailMapper.selectListByTaskId(Fixture.TASK_ID))
                .thenAnswer(invocation -> currentDetails.get());
        when(fixture.pieceDetailMapper.insertBatch(any())).thenAnswer(invocation -> {
            List<MesPqcInspectionPieceDetailDO> rows = invocation.getArgument(0);
            rows.get(0).setId(7200L);
            currentDetails.set(rows);
            return true;
        });
        when(fixture.revisionService.updatePqcInspectionRecord(any())).thenAnswer(invocation -> {
            event.setRawPayload(invocation.getArgument(0, MesProcessPoolEventRevisionUpdateReqBO.class).getAfterPayload());
            return 701L;
        });
        MesPqcProcessInspectionAggregateDetailMapper aggregateMapper = mock(MesPqcProcessInspectionAggregateDetailMapper.class);
        when(aggregateMapper.deleteByEventId(Fixture.EVENT_ID)).thenReturn(1);
        when(aggregateMapper.insertBatch(any())).thenReturn(true);
        ReflectionTestUtils.setField(fixture.service, "aggregationService",
                new MesPqcProcessInspectionAggregationServiceImpl(recordMapper, fixture.eventMapper,
                        fixture.taskMapper, fixture.pieceDetailMapper, aggregateMapper, fixture.reviewMapper));

        assertEquals(701L, fixture.service.correct(fixture.command("合格")));

        assertEquals(2, reviews.size(), "A committed approval must gain a signed correction review");
        assertEquals(7000L, reviews.get(0).getId(), "The previous review remains as evidence");
        assertEquals(7001L, committedRecord.getProcessInspectionReviewId());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MesPqcProcessInspectionAggregateDetailDO>> rows = ArgumentCaptor.forClass(List.class);
        verify(aggregateMapper).insertBatch(rows.capture());
        assertEquals("合格", rows.getValue().get(0).getMeasuredValue());
        assertEquals(7200L, rows.getValue().get(0).getSourcePieceDetailId());
        assertEquals(7001L, rows.getValue().get(0).getReviewId());
        assertEquals("PENDING", snapshotRecord.getProcessInspectionAggregationStatus());
    }

    @Test
    void priorCorrectionCommittedAfterIdentityReadKeepsCurrentPieceEvidenceAndEquipment() {
        Fixture fixture = new Fixture("BOOLEAN", null, null, null);
        MesPqcInspectionPieceDetailDO snapshotPiece = Fixture.existingDetail("BOOLEAN", null, null, null)
                .setSelectedEquipmentId(8100L).setSelectedEquipmentNumber("OLD-EQUIPMENT");
        MesPqcInspectionPieceDetailDO committedPiece = Fixture.existingDetail("BOOLEAN", null, null, null)
                .setId(7150L).setMeasuredValue("不合格").setItemResult("不合格").setJudgement("FAILURE")
                .setSelectedEquipmentId(8200L).setSelectedEquipmentNumber("CURRENT-EQUIPMENT");
        AtomicBoolean identityRead = new AtomicBoolean();
        AtomicBoolean previousCorrectionCommitted = new AtomicBoolean();
        MesProProcessPoolEventDO event = fixture.eventMapper.selectByIdForUpdate(Fixture.EVENT_ID);
        when(fixture.eventMapper.selectById(Fixture.EVENT_ID)).thenAnswer(invocation -> {
            identityRead.set(true);
            return event;
        });
        when(fixture.activeOrderMapper.selectByIdForUpdate(5001L)).thenAnswer(invocation -> {
            assertTrue(identityRead.get());
            event.setRawPayload("{\"inspectionResult\":\"FAILURE\",\"scrapQuantity\":1}");
            previousCorrectionCommitted.set(true);
            return fixture.activeOrder;
        });
        initializeMapperMetadata(MesPqcInspectionPieceDetailDO.class);
        MesPqcInspectionPieceDetailMapper pieceMapper = mock(MesPqcInspectionPieceDetailMapper.class,
                org.mockito.Mockito.CALLS_REAL_METHODS);
        org.mockito.Mockito.doAnswer(invocation -> {
            assertTrue(previousCorrectionCommitted.get());
            Wrapper<MesPqcInspectionPieceDetailDO> query = invocation.getArgument(0);
            return List.of(query.getSqlSegment().contains("FOR UPDATE") ? committedPiece : snapshotPiece);
        }).when(pieceMapper).selectList(org.mockito.ArgumentMatchers.<Wrapper<MesPqcInspectionPieceDetailDO>>any());
        org.mockito.Mockito.doReturn(1).when(pieceMapper).deleteByTaskId(Fixture.TASK_ID);
        org.mockito.Mockito.doReturn(true).when(pieceMapper).insertBatch(any());
        ReflectionTestUtils.setField(fixture.service, "pieceDetailMapper", pieceMapper);

        assertEquals(701L, fixture.service.correct(fixture.command("合格")));

        ArgumentCaptor<MesProcessPoolEventRevisionUpdateReqBO> revision =
                ArgumentCaptor.forClass(MesProcessPoolEventRevisionUpdateReqBO.class);
        verify(fixture.revisionService).updatePqcInspectionRecord(revision.capture());
        MesProcessPoolEventRevisionFieldChangeBO sampleChange = revision.getValue().getChangedFields().stream()
                .filter(change -> "PQC.ITEM.QA-001".equals(change.getFieldCode())).findFirst().orElseThrow();
        assertEquals("不合格", sampleChange.getBeforeValue(), "Audit evidence must describe the immediately preceding revision");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MesPqcInspectionPieceDetailDO>> rows = ArgumentCaptor.forClass(List.class);
        verify(pieceMapper).insertBatch(rows.capture());
        assertEquals(8200L, rows.getValue().get(0).getSelectedEquipmentId());
        assertEquals("CURRENT-EQUIPMENT", rows.getValue().get(0).getSelectedEquipmentNumber());
        assertEquals("原值", snapshotPiece.getMeasuredValue());
        assertEquals("不合格", committedPiece.getMeasuredValue());
    }

    private static void initializeMapperMetadata(Class<?> entityClass) {
        Configuration configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, "pqc-correction-snapshot-test"),
                entityClass);
    }

    @Test
    void correctionRefreshExcludesHistoryVisibleOnlyToOldSnapshot() {
        Fixture fixture = new Fixture("BOOLEAN", null, null, null);
        MesProProcessPoolEventDO event = fixture.eventMapper.selectById(Fixture.EVENT_ID);
        event.setQaProcessId(6001L);
        MesPqcInspectionTaskDO task = fixture.taskMapper.selectById(Fixture.TASK_ID);
        task.setTaskStatus("CONFIRMED").setQaProcessId(6001L).setRouteVersionId(2101L)
                .setRegulationVersionId(6101L).setInspectionType("PROCESS")
                .setBusinessDate(java.time.LocalDate.of(2026, 9, 28)).setShiftCode("DAY").setRoundNo(1);
        fixture.pqcRecordMapper.selectByEventId(Fixture.EVENT_ID)
                .setWorkOrderId(1001L).setRouteId(2001L).setQaProcessId(6001L)
                .setProcessInspectionAggregationStatus("AGGREGATED").setProcessInspectionReviewId(7000L);
        String payloadHash = cn.iocoder.yudao.module.mes.service.pro.batchrecord
                .MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(event.getRawPayload());
        MesProcessPoolSubmissionReviewDO correctionReview = MesProcessPoolSubmissionReviewDO.builder()
                .id(7001L).eventId(Fixture.EVENT_ID).leaderType("PQC").leaderUserId(Fixture.ACTOR_ID)
                .reviewStatus("APPROVED").reviewSignatureId(9102L).reviewSignatureUserId(Fixture.ACTOR_ID)
                .reviewSignatureSnapshotJson("{\"actionType\":\"PQC_INSPECTION_CORRECTION\","
                        + "\"supersededReviewId\":7000,\"revisionId\":701,\"signatureId\":9102,"
                        + "\"payloadHash\":\"" + payloadHash + "\"}").build();
        correctionReview.setTenantId(1L);
        when(fixture.reviewMapper.selectLatestByEventIdForUpdate(Fixture.EVENT_ID)).thenReturn(correctionReview);
        MesPqcInspectionPieceDetailDO snapshotHistory = Fixture.existingDetail("BOOLEAN", null, null, null);
        MesPqcInspectionPieceDetailDO ownCurrentPiece = Fixture.existingDetail("BOOLEAN", null, null, null)
                .setId(7200L).setMeasuredValue("合格").setItemResult("合格");
        initializeMapperMetadata(MesPqcInspectionPieceDetailDO.class);
        MesPqcInspectionPieceDetailMapper pieceMapper = mock(MesPqcInspectionPieceDetailMapper.class,
                org.mockito.Mockito.CALLS_REAL_METHODS);
        org.mockito.Mockito.doAnswer(invocation -> {
            Wrapper<MesPqcInspectionPieceDetailDO> query = invocation.getArgument(0);
            // A prior transaction deleted history; this transaction deleted its successor and inserted current.
            // A consistent snapshot can still see history together with our own newly inserted row.
            return query.getSqlSegment().contains("FOR UPDATE")
                    ? List.of(ownCurrentPiece) : List.of(snapshotHistory, ownCurrentPiece);
        }).when(pieceMapper).selectList(org.mockito.ArgumentMatchers.<Wrapper<MesPqcInspectionPieceDetailDO>>any());
        when(fixture.pqcRecordMapper.replaceProcessInspectionReviewIfAggregated(any(), any(), any(), any(), any()))
                .thenReturn(1);
        MesPqcProcessInspectionAggregateDetailMapper aggregateMapper = mock(MesPqcProcessInspectionAggregateDetailMapper.class);
        when(aggregateMapper.deleteByEventId(Fixture.EVENT_ID)).thenReturn(1);
        when(aggregateMapper.insertBatch(any())).thenReturn(true);
        MesPqcProcessInspectionAggregationService realAggregation = new MesPqcProcessInspectionAggregationServiceImpl(
                fixture.pqcRecordMapper, fixture.eventMapper, fixture.taskMapper, pieceMapper, aggregateMapper,
                fixture.reviewMapper);

        realAggregation.refreshCorrectedPqcSubmission(Fixture.EVENT_ID, 7000L, 7001L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MesPqcProcessInspectionAggregateDetailDO>> rows = ArgumentCaptor.forClass(List.class);
        verify(aggregateMapper).insertBatch(rows.capture());
        assertEquals(1, rows.getValue().size(), "Current aggregation must exclude history visible to an older snapshot");
        assertEquals(7200L, rows.getValue().get(0).getSourcePieceDetailId());
        assertEquals("合格", rows.getValue().get(0).getMeasuredValue());
    }

    @Test
    void concurrentCompletionCanFinishBeforeCorrectionWithoutReverseLockWait() throws Exception {
        Fixture fixture = new Fixture("BOOLEAN", null, null, null);
        var orderLock = new java.util.concurrent.locks.ReentrantLock();
        var workLock = new java.util.concurrent.locks.ReentrantLock();
        var firstLockAttempt = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        when(fixture.activeOrderMapper.selectByIdForUpdate(5001L)).thenAnswer(invocation -> {
            firstLockAttempt.countDown();
            orderLock.lock();
            return fixture.activeOrder;
        });
        org.mockito.Mockito.doAnswer(invocation -> {
            workLock.lock();
            firstLockAttempt.countDown();
            return null;
        }).when(fixture.nonconformanceReviewService).ensureWorkOrderNotFrozen(1001L, "PQC检验更正");
        orderLock.lock();
        try {
            var correction = executor.submit(() -> {
                try {
                    return assertThrows(ServiceException.class,
                            () -> fixture.service.correct(fixture.command("合格")));
                } finally {
                    if (workLock.isHeldByCurrentThread()) workLock.unlock();
                    if (orderLock.isHeldByCurrentThread()) orderLock.unlock();
                }
            });
            assertTrue(firstLockAttempt.await(5, java.util.concurrent.TimeUnit.SECONDS));
            boolean completionCanLockWorkOrder = workLock.tryLock(300, java.util.concurrent.TimeUnit.MILLISECONDS);
            if (completionCanLockWorkOrder) workLock.unlock();
            fixture.activeOrder.setBusinessStatus("COMPLETED");
            orderLock.unlock();
            correction.get(5, java.util.concurrent.TimeUnit.SECONDS);
            assertTrue(completionCanLockWorkOrder, "Correction must not hold the work order while waiting for completion's active order");
            verifyNoInteractions(fixture.signatureService, fixture.revisionService, fixture.reviewMapper);
        } finally {
            if (orderLock.isHeldByCurrentThread()) orderLock.unlock();
            executor.shutdownNow();
        }
    }

    @Test
    void correctConfirmedPqcUsingRealAggregationReplacesItsFormalDetails() {
        Fixture fixture = new Fixture("BOOLEAN", null, null, null);
        MesProProcessPoolEventDO event = fixture.eventMapper.selectByIdForUpdate(Fixture.EVENT_ID);
        event.setQaProcessId(6001L);
        MesPqcInspectionTaskDO task = fixture.taskMapper.selectByIdForUpdate(Fixture.TASK_ID);
        task.setTaskStatus(MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED).setQaProcessId(6001L)
                .setRouteVersionId(2101L).setRegulationVersionId(6101L).setInspectionType("PROCESS")
                .setBusinessDate(java.time.LocalDate.of(2026, 9, 28)).setShiftCode("DAY").setRoundNo(1);
        MesProProcessPoolPqcRecordDO record = fixture.pqcRecordMapper.selectByEventId(Fixture.EVENT_ID);
        record.setWorkOrderId(1001L).setRouteId(2001L).setQaProcessId(6001L)
                .setProcessInspectionAggregationStatus("AGGREGATED").setProcessInspectionReviewId(7000L);
        when(fixture.eventMapper.selectById(Fixture.EVENT_ID)).thenReturn(event);
        when(fixture.taskMapper.selectById(Fixture.TASK_ID)).thenReturn(task);
        AtomicReference<List<MesPqcInspectionPieceDetailDO>> currentDetails = new AtomicReference<>(
                fixture.pieceDetailMapper.selectListByTaskId(Fixture.TASK_ID));
        when(fixture.pieceDetailMapper.selectListByTaskId(Fixture.TASK_ID))
                .thenAnswer(invocation -> currentDetails.get());
        when(fixture.pieceDetailMapper.insertBatch(any())).thenAnswer(invocation -> {
            List<MesPqcInspectionPieceDetailDO> rows = invocation.getArgument(0);
            for (int i = 0; i < rows.size(); i++) rows.get(i).setId(7200L + i);
            currentDetails.set(rows);
            return true;
        });
        when(fixture.taskMapper.updateById(any(MesPqcInspectionTaskDO.class))).thenAnswer(invocation -> {
            task.setActualInspectionQuantity(invocation.getArgument(0, MesPqcInspectionTaskDO.class)
                    .getActualInspectionQuantity());
            return 1;
        });
        MesPqcProcessInspectionAggregateDetailMapper aggregateMapper =
                mock(MesPqcProcessInspectionAggregateDetailMapper.class);
        when(aggregateMapper.deleteByEventId(Fixture.EVENT_ID)).thenReturn(1);
        when(aggregateMapper.insertBatch(any())).thenReturn(true);
        fixture.setupConfirmedReview();
        when(fixture.pqcRecordMapper.replaceProcessInspectionReviewIfAggregated(any(), any(), any(), any(), any()))
                .thenReturn(1);
        when(fixture.revisionService.updatePqcInspectionRecord(any())).thenAnswer(invocation -> {
            event.setRawPayload(invocation.getArgument(0, MesProcessPoolEventRevisionUpdateReqBO.class).getAfterPayload());
            return 701L;
        });
        MesPqcProcessInspectionAggregationService realAggregation =
                new MesPqcProcessInspectionAggregationServiceImpl(fixture.pqcRecordMapper,
                        fixture.eventMapper, fixture.taskMapper, fixture.pieceDetailMapper, aggregateMapper,
                        fixture.reviewMapper);
        ReflectionTestUtils.setField(fixture.service, "aggregationService", realAggregation);

        assertEquals(701L, fixture.service.correct(fixture.command("合格")));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MesPqcProcessInspectionAggregateDetailDO>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(aggregateMapper).insertBatch(captor.capture());
        assertEquals("合格", captor.getValue().get(0).getMeasuredValue());
        assertEquals(7200L, captor.getValue().get(0).getSourcePieceDetailId());
        assertEquals(MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED, task.getTaskStatus());
        assertEquals(7001L, captor.getValue().get(0).getReviewId());
        verify(aggregateMapper).deleteByEventId(Fixture.EVENT_ID);
    }

    @Test
    void blocksCompletedRemovedAndReworkedCycleBeforeSignatureOrFormalWrites() {
        for (String status : List.of("COMPLETED", "REWORKED", "RELEASED", "REMOVED")) {
            Fixture fixture = new Fixture("BOOLEAN", null, null, null);
            fixture.activeOrder.setBusinessStatus(status);
            assertThrows(ServiceException.class, () -> fixture.service.correct(fixture.command("合格")));
            verifyNoInteractions(fixture.signatureService, fixture.revisionService, fixture.reviewMapper);
            verify(fixture.pieceDetailMapper, never()).deleteByTaskId(any());
        }
    }

    @Test
    void twoSignedCorrectionsRefreshCurrentAggregationAndKeepReviewAndPieceHistory() {
        Fixture fixture = new Fixture("BOOLEAN", null, null, null);
        MesProProcessPoolEventDO event = fixture.eventMapper.selectById(Fixture.EVENT_ID);
        event.setQaProcessId(6001L);
        MesPqcInspectionTaskDO task = fixture.taskMapper.selectById(Fixture.TASK_ID);
        task.setTaskStatus("CONFIRMED").setQaProcessId(6001L).setRouteVersionId(2101L)
                .setRegulationVersionId(6101L).setInspectionType("PROCESS")
                .setBusinessDate(java.time.LocalDate.of(2026, 9, 28)).setShiftCode("DAY").setRoundNo(1);
        MesProProcessPoolPqcRecordDO record = fixture.pqcRecordMapper.selectByEventId(Fixture.EVENT_ID);
        record.setWorkOrderId(1001L).setRouteId(2001L).setQaProcessId(6001L)
                .setProcessInspectionAggregationStatus("AGGREGATED").setProcessInspectionReviewId(7000L);
        List<MesProcessPoolSubmissionReviewDO> reviews = fixture.setupConfirmedReview();
        List<List<MesPqcInspectionPieceDetailDO>> pieceHistory = new ArrayList<>();
        pieceHistory.add(fixture.pieceDetailMapper.selectListByTaskId(Fixture.TASK_ID));
        when(fixture.pieceDetailMapper.selectListByTaskId(Fixture.TASK_ID))
                .thenAnswer(invocation -> pieceHistory.get(pieceHistory.size() - 1));
        when(fixture.pieceDetailMapper.insertBatch(any())).thenAnswer(invocation -> {
            List<MesPqcInspectionPieceDetailDO> next = invocation.getArgument(0);
            next.get(0).setId(7200L + pieceHistory.size());
            pieceHistory.add(next);
            return true;
        });
        AtomicLong revisionId = new AtomicLong(700L);
        when(fixture.revisionService.updatePqcInspectionRecord(any())).thenAnswer(invocation -> {
            event.setRawPayload(invocation.getArgument(0, MesProcessPoolEventRevisionUpdateReqBO.class).getAfterPayload());
            return revisionId.incrementAndGet();
        });
        AtomicLong signatureId = new AtomicLong(9200L);
        org.mockito.Mockito.doAnswer(invocation -> fixture.formalSignatures.prepare(invocation.getArgument(0),
                Fixture.signature().setSignatureId(signatureId.incrementAndGet())))
                .when(fixture.signatureService).recordFieldChangeSignature(any());
        when(fixture.pqcRecordMapper.replaceProcessInspectionReviewIfAggregated(any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    assertEquals(record.getProcessInspectionReviewId(), invocation.getArgument(2));
                    record.setProcessInspectionReviewId(invocation.getArgument(3));
                    return 1;
                });
        MesPqcProcessInspectionAggregateDetailMapper aggregateMapper = mock(MesPqcProcessInspectionAggregateDetailMapper.class);
        when(aggregateMapper.deleteByEventId(Fixture.EVENT_ID)).thenReturn(1);
        when(aggregateMapper.insertBatch(any())).thenReturn(true);
        ReflectionTestUtils.setField(fixture.service, "aggregationService",
                new MesPqcProcessInspectionAggregationServiceImpl(fixture.pqcRecordMapper, fixture.eventMapper,
                        fixture.taskMapper, fixture.pieceDetailMapper, aggregateMapper, fixture.reviewMapper));

        assertEquals(701L, fixture.service.correct(fixture.command("合格")));
        assertEquals(702L, fixture.service.correct(fixture.command(List.of("不合格"), 1)));

        assertEquals(3, pieceHistory.size());
        assertEquals("原值", pieceHistory.get(0).get(0).getMeasuredValue());
        assertEquals("FAILURE", pieceHistory.get(2).get(0).getJudgement());
        assertEquals(3, reviews.size());
        assertEquals(9201L, reviews.get(1).getReviewSignatureId());
        assertEquals(9202L, reviews.get(2).getReviewSignatureId());
        assertEquals(7002L, record.getProcessInspectionReviewId());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MesPqcProcessInspectionAggregateDetailDO>> rows = ArgumentCaptor.forClass(List.class);
        verify(aggregateMapper, org.mockito.Mockito.times(2)).insertBatch(rows.capture());
        assertEquals("FAILURE", rows.getAllValues().get(1).get(0).getJudgement());
        assertEquals(7002L, rows.getAllValues().get(1).get(0).getReviewId());
    }

    @Test
    void blocksPendingReleaseApplicationBeforeSignatureOrFormalWrites() {
        Fixture fixture = new Fixture("BOOLEAN", null, null, null);
        when(fixture.releaseStateService.isReleaseApplicationLockedForUpdate(5001L)).thenReturn(true);
        assertThrows(ServiceException.class, () -> fixture.service.correct(fixture.command("合格")));
        verifyNoInteractions(fixture.signatureService, fixture.revisionService, fixture.reviewMapper);
    }

    @Test
    void rejectedCorrectionSignsCurrentReviewLinkInsteadOfInheritingPreviousPayloadLink() {
        Fixture fixture = new Fixture("BOOLEAN", null, null, null);
        fixture.eventMapper.selectById(Fixture.EVENT_ID).setRawPayload(
                "{\"inspectionResult\":\"SUCCESS\",\"scrapQuantity\":0,\"supersededReviewId\":6999}");
        when(fixture.reviewMapper.selectLatestByEventIdForUpdate(Fixture.EVENT_ID))
                .thenReturn(MesProcessPoolSubmissionReviewDO.builder().id(7000L).eventId(Fixture.EVENT_ID)
                        .leaderType("PQC").reviewStatus("REJECTED").build());

        assertEquals(701L, fixture.service.correct(fixture.command("合格")));

        ArgumentCaptor<MesProcessPoolEventRevisionUpdateReqBO> revision =
                ArgumentCaptor.forClass(MesProcessPoolEventRevisionUpdateReqBO.class);
        verify(fixture.revisionService).updatePqcInspectionRecord(revision.capture());
        assertTrue(revision.getValue().getAfterPayload().contains("\"supersededReviewId\":7000"));
        assertEquals(9102L, revision.getValue().getRevisionSignatureId());
        verify(fixture.reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
        verifyNoInteractions(fixture.aggregationService);
    }

    @Test
    void correctPqcInspection_shouldRejectWhenPqcSubmissionIsUnderNonconformanceReview() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/"
                        + "MesProcessPoolPqcInspectionCorrectionService.java"));

        int guard = source.indexOf("nonconformanceReviewService.ensureWorkOrderNotFrozen");
        int signature = source.indexOf("recordCorrectionSignature");
        int formalUpdate = source.indexOf("updateFormalPqcTables");
        assertTrue(guard > 0, "PQC correction must check nonconformance freeze before controlled correction writes");
        assertTrue(guard < signature, "PQC correction freeze gate must run before electronic signature is recorded");
        assertTrue(guard < formalUpdate, "PQC correction freeze gate must run before formal PQC facts are overwritten");
    }

    @Test
    void correctStopsBeforeSignatureAndFormalWritesWhenNonconformanceReviewFreezesWorkOrder() {
        Fixture fixture = new Fixture("BOOLEAN", null, null, null);
        doThrow(new ServiceException(PRO_EDHR_NONCONFORMANCE_REVIEW_FROZEN_ACTION_LOCKED))
                .when(fixture.nonconformanceReviewService).ensureWorkOrderNotFrozen(1001L, "PQC检验更正");

        ServiceException error = assertThrows(ServiceException.class,
                () -> fixture.service.correct(fixture.command("不合格")));

        assertEquals(PRO_EDHR_NONCONFORMANCE_REVIEW_FROZEN_ACTION_LOCKED.getCode(), error.getCode());
        verify(fixture.nonconformanceReviewService).ensureWorkOrderNotFrozen(1001L, "PQC检验更正");
        verifyNoInteractions(fixture.signatureService, fixture.releaseStateService,
                fixture.pqcRecordMapper, fixture.pieceDetailMapper, fixture.revisionService);
    }

    @Test
    void appliesCanonicalBooleanSemantics() {
        assertCorrection("BOOLEAN", "合格", null, null, null, "SUCCESS", "合格");
        assertCorrection("BOOLEAN", "不合格", null, null, null, "FAILURE", "不合格");
    }

    @Test
    void appliesCanonicalNumericInclusiveBoundsAndPrecision() {
        assertCorrection("NUMERIC", "1.00", decimal("1.00"), decimal("2.00"), 2, "SUCCESS", "1.00");
        assertCorrection("NUMERIC", "2.00", decimal("1.00"), decimal("2.00"), 2, "SUCCESS", "2.00");
        assertCorrection("NUMERIC", "2.01", decimal("1.00"), decimal("2.00"), 2, "FAILURE", "2.01");

        assertInvalidCorrection("NUMERIC", "1.001", decimal("1.00"), decimal("2.00"), 2);
        assertInvalidCorrection("NUMERIC", "1e0", decimal("1.00"), decimal("2.00"), 2);
    }

    @Test
    void appliesCanonicalTextSemanticsAndRejectsBlankText() {
        assertCorrection("TEXT", "  修正说明  ", null, null, null, "SUCCESS", "修正说明");
        assertInvalidCorrection("TEXT", "   ", null, null, null);
    }

    @Test
    void rejectsLegacyNumberAndChoiceAliases() {
        assertInvalidCorrection("NUMBER", "1.00", decimal("1.00"), decimal("2.00"), 2);
        assertInvalidCorrection("CHOICE", "合格", null, null, null);
    }

    @Test
    void rejectsScrapQuantityAboveActualInspectionQuantityBeforeLoadingEvent() {
        Fixture fixture = new Fixture("BOOLEAN", null, null, null);

        ServiceException error = assertThrows(ServiceException.class,
                () -> fixture.service.correct(fixture.command(
                        List.of("合格", "合格", "合格", "合格", "合格"), 10)));

        assertEquals(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED.getCode(), error.getCode());
        verify(fixture.eventMapper, never()).selectByIdForUpdate(any());
        verify(fixture.signatureService, never()).recordFieldChangeSignature(any());
        verify(fixture.revisionService, never()).updatePqcInspectionRecord(any());
    }

    @Test
    void rejectsScrapQuantityBelowFailedPieceDetailFloorBeforeWrite() {
        Fixture fixture = new Fixture("BOOLEAN", null, null, null);

        ServiceException error = assertThrows(ServiceException.class,
                () -> fixture.service.correct(fixture.command(
                        List.of("不合格", "不合格", "合格", "合格", "合格"), 1)));

        assertEquals(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED.getCode(), error.getCode());
        verify(fixture.signatureService, never()).recordFieldChangeSignature(any());
        verify(fixture.revisionService, never()).updatePqcInspectionRecord(any());
        verify(fixture.taskMapper, never()).updateById(any(MesPqcInspectionTaskDO.class));
        verify(fixture.pieceDetailMapper, never()).deleteByTaskId(any());
        verify(fixture.pieceDetailMapper, never()).insertBatch(any());
        verify(fixture.pqcRecordMapper, never()).updateById(any(MesProProcessPoolPqcRecordDO.class));
    }

    @Test
    void permitsScrapQuantityEqualFailedPieceDetailFloor() {
        Fixture fixture = new Fixture("BOOLEAN", null, null, null);

        assertEquals(701L, fixture.service.correct(fixture.command(
                List.of("不合格", "不合格", "合格", "合格", "合格"), 2)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MesPqcInspectionPieceDetailDO>> detailsCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(fixture.pieceDetailMapper).insertBatch(detailsCaptor.capture());
        List<MesPqcInspectionPieceDetailDO> details = detailsCaptor.getValue();
        assertEquals(5, details.size());
        assertEquals(2L, details.stream()
                .filter(detail -> MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE.equals(
                        detail.getJudgement()))
                .map(MesPqcInspectionPieceDetailDO::getSampleNo)
                .distinct()
                .count());

        ArgumentCaptor<MesProProcessPoolPqcRecordDO> recordCaptor =
                ArgumentCaptor.forClass(MesProProcessPoolPqcRecordDO.class);
        verify(fixture.pqcRecordMapper).updateById(recordCaptor.capture());
        assertEquals(MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE,
                recordCaptor.getValue().getInspectionResult());
    }

    @Test
    void rejectsFrozenWorkOrderBeforeCorrectionWrites() {
        Fixture fixture = new Fixture("BOOLEAN", null, null, null);
        doThrow(new ServiceException(PRO_EDHR_NONCONFORMANCE_REVIEW_FROZEN_ACTION_LOCKED))
                .when(fixture.nonconformanceReviewService).ensureWorkOrderNotFrozen(1001L, "PQC检验更正");

        ServiceException error = assertThrows(ServiceException.class,
                () -> fixture.service.correct(fixture.command("不合格")));

        assertEquals(PRO_EDHR_NONCONFORMANCE_REVIEW_FROZEN_ACTION_LOCKED.getCode(), error.getCode());
        verify(fixture.nonconformanceReviewService).ensureWorkOrderNotFrozen(1001L, "PQC检验更正");
        verify(fixture.signatureService, never()).recordFieldChangeSignature(any());
        verify(fixture.revisionService, never()).updatePqcInspectionRecord(any());
        verify(fixture.pqcRecordMapper, never()).selectByEventId(any());
        verify(fixture.pqcRecordMapper, never()).selectByEventIdForUpdate(any());
        verify(fixture.pieceDetailMapper, never()).deleteByTaskId(any());
        verify(fixture.pieceDetailMapper, never()).insertBatch(any());
        verify(fixture.taskMapper, never()).updateById(any(MesPqcInspectionTaskDO.class));
        verify(fixture.pqcRecordMapper, never()).updateById(any(MesProProcessPoolPqcRecordDO.class));
        verify(fixture.aggregationService, never()).aggregateApprovedPqcSubmission(any(), any());
    }

    private static void assertCorrection(String resultType, String requestedValue,
                                         BigDecimal lower, BigDecimal upper, Integer precision,
                                         String expectedJudgement, String expectedStoredValue) {
        Fixture fixture = new Fixture(resultType, lower, upper, precision);

        int scrapQuantity = MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE.equals(expectedJudgement) ? 1 : 0;
        assertEquals(701L, fixture.service.correct(fixture.command(List.of(requestedValue), scrapQuantity)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MesPqcInspectionPieceDetailDO>> detailsCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(fixture.pieceDetailMapper).insertBatch(detailsCaptor.capture());
        MesPqcInspectionPieceDetailDO detail = detailsCaptor.getValue().get(0);
        assertEquals(resultType, detail.getResultType());
        assertEquals(expectedStoredValue, detail.getMeasuredValue());
        assertEquals(expectedJudgement, detail.getJudgement());

        ArgumentCaptor<MesProProcessPoolPqcRecordDO> recordCaptor =
                ArgumentCaptor.forClass(MesProProcessPoolPqcRecordDO.class);
        verify(fixture.pqcRecordMapper).updateById(recordCaptor.capture());
        assertEquals(expectedJudgement, recordCaptor.getValue().getInspectionResult());

        ArgumentCaptor<MesProcessPoolEventRevisionUpdateReqBO> revisionCaptor =
                ArgumentCaptor.forClass(MesProcessPoolEventRevisionUpdateReqBO.class);
        verify(fixture.revisionService).updatePqcInspectionRecord(revisionCaptor.capture());
        String afterPayload = revisionCaptor.getValue().getAfterPayload();
        org.junit.jupiter.api.Assertions.assertFalse(
                afterPayload.contains("nonconformanceDescription"),
                "PQC correction payload must not write the removed nonconformance description");
        org.junit.jupiter.api.Assertions.assertFalse(
                afterPayload.contains("defectDescription"),
                "PQC correction payload must not write the removed defect description");
    }

    private static void assertInvalidCorrection(String resultType, String requestedValue,
                                                BigDecimal lower, BigDecimal upper, Integer precision) {
        Fixture fixture = new Fixture(resultType, lower, upper, precision);

        ServiceException error = assertThrows(ServiceException.class,
                () -> fixture.service.correct(fixture.command(requestedValue)));

        assertEquals(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED.getCode(), error.getCode());
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }

    private static final class Fixture {

        private static final long EVENT_ID = 176L;
        private static final long TASK_ID = 5101L;
        private static final long ACTOR_ID = 3001L;

        private final MesProProcessPoolEventMapper eventMapper =
                mock(MesProProcessPoolEventMapper.class);
        private final MesPqcInspectionTaskMapper taskMapper =
                mock(MesPqcInspectionTaskMapper.class);
        private final MesProProcessPoolPqcRecordMapper pqcRecordMapper =
                mock(MesProProcessPoolPqcRecordMapper.class);
        private final MesPqcInspectionPieceDetailMapper pieceDetailMapper =
                mock(MesPqcInspectionPieceDetailMapper.class);
        private final MesProcessPoolEventRevisionService revisionService =
                mock(MesProcessPoolEventRevisionService.class);
        private final MesProBatchRecordExecutionSignatureService signatureService =
                mock(MesProBatchRecordExecutionSignatureService.class);
        private final MesReportAllocationReleaseStateService releaseStateService =
                mock(MesReportAllocationReleaseStateService.class);
        private final MesProEdhrNonconformanceReviewService nonconformanceReviewService =
                mock(MesProEdhrNonconformanceReviewService.class);
        private final MesPqcProcessInspectionAggregationService aggregationService =
                mock(MesPqcProcessInspectionAggregationService.class);
        private final MesProcessPoolActiveOrderMapper activeOrderMapper = mock(MesProcessPoolActiveOrderMapper.class);
        private final MesProcessPoolSubmissionReviewMapper reviewMapper = mock(MesProcessPoolSubmissionReviewMapper.class);
        private final MesPqcProcessInspectionAggregateDetailMapper auditAggregateMapper =
                mock(MesPqcProcessInspectionAggregateDetailMapper.class);
        private final MesProcessPoolActiveOrderDO activeOrder = MesProcessPoolActiveOrderDO.builder()
                .id(5001L).workOrderId(1001L).activeStatus("ACTIVE").businessStatus("ACTIVE").build();
        private final MesProcessPoolPqcInspectionCorrectionService service;
        private final FormalSignatureFixture formalSignatures;

        private Fixture(String resultType, BigDecimal lower, BigDecimal upper, Integer precision) {
            MesTeamLeaderScopeService scopeService = mock(MesTeamLeaderScopeService.class);

            MesProProcessPoolEventDO event = event();
            MesPqcInspectionTaskDO task = task();
            when(eventMapper.selectByIdForUpdate(EVENT_ID)).thenReturn(event);
            when(eventMapper.selectById(EVENT_ID)).thenReturn(event);
            when(taskMapper.selectByIdForUpdate(TASK_ID)).thenReturn(task);
            when(taskMapper.selectById(TASK_ID)).thenReturn(task);
            activeOrder.setTenantId(1L);
            when(activeOrderMapper.selectByIdForUpdate(5001L)).thenReturn(activeOrder);
            when(releaseStateService.findReleasedActiveOrderIdsForUpdate(List.of(5001L))).thenReturn(Set.of());
            when(pqcRecordMapper.selectByEventId(EVENT_ID)).thenReturn(record());
            // Nonconcurrent fixtures expose the same row to both read modes; snapshot tests use separate versions.
            when(pqcRecordMapper.selectByEventIdForUpdate(EVENT_ID))
                    .thenAnswer(invocation -> pqcRecordMapper.selectByEventId(EVENT_ID));
            when(pieceDetailMapper.selectListByTaskId(TASK_ID))
                    .thenReturn(List.of(existingDetail(resultType, lower, upper, precision)));
            when(pieceDetailMapper.selectListByTaskIdForUpdate(TASK_ID))
                    .thenAnswer(invocation -> pieceDetailMapper.selectListByTaskId(TASK_ID));
            when(signatureService.recordFieldChangeSignature(any())).thenReturn(signature());
            when(revisionService.updatePqcInspectionRecord(any())).thenReturn(701L);
            when(taskMapper.updateById(any(MesPqcInspectionTaskDO.class))).thenReturn(1);
            when(pieceDetailMapper.insertBatch(any())).thenReturn(Boolean.TRUE);
            when(pqcRecordMapper.updateById(any(MesProProcessPoolPqcRecordDO.class))).thenReturn(1);

            service = new MesProcessPoolPqcInspectionCorrectionService(eventMapper, pqcRecordMapper,
                    taskMapper, pieceDetailMapper, revisionService, signatureService, scopeService,
                    releaseStateService, aggregationService, nonconformanceReviewService);
            ReflectionTestUtils.setField(service, "activeOrderMapper", activeOrderMapper);
            ReflectionTestUtils.setField(service, "reviewMapper", reviewMapper);
            ReflectionTestUtils.setField(service, "gxpAuditService",
                    mock(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService.class));
            ReflectionTestUtils.setField(service, "aggregateDetailMapper", auditAggregateMapper);
            formalSignatures = new FormalSignatureFixture(this, "valid");
            ReflectionTestUtils.setField(service, "electronicSignatureQueryService", formalSignatures.query);
        }

        private List<MesProcessPoolSubmissionReviewDO> setupConfirmedReview() {
            MesProcessPoolSubmissionReviewDO review = MesProcessPoolSubmissionReviewDO.builder()
                    .id(7000L).eventId(EVENT_ID).leaderType("PQC").leaderUserId(ACTOR_ID)
                    .reviewStatus("APPROVED").reviewSignatureId(9000L).reviewSignatureUserId(ACTOR_ID).build();
            review.setTenantId(1L);
            List<MesProcessPoolSubmissionReviewDO> history = new ArrayList<>(List.of(review));
            when(reviewMapper.selectLatestByEventIdForUpdate(EVENT_ID))
                    .thenAnswer(invocation -> history.get(history.size() - 1));
            when(reviewMapper.insert(any(MesProcessPoolSubmissionReviewDO.class))).thenAnswer(invocation -> {
                MesProcessPoolSubmissionReviewDO next = invocation.getArgument(0);
                next.setId(7000L + history.size());
                history.add(next);
                return 1;
            });
            return history;
        }

        private MesProcessPoolPqcInspectionCorrectionCommand command(String requestedValue) {
            return command(List.of(requestedValue), 0);
        }

        private MesProcessPoolPqcInspectionCorrectionCommand command(List<String> requestedValues,
                                                                     int scrapQuantity) {
            return new MesProcessPoolPqcInspectionCorrectionCommand()
                    .setEventId(EVENT_ID)
                    .setActorUserId(ACTOR_ID)
                    .setActualInspectionQuantity(requestedValues.size())
                    .setScrapQuantity(scrapQuantity)
                    .setItemResults(List.of(new MesProcessPoolPqcInspectionCorrectionCommand.ItemResultCommand()
                            .setItemCode("QA-001")
                            .setSampleValues(requestedValues)))
                    .setChangeReason("纠正检验值")
                    .setSignaturePassword("valid-password");
        }

        private static MesProProcessPoolEventDO event() {
            MesProProcessPoolEventDO event = MesProProcessPoolEventDO.builder()
                    .id(EVENT_ID).eventType(MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION)
                    .feedbackSourceType("MES_PQC_INSPECTION_TASK").feedbackSourceId(TASK_ID)
                    .recordbookSourceType("MES_PQC_INSPECTION_TASK").recordbookSourceId(TASK_ID)
                    .workOrderId(1001L).routeId(2001L).routeProcessId(3001L).processId(4001L)
                    .actualEmployeeId(101L).rawPayload("{\"inspectionResult\":\"SUCCESS\"," +
                            "\"scrapQuantity\":0,\"nonconformanceDescription\":\"纠正前\"}")
                    .build();
            event.setTenantId(1L);
            return event;
        }

        private static MesPqcInspectionTaskDO task() {
            MesPqcInspectionTaskDO task = MesPqcInspectionTaskDO.builder().id(TASK_ID).activeOrderId(5001L)
                    .workOrderId(1001L).routeId(2001L).routeProcessId(3001L).processId(4001L)
                    .actualInspectionQuantity(1).taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED).build();
            task.setTenantId(1L);
            return task;
        }

        private static MesProProcessPoolPqcRecordDO record() {
            MesProProcessPoolPqcRecordDO record = MesProProcessPoolPqcRecordDO.builder()
                    .id(6101L).eventId(EVENT_ID).inspectionResult("SUCCESS").build();
            record.setTenantId(1L);
            return record;
        }

        private static MesPqcInspectionPieceDetailDO existingDetail(
                String resultType, BigDecimal lower, BigDecimal upper, Integer precision) {
            MesPqcInspectionPieceDetailDO detail = MesPqcInspectionPieceDetailDO.builder()
                    .id(7101L).taskId(TASK_ID).sampleNo(1).itemCode("QA-001").itemName("检验项目")
                    .inspectionMethod("目测").standardText("正式标准")
                    .standardLowerLimit(lower).standardUpperLimit(upper).standardPrecision(precision)
                    .resultType(resultType).itemResult("原值").measuredValue("原值").judgement("SUCCESS")
                    .build();
            detail.setTenantId(1L);
            return detail;
        }

        private static MesProBatchRecordExecutionFieldAuditSignatureResult signature() {
            return new MesProBatchRecordExecutionFieldAuditSignatureResult()
                    .setSignatureId(9102L).setActorId(ACTOR_ID).setActorName("PQC组长")
                    .setSignedAt(LocalDateTime.of(2026, 8, 14, 10, 0));
        }
    }
}
