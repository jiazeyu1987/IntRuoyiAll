package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesActiveOrderReworkCycleService;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureQueryServiceImpl;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.*;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.SystemEntitlementClaimDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.SystemEntitlementGrantDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.SystemEntitlementAuditEventDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.*;
import cn.iocoder.yudao.module.system.dal.mysql.permission.*;
import cn.iocoder.yudao.module.system.service.permission.*;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.mes.approval.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApiImpl;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
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
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Current NCR paths only. Real MyBatis business writes, rework/task services, specialized ledger,
 * independent signature verification and GxpAuditService share one physical H2/Spring transaction.
 * Signature issuance/password authentication is an explicit double; runtime claims and revocation use real services.
 * Test-only MySQL bit translation is not production compatibility or an InnoDB isolation proof.
 */
@org.junit.jupiter.api.parallel.Execution(org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD)
class MesNcrScopeAuditTransactionTest {
    private static final String REVIEW = "mes_pro_edhr_nonconformance_review";
    private static final String COUNTER = REVIEW + "_counter";
    private static final String WORK = "mes_pro_work_order";
    private static final String BATCH = "mes_pro_edhr_batch_execution";
    private static final String ACTIVE = "mes_pro_process_pool_active_order";
    private static final String APPLICATION = ACTIVE + "_release_application";
    private static final String SNAPSHOT = ACTIVE + "_process_snapshot";
    private static final String PQC = "mes_pqc_inspection_task";
    private static final String TASK = "mes_pro_edhr_work_task";
    private static final String RELEASE = "mes_pro_edhr_release_transaction";
    private static final String SPECIALIZED = "mes_pro_edhr_operation_audit_event";
    private static final String SIGNATURE = "system_electronic_signature";
    private static final String CREATE = "mes.nonconformance.active-order.create";
    private static final AtomicBoolean FAILURE_SAW_WRITES = new AtomicBoolean();
    private static String expectedDisposition;
    private JdbcTemplate jdbc;
    private DataSourceTransactionManager transactions;
    private MesProEdhrNonconformanceReviewServiceImpl service;
    private ElectronicSignatureRecordMapper signatures;
    private ElectronicSignatureQueryServiceImpl signatureQuery;
    private MesProBatchRecordExecutionSignatureService signer;
    private PermissionApi permissionBoundary;
    private SystemEntitlementService entitlement;
    private SqlSessionTemplate session;
    private MesProEdhrWorkTaskServiceImpl tasks;
    private final List<String> tables = new ArrayList<>();
    private final List<String> cancellationLocks = new ArrayList<>();

