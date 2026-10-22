package cn.iocoder.yudao.module.bpm.approval.content;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.dal.dataobject.approval.BpmApprovalReviewPrepareDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.approval.BpmApprovalReviewPrepareMapper;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.UUID;

@Service
public class ApprovalReviewPrepareService {
    private final BpmApprovalReviewPrepareMapper mapper;
    private final java.time.Clock clock;
    public ApprovalReviewPrepareService(BpmApprovalReviewPrepareMapper mapper,java.time.Clock clock) { this.mapper = mapper; this.clock=clock; }
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public BpmApprovalReviewPrepareDO insert(Long actorId, PreparedApprovalContent content) {
        Long tenant = TenantContextHolder.getRequiredTenantId();
        if (actorId == null || actorId <= 0 || !Objects.equals(tenant, content.identity().tenantId())) throw new IllegalArgumentException("APPROVAL_PREPARE_IDENTITY_INVALID");
        var row = new BpmApprovalReviewPrepareDO(); var identity = content.identity();
        row.setTenantId(tenant); row.setActorId(actorId); row.setPrepareId(UUID.randomUUID().toString()); row.setOwnerCode(identity.ownerCode()); row.setSourceTaskType(identity.sourceTaskType()); row.setSourceTaskId(identity.sourceTaskId()); row.setProcessInstanceId(identity.processInstanceId()); row.setSubjectType(identity.domainSubjectType()); row.setSubjectId(identity.domainSubjectId());
        row.setContentPolicyVersion(content.contentPolicyVersion()); row.setSubjectVersion(content.subjectVersion()); row.setPreviewContentHash(content.contentHash()); row.setScopeCode(content.scopeCode()); row.setCanonicalBusinessContentJson(content.canonicalBusinessContentJson());
        row.setPreparedAt(LocalDateTime.ofInstant(clock.instant(),ZoneId.of("Asia/Shanghai")).truncatedTo(ChronoUnit.SECONDS)); row.setExpiresAt(row.getPreparedAt().plusMinutes(15)); row.setPrepareRecordHash(recordHash(row));
        if (mapper.insert(row) != 1 || row.getId() == null) throw new IllegalStateException("APPROVAL_PREPARE_INSERT_FAILED");
        var stored = require(actorId, row.getPrepareId(), false);
        if (!row.equals(stored)) throw new IllegalStateException("APPROVAL_PREPARE_READBACK_MISMATCH");
        return stored;
    }
    public BpmApprovalReviewPrepareDO require(Long actorId, String prepareId, boolean requireFresh) {
        var row = mapper.selectExact(TenantContextHolder.getRequiredTenantId(), prepareId);
        if (row == null || !Objects.equals(actorId, row.getActorId()) || !Objects.equals(TenantContextHolder.getRequiredTenantId(), row.getTenantId())) throw new IllegalArgumentException("APPROVAL_PREPARE_NOT_FOUND");
        if (!Objects.equals(row.getPreviewContentHash(), ApprovalContentJson.hash(row.getCanonicalBusinessContentJson())) || !Objects.equals(row.getPrepareRecordHash(), recordHash(row))) throw new IllegalStateException("APPROVAL_PREPARE_INTEGRITY_FAILED");
        if (requireFresh && !row.getExpiresAt().isAfter(LocalDateTime.ofInstant(clock.instant(),ZoneId.of("Asia/Shanghai")))) throw new IllegalArgumentException("APPROVAL_PREPARE_EXPIRED");
        return row;
    }
    public static String recordHash(BpmApprovalReviewPrepareDO row) {
        var input = new LinkedHashMap<String, Object>();
        input.put("tenantId", String.valueOf(row.getTenantId())); input.put("actorId", String.valueOf(row.getActorId())); input.put("prepareId", row.getPrepareId()); input.put("ownerCode", row.getOwnerCode()); input.put("sourceTaskType", row.getSourceTaskType()); input.put("sourceTaskId", row.getSourceTaskId()); input.put("processInstanceId", row.getProcessInstanceId()); input.put("subjectType", row.getSubjectType()); input.put("subjectId", row.getSubjectId()); input.put("contentPolicyVersion", row.getContentPolicyVersion()); input.put("subjectVersion", row.getSubjectVersion()); input.put("previewContentHash", row.getPreviewContentHash()); input.put("scopeCode", row.getScopeCode()); input.put("canonicalBusinessContentJson", row.getCanonicalBusinessContentJson()); input.put("preparedAt", row.getPreparedAt().toString()); input.put("expiresAt", row.getExpiresAt().toString());
        return ApprovalContentJson.hash(ApprovalContentJson.canonical(ApprovalContentJson.parse(JsonUtils.toJsonString(input))));
    }
}
