package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Exact read intent/business keys. No field in this record constitutes an authorization grant. */
public record DccControlledFileBodyReadContext(Intent intent, String controlledFileId, String expectedVersionNo,
        String applicationType, String bpmRound, String taskId) {
    public enum Intent { DAILY, AUTHORING, APPROVAL_TASK, APPROVAL_HISTORY, VERSION_HISTORY, TRAINING_APPLICATION }
    public static final String HEADER = "X-DCC-Body-Read-Context";
    public static final String PURPOSE_PREFIX = "BR_";
    public DccControlledFileBodyReadContext {
        Objects.requireNonNull(intent, "body read intent");
        if (controlledFileId == null || !controlledFileId.matches("[1-9][0-9]*")) invalid();
        try { if (Long.parseLong(controlledFileId) <= 0) invalid(); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("BODY_READ_FILE_ID_INVALID", ex); }
        if (intent == Intent.DAILY) {
            if (expectedVersionNo != null || applicationType != null || bpmRound != null || taskId != null) invalid();
        } else {
            exact(expectedVersionNo, 128);
            boolean roundRequired = Set.of(Intent.APPROVAL_TASK, Intent.APPROVAL_HISTORY, Intent.TRAINING_APPLICATION).contains(intent);
            if (roundRequired || applicationType != null || bpmRound != null) {
                if (intent == Intent.VERSION_HISTORY || !Set.of("UPLOAD", "REVISION", "OBSOLETE").contains(applicationType == null ? "" : applicationType)) invalid();
                exact(bpmRound, 64);
            }
            if (intent == Intent.APPROVAL_TASK) exact(taskId, 64);
            else if (intent == Intent.APPROVAL_HISTORY) { if (taskId != null) exact(taskId, 64); }
            else if (taskId != null) invalid();
            if ((intent == Intent.AUTHORING || intent == Intent.TRAINING_APPLICATION) && "OBSOLETE".equals(applicationType)) invalid();
        }
    }
    public static DccControlledFileBodyReadContext daily(Long fileId) {
        return new DccControlledFileBodyReadContext(Intent.DAILY, String.valueOf(fileId), null, null, null, null);
    }
    public void requireFile(Long fileId, String version) {
        if (!Objects.equals(controlledFileId, String.valueOf(fileId))
                || intent != Intent.DAILY && !Objects.equals(expectedVersionNo, version)) invalid();
    }
    public Map<String, String> canonicalFields() {
        var values = new LinkedHashMap<String, String>();
        values.put("intent", intent.name()); values.put("controlledFileId", controlledFileId);
        if (expectedVersionNo != null) values.put("expectedVersionNo", expectedVersionNo);
        if (applicationType != null) values.put("applicationType", applicationType);
        if (bpmRound != null) values.put("bpmRound", bpmRound);
        if (taskId != null) values.put("taskId", taskId);
        return values;
    }
    public String canonicalJson() { return JsonUtils.toJsonString(canonicalFields()); }
    public String wire() { return Base64.getUrlEncoder().withoutPadding().encodeToString(canonicalJson().getBytes(StandardCharsets.UTF_8)); }
    public String purpose() {
        try { return PURPOSE_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(canonicalJson().getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 is required", ex); }
    }
    public static boolean isBodyPurpose(String purpose) { return purpose != null && purpose.matches("BR_[A-Za-z0-9_-]{43}"); }
    public static DccControlledFileBodyReadContext fromWire(Long fileId, String wire) {
        if (wire == null || wire.length() > 2048 || !wire.matches("[A-Za-z0-9_-]+")) invalid();
        var values = JsonUtils.parseObject(new String(Base64.getUrlDecoder().decode(wire), StandardCharsets.UTF_8), Map.class);
        if (values == null || values.values().stream().anyMatch(value -> !(value instanceof String))) invalid();
        var context = new DccControlledFileBodyReadContext(Intent.valueOf((String) values.get("intent")),
                (String) values.get("controlledFileId"), (String) values.get("expectedVersionNo"),
                (String) values.get("applicationType"), (String) values.get("bpmRound"), (String) values.get("taskId"));
        if (!context.canonicalFields().equals(values) || !Objects.equals(context.controlledFileId(), String.valueOf(fileId))
                || !context.wire().equals(wire)) invalid();
        return context;
    }
    private static void exact(String value, int limit) {
        if (value == null || value.isBlank() || !value.equals(value.strip()) || value.length() > limit
                || value.chars().anyMatch(character -> character < 32 || character == 127)) invalid();
    }
    private static void invalid() { throw new IllegalArgumentException("BODY_READ_CONTEXT_INVALID"); }
}
