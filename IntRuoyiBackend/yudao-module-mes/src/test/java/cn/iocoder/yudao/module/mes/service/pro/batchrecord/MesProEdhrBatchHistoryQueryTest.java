package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesProEdhrBatchHistoryQueryTest {
    @Test void overviewSqlMatchesHandoverAccessForOwnersRecipientsRevocationAndTenant() throws Exception {
        try (var security = mockStatic(cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.class);
             var connection = DriverManager.getConnection("jdbc:h2:mem:historyHandover;MODE=MySQL", "sa", "")) {
            security.when(cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils::getLoginUserId).thenReturn(20L);
            cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(1L);
            var statement = connection.createStatement();
            statement.execute("CREATE TABLE mes_pro_edhr_batch_execution(id BIGINT, tenant_id BIGINT, status INT, deleted INT)");
            statement.execute("CREATE TABLE mes_pro_edhr_market_release_handover(id BIGINT, tenant_id BIGINT, batch_execution_id BIGINT, original_user_id BIGINT, recipient_user_id BIGINT, recalled_at TIMESTAMP, deleted INT)");
            for (int id = 1; id <= 7; id++) statement.execute("INSERT INTO mes_pro_edhr_batch_execution VALUES(" + id + ",1,10,0)");
            statement.execute("INSERT INTO mes_pro_edhr_batch_execution VALUES(8,2,10,0),(9,1,10,1)");
            statement.execute("INSERT INTO mes_pro_edhr_market_release_handover VALUES"
                    + "(11,1,1,10,20,NULL,0),(12,1,1,10,30,NULL,0),"
                    + "(21,1,2,20,30,NULL,0),(31,1,3,10,20,NULL,0),"
                    + "(51,1,5,10,20,'2026-10-27 10:00:00',0),"
                    + "(61,2,6,10,20,'2026-10-27 10:00:00',0),"
                    + "(71,1,7,10,20,NULL,0),(72,1,7,10,30,NULL,1)");
            var expected = new ArrayList<Long>();
            // Feed the same persisted tenant-scoped, undeleted histories to the removed
            // Java check, then compare with the mapper's actual SQL result.
            for (long id = 7; id >= 1; id--) {
                var history = new ArrayList<cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesMarketReleaseHandoverMapper.Row>();
                try (var prepared = connection.prepareStatement("SELECT * FROM mes_pro_edhr_market_release_handover WHERE tenant_id=1 AND deleted=0 AND batch_execution_id=? ORDER BY id DESC")) {
                    prepared.setLong(1, id);
                    var rows = prepared.executeQuery();
                    while (rows.next()) {
                        var recalled = rows.getTimestamp("recalled_at");
                        history.add(new cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesMarketReleaseHandoverMapper.Row()
                                .setId(rows.getLong("id")).setBatchExecutionId(id)
                                .setOriginalUserId(rows.getLong("original_user_id"))
                                .setRecipientUserId(rows.getLong("recipient_user_id"))
                                .setRecalledAt(recalled == null ? null : recalled.toLocalDateTime()));
                    }
                }
                if (MesMarketReleaseAccess.resolve(history, 20L).visible()) expected.add(id);
            }
            assertEquals(List.of(7L, 6L, 4L, 3L, 2L), expected);
            var request = new EdhrBatchExecutionPageReqVO();
            assertEquals(expected, query(connection, request, null));
            assertEquals(List.of(4L, 3L), query(connection, request, "LIMIT 2 OFFSET 2"));
            assertEquals(List.of(4L, 3L, 2L), query(connection, request, null, true, 6L));
        } finally {
            cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear();
        }
    }

    @Test void scanWindowUsesExclusiveDescendingCursorAndFixedBound() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:historyScan;MODE=MySQL", "sa", "")) {
            var statement = connection.createStatement();
            statement.execute("CREATE TABLE mes_pro_edhr_batch_execution(id BIGINT, tenant_id BIGINT, status INT, deleted INT)");
            for (int id = 1; id <= 130; id++) statement.execute("INSERT INTO mes_pro_edhr_batch_execution VALUES(" + id + ",1,10,0)");
            var request = new EdhrBatchExecutionPageReqVO();
            var first = query(connection, request, null, true, null);
            assertEquals(128, first.size());
            assertEquals(130L, first.get(0));
            assertEquals(3L, first.get(127));
            var second = query(connection, request, null, true, first.get(127));
            assertEquals(List.of(2L, 1L), second);
            assertTrue(Collections.disjoint(first, second));
            assertTrue(query(connection, request, null, true, 1L).isEmpty());
        }
    }

    @Test void terminalScopeAndPaginationUseTheSameSqlFilter() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:historyTerminal;MODE=MySQL", "sa", "")) {
            var statement = connection.createStatement();
            statement.execute("CREATE TABLE mes_pro_edhr_batch_execution(id BIGINT, tenant_id BIGINT, status INT, deleted INT, work_order_code VARCHAR)");
            statement.execute("CREATE TABLE mes_pro_edhr_release_transaction(id BIGINT,tenant_id BIGINT,batch_execution_id BIGINT,deleted INT,release_status VARCHAR,approved_at TIMESTAMP)");
            int[] statuses = {0,10,15,20,25,30,30,40,50,50,60,60,10,30,30};
            for (int i = 0; i < statuses.length; i++) {
                statement.execute("INSERT INTO mes_pro_edhr_batch_execution VALUES(" + (i+1) + ",1," + statuses[i] + ",0,'WO-" + (i+1) + "')");
            }
            statement.execute("INSERT INTO mes_pro_edhr_batch_execution VALUES(16,1,40,1,'DELETED'),(17,2,40,0,'OTHER')");
            for (int id : new int[]{7,10,12,13}) {
                statement.execute("INSERT INTO mes_pro_edhr_release_transaction VALUES("+id+",1,"+id+",0,'RELEASED','2026-10-27 10:00:00')");
            }
            statement.execute("INSERT INTO mes_pro_edhr_release_transaction VALUES(14,2,14,0,'RELEASED','2026-10-27 10:00:00'),(15,1,15,1,'RELEASED','2026-10-27 10:00:00')");
            var request = new EdhrBatchExecutionPageReqVO();
            request.setCompletedTraceOnly(true); request.setExcludeStatuses(List.of(50));
            assertEquals(List.of(13L,12L,11L,8L,7L), query(connection, request, null));
            assertEquals(List.of(11L,8L), query(connection, request, "LIMIT 2 OFFSET 2"));
            request.setWorkOrderCode("WO-11");
            assertEquals(List.of(11L), query(connection, request, null), "作废批次无需已放行事务也能被检索");
            request.setWorkOrderCode(null);
            request.setReleasedOnly(true);
            request.setReleaseApprovedTime(new LocalDateTime[]{LocalDateTime.of(2026,10,27,0,0),LocalDateTime.of(2026,10,28,0,0)});
            assertEquals(List.of(13L,12L,7L), query(connection, request, null));
            request.setReleaseApprovedTime(new LocalDateTime[]{LocalDateTime.of(2026,10,1,0,0),LocalDateTime.of(2026,10,2,0,0)});
            assertTrue(query(connection, request, null).isEmpty());
            request = new EdhrBatchExecutionPageReqVO();
            assertFalse(query(connection, request, null).contains(11L), "普通列表继续使用原有作废排除规则");
        }
    }

    @Test void historyTimesUseFormalOrderAndApprovedPqcReceipts() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:historyTimes;MODE=MySQL", "sa", "")) {
            var statement = connection.createStatement();
            statement.execute("CREATE TABLE mes_pro_edhr_batch_execution_origin(tenant_id BIGINT,batch_execution_id BIGINT,active_order_id BIGINT,work_order_id BIGINT,deleted INT)");
            statement.execute("CREATE TABLE mes_pro_process_pool_active_order(id BIGINT,tenant_id BIGINT,work_order_id BIGINT,joined_at TIMESTAMP,deleted INT)");
            statement.execute("CREATE TABLE mes_pro_process_pool_active_order_release_application(tenant_id BIGINT,batch_execution_id BIGINT,active_order_id BIGINT,work_order_id BIGINT,deleted INT,pqc_decision VARCHAR,pqc_decided_at TIMESTAMP)");
            statement.execute("INSERT INTO mes_pro_edhr_batch_execution_origin VALUES(1,1,11,100,0),(1,1,11,100,0),(1,2,12,100,0),(2,3,21,100,0),(1,4,14,100,1),(1,5,15,100,0)");
            statement.execute("INSERT INTO mes_pro_process_pool_active_order VALUES(11,1,100,'2026-10-01 08:00:00',1),(12,1,100,'2026-10-03 08:00:00',0),(21,2,100,'2026-10-04 08:00:00',0),(14,1,100,'2026-10-05 08:00:00',0)");
            statement.execute("INSERT INTO mes_pro_process_pool_active_order_release_application VALUES(1,1,11,100,0,'APPROVE','2026-10-02 08:00:00'),(1,1,11,100,0,'REJECT','2026-10-04 08:00:00'),(1,1,12,100,0,'APPROVE','2026-10-05 08:00:00'),(2,1,11,100,0,'APPROVE','2026-10-06 08:00:00'),(1,1,11,100,1,'APPROVE','2026-10-07 08:00:00')");
            var config = new MybatisConfiguration();
            String script = cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper.class
                    .getMethod("selectHistoryTimes", Long.class, Collection.class)
                    .getAnnotation(org.apache.ibatis.annotations.Select.class).value()[0];
            var params = Map.of("tenantId", 1L, "batchIds", List.of(1L, 2L, 3L, 4L, 5L));
            var bound = new XMLLanguageDriver().createSqlSource(config, script, Map.class).getBoundSql(params);
            var mapped = new MappedStatement.Builder(config, "times", parameter -> bound, SqlCommandType.SELECT).build();
            try (var prepared = connection.prepareStatement(bound.getSql())) {
                new DefaultParameterHandler(mapped, params, bound).setParameters(prepared);
                var rows = prepared.executeQuery();
                Map<Long, LocalDateTime> starts = new HashMap<>();
                Map<Long, LocalDateTime> pqc = new HashMap<>();
                while (rows.next()) {
                    Long id = rows.getLong("batch_execution_id");
                    assertFalse(starts.containsKey(id), "重复正式来源不得增加列表行数");
                    var start = rows.getTimestamp("started_at");
                    var approved = rows.getTimestamp("pqc_released_at");
                    starts.put(id, start == null ? null : start.toLocalDateTime());
                    pqc.put(id, approved == null ? null : approved.toLocalDateTime());
                }
                assertEquals(Set.of(1L, 2L, 5L), starts.keySet());
                assertEquals(LocalDateTime.of(2026,10,1,8,0), starts.get(1L));
                assertEquals(LocalDateTime.of(2026,10,3,8,0), starts.get(2L));
                assertEquals(LocalDateTime.of(2026,10,2,8,0), pqc.get(1L));
                assertNull(pqc.get(2L), "同工单另一个周期的批准时间不能串用");
                assertNull(starts.get(5L), "无正式来源不能以创建时间补齐");
            }
        }
    }

    private List<Long> query(Connection connection, EdhrBatchExecutionPageReqVO request, String paging) throws Exception {
        return query(connection, request, paging, false, null);
    }

    private List<Long> query(Connection connection, EdhrBatchExecutionPageReqVO request, String paging,
                             boolean scan, Long beforeId) throws Exception {
        var config = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(config, MesProEdhrBatchExecutionDO.class.getName()), MesProEdhrBatchExecutionDO.class);
        var mapper = mock(MesProEdhrBatchExecutionMapper.class, CALLS_REAL_METHODS);
        doReturn(List.of()).when(mapper).selectList(any(Wrapper.class));
        if (scan) mapper.selectScanWindow(request, beforeId);
        else mapper.selectList(request);
        ArgumentCaptor<Wrapper> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).selectList(captor.capture());
        Wrapper<?> wrapper = captor.getValue();
        // 租户和逻辑删除由生产环境插件注入；此处显式限定，执行 Mapper 生成的真实状态条件。
        String sql = "SELECT id FROM mes_pro_edhr_batch_execution WHERE tenant_id=1 AND deleted=0 AND " + wrapper.getSqlSegment();
        if (paging != null) sql += " " + paging;
        var params = Map.of("ew", wrapper);
        var source = new XMLLanguageDriver().createSqlSource(config, sql, Map.class);
        var bound = source.getBoundSql(params);
        var mapped = new MappedStatement.Builder(config, "history", source, SqlCommandType.SELECT).build();
        try (var statement = connection.prepareStatement(bound.getSql())) {
            new DefaultParameterHandler(mapped, params, bound).setParameters(statement);
            var result = statement.executeQuery();
            List<Long> ids = new ArrayList<>();
            while (result.next()) ids.add(result.getLong(1));
            return ids;
        }
    }
}
