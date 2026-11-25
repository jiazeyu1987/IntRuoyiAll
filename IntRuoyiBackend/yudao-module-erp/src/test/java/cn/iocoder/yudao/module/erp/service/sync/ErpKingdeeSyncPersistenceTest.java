package cn.iocoder.yudao.module.erp.service.sync;

import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseOrderItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOrderItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.kingdeeautosync.ErpKingdeeTableAutoSyncPlanMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** Exercise generated mapper SQL and transaction behavior against a real relational database. */
class ErpKingdeeSyncPersistenceTest {
    private JdbcTemplate jdbc;
    private SqlSessionTemplate session;
    private TransactionTemplate transactions;

    @BeforeEach void prepare() throws Exception {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=COUNT", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        for (String type : new String[]{"purchase", "sale"}) {
            jdbc.execute("CREATE TABLE erp_" + type + "_order_items (id bigint PRIMARY KEY, order_id bigint,"
                    + "source_entry_id varchar(64), product_id bigint, product_unit_id bigint, product_price decimal(20,6),"
                    + "count decimal(20,6), total_price decimal(20,6), tax_percent decimal(20,6), tax_price decimal(20,6),"
                    + "remark varchar(255), " + (type.equals("purchase") ? "in_count" : "out_count") + " decimal(20,6),"
                    + "return_count decimal(20,6), creator varchar(64), updater varchar(64), create_time timestamp,"
                    + "update_time timestamp, deleted int DEFAULT 0)");
            jdbc.update("INSERT INTO erp_" + type + "_order_items"
                    + "(id,order_id,source_entry_id,product_id,product_unit_id,product_price,count,remark)"
                    + " VALUES (101,100,'e1',10,20,1,10,'old source remark')");
        }
        jdbc.execute("CREATE TABLE erp_kingdee_table_auto_sync_plan (id bigint PRIMARY KEY,enabled boolean,"
                + "last_auto_attempt_date date, last_auto_run_date date, auto_attempt_count int DEFAULT 0, last_auto_attempt_time timestamp, last_run_time timestamp,last_status varchar(32),last_message varchar(1000),"
                + "deleted int DEFAULT 0)");
        jdbc.update("INSERT INTO erp_kingdee_table_auto_sync_plan (id,enabled) VALUES (1,true),(2,false)");
        var configuration = new MybatisConfiguration();
        configuration.addMapper(cn.iocoder.yudao.module.erp.dal.mysql.sync.ErpKingdeeSyncRunMapper.class);
        configuration.addMapper(ErpPurchaseOrderItemMapper.class);
        configuration.addMapper(ErpSaleOrderItemMapper.class);
        configuration.addMapper(ErpKingdeeTableAutoSyncPlanMapper.class);
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setConfiguration(configuration);
        session = new SqlSessionTemplate(factory.getObject());
    }

    @Test void orderProgressUsesOrderTypeAndCommitsWithBusinessTransaction() {
        jdbc.execute("CREATE TABLE erp_kingdee_sync_run (id bigint PRIMARY KEY, sync_type varchar(64), status int,"
                + " created_count int DEFAULT 0, updated_count int DEFAULT 0, skipped_count int DEFAULT 0,"
                + " deleted int DEFAULT 0)");
        jdbc.update("INSERT INTO erp_kingdee_sync_run(id,sync_type,status) VALUES "
                + "(1,'PURCHASE_ORDER',10),(2,'SALE_ORDER',10),(3,'PRODUCTION_MATERIAL_LIST',10),(4,'PURCHASE_ORDER',40)");
        var mapper = session.getMapper(cn.iocoder.yudao.module.erp.dal.mysql.sync.ErpKingdeeSyncRunMapper.class);
        assertThrows(IllegalStateException.class, () -> transactions.executeWithoutResult(status -> {
            assertEquals(1, mapper.addOrderProgress(1L, 1, 0, 0));
            jdbc.update("UPDATE erp_purchase_order_items SET remark='rollback' WHERE id=101");
            throw new IllegalStateException("rollback both");
        }));
        assertEquals(0, jdbc.queryForObject("SELECT created_count FROM erp_kingdee_sync_run WHERE id=1", Integer.class));
        assertEquals("old source remark", jdbc.queryForObject("SELECT remark FROM erp_purchase_order_items WHERE id=101", String.class));
        transactions.executeWithoutResult(status -> {
            assertEquals(1, mapper.addOrderProgress(1L, 1, 0, 0));
            assertEquals(1, mapper.addOrderProgress(2L, 0, 1, 0));
            assertEquals(0, mapper.addOrderProgress(3L, 1, 0, 0));
            assertEquals(0, mapper.addOrderProgress(4L, 1, 0, 0));
        });
        assertEquals(1, jdbc.queryForObject("SELECT created_count FROM erp_kingdee_sync_run WHERE id=1", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT updated_count FROM erp_kingdee_sync_run WHERE id=2", Integer.class));
    }
    private ErpPurchaseOrderItemDO sourceItem() {
        return new ErpPurchaseOrderItemDO().setId(101L).setSourceEntryId("e1").setCount(BigDecimal.TEN)
                .setTotalPrice(BigDecimal.TEN).setTaxPrice(BigDecimal.ZERO)
                .setInCount(BigDecimal.ZERO).setReturnCount(BigDecimal.ZERO);
    }

    @Test void sourceNullFieldsAreActuallyClearedAndIdIsPreserved() {
        var mapper = session.getMapper(ErpPurchaseOrderItemMapper.class);
        assertEquals(1, mapper.updateSourceItem(sourceItem(), 100L));
        var stored = mapper.selectById(101L);
        assertEquals("e1", stored.getSourceEntryId());
        assertEquals(100L, stored.getOrderId());
        assertNull(stored.getRemark());
        assertNull(stored.getProductId());
        assertNull(stored.getProductUnitId());
        assertEquals(0, stored.getInCount().signum());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM erp_purchase_order_items", Integer.class));
        assertEquals(0, mapper.updateSourceItem(sourceItem(), 999L));
    }

    @Test void salesSourceUpdateAlsoClearsNullFieldsAndPersistsQuantities() {
        var mapper = session.getMapper(ErpSaleOrderItemMapper.class);
        assertEquals(1, mapper.updateSourceItem(new ErpSaleOrderItemDO().setId(101L).setSourceEntryId("e1")
                .setCount(BigDecimal.TEN).setOutCount(BigDecimal.ONE).setReturnCount(BigDecimal.ZERO), 100L));
        var stored = mapper.selectById(101L);
        assertNull(stored.getRemark());
        assertEquals(0, stored.getOutCount().compareTo(BigDecimal.ONE));
        assertEquals("e1", stored.getSourceEntryId());
    }

    @Test void failedTransactionKeepsOriginalLineAndFields() {
        var mapper = session.getMapper(ErpPurchaseOrderItemMapper.class);
        assertThrows(IllegalStateException.class, () -> transactions.executeWithoutResult(status -> {
            assertEquals(1, mapper.updateSourceItem(sourceItem(), 100L));
            throw new IllegalStateException("later source record failed");
        }));
        var stored = mapper.selectById(101L);
        assertEquals("old source remark", stored.getRemark());
        assertEquals(10L, stored.getProductId());
        assertEquals(100L, stored.getOrderId());
    }

    @Test void independentOrderTransactionsCommitBusinessAndProgressTogether() {
        jdbc.execute("CREATE TABLE order_progress (id bigint PRIMARY KEY, updated_count int)");
        jdbc.update("INSERT INTO order_progress VALUES (1,0)");
        var independent = new TransactionTemplate(transactions.getTransactionManager());
        independent.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        var mapper = session.getMapper(ErpPurchaseOrderItemMapper.class);
        assertThrows(IllegalStateException.class, () -> independent.executeWithoutResult(status -> {
            mapper.updateSourceItem(sourceItem().setRemark("failed write"), 100L);
            jdbc.update("UPDATE order_progress SET updated_count=updated_count+1 WHERE id=1");
            throw new IllegalStateException("failure after progress write");
        }));
        assertEquals("old source remark", mapper.selectById(101L).getRemark());
        assertEquals(0, jdbc.queryForObject("SELECT updated_count FROM order_progress WHERE id=1", Integer.class));
        independent.executeWithoutResult(status -> {
            mapper.updateSourceItem(sourceItem().setRemark("committed write"), 100L);
            jdbc.update("UPDATE order_progress SET updated_count=updated_count+1 WHERE id=1");
        });
        assertEquals("committed write", mapper.selectById(101L).getRemark());
        assertEquals(1, jdbc.queryForObject("SELECT updated_count FROM order_progress WHERE id=1", Integer.class));
    }

    @Test void automaticAttemptHonorsRetryIntervalDailyBudgetAndDisabledPlan() {
        var mapper = session.getMapper(ErpKingdeeTableAutoSyncPlanMapper.class);
        var day = LocalDate.of(2026, 11, 24);
        assertEquals(1, mapper.claimAutoAttempt(1L, day));
        assertEquals("RUNNING", jdbc.queryForObject("SELECT last_status FROM erp_kingdee_table_auto_sync_plan WHERE id=1", String.class));
        assertEquals(0, mapper.claimAutoAttempt(1L, day));
        assertEquals(0, mapper.claimAutoAttempt(1L, day.minusDays(1)));
        for (int attempt = 2; attempt <= 10; attempt++) {
            jdbc.update("UPDATE erp_kingdee_table_auto_sync_plan SET last_auto_attempt_time = ? WHERE id=1",
                    java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")).minusMinutes(16));
            assertEquals(1, mapper.claimAutoAttempt(1L, day), "attempt " + attempt + " should be allowed");
            assertEquals(attempt, jdbc.queryForObject(
                    "SELECT auto_attempt_count FROM erp_kingdee_table_auto_sync_plan WHERE id=1", Integer.class));
        }
        jdbc.update("UPDATE erp_kingdee_table_auto_sync_plan SET last_auto_attempt_time = ? WHERE id=1",
                java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")).minusMinutes(16));
        assertEquals(0, mapper.claimAutoAttempt(1L, day));
        assertEquals(1, mapper.claimAutoAttempt(1L, day.plusDays(1)));
        assertEquals(1, jdbc.queryForObject(
                "SELECT auto_attempt_count FROM erp_kingdee_table_auto_sync_plan WHERE id=1", Integer.class));
        assertEquals(0, mapper.claimAutoAttempt(2L, day));
    }
}
