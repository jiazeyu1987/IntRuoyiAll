package cn.iocoder.yudao.module.mes.service.pro.craft;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.craft.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.craft.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import com.alibaba.fastjson.*;
import org.junit.jupiter.api.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 真实事务覆盖独立入库、去重及失败回滚；流程服务没有参与写入。 */
class MesProCraftInitializationTest {
    private MesProCraftService service;
    private JdbcTemplate jdbc;
    private MesProCraftMapper crafts;
    private MesProCraftChangeMapper changes;
    private MesProRouteVersionMapper versions;
    private final List<MesProRouteVersionDO> routes = new ArrayList<>();

    @BeforeEach @SuppressWarnings("unchecked") void setup() {
        TenantContextHolder.setTenantId(1L);
        var source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID(), "sa", "");
        // Keep the database alive across independent preview and import transactions.
        source.setUrl(source.getUrl() + ";DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("create table sequence_state (next_value bigint)");
        jdbc.update("insert into sequence_state values (0)");
        jdbc.execute("create table crafts (id bigint primary key, code varchar(64), name varchar(128), process_id bigint, parameters_json clob)");
        jdbc.execute("create table changes (status varchar(32), candidate_ids varchar(128))");
        crafts = mock(MesProCraftMapper.class);
        changes = mock(MesProCraftChangeMapper.class);
        versions = mock(MesProRouteVersionMapper.class);
        var processes = mock(MesProProcessMapper.class);
        when(processes.selectById(anyLong())).thenReturn(new MesProProcessDO());
        when(versions.selectActiveListWithExistingRoutes()).thenAnswer(call -> routes);
        when(crafts.lockSequence(1L)).thenAnswer(call -> jdbc.queryForObject("select next_value from sequence_state", Long.class));
        when(crafts.advanceSequence(eq(1L), anyLong())).thenAnswer(call ->
                jdbc.update("update sequence_state set next_value=?", (Long) call.getArgument(1)));
        AtomicLong ids = new AtomicLong();
        when(crafts.insert(any(MesProCraftDO.class))).thenAnswer(call -> {
            MesProCraftDO craft = call.getArgument(0);
            craft.setId(ids.incrementAndGet());
            return jdbc.update("insert into crafts values (?,?,?,?,?)", craft.getId(), craft.getCode(),
                    craft.getName(), craft.getProcessId(), craft.getParametersJson());
        });
        when(crafts.selectList(any(LambdaQueryWrapperX.class))).thenAnswer(call ->
                jdbc.query("select * from crafts order by id", (rs, row) -> {
                    MesProCraftDO craft = new MesProCraftDO();
                    craft.setId(rs.getLong("id")); craft.setCode(rs.getString("code"));
                    craft.setName(rs.getString("name")); craft.setProcessId(rs.getLong("process_id"));
                    craft.setParametersJson(rs.getString("parameters_json")); return craft;
                }));
        when(changes.insert(any(MesProCraftChangeDO.class))).thenAnswer(call -> {
            MesProCraftChangeDO change = call.getArgument(0);
            change.setId(1L);
            return jdbc.update("insert into changes values (?,?)", change.getStatus(), change.getCandidateIdsJson());
        });
        MesProCraftService target = new MesProCraftService();
        ReflectionTestUtils.setField(target, "craftMapper", crafts);
        ReflectionTestUtils.setField(target, "changeMapper", changes);
        ReflectionTestUtils.setField(target, "versionMapper", versions);
        ReflectionTestUtils.setField(target, "processMapper", processes);
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(source), new AnnotationTransactionAttributeSource()));
        service = (MesProCraftService) proxy.getProxy();
    }

    @AfterEach void clearContext() { TenantContextHolder.clear(); }

    private MesProRouteVersionDO route(long id, Long process, int... values) {
        JSONObject root = new JSONObject();
        root.put("routeName", "来源流程" + id);
        JSONObject data = new JSONObject(); root.put("configSnapshots", data);
        if (process != null) {
            JSONObject config = new JSONObject();
            config.put("routeProcessId", id * 10); config.put("processId", process); config.put("processName", "清洗");
            JSONArray rules = new JSONArray();
            for (int i = 0; i < values.length; i++) {
                JSONObject rule = JSON.parseObject("{\"parameterCode\":\"COUNT\",\"parameterName\":\"次数\",\"valueType\":\"INTEGER\",\"standardText\":\"清洗次数\"}");
                rule.put("deviceId", 31L + i); rule.put("defaultValue", values[i]); rules.add(rule);
            }
            config.put("parameterRules", rules);
            data.put("productionProcessConfigs", new JSONArray(List.of(config)));
        }
        MesProRouteVersionDO version = new MesProRouteVersionDO();
        version.setId(id); version.setRouteId(id); version.setRouteSnapshotJson(root.toJSONString());
        version.setRouteSnapshotSha256("source-" + id); routes.add(version); return version;
    }

    private String token() { return (String) service.initializationPreview().get("impactToken"); }
    private int count(String table) { return jdbc.queryForObject("select count(*) from " + table, Integer.class); }
    private void assertRolledBack() {
        assertEquals(0, count("crafts")); assertEquals(0, count("changes"));
        assertEquals(0L, jdbc.queryForObject("select next_value from sequence_state", Long.class));
    }

    @Test void importsDistinctSchemesWithoutChangingRoutesOrWaitingForCandidates() {
        route(1, 22L, 2, 3, 2); route(2, 22L, 2); route(3, 23L, 2); route(4, null);
        when(versions.selectOpenCandidateByRouteId(anyLong())).thenThrow(new AssertionError("只提取入库不应检查候选版本"));
        List<String> before = routes.stream().map(MesProRouteVersionDO::getRouteSnapshotJson).toList();
        Map<String, Object> preview = service.initializationPreview();
        assertEquals(4, ((JSONArray) preview.get("sources")).size());
        assertEquals(3L, preview.get("newCraftCount"));
        assertEquals(1, ((List<?>) preview.get("warnings")).size());
        assertTrue(((List<?>) preview.get("blockers")).isEmpty());
        MesProCraftChangeDO result = service.initialize((String) preview.get("impactToken"));
        assertEquals("APPLIED", result.getStatus()); assertEquals("[]", result.getCandidateIdsJson());
        assertEquals(3, count("crafts")); assertEquals(1, count("changes"));
        assertEquals(List.of("GY-0001", "GY-0002", "GY-0003"), jdbc.queryForList("select code from crafts order by id", String.class));
        assertEquals(before, routes.stream().map(MesProRouteVersionDO::getRouteSnapshotJson).toList());
        for (String parameters : jdbc.queryForList("select parameters_json from crafts", String.class)) {
            assertFalse(parameters.contains("deviceId")); assertFalse(parameters.contains("routeProcessId"));
        }
        verify(versions, times(2)).selectActiveListWithExistingRoutes(); verifyNoMoreInteractions(versions);
    }

    @Test void repeatedExtractionDoesNotCreateDuplicatesOrEmptyAuditRows() {
        route(1, 22L, 2, 2);
        service.initialize(token());
        Map<String, Object> preview = service.initializationPreview();
        assertEquals(0L, preview.get("newCraftCount")); assertEquals(1L, preview.get("existingCraftCount"));
        assertThrows(RuntimeException.class, () -> service.initialize((String) preview.get("impactToken")));
        assertEquals(1, count("crafts")); assertEquals(1, count("changes"));
    }

    @Test void changedSourceRequiresNewPreviewBeforeAnyWrite() {
        MesProRouteVersionDO source = route(1, 22L, 2);
        String token = token();
        source.setRouteSnapshotJson(source.getRouteSnapshotJson().replace("\"defaultValue\":2", "\"defaultValue\":3"));
        assertThrows(RuntimeException.class, () -> service.initialize(token)); assertRolledBack();
    }

    @Test void invalidParametersStillBlockEntireImport() {
        route(1, 22L, 2);
        MesProRouteVersionDO invalid = route(2, 22L, 3);
        invalid.setRouteSnapshotJson(invalid.getRouteSnapshotJson().replace("INTEGER", "UNKNOWN"));
        Map<String, Object> preview = service.initializationPreview();
        assertEquals(1, ((List<?>) preview.get("blockers")).size());
        assertThrows(RuntimeException.class, () -> service.initialize((String) preview.get("impactToken")));
        assertRolledBack();
    }

    @Test void secondInsertFailureRollsBackCraftsSequenceAndAudit() {
        route(1, 22L, 2, 3);
        doAnswer(call -> { throw new IllegalStateException("second insert failed"); })
                .when(crafts).insert(argThat((MesProCraftDO craft) -> "GY-0002".equals(craft.getCode())));
        String token = token();
        assertThrows(IllegalStateException.class, () -> service.initialize(token)); assertRolledBack();
    }

    @Test void auditFailureRollsBackAllImportedCrafts() {
        route(1, 22L, 2, 3);
        when(changes.insert(any(MesProCraftChangeDO.class))).thenReturn(0);
        String token = token();
        assertThrows(RuntimeException.class, () -> service.initialize(token)); assertRolledBack();
    }
}
