package cn.iocoder.yudao.server.profileworkbench;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.workbench.index.WorkbenchWriteBoundary;
import cn.iocoder.yudao.module.system.service.profileworkbench.index.ProjectionRepository;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import org.junit.jupiter.api.*;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.*;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectionS4BoundaryTest {
    public static class BusinessService {
        @WorkbenchWriteBoundary public void write(Runnable action){action.run();}
        @WorkbenchWriteBoundary(Scope.GLOBAL) public void global(Runnable action){action.run();}
    }
    final ProjectionRepository repository=mock(ProjectionRepository.class);
    final ProjectionExportRegistry exports=mock(ProjectionExportRegistry.class);
    final Connection connection=mock(Connection.class);
    final DataSource dataSource=mock(DataSource.class);
    BusinessService service;
    final Global global=new Global(0,0,"r");
    final Tenant tenant=new Tenant(2,0,0,null);
    final FrozenExport before=new FrozenExport(2,new Watermark(0,0),"r",Set.of("WORK_ORDER"),List.of(),List.of());
    final FrozenExport after=new FrozenExport(2,new Watermark(0,1),"r",Set.of("WORK_ORDER"),List.of(),List.of());
    @BeforeEach @SuppressWarnings("unchecked") void setup() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);when(connection.getAutoCommit()).thenReturn(true);
        when(connection.getTransactionIsolation()).thenReturn(Connection.TRANSACTION_READ_COMMITTED);
        when(repository.lockGlobal(anyBoolean())).thenReturn(global);when(repository.lockTenant(2,true)).thenReturn(tenant);
        when(exports.export(2,global,tenant)).thenReturn(before);
        when(exports.export(2,global,new Tenant(2,1,0,null))).thenReturn(after);
        when(exports.changes(before,after)).thenReturn(List.of());
        ObjectProvider<ProjectionExportRegistry> provider=mock(ObjectProvider.class);when(provider.getObject()).thenReturn(exports);
        AspectJProxyFactory factory=new AspectJProxyFactory(new BusinessService());
        factory.addAspect(new ProjectionWriteBoundaryAspect(repository,provider,new DataSourceTransactionManager(dataSource),dataSource));
        assertTrue(BusinessService.class.getMethod("write",Runnable.class).isAnnotationPresent(WorkbenchWriteBoundary.class));
        var pointcut=new org.springframework.aop.aspectj.AspectJExpressionPointcut();
        pointcut.setExpression("@annotation(cn.iocoder.yudao.module.system.api.workbench.index.WorkbenchWriteBoundary)");
        assertTrue(pointcut.matches(BusinessService.class.getMethod("write",Runnable.class),BusinessService.class),"Annotation pointcut alone failed");
        pointcut=new org.springframework.aop.aspectj.AspectJExpressionPointcut();
        pointcut.setExpression(ProjectionWriteBoundaryAspect.class.getMethod("around",org.aspectj.lang.ProceedingJoinPoint.class).getAnnotation(org.aspectj.lang.annotation.Around.class).value());
        assertTrue(pointcut.matches(BusinessService.class.getMethod("write",Runnable.class),BusinessService.class),"Actual write pointcut does not match annotated root");
        assertTrue(factory.getAdvisors().length>0,"Actual aspect has no eligible advisors");
        service=factory.getProxy();TenantContextHolder.setTenantId(2L);
    }
    @AfterEach void clear(){ProjectionWriteContext.CURRENT.remove();TenantContextHolder.clear();DynamicDataSourceContextHolder.clear();}
    @Test void actualAdviceFencesBeforeBusinessAndCommitsCompleteBatchAfterIt() throws Exception {
        Runnable business=mock(Runnable.class);
        doAnswer(i->{assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            assertEquals("master",DynamicDataSourceContextHolder.peek());assertSame(connection,ProjectionWriteContext.CURRENT.get().connection);
            ProjectionWriteContext.CURRENT.get().changed=true;return null;}).when(business).run();
        DynamicDataSourceContextHolder.push("caller");service.write(business);
        var order=inOrder(repository,exports,business,connection);
        order.verify(repository).lockGlobal(false);order.verify(repository).lockTenant(2,true);
        order.verify(exports).export(2,global,tenant);order.verify(business).run();
        order.verify(exports).export(2,global,new Tenant(2,1,0,null));
        order.verify(repository).append(argThat(b->b.events().size()==1&&b.events().get(0).kind()==EventKind.CONTROL&&b.sequence()==1));
        order.verify(repository).advanceTenant(tenant,new Tenant(2,1,0,null));order.verify(connection).commit();
        assertEquals("caller",DynamicDataSourceContextHolder.peek());assertNull(ProjectionWriteContext.CURRENT.get());
    }
    @Test void noFormalChangeProducesNoOutboxOrWatermark() throws Exception {
        service.write(()->{});verify(repository,never()).append(any());verify(repository,never()).advanceTenant(any(),any());verify(connection).commit();
    }
    @Test void businessFailureRollsBackAndPreservesOriginalException() throws Exception {
        IllegalArgumentException failure=new IllegalArgumentException("formal failure");
        assertSame(failure,assertThrows(IllegalArgumentException.class,()->service.write(()->{ProjectionWriteContext.CURRENT.get().changed=true;throw failure;})));
        verify(connection).rollback();verify(repository,never()).append(any());assertNull(ProjectionWriteContext.CURRENT.get());
    }
    @Test void caughtWriteFailureCannotCommitPartialFormalState() throws Exception {
        assertThrows(IllegalStateException.class,()->service.write(()->{ProjectionWriteContext.CURRENT.get().changed=true;ProjectionWriteContext.CURRENT.get().failed=true;}));
        verify(connection).rollback();verify(repository,never()).append(any());
    }
    @Test void globalAnnotationRequiresExclusiveGlobalFenceAndAdvancesSecurity() throws Exception {
        service.global(()->ProjectionWriteContext.CURRENT.get().changed=true);
        verify(repository).lockGlobal(true);verify(repository,never()).lockTenant(anyLong(),anyBoolean());verifyNoInteractions(exports);
        verify(repository).append(argThat(b->b.scope()==Scope.GLOBAL&&b.tenantId()==0&&b.events().get(0).kind()==EventKind.CONTROL));
        verify(repository).advanceGlobal(global,new Global(1,1,"r"));verify(connection).commit();
    }
    @Test void swallowedNestedGlobalViolationStillRollsBackTenantRoot() throws Exception {
        assertThrows(IllegalStateException.class,()->service.write(()->{try {service.global(()->{});}catch(IllegalStateException expected){};}));
        verify(connection).rollback();verify(repository,never()).append(any());
    }
    @Test void outboxFailureRollsBackFormalBusinessAndNeverAdvancesHead() throws Exception {
        doThrow(new IllegalStateException("outbox failed")).when(repository).append(any());
        assertThrows(IllegalStateException.class,()->service.write(()->ProjectionWriteContext.CURRENT.get().changed=true));
        verify(connection).rollback();verify(repository,never()).advanceTenant(any(),any());
    }
    private <T> T proxy(T target,boolean ordinaryTransactions) {
        ObjectProvider<ProjectionExportRegistry> provider=mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(exports);
        var manager=new DataSourceTransactionManager(dataSource);
        AspectJProxyFactory factory=new AspectJProxyFactory(target);
        factory.setProxyTargetClass(true);
        // AspectJProxyFactory preserves insertion order for programmatically added
        // advice. Spring auto-proxying orders the production aspect (-1000) before
        // ordinary @Transactional advice; assemble this fixture in that same order.
        factory.addAspect(new ProjectionWriteBoundaryAspect(repository,provider,manager,dataSource));
        if(ordinaryTransactions) factory.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(
            manager,new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));
        return factory.getProxy();
    }
    @Test void actualPasswordResetIsFencedBeforeItsOrdinaryTransactionAndNestedUserWrite() throws Exception {
        var user=mock(cn.iocoder.yudao.module.system.service.user.AdminUserServiceImpl.class);
        var account=new cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO().setId(7L);
        when(user.getUserByMobile("test-mobile")).thenReturn(account);
        doAnswer(i->{assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            assertSame(connection,ProjectionWriteContext.CURRENT.get().connection);
            ProjectionWriteContext.CURRENT.get().changed=true;return null;}).when(user).updateUserPassword(7L,"test-password");
        var target=new cn.iocoder.yudao.module.system.service.auth.AdminAuthServiceImpl();
        var field=target.getClass().getDeclaredField("userService");field.setAccessible(true);field.set(target,proxy(user,false));
        field=target.getClass().getDeclaredField("smsCodeApi");field.setAccessible(true);
        field.set(target,mock(cn.iocoder.yudao.module.system.api.sms.SmsCodeApi.class));
        cn.iocoder.yudao.module.system.service.auth.AdminAuthService auth=proxy(target,true);
        var request=new cn.iocoder.yudao.module.system.controller.admin.auth.vo.AuthResetPasswordReqVO();
        request.setMobile("test-mobile");request.setCode("test-code");request.setPassword("test-password");
        auth.resetPassword(request);
        var order=inOrder(repository,user,connection);
        order.verify(repository).lockGlobal(false);order.verify(repository).lockTenant(2,true);
        order.verify(user).getUserByMobile("test-mobile");order.verify(user).updateUserPassword(7L,"test-password");
        order.verify(repository).append(any());order.verify(repository).advanceTenant(any(),any());order.verify(connection).commit();
    }
    @Test void actualSessionLockKeepsItsRequiredExistingTransactionWithoutStartingAProjectionRoot() throws Exception {
        var target=new cn.iocoder.yudao.module.system.service.user.AdminUserServiceImpl();
        var mapper=mock(cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper.class);
        var account=new cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO().setId(7L);
        when(mapper.selectPermissionSubjectForUpdate(2L,7L)).thenReturn(account);
        when(mapper.selectById(7L)).thenReturn(account);
        var field=target.getClass().getDeclaredField("userMapper");field.setAccessible(true);field.set(target,mapper);
        cn.iocoder.yudao.module.system.service.user.AdminUserService user=proxy(target,false);
        new org.springframework.transaction.support.TransactionTemplate(new DataSourceTransactionManager(dataSource))
            .executeWithoutResult(status->{assertSame(account,user.lockUserForSessionMutation(2L,7L));
                assertSame(account,user.getUser(7L));});
        verifyNoInteractions(repository,exports);verify(connection).commit();
    }
    @Test void tokenIssuanceTransactionRootsMatchButRefreshAndRevocationKeepTheirExistingTenantProtocol() throws Exception {
        var pointcut=new org.springframework.aop.aspectj.AspectJExpressionPointcut();
        pointcut.setExpression(ProjectionWriteBoundaryAspect.class.getMethod("around",org.aspectj.lang.ProceedingJoinPoint.class)
            .getAnnotation(org.aspectj.lang.annotation.Around.class).value());
        var owner=cn.iocoder.yudao.module.system.service.oauth2.OAuth2TokenServiceImpl.class;
        int matched=0;
        for(var method:owner.getDeclaredMethods()) {
            if(Set.of("createAccessToken","createPasswordAccessToken","createMobileAccessToken","createAdminAccessToken").contains(method.getName())) {
                assertTrue(pointcut.matches(method,owner),method.getName());matched++;
            } else if(Set.of("refreshAccessToken","refreshAdminAccessToken","removeAccessToken","getAccessToken","checkAccessToken").contains(method.getName()))
                assertFalse(pointcut.matches(method,owner),method.getName());
        }
        assertEquals(4,matched);
    }
    @Test void approvalAndElectronicSignatureTransactionRootsAreFenced() throws Exception {
        var pointcut=new org.springframework.aop.aspectj.AspectJExpressionPointcut();
        pointcut.setExpression(ProjectionWriteBoundaryAspect.class.getMethod("around",org.aspectj.lang.ProceedingJoinPoint.class)
            .getAnnotation(org.aspectj.lang.annotation.Around.class).value());
        int matched=0;
        for(var owner:List.of(cn.iocoder.yudao.module.bpm.approval.service.ApprovalCenterServiceImpl.class,
            cn.iocoder.yudao.module.signature.service.ElectronicSignatureServiceImpl.class)) {
            for(var method:owner.getDeclaredMethods()) if(Set.of("reviewTask","sign","signAuthorized").contains(method.getName())) {
                assertTrue(pointcut.matches(method,owner),owner.getSimpleName()+"#"+method.getName());matched++;
            }
        }
        assertEquals(3,matched);
    }
    @Test void actualNativeConfigImportPublishesGlobalControlWithoutMapperChange() throws Exception {
        var target=mock(cn.iocoder.yudao.module.system.service.configpackage.SystemConfigPackageServiceImpl.class);
        doAnswer(i->{assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            assertSame(connection,ProjectionWriteContext.CURRENT.get().connection);
            assertEquals(Scope.GLOBAL,ProjectionWriteContext.CURRENT.get().scope);
            assertFalse(ProjectionWriteContext.CURRENT.get().changed);return null;})
            .when(target).importPackage(any(byte[].class),eq(true),eq("snapshot"),anyCollection());
        cn.iocoder.yudao.module.system.service.configpackage.SystemConfigPackageService imported=proxy(target,true);
        imported.importPackage(new byte[]{1},true,"snapshot",List.of());
        verify(repository).lockGlobal(true);verify(repository,never()).lockTenant(anyLong(),anyBoolean());
        verify(repository).append(argThat(b->b.scope()==Scope.GLOBAL&&b.events().size()==1&&b.sequence()==1));
        verify(repository).advanceGlobal(global,new Global(1,1,"r"));verify(connection).commit();
    }
    @Test void actualSourceAdapterDescriptorRemainsAReadWithoutProjectionRoot() throws Exception {
        ObjectProvider<ProjectionExportRegistry> provider=mock(ObjectProvider.class);
        AspectJProxyFactory factory=new AspectJProxyFactory(new cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrWorkbenchSource());
        factory.addAspect(new ProjectionWriteBoundaryAspect(repository,provider,new DataSourceTransactionManager(dataSource),dataSource));
        cn.iocoder.yudao.module.system.api.workbench.SourceAdapter source=factory.getProxy();
        assertEquals("EDHR_WORK_TASK",source.descriptor().sourceId());verifyNoInteractions(repository,exports,connection,provider);
    }
}
