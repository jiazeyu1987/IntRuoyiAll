-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=config; riskLevel=medium
INSERT INTO `infra_config` (
  `category`, `type`, `name`, `config_key`, `value`, `visible`, `remark`,
  `creator`, `create_time`, `updater`, `update_time`, `deleted`
)
SELECT
  seed.`category`, seed.`type`, seed.`name`, seed.`config_key`, seed.`value`, seed.`visible`, seed.`remark`,
  '1', NOW(), '1', NOW(), b'0'
FROM (
  SELECT 'runtime-control' AS `category`, 1 AS `type`,
         '运行控制台-后端基础运行镜像模式' AS `name`,
         'runtime-control.release-package.backend-runtime-base-mode' AS `config_key`,
         'offline-tar' AS `value`, b'1' AS `visible`,
         '运行控制台构建 release 包时传给发布脚本的 -BackendRuntimeBaseMode。' AS `remark`
  UNION ALL
  SELECT 'runtime-control', 1,
         '运行控制台-后端基础运行镜像离线包路径',
         'runtime-control.release-package.backend-runtime-base-tar-path',
         'D:/ProjectPackage/Int/BaseImages/intruoyi-backend-runtime-base-2026.06.05-jre21-noble-docker29.2.1.tar',
         b'1',
         '运行控制台构建 release 包时传给发布脚本的 -BackendRuntimeBaseTarPath。'
  UNION ALL
  SELECT 'runtime-control', 1,
         '运行控制台-后端基础运行镜像离线包 SHA256',
         'runtime-control.release-package.backend-runtime-base-tar-sha256',
         '5bcd568b46ba9f28bfa7ac8ee67e283aea95bd585a89842a14cc40827fa71603',
         b'1',
         '运行控制台构建 release 包时传给发布脚本的 -BackendRuntimeBaseTarSha256。'
  UNION ALL
  SELECT 'runtime-control', 1,
         '运行控制台-后端基础运行镜像名称',
         'runtime-control.release-package.backend-runtime-base-image',
         'intruoyi-backend-runtime-base:2026.06.05-jre21-noble-docker29.2.1',
         b'1',
         '运行控制台构建 release 包时传给发布脚本的 -BackendRuntimeBaseImage。'
  UNION ALL
  SELECT 'runtime-control', 1,
         '运行控制台-后端基础运行镜像 digest',
         'runtime-control.release-package.backend-runtime-base-digest',
         'sha256:b4f7d85f325665c3b372379e6d352c3c8be2d3a08add68ac0dc151f720160be7',
         b'1',
         '运行控制台构建 release 包时传给发布脚本的 -BackendRuntimeBaseDigest。'
  UNION ALL
  SELECT 'runtime-control', 1,
         '运行控制台-后端基础运行镜像版本',
         'runtime-control.release-package.backend-runtime-base-version',
         '2026.06.05-jre21-noble-docker29.2.1',
         b'1',
         '运行控制台构建 release 包时传给发布脚本的 -BackendRuntimeBaseVersion。'
) AS seed
WHERE NOT EXISTS (
  SELECT 1 FROM `infra_config`
  WHERE `config_key` = seed.`config_key`
);
