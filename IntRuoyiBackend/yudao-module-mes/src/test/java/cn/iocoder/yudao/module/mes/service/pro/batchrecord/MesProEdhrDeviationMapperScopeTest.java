package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeviationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrDeviationMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/** Executes production query predicates against an isolated database, without business database access. */
class MesProEdhrDeviationMapperScopeTest {
    private Connection connection;
    private final MybatisConfiguration configuration = new MybatisConfiguration();

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:h2:mem:deviation_scope;MODE=MySQL", "sa", "");
        try (var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE mes_pro_edhr_deviation (id BIGINT PRIMARY KEY, tenant_id BIGINT, "
                    + "active_order_id BIGINT, batch_execution_id BIGINT, status VARCHAR(16), deleted INT, "
                    + "close_reason VARCHAR(64), nonconformance_review_id BIGINT, closed_at TIMESTAMP, version INT DEFAULT 1)");
            statement.execute("CREATE TABLE mes_pro_edhr_batch_execution_origin (tenant_id BIGINT, "
                    + "active_order_id BIGINT, batch_execution_id BIGINT, deleted INT, entry_type VARCHAR(64))");
        }
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, "deviation-scope"),
                MesProEdhrDeviationDO.class);
    }

    @AfterEach
    void tearDown() throws Exception {
        connection.close();
    }

    @Test
    void releaseBlocksOpenOrderDeviationOnlyAfterItsFormalBatchExists() throws Exception {
        deviation(1, 71, 991L, null, "OPEN", 0);
        assertEquals(List.of(), openForBatch(71, 41));
        origin(71, 991, 41, 0, "ACTIVE_ORDER_COMPLETION");
        assertEquals(List.of(1L), openForBatch(71, 41));
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE mes_pro_edhr_deviation SET status='CLOSED' WHERE id=1");
        }
        assertEquals(List.of(), openForBatch(71, 41));
    }

    @Test
    void releasePreservesDirectBatchDeviationsAndRejectsOtherTenantsAndCycles() throws Exception {
        origin(71, 991, 41, 0, "ACTIVE_ORDER_COMPLETION");
        deviation(1, 72, 991L, null, "OPEN", 0);
        deviation(2, 71, 992L, null, "OPEN", 0);
        deviation(3, 71, 991L, null, "OPEN", 1);
        deviation(4, 71, 991L, null, "CLOSED", 0);
        deviation(5, 71, null, 42L, "OPEN", 0);
        assertEquals(List.of(), openForBatch(71, 41));
        deviation(6, 71, null, 41L, "OPEN", 0);
        assertEquals(List.of(6L), openForBatch(71, 41));
    }

    @Test
    void releaseIgnoresDeletedForeignAndNonformalOrigins() throws Exception {
        deviation(1, 71, 991L, null, "OPEN", 0);
        origin(72, 991, 41, 0, "ACTIVE_ORDER_COMPLETION");
        origin(71, 991, 41, 1, "ACTIVE_ORDER_COMPLETION");
        origin(71, 991, 41, 0, "UNRELATED_SOURCE");
        assertEquals(List.of(), openForBatch(71, 41));
    }

    @Test
    void orderPageKeepsLegacyBatchDeviationsAndPrecompletionOrderDeviations() throws Exception {
        origin(71, 991, 41, 0, "ACTIVE_ORDER_COMPLETION");
        origin(72, 991, 42, 0, "ACTIVE_ORDER_COMPLETION");
        origin(71, 991, 43, 1, "ACTIVE_ORDER_COMPLETION");
        origin(71, 991, 44, 0, "UNRELATED_SOURCE");
        deviation(1, 71, 991L, null, "OPEN", 0);
        deviation(2, 71, null, 41L, "CLOSED", 0);
        deviation(3, 71, 992L, null, "OPEN", 0);
        deviation(4, 72, 991L, null, "OPEN", 0);
        deviation(5, 71, null, 42L, "OPEN", 0);
        deviation(6, 71, null, 43L, "OPEN", 0);
        deviation(7, 71, null, 44L, "OPEN", 0);
        assertEquals(List.of(2L, 1L), page(null, 991L));
    }

    @Test
    void batchPageFindsOrderDeviationsThroughOnlyFormalTenantVisibleOrigins() throws Exception {
        origin(71, 991, 41, 0, "ACTIVE_ORDER_COMPLETION");
        origin(72, 992, 41, 0, "ACTIVE_ORDER_COMPLETION");
        origin(71, 993, 41, 1, "ACTIVE_ORDER_COMPLETION");
        origin(71, 994, 41, 0, "UNRELATED_SOURCE");
        deviation(1, 71, 991L, null, "OPEN", 0);
        deviation(2, 71, null, 41L, "CLOSED", 0);
        deviation(3, 71, 992L, null, "OPEN", 0);
        deviation(4, 71, 993L, null, "OPEN", 0);
        deviation(5, 71, 994L, null, "OPEN", 0);
        deviation(6, 72, 991L, null, "OPEN", 0);
        assertEquals(List.of(2L, 1L), page(41L, null));
    }

    private List<Long> openForBatch(long tenantId, long batchId) throws Exception {
        String sql = String.join("\n", MesProEdhrDeviationMapper.class
                .getMethod("selectOpenIdByTenantAndBatchExecutionIdForUpdate", Long.class, Long.class)
                .getAnnotation(Select.class).value());
        return query(sql, Map.of("tenantId", tenantId, "batchExecutionId", batchId));
    }

    @Test
    void orderTransferClosesOnlySelectedOpenRowsFromTheOriginalTenantAndCycle() throws Exception {
        deviation(1, 71, 991L, null, "OPEN", 0);
        deviation(2, 71, 992L, null, "OPEN", 0);
        deviation(3, 72, 991L, null, "OPEN", 0);
        deviation(4, 71, 991L, 41L, "OPEN", 0);
        deviation(5, 71, 991L, null, "OPEN", 1);
        deviation(6, 71, 991L, null, "OPEN", 0);
        assertEquals(1, transfer(null, 991L, List.of(1L, 2L, 3L, 4L, 5L)));
        assertEquals(List.of(1L), query("SELECT id FROM mes_pro_edhr_deviation WHERE "
                + "status='CLOSED' AND nonconformance_review_id=501 ORDER BY id", Map.of()));
        assertEquals(0, transfer(null, 991L, List.of(1L)));
    }

    @Test
    void batchTransferPreservesTheExistingBatchSourceBoundary() throws Exception {
        deviation(1, 71, null, 41L, "OPEN", 0);
        deviation(2, 71, 991L, null, "OPEN", 0);
        deviation(3, 71, null, 42L, "OPEN", 0);
        deviation(4, 72, null, 41L, "OPEN", 0);
        assertEquals(1, transfer(41L, null, List.of(1L, 2L, 3L, 4L)));
        assertEquals(List.of(1L), query("SELECT id FROM mes_pro_edhr_deviation WHERE status='CLOSED'", Map.of()));
    }

    private int transfer(Long batchId, Long orderId, List<Long> deviationIds) throws Exception {
        String sql = String.join("\n", MesProEdhrDeviationMapper.class.getMethod("closeToNonconformance",
                Long.class, Long.class, Long.class, java.util.Collection.class, Long.class, LocalDateTime.class)
                .getAnnotation(Update.class).value());
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("tenantId", 71L);
        parameters.put("batchExecutionId", batchId);
        parameters.put("activeOrderId", orderId);
        parameters.put("deviationIds", deviationIds);
        parameters.put("reviewId", 501L);
        parameters.put("closedAt", LocalDateTime.of(2026, 11, 16, 12, 0));
        var bound = new XMLLanguageDriver().createSqlSource(configuration, sql, Map.class).getBoundSql(parameters);
        try (var statement = connection.prepareStatement(bound.getSql())) {
            var values = configuration.newMetaObject(parameters);
            for (int i = 0; i < bound.getParameterMappings().size(); i++) {
                String property = bound.getParameterMappings().get(i).getProperty();
                statement.setObject(i + 1, bound.hasAdditionalParameter(property)
                        ? bound.getAdditionalParameter(property) : values.getValue(property));
            }
            return statement.executeUpdate();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Long> page(Long batchId, Long orderId) throws Exception {
        var wrapper = new AtomicReference<LambdaQueryWrapper<MesProEdhrDeviationDO>>();
        var mapper = mock(MesProEdhrDeviationMapper.class, CALLS_REAL_METHODS);
        doAnswer(invocation -> {
            wrapper.set(invocation.getArgument(1));
            return invocation.getArgument(0);
        }).when(mapper).selectPage(any(Page.class), any(Wrapper.class));
        mapper.selectPageByTenant(new Page<>(), 71L, null, null, batchId, orderId,
                null, null, null, null, null, new PageParam());
        return query("SELECT id FROM mes_pro_edhr_deviation WHERE " + wrapper.get().getSqlSegment(),
                Map.of("ew", wrapper.get()));
    }

    private List<Long> query(String sql, Map<String, Object> parameters) throws Exception {
        var bound = new XMLLanguageDriver().createSqlSource(configuration, sql, Map.class).getBoundSql(parameters);
        try (var statement = connection.prepareStatement(bound.getSql())) {
            var values = configuration.newMetaObject(parameters);
            for (int i = 0; i < bound.getParameterMappings().size(); i++) {
                statement.setObject(i + 1, values.getValue(bound.getParameterMappings().get(i).getProperty()));
            }
            try (var rows = statement.executeQuery()) {
                List<Long> ids = new ArrayList<>();
                while (rows.next()) ids.add(rows.getLong(1));
                return ids;
            }
        }
    }

    private void deviation(long id, long tenantId, Long orderId, Long batchId, String status, int deleted)
            throws Exception {
        try (var statement = connection.prepareStatement("INSERT INTO mes_pro_edhr_deviation "
                + "(id,tenant_id,active_order_id,batch_execution_id,status,deleted) VALUES (?,?,?,?,?,?)")) {
            statement.setLong(1, id);
            statement.setLong(2, tenantId);
            statement.setObject(3, orderId);
            statement.setObject(4, batchId);
            statement.setString(5, status);
            statement.setInt(6, deleted);
            statement.executeUpdate();
        }
    }

    private void origin(long tenantId, long orderId, long batchId, int deleted, String entryType) throws Exception {
        try (var statement = connection.prepareStatement("INSERT INTO mes_pro_edhr_batch_execution_origin VALUES (?,?,?,?,?)")) {
            statement.setLong(1, tenantId);
            statement.setLong(2, orderId);
            statement.setLong(3, batchId);
            statement.setInt(4, deleted);
            statement.setString(5, entryType);
            statement.executeUpdate();
        }
    }
}
