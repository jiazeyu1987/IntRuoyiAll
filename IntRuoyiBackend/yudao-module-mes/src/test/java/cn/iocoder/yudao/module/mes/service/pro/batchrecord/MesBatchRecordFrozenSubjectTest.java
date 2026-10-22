package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Byte-contract tests for the current V1 extraction, not V2 writing or formal signature validation. */
class MesBatchRecordFrozenSubjectTest {

    private static final String FULL_PAYLOAD =
            "900\nAPPROVE\npi-900\ntask-900\nnode-900\n审批节点\ncell-1\n0\n2\n"
                    + "SOURCE\n700\n正式来源\nAPPROVE\n3\nhead-hash\ncells-hash\nchallenge-hash";

    // Fixed V1 byte oracle: deliberately independent of the adapter's builder and JSON field sorting.
    private static final String FULL_BODY =
            "{\"adapter\":\"MES_BATCH_RECORD\",\"executionId\":\"900\",\"actionType\":\"APPROVE\","
                    + "\"processInstanceId\":\"pi-900\",\"bpmTaskId\":\"task-900\","
                    + "\"bpmTaskDefinitionKey\":\"node-900\",\"bpmTaskName\":\"审批节点\","
                    + "\"signatureCellKey\":\"cell-1\",\"signatureRowIndex\":\"0\","
                    + "\"signatureColumnIndex\":\"2\",\"reviewSourceType\":\"SOURCE\","
                    + "\"reviewSourceId\":\"700\",\"reviewSourceName\":\"正式来源\","
                    + "\"approvalResult\":\"APPROVE\",\"reason\":\"通过\","
                    + "\"fieldAuditRevision\":\"3\",\"fieldAuditHeadHash\":\"head-hash\","
                    + "\"cellValuesHash\":\"cells-hash\",\"signatureChallengeHash\":\"challenge-hash\"}";

    private static final String PRODUCTION_BODY =
            "{\"adapter\":\"MES_BATCH_RECORD\",\"executionId\":\"0\",\"actionType\":\"PRODUCTION_SUBMIT\","
                    + "\"processInstanceId\":\"\",\"bpmTaskId\":\"\",\"bpmTaskDefinitionKey\":\"\","
                    + "\"bpmTaskName\":\"\",\"signatureCellKey\":\"\",\"signatureRowIndex\":\"\","
                    + "\"signatureColumnIndex\":\"\",\"reviewSourceType\":\"\",\"reviewSourceId\":\"\","
                    + "\"reviewSourceName\":\"\",\"approvalResult\":\"\",\"reason\":\"生产提交\","
                    + "\"fieldAuditRevision\":\"\",\"fieldAuditHeadHash\":\"\",\"cellValuesHash\":\"\","
                    + "\"signatureChallengeHash\":\"\"}";

    private static final Set<String> BUSINESS_KEYS = Set.of(
            "adapter", "executionId", "actionType", "processInstanceId", "bpmTaskId",
            "bpmTaskDefinitionKey", "bpmTaskName", "signatureCellKey", "signatureRowIndex",
            "signatureColumnIndex", "reviewSourceType", "reviewSourceId", "reviewSourceName",
            "approvalResult", "reason", "fieldAuditRevision", "fieldAuditHeadHash",
            "cellValuesHash", "signatureChallengeHash");

    @Test
    void decode17ExposesEveryFrozenFieldInPublicRecord() {
        MesBatchRecordSignatureSubjectAdapter.DecodedSubject decoded =
                MesBatchRecordSignatureSubjectAdapter.decodeFrozenSubject(rawSubject(FULL_PAYLOAD));
        assertAll(
                () -> assertEquals("900", decoded.executionId()),
                () -> assertEquals("APPROVE", decoded.actionType()),
                () -> assertEquals("pi-900", decoded.processInstanceId()),
                () -> assertEquals("task-900", decoded.bpmTaskId()),
                () -> assertEquals("node-900", decoded.bpmTaskDefinitionKey()),
                () -> assertEquals("审批节点", decoded.bpmTaskName()),
                () -> assertEquals("cell-1", decoded.signatureCellKey()),
                () -> assertEquals("0", decoded.signatureRowIndex()),
                () -> assertEquals("2", decoded.signatureColumnIndex()),
                () -> assertEquals("SOURCE", decoded.reviewSourceType()),
                () -> assertEquals("700", decoded.reviewSourceId()),
                () -> assertEquals("正式来源", decoded.reviewSourceName()),
                () -> assertEquals("APPROVE", decoded.approvalResult()),
                () -> assertEquals("3", decoded.fieldAuditRevision()),
                () -> assertEquals("head-hash", decoded.fieldAuditHeadHash()),
                () -> assertEquals("cells-hash", decoded.cellValuesHash()),
                () -> assertEquals("challenge-hash", decoded.signatureChallengeHash()),
                () -> assertNull(decoded.opinionSource()));
    }

