package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.service.pro.processpool.ProcessPoolTimelinePqcGroupSqlTest;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Executes actual production mapper statements/fragments against task-owned in-memory H2 fixtures. */
public class MesSa06IdentityMapperTest {
    private static final Path ROOT = Path.of(System.getProperty("sa06.mapperRoot", "src/main/resources/mapper/pro/processpool"));
    private static final String DETAIL = "cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderDetailReadMapper";
    private static final String TIMELINE = "cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolTimelineReadMapper";
    private static final String REVISION = "cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionMapper";

    @Test void actualDetailsAndPqcPartiesResolveSameIdOnlyWithinPersistedDomain() throws Exception {
        try (var db = database()) {
            var config = config("MesProcessPoolActiveOrderDetailReadMapper.xml", "");
            String sql = config.getMappedStatement(DETAIL + ".selectByActiveOrderId")
                    .getBoundSql(Map.of("activeOrderId", 100L)).getSql();
            try (var query = db.prepareStatement(sql)) {
                query.setLong(1, 100);
                try (var rows = query.executeQuery()) {
                    assertTrue(rows.next()); assertEquals("Temporary B", rows.getString("submitterName"));
                    assertEquals("Reviewer S", rows.getString("reviewerName"));
                    assertTrue(rows.next()); assertEquals("System S", rows.getString("submitterName"));
                    assertFalse(rows.next());
                }
            }
            var parties = config.getMappedStatement(DETAIL + ".selectEventPartiesByEventIds")
                    .getBoundSql(Map.of("eventIds", List.of(1L, 2L, 3L, 4L, 5L))).getSql();
            try (var query = db.prepareStatement(parties)) {
                for (int i = 1; i <= 5; i++) query.setLong(i, i);
                try (var rows = query.executeQuery()) {
                    var names = new java.util.HashMap<Long, String>();
                    while (rows.next()) names.put(rows.getLong("eventId"), rows.getString("submitterName"));
                    assertEquals("Temporary B", names.get(1L));
                    assertEquals("System S", names.get(2L));
                    // PQC's selected employee is formally a system user, even with a same-id profile.
                    assertEquals("System S", names.get(3L));
                    assertNull(names.get(4L), "Unknown domain must never guess a user by numeric ID");
                    assertNull(names.get(5L), "Person from another tenant must not supply a name");
                }
            }
        }
    }

    @Test void timelineAuthorityAndRevisionNameFilterFollowSameDomain() throws Exception {
        try (var db = database()) {
            var config = config("MesProProcessPoolTimelineReadMapper.xml", """
                <select id="identityProbe" resultType="map">
                  SELECT pool_event.id,
                  <include refid="ActualEmployeeName"/> AS person_name
                  FROM mes_pro_process_pool_event pool_event
                  LEFT JOIN mes_pro_work_order work_order ON work_order.id=pool_event.work_order_id
                  <include refid="TimelineAuthorityJoins"/>
                  ORDER BY pool_event.id
                </select>
                """);
            try (var query = db.createStatement(); var rows = query.executeQuery(config.getMappedStatement(TIMELINE + ".identityProbe").getBoundSql(Map.of()).getSql())) {
                assertTrue(rows.next()); assertEquals("Temporary B", rows.getString("person_name"));
                assertTrue(rows.next()); assertEquals("System S", rows.getString("person_name"));
                assertTrue(rows.next()); assertEquals("System S", rows.getString("person_name"));
                assertTrue(rows.next()); assertNull(rows.getString("person_name"));
                assertTrue(rows.next()); assertNull(rows.getString("person_name"));
            }
            var revision = config("MesProProcessPoolEventRevisionMapper.xml", "");
            var req = new cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProProductionReportRevisionLogPageReqVO();
            req.setActualEmployeeName("Temporary B");
            var bound = revision.getMappedStatement(REVISION + ".selectProductionReportRevisionLogCount")
                    .getBoundSql(Map.of("employeeUserIds", List.of(7L), "reqVO", req));
            try (var query = db.prepareStatement(bound.getSql())) {
                query.setLong(1, 7); query.setString(2, "Temporary B");
                try (var rows = query.executeQuery()) { assertTrue(rows.next()); assertEquals(1, rows.getInt(1)); }
                query.setString(2, "System S");
                try (var rows = query.executeQuery()) { assertTrue(rows.next()); assertEquals(1, rows.getInt(1)); }
            }
        }
    }

