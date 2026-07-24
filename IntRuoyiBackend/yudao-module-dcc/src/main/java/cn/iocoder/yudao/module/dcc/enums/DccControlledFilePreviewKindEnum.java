package cn.iocoder.yudao.module.dcc.enums;

import cn.hutool.core.util.StrUtil;

public enum DccControlledFilePreviewKindEnum {

    PDF("PDF"),
    IMAGE("IMAGE"),
    TEXT("TEXT"),
    OFFICE("OFFICE"),
    VIDEO("VIDEO"),
    AUDIO("AUDIO"),
    DOWNLOAD_ONLY("DOWNLOAD_ONLY");

    private final String code;

    DccControlledFilePreviewKindEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static DccControlledFilePreviewKindEnum resolve(String fileName, String contentType) {
        String normalizedFileName = StrUtil.nullToEmpty(fileName).toLowerCase();
        String normalizedContentType = StrUtil.nullToEmpty(contentType).toLowerCase();
        if (normalizedContentType.equals("application/pdf") || normalizedFileName.endsWith(".pdf")) {
            return PDF;
        }
        if (normalizedContentType.startsWith("image/") || normalizedFileName.matches(".*\\.(png|jpg|jpeg|gif|webp|bmp|tif|tiff)$")) {
            return IMAGE;
        }
        if (normalizedContentType.startsWith("video/")
                || normalizedFileName.matches(".*\\.(mp4|m4v|webm|ogv|mov)$")) {
            return VIDEO;
        }
        if (normalizedContentType.startsWith("audio/")
                || normalizedFileName.matches(".*\\.(mp3|wav|ogg|oga|m4a|aac|flac)$")) {
            return AUDIO;
        }
        if (normalizedContentType.startsWith("text/")
                || normalizedContentType.equals("application/json")
                || normalizedContentType.equals("application/xml")
                || normalizedContentType.equals("application/javascript")
                || normalizedContentType.equals("application/x-yaml")
                || normalizedFileName.matches(".*\\.(txt|csv|md|json|xml|log|yaml|yml|html|htm|css|js|ts|java|sql|properties|ini|conf)$")) {
            return TEXT;
        }
        if (normalizedFileName.matches(".*\\.(doc|docx|xls|xlsx|ppt|pptx|odt|ods|odp|rtf)$")
                || normalizedContentType.contains("officedocument")
                || normalizedContentType.contains("msword")
                || normalizedContentType.contains("ms-excel")
                || normalizedContentType.contains("ms-powerpoint")
                || normalizedContentType.contains("opendocument")
                || normalizedContentType.equals("application/rtf")) {
            return OFFICE;
        }
        return DOWNLOAD_ONLY;
    }
}