    @Test
    void canonical17MatchesFixedV1BytesAndExactStringKeys() {
        String body = MesBatchRecordSignatureSubjectAdapter.canonicalBusinessContent(
                rawSubject(FULL_PAYLOAD), "APPROVE", "通过");
        assertEquals(FULL_BODY, body);
        assertBusinessKeys(body, false);
    }

    @Test
    void productionZeroAndAllEmptySlotsMatchFixedV1Bytes() {
        String[] parts = new String[17];
        Arrays.fill(parts, "");
        parts[0] = "0";
        parts[1] = "PRODUCTION_SUBMIT";
        String subject = rawSubject(String.join("\n", parts));
        var decoded = MesBatchRecordSignatureSubjectAdapter.decodeFrozenSubject(subject);
        assertEquals("0", decoded.executionId());
        assertEquals("", decoded.processInstanceId());
        assertEquals("", decoded.signatureChallengeHash());
        assertEquals(PRODUCTION_BODY, MesBatchRecordSignatureSubjectAdapter.canonicalBusinessContent(
                subject, "PRODUCTION_SUBMIT", "生产提交"));
        assertBusinessKeys(PRODUCTION_BODY, false);
    }

    @Test
    void decoderPreservesTrailingEmptySeventeenthSlot() {
        String subject = changedPart(16, "");
        assertEquals(17, payload(subject).split("\n", -1).length);
        assertEquals("", MesBatchRecordSignatureSubjectAdapter.decodeFrozenSubject(subject).signatureChallengeHash());
        assertEquals(FULL_BODY.replace("\"signatureChallengeHash\":\"challenge-hash\"",
                        "\"signatureChallengeHash\":\"\""),
                MesBatchRecordSignatureSubjectAdapter.canonicalBusinessContent(subject, "APPROVE", "通过"));
    }

    @Test
    void encoderAndPureDecoderRoundTripOriginalNormalizedPayload() {
        String subject = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(
                900L, " APPROVE ", " pi-900 ", " task-900 ", " node-900 ", " 审批节点 ",
                " cell-1 ", 0, 2, " SOURCE ", 700L, " 正式来源 ", " APPROVE ",
                3L, " head-hash ", " cells-hash ", " challenge-hash ");
        assertEquals(FULL_PAYLOAD, payload(subject));
        assertEquals(rawSubject(FULL_PAYLOAD), subject);
        assertFalse(subject.contains("="));
        assertEquals("正式来源", MesBatchRecordSignatureSubjectAdapter.decodeFrozenSubject(subject).reviewSourceName());
        assertEquals(FULL_BODY, MesBatchRecordSignatureSubjectAdapter.canonicalBusinessContent(
                subject, "APPROVE", "通过"));
    }

    @Test
    void encoderNullsKeepAllOriginalEmptySlots() {
        String subject = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(
                0L, "PRODUCTION_SUBMIT", null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null);
        String[] expected = new String[17];
        Arrays.fill(expected, "");
        expected[0] = "0";
        expected[1] = "PRODUCTION_SUBMIT";
        assertEquals(String.join("\n", expected), payload(subject));
        assertEquals(PRODUCTION_BODY, MesBatchRecordSignatureSubjectAdapter.canonicalBusinessContent(
                subject, "PRODUCTION_SUBMIT", "生产提交"));
    }

