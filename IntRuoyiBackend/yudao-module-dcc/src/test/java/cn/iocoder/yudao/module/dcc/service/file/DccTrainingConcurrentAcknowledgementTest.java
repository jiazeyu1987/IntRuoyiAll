package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingAssignmentDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;

class DccTrainingConcurrentAcknowledgementTest extends BaseDbUnitTest {
    @Resource private DataSource dataSource;
    @Resource private PlatformTransactionManager transactionManager;
    @Resource(name = "dccControlledFileMapper") private DccControlledFileMapper fileMapper;
    @Resource private DccControlledFileTrainingMapper trainingMapper;
    @Resource private DccControlledFileTrainingAssignmentMapper assignmentMapper;
    @Resource private DccControlledFileTrainingProgressMapper progressMapper;

    @Test
    void lastTwoUsersConfirmConcurrentlyAndCompleteTheFileGate() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("""
                INSERT INTO dcc_controlled_file (id,master_id,category_id,directory_id,source_file_id,title,file_name,file_number,version_no,
                  original_file_id,submitter_id,requester_id,status,tenant_id,deleted)
                VALUES (99001,99001,10,20,100,'培训文件','培训文件','TRAIN-99001','A/1',100,99,99,'TRAINING_IN_PROGRESS',1,0)
                """);
        jdbc.update("""
                INSERT INTO dcc_controlled_file_training (id,controlled_file_id,department_id,status,tenant_id,deleted)
                VALUES (99002,99001,10,'PENDING',1,0)
                """);
        for (long user : new long[]{101, 102}) {
            jdbc.update("""
                    INSERT INTO dcc_controlled_file_training_assignment (id,training_id,user_id,status,tenant_id,deleted)
                    VALUES (?,99002,?,'PENDING',1,0)
                    """, 99000 + user, user);
            jdbc.update("""
                    INSERT INTO dcc_controlled_file_training_progress
                      (id,controlled_file_id,user_id,required_view_seconds,accumulated_view_seconds,tenant_id,deleted)
                    VALUES (?,99001,?,600,600,1,0)
                    """, 99500 + user, user);
        }
        CountDownLatch firstBeforeWrite = new CountDownLatch(1);
        CountDownLatch secondReadOrLock = new CountDownLatch(1);
        ThreadLocal<Long> currentUser = new ThreadLocal<>();
        DccControlledFileMapper files = mock(DccControlledFileMapper.class, invocation -> {
            if (Long.valueOf(102).equals(currentUser.get())
                    && invocation.getMethod().getName().equals("selectByIdAndTenantForUpdate")) {
                secondReadOrLock.countDown();
            }
            return delegatesTo(fileMapper).answer(invocation);
        });
        DccControlledFileTrainingAssignmentMapper assignments = mock(DccControlledFileTrainingAssignmentMapper.class,
                invocation -> {
                    String method = invocation.getMethod().getName();
                    if (method.equals("updateById") && invocation.getArgument(0) instanceof DccControlledFileTrainingAssignmentDO row
                            && Long.valueOf(99101).equals(row.getId())) {
                        firstBeforeWrite.countDown();
                        assertTrue(secondReadOrLock.await(10, TimeUnit.SECONDS), "第二个确认必须已开始竞争");
                    }
                    Object result = delegatesTo(assignmentMapper).answer(invocation);
                    if (Long.valueOf(102).equals(currentUser.get()) && method.startsWith("selectListByTrainingId")) {
                        secondReadOrLock.countDown();
                    }
                    return result;
                });
        DccTrainingAssignmentAckServiceImpl service = new DccTrainingAssignmentAckServiceImpl();
        ReflectionTestUtils.setField(service, "controlledFileMapper", files);
        ReflectionTestUtils.setField(service, "trainingMapper", trainingMapper);
        ReflectionTestUtils.setField(service, "trainingAssignmentMapper", assignments);
        ReflectionTestUtils.setField(service, "trainingProgressMapper", progressMapper);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> confirm(service, currentUser, 101));
            assertTrue(firstBeforeWrite.await(10, TimeUnit.SECONDS), "第一个确认必须到达业务写入");
            Future<?> second = executor.submit(() -> confirm(service, currentUser, 102));
            first.get(15, TimeUnit.SECONDS);
            second.get(15, TimeUnit.SECONDS);
            assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_training_assignment WHERE training_id=99002 AND status='ACKNOWLEDGED'", Integer.class));
            assertEquals("ACKNOWLEDGED", jdbc.queryForObject("SELECT status FROM dcc_controlled_file_training WHERE id=99002", String.class));
            assertEquals("PENDING_MANUAL_DISTRIBUTION", jdbc.queryForObject("SELECT status FROM dcc_controlled_file WHERE id=99001", String.class));
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private void confirm(DccTrainingAssignmentAckServiceImpl service, ThreadLocal<Long> userContext, long user) {
        TenantContextHolder.setTenantId(1L);
        userContext.set(user);
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(ignored -> service.acknowledgeTraining(user, 99001L));
        } finally {
            userContext.remove();
            TenantContextHolder.clear();
        }
    }
}
