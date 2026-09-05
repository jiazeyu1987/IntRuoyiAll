package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceGovernanceBatchDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceGovernanceItemDO;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;

@Service
public class DccControlledFileSourceGovernanceManifestHasher {

    public String sha256(DccControlledFileSourceGovernanceBatchDO batch,
                         List<DccControlledFileSourceGovernanceItemDO> items) {
        StringBuilder canonical = new StringBuilder();
        append(canonical, batch.getSchemaVersion(), batch.getRuleVersion(), batch.getTenantScopeJson(),
                batch.getTenantScopeSha256(), batch.getSnapshotMaxControlledFileId(),
                batch.getEffectiveControlledFileCount());
        items.stream().sorted(Comparator
                        .comparing(DccControlledFileSourceGovernanceItemDO::getTenantId,
                                Comparator.nullsFirst(Long::compareTo))
                        .thenComparing(DccControlledFileSourceGovernanceItemDO::getControlledFileId,
                                Comparator.nullsFirst(Long::compareTo)))
                .forEach(item -> {
                    boolean blockedAtPreparation = "NO_ACTION".equals(item.getGovernanceAction());
                    append(canonical, item.getTenantId(), item.getControlledFileId(),
                            item.getLegacySourceFileId(), item.getSnapshotSourceFileId(),
                            item.getSnapshotSourceSha256(), item.getSnapshotLocationHash(),
                            item.getSnapshotSourceConfigId(), item.getSnapshotSourcePath(),
                            item.getSnapshotSourceDeleted(), item.getSnapshotHistoryEvidenceHash(),
                            item.getSharedGroupKey(), item.getGovernanceAction(),
                            blockedAtPreparation ? item.getItemStatus() : "PROCESSABLE",
                            blockedAtPreparation ? item.getBlockerReasonCode() : null,
                            blockedAtPreparation ? item.getBlockerDetail() : null);
                });
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }

    private void append(StringBuilder canonical, Object... values) {
        for (Object value : values) {
            String text = value == null ? "<null>" : String.valueOf(value);
            canonical.append(text.length()).append(':').append(text).append('|');
        }
        canonical.append('\n');
    }
}