    @Test
    void v1BodyPreservesTrimAndNullReasonEncodingAbility() {
        String subject = changedPart(5, "  审批节点  ");
        assertEquals("  审批节点  ",
                MesBatchRecordSignatureSubjectAdapter.decodeFrozenSubject(subject).bpmTaskName());
        assertEquals(FULL_BODY, MesBatchRecordSignatureSubjectAdapter.canonicalBusinessContent(
                subject, "APPROVE", "  通过  "));
        assertEquals(FULL_BODY.replace("\"reason\":\"通过\"", "\"reason\":\"\""),
                MesBatchRecordSignatureSubjectAdapter.canonicalBusinessContent(subject, "APPROVE", null));
        // Encoding ability is not authorization: the real signing command rejects blank reason.
    }

    @Test
    void bothUserSourceActionsPreserveFixedEscapedOpinionBytes() {
        String[][] cases = {
                {"普通中文", "普通中文"},
                {"第一行\n第二行", "第一行\\n第二行"},
                {"第一行\r\n第二行", "第一行\\r\\n第二行"},
                {"第一段\t第二段", "第一段\\t第二段"},
                {"意见\"引号\"\\原路径", "意见\\\"引号\\\"\\\\原路径"},
                {"APPROVE", "APPROVE"},
                {"PQC_RELEASE", "PQC_RELEASE"}
        };
        for (String action : List.of("APPROVE", "PQC_RELEASE")) {
            for (String[] opinion : cases) {
                String subject = withRawSource(action, "MES_OPINION_SOURCE_V1:USER");
                assertEquals("USER", MesBatchRecordSignatureSubjectAdapter.decodeFrozenSubject(subject).opinionSource());
                String body = MesBatchRecordSignatureSubjectAdapter.canonicalBusinessContent(
                        subject, action, opinion[0]);
                assertEquals(expectedOpinionBody(action, opinion[1], "USER"), body,
                        action + " / " + opinion[1]);
                assertEquals(opinion[0], JsonUtils.parseTree(body).path("reason").asText());
                assertEquals(opinion[0],
                        MesBatchRecordSignatureSubjectAdapter.verifiedUserOpinion(subject, action, opinion[0]));
                assertBusinessKeys(body, true);
            }
        }
    }

    @Test
    void bothSystemSourceActionsKeepActionReasonAndNoUserOpinion() {
        for (String action : List.of("APPROVE", "PQC_RELEASE")) {
            String subject = withRawSource(action, "MES_OPINION_SOURCE_V1:SYSTEM");
            assertEquals("SYSTEM", MesBatchRecordSignatureSubjectAdapter.decodeFrozenSubject(subject).opinionSource());
            assertEquals(expectedOpinionBody(action, action, "SYSTEM"),
                    MesBatchRecordSignatureSubjectAdapter.canonicalBusinessContent(subject, action, action));
            assertNull(MesBatchRecordSignatureSubjectAdapter.verifiedUserOpinion(subject, action, action));
            assertEquals(subject, MesBatchRecordSignatureSubjectAdapter.withOpinionSource(
                    rawSubject(payloadForAction(action)), null));
        }
    }

    @Test
    void identicalReasonWithDifferentSourceHasDifferentCompleteSubjectVersion() throws Exception {
        for (String action : List.of("APPROVE", "PQC_RELEASE")) {
            String legacy = rawSubject(payloadForAction(action));
            String user = withRawSource(action, "MES_OPINION_SOURCE_V1:USER");
            String system = withRawSource(action, "MES_OPINION_SOURCE_V1:SYSTEM");
            assertEquals(user, MesBatchRecordSignatureSubjectAdapter.withOpinionSource(legacy, action));
            assertNotEquals(user, system);
            assertNotEquals(MesBatchRecordSignatureSubjectAdapter.subjectVersion(user),
                    MesBatchRecordSignatureSubjectAdapter.subjectVersion(system));
            assertEquals(independentSha256(user), MesBatchRecordSignatureSubjectAdapter.subjectVersion(user));
            assertEquals(independentSha256(system), MesBatchRecordSignatureSubjectAdapter.subjectVersion(system));
            assertEquals(legacy, MesBatchRecordSignatureSubjectAdapter.subjectWithoutOpinionSource(user));
            assertEquals(legacy, MesBatchRecordSignatureSubjectAdapter.subjectWithoutOpinionSource(system));
            assertNotEquals(MesBatchRecordSignatureSubjectAdapter.subjectVersion(legacy),
                    MesBatchRecordSignatureSubjectAdapter.subjectVersion(user));
            assertNotEquals(MesBatchRecordSignatureSubjectAdapter.canonicalBusinessContent(user, action, action),
                    MesBatchRecordSignatureSubjectAdapter.canonicalBusinessContent(system, action, action));
        }
    }

