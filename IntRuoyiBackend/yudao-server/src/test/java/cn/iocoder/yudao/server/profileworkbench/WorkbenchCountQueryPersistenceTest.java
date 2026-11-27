package cn.iocoder.yudao.server.profileworkbench;

import cn.iocoder.yudao.module.system.api.workbench.*;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class WorkbenchCountQueryPersistenceTest {
    private Configuration configuration;
    private JdbcTemplate jdbc;

    @BeforeEach void prepare() throws Exception {
        jdbc = new JdbcTemplate(new DriverManagerDataSource(
            "jdbc:h2:mem:workbench_count_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE fixture_task(tenant_id BIGINT, task_key VARCHAR(160), task_type VARCHAR(30), source VARCHAR(30), detail VARCHAR(80), status_label VARCHAR(30))");
        jdbc.execute("CREATE TABLE system_profile_workbench_task_visibility(tenant_id BIGINT, user_id BIGINT, task_key VARCHAR(160), deleted INT)");
        jdbc.update("INSERT INTO fixture_task VALUES(1,'S:a','type','source','needle','pending'),(1,'S:A','type','source','needle','pending'),(1,'S:hidden','type','source','needle','pending'),(1,'S:other','other','source','hay','pending'),(2,'S:foreign','type','source','needle','pending')");
        jdbc.update("INSERT INTO system_profile_workbench_task_visibility VALUES(1,3,'S:hidden',0),(2,3,'S:a',0),(1,4,'S:A',0)");
        configuration = new Configuration();
        String resource = "yudao-module-system/src/main/resources/mapper/profileworkbench/WorkbenchQuerySql.xml";
        try (var input = Files.newInputStream(Path.of("..").resolve(resource))) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        String fixture = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
            <mapper namespace="fixture">
              <sql id="base">SELECT task_key,task_type,source,detail,status_label FROM fixture_task WHERE tenant_id=#{q.tenantId}</sql>
              <select id="count" resultType="java.lang.Long"><include refid="cn.iocoder.yudao.module.system.api.workbench.WorkbenchQuerySql.exactCount"><property name="baseRef" value="fixture.base"/></include></select>
            </mapper>
            """;
        new XMLMapperBuilder(new ByteArrayInputStream(fixture.getBytes(StandardCharsets.UTF_8)),
            configuration, "fixture", configuration.getSqlFragments()).parse();
    }

    private Long count(String partition, String text) {
        var query = new WorkbenchQuerySpec("visible", null, null, null).context(1,3,1).partition(partition);
        if (text != null) { query.setFilterField("detail"); query.setFilterOperator("contains"); query.setFilterValue(text); }
        var parameters = Map.<String,Object>of("q", query);
        var bound = configuration.getMappedStatement("fixture.count").getBoundSql(parameters);
        // H2 fixtures already use Unicode; adapt MySQL conversion and preserve complete identity bytes.
        String sql = bound.getSql().replace("CONVERT(", "(").replace(" USING utf8mb4)", ")")
            .replace(" AS BINARY)", " AS VARBINARY)");
        Object[] values = bound.getParameterMappings().stream().map(mapping -> bound.hasAdditionalParameter(mapping.getProperty())
            ? bound.getAdditionalParameter(mapping.getProperty()) : SystemMetaObject.forObject(parameters).getValue(mapping.getProperty())).toArray();
        return jdbc.queryForObject(sql, Long.class, values);
    }

    @Test void exactCountsPreserveBinaryIdentityFiltersAndPersonalTenantVisibility() {
        assertEquals(4L, count(null,null));
        assertEquals(3L, count("visible",null));
        assertEquals(1L, count("hidden",null));
        assertEquals(2L, count("visible","needle"));
        assertEquals(1L, count("hidden","needle"));
        assertEquals(0L, count("visible","missing"));
    }

    @Test void duplicateBusinessIdentityFailsEvenWhenExcludedByFilterOrPartition() {
        jdbc.update("INSERT INTO fixture_task VALUES(1,'S:duplicate','other','source','hay','pending'),(1,'S:duplicate','other','source','hay','pending')");
        for (String partition : Arrays.asList(null,"visible","hidden")) {
            assertNull(count(partition,null));
            assertNull(count(partition,"needle"));
        }
    }

    @Test void emptySourceStillReturnsExactZero() {
        jdbc.update("DELETE FROM fixture_task");
        for (String partition : Arrays.asList(null,"visible","hidden")) assertEquals(0L,count(partition,"needle"));
    }
}
