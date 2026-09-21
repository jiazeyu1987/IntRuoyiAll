package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOperationRespVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeControlOperationStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ReleaseWorkflowTestEvidenceStore {

    private static final ZoneId OPERATION_TIME_ZONE = ZoneId.of("Asia/Shanghai");
    private final RuntimeControlProperties properties;
    private final RuntimeControlOperationStore operationStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReleaseWorkflowTestEvidenceStore(RuntimeControlProperties properties,
                                            RuntimeControlOperationStore operationStore) {
        this.properties = properties;
        this.operationStore = operationStore;
    }

    public Path write(String workflowId, String releaseTag, RuntimeControlOperationRespVO operation) {
        validateOperation(operation, releaseTag);
        Path path = evidencePath(workflowId, operation.getOperationId());
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("operationId", operation.getOperationId());
        evidence.put("requestedAt", requestedAt(operation));
        evidence.put("environment", "test");
        evidence.put("action", "publish-test");
        evidence.put("status", "SUCCESS");
        evidence.put("parameters", Map.of("releaseTag", releaseTag));
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            objectMapper.writeValue(temporary.toFile(), evidence);
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return path;
        } catch (IOException ex) {
            throw new IllegalStateException("TEST_OPERATION_EVIDENCE_WRITE_FAILED", ex);
        }
    }

    public VerifiedEvidence verify(ReleaseWorkflowRecord workflow) {
        String operationId = workflow.testOperationId();
        if (operationId == null || workflow.testOperationEvidencePath() == null) {
            throw new IllegalStateException("TEST_OPERATION_EVIDENCE_MISSING");
        }
        Path expected = evidencePath(workflow.workflowId(), operationId);
        if (!expected.toString().equals(workflow.testOperationEvidencePath()) || !Files.isRegularFile(expected)) {
            throw new IllegalStateException("TEST_OPERATION_EVIDENCE_PATH_INVALID");
        }
        RuntimeControlOperationRespVO operation = operationStore.findById(operationId);
        validateOperation(operation, workflow.releaseTag());
        try {
            byte[] bytes = Files.readAllBytes(expected);
            JsonNode evidence = objectMapper.readTree(bytes);
            String timestamp = requestedAt(operation);
            if (evidence == null
                    || !operationId.equals(text(evidence, "operationId"))
                    || !timestamp.equals(text(evidence, "requestedAt"))
                    || !"test".equals(text(evidence, "environment"))
                    || !"publish-test".equals(text(evidence, "action"))
                    || !"SUCCESS".equals(text(evidence, "status"))
                    || !workflow.releaseTag().equals(text(evidence.path("parameters"), "releaseTag"))) {
                throw new IllegalStateException("TEST_OPERATION_EVIDENCE_MISMATCH");
            }
            Instant.parse(timestamp);
            return new VerifiedEvidence(timestamp, ReleaseDigestContract.manifestDigest(bytes));
        } catch (IOException | IllegalArgumentException ex) {
            throw new IllegalStateException("TEST_OPERATION_EVIDENCE_INVALID", ex);
        }
    }

    private void validateOperation(RuntimeControlOperationRespVO operation, String releaseTag) {
        if (operation == null || operation.getRequestedAt() == null
                || !"succeeded".equals(operation.getStatus())
                || !"publish-test".equals(operation.getAction())
                || !"test".equals(operation.getEnvironment())
                || operation.getParameters() == null
                || !releaseTag.equals(operation.getParameters().get("releaseTag"))) {
            throw new IllegalStateException("TEST_OPERATION_EVIDENCE_SOURCE_INVALID");
        }
    }

    private String requestedAt(RuntimeControlOperationRespVO operation) {
        return operation.getRequestedAt().atZone(OPERATION_TIME_ZONE).toInstant().toString();
    }

    private Path evidencePath(String workflowId, String operationId) {
        if (workflowId == null || !workflowId.matches("(?:rw-[a-z0-9]{8,32}|wf-[a-z0-9-]{6,64})")
                || operationId == null || !operationId.matches("(?:op-)?[a-z0-9-]{8,64}")) {
            throw new IllegalStateException("TEST_OPERATION_EVIDENCE_ID_INVALID");
        }
        return Path.of(properties.getStateDir()).toAbsolutePath().normalize()
                .resolve("workflow-evidence").resolve(workflowId).resolve(operationId + ".json");
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || !value.isTextual() ? "" : value.asText();
    }

    public record VerifiedEvidence(String requestedAt, String sha256) {
    }
}
