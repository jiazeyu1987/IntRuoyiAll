-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260705_showroom_legacy_product_code_name_backfill; type=data; riskLevel=medium
-- 20260706 showroom legacy product code English-name backfill
-- Generated from strict unique normalized English-name matches only.
-- Safe guards: target legacy_product_code must still be NULL and product_* must not be occupied.
START TRANSACTION;

-- tenant_id=1 product_015 -> INT-15
-- product_name_en=Inflation Device II
UPDATE showroom_product
SET legacy_product_code = 'product_015'
WHERE tenant_id = 1
AND product_code = 'INT-15'
AND deleted = 0
AND legacy_product_code IS NULL
AND NOT EXISTS (
    SELECT 1 FROM (
        SELECT id FROM showroom_product
        WHERE tenant_id = 1
        AND legacy_product_code = 'product_015'
        AND deleted = 0
    ) occupied
);

-- tenant_id=1 product_066 -> INT-67
-- product_name_en=Neural Aspiration Catheter
UPDATE showroom_product
SET legacy_product_code = 'product_066'
WHERE tenant_id = 1
AND product_code = 'INT-67'
AND deleted = 0
AND legacy_product_code IS NULL
AND NOT EXISTS (
    SELECT 1 FROM (
        SELECT id FROM showroom_product
        WHERE tenant_id = 1
        AND legacy_product_code = 'product_066'
        AND deleted = 0
    ) occupied
);

-- tenant_id=1 product_081 -> INT-82
-- product_name_en=Delivery Catheter
UPDATE showroom_product
SET legacy_product_code = 'product_081'
WHERE tenant_id = 1
AND product_code = 'INT-82'
AND deleted = 0
AND legacy_product_code IS NULL
AND NOT EXISTS (
    SELECT 1 FROM (
        SELECT id FROM showroom_product
        WHERE tenant_id = 1
        AND legacy_product_code = 'product_081'
        AND deleted = 0
    ) occupied
);

-- tenant_id=1 product_086 -> INT-87
-- product_name_en=Support Catheter
UPDATE showroom_product
SET legacy_product_code = 'product_086'
WHERE tenant_id = 1
AND product_code = 'INT-87'
AND deleted = 0
AND legacy_product_code IS NULL
AND NOT EXISTS (
    SELECT 1 FROM (
        SELECT id FROM showroom_product
        WHERE tenant_id = 1
        AND legacy_product_code = 'product_086'
        AND deleted = 0
    ) occupied
);

-- tenant_id=1 product_095 -> INT-96
-- product_name_en=Absorbable Haemostatic Matrix
UPDATE showroom_product
SET legacy_product_code = 'product_095'
WHERE tenant_id = 1
AND product_code = 'INT-96'
AND deleted = 0
AND legacy_product_code IS NULL
AND NOT EXISTS (
    SELECT 1 FROM (
        SELECT id FROM showroom_product
        WHERE tenant_id = 1
        AND legacy_product_code = 'product_095'
        AND deleted = 0
    ) occupied
);

-- tenant_id=1 product_149 -> INT-150
-- product_name_en=Steerable Catheter
UPDATE showroom_product
SET legacy_product_code = 'product_149'
WHERE tenant_id = 1
AND product_code = 'INT-150'
AND deleted = 0
AND legacy_product_code IS NULL
AND NOT EXISTS (
    SELECT 1 FROM (
        SELECT id FROM showroom_product
        WHERE tenant_id = 1
        AND legacy_product_code = 'product_149'
        AND deleted = 0
    ) occupied
);

-- tenant_id=122 product_015 -> INT-15
-- product_name_en=Inflation Device II
UPDATE showroom_product
SET legacy_product_code = 'product_015'
WHERE tenant_id = 122
AND product_code = 'INT-15'
AND deleted = 0
AND legacy_product_code IS NULL
AND NOT EXISTS (
    SELECT 1 FROM (
        SELECT id FROM showroom_product
        WHERE tenant_id = 122
        AND legacy_product_code = 'product_015'
        AND deleted = 0
    ) occupied
);

-- tenant_id=122 product_066 -> INT-67
-- product_name_en=Neural Aspiration Catheter
UPDATE showroom_product
SET legacy_product_code = 'product_066'
WHERE tenant_id = 122
AND product_code = 'INT-67'
AND deleted = 0
AND legacy_product_code IS NULL
AND NOT EXISTS (
    SELECT 1 FROM (
        SELECT id FROM showroom_product
        WHERE tenant_id = 122
        AND legacy_product_code = 'product_066'
        AND deleted = 0
    ) occupied
);

-- tenant_id=122 product_081 -> INT-82
-- product_name_en=Delivery Catheter
UPDATE showroom_product
SET legacy_product_code = 'product_081'
WHERE tenant_id = 122
AND product_code = 'INT-82'
AND deleted = 0
AND legacy_product_code IS NULL
AND NOT EXISTS (
    SELECT 1 FROM (
        SELECT id FROM showroom_product
        WHERE tenant_id = 122
        AND legacy_product_code = 'product_081'
        AND deleted = 0
    ) occupied
);

-- tenant_id=122 product_086 -> INT-87
-- product_name_en=Support Catheter
UPDATE showroom_product
SET legacy_product_code = 'product_086'
WHERE tenant_id = 122
AND product_code = 'INT-87'
AND deleted = 0
AND legacy_product_code IS NULL
AND NOT EXISTS (
    SELECT 1 FROM (
        SELECT id FROM showroom_product
        WHERE tenant_id = 122
        AND legacy_product_code = 'product_086'
        AND deleted = 0
    ) occupied
);

-- tenant_id=122 product_095 -> INT-96
-- product_name_en=Absorbable Haemostatic Matrix
UPDATE showroom_product
SET legacy_product_code = 'product_095'
WHERE tenant_id = 122
AND product_code = 'INT-96'
AND deleted = 0
AND legacy_product_code IS NULL
AND NOT EXISTS (
    SELECT 1 FROM (
        SELECT id FROM showroom_product
        WHERE tenant_id = 122
        AND legacy_product_code = 'product_095'
        AND deleted = 0
    ) occupied
);

-- tenant_id=122 product_149 -> INT-150
-- product_name_en=Steerable Catheter
UPDATE showroom_product
SET legacy_product_code = 'product_149'
WHERE tenant_id = 122
AND product_code = 'INT-150'
AND deleted = 0
AND legacy_product_code IS NULL
AND NOT EXISTS (
    SELECT 1 FROM (
        SELECT id FROM showroom_product
        WHERE tenant_id = 122
        AND legacy_product_code = 'product_149'
        AND deleted = 0
    ) occupied
);
COMMIT;
