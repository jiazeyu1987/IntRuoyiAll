package cn.iocoder.yudao.module.bpm.approval.content;
public record PreviewAttachment(String fileId, String fileName, String version, String sha256,
        String fileSize, String contentType, String readUrl) { }
