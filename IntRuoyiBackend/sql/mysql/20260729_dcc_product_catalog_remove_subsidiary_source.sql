-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260729_dcc_product_catalog_project_code_columns; type=data; riskLevel=medium
-- DCC 产品目录删除“子公司产品”来源数据；后续只保留“瑛泰产品”来源。
-- Rollback: restore deleted subsidiary-source rows from the pre-migration database backup or the original 20260710 seed file after reconciling any user edits.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DELETE FROM `dcc_product_catalog`
WHERE HEX(`data_source`) = 'E5AD90E585ACE58FB8E4BAA7E59381';

