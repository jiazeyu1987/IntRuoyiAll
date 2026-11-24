package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class ErpOrderQuantityConcurrencyTest {
    @Test void parentLockWaitThenCurrentReadIncludesCommittedDocumentQuantity() throws Exception {
        var source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=COUNT;LOCK_TIMEOUT=5000", "sa", "");
        var jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE parent_order(id bigint primary key)");
        jdbc.update("INSERT INTO parent_order VALUES(100)");
        jdbc.execute("CREATE TABLE erp_purchase_in_items(id bigint primary key,in_id bigint,order_item_id bigint,"
                + "warehouse_id bigint,product_id bigint,product_unit_id bigint,product_price decimal(20,6),count decimal(20,6),"
                + "total_price decimal(20,6),tax_percent decimal(20,6),tax_price decimal(20,6),remark varchar(255),"
                + "creator varchar(64),updater varchar(64),create_time timestamp,update_time timestamp,deleted int default 0)");
        var configuration = new MybatisConfiguration(); configuration.addMapper(ErpPurchaseInItemMapper.class);
        var factory = new MybatisSqlSessionFactoryBean(); factory.setDataSource(source); factory.setConfiguration(configuration);
        var mapper = new SqlSessionTemplate(factory.getObject()).getMapper(ErpPurchaseInItemMapper.class);
        var transactions = new TransactionTemplate(new DataSourceTransactionManager(source));
        var writerLocked = new CountDownLatch(1); var allowCommit = new CountDownLatch(1); var readerStarted = new CountDownLatch(1);
        var workers = Executors.newFixedThreadPool(2);
        try {
            var writer = workers.submit(() -> transactions.executeWithoutResult(status -> {
                jdbc.queryForObject("SELECT id FROM parent_order WHERE id=100 FOR UPDATE", Long.class);
                jdbc.update("INSERT INTO erp_purchase_in_items(id,in_id,order_item_id,count) VALUES(1,7,10,3)");
                writerLocked.countDown();
                try { if (!allowCommit.await(5, TimeUnit.SECONDS)) throw new AssertionError("reader never started"); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
            }));
            assertTrue(writerLocked.await(5, TimeUnit.SECONDS));
            var reader = workers.submit(() -> transactions.execute(status -> {
                readerStarted.countDown();
                jdbc.queryForObject("SELECT id FROM parent_order WHERE id=100 FOR UPDATE", Long.class);
                return mapper.selectOrderItemCountSumMapByInIds(List.of(7L)).get(10L);
            }));
            assertTrue(readerStarted.await(5, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> reader.get(150, TimeUnit.MILLISECONDS));
            allowCommit.countDown(); writer.get(5, TimeUnit.SECONDS);
            assertEquals(0, new BigDecimal("3").compareTo(reader.get(5, TimeUnit.SECONDS)));
        } finally { allowCommit.countDown(); workers.shutdownNow(); }
    }
}
