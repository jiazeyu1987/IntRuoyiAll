-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260521_showroom_company_cover_image; type=schema; riskLevel=medium
ALTER TABLE `showroom_product`
    ADD COLUMN `legacy_product_code` varchar(64) DEFAULT NULL COMMENT '旧展厅底表产品编码，如 product_001' AFTER `product_code`;

CREATE UNIQUE INDEX `uk_showroom_product_legacy_code`
    ON `showroom_product` (`tenant_id`, `legacy_product_code`);
