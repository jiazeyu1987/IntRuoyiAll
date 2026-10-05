package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesFrontlineLossReasonValidator;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.*;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureQueryServiceImpl;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.*;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.*;
import org.h2.api.Trigger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Real H2/MyBatis writes, revision/summary, signature query and GxP writer in one Spring transaction.
 * Password authentication, employee authorization and FIFO checking are explicit boundary doubles.
 * The bit-literal translation is test-only and is not an InnoDB concurrency claim.
 */
class MesProcessPoolProductionReportCorrectionAuditTransactionTest {
    private static final String OPERATION = "mes.production-report.correct";
    private static final String EVENT = "mes_pro_process_pool_event";
    private static final String REVISION = EVENT + "_revision";
    private static final String DIFF = REVISION + "_diff";
    private static final String FRAGMENT = "mes_pro_process_pool_quantity_fragment";
    private static final String SIGNATURE = "system_electronic_signature";
    private static final AtomicBoolean FAILURE_SAW_WRITES = new AtomicBoolean();
    private JdbcTemplate jdbc;
    private MesProcessPoolProductionReportCorrectionService service;
    private MesTeamLeaderScopeService scope;
    private MesProBatchRecordExecutionSignatureService signer;
    private ElectronicSignatureRecordMapper signatures;
    private ElectronicSignatureQueryServiceImpl signatureQuery;
    private String signatureDefect = "valid";