    @Test
    void legacy17KeepsUnknownOpinionSourceEvenWhenReasonIsActionWord() {
        String subject = rawSubject(FULL_PAYLOAD);
        assertNull(MesBatchRecordSignatureSubjectAdapter.decodeFrozenSubject(subject).opinionSource());
        assertNull(MesBatchRecordSignatureSubjectAdapter.verifiedUserOpinion(subject, "APPROVE", "APPROVE"));
        assertEquals(subject, MesBatchRecordSignatureSubjectAdapter.subjectWithoutOpinionSource(subject));
        String body = MesBatchRecordSignatureSubjectAdapter.canonicalBusinessContent(subject, "APPROVE", "APPROVE");
        assertEquals(FULL_BODY.replace("\"reason\":\"通过\"", "\"reason\":\"APPROVE\""), body);
        assertBusinessKeys(body, false);
    }

    @Test
    void pureV1ExtractionDoesNotInventActionRegistrationGuardFor17Parts() {
        String subject = changedPart(1, "UNREGISTERED_OLD_VALUE");
        assertEquals("UNREGISTERED_OLD_VALUE",
                MesBatchRecordSignatureSubjectAdapter.decodeFrozenSubject(subject).actionType());
        assertEquals(FULL_BODY.replace("\"actionType\":\"APPROVE\"", "\"actionType\":\"UNREGISTERED_OLD_VALUE\""),
                MesBatchRecordSignatureSubjectAdapter.canonicalBusinessContent(
                        subject, "UNREGISTERED_OLD_VALUE", "通过"));
        // Registration and signing authority stay at their existing callers, not this V1 decoder.
    }

    @Test
    void legacy17BodyRetainsCommandActionOverrideEncodingBehavior() {
        assertEquals(FULL_BODY.replace("\"actionType\":\"APPROVE\"", "\"actionType\":\"REJECT\""),
                MesBatchRecordSignatureSubjectAdapter.canonicalBusinessContent(
                        rawSubject(FULL_PAYLOAD), "REJECT", "通过"));
        // Existing 17-part loadAndAuthorize does not add the 18-part opinion action guard.
    }

    @Test
    void invalidPartCountsKeepSubjectInvalidException() {
        for (int count : List.of(1, 16, 19)) {
            String[] parts = new String[count];
            Arrays.fill(parts, "x");
            String subject = rawSubject(String.join("\n", parts));
            assertFailure("MES_SIGNATURE_SUBJECT_INVALID",
                    () -> MesBatchRecordSignatureSubjectAdapter.decodeFrozenSubject(subject));
            assertFailure("MES_SIGNATURE_SUBJECT_INVALID",
                    () -> MesBatchRecordSignatureSubjectAdapter.canonicalBusinessContent(subject, "APPROVE", "通过"));
        }
    }

    @Test
    void emptyEighteenthSourceIsRejectedInsteadOfDowngradedTo17() {
        String subject = withRawSource("APPROVE", "");
        assertFailure("MES_SIGNATURE_OPINION_SOURCE_INVALID",
                () -> MesBatchRecordSignatureSubjectAdapter.decodeFrozenSubject(subject));
        assertFailure("MES_SIGNATURE_OPINION_SOURCE_INVALID",
                () -> MesBatchRecordSignatureSubjectAdapter.canonicalBusinessContent(subject, "APPROVE", "通过"));
    }

