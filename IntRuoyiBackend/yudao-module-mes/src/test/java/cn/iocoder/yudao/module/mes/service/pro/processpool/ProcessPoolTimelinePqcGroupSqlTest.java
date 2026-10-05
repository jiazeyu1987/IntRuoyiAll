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


    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"OWN_RETURN_CORRECTION","LEADER_PROFILE_CORRECTION"})
    void productionWorkbenchReopensOnlyExactSignedCorrectionWithRealPendingReview(String origin) throws Exception {
        try(Connection db=database()) {
            event(db,1,101,null);review(db,1,101,101,"REJECTED");
            try(var st=db.createStatement()) {
                st.executeUpdate("UPDATE mes_pro_process_pool_event SET event_type='PRODUCTION_SUBMIT', report_management_status='UNALLOCATED' WHERE id=101");
                st.executeUpdate("UPDATE mes_pro_process_pool_submission_review SET leader_user_id=7 WHERE id=101");
            }
            var req=new ProcessPoolTimelinePageReqVO().setEventType("PRODUCTION_SUBMIT").setAllocationView("WORKBENCH").setSubmissionReviewStatus("PENDING");req.setPageNo(1);req.setPageSize(20);
            var params=Map.<String,Object>of("reqVO",req);
            assertEquals(List.of(),rows(db,params));
            correction(db,1,101,101,101,901L);
            String domain="LEADER_PROFILE_CORRECTION".equals(origin)?"MES_EMPLOYEE_PROFILE":"SYSTEM_USER";
            Long signer="LEADER_PROFILE_CORRECTION".equals(origin)?342L:7L;
            String payload="{\"activeOrderId\":413,\"signatureIdentityDomain\":\""+domain+"\",\"supersededReviewId\":101,\"count\":3}";
            try(var st=db.prepareStatement("UPDATE mes_pro_process_pool_event SET raw_payload=? WHERE id=101")) { st.setString(1,payload);st.executeUpdate(); }
            try(var st=db.prepareStatement("UPDATE mes_pro_process_pool_event_revision SET after_payload=? WHERE id=101")) { st.setString(1,payload);st.executeUpdate(); }
            try(var st=db.createStatement()) {
                st.executeUpdate("UPDATE mes_pro_process_pool_event SET work_order_id=123,route_process_id=88,actual_employee_id="+signer+",signature_user_id="+signer+",signature_id=9001 WHERE id=101");
                st.executeUpdate("UPDATE mes_pro_process_pool_submission_review SET leader_type='PRODUCTION' WHERE id=101");
                st.executeUpdate("UPDATE mes_pro_process_pool_event_revision SET before_payload='{\"count\":2}',change_reason='核对正式报工' WHERE id=101");
                st.executeUpdate("INSERT INTO system_electronic_signature(id,tenant_id,module_code,action_code,subject_type,actor_id,verification_status,signed_at,canonical_content_json) VALUES(9001,1,'MES','PRODUCTION_SUBMIT','MES_BATCH_RECORD',"+signer+",'VALID','2026-10-02 10:00:00','{\"signatureIdentity\":{\"domain\":\""+domain+"\",\"signerId\":"+signer+",\"tenantId\":1}}')");
                st.executeUpdate("INSERT INTO system_electronic_signature(id,tenant_id,module_code,action_code,subject_type,actor_id,verification_status,signed_at) VALUES(901,1,'MES','FIELD_CHANGE','MES_BATCH_RECORD',7,'VALID','2026-10-02 13:00:00')");
            }
            assertEquals(List.of(),rows(db,params)); // Signature alone is not a pending review.
            try(var st=db.prepareStatement("INSERT INTO mes_active_order_handoff_task(id,tenant_id,deleted,task_type,source_type,source_id,round_id,status,initiated_by,candidate_user_snapshot,active_order_id,responsibility_snapshot_json,work_order_id,route_process_id) VALUES(501,1,0,'PRODUCTION_REVIEW','PROCESS_POOL_EVENT_SIGNED_REVISION',101,101,'TODO',7,'7',413,?,123,88)")) {
                st.setString(1,"{\"correctionOrigin\":\""+origin+"\"}");st.executeUpdate();
            }
            assertEquals(List.of("101:PENDING"),rows(db,params));
            BoundSql count=configuration().getMappedStatement(NAMESPACE+".selectTimelineCount").getBoundSql(params);
            try(var st=db.prepareStatement(count.getSql()+" AND pool_event.tenant_id=1")) {bind(st,count,params);try(var result=st.executeQuery()){result.next();assertEquals(1,result.getLong(1));}}
            try(var st=db.createStatement();var audit=st.executeQuery("SELECT review_status FROM mes_pro_process_pool_submission_review WHERE id=101")){audit.next();assertEquals("REJECTED",audit.getString(1));}
            for(String mutation:List.of("round_id=999","tenant_id=2","status='DONE'","candidate_user_snapshot='8'","active_order_id=414","responsibility_snapshot_json='{\"correctionOrigin\":\"LEADER_PQC_CORRECTION\"}'")) {
                try(var st=db.createStatement()){st.executeUpdate("UPDATE mes_active_order_handoff_task SET "+mutation+" WHERE id=501");}
                assertEquals(List.of(),rows(db,params),mutation);
                try(var st=db.prepareStatement("UPDATE mes_active_order_handoff_task SET round_id=101,tenant_id=1,status='TODO',candidate_user_snapshot='7',active_order_id=413,responsibility_snapshot_json=? WHERE id=501")) {st.setString(1,"{\"correctionOrigin\":\""+origin+"\"}");st.executeUpdate();}
            }
            correction(db,1,302,101,999,902L);assertEquals(List.of(),rows(db,params)); // Newer wrong round supersedes old correction.
        }
    }

    @Test
    void originalProductionActorSignedCorrectionReopensOnlyItsExactRejectedRound() throws Exception {
        try (Connection db = database()) {
            productionCorrection(db);
            assertProductionStatus(db, "PENDING");
            assertEquals(List.of("101:PENDING"), rows(db, productionRequest("PENDING")));
            assertEquals(List.of(), rows(db, productionRequest("REJECTED")));
            try (var statement = db.createStatement()) {
                statement.executeUpdate("INSERT INTO mes_pro_process_pool_submission_review(id,tenant_id,event_id,leader_user_id,leader_type,review_status,reviewed_at) VALUES (202,1,101,9,'PRODUCTION','REJECTED','2026-10-02 14:00:00')");
            }
            assertProductionStatus(db, "REJECTED");
            assertEquals(List.of(), rows(db, productionRequest("PENDING")));
            try (var statement = db.createStatement()) {
                statement.executeUpdate("UPDATE mes_pro_process_pool_submission_review SET review_status='APPROVED' WHERE id=202");
            }
            assertProductionStatus(db, "APPROVED");
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
            "WRONG_USER", "WRONG_DOMAIN", "WRONG_SIGNED_DOMAIN", "WRONG_FIELD_ACTION", "INVALID_SIGNATURE",
            "WRONG_SIGNATURE_ACTOR", "WRONG_ORIGINAL_SIGNER", "WRONG_REJECT", "WRONG_REVISION", "UNSIGNED_REVISION",
            "WRONG_TENANT", "WRONG_BODY", "NO_BUSINESS_DIFF", "OLD_REVISION", "WRONG_LEADER_TYPE",
            "TASK_MISSING", "TASK_CANCELED", "TASK_WRONG_ORIGIN", "TASK_WRONG_ROUND", "TASK_WRONG_CYCLE",
            "TASK_WRONG_ACTOR", "TASK_WRONG_LEADER", "TASK_MULTI_LEADER", "TASK_JSON_LEADER", "TASK_DUPLICATED",
            "TASK_LEADER_PADDED_LEFT", "TASK_LEADER_PADDED_RIGHT", "TASK_LEADER_EMPTY", "TASK_LEADER_WHITESPACE"
    })
    void invalidProductionCorrectionKeepsOriginalRejectedProjection(String invalid) throws Exception {
        try (Connection db = database()) {
            productionCorrection(db);
            String mutation = switch (invalid) {
                case "WRONG_USER" -> "UPDATE mes_pro_process_pool_event_revision SET modified_by_user_id=8,revision_signature_user_id=8,revision_signature_snapshot='{\"signatureId\":901,\"actorId\":8,\"signedAt\":\"2026-10-02T13:00:00\"}' WHERE id=301";
                case "WRONG_DOMAIN" -> "UPDATE mes_pro_process_pool_event SET raw_payload=REPLACE(raw_payload,'SYSTEM_USER','MES_EMPLOYEE_PROFILE') WHERE id=101";
                case "WRONG_SIGNED_DOMAIN" -> "UPDATE system_electronic_signature SET canonical_content_json=REPLACE(canonical_content_json,'SYSTEM_USER','MES_EMPLOYEE_PROFILE') WHERE id=9001";
                case "WRONG_FIELD_ACTION" -> "UPDATE system_electronic_signature SET action_code='PRODUCTION_SUBMIT' WHERE id=901";
                case "INVALID_SIGNATURE" -> "UPDATE system_electronic_signature SET verification_status='INVALID' WHERE id=901";
                case "WRONG_SIGNATURE_ACTOR" -> "UPDATE system_electronic_signature SET actor_id=8 WHERE id=901";
                case "WRONG_ORIGINAL_SIGNER" -> "UPDATE mes_pro_process_pool_event SET signature_user_id=8 WHERE id=101";
                case "WRONG_REJECT" -> "UPDATE mes_pro_process_pool_event_revision SET after_payload=REPLACE(after_payload,'201','999') WHERE id=301";
                case "WRONG_REVISION" -> "UPDATE mes_pro_process_pool_event_revision SET revision_status='REVOKED' WHERE id=301";
                case "UNSIGNED_REVISION" -> "UPDATE mes_pro_process_pool_event_revision SET revision_signature_id=NULL WHERE id=301";
                case "WRONG_TENANT" -> "UPDATE mes_pro_process_pool_event_revision SET tenant_id=2 WHERE id=301";
                case "WRONG_BODY" -> "UPDATE mes_pro_process_pool_event SET raw_payload='{}' WHERE id=101";
                case "NO_BUSINESS_DIFF" -> "UPDATE mes_pro_process_pool_event_revision SET before_payload=after_payload WHERE id=301";
                case "OLD_REVISION" -> "UPDATE mes_pro_process_pool_event_revision SET server_revision_time='2026-10-02 11:00:00' WHERE id=301";
                case "WRONG_LEADER_TYPE" -> "UPDATE mes_pro_process_pool_submission_review SET leader_type='PQC' WHERE id=201";
                case "TASK_MISSING" -> "DELETE FROM mes_active_order_handoff_task WHERE id=501";
                case "TASK_CANCELED" -> "UPDATE mes_active_order_handoff_task SET status='CANCELED' WHERE id=501";
                case "TASK_WRONG_ORIGIN" -> "UPDATE mes_active_order_handoff_task SET responsibility_snapshot_json='{\"correctionOrigin\":\"LEADER_PQC_CORRECTION\"}' WHERE id=501";
                case "TASK_WRONG_ROUND" -> "UPDATE mes_active_order_handoff_task SET round_id=302 WHERE id=501";
                case "TASK_WRONG_CYCLE" -> "UPDATE mes_active_order_handoff_task SET active_order_id=415 WHERE id=501";
                case "TASK_WRONG_ACTOR" -> "UPDATE mes_active_order_handoff_task SET initiated_by=8 WHERE id=501";
                case "TASK_WRONG_LEADER" -> "UPDATE mes_active_order_handoff_task SET candidate_user_snapshot='8' WHERE id=501";
                case "TASK_MULTI_LEADER" -> "UPDATE mes_active_order_handoff_task SET candidate_user_snapshot='9,8' WHERE id=501";
                case "TASK_JSON_LEADER" -> "UPDATE mes_active_order_handoff_task SET candidate_user_snapshot='[9]' WHERE id=501";
                case "TASK_LEADER_PADDED_LEFT" -> "UPDATE mes_active_order_handoff_task SET candidate_user_snapshot=' 9' WHERE id=501";
                case "TASK_LEADER_PADDED_RIGHT" -> "UPDATE mes_active_order_handoff_task SET candidate_user_snapshot='9 ' WHERE id=501";
                case "TASK_LEADER_EMPTY" -> "UPDATE mes_active_order_handoff_task SET candidate_user_snapshot='' WHERE id=501";
                case "TASK_LEADER_WHITESPACE" -> "UPDATE mes_active_order_handoff_task SET candidate_user_snapshot=' ' WHERE id=501";
                case "TASK_DUPLICATED" -> "INSERT INTO mes_active_order_handoff_task SELECT 502,tenant_id,deleted,active_order_id,work_order_id,route_process_id,task_type,source_type,source_id,round_id,candidate_user_snapshot,responsibility_snapshot_json,initiated_by,status FROM mes_active_order_handoff_task WHERE id=501";
                default -> throw new IllegalArgumentException("Unknown invalid fixture: " + invalid);
            };
            try (var statement = db.createStatement()) {
                statement.executeUpdate(mutation);
                if ("WRONG_DOMAIN".equals(invalid)) {
                    statement.executeUpdate("UPDATE mes_pro_process_pool_event_revision SET after_payload=REPLACE(after_payload,'SYSTEM_USER','MES_EMPLOYEE_PROFILE') WHERE id=301");
                } else if ("WRONG_REJECT".equals(invalid)) {
                    statement.executeUpdate("UPDATE mes_pro_process_pool_event SET raw_payload=REPLACE(raw_payload,'201','999') WHERE id=101");
                }
            }
            assertProductionStatus(db, "REJECTED");
            assertEquals(List.of(), rows(db, productionRequest("PENDING")));
        }
    }

    @Test
    void newerEffectiveProductionRevisionCannotReuseOlderSignedCorrectionTask() throws Exception {
        try (Connection db = database()) {
            productionCorrection(db);
            try (var statement = db.createStatement()) {
                statement.executeUpdate("INSERT INTO mes_pro_process_pool_event_revision SELECT 302,tenant_id,event_id,revision_status,revision_signature_id,revision_signature_user_id,modified_by_user_id,revision_signature_snapshot,after_payload,TIMESTAMP '2026-10-02 14:00:00',deleted,before_payload,change_reason FROM mes_pro_process_pool_event_revision WHERE id=301");
            }
            assertProductionStatus(db, "REJECTED");
        }
    }

    @Test
    void longSystemLeaderIdRequiresEveryCandidateByteToMatch() throws Exception {
        try (Connection db = database()) {
            productionCorrection(db);
            try (var statement = db.createStatement()) {
                statement.executeUpdate("UPDATE mes_pro_process_pool_submission_review SET leader_user_id=9908090341 WHERE id=201");
                statement.executeUpdate("UPDATE mes_active_order_handoff_task SET candidate_user_snapshot='9908090341' WHERE id=501");
            }
            assertProductionStatus(db, "PENDING");
            try (var statement = db.createStatement()) {
                statement.executeUpdate("UPDATE mes_active_order_handoff_task SET candidate_user_snapshot='9908090342' WHERE id=501");
            }
            assertProductionStatus(db, "REJECTED");
        }
    }

    private static Map<String, Object> productionRequest(String status) {
        var params = request(status, 1, 20);
        ((ProcessPoolTimelinePageReqVO) params.get("reqVO")).setEventType("PRODUCTION_SUBMIT");
        return params;
    }

    private static void assertProductionStatus(Connection db, String expected) throws Exception {
        assertEquals(List.of("101:" + expected), rows(db, productionRequest(null)));
        BoundSql count = configuration().getMappedStatement(NAMESPACE + ".selectTimelineCount")
                .getBoundSql(productionRequest(expected));
        try (var statement = db.prepareStatement(count.getSql() + " AND pool_event.tenant_id = 1")) {
            bind(statement, count, productionRequest(expected));
            try (var result = statement.executeQuery()) { result.next(); assertEquals(1L, result.getLong(1)); }
        }
    }

    private static void productionCorrection(Connection db) throws Exception {
        String before = "{\"activeOrderId\":414,\"signatureIdentityDomain\":\"SYSTEM_USER\",\"cleaningCount\":2}";
        String after = "{\"activeOrderId\":414,\"signatureIdentityDomain\":\"SYSTEM_USER\",\"cleaningCount\":3,\"supersededReviewId\":201}";
        try (var statement = db.prepareStatement("INSERT INTO mes_pro_process_pool_event(id,tenant_id,event_type,raw_payload,work_order_id,route_process_id,actual_employee_id,signature_user_id,signature_id) VALUES(101,1,'PRODUCTION_SUBMIT',?,123,88,7,7,9001)")) {
            statement.setString(1, after); statement.executeUpdate();
        }
        try (var statement = db.createStatement()) {
            statement.executeUpdate("INSERT INTO mes_pro_process_pool_submission_review(id,tenant_id,event_id,leader_user_id,leader_type,review_status,reviewed_at) VALUES(201,1,101,9,'PRODUCTION','REJECTED','2026-10-02 12:00:00')");
            statement.executeUpdate("INSERT INTO system_electronic_signature(id,tenant_id,module_code,action_code,subject_type,actor_id,verification_status,signed_at,canonical_content_json) VALUES(9001,1,'MES','PRODUCTION_SUBMIT','MES_BATCH_RECORD',7,'VALID','2026-10-02 10:00:00','{\"signatureIdentity\":{\"domain\":\"SYSTEM_USER\",\"signerId\":7,\"tenantId\":1}}')");
            statement.executeUpdate("INSERT INTO system_electronic_signature(id,tenant_id,module_code,action_code,subject_type,actor_id,verification_status,signed_at) VALUES(901,1,'MES','FIELD_CHANGE','MES_BATCH_RECORD',7,'VALID','2026-10-02 13:00:00')");
            statement.executeUpdate("INSERT INTO mes_active_order_handoff_task(id,tenant_id,active_order_id,work_order_id,route_process_id,task_type,source_type,source_id,round_id,candidate_user_snapshot,responsibility_snapshot_json,initiated_by,status) VALUES(501,1,414,123,88,'PRODUCTION_REVIEW','PROCESS_POOL_EVENT_SIGNED_REVISION',101,301,'9','{\"correctionOrigin\":\"OWN_RETURN_CORRECTION\"}',7,'TODO')");
        }
        try (var statement = db.prepareStatement("INSERT INTO mes_pro_process_pool_event_revision(id,tenant_id,event_id,revision_status,revision_signature_id,revision_signature_user_id,modified_by_user_id,revision_signature_snapshot,after_payload,server_revision_time,before_payload,change_reason) VALUES(301,1,101,'EFFECTIVE',901,7,7,'{\"signatureId\":901,\"actorId\":7,\"signedAt\":\"2026-10-02T13:00:00\"}',?,'2026-10-02 13:00:00',?,'核对清洗次数')")) {
            statement.setString(1, after); statement.setString(2, before); statement.executeUpdate();
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
        // MySQL CAST(... AS BINARY) keeps all bytes; H2 BINARY defaults to fixed length 1.
        // Normalize only these dialect casts, keeping exact byte equality and all status predicates.
        // H2 does not prove MySQL collation behavior; the real MySQL expression has separate evidence.
        String xml = Files.readString(XML).replace(" AS CHAR)", " AS VARCHAR)")
                .replace(" AS BINARY)", " AS VARBINARY)")
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
            statement.execute("CREATE DOMAIN IF NOT EXISTS UNSIGNED AS BIGINT");
            // Columns checked against existing event/review/revision migrations; only queried columns are needed here.
            statement.execute("CREATE TABLE mes_pro_process_pool_event(id BIGINT,tenant_id BIGINT,deleted INT DEFAULT 0,event_type VARCHAR(40),raw_payload VARCHAR(4000),work_order_id BIGINT,report_management_status VARCHAR(40))");
            statement.execute("CREATE TABLE mes_pro_work_order(id BIGINT,tenant_id BIGINT,deleted INT DEFAULT 0)");
            statement.execute("CREATE TABLE mes_pro_process_pool_submission_review(id BIGINT,tenant_id BIGINT,event_id BIGINT,leader_user_id BIGINT,review_status VARCHAR(30),review_remark VARCHAR(50),reviewed_at TIMESTAMP,deleted INT DEFAULT 0)");
            statement.execute("CREATE TABLE mes_pro_process_pool_event_revision(id BIGINT,tenant_id BIGINT,event_id BIGINT,revision_status VARCHAR(30),revision_signature_id BIGINT,revision_signature_user_id BIGINT,modified_by_user_id BIGINT,revision_signature_snapshot VARCHAR(2000),after_payload VARCHAR(4000),server_revision_time TIMESTAMP,deleted INT DEFAULT 0)");
            statement.execute("ALTER TABLE mes_pro_process_pool_event ADD route_process_id BIGINT");
            statement.execute("ALTER TABLE mes_pro_process_pool_event ADD actual_employee_id BIGINT");
            statement.execute("ALTER TABLE mes_pro_process_pool_event ADD signature_user_id BIGINT");
            statement.execute("ALTER TABLE mes_pro_process_pool_event ADD signature_id BIGINT");
            statement.execute("ALTER TABLE mes_pro_process_pool_submission_review ADD leader_type VARCHAR(30)");
            statement.execute("ALTER TABLE mes_pro_process_pool_event_revision ADD before_payload VARCHAR(4000)");
            statement.execute("ALTER TABLE mes_pro_process_pool_event_revision ADD change_reason VARCHAR(200)");
            statement.execute("CREATE TABLE system_electronic_signature(id BIGINT,tenant_id BIGINT,deleted INT DEFAULT 0,module_code VARCHAR(30),action_code VARCHAR(40),subject_type VARCHAR(40),actor_id BIGINT,verification_status VARCHAR(30),signed_at TIMESTAMP,canonical_content_json VARCHAR(2000))");
            statement.execute("CREATE TABLE mes_active_order_handoff_task(id BIGINT,tenant_id BIGINT,deleted INT DEFAULT 0,active_order_id BIGINT,work_order_id BIGINT,route_process_id BIGINT,task_type VARCHAR(40),source_type VARCHAR(40),source_id BIGINT,round_id BIGINT,candidate_user_snapshot VARCHAR(2000),responsibility_snapshot_json VARCHAR(2000),initiated_by BIGINT,status VARCHAR(30))");
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
        if (path.startsWith("$[")) node = node.path(Integer.parseInt(path.substring(2, path.length() - 1)));
        else if (!"$".equals(path)) {
            for (String segment : path.substring(2).split("\\.")) node = node.path(segment);
        }
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
