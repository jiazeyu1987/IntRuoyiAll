package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.module.dcc.registrationcertificate.service.accesspolicy.DccRegistrationCertificateAccessPolicyService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccRegistrationCertificateDepartmentMembershipTest {
    @Test
    void registrationIdentityFollowsOnlyLiveSameTenantAncestors() {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:registration-department;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("CREATE TABLE system_users(id BIGINT,tenant_id BIGINT,dept_id BIGINT,deleted INT)");
        jdbc.execute("CREATE TABLE system_dept(id BIGINT,tenant_id BIGINT,parent_id BIGINT,name VARCHAR(100),deleted INT)");
        jdbc.update("INSERT INTO system_dept VALUES (1,1,0,'注册部',0),(2,1,1,'国内注册',0),(3,1,2,'子组',0),(4,1,0,'生产部',0),(5,1,1,'已删除',1),(6,2,1,'国际注册',0),(7,1,8,'循环甲',0),(8,1,7,'循环乙',0)");
        jdbc.update("INSERT INTO system_users VALUES (11,1,1,0),(12,1,2,0),(13,1,3,0),(14,1,4,0),(15,1,5,0),(16,1,6,0),(17,1,7,0),(18,1,99,0),(19,1,2,1)");
        DccRegistrationCertificateAccessPolicyService policy = mock(DccRegistrationCertificateAccessPolicyService.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(policy,"jdbcTemplate",jdbc);
        for (long user : new long[]{11,12,13}) assertEquals(true, membership(policy,user));
        for (long user : new long[]{14,15,16,18,19,999}) assertEquals(false, membership(policy,user));
        assertThrows(IllegalStateException.class, () -> membership(policy,17));
    }

    private Object membership(DccRegistrationCertificateAccessPolicyService policy,long user) {
        return ReflectionTestUtils.invokeMethod(policy,"isRegistrationDepartment",1L,user);
    }
}
