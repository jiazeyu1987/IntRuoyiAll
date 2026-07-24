package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeControlReleasePackageConfigSqlContractTest {

    private static final Path RELEASE_PACKAGE_CONFIG_SQL_PATH = Path.of(System.getProperty("user.dir"))
            .resolve("../sql/mysql/20260613_runtime_control_release_package_config.sql")
            .normalize();

    @Test
    void seedSqlShouldCreateRuntimeReleasePackageDatabaseConfigIdempotently() throws Exception {
        assertTrue(Files.isRegularFile(RELEASE_PACKAGE_CONFIG_SQL_PATH),
                "runtime release package config seed SQL must exist");
        String sql = Files.readString(RELEASE_PACKAGE_CONFIG_SQL_PATH, StandardCharsets.UTF_8);

        assertTrue(sql.contains("infra_config"), "seed SQL must insert infra_config rows");
        assertTrue(sql.contains("runtime-control.release-package.backend-runtime-base-mode"));
        assertTrue(sql.contains("runtime-control.release-package.backend-runtime-base-tar-path"));
        assertTrue(sql.contains("runtime-control.release-package.backend-runtime-base-tar-sha256"));
        assertTrue(sql.contains("runtime-control.release-package.backend-runtime-base-image"));
        assertTrue(sql.contains("runtime-control.release-package.backend-runtime-base-digest"));
        assertTrue(sql.contains("runtime-control.release-package.backend-runtime-base-version"));
        assertTrue(sql.contains("offline-tar"), "seed SQL must set the default offline tar mode");
        assertTrue(sql.contains("WHERE NOT EXISTS"), "seed SQL must not overwrite customized config rows");
        assertTrue(!sql.contains("ON DUPLICATE KEY UPDATE"),
                "seed SQL must not overwrite administrator changes through duplicate-key updates");
        assertTrue(sql.contains("b'1'"), "non-secret runtime release package config should be visible");
    }
}
