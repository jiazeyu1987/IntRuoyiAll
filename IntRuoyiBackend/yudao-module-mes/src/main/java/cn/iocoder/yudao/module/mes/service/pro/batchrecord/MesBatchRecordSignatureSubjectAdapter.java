package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.signature.api.ElectronicSignatureSubjectAdapter;
import cn.iocoder.yudao.module.signature.api.dto.SignatureActionDefinition;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectSnapshot;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Set;

@Component
public class MesBatchRecordSignatureSubjectAdapter implements ElectronicSignatureSubjectAdapter {

    static final String MODULE_CODE = "MES";
    static final String SUBJECT_TYPE = "MES_BATCH_RECORD";
    static final String POLICY_VERSION = "mes-batch-record-signature-v1";

    @Override
    public String moduleCode() {
        return MODULE_CODE;
    }

    @Override
    public Set<SignatureActionDefinition> supportedActions() {
        return Set.of(
                action(MesProBatchRecordExecutionSignatureService.ACTION_SUBMIT, "批记录提交"),
                action(MesProBatchRecordExecutionSignatureService.ACTION_APPROVE, "批记录审批通过"),
                action(MesProBatchRecordExecutionSignatureService.ACTION_REVIEW_APPROVE, "批记录复核通过"),
                action(MesProBatchRecordExecutionSignatureService.ACTION_REJECT, "批记录审批驳回"),
                action(MesProBatchRecordExecutionSignatureService.ACTION_ARCHIVE_SEAL, "批记录归档密封"),
                action(MesProBatchRecordExecutionSignatureService.ACTION_FIELD_CHANGE, "批记录字段变更"),
                action(MesProBatchRecordExecutionSignatureService.ACTION_FORM_REVIEW, "批记录表单复核"),
                action(MesProBatchRecordExecutionSignatureService.ACTION_PRODUCTION_SUBMIT, "生产提交"),
                action(MesProBatchRecordExecutionSignatureService.ACTION_PQC_SUBMIT, "PQC提交"),
                action(MesProBatchRecordExecutionSignatureService.ACTION_PQC_RELEASE, "PQC放行"),
                action(MesProBatchRecordExecutionSignatureService.ACTION_TEAM_LEADER_REVIEW, "班组长复核"),
                action(MesProBatchRecordExecutionSignatureService.ACTION_BATCH_VOID_REQUEST, "eDHR批次作废申请"),
                action(MesProBatchRecordExecutionSignatureService.ACTION_BATCH_CLOSE, "关闭eDHR批次"),
                action(MesProBatchRecordExecutionSignatureService.ACTION_QUALITY_REJECT, "质量终态拒收eDHR批次"),
                action(MesProBatchRecordExecutionSignatureService.ACTION_SPECIAL_NODE_SKIP, "跳过eDHR特殊工序"),
                action(MesProBatchRecordExecutionSignatureService.ACTION_ROUTE_FORM_OPTIONAL_SKIP, "跳过eDHR可选路线表单")
        );
    }

