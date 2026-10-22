package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.bpm.approval.content.ApprovalContentJson;
import cn.iocoder.yudao.module.mes.approval.content.EdhrReleaseContentPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** Isolated H2 physical transactions; no application bootstrap or production datasource. */
class MesProductionRequestTransactionIntegrationTest {
    private JdbcTemplate jdbc;
    private TransactionTemplate transaction;

    @BeforeEach void database() {
        var source=new DriverManagerDataSource("jdbc:h2:mem:production_request_"+UUID.randomUUID()
                +";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc=new JdbcTemplate(source);
        transaction=new TransactionTemplate(new DataSourceTransactionManager(source));
        jdbc.execute("CREATE TABLE command_receipt (request_key VARCHAR(128) PRIMARY KEY, proof VARCHAR(1000) NOT NULL)");
        jdbc.execute("CREATE TABLE business_effect (id BIGINT PRIMARY KEY)");
    }

    @Test void committedProofAcceptsSameBusinessAndRejectsChangedFieldsWithoutNewEffects() {
        var first=MesProFrontlineFeedbackSubmitTestData.buildSubmitReq();
        String hash=MesProductionSubmitRequestIdentity.hash(first);
        String key=first.getProcessPoolSubmissionIdempotencyKey();
        transaction.executeWithoutResult(s -> {
            jdbc.update("INSERT INTO business_effect VALUES (?)",1L);
            jdbc.update("INSERT INTO command_receipt VALUES (?,?)",key,JsonUtils.toJsonString(
                    Map.of("version",MesProductionSubmitRequestIdentity.VERSION,"hash",hash)));
        });
        var retry=MesProFrontlineFeedbackSubmitTestData.buildSubmitReq();
        retry.setSignaturePassword("another-current-password");
        MesProductionSubmitRequestIdentity.requireMatches(MesProductionSubmitRequestIdentity.hash(retry),
                JsonUtils.parseTree(jdbc.queryForObject("SELECT proof FROM command_receipt WHERE request_key=?",String.class,key)));
        retry.getFeedbackPayload().setRemark("changed business remark");
        assertThrows(IllegalArgumentException.class,()->transaction.executeWithoutResult(s -> {
            jdbc.update("INSERT INTO business_effect VALUES (?)",2L);
            MesProductionSubmitRequestIdentity.requireMatches(MesProductionSubmitRequestIdentity.hash(retry),
                    JsonUtils.parseTree(jdbc.queryForObject("SELECT proof FROM command_receipt WHERE request_key=?",String.class,key)));
        }));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM business_effect",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM command_receipt",Integer.class));
    }

    @Test void downstreamFailureRollsBackBothBusinessAndReceiptOnRealConnection() {
        var first=MesProFrontlineFeedbackSubmitTestData.buildSubmitReq();
        assertThrows(IllegalStateException.class,()->transaction.executeWithoutResult(s -> {
            jdbc.update("INSERT INTO business_effect VALUES (?)",1L);
            jdbc.update("INSERT INTO command_receipt VALUES (?,?)","rollback",JsonUtils.toJsonString(
                    Map.of("version",MesProductionSubmitRequestIdentity.VERSION,"hash",MesProductionSubmitRequestIdentity.hash(first))));
            throw new IllegalStateException("downstream failed after receipt insert");
        }));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM business_effect",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM command_receipt",Integer.class));
    }

    @Test void incompleteOrUnknownSixRootsCannotCommitEffects() {
        var policy=new EdhrReleaseContentPolicy();
        for(String body:java.util.List.of("{}", "{\"contentPolicyVersion\":\"approval-content-v1\",\"scopeCode\":\"EDHR_RELEASE\",\"identity\":{},\"workTask\":{},\"release\":{},\"finalization\":{}}")) {
            assertThrows(IllegalArgumentException.class,()->transaction.executeWithoutResult(s -> {
                jdbc.update("INSERT INTO business_effect VALUES (?)",1L);
                policy.validate(ApprovalContentJson.parse(body));
            }));
            assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM business_effect",Integer.class));
        }
    }

    @Test void everyOriginalBusinessSectionIsBoundAndClientProofIsRejected() {
        String baseline=MesProductionSubmitRequestIdentity.hash(MesProFrontlineFeedbackSubmitTestData.buildSubmitReq());
        var context=MesProFrontlineFeedbackSubmitTestData.buildSubmitReq();context.getProcessPoolContext().setActiveOrderId(999L);
        var payload=MesProFrontlineFeedbackSubmitTestData.buildSubmitReq();payload.getRawPayload().put("customInspection","changed");
        var identity=MesProFrontlineFeedbackSubmitTestData.buildSubmitReq();identity.setActualEmployeeId(999L);
        var snapshot=MesProFrontlineFeedbackSubmitTestData.buildSubmitReq();snapshot.setFrontlineSessionSnapshotHash("changed");
        for(var changed:java.util.List.of(context,payload,identity,snapshot)) assertNotEquals(baseline,MesProductionSubmitRequestIdentity.hash(changed));
        payload.getRawPayload().put(MesProductionSubmitRequestIdentity.RAW_KEY,Map.of("hash",baseline));
        assertThrows(IllegalArgumentException.class,()->MesProductionSubmitRequestIdentity.hash(payload));
    }
}
