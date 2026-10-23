package cn.iocoder.yudao.module.dcc.service.token;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.stream.Stream;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real HMAC and JSON source-claim roundtrip; no implicit key or storage-content hash. */
class DccViewerTokenSourceBoundTest {
    private final DccViewerTokenService tokens = new DccViewerTokenService();
    private final DccPreviewSourceIdentity source = new DccPreviewSourceIdentity(100L, 1L,
            "r3/source.pdf", "source.pdf", "application/pdf", 10L);
    private final DccViewerTokenIssueCommand command = new DccViewerTokenIssueCommand(
            31L, 99L, 910L, "A/2", 800L, "r3-exact-context-purpose", 900L);
    private static final Instant ISSUED = Instant.parse("2026-10-15T03:00:00Z");

    @BeforeEach void explicitSigningKeyAndClock() {
        ReflectionTestUtils.setField(tokens, "hmacSecret", "r3-test-explicit-hmac-secret-at-least-32-characters");
        ReflectionTestUtils.setField(tokens, "clock", Clock.fixed(ISSUED, ZoneOffset.UTC));
    }

    @Test void actualSignedSourceClaimRoundtripsAndPreservesOpaqueNonce() {
        var issued = tokens.issueSourceBound(command, source);
        var verified = tokens.verifySourceBound(issued.token(), expected(issued), source);
        assertEquals(source, verified.getPreviewSource());
        assertEquals(issued.payload().getNonce(), verified.getNonce());
        assertEquals(issued.payload().getTokenId(), verified.getTokenId());
    }

    static Stream<DccPreviewSourceIdentity> changedSources() {
        return Stream.of(new DccPreviewSourceIdentity(200L, 1L, "r3/source.pdf", "source.pdf", "application/pdf", 10L),
                new DccPreviewSourceIdentity(100L, 2L, "r3/source.pdf", "source.pdf", "application/pdf", 10L),
                new DccPreviewSourceIdentity(100L, 1L, "r3/other.pdf", "source.pdf", "application/pdf", 10L),
                new DccPreviewSourceIdentity(100L, 1L, "r3/source.pdf", "other.pdf", "application/pdf", 10L),
                new DccPreviewSourceIdentity(100L, 1L, "r3/source.pdf", "source.pdf", "image/png", 10L),
                new DccPreviewSourceIdentity(100L, 1L, "r3/source.pdf", "source.pdf", "application/pdf", 11L),
                new DccPreviewSourceIdentity(100L, 1L, "r3/source.pdf", null, "application/pdf", 10L),
                new DccPreviewSourceIdentity(100L, 1L, "r3/source.pdf", "source.pdf", null, 10L));
    }

    static Stream<DccPreviewSourceIdentity> nullableSources() {
        return Stream.of(new DccPreviewSourceIdentity(100L, 1L, "r3/source.pdf", null, "application/pdf", 10L),
                new DccPreviewSourceIdentity(100L, 1L, "r3/source.pdf", "source.pdf", null, 10L),
                new DccPreviewSourceIdentity(100L, 1L, "r3/source.pdf", null, null, 10L));
    }

    @ParameterizedTest @MethodSource("nullableSources")
    void persistedNullableDescriptionsRoundtripWithoutSubstitution(DccPreviewSourceIdentity nullable) {
        var issued = tokens.issueSourceBound(command, nullable);
        var verified = tokens.verifySourceBound(issued.token(), expected(issued), nullable).getPreviewSource();
        assertEquals(nullable, verified);
        assertEquals(nullable.name(), verified.name()); assertEquals(nullable.contentType(), verified.contentType());
    }

    @ParameterizedTest @MethodSource("changedSources")
    void eachPersistedSourceFieldIsPartOfTheSignedCapability(DccPreviewSourceIdentity changed) {
        var issued = tokens.issueSourceBound(command, source);
        assertServiceException(() -> tokens.verifySourceBound(issued.token(), expected(issued), changed),
                CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
    }

    @Test void changedClaimWithoutSignatureCannotPassEvenWithTheChangedExpectedSource() {
        var issued = tokens.issueSourceBound(command, source);
        var pieces = issued.token().split("\\.", -1);
        var json = new String(Base64.getUrlDecoder().decode(pieces[0]), StandardCharsets.UTF_8);
        var changed = json.replace("r3/source.pdf", "r3/forged.pdf");
        assertNotEquals(json, changed);
        var tampered = Base64.getUrlEncoder().withoutPadding().encodeToString(changed.getBytes(StandardCharsets.UTF_8)) + "." + pieces[1];
        var expectedSource = new DccPreviewSourceIdentity(100L, 1L, "r3/forged.pdf", "source.pdf", "application/pdf", 10L);
        assertServiceException(() -> tokens.verifySourceBound(tampered, expected(issued), expectedSource),
                CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
    }

    @Test void signedLegacyTokenIsValidForLegacyUseButMissingSourceCannotBeUpgraded() {
        var issued = tokens.issue(command);
        assertNotNull(tokens.verify(issued.token(), expected(issued)));
        assertServiceException(() -> tokens.verifySourceBound(issued.token(), expected(issued), source),
                CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
    }

    @Test void sourceBoundCapabilityCannotUseTheLegacyEntryThatOmitsSourceChecks() {
        var issued = tokens.issueSourceBound(command, source);
        assertServiceException(() -> tokens.verify(issued.token(), expected(issued)),
                CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
    }

    @Test void sourceBoundTokenStillRequiresUnexpiredCapability() {
        var issued = tokens.issueSourceBound(command, source);
        ReflectionTestUtils.setField(tokens, "clock", Clock.fixed(ISSUED.plusSeconds(900), ZoneOffset.UTC));
        assertServiceException(() -> tokens.verifySourceBound(issued.token(), expected(issued), source),
                CONTROLLED_FILE_VIEWER_TOKEN_EXPIRED);
    }

    @Test void missingPersistedSourceFactsNeverBecomeDefaults() {
        assertThrows(IllegalArgumentException.class, () -> new DccPreviewSourceIdentity(100L, null,
                "r3/source.pdf", "source.pdf", "application/pdf", 10L));
        assertThrows(IllegalArgumentException.class, () -> new DccPreviewSourceIdentity(100L, 1L,
                "r3/source.pdf", "source.pdf", "application/pdf", null));
        assertThrows(IllegalArgumentException.class, () -> new DccPreviewSourceIdentity(100L, 1L,
                " ", "source.pdf", "application/pdf", 10L));
        assertThrows(NullPointerException.class, () -> tokens.issueSourceBound(command, null));
    }

    private DccViewerTokenExpectedContext expected(DccIssuedViewerToken issued) {
        return new DccViewerTokenExpectedContext(command.tenantId(), command.userId(), command.fileId(), command.versionId(),
                command.accessEventId(), command.purpose(), command.ttlSeconds(), issued.payload().getNonce(), issued.payload().getTokenId());
    }
}
