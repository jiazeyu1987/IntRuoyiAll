package cn.iocoder.yudao.module.system.service.gxpaudit;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GxpAuditDevelopmentModeTest {
    @AfterEach void restoreFormalMode() { new GxpAuditDevelopmentMode(new MockEnvironment(), cn.iocoder.yudao.module.infra.enums.config.GxpAuditMode.FORMAL); }
    static MockEnvironment local() {
        var env = new MockEnvironment().withProperty("spring.datasource.dynamic.datasource.master.url", "jdbc:mysql://127.0.0.1:23306/test");
        env.setActiveProfiles("local"); return env;
    }
    @ParameterizedTest @ValueSource(strings={"prod", "dev", "local,prod", ""})
    void cannotDisableOutsideExclusiveLocal(String profiles) {
        var env=local(); env.setActiveProfiles(profiles.isEmpty()?new String[]{}:profiles.split(","));
        assertThrows(IllegalStateException.class,()->new GxpAuditDevelopmentMode(env, cn.iocoder.yudao.module.infra.enums.config.GxpAuditMode.DEVELOPMENT_DISABLED));
    }
    @Test void cannotDisableWithRemoteMasterOrSlave() {
        var remoteMaster=local().withProperty("spring.datasource.dynamic.datasource.master.url","jdbc:mysql://remote/db");
        assertThrows(IllegalStateException.class,()->new GxpAuditDevelopmentMode(remoteMaster, cn.iocoder.yudao.module.infra.enums.config.GxpAuditMode.DEVELOPMENT_DISABLED));
        var env=local().withProperty("spring.datasource.dynamic.datasource.slave.url","jdbc:mysql://remote/db");
        var remoteSlave=env;
        assertThrows(IllegalStateException.class,()->new GxpAuditDevelopmentMode(remoteSlave, cn.iocoder.yudao.module.infra.enums.config.GxpAuditMode.DEVELOPMENT_DISABLED));
    }
    @Test void defaultRemainsFormalAndDisabledStatusIsNeverAcceptedInFormalMode() {
        new GxpAuditDevelopmentMode(new MockEnvironment(), cn.iocoder.yudao.module.infra.enums.config.GxpAuditMode.FORMAL);
        assertFalse(GxpAuditDevelopmentMode.disabled());
        assertFalse(GxpAuditDevelopmentMode.auditBindingAccepted("DEVELOPMENT_DISABLED"));
    }
    @Test void isolatedCheckpointRequiresMatchingSpaceAndBothDatasourceHosts() {
        var env=local().withProperty("INTRUOYI_CHECKPOINT_SPACE", "checkpoint-c6")
                .withProperty("intruoyi.checkpoint.space-id", "checkpoint-c6")
                .withProperty("spring.datasource.dynamic.datasource.master.url",
                        "jdbc:mysql://checkpoint-c6-mysql-1:3306/ruoyi-vue-pro?useSSL=false");
        new GxpAuditDevelopmentMode(env, cn.iocoder.yudao.module.infra.enums.config.GxpAuditMode.DEVELOPMENT_DISABLED);
        assertTrue(GxpAuditDevelopmentMode.disabled());
        env.withProperty("spring.datasource.dynamic.datasource.slave.url",
                "jdbc:mysql://checkpoint-other-mysql-1:3306/ruoyi-vue-pro");
        assertThrows(IllegalStateException.class, () -> new GxpAuditDevelopmentMode(env, cn.iocoder.yudao.module.infra.enums.config.GxpAuditMode.DEVELOPMENT_DISABLED));
        env.withProperty("spring.datasource.dynamic.datasource.slave.url",
                "jdbc:mysql://checkpoint-c6-mysql-1:3306/ruoyi-vue-pro")
                .withProperty("intruoyi.checkpoint.space-id", "checkpoint-other");
        assertThrows(IllegalStateException.class, () -> new GxpAuditDevelopmentMode(env, cn.iocoder.yudao.module.infra.enums.config.GxpAuditMode.DEVELOPMENT_DISABLED));
    }
    @Test void disabledWriterAndQueriesNeverTouchAuditPersistence() {
        new GxpAuditDevelopmentMode(local(), cn.iocoder.yudao.module.infra.enums.config.GxpAuditMode.DEVELOPMENT_DISABLED);
        var mapper=mock(GxpAuditEventMapper.class); var relation=mock(GxpAuditEventRelationMapper.class);
        var ledger=mock(GxpAuditLedgerSequenceMapper.class);
        var writer=new GxpAuditServiceImpl();
        ReflectionTestUtils.setField(writer,"auditEventMapper",mapper);
        ReflectionTestUtils.setField(writer,"ledgerSequenceMapper",ledger);
        writer.acquireLedgerLock(); writer.assertLedgerLockHeld();
        var result=writer.append(GxpAuditCommand.builder().operationId("test").build());
        assertEquals("DEVELOPMENT_DISABLED",result.auditStatus()); assertTrue(result.eventId()<0);
        assertNull(result.ledgerSequence()); assertEquals("DEVELOPMENT_DISABLED",result.eventHash());
        var query=new GxpAuditQueryServiceImpl();
        ReflectionTestUtils.setField(query,"auditEventMapper",mapper); ReflectionTestUtils.setField(query,"relationMapper",relation);
        assertEquals(0L,query.page(new GxpAuditEventPageQuery()).getTotal());
        assertNull(query.get(1L,1L)); assertTrue(query.listRelations(1L).isEmpty());
        verifyNoInteractions(mapper,relation,ledger);
    }
    @Test void sqlGuardBlocksForgottenAuditQueriesButAllowsBusinessQueries() throws Throwable {
        new GxpAuditDevelopmentMode(local(), cn.iocoder.yudao.module.infra.enums.config.GxpAuditMode.DEVELOPMENT_DISABLED);
        var executor=mock(org.apache.ibatis.executor.Executor.class);
        var configuration=new org.apache.ibatis.session.Configuration();
        org.apache.ibatis.mapping.SqlSource sql=p->new org.apache.ibatis.mapping.BoundSql(configuration,"SELECT 1",java.util.List.of(),p);
        var audit=new org.apache.ibatis.mapping.MappedStatement.Builder(configuration,"test.dal.mysql.gxpaudit.GxpAuditEventMapper.selectList",sql,org.apache.ibatis.mapping.SqlCommandType.SELECT).build();
        var method=org.apache.ibatis.executor.Executor.class.getMethod("query",org.apache.ibatis.mapping.MappedStatement.class,Object.class,org.apache.ibatis.session.RowBounds.class,org.apache.ibatis.session.ResultHandler.class);
        var guard=new GxpAuditDevelopmentSqlGuard();
        var blocked=new org.apache.ibatis.plugin.Invocation(executor,method,new Object[]{audit,null,org.apache.ibatis.session.RowBounds.DEFAULT,null});
        assertThrows(IllegalStateException.class,()->guard.intercept(blocked));
        verifyNoInteractions(executor);
        var business=new org.apache.ibatis.mapping.MappedStatement.Builder(configuration,"test.BusinessMapper.selectList",sql,org.apache.ibatis.mapping.SqlCommandType.SELECT).build();
        var allowed=new org.apache.ibatis.plugin.Invocation(executor,method,new Object[]{business,null,org.apache.ibatis.session.RowBounds.DEFAULT,null});
        guard.intercept(allowed);
        verify(executor).query(business,null,org.apache.ibatis.session.RowBounds.DEFAULT,null);
    }

    @Test void reservedDevelopmentMarkerIsPreservedWithoutCreatingAnAuditEvent() {
        new GxpAuditDevelopmentMode(local(), cn.iocoder.yudao.module.infra.enums.config.GxpAuditMode.DEVELOPMENT_DISABLED); var writer=new GxpAuditServiceImpl();
        Long id=writer.reserveEventId("op","subject","source","key");
        assertTrue(id<0); assertEquals(id,writer.append(GxpAuditCommand.builder().reservedEventId(id).build()).eventId());
    }
}
