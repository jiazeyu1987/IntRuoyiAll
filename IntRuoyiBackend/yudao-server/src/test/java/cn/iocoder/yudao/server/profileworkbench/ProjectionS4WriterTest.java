package cn.iocoder.yudao.server.profileworkbench;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.transaction.Transaction;
import org.junit.jupiter.api.*;
import java.sql.Connection;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;

class ProjectionS4WriterTest {
    final Connection connection=mock(Connection.class);final Executor executor=mock(Executor.class);
    @BeforeEach void setup() throws Exception {
        Transaction transaction=mock(Transaction.class);when(transaction.getConnection()).thenReturn(connection);when(executor.getTransaction()).thenReturn(transaction);
        when(executor.update(any(),any())).thenReturn(1);TenantContextHolder.setTenantId(2L);
    }
    @AfterEach void clear(){ProjectionWriteContext.CURRENT.remove();TenantContextHolder.clear();}
    Object update(String namespace,String sql) throws Throwable {
        return execute(namespace,sql,SqlCommandType.UPDATE);
    }
    Object execute(String namespace,String sql,SqlCommandType type) throws Throwable {
        var statement=new MappedStatement.Builder(new Configuration(),namespace+".update",new StaticSqlSource(new Configuration(),sql),type).build();
        return new ProjectionWriterGuard().intercept(new Invocation(executor,Executor.class.getMethod("update",MappedStatement.class,Object.class),new Object[]{statement,new Object()}));
    }
    @Test void unboundWriteFailsBeforeMapperExecutes(){
        assertThrows(IllegalStateException.class,()->update("owner.MesProEdhrWorkTaskMapper","UPDATE mes_pro_edhr_work_task SET status='DONE'"));
        verifyNoInteractions(executor);
    }
    @Test void alternateMapperNamespaceCannotBypassFormalTableFence(){
        assertThrows(IllegalStateException.class,()->update("owner.CustomImportMapper","DELETE FROM system_users WHERE tenant_id=2"));
        verifyNoInteractions(executor);
    }
    @Test void sameTransactionMarksCompleteTaskAndSecurityEffects() throws Throwable {
        var held=new ProjectionWriteContext(Scope.TENANT,2,connection);ProjectionWriteContext.CURRENT.set(held);
        assertEquals(1,update("owner.CustomImportMapper","UPDATE system_role SET status=1 WHERE tenant_id=2"));
        assertTrue(held.changed);assertTrue(held.securityChanged);
    }
    @Test void globalWritesRequireGlobalFenceAndTenantCannotDrift(){
        ProjectionWriteContext.CURRENT.set(new ProjectionWriteContext(Scope.TENANT,2,connection));
        assertThrows(IllegalStateException.class,()->update("owner.MenuMapper","UPDATE system_menu SET permission='x'"));
        TenantContextHolder.setTenantId(3L);
        assertThrows(IllegalStateException.class,()->update("owner.RoleMapper","UPDATE system_role SET status=1"));
    }
    @Test void detachedTransactionIsRejectedBeforeBusinessMutation(){
        ProjectionWriteContext.CURRENT.set(new ProjectionWriteContext(Scope.TENANT,2,mock(Connection.class)));
        assertThrows(IllegalStateException.class,()->update("owner.RoleMapper","UPDATE system_role SET status=1"));
    }
    @Test void zeroRowUpdateDoesNotPretendACommitChangedFacts() throws Throwable {
        when(executor.update(any(),any())).thenReturn(0);var held=new ProjectionWriteContext(Scope.TENANT,2,connection);ProjectionWriteContext.CURRENT.set(held);
        assertEquals(0,update("owner.RoleMapper","UPDATE system_role SET status=1 WHERE id=9"));assertFalse(held.changed);
    }
    @Test void readingGlobalMenuInsideTenantMutationDoesNotRequireGlobalFence() throws Throwable {
        var held=new ProjectionWriteContext(Scope.TENANT,2,connection);ProjectionWriteContext.CURRENT.set(held);
        assertEquals(1,update("owner.CustomImportMapper","UPDATE system_role SET status=1 WHERE EXISTS(SELECT 1 FROM system_menu WHERE permission='x')"));
        assertTrue(held.changed);assertTrue(held.securityChanged);
    }
    @Test void joinedAliasMutationOfGlobalTableCannotPassTenantFence(){
        var held=new ProjectionWriteContext(Scope.TENANT,2,connection);ProjectionWriteContext.CURRENT.set(held);
        assertThrows(IllegalStateException.class,()->update("owner.CustomImportMapper","UPDATE system_role r JOIN system_menu m ON m.id=r.id SET m.permission='x'"));
        assertTrue(held.failed);verifyNoInteractions(executor);
    }
    @Test void unqualifiedJoinedGlobalMutationFailsBeforeMapperExecutes(){
        var held=new ProjectionWriteContext(Scope.TENANT,2,connection);ProjectionWriteContext.CURRENT.set(held);
        assertThrows(IllegalStateException.class,()->update("owner.CustomImportMapper","UPDATE system_role r JOIN system_menu m ON m.id=r.id SET permission='x'"));
        assertTrue(held.failed);verifyNoInteractions(executor);
    }
    @Test void plainNoticeInsertRetainsIndependentDeliveryButUpdatesAndUpsertsAreGuarded() throws Throwable {
        assertEquals(1,execute("owner.NotifyMessageMapper","INSERT INTO system_notify_message(id,content) VALUES(1,'new')",SqlCommandType.INSERT));
        assertThrows(IllegalStateException.class,()->update("owner.NotifyMessageMapper","UPDATE system_notify_message SET content='changed' WHERE id=1"));
        assertThrows(IllegalStateException.class,()->execute("owner.NotifyMessageMapper","INSERT INTO system_notify_message(id,content) VALUES(1,'changed') ON DUPLICATE KEY UPDATE content='changed'",SqlCommandType.INSERT));
        verify(executor,times(1)).update(any(),any());
    }

    @Test void handoffCompletionAndCycleClosureRequireFenceAndMarkRefresh() throws Throwable {
        for(String table:new String[]{"mes_active_order_handoff_task","mes_pro_process_pool_active_order"}) {
            assertThrows(IllegalStateException.class,()->update("owner.CustomImportMapper","UPDATE "+table+" SET status='DONE'"));
            var held=new ProjectionWriteContext(Scope.TENANT,2,connection);ProjectionWriteContext.CURRENT.set(held);
            assertEquals(1,update("owner.CustomImportMapper","UPDATE "+table+" SET status='DONE'"));
            assertTrue(held.changed);assertFalse(held.securityChanged);ProjectionWriteContext.CURRENT.remove();
        }
    }
}
