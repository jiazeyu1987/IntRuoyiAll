package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApiImpl;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApiImpl;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.*;
import cn.iocoder.yudao.module.system.dal.dataobject.notify.*;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.*;
import cn.iocoder.yudao.module.system.dal.mysql.permission.*;
import cn.iocoder.yudao.module.system.dal.mysql.notify.*;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.*;
import cn.iocoder.yudao.module.system.service.permission.*;
import cn.iocoder.yudao.module.system.service.permission.bo.SystemEntitlementSyncCommand;
import cn.iocoder.yudao.module.system.service.notify.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.fasterxml.jackson.databind.JsonNode;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.h2.api.Trigger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;
import java.lang.reflect.Modifier;
import java.sql.*;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Current MES auxiliary call boundary, real system services/mappers/Gxp writer and private H2.
 * Parent task rules are covered by the existing current release suites, not fabricated here.
 * Only template lookup/formatting is an explicit external seam. */
class MesWorkTaskAuxiliaryAuditTransactionTest {
    static final String CLAIM = "system_entitlement_claim", GRANT = "system_entitlement_grant";
    static final String SPECIAL = "system_entitlement_audit_event", MESSAGE = "system_notify_message";
    static final String EVENT = "gxp_audit_event", POLICY = "MES_EDHR_FILLER_MINIMAL";
    static final String ENT = "mes.work-task.entitlement", NOTIFY = "mes.work-task.notify";
    JdbcTemplate jdbc;
    DriverManagerDataSource ds;
    SqlSessionTemplate session;
    DataSourceTransactionManager manager;
    MesProEdhrWorkTaskServiceImpl tasks;
    SystemEntitlementService entitlement;
    GxpAuditService audit;
    MesProEdhrWorkTaskDO task;
    static boolean failureSawWrites;

