package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
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

    public static final String MODULE_CODE = "MES";
    public static final String SUBJECT_TYPE = "MES_BATCH_RECORD";
    static final String POLICY_VERSION = "mes-batch-record-signature-v1";
    private static final String OPINION_SOURCE_PREFIX = "MES_OPINION_SOURCE_V1:";

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
                action(MesProBatchRecordExecutionSignatureService.ACTION_MARKET_RELEASE, "上市放行"),
                action(MesProBatchRecordExecutionSignatureService.ACTION_MARKET_RELEASE_REJECT, "不予上市放行"),
                action(MesProBatchRecordExecutionSignatureService.ACTION_TEAM_LEADER_REVIEW, "班组长复核"),
                action(MesProBatchRecordExecutionSignatureService.ACTION_BATCH_VOID_REQUEST, "eDHR批次作废申请"),
                action(MesProBatchRecordExecutionSignatureService.ACTION_BATCH_CLOSE, "关闭eDHR批次"),
                action(MesProBatchRecordExecutionSignatureService.ACTION_QUALITY_REJECT, "质量终态拒收eDHR批次"),
                action(MesProBatchRecordExecutionSignatureService.ACTION_NONCONFORMANCE_REJECT,
                        "上市放行负责人发起eDHR不合格评审"),
                action(MesProBatchRecordExecutionSignatureService.ACTION_NONCONFORMANCE_REVIEW_CREATE,
                        "QA发起关键偏差不合格评审"),
                action(MesProBatchRecordExecutionSignatureService.ACTION_SPECIAL_NODE_SKIP, "跳过eDHR特殊工序"),
                action(MesProBatchRecordExecutionSignatureService.ACTION_ROUTE_FORM_OPTIONAL_SKIP, "跳过eDHR可选路线表单"),
                action(MesProBatchRecordExecutionSignatureService.ACTION_QA_DISPOSITION, "QA不合格评审处置"),
                action(MesProBatchRecordExecutionSignatureService.ACTION_NONCONFORMANCE_REVIEW_CREATE,
                        "eDHR不合格评审创建"),
                action(MesProBatchRecordExecutionSignatureService.ACTION_DEVIATION_INITIATION, "发起eDHR偏差"),
                action(MesProBatchRecordExecutionSignatureService.ACTION_DEVIATION_HANDLING_PREPARE, "偏差处理编制"),
                action(MesProBatchRecordExecutionSignatureService.ACTION_DEVIATION_HANDLING_VERIFY, "偏差处理验证"),
                action(MesProBatchRecordExecutionSignatureService.ACTION_DEVIATION_HANDLING_DEPARTMENT, "偏差部门负责人确认"),
                action(MesProBatchRecordExecutionSignatureService.ACTION_DEVIATION_HANDLING_QA, "偏差QA关闭确认"),
                action(MesProBatchRecordExecutionSignatureService.ACTION_DEVIATION_HANDLING_QUALITY, "偏差质量负责人批准"),
                action(MesProBatchRecordExecutionSignatureService.ACTION_DEVIATION_HANDLING_MANAGEMENT, "关键偏差管理者代表批准")
        );
    }

    @Override
    public SignatureSubjectSnapshot loadAndAuthorize(SignatureSubjectCommand command) {
        DecodedSubject decoded = decodeFrozenSubject(command.subjectId());
        String canonicalJson = canonicalBusinessContent(command.subjectId(), command.actionCode(), command.reason());
        return new SignatureSubjectSnapshot(SUBJECT_TYPE, command.subjectId(), subjectVersion(command.subjectId()),
                canonicalJson, null, null, null, decoded.processInstanceId(), decoded.bpmTaskId(),
                decoded.bpmTaskDefinitionKey(), nodeOrder(decoded.bpmTaskDefinitionKey()));
    }

    public static String canonicalBusinessContent(String subjectId, String actionCode, String reason) {
        DecodedSubject decoded = decodeFrozenSubject(subjectId);
        if (decoded.opinionSource() != null) verifiedUserOpinion(subjectId, actionCode, reason);
        String canonicalJson = "{"
                + "\"adapter\":\"MES_BATCH_RECORD\","
                + "\"executionId\":\"" + json(decoded.executionId()) + "\","
                + "\"actionType\":\"" + json(actionCode) + "\","
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
                + "\"reason\":\"" + json(reason) + "\","
                + "\"fieldAuditRevision\":\"" + json(decoded.fieldAuditRevision()) + "\","
                + "\"fieldAuditHeadHash\":\"" + json(decoded.fieldAuditHeadHash()) + "\","
                + "\"cellValuesHash\":\"" + json(decoded.cellValuesHash()) + "\","
                + "\"signatureChallengeHash\":\"" + json(decoded.signatureChallengeHash()) + "\""
                + (decoded.opinionSource() == null ? "" : ",\"opinionSource\":\"" + json(decoded.opinionSource()) + "\"")
                + "}";
        return canonicalJson;
    }

    public static String encodeSubjectId(Long executionId, String actionType, String processInstanceId, String bpmTaskId,
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

    public static String subjectVersion(String subjectId) {
        return sha256(subjectId);
    }

    /** An explicit V1 opinion fact; historical 17-part subjects are never rewritten. */
    public static String withOpinionSource(String subjectId, String opinion) {
        DecodedSubject subject = decodeFrozenSubject(subjectId);
        if (subject.opinionSource() != null || !("PQC_RELEASE".equals(subject.actionType())
                || "APPROVE".equals(subject.actionType()))) {
            throw new IllegalArgumentException("MES_SIGNATURE_OPINION_SOURCE_INVALID");
        }
        String payload = new String(Base64.getUrlDecoder().decode(subjectId), StandardCharsets.UTF_8);
        return Base64.getUrlEncoder().withoutPadding().encodeToString((payload + "\n" + OPINION_SOURCE_PREFIX
                + (opinion == null || opinion.isBlank() ? "SYSTEM" : "USER")).getBytes(StandardCharsets.UTF_8));
    }

    /** Call only after the persisted signature's identity, canonical content and hashes have been verified. */
    public static String verifiedUserOpinion(String subjectId, String action, String reason) {
        DecodedSubject subject = decodeFrozenSubject(subjectId);
        if (!action.equals(subject.actionType())) throw new IllegalArgumentException("MES_SIGNATURE_OPINION_ACTION_CONFLICT");
        if (subject.opinionSource() == null) return null;
        if ("USER".equals(subject.opinionSource())) {
            if (reason == null || reason.isBlank()) throw new IllegalArgumentException("MES_SIGNATURE_USER_OPINION_REQUIRED");
            return reason;
        }
        if (!action.equals(reason)) throw new IllegalArgumentException("MES_SIGNATURE_SYSTEM_REASON_CONFLICT");
        return null;
    }

    public static String subjectWithoutOpinionSource(String subjectId) {
        DecodedSubject subject = decodeFrozenSubject(subjectId);
        if (subject.opinionSource() == null) return subjectId;
        String payload = new String(Base64.getUrlDecoder().decode(subjectId), StandardCharsets.UTF_8);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                payload.substring(0, payload.lastIndexOf('\n')).getBytes(StandardCharsets.UTF_8));
    }

    private static SignatureActionDefinition action(String actionCode, String meaningLabel) {
        return new SignatureActionDefinition(MODULE_CODE, actionCode, SUBJECT_TYPE, actionCode, meaningLabel,
                POLICY_VERSION);
    }

    public static DecodedSubject decodeFrozenSubject(String subjectId) {
        String payload = new String(Base64.getUrlDecoder().decode(subjectId), StandardCharsets.UTF_8);
        String[] values = payload.split("\n", -1);
        if (values.length != 17 && values.length != 18) {
            throw new IllegalArgumentException("MES_SIGNATURE_SUBJECT_INVALID");
        }
        String source = null;
        if (values.length == 18) {
            if (!("PQC_RELEASE".equals(values[1]) || "APPROVE".equals(values[1]))
                    || !(OPINION_SOURCE_PREFIX.concat("USER").equals(values[17])
                    || OPINION_SOURCE_PREFIX.concat("SYSTEM").equals(values[17]))) {
                throw new IllegalArgumentException("MES_SIGNATURE_OPINION_SOURCE_INVALID");
            }
            source = values[17].substring(OPINION_SOURCE_PREFIX.length());
        }
        return new DecodedSubject(values[0], values[1], values[2], values[3], values[4], values[5], values[6],
                values[7], values[8], values[9], values[10], values[11], values[12], values[13], values[14],
                values[15], values[16], source);
    }

    private static Integer nodeOrder(String taskDefinitionKey) {
        return taskDefinitionKey == null || taskDefinitionKey.isBlank() ? null : Math.abs(taskDefinitionKey.hashCode());
    }

    private static String json(String value) {
        String encoded = JsonUtils.toJsonString(value(value));
        return encoded.substring(1, encoded.length() - 1);
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

    public record DecodedSubject(String executionId, String actionType, String processInstanceId, String bpmTaskId,
                                  String bpmTaskDefinitionKey, String bpmTaskName, String signatureCellKey,
                                  String signatureRowIndex, String signatureColumnIndex, String reviewSourceType,
                                  String reviewSourceId, String reviewSourceName, String approvalResult,
                                  String fieldAuditRevision, String fieldAuditHeadHash, String cellValuesHash,
                                  String signatureChallengeHash, String opinionSource) {
    }
}
