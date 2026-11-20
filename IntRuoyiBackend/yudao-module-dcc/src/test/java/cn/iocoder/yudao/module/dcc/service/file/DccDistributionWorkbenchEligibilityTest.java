package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.system.api.workbench.QueryContext;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.io.StringReader;
import java.nio.file.*;
import java.sql.Connection;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Runs the shared production eligibility SQL against isolated data, without touching business records. */
class DccDistributionWorkbenchEligibilityTest {
    private JdbcTemplate jdbc;
    private Connection fixture;
    private Configuration configuration;

    @BeforeEach void setup() throws Exception {
        var source = new DriverManagerDataSource("jdbc:h2:mem:distribution_" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_UPPER=false", "sa", "");
        fixture = source.getConnection();
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE dcc_controlled_file (id BIGINT, tenant_id BIGINT, deleted INT, title VARCHAR, file_number VARCHAR, version_no VARCHAR, process_instance_id VARCHAR, status VARCHAR, published_time TIMESTAMP, controlled_time TIMESTAMP, activated_time TIMESTAMP, distributed_time TIMESTAMP, published_file_id BIGINT)");
        jdbc.execute("CREATE TABLE dcc_controlled_file_distribution (id BIGINT, tenant_id BIGINT, deleted INT, controlled_file_id BIGINT, distribution_medium VARCHAR, status VARCHAR, recovered_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE dcc_controlled_file_distribution_recipient (id BIGINT, tenant_id BIGINT, deleted INT, distribution_id BIGINT, user_id BIGINT, acknowledged_at TIMESTAMP, message_job_id BIGINT)");
        jdbc.execute("CREATE TABLE dcc_controlled_file_message_job (id BIGINT, tenant_id BIGINT, deleted INT, business_type VARCHAR, business_id BIGINT, recipient_user_id BIGINT, status VARCHAR, sent_at TIMESTAMP)");
        jdbc.update("INSERT INTO dcc_controlled_file VALUES (5,1,0,'SOP','SOP-1','C/1','round-1','ACTIVE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,500)");
        jdbc.update("INSERT INTO dcc_controlled_file_distribution VALUES (10,1,0,5,'PUBLIC_FOLDER','SENT',NULL)");
        jdbc.update("INSERT INTO dcc_controlled_file_distribution_recipient VALUES (11,1,0,10,99,NULL,20)");
        jdbc.update("INSERT INTO dcc_controlled_file_message_job VALUES (20,1,0,'DISTRIBUTION',10,99,'SENT',CURRENT_TIMESTAMP)");
        configuration = new Configuration();
        for (String file : new String[] {
                "../yudao-module-system/src/main/resources/mapper/profileworkbench/WorkbenchQuerySql.xml",
                "src/main/resources/mapper/profileworkbench/DccWorkbenchTodoMapper.xml"}) {
            try (var input = Files.newInputStream(Path.of(file))) {
                new XMLMapperBuilder(input, configuration, file, configuration.getSqlFragments()).parse();
            }
        }
        String wrapper = """
                <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
                <mapper namespace="eligibility"><select id="rows" resultType="map">
                <include refid="cn.iocoder.yudao.module.dcc.dal.mysql.file.DccWorkbenchTodoMapper.baseDistribution"/>
                </select></mapper>
                """;
        new XMLMapperBuilder(new StringReader(wrapper), configuration, "eligibility", configuration.getSqlFragments()).parse();
    }

    @AfterEach void close() throws Exception { fixture.close(); }

    private int eligibleRows() {
        var query = new QueryContext();
        query.setTenantId(1L); query.setUserId(99L);
        var bound = configuration.getMappedStatement("eligibility.rows").getBoundSql(Map.of("q", query));
        Object[] values = bound.getParameterMappings().stream()
                .map(mapping -> configuration.newMetaObject(Map.of("q", query)).getValue(mapping.getProperty())).toArray();
        return jdbc.queryForList(bound.getSql(), values).size();
    }

    @Test void effectiveFileStillNeedsItsOwnReceiptAndDisappearsAfterAcknowledgement() {
        assertEquals(1, eligibleRows());
        jdbc.update("UPDATE dcc_controlled_file_distribution_recipient SET acknowledged_at=CURRENT_TIMESTAMP");
        assertEquals(0, eligibleRows());
    }

    @Test void countPageAndVisibilityIdentityUseTheSameEligibility() throws Exception {
        String source = Files.readString(Path.of("src/main/resources/mapper/profileworkbench/DccWorkbenchTodoMapper.xml"));
        String[] eligibility = {"j.business_type = 'DISTRIBUTION'", "j.status = 'SENT'", "d.recovered_at IS NULL",
                "d.status IN ('PENDING','SENT','READ','ACKNOWLEDGED')", "f.status = 'ACTIVE'", "f.activated_time IS NOT NULL"};
        for (String condition : eligibility) assertEquals(1, source.split(java.util.regex.Pattern.quote(condition), -1).length - 1, condition);
        for (String statement : new String[] {"countDistribution", "chunkDistribution", "identityDistribution"})
            assertTrue(source.contains("id=\"" + statement + "\""), statement);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "UPDATE dcc_controlled_file SET status='SUPERSEDED'",
        "UPDATE dcc_controlled_file SET status='OBSOLETE'",
        "UPDATE dcc_controlled_file SET controlled_time=NULL",
        "UPDATE dcc_controlled_file SET activated_time=NULL",
        "UPDATE dcc_controlled_file SET distributed_time=NULL",
        "UPDATE dcc_controlled_file SET published_file_id=NULL",
        "UPDATE dcc_controlled_file SET version_no=' '",
        "UPDATE dcc_controlled_file SET process_instance_id=NULL",
        "UPDATE dcc_controlled_file_distribution SET recovered_at=CURRENT_TIMESTAMP",
        "UPDATE dcc_controlled_file_distribution SET status='RECOVERED'",
        "UPDATE dcc_controlled_file_distribution SET distribution_medium='PAPER'",
        "UPDATE dcc_controlled_file_distribution_recipient SET message_job_id=NULL",
        "UPDATE dcc_controlled_file_distribution_recipient SET user_id=98",
        "UPDATE dcc_controlled_file_message_job SET status='PENDING',sent_at=NULL",
        "UPDATE dcc_controlled_file_message_job SET status='FAILED',sent_at=NULL",
        "UPDATE dcc_controlled_file_message_job SET sent_at=NULL",
        "UPDATE dcc_controlled_file_message_job SET business_type='TRAINING'",
        "UPDATE dcc_controlled_file_message_job SET business_id=12",
        "UPDATE dcc_controlled_file_message_job SET recipient_user_id=98",
        "UPDATE dcc_controlled_file_message_job SET tenant_id=2",
        "UPDATE dcc_controlled_file_message_job SET deleted=1"
    })
    void unopenableDistributionMustNotAppearAsTodo(String mutation) {
        jdbc.update(mutation);
        assertEquals(0, eligibleRows(), mutation);
    }
}
