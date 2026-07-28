-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260728_rename_mdm_product_menu; type=data; riskLevel=low
-- Correct the MDM product menu name using UTF-8 hex so mysql client charset cannot corrupt the value.

BEGIN;

SET @mdm_product_menu_name := CONVERT(UNHEX('E5B195E58E85E4B8BBE695B0E68DAE') USING utf8mb4);

UPDATE `system_menu`
SET `name` = @mdm_product_menu_name,
    `updater` = 'fix-mdm-product-menu-utf8',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `id` = 990201
  AND `permission` = 'mdm:product:query';

COMMIT;
