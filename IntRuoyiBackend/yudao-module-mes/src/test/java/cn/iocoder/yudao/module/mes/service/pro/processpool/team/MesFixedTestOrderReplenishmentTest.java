package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MesFixedTestOrderReplenishmentTest {
    JdbcTemplate jdbc;
    MesFixedTestOrderDataService service;
    MesActiveOrderActionLog audit = mock(MesActiveOrderActionLog.class);
    final String code = MesFixedTestOrderInitializer.WORK_ORDER_CODE;
    final String raw = "{\"simulatedCopyFrom\":\"BL-1\",\"targetProductionOrderNo\":\"" + code + "\"}";

    @BeforeEach
    void setup() {
        var ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:replenishment" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE");
        jdbc = new JdbcTemplate(ds);
        ds.setURL(ds.getURL() + ";DB_CLOSE_DELAY=-1");
        jdbc.execute("CREATE TABLE erp_kingdee_production_replenishment_list_item (id BIGINT PRIMARY KEY,tenant_id BIGINT,deleted INT DEFAULT 0,source_line_key VARCHAR,source_bill_no VARCHAR,raw_payload VARCHAR,production_order_no VARCHAR,material_number VARCHAR,actual_quantity DECIMAL(18,6),production_replenishment_list_id BIGINT,updater VARCHAR)");
        jdbc.execute("CREATE TABLE erp_kingdee_production_replenishment_list (id BIGINT PRIMARY KEY,tenant_id BIGINT,deleted INT DEFAULT 0,updater VARCHAR)");
        jdbc.execute("CREATE TABLE mes_pro_process_pool_active_order (id BIGINT PRIMARY KEY,tenant_id BIGINT,work_order_id BIGINT,active_status VARCHAR,deleted INT DEFAULT 0)");
        jdbc.execute("CREATE TABLE mes_pro_process_pool_active_order_completion_receipt (id BIGINT PRIMARY KEY,tenant_id BIGINT,active_order_id BIGINT,deleted INT DEFAULT 0)");
        jdbc.execute("CREATE TABLE mes_pro_edhr_operation_audit_event (id BIGINT PRIMARY KEY,tenant_id BIGINT,object_type VARCHAR,object_id VARCHAR,deleted INT DEFAULT 0)");
        service = new MesFixedTestOrderDataService(jdbc, null, null);
        ReflectionTestUtils.setField(service, "actionLog", audit);
    }

    Map<String, Object> source() {
        return Map.of("source_line_key", "COPY-LINE", "source_bill_no", "SIM-COPY-BL-1-20260905",
                "raw_payload", raw, "material_number", "LABEL", "actual_quantity", 379);
    }

    void insertCopy(long tenant, long itemId, long headerId) {
        jdbc.update("INSERT INTO erp_kingdee_production_replenishment_list VALUES(?,?,0,NULL)", headerId, tenant);
        jdbc.update("INSERT INTO erp_kingdee_production_replenishment_list_item VALUES(?,?,0,?,?,?,?,?,?,?,NULL)",
                itemId, tenant, "COPY-LINE", "SIM-COPY-BL-1-20260905", raw, code, "LABEL", 379, headerId);
    }

    int remove() {
        return ReflectionTestUtils.invokeMethod(service, "removeHistoricalReplenishments", 186L, 20L,
                Map.of("id", 100L, "code", code), List.of(source()));
    }

    @Test
    void removesOnlyCurrentTenantSimulationAndAuditsItsOriginalRows() {
        insertCopy(186, 1, 11);
        insertCopy(1, 2, 12);
        jdbc.update("INSERT INTO mes_pro_process_pool_active_order VALUES(200,186,100,'ACTIVE',0)");
        assertEquals(1, remove());
        assertEquals(1, jdbc.queryForObject("SELECT deleted FROM erp_kingdee_production_replenishment_list_item WHERE id=1", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT deleted FROM erp_kingdee_production_replenishment_list WHERE id=11", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT deleted FROM erp_kingdee_production_replenishment_list_item WHERE id=2", Integer.class));
        verify(audit).recordPreparation(eq(20L), eq(100L), eq(false), argThat(value -> value.toString().contains("COPY-LINE")));
        assertEquals(0, remove());
    }

    @Test
    void sharedHeaderAndRealReplenishmentArePreserved() {
        insertCopy(186, 1, 11);
        jdbc.update("INSERT INTO erp_kingdee_production_replenishment_list_item(id,tenant_id,production_replenishment_list_id,source_line_key,production_order_no,raw_payload) VALUES(2,186,11,'REAL-LINE',?,'{}')", code);
        assertEquals(1, remove());
        assertEquals(0, jdbc.queryForObject("SELECT deleted FROM erp_kingdee_production_replenishment_list WHERE id=11", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT deleted FROM erp_kingdee_production_replenishment_list_item WHERE id=2", Integer.class));
        assertFalse(MesFixedTestOrderReplenishmentPolicy.isHistoricalCopy(Map.of("source_bill_no", "BL-1", "raw_payload", "{}")));
    }

    @Test
    void completedOrderCannotLoseItsSourceEvidence() {
        insertCopy(186, 1, 11);
        jdbc.update("INSERT INTO mes_pro_process_pool_active_order VALUES(200,186,100,'COMPLETED',0)");
        assertThrows(RuntimeException.class, this::remove);
        assertEquals(0, jdbc.queryForObject("SELECT deleted FROM erp_kingdee_production_replenishment_list_item WHERE id=1", Integer.class));
        verifyNoInteractions(audit);
    }

    @Test
    void signedReplenishmentCannotBeRemoved() {
        insertCopy(186, 1, 11);
        jdbc.update("INSERT INTO mes_pro_process_pool_active_order VALUES(200,186,100,'ACTIVE',0)");
        jdbc.update("INSERT INTO mes_pro_edhr_operation_audit_event VALUES(300,186,'ACTIVE_ORDER_REPLENISHMENT','200',0)");
        assertThrows(RuntimeException.class, this::remove);
        verifyNoInteractions(audit);
    }

    @Test
    void absentOptionalReplenishmentCreatesNothing() {
        MesFixedTestOrderDataService.DocumentResult result = ReflectionTestUtils.invokeMethod(service, "prepareIssues",
                "replenishment", "补料单明细", 186L, 20L, false, MesFixedTestOrderDataSet.FIRST,
                null, Map.of("id", 100L, "code", code));
        assertEquals(new MesFixedTestOrderDataService.DocumentResult("补料单明细", 0, 0, 0, 0), result);
        verifyNoInteractions(audit);
    }
}
