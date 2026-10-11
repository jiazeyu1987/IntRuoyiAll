package cn.iocoder.yudao.module.bpm.approval.content;

import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalReviewPreviewService {
    private final ApprovalContentResolverRegistry registry;
    private final ApprovalReviewPrepareService prepares;
    public ApprovalReviewPreviewService(ApprovalContentResolverRegistry registry, ApprovalReviewPrepareService prepares) { this.registry = registry; this.prepares = prepares; }
    /** Resolver finishes its short RR read before the independent immutable INSERT starts. */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ApprovalReviewPreview preview(Long actorId, ApprovalModuleCode module, String type, String taskId, String processId) {
        var content = registry.require(module).prepare(actorId, type, taskId, processId);
        var prepare = prepares.insert(actorId, content);
        return new ApprovalReviewPreview(prepare.getPrepareId(), content, prepare.getExpiresAt().toString() + "+08:00");
    }
    public record ApprovalReviewPreview(String prepareId, PreparedApprovalContent content, String expiresAt) { }
}
