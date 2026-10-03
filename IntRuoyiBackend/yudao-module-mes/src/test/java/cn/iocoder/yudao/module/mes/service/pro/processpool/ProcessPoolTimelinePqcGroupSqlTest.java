package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelinePageReqVO;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Executes production XML joins/filters in isolated H2. JSON aliases cover this fixture, not MySQL parity. */
public class ProcessPoolTimelinePqcGroupSqlTest {
    private static final String NAMESPACE = "cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolTimelineReadMapper";
    private static final Path XML = Path.of("src/main/resources/mapper/pro/processpool/MesProProcessPoolTimelineReadMapper.xml");

    @Test
    void approvedNewestMemberMustNotHideOlderPendingMembers() throws Exception {
        try (Connection db = database()) {
            event(db, 1, 101, "g"); event(db, 1, 102, "g"); event(db, 1, 103, "g");
            review(db, 1, 203, 103, "APPROVED");
            assertRows(db, null, List.of("102:PENDING"));
            assertRows(db, "PENDING", List.of("102:PENDING"));
            assertRows(db, "APPROVED", List.of());
            review(db, 1, 202, 102, "APPROVED");
            assertRows(db, "PENDING", List.of("101:PENDING"));
            review(db, 1, 201, 101, "APPROVED");
            assertRows(db, "PENDING", List.of());
            assertRows(db, "APPROVED", List.of("103:APPROVED"));
        }
    }

    @Test
    void rejectedMemberRemainsVisibleUntilItsOwnSignedCorrectionReopensReview() throws Exception {
        try (Connection db = database()) {
            event(db, 1, 101, "g"); event(db, 1, 102, "g");
            review(db, 1, 201, 101, "REJECTED"); review(db, 1, 202, 102, "APPROVED");
            assertRows(db, null, List.of("101:REJECTED"));
            assertRows(db, "REJECTED", List.of("101:REJECTED"));
            assertRows(db, "APPROVED", List.of());
            correction(db, 1, 301, 101, 201, 901L);
            assertRows(db, "PENDING", List.of("101:PENDING"));
            assertRows(db, "REJECTED", List.of());
            review(db, 1, 203, 101, "APPROVED");
            assertRows(db, "PENDING", List.of());
            assertRows(db, "APPROVED", List.of("102:APPROVED"));
        }
    }

    @Test
    void unsignedMismatchedAndSupersededCorrectionsDoNotReopenRejectedMember() throws Exception {
        try (Connection db = database()) {
            event(db, 1, 101, "g"); event(db, 1, 102, "g");
            review(db, 1, 201, 101, "REJECTED"); review(db, 1, 202, 102, "APPROVED");
            correction(db, 1, 301, 101, 201, null);
            assertRows(db, "PENDING", List.of());
            correction(db, 1, 302, 101, 999, 902L);
            assertRows(db, "PENDING", List.of());
            correction(db, 1, 303, 101, 201, 903L);
            try (var statement = db.createStatement()) {
                statement.executeUpdate("UPDATE mes_pro_process_pool_event SET raw_payload='{\"pqcSubmissionGroupId\":\"g\",\"changed\":true}' WHERE id=101");
            }
            assertRows(db, "PENDING", List.of());
            correction(db, 1, 304, 101, 201, 904L);
            review(db, 1, 205, 101, "REJECTED");
            assertRows(db, "PENDING", List.of());
            assertRows(db, null, List.of("101:REJECTED"));
        }
    }

    @Test
    void groupReviewAndCorrectionLookupsStayWithinTenantAndIgnoreDeletedMembers() throws Exception {
        try (Connection db = database()) {
            event(db, 1, 101, "g"); event(db, 1, 102, "g");
            event(db, 2, 999, "g");
            review(db, 1, 202, 102, "APPROVED");
            review(db, 2, 999, 101, "APPROVED");
            assertRows(db, "PENDING", List.of("101:PENDING"));
            review(db, 1, 201, 101, "REJECTED");
            correction(db, 2, 301, 101, 201, 901L);
            assertRows(db, "PENDING", List.of());
            assertRows(db, "REJECTED", List.of("101:REJECTED"));
            event(db, 1, 1000, "g");
            try (var statement = db.createStatement()) {
                statement.executeUpdate("UPDATE mes_pro_process_pool_event SET deleted=1 WHERE id=1000");
            }
            assertRows(db, null, List.of("101:REJECTED"));
        }
    }