    @BeforeEach
    void fixture() throws Exception {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:ncr_scope_" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        for (Class<?> row : List.of(MesProEdhrNonconformanceReviewDO.class,
                MesProEdhrNonconformanceReviewCounterDO.class, MesProWorkOrderDO.class,
                MesProEdhrBatchExecutionDO.class, MesProEdhrBatchExecutionOriginDO.class,
                MesProcessPoolActiveOrderDO.class, MesProcessPoolActiveOrderReleaseApplicationDO.class,
                MesProcessPoolActiveOrderProcessSnapshotDO.class, MesPqcInspectionTaskDO.class,
                MesProEdhrWorkTaskDO.class, MesProEdhrReleaseTransactionDO.class,
                cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.MesActiveOrderHandoffTaskDO.class,
                SystemEntitlementClaimDO.class, SystemEntitlementGrantDO.class, SystemEntitlementAuditEventDO.class, SystemEntitlementPolicyDO.class, MenuDO.class, AdminUserDO.class,
                MesProEdhrOperationAuditEventDO.class, FileDO.class, ElectronicSignatureRecordDO.class,
                GxpAuditEventDO.class, GxpAuditEventRelationDO.class, GxpAuditLedgerSequenceDO.class,
                GxpAuditPolicyActivationDO.class, GxpAuditPolicyOperationDO.class)) createTable(row);
        var config = new MybatisConfiguration();
        config.setMapUnderscoreToCamelCase(true);
        config.addInterceptor(new MysqlBitsForH2());
        config.addInterceptor(new CancellationLockProbe(cancellationLocks, () -> cancellationProbeEnabled));
        List.of(MesProEdhrNonconformanceReviewMapper.class, MesProEdhrNonconformanceReviewCounterMapper.class,
                MesProWorkOrderMapper.class, MesProEdhrBatchExecutionMapper.class, MesProEdhrBatchExecutionOriginMapper.class,
                MesProcessPoolActiveOrderMapper.class, MesProcessPoolActiveOrderReleaseApplicationMapper.class,
                MesProcessPoolActiveOrderProcessSnapshotMapper.class, MesPqcInspectionTaskMapper.class,
                MesProEdhrWorkTaskMapper.class, MesProEdhrReleaseTransactionMapper.class,
                cn.iocoder.yudao.module.mes.dal.mysql.pro.handoff.MesActiveOrderHandoffTaskMapper.class,
                MesProEdhrOperationAuditEventMapper.class, FileMapper.class, ElectronicSignatureRecordMapper.class,
                GxpAuditEventMapper.class, GxpAuditEventRelationMapper.class, GxpAuditLedgerSequenceMapper.class,
                GxpAuditPolicyActivationMapper.class, GxpAuditPolicyOperationMapper.class, SystemEntitlementClaimMapper.class, SystemEntitlementGrantMapper.class,
                SystemEntitlementAuditEventMapper.class, SystemEntitlementPolicyMapper.class, MenuMapper.class, AdminUserMapper.class).forEach(config::addMapper);
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setConfiguration(config);
        session = new SqlSessionTemplate(Objects.requireNonNull(factory.getObject()));
        transactions = new DataSourceTransactionManager(dataSource);
        var writer = new GxpAuditServiceImpl();
        inject(writer, "auditEventMapper", session.getMapper(GxpAuditEventMapper.class));
        inject(writer, "eventRelationMapper", session.getMapper(GxpAuditEventRelationMapper.class));
        inject(writer, "ledgerSequenceMapper", session.getMapper(GxpAuditLedgerSequenceMapper.class));
        inject(writer, "policyActivationMapper", session.getMapper(GxpAuditPolicyActivationMapper.class));
        inject(writer, "policyOperationMapper", session.getMapper(GxpAuditPolicyOperationMapper.class));
        var specialized = new MesProEdhrOperationAuditServiceImpl();
        inject(specialized, "auditEventMapper", session.getMapper(MesProEdhrOperationAuditEventMapper.class));
        tasks = new MesProEdhrWorkTaskServiceImpl();
        inject(tasks, "workTaskMapper", session.getMapper(MesProEdhrWorkTaskMapper.class));
        var realEntitlement = new SystemEntitlementServiceImpl();
        for (var f : realEntitlement.getClass().getDeclaredFields()) if(f.getType().getSimpleName().endsWith("Mapper"))
            inject(realEntitlement,f.getName(),session.getMapper(f.getType()));
        entitlement = (SystemEntitlementService) tx(realEntitlement);
        var realPermissions = new PermissionApiImpl(); inject(realPermissions,"entitlementService",entitlement);
        permissionBoundary = spy(realPermissions);
        inject(tasks, "permissionApi", permissionBoundary);
        // Real PermissionApi delegates claim changes to the transactional entitlement service.
        inject(tasks, "auxiliaryAudit", new MesWorkTaskAuxiliaryAudit(dataSource, (GxpAuditService) tx(writer)));
        signatures = session.getMapper(ElectronicSignatureRecordMapper.class);
        signatureQuery = new ElectronicSignatureQueryServiceImpl();
        inject(signatureQuery, "signatureRecordMapper", signatures);
        inject(signatureQuery, "subjectAdapters", List.of(new MesBatchRecordSignatureSubjectAdapter()));
        signer = mock(MesProBatchRecordExecutionSignatureService.class);
        when(signer.recordQaDispositionSignature(any(), any(), any(), any(), any())).thenAnswer(call ->
                insertSignature(call.getArgument(0), call.getArgument(1), call.getArgument(3), call.getArgument(4)));
        var target = new MesProEdhrNonconformanceReviewServiceImpl();
        { org.springframework.test.util.ReflectionTestUtils.setField(target, "handoffService", org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffService.class)); }
        Map<String, Class<?>> dependencies = Map.ofEntries(
                Map.entry("reviewMapper", MesProEdhrNonconformanceReviewMapper.class),
                Map.entry("reviewCounterMapper", MesProEdhrNonconformanceReviewCounterMapper.class),
                Map.entry("workOrderMapper", MesProWorkOrderMapper.class),
                Map.entry("batchExecutionMapper", MesProEdhrBatchExecutionMapper.class),
                Map.entry("batchExecutionOriginMapper", MesProEdhrBatchExecutionOriginMapper.class),
                Map.entry("activeOrderMapper", MesProcessPoolActiveOrderMapper.class),
                Map.entry("releaseApplicationMapper", MesProcessPoolActiveOrderReleaseApplicationMapper.class),
                Map.entry("processSnapshotMapper", MesProcessPoolActiveOrderProcessSnapshotMapper.class),
                Map.entry("pqcInspectionTaskMapper", MesPqcInspectionTaskMapper.class),
                Map.entry("workTaskMapper", MesProEdhrWorkTaskMapper.class),
                Map.entry("releaseTransactionMapper", MesProEdhrReleaseTransactionMapper.class),
                Map.entry("fileMapper", FileMapper.class), Map.entry("auditReceiptMapper", GxpAuditEventMapper.class));
        dependencies.forEach((name, type) -> inject(target, name, session.getMapper(type)));
        inject(target, "signatureService", signer);
        inject(target, "signatureRecordMapper", signatures);
        inject(target, "signatureQueryService", signatureQuery);
        inject(target, "operationAuditService", tx(specialized));
        inject(target, "workTaskService", tx(tasks));
        inject(target, "reworkCycleService", tx(new MesActiveOrderReworkCycleService(
                session.getMapper(MesProcessPoolActiveOrderMapper.class),
                session.getMapper(MesProcessPoolActiveOrderProcessSnapshotMapper.class),
                session.getMapper(MesPqcInspectionTaskMapper.class))));
        inject(target, "unifiedAudit", tx(writer));
        service = (MesProEdhrNonconformanceReviewServiceImpl) tx(target);
        inject(tasks,"batchExecutionMapper",session.getMapper(MesProEdhrBatchExecutionMapper.class));
        inject(tasks,"workOrderMapper",session.getMapper(MesProWorkOrderMapper.class));
        inject(tasks,"nonconformanceReviewService",service);
        TenantContextHolder.setTenantId(1L);
        var actor = new LoginUser();
        actor.setId(21L);
        actor.setTenantId(1L);
        actor.setUserType(2);
        actor.setInfo(Map.of("username", "ncr-transaction-fixture", LoginUser.INFO_KEY_NICKNAME, "NCR fixture"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor, null, List.of()));
        jdbc.update("INSERT INTO gxp_audit_ledger_sequence(tenant_id,next_ledger_sequence) VALUES(1,1)");
        jdbc.update("INSERT INTO gxp_audit_policy_activation(id,tenant_id,policy_version) VALUES(1,1,'ncr-scope-fixture')");
        for (String operation : List.of(CREATE, "mes.nonconformance.rework", "mes.nonconformance.void", "mes.nonconformance.concession", "mes.work-task.entitlement")) {
            jdbc.update("INSERT INTO gxp_audit_policy_operation(tenant_id,policy_version,operation_id,domain,"
                    + "subject_type,action_type,reason_policy,signature_policy,state_policy,applicability,active)"
                    + " VALUES(1,'ncr-scope-fixture',?,'MES','NONCONFORMANCE_REVIEW',?,'USER_REQUIRED',?,?,'GXP',1)",
                    operation, CREATE.equals(operation) ? "CREATE" : "UPDATE",
                    CREATE.equals(operation) ? "NONE" : "REQUIRED",
                    CREATE.equals(operation) ? "ABSENT_TO_PRESENT" : "PRESENT_TO_PRESENT");
        }
        jdbc.update("UPDATE gxp_audit_policy_operation SET subject_type='MES_WORK_TASK',reason_policy='SYSTEM',signature_policy='NONE' WHERE operation_id='mes.work-task.entitlement'");
        for(String policy:List.of("MES_EDHR_FILLER_MINIMAL","MES_EDHR_APPROVAL_REVIEWER_MINIMAL","MES_EDHR_RELEASE_APPROVER_MINIMAL"))
            jdbc.update("INSERT INTO system_entitlement_policy(policy_code,status,allowed_permission_codes_json,forbidden_permission_codes_json) VALUES(?,0,'[\"mes:task:fill\"]','[]')",policy);
        jdbc.update("INSERT INTO system_menu(id,permission,status) VALUES(1,'mes:task:fill',0)");
        jdbc.update("INSERT INTO system_users(id,tenant_id,status,nickname) VALUES(21,1,0,'QA'),(22,1,0,'Original PQC'),(23,1,0,'Candidate')");
        jdbc.update("INSERT INTO " + WORK + "(id,tenant_id,code,batch_code,temporary_frozen)"
                + " VALUES(3001,1,'WO-NCR','BATCH-NCR',0)");
        jdbc.update("INSERT INTO " + ACTIVE + "(id,tenant_id,work_order_id,leader_user_id,route_id,route_version_id,"
                + "qa_regulation_version_id,erp_fixed_quantity_snapshot,active_status,business_status,version)"
                + " VALUES(8101,1,3001,3002,4001,4002,5001,10,'ACTIVE','ACTIVE',1)");
        jdbc.update("INSERT INTO " + BATCH + "(id,tenant_id,work_order_id,batch_execution_code,work_order_code,batch_code,status)"
                + " VALUES(9001,1,3001,'EXEC-NCR','WO-NCR','BATCH-NCR',10)");
        FAILURE_SAW_WRITES.set(false);
        expectedDisposition = null;
    }

    @AfterEach
    void close() throws SQLException {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
        if (jdbc != null) {
            // SHUTDOWN closes its connection before JdbcTemplate's post-execute getWarnings.
            // Native JDBC avoids querying that closed statement; shutdown errors still propagate.
            try (Connection connection = Objects.requireNonNull(jdbc.getDataSource()).getConnection();
                 var statement = connection.createStatement()) {
                statement.execute("SHUTDOWN");
            }
        }
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"direct,open","direct,completed","direct,application","direct,released",
            "erp,open","erp,completed","erp,application","erp,released"})
    void workOrderCancellationUsesRealCycleMapperHandoffAndAuditAndPreservesHistory(String entry, String state) {
        jdbc.update("UPDATE "+WORK+" SET status=1 WHERE id=3001");
        jdbc.update("INSERT INTO "+ACTIVE+"(id,tenant_id,work_order_id,active_status,business_status,version) VALUES(8102,1,3001,'REMOVED','COMPLETED',7)");
        String handoffTable="mes_active_order_handoff_task";
        for(long id:List.of(8201L,8202L,8203L)) jdbc.update("INSERT INTO "+handoffTable
                +"(id,tenant_id,active_order_id,work_order_id,status,row_version) VALUES(?,1,?,3001,?,0)",
                id,id==8203L?8102L:8101L,id==8202L?"DONE":"TODO");
        if(state.equals("completed")) jdbc.update("UPDATE "+ACTIVE+" SET business_status='COMPLETED' WHERE id=8101");
        if(state.equals("application")||state.equals("released")) jdbc.update("INSERT INTO "+APPLICATION
                +"(id,tenant_id,active_order_id,work_order_id,application_status,version) VALUES(8301,1,8101,3001,?,1)",
                state.equals("released")?"RELEASED":"BATCH_OPEN");
        jdbc.update("INSERT INTO gxp_audit_policy_operation(tenant_id,policy_version,operation_id,domain,subject_type,action_type,reason_policy,signature_policy,state_policy,applicability,active)"
                +" VALUES(1,'ncr-scope-fixture','mes.active-order-handoff.task-closed','MES','MES_HANDOFF_TASK','UPDATE','SYSTEM','NONE','PRESENT_TO_PRESENT','GXP',1)");
        var realAudit=new cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffAudit();
        inject(realAudit,"audit",ReflectionTestUtils.getField(service,"unifiedAudit"));
        var handoff=new cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffService();
        inject(handoff,"tasks",session.getMapper(cn.iocoder.yudao.module.mes.dal.mysql.pro.handoff.MesActiveOrderHandoffTaskMapper.class));
        inject(handoff,"audit",realAudit);
        var work=new cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderServiceImpl();
        inject(work,"workOrderMapper",session.getMapper(MesProWorkOrderMapper.class));
        inject(work,"gxpAuditService",ReflectionTestUtils.getField(service,"unifiedAudit"));
        inject(work,"activeOrderMapper",session.getMapper(MesProcessPoolActiveOrderMapper.class));
        inject(work,"handoffService",tx(handoff));
        inject(work,"releaseStateService",new cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesReportAllocationReleaseStateService(
                session.getMapper(MesProcessPoolActiveOrderReleaseApplicationMapper.class),session.getMapper(MesProEdhrReleaseTransactionMapper.class)));
        var oldAllocationBoundary=mock(cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesReportAllocationOrderChangeService.class);
        var oldTaskBoundary=mock(cn.iocoder.yudao.module.mes.service.pro.task.MesProTaskService.class);
        inject(work,"reportAllocationOrderChangeService",oldAllocationBoundary);inject(work,"taskService",oldTaskBoundary);
        var transactional=(cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderServiceImpl)tx(work);
        Runnable cancel=()->transactional.cancelWorkOrder(3001L);
        if(entry.equals("erp")) {
            // Update another real work order in the ERP outer transaction before its later VOID cancellation.
            jdbc.update("INSERT INTO "+WORK+"(id,tenant_id,code,status,product_id,batch_code,quantity) VALUES(3002,1,'NORMAL',1,44,'BATCH-NORMAL',5)");
            var client=mock(cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProductionOrderClient.class);
            var configService=mock(cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService.class);
            var properties=new cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties();
            properties.setBaseUrl("https://fixture.invalid");properties.setAcctId("fixture");properties.setUsername("fixture");
            properties.setPassword("fixture-only");properties.setLcid(2052);
            when(configService.getEffectiveProperties()).thenReturn(properties);
            var normal=new cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProductionOrder();
            normal.setFid("NORMAL-FID");normal.setBillNo("NORMAL");normal.setMaterialNumber("MAT");
            normal.setQuantity(new BigDecimal("12"));normal.setBatchNumber("BATCH-NORMAL");
            normal.setDocumentStatus("C");normal.setStatus("2");
            normal.setPlannedStartDate(LocalDateTime.of(2026,10,5,0,0));normal.setPlannedEndDate(LocalDateTime.of(2026,10,6,0,0));
            when(client.fetchProductionOrdersByBillDateRange(any(),any(),any())).thenReturn(List.of(normal));
            var voided=new cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProductionOrder();
            voided.setBillNo("VOIDED");voided.setDocumentStatus("Z");
            when(client.fetchProductionOrdersByBillNos(any(),any())).thenReturn(List.of(voided));
            var records=mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesKingdeeProductionOrderSyncRecordMapper.class);
            when(records.selectList()).thenReturn(List.of(new cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesKingdeeProductionOrderSyncRecordDO()
                    .setId(900L).setSourceBillNo("VOIDED").setWorkOrderId(3001L)));
            var items=mock(cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper.class);
            when(items.selectByCode("MAT")).thenReturn(new cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO().setId(44L));
            var sync=new cn.iocoder.yudao.module.mes.service.pro.workorder.sync.MesKingdeeProductionOrderSyncServiceImpl(client,configService,transactional,
                    session.getMapper(MesProWorkOrderMapper.class),records,
                    mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper.class),
                    mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderDiffMapper.class),items,
                    mock(cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemTypeMapper.class),
                    mock(cn.iocoder.yudao.module.mes.dal.mysql.md.unitmeasure.MesMdUnitMeasureMapper.class),
                    session.getMapper(MesProcessPoolActiveOrderMapper.class),
                    mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper.class));
            inject(sync,"gxpAuditService",ReflectionTestUtils.getField(service,"unifiedAudit"));
            var outer=(cn.iocoder.yudao.module.mes.service.pro.workorder.sync.MesKingdeeProductionOrderSyncServiceImpl)tx(sync);
            cancel=outer::syncWorkOrders;
        }
        var before=snapshot();cancellationLocks.clear();
        if(!state.equals("open")) {
            Runnable observedCancel=cancel;
            assertThrows(RuntimeException.class,()->runObservedCancellation(observedCancel));
            assertLedgerBeforeBusinessLocks(entry);
            assertEquals(before,snapshot());verifyNoInteractions(oldAllocationBoundary,oldTaskBoundary);return;
        }
        runObservedCancellation(cancel);assertLedgerBeforeBusinessLocks(entry);
        if(entry.equals("erp")) assertEquals(0,new BigDecimal("12").compareTo(
                jdbc.queryForObject("SELECT quantity FROM "+WORK+" WHERE id=3002",BigDecimal.class)));
        var cycle=session.getMapper(MesProcessPoolActiveOrderMapper.class).selectById(8101L);
        assertEquals("REMOVED",cycle.getActiveStatus());assertEquals("CANCELED",cycle.getBusinessStatus());assertEquals(2,cycle.getVersion());
        assertEquals(3,session.getMapper(MesProWorkOrderMapper.class).selectById(3001L).getStatus());
        assertEquals("CANCELED",jdbc.queryForObject("SELECT status FROM "+handoffTable+" WHERE id=8201",String.class));
        assertEquals(3001L,jdbc.queryForObject("SELECT completion_source_id FROM "+handoffTable+" WHERE id=8201",Long.class));
        assertEquals("DONE",jdbc.queryForObject("SELECT status FROM "+handoffTable+" WHERE id=8202",String.class));
        assertEquals("TODO",jdbc.queryForObject("SELECT status FROM "+handoffTable+" WHERE id=8203",String.class));
        assertEquals(7,session.getMapper(MesProcessPoolActiveOrderMapper.class).selectById(8102L).getVersion());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM gxp_audit_event WHERE operation_id='mes.active-order-handoff.task-closed'",Integer.class));
    }

    @Test
    void unsignedCreateRelationFailureRollsBackReviewCounterFreezeAndBothAudits() {
        jdbc.update("INSERT INTO mes_pro_edhr_batch_execution_origin(id,tenant_id,batch_execution_id,"
                + "active_order_id,work_order_id,entry_type) VALUES(9101,1,9001,8101,3001,'ACTIVE_ORDER_COMPLETION')");
        Map<String, List<Map<String, Object>>> before = snapshot();
        installFailureTrigger();
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        assertThrows(RuntimeException.class, () -> service.create(new MesProEdhrNonconformanceReviewCreateReqVO()
                .setActiveOrderId(8101L).setNonconformanceReason("Observed nonconformance")));
        assertTrue(FAILURE_SAW_WRITES.get(), "Failure must observe real review/counter/freeze/specialized/event/relation writes");
        assertRestored(before);
        verifyNoInteractions(signer);
    }

    @ParameterizedTest
    @ValueSource(strings = {"rework", "void"})
    void dispositionRelationFailureRollsBackReleaseTasksSignatureCycleAndBothAudits(String disposition) {
        seedDisposition();
        expectedDisposition = disposition;
        Map<String, List<Map<String, Object>>> before = snapshot();
        installFailureTrigger();
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        assertThrows(RuntimeException.class, () -> service.dispose(new MesProEdhrNonconformanceReviewDisposeReqVO()
                .setId(1001L).setDisposition(disposition).setReviewOpinion("Observed disposition")
                .setSignaturePassword("test-only-issuance-boundary")
                .setReviewMaterials(List.of(new MesProEdhrNonconformanceReviewDisposeReqVO.ReviewMaterialReqVO()
                        .setFileId(9102L).setUrl("https://fixture.invalid/review.pdf").setFileName("review.pdf").setSortNo(1)))));
        assertTrue(FAILURE_SAW_WRITES.get(), "Failure must follow actual downstream and audit writes, never setup failure");
        assertRestored(before);
        verify(permissionBoundary).revokeEntitlementSource(any());
    }

    private void seedDisposition() {
        jdbc.update("UPDATE " + WORK + " SET temporary_frozen=1 WHERE id=3001");
        jdbc.update("UPDATE " + BATCH + " SET status=15 WHERE id=9001");
        jdbc.update("UPDATE " + ACTIVE + " SET business_status='COMPLETED' WHERE id=8101");
        jdbc.update("INSERT INTO " + REVIEW + "(id,tenant_id,source_type,source_id,active_order_id,"
                + "work_order_id,batch_execution_id,review_status,previous_batch_status,previous_work_order_temporary_frozen,"
                + "frozen_at,nonconformance_reason) VALUES(1001,1,'ACTIVE_ORDER',8101,8101,3001,9001,"
                + "'pending_review',10,0,'2026-09-29 08:00:00','Observed nonconformance')");
        jdbc.update("INSERT INTO " + APPLICATION + "(id,tenant_id,active_order_id,work_order_id,application_status,"
                + "version,pqc_release_work_task_id,release_approval_work_task_id,release_transaction_id)"
                + " VALUES(7001,1,8101,3001,'MANAGER_RELEASE_PENDING',1,8001,8002,9301)");
        jdbc.update("INSERT INTO " + TASK + "(id,tenant_id,task_type,business_scope_type,business_scope_id,status)"
                + " VALUES(8001,1,'PQC_PRODUCTION_RELEASE','RELEASE_APPLICATION',7001,'TODO')");
        jdbc.update("INSERT INTO " + TASK + "(id,tenant_id,task_type,business_scope_type,business_scope_id,status,candidate_user_snapshot)"
                + " VALUES(8002,1,'RELEASE_APPROVE','RELEASE_TRANSACTION',9301,'TODO','[21]')");
        jdbc.update("INSERT INTO " + RELEASE + "(id,tenant_id,release_status,version)"
                + " VALUES(9301,1,'PENDING_APPROVAL',4)");
        jdbc.update("INSERT INTO infra_file(id,config_id,name,path,url,size,type)"
                + " VALUES(9102,10,'review.pdf','mes/edhr-ncr/reviews/1001/upload-1/review.pdf',"
                + "'https://fixture.invalid/review.pdf',10,'application/pdf')");
        for (long i = 1; i <= 2; i++) {
            jdbc.update("INSERT INTO " + SNAPSHOT + "(id,tenant_id,active_order_id,work_order_id,route_id,route_version_id,"
                    + "route_process_id,production_config_snapshot_json) VALUES(?,1,8101,3001,4001,4002,?,?)",
                    4100 + i, 5100 + i, "{\"frozen\":" + i + "}");
            jdbc.update("INSERT INTO " + PQC + "(id,tenant_id,active_order_id,work_order_id,route_id,route_version_id,"
                    + "route_process_id,task_status,actual_inspection_quantity,submitted_event_id,submitted_content_hash)"
                    + " VALUES(?,1,8101,3001,4001,4002,?,'CONFIRMED',10,?,'prior-inspection')",
                    6100 + i, 5100 + i, 7100 + i);
        }
    }

    private Long insertSignature(Long actor, Long review, String reason, String aggregateHash) {
        String action = "QA_DISPOSITION";
        String subject = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(0L, action,
                null, null, null, null, null, null, null, "EDHR_NONCONFORMANCE_REVIEW", review,
                "eDHR不合格评审处置", action, null, null, aggregateHash, null);
        String version = MesBatchRecordSignatureSubjectAdapter.subjectVersion(subject);
        return insertFormalSignature(9101L, actor, action, subject, version, reason);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"false,rework","false,void","true,rework","true,void"})
    void qaClosurePreservesPqcAndProjectsFormalActorWithRealNcrMapperAndDoneSql(boolean approved,String disposition) {
        seedDisposition();
        jdbc.update("UPDATE "+APPLICATION+" SET batch_execution_id=9001,applied_at='2026-09-29 07:00:00',application_status=? WHERE id=7001", approved?"MANAGER_RELEASE_PENDING":"PQC_RELEASE_PENDING");
        jdbc.update("UPDATE "+TASK+" SET batch_execution_id=9001,work_order_id=3001,assignee_user_id=23,candidate_user_snapshot='22,23',task_code='PQC-SA28' WHERE id=8001");
        String receipt="{\"original\":\"unchanged\"}";
        if(approved) {
            String subject=MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(9001L,"PQC_RELEASE",null,null,null,null,null,null,null,
                    "PQC_RELEASE_APPLICATION",7001L,"PQC生产放行","PQC_RELEASE",null,null,null,null);
            insertFormalSignature(9201L,22L,"PQC_RELEASE",subject,MesBatchRecordSignatureSubjectAdapter.subjectVersion(subject),"PQC approved");
            receipt=JsonUtils.toJsonString(Map.of("applicationId",7001L,"pqcReleaseWorkTaskId",8001L,"batchExecutionId",9001L,
                    "decidedBy",22L,"decision","APPROVE","signatureId",9201L));
            jdbc.update("UPDATE "+APPLICATION+" SET pqc_decision='APPROVE',pqc_decided_by=22,pqc_decided_at='2026-09-29 09:00:00',dossier_summary_json=? WHERE id=7001",receipt);
            jdbc.update("UPDATE "+TASK+" SET status='DONE',reason='APPROVE',completed_at='2026-09-29 09:00:00' WHERE id=8001");
        } else jdbc.update("UPDATE "+APPLICATION+" SET dossier_summary_json=? WHERE id=7001",receipt);
        service.dispose(dispositionRequest(disposition));
        var applications=session.getMapper(MesProcessPoolActiveOrderReleaseApplicationMapper.class);
        var taskMapper=session.getMapper(MesProEdhrWorkTaskMapper.class);
        var application=applications.selectById(7001L); var task=taskMapper.selectById(8001L);
        String closure="NONCONFORMANCE_"+disposition.toUpperCase(java.util.Locale.ROOT);
        assertEquals(closure,application.getApplicationStatus()); assertEquals(1001L,application.getQaClosureReviewId());
        assertEquals(receipt,application.getDossierSummaryJson());
        var detailService = closureDetailReader();
        var detail = detailService.getFormalDetail(8101L);
        var archived = archivedPqcDetail(detailService, application);
        assertEquals(detail.getActiveOrderStatus().getStatus(),archived.getActiveOrderStatus().getStatus());
        if(approved)assertEquals(9201L,archived.getPqcProductionRelease().getSignature().getSignatureId());
        else assertNull(archived.getPqcProductionRelease());
        assertEquals(closure, detail.getActiveOrderStatus().getStatus());
        assertEquals("void".equals(disposition)?"已作废":"已返工关闭", detail.getActiveOrderStatus().getStatusLabel());
        assertEquals("ACTIVE_ORDER", session.getMapper(MesProEdhrNonconformanceReviewMapper.class).selectById(1001L).getSourceType());
        if (approved) assertEquals(9201L, detail.getPqcProductionRelease().getSignature().getSignatureId());
        else assertNull(detail.getPqcProductionRelease());
        verifyOtherPersistedClosureSourceBindings(detailService, application);
        for (String mismatch : List.of("source_id=999", "active_order_id=999", "work_order_id=999", "tenant_id=2", "review_status='pending_review'", "batch_execution_id=999", "source_type='UNKNOWN'")) {
            jdbc.update("UPDATE " + REVIEW + " SET " + mismatch + " WHERE id=1001");
            assertThrows(IllegalStateException.class, () -> detailService.getFormalDetail(8101L), mismatch);
            jdbc.update("UPDATE " + REVIEW + " SET source_id=8101,active_order_id=8101,work_order_id=3001,tenant_id=1,review_status='closed',batch_execution_id=9001,source_type='ACTIVE_ORDER' WHERE id=1001");
        }

        assertEquals(approved?"APPROVE":null,application.getPqcDecision());
        assertEquals(approved?22L:null,application.getPqcDecidedBy());
        assertEquals(approved?"APPROVE":closure,task.getReason()); assertEquals(23L,task.getAssigneeUserId());
        assertEquals("22,23",task.getCandidateUserSnapshot());
        var resolver=new MesManagerReleaseCompletedActorResolver(taskMapper,session.getMapper(MesProEdhrReleaseTransactionMapper.class));
        inject(resolver,"applicationMapper",applications);inject(resolver,"users",session.getMapper(AdminUserMapper.class));
        inject(resolver,"signatures",signatureQuery);inject(resolver,"nonconformanceReviews",session.getMapper(MesProEdhrNonconformanceReviewMapper.class));
        Long actor=approved?22L:21L;
        assertEquals(actor,resolver.resolvePqc(8001L,"RELEASE_APPLICATION",7001L,9001L,3001L,task.getReason(),task.getCompletedAt()));
        var req=new MesProEdhrWorkTaskPageReqVO();
        assertEquals(List.of(8001L),taskMapper.selectDonePage(req,actor).getList().stream().map(MesProEdhrWorkTaskDO::getId).toList());
        assertTrue(taskMapper.selectDonePage(req,23L).getList().isEmpty());
        var adapter=new MesProEdhrApprovalTaskAdapter(tasks,mock(MesProEdhrReleaseService.class),resolver);
        cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskSummary summary=ReflectionTestUtils.invokeMethod(adapter,"toSummary",
                cn.iocoder.yudao.framework.common.util.object.BeanUtils.toBean(task,MesProEdhrWorkTaskRespVO.class));
        cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineEntry timeline=ReflectionTestUtils.invokeMethod(adapter,"toTimelineEntry",task);
        assertEquals(actor,summary.getAssigneeUserId());assertEquals(actor,timeline.getActorUserId());
        assertEquals(approved?"APPROVED":closure,timeline.getAction());
        if(!approved) { assertNull(summary.getApprovalResult());assertTrue(timeline.getActionLabel().contains("QA")); }
        assertEquals("VALID",signatureQuery.verifyEvidence(9101L).verificationStatus());
        if(approved) assertEquals("VALID",signatureQuery.verifyEvidence(9201L).verificationStatus());
        jdbc.update("UPDATE "+APPLICATION+" SET qa_closure_review_id=9999 WHERE id=7001");
        assertThrows(IllegalStateException.class,()->resolver.resolvePqc(8001L,"RELEASE_APPLICATION",7001L,9001L,3001L,task.getReason(),task.getCompletedAt()));
        assertTrue(taskMapper.selectDonePage(req,actor).getList().isEmpty());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"concession_release"})
    void actualQaRecoveryRestoresDossierUpload(String disposition) {
        seedDisposition();
        jdbc.update("DELETE FROM "+APPLICATION);jdbc.update("UPDATE "+ACTIVE+" SET business_status='ACTIVE' WHERE id=8101");
        var active=session.getMapper(MesProcessPoolActiveOrderMapper.class).selectById(8101L);
        var files=mock(cn.iocoder.yudao.module.infra.service.file.FileService.class);
        var relations=mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderDossierFileMapper.class);
        var users=mock(cn.iocoder.yudao.module.system.api.user.AdminUserApi.class);
        when(users.getUser(21L)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(21L).setNickname("QA"));
        var dossier=new cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesActiveOrderDossierFileService(
                session.getMapper(MesProcessPoolActiveOrderReleaseApplicationMapper.class),session.getMapper(MesProcessPoolActiveOrderMapper.class),
                relations,session.getMapper(MesProEdhrNonconformanceReviewMapper.class),users,files,mock(MesProEdhrOperationAuditService.class));
        inject(dossier,"nonconformanceReviewService",service);inject(dossier,"gxpAuditService",mock(GxpAuditService.class));
        var scope=mock(cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesActiveOrderDossierReadScopeService.class);
        when(scope.requireMutation(21L,8101L,null,false)).thenReturn(new cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesActiveOrderDossierReadScopeService.Context(active,null));
        inject(dossier,"dossierReadScopeService",scope);
        byte[] content="real-test-pdf".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var upload=new cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesActiveOrderDossierFileService.UploadCommand(8101L,null,"OTHER_FILE","recovery.pdf","application/pdf",content);
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->dossier.upload(21L,upload));
        verifyNoInteractions(files,relations);
        service.dispose(dispositionRequest(disposition));
        assertFalse(session.getMapper(MesProWorkOrderMapper.class).selectById(3001L).getTemporaryFrozen());
        assertEquals(disposition,session.getMapper(MesProEdhrNonconformanceReviewMapper.class).selectById(1001L).getDisposition());
        when(files.createFileAndReturnId(any(),any(),any(),any())).thenReturn(9501L);
        when(files.getFile(9501L)).thenReturn(FileDO.builder().id(9501L).configId(1L).name("recovery.pdf")
                .path("mes/active-order-dossier/8101/OTHER_FILE/recovery.pdf").url("https://fixture.invalid/recovery.pdf").type("application/pdf").size((long)content.length).build());
        when(relations.insert(any(cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDossierFileDO.class))).thenAnswer(call->{
            call.getArgument(0,cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDossierFileDO.class).setId(9502L);return 1;
        });
        assertEquals(9501L,dossier.upload(21L,upload).fileId());
        verify(relations).insert(any(cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDossierFileDO.class));
    }

    /** Reader contract for the other current producer source identities; ACTIVE_ORDER disposal above is real. */
    private void verifyOtherPersistedClosureSourceBindings(
            cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderDetailServiceImpl reader,
            MesProcessPoolActiveOrderReleaseApplicationDO application) {
        createTable(cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO.class);
        session.getConfiguration().addMapper(cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper.class);
        inject(reader,"eventMapper",session.getMapper(cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper.class));
        var pqcSource=spy(session.getMapper(MesPqcInspectionTaskMapper.class));
        // Unrelated detail rows stay outside this closure contract; exact source query still uses H2.
        doReturn(List.of()).when(pqcSource).selectListByActiveOrderId(any());
        inject(reader,"pqcTaskMapper",pqcSource);
        jdbc.update("INSERT INTO mes_pro_process_pool_event(id,tenant_id,event_type,work_order_id) VALUES(9301,1,'PQC_INSPECTION',3001)");
        jdbc.update("INSERT INTO mes_pqc_inspection_task(id,tenant_id,submitted_event_id,work_order_id,active_order_id) VALUES(9302,1,9301,3001,8101)");
        for (var source : Map.of("PQC_RELEASE",7001L,"DEVIATION",9001L,"PQC_SUBMISSION",9301L).entrySet()) {
            jdbc.update("UPDATE "+REVIEW+" SET source_type=?,source_id=? WHERE id=1001",source.getKey(),source.getValue());
            assertEquals(application.getApplicationStatus(),archivedPqcDetail(reader,application).getActiveOrderStatus().getStatus());
            jdbc.update("UPDATE "+REVIEW+" SET source_id=999 WHERE id=1001");
            assertThrows(IllegalStateException.class,()->archivedPqcDetail(reader,application),source.getKey());
        }
        jdbc.update("UPDATE "+REVIEW+" SET source_type='PQC_SUBMISSION',source_id=9301 WHERE id=1001");
        for (String mismatch : List.of("tenant_id=2","work_order_id=999","active_order_id=999")) {
            jdbc.update("UPDATE mes_pqc_inspection_task SET "+mismatch+" WHERE id=9302");
            assertThrows(IllegalStateException.class,()->archivedPqcDetail(reader,application),mismatch);
            jdbc.update("UPDATE mes_pqc_inspection_task SET tenant_id=1,work_order_id=3001,active_order_id=8101 WHERE id=9302");
        }
        jdbc.update("UPDATE "+REVIEW+" SET source_type='ACTIVE_ORDER',source_id=8101 WHERE id=1001");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans={false,true})
    void independentBatchVoidThenActualArchivedDetailPreservesPriorPqc(boolean approved) throws Exception {
        createTable(MesProEdhrRecordChangeEventDO.class);createTable(MesProEdhrBatchExecutionArchiveDO.class);
        var config=session.getConfiguration();
        var tenantPlugin=new com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor();
        new cn.iocoder.yudao.framework.tenant.config.YudaoTenantAutoConfiguration().tenantLineInnerInterceptor(
                new cn.iocoder.yudao.framework.tenant.config.TenantProperties(),tenantPlugin);config.addInterceptor(tenantPlugin);
        config.addMapper(MesProEdhrRecordChangeEventMapper.class);config.addMapper(MesProEdhrBatchExecutionArchiveMapper.class);
        jdbc.update("INSERT INTO "+APPLICATION+"(id,tenant_id,active_order_id,work_order_id,batch_execution_id,application_status,version,pqc_release_work_task_id,applied_at) VALUES(7001,1,8101,3001,9001,?,1,8001,CURRENT_TIMESTAMP)",
                approved?"MANAGER_RELEASE_PENDING":"PQC_RELEASE_PENDING");
        jdbc.update("INSERT INTO "+TASK+"(id,tenant_id,task_type,business_scope_type,business_scope_id,work_order_id,status,candidate_user_snapshot) VALUES(8001,1,'PQC_PRODUCTION_RELEASE','RELEASE_APPLICATION',7001,3001,?,'22')",approved?"DONE":"TODO");
        if(approved) {
            String subject=MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(9001L,"PQC_RELEASE",null,null,null,null,null,null,null,
                    "PQC_RELEASE_APPLICATION",7001L,"PQC生产放行","PQC_RELEASE",null,null,null,null);
            insertFormalSignature(9201L,22L,"PQC_RELEASE",subject,MesBatchRecordSignatureSubjectAdapter.subjectVersion(subject),"PQC approved");
            jdbc.update("UPDATE "+APPLICATION+" SET pqc_decision='APPROVE',pqc_decided_by=22,pqc_decided_at=CURRENT_TIMESTAMP,dossier_summary_json=? WHERE id=7001",
                    JsonUtils.toJsonString(Map.of("applicationId",7001L,"pqcReleaseWorkTaskId",8001L,"batchExecutionId",9001L,"decidedBy",22L,"decision","APPROVE","signatureId",9201L)));
        }
        var appMapper=session.getMapper(MesProcessPoolActiveOrderReleaseApplicationMapper.class);
        String before=JsonUtils.toJsonString(appMapper.selectById(7001L));
        var writer=new MesProEdhrBatchVoidEffectServiceImpl();
        for(var f:writer.getClass().getDeclaredFields())if(!Modifier.isStatic(f.getModifiers()))inject(writer,f.getName(),mock(f.getType()));
        var changes=session.getMapper(MesProEdhrRecordChangeEventMapper.class);
        inject(writer,"batchExecutionMapper",session.getMapper(MesProEdhrBatchExecutionMapper.class));
        inject(writer,"batchArchiveMapper",session.getMapper(MesProEdhrBatchExecutionArchiveMapper.class));
        inject(writer,"changeEventMapper",changes);inject(writer,"releaseApplications",appMapper);inject(writer,"workTaskService",tasks);
        var guard=new MesEdhrBatchLifecycleGuard();inject(guard,"reviewMapper",session.getMapper(MesProEdhrNonconformanceReviewMapper.class));
        inject(guard,"batchMapper",session.getMapper(MesProEdhrBatchExecutionMapper.class));inject(guard,"changeMapper",changes);inject(writer,"lifecycleGuard",guard);
        when(signer.recordBatchVoidRequestSignature(any(),any(),any(),any(),any())).thenReturn(9401L);inject(writer,"signatureService",signer);
        var actual=(MesProEdhrBatchVoidEffectService)tx(writer);
        var change=actual.executeDirectPlatformVoidBatchExecution(new cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangeRequestReqVO()
                .setBatchExecutionId(9001L).setReasonCategory("ORDER_CANCELLED").setReasonText("independent batch void").setPassword("test-only"),21L);
        assertEquals("EFFECTIVE",changes.selectById(change.getId()).getChangeStatus());
        assertEquals(60,session.getMapper(MesProEdhrBatchExecutionMapper.class).selectById(9001L).getStatus());
        var application=appMapper.selectById(7001L);
        // The actual writer closes pending applications only; approved PQC facts remain immutable.
        assertEquals(approved?"MANAGER_RELEASE_PENDING":"BATCH_VOIDED",application.getApplicationStatus());
        if(approved)assertEquals(before,JsonUtils.toJsonString(application));
        var reader=closureDetailReader();inject(reader,"recordChangeEventMapper",changes);
        var detail=archivedPqcDetail(reader,application);
        assertEquals(application.getApplicationStatus(),detail.getActiveOrderStatus().getStatus());
        if(approved)assertEquals(9201L,detail.getPqcProductionRelease().getSignature().getSignatureId());
        else {
            assertNull(detail.getPqcProductionRelease());assertEquals("已作废",detail.getActiveOrderStatus().getStatusLabel());
            for(String mismatch:List.of("batch_execution_id=999","change_status='SUBMITTED'","change_type='CORRECTION'","target_scope='CELL'","tenant_id=2")) {
                jdbc.update("UPDATE mes_pro_edhr_record_change_event SET "+mismatch+" WHERE id=?",change.getId());
                assertThrows(IllegalStateException.class,()->archivedPqcDetail(reader,application),mismatch);
                jdbc.update("UPDATE mes_pro_edhr_record_change_event SET batch_execution_id=9001,change_status='EFFECTIVE',change_type='VOID',target_scope='BATCH',tenant_id=1 WHERE id=?",change.getId());
            }
        }
    }

    private cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderDetail archivedPqcDetail(
            cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderDetailServiceImpl reader,
            MesProcessPoolActiveOrderReleaseApplicationDO application) {
        var authorization=mock(cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcProductionReleaseService.class);
        when(authorization.get(22L,application.getId())).thenReturn(new cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcProductionReleaseDecisionResult()
                .setStatus(application.getApplicationStatus()).setDecision(application.getPqcDecision()).setSignatureId("APPROVE".equals(application.getPqcDecision())?9201L:null));
        var materials=mock(cn.iocoder.yudao.module.mes.service.pro.workorder.kingdee.MesKingdeeProductionMaterialListQueryService.class);
        when(materials.getPage(any())).thenReturn(new cn.iocoder.yudao.framework.common.pojo.PageResult<>(List.of(),0L));
        var users=(cn.iocoder.yudao.module.system.service.user.AdminUserService)ReflectionTestUtils.getField(reader,"adminUserService");
        var entry=new cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcReleaseOrderDetailService(authorization,
                session.getMapper(MesProcessPoolActiveOrderReleaseApplicationMapper.class),session.getMapper(MesProcessPoolActiveOrderMapper.class),
                signatureQuery,users,reader,materials);
        return entry.get(22L,application.getId()).detail();
    }

    private cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderDetailServiceImpl closureDetailReader() {
        Class<cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderDetailServiceImpl> type =
                cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderDetailServiceImpl.class;
        var constructor = type.getConstructors()[0];
        Object[] args = Arrays.stream(constructor.getParameterTypes()).map(org.mockito.Mockito::mock).toArray();
        final cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderDetailServiceImpl reader;
        try { reader = (cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderDetailServiceImpl) constructor.newInstance(args); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        for (var field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            if (ReflectionTestUtils.getField(reader, field.getName()) == null) inject(reader, field.getName(), mock(field.getType()));
        }
        inject(reader,"activeOrderMapper",session.getMapper(MesProcessPoolActiveOrderMapper.class));
        inject(reader,"releaseApplicationMapper",session.getMapper(MesProcessPoolActiveOrderReleaseApplicationMapper.class));
        inject(reader,"nonconformanceReviewMapper",session.getMapper(MesProEdhrNonconformanceReviewMapper.class));
        inject(reader,"batchExecutionMapper",session.getMapper(MesProEdhrBatchExecutionMapper.class));
        inject(reader,"signatureQueryService",signatureQuery);
        var users=(cn.iocoder.yudao.module.system.service.user.AdminUserService) ReflectionTestUtils.getField(reader,"adminUserService");
        when(users.getUser(22L)).thenReturn(new AdminUserDO().setId(22L).setNickname("PQC reviewer"));
        var rows=(MesProcessPoolActiveOrderDetailReadMapper)ReflectionTestUtils.getField(reader,"detailReadMapper");
        when(rows.selectByActiveOrderId(8101L)).thenReturn(List.of(new MesTeamLeaderActiveOrderDetailReadDO()
                .setSnapshotId(4101L).setActiveOrderId(8101L).setWorkOrderId(3001L).setWorkOrderCode("WORK-3001")
                .setRouteName("frozen route").setRouteProcessId(5101L).setProcessId(6101L).setProcessCode("P-6101")
                .setProcessName("Production").setRequiredQuantity(BigDecimal.TEN).setKeyFlag(true)));
        return reader;
    }

    @Test void voidClosesOnlyActiveBatchFillAndReworkWithActualRuntimeClaimsAndPreservesDoneAndOtherBatch() {
        seedDisposition();
        for(long id:List.of(81001L,81002L,81003L,81004L)) {
            String type=id==81002L?"REWORK":"FILL";String status=id==81003L?"DONE":"TODO";
            jdbc.update("INSERT INTO "+TASK+"(id,tenant_id,batch_execution_id,work_order_id,task_type,status,candidate_user_snapshot,assignee_user_id) VALUES(?,1,?,3001,?,?,'23',23)",id,id==81004L?9002L:9001L,type,status);
            if(id!=81003L) entitlement.syncClaims(cn.iocoder.yudao.module.system.service.permission.bo.SystemEntitlementSyncCommand.builder()
                    .tenantId(1L).sourceType("EDHR_WORK_TASK_ASSIGNEE").sourceKey("WORK_TASK|"+id).sourceVersion("1")
                    .sourceDigest("formal-task").policyCode("MES_EDHR_FILLER_MINIMAL").resolvedUserIds(Set.of(23L))
                    .operatorUserId(21L).operatorUsername("QA").build());
        }
        assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM system_entitlement_claim WHERE status='ACTIVE'",Integer.class));
        service.dispose(dispositionRequest("void"));
        for(long id:List.of(81001L,81002L)) {
            assertEquals("CANCELED",jdbc.queryForObject("SELECT status FROM "+TASK+" WHERE id=?",String.class,id));
            assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM system_entitlement_claim WHERE source_key=? AND status='ACTIVE'",Integer.class,"WORK_TASK|"+id));
        }
        assertEquals("DONE",jdbc.queryForObject("SELECT status FROM "+TASK+" WHERE id=81003",String.class));
        assertEquals("TODO",jdbc.queryForObject("SELECT status FROM "+TASK+" WHERE id=81004",String.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM system_entitlement_claim WHERE status='ACTIVE'",Integer.class));
        assertTrue(entitlement.hasAnyPermission(23L,"mes:task:fill"),"unrelated formal source must retain its grant");
    }

    private MesProEdhrNonconformanceReviewDisposeReqVO dispositionRequest(String disposition) {
        return new MesProEdhrNonconformanceReviewDisposeReqVO().setId(1001L).setDisposition(disposition)
                .setReviewOpinion("Observed disposition").setSignaturePassword("test-only-issuance-boundary")
                .setReviewMaterials(List.of(new MesProEdhrNonconformanceReviewDisposeReqVO.ReviewMaterialReqVO()
                        .setFileId(9102L).setUrl("https://fixture.invalid/review.pdf").setFileName("review.pdf").setSortNo(1)));
    }

    private Long insertFormalSignature(Long signatureId, Long actor, String action, String subject, String version, String reason) {
        var adapter = new MesBatchRecordSignatureSubjectAdapter();
        var snapshot = adapter.loadAndAuthorize(new SignatureSubjectCommand(actor, "MES", action,
                "MES_BATCH_RECORD", subject, version, reason));
        String canonical = JsonUtils.toJsonString(sorted(JsonUtils.parseTree(snapshot.canonicalContentJson())));
        var definition = adapter.supportedActions().stream().filter(a -> action.equals(a.actionCode())).findFirst().orElseThrow();
        var time = LocalDateTime.of(2026, 9, 29, 9, 0);
        var record = ElectronicSignatureRecordDO.builder().id(signatureId).moduleCode("MES").actionCode(action)
                .subjectType("MES_BATCH_RECORD").subjectId(subject).subjectVersion(version).actorId(actor)
                .meaningCode(definition.meaningCode()).meaningLabel(definition.meaningLabel()).reason(reason)
                .signedAt(time).timeEvidenceId("SERVER_CLOCK:" + time).authenticationMethod("SESSION_PLUS_PASSWORD")
                .canonicalContentJson(canonical).contentHash(DigestUtil.sha256Hex(canonical)).algorithm("SHA-256")
                .keyVersion("system-local-v1").policyVersion(definition.policyVersion()).verificationStatus("VALID").build();
        record.setTenantId(1L);
        record.setEvidenceHash(DigestUtil.sha256Hex(String.join("|", "1", actor.toString(), "MES", action,
                "MES_BATCH_RECORD", subject, version, record.getMeaningCode(), record.getMeaningLabel(), reason,
                time.toString(), record.getTimeEvidenceId(), "SESSION_PLUS_PASSWORD", record.getContentHash(),
                "", "", "", "", "", "SHA-256", "system-local-v1", record.getPolicyVersion(), "VALID")));
        assertEquals(1, signatures.insert(record));
        assertEquals("VALID", signatureQuery.verifyEvidence(signatureId).verificationStatus());
        return record.getId();
    }

    private void installFailureTrigger() {
        jdbc.execute("CREATE TRIGGER fail_ncr_relation BEFORE INSERT ON gxp_audit_event_relation FOR EACH ROW CALL '"
                + FailAfterWrites.class.getName() + "'");
    }

    public static class FailAfterWrites implements Trigger {
        @Override public void fire(Connection connection, Object[] oldRow, Object[] newRow) throws SQLException {
            // Let the first real relation persist; fail the second so relation rollback is also observed.
            if (scalar(connection, "SELECT COUNT(*) FROM gxp_audit_event_relation") < 1) return;
            boolean common = scalar(connection, "SELECT COUNT(*) FROM gxp_audit_event") == 1
                    && scalar(connection, "SELECT COUNT(*) FROM " + SPECIALIZED) == 1
                    && scalar(connection, "SELECT next_ledger_sequence FROM gxp_audit_ledger_sequence") == 2;
            boolean changed;
            if (expectedDisposition == null) {
                changed = scalar(connection, "SELECT COUNT(*) FROM " + REVIEW + " WHERE review_status='pending_review'") == 1
                        && scalar(connection, "SELECT COUNT(*) FROM " + COUNTER + " WHERE current_serial=0") == 1
                        && scalar(connection, "SELECT temporary_frozen FROM " + WORK + " WHERE id=3001") == 1
                        && scalar(connection, "SELECT status FROM " + BATCH + " WHERE id=9001") == 15
                        && scalar(connection, "SELECT COUNT(*) FROM " + SIGNATURE) == 0;
            } else {
                boolean rework = "rework".equals(expectedDisposition);
                changed = scalar(connection, "SELECT COUNT(*) FROM " + REVIEW + " WHERE id=1001 AND review_status='closed'") == 1
                        && scalar(connection, "SELECT COUNT(*) FROM " + SIGNATURE) == 1
                        && scalar(connection, "SELECT COUNT(*) FROM " + APPLICATION
                            + " WHERE id=7001 AND application_status='NONCONFORMANCE_" + expectedDisposition.toUpperCase(java.util.Locale.ROOT) + "' AND version=2") == 1
                        && scalar(connection, "SELECT COUNT(*) FROM " + TASK + " WHERE id=8001 AND status='DONE'") == 1
                        && scalar(connection, "SELECT COUNT(*) FROM " + TASK + " WHERE id=8002 AND status='CANCELED'") == 1
                        && scalar(connection, "SELECT COUNT(*) FROM " + RELEASE
                            + " WHERE id=9301 AND release_status='REJECTED' AND version=5") == 1
                        && scalar(connection, "SELECT status FROM " + BATCH + " WHERE id=9001") == (rework ? 50 : 60)
                        && scalar(connection, "SELECT temporary_frozen FROM " + WORK + " WHERE id=3001") == (rework ? 0 : 1)
                        && scalar(connection, "SELECT COUNT(*) FROM " + ACTIVE + " WHERE rework_review_id=1001") == (rework ? 1 : 0)
                        && scalar(connection, "SELECT COUNT(*) FROM " + SNAPSHOT + " WHERE active_order_id<>8101") == (rework ? 2 : 0)
                        && scalar(connection, "SELECT COUNT(*) FROM " + PQC + " WHERE active_order_id<>8101"
                            + " AND task_status='PENDING' AND actual_inspection_quantity=0") == (rework ? 2 : 0)
                        && scalar(connection, "SELECT COUNT(*) FROM " + ACTIVE
                            + " WHERE id=8101 AND active_status='REMOVED' AND business_status='REWORKED' AND version=2") == (rework ? 1 : 0);
            }
            FAILURE_SAW_WRITES.set(common && changed);
            throw new SQLException("Injected NCR relation failure after actual domain and ledger writes");
        }
        private static long scalar(Connection connection, String query) throws SQLException {
            try (var statement = connection.createStatement(); var rows = statement.executeQuery(query)) {
                if (!rows.next()) throw new SQLException("Required transaction evidence row absent");
                return rows.getLong(1);
            }
        }
    }

    private Map<String, List<Map<String, Object>>> snapshot() {
        Map<String, List<Map<String, Object>>> result = new TreeMap<>();
        tables.forEach(table -> result.put(table, jdbc.queryForList("SELECT * FROM " + table + " ORDER BY 1")));
        return result;
    }

    private void assertRestored(Map<String, List<Map<String, Object>>> before) {
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive(), "Read back outside the service transaction");
        assertEquals(before, snapshot(), "Every actual business, signature, specialized/unified audit row and watermark must restore");
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM gxp_audit_event_relation", Integer.class));
        assertEquals(1L, jdbc.queryForObject("SELECT next_ledger_sequence FROM gxp_audit_ledger_sequence", Long.class));
    }

    private static JsonNode sorted(JsonNode node) {
        if (!node.isObject()) return node;
        ObjectNode result = JsonUtils.getObjectMapper().createObjectNode();
        TreeSet<String> keys = new TreeSet<>();
        node.fieldNames().forEachRemaining(keys::add);
        keys.forEach(key -> result.set(key, sorted(node.get(key))));
        return result;
    }

    @Intercepts(@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}))
    public static class MysqlBitsForH2 implements Interceptor {
        @Override public Object intercept(Invocation invocation) throws Throwable {
            var bound = ((StatementHandler) invocation.getTarget()).getBoundSql();
            ReflectionTestUtils.setField(bound, "sql", bound.getSql().replace("b'0'", "0").replace("b'1'", "1"));
            return invocation.proceed();
        }
    }

    private boolean cancellationProbeEnabled;

    private void runObservedCancellation(Runnable cancel) {
        cancellationProbeEnabled=true;
        try { cancel.run(); }
        finally { cancellationProbeEnabled=false; }
    }

    private void assertLedgerBeforeBusinessLocks(String entry) {
        assertFalse(cancellationLocks.isEmpty());
        assertTrue(cancellationLocks.get(0).contains("gxp_audit_ledger_sequence"),cancellationLocks.toString());
        assertTrue(cancellationLocks.stream().anyMatch(sql->sql.contains(ACTIVE)),"Actual active-cycle SQL must execute");
        assertTrue(cancellationLocks.stream().anyMatch(sql->sql.contains(WORK)),"Actual work-order SQL must execute");
        if(entry.equals("erp")) {
            int normalUpdate=-1;
            for(int i=0;i<cancellationLocks.size();i++) {
                String sql=cancellationLocks.get(i);
                if(sql.startsWith("update ") && sql.contains(WORK) && sql.contains("work_order_id=3002")) normalUpdate=i;
            }
            assertTrue(normalUpdate>0,"The ledger lock must precede NORMAL's actual UPDATE, not only later cancellation: "+cancellationLocks);
            assertTrue(cancellationLocks.subList(0,normalUpdate).stream().anyMatch(sql->sql.contains("gxp_audit_ledger_sequence")));
        }
    }

    @Intercepts(@Signature(type=StatementHandler.class,method="prepare",args={Connection.class,Integer.class}))
    public static class CancellationLockProbe implements Interceptor {
        private final List<String> statements;
        private final java.util.function.BooleanSupplier enabled;
        CancellationLockProbe(List<String> statements, java.util.function.BooleanSupplier enabled) {
            this.statements=statements; this.enabled=enabled;
        }
        @Override public Object intercept(Invocation invocation) throws Throwable {
            if(!enabled.getAsBoolean()) return invocation.proceed();
            var bound=((StatementHandler)invocation.getTarget()).getBoundSql();
            String sql=bound.getSql().trim().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT);
            if(sql.contains("for update") || sql.startsWith("update ") || sql.startsWith("insert ") || sql.startsWith("delete ")) {
                assertTrue(TransactionSynchronizationManager.isActualTransactionActive(),"Locks require the actual transaction");
                if(sql.startsWith("update ") && sql.contains(WORK) && bound.getParameterObject() instanceof Map<?,?> params
                        && params.get("et") instanceof MesProWorkOrderDO workOrder) {
                    sql += " /* work_order_id="+workOrder.getId()+" */";
                }
                statements.add(sql);
            }
            return invocation.proceed();
        }
    }

    private Object tx(Object target) {
        var proxy = new ProxyFactory(target);
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
        return proxy.getProxy();
    }
    private static void inject(Object target, String field, Object value) {
        ReflectionTestUtils.setField(target, field, value);
    }
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
                        : field.getType() == LocalDateTime.class ? "TIMESTAMP" : field.getType() == LocalDate.class ? "DATE"
                        : "VARCHAR(1000000)";
                if (name.equals("id")) sqlType = "BIGINT AUTO_INCREMENT PRIMARY KEY";
                else if (field.isAnnotationPresent(TableId.class)) sqlType += " PRIMARY KEY";
                columns.add("`" + name + "` " + sqlType);
            }
        }
        String table = row.getAnnotation(TableName.class).value();
        // These BaseDO mappings omit tenant_id present in both production table definitions.
        if (row == MesProEdhrWorkTaskDO.class || row == MesProEdhrReleaseTransactionDO.class
                || row == MesProEdhrRecordChangeEventDO.class || row == MesProEdhrBatchExecutionArchiveDO.class) {
            columns.add("`tenant_id` BIGINT NOT NULL DEFAULT 0");
        }
        jdbc.execute("CREATE TABLE " + table + "(" + String.join(",", columns) + ")");
        tables.add(table);
    }
}
