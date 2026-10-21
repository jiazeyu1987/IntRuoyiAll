package cn.iocoder.yudao.server.profileworkbench;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.*;
import cn.iocoder.yudao.module.system.service.profileworkbench.index.ProjectionRepository;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.*;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.*;
import org.springframework.transaction.event.TransactionalEventListenerFactory;

import java.sql.Connection;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Actual event listener, AOP, Tx-C transaction owner, Mapper guard and H2 commits.
 * Producer business and projection exports are explicit boundary doubles; the MES
 * Tx-C audit transaction suite separately exercises the full producer and trace graph.
 */
class ProjectionEdhrTraceTransactionTest {
    public interface BatchMapper {
        @Update("UPDATE mes_pro_edhr_batch_execution SET provisioning_status=#{status} WHERE id=1 AND tenant_id=2")
        int updateStatus(String status);
    }

    private DriverManagerDataSource dataSource;
    private DataSourceTransactionManager manager;
    private JdbcTemplate jdbc;
    private BatchMapper mapper;
    private AnnotationConfigApplicationContext events;
    private ProjectionWriteBoundaryAspect aspect;
    private ProjectionRepository repository;
    private MesProEdhrBatchTraceTransactionService trace;
    private MesProEdhrBatchTraceTxCProducer business;
    private MesProEdhrBatchTraceTxCInvoker producer;
    private final List<Connection> attemptConnections=new ArrayList<>();
    private final Global global=new Global(0,0,"test-registry");

    @BeforeEach @SuppressWarnings("unchecked") void setup() throws Exception {
        dataSource=new DriverManagerDataSource("jdbc:h2:mem:trace_fence_"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1", "sa", "");
        manager=new DataSourceTransactionManager(dataSource);jdbc=new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE mes_pro_edhr_batch_execution(id BIGINT PRIMARY KEY,tenant_id BIGINT,provisioning_status VARCHAR(64))");
        jdbc.update("INSERT INTO mes_pro_edhr_batch_execution VALUES(1,2,'BATCH_PROVISIONING')");
        jdbc.execute("CREATE TABLE test_audit(id INT PRIMARY KEY,result VARCHAR(64))");
        jdbc.execute("CREATE TABLE test_parent(id INT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE test_head(id INT PRIMARY KEY,head BIGINT)");
        jdbc.update("INSERT INTO test_head VALUES(2,0)");
        jdbc.execute("CREATE TABLE test_batch(sequence BIGINT PRIMARY KEY)");
        var configuration=new Configuration(new Environment("test",new SpringManagedTransactionFactory(),dataSource));
        configuration.addInterceptor(new ProjectionWriterGuard());configuration.addMapper(BatchMapper.class);
        mapper=new SqlSessionTemplate(new SqlSessionFactoryBuilder().build(configuration)).getMapper(BatchMapper.class);
        repository=mock(ProjectionRepository.class);
        when(repository.lockGlobal(false)).thenAnswer(i->{assertTrue(TransactionSynchronizationManager.isActualTransactionActive());return global;});
        when(repository.lockTenant(2,true)).thenAnswer(i->new Tenant(2,jdbc.queryForObject("SELECT head FROM test_head WHERE id=2 FOR UPDATE",Long.class),0,null));
        doAnswer(i->{Batch batch=i.getArgument(0);jdbc.update("INSERT INTO test_batch VALUES(?)",batch.sequence());return null;}).when(repository).append(any());
        doAnswer(i->{Tenant old=i.getArgument(0),next=i.getArgument(1);
            assertEquals(1,jdbc.update("UPDATE test_head SET head=? WHERE id=2 AND head=?",next.head(),old.head()));return null;
        }).when(repository).advanceTenant(any(),any());
        var exports=mock(ProjectionExportRegistry.class);
        when(exports.export(eq(2L),eq(global),any())).thenAnswer(i->new FrozenExport(2,new Watermark(0,((Tenant)i.getArgument(2)).head()),"test-registry",Set.of("EDHR_WORK_TASK"),List.of(),List.of()));
        when(exports.changes(any(),any())).thenReturn(List.of());
        ObjectProvider<ProjectionExportRegistry> provider=mock(ObjectProvider.class);when(provider.getObject()).thenReturn(exports);
        aspect=new ProjectionWriteBoundaryAspect(repository,provider,manager,dataSource);
        trace=proxy(new MesProEdhrBatchTraceTransactionService(manager));
        business=mock(MesProEdhrBatchTraceTxCProducer.class);
        producer=proxy(business);
        events=new AnnotationConfigApplicationContext();
        events.registerBean(TransactionalEventListenerFactory.class);
        events.registerBean(MesProEdhrBatchTraceTxCApplicationService.class,()->proxy(new MesProEdhrBatchTraceTxCApplicationService(producer)));
        events.refresh();TenantContextHolder.setTenantId(2L);
    }

    private <T> T proxy(T target) {
        var factory=new AspectJProxyFactory(target);factory.setProxyTargetClass(true);factory.addAspect(aspect);return factory.getProxy();
    }

    @AfterEach void close() {
        if(events!=null)events.close();
        ProjectionWriteContext.CURRENT.remove();TenantContextHolder.clear();DynamicDataSourceContextHolder.clear();
        if(jdbc!=null)jdbc.execute("SHUTDOWN");
    }

    private MesProEdhrBatchProvisionedEvent event() {
        return new MesProEdhrBatchProvisionedEvent().setTenantId(2L).setBatchExecutionId(1L)
            .setProvisioningReceiptId(3L).setEventId("trace-1").setIdempotencyKey("trace-1");
    }

    private MesProEdhrBatchTraceTxCResult write(String state) {
        assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
        var held=Objects.requireNonNull(ProjectionWriteContext.CURRENT.get());
        Connection connection=DataSourceUtils.getConnection(dataSource);assertSame(connection,held.connection);
        assertEquals(2L,held.tenant);assertEquals("master",DynamicDataSourceContextHolder.peek());
        attemptConnections.add(connection);
        assertEquals(1,mapper.updateStatus(state));
        jdbc.update("INSERT INTO test_audit VALUES(1,?)",state);
        return new MesProEdhrBatchTraceTxCResult().setStatus(state);
    }

    private void publishFromParent() {
        assertDoesNotThrow(()->{
            aspect.engineWrite(()->{
                assertNotNull(ProjectionWriteContext.CURRENT.get());
                jdbc.update("INSERT INTO test_parent VALUES(1)");events.publishEvent(event());
                assertEquals(0,rows("test_batch"));verifyNoInteractions(business);
                return null;
            });
        });
    }

    private int rows(String table){return jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class);}
    private String state(){return jdbc.queryForObject("SELECT provisioning_status FROM mes_pro_edhr_batch_execution WHERE id=1",String.class);}
    private long head(){return jdbc.queryForObject("SELECT head FROM test_head WHERE id=2",Long.class);}

