-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260607_product_master_data; type=data; riskLevel=low
-- Rename the MDM product menu tab label while preserving product master business wording.

BEGIN;

SET @mdm_product_menu_name := '展厅主数据';

UPDATE `system_menu`
SET `name` = @mdm_product_menu_name,
    `updater` = 'rename-mdm-product-menu',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `id` = 990201
  AND `permission` = 'mdm:product:query';

COMMIT;
