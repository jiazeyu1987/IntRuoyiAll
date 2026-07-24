package cn.iocoder.yudao.module.showroom.controller.admin.vo.product;

public record ShowroomProductImportExtra(String productName, String sellingPointsCopy, ImportedCoverImage coverImage) {

    public boolean hasCoverImage() {
        return coverImage != null;
    }

    public record ImportedCoverImage(byte[] content, String fileExtension, String mimeType) {
    }
}
