package cn.iocoder.yudao.module.system.service.gxpaudit;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventRelationDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventRelationMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Real isolated H2 queries verify list projection and unchanged full-detail bytes, not MySQL timing. */
@Import(GxpAuditQueryServiceImpl.class)
class GxpAuditScopedPageProjectionTest extends BaseDbUnitTest {

    @Resource private GxpAuditQueryServiceImpl queryService;
    @Resource private GxpAuditEventMapper eventMapper;
    @Resource private GxpAuditEventRelationMapper relationMapper;

    @Test
    void scopedPageOmitsLargeStateBytesButKeepsIdentityActorsManifestsAndHashes() {
        GxpAuditEventDO original = event(1L, "RELEASE_TRANSACTION:233", true);
        relate(original, "ACTIVE_ORDER", "421");

        var page = queryService.page(scope(1, 20));

        assertEquals(1L, page.getTotal());
        GxpAuditEventDO summary = page.getList().get(0);
        assertNull(summary.getBeforeStateJson());
        assertNull(summary.getAfterStateJson());
        assertNull(summary.getCanonicalEventJson());
        assertEquals(original.getId(), summary.getId());
        assertEquals(original.getLedgerSequence(), summary.getLedgerSequence());
        assertEquals(original.getOperationId(), summary.getOperationId());
        assertEquals(original.getServerOccurredAt(), summary.getServerOccurredAt());
        assertEquals(original.getActorId(), summary.getActorId());
        assertEquals(original.getAuthenticatedActorJson(), summary.getAuthenticatedActorJson());
        assertEquals(original.getPerformedByJson(), summary.getPerformedByJson());
        assertEquals(original.getSignatureRecordId(), summary.getSignatureRecordId());
        assertEquals(original.getSignatureContentHash(), summary.getSignatureContentHash());
        assertEquals(original.getEventHash(), summary.getEventHash());
        assertEquals(original.getPreviousEventHash(), summary.getPreviousEventHash());
        assertEquals(original.getStatePayloadHash(), summary.getStatePayloadHash());
        assertEquals(original.getIdempotencyPayloadHash(), summary.getIdempotencyPayloadHash());
        assertEquals(original.getRelationManifestJson(), summary.getRelationManifestJson());
        assertEquals(original.getEvidenceManifestJson(), summary.getEvidenceManifestJson());

        GxpAuditEventDO full = queryService.get(original.getId());
        assertEquals(original, full);
        assertEquals(original.getEventHash(), DigestUtil.sha256Hex(full.getCanonicalEventJson()));
        assertEquals(1, queryService.listRelations(original.getId()).size());
    }

    @Test
    void scopedProjectionPreservesTenantScopeFiltersTotalAndDescendingPagination() {
        GxpAuditEventDO related = event(1L, "RELEASE_TRANSACTION:233", true);
        relate(related, "ACTIVE_ORDER", "421");
        GxpAuditEventDO direct = event(2L, "MES_ACTIVE_ORDER:421", true);
        event(3L, "ACTIVE_ORDER:999", true);
        event(4L, "ACTIVE_ORDER:421", false);
        TenantContextHolder.setTenantId(2L);
        GxpAuditEventDO otherTenant = event(1L, "ACTIVE_ORDER:421", true);
        relate(otherTenant, "ACTIVE_ORDER", "421");
        TenantContextHolder.setTenantId(1L);

        GxpAuditEventPageQuery query = scope(1, 1).setSignaturePresent(true)
                .setActorId(347L).setOperationId("mes.market-release.approve")
                .setOccurredAt(new LocalDateTime[]{LocalDateTime.of(2026, 10, 6, 0, 0),
                        LocalDateTime.of(2026, 10, 7, 0, 0)});
        var first = queryService.page(query);
        var second = queryService.page(query.setPageNo(2));

        assertEquals(2L, first.getTotal());
        assertEquals(2L, second.getTotal());
        assertEquals(List.of(direct.getId()), first.getList().stream().map(GxpAuditEventDO::getId).toList());
        assertEquals(List.of(related.getId()), second.getList().stream().map(GxpAuditEventDO::getId).toList());
        assertNull(queryService.get(otherTenant.getId()));
    }

    @Test
    void unscopedPageAndFullDetailRetainOriginalPayloadContract() {
        GxpAuditEventDO original = event(1L, "ACTIVE_ORDER:421", true);

        var page = queryService.page(new GxpAuditEventPageQuery().setPageNo(1).setPageSize(20));

        assertEquals(List.of(original), page.getList());
        assertEquals(original, queryService.get(original.getId()));
    }

    private static GxpAuditEventPageQuery scope(int pageNo, int pageSize) {
        return new GxpAuditEventPageQuery().setScopeType("ACTIVE_ORDER").setScopeId(421L)
                .setPageNo(pageNo).setPageSize(pageSize);
    }

    private GxpAuditEventDO event(long sequence, String subject, boolean signed) {
        GxpAuditEventDO event = new GxpAuditEventDO();
        event.setTenantId(TenantContextHolder.getRequiredTenantId());
        event.setLedgerSequence(sequence);
        event.setOperationId("mes.market-release.approve");
        event.setDomain("MES");
        event.setSubjectType("ACTIVE_ORDER");
        event.setSubjectId(subject);
        event.setSubjectVersion("3");
        event.setAction("APPROVE");
        event.setReason("isolated query fixture");
        event.setActorId(347L);
        event.setActorUsername("query-fixture");
        event.setActorDisplayName("query fixture actor");
        event.setServerOccurredAt(LocalDateTime.of(2026, 10, 6, 12, 0));
        event.setBeforeState("PRESENT");
        event.setAfterState("PRESENT");
        event.setBeforeStateJson("{\"before\":\"" + "b".repeat(262144) + "\"}");
        event.setAfterStateJson("{\"after\":\"" + "a".repeat(262144) + "\"}");
        event.setCanonicalEventJson("{\"before\":" + event.getBeforeStateJson()
                + ",\"after\":" + event.getAfterStateJson() + "}");
        event.setPolicyVersion("fixture-v2");
        event.setIdempotencyKey("fixture-" + event.getTenantId() + "-" + sequence);
        event.setSignatureRecordId(signed ? "signature-fixture" : null);
        event.setSignatureContentHash(signed ? "s".repeat(64) : null);
        event.setIdempotencyPayloadHash("i".repeat(64));
        event.setEventHash(DigestUtil.sha256Hex(event.getCanonicalEventJson()));
        event.setPreviousEventHash("p".repeat(64));
        event.setStatePayloadHash("h".repeat(64));
        event.setAlgorithm("SHA-256");
        event.setAuthenticatedActorJson("{\"kind\":\"USER\",\"id\":\"347\"}");
        event.setPerformedByJson("{\"kind\":\"USER\",\"id\":\"347\"}");
        event.setRelationManifestJson("[{\"targetType\":\"ACTIVE_ORDER\",\"targetId\":\"421\"}]");
        event.setEvidenceManifestJson("[{\"sha256\":\"" + "e".repeat(64) + "\"}]");
        eventMapper.insert(event);
        return event;
    }

    private void relate(GxpAuditEventDO event, String targetType, String targetId) {
        GxpAuditEventRelationDO relation = new GxpAuditEventRelationDO();
        relation.setTenantId(event.getTenantId());
        relation.setEventId(event.getId());
        relation.setRelationType("PRIMARY");
        relation.setTargetType(targetType);
        relation.setTargetId(targetId);
        relation.setCreatedAtUtc(event.getServerOccurredAt());
        relationMapper.insert(relation);
    }
}
