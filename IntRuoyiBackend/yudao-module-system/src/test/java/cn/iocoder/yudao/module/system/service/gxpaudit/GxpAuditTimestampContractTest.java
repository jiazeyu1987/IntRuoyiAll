package cn.iocoder.yudao.module.system.service.gxpaudit;

import cn.iocoder.yudao.framework.common.util.json.databind.TimestampLocalDateTimeSerializer;
import cn.iocoder.yudao.module.system.controller.admin.gxpaudit.vo.GxpAuditEventPageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.gxpaudit.vo.GxpAuditEventRelationRespVO;
import cn.iocoder.yudao.module.system.controller.admin.gxpaudit.vo.GxpAuditEventRespVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

class GxpAuditTimestampContractTest {

    private static final LocalDateTime UTC_ACTION = LocalDateTime.of(2026, 10, 1, 12, 35, 38, 830_000_000);
    private static final long ACTION_EPOCH = 1790858138830L;

    @Test
    void ledgerResponseAndRelationsUseUtcOnDifferentHostZonesWithoutChangingEvidence() throws Exception {
        TimeZone original = TimeZone.getDefault();
        try {
            for (String zone : List.of("UTC", "Asia/Shanghai")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                GxpAuditEventRespVO response = new GxpAuditEventRespVO()
                        .setServerOccurredAt(UTC_ACTION)
                        .setEventHash("existing-ledger-hash")
                        .setCanonicalEventJson("{\"serverOccurredAt\":\"2026-10-01T12:35:38.830\"}")
                        .setRelations(List.of(new GxpAuditEventRelationRespVO().setCreatedAtUtc(UTC_ACTION)));
                JsonNode json = mapper().readTree(mapper().writeValueAsString(response));
                assertEquals(ACTION_EPOCH, json.get("serverOccurredAt").longValue(), zone);
                assertEquals(ACTION_EPOCH, json.get("relations").get(0).get("createdAtUtc").longValue(), zone);
                assertEquals("existing-ledger-hash", json.get("eventHash").textValue());
                assertEquals(response.getCanonicalEventJson(), json.get("canonicalEventJson").textValue());
                assertEquals(UTC_ACTION, response.getServerOccurredAt());

                LocalBusinessTime business = new LocalBusinessTime();
                business.signedAt = UTC_ACTION;
                long localEpoch = mapper().readTree(mapper().writeValueAsString(business))
                        .get("signedAt").longValue();
                assertEquals(UTC_ACTION.atZone(TimeZone.getDefault().toZoneId()).toInstant().toEpochMilli(),
                        localEpoch, "ordinary local business serializer must remain unchanged");
            }
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    void localBrowserRangeHasTheSameUtcMeaningOnAllHostsIncludingPreviousDayBoundary() {
        // Browser selection 20:00-21:00 at UTC+8 is transported as instants, not ambiguous local strings.
        Long[] browserRange = {LocalDateTime.of(2026, 10, 1, 20, 0).toInstant(ZoneOffset.ofHours(8)).toEpochMilli(),
                LocalDateTime.of(2026, 10, 1, 21, 0).toInstant(ZoneOffset.ofHours(8)).toEpochMilli()};
        GxpAuditEventPageReqVO request = new GxpAuditEventPageReqVO().setOccurredAt(browserRange);
        assertArrayEquals(new LocalDateTime[]{LocalDateTime.of(2026, 10, 1, 12, 0),
                LocalDateTime.of(2026, 10, 1, 13, 0)}, request.toUtcOccurredAt());
        assertTrue(!UTC_ACTION.isBefore(request.toUtcOccurredAt()[0])
                && !UTC_ACTION.isAfter(request.toUtcOccurredAt()[1]));

        request.setOccurredAt(new Long[]{LocalDateTime.of(2026, 10, 2, 0, 0)
                .toInstant(ZoneOffset.ofHours(8)).toEpochMilli(),
                LocalDateTime.of(2026, 10, 2, 0, 30).toInstant(ZoneOffset.ofHours(8)).toEpochMilli()});
        assertArrayEquals(new LocalDateTime[]{LocalDateTime.of(2026, 10, 1, 16, 0),
                LocalDateTime.of(2026, 10, 1, 16, 30)}, request.toUtcOccurredAt());
    }

    @Test
    void missingRangeIsUnfilteredAndInvalidRangesAreRejectedByValidationAndConversion() {
        GxpAuditEventPageReqVO request = new GxpAuditEventPageReqVO();
        assertNull(request.toUtcOccurredAt());
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validate(request).isEmpty());
            for (Long[] invalid : List.of(new Long[0], new Long[]{ACTION_EPOCH},
                    new Long[]{ACTION_EPOCH, null}, new Long[]{ACTION_EPOCH + 1, ACTION_EPOCH},
                    new Long[]{Long.MIN_VALUE, Long.MAX_VALUE})) {
                request.setOccurredAt(invalid);
                assertFalse(validator.validate(request).isEmpty());
                assertThrows(IllegalArgumentException.class, request::toUtcOccurredAt);
            }
            request.setOccurredAt(new Long[]{ACTION_EPOCH, ACTION_EPOCH});
            assertTrue(validator.validate(request).isEmpty());
            assertArrayEquals(new LocalDateTime[]{UTC_ACTION, UTC_ACTION}, request.toUtcOccurredAt());
        }
    }

    private static ObjectMapper mapper() {
        JavaTimeModule times = new JavaTimeModule();
        times.addSerializer(LocalDateTime.class, TimestampLocalDateTimeSerializer.INSTANCE);
        return new ObjectMapper().registerModule(times);
    }

    public static class LocalBusinessTime {
        public LocalDateTime signedAt;
    }
}
