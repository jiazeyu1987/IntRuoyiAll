package cn.iocoder.yudao.module.showroom.content.model;

public record ShowroomProductAttachment(Long id, Long productId, Long productRevisionId, String assetType,
                                        Long fileId, String originalName, String mimeType, Long fileSize,
                                        int displayOrder) {
}
