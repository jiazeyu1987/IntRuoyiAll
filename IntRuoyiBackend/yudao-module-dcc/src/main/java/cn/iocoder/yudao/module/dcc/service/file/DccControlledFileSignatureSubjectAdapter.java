package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.signature.api.ElectronicSignatureSubjectAdapter;
import cn.iocoder.yudao.module.signature.api.dto.SignatureActionDefinition;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectSnapshot;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

@Component
public class DccControlledFileSignatureSubjectAdapter implements ElectronicSignatureSubjectAdapter {

    static final String MODULE_CODE = "DCC";
    static final String SUBJECT_TYPE = "DCC_CONTROLLED_FILE";
    static final String POLICY_VERSION = "dcc-controlled-file-signature-v1";

    @Override
    public String moduleCode() {
        return MODULE_CODE;
    }

    @Override
    public Set<SignatureActionDefinition> supportedActions() {
        return Set.of(
                action("APPROVE", "审批通过"),
                action("REJECT", "审批驳回"),
                action("RETURN", "流程回退"),
                action("TRANSFER", "任务转办"),
                action("ADD_SIGN", "加签"),
                action("DISTRIBUTION_ACK", "分发确认"),
                action("DISTRIBUTION_SIGN", "分发签收")
        );
    }

    @Override
    public SignatureSubjectSnapshot loadAndAuthorize(SignatureSubjectCommand command) {
        DecodedSubject decoded = decodeSubjectId(command.subjectId());
        String canonicalJson = "{"
                + "\"adapter\":\"DCC_CONTROLLED_FILE\","
                + "\"controlledFileId\":\"" + json(decoded.controlledFileId()) + "\","
                + "\"revisionId\":\"" + json(decoded.revisionId()) + "\","
                + "\"versionNo\":\"" + json(decoded.versionNo()) + "\","
                + "\"taskId\":\"" + json(decoded.taskId()) + "\","
                + "\"stageCode\":\"" + json(decoded.stageCode()) + "\","
                + "\"actionType\":\"" + json(command.actionCode()) + "\","
                + "\"meaningCode\":\"" + json(decoded.meaningCode()) + "\","
                + "\"reason\":\"" + json(command.reason()) + "\","
                + "\"recordHashSnapshot\":\"" + json(decoded.recordHashSnapshot()) + "\","
                + "\"sourceFileHash\":\"" + json(decoded.sourceFileHash()) + "\","
                + "\"controlledCopyHash\":\"" + json(decoded.controlledCopyHash()) + "\","
                + "\"legacyEvidenceHash\":\"" + json(decoded.evidenceHash()) + "\","
                + "\"canonicalPayloadHash\":\"" + json(decoded.canonicalPayloadHash()) + "\""
                + "}";
        return new SignatureSubjectSnapshot(SUBJECT_TYPE, command.subjectId(), subjectVersion(command.subjectId()),
                canonicalJson, null, null, null, null, decoded.taskId(), decoded.stageCode(), nodeOrder(decoded.stageCode()));
    }

    static String encodeSubjectId(Long controlledFileId, String taskId, String stageCode, String actionType,
                                  String meaningCode, DccControlledFileSignatureEvidence evidence) {
        String payload = String.join("\n",
                value(controlledFileId),
                value(evidence.getRevisionId()),
                value(evidence.getVersionNo()),
                value(taskId),
                value(stageCode),
                value(actionType),
                value(meaningCode),
                value(evidence.getRecordHashSnapshot()),
                value(evidence.getSourceFileHash()),
                value(evidence.getControlledCopyHash()),
                value(evidence.getEvidenceHash()),
                ApprovalSignatureHash.sha256(value(evidence.getCanonicalPayload())));
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    static String subjectVersion(String subjectId) {
        return ApprovalSignatureHash.sha256(subjectId);
    }

    private static SignatureActionDefinition action(String actionCode, String meaningLabel) {
        return new SignatureActionDefinition(MODULE_CODE, actionCode, SUBJECT_TYPE, actionCode, meaningLabel,
                POLICY_VERSION);
    }

    private static DecodedSubject decodeSubjectId(String subjectId) {
        String payload = new String(Base64.getUrlDecoder().decode(subjectId), StandardCharsets.UTF_8);
        String[] values = payload.split("\n", -1);
        if (values.length != 12) {
            throw new IllegalArgumentException("DCC_SIGNATURE_SUBJECT_INVALID");
        }
        return new DecodedSubject(values[0], values[1], values[2], values[3], values[4], values[5],
                values[6], values[7], values[8], values[9], values[10], values[11]);
    }

    private static Integer nodeOrder(String stageCode) {
        return switch (value(stageCode)) {
            case "APPLICANT_REWORK" -> 10;
            case "DOC_CONTROL_REVIEW" -> 20;
            case "MATRIX_REVIEW" -> 30;
            case "MATRIX_APPROVAL" -> 40;
            case "DOC_CONTROL_APPROVAL" -> 50;
            case "DISTRIBUTION" -> 60;
            default -> null;
        };
    }

    private static String json(String value) {
        return value(value).replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String value(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private record DecodedSubject(String controlledFileId, String revisionId, String versionNo, String taskId,
                                  String stageCode, String actionType, String meaningCode, String recordHashSnapshot,
                                  String sourceFileHash, String controlledCopyHash, String evidenceHash,
                                  String canonicalPayloadHash) {
    }

}
