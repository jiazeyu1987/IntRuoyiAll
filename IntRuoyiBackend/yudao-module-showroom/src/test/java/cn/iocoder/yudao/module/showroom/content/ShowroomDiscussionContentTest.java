package cn.iocoder.yudao.module.showroom.content;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomCommentAnchorType;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductComment;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductRevision;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomPersistentContentService;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomProductCommentService;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomChangeRequest;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomPersistentWorkflowService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import({
        ShowroomPersistentContentService.class,
        ShowroomPersistentWorkflowService.class,
        ShowroomProductCommentService.class
})
class ShowroomDiscussionContentTest extends BaseDbUnitTest {

    @Resource
    private ShowroomPersistentContentService contentService;
    @Resource
    private ShowroomPersistentWorkflowService workflowService;
    @Resource
    private ShowroomProductCommentService commentService;

    @Test
    void discussionShouldKeepAnchorIdentityAcrossRepliesAndResolution() {
        ShowroomProductRevision productRevision = publishProduct("YT-GW-001", "导管鞘组 V1");
        ShowroomProductComment thread = commentService.createThread(productRevision.productId(), productRevision.revisionId(),
                null, ShowroomCommentAnchorType.FIELD, "core_selling_points", 701L, "请补充卖点");
        ShowroomProductComment reply = commentService.reply(thread.commentId(), 702L, "已补充");
        ShowroomProductComment resolved = commentService.resolve(thread.commentId(), 703L);

        assertEquals(thread.productId(), reply.productId());
        assertEquals(thread.targetRevisionId(), reply.targetRevisionId());
        assertEquals(thread.anchorType(), reply.anchorType());
        assertEquals(thread.anchorKey(), reply.anchorKey());
        assertEquals("RESOLVED", resolved.status());
        assertEquals(2, commentService.pageByProduct(productRevision.productId(), ShowroomCommentAnchorType.FIELD,
                "core_selling_points", null, "RESOLVED").size());
    }

    @Test
    void changeRequestAnchorShouldRequireMatchingProductContext() {
        ShowroomProductRevision productRevision = publishProduct("YT-GW-001", "导管鞘组 V1");
        ShowroomProductRevision otherProduct = publishProduct("YT-GW-002", "另一个产品");
        ShowroomChangeRequest request = workflowService.submit(new cn.iocoder.yudao.module.showroom.workflow.model.ShowroomWorkflowStart(
                "PRODUCT", productRevision.productId(), productRevision.revisionId(), "product", "MANUAL",
                100L, 8L, 200L, 300L, null, List.of(
                new cn.iocoder.yudao.module.showroom.workflow.model.ShowroomChangeRequestItem("core_selling_points",
                        jsonValue("旧值"), jsonValue("新值")))));

        ShowroomProductComment thread = commentService.createThread(productRevision.productId(), productRevision.revisionId(),
                request.changeRequestId(), ShowroomCommentAnchorType.CHANGE_REQUEST, null, 701L, "审批讨论");
        assertEquals(request.changeRequestId(), thread.changeRequestId());

        IllegalStateException missingAnchor = assertThrows(IllegalStateException.class,
                () -> commentService.createThread(otherProduct.productId(), otherProduct.revisionId(),
                        request.changeRequestId(), ShowroomCommentAnchorType.CHANGE_REQUEST, null, 701L, "跨产品讨论"));

        assertTrue(missingAnchor.getMessage().contains("SHOWROOM_DISCUSSION_TARGET_INVALID"));
    }

    private ShowroomProductRevision publishProduct(String productCode, String nameCn) {
        ShowroomProductRevision draft = contentService.saveProductDraft(new ShowroomProductDraft(null, productCode,
                nameCn, "Introducer Sheath Set", Map.of(
                "target_market", "市场",
                "core_selling_points", "卖点"
        )));
        return contentService.publishProductRevision(draft.revisionId(), 901L);
    }

    private static String jsonValue(String value) {
        return "{\"value\":\"" + value + "\"}";
    }

}
