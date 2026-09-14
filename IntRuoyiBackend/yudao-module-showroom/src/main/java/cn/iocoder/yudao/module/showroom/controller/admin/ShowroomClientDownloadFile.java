package cn.iocoder.yudao.module.showroom.controller.admin;

import org.springframework.http.MediaType;

public record ShowroomClientDownloadFile(String code, String resourcePath, String fileName, MediaType contentType) {

    public static final ShowroomClientDownloadFile ANDROID = new ShowroomClientDownloadFile(
            "android",
            "showroom/client-downloads/v1.0/YingtaiShowroomClient-Android-v1.0.apk",
            "YingtaiShowroomClient-Android-v1.0.apk",
            MediaType.parseMediaType("application/vnd.android.package-archive"));
}
