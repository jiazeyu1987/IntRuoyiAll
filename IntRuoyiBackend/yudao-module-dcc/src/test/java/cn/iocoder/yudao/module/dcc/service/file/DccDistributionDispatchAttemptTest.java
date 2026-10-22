package cn.iocoder.yudao.module.dcc.service.file;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class DccDistributionDispatchAttemptTest extends BaseMockitoUnitTest {
    @Mock DccDistributionBatchMapper batches;
    @Mock DccDistributionDispatchAttemptMapper attempts;
    @Mock DccControlledFileMessageJobMapper jobs;
    @Mock GxpAuditService audit;
    Object service;DccDistributionBatchDO batch;DccControlledFileMessageJobDO job;
    List<DccDistributionDispatchAttemptDO> rows=new ArrayList<>();
    @BeforeEach void fixture()throws Exception {
        TenantContextHolder.setTenantId(1L);TransactionSynchronizationManager.setActualTransactionActive(true);
        service=assertDoesNotThrow(()->Class.forName("cn.iocoder.yudao.module.dcc.service.file.DccDistributionDispatchAttemptService").getConstructor().newInstance());
        for(var pair:new Object[][]{{"batchMapper",batches},{"attemptMapper",attempts},{"messageJobMapper",jobs},{"gxpAuditService",audit}})
            org.springframework.test.util.ReflectionTestUtils.setField(service,(String)pair[0],pair[1]);
        batch=DccDistributionBatchDO.builder().id(51L).tenantId(1L).status("DISPATCH_READY").rowVersion(1L).build();
        job=DccControlledFileMessageJobDO.builder().id(81L).tenantId(1L).distributionBatchId(51L).businessType("DISTRIBUTION").recipientUserId(99L).templateCode("dcc_distribution").status("PENDING").build();
        lenient().when(batches.lock(1L,51L)).thenReturn(batch);lenient().when(jobs.selectByIdAndTenantForUpdate(1L,81L)).thenReturn(job);
        lenient().when(attempts.lockJob(1L,81L)).thenAnswer(ignored->List.copyOf(rows));
        lenient().when(attempts.lock(eq(1L),anyLong())).thenAnswer(call->rows.stream().filter(row->row.getId().equals(call.getArgument(1))).findFirst().orElse(null));
        lenient().when(attempts.insert(any(DccDistributionDispatchAttemptDO.class))).thenAnswer(call->{var row=call.getArgument(0,DccDistributionDispatchAttemptDO.class);row.setId(91L+rows.size());rows.add(row);return 1;});
        lenient().when(attempts.updateById(any(DccDistributionDispatchAttemptDO.class))).thenReturn(1);
        lenient().when(batches.updateById(any(DccDistributionBatchDO.class))).thenReturn(1);
        lenient().when(jobs.selectBatchJobsForUpdate(1L,51L)).thenAnswer(ignored->List.of(job));
    }
    @AfterEach void clear(){TenantContextHolder.clear();TransactionSynchronizationManager.setActualTransactionActive(false);}
    Object call(String name,Class<?>[] types,Object...values)throws Exception{try{return service.getClass().getMethod(name,types).invoke(service,values);}catch(java.lang.reflect.InvocationTargetException error){if(error.getCause() instanceof RuntimeException runtime)throw runtime;throw error;}}
    DccDistributionDispatchAttemptDO start()throws Exception{return(DccDistributionDispatchAttemptDO)call("startAttempt",new Class<?>[]{Long.class,Long.class},51L,81L);}
    void finish(Long id,boolean sent,RuntimeException error)throws Exception{call("finishAttempt",new Class<?>[]{Long.class,Long.class,boolean.class,Long.class,RuntimeException.class},51L,id,sent,sent?901L:null,error);}
    @Test void repeatedAttemptKeepsSamePlatformKeyWithDistinctRealAttemptNumbers()throws Exception{var one=start();var two=start();assertEquals(1,one.getAttemptNo());assertEquals(2,two.getAttemptNo());assertEquals("DCC_MESSAGE_JOB:1:81",one.getBusinessKey());assertEquals(one.getBusinessKey(),two.getBusinessKey());assertEquals("STARTED",one.getStatus());}
    @Test void fullFailureTextIsRetainedAndSensitiveMaterialRedacted()throws Exception{var row=start();String message="失败".repeat(700)+" password=private";finish(row.getId(),false,new IllegalStateException(message));assertEquals("FAILED",row.getStatus());assertTrue(row.getErrorMessage().length()>500);assertFalse(row.getErrorMessage().contains("private"));assertNotNull(row.getFinishedAt());}
    @Test void alreadySentJobCannotBeOverwrittenByConcurrentFailure()throws Exception{var row=start();job.setStatus("SENT");job.setSentAt(LocalDateTime.of(2026,10,11,10,0));finish(row.getId(),false,new IllegalStateException("late failure"));assertEquals("SENT",batch.getStatus());assertEquals(job.getSentAt(),batch.getSentAt());}
    @Test void startedAttemptRecoveryNeverCreatesAnotherReceiptOrPlatformIdentity()throws Exception{var row=start();finish(row.getId(),false,new IllegalStateException("ack lost"));var retry=start();finish(retry.getId(),true,null);assertEquals(2,rows.size());assertEquals(row.getBusinessKey(),retry.getBusinessKey());assertEquals("SENT",retry.getStatus());}
    @Test void noTransactionOrForeignBatchNeverCreatesAttempt(){TransactionSynchronizationManager.setActualTransactionActive(false);assertThrows(Exception.class,this::start);verify(attempts,never()).insert(any(DccDistributionDispatchAttemptDO.class));}
    @Test void maxIntAttemptCannotOverflowIntoAnotherIdentity()throws Exception{rows.add(DccDistributionDispatchAttemptDO.builder().id(90L).attemptNo(Integer.MAX_VALUE).build());assertThrows(Exception.class,this::start);verify(attempts,never()).insert(any(DccDistributionDispatchAttemptDO.class));}
    @Test void completedAttemptReplayMustMatchOutcomeAndMessage()throws Exception{var row=start();finish(row.getId(),true,null);finish(row.getId(),true,null);
        assertThrows(Exception.class,()->finish(row.getId(),false,new IllegalStateException("changed failure")));
        assertThrows(Exception.class,()->call("finishAttempt",new Class<?>[]{Long.class,Long.class,boolean.class,Long.class,RuntimeException.class},51L,row.getId(),true,902L,null));}
}
