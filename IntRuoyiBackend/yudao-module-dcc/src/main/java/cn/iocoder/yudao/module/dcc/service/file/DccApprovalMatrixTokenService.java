package cn.iocoder.yudao.module.dcc.service.file;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Technical baseline/confirmation challenge, neither business approval nor electronic signature. */
@Service
public class DccApprovalMatrixTokenService {
    @Resource private DccApprovalMatrixTokenProperties properties;
    // BEGIN APPLICATION_CUSTOM TOKEN CONTRACT
    private java.time.Clock customClock = java.time.Clock.systemUTC();
    public static final long CUSTOM_TOKEN_LIFETIME_SECONDS = 900;
    private static final String CUSTOM_CONTRACT = "DCC_APPLICATION_CUSTOM_TOKEN_V1";
    private static final String CUSTOM_CONFIG = "APPLICATION_CUSTOM_CONFIGURATION";
    private static final String CUSTOM_SUPERVISOR = "APPLICATION_CUSTOM_NATIVE_SUPERVISOR";
    private static final Set<String> CUSTOM_FIELDS = Set.of("version","contractVersion","purpose","phase","sourceKind","canonical","canonicalDigest","issuedAt","expiresAt");
    private static final com.fasterxml.jackson.databind.ObjectMapper CUSTOM_JSON = new com.fasterxml.jackson.databind.ObjectMapper(
            new com.fasterxml.jackson.core.JsonFactory().enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION))
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private final DccApplicationCustomMatrixService customMatrices = new DccApplicationCustomMatrixService();
    public record CustomSupervisorProof(Map<String,Object> payload,String sourceDigest,String sourceToken) {
        public CustomSupervisorProof { payload=Collections.unmodifiableMap(new TreeMap<>(payload)); }
    }
    public String issueCustomPreview(DccApplicationCustomMatrixService.PreviewSourceContext context,DccApplicationCustomMatrixService.CanonicalSelection selection){
        customPreviewSelection(context,selection);return sign(customPayload(CUSTOM_CONFIG,"PREVIEW",selection));
    }
    public void requireCustomPreview(DccApplicationCustomMatrixService.PreviewSourceContext context,DccApplicationCustomMatrixService.CanonicalSelection selection,String token){
        customPreviewSelection(context,selection);requireCustomToken(CUSTOM_CONFIG,"PREVIEW",selection,token);
    }
    public String issueCustomReserved(DccApplicationCustomMatrixService.SourceContext context,DccApplicationCustomMatrixService.CanonicalSelection selection){
        customReservedSelection(context,selection);return sign(customPayload(CUSTOM_CONFIG,"RESERVED",selection));
    }
    public void requireCustomReserved(DccApplicationCustomMatrixService.SourceContext context,DccApplicationCustomMatrixService.CanonicalSelection selection,String token){
        customReservedSelection(context,selection);requireCustomToken(CUSTOM_CONFIG,"RESERVED",selection,token);
    }
    public CustomSupervisorProof issueCustomSupervisorPreview(DccApplicationCustomMatrixService.PreviewSourceContext context,DccApplicationCustomMatrixService.CanonicalSelection selection,DccDirectSupervisorSource.Evidence evidence){
        var actual=customPreviewSelection(context,selection);customMatrices.compile(context.approvalSourceMode(),actual.applicationCustomMatrix(),actual.selectedSignoffDepartmentIds());
        requireCustomEvidence(evidence,context.tenantId(),context.requesterId());return customSupervisorProof("PREVIEW",selection,evidence);
    }
    public CustomSupervisorProof issueCustomSupervisorReserved(DccApplicationCustomMatrixService.SourceContext context,DccApplicationCustomMatrixService.CanonicalSelection selection,DccDirectSupervisorSource.Evidence evidence){
        var actual=customReservedSelection(context,selection);customMatrices.compile(context.approvalSourceMode(),actual.applicationCustomMatrix(),actual.selectedSignoffDepartmentIds());
        requireCustomEvidence(evidence,context.tenantId(),context.requesterId());return customSupervisorProof("RESERVED",selection,evidence);
    }
    public void requireCustomSupervisorPreview(DccApplicationCustomMatrixService.PreviewSourceContext context,DccApplicationCustomMatrixService.CanonicalSelection selection,DccDirectSupervisorSource.Evidence evidence,String digest,String token){
        customPreviewSelection(context,selection);requireCustomEvidence(evidence,context.tenantId(),context.requesterId());requireCustomSupervisor("PREVIEW",selection,evidence,digest,token);
    }
    public void requireCustomSupervisorReserved(DccApplicationCustomMatrixService.SourceContext context,DccApplicationCustomMatrixService.CanonicalSelection selection,DccDirectSupervisorSource.Evidence evidence,String digest,String token){
        customReservedSelection(context,selection);requireCustomEvidence(evidence,context.tenantId(),context.requesterId());requireCustomSupervisor("RESERVED",selection,evidence,digest,token);
    }
    /** Only for already-frozen historical provenance; never authorizes a new submission or bypasses current readiness. */
    public void requireRecordedCustomSupervisorReserved(DccApplicationCustomMatrixService.SourceContext context,DccApplicationCustomMatrixService.CanonicalSelection selection,DccDirectSupervisorSource.Evidence evidence,String digest,String token){
        customReservedSelection(context,selection);requireCustomEvidence(evidence,context.tenantId(),context.requesterId());
        var payload=requireCustomToken(CUSTOM_SUPERVISOR,"RESERVED",selection,token,false);
        if(digest==null||!digest.matches("[a-f0-9]{64}")||!Objects.equals(digest,nativeDigest(payload))||!Objects.equals(payload.get("evidence"),evidenceValue(evidence)))throw customInvalid();
    }
    private DccApplicationCustomMatrixService.SavedSelection customPreviewSelection(DccApplicationCustomMatrixService.PreviewSourceContext context,DccApplicationCustomMatrixService.CanonicalSelection selection){
        if(selection==null)throw customInvalid();return customMatrices.readPreviewCanonical(selection.canonical(),selection.digest(),context);
    }
    private DccApplicationCustomMatrixService.SavedSelection customReservedSelection(DccApplicationCustomMatrixService.SourceContext context,DccApplicationCustomMatrixService.CanonicalSelection selection){
        if(selection==null)throw customInvalid();return customMatrices.readCanonical(selection.canonical(),selection.digest(),context);
    }
    private Map<String,Object> customPayload(String purpose,String phase,DccApplicationCustomMatrixService.CanonicalSelection selection){
        var now=java.time.Instant.ofEpochSecond(customClock.instant().getEpochSecond());var payload=new TreeMap<String,Object>();
        payload.put("version","DCC_MATRIX_TOKEN_V1");payload.put("contractVersion",CUSTOM_CONTRACT);payload.put("purpose",purpose);
        payload.put("phase",phase);payload.put("sourceKind","APPLICATION_CUSTOM");payload.put("canonical",selection.canonical());payload.put("canonicalDigest",selection.digest());
        payload.put("issuedAt",now.toString());payload.put("expiresAt",now.plusSeconds(CUSTOM_TOKEN_LIFETIME_SECONDS).toString());return payload;
    }
    private CustomSupervisorProof customSupervisorProof(String phase,DccApplicationCustomMatrixService.CanonicalSelection selection,DccDirectSupervisorSource.Evidence evidence){
        var payload=customPayload(CUSTOM_SUPERVISOR,phase,selection);payload.put("evidence",Collections.unmodifiableMap(evidenceValue(evidence)));
        return new CustomSupervisorProof(payload,nativeDigest(payload),sign(payload));
    }
    private void requireCustomSupervisor(String phase,DccApplicationCustomMatrixService.CanonicalSelection selection,DccDirectSupervisorSource.Evidence evidence,String digest,String token){
        var payload=requireCustomToken(CUSTOM_SUPERVISOR,phase,selection,token);
        if(digest==null||!digest.matches("[a-f0-9]{64}")||!Objects.equals(digest,nativeDigest(payload))
                ||!Objects.equals(payload.get("evidence"),evidenceValue(evidence)))throw customInvalid();
    }
    private Map<String,Object> requireCustomToken(String purpose,String phase,DccApplicationCustomMatrixService.CanonicalSelection selection,String token){
        return requireCustomToken(purpose,phase,selection,token,true);
    }
    private Map<String,Object> requireCustomToken(String purpose,String phase,DccApplicationCustomMatrixService.CanonicalSelection selection,String token,boolean current){
        properties.requireSigningKey();
        if(token==null||token.isBlank()||token.length()>131072)throw customInvalid();var parts=token.split("\\.",-1);if(parts.length!=2)throw customInvalid();
        final byte[] encoded;final String raw;
        try{
            encoded=Base64.getUrlDecoder().decode(parts[0]);var signature=Base64.getUrlDecoder().decode(parts[1]);
            if(!Objects.equals(parts[0],Base64.getUrlEncoder().withoutPadding().encodeToString(encoded))
                    ||!Objects.equals(parts[1],Base64.getUrlEncoder().withoutPadding().encodeToString(signature))
                    ||!MessageDigest.isEqual(mac(parts[0]),signature))throw customInvalid();
            raw=StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                    .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(encoded)).toString();
        }catch(IllegalArgumentException|java.nio.charset.CharacterCodingException malformed){throw customInvalid();}
        final Map<String,Object> payload;
        try{
            var tree=CUSTOM_JSON.readTree(raw);if(tree==null||!tree.isObject())throw customInvalid();
            var fields=new HashSet<String>();tree.fieldNames().forEachRemaining(fields::add);var expected=new HashSet<>(CUSTOM_FIELDS);if(CUSTOM_SUPERVISOR.equals(purpose))expected.add("evidence");
            if(!fields.equals(expected))throw customInvalid();
            payload=CUSTOM_JSON.convertValue(tree,new com.fasterxml.jackson.core.type.TypeReference<TreeMap<String,Object>>(){});
        }catch(com.fasterxml.jackson.core.JsonProcessingException|IllegalArgumentException malformed){throw customInvalid();}
        if(!"DCC_MATRIX_TOKEN_V1".equals(payload.get("version"))||!CUSTOM_CONTRACT.equals(payload.get("contractVersion"))
                ||!purpose.equals(payload.get("purpose"))||!phase.equals(payload.get("phase"))||!"APPLICATION_CUSTOM".equals(payload.get("sourceKind"))
                ||!Objects.equals(selection.canonical(),payload.get("canonical"))||!Objects.equals(selection.digest(),payload.get("canonicalDigest"))
                ||!raw.equals(JsonUtils.toJsonString(new TreeMap<>(payload))))throw customInvalid();
        try{
            if(!(payload.get("issuedAt") instanceof String issued)||!(payload.get("expiresAt") instanceof String expires))throw customInvalid();
            var start=java.time.Instant.parse(issued);var end=java.time.Instant.parse(expires);var now=customClock.instant();
            if(!issued.equals(start.toString())||!expires.equals(end.toString())||start.getNano()!=0||end.getNano()!=0
                    ||!end.equals(start.plusSeconds(CUSTOM_TOKEN_LIFETIME_SECONDS))||now.isBefore(start)||current&&!now.isBefore(end))throw customInvalid();
        }catch(java.time.DateTimeException malformed){throw customInvalid();}
        return payload;
    }
    private void requireCustomEvidence(DccDirectSupervisorSource.Evidence evidence,Long tenant,Long requester){
        if(evidence==null||!Objects.equals(evidence.tenantId(),tenant)||!Objects.equals(evidence.requesterId(),requester)
                ||evidence.supervisorUserId()==null||evidence.supervisorUserId()<=0||!customSourceText(evidence.sourceSystem())
                ||!customSourceText(evidence.sourceIdentity())||!customSourceText(evidence.sourceVersion()))throw customInvalid();
    }
    private boolean customSourceText(String text){
        if(text==null||text.isBlank()||!text.equals(text.strip())||text.length()>1024)return false;
        for(int i=0;i<text.length();i++){char c=text.charAt(i);if(Character.isISOControl(c))return false;if(Character.isHighSurrogate(c)){if(++i>=text.length()||!Character.isLowSurrogate(text.charAt(i)))return false;}else if(Character.isLowSurrogate(c))return false;}return true;
    }
    private IllegalArgumentException customInvalid(){return new IllegalArgumentException("APPLICATION_CUSTOM_TOKEN_INVALID：本次自定义配置或主管来源凭证无效、已过期或身份不一致");}
    // END APPLICATION_CUSTOM TOKEN CONTRACT
    public record Context(Long tenantId,Long actorId,Long projectId,Long categoryId,Long taxonomyId,String actionType,Long fileId,String applicationContextKey){}
    public record NativeSupervisorProof(Map<String,Object> payload,String sourceDigest,String sourceToken) {}
    public NativeSupervisorProof issueNativeSupervisor(Context context,String matrixDigest,String exactId,String exactDigest,List<Long> departments,DccDirectSupervisorSource.Evidence evidence){
        validate(context);if(!Set.of("NEW","REVISION").contains(context.actionType())||matrixDigest==null||!matrixDigest.matches("[a-f0-9]{64}")||exactId==null||exactId.isBlank()||!exactId.equals(exactId.strip())||exactDigest==null||!exactDigest.matches("[a-f0-9]{64}")
                ||departments==null||departments.isEmpty()||departments.stream().anyMatch(id->id==null||id<=0)||departments.stream().distinct().count()!=departments.size()||evidence==null)throw nativeInvalid();
        var payload=new TreeMap<String,Object>();payload.put("version","DCC_MATRIX_TOKEN_V1");payload.put("purpose","NATIVE_SUPERVISOR_SOURCE");payload.put("context",contextValue(context));payload.put("matrixDigest",matrixDigest);payload.put("exactDefinitionId",exactId);payload.put("exactDefinitionDigest",exactDigest);payload.put("selectedDepartments",departments.stream().map(String::valueOf).toList());payload.put("evidence",evidenceValue(evidence));
        String digest=nativeDigest(payload);return new NativeSupervisorProof(Collections.unmodifiableMap(payload),digest,sign(payload));
    }
    public void requireNativeSupervisor(NativeSupervisorProof actual,String expectedDigest,String token){
        var saved=verify(token);if(actual==null||expectedDigest==null||!expectedDigest.matches("[a-f0-9]{64}")||!"NATIVE_SUPERVISOR_SOURCE".equals(saved.get("purpose"))||!Objects.equals(nativeDigest(saved),expectedDigest)||!Objects.equals(actual.sourceDigest(),expectedDigest)
                ||!Objects.equals(JsonUtils.toJsonString(actual.payload()),JsonUtils.toJsonString(new TreeMap<>(saved))))throw nativeInvalid();
    }
    public Map<String,Object> verifyRecordedNativeSupervisor(String expectedDigest,String token){
        var saved=verify(token);if(!"NATIVE_SUPERVISOR_SOURCE".equals(saved.get("purpose"))||expectedDigest==null||!Objects.equals(expectedDigest,nativeDigest(saved)))throw nativeInvalid();return saved;
    }
    public void requireRecordedNativeSupervisor(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO file,cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccApprovalMatrixBaselineVO baseline,List<DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode> nodes,Map<String,Object> frozen){
        if(file==null||baseline==null||frozen==null||!(frozen.get("sourceDigest") instanceof String digest)||!(frozen.get("sourceToken") instanceof String token)||!(frozen.get("payload") instanceof Map<?,?>))throw nativeInvalid();
        var payload=verifyRecordedNativeSupervisor(digest,token);if(!Objects.equals(JsonUtils.toJsonString(new TreeMap<>(payload)),JsonUtils.toJsonString(new TreeMap<>((Map<String,Object>)frozen.get("payload"))))||!(payload.get("context") instanceof List<?> context)||context.size()!=8||!(payload.get("evidence") instanceof Map<?,?> evidence))throw nativeInvalid();
        if(!Objects.equals(context.get(0),file.getTenantId().toString())||!Objects.equals(context.get(1),String.valueOf(file.getSubmitterId()))||!Objects.equals(context.get(2),file.getDccProjectCodeId().toString())||!Objects.equals(context.get(3),file.getCategoryId().toString())||!Objects.equals(context.get(4),file.getFileTypeTaxonomyId().toString())||!Objects.equals(context.get(5),file.getChangeType())
                ||context.get(6)!=null&&!Objects.equals(context.get(6),file.getId().toString())&&!Objects.equals(context.get(6),String.valueOf(file.getSelectedIterationControlledFileId()))
                ||!Objects.equals(payload.get("matrixDigest"),baseline.getMatrixDigest())||!Objects.equals(payload.get("exactDefinitionId"),file.getPreparedProcessDefinitionId())||!Objects.equals(payload.get("exactDefinitionDigest"),file.getPreparedDefinitionDigest())||!Objects.equals(evidence.get("tenantId"),context.get(0))||!Objects.equals(evidence.get("requesterId"),context.get(1)))throw nativeInvalid();
        var supervisor=nodes.stream().filter(n->"SUPERVISOR_REVIEW".equals(n.stageCode())).toList();var signoff=nodes.stream().filter(n->"MATRIX_REVIEW".equals(n.stageCode())).toList();
        if(supervisor.size()!=1||signoff.size()!=1||supervisor.get(0).resolvedUserIds().size()!=1||!Objects.equals(supervisor.get(0).resolvedUserIds().get(0).toString(),evidence.get("supervisorUserId"))||!Objects.equals(signoff.get(0).candidateSourceIds().stream().map(String::valueOf).toList(),payload.get("selectedDepartments")))throw nativeInvalid();
    }
    private Map<String,Object> evidenceValue(DccDirectSupervisorSource.Evidence evidence){return new TreeMap<>(Map.of("tenantId",evidence.tenantId().toString(),"requesterId",evidence.requesterId().toString(),"supervisorUserId",evidence.supervisorUserId().toString(),"sourceSystem",evidence.sourceSystem(),"sourceIdentity",evidence.sourceIdentity(),"sourceVersion",evidence.sourceVersion()));}
    private String nativeDigest(Map<String,Object> payload){return cn.hutool.crypto.digest.DigestUtil.sha256Hex(JsonUtils.toJsonString(new TreeMap<>(payload)));}
    private IllegalArgumentException nativeInvalid(){return new IllegalArgumentException("NATIVE_SUPERVISOR_SOURCE_EXPECTATION_INVALID");}
    public String issueBaseline(Context context,String canonical){var payload=payload("BASELINE",context,canonical);return sign(payload);}
    public String verifyBaseline(Context context,String token){var payload=verify(token);requireContext(payload,"BASELINE",context);String canonical=(String)payload.get("baseline");DccApprovalMatrixCanonicalizer.canonicalize(JsonUtils.parseObject(canonical,Map.class));return canonical;}
    public String issueConfirmation(Context context,String canonical,Long id,Integer version,String digest){requireExpected(id,version,digest);var payload=payload("CONFIRMATION",context,canonical);payload.put("afterId",String.valueOf(id));payload.put("afterVersion",version);payload.put("afterDigest",digest);return sign(payload);}
    public void requireConfirmation(Context context,String token,String canonical,Long id,Integer version,String digest){requireExpected(id,version,digest);var payload=verify(token);requireContext(payload,"CONFIRMATION",context);if(!(payload.get("afterVersion") instanceof Number actualVersion)||!Objects.equals(canonical,payload.get("baseline"))||!Objects.equals(String.valueOf(id),payload.get("afterId"))||version!=actualVersion.intValue()||!Objects.equals(digest,payload.get("afterDigest")))throw invalid();}
    private Map<String,Object> payload(String purpose,Context context,String canonical){validate(context);if(!Objects.equals(canonical,DccApprovalMatrixCanonicalizer.canonicalize(JsonUtils.parseObject(canonical,Map.class))))throw invalid();var payload=new TreeMap<String,Object>();payload.put("version","DCC_MATRIX_TOKEN_V1");payload.put("purpose",purpose);payload.put("context",contextValue(context));payload.put("baseline",canonical);return payload;}
    private String sign(Map<String,Object> payload){String encoded=Base64.getUrlEncoder().withoutPadding().encodeToString(JsonUtils.toJsonString(payload).getBytes(StandardCharsets.UTF_8));return encoded+"."+Base64.getUrlEncoder().withoutPadding().encodeToString(mac(encoded));}
    private Map<String,Object> verify(String token){properties.requireSigningKey();if(token==null||token.isBlank()||token.length()>131072)throw invalid();String[] parts=token.split("\\.",-1);if(parts.length!=2)throw invalid();try{if(!MessageDigest.isEqual(mac(parts[0]),Base64.getUrlDecoder().decode(parts[1])))throw invalid();var payload=JsonUtils.parseObject(new String(Base64.getUrlDecoder().decode(parts[0]),StandardCharsets.UTF_8),Map.class);if(payload==null||!"DCC_MATRIX_TOKEN_V1".equals(payload.get("version")))throw invalid();return payload;}catch(IllegalArgumentException malformed){throw invalid();}}
    private byte[] mac(String input){try{var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(properties.requireSigningKey(),"HmacSHA256"));return mac.doFinal(input.getBytes(StandardCharsets.UTF_8));}catch(java.security.GeneralSecurityException unavailable){throw new IllegalStateException("matrix confirmation signing dependency unavailable",unavailable);}}
    private void requireContext(Map<String,Object> payload,String purpose,Context context){validate(context);if(!purpose.equals(payload.get("purpose"))||!Objects.equals(contextValue(context),payload.get("context")))throw invalid();}
    private List<Object> contextValue(Context c){return Arrays.asList(String.valueOf(c.tenantId()),String.valueOf(c.actorId()),String.valueOf(c.projectId()),String.valueOf(c.categoryId()),String.valueOf(c.taxonomyId()),c.actionType(),c.fileId()==null?null:String.valueOf(c.fileId()),c.applicationContextKey());}
    private void validate(Context c){if(c==null||c.tenantId()==null||c.tenantId()<=0||c.actorId()==null||c.actorId()<=0||c.projectId()==null||c.projectId()<=0||c.categoryId()==null||c.categoryId()<=0||c.taxonomyId()==null||c.taxonomyId()<=0||c.fileId()!=null&&c.fileId()<=0||!Set.of("NEW","REVISION","OBSOLETE").contains(c.actionType()==null?"":c.actionType())||c.applicationContextKey()==null||c.applicationContextKey().isBlank()||c.applicationContextKey().length()>128)throw invalid();}
    private void requireExpected(Long id,Integer version,String digest){if(id==null||id<=0||version==null||version<=0||digest==null||!digest.matches("[a-f0-9]{64}"))throw invalid();}
    private IllegalArgumentException invalid(){return new IllegalArgumentException("APPROVAL_MATRIX_BASELINE_OR_CONFIRMATION_TOKEN_INVALID");}
}