    @Test void afterCommitUsesOneIndependentFenceAndCommitsBatchAuditAndProjectionTogether() {
        var parentConnection=new AtomicReference<Connection>();
        when(business.produce(any())).thenAnswer(i->{
            assertNull(ProjectionWriteContext.CURRENT.get(),"The orchestration entry must not acquire a fence");
            parentConnection.set(DataSourceUtils.getConnection(dataSource));
            return trace.execute(status->write("BATCH_READY"));
        });
        DynamicDataSourceContextHolder.push("caller");publishFromParent();
        assertEquals("BATCH_READY",state());assertEquals(1,rows("test_parent"));assertEquals(1,rows("test_audit"));
        assertEquals(1,rows("test_batch"));assertEquals(1,head());
        assertNotSame(parentConnection.get(),attemptConnections.get(0));assertNull(ProjectionWriteContext.CURRENT.get());
        assertEquals("caller",DynamicDataSourceContextHolder.peek());
    }

    @Test void failedAttemptRollsBackBeforeFreshFailureTransactionAndOnlyThatCommitAdvancesHead() {
        var failure=new IllegalArgumentException("source witness changed");
        when(business.produce(any())).thenAnswer(i->{
            assertSame(failure,assertThrows(IllegalArgumentException.class,()->trace.execute(status->{write("BATCH_READY");throw failure;})));
            assertNull(ProjectionWriteContext.CURRENT.get());assertEquals("BATCH_PROVISIONING",state());
            return trace.execute(status->write("BATCH_PROVISIONING_RETRYABLE"));
        });
        publishFromParent();
        assertEquals("BATCH_PROVISIONING_RETRYABLE",state());assertEquals(1,rows("test_audit"));assertEquals(1,head());
        assertEquals(1,rows("test_batch"));assertNotSame(attemptConnections.get(0),attemptConnections.get(1));
        assertNull(ProjectionWriteContext.CURRENT.get());
    }

