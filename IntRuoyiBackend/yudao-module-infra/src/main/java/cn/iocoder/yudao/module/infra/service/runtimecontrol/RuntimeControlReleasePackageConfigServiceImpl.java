package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import org.springframework.stereotype.Service;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED;

@Service
public class RuntimeControlReleasePackageConfigServiceImpl implements RuntimeControlReleasePackageConfigService {

    public static final String KEY_BACKEND_RUNTIME_BASE_MODE =
            "runtime-control.release-package.backend-runtime-base-mode";
    public static final String KEY_BACKEND_RUNTIME_BASE_TAR_PATH =
            "runtime-control.release-package.backend-runtime-base-tar-path";
    public static final String KEY_BACKEND_RUNTIME_BASE_TAR_SHA256 =
            "runtime-control.release-package.backend-runtime-base-tar-sha256";
    public static final String KEY_BACKEND_RUNTIME_BASE_IMAGE =
            "runtime-control.release-package.backend-runtime-base-image";
    public static final String KEY_BACKEND_RUNTIME_BASE_DIGEST =
            "runtime-control.release-package.backend-runtime-base-digest";
    public static final String KEY_BACKEND_RUNTIME_BASE_VERSION =
            "runtime-control.release-package.backend-runtime-base-version";

    private final ConfigService configService;

    public RuntimeControlReleasePackageConfigServiceImpl(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public RuntimeControlReleasePackageConfig getRequiredBackendRuntimeBaseConfig() {
        return new RuntimeControlReleasePackageConfig(
                requiredConfigValue(KEY_BACKEND_RUNTIME_BASE_MODE, "-BackendRuntimeBaseMode"),
                requiredConfigValue(KEY_BACKEND_RUNTIME_BASE_TAR_PATH, "-BackendRuntimeBaseTarPath"),
                requiredConfigValue(KEY_BACKEND_RUNTIME_BASE_TAR_SHA256, "-BackendRuntimeBaseTarSha256"),
                requiredConfigValue(KEY_BACKEND_RUNTIME_BASE_IMAGE, "-BackendRuntimeBaseImage"),
                requiredConfigValue(KEY_BACKEND_RUNTIME_BASE_DIGEST, "-BackendRuntimeBaseDigest"),
                requiredConfigValue(KEY_BACKEND_RUNTIME_BASE_VERSION, "-BackendRuntimeBaseVersion"));
    }

    private String requiredConfigValue(String key, String scriptArgumentName) {
        ConfigDO config = configService.getConfigByKey(key);
        String value = config == null ? null : config.getValue();
        if (StrUtil.isBlank(value)) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED,
                    "infra_config." + key + " (" + scriptArgumentName + ")");
        }
        return StrUtil.trim(value);
    }

}
