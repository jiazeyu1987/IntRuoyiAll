package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.*;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/** Actual JSON and BeanValidation transport contract; server-source HMAC is a separate workflow gate. */
class DccNativeSupervisorBoundaryTest {
    private static final String DIGEST = "a".repeat(64);
    private static final String TOKEN = "server-source-payload.signature";
    private final List<Class<?>> requests = List.of(DccControlledFileSubmitReqVO.class,
            DccControlledFileSubmitIterationReqVO.class, DccWorkingApplicationSaveReqVO.class);

    private boolean validSourcePair(Object value) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            return factory.getValidator().validate(value).stream().noneMatch(violation ->
                    violation.getPropertyPath().toString().equals("supervisorSourceExpectationComplete"));
        }
    }
    @Test void completeSourceExpectationRoundTripsInAllOriginalRequestsWithoutBecomingAuthority() {
        String json = "{\"expectedSupervisorSourceDigest\":\"" + DIGEST
                + "\",\"supervisorSourceToken\":\"" + TOKEN + "\"}";
        for (var type : requests) {
            Object value = JsonUtils.parseObject(json, type);
            var encoded = JsonUtils.parseObject(JsonUtils.toJsonString(value), Map.class);
            assertEquals(DIGEST, encoded.get("expectedSupervisorSourceDigest"), type.getSimpleName());
            assertEquals(TOKEN, encoded.get("supervisorSourceToken"), type.getSimpleName());
            assertTrue(validSourcePair(value));
            assertFalse(encoded.containsKey("supervisorSourceExpectationComplete"));
            assertFalse(encoded.containsKey("supervisorUserId"));
        }
    }
    @Test void plainDraftLegacyAndExternalMayOmitTheWholeSourceExpectation() {
        for (var type : requests) assertTrue(validSourcePair(JsonUtils.parseObject("{}", type)));
    }
    @Test void partialMalformedOrUnboundedSourceExpectationsCannotPassTheTransportGate() {
        List<String> invalid = List.of(
                "{\"expectedSupervisorSourceDigest\":\"" + DIGEST + "\"}",
                "{\"supervisorSourceToken\":\"" + TOKEN + "\"}",
                "{\"expectedSupervisorSourceDigest\":\"bad\",\"supervisorSourceToken\":\"" + TOKEN + "\"}",
                "{\"expectedSupervisorSourceDigest\":\"" + DIGEST.toUpperCase()
                        + "\",\"supervisorSourceToken\":\"" + TOKEN + "\"}",
                "{\"expectedSupervisorSourceDigest\":\"" + DIGEST + "\",\"supervisorSourceToken\":\" \"}",
                "{\"expectedSupervisorSourceDigest\":\"" + DIGEST + "\",\"supervisorSourceToken\":\" " + TOKEN + "\"}",
                "{\"expectedSupervisorSourceDigest\":\"" + DIGEST + "\",\"supervisorSourceToken\":\"x\\ny\"}",
                "{\"expectedSupervisorSourceDigest\":\"" + DIGEST + "\",\"supervisorSourceToken\":\""
                        + "x".repeat(131073) + "\"}");
        for (var type : requests) for (String json : invalid)
            assertFalse(validSourcePair(JsonUtils.parseObject(json, type)), type.getSimpleName());
    }
    @Test void readinessReturnsTheActualOptionalSourcePairAndDoesNotInventAnAbsentSource() throws Exception {
        var value = DccControlledFileRouteReadinessRespVO.builder().ready(true)
                .executionContract("PUBLIC_MATRIX_NATIVE_V1").build();
        value.getClass().getMethod("setSupervisorSourceDigest", String.class).invoke(value, DIGEST);
        value.getClass().getMethod("setSupervisorSourceToken", String.class).invoke(value, TOKEN);
        var encoded = JsonUtils.parseObject(JsonUtils.toJsonString(value), Map.class);
        assertEquals(DIGEST, encoded.get("supervisorSourceDigest"));
        assertEquals(TOKEN, encoded.get("supervisorSourceToken"));
        var absent = DccControlledFileRouteReadinessRespVO.builder().ready(false).build();
        var absentJson = JsonUtils.parseObject(JsonUtils.toJsonString(absent), Map.class);
        assertNull(absentJson.get("supervisorSourceDigest"));
        assertNull(absentJson.get("supervisorSourceToken"));
    }
}