    @Test void auditFailureRollsBackTheIndependentAttemptAndDoesNotPublishItsProjection() {
        var failure=new IllegalStateException("audit write failed");
        var observed=new AtomicReference<Throwable>();
        when(business.produce(any())).thenAnswer(i->{
            observed.set(assertThrows(IllegalStateException.class,()->trace.execute(status->{write("BATCH_READY");throw failure;})));
            return null;
        });
        publishFromParent();
        assertSame(failure,observed.get());assertEquals(1,attemptConnections.size());
        assertEquals(1,rows("test_parent"));assertEquals("BATCH_PROVISIONING",state());
        assertEquals(0,rows("test_audit"));assertEquals(0,rows("test_batch"));assertEquals(0,head());
        verify(repository,never()).append(any());assertNull(ProjectionWriteContext.CURRENT.get());
    }

    @Test void projectionFailureRollsBackBusinessAndAuditButKeepsAlreadyCommittedParent() {
        var failure=new IllegalStateException("projection append failed");
        var observed=new AtomicReference<Throwable>();
        doAnswer(i->{jdbc.update("INSERT INTO test_batch VALUES(1)");throw failure;}).when(repository).append(any());
        when(business.produce(any())).thenAnswer(i->{
            observed.set(assertThrows(IllegalStateException.class,()->trace.execute(status->write("BATCH_READY"))));return null;
        });
        publishFromParent();
        assertSame(failure,observed.get());assertEquals(1,attemptConnections.size());
        assertEquals(1,rows("test_parent"));assertEquals("BATCH_PROVISIONING",state());assertEquals(0,rows("test_audit"));
        assertEquals(0,rows("test_batch"));assertEquals(0,head());assertNull(ProjectionWriteContext.CURRENT.get());
    }

    @Test void replayWithoutFormalMutationDoesNotAppendAnotherBatch() {
        trace.execute(status->write("BATCH_READY"));
        assertEquals("BATCH_READY",trace.execute(status->state()));
        assertEquals(1,head());assertEquals(1,rows("test_batch"));assertEquals(1,rows("test_audit"));
        verify(repository,times(1)).append(any());
    }

    @Test void independentCommitSurvivesUnfencedParentRollbackAndRestoresItsConnection() {
        new TransactionTemplate(manager).executeWithoutResult(status->{
            Connection parent=DataSourceUtils.getConnection(dataSource);jdbc.update("INSERT INTO test_parent VALUES(1)");
            trace.execute(inner->write("BATCH_READY"));assertSame(parent,DataSourceUtils.getConnection(dataSource));
            assertNotSame(parent,attemptConnections.get(0));assertNull(ProjectionWriteContext.CURRENT.get());status.setRollbackOnly();
        });
        assertEquals(0,rows("test_parent"));assertEquals("BATCH_READY",state());assertEquals(1,rows("test_audit"));assertEquals(1,head());
    }

    @Test void liveFencedParentCannotDeadlockItselfByStartingTxCBeforeCommit() {
        new TransactionTemplate(manager).executeWithoutResult(status->{
            var parent=new ProjectionWriteContext(Scope.TENANT,2,DataSourceUtils.getConnection(dataSource));
            ProjectionWriteContext.CURRENT.set(parent);var calls=new AtomicInteger();
            assertThrows(IllegalStateException.class,()->trace.execute(inner->calls.incrementAndGet()));
            assertEquals(0,calls.get());assertSame(parent,ProjectionWriteContext.CURRENT.get());ProjectionWriteContext.CURRENT.remove();
        });
        verifyNoInteractions(repository);assertEquals(0,head());
    }

    @Test void protocolDisabledKeepsTheMesIndependentTransactionWithoutWorkbenchInfrastructure() {
        var original=new MesProEdhrBatchTraceTransactionService(manager);
        new TransactionTemplate(manager).executeWithoutResult(status->{
            Connection parent=DataSourceUtils.getConnection(dataSource);
            original.execute(inner->{assertNotSame(parent,DataSourceUtils.getConnection(dataSource));
                assertNull(ProjectionWriteContext.CURRENT.get());jdbc.update("INSERT INTO test_audit VALUES(1,'original')");return null;});
            status.setRollbackOnly();
        });
        assertEquals(1,rows("test_audit"));verifyNoInteractions(repository);
    }
}
