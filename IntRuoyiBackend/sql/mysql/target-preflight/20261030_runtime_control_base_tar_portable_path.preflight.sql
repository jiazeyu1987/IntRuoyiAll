-- release-target-preflight: migrationId=20261030_runtime_control_base_tar_portable_path; allowedEnvironments=test,prod,backup
SELECT CASE
  WHEN COUNT(*) <= 1 AND COUNT(*) = COALESCE(SUM(CASE
    WHEN BINARY value IN (
      'D:/ProjectPackage/Int/BaseImages/intruoyi-backend-runtime-base-2026.06.05-jre21-noble-docker29.2.1.tar',
      '.runtime/base-images/intruoyi-backend-runtime-base-2026.06.05-jre21-noble-docker29.2.1.tar'
    ) THEN 1 ELSE 0 END), 0)
  THEN 'TARGET_PREFLIGHT_PASS:20261030_runtime_control_base_tar_portable_path'
  ELSE 'TARGET_PREFLIGHT_BLOCKED:20261030_runtime_control_base_tar_portable_path'
END
FROM infra_config
WHERE config_key = 'runtime-control.release-package.backend-runtime-base-tar-path';
