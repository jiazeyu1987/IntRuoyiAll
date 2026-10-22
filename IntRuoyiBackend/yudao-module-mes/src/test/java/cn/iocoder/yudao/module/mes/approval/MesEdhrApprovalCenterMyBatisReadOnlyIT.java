package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.framework.tenant.config.TenantProperties;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.db.TenantDatabaseInterceptor;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.MesActiveOrderHandoffTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.handoff.MesActiveOrderHandoffTaskMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.logging.nologging.NoLoggingImpl;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.net.URI;
import java.sql.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Explicit opt-in only. Isolated Spring/MyBatis assembly, never application/Boot initialization. */
class MesEdhrApprovalCenterMyBatisReadOnlyIT {
    @Test
    void realSpringManagerAndTwoRealMapperStatementsShareReadOnlyRepeatableReadPhysicalConnection() throws Exception {
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive(),"isolated gate cannot join another transaction");
        assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty(),"thread must start without bound resources");
        assertNull(TenantContextHolder.getTenantId(),"thread must start without tenant context");
        DataSource source = localMasterDataSource();
        var diagnostic = new SafeDiagnostic();
        var probe = new StatementConnectionProbe(source,diagnostic);
        var factory = mapperFactory(source,probe);
        assertSame(source,factory.getConfiguration().getEnvironment().getDataSource());
        assertInstanceOf(SpringManagedTransactionFactory.class,factory.getConfiguration().getEnvironment().getTransactionFactory());
        var session = new SqlSessionTemplate(factory);
        var work = session.getMapper(MesProEdhrWorkTaskMapper.class);
        var handoff = session.getMapper(MesActiveOrderHandoffTaskMapper.class);
        var manager = new DataSourceTransactionManager(source);
        // Standard Spring manager default; physical/session read-only is independently asserted below.
        assertFalse(manager.isEnforceReadOnly(), "standard Spring manager default required");
        manager.setValidateExistingTransaction(true);
        assertSame(source,manager.getDataSource());
        var transaction = new TransactionTemplate(manager);
        transaction.setReadOnly(true);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        try {
            diagnostic.at(GateStage.TX_BEGIN,"TransactionTemplate.execute");
            transaction.execute(status -> {
                // No writes even on success. Rollback also closes the task-owned physical connection.
                status.setRollbackOnly();
                Connection db = DataSourceUtils.getConnection(source);
                try {
                    diagnostic.at(GateStage.PHYSICAL_VERIFY,"DatabaseMetaData.getDatabaseProductName");
                    assertEquals("MySQL",db.getMetaData().getDatabaseProductName(),"H2 cannot substitute for MySQL");
                    diagnostic.at(GateStage.PHYSICAL_VERIFY,"DatabaseMetaData.getDatabaseMajorVersion");
                    assertTrue(db.getMetaData().getDatabaseMajorVersion()>=8,"MySQL8 required");
                    probe.verify(db);
                    diagnostic.at(GateStage.SCHEMA_WORK,"information_schema.columns");
                    requireMappedSchema(db,TableInfoHelper.getTableInfo(MesProEdhrWorkTaskDO.class),diagnostic);
                    diagnostic.at(GateStage.SCHEMA_HANDOFF,"information_schema.columns");
                    requireMappedSchema(db,TableInfoHelper.getTableInfo(MesActiveOrderHandoffTaskDO.class),diagnostic);
                    diagnostic.at(GateStage.TENANT,"namedTenant.select");
                    long tenant = uniquePositiveId(db,"SELECT id FROM system_tenant WHERE deleted=0 AND status=0 AND CAST(name AS BINARY)=CAST(? AS BINARY)","NAMED_TENANT","芋道源码");
                    diagnostic.at(GateStage.ACTOR,"tenantAdmin.select");
                    long actor = uniquePositiveId(db,"SELECT id FROM system_users WHERE deleted=0 AND status=0 AND tenant_id=? AND CAST(username AS BINARY)=CAST(? AS BINARY)","TENANT_ADMIN",tenant,"admin");
                    TenantContextHolder.setTenantId(tenant);
                    for (boolean done : List.of(false,true)) for (boolean global : List.of(false,true)) {
                        int start = probe.executed.size();
                        probe.stage = (done?"DONE":"TODO")+"/"+(global?"global":"person")+"/work";
                        diagnostic.view = (done?"DONE":"TODO")+"/"+(global?"GLOBAL":"PERSONAL");
                        diagnostic.at(GateStage.WORK_MAPPER,"MesProEdhrWorkTaskMapper.selectApprovalCenterPrefix");
                        var workPage = work.selectApprovalCenterPrefix(new MesProEdhrWorkTaskPageReqVO(),global?null:actor,tenant,done,"TODO",400L);
                        assertNotNull(workPage.getTotal()); assertTrue(workPage.getTotal()>=workPage.getList().size());
                        assertTrue(workPage.getList().size()<=400);
                        probe.stage = (done?"DONE":"TODO")+"/"+(global?"global":"person")+"/handoff";
                        diagnostic.at(GateStage.HANDOFF_MAPPER,"MesActiveOrderHandoffTaskMapper.selectApprovalCenterPrefix");
                        var handoffPage = handoff.selectApprovalCenterPrefix(tenant,actor,global,done,null,400L);
                        assertNotNull(handoffPage.getTotal()); assertTrue(handoffPage.getTotal()>=handoffPage.getList().size());
                        assertTrue(handoffPage.getList().size()<=400);
                        // Both count/list pairs and H's corruption count must actually execute, including empty sets.
                        assertEquals(List.of("work/count","work/list","handoff/count","handoff/count","handoff/list"),
                                probe.executed.subList(start,probe.executed.size()).stream().map(ReadEvent::operation).toList(),
                                "real statement coverage required; no cached or mocked count/list");
                        assertEquals(probe.prepared.size(),probe.executed.size(),"every prepared mapper statement must execute");
                        System.out.println("MYSQL_SPRING_MYBATIS_CASE: view="+(done?"DONE":"TODO")+", mode="+(global?"global":"person")
                                +", work="+workPage.getTotal()+", H="+handoffPage.getTotal()+", actualStatements=5, K=400");
                    }
                    assertEquals(20,probe.executed.size());
                    assertEquals(1,probe.executed.stream().map(ReadEvent::connectionId).distinct().count());
                    probe.verify(db);
                } catch (SQLException failure) {
                    throw diagnostic.redacted(failure);
                } catch (RuntimeException | Error failure) {
                    diagnostic.capture(failure);
                    throw failure;
                } finally {
                    TenantContextHolder.clear();
                    diagnostic.at(GateStage.RELEASE,"DataSourceUtils.releaseConnection");
                    DataSourceUtils.releaseConnection(db,source);
                }
                diagnostic.at(GateStage.ROLLBACK,"DataSourceTransactionManager.rollback");
                return null;
            });
        } catch (RuntimeException | Error failure) {
            // MyBatis/Spring JDBC exceptions can embed SQL, payload and connection details.
            SQLException sql = sqlCause(failure);
            if (sql!=null) throw diagnostic.redacted(failure);
            if (failure instanceof AssertionError assertion) throw assertion;
            throw new IllegalStateException(diagnostic.capture(failure));
        } finally {
            TenantContextHolder.clear();
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
            assertFalse(TransactionSynchronizationManager.hasResource(source));
            assertFalse(TransactionSynchronizationManager.hasResource(factory));
            assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty(),"Spring resources must be released");
            if (probe.connection!=null) {
                diagnostic.at(GateStage.RELEASE_ASSERT,"Connection.isClosed");
                try { assertTrue(probe.connection.isClosed(),"task-owned physical connection must close after rollback"); }
                catch (SQLException failure) { throw diagnostic.redacted(failure); }
            }
        }
        System.out.println("MYSQL_SPRING_MYBATIS_SNAPSHOT: 20 real statements, one physical connection, readonly/RR, resources released; isolated assembly only, running backend datasource unverified");
    }

    private static SqlSessionFactory mapperFactory(DataSource source,StatementConnectionProbe probe) throws Exception {
        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setLogImpl(NoLoggingImpl.class);
        configuration.setCacheEnabled(false);
        configuration.setLocalCacheScope(LocalCacheScope.STATEMENT);
        var global = new GlobalConfig();
        var database = new GlobalConfig.DbConfig();
        database.setLogicDeleteValue("1"); database.setLogicNotDeleteValue("0");
        global.setDbConfig(database);
        var tenant = new TenantProperties();
        // Only these mapper tables are assembled; production handler and interceptor order are retained.
        // Production application.yaml ignores dcc_product_catalog, which is never queried by this gate.
        tenant.setIgnoreTables(Set.of("dcc_product_catalog"));
        var plugins = new MybatisPlusInterceptor();
        var tenantHandler=new TenantDatabaseInterceptor(tenant);
        plugins.addInnerInterceptor(new TenantLineInnerInterceptor(tenantHandler));
        plugins.addInnerInterceptor(new PaginationInnerInterceptor());
        var bean = new MybatisSqlSessionFactoryBean();
        bean.setDataSource(source); bean.setConfiguration(configuration); bean.setGlobalConfig(global);
        bean.setTransactionFactory(new SpringManagedTransactionFactory());
        bean.setPlugins(plugins,probe);
        SqlSessionFactory factory = Objects.requireNonNull(bean.getObject(),"MYSQL_GATE_SQL_SESSION_FACTORY_MISSING");
        factory.getConfiguration().addMapper(MesProEdhrWorkTaskMapper.class);
        factory.getConfiguration().addMapper(MesActiveOrderHandoffTaskMapper.class);
        // Production's mapper scan also registers metadata for the tables used by these subqueries.
        // Metadata only: no extra mapper proxies, entity reads, initialization or schema mutation.
        for (Class<?> related : List.of(MesProEdhrBatchExecutionDO.class,MesProEdhrBatchExecutionTaskDO.class,
                MesProEdhrReleaseTransactionDO.class,MesProEdhrNonconformanceReviewDO.class,
                MesProcessPoolActiveOrderDO.class,MesProcessPoolActiveOrderReleaseApplicationDO.class,AdminUserDO.class)) {
            var assistant=new MapperBuilderAssistant(factory.getConfiguration(),"readonly-related-metadata");
            assistant.setCurrentNamespace(related.getName());
            TableInfoHelper.initTableInfo(assistant,related);
        }
        assertFalse(tenantHandler.ignoreTable("mes_pro_edhr_work_task"),"production tenant handler must apply to work");
        assertFalse(tenantHandler.ignoreTable("mes_active_order_handoff_task"),"production tenant handler must apply to H");
        assertTrue(factory.getConfiguration().hasStatement(MesProEdhrWorkTaskMapper.class.getName()+".selectCount"));
        assertTrue(factory.getConfiguration().hasStatement(MesActiveOrderHandoffTaskMapper.class.getName()+".selectList"));
        return factory;
    }

    private record ReadEvent(String operation,long connectionId) { }

    @Intercepts({
            @Signature(type=StatementHandler.class,method="prepare",args={Connection.class,Integer.class}),
            @Signature(type=StatementHandler.class,method="query",args={Statement.class,ResultHandler.class})
    })
    private static final class StatementConnectionProbe implements Interceptor {
        private final DataSource source;
        private final List<ReadEvent> prepared = new ArrayList<>(), executed = new ArrayList<>();
        private String stage;
        private Long id;
        private Connection connection;
        private final SafeDiagnostic diagnostic;
        private StatementConnectionProbe(DataSource source,SafeDiagnostic diagnostic) {
            this.source=source; this.diagnostic=diagnostic;
        }
        @Override public Object intercept(Invocation call) throws Throwable {
            StatementHandler handler=(StatementHandler)call.getTarget();
            String sql=handler.getBoundSql().getSql().stripLeading();
            assertTrue(sql.regionMatches(true,0,"SELECT",0,6),"gate forbids non-SELECT mapper statements");
            assertFalse(sql.toUpperCase(Locale.ROOT).contains("FOR UPDATE"),"locking writer reads forbidden");
            assertNotNull(stage,"every mapper read must have an explicit query stage");
            String sourceName=stage.endsWith("/work")?"work":"handoff";
            String operation=sourceName+(sql.toUpperCase(Locale.ROOT).matches("(?s)^SELECT\\s+COUNT\\s*\\(.*")?"/count":"/list");
            diagnostic.statement = sourceName.toUpperCase(Locale.ROOT)+(operation.endsWith("/count")?"_COUNT":"_LIST");
            diagnostic.at(GateStage.STATEMENT_CONNECTION,call.getMethod().getName().equals("prepare")
                    ? "StatementHandler.prepare" : "Statement.getConnection");
            Connection actual=call.getMethod().getName().equals("prepare")?(Connection)call.getArgs()[0]:((Statement)call.getArgs()[0]).getConnection();
            long actualId=verify(actual);
            diagnostic.at(GateStage.EXECUTE_STATEMENT,call.getMethod().getName().equals("prepare")
                    ? "StatementHandler.prepare" : "StatementHandler.query");
            Object result=call.proceed();
            (call.getMethod().getName().equals("prepare")?prepared:executed).add(new ReadEvent(operation,actualId));
            return result;
        }
        private long verify(Connection actual) throws SQLException {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
            assertEquals(TransactionDefinition.ISOLATION_REPEATABLE_READ,TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());
            Object resource=TransactionSynchronizationManager.getResource(source);
            assertInstanceOf(ConnectionHolder.class,resource,"real Spring connection binding required");
            assertSame(((ConnectionHolder)resource).getConnection(),actual,"mapper must use manager-bound connection");
            diagnostic.at(GateStage.PHYSICAL_VERIFY,"Connection.isReadOnly");
            assertTrue(actual.isReadOnly());
            diagnostic.at(GateStage.PHYSICAL_VERIFY,"Connection.getAutoCommit");
            assertFalse(actual.getAutoCommit());
            diagnostic.at(GateStage.PHYSICAL_VERIFY,"Connection.getTransactionIsolation");
            assertEquals(Connection.TRANSACTION_REPEATABLE_READ,actual.getTransactionIsolation());
            diagnostic.at(GateStage.SESSION_VERIFY,"session.connectionIdIsolationReadOnly.select");
            try (PreparedStatement sql=actual.prepareStatement("SELECT CONNECTION_ID(),@@session.transaction_isolation,@@session.transaction_read_only"); ResultSet row=sql.executeQuery()) {
                assertTrue(row.next()); long actualId=row.getLong(1);
                assertEquals("REPEATABLE-READ",row.getString(2)); assertEquals(1,row.getInt(3));
                if (id==null) id=actualId; else assertEquals(id.longValue(),actualId,"all actual mapper statements must share physical connection");
                connection=actual;
                return actualId;
            }
        }
    }

    private static void requireMappedSchema(Connection db,TableInfo table,SafeDiagnostic diagnostic) throws SQLException {
        assertNotNull(table,"real mapper TableInfo required");
        var required=new HashSet<String>(); required.add(table.getKeyColumn());
        table.getFieldList().forEach(field -> required.add(field.getColumn().replace("`","")));
        // Work's explicit tenant predicate is necessary even if the existing DO omits a mapped tenant field.
        required.add("tenant_id");
        var actual=new HashSet<String>();
        try (PreparedStatement sql=db.prepareStatement("SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=?")) {
            sql.setString(1,table.getTableName());
            try (ResultSet rows=sql.executeQuery()) { while(rows.next()) actual.add(rows.getString(1)); }
        }
        for (String column:required) assertTrue(actual.contains(column),"MYSQL_GATE_MAPPED_SCHEMA_MISSING: "+table.getTableName()+"."+column);
        diagnostic.method = "information_schema.engine";
        try (PreparedStatement sql=db.prepareStatement("SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=?")) {
            sql.setString(1,table.getTableName());
            try (ResultSet row=sql.executeQuery()) { assertTrue(row.next()); assertEquals("InnoDB",row.getString(1)); }
        }
    }

    private static long uniquePositiveId(Connection db,String query,String relation,Object... values) throws SQLException {
        try (PreparedStatement sql=db.prepareStatement(query)) {
            for (int i=0;i<values.length;i++) sql.setObject(i+1,values[i]);
            try (ResultSet rows=sql.executeQuery()) {
                assertTrue(rows.next(),"MYSQL_GATE_"+relation+"_RELATION_MISSING");
                long id=rows.getLong(1); assertFalse(rows.wasNull()); assertTrue(id>0,"MYSQL_GATE_"+relation+"_IDENTITY_INVALID");
                assertFalse(rows.next(),"MYSQL_GATE_"+relation+"_RELATION_AMBIGUOUS");
                return id;
            }
        }
    }

    private static DataSource localMasterDataSource() {
        String url=required("EDHR_MYSQL_JDBC_URL"); URI endpoint;
        try { if (!url.startsWith("jdbc:mysql://")) throw new IllegalArgumentException(); endpoint=URI.create(url.substring(5)); }
        catch (RuntimeException invalid) { throw new IllegalStateException("MYSQL_GATE_INVALID_LOCAL_URL"); }
        assertTrue(Set.of("localhost","127.0.0.1","::1","[::1]").contains(endpoint.getHost()),"authorized local master required");
        assertNull(endpoint.getUserInfo(),"private credentials must stay out of URL");
        assertFalse(url.toLowerCase(Locale.ROOT).matches(".*[?&](user|password)=.*"),"private credentials must stay out of URL");
        return new PrivateMasterDataSource(url,required("EDHR_MYSQL_USER"),required("EDHR_MYSQL_PASSWORD"));
    }
    // DriverManagerDataSource debug logging includes the URL; keep this private connector free of it.
    private static final class PrivateMasterDataSource extends AbstractDataSource {
        private final String url,user,password;
        private PrivateMasterDataSource(String url,String user,String password) { this.url=url; this.user=user; this.password=password; }
        @Override public Connection getConnection() throws SQLException { return DriverManager.getConnection(url,user,password); }
        @Override public Connection getConnection(String username,String secret) { throw new UnsupportedOperationException("MYSQL_GATE_EXPLICIT_PRIVATE_MASTER_ONLY"); }
    }
    private static String required(String name) {
        String value=System.getenv(name);
        if (value==null || value.isBlank()) throw new IllegalStateException("MYSQL_READ_ONLY_GATE_PREREQUISITE_MISSING: "+name);
        return value;
    }
    private static SQLException sqlCause(Throwable failure) {
        var seen=Collections.newSetFromMap(new IdentityHashMap<Throwable,Boolean>());
        for (Throwable cause=failure;cause!=null && seen.add(cause);cause=cause.getCause()) if (cause instanceof SQLException sql) return sql;
        return null;
    }
    private enum GateStage {
        TX_BEGIN, PHYSICAL_VERIFY, SCHEMA_WORK, SCHEMA_HANDOFF, TENANT, ACTOR,
        WORK_MAPPER, HANDOFF_MAPPER, STATEMENT_CONNECTION, SESSION_VERIFY,
        EXECUTE_STATEMENT, RELEASE, ROLLBACK, RELEASE_ASSERT
    }
    private static final class SafeDiagnostic {
        private static final int MAX_CAUSES=6, MAX_FRAMES_PER_CAUSE=64;
        private GateStage stage = GateStage.TX_BEGIN;
        private String method = "TransactionTemplate.execute", statement = "NONE", view = "NONE", failureSummary;
        private void at(GateStage stage,String method) { this.stage=stage; this.method=method; }
        private String capture(Throwable failure) {
            if (failureSummary != null) return failureSummary;
            var causes=new ArrayList<Throwable>();
            var seen=Collections.newSetFromMap(new IdentityHashMap<Throwable,Boolean>());
            Throwable cause=failure;
            while (cause!=null && causes.size()<MAX_CAUSES && seen.add(cause)) {
                causes.add(cause);
                cause=cause.getCause();
            }
            boolean causesTruncated=cause!=null;
            SQLException sql=causes.stream().filter(SQLException.class::isInstance)
                    .map(SQLException.class::cast).findFirst().orElse(null);
            String state=sql==null ? "NONE" : sql.getSQLState();
            if (state==null || !state.matches("[A-Z0-9]{5}|NONE")) state="UNKNOWN";
            var frames=new ArrayList<StackTraceElement>();
            boolean framesTruncated=false;
            // Innermost operation first. Factory overloads do not identify the failed operation.
            for (int index=causes.size()-1;index>=0;index--) {
                StackTraceElement[] trace=causes.get(index).getStackTrace();
                framesTruncated |= trace.length>MAX_FRAMES_PER_CAUSE;
                for (int frameIndex=0;frameIndex<Math.min(trace.length,MAX_FRAMES_PER_CAUSE);frameIndex++) {
                    StackTraceElement frame=trace[frameIndex];
                    String owner=frame.getClassName();
                    if (owner.equals("com.mysql.cj.jdbc.exceptions.SQLError")
                            || owner.equals("com.mysql.cj.jdbc.exceptions.SQLExceptionsMapping")
                            || owner.equals("com.mysql.cj.exceptions.ExceptionFactory")) continue;
                    if (owner.startsWith("com.mysql.") || owner.startsWith("org.springframework.")
                            || owner.startsWith("org.apache.ibatis.") || owner.startsWith("cn.iocoder.")) frames.add(frame);
                }
            }
            var methods=new LinkedHashSet<String>();
            addAnchors(methods,frames,"com.mysql.",2);
            addAnchors(methods,frames,"org.springframework.",2);
            addAnchors(methods,frames,"org.apache.ibatis.",1);
            addAnchors(methods,frames,"cn.iocoder.",1);
            failureSummary="MYSQL_SPRING_MYBATIS_GATE_FAILURE: stage="+stage+", method="+method
                    +", setupStep="+setupStep(frames)
                    +", statement="+statement+", view="+view+", exception="+failure.getClass().getName()
                    +", sqlException="+(sql==null?"NONE":sql.getClass().getName())
                    +", state="+state+", code="+(sql==null?0:sql.getErrorCode())
                    +", causes="+causes.stream().map(item -> identifier(item.getClass().getName())).toList()
                    +", causesTruncated="+causesTruncated+", framesTruncated="+framesTruncated+", anchors="+methods;
            return failureSummary;
        }
        private static void addAnchors(Set<String> anchors,List<StackTraceElement> frames,String prefix,int limit) {
            var operations=new HashSet<String>();
            for (StackTraceElement frame:frames) {
                if (!frame.getClassName().startsWith(prefix)) continue;
                String operation=identifier(frame.getClassName()+"."+frame.getMethodName());
                if (operations.add(operation)) {
                    anchors.add(operation+":"+frame.getLineNumber());
                    if (operations.size()==limit) return;
                }
            }
        }
        private String setupStep(List<StackTraceElement> frames) {
            if (stage!=GateStage.TX_BEGIN) return "NONE";
            boolean enforcing=hasFrame(frames,"org.springframework.jdbc.datasource.DataSourceTransactionManager",
                    "prepareTransactionalConnection");
            boolean executing=hasFrame(frames,"com.mysql.cj.jdbc.StatementImpl","executeUpdateInternal");
            if (enforcing && executing) return "ENFORCE_READ_ONLY";
            for (StackTraceElement frame:frames) {
                if (!frame.getClassName().equals("com.mysql.cj.jdbc.ConnectionImpl")) continue;
                String step=switch (frame.getMethodName()) {
                    case "setReadOnly", "setReadOnlyInternal" -> "SET_READ_ONLY";
                    case "getTransactionIsolation" -> "GET_ISOLATION";
                    case "setTransactionIsolation" -> "SET_ISOLATION";
                    case "getAutoCommit" -> "GET_AUTOCOMMIT";
                    case "setAutoCommit" -> "SET_AUTOCOMMIT";
                    default -> "UNKNOWN";
                };
                if (!"UNKNOWN".equals(step)) return step;
            }
            if (hasFrame(frames,"com.mysql.cj.jdbc.NonRegisteringDriver","connect")
                    || hasFrame(frames,PrivateMasterDataSource.class.getName(),"getConnection")) return "ACQUIRE_CONNECTION";
            return "UNKNOWN";
        }
        private static boolean hasFrame(List<StackTraceElement> frames,String owner,String method) {
            return frames.stream().anyMatch(frame -> owner.equals(frame.getClassName()) && method.equals(frame.getMethodName()));
        }
        // Only code identifiers and line numbers: never Throwable/message/SQL/arguments/URL.
        private static String identifier(String value) { return value.replaceAll("[^A-Za-z0-9_.$]","_"); }
        private AssertionError redacted(Throwable failure) { return new AssertionError(capture(failure)); }
    }
}
