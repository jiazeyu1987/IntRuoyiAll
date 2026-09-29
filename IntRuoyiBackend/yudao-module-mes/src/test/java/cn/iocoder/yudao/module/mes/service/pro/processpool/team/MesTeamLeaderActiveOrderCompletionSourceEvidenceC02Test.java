package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.jackson.config.YudaoJacksonAutoConfiguration;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolOrderProcessCompletionDO;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** C02 RED contract: formal Long serialization must remain replayable by completion evidence matching. */
class MesTeamLeaderActiveOrderCompletionSourceEvidenceC02Test {

    private static final long LARGE_BATCH_RECORD_ID = 9_007_199_254_740_993L;
    private static final String FORMAL_SOURCE = JsonUtils.toJsonString(Map.of(
            "completions", List.of(Map.of("id", 301L, "completionStatus", "COMPLETED"))));
    private static final String LOSS_FACTS = JsonUtils.toJsonString(List.of(Map.of("processId", 1L)));

    private com.fasterxml.jackson.databind.ObjectMapper originalMapper;

    @BeforeEach
    void useFormalRuntimeSerializer() {
        originalMapper = JsonUtils.getObjectMapper();
        var configuration = new YudaoJacksonAutoConfiguration();
        configuration.jsonUtils(originalMapper.copy().registerModule(configuration.timestampSupportModuleBean()));
    }

    @AfterEach
    void restoreJsonRuntime() {
        JsonUtils.init(originalMapper);
    }

    @Test
    void formalWriterLargeBatchRecordIdMustMatchOnCompletionRetry() {
        var completion = MesProcessPoolOrderProcessCompletionDO.builder()
                .id(301L)
                .completionStatus("COMPLETED")
                .backfillStatus(MesProcessPoolOrderProcessCompletionDO.BACKFILL_STATUS_SUCCESS)
                .backfillExecutionId(LARGE_BATCH_RECORD_ID)
                .build();
        String liveSignature = JsonUtils.toJsonString(Map.of(
                "productionCompletionSignatures", List.of(completion)));
        JsonNode serializedId = JsonUtils.parseTree(liveSignature)
                .path("productionCompletionSignatures").path(0).path("backfillExecutionId");

        assertTrue(serializedId.isTextual(), "formal NumberSerializer must write the large Long as text");
        assertTrue(MesTeamLeaderActiveOrderCompletionSourceEvidence.matches(
                receipt(), draft(liveSignature)),
                "a formally serialized exact batch record ID must survive completion retry matching");
    }

    @Test
    void equivalentJsonReadbackMustMatchCompletionRetryEvidence() {
        String equivalentFormalSource = " { \"completions\" : [ { \"completionStatus\" : \"COMPLETED\","
                + " \"id\" : 301.0 } ] } ";
        String equivalentLossFacts = " [ { \"processId\" : 1.0 } ] ";

        assertTrue(MesTeamLeaderActiveOrderCompletionSourceEvidence.matches(
                receipt(9001L), draft(equivalentFormalSource, signatureWithToken("9001"),
                        equivalentLossFacts, 9001L)),
                "JSON key order, whitespace, and exact integral decimal formatting must not break replay");
    }

    @ParameterizedTest
    @ValueSource(strings = {"9007199254740993.5", "9223372036854775808", "-9223372036854775809"})
    void nonIntegralOrOverflowBatchRecordIdsMustRemainRejected(String jsonToken) {
        assertFalse(MesTeamLeaderActiveOrderCompletionSourceEvidence.matches(
                receipt(), draft(signatureWithToken(jsonToken))));
    }

    @Test
    void differentTextualBatchRecordIdMustRemainRejected() {
        assertFalse(MesTeamLeaderActiveOrderCompletionSourceEvidence.matches(
                receipt(), draft(signatureWithToken('"' + Long.toString(LARGE_BATCH_RECORD_ID - 1) + '"'))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"source", "signature", "loss"})
    void distinctExactDecimalFactsMustNotMatchOnReplay(String location) {
        var frozen = receipt(9001L);
        String frozenNumber = "0.123456789012345678901";
        String changedNumber = "0.123456789012345678902";
        String liveSource = FORMAL_SOURCE;
        String liveSignature = signatureWithToken("9001");
        String liveLoss = LOSS_FACTS;
        if ("source".equals(location)) {
            frozen.setFormalSourceSnapshotJson(FORMAL_SOURCE.replace("\"id\":301", "\"quantity\":" + frozenNumber + ",\"id\":301"));
            liveSource = FORMAL_SOURCE.replace("\"id\":301", "\"quantity\":" + changedNumber + ",\"id\":301");
        } else if ("signature".equals(location)) {
            frozen.setSignatureSnapshotJson(frozen.getSignatureSnapshotJson().replace("\"id\":301", "\"quantity\":" + frozenNumber + ",\"id\":301"));
            liveSignature = liveSignature.replace("\"id\":301", "\"quantity\":" + changedNumber + ",\"id\":301");
        } else {
            frozen.setLossConditionFactsJson("[{\"quantity\":" + frozenNumber + "}]");
            liveLoss = "[{\"quantity\":" + changedNumber + "}]";
        }
        frozen.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(frozen));
        assertFalse(MesTeamLeaderActiveOrderCompletionSourceEvidence.matches(
                frozen, draft(liveSource, liveSignature, liveLoss, 9001L)),
                "different exact decimal facts in " + location + " must not collapse during replay comparison");
    }

