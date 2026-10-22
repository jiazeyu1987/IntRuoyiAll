package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.signature.api.ElectronicSignatureSubjectAdapter;
import cn.iocoder.yudao.module.signature.api.dto.SignatureActionDefinition;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectSnapshot;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;
import java.util.Objects;

@Component
public class DccControlledFileSignatureSubjectAdapter implements ElectronicSignatureSubjectAdapter {
    private static final ThreadLocal<java.util.Map<String,String>> DELAYED_INTENTS=new ThreadLocal<>();
    public static final class DelayedIntentScope implements AutoCloseable {
        private DelayedIntentScope(){}
        @Override public void close(){DELAYED_INTENTS.remove();}
    }
    /** Explicit one-call server-owned intent scope. No request payload or persistent cache can manufacture it. */
    public static DelayedIntentScope openDelayedIntent(DccControlledFileSignatureEvidence intent){
        if(DELAYED_INTENTS.get()!=null || intent==null || !"v5-delayed-effective".equals(intent.getEvidencePayloadVersion())
                || intent.getCanonicalPayload()==null || !"".equals(intent.getEvidenceHash()))
            throw new IllegalArgumentException("延迟签名正式意图作用域缺失或嵌套");
        DELAYED_INTENTS.set(java.util.Map.of(ApprovalSignatureHash.sha256(intent.getCanonicalPayload()),intent.getCanonicalPayload()));
        return new DelayedIntentScope();
    }
    @jakarta.annotation.Resource private DccControlledFileSignatureEvidenceService signatureEvidenceService;

    static final String MODULE_CODE = "DCC";
    static final String SUBJECT_TYPE = "DCC_CONTROLLED_FILE";
    static final String POLICY_VERSION = "dcc-controlled-file-signature-v1";
    static final String RECEIPT_POLICY_VERSION = "dcc-distribution-receipt-optional-comment-v2";

    @Override
    public String moduleCode() {
        return MODULE_CODE;
    }