    @BeforeEach
    void fixture() throws Exception {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:production_correction_" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        for (Class<?> row : List.of(MesProProcessPoolEventDO.class, MesProProcessPoolQuantityFragmentDO.class,
                MesProFeedbackDO.class, MesProFeedbackMaterialDO.class, MesProProcessPoolEventRevisionDO.class,
                MesProProcessPoolEventRevisionDiffDO.class, ElectronicSignatureRecordDO.class, GxpAuditEventDO.class,
                GxpAuditEventRelationDO.class, GxpAuditLedgerSequenceDO.class, GxpAuditPolicyActivationDO.class,
                GxpAuditPolicyOperationDO.class)) createTable(row);
        assertEquals(SIGNATURE, ElectronicSignatureRecordDO.class.getAnnotation(TableName.class).value());
        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addInterceptor(new MysqlBitsForH2());
        List.of(MesProProcessPoolEventMapper.class, MesProProcessPoolQuantityFragmentMapper.class,
                MesProFeedbackMapper.class, MesProFeedbackMaterialMapper.class, MesProProcessPoolEventRevisionMapper.class,
                MesProProcessPoolEventRevisionDiffMapper.class, ElectronicSignatureRecordMapper.class,
                GxpAuditEventMapper.class, GxpAuditEventRelationMapper.class, GxpAuditLedgerSequenceMapper.class,
                GxpAuditPolicyActivationMapper.class, GxpAuditPolicyOperationMapper.class).forEach(configuration::addMapper);
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setConfiguration(configuration);
        var session = new SqlSessionTemplate(Objects.requireNonNull(factory.getObject()));
        var manager = new DataSourceTransactionManager(dataSource);
        var events = session.getMapper(MesProProcessPoolEventMapper.class);
        var revisions = session.getMapper(MesProProcessPoolEventRevisionMapper.class);
        var diffs = session.getMapper(MesProProcessPoolEventRevisionDiffMapper.class);
        signatures = session.getMapper(ElectronicSignatureRecordMapper.class);
        signatureQuery = new ElectronicSignatureQueryServiceImpl();
        ReflectionTestUtils.setField(signatureQuery, "signatureRecordMapper", signatures);
        ReflectionTestUtils.setField(signatureQuery, "subjectAdapters", List.of(new MesBatchRecordSignatureSubjectAdapter()));
        signer = mock(MesProBatchRecordExecutionSignatureService.class);
        when(signer.recordFieldChangeSignature(any())).thenAnswer(call -> insertSignature(call.getArgument(0)));
        var revisionTarget = new MesProcessPoolEventRevisionServiceImpl(events, revisions, diffs,
                mock(MesProcessPoolFifoAllocationService.class), mock(MesProcessPoolSubmissionReviewMapper.class), signer);
        org.springframework.test.util.ReflectionTestUtils.setField(revisionTarget, "nonconformanceReviewService", org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService.class));
        var summary = new MesProductionReportManagementSummaryService(events, mock(MesProcessPoolReportAllocationMapper.class),
                mock(MesProcessPoolActiveOrderReleaseApplicationMapper.class), new MesReportAllocationPoolQuantityService(),
                mock(MesReportAllocationReleaseStateService.class));
        scope = mock(MesTeamLeaderScopeService.class);
        var target = new MesProcessPoolProductionReportCorrectionService(events,
                session.getMapper(MesProProcessPoolQuantityFragmentMapper.class), session.getMapper(MesProFeedbackMapper.class),
                session.getMapper(MesProFeedbackMaterialMapper.class), (MesProcessPoolEventRevisionService) tx(revisionTarget, manager),
                signer, mock(MesFrontlineLossReasonValidator.class), scope, summary);
        org.springframework.test.util.ReflectionTestUtils.setField(target, "nonconformanceReviewService", org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService.class));
        var fixtureOwners = org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffOwnerResolver.class);
        org.springframework.test.util.ReflectionTestUtils.setField(target, "handoffOwners", fixtureOwners);
        org.mockito.Mockito.lenient().when(fixtureOwners.submissionIdentity(org.mockito.ArgumentMatchers.any())).thenAnswer(call -> new cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffOwnerResolver.SubmissionIdentity("SYSTEM_USER", call.getArgument(0, cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO.class).getActualEmployeeId(), call.getArgument(0, cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO.class).getDeviceAccountId()));
        var writer = new GxpAuditServiceImpl();
        ReflectionTestUtils.setField(writer, "auditEventMapper", session.getMapper(GxpAuditEventMapper.class));
        ReflectionTestUtils.setField(writer, "eventRelationMapper", session.getMapper(GxpAuditEventRelationMapper.class));
        ReflectionTestUtils.setField(writer, "ledgerSequenceMapper", session.getMapper(GxpAuditLedgerSequenceMapper.class));
        ReflectionTestUtils.setField(writer, "policyActivationMapper", session.getMapper(GxpAuditPolicyActivationMapper.class));
        ReflectionTestUtils.setField(writer, "policyOperationMapper", session.getMapper(GxpAuditPolicyOperationMapper.class));
        // Pre-fix RED must reach the actual missing append, not fail merely on a new field's absence.
        Map<Class<?>, Object> injected = Map.of(GxpAuditService.class, tx(writer, manager),
                ElectronicSignatureQueryService.class, signatureQuery, MesProProcessPoolEventRevisionMapper.class, revisions,
                MesProProcessPoolEventRevisionDiffMapper.class, diffs);
        for (var field : target.getClass().getDeclaredFields()) {
            if (injected.containsKey(field.getType())) {
                field.setAccessible(true);
                field.set(target, injected.get(field.getType()));
            }
        }
        service = (MesProcessPoolProductionReportCorrectionService) tx(target, manager);
        TenantContextHolder.setTenantId(1L);
        var actor = new LoginUser();
        actor.setId(3001L);
        actor.setTenantId(1L);
        actor.setInfo(Map.of("username", "correction-test", LoginUser.INFO_KEY_NICKNAME, "correction-test"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor, null));
        jdbc.update("INSERT INTO gxp_audit_ledger_sequence(tenant_id,next_ledger_sequence) VALUES(1,1)");
        jdbc.update("INSERT INTO gxp_audit_policy_activation(id,tenant_id,policy_version) VALUES(1,1,'correction-test')");
        jdbc.update("INSERT INTO gxp_audit_policy_operation(tenant_id,policy_version,operation_id,domain,subject_type,"
                + "action_type,reason_policy,signature_policy,state_policy,applicability,active)"
                + " VALUES(1,'correction-test',?,'MES','MES_PROCESS_POOL_EVENT','UPDATE','USER_REQUIRED','REQUIRED',"
                + "'PRESENT_TO_PRESENT','GXP',1)", OPERATION);
        String payload = "{\"outputQuantity\":4,\"lossQuantity\":0,\"lossDetails\":[],"
                + "\"fieldValues\":{\"OUTPUT_QUANTITY\":4,\"SCRAP_QUANTITY\":0},"
                + "\"materialDetails\":[{\"materialId\":3401,\"materialCode\":\"M-1\",\"materialName\":\"original\","
                + "\"outputQuantity\":4,\"lossQuantity\":0,\"lossDetails\":[],\"deviceParameterReadings\":[]}]}";
        jdbc.update("INSERT INTO " + EVENT + "(id,tenant_id,pool_id,event_type,work_order_id,route_id,route_process_id,"
                + "process_id,actual_employee_id,feedback_source_type,feedback_source_id,raw_payload,signature_id,"
                + "report_output_quantity,report_allocated_quantity,report_unallocated_quantity)"
                + " VALUES(176,1,100, 'PRODUCTION_SUBMIT',20,30,31,40,2001,'MES_PRO_FEEDBACK',5101,?,8001,4,0,4)", payload);
        jdbc.update("INSERT INTO " + FRAGMENT + "(id,tenant_id,event_id,source_quantity_type,total_quantity,"
                + "allocated_quantity,available_quantity) VALUES(6001,1,176,'OUTPUT',4,0,4)");
        jdbc.update("INSERT INTO mes_pro_feedback(id,work_order_id,route_id,process_id,feedback_quantity,"
                + "qualified_quantity,unqualified_quantity) VALUES(5101,20,30,40,4,4,0)");
        jdbc.update("INSERT INTO mes_pro_feedback_material(id,tenant_id,feedback_id,active_order_id,work_order_id,"
                + "material_id,material_code,material_name,output_quantity,loss_quantity,loss_details_json,"
                + "device_parameter_readings_json) VALUES(6101,1,5101,101,20,3401,'M-1','original',4,0,'[]','[]')");
        FAILURE_SAW_WRITES.set(false);
    }

    @AfterEach
    void close() throws SQLException {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
        if (jdbc != null) {
            // SHUTDOWN closes H2 immediately; JdbcTemplate would then query warnings on the closed statement.
            try (var connection = Objects.requireNonNull(jdbc.getDataSource()).getConnection();
                 var statement = connection.createStatement()) {
                statement.execute("SHUTDOWN");
            }
        }
    }


    @Test
    void profileOriginalIdentitySurvivesLeaderSignedCorrectionAndActualAuditWrites() throws Exception {
        var target=org.springframework.test.util.AopTestUtils.getUltimateTargetObject(service);
        var owners=(cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffOwnerResolver)ReflectionTestUtils.getField(target,"handoffOwners");
        org.mockito.Mockito.doReturn(new cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffOwnerResolver.SubmissionIdentity("MES_EMPLOYEE_PROFILE",2001L,1L)).when(owners).submissionIdentity(any());
        when(owners.profileProductionLeader(any())).thenReturn(3001L);
        var reviews=mock(MesProcessPoolSubmissionReviewMapper.class);
        when(reviews.selectLatestByEventIdForUpdate(176L)).thenReturn(cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO.builder()
                .id(176L).eventId(176L).leaderUserId(3001L).leaderType("PRODUCTION").reviewStatus("REJECTED").build());
        var handoff=mock(cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffService.class);
        ReflectionTestUtils.setField(target,"submissionReviews",reviews);ReflectionTestUtils.setField(target,"handoffService",handoff);
        var original=JsonUtils.parseTree(jdbc.queryForObject("SELECT raw_payload FROM "+EVENT+" WHERE id=176",String.class));
        ((ObjectNode)original).put("actualEmployeeIdentityDomain","MES_EMPLOYEE_PROFILE").put("activeOrderId",413);
        jdbc.update("UPDATE "+EVENT+" SET signature_user_id=2001, raw_payload=? WHERE id=176",original.toString());
        long revisionId=service.correct(command());
        assertEquals(8001L,jdbc.queryForObject("SELECT signature_id FROM "+EVENT+" WHERE id=176",Long.class));
        assertEquals(2001L,jdbc.queryForObject("SELECT signature_user_id FROM "+EVENT+" WHERE id=176",Long.class));
        var revisionPayload=JsonUtils.parseTree(jdbc.queryForObject("SELECT after_payload FROM "+REVISION+" WHERE id=?",String.class,revisionId));
        assertEquals("MES_EMPLOYEE_PROFILE",revisionPayload.path("actualEmployeeIdentityDomain").asText());assertEquals(176L,revisionPayload.path("supersededReviewId").asLong());
        assertEquals(3001L,jdbc.queryForObject("SELECT revision_signature_user_id FROM "+REVISION+" WHERE id=?",Long.class,revisionId));
        assertEquals(9102L,jdbc.queryForObject("SELECT revision_signature_id FROM "+REVISION+" WHERE id=?",Long.class,revisionId));
        assertEquals(1,count(SIGNATURE));assertEquals(1,count("gxp_audit_event"));assertTrue(count(DIFF)>0);
        verify(handoff).completeProfileLeaderCorrection(176L,176L,revisionId,3001L);verifyNoInteractions(scope);
    }

    @Test
    void correctionPersistsCompleteBeforeAfterAndVerifiedSignature() {
        long revisionId = service.correct(command());
        assertEquals(1, count("gxp_audit_event"), "Formal correction must append one unified business fact");
        assertEquals(1, count(REVISION));
        assertTrue(count(DIFF) >= 2);
        JsonNode before = state("before_state_json");
        JsonNode after = state("after_state_json");
        assertEquals("4", before.path("event").path("reportOutputQuantity").asText());
        assertEquals("6", after.path("event").path("reportOutputQuantity").asText());
        assertEquals("4", before.path("feedback").path("qualifiedQuantity").asText());
        assertEquals("6", after.path("feedback").path("qualifiedQuantity").asText());
        assertEquals("4", before.path("materials").get(0).path("outputQuantity").asText());
        assertEquals("6", after.path("materials").get(0).path("outputQuantity").asText());
        assertEquals("original", after.path("materials").get(0).path("materialName").asText());
        assertEquals("4", before.path("outputFragment").path("totalQuantity").asText());
        assertEquals("6", after.path("outputFragment").path("totalQuantity").asText());
        assertTrue(before.path("revision").isNull());
        assertEquals(revisionId, after.path("revision").path("id").asLong());
        assertEquals(count(DIFF), after.path("revisionDiffs").size());
        assertEquals("9102", jdbc.queryForObject("SELECT signature_record_id FROM gxp_audit_event", String.class));
        assertEquals(signatures.selectById(9102L).getContentHash(),
                jdbc.queryForObject("SELECT signature_content_hash FROM gxp_audit_event", String.class));
        assertEquals("USER", jdbc.queryForObject("SELECT reason_source FROM gxp_audit_event", String.class));
        assertEquals("correct measured quantity", jdbc.queryForObject("SELECT reason FROM gxp_audit_event", String.class));
        assertEquals(OPERATION, jdbc.queryForObject("SELECT operation_id FROM gxp_audit_event", String.class));
        assertTrue(jdbc.queryForObject("SELECT idempotency_key FROM gxp_audit_event", String.class).length() <= 96);
        assertTrue(jdbc.queryForList("SELECT target_type FROM gxp_audit_event_relation", String.class)
                .containsAll(List.of("PROCESS_POOL_EVENT", "WORK_ORDER", "FEEDBACK", "ACTIVE_ORDER", "REVISION", "SIGNATURE")));
        assertEquals(2L, watermark());
    }

    @Test
    void relationFailureRollsBackBusinessSignatureRevisionAndLedger() {
        jdbc.execute("CREATE TRIGGER fail_relation BEFORE INSERT ON gxp_audit_event_relation FOR EACH ROW CALL '"
                + FailAfterWrites.class.getName() + "'");
        assertThrows(RuntimeException.class, () -> service.correct(command()));
        assertTrue(FAILURE_SAW_WRITES.get(), "Failure must observe actual persisted business/signature/revision/audit rows");
        assertUnchanged();
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "tenant", "actor", "action", "source", "content", "evidence"})
    void invalidStoredSignatureRejectsBeforeBusinessMutation(String defect) {
        signatureDefect = defect;
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class, () -> service.correct(command()));
        assertUnchanged();
    }

    @Test
    void originalReasonScopeAndNoDifferenceGuardsRemainEffective() {
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> service.correct(command().setChangeReason("  ")));
        doThrow(new IllegalStateException("scope denied")).when(scope).assertCanAccessEmployee(3001L, "PRODUCTION", 2001L);
        assertThrows(IllegalStateException.class, () -> service.correct(command()));
        reset(scope);
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> service.correct(command().setOutputQuantity(new BigDecimal("4")).setMaterialDetails(null)));
        verifyNoInteractions(signer);
        assertUnchanged();
    }

    private MesProcessPoolProductionReportCorrectionCommand command() {
        return new MesProcessPoolProductionReportCorrectionCommand().setEventId(176L).setActorUserId(3001L)
                .setOutputQuantity(new BigDecimal("6")).setLossDetails(List.of()).setDeviceParameterReadings(List.of())
                .setMaterialDetails(List.of(new MesProcessPoolProductionReportCorrectionCommand.MaterialDetailCommand()
                        .setMaterialId(3401L).setOutputQuantity(new BigDecimal("6")).setLossQuantity(BigDecimal.ZERO)
                        .setLossDetails(List.of()).setDeviceParameterReadings(List.of())))
                .setChangeReason(" correct measured quantity ").setSignaturePassword("isolated-test-secret");
    }

    private MesProBatchRecordExecutionFieldAuditSignatureResult insertSignature(MesProBatchRecordExecutionFieldAuditSignatureCommand input) {
        assertEquals("isolated-test-secret", input.getPassword());
        String action = "action".equals(signatureDefect) ? "APPROVE" : "FIELD_CHANGE";
        String challenge = "source".equals(signatureDefect) ? "f".repeat(64) : input.getSignatureChallengeHash();
        String subject = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(0L, action,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, challenge);
        String version = DigestUtil.sha256Hex(subject);
        long actor = "actor".equals(signatureDefect) ? 3002L : 3001L;
        var adapter = new MesBatchRecordSignatureSubjectAdapter();
        var snapshot = adapter.loadAndAuthorize(new SignatureSubjectCommand(actor, "MES", action,
                "MES_BATCH_RECORD", subject, version, input.getReasonText()));
        String canonical = JsonUtils.toJsonString(sorted(JsonUtils.parseTree(snapshot.canonicalContentJson())));
        var definition = adapter.supportedActions().stream().filter(a -> action.equals(a.actionCode())).findFirst().orElseThrow();
        var time = LocalDateTime.of(2026, 9, 29, 7, 0);
        var record = ElectronicSignatureRecordDO.builder().id(9102L).moduleCode("MES").actionCode(action)
                .subjectType("MES_BATCH_RECORD").subjectId(subject).subjectVersion(version).actorId(actor)
                .meaningCode(definition.meaningCode()).meaningLabel(definition.meaningLabel()).reason(input.getReasonText())
                .signedAt(time).timeEvidenceId("SERVER_CLOCK:" + time).authenticationMethod("SESSION_PLUS_PASSWORD")
                .canonicalContentJson(canonical).contentHash(DigestUtil.sha256Hex(canonical)).algorithm("SHA-256")
                .keyVersion("system-local-v1").policyVersion(definition.policyVersion()).verificationStatus("VALID").build();
        record.setTenantId(1L);
        record.setEvidenceHash(DigestUtil.sha256Hex(String.join("|", "1", Long.toString(actor), "MES", action,
                "MES_BATCH_RECORD", subject, version, record.getMeaningCode(), record.getMeaningLabel(), record.getReason(),
                time.toString(), record.getTimeEvidenceId(), "SESSION_PLUS_PASSWORD", record.getContentHash(),
                "", "", "", "", "", "SHA-256", "system-local-v1", record.getPolicyVersion(), "VALID")));
        if ("tenant".equals(signatureDefect)) record.setTenantId(2L);
        if ("content".equals(signatureDefect)) record.setCanonicalContentJson("{\"altered\":true}");
        if ("evidence".equals(signatureDefect)) record.setEvidenceHash("0".repeat(64));
        if (!"missing".equals(signatureDefect)) signatures.insert(record);
        if ("valid".equals(signatureDefect)) assertEquals("VALID", signatureQuery.verifyEvidence(9102L).verificationStatus());
        return new MesProBatchRecordExecutionFieldAuditSignatureResult().setSignatureId(9102L).setActorId(3001L).setSignedAt(time);
    }

    private static JsonNode sorted(JsonNode node) {
        if (!node.isObject()) return node;
        ObjectNode result = JsonUtils.getObjectMapper().createObjectNode();
        TreeSet<String> keys = new TreeSet<>();
        node.fieldNames().forEachRemaining(keys::add);
        keys.forEach(key -> result.set(key, sorted(node.get(key))));
        return result;
    }

    private void assertUnchanged() {
        assertEquals(0, count(REVISION));
        assertEquals(0, count(DIFF));
        assertEquals(0, count(SIGNATURE));
        assertEquals(0, count("gxp_audit_event"));
        assertEquals(0, count("gxp_audit_event_relation"));
        assertEquals(1L, watermark());
        for (String query : List.of("SELECT report_output_quantity FROM " + EVENT,
                "SELECT qualified_quantity FROM mes_pro_feedback", "SELECT output_quantity FROM mes_pro_feedback_material",
                "SELECT total_quantity FROM " + FRAGMENT)) {
            assertEquals(0, new BigDecimal("4").compareTo(jdbc.queryForObject(query, BigDecimal.class)));
        }
    }

    public static class FailAfterWrites implements Trigger {
        @Override public void fire(Connection connection, Object[] oldRow, Object[] newRow) throws SQLException {
            try (var statement = connection.createStatement(); var rows = statement.executeQuery(
                    "SELECT (SELECT COUNT(*) FROM gxp_audit_event),(SELECT COUNT(*) FROM " + REVISION + "),"
                            + "(SELECT COUNT(*) FROM " + DIFF + "),(SELECT COUNT(*) FROM " + SIGNATURE + "),"
                            + "(SELECT report_output_quantity FROM " + EVENT + " WHERE id=176),"
                            + "(SELECT output_quantity FROM mes_pro_feedback_material WHERE id=6101),"
                            + "(SELECT total_quantity FROM " + FRAGMENT + " WHERE id=6001)")) {
                rows.next();
                FAILURE_SAW_WRITES.set(rows.getInt(1) == 1 && rows.getInt(2) == 1 && rows.getInt(3) >= 2
                        && rows.getInt(4) == 1 && rows.getInt(5) == 6 && rows.getInt(6) == 6 && rows.getInt(7) == 6);
            }
            throw new SQLException("Injected audit relation failure after actual correction writes");
        }
    }

    @Intercepts(@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}))
    public static class MysqlBitsForH2 implements Interceptor {
        @Override public Object intercept(Invocation invocation) throws Throwable {
            var bound = ((StatementHandler) invocation.getTarget()).getBoundSql();
            ReflectionTestUtils.setField(bound, "sql", bound.getSql().replace("b'0'", "0").replace("b'1'", "1"));
            return invocation.proceed();
        }
    }

    private static Object tx(Object target, DataSourceTransactionManager manager) {
        var proxy = new ProxyFactory(target);
        proxy.addAdvice(new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource()));
        return proxy.getProxy();
    }
    private int count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); }
    private long watermark() { return jdbc.queryForObject("SELECT next_ledger_sequence FROM gxp_audit_ledger_sequence", Long.class); }
    private JsonNode state(String column) { return JsonUtils.parseTree(jdbc.queryForObject("SELECT " + column + " FROM gxp_audit_event", String.class)); }
    private void createTable(Class<?> row) {
        List<String> columns = new ArrayList<>();
        for (Class<?> type = row; type != Object.class; type = type.getSuperclass()) {
            for (var field : type.getDeclaredFields()) {
                var mapping = field.getAnnotation(TableField.class);
                if (Modifier.isStatic(field.getModifiers()) || (mapping != null && !mapping.exist())) continue;
                String name = mapping != null && !mapping.value().isBlank() ? mapping.value()
                        : field.getName().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
                String sqlType = field.getType() == Long.class ? "BIGINT" : field.getType() == Integer.class ? "INT"
                        : field.getType() == Boolean.class ? "INT DEFAULT 0" : field.getType() == BigDecimal.class ? "DECIMAL(24,8)"
                        : field.getType() == LocalDateTime.class ? "TIMESTAMP" : "VARCHAR(1000000)";
                columns.add("`" + name + "` " + (name.equals("id") ? "BIGINT AUTO_INCREMENT PRIMARY KEY" : sqlType));
            }
        }
        jdbc.execute("CREATE TABLE " + row.getAnnotation(TableName.class).value() + "(" + String.join(",", columns) + ")");
    }
}
