package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DccWorkingSubmissionConditionTest {
    @Test
    void onlyOwnedUnlockedWorkingVersionCanBeClaimedOnce() throws Exception {
        var method = DccControlledFileMapper.class.getMethod("claimWorkingIterationSubmission",
                Long.class, Long.class, DccControlledFileDO.class);
        var dataSource = new UnpooledDataSource("org.h2.Driver",
                "jdbc:h2:mem:submission_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        try (var connection = dataSource.getConnection(); var sql = connection.createStatement()) {
            sql.execute("""
                    CREATE TABLE dcc_controlled_file (
                      id BIGINT PRIMARY KEY, tenant_id BIGINT, requester_id BIGINT, checked_out_by BIGINT,
                      status VARCHAR(64), deleted INT, submitter_id BIGINT, submit_idempotency_key VARCHAR(128),
                      submit_payload_hash VARCHAR(128), process_definition_key VARCHAR(128), submitted_time TIMESTAMP,
                      updater VARCHAR(64), update_time TIMESTAMP, checked_out_time TIMESTAMP, checked_out_reason VARCHAR(128))
                    """);
            sql.execute("""
                    INSERT INTO dcc_controlled_file(id,tenant_id,requester_id,checked_out_by,status,deleted) VALUES
                      (1,31,99,NULL,'WORKING',0), (2,31,99,99,'WORKING',0),
                      (3,31,99,NULL,'PENDING_MATRIX_REVIEW',0), (4,32,99,NULL,'WORKING',0),
                      (5,31,88,NULL,'WORKING',0), (6,31,99,NULL,'WORKING',1), (7,31,99,NULL,'WORKING',0)
                    """);
        }
        Configuration configuration = new Configuration(new Environment("test", new JdbcTransactionFactory(), dataSource));
        configuration.addMapper(DccControlledFileMapper.class);
        var factory = new SqlSessionFactoryBuilder().build(configuration);
        try (var session = factory.openSession(true)) {
            var mapper = session.getMapper(DccControlledFileMapper.class);
            for (long id = 1; id <= 6; id++) {
                DccControlledFileDO change = DccControlledFileDO.builder().id(id).status("PENDING_DOC_CONTROL_REVIEW")
                        .submitIdempotencyKey("key-" + id).submitPayloadHash("hash-" + id)
                        .processDefinitionKey("dcc-controlled-file-approval").submittedTime(LocalDateTime.now()).build();
                assertEquals(id == 1 ? 1 : 0, method.invoke(mapper, 31L, 99L, change), "claim file " + id);
                assertEquals(0, method.invoke(mapper, 31L, 99L, change), "cannot claim twice " + id);
            }
        }
        var start = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var submission = executor.submit(() -> {
                start.await();
                try (var session = factory.openSession(true)) {
                    return session.getMapper(DccControlledFileMapper.class).claimWorkingIterationSubmission(31L, 99L,
                            DccControlledFileDO.builder().id(7L).status("PENDING_DOC_CONTROL_REVIEW")
                                    .submitIdempotencyKey("race").submitPayloadHash("race-hash")
                                    .processDefinitionKey("dcc-controlled-file-approval").submittedTime(LocalDateTime.now()).build());
                }
            });
            var checkout = executor.submit(() -> {
                start.await();
                try (var session = factory.openSession(true)) {
                    return session.getMapper(DccControlledFileMapper.class)
                            .checkoutByIdAndTenantWhenAvailable(31L, 7L, 99L, "race");
                }
            });
            start.countDown();
            assertEquals(1, submission.get(10, java.util.concurrent.TimeUnit.SECONDS)
                    + checkout.get(10, java.util.concurrent.TimeUnit.SECONDS), "exactly one concurrent mutation wins");
        } finally {
            executor.shutdownNow();
        }
        try (var connection = dataSource.getConnection(); var sql = connection.createStatement()) {
            sql.execute("DROP ALL OBJECTS");
        }
    }
}
