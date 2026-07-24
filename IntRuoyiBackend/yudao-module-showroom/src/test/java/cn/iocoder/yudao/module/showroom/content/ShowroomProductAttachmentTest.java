package cn.iocoder.yudao.module.showroom.content;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductAttachment;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductDraft;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomPersistentContentService;
import cn.iocoder.yudao.module.showroom.controller.admin.ShowroomAdminController;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomProductRevisionAttachmentMapper;
import cn.iocoder.yudao.module.showroom.release.ShowroomReleaseAutoPublishService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(ShowroomPersistentContentService.class)
class ShowroomProductAttachmentTest extends BaseDbUnitTest {

    @Resource
    private ShowroomPersistentContentService contentService;
    @Resource
    private ShowroomProductRevisionAttachmentMapper attachmentMapper;
    @MockBean
    private ShowroomReleaseAutoPublishService releaseAutoPublishService;

    @Test
    void productDraftShouldPersistAttachmentSnapshotByRevision() {
        var first = contentService.saveProductDraft(new ShowroomProductDraft(
                null,
                "ATTACH-001",
                "附件产品",
                "Attachment Product",
                Map.of("target_market", "冠脉介入"),
                List.of(
                        attachment("image", 101L, "image.png", "image/png", 1200L, 2),
                        attachment("text", 102L, "manual.pdf", "application/pdf", 900L, 1)
                )));

        assertEquals(List.of("manual.pdf", "image.png"),
                contentService.getProductRevision(first.revisionId()).attachments().stream()
                        .map(ShowroomProductAttachment::originalName)
                        .toList());
        assertEquals(2, attachmentMapper.selectByRevisionId(first.revisionId()).size());

        var second = contentService.saveProductDraft(new ShowroomProductDraft(
                first.productId(),
                "ATTACH-001",
                "附件产品二版",
                "Attachment Product V2",
                Map.of("target_market", "冠脉介入"),
                List.of(attachment("video", 103L, "demo.mp4", "video/mp4", 2048L, 1))));

        assertEquals(List.of("manual.pdf", "image.png"),
                contentService.getProductRevision(first.revisionId()).attachments().stream()
                        .map(ShowroomProductAttachment::originalName)
                        .toList());
        assertEquals(List.of("demo.mp4"),
                contentService.getProductRevision(second.revisionId()).attachments().stream()
                        .map(ShowroomProductAttachment::originalName)
                        .toList());
    }

    @Test
    void productDraftShouldRejectMoreThanTwentyAttachments() {
        List<ShowroomProductAttachment> attachments = IntStream.rangeClosed(1, 21)
                .mapToObj(index -> attachment("image", 1000L + index, "image-" + index + ".png",
                        "image/png", 100L, index))
                .toList();

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> contentService.saveProductDraft(new ShowroomProductDraft(
                        null,
                        "ATTACH-LIMIT",
                        "附件数量产品",
                        "Attachment Limit Product",
                        Map.of(),
                        attachments)));

        assertTrue(exception.getMessage().contains("SHOWROOM_PRODUCT_ATTACHMENT_LIMIT_EXCEEDED"));
    }

    @Test
    void productAttachmentResponseShouldExposeFileUrl() {
        assertTrue(Arrays.stream(ShowroomAdminController.ProductAttachmentRespVO.class.getRecordComponents())
                .anyMatch(component -> component.getName().equals("url")));
    }

    private static ShowroomProductAttachment attachment(String assetType, Long fileId, String originalName,
                                                       String mimeType, Long fileSize, int displayOrder) {
        return new ShowroomProductAttachment(null, null, null, assetType, fileId, originalName, mimeType,
                fileSize, displayOrder);
    }
}