    @BeforeEach void setup() throws Exception {
        ds = new DriverManagerDataSource("jdbc:h2:mem:task_aux_" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(ds);
        for (Class<?> row : List.of(SystemEntitlementClaimDO.class, SystemEntitlementGrantDO.class,
                SystemEntitlementAuditEventDO.class, SystemEntitlementPolicyDO.class, MenuDO.class,
                NotifyMessageDO.class, MesProEdhrWorkTaskDO.class, GxpAuditEventDO.class,
                GxpAuditEventRelationDO.class, GxpAuditLedgerSequenceDO.class,
                GxpAuditPolicyActivationDO.class, GxpAuditPolicyOperationDO.class)) table(row);
        jdbc.execute("CREATE TABLE parent_marker(id BIGINT PRIMARY KEY)");
        var config = new MybatisConfiguration();
        config.setMapUnderscoreToCamelCase(true);
        var plugin = new MybatisPlusInterceptor();
        plugin.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            public Expression getTenantId() { return new LongValue(TenantContextHolder.getRequiredTenantId()); }
            public boolean ignoreTable(String table) { return !MESSAGE.equals(table); }
        }));
        config.addInterceptor(plugin);
        for (Class<?> m : List.of(SystemEntitlementClaimMapper.class, SystemEntitlementGrantMapper.class,
                SystemEntitlementAuditEventMapper.class, SystemEntitlementPolicyMapper.class, MenuMapper.class,
                NotifyMessageMapper.class, GxpAuditEventMapper.class, GxpAuditEventRelationMapper.class,
                GxpAuditLedgerSequenceMapper.class, GxpAuditPolicyActivationMapper.class,
                GxpAuditPolicyOperationMapper.class)) config.addMapper(m);
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(ds); factory.setConfiguration(config);
        session = new SqlSessionTemplate(Objects.requireNonNull(factory.getObject()));
        manager = new DataSourceTransactionManager(ds);
        var writer = new GxpAuditServiceImpl();
        wireMappers(writer);
        audit = (GxpAuditService) tx(writer);
        var realEntitlement = new SystemEntitlementServiceImpl(); wireMappers(realEntitlement);
        entitlement = (SystemEntitlementService) tx(realEntitlement);
        var permissions = new PermissionApiImpl();
        ReflectionTestUtils.setField(permissions, "entitlementService", entitlement);
        var messages = new NotifyMessageServiceImpl(); wireMappers(messages);
        var send = new NotifySendServiceImpl();
        ReflectionTestUtils.setField(send, "notifyMessageService", tx(messages));
        NotifyTemplateService templates = mock(NotifyTemplateService.class);
        NotifyTemplateDO template = new NotifyTemplateDO();
        template.setId(10L); template.setCode("MES_EDHR_FILL_TASK_ASSIGNED"); template.setType(1);
        template.setStatus(0); template.setNickname("Production"); template.setContent("Task");
        template.setParams(List.of("workTaskId", "actionUrl"));
        when(templates.getNotifyTemplateByCodeFromCache(anyString())).thenReturn(template);
        when(templates.formatNotifyTemplateContent(anyString(), anyMap())).thenReturn("Assigned task 7001");
        ReflectionTestUtils.setField(send, "notifyTemplateService", templates);
        var notificationApi = new NotifyMessageSendApiImpl();
        ReflectionTestUtils.setField(notificationApi, "notifySendService", tx(send));
        tasks = new MesProEdhrWorkTaskServiceImpl();
        ReflectionTestUtils.setField(tasks, "permissionApi", permissions);
        ReflectionTestUtils.setField(tasks, "notifyMessageSendApi", notificationApi);
        // Discover newly added resource by field metadata, so RED runs the unchanged original calls.
        for (var field : tasks.getClass().getDeclaredFields()) {
            if (field.getType().getSimpleName().equals("MesWorkTaskAuxiliaryAudit")) {
                Object boundary = field.getType().getConstructor(DataSource.class, GxpAuditService.class)
                        .newInstance(ds, audit);
                ReflectionTestUtils.setField(tasks, field.getName(), boundary);
            }
        }
        actor(1L);
        for (long tenant : List.of(1L, 2L)) {
            jdbc.update("INSERT INTO gxp_audit_ledger_sequence(tenant_id,next_ledger_sequence) VALUES(?,1)", tenant);
            jdbc.update("INSERT INTO gxp_audit_policy_activation(id,tenant_id,policy_version) VALUES(?,?,'aux-test')", tenant, tenant);
            for (String operation : List.of(ENT, NOTIFY))
                jdbc.update("INSERT INTO gxp_audit_policy_operation(tenant_id,policy_version,operation_id,domain,"
                        + "subject_type,action_type,reason_policy,signature_policy,state_policy,applicability,active)"
                        + " VALUES(?,'aux-test',?,'MES','MES_WORK_TASK','UPDATE','SYSTEM','NONE','PRESENT_TO_PRESENT','GXP',1)",
                        tenant, operation);
        }
        jdbc.update("INSERT INTO system_entitlement_policy(id,policy_code,status,allowed_permission_codes_json,forbidden_permission_codes_json)"
                + " VALUES(1,?,0,'[\"mes:task:fill\"]','[]')", POLICY);
        jdbc.update("INSERT INTO system_menu(id,permission,status) VALUES(1,'mes:task:fill',0)");
        jdbc.update("INSERT INTO mes_pro_edhr_work_task(id,tenant_id,task_type,status,candidate_user_snapshot)"
                + " VALUES(7001,1,'FILL','TODO','11,12')");
        task = new MesProEdhrWorkTaskDO(); task.setId(7001L);
        task.setTaskType("FILL"); task.setStatus("TODO"); task.setCandidateUserSnapshot("11,12");
        task.setCandidateSourceType("USER"); task.setCandidateSourceId(11L); task.setActionUrl("/tasks/7001");
        task.setWorkOrderCode("WO-1"); task.setBatchCode("B-1"); task.setProcessName("Assembly");
        failureSawWrites = false;
    }

    @AfterEach void close() throws SQLException {
        SecurityContextHolder.clearContext(); TenantContextHolder.clear();
        // JdbcTemplate debug warning handling queries a connection after SHUTDOWN closes it.
        // Execute directly so cleanup is independent of logging level, without swallowing errors.
        if (ds != null) try (var connection = ds.getConnection(); var statement = connection.createStatement()) {
            statement.execute("SHUTDOWN");
        }
    }

    @Test void syncRecordsActualClaimsAllRebuiltGrantsAndSpecializedRows() {
        // A second source for the same user must survive and contribute to rebuilt grants.
        entitlement.syncClaims(SystemEntitlementSyncCommand.builder().tenantId(1L).sourceType("OTHER")
                .sourceKey("other-1").sourceVersion("1").sourceDigest("other").policyCode(POLICY)
                .resolvedUserIds(Set.of(11L)).operatorUserId(101L).operatorUsername("operator").build());
        var before = state();
        run("syncRuntimeTaskEntitlement");
        assertEquals(3, count(CLAIM)); assertEquals(2, count(GRANT));
        var event = event(ENT); assertState(event, before, state(), false);
        assertTrue(event.getAfterStateJson().contains("WORK_TASK|7001"));
        assertTrue(event.getAfterStateJson().contains("active_claim_count"));
        assertEquals(101L, event.getActorId());
        assertEquals(1, count(EVENT));
    }

    @Test void revokeRecordsActualRowsAndNoWriteReplayAddsNoFact() {
        run("syncRuntimeTaskEntitlement");
        var before = state();
        run("revokeRuntimeTaskEntitlement");
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM " + CLAIM + " WHERE status='ACTIVE'", Integer.class));
        var events = mapper(GxpAuditEventMapper.class).selectList();
        assertEquals(2, events.size()); assertState(events.get(1), before, state(), false);
        run("revokeRuntimeTaskEntitlement");
        assertEquals(2, count(EVENT), "Existing no-active-claim replay must not add audit facts");
    }

    @Test void recipientChangeIncludesRemovedAndNewUsersWithoutTouchingForeignTenant() {
        run("syncRuntimeTaskEntitlement");
        jdbc.update("INSERT INTO system_entitlement_grant(tenant_id,resolved_user_id,status,active_claim_count) VALUES(2,11,'ACTIVE',9)");
        var before = state();
        task.setCandidateUserSnapshot("12,13");
        run("syncRuntimeTaskEntitlement");
        var events = mapper(GxpAuditEventMapper.class).selectList();
        assertEquals(2, events.size()); assertState(events.get(1), before, state(), false);
        assertEquals(9, jdbc.queryForObject("SELECT active_claim_count FROM " + GRANT + " WHERE tenant_id=2", Integer.class));
        assertFalse(events.get(1).getAfterStateJson().contains("\"tenant_id\":2"));
    }

    @Test void notificationsFreezeActualMessageIdsRecipientsAndContent() {
        var before = state();
        run("sendNotify");
        assertEquals(2, count(MESSAGE));
        var event = event(NOTIFY); assertState(event, before, state(), true);
        assertTrue(event.getAfterStateJson().contains("Assigned task 7001"));
        assertTrue(event.getAfterStateJson().contains("workTaskId"));
        assertEquals(List.of(11L,12L), jdbc.queryForList("SELECT user_id FROM " + MESSAGE + " ORDER BY id", Long.class));
    }

    @ParameterizedTest @ValueSource(strings={"syncRuntimeTaskEntitlement","revokeRuntimeTaskEntitlement","sendNotify"})
    void auditFailureRollsBackAuxiliaryAndParentWrites(String method) {
        if (method.startsWith("revoke")) run("syncRuntimeTaskEntitlement");
        var before = state(); int events = count(EVENT);
        jdbc.execute("CREATE TRIGGER fail_aux BEFORE INSERT ON gxp_audit_event_relation FOR EACH ROW CALL '"
                + RelationFailure.class.getName() + "'");
        assertThrows(RuntimeException.class, () -> new TransactionTemplate(manager).executeWithoutResult(status -> {
            jdbc.update("INSERT INTO parent_marker(id) VALUES(1)");
            ReflectionTestUtils.invokeMethod(tasks, method, task);
        }));
        assertTrue(failureSawWrites, "Failure must occur after real downstream writes");
        assertEquals(before, state()); assertEquals(events, count(EVENT)); assertEquals(0, count("parent_marker"));
    }

    @ParameterizedTest @ValueSource(strings={"syncRuntimeTaskEntitlement","sendNotify"})
    void foreignTenantTaskFailsBeforeDownstreamWrite(String method) {
        actor(2L);
        assertThrows(RuntimeException.class, () -> run(method));
        assertEquals(0, count(CLAIM)); assertEquals(0, count(MESSAGE)); assertEquals(0, count(EVENT));
    }

    @Test void missingTransactionFailsBeforeNotificationWrite() {
        assertThrows(RuntimeException.class, () -> ReflectionTestUtils.invokeMethod(tasks, "sendNotify", task));
        assertEquals(0, count(MESSAGE)); assertEquals(0, count(EVENT));
    }

    public static class RelationFailure implements Trigger {
        public void fire(Connection c, Object[] oldRow, Object[] newRow) throws SQLException {
            try (var s = c.createStatement(); var r = s.executeQuery("SELECT COUNT(*) FROM parent_marker")) {
                r.next(); failureSawWrites = r.getInt(1) == 1;
            }
            throw new SQLException("deliberate auxiliary relation failure");
        }
    }
    void run(String method) {
        new TransactionTemplate(manager).executeWithoutResult(status -> ReflectionTestUtils.invokeMethod(tasks, method, task));
    }
    Map<String,JsonNode> state() {
        Map<String,JsonNode> result = new LinkedHashMap<>();
        for (String table : List.of(CLAIM,GRANT,SPECIAL,MESSAGE))
            result.put(table, JsonUtils.parseTree(JsonUtils.toJsonString(jdbc.queryForList(
                    "SELECT * FROM " + table + " WHERE tenant_id=1 ORDER BY id"))));
        return result;
    }
    void assertState(GxpAuditEventDO event, Map<String,JsonNode> before, Map<String,JsonNode> after, boolean notifications) {
        JsonNode b=JsonUtils.parseTree(event.getBeforeStateJson()), a=JsonUtils.parseTree(event.getAfterStateJson());
        for (String table : notifications ? List.of(MESSAGE) : List.of(CLAIM,GRANT,SPECIAL)) {
            // OTHER claims/events are not mutated by this work task, but all grants of affected users are.
            JsonNode expectedBefore=before.get(table), expectedAfter=after.get(table);
            if (!notifications && !table.equals(GRANT)) {
                var eb = JsonUtils.parseTree("[]"); var ea = JsonUtils.parseTree("[]");
                expectedBefore.forEach(row -> { if ("WORK_TASK|7001".equals(row.path("source_key").asText())) ((com.fasterxml.jackson.databind.node.ArrayNode) eb).add(row); });
                expectedAfter.forEach(row -> { if ("WORK_TASK|7001".equals(row.path("source_key").asText())) ((com.fasterxml.jackson.databind.node.ArrayNode) ea).add(row); });
                expectedBefore=eb; expectedAfter=ea;
            }
            assertEquals(expectedBefore,b.get(table), "before " + table);
            assertEquals(expectedAfter,a.get(table), "after " + table);
        }
    }
    GxpAuditEventDO event(String operation) {
        var events=mapper(GxpAuditEventMapper.class).selectList();
        assertEquals(1,events.size(),"A real current auxiliary audit is required");
        assertEquals(operation,events.get(0).getOperationId()); return events.get(0);
    }
    int count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table,Integer.class); }
    <T> T mapper(Class<T> type) { return session.getMapper(type); }
    void wireMappers(Object target) {
        for (var f:target.getClass().getDeclaredFields())
            if (f.getType().getSimpleName().endsWith("Mapper")) ReflectionTestUtils.setField(target,f.getName(),mapper(f.getType()));
    }
    Object tx(Object target) {
        var proxy=new ProxyFactory(target); proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(manager,new AnnotationTransactionAttributeSource())); return proxy.getProxy();
    }
    void actor(Long tenant) {
        TenantContextHolder.setTenantId(tenant);
        var user=new LoginUser().setId(101L).setUserType(2).setTenantId(tenant).setInfo(Map.of("nickname","operator"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,null,List.of()));
    }
    void table(Class<?> row) {
        List<String> columns=new ArrayList<>();
        for (Class<?> type=row;type!=Object.class;type=type.getSuperclass()) for(var f:type.getDeclaredFields()) {
            var map=f.getAnnotation(TableField.class);
            if(Modifier.isStatic(f.getModifiers()) || map!=null&&!map.exist()) continue;
            String name=map!=null&&!map.value().isBlank()?map.value():f.getName().replaceAll("([a-z0-9])([A-Z])","$1_$2").toLowerCase(Locale.ROOT);
            String sql=f.getType()==Long.class?"BIGINT":f.getType()==Integer.class?"INT":f.getType()==Boolean.class?"INT DEFAULT 0":f.getType()==LocalDateTime.class?"TIMESTAMP":"VARCHAR(1000000)";
            if(name.equals("id")) sql="BIGINT AUTO_INCREMENT PRIMARY KEY";
            else if(f.isAnnotationPresent(TableId.class)) sql+=" PRIMARY KEY";
            columns.add(name+" "+sql);
        }
        if (row == MesProEdhrWorkTaskDO.class) columns.add("tenant_id BIGINT");
        jdbc.execute("CREATE TABLE "+row.getAnnotation(TableName.class).value()+"("+String.join(",",columns)+")");
    }
}
