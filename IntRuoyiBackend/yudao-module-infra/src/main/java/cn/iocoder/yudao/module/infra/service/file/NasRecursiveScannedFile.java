package cn.iocoder.yudao.module.infra.service.file;

public record NasRecursiveScannedFile(
        String rootPath,
        String path,
        String name,
        Long size,
        Long modifiedAt,
        Boolean hidden,
        Boolean system
) {
}
