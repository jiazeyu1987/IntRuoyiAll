package cn.iocoder.yudao.module.showroom.asset;

public record ShowroomPreviewAssetFiles(Long desktopFileId, Long mobileFileId, Long padFileId) {

    public boolean hasAllDeviceFiles() {
        return desktopFileId != null && mobileFileId != null && padFileId != null;
    }

}
