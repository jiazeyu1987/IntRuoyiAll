package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceOwnershipDO;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class DccControlledFileSourceGovernanceClassifier {

    public DccControlledFileSourceGovernanceDecision classify(
            Long tenantId,
            Long controlledFileId,
            Long sourceFileId,
            boolean sourceRecordExists,
            boolean sourceRecordDeleted,
            boolean locationComplete,
            boolean contentReadable,
            DccControlledFileSourceOwnershipDO ownership,
            String actualSourceSha256,
            List<GlobalReference> globalReferences,
            Set<Long> tenantScope) {
        if (sourceFileId == null) {
            return blocked("SOURCE_REFERENCE_MISSING", "source_file_id 为空");
        }
        if (!sourceRecordExists) {
            return blocked("SOURCE_RECORD_MISSING", "全局 infra_file 记录不存在");
        }
        if (sourceRecordDeleted) {
            return blocked("SOURCE_RECORD_DELETED", "全局 infra_file 记录已软删除");
        }
        if (!locationComplete) {
            return blocked("SOURCE_LOCATION_INCOMPLETE", "源文件存储定位不完整");
        }
        if (!contentReadable) {
            return blocked("SOURCE_CONTENT_UNREADABLE", "源文件正文不可读取");
        }
        if (globalReferences == null || tenantScope == null) {
            return blocked("SOURCE_GLOBAL_REFERENCE_CHECK_UNAVAILABLE", "全局 source_file_id 引用核验不可用");
        }
        if (globalReferences.stream().anyMatch(reference -> !tenantScope.contains(reference.tenantId()))) {
            return blocked("SOURCE_GLOBAL_REFERENCE_OUT_OF_SCOPE", "全局源文件存在冻结范围外有效引用");
        }
        if (globalReferences.stream().noneMatch(reference ->
                Objects.equals(reference.tenantId(), tenantId)
                        && Objects.equals(reference.controlledFileId(), controlledFileId))) {
            return blocked("SOURCE_REFERENCE_NOT_IN_GLOBAL_INDEX", "当前受控文件不在全局 source_file_id 引用结果中");
        }
        if (actualSourceSha256 == null || !actualSourceSha256.matches("[0-9a-f]{64}")) {
            return blocked("SOURCE_HASH_MISMATCH", "源文件正文 SHA-256 缺失或格式不正确");
        }
        if (ownership != null) {
            if (!Objects.equals(ownership.getTenantId(), tenantId)
                    || globalReferences.stream().noneMatch(reference ->
                    Objects.equals(reference.tenantId(), tenantId)
                            && Objects.equals(reference.controlledFileId(), ownership.getControlledFileId()))
                    || !Objects.equals(ownership.getSourceFileId(), sourceFileId)) {
                return blocked("OWNERSHIP_POINTER_MISMATCH", "ownership 与当前受控源文件指针不一致");
            }
            if (!Objects.equals(ownership.getSourceSha256(), actualSourceSha256)) {
                return blocked("SOURCE_HASH_MISMATCH", "ownership SHA-256 与正文重算值不一致");
            }
        }
        String action = globalReferences.size() > 1 ? "COPY_SHARED_SOURCE" : "CLAIM_SOURCE";
        return new DccControlledFileSourceGovernanceDecision("READY", action, null, null);
    }

    private DccControlledFileSourceGovernanceDecision blocked(String reasonCode, String detail) {
        return new DccControlledFileSourceGovernanceDecision("BLOCKED", null, reasonCode, detail);
    }

    public record GlobalReference(Long tenantId, Long sourceFileId, Long controlledFileId) {
    }
}
