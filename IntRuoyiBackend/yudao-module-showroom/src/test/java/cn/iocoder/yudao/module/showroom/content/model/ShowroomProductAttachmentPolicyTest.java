package cn.iocoder.yudao.module.showroom.content.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShowroomProductAttachmentPolicyTest {

    @Test
    void validateUploadShouldAcceptM4vVideoAttachment() {
        assertEquals(ShowroomProductAttachmentPolicy.ASSET_TYPE_VIDEO,
                ShowroomProductAttachmentPolicy.validateUpload("video", "七木-鼻窦支架.m4v",
                        "video/mp4", 1024L));
    }

    @Test
    void validateUploadShouldAcceptUppercaseM4vVideoAttachment() {
        assertEquals(ShowroomProductAttachmentPolicy.ASSET_TYPE_VIDEO,
                ShowroomProductAttachmentPolicy.validateUpload("video", "璞慧-神经取栓2022.11.21.m4V",
                        "video/mp4", 1024L));
    }
}
