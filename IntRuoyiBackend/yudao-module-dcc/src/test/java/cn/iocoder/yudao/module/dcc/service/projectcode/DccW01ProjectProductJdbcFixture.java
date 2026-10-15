package cn.iocoder.yudao.module.dcc.service.projectcode;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;

/** Explicit real H2 JDBC bean for writer fixtures; no production dependency or mock allocator. */
@TestConfiguration(proxyBeanMethods = false)
class DccW01ProjectProductJdbcFixture {
    @Bean @ConditionalOnMissingBean(JdbcTemplate.class)
    JdbcTemplate jdbcTemplate(DataSource source) { return new JdbcTemplate(source); }
}
