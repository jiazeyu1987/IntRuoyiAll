package cn.iocoder.yudao.module.bpm.approval.service.ownership;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.api.task.dto.ApprovalProcessAuthorityDTO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.approval.BpmApprovalProcessOwnershipDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.approval.BpmApprovalProcessOwnershipMapper;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class ApprovalProcessOwnershipService {
    private final BpmApprovalProcessOwnershipMapper mapper;
    public ApprovalProcessOwnershipService(BpmApprovalProcessOwnershipMapper mapper) { this.mapper = mapper; }

    @Transactional(propagation=Propagation.MANDATORY)
    public void record(ProcessInstance process, ProcessDefinition definition, ApprovalProcessAuthorityDTO authority,
            Map<String,List<Long>> selected) {
        ApprovalProcessOwnershipRegistry.require(authority);
        Long tenant = TenantContextHolder.getRequiredTenantId();
        if (!TransactionSynchronizationManager.isActualTransactionActive() || process == null || definition == null
                || !Objects.equals(tenant, authority.tenantId())
                || !Objects.equals(tenant.toString(), process.getTenantId())
                || !Objects.equals(process.getProcessDefinitionId(), definition.getId())
                || !Objects.equals(process.getBusinessKey(), authority.expectedBusinessKey()))
            throw new IllegalStateException("APPROVAL_OWNERSHIP_ENGINE_AUTHORITY_MISMATCH");
        ApprovalProcessOwnershipRegistry.text(process.getId(),128);
        ApprovalProcessOwnershipRegistry.text(definition.getId(),128);
        ApprovalProcessOwnershipRegistry.text(definition.getKey(),128);
        var row = new BpmApprovalProcessOwnershipDO(); row.setTenantId(tenant); row.setProcessInstanceId(process.getId());
        row.setProcessDefinitionId(definition.getId()); row.setProcessDefinitionKey(definition.getKey());
        row.setBusinessKey(process.getBusinessKey()); row.setProjectionCode(authority.projectionCode());
        row.setAuthorityType(authority.authorityType()); row.setAuthorityId(authority.authorityId());
        row.setAuthorityVersion(authority.authorityVersion()); row.setWriterCode(authority.writerCode());
        row.setCreatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)); row.setEvidenceDigest(digest(row));
        var users=selectedUsers(authority,selected);
        var old = mapper.selectExact(tenant,process.getId());
        if (old != null) {
            verify(old); row.setCreatedAt(old.getCreatedAt());
            if (!Objects.equals(old.getEvidenceDigest(),digest(row))) throw new IllegalStateException("APPROVAL_OWNERSHIP_CONFLICT");
            if (!Objects.equals(new ArrayList<>(users),mapper.selectSelectedCandidates(tenant,process.getId())))
                throw new IllegalStateException("APPROVAL_SELECTED_CANDIDATES_CONFLICT");
            return;
        }
        if (mapper.insert(row) != 1 || row.getId() == null) throw new IllegalStateException("APPROVAL_OWNERSHIP_INSERT_FAILED");
        var persisted = mapper.selectExact(tenant,process.getId()); verify(persisted);
        if (!Objects.equals(row.getEvidenceDigest(),persisted.getEvidenceDigest()))
            throw new IllegalStateException("APPROVAL_OWNERSHIP_READBACK_MISMATCH");
        if ("REGISTRATION_CERTIFICATE_SUBMIT".equals(authority.writerCode())) {
            for (Long user : users) if (mapper.insertSelectedCandidate(tenant,process.getId(),user) != 1)
                throw new IllegalStateException("APPROVAL_SELECTED_CANDIDATE_INSERT_FAILED");
            if (!Objects.equals(new ArrayList<>(users),mapper.selectSelectedCandidates(tenant,process.getId())))
                throw new IllegalStateException("APPROVAL_SELECTED_CANDIDATE_READBACK_MISMATCH");
        }
    }

    private static SortedSet<Long> selectedUsers(ApprovalProcessAuthorityDTO authority,Map<String,List<Long>> selected) {
        var users=new TreeSet<Long>();
        if(!"REGISTRATION_CERTIFICATE_SUBMIT".equals(authority.writerCode())) return users;
        if(selected==null || selected.isEmpty()) throw new IllegalStateException("APPROVAL_SELECTED_CANDIDATES_REQUIRED");
        for(var values:selected.values()) {
            if(values==null || values.stream().anyMatch(id->id==null || id<=0))
                throw new IllegalStateException("APPROVAL_SELECTED_CANDIDATES_REQUIRED");
            users.addAll(values);
        }
        if(users.isEmpty()) throw new IllegalStateException("APPROVAL_SELECTED_CANDIDATES_REQUIRED");
        return users;
    }

    public BpmApprovalProcessOwnershipDO requireExact(Long tenant, String process, String definition, String key, String businessKey) {
        var row = mapper.selectExact(tenant,process); verify(row);
        if (!Objects.equals(tenant,row.getTenantId()) || !Objects.equals(process,row.getProcessInstanceId())
                || !Objects.equals(definition,row.getProcessDefinitionId()) || !Objects.equals(key,row.getProcessDefinitionKey())
                || !Objects.equals(businessKey,row.getBusinessKey())) throw new IllegalStateException("APPROVAL_OWNERSHIP_ENGINE_MISMATCH");
        return row;
    }

    public void verifyRegistrationSnapshot(BpmApprovalProcessOwnershipDO row) {
        if(!"REGISTRATION_REQUEST".equals(row.getAuthorityType())) return;
        var anchors=Objects.requireNonNull(mapper.selectRegistrationAnchors(row.getTenantId(),row.getProcessInstanceId()),
                "APPROVAL_REGISTRATION_ANCHORS_REQUIRED");
        if(anchors.size()!=1) throw new IllegalStateException("APPROVAL_REGISTRATION_FORMAL_RELATION_REQUIRED");
        var anchor=anchors.get(0);
        if(anchor.getRequestId()==null || !Objects.equals(row.getAuthorityId(),anchor.getRequestId().toString())
                || anchor.getRequestKey()==null || !Objects.equals(row.getAuthorityVersion(),DigestUtil.sha256Hex(anchor.getRequestKey()))
                || !Objects.equals(row.getBusinessKey(),anchor.getBusinessKey()))
            throw new IllegalStateException("APPROVAL_REGISTRATION_AUTHORITY_MISMATCH");
        var detail=JsonUtils.parseTree(anchor.getDetailJson());
        if(detail==null || !detail.isObject() || !detail.path("candidateUserIds").isArray()
                || detail.path("candidateUserIds").isEmpty()) throw new IllegalStateException("APPROVAL_REGISTRATION_CANDIDATE_SNAPSHOT_REQUIRED");
        var snapshot=new TreeSet<Long>();
        for(var id:detail.path("candidateUserIds")) {
            String text=id.isIntegralNumber() && id.canConvertToLong() ? id.asText()
                    : id.isTextual() ? id.textValue() : null;
            if(text==null || !text.matches("[1-9][0-9]*")) throw new IllegalStateException("APPROVAL_REGISTRATION_CANDIDATE_INVALID");
            long value=Long.parseLong(text);
            if(!snapshot.add(value)) throw new IllegalStateException("APPROVAL_REGISTRATION_CANDIDATE_DUPLICATE");
        }
        if(!Objects.equals(new ArrayList<>(snapshot),mapper.selectSelectedCandidates(row.getTenantId(),row.getProcessInstanceId())))
            throw new IllegalStateException("APPROVAL_REGISTRATION_CANDIDATE_SNAPSHOT_MISMATCH");
    }

    public static void verify(BpmApprovalProcessOwnershipDO row) {
        if (row == null || row.getCreatedAt() == null || row.getId() == null || row.getId() <= 0)
            throw new IllegalStateException("APPROVAL_OWNERSHIP_PROVENANCE_MISSING");
        ApprovalProcessOwnershipRegistry.require(new ApprovalProcessAuthorityDTO(row.getTenantId(),row.getProjectionCode(),
                row.getAuthorityType(),row.getAuthorityId(),row.getAuthorityVersion(),row.getWriterCode(),row.getBusinessKey()));
        ApprovalProcessOwnershipRegistry.text(row.getProcessInstanceId(),128);
        ApprovalProcessOwnershipRegistry.text(row.getProcessDefinitionId(),128);
        ApprovalProcessOwnershipRegistry.text(row.getProcessDefinitionKey(),128);
        if (!Objects.equals(row.getEvidenceDigest(),digest(row))) throw new IllegalStateException("APPROVAL_OWNERSHIP_FACT_TAMPERED");
    }

    static String digest(BpmApprovalProcessOwnershipDO row) {
        var values = new TreeMap<String,Object>(); values.put("tenant",row.getTenantId()); values.put("process",row.getProcessInstanceId());
        values.put("definition",row.getProcessDefinitionId()); values.put("definitionKey",row.getProcessDefinitionKey());
        values.put("businessKey",row.getBusinessKey()); values.put("projection",row.getProjectionCode());
        values.put("authorityType",row.getAuthorityType()); values.put("authorityId",row.getAuthorityId());
        values.put("authorityVersion",row.getAuthorityVersion()); values.put("createdAt",row.getCreatedAt().toString());
        values.put("writer",row.getWriterCode()); return DigestUtil.sha256Hex(JsonUtils.toJsonString(values));
    }
}
