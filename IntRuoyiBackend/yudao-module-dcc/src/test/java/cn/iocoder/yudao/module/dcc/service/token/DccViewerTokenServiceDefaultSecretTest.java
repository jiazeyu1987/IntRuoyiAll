package cn.iocoder.yudao.module.dcc.service.token;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DccViewerTokenServiceDefaultSecretTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-02T03:30:00Z"), ZoneOffset.UTC);

    @Test
    void issueAndVerify_useBuiltInSecretWhenConfigMissing() {
        DccViewerTokenService service = new DccViewerTokenService();
        ReflectionTestUtils.setField(service, "clock", FIXED_CLOCK);
        ReflectionTestUtils.setField(service, "hmacSecret", "");

        DccIssuedViewerToken issued = assertDoesNotThrow(() -> service.issue(new DccViewerTokenIssueCommand(
                31L, 2001L, 1001L, "V1.0", 333L, "CONTROLLED_PREVIEW", 300L)));

        DccViewerTokenPayload payload = assertDoesNotThrow(() -> service.verify(issued.token(),
                new DccViewerTokenExpectedContext(31L, 2001L, 1001L, "V1.0", 333L,
                        "CONTROLLED_PREVIEW", 300L, issued.payload().getNonce(), issued.payload().getTokenId())));
        assertEquals(31L, payload.getTenantId());
        assertEquals(2001L, payload.getUserId());
        assertEquals(1001L, payload.getFileId());
    }

}