    @Test
    void distinctExactDecimalNestedZeroLossConfirmationMustNotMatchOnReplay() {
        var frozen = receipt(9001L);
        frozen.setLossConditionFactsJson(nestedConfirmationFacts("0.123456789012345678901"));
        String liveLoss = nestedConfirmationFacts("0.123456789012345678902");
        frozen.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(frozen));

        assertFalse(MesTeamLeaderActiveOrderCompletionSourceEvidence.matches(
                frozen, draft(FORMAL_SOURCE, signatureWithToken("9001"), liveLoss, 9001L)),
                "different exact decimals in nested zero-loss confirmation must not collapse during replay comparison");
    }

    @Test
    void equivalentTrailingZerosInNestedZeroLossConfirmationMustMatchOnReplay() {
        var frozen = receipt(9001L);
        frozen.setLossConditionFactsJson(nestedConfirmationFacts("0.1234500"));
        String liveLoss = nestedConfirmationFacts("0.12345");
        frozen.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(frozen));

        assertTrue(MesTeamLeaderActiveOrderCompletionSourceEvidence.matches(
                frozen, draft(FORMAL_SOURCE, signatureWithToken("9001"), liveLoss, 9001L)),
                "equivalent trailing zeros in nested zero-loss confirmation must match during replay");
    }

    private MesProcessPoolActiveOrderCompletionReceiptDO receipt() {
        return receipt(LARGE_BATCH_RECORD_ID);
    }

    private MesProcessPoolActiveOrderCompletionReceiptDO receipt(long batchRecordId) {
        var receipt = MesProcessPoolActiveOrderCompletionReceiptDO.builder()
                .activeOrderId(10L)
                .workOrderId(30L)
                .batchCode("C02-BATCH")
                .routeId(40L)
                .routeVersionId(41L)
                .leaderUserId(20L)
                .requestIdempotencyKey("c02-retry")
                .requestPayloadHash("payload")
                .sourceSnapshotHash("source")
                .formalSourceSnapshotJson(FORMAL_SOURCE)
                .signatureSnapshotJson("{\"productionCompletionSignatures\":[{\"id\":301,\"completionStatus\":\"COMPLETED\"}]}")
                .expectedVersion(2)
                .completedVersion(3)
                .receiptStatus(MesProcessPoolActiveOrderCompletionReceiptDO.RECEIPT_STATUS_BACKFILL_SUCCEEDED)
                .completionStatus(MesProcessPoolActiveOrderCompletionReceiptDO.STATUS_SUCCESS)
                .batchRecordStatus(MesProcessPoolActiveOrderCompletionReceiptDO.BACKFILL_STATUS_SUCCESS)
                .processInspectionStatus(MesProcessPoolActiveOrderCompletionReceiptDO.BACKFILL_STATUS_SUCCESS)
                .batchRecordId(batchRecordId)
                .processInspectionId(302L)
                .lossReportStatus(MesProcessPoolActiveOrderCompletionReceiptDO.LOSS_REPORT_STATUS_NOT_REQUIRED)
                .hasActualLoss(false)
                .lossQuantity(BigDecimal.ZERO)
                .lossConditionFactsJson(LOSS_FACTS)
                .build();
        receipt.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt));
        return receipt;
    }

    private MesTeamLeaderActiveOrderCompletionBackfillDraft draft(String signatureJson) {
        return draft(FORMAL_SOURCE, signatureJson, LOSS_FACTS, LARGE_BATCH_RECORD_ID);
    }

    private MesTeamLeaderActiveOrderCompletionBackfillDraft draft(String formalSourceJson,
                                                                  String signatureJson,
                                                                  String lossFactsJson,
                                                                  long batchRecordId) {
        return new MesTeamLeaderActiveOrderCompletionBackfillDraft()
                .setFormalSourceSnapshotJson(formalSourceJson)
                .setSignatureSnapshotJson(signatureJson)
                .setLossConditionFactsJson(lossFactsJson)
                .setBatchRecordId(batchRecordId);
    }

    private String signatureWithToken(String jsonToken) {
        return "{\"productionCompletionSignatures\":[{\"id\":301,"
                + "\"completionStatus\":\"COMPLETED\",\"backfillStatus\":\"SUCCESS\","
                + "\"backfillExecutionId\":" + jsonToken + ",\"backfillError\":null}]}";
    }

    private String nestedConfirmationFacts(String quantity) {
        String confirmation = "{\"quantity\":" + quantity + "}";
        return "[{\"zeroLossConfirmationSnapshot\":" + JsonUtils.toJsonString(confirmation) + "}]";
    }
}
