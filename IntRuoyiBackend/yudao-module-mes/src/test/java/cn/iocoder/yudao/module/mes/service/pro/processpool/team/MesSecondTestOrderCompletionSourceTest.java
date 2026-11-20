package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Real isolated SQL checks for the second fixture's completion evidence. */
class MesSecondTestOrderCompletionSourceTest {
    private final MesFixedTestOrderInitializerTest fixture = new MesFixedTestOrderInitializerTest();
    private MesFixedTestOrderCompletionSourceInitializer initializer;

    @BeforeEach
    void setup() {
        fixture.setup();
        for (String table : List.of("mes_pro_work_order", "mes_wm_product_issue", "mes_wm_product_issue_line",
                "mes_wm_product_issue_detail", "mes_wm_batch", "mes_wm_material_stock")) fixture.createTable(table);
        var jdbc = fixture.jdbc;
        jdbc.execute("CREATE TABLE system_tenant(id BIGINT PRIMARY KEY,deleted INT)");
        jdbc.update("INSERT INTO system_tenant VALUES(165,0)");
        jdbc.update("INSERT INTO mes_pro_work_order(id,tenant_id,deleted,code) VALUES(300,1,0,?),(900,165,0,?),(901,165,0,?),(902,165,0,'ORDINARY')",
                MesFixedTestOrderDataSet.FIRST.workOrderCode(), MesFixedTestOrderDataSet.FIRST.workOrderCode(),
                MesFixedTestOrderDataSet.SECOND.workOrderCode());
        jdbc.update("INSERT INTO mes_wm_product_issue(id,tenant_id,deleted,code,name,work_order_id,status) VALUES(201,1,0,'ISSUE','Reference issue',300,4)");
        jdbc.update("INSERT INTO mes_wm_batch(id,tenant_id,deleted,code,item_id,work_order_id) VALUES(202,1,0,'LOT',10,300)");
        jdbc.update("INSERT INTO mes_wm_material_stock(id,tenant_id,deleted,item_id,batch_id,batch_code,quantity) VALUES(203,1,0,10,202,'LOT',100)");
        jdbc.update("INSERT INTO mes_wm_product_issue_line(id,tenant_id,deleted,issue_id,item_id,quantity,batch_id) VALUES(204,1,0,201,10,5,202)");
        jdbc.update("INSERT INTO mes_wm_product_issue_detail(id,tenant_id,deleted,issue_id,line_id,material_stock_id,item_id,quantity,batch_id,batch_code) VALUES(205,1,0,201,204,203,10,5,202,'LOT')");
        TenantContextHolder.setTenantId(165L);
        initializer = new MesFixedTestOrderCompletionSourceInitializer(jdbc);
    }

    @AfterEach
    void clear() { TenantContextHolder.clear(); }

    @Test
    void secondOrderImportsIndependentEvidenceAndLeavesFirstAndReferenceUntouched() {
        fixture.tx.executeWithoutResult(s -> initializer.prepare(900L, 77L));
        var first = fixture.jdbc.queryForList("SELECT * FROM mes_wm_product_issue WHERE tenant_id=165");
        var firstStocks = fixture.jdbc.queryForList("SELECT * FROM mes_wm_material_stock WHERE tenant_id=165");
        fixture.tx.executeWithoutResult(s -> initializer.prepare(901L, 77L));
        fixture.tx.executeWithoutResult(s -> initializer.prepare(901L, 77L));
        assertEquals(first, fixture.jdbc.queryForList("SELECT * FROM mes_wm_product_issue WHERE tenant_id=165 AND work_order_id=900"));
        assertEquals(firstStocks, fixture.jdbc.queryForList("SELECT s.* FROM mes_wm_material_stock s JOIN mes_wm_batch b ON b.id=s.batch_id WHERE s.tenant_id=165 AND b.work_order_id=900"));
        assertEquals(2, fixture.jdbc.queryForObject("SELECT COUNT(*) FROM mes_wm_product_issue WHERE tenant_id=165", Integer.class));
        assertEquals(2, fixture.jdbc.queryForObject("SELECT COUNT(DISTINCT material_stock_id) FROM mes_wm_product_issue_detail WHERE tenant_id=165", Integer.class));
        var second = fixture.jdbc.queryForMap("SELECT i.code issue_code,b.code batch_code,s.batch_code stock_batch_code,d.batch_code detail_batch_code FROM mes_wm_product_issue i JOIN mes_wm_product_issue_detail d ON d.issue_id=i.id AND d.tenant_id=i.tenant_id JOIN mes_wm_batch b ON b.id=d.batch_id AND b.tenant_id=d.tenant_id JOIN mes_wm_material_stock s ON s.id=d.material_stock_id AND s.tenant_id=d.tenant_id WHERE i.tenant_id=165 AND i.work_order_id=901 AND b.work_order_id=901");
        assertTrue(second.get("issue_code").toString().startsWith("FT2-"));
        assertNotEquals("LOT", second.get("batch_code"));
        assertEquals(second.get("batch_code"), second.get("stock_batch_code"));
        assertEquals(second.get("batch_code"), second.get("detail_batch_code"));
        assertEquals("ISSUE", fixture.jdbc.queryForObject("SELECT code FROM mes_wm_product_issue WHERE tenant_id=1", String.class));
        assertEquals("LOT", fixture.jdbc.queryForObject("SELECT batch_code FROM mes_wm_product_issue_detail WHERE tenant_id=1", String.class));
    }

    @Test
    void badReferenceIdentityFailsBeforeAnySecondOrderImport() {
        fixture.jdbc.update("UPDATE mes_wm_product_issue_detail SET batch_code='WRONG' WHERE id=205");
        assertThrows(RuntimeException.class, () -> fixture.tx.executeWithoutResult(s -> initializer.prepare(901L, 77L)));
        assertEquals(0, fixture.jdbc.queryForObject("SELECT COUNT(*) FROM mes_wm_product_issue WHERE tenant_id=165", Integer.class));
        assertEquals(0, fixture.jdbc.queryForObject("SELECT COUNT(*) FROM mes_wm_material_stock WHERE tenant_id=165", Integer.class));
    }

    @Test
    void ordinaryOrdersRemainSubjectToTheirOwnFormalSources() {
        fixture.tx.executeWithoutResult(s -> initializer.prepare(902L, 77L));
        assertEquals(0, fixture.jdbc.queryForObject("SELECT COUNT(*) FROM mes_wm_product_issue WHERE tenant_id=165", Integer.class));
    }
}
