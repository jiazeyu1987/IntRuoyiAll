package cn.iocoder.yudao.module.dcc.service.file;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DccDistributionBatchServiceTest extends BaseMockitoUnitTest {
    @Mock DccControlledFileMapper files;
    @Mock DccDistributionBatchMapper batches;
    @Mock JdbcTemplate jdbc;
    @Mock GxpAuditService audit;
    @Mock PlatformTransactionManager transactions;
    @Mock DccWorkflowDistributionService distribution;
    DccControlledFileDO file;
    DccControlledFileLifecycleEvent event;
    DccDistributionBatchDO saved;
    Object service;
    @BeforeEach void fixture() throws Exception {
        TenantContextHolder.setTenantId(1L);TransactionSynchronizationManager.setActualTransactionActive(true);
        var now=LocalDateTime.of(2026,10,11,9,0);file=DccControlledFileDO.builder().id(42L).masterId(10L).versionNo("B/1")
                .processInstanceId("round-42").status("ACTIVE").controlledTime(now).activatedTime(now).distributionDepartmentIdsJson("[\"61\"]").build();file.setTenantId(1L);
        event=new DccControlledFileLifecycleEvent("event42","ACTIVATED",1L,10L,42L,null,"B/1","round-42",now);
        service=assertDoesNotThrow(()->Class.forName("cn.iocoder.yudao.module.dcc.service.file.DccDistributionBatchService").getConstructor().newInstance());
        for(var pair:new Object[][]{{"controlledFileMapper",files},{"batchMapper",batches},{"jdbcTemplate",jdbc},{"gxpAuditService",audit},{"transactionManager",transactions},{"distributionService",distribution}})
            org.springframework.test.util.ReflectionTestUtils.setField(service,(String)pair[0],pair[1]);
        lenient().when(files.selectByIdAndTenantForUpdate(1L,42L)).thenReturn(file);
        lenient().when(batches.lockVersion(1L,42L,"round-42")).thenAnswer(ignored->saved);
        lenient().when(batches.selectById(51L)).thenAnswer(ignored->saved);
        lenient().when(batches.lock(1L,51L)).thenAnswer(ignored->saved);
        lenient().when(jdbc.queryForList(anyString(),any(Object[].class))).thenReturn(List.of(Map.of("id",9L,"event_key","event42","event_type","ACTIVATED","master_id",10L,"controlled_file_id",42L,"version_no","B/1","approval_process_instance_id","round-42","occurred_at",java.sql.Timestamp.valueOf(now))));
        lenient().when(batches.insert(any(DccDistributionBatchDO.class))).thenAnswer(call->{saved=call.getArgument(0);saved.setId(51L);return 1;});
        lenient().when(batches.updateById(any(DccDistributionBatchDO.class))).thenAnswer(call->{var row=call.getArgument(0,DccDistributionBatchDO.class);saved.setStatus(row.getStatus());saved.setErrorCode(row.getErrorCode());saved.setErrorMessage(row.getErrorMessage());saved.setRowVersion(row.getRowVersion());return 1;});
        lenient().when(transactions.getTransaction(any())).thenAnswer(ignored->new SimpleTransactionStatus());
    }
    @AfterEach void clear(){TenantContextHolder.clear();TransactionSynchronizationManager.setActualTransactionActive(false);}
    Object invoke(String method,Class<?>[] types,Object...values)throws Exception{
        try{return service.getClass().getMethod(method,types).invoke(service,values);}catch(java.lang.reflect.InvocationTargetException ex){if(ex.getCause() instanceof RuntimeException runtime)throw runtime;throw ex;}
    }
    Long accept()throws Exception{return(Long)invoke("acceptActivatedEvent",new Class<?>[]{DccControlledFileLifecycleEvent.class},event);}
    DccDistributionBatchResult prepare(DccDistributionRosterSnapshot snapshot,boolean policy)throws Exception{return(DccDistributionBatchResult)invoke("prepareAndDispatchBatch",new Class<?>[]{Long.class,DccDistributionRosterSnapshot.class,boolean.class},51L,snapshot,policy);}
    @Test void acceptsOnceWithFrozenVersionAndDepartmentValue()throws Exception{assertEquals(51L,accept());assertEquals(51L,accept());verify(batches).insert(any(DccDistributionBatchDO.class));assertEquals("PENDING",saved.getStatus());assertEquals("[\"61\"]",saved.getDepartmentIdsJson());verify(audit).append(any());}
    @Test void undecidedPolicyPersistsRealFailureWithoutRecipientOrFileWrites()throws Exception{accept();var result=prepare(null,false);assertEquals("FAILED",result.status());assertEquals("ROSTER_POLICY_UNDECIDED",result.errorCode());verifyNoInteractions(distribution);verify(files,never()).updateById(any(DccControlledFileDO.class));}
    @Test void missingSnapshotAndExplicitEmptyDepartmentsDoNotBecomeSent()throws Exception{accept();assertEquals("ROSTER_SNAPSHOT_MISSING",prepare(null,true).errorCode());saved.setDepartmentIdsJson("[]");assertEquals("EMPTY_SCOPE_UNDECIDED",prepare(null,true).errorCode());verifyNoInteractions(distribution);}
    @Test void acceptMustRequireActualTransaction(){TransactionSynchronizationManager.setActualTransactionActive(false);assertThrows(Exception.class,this::accept);verify(batches,never()).insert(any(DccDistributionBatchDO.class));}
    @Test void auditFailurePropagatesRatherThanFakeAccept(){doThrow(new IllegalStateException("actual policy unavailable")).when(audit).append(any());assertThrows(Exception.class,this::accept);}
    @Test void controlledEventCannotCreateDistributionBatch(){event=new DccControlledFileLifecycleEvent("event42","CONTROLLED",1L,10L,42L,null,"B/1","round-42",event.occurredAt());assertThrows(Exception.class,this::accept);verify(batches,never()).insert(any(DccDistributionBatchDO.class));}
    @Test void departmentJsonMustBeExactNormalizedLongStringArray(){
        for(String invalid:List.of("[61]","[\" 61\"]","[\"061\"]","[\"9223372036854775808\"]","[\"61\",\"61\"]","[ \"61\" ]","[\"62\",\"61\"]"))
            assertThrows(IllegalArgumentException.class,()->DccDistributionBatchService.normalizeDepartments(invalid),invalid);
        assertEquals("[\"61\",\"9223372036854775807\"]",DccDistributionBatchService.normalizeDepartments("[\"61\",\"9223372036854775807\"]"));
    }
    @Test void failureCodeAndExceptionMustBeRealBeforeAnyWrite()throws Exception{accept();
        assertThrows(Exception.class,()->invoke("recordFailure",new Class<?>[]{Long.class,String.class,RuntimeException.class},51L,"",new IllegalStateException("x")));
        assertThrows(Exception.class,()->invoke("recordFailure",new Class<?>[]{Long.class,String.class,RuntimeException.class},51L,"FAILED",null));
        verify(batches,never()).updateById(any(DccDistributionBatchDO.class));
    }
}
