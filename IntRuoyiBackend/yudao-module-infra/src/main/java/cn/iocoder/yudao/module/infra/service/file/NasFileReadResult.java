package cn.iocoder.yudao.module.infra.service.file;

public record NasFileReadResult(
        String name,
        String path,
        String contentType,
        byte[] bytes
) {
}
