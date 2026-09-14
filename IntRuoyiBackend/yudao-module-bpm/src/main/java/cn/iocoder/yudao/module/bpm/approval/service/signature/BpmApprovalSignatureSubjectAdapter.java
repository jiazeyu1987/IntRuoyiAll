package cn.iocoder.yudao.module.bpm.approval.service.signature;

import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskReviewContext;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureSubjectAdapter;
import cn.iocoder.yudao.module.signature.api.dto.SignatureActionDefinition;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectSnapshot;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

@Component
public class BpmApprovalSignatureSubjectAdapter implements ElectronicSignatureSubjectAdapter {

    private static final String SUBJECT_TYPE = ApprovalSignatureRecordServiceImpl.SUBJECT_TYPE;
    private static final String POLICY_VERSION = "bpm-approval-signature-v1";

    @Override
    public String moduleCode() {
        return ApprovalSignatureRecordServiceImpl.MODULE_CODE;
    }

    @Override
    public Set<SignatureActionDefinition> supportedActions() {
        return Set.of(
                new SignatureActionDefinition(moduleCode(), "APPROVE", SUBJECT_TYPE,
                        "APPROVE", "审批通过", POLICY_VERSION),
                new SignatureActionDefinition(moduleCode(), "REJECT", SUBJECT_TYPE,
                        "REJECT", "审批驳回", POLICY_VERSION)
        );
    }

    @Override
    public SignatureSubjectSnapshot loadAndAuthorize(SignatureSubjectCommand command) {
        DecodedSubject decoded = decodeSubjectId(command.subjectId());
        String canonicalJson = "{"
                + "\"adapter\":\"BPM_APPROVAL\","
                + "\"moduleCode\":\"" + json(decoded.moduleCode()) + "\","
                + "\"sourceTaskType\":\"" + json(decoded.sourceTaskType()) + "\","
                + "\"sourceTaskId\":\"" + json(decoded.sourceTaskId()) + "\","
                + "\"businessKey\":\"" + json(decoded.businessKey()) + "\","
                + "\"processInstanceId\":\"" + json(decoded.processInstanceId()) + "\","
                + "\"reviewResult\":\"" + json(command.actionCode()) + "\","
                + "\"reason\":\"" + json(command.reason()) + "\","
                + "\"signatureImagePayloadHash\":\"" + json(decoded.signatureImagePayloadHash()) + "\""
                + "}";
        return new SignatureSubjectSnapshot(SUBJECT_TYPE, command.subjectId(), subjectVersion(command.subjectId()),
                canonicalJson, null, null, null, decoded.processInstanceId(), decoded.sourceTaskId(),
                decoded.sourceTaskType(), null);
    }

    static String encodeSubjectId(ApprovalTaskReviewContext context, ApprovalSignatureImageSnapshot imageSnapshot) {
        String payload = String.join("\n",
                context.getModuleCode().name(),
                value(context.getSourceTaskType()),
                value(context.getSourceTaskId()),
                value(context.getBusinessKey()),
                value(context.getProcessInstanceId()),
                ApprovalSignatureRecordServiceImpl.sha256(
                        ApprovalSignatureRecordServiceImpl.signatureImagePayload(imageSnapshot)));
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    static String subjectVersion(String subjectId) {
        return ApprovalSignatureRecordServiceImpl.sha256(subjectId);
    }

    private static DecodedSubject decodeSubjectId(String subjectId) {
        String payload = new String(Base64.getUrlDecoder().decode(subjectId), StandardCharsets.UTF_8);
        String[] values = payload.split("\n", -1);
        if (values.length != 6) {
            throw new IllegalArgumentException("BPM_APPROVAL_SIGNATURE_SUBJECT_INVALID");
        }
        return new DecodedSubject(values[0], values[1], emptyToNull(values[2]), emptyToNull(values[3]),
                emptyToNull(values[4]), values[5]);
    }

    private static String json(String value) {
        return value(value).replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String value(String value) {
        return value == null ? "" : value.trim();
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private record DecodedSubject(String moduleCode, String sourceTaskType, String sourceTaskId, String businessKey,
                                  String processInstanceId, String signatureImagePayloadHash) {
    }

}
