package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

class RuntimeControlReleasePackageConfigServiceImplTest extends BaseMockitoUnitTest {

    @Mock
    private ConfigService configService;

    private RuntimeControlReleasePackageConfigServiceImpl configServiceImpl;

    @BeforeEach
    void setUp() {
        configServiceImpl = new RuntimeControlReleasePackageConfigServiceImpl(configService);
    }

    @Test
    void getRequiredBackendRuntimeBaseConfigShouldReadDatabaseConfigKeys() {
        doReturn(config(RuntimeControlReleasePackageConfigServiceImpl.KEY_BACKEND_RUNTIME_BASE_MODE, " offline-tar "))
                .when(configService).getConfigByKey(eq(RuntimeControlReleasePackageConfigServiceImpl.KEY_BACKEND_RUNTIME_BASE_MODE));
        doReturn(config(RuntimeControlReleasePackageConfigServiceImpl.KEY_BACKEND_RUNTIME_BASE_TAR_PATH,
                " D:/ProjectPackage/Int/BaseImages/intruoyi-backend-runtime-base-20260604.tar "))
                .when(configService).getConfigByKey(eq(RuntimeControlReleasePackageConfigServiceImpl.KEY_BACKEND_RUNTIME_BASE_TAR_PATH));
        doReturn(config(RuntimeControlReleasePackageConfigServiceImpl.KEY_BACKEND_RUNTIME_BASE_TAR_SHA256,
                " 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef "))
                .when(configService).getConfigByKey(eq(RuntimeControlReleasePackageConfigServiceImpl.KEY_BACKEND_RUNTIME_BASE_TAR_SHA256));
        doReturn(config(RuntimeControlReleasePackageConfigServiceImpl.KEY_BACKEND_RUNTIME_BASE_IMAGE,
                " intruoyi-backend-runtime-base:20260604 "))
                .when(configService).getConfigByKey(eq(RuntimeControlReleasePackageConfigServiceImpl.KEY_BACKEND_RUNTIME_BASE_IMAGE));
        doReturn(config(RuntimeControlReleasePackageConfigServiceImpl.KEY_BACKEND_RUNTIME_BASE_DIGEST,
                " sha256:abcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcd "))
                .when(configService).getConfigByKey(eq(RuntimeControlReleasePackageConfigServiceImpl.KEY_BACKEND_RUNTIME_BASE_DIGEST));
        doReturn(config(RuntimeControlReleasePackageConfigServiceImpl.KEY_BACKEND_RUNTIME_BASE_VERSION, " 20260604 "))
                .when(configService).getConfigByKey(eq(RuntimeControlReleasePackageConfigServiceImpl.KEY_BACKEND_RUNTIME_BASE_VERSION));

        RuntimeControlReleasePackageConfig result = configServiceImpl.getRequiredBackendRuntimeBaseConfig();

        assertEquals("offline-tar", result.backendRuntimeBaseMode());
        assertEquals("D:/ProjectPackage/Int/BaseImages/intruoyi-backend-runtime-base-20260604.tar",
                result.backendRuntimeBaseTarPath());
        assertEquals("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                result.backendRuntimeBaseTarSha256());
        assertEquals("intruoyi-backend-runtime-base:20260604", result.backendRuntimeBaseImage());
        assertEquals("sha256:abcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcd",
                result.backendRuntimeBaseDigest());
        assertEquals("20260604", result.backendRuntimeBaseVersion());
    }

    @Test
    void getRequiredBackendRuntimeBaseConfigShouldFailFastWhenDatabaseConfigIsMissing() {
        doReturn(config(RuntimeControlReleasePackageConfigServiceImpl.KEY_BACKEND_RUNTIME_BASE_MODE, "offline-tar"))
                .when(configService).getConfigByKey(eq(RuntimeControlReleasePackageConfigServiceImpl.KEY_BACKEND_RUNTIME_BASE_MODE));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> configServiceImpl.getRequiredBackendRuntimeBaseConfig());

        assertTrue(exception.getMessage().contains("infra_config.runtime-control.release-package.backend-runtime-base-tar-path"));
        assertTrue(exception.getMessage().contains("-BackendRuntimeBaseTarPath"));
    }

    private ConfigDO config(String key, String value) {
        ConfigDO config = new ConfigDO();
        config.setConfigKey(key);
        config.setValue(value);
        return config;
    }
}