    @Test
    void independentGroupsSingleEventsAndPagingHaveMatchingTotals() throws Exception {
        try (Connection db = database()) {
            event(db, 1, 101, "a"); event(db, 1, 102, "a"); event(db, 1, 103, "b");
            event(db, 1, 104, null); event(db, 1, 105, null);
            review(db, 1, 201, 102, "APPROVED");
            assertRows(db, "PENDING", List.of("105:PENDING", "104:PENDING", "103:PENDING", "101:PENDING"));
            assertEquals(List.of("103:PENDING", "101:PENDING"), rows(db, request("PENDING", 2, 2)));
        }
    }

    @Test
    void uncorrectedRejectedMemberMustRemainAccessibleBeforeGroupCanBeApproved() throws Exception {
        try (Connection db = database()) {
            event(db, 1, 101, "g"); event(db, 1, 102, "g");
            review(db, 1, 201, 101, "REJECTED"); review(db, 1, 202, 102, "REJECTED");
            assertRows(db, null, List.of("102:REJECTED"));
            correction(db, 1, 302, 102, 202, 902L);
            assertRows(db, null, List.of("101:REJECTED"));
            assertRows(db, "PENDING", List.of());
            correction(db, 1, 301, 101, 201, 901L);
            assertRows(db, "PENDING", List.of("102:PENDING"));
        }
    }

    private static void assertRows(Connection db, String status, List<String> expected) throws Exception {
        var params = request(status, 1, 20);
        assertEquals(expected, rows(db, params));
        // Execute the actual production count statement with the explicit test tenant predicate.
        BoundSql count = configuration().getMappedStatement(NAMESPACE + ".selectTimelineCount").getBoundSql(params);
        try (var statement = db.prepareStatement(count.getSql() + " AND pool_event.tenant_id = 1")) {
            bind(statement, count, params);
            try (var result = statement.executeQuery()) { result.next(); assertEquals(expected.size(), result.getLong(1)); }
        }
    }

    private static List<String> rows(Connection db, Map<String, Object> params) throws Exception {
        BoundSql sql = configuration().getMappedStatement(NAMESPACE + ".testPqcRows").getBoundSql(params);
        try (var statement = db.prepareStatement(sql.getSql())) {
            bind(statement, sql, params);
            List<String> result = new ArrayList<>();
            try (var rows = statement.executeQuery()) {
                while (rows.next()) result.add(rows.getLong(1) + ":" + rows.getString(2));
            }
            return result;
        }
    }

    private static Configuration configuration() throws Exception {
        // Reuse the real projection join/filter fragments; no handwritten representative-selection SQL.
        String probe = """
                <select id="testPqcRows" resultType="map">
                  <bind name="offset" value="(reqVO.pageNo - 1) * reqVO.pageSize"/>
                  SELECT pool_event.id, COALESCE(latest_submission_review.review_status, 'PENDING')
                  FROM mes_pro_process_pool_event pool_event
                  <include refid="LatestSubmissionReviewJoin"/>
                  WHERE pool_event.tenant_id = 1 AND <include refid="TimelineFilters"/>
                  ORDER BY pool_event.id DESC LIMIT #{offset}, #{reqVO.pageSize}
                </select>
                """;
        // MySQL CAST(... AS CHAR) preserves numeric text; H2 defaults CHAR to length 1.
        // Normalize this dialect-only cast; representative/status predicates remain production SQL.
        String xml = Files.readString(XML).replace(" AS CHAR)", " AS VARCHAR)")
                .replace("</mapper>", probe + "</mapper>");
        Configuration configuration = new Configuration();
        new XMLMapperBuilder(new StringReader(xml), configuration, XML.toString(), configuration.getSqlFragments()).parse();
        return configuration;
    }

    private static Map<String, Object> request(String status, int page, int size) {
        var request = new ProcessPoolTimelinePageReqVO().setEventType("PQC_INSPECTION").setSubmissionReviewStatus(status);
        request.setPageNo(page); request.setPageSize(size);
        return Map.of("reqVO", request);
    }

    private static void bind(java.sql.PreparedStatement statement, BoundSql sql, Map<String, Object> params) throws Exception {
        var meta = new Configuration().newMetaObject(params);
        int index = 1;
        for (var mapping : sql.getParameterMappings()) {
            String name = mapping.getProperty();
            statement.setObject(index++, sql.hasAdditionalParameter(name) ? sql.getAdditionalParameter(name) : meta.getValue(name));
        }
    }

