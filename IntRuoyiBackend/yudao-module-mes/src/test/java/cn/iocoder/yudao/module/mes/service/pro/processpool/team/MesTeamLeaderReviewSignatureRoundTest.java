package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.service.file.DccElectronicSignatureAuthorizationService;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchRecordSignatureSubjectAdapter;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHasher;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSignatureResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.MesReleaseAffectedStateCollector;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureService;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureServiceImpl;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Real review/MES/unified services and persisted signatures/reviews in one physical H2 transaction.
 * Directory, formal correction discovery, aggregation and audit policy are isolated boundaries. */
class MesTeamLeaderReviewSignatureRoundTest {
    private JdbcTemplate jdbc;
    private DataSourceTransactionManager transactions;
    private MesTeamLeaderSubmissionReviewService service;
    private ElectronicSignatureRecordMapper signatures;
    private MesProcessPoolSubmissionReviewMapper reviews;
    private MesProProcessPoolEventMapper events;
    private MesProProcessPoolEventRevisionMapper revisions;
    private GxpAuditService audit;
    private cn.iocoder.yudao.module.mes.service.pro.handoff.MesSignedReturnCorrectionResolver correctionDiscovery;
    private MesProProcessPoolEventDO event;
    private final java.util.Map<Long, MesProProcessPoolEventDO> displayedMembers = new java.util.LinkedHashMap<>();

