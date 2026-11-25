package cn.iocoder.yudao.module.mes.service.pro.schedule;

import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProReplanRequestMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class MesProReplanRequestMapperTest {
    @Test
    void uniqueReceiptSerializesConcurrentRequestsAndRollbackAllowsRetry() throws Exception {
        var source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:replan_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000");
        var jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE mes_pro_replan_request (tenant_id BIGINT NOT NULL, operator_id BIGINT NOT NULL, request_key VARCHAR(128) NOT NULL, payload_hash VARCHAR(64) NOT NULL, result_json CLOB, PRIMARY KEY(tenant_id,operator_id,request_key))");
        var configuration = new Configuration(new Environment("test", new SpringManagedTransactionFactory(), source));
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(MesProReplanRequestMapper.class);
        var mapper = new SqlSessionTemplate(new SqlSessionFactoryBuilder().build(configuration))
                .getMapper(MesProReplanRequestMapper.class);
        var tx = new TransactionTemplate(new DataSourceTransactionManager(source));
        tx.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
        var start = new CountDownLatch(1);
        var computed = new AtomicInteger();
        var pool = Executors.newFixedThreadPool(2);
        Callable<String> submit = () -> {
            assertTrue(start.await(5, TimeUnit.SECONDS));
            return tx.execute(status -> {
                mapper.claim(1L, 9L, "key", "hash");
                var receipt = mapper.lock(1L, 9L, "key");
                if (receipt.getResultJson() != null) return receipt.getResultJson();
                computed.incrementAndGet();
                assertEquals(1, mapper.complete(1L, 9L, "key", "original-result"));
                return "original-result";
            });
        };
        try {
            var first = pool.submit(submit);
            var second = pool.submit(submit);
            start.countDown();
            assertEquals("original-result", first.get(10, TimeUnit.SECONDS));
            assertEquals("original-result", second.get(10, TimeUnit.SECONDS));
            assertEquals(1, computed.get());
            tx.executeWithoutResult(status -> {
                mapper.claim(1L, 10L, "key", "operator-hash");
                mapper.claim(2L, 9L, "key", "tenant-hash");
                assertEquals("operator-hash", mapper.lock(1L, 10L, "key").getPayloadHash());
                assertEquals("tenant-hash", mapper.lock(2L, 9L, "key").getPayloadHash());
            });
            assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> {
                mapper.claim(1L, 9L, "retry", "failed-hash");
                mapper.complete(1L, 9L, "retry", "rolled-back");
                throw new IllegalStateException("fail computation");
            }));
            tx.executeWithoutResult(status -> {
                mapper.claim(1L, 9L, "retry", "retry-hash");
                var receipt = mapper.lock(1L, 9L, "retry");
                assertEquals("retry-hash", receipt.getPayloadHash());
                assertNull(receipt.getResultJson());
                assertEquals(1, mapper.complete(1L, 9L, "retry", "retried"));
            });
        } finally {
            pool.shutdownNow();
            jdbc.execute("DROP ALL OBJECTS");
        }
    }
}
