package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceGovernanceBatchDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceGovernanceItemDO;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Objects;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SOURCE_GOVERNANCE_ITEM_BLOCKED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SOURCE_GOVERNANCE_MANIFEST_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SOURCE_GOVERNANCE_SCOPE_INVALID;

@Service
public class DccControlledFileSourceGovernanceManifestService {

    public static final String CURRENT_RULE_VERSION = "dcc-source-ownership-v1";
    public static final String CURRENT_SCHEMA_VERSION = "dcc-source-governance-v1";

    private final DccControlledFileSourceGovernanceManifestHasher manifestHasher;

    public DccControlledFileSourceGovernanceManifestService(
            DccControlledFileSourceGovernanceManifestHasher manifestHasher) {
        this.manifestHasher = manifestHasher;
    }

    public void requireConfirmed(DccControlledFileSourceGovernanceBatchDO batch,
                                 String manifestSha256, String requestSha256) {
        if (batch == null
                || !(Objects.equals(batch.getBatchStatus(), "CONFIRMED")
                || Objects.equals(batch.getBatchStatus(), "COMPLETED")
                || Objects.equals(batch.getBatchStatus(), "BLOCKED")
                || Objects.equals(batch.getBatchStatus(), "FAILED"))
                || !Objects.equals(batch.getManifestSha256(), manifestSha256)
                || !Objects.equals(batch.getRequestSha256(), requestSha256)) {
            throw exception(CONTROLLED_FILE_SOURCE_GOVERNANCE_MANIFEST_INVALID);
        }
    }

    public void requireVersioned(DccControlledFileSourceGovernanceBatchDO batch) {
        if (batch == null || !Objects.equals(CURRENT_RULE_VERSION, batch.getRuleVersion())
                || !Objects.equals(CURRENT_SCHEMA_VERSION, batch.getSchemaVersion())) {
            throw exception(CONTROLLED_FILE_SOURCE_GOVERNANCE_MANIFEST_INVALID);
        }
    }

    public void requireManifestContent(DccControlledFileSourceGovernanceBatchDO batch,
                                       List<DccControlledFileSourceGovernanceItemDO> items) {
        if (batch == null || items == null
                || !Objects.equals(batch.getManifestSha256(), manifestHasher.sha256(batch, items))) {
            throw exception(CONTROLLED_FILE_SOURCE_GOVERNANCE_MANIFEST_INVALID);
        }
    }

    public void requireProcessable(DccControlledFileSourceGovernanceItemDO item) {
        if (item == null || !Objects.equals(item.getItemStatus(), "READY")) {
            throw exception(CONTROLLED_FILE_SOURCE_GOVERNANCE_ITEM_BLOCKED,
                    item == null ? null : item.getControlledFileId());
        }
    }

    public boolean isCompleted(DccControlledFileSourceGovernanceItemDO item) {
        return item != null && Objects.equals(item.getItemStatus(), "COMPLETED");
    }

    public void requireItemInScope(DccControlledFileSourceGovernanceBatchDO batch,
                                   DccControlledFileSourceGovernanceItemDO item,
                                   Set<Long> tenantScope) {
        Set<Long> frozenTenantScope = frozenTenantScope(batch);
        if (batch == null || item == null || !Objects.equals(batch.getId(), item.getBatchId())
                || tenantScope == null || !tenantScope.equals(frozenTenantScope)
                || !frozenTenantScope.contains(item.getTenantId())) {
            throw exception(CONTROLLED_FILE_SOURCE_GOVERNANCE_SCOPE_INVALID);
        }
    }

    public void requireTenantInScope(DccControlledFileSourceGovernanceBatchDO batch, Long tenantId) {
        if (tenantId == null || !frozenTenantScope(batch).contains(tenantId)) {
            throw exception(CONTROLLED_FILE_SOURCE_GOVERNANCE_SCOPE_INVALID);
        }
    }

    private Set<Long> frozenTenantScope(DccControlledFileSourceGovernanceBatchDO batch) {
        if (batch == null || batch.getTenantScopeJson() == null || batch.getTenantScopeSha256() == null
                || !Objects.equals(batch.getTenantScopeSha256(), sha256(batch.getTenantScopeJson()))) {
            throw exception(CONTROLLED_FILE_SOURCE_GOVERNANCE_SCOPE_INVALID);
        }
        try {
            List<Long> tenantIds = JsonUtils.parseArray(batch.getTenantScopeJson(), Long.class);
            if (tenantIds == null || tenantIds.isEmpty() || tenantIds.stream().anyMatch(Objects::isNull)
                    || new HashSet<>(tenantIds).size() != tenantIds.size()) {
                throw exception(CONTROLLED_FILE_SOURCE_GOVERNANCE_SCOPE_INVALID);
            }
            return Set.copyOf(tenantIds);
        } catch (RuntimeException ex) {
            if (ex instanceof cn.iocoder.yudao.framework.common.exception.ServiceException) {
                throw ex;
            }
            throw exception(CONTROLLED_FILE_SOURCE_GOVERNANCE_SCOPE_INVALID);
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }
}
