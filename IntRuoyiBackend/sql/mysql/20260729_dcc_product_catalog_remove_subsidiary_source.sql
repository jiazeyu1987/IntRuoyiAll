-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260729_dcc_product_catalog_project_code_columns; type=data; riskLevel=medium
-- DCC 产品目录停用“子公司产品”来源数据；后续只展示“瑛泰产品”来源，历史记录保留供审计。
-- Rollback: set deleted = b'0' for the exact subsidiary-source rows after reconciling any user edits.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

UPDATE `dcc_product_catalog`
   SET `deleted` = b'1',
       `updater` = 'dcc-catalog-remove-subsidiary',
       `update_time` = NOW()
 WHERE HEX(`data_source`) = 'E5AD90E585ACE58FB8E4BAA7E59381'
   AND `deleted` = b'0';
