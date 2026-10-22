package cn.iocoder.yudao.module.bpm.approval.content;
import cn.iocoder.yudao.module.bpm.dal.dataobject.approval.BpmApprovalReviewPrepareDO;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ApprovalReviewPrepareIntegrityTest {
    private BpmApprovalReviewPrepareDO fact() {
        var r=new BpmApprovalReviewPrepareDO();r.setTenantId(1L);r.setActorId(2L);r.setPrepareId("ce75d9a0-9194-4bcb-a26e-ef302b9bc956");r.setOwnerCode("EDHR");r.setSourceTaskType("EDHR_WORK_TASK");r.setSourceTaskId("3");r.setSubjectType("EDHR_RELEASE");r.setSubjectId("4");r.setContentPolicyVersion("approval-content-v1");r.setSubjectVersion("EDHR1:"+"a".repeat(64));r.setPreviewContentHash(ApprovalContentJson.hash("{}"));r.setCanonicalBusinessContentJson("{}");r.setScopeCode("EDHR_RELEASE");r.setPreparedAt(LocalDateTime.of(2026,10,10,10,0));r.setExpiresAt(r.getPreparedAt().plusMinutes(15));return r;
    }
    @Test void actorContentExpiryAndIdentityAreProtectedButGeneratedDatabaseIdIsNot() {
        var r=fact();String digest=ApprovalReviewPrepareService.recordHash(r);r.setId(17L);assertEquals(digest,ApprovalReviewPrepareService.recordHash(r));
        r.setActorId(5L);assertNotEquals(digest,ApprovalReviewPrepareService.recordHash(r));r=fact();r.setExpiresAt(r.getExpiresAt().plusSeconds(1));assertNotEquals(digest,ApprovalReviewPrepareService.recordHash(r));r=fact();r.setSourceTaskId("9");assertNotEquals(digest,ApprovalReviewPrepareService.recordHash(r));r=fact();r.setCanonicalBusinessContentJson("{\"changed\":true}");assertNotEquals(digest,ApprovalReviewPrepareService.recordHash(r));
    }
}
