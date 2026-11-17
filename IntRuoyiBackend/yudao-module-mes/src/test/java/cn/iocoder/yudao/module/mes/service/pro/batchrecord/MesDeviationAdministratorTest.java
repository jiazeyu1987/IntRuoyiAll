package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesDeviationAdministratorMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeviationDO;
import com.alibaba.fastjson.JSON;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.sql.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MesDeviationAdministratorTest {
    private Connection connection;
    private SqlSession session;
    private MesDeviationAdministratorMapper mapper;

    public static Boolean jsonContains(String json, String candidate, String path) {
        return json != null && JSON.parseArray(json).contains(JSON.parse(candidate));
    }

    @BeforeEach
    void setUp() throws Exception {
        var ds = new UnpooledDataSource("org.h2.Driver", "jdbc:h2:mem:admin_"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        connection = ds.getConnection();
        try (var sql=connection.createStatement()) {
            sql.execute("CREATE ALIAS JSON_CONTAINS FOR 'cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesDeviationAdministratorTest.jsonContains'");
            sql.execute("CREATE TABLE system_users(id BIGINT PRIMARY KEY,tenant_id BIGINT,username VARCHAR,nickname VARCHAR,status INT,deleted INT)");
            sql.execute("CREATE TABLE system_role(id BIGINT PRIMARY KEY,tenant_id BIGINT,code VARCHAR,status INT,deleted INT)");
            sql.execute("CREATE TABLE system_user_role(user_id BIGINT,role_id BIGINT,tenant_id BIGINT,deleted INT)");
            sql.execute("CREATE TABLE system_menu(id BIGINT PRIMARY KEY,permission VARCHAR,status INT,deleted INT)");
            sql.execute("CREATE TABLE system_role_menu(role_id BIGINT,menu_id BIGINT,tenant_id BIGINT,deleted INT)");
            sql.execute("CREATE TABLE system_tenant(id BIGINT PRIMARY KEY,package_id BIGINT,status INT,deleted INT,expire_time TIMESTAMP)");
            sql.execute("CREATE TABLE system_tenant_package(id BIGINT PRIMARY KEY,menu_ids VARCHAR,status INT,deleted INT)");
            sql.execute("INSERT INTO system_tenant VALUES(71,9,0,0,DATEADD('DAY',1,CURRENT_TIMESTAMP)),(72,0,0,0,DATEADD('DAY',1,CURRENT_TIMESTAMP))");
            sql.execute("INSERT INTO system_tenant_package VALUES(9,'[1,2,3]',0,0)");
            sql.execute("INSERT INTO system_users VALUES(1,71,'admin1','管理员甲',0,0),(2,71,'normal','普通用户',0,0),(3,72,'foreign','其他租户',0,0)");
            sql.execute("INSERT INTO system_role VALUES(10,71,'deviation_admin_permission',0,0),(20,71,'super_admin',0,0),(30,72,'deviation_admin_permission',0,0)");
            sql.execute("INSERT INTO system_user_role VALUES(1,10,71,0),(2,20,71,0),(3,30,72,0)");
            sql.execute("INSERT INTO system_menu VALUES(1,'mes:pro-edhr-deviation:query',0,0),(2,'mes:pro-edhr-deviation:handle',0,0),(3,'mes:pro-edhr-work-task:query',0,0)");
            sql.execute("INSERT INTO system_role_menu VALUES(10,1,71,0),(10,2,71,0),(10,3,71,0),(30,1,72,0),(30,2,72,0),(30,3,72,0)");
        }
        var config=new Configuration(new Environment("test",new JdbcTransactionFactory(),ds));
        config.addMapper(MesDeviationAdministratorMapper.class);
        session=new SqlSessionFactoryBuilder().build(config).openSession();
        mapper=session.getMapper(MesDeviationAdministratorMapper.class);
    }
    @AfterEach void close() throws Exception { session.close(); connection.close(); TenantContextHolder.clear(); }
    private void execute(String sql) throws Exception { try(var statement=connection.createStatement()) {statement.execute(sql);} session.clearCache(); }
    @Test void exactTenantRoleAndNoSuperAdministratorBypass() {
        assertEquals(java.util.List.of(1L),mapper.selectCandidates(71L).stream().map(row->row.getId()).toList());
        assertNull(mapper.selectEligibleForUpdate(71L,2L));
        assertNull(mapper.selectEligibleForUpdate(71L,3L));
        assertEquals(3L,mapper.selectEligibleForUpdate(72L,3L).getId());
    }
    @Test void liveUserAndRoleDisableOrMembershipRevocationExcludesCandidate() throws Exception {
        for(String sql:java.util.List.of("UPDATE system_users SET status=1 WHERE id=1", "UPDATE system_role SET status=1 WHERE id=10", "UPDATE system_role SET deleted=1 WHERE id=10", "UPDATE system_user_role SET deleted=1 WHERE user_id=1")) {
            execute(sql); assertNull(mapper.selectEligibleForUpdate(71L,1L)); assertTrue(mapper.selectCandidates(71L).isEmpty());
            execute("UPDATE system_users SET status=0; UPDATE system_role SET status=0,deleted=0; UPDATE system_user_role SET deleted=0");
        }
    }
    @Test void missingDisabledOrRevokedPermissionRejectsEvenWithRole() throws Exception {
        execute("UPDATE system_role_menu SET deleted=1 WHERE role_id=10 AND menu_id=2"); assertNull(mapper.selectEligibleForUpdate(71L,1L));
        execute("UPDATE system_role_menu SET deleted=0; UPDATE system_menu SET status=1 WHERE id=3"); assertTrue(mapper.selectCandidates(71L).isEmpty());
    }
    @Test void packageRevocationAndTenantExpiryRejectAuthoritatively() throws Exception {
        execute("UPDATE system_tenant_package SET menu_ids='[1,3]' WHERE id=9"); assertNull(mapper.selectEligibleForUpdate(71L,1L));
        execute("UPDATE system_tenant_package SET menu_ids='[1,2,3]'; UPDATE system_tenant SET expire_time=DATEADD('DAY',-1,CURRENT_TIMESTAMP) WHERE id=71");
        assertTrue(mapper.selectCandidates(71L).isEmpty());
    }
    @Test void businessCandidatePoolExplicitlyDisablesDepartmentScopeButNotTenantScope() {
        var annotation=MesDeviationAdministratorService.class.getAnnotation(cn.iocoder.yudao.framework.datapermission.core.annotation.DataPermission.class);
        assertNotNull(annotation); assertFalse(annotation.enable());
        assertTrue(MesDeviationAdministratorMapper.ELIGIBLE.contains("u.tenant_id=#{tenantId}"));
    }
    @Test void systemPackageUsesCurrentJavaConstantAndDeduplicatesCandidateDisplay() throws Exception {
        execute("UPDATE system_tenant SET package_id="+cn.iocoder.yudao.module.system.dal.dataobject.tenant.TenantDO.PACKAGE_ID_SYSTEM+" WHERE id=71");
        execute("UPDATE system_tenant_package SET status=1; INSERT INTO system_user_role VALUES(1,10,71,0)");
        assertEquals(java.util.List.of(1L), mapper.selectCandidates(71L).stream().map(row->row.getId()).toList());
    }
    @Test void requireReceiverAndReceiveDispatchRejectBeforeCreatingTask() {
        TenantContextHolder.setTenantId(71L);
        var service=new MesDeviationAdministratorService(); ReflectionTestUtils.setField(service,"administratorMapper",mapper);
        assertThrows(ServiceException.class,()->service.requireReceiver(null));
        assertThrows(ServiceException.class,()->service.requireReceiver(2L));
        assertEquals(1L,service.requireReceiver(1L).getId());
        var tasks=new MesProEdhrWorkTaskServiceImpl(); ReflectionTestUtils.setField(tasks,"administratorService",service);
        var ledger=mock(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService.class);
        ReflectionTestUtils.setField(tasks,"sourceLedger",ledger);
        assertThrows(ServiceException.class,()->tasks.assignDeviationTask(new MesProEdhrDeviationDO().setId(90L),
                MesProEdhrWorkTaskService.TASK_TYPE_DEVIATION_RECEIVE,2L,"接收调查"));
    }
}