    @Test
    void wrongSourcePrefixValueCaseOrWhitespaceIsRejected() {
        for (String source : List.of("USER", "MES_OPINION_SOURCE_V2:USER",
                "MES_OPINION_SOURCE_V1:user", "MES_OPINION_SOURCE_V1:UNKNOWN",
                " MES_OPINION_SOURCE_V1:USER", "MES_OPINION_SOURCE_V1:SYSTEM ")) {
            String subject = withRawSource("APPROVE", source);
            assertFailure("MES_SIGNATURE_OPINION_SOURCE_INVALID",
                    () -> MesBatchRecordSignatureSubjectAdapter.decodeFrozenSubject(subject));
            assertFailure("MES_SIGNATURE_OPINION_SOURCE_INVALID",
                    () -> MesBatchRecordSignatureSubjectAdapter.canonicalBusinessContent(subject, "APPROVE", "通过"));
        }
    }

    @Test
    void eighteenthSourceOnOtherActionsIsRejected() {
        for (String action : List.of("SUBMIT", "REJECT", "REVIEW_APPROVE", "PRODUCTION_SUBMIT", "QA_DISPOSITION")) {
            String subject = withRawSource(action, "MES_OPINION_SOURCE_V1:USER");
            assertFailure("MES_SIGNATURE_OPINION_SOURCE_INVALID",
                    () -> MesBatchRecordSignatureSubjectAdapter.decodeFrozenSubject(subject));
            assertFailure("MES_SIGNATURE_OPINION_SOURCE_INVALID",
                    () -> MesBatchRecordSignatureSubjectAdapter.canonicalBusinessContent(subject, action, "通过"));
        }
    }

    @Test
    void opinion18ActionConflictIsRejected() {
        for (String action : List.of("APPROVE", "PQC_RELEASE")) {
            String subject = withRawSource(action, "MES_OPINION_SOURCE_V1:USER");
            assertFailure("MES_SIGNATURE_OPINION_ACTION_CONFLICT",
                    () -> MesBatchRecordSignatureSubjectAdapter.canonicalBusinessContent(subject, "REJECT", "通过"));
        }
    }

    @Test
    void user18NullEmptyOrBlankReasonIsRejected() {
        for (String action : List.of("APPROVE", "PQC_RELEASE")) {
            String subject = withRawSource(action, "MES_OPINION_SOURCE_V1:USER");
            for (String reason : Arrays.asList(null, "", " \t\r\n")) {
                assertFailure("MES_SIGNATURE_USER_OPINION_REQUIRED",
                        () -> MesBatchRecordSignatureSubjectAdapter.canonicalBusinessContent(subject, action, reason));
            }
        }
    }

    @Test
    void system18ReasonMustExactlyEqualActionBeforeV1Trim() {
        for (String action : List.of("APPROVE", "PQC_RELEASE")) {
            String subject = withRawSource(action, "MES_OPINION_SOURCE_V1:SYSTEM");
            for (String reason : Arrays.asList(null, "", " ", "人工意见", " " + action + " ")) {
                assertFailure("MES_SIGNATURE_SYSTEM_REASON_CONFLICT",
                        () -> MesBatchRecordSignatureSubjectAdapter.canonicalBusinessContent(subject, action, reason));
            }
        }
    }

    @Test
    void sourceAttachmentRejectsAlreadyExtendedAndNonOpinionSubjects() {
        assertFailure("MES_SIGNATURE_OPINION_SOURCE_INVALID",
                () -> MesBatchRecordSignatureSubjectAdapter.withOpinionSource(
                        withRawSource("APPROVE", "MES_OPINION_SOURCE_V1:USER"), "通过"));
        assertFailure("MES_SIGNATURE_OPINION_SOURCE_INVALID",
                () -> MesBatchRecordSignatureSubjectAdapter.withOpinionSource(changedPart(1, "REJECT"), "通过"));
    }

    @Test
    void malformedBase64KeepsDecoderFailure() {
        assertThrows(IllegalArgumentException.class,
                () -> MesBatchRecordSignatureSubjectAdapter.decodeFrozenSubject("%%%"));
        assertThrows(IllegalArgumentException.class,
                () -> MesBatchRecordSignatureSubjectAdapter.canonicalBusinessContent("%%%", "APPROVE", "通过"));
    }

