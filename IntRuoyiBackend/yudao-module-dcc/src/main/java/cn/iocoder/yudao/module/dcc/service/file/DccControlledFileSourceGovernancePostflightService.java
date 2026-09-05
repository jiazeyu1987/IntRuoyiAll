package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceGovernanceBatchDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceGovernanceItemDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceOwnershipDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSourceGovernanceBatchMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSourceGovernanceItemMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSourceOwnershipMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SOURCE_GOVERNANCE_MANIFEST_INVALID;

@Service
public class DccControlledFileSourceGovernancePostflightService {

    @Resource
    private DccControlledFileSourceGovernanceBatchMapper batchMapper;
    @Resource
    private DccControlledFileSourceGovernanceItemMapper itemMapper;
    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccControlledFileSourceOwnershipMapper ownershipMapper;
    @Resource
    private DccControlledFileSourceOwnershipService ownershipService;

    public DccControlledFileSourceGovernancePostflightReport inspectCompletedItems(String taskKey) {
        DccControlledFileSourceGovernanceBatchDO batch = batchMapper.selectByTaskKey(taskKey);
        if (batch == null) {
            throw exception(CONTROLLED_FILE_SOURCE_GOVERNANCE_MANIFEST_INVALID);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (batch.getTenantScopeJson() == null || !batch.getTenantScopeJson().matches(
                ".*(?:^|\\[|,|\\s)" + tenantId + "(?:$|,|\\s|\\]).*")) {
            throw exception(CONTROLLED_FILE_SOURCE_GOVERNANCE_MANIFEST_INVALID);
        }
        List<DccControlledFileSourceGovernanceItemDO> items =
                itemMapper.selectByBatchAndTenant(batch.getId(), tenantId).stream()
                        .filter(item -> Objects.equals(item.getItemStatus(), "COMPLETED"))
                        .toList();
        List<DccControlledFileSourceGovernancePostflightFinding> findings = new ArrayList<>();
        int valid = 0;
        for (DccControlledFileSourceGovernanceItemDO item : items) {
            DccControlledFileDO controlledFile = controlledFileMapper.selectByIdAndTenantIncludingDeleted(
                    tenantId, item.getControlledFileId());
            if (controlledFile == null || Boolean.TRUE.equals(controlledFile.getDeleted())) {
                findings.add(finding(item, "CONTROLLED_FILE_MISSING", "受控文件记录不存在或已删除"));
                continue;
            }
            if (item.getSnapshotHistoryEvidenceHash() != null
                    && !Objects.equals(item.getSnapshotHistoryEvidenceHash(), historyEvidenceHash(controlledFile))) {
                findings.add(finding(item, "HISTORICAL_EVIDENCE_DRIFTED", "原始/发布/盖章/生命周期证据与快照不一致"));
                continue;
            }
            Long expectedSourceId = item.getIsolatedSourceFileId() == null
                    ? item.getSnapshotSourceFileId() : item.getIsolatedSourceFileId();
            if (!Objects.equals(expectedSourceId, controlledFile.getSourceFileId())) {
                findings.add(finding(item, "SOURCE_POINTER_MISMATCH", "受控文件 source_file_id 与完成记录不一致"));
                continue;
            }
            List<DccControlledFileMapper.GlobalSourceReference> references =
                    controlledFileMapper.selectGlobalEffectiveSourceReferences(
                            expectedSourceId, batch.getSnapshotMaxControlledFileId());
            if (references != null && references.size() > 1) {
                findings.add(finding(item, "COMPLETED_SOURCE_STILL_SHARED",
                        "完成后的 source_file_id 仍被多个有效受控记录共享"));
                continue;
            }
            DccControlledFileSourceOwnershipDO ownership = ownershipMapper.selectByControlledFileId(
                    tenantId, item.getControlledFileId());
            if (ownership == null || !Objects.equals(ownership.getSourceFileId(), expectedSourceId)
                    || !Objects.equals(ownership.getSourceSha256(), item.getSourceSha256())) {
                findings.add(finding(item, "OWNERSHIP_EVIDENCE_MISMATCH", "独占关系或 SHA-256 证据不一致"));
                continue;
            }
            try {
                DccControlledFilePreparedSource actual = ownershipService.inspectSource(expectedSourceId);
                if (!Objects.equals(actual.sourceSha256(), item.getSourceSha256())) {
                    findings.add(finding(item, "SOURCE_HASH_MISMATCH", "当前源文件 SHA-256 与完成记录不一致"));
                    continue;
                }
            } catch (RuntimeException ex) {
                findings.add(finding(item, "SOURCE_CONTENT_UNREADABLE", ex.getMessage()));
                continue;
            }
            valid++;
        }
        return new DccControlledFileSourceGovernancePostflightReport(taskKey, items.size(), valid, findings);
    }

    public static String historyEvidenceHash(DccControlledFileDO file) {
        String value = String.join("|", String.valueOf(file.getOriginalFileId()),
                String.valueOf(file.getDrawingPdfFileId()), String.valueOf(file.getTrainingRecordFileId()),
                String.valueOf(file.getPublishedFileId()), String.valueOf(file.getStampedFileId()),
                String.valueOf(file.getVersionNo()), String.valueOf(file.getStatus()),
                String.valueOf(file.getProcessInstanceId()), String.valueOf(file.getSubmittedTime()),
                String.valueOf(file.getApprovedTime()), String.valueOf(file.getPublishedTime()),
                String.valueOf(file.getStampedTime()), String.valueOf(file.getCheckedOutBy()),
                String.valueOf(file.getCheckedOutTime()));
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }

    private DccControlledFileSourceGovernancePostflightFinding finding(
            DccControlledFileSourceGovernanceItemDO item, String reasonCode, String detail) {
        return new DccControlledFileSourceGovernancePostflightFinding(
                item.getId(), item.getControlledFileId(), "INVALID", reasonCode, detail);
    }
}
