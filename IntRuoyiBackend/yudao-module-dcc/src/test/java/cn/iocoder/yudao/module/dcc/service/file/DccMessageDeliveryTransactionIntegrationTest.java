package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMessageJobDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMessageJobMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingMapper;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingDO;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.dao.DuplicateKeyException;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DccMessageDeliveryTransactionIntegrationTest extends BaseDbUnitTest {
    @Resource private DataSource dataSource;
    @Resource private PlatformTransactionManager transactionManager;
    @Resource private DccControlledFileMessageJobMapper messageJobMapper;
    private JdbcTemplate jdbc;
    private DccControlledFileMessageDeliveryService service;
    private NotifyMessageSendApi platform;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE IF NOT EXISTS dcc_test_notification_message (id BIGINT PRIMARY KEY)");
        jdbc.update("DELETE FROM dcc_test_notification_message");
        service = new DccControlledFileMessageDeliveryService();
        platform = mock(NotifyMessageSendApi.class);
        ReflectionTestUtils.setField(service, "messageJobMapper", messageJobMapper);
        ReflectionTestUtils.setField(service, "notifyMessageSendApi", platform);
        ReflectionTestUtils.setField(service, "transactionManager", transactionManager);
        var training=mock(DccControlledFileTrainingMapper.class);
        when(training.selectById(10L)).thenReturn(DccControlledFileTrainingDO.builder().id(10L).controlledFileId(42L).build());
        var files=mock(DccControlledFileMapper.class);
        when(files.selectById(42L)).thenReturn(DccControlledFileDO.builder().id(42L).title("培训文件").versionNo("B/1").build());
        ReflectionTestUtils.setField(service,"trainingMapper",training);
        ReflectionTestUtils.setField(service,"controlledFileMapper",files);
    }

    @AfterEach
    void clearTenant() { TenantContextHolder.clear(); }

    @Test
    void afterCommitCommitsNotificationAndSentStateInANewTransaction() {
        when(platform.sendSingleMessageIdempotentlyToAdmin(any())).thenAnswer(ignored -> {
            return platformTransaction().execute(status -> {
                jdbc.update("INSERT INTO dcc_test_notification_message VALUES (1)"); return 1L;
            });
        });
        new TransactionTemplate(transactionManager).executeWithoutResult(ignored -> {
            insertJob();
            service.dispatchMessageJob(job(), Map.of("title", "培训文件"));
            verifyNoInteractions(platform);
        });
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_test_notification_message", Integer.class));
        assertEquals("SENT", status());
    }

    @Test
    void failedSendRollsBackNotificationAndPersistsRetryableFailure() {
        when(platform.sendSingleMessageIdempotentlyToAdmin(any())).thenAnswer(ignored -> {
            return platformTransaction().execute(status -> {
                jdbc.update("INSERT INTO dcc_test_notification_message VALUES (1)");
                throw new IllegalStateException("notification write failed");
            });
        });
        new TransactionTemplate(transactionManager).executeWithoutResult(ignored -> {
            insertJob();
            service.dispatchMessageJob(job(), Map.of("title", "培训文件"));
        });
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_test_notification_message", Integer.class));
        assertEquals("FAILED", status());
    }

    @Test
    void concurrentReplaysUseOnePlatformMessageAndKeepSentState() throws Exception {
        insertJob();
        CountDownLatch firstSending = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        when(platform.sendSingleMessageIdempotentlyToAdmin(any())).thenAnswer(ignored -> {
            firstSending.countDown();
            assertTrue(secondStarted.await(10, TimeUnit.SECONDS));
            try {
                return platformTransaction().execute(status -> {
                    jdbc.update("INSERT INTO dcc_test_notification_message VALUES (1)"); return 1L;
                });
            } catch (DuplicateKeyException duplicate) {
                assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_test_notification_message", Integer.class));
                return 1L;
            }
        });
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> send(null));
            assertTrue(firstSending.await(10, TimeUnit.SECONDS));
            Future<?> second = executor.submit(() -> send(secondStarted));
            first.get(15, TimeUnit.SECONDS);
            second.get(15, TimeUnit.SECONDS);
            verify(platform, atLeastOnce()).sendSingleMessageIdempotentlyToAdmin(any());
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_test_notification_message", Integer.class));
            assertEquals("SENT", status());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private void send(CountDownLatch started) {
        TenantContextHolder.setTenantId(1L);
        try {
            if (started != null) started.countDown();
            service.replayMessageJob(job());
        } finally {
            TenantContextHolder.clear();
        }
    }

    private void insertJob() {
        jdbc.update("""
                INSERT INTO dcc_controlled_file_message_job
                  (id,business_type,business_id,template_code,recipient_user_id,status,tenant_id,deleted)
                VALUES (99001,'TRAINING',10,'dcc_training',101,'PENDING',1,0)
                """);
    }

    private DccControlledFileMessageJobDO job() {
        return DccControlledFileMessageJobDO.builder().id(99001L).status("PENDING").build();
    }

    private String status() {
        return jdbc.queryForObject("SELECT status FROM dcc_controlled_file_message_job WHERE id=99001", String.class);
    }

    private TransactionTemplate platformTransaction() {
        var transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transaction;
    }
}