    static Configuration config(String file, String probe) throws Exception {
        var path = ROOT.resolve(file);
        // Dialect-only CHAR width / UNSIGNED conversion; joins and predicates remain production SQL.
        String xml = Files.readString(path).replace(" AS CHAR)", " AS VARCHAR)")
                .replace(" AS UNSIGNED)", " AS BIGINT)").replace("</mapper>", probe + "</mapper>");
        var config = new Configuration();
        new XMLMapperBuilder(new StringReader(xml), config, path.toString(), config.getSqlFragments()).parse();
        return config;
    }

    private static Connection database() throws Exception {
        var db = DriverManager.getConnection("jdbc:h2:mem:sa06_" + UUID.randomUUID() + ";MODE=MySQL");
        try (var s = db.createStatement()) {
            String aliases = ProcessPoolTimelinePqcGroupSqlTest.class.getName();
            s.execute("CREATE ALIAS JSON_VALID FOR '" + aliases + ".jsonValid'");
            s.execute("CREATE ALIAS JSON_EXTRACT FOR '" + aliases + ".jsonExtract'");
            s.execute("CREATE ALIAS JSON_UNQUOTE FOR '" + aliases + ".jsonUnquote'");
            s.execute("CREATE TABLE system_users(id BIGINT,tenant_id BIGINT,nickname VARCHAR,username VARCHAR,deleted INT DEFAULT 0)");
            s.execute("CREATE TABLE mes_pro_process_pool_team_employee_profile(id BIGINT,tenant_id BIGINT,display_name VARCHAR,employee_name VARCHAR,deleted INT DEFAULT 0,system_user_id BIGINT,enabled BOOLEAN DEFAULT TRUE)");
            s.execute("CREATE TABLE mes_pro_process_pool_event(id BIGINT,tenant_id BIGINT,event_type VARCHAR,template_type VARCHAR,actual_employee_id BIGINT,raw_payload VARCHAR,signature_id BIGINT,server_submit_time TIMESTAMP,work_order_id BIGINT,process_id BIGINT,device_id BIGINT,pqc_task_id BIGINT,feedback_source_type VARCHAR,deleted INT DEFAULT 0)");
            s.execute("CREATE TABLE mes_pro_process_pool_active_order_process_snapshot(id BIGINT,active_order_id BIGINT,work_order_id BIGINT,route_id BIGINT,route_process_id BIGINT,process_id BIGINT,process_code_snapshot VARCHAR,process_name_snapshot VARCHAR,planned_quantity_snapshot DECIMAL,tenant_id BIGINT,deleted INT DEFAULT 0)");
            s.execute("CREATE TABLE mes_pro_process_pool_active_order(id BIGINT,tenant_id BIGINT,deleted INT DEFAULT 0)");
            s.execute("CREATE TABLE mes_pro_work_order(id BIGINT,tenant_id BIGINT,code VARCHAR,batch_code VARCHAR,quantity DECIMAL,demand_bill_no VARCHAR,drawing_number VARCHAR,product_id BIGINT,material_specification VARCHAR,create_time TIMESTAMP,deleted INT DEFAULT 0)");
            s.execute("CREATE TABLE mes_md_item(id BIGINT,tenant_id BIGINT,code VARCHAR,name VARCHAR,deleted INT DEFAULT 0)");
            s.execute("CREATE TABLE mes_pro_route(id BIGINT,name VARCHAR,deleted INT DEFAULT 0)");
            s.execute("CREATE TABLE mes_pro_process(id BIGINT,tenant_id BIGINT,code VARCHAR,name VARCHAR,deleted INT DEFAULT 0)");
            s.execute("CREATE TABLE mes_pro_route_process(id BIGINT,route_id BIGINT,process_id BIGINT,key_flag BOOLEAN,deleted INT DEFAULT 0)");
            s.execute("CREATE TABLE mes_pro_process_pool_report_allocation(id BIGINT,active_order_id BIGINT,route_process_id BIGINT,process_id BIGINT,event_id BIGINT,allocated_quantity DECIMAL,tenant_id BIGINT,lifecycle_status VARCHAR,deleted INT DEFAULT 0)");
            s.execute("CREATE TABLE mes_pro_process_pool_team_device(id BIGINT,tenant_id BIGINT,device_code VARCHAR,device_name VARCHAR,deleted INT DEFAULT 0)");
            s.execute("CREATE TABLE mes_pro_process_pool_submission_review(id BIGINT,tenant_id BIGINT,event_id BIGINT,leader_user_id BIGINT,review_signature_id BIGINT,reviewed_at TIMESTAMP,deleted INT DEFAULT 0)");
            s.execute("CREATE TABLE mes_pqc_inspection_task(id BIGINT,tenant_id BIGINT,deleted INT DEFAULT 0)");
            s.execute("CREATE TABLE mes_pro_process_pool_event_revision(id BIGINT,event_id BIGINT,tenant_id BIGINT,modified_by_user_id BIGINT,revision_status VARCHAR,server_revision_time TIMESTAMP,deleted INT DEFAULT 0)");
            s.execute("INSERT INTO system_users VALUES(7,1,'System S','system-s',0),(8,1,'Reviewer S','reviewer',0),(9,2,'Other tenant','other',0)");
            s.execute("INSERT INTO mes_pro_process_pool_team_employee_profile(id,tenant_id,display_name,employee_name,deleted) VALUES(7,1,'Temporary B','temporary',0),(8,1,'Unrelated reviewer profile','other',0),(9,2,'Other tenant profile','other',0)");
            s.execute("INSERT INTO mes_pro_process_pool_event(id,tenant_id,event_type,actual_employee_id,raw_payload,server_submit_time,work_order_id,process_id) VALUES(1,1,'PRODUCTION_SUBMIT',7,'{\"signatureIdentityDomain\":\"MES_EMPLOYEE_PROFILE\"}','2026-10-04 10:00:00',10,30),(2,1,'PRODUCTION_SUBMIT',7,'{\"signatureIdentityDomain\":\"SYSTEM_USER\"}','2026-10-04 11:00:00',10,30),(3,1,'PQC_INSPECTION',7,'{}','2026-10-04 12:00:00',10,30),(4,1,'PRODUCTION_SUBMIT',7,'{\"signatureIdentityDomain\":\"UNKNOWN\"}','2026-10-04 13:00:00',10,30),(5,1,'PRODUCTION_SUBMIT',9,'{\"signatureIdentityDomain\":\"SYSTEM_USER\"}','2026-10-04 14:00:00',10,30)");
            s.execute("INSERT INTO mes_pro_process_pool_active_order VALUES(100,1,0)");
            s.execute("INSERT INTO mes_pro_work_order(id,tenant_id,code) VALUES(10,1,'Order')");
            s.execute("INSERT INTO mes_pro_route VALUES(20,'Route',0)");
            s.execute("INSERT INTO mes_pro_process VALUES(30,1,'P','Process',0)");
            s.execute("INSERT INTO mes_pro_process_pool_active_order_process_snapshot(id,active_order_id,work_order_id,route_id,route_process_id,process_id,tenant_id) VALUES(1,100,10,20,40,30,1)");
            s.execute("INSERT INTO mes_pro_process_pool_report_allocation(id,active_order_id,route_process_id,process_id,event_id,allocated_quantity,tenant_id,lifecycle_status) VALUES(1,100,40,30,1,1,1,'CURRENT'),(2,100,40,30,2,1,1,'CURRENT')");
            s.execute("INSERT INTO mes_pro_process_pool_submission_review(id,tenant_id,event_id,leader_user_id,reviewed_at) VALUES(1,1,1,8,'2026-10-04 14:00:00')");
            s.execute("INSERT INTO mes_pro_process_pool_event_revision(id,event_id,tenant_id,revision_status) VALUES(1,1,1,'EFFECTIVE'),(2,2,1,'EFFECTIVE')");
        }
        return db;
    }
}
