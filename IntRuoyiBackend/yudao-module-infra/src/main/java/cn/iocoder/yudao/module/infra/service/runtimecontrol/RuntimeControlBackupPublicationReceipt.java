package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow.ReleaseWorkflowRecord;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

/** Internal receipt. Raw owner identity is never placed in the HTTP operation response. */
public record RuntimeControlBackupPublicationReceipt(int schemaVersion, String state, Binding binding,
                                                      RuntimeEvidence runtime, String receiptDigest,
                                                      String receiptBase64, String confirmationDecisionDigest) {
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    private static final String PREFIX = "REVIEW_PUBLISH_RECEIPT_JSON=";
    private static final String MISSING = "REVIEW_PUBLISH_RECEIPT_MISSING=1";

    public record Binding(String workflowId, String operationId, String releaseTag, String packageDigest,
                          String manifestDigest, String targetEnvironment, String targetHost,
                          String runtimeDir, String leaseToken) { }

    public record RuntimeEvidence(String imageTag, String backendImage, String frontendImage,
                                  String healthStatus, int frontendHttp) { }

    public void verifyFor(ReleaseWorkflowRecord workflow) {
        if (workflow == null || workflow.backupIntent() == null || binding == null || runtime == null
                || schemaVersion != 1 || !Objects.equals(binding.workflowId(), workflow.workflowId())
                || !Objects.equals(binding.operationId(), workflow.operationId())
                || !Objects.equals(binding.releaseTag(), workflow.releaseTag())
                || workflow.packageDigest() == null || workflow.manifestDigest() == null
                || !Objects.equals(binding.packageDigest(), workflow.packageDigest())
                || !Objects.equals(binding.manifestDigest(), workflow.manifestDigest())
                || !"backup".equals(binding.targetEnvironment()) || !"172.30.30.59".equals(binding.targetHost())
                || !"/opt/intruoyi/runtime".equals(binding.runtimeDir())
                || binding.leaseToken() == null || !binding.leaseToken().matches("[0-9a-f]{32}")) {
            throw new IllegalArgumentException("BACKUP_PUBLICATION_RECEIPT_BINDING_MISMATCH");
        }
        if (!workflow.releaseTag().equals(runtime.imageTag())
                || !("intruoyi-backend:" + workflow.releaseTag()).equals(runtime.backendImage())
                || !("intruoyi-frontend:" + workflow.releaseTag()).equals(runtime.frontendImage())
                || !"UP".equals(runtime.healthStatus()) || runtime.frontendHttp() != 200) {
            throw new IllegalArgumentException("BACKUP_PUBLICATION_RUNTIME_ACCEPTANCE_FAILED");
        }
        if (!("AWAITING_CONFIRMATION".equals(state) && confirmationDecisionDigest == null)
                && !("CONFIRMED".equals(state) && confirmationDecisionDigest != null
                    && confirmationDecisionDigest.matches("[0-9a-f]{64}"))) {
            throw new IllegalArgumentException("BACKUP_PUBLICATION_RECEIPT_STATE_INVALID");
        }
        try {
            byte[] original = Base64.getDecoder().decode(Objects.requireNonNull(receiptBase64));
            if (!Base64.getEncoder().encodeToString(original).equals(receiptBase64)
                    || receiptDigest == null || !receiptDigest.matches("[0-9a-f]{64}")
                    || !sha256(original).equals(receiptDigest)) {
                throw new IllegalArgumentException("BACKUP_PUBLICATION_RECEIPT_DIGEST_MISMATCH");
            }
            String utf8 = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                    .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(original)).toString();
            var core = JSON.readTree(utf8);
            if (core == null || !core.isObject() || core.size() != 3
                    || !core.path("schemaVersion").isInt() || core.path("schemaVersion").intValue() != schemaVersion
                    || !JSON.valueToTree(binding).equals(core.get("binding"))
                    || !JSON.valueToTree(runtime).equals(core.get("runtime"))) {
                throw new IllegalArgumentException("BACKUP_PUBLICATION_RECEIPT_ENVELOPE_MISMATCH");
            }
        } catch (IOException | NullPointerException ex) {
            throw new IllegalArgumentException("BACKUP_PUBLICATION_RECEIPT_BYTES_INVALID", ex);
        }
    }

    public RuntimeControlBackupPublicationReceipt awaitingConfirmation() {
        return new RuntimeControlBackupPublicationReceipt(schemaVersion, "AWAITING_CONFIRMATION", binding,
                runtime, receiptDigest, receiptBase64, null);
    }

    public boolean sameImmutableReceipt(RuntimeControlBackupPublicationReceipt other) {
        return other != null && schemaVersion == other.schemaVersion && Objects.equals(binding, other.binding)
                && Objects.equals(runtime, other.runtime) && Objects.equals(receiptDigest, other.receiptDigest)
                && Objects.equals(receiptBase64, other.receiptBase64);
    }

    public static Optional<RuntimeControlBackupPublicationReceipt> parseOutput(String output) {
        String payload = null;
        int missing = 0;
        for (String line : Objects.requireNonNull(output).lines().toList()) {
            if (line.startsWith(PREFIX)) {
                if (payload != null) throw new IllegalArgumentException("BACKUP_PUBLICATION_RECEIPT_OUTPUT_AMBIGUOUS");
                payload = line.substring(PREFIX.length());
            }
            if (MISSING.equals(line)) missing++;
        }
        if (missing == 1 && payload == null) return Optional.empty();
        if (missing != 0 || payload == null) {
            throw new IllegalArgumentException("BACKUP_PUBLICATION_RECEIPT_OUTPUT_REQUIRED");
        }
        try {
            return Optional.of(JSON.readValue(payload, RuntimeControlBackupPublicationReceipt.class));
        } catch (IOException ex) {
            throw new IllegalArgumentException("BACKUP_PUBLICATION_RECEIPT_JSON_INVALID", ex);
        }
    }

    public static Optional<RuntimeControlBackupPublicationReceipt> parseLog(Path path) throws IOException {
        StringBuilder protocol = new StringBuilder();
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(PREFIX) || MISSING.equals(line)) protocol.append(line).append('\n');
            }
        }
        return parseOutput(protocol.toString());
    }

    public static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA256_UNAVAILABLE", ex);
        }
    }
}