    private static Connection database() throws Exception {
        Connection db = DriverManager.getConnection("jdbc:h2:mem:pqc_group_" + UUID.randomUUID() + ";MODE=MySQL");
        try (var statement = db.createStatement()) {
            String type = ProcessPoolTimelinePqcGroupSqlTest.class.getName();
            statement.execute("CREATE ALIAS JSON_VALID FOR '" + type + ".jsonValid'");
            statement.execute("CREATE ALIAS JSON_EXTRACT FOR '" + type + ".jsonExtract'");
            statement.execute("CREATE ALIAS JSON_UNQUOTE FOR '" + type + ".jsonUnquote'");
            statement.execute("CREATE ALIAS JSON_TYPE FOR '" + type + ".jsonType'");
            // Columns checked against existing event/review/revision migrations; only queried columns are needed here.
            statement.execute("CREATE TABLE mes_pro_process_pool_event(id BIGINT,tenant_id BIGINT,deleted INT DEFAULT 0,event_type VARCHAR(40),raw_payload VARCHAR(4000),work_order_id BIGINT)");
            statement.execute("CREATE TABLE mes_pro_work_order(id BIGINT,tenant_id BIGINT,deleted INT DEFAULT 0)");
            statement.execute("CREATE TABLE mes_pro_process_pool_submission_review(id BIGINT,tenant_id BIGINT,event_id BIGINT,leader_user_id BIGINT,review_status VARCHAR(30),review_remark VARCHAR(50),reviewed_at TIMESTAMP,deleted INT DEFAULT 0)");
            statement.execute("CREATE TABLE mes_pro_process_pool_event_revision(id BIGINT,tenant_id BIGINT,event_id BIGINT,revision_status VARCHAR(30),revision_signature_id BIGINT,revision_signature_user_id BIGINT,modified_by_user_id BIGINT,revision_signature_snapshot VARCHAR(2000),after_payload VARCHAR(4000),server_revision_time TIMESTAMP,deleted INT DEFAULT 0)");
        }
        return db;
    }

    private static void event(Connection db, long tenant, long id, String group) throws Exception {
        try (var statement = db.prepareStatement("INSERT INTO mes_pro_process_pool_event(id,tenant_id,event_type,raw_payload) VALUES (?,?,'PQC_INSPECTION',?)")) {
            statement.setLong(1, id); statement.setLong(2, tenant);
            statement.setString(3, group == null ? "{}" : "{\"pqcSubmissionGroupId\":\"" + group + "\"}"); statement.executeUpdate();
        }
    }

    private static void review(Connection db, long tenant, long id, long event, String status) throws Exception {
        try (var statement = db.prepareStatement("INSERT INTO mes_pro_process_pool_submission_review(id,tenant_id,event_id,review_status,reviewed_at) VALUES (?,?,?,?, '2026-10-02 12:00:00')")) {
            statement.setLong(1, id); statement.setLong(2, tenant); statement.setLong(3, event); statement.setString(4, status); statement.executeUpdate();
        }
    }

    private static void correction(Connection db, long tenant, long id, long event, long review, Long signature) throws Exception {
        String payload = "{\"pqcSubmissionGroupId\":\"g\",\"supersededReviewId\":" + review + "}";
        try (var statement = db.prepareStatement("UPDATE mes_pro_process_pool_event SET raw_payload=? WHERE id=? AND tenant_id=?")) {
            statement.setString(1, payload); statement.setLong(2, event); statement.setLong(3, tenant); statement.executeUpdate();
        }
        try (var statement = db.prepareStatement("INSERT INTO mes_pro_process_pool_event_revision(id,tenant_id,event_id,revision_status,revision_signature_id,revision_signature_user_id,modified_by_user_id,revision_signature_snapshot,after_payload,server_revision_time) VALUES (?,?,?,'EFFECTIVE',?,7,7,?,?, '2026-10-02 13:00:00')")) {
            statement.setLong(1, id); statement.setLong(2, tenant); statement.setLong(3, event); statement.setObject(4, signature);
            statement.setString(5, "{\"signatureId\":" + signature + ",\"actorId\":7,\"signedAt\":\"2026-10-02T13:00:00\"}");
            statement.setString(6, payload); statement.executeUpdate();
        }
    }

    public static boolean jsonValid(String json) {
        if (json == null) return false;
        try { return JsonUtils.parseTree(json) != null; } catch (RuntimeException invalidJson) { return false; }
    }
    public static String jsonExtract(String json, String path) {
        if (json == null) return null;
        var node = JsonUtils.parseTree(json);
        if (!"$".equals(path)) node = node.path(path.substring(2));
        return node.isMissingNode() ? null : node.toString();
    }
    public static String jsonUnquote(String json) {
        if (json == null) return null;
        var node = JsonUtils.parseTree(json);
        return node.isTextual() ? node.textValue() : node.toString();
    }
    public static String jsonType(String json) {
        if (json == null) return null;
        var node = JsonUtils.parseTree(json);
        return node.isIntegralNumber() ? "INTEGER" : node.isNull() ? "NULL" : node.isTextual() ? "STRING" : "OBJECT";
    }
}
