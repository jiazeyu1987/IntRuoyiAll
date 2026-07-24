package cn.iocoder.yudao.module.erp.service.kingdee.event;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.erp.dal.dataobject.kingdee.event.ErpKingdeeEventCallbackDO;
import cn.iocoder.yudao.module.erp.dal.mysql.kingdee.event.ErpKingdeeEventCallbackMapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_EVENT_CALLBACK_FIELD_MISSING;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_EVENT_CALLBACK_PAYLOAD_INVALID;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_EVENT_CALLBACK_SECRET_MISSING;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_EVENT_CALLBACK_SIGNATURE_INVALID;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_EVENT_CALLBACK_SIGNATURE_MISSING;

@Service
public class ErpKingdeeEventCallbackServiceImpl implements ErpKingdeeEventCallbackService {

    private static final String SIGN_ALGORITHM = "HmacSHA256";
    private static final int EVENT_KEY_MAX_LENGTH = 191;

    private final ErpKingdeeEventCallbackMapper callbackMapper;
    private final String callbackSecret;

    public ErpKingdeeEventCallbackServiceImpl(
            ErpKingdeeEventCallbackMapper callbackMapper,
            @Value("${yudao.erp.kingdee.event-callback-secret:}") String callbackSecret) {
        this.callbackMapper = callbackMapper;
        this.callbackSecret = callbackSecret;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpKingdeeEventCallbackResult receive(String rawBody, String signature, String timestamp, String nonce) {
        validateSignature(rawBody, signature, timestamp, nonce);
        EventPayload payload = parsePayload(rawBody);
        NormalizedEvent normalizedEvent = normalize(payload);

        ErpKingdeeEventCallbackDO existing = callbackMapper.selectByEventKey(normalizedEvent.eventKey);
        if (existing != null) {
            return ErpKingdeeEventCallbackResult.duplicate(existing.getId(), existing.getEventKey(), existing.getEventId());
        }

        ErpKingdeeEventCallbackDO event = buildEvent(rawBody, signature, timestamp, nonce, normalizedEvent);
        try {
            callbackMapper.insert(event);
        } catch (DuplicateKeyException ex) {
            ErpKingdeeEventCallbackDO duplicated = callbackMapper.selectByEventKey(normalizedEvent.eventKey);
            if (duplicated != null) {
                return ErpKingdeeEventCallbackResult.duplicate(
                        duplicated.getId(), duplicated.getEventKey(), duplicated.getEventId());
            }
            throw ex;
        }
        return ErpKingdeeEventCallbackResult.accepted(event.getId(), event.getEventKey(), event.getEventId());
    }

    private void validateSignature(String rawBody, String signature, String timestamp, String nonce) {
        if (StrUtil.isBlank(callbackSecret)) {
            throw exception(KINGDEE_EVENT_CALLBACK_SECRET_MISSING, "yudao.erp.kingdee.event-callback-secret");
        }
        if (StrUtil.isBlank(rawBody)) {
            throw exception(KINGDEE_EVENT_CALLBACK_PAYLOAD_INVALID, "request body is blank");
        }
        if (StrUtil.isBlank(signature)) {
            throw exception(KINGDEE_EVENT_CALLBACK_SIGNATURE_MISSING, HEADER_SIGNATURE);
        }
        if (StrUtil.isBlank(timestamp)) {
            throw exception(KINGDEE_EVENT_CALLBACK_SIGNATURE_MISSING, HEADER_TIMESTAMP);
        }
        if (StrUtil.isBlank(nonce)) {
            throw exception(KINGDEE_EVENT_CALLBACK_SIGNATURE_MISSING, HEADER_NONCE);
        }
        String expected = hmacSha256Hex(timestamp + "\n" + nonce + "\n" + rawBody, callbackSecret);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                signature.trim().getBytes(StandardCharsets.UTF_8))) {
            throw exception(KINGDEE_EVENT_CALLBACK_SIGNATURE_INVALID);
        }
    }

    private EventPayload parsePayload(String rawBody) {
        try {
            EventPayload payload = JsonUtils.parseObject(rawBody, EventPayload.class);
            if (payload == null) {
                throw exception(KINGDEE_EVENT_CALLBACK_PAYLOAD_INVALID, "request body is blank");
            }
            return payload;
        } catch (RuntimeException ex) {
            throw exception(KINGDEE_EVENT_CALLBACK_PAYLOAD_INVALID, ex.getMessage());
        }
    }

    private NormalizedEvent normalize(EventPayload payload) {
        String formId = trimToNull(payload.getFormId());
        if (formId == null) {
            throw exception(KINGDEE_EVENT_CALLBACK_FIELD_MISSING, "formId");
        }
        String operation = trimToNull(payload.getOperation());
        if (operation == null) {
            throw exception(KINGDEE_EVENT_CALLBACK_FIELD_MISSING, "operation");
        }
        String sourceFid = firstNotBlank(payload.getSourceFid(), payload.getFid());
        String billNo = trimToNull(payload.getBillNo());
        if (sourceFid == null && billNo == null) {
            throw exception(KINGDEE_EVENT_CALLBACK_FIELD_MISSING, "sourceFid or billNo");
        }
        LocalDateTime eventTime = parseEventTime(payload.getEventTime());
        String eventId = trimToNull(payload.getEventId());
        String eventKey = buildEventKey(formId, eventId, sourceFid, billNo, operation, eventTime);
        if (eventKey.length() > EVENT_KEY_MAX_LENGTH) {
            throw exception(KINGDEE_EVENT_CALLBACK_PAYLOAD_INVALID, "eventKey exceeds " + EVENT_KEY_MAX_LENGTH);
        }
        return new NormalizedEvent(eventKey, eventId, formId, sourceFid, billNo, operation, eventTime);
    }

    private static LocalDateTime parseEventTime(String eventTime) {
        String normalizedEventTime = trimToNull(eventTime);
        if (normalizedEventTime == null) {
            throw exception(KINGDEE_EVENT_CALLBACK_FIELD_MISSING, "eventTime");
        }
        try {
            return LocalDateTime.parse(normalizedEventTime);
        } catch (DateTimeParseException ex) {
            throw exception(KINGDEE_EVENT_CALLBACK_PAYLOAD_INVALID, "eventTime");
        }
    }

    private static String buildEventKey(String formId, String eventId, String sourceFid, String billNo,
                                        String operation, LocalDateTime eventTime) {
        if (eventId != null) {
            return formId + ":" + eventId;
        }
        return formId + ":" + StrUtil.blankToDefault(sourceFid, "-") + ":" + StrUtil.blankToDefault(billNo, "-")
                + ":" + operation + ":" + eventTime;
    }

    private static ErpKingdeeEventCallbackDO buildEvent(String rawBody, String signature, String timestamp, String nonce,
                                                       NormalizedEvent normalizedEvent) {
        return ErpKingdeeEventCallbackDO.builder()
                .eventKey(normalizedEvent.eventKey)
                .eventId(normalizedEvent.eventId)
                .sourceFormId(normalizedEvent.formId)
                .sourceFid(normalizedEvent.sourceFid)
                .sourceBillNo(normalizedEvent.billNo)
                .operation(normalizedEvent.operation)
                .eventTime(normalizedEvent.eventTime)
                .signature(signature.trim())
                .nonce(nonce.trim())
                .callbackTimestamp(timestamp.trim())
                .status(ErpKingdeeEventCallbackDO.STATUS_PENDING)
                .rawPayload(rawBody)
                .build();
    }

    private static String hmacSha256Hex(String text, String secret) {
        try {
            Mac mac = Mac.getInstance(SIGN_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), SIGN_ALGORITHM));
            byte[] digest = mac.doFinal(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable", ex);
        }
    }

    private static String firstNotBlank(String first, String second) {
        String normalizedFirst = trimToNull(first);
        return normalizedFirst != null ? normalizedFirst : trimToNull(second);
    }

    private static String trimToNull(String value) {
        return StrUtil.blankToDefault(StrUtil.trim(value), null);
    }

    @Data
    private static class EventPayload {

        private String eventId;
        private String formId;
        private String sourceFid;
        private String fid;
        private String billNo;
        private String operation;
        private String eventTime;

    }

    private record NormalizedEvent(String eventKey, String eventId, String formId, String sourceFid, String billNo,
                                   String operation, LocalDateTime eventTime) {
    }

}
