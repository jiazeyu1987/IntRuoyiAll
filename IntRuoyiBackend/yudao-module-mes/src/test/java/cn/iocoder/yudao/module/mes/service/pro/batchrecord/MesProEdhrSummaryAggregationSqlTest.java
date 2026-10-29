package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.sql.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Executes production annotation SQL against H2; no representative-selection SQL is duplicated. */
class MesProEdhrSummaryAggregationSqlTest {
    @Test void accessReaderUsesProjectionAndRetainsNoHistoryDefaults() {
        var mapper=org.mockito.Mockito.mock(MesMarketReleaseHandoverMapper.class);
        var latest=new MesMarketReleaseHandoverMapper.AccessRow();
        latest.setId(7L);latest.setBatchExecutionId(1L);latest.setOriginalUserId(10L);latest.setRecipientUserId(30L);
        latest.setActorRecipient(true);
        org.mockito.Mockito.when(mapper.selectAccessByBatchIds(List.of(1L,2L),20L)).thenReturn(List.of(latest));
        var access=new MesMarketReleaseAccess(mapper,
                org.mockito.Mockito.mock(MesProBatchRecordExecutionMapper.class),
                org.mockito.Mockito.mock(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService.class));
        var result=access.getAll(List.of(1L,2L),20L);
        assertFalse(result.get(1L).visible());assertFalse(result.get(1L).writable());
        assertEquals(MesMarketReleaseAccess.resolve(List.of(),20L),result.get(2L));
        org.mockito.Mockito.verify(mapper).selectAccessByBatchIds(List.of(1L,2L),20L);
        assertTrue(access.getAll(List.of(),20L).isEmpty());
        org.mockito.Mockito.verifyNoMoreInteractions(mapper);
    }

    @Test void latestReleaseMatchesDescendingHistoryAcrossStatusesAndKeepsOneRowPerBatch() throws Exception {
        try (Connection db = database()) {
            try (var insert = db.prepareStatement("INSERT INTO mes_pro_edhr_release_transaction VALUES(?,?,?,?,?)")) {
                String[] statuses = {"RELEASED", "WITHDRAWN", "REJECTED", null};
                for (long batch = 1; batch <= 3; batch++) for (int i = 1; i <= 400; i++) {
                    insert.setLong(1, batch * 1000 + i); insert.setLong(2, 1);
                    insert.setInt(3, i == 400 ? 1 : 0); insert.setLong(4, batch);
                    insert.setString(5, statuses[i % statuses.length]); insert.addBatch();
                }
                insert.executeBatch();
            }
            execute(db, "INSERT INTO mes_pro_edhr_release_transaction VALUES(99999,2,0,1,'RELEASED')");
            var params = Map.<String,Object>of("batchIds", List.of(1L,2L), "tenantId", 1L);
            var actual = query(db, MesProEdhrReleaseTransactionMapper.class,
                    "selectLatestByBatchExecutionIdsInternal", params);
            var old = raw(db, "SELECT * FROM mes_pro_edhr_release_transaction WHERE tenant_id=1 AND deleted=0 AND batch_execution_id IN (1,2) ORDER BY id DESC");
            Map<Long,Long> expected = new HashMap<>();
            for (var row : old) expected.putIfAbsent(number(row,"BATCH_EXECUTION_ID"), number(row,"ID"));
            assertEquals(2, actual.size());
            assertEquals(798, old.size());
            for (var row : actual) {
                assertEquals(expected.get(number(row,"BATCH_EXECUTION_ID")), number(row,"ID"));
                assertNull(row.get("RELEASE_STATUS")); // Latest NULL status must not be skipped.
            }
            execute(db,"INSERT INTO mes_pro_edhr_release_transaction VALUES(8000,1,0,1,'WITHDRAWN')");
            actual=query(db,MesProEdhrReleaseTransactionMapper.class,"selectLatestByBatchExecutionIdsInternal",params);
            assertEquals("WITHDRAWN",actual.get(0).get("RELEASE_STATUS"));
        }
    }