    @BeforeEach
    void fixture() throws Exception {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:review_round_" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        // Reuse the unified module's actual test DDL, including the real idempotency unique key/capacities.
        String signatureDdl = Files.readString(Path.of("../yudao-module-signature/src/test/resources/sql/create_tables.sql"));
        jdbc.execute(signatureDdl.substring(0, signatureDdl.indexOf(';') + 1));
        // Review columns checked against 20260730 team_leader / 20260803 signature and simulation migrations.
        jdbc.execute("""
                CREATE TABLE mes_pro_process_pool_submission_review (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL DEFAULT 1,
                  review_round INT NOT NULL DEFAULT 0, source_revision_id BIGINT, superseded_review_id BIGINT,
                  event_id BIGINT NOT NULL, leader_user_id BIGINT NOT NULL, leader_type VARCHAR(32),
                  review_status VARCHAR(32) NOT NULL, review_remark VARCHAR(1000), reviewed_at TIMESTAMP,
                  review_signature_id BIGINT, review_signature_user_id BIGINT, review_signature_snapshot_json VARCHAR(4000),
                  simulated BOOLEAN DEFAULT FALSE, simulation_stage VARCHAR(64), simulation_run_id VARCHAR(128),
                  creator VARCHAR(64) DEFAULT '', updater VARCHAR(64) DEFAULT '',
                  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  deleted BOOLEAN DEFAULT FALSE)
                """);
        var config = new MybatisConfiguration();
        config.setMapUnderscoreToCamelCase(true);
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setConfiguration(config);
        factory.setGlobalConfig(new com.baomidou.mybatisplus.core.config.GlobalConfig()
                .setDbConfig(new com.baomidou.mybatisplus.core.config.GlobalConfig.DbConfig()
                        .setIdType(com.baomidou.mybatisplus.annotation.IdType.AUTO))
                .setMetaObjectHandler(new cn.iocoder.yudao.framework.mybatis.core.handler.DefaultDBFieldHandler()));
        var sqlFactory = Objects.requireNonNull(factory.getObject());
        sqlFactory.getConfiguration().addMapper(ElectronicSignatureRecordMapper.class);
        sqlFactory.getConfiguration().addMapper(MesProcessPoolSubmissionReviewMapper.class);
        var session = new SqlSessionTemplate(sqlFactory);
        signatures = session.getMapper(ElectronicSignatureRecordMapper.class);
        reviews = session.getMapper(MesProcessPoolSubmissionReviewMapper.class);
        transactions = new DataSourceTransactionManager(dataSource);
        var credentials = mock(AdminUserApi.class);
        audit = mock(GxpAuditService.class);
        var unified = new ElectronicSignatureServiceImpl();
        ReflectionTestUtils.setField(unified, "adminUserApi", credentials);
        ReflectionTestUtils.setField(unified, "signatureRecordMapper", signatures);
        ReflectionTestUtils.setField(unified, "gxpAuditService", audit);
        ReflectionTestUtils.setField(unified, "subjectAdapters", List.of(new MesBatchRecordSignatureSubjectAdapter()));
        var mes = new MesProBatchRecordExecutionSignatureService();
        var users = mock(AdminUserService.class);
        when(users.getUser(3001L)).thenReturn(AdminUserDO.builder().id(3001L)
                .username("reviewer").nickname("PQC reviewer").build());
        var authorization = mock(DccElectronicSignatureAuthorizationService.class);
        when(authorization.isElectronicSignatureEnabled(3001L)).thenReturn(true);
        ReflectionTestUtils.setField(mes, "adminUserService", users);
        ReflectionTestUtils.setField(mes, "authorizationService", authorization);
        ReflectionTestUtils.setField(mes, "permissionService", mock(PermissionService.class));
        ReflectionTestUtils.setField(mes, "adminUserApi", credentials);
        ReflectionTestUtils.setField(mes, "electronicSignatureService", (ElectronicSignatureService) transactional(unified));
        events = mock(MesProProcessPoolEventMapper.class);
        revisions = mock(MesProProcessPoolEventRevisionMapper.class);
        var target = new MesTeamLeaderSubmissionReviewServiceImpl(mock(MesTeamLeaderScopeService.class), events,
                reviews, mock(MesPqcProcessInspectionAggregationService.class));
        // Actual open freeze authority; persistence reads are boundaries for this signature/transaction fixture.
        var openFreeze = new cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(openFreeze,"reviewMapper",org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper.class));
        var openOrders = org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper.class);
        org.mockito.Mockito.lenient().when(openOrders.selectByIdForUpdate(org.mockito.ArgumentMatchers.anyLong())).thenAnswer(call ->
                cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO.builder().id(call.getArgument(0)).temporaryFrozen(false).build());
        org.springframework.test.util.ReflectionTestUtils.setField(openFreeze,"workOrderMapper",openOrders);
        org.springframework.test.util.ReflectionTestUtils.setField(target,"nonconformanceReviewService",openFreeze);
        { org.springframework.test.util.ReflectionTestUtils.setField(target, "handoffService", org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffService.class)); }
        correctionDiscovery=mock(cn.iocoder.yudao.module.mes.service.pro.handoff.MesSignedReturnCorrectionResolver.class);
        ReflectionTestUtils.setField(target,"returnCorrectionResolver",correctionDiscovery);
        var tasks = mock(MesPqcInspectionTaskMapper.class);
        when(tasks.selectById(5101L)).thenReturn(MesPqcInspectionTaskDO.builder().id(5101L).activeOrderId(8101L).build());
        when(tasks.selectById(5102L)).thenReturn(MesPqcInspectionTaskDO.builder().id(5102L).activeOrderId(8101L).build());
        ReflectionTestUtils.setField(target, "signatureService", transactional(mes));
        ReflectionTestUtils.setField(target, "revisionMapper", revisions);
        ReflectionTestUtils.setField(target, "pqcTaskMapper", tasks);
        ReflectionTestUtils.setField(target, "gxpAuditService", audit);
        ReflectionTestUtils.setField(target, "affectedStateCollector", mock(MesReleaseAffectedStateCollector.class));
        service = (MesTeamLeaderSubmissionReviewService) transactional(target);
        event = event(1001L, 5101L, "{\"outputQuantity\":10}");
        displayedMembers.clear();
        displayedMembers.put(event.getId(), event);
        when(events.selectByIdForUpdate(1001L)).thenReturn(event);
        TenantContextHolder.setTenantId(1L);
        var login = new LoginUser();
        login.setId(3001L); login.setTenantId(1L);
        login.setInfo(Map.of("username", "reviewer", LoginUser.INFO_KEY_NICKNAME, "PQC reviewer"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login, null));
    }

    @AfterEach
    void close() throws Exception {
        SecurityContextHolder.clearContext(); TenantContextHolder.clear();
        if (jdbc != null) {
            try (var connection = Objects.requireNonNull(jdbc.getDataSource()).getConnection();
                 var statement = connection.createStatement()) { statement.execute("SHUTDOWN"); }
        }
    }

    @Test
    void originalReviewerCanApproveSignedCorrectionWithoutIdempotencyCollision() {
        Long rejected = service.reviewSubmission(request("REJECTED"));
        Long firstSignature = reviews.selectById(rejected).getReviewSignatureId();
        correct(event, rejected, 8001L);
        var approvedRequest = request("APPROVED");
        Long approved = assertDoesNotThrow(() -> service.reviewSubmission(approvedRequest));
        assertNotEquals(firstSignature, reviews.selectById(approved).getReviewSignatureId());
        assertRound(approved, "APPROVED", 8001L, rejected);
        assertEquals(approved, service.reviewSubmission(approvedRequest));
        assertCounts(2, 2);
    }

    @Test
    void repeatedRejectionThenApprovalSignsEachExactRoundAndReplaysWithoutNewRows() {
        var firstRequest = request("REJECTED");
        Long first = service.reviewSubmission(firstRequest);
        Long firstSignature = reviews.selectById(first).getReviewSignatureId();
        assertEquals(first, service.reviewSubmission(firstRequest));
        assertCounts(1, 1);
        correct(event, first, 8001L);
        var secondRequest = request("REJECTED");
        Long second = service.reviewSubmission(secondRequest);
        assertNotEquals(firstSignature, reviews.selectById(second).getReviewSignatureId(),
                "A correction changes the signed content even when the decision stays REJECTED");
        assertRound(second, "REJECTED", 8001L, first);
        assertEquals(second, service.reviewSubmission(secondRequest));
        assertCounts(2, 2);
        correct(event, second, 8002L);
        var thirdRequest = request("APPROVED");
        Long third = service.reviewSubmission(thirdRequest);
        assertRound(third, "APPROVED", 8002L, second);
        assertEquals(third, service.reviewSubmission(thirdRequest));
        assertCounts(3, 3);
        assertEquals(3, jdbc.queryForObject("SELECT COUNT(DISTINCT content_hash) FROM system_electronic_signature", Integer.class));
        assertEquals(firstSignature, reviews.selectById(first).getReviewSignatureId());
    }

    @Test
    void secondCorrectedMembersFailureRollsBackActualUnifiedSignaturesAndReviews() {
        event.setRawPayload("{\"pqcSubmissionGroupId\":\"g\",\"outputQuantity\":10}");
        var sibling = event(1002L, 5102L, event.getRawPayload());
        displayedMembers.put(sibling.getId(), sibling);
        when(events.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(event, sibling));
        service.reviewSubmission(request("REJECTED"));
        var originals = jdbc.queryForList("SELECT * FROM system_electronic_signature ORDER BY id");
        var oldReviews = jdbc.queryForList("SELECT * FROM mes_pro_process_pool_submission_review ORDER BY id");
        correct(event, reviews.selectLatestByEventIdForUpdate(1001L).getId(), 8001L);
        correct(sibling, reviews.selectLatestByEventIdForUpdate(1002L).getId(), 8002L);
        AtomicBoolean sawBothNewSignatures = new AtomicBoolean();
        doAnswer(invocation -> {
            GxpAuditCommand command = invocation.getArgument(0);
            if ("mes.pqc.review.reject".equals(command.getOperationId())
                    && "MES_PROCESS_POOL_EVENT:1002".equals(command.getSubjectId())) {
                sawBothNewSignatures.set(count("system_electronic_signature") == 4
                        && count("mes_pro_process_pool_submission_review") == 4);
                throw new IllegalStateException("second corrected member audit failed");
            }
            return null;
        }).when(audit).append(any());
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> service.reviewSubmission(request("REJECTED")));
        assertEquals("second corrected member audit failed", failure.getMessage());
        assertTrue(sawBothNewSignatures.get(), "The injected failure must observe two newly persisted round signatures");
        assertEquals(originals, jdbc.queryForList("SELECT * FROM system_electronic_signature ORDER BY id"));
        assertEquals(oldReviews, jdbc.queryForList("SELECT * FROM mes_pro_process_pool_submission_review ORDER BY id"));
    }

    private void assertRound(Long reviewId, String status, Long revisionId, Long superseded) {
        MesProcessPoolSubmissionReviewDO review = reviews.selectById(reviewId);
        ElectronicSignatureRecordDO signature = signatures.selectById(review.getReviewSignatureId());
        var content = JsonUtils.parseTree(signature.getCanonicalContentJson());
        assertEquals(status, content.path("approvalResult").asText());
        assertEquals(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(event.getRawPayload()),
                content.path("cellValuesHash").asText());
        var round = JsonUtils.parseTree(content.path("reviewSourceName").asText());
        assertEquals(1001L, round.path("eventId").asLong());
        assertEquals(revisionId.longValue(), round.path("revisionId").asLong());
        assertEquals(superseded.longValue(), round.path("supersededReviewId").asLong());
        assertEquals(revisionId + 10000L, round.path("revisionSignatureId").asLong());
        assertEquals(3001L, signature.getActorId());
        assertTrue(signature.getIdempotencyKey().length() <= 191);
        String[] subject = new String(Base64.getUrlDecoder().decode(signature.getSubjectId()), StandardCharsets.UTF_8).split("\n", -1);
        assertEquals(17, subject.length, "SIGN01 keeps its existing subject protocol");
        assertEquals("PROCESS_POOL_EVENT", subject[9]); assertEquals("1001", subject[10]);
    }

    private void correct(MesProProcessPoolEventDO source, Long previous, Long revisionId) {
        var payload = new java.util.LinkedHashMap<String, Object>();
        if (source.getRawPayload().contains("pqcSubmissionGroupId")) payload.put("pqcSubmissionGroupId", "g");
        payload.put("outputQuantity", revisionId); payload.put("supersededReviewId", previous);
        source.setRawPayload(JsonUtils.toJsonString(payload));
        var signature = new MesProBatchRecordExecutionFieldAuditSignatureResult()
                .setSignatureId(revisionId + 10000L).setActorId(2001L).setSignedAt(LocalDateTime.of(2026, 10, 3, 10, 0));
        var correction = MesProProcessPoolEventRevisionDO.builder().id(revisionId).eventId(source.getId())
                .revisionStatus("EFFECTIVE").afterPayload(source.getRawPayload()).modifiedByUserId(2001L)
                .revisionSignatureId(signature.getSignatureId()).revisionSignatureUserId(2001L)
                .revisionSignatureSnapshot(JsonUtils.toJsonString(signature)).build();
        correction.setTenantId(1L);
        when(revisions.selectListByEventIdForUpdate(source.getId())).thenReturn(List.of(correction));
        var discovery=correctionDiscovery;
        org.mockito.Mockito.lenient().when(discovery.find(org.mockito.ArgumentMatchers.eq(source), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    MesProcessPoolSubmissionReviewDO previousReview=invocation.getArgument(1);
                    return previousReview!=null&&"REJECTED".equals(previousReview.getReviewStatus())
                        &&java.util.Objects.equals(previousReview.getId(),cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(correction.getAfterPayload()).path("supersededReviewId").longValue())
                        &&java.util.Objects.equals(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(source.getRawPayload()),MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(correction.getAfterPayload())) ? correction : null;
                });
    }

    private MesProProcessPoolEventDO event(Long id, Long taskId, String payload) {
        var source = MesProProcessPoolEventDO.builder().id(id).eventType("PQC_INSPECTION")
                .actualEmployeeId(2001L).workOrderId(9001L).routeId(9002L).qaProcessId(9003L)
                .feedbackSourceType("MES_PQC_INSPECTION_TASK").feedbackSourceId(taskId).rawPayload(payload).build();
        source.setTenantId(1L); return source;
    }
    private MesTeamLeaderSubmissionReviewReqBO request(String status) {
        return MesTeamLeaderSubmissionReviewReqBO.builder().eventId(1001L).leaderUserId(3001L)
                .leaderType("PQC").reviewStatus(status).reviewRemark("REJECTED".equals(status) ? "检测数据需补正" : "已核对")
                .expectedReviews(displayedMembers.values().stream().map(member -> {
                    var latestReview = reviews.selectLatestByEventIdForUpdate(member.getId());
                    var effectiveRevisions = revisions.selectListByEventIdForUpdate(member.getId());
                    return new MesSubmissionReviewExpectedContext().setEventId(member.getId())
                            .setPayloadHash(MesProBatchRecordExecutionFieldAuditHasher.sha256(member.getRawPayload()))
                            .setRevisionId(effectiveRevisions.isEmpty() ? 0L : effectiveRevisions.get(0).getId())
                            .setReviewId(latestReview == null ? 0L : latestReview.getId())
                            .setReviewRound(latestReview == null ? 0 : latestReview.getReviewRound());
                }).toList())
                .signaturePassword("fixture-credential").build();
    }
    private Object transactional(Object target) {
        var proxy = new ProxyFactory(target); proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
        return proxy.getProxy();
    }
    private int count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); }
    private void assertCounts(int signatureCount, int reviewCount) {
        assertEquals(signatureCount, count("system_electronic_signature"));
        assertEquals(reviewCount, count("mes_pro_process_pool_submission_review"));
    }
}
