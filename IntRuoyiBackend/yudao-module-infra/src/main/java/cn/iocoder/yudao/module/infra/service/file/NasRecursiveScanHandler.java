package cn.iocoder.yudao.module.infra.service.file;

public interface NasRecursiveScanHandler {

    void onCurrentDirectory(String path);

    void onFile(NasRecursiveScannedFile file);

    void onSkippedDirectory(NasRecursiveSkippedDirectory directory);
}