    @Test void handoverProjectionMatchesFullHistoryForNullAndFormerRecipientsAndRecall() throws Exception {
        try (Connection db = database()) {
            try (var insert = db.prepareStatement("INSERT INTO mes_pro_edhr_market_release_handover VALUES(?,?,?,?,?,?,?,?,?)")) {
                for (long batch=1;batch<=4;batch++) for (int i=1;i<=250;i++) {
                    insert.setLong(1,batch*1000+i);insert.setLong(2,1);insert.setInt(3,0);insert.setLong(4,batch);
                    insert.setObject(5,batch==4?null:10L);insert.setString(6,"original");
                    insert.setObject(7,i==1?null:(i==250?30L:20L));insert.setString(8,"recipient");
                    insert.setObject(9,batch%2==0?Timestamp.valueOf("2026-10-01 10:00:00"):null);insert.addBatch();
                }
                insert.executeBatch();
            }
            execute(db,"INSERT INTO mes_pro_edhr_market_release_handover VALUES(99999,2,0,1,99,'foreign',99,'foreign',NULL)");
            execute(db,"INSERT INTO mes_pro_edhr_market_release_handover VALUES(99998,1,1,1,99,'deleted',99,'deleted',NULL)");
            for (Long actor : Arrays.asList(null,10L,20L,30L,40L,99L)) {
                Map<String,Object> params = new HashMap<>();
                params.put("batchIds",List.of(1L,2L,3L,4L,5L));params.put("actor",actor);params.put("tenantId",1L);
                var actual=query(db,MesMarketReleaseHandoverMapper.class,"selectAccessByBatchIdsInternal",params);
                assertEquals(4,actual.size(),"1000 history rows must become four access rows");
                for(var row:actual) {
                    long batch=number(row,"BATCH_EXECUTION_ID");
                    var history=raw(db,"SELECT * FROM mes_pro_edhr_market_release_handover WHERE tenant_id=1 AND deleted=0 AND batch_execution_id="+batch+" ORDER BY id DESC")
                            .stream().map(MesProEdhrSummaryAggregationSqlTest::handover).toList();
                    boolean recipient=Boolean.TRUE.equals(row.get("ACTOR_RECIPIENT")) || row.get("ACTOR_RECIPIENT") instanceof Number n && n.intValue()!=0;
                    assertEquals(MesMarketReleaseAccess.resolve(history,actor),
                            MesMarketReleaseAccess.resolve(handover(row),actor,recipient));
                }
            }
            // No history retains unrestricted access, including anonymous readers.
            assertEquals(new MesMarketReleaseAccess.Access(true,true,false,false,null,null,null,null),
                    MesMarketReleaseAccess.resolve(List.of(),null));
        }
    }

    @Test void freezeAggregatesMatchOldRowsAndBoundRowsByRequestedScopes() throws Exception {
        try(Connection db=database()) {
            try(var insert=db.prepareStatement("INSERT INTO mes_pro_edhr_nonconformance_review VALUES(?,?,?,?,?,?,?,?,?)")) {
                Random random=new Random(73);
                String[] statuses={"pending_review","PENDING_REVIEW","approved",null};
                String[] dispositions={"void","VOID","release",null};
                String[] sources={"PQC_RELEASE","pqc_release","PQC_SUBMISSION",null};
                for(long id=1;id<=2000;id++) {
                    insert.setLong(1,id);insert.setLong(2,id%7==0?2:1);insert.setInt(3,id%11==0?1:0);
                    insert.setObject(4,id%5==0?null:1+random.nextInt(5));
                    insert.setObject(5,id%5==1?null:101+random.nextInt(5));
                    insert.setObject(6,id%5==2?null:201+random.nextInt(5));
                    insert.setString(7,statuses[random.nextInt(statuses.length)]);insert.setString(8,dispositions[random.nextInt(dispositions.length)]);
                    insert.setString(9,sources[random.nextInt(sources.length)]);insert.addBatch();
                }
                insert.executeBatch();
            }
            var params=Map.<String,Object>of("batchIds",List.of(1L,2L),"workOrderIds",List.of(101L,102L),
                    "activeOrderIds",List.of(201L,202L),"tenantId",1L);
            var actual=query(db,MesProEdhrNonconformanceReviewMapper.class,"selectFreezeCountsInternal",params);
            var old=raw(db,"SELECT * FROM mes_pro_edhr_nonconformance_review WHERE tenant_id=1 AND deleted=0 AND (batch_execution_id IN (1,2) OR work_order_id IN (101,102) OR active_order_id IN (201,202)) AND (review_status='pending_review' OR disposition='void')");
            Map<String,Long> expected=new HashMap<>();
            for(var row:old) {
                Long batch=number(row,"BATCH_EXECUTION_ID"),work=number(row,"WORK_ORDER_ID"),active=number(row,"ACTIVE_ORDER_ID");
                if(batch!=null && List.of(1L,2L).contains(batch) && "pending_review".equals(row.get("REVIEW_STATUS"))) expected.merge("BATCH:"+batch,1L,Long::sum);
                if(work!=null && List.of(101L,102L).contains(work)) expected.merge("WORK_ORDER:"+work,1L,Long::sum);
                if(active!=null && List.of(201L,202L).contains(active) && row.get("SOURCE_TYPE")!=null && !"PQC_RELEASE".equals(row.get("SOURCE_TYPE"))) expected.merge("ACTIVE_ORDER:"+active,1L,Long::sum);
            }
            Map<String,Long> observed=new HashMap<>();
            for(var row:actual) observed.put(row.get("SCOPE")+":"+number(row,"SCOPE_ID"),number(row,"REVIEW_COUNT"));
            assertEquals(expected,observed);
            assertTrue(old.size()>300);assertTrue(actual.size()<=6);
            assertTrue(observed.get("BATCH:1")>1,"Duplicate pending evidence must remain countable");
            var onlyBatch=Map.<String,Object>of("batchIds",List.of(1L),"workOrderIds",List.of(),"activeOrderIds",List.of(),"tenantId",1L);
            assertEquals(1,query(db,MesProEdhrNonconformanceReviewMapper.class,"selectFreezeCountsInternal",onlyBatch).size());
        }
    }

