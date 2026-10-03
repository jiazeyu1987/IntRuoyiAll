package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.MesReleaseAffectedStateCollector;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Uses actual MyBatis mappers, H2 rows and a Spring transaction proxy. */
public class MesActiveOrderReworkCycleTransactionTest {
    private JdbcTemplate jdbc;
    private MesProcessPoolActiveOrderMapper orders;
    private MesProcessPoolActiveOrderProcessSnapshotMapper snapshots;
    private MesPqcInspectionTaskMapper tasks;
    private MesActiveOrderReworkCycleService service;
    private MesProcessPoolReportAllocationMapper allocations;
    private MesProProcessPoolEventMapper events;
    private MesProcessPoolActiveOrderCompletionReceiptMapper receipts;
    private MesTeamLeaderActiveOrderCompletionProgressPortImpl progress;
    private MesTeamLeaderActiveOrderCompletionServiceImpl completion;
    private Long workOrderId = 9L;
    private Long routeId = 4L;
    private Long routeVersionId = 5L;

    public void setUpForDownstream(Long workOrderId, Long routeId, Long routeVersionId) throws Exception {
        this.workOrderId = workOrderId;
        this.routeId = routeId;
        this.routeVersionId = routeVersionId;
        setUp();
    }

    public MesProcessPoolActiveOrderCompletionReceiptMapper receiptMapper() {
        return receipts;
    }

    @BeforeEach
    void setUp() throws Exception {
        var source = new DriverManagerDataSource("jdbc:h2:mem:rework_" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false", "sa", "");
        jdbc = new JdbcTemplate(source);
        createTable(MesProcessPoolActiveOrderDO.class, "mes_pro_process_pool_active_order");
        createTable(MesProcessPoolActiveOrderProcessSnapshotDO.class, "mes_pro_process_pool_active_order_process_snapshot");
        createTable(MesPqcInspectionTaskDO.class, "mes_pqc_inspection_task");
        createTable(MesProcessPoolReportAllocationDO.class, "mes_pro_process_pool_report_allocation");
        createTable(MesProProcessPoolEventDO.class, "mes_pro_process_pool_event");
        createTable(MesProcessPoolActiveOrderCompletionReceiptDO.class, "mes_pro_process_pool_active_order_completion_receipt");
        createTable(MesProcessPoolOrderProcessCompletionDO.class, "mes_pro_process_pool_order_process_completion");
        createTable(MesProProcessPoolPqcRecordDO.class, "mes_pro_process_pool_pqc_record");
        createTable(MesPqcProcessInspectionAggregateDetailDO.class, "mes_pqc_process_inspection_aggregate_detail");
        jdbc.execute("ALTER TABLE mes_pro_process_pool_active_order ADD current_route_version_id BIGINT GENERATED ALWAYS AS "
                + "(CASE WHEN business_status IN ('REWORKED','VERSION_UPGRADED') THEN NULL ELSE route_version_id END)");
        jdbc.execute("CREATE UNIQUE INDEX uk_cycle ON mes_pro_process_pool_active_order "
                + "(tenant_id,work_order_id,route_id,current_route_version_id,deleted)");
        jdbc.execute("CREATE UNIQUE INDEX uk_review ON mes_pro_process_pool_active_order (tenant_id,rework_review_id,deleted)");
        var config = new MybatisConfiguration();
        config.setMapUnderscoreToCamelCase(true);
        config.addMapper(MesProcessPoolActiveOrderMapper.class);
        config.addMapper(MesProcessPoolActiveOrderProcessSnapshotMapper.class);
        config.addMapper(MesPqcInspectionTaskMapper.class);
        config.addMapper(MesProcessPoolReportAllocationMapper.class);
        config.addMapper(MesProProcessPoolEventMapper.class);
        config.addMapper(MesProcessPoolActiveOrderCompletionReceiptMapper.class);
        var bean = new MybatisSqlSessionFactoryBean();
        bean.setDataSource(source);
        bean.setConfiguration(config);
        SqlSessionFactory factory = bean.getObject();
        var session = new SqlSessionTemplate(factory);
        orders = session.getMapper(MesProcessPoolActiveOrderMapper.class);
        snapshots = session.getMapper(MesProcessPoolActiveOrderProcessSnapshotMapper.class);
        tasks = session.getMapper(MesPqcInspectionTaskMapper.class);
        allocations = session.getMapper(MesProcessPoolReportAllocationMapper.class);
        events = session.getMapper(MesProProcessPoolEventMapper.class);
        receipts = session.getMapper(MesProcessPoolActiveOrderCompletionReceiptMapper.class);
        progress = new MesTeamLeaderActiveOrderCompletionProgressPortImpl(snapshots, allocations, tasks, events);
        var backfill = mock(MesTeamLeaderActiveOrderCompletionBackfillPort.class);
        when(backfill.prepare(anyLong(), any(), any())).thenAnswer(invocation ->
                completionDraft(invocation.getArgument(1)));
        when(backfill.matchesReceiptSources(anyLong(), any(), any(), any())).thenReturn(true);
        completion = new MesTeamLeaderActiveOrderCompletionServiceImpl(orders, receipts, progress, backfill,
                mock(MesTeamLeaderActiveOrderPickListCompletionSourceService.class),
                mock(MesActiveOrderTransferTraceService.class), mock(MesPqcProcessInspectionAggregationService.class));
        // This fixture tests rework/completion transactions. The dedicated audit transaction
        // test uses the real GxP writer; here its boundary must return distinct event bindings.
        var audit = mock(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService.class);
        var auditSequence = new java.util.concurrent.atomic.AtomicLong(8800L);
        when(audit.append(any())).thenAnswer(invocation -> {
            long sequence = auditSequence.incrementAndGet();
            return new cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditAppendResult(
                    sequence, sequence, "fixture-audit-hash-" + sequence, false);
        });
        org.springframework.test.util.ReflectionTestUtils.setField(completion, "gxpAuditService", audit);
        org.springframework.test.util.ReflectionTestUtils.setField(completion, "affectedStateCollector",
                new MesReleaseAffectedStateCollector(source));
        // The real collector and completion mappers must join this fixture's physical transaction,
        // including when this fixture is called from a different datasource's Spring test transaction.
        var transactionManager = new DataSourceTransactionManager(source);
        var completionProxy = new ProxyFactory(completion);
        completionProxy.setProxyTargetClass(true);
        completionProxy.addAdvice(new TransactionInterceptor(transactionManager,
                new AnnotationTransactionAttributeSource()));
        completion = (MesTeamLeaderActiveOrderCompletionServiceImpl) completionProxy.getProxy();
        var target = new MesActiveOrderReworkCycleService(orders, snapshots, tasks);
        var proxy = new ProxyFactory(target);
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(transactionManager,
                new AnnotationTransactionAttributeSource()));
        service = (MesActiveOrderReworkCycleService) proxy.getProxy();
        seedCycle();
    }

