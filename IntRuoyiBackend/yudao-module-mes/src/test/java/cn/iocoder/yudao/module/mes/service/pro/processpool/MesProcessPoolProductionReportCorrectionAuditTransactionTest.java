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
import static org.mockito.ArgumentMatchers.anyCollection;
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
    private SqlSessionTemplate session;
    private DataSourceTransactionManager manager;
    private boolean roundTrip;
    private org.springframework.context.support.GenericApplicationContext batchContext;
    private Object previousBeans;
    private Object previousContext;

    @BeforeEach
    void fixture() throws Exception {
        previousBeans = ReflectionTestUtils.getField(cn.hutool.extra.spring.SpringUtil.class, "beanFactory");
        previousContext = ReflectionTestUtils.getField(com.baomidou.mybatisplus.extension.spi.CompatibleHelper.getCompatibleSet().getClass(), "applicationContext");
        batchContext = new org.springframework.context.support.GenericApplicationContext();
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:production_correction_" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        for (Class<?> row : List.of(MesProProcessPoolEventDO.class, MesProProcessPoolQuantityFragmentDO.class,
                MesProFeedbackDO.class, MesProFeedbackMaterialDO.class, MesProProcessPoolEventRevisionDO.class,
                MesProProcessPoolEventRevisionDiffDO.class, ElectronicSignatureRecordDO.class, GxpAuditEventDO.class,
                GxpAuditEventRelationDO.class, GxpAuditLedgerSequenceDO.class, GxpAuditPolicyActivationDO.class,
                GxpAuditPolicyOperationDO.class)) createTable(row);
        jdbc.execute("ALTER TABLE mes_pro_feedback ADD COLUMN tenant_id BIGINT");
        assertEquals(SIGNATURE, ElectronicSignatureRecordDO.class.getAnnotation(TableName.class).value());
        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addInterceptor(new MysqlBitsForH2());
        var tenantPlugin=new com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor();
        new cn.iocoder.yudao.framework.tenant.config.YudaoTenantAutoConfiguration().tenantLineInnerInterceptor(
                new cn.iocoder.yudao.framework.tenant.config.TenantProperties(),tenantPlugin);
        configuration.addInterceptor(tenantPlugin);
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setConfiguration(configuration);
        factory.setApplicationContext(batchContext);
        factory.setGlobalConfig(new com.baomidou.mybatisplus.core.config.GlobalConfig().setMetaObjectHandler(new cn.iocoder.yudao.framework.mybatis.core.handler.DefaultDBFieldHandler()));
        session = new SqlSessionTemplate(Objects.requireNonNull(factory.getObject()));
        batchContext.getBeanFactory().registerSingleton("dataSource", dataSource);
        batchContext.getBeanFactory().registerSingleton("sqlSessionFactory", session.getSqlSessionFactory());
        batchContext.refresh();
        new cn.hutool.extra.spring.SpringUtil().postProcessBeanFactory(batchContext.getBeanFactory());
        List.of(MesProProcessPoolEventMapper.class, MesProProcessPoolQuantityFragmentMapper.class,
                MesProFeedbackMapper.class, MesProFeedbackMaterialMapper.class, MesProProcessPoolEventRevisionMapper.class,
                MesProProcessPoolEventRevisionDiffMapper.class, ElectronicSignatureRecordMapper.class,
                GxpAuditEventMapper.class, GxpAuditEventRelationMapper.class, GxpAuditLedgerSequenceMapper.class,
                GxpAuditPolicyActivationMapper.class, GxpAuditPolicyOperationMapper.class).forEach(configuration::addMapper);
        manager = new DataSourceTransactionManager(dataSource);
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
        ReflectionTestUtils.setField(target,"submissionReviews",mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper.class));
        ReflectionTestUtils.setField(target,"sharedReportGuard",MesSharedProductionReportCorrectionGuardTest.openFixture(20L,30L));
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
                + "\"activeOrderId\":413,\"fieldValues\":{\"OUTPUT_QUANTITY\":4,\"SCRAP_QUANTITY\":0},"
                + "\"materialDetails\":[{\"materialId\":3401,\"materialCode\":\"M-1\",\"materialName\":\"original\","
                + "\"outputQuantity\":4,\"lossQuantity\":0,\"lossDetails\":[],\"deviceParameterReadings\":[]}]}";
        jdbc.update("INSERT INTO " + EVENT + "(id,tenant_id,pool_id,event_type,work_order_id,route_id,route_process_id,"
                + "process_id,actual_employee_id,feedback_source_type,feedback_source_id,raw_payload,signature_id,"
                + "report_output_quantity,report_allocated_quantity,report_unallocated_quantity)"
                + " VALUES(176,1,100, 'PRODUCTION_SUBMIT',20,30,31,40,2001,'MES_PRO_FEEDBACK',5101,?,8001,4,0,4)", payload);
        jdbc.update("INSERT INTO " + FRAGMENT + "(id,tenant_id,event_id,source_quantity_type,total_quantity,"
                + "allocated_quantity,available_quantity) VALUES(6001,1,176,'OUTPUT',4,0,4)");
        jdbc.update("INSERT INTO mes_pro_feedback(id,tenant_id,work_order_id,route_id,process_id,feedback_quantity,"
                + "qualified_quantity,unqualified_quantity) VALUES(5101,1,20,30,40,4,4,0)");
        jdbc.update("INSERT INTO mes_pro_feedback_material(id,tenant_id,feedback_id,active_order_id,work_order_id,"
                + "material_id,material_code,material_name,output_quantity,loss_quantity,loss_details_json,"
                + "device_parameter_readings_json) VALUES(6101,1,5101,101,20,3401,'M-1','original',4,0,'[]','[]')");
        FAILURE_SAW_WRITES.set(false);
    }

    @AfterEach
    void close() throws SQLException {
        if (batchContext != null) batchContext.close();
        ReflectionTestUtils.setField(cn.hutool.extra.spring.SpringUtil.class, "beanFactory", previousBeans);
        com.baomidou.mybatisplus.extension.spi.CompatibleHelper.getCompatibleSet().setContext(previousContext);
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

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"SYSTEM_USER,APPROVED","SYSTEM_USER,REJECTED","MES_EMPLOYEE_PROFILE,APPROVED","MES_EMPLOYEE_PROFILE,REJECTED"})
    void rejectionSignedCorrectionTimelineAndNextDecisionUseSameFormalContext(String domain, String nextDecision) throws Exception {
        roundTrip=true;
        var target=org.springframework.test.util.AopTestUtils.getUltimateTargetObject(service);
        var config=session.getConfiguration();
        for(var row:List.of(cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO.class,
                cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationStateDO.class,
                cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO.class,
                cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.MesActiveOrderHandoffTaskDO.class))createTable(row);
        List.of(MesProcessPoolSubmissionReviewMapper.class,MesProcessPoolReportAllocationStateMapper.class,
                MesProcessPoolReportAllocationMapper.class,cn.iocoder.yudao.module.mes.dal.mysql.pro.handoff.MesActiveOrderHandoffTaskMapper.class).forEach(config::addMapper);
        var reviews=session.getMapper(MesProcessPoolSubmissionReviewMapper.class);
        var events=session.getMapper(MesProProcessPoolEventMapper.class);
        var revisions=session.getMapper(MesProProcessPoolEventRevisionMapper.class);
        var state=session.getMapper(MesProcessPoolReportAllocationStateMapper.class);
        var allocations=session.getMapper(MesProcessPoolReportAllocationMapper.class);
        var tasks=session.getMapper(cn.iocoder.yudao.module.mes.dal.mysql.pro.handoff.MesActiveOrderHandoffTaskMapper.class);
        var original=JsonUtils.parseTree(events.selectById(176L).getRawPayload());
        long submitter="SYSTEM_USER".equals(domain)?3001L:2001L;
        ((ObjectNode)original).put("signatureIdentityDomain",domain).put("actualEmployeeIdentityDomain",domain);
        jdbc.update("UPDATE "+EVENT+" SET actual_employee_id=?,signature_user_id=?,raw_payload=? WHERE id=176",submitter,submitter,original.toString());
        // The original signature is an established fixture boundary, while correction signature+audit are actual persisted evidence.
        jdbc.update("INSERT INTO "+SIGNATURE+"(id,tenant_id,module_code,action_code,actor_id,verification_status,canonical_content_json) VALUES(8001,1,'MES','PRODUCTION_SUBMIT',?,'VALID',?)",
                submitter,JsonUtils.toJsonString(Map.of("signatureIdentity",Map.of("domain",domain,"signerId",submitter,"tenantId",1))));
        var owners=(cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffOwnerResolver)ReflectionTestUtils.getField(target,"handoffOwners");
        doReturn(new cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffOwnerResolver.SubmissionIdentity(domain,submitter,1L)).when(owners).submissionIdentity(any());
        when(owners.profileProductionLeader(any())).thenReturn(3001L);when(owners.originalActor(any())).thenReturn(3001L);
        var evidence=new MesCorrectionSignatureEvidenceReader(session.getMapper(MesProProcessPoolEventRevisionDiffMapper.class),signatureQuery,session.getMapper(GxpAuditEventMapper.class));
        var resolver=new cn.iocoder.yudao.module.mes.service.pro.handoff.MesSignedReturnCorrectionResolver();
        ReflectionTestUtils.setField(resolver,"revisions",revisions);ReflectionTestUtils.setField(resolver,"tasks",tasks);
        ReflectionTestUtils.setField(resolver,"owners",owners);ReflectionTestUtils.setField(resolver,"correctionEvidence",evidence);
        var commandTarget=roundCommand(events,reviews,state,allocations,resolver);
        var command=(MesReportAllocationCommandService)tx(commandTarget,manager);
        var opened=roundTimeline().getExpectedReviews().get(0);
        Long rejected=command.rejectProductionSubmission(176L,3001L,"correct the measured report","fixture-password",opened);
        assertEquals("REJECTED",reviews.selectById(rejected).getReviewStatus());
        var stale=roundTimeline().getExpectedReviews().get(0);
        ReflectionTestUtils.setField(target,"submissionReviews",reviews);
        var handoff=mock(cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffService.class);
        ReflectionTestUtils.setField(target,"handoffService",handoff);
        org.mockito.stubbing.Answer<Object> schedule=call->{
            Long revisionId=call.getArgument(2);
            var task=new cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.MesActiveOrderHandoffTaskDO()
                    .setId(501L).setActiveOrderId(413L).setWorkOrderId(20L).setRouteProcessId(31L).setTaskType("PRODUCTION_REVIEW")
                    .setSourceType("PROCESS_POOL_EVENT_SIGNED_REVISION").setSourceId(176L).setRoundId(revisionId).setStatus("TODO")
                    .setInitiatedBy(3001L).setCandidateUserSnapshot("3001").setResponsibilitySnapshotJson(JsonUtils.toJsonString(Map.of("correctionOrigin",
                            "SYSTEM_USER".equals(domain)?"OWN_RETURN_CORRECTION":"LEADER_PROFILE_CORRECTION")));
            task.setTenantId(1L);tasks.insert(task);return null;
        };
        doAnswer(schedule).when(handoff).completeProfileLeaderCorrection(any(),any(),any(),any());
        doAnswer(schedule).when(handoff).completeReturnAndScheduleReview(any(),any(),any(),any());
        var own=mock(MesFrontlineReturnCorrectionService.class);
        var context=new MesFrontlineReturnCorrectionService.ReturnContext(events.selectById(176L),reviews.selectById(rejected),413L,0L,"return-task");
        when(own.requireOwnReturned(176L,413L,rejected,0L,3001L,"PRODUCTION")).thenReturn(context);
        doAnswer(call->{Long revisionId=call.getArgument(1);handoff.completeReturnAndScheduleReview(176L,rejected,revisionId,3001L);return new MesFrontlineReturnCorrectionService.CorrectionResult(176L,revisionId,List.of());})
                .when(own).complete(any(),any(),any());
        ReflectionTestUtils.setField(target,"ownReturnService",own);
        Long revisionId="SYSTEM_USER".equals(domain)?service.correctOwnReturned(command(),413L,rejected,0L).revisionId():service.correct(command());
        assertEquals(1L,revisions.selectById(revisionId).getTenantId(),"Persisted revision tenant");
        evidence.require(events.selectById(176L),revisions.selectById(revisionId));
        var refreshed=roundTimeline();
        assertEquals("PENDING",refreshed.getSubmissionReviewStatus());
        assertEquals("REJECTED",refreshed.getExpectedReviews().get(0).getReviewStatus());
        assertEquals(rejected,refreshed.getExpectedReviews().get(0).getReviewId());
        assertEquals(revisionId,refreshed.getExpectedReviews().get(0).getRevisionId());
        int before=jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_process_pool_submission_review",Integer.class);
        assertThrows(IllegalStateException.class,()->command.rejectProductionSubmission(176L,3001L,"stale","fixture-password",stale));
        assertEquals(before,jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_process_pool_submission_review",Integer.class));
        var current=refreshed.getExpectedReviews().get(0);
        if("REJECTED".equals(nextDecision))command.rejectProductionSubmission(176L,3001L,"checked corrected content","fixture-password",current);
        else command.save(MesReportAllocationSaveCommand.builder().eventId(176L).leaderUserId(3001L).leaderType("PRODUCTION")
                .expectedVersion(current.getAllocationVersion()).expectedReview(current).allocationMode("MANUAL").signaturePassword("fixture-password")
                .reason("approve corrected content").allocations(List.of(MesReportAllocationSaveLine.builder().activeOrderId(413L).allocatedQuantity(BigDecimal.ONE).build())).build());
        assertEquals(nextDecision,reviews.selectLatestByEventIdForUpdate(176L).getReviewStatus());
        assertEquals("REJECTED",reviews.selectById(rejected).getReviewStatus());
        assertEquals(1,reviews.selectLatestByEventIdForUpdate(176L).getReviewRound());
    }

    private MesReportAllocationCommandService roundCommand(MesProProcessPoolEventMapper events,MesProcessPoolSubmissionReviewMapper reviews,
            MesProcessPoolReportAllocationStateMapper state,MesProcessPoolReportAllocationMapper allocations,
            cn.iocoder.yudao.module.mes.service.pro.handoff.MesSignedReturnCorrectionResolver resolver) throws Exception {
        var constructor=MesReportAllocationCommandService.class.getConstructors()[0];
        var target=(MesReportAllocationCommandService)constructor.newInstance(Arrays.stream(constructor.getParameterTypes()).map(org.mockito.Mockito::mock).toArray());
        for(var f:target.getClass().getDeclaredFields())if(!Modifier.isStatic(f.getModifiers())&&ReflectionTestUtils.getField(target,f.getName())==null)ReflectionTestUtils.setField(target,f.getName(),mock(f.getType()));
        ReflectionTestUtils.setField(target,"eventMapper",events);ReflectionTestUtils.setField(target,"reviewMapper",reviews);
        ReflectionTestUtils.setField(target,"stateMapper",state);ReflectionTestUtils.setField(target,"allocationMapper",allocations);
        ReflectionTestUtils.setField(target,"revisionMapper",session.getMapper(MesProProcessPoolEventRevisionMapper.class));
        ReflectionTestUtils.setField(target,"returnCorrectionResolver",resolver);
        ReflectionTestUtils.setField(target,"poolQuantityService",new MesReportAllocationPoolQuantityService());
        var authorizer=(MesRouteStartProductionLeaderAuthorizationService)ReflectionTestUtils.getField(target,"routeStartAuthorizationService");
        when(authorizer.listAuthorizedRouteProcesses(3001L)).thenReturn(List.of(cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO.builder().id(31L).processId(40L).build()));
        var signature=(MesProBatchRecordExecutionSignatureService)ReflectionTestUtils.getField(target,"signatureService");
        when(signature.recordTeamLeaderReviewSignature(any(),any(),any(),any(),any(),any())).thenReturn(9901L);
        var active=(MesProcessPoolActiveOrderMapper)ReflectionTestUtils.getField(target,"activeOrderMapper");
        var order=cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO.builder()
                .id(413L).leaderUserId(3001L).workOrderId(20L).routeId(30L).routeVersionId(100L).activeStatus("ACTIVE").businessStatus("ACTIVE").build();order.setTenantId(1L);
        when(active.selectActiveListByLeaderForUpdate(3001L)).thenReturn(List.of(order));when(active.selectByIdForUpdate(413L)).thenReturn(order);when(active.selectById(413L)).thenReturn(order);
        var work=(cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper)ReflectionTestUtils.getField(target,"workOrderMapper");
        var workRow=cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO.builder().id(20L).code("WORK-20").quantity(BigDecimal.TEN).build();
        when(work.selectListByIdsForUpdate(any())).thenReturn(List.of(workRow));when(work.selectListByIds(any())).thenReturn(List.of(workRow));
        var targets=(MesTeamLeaderOrderProcessTargetService)ReflectionTestUtils.getField(target,"targetService");
        when(targets.requireUniqueTargetForProcess(any(),any())).thenReturn(new MesTeamLeaderOrderProcessTarget(31L,40L,BigDecimal.TEN,BigDecimal.ONE,BigDecimal.TEN));
        var snapshots=(MesProcessPoolActiveOrderProcessSnapshotMapper)ReflectionTestUtils.getField(target,"activeOrderProcessSnapshotMapper");
        var frozen=cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO.builder()
                .activeOrderId(413L).workOrderId(20L).routeId(30L).routeVersionId(100L).routeProcessId(31L).processId(40L)
                .overagePercentSnapshot(BigDecimal.ZERO).productionConfigSnapshotJson("{\"outputMaterialIds\":[]}").build();
        when(snapshots.selectListByActiveOrderAndProcessForUpdate(any(),any())).thenReturn(List.of(frozen));when(snapshots.selectByActiveOrderAndProcess(any(),any(),any())).thenReturn(frozen);
        var audits=(MesProcessPoolReportAllocationAdjustmentAuditMapper)ReflectionTestUtils.getField(target,"auditMapper");when(audits.insertBatch(anyCollection())).thenReturn(true);
        return target;
    }

    private cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelineEventRespVO roundTimeline() throws Exception {
        String xml=java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/mapper/pro/processpool/MesProProcessPoolTimelineReadMapper.xml"));
        String probe="<select id='roundProbe' resultType='map'>SELECT pool_event.id, pool_event.event_type AS eventType, pool_event.raw_payload AS originalPayloadJson, <include refid='DisplayedReviewContextColumns'/> COALESCE(latest_submission_review.review_status,'PENDING') AS submissionReviewStatus FROM mes_pro_process_pool_event pool_event <include refid='LatestSubmissionReviewJoin'/> WHERE pool_event.id=176</select>";
        var configuration=new org.apache.ibatis.session.Configuration();
        xml=xml.replace(" AS CHAR)"," AS VARCHAR)").replace(" AS BINARY)"," AS VARBINARY)").replace("</mapper>",probe+"</mapper>");
        new org.apache.ibatis.builder.xml.XMLMapperBuilder(new java.io.StringReader(xml),configuration,"round.xml",configuration.getSqlFragments()).parse();
        String type=ProcessPoolTimelinePqcGroupSqlTest.class.getName();
        for(String function:List.of("JSON_VALID","JSON_EXTRACT","JSON_UNQUOTE","JSON_TYPE")) {
            String method=switch(function){case "JSON_VALID"->"jsonValid";case "JSON_EXTRACT"->"jsonExtract";case "JSON_UNQUOTE"->"jsonUnquote";default->"jsonType";};
            jdbc.execute("CREATE ALIAS IF NOT EXISTS "+function+" FOR '"+type+"."+method+"'");
        }
        var sql=configuration.getMappedStatement("cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolTimelineReadMapper.roundProbe").getBoundSql(null).getSql();
        var row=jdbc.queryForObject(sql,(r,n)->new ProcessPoolTimelineEventReadDO().setId(r.getLong("id")).setEventType(r.getString("eventType"))
                .setOriginalPayloadJson(r.getString("originalPayloadJson")).setDisplayedReviewId(r.getLong("displayedReviewId"))
                .setDisplayedReviewRound(r.getInt("displayedReviewRound")).setDisplayedRevisionId(r.getLong("displayedRevisionId"))
                .setDisplayedAllocationVersion(r.getInt("displayedAllocationVersion")).setDisplayedReviewStatus(r.getString("displayedReviewStatus"))
                .setSubmissionReviewStatus(r.getString("submissionReviewStatus")));
        var timeline=new ProcessPoolTimelineServiceImpl(mock(MesProProcessPoolTimelineReadMapper.class));
        return ReflectionTestUtils.invokeMethod(timeline,"toEventRespVO",row);
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
        var time = roundTrip ? LocalDateTime.now() : LocalDateTime.of(2026, 9, 29, 7, 0);
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
        return new MesProBatchRecordExecutionFieldAuditSignatureResult().setSignatureId(9102L).setActorId(3001L).setActorName("correction-test").setSignedAt(time);
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
