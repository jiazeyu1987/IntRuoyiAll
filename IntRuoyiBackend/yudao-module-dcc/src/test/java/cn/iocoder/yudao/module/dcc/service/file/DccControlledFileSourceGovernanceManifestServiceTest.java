package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceGovernanceBatchDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceGovernanceItemDO;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SOURCE_GOVERNANCE_ITEM_BLOCKED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SOURCE_GOVERNANCE_MANIFEST_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SOURCE_GOVERNANCE_SCOPE_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DccControlledFileSourceGovernanceManifestServiceTest {

    private final DccControlledFileSourceGovernanceManifestService service =
            new DccControlledFileSourceGovernanceManifestService(
                    new DccControlledFileSourceGovernanceManifestHasher());

    @Test
    void requireConfirmed_rejectsUnconfirmedOrDigestMismatch() {
        DccControlledFileSourceGovernanceBatchDO batch = DccControlledFileSourceGovernanceBatchDO.builder()
                .batchStatus("DRAFT").manifestSha256("manifest").requestSha256("request").build();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.requireConfirmed(batch, "manifest", "request"));

        assertEquals(CONTROLLED_FILE_SOURCE_GOVERNANCE_MANIFEST_INVALID.getCode(), ex.getCode());
    }

    @Test
    void requireConfirmed_acceptsExactConfirmedManifest() {
        DccControlledFileSourceGovernanceBatchDO batch = DccControlledFileSourceGovernanceBatchDO.builder()
                .batchStatus("CONFIRMED").manifestSha256("manifest").requestSha256("request").build();

        service.requireConfirmed(batch, "manifest", "request");
    }

    @Test
    void requireConfirmed_acceptsTerminalBatchForIdempotentRetry() {
        DccControlledFileSourceGovernanceBatchDO batch = DccControlledFileSourceGovernanceBatchDO.builder()
                .batchStatus("COMPLETED").manifestSha256("manifest").requestSha256("request").build();
        service.requireConfirmed(batch, "manifest", "request");
    }

    @Test
    void requireVersioned_rejectsUnknownRuleOrSchemaVersion() {
        DccControlledFileSourceGovernanceBatchDO batch = DccControlledFileSourceGovernanceBatchDO.builder()
                .ruleVersion(DccControlledFileSourceGovernanceManifestService.CURRENT_RULE_VERSION)
                .schemaVersion(DccControlledFileSourceGovernanceManifestService.CURRENT_SCHEMA_VERSION).build();
        service.requireVersioned(batch);
        batch.setRuleVersion("old-rule");
        assertThrows(ServiceException.class, () -> service.requireVersioned(batch));
    }

    @Test
    void requireProcessable_rejectsBlockedItemAndAcceptsCompletedIdempotency() {
        DccControlledFileSourceGovernanceItemDO blocked = DccControlledFileSourceGovernanceItemDO.builder()
                .itemStatus("BLOCKED").blockerReasonCode("SOURCE_RECORD_DELETED").build();
        ServiceException blockedException = assertThrows(ServiceException.class,
                () -> service.requireProcessable(blocked));
        assertEquals(CONTROLLED_FILE_SOURCE_GOVERNANCE_ITEM_BLOCKED.getCode(), blockedException.getCode());

        DccControlledFileSourceGovernanceItemDO completed = DccControlledFileSourceGovernanceItemDO.builder()
                .itemStatus("COMPLETED").build();
        assertEquals(true, service.isCompleted(completed));
    }

    @Test
    void requireTenantInScope_rejectsTenantNotInFrozenJson() {
        DccControlledFileSourceGovernanceBatchDO batch = DccControlledFileSourceGovernanceBatchDO.builder()
                .tenantScopeJson("[31,32]").tenantScopeSha256(sha256("[31,32]")).build();
        service.requireTenantInScope(batch, 31L);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.requireTenantInScope(batch, 3L));
        assertEquals(CONTROLLED_FILE_SOURCE_GOVERNANCE_SCOPE_INVALID.getCode(), ex.getCode());
    }

    @Test
    void requireItemInScope_rejectsItemTenantOutsideFrozenScope() {
        DccControlledFileSourceGovernanceBatchDO batch = DccControlledFileSourceGovernanceBatchDO.builder()
                .id(55L).tenantScopeJson("[31]").tenantScopeSha256(sha256("[31]")).build();
        DccControlledFileSourceGovernanceItemDO item = DccControlledFileSourceGovernanceItemDO.builder()
                .batchId(55L).tenantId(32L).controlledFileId(901L).build();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.requireItemInScope(batch, item, java.util.Set.of(32L)));

        assertEquals(CONTROLLED_FILE_SOURCE_GOVERNANCE_SCOPE_INVALID.getCode(), ex.getCode());
    }

    @Test
    void requireItemInScope_rejectsCallerScopeWiderThanFrozenScope() {
        DccControlledFileSourceGovernanceBatchDO batch = DccControlledFileSourceGovernanceBatchDO.builder()
                .id(55L).tenantScopeJson("[31]").tenantScopeSha256(sha256("[31]")).build();
        DccControlledFileSourceGovernanceItemDO item = DccControlledFileSourceGovernanceItemDO.builder()
                .batchId(55L).tenantId(31L).controlledFileId(901L).build();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.requireItemInScope(batch, item, java.util.Set.of(31L, 32L)));

        assertEquals(CONTROLLED_FILE_SOURCE_GOVERNANCE_SCOPE_INVALID.getCode(), ex.getCode());
    }

    @Test
    void requireTenantInScope_rejectsTamperedFrozenScopeHash() {
        DccControlledFileSourceGovernanceBatchDO batch = DccControlledFileSourceGovernanceBatchDO.builder()
                .tenantScopeJson("[31]").tenantScopeSha256("tampered").build();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.requireTenantInScope(batch, 31L));

        assertEquals(CONTROLLED_FILE_SOURCE_GOVERNANCE_SCOPE_INVALID.getCode(), ex.getCode());
    }

    @Test
    void requireManifestContentRejectsImmutableSnapshotTamperingButAllowsExecutionStatusChange() {
        DccControlledFileSourceGovernanceManifestHasher hasher =
                new DccControlledFileSourceGovernanceManifestHasher();
        DccControlledFileSourceGovernanceBatchDO batch = DccControlledFileSourceGovernanceBatchDO.builder()
                .tenantScopeJson("[31]").tenantScopeSha256("scope").snapshotMaxControlledFileId(901L)
                .effectiveControlledFileCount(1L)
                .ruleVersion(DccControlledFileSourceGovernanceManifestService.CURRENT_RULE_VERSION)
                .schemaVersion(DccControlledFileSourceGovernanceManifestService.CURRENT_SCHEMA_VERSION).build();
        DccControlledFileSourceGovernanceItemDO item = DccControlledFileSourceGovernanceItemDO.builder()
                .tenantId(31L).controlledFileId(901L).legacySourceFileId(700L).snapshotSourceFileId(700L)
                .snapshotSourceSha256("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                .governanceAction("CLAIM_SOURCE").itemStatus("READY").build();
        batch.setManifestSha256(hasher.sha256(batch, List.of(item)));
        service.requireManifestContent(batch, List.of(item));

        item.setItemStatus("COMPLETED");
        service.requireManifestContent(batch, List.of(item));

        item.setSnapshotSourceFileId(701L);
        assertThrows(ServiceException.class, () -> service.requireManifestContent(batch, List.of(item)));
    }

    private static String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