    @Test
    void actualAdapterKeepsFixedBodyAndAllSnapshotAttributes() {
        String subject = rawSubject(FULL_PAYLOAD);
        var snapshot = new MesBatchRecordSignatureSubjectAdapter().loadAndAuthorize(
                new SignatureSubjectCommand(99L, "MES", "APPROVE", "MES_BATCH_RECORD",
                        subject, "ignored-input-version", "通过"));
        assertAll(
                () -> assertEquals(FULL_BODY, snapshot.canonicalContentJson()),
                () -> assertEquals(FULL_BODY,
                        MesBatchRecordSignatureSubjectAdapter.canonicalBusinessContent(subject, "APPROVE", "通过")),
                () -> assertEquals("MES_BATCH_RECORD", snapshot.subjectType()),
                () -> assertEquals(subject, snapshot.subjectId()),
                () -> assertEquals(MesBatchRecordSignatureSubjectAdapter.subjectVersion(subject), snapshot.subjectVersion()),
                () -> assertNull(snapshot.beforeContentJson()),
                () -> assertNull(snapshot.afterContentJson()),
                () -> assertNull(snapshot.fieldDiffJson()),
                () -> assertEquals("pi-900", snapshot.processInstanceId()),
                () -> assertEquals("task-900", snapshot.taskId()),
                () -> assertEquals("node-900", snapshot.nodeCode()),
                () -> assertEquals(Integer.valueOf(Math.abs("node-900".hashCode())), snapshot.nodeOrder()));
        for (String action : List.of("APPROVE", "PQC_RELEASE")) {
            for (String source : List.of("USER", "SYSTEM")) {
                String extended = withRawSource(action, "MES_OPINION_SOURCE_V1:" + source);
                var extendedSnapshot = new MesBatchRecordSignatureSubjectAdapter().loadAndAuthorize(
                        new SignatureSubjectCommand(99L, "MES", action, "MES_BATCH_RECORD", extended,
                                MesBatchRecordSignatureSubjectAdapter.subjectVersion(extended), action));
                assertEquals(expectedOpinionBody(action, action, source), extendedSnapshot.canonicalContentJson());
                assertEquals(extended, extendedSnapshot.subjectId());
                assertEquals(MesBatchRecordSignatureSubjectAdapter.subjectVersion(extended),
                        extendedSnapshot.subjectVersion());
            }
        }
    }

    private static void assertBusinessKeys(String body, boolean withSource) {
        var tree = JsonUtils.parseTree(body);
        Set<String> actual = new HashSet<>();
        tree.fieldNames().forEachRemaining(actual::add);
        Set<String> expected = new HashSet<>(BUSINESS_KEYS);
        if (withSource) expected.add("opinionSource");
        assertEquals(expected, actual);
        tree.forEach(value -> assertTrue(value.isTextual()));
        for (String metadata : List.of("evidenceFormatVersion", "commandHashVersion", "signatureIdentity",
                "signatureTime", "signatureImage", "nodePosition")) {
            assertFalse(tree.has(metadata));
        }
    }

    private static String expectedOpinionBody(String action, String escapedReason, String source) {
        String expected = FULL_BODY.replace("\"actionType\":\"APPROVE\"", "\"actionType\":\"" + action + "\"")
                .replace("\"reason\":\"通过\"", "\"reason\":\"" + escapedReason + "\"");
        return expected.substring(0, expected.length() - 1) + ",\"opinionSource\":\"" + source + "\"}";
    }

    private static String rawSubject(String payload) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    private static String payload(String subject) {
        return new String(Base64.getUrlDecoder().decode(subject), StandardCharsets.UTF_8);
    }

    private static String payloadForAction(String action) {
        String[] parts = FULL_PAYLOAD.split("\n", -1);
        parts[1] = action;
        return String.join("\n", parts);
    }

    private static String changedPart(int index, String value) {
        String[] parts = FULL_PAYLOAD.split("\n", -1);
        parts[index] = value;
        return rawSubject(String.join("\n", parts));
    }

    private static String withRawSource(String action, String source) {
        return rawSubject(payloadForAction(action) + "\n" + source);
    }

    private static String independentSha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static void assertFailure(String expectedMessage, org.junit.jupiter.api.function.Executable call) {
        assertEquals(expectedMessage, assertThrows(IllegalArgumentException.class, call).getMessage());
    }
}
