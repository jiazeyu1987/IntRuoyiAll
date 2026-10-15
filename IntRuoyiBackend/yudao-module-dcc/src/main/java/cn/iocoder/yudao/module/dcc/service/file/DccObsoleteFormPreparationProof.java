package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Leaf technical proof for independent server-prepared form JSON. No preparation/lifecycle/BPM dependency. */
@Service
public class DccObsoleteFormPreparationProof {
    private static final String PURPOSE="DCC_OBSOLETE_FORM_PREPARATION_V1";
    @Resource private DccApprovalMatrixTokenProperties signing;
    public String sign(Map<String,Object> payload){return Base64.getUrlEncoder().withoutPadding().encodeToString(mac(PURPOSE+"\n"+canonical(payload)));}
    public void verify(Map<String,Object> payload,String token){try{if(!MessageDigest.isEqual(mac(PURPOSE+"\n"+canonical(payload)),Base64.getUrlDecoder().decode(token)))throw invalid();}catch(IllegalArgumentException malformed){throw invalid();}}
    public DccWorkflowDefinitionCatalog.Graph verifyRecorded(DccControlledFileDO file,FormActionInstanceDO form,Map<?,?> source,String exactId,String exactDigest){
        var payload=new TreeMap<String,Object>((Map<String,Object>)source);Object token=payload.remove("authorizationToken");if(!(token instanceof String text))throw invalid();verify(payload,text);
        boolean v1=PURPOSE.equals(payload.get("formatVersion"))&&"UNBOUND_FORM_DRAFT".equals(payload.get("attributeState"));
        boolean v2="DCC_OBSOLETE_FORM_PREPARATION_V2".equals(payload.get("formatVersion"))&&"RESERVED_FORM_DRAFT".equals(payload.get("attributeState"));
        if(v2&&(!(payload.get("attributeRound") instanceof Number round)||round.intValue()<=0||!(payload.get("defaultSourceJson") instanceof String)||!(payload.get("actualAttributesJson") instanceof String)
                ||!Objects.equals(cn.hutool.crypto.digest.DigestUtil.sha256Hex(canonical(Arrays.asList(payload.get("attributeRound"),payload.get("defaultSourceJson"),payload.get("actualAttributesJson")))),payload.get("attributeDigest"))))throw invalid();
        if(!(v1||v2)||!Objects.equals(file.getTenantId().toString(),payload.get("tenantId"))||!Objects.equals(form.getId().toString(),payload.get("formInstanceId"))
                ||!Objects.equals(form.getInstanceCode(),payload.get("formInstanceCode"))||!Objects.equals(file.getId().toString(),payload.get("controlledFileId"))
                ||!Objects.equals(file.getVersionNo(),payload.get("controlledFileVersionNo"))||!Objects.equals(form.getApplicantUserId().toString(),payload.get("applicantUserId"))
                ||!Objects.equals(file.getDccProjectCodeId().toString(),payload.get("projectId"))||!Objects.equals(exactId,payload.get("preparedProcessDefinitionId"))||!Objects.equals(exactDigest,payload.get("preparedDefinitionDigest")))throw invalid();
        if(!(payload.get("sourceApprovalMatrixJson") instanceof String raw)||!(payload.get("runtimeApprovalNodesJson") instanceof String runtime)||!(payload.get("supervisorProvenance") instanceof Map<?,?> org))throw invalid();
        var baseline=DccApprovalMatrixSourceService.readCanonical(raw);if(!Objects.equals(file.getTenantId(),baseline.getTenantId())||!Objects.equals(file.getCategoryId(),baseline.getCategoryId())
                ||!Objects.equals(file.getFileTypeTaxonomyId(),baseline.getFileTypeTaxonomyId())||!Objects.equals(baseline.getMatrixDigest(),payload.get("sourceApprovalMatrixDigest")))throw invalid();
        if(!Objects.equals(file.getTenantId().toString(),org.get("tenantId"))||!Objects.equals(form.getApplicantUserId().toString(),org.get("requesterId"))||org.get("sourceSystem")==null||org.get("sourceIdentity")==null||org.get("sourceVersion")==null
                ||!(org.get("supervisorUserId") instanceof String person)||!person.matches("[1-9][0-9]*"))throw invalid();
        var document=JsonUtils.parseObject(runtime,Map.class);if(document==null||!"DCC_RUNTIME_APPROVAL_NODES_V1".equals(document.get("formatVersion"))||!(document.get("groups") instanceof List<?>))throw invalid();
        var nodes=JsonUtils.parseArray(JsonUtils.toJsonString(document.get("nodes")),DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode.class);
        if(nodes==null||!nodes.stream().map(DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode::stageCode).toList().equals(List.of("SUPERVISOR_REVIEW","MATRIX_REVIEW","MATRIX_APPROVAL"))
                ||!nodes.get(0).resolvedUserIds().equals(List.of(Long.valueOf(person))))throw invalid();
        var business=JsonUtils.parseObject(form.getFormDataJson(),Map.class);business.remove("dccExecutionPreparation");if(!Objects.equals(cn.hutool.crypto.digest.DigestUtil.sha256Hex(canonical(business)),payload.get("businessPayloadDigest")))throw invalid();
        return DccWorkflowDefinitionCatalog.require((String)payload.get("executionContract"),DccControlledFileProcessDefinitionKeys.OBSOLETE,exactDigest);
    }
    public String canonical(Object object){return JsonUtils.toJsonString(sorted(object));}
    private Object sorted(Object input){if(input instanceof Map<?,?> map){var result=new TreeMap<String,Object>();map.forEach((k,v)->result.put(k.toString(),sorted(v)));return result;}if(input instanceof List<?> list)return list.stream().map(this::sorted).toList();return input;}
    private byte[] mac(String text){try{var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(signing.requireSigningKey(),"HmacSHA256"));return mac.doFinal(text.getBytes(StandardCharsets.UTF_8));}catch(java.security.GeneralSecurityException failure){throw new IllegalStateException("DCC_OBSOLETE_PREPARATION_SIGNING_UNAVAILABLE",failure);}}
    private IllegalArgumentException invalid(){return new IllegalArgumentException("DCC_OBSOLETE_RECORDED_PREPARATION_PROOF_INVALID");}
}
