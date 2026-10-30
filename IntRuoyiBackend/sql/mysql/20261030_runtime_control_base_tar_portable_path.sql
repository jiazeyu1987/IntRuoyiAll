-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260613_runtime_control_release_package_config; type=config; riskLevel=low
UPDATE `infra_config`
SET `value` = '.runtime/base-images/intruoyi-backend-runtime-base-2026.06.05-jre21-noble-docker29.2.1.tar'
WHERE `config_key` = 'runtime-control.release-package.backend-runtime-base-tar-path'
  AND `value` = 'D:/ProjectPackage/Int/BaseImages/intruoyi-backend-runtime-base-2026.06.05-jre21-noble-docker29.2.1.tar';
