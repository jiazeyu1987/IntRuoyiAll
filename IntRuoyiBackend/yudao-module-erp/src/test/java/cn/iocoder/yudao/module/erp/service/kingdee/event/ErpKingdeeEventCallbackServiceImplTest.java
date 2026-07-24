package cn.iocoder.yudao.module.erp.service.kingdee.event;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.erp.dal.dataobject.kingdee.event.ErpKingdeeEventCallbackDO;
import cn.iocoder.yudao.module.erp.dal.mysql.kingdee.event.ErpKingdeeEventCallbackMapper;
import cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(ErpKingdeeEventCallbackServiceImpl.class)
class ErpKingdeeEventCallbackServiceImplTest extends BaseDbUnitTest {

    private static final String SECRET = "unit-test-kingdee-callback-secret";
    private static final String TIMESTAMP = "2026-06-17T20:30:00";
    private static final String NONCE = "nonce-001";

    @Resource
    private ErpKingdeeEventCallbackService callbackService;
    @Resource
    private ErpKingdeeEventCallbackMapper callbackMapper;

    @Test
    void receive_validSignature_shouldPersistPendingEvent() throws Exception {
        String rawBody = buildPayload("evt-001", "PRD_MO", "10001", "MO20260617001", "Audit",
                "2026-06-17T20:29:58");

        ErpKingdeeEventCallbackResult result = callbackService.receive(rawBody, sign(rawBody), TIMESTAMP, NONCE);

        assertFalse(result.isDuplicate());
        assertEquals("accepted", result.getStatus());
        assertEquals("PRD_MO:evt-001", result.getEventKey());

        ErpKingdeeEventCallbackDO persisted = callbackMapper.selectByEventKey("PRD_MO:evt-001");
        assertNotNull(persisted);
        assertEquals("PRD_MO", persisted.getSourceFormId());
        assertEquals("10001", persisted.getSourceFid());
        assertEquals("MO20260617001", persisted.getSourceBillNo());
        assertEquals("Audit", persisted.getOperation());
        assertEquals(LocalDateTime.of(2026, 6, 17, 20, 29, 58), persisted.getEventTime());
        assertEquals(ErpKingdeeEventCallbackDO.STATUS_PENDING, persisted.getStatus());
        assertEquals(rawBody, persisted.getRawPayload());
    }

    @Test
    void receive_duplicateEvent_shouldReturnDuplicateWithoutSecondInsert() throws Exception {
        String rawBody = buildPayload("evt-duplicate", "PRD_PPBOM", "20001", "PPBOM20260617001", "Save",
                "2026-06-17T20:31:00");

        callbackService.receive(rawBody, sign(rawBody), TIMESTAMP, NONCE);
        ErpKingdeeEventCallbackResult duplicate = callbackService.receive(rawBody, sign(rawBody), TIMESTAMP, NONCE);

        assertTrue(duplicate.isDuplicate());
        assertEquals("duplicate", duplicate.getStatus());
        assertEquals("PRD_PPBOM:evt-duplicate", duplicate.getEventKey());
        assertEquals(1L, callbackMapper.selectCount());
    }

    @Test
    void receive_invalidSignature_shouldRejectAndNotPersist() throws Exception {
        String rawBody = buildPayload("evt-invalid-signature", "ENG_BOM", "30001", "BOM20260617001", "Submit",
                "2026-06-17T20:32:00");

        assertServiceException(() -> callbackService.receive(rawBody, "bad-signature", TIMESTAMP, NONCE),
                ErrorCodeConstants.KINGDEE_EVENT_CALLBACK_SIGNATURE_INVALID);
        assertEquals(0L, callbackMapper.selectCount());
    }

    @Test
    void receive_missingBusinessKey_shouldRejectAndNotPersist() throws Exception {
        String rawBody = buildPayload("evt-missing-business-key", "PRD_MO", "", "", "Audit",
                "2026-06-17T20:33:00");

        assertServiceException(() -> callbackService.receive(rawBody, sign(rawBody), TIMESTAMP, NONCE),
                ErrorCodeConstants.KINGDEE_EVENT_CALLBACK_FIELD_MISSING, "sourceFid or billNo");
        assertEquals(0L, callbackMapper.selectCount());
    }

    private static String buildPayload(String eventId, String formId, String fid, String billNo, String operation,
                                       String eventTime) {
        return """
                {"eventId":"%s","formId":"%s","sourceFid":"%s","billNo":"%s","operation":"%s","eventTime":"%s"}
                """.formatted(eventId, formId, fid, billNo, operation, eventTime).trim();
    }

    private static String sign(String rawBody) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal((TIMESTAMP + "\n" + NONCE + "\n" + rawBody).getBytes(StandardCharsets.UTF_8));
        return toHex(digest);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte item : bytes) {
            result.append(String.format("%02x", item));
        }
        return result.toString();
    }

}
