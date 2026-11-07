package cn.iocoder.yudao.module.bpm.approval.content;
import cn.iocoder.yudao.module.bpm.dal.dataobject.approval.BpmApprovalReviewPrepareDO;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ApprovalReviewPrepareIntegrityTest {
    private BpmApprovalReviewPrepareDO fact() {
        var r=new BpmApprovalReviewPrepareDO();r.setTenantId(1L);r.setActorId(2L);r.setPrepareId("ce75d9a0-9194-4bcb-a26e-ef302b9bc956");r.setOwnerCode("EDHR");r.setSourceTaskType("EDHR_WORK_TASK");r.setSourceTaskId("3");r.setSubjectType("EDHR_RELEASE");r.setSubjectId("4");r.setContentPolicyVersion("approval-content-v1");r.setSubjectVersion("EDHR1:"+"a".repeat(64));r.setPreviewContentHash(ApprovalContentJson.hash("{}"));r.setCanonicalBusinessContentJson("{}");r.setScopeCode("EDHR_RELEASE");r.setPreparedAt(LocalDateTime.of(2026,10,10,10,0));r.setExpiresAt(r.getPreparedAt().plusMinutes(15));return r;
    }
    @Test void expiryBoundaryRejectsSigningButRetainsHistoricalRead() {
        var row = fact();
        row.setPrepareRecordHash(ApprovalReviewPrepareService.recordHash(row));
        var mapper = org.mockito.Mockito.mock(cn.iocoder.yudao.module.bpm.dal.mysql.approval.BpmApprovalReviewPrepareMapper.class);
        org.mockito.Mockito.when(mapper.selectExact(1L, row.getPrepareId())).thenReturn(row);
        try {
            cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(1L);
            var before = new ApprovalReviewPrepareService(mapper, java.time.Clock.fixed(java.time.Instant.parse("2026-10-10T02:14:59Z"), java.time.ZoneOffset.UTC));
            assertSame(row, before.require(2L, row.getPrepareId(), true));
            var boundary = new ApprovalReviewPrepareService(mapper, java.time.Clock.fixed(java.time.Instant.parse("2026-10-10T02:15:00Z"), java.time.ZoneOffset.UTC));
            var error = assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class, () -> boundary.require(2L, row.getPrepareId(), true));
            assertEquals(400, error.getCode());
            assertEquals("审批预览已过期，请重新预览并签名。", error.getMessage());
            assertSame(row, boundary.require(2L, row.getPrepareId(), false));
        } finally { cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear(); }
    }

    @Test void actorContentExpiryAndIdentityAreProtectedButGeneratedDatabaseIdIsNot() {
        var r=fact();String digest=ApprovalReviewPrepareService.recordHash(r);r.setId(17L);assertEquals(digest,ApprovalReviewPrepareService.recordHash(r));
        r.setActorId(5L);assertNotEquals(digest,ApprovalReviewPrepareService.recordHash(r));r=fact();r.setExpiresAt(r.getExpiresAt().plusSeconds(1));assertNotEquals(digest,ApprovalReviewPrepareService.recordHash(r));r=fact();r.setSourceTaskId("9");assertNotEquals(digest,ApprovalReviewPrepareService.recordHash(r));r=fact();r.setCanonicalBusinessContentJson("{\"changed\":true}");assertNotEquals(digest,ApprovalReviewPrepareService.recordHash(r));
    }
}
