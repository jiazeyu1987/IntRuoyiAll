package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionPieceDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionPieceDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamLeaderScopeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolEventService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolCreatePqcInspectionReqDTO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.pqc.MesPqcItemEquipmentConfigService;
import cn.iocoder.yudao.module.mes.service.qa.regulation.MesQaInspectionRegulationService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PQC_SUBMISSION_CONTENT_CONFLICT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MesFrontlinePqcSubmissionConcurrencyTest {

    private static final long LOGIN_USER_ID = 100L;
    private static final long EMPLOYEE_A = 101L;
    private static final long EMPLOYEE_B = 102L;
    private static final long ACTIVE_ORDER_ID = 5001L;
    private static final long TASK_ID = 9101L;
    private static final long WORK_ORDER_ID = 1001L;
    private static final long ROUTE_ID = 2001L;
    private static final long ROUTE_VERSION_ID = 3001L;
    private static final long DCC_PROJECT_ID = 6001L;
    private static final long REGULATION_ID = 7001L;
    private static final long REGULATION_VERSION_ID = 8001L;
    private static final long QA_PROCESS_ID = 9001L;

    @Test
    void sameContentConcurrentTransactionsReturnOneReceiptAndOneFormalWriteSet() throws Exception {
        Fixture fixture = new Fixture();

        Pair outcomes = fixture.submitConcurrently(
                fixture.command(EMPLOYEE_A, "合格"), fixture.command(EMPLOYEE_A, "合格"));

        assertNull(outcomes.first.error());
        assertNull(outcomes.second.error());
        assertEquals(outcomes.first.result().pqcEventId(), outcomes.second.result().pqcEventId());
        fixture.assertExactlyOneFormalWriteSet();
    }

    @Test
    void conflictingContentConcurrentTransactionsAllowOneCasWinnerAndRejectTheOther() throws Exception {
        Fixture fixture = new Fixture();

        Pair outcomes = fixture.submitConcurrently(
                fixture.command(EMPLOYEE_A, "合格"), fixture.command(EMPLOYEE_A, "不合格"));

        assertOneSuccessAndOneConflict(outcomes);
        fixture.assertExactlyOneFormalWriteSet();
    }

    @Test
    void differentActualEmployeesShareOneTaskIdempotencyDomainUnderConcurrency() throws Exception {
        Fixture fixture = new Fixture();

        Pair outcomes = fixture.submitConcurrently(
                fixture.command(EMPLOYEE_A, "合格"), fixture.command(EMPLOYEE_B, "合格"));

        assertOneSuccessAndOneConflict(outcomes);
        fixture.assertExactlyOneFormalWriteSet();
        long persistedActor = fixture.jdbc.queryForObject(
                "SELECT actual_employee_id FROM pqc_event", Long.class);
        assertTrue(persistedActor == EMPLOYEE_A || persistedActor == EMPLOYEE_B);
    }

    private static void assertOneSuccessAndOneConflict(Pair pair) {
        List<Outcome> successes = List.of(pair.first(), pair.second()).stream()
                .filter(outcome -> outcome.result() != null).toList();
        List<Outcome> failures = List.of(pair.first(), pair.second()).stream()
                .filter(outcome -> outcome.error() != null).toList();
        assertEquals(1, successes.size());
        assertEquals(1, failures.size());
        assertEquals(PRO_FRONTLINE_PQC_SUBMISSION_CONTENT_CONFLICT.getCode(), failures.get(0).error().getCode());
    }

    private record Outcome(MesFrontlinePqcSubmitResult result, ServiceException error) {
    }

    private record Pair(Outcome first, Outcome second) {
    }

    private static final class Fixture {

        private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 8, 14);
        private static final LocalDateTime SUBMIT_TIME = LocalDateTime.of(2026, 8, 14, 10, 30);

        private final JdbcTemplate jdbc;
        private final TransactionTemplate transactionTemplate;
        private final AtomicInteger casSuccessCount = new AtomicInteger();
        private final MesFrontlinePqcContextService service;

        private Fixture() {
            JdbcDataSource dataSource = new JdbcDataSource();
            dataSource.setURL("jdbc:h2:mem:pqc_concurrency_" + UUID.randomUUID()
                    + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
            dataSource.setUser("sa");
            jdbc = new JdbcTemplate(dataSource);
            createSchema();
            insertPendingTask();

            DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
            transactionTemplate = new TransactionTemplate(transactionManager);
            transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);

            MesProcessPoolActiveOrderMapper activeOrderMapper = mock(MesProcessPoolActiveOrderMapper.class);
            MesProProcessPoolEventMapper eventMapper = mock(MesProProcessPoolEventMapper.class);
            MesPqcInspectionTaskMapper taskMapper = mock(MesPqcInspectionTaskMapper.class);
            MesPqcInspectionPieceDetailMapper pieceDetailMapper = mock(MesPqcInspectionPieceDetailMapper.class);
            MesProProcessPoolPqcRecordMapper recordMapper = mock(MesProProcessPoolPqcRecordMapper.class);
            MesProcessPoolTeamLeaderScopeMapper scopeMapper = mock(MesProcessPoolTeamLeaderScopeMapper.class);
            AdminUserApi adminUserApi = mock(AdminUserApi.class);
            MesQaInspectionRegulationProcessMapper processMapper =
                    mock(MesQaInspectionRegulationProcessMapper.class);
            MesQaInspectionRegulationVersionMapper versionMapper =
                    mock(MesQaInspectionRegulationVersionMapper.class);
            MesQaInspectionRegulationMapper regulationMapper = mock(MesQaInspectionRegulationMapper.class);
            MesQaInspectionRegulationItemMapper itemMapper = mock(MesQaInspectionRegulationItemMapper.class);
            MesPqcItemEquipmentConfigService pqcItemEquipmentConfigService =
                    mock(MesPqcItemEquipmentConfigService.class);
            DccProjectCodeMapper dccMapper = mock(DccProjectCodeMapper.class);
            MesProBatchRecordExecutionSignatureService signatureService =
                    mock(MesProBatchRecordExecutionSignatureService.class);
            MesProcessPoolEventService eventService = mock(MesProcessPoolEventService.class);

            when(taskMapper.selectByIdForUpdate(TASK_ID)).thenAnswer(ignored -> selectTaskForUpdate());
            when(taskMapper.updateSubmittedIfPending(anyLong(), any(), anyString(), anyString(), anyString()))
                    .thenAnswer(invocation -> updateSubmittedIfPending(invocation.getArgument(0),
                            invocation.getArgument(1), invocation.getArgument(2),
                            invocation.getArgument(3), invocation.getArgument(4)));
            when(taskMapper.updateSubmittedEventId(anyLong(), anyLong())).thenAnswer(invocation -> {
                Long taskId = invocation.getArgument(0, Long.class);
                Long eventId = invocation.getArgument(1, Long.class);
                return jdbc.update("UPDATE pqc_task SET submitted_event_id = ? WHERE id = ?",
                        eventId, taskId);
            });
            when(pieceDetailMapper.insertBatch(any(Collection.class))).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Collection<MesPqcInspectionPieceDetailDO> details = invocation.getArgument(0, Collection.class);
                details.forEach(detail -> jdbc.update(
                        "INSERT INTO pqc_detail(task_id, item_code, measured_value, judgement) VALUES (?, ?, ?, ?)",
                        detail.getTaskId(), detail.getItemCode(), detail.getMeasuredValue(), detail.getJudgement()));
                return Boolean.TRUE;
            });
            when(signatureService.recordPqcSubmitSignature(anyLong(), anyString(), anyString()))
                    .thenAnswer(invocation -> insertSignature(invocation.getArgument(0)));
            when(eventService.createPqcInspectionEvent(any(MesProcessPoolCreatePqcInspectionReqDTO.class)))
                    .thenAnswer(invocation -> insertEventAndRecord(invocation.getArgument(0)));
            when(eventMapper.selectById(anyLong())).thenAnswer(invocation -> selectEvent(invocation.getArgument(0)));
            when(eventMapper.selectListPqcByTaskId(anyString(), anyLong())).thenAnswer(invocation ->
                    selectEventsByTask(invocation.getArgument(1)));
            when(recordMapper.selectByEventId(anyLong())).thenAnswer(invocation ->
                    selectRecord(invocation.getArgument(0)));

            when(activeOrderMapper.selectById(ACTIVE_ORDER_ID)).thenReturn(activeOrder());
            when(scopeMapper.selectActiveScopesByLeaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC))
                    .thenReturn(List.of(employeeScope(EMPLOYEE_A), employeeScope(EMPLOYEE_B)));
            when(adminUserApi.getUserList(any())).thenReturn(List.of(
                    user(LOGIN_USER_ID), user(EMPLOYEE_A), user(EMPLOYEE_B)));
            when(processMapper.selectById(QA_PROCESS_ID)).thenReturn(
                    MesQaInspectionRegulationProcessDO.builder().id(QA_PROCESS_ID)
                            .regulationVersionId(REGULATION_VERSION_ID).build());
            when(versionMapper.selectById(REGULATION_VERSION_ID)).thenReturn(
                    MesQaInspectionRegulationVersionDO.builder().id(REGULATION_VERSION_ID)
                            .regulationId(REGULATION_ID).lifecycleStatus("PUBLISHED").build());
            when(regulationMapper.selectById(REGULATION_ID)).thenReturn(
                    MesQaInspectionRegulationDO.builder().id(REGULATION_ID)
                            .dccProjectCodeId(DCC_PROJECT_ID).build());
            when(dccMapper.selectById(DCC_PROJECT_ID)).thenReturn(
                    DccProjectCodeDO.builder().id(DCC_PROJECT_ID).build());
            when(itemMapper.selectListByVersionId(REGULATION_VERSION_ID)).thenReturn(List.of(publishedItem()));
            when(pqcItemEquipmentConfigService.listEnabledEquipmentOptionsByItemCodes(any())).thenReturn(Map.of());

            service = new MesFrontlinePqcContextServiceImpl(activeOrderMapper, eventMapper,
                    mock(MesProcessPoolActiveOrderProcessSnapshotMapper.class),
                    mock(MesProWorkOrderMapper.class), mock(MesProRouteMapper.class),
                    mock(MesProRouteVersionMapper.class), dccMapper, regulationMapper, versionMapper,
                    processMapper, itemMapper, pqcItemEquipmentConfigService, mock(MesQaInspectionRegulationService.class),
                    taskMapper, pieceDetailMapper, mock(MesMdItemService.class), scopeMapper, adminUserApi,
                    eventService, recordMapper, signatureService);
        }

        private Pair submitConcurrently(MesFrontlinePqcSubmitCommand firstCommand,
                                        MesFrontlinePqcSubmitCommand secondCommand) throws Exception {
            CyclicBarrier transactionStart = new CyclicBarrier(2);
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                Future<Outcome> first = executor.submit(() -> submitInTransaction(firstCommand, transactionStart));
                Future<Outcome> second = executor.submit(() -> submitInTransaction(secondCommand, transactionStart));
                return new Pair(first.get(), second.get());
            } finally {
                executor.shutdownNow();
            }
        }

        private Outcome submitInTransaction(MesFrontlinePqcSubmitCommand command, CyclicBarrier start) {
            try {
                MesFrontlinePqcSubmitResult result = transactionTemplate.execute(status -> {
                    try {
                        start.await();
                    } catch (Exception ex) {
                        throw new IllegalStateException(ex);
                    }
                    return service.submitPqcInspection(LOGIN_USER_ID, command);
                });
                return new Outcome(result, null);
            } catch (ServiceException ex) {
                return new Outcome(null, ex);
            }
        }

        private void assertExactlyOneFormalWriteSet() {
            assertEquals(1, casSuccessCount.get(), "PENDING->SUBMITTED CAS must have one winner");
            assertEquals(1, count("pqc_signature"));
            assertEquals(1, count("pqc_detail"));
            assertEquals(1, count("pqc_record"));
            assertEquals(1, count("pqc_event"));
            assertEquals("SUBMITTED", jdbc.queryForObject("SELECT status FROM pqc_task WHERE id = ?",
                    String.class, TASK_ID));
            assertNotNull(jdbc.queryForObject("SELECT submitted_content_hash FROM pqc_task WHERE id = ?",
                    String.class, TASK_ID));
            assertNotNull(jdbc.queryForObject("SELECT submitted_event_id FROM pqc_task WHERE id = ?",
                    Long.class, TASK_ID));
        }

        private int count(String table) {
            return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        }

        private MesFrontlinePqcSubmitCommand command(long actualEmployeeId, String sampleValue) {
            return MesFrontlinePqcSubmitCommand.builder()
                    .activeOrderId(ACTIVE_ORDER_ID).pqcTaskId(TASK_ID)
                    .regulationVersionId(REGULATION_VERSION_ID).qaProcessId(QA_PROCESS_ID)
                    .actualEmployeeId(actualEmployeeId).actualInspectionQuantity(1)
                    .signaturePassword("valid-password").scrapQuantity(0)
                    .itemResults(List.of(MesFrontlinePqcSubmitCommand.ItemResult.builder()
                            .itemCode("QA-001").sampleValues(List.of(sampleValue)).build()))
                    .rawPayload(Map.of()).build();
        }

        private void createSchema() {
            jdbc.execute("CREATE TABLE pqc_task (id BIGINT PRIMARY KEY, status VARCHAR(32) NOT NULL, " +
                    "actual_quantity INT, submitted_content_hash CHAR(64), submitted_event_id BIGINT)");
            jdbc.execute("CREATE TABLE pqc_signature (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, " +
                    "actual_employee_id BIGINT NOT NULL)");
            jdbc.execute("CREATE TABLE pqc_detail (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, " +
                    "task_id BIGINT NOT NULL, item_code VARCHAR(64) NOT NULL, measured_value VARCHAR(128) NOT NULL, " +
                    "judgement VARCHAR(32) NOT NULL)");
            jdbc.execute("CREATE TABLE pqc_event (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, " +
                    "task_id BIGINT NOT NULL UNIQUE, actual_employee_id BIGINT NOT NULL, signature_id BIGINT NOT NULL, " +
                    "server_submit_time TIMESTAMP NOT NULL, raw_payload CLOB)");
            jdbc.execute("CREATE TABLE pqc_record (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, " +
                    "event_id BIGINT NOT NULL UNIQUE, signature_id BIGINT NOT NULL, inspection_result VARCHAR(32) NOT NULL, " +
                    "server_submit_time TIMESTAMP NOT NULL)");
        }

        private void insertPendingTask() {
            jdbc.update("INSERT INTO pqc_task(id, status) VALUES (?, 'PENDING')", TASK_ID);
        }

        private MesPqcInspectionTaskDO selectTaskForUpdate() {
            return jdbc.queryForObject("SELECT status, actual_quantity, submitted_content_hash, submitted_event_id " +
                            "FROM pqc_task WHERE id = ? FOR UPDATE",
                    (rs, rowNum) -> MesPqcInspectionTaskDO.builder().id(TASK_ID)
                            .activeOrderId(ACTIVE_ORDER_ID).workOrderId(WORK_ORDER_ID).routeId(ROUTE_ID)
                            .routeVersionId(ROUTE_VERSION_ID).qaProcessId(QA_PROCESS_ID)
                            .qaItemCode("QA-001")
                            .regulationVersionId(REGULATION_VERSION_ID).inspectionType("FIRST")
                            .inspectionRuleKey("FIRST").businessDate(BUSINESS_DATE).shiftCode("FIRST").roundNo(1)
                            .plannedInspectionQuantity(1).actualInspectionQuantity((Integer) rs.getObject(2))
                            .taskStatus(rs.getString(1)).submittedContentHash(rs.getString(3))
                            .submittedEventId((Long) rs.getObject(4)).build(), TASK_ID);
        }

        private int updateSubmittedIfPending(Long taskId, Integer actualQuantity, String contentHash,
                                             String pendingStatus, String submittedStatus) {
            int updated = jdbc.update("UPDATE pqc_task SET status = ?, actual_quantity = ?, " +
                            "submitted_content_hash = ? WHERE id = ? AND status = ?",
                    submittedStatus, actualQuantity, contentHash, taskId, pendingStatus);
            casSuccessCount.addAndGet(updated);
            return updated;
        }

        private Long insertSignature(Long actualEmployeeId) {
            return insertAndReturnKey("INSERT INTO pqc_signature(actual_employee_id) VALUES (?)",
                    statement -> statement.setLong(1, actualEmployeeId));
        }

        private Long insertEventAndRecord(MesProcessPoolCreatePqcInspectionReqDTO request) {
            Long eventId = insertAndReturnKey("INSERT INTO pqc_event(task_id, actual_employee_id, signature_id, " +
                            "server_submit_time, raw_payload) VALUES (?, ?, ?, ?, ?)", statement -> {
                        statement.setLong(1, request.getFeedbackSourceId());
                        statement.setLong(2, request.getActualEmployeeId());
                        statement.setLong(3, request.getSignatureId());
                        statement.setTimestamp(4, Timestamp.valueOf(SUBMIT_TIME));
                        statement.setString(5, request.getRawPayload());
                    });
            jdbc.update("INSERT INTO pqc_record(event_id, signature_id, inspection_result, server_submit_time) " +
                            "VALUES (?, ?, ?, ?)", eventId, request.getSignatureId(), request.getInspectionResult(),
                    Timestamp.valueOf(SUBMIT_TIME));
            return eventId;
        }

        private Long insertAndReturnKey(String sql, StatementBinder binder) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbc.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                binder.bind(statement);
                return statement;
            }, keyHolder);
            assertNotNull(keyHolder.getKey());
            return keyHolder.getKey().longValue();
        }

        private MesProProcessPoolEventDO selectEvent(Long eventId) {
            return jdbc.queryForObject("SELECT id, task_id, actual_employee_id, signature_id, " +
                            "server_submit_time, raw_payload FROM pqc_event WHERE id = ?",
                    (rs, rowNum) -> MesProProcessPoolEventDO.builder().id(rs.getLong("id"))
                            .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION)
                            .feedbackSourceType("MES_PQC_INSPECTION_TASK")
                            .feedbackSourceId(rs.getLong("task_id"))
                            .actualEmployeeId(rs.getLong("actual_employee_id"))
                            .signatureId(rs.getLong("signature_id"))
                            .serverSubmitTime(rs.getTimestamp("server_submit_time").toLocalDateTime())
                            .rawPayload(rs.getString("raw_payload")).build(), eventId);
        }

        private List<MesProProcessPoolEventDO> selectEventsByTask(Long taskId) {
            return jdbc.query("SELECT id FROM pqc_event WHERE task_id = ? ORDER BY id",
                    (rs, rowNum) -> selectEvent(rs.getLong("id")), taskId);
        }

        private MesProProcessPoolPqcRecordDO selectRecord(Long eventId) {
            return jdbc.queryForObject("SELECT id, event_id, signature_id, inspection_result, server_submit_time " +
                            "FROM pqc_record WHERE event_id = ?",
                    (rs, rowNum) -> MesProProcessPoolPqcRecordDO.builder().id(rs.getLong("id"))
                            .eventId(rs.getLong("event_id")).signatureId(rs.getLong("signature_id"))
                            .inspectionResult(rs.getString("inspection_result"))
                            .serverSubmitTime(rs.getTimestamp("server_submit_time").toLocalDateTime()).build(), eventId);
        }

        private static MesProcessPoolActiveOrderDO activeOrder() {
            return MesProcessPoolActiveOrderDO.builder().id(ACTIVE_ORDER_ID).workOrderId(WORK_ORDER_ID)
                    .routeId(ROUTE_ID).routeVersionId(ROUTE_VERSION_ID).activeStatus("ACTIVE").build();
        }

        private static MesQaInspectionRegulationItemDO publishedItem() {
            return MesQaInspectionRegulationItemDO.builder().id(8101L)
                    .regulationVersionId(REGULATION_VERSION_ID).qaProcessId(QA_PROCESS_ID)
                    .inspectionType("FIRST").itemSort(1).itemCode("QA-001").itemName("外观")
                    .inspectionMethod("目测").inspectionTool("目测").standardText("应合格")
                    .samplingPlanText("全检").equipmentRequired(false).resultType("BOOLEAN")
                    .firstInspectionQuantity(1).build();
        }

        private static MesProcessPoolTeamLeaderScopeDO employeeScope(long employeeId) {
            return MesProcessPoolTeamLeaderScopeDO.builder().id(employeeId).leaderUserId(LOGIN_USER_ID)
                    .leaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC)
                    .scopeType(MesProcessPoolTeamLeaderScopeDO.SCOPE_TYPE_EMPLOYEE)
                    .employeeUserId(employeeId).enabled(true).build();
        }

        private static AdminUserRespDTO user(long userId) {
            AdminUserRespDTO user = new AdminUserRespDTO();
            user.setId(userId);
            user.setUsername("user-" + userId);
            user.setNickname("PQC-" + userId);
            user.setStatus(CommonStatusEnum.ENABLE.getStatus());
            return user;
        }

        @FunctionalInterface
        private interface StatementBinder {
            void bind(PreparedStatement statement) throws java.sql.SQLException;
        }
    }
}