    @Override
    public SignatureSubjectSnapshot loadAndAuthorize(SignatureSubjectCommand command) {
        DecodedSubject decoded = decodeSubjectId(command.subjectId());
        String canonicalJson = "{"
                + "\"adapter\":\"MES_BATCH_RECORD\","
                + "\"executionId\":\"" + json(decoded.executionId()) + "\","
                + "\"actionType\":\"" + json(command.actionCode()) + "\","
                + "\"processInstanceId\":\"" + json(decoded.processInstanceId()) + "\","
                + "\"bpmTaskId\":\"" + json(decoded.bpmTaskId()) + "\","
                + "\"bpmTaskDefinitionKey\":\"" + json(decoded.bpmTaskDefinitionKey()) + "\","
                + "\"bpmTaskName\":\"" + json(decoded.bpmTaskName()) + "\","
                + "\"signatureCellKey\":\"" + json(decoded.signatureCellKey()) + "\","
                + "\"signatureRowIndex\":\"" + json(decoded.signatureRowIndex()) + "\","
                + "\"signatureColumnIndex\":\"" + json(decoded.signatureColumnIndex()) + "\","
                + "\"reviewSourceType\":\"" + json(decoded.reviewSourceType()) + "\","
                + "\"reviewSourceId\":\"" + json(decoded.reviewSourceId()) + "\","
                + "\"reviewSourceName\":\"" + json(decoded.reviewSourceName()) + "\","
                + "\"approvalResult\":\"" + json(decoded.approvalResult()) + "\","
                + "\"reason\":\"" + json(command.reason()) + "\","
                + "\"fieldAuditRevision\":\"" + json(decoded.fieldAuditRevision()) + "\","
                + "\"fieldAuditHeadHash\":\"" + json(decoded.fieldAuditHeadHash()) + "\","
                + "\"cellValuesHash\":\"" + json(decoded.cellValuesHash()) + "\","
                + "\"signatureChallengeHash\":\"" + json(decoded.signatureChallengeHash()) + "\""
                + "}";
        return new SignatureSubjectSnapshot(SUBJECT_TYPE, command.subjectId(), subjectVersion(command.subjectId()),
                canonicalJson, null, null, null, decoded.processInstanceId(), decoded.bpmTaskId(),
                decoded.bpmTaskDefinitionKey(), nodeOrder(decoded.bpmTaskDefinitionKey()));
    }

    static String encodeSubjectId(Long executionId, String actionType, String processInstanceId, String bpmTaskId,
                                  String bpmTaskDefinitionKey, String bpmTaskName, String signatureCellKey,
                                  Integer signatureRowIndex, Integer signatureColumnIndex, String reviewSourceType,
                                  Long reviewSourceId, String reviewSourceName, String approvalResult,
                                  Long fieldAuditRevision, String fieldAuditHeadHash, String cellValuesHash,
                                  String signatureChallengeHash) {
        String payload = String.join("\n",
                value(executionId), value(actionType), value(processInstanceId), value(bpmTaskId),
                value(bpmTaskDefinitionKey), value(bpmTaskName), value(signatureCellKey), value(signatureRowIndex),
                value(signatureColumnIndex), value(reviewSourceType), value(reviewSourceId), value(reviewSourceName),
                value(approvalResult), value(fieldAuditRevision), value(fieldAuditHeadHash), value(cellValuesHash),
                value(signatureChallengeHash));
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    static String subjectVersion(String subjectId) {
        return sha256(subjectId);
    }

    private static SignatureActionDefinition action(String actionCode, String meaningLabel) {
        return new SignatureActionDefinition(MODULE_CODE, actionCode, SUBJECT_TYPE, actionCode, meaningLabel,
                POLICY_VERSION);
    }

    private static DecodedSubject decodeSubjectId(String subjectId) {
        String payload = new String(Base64.getUrlDecoder().decode(subjectId), StandardCharsets.UTF_8);
        String[] values = payload.split("\n", -1);
        if (values.length != 17) {
            throw new IllegalArgumentException("MES_SIGNATURE_SUBJECT_INVALID");
        }
        return new DecodedSubject(values[0], values[1], values[2], values[3], values[4], values[5], values[6],
                values[7], values[8], values[9], values[10], values[11], values[12], values[13], values[14],
                values[15], values[16]);
    }

    private static Integer nodeOrder(String taskDefinitionKey) {
        return taskDefinitionKey == null || taskDefinitionKey.isBlank() ? null : Math.abs(taskDefinitionKey.hashCode());
    }

    private static String json(String value) {
        return value(value).replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String value(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String sha256(String payload) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", ex);
        }
    }

    private record DecodedSubject(String executionId, String actionType, String processInstanceId, String bpmTaskId,
                                  String bpmTaskDefinitionKey, String bpmTaskName, String signatureCellKey,
                                  String signatureRowIndex, String signatureColumnIndex, String reviewSourceType,
                                  String reviewSourceId, String reviewSourceName, String approvalResult,
                                  String fieldAuditRevision, String fieldAuditHeadHash, String cellValuesHash,
                                  String signatureChallengeHash) {
    }
}
