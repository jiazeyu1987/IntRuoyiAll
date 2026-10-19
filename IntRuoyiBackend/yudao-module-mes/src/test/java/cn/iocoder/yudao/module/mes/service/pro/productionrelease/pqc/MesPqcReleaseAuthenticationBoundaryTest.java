package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.service.signature.SystemSignatureAuthenticationService;
import cn.iocoder.yudao.module.system.api.signature.dto.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import cn.iocoder.yudao.module.system.api.permission.ApprovalAuthorityApi;
import org.junit.jupiter.api.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.support.*;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class MesPqcReleaseAuthenticationBoundaryTest {
    @AfterEach void clear(){TenantContextHolder.clear();}
    @Test void failedPasswordAttemptCommitsBeforeReleaseFailureAndNeverAcquiresBusinessLocks() {
        var f=new Fixture(false);
        assertThrows(RuntimeException.class,()->new TransactionTemplate(f.transactions).executeWithoutResult(tx->f.proxy.approve(2L,f.command())));
        assertEquals(1,f.db.queryForObject("SELECT attempts FROM auth",Integer.class));
        verifyNoInteractions(f.ledger,f.authority);
    }
    @Test void successfulAuthenticationIsRevalidatedInsideBusinessTransactionAndSurvivesBusinessFailure() {
        var f=new Fixture(true);
        doAnswer(c->{assertTrue(TransactionSynchronizationManager.isActualTransactionActive());throw new IllegalStateException("credential disabled after authentication");})
                .when(f.authentication).lockAndRevalidate(any(),any());
        assertThrows(RuntimeException.class,()->f.proxy.approve(2L,f.command()));
        assertEquals(1,f.db.queryForObject("SELECT attempts FROM auth",Integer.class));
        var order=inOrder(f.authentication,f.ledger,f.authority);
        order.verify(f.authentication).authenticate(any());order.verify(f.ledger).acquireLedgerLock();
        order.verify(f.authority).lockTenantApprovalAuthority(71L);order.verify(f.authentication).lockAndRevalidate(any(),any());
    }
    static class Fixture {
        final SystemSignatureAuthenticationService authentication=mock(SystemSignatureAuthenticationService.class);
        final GxpAuditService ledger=mock(GxpAuditService.class);
        final ApprovalAuthorityApi authority=mock(ApprovalAuthorityApi.class);
        final DataSourceTransactionManager transactions;
        final JdbcTemplate db;
        final MesPqcProductionReleaseService proxy;
        Fixture(boolean success) {
            TenantContextHolder.setTenantId(71L);
            var ds=new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1","sa","");
            transactions=new DataSourceTransactionManager(ds);db=new JdbcTemplate(ds);
            db.execute("CREATE TABLE auth(attempts INT)");db.update("INSERT INTO auth VALUES(0)");
            var service=new MesPqcProductionReleaseServiceImpl(null,null,null,null,null,null,null,null,null,null,null,null);
            ReflectionTestUtils.setField(service,"signatureAuthentication",authentication);ReflectionTestUtils.setField(service,"transactionManager",transactions);
            ReflectionTestUtils.setField(service,"gxpAuditService",ledger);ReflectionTestUtils.setField(service,"approvalAuthority",authority);
            when(authentication.authenticate(any())).thenAnswer(c->{
                assertFalse(TransactionSynchronizationManager.isActualTransactionActive(),"Authentication must precede the release transaction");
                var independent=new TransactionTemplate(transactions);independent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                return independent.execute(tx->{db.update("UPDATE auth SET attempts=attempts+1");
                    return new SignatureAuthenticationOutcome(success?new AuthenticatedSigner(71L,"SYSTEM_USER",2L,2L,UUID.randomUUID().toString(),1L,"request"):null,success?null:"WRONG_PASSWORD");});
            });
            var factory=new ProxyFactory(service);factory.addAdvice(new TransactionInterceptor(transactions,new AnnotationTransactionAttributeSource()));
            proxy=(MesPqcProductionReleaseService)factory.getProxy();
        }
        MesPqcProductionReleaseApproveCommand command(){return new MesPqcProductionReleaseApproveCommand().setApplicationId(3L).setPqcReleaseWorkTaskId(4L)
                .setExpectedVersion(1).setSignaturePassword("test-credential").setIdempotencyKey("approve");}
    }
}