    @Test
    void twoReworksCopyFrozenPlansResetExecutionAndReplaySameReview() {
        exerciseTwoReworks(receipt -> {});
    }

    public void exerciseTwoReworks(java.util.function.Consumer<MesProcessPoolActiveOrderCompletionReceiptDO> downstream) {
        TenantUtils.execute(1L, () -> exerciseTwoReworksInFixtureTenant(downstream));
    }

    private void exerciseTwoReworksInFixtureTenant(
            java.util.function.Consumer<MesProcessPoolActiveOrderCompletionReceiptDO> downstream) {
        Long source = 1L;
        for (long review = 100; review <= 101; review++) {
            var reworkAt = LocalDateTime.of(2026, 9, 28, 8, 0).plusDays(review - 100);
            Long next = service.start(source, workOrderId, review, reworkAt);
            assertNotEquals(source, next);
            assertEquals(next, service.start(source, workOrderId, review, LocalDateTime.now()));
            var oldOrder = orders.selectById(source);
            var nextOrder = orders.selectById(next);
            assertEquals("REWORKED", oldOrder.getBusinessStatus());
            assertEquals("REMOVED", oldOrder.getActiveStatus());
            assertEquals("ACTIVE", nextOrder.getBusinessStatus());
            assertEquals(source, nextOrder.getReworkSourceActiveOrderId());
            assertEquals(review, nextOrder.getReworkReviewId());
            assertEquals(routeVersionId, nextOrder.getRouteVersionId());
            assertEquals(6L, nextOrder.getQaRegulationVersionId());
            assertEquals(0, nextOrder.getVersion());
            assertEquals(0, progress.read(2L, nextOrder).getProductionProgressPercent().signum());
            assertEquals(0, progress.read(2L, nextOrder).getInspectionProgressPercent().signum());
            assertThrows(RuntimeException.class, () -> completion.complete(2L, completionCommand(nextOrder)));
            var nextSnapshot = snapshots.selectListByActiveOrderId(next).get(0);
            assertEquals("{\"outputMaterialIds\":[]}", nextSnapshot.getProductionConfigSnapshotJson());
            assertEquals(0, BigDecimal.TEN.compareTo(nextSnapshot.getPlannedQuantitySnapshot()));
            var nextTask = tasks.selectListByActiveOrderId(next).get(0);
            assertEquals("PENDING", nextTask.getTaskStatus());
            assertNull(nextTask.getSubmittedEventId());
            assertNull(nextTask.getSubmittedContentHash());
            assertEquals(0, nextTask.getActualInspectionQuantity());
            assertEquals(reworkAt.toLocalDate(), nextTask.getBusinessDate());
            assertEquals(2, nextTask.getPlannedInspectionQuantity());
            assertEquals("CONFIRMED", tasks.selectListByActiveOrderId(source).get(0).getTaskStatus());
            var event = new MesProProcessPoolEventDO().setId(500L + review).setWorkOrderId(workOrderId)
                    .setRouteId(routeId).setRouteProcessId(12L).setProcessId(13L)
                    .setEventType(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT)
                    .setReportOutputQuantity(BigDecimal.TEN).setRawPayload("{\"materialDetails\":[]}");
            event.setTenantId(1L);
            event.setDeleted(false);
            events.insert(event);
            var allocation = new MesProcessPoolReportAllocationDO().setActiveOrderId(next).setWorkOrderId(workOrderId)
                    .setRouteProcessId(12L).setProcessId(13L).setEventId(event.getId())
                    .setAllocatedQuantity(BigDecimal.TEN).setLifecycleStatus("CURRENT");
            allocation.setTenantId(1L);
            allocation.setDeleted(false);
            allocations.insert(allocation);
            nextTask.setTaskStatus("CONFIRMED").setActualInspectionQuantity(2).setSubmittedEventId(500L + review);
            tasks.updateById(nextTask);
            assertTrue(progress.read(2L, nextOrder).isDoubleComplete());
            var completed = completion.complete(2L, completionCommand(nextOrder));
            assertNotNull(completed.getCompletionReceiptId());
            assertEquals(completed.getCompletionReceiptId(), completion.complete(2L, completionCommand(nextOrder))
                    .getCompletionReceiptId());
            assertEquals("COMPLETED", orders.selectById(next).getBusinessStatus());
            assertEquals(next, receipts.selectById(completed.getCompletionReceiptId()).getActiveOrderId());
            downstream.accept(receipts.selectById(completed.getCompletionReceiptId()));
            source = next;
        }
        assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_process_pool_active_order", Integer.class));
        assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pqc_inspection_task", Integer.class));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_process_pool_active_order_completion_receipt", Integer.class));
    }

    @Test
    void insertFailureRollsBackRetirementNewOrderAndAlreadyCopiedSnapshot() {
        jdbc.execute("ALTER TABLE mes_pqc_inspection_task ADD CONSTRAINT no_new_cycle CHECK (active_order_id=1)");
        assertThrows(RuntimeException.class, () -> service.start(1L, 9L, 100L, LocalDateTime.now()));
        assertEquals("COMPLETED", orders.selectById(1L).getBusinessStatus());
        assertEquals("ACTIVE", orders.selectById(1L).getActiveStatus());
        assertEquals(7, orders.selectById(1L).getVersion());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_process_pool_active_order", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_process_pool_active_order_process_snapshot", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pqc_inspection_task", Integer.class));
    }

    private void seedCycle() {
        var order = new MesProcessPoolActiveOrderDO().setId(1L).setLeaderUserId(2L).setWorkOrderId(workOrderId)
                .setRouteId(routeId).setRouteVersionId(routeVersionId).setQaRegulationVersionId(6L)
                .setErpFixedQuantitySnapshot(BigDecimal.TEN).setActiveStatus("ACTIVE").setBusinessStatus("COMPLETED")
                .setVersion(7).setSortOrder(1L);
        order.setTenantId(1L);
        order.setDeleted(false);
        orders.insert(order);
        var snapshot = new MesProcessPoolActiveOrderProcessSnapshotDO().setId(10L).setActiveOrderId(1L)
                .setWorkOrderId(workOrderId).setRouteId(routeId).setRouteVersionId(routeVersionId).setRouteProcessId(12L).setProcessId(13L)
                .setProductionConfigSnapshotJson("{\"outputMaterialIds\":[]}").setPlannedQuantitySnapshot(BigDecimal.TEN);
        snapshot.setTenantId(1L);
        snapshot.setDeleted(false);
        snapshots.insert(snapshot);
        var task = new MesPqcInspectionTaskDO().setId(20L).setActiveOrderId(1L).setWorkOrderId(workOrderId)
                .setRouteId(routeId).setRouteVersionId(routeVersionId).setRouteProcessId(12L).setProcessId(13L)
                .setRegulationVersionId(6L).setTaskStatus("CONFIRMED").setPlannedInspectionQuantity(2)
                .setBusinessDate(LocalDate.of(2026, 9, 27))
                .setActualInspectionQuantity(2).setSubmittedEventId(30L).setSubmittedContentHash("previous-hash");
        task.setTenantId(1L);
        task.setDeleted(false);
        tasks.insert(task);
    }

    private void createTable(Class<?> type, String table) {
        var columns = new ArrayList<String>();
        for (Class<?> current = type; current != Object.class; current = current.getSuperclass()) {
            for (var field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                String name = field.getName().replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
                Class<?> fieldType = field.getType();
                String sqlType = fieldType == Long.class ? "BIGINT" : fieldType == Integer.class ? "INT"
                        : fieldType == Boolean.class ? "BOOLEAN" : fieldType == BigDecimal.class ? "DECIMAL(24,6)"
                        : fieldType == LocalDateTime.class ? "TIMESTAMP" : fieldType == LocalDate.class ? "DATE"
                        : "VARCHAR(10000)";
                columns.add(name + " " + sqlType + (name.equals("id") ? " PRIMARY KEY"
                        : name.equals("deleted") ? " DEFAULT FALSE" : ""));
            }
        }
        jdbc.execute("CREATE TABLE " + table + "(" + String.join(",", columns) + ")");
    }

    private MesTeamLeaderActiveOrderCompletionCommand completionCommand(MesProcessPoolActiveOrderDO order) {
        return new MesTeamLeaderActiveOrderCompletionCommand().setActiveOrderId(order.getId())
                .setExpectedVersion(order.getVersion()).setIdempotencyKey("complete-" + order.getId());
    }

    private MesTeamLeaderActiveOrderCompletionBackfillDraft completionDraft(MesProcessPoolActiveOrderDO order) {
        return new MesTeamLeaderActiveOrderCompletionBackfillDraft()
                .setWorkOrderId(workOrderId).setBatchCode("BATCH-9").setRouteId(routeId).setRouteVersionId(routeVersionId)
                .setSourceSnapshotHash("source-" + order.getId()).setFormalSourceSnapshotJson("{\"formal\":true}")
                .setSignatureSnapshotJson("{\"signature\":true}").setBatchRecordSourceIdsJson("[1]")
                .setProcessInspectionSourceIdsJson("[2]").setBatchRecordStatus("SUCCESS")
                .setProcessInspectionStatus("SUCCESS").setLossReportStatus("NOT_REQUIRED")
                .setBatchRecordId(order.getId()).setProcessInspectionId(order.getId())
                .setHasActualLoss(false).setLossQuantity(BigDecimal.ZERO)
                .setZeroLossConfirmationSnapshot("{\"confirmed\":true}")
                .setLossConditionFactsJson("[{\"processId\":13,\"status\":\"NO_LOSS\","
                        + "\"hasActualLoss\":false,\"lossQuantity\":0,"
                        + "\"zeroLossConfirmationSnapshot\":\"{\\\"confirmed\\\":true}\","
                        + "\"sourceHash\":\"loss-source-1\"}]");
    }
}