    @Override
    public Set<SignatureActionDefinition> supportedActions() {
        return Set.of(
                action("ASSIGN", "会签指派确认"),
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
    public ServerTimeResolution serverTimeResolution(SignatureSubjectCommand command, SignatureSubjectSnapshot snapshot) {
        var wire=new String(Base64.getUrlDecoder().decode(command.subjectId()),StandardCharsets.UTF_8);
        if(wire.startsWith("v5-delayed-effective\n")){
            if(!"DELAYED_EFFECTIVE_CONFIRM".equals(snapshot.nodeCode()) || snapshot.processInstanceId()==null || snapshot.taskId()==null)
                throw new IllegalArgumentException("延迟确认签名服务端时间策略必须绑定正式已授权本轮subject");
            return ServerTimeResolution.MICROS;
        }
        return ServerTimeResolution.ORIGINAL;
    }
    @Override
    public SignatureSubjectSnapshot bindServerSignedAt(SignatureSubjectCommand command,SignatureSubjectSnapshot authorized,java.time.LocalDateTime signedAt){
        var decoded=decodeSubjectId(command.subjectId());if(decoded.delayedIntent()==null)return authorized;
        requireDelayedIntent(command,decoded);
        if(!"DELAYED_EFFECTIVE_CONFIRM".equals(authorized.nodeCode()) || signedAt==null || signedAt.getNano()%1000!=0)
            throw new IllegalArgumentException("延迟确认仅允许正式服务端微秒签名时间");
        var req=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(decoded.delayedIntent(),DccControlledFileSignatureEvidenceCreateReq.class);
        if(req==null || !Objects.equals(command.actorId(),req.getSignerUserId()) || !Objects.equals(authorized.taskId(),req.getTaskId())
                || !Objects.equals(authorized.processInstanceId(),req.getProcessInstanceId()) || !Objects.equals(command.reason(),req.getReasonText()))
            throw new IllegalArgumentException("延迟确认签名时间绑定与已授权业务内容不一致");
        req.setSignedAt(signedAt);var evidence=signatureEvidenceService.createEvidence(req);
        if(!Objects.equals(evidence.getSourceFileHash(),decoded.sourceFileHash()) || !Objects.equals(String.valueOf(evidence.getRevisionId()),decoded.revisionId())
                || !Objects.equals(evidence.getVersionNo(),decoded.versionNo()))throw new IllegalArgumentException("时间绑定期间本次正文或版本身份变化");
        var content=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(authorized.canonicalContentJson(),java.util.LinkedHashMap.class);
        content.put("serverSignedAt",signedAt.toString());content.put("derivedSignatureEvidence",evidence);
        return new SignatureSubjectSnapshot(authorized.subjectType(),authorized.subjectId(),authorized.subjectVersion(),
                cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(content),authorized.beforeContentJson(),authorized.afterContentJson(),authorized.fieldDiffJson(),
                authorized.processInstanceId(),authorized.taskId(),authorized.nodeCode(),authorized.nodeOrder());
    }

    @Override
    public boolean isReasonRequired(String actionCode, String subjectType) {
        return !("DISTRIBUTION_ACK".equals(actionCode) && SUBJECT_TYPE.equals(subjectType));
    }

    @Override
    public SignatureSubjectSnapshot loadAndAuthorize(SignatureSubjectCommand command) {
        DecodedSubject decoded = decodeSubjectId(command.subjectId());
        if(decoded.delayedIntent()!=null)requireDelayedIntent(command,decoded);
        if (!MODULE_CODE.equals(command.moduleCode()) || !SUBJECT_TYPE.equals(command.subjectType())
                || !decoded.actionType().equals(command.actionCode())
                || !subjectVersion(command.subjectId()).equals(command.expectedSubjectVersion()))
            throw new IllegalArgumentException("DCC_SIGNATURE_SUBJECT_CONTEXT_MISMATCH");
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
                + (decoded.processInstanceId() == null ? "" : ",\"processInstanceId\":\"" + json(decoded.processInstanceId()) + "\"")
                + "}";
        return new SignatureSubjectSnapshot(SUBJECT_TYPE, command.subjectId(), subjectVersion(command.subjectId()),
                canonicalJson, null, null, null, decoded.processInstanceId(), decoded.taskId(), decoded.stageCode(),
                nodeOrder(decoded.stageCode(),decoded.processInstanceId()!=null));
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
        if (DccControlledFileSignatureEvidenceServiceImpl.PAYLOAD_VERSION_V4_WORKFLOW.equals(evidence.getEvidencePayloadVersion())
                || DccControlledFileSignatureEvidenceServiceImpl.PAYLOAD_VERSION_V5_DELAYED.equals(evidence.getEvidencePayloadVersion())) {
            if (evidence.getProcessInstanceId() == null || evidence.getProcessInstanceId().isBlank())
                throw new IllegalArgumentException("DCC_SIGNATURE_WORKFLOW_ROUND_REQUIRED");
            payload = evidence.getEvidencePayloadVersion() + "\n" + evidence.getProcessInstanceId() + "\n" + payload;
        }
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    static String subjectVersion(String subjectId) {
        return ApprovalSignatureHash.sha256(subjectId);
    }

    private static SignatureActionDefinition action(String actionCode, String meaningLabel) {
        return new SignatureActionDefinition(MODULE_CODE, actionCode, SUBJECT_TYPE, actionCode, meaningLabel,
                "DISTRIBUTION_ACK".equals(actionCode) ? RECEIPT_POLICY_VERSION : POLICY_VERSION);
    }

    private static DecodedSubject decodeSubjectId(String subjectId) {
        String payload = new String(Base64.getUrlDecoder().decode(subjectId), StandardCharsets.UTF_8);
        String[] values = payload.split("\n", -1);
        String processInstanceId = null;
        String delayedIntent = null;
        if(values.length>0 && "v5-delayed-effective".equals(values[0])){
            if(values.length!=14 || DELAYED_INTENTS.get()==null)throw new IllegalArgumentException("新延迟签名必须绑定服务端本次正式意图作用域");
            delayedIntent=DELAYED_INTENTS.get().get(values[13]);
        }
        if(values.length>0 && "v5-delayed-effective".equals(values[0]) && delayedIntent==null)
            throw new IllegalArgumentException("新延迟签名必须包含完整正式意图，不能省略时间绑定");
        if (values.length == 14 && ("v4-workflow".equals(values[0]) || "v5-delayed-effective".equals(values[0])) && !values[1].isBlank()) {
            processInstanceId = values[1];
            values = java.util.Arrays.copyOfRange(values, 2, 14);
        } else if (values.length != 12) {
            throw new IllegalArgumentException("DCC_SIGNATURE_SUBJECT_INVALID");
        }
        return new DecodedSubject(values[0], values[1], values[2], values[3], values[4], values[5],
                values[6], values[7], values[8], values[9], values[10], values[11], processInstanceId,delayedIntent);
    }

    private static Integer nodeOrder(String stageCode,boolean workflowRound) {
        if(workflowRound) {
            return switch(value(stageCode)) {
                case "SUPERVISOR_REVIEW" -> 0;
                case "APPLICANT_REWORK" -> 5;
                case "MATRIX_REVIEW" -> 10;
                case "MATRIX_APPROVAL" -> 20;
                case "DOC_CONTROL_REVIEW" -> 30;
                case "DELAYED_EFFECTIVE_CONFIRM" -> 35;
                case "DISTRIBUTION" -> 40;
                default -> throw new IllegalArgumentException("DCC_WORKFLOW_SIGNATURE_STAGE_UNSUPPORTED");
            };
        }
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
    private void requireDelayedIntent(SignatureSubjectCommand command,DecodedSubject decoded){
        if(!"DELAYED_EFFECTIVE_CONFIRM".equals(decoded.stageCode()) || !"APPROVE".equals(decoded.actionType())
                || !"DELAYED_EFFECTIVE_CONFIRM_APPROVE".equals(decoded.meaningCode()) || !decoded.evidenceHash().isEmpty()
                || !ApprovalSignatureHash.sha256(decoded.delayedIntent()).equals(decoded.canonicalPayloadHash()))
            throw new IllegalArgumentException("延迟签名正式意图canonical摘要或动作身份不一致");
        DccControlledFileSignatureEvidenceCreateReq req;
        try {
            var mapper=new com.fasterxml.jackson.databind.ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            mapper.enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
            mapper.enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            var json=mapper.readTree(decoded.delayedIntent());
            // Current formal producer serializes timestamps explicitly; reject unknown/duplicate fields before transport conversion.
            var allowed=new java.util.HashSet<String>();for(var field:DccControlledFileSignatureEvidenceCreateReq.class.getDeclaredFields())allowed.add(field.getName());
            var fields=json.fieldNames();while(fields.hasNext())if(!allowed.contains(fields.next()))throw new IllegalArgumentException("未知延迟签名意图字段");
            req=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(decoded.delayedIntent(),DccControlledFileSignatureEvidenceCreateReq.class);
        } catch(Exception error){throw new IllegalArgumentException("延迟签名意图包含重复、未知或无效字段",error);}
        if(req==null || req.getSignedAt()!=null || !Objects.equals(req.getTenantId(),cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId())
                || !Objects.equals(req.getSignerUserId(),command.actorId()) || !Objects.equals(String.valueOf(req.getControlledFileId()),decoded.controlledFileId())
                || !Objects.equals(req.getTaskId(),decoded.taskId()) || !Objects.equals(req.getProcessInstanceId(),decoded.processInstanceId())
                || !Objects.equals(req.getMeaningCode(),decoded.meaningCode()) || !"APPROVED".equals(req.getTaskActionResult())
                || !Objects.equals(command.reason(),req.getReasonText()))throw new IllegalArgumentException("延迟签名意图租户、办理人、版本或原因身份不一致");
        var regenerated=signatureEvidenceService.createDelayedSigningIntent(req);
        if(!Objects.equals(regenerated.getCanonicalPayload(),decoded.delayedIntent()) || !Objects.equals(regenerated.getSourceFileHash(),decoded.sourceFileHash())
                || !Objects.equals(regenerated.getRecordHashSnapshot(),decoded.recordHashSnapshot()) || !Objects.equals(String.valueOf(regenerated.getRevisionId()),decoded.revisionId())
                || !Objects.equals(regenerated.getVersionNo(),decoded.versionNo()))throw new IllegalArgumentException("延迟签名正式意图与当前源文件/版本或完整快照不一致");
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
                                  String canonicalPayloadHash, String processInstanceId,String delayedIntent) {
    }

}
