package cn.iocoder.yudao.module.infra.service.runtimecontrol;

public record RuntimeControlReleasePackageConfig(
        String backendRuntimeBaseMode,
        String backendRuntimeBaseTarPath,
        String backendRuntimeBaseTarSha256,
        String backendRuntimeBaseImage,
        String backendRuntimeBaseDigest,
        String backendRuntimeBaseVersion) {
}