    private static MesMarketReleaseHandoverMapper.Row handover(Map<String,Object> row) {
        return new MesMarketReleaseHandoverMapper.Row().setId(number(row,"ID"))
                .setBatchExecutionId(number(row,"BATCH_EXECUTION_ID")).setOriginalUserId(number(row,"ORIGINAL_USER_ID"))
                .setRecipientUserId(number(row,"RECIPIENT_USER_ID")).setOriginalUserName((String)row.get("ORIGINAL_USER_NAME"))
                .setRecipientUserName((String)row.get("RECIPIENT_USER_NAME"))
                .setRecalledAt(row.get("RECALLED_AT")==null?null:((Timestamp)row.get("RECALLED_AT")).toLocalDateTime());
    }
    private static Long number(Map<String,Object> row,String key) {return row.get(key)==null?null:((Number)row.get(key)).longValue();}
    private static List<Map<String,Object>> query(Connection db,Class<?> mapper,String name,Map<String,Object> params) throws Exception {
        var method=Arrays.stream(mapper.getMethods()).filter(m->m.getName().equals(name)).findFirst().orElseThrow();
        // MySQL BINARY casts are variable-width; H2 BINARY defaults to one byte.
        String script=String.join(" ",method.getAnnotation(Select.class).value()).replace(" AS BINARY)"," AS VARBINARY)");
        Configuration configuration=new Configuration();
        BoundSql sql=new XMLLanguageDriver().createSqlSource(configuration,script,Map.class).getBoundSql(params);
        try(var statement=db.prepareStatement(sql.getSql())) {
            var meta=configuration.newMetaObject(params);int index=1;
            for(var mapping:sql.getParameterMappings()) {
                String property=mapping.getProperty();
                statement.setObject(index++,sql.hasAdditionalParameter(property)?sql.getAdditionalParameter(property):meta.getValue(property));
            }
            try(var rows=statement.executeQuery()){return collect(rows);}
        }
    }
    private static List<Map<String,Object>> raw(Connection db,String sql) throws Exception {
        try(var statement=db.createStatement();var rows=statement.executeQuery(sql)){return collect(rows);}
    }
    private static List<Map<String,Object>> collect(ResultSet rows) throws Exception {
        List<Map<String,Object>> result=new ArrayList<>();
        while(rows.next()) {
            Map<String,Object> row=new HashMap<>();
            for(int i=1;i<=rows.getMetaData().getColumnCount();i++) row.put(rows.getMetaData().getColumnLabel(i).toUpperCase(Locale.ROOT),rows.getObject(i));
            result.add(row);
        }
        return result;
    }
    private static void execute(Connection db,String sql) throws Exception {try(var statement=db.createStatement()){statement.execute(sql);}}
    private static Connection database() throws Exception {
        var db=DriverManager.getConnection("jdbc:h2:mem:edhr_summary_"+UUID.randomUUID()+";MODE=MySQL");
        execute(db,"CREATE TABLE mes_pro_edhr_release_transaction(id BIGINT,tenant_id BIGINT,deleted INT,batch_execution_id BIGINT,release_status VARCHAR(40))");
        execute(db,"CREATE TABLE mes_pro_edhr_market_release_handover(id BIGINT,tenant_id BIGINT,deleted INT,batch_execution_id BIGINT,original_user_id BIGINT,original_user_name VARCHAR(40),recipient_user_id BIGINT,recipient_user_name VARCHAR(40),recalled_at TIMESTAMP)");
        execute(db,"CREATE TABLE mes_pro_edhr_nonconformance_review(id BIGINT,tenant_id BIGINT,deleted INT,batch_execution_id BIGINT,work_order_id BIGINT,active_order_id BIGINT,review_status VARCHAR_IGNORECASE(40),disposition VARCHAR_IGNORECASE(40),source_type VARCHAR_IGNORECASE(40))");
        return db;
    }
}
